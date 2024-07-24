/*
 * Copyright (c) 2021-2022 International Innovation Center of Tsinghua University, Shanghai
 * Ventus is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details. */
package pipeline

import chisel3._
import chisel3.util._
import top.parameters._
object LookupTree {
  def apply[T <: Data](key: UInt, mapping: Iterable[(UInt, T)]): T =
    Mux1H(mapping.map(p => (p._1 === key, p._2)))
}
class MULin(num_thread: Int = num_thread) extends Bundle{
  val mask                = Vec(num_thread,Bool())
  val a, b, c             = (UInt(xLen.W))
  val ctrl                = new CtrlSigs
}
class MULout(num_thread: Int = num_thread) extends Bundle{
  val mask = Vec(num_thread,Bool())
  val result = UInt(xLen.W)
  val ctrl = new CtrlSigs
}

abstract class MulModule(num_thread: Int = num_thread) extends Module{
  val io = IO(new Bundle{
    val in = Flipped(DecoupledIO(new MULin(num_thread)))
    val out = DecoupledIO(new MULout(num_thread))
  })
}

trait HasPipelineReg{ this: MulModule =>
  def latency: Int

  //val ready = Wire(Bool())
  //val cnt = RegInit(0.U((log2Up(latency)+1).W))

  //ready := (cnt < latency.U) || (cnt === latency.U && io.out.ready)
  //cnt := cnt + io.in.fire - io.out.fire

  val valids = io.in.valid +: Array.fill(latency)(RegInit(false.B))
  for(i <- 1 to latency){
    when(!(!io.out.ready && valids.drop(i).reduce(_&&_) )){ valids(i) := valids(i-1) }
  }

  def regEnable(i: Int): Bool = valids(i-1) && !(!io.out.ready && valids.drop(i).reduce(_&&_) )

  def PipelineReg[T<:Data](i: Int)(next: T) = RegEnable(next, valids(i-1) && !(!io.out.ready && valids.drop(i).reduce(_&&_) ))
  def S1Reg[T<:Data](next: T):T = PipelineReg[T](1)(next)
  def S2Reg[T<:Data](next: T):T = PipelineReg[T](2)(next)
  def S3Reg[T<:Data](next: T):T = PipelineReg[T](3)(next)
  def S4Reg[T<:Data](next: T):T = PipelineReg[T](4)(next)
  def S5Reg[T<:Data](next: T):T = PipelineReg[T](5)(next)

  io.in.ready := !(!io.out.ready && valids.drop(1).reduce(_&&_))
  io.out.valid := valids.last
}

abstract class CarrySaveAdderMToN(m: Int, n: Int)(len: Int) extends Module{
  val io = IO(new Bundle() {
    val in = Input(Vec(m, UInt(len.W)))
    val out = Output(Vec(n, UInt(len.W)))
  })
}

class CSA2_2(len: Int) extends CarrySaveAdderMToN(2, 2)(len) {
  val temp = Wire(Vec(len, UInt(2.W)))
  for((t, i) <- temp.zipWithIndex){
    val (a, b) = (io.in(0)(i), io.in(1)(i))
    val sum = a ^ b
    val cout = a & b
    t := Cat(cout, sum)
  }
  io.out.zipWithIndex.foreach({case(x, i) => x := Cat(temp.reverse map(_(i)))})
}

class CSA3_2(len: Int) extends CarrySaveAdderMToN(3, 2)(len){
  val temp = Wire(Vec(len, UInt(2.W)))
  for((t, i) <- temp.zipWithIndex){
    val (a, b, cin) = (io.in(0)(i), io.in(1)(i), io.in(2)(i))
    val a_xor_b = a ^ b
    val a_and_b = a & b
    val sum = a_xor_b ^ cin
    val cout = a_and_b | (a_xor_b & cin)
    t := Cat(cout, sum)
  }
  io.out.zipWithIndex.foreach({case(x, i) => x := Cat(temp.reverse map(_(i)))})
}

class CSA5_3(len: Int)extends CarrySaveAdderMToN(5, 3)(len){
  val FAs = Array.fill(2)(Module(new CSA3_2(len)))
  FAs(0).io.in := io.in.take(3)
  FAs(1).io.in := VecInit(FAs(0).io.out(0), io.in(3), io.in(4))
  io.out := VecInit(FAs(1).io.out(0), FAs(0).io.out(1), FAs(1).io.out(1))
}

class C22 extends CSA2_2(1)
class C32 extends CSA3_2(1)
class C53 extends CSA5_3(1)

object SignExt {
  def apply(a: UInt, len: Int): UInt = {
    val aLen = a.getWidth
    val signBit = a(aLen-1)
    if (aLen >= len) a(len-1,0) else Cat(Fill(len - aLen, signBit), a)
  }
}

object ZeroExt {
  def apply(a: UInt, len: Int): UInt = {
    val aLen = a.getWidth
    if (aLen >= len) a(len-1,0) else Cat(0.U((len - aLen).W), a)
  }
}
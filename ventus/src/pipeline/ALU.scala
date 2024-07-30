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

//test

package pipeline

import chisel3._
import chisel3.util._
//import chisel3.stage.ChiselStage._
import scala.util.Random._
/*
==== Supported Instructions ====
RV32I except:
ecall/ebreak
*/
object ALUOps {
  val SZ_ALU_FUNC = 5
  def FN_X = BitPat("b?????")
  def FN_ADD = 0.U(5.W)
  def FN_SL = 1.U(5.W)
  def FN_SEQ = 2.U(5.W)
  def FN_SNE = 3.U(5.W)
  def FN_XOR = 4.U(5.W)
  def FN_SR = 5.U(5.W)
  def FN_OR = 6.U(5.W)
  def FN_AND = 7.U(5.W)
  def FN_SUB = 10.U(5.W)
  def FN_SRA = 11.U(5.W)
  def FN_SLT = 12.U(5.W)
  def FN_SGE = 13.U(5.W)
  def FN_SLTU = 14.U(5.W)
  def FN_SGEU = 15.U(5.W)
  def FN_MAX=16.U(5.W)
  def FN_MIN=17.U(5.W)
  def FN_MAXU=18.U(5.W)
  def FN_MINU=19.U(5.W)
  def FN_A1ZERO = 8.U(5.W)
  def FN_A2ZERO: UInt = 9.U(5.W)
  def FN_MUL = 20.U(5.W)
  def FN_MULH = 21.U(5.W)
  def FN_MULHU = 22.U(5.W)
  def FN_MULHSU = 23.U(5.W)
  def FN_MACC = 24.U(5.W)
  def FN_NMSAC = 25.U(5.W)
  def FN_MADD = 26.U(5.W)
  def FN_NMSUB = 27.U(5.W)

  def isSub(cmd: UInt) = (cmd >= FN_SUB) & (cmd <= FN_SGEU)
  def isCmp(cmd: UInt) = (cmd >= FN_SLT) & (cmd <= FN_SGEU)
  def cmpUnsigned(cmd: UInt) = cmd(1)
  def cmpInverted(cmd: UInt) = cmd(0)
  def cmpEq(cmd: UInt) = !cmd(3)
  def isMIN(cmd:UInt)=(cmd(4,2)===("b100").U)
  def isMUL(cmd:UInt)=(cmd(4,2)===("b101").U)
  def isMAC(cmd:UInt)=(cmd(4,2)===("b110").U)
}

import ALUOps._
import top.parameters._
class ScalarALU() extends Module{
  val io = IO(new Bundle() {
    val func        = Input(UInt(5.W))
    val in2         = Input(UInt(xLen.W))
    val in1         = Input(UInt(xLen.W))
    val in3         = Input(UInt(xLen.W))
    val out         = Output(UInt(xLen.W))
    //val adder_out   = Output(UInt(xLen.W))
    val cmp_out     = Output(Bool())
  })

  //ADD, SUB
  val in2_inv = Mux(isSub(io.func), (~io.in2).asUInt, io.in2)
  val adder_out = io.in1 + in2_inv + isSub(io.func).asUInt
  val in1_xor_in2 = io.in1 ^ in2_inv

  //SLT, SLTU
  val slt =
    Mux(io.in1(xLen-1) === io.in2(xLen-1), adder_out(xLen-1),
      Mux(cmpUnsigned(io.func), io.in2(xLen-1), io.in1(xLen-1)))
  io.cmp_out := cmpInverted(io.func) ^ Mux(cmpEq(io.func), in1_xor_in2 === 0.U(xLen.W), slt)

  //SLL, SRL, SRA
  val (shamt, shin_r) = (io.in2(4,0), io.in1)
  val shin = Mux(io.func === FN_SR || io.func === FN_SRA, shin_r, Reverse(shin_r))
  val shout_r = (Cat(isSub(io.func)&shin(xLen-1), shin).asSInt >> shamt)(xLen-1, 0)
  val shout_l = Reverse(shout_r)
  val shout = Mux(io.func === FN_SR || io.func === FN_SRA, shout_r, 0.U(xLen.W)) |
    Mux(io.func === FN_SL,                       shout_l, 0.U(xLen.W))

  //AND, OR, XOR
  val logic = Mux(io.func === FN_XOR, io.in1 ^ io.in2,
    Mux(io.func === FN_OR, io.in1 | io.in2,
      Mux(io.func === FN_AND, io.in1 & io.in2, 0.U(xLen.W))))

  val shift_logic_cmp = (isCmp(io.func)&&slt) | logic | shout
  val out = Mux(io.func === FN_ADD || io.func === FN_SUB, adder_out, Mux(io.func === FN_SEQ, io.in1 === io.in2 , Mux(io.func === FN_SNE,io.in1 =/= io.in2,shift_logic_cmp)))
  //val out = Mux(io.func === FN_ADD || io.func === FN_SUB, adder_out, shift_logic_cmp)

  //MIN, MAX
  val minu=Mux(io.in1>io.in2,io.in2,io.in1)
  val maxu=Mux(io.in1>io.in2,io.in1,io.in2)
  val in1s=io.in1.asSInt
  val in2s=io.in2.asSInt
  val mins=Mux(in1s>in2s,in2s,in1s).asUInt
  val maxs=Mux(in1s>in2s,in1s,in2s).asUInt
  val minmaxout = Mux(io.func===FN_MIN,mins,
                  Mux(io.func===FN_MAX,maxs,
                  Mux(io.func===FN_MINU,minu,maxu) ) )

  io.out := Mux(io.func===FN_A1ZERO,io.in2,
            Mux(io.func===FN_A2ZERO,io.in1,
            Mux(isMIN(io.func),minmaxout, out)))
  //debug
  //printf(p"0x${Hexadecimal(io.in1)} op 0x${Hexadecimal(io.in2)} = 0x${Hexadecimal(io.out)}\n")
  //~debug
}


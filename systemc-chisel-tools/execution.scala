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
import parameters._
import IDecode._

class BranchCtrl extends Bundle{
  val wid=UInt(depth_warp.W)
  val jump=Bool()
  val new_pc=UInt(32.W)
}
class ALUexe extends Module{
  val io = IO(new Bundle() {
    val in = Flipped(DecoupledIO(new sExeData()))
    val out = DecoupledIO(new WriteScalarCtrl())
    val out2br = DecoupledIO(new BranchCtrl())
  })
  val alu=Module(new ScalarALU())
  alu.io.in1:=io.in.bits.in1
  alu.io.in2:=io.in.bits.in2
  alu.io.in3:=io.in.bits.in3
  alu.io.func:=io.in.bits.ctrl.alu_fn(4,0)
  val result=Module(new Queue(new WriteScalarCtrl,1,pipe=true))
  val result_br=Module(new Queue(new BranchCtrl,1,pipe=true))
  result.io.deq<>io.out
  result_br.io.deq<>io.out2br
  result.io.enq.bits:=0.U.asTypeOf(new WriteScalarCtrl)
  result.io.enq.bits.warp_id:=io.in.bits.ctrl.wid
  result.io.enq.bits.wb_wxd_rd:=alu.io.out
  result.io.enq.bits.reg_idxw:=io.in.bits.ctrl.reg_idxw
  result.io.enq.bits.wxd:=io.in.bits.ctrl.wxd

  io.in.ready:=MuxLookup(io.in.bits.ctrl.branch,result_br.io.enq.ready&result.io.enq.ready,Seq(B_B->result_br.io.enq.ready,B_N->result.io.enq.ready))

  result_br.io.enq.bits.wid:=io.in.bits.ctrl.wid
  result_br.io.enq.bits.new_pc:=io.in.bits.in3
  result_br.io.enq.bits.jump:=MuxLookup(io.in.bits.ctrl.branch,false.B,Seq(B_B->alu.io.cmp_out,B_J->true.B,B_R->true.B))

  result_br.io.enq.valid:=io.in.valid&(io.in.bits.ctrl.branch=/=B_N)
  result.io.enq.valid:=io.in.valid&io.in.bits.ctrl.wxd
}


object ALUexeMain extends App {
  println("Generating the ALUexe hardware")
  (new chisel3.stage.ChiselStage).emitVerilog(new ALUexe(), Array("--target-dir", "generated"))
}

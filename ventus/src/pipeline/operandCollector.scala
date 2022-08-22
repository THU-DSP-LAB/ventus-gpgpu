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

class WriteVecCtrl extends Bundle{
  val wb_wfd_rd=(Vec(num_thread,UInt(xLen.W)))
  val wfd_mask=Vec(num_thread,Bool())
  val wfd=Bool()
  val reg_idxw=UInt(5.W)
  val warp_id=UInt(depth_warp.W)
}
class WriteScalarCtrl extends Bundle{
  val wb_wxd_rd=(UInt(xLen.W))
  val wxd=Bool()
  val reg_idxw=UInt(5.W)
  val warp_id=UInt(depth_warp.W)
}


class operandCollector extends Module{
  val io=IO(new Bundle {
    val control=Input(new CtrlSigs())
    //val inst=Input(UInt(32.W))
    val alu_src1=Output(Vec(num_thread,UInt(xLen.W)))
    val alu_src2=Output(Vec(num_thread,UInt(xLen.W)))
    val alu_src3=Output(Vec(num_thread,UInt(xLen.W)))
    val mask=Output(Vec(num_thread,Bool()))
    val writeScalarCtrl=Flipped(DecoupledIO(new WriteScalarCtrl)) //should be used as decoupledIO
    val writeVecCtrl=Flipped(DecoupledIO(new WriteVecCtrl))
    })
  val vectorRegFile=VecInit(Seq.fill(num_warp)(Module(new FloatRegFile).io))
  val scalarRegFile=VecInit(Seq.fill(num_warp)(Module(new RegFile).io))
  val imm=Module(new ImmGen())
  imm.io.inst:=io.control.inst
  imm.io.sel:=io.control.sel_imm

  vectorRegFile.foreach(x=>{
    x.rs1idx:=io.control.reg_idx1
    x.rs2idx:=io.control.reg_idx2
    x.rs3idx:=io.control.reg_idx3
    x.rdidx:=io.writeVecCtrl.bits.reg_idxw
    x.rd:=io.writeVecCtrl.bits.wb_wfd_rd
    x.rdwen:=false.B
    x.rdwmask:=io.writeVecCtrl.bits.wfd_mask
    //y.rdwen:=io.writeCtrl.wfd
  })
  scalarRegFile.foreach(y=>{
    y.rs1idx:=io.control.reg_idx1
    y.rs2idx:=io.control.reg_idx2
    y.rs3idx:=io.control.reg_idx3
    y.rdidx:=io.writeScalarCtrl.bits.reg_idxw
    y.rd:=io.writeScalarCtrl.bits.wb_wxd_rd
    y.rdwen:=false.B
    //y.rdwen:=io.writeCtrl.wxd
  })
  vectorRegFile(io.writeVecCtrl.bits.warp_id).rdwen:=io.writeVecCtrl.bits.wfd&io.writeVecCtrl.valid
  scalarRegFile(io.writeScalarCtrl.bits.warp_id).rdwen:=io.writeScalarCtrl.bits.wxd&io.writeScalarCtrl.valid
  io.writeScalarCtrl.ready:=true.B
  io.writeVecCtrl.ready:=true.B

  (0 until num_thread).foreach(x=>{
    io.alu_src1(x):=MuxLookup(io.control.sel_alu1,0.U,Array(A1_RS1->scalarRegFile(io.control.wid).rs1,A1_VRS1->(vectorRegFile(io.control.wid).rs1(x)),A1_IMM->imm.io.out,A1_PC->io.control.pc))//io.control.reg_idx1))
    io.alu_src2(x):=MuxLookup(io.control.sel_alu2,0.U,Array(A2_RS2->scalarRegFile(io.control.wid).rs2,A2_IMM->imm.io.out,A2_VRS2->vectorRegFile(io.control.wid).rs2(x),A2_SIZE->4.U))
    io.alu_src3(x):=MuxLookup(io.control.sel_alu3,0.U,Array(A3_PC->Mux(io.control.branch===B_R,(imm.io.out+scalarRegFile(io.control.wid).rs1),(io.control.pc+imm.io.out)),A3_VRS3->vectorRegFile(io.control.wid).rs3(x),A3_SD->Mux(io.control.isvec,vectorRegFile(io.control.wid).rs3(x),scalarRegFile(io.control.wid).rs2),A3_FRS3->(scalarRegFile(io.control.wid).rs3)))
    io.mask(x):=Mux(io.control.mask,vectorRegFile(io.control.wid).v0(0).apply(x),Mux(io.control.isvec,true.B,!x.asUInt.orR))

  })

}

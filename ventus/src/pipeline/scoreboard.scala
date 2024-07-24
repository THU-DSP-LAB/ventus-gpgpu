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
import chisel3.util.{Decoupled, DecoupledIO, MuxLookup, Queue, UIntToOH}
import top.parameters._
import IDecode._


class CtrlSigs extends Bundle {
  val inst = UInt(32.W)
  val wid = UInt(depth_warp.W)
  val fp = Bool()
  val branch = UInt(2.W)
  val simt_stack = Bool()
  val simt_stack_op = Bool()
  val barrier = Bool()
  val csr = UInt(2.W)
  val reverse = Bool()
  val sel_alu2 = UInt(2.W)
  val sel_alu1 = UInt(2.W)
  val isvec = Bool()
  val sel_alu3 = UInt(2.W)
  val mask=Bool()
  val sel_imm = UInt(4.W)
  val mem_whb = UInt(2.W)
  val mem_unsigned = Bool()
  val alu_fn = UInt(6.W)
  val force_rm_rtz = Bool()
  val is_vls12 = Bool()
  val mem = Bool()
  val mul = Bool()
  val tc = Bool()
  val disable_mask = Bool()
  val custom_signal_0 = Bool()
  val mem_cmd = UInt(2.W)
  val mop = UInt(2.W)
  val reg_idx1 = UInt((regidx_width + regext_width).W) // 8.W
  val reg_idx2 = UInt((regidx_width + regext_width).W)
  val reg_idx3 = UInt((regidx_width + regext_width).W)
  val reg_idxw = UInt((regidx_width + regext_width).W)
  val wvd = Bool()
  val fence = Bool()
  val sfu = Bool()
  val readmask = Bool()
  val writemask = Bool()
  val wxd = Bool()
  val pc=UInt(32.W)
  val imm_ext = UInt((1+6).W) // new! immext
  val spike_info=if(SPIKE_OUTPUT) Some(new InstWriteBack) else None
  val atomic= Bool()
  val aq = Bool()
  val rl = Bool()
  //override def cloneType: CtrlSigs.this.type = new CtrlSigs().asInstanceOf[this.type]
}
class scoreboardIO extends Bundle{
  val ibuffer_if_ctrl=Input(new CtrlSigs())
  val if_ctrl=Input(new CtrlSigs())
  val wb_v_ctrl=Input(new WriteVecCtrl())
  val wb_x_ctrl=Input(new WriteScalarCtrl())
  val if_fire=Input(Bool())
  val br_ctrl=Input(Bool())
  val fence_end=Input(Bool())
  val wb_v_fire=Input(Bool())
  val wb_x_fire=Input(Bool())
  val delay=Output(Bool())
  val op_colV_in_fire = Input(Bool())
  val op_colV_out_fire = Input(Bool())
  val op_colX_in_fire=Input(Bool())
  val op_colX_out_fire=Input(Bool())
}
class ScoreboardUtil(n: Int,zero:Boolean=false)
{
  def set(en: Bool, addr: UInt): Unit = update(en, _next.asUInt | mask(en, addr)) // set r(addr) = 1
  def clear(en: Bool, addr: UInt): Unit = update(en, _next.asUInt & (~mask(en, addr)).asUInt) //clear r(addr) = 0
  def read(addr: UInt): Bool = r(addr)
  def readBypassed(addr: UInt): Bool = _next(addr)
  private val _r = RegInit(0.U(n.W))
  private val r = if(zero) (_r >> 1 << 1) else _r
  private var _next = r
  private var ens = false.B
  private def mask(en: Bool, addr: UInt) = Mux(en, (1.U << addr).asUInt, 0.U)
  private def update(en: Bool, update: UInt) = {
    _next = update
    ens = ens || en
    when (ens) { _r := _next }
  }
}
class Scoreboard extends Module{
  val io=IO(new scoreboardIO())
  val vectorReg=new ScoreboardUtil(1 << (regidx_width + regext_width))
  val scalarReg=new ScoreboardUtil(1 << (regidx_width + regext_width),true)
  val beqReg=new ScoreboardUtil(1)
  val OpColRegV=new ScoreboardUtil(1)
  val OpColRegX=new ScoreboardUtil(1)
  val fenceReg=new ScoreboardUtil(1) // after LSU rebuild, this could be cancelled.
  // TODO: CSR operation may cause unexpected situation.
  vectorReg.set(io.if_fire & io.if_ctrl.wvd,io.if_ctrl.reg_idxw)
  vectorReg.clear(io.wb_v_fire & io.wb_v_ctrl.wvd,io.wb_v_ctrl.reg_idxw)
  scalarReg.set(io.if_fire & io.if_ctrl.wxd,io.if_ctrl.reg_idxw)
  scalarReg.clear(io.wb_x_fire & io.wb_x_ctrl.wxd,io.wb_x_ctrl.reg_idxw)
  beqReg.set(io.if_fire & ((io.if_ctrl.branch=/=0.U)|(io.if_ctrl.barrier)),0.U)
  beqReg.clear(io.br_ctrl,0.U)
  OpColRegV.set(io.op_colV_in_fire, 0.U)
  OpColRegV.clear(io.op_colV_out_fire, 0.U)
  OpColRegX.set(io.op_colX_in_fire, 0.U)
  OpColRegX.clear(io.op_colX_out_fire, 0.U)
  fenceReg.set(io.if_fire & io.if_ctrl.fence,0.U)
  fenceReg.clear(io.fence_end,0.U)
  val read1=MuxLookup(io.ibuffer_if_ctrl.sel_alu1,false.B)(Array(A1_RS1->scalarReg.read(io.ibuffer_if_ctrl.reg_idx1),A1_VRS1->vectorReg.read(io.ibuffer_if_ctrl.reg_idx1)))
  val read2=MuxLookup(io.ibuffer_if_ctrl.sel_alu2,false.B)(Array(A2_RS2->scalarReg.read(io.ibuffer_if_ctrl.reg_idx2),A2_VRS2->vectorReg.read(io.ibuffer_if_ctrl.reg_idx2)))
  val read3=MuxLookup(io.ibuffer_if_ctrl.sel_alu3,false.B)(Array(A3_VRS3->vectorReg.read(io.ibuffer_if_ctrl.reg_idx3),
    A3_SD->Mux(io.ibuffer_if_ctrl.isvec& (!io.ibuffer_if_ctrl.readmask),vectorReg.read(io.ibuffer_if_ctrl.reg_idx3),Mux(io.ibuffer_if_ctrl.isvec,vectorReg.read(io.ibuffer_if_ctrl.reg_idx2),scalarReg.read(io.ibuffer_if_ctrl.reg_idx2))),
    A3_FRS3->scalarReg.read(io.ibuffer_if_ctrl.reg_idx3),
    A3_PC-> Mux(io.ibuffer_if_ctrl.branch===B_R, scalarReg.read(io.ibuffer_if_ctrl.reg_idx1),false.B)
  ))
  val readm=Mux(io.ibuffer_if_ctrl.mask,vectorReg.read(0.U),false.B)
  val readw=Mux(io.ibuffer_if_ctrl.wxd,scalarReg.read(io.ibuffer_if_ctrl.reg_idxw),false.B)|Mux(io.ibuffer_if_ctrl.wvd,vectorReg.read(io.ibuffer_if_ctrl.reg_idxw),false.B)
  val readb=beqReg.read(0.U)
  val read_op_colV=OpColRegV.read(0.U)
  val read_op_colX=OpColRegX.read(0.U)
  val readf=io.ibuffer_if_ctrl.mem & fenceReg.read(0.U)
  io.delay:=read1|read2|read3|readm|readw|readb|readf|read_op_colV|read_op_colX
}
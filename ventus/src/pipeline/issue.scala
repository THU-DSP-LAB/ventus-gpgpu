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

class vExeData extends Bundle{
  val in1=Vec(num_thread,UInt(xLen.W))
  val in2=Vec(num_thread,UInt(xLen.W))
  val in3=Vec(num_thread,UInt(xLen.W))
  val mask=Vec(num_thread,Bool())
  val ctrl=new CtrlSigs()
}

class sExeData extends Bundle{
  val in1=UInt(xLen.W)
  val in2=UInt(xLen.W)
  val in3=UInt(xLen.W)
  val ctrl=new CtrlSigs()
}

class warpSchedulerExeData extends Bundle{
  val ctrl=new CtrlSigs()
}
class csrExeData extends Bundle{
  val ctrl=new CtrlSigs()
  val in1=UInt(xLen.W)
}

class Issue extends Module{
  val io = IO(new Bundle{
    val in=Flipped(DecoupledIO(new vExeData))
    val out_sALU=DecoupledIO(new sExeData)
    val out_vALU=DecoupledIO(new vExeData)
    val out_vFPU=DecoupledIO(new vExeData)
    val out_LSU=DecoupledIO(new vExeData)
    val out_SFU=DecoupledIO(new vExeData)
    val out_SIMT=DecoupledIO(new simtExeData)
    val out_warpscheduler=DecoupledIO(new warpSchedulerExeData())
    val out_CSR=DecoupledIO(new csrExeData())
    val out_MUL=DecoupledIO(new vExeData)
    val out_TC=DecoupledIO(new vExeData)
  })
  val inputBuf=Queue.apply(io.in,0)//Module(new Queue(new vExeData,entries = 1,pipe=true))


  io.out_sALU.bits.in1:=inputBuf.bits.in1(0)
  io.out_sALU.bits.in2:=inputBuf.bits.in2(0)
  io.out_sALU.bits.in3:=inputBuf.bits.in3(0)
  io.out_sALU.bits.ctrl:=inputBuf.bits.ctrl
  io.out_vALU.bits:=inputBuf.bits
  io.out_vFPU.bits:=inputBuf.bits
  io.out_MUL.bits:=inputBuf.bits
  io.out_SFU.bits:=inputBuf.bits
  io.out_LSU.bits:=inputBuf.bits
  io.out_TC.bits:=inputBuf.bits
  io.out_SIMT.bits.PC_branch:=inputBuf.bits.in3(0)
  io.out_SIMT.bits.PC_execute := inputBuf.bits.ctrl.pc
  //io.out_SIMT.bits.PC_reconv := inputBuf.bits.in1(0)
  io.out_SIMT.bits.wid:=inputBuf.bits.ctrl.wid
  io.out_SIMT.bits.opcode:=inputBuf.bits.ctrl.simt_stack_op
  io.out_SIMT.bits.mask_init:=inputBuf.bits.mask.asUInt
  if(SPIKE_OUTPUT) {
    io.out_SIMT.bits.spike_info.get:=inputBuf.bits.ctrl.spike_info.get

    when(io.out_warpscheduler.fire/*&&io.out_LSU.bits.ctrl.wid===wid_to_check.U*/){
      printf(p"sm ${io.out_LSU.bits.ctrl.spike_info.get.sm_id} warp ${Decimal(io.out_LSU.bits.ctrl.wid)} ")
      printf(p"0x${Hexadecimal(io.out_LSU.bits.ctrl.spike_info.get.pc)} 0x${Hexadecimal(io.out_LSU.bits.ctrl.spike_info.get.inst)}")
      when(io.out_warpscheduler.bits.ctrl.barrier & !io.out_warpscheduler.bits.ctrl.simt_stack_op){printf(p" barrier\n")}
      when(io.out_warpscheduler.bits.ctrl.simt_stack_op){printf(p" endprg\n")}
    }
  }
  io.out_warpscheduler.bits.ctrl:=inputBuf.bits.ctrl
  io.out_CSR.bits.ctrl:=inputBuf.bits.ctrl
  io.out_CSR.bits.in1:=inputBuf.bits.in1(0)

  io.out_TC.valid:=false.B
  io.out_sALU.valid:=false.B
  io.out_vALU.valid:=false.B
  io.out_SIMT.valid:=false.B
  io.out_LSU.valid:=false.B
  io.out_vFPU.valid:=false.B
  io.out_MUL.valid:=false.B
  io.out_warpscheduler.valid:=false.B
  io.out_CSR.valid:=false.B
  io.out_SFU.valid:=false.B
  inputBuf.ready:=false.B
  when(inputBuf.bits.ctrl.tc){
    io.out_TC.valid:=inputBuf.valid
    inputBuf.ready:=io.out_TC.ready
  }.elsewhen(inputBuf.bits.ctrl.sfu){
    io.out_SFU.valid:=inputBuf.valid
    inputBuf.ready:=io.out_SFU.ready
  }.elsewhen(inputBuf.bits.ctrl.fp){
    io.out_vFPU.valid:=inputBuf.valid
    inputBuf.ready:=io.out_vFPU.ready
  }.elsewhen(inputBuf.bits.ctrl.csr.orR()){
    io.out_CSR.valid:=inputBuf.valid
    inputBuf.ready:=io.out_CSR.ready
  }.elsewhen(inputBuf.bits.ctrl.mul){
    io.out_MUL.valid:=inputBuf.valid
    inputBuf.ready:=io.out_MUL.ready
  }
    .elsewhen(inputBuf.bits.ctrl.mem){
    //io.out_LSU<>inputBuf
    io.out_LSU.valid:=inputBuf.valid
    inputBuf.ready:=io.out_LSU.ready
  }.elsewhen(inputBuf.bits.ctrl.isvec){
    when(inputBuf.bits.ctrl.simt_stack){
      when(io.out_SIMT.bits.opcode === 0.U){
        val beqv_ready=io.out_SIMT.ready&io.out_vALU.ready
        io.out_vALU.valid:=inputBuf.valid & beqv_ready
        io.out_SIMT.valid:=inputBuf.valid & beqv_ready
        inputBuf.ready:=beqv_ready
      }.otherwise{
        io.out_SIMT.valid:=inputBuf.valid
        inputBuf.ready:=io.out_SIMT.ready
      }
    }.otherwise{
      io.out_vALU.valid:=inputBuf.valid
      inputBuf.ready:=io.out_vALU.ready
    }
  }.elsewhen(inputBuf.bits.ctrl.barrier){
    io.out_warpscheduler.valid:=inputBuf.valid
    inputBuf.ready:=io.out_warpscheduler.ready
  }.otherwise({
    io.out_sALU.valid:=inputBuf.valid
    inputBuf.ready:=io.out_sALU.ready
  })
  when(io.in.fire()&io.in.bits.ctrl.wid===0.U){
    //printf(p"wid=${io.in.bits.ctrl.wid},pc=0x${Hexadecimal(io.in.bits.ctrl.pc)},inst=0x${Hexadecimal(io.in.bits.ctrl.inst)}\n")
  }
}

// archive one to mux arbiter
class arbiter_o2m(numTarget:Int) extends Module {
  val io = IO(new Bundle {
    val in = Flipped(DecoupledIO(new Bool()))
    val out = (Vec(numTarget, DecoupledIO(new Bool())))
    val stall = Output(Bool())
  })
  io.stall := io.in.ready
  io.out.foreach(x=>{x.bits:=io.in.bits;x.valid:=false.B})

  for (i <- 0 until numTarget) {
  when(io.out(i).ready)
    {for (j<-0 until numTarget)
      {io.out(j).valid := false.B}
      io.out(i).valid:=true.B}
  }
  val ready=Wire(Vec(numTarget,Bool()))
  (0 until numTarget).foreach(x=>ready(x):=io.out(x).ready)

  io.in.ready:=ready.reduce(_ | _)

}

class XVDualIssue(num_buffer: Int) extends Module{
  val io = IO(new Bundle{
    val in = Flipped(Vec(num_buffer, Decoupled(Output(new vExeData))))
    val out_x = Decoupled(Output(new vExeData))
    val out_v = Decoupled(Output(new vExeData))
  })
  def inst_is_vec(in: vExeData): Bool = {
    val out = Wire(new Bool)
    // sALU | CSR | warpscheduler
    // vFPU | vSFU | vALU&SIMT | vMUL | vTC | LSU
    when(in.ctrl.tc || in.ctrl.fp || in.ctrl.mul || in.ctrl.sfu || in.ctrl.mem){
      out := true.B
    }.elsewhen(in.ctrl.csr.orR || in.ctrl.barrier){
      out := false.B
    }.elsewhen(in.ctrl.isvec){
      out := true.B
    }.otherwise{
      out := false.B
    }
    out
  }
  val arb_x = Module(new RRArbiter(new vExeData, num_buffer))
  val arb_v = Module(new RRArbiter(new vExeData, num_buffer))

  (0 until num_buffer).foreach{ i =>
    val in_isvec = inst_is_vec(io.in(i).bits)
    io.in(i).ready := Mux(in_isvec, arb_v.io.in(i).ready, arb_x.io.in(i).ready)

    arb_x.io.in(i).valid := io.in(i).valid && !in_isvec
    arb_x.io.in(i).bits := io.in(i).bits
    arb_v.io.in(i).valid := io.in(i).valid && in_isvec
    arb_v.io.in(i).bits := io.in(i).bits
  }
  io.out_x <> arb_x.io.out
  io.out_v <> arb_v.io.out
}

class IssueV2 extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Vec(num_issue,Decoupled(Output(new vExeData))))
    val out_sALU = DecoupledIO(new sExeData)
    val out_vALU = DecoupledIO(new vExeData)
    val out_vFPU = DecoupledIO(new vExeData)
    val out_LSU = DecoupledIO(new vExeData)
    val out_SFU = DecoupledIO(new vExeData)
    val out_SIMT = DecoupledIO(new simtExeData)
    val out_warpscheduler = DecoupledIO(new warpSchedulerExeData())
    val out_CSR = DecoupledIO(new csrExeData())
    val out_MUL = DecoupledIO(new vExeData)
    val out_TC = DecoupledIO(new vExeData)
  })
  class vALU_SIMT_Comb extends Bundle{
    val en = UInt(2.W) // high: SIMT, low: vALU
    val vALU = new vExeData
    val SIMT = new simtExeData
  }
  val arb_sALU = Module(new RRArbiter(new sExeData, num_issue))
  val arb_vALU = Module(new RRArbiter(new vALU_SIMT_Comb, num_issue))
  val arb_vFPU = Module(new RRArbiter(new vExeData, num_issue))
  val arb_LSU = Module(new RRArbiter(new vExeData, num_issue))
  val arb_SFU = Module(new RRArbiter(new vExeData, num_issue))
  val arb_warpscheduler = Module(new RRArbiter(new warpSchedulerExeData, num_issue))
  val arb_CSR = Module(new RRArbiter(new csrExeData, num_issue))
  val arb_MUL = Module(new RRArbiter(new vExeData, num_issue))
  val arb_TC = Module(new RRArbiter(new vExeData, num_issue))

  val inputBuf = io.in.map{Queue.apply(_, 0)}
  (0 until num_issue).foreach{ i =>
    arb_sALU.io.in(i).valid := false.B
    arb_vALU.io.in(i).valid := false.B
    arb_vFPU.io.in(i).valid := false.B
    arb_LSU.io.in(i).valid := false.B
    arb_SFU.io.in(i).valid := false.B
    arb_warpscheduler.io.in(i).valid := false.B
    arb_CSR.io.in(i).valid := false.B
    arb_MUL.io.in(i).valid := false.B
    arb_TC.io.in(i).valid := false.B
    when(inputBuf(i).deq.ctrl.tc){  // TC
      arb_TC.io.in(i).valid := inputBuf(i).valid
      inputBuf(i).ready := arb_TC.io.in(i).ready
    }.elsewhen(inputBuf(i).deq.ctrl.sfu){ // SFU
      arb_SFU.io.in(i).valid := inputBuf(i).valid
      inputBuf(i).ready := arb_SFU.io.in(i).ready
    }.elsewhen(inputBuf(i).bits.ctrl.fp) {  // vFP
      arb_vFPU.io.in(i).valid := inputBuf(i).valid
      inputBuf(i).ready := arb_vFPU.io.in(i).ready
    }.elsewhen(inputBuf(i).bits.ctrl.csr.orR) { // CSR
      arb_CSR.io.in(i).valid := inputBuf(i).valid
      inputBuf(i).ready := arb_CSR.io.in(i).ready
    }.elsewhen(inputBuf(i).bits.ctrl.mul) { // vMUL
      arb_MUL.io.in(i).valid := inputBuf(i).valid
      inputBuf(i).ready := arb_MUL.io.in(i).ready
    }.elsewhen(inputBuf(i).bits.ctrl.mem){ // LSU
      arb_LSU.io.in(i).valid := inputBuf(i).valid
      inputBuf(i).ready := arb_LSU.io.in(i).ready
    }.elsewhen(inputBuf(i).bits.ctrl.isvec){ // vALU or SIMT
      arb_vALU.io.in(i).valid := inputBuf(i).valid
      inputBuf(i).ready := arb_vALU.io.in(i).ready
    }.elsewhen(inputBuf(i).bits.ctrl.barrier){ // warp_scheduler
      arb_warpscheduler.io.in(i).valid := inputBuf(i).valid
      inputBuf(i).ready := arb_warpscheduler.io.in(i).ready
    }.otherwise{ // sALU
      arb_sALU.io.in(i).valid := inputBuf(i).valid
      inputBuf(i).ready := arb_sALU.io.in(i).ready
    }

    arb_TC.io.in(i).bits := inputBuf(i).bits
    arb_SFU.io.in(i).bits := inputBuf(i).bits
    arb_vFPU.io.in(i).bits := inputBuf(i).bits
    arb_MUL.io.in(i).bits := inputBuf(i).bits
    arb_LSU.io.in(i).bits := inputBuf(i).bits
    arb_CSR.io.in(i).bits.ctrl := inputBuf(i).bits.ctrl
    arb_CSR.io.in(i).bits.in1 := inputBuf(i).bits.in1(0)
    arb_warpscheduler.io.in(i).bits.ctrl := inputBuf(i).bits.ctrl
    arb_sALU.io.in(i).bits.in1 := inputBuf(i).bits.in1(0)
    arb_sALU.io.in(i).bits.in2 := inputBuf(i).bits.in2(0)
    arb_sALU.io.in(i).bits.in3 := inputBuf(i).bits.in3(0)
    arb_sALU.io.in(i).bits.ctrl := inputBuf(i).bits.ctrl

    arb_vALU.io.in(i).bits.vALU := inputBuf(i).bits
    arb_vALU.io.in(i).bits.SIMT.PC_branch := inputBuf(i).bits.in3(0)
    arb_vALU.io.in(i).bits.SIMT.wid := inputBuf(i).bits.ctrl.wid
    arb_vALU.io.in(i).bits.SIMT.opcode := inputBuf(i).bits.ctrl.simt_stack_op
    arb_vALU.io.in(i).bits.SIMT.mask_init := inputBuf(i).bits.mask.asUInt
    when(inputBuf(i).bits.ctrl.simt_stack){
      when(inputBuf(i).bits.ctrl.simt_stack_op === 0.U){ // SIMT & vALU
        arb_vALU.io.in(i).bits.en := "b11".U(2.W)
      }.otherwise{ // SIMT Only
        arb_vALU.io.in(i).bits.en := "b10".U(2.W)
      }
    }.otherwise{ // vALU Only
      arb_vALU.io.in(i).bits.en := "b01".U(2.W)
    }
  }
  io.out_TC <> arb_TC.io.out
  io.out_SFU <> arb_SFU.io.out
  io.out_vFPU <> arb_vFPU.io.out
  io.out_MUL <> arb_MUL.io.out
  io.out_LSU <> arb_LSU.io.out
  io.out_CSR <> arb_CSR.io.out
  io.out_warpscheduler <> arb_warpscheduler.io.out
  io.out_sALU <> arb_sALU.io.out

  io.out_vALU.valid := arb_vALU.io.out.valid && arb_vALU.io.out.bits.en(0)
  io.out_vALU.bits := arb_vALU.io.out.bits.vALU
  io.out_SIMT.valid := arb_vALU.io.out.valid && arb_vALU.io.out.bits.en(1)
  io.out_SIMT.bits := arb_vALU.io.out.bits.SIMT
  arb_vALU.io.out.ready := io.out_vALU.ready && io.out_SIMT.ready
}
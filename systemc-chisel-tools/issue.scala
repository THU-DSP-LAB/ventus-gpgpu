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
  io.out_SIMT.bits.PC_branch:=inputBuf.bits.in3(0)
  io.out_SIMT.bits.wid:=inputBuf.bits.ctrl.wid
  io.out_SIMT.bits.opcode:=inputBuf.bits.ctrl.simt_stack_op
  io.out_SIMT.bits.mask_init:=inputBuf.bits.mask.asUInt()

  io.out_warpscheduler.bits.ctrl:=inputBuf.bits.ctrl
  io.out_CSR.bits.ctrl:=inputBuf.bits.ctrl
  io.out_CSR.bits.in1:=inputBuf.bits.in1(0)

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
  when(inputBuf.bits.ctrl.sfu){
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
      inputBuf.ready:=beqv_ready}
        .otherwise({
        io.out_SIMT.valid:=inputBuf.valid
        inputBuf.ready:=io.out_SIMT.ready
      })
    }.otherwise({
      io.out_vALU.valid:=inputBuf.valid
      inputBuf.ready:=io.out_vALU.ready
    })
  }.elsewhen(inputBuf.bits.ctrl.barrier){
    io.out_warpscheduler.valid:=inputBuf.valid
    inputBuf.ready:=io.out_warpscheduler.ready
  }.otherwise({
    io.out_sALU.valid:=inputBuf.valid
    inputBuf.ready:=io.out_sALU.ready
  })
  when(io.in.fire&io.in.bits.ctrl.wid===0.U){
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

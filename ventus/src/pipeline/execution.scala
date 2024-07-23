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

import FPUv2.TensorCoreFP32
import FPUv2.utils.{EmptyFPUCtrl, TestFPUCtrl}
import chisel3._
import chisel3.util._
import top.parameters._
import IDecode._

class BranchCtrl extends Bundle{
  val wid=UInt(depth_warp.W)
  val jump=Bool()
  val new_pc=UInt(32.W)
  val spike_info = if (SPIKE_OUTPUT) Some(new InstWriteBack) else None
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
  if(SPIKE_OUTPUT){
    result.io.enq.bits.spike_info.get:=io.in.bits.ctrl.spike_info.get
    result_br.io.enq.bits.spike_info.get:=io.in.bits.ctrl.spike_info.get
  }
  io.in.ready:=MuxLookup(io.in.bits.ctrl.branch,result_br.io.enq.ready&result.io.enq.ready)(Seq(B_B->result_br.io.enq.ready,B_N->result.io.enq.ready))

  result_br.io.enq.bits.wid:=io.in.bits.ctrl.wid
  result_br.io.enq.bits.new_pc:=io.in.bits.in3
  result_br.io.enq.bits.jump:=MuxLookup(io.in.bits.ctrl.branch,false.B)(Seq(B_B->alu.io.cmp_out,B_J->true.B,B_R->true.B))

  result_br.io.enq.valid:=io.in.valid&(io.in.bits.ctrl.branch=/=B_N)
  result.io.enq.valid:=io.in.valid&io.in.bits.ctrl.wxd
}
class vMULexe extends Module{
  val io = IO(new Bundle {
    val in = Flipped(DecoupledIO(new vExeData()))
    val out_x = DecoupledIO(new WriteScalarCtrl())
    val out_v = DecoupledIO(new WriteVecCtrl)
  })
  val mul=VecInit(Seq.fill(num_thread)(Module(new ArrayMultiplier(num_thread, xLen)).io))
  val result_x=Module(new Queue(new WriteScalarCtrl,1,pipe=true))
  val result_v=Module(new Queue(new WriteVecCtrl,1,pipe=true))
  (0 until num_thread).foreach(x=>{
    mul(x).in.bits.a := io.in.bits.in1(x)
    mul(x).in.bits.b := io.in.bits.in2(x)
    mul(x).in.bits.c := io.in.bits.in3(x)
    mul(x).in.bits.ctrl := io.in.bits.ctrl
    mul(x).in.bits.mask := io.in.bits.mask
    mul(x).in.valid:=io.in.valid
    result_v.io.enq.bits.wb_wvd_rd(x) := mul(x).out.bits.result
    mul(x).out.ready:=Mux(mul(x).out.bits.ctrl.wxd,result_x.io.enq.ready,result_v.io.enq.ready)
    when(io.in.bits.ctrl.reverse){
      mul(x).in.bits.a:=io.in.bits.in2(x)
      mul(x).in.bits.b:=io.in.bits.in1(x)
    }
  })


  result_v.io.enq.bits.warp_id:=mul(0).out.bits.ctrl.wid
  result_v.io.enq.bits.reg_idxw:=mul(0).out.bits.ctrl.reg_idxw
  result_v.io.enq.bits.wvd:=mul(0).out.bits.ctrl.wvd
  result_v.io.enq.bits.wvd_mask:=mul(0).out.bits.mask
  if (SPIKE_OUTPUT) {
    result_v.io.enq.bits.spike_info.get := mul(0).out.bits.ctrl.spike_info.get
    result_x.io.enq.bits.spike_info.get := mul(0).out.bits.ctrl.spike_info.get
  }
  result_x.io.enq.bits.warp_id:=mul(0).out.bits.ctrl.wid
  result_x.io.enq.bits.reg_idxw:=mul(0).out.bits.ctrl.reg_idxw
  result_x.io.enq.bits.wxd:=mul(0).out.bits.ctrl.wxd
  result_x.io.enq.bits.wb_wxd_rd:=mul(0).out.bits.result

  result_v.io.enq.valid:=mul(0).out.valid&mul(0).out.bits.ctrl.wvd
  result_x.io.enq.valid:=mul(0).out.valid&mul(0).out.bits.ctrl.wxd
  io.in.ready:=mul(0).in.ready//Mux(io.in.bits.ctrl.wfd,result_v.io.enq.ready,Mux(io.in.bits.ctrl.wxd,result_x.io.enq.ready,true.B))
  io.out_v<>result_v.io.deq
  io.out_x<>result_x.io.deq

}

class TCCtrlv2(xLen: Int, depth_warp: Int) extends FPUv2.TCCtrl(xLen, depth_warp){
  val spike_info=if(SPIKE_OUTPUT) Some(new InstWriteBack) else None
}
class vTCexe extends Module{
  val io = IO(new Bundle {
    val in = Flipped(DecoupledIO(new vExeData()))
    val rm = Input(UInt(3.W))
    //val out_x = DecoupledIO(new WriteScalarCtrl())
    val out_v = DecoupledIO(new WriteVecCtrl)
  })
  val tensor = Module(new TensorCoreFP32(num_thread, tc_dim(0), tc_dim(1), tc_dim(2), new TCCtrlv2(xLen, depth_warp)))

  val result_v=Module(new Queue(new WriteVecCtrl,1,pipe=true))

  if(SPIKE_OUTPUT){
    val tcctrl_i = Wire(new TCCtrlv2(xLen, depth_warp))
    tcctrl_i.spike_info.get := io.in.bits.ctrl.spike_info.get
    tcctrl_i.reg_idxw := io.in.bits.ctrl.reg_idxw
    tcctrl_i.warpID := io.in.bits.ctrl.wid
    tensor.io.in.bits.ctrl := tcctrl_i
  }
  tensor.io.in.bits.ctrl.reg_idxw:=io.in.bits.ctrl.reg_idxw
  tensor.io.in.bits.ctrl.warpID:=io.in.bits.ctrl.wid
  tensor.io.in.valid:=io.in.valid
  io.in.ready:=tensor.io.in.ready
  (0 until num_thread).foreach(x=>{
    tensor.io.in.bits.data(x).ctrl.foreach{ _ :=0.U.asTypeOf(EmptyFPUCtrl())}
    tensor.io.in.bits.data(x).op:=0.U
    tensor.io.in.bits.data(x).rm:=io.rm
    tensor.io.in.bits.data(x).a:=io.in.bits.in1(x)
    tensor.io.in.bits.data(x).b:=io.in.bits.in2(x)
    tensor.io.in.bits.data(x).c:=io.in.bits.in3(x)
    result_v.io.enq.bits.wb_wvd_rd(x):=tensor.io.out.bits.data(x).result
  })

  result_v.io.enq.bits.warp_id:=tensor.io.out.bits.ctrl.warpID
  result_v.io.enq.bits.reg_idxw:=tensor.io.out.bits.ctrl.reg_idxw
  result_v.io.enq.bits.wvd:=tensor.io.out.valid
  result_v.io.enq.bits.wvd_mask.foreach(_:=true.B)
  result_v.io.enq.valid:=tensor.io.out.valid
  tensor.io.out.ready:=result_v.io.enq.ready

  if (SPIKE_OUTPUT) {
    val tcctrl_o = Wire(new TCCtrlv2(xLen, depth_warp))
    tcctrl_o := tensor.io.out.bits.ctrl
    result_v.io.enq.bits.spike_info.get := tcctrl_o.spike_info.get
  }

  io.out_v<>result_v.io.deq

}

class vMULv2(softThread: Int = num_thread, hardThread: Int = num_thread) extends Module {
  assert(softThread % hardThread == 0)
  class vExeData2(num_thread: Int = softThread) extends vExeData {
    override val in1 = Vec(num_thread, UInt(xLen.W))
    override val in2 = Vec(num_thread, UInt(xLen.W))
    override val in3 = Vec(num_thread, UInt(xLen.W))
    override val mask = Vec(num_thread, Bool())
  }
  class WriteVecCtrl2(num_thread: Int = softThread) extends WriteVecCtrl {
    override val wb_wvd_rd = Vec(num_thread, UInt(xLen.W))
    override val wvd_mask = Vec(num_thread, Bool())
  }
//  class ArrayMultiplier2(num_thread: Int = softThread) extends ArrayMultiplier(xLen) {
//    override val io = IO(new Bundle {
//      val in = Flipped(DecoupledIO(new MULin(num_thread)))
//      val out = DecoupledIO(new MULout(num_thread))
//    })
//  }

  val io = IO(new Bundle{
    val in = Flipped(DecoupledIO(new vExeData2))
    val out_x = DecoupledIO(new WriteScalarCtrl)
    val out_v = DecoupledIO(new WriteVecCtrl2)
  })
  val mul = VecInit(Seq.fill(hardThread)(Module(new ArrayMultiplier(softThread, xLen)).io))
  val result_x = Module(new Queue(new WriteScalarCtrl, 1, pipe = true))
  val result_v = Module(new Queue(new WriteVecCtrl2, 1, pipe = true))

  if(softThread == hardThread){
    (0 until num_thread).foreach(x => {
      mul(x).in.bits.a := io.in.bits.in1(x)
      mul(x).in.bits.b := io.in.bits.in2(x)
      mul(x).in.bits.c := io.in.bits.in3(x)
      mul(x).in.bits.ctrl := io.in.bits.ctrl
      mul(x).in.bits.mask := io.in.bits.mask
      mul(x).in.valid := io.in.valid
      result_v.io.enq.bits.wb_wvd_rd(x) := mul(x).out.bits.result
      mul(x).out.ready := Mux(mul(x).out.bits.ctrl.wxd, result_x.io.enq.ready, result_v.io.enq.ready)
      when(io.in.bits.ctrl.reverse) {
        mul(x).in.bits.a := io.in.bits.in2(x)
        mul(x).in.bits.b := io.in.bits.in1(x)
      }
    })
    result_v.io.enq.bits.warp_id := mul(0).out.bits.ctrl.wid
    result_v.io.enq.bits.reg_idxw := mul(0).out.bits.ctrl.reg_idxw
    result_v.io.enq.bits.wvd := mul(0).out.bits.ctrl.wvd
    result_v.io.enq.bits.wvd_mask := mul(0).out.bits.mask

    result_x.io.enq.bits.warp_id := mul(0).out.bits.ctrl.wid
    result_x.io.enq.bits.reg_idxw := mul(0).out.bits.ctrl.reg_idxw
    result_x.io.enq.bits.wxd := mul(0).out.bits.ctrl.wxd
    result_x.io.enq.bits.wb_wxd_rd := mul(0).out.bits.result

    if (SPIKE_OUTPUT) {
      result_v.io.enq.bits.spike_info.get := mul(0).out.bits.ctrl.spike_info.get
      result_x.io.enq.bits.spike_info.get := mul(0).out.bits.ctrl.spike_info.get
    }

    result_v.io.enq.valid := mul(0).out.valid & mul(0).out.bits.ctrl.wvd
    result_x.io.enq.valid := mul(0).out.valid & mul(0).out.bits.ctrl.wxd
    io.in.ready := mul(0).in.ready
  }
  else{
    val maxIter = softThread / hardThread

    val inReg = Reg(new vExeData2)
    val hardResult = VecInit.fill(softThread)(0.U(xLen.W))
    val resultReg = Reg(new WriteVecCtrl2)
    val outFIFOReady = Mux(resultReg.wvd, result_v.io.enq.ready, result_x.io.enq.ready)

    val sendNS = WireInit(0.U(log2Ceil(maxIter+1).W))
    val sendCS = RegNext(sendNS)
    val send = Wire(Decoupled(UInt(0.W)))
    send.bits := DontCare

    io.in.ready := sendCS === 0.U || (sendCS === maxIter.U && send.ready)

    switch(sendCS){
      is(0.U){
        when(io.in.fire){
          when(io.in.bits.ctrl.wvd){
            sendNS := 1.U
          }.elsewhen(io.in.bits.ctrl.wxd){
            sendNS := maxIter.U
          }.otherwise{
            sendNS := 0.U
          }
        }
      }
      is((1 until maxIter).map{_.U}){
        when(send.fire){
          sendNS := sendCS +% 1.U
        }.otherwise{
          sendNS := sendCS
        }
      }
      is(maxIter.U){
        when(send.fire){
          when(io.in.fire){
            when(io.in.bits.ctrl.wvd){
              sendNS := 1.U
            }.elsewhen(io.in.bits.ctrl.wxd) {
              sendNS := maxIter.U
            }.otherwise{
              sendNS := 0.U
            }
          }.otherwise{
            sendNS := 0.U
          }
        }.otherwise{
          sendNS := sendCS
        }
      }
    }

    switch(sendNS){
      is(0.U){
        inReg := 0.U.asTypeOf(inReg)
      }
      is(1.U){
        when(io.in.fire){
          inReg := io.in.bits
        }
      }
      is((2 to maxIter).map{_.U}){
        when(send.fire){
          (0 until softThread).foreach{ i =>
            if(i + hardThread < softThread){
              inReg.in1(i) := inReg.in1(i + hardThread)
              inReg.in2(i) := inReg.in2(i + hardThread)
              inReg.in3(i) := inReg.in3(i + hardThread)
            }
            else{
              inReg.in1(i) := 0.U
              inReg.in2(i) := 0.U
              inReg.in3(i) := 0.U
            }
          }
        }
        when(io.in.fire){ // won't trigger if sendCS===maxIter-1 (since io.in.ready===false)
          inReg := io.in.bits
        }
      }
    }
    send.valid := sendCS =/= 0.U
    send.ready := mul(0).in.ready

    (0 until hardThread).foreach{ x =>
      mul(x).in.bits.a := inReg.in1(x)
      mul(x).in.bits.b := inReg.in2(x)
      mul(x).in.bits.c := inReg.in3(x)
      mul(x).in.bits.ctrl := inReg.ctrl
      mul(x).in.bits.mask := inReg.mask
      mul(x).in.valid := send.valid
      when(inReg.ctrl.reverse) {
        mul(x).in.bits.a := inReg.in2(x)
        mul(x).in.bits.b := inReg.in1(x)
      }
    }

    val recvNS = WireInit(0.U(log2Ceil(maxIter+1).W))
    val recvCS = RegNext(recvNS)
    val recv = Wire(Decoupled(UInt(0.W)))
    recv.bits := DontCare

    switch(recvCS){
      is(0.U){
        when(recv.fire){
          when(mul(0).out.bits.ctrl.wxd){
            recvNS := maxIter.U
          }.elsewhen(mul(0).out.bits.ctrl.wvd){
            recvNS := 1.U
          }.otherwise{
            recvNS := 0.U
          }
        }.otherwise{
          recvNS := 0.U
        }
      }
      is((1 until maxIter).map{_.U}){
        when(recv.fire){
          recvNS := recvCS +% 1.U
        }.otherwise{
          recvNS := 0.U
        }
      }
      is(maxIter.U){
        when(outFIFOReady){
          when(recv.fire){
            when(mul(0).out.bits.ctrl.wxd){
              recvNS := maxIter.U
            }.elsewhen(mul(0).out.bits.ctrl.wvd){
              recvNS := 1.U
            }.otherwise {
              recvNS := 0.U
            }
          }.otherwise{
            recvNS := 0.U
          }
        }.otherwise{
          recvNS := recvCS
        }
      }
    }

    switch(recvNS){
      is(0.U){
        resultReg := 0.U.asTypeOf(resultReg)
      }
      is((1 to maxIter).map{_.U}){
        when(recv.fire){
          (0 until softThread).foreach{ i =>
            if(i + hardThread < softThread){
              resultReg.wb_wvd_rd(i) := resultReg.wb_wvd_rd(i + hardThread)
            }
            else{
              resultReg.wb_wvd_rd(i) := hardResult(i + hardThread - softThread)
            }
          }
          when(recvNS===maxIter.U){
            resultReg.wvd_mask := mul(0).out.bits.mask
            resultReg.reg_idxw := mul(0).out.bits.ctrl.reg_idxw
            resultReg.warp_id := mul(0).out.bits.ctrl.wid
            resultReg.wvd := mul(0).out.bits.ctrl.wvd
          }
        }
      }
    }
    recv.ready := recvCS =/= maxIter.U || (recvCS===maxIter.U && outFIFOReady)
    recv.valid := mul(0).out.valid
    (0 until hardThread).foreach{ x =>
      mul(x).out.ready := recv.ready
      hardResult(x) := mul(x).out.bits.result
    }
    result_v.io.enq.bits := resultReg
    result_v.io.enq.valid := recvCS === maxIter.U && resultReg.wvd
    result_x.io.enq.bits.wb_wxd_rd := resultReg.wb_wvd_rd(softThread-hardThread)
    result_x.io.enq.bits.wxd := !resultReg.wvd
    result_x.io.enq.bits.warp_id := resultReg.warp_id
    result_x.io.enq.bits.reg_idxw := resultReg.reg_idxw
    result_x.io.enq.valid := recvCS === maxIter.U && !resultReg.wvd

    if (SPIKE_OUTPUT) {
      result_v.io.enq.bits.spike_info.get := mul(0).out.bits.ctrl.spike_info.get
      result_x.io.enq.bits.spike_info.get := mul(0).out.bits.ctrl.spike_info.get
    }
  }
  io.out_v <> result_v.io.deq
  io.out_x <> result_x.io.deq
}
class vALUexe extends Module{
  val io = IO(new Bundle {
    val in = Flipped(DecoupledIO(new vExeData()))
    val out = DecoupledIO(new WriteVecCtrl())
    val out2simt_stack = DecoupledIO(new vec_alu_bus())
  })
  val alu=VecInit(Seq.fill(num_thread)((Module(new ScalarALU())).io))
  val result=Module(new Queue(new WriteVecCtrl,1,pipe=true))
  val result2simt=Module(new Queue(new vec_alu_bus,1,pipe=true))
  (0 until num_thread).foreach(x=>{
    alu(x).in1:=io.in.bits.in1(x)
    alu(x).in2:=io.in.bits.in2(x)
    alu(x).in3:=io.in.bits.in3(x)
    alu(x).func:=io.in.bits.ctrl.alu_fn(4,0)
    result.io.enq.bits.wb_wvd_rd(x):=alu(x).out
    when(io.in.bits.ctrl.reverse){
      alu(x).in1:=io.in.bits.in2(x)
      alu(x).in2:=io.in.bits.in1(x)
    }
    when((io.in.bits.ctrl.alu_fn===FN_VMANDNOT)|(io.in.bits.ctrl.alu_fn===FN_VMORNOT)|(io.in.bits.ctrl.alu_fn===FN_VMNAND)|(io.in.bits.ctrl.alu_fn===FN_VMNOR)|(io.in.bits.ctrl.alu_fn===FN_VMXNOR)){
      when((io.in.bits.ctrl.alu_fn===FN_VMANDNOT)|(io.in.bits.ctrl.alu_fn===FN_VMORNOT)){
        alu(x).in1:=(~io.in.bits.in1(x))
        alu(x).func:=Cat(3.U(4.W),io.in.bits.ctrl.alu_fn(0))
      }.otherwise({
        when(io.in.bits.ctrl.alu_fn===FN_VMXNOR){alu(x).func:=FN_XOR(4,0)}
          .otherwise(alu(x).func:=Cat(3.U(4.W),io.in.bits.ctrl.alu_fn(0)))
        result.io.enq.bits.wb_wvd_rd(x):=(~alu(x).out)
      })
    }
    when(io.in.bits.ctrl.alu_fn===FN_VID){
      result.io.enq.bits.wb_wvd_rd(x):=x.asUInt
    }
    when(io.in.bits.ctrl.alu_fn===FN_VMERGE){
      result.io.enq.bits.wb_wvd_rd(x):=Mux(io.in.bits.mask(x),io.in.bits.in1(x),io.in.bits.in2(x))
      result.io.enq.bits.wvd_mask(x):=true.B
    }
  })
  when(io.in.bits.ctrl.writemask){
    result.io.enq.bits.wb_wvd_rd(0):=Mux(io.in.bits.ctrl.readmask,alu(0).out,VecInit((0 until num_thread).map(x=>{Mux(io.in.bits.mask(x),alu(x).out(0),0.U)})).asUInt)
    result.io.enq.bits.wvd_mask(0):=1.U
    when((io.in.bits.ctrl.alu_fn===FN_VMNAND)|(io.in.bits.ctrl.alu_fn===FN_VMNOR)|(io.in.bits.ctrl.alu_fn===FN_VMXNOR)){
      result.io.enq.bits.wb_wvd_rd(0):=VecInit((0 until num_thread).map(x=>{Mux(io.in.bits.mask(x),!alu(x).out(0),false.B)})).asUInt
    }
  }

  result.io.enq.bits.warp_id:=io.in.bits.ctrl.wid
  result.io.enq.bits.reg_idxw:=io.in.bits.ctrl.reg_idxw
  result.io.enq.bits.wvd:=io.in.bits.ctrl.wvd
  result.io.enq.bits.wvd_mask:=io.in.bits.mask
  result.io.enq.valid:=io.in.valid&io.in.bits.ctrl.wvd&(!io.in.bits.ctrl.simt_stack)
  if (SPIKE_OUTPUT) {
    result.io.enq.bits.spike_info.get := io.in.bits.ctrl.spike_info.get
  }
  result2simt.io.enq.bits.wid:=io.in.bits.ctrl.wid
  result2simt.io.enq.bits.if_mask:= ~(VecInit(alu.map({x=>x.cmp_out})).asUInt)
  result2simt.io.enq.valid:=io.in.valid&io.in.bits.ctrl.simt_stack

  io.in.ready:=Mux(io.in.bits.ctrl.simt_stack,result2simt.io.enq.ready,result.io.enq.ready)
  io.out<>result.io.deq
  io.out2simt_stack<>result2simt.io.deq
}

class vALUv2(softThread: Int = num_thread, hardThread: Int = num_thread) extends Module {
  assert(softThread % hardThread == 0)
  //assert(softThread > 2 && hardThread > 1)

  class vExeData2(num_thread: Int = softThread) extends vExeData{
    override val in1 = Vec(num_thread, UInt(xLen.W))
    override val in2 = Vec(num_thread, UInt(xLen.W))
    override val in3 = Vec(num_thread, UInt(xLen.W))
    override val mask = Vec(num_thread, Bool())
  }

  class WriteVecCtrl2(num_thread: Int = softThread) extends WriteVecCtrl{
    override val wb_wvd_rd = Vec(num_thread, UInt(xLen.W))
    override val wvd_mask = Vec(num_thread, Bool())
  }

  class vec_alu_bus2(num_thread: Int = softThread) extends vec_alu_bus {
    override val if_mask = UInt(num_thread.W)
  }

  val io = IO(new Bundle {
    val in = Flipped(DecoupledIO(new vExeData2()))
    val out = DecoupledIO(new WriteVecCtrl2())
    val out2simt_stack = DecoupledIO(new vec_alu_bus2())
  })

  val alu=VecInit(Seq.fill(hardThread)((Module(new ScalarALU())).io))

  val result = Module(new Queue(new WriteVecCtrl2, 1, pipe = true))
  val result2simt = Module(new Queue(new vec_alu_bus2, 1, pipe = true))
  //==========================================
  if(softThread == hardThread){
    (0 until num_thread).foreach(x => {
      alu(x).in1 := io.in.bits.in1(x)
      alu(x).in2 := io.in.bits.in2(x)
      alu(x).in3 := io.in.bits.in3(x)
      alu(x).func := io.in.bits.ctrl.alu_fn(4, 0)
      result.io.enq.bits.wb_wvd_rd(x) := alu(x).out
      when(io.in.bits.ctrl.reverse) {
        alu(x).in1 := io.in.bits.in2(x)
        alu(x).in2 := io.in.bits.in1(x)
      }
      when((io.in.bits.ctrl.alu_fn === FN_VMANDNOT) | (io.in.bits.ctrl.alu_fn === FN_VMORNOT) | (io.in.bits.ctrl.alu_fn === FN_VMNAND) | (io.in.bits.ctrl.alu_fn === FN_VMNOR) | (io.in.bits.ctrl.alu_fn === FN_VMXNOR)) {
        when((io.in.bits.ctrl.alu_fn === FN_VMANDNOT) | (io.in.bits.ctrl.alu_fn === FN_VMORNOT)) {
          alu(x).in1 := (~io.in.bits.in1(x))
          alu(x).func := Cat(3.U(4.W), io.in.bits.ctrl.alu_fn(0))
        }.otherwise({
          when(io.in.bits.ctrl.alu_fn === FN_VMXNOR) {
            alu(x).func := FN_XOR(4, 0)
          }
            .otherwise(alu(x).func := Cat(3.U(4.W), io.in.bits.ctrl.alu_fn(0)))
          result.io.enq.bits.wb_wvd_rd(x) := (~alu(x).out)
        })
      }
      when(io.in.bits.ctrl.alu_fn === FN_VID) {
        result.io.enq.bits.wb_wvd_rd(x) := x.asUInt
      }
      when(io.in.bits.ctrl.alu_fn === FN_VMERGE) {
        result.io.enq.bits.wb_wvd_rd(x) := Mux(io.in.bits.mask(x), io.in.bits.in1(x), io.in.bits.in2(x))
        result.io.enq.bits.wvd_mask(x) := true.B
      }
    })
    when(io.in.bits.ctrl.writemask) {
      result.io.enq.bits.wb_wvd_rd(0) := Mux(io.in.bits.ctrl.readmask, alu(0).out, VecInit((0 until num_thread).map(x => {
        Mux(io.in.bits.mask(x), alu(x).out(0), 0.U)
      })).asUInt)
      result.io.enq.bits.wvd_mask(0) := 1.U
      when((io.in.bits.ctrl.alu_fn === FN_VMNAND) | (io.in.bits.ctrl.alu_fn === FN_VMNOR) | (io.in.bits.ctrl.alu_fn === FN_VMXNOR)) {
        result.io.enq.bits.wb_wvd_rd(0) := VecInit((0 until num_thread).map(x => {
          Mux(io.in.bits.mask(x), !alu(x).out(0), false.B)
        })).asUInt
      }
    }

    result.io.enq.bits.warp_id := io.in.bits.ctrl.wid
    result.io.enq.bits.reg_idxw := io.in.bits.ctrl.reg_idxw
    result.io.enq.bits.wvd := io.in.bits.ctrl.wvd
    result.io.enq.bits.wvd_mask := io.in.bits.mask
    result.io.enq.valid := io.in.valid & io.in.bits.ctrl.wvd & (!io.in.bits.ctrl.simt_stack)

    result2simt.io.enq.bits.wid := io.in.bits.ctrl.wid
    result2simt.io.enq.bits.if_mask := ~(VecInit(alu.map({ x => x.cmp_out })).asUInt)
    result2simt.io.enq.valid := io.in.valid & io.in.bits.ctrl.simt_stack

    if(SPIKE_OUTPUT){
      result.io.enq.bits.spike_info.foreach{ _ := io.in.bits.ctrl.spike_info.get }
    }

    io.in.ready := Mux(io.in.bits.ctrl.simt_stack, result2simt.io.enq.ready, result.io.enq.ready)
  }
  //==========================================
  else{
    val maxIter = softThread / hardThread

    val inReg = Reg(new vExeData2)
    val hardResult = VecInit.fill(softThread)(0.U(xLen.W))
    val resultReg = Reg(new WriteVecCtrl2)
    val simtReg = Reg(new vec_alu_bus2)
    val outFIFOReady = Mux(inReg.ctrl.simt_stack, result2simt.io.enq.ready, result.io.enq.ready)

    val sendNS = WireInit(0.U(log2Ceil(maxIter+1).W))
    val sendCS = RegNext(sendNS)
    val send = Wire(Decoupled(UInt(0.W)))
    send.bits := DontCare
    val recv = Wire(Decoupled(UInt(0.W)))
    recv.bits := DontCare

    switch(sendCS){
      is(0.U){
        when(io.in.fire){
          sendNS := sendCS +% 1.U
        }.otherwise{ sendNS := 0.U }
      }
      is((1 until maxIter).map{_.U}){
        when(send.fire){ sendNS := sendCS +% 1.U }.otherwise{ sendNS := sendCS }
      }
      is(maxIter.U){
        when(send.fire){
          when(io.in.fire){ // continuous flow
            sendNS := 1.U
          }.otherwise{
            sendNS := 0.U
          }
        }.otherwise{
          sendNS := sendCS
        }
      }
    }

    switch(sendNS){
      is(0.U){
        inReg := 0.U.asTypeOf(inReg)
      }
      is(1.U){
        when(io.in.fire){
          inReg := io.in.bits
        }
      }
      is((2 to maxIter).map{_.U}){
        when(sendCS =/= sendNS){
          (0 until softThread).foreach { i =>
            if (i + hardThread < softThread) {
              inReg.in1(i) := inReg.in1(i + hardThread)
              inReg.in2(i) := inReg.in2(i + hardThread)
              inReg.in3(i) := inReg.in3(i + hardThread)
            }
            else {
              inReg.in1(i) := 0.U
              inReg.in2(i) := 0.U
              inReg.in3(i) := 0.U
            }
          }
        }
      }
    }
    send.valid := sendCS =/= 0.U
    send.ready := recv.ready

    val recvNS = WireInit(0.U(log2Ceil(maxIter+1).W))
    val recvCS = RegNext(recvNS)

    val recv_wvd = Reg(Bool())
    val recv_simt_stack = Reg(Bool())

    switch(recvCS){
      is((0 until maxIter).map{_.U}){
        when(recv.fire){
          recvNS := recvCS +% 1.U
        }.otherwise{
          recvNS := recvCS // may never happen
        }
      }
      is(maxIter.U){
        when(outFIFOReady){ // continuous flow
          when(recv.fire){
            recvNS := 1.U
          }.otherwise{
            recvNS := 0.U
          }
        }.otherwise{
          recvNS := recvCS
        }
      }
    }

    switch(recvNS){
      is(0.U){
        resultReg := 0.U.asTypeOf(resultReg)
        simtReg.if_mask := 0.U(softThread.W)
      }
      is((1 to maxIter).map{_.U}){
        when(recv.fire){
          (0 until softThread).foreach { i =>
            if (i + hardThread < softThread) {
              resultReg.wb_wvd_rd(i) := resultReg.wb_wvd_rd(i + hardThread)
            }
            else {
              resultReg.wb_wvd_rd(i) := hardResult(i + hardThread - softThread)
            }
            simtReg.if_mask := Cat(~(VecInit(alu.map({ x => x.cmp_out })).asUInt), simtReg.if_mask(softThread-1, softThread-hardThread))
          }
          when(recvNS===maxIter.U){
            //resultReg.wb_wvd_rd.foreach{ _ := 0.U }
            resultReg.warp_id := inReg.ctrl.wid
            resultReg.reg_idxw := inReg.ctrl.reg_idxw
            resultReg.wvd := inReg.ctrl.wvd
            resultReg.wvd_mask := inReg.mask
            if(SPIKE_OUTPUT){
              resultReg.spike_info.foreach{ _ := inReg.ctrl.spike_info.get }
            }
            simtReg.wid := inReg.ctrl.wid
            recv_wvd := inReg.ctrl.wvd
            recv_simt_stack := inReg.ctrl.simt_stack
          }
        }
      }
    }
    recv.ready := recvCS =/= maxIter.U || (recvCS===maxIter.U && outFIFOReady)
    recv.valid := send.valid

    (0 until hardThread).foreach{ x =>
      alu(x).in1 := inReg.in1(x)
      alu(x).in2 := inReg.in2(x)
      alu(x).in3 := inReg.in3(x)
      alu(x).func := inReg.ctrl.alu_fn(4, 0)
      hardResult(x) := alu(x).out
      when(inReg.ctrl.reverse) {
        alu(x).in1 := inReg.in2(x)
        alu(x).in2 := inReg.in1(x)
      }
      when((inReg.ctrl.alu_fn === FN_VMANDNOT) | (inReg.ctrl.alu_fn === FN_VMORNOT) | (inReg.ctrl.alu_fn === FN_VMNAND) | (inReg.ctrl.alu_fn === FN_VMNOR) | (inReg.ctrl.alu_fn === FN_VMXNOR)) {
        when((inReg.ctrl.alu_fn === FN_VMANDNOT) | (inReg.ctrl.alu_fn === FN_VMORNOT)) {
          alu(x).in1 := (~inReg.in1(x))
          alu(x).func := Cat(3.U(4.W), inReg.ctrl.alu_fn(0))
        }.otherwise({
          when(inReg.ctrl.alu_fn === FN_VMXNOR) {
            alu(x).func := FN_XOR(4, 0)
          }
            .otherwise(alu(x).func := Cat(3.U(4.W), inReg.ctrl.alu_fn(0)))
          hardResult(x) := (~alu(x).out)
        })
      }
      when(inReg.ctrl.alu_fn === FN_VID) {
        hardResult(x) := Mux(sendCS===0.U, 0.U, (sendCS-1.U)*hardThread.U + x.U)
      }
      when(inReg.ctrl.alu_fn === FN_VMERGE) {
        hardResult(x) := Mux(inReg.mask(x), inReg.in1(x), inReg.in2(x))
      }
    }
    result.io.enq.valid := recvCS===maxIter.U && recv_wvd && !recv_simt_stack
    result2simt.io.enq.valid := recvCS===maxIter.U && recv_simt_stack
    result.io.enq.bits := resultReg
    result2simt.io.enq.bits := simtReg

    io.in.ready := sendCS === 0.U || (sendCS===maxIter.U && outFIFOReady)
  }
  result.io.deq <> io.out
  result2simt.io.deq <> io.out2simt_stack
}

class ctrl_fpu extends Bundle{
val ctrl=new CtrlSigs
val mask=Vec(num_thread,Bool())
}
class FPUexe(softThread: Int = num_thread, hardThread: Int = num_thread) extends Module {
    assert(softThread % hardThread == 0)
  val io = IO(new Bundle {
    val in = Flipped(DecoupledIO(new vExeData()))
    val rm = Input(UInt(3.W))
    val out_x = DecoupledIO(new WriteScalarCtrl())
    val out_v = DecoupledIO(new WriteVecCtrl)
  })
  class TestFPUCtrl(depth_warp: Int, num_thread: Int, SPIKE_OUTPUT: Boolean)
    extends FPUv2.utils.TestFPUCtrl(depth_warp, num_thread){
    val spike_info = if(SPIKE_OUTPUT) Some(new InstWriteBack) else None
  }
  val fpu = Module(new FPUv2.VectorFPU(8, 24, softThread, hardThread, new TestFPUCtrl(depth_warp, num_thread,SPIKE_OUTPUT=SPIKE_OUTPUT)))
  (0 until num_thread).foreach{ x =>
    fpu.io.in.bits.data(x).a := io.in.bits.in1(x)
    fpu.io.in.bits.data(x).b := io.in.bits.in2(x)
    fpu.io.in.bits.data(x).c := io.in.bits.in3(x)
    fpu.io.in.bits.data(x).rm := io.rm
    when(io.in.bits.ctrl.reverse){
      fpu.io.in.bits.data(x).a := io.in.bits.in2(x)
      fpu.io.in.bits.data(x).b := io.in.bits.in1(x)
    }
    fpu.io.in.bits.data(x).op := io.in.bits.ctrl.alu_fn
    when((io.in.bits.ctrl.alu_fn===FN_VFMADD)|(io.in.bits.ctrl.alu_fn===FN_VFMSUB)|(io.in.bits.ctrl.alu_fn===FN_VFNMADD)|(io.in.bits.ctrl.alu_fn===FN_VFNMSUB)){
      fpu.io.in.bits.data(x).op := io.in.bits.ctrl.alu_fn-10.U
      fpu.io.in.bits.data(x).a := io.in.bits.in1(x)
      fpu.io.in.bits.data(x).b := io.in.bits.in3(x)
      fpu.io.in.bits.data(x).c := io.in.bits.in2(x)
    }
  }
  fpu.io.in.bits.ctrl.wvd := io.in.bits.ctrl.wvd
  fpu.io.in.bits.ctrl.wxd := io.in.bits.ctrl.wxd
  fpu.io.in.bits.ctrl.regIndex := io.in.bits.ctrl.reg_idxw
  fpu.io.in.bits.ctrl.warpID := io.in.bits.ctrl.wid
  fpu.io.in.bits.ctrl.vecMask := io.in.bits.mask.asUInt
  fpu.io.in.valid := io.in.valid
  io.in.ready := fpu.io.in.ready

  (0 until num_thread).foreach{ x =>
    io.out_v.bits.wb_wvd_rd(x) := fpu.io.out.bits.data(x).result(31,0)
  }
  if (SPIKE_OUTPUT) {
//    fpu.io.in.bits.ctrl.asInstanceOf(new TestFPUCtrl(depth_warp, num_thread,SPIKE_OUTPUT=SPIKE_OUTPUT)).spike_info.get := io.in.bits.ctrl.spike_info.get
    fpu.io.in.bits.ctrl.spike_info.get := io.in.bits.ctrl.spike_info.get // <- IDEA报错但能正常运行
    io.out_v.bits.spike_info.get := fpu.io.out.bits.ctrl.spike_info.get
    io.out_x.bits.spike_info.get := fpu.io.out.bits.ctrl.spike_info.get
  }
  io.out_x.bits.wb_wxd_rd := fpu.io.out.bits.data(0).result(31,0)
  io.out_v.bits.reg_idxw := fpu.io.out.bits.ctrl.regIndex
  io.out_x.bits.reg_idxw := fpu.io.out.bits.ctrl.regIndex
  io.out_v.bits.warp_id := fpu.io.out.bits.ctrl.warpID
  io.out_x.bits.warp_id := fpu.io.out.bits.ctrl.warpID
  io.out_v.bits.wvd_mask := VecInit(fpu.io.out.bits.ctrl.vecMask.asBools)
  io.out_v.bits.wvd := fpu.io.out.bits.ctrl.wvd
  io.out_x.bits.wxd := fpu.io.out.bits.ctrl.wxd
  io.out_v.valid := fpu.io.out.valid && fpu.io.out.bits.ctrl.wvd
  io.out_x.valid := fpu.io.out.valid && fpu.io.out.bits.ctrl.wxd
  fpu.io.out.ready := Mux(fpu.io.out.bits.ctrl.wvd, io.out_v.ready, io.out_x.ready)
}

class SFUexe extends Module{
  val io = IO(new Bundle {
    val in = Flipped(DecoupledIO(new vExeData()))
    val rm = Input(UInt(3.W))
    val out_x = DecoupledIO(new WriteScalarCtrl())
    val out_v = DecoupledIO(new WriteVecCtrl)
  })
  val result_x=Module(new Queue(new WriteScalarCtrl,1,pipe=true))
  val result_v=Module(new Queue(new WriteVecCtrl,1,pipe=true))
  val s_idle :: s_busy :: s_finish :: Nil = Enum(3)
  val state=RegInit(s_idle)

  val data_buffer=Queue(io.in,1)
  val mask=RegInit(0.U(num_thread.W))
  val num_grp = num_thread/num_sfu
  val mask_grp=Wire(Vec((num_grp),(Bool())))
  mask_grp.zipWithIndex.foreach(x=>x._1:=(mask(x._2*num_sfu+num_sfu-1,x._2*num_sfu)).orR)
  //val ctrl_fma=Module(new Queue(new ctrl_fpu,5,flow=true))


  val out_data=RegInit(VecInit(Seq.fill(num_thread)(0.U(xLen.W))))

  val i_cnt = PriorityEncoder(mask_grp)
  val next_i_cnt = PriorityEncoder((mask_grp.asUInt & ( ~(1.U(num_thread.W)<<i_cnt)).asUInt))
  val i_valid = RegInit(false.B) // a better valid should change for each fire.
  val i_ctrl = data_buffer.bits.ctrl
  val i_data1 = WireInit(VecInit(Seq.fill(num_sfu)(0.U(xLen.W))))
  val i_data2 = WireInit(VecInit(Seq.fill(num_sfu)(0.U(xLen.W))))
  val i_data3 = WireInit(VecInit(Seq.fill(num_sfu)(0.U(xLen.W))))
  val i_mask = WireInit(VecInit(Seq.fill(num_sfu)(false.B)))
  for(i<-0 until num_grp) {
    when(i.asUInt===i_cnt){
      val i_1 = VecInit(data_buffer.bits.in1.slice(i*num_sfu,i*num_sfu+num_sfu))
      val i_2 = VecInit(data_buffer.bits.in2.slice(i*num_sfu,i*num_sfu+num_sfu))
      i_data1:=Mux(i_ctrl.reverse,i_2,i_1)
      i_data2:=Mux(i_ctrl.reverse,i_1,i_2)
      i_data3 := VecInit(data_buffer.bits.in3.slice(i*num_sfu,i*num_sfu+num_sfu))
      i_mask:=mask(i*num_sfu+num_sfu-1,i*num_sfu).asBools
    }}

  val intDiv=VecInit(Seq.fill(num_sfu)(Module(new IntDivMod(xLen)).io))
  val floatDiv=VecInit(Seq.fill(num_sfu)(Module(new FloatDivSqrt).io))
  val alu_out_arbiter=VecInit(Seq.fill(num_sfu)(Module(new Arbiter(UInt(xLen.W),2)).io))
  alu_out_arbiter.foreach(x=>x.out.ready:=alu_out_arbiter.map(x=>x.out.valid).reduce(_&_))// i_ctrl.wfd & result_v.io.enq.ready | i_ctrl.wxd & result_x.io.enq.ready | !(i_ctrl.wxd&i_ctrl.wfd)
  //result_x.io.enq.bits:=Cat(out_data(0),i_ctrl.wxd,i_ctrl.reg_idxw,i_ctrl.wid).asTypeOf(new WriteScalarCtrl)
  //result_v.io.enq.bits:=Cat(out_data.asUInt,data_buffer.bits.mask.asUInt,i_ctrl.wfd,i_ctrl.reg_idxw,i_ctrl.wid).asTypeOf(new WriteVecCtrl)
  result_v.io.enq.bits.wvd_mask:=data_buffer.bits.mask
  result_v.io.enq.bits.wvd:=i_ctrl.wvd
  result_v.io.enq.bits.wb_wvd_rd:=out_data
  result_v.io.enq.bits.reg_idxw:=i_ctrl.reg_idxw
  result_v.io.enq.bits.warp_id:=i_ctrl.wid
  result_x.io.enq.bits.wxd:=i_ctrl.wxd
  result_x.io.enq.bits.warp_id:=i_ctrl.wid
  result_x.io.enq.bits.reg_idxw:=i_ctrl.reg_idxw
  result_x.io.enq.bits.wb_wxd_rd:=out_data(0)
  if (SPIKE_OUTPUT) {
    result_v.io.enq.bits.spike_info.get := i_ctrl.spike_info.get
    result_x.io.enq.bits.spike_info.get := i_ctrl.spike_info.get
  }
  result_x.io.enq.valid:=state===s_finish&i_ctrl.wxd
  result_v.io.enq.valid:=state===s_finish&i_ctrl.wvd
  val o_ready= i_ctrl.isvec&result_v.io.enq.ready | !i_ctrl.isvec & result_x.io.enq.ready
  for(i <- 0 until num_sfu)
  {
    alu_out_arbiter(i).in(0).bits := Mux(i_ctrl.alu_fn(0), intDiv(i).out.bits.r, intDiv(i).out.bits.q)
    alu_out_arbiter(i).in(1).bits := floatDiv(i).out.bits.result
    alu_out_arbiter(i).in(0).valid := intDiv(i).out.valid
    alu_out_arbiter(i).in(1).valid := floatDiv(i).out.valid
    intDiv(i).out.ready := alu_out_arbiter(i).in(0).ready
    floatDiv(i).out.ready := alu_out_arbiter(i).in(1).ready

    intDiv(i).in.bits.a := 1.U
    intDiv(i).in.bits.d := 1.U
    floatDiv(i).in.bits.a :=(0x3f800000L).U(32.W)
    floatDiv(i).in.bits.b :=(0x3f800000L).U(32.W)
    floatDiv(i).in.bits.c :=(0x3f800000L).U(32.W)
    for(j <- 0 until num_grp){
      when(j.asUInt===i_cnt & i_mask(i)){
    intDiv(i).in.bits.a := i_data1(i)
    intDiv(i).in.bits.d := i_data2(i)
    floatDiv(i).in.bits.a := i_data1(i)
    floatDiv(i).in.bits.b := i_data2(i)
    floatDiv(i).in.bits.c := i_data3(i)}}
    intDiv(i).in.bits.signed := !i_ctrl.alu_fn(1)
    floatDiv(i).in.bits.rm := io.rm
    floatDiv(i).in.bits.op := i_ctrl.alu_fn(2, 0)

    intDiv(i).in.valid := !i_ctrl.fp & i_valid
    floatDiv(i).in.valid := i_ctrl.fp & i_valid
  }
    val i_ready = Mux(i_ctrl.fp, floatDiv(0).in.ready, intDiv(0).in.ready)
  data_buffer.ready:=state===s_finish&o_ready

  val alu_out_fire = alu_out_arbiter(0).out.fire
  switch(state){
    is(s_idle){
      when(io.in.fire){
        state:=s_busy
        mask:=io.in.bits.mask.asUInt
        i_valid:=true.B
      }
    }
    is(s_busy) {
      when(i_valid & i_ready){
        i_valid:=false.B
      }
      when(data_buffer.bits.ctrl.isvec & alu_out_fire) {
        for(i <- 0 until num_grp){
          when(i.asUInt===i_cnt){
            val next_mask=mask & (~((Fill(num_sfu,1.U(1.W))).asTypeOf(UInt(xLen.W)) << i*num_sfu)).asUInt
            mask := next_mask
            i_valid:=true.B
            for(j <- 0 until num_sfu) out_data(i*num_sfu+j) := alu_out_arbiter(j).out.bits
            when(!next_mask.orR){
              state := s_finish
              i_valid:=false.B
            }
          }
        }


      }.elsewhen(alu_out_fire) {
        out_data(0) := alu_out_arbiter(0).out.bits
        state := s_finish
        i_valid:=false.B
      }

    }
    is(s_finish){
      when(o_ready){
        state:=s_idle
        out_data:=0.U.asTypeOf(Vec(num_thread,UInt(xLen.W)))
      }
    }
  }

  io.out_v<>result_v.io.deq
  io.out_x<>result_x.io.deq
}


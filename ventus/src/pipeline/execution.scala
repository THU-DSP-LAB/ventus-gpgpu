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
class vMULexe extends Module{
  val io = IO(new Bundle {
    val in = Flipped(DecoupledIO(new vExeData()))
    val out_x = DecoupledIO(new WriteScalarCtrl())
    val out_v = DecoupledIO(new WriteVecCtrl)
  })
  val mul=VecInit(Seq.fill(num_thread)(Module(new ArrayMultiplier(xLen)).io))
  val result_x=Module(new Queue(new WriteScalarCtrl,1,pipe=true))
  val result_v=Module(new Queue(new WriteVecCtrl,1,pipe=true))
  (0 until num_thread).foreach(x=>{
    mul(x).in.bits.a := io.in.bits.in1(x)
    mul(x).in.bits.b := io.in.bits.in2(x)
    mul(x).in.bits.c := io.in.bits.in3(x)
    mul(x).in.bits.ctrl := io.in.bits.ctrl
    mul(x).in.bits.mask := io.in.bits.mask
    mul(x).in.valid:=io.in.valid
    result_v.io.enq.bits.wb_wfd_rd(x) := mul(x).out.bits.result
    mul(x).out.ready:=Mux(mul(x).out.bits.ctrl.wxd,result_x.io.enq.ready,result_v.io.enq.ready)
    when(io.in.bits.ctrl.reverse){
      mul(x).in.bits.a:=io.in.bits.in2(x)
      mul(x).in.bits.b:=io.in.bits.in1(x)
    }
  })


  result_v.io.enq.bits.warp_id:=mul(0).out.bits.ctrl.wid
  result_v.io.enq.bits.reg_idxw:=mul(0).out.bits.ctrl.reg_idxw
  result_v.io.enq.bits.wfd:=mul(0).out.bits.ctrl.wfd
  result_v.io.enq.bits.wfd_mask:=mul(0).out.bits.mask

  result_x.io.enq.bits.warp_id:=mul(0).out.bits.ctrl.wid
  result_x.io.enq.bits.reg_idxw:=mul(0).out.bits.ctrl.reg_idxw
  result_x.io.enq.bits.wxd:=mul(0).out.bits.ctrl.wxd
  result_x.io.enq.bits.wb_wxd_rd:=mul(0).out.bits.result

  result_v.io.enq.valid:=mul(0).out.valid&mul(0).out.bits.ctrl.wfd
  result_x.io.enq.valid:=mul(0).out.valid&mul(0).out.bits.ctrl.wxd
  io.in.ready:=mul(0).in.ready//Mux(io.in.bits.ctrl.wfd,result_v.io.enq.ready,Mux(io.in.bits.ctrl.wxd,result_x.io.enq.ready,true.B))
  io.out_v<>result_v.io.deq
  io.out_x<>result_x.io.deq

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
    result.io.enq.bits.wb_wfd_rd(x):=alu(x).out
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
        result.io.enq.bits.wb_wfd_rd(x):=(~alu(x).out)
      })
    }
    when(io.in.bits.ctrl.alu_fn===FN_VID){
      result.io.enq.bits.wb_wfd_rd(x):=x.asUInt()
    }
    when(io.in.bits.ctrl.alu_fn===FN_VMERGE){
      result.io.enq.bits.wb_wfd_rd(x):=Mux(io.in.bits.mask(x),io.in.bits.in1(x),io.in.bits.in2(x))
      result.io.enq.bits.wfd_mask(x):=true.B
    }
  })
  when(io.in.bits.ctrl.writemask){
    result.io.enq.bits.wb_wfd_rd(0):=Mux(io.in.bits.ctrl.readmask,alu(0).out,VecInit((0 until num_thread).map(x=>{Mux(io.in.bits.mask(x),alu(x).out(0),0.U)})).asUInt)
    result.io.enq.bits.wfd_mask(0):=1.U
    when((io.in.bits.ctrl.alu_fn===FN_VMNAND)|(io.in.bits.ctrl.alu_fn===FN_VMNOR)|(io.in.bits.ctrl.alu_fn===FN_VMXNOR)){
      result.io.enq.bits.wb_wfd_rd(0):=VecInit((0 until num_thread).map(x=>{Mux(io.in.bits.mask(x),!alu(x).out(0),false.B)})).asUInt
    }
  }

  result.io.enq.bits.warp_id:=io.in.bits.ctrl.wid
  result.io.enq.bits.reg_idxw:=io.in.bits.ctrl.reg_idxw
  result.io.enq.bits.wfd:=io.in.bits.ctrl.wfd
  result.io.enq.bits.wfd_mask:=io.in.bits.mask
  result.io.enq.valid:=io.in.valid&io.in.bits.ctrl.wfd&(!io.in.bits.ctrl.simt_stack)

  result2simt.io.enq.bits.wid:=io.in.bits.ctrl.wid
  result2simt.io.enq.bits.if_mask:= ~(VecInit(alu.map({x=>x.cmp_out})).asUInt)
  result2simt.io.enq.valid:=io.in.valid&io.in.bits.ctrl.simt_stack

  io.in.ready:=Mux(io.in.bits.ctrl.simt_stack,result2simt.io.enq.ready,result.io.enq.ready)
  io.out<>result.io.deq
  io.out2simt_stack<>result2simt.io.deq
}
class ctrl_fpu extends Bundle{
val ctrl=new CtrlSigs
val mask=Vec(num_thread,Bool())
}
class FPUexe extends Module{
  val io = IO(new Bundle {
    val in = Flipped(DecoupledIO(new vExeData()))
    val rm = Input(UInt(3.W))
    val out_x = DecoupledIO(new WriteScalarCtrl())
    val out_v = DecoupledIO(new WriteVecCtrl)
  })
  val fpu=VecInit(Seq.fill(num_thread)((Module(new ScalarFPU())).io))
  val result_x=Module(new Queue(new WriteScalarCtrl,1,pipe=true))
  val result_v=Module(new Queue(new WriteVecCtrl,1,pipe=true))
  (0 until num_thread).foreach(x=> {
    fpu(x).in.bits.a := io.in.bits.in1(x)
    fpu(x).in.bits.b := io.in.bits.in2(x)
    fpu(x).in.bits.c := io.in.bits.in3(x)
    fpu(x).in.bits.fpuop := io.in.bits.ctrl.alu_fn
    fpu(x).in.bits.rm := io.rm
    fpu(x).in.valid:=io.in.valid
    result_v.io.enq.bits.wb_wfd_rd(x) := fpu(x).out.bits.result
    //when(io.in.bits.ctrl.alu_fn===)
    when(io.in.bits.ctrl.reverse) {
      fpu(x).in.bits.a := io.in.bits.in2(x)
      fpu(x).in.bits.b := io.in.bits.in1(x)
    }
    when((io.in.bits.ctrl.alu_fn===FN_VFMADD)|(io.in.bits.ctrl.alu_fn===FN_VFMSUB)|(io.in.bits.ctrl.alu_fn===FN_VFNMADD)|(io.in.bits.ctrl.alu_fn===FN_VFNMSUB)){
      fpu(x).in.bits.fpuop:=io.in.bits.ctrl.alu_fn-10.U
      fpu(x).in.bits.a := io.in.bits.in1(x)
      fpu(x).in.bits.b := io.in.bits.in3(x)
      fpu(x).in.bits.c := io.in.bits.in2(x)
    }
  })

  val ctrl_fma=Module(new Queue(new ctrl_fpu,5,flow=true))
  val ctrl_else=Module(new Queue(new ctrl_fpu,1,flow=true))
  ctrl_fma.io.enq.bits.ctrl:=io.in.bits.ctrl
  ctrl_fma.io.enq.bits.mask:=io.in.bits.mask
  ctrl_fma.io.enq.valid:=fpu(0).in.fire & fpu(0).in.bits.fpuop(5,3) === 0.U
  ctrl_fma.io.deq.ready:=fpu(0).out.fire & fpu(0).select === 0.U
  ctrl_else.io.enq.bits.ctrl:=io.in.bits.ctrl
  ctrl_else.io.enq.bits.mask:=io.in.bits.mask
  ctrl_else.io.enq.valid:=fpu(0).in.fire & fpu(0).in.bits.fpuop(5,3) =/= 0.U
  ctrl_else.io.deq.ready:=fpu(0).out.fire & fpu(0).select =/= 0.U
  val ctrl=Mux(fpu(0).select===0.U,ctrl_fma.io.deq.bits,ctrl_else.io.deq.bits)
  (0 until num_thread).foreach(x=> {
    fpu(x).out.ready:=Mux(ctrl.ctrl.wxd,result_x.io.enq.ready,result_v.io.enq.ready)
  })

  result_v.io.enq.bits.warp_id:=ctrl.ctrl.wid
  result_v.io.enq.bits.reg_idxw:=ctrl.ctrl.reg_idxw
  result_v.io.enq.bits.wfd:=ctrl.ctrl.wfd
  result_v.io.enq.bits.wfd_mask:=ctrl.mask

  result_x.io.enq.bits.warp_id:=ctrl.ctrl.wid
  result_x.io.enq.bits.reg_idxw:=ctrl.ctrl.reg_idxw
  result_x.io.enq.bits.wxd:=ctrl.ctrl.wxd
  result_x.io.enq.bits.wb_wxd_rd:=fpu(0).out.bits.result

  result_v.io.enq.valid:=fpu(0).out.valid&ctrl.ctrl.wfd
  result_x.io.enq.valid:=fpu(0).out.valid&ctrl.ctrl.wxd
  io.in.ready:=fpu(0).in.ready//Mux(io.in.bits.ctrl.wfd,result_v.io.enq.ready,Mux(io.in.bits.ctrl.wxd,result_x.io.enq.ready,true.B))
  io.out_v<>result_v.io.deq
  io.out_x<>result_x.io.deq
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
  result_v.io.enq.bits.wfd_mask:=data_buffer.bits.mask
  result_v.io.enq.bits.wfd:=i_ctrl.wfd
  result_v.io.enq.bits.wb_wfd_rd:=out_data
  result_v.io.enq.bits.reg_idxw:=i_ctrl.reg_idxw
  result_v.io.enq.bits.warp_id:=i_ctrl.wid
  result_x.io.enq.bits.wxd:=i_ctrl.wxd
  result_x.io.enq.bits.warp_id:=i_ctrl.wid
  result_x.io.enq.bits.reg_idxw:=i_ctrl.reg_idxw
  result_x.io.enq.bits.wb_wxd_rd:=out_data(0)

  result_x.io.enq.valid:=state===s_finish&i_ctrl.wxd
  result_v.io.enq.valid:=state===s_finish&i_ctrl.wfd
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
      when(io.in.fire()){
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
            when(!next_mask.orR()){
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


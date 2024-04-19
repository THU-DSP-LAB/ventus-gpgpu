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
import top.parameters.{SPIKE_OUTPUT, wid_to_check, xLen}
class Branch_back extends Module{
  val io = IO(new Bundle{
    val out=DecoupledIO(new BranchCtrl)
    val in0=Flipped(DecoupledIO(new BranchCtrl))
    val in1=Flipped(DecoupledIO(new BranchCtrl))
  })
  val fifo0=Queue.apply(io.in0,0)
  val fifo1=Queue.apply(io.in1,0)
  val arbiter=Module(new Arbiter(new BranchCtrl(),2))
  arbiter.io.in(0)<>fifo0
  arbiter.io.in(1)<>fifo1
  arbiter.io.out<>io.out
  if (SPIKE_OUTPUT) {
    when(io.out.fire/* && io.out.bits.wid === wid_to_check.U*/) {
      printf(p"sm ${io.out.bits.spike_info.get.sm_id} warp ${io.out.bits.wid} ")
      printf(p"0x${Hexadecimal(io.out.bits.spike_info.get.pc)} 0x${Hexadecimal(io.out.bits.spike_info.get.inst)}")
      printf(p" Jump? ${Decimal(io.out.bits.jump)}  ${Hexadecimal(io.out.bits.new_pc)}\n")
    }
}}
class InstWriteBack extends Bundle{
  val sm_id = UInt(8.W)
  val pc=UInt(xLen.W)
  val inst=UInt(32.W)
}
class Writeback(num_x:Int,num_v:Int) extends Module{
  val io = IO(new Bundle{
    val out_v=(DecoupledIO(new WriteVecCtrl))
    val out_x=(DecoupledIO(new WriteScalarCtrl))
    val in_x=Vec(num_x,Flipped(DecoupledIO(new WriteScalarCtrl)))
    val in_v=Vec(num_v,Flipped(DecoupledIO(new WriteVecCtrl)))
  })
  //val fifo=VecInit(Seq.fill(3)(Module(new Queue((new WriteCtrl),2)).io))
  val fifo_x=for(i<-0 until num_x) yield
  { val x=Queue.apply(io.in_x(i),0)
    x
  }
  val fifo_v=for(i<-0 until num_v) yield
  { val x=Queue.apply(io.in_v(i),0)
    x
  }
  val arbiter_x=Module(new Arbiter(new WriteScalarCtrl(),num_x))
  val arbiter_v=Module(new Arbiter(new WriteVecCtrl(),num_v))
  arbiter_x.io.in<>fifo_x
  arbiter_v.io.in<>fifo_v
  arbiter_x.io.out<>io.out_x
  arbiter_v.io.out<>io.out_v
  //send to operand collector
  if(SPIKE_OUTPUT){
    when(io.out_x.fire/*&&io.out_x.bits.warp_id===wid_to_check.U*/){
      printf(p"sm ${io.out_x.bits.spike_info.get.sm_id} warp ${Decimal(io.out_x.bits.warp_id)} ")
      printf(p"0x${Hexadecimal(io.out_x.bits.spike_info.get.pc)} 0x${Hexadecimal(io.out_x.bits.spike_info.get.inst)}")
      printf(p" x${io.out_x.bits.reg_idxw}  ${Hexadecimal(io.out_x.bits.wb_wxd_rd)}\n")
    }
    when(io.out_v.fire/* && io.out_v.bits.warp_id === wid_to_check.U*/) {
      printf(p"sm ${io.out_v.bits.spike_info.get.sm_id} warp ${Decimal(io.out_v.bits.warp_id)} ")
      printf(p"0x${Hexadecimal(io.out_v.bits.spike_info.get.pc)} 0x${Hexadecimal(io.out_v.bits.spike_info.get.inst)}")
      printf(p" v${io.out_v.bits.reg_idxw} ")
      printf(p"${Binary(io.out_v.bits.wvd_mask.asUInt)} ")
      io.out_v.bits.wb_wvd_rd.reverse.foreach(x=>printf(p"${Hexadecimal(x.asUInt)} "))
      printf(p"\n")
    }
  }
}

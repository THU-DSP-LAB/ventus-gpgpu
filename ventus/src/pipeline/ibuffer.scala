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



class instbuffer extends Module{
  val io = IO(new Bundle{
    val in=Flipped(DecoupledIO(new CtrlSigs))
    val flush=Flipped(ValidIO(UInt(depth_warp.W)))
    val out=Vec(num_warp,Decoupled(new CtrlSigs))
    val ibuffer_ready=Output(Vec(num_warp,Bool()))
  })
  //val fifo=VecInit(Seq.fill(num_warp)(Module(new QueueWithFlush((new CtrlSigs),2,hasFlush = true)).io))
  io.in.ready:=false.B
  val fifo=(0 until num_warp).map(i=>{
    val x_single=Module(new Queue(new CtrlSigs,num_ibuffer,hasFlush=true))
      io.ibuffer_ready(i):=x_single.io.enq.ready
      x_single.io.enq.bits:=io.in.bits
      x_single.io.enq.valid:=Mux((i.asUInt===io.in.bits.wid),io.in.valid,false.B)
      when(i.asUInt===io.in.bits.wid){io.in.ready:=x_single.io.enq.ready}
      io.out(i)<>x_single.io.deq
      x_single.flush:=io.flush.valid&(i.asUInt===io.flush.bits)
    x_single
  })
  //val arbiter=Module(new arbiter_m2o(3))
}
class ibuffer2issue extends Module{
  val io = IO(new Bundle{
    val in=Flipped(Vec(num_warp,Decoupled(new CtrlSigs)))
    val out=Decoupled(new CtrlSigs)
    val out_sel=Output(UInt(depth_warp.W))
  })
  val rrarbit=Module(new RRArbiter(new CtrlSigs(),num_warp))
  rrarbit.io.in<>io.in
  io.out<>rrarbit.io.out
/*
  rrarbit.io.out.ready:=false.B
  io.out.bits:=io.in(0).bits
  io.out.valid:=io.in(0).valid
  io.in.foreach(_.ready:=false.B)
  io.in(0).ready:=io.out.ready
*/
  io.out_sel:=rrarbit.io.chosen
  //input:ibuffer output: issue exe

}

// "num_fetch -> 1" slow down
//
class InstrBufferV2 extends Module{
  val io = IO(new Bundle{
    val in = Flipped(DecoupledIO(new Bundle{
      val control = Vec(num_fetch, new CtrlSigs)
      val control_mask = Vec(num_fetch, Bool())
    }))
    val flush_wid = Flipped(ValidIO(UInt(depth_warp.W)))
    val ibuffer_ready = Output(Vec(num_warp, Bool()))
    val out = Vec(num_warp, DecoupledIO(Output(new CtrlSigs)))
  })
  val buffers = VecInit(Seq.fill(num_warp)(Module(new Queue(Vec(num_fetch, new CtrlSigs), size_ibuffer, hasFlush = true)).io))
  val buffers_mask = VecInit(Seq.fill(num_warp)(Module(new Queue(Vec(num_fetch, Bool()), size_ibuffer, hasFlush = true)).io))
  (0 until num_warp).foreach{ i =>
    buffers(i).enq.valid := io.in.bits.control(0).wid === i.U && io.in.valid
    buffers(i).enq.bits := io.in.bits.control
    buffers(i).flush.foreach{ _ := io.flush_wid.valid && io.flush_wid.bits === i.U }
    buffers_mask(i).enq.valid := io.in.bits.control(0).wid === i.U && io.in.valid
    buffers_mask(i).enq.bits := io.in.bits.control_mask
    buffers_mask(i).flush.foreach{ _ := io.flush_wid.valid && io.flush_wid.bits === i.U }
    io.in.ready := buffers(io.in.bits.control(0).wid).enq.ready
    io.ibuffer_ready(i) := buffers(i).enq.ready
  }

  io.in.ready := buffers(io.in.bits.control(0).wid).enq.ready
  when(io.in.fire){
    buffers(io.in.bits.control(0).wid).enq.bits := io.in.bits.control
    buffers_mask(io.in.bits.control(0).wid).enq.bits := io.in.bits.control_mask
    when(io.flush_wid.valid){
      buffers(io.flush_wid.bits).enq.bits := 0.U.asTypeOf(Vec(num_fetch, new CtrlSigs))
      buffers_mask(io.flush_wid.bits).enq.bits := 0.U.asTypeOf(Vec(num_fetch, Bool()))
    }
  }
  class SlowDown extends Module{
    val io = IO(new Bundle{
      val in = Flipped(DecoupledIO(new Bundle{
        val control = Vec(num_fetch, new CtrlSigs)
        val control_mask = Vec(num_fetch, Bool())
      }))
      val flush = Input(Bool())
      val out = DecoupledIO(new CtrlSigs)
    })

    val control_reg = RegInit(0.U.asTypeOf(io.in.bits.control))
    val mask_reg = RegInit(0.U(num_fetch.W))
    val ptr = PriorityEncoder(mask_reg)

    val mask_next = mask_reg & (~(1.U << ptr)(num_fetch-1, 0)).asUInt
    when(io.flush){
      mask_reg := 0.U
      control_reg := 0.U.asTypeOf(control_reg)
    }.otherwise{
      when(io.out.fire) {
        mask_reg := mask_next
      }
      when(io.in.fire){
          mask_reg := io.in.bits.control_mask.asUInt // cover io.out.fire
          control_reg := io.in.bits.control
      }
    }
    io.in.ready := mask_next === 0.U && io.out.ready
    io.out.valid := mask_reg =/= 0.U
    io.out.bits := control_reg(ptr)
  }
  val slowDownArray = Seq.fill(num_warp)(Module(new SlowDown))
  (0 until num_warp).foreach{ i =>
    slowDownArray(i).io.flush := io.flush_wid.bits === i.U && io.flush_wid.valid
    slowDownArray(i).io.in.bits.control := buffers(i).deq.bits
    slowDownArray(i).io.in.bits.control_mask := buffers_mask(i).deq.bits
    slowDownArray(i).io.in.valid := buffers(i).deq.valid
    buffers(i).deq.ready := slowDownArray(i).io.in.ready
    buffers_mask(i).deq.ready := slowDownArray(i).io.in.ready

    io.out(i) <> slowDownArray(i).io.out
  }
}

class IBuffer2OpC extends Module{
  val io = IO(new Bundle {
    val in = Vec(num_warp, Flipped(DecoupledIO(Output(new CtrlSigs))))
    val out = Vec(num_fetch, DecoupledIO(Output(new CtrlSigs)))
  })
  val in_split = (0 until num_fetch).map { i => (num_warp + i) / num_fetch }.reverse
  val arbiters = in_split.reverse.dropWhile(_ <= 1).reverse.map { x =>
    Module(new RRArbiter(new CtrlSigs, x))
  }
  (0 until num_fetch).foreach{ i =>
    if(in_split(i) == 0){
      io.out(i).valid := false.B
      io.out(i).bits := 0.U.asTypeOf(new CtrlSigs)
    }
    else if(in_split(i) == 1){ io.out(i) <> io.in(i) }
    else{
      io.out(i) <> arbiters(i).io.out
      arbiters(i).io.in.zipWithIndex.foreach{ case(in, j) =>
        in <> io.in(j * num_fetch + i)
      }
    }
  }
}
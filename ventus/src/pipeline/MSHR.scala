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

import top.parameters._
import chisel3._
import chisel3.util._

class MSHROutput extends Bundle{
  val tag = new MshrTag
  val data = Vec(num_thread, UInt(xLen.W))
}

class MSHRv2 extends Module{
  val io = IO(new Bundle{
    val from_addr = Flipped(DecoupledIO(new Bundle{
      val tag = Input(new MshrTag)
    }))
    val idx_entry = Output(UInt(log2Up(lsu_nMshrEntry).W))
    val from_dcache = Flipped(DecoupledIO(new DCacheCoreRsp_np))
    val to_pipe = DecoupledIO(new MSHROutput)
  })
  val data = Mem(lsu_nMshrEntry, Vec(num_thread, UInt(xLen.W)))
  val tag = Mem(lsu_nMshrEntry, UInt((io.from_addr.bits.tag.getWidth).W))
  //val targetMask = RegInit(VecInit(Seq.fill(lsu_nMshrEntry)(0.U(num_thread.W))))
  val currentMask = RegInit(VecInit(Seq.fill(lsu_nMshrEntry)(0.U(num_thread.W)))) // 0: complete
  val inv_activeMask = VecInit(io.from_dcache.bits.activeMask.map(!_)).asUInt
  val used = RegInit(0.U(lsu_nMshrEntry.W))
  val complete = VecInit(currentMask.map{_===0.U}).asUInt & used
  val output_entry = Mux(complete.orR, PriorityEncoder(complete), 0.U)
  val valid_entry = Mux(used.andR, 0.U, PriorityEncoder(~used))
  val reg_req = Reg(new MshrTag)

  val s_idle :: s_add  :: s_out :: Nil = Enum(3)
  val state = RegInit(s_idle)

  io.from_dcache.ready := state===s_idle// && used.orR
  io.from_addr.ready := state===s_idle && !(used.andR)
  io.idx_entry := Mux(io.from_addr.fire, valid_entry, 0.U) // return the MSHR entry

  when(state===s_idle){
    when(io.from_dcache.fire){
      when(io.from_addr.fire){
        state := s_add
      }.elsewhen(io.to_pipe.ready && currentMask(io.from_dcache.bits.instrId)===io.from_dcache.bits.activeMask.asUInt){
        state := s_out
      }.otherwise{state := s_idle}
    }.elsewhen(complete.orR&&io.to_pipe.ready){
      state := s_out
    }.otherwise{
      state := s_idle
    }
  }.elsewhen(state===s_out){
    when(io.to_pipe.ready && complete.bitSet(valid_entry, false.B).orR){state:=s_out}.otherwise{state:=s_idle}
  }.elsewhen(state===s_add){
    when(io.to_pipe.ready && complete.orR){state:=s_out}.otherwise{state:=s_idle}
  }.otherwise{state:=s_idle}

  switch(state){
    is(s_idle){
      when(io.from_dcache.fire){ // deal with update request immediately
        data.write(io.from_dcache.bits.instrId, io.from_dcache.bits.data, io.from_dcache.bits.activeMask) // data update
        currentMask(io.from_dcache.bits.instrId) := currentMask(io.from_dcache.bits.instrId) & inv_activeMask // mask update
        when(io.from_addr.fire){reg_req := io.from_addr.bits.tag} // both input valid: save the add request, and deal with it in the next cycle
      }.elsewhen(io.from_addr.fire){// deal with add request immediately
        used := used.bitSet(valid_entry, true.B) // set MSHR entry used
        tag.write(valid_entry,
          //Cat(io.from_addr.bits.tag.warp_id, io.from_addr.bits.tag.reg_idxw, io.from_addr.bits.tag.mask.asUInt)
          io.from_addr.bits.tag.asUInt
        )
        data.write(valid_entry, VecInit(Seq.fill(num_thread)(0.U)))    // data initialize
        currentMask(valid_entry) := io.from_addr.bits.tag.mask.asUInt   // mask initialize
      }
    }
    is(s_add){
      used := used.bitSet(valid_entry, true.B)
      tag.write(valid_entry,
        //Cat(reg_req.warp_id, reg_req.reg_idxw, reg_req.mask.asUInt)
        reg_req.asUInt
      )
      data.write(valid_entry, VecInit(Seq.fill(num_thread)(0.U)))
      currentMask(valid_entry) := reg_req.mask.asUInt
    }
    is(s_out){ // release MSHR line
      when(io.to_pipe.fire){used := used.bitSet(output_entry, false.B)}
    }
  }
  val output_tag = tag.read(output_entry).asTypeOf(new MshrTag)
  val raw_data = data.read(output_entry)
  val output_data = Wire(Vec(num_thread, UInt(xLen.W)))
  (0 until num_thread).foreach{ x =>
    output_data(x) := Mux(output_tag.mask(x),
      ByteExtract(output_tag.unsigned, raw_data(x), output_tag.wordOffset1H(x)),
      0.U(xLen.W)
    )
  }
  io.to_pipe.valid := complete.orR && state===s_out
  io.to_pipe.bits.tag := output_tag.asTypeOf(new MshrTag)
  io.to_pipe.bits.data := output_data
}
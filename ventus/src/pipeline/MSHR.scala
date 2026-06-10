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
  val data = SyncReadMem(lsu_nMshrEntry, Vec(num_thread, UInt(xLen.W)))
  val tag = SyncReadMem(lsu_nMshrEntry, UInt((io.from_addr.bits.tag.getWidth).W))
  //val targetMask = RegInit(VecInit(Seq.fill(lsu_nMshrEntry)(0.U(num_thread.W))))
  val currentMask = RegInit(VecInit(Seq.fill(lsu_nMshrEntry)(0.U(num_thread.W)))) // 0: complete
  val inv_activeMask = VecInit(io.from_dcache.bits.activeMask.map(!_)).asUInt
  val used = RegInit(0.U(lsu_nMshrEntry.W))
  val complete = VecInit(currentMask.map{_===0.U}).asUInt & used
  val output_entry = Mux(complete.orR, PriorityEncoder(complete), 0.U)
  val valid_entry = Mux(used.andR, 0.U, PriorityEncoder(~used))
  val reg_req = RegInit(0.U.asTypeOf(new MshrTag))
  val read_entry = RegInit(0.U(log2Up(lsu_nMshrEntry).W))
  val rsp_valid = RegInit(false.B)
  val rsp_tag = RegInit(0.U.asTypeOf(new MshrTag))
  val rsp_raw_data = RegInit(VecInit(Seq.fill(num_thread)(0.U(xLen.W))))

  val s_idle :: s_add :: s_read :: s_out :: Nil = Enum(4)
  val state = RegInit(s_idle)
  val read_resp_valid = RegNext(state === s_read, false.B)
  val output_valid = read_resp_valid || rsp_valid
  val tag_write_valid = WireInit(false.B)
  val tag_write_entry = WireInit(0.U(log2Up(lsu_nMshrEntry).W))
  val tag_write_data = WireInit(0.U((io.from_addr.bits.tag.getWidth).W))

  io.from_dcache.ready := state===s_idle// && used.orR
  io.from_addr.ready := state===s_idle && !(used.andR)
  io.idx_entry := Mux(io.from_addr.fire, valid_entry, 0.U) // return the MSHR entry

  when(state===s_idle){
    when(io.from_dcache.fire){
      when(io.from_addr.fire){
        state := s_add
      }.elsewhen(currentMask(io.from_dcache.bits.instrId)===io.from_dcache.bits.activeMask.asUInt){
        state := s_read
        read_entry := io.from_dcache.bits.instrId
      }.otherwise{state := s_idle}
    }.elsewhen(complete.orR){
      state := s_read
      read_entry := output_entry
    }.otherwise{
      state := s_idle
    }
  }.elsewhen(state===s_add){
    when(complete.orR){
      state := s_read
      read_entry := output_entry
    }.otherwise{state:=s_idle}
  }.elsewhen(state===s_read){
    state := s_out
  }.elsewhen(state===s_out){
    val remaining_complete = complete.bitSet(read_entry, false.B)
    when(io.to_pipe.fire && remaining_complete.orR){
      state:=s_read
      read_entry := PriorityEncoder(remaining_complete)
    }.elsewhen(io.to_pipe.fire){
      state:=s_idle
    }.otherwise{state:=s_out}
  }.otherwise{state:=s_idle}

  switch(state){
    is(s_idle){
      when(io.from_dcache.fire){ // deal with update request immediately
        data.write(io.from_dcache.bits.instrId, io.from_dcache.bits.data, io.from_dcache.bits.activeMask) // data update
        currentMask(io.from_dcache.bits.instrId) := currentMask(io.from_dcache.bits.instrId) & inv_activeMask // mask update
        when(io.from_addr.fire){reg_req := io.from_addr.bits.tag} // both input valid: save the add request, and deal with it in the next cycle
      }.elsewhen(io.from_addr.fire){// deal with add request immediately
        used := used.bitSet(valid_entry, true.B) // set MSHR entry used
        tag_write_valid := true.B
        tag_write_entry := valid_entry
        tag_write_data := io.from_addr.bits.tag.asUInt
        // 数据存储应当不需要初始化，对应mask初始化即可。这里无mask的写端口似乎会使得上边有mask的写端口的mask也消失，故移除
        // data.write(valid_entry, VecInit(Seq.fill(num_thread)(0.U)), VecInit.fill(num_thread)(true.B))    // data initialize
        currentMask(valid_entry) := io.from_addr.bits.tag.mask.asUInt   // mask initialize
      }
    }
    is(s_add){
      used := used.bitSet(valid_entry, true.B)
      tag_write_valid := true.B
      tag_write_entry := valid_entry
      tag_write_data := reg_req.asUInt
      // data.write(valid_entry, VecInit(Seq.fill(num_thread)(0.U)), VecInit.fill(num_thread)(true.B))    // data initialize
      currentMask(valid_entry) := reg_req.mask.asUInt
    }
    is(s_out){ // release MSHR line
      when(io.to_pipe.fire){used := used.bitSet(read_entry, false.B)}
    }
  }
  when(tag_write_valid){
    tag.write(tag_write_entry, tag_write_data)
  }
  val output_tag_sram = tag.read(read_entry, state === s_read).asTypeOf(new MshrTag)
  val raw_data_sram = data.read(read_entry, state === s_read)
  when(read_resp_valid){
    when(!io.to_pipe.ready){
      rsp_valid := true.B
      rsp_tag := output_tag_sram
      rsp_raw_data := raw_data_sram
    }.otherwise{
      rsp_valid := false.B
    }
  }.elsewhen(io.to_pipe.fire){
    rsp_valid := false.B
  }
  val output_tag = MuxCase(0.U.asTypeOf(new MshrTag), Seq(
    rsp_valid -> rsp_tag,
    read_resp_valid -> output_tag_sram
  ))
  val raw_data = MuxCase(VecInit(Seq.fill(num_thread)(0.U(xLen.W))), Seq(
    rsp_valid -> rsp_raw_data,
    read_resp_valid -> raw_data_sram
  ))
  val output_data = Wire(Vec(num_thread, UInt(xLen.W)))
  (0 until num_thread).foreach{ x =>
    output_data(x) := Mux(output_tag.mask(x),
      ByteExtract(output_tag.unsigned, raw_data(x), output_tag.wordOffset1H(x)),
      0.U(xLen.W)
    )
  }
  io.to_pipe.valid := output_valid && state===s_out
  io.to_pipe.bits.tag := output_tag.asTypeOf(new MshrTag)
  io.to_pipe.bits.data := output_data
}

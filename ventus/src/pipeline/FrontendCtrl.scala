/*
 * Copyright (c) 2021-2022 International Innovation Center of Tsinghua University, Shanghai
 * Ventus is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package pipeline

import chisel3._
import chisel3.experimental.hierarchy.{instantiable, public}
import chisel3.util._
import top.parameters._

class FrontendCtrlIO extends Bundle {
  private val localDepthWarp =
    if (num_warp_per_subcore == 1) 1 else log2Ceil(num_warp_per_subcore)

  val pc_req = Vec(num_subcore, Flipped(DecoupledIO(new ICachePipeReq_np)))
  val pc_rsp = Vec(num_subcore, ValidIO(new ICachePipeRsp_np))

  val icache_req = DecoupledIO(new ICachePipeReq_np)
  val icache_rsp = Flipped(DecoupledIO(new ICachePipeRsp_np))

  val flush = Vec(num_subcore, Input(Valid(UInt(localDepthWarp.W))))
  val flush_cache = Vec(num_subcore, Input(Valid(UInt(localDepthWarp.W))))
  val launch_flush = Input(Valid(UInt(depth_warp.W)))
  val rsp_kill = Input(Valid(UInt(depth_warp.W)))
  val wg_tag_lookup = Input(Vec(num_warp, UInt(TAG_WIDTH.W)))
  val frontend_gen_lookup = Input(Vec(num_warp, UInt(frontend_gen_width.W)))
  val recently_freed = Input(UInt(num_warp.W))

  val frontend_flush_mask = Output(UInt(num_warp.W))
  val external_flush_pipe = ValidIO(UInt(depth_warp.W))

  val accept_icache_rsp = Output(Bool())
  val ibuffer_ready_for_rsp = Input(Bool())
}

@instantiable
class FrontendCtrl extends Module {
  @public val io = IO(new FrontendCtrlIO)

  private def internalWid(sc: Int, localWid: UInt): UInt =
    PipeSubcoreHelpers.internalWid(sc, localWid)

  val sourceWidth = if (num_subcore <= 1) 1 else log2Ceil(num_subcore)
  val frontendOutstanding = RegInit(VecInit(Seq.fill(num_warp)(false.B)))
  val frontendOutstandingAddr = RegInit(VecInit(Seq.fill(num_warp)(0.U(32.W))))
  val frontendOutstandingSource = RegInit(VecInit(Seq.fill(num_warp)(0.U(sourceWidth.W))))
  val frontendOutstandingWfTag = RegInit(VecInit(Seq.fill(num_warp)(0.U(TAG_WIDTH.W))))
  val frontendOutstandingGen = RegInit(VecInit(Seq.fill(num_warp)(0.U(frontend_gen_width.W))))
  val acceptIcacheRsp = Wire(Bool())
  val pcReqArb = Module(new RRArbiter(new ICachePipeReq_np, num_subcore))

  val flushMasks = (0 until num_subcore).map { sc =>
    val flushWid = internalWid(sc, io.flush(sc).bits)
    Mux(io.flush(sc).valid, UIntToOH(flushWid, num_warp), 0.U(num_warp.W))
  }
  val launchFlushMask =
    Mux(io.launch_flush.valid, UIntToOH(io.launch_flush.bits, num_warp), 0.U(num_warp.W))
  val frontendFlushMask = flushMasks.reduce(_ | _) | launchFlushMask
  val frontendFlushMaskForReqBlock = RegNext(frontendFlushMask, 0.U(num_warp.W))
  val frontendFlushMaskForRspDrop = RegNext(frontendFlushMask, 0.U(num_warp.W))
  io.frontend_flush_mask := frontendFlushMask

  val externalFlushMasks = (0 until num_subcore).map { sc =>
    val flushWid = internalWid(sc, io.flush(sc).bits)
    val flushCacheWid = internalWid(sc, io.flush_cache(sc).bits)
    Seq(io.flush(sc).valid -> flushWid, io.flush_cache(sc).valid -> flushCacheWid)
  }.reduce(_ ++ _)
  io.external_flush_pipe.valid := externalFlushMasks.map(_._1).reduce(_ || _)
  io.external_flush_pipe.bits := PriorityMux(externalFlushMasks)

  val rspKillMask = Mux(io.rsp_kill.valid, UIntToOH(io.rsp_kill.bits, num_warp), 0.U(num_warp.W))
  val frontendOutstandingClearMask = frontendFlushMask | rspKillMask

  when(frontendOutstandingClearMask.orR) {
    for (wid <- 0 until num_warp) {
      when(frontendOutstandingClearMask(wid)) {
        frontendOutstanding(wid) := false.B
        frontendOutstandingAddr(wid) := 0.U
        frontendOutstandingSource(wid) := 0.U
        frontendOutstandingWfTag(wid) := 0.U
        frontendOutstandingGen(wid) := 0.U
      }
    }
  }

  for (sc <- 0 until num_subcore) {
    val reqWid = io.pc_req(sc).bits.warpid
    val reqBlocked = frontendOutstanding(reqWid) ||
      frontendFlushMask(reqWid) || frontendFlushMaskForReqBlock(reqWid)
    pcReqArb.io.in(sc).valid := io.pc_req(sc).valid && !reqBlocked
    pcReqArb.io.in(sc).bits := io.pc_req(sc).bits
    pcReqArb.io.in(sc).bits.source := sc.U
    pcReqArb.io.in(sc).bits.wf_tag := io.wg_tag_lookup(reqWid)
    pcReqArb.io.in(sc).bits.frontend_gen := io.frontend_gen_lookup(reqWid)
    io.pc_req(sc).ready := pcReqArb.io.in(sc).ready && !reqBlocked
  }

  io.icache_req.valid := pcReqArb.io.out.valid
  io.icache_req.bits := pcReqArb.io.out.bits
  pcReqArb.io.out.ready := io.icache_req.ready

  when(io.icache_req.fire) {
    frontendOutstanding(io.icache_req.bits.warpid) := true.B
    frontendOutstandingAddr(io.icache_req.bits.warpid) := io.icache_req.bits.addr
    frontendOutstandingSource(io.icache_req.bits.warpid) := io.icache_req.bits.source
    frontendOutstandingWfTag(io.icache_req.bits.warpid) := io.icache_req.bits.wf_tag
    frontendOutstandingGen(io.icache_req.bits.warpid) := io.icache_req.bits.frontend_gen
  }

  val rspWid = io.icache_rsp.bits.warpid
  val rspOwner = PipeSubcoreHelpers.subcoreValue(rspWid)
  val rspOutstandingMatch = frontendOutstanding(rspWid)
  val rspAddrMatch = frontendOutstandingAddr(rspWid) === io.icache_rsp.bits.addr
  val rspSourceMatch = frontendOutstandingSource(rspWid) === io.icache_rsp.bits.source
  val rspWfTagMatch = io.icache_rsp.bits.wf_tag === frontendOutstandingWfTag(rspWid)
  val rspFrontendGenMatch = io.icache_rsp.bits.frontend_gen === frontendOutstandingGen(rspWid)
  val rspFlushHit = frontendFlushMaskForRspDrop(rspWid)
  val rspKillHit = io.rsp_kill.valid && io.rsp_kill.bits === rspWid
  acceptIcacheRsp := io.icache_rsp.valid &&
    rspOutstandingMatch &&
    rspAddrMatch &&
    rspSourceMatch &&
    rspWfTagMatch &&
    rspFrontendGenMatch &&
    !rspFlushHit &&
    !rspKillHit

  io.accept_icache_rsp := acceptIcacheRsp
  io.icache_rsp.ready := Mux(acceptIcacheRsp, io.ibuffer_ready_for_rsp, true.B)

  when(io.icache_rsp.fire && acceptIcacheRsp) {
    frontendOutstanding(rspWid) := false.B
  }

  for (sc <- 0 until num_subcore) {
    val rspHitsOwner = rspOwner === sc.U
    io.pc_rsp(sc).valid := acceptIcacheRsp && rspHitsOwner
    io.pc_rsp(sc).bits := io.icache_rsp.bits
    io.pc_rsp(sc).bits.status := Mux(io.ibuffer_ready_for_rsp, io.icache_rsp.bits.status, 1.U(2.W))
  }

}

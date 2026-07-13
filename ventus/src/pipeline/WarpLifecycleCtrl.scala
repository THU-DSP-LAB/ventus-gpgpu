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

class WarpLifecycleLaunchEvent extends Bundle {
  val wid = UInt(depth_warp.W)
  val wf_tag = UInt(TAG_WIDTH.W)
  val wg_wf_count = UInt(log2Ceil(CTA_SCHE_CONFIG.WG.NUM_WF_MAX + 1).W)
}

class WarpLifecycleCtrlIO extends Bundle {
  val launch = Input(Valid(new WarpLifecycleLaunchEvent))
  val complete = Input(Valid(UInt(depth_warp.W)))

  val branch_jump = Input(Vec(num_subcore, Valid(UInt(depth_warp.W))))
  val barrier_arrive = Input(Vec(num_subcore, Valid(UInt(depth_warp.W))))
  val endprg = Input(Vec(num_subcore, Valid(UInt(depth_warp.W))))

  val flush_dcache = DecoupledIO(Bool())

  val wg_tag_lookup = Output(Vec(num_warp, UInt(TAG_WIDTH.W)))
  val frontend_gen_lookup = Output(Vec(num_warp, UInt(16.W)))
  val recently_freed = Output(UInt(num_warp.W))
  val global_barrier_wait = Output(UInt(num_warp.W))
  val barrier_release_mask = Output(UInt(num_warp.W))
  val wg_id_lookup = Output(UInt(depth_warp.W))
}

@instantiable
class WarpLifecycleCtrl extends Module {
  @public val io = IO(new WarpLifecycleCtrlIO)

  private val wfIdWidth = log2Ceil(num_warp_in_a_block)

  val wgTagTable = RegInit(VecInit(Seq.fill(num_warp)(0.U(TAG_WIDTH.W))))
  val frontendGenTable = RegInit(VecInit(Seq.fill(num_warp)(0.U(16.W))))
  val recentlyFreed = RegInit(0.U(num_warp.W))

  val globalWarpBarCur = RegInit(VecInit(Seq.fill(num_block)(0.U(num_warp_in_a_block.W))))
  val globalWarpBarExp = RegInit(VecInit(Seq.fill(num_block)(0.U(num_warp_in_a_block.W))))
  val globalWarpBarBelong = RegInit(VecInit(Seq.fill(num_block)(0.U(num_warp.W))))
  val globalBarrierWait = RegInit(0.U(num_warp.W))
  val globalWarpEndCnt = RegInit(VecInit(Seq.fill(num_block)(0.U(num_warp.W))))
  val globalWarpWgValid = RegInit(VecInit(Seq.fill(num_block)(false.B)))

  io.wg_tag_lookup := wgTagTable
  io.frontend_gen_lookup := frontendGenTable
  io.recently_freed := recentlyFreed
  io.global_barrier_wait := globalBarrierWait

  val needFlushMask = Wire(Vec(num_block, Bool()))
  for (i <- 0 until num_block) {
    needFlushMask(i) := (globalWarpEndCnt(i).orR === false.B) && globalWarpWgValid(i)
  }
  val flushDCacheValid = needFlushMask.asUInt.orR
  val flushDCacheEntry = OHToUInt(needFlushMask.asUInt)
  io.flush_dcache.valid := flushDCacheValid
  io.flush_dcache.bits := flushDCacheValid

  val barrierWgIdVec = Wire(Vec(num_subcore, UInt((TAG_WIDTH - wfIdWidth).W)))
  val barrierWfIdVec = Wire(Vec(num_subcore, UInt(wfIdWidth.W)))
  for (sc <- 0 until num_subcore) {
    val barrierTag = wgTagTable(io.barrier_arrive(sc).bits)
    barrierWgIdVec(sc) := barrierTag(TAG_WIDTH - 1, wfIdWidth)
    barrierWfIdVec(sc) := barrierTag(wfIdWidth - 1, 0)
  }

  val barrierArriveMask = (0 until num_subcore)
    .map { sc =>
      Mux(
        io.barrier_arrive(sc).valid,
        UIntToOH(io.barrier_arrive(sc).bits, num_warp),
        0.U(num_warp.W)
      )
    }
    .reduce(_ | _)

  val barrierReleasePerWg = Wire(Vec(num_block, UInt(num_warp.W)))
  barrierReleasePerWg.foreach(_ := 0.U)
  for (wg <- 0 until num_block) {
    val arrivals = (0 until num_subcore)
      .map { sc =>
        Mux(
          io.barrier_arrive(sc).valid && (barrierWgIdVec(sc) === wg.U),
          UIntToOH(barrierWfIdVec(sc), num_warp_in_a_block),
          0.U(num_warp_in_a_block.W)
        )
      }
      .reduce(_ | _)
    val barrierComplete =
      arrivals.orR && ((globalWarpBarCur(wg) | arrivals) === globalWarpBarExp(wg))

    when(arrivals.orR) {
      globalWarpBarCur(wg) := Mux(barrierComplete, 0.U, globalWarpBarCur(wg) | arrivals)
    }
    barrierReleasePerWg(wg) := Mux(barrierComplete, globalWarpBarBelong(wg), 0.U)
  }
  val barrierReleaseMask = barrierReleasePerWg.reduce(_ | _)
  io.barrier_release_mask := barrierReleaseMask

  val wgLookupCandidates = (0 until num_subcore).flatMap { sc =>
    Seq(
      io.barrier_arrive(sc).valid -> io.barrier_arrive(sc).bits,
      io.endprg(sc).valid -> io.endprg(sc).bits
    )
  }
  io.wg_id_lookup := MuxCase(0.U(depth_warp.W), wgLookupCandidates)

  when(io.launch.valid) {
    val newWgId = io.launch.bits.wf_tag(TAG_WIDTH - 1, wfIdWidth)
    val newWgWfCount = io.launch.bits.wg_wf_count
    val newWarpMask = UIntToOH(io.launch.bits.wid, num_warp)

    wgTagTable(io.launch.bits.wid) := io.launch.bits.wf_tag
    frontendGenTable(io.launch.bits.wid) := frontendGenTable(io.launch.bits.wid) + 1.U
    globalWarpBarBelong(newWgId) := globalWarpBarBelong(newWgId) | newWarpMask
    when(!globalWarpBarBelong(newWgId).orR) {
      globalWarpBarCur(newWgId) := 0.U
      globalWarpBarExp(newWgId) := (1.U << newWgWfCount).asUInt - 1.U
    }
    globalWarpEndCnt(newWgId) := globalWarpEndCnt(newWgId) | newWarpMask
    globalWarpWgValid(newWgId) := true.B
  }

  when(io.complete.valid) {
    val endTag = wgTagTable(io.complete.bits)
    val endWgId = endTag(TAG_WIDTH - 1, wfIdWidth)
    val endWarpMask = UIntToOH(io.complete.bits, num_warp)

    globalWarpBarBelong(endWgId) := globalWarpBarBelong(endWgId) & (~endWarpMask).asUInt
    globalWarpEndCnt(endWgId) := globalWarpEndCnt(endWgId) & (~endWarpMask).asUInt
  }

  val recentAllocMask =
    Mux(io.launch.valid, UIntToOH(io.launch.bits.wid, num_warp), 0.U(num_warp.W))
  val recentFreeMask =
    Mux(io.complete.valid, UIntToOH(io.complete.bits, num_warp), 0.U(num_warp.W))
  recentlyFreed := (recentlyFreed | recentFreeMask) & (~recentAllocMask).asUInt

  when(flushDCacheValid && io.flush_dcache.ready) {
    globalWarpWgValid(flushDCacheEntry) := false.B
  }

  for (sc <- 0 until num_subcore) {
    when(io.branch_jump(sc).valid) {
      frontendGenTable(io.branch_jump(sc).bits) :=
        frontendGenTable(io.branch_jump(sc).bits) + 1.U
    }
  }

  globalBarrierWait := (globalBarrierWait | barrierArriveMask) & (~barrierReleaseMask).asUInt
}

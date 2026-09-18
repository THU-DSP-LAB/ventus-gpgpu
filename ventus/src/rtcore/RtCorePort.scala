/*
 * RTcore module boundary for the evolving Ventus SM pipeline.
 *
 * This file intentionally defines bundles only: it lets the decoder/issue path,
 * RTcore implementation, RT Local SRAM service, and cache adapter evolve independently.
 * The interface is wired into `pipe.scala`; its local-SRAM and performance
 * event ports deliberately stay distinct from generic GPGPU LSU traffic.  The
 * interface is described by
 * docs/rtcore/microarchitecture/rtcore-hardware-microarchitecture.md.
 */
package rtcore

import chisel3._
import chisel3.util._
import lsu.{LsuClientRequest, LsuClientResponse}
import top.parameters._
import rtcore.RtCorePortParameters.{regidx_totalwidth => regidx_totalwidth}

object RtCorePortParameters {
  // RTcore accepts one warp command at a time.  This is deliberately
  // independent of the number of resident SIMT warps in the enclosing SM.
  val residentWarpCount = 1
  val residentWarpIdWidth = math.max(1, log2Ceil(residentWarpCount))
  val issueIdWidth = 8
  // Intersection pipelines use this as an internal ownership tag; it is not
  // an LSU request identifier.
  val requestIdWidth = 8
  val lineWordCount = dcache_BlockWords
  val lineBytes = lineWordCount * (xLen / 8)
  require(lineBytes == 128, "RTcore reads use one 128-byte DCache line")
  val regidx_totalwidth = regidx_width + regext_width
}

/** Warp-granular representation of a decoded `vt.rt.traverse`. */
class RtWarpCommand extends Bundle {
  val warpId = UInt(depth_warp.W)
  val issueId = UInt(RtCorePortParameters.issueIdWidth.W)
  val destinationVd = UInt(regidx_totalwidth.W)
  val activeMask = UInt(num_thread.W)
}

/** Completion consumed by vector writeback and the warp scoreboard. */
class RtWarpCompletion extends Bundle {
  val warpId = UInt(depth_warp.W)
  val issueId = UInt(RtCorePortParameters.issueIdWidth.W)
  val destinationVd = UInt(regidx_totalwidth.W)
  val completionMask = UInt(num_thread.W)
  val laneStatus = Vec(num_thread, UInt(xLen.W))
  // Physical traversal state has been drained and reclaimed. This is not
  // the software session's rt.release; its per-warp Local SRAM remains live.
  val contextReleased = Bool()
}

/** A masked end-of-session acknowledgement request from `vt.rt.release`. */
class RtReleaseCommand extends Bundle {
  val warpId = UInt(depth_warp.W)
  val activeMask = UInt(num_thread.W)
}

/** Release retires the complete original mask of the retained RT session. */
class RtReleaseAck extends Bundle {
  val warpId = UInt(depth_warp.W)
  val releasedMask = UInt(num_thread.W)
}

/**
  * One-cycle RTcore-internal PMU events.  They deliberately expose completed
  * handshakes rather than inferred state so `pipe` can keep the common
  * performance-window/reset policy without confusing RT Local SRAM traffic
  * with the generic pipeline LSU.
  */
class RtCorePerfEvents extends Bundle {
  val detailed = if (PMU_DETAILED) Some(new RtDetailedSignals) else None
  val localSramReadRequest = Bool()
  val localSramWriteRequest = Bool()
  val localSramReadResponse = Bool()
  val localSramBackpressure = Bool()
  val raySetupOutput = Bool()
  val asLineAcquire = Bool()
  val asLineReady = UInt(log2Ceil(num_thread + 1).W)
  val asAcquireBackpressure = Bool()
  val asCacheResidentHit = Bool()
  val asCachePendingMerge = Bool()
  val asCacheMiss = Bool()
  val asCacheRefill = Bool()
  val asCacheEviction = Bool()
  val box4Issue = UInt(log2Ceil(RtCoreSchedulerParameters.boxUnitCount + 1).W)
  val box4Result = UInt(log2Ceil(RtCoreSchedulerParameters.boxUnitCount + 1).W)
  val triangleIssue = UInt(log2Ceil(RtCoreSchedulerParameters.triangleUnitCount + 1).W)
  val triangleResult = UInt(log2Ceil(RtCoreSchedulerParameters.triangleUnitCount + 1).W)
  val triangleResolveIssue = Bool()
  val triangleResolveResult = Bool()
  val stackPush = UInt(log2Ceil(num_thread + 1).W)
  val stackPop = UInt(log2Ceil(num_thread + 1).W)
}

/**
 * All externally visible RTcore ports.
 *
 * RtLsu is the sole RTcore-to-LSU boundary.  It is a cacheline client: DCache
 * tags remain LSU-private; opaque client tags identify AS entries on return.
 */
class RtCoreIO extends Bundle {
  val command = Flipped(Decoupled(new RtWarpCommand))
  val completion = Decoupled(new RtWarpCompletion)
  val release = Flipped(Decoupled(new RtReleaseCommand))
  val releaseAck = Decoupled(new RtReleaseAck)

  val localRequest = Decoupled(new RtLocalSramRequest)
  val localResponse = Flipped(Decoupled(new RtLocalSramResponse))

  val lsuRequest = Decoupled(new LsuClientRequest)
  val lsuResponse = Flipped(Decoupled(new LsuClientResponse))
  val perf = Output(new RtCorePerfEvents)
}

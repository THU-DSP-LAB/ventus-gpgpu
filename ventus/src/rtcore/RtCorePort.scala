/*
 * RTcore module boundary for the evolving Ventus SM pipeline.
 *
 * This file intentionally defines bundles only: it lets the decoder/issue path,
 * RTcore implementation, PDS service, and cache adapter evolve independently.
 * It is not wired into pipe.scala yet, so it cannot alter existing GPGPU timing
 * or LSU behaviour.  The interface is described by
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
  val pdsWriteWordCount = 16
  val pdsWriteBytes = pdsWriteWordCount * (xLen / 8)
  // MSHR capacity is transaction concurrency, not warp residency.
  val rtLsuMshrEntryCount = 16
  require(lineBytes == 128, "RTcore reads use one 128-byte DCache line")
  require(pdsWriteBytes == 64, "RTcore PDS writes are fixed 64-byte blocks")
  val regidx_totalwidth = regidx_width + regext_width
}

/**
  * PDS context captured for one issued warp.
  *
  * Field-major PDS reads derive their address from this context and the
  * requested field word. PDS writes carry their already-computed 64-byte block
  * address and mask directly. Every fixed-header field, including both
  * hit-records, is field-major.
  */
class RtPdsContext extends Bundle {
  val baseAddress = UInt(xLen.W)
  val numWarps = UInt(xLen.W)
  val numThreads = UInt(xLen.W)
  val pdsWarpTidBase = UInt(xLen.W)
}

/** Warp-granular representation of a decoded `vt.rt.traverse`. */
class RtWarpCommand extends Bundle {
  val warpId = UInt(depth_warp.W)
  val issueId = UInt(RtCorePortParameters.issueIdWidth.W)
  val destinationVd = UInt(regidx_totalwidth.W)
  val activeMask = UInt(num_thread.W)
  val pds = new RtPdsContext
}

/** Completion consumed by vector writeback and the warp scoreboard. */
class RtWarpCompletion extends Bundle {
  val warpId = UInt(depth_warp.W)
  val issueId = UInt(RtCorePortParameters.issueIdWidth.W)
  val destinationVd = UInt(regidx_totalwidth.W)
  val completionMask = UInt(num_thread.W)
  val laneStatus = Vec(num_thread, UInt(xLen.W))
}

/**
 * All externally visible RTcore ports.
 *
 * RtLsu is the sole RTcore-to-LSU boundary.  It is a cacheline client: DCache
 * tags and RtLsuMshr entry IDs are both private to their respective modules.
 */
class RtCoreIO extends Bundle {
  val command = Flipped(Decoupled(new RtWarpCommand))
  val completion = Decoupled(new RtWarpCompletion)

  val lsuRequest = Decoupled(new LsuClientRequest)
  val lsuResponse = Flipped(Decoupled(new LsuClientResponse))
}

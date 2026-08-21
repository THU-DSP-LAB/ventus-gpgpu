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
import top.parameters._
import rtcore.RtCorePortParameters.{regidx_totalwidth => regidx_totalwidth}

object RtCorePortParameters {
  // These are RTcore-local tags, not aliases for LSU or DCache MSHR indices.
  val issueIdWidth = 8
  val requestIdWidth = 8
  val generationWidth = 4
  val pdsWordOffsetWidth = 10
  val lineWordCount = dcache_BlockWords
  val regidx_totalwidth = regidx_width + regext_width
}

/** Context needed to translate a logical PDS slot for one issued warp. */
class RtPdsContext extends Bundle {
  val baseAddress = UInt(xLen.W)
  val numWarps = UInt(xLen.W)
  val numThreads = UInt(xLen.W)
  val threadId = UInt(xLen.W)
}

/** Warp-granular representation of a decoded `vt.rt.traverse`. */
class RtWarpCommand extends Bundle {
  val warpId = UInt(depth_warp.W)
  val issueId = UInt(RtCorePortParameters.issueIdWidth.W)
  val destinationVd = UInt(regidx_totalwidth.W)
  val activeMask = UInt(num_thread.W)
  val pdsSlots = Vec(num_thread, UInt(xLen.W))
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

/** Fixed-ABI PDS header operation.  It is deliberately separate from AS reads. */
class RtPdsRequest extends Bundle {
  val requestId = UInt(RtCorePortParameters.requestIdWidth.W)
  val generation = UInt(RtCorePortParameters.generationWidth.W)
  val wordOffset = UInt(RtCorePortParameters.pdsWordOffsetWidth.W)
  val write = Bool()
  val laneMask = UInt(num_thread.W)
  val slots = Vec(num_thread, UInt(xLen.W))
  val writeData = Vec(num_thread, UInt(xLen.W))
}

class RtPdsResponse extends Bundle {
  val requestId = UInt(RtCorePortParameters.requestIdWidth.W)
  val generation = UInt(RtCorePortParameters.generationWidth.W)
  val laneMask = UInt(num_thread.W)
  val readData = Vec(num_thread, UInt(xLen.W))
  val faultMask = UInt(num_thread.W)
}

/** Cache-line read transaction emitted by the AS fetch engine. */
class RtAsReadRequest extends Bundle {
  val requestId = UInt(RtCorePortParameters.requestIdWidth.W)
  val generation = UInt(RtCorePortParameters.generationWidth.W)
  val lineAddress = UInt(memReqLen.W)
  val wordMask = UInt(RtCorePortParameters.lineWordCount.W)
}

class RtAsReadResponse extends Bundle {
  val requestId = UInt(RtCorePortParameters.requestIdWidth.W)
  val generation = UInt(RtCorePortParameters.generationWidth.W)
  val data = Vec(RtCorePortParameters.lineWordCount, UInt(xLen.W))
  val fault = Bool()
}

/**
 * All externally visible RTcore ports.
 *
 * `as` is an adapter-level interface, not the current DCacheCoreReq_np port:
 * the present cache port has only LSU-sized instrId tags and cannot preserve an
 * RT request's source/generation across multiple outstanding reads.
 */
class RtCoreIO extends Bundle {
  val command = Flipped(Decoupled(new RtWarpCommand))
  val completion = Decoupled(new RtWarpCompletion)

  val pdsRequest = Decoupled(new RtPdsRequest)
  val pdsResponse = Flipped(Decoupled(new RtPdsResponse))

  val asReadRequest = Decoupled(new RtAsReadRequest)
  val asReadResponse = Flipped(Decoupled(new RtAsReadResponse))

  val busy = Output(Bool())
}

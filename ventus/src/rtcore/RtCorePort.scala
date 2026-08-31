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
  val pdsWordOffsetWidth = 10
  val lineWordCount = dcache_BlockWords
  val pdsWriteWordCount = 16
  val pdsWriteBytes = pdsWriteWordCount * (xLen / 8)
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

/** Field-major PDS read for one fixed-header word across a warp. */
class RtPdsReadRequest extends Bundle {
  val requestId = UInt(RtCorePortParameters.requestIdWidth.W)
  val fieldWordOffset = UInt(RtCorePortParameters.pdsWordOffsetWidth.W)
  val readLaneMask = UInt(num_thread.W)
  val pds = new RtPdsContext
}

/** Physical 64-byte PDS write prepared by the hit-record write table. */
class RtPdsWriteRequest extends Bundle {
  val blockAddress = UInt(memReqLen.W)
  val wordMask = UInt(RtCorePortParameters.pdsWriteWordCount.W)
  val data = Vec(
    RtCorePortParameters.pdsWriteWordCount,
    UInt(xLen.W)
  )
}

/**
 * Legacy combined PDS operation used only by RtPdsTable during migration.
 * New RTcore interfaces use RtPdsReadRequest and RtPdsWriteRequest directly.
 */
class RtPdsRequest extends Bundle {
  val requestId = UInt(RtCorePortParameters.requestIdWidth.W)
  val write = Bool()
  val fieldWordOffset = UInt(RtCorePortParameters.pdsWordOffsetWidth.W)
  val readLaneMask = UInt(num_thread.W)
  val writeBlockAddress = UInt(memReqLen.W)
  val writeWordMask = UInt(RtCorePortParameters.pdsWriteWordCount.W)
  val pds = new RtPdsContext
  val writeData = Vec(
    RtCorePortParameters.pdsWriteWordCount,
    UInt(xLen.W)
  )
}

/** A PDS field-major read response; readData(lane) is that lane's field word. */
class RtPdsResponse extends Bundle {
  val requestId = UInt(RtCorePortParameters.requestIdWidth.W)
  val laneMask = UInt(num_thread.W)
  val readData = Vec(num_thread, UInt(xLen.W))
  val faultMask = UInt(num_thread.W)
}

/** Cache-line read transaction emitted by the AS fetch engine. */
class RtAsReadRequest extends Bundle {
  val requestId = UInt(RtCorePortParameters.requestIdWidth.W)
  val lineAddress = UInt(memReqLen.W)
  val wordMask = UInt(RtCorePortParameters.lineWordCount.W)
}

class RtAsReadResponse extends Bundle {
  val requestId = UInt(RtCorePortParameters.requestIdWidth.W)
  val data = Vec(RtCorePortParameters.lineWordCount, UInt(xLen.W))
  val fault = Bool()
}

/**
 * All externally visible RTcore ports.
 *
 * `as` is an adapter-level interface, not the current DCacheCoreReq_np port:
 * the present cache port has only LSU-sized instrId tags and cannot preserve an
 * RT request's source identity across multiple outstanding reads.
 */
class RtCoreIO extends Bundle {
  val command = Flipped(Decoupled(new RtWarpCommand))
  val completion = Decoupled(new RtWarpCompletion)

  val pdsReadRequest = Decoupled(new RtPdsReadRequest)
  val pdsWriteRequest = Decoupled(new RtPdsWriteRequest)
  val pdsResponse = Flipped(Decoupled(new RtPdsResponse))

  val asReadRequest = Decoupled(new RtAsReadRequest)
  val asReadResponse = Flipped(Decoupled(new RtAsReadResponse))

  val busy = Output(Bool())
}

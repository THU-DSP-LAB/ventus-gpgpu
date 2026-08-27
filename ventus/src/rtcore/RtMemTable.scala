/*
 * Shared RTcore memory-transaction state.
 *
 * One entry represents one line-aligned DCache transaction after PDS address
 * generation or AS line coalescing.  It is not an L1D MSHR: L1D retains
 * ownership of cache-miss tracking, while this table retains RTcore request
 * identity and response fan-out.
 */
package rtcore

import chisel3._
import chisel3.util._
import top.parameters._

object RtMemTableParameters {
  // Initial RTcore resource point: PDS and AS share these entries.
  val entryCount = 8

  // A merged AS line can wake this many logical AS fetch requests.
  val maxAsWaitersPerEntry = 2

  val lineWordCount = dcache_BlockWords
  val lineBytes = lineWordCount * (xLen / 8)

  /*
   * A field-major PDS read has exactly one word for every local lane.  If
   * either width changes, the PDS address/response contract must be redesigned
   * explicitly instead of silently reintroducing laneWordIndex metadata.
   */
  require(
    lineWordCount == num_thread,
    "RtMemTable field-major PDS reads require one DCache word per local lane"
  )
}

/** Request source retained by one shared RtMemTable entry. */
object RtMemKind {
  val pds = 0.U(1.W)
  val as = 1.U(1.W)
}

/** One logical AS request waiting for a coalesced cache-line response. */
class RtAsLineWaiter extends Bundle {
  val requestId = UInt(RtCorePortParameters.requestIdWidth.W)
  val wordMask = UInt(RtMemTableParameters.lineWordCount.W)
}

/**
 * PDS read information retained with an already-computed cache-line request.
 *
 * Reads are field-major: one field word is fetched for every lane, so the
 * returned DCache line is already indexed by local lane number.  PDS writes
 * arrive with their cache-line address and payload already computed by RTcore.
 */
class RtPdsLineMetadata extends Bundle {
  val write = Bool()
  // Valid for reads: a field-major PDS header word shared by the warp.
  val fieldWordOffset = UInt(RtCorePortParameters.pdsWordOffsetWidth.W)
  val readLaneMask = UInt(num_thread.W)
}

/**
 * State for one line-aligned transaction between RTcore and the DCache client
 * arbiter.
 *
 * lineAddress is a line-aligned byte address in the DCache address domain;
 * with the current 32-word, 4-byte-word DCache, bits [6:0] are zero.  PDS
 * reads, PDS compact writes, and AS reads all allocate exactly one shared
 * dcache_BlockWords payload below.  The fields before cacheLineData explain
 * how that common line is interpreted; they are not another payload.
 *
 * State encoding:
 *   !valid                         : free entry
 *   valid && !cacheRequestSent     : allocated, awaiting DCache acceptance
 *   valid && cacheRequestSent && !cacheResponseValid
 *                                  : awaiting DCache response
 *   valid && cacheResponseValid    : response buffered for PDS or AS output
 */
class RtMemTableEntry extends Bundle {
  val valid = Bool()
  val cacheRequestSent = Bool()
  val cacheResponseValid = Bool()

  val kind = UInt(1.W)
  val lineAddress = UInt(memReqLen.W)
  val requestId = UInt(RtCorePortParameters.requestIdWidth.W)
  // Valid/dirty words for a compact PDS write, or words requested by AS.
  val cacheWordMask = UInt(RtMemTableParameters.lineWordCount.W)

  // Used when kind === RtMemKind.pds; physically present for a uniform entry.
  val pds = new RtPdsLineMetadata

  // Used when kind === RtMemKind.as.  Every valid waiter receives the same
  // cache line once cacheResponseValid is asserted.
  val asWaiterValid = Vec(RtMemTableParameters.maxAsWaitersPerEntry, Bool())
  val asWaiters = Vec(
    RtMemTableParameters.maxAsWaitersPerEntry,
    new RtAsLineWaiter
  )

  /*
   * The sole cache-line payload of an entry.  Before DCache acceptance it
   * contains a PDS compact write line; after a read response it contains the
   * returned line for either PDS lane fan-out or AS waiter fan-out.
   */
  val cacheLineData = Vec(
    RtMemTableParameters.lineWordCount,
    UInt(xLen.W)
  )
  val responseFault = Bool()
}

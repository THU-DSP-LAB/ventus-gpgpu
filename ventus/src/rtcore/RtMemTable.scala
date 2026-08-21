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
  val entryIdWidth = log2Ceil(entryCount)

  // A merged AS line can wake this many logical AS fetch requests.
  val maxAsWaitersPerEntry = 4

  val lineWordCount = dcache_BlockWords
  val lineWordIndexWidth = log2Ceil(lineWordCount)
}

/** Request source retained by one shared RtMemTable entry. */
object RtMemKind {
  val pds = 0.U(1.W)
  val as = 1.U(1.W)
}

/**
 * Opaque transaction identity carried through the DCache client arbiter.
 *
 * `entryId` indexes the table.  `generation` rejects a late response after
 * the entry has been released and reallocated.
 */
class RtMemTableTag extends Bundle {
  val entryId = UInt(RtMemTableParameters.entryIdWidth.W)
  val generation = UInt(RtCorePortParameters.generationWidth.W)
}

/** One logical AS request waiting for a coalesced cache-line response. */
class RtAsLineWaiter extends Bundle {
  val requestId = UInt(RtCorePortParameters.requestIdWidth.W)
  val generation = UInt(RtCorePortParameters.generationWidth.W)
  val wordMask = UInt(RtMemTableParameters.lineWordCount.W)
}

/**
 * PDS information needed after address generation has reduced a request to
 * one cache line.  `laneWordIndex` selects each active lane's word in that
 * line; inactive lanes are ignored through `laneMask`.
 */
class RtPdsLineMetadata extends Bundle {
  val requestId = UInt(RtCorePortParameters.requestIdWidth.W)
  val generation = UInt(RtCorePortParameters.generationWidth.W)
  val write = Bool()
  val laneMask = UInt(num_thread.W)
  val laneWordIndex = Vec(
    num_thread,
    UInt(RtMemTableParameters.lineWordIndexWidth.W)
  )
  val writeData = Vec(num_thread, UInt(xLen.W))
}

/**
 * State for one line-aligned transaction between RTcore and the DCache client
 * arbiter.
 *
 * `lineAddress` is a line-aligned byte address in the DCache address domain;
 * with the current 32-word, 4-byte-word DCache, bits [6:0] are zero.
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
  val generation = UInt(RtCorePortParameters.generationWidth.W)
  val lineAddress = UInt(memReqLen.W)
  val wordMask = UInt(RtMemTableParameters.lineWordCount.W)

  // Used when kind === RtMemKind.pds; physically present for a uniform entry.
  val pds = new RtPdsLineMetadata

  // Used when kind === RtMemKind.as.  Each valid waiter receives the same
  // returned line; responseSent lets the table drain them over multiple cycles.
  val asWaiterValid = Vec(RtMemTableParameters.maxAsWaitersPerEntry, Bool())
  val asWaiterResponseSent = Vec(RtMemTableParameters.maxAsWaitersPerEntry, Bool())
  val asWaiters = Vec(
    RtMemTableParameters.maxAsWaitersPerEntry,
    new RtAsLineWaiter
  )

  // Required because DCache, PDS response, and AS response use Decoupled
  // handshakes: a response may need to remain resident while an output stalls.
  val responseData = Vec(
    RtMemTableParameters.lineWordCount,
    UInt(xLen.W)
  )
  val responseFault = Bool()
}

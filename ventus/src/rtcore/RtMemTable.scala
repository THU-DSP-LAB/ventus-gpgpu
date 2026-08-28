/*
 * RTcore cache-transaction state.
 *
 * PDS admission and AS fetches share the DCache client, but not a response
 * holding bank. Their request identities, merge rules, and consumer lifetimes
 * differ, so each kind has its own compact entry type below.
 */
package rtcore

import chisel3._
import chisel3.util._
import top.parameters._

object RtMemTableParameters {
  /*
   * Initial resource point. PDS reads/writes use a small ping-pong bank: a
   * returned field-major line is consumed by ray setup and released. AS
   * entries retain a returned line until all logical fetch waiters consume it.
   */
  val pdsEntryCount = 2
  val asEntryCount = 8
  val maxAsWaitersPerEntry = 2

  val lineWordCount = dcache_BlockWords
  val lineBytes = lineWordCount * (xLen / 8)

  /*
   * A field-major PDS read has exactly one word for every local lane. If
   * either width changes, the PDS address/response contract must be redesigned
   * explicitly instead of silently introducing a lane-word index.
   */
  require(
    lineWordCount == num_thread,
    "RtMemTable field-major PDS reads require one DCache word per local lane"
  )

  /*
   * Compatibility default for currently separate, legacy completion queues.
   * New RTcore integration instantiates RtMemTable and names the PDS or AS
   * bank explicitly rather than treating this as a shared-entry count.
   */
  val entryCount = asEntryCount
}

/**
 * Arbitration policy for the common DCache request port.
 *
 * When both banks have issuable work, grant the bank selected by preferPds
 * and flip the preference after that grant. If only one bank is issuable,
 * grant it without changing the preference. Each bank also allocates free
 * entries from its independent round-robin cursor.
 */
object RtMemTableScheduling {
  val preferPdsAfterReset = true
}

/** Mutable scheduling state retained alongside the two entry banks. */
class RtMemTableSchedulingState extends Bundle {
  val preferPds = Bool()
  val nextPdsEntry = UInt(log2Ceil(RtMemTableParameters.pdsEntryCount).W)
  val nextAsEntry = UInt(log2Ceil(RtMemTableParameters.asEntryCount).W)
}

/** One logical AS fetch waiting for a coalesced cache-line response. */
class RtAsLineWaiter extends Bundle {
  val requestId = UInt(RtCorePortParameters.requestIdWidth.W)
  val wordMask = UInt(RtMemTableParameters.lineWordCount.W)
}

/**
 * One PDS cache-line transaction.
 *
 * PDS is not coalesced through AS waiters: reads identify one field-major
 * header word for a warp, while writes already arrive as a computed cache
 * line. A PDS requestId identifies its direct response consumer.
 */
class RtPdsMemTableEntry extends Bundle {
  val valid = Bool()
  val cacheRequestSent = Bool()
  val cacheResponseValid = Bool()

  val lineAddress = UInt(memReqLen.W)
  val requestId = UInt(RtCorePortParameters.requestIdWidth.W)
  val write = Bool()
  val cacheWordMask = UInt(RtMemTableParameters.lineWordCount.W)

  // Valid for reads: the same fixed-header field for every lane in the warp.
  val fieldWordOffset = UInt(RtCorePortParameters.pdsWordOffsetWidth.W)
  val readLaneMask = UInt(num_thread.W)

  /*
   * Before DCache acceptance this is a compact PDS write line. After a PDS
   * read response it is the field-major line indexed by local lane number.
   */
  val cacheLineData = Vec(
    RtMemTableParameters.lineWordCount,
    UInt(xLen.W)
  )
  val responseFault = Bool()
}

/**
 * One coalesced AS cache-line transaction.
 *
 * Multiple logical AS fetches for the same line share its cache request and
 * retained response payload. There is intentionally no entry-wide requestId:
 * every logical consumer owns the requestId in its waiter record.
 */
class RtAsMemTableEntry extends Bundle {
  val valid = Bool()
  val cacheRequestSent = Bool()
  val cacheResponseValid = Bool()

  val lineAddress = UInt(memReqLen.W)
  // OR of all requested words; it is the physical DCache request mask.
  val cacheWordMask = UInt(RtMemTableParameters.lineWordCount.W)

  val waiterValid = Vec(RtMemTableParameters.maxAsWaitersPerEntry, Bool())
  val waiters = Vec(
    RtMemTableParameters.maxAsWaitersPerEntry,
    new RtAsLineWaiter
  )

  // Retained until all valid waiters have consumed the returned AS line.
  val cacheLineData = Vec(
    RtMemTableParameters.lineWordCount,
    UInt(xLen.W)
  )
  val responseFault = Bool()
}

/**
 * Complete RTcore transaction-table storage.
 *
 * This is a Bundle, not a transaction-engine Module: a future controller owns
 * allocation, request issue, response demux, and consumer handshakes. It
 * instantiates this one top-level type and never needs a union entry with
 * PDS-only and AS-only metadata occupying the same record.
 */
class RtMemTable extends Bundle {
  val scheduling = new RtMemTableSchedulingState
  val pdsEntries = Vec(
    RtMemTableParameters.pdsEntryCount,
    new RtPdsMemTableEntry
  )
  val asEntries = Vec(
    RtMemTableParameters.asEntryCount,
    new RtAsMemTableEntry
  )
}

/*
 * Bounded transaction table for lane-major hit-record writes.
 *
 * It stores complete per-lane completion records, never independent record
 * fields.  A slow lane therefore consumes one table entry rather than nine
 * field entries, and an external PDS/cache adapter can drain records later.
 */
package rtcore

import chisel3._
import chisel3.util._

class RtHitRecordWriteTable(
    val entryCount: Int = RtMemTableParameters.entryCount
) extends Module {
  require(entryCount > 0)

  val io = IO(new Bundle {
    val enq = Flipped(Decoupled(new RtHitRecordWrite))
    val deq = Decoupled(new RtHitRecordWrite)
    val occupancy = Output(UInt(log2Ceil(entryCount + 1).W))
  })

  val entries = Module(new Queue(new RtHitRecordWrite, entryCount, pipe = true))
  entries.io.enq <> io.enq
  io.deq <> entries.io.deq
  io.occupancy := entries.io.count
}

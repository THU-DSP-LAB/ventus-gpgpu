/*
 * Internal lane-major hit-record write protocol.
 *
 * This is deliberately separate from RtPdsRequest: the PDS/cache adapter owns
 * physical address formation, while an intersection unit completes one lane's
 * compact candidate or committed record at a time.
 */
package rtcore

import chisel3._
import top.parameters._

object RtHitRecordKind {
  val candidate = 0.U(1.W)
  val committed = 1.U(1.W)
}

/** A complete or masked fragment of one lane-major hit record. */
class RtHitRecordWrite extends Bundle {
  val warpId = UInt(depth_warp.W)
  val laneId = UInt(RtCorePortParameters.laneIdWidth.W)
  val kind = UInt(1.W)
  val wordMask = UInt(RtAbiLayout.HitRecordWordCount.W)
  val words = Vec(RtAbiLayout.HitRecordWordCount, UInt(xLen.W))
  val pds = new RtPdsContext
}

/*
 * Completion port exposed by a future intersection unit.  No RayFlex module
 * is instantiated here: this bundle makes its asynchronous, per-lane result
 * contract explicit before an IU implementation is selected.
 */
class RtIntersectionCompletion extends RtHitRecordWrite

/*
 * RTcore structural sub-top for asynchronous intersection completion.
 *
 * This is not the GPGPU top-level and has no RayFlex dependency.  It defines
 * the integration seam: future disjoint box/triangle IU instances drive the
 * completionIn ports; a PDS/cache adapter drains lane-major record writes.
 */
package rtcore

import chisel3._
import chisel3.util._

class RtCoreIntersectionCluster(
    val intersectionUnitCount: Int = 4,
    val perLaneQueueDepth: Int = 2,
    val writeTableEntries: Int = RtMemTableParameters.entryCount
) extends Module {
  val io = IO(new Bundle {
    val completionIn = Vec(
      intersectionUnitCount,
      Flipped(Decoupled(new RtIntersectionCompletion))
    )
    // Connect to the PDS lane-major write adapter when that adapter exists.
    val recordWrite = Decoupled(new RtHitRecordWrite)
    val busy = Output(Bool())
  })

  val intersectionArray = Module(
    new RtIntersectionArray(intersectionUnitCount, perLaneQueueDepth)
  )
  val writeTable = Module(new RtHitRecordWriteTable(writeTableEntries))

  intersectionArray.io.completionIn <> io.completionIn
  writeTable.io.enq <> intersectionArray.io.recordWrite
  io.recordWrite <> writeTable.io.deq
  io.busy := writeTable.io.occupancy =/= 0.U
}

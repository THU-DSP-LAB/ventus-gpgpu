/*
 * Parallel intersection completion collection.
 *
 * Each IU emits a lane-major record.  The array first arbitrates only among
 * IUs targeting the same lane, then buffers one small FIFO per lane.  This
 * avoids a global IU x lane x record-word crossbar: different lanes can be
 * accepted concurrently and only equal-lane completions contend.
 */
package rtcore

import chisel3._
import chisel3.util._
import top.parameters._

class RtIntersectionArray(
    val intersectionUnitCount: Int = 4,
    val perLaneQueueDepth: Int = 2
) extends Module {
  require(intersectionUnitCount > 0)
  require(perLaneQueueDepth > 0)

  val io = IO(new Bundle {
    // Future box/triangle IUs connect here.  They are not instantiated by
    // this structural module.
    val completionIn = Vec(
      intersectionUnitCount,
      Flipped(Decoupled(new RtIntersectionCompletion))
    )
    val recordWrite = Decoupled(new RtHitRecordWrite)
  })

  val laneArbiters = Seq.fill(num_thread)(
    Module(new RRArbiter(new RtIntersectionCompletion, intersectionUnitCount))
  )
  val laneQueues = Seq.fill(num_thread)(
    Module(new Queue(new RtHitRecordWrite, perLaneQueueDepth, pipe = true))
  )

  for (lane <- 0 until num_thread) {
    for (unit <- 0 until intersectionUnitCount) {
      laneArbiters(lane).io.in(unit).bits := io.completionIn(unit).bits
      laneArbiters(lane).io.in(unit).valid :=
        io.completionIn(unit).valid && io.completionIn(unit).bits.laneId === lane.U
    }
    laneQueues(lane).io.enq.bits := laneArbiters(lane).io.out.bits
    laneQueues(lane).io.enq.valid := laneArbiters(lane).io.out.valid
    laneArbiters(lane).io.out.ready := laneQueues(lane).io.enq.ready
  }

  // A completion can name exactly one lane, hence it receives ready from the
  // corresponding lane-local arbiter only.
  for (unit <- 0 until intersectionUnitCount) {
    io.completionIn(unit).ready := MuxLookup(
      io.completionIn(unit).bits.laneId,
      false.B
    )(
      (0 until num_thread).map { lane =>
        lane.U -> laneArbiters(lane).io.in(unit).ready
      }.toSeq
    )
  }

  // The current write-table interface is one record wide.  Keeping this final
  // arbiter narrow makes the future PDS/cache port width independent of IU
  // count; later banking can replace only this boundary.
  val writeArbiter = Module(new RRArbiter(new RtHitRecordWrite, num_thread))
  for (lane <- 0 until num_thread) {
    writeArbiter.io.in(lane) <> laneQueues(lane).io.deq
  }
  io.recordWrite <> writeArbiter.io.out
}

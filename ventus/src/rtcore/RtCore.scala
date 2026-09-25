package rtcore

import chisel3._

/**
  * Complete internal RTcore datapath.
  *
  * The scheduler owns all traversal and LSU-facing control.  This wrapper
  * supplies its physically independent intersection resources: eight Box4
  * pipelines and two Triangle pipelines.  Decode, scoreboard, and the SM LSU
  * client arbiter remain outside this module.
  */
class RtCore extends Module {
  import RtCoreSchedulerParameters._

  val io = IO(new RtCoreIO)

  val scheduler = Module(new RtCoreScheduler)
  val boxUnits = Seq.tabulate(boxUnitCount) { index =>
    Module(new RtBox4Pipeline).suggestName(s"box4_$index")
  }
  val triangleUnits = Seq.tabulate(triangleUnitCount) { index =>
    Module(new RtTrianglePipeline).suggestName(s"triangle_$index")
  }

  scheduler.io.command <> io.command
  io.completion <> scheduler.io.completion
  scheduler.io.release <> io.release
  io.releaseAck <> scheduler.io.releaseAck
  io.lsuRequest <> scheduler.io.lsuRequest
  scheduler.io.lsuResponse <> io.lsuResponse
  io.localRequest <> scheduler.io.localRequest
  scheduler.io.localResponse <> io.localResponse
  io.localNarrowRequest <> scheduler.io.localNarrowRequest
  scheduler.io.localNarrowResponse <> io.localNarrowResponse
  io.perf := scheduler.io.perf

  for (index <- 0 until boxUnitCount) {
    boxUnits(index).in <> scheduler.boxIssue(index)
    scheduler.boxResult(index) <> boxUnits(index).out
  }
  for (index <- 0 until triangleUnitCount) {
    triangleUnits(index).in <> scheduler.triangleIssue(index)
    scheduler.triangleResult(index) <> triangleUnits(index).out
  }
}

package rtcore

import chisel3._
import chiseltest._
import lsu.LsuClientId
import org.scalatest.freespec.AnyFreeSpec
import top.parameters._

/** Scheduler control-path test with a deterministic fake RtLsu and idle fake IUs. */
class RtCoreSchedulerFlowTest extends AnyFreeSpec with ChiselScalatestTester {
  private def driveIdle(dut: RtCoreScheduler): Unit = {
    dut.io.command.valid.poke(false.B)
    dut.io.completion.ready.poke(true.B)
    dut.io.lsuRequest.ready.poke(false.B)
    dut.io.lsuResponse.valid.poke(false.B)
    dut.io.lsuResponse.bits.clientId.poke(LsuClientId.RtCore)
    dut.io.lsuResponse.bits.lineBase.poke(0.U)
    dut.io.lsuResponse.bits.coverageMask.poke(0.U)
    dut.io.lsuResponse.bits.data.foreach(_.poke(0.U))
    dut.io.lsuResponse.bits.fault.poke(false.B)
    dut.boxIssue.foreach(_.ready.poke(true.B))
    dut.boxResult.foreach { port =>
      port.valid.poke(false.B)
      port.bits.requestId.poke(0.U)
      port.bits.tmin.foreach(_.poke(0.U))
      port.bits.isIntersect.foreach(_.poke(false.B))
      port.bits.boxIndex.foreach(_.poke(0.U))
    }
    dut.triangleIssue.foreach(_.ready.poke(true.B))
    dut.triangleResult.foreach { port =>
      port.valid.poke(false.B)
      port.bits.requestId.poke(0.U)
      port.bits.uNum.poke(0.U)
      port.bits.vNum.poke(0.U)
      port.bits.tNum.poke(0.U)
      port.bits.tDenom.poke(0.U)
      port.bits.hit.poke(false.B)
    }
  }

  private def waitForRequest(dut: RtCoreScheduler, limit: Int = 400): (BigInt, BigInt) = {
    dut.io.lsuRequest.ready.poke(true.B)
    var left = limit
    while (!dut.io.lsuRequest.valid.peek().litToBoolean && left > 0) {
      dut.clock.step()
      left -= 1
    }
    assert(left > 0, s"no LSU request within $limit cycles")
    dut.io.lsuRequest.bits.clientId.expect(LsuClientId.RtCore)
    val address = dut.io.lsuRequest.bits.lineBase.peek().litValue
    val coverage = dut.io.lsuRequest.bits.coverageMask.peek().litValue
    dut.clock.step()
    dut.io.lsuRequest.ready.poke(false.B)
    (address, coverage)
  }

  private def respond(dut: RtCoreScheduler, address: BigInt, coverage: BigInt, data: Seq[BigInt]): Unit = {
    dut.io.lsuResponse.bits.clientId.poke(LsuClientId.RtCore)
    dut.io.lsuResponse.bits.lineBase.poke(address.U)
    dut.io.lsuResponse.bits.coverageMask.poke(coverage.U)
    dut.io.lsuResponse.bits.fault.poke(false.B)
    dut.io.lsuResponse.bits.data.zip(data).foreach { case (word, value) => word.poke(value.U) }
    dut.io.lsuResponse.valid.poke(true.B)
    var left = 20
    while (!dut.io.lsuResponse.ready.peek().litToBoolean && left > 0) {
      dut.clock.step()
      left -= 1
    }
    assert(left > 0, s"LSU response at 0x${address.toString(16)} was not accepted")
    dut.clock.step()
    dut.io.lsuResponse.valid.poke(false.B)
    // The MSHR routes this reply to its owner on the following cycle.
    dut.clock.step()
  }

  "scheduler owns a physical warp through PDS read, header miss, write drain, and completion" in {
    test(new RtCoreScheduler).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      dut.reset.poke(true.B)
      driveIdle(dut)
      dut.clock.step(2)
      dut.reset.poke(false.B)

      val pdsBase = BigInt(0x1000)
      dut.io.command.bits.warpId.poke(5.U)
      dut.io.command.bits.issueId.poke(0x3a.U)
      dut.io.command.bits.destinationVd.poke(7.U)
      dut.io.command.bits.activeMask.poke(1.U)
      dut.io.command.bits.pds.baseAddress.poke(pdsBase.U)
      dut.io.command.bits.pds.numWarps.poke(1.U)
      dut.io.command.bits.pds.numThreads.poke(num_thread.U)
      dut.io.command.bits.pds.pdsWarpTidBase.poke(0.U)
      dut.io.command.valid.poke(true.B)
      dut.io.command.ready.expect(true.B)
      dut.clock.step()
      dut.io.command.valid.poke(false.B)
      dut.io.command.ready.expect(false.B)

      val rayFields = Seq[BigInt](
        0x8000, 0, 0, 0,
        0x3f800000L, 0x40000000L, 0x40400000L, 0,
        0x40800000L, 0x41000000L, 0x41400000L, 0x42c80000L
      )
      var pendingRayFields = rayFields.indices.toSet
      while (pendingRayFields.nonEmpty) {
        val (address, coverage) = waitForRequest(dut)
        val field = ((address - pdsBase) / RtCorePortParameters.lineBytes).toInt - RtAbiLayout.SlotAccelLo
        assert(pendingRayFields.contains(field),
          s"unexpected or duplicate PDS field $field at 0x${address.toString(16)}")
        pendingRayFields -= field
        respond(dut, address, coverage, Seq.fill(RtCorePortParameters.lineWordCount)(rayFields(field)))
      }

      val (headerAddress, headerCoverage) = waitForRequest(dut)
      assert(headerAddress == BigInt(0x8000))
      val header = Array.fill[BigInt](RtCorePortParameters.lineWordCount)(0)
      header((RtVtasLayout.AsHeaderRootNodeRef / 4).toInt) = BigInt(RtVtasLayout.InvalidNodeRef)
      respond(dut, headerAddress, headerCoverage, header.toSeq)

      var left = 100
      while (!dut.io.completion.valid.peek().litToBoolean && left > 0) {
        dut.clock.step()
        left -= 1
      }
      assert(left > 0, "scheduler did not complete the header miss")
      dut.io.completion.bits.warpId.expect(5.U)
      dut.io.completion.bits.issueId.expect(0x3a.U)
      dut.io.completion.bits.destinationVd.expect(7.U)
      dut.io.completion.bits.completionMask.expect(1.U)
      dut.io.completion.bits.laneStatus(0).expect(RtAbiLayout.TraversalCompleteMiss.U)
      dut.clock.step()
    }
  }
}

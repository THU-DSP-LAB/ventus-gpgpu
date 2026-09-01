package rtcore

import chisel3._
import chiseltest._
import lsu.LsuClientId
import org.scalatest.freespec.AnyFreeSpec
import top.parameters._

/** End-to-end RTcore wiring test with its real Box4 and Triangle pipelines. */
class RtCoreIntegrationTest extends AnyFreeSpec with ChiselScalatestTester {
  private val pdsBase = BigInt(0x1000)
  private val asBase = BigInt(0x8000)
  private val rayFields = Seq[BigInt](
    0x8000, 0, 0, 0,
    0x3f800000L, 0x40000000L, 0x40400000L, 0,
    0x40800000L, 0x41000000L, 0x41400000L, 0x42c80000L
  )

  private def driveIdle(dut: RtCore): Unit = {
    dut.io.command.valid.poke(false.B)
    dut.io.command.bits.warpId.poke(0.U)
    dut.io.command.bits.issueId.poke(0.U)
    dut.io.command.bits.destinationVd.poke(0.U)
    dut.io.command.bits.activeMask.poke(0.U)
    dut.io.command.bits.pds.baseAddress.poke(0.U)
    dut.io.command.bits.pds.numWarps.poke(1.U)
    dut.io.command.bits.pds.numThreads.poke(num_thread.U)
    dut.io.command.bits.pds.pdsWarpTidBase.poke(0.U)
    dut.io.completion.ready.poke(true.B)
    dut.io.lsuRequest.ready.poke(false.B)
    dut.io.lsuResponse.valid.poke(false.B)
    dut.io.lsuResponse.bits.clientId.poke(LsuClientId.RtCore)
    dut.io.lsuResponse.bits.lineBase.poke(0.U)
    dut.io.lsuResponse.bits.coverageMask.poke(0.U)
    dut.io.lsuResponse.bits.data.foreach(_.poke(0.U))
    dut.io.lsuResponse.bits.fault.poke(false.B)
  }

  private def waitForRequest(dut: RtCore, limit: Int = 400): (BigInt, BigInt) = {
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

  private def respond(
      dut: RtCore,
      address: BigInt,
      coverage: BigInt,
      data: Seq[BigInt]
  ): Unit = {
    dut.io.lsuResponse.bits.clientId.poke(LsuClientId.RtCore)
    dut.io.lsuResponse.bits.lineBase.poke(address.U)
    dut.io.lsuResponse.bits.coverageMask.poke(coverage.U)
    dut.io.lsuResponse.bits.fault.poke(false.B)
    dut.io.lsuResponse.bits.data.zip(data).foreach { case (word, value) =>
      word.poke(value.U)
    }
    dut.io.lsuResponse.valid.poke(true.B)
    var left = 20
    while (!dut.io.lsuResponse.ready.peek().litToBoolean && left > 0) {
      dut.clock.step()
      left -= 1
    }
    assert(left > 0, s"LSU response at 0x${address.toString(16)} was not accepted")
    dut.clock.step()
    dut.io.lsuResponse.valid.poke(false.B)
    dut.clock.step()
  }

  private def admitSingleLane(dut: RtCore): Unit = {
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
  }

  private def returnTraversalRay(dut: RtCore): Unit = {
    var pending = RtMemTableParameters.pdsTraversalReadFields.toSet
    while (pending.nonEmpty) {
      val (address, coverage) = waitForRequest(dut)
      val field = ((address - pdsBase) / RtCorePortParameters.lineBytes).toInt -
        RtAbiLayout.SlotAccelLo
      assert(pending.contains(field), s"unexpected PDS field $field")
      pending -= field
      respond(dut, address, coverage,
        Seq.fill(RtCorePortParameters.lineWordCount)(rayFields(field)))
    }
  }

  private def awaitCompletion(dut: RtCore): Unit = {
    var left = 300
    while (!dut.io.completion.valid.peek().litToBoolean && left > 0) {
      dut.clock.step()
      left -= 1
    }
    assert(left > 0, "RTcore did not complete traversal")
    dut.io.completion.bits.warpId.expect(5.U)
    dut.io.completion.bits.issueId.expect(0x3a.U)
    dut.io.completion.bits.destinationVd.expect(7.U)
    dut.io.completion.bits.completionMask.expect(1.U)
    dut.io.completion.bits.laneStatus(0).expect(RtAbiLayout.TraversalCompleteMiss.U)
  }

  "RtCore routes a Box4 miss through a physical Box4 pipeline" in {
    test(new RtCore).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      dut.reset.poke(true.B)
      driveIdle(dut)
      dut.clock.step(2)
      dut.reset.poke(false.B)
      admitSingleLane(dut)
      returnTraversalRay(dut)

      val (headerAddress, headerCoverage) = waitForRequest(dut)
      assert(headerAddress == asBase)
      val header = Array.fill[BigInt](RtCorePortParameters.lineWordCount)(0)
      header((RtVtasLayout.AsHeaderRootNodeRef / 4).toInt) =
        BigInt(0x80 | RtVtasLayout.NodeBox4)
      respond(dut, headerAddress, headerCoverage, header.toSeq)

      val (boxAddress, boxCoverage) = waitForRequest(dut)
      assert(boxAddress == asBase + 0x80)
      assert(boxCoverage == BigInt("ffffffff", 16))
      val box = Array.fill[BigInt](RtCorePortParameters.lineWordCount)(0)
      val negativeFive = BigInt("c0a00000", 16)
      val negativeFour = BigInt("c0800000", 16)
      for (child <- 0 until 4) {
        box(child) = BigInt(RtVtasLayout.InvalidNodeRef)
        Seq(RtVtasLayout.Box4MinX, RtVtasLayout.Box4MinY, RtVtasLayout.Box4MinZ).foreach {
          offset => box((offset / 4).toInt + child) = negativeFive
        }
        Seq(RtVtasLayout.Box4MaxX, RtVtasLayout.Box4MaxY, RtVtasLayout.Box4MaxZ).foreach {
          offset => box((offset / 4).toInt + child) = negativeFour
        }
      }
      respond(dut, boxAddress, boxCoverage, box.toSeq)
      awaitCompletion(dut)
    }
  }

  "RtCore routes a Triangle miss through a physical Triangle pipeline" in {
    test(new RtCore).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      dut.reset.poke(true.B)
      driveIdle(dut)
      dut.clock.step(2)
      dut.reset.poke(false.B)
      admitSingleLane(dut)
      returnTraversalRay(dut)

      val (headerAddress, headerCoverage) = waitForRequest(dut)
      assert(headerAddress == asBase)
      val header = Array.fill[BigInt](RtCorePortParameters.lineWordCount)(0)
      header((RtVtasLayout.AsHeaderRootNodeRef / 4).toInt) =
        BigInt(0x80 | RtVtasLayout.NodeTriangle)
      respond(dut, headerAddress, headerCoverage, header.toSeq)

      val (triangleAddress, triangleCoverage) = waitForRequest(dut)
      assert(triangleAddress == asBase + 0x80)
      assert(triangleCoverage == BigInt("ffff", 16))
      respond(dut, triangleAddress, triangleCoverage,
        Seq.fill(RtCorePortParameters.lineWordCount)(BigInt(0)))
      awaitCompletion(dut)
    }
  }
}

package rtcore

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import top.parameters._

class RtCoreSchedulerTest extends AnyFreeSpec with ChiselScalatestTester {
  private def driveIdle(dut: RtCoreScheduler): Unit = {
    dut.io.command.valid.poke(false.B)
    dut.io.command.bits.warpId.poke(0.U)
    dut.io.command.bits.issueId.poke(0.U)
    dut.io.command.bits.destinationVd.poke(0.U)
    dut.io.command.bits.activeMask.poke(0.U)
    dut.io.command.bits.pds.baseAddress.poke(0.U)
    dut.io.command.bits.pds.numWarps.poke(num_warp.U)
    dut.io.command.bits.pds.numThreads.poke(num_thread.U)
    dut.io.command.bits.pds.pdsWarpTidBase.poke(0.U)
    dut.io.completion.ready.poke(true.B)
    dut.io.rtLsuReport.ready.poke(true.B)
    dut.io.rtLsuRespond.valid.poke(false.B)
    dut.io.rtLsuRespond.bits.mshrId.poke(0.U)
    dut.io.rtLsuRespond.bits.data.foreach(_.poke(0.U))
    dut.io.rtLsuRespond.bits.fault.poke(false.B)
    dut.boxIssue.foreach(_.ready.poke(true.B))
    dut.boxResult.foreach { result =>
      result.valid.poke(false.B)
      result.bits.requestId.poke(0.U)
      result.bits.tmin.foreach(_.poke(0.U))
      result.bits.isIntersect.foreach(_.poke(false.B))
      result.bits.boxIndex.foreach(_.poke(0.U))
    }
    dut.triangleIssue.foreach(_.ready.poke(true.B))
    dut.triangleResult.foreach { result =>
      result.valid.poke(false.B)
      result.bits.requestId.poke(0.U)
      result.bits.uNum.poke(0.U)
      result.bits.vNum.poke(0.U)
      result.bits.tNum.poke(0.U)
      result.bits.tDenom.poke(0.U)
      result.bits.hit.poke(false.B)
    }
  }

  "fresh command admits PDS state and starts with an accel-lo cacheline read" in {
    test(new RtCoreScheduler).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      driveIdle(dut)
      dut.clock.step()
      dut.io.command.bits.warpId.poke(2.U)
      dut.io.command.bits.issueId.poke(0x5a.U)
      dut.io.command.bits.destinationVd.poke(3.U)
      dut.io.command.bits.activeMask.poke(1.U)
      dut.io.command.bits.pds.baseAddress.poke(0x8000.U)
      dut.io.command.bits.pds.pdsWarpTidBase.poke((2 * num_thread).U)
      dut.io.command.valid.poke(true.B)
      dut.io.command.ready.expect(true.B)
      dut.clock.step()
      dut.io.command.valid.poke(false.B)

      // Field 0 is accel_lo, which follows status in the physical PDS header.
      dut.io.rtLsuReport.valid.expect(true.B)
      dut.io.rtLsuReport.bits.write.expect(false.B)
      dut.io.rtLsuReport.bits.is64B.expect(false.B)
      dut.io.rtLsuReport.bits.cacheLineAddress.expect(0x8500.U)
    }
  }
}

package rtcore

import chisel3._
import chiseltest._
import lsu.LsuClientId
import org.scalatest.freespec.AnyFreeSpec

class RtResidentTablesTest extends AnyFreeSpec with ChiselScalatestTester {
  private val traversalFields = RtMemTableParameters.pdsTraversalReadFields

  private def driveContext(context: RtPdsContext): Unit = {
    context.baseAddress.poke(0x1000.U)
    context.numWarps.poke(4.U)
    context.numThreads.poke(32.U)
    context.pdsWarpTidBase.poke(64.U)
  }

  "resident PDS read rows accept out-of-order line returns and require retire" in {
    test(new RtPdsReadTable).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      dut.io.admission.valid.poke(false.B)
      dut.io.laneOut.ready.poke(false.B)
      dut.io.faultOut.ready.poke(true.B)
      dut.io.retire.valid.poke(false.B)
      dut.io.candidate.ready.poke(true.B)
      dut.io.injection.valid.poke(false.B)

      dut.io.admission.bits.warpId.poke(0.U)
      driveContext(dut.io.admission.bits.pds)
      dut.io.admission.bits.activeMask.poke((BigInt(1) << 2).U)
      dut.io.admission.valid.poke(true.B)
      dut.io.admission.ready.expect(true.B)
      dut.clock.step()
      dut.io.admission.valid.poke(false.B)

      for (field <- traversalFields) {
        dut.io.candidate.valid.expect(true.B)
        dut.io.candidate.bits.source.expect(RtLsuSource.pdsRead)
        dut.io.candidate.bits.locator.tableLocation.expect(field.U)
        dut.io.candidate.bits.cacheLineAddress.expect((0x1300 + field * 0x200).U)
        dut.io.candidate.bits.wordMask.expect("hffffffff".U)
        dut.clock.step()
      }

      for (field <- traversalFields.reverse) {
        dut.io.injection.bits.field.poke(field.U)
        dut.io.injection.bits.fault.poke(false.B)
        dut.io.injection.bits.data.zipWithIndex.foreach { case (word, lane) =>
          word.poke((field * 100 + lane).U)
        }
        dut.io.injection.valid.poke(true.B)
        dut.io.injection.ready.expect(true.B)
        dut.clock.step()
        dut.io.injection.valid.poke(false.B)
      }

      dut.io.laneOut.valid.expect(true.B)
      dut.io.laneOut.bits.warpId.expect(0.U)
      dut.io.laneOut.bits.laneId.expect(2.U)
      traversalFields.foreach { field =>
        dut.io.laneOut.bits.data(field).expect((field * 100 + 2).U)
      }
      dut.io.retire.bits.warpId.poke(0.U)
      dut.io.retire.valid.poke(true.B)
      dut.io.retire.ready.expect(false.B)
      dut.io.retire.valid.poke(false.B)
      dut.io.laneOut.ready.poke(true.B)
      dut.clock.step()
      dut.io.laneOut.ready.poke(false.B)
      dut.io.retire.valid.poke(true.B)
      dut.io.retire.ready.expect(true.B)
    }
  }

  "PDS read table owns prepared ray data and mutable current tmax" in {
    test(new RtPdsReadTable).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      dut.io.admission.valid.poke(false.B)
      dut.io.laneOut.ready.poke(false.B)
      dut.io.faultOut.ready.poke(true.B)
      dut.io.retire.valid.poke(false.B)
      dut.io.candidate.ready.poke(true.B)
      dut.io.injection.valid.poke(false.B)
      dut.io.raySetup.valid.poke(false.B)
      dut.io.rayTmaxUpdate.valid.poke(false.B)
      dut.io.nodeRayLookup.warpId.poke(0.U)
      dut.io.nodeRayLookup.laneId.poke(0.U)
      dut.io.issueRayLookup.warpId.poke(0.U)
      dut.io.issueRayLookup.laneId.poke(0.U)
      dut.io.resultRayLookup.warpId.poke(0.U)
      dut.io.resultRayLookup.laneId.poke(0.U)

      dut.io.admission.bits.warpId.poke(0.U)
      driveContext(dut.io.admission.bits.pds)
      dut.io.admission.bits.activeMask.poke(1.U)
      dut.io.admission.valid.poke(true.B)
      dut.clock.step()
      dut.io.admission.valid.poke(false.B)
      for (_ <- traversalFields) { dut.clock.step() }
      for (field <- traversalFields) {
        dut.io.injection.bits.field.poke(field.U)
        dut.io.injection.bits.fault.poke(false.B)
        dut.io.injection.bits.data.zipWithIndex.foreach { case (word, lane) =>
          word.poke((field * 100 + lane).U)
        }
        dut.io.injection.valid.poke(true.B)
        dut.clock.step()
        dut.io.injection.valid.poke(false.B)
      }
      dut.io.laneOut.ready.poke(true.B)
      dut.clock.step()
      dut.io.laneOut.ready.poke(false.B)

      dut.io.raySetup.bits.warpId.poke(0.U)
      dut.io.raySetup.bits.laneId.poke(0.U)
      dut.io.raySetup.bits.accelLo.poke(0x8000.U)
      dut.io.raySetup.bits.accelHi.poke(0.U)
      dut.io.raySetup.bits.traceMeta0.poke(0.U)
      dut.io.raySetup.bits.traceMeta1.poke(0.U)
      dut.io.raySetup.bits.ray.origin.x.poke(0.U)
      dut.io.raySetup.bits.ray.origin.y.poke(0.U)
      dut.io.raySetup.bits.ray.origin.z.poke(0.U)
      dut.io.raySetup.bits.ray.dir.x.poke(0.U)
      dut.io.raySetup.bits.ray.dir.y.poke(0.U)
      dut.io.raySetup.bits.ray.dir.z.poke(0.U)
      dut.io.raySetup.bits.ray.inv.x.poke(0x3f800000L.U)
      dut.io.raySetup.bits.ray.inv.y.poke(0x40000000L.U)
      dut.io.raySetup.bits.ray.inv.z.poke(0x40400000L.U)
      dut.io.raySetup.bits.ray.originalTmin.poke(0x3dcccccdL.U)
      dut.io.raySetup.bits.ray.extent.poke(0x41200000L.U)
      dut.io.raySetup.bits.ray.kx.poke(0.U)
      dut.io.raySetup.bits.ray.ky.poke(1.U)
      dut.io.raySetup.bits.ray.kz.poke(2.U)
      dut.io.raySetup.bits.ray.shear.x.poke(11.U)
      dut.io.raySetup.bits.ray.shear.y.poke(12.U)
      dut.io.raySetup.bits.ray.shear.z.poke(13.U)
      dut.io.raySetup.valid.poke(true.B)
      dut.io.raySetup.ready.expect(true.B)
      dut.clock.step()
      dut.io.raySetup.valid.poke(false.B)

      dut.io.nodeRayLookup.warpId.poke(0.U)
      dut.io.nodeRayLookup.laneId.poke(0.U)
      dut.io.nodeRay.valid.expect(true.B)
      dut.io.nodeRay.asBase.expect(0x8000.U)
      dut.io.nodeRay.currentTmax.expect(0x41200000L.U)
      dut.io.nodeRay.ray.origin.x.expect(400.U)
      dut.io.nodeRay.ray.dir.z.expect(1000.U)
      dut.io.nodeRay.ray.inv.z.expect(0x40400000L.U)
      dut.io.nodeRay.ray.originalTmin.expect(0x3dcccccdL.U)
      dut.io.nodeRay.ray.kz.expect(2.U)

      dut.io.rayTmaxUpdate.bits.warpId.poke(0.U)
      dut.io.rayTmaxUpdate.bits.laneId.poke(0.U)
      dut.io.rayTmaxUpdate.bits.tmax.poke(0x40800000L.U)
      dut.io.rayTmaxUpdate.valid.poke(true.B)
      dut.io.rayTmaxUpdate.ready.expect(true.B)
      dut.clock.step()
      dut.io.rayTmaxUpdate.valid.poke(false.B)
      dut.io.nodeRay.currentTmax.expect(0x40800000L.U)
      dut.io.nodeRay.ray.extent.expect(0x40800000L.U)
    }
  }

  "PDS write table advances a 64-byte half only after its acknowledgement" in {
    test(new RtPdsWriteTable(warpEntryCount = 1)).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      dut.io.admission.valid.poke(false.B)
      dut.io.update.valid.poke(false.B)
      dut.io.finish.valid.poke(false.B)
      dut.io.candidate.ready.poke(true.B)
      dut.io.injection.valid.poke(false.B)
      dut.io.drained.ready.poke(false.B)

      dut.io.admission.bits.warpId.poke(0.U)
      driveContext(dut.io.admission.bits.pds)
      dut.io.admission.bits.activeMask.poke(((BigInt(1) << 0) | (BigInt(1) << 17)).U)
      dut.io.admission.valid.poke(true.B)
      dut.clock.step()
      dut.io.admission.valid.poke(false.B)
      for (lane <- Seq(0, 17)) {
        dut.io.update.bits.warpId.poke(0.U)
        dut.io.update.bits.laneId.poke(lane.U)
        dut.io.update.bits.committed.poke(true.B)
        dut.io.update.bits.dirty.poke((BigInt(1) << 2).U)
        dut.io.update.bits.data.zipWithIndex.foreach { case (word, index) =>
          word.poke((lane * 100 + index).U)
        }
        dut.io.update.valid.poke(true.B)
        dut.clock.step()
        dut.io.update.valid.poke(false.B)
      }
      dut.io.finish.bits.warpId.poke(0.U)
      dut.io.finish.bits.terminalMask.poke(((BigInt(1) << 0) | (BigInt(1) << 17)).U)
      dut.io.finish.valid.poke(true.B)
      dut.clock.step()
      dut.io.finish.valid.poke(false.B)

      var acknowledgements = 0
      for (_ <- 0 until 48) {
        if (dut.io.candidate.valid.peek().litToBoolean) {
          dut.io.candidate.bits.source.expect(RtLsuSource.pdsWrite)
          dut.io.candidate.bits.write.expect(true.B)
          dut.io.candidate.bits.is64B.expect(true.B)
          if (dut.io.candidate.bits.locator.tableLocation.peek().litValue.testBit(0))
            dut.io.candidate.bits.writeData(1).expect(1702.U)
          else
            dut.io.candidate.bits.writeData(0).expect(2.U)
          dut.clock.step()
          dut.io.injection.bits.record.poke(true.B)
          dut.io.injection.bits.word.poke(2.U)
          dut.io.injection.bits.upperHalf.poke((acknowledgements == 1).B)
          dut.io.injection.bits.fault.poke(false.B)
          dut.io.injection.valid.poke(true.B)
          dut.io.injection.ready.expect(true.B)
          dut.clock.step()
          dut.io.injection.valid.poke(false.B)
          acknowledgements += 1
        } else {
          dut.clock.step()
        }
      }
      assert(acknowledgements == 2)
      dut.io.drained.valid.expect(true.B)
      dut.io.drained.bits.warpId.expect(0.U)
      dut.io.drained.ready.poke(true.B)
    }
  }

  "AS table merges only before it reports its line" in {
    test(new RtAsTable) { dut =>
      dut.io.acquire.valid.poke(false.B)
      dut.io.readyLine.ready.poke(true.B)
      dut.io.candidate.ready.poke(false.B)
      dut.io.injection.valid.poke(false.B)
      for (lane <- Seq(3, 7)) {
        dut.io.acquire.bits.lineAddress.poke(0x800.U)
        dut.io.acquire.bits.wordMask.poke((BigInt(1) << lane).U)
        dut.io.acquire.bits.is64B.poke(false.B)
        dut.io.acquire.bits.waiter.warpId.poke(0.U)
        dut.io.acquire.bits.waiter.laneId.poke(lane.U)
        dut.io.acquire.bits.waiter.nodeKind.poke(0.U)
        dut.io.acquire.valid.poke(true.B)
        dut.io.acquire.ready.expect(true.B)
        dut.clock.step()
        dut.io.acquire.valid.poke(false.B)
      }
      dut.io.candidate.valid.expect(true.B)
      dut.io.candidate.bits.wordMask.expect(((BigInt(1) << 3) | (BigInt(1) << 7)).U)
      dut.io.candidate.ready.poke(true.B)
      dut.clock.step()
      dut.io.candidate.ready.poke(false.B)

      dut.io.acquire.bits.lineAddress.poke(0x800.U)
      dut.io.acquire.bits.wordMask.poke((BigInt(1) << 9).U)
      dut.io.acquire.bits.is64B.poke(false.B)
      dut.io.acquire.bits.waiter.warpId.poke(0.U)
      dut.io.acquire.bits.waiter.laneId.poke(9.U)
      dut.io.acquire.bits.waiter.nodeKind.poke(0.U)
      dut.io.acquire.valid.poke(true.B)
      dut.clock.step()
      dut.io.acquire.valid.poke(false.B)
      dut.io.candidate.valid.expect(true.B)
      dut.io.candidate.bits.wordMask.expect((BigInt(1) << 9).U)
    }
  }

  "RtLsuMshr gives PDS reads priority and routes the buffered return" in {
    test(new RtLsuMshr) { dut =>
      dut.io.request.ready.poke(true.B)
      dut.io.response.valid.poke(false.B)
      dut.io.pdsReadInjection.ready.poke(true.B)
      dut.io.pdsWriteInjection.ready.poke(true.B)
      dut.io.asInjection.ready.poke(true.B)
      for (candidate <- Seq(dut.io.pdsRead, dut.io.pdsWrite, dut.io.as)) {
        candidate.valid.poke(true.B)
        candidate.bits.cacheLineAddress.poke(0x400.U)
        candidate.bits.write.poke(false.B)
        candidate.bits.is64B.poke(false.B)
        candidate.bits.wordMask.poke(1.U)
        candidate.bits.writeData.foreach(_.poke(0.U))
        candidate.bits.locator.tableLocation.poke(4.U)
      }
      dut.io.pdsRead.bits.source.poke(RtLsuSource.pdsRead)
      dut.io.pdsWrite.bits.source.poke(RtLsuSource.pdsWrite)
      dut.io.as.bits.source.poke(RtLsuSource.as)
      dut.io.request.valid.expect(true.B)
      dut.io.pdsRead.ready.expect(true.B)
      dut.io.pdsWrite.ready.expect(false.B)
      dut.io.request.bits.clientId.expect(LsuClientId.RtCore)
      dut.io.request.bits.lineBase.expect(0x400.U)
      dut.io.request.bits.coverageMask.expect(1.U)
      dut.clock.step()
      dut.io.pdsRead.valid.poke(false.B)
      dut.io.pdsWrite.valid.poke(false.B)
      dut.io.as.valid.poke(false.B)

      dut.io.response.bits.clientId.poke(LsuClientId.RtCore)
      dut.io.response.bits.lineBase.poke(0x400.U)
      dut.io.response.bits.coverageMask.poke(1.U)
      dut.io.response.bits.data.zipWithIndex.foreach { case (word, index) => word.poke((0x100 + index).U) }
      dut.io.response.bits.fault.poke(false.B)
      dut.io.response.valid.poke(true.B)
      dut.io.response.ready.expect(true.B)
      dut.clock.step()
      dut.io.response.valid.poke(false.B)
      dut.io.pdsReadInjection.valid.expect(true.B)
      dut.io.pdsReadInjection.bits.field.expect(4.U)
      dut.io.pdsReadInjection.bits.data(0).expect(0x100.U)
      dut.clock.step()
    }
  }
}

package rtcore

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class RtResidentTablesTest extends AnyFreeSpec with ChiselScalatestTester {
  private val fieldCount = RtMemTableParameters.pdsRayStateFieldCount

  private def driveContext(context: RtPdsContext): Unit = {
    context.baseAddress.poke(0x1000.U)
    context.numWarps.poke(4.U)
    context.numThreads.poke(32.U)
    context.pdsWarpTidBase.poke(64.U)
  }

  "resident PDS read rows survive delivery until explicit retire" in {
    test(new RtPdsReadTable) { dut =>
      dut.io.admission.valid.poke(false.B)
      dut.io.laneOut.ready.poke(false.B)
      dut.io.faultOut.ready.poke(true.B)
      dut.io.retire.valid.poke(false.B)
      dut.io.request.ready.poke(true.B)
      dut.io.response.valid.poke(false.B)
      dut.io.response.bits.requestId.poke(0.U)
      dut.io.response.bits.laneMask.poke(0.U)
      dut.io.response.bits.faultMask.poke(0.U)
      dut.io.response.bits.readData.foreach(_.poke(0.U))

      dut.io.admission.bits.workId.poke(9.U)
      dut.io.admission.bits.warpId.poke(1.U)
      driveContext(dut.io.admission.bits.pds)
      dut.io.admission.bits.activeMask.poke((BigInt(1) << 2).U)
      dut.io.admission.valid.poke(true.B)
      dut.io.admission.ready.expect(true.B)
      dut.clock.step()
      dut.io.admission.valid.poke(false.B)

      val requests = Array.fill[(BigInt, Int)](fieldCount)((0, 0))
      for (field <- 0 until fieldCount) {
        dut.io.request.valid.expect(true.B)
        dut.io.request.bits.fieldWordOffset.expect((RtAbiLayout.SlotAccelLo + field).U)
        requests(field) = (
          dut.io.request.bits.requestId.peek().litValue,
          dut.io.request.bits.fieldWordOffset.peek().litValue.toInt
        )
        dut.clock.step()
      }
      requests.reverse.foreach { case (requestId, fieldWord) =>
        dut.io.response.bits.requestId.poke(requestId.U)
        dut.io.response.bits.laneMask.poke((BigInt(1) << 2).U)
        dut.io.response.bits.faultMask.poke(0.U)
        dut.io.response.bits.readData.zipWithIndex.foreach { case (word, lane) =>
          word.poke((fieldWord * 100 + lane).U)
        }
        dut.io.response.valid.poke(true.B)
        dut.io.response.ready.expect(true.B)
        dut.clock.step()
        dut.io.response.valid.poke(false.B)
      }

      dut.io.laneOut.valid.expect(true.B)
      dut.io.laneOut.bits.workId.expect(9.U)
      dut.io.laneOut.bits.warpId.expect(1.U)
      dut.io.laneOut.bits.laneId.expect(2.U)
      dut.io.laneOut.bits.data.zipWithIndex.foreach { case (word, field) =>
        word.expect(((RtAbiLayout.SlotAccelLo + field) * 100 + 2).U)
      }
      // No retire before the row is explicitly delivered.
      dut.io.retire.bits.workId.poke(9.U)
      dut.io.retire.bits.warpId.poke(1.U)
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

  "write table transposes dirty lane updates only after finish" in {
    test(new RtPdsWriteTable(warpEntryCount = 1)) { dut =>
      dut.io.admission.valid.poke(false.B)
      dut.io.update.valid.poke(false.B)
      dut.io.finish.valid.poke(false.B)
      dut.io.write.ready.poke(true.B)
      dut.io.drained.ready.poke(true.B)

      dut.io.admission.bits.workId.poke(7.U)
      dut.io.admission.bits.warpId.poke(0.U)
      driveContext(dut.io.admission.bits.pds)
      dut.io.admission.bits.activeMask.poke(((BigInt(1) << 0) | (BigInt(1) << 17)).U)
      dut.io.admission.valid.poke(true.B)
      dut.io.admission.ready.expect(true.B)
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
        dut.io.update.ready.expect(true.B)
        dut.clock.step()
        dut.io.update.valid.poke(false.B)
      }
      dut.io.finish.bits.workId.poke(7.U)
      dut.io.finish.bits.warpId.poke(0.U)
      dut.io.finish.bits.terminalMask.poke(((BigInt(1) << 0) | (BigInt(1) << 17)).U)
      dut.io.finish.valid.poke(true.B)
      dut.io.finish.ready.expect(true.B)
      dut.clock.step()
      dut.io.finish.valid.poke(false.B)

      var observed = Vector.empty[(BigInt, BigInt, BigInt)]
      for (_ <- 0 until 48) {
        if (dut.io.write.valid.peek().litToBoolean) {
          observed :+= ((
            dut.io.write.bits.blockAddress.peek().litValue,
            dut.io.write.bits.wordMask.peek().litValue,
            dut.io.write.bits.data(0).peek().litValue
          ))
        }
        dut.clock.step()
      }
      assert(observed.toSet == Set(
        (BigInt(0x4d00), BigInt(1), BigInt(2)),
        (BigInt(0x4d40), BigInt(2), BigInt(1702))
      ))
      dut.io.drained.valid.expect(true.B)
      dut.io.drained.bits.workId.expect(7.U)
    }
  }

  "AS table coalesces matching pending lines and fans out the response" in {
    test(new RtAsTable) { dut =>
      dut.io.acquire.valid.poke(false.B)
      dut.io.readyLine.ready.poke(true.B)
      dut.io.request.ready.poke(false.B)
      dut.io.response.valid.poke(false.B)
      dut.io.response.bits.requestId.poke(0.U)
      dut.io.response.bits.data.foreach(_.poke(0.U))
      dut.io.response.bits.fault.poke(false.B)

      for (lane <- Seq(3, 7)) {
        dut.io.acquire.bits.lineAddress.poke(0x800.U)
        dut.io.acquire.bits.wordMask.poke((BigInt(1) << lane).U)
        dut.io.acquire.bits.waiter.warpId.poke(1.U)
        dut.io.acquire.bits.waiter.laneId.poke(lane.U)
        dut.io.acquire.bits.waiter.nodeKind.poke(0.U)
        dut.io.acquire.bits.waiter.nodeRef.poke(0x180.U)
        dut.io.acquire.valid.poke(true.B)
        dut.io.acquire.ready.expect(true.B)
        dut.clock.step()
        dut.io.acquire.valid.poke(false.B)
      }
      dut.io.request.ready.poke(true.B)
      dut.io.request.valid.expect(true.B)
      dut.io.request.bits.lineAddress.expect(0x800.U)
      dut.io.request.bits.wordMask.expect(((BigInt(1) << 3) | (BigInt(1) << 7)).U)
      val requestId = dut.io.request.bits.requestId.peek().litValue
      dut.clock.step()

      dut.io.response.bits.requestId.poke(requestId.U)
      dut.io.response.bits.data.zipWithIndex.foreach { case (word, index) => word.poke((0x100 + index).U) }
      dut.io.response.valid.poke(true.B)
      dut.io.response.ready.expect(true.B)
      dut.clock.step()
      dut.io.response.valid.poke(false.B)
      val delivered = scala.collection.mutable.Set.empty[BigInt]
      for (_ <- 0 until 4) {
        if (dut.io.readyLine.valid.peek().litToBoolean)
          delivered += dut.io.readyLine.bits.waiter.laneId.peek().litValue
        dut.clock.step()
      }
      assert(delivered == Set(BigInt(3), BigInt(7)))
    }
  }
}

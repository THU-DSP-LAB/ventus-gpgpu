package rtcore

import chisel3._
import chisel3.util._
import chiseltest._
import lsu.LsuClientId
import org.scalatest.freespec.AnyFreeSpec

/** Transaction-directory pressure and response-routing tests. */
class RtLsuMshrStressTest extends AnyFreeSpec with ChiselScalatestTester {
  private def driveCandidate(candidate: DecoupledIO[RtLsuCandidate], source: UInt,
      address: BigInt, field: Int, wordMask: BigInt = 1, is64B: Boolean = false): Unit = {
    candidate.valid.poke(true.B)
    candidate.bits.source.poke(source)
    candidate.bits.cacheLineAddress.poke(address.U)
    candidate.bits.write.poke(false.B)
    candidate.bits.is64B.poke(is64B.B)
    candidate.bits.wordMask.poke(wordMask.U)
    candidate.bits.writeData.foreach(_.poke(0.U))
    candidate.bits.locator.warpId.poke(0.U)
    candidate.bits.locator.field.poke(field.U)
    candidate.bits.locator.record.poke(false.B)
    candidate.bits.locator.word.poke(0.U)
    candidate.bits.locator.upperHalf.poke(false.B)
    candidate.bits.locator.asEntry.poke(0.U)
  }

  private def driveIdle(dut: RtLsuMshr): Unit = {
    dut.io.request.ready.poke(true.B)
    dut.io.pdsRead.valid.poke(false.B)
    dut.io.pdsWrite.valid.poke(false.B)
    dut.io.as.valid.poke(false.B)
    dut.io.response.valid.poke(false.B)
    dut.io.response.bits.clientId.poke(LsuClientId.RtCore)
    dut.io.response.bits.lineBase.poke(0.U)
    dut.io.response.bits.coverageMask.poke(0.U)
    dut.io.response.bits.data.foreach(_.poke(0.U))
    dut.io.response.bits.fault.poke(false.B)
    dut.io.pdsReadInjection.ready.poke(true.B)
    dut.io.pdsWriteInjection.ready.poke(true.B)
    dut.io.asInjection.ready.poke(true.B)
  }

  private def returnRead(dut: RtLsuMshr, tag: Int, lineBase: Int, mask: BigInt, field: Int): Unit = {
    dut.io.response.bits.clientId.poke(LsuClientId.RtCore)
    dut.io.response.bits.lineBase.poke(lineBase.U)
    dut.io.response.bits.coverageMask.poke(mask.U)
    dut.io.response.bits.fault.poke(false.B)
    dut.io.response.bits.data.zipWithIndex.foreach { case (word, index) =>
      word.poke((tag * 0x100 + index).U)
    }
    dut.io.response.valid.poke(true.B)
    dut.io.response.ready.expect(true.B)
    dut.clock.step()
    dut.io.response.valid.poke(false.B)
    dut.io.pdsReadInjection.valid.expect(true.B)
    dut.io.pdsReadInjection.bits.warpId.expect(0.U)
    dut.io.pdsReadInjection.bits.field.expect(field.U)
    dut.io.pdsReadInjection.bits.data(0).expect((tag * 0x100).U)
    dut.clock.step()
  }

  "RtLsuMshr accepts sixteen transactions, stalls when full, and routes out-of-order replies" in {
    test(new RtLsuMshr).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      driveIdle(dut)
      dut.clock.step()

      val capacity = RtCorePortParameters.rtLsuMshrEntryCount
      for (id <- 0 until capacity) {
        driveCandidate(dut.io.pdsRead, RtLsuSource.pdsRead, 0x1000 + id * 0x80, id % RtMemTableParameters.pdsRayStateFieldCount)
        dut.io.request.valid.expect(true.B)
        dut.io.request.bits.clientId.expect(LsuClientId.RtCore)
        dut.io.request.bits.lineBase.expect((0x1000 + id * 0x80).U)
        dut.io.request.bits.coverageMask.expect(1.U)
        dut.io.pdsRead.ready.expect(true.B)
        dut.clock.step()
      }
      dut.io.pdsRead.valid.poke(false.B)
      dut.io.request.valid.expect(false.B)

      val responseOrder = Seq(15, 3, 10, 0, 7, 14, 2, 12, 5, 9, 1, 13, 4, 11, 6, 8)
      responseOrder.foreach { id =>
        returnRead(dut, id, 0x1000 + id * 0x80, 1,
          id % RtMemTableParameters.pdsRayStateFieldCount)
      }

      // The directory is empty again and accepts another cacheline client request.
      driveCandidate(dut.io.as, RtLsuSource.as, 0x4000, 0)
      dut.io.request.valid.expect(true.B)
      dut.io.request.bits.lineBase.expect(0x4000.U)
      dut.io.as.ready.expect(true.B)
    }
  }

  "RtLsuMshr merges a back-pressured pair of 64B reads and fans out its reply" in {
    test(new RtLsuMshr).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      driveIdle(dut)
      dut.clock.step()

      // No intentional delay is inserted.  This merge is available only
      // because LSU backpressure keeps the lower half unissued.
      dut.io.request.ready.poke(false.B)
      driveCandidate(dut.io.pdsRead, RtLsuSource.pdsRead, 0x2800, 2,
        wordMask = 0xffff, is64B = true)
      dut.io.request.valid.expect(true.B)
      dut.io.request.bits.lineBase.expect(0x2800.U)
      dut.io.request.bits.coverageMask.expect("h0000ffff".U)
      dut.io.pdsRead.ready.expect(true.B)
      dut.clock.step()

      driveCandidate(dut.io.pdsRead, RtLsuSource.pdsRead, 0x2840, 9,
        wordMask = 0xffff, is64B = true)
      dut.io.pdsRead.ready.expect(true.B)
      dut.clock.step()
      dut.io.pdsRead.valid.poke(false.B)

      dut.io.request.ready.poke(true.B)
      dut.io.request.valid.expect(true.B)
      dut.io.request.bits.lineBase.expect(0x2800.U)
      dut.io.request.bits.coverageMask.expect("hffffffff".U)
      dut.clock.step()

      dut.io.response.bits.clientId.poke(LsuClientId.RtCore)
      dut.io.response.bits.lineBase.poke(0x2800.U)
      dut.io.response.bits.coverageMask.poke("hffffffff".U)
      dut.io.response.bits.fault.poke(false.B)
      dut.io.response.bits.data.zipWithIndex.foreach { case (word, index) =>
        word.poke((0x900 + index).U)
      }
      dut.io.response.valid.poke(true.B)
      dut.io.response.ready.expect(true.B)
      dut.clock.step()
      dut.io.response.valid.poke(false.B)

      for ((field, expectedWord) <- Seq((2, 0x900), (9, 0x910))) {
        dut.io.pdsReadInjection.valid.expect(true.B)
        dut.io.pdsReadInjection.bits.field.expect(field.U)
        dut.io.pdsReadInjection.bits.data(0).expect(expectedWord.U)
        dut.clock.step()
      }
      dut.io.pdsReadInjection.valid.expect(false.B)
    }
  }
}

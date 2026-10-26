package rtcore

import chisel3._
import chiseltest._
import lsu.LsuClientId
import org.scalatest.freespec.AnyFreeSpec

class RtLsuAdapterTest extends AnyFreeSpec with ChiselScalatestTester {
  private def zero(data: Data): Unit = data match {
    case b: Bool => b.poke(false.B)
    case u: UInt => u.poke(0.U)
    case a: Aggregate => a.getElements.foreach(zero)
  }

  "issues consecutive tagged requests and normalizes out-of-order replies using AS entries" in {
    test(new RtMemTable).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      zero(dut.io.asAcquire.bits)
      zero(dut.io.lsuResponse.bits)
      dut.io.asAcquire.valid.poke(false.B)
      dut.io.asLineRead.foreach(_.entryId.poke(0.U))
      dut.io.asConsumeLanes.poke(0.U)
      dut.io.lsuRequest.ready.poke(false.B)
      dut.io.lsuResponse.valid.poke(false.B)
      dut.io.clear.valid.poke(false.B)
      dut.io.clear.bits.poke(true.B)
      val addresses = Seq(0x100, 0x140, 0x200)
      for (i <- addresses.indices) {
        dut.io.asAcquire.bits.lineAddress.poke(addresses(i).U)
        dut.io.asAcquire.bits.is64B.poke((i < 2).B)
        dut.io.asAcquire.bits.wordMask.poke((if (i < 2) BigInt("ffff", 16) else BigInt("ffffffff", 16)).U)
        dut.io.asAcquire.bits.waiter.laneId.poke(i.U)
        dut.io.asAcquire.valid.poke(true.B)
        dut.io.asAcquire.ready.expect(true.B)
        dut.clock.step()
      }
      dut.io.asAcquire.valid.poke(false.B)
      dut.io.clear.ready.expect(false.B)
      dut.io.lsuRequest.ready.poke(true.B)
      for (i <- addresses.indices) {
        dut.io.lsuRequest.valid.expect(true.B)
        dut.io.lsuRequest.bits.clientTag.expect(i.U)
        dut.io.lsuRequest.bits.write.expect(false.B)
        dut.io.lsuRequest.bits.writeData.foreach(_.expect(0.U))
        dut.io.lsuRequest.bits.lineBase.expect((addresses(i) & ~127).U)
        dut.io.lsuRequest.bits.coverageMask.expect(Seq("0000ffff", "ffff0000", "ffffffff").map(BigInt(_, 16))(i).U)
        dut.clock.step() // all three requests leave before any response
      }
      dut.io.lsuRequest.valid.expect(false.B)
      dut.io.clear.valid.poke(true.B)
      dut.io.clear.ready.expect(false.B) // no release with in-flight tags
      dut.io.clear.valid.poke(false.B)
      for (i <- Seq(1, 2, 0)) {
        dut.io.lsuResponse.bits.clientId.poke(LsuClientId.RtCore)
        dut.io.lsuResponse.bits.clientTag.poke(i.U)
        dut.io.lsuResponse.bits.lineBase.poke((addresses(i) & ~127).U)
        dut.io.lsuResponse.bits.data.zipWithIndex.foreach { case (word, n) => word.poke((0x1000 + i * 256 + n).U) }
        dut.io.lsuResponse.valid.poke(true.B)
        dut.io.lsuResponse.ready.expect(true.B)
        dut.clock.step()
        dut.io.lsuResponse.valid.poke(false.B)
        dut.io.asReadyMeta(i).valid.expect(true.B)
        dut.io.asLineRead(0).entryId.poke(dut.io.asReadyMeta(i).bits.entryId.peek())
        for (n <- 0 until 32) {
          val expected = if (i < 2 && n >= 16) 0 else 0x1000 + i * 256 + n + (if (i == 1) 16 else 0)
          dut.io.asLineRead(0).data(n).expect(expected.U)
        }
        dut.clock.step(2) // hold a ready node under consumer backpressure
        dut.io.asReadyMeta(i).valid.expect(true.B)
        dut.io.asConsumeLanes.poke((BigInt(1) << i).U)
        dut.clock.step()
        dut.io.asConsumeLanes.poke(0.U)
      }
      dut.io.clear.valid.poke(true.B)
      dut.io.clear.ready.expect(true.B)
      dut.clock.step()
      dut.io.clear.valid.poke(false.B)
      dut.io.quiescent.expect(true.B)
      // Tag zero can be reused only after the previous residency has ended.
      dut.io.asAcquire.bits.lineAddress.poke(0x300.U)
      dut.io.asAcquire.valid.poke(true.B)
      dut.clock.step()
      dut.io.asAcquire.valid.poke(false.B)
      dut.io.lsuRequest.bits.clientTag.expect(0.U)
      dut.io.lsuRequest.valid.expect(true.B)
    }
  }
}

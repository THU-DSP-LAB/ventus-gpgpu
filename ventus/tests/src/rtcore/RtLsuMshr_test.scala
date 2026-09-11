package rtcore

import chisel3._
import chiseltest._
import lsu.LsuClientResponse
import org.scalatest.freespec.AnyFreeSpec

class RtLsuMshrTest extends AnyFreeSpec with ChiselScalatestTester {
  private def idle(dut: RtLsuMshr): Unit = {
    dut.io.as.valid.poke(false.B)
    dut.io.as.bits.source.poke(RtLsuSource.as)
    dut.io.as.bits.cacheLineAddress.poke(0.U)
    dut.io.as.bits.write.poke(false.B)
    dut.io.as.bits.is64B.poke(false.B)
    dut.io.as.bits.wordMask.poke(0.U)
    dut.io.as.bits.writeData.foreach(_.poke(0.U))
    dut.io.as.bits.locator.tableLocation.poke(0.U)
    dut.io.request.ready.poke(false.B)
    dut.io.response.valid.poke(false.B)
    dut.io.response.bits.clientId.poke(0.U)
    dut.io.response.bits.lineBase.poke(0.U)
    dut.io.response.bits.coverageMask.poke(0.U)
    dut.io.response.bits.data.foreach(_.poke(0.U))
    dut.io.response.bits.fault.poke(false.B)
    dut.io.asInjection.ready.poke(false.B)
  }

  "normalizes the upper 64-byte half-line before injecting an AS node" in {
    test(new RtLsuMshr) { dut =>
      idle(dut)
      dut.io.as.bits.cacheLineAddress.poke("h140".U) // word offset 16 in its 128B line
      dut.io.as.bits.is64B.poke(true.B)
      dut.io.as.bits.wordMask.poke("hffff".U)
      dut.io.as.bits.locator.tableLocation.poke(7.U)
      dut.io.as.valid.poke(true.B)
      dut.io.as.ready.expect(true.B)
      dut.clock.step()
      dut.io.as.valid.poke(false.B)

      dut.io.request.valid.expect(true.B)
      dut.io.request.ready.poke(true.B)
      dut.clock.step()
      dut.io.request.ready.poke(false.B)

      dut.io.response.bits.data.zipWithIndex.foreach { case (data, word) =>
        data.poke((0x100 + word).U)
      }
      dut.io.response.bits.fault.poke(false.B)
      dut.io.response.valid.poke(true.B)
      dut.io.response.ready.expect(true.B)
      dut.clock.step()
      dut.io.response.valid.poke(false.B)

      dut.io.asInjection.valid.expect(true.B)
      dut.io.asInjection.bits.entry.expect(7.U)
      dut.io.asInjection.bits.data.take(16).zipWithIndex.foreach { case (data, word) =>
        data.expect((0x110 + word).U)
      }
      dut.io.asInjection.bits.data.drop(16).foreach(_.expect(0.U))
      dut.io.asInjection.ready.poke(true.B)
      dut.clock.step()
    }
  }
}

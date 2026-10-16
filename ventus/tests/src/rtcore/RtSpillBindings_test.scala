package rtcore

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class RtSpillBindingsTest extends AnyFreeSpec with ChiselScalatestTester {
  "driver-assigned parent slots are stable across repeated resume and validity is independent of address" in {
    test(new RtSpillBindings).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      dut.io.descriptor.spillPoolBase.poke("h100000".U)
      dut.io.descriptor.slotsPerCore.poke(256.U)
      dut.io.descriptor.perCoreStride.poke((96 * 1024).U)
      dut.io.descriptor.dispatchEpoch.poke(1.U)
      dut.io.coreId.poke(2.U)
      dut.io.query.valid.poke(true.B)
      dut.io.query.bits.rowBase.poke(200.U)
      dut.io.query.bits.laneId.poke(29.U)
      dut.io.update.valid.poke(false.B)
      dut.io.update.bits.parent.rowBase.poke(200.U)
      dut.io.update.bits.parent.laneId.poke(29.U)
      dut.io.update.bits.suspended.poke(false.B)
      val pointer = 0x100000 + 2 * 96 * 1024 + (5 * 32 + 29) * 384
      dut.clock.step()
      dut.io.pointer.expect(pointer.U)
      dut.io.suspended.expect(false.B)
      def mark(value: Boolean): Unit = {
        dut.io.update.bits.suspended.poke(value.B)
        dut.io.update.valid.poke(true.B)
        dut.clock.step()
        dut.io.update.valid.poke(false.B)
        dut.io.pointer.expect(pointer.U)
        dut.io.suspended.expect(value.B)
      }
      for (_ <- 0 until 8) {
        // Nonzero assigned address does not allow resume before publishing.
        dut.clock.step(3)
        dut.io.suspended.expect(false.B)
        mark(true)
        dut.clock.step(3)
        dut.io.suspended.expect(true.B)
        mark(false)
      }
      mark(true)
      dut.io.query.bits.rowBase.poke(0.U)
      dut.io.query.bits.laneId.poke(0.U)
      dut.io.pointer.expect((0x100000 + 2 * 96 * 1024).U)
      dut.io.suspended.expect(false.B)
      dut.io.query.bits.rowBase.poke(200.U)
      dut.io.query.bits.laneId.poke(29.U)
      dut.io.suspended.expect(true.B)
      dut.io.descriptor.dispatchEpoch.poke(2.U)
      dut.io.suspended.expect(false.B)
      dut.clock.step()
      dut.io.pointer.expect(pointer.U)
      dut.io.suspended.expect(false.B)
    }
  }
}

package rtcore

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class RtSpillParentAccessTest extends AnyFreeSpec with ChiselScalatestTester {
  "fixed address installation and callback reads stay on the parent row under backpressure" in {
    test(new RtSpillParentAccess).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      dut.io.command.valid.poke(false.B)
      dut.io.command.bits.rowBase.poke(200.U) // Parent warp 5, not executing warp 0.
      dut.io.command.bits.laneId.poke(29.U)
      dut.io.command.bits.write.poke(false.B)
      dut.io.command.bits.pointerOnly.poke(false.B)
      dut.io.command.bits.pointer.poke(0.U)
      dut.io.result.ready.poke(false.B)
      dut.io.request.ready.poke(false.B)
      dut.io.response.valid.poke(false.B)
      dut.io.response.bits.readData.poke(0.U)
      def start(write: Boolean, pointer: BigInt = 0, pointerOnly: Boolean = false): Unit = {
        dut.io.command.bits.write.poke(write.B)
        dut.io.command.bits.pointerOnly.poke(pointerOnly.B)
        dut.io.command.bits.pointer.poke(pointer.U)
        dut.io.command.valid.poke(true.B)
        dut.io.command.ready.expect(true.B)
        dut.clock.step()
        dut.io.command.valid.poke(false.B)
      }
      def transaction(field: Int, write: Boolean, data: BigInt): Unit = {
        for (_ <- 0 until 3) {
          dut.io.request.valid.expect(true.B)
          dut.io.request.bits.rowBase.expect(200.U)
          dut.io.request.bits.laneId.expect(29.U)
          dut.io.request.bits.fieldId.expect(field.U)
          dut.io.request.bits.write.expect(write.B)
          if (write) dut.io.request.bits.writeData.expect(data.U)
          dut.io.result.valid.expect(false.B)
          dut.clock.step()
        }
        dut.io.request.ready.poke(true.B)
        dut.clock.step()
        dut.io.request.ready.poke(false.B)
        if (!write) {
          dut.clock.step(2)
          dut.io.result.valid.expect(false.B)
          dut.io.response.bits.readData.poke(data.U)
          dut.io.response.valid.poke(true.B)
          dut.io.response.ready.expect(true.B)
          dut.clock.step()
          dut.io.response.valid.poke(false.B)
        }
      }
      def finish(pointer: BigInt): Unit = {
        for (_ <- 0 until 3) {
          dut.io.result.valid.expect(true.B)
          dut.io.result.bits.pointer.expect(pointer.U)
          dut.io.request.valid.expect(false.B)
          dut.clock.step()
        }
        dut.io.result.ready.poke(true.B)
        dut.clock.step()
        dut.io.result.ready.poke(false.B)
      }
      val pointer = BigInt("1234567812345600", 16)
      start(true, pointer)
      transaction(17, true, pointer >> 32)
      transaction(16, true, pointer & BigInt("ffffffff", 16))
      finish(pointer)
      start(false, pointerOnly = true)
      transaction(16, false, pointer & BigInt("ffffffff", 16))
      transaction(17, false, pointer >> 32)
      finish(pointer)
      for (decision <- Seq(RtAbiLayout.CallbackAccept, RtAbiLayout.CallbackIgnore, RtAbiLayout.CallbackTerminate)) {
        start(false)
        transaction(16, false, pointer & BigInt("ffffffff", 16))
        transaction(17, false, pointer >> 32)
        transaction(RtAbiLayout.LocalControlBaseBytes / 4, false, decision)
        transaction(RtAbiLayout.LocalCommittedHitRecordBaseBytes / 4 + RtAbiLayout.HitRecordMeta0, false, 1)
        transaction(RtAbiLayout.LocalCommittedHitRecordBaseBytes / 4 + RtAbiLayout.HitRecordHitT, false, BigInt("40000000", 16))
        dut.io.result.bits.callbackDecision.expect(decision.U)
        dut.io.result.bits.committedMeta0.expect(1.U)
        dut.io.result.bits.committedHitT.expect("h40000000".U)
        finish(pointer)
      }
      // A new ray has no spill pointer, so stale callback/committed fields
      // must not be mistaken for a continuation.
      start(false)
      transaction(16, false, 0)
      transaction(17, false, 0)
      finish(0)
    }
  }
}

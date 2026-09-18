package rtcore

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import top.parameters._

class RtLocalSramTest extends AnyFreeSpec with ChiselScalatestTester {
  "holds a stalled response while another warp presents a request" in {
    test(new RtLocalSram).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      idle(dut)
      dut.io.request.bits.warpId.poke(3.U)
      dut.io.request.bits.fieldId.poke(33.U)
      dut.io.request.bits.write.poke(true.B)
      dut.io.request.bits.laneMask.poke("hffffffff".U)
      dut.io.request.bits.writeData.zipWithIndex.foreach { case (word, lane) => word.poke((0x41000000 + lane).U) }
      dut.io.request.valid.poke(true.B)
      dut.clock.step()
      dut.io.request.bits.write.poke(false.B)
      dut.clock.step()
      dut.io.response.ready.poke(false.B)
      // This request is not accepted, but its payload changes the macro's
      // address pins. The pending response must retain the previous row.
      dut.io.request.bits.warpId.poke(4.U)
      dut.io.request.bits.fieldId.poke(5.U)
      for (_ <- 0 until 5) {
        dut.io.request.ready.expect(false.B)
        dut.io.response.valid.expect(true.B)
        dut.io.response.bits.readData.zipWithIndex.foreach { case (word, lane) => word.expect((0x41000000 + lane).U) }
        dut.clock.step()
      }
      dut.io.response.ready.poke(true.B)
      dut.clock.step()
      dut.io.request.valid.poke(false.B)
      dut.io.response.valid.expect(false.B)
    }
  }

  private def idle(dut: RtLocalSram): Unit = {
    dut.io.request.valid.poke(false.B)
    dut.io.request.bits.warpId.poke(0.U); dut.io.request.bits.fieldId.poke(0.U)
    dut.io.request.bits.write.poke(false.B); dut.io.request.bits.laneMask.poke(0.U)
    dut.io.request.bits.writeData.foreach(_.poke(0.U)); dut.io.response.ready.poke(true.B)
  }
  "maps a field-major warp row across all 32 banks" in {
    test(new RtLocalSram) { dut =>
      idle(dut); dut.clock.step()
      dut.io.request.bits.warpId.poke(7.U); dut.io.request.bits.fieldId.poke(36.U)
      dut.io.request.bits.write.poke(true.B); dut.io.request.bits.laneMask.poke("hffffffff".U)
      dut.io.request.bits.writeData.zipWithIndex.foreach { case (word, lane) => word.poke((0x100 + lane).U) }
      dut.io.request.valid.poke(true.B); dut.io.request.ready.expect(true.B); dut.clock.step()
      dut.io.request.valid.poke(false.B); dut.clock.step()
      dut.io.request.bits.write.poke(false.B); dut.io.request.valid.poke(true.B)
      dut.io.request.ready.expect(true.B); dut.clock.step(); dut.io.request.valid.poke(false.B)
      dut.io.response.valid.expect(true.B)
      dut.io.response.bits.readData.zipWithIndex.foreach { case (word, lane) => word.expect((0x100 + lane).U) }
    }
  }

  "does not accept a request while a synchronous read response is live" in {
    test(new RtLocalSram) { dut =>
      idle(dut); dut.clock.step()
      dut.io.request.bits.warpId.poke(0.U)
      dut.io.request.bits.fieldId.poke(18.U)
      dut.io.request.bits.write.poke(true.B)
      dut.io.request.bits.laneMask.poke("hffffffff".U)
      dut.io.request.bits.writeData.zipWithIndex.foreach { case (word, lane) => word.poke(lane.U) }
      dut.io.request.valid.poke(true.B); dut.clock.step()

      dut.io.request.bits.fieldId.poke(20.U)
      dut.io.request.bits.writeData.foreach(_.poke(1.U))
      dut.clock.step()

      dut.io.request.bits.fieldId.poke(18.U)
      dut.io.request.bits.write.poke(false.B)
      dut.io.request.valid.poke(true.B); dut.io.request.ready.expect(true.B); dut.clock.step()
      dut.io.response.valid.expect(true.B)
      dut.io.response.bits.readData.zipWithIndex.foreach { case (word, lane) => word.expect(lane.U) }

      // A response is a stable, exclusive view of the synchronous macro.
      dut.io.request.bits.fieldId.poke(20.U)
      dut.io.request.ready.expect(false.B); dut.clock.step()
      dut.io.request.ready.expect(true.B); dut.clock.step()
      dut.io.response.valid.expect(true.B)
      dut.io.response.bits.readData.foreach(_.expect(1.U))
    }
  }
}

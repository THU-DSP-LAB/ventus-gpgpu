package rtcore

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class RtSpillTransferTest extends AnyFreeSpec with ChiselScalatestTester {
  "spill waits for every page ack; restore skips unused overflow and TERMINATE skips all pages" in {
    test(new RtSpillTransfer).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      dut.io.command.valid.poke(false.B)
      dut.io.command.bits.pointer.poke("h100000".U)
      dut.io.command.bits.restore.poke(false.B)
      dut.io.command.bits.terminate.poke(false.B)
      val image = dut.io.command.bits.image
      for (i <- 0 until 32) {
        image.baseNodeRef(i).poke((0x1000 + i).U)
        image.overflowNodeRef(i).poke((0x2000 + i).U)
      }
      image.baseObjectTags.poke("h80000001".U)
      image.overflowObjectTags.poke(2.U)
      image.stackDepth.poke(49.U)
      image.valid.poke(true.B)
      image.instanceNodeRef.poke("h12345678".U)
      dut.io.result.ready.poke(false.B)
      dut.io.request.ready.poke(false.B)
      dut.io.response.valid.poke(false.B)
      dut.io.response.bits.clientId.poke(1.U)
      dut.io.response.bits.clientTag.poke(30.U)
      dut.io.response.bits.lineBase.poke(0.U)
      dut.io.response.bits.coverageMask.poke(0.U)
      dut.io.response.bits.data.foreach(_.poke(0.U))
      dut.io.response.bits.fault.poke(false.B)

      def start(restore: Boolean, terminate: Boolean = false): Unit = {
        dut.io.command.bits.restore.poke(restore.B)
        dut.io.command.bits.terminate.poke(terminate.B)
        dut.io.command.valid.poke(true.B)
        dut.io.command.ready.expect(true.B)
        dut.clock.step()
        dut.io.command.valid.poke(false.B)
      }
      def transfer(page: Int, restore: Boolean, depth: Int = 32): Unit = {
        dut.io.request.valid.expect(true.B)
        dut.io.request.bits.lineBase.expect((0x100000 + page * 128).U)
        dut.io.request.bits.write.expect((!restore).B)
        dut.clock.step(3)
        dut.io.request.valid.expect(true.B)
        dut.io.result.valid.expect(false.B)
        dut.io.request.ready.poke(true.B)
        dut.clock.step()
        dut.io.request.ready.poke(false.B)
        dut.clock.step(3)
        dut.io.result.valid.expect(false.B)
        dut.io.response.bits.data.foreach(_.poke(0.U))
        if (page == 1) {
          dut.io.response.bits.data(0).poke("h80000001".U)
          dut.io.response.bits.data(1).poke(2.U)
          dut.io.response.bits.data(2).poke(depth.U)
          dut.io.response.bits.data(3).poke(1.U)
          dut.io.response.bits.data(4).poke("h12345678".U)
        } else {
          for (i <- 0 until 32)
            dut.io.response.bits.data(i).poke(((if (page == 0) 0x3000 else 0x4000) + i).U)
        }
        dut.io.response.valid.poke(true.B)
        dut.io.response.bits.lineBase.poke((0x100000 + page * 128).U)
        dut.io.response.ready.expect(true.B)
        dut.clock.step()
        dut.io.response.valid.poke(false.B)
      }
      def finish(): Unit = {
        dut.io.result.valid.expect(true.B)
        dut.io.request.valid.expect(false.B)
        dut.clock.step(3)
        dut.io.result.valid.expect(true.B)
        dut.io.result.ready.poke(true.B)
        dut.clock.step()
        dut.io.result.ready.poke(false.B)
      }
      start(false)
      dut.io.request.bits.writeData(31).expect("h101f".U)
      transfer(0, false)
      dut.io.request.bits.writeData(2).expect(49.U)
      dut.io.request.bits.writeData(4).expect("h12345678".U)
      transfer(1, false)
      dut.io.request.bits.writeData(31).expect("h201f".U)
      transfer(2, false)
      finish()
      for (depth <- Seq(0, 32, 49, 64)) {
        start(true)
        transfer(0, true)
        transfer(1, true, depth)
        if (depth > 32) transfer(2, true)
        dut.io.result.bits.image.stackDepth.expect(depth.U)
        dut.io.result.bits.image.baseObjectTags.expect("h80000001".U)
        dut.io.result.bits.image.instanceNodeRef.expect("h12345678".U)
        for (i <- 0 until 32) {
          dut.io.result.bits.image.baseNodeRef(i).expect((0x3000 + i).U)
          dut.io.result.bits.image.overflowNodeRef(i).expect((if (depth > 32) 0x4000 + i else 0).U)
        }
        finish()
      }
      start(true, terminate = true)
      dut.io.request.valid.expect(false.B)
      dut.io.result.bits.terminate.expect(true.B)
      finish()
    }
  }
}

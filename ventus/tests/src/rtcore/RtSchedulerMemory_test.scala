package rtcore

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import top.parameters._

class RtLocalReadArbiterTest extends AnyFreeSpec with ChiselScalatestTester {
  private val banks = RtLocalSramParameters.corePortCount

  private def defaults(dut: RtLocalReadArbiter): Unit = {
    dut.io.parentRequest.valid.poke(false.B)
    request(dut.io.parentRequest.bits, lane = 0)
    dut.io.parentResponse.ready.poke(true.B)
    for (bank <- 0 until banks) {
      dut.io.contextRequest(bank).valid.poke(false.B)
      request(dut.io.contextRequest(bank).bits, lane = bank)
      dut.io.contextResponse(bank).ready.poke(true.B)
      dut.io.localRequest(bank).valid.poke(false.B)
      request(dut.io.localRequest(bank).bits, lane = bank)
      dut.io.localResponse(bank).ready.poke(true.B)
      dut.io.bankRequest(bank).ready.poke(true.B)
      dut.io.bankResponse(bank).valid.poke(false.B)
      dut.io.bankResponse(bank).bits.readData.poke(0.U)
    }
  }

  private def request(dst: RtLocalSramNarrowRequest, lane: Int,
                      row: Int = 0, write: Boolean = false): Unit = {
    dst.warpId.poke(0.U)
    dst.rowBase.poke(row.U)
    dst.laneId.poke(lane.U)
    dst.fieldId.poke(RtAbiLayout.SlotOriginX.U)
    dst.write.poke(write.B)
    dst.writeData.poke(123.U)
  }

  "parent reads win collisions and retain response ownership under backpressure" in {
    test(new RtLocalReadArbiter).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      defaults(dut)
      dut.clock.step()
      request(dut.io.parentRequest.bits, lane = 19, row = 120)
      request(dut.io.contextRequest(0).bits, lane = 3, row = 40)
      dut.io.parentRequest.valid.poke(true.B)
      dut.io.contextRequest(0).valid.poke(true.B)
      dut.io.localRequest(3).valid.poke(true.B)
      dut.io.bankRequest(3).ready.poke(false.B)
      dut.io.bankRequest(3).bits.rowBase.expect(120.U)
      dut.io.parentRequest.ready.expect(false.B)
      dut.io.contextRequest(0).ready.expect(false.B)
      dut.io.localRequest(3).ready.expect(false.B)
      dut.clock.step()
      dut.io.quiescent.expect(true.B)
      dut.io.bankRequest(3).ready.poke(true.B)
      dut.io.parentRequest.ready.expect(true.B)
      dut.clock.step()
      dut.io.parentRequest.valid.poke(false.B)
      dut.io.localRequest(3).valid.poke(false.B)
      dut.io.quiescent.expect(false.B)
      dut.io.contextRequest(0).ready.expect(false.B)
      dut.io.bankResponse(3).valid.poke(true.B)
      dut.io.bankResponse(3).bits.readData.poke(456.U)
      dut.io.parentResponse.ready.poke(false.B)
      dut.io.parentResponse.valid.expect(true.B)
      dut.io.parentResponse.bits.readData.expect(456.U)
      dut.io.contextResponse(0).valid.expect(false.B)
      dut.io.localResponse(3).valid.expect(false.B)
      dut.io.bankResponse(3).ready.expect(false.B)
      dut.clock.step(2)
      dut.io.quiescent.expect(false.B)
      dut.io.parentResponse.ready.poke(true.B)
      dut.clock.step()
      dut.io.bankResponse(3).valid.poke(false.B)
      dut.io.quiescent.expect(true.B)
      dut.io.contextRequest(0).ready.expect(true.B)
    }
  }

  "context responses follow the reader rather than the physical parent bank" in {
    test(new RtLocalReadArbiter).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      defaults(dut)
      dut.clock.step()
      request(dut.io.contextRequest(0).bits, lane = 19, row = 120)
      request(dut.io.contextRequest(1).bits, lane = 11, row = 80)
      request(dut.io.contextRequest(2).bits, lane = 20, row = 40)
      for (reader <- 0 to 2) dut.io.contextRequest(reader).valid.poke(true.B)
      dut.io.contextRequest(0).ready.expect(true.B)
      dut.io.contextRequest(1).ready.expect(false.B)
      dut.io.contextRequest(2).ready.expect(true.B)
      dut.io.bankRequest(3).bits.rowBase.expect(120.U)
      dut.io.bankRequest(4).bits.rowBase.expect(40.U)
      dut.clock.step()
      dut.io.contextRequest(0).valid.poke(false.B)
      dut.io.contextRequest(2).valid.poke(false.B)
      dut.io.bankResponse(3).valid.poke(true.B)
      dut.io.bankResponse(3).bits.readData.poke(333.U)
      dut.io.bankResponse(4).valid.poke(true.B)
      dut.io.bankResponse(4).bits.readData.poke(444.U)
      dut.io.contextResponse(0).ready.poke(false.B)
      dut.io.contextResponse(0).valid.expect(true.B)
      dut.io.contextResponse(0).bits.readData.expect(333.U)
      dut.io.contextResponse(1).valid.expect(false.B)
      dut.io.contextResponse(2).valid.expect(true.B)
      dut.io.contextResponse(2).bits.readData.expect(444.U)
      dut.clock.step()
      dut.io.bankResponse(4).valid.poke(false.B)
      dut.io.quiescent.expect(false.B)
      dut.io.contextRequest(1).ready.expect(false.B)
      dut.io.contextResponse(0).ready.poke(true.B)
      dut.clock.step()
      dut.io.bankResponse(3).valid.poke(false.B)
      dut.io.quiescent.expect(true.B)
      dut.io.contextRequest(1).ready.expect(true.B)
    }
  }

  "parent writes need no read response and ordinary LocalState responses pass through" in {
    test(new RtLocalReadArbiter).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      defaults(dut)
      dut.clock.step()
      request(dut.io.parentRequest.bits, lane = 19, write = true)
      dut.io.parentRequest.valid.poke(true.B)
      dut.clock.step()
      dut.io.parentRequest.valid.poke(false.B)
      dut.io.quiescent.expect(true.B)
      dut.io.localRequest(3).valid.poke(true.B)
      dut.io.localRequest(3).ready.expect(true.B)
      dut.clock.step()
      dut.io.localRequest(3).valid.poke(false.B)
      dut.io.bankResponse(3).valid.poke(true.B)
      dut.io.bankResponse(3).bits.readData.poke(789.U)
      dut.io.localResponse(3).ready.poke(false.B)
      dut.io.localResponse(3).valid.expect(true.B)
      dut.io.localResponse(3).bits.readData.expect(789.U)
      dut.io.parentResponse.valid.expect(false.B)
      dut.io.bankResponse(3).ready.expect(false.B)
      dut.io.localResponse(3).ready.poke(true.B)
      dut.io.bankResponse(3).ready.expect(true.B)
      dut.clock.step()
    }
  }
}

class RtSpillLsuArbiterTest extends AnyFreeSpec with ChiselScalatestTester {
  "LSU ownership changes only at transaction boundaries and propagates backpressure" in {
    test(new RtSpillLsuArbiter).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      dut.io.begin.poke(false.B)
      dut.io.end.poke(false.B)
      dut.io.tableQuiescent.poke(true.B)
      dut.io.request.ready.poke(true.B)
      dut.io.response.valid.poke(true.B)
      dut.io.response.bits.clientId.poke(0.U)
      dut.io.response.bits.clientTag.poke(30.U)
      dut.io.response.bits.lineBase.poke(384.U)
      dut.io.response.bits.coverageMask.poke(1.U)
      dut.io.response.bits.fault.poke(false.B)
      for (word <- 0 until dcache_BlockWords) dut.io.response.bits.data(word).poke(word.U)
      for ((port, tag) <- Seq((dut.io.asRequest, 1), (dut.io.spillRequest, 30))) {
        port.valid.poke(true.B)
        port.bits.clientId.poke(0.U)
        port.bits.clientTag.poke(tag.U)
        port.bits.lineBase.poke((tag * 128).U)
        port.bits.coverageMask.poke(1.U)
        port.bits.write.poke(false.B)
        for (word <- 0 until dcache_BlockWords) port.bits.writeData(word).poke(0.U)
      }
      dut.io.asResponse.ready.poke(true.B)
      dut.io.spillResponse.ready.poke(false.B)
      dut.io.request.bits.clientTag.expect(1.U)
      dut.io.asRequest.ready.expect(true.B)
      dut.io.spillRequest.ready.expect(false.B)
      dut.io.asResponse.valid.expect(true.B)
      dut.io.spillResponse.valid.expect(false.B)
      dut.io.response.ready.expect(true.B)
      dut.io.begin.poke(true.B)
      dut.clock.step()
      dut.io.begin.poke(false.B)
      dut.io.request.bits.clientTag.expect(30.U)
      dut.io.asRequest.ready.expect(false.B)
      dut.io.spillRequest.ready.expect(true.B)
      dut.io.asResponse.valid.expect(false.B)
      dut.io.spillResponse.valid.expect(true.B)
      dut.io.spillResponse.bits.data(7).expect(7.U)
      dut.io.response.ready.expect(false.B)
      dut.io.request.ready.poke(false.B)
      dut.io.spillRequest.ready.expect(false.B)
      dut.clock.step(2)
      dut.io.request.bits.clientTag.expect(30.U)
      dut.io.spillResponse.ready.poke(true.B)
      dut.io.response.ready.expect(true.B)
      dut.io.end.poke(true.B)
      dut.clock.step()
      dut.io.end.poke(false.B)
      dut.io.request.ready.poke(true.B)
      dut.io.request.bits.clientTag.expect(1.U)
      dut.io.asRequest.ready.expect(true.B)
      dut.io.asResponse.valid.expect(true.B)
      dut.io.spillResponse.valid.expect(false.B)
    }
  }
}

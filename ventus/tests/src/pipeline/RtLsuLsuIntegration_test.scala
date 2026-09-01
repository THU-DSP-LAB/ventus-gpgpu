package pipeline

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import rtcore._

/** Test-only RTcore-to-LSU wiring with controllable request backpressure. */
class RtLsuLsuHarness extends Module {
  val io = IO(new Bundle {
    val rtRequestEnable = Input(Bool())
    val pdsRead = Flipped(Decoupled(new RtLsuCandidate))
    val pdsReadInjection = Decoupled(new RtPdsReadInjection)
    val dcacheReq = Decoupled(new DCacheCoreReq_np)
    val dcacheRsp = Flipped(Decoupled(new DCacheCoreRsp_np))
  })

  val rt = Module(new RtLsuMshr)
  val lsu = Module(new LSUexe)
  val requestQueue = Module(new Queue(new _root_.lsu.LsuClientRequest, entries = 1, pipe = true))

  rt.io.pdsRead <> io.pdsRead
  rt.io.pdsWrite.valid := false.B
  rt.io.pdsWrite.bits := 0.U.asTypeOf(new RtLsuCandidate)
  rt.io.as.valid := false.B
  rt.io.as.bits := 0.U.asTypeOf(new RtLsuCandidate)
  io.pdsReadInjection <> rt.io.pdsReadInjection
  rt.io.pdsWriteInjection.ready := true.B
  rt.io.asInjection.ready := true.B

  requestQueue.io.enq.valid := rt.io.request.valid && io.rtRequestEnable
  requestQueue.io.enq.bits := rt.io.request.bits
  rt.io.request.ready := requestQueue.io.enq.ready && io.rtRequestEnable
  lsu.io.cachelineClientReq <> requestQueue.io.deq
  rt.io.response <> lsu.io.cachelineClientRsp

  lsu.io.lsu_req.valid := false.B
  lsu.io.lsu_req.bits := 0.U.asTypeOf(new vExeData)
  lsu.io.lsu_rsp.ready := true.B
  io.dcacheReq <> lsu.io.dcache_req
  lsu.io.dcache_rsp <> io.dcacheRsp
  lsu.io.shared_req.ready := true.B
  lsu.io.shared_rsp.valid := false.B
  lsu.io.shared_rsp.bits := 0.U.asTypeOf(new DCacheCoreRsp_np)
  lsu.io.flush_dcache.valid := false.B
  lsu.io.flush_dcache.bits := false.B
  lsu.io.csr_pds := 0.U
  lsu.io.csr_numw := 1.U
  lsu.io.csr_numt := 1.U
  lsu.io.csr_tid := 0.U
}

class RtLsuLsuIntegrationTest extends AnyFreeSpec with ChiselScalatestTester {
  private def zero(data: Data): Unit = data match {
    case bool: Bool => bool.poke(false.B)
    case uint: UInt => uint.poke(0.U)
    case aggregate: Aggregate => aggregate.getElements.foreach(zero)
  }

  private def idle(dut: RtLsuLsuHarness): Unit = {
    dut.io.rtRequestEnable.poke(false.B)
    dut.io.pdsRead.valid.poke(false.B)
    zero(dut.io.pdsRead.bits)
    dut.io.pdsReadInjection.ready.poke(true.B)
    dut.io.dcacheReq.ready.poke(true.B)
    dut.io.dcacheRsp.valid.poke(false.B)
    zero(dut.io.dcacheRsp.bits)
  }

  private def read64(dut: RtLsuLsuHarness, address: Int): Unit = {
    dut.io.pdsRead.bits.source.poke(RtLsuSource.pdsRead)
    dut.io.pdsRead.bits.cacheLineAddress.poke(address.U)
    dut.io.pdsRead.bits.write.poke(false.B)
    dut.io.pdsRead.bits.is64B.poke(true.B)
    dut.io.pdsRead.bits.wordMask.poke("hffff".U)
    dut.io.pdsRead.bits.writeData.foreach(_.poke(0.U))
    zero(dut.io.pdsRead.bits.locator)
    dut.io.pdsRead.valid.poke(true.B)
    dut.io.pdsRead.ready.expect(true.B)
    dut.clock.step()
    dut.io.pdsRead.valid.poke(false.B)
  }

  "RTcore merges 64B halves before LSU and receives the single cacheline reply" in {
    test(new RtLsuLsuHarness).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      idle(dut)
      dut.clock.step()

      read64(dut, 0x2800)
      read64(dut, 0x2840)
      dut.io.dcacheReq.valid.expect(false.B)

      dut.io.dcacheReq.ready.poke(false.B)
      dut.io.rtRequestEnable.poke(true.B)
      dut.clock.step(3)
      dut.io.dcacheReq.valid.expect(true.B)
      dut.io.dcacheReq.bits.perLaneAddr(0).activeMask.expect(true.B)
      dut.io.dcacheReq.bits.perLaneAddr(31).activeMask.expect(true.B)
      dut.io.dcacheReq.bits.setIdx.expect(0x10.U)
      val instrId = dut.io.dcacheReq.bits.instrId.peek().litValue
      dut.io.dcacheReq.ready.poke(true.B)
      dut.clock.step()
      dut.io.dcacheReq.valid.expect(false.B)

      dut.io.pdsReadInjection.ready.poke(false.B)
      dut.io.dcacheRsp.bits.instrId.poke(instrId.U)
      dut.io.dcacheRsp.bits.activeMask.foreach(_.poke(true.B))
      dut.io.dcacheRsp.bits.data.zipWithIndex.foreach { case (word, index) =>
        word.poke((0x900 + index).U)
      }
      dut.io.dcacheRsp.valid.poke(true.B)
      dut.io.dcacheRsp.ready.expect(true.B)
      dut.clock.step()
      dut.io.dcacheRsp.valid.poke(false.B)
      dut.clock.step(2)

      for (firstWord <- Seq(0x900, 0x910)) {
        dut.io.pdsReadInjection.valid.expect(true.B)
        dut.io.pdsReadInjection.bits.data(0).expect(firstWord.U)
        dut.io.pdsReadInjection.ready.poke(true.B)
        dut.clock.step()
        dut.io.pdsReadInjection.ready.poke(false.B)
      }
      dut.io.pdsReadInjection.valid.expect(false.B)
    }
  }
}

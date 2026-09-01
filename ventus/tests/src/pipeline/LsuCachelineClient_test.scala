package pipeline

import chisel3._
import chiseltest._
import lsu.LsuClientId
import org.scalatest.freespec.AnyFreeSpec

/** Verifies that the RT cacheline port never exposes the DCache instrId. */
class LsuCachelineClientTest extends AnyFreeSpec with ChiselScalatestTester {
  private def zero(data: Data): Unit = data match {
    case bool: Bool => bool.poke(false.B)
    case uint: UInt => uint.poke(0.U)
    case aggregate: Aggregate => aggregate.getElements.foreach(zero)
  }

  private def driveIdle(dut: LSUexe): Unit = {
    dut.io.lsu_req.valid.poke(false.B)
    zero(dut.io.lsu_req.bits)
    dut.io.lsu_rsp.ready.poke(true.B)
    dut.io.cachelineClientReq.valid.poke(false.B)
    zero(dut.io.cachelineClientReq.bits)
    dut.io.cachelineClientRsp.ready.poke(true.B)
    dut.io.dcache_req.ready.poke(true.B)
    dut.io.dcache_rsp.valid.poke(false.B)
    zero(dut.io.dcache_rsp.bits)
    dut.io.shared_req.ready.poke(true.B)
    dut.io.shared_rsp.valid.poke(false.B)
    zero(dut.io.shared_rsp.bits)
    dut.io.flush_dcache.valid.poke(false.B)
    dut.io.flush_dcache.bits.poke(false.B)
    dut.io.csr_pds.poke(0.U)
    dut.io.csr_numw.poke(1.U)
    dut.io.csr_numt.poke(1.U)
    dut.io.csr_tid.poke(0.U)
  }

  "LSU allocates and consumes the DCache tag behind the RT cacheline client port" in {
    test(new LSUexe).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      driveIdle(dut)
      dut.clock.step()

      // clientId=2 verifies that LSU preserves a generic non-PIPE endpoint
      // instead of hard-coding the RTCORE ID at its ingress.
      dut.io.cachelineClientReq.bits.clientId.poke(2.U)
      dut.io.cachelineClientReq.bits.lineBase.poke(0x1800.U)
      dut.io.cachelineClientReq.bits.coverageMask.poke("h0000ffff".U)
      dut.io.cachelineClientReq.bits.write.poke(false.B)
      dut.io.cachelineClientReq.bits.writeData.foreach(_.poke(0.U))
      dut.io.cachelineClientReq.valid.poke(true.B)
      dut.io.cachelineClientReq.ready.expect(true.B)
      dut.clock.step()
      dut.io.cachelineClientReq.valid.poke(false.B)

      // The physical instrId is visible only on the DCache-side interface.
      dut.io.dcache_req.valid.expect(true.B)
      dut.io.dcache_req.bits.setIdx.expect(0x10.U)
      dut.io.dcache_req.bits.perLaneAddr(0).activeMask.expect(true.B)
      dut.io.dcache_req.bits.perLaneAddr(15).activeMask.expect(true.B)
      dut.io.dcache_req.bits.perLaneAddr(16).activeMask.expect(false.B)
      val instrId = dut.io.dcache_req.bits.instrId.peek().litValue
      dut.clock.step()

      dut.io.dcache_rsp.bits.instrId.poke(instrId.U)
      dut.io.dcache_rsp.bits.activeMask.zipWithIndex.foreach { case (mask, index) =>
        mask.poke((index < 16).B)
      }
      dut.io.dcache_rsp.bits.data.zipWithIndex.foreach { case (word, index) =>
        word.poke((0x400 + index).U)
      }
      dut.io.dcache_rsp.valid.poke(true.B)
      dut.io.dcache_rsp.ready.expect(true.B)
      dut.clock.step()
      dut.io.dcache_rsp.valid.poke(false.B)

      dut.io.cachelineClientRsp.valid.expect(true.B)
      dut.io.cachelineClientRsp.bits.clientId.expect(2.U)
      dut.io.cachelineClientRsp.bits.lineBase.expect(0x1800.U)
      dut.io.cachelineClientRsp.bits.coverageMask.expect("h0000ffff".U)
      dut.io.cachelineClientRsp.bits.data(0).expect(0x400.U)
      dut.io.cachelineClientRsp.bits.data(15).expect(0x40f.U)
      dut.clock.step()
    }
  }
}

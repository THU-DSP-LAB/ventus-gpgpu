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

  private def driveClientRequest(dut: LSUexe, clientId: Int, lineBase: Int,
      mask: BigInt, write: Boolean = false, dataBase: Int = 0): Unit = {
    dut.io.cachelineClientReq.bits.clientId.poke(clientId.U)
    dut.io.cachelineClientReq.bits.lineBase.poke(lineBase.U)
    dut.io.cachelineClientReq.bits.coverageMask.poke(mask.U)
    dut.io.cachelineClientReq.bits.write.poke(write.B)
    dut.io.cachelineClientReq.bits.writeData.zipWithIndex.foreach { case (word, index) =>
      word.poke((dataBase + index).U)
    }
    dut.io.cachelineClientReq.valid.poke(true.B)
  }

  private def returnDcache(dut: LSUexe, instrId: BigInt, mask: BigInt,
      dataBase: Int): Unit = {
    dut.io.dcache_rsp.bits.instrId.poke(instrId.U)
    dut.io.dcache_rsp.bits.activeMask.zipWithIndex.foreach { case (active, index) =>
      active.poke(mask.testBit(index).B)
    }
    dut.io.dcache_rsp.bits.data.zipWithIndex.foreach { case (word, index) =>
      word.poke((dataBase + index).U)
    }
    dut.io.dcache_rsp.valid.poke(true.B)
    dut.io.dcache_rsp.ready.expect(true.B)
    dut.clock.step()
    dut.io.dcache_rsp.valid.poke(false.B)
  }

  private def drivePipeLoad(dut: LSUexe, address: Int): Unit = {
    zero(dut.io.lsu_req.bits)
    dut.io.lsu_req.bits.in1(0).poke(address.U)
    dut.io.lsu_req.bits.mask(0).poke(true.B)
    dut.io.lsu_req.bits.ctrl.isvec.poke(true.B)
    dut.io.lsu_req.bits.ctrl.mem_cmd.poke(1.U) // M_XRD
    dut.io.lsu_req.bits.ctrl.mem_whb.poke(3.U) // MEM_W
    dut.io.lsu_req.bits.ctrl.mem_unsigned.poke(true.B)
    dut.io.lsu_req.bits.ctrl.mop.poke(0.U)
    dut.io.lsu_req.valid.poke(true.B)
  }

  private def acceptDcache(dut: LSUexe): (BigInt, BigInt) = {
    dut.io.dcache_req.ready.poke(false.B)
    var waitCycles = 0
    while (!dut.io.dcache_req.valid.peek().litToBoolean && waitCycles < 12) {
      dut.clock.step()
      waitCycles += 1
    }
    assert(dut.io.dcache_req.valid.peek().litToBoolean,
      s"DCache request did not arrive after $waitCycles cycles")
    val instrId = dut.io.dcache_req.bits.instrId.peek().litValue
    val mask = dut.io.dcache_req.bits.perLaneAddr.map(_.activeMask.peek().litToBoolean)
      .zipWithIndex.collect { case (true, index) => BigInt(1) << index }.foldLeft(BigInt(0))(_ | _)
    dut.io.dcache_req.ready.poke(true.B)
    dut.clock.step()
    (instrId, mask)
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

  "LSU accumulates a multi-beat cacheline reply and holds it under client backpressure" in {
    test(new LSUexe).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      driveIdle(dut)
      dut.clock.step()

      driveClientRequest(dut, clientId = 2, lineBase = 0x2000, mask = BigInt("ffffffff", 16))
      dut.io.cachelineClientReq.ready.expect(true.B)
      dut.clock.step()
      dut.io.cachelineClientReq.valid.poke(false.B)

      dut.io.dcache_req.valid.expect(true.B)
      val instrId = dut.io.dcache_req.bits.instrId.peek().litValue
      dut.clock.step()

      returnDcache(dut, instrId, BigInt("0000ffff", 16), 0x100)
      dut.io.cachelineClientRsp.valid.expect(false.B)

      dut.io.cachelineClientRsp.ready.poke(false.B)
      returnDcache(dut, instrId, BigInt("ffff0000", 16), 0x200)
      dut.io.cachelineClientRsp.valid.expect(true.B)
      dut.io.cachelineClientRsp.bits.clientId.expect(2.U)
      dut.io.cachelineClientRsp.bits.lineBase.expect(0x2000.U)
      dut.io.cachelineClientRsp.bits.coverageMask.expect("hffffffff".U)
      dut.io.cachelineClientRsp.bits.data(0).expect(0x100.U)
      dut.io.cachelineClientRsp.bits.data(15).expect(0x10f.U)
      dut.io.cachelineClientRsp.bits.data(16).expect(0x210.U)
      dut.io.cachelineClientRsp.bits.data(31).expect(0x21f.U)
      dut.clock.step(2)
      dut.io.cachelineClientRsp.valid.expect(true.B)
      dut.io.cachelineClientRsp.bits.data(16).expect(0x210.U)

      dut.io.cachelineClientRsp.ready.poke(true.B)
      dut.clock.step()
      dut.io.cachelineClientRsp.valid.expect(false.B)
    }
  }

  "LSU preserves queued generic writes and routes client IDs after out-of-order replies" in {
    test(new LSUexe).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      driveIdle(dut)
      dut.clock.step()

      // Keep DCache stalled while a full-line write is resident in clientPending.
      dut.io.dcache_req.ready.poke(false.B)
      driveClientRequest(dut, clientId = 2, lineBase = 0x2800,
        mask = BigInt("ffffffff", 16), write = true, dataBase = 0x500)
      dut.io.cachelineClientReq.ready.expect(true.B)
      dut.clock.step()
      dut.io.cachelineClientReq.valid.poke(false.B)
      dut.io.dcache_req.valid.expect(true.B)
      dut.io.dcache_req.bits.opcode.expect(1.U)
      dut.io.dcache_req.bits.data(0).expect(0x500.U)
      dut.io.dcache_req.bits.data(31).expect(0x51f.U)
      dut.clock.step(2)
      dut.io.dcache_req.bits.data(31).expect(0x51f.U)

      dut.io.dcache_req.ready.poke(true.B)
      val writeInstrId = dut.io.dcache_req.bits.instrId.peek().litValue
      dut.clock.step()

      driveClientRequest(dut, clientId = 3, lineBase = 0x3000, mask = BigInt("00000003", 16))
      dut.io.cachelineClientReq.ready.expect(true.B)
      dut.clock.step()
      dut.io.cachelineClientReq.valid.poke(false.B)
      dut.io.dcache_req.valid.expect(true.B)
      val readInstrId = dut.io.dcache_req.bits.instrId.peek().litValue
      assert(readInstrId != writeInstrId, "DCache instrId was reused before the write completed")
      dut.clock.step()

      // DCache may return the later request first.  It must retain client 3's metadata.
      returnDcache(dut, readInstrId, BigInt("00000003", 16), 0x700)
      dut.io.cachelineClientRsp.valid.expect(true.B)
      dut.io.cachelineClientRsp.bits.clientId.expect(3.U)
      dut.io.cachelineClientRsp.bits.lineBase.expect(0x3000.U)
      dut.io.cachelineClientRsp.bits.data(1).expect(0x701.U)
      dut.clock.step()

      returnDcache(dut, writeInstrId, BigInt("ffffffff", 16), 0)
      dut.io.cachelineClientRsp.valid.expect(true.B)
      dut.io.cachelineClientRsp.bits.clientId.expect(2.U)
      dut.io.cachelineClientRsp.bits.lineBase.expect(0x2800.U)
      dut.io.cachelineClientRsp.bits.coverageMask.expect("hffffffff".U)
      dut.clock.step()
    }
  }

  "PIPE and a generic client retain separate MSHRs and responses" in {
    test(new LSUexe).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      driveIdle(dut)
      dut.clock.step()

      // Present both upstream clients together.  PIPE's vector-load path takes
      // its normal FIFO/AddrCalculate path; generic traffic bypasses neither
      // the shared MSHR nor DCache arbitration.
      dut.io.dcache_req.ready.poke(false.B)
      drivePipeLoad(dut, address = 0x4000)
      driveClientRequest(dut, clientId = 2, lineBase = 0x3800, mask = 2)
      dut.io.lsu_req.ready.expect(true.B)
      dut.io.cachelineClientReq.ready.expect(true.B)
      dut.clock.step()
      dut.io.lsu_req.valid.poke(false.B)
      dut.io.cachelineClientReq.valid.poke(false.B)

      val first = acceptDcache(dut)
      val second = acceptDcache(dut)
      val requests = Seq(first, second)
      val generic = requests.find(_._2 == 2).getOrElse(fail("generic DCache request was not issued"))
      val pipe = requests.find(_._2 == 1).getOrElse(fail("PIPE DCache request was not issued"))
      assert(generic._1 != pipe._1, "PIPE and generic client shared an in-flight DCache instrId")

      // Reply to PIPE first and then generic, proving routing does not depend
      // on DCache completion order.
      returnDcache(dut, pipe._1, 1, 0x600)
      dut.io.lsu_rsp.valid.expect(true.B)
      dut.io.lsu_rsp.bits.tag.clientId.expect(LsuClientId.Pipe)
      dut.io.lsu_rsp.bits.data(0).expect(0x600.U)
      dut.clock.step()

      dut.io.cachelineClientRsp.ready.poke(false.B)
      returnDcache(dut, generic._1, 2, 0x700)
      dut.io.cachelineClientRsp.valid.expect(true.B)
      dut.io.cachelineClientRsp.bits.clientId.expect(2.U)
      dut.io.cachelineClientRsp.bits.lineBase.expect(0x3800.U)
      dut.io.cachelineClientRsp.bits.coverageMask.expect(2.U)
      dut.io.cachelineClientRsp.bits.data(1).expect(0x701.U)
      dut.io.cachelineClientRsp.ready.poke(true.B)
      dut.clock.step()
    }
  }

  "LSU rejects PIPE on the generic client port" in {
    intercept[ChiselAssertionError] {
      test(new LSUexe).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
        driveIdle(dut)
        driveClientRequest(dut, clientId = 0, lineBase = 0x4000, mask = 1)
        dut.clock.step()
      }
    }
  }

  "LSU rejects a non-cacheline-aligned generic request" in {
    intercept[ChiselAssertionError] {
      test(new LSUexe).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
        driveIdle(dut)
        driveClientRequest(dut, clientId = 2, lineBase = 0x4004, mask = 1)
        dut.clock.step()
      }
    }
  }
}

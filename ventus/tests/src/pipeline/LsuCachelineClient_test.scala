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

  /** Indexed vector load: each enabled lane supplies its full address. */
  private def driveSparsePipeLoad(dut: LSUexe, addresses: Map[Int, BigInt]): Unit = {
    zero(dut.io.lsu_req.bits)
    addresses.foreach { case (lane, address) =>
      dut.io.lsu_req.bits.in1(lane).poke(address.U)
      dut.io.lsu_req.bits.mask(lane).poke(true.B)
    }
    dut.io.lsu_req.bits.ctrl.isvec.poke(true.B)
    dut.io.lsu_req.bits.ctrl.mem_cmd.poke(1.U) // M_XRD
    dut.io.lsu_req.bits.ctrl.mem_whb.poke(3.U) // MEM_W
    dut.io.lsu_req.bits.ctrl.mem_unsigned.poke(true.B)
    // AddrCalculate uses in1 + in2 for mop=1; in2 remains zero above.
    dut.io.lsu_req.bits.ctrl.mop.poke(1.U)
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

  "LSU ignores a zero-mask reply for an unused tag while an AS-style read is outstanding" in {
    test(new LSUexe).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      driveIdle(dut)
      dut.clock.step()

      driveClientRequest(dut, clientId = 2, lineBase = 0x2400,
        mask = BigInt("ffffffff", 16))
      dut.io.cachelineClientReq.ready.expect(true.B)
      dut.clock.step()
      dut.io.cachelineClientReq.valid.poke(false.B)
      val asInstrId = acceptDcache(dut)._1
      val unusedInstrId = (asInstrId + 1) % 8

      // Flush/invalidate acknowledgements carry no lane data and do not own an
      // LSU MSHR.  They must not turn an unused entry into a fake completion.
      returnDcache(dut, unusedInstrId, 0, 0)
      dut.io.cachelineClientRsp.valid.expect(false.B)

      // Before the fix, MSHRv2 enters s_out here with no complete entry, so
      // this real AS-style reply is permanently backpressured.
      returnDcache(dut, asInstrId, BigInt("ffffffff", 16), 0x300)
      dut.io.cachelineClientRsp.valid.expect(true.B)
      dut.io.cachelineClientRsp.bits.clientId.expect(2.U)
      dut.io.cachelineClientRsp.bits.lineBase.expect(0x2400.U)
      dut.io.cachelineClientRsp.bits.data(0).expect(0x300.U)
      dut.io.cachelineClientRsp.bits.data(31).expect(0x31f.U)
      dut.clock.step()
    }
  }

  "LSU accepts an AS-style reply after a real flush acknowledgement" in {
    test(new LSUexe).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      driveIdle(dut)
      dut.clock.step()

      // Keep tag 0 allocated for the cacheline client, then allocate and
      // release another tag through PIPE.  AddrCalculate retains that second
      // tag for the following zero-mask flush acknowledgement.
      driveClientRequest(dut, clientId = 2, lineBase = 0x3400,
        mask = BigInt("ffffffff", 16))
      dut.clock.step()
      dut.io.cachelineClientReq.valid.poke(false.B)
      val asInstrId = acceptDcache(dut)._1

      drivePipeLoad(dut, address = 0x4000)
      dut.io.lsu_req.ready.expect(true.B)
      dut.clock.step()
      dut.io.lsu_req.valid.poke(false.B)
      val pipeInstrId = acceptDcache(dut)._1
      assert(pipeInstrId != asInstrId)
      returnDcache(dut, pipeInstrId, 1, 0x500)
      dut.io.lsu_rsp.valid.expect(true.B)
      dut.clock.step()

      dut.io.flush_dcache.valid.poke(true.B)
      dut.io.flush_dcache.ready.expect(true.B)
      dut.clock.step()
      dut.io.flush_dcache.valid.poke(false.B)
      val flushInstrId = acceptDcache(dut)._1
      assert(flushInstrId == pipeInstrId)
      returnDcache(dut, flushInstrId, 0, 0)

      returnDcache(dut, asInstrId, BigInt("ffffffff", 16), 0x600)
      dut.io.cachelineClientRsp.valid.expect(true.B)
      dut.io.cachelineClientRsp.bits.clientId.expect(2.U)
      dut.io.cachelineClientRsp.bits.lineBase.expect(0x3400.U)
      dut.io.cachelineClientRsp.bits.data(31).expect(0x61f.U)
      dut.clock.step()
    }
  }

  "LSU accepts a zero-mask reply and a new cacheline request in one cycle" in {
    test(new LSUexe).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      driveIdle(dut)
      dut.clock.step()

      driveClientRequest(dut, clientId = 2, lineBase = 0x4400,
        mask = BigInt("ffffffff", 16))
      dut.clock.step()
      dut.io.cachelineClientReq.valid.poke(false.B)
      val asInstrId = acceptDcache(dut)._1

      driveClientRequest(dut, clientId = 3, lineBase = 0x4800,
        mask = BigInt("ffffffff", 16))
      dut.io.dcache_rsp.bits.instrId.poke(((asInstrId + 2) % 8).U)
      dut.io.dcache_rsp.bits.activeMask.foreach(_.poke(false.B))
      dut.io.dcache_rsp.bits.data.foreach(_.poke(0.U))
      dut.io.dcache_rsp.valid.poke(true.B)
      dut.io.cachelineClientReq.ready.expect(true.B)
      dut.io.dcache_rsp.ready.expect(true.B)
      dut.clock.step()
      dut.io.cachelineClientReq.valid.poke(false.B)
      dut.io.dcache_rsp.valid.poke(false.B)
      // Simultaneous address allocation takes MSHRv2 through s_add for one
      // cycle before it can accept the held AS response.
      dut.clock.step()

      returnDcache(dut, asInstrId, BigInt("ffffffff", 16), 0x700)
      dut.io.cachelineClientRsp.valid.expect(true.B)
      dut.io.cachelineClientRsp.bits.lineBase.expect(0x4400.U)
      dut.clock.step()
    }
  }

  "LSU ignores a zero-mask reply after its tag has been reused" in {
    test(new LSUexe).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      driveIdle(dut)
      dut.clock.step()

      driveClientRequest(dut, clientId = 2, lineBase = 0x4c00,
        mask = BigInt("ffffffff", 16))
      dut.clock.step()
      dut.io.cachelineClientReq.valid.poke(false.B)
      val firstInstrId = acceptDcache(dut)._1
      returnDcache(dut, firstInstrId, BigInt("ffffffff", 16), 0x800)
      dut.io.cachelineClientRsp.valid.expect(true.B)
      dut.clock.step()

      driveClientRequest(dut, clientId = 3, lineBase = 0x5000,
        mask = BigInt("ffffffff", 16))
      dut.clock.step()
      dut.io.cachelineClientReq.valid.poke(false.B)
      val reusedInstrId = acceptDcache(dut)._1
      assert(reusedInstrId == firstInstrId)
      returnDcache(dut, reusedInstrId, 0, 0)
      dut.io.cachelineClientRsp.valid.expect(false.B)
      returnDcache(dut, reusedInstrId, BigInt("ffffffff", 16), 0x900)
      dut.io.cachelineClientRsp.valid.expect(true.B)
      dut.io.cachelineClientRsp.bits.lineBase.expect(0x5000.U)
      dut.clock.step()
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

  "PIPE preserves sparse lane data while one vector load is split across cache lines" in {
    test(new LSUexe).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      driveIdle(dut)
      dut.clock.step()

      // Lanes 1/3 share one 128-byte line; lanes 5/12 share the next.  This
      // exercises AddrCalculate's request split and MSHRv2's response merge.
      driveSparsePipeLoad(dut, Map(
        1 -> BigInt("90001000", 16),
        3 -> BigInt("9000101c", 16),
        5 -> BigInt("90001080", 16),
        12 -> BigInt("900010fc", 16)))
      dut.io.lsu_req.ready.expect(true.B)
      dut.clock.step()
      dut.io.lsu_req.valid.poke(false.B)

      val requests = Seq(acceptDcache(dut), acceptDcache(dut))
      val lowerLine = requests.find(_._2 == BigInt("0000000a", 16))
        .getOrElse(fail(s"missing lanes 1/3 DCache request: $requests"))
      val upperLine = requests.find(_._2 == BigInt("00001020", 16))
        .getOrElse(fail(s"missing lanes 5/12 DCache request: $requests"))

      // DCache data is lane-indexed at this boundary.  Returning the upper
      // line first proves the pending mask retains both partial responses.
      returnDcache(dut, upperLine._1, upperLine._2, 0x2000)
      dut.io.lsu_rsp.valid.expect(false.B)
      returnDcache(dut, lowerLine._1, lowerLine._2, 0x1000)
      dut.io.lsu_rsp.valid.expect(true.B)
      dut.io.lsu_rsp.bits.tag.clientId.expect(LsuClientId.Pipe)
      Seq(1, 3, 5, 12).foreach(lane => dut.io.lsu_rsp.bits.tag.mask(lane).expect(true.B))
      dut.io.lsu_rsp.bits.data(1).expect(0x1001.U)
      dut.io.lsu_rsp.bits.data(3).expect(0x1003.U)
      dut.io.lsu_rsp.bits.data(5).expect(0x2005.U)
      dut.io.lsu_rsp.bits.data(12).expect(0x200c.U)
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

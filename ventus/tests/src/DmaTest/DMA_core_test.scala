/*
 * DMA_core Unit Test - Plan A: Module-level verification
 * Tests the DMA_core module in isolation by mocking L2 cache and shared memory interfaces.
 */
package DmaTest

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import pipeline._
import L1Cache.MyConfig
import config.config.Parameters
import top.parameters._

class DMA_core_test extends AnyFreeSpec with ChiselScalatestTester {

  implicit val p: Parameters = (new MyConfig).toInstance

  private val L2CachelineBytes = dcache_BlockWords * BytesOfWord
  private val AddrAlignMask = ~(L2CachelineBytes - 1)

  case class DmaCmd(
      name: String,
      src: Int,
      dst: Int,
      wid: Int,
      funct: Int,
      in2: Int,
      ctrlCopysize: Int = 0
  ) {
    def expectedCopyBytes: Int = if (funct == 0) (4 << ctrlCopysize) else in2
    def expectedCachelines: Seq[Int] = {
      val start = src & AddrAlignMask
      val endExclusive = src + expectedCopyBytes
      val last = (endExclusive - 1) & AddrAlignMask
      (start to last by L2CachelineBytes).toSeq
    }
  }

  case class SharedRspPlan(instrId: BigInt, activeMask: Seq[Boolean])

  case class SharedReqObs(instrId: BigInt, activeMask: Seq[Boolean], laneData: Seq[BigInt])

  // Helper: poke all CtrlSigs fields to zero/false, then set specific ones
  def pokeCtrlSigsZero(ctrl: CtrlSigs): Unit = {
    ctrl.inst.poke(0.U)
    ctrl.wid.poke(0.U)
    ctrl.fp.poke(false.B)
    ctrl.branch.poke(0.U)
    ctrl.simt_stack.poke(false.B)
    ctrl.simt_stack_op.poke(false.B)
    ctrl.barrier.poke(false.B)
    ctrl.csr.poke(0.U)
    ctrl.reverse.poke(false.B)
    ctrl.sel_alu2.poke(0.U)
    ctrl.sel_alu1.poke(0.U)
    ctrl.isvec.poke(false.B)
    ctrl.sel_alu3.poke(0.U)
    ctrl.mask.poke(false.B)
    ctrl.sel_imm.poke(0.U)
    ctrl.mem_whb.poke(0.U)
    ctrl.mem_unsigned.poke(false.B)
    ctrl.alu_fn.poke(0.U)
    ctrl.force_rm_rtz.poke(false.B)
    ctrl.is_vls12.poke(false.B)
    ctrl.mem.poke(false.B)
    ctrl.mul.poke(false.B)
    ctrl.tc.poke(false.B)
    ctrl.disable_mask.poke(false.B)
    ctrl.custom_signal_0.poke(false.B)
    ctrl.mem_cmd.poke(0.U)
    ctrl.mop.poke(0.U)
    ctrl.reg_idx1.poke(0.U)
    ctrl.reg_idx2.poke(0.U)
    ctrl.reg_idx3.poke(0.U)
    ctrl.reg_idxw.poke(0.U)
    ctrl.wvd.poke(false.B)
    ctrl.fence.poke(false.B)
    ctrl.sfu.poke(false.B)
    ctrl.readmask.poke(false.B)
    ctrl.writemask.poke(false.B)
    ctrl.wxd.poke(false.B)
    ctrl.pc.poke(0.U)
    ctrl.imm_ext.poke(0.U)
    ctrl.atomic.poke(false.B)
    ctrl.aq.poke(false.B)
    ctrl.rl.poke(false.B)
    ctrl.dma.poke(false.B)
    ctrl.funct.poke(0.U)
    ctrl.copysize.poke(0.U)
  }

  def pokeCtrlSpikeInfo(ctrl: CtrlSigs): Unit = {
    ctrl.spike_info.foreach { info =>
      info.sm_id.poke(0.U)
      info.pc.poke(0.U)
      info.inst.poke(0.U)
      info.dispatch_id.foreach(_.poke(0.U))
      info.is_extended.foreach(_.poke(false.B))
    }
  }

  def initDut(dut: DMA_core): Unit = {
    dut.io.dma_req.valid.poke(false.B)
    dut.io.dma_cache_rsp.valid.poke(false.B)
    dut.io.shared_rsp.valid.poke(false.B)
    dut.io.dma_cache_req.ready.poke(true.B)
    dut.io.shared_req.ready.poke(false.B)
    dut.io.fence_end_dma.ready.poke(true.B)
    // TLB ports: not ready/valid by default
    dut.io.to_l2TLB.ready.poke(false.B)
    dut.io.from_l2TLB.valid.poke(false.B)
    dut.io.from_l2TLB.bits.paddr.poke(0.U)
    dut.clock.step(2)
  }

  def sampleFence(dut: DMA_core, fences: scala.collection.mutable.ArrayBuffer[BigInt]): Unit = {
    if (dut.io.fence_end_dma.valid.peekBoolean()) {
      fences += dut.io.fence_end_dma.bits.peekInt()
    }
  }

  def stepAndSample(
      dut: DMA_core,
      fences: scala.collection.mutable.ArrayBuffer[BigInt],
      cycles: Int = 1
  ): Unit = {
    for (_ <- 0 until cycles) {
      dut.clock.step()
      sampleFence(dut, fences)
    }
  }

  /** TLB mock state: tracks pending TLB request for multi-cycle response */
  class TlbMockState(
      val translate: BigInt => BigInt = identity,
      val delay: Int = 0,
      val intermittentReady: Boolean = false
  ) {
    var pendingVaddr: Option[BigInt] = None
    var delayCnt: Int = 0
    var readyCycle: Int = 0
  }

  /** Drive TLB mock for one cycle. Call this every cycle in the service loop. */
  def mockTlbCycle(dut: DMA_core, fences: scala.collection.mutable.ArrayBuffer[BigInt], tlb: TlbMockState): Unit = {
    // Handle TLB request acceptance
    if (tlb.pendingVaddr.isEmpty && dut.io.to_l2TLB.valid.peekBoolean()) {
      val shouldAccept = if (tlb.intermittentReady) {
        tlb.readyCycle += 1
        (tlb.readyCycle % 3) != 0  // reject every 3rd cycle
      } else true
      if (shouldAccept) {
        val vaddr = dut.io.to_l2TLB.bits.vaddr.peekInt()
        dut.io.to_l2TLB.ready.poke(true.B)
        dut.clock.step()
        sampleFence(dut, fences)
        dut.io.to_l2TLB.ready.poke(false.B)
        tlb.pendingVaddr = Some(vaddr)
        tlb.delayCnt = 0
      }
    }
    // Handle TLB response delivery (after delay)
    tlb.pendingVaddr.foreach { vaddr =>
      if (tlb.delayCnt >= tlb.delay) {
        val paddr = tlb.translate(vaddr)
        dut.io.from_l2TLB.valid.poke(true.B)
        dut.io.from_l2TLB.bits.paddr.poke(paddr.U)
        if (dut.io.from_l2TLB.ready.peekBoolean()) {
          dut.clock.step()
          sampleFence(dut, fences)
          dut.io.from_l2TLB.valid.poke(false.B)
          tlb.pendingVaddr = None
        }
      } else {
        tlb.delayCnt += 1
      }
    }
  }

  def driveCmd(
      dut: DMA_core,
      cmd: DmaCmd,
      fences: scala.collection.mutable.ArrayBuffer[BigInt] = scala.collection.mutable.ArrayBuffer.empty[BigInt]
  ): Unit = {
    dut.io.dma_req.valid.poke(true.B)
    for (i <- 0 until num_thread) {
      dut.io.dma_req.bits.in1(i).poke(if (i == 0) cmd.src.U else 0.U)
      dut.io.dma_req.bits.in2(i).poke(if (i == 0) cmd.in2.U else 0.U)
      dut.io.dma_req.bits.in3(i).poke(if (i == 0) cmd.dst.U else 0.U)
      dut.io.dma_req.bits.mask(i).poke(true.B)
    }
    pokeCtrlSigsZero(dut.io.dma_req.bits.ctrl)
    pokeCtrlSpikeInfo(dut.io.dma_req.bits.ctrl)
    dut.io.dma_req.bits.ctrl.dma.poke(true.B)
    dut.io.dma_req.bits.ctrl.funct.poke(cmd.funct.U)
    dut.io.dma_req.bits.ctrl.copysize.poke(cmd.ctrlCopysize.U)
    dut.io.dma_req.bits.ctrl.wid.poke(cmd.wid.U)

    var cycles = 0
    while (!dut.io.dma_req.ready.peekBoolean() && cycles < 50) {
      stepAndSample(dut, fences)
      cycles += 1
    }
    assert(dut.io.dma_req.ready.peekBoolean(), s"${cmd.name}: dma_req should be ready")
    stepAndSample(dut, fences)
    dut.io.dma_req.valid.poke(false.B)
  }

  def collectSharedReq(dut: DMA_core, tag: String): Option[SharedReqObs] = {
    if (!dut.io.shared_req.valid.peekBoolean()) return None
    val instrId = dut.io.shared_req.bits.instrId.peekInt()
    val activeMask = (0 until num_thread).map { i =>
      dut.io.shared_req.bits.perLaneAddr(i).activeMask.peekBoolean()
    }
    val laneData = (0 until num_thread).map { i =>
      dut.io.shared_req.bits.data(i).peekInt()
    }
    val activeCount = activeMask.count(identity)
    val isWrite = dut.io.shared_req.bits.isWrite.peekBoolean()
    assert(isWrite, s"$tag: shared_req should be write")
    assert(activeCount > 0, s"$tag: shared_req should have active lanes")
    Some(SharedReqObs(instrId, activeMask, laneData))
  }

  def sendSharedRsp(
      dut: DMA_core,
      plan: SharedReqObs,
      fences: scala.collection.mutable.ArrayBuffer[BigInt]
  ): Unit = {
    dut.io.shared_req.ready.poke(true.B)
    stepAndSample(dut, fences)
    dut.io.shared_req.ready.poke(false.B)

    dut.io.shared_rsp.valid.poke(true.B)
    dut.io.shared_rsp.bits.instrId.poke(plan.instrId.U)
    for (i <- 0 until num_thread) {
      dut.io.shared_rsp.bits.activeMask(i).poke(plan.activeMask(i).B)
      dut.io.shared_rsp.bits.data(i).poke(0.U)
    }
    var cycles = 0
    while (!dut.io.shared_rsp.ready.peekBoolean() && cycles < 30) {
      stepAndSample(dut, fences)
      cycles += 1
    }
    assert(dut.io.shared_rsp.ready.peekBoolean(), "shared_rsp should be ready")
    stepAndSample(dut, fences)
    dut.io.shared_rsp.valid.poke(false.B)
  }

  def serviceUntilDone(
      dut: DMA_core,
      pendingCmds: Seq[DmaCmd],
      injectSecondCmdAfterFirstReq: Boolean = false,
      tlb: TlbMockState = new TlbMockState()
  ): (Seq[(BigInt, BigInt)], Seq[BigInt], Int, Seq[SharedReqObs]) = {
    val expectedReqs = pendingCmds.flatMap(_.expectedCachelines)
    val expectedFences = pendingCmds.map(_.wid.toLong)
    val reqs = scala.collection.mutable.ArrayBuffer.empty[(BigInt, BigInt)]
    val fences = scala.collection.mutable.ArrayBuffer.empty[BigInt]
    var sharedReqCount = 0
    val sharedReqObs = scala.collection.mutable.ArrayBuffer.empty[SharedReqObs]
    var injectedSecond = false
    var cycles = 0

    while ((reqs.length < expectedReqs.length || fences.length < expectedFences.length) && cycles < 800) {
      var progressed = false

      // Service TLB mock each cycle
      mockTlbCycle(dut, fences, tlb)

      if (dut.io.dma_cache_req.valid.peekBoolean()) {
        val source = dut.io.dma_cache_req.bits.a_source.peekInt()
        val addr = dut.io.dma_cache_req.bits.a_addr.map(_.peekInt()).getOrElse(BigInt(0))
        reqs += ((source, addr))

        // Handshake the request once, then return one response.
        stepAndSample(dut, fences)
        dut.io.dma_cache_rsp.valid.poke(true.B)
        dut.io.dma_cache_rsp.bits.d_opcode.poke(1.U)
        dut.io.dma_cache_rsp.bits.d_source.poke(source.U)
        dut.io.dma_cache_rsp.bits.d_addr.poke(0.U)
        for (w <- 0 until dcache_BlockWords) {
          dut.io.dma_cache_rsp.bits.d_data(w).poke((0xA0000000L + reqs.length * 0x100 + w * 4).U)
        }
        var rspCycles = 0
        while (!dut.io.dma_cache_rsp.ready.peekBoolean() && rspCycles < 30) {
          stepAndSample(dut, fences)
          rspCycles += 1
        }
        assert(dut.io.dma_cache_rsp.ready.peekBoolean(), "dma_cache_rsp should be ready")
        stepAndSample(dut, fences)
        dut.io.dma_cache_rsp.valid.poke(false.B)

        if (injectSecondCmdAfterFirstReq && !injectedSecond && pendingCmds.length > 1) {
          driveCmd(dut, pendingCmds(1), fences)
          injectedSecond = true
        }
        progressed = true
      } else {
        var keepDrainingShared = true
        while (keepDrainingShared) {
          collectSharedReq(dut, "serviceUntilDone") match {
            case Some(plan) =>
              sharedReqCount += 1
              sharedReqObs += plan
              sendSharedRsp(dut, plan, fences)
              progressed = true
            case None =>
              keepDrainingShared = false
          }
        }
      }

      if (!progressed) {
        stepAndSample(dut, fences)
      }
      cycles += 1
    }

    assert(reqs.length == expectedReqs.length, s"Expected ${expectedReqs.length} L2 requests, got ${reqs.length}")
    assert(fences.length == expectedFences.length, s"Expected ${expectedFences.length} fences, got ${fences.length}")
    (reqs.toSeq, fences.toSeq, sharedReqCount, sharedReqObs.toSeq)
  }

  "S1_single_bulk_copy" in {
    test(new DMA_core).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      initDut(dut)
      val cmd = DmaCmd("S1", src = 0x1000, dst = 0x0, wid = 0, funct = 1, in2 = L2CachelineBytes)
      driveCmd(dut, cmd)
      val (reqs, fences, sharedReqs, _) = serviceUntilDone(dut, Seq(cmd))

      assert(reqs.map(_._2) == Seq(BigInt(0x1000)))
      assert(reqs.head._1 == 0, s"S1 first source should be 0, got ${reqs.head._1}")
      assert(fences == Seq(BigInt(0)))
      assert(sharedReqs > 0, "S1 should issue shared writes")
    }
  }

  "S2_cross_cacheline_bulk_copy" in {
    test(new DMA_core).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      initDut(dut)
      val cmd = DmaCmd("S2", src = 0x1078, dst = 0x100, wid = 1, funct = 1, in2 = 32)
      driveCmd(dut, cmd)
      val (reqs, _, sharedReqs, _) = serviceUntilDone(dut, Seq(cmd), injectSecondCmdAfterFirstReq = false)

      // Current design should at least generate the two expected cacheline reads.
      assert(reqs.take(2).map(_._2) == Seq(BigInt(0x1000), BigInt(0x1080)),
        s"S2 L2 addrs mismatch: ${reqs.map(_._2.toString(16))}")
      assert(sharedReqs >= 2, s"S2 should issue at least 2 shared writes, got $sharedReqs")
    }
  }

  "S3_copysize_min_transfer" in {
    test(new DMA_core).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      initDut(dut)
      val cmd = DmaCmd("S3", src = 0x2004, dst = 0x40, wid = 2, funct = 0, in2 = 0, ctrlCopysize = 0)
      driveCmd(dut, cmd)
      val (reqs, fences, sharedReqs, _) = serviceUntilDone(dut, Seq(cmd))

      assert(reqs.map(_._2) == Seq(BigInt(0x2000)))
      assert(fences == Seq(BigInt(2)))
      assert(sharedReqs >= 1, s"S3 should issue at least 1 shared write, got $sharedReqs")
    }
  }

  "S4_multi_dma_inflight" in {
    test(new DMA_core).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      initDut(dut)
      val cmd0 = DmaCmd("S4_cmd0", src = 0x3000, dst = 0x80, wid = 3, funct = 1, in2 = L2CachelineBytes)
      val cmd1 = DmaCmd("S4_cmd1", src = 0x4008, dst = 0x180, wid = 4, funct = 1, in2 = 32)

      driveCmd(dut, cmd0)
      val (reqs, fences, sharedReqs, _) = serviceUntilDone(
        dut,
        Seq(cmd0, cmd1.copy(wid = 5)),
        injectSecondCmdAfterFirstReq = true
      )

      val reqAddrs = reqs.map(_._2)
      val reqSources = reqs.map(_._1)
      assert(reqAddrs.take(2) == Seq(BigInt(0x3000), BigInt(0x4000)),
        s"S4 L2 addrs mismatch: ${reqAddrs.map(_.toString(16))}")
      assert(reqSources.take(2).distinct.size == 2,
        s"S4 requests should use different a_source values, got ${reqSources.mkString(",")}")
      assert(fences.nonEmpty, "S4 should complete at least one DMA command")
      assert(sharedReqs >= 2, s"S4 should issue shared writes for both cmds, got $sharedReqs")
    }
  }

  "I1_copysize_4B_exact_payload" in {
    test(new DMA_core).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      initDut(dut)
      val cmd = DmaCmd("I1", src = 0x2004, dst = 0x40, wid = 6, funct = 0, in2 = 0, ctrlCopysize = 0)
      driveCmd(dut, cmd)
      val (reqs, fences, _, sharedObs) = serviceUntilDone(dut, Seq(cmd))

      assert(reqs.map(_._2) == Seq(BigInt(0x2000)),
        s"I1 L2 addr mismatch: ${reqs.map(_._2.toString(16))}")
      assert(fences == Seq(BigInt(6)), s"I1 fence mismatch: $fences")
      assert(sharedObs.nonEmpty, "I1 should have one shared write request")

      val obs = sharedObs.head
      val activeLanes = obs.activeMask.zipWithIndex.collect { case (true, idx) => idx }
      assert(activeLanes == Seq(0), s"I1 active lanes should be lane0 only, got $activeLanes")
      assert(obs.laneData(0) == BigInt("a0000104", 16),
        f"I1 lane0 payload mismatch: got 0x${obs.laneData(0)}%x expected 0xa0000104")
    }
  }

  "I4_bulk_32B_cross_cacheline_exact_payload" in {
    test(new DMA_core).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      initDut(dut)
      val cmd = DmaCmd("I4", src = 0x1078, dst = 0x100, wid = 7, funct = 1, in2 = 32)
      driveCmd(dut, cmd)
      val (reqs, fences, _, sharedObs) = serviceUntilDone(dut, Seq(cmd))

      assert(reqs.map(_._2) == Seq(BigInt(0x1000), BigInt(0x1080)),
        s"I4 L2 addrs mismatch: ${reqs.map(_._2.toString(16))}")
      assert(fences == Seq(BigInt(7)), s"I4 fence mismatch: $fences")

      val activeCountsSorted = sharedObs.map(_.activeMask.count(identity)).sorted
      assert(activeCountsSorted == Seq(2, 6),
        s"I4 shared request active counts mismatch: $activeCountsSorted")

      val observedActiveData = sharedObs.flatMap { obs =>
        obs.activeMask.zipWithIndex.collect { case (true, lane) => obs.laneData(lane) }
      }.sorted

      val expectedActiveData = Seq(
        BigInt("a0000178", 16), BigInt("a000017c", 16),
        BigInt("a0000200", 16), BigInt("a0000204", 16), BigInt("a0000208", 16), BigInt("a000020c", 16),
        BigInt("a0000210", 16), BigInt("a0000214", 16)
      ).sorted

      assert(observedActiveData == expectedActiveData,
        s"I4 active payload mismatch: got=${observedActiveData.map(_.toString(16))}, expected=${expectedActiveData.map(_.toString(16))}")
    }
  }

  "I3_bulk_128B_aligned_exact_payload" in {
    test(new DMA_core).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      initDut(dut)
      val cmd = DmaCmd("I3", src = 0x1000, dst = 0x0, wid = 0, funct = 1, in2 = 128)
      driveCmd(dut, cmd)
      val (reqs, fences, _, sharedObs) = serviceUntilDone(dut, Seq(cmd))

      assert(reqs.map(_._2) == Seq(BigInt(0x1000)),
        s"I3 L2 addr mismatch: ${reqs.map(_._2.toString(16))}")
      assert(fences == Seq(BigInt(0)), s"I3 fence mismatch: $fences")
      assert(sharedObs.size == 1, s"I3 should have exactly one shared request, got ${sharedObs.size}")

      val obs = sharedObs.head
      val activeLanes = obs.activeMask.zipWithIndex.collect { case (true, idx) => idx }
      assert(activeLanes == (0 until 32), s"I3 active lanes mismatch: $activeLanes")

      val expectedData = (0 until 32).map(i => BigInt("a0000100", 16) + BigInt(i * 4))
      val observedData = activeLanes.map(obs.laneData)
      assert(observedData == expectedData,
        s"I3 payload mismatch: got=${observedData.map(_.toString(16))}, expected=${expectedData.map(_.toString(16))}")
    }
  }

  "I5_bulk_192B_multi_cacheline_exact_payload" in {
    test(new DMA_core).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      initDut(dut)
      val cmd = DmaCmd("I5", src = 0x1000, dst = 0x80, wid = 1, funct = 1, in2 = 192)
      driveCmd(dut, cmd)
      val (reqs, fences, _, sharedObs) = serviceUntilDone(dut, Seq(cmd))

      assert(reqs.map(_._2) == Seq(BigInt(0x1000), BigInt(0x1080)),
        s"I5 L2 addrs mismatch: ${reqs.map(_._2.toString(16))}")
      assert(fences == Seq(BigInt(1)), s"I5 fence mismatch: $fences")

      val activeCountsSorted = sharedObs.map(_.activeMask.count(identity)).sorted
      assert(activeCountsSorted == Seq(16, 32),
        s"I5 shared request active counts mismatch: $activeCountsSorted")

      val observedActiveData = sharedObs.flatMap { obs =>
        obs.activeMask.zipWithIndex.collect { case (true, lane) => obs.laneData(lane) }
      }.sorted

      val expectedFirstLine = (0 until 32).map(i => BigInt("a0000100", 16) + BigInt(i * 4))
      val expectedSecondLine = (0 until 16).map(i => BigInt("a0000200", 16) + BigInt(i * 4))
      val expectedActiveData = (expectedFirstLine ++ expectedSecondLine).sorted

      assert(observedActiveData == expectedActiveData,
        s"I5 active payload mismatch: got=${observedActiveData.map(_.toString(16))}, expected=${expectedActiveData.map(_.toString(16))}")
    }
  }

  "I6_bulk_64B_dst_offset_exact_payload" in {
    test(new DMA_core).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      initDut(dut)
      val cmd = DmaCmd("I6", src = 0x1040, dst = 0x1c0, wid = 2, funct = 1, in2 = 64)
      driveCmd(dut, cmd)
      val (reqs, fences, _, sharedObs) = serviceUntilDone(dut, Seq(cmd))

      assert(reqs.map(_._2) == Seq(BigInt(0x1000)),
        s"I6 L2 addr mismatch: ${reqs.map(_._2.toString(16))}")
      assert(fences == Seq(BigInt(2)), s"I6 fence mismatch: $fences")
      assert(sharedObs.size == 1, s"I6 should have exactly one shared request, got ${sharedObs.size}")

      val obs = sharedObs.head
      val activeLanes = obs.activeMask.zipWithIndex.collect { case (true, idx) => idx }
      assert(activeLanes == (0 until 16), s"I6 active lanes mismatch: $activeLanes")

      val expectedData = (0 until 16).map(i => BigInt("a0000140", 16) + BigInt(i * 4))
      val observedData = activeLanes.map(obs.laneData)
      assert(observedData == expectedData,
        s"I6 payload mismatch: got=${observedData.map(_.toString(16))}, expected=${expectedData.map(_.toString(16))}")
    }
  }

  // ============================================================
  // New tests from consensus review
  // ============================================================

  /**
   * Service DMA with configurable L2 response delay.
   * Returns the same tuple as serviceUntilDone.
   */
  def serviceWithL2Delay(
      dut: DMA_core,
      pendingCmds: Seq[DmaCmd],
      l2DelayCycles: Int,
      tlb: TlbMockState = new TlbMockState()
  ): (Seq[(BigInt, BigInt)], Seq[BigInt], Int, Seq[SharedReqObs]) = {
    val expectedReqs = pendingCmds.flatMap(_.expectedCachelines)
    val expectedFences = pendingCmds.map(_.wid.toLong)
    val reqs = scala.collection.mutable.ArrayBuffer.empty[(BigInt, BigInt)]
    val fences = scala.collection.mutable.ArrayBuffer.empty[BigInt]
    var sharedReqCount = 0
    val sharedReqObs = scala.collection.mutable.ArrayBuffer.empty[SharedReqObs]
    var cycles = 0

    while ((reqs.length < expectedReqs.length || fences.length < expectedFences.length) && cycles < 1200) {
      var progressed = false

      // Service TLB mock each cycle
      mockTlbCycle(dut, fences, tlb)

      if (dut.io.dma_cache_req.valid.peekBoolean()) {
        val source = dut.io.dma_cache_req.bits.a_source.peekInt()
        val addr = dut.io.dma_cache_req.bits.a_addr.map(_.peekInt()).getOrElse(BigInt(0))
        reqs += ((source, addr))

        // Handshake the request, then stall new L2 requests until response done
        stepAndSample(dut, fences)
        dut.io.dma_cache_req.ready.poke(false.B)

        // Insert configurable delay before L2 response
        for (_ <- 0 until l2DelayCycles) {
          stepAndSample(dut, fences)
        }

        dut.io.dma_cache_rsp.valid.poke(true.B)
        dut.io.dma_cache_rsp.bits.d_opcode.poke(1.U)
        dut.io.dma_cache_rsp.bits.d_source.poke(source.U)
        dut.io.dma_cache_rsp.bits.d_addr.poke(0.U)
        for (w <- 0 until dcache_BlockWords) {
          dut.io.dma_cache_rsp.bits.d_data(w).poke((0xA0000000L + reqs.length * 0x100 + w * 4).U)
        }
        var rspCycles = 0
        while (!dut.io.dma_cache_rsp.ready.peekBoolean() && rspCycles < 60) {
          stepAndSample(dut, fences)
          rspCycles += 1
        }
        assert(dut.io.dma_cache_rsp.ready.peekBoolean(), "dma_cache_rsp should be ready")
        stepAndSample(dut, fences)
        dut.io.dma_cache_rsp.valid.poke(false.B)
        dut.io.dma_cache_req.ready.poke(true.B)
        sampleFence(dut, fences)
        progressed = true
      } else {
        var keepDrainingShared = true
        while (keepDrainingShared) {
          collectSharedReq(dut, "serviceWithL2Delay") match {
            case Some(plan) =>
              sharedReqCount += 1
              sharedReqObs += plan
              sendSharedRsp(dut, plan, fences)
              progressed = true
            case None =>
              keepDrainingShared = false
          }
        }
      }

      sampleFence(dut, fences)
      if (!progressed) {
        stepAndSample(dut, fences)
      }
      cycles += 1
    }

    assert(reqs.length == expectedReqs.length, s"Expected ${expectedReqs.length} L2 requests, got ${reqs.length}")
    assert(fences.length == expectedFences.length, s"Expected ${expectedFences.length} fences, got ${fences.length}")
    (reqs.toSeq, fences.toSeq, sharedReqCount, sharedReqObs.toSeq)
  }

  "T1_L2_delayed_response_bulk_128B" in {
    test(new DMA_core).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      initDut(dut)
      val cmd = DmaCmd("T1", src = 0x1000, dst = 0x0, wid = 0, funct = 1, in2 = 128)
      driveCmd(dut, cmd)
      val (reqs, fences, _, sharedObs) = serviceWithL2Delay(dut, Seq(cmd), l2DelayCycles = 10)

      assert(reqs.map(_._2) == Seq(BigInt(0x1000)), s"T1 L2 addr mismatch")
      assert(fences == Seq(BigInt(0)), s"T1 fence mismatch: $fences")
      assert(sharedObs.size == 1, s"T1 should have exactly one shared request")

      // Verify payload matches I3 (same command, just with delay)
      val obs = sharedObs.head
      val activeLanes = obs.activeMask.zipWithIndex.collect { case (true, idx) => idx }
      assert(activeLanes == (0 until 32), s"T1 active lanes mismatch")

      val expectedData = (0 until 32).map(i => BigInt("a0000100", 16) + BigInt(i * 4))
      val observedData = activeLanes.map(obs.laneData)
      assert(observedData == expectedData,
        s"T1 payload mismatch with L2 delay: got=${observedData.map(_.toString(16))}")
    }
  }

  "T1b_L2_delayed_response_cross_cacheline" in {
    test(new DMA_core).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      initDut(dut)
      val cmd = DmaCmd("T1b", src = 0x1078, dst = 0x100, wid = 3, funct = 1, in2 = 32)
      driveCmd(dut, cmd)
      val (reqs, fences, _, sharedObs) = serviceWithL2Delay(dut, Seq(cmd), l2DelayCycles = 5)

      assert(reqs.map(_._2) == Seq(BigInt(0x1000), BigInt(0x1080)),
        s"T1b L2 addrs mismatch")
      assert(fences == Seq(BigInt(3)), s"T1b fence mismatch: $fences")

      val activeCountsSorted = sharedObs.map(_.activeMask.count(identity)).sorted
      assert(activeCountsSorted == Seq(2, 6),
        s"T1b shared request active counts mismatch: $activeCountsSorted")
    }
  }

  // ============================================================
  // TLB integration tests
  // ============================================================

  "T_TLB_1_basic_tlb_path" in {
    test(new DMA_core).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      initDut(dut)
      val cmd = DmaCmd("T-TLB-1", src = 0x1000, dst = 0x0, wid = 0, funct = 1, in2 = 128)
      driveCmd(dut, cmd)
      // Identity mapping TLB mock — verify paddr reaches L2
      val tlb = new TlbMockState()
      val (reqs, fences, _, sharedObs) = serviceUntilDone(dut, Seq(cmd), tlb = tlb)

      // With identity mapping, L2 request addr should equal original aligned vaddr
      assert(reqs.map(_._2) == Seq(BigInt(0x1000)),
        s"T-TLB-1 L2 addr mismatch: ${reqs.map(_._2.toString(16))}")
      assert(fences == Seq(BigInt(0)), s"T-TLB-1 fence mismatch: $fences")
      assert(sharedObs.size == 1, s"T-TLB-1 should have exactly one shared request")

      // Verify payload is correct (same as I3)
      val obs = sharedObs.head
      val activeLanes = obs.activeMask.zipWithIndex.collect { case (true, idx) => idx }
      assert(activeLanes == (0 until 32), s"T-TLB-1 active lanes mismatch")
      val expectedData = (0 until 32).map(i => BigInt("a0000100", 16) + BigInt(i * 4))
      val observedData = activeLanes.map(obs.laneData)
      assert(observedData == expectedData,
        s"T-TLB-1 payload mismatch: got=${observedData.map(_.toString(16))}")
    }
  }

  "T_TLB_2_cross_page_dma" in {
    test(new DMA_core).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      initDut(dut)
      // src=0x0FF0 with 128 bytes crosses a 4KB page boundary:
      // cacheline 0: vaddr 0x0F80 (page 0) → paddr 0x20000F80
      // cacheline 1: vaddr 0x1000 (page 1) → paddr 0x30001000
      val cmd = DmaCmd("T-TLB-2", src = 0x0FF0, dst = 0x0, wid = 0, funct = 1, in2 = 128)
      driveCmd(dut, cmd)

      val pageMap = Map(
        BigInt(0) -> BigInt(0x20000000L),   // page 0 VPN→PPN offset
        BigInt(1) -> BigInt(0x30000000L)    // page 1 VPN→PPN offset
      )
      def crossPageTranslate(vaddr: BigInt): BigInt = {
        val pageNum = vaddr / 4096
        val offset = vaddr % 4096
        val physBase = pageMap.getOrElse(pageNum, pageNum * 4096)
        physBase + offset
      }
      val tlb = new TlbMockState(translate = crossPageTranslate)
      val (reqs, fences, _, _) = serviceUntilDone(dut, Seq(cmd), tlb = tlb)

      // Verify L2 requests use translated physical addresses
      // Cacheline 0: vaddr 0x0F80 → page 0 offset 0xF80 → paddr 0x20000F80
      // Cacheline 1: vaddr 0x1000 → page 1 offset 0x000 → paddr 0x30000000
      assert(reqs.length == 2, s"T-TLB-2 should have 2 L2 requests, got ${reqs.length}")
      assert(reqs(0)._2 == BigInt(0x20000F80L),
        s"T-TLB-2 first L2 addr mismatch: got 0x${reqs(0)._2.toString(16)}, expected 0x20000f80")
      assert(reqs(1)._2 == BigInt(0x30000000L),
        s"T-TLB-2 second L2 addr mismatch: got 0x${reqs(1)._2.toString(16)}, expected 0x30000000")
      assert(fences == Seq(BigInt(0)), s"T-TLB-2 fence mismatch: $fences")
    }
  }

  "T_TLB_3_high_latency_tlb" in {
    test(new DMA_core).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      initDut(dut)
      val cmd = DmaCmd("T-TLB-3", src = 0x1000, dst = 0x0, wid = 0, funct = 1, in2 = 128)
      driveCmd(dut, cmd)
      // TLB with 50-cycle delay per translation
      val tlb = new TlbMockState(delay = 50)
      val (reqs, fences, _, sharedObs) = serviceUntilDone(dut, Seq(cmd), tlb = tlb)

      assert(reqs.map(_._2) == Seq(BigInt(0x1000)),
        s"T-TLB-3 L2 addr mismatch: ${reqs.map(_._2.toString(16))}")
      assert(fences == Seq(BigInt(0)), s"T-TLB-3 fence mismatch: $fences")
      assert(sharedObs.nonEmpty, "T-TLB-3 should have shared requests")
    }
  }

  "T_TLB_4_backpressure" in {
    test(new DMA_core).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      initDut(dut)
      val cmd = DmaCmd("T-TLB-4", src = 0x1000, dst = 0x0, wid = 0, funct = 1, in2 = 256)
      driveCmd(dut, cmd)
      // TLB with intermittent backpressure (ready drops every 3rd cycle)
      val tlb = new TlbMockState(intermittentReady = true)
      val (reqs, fences, _, sharedObs) = serviceUntilDone(dut, Seq(cmd), tlb = tlb)

      // 256B = 2 cachelines
      assert(reqs.length == 2, s"T-TLB-4 should have 2 L2 requests, got ${reqs.length}")
      assert(reqs.map(_._2) == Seq(BigInt(0x1000), BigInt(0x1080)),
        s"T-TLB-4 L2 addrs mismatch: ${reqs.map(_._2.toString(16))}")
      assert(fences == Seq(BigInt(0)), s"T-TLB-4 fence mismatch: $fences")
      assert(sharedObs.nonEmpty, "T-TLB-4 should have shared requests")
    }
  }

  // ============================================================
  // MMU + DMA combined E2E smoke tests
  //
  // ============================================================

  "T_MMU_DMA_E2E_1_multipage_bulk" in {
    test(new DMA_core).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      initDut(dut)
      // 512B BULK copy starting at 0x0F80 (16 bytes before page boundary) —
      // 覆盖 4 个 cacheline，跨 2 个 page。每 page 翻译到不相邻的 paddr 基址。
      // cacheline0: v=0x0F80 (page 0) → p=0x20000F80
      // cacheline1: v=0x1000 (page 1) → p=0x30000000
      // cacheline2: v=0x1080 (page 1) → p=0x30000080
      // cacheline3: v=0x1100 (page 1) → p=0x30000100
      val cmd = DmaCmd("T-MMU-E2E-1", src = 0x0F80, dst = 0x400, wid = 3, funct = 1, in2 = 4 * L2CachelineBytes)
      driveCmd(dut, cmd)

      val pageMap = Map(
        BigInt(0) -> BigInt(0x20000000L),
        BigInt(1) -> BigInt(0x30000000L),
      )
      def translate(vaddr: BigInt): BigInt = {
        val pageNum = vaddr / 4096
        val offset = vaddr % 4096
        val physBase = pageMap.getOrElse(pageNum, pageNum * 4096)
        physBase + offset
      }
      val tlb = new TlbMockState(translate = translate, delay = 2)
      val (reqs, fences, _, sharedObs) = serviceUntilDone(dut, Seq(cmd), tlb = tlb)

      assert(reqs.length == 4, s"T-MMU-E2E-1 should have 4 L2 requests (one per cacheline), got ${reqs.length}")
      val expectedPaddrs = Seq(
        BigInt(0x20000F80L),
        BigInt(0x30000000L),
        BigInt(0x30000080L),
        BigInt(0x30000100L),
      )
      assert(reqs.map(_._2) == expectedPaddrs,
        s"T-MMU-E2E-1 paddr sequence mismatch: got=${reqs.map(_._2.toString(16))}, expected=${expectedPaddrs.map(_.toString(16))}")
      // 同一条 BULK 指令下 inst_mem_index 保持不变，a_source 低位相同属正常；
      // 只要 L2 能按顺序响应，即视作单指令多 cacheline 的 MMU+DMA 路径通畅。
      assert(fences == Seq(BigInt(3)), s"T-MMU-E2E-1 fence mismatch: $fences")
      assert(sharedObs.nonEmpty, "T-MMU-E2E-1 should issue shared writes")
    }
  }

  "T_MMU_DMA_E2E_2_concurrent_cmds_distinct_pages" in {
    test(new DMA_core).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      initDut(dut)
      // 两条 BULK 命令落在完全不同的 page：cmd0 用 page 2，cmd1 用 page 5。
      // 期望每条命令自己的 cacheline 翻译独立完成，结果 paddr 不串。
      val cmd0 = DmaCmd("T-MMU-E2E-2-cmd0", src = 0x2000, dst = 0x0000, wid = 4, funct = 1, in2 = L2CachelineBytes)
      val cmd1 = DmaCmd("T-MMU-E2E-2-cmd1", src = 0x5040, dst = 0x0200, wid = 5, funct = 1, in2 = 32)

      val pageMap = Map(
        BigInt(2) -> BigInt(0x40000000L),  // cmd0: page 2 → phys base 0x4000_0000
        BigInt(5) -> BigInt(0x80000000L),  // cmd1: page 5 → phys base 0x8000_0000
      )
      def translate(vaddr: BigInt): BigInt = {
        val pageNum = vaddr / 4096
        val offset = vaddr % 4096
        val physBase = pageMap.getOrElse(pageNum, pageNum * 4096)
        physBase + offset
      }
      val tlb = new TlbMockState(translate = translate, delay = 1)

      driveCmd(dut, cmd0)
      val (reqs, fences, _, sharedObs) = serviceUntilDone(
        dut,
        Seq(cmd0, cmd1),
        injectSecondCmdAfterFirstReq = true,
        tlb = tlb,
      )

      // 第一条命令 cacheline-aligned vaddr=0x2000 → paddr=0x40000000
      // 第二条命令 src=0x5040，DMA 做 cacheline 对齐后 vaddr=0x5000 → paddr=0x80000000
      val paddrs = reqs.map(_._2).toSet
      assert(paddrs.contains(BigInt(0x40000000L)),
        s"T-MMU-E2E-2 cmd0 paddr missing. got=${reqs.map(_._2.toString(16))}")
      assert(paddrs.contains(BigInt(0x80000000L)),
        s"T-MMU-E2E-2 cmd1 paddr missing. got=${reqs.map(_._2.toString(16))}")
      assert(fences.toSet == Set(BigInt(4), BigInt(5)),
        s"T-MMU-E2E-2 fence set mismatch: got=$fences, expected={4,5}")
      assert(sharedObs.nonEmpty, "T-MMU-E2E-2 should issue shared writes for both cmds")
    }
  }
}
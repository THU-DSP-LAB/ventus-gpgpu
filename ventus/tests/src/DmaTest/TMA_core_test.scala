/*
 * TMA Phase 2A Unit Test - CP_ASYNC_TENSOR (funct=3) tensor copy verification
 * Tests tensor 1D-5D address iteration, OOB fill, and back-to-back B2 fix.
 */
package DmaTest

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import pipeline._
import L1Cache.MyConfig
import config.config.Parameters
import top.parameters._

class TMA_core_test extends AnyFreeSpec with ChiselScalatestTester {

  implicit val p: Parameters = (new MyConfig).toInstance

  private val L2CachelineBytes = dcache_BlockWords * BytesOfWord // 128

  // ---- Case classes ----
  case class SharedReqObs(
      instrId: BigInt,
      activeMask: Seq[Boolean],
      laneData: Seq[BigInt],
      blockOffsets: Seq[BigInt]
  )

  case class TmaCmd(
      name: String,
      wid: Int = 0,
      dataType: Int,          // DataType enum matching DMA_core.DataType / cp_async_tensor.h
      tensorRank: Int,        // 1~5
      globalAddress: Int,     // global memory base address
      globalDim: Seq[Int],    // length 5
      globalStrides: Seq[Int],// length 5, bytes
      boxAddress: Int,        // Box start in global memory
      boxDim: Seq[Int],       // length 5
      elementStrides: Seq[Int],// length 5
      oobfill: Int = 0,       // 0=zero, 1=NaN
      dst: Int                // Shared Memory destination
  )

  // ---- Reference model ----
  object TmaRefModel {
    def datawidthFromType(dataType: Int): Int = dataType match {
      case 0 | 3 => 1  // UINT8, INT8
      case 1 | 4 | 7 | 8 => 2  // UINT16, INT16, FLOAT16, BFLOAT16
      case 2 | 5 | 6 => 4  // UINT32, INT32, FLOAT32
      case 9 | 10 | 11 => 8  // UINT64, INT64, FLOAT64
      case _ => 4
    }

    /** Compute expected L2 cacheline-aligned addresses for a tensor copy */
    def computeExpectedL2Addrs(cmd: TmaCmd): Seq[Int] = {
      val dw = datawidthFromType(cmd.dataType)
      val boxElems = (0 until 5).map { x =>
        if (cmd.elementStrides(x) > 0)
          (cmd.boxDim(x) + cmd.elementStrides(x) - 1) / cmd.elementStrides(x)
        else 1
      }

      // Iterate through all dim0 rows
      val addrs = scala.collection.mutable.ArrayBuffer.empty[Int]

      def iterDims(rank: Int): Unit = {
        val counts = Array.fill(5)(0)
        var done = false
        while (!done) {
          // Compute tensor_dim0_start for this iteration
          var td0s = cmd.globalAddress
          for (k <- 1 until rank) {
            td0s += counts(k) * cmd.globalStrides(k - 1) * cmd.elementStrides(k)
          }
          // Compute box_dim0_start
          var bd0s = cmd.boxAddress
          for (k <- 1 until rank) {
            bd0s += counts(k) * cmd.globalStrides(k - 1) * cmd.elementStrides(k)
          }

          // For this dim0 row, determine which cachelines are needed
          val rowBytes = cmd.boxDim(0) * dw
          val startAddr = bd0s
          val endAddr = startAddr + rowBytes
          val firstCL = startAddr & ~(L2CachelineBytes - 1)
          val lastCL = (endAddr - 1) & ~(L2CachelineBytes - 1)
          for (cl <- firstCL to lastCL by L2CachelineBytes) {
            addrs += cl
          }

          // Advance multi-dimensional counter
          if (rank <= 1) {
            done = true
          } else {
            var carry = true
            var dim = 1
            while (carry && dim < rank) {
              counts(dim) += 1
              if (counts(dim) >= boxElems(dim)) {
                counts(dim) = 0
                dim += 1
              } else {
                carry = false
              }
            }
            if (carry) done = true
          }
        }
      }

      iterDims(cmd.tensorRank)
      addrs.toSeq
    }
  }

  // ---- Helper functions (copied from DMA_core_test.scala) ----

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

  class TlbMockState(
      val translate: BigInt => BigInt = identity,
      val delay: Int = 0,
      val intermittentReady: Boolean = false
  ) {
    var pendingVaddr: Option[BigInt] = None
    var delayCnt: Int = 0
    var readyCycle: Int = 0
  }

  def mockTlbCycle(dut: DMA_core, fences: scala.collection.mutable.ArrayBuffer[BigInt], tlb: TlbMockState): Unit = {
    if (tlb.pendingVaddr.isEmpty && dut.io.to_l2TLB.valid.peekBoolean()) {
      val shouldAccept = if (tlb.intermittentReady) {
        tlb.readyCycle += 1
        (tlb.readyCycle % 3) != 0
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

  // ---- TMA-specific driver ----
  def driveTmaCmd(
      dut: DMA_core,
      cmd: TmaCmd,
      fences: scala.collection.mutable.ArrayBuffer[BigInt] = scala.collection.mutable.ArrayBuffer.empty[BigInt]
  ): Unit = {
    dut.io.dma_req.valid.poke(true.B)
    // in1: lane mapping for global tensor params
    for (i <- 0 until num_thread) {
      val v = i match {
        case 0  => cmd.dataType
        case 1  => cmd.tensorRank
        case 2  => cmd.globalAddress
        case x if x >= 3 && x <= 7  => cmd.globalDim(x - 3)
        case x if x >= 8 && x <= 12 => cmd.globalStrides(x - 8)
        case _ => 0
      }
      dut.io.dma_req.bits.in1(i).poke(v.U)
    }
    // in2: lane mapping for box params
    for (i <- 0 until num_thread) {
      val v = i match {
        case 0  => cmd.boxAddress
        case x if x >= 1 && x <= 5   => cmd.boxDim(x - 1)
        case x if x >= 6 && x <= 10  => cmd.elementStrides(x - 6)
        case 11 => 0  // interleaveMode
        case 12 => 0  // swizzleMode
        case 13 => 0  // L2promotion
        case 14 => cmd.oobfill
        case _  => 0
      }
      dut.io.dma_req.bits.in2(i).poke(v.U)
    }
    // in3: lane 0 = dst
    for (i <- 0 until num_thread) {
      dut.io.dma_req.bits.in3(i).poke(if (i == 0) cmd.dst.U else 0.U)
      dut.io.dma_req.bits.mask(i).poke(true.B)
    }
    pokeCtrlSigsZero(dut.io.dma_req.bits.ctrl)
    pokeCtrlSpikeInfo(dut.io.dma_req.bits.ctrl)
    dut.io.dma_req.bits.ctrl.dma.poke(true.B)
    dut.io.dma_req.bits.ctrl.funct.poke(3.U)
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
    val blockOffsets = (0 until num_thread).map { i =>
      dut.io.shared_req.bits.perLaneAddr(i).blockOffset.peekInt()
    }
    val laneData = (0 until num_thread).map { i =>
      dut.io.shared_req.bits.data(i).peekInt()
    }
    val activeCount = activeMask.count(identity)
    val isWrite = dut.io.shared_req.bits.isWrite.peekBoolean()
    assert(isWrite, s"$tag: shared_req should be write")
    assert(activeCount > 0, s"$tag: shared_req should have active lanes")
    Some(SharedReqObs(instrId, activeMask, laneData, blockOffsets))
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
    sampleFence(dut, fences)
  }

  /** Service loop: handle L2 requests/responses and shared memory until all expected
    * cachelines are fetched and fences are received. */
  def tmaServiceUntilDone(
      dut: DMA_core,
      expectedL2Count: Int,
      expectedFenceCount: Int,
      tlb: TlbMockState = new TlbMockState()
  ): (Seq[(BigInt, BigInt)], Seq[BigInt], Seq[SharedReqObs]) = {
    val reqs = scala.collection.mutable.ArrayBuffer.empty[(BigInt, BigInt)]
    val fences = scala.collection.mutable.ArrayBuffer.empty[BigInt]
    val sharedReqObs = scala.collection.mutable.ArrayBuffer.empty[SharedReqObs]
    var cycles = 0
    val maxCycles = 2000

    while ((reqs.length < expectedL2Count || fences.length < expectedFenceCount) && cycles < maxCycles) {
      var progressed = false

      mockTlbCycle(dut, fences, tlb)

      if (dut.io.dma_cache_req.valid.peekBoolean()) {
        val source = dut.io.dma_cache_req.bits.a_source.peekInt()
        val addr = dut.io.dma_cache_req.bits.a_addr.map(_.peekInt()).getOrElse(BigInt(0))
        reqs += ((source, addr))

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
        sampleFence(dut, fences)
        progressed = true
      } else {
        var keepDrainingShared = true
        while (keepDrainingShared) {
          collectSharedReq(dut, "tmaService") match {
            case Some(plan) =>
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

    assert(reqs.length == expectedL2Count,
      s"Expected $expectedL2Count L2 requests, got ${reqs.length} (cycles=$cycles)")
    assert(fences.length == expectedFenceCount,
      s"Expected $expectedFenceCount fences, got ${fences.length}")
    (reqs.toSeq, fences.toSeq, sharedReqObs.toSeq)
  }

  // =====================================================================
  // Test cases
  // =====================================================================

  "TMA_T1_2d_aligned" in {
    // 2D tensor: FP32, 16x16 box, boxDim(0)=16 elements, boxDim(1)=16 elements
    // globalStrides(0) = 64 bytes (16 FP32 elements * 4 bytes = 64)
    // Each dim0 row = 16*4 = 64 bytes < 128 bytes cacheline → 1 CL per row
    // 16 rows → 16 CL requests total
    test(new DMA_core).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      initDut(dut)
      val cmd = TmaCmd(
        name = "T1_2d",
        wid = 0,
        dataType = 6,           // FLOAT32
        tensorRank = 2,
        globalAddress = 0x10000,
        globalDim = Seq(16, 16, 1, 1, 1),
        globalStrides = Seq(64, 0, 0, 0, 0),  // stride(0) = 16*4 = 64
        boxAddress = 0x10000,
        boxDim = Seq(16, 16, 1, 1, 1),
        elementStrides = Seq(1, 1, 1, 1, 1),
        dst = 0x0
      )

      val expectedL2 = TmaRefModel.computeExpectedL2Addrs(cmd)
      val fences = scala.collection.mutable.ArrayBuffer.empty[BigInt]
      driveTmaCmd(dut, cmd, fences)
      val (reqs, fenceResult, sharedObs) = tmaServiceUntilDone(dut, expectedL2.length, 1)

      // Verify L2 addresses
      val reqAddrs = reqs.map(_._2.toInt)
      assert(reqAddrs == expectedL2,
        s"T1 L2 addrs mismatch:\n  got=${reqAddrs.map(a => f"0x$a%x")}\n  exp=${expectedL2.map(a => f"0x$a%x")}")

      // Verify fence
      assert(fenceResult.contains(BigInt(cmd.wid)), s"T1 fence should contain wid=${cmd.wid}")

      // Verify shared writes happened
      assert(sharedObs.nonEmpty, "T1 should issue shared memory writes")
    }
  }

  "TMA_T4_3d_carry" in {
    // 3D tensor: FP32, 4x4x4 box
    // globalStrides(0)=16 bytes (4*4), globalStrides(1)=64 bytes (4*16)
    // 4*4 = 16 dim0 rows, each 4*4=16 bytes → 1 CL per row → 16 CL requests
    test(new DMA_core).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      initDut(dut)
      val cmd = TmaCmd(
        name = "T4_3d",
        wid = 1,
        dataType = 6,
        tensorRank = 3,
        globalAddress = 0x20000,
        globalDim = Seq(4, 4, 4, 1, 1),
        globalStrides = Seq(16, 64, 0, 0, 0),
        boxAddress = 0x20000,
        boxDim = Seq(4, 4, 4, 1, 1),
        elementStrides = Seq(1, 1, 1, 1, 1),
        dst = 0x100
      )

      val expectedL2 = TmaRefModel.computeExpectedL2Addrs(cmd)
      driveTmaCmd(dut, cmd)
      val (reqs, fenceResult, sharedObs) = tmaServiceUntilDone(dut, expectedL2.length, 1)

      val reqAddrs = reqs.map(_._2.toInt)
      assert(reqAddrs == expectedL2,
        s"T4 L2 addrs mismatch:\n  got=${reqAddrs.map(a => f"0x$a%x")}\n  exp=${expectedL2.map(a => f"0x$a%x")}")
      assert(fenceResult.contains(BigInt(cmd.wid)), s"T4 fence mismatch")
      assert(sharedObs.nonEmpty, "T4 should issue shared memory writes")
    }
  }

  "TMA_T2_2d_oob_zero" in {
    // 2D tensor with OOB: FP32, boxDim(0)=16 but globalDim(0)=8
    // Elements 8..15 in each row are out of bounds → should be filled with zero
    // Each row = 16*4=64 bytes, 4 rows → 4 CL requests
    test(new DMA_core).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      initDut(dut)
      val cmd = TmaCmd(
        name = "T2_oob",
        wid = 2,
        dataType = 6,
        tensorRank = 2,
        globalAddress = 0x30000,
        globalDim = Seq(8, 4, 1, 1, 1),   // only 8 elements wide
        globalStrides = Seq(32, 0, 0, 0, 0), // 8*4=32
        boxAddress = 0x30000,
        boxDim = Seq(16, 4, 1, 1, 1),     // requesting 16 elements wide → OOB
        elementStrides = Seq(1, 1, 1, 1, 1),
        oobfill = 0,  // fill zero
        dst = 0x200
      )

      val expectedL2 = TmaRefModel.computeExpectedL2Addrs(cmd)
      driveTmaCmd(dut, cmd)
      val (reqs, fenceResult, sharedObs) = tmaServiceUntilDone(dut, expectedL2.length, 1)

      val reqAddrs = reqs.map(_._2.toInt)
      assert(reqAddrs == expectedL2,
        s"T2 L2 addrs mismatch:\n  got=${reqAddrs.map(a => f"0x$a%x")}\n  exp=${expectedL2.map(a => f"0x$a%x")}")
      assert(fenceResult.contains(BigInt(cmd.wid)), s"T2 fence mismatch")
      assert(sharedObs.nonEmpty, "T2 should issue shared memory writes")
    }
  }

  "TMA_T10_back2back_B2" in {
    // Back-to-back tensor commands: first 3D, then 2D
    // Tests B2 fix: second command must NOT inherit first command's dim_step_reg
    test(new DMA_core).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      initDut(dut)

      // First: 3D 2x2x2 FP32
      val cmd1 = TmaCmd(
        name = "T10_first",
        wid = 3,
        dataType = 6,
        tensorRank = 3,
        globalAddress = 0x40000,
        globalDim = Seq(2, 2, 2, 1, 1),
        globalStrides = Seq(8, 16, 0, 0, 0),
        boxAddress = 0x40000,
        boxDim = Seq(2, 2, 2, 1, 1),
        elementStrides = Seq(1, 1, 1, 1, 1),
        dst = 0x300
      )
      val expectedL2_1 = TmaRefModel.computeExpectedL2Addrs(cmd1)
      driveTmaCmd(dut, cmd1)
      val (reqs1, fences1, _) = tmaServiceUntilDone(dut, expectedL2_1.length, 1)

      assert(fences1.contains(BigInt(cmd1.wid)), s"T10 first fence should be wid=${cmd1.wid}")

      // Second: 2D 4x4 FP32
      val cmd2 = TmaCmd(
        name = "T10_second",
        wid = 4,
        dataType = 6,
        tensorRank = 2,
        globalAddress = 0x50000,
        globalDim = Seq(4, 4, 1, 1, 1),
        globalStrides = Seq(16, 0, 0, 0, 0),
        boxAddress = 0x50000,
        boxDim = Seq(4, 4, 1, 1, 1),
        elementStrides = Seq(1, 1, 1, 1, 1),
        dst = 0x400
      )
      val expectedL2_2 = TmaRefModel.computeExpectedL2Addrs(cmd2)
      driveTmaCmd(dut, cmd2)
      val (reqs2, fences2, _) = tmaServiceUntilDone(dut, expectedL2_2.length, 1)

      val reqAddrs2 = reqs2.map(_._2.toInt)
      assert(reqAddrs2 == expectedL2_2,
        s"T10 second cmd L2 addrs mismatch (B2 bug?):\n  got=${reqAddrs2.map(a => f"0x$a%x")}\n  exp=${expectedL2_2.map(a => f"0x$a%x")}")
      assert(fences2.contains(BigInt(cmd2.wid)), s"T10 second fence should be wid=${cmd2.wid}")
    }
  }

  "TMA_T12_5d" in {
    // 5D tensor: FP32, 2x2x2x2x2 box
    // 2^4 = 16 dim0 rows, each 2*4=8 bytes → 1 CL per row → 16 CL requests
    test(new DMA_core).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      initDut(dut)
      val cmd = TmaCmd(
        name = "T12_5d",
        wid = 5,
        dataType = 6,
        tensorRank = 5,
        globalAddress = 0x60000,
        globalDim = Seq(2, 2, 2, 2, 2),
        globalStrides = Seq(8, 16, 32, 64, 0),
        boxAddress = 0x60000,
        boxDim = Seq(2, 2, 2, 2, 2),
        elementStrides = Seq(1, 1, 1, 1, 1),
        dst = 0x500
      )

      val expectedL2 = TmaRefModel.computeExpectedL2Addrs(cmd)
      driveTmaCmd(dut, cmd)
      val (reqs, fenceResult, sharedObs) = tmaServiceUntilDone(dut, expectedL2.length, 1)

      val reqAddrs = reqs.map(_._2.toInt)
      assert(reqAddrs == expectedL2,
        s"T12 L2 addrs mismatch:\n  got=${reqAddrs.map(a => f"0x$a%x")}\n  exp=${expectedL2.map(a => f"0x$a%x")}")
      assert(fenceResult.contains(BigInt(cmd.wid)), s"T12 fence mismatch")
      assert(sharedObs.nonEmpty, "T12 should issue shared memory writes")
    }
  }

  // =====================================================================
  /** Service loop with deterministic per-row L2 response:
    * - The N-th L2 request gets d_data(w) = (N << 28) | (w & 0xFFFFFFF)
    *   so that the top 4 bits encode which "row iteration" (1-based) it belongs to.
    * - Returns (reqs, fences, sharedObs) same as tmaServiceUntilDone.
    */
  def tmaServiceWithRowTaggedData(
      dut: DMA_core,
      expectedL2Count: Int,
      expectedFenceCount: Int,
      tlb: TlbMockState = new TlbMockState()
  ): (Seq[(BigInt, BigInt)], Seq[BigInt], Seq[SharedReqObs]) = {
    val reqs = scala.collection.mutable.ArrayBuffer.empty[(BigInt, BigInt)]
    val fences = scala.collection.mutable.ArrayBuffer.empty[BigInt]
    val sharedReqObs = scala.collection.mutable.ArrayBuffer.empty[SharedReqObs]
    var cycles = 0
    val maxCycles = 2000

    while ((reqs.length < expectedL2Count || fences.length < expectedFenceCount) && cycles < maxCycles) {
      var progressed = false
      mockTlbCycle(dut, fences, tlb)

      if (dut.io.dma_cache_req.valid.peekBoolean()) {
        val source = dut.io.dma_cache_req.bits.a_source.peekInt()
        val addr = dut.io.dma_cache_req.bits.a_addr.map(_.peekInt()).getOrElse(BigInt(0))
        reqs += ((source, addr))
        val rowTag = reqs.length  // 1-based row index

        stepAndSample(dut, fences)
        dut.io.dma_cache_rsp.valid.poke(true.B)
        dut.io.dma_cache_rsp.bits.d_opcode.poke(1.U)
        dut.io.dma_cache_rsp.bits.d_source.poke(source.U)
        dut.io.dma_cache_rsp.bits.d_addr.poke(0.U)
        for (w <- 0 until dcache_BlockWords) {
          val payload = ((BigInt(rowTag) & 0xf) << 28) | BigInt(w * 4)
          dut.io.dma_cache_rsp.bits.d_data(w).poke(payload.U)
        }
        var rspCycles = 0
        while (!dut.io.dma_cache_rsp.ready.peekBoolean() && rspCycles < 30) {
          stepAndSample(dut, fences)
          rspCycles += 1
        }
        assert(dut.io.dma_cache_rsp.ready.peekBoolean(), "dma_cache_rsp should be ready")
        stepAndSample(dut, fences)
        dut.io.dma_cache_rsp.valid.poke(false.B)
        sampleFence(dut, fences)
        progressed = true
      } else {
        var keepDrainingShared = true
        while (keepDrainingShared) {
          collectSharedReq(dut, "tmaServiceRowTag") match {
            case Some(plan) =>
              sharedReqObs += plan
              sendSharedRsp(dut, plan, fences)
              progressed = true
            case None =>
              keepDrainingShared = false
          }
        }
      }
      sampleFence(dut, fences)
      if (!progressed) stepAndSample(dut, fences)
      cycles += 1
    }
    assert(reqs.length == expectedL2Count,
      s"Expected $expectedL2Count L2 requests, got ${reqs.length}")
    assert(fences.length == expectedFenceCount,
      s"Expected $expectedFenceCount fences, got ${fences.length}")
    (reqs.toSeq, fences.toSeq, sharedReqObs.toSeq)
  }

  // Helper: for a shared_req observation, collect the set of row-tags that appear
  // in the high 4 bits of any active lane's data. If a single shared_req mixes
  // row-tags, that by itself signals the P0-3 bug (row 2 write sees row 1 data).
  def rowTagsOf(obs: SharedReqObs): Set[Int] =
    obs.activeMask.zipWithIndex.collect { case (true, lane) =>
      ((obs.laneData(lane) >> 28) & 0xf).toInt
    }.toSet

  def activeLaneDataLsb(obs: SharedReqObs): Seq[Int] =
    obs.activeMask.zip(obs.laneData).collect { case (true, data) =>
      (data & 0xff).toInt
    }

  def activeBlockOffsets(obs: SharedReqObs): Seq[Int] =
    obs.activeMask.zip(obs.blockOffsets).collect { case (true, blockOffset) =>
      blockOffset.toInt
    }

  "TMA_T20_2d_datawidth2_row_attribution" in {
    // Module-level guard for the 2B tensor path used by FP16_2D_8x8.
    // 2D FP16 tensor 8x8, no box offset, no padding, elementStrides=1.
    // All 8 rows share cacheline 0 (row i covers bytes [i*16, i*16+16)).
    // Expectation: shared_req #i has data whose row-tag == i+1 (1-based from L2 response).
    test(new DMA_core).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      initDut(dut)
      val cmd = TmaCmd(
        name = "T20_fp16_2d_8x8",
        wid = 6,
        dataType = 7,              // FLOAT16
        tensorRank = 2,
        globalAddress = 0x70000,
        globalDim = Seq(8, 8, 1, 1, 1),
        globalStrides = Seq(16, 0, 0, 0, 0),
        boxAddress = 0x70000,
        boxDim = Seq(8, 8, 1, 1, 1),
        elementStrides = Seq(1, 1, 1, 1, 1),
        dst = 0x600
      )
      driveTmaCmd(dut, cmd)
      val (_, fenceResult, sharedObs) = tmaServiceWithRowTaggedData(dut, 8, 1)

      assert(fenceResult.contains(BigInt(cmd.wid)), "T20 fence mismatch")
      assert(sharedObs.size >= 8,
        s"T20 expected >= 8 shared_req (one per row), got ${sharedObs.size}")

      // Each shared_req should be single-row (its active-lane data all tagged to one row).
      sharedObs.zipWithIndex.foreach { case (obs, i) =>
        val tags = rowTagsOf(obs)
        assert(tags.size == 1,
          s"T20 shared_req #$i mixes rows (P0-3 bug?): tags=$tags, data=${obs.laneData.zipWithIndex.filter(p => obs.activeMask(p._2)).map(p => f"lane${p._2}=0x${p._1}%x").mkString(",")}")
      }
    }
  }

  "TMA_T21_2d_datawidth1_row_attribution" in {
    // 2D INT8 tensor 16x16, each row is 16 bytes, 16 rows, all in cacheline 0 + 1
    // (since 16 rows * 16 stride = 256 bytes spans two cachelines).
    test(new DMA_core).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      initDut(dut)
      val cmd = TmaCmd(
        name = "T21_i8_2d_16x16",
        wid = 7,
        dataType = 3,              // INT8
        tensorRank = 2,
        globalAddress = 0x80000,
        globalDim = Seq(16, 16, 1, 1, 1),
        globalStrides = Seq(16, 0, 0, 0, 0),
        boxAddress = 0x80000,
        boxDim = Seq(16, 16, 1, 1, 1),
        elementStrides = Seq(1, 1, 1, 1, 1),
        dst = 0x700
      )
      driveTmaCmd(dut, cmd)
      // 16 rows, each occupies 1 L2 request regardless of cacheline reuse
      val (_, fenceResult, sharedObs) = tmaServiceWithRowTaggedData(dut, 16, 1)

      assert(fenceResult.contains(BigInt(cmd.wid)), "T21 fence mismatch")
      assert(sharedObs.size >= 16,
        s"T21 expected >= 16 shared_req, got ${sharedObs.size}")
      sharedObs.zipWithIndex.foreach { case (obs, i) =>
        val tags = rowTagsOf(obs)
        assert(tags.size == 1,
          s"T21 shared_req #$i mixes rows (P0-3 bug?): tags=$tags")
      }
    }
  }

  /** Like tmaServiceWithRowTaggedData, but BATCHES pending L2 responses: let N
    * requests fire before responding to any of them. This exposes tag-slot /
    * data-slot aliasing races that the lock-step mock hides. Used to reproduce
    * the RTL-only P0-3 failure.
    */
  def tmaServiceBatchedResponses(
      dut: DMA_core,
      expectedL2Count: Int,
      expectedFenceCount: Int,
      batchBeforeRsp: Int = 4,    // hold back this many L2 rsp before draining
      tlb: TlbMockState = new TlbMockState()
  ): (Seq[(BigInt, BigInt)], Seq[BigInt], Seq[SharedReqObs]) = {
    val reqs = scala.collection.mutable.ArrayBuffer.empty[(BigInt, BigInt)]
    val pendingRsp = scala.collection.mutable.Queue.empty[(BigInt, Int)] // (source, rowTag)
    val fences = scala.collection.mutable.ArrayBuffer.empty[BigInt]
    val sharedReqObs = scala.collection.mutable.ArrayBuffer.empty[SharedReqObs]
    var cycles = 0
    val maxCycles = 4000

    while ((reqs.length < expectedL2Count || fences.length < expectedFenceCount) && cycles < maxCycles) {
      var progressed = false
      mockTlbCycle(dut, fences, tlb)

      // 1) capture any new L2 request, but don't respond yet
      if (dut.io.dma_cache_req.valid.peekBoolean()) {
        val source = dut.io.dma_cache_req.bits.a_source.peekInt()
        val addr = dut.io.dma_cache_req.bits.a_addr.map(_.peekInt()).getOrElse(BigInt(0))
        reqs += ((source, addr))
        pendingRsp += ((source, reqs.length))
        stepAndSample(dut, fences)
        progressed = true
      }

      // 2) drain pending L2 responses only when batch reaches threshold or no more
      //    requests expected. This keeps multiple tag slots in-flight simultaneously.
      val shouldDrain = (pendingRsp.nonEmpty &&
        (pendingRsp.size >= batchBeforeRsp || reqs.length >= expectedL2Count))
      if (shouldDrain) {
        while (pendingRsp.nonEmpty) {
          val (source, rowTag) = pendingRsp.dequeue()
          dut.io.dma_cache_rsp.valid.poke(true.B)
          dut.io.dma_cache_rsp.bits.d_opcode.poke(1.U)
          dut.io.dma_cache_rsp.bits.d_source.poke(source.U)
          dut.io.dma_cache_rsp.bits.d_addr.poke(0.U)
          for (w <- 0 until dcache_BlockWords) {
            val payload = ((BigInt(rowTag) & 0xf) << 28) | BigInt(w * 4)
            dut.io.dma_cache_rsp.bits.d_data(w).poke(payload.U)
          }
          var rspCycles = 0
          while (!dut.io.dma_cache_rsp.ready.peekBoolean() && rspCycles < 100) {
            stepAndSample(dut, fences)
            rspCycles += 1
          }
          if (rspCycles >= 100) {
            // Drain shared_req loopback to unblock Temp_mem, then retry
            collectSharedReq(dut, "batchDrain") match {
              case Some(plan) => sharedReqObs += plan; sendSharedRsp(dut, plan, fences)
              case None => // shared path idle too, need more cycles
            }
            rspCycles = 0
            while (!dut.io.dma_cache_rsp.ready.peekBoolean() && rspCycles < 200) {
              stepAndSample(dut, fences)
              rspCycles += 1
            }
          }
          assert(dut.io.dma_cache_rsp.ready.peekBoolean(), s"batch dma_cache_rsp hung on rowTag=$rowTag")
          stepAndSample(dut, fences)
          dut.io.dma_cache_rsp.valid.poke(false.B)
          sampleFence(dut, fences)
        }
        progressed = true
      }

      // 3) drain any shared_req that have piled up
      var keepDrainingShared = true
      while (keepDrainingShared) {
        collectSharedReq(dut, "batchService") match {
          case Some(plan) =>
            sharedReqObs += plan
            sendSharedRsp(dut, plan, fences)
            progressed = true
          case None => keepDrainingShared = false
        }
      }

      sampleFence(dut, fences)
      if (!progressed) stepAndSample(dut, fences)
      cycles += 1
    }
    assert(reqs.length == expectedL2Count,
      s"Expected $expectedL2Count L2 requests, got ${reqs.length}")
    assert(fences.length == expectedFenceCount,
      s"Expected $expectedFenceCount fences, got ${fences.length}")
    (reqs.toSeq, fences.toSeq, sharedReqObs.toSeq)
  }

  "TMA_T23_2d_datawidth2_batched_response" in {
    // Same tensor layout as T20, but with batched L2 response. This keeps multiple
    // tag slots in-flight at once and guards the row attribution fix under overlap.
    test(new DMA_core).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      initDut(dut)
      val cmd = TmaCmd(
        name = "T23_fp16_2d_8x8_batched",
        wid = 1,
        dataType = 7,
        tensorRank = 2,
        globalAddress = 0x70000,
        globalDim = Seq(8, 8, 1, 1, 1),
        globalStrides = Seq(16, 0, 0, 0, 0),
        boxAddress = 0x70000,
        boxDim = Seq(8, 8, 1, 1, 1),
        elementStrides = Seq(1, 1, 1, 1, 1),
        dst = 0x600
      )
      driveTmaCmd(dut, cmd)
      val (_, fenceResult, sharedObs) = tmaServiceBatchedResponses(dut, 8, 1, batchBeforeRsp = 4)

      assert(fenceResult.contains(BigInt(cmd.wid)), "T23 fence mismatch")
      assert(sharedObs.nonEmpty, "T23 should issue shared memory writes")
      sharedObs.zipWithIndex.foreach { case (obs, i) =>
        val tags = rowTagsOf(obs)
        assert(tags.size == 1,
          s"T23 shared_req #$i mixes rows (P0-3 bug?): tags=$tags, " +
            s"actives=${obs.laneData.zipWithIndex.filter(p => obs.activeMask(p._2)).map(p => f"l${p._2}=0x${p._1}%x").mkString(",")}")
      }
    }
  }

  "TMA_T22_2d_subbox_row_attribution" in {
    // Repro of P0-1 (tma_matrix_test FP32_2D_subbox_8x8_at_2_2 failure).
    // 2D FP32, tensor 8x8, box 4x4 at offset [2,2] inside it.
    // Row N covers bytes [2*32 + 2*4 + N*32, ...) = [72 + N*32, 72+N*32+16) globally,
    // sharing cacheline 0 (rows 0..1) and cacheline 128 (rows 2..3) at L2.
    test(new DMA_core).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      initDut(dut)
      val cmd = TmaCmd(
        name = "T22_subbox",
        wid = 2,
        dataType = 6,              // FLOAT32
        tensorRank = 2,
        globalAddress = 0x90000,
        globalDim = Seq(8, 8, 1, 1, 1),
        globalStrides = Seq(32, 0, 0, 0, 0),
        // boxAddress = globalAddress + boxOffsetElems*globalStrides
        // boxOffsetElems = [2,2] → +2*4 +2*32 = +72
        boxAddress = 0x90000 + 72,
        boxDim = Seq(4, 4, 1, 1, 1),
        elementStrides = Seq(1, 1, 1, 1, 1),
        dst = 0x800
      )
      driveTmaCmd(dut, cmd)
      // 4 rows × 1 L2 request each (one per row box_dim0 bytes are 16, fit in one cl)
      val (_, fenceResult, sharedObs) = tmaServiceWithRowTaggedData(dut, 4, 1)

      assert(fenceResult.contains(BigInt(cmd.wid)), "T22 fence mismatch")
      assert(sharedObs.nonEmpty, "T22 should issue shared memory writes (P0-1: row0 may drop)")
      assert(sharedObs.size >= 4,
        s"T22 expected >= 4 shared_req (one per sub-box row), got ${sharedObs.size}")
      sharedObs.zipWithIndex.foreach { case (obs, i) =>
        val tags = rowTagsOf(obs)
        assert(tags.size == 1,
          s"T22 shared_req #$i mixes rows (P0-1 bug?): tags=$tags")
      }
    }
  }

  "TMA_T24_2d_estride2_dim0_gather" in {
    // Expectation: for each 4-element row, only every other FP32 in source is kept,
    // so active lane payload offsets are [0, 8, 16, 24] while shared block offsets
    // are packed contiguously.
    test(new DMA_core).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      initDut(dut)
      val cmd = TmaCmd(
        name = "T24_fp32_estride2",
        wid = 2,
        dataType = 6,
        tensorRank = 2,
        globalAddress = 0xA0000,
        globalDim = Seq(8, 4, 1, 1, 1),
        globalStrides = Seq(32, 0, 0, 0, 0),
        boxAddress = 0xA0000,
        boxDim = Seq(4, 4, 1, 1, 1),
        elementStrides = Seq(2, 1, 1, 1, 1),
        dst = 0x000
      )
      driveTmaCmd(dut, cmd)
      val (_, fenceResult, sharedObs) = tmaServiceWithRowTaggedData(dut, 4, 1)

      assert(fenceResult.contains(BigInt(cmd.wid)), "T24 fence mismatch")
      assert(sharedObs.size >= 4,
        s"T24 expected >= 4 shared_req (one per row), got ${sharedObs.size}")

      sharedObs.zipWithIndex.foreach { case (obs, i) =>
        val expectedPayloadOffsets = Seq(0, 8, 16, 24).map(_ + i * 32)
        val expectedBlockOffsets = Seq(0, 1, 2, 3).map(_ + i * 4)
        val payloadOffsets = activeLaneDataLsb(obs)
        val blockOffsets = activeBlockOffsets(obs)
        assert(payloadOffsets == expectedPayloadOffsets,
          s"T24 shared_req #$i picked wrong source columns: payloadOffsets=$payloadOffsets")
        assert(blockOffsets == expectedBlockOffsets,
          s"T24 shared_req #$i destination packing mismatch: blockOffsets=$blockOffsets")
      }
    }
  }

  "TMA_T25_2d_padded_rows_row_attribution" in {
    // Module-level guard for FP32_2D_padded_rows_4x4_stride64.
    // Each row is 16B of payload separated by 64B in source memory.
    test(new DMA_core).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      initDut(dut)
      val cmd = TmaCmd(
        name = "T25_padded_rows",
        wid = 3,
        dataType = 6,
        tensorRank = 2,
        globalAddress = 0xB0000,
        globalDim = Seq(4, 4, 1, 1, 1),
        globalStrides = Seq(64, 0, 0, 0, 0),
        boxAddress = 0xB0000,
        boxDim = Seq(4, 4, 1, 1, 1),
        elementStrides = Seq(1, 1, 1, 1, 1),
        dst = 0x000
      )
      driveTmaCmd(dut, cmd)
      val (_, fenceResult, sharedObs) = tmaServiceWithRowTaggedData(dut, 4, 1)

      assert(fenceResult.contains(BigInt(cmd.wid)), "T25 fence mismatch")
      assert(sharedObs.size >= 4,
        s"T25 expected >= 4 shared_req (one per row), got ${sharedObs.size}")
      sharedObs.zipWithIndex.foreach { case (obs, i) =>
        val tags = rowTagsOf(obs)
        assert(tags.size == 1,
          s"T25 shared_req #$i mixes padded rows: tags=$tags")
      }
    }
  }
}

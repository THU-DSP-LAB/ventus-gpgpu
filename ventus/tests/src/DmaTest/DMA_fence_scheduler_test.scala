/*
 * DMA fence scheduler closure test
 * Verifies that CP_ASYNC_FENCE blocks the issuing warp until DMA completion arrives.
 */
package DmaTest

import L1Cache.MyConfig
import chisel3._
import chiseltest._
import config.config.Parameters
import org.scalatest.freespec.AnyFreeSpec
import pipeline.warp_scheduler
import top.parameters.max_dma_inst

class DMA_fence_scheduler_test extends AnyFreeSpec with ChiselScalatestTester {

  implicit val p: Parameters = (new MyConfig).toInstance

  private def bit(value: BigInt, idx: Int): BigInt = (value >> idx) & 1

  private def initScheduler(dut: warp_scheduler): Unit = {
    dut.io.pc_reset.poke(false.B)
    dut.io.warpReq.valid.poke(false.B)
    dut.io.warpReq.bits.wid.poke(0.U)
    dut.io.warpReq.bits.CTAdata.dispatch2cu_wg_wf_count.poke(0.U)
    dut.io.warpReq.bits.CTAdata.dispatch2cu_wf_size_dispatch.poke(0.U)
    dut.io.warpReq.bits.CTAdata.dispatch2cu_wf_tag_dispatch.poke(0.U)
    dut.io.warpReq.bits.CTAdata.dispatch2cu_start_pc_dispatch.poke(0.U)
    dut.io.warpRsp.ready.poke(true.B)
    dut.io.wg_id_tag.poke(0.U)

    dut.io.pc_req.ready.poke(true.B)
    dut.io.pc_rsp.valid.poke(false.B)
    dut.io.pc_rsp.bits.warpid.poke(0.U)
    dut.io.pc_rsp.bits.addr.poke(0.U)
    dut.io.pc_rsp.bits.data.poke(0.U)
    dut.io.pc_rsp.bits.mask.poke(0.U)
    dut.io.pc_rsp.bits.status.poke(0.U)

    dut.io.branch.valid.poke(false.B)
    dut.io.branch.bits.wid.poke(0.U)
    dut.io.branch.bits.jump.poke(false.B)
    dut.io.branch.bits.new_pc.poke(0.U)
    dut.io.warp_control.valid.poke(false.B)
    dut.io.warp_control.bits.ctrl.wid.poke(0.U)
    dut.io.warp_control.bits.ctrl.simt_stack_op.poke(false.B)
    dut.io.warp_control.bits.ctrl.barrier.poke(false.B)
    dut.io.warp_control.bits.ctrl.dma.poke(false.B)
    dut.io.warp_control.bits.ctrl.funct.poke(0.U)

    dut.io.dma_issue.valid.poke(false.B)
    dut.io.dma_issue.bits.poke(0.U)
    dut.io.dma_complete.valid.poke(false.B)
    dut.io.dma_complete.bits.poke(0.U)

    dut.io.issued_warp.valid.poke(false.B)
    dut.io.issued_warp.bits.poke(0.U)
    dut.io.scoreboard_busy.poke(0.U)
    dut.io.exe_busy.poke(0.U)
    dut.io.pc_ibuffer_ready.foreach(_.poke(1.U))
    dut.io.flushDCache.ready.poke(true.B)

    dut.clock.step(2)
  }

  private def activateWarp(dut: warp_scheduler, wid: Int): Unit = {
    dut.io.warpReq.valid.poke(true.B)
    dut.io.warpReq.bits.wid.poke(wid.U)
    dut.io.warpReq.bits.CTAdata.dispatch2cu_wg_wf_count.poke(1.U)
    dut.io.warpReq.bits.CTAdata.dispatch2cu_wf_size_dispatch.poke(1.U)
    dut.io.warpReq.bits.CTAdata.dispatch2cu_wf_tag_dispatch.poke(0.U)
    dut.io.warpReq.bits.CTAdata.dispatch2cu_start_pc_dispatch.poke("h80000000".U)

    while (!dut.io.warpReq.ready.peekBoolean()) {
      dut.clock.step()
    }
    dut.clock.step()
    dut.io.warpReq.valid.poke(false.B)
  }

  private def issueDmaFence(dut: warp_scheduler, wid: Int): Unit = {
    dut.io.warp_control.valid.poke(true.B)
    dut.io.warp_control.bits.ctrl.wid.poke(wid.U)
    dut.io.warp_control.bits.ctrl.simt_stack_op.poke(false.B)
    dut.io.warp_control.bits.ctrl.dma.poke(true.B)
    dut.io.warp_control.bits.ctrl.funct.poke(4.U)
    dut.io.warp_control.bits.ctrl.barrier.poke(true.B)

    while (!dut.io.warp_control.ready.peekBoolean()) {
      dut.clock.step()
    }
    dut.clock.step()
    dut.io.warp_control.valid.poke(false.B)
  }

  private def pulseDmaIssue(dut: warp_scheduler, wid: Int): Unit = {
    dut.io.dma_issue.valid.poke(true.B)
    dut.io.dma_issue.bits.poke(wid.U)
    dut.clock.step()
    dut.io.dma_issue.valid.poke(false.B)
  }

  private def pulseDmaComplete(dut: warp_scheduler, wid: Int): Unit = {
    dut.io.dma_complete.valid.poke(true.B)
    dut.io.dma_complete.bits.poke(wid.U)
    dut.clock.step()
    dut.io.dma_complete.valid.poke(false.B)
  }

  "DMA fence blocks until completion" in {
    test(new warp_scheduler).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      initScheduler(dut)
      activateWarp(dut, wid = 0)

      assert(bit(dut.io.warp_ready.peekInt(), 0) == 1, "warp0 should be schedulable before DMA fence")
      assert(dut.io.dma_inflight_dbg(0).peekInt() == 0, "inflight should start from zero")
      assert(dut.io.dma_issue_allow(0).peekBoolean(), "DMA issue should be allowed at zero inflight")

      pulseDmaIssue(dut, wid = 0)

      assert(dut.io.dma_inflight_dbg(0).peekInt() == 1, "DMA issue must increment inflight count")

      issueDmaFence(dut, wid = 0)

      assert(bit(dut.io.dma_fence_wait_dbg.peekInt(), 0) == 1, "DMA fence should arm wait bit when inflight DMA exists")
      assert(bit(dut.io.warp_ready.peekInt(), 0) == 0, "warp0 must be blocked while DMA fence wait is set")

      pulseDmaComplete(dut, wid = 0)
      dut.clock.step()

      assert(dut.io.dma_inflight_dbg(0).peekInt() == 0, "DMA completion must decrement inflight count")
      assert(bit(dut.io.dma_fence_wait_dbg.peekInt(), 0) == 0, "DMA fence wait bit should clear after completion")
      assert(bit(dut.io.warp_ready.peekInt(), 0) == 1, "warp0 should be released after DMA completion")
      assert(dut.io.dma_issue_allow(0).peekBoolean(), "DMA issue should be allowed again after completion")
    }
  }

  "DMA issue allow deasserts at inflight limit and recovers on completion" in {
    test(new warp_scheduler).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      initScheduler(dut)
      activateWarp(dut, wid = 0)

      for (_ <- 0 until max_dma_inst) {
        assert(dut.io.dma_issue_allow(0).peekBoolean(), "DMA issue should be allowed before reaching max inflight")
        pulseDmaIssue(dut, wid = 0)
      }

      assert(dut.io.dma_inflight_dbg(0).peekInt() == max_dma_inst, "inflight should saturate at max_dma_inst")
      assert(!dut.io.dma_issue_allow(0).peekBoolean(), "DMA issue must be blocked once inflight reaches max_dma_inst")

      pulseDmaComplete(dut, wid = 0)
      dut.clock.step()

      assert(dut.io.dma_inflight_dbg(0).peekInt() == (max_dma_inst - 1), "DMA completion should release one inflight slot")
      assert(dut.io.dma_issue_allow(0).peekBoolean(), "DMA issue should reopen after an inflight slot is released")
    }
  }

  "Multi-warp DMA + fence interleave: fence on warp0 does not block warp1" in {
    test(new warp_scheduler).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      initScheduler(dut)
      activateWarp(dut, wid = 0)
      activateWarp(dut, wid = 1)

      // Both warps issue DMA
      pulseDmaIssue(dut, wid = 0)
      pulseDmaIssue(dut, wid = 1)

      assert(dut.io.dma_inflight_dbg(0).peekInt() == 1, "warp0 inflight should be 1")
      assert(dut.io.dma_inflight_dbg(1).peekInt() == 1, "warp1 inflight should be 1")

      // Only warp0 fences
      issueDmaFence(dut, wid = 0)

      assert(bit(dut.io.dma_fence_wait_dbg.peekInt(), 0) == 1, "warp0 should be fence-waiting")
      assert(bit(dut.io.warp_ready.peekInt(), 0) == 0, "warp0 must be blocked by fence")
      assert(bit(dut.io.warp_ready.peekInt(), 1) == 1, "warp1 must remain schedulable (no fence)")

      // Complete warp0's DMA
      pulseDmaComplete(dut, wid = 0)
      dut.clock.step()

      assert(bit(dut.io.dma_fence_wait_dbg.peekInt(), 0) == 0, "warp0 fence should clear after completion")
      assert(bit(dut.io.warp_ready.peekInt(), 0) == 1, "warp0 should be released")
      assert(bit(dut.io.warp_ready.peekInt(), 1) == 1, "warp1 should still be schedulable")
      assert(dut.io.dma_inflight_dbg(1).peekInt() == 1, "warp1 inflight should still be 1")

      // Complete warp1's DMA
      pulseDmaComplete(dut, wid = 1)
      dut.clock.step()

      assert(dut.io.dma_inflight_dbg(1).peekInt() == 0, "warp1 inflight should be 0 after completion")
    }
  }

  "Fence with zero inflight is a no-op" in {
    test(new warp_scheduler).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      initScheduler(dut)
      activateWarp(dut, wid = 0)

      // No DMA issued, directly fence
      issueDmaFence(dut, wid = 0)

      // With zero inflight, fence_wait should NOT arm
      assert(bit(dut.io.dma_fence_wait_dbg.peekInt(), 0) == 0,
        "Fence with zero inflight should not set fence_wait")
      assert(bit(dut.io.warp_ready.peekInt(), 0) == 1,
        "Warp should remain schedulable after no-op fence")
    }
  }

  "All warps concurrent DMA then fence: all block, then release one by one" in {
    test(new warp_scheduler).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      initScheduler(dut)
      val numWarps = scala.math.min(top.parameters.num_warp, max_dma_inst)

      // Activate and issue DMA on all warps
      for (w <- 0 until numWarps) {
        activateWarp(dut, wid = w)
        pulseDmaIssue(dut, wid = w)
      }

      // All warps issue fence
      for (w <- 0 until numWarps) {
        issueDmaFence(dut, wid = w)
      }

      // All warps should be blocked
      for (w <- 0 until numWarps) {
        assert(bit(dut.io.warp_ready.peekInt(), w) == 0,
          s"warp $w must be blocked after DMA fence")
      }

      // Complete one by one and verify selective release
      for (w <- 0 until numWarps) {
        pulseDmaComplete(dut, wid = w)
        dut.clock.step()

        assert(bit(dut.io.warp_ready.peekInt(), w) == 1,
          s"warp $w should be released after DMA completion")
        // Verify remaining warps still blocked
        for (r <- (w + 1) until numWarps) {
          assert(bit(dut.io.warp_ready.peekInt(), r) == 0,
            s"warp $r should still be blocked (not yet completed)")
        }
      }
    }
  }
}

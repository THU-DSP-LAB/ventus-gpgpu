package pipeline

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import top.parameters._

class WarpSchedulerSimultaneousHarness extends Module {
  val io = IO(new Bundle {
    // Warp dispatch interface
    val warpReqValid = Input(Bool())
    val warpReqWid = Input(UInt(depth_warp.W))
    val warpReqWgId = Input(UInt(log2Ceil(CTA_SCHE_CONFIG.WG.NUM_WG_DIM_MAX + 1).W))
    val warpReqWfCount = Input(UInt(log2Ceil(CTA_SCHE_CONFIG.WG.NUM_WF_MAX + 1).W))

    // Warp end/control interface
    val warpCtrlValid = Input(Bool())
    val warpCtrlWid = Input(UInt(depth_warp.W))
    val warpCtrlEndprg = Input(Bool())
    val warpCtrlWgId = Input(UInt(TAG_WIDTH.W))

    // Flush DCache monitoring
    val flushDCacheReq = Output(Bool())
  })

  val scheduler = Module(new warp_scheduler)
  scheduler.io.pc_reset := false.B

  // Default tie-offs
  scheduler.io.warpReq.valid := io.warpReqValid
  scheduler.io.warpReq.bits := 0.U.asTypeOf(scheduler.io.warpReq.bits)
  scheduler.io.warpReq.bits.wid := io.warpReqWid
  val WF_ID_WIDTH = log2Ceil(num_warp_in_a_block)
  val tagDispatch = (io.warpReqWgId << WF_ID_WIDTH).asUInt
  scheduler.io.warpReq.bits.CTAdata.dispatch2cu_wf_tag_dispatch := tagDispatch
  scheduler.io.warpReq.bits.CTAdata.dispatch2cu_wg_wf_count := io.warpReqWfCount

  scheduler.io.warp_control.valid := io.warpCtrlValid
  scheduler.io.warp_control.bits := 0.U.asTypeOf(scheduler.io.warp_control.bits)
  scheduler.io.warp_control.bits.ctrl.simt_stack_op := io.warpCtrlEndprg
  scheduler.io.warp_control.bits.ctrl.wid := io.warpCtrlWid

  scheduler.io.wg_id_tag := (io.warpCtrlWgId << WF_ID_WIDTH).asUInt

  scheduler.io.warpRsp.ready := true.B
  scheduler.io.pc_req.ready := true.B
  scheduler.io.pc_rsp.valid := false.B
  scheduler.io.pc_rsp.bits := 0.U.asTypeOf(scheduler.io.pc_rsp.bits)
  scheduler.io.branch.valid := false.B
  scheduler.io.branch.bits := 0.U.asTypeOf(scheduler.io.branch.bits)
  scheduler.io.issued_warp.valid := false.B
  scheduler.io.issued_warp.bits := 0.U
  scheduler.io.scoreboard_busy := 0.U
  scheduler.io.exe_busy := 0.U
  scheduler.io.pc_ibuffer_ready.foreach(_ := 0.U)
  scheduler.io.flushDCache.ready := false.B // Hold flush unacknowledged to observe need_flush

  io.flushDCacheReq := scheduler.io.flushDCache.valid
}

class WarpSchedulerSimultaneousAllocEndTest extends AnyFreeSpec with ChiselScalatestTester {

  "warp_scheduler preserves both allocation and termination when occurring in the same cycle for same WG" in {
    test(new WarpSchedulerSimultaneousHarness) { dut =>
      dut.reset.poke(true.B)
      dut.clock.step(2)
      dut.reset.poke(false.B)

      dut.io.warpReqValid.poke(false.B)
      dut.io.warpCtrlValid.poke(false.B)
      dut.io.warpCtrlEndprg.poke(false.B)
      dut.clock.step(1)

      val wgId = 0.U

      // Cycle 1: Dispatch Warp 0 for WG 0 (wfCount = 2)
      dut.io.warpReqValid.poke(true.B)
      dut.io.warpReqWid.poke(0.U)
      dut.io.warpReqWgId.poke(wgId)
      dut.io.warpReqWfCount.poke(2.U)
      dut.clock.step(1)

      // Cycle 2: SIMULTANEOUS EVENT for WG 0:
      // - Dispatch Warp 1 (warpReq)
      // - Warp 0 ends (warpCtrl with endprg = true)
      dut.io.warpReqValid.poke(true.B)
      dut.io.warpReqWid.poke(1.U)
      dut.io.warpReqWgId.poke(wgId)
      dut.io.warpReqWfCount.poke(2.U)

      dut.io.warpCtrlValid.poke(true.B)
      dut.io.warpCtrlEndprg.poke(true.B)
      dut.io.warpCtrlWid.poke(0.U)
      dut.io.warpCtrlWgId.poke(wgId)
      dut.clock.step(1)

      // Idle interfaces
      dut.io.warpReqValid.poke(false.B)
      dut.io.warpCtrlValid.poke(false.B)
      dut.clock.step(1)

      // CRITICAL ASSERTION:
      // WG 0 should NOT be flushing DCache yet, because Warp 1 is still active!
      // In the buggy implementation, Warp 0's clear clobbered Warp 1's allocation,
      // leaving warp_endprg_cnt(0) == 0, triggering an erroneous premature flush!
      assert(!dut.io.flushDCacheReq.peek().litToBoolean,
        "flushDCache MUST NOT be asserted prematurely while Warp 1 is still active in WG 0")

      // Now Warp 1 ends
      dut.io.warpCtrlValid.poke(true.B)
      dut.io.warpCtrlEndprg.poke(true.B)
      dut.io.warpCtrlWid.poke(1.U)
      dut.io.warpCtrlWgId.poke(wgId)
      dut.clock.step(1)
      dut.io.warpCtrlValid.poke(false.B)
      dut.clock.step(1)

      // Now all warps of WG 0 have completed: flushDCache MUST be asserted!
      assert(dut.io.flushDCacheReq.peek().litToBoolean,
        "flushDCache MUST be asserted now that both Warp 0 and Warp 1 have completed")

      println("[PASS] warp_scheduler simultaneous alloc & end race condition resolved cleanly on master")
    }
  }
}

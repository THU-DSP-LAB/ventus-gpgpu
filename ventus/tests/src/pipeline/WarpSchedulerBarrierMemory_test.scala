package play.warpscheduler

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import pipeline.warp_scheduler
import top.parameters._

class WarpSchedulerBarrierHarness extends Module {
  val io = IO(new Bundle {
    val valid = Input(Bool())
    val wid = Input(UInt(depth_warp.W))
    val endprg = Input(Bool())
    val lsuIdle = Input(UInt(num_warp.W))
    val ready = Output(Bool())
  })

  val scheduler = Module(new warp_scheduler)
  scheduler.io.pc_reset := false.B
  scheduler.io.warpReq.valid := false.B
  scheduler.io.warpReq.bits := 0.U.asTypeOf(scheduler.io.warpReq.bits)
  scheduler.io.warpRsp.ready := true.B
  scheduler.io.wg_id_tag := 0.U
  scheduler.io.pc_req.ready := true.B
  scheduler.io.pc_rsp.valid := false.B
  scheduler.io.pc_rsp.bits := 0.U.asTypeOf(scheduler.io.pc_rsp.bits)
  scheduler.io.branch.valid := false.B
  scheduler.io.branch.bits := 0.U.asTypeOf(scheduler.io.branch.bits)
  scheduler.io.warp_control.valid := io.valid
  scheduler.io.warp_control.bits := 0.U.asTypeOf(scheduler.io.warp_control.bits)
  scheduler.io.warp_control.bits.ctrl.barrier := true.B
  scheduler.io.warp_control.bits.ctrl.simt_stack_op := io.endprg
  scheduler.io.warp_control.bits.ctrl.wid := io.wid
  scheduler.io.issued_warp.valid := false.B
  scheduler.io.issued_warp.bits := 0.U
  scheduler.io.scoreboard_busy := 0.U
  scheduler.io.exe_busy := 0.U
  scheduler.io.lsu_idle := io.lsuIdle
  scheduler.io.pc_ibuffer_ready.foreach(_ := 0.U)
  scheduler.io.flushDCache.ready := true.B
  scheduler.io.flushDCacheDone := false.B

  io.ready := scheduler.io.warp_control.ready
}

class WarpSchedulerBarrierMemoryTest extends AnyFreeSpec with ChiselScalatestTester {
  private def initialize(dut: WarpSchedulerBarrierHarness): Unit = {
    dut.io.valid.poke(false.B)
    dut.io.wid.poke(0.U)
    dut.io.endprg.poke(false.B)
    dut.io.lsuIdle.poke(((BigInt(1) << num_warp) - 1).U)
    dut.reset.poke(true.B)
    dut.clock.step(2)
    dut.reset.poke(false.B)
  }

  "barrier waits for its warp's outstanding LSU requests" in {
    test(new WarpSchedulerBarrierHarness) { dut =>
      initialize(dut)

      val wid = 3
      dut.io.valid.poke(true.B)
      dut.io.endprg.poke(false.B)
      dut.io.wid.poke(wid.U)

      val allIdle = (BigInt(1) << num_warp) - 1
      dut.io.lsuIdle.poke((allIdle & ~(BigInt(1) << wid)).U)
      dut.io.ready.expect(false.B)

      dut.io.lsuIdle.poke((BigInt(1) << wid).U)
      dut.io.ready.expect(true.B)
    }
  }

  "endprg remains independent of the barrier memory-drain rule" in {
    test(new WarpSchedulerBarrierHarness) { dut =>
      initialize(dut)

      val wid = 3
      dut.io.valid.poke(true.B)
      dut.io.endprg.poke(true.B)
      dut.io.wid.poke(wid.U)
      dut.io.lsuIdle.poke(0.U)

      dut.io.ready.expect(true.B)
    }
  }
}

package pipeline

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

/** Regression for `vlrt; ...; vsrt` RAW handling.
  *
  * A local load writes a vector register.  A following local store must wait
  * for that vector writeback before the operand collector can issue it. */
class RtScoreboardTest extends AnyFreeSpec with ChiselScalatestTester {
  private def zero(data: Data): Unit = data match {
    case bool: Bool => bool.poke(false.B)
    case uint: UInt => uint.poke(0.U)
    case aggregate: Aggregate => aggregate.getElements.foreach(zero)
  }

  private def idle(dut: Scoreboard): Unit = {
    zero(dut.io.ibuffer_if_ctrl); zero(dut.io.if_ctrl)
    zero(dut.io.wb_v_ctrl); zero(dut.io.wb_x_ctrl)
    dut.io.if_fire.poke(false.B); dut.io.br_ctrl.poke(false.B)
    dut.io.fence_end.poke(false.B); dut.io.wb_v_fire.poke(false.B)
    dut.io.wb_x_fire.poke(false.B); dut.io.op_colV_in_fire.poke(false.B)
    dut.io.op_colV_out_fire.poke(false.B); dut.io.op_colX_in_fire.poke(false.B)
    dut.io.op_colX_out_fire.poke(false.B)
  }

  "holds an RT-local store until the producing RT-local load writes v8" in {
    test(new Scoreboard) { dut =>
      idle(dut)
      // The consumer is decoded as vsrt.w v8, field4.
      dut.io.ibuffer_if_ctrl.sel_alu2.poke(IDecode.A2_VRS2)
      dut.io.ibuffer_if_ctrl.reg_idx2.poke(8.U)
      dut.io.ibuffer_if_ctrl.isvec.poke(true.B)
      dut.io.delay.expect(false.B)

      // vlrt.w v8, field4 enters the machine and reserves v8.
      dut.io.if_ctrl.wvd.poke(true.B)
      dut.io.if_ctrl.reg_idxw.poke(8.U)
      dut.io.if_fire.poke(true.B)
      dut.clock.step()
      dut.io.if_fire.poke(false.B)
      dut.io.stall_read2.expect(true.B)
      dut.io.delay.expect(true.B)

      // Its vector writeback releases exactly that dependency.
      dut.io.wb_v_ctrl.wvd.poke(true.B)
      dut.io.wb_v_ctrl.reg_idxw.poke(8.U)
      dut.io.wb_v_fire.poke(true.B)
      dut.clock.step()
      dut.io.wb_v_fire.poke(false.B)
      dut.io.stall_read2.expect(false.B)
      dut.io.delay.expect(false.B)
    }
  }
}

package pipeline

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import top.parameters._
import pipeline.ALUOps._
import pipeline.IDecode._

class VmvXSTest extends AnyFreeSpec with ChiselScalatestTester {
  "InstrDecodeV2 (Issue #98)" - {
    "should decode VMV_X_S as a scalar-write instruction (isvec=false, wxd=true)" in {
      test(new InstrDecodeV2) { dut =>
        // funct6(010000) | vm(1) | vs2(00010) | rs1(00000) | funct3(010) | rd(00001) | opcode(1010111)
        val vmv_x_s_inst = "h422020d7".U(instLen.W)

        dut.io.inst(0).poke(vmv_x_s_inst)
        for (i <- 1 until num_fetch) {
          dut.io.inst(i).poke(0.U)
        }
        dut.io.inst_mask(0).poke(true.B)
        for (i <- 1 until num_fetch) {
          dut.io.inst_mask(i).poke(false.B)
        }
        dut.io.pc.poke(0.U)
        dut.io.wid.poke(0.U)
        dut.io.sm_id.poke(0.U)
        dut.io.flush_wid.valid.poke(false.B)
        dut.io.flush_wid.bits.poke(0.U)
        for (i <- 0 until num_warp) {
          dut.io.ibuffer_ready(i).poke(true.B)
        }

        dut.clock.step(1)

        val ctrl = dut.io.control(0)
        // Verify VMV_X_S routes to scalar execution pipeline
        assert(!ctrl.isvec.peek().litToBoolean, "VMV_X_S must have isvec=false so it executes via scalar ALU")
        assert(ctrl.wxd.peek().litToBoolean, "VMV_X_S must have wxd=true to write back to scalar register rd")
        assert(!ctrl.wvd.peek().litToBoolean, "VMV_X_S must have wvd=false")
        assert(ctrl.sel_alu2.peek().litValue == 2, "VMV_X_S must read vector operand A2_VRS2")
        assert(ctrl.alu_fn.peek().litValue == 8, "VMV_X_S alu_fn must be FN_A1ZERO")
      }
    }

    "should decode VFMV_F_S as a scalar-write instruction (isvec=false, wxd=true)" in {
      test(new InstrDecodeV2) { dut =>
        // funct6(010000) | vm(1) | vs2(00010) | rs1(00000) | funct3(001) | rd(00001) | opcode(1010111)
        // 010000 1 00010 00000 001 00001 1010111 = 0x422010d7
        val vfmv_f_s_inst = "h422010d7".U(instLen.W)

        dut.io.inst(0).poke(vfmv_f_s_inst)
        for (i <- 1 until num_fetch) {
          dut.io.inst(i).poke(0.U)
        }
        dut.io.inst_mask(0).poke(true.B)
        for (i <- 1 until num_fetch) {
          dut.io.inst_mask(i).poke(false.B)
        }
        dut.io.pc.poke(0.U)
        dut.io.wid.poke(0.U)
        dut.io.sm_id.poke(0.U)
        dut.io.flush_wid.valid.poke(false.B)
        dut.io.flush_wid.bits.poke(0.U)
        for (i <- 0 until num_warp) {
          dut.io.ibuffer_ready(i).poke(true.B)
        }

        dut.clock.step(1)

        val ctrl = dut.io.control(0)
        assert(!ctrl.isvec.peek().litToBoolean, "VFMV_F_S must have isvec=false")
        assert(ctrl.wxd.peek().litToBoolean, "VFMV_F_S must have wxd=true")
        assert(!ctrl.wvd.peek().litToBoolean, "VFMV_F_S must have wvd=false")
      }
    }
  }
}

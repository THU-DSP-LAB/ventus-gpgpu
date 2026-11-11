package pipeline

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import top.parameters._

class FpuRoundingModeTestModule extends Module {
  val io = IO(new Bundle {
    val ctrl = Input(new CtrlSigs)
    val csr_rm = Input(UInt(3.W))
    val fpu_rm = Output(UInt(3.W))
    val sfu_rm = Output(UInt(3.W))
  })

  // Exact logic from pipe.scala
  val fpu_inst_rm = io.ctrl.inst(14, 12)
  val fpu_static_rm = !io.ctrl.isvec && fpu_inst_rm =/= 7.U
  io.fpu_rm := Mux(io.ctrl.force_rm_rtz, RoundingMode.RTZ,
               Mux(fpu_static_rm, fpu_inst_rm, io.csr_rm))

  val sfu_inst_rm = io.ctrl.inst(14, 12)
  val sfu_static_rm = !io.ctrl.isvec && sfu_inst_rm =/= 7.U
  io.sfu_rm := Mux(sfu_static_rm, sfu_inst_rm, io.csr_rm)
}

class FpuRoundingModeTest extends AnyFreeSpec with ChiselScalatestTester {

  "FPU and SFU Static Rounding Mode (Issue #112 F3)" - {

    "Scalar FP instruction with static RUP (rm=3) must select RUP when CSR frm is RNE" in {
      test(new FpuRoundingModeTestModule) { dut =>
        dut.io.ctrl.isvec.poke(false.B) // scalar
        dut.io.ctrl.force_rm_rtz.poke(false.B)
        dut.io.ctrl.inst.poke((3 << 12).U) // rm = 3 (RUP)
        dut.io.csr_rm.poke(0.U) // CSR frm = 0 (RNE)

        dut.clock.step(1)
        assert(dut.io.fpu_rm.peek().litValue == 3, "fpu_rm must be 3 (RUP)")
        assert(dut.io.sfu_rm.peek().litValue == 3, "sfu_rm must be 3 (RUP)")
      }
    }

    "Scalar FP instruction with static RTZ (rm=1) must select RTZ" in {
      test(new FpuRoundingModeTestModule) { dut =>
        dut.io.ctrl.isvec.poke(false.B)
        dut.io.ctrl.force_rm_rtz.poke(false.B)
        dut.io.ctrl.inst.poke((1 << 12).U) // rm = 1 (RTZ)
        dut.io.csr_rm.poke(0.U) // CSR frm = 0 (RNE)

        dut.clock.step(1)
        assert(dut.io.fpu_rm.peek().litValue == 1, "fpu_rm must be 1 (RTZ)")
        assert(dut.io.sfu_rm.peek().litValue == 1, "sfu_rm must be 1 (RTZ)")
      }
    }

    "Scalar FP instruction with dynamic DYN (rm=7) must select CSR frm" in {
      test(new FpuRoundingModeTestModule) { dut =>
        dut.io.ctrl.isvec.poke(false.B)
        dut.io.ctrl.force_rm_rtz.poke(false.B)
        dut.io.ctrl.inst.poke((7 << 12).U) // rm = 7 (DYN)
        dut.io.csr_rm.poke(2.U) // CSR frm = 2 (RDN)

        dut.clock.step(1)
        assert(dut.io.fpu_rm.peek().litValue == 2, "fpu_rm must use CSR frm (RDN=2)")
        assert(dut.io.sfu_rm.peek().litValue == 2, "sfu_rm must use CSR frm (RDN=2)")
      }
    }

    "Vector instruction must use dynamic CSR frm regardless of inst(14, 12)" in {
      test(new FpuRoundingModeTestModule) { dut =>
        dut.io.ctrl.isvec.poke(true.B) // vector
        dut.io.ctrl.force_rm_rtz.poke(false.B)
        dut.io.ctrl.inst.poke((3 << 12).U) // inst(14, 12) = 3 (funct3 OPFVV, not static rm)
        dut.io.csr_rm.poke(0.U) // CSR frm = 0 (RNE)

        dut.clock.step(1)
        assert(dut.io.fpu_rm.peek().litValue == 0, "Vector instruction must use CSR frm")
        assert(dut.io.sfu_rm.peek().litValue == 0, "Vector instruction must use CSR frm")
      }
    }

    "force_rm_rtz must override static and dynamic rm to RTZ" in {
      test(new FpuRoundingModeTestModule) { dut =>
        dut.io.ctrl.isvec.poke(false.B)
        dut.io.ctrl.force_rm_rtz.poke(true.B)
        dut.io.ctrl.inst.poke((3 << 12).U) // rm = 3 (RUP)
        dut.io.csr_rm.poke(0.U)

        dut.clock.step(1)
        assert(dut.io.fpu_rm.peek().litValue == 1, "force_rm_rtz must force RTZ (1)")
      }
    }
  }
}

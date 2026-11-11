package pipeline

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import top.parameters._
import pipeline.IDecode._
import pipeline.Instructions._

class VectorMaskWritebackTest extends AnyFreeSpec with ChiselScalatestTester {
  "DecodeUnit enables writemask for vector mask instructions" in {
    test(new InstrDecodeV2()).withAnnotations(Seq()) { dut =>
      val maskInsts = Seq(
        ("vmand.mm",  0x6620a1d7L), // vmand.mm v3, v1, v2
        ("vmor.mm",   0x6a20a1d7L), // vmor.mm v3, v1, v2
        ("vmxor.mm",  0x6e20a1d7L), // vmxor.mm v3, v1, v2
        ("vmnand.mm", 0x7620a1d7L)  // vmnand.mm v3, v1, v2
      )

      dut.io.pc.poke(0x1000.U)
      dut.io.wid.poke(0.U)
      dut.io.sm_id.poke(0.U)
      dut.io.flush_wid.valid.poke(false.B)
      dut.io.flush_wid.bits.poke(0.U)
      for (w <- 0 until num_warp) {
        dut.io.ibuffer_ready(w).poke(true.B)
      }

      for ((name, instVal) <- maskInsts) {
        dut.io.inst(0).poke(instVal.U)
        dut.io.inst(1).poke(0.U)
        dut.io.inst_mask(0).poke(true.B)
        dut.io.inst_mask(1).poke(false.B)
        dut.clock.step(1)

        val ctrl = dut.io.control(0)
        assert(ctrl.writemask.peek().litToBoolean, s"[$name] writemask must be true!")
        assert(ctrl.wvd.peek().litToBoolean, s"[$name] wvd must be true!")
        assert(ctrl.readmask.peek().litToBoolean, s"[$name] readmask must be true!")
        println(s"[PASS] $name decoded with writemask=true, wvd=true")
      }
    }
  }

  "vALUexe correctly packs mask bits into lane 0 with only lane 0 write-enable" in {
    test(new vALUexe()).withAnnotations(Seq()) { dut =>
      dut.io.out.ready.poke(true.B)
      dut.io.out2simt_stack.ready.poke(true.B)

      // Test vmand: lane mask results packed into lane 0
      dut.io.in.valid.poke(true.B)
      dut.io.in.bits.ctrl.wid.poke(0.U)
      dut.io.in.bits.ctrl.reg_idxw.poke(3.U)
      dut.io.in.bits.ctrl.wvd.poke(true.B)
      dut.io.in.bits.ctrl.writemask.poke(true.B)
      dut.io.in.bits.ctrl.readmask.poke(false.B)
      dut.io.in.bits.ctrl.simt_stack.poke(false.B)
      dut.io.in.bits.ctrl.alu_fn.poke(FN_AND)
      dut.io.in.bits.ctrl.reverse.poke(false.B)

      // Input: lane i has in1(i)=1, in2(i) alternates (1 for even, 0 for odd)
      // active mask: all active
      for (i <- 0 until num_thread) {
        dut.io.in.bits.in1(i).poke(1.U)
        dut.io.in.bits.in2(i).poke((if (i % 2 == 0) 1 else 0).U)
        dut.io.in.bits.in3(i).poke(0.U)
        dut.io.in.bits.mask(i).poke(true.B)
      }

      dut.clock.step(1)

      assert(dut.io.out.valid.peek().litToBoolean, "vALU output must be valid")
      val wvdMask = dut.io.out.bits.wvd_mask
      assert(wvdMask(0).peek().litToBoolean, "Lane 0 write mask must be true for writemask")
      for (i <- 1 until num_thread) {
        assert(!wvdMask(i).peek().litToBoolean, s"Lane $i write mask must be false for writemask!")
      }

      val packedResult = dut.io.out.bits.wb_wvd_rd(0).peek().litValue
      // Even lanes are 1, odd lanes are 0 -> bit pattern 0x55555555 (for 32 threads)
      val expectedPattern = BigInt("55555555", 16) & ((BigInt(1) << num_thread) - 1)
      assert(packedResult == expectedPattern, f"Packed mask mismatch: expected 0x$expectedPattern%x, got 0x$packedResult%x")
      println(f"[PASS] vALUexe packed result 0x$packedResult%x matches expected 0x$expectedPattern%x, only lane 0 write enabled")
    }
  }
}

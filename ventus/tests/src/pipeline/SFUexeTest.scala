package pipeline

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import top.parameters._

class SFUexeTest extends AnyFreeSpec with ChiselScalatestTester {

  "SFUexe (Issue #112 F6 & F7)" - {

    "F6: SFUexe should snapshot rm at acceptance and isolate from foreign-warp arriving at input" in {
      test(new SFUexe) { dut =>
        // Warp 0 issues vector divide 1.0f / 3.0f with rm = RTZ (1)
        // In IEEE-754: 1.0f / 3.0f =
        //   RTZ (rm=1): 0x3eaaaaaa
        //   RNE (rm=0): 0x3eaaaaab
        dut.io.out_v.ready.poke(true.B)
        dut.io.out_x.ready.poke(true.B)

        dut.io.in.valid.poke(true.B)
        dut.io.in.bits.ctrl.wid.poke(0.U)
        dut.io.in.bits.ctrl.fp.poke(true.B)
        dut.io.in.bits.ctrl.isvec.poke(true.B)
        dut.io.in.bits.ctrl.wvd.poke(true.B)
        dut.io.in.bits.ctrl.wxd.poke(false.B)
        dut.io.in.bits.ctrl.reverse.poke(false.B)
        dut.io.in.bits.ctrl.alu_fn.poke(0.U) // FN_FDIV
        dut.io.in.bits.ctrl.inst.poke(0.U)

        for (t <- 0 until num_thread) {
          dut.io.in.bits.in1(t).poke("h3f800000".U) // 1.0f
          dut.io.in.bits.in2(t).poke("h40400000".U) // 3.0f
          dut.io.in.bits.in3(t).poke(0.U)
          dut.io.in.bits.mask(t).poke(true.B)
        }
        dut.io.rm.poke(1.U) // RTZ

        // Before fire, io.wid should be Warp 0
        assert(dut.io.wid.peek().litValue == 0, "io.wid should reflect input warp before fire")

        dut.clock.step(1) // io.in fires, Warp 0 divide is enqueued

        // In the next cycle, Warp 1 arrives at io.in and rm switches to RNE (0)
        dut.io.in.valid.poke(true.B)
        dut.io.in.bits.ctrl.wid.poke(1.U)
        dut.io.rm.poke(0.U) // foreign warp rm = RNE (0)

        // While SFU is busy processing Warp 0's lane groups, io.wid must report Warp 0
        var cycles = 0
        while (!dut.io.out_v.valid.peek().litToBoolean && cycles < 300) {
          assert(dut.io.wid.peek().litValue == 0, s"io.wid should remain 0 while executing Warp 0 (cycle $cycles)")
          dut.clock.step(1)
          cycles += 1
        }

        assert(dut.io.out_v.valid.peek().litToBoolean, "SFUexe out_v should become valid")
        assert(dut.io.out_v.bits.warp_id.peek().litValue == 0, "Warp ID should be 0")

        // Every lane across all lane groups must have used RTZ (0x3eaaaaaa), not RNE (0x3eaaaaab)
        val expectedRtz = BigInt("3eaaaaaa", 16)
        for (t <- 0 until num_thread) {
          val res = dut.io.out_v.bits.wb_wvd_rd(t).peek().litValue
          assert(res == expectedRtz, s"Lane $t: expected RTZ 0x${expectedRtz.toString(16)} but got 0x${res.toString(16)} (crosstalk detected)")
        }
      }
    }

    "F7: SFUexe should snapshot rm and isolate from younger CSR writes in the same warp" in {
      test(new SFUexe) { dut =>
        dut.io.out_v.ready.poke(true.B)
        dut.io.out_x.ready.poke(true.B)

        dut.io.in.valid.poke(true.B)
        dut.io.in.bits.ctrl.wid.poke(2.U) // Warp 2
        dut.io.in.bits.ctrl.fp.poke(true.B)
        dut.io.in.bits.ctrl.isvec.poke(true.B)
        dut.io.in.bits.ctrl.wvd.poke(true.B)
        dut.io.in.bits.ctrl.wxd.poke(false.B)
        dut.io.in.bits.ctrl.reverse.poke(false.B)
        dut.io.in.bits.ctrl.alu_fn.poke(0.U) // FN_FDIV
        dut.io.in.bits.ctrl.inst.poke(0.U)

        for (t <- 0 until num_thread) {
          dut.io.in.bits.in1(t).poke("h3f800000".U) // 1.0f
          dut.io.in.bits.in2(t).poke("h40400000".U) // 3.0f
          dut.io.in.bits.in3(t).poke(0.U)
          dut.io.in.bits.mask(t).poke(true.B)
        }
        dut.io.rm.poke(1.U) // RTZ

        dut.clock.step(1) // accepted

        // Warp 2 has no new instruction at io.in, but a younger csrwi retired changing frm to RNE
        dut.io.in.valid.poke(false.B)
        dut.io.rm.poke(0.U) // CSR frm updated to RNE mid-flight

        var cycles = 0
        while (!dut.io.out_v.valid.peek().litToBoolean && cycles < 300) {
          dut.clock.step(1)
          cycles += 1
        }

        assert(dut.io.out_v.valid.peek().litToBoolean, "SFUexe out_v should become valid")
        val expectedRtz = BigInt("3eaaaaaa", 16)
        for (t <- 0 until num_thread) {
          val res = dut.io.out_v.bits.wb_wvd_rd(t).peek().litValue
          assert(res == expectedRtz, s"Lane $t: expected RTZ 0x${expectedRtz.toString(16)} but got 0x${res.toString(16)} (younger CSR write corrupted divide)")
        }
      }
    }

    "SFUexe scalar divide respects static rm encoded in instruction" in {
      test(new SFUexe) { dut =>
        dut.io.out_v.ready.poke(true.B)
        dut.io.out_x.ready.poke(true.B)

        dut.io.in.valid.poke(true.B)
        dut.io.in.bits.ctrl.wid.poke(0.U)
        dut.io.in.bits.ctrl.fp.poke(true.B)
        dut.io.in.bits.ctrl.isvec.poke(false.B) // scalar
        dut.io.in.bits.ctrl.wvd.poke(false.B)
        dut.io.in.bits.ctrl.wxd.poke(true.B)
        dut.io.in.bits.ctrl.reverse.poke(false.B)
        dut.io.in.bits.ctrl.alu_fn.poke(0.U) // FN_FDIV
        // Encode rm = RTZ (1) in inst(14, 12): (1 << 12) = 0x1000
        dut.io.in.bits.ctrl.inst.poke((1 << 12).U)

        dut.io.in.bits.in1(0).poke("h3f800000".U) // 1.0f
        dut.io.in.bits.in2(0).poke("h40400000".U) // 3.0f
        dut.io.in.bits.mask(0).poke(true.B)

        dut.io.rm.poke(0.U) // Dynamic CSR frm is RNE (0)

        dut.clock.step(1)
        dut.io.in.valid.poke(false.B)

        var cycles = 0
        while (!dut.io.out_x.valid.peek().litToBoolean && cycles < 100) {
          dut.clock.step(1)
          cycles += 1
        }

        assert(dut.io.out_x.valid.peek().litToBoolean, "SFUexe out_x should become valid")
        val res = dut.io.out_x.bits.wb_wxd_rd.peek().litValue
        val expectedRtz = BigInt("3eaaaaaa", 16)
        assert(res == expectedRtz, s"Expected static RTZ 0x${expectedRtz.toString(16)} but got 0x${res.toString(16)}")
      }
    }
  }
}


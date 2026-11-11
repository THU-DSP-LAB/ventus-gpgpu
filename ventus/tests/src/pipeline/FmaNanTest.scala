package pipeline

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import FPUv2._

class FmaNanTest extends AnyFreeSpec with ChiselScalatestTester {

  "FMA NaN addend handling (Issue #112 F2)" - {

    "FMA with NaN addend c should produce NaN, not -inf" in {
      // 8 exp bits, 24 precision bits (IEEE-754 single precision)
      test(new FMA(8, 24)) { dut =>
        dut.io.in.valid.poke(true.B)
        dut.io.in.bits.op.poke(FPUOps.FN_FMADD) // a * b + c
        dut.io.in.bits.a.poke("h3f800000".U) // 1.0f
        dut.io.in.bits.b.poke("h40000000".U) // 2.0f
        dut.io.in.bits.c.poke("h7fc00000".U) // qNaN
        dut.io.in.bits.rm.poke(0.U) // RNE

        dut.clock.step(1)
        dut.io.in.valid.poke(false.B)

        // FMA has a 2-stage pipeline (s1 -> s2)
        var cycles = 0
        while (!dut.io.out.valid.peek().litToBoolean && cycles < 10) {
          dut.clock.step(1)
          cycles += 1
        }

        assert(dut.io.out.valid.peek().litToBoolean, "FMA out should be valid")
        val res = dut.io.out.bits.result.peek().litValue
        val isNaN = ((res >> 23) & 0xff) == 0xff && (res & 0x7fffff) != 0
        assert(isNaN, s"Expected NaN for 1.0 * 2.0 + NaN, but got 0x${res.toString(16)} (defect F2)")
      }
    }

    "FMA standard multiply-add 2.0 * 3.0 + 4.0 should return 10.0f" in {
      test(new FMA(8, 24)) { dut =>
        dut.io.in.valid.poke(true.B)
        dut.io.in.bits.op.poke(FPUOps.FN_FMADD) // a * b + c
        dut.io.in.bits.a.poke("h40000000".U) // 2.0f
        dut.io.in.bits.b.poke("h40400000".U) // 3.0f
        dut.io.in.bits.c.poke("h40800000".U) // 4.0f
        dut.io.in.bits.rm.poke(0.U) // RNE

        dut.clock.step(1)
        dut.io.in.valid.poke(false.B)

        var cycles = 0
        while (!dut.io.out.valid.peek().litToBoolean && cycles < 10) {
          dut.clock.step(1)
          cycles += 1
        }

        assert(dut.io.out.valid.peek().litToBoolean, "FMA out should be valid")
        val res = dut.io.out.bits.result.peek().litValue
        val expected = BigInt("41200000", 16) // 10.0f
        assert(res == expected, s"Expected 10.0f (0x41200000) but got 0x${res.toString(16)}")
      }
    }
  }
}

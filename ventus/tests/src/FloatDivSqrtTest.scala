package pipeline

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import pipeline.RoundingMode._

class FloatDivSqrtTest extends AnyFreeSpec with ChiselScalatestTester {

  "FloatDivSqrt (Issue #112 F1 & F5)" - {
    "fsqrt(-0.0) should return -0.0" in {
      test(new FloatDivSqrt) { dut =>
        dut.io.in.valid.poke(true.B)
        dut.io.in.bits.op.poke(1.U) // sqrt
        dut.io.in.bits.a.poke("h80000000".U) // -0.0f
        dut.io.in.bits.b.poke(0.U)
        dut.io.in.bits.c.poke(0.U)
        dut.io.in.bits.rm.poke(RNE)
        dut.io.out.ready.poke(true.B)

        dut.clock.step(1)
        dut.io.in.valid.poke(false.B)

        var cycles = 0
        while (!dut.io.out.valid.peek().litToBoolean && cycles < 100) {
          dut.clock.step(1)
          cycles += 1
        }
        val res = dut.io.out.bits.result.peek().litValue
        assert(res == BigInt("80000000", 16), s"Expected -0.0 (0x80000000) but got 0x${res.toString(16)}")
      }
    }

    "fsqrt(+0.0) should return +0.0" in {
      test(new FloatDivSqrt) { dut =>
        dut.io.in.valid.poke(true.B)
        dut.io.in.bits.op.poke(1.U) // sqrt
        dut.io.in.bits.a.poke(0.U) // +0.0f
        dut.io.in.bits.b.poke(0.U)
        dut.io.in.bits.c.poke(0.U)
        dut.io.in.bits.rm.poke(RNE)
        dut.io.out.ready.poke(true.B)

        dut.clock.step(1)
        dut.io.in.valid.poke(false.B)

        var cycles = 0
        while (!dut.io.out.valid.peek().litToBoolean && cycles < 100) {
          dut.clock.step(1)
          cycles += 1
        }
        val res = dut.io.out.bits.result.peek().litValue
        assert(res == BigInt(0), s"Expected +0.0 (0x0) but got 0x${res.toString(16)}")
      }
    }

    "fsqrt(4.0) should return 2.0" in {
      test(new FloatDivSqrt) { dut =>
        dut.io.in.valid.poke(true.B)
        dut.io.in.bits.op.poke(1.U) // sqrt
        dut.io.in.bits.a.poke("h40800000".U) // 4.0f
        dut.io.in.bits.b.poke(0.U)
        dut.io.in.bits.c.poke(0.U)
        dut.io.in.bits.rm.poke(RNE)
        dut.io.out.ready.poke(true.B)

        dut.clock.step(1)
        dut.io.in.valid.poke(false.B)

        var cycles = 0
        while (!dut.io.out.valid.peek().litToBoolean && cycles < 100) {
          dut.clock.step(1)
          cycles += 1
        }
        val res = dut.io.out.bits.result.peek().litValue
        assert(res == BigInt("40000000", 16), s"Expected 2.0 (0x40000000) but got 0x${res.toString(16)}")
      }
    }

    "fdiv overflow should saturate to +infinity with overflow flag set" in {
      test(new FloatDivSqrt) { dut =>
        dut.io.in.valid.poke(true.B)
        dut.io.in.bits.op.poke(0.U) // div
        dut.io.in.bits.a.poke("h7f000000".U) // ~1.7e38
        dut.io.in.bits.b.poke("h3f000000".U) // 0.5f
        dut.io.in.bits.c.poke(0.U)
        dut.io.in.bits.rm.poke(RNE)
        dut.io.out.ready.poke(true.B)

        dut.clock.step(1)
        dut.io.in.valid.poke(false.B)

        var cycles = 0
        while (!dut.io.out.valid.peek().litToBoolean && cycles < 100) {
          dut.clock.step(1)
          cycles += 1
        }
        val res = dut.io.out.bits.result.peek().litValue
        val ovf = dut.io.out.bits.flags.overflow.peek().litToBoolean
        assert(res == BigInt("7f800000", 16), s"Expected +inf (0x7f800000) on overflow but got 0x${res.toString(16)}")
        assert(ovf, "Expected overflow flag to be true")
      }
    }

    "fdiv 1.0 / 2.0 should return 0.5" in {
      test(new FloatDivSqrt) { dut =>
        dut.io.in.valid.poke(true.B)
        dut.io.in.bits.op.poke(0.U) // div
        dut.io.in.bits.a.poke("h3f800000".U) // 1.0f
        dut.io.in.bits.b.poke("h40000000".U) // 2.0f
        dut.io.in.bits.c.poke(0.U)
        dut.io.in.bits.rm.poke(RNE)
        dut.io.out.ready.poke(true.B)

        dut.clock.step(1)
        dut.io.in.valid.poke(false.B)

        var cycles = 0
        while (!dut.io.out.valid.peek().litToBoolean && cycles < 100) {
          dut.clock.step(1)
          cycles += 1
        }
        val res = dut.io.out.bits.result.peek().litValue
        assert(res == BigInt("3f000000", 16), s"Expected 0.5 (0x3f000000) but got 0x${res.toString(16)}")
      }
    }
  }
}

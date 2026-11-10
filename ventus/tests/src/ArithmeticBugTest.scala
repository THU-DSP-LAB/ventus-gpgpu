package pipeline

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import pipeline.ALUOps._

class ArithmeticBugTest extends AnyFreeSpec with ChiselScalatestTester {

  "IntDivMod (Issue #100)" - {
    "should divide 0xa36566c8 by 1 correctly (unsigned)" in {
      test(new IntDivMod(32)) { dut =>
        dut.io.in.valid.poke(true.B)
        dut.io.in.bits.a.poke("ha36566c8".U)
        dut.io.in.bits.d.poke(1.U)
        dut.io.in.bits.signed.poke(false.B)
        dut.io.out.ready.poke(true.B)

        dut.clock.step(1)
        dut.io.in.valid.poke(false.B)

        var cycles = 0
        while (!dut.io.out.valid.peek().litToBoolean && cycles < 100) {
          dut.clock.step(1)
          cycles += 1
        }
        val q = dut.io.out.bits.q.peek().litValue
        val r = dut.io.out.bits.r.peek().litValue
        assert(q == BigInt("a36566c8", 16), s"Expected 0xa36566c8 but got 0x${q.toString(16)}")
        assert(r == BigInt(0), s"Expected 0 but got 0x${r.toString(16)}")
      }
    }

    "should divide 0xa36566c8 by 3 correctly" in {
      test(new IntDivMod(32)) { dut =>
        dut.io.in.valid.poke(true.B)
        dut.io.in.bits.a.poke("ha36566c8".U)
        dut.io.in.bits.d.poke(3.U)
        dut.io.in.bits.signed.poke(false.B)
        dut.io.out.ready.poke(true.B)

        dut.clock.step(1)
        dut.io.in.valid.poke(false.B)

        var cycles = 0
        while (!dut.io.out.valid.peek().litToBoolean && cycles < 100) {
          dut.clock.step(1)
          cycles += 1
        }
        val q = dut.io.out.bits.q.peek().litValue
        val r = dut.io.out.bits.r.peek().litValue
        val expectedQ = BigInt("a36566c8", 16) / 3
        val expectedR = BigInt("a36566c8", 16) % 3
        assert(q == expectedQ, s"Expected 0x${expectedQ.toString(16)} but got 0x${q.toString(16)}")
        assert(r == expectedR, s"Expected 0x${expectedR.toString(16)} but got 0x${r.toString(16)}")
      }
    }

    "should handle signed division correctly (-10 / 2)" in {
      test(new IntDivMod(32)) { dut =>
        // -10 in 32-bit hex: 0xfffffff6
        dut.io.in.valid.poke(true.B)
        dut.io.in.bits.a.poke("hfffffff6".U)
        dut.io.in.bits.d.poke(2.U)
        dut.io.in.bits.signed.poke(true.B)
        dut.io.out.ready.poke(true.B)

        dut.clock.step(1)
        dut.io.in.valid.poke(false.B)

        while (!dut.io.out.valid.peek().litToBoolean) {
          dut.clock.step(1)
        }
        val q = dut.io.out.bits.q.peek().litValue
        val r = dut.io.out.bits.r.peek().litValue
        val expectedQ = (BigInt(-5) & BigInt("FFFFFFFF", 16))
        assert(q == expectedQ, s"Expected -5 (0x${expectedQ.toString(16)}) but got 0x${q.toString(16)}")
        assert(r == BigInt(0), s"Expected 0 but got 0x${r.toString(16)}")
      }
    }
  }

  "Multiplier (Issue #101)" - {
    "should return high 32 bits for MULHSU" in {
      test(new ArrayMultiplier(4, 32)) { dut =>
        dut.io.in.valid.poke(true.B)
        dut.io.in.bits.a.poke("habababab".U) // signed
        dut.io.in.bits.b.poke("ha36566c8".U) // unsigned
        dut.io.in.bits.c.poke(0.U)
        dut.io.in.bits.ctrl.alu_fn.poke(FN_MULHSU)
        dut.io.out.ready.poke(true.B)

        dut.clock.step(1)
        dut.io.in.valid.poke(false.B)

        while (!dut.io.out.valid.peek().litToBoolean) {
          dut.clock.step(1)
        }
        val res = dut.io.out.bits.result.peek().litValue
        // Expected high 32 bits: 0xca2ce72d (Spike verified)
        assert(res == BigInt("ca2ce72d", 16), s"Expected 0xca2ce72d but got 0x${res.toString(16)}")
      }
    }

    "should return high 32 bits for MULH and MULHU, and low 32 bits for MUL" in {
      test(new ArrayMultiplier(4, 32)) { dut =>
        dut.io.out.ready.poke(true.B)

        // Test MULHU
        dut.io.in.valid.poke(true.B)
        dut.io.in.bits.a.poke("habababab".U)
        dut.io.in.bits.b.poke("ha36566c8".U)
        dut.io.in.bits.c.poke(0.U)
        dut.io.in.bits.ctrl.alu_fn.poke(FN_MULHU)
        dut.clock.step(1)
        dut.io.in.valid.poke(false.B)

        while (!dut.io.out.valid.peek().litToBoolean) {
          dut.clock.step(1)
        }
        val resMulhu = dut.io.out.bits.result.peek().litValue
        assert(resMulhu == BigInt("6d924df5", 16), s"MULHU expected 0x6d924df5 but got 0x${resMulhu.toString(16)}")

        // Test MULH
        dut.clock.step(1)
        dut.io.in.valid.poke(true.B)
        dut.io.in.bits.a.poke("habababab".U)
        dut.io.in.bits.b.poke("ha36566c8".U)
        dut.io.in.bits.c.poke(0.U)
        dut.io.in.bits.ctrl.alu_fn.poke(FN_MULH)
        dut.clock.step(1)
        dut.io.in.valid.poke(false.B)

        while (!dut.io.out.valid.peek().litToBoolean) {
          dut.clock.step(1)
        }
        val resMulh = dut.io.out.bits.result.peek().litValue
        assert(resMulh == BigInt("1e813b82", 16), s"MULH expected 0x1e813b82 but got 0x${resMulh.toString(16)}")

        // Test MUL
        dut.clock.step(1)
        dut.io.in.valid.poke(true.B)
        dut.io.in.bits.a.poke("habababab".U)
        dut.io.in.bits.b.poke("ha36566c8".U)
        dut.io.in.bits.c.poke(0.U)
        dut.io.in.bits.ctrl.alu_fn.poke(FN_MUL)
        dut.clock.step(1)
        dut.io.in.valid.poke(false.B)

        while (!dut.io.out.valid.peek().litToBoolean) {
          dut.clock.step(1)
        }
        val resMul = dut.io.out.bits.result.peek().litValue
        assert(resMul == BigInt("1ffb3f98", 16), s"MUL expected 0x1ffb3f98 but got 0x${resMul.toString(16)}")
      }
    }
  }
}

package pipeline

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import java.lang.Float.{floatToIntBits, intBitsToFloat}

class ExpFp32Test extends AnyFreeSpec with ChiselScalatestTester {

  "EXPFP32 hardware exponential unit (Issue #99)" - {

    "computes exp(0.01) matching PoC and golden reference" in {
      test(new EXPFP32) { dut =>
        dut.io.in.valid.poke(true.B)
        // 0.01f in hex is 0x3c23d70a
        dut.io.in.bits.in.poke("h3c23d70a".U)
        dut.io.in.bits.rm.poke(0.U) // RNE
        dut.io.out.ready.poke(true.B)

        dut.clock.step(1)
        dut.io.in.valid.poke(false.B)

        var cycles = 0
        while (!dut.io.out.valid.peek().litToBoolean && cycles < 30) {
          dut.clock.step(1)
          cycles += 1
        }

        assert(dut.io.out.valid.peek().litToBoolean, "EXPFP32 out should become valid")
        val resBits = dut.io.out.bits.out.peek().litValue.toInt
        val resFloat = intBitsToFloat(resBits)
        val expected = Math.exp(0.01).toFloat // ~1.01005

        val relErr = Math.abs(resFloat - expected) / expected
        assert(relErr < 1e-4, s"Expected exp(0.01) ~= $expected, but got $resFloat (0x${resBits.toHexString}), relErr=$relErr")
      }
    }

    "computes exp(0.0) == 1.0f" in {
      test(new EXPFP32) { dut =>
        dut.io.in.valid.poke(true.B)
        dut.io.in.bits.in.poke(0.U) // 0.0f
        dut.io.in.bits.rm.poke(0.U)
        dut.io.out.ready.poke(true.B)

        dut.clock.step(1)
        dut.io.in.valid.poke(false.B)

        var cycles = 0
        while (!dut.io.out.valid.peek().litToBoolean && cycles < 30) {
          dut.clock.step(1)
          cycles += 1
        }

        assert(dut.io.out.valid.peek().litToBoolean, "EXPFP32 out should become valid")
        val resBits = dut.io.out.bits.out.peek().litValue.toInt
        val resFloat = intBitsToFloat(resBits)
        assert(Math.abs(resFloat - 1.0f) < 1e-5, s"Expected exp(0) == 1.0, got $resFloat")
      }
    }

    "computes exp(1.0) ~= 2.71828f" in {
      test(new EXPFP32) { dut =>
        dut.io.in.valid.poke(true.B)
        dut.io.in.bits.in.poke("h3f800000".U) // 1.0f
        dut.io.in.bits.rm.poke(0.U)
        dut.io.out.ready.poke(true.B)

        dut.clock.step(1)
        dut.io.in.valid.poke(false.B)

        var cycles = 0
        while (!dut.io.out.valid.peek().litToBoolean && cycles < 30) {
          dut.clock.step(1)
          cycles += 1
        }

        assert(dut.io.out.valid.peek().litToBoolean, "EXPFP32 out should become valid")
        val resBits = dut.io.out.bits.out.peek().litValue.toInt
        val resFloat = intBitsToFloat(resBits)
        val expected = Math.exp(1.0).toFloat // 2.7182818f
        val relErr = Math.abs(resFloat - expected) / expected
        assert(relErr < 1e-4, s"Expected exp(1.0) ~= $expected, but got $resFloat, relErr=$relErr")
      }
    }
  }
}

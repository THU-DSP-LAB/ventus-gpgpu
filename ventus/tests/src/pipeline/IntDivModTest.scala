package pipeline

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class IntDivModTest extends AnyFreeSpec with ChiselScalatestTester {
  "IntDivMod computes correct quotient and remainder across all signed and unsigned corner cases" in {
    test(new IntDivMod(32)).withAnnotations(Seq()) { dut =>
      dut.io.in.valid.poke(false.B)
      dut.io.out.ready.poke(true.B)
      dut.reset.poke(true.B)
      dut.clock.step(2)
      dut.reset.poke(false.B)
      dut.clock.step()

      def testDiv(a: BigInt, d: BigInt, signed: Boolean, expQ: BigInt, expR: BigInt): Unit = {
        dut.io.in.valid.poke(true.B)
        dut.io.in.bits.a.poke(a.U)
        dut.io.in.bits.d.poke(d.U)
        dut.io.in.bits.signed.poke(signed.B)

        var limit = 64
        while (!dut.io.in.ready.peek().litToBoolean && limit > 0) {
          dut.clock.step()
          limit -= 1
        }
        dut.clock.step()
        dut.io.in.valid.poke(false.B)

        limit = 64
        while (!dut.io.out.valid.peek().litToBoolean && limit > 0) {
          dut.clock.step()
          limit -= 1
        }

        val actQ = dut.io.out.bits.q.peek().litValue
        val actR = dut.io.out.bits.r.peek().litValue
        println(s"Test: a=0x${a.toString(16)}, d=0x${d.toString(16)}, signed=$signed -> Q=0x${actQ.toString(16)}, R=0x${actR.toString(16)}")
        assert(actQ == expQ, s"Mismatch in Q for a=0x${a.toString(16)}, d=0x${d.toString(16)}: got 0x${actQ.toString(16)}, expected 0x${expQ.toString(16)}")
        assert(actR == expR, s"Mismatch in R for a=0x${a.toString(16)}, d=0x${d.toString(16)}: got 0x${actR.toString(16)}, expected 0x${expR.toString(16)}")
        dut.clock.step()
      }

      // Corner case 1: Unsigned divide by 1 with MSB=1
      testDiv(BigInt("df3942c4", 16), BigInt(1), false, BigInt("df3942c4", 16), BigInt(0))

      // Corner case 2: Signed INT_MIN / -1 overflow (RISC-V spec: Q = INT_MIN, R = 0)
      testDiv(BigInt("80000000", 16), BigInt("ffffffff", 16), true, BigInt("80000000", 16), BigInt(0))

      // Corner case 3: Signed INT_MIN / 1 (RISC-V spec: Q = INT_MIN, R = 0)
      testDiv(BigInt("80000000", 16), BigInt(1), true, BigInt("80000000", 16), BigInt(0))

      // Corner case 4: Division by zero (RISC-V spec: Q = -1, R = a)
      testDiv(BigInt(100), BigInt(0), false, BigInt("ffffffff", 16), BigInt(100))

      // Corner case 5: Standard division
      testDiv(BigInt(100), BigInt(7), false, BigInt(14), BigInt(2))

      // Corner case 6: Signed negative division (-7 / 3 = -2, rem -1)
      testDiv(BigInt("fffffff9", 16), BigInt(3), true, BigInt("fffffffe", 16), BigInt("ffffffff", 16))

      // Corner case 7: Signed negative divisor (7 / -3 = -2, rem 1)
      testDiv(BigInt(7), BigInt("fffffffd", 16), true, BigInt("fffffffe", 16), BigInt(1))
    }
  }
}

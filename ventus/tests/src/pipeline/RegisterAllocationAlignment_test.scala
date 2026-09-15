package pipeline

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import top.parameters._

/** Replay the physical bank collision between warp0.v32 and warp1.v3.
  * The bank/row equations are those used by operandCollector.scala.
  */
class RegisterAllocationAlignmentTest extends AnyFreeSpec with ChiselScalatestTester {
  "padding a 33-register allocation isolates the next warp's origin register" in {
    test(new FloatRegFileBank).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      require(num_bank == 4, "this regression captures the four-bank launch ABI")
      assert((32 + 0) % num_bank == (3 + 1) % num_bank)
      val stackRow = 32 / num_bank
      val origin = BigInt("bf000000", 16)
      val stackBase = BigInt("820", 16)
      dut.io.rdwen.poke(false.B)
      dut.io.rsidx.poke(0.U)
      dut.io.rdidx.poke(0.U)
      for (lane <- 0 until num_thread) {
        dut.io.rd(lane).poke(0.U)
        dut.io.rdwmask(lane).poke(true.B)
      }
      dut.clock.step(2)

      def write(row: Int, value: BigInt): Unit = {
        dut.io.rdwen.poke(true.B)
        dut.io.rdidx.poke(row.U)
        for (lane <- 0 until num_thread) dut.io.rd(lane).poke(value.U)
        dut.clock.step()
        dut.io.rdwen.poke(false.B)
      }

      for (stride <- Seq(33, 36)) {
        val originRow = stride / num_bank + 3 / num_bank
        write(originRow, origin)
        write(stackRow, stackBase)
        dut.io.rsidx.poke(originRow.U)
        dut.clock.step(2)
        // 33 intentionally reproduces the bug; 36 must preserve every lane.
        val expected = if (stride == 33) stackBase else origin
        for (lane <- 0 until num_thread) dut.io.rs(lane).expect(expected.U)
      }
    }
  }
}

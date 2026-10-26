package pipeline

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import top.parameters._

/** Isolates the raygen hit-position add, without traversal or shader control flow. */
class FpuReflectionOriginTest extends AnyFreeSpec with ChiselScalatestTester {
  private def zero(data: Data): Unit = data match {
    case bool: Bool => bool.poke(false.B)
    case uint: UInt => uint.poke(0.U)
    case aggregate: Aggregate => aggregate.getElements.foreach(zero)
  }
  private def raw(x: Float): BigInt = BigInt(java.lang.Float.floatToRawIntBits(x)) & BigInt("ffffffff", 16)

  "raygen position adds preserve the camera origin including zero direction" in {
    test(new FPUexe).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      val pairs = Vector((0.0f, -0.5f), (-0.5f, 0.0f), (-0.0f, -0.5f),
        (-0.5f, -0.0f), (0.505f, -0.5f), (-0.5f, 0.505f),
        (0.0f, 2.0f), (2.0f, 0.0f), (0.000487f, -0.5f), (-0.5f, 0.000487f))
      zero(dut.io.in.bits)
      dut.io.in.valid.poke(false.B)
      dut.io.rm.poke(0.U)
      dut.io.out_v.ready.poke(true.B)
      dut.io.out_x.ready.poke(true.B)
      dut.clock.step(2)
      for (lane <- 0 until num_thread) {
        val (a, b) = pairs(lane % pairs.size)
        dut.io.in.bits.in1(lane).poke(raw(a).U)
        dut.io.in.bits.in2(lane).poke(raw(b).U)
        dut.io.in.bits.mask(lane).poke(true.B)
      }
      dut.io.in.bits.ctrl.fp.poke(true.B)
      dut.io.in.bits.ctrl.isvec.poke(true.B)
      dut.io.in.bits.ctrl.wvd.poke(true.B)
      dut.io.in.bits.ctrl.alu_fn.poke(IDecode.FN_FADD)
      dut.io.in.valid.poke(true.B)
      dut.io.in.ready.expect(true.B)
      dut.clock.step()
      dut.io.in.valid.poke(false.B)
      var cycles = 0
      while (!dut.io.out_v.valid.peek().litToBoolean && cycles < 100) {
        dut.clock.step()
        cycles += 1
      }
      assert(cycles < 100, "position add did not complete")
      for (lane <- 0 until num_thread) {
        val (a, b) = pairs(lane % pairs.size)
        dut.io.out_v.bits.wb_wvd_rd(lane).expect(raw(a + b).U)
      }
    }
  }
}

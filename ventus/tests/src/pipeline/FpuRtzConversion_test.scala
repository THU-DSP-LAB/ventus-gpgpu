package pipeline

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import top.parameters._

/** Covers the vector RTZ conversion emitted by texture-coordinate lowering. */
class FpuRtzConversionTest extends AnyFreeSpec with ChiselScalatestTester {
  private def zero(data: Data): Unit = data match {
    case bool: Bool => bool.poke(false.B)
    case uint: UInt => uint.poke(0.U)
    case aggregate: Aggregate => aggregate.getElements.foreach(zero)
  }

  private def raw(value: Float): BigInt = BigInt(java.lang.Float.floatToRawIntBits(value)) & BigInt("ffffffff", 16)
  private def signed32(value: Int): BigInt = BigInt(value) & BigInt("ffffffff", 16)

  "vector FPU converts sampler-like coordinates toward zero per active lane" in {
    test(new FPUexe).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      val values = Vector(-1024.75f, -1.999f, -1.0f, -0.999f, -0.5f, -0.001f,
        0.0f, 0.001f, 0.5f, 0.999f, 1.0f, 1.999f, 1023.999f, 1024.0f,
        1024.75f, 2047.5f)
      val active = Set(0, 1, 3, 4, 6, 8, 9, 11, 12, 14, 15)

      zero(dut.io.in.bits)
      dut.io.in.valid.poke(false.B)
      dut.io.rm.poke(1.U) // RoundingMode.RTZ
      dut.io.out_v.ready.poke(true.B)
      dut.io.out_x.ready.poke(true.B)
      dut.clock.step(2)

      values.zipWithIndex.foreach { case (value, lane) =>
        dut.io.in.bits.in1(lane).poke(raw(value).U)
        dut.io.in.bits.mask(lane).poke(active.contains(lane).B)
      }
      dut.io.in.bits.ctrl.fp.poke(true.B)
      dut.io.in.bits.ctrl.isvec.poke(true.B)
      dut.io.in.bits.ctrl.wvd.poke(true.B)
      dut.io.in.bits.ctrl.alu_fn.poke(IDecode.FN_F2I)
      dut.io.in.valid.poke(true.B)
      dut.io.in.ready.expect(true.B)
      dut.clock.step()
      dut.io.in.valid.poke(false.B)

      var cycles = 0
      while (!dut.io.out_v.valid.peek().litToBoolean && cycles < 100) {
        dut.clock.step()
        cycles += 1
      }
      assert(cycles < 100, "vector FPU RTZ conversion did not complete")
      active.foreach { lane =>
        dut.io.out_v.bits.wvd_mask(lane).expect(true.B)
        dut.io.out_v.bits.wb_wvd_rd(lane).expect(signed32(values(lane).toInt).U)
      }
      (0 until num_thread).filterNot(active).foreach { lane =>
        dut.io.out_v.bits.wvd_mask(lane).expect(false.B)
      }
    }
  }

  "vector FPU scales sampler coordinates per active lane" in {
    test(new FPUexe).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      val coordinates = Vector(-0.25f, -0.001f, 0.0f, 0.03125f, 0.067f, 0.125f,
        0.2499f, 0.333f, 0.5f, 0.667f, 0.75f, 0.875f, 0.999f, 1.0f, 1.25f, 2.0f)
      val active = Set(0, 1, 3, 4, 6, 7, 9, 10, 12, 14, 15)
      val extent = 1024.0f

      zero(dut.io.in.bits)
      dut.io.in.valid.poke(false.B)
      dut.io.rm.poke(0.U)
      dut.io.out_v.ready.poke(true.B)
      dut.io.out_x.ready.poke(true.B)
      dut.clock.step(2)

      coordinates.zipWithIndex.foreach { case (value, lane) =>
        dut.io.in.bits.in1(lane).poke(raw(value).U)
        dut.io.in.bits.in2(lane).poke(raw(extent).U)
        dut.io.in.bits.mask(lane).poke(active.contains(lane).B)
      }
      dut.io.in.bits.ctrl.fp.poke(true.B)
      dut.io.in.bits.ctrl.isvec.poke(true.B)
      dut.io.in.bits.ctrl.wvd.poke(true.B)
      dut.io.in.bits.ctrl.alu_fn.poke(IDecode.FN_FMUL)
      dut.io.in.valid.poke(true.B)
      dut.io.in.ready.expect(true.B)
      dut.clock.step()
      dut.io.in.valid.poke(false.B)

      var cycles = 0
      while (!dut.io.out_v.valid.peek().litToBoolean && cycles < 100) {
        dut.clock.step()
        cycles += 1
      }
      assert(cycles < 100, "vector FPU coordinate scaling did not complete")
      active.foreach { lane =>
        dut.io.out_v.bits.wvd_mask(lane).expect(true.B)
        dut.io.out_v.bits.wb_wvd_rd(lane).expect(raw(coordinates(lane) * extent).U)
      }
    }
  }

  "vector FPU performs sampler interpolation adds and subtracts per active lane" in {
    test(new FPUexe).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      val left = Vector(0.0f, 1.0f / 255.0f, 0.25f, 0.5f, 0.75f, 1.0f,
        -0.25f, 3.5f, 17.0f, 85.0f / 255.0f, 0.9f, -1.0f, 4.0f, 32.0f, 0.125f, 2.0f)
      val right = Vector(1.0f, 2.0f / 255.0f, 0.125f, 0.25f, 0.5f, 0.75f,
        0.25f, -1.5f, 3.0f, 34.0f / 255.0f, 0.1f, 2.0f, -4.0f, 16.0f, 0.5f, -2.0f)
      val active = Set(0, 1, 3, 4, 6, 7, 9, 10, 12, 14, 15)

      def execute(op: UInt, expected: (Float, Float) => Float): Unit = {
        zero(dut.io.in.bits)
        dut.io.in.valid.poke(false.B)
        dut.io.rm.poke(0.U)
        dut.io.out_v.ready.poke(true.B)
        dut.io.out_x.ready.poke(true.B)
        dut.clock.step(2)
        left.indices.foreach { lane =>
          dut.io.in.bits.in1(lane).poke(raw(left(lane)).U)
          dut.io.in.bits.in2(lane).poke(raw(right(lane)).U)
          dut.io.in.bits.mask(lane).poke(active.contains(lane).B)
        }
        dut.io.in.bits.ctrl.fp.poke(true.B)
        dut.io.in.bits.ctrl.isvec.poke(true.B)
        dut.io.in.bits.ctrl.wvd.poke(true.B)
        dut.io.in.bits.ctrl.alu_fn.poke(op)
        dut.io.in.valid.poke(true.B)
        dut.io.in.ready.expect(true.B)
        dut.clock.step()
        dut.io.in.valid.poke(false.B)
        var cycles = 0
        while (!dut.io.out_v.valid.peek().litToBoolean && cycles < 100) {
          dut.clock.step(); cycles += 1
        }
        assert(cycles < 100, s"vector FPU op $op did not complete")
        active.foreach { lane =>
          dut.io.out_v.bits.wb_wvd_rd(lane).expect(raw(expected(left(lane), right(lane))).U)
        }
        dut.clock.step()
      }

      execute(IDecode.FN_FSUB, _ - _)
      execute(IDecode.FN_FADD, _ + _)
    }
  }

}

package pipeline

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import top.parameters._

/** Exercises the SFU wrapper used by VREM, not merely IntDivMod in isolation.
  *
  * Scalar sampler-height DIVU must ignore the SIMT mask on the shared issue
  * path, including a zero mask when lane zero is inactive.
  *
  * Texture sampling supplies lane-varying signed coordinates to VREM_VX.  The
  * SFU executes a physical group at a time, so this covers different division
  * latencies, sparse masks, and the handoff to the vector writeback channel.
  */
class SfuRemainderTest extends AnyFreeSpec with ChiselScalatestTester {
  private def zero(data: Data): Unit = data match {
    case bool: Bool => bool.poke(false.B)
    case uint: UInt => uint.poke(0.U)
    case aggregate: Aggregate => aggregate.getElements.foreach(zero)
  }

  private def signed32(value: Int): BigInt = BigInt(value) & BigInt("ffffffff", 16)
  private def raw(value: Float): BigInt =
    BigInt(java.lang.Float.floatToRawIntBits(value)) & BigInt("ffffffff", 16)

  "scalar DIVU computes texture height even when SIMT lane zero is inactive" in {
    test(new SFUexe).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      zero(dut.io.in.bits)
      dut.io.in.valid.poke(false.B)
      dut.io.rm.poke(0.U)
      dut.io.out_v.ready.poke(true.B)
      dut.io.out_x.ready.poke(false.B)
      dut.clock.step(2)
      // Operand collection makes a scalar mask of 1, then the shared issue
      // path intersects it with SIMT. The texture hit mask excludes lane 0,
      // leaving mask=0 at the scalar SFU input, not the original hit mask.
      dut.io.in.bits.in1(0).poke((4 * 1024 * 1024).U)
      dut.io.in.bits.in2(0).poke(4096.U)
      dut.io.in.bits.ctrl.sfu.poke(true.B)
      dut.io.in.bits.ctrl.wxd.poke(true.B)
      dut.io.in.bits.ctrl.alu_fn.poke(IDecode.FN_DIVU)
      dut.io.in.bits.ctrl.reg_idxw.poke(7.U)
      dut.io.in.valid.poke(true.B)
      dut.io.in.ready.expect(true.B)
      dut.clock.step()
      dut.io.in.valid.poke(false.B)
      var cycles = 0
      while (!dut.io.out_x.valid.peek().litToBoolean && cycles < 100) {
        dut.clock.step(); cycles += 1
      }
      assert(cycles < 100, "scalar sampler-height DIVU did not complete")
      dut.io.out_x.bits.wb_wxd_rd.expect(1024.U)
      dut.io.out_x.bits.reg_idxw.expect(7.U)
      dut.io.out_v.valid.expect(false.B)
      dut.io.out_x.ready.poke(true.B)
      dut.clock.step()
    }
  }

  "scalar SFU ignores vector masks for integer and floating operations under backpressure" in {
    test(new SFUexe).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      zero(dut.io.in.bits)
      dut.io.in.valid.poke(false.B)
      dut.io.rm.poke(0.U)
      dut.io.out_v.ready.poke(true.B)
      dut.io.out_x.ready.poke(false.B)
      dut.clock.step(2)
      val operations = Seq(
        (IDecode.FN_DIVU, false, BigInt(4194304), BigInt(4096), BigInt(1024)),
        (IDecode.FN_REMU, false, BigInt(4194311), BigInt(4096), BigInt(7)),
        (IDecode.FN_DIV, false, signed32(-65), BigInt(4), signed32(-16)),
        (IDecode.FN_REM, false, signed32(-65), BigInt(4), signed32(-1)),
        (IDecode.FN_FDIV, true, raw(6.0f), raw(2.0f), raw(3.0f)))
      val masks = Seq(BigInt(0), BigInt(1), BigInt(1) << 31, BigInt("ff60", 16))
      for (mask <- masks; (fn, fp, a, b, expected) <- operations) {
        zero(dut.io.in.bits)
        dut.io.in.bits.in1(0).poke(a.U)
        dut.io.in.bits.in2(0).poke(b.U)
        for (lane <- 0 until num_thread)
          dut.io.in.bits.mask(lane).poke(mask.testBit(lane).B)
        dut.io.in.bits.ctrl.sfu.poke(true.B)
        dut.io.in.bits.ctrl.wxd.poke(true.B)
        dut.io.in.bits.ctrl.fp.poke(fp.B)
        dut.io.in.bits.ctrl.alu_fn.poke(fn)
        dut.io.in.bits.ctrl.reg_idxw.poke(7.U)
        dut.io.in.valid.poke(true.B)
        dut.io.in.ready.expect(true.B)
        dut.clock.step()
        dut.io.in.valid.poke(false.B)
        var cycles = 0
        while (!dut.io.out_x.valid.peek().litToBoolean && cycles < 100) {
          dut.clock.step(); cycles += 1
        }
        assert(cycles < 100, s"scalar SFU did not complete, mask=$mask")
        for (_ <- 0 until 3) {
          dut.io.out_x.valid.expect(true.B)
          dut.io.out_x.bits.wb_wxd_rd.expect(expected.U)
          dut.io.out_x.bits.reg_idxw.expect(7.U)
          dut.io.out_v.valid.expect(false.B)
          dut.clock.step()
        }
        dut.io.out_x.ready.poke(true.B)
        dut.clock.step()
        dut.io.out_x.valid.expect(false.B)
        dut.io.out_x.ready.poke(false.B)
      }
    }
  }

  "SFU preserves lane-varying signed VREM results across sparse groups" in {
    test(new SFUexe).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      val dividend = Vector(-1025, -1024, -1023, -1, 0, 1, 1023, 1024,
        1025, 2047, 2048, 2049, -2049, -2048, -2047, 511)
      val active = Set(0, 1, 2, 3, 5, 6, 8, 11, 12, 14, 15)
      val divisor = 1024

      zero(dut.io.in.bits)
      dut.io.in.valid.poke(false.B)
      dut.io.rm.poke(0.U)
      dut.io.out_v.ready.poke(true.B)
      dut.io.out_x.ready.poke(true.B)
      dut.clock.step(2)

      dividend.zipWithIndex.foreach { case (value, lane) =>
        dut.io.in.bits.in1(lane).poke(signed32(value).U)
        dut.io.in.bits.in2(lane).poke(signed32(divisor).U)
        dut.io.in.bits.mask(lane).poke(active.contains(lane).B)
      }
      dut.io.in.bits.ctrl.isvec.poke(true.B)
      dut.io.in.bits.ctrl.sfu.poke(true.B)
      dut.io.in.bits.ctrl.wvd.poke(true.B)
      dut.io.in.bits.ctrl.alu_fn.poke(IDecode.FN_REM)
      dut.io.in.valid.poke(true.B)
      dut.io.in.ready.expect(true.B)
      dut.clock.step()
      dut.io.in.valid.poke(false.B)

      var cycles = 0
      while (!dut.io.out_v.valid.peek().litToBoolean && cycles < 400) {
        dut.clock.step()
        cycles += 1
      }
      assert(cycles < 400, "SFU VREM did not complete")
      active.foreach { lane =>
        dut.io.out_v.bits.wvd_mask(lane).expect(true.B)
        dut.io.out_v.bits.wb_wvd_rd(lane).expect(signed32(dividend(lane) % divisor).U)
      }
      (0 until num_thread).filterNot(active).foreach { lane =>
        dut.io.out_v.bits.wvd_mask(lane).expect(false.B)
      }
    }
  }

  "SFU divides sampler RGBA bytes by 255 per active lane" in {
    test(new SFUexe).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      val bytes = Vector(0, 1, 18, 21, 22, 34, 51, 67,
        85, 118, 119, 130, 136, 144, 152, 255)
      val active = Set(0, 1, 3, 4, 6, 7, 9, 10, 12, 14, 15)

      zero(dut.io.in.bits)
      dut.io.in.valid.poke(false.B)
      dut.io.rm.poke(0.U)
      dut.io.out_v.ready.poke(true.B)
      dut.io.out_x.ready.poke(true.B)
      dut.clock.step(2)

      bytes.zipWithIndex.foreach { case (value, lane) =>
        dut.io.in.bits.in1(lane).poke(raw(value.toFloat).U)
        dut.io.in.bits.in2(lane).poke(raw(255.0f).U)
        dut.io.in.bits.mask(lane).poke(active.contains(lane).B)
      }
      dut.io.in.bits.ctrl.fp.poke(true.B)
      dut.io.in.bits.ctrl.isvec.poke(true.B)
      dut.io.in.bits.ctrl.sfu.poke(true.B)
      dut.io.in.bits.ctrl.wvd.poke(true.B)
      dut.io.in.bits.ctrl.alu_fn.poke(IDecode.FN_FDIV)
      dut.io.in.valid.poke(true.B)
      dut.io.in.ready.expect(true.B)
      dut.clock.step()
      dut.io.in.valid.poke(false.B)

      var cycles = 0
      while (!dut.io.out_v.valid.peek().litToBoolean && cycles < 500) {
        dut.clock.step()
        cycles += 1
      }
      assert(cycles < 500, "SFU RGBA normalization did not complete")
      active.foreach { lane =>
        dut.io.out_v.bits.wvd_mask(lane).expect(true.B)
        dut.io.out_v.bits.wb_wvd_rd(lane).expect(raw(bytes(lane).toFloat / 255.0f).U)
      }
    }
  }
}

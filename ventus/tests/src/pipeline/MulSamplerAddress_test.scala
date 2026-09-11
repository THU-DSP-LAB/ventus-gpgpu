package pipeline

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import top.parameters._

/** Validates the vector integer multiply used to form sampled-texture byte offsets. */
class MulSamplerAddressTest extends AnyFreeSpec with ChiselScalatestTester {
  private def zero(data: Data): Unit = data match {
    case bool: Bool => bool.poke(false.B)
    case uint: UInt => uint.poke(0.U)
    case aggregate: Aggregate => aggregate.getElements.foreach(zero)
  }

  "vmadd.vx uses the old destination as the row and preserves masked results under backpressure" in {
    test(new vMULv2(num_thread, num_lane)).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      val active = Set(0, 2, 7, 15, 16, 19, 25, 31)
      val rows = Vector.tabulate(num_thread)(lane => (lane * 131 + 767) % 1024)
      val columns = Vector.tabulate(num_thread)(lane => (lane * 67 + 769) % 1024)
      zero(dut.io.in.bits)
      dut.io.in.valid.poke(false.B)
      dut.io.out_v.ready.poke(false.B)
      dut.io.out_x.ready.poke(true.B)
      dut.clock.step(2)
      // vmadd.vx vd, rs1, vs2: vd = old(vd) * rs1 + vs2.
      // Operand collector supplies scalar stride, x*4, and old vector row.
      rows.indices.foreach { lane =>
        dut.io.in.bits.in1(lane).poke(4096.U)
        dut.io.in.bits.in2(lane).poke((columns(lane) * 4).U)
        dut.io.in.bits.in3(lane).poke(rows(lane).U)
        dut.io.in.bits.mask(lane).poke(active.contains(lane).B)
      }
      dut.io.in.bits.ctrl.mul.poke(true.B)
      dut.io.in.bits.ctrl.isvec.poke(true.B)
      dut.io.in.bits.ctrl.wvd.poke(true.B)
      dut.io.in.bits.ctrl.reverse.poke(true.B)
      dut.io.in.bits.ctrl.alu_fn.poke(IDecode.FN_MADD)
      dut.io.in.bits.ctrl.reg_idxw.poke(21.U)
      dut.io.in.valid.poke(true.B)
      dut.io.in.ready.expect(true.B)
      dut.clock.step()
      dut.io.in.valid.poke(false.B)
      var cycles = 0
      while (!dut.io.out_v.valid.peek().litToBoolean && cycles < 100) {
        dut.clock.step(); cycles += 1
      }
      assert(cycles < 100, "vmadd.vx sampler address did not complete")
      for (_ <- 0 until 5) {
        dut.io.out_v.valid.expect(true.B)
        dut.io.out_v.bits.reg_idxw.expect(21.U)
        rows.indices.foreach { lane =>
          dut.io.out_v.bits.wvd_mask(lane).expect(active.contains(lane).B)
          if (active.contains(lane))
            dut.io.out_v.bits.wb_wvd_rd(lane).expect((rows(lane) * 4096 + columns(lane) * 4).U)
        }
        dut.clock.step()
      }
      dut.io.out_v.ready.poke(true.B)
      dut.clock.step()
      dut.io.out_v.valid.expect(false.B)
    }
  }

  "vector multiplier preserves scattered sampler row offsets across both physical groups" in {
    test(new vMULv2(num_thread, num_lane)).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      val rows = Vector(0, 1, 2, 3, 7, 15, 31, 63, 127, 255, 511, 1023, 4, 8, 16, 32,
        5, 9, 17, 33, 65, 129, 257, 513, 6, 10, 18, 34, 66, 130, 258, 514)
      val strides = Vector(4, 16, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768,
        12, 20, 28, 36, 44, 52, 60, 68, 76, 84, 92, 100, 108, 116, 124, 132, 140, 148, 156, 164)
      val active = Set(0, 2, 3, 5, 7, 8, 10, 11, 14, 16, 17, 19, 21, 23, 24, 27, 29, 31)

      zero(dut.io.in.bits)
      dut.io.in.valid.poke(false.B)
      dut.io.out_v.ready.poke(true.B)
      dut.io.out_x.ready.poke(true.B)
      dut.clock.step(2)
      rows.indices.foreach { lane =>
        dut.io.in.bits.in1(lane).poke(rows(lane).U)
        dut.io.in.bits.in2(lane).poke(strides(lane).U)
        dut.io.in.bits.mask(lane).poke(active.contains(lane).B)
      }
      dut.io.in.bits.ctrl.mul.poke(true.B)
      dut.io.in.bits.ctrl.isvec.poke(true.B)
      dut.io.in.bits.ctrl.wvd.poke(true.B)
      dut.io.in.bits.ctrl.alu_fn.poke(IDecode.FN_MUL)
      dut.io.in.valid.poke(true.B)
      dut.io.in.ready.expect(true.B)
      dut.clock.step()
      dut.io.in.valid.poke(false.B)

      var cycles = 0
      while (!dut.io.out_v.valid.peek().litToBoolean && cycles < 100) {
        dut.clock.step(); cycles += 1
      }
      assert(cycles < 100, "vector sampler-offset multiply did not complete")
      active.foreach { lane =>
        dut.io.out_v.bits.wvd_mask(lane).expect(true.B)
        dut.io.out_v.bits.wb_wvd_rd(lane).expect((BigInt(rows(lane)) * strides(lane)).U)
      }
      (0 until num_thread).filterNot(active).foreach { lane =>
        dut.io.out_v.bits.wvd_mask(lane).expect(false.B)
      }
    }
  }
}

package rtcore

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import top.parameters._

class RtWorldRayContextsTest extends AnyFreeSpec with ChiselScalatestTester {
  "fills and consumes one raw Box4 world-ray context" in {
    test(new RtWorldRayContexts) { dut =>
      dut.io.clear.poke(false.B)
      dut.io.warpId.poke(0.U)
      dut.io.prefetch.valid.poke(false.B)
      dut.io.prefetch.bits.laneId.poke(0.U)
      dut.io.prefetch.bits.purpose.poke(RtWorldRayPurpose.box)
      for (bank <- 0 until RtCoreSchedulerParameters.boxUnitCount) {
        dut.io.lookupLane(bank).poke(0.U)
        dut.io.setupLookupLane(bank).poke(0.U)
        dut.io.consume(bank).poke(false.B)
        dut.io.setupConsume(bank).poke(false.B)
        dut.io.request(bank).ready.poke(true.B)
        dut.io.response(bank).valid.poke(false.B)
        dut.io.response(bank).bits.readData.poke(0.U)
      }
      dut.clock.step()

      dut.io.prefetch.valid.poke(true.B)
      dut.io.prefetch.ready.expect(true.B)
      dut.clock.step()
      dut.io.prefetch.valid.poke(false.B)

      for (field <- 0 until 11) {
        dut.io.request(0).valid.expect(true.B)
        dut.io.request(0).bits.laneId.expect(0.U)
        dut.io.request(0).bits.fieldId.expect((RtAbiLayout.SlotOriginX + field).U)
        dut.clock.step()
        dut.io.response(0).valid.poke(true.B)
        dut.io.response(0).bits.readData.poke((0x100 + field).U)
        dut.io.response(0).ready.expect(true.B)
        dut.clock.step()
        dut.io.response(0).valid.poke(false.B)
      }

      val ray = dut.io.result(0).ray
      dut.io.result(0).valid.expect(true.B)
      ray.origin.x.expect(0x100.U); ray.origin.y.expect(0x101.U); ray.origin.z.expect(0x102.U)
      ray.originalTmin.expect(0x103.U)
      ray.dir.x.expect(0x104.U); ray.dir.y.expect(0x105.U); ray.dir.z.expect(0x106.U)
      ray.extent.expect(0x107.U)
      ray.inv.x.expect(0x108.U); ray.inv.y.expect(0x109.U); ray.inv.z.expect(0x10a.U)

      dut.io.consume(0).poke(true.B)
      dut.clock.step()
      dut.io.consume(0).poke(false.B)
      dut.io.result(0).valid.expect(false.B)
    }
  }
}

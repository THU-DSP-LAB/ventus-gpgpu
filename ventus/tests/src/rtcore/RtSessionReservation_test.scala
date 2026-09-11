package rtcore

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import top.parameters._

class RtSessionReservationTest extends AnyFreeSpec with ChiselScalatestTester {
  "foreign traversals wait outside the shared queue until release acknowledgement" in {
    test(new RtSessionReservation) { dut =>
      dut.io.issued.valid.poke(false.B)
      dut.io.released.valid.poke(false.B)
      dut.io.releaseIssued.valid.poke(false.B)
      dut.io.releaseIssued.bits.poke(0.U)
      dut.io.issued.bits.poke(0.U)
      dut.io.released.bits.poke(0.U)
      dut.io.allowed.foreach(_.expect(true.B))
      for (owner <- Seq(0, 1, 0)) {
        dut.io.issued.bits.poke(owner.U)
        dut.io.issued.valid.poke(true.B)
        dut.clock.step()
        dut.io.issued.valid.poke(false.B)
        // Completion and callback latency must not release the reservation.
        for (_ <- 0 until 20) {
          for (warp <- 0 until num_warp) dut.io.allowed(warp).expect((warp == owner).B)
          dut.clock.step()
        }
        // The owner can issue nested traces while the other warp waits.
        dut.io.issued.valid.poke(true.B)
        dut.clock.step()
        dut.io.issued.valid.poke(false.B)
        dut.io.owner.expect(owner.U)
        dut.io.releaseIssued.bits.poke(owner.U)
        dut.io.releaseIssued.valid.poke(true.B)
        dut.clock.step()
        dut.io.releaseIssued.valid.poke(false.B)
        // A queued next session from the same warp must also wait for ack.
        dut.io.allowed.foreach(_.expect(false.B))
        dut.clock.step(5)
        dut.io.released.bits.poke(owner.U)
        dut.io.released.valid.poke(true.B)
        dut.clock.step()
        dut.io.released.valid.poke(false.B)
        dut.io.allowed.foreach(_.expect(true.B))
        dut.io.reserved.expect(false.B)
      }
    }
  }
}

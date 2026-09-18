package rtcore

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import top.parameters._

class RtSessionReservationTest extends AnyFreeSpec with ChiselScalatestTester {
  "foreign traversals wait outside the shared queue until release acknowledgement" in {
    test(new RtSessionReservation) { dut =>
      dut.io.issued.valid.poke(false.B)
      dut.io.traverseResolved.valid.poke(false.B)
      dut.io.traverseResolved.bits.warpId.poke(0.U)
      dut.io.traverseResolved.bits.wasEmpty.poke(false.B)
      dut.io.released.valid.poke(false.B)
      dut.io.completed.valid.poke(false.B)
      dut.io.completed.bits.warpId.poke(0.U)
      dut.io.completed.bits.contextReleased.poke(false.B)
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
        // Admission is provisional until RT issue resolves the effective mask.
        dut.io.allowed.foreach(_.expect(false.B))
        dut.io.traverseResolved.bits.warpId.poke(owner.U)
        dut.io.traverseResolved.valid.poke(true.B)
        dut.clock.step()
        dut.io.traverseResolved.valid.poke(false.B)
        // No traversal, including the owner's, enters before actual writeback.
        for (_ <- 0 until 20) {
          dut.io.allowed.foreach(_.expect(false.B))
          dut.clock.step()
        }
        dut.io.completed.bits.warpId.poke(owner.U)
        dut.io.completed.valid.poke(true.B)
        dut.clock.step()
        dut.io.completed.valid.poke(false.B)
        for (warp <- 0 until num_warp) dut.io.allowed(warp).expect((warp == owner).B)
        // The owner can issue nested traces while the other warp waits.
        dut.io.issued.valid.poke(true.B)
        dut.clock.step()
        dut.io.issued.valid.poke(false.B)
        dut.io.owner.expect(owner.U)
        dut.io.traverseResolved.valid.poke(true.B)
        dut.clock.step()
        dut.io.traverseResolved.valid.poke(false.B)
        dut.io.completed.valid.poke(true.B)
        dut.clock.step()
        dut.io.completed.valid.poke(false.B)
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

  "context-releasing writeback permits A B A handoff without software release" in {
    test(new RtSessionReservation) { dut =>
      dut.io.issued.valid.poke(false.B)
      dut.io.traverseResolved.valid.poke(false.B)
      dut.io.releaseIssued.valid.poke(false.B)
      dut.io.released.valid.poke(false.B)
      dut.io.completed.valid.poke(false.B)
      for (owner <- Seq(0, 1, 0)) {
        dut.io.allowed.foreach(_.expect(true.B))
        dut.io.issued.bits.poke(owner.U)
        dut.io.issued.valid.poke(true.B)
        dut.clock.step()
        dut.io.issued.valid.poke(false.B)
        dut.io.traverseResolved.bits.warpId.poke(owner.U)
        dut.io.traverseResolved.bits.wasEmpty.poke(false.B)
        dut.io.traverseResolved.valid.poke(true.B)
        dut.clock.step()
        dut.io.traverseResolved.valid.poke(false.B)
        dut.clock.step(10)
        dut.io.allowed.foreach(_.expect(false.B))
        dut.io.reserved.expect(true.B)
        dut.io.completed.bits.warpId.poke(owner.U)
        dut.io.completed.bits.contextReleased.poke(true.B)
        dut.io.completed.valid.poke(true.B)
        dut.clock.step()
        dut.io.completed.valid.poke(false.B)
        dut.io.reserved.expect(false.B)
      }
      dut.io.allowed.foreach(_.expect(true.B))
    }
  }

  "empty traversal cancels only a provisional reservation" in {
    test(new RtSessionReservation) { dut =>
      dut.io.issued.valid.poke(false.B)
      dut.io.traverseResolved.valid.poke(false.B)
      dut.io.releaseIssued.valid.poke(false.B)
      dut.io.released.valid.poke(false.B)
      dut.io.completed.valid.poke(false.B)
      def traverse(empty: Boolean): Unit = {
        dut.io.issued.bits.poke(0.U)
        dut.io.issued.valid.poke(true.B)
        dut.clock.step()
        dut.io.issued.valid.poke(false.B)
        dut.io.traverseResolved.bits.warpId.poke(0.U)
        dut.io.traverseResolved.bits.wasEmpty.poke(empty.B)
        dut.io.traverseResolved.valid.poke(true.B)
        dut.clock.step()
        dut.io.traverseResolved.valid.poke(false.B)
      }
      traverse(true)
      dut.io.reserved.expect(false.B)
      dut.io.allowed.foreach(_.expect(true.B))
      traverse(false)
      dut.io.completed.bits.warpId.poke(0.U)
      dut.io.completed.bits.contextReleased.poke(false.B)
      dut.io.completed.valid.poke(true.B)
      dut.clock.step()
      dut.io.completed.valid.poke(false.B)
      traverse(true)
      dut.io.reserved.expect(true.B)
      for (warp <- 0 until num_warp) dut.io.allowed(warp).expect((warp == 0).B)
    }
  }
}

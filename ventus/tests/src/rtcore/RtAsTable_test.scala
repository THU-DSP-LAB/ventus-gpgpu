package rtcore

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class RtAsTableTest extends AnyFreeSpec with ChiselScalatestTester {
  "retains lines, merges in-flight waiters, replaces backwards and clears safely" in {
    test(new RtAsTable).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      dut.io.acquire.valid.poke(false.B)
      dut.io.acquire.bits.lineAddress.poke(0.U)
      dut.io.acquire.bits.wordMask.poke("hffffffff".U)
      dut.io.acquire.bits.is64B.poke(false.B)
      dut.io.acquire.bits.waiter.warpId.poke(0.U)
      dut.io.acquire.bits.waiter.laneId.poke(0.U)
      dut.io.acquire.bits.waiter.nodeKind.poke(RtVtasNodeKind.box4)
      dut.io.acquire.bits.waiter.objectRay.poke(false.B)
      dut.io.candidate.ready.poke(false.B)
      dut.io.lineRead.foreach(_.entryId.poke(0.U))
      dut.io.consumeLanes.poke(0.U)
      dut.io.injection.valid.poke(false.B)
      dut.io.injection.bits.entry.poke(0.U)
      dut.io.injection.bits.fault.poke(false.B)
      dut.io.injection.bits.data.foreach(_.poke(0.U))
      dut.io.clear.valid.poke(false.B)
      dut.io.clear.bits.poke(true.B)

      def acquire(address: Int, lane: Int, hit: Boolean = false, merge: Boolean = false): Unit = {
        dut.io.acquire.bits.lineAddress.poke(address.U)
        dut.io.acquire.bits.waiter.laneId.poke(lane.U)
        dut.io.acquire.valid.poke(true.B)
        dut.io.acquire.ready.expect(true.B)
        dut.io.residentHit.expect(hit.B)
        dut.io.pendingMerge.expect(merge.B)
        dut.clock.step()
        dut.io.acquire.valid.poke(false.B)
      }
      def dispatch(index: Int): Unit = {
        dut.io.candidate.valid.expect(true.B)
        dut.io.candidate.bits.entryId.expect(index.U)
        dut.io.candidate.ready.poke(true.B)
        dut.clock.step()
        dut.io.candidate.ready.poke(false.B)
        dut.io.candidate.valid.expect(false.B) // no duplicate while in flight
      }
      def inject(index: Int): Unit = {
        dut.io.injection.bits.entry.poke(index.U)
        dut.io.injection.bits.data.zipWithIndex.foreach { case (word, i) => word.poke((0x1000 + i).U) }
        dut.io.injection.valid.poke(true.B)
        dut.io.injection.ready.expect(true.B)
        dut.clock.step()
        dut.io.injection.valid.poke(false.B)
      }
      def serve(lane: Int): Unit = {
        dut.io.readyMeta(lane).valid.expect(true.B)
        dut.io.readyMeta(lane).bits.nodeKind.expect(RtVtasNodeKind.box4)
        dut.io.lineRead(0).entryId.poke(dut.io.readyMeta(lane).bits.entryId.peek())
        dut.io.lineRead(0).data.head.expect("h1000".U)
        dut.io.consumeLanes.poke((BigInt(1) << lane).U)
        dut.clock.step()
        dut.io.consumeLanes.poke(0.U)
      }
      def clear(): Unit = {
        dut.io.clear.valid.poke(true.B)
        dut.io.clear.ready.expect(true.B)
        dut.io.acquire.ready.expect(false.B)
        dut.clock.step()
        dut.io.clear.valid.poke(false.B)
        dut.io.idle.expect(true.B)
      }

      acquire(0x100, 0)
      dispatch(0)
      dut.io.clear.ready.expect(false.B)
      for (lane <- 1 until 32) acquire(0x100, lane, merge = true)
      dut.io.acquire.valid.poke(true.B)
      dut.io.acquire.ready.expect(false.B) // full matching line cannot be duplicated
      dut.io.acquire.valid.poke(false.B)
      inject(0)
      // The scheduler may dispatch multiple fixed IU groups from the same
      // returned line.  Both waiter slots must clear together, not just the
      // last dynamically-indexed write in the lane loop.
      dut.io.readyMeta(0).valid.expect(true.B)
      dut.io.readyMeta(0).bits.nodeKind.expect(RtVtasNodeKind.box4)
      dut.io.readyMeta(17).valid.expect(true.B)
      dut.io.readyMeta(17).bits.nodeKind.expect(RtVtasNodeKind.box4)
      dut.io.consumeLanes.poke((BigInt(1) << 0 | BigInt(1) << 17).U)
      dut.clock.step()
      dut.io.consumeLanes.poke(0.U)
      dut.io.readyMeta(0).valid.expect(false.B)
      dut.io.readyMeta(17).valid.expect(false.B)
      for (lane <- (1 until 32).filter(_ != 17)) serve(lane)
      dut.io.idle.expect(true.B) // retained cache lines have no outstanding work
      acquire(0x100, 0, hit = true)
      // Same-cycle acquire and last waiter service preserve the new waiter.
      dut.io.consumeLanes.poke(1.U)
      acquire(0x100, 1, hit = true)
      dut.io.consumeLanes.poke(0.U)
      serve(1)
      dut.io.candidate.valid.expect(false.B)
      clear()

      for (index <- 0 until 32) {
        acquire(0x100 + index * 128, 0)
        dispatch(index)
        inject(index)
        serve(0)
      }
      // Protect slot 31 while it has an unserved waiter: replacement skips it.
      acquire(0x100 + 31 * 128, 1, hit = true)
      acquire(0x2000, 0)
      dispatch(30)
      inject(30)
      serve(0)
      serve(1)
      acquire(0x2080, 0)
      dispatch(29)
      inject(29)
      serve(0)
      clear()
      // Clear resets both cold-fill and reverse-replacement cursors.
      for (index <- 0 until 32) {
        acquire(0x100 + index * 128, 0)
        dispatch(index)
        inject(index)
        serve(0)
      }
      for (index <- 31 to 0 by -1) {
        acquire(0x3000 + (31 - index) * 128, 0)
        dispatch(index)
        inject(index)
        serve(0)
      }
      clear()
      // A full table of pending refills must backpressure a different address.
      for (index <- 0 until 32) acquire(0x100 + index * 128, index)
      dut.io.acquire.bits.lineAddress.poke(0x4000.U)
      dut.io.acquire.valid.poke(true.B)
      dut.io.acquire.ready.expect(false.B)
      dut.io.clear.ready.expect(false.B)
      dut.io.acquire.valid.poke(false.B)
      for (index <- 0 until 32) {
        dut.io.candidate.bits.entryId.expect(index.U)
        dut.io.candidate.ready.poke(true.B)
        dut.clock.step()
        dut.io.candidate.ready.poke(false.B)
        inject(index)
        serve(index)
      }
      clear()
      // 64B and 128B representations are distinct cache keys.
      acquire(0x400, 0)
      dispatch(0)
      inject(0)
      serve(0)
      dut.io.acquire.bits.is64B.poke(true.B)
      dut.io.acquire.bits.wordMask.poke("hffff".U)
      acquire(0x400, 1)
      dispatch(1)
      inject(1)
      serve(1)
      dut.io.acquire.bits.is64B.poke(false.B)
      dut.io.acquire.bits.wordMask.poke("hffffffff".U)
      acquire(0x400, 0, hit = true)
      serve(0)
      clear()
    }
  }
}

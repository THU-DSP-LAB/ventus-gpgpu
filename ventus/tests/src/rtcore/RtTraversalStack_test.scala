package rtcore

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class RtTraversalStackTest extends AnyFreeSpec with ChiselScalatestTester {
  private def driveIdle(dut: RtTraversalStack): Unit = {
    dut.io.init.valid.poke(false.B)
    dut.io.push.valid.poke(false.B)
    dut.io.pop.valid.poke(false.B)
    dut.io.clear.valid.poke(false.B)
    dut.io.init.bits.laneMask.poke(0.U)
    dut.io.init.bits.rootNodeRef.foreach(_.poke(0.U))
    dut.io.push.bits.laneMask.poke(0.U)
    dut.io.push.bits.nodeRef.foreach(_.poke(0.U))
    dut.io.pop.bits.laneMask.poke(0.U)
    dut.io.clear.bits.laneMask.poke(0.U)
  }

  private def init(
      dut: RtTraversalStack,
      mask: BigInt,
      roots: Map[Int, BigInt]
  ): Unit = {
    dut.io.init.bits.laneMask.poke(mask.U)
    dut.io.init.bits.rootNodeRef.zipWithIndex.foreach { case (ref, lane) =>
      ref.poke(roots.getOrElse(lane, BigInt(0)).U)
    }
    dut.io.init.valid.poke(true.B)
    dut.io.init.ready.expect(true.B)
    dut.clock.step()
    dut.io.init.valid.poke(false.B)
  }

  private def push(
      dut: RtTraversalStack,
      mask: BigInt,
      values: Map[Int, BigInt]
  ): Unit = {
    dut.io.push.bits.laneMask.poke(mask.U)
    dut.io.push.bits.nodeRef.zipWithIndex.foreach { case (ref, lane) =>
      ref.poke(values.getOrElse(lane, BigInt(0)).U)
    }
    dut.io.push.valid.poke(true.B)
    dut.io.push.ready.expect(true.B)
    dut.clock.step()
    dut.io.push.valid.poke(false.B)
  }

  private def pop(
      dut: RtTraversalStack,
      mask: BigInt,
      expected: Map[Int, BigInt]
  ): Unit = {
    dut.io.pop.bits.laneMask.poke(mask.U)
    dut.io.pop.valid.poke(true.B)
    dut.io.pop.ready.expect(true.B)
    expected.foreach { case (lane, value) => dut.io.popNodeRef(lane).expect(value.U) }
    dut.clock.step()
    dut.io.pop.valid.poke(false.B)
  }

  private def clear(dut: RtTraversalStack, mask: BigInt): Unit = {
    dut.io.clear.bits.laneMask.poke(mask.U)
    dut.io.clear.valid.poke(true.B)
    dut.io.clear.ready.expect(true.B)
    dut.clock.step()
    dut.io.clear.valid.poke(false.B)
  }

  "lane-major stacks preserve masked LIFO state for one resident warp" in {
    test(new RtTraversalStack(stackDepth = 4)) { dut =>
      driveIdle(dut)
      val lanes01 = BigInt(3)
      init(dut, lanes01, Map(0 -> 0x10, 1 -> 0x20))
      dut.io.laneLive.expect(lanes01.U)
      dut.io.laneEmpty.expect(0.U)

      push(dut, lanes01, Map(0 -> 0x11, 1 -> 0x21))
      push(dut, lanes01, Map(0 -> 0x12, 1 -> 0x22))
      pop(dut, lanes01, Map(0 -> 0x12, 1 -> 0x22))
      pop(dut, lanes01, Map(0 -> 0x11, 1 -> 0x21))
      pop(dut, lanes01, Map(0 -> 0x10, 1 -> 0x20))
      dut.io.laneLive.expect(lanes01.U)
      dut.io.laneEmpty.expect(lanes01.U)

      init(dut, BigInt(1) << 5, Map(5 -> 0x55))
      push(dut, BigInt(1) << 5, Map(5 -> 0x56))
      dut.io.laneEmpty.expect(lanes01.U)
      pop(dut, BigInt(1) << 5, Map(5 -> 0x56))
      pop(dut, BigInt(1) << 5, Map(5 -> 0x55))
      clear(dut, lanes01)
      dut.io.laneLive.expect((BigInt(1) << 5).U)
      init(dut, BigInt(1), Map(0 -> 0x99))
      pop(dut, BigInt(1), Map(0 -> 0x99))
    }
  }

  "Box4 deferred children are pushed far-to-near and later popped near-to-far" in {
    test(new RtTraversalStack(stackDepth = 4)) { dut =>
      driveIdle(dut)
      val lane = BigInt(1) << 7
      init(dut, lane, Map(7 -> 0x100))
      pop(dut, lane, Map(7 -> 0x100))

      // The nearest child continues directly.  Scheduler pushes the three
      // deferred children one per cycle: farthest first, then nearer ones.
      push(dut, lane, Map(7 -> 0x400))
      push(dut, lane, Map(7 -> 0x300))
      push(dut, lane, Map(7 -> 0x200))
      pop(dut, lane, Map(7 -> 0x200))
      pop(dut, lane, Map(7 -> 0x300))
      pop(dut, lane, Map(7 -> 0x400))
    }
  }

  "capacity and empty diagnostics do not mutate a lane stack" in {
    test(new RtTraversalStack(stackDepth = 2)) { dut =>
      driveIdle(dut)
      init(dut, BigInt(1), Map(0 -> 1))
      push(dut, BigInt(1), Map(0 -> 2))
      dut.io.laneFull.expect(1.U)

      dut.io.push.bits.laneMask.poke(1.U)
      dut.io.push.bits.nodeRef(0).poke(3.U)
      dut.io.push.valid.poke(true.B)
      dut.io.pushWouldOverflow.expect(true.B)
      dut.io.push.ready.expect(false.B)
      dut.clock.step()
      dut.io.push.valid.poke(false.B)
      pop(dut, BigInt(1), Map(0 -> 2))
      pop(dut, BigInt(1), Map(0 -> 1))

      dut.io.pop.bits.laneMask.poke(1.U)
      dut.io.pop.valid.poke(true.B)
      dut.io.popWouldUnderflow.expect(true.B)
      dut.io.pop.ready.expect(false.B)
      dut.clock.step()
      dut.io.pop.valid.poke(false.B)
      dut.io.laneEmpty.expect(1.U)
    }
  }

  "depth one rejects a second entry without corrupting its root" in {
    test(new RtTraversalStack(stackDepth = 1)) { dut =>
      driveIdle(dut)
      init(dut, BigInt(1) << 2, Map(2 -> 0xabc))
      dut.io.push.bits.laneMask.poke((BigInt(1) << 2).U)
      dut.io.push.bits.nodeRef(2).poke(0xdef.U)
      dut.io.push.valid.poke(true.B)
      dut.io.push.ready.expect(false.B)
      dut.io.pushWouldOverflow.expect(true.B)
      dut.io.push.valid.poke(false.B)
      pop(dut, BigInt(1) << 2, Map(2 -> 0xabc))
    }
  }
}

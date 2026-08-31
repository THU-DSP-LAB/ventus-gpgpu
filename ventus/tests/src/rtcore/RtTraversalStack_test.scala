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
    dut.io.statusWarpId.poke(0.U)
    dut.io.init.bits.warpId.poke(0.U)
    dut.io.init.bits.laneMask.poke(0.U)
    dut.io.init.bits.rootNodeRef.foreach(_.poke(0.U))
    dut.io.push.bits.warpId.poke(0.U)
    dut.io.push.bits.laneMask.poke(0.U)
    dut.io.push.bits.nodeRef.foreach(_.poke(0.U))
    dut.io.pop.bits.warpId.poke(0.U)
    dut.io.pop.bits.laneMask.poke(0.U)
    dut.io.clear.bits.warpId.poke(0.U)
    dut.io.clear.bits.laneMask.poke(0.U)
  }

  private def init(
      dut: RtTraversalStack,
      warp: Int,
      mask: BigInt,
      roots: Map[Int, BigInt]
  ): Unit = {
    dut.io.init.bits.warpId.poke(warp.U)
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
      warp: Int,
      mask: BigInt,
      values: Map[Int, BigInt]
  ): Unit = {
    dut.io.push.bits.warpId.poke(warp.U)
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
      warp: Int,
      mask: BigInt,
      expected: Map[Int, BigInt]
  ): Unit = {
    dut.io.pop.bits.warpId.poke(warp.U)
    dut.io.pop.bits.laneMask.poke(mask.U)
    dut.io.pop.valid.poke(true.B)
    dut.io.pop.ready.expect(true.B)
    expected.foreach { case (lane, value) => dut.io.popNodeRef(lane).expect(value.U) }
    dut.clock.step()
    dut.io.pop.valid.poke(false.B)
  }

  private def clear(dut: RtTraversalStack, warp: Int, mask: BigInt): Unit = {
    dut.io.clear.bits.warpId.poke(warp.U)
    dut.io.clear.bits.laneMask.poke(mask.U)
    dut.io.clear.valid.poke(true.B)
    dut.io.clear.ready.expect(true.B)
    dut.clock.step()
    dut.io.clear.valid.poke(false.B)
  }

  "lane-major stacks preserve masked LIFO state without warp aliasing" in {
    test(new RtTraversalStack(stackDepth = 4)) { dut =>
      driveIdle(dut)
      val lanes01 = BigInt(3)
      init(dut, warp = 1, lanes01, Map(0 -> 0x10, 1 -> 0x20))
      dut.io.statusWarpId.poke(1.U)
      dut.io.laneLive.expect(lanes01.U)
      dut.io.laneEmpty.expect(0.U)

      push(dut, warp = 1, lanes01, Map(0 -> 0x11, 1 -> 0x21))
      push(dut, warp = 1, lanes01, Map(0 -> 0x12, 1 -> 0x22))
      pop(dut, warp = 1, lanes01, Map(0 -> 0x12, 1 -> 0x22))
      pop(dut, warp = 1, lanes01, Map(0 -> 0x11, 1 -> 0x21))
      pop(dut, warp = 1, lanes01, Map(0 -> 0x10, 1 -> 0x20))
      dut.io.laneLive.expect(lanes01.U)
      dut.io.laneEmpty.expect(lanes01.U)

      init(dut, warp = 2, BigInt(1) << 5, Map(5 -> 0x55))
      push(dut, warp = 2, BigInt(1) << 5, Map(5 -> 0x56))
      dut.io.statusWarpId.poke(1.U)
      dut.io.laneEmpty.expect(lanes01.U)
      pop(dut, warp = 2, BigInt(1) << 5, Map(5 -> 0x56))
      pop(dut, warp = 2, BigInt(1) << 5, Map(5 -> 0x55))
      clear(dut, warp = 1, lanes01)
      dut.io.statusWarpId.poke(1.U)
      dut.io.laneLive.expect(0.U)
      init(dut, warp = 1, BigInt(1), Map(0 -> 0x99))
      pop(dut, warp = 1, BigInt(1), Map(0 -> 0x99))
    }
  }

  "Box4 deferred children are pushed far-to-near and later popped near-to-far" in {
    test(new RtTraversalStack(stackDepth = 4)) { dut =>
      driveIdle(dut)
      val lane = BigInt(1) << 7
      init(dut, warp = 0, lane, Map(7 -> 0x100))
      pop(dut, warp = 0, lane, Map(7 -> 0x100))

      // The nearest child continues directly.  Scheduler pushes the three
      // deferred children one per cycle: farthest first, then nearer ones.
      push(dut, warp = 0, lane, Map(7 -> 0x400))
      push(dut, warp = 0, lane, Map(7 -> 0x300))
      push(dut, warp = 0, lane, Map(7 -> 0x200))
      pop(dut, warp = 0, lane, Map(7 -> 0x200))
      pop(dut, warp = 0, lane, Map(7 -> 0x300))
      pop(dut, warp = 0, lane, Map(7 -> 0x400))
    }
  }

  "capacity and empty diagnostics do not mutate a lane stack" in {
    test(new RtTraversalStack(stackDepth = 2)) { dut =>
      driveIdle(dut)
      init(dut, warp = 0, BigInt(1), Map(0 -> 1))
      push(dut, warp = 0, BigInt(1), Map(0 -> 2))
      dut.io.statusWarpId.poke(0.U)
      dut.io.laneFull.expect(1.U)

      dut.io.push.bits.warpId.poke(0.U)
      dut.io.push.bits.laneMask.poke(1.U)
      dut.io.push.bits.nodeRef(0).poke(3.U)
      dut.io.push.valid.poke(true.B)
      dut.io.pushWouldOverflow.expect(true.B)
      dut.io.push.ready.expect(false.B)
      dut.clock.step()
      dut.io.push.valid.poke(false.B)
      pop(dut, warp = 0, BigInt(1), Map(0 -> 2))
      pop(dut, warp = 0, BigInt(1), Map(0 -> 1))

      dut.io.pop.bits.warpId.poke(0.U)
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
      init(dut, warp = 3, BigInt(1) << 2, Map(2 -> 0xabc))
      dut.io.push.bits.warpId.poke(3.U)
      dut.io.push.bits.laneMask.poke((BigInt(1) << 2).U)
      dut.io.push.bits.nodeRef(2).poke(0xdef.U)
      dut.io.push.valid.poke(true.B)
      dut.io.push.ready.expect(false.B)
      dut.io.pushWouldOverflow.expect(true.B)
      dut.io.push.valid.poke(false.B)
      pop(dut, warp = 3, BigInt(1) << 2, Map(2 -> 0xabc))
    }
  }
}

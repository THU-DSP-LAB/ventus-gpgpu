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
    dut.io.init.bits.objectRay.foreach(_.poke(false.B))
    dut.io.push.bits.laneMask.poke(0.U)
    dut.io.push.bits.nodeRef.foreach(_.poke(0.U))
    dut.io.push.bits.objectRay.foreach(_.poke(false.B))
    dut.io.pop.bits.laneMask.poke(0.U)
    dut.io.clear.bits.laneMask.poke(0.U)
  }

  private def init(
      dut: RtTraversalStack,
      mask: BigInt,
      roots: Map[Int, BigInt],
      objectRays: Map[Int, Boolean] = Map.empty
  ): Unit = {
    dut.io.init.bits.laneMask.poke(mask.U)
    dut.io.init.bits.rootNodeRef.zipWithIndex.foreach { case (ref, lane) =>
      ref.poke(roots.getOrElse(lane, BigInt(0)).U)
    }
    dut.io.init.bits.objectRay.zipWithIndex.foreach { case (tag, lane) =>
      tag.poke(objectRays.getOrElse(lane, false).B)
    }
    dut.io.init.valid.poke(true.B)
    dut.io.init.ready.expect(true.B)
    dut.clock.step()
    dut.io.init.valid.poke(false.B)
  }

  private def push(
      dut: RtTraversalStack,
      mask: BigInt,
      values: Map[Int, BigInt],
      objectRays: Map[Int, Boolean] = Map.empty
  ): Unit = {
    dut.io.push.bits.laneMask.poke(mask.U)
    dut.io.push.bits.nodeRef.zipWithIndex.foreach { case (ref, lane) =>
      ref.poke(values.getOrElse(lane, BigInt(0)).U)
    }
    dut.io.push.bits.objectRay.zipWithIndex.foreach { case (tag, lane) =>
      tag.poke(objectRays.getOrElse(lane, false).B)
    }
    dut.io.push.valid.poke(true.B)
    dut.io.push.ready.expect(true.B)
    dut.io.pushAcceptedMask.expect(mask.U)
    dut.clock.step()
    dut.io.push.valid.poke(false.B)
  }

  private def pop(
      dut: RtTraversalStack,
      mask: BigInt,
      expected: Map[Int, BigInt],
      objectRays: Map[Int, Boolean] = Map.empty
  ): Unit = {
    dut.io.pop.bits.laneMask.poke(mask.U)
    dut.io.pop.valid.poke(true.B)
    dut.io.pop.ready.expect(true.B)
    expected.foreach { case (lane, value) =>
      dut.io.popNodeRef(lane).expect(value.U)
      dut.io.popObjectRay(lane).expect(objectRays.getOrElse(lane, false).B)
    }
    dut.clock.step()
    dut.io.pop.valid.poke(false.B)
  }

  "ray-space tags follow their node reference through LIFO operations" in {
    test(new RtTraversalStack(stackDepth = 4)).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      driveIdle(dut)
      init(dut, BigInt(1), Map(0 -> 0x10), Map(0 -> false))
      push(dut, BigInt(1), Map(0 -> 0x20), Map(0 -> true))
      push(dut, BigInt(1), Map(0 -> 0x30), Map(0 -> false))
      pop(dut, BigInt(1), Map(0 -> 0x30), Map(0 -> false))
      pop(dut, BigInt(1), Map(0 -> 0x20), Map(0 -> true))
      pop(dut, BigInt(1), Map(0 -> 0x10), Map(0 -> false))
    }
  }

  private def clear(dut: RtTraversalStack, mask: BigInt): Unit = {
    dut.io.clear.bits.laneMask.poke(mask.U)
    dut.io.clear.valid.poke(true.B)
    dut.io.clear.ready.expect(true.B)
    dut.clock.step()
    dut.io.clear.valid.poke(false.B)
  }

  "lane-major stacks preserve masked LIFO state for one resident warp" in {
    test(new RtTraversalStack(stackDepth = 4)).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
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

  "independent lanes can push and pop in the same cycle" in {
    test(new RtTraversalStack(stackDepth = 4)).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      driveIdle(dut)
      init(dut, BigInt(3), Map(0 -> 0x10, 1 -> 0x20))
      dut.io.push.bits.laneMask.poke(1.U)
      dut.io.push.bits.nodeRef(0).poke(0x11.U)
      dut.io.pop.bits.laneMask.poke(2.U)
      dut.io.push.valid.poke(true.B)
      dut.io.pop.valid.poke(true.B)
      dut.io.push.ready.expect(true.B)
      dut.io.pop.ready.expect(true.B)
      dut.io.popNodeRef(1).expect(0x20.U)
      dut.clock.step()
      dut.io.push.valid.poke(false.B)
      dut.io.pop.valid.poke(false.B)
      dut.io.push.bits.laneMask.poke(0.U)
      dut.io.pop.bits.laneMask.poke(1.U)
      dut.io.pop.valid.poke(true.B)
      dut.io.pop.ready.expect(true.B)
      dut.io.popNodeRef(0).expect(0x11.U)
      dut.clock.step()
    }
  }

  "Box4 deferred children are pushed far-to-near and later popped near-to-far" in {
    test(new RtTraversalStack(stackDepth = 4)).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
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
    test(new RtTraversalStack(stackDepth = 2)).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      driveIdle(dut)
      init(dut, BigInt(1), Map(0 -> 1))
      push(dut, BigInt(1), Map(0 -> 2))
      for (value <- 3 to 34) push(dut, BigInt(1), Map(0 -> BigInt(value)))
      dut.io.laneFull.expect(1.U)

      dut.io.push.bits.laneMask.poke(1.U)
      dut.io.push.bits.nodeRef(0).poke(3.U)
      dut.io.push.valid.poke(true.B)
      dut.io.pushWouldOverflow.expect(true.B)
      dut.io.push.ready.expect(false.B)
      dut.clock.step()
      dut.io.push.valid.poke(false.B)
      for (value <- 34 to 3 by -1) pop(dut, BigInt(1), Map(0 -> BigInt(value)))
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

  "depth one uses one overflow bank then rejects further entries" in {
    test(new RtTraversalStack(stackDepth = 1)).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      driveIdle(dut)
      init(dut, BigInt(1) << 2, Map(2 -> 0xabc))
      for (value <- 1 to 32) push(dut, BigInt(1) << 2, Map(2 -> BigInt(value)))
      dut.io.push.bits.laneMask.poke((BigInt(1) << 2).U)
      dut.io.push.bits.nodeRef(2).poke(0xdef.U)
      dut.io.push.valid.poke(true.B)
      dut.io.push.ready.expect(false.B)
      dut.io.pushWouldOverflow.expect(true.B)
      dut.io.push.valid.poke(false.B)
      for (value <- 32 to 1 by -1) pop(dut, BigInt(1) << 2, Map(2 -> BigInt(value)))
      pop(dut, BigInt(1) << 2, Map(2 -> 0xabc))
    }
  }

  "four shared overflow banks isolate stalled lanes and recycle after pop clear and reset" in {
    test(new RtTraversalStack).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      driveIdle(dut)
      init(dut, BigInt(63), (0 until 6).map(i => i -> BigInt(i)).toMap)
      // Five contenders have full base stacks, while lane 5 still has room.
      for (depth <- 1 until 32) {
        push(dut, BigInt(31), (0 until 5).map(i => i -> BigInt(1000*i+depth)).toMap)
      }
      dut.io.push.valid.poke(true.B)
      dut.io.push.bits.laneMask.poke(63.U)
      for (i <- 0 until 6) {
        dut.io.push.bits.nodeRef(i).poke((1000*i+32).U)
        dut.io.push.bits.objectRay(i).poke(true.B)
      }
      dut.io.push.ready.expect(true.B)
      dut.io.pushAcceptedMask.expect(47.U) // lanes 0..3 + 5; lane 4 retries
      dut.io.pushWouldOverflow.expect(true.B)
      dut.clock.step()
      dut.io.push.valid.poke(false.B)
      dut.io.laneFull.expect(16.U)
      // A blocked push in lane 4 does not block a disjoint pop releasing bank0.
      dut.io.push.valid.poke(true.B)
      dut.io.push.bits.laneMask.poke(16.U)
      dut.io.pop.valid.poke(true.B)
      dut.io.pop.bits.laneMask.poke(1.U)
      dut.io.push.ready.expect(false.B)
      dut.io.pop.ready.expect(true.B)
      dut.io.popNodeRef(0).expect(32.U)
      dut.io.popObjectRay(0).expect(true.B)
      dut.clock.step()
      dut.io.pop.valid.poke(false.B)
      dut.io.pushAcceptedMask.expect(16.U)
      dut.clock.step()
      dut.io.push.valid.poke(false.B)
      // Lane 0's original stack is intact; recycled bank now belongs to lane4.
      pop(dut, BigInt(1), Map(0 -> 31))
      pop(dut, BigInt(16), Map(4 -> 4032), Map(4 -> true))
      pop(dut, BigInt(16), Map(4 -> 4031))
      pop(dut, BigInt(32), Map(5 -> 5032), Map(5 -> true))
      clear(dut, BigInt(14))
      init(dut, BigInt(14), Map(1 -> 71, 2 -> 72, 3 -> 73))
      pop(dut, BigInt(14), Map(1 -> 71, 2 -> 72, 3 -> 73))
      // Refill all four banks, reset while owned, then repeat allocation.
      for (_ <- 0 until 2) {
        dut.reset.poke(true.B); dut.clock.step(2); dut.reset.poke(false.B)
        driveIdle(dut)
        dut.io.laneLive.expect(0.U)
        init(dut, BigInt(15), (0 until 4).map(i => i -> BigInt(i)).toMap)
        for (depth <- 1 to 32) push(dut, BigInt(15), (0 until 4).map(i => i -> BigInt(depth)).toMap)
      }
    }
  }

  "a lane stops at depth64 even with free banks and preserves tagged LIFO data" in {
    test(new RtTraversalStack).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      driveIdle(dut)
      init(dut, BigInt(3), Map(0 -> 0, 1 -> 100))
      for (depth <- 1 until 64) push(dut, BigInt(1), Map(0 -> BigInt(depth)), Map(0 -> (depth % 2 == 1)))
      dut.io.laneFull.expect(1.U)
      dut.io.push.valid.poke(true.B)
      dut.io.push.bits.laneMask.poke(3.U)
      dut.io.push.bits.nodeRef(0).poke(999.U)
      dut.io.push.bits.nodeRef(1).poke(101.U)
      dut.io.pushAcceptedMask.expect(2.U)
      dut.io.pushWouldOverflow.expect(true.B)
      dut.clock.step()
      dut.io.push.valid.poke(false.B)
      for (depth <- 63 to 0 by -1) pop(dut, BigInt(1), Map(0 -> BigInt(depth)), Map(0 -> (depth % 2 == 1)))
      pop(dut, BigInt(2), Map(1 -> 101))
      pop(dut, BigInt(2), Map(1 -> 100))
      dut.io.laneEmpty.expect(3.U)
    }
  }
}

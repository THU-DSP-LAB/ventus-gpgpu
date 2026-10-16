package rtcore

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import top.parameters._

class RtParentRayHistoryTest extends AnyFreeSpec with ChiselScalatestTester {
  "new raygen-loop rays batch but first-ray candidate resumes retain their classification" in {
    test(new RtParentRayHistory) { dut =>
      dut.io.allocated.valid.poke(false.B)
      dut.io.allocated.bits.poke(0.U)
      dut.io.published.valid.poke(true.B)
      dut.io.published.bits.warpId.poke(0.U)
      dut.io.published.bits.mask.poke(3.U)
      dut.clock.step()
      dut.io.secondary(0).expect(0.U)
      // A callback resume has no fresh-header publication, even at depth 0.
      dut.io.published.valid.poke(false.B)
      dut.clock.step(5)
      dut.io.secondary(0).expect(0.U)
      // Lane 0 publishes its next ray; lane 2 publishes its first ray.
      dut.io.published.valid.poke(true.B)
      dut.io.published.bits.mask.poke(5.U)
      dut.clock.step()
      dut.io.secondary(0).expect(1.U)
      dut.io.published.bits.mask.poke(2.U)
      dut.clock.step()
      dut.io.secondary(0).expect(3.U)
      dut.io.published.valid.poke(false.B)
      dut.clock.step(5)
      dut.io.secondary(0).expect(3.U)
      dut.io.secondary(1).expect(0.U)
      // A recycled physical warp row must not inherit the old ray history.
      dut.io.allocated.valid.poke(true.B)
      dut.clock.step()
      dut.io.secondary(0).expect(0.U)
      dut.io.allocated.valid.poke(false.B)
      dut.io.published.valid.poke(true.B)
      dut.io.published.bits.mask.poke(3.U)
      dut.clock.step()
      dut.io.secondary(0).expect(0.U)
    }
  }
}

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

class RtConcurrentReservationTest extends AnyFreeSpec with ChiselScalatestTester {
  private def init(d: RtSessionReservation): Unit = {
    d.io.issued.valid.poke(false.B)
    d.io.traverseResolved.valid.poke(false.B)
    d.io.completed.valid.poke(false.B)
    d.io.releaseIssued.valid.poke(false.B)
    d.io.released.valid.poke(false.B)
    d.io.concurrent.get.foreach(_.poke(true.B))
    d.io.issuedExclusive.get.poke(false.B)
  }
  private def issue(d: RtSessionReservation, warp: Int, empty: Boolean = false): Unit = {
    d.io.allowed(warp).expect(true.B)
    d.io.issued.bits.poke(warp.U); d.io.issued.valid.poke(true.B)
    d.clock.step(); d.io.issued.valid.poke(false.B)
    d.io.traverseResolved.bits.warpId.poke(warp.U)
    d.io.traverseResolved.bits.wasEmpty.poke(empty.B)
    d.io.traverseResolved.valid.poke(true.B)
    d.clock.step(); d.io.traverseResolved.valid.poke(false.B)
  }
  "independent parents enter concurrently and disjoint completion/admission cannot lose a credit" in {
    test(new RtSessionReservation(true)) { d =>
      init(d)
      issue(d, 0); issue(d, 1)
      d.io.allowed(0).expect(false.B); d.io.allowed(1).expect(false.B)
      d.io.allowed(2).expect(true.B)
      d.io.completed.valid.poke(true.B)
      d.io.completed.bits.warpId.poke(0.U)
      d.io.completed.bits.contextReleased.poke(true.B)
      d.io.issued.valid.poke(true.B); d.io.issued.bits.poke(2.U)
      d.clock.step(); d.io.completed.valid.poke(false.B); d.io.issued.valid.poke(false.B)
      d.io.allowed(0).expect(true.B); d.io.allowed(1).expect(false.B)
      d.io.allowed(2).expect(false.B)
      d.io.traverseResolved.valid.poke(true.B)
      d.io.traverseResolved.bits.warpId.poke(2.U)
      d.io.traverseResolved.bits.wasEmpty.poke(true.B)
      d.clock.step(); d.io.traverseResolved.valid.poke(false.B)
      d.io.allowed(2).expect(true.B)
      d.io.allowed(1).expect(false.B)
    }
  }
  "legacy empty admission cancels only its provisional lock and retained release stays exclusive" in {
    test(new RtSessionReservation(true)) { d =>
      init(d); d.io.concurrent.get(0).poke(false.B)
      issue(d, 0, empty = true)
      d.io.reserved.expect(false.B); d.io.allowed.foreach(_.expect(true.B))
      issue(d, 0)
      d.io.allowed.foreach(_.expect(false.B))
      d.io.completed.valid.poke(true.B)
      d.io.completed.bits.warpId.poke(0.U)
      d.io.completed.bits.contextReleased.poke(false.B)
      d.clock.step(); d.io.completed.valid.poke(false.B)
      issue(d, 0, empty = true)
      d.io.reserved.expect(true.B)
      for (warp <- 0 until num_warp) d.io.allowed(warp).expect((warp == 0).B)
      d.io.releaseIssued.valid.poke(true.B); d.io.releaseIssued.bits.poke(0.U)
      d.clock.step(); d.io.releaseIssued.valid.poke(false.B)
      d.io.allowed.foreach(_.expect(false.B))
      d.io.released.valid.poke(true.B); d.io.released.bits.poke(0.U)
      d.clock.step(); d.io.released.valid.poke(false.B)
      d.io.allowed.foreach(_.expect(true.B))
    }
  }
}

class RtTaskBatcherTest extends AnyFreeSpec with ChiselScalatestTester {
  private def init(d: RtTaskBatcher): Unit = {
    d.io.command.valid.poke(false.B)
    d.io.schedulerCommand.ready.poke(true.B)
    d.io.schedulerCompletion.valid.poke(false.B)
    d.io.schedulerCompletion.bits.warpId.poke(7.U)
    d.io.schedulerCompletion.bits.issueId.poke(0.U)
    d.io.schedulerCompletion.bits.destinationVd.poke(0.U)
    d.io.schedulerCompletion.bits.completionMask.poke("hffffffff".U)
    d.io.schedulerCompletion.bits.laneStatus.foreach(_.poke(0.U))
    d.io.schedulerCompletion.bits.contextReleased.poke(true.B)
    d.io.completion.ready.poke(true.B)
    val c = d.io.command.bits
    c.spillPool.spillPoolBase.poke("h100000".U)
    c.spillPool.slotsPerCore.poke(256.U)
    c.spillPool.perCoreStride.poke(98304.U)
    c.spillPool.dispatchEpoch.poke(1.U)
    c.workgroupTag.poke(0.U); c.workgroupMember.poke(0.U)
    c.workgroupWarps.poke(1.U); c.workgroupMemberValid.poke(1.U)
    c.workgroupMemberWarp.foreach(_.poke(0.U))
    c.grouped.poke(false.B); c.locator.foreach(_.poke(0.U))
  }
  private def submit(d: RtTaskBatcher, warp: Int, mask: BigInt,
                     eligible: Boolean = true, epoch: Int = 1): Unit = {
    val c = d.io.command.bits
    c.warpId.poke(warp.U); c.activeMask.poke(mask.U)
    c.issueId.poke((warp + 10).U); c.destinationVd.poke((warp + 20).U)
    c.batchEligible.poke(eligible.B); c.spillPool.dispatchEpoch.poke(epoch.U)
    d.io.command.valid.poke(true.B)
    d.io.command.ready.expect(true.B)
    d.clock.step(); d.io.command.valid.poke(false.B)
  }
  private def occupy(d: RtTaskBatcher): Unit = {
    submit(d, 7, BigInt("ffffffff", 16), eligible = false)
    d.io.schedulerCommand.ready.poke(false.B)
  }
  private def finishDirect(d: RtTaskBatcher): Unit = {
    d.io.schedulerCompletion.valid.poke(true.B)
    d.io.completion.bits.warpId.expect(7.U)
    d.io.schedulerCompletion.ready.expect(true.B)
    d.clock.step(); d.io.schedulerCompletion.valid.poke(false.B)
    d.clock.step() // build pending descriptors, without waiting for a cohort
    d.io.schedulerCommand.valid.expect(true.B)
  }
  "sparse parents share one bank-preserving batch and receive their own statuses under backpressure" in {
    test(new RtTaskBatcher) { d =>
      init(d); occupy(d)
      submit(d, 0, BigInt(1) | (BigInt(1) << 8))
      submit(d, 1, (BigInt(1) << 24) | (BigInt(1) << 1))
      finishDirect(d)
      val c = d.io.schedulerCommand.bits
      c.grouped.expect(true.B)
      c.activeMask.expect((BigInt(1) | (BigInt(1) << 8) | (BigInt(1) << 16) | 2).U)
      c.locator(0).expect(0.U); c.locator(8).expect(8.U)
      c.locator(16).expect((32 + 24).U); c.locator(1).expect(33.U)
      for (i <- 0 until num_warp) c.workgroupMemberWarp(i).expect(i.U)
      // Unrelated command input cannot perturb an already-built packet.
      submit(d, 2, BigInt(4))
      c.locator(16).expect(56.U)
      d.io.schedulerCommand.ready.poke(true.B)
      d.clock.step(); d.io.schedulerCommand.ready.poke(false.B)
      d.io.schedulerCompletion.bits.completionMask.poke((BigInt(1) | (BigInt(1) << 8) | (BigInt(1) << 16) | 2).U)
      for (i <- 0 until num_thread) d.io.schedulerCompletion.bits.laneStatus(i).poke((1000 + i).U)
      d.io.schedulerCompletion.valid.poke(true.B)
      d.io.completion.ready.poke(false.B)
      d.io.completion.bits.warpId.expect(0.U)
      d.io.completion.bits.laneStatus(0).expect(1000.U)
      d.io.completion.bits.laneStatus(8).expect(1008.U)
      d.clock.step(4)
      d.io.completion.bits.issueId.expect(10.U)
      d.io.completion.bits.destinationVd.expect(20.U)
      d.io.schedulerCompletion.ready.expect(false.B)
      d.io.completion.ready.poke(true.B); d.clock.step()
      d.io.completion.bits.warpId.expect(1.U)
      d.io.completion.bits.completionMask.expect(((BigInt(1) << 24) | 2).U)
      d.io.completion.bits.laneStatus(24).expect(1016.U)
      d.io.completion.bits.laneStatus(1).expect(1001.U)
      d.io.completion.bits.issueId.expect(11.U)
      d.io.schedulerCompletion.ready.expect(true.B)
      d.clock.step(); d.io.schedulerCompletion.valid.poke(false.B)
      d.clock.step(); d.io.schedulerCommand.valid.expect(true.B)
      d.io.schedulerCommand.bits.warpId.expect(2.U)
      d.io.schedulerCommand.bits.grouped.expect(false.B)
    }
  }
  "a full bank cannot steal a slot from another bank or split the parent request" in {
    test(new RtTaskBatcher) { d =>
      init(d); occupy(d)
      val mask = BigInt("01010101", 16)
      submit(d, 0, mask); submit(d, 1, BigInt(1))
      finishDirect(d)
      d.io.schedulerCommand.bits.grouped.expect(false.B)
      d.io.schedulerCommand.bits.activeMask.expect(mask.U)
      d.io.schedulerCommand.ready.poke(true.B); d.clock.step()
      d.io.schedulerCompletion.bits.completionMask.poke(mask.U)
      d.io.schedulerCompletion.valid.poke(true.B)
      d.io.completion.bits.warpId.expect(0.U)
      d.clock.step(); d.io.schedulerCompletion.valid.poke(false.B)
      d.io.schedulerCommand.ready.poke(false.B); d.clock.step()
      d.io.schedulerCommand.bits.warpId.expect(1.U)
      d.io.schedulerCommand.bits.activeMask.expect(1.U)
    }
  }
  "primary and incompatible dispatch descriptors do not join a sparse batch" in {
    test(new RtTaskBatcher) { d =>
      init(d); occupy(d)
      submit(d, 0, BigInt(1), eligible = false)
      submit(d, 1, BigInt(2), epoch = 2)
      finishDirect(d)
      d.io.schedulerCommand.bits.grouped.expect(false.B)
      d.io.schedulerCommand.bits.activeMask.expect(1.U)
      d.io.schedulerCommand.ready.poke(true.B); d.clock.step()
      d.io.schedulerCompletion.bits.completionMask.poke(1.U)
      d.io.schedulerCompletion.valid.poke(true.B); d.clock.step()
      d.io.schedulerCompletion.valid.poke(false.B)
      d.io.schedulerCommand.ready.poke(false.B); d.clock.step()
      d.io.schedulerCommand.bits.warpId.expect(1.U)
      d.io.schedulerCommand.bits.spillPool.dispatchEpoch.expect(2.U)
    }
  }
}

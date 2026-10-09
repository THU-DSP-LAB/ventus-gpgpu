package rtcore

import chisel3._
import chisel3.util.DecoupledIO
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import top.parameters._

class RtCompletionPolicyHarness extends Module {
  val io = IO(new Bundle {
    val completion = Input(new RtWarpCompletion)
    val grouped = Input(Bool())
    val terminalFence = Output(Bool())
  })
  io.terminalFence := io.grouped && io.completion.contextReleased && !io.completion.hasCandidate
}

/** Real scheduler with handshake-driven SRAM/AS/IU services. Arithmetic unit
  * values are prescribed so these tests isolate register ownership and DFS,
  * rather than repeating the numeric pipeline reference tests. */
class RtSchedulerRegisterLifetimeTest extends AnyFreeSpec with ChiselScalatestTester {
  "one child-ref bank preserves a nontrivial permutation through deferred DFS pushes" in {
    test(new RtCoreScheduler).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      exercise(dut, candidate = false)
    }
  }

  "opaque committed and procedural candidate records both survive candidate ignore/resume" in {
    test(new RtCoreScheduler).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      exercise(dut, candidate = true)
    }
  }

  private def exercise(dut: RtCoreScheduler, candidate: Boolean): Unit = {
    val base = BigInt(0x100000)
    val words = RtCorePortParameters.lineWordCount
    val banks = RtLocalSramParameters.corePortCount
    val local = scala.collection.mutable.Map.empty[(Int, Int, Int), BigInt]
    def put(field: Int, value: BigInt): Unit = local((0, field, 0)) = value
    def get(row: Int, field: Int, lane: Int): BigInt = local.getOrElse((row, field, lane), BigInt(0))
    put(RtAbiLayout.SlotAccelLo, base)
    put(RtAbiLayout.SlotTmax, BigInt(0x41200000)) // 10
    put(RtAbiLayout.SlotDirectionZ, BigInt(0x3f800000))
    put(RtAbiLayout.SlotInvX, BigInt(0x7f800000))
    put(RtAbiLayout.SlotInvY, BigInt(0x7f800000))
    put(RtAbiLayout.SlotInvZ, BigInt(0x3f800000))

    def asLine(address: BigInt): Seq[BigInt] = {
      val data = Array.fill(words)(BigInt(0))
      if (address == base) {
        data((RtVtasLayout.AsHeaderRootNodeRef / 4).toInt) = BigInt(0x81)
      } else if (address == base + 128) {
        val refs = if (candidate) Seq(0x102L, 0x184L, 0xffffffffL, 0xffffffffL)
          else Seq(0x101L, 0x181L, 0x201L, 0x281L)
        refs.zipWithIndex.foreach { case (ref, i) => data(i) = BigInt(ref) }
        data((RtVtasLayout.Box4MinX / 4).toInt) = BigInt(1) // root marker
      } else if (candidate && address == base + 256) {
        data((RtVtasLayout.TriangleFlags / 4).toInt) = BigInt(1) // opaque
        data((RtVtasLayout.TrianglePrimitiveId / 4).toInt) = BigInt(5)
        data((RtVtasLayout.TriangleGeometryId / 4).toInt) = BigInt(7)
      } else if (candidate && address == base + 384) {
        data((RtVtasLayout.AabbMin / 4).toInt) = BigInt(17) // procedural marker
        data((RtVtasLayout.AabbPrimitiveId / 4).toInt) = BigInt(9)
        data((RtVtasLayout.AabbGeometryId / 4).toInt) = BigInt(11)
        data((RtVtasLayout.AabbHitKind / 4).toInt) = BigInt(0x77)
      } else {
        for (i <- 0 until 4) data(i) = BigInt(0xffffffffL)
        data((RtVtasLayout.Box4MinX / 4).toInt) = (address - base) / 128
      }
      data.toSeq
    }

    val command = dut.io.command.bits
    dut.io.coreId.poke(0.U)
    dut.io.command.valid.poke(false.B)
    command.warpId.poke(0.U)
    command.workgroupTag.poke(0.U)
    command.workgroupMember.poke(0.U)
    command.workgroupWarps.poke(1.U)
    command.workgroupMemberValid.poke(1.U)
    command.workgroupMemberWarp.foreach(_.poke(0.U))
    command.issueId.poke(1.U)
    command.destinationVd.poke(0.U)
    command.activeMask.poke(1.U)
    command.grouped.poke(false.B)
    command.locator.zipWithIndex.foreach { case (p, i) => p.poke(i.U) }
    command.spillPool.spillPoolBase.poke(0.U) // exercise retained callback path
    command.spillPool.slotsPerCore.poke(256.U)
    command.spillPool.perCoreStride.poke(98304.U)
    command.spillPool.dispatchEpoch.poke(1.U)
    dut.io.completion.ready.poke(true.B)
    dut.io.release.valid.poke(false.B)
    dut.io.release.bits.warpId.poke(0.U)
    dut.io.release.bits.activeMask.poke(1.U)
    dut.io.releaseAck.ready.poke(true.B)
    dut.io.lsuResponse.bits.fault.poke(false.B)
    dut.io.lsuResponse.bits.clientId.poke(1.U)
    dut.io.lsuResponse.bits.coverageMask.poke(0.U)

    var cycle = 0
    var wide: Option[Seq[BigInt]] = None
    val narrow = Array.fill[Option[BigInt]](banks)(None)
    var memory: Option[(BigInt, BigInt, Seq[BigInt])] = None
    val boxes = Array.fill[Option[(BigInt, Int)]](RtCoreSchedulerParameters.boxUnitCount)(None)
    val triangles = Array.fill[Option[BigInt]](RtCoreSchedulerParameters.triangleUnitCount)(None)
    val issuedMarkers = scala.collection.mutable.ArrayBuffer.empty[Int]
    val completions = scala.collection.mutable.ArrayBuffer.empty[(BigInt, Boolean)]
    def fired[T <: Data](p: DecoupledIO[T]): Boolean =
      p.valid.peek().litToBoolean && p.ready.peek().litToBoolean

    def tick(): Unit = {
      dut.io.localRequest.ready.poke(true.B)
      dut.io.localResponse.valid.poke(wide.nonEmpty.B)
      for (i <- 0 until num_thread) dut.io.localResponse.bits.readData(i).poke(wide.map(_(i)).getOrElse(BigInt(0)).U)
      for (bank <- 0 until banks) {
        dut.io.localNarrowRequest(bank).ready.poke((cycle % 3 != 1).B)
        dut.io.localNarrowResponse(bank).valid.poke(narrow(bank).nonEmpty.B)
        dut.io.localNarrowResponse(bank).bits.readData.poke(narrow(bank).getOrElse(BigInt(0)).U)
      }
      dut.io.lsuRequest.ready.poke((cycle % 5 != 1).B)
      dut.io.lsuResponse.valid.poke(memory.nonEmpty.B)
      dut.io.lsuResponse.bits.clientTag.poke(memory.map(_._1).getOrElse(BigInt(0)).U)
      dut.io.lsuResponse.bits.lineBase.poke(memory.map(_._2).getOrElse(BigInt(0)).U)
      for (i <- 0 until words) dut.io.lsuResponse.bits.data(i).poke(memory.map(_._3(i)).getOrElse(BigInt(0)).U)
      for (unit <- boxes.indices) {
        dut.boxIssue(unit).ready.poke((cycle % 4 != 1).B)
        dut.boxResult(unit).valid.poke(boxes(unit).nonEmpty.B)
        val (id, marker) = boxes(unit).getOrElse((BigInt(0), 0))
        dut.boxResult(unit).bits.requestId.poke(id.U)
        val count = if (marker == 1) { if (candidate) 2 else 4 }
          else if (candidate && marker == 17) 1 else 0
        val order = if (candidate) Seq(0, 1, 2, 3) else Seq(2, 0, 3, 1)
        for (i <- 0 until 4) {
          dut.boxResult(unit).bits.boxIndex(i).poke(order(i).U)
          dut.boxResult(unit).bits.isIntersect(i).poke((i < count).B)
          dut.boxResult(unit).bits.tmin(i).poke((if (marker == 17) 0x3f000000L else 0x3e800000L).U)
        }
      }
      for (unit <- triangles.indices) {
        dut.triangleIssue(unit).ready.poke(true.B)
        dut.triangleResult(unit).valid.poke(triangles(unit).nonEmpty.B)
        val out = dut.triangleResult(unit).bits
        out.requestId.poke(triangles(unit).getOrElse(BigInt(0)).U)
        out.uNum.poke("h3e800000".U); out.vNum.poke("h3e800000".U)
        out.tNum.poke("h3f800000".U); out.tDenom.poke("h3f800000".U)
        out.hit.poke(true.B); out.frontFace.poke(true.B)
      }

      if (fired(dut.io.localResponse)) wide = None
      if (fired(dut.io.localRequest)) {
        val p = dut.io.localRequest.bits
        val row = p.warpId.peek().litValue.toInt * RtLocalSramParameters.fieldCount
        val field = p.fieldId.peek().litValue.toInt
        if (p.write.peek().litToBoolean) {
          for (lane <- 0 until num_thread if p.laneMask.peek().litValue.testBit(lane))
            local((row, field, lane)) = p.writeData(lane).peek().litValue
        } else {
          assert(wide.isEmpty)
          wide = Some((0 until num_thread).map(lane => get(row, field, lane)))
        }
      }
      for (bank <- 0 until banks) {
        if (fired(dut.io.localNarrowResponse(bank))) narrow(bank) = None
        if (fired(dut.io.localNarrowRequest(bank))) {
          val p = dut.io.localNarrowRequest(bank).bits
          val key = (p.rowBase.peek().litValue.toInt, p.fieldId.peek().litValue.toInt, p.laneId.peek().litValue.toInt)
          if (p.write.peek().litToBoolean) local(key) = p.writeData.peek().litValue
          else {
            assert(narrow(bank).isEmpty)
            narrow(bank) = Some(local.getOrElse(key, BigInt(0)))
          }
        }
      }
      if (fired(dut.io.lsuResponse)) memory = None
      if (fired(dut.io.lsuRequest)) {
        val p = dut.io.lsuRequest.bits
        assert(memory.isEmpty && !p.write.peek().litToBoolean)
        val address = p.lineBase.peek().litValue
        memory = Some((p.clientTag.peek().litValue, address, asLine(address)))
      }
      for (unit <- boxes.indices) {
        if (fired(dut.boxResult(unit))) boxes(unit) = None
        if (fired(dut.boxIssue(unit))) {
          assert(boxes(unit).isEmpty)
          val marker = dut.boxIssue(unit).bits.aabb(0).x_min.peek().litValue.toInt
          issuedMarkers += marker
          boxes(unit) = Some((dut.boxIssue(unit).bits.requestId.peek().litValue, marker))
        }
      }
      for (unit <- triangles.indices) {
        if (fired(dut.triangleResult(unit))) triangles(unit) = None
        if (fired(dut.triangleIssue(unit))) {
          assert(triangles(unit).isEmpty)
          triangles(unit) = Some(dut.triangleIssue(unit).bits.requestId.peek().litValue)
        }
      }
      if (fired(dut.io.completion)) {
        dut.io.completion.bits.warpId.expect(0.U)
        dut.io.completion.bits.completionMask.expect(1.U)
        completions += ((dut.io.completion.bits.laneStatus(0).peek().litValue,
          dut.io.completion.bits.contextReleased.peek().litToBoolean))
      }
      dut.clock.step()
      cycle += 1
    }

    def submit(): Unit = {
      dut.io.command.valid.poke(true.B)
      var waiting = 0
      while (!dut.io.command.ready.peek().litToBoolean && waiting < 100) { tick(); waiting += 1 }
      dut.io.command.ready.expect(true.B)
      tick()
      dut.io.command.valid.poke(false.B)
    }
    def finish(count: Int): Unit = {
      val limit = cycle + 1500
      while (completions.size < count && cycle < limit) tick()
      assert(completions.size == count, s"scheduler did not complete by cycle $cycle")
    }

    tick()
    submit()
    finish(1)
    if (!candidate) {
      assert(issuedMarkers.toSeq == Seq(1, 4, 2, 5, 3))
      assert(completions.head == ((BigInt(RtAbiLayout.TraversalCompleteMiss), true)))
    } else {
      val candidateBase = RtAbiLayout.LocalCandidateHitRecordBaseBytes / RtAbiLayout.AbiWordBytes
      val committedBase = RtAbiLayout.LocalCommittedHitRecordBaseBytes / RtAbiLayout.AbiWordBytes
      assert(completions.head == ((BigInt(RtAbiLayout.TraversalCandidateProceduralAabb), false)))
      assert(get(0, candidateBase + RtAbiLayout.HitRecordPrimitiveId, 0) == 9)
      assert(get(0, candidateBase + RtAbiLayout.HitRecordGeometryId, 0) == 11)
      assert(get(0, candidateBase + RtAbiLayout.HitRecordHitT, 0) == BigInt(0x3f000000))
      assert(get(0, committedBase + RtAbiLayout.HitRecordPrimitiveId, 0) == 5)
      assert(get(0, committedBase + RtAbiLayout.HitRecordHitT, 0) == BigInt(0x3f800000))
      put(RtAbiLayout.LocalControlBaseBytes / RtAbiLayout.AbiWordBytes, BigInt(RtAbiLayout.CallbackIgnore))
      command.issueId.poke(2.U)
      submit()
      finish(2)
      assert(completions(1) == ((BigInt(RtAbiLayout.TraversalCompleteHit), true)))
      assert(get(0, committedBase + RtAbiLayout.HitRecordPrimitiveId, 0) == 5)
      assert(get(0, committedBase + RtAbiLayout.HitRecordHitT, 0) == BigInt(0x3f800000))
    }
  }
}

class RtCompletionPolicyTest extends AnyFreeSpec with ChiselScalatestTester {
  "physically released candidates bypass the logical group terminal fence" in {
    test(new RtCompletionPolicyHarness).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      dut.io.grouped.poke(true.B)
      dut.io.completion.warpId.poke(0.U)
      dut.io.completion.issueId.poke(0.U)
      dut.io.completion.destinationVd.poke(0.U)
      dut.io.completion.completionMask.poke(1.U)
      dut.io.completion.contextReleased.poke(true.B)
      dut.io.completion.laneStatus.foreach(_.poke(RtAbiLayout.TraversalCompleteMiss.U))
      for (status <- Seq(RtAbiLayout.TraversalCandidateProceduralAabb,
          RtAbiLayout.TraversalCandidateNonOpaqueTriangle)) {
        dut.io.completion.laneStatus(0).poke(status.U)
        dut.io.terminalFence.expect(false.B)
        dut.io.completion.completionMask.poke(2.U)
        dut.io.terminalFence.expect(true.B) // Inactive candidate lane ignored.
        dut.io.completion.completionMask.poke(1.U)
      }
      dut.io.completion.laneStatus(0).poke(RtAbiLayout.TraversalCompleteHit.U)
      dut.io.terminalFence.expect(true.B)
      dut.io.completion.contextReleased.poke(false.B)
      dut.io.terminalFence.expect(false.B)
      dut.io.completion.contextReleased.poke(true.B)
      dut.io.grouped.poke(false.B)
      dut.io.terminalFence.expect(false.B)
    }
  }
}

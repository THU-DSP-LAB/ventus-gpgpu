package rtcore

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import top.parameters._

/** Contract test for RT Local SRAM visibility.
  *
  * A shader is allowed to inspect its hit records only after the scheduler
  * publishes traversal completion.  `drained` is the local-state side of that
  * boundary, so it must follow every candidate/committed record write.  A
  * nested trace also must clear the prior trace's records before its own
  * completion becomes visible.
  */
class RtLocalStateTest extends AnyFreeSpec with ChiselScalatestTester {
  "nested gather cannot steal a resident warp or expand its mask" in {
    test(new RtLocalRayState) { dut =>
      idle(dut)
      admit(dut, restart = false, mask = 3)
      dut.io.restart.valid.poke(true.B)
      dut.io.restart.bits.warpId.poke(4.U)
      dut.io.restart.bits.activeMask.poke(3.U)
      dut.io.restart.ready.expect(false.B)
      dut.clock.step(5)
      dut.io.restart.bits.warpId.poke(3.U)
      dut.io.restart.bits.activeMask.poke(7.U)
      dut.io.restart.ready.expect(false.B)
      dut.clock.step(5)
      dut.io.restart.bits.activeMask.poke(1.U)
      dut.io.restart.ready.expect(true.B)
      dut.clock.step()
      dut.io.restart.valid.poke(false.B)
      dut.io.request.bits.warpId.expect(3.U)
      dut.io.laneOut.ready.poke(false.B)
      completeRayGather(dut)
      dut.io.laneOut.valid.expect(true.B)
      dut.io.laneOut.bits.laneId.expect(0.U)
      dut.io.laneOut.ready.poke(true.B)
      dut.clock.step()
      dut.io.laneOut.valid.expect(false.B)
    }
  }

  private val activeLane0 = 1
  private val hitWords = RtAbiLayout.HitRecordWordCount
  private val candidateBase = RtAbiLayout.LocalCandidateHitRecordBaseBytes / RtAbiLayout.AbiWordBytes
  private val committedBase = RtAbiLayout.LocalCommittedHitRecordBaseBytes / RtAbiLayout.AbiWordBytes

  private def idle(dut: RtLocalRayState): Unit = {
    dut.io.admission.valid.poke(false.B)
    dut.io.admission.bits.warpId.poke(0.U)
    dut.io.admission.bits.activeMask.poke(0.U)
    dut.io.restart.valid.poke(false.B)
    dut.io.restart.bits.warpId.poke(0.U)
    dut.io.restart.bits.activeMask.poke(0.U)
    dut.io.laneOut.ready.poke(true.B)
    dut.io.resume.valid.poke(false.B)
    dut.io.resume.bits.activeMask.poke(0.U)
    dut.io.resumeOut.ready.poke(true.B)
    dut.io.raySetup.valid.poke(false.B)
    dut.io.raySetup.bits.laneId.poke(0.U)
    dut.io.rayTmaxUpdate.valid.poke(false.B)
    dut.io.rayTmaxUpdate.bits.laneId.poke(0.U)
    dut.io.rayTmaxUpdate.bits.tmax.poke(0.U)
    dut.io.nodeRayLookup.laneId.poke(0.U)
    dut.io.issueRayLookup.laneId.poke(0.U)
    dut.io.resultRayLookup.laneId.poke(0.U)
    dut.io.hitUpdate.valid.poke(false.B)
    dut.io.hitUpdate.bits.laneId.poke(0.U)
    dut.io.hitUpdate.bits.committed.poke(false.B)
    dut.io.hitUpdate.bits.dirty.poke(0.U)
    dut.io.hitUpdate.bits.data.foreach(_.poke(0.U))
    dut.io.finish.valid.poke(false.B)
    dut.io.finish.bits.terminalMask.poke(0.U)
    dut.io.drained.ready.poke(false.B)
    dut.io.retire.valid.poke(false.B)
    dut.io.retire.bits.poke(0.U)
    dut.io.request.ready.poke(false.B)
    dut.io.response.valid.poke(false.B)
    dut.io.response.bits.readData.foreach(_.poke(0.U))
  }

  private def admit(dut: RtLocalRayState, restart: Boolean, mask: Int = activeLane0): Unit = {
    val port = if (restart) dut.io.restart else dut.io.admission
    port.bits.warpId.poke(3.U)
    port.bits.activeMask.poke(mask.U)
    port.valid.poke(true.B)
    port.ready.expect(true.B)
    dut.clock.step()
    port.valid.poke(false.B)
  }

  /** Supply the fixed 15-word input record exactly as the Local SRAM does.
    * Completion is only meaningful after this normal gather has completed. */
  private def completeRayGather(dut: RtLocalRayState): Unit = {
    dut.io.request.ready.poke(true.B)
    for (offset <- 0 until 15) {
      dut.io.request.valid.expect(true.B)
      dut.io.request.bits.write.expect(false.B)
      dut.io.request.bits.fieldId.expect((RtAbiLayout.SlotAccelLo + offset).U)
      dut.clock.step()
      dut.io.response.ready.expect(true.B)
      dut.io.response.bits.readData.foreach(_.poke(0.U))
      dut.io.response.valid.poke(true.B)
      dut.clock.step()
      dut.io.response.valid.poke(false.B)
    }
    dut.io.request.valid.expect(false.B)
  }

  private def finishAndCollectWrites(dut: RtLocalRayState): Seq[(BigInt, BigInt, BigInt)] = {
    dut.io.finish.bits.terminalMask.poke(activeLane0.U)
    dut.io.finish.valid.poke(true.B)
    dut.io.finish.ready.expect(true.B)
    dut.clock.step()
    dut.io.finish.valid.poke(false.B)

    dut.io.request.ready.poke(true.B)
    var writes = Vector.empty[(BigInt, BigInt, BigInt)]
    var cycles = 0
    while (!dut.io.drained.valid.peek().litToBoolean && cycles < 64) {
      if (dut.io.request.valid.peek().litToBoolean) {
        dut.io.request.bits.write.expect(true.B)
        writes :+= (
          dut.io.request.bits.fieldId.peek().litValue,
          dut.io.request.bits.laneMask.peek().litValue,
          dut.io.request.bits.writeData(0).peek().litValue)
      }
      dut.clock.step()
      cycles += 1
    }
    assert(cycles < 64, "Local hit records never drained")
    writes
  }

  "flushes a hit record before completion and clears it before a restarted completion" in {
    test(new RtLocalRayState) { dut =>
      idle(dut)
      dut.clock.step()

      admit(dut, restart = false)
      completeRayGather(dut)

      val hitWordsData = (0 until hitWords).map(word => BigInt(0x100 + word))
      dut.io.hitUpdate.bits.laneId.poke(0.U)
      dut.io.hitUpdate.bits.committed.poke(true.B)
      dut.io.hitUpdate.bits.dirty.poke(((BigInt(1) << hitWords) - 1).U)
      dut.io.hitUpdate.bits.data.zip(hitWordsData).foreach { case (data, value) => data.poke(value.U) }
      dut.io.hitUpdate.valid.poke(true.B)
      dut.io.hitUpdate.ready.expect(true.B)
      dut.clock.step()
      dut.io.hitUpdate.valid.poke(false.B)

      val firstWrites = finishAndCollectWrites(dut)
      // `drained` is not allowed while the SRAM writes are still being issued.
      dut.io.drained.valid.expect(true.B)
      assert(firstWrites.length == 2 * hitWords)
      firstWrites.zipWithIndex.foreach { case ((field, laneMask, data), index) =>
        assert(laneMask == activeLane0)
        if (index < hitWords) {
          assert(field == candidateBase + index)
          assert(data == 0, s"candidate word $index was not cleared")
        } else {
          val word = index - hitWords
          assert(field == committedBase + word)
          assert(data == hitWordsData(word), s"committed word $word did not reach SRAM")
        }
      }

      dut.io.drained.ready.poke(true.B)
      dut.clock.step()
      dut.io.drained.ready.poke(false.B)
      dut.io.request.ready.poke(false.B)

      // A closest-hit/miss shader may issue a nested trace before release.
      // That new trace must not expose the first trace's committed hit record.
      admit(dut, restart = true)
      completeRayGather(dut)
      val restartedWrites = finishAndCollectWrites(dut)
      dut.io.drained.valid.expect(true.B)
      assert(restartedWrites.length == 2 * hitWords)
      restartedWrites.zipWithIndex.foreach { case ((field, laneMask, data), index) =>
        assert(laneMask == activeLane0)
        val expectedField = if (index < hitWords) candidateBase + index else committedBase + index - hitWords
        assert(field == expectedField)
        assert(data == 0, s"restart left stale hit data in field $field")
      }
    }
  }

  "keeps the session mask while a nested trace gathers only its divergent lanes" in {
    test(new RtLocalRayState) { dut =>
      val sessionMask = 3
      val childMask = 2
      idle(dut)
      dut.clock.step()

      admit(dut, restart = false, mask = sessionMask)
      // A nested command replaces the pending gather with its own divergent
      // mask, but must not shrink the session that eventual finish/release
      // flushes from RT Local SRAM.
      admit(dut, restart = true, mask = childMask)
      dut.io.finish.bits.terminalMask.poke(childMask.U)
      dut.io.finish.valid.poke(true.B)
      dut.io.finish.ready.expect(false.B)
      dut.io.finish.bits.terminalMask.poke(sessionMask.U)
      dut.io.finish.ready.expect(true.B)
      dut.io.finish.valid.poke(false.B)

      dut.io.laneOut.ready.poke(false.B)
      completeRayGather(dut)
      dut.io.laneOut.valid.expect(true.B)
      dut.io.laneOut.bits.laneId.expect(1.U)
      dut.io.laneOut.ready.poke(true.B)
      dut.clock.step()
      dut.io.laneOut.valid.expect(false.B)
    }
  }
}

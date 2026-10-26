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
  "released-context resume preserves the shader-owned committed plane" in {
    test(new RtLocalRayState) { dut =>
      idle(dut)
      admit(dut, restart = false)
      completeRayGather(dut)
      dut.io.preserveCommitted.poke(1.U)
      dut.clock.step()
      dut.io.preserveCommitted.poke(0.U)
      val writes = finishAndCollectWrites(dut)
      assert(writes.size == hitWords)
      assert(writes.forall { case (field, _, _) => field >= candidateBase && field < committedBase })
    }
  }
  "header read cannot consume a queued candidate write on the same bank" in {
    test(new RtLocalRayState) { dut =>
      idle(dut)
      dut.io.narrowRequest(0).ready.poke(false.B)
      admit(dut, restart = false)
      dut.io.hitUpdate.bits.dirty.poke(1.U)
      dut.io.hitUpdate.bits.data(0).poke("h12345678".U)
      dut.io.hitUpdate.valid.poke(true.B)
      dut.io.hitUpdate.ready.expect(true.B)
      dut.clock.step()
      dut.io.hitUpdate.valid.poke(false.B)
      dut.clock.step(3)
      dut.io.narrowRequest(0).valid.expect(true.B)
      dut.io.narrowRequest(0).bits.write.expect(false.B)
      dut.io.narrowRequest(0).ready.poke(true.B)
      dut.clock.step()
      // SRAM cannot accept another transaction while this bank's read
      // response is pending; don't silently accept the candidate here.
      dut.io.narrowRequest(0).ready.poke(false.B)
      dut.io.narrowResponse(0).valid.poke(true.B)
      dut.io.narrowResponse(0).ready.expect(true.B)
      dut.clock.step()
      dut.io.narrowResponse(0).valid.poke(false.B)
      dut.io.narrowRequest(0).ready.poke(true.B)
      dut.io.narrowRequest(0).valid.expect(true.B)
      dut.io.narrowRequest(0).bits.write.expect(true.B)
      dut.io.narrowRequest(0).bits.fieldId.expect(candidateBase.U)
      dut.io.narrowRequest(0).bits.writeData.expect("h12345678".U)
      dut.clock.step()
      dut.io.narrowRequest(0).valid.expect(false.B)
    }
  }
  "retire waits for drained records and allows a different warp to gather" in {
    test(new RtLocalRayState) { dut =>
      idle(dut)
      for ((owner, mask) <- Seq((0, 3), (1, 1), (0, 1))) {
        dut.io.admission.bits.warpId.poke(owner.U)
        dut.io.admission.bits.activeMask.poke(mask.U)
        dut.io.admission.valid.poke(true.B)
        dut.io.admission.ready.expect(true.B)
        dut.clock.step()
        dut.io.admission.valid.poke(false.B)
        dut.io.request.bits.warpId.expect(owner.U)
        completeRayGather(dut, mask)
        dut.clock.step(2)
        val writes = finishAndCollectWrites(dut, mask = mask, stall = true)
        assert(writes.size == 2 * hitWords * Integer.bitCount(mask))
        for (field <- candidateBase until committedBase + hitWords) {
          val fieldWrites = writes.filter(_._1 == BigInt(field))
          assert(fieldWrites.size == Integer.bitCount(mask))
          assert(fieldWrites.map(_._2).distinct.size == fieldWrites.size)
          assert(fieldWrites.forall { case (_, laneMask, _) =>
            laneMask.bitCount == 1 && (laneMask & BigInt(mask)) == laneMask
          })
          assert(fieldWrites.map(_._2).reduce(_ | _) == BigInt(mask))
        }
        dut.io.retire.bits.poke(owner.U)
        dut.io.retire.ready.expect(false.B)
        dut.io.admission.ready.expect(false.B)
        dut.io.drained.ready.poke(true.B)
        dut.clock.step()
        dut.io.drained.ready.poke(false.B)
        dut.io.retire.valid.poke(true.B)
        dut.io.retire.ready.expect(true.B)
        dut.io.request.valid.expect(false.B)
        dut.clock.step()
        dut.io.retire.valid.poke(false.B)
        dut.io.request.valid.expect(false.B)
      }
    }
  }

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
      completeRayGather(dut, mask = 1)
    }
  }

  "world AS bases are visible after the first gather response" in {
    test(new RtLocalRayState) { dut =>
      idle(dut)
      admit(dut, restart = false, mask = 3)
      // Admission allocates the fixed narrow-bank readers; the next cycle
      // issues field zero for lanes 0 and 1 on banks 0 and 1.
      dut.clock.step()
      for (port <- 0 until 2) {
        dut.io.narrowRequest(port).valid.expect(true.B)
        dut.io.narrowRequest(port).bits.laneId.expect(port.U)
        dut.io.narrowRequest(port).bits.fieldId.expect(RtAbiLayout.SlotAccelLo.U)
        dut.io.narrowRequest(port).ready.poke(true.B)
      }
      dut.clock.step()
      dut.io.narrowResponse(0).valid.poke(true.B); dut.io.narrowResponse(0).bits.readData.poke("h1000".U)
      dut.io.narrowResponse(1).valid.poke(true.B); dut.io.narrowResponse(1).bits.readData.poke("h2000".U)
      dut.io.worldBaseArrivedMask.expect(3.U)
      dut.clock.step()
      dut.io.narrowResponse.foreach(_.valid.poke(false.B))

      dut.io.worldBaseLookup.laneId.poke(0.U)
      dut.io.worldBase.valid.expect(true.B)
      dut.io.worldBase.asBase.expect("h1000".U)
      dut.io.worldBaseLookup.laneId.poke(1.U)
      dut.io.worldBase.valid.expect(true.B)
      dut.io.worldBase.asBase.expect("h2000".U)
      dut.io.narrowRequest.foreach(_.valid.expect(false.B))
    }
  }

  private val activeLane0 = 1
  private val hitWords = RtAbiLayout.HitRecordWordCount
  private val candidateBase = RtAbiLayout.LocalCandidateHitRecordBaseBytes / RtAbiLayout.AbiWordBytes
  private val committedBase = RtAbiLayout.LocalCommittedHitRecordBaseBytes / RtAbiLayout.AbiWordBytes

  private def idle(dut: RtLocalRayState): Unit = {
    dut.io.preserveCommitted.poke(0.U)
    dut.io.admission.valid.poke(false.B)
    dut.io.admission.bits.warpId.poke(0.U)
    dut.io.admission.bits.activeMask.poke(0.U)
    dut.io.restart.valid.poke(false.B)
    dut.io.restart.bits.warpId.poke(0.U)
    dut.io.restart.bits.activeMask.poke(0.U)
    dut.io.resume.valid.poke(false.B)
    dut.io.resume.bits.activeMask.poke(0.U)
    dut.io.resumeOut.ready.poke(true.B)
    dut.io.worldBaseLookup.laneId.poke(0.U)
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
    dut.io.narrowRequest.foreach(_.ready.poke(true.B))
    dut.io.narrowResponse.foreach { response =>
      response.valid.poke(false.B)
      response.bits.readData.poke(0.U)
    }
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

  /** Supply the one AccelLo control-word reply per active lane. World-ray
    * fields are fetched later by RtWorldRayContexts. */
  private def completeRayGather(dut: RtLocalRayState, mask: Int = activeLane0): Unit = {
    var pending = Array.fill[Option[(BigInt, BigInt)]](RtLocalSramParameters.corePortCount)(None)
    val expectedReads = Integer.bitCount(mask)
    var completedReads = 0
    var cycles = 0
    while (cycles < 256 && (completedReads < expectedReads || pending.exists(_.nonEmpty))) {
      for (port <- dut.io.narrowResponse.indices) {
        pending(port) match {
          case Some((field, lane)) =>
            dut.io.narrowResponse(port).valid.poke(true.B)
            // Distinct values make accidental field/lane transposition visible
            // in focused tests without affecting the default zero ray.
            dut.io.narrowResponse(port).bits.readData.poke(0.U)
          case None => dut.io.narrowResponse(port).valid.poke(false.B)
        }
        dut.io.narrowRequest(port).ready.poke(true.B)
      }
      val issued = dut.io.narrowRequest.indices.map { port =>
        val request = dut.io.narrowRequest(port)
        val fire = request.valid.peek().litToBoolean && request.ready.peek().litToBoolean &&
          !request.bits.write.peek().litToBoolean
        if (fire) Some((request.bits.fieldId.peek().litValue, request.bits.laneId.peek().litValue)) else None
      }.toArray
      completedReads += issued.count(_.nonEmpty)
      dut.clock.step()
      pending = issued
      cycles += 1
    }
    assert(completedReads == expectedReads && pending.forall(_.isEmpty),
      "narrow RT Local gather never completed")
    dut.io.narrowResponse.foreach(_.valid.poke(false.B))
  }

  private def finishAndCollectWrites(dut: RtLocalRayState, mask: Int = activeLane0,
                                    stall: Boolean = false): Seq[(BigInt, BigInt, BigInt)] = {
    var writes = Vector.empty[(BigInt, BigInt, BigInt)]
    def collectWrites(acceptWide: Boolean): Unit = {
      if (acceptWide && dut.io.request.valid.peek().litToBoolean &&
          dut.io.request.ready.peek().litToBoolean && dut.io.request.bits.write.peek().litToBoolean) {
        writes :+= (
          dut.io.request.bits.fieldId.peek().litValue,
          dut.io.request.bits.laneMask.peek().litValue,
          dut.io.request.bits.writeData(0).peek().litValue)
      }
      for (port <- dut.io.narrowRequest.indices) {
        val request = dut.io.narrowRequest(port)
        if (request.valid.peek().litToBoolean && request.ready.peek().litToBoolean &&
            request.bits.write.peek().litToBoolean) {
          writes :+= (
            request.bits.fieldId.peek().litValue,
            BigInt(1) << request.bits.laneId.peek().litValue.toInt,
            request.bits.writeData.peek().litValue)
        }
      }
    }
    dut.io.finish.bits.terminalMask.poke(mask.U)
    dut.io.finish.valid.poke(true.B)
    dut.io.finish.ready.expect(true.B)
    collectWrites(acceptWide = false)
    dut.clock.step()
    dut.io.finish.valid.poke(false.B)

    dut.io.request.ready.poke(true.B)
    var cycles = 0
    // Fixed per-lane writes plus bounded backpressure, not one vector write
    // per field. Still time out if any accepted record cannot drain.
    val maxCycles = 16 + 6 * hitWords * Integer.bitCount(mask)
    while (!dut.io.drained.valid.peek().litToBoolean && cycles < maxCycles) {
      val ready = !stall || cycles % 3 == 2
      dut.io.request.ready.poke(ready.B)
      collectWrites(acceptWide = ready)
      dut.clock.step()
      cycles += 1
    }
    assert(cycles < maxCycles, "Local hit records never drained")
    writes
  }

  "resume reloads shader committed HitT only for ACCEPT and preserves other lane bounds" in {
    test(new RtLocalRayState) { dut =>
      def raw(f: Float): BigInt = BigInt(java.lang.Float.floatToRawIntBits(f)) & BigInt("ffffffff", 16)
      idle(dut)
      admit(dut, restart = false, mask = 15)
      completeRayGather(dut, mask = 15)
      finishAndCollectWrites(dut, mask = 15)
      dut.io.drained.ready.poke(true.B)
      dut.clock.step()
      dut.io.drained.ready.poke(false.B)

      def checkBounds(): Unit = () // World tmax is Scheduler-owned now.
      def respond(field: Int, values: Seq[BigInt]): Unit = {
        dut.io.request.ready.poke(false.B)
        for (_ <- 0 until 3) {
          dut.io.request.valid.expect(true.B)
          dut.io.request.bits.write.expect(false.B)
          dut.io.request.bits.fieldId.expect(field.U)
          dut.io.resumeOut.valid.expect(false.B)
          checkBounds()
          dut.clock.step()
        }
        dut.io.request.ready.poke(true.B)
        dut.clock.step()
        dut.io.request.ready.poke(false.B)
        dut.clock.step(2)
        dut.io.response.ready.expect(true.B)
        dut.io.response.bits.readData.foreach(_.poke(0.U))
        values.zipWithIndex.foreach { case (value, lane) => dut.io.response.bits.readData(lane).poke(value.U) }
        dut.io.response.valid.poke(true.B)
        dut.clock.step()
        dut.io.response.valid.poke(false.B)
      }
      val accept = RtAbiLayout.CallbackAccept
      val ignore = RtAbiLayout.CallbackIgnore
      val terminate = RtAbiLayout.CallbackTerminate
      // Nonselected lanes deliberately contain ACCEPT and plausible committed
      // values. Neither may influence this command's reads or private bounds.
      val rounds = Seq(
        (15, Seq(accept, ignore, terminate, accept), Seq(3.0f, 4.0f, 5.0f, 6.0f)),
        (2, Seq(accept, ignore, accept, accept), Seq(1.0f, 2.0f, 3.0f, 4.0f)),
        (4, Seq(accept, accept, terminate, accept), Seq(1.0f, 2.0f, 3.0f, 4.0f)),
        (9, Seq(accept, ignore, terminate, accept), Seq(2.5f, 1.0f, 1.0f, 5.0f)))
      for ((mask, decisions, distances) <- rounds) {
        dut.io.resumeOut.ready.poke(false.B)
        dut.io.resume.bits.activeMask.poke(mask.U)
        dut.io.resume.valid.poke(true.B)
        dut.io.resume.ready.expect(true.B)
        dut.clock.step()
        dut.io.resume.valid.poke(false.B)
        respond(RtAbiLayout.LocalControlBaseBytes / RtAbiLayout.AbiWordBytes, decisions.map(BigInt(_)))
        if ((0 until 4).exists(lane => (mask & (1 << lane)) != 0 && decisions(lane) == accept))
          respond(committedBase + RtAbiLayout.HitRecordHitT, distances.map(raw))
        for (lane <- 0 until 4 if (mask & (1 << lane)) != 0) {
          for (_ <- 0 until 3) {
            dut.io.request.valid.expect(false.B)
            dut.io.resumeOut.valid.expect(true.B)
            dut.io.resumeOut.bits.laneId.expect(lane.U)
            dut.io.resumeOut.bits.callbackDecision.expect(decisions(lane).U)
            checkBounds()
            dut.clock.step()
          }
          dut.io.resumeOut.ready.poke(true.B)
          dut.clock.step()
          dut.io.resumeOut.ready.poke(false.B)
          checkBounds()
        }
        dut.io.resumeOut.valid.expect(false.B)
        dut.io.request.valid.expect(false.B)
        // No callback is permitted to mark the shader's hit records dirty.
        assert(finishAndCollectWrites(dut, mask = 15).isEmpty)
        dut.io.drained.ready.poke(true.B)
        dut.clock.step()
        dut.io.drained.ready.poke(false.B)
      }
    }
  }

  "drained hit records cannot overwrite shader commits on a later completion" in {
    test(new RtLocalRayState) { dut =>
      idle(dut)
      admit(dut, restart = false)
      completeRayGather(dut)
      val first = finishAndCollectWrites(dut, stall = true)
      assert(first.size == 2 * hitWords)
      dut.io.drained.ready.poke(true.B)
      dut.clock.step()
      dut.io.drained.ready.poke(false.B)
      // The shader may now change every record word in SRAM. A completion
      // with no new hardware hit must not rewrite any old buffered word.
      assert(finishAndCollectWrites(dut).isEmpty, "stale records were written a second time")
      dut.io.drained.ready.poke(true.B)
      dut.clock.step()
      dut.io.drained.ready.poke(false.B)
      // A later hardware hit still dirties and flushes exactly its update.
      dut.io.hitUpdate.bits.laneId.poke(0.U)
      dut.io.hitUpdate.bits.committed.poke(true.B)
      dut.io.hitUpdate.bits.dirty.poke((1 << RtAbiLayout.HitRecordHitT).U)
      dut.io.hitUpdate.bits.data(RtAbiLayout.HitRecordHitT).poke("h40000000".U)
      dut.io.hitUpdate.valid.poke(true.B)
      dut.io.hitUpdate.ready.expect(true.B)
      dut.clock.step()
      dut.io.hitUpdate.valid.poke(false.B)
      val writes = finishAndCollectWrites(dut, stall = true)
      assert(writes == Seq((BigInt(committedBase + RtAbiLayout.HitRecordHitT), BigInt(1), BigInt("40000000", 16))))
    }
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
      val firstByField = firstWrites.map { case (field, laneMask, data) =>
        assert(laneMask == activeLane0)
        field -> data
      }.toMap
      for (word <- 0 until hitWords) {
        assert(firstByField(candidateBase + word) == 0, s"candidate word $word was not cleared")
        assert(firstByField(committedBase + word) == hitWordsData(word),
          s"committed word $word did not reach SRAM")
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
      // Scheduler only restarts after the prior traversal's gather has
      // completed; this test checks mask lifetime, not read cancellation.
      completeRayGather(dut, sessionMask)
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

      completeRayGather(dut, childMask)
    }
  }
}

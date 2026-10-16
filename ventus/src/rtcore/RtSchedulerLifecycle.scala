package rtcore

import chisel3._
import chisel3.util._
import top.parameters._

/** Install the candidate spill/restore transitions at the scheduler's
  * continuation phase. Registers belong to the scheduler; this is ordered
  * Chisel wiring, not a separately clocked FSM or a shader continuation.
  * Admission is wired before this call and datapath/completion after it.
  */
private[rtcore] object RtSchedulerContinuation {
  def connect(scheduler: RtCoreScheduler): Unit = {
    import scheduler._
    switch(continuationPhase) {
      is(cInitSelect) {
        val ready = VecInit((0 until num_thread).map(lane =>
          continuationMask(lane) && laneState(0)(lane) === sContinuationInit)).asUInt
        when(!continuationMask.orR) { continuationPhase := cIdle }
          .elsewhen(ready.orR) {
            continuationLane := PriorityEncoder(ready)
            continuationPhase := cInitParent
          }
      }
      is(cInitParent) {
        parentAccess.io.command.valid := true.B
        parentAccess.io.command.bits.write := !bindings.io.suspended
        when(parentAccess.io.command.fire) {
          continuationWasSuspended := bindings.io.suspended
          continuationPhase := cInitParentWait
        }
      }
      is(cInitParentWait) {
        parentAccess.io.result.ready := true.B
        when(parentAccess.io.result.fire) {
          when(continuationWasSuspended) {
            val result = parentAccess.io.result.bits
            assert(result.pointer === bindings.io.pointer)
            assert(result.callbackDecision === RtAbiLayout.CallbackAccept.U ||
              result.callbackDecision === RtAbiLayout.CallbackIgnore.U ||
              result.callbackDecision === RtAbiLayout.CallbackTerminate.U)
            continuationPointer := result.pointer
            continuationDecision := result.callbackDecision
            val committed = result.committedMeta0(RtAbiLayout.HitRecordMeta0ValidShift)
            committedValid(0)(continuationLane) := committed
            when(committed) {
              worldTmax(0)(continuationLane) := result.committedHitT
              worldTmaxValid(0) := worldTmaxValid(0) | UIntToOH(continuationLane, num_thread)
            }
            local.io.preserveCommitted := UIntToOH(continuationLane, num_thread)
            continuationPhase := cRestoreStart
          }.otherwise {
            laneState(0)(continuationLane) := sHeaderRequest
            continuationMask := continuationMask & ~UIntToOH(continuationLane, num_thread)
            continuationPhase := cInitSelect
          }
        }
      }
      is(cRestoreStart) {
        spillTransfer.io.command.valid := table.io.quiescent
        when(spillTransfer.io.command.fire) { continuationPhase := cRestoreWait }
      }
      is(cRestoreWait) {
        spillTransfer.io.result.ready := true.B
        when(spillTransfer.io.result.fire) {
          when(continuationDecision === RtAbiLayout.CallbackTerminate.U) {
            laneState(0)(continuationLane) := sTerminal
            laneStatus(0)(continuationLane) := RtAbiLayout.TraversalTerminated.U
            bindings.io.update.valid := true.B
            continuationMask := continuationMask & ~UIntToOH(continuationLane, num_thread)
            continuationPhase := cInitSelect
          }.otherwise {
            continuationImage := spillTransfer.io.result.bits.image
            continuationPhase := cRestoreStack
          }
        }
      }
      is(cRestoreStack) {
        stack.io.restore.valid := true.B
        stack.io.restore.bits.laneId := continuationLane
        stack.io.restore.bits.image := continuationImage
        when(stack.io.restore.fire) {
          bindings.io.update.valid := true.B
          instanceNodeRef(continuationLane) := continuationImage.instanceNodeRef
          val hasObject = continuationImage.baseObjectTags.orR || continuationImage.overflowObjectTags.orR
          when(hasObject) {
            assert(continuationImage.instanceNodeRef =/= RtVtasLayout.InvalidNodeRef.U)
            pendingNodeRef(0)(continuationLane) := continuationImage.instanceNodeRef
            pendingNodeObjectRay(0)(continuationLane) := false.B
            pendingNodeValid(0)(continuationLane) := true.B
            rebuildingInstance(continuationLane) := true.B
            laneState(0)(continuationLane) := sNodePending
          }.otherwise { laneState(0)(continuationLane) := sStackReady }
          continuationMask := continuationMask & ~UIntToOH(continuationLane, num_thread)
          continuationPhase := cInitSelect
        }
      }
      is(cSpillSelect) {
        when(continuationMask.orR) {
          continuationLane := PriorityEncoder(continuationMask)
          continuationPhase := cSpillParent
        }
      }
      is(cSpillParent) {
        parentAccess.io.command.valid := true.B
        parentAccess.io.command.bits.pointerOnly := true.B
        when(parentAccess.io.command.fire) { continuationPhase := cSpillParentWait }
      }
      is(cSpillParentWait) {
        parentAccess.io.result.ready := true.B
        when(parentAccess.io.result.fire) {
          assert(parentAccess.io.result.bits.pointer === bindings.io.pointer)
          assert(!bindings.io.suspended)
          continuationPointer := parentAccess.io.result.bits.pointer
          continuationPhase := cSpillStart
        }
      }
      is(cSpillStart) {
        spillTransfer.io.command.valid := table.io.quiescent
        when(spillTransfer.io.command.fire) { continuationPhase := cSpillWait }
      }
      is(cSpillWait) {
        spillTransfer.io.result.ready := true.B
        when(spillTransfer.io.result.fire) {
          // Only after all three LSU write acknowledgements. Shader callback
          // cannot run until the later context-releasing completion.
          bindings.io.update.valid := true.B
          bindings.io.update.bits.suspended := true.B
          continuationMask := continuationMask & ~UIntToOH(continuationLane, num_thread)
          continuationPhase := cSpillSelect
        }
      }
    }

  }
}

/** Install finish, candidate publication, physical reclaim and software
  * release wiring after traversal results. These distinct lifetimes must not
  * be collapsed: physical reclaim can leave a persistent parent suspended.
  * This helper adds no Module boundary, handshake stages or shader state.
  */
private[rtcore] object RtSchedulerCompletion {
  def connect(scheduler: RtCoreScheduler): Unit = {
    import scheduler._
    val residentWarpCount = RtCorePortParameters.residentWarpCount
    // A completed warp first flushes both hit-record planes.  The write table's
    // drain acknowledgement is the point at which completion may be published.
    val terminalMasks = Wire(Vec(residentWarpCount, UInt(num_thread.W)))
    for (warp <- 0 until residentWarpCount) {
      terminalMasks(warp) := VecInit((0 until num_thread).map(lane =>
        laneState(warp)(lane) === sTerminal)).asUInt
    }
    val finishMasks = VecInit((0 until residentWarpCount).map { warp =>
      sessionActive(warp) && !completionPublished(warp) && !finishIssued(warp) &&
        (terminalMasks(warp) & activeMask(warp)) === activeMask(warp)
    }).asUInt
    val finishWarp = PriorityEncoder(finishMasks)
    local.io.finish.valid := finishMasks.orR
    local.io.finish.bits.terminalMask := activeMask(finishWarp)
    when(local.io.finish.fire) { finishIssued(finishWarp) := true.B }

    val completionPending = RegInit(false.B)
    val completionBits = Reg(new RtWarpCompletion)
    val finalRetirePending = RegInit(false.B)
    val contextRetirePending = RegInit(false.B)
    val releasePending = RegInit(false.B)
    val releaseStackCleared = RegInit(false.B)
    // Keep the retirement staging cycle; this does not invalidate the AS cache.
    val retireDrainReady = RegInit(false.B)
    local.io.drained.ready := !completionPending && !finalRetirePending &&
      continuationPhase === cIdle && table.io.quiescent &&
      localReadArbiter.io.quiescent
    when(local.io.drained.fire) {
      completionBits.warpId := residentOwnerWarpId
      completionBits.issueId := warpIssueId(0)
      completionBits.destinationVd := warpDestination(0)
      completionBits.completionMask := commandMask(0)
      completionBits.laneStatus := laneStatus(0)
      // Include candidates outside the most recent command's sparse mask.
      val candidates = candidateValid(0).asUInt & activeMask(0)
      val spillCandidates = candidates.orR && spillEnabled
      val retainContext = candidates.orR && !spillEnabled
      completionBits.contextReleased := !retainContext
      when(spillCandidates) {
        continuationMask := candidates
        continuationPhase := cSpillSelect
      }.otherwise { completionPending := true.B }
      when(!retainContext && !spillCandidates) {
        contextRetirePending := true.B
        releaseStackCleared := false.B
        retireDrainReady := false.B
      }
    }
    when(continuationPhase === cSpillSelect && !continuationMask.orR) {
      continuationPhase := cIdle
      completionPending := true.B
      contextRetirePending := true.B
      releaseStackCleared := false.B
      retireDrainReady := false.B
    }
    // Opt-in full-app diagnosis: distinguishes a traversal status error from a
    // later shader callback/control-flow error without perturbing normal RTL.
    if (sys.env.get("VENTUS_RTCORE_TRACE").contains("1")) {
      when(local.io.drained.fire) {
        val hitMask = VecInit((0 until num_thread).map(lane =>
          laneStatus(0)(lane) === RtAbiLayout.TraversalCompleteHit.U)).asUInt
        val nonOpaqueMask = VecInit((0 until num_thread).map(lane =>
          laneStatus(0)(lane) === RtAbiLayout.TraversalCandidateNonOpaqueTriangle.U)).asUInt
        val proceduralMask = VecInit((0 until num_thread).map(lane =>
          laneStatus(0)(lane) === RtAbiLayout.TraversalCandidateProceduralAabb.U)).asUInt
        val terminatedMask = VecInit((0 until num_thread).map(lane =>
          laneStatus(0)(lane) === RtAbiLayout.TraversalTerminated.U)).asUInt
        printf(p"[RTCORE trace] drain warp=${Decimal(residentOwnerWarpId)}" +
          p" mask=0x${Hexadecimal(commandMask(0))}" +
          p" hit=0x${Hexadecimal(hitMask)} nonopaque=0x${Hexadecimal(nonOpaqueMask)}" +
          p" procedural=0x${Hexadecimal(proceduralMask)} terminated=0x${Hexadecimal(terminatedMask)}\n")
      }
    }
    // Reclaim before publishing a context-releasing completion. Pipeline holds
    // the reservation until this completion reaches vector writeback.
    io.completion.valid := completionPending && !contextRetirePending
    io.completion.bits := completionBits
    when(io.completion.fire) {
      completionPending := false.B
      completionPublished(0) := !completionBits.contextReleased
      finishIssued(0) := false.B
    }

    // Software release destroys a retained context. A new sparse child trace
    // can have a smaller physical mask than the final reconverged release.
    val retireRequested = releasePending || contextRetirePending
    retiring := retireRequested || finalRetirePending || completionPending || io.release.valid
    val releaseAllowed = sessionActive(0) && completionPublished(0) && !completionPending &&
      io.release.bits.warpId === residentOwnerWarpId &&
      (io.release.bits.activeMask & activeMask(0)) === activeMask(0)
    io.release.ready := releaseAllowed && !retireRequested && !finalRetirePending
    when(io.release.fire) {
      releasePending := true.B
      releaseStackCleared := false.B
    }
    when(io.release.fire) {
      assert(io.release.bits.warpId === residentOwnerWarpId,
        "release warp must match the resident RT session")
      assert((io.release.bits.activeMask & activeMask(0)) === activeMask(0),
        "release must cover the complete retained RT context mask")
      for (lane <- 0 until num_thread) when(activeMask(0)(lane)) {
        assert(laneState(0)(lane) === sTerminal,
          "release may retire only a completed retained lane")
      }
    }
    // A nested traversal starts from the shader's newly written local ray and
    // must not inherit stale DFS entries from the preceding opaque/miss trace.
    // Its Local gather begins in the following cycle, so this same-cycle clear
    // is complete before the new header can initialise a root node.
    stack.io.clear.valid := (retireRequested && !releaseStackCleared) || restartStackClearRequest
    stack.io.clear.bits.laneMask := Mux(retireRequested, activeMask(0), io.command.bits.activeMask)
    when(stack.io.clear.fire && retireRequested) { releaseStackCleared := true.B }
    when(retireRequested && releaseStackCleared && !finalRetirePending) {
      finalRetirePending := true.B
      for (lane <- 0 until num_thread) when(activeMask(0)(lane)) {
        laneState(0)(lane) := sIdle
        candidateValid(0)(lane) := false.B
        committedValid(0)(lane) := false.B
        laneStatus(0)(lane) := 0.U
        boxDeferredCount(0)(lane) := 0.U
        boxPushedCount(0)(lane) := 0.U
        pendingNodeValid(0)(lane) := false.B
      }
    }
    // Auto-reclaim never emits a software release acknowledgement. In both
    // paths, retire local ownership only after pending AS traffic has drained.
    local.io.retire.valid := finalRetirePending && retireDrainReady && table.io.quiescent &&
      (contextRetirePending || io.releaseAck.ready)
    local.io.retire.bits := residentOwnerWarpId
    // An RT context retire drains its waiters but must not invalidate returned
    // AS lines: the table is an SM-local cache shared by subsequent contexts.
    table.io.clear.valid := false.B
    table.io.clear.bits := true.B
    when(finalRetirePending && !retireDrainReady) { retireDrainReady := true.B }
    io.releaseAck.valid := releasePending && finalRetirePending && retireDrainReady &&
      local.io.retire.ready && table.io.quiescent
    io.releaseAck.bits.warpId := residentOwnerWarpId
    io.releaseAck.bits.releasedMask := activeMask(0)
    when(local.io.retire.fire) {
      when(contextRetirePending) {
        assert(completionPending && completionBits.contextReleased,
          "automatic reclamation must retain its context-releasing completion")
        assert(!(candidateValid(0).asUInt & activeMask(0)).orR,
          "automatic reclamation must not discard retained candidates")
      }
      retireDrainReady := false.B
      releasePending := false.B
      contextRetirePending := false.B
      releaseStackCleared := false.B
      sessionActive(0) := false.B
      finishIssued(0) := false.B
      completionPublished(0) := false.B
      activeMask(0) := 0.U
      commandMask(0) := 0.U
      finalRetirePending := false.B
      for (lane <- 0 until num_thread) {
        laneState(0)(lane) := sIdle
        pendingNodeValid(0)(lane) := false.B
      }
    }
  }
}

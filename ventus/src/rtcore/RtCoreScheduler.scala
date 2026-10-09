package rtcore

import chisel3._
import chisel3.util._
import hardfloat._
import top.parameters._

/**
  * Warp/lane traversal controller.
  *
  * It is the RTcore control boundary: commands start RT Local SRAM gather,
  * local-ready notifications feed setup and traversal, and only this module
  * chooses an IU destination. RtMemTable owns AS traffic; RtSpillTransfer
  * borrows the LSU only after the table and the physical session drain. The
  * stack carries a world/object tag so one lane can return from a BLAS to a
  * TLAS sibling without ever storing object rays in RT Local SRAM.
  */
class RtCoreScheduler extends Module {
  import RtCoreSchedulerParameters._

  private val residentWarpCount = RtCorePortParameters.residentWarpCount
  private val residentWarpIdWidth = RtCorePortParameters.residentWarpIdWidth
  // This elaboration-only trace records ray coherence offline.  It must never
  // become part of the normal scheduling/PMU datapath.
  private val coherenceRecordTrace = sys.env.get("VENTUS_RTCORE_RECORD_TRACE").contains("1")

  val io = IO(new RtCoreIO)
  val boxIssue = IO(Vec(boxUnitCount, Decoupled(new RtBox4IntersectionInput)))
  val boxResult = IO(Flipped(Vec(boxUnitCount, Decoupled(new RtBox4IntersectionOutput))))
  val triangleIssue = IO(Vec(triangleUnitCount, Decoupled(new RtTriangleIntersectionInput)))
  val triangleResult = IO(Flipped(Vec(triangleUnitCount, Decoupled(new RtTriangleIntersectionOutput))))

  // Datapath services and continuation transaction defaults.
  val table = Module(new RtMemTable)
  val local = Module(new RtLocalRayState)
  val worldContexts = Module(new RtWorldRayContexts)
  val stack = Module(new RtTraversalStack)
  val bindings = Module(new RtSpillBindings)
  val parentAccess = Module(new RtSpillParentAccess)
  val spillTransfer = Module(new RtSpillTransfer)
  val Seq(cIdle, cInitSelect, cInitParent, cInitParentWait, cRestoreStart,
    cRestoreWait, cRestoreStack, cSpillSelect, cSpillParent, cSpillParentWait,
    cSpillStart, cSpillWait) = Enum(12)
  val continuationPhase = RegInit(cIdle)
  val continuationLane = RegInit(0.U(log2Ceil(num_thread).W))
  val continuationMask = RegInit(0.U(num_thread.W))
  val continuationPointer = Reg(UInt(64.W))
  val continuationDecision = Reg(UInt(xLen.W))
  val continuationImage = Reg(new RtSpillSlotImage)
  val continuationWasSuspended = Reg(Bool())
  val spillPool = RegInit(0.U.asTypeOf(new RtSpillPoolDescriptor))
  val rebuildingInstance = RegInit(VecInit(Seq.fill(num_thread)(false.B)))
  val instanceNodeRef = Reg(Vec(num_thread, UInt(32.W)))
  val initializingContinuation = continuationPhase >= cInitSelect && continuationPhase <= cRestoreStack
  val spillEnabled = spillPool.spillPoolBase =/= 0.U && spillPool.dispatchEpoch =/= 0.U &&
    spillPool.slotsPerCore >= RtSpillSlotLayout.slotsPerCore.U
  bindings.io.descriptor := spillPool
  bindings.io.coreId := io.coreId
  bindings.io.query.valid := continuationPhase =/= cIdle
  bindings.io.query.bits.rowBase := local.io.targetRowBase(continuationLane)
  bindings.io.query.bits.laneId := local.io.targetLane(continuationLane)
  bindings.io.update.valid := false.B
  bindings.io.update.bits.parent := bindings.io.query.bits
  bindings.io.update.bits.suspended := false.B
  parentAccess.io.command.valid := false.B
  parentAccess.io.command.bits.rowBase := local.io.targetRowBase(continuationLane)
  parentAccess.io.command.bits.laneId := local.io.targetLane(continuationLane)
  parentAccess.io.command.bits.write := false.B
  parentAccess.io.command.bits.pointerOnly := false.B
  parentAccess.io.command.bits.pointer := bindings.io.pointer
  parentAccess.io.result.ready := false.B
  spillTransfer.io.command.valid := false.B
  spillTransfer.io.command.bits.pointer := continuationPointer
  spillTransfer.io.command.bits.restore := initializingContinuation
  spillTransfer.io.command.bits.terminate := initializingContinuation &&
    continuationDecision === RtAbiLayout.CallbackTerminate.U
  stack.io.snapshotLane := continuationLane
  spillTransfer.io.command.bits.image := stack.io.snapshot
  spillTransfer.io.command.bits.image.instanceNodeRef := instanceNodeRef(continuationLane)
  spillTransfer.io.result.ready := false.B
  stack.io.restore.valid := false.B
  stack.io.restore.bits := 0.U.asTypeOf(new RtTraversalStackRestore)
  local.io.preserveCommitted := 0.U
  val raySetup = Module(new RtRaySetup)
  val directSetupRequest = WireDefault(false.B)
  val directSetupLane = WireDefault(0.U(log2Ceil(num_thread).W))
  val directSetupRay = WireDefault(0.U.asTypeOf(new Ray(recorded_float = false)))
  val triangleResolve = Module(new RtTriangleResolve)
  for (port <- 0 until RtMemTableParameters.asReadPortCount)
    table.io.asLineRead(port).entryId := 0.U
  val spillLsuArbiter = Module(new RtSpillLsuArbiter)
  spillLsuArbiter.io.begin := spillTransfer.io.command.fire
  spillLsuArbiter.io.end := spillTransfer.io.result.fire
  spillLsuArbiter.io.tableQuiescent := table.io.quiescent
  spillLsuArbiter.io.asRequest <> table.io.lsuRequest
  table.io.lsuResponse <> spillLsuArbiter.io.asResponse
  spillLsuArbiter.io.spillRequest <> spillTransfer.io.request
  spillTransfer.io.response <> spillLsuArbiter.io.spillResponse
  io.lsuRequest <> spillLsuArbiter.io.request
  spillLsuArbiter.io.response <> io.lsuResponse
  io.localRequest <> local.io.request
  local.io.response <> io.localResponse
  val localReadArbiter = Module(new RtLocalReadArbiter)
  localReadArbiter.io.parentRequest <> parentAccess.io.request
  parentAccess.io.response <> localReadArbiter.io.parentResponse
  localReadArbiter.io.contextRequest <> worldContexts.io.request
  worldContexts.io.response <> localReadArbiter.io.contextResponse
  localReadArbiter.io.localRequest <> local.io.narrowRequest
  local.io.narrowResponse <> localReadArbiter.io.localResponse
  io.localNarrowRequest <> localReadArbiter.io.bankRequest
  localReadArbiter.io.bankResponse <> io.localNarrowResponse

  // Physical lane state. These arrays describe one execution session, not
  // persistent shader parents; LocalState/bindings resolve those separately.
  val Seq(sIdle, sRayLoad, sHeaderRequest, sHeaderPending, sStackReady,
    sNodePending, sBoxInflight, sBoxPush, sTriangleInflight, sTerminal,
    sResumePending, sInstanceTransform, sBlasHeaderRequest, sBlasHeaderPending,
    sAabbCandidate, sContinuationInit) = Enum(16)
  val laneState = RegInit(VecInit(Seq.fill(residentWarpCount)(VecInit(Seq.fill(num_thread)(sIdle)))))
  val laneStatus = Reg(Vec(residentWarpCount, Vec(num_thread, UInt(xLen.W))))
  val committedValid = RegInit(VecInit(Seq.fill(residentWarpCount)(VecInit(Seq.fill(num_thread)(false.B)))))
  val activeMask = RegInit(VecInit(Seq.fill(residentWarpCount)(0.U(num_thread.W))))
  // `activeMask` is the session ownership mask.  A completion only reports
  // the command mask, so a subset of retained candidate lanes may resume.
  val commandMask = RegInit(VecInit(Seq.fill(residentWarpCount)(0.U(num_thread.W))))
  val warpIssueId = Reg(Vec(residentWarpCount, UInt(RtCorePortParameters.issueIdWidth.W)))
  val coherenceEpoch = if (coherenceRecordTrace) Some(RegInit(0.U(32.W))) else None
  val coherenceLaneEpoch = if (coherenceRecordTrace)
    Some(Reg(Vec(residentWarpCount, Vec(num_thread, UInt(32.W))))) else None
  val coherenceLaneDepth = if (coherenceRecordTrace)
    Some(Reg(Vec(residentWarpCount, Vec(num_thread, UInt(RtAbiLayout.TraceMeta1ActiveLevelBits.W))))) else None
  val warpDestination = Reg(Vec(residentWarpCount, UInt(RtCorePortParameters.regidx_totalwidth.W)))
  val sessionActive = RegInit(VecInit(Seq.fill(residentWarpCount)(false.B)))
  val finishIssued = RegInit(VecInit(Seq.fill(residentWarpCount)(false.B)))
  // Completion has been handed to pipe, but the retained RT session still
  // owns its RT Local buffer/stack state until resume or final release.
  val completionPublished = RegInit(VecInit(Seq.fill(residentWarpCount)(false.B)))
  // One bank per lane: unsorted refs while its Box IU is in flight, then
  // reordered in place until all deferred DFS pushes have been accepted.
  val boxChildRefs = Reg(Vec(residentWarpCount, Vec(num_thread, Vec(4, UInt(32.W)))))
  val boxDeferredCount = Reg(Vec(residentWarpCount, Vec(num_thread, UInt(3.W))))
  val boxPushedCount = Reg(Vec(residentWarpCount, Vec(num_thread, UInt(3.W))))
  val boxObjectRay = RegInit(VecInit(Seq.fill(residentWarpCount)(VecInit(Seq.fill(num_thread)(false.B)))))
  val boxIsAabb = RegInit(VecInit(Seq.fill(residentWarpCount)(VecInit(Seq.fill(num_thread)(false.B)))))
  val triangleMeta = Reg(Vec(residentWarpCount, Vec(num_thread, new RtTriangleDispatchMeta)))
  val candidateValid = RegInit(VecInit(Seq.fill(residentWarpCount)(VecInit(Seq.fill(num_thread)(false.B)))))
  // Only a pending procedural record, not a mirror of the two SRAM planes.
  // Capture its metadata at issue; the Box result supplies HitT later.
  val candidateData = Reg(Vec(residentWarpCount, Vec(num_thread,
    Vec(RtAbiLayout.HitRecordWordCount, UInt(xLen.W)))))
  // A lane keeps its popped continuation here until the shared AS acquire
  // port accepts it.  This decouples independent stack pops from single-port
  // AS admission without duplicating AS cacheline data.
  val pendingNodeValid = RegInit(VecInit(Seq.fill(residentWarpCount)(VecInit(Seq.fill(num_thread)(false.B)))))
  val pendingNodeRef = Reg(Vec(residentWarpCount, Vec(num_thread, UInt(32.W))) )
  val pendingNodeObjectRay = RegInit(VecInit(Seq.fill(residentWarpCount)(VecInit(Seq.fill(num_thread)(false.B)))))
  // One private object context per lane is sufficient for VTAS's TLAS->BLAS
  // nesting.  Object-tagged descendants are exhausted before DFS returns to
  // another world-tagged Instance, at which point this entry may be replaced.
  val objectPrepared = RegInit(VecInit(Seq.fill(residentWarpCount)(VecInit(
    Seq.fill(num_thread)(0.U.asTypeOf(new RtPreparedRay))))))
  // Direct BLAS roots do not pass an Instance transform, but Triangle IU
  // still needs RaySetup's shear/kx/ky/kz.  This tiny per-lane fallback is
  // populated only for an actually returned world-space Triangle line.
  val directPrepared = RegInit(VecInit(Seq.fill(residentWarpCount)(VecInit(
    Seq.fill(num_thread)(0.U.asTypeOf(new RtPreparedRay))))))
  val directSetupStarted = RegInit(VecInit(Seq.fill(residentWarpCount)(0.U(num_thread.W))))
  // The only world-ray value that survives a BLAS visit is its mutable near
  // hit bound; world coordinates remain in short-lived on-demand contexts.
  val worldTmax = Reg(Vec(residentWarpCount, Vec(num_thread, UInt(xLen.W))))
  val worldTmaxValid = RegInit(VecInit(Seq.fill(residentWarpCount)(0.U(num_thread.W))))
  val objectBase = Reg(Vec(residentWarpCount, Vec(num_thread, UInt(memReqLen.W))))
  val objectInstanceId = Reg(Vec(residentWarpCount, Vec(num_thread, UInt(xLen.W))))
  val objectCustomIndex = Reg(Vec(residentWarpCount, Vec(num_thread, UInt(24.W))))
  val objectSbtRecordOffset = Reg(Vec(residentWarpCount, Vec(num_thread, UInt(24.W))))
  // This is control state, unlike the lane payload arrays above.  It gates
  // retained-session resume/release admission, so its reset value must be
  // defined even while `sessionActive` is false.
  val residentOwnerWarpId = RegInit(0.U(depth_warp.W))
  when(continuationPhase === cRestoreWait && spillTransfer.io.response.fire &&
    spillTransfer.io.response.bits.lineBase === continuationPointer + 128.U &&
    !spillTransfer.io.response.bits.data(RtSpillSlotLayout.validWord)(0)) {
    printf(p"[RT spill invariant] core=${Decimal(io.coreId)} owner=${Decimal(residentOwnerWarpId)}" +
      p" lane=${Decimal(continuationLane)} parentRow=${Decimal(bindings.io.query.bits.rowBase)}" +
      p" parentLane=${Decimal(bindings.io.query.bits.laneId)}" +
      p" decision=${Decimal(continuationDecision)}\n")
  }

  val instanceTransform = Module(new RtInstanceTransform)
  val objectRaySetup = Module(new RtRaySetup)
  worldContexts.io.targetRowBase := local.io.targetRowBase
  worldContexts.io.targetLane := local.io.targetLane
  worldContexts.io.clear := io.command.fire || io.releaseAck.fire
  for (unit <- 0 until boxUnitCount) {
    worldContexts.io.lookupLane(unit) := 0.U
    worldContexts.io.consume(unit) := false.B
    worldContexts.io.setupLookupLane(unit) := 0.U
    worldContexts.io.setupConsume(unit) := false.B
  }

  private def requestId(warp: UInt, lane: UInt): UInt =
    Cat(0.U((RtCorePortParameters.requestIdWidth - log2Ceil(num_thread)).W), lane)
  private def requestWarp(id: UInt): UInt = 0.U(residentWarpIdWidth.W)
  private def requestLane(id: UInt): UInt = id(log2Ceil(num_thread) - 1, 0)
  private def nodeType(nodeRef: UInt): UInt = nodeRef(2, 0)
  private def nodeOffset(nodeRef: UInt): UInt = nodeRef & RtVtasLayout.NodeRefOffsetMask.U(32.W)
  private def roundRobin(mask: UInt, start: UInt, count: Int): UInt = {
    require(count > 1 && (count & (count - 1)) == 0)
    val rotated = (Cat(mask, mask) >> start)(count - 1, 0)
    (start + PriorityEncoder(rotated))(log2Ceil(count) - 1, 0)
  }
  val nextTriangleResult = RegInit(0.U(log2Ceil(triangleUnitCount).W))

  // A new command either creates a physical session, resumes retained
  // candidate lanes, or starts a nested traversal after an opaque/miss
  // completion.  RT Local control is the callback authority only on resume.
  val commandSlot = 0.U(residentWarpIdWidth.W)
  val retiring = Wire(Bool())
  val freshCommand = !retiring && io.command.bits.activeMask.orR && !sessionActive(commandSlot)
  val retainedCandidateMask = candidateValid(commandSlot).asUInt & io.command.bits.activeMask
  val ownedCommand = !retiring && io.command.bits.warpId === residentOwnerWarpId &&
    (io.command.bits.activeMask & ~activeMask(commandSlot)) === 0.U
  val resumeCommand = ownedCommand && io.command.bits.activeMask.orR && sessionActive(commandSlot) &&
    completionPublished(commandSlot) && retainedCandidateMask.orR
  val restartCommand = ownedCommand && io.command.bits.activeMask.orR && sessionActive(commandSlot) &&
    completionPublished(commandSlot) && !retainedCandidateMask.orR
  val commandAllowed = freshCommand || resumeCommand || restartCommand
  val groupExpectedMembers = (1.U(num_warp_in_a_block.W) << io.command.bits.workgroupWarps) - 1.U
  val groupMappingReady = !io.command.bits.grouped ||
    ((io.command.bits.workgroupMemberValid & groupExpectedMembers) === groupExpectedMembers)
  local.io.admission.valid := io.command.valid && freshCommand
  local.io.admission.bits.warpId := io.command.bits.warpId
  local.io.admission.bits.activeMask := io.command.bits.activeMask
  local.io.admission.bits.grouped := io.command.bits.grouped
  local.io.admission.bits.locator := io.command.bits.locator
  local.io.admission.bits.memberWarp := io.command.bits.workgroupMemberWarp
  local.io.restart.valid := io.command.valid && restartCommand
  local.io.restart.bits.warpId := io.command.bits.warpId
  local.io.restart.bits.activeMask := io.command.bits.activeMask
  // Restart retains the LocalState mapping captured on admission; these
  // fields are intentionally ignored by LocalState for restart, but they
  // must still be driven as a complete Decoupled bundle.
  local.io.restart.bits.grouped := false.B
  local.io.restart.bits.locator := VecInit(Seq.fill(num_thread)(0.U(xLen.W)))
  local.io.restart.bits.memberWarp := VecInit(Seq.fill(num_warp_in_a_block)(0.U(depth_warp.W)))
  local.io.resume.valid := io.command.valid && resumeCommand
  local.io.resume.bits.activeMask := io.command.bits.activeMask
  val restartStackClearRequest = io.command.valid && restartCommand && local.io.restart.ready
  io.command.ready := commandAllowed && groupMappingReady && Mux(freshCommand,
    local.io.admission.ready, Mux(resumeCommand, local.io.resume.ready,
      local.io.restart.ready && stack.io.clear.ready))
  when(io.command.fire) {
    val warp = commandSlot
    if (sys.env.get("VENTUS_RTCORE_DEEP_TRACE").contains("1") ||
        sys.env.get("VENTUS_RTCORE_RECORD_TRACE").contains("1")) {
      printf(p"[RTCORE trace] command warp=${Decimal(io.command.bits.warpId)}" +
        p" issue=${Decimal(io.command.bits.issueId)} mask=0x${Hexadecimal(io.command.bits.activeMask)}\\n")
    }
    when(freshCommand) { residentOwnerWarpId := io.command.bits.warpId }
    when(io.command.bits.grouped) {
      for (lane <- 0 until num_thread) {
        val logicalMember = io.command.bits.locator(lane)(
          log2Ceil(num_thread) + log2Ceil(num_warp_in_a_block) - 1,
          log2Ceil(num_thread))
        when(io.command.bits.activeMask(lane)) {
          assert(logicalMember < io.command.bits.workgroupWarps,
            "group traverse logical TID names a member outside this workgroup")
        }
      }
    }
    commandMask(warp) := io.command.bits.activeMask
    warpIssueId(warp) := io.command.bits.issueId
    if (coherenceRecordTrace) {
      when(freshCommand || restartCommand) {
        coherenceEpoch.get := coherenceEpoch.get + 1.U
        for (lane <- 0 until num_thread) when(io.command.bits.activeMask(lane)) {
          coherenceLaneEpoch.get(warp)(lane) := coherenceEpoch.get
          coherenceLaneDepth.get(warp)(lane) := 0.U
        }
      }
    }
    warpDestination(warp) := io.command.bits.destinationVd
    finishIssued(warp) := false.B
    when(freshCommand || restartCommand) {
      spillPool := io.command.bits.spillPool
      val enabled = io.command.bits.spillPool.spillPoolBase =/= 0.U &&
        io.command.bits.spillPool.dispatchEpoch =/= 0.U &&
        io.command.bits.spillPool.slotsPerCore >= RtSpillSlotLayout.slotsPerCore.U
      continuationPhase := Mux(enabled, cInitSelect, cIdle)
      continuationMask := io.command.bits.activeMask
      for (lane <- 0 until num_thread) when(io.command.bits.activeMask(lane)) {
        instanceNodeRef(lane) := RtVtasLayout.InvalidNodeRef.U
        rebuildingInstance(lane) := false.B
      }
    }
    when(freshCommand) {
      sessionActive(warp) := true.B
      completionPublished(warp) := false.B
      activeMask(warp) := io.command.bits.activeMask
      worldTmaxValid(warp) := 0.U
      directSetupStarted(warp) := 0.U
      for (lane <- 0 until num_thread) {
        when(io.command.bits.activeMask(lane)) {
          laneState(warp)(lane) := sRayLoad
          laneStatus(warp)(lane) := RtAbiLayout.TraversalCompleteMiss.U
          committedValid(warp)(lane) := false.B
          candidateValid(warp)(lane) := false.B
          pendingNodeValid(warp)(lane) := false.B
          directPrepared(warp)(lane).valid := false.B
        }.otherwise {
          laneState(warp)(lane) := sIdle
        }
      }
    }.elsewhen(restartCommand) {
      assert((io.command.bits.activeMask & ~activeMask(warp)) === 0.U,
        "nested traversal mask must be a subset of the resident RT session")
      completionPublished(warp) := false.B
      worldTmaxValid(warp) := worldTmaxValid(warp) & ~io.command.bits.activeMask
      for (lane <- 0 until num_thread) {
        when(io.command.bits.activeMask(lane)) {
          laneState(warp)(lane) := sRayLoad
          laneStatus(warp)(lane) := RtAbiLayout.TraversalCompleteMiss.U
          committedValid(warp)(lane) := false.B
          candidateValid(warp)(lane) := false.B
          boxDeferredCount(warp)(lane) := 0.U
          boxPushedCount(warp)(lane) := 0.U
          boxObjectRay(warp)(lane) := false.B
          pendingNodeValid(warp)(lane) := false.B
          directPrepared(warp)(lane).valid := false.B
        }
      }
      directSetupStarted(warp) := directSetupStarted(warp) & ~io.command.bits.activeMask
    }.otherwise {
      completionPublished(warp) := false.B
      for (lane <- 0 until num_thread) {
        when(io.command.bits.activeMask(lane)) {
          assert(candidateValid(warp)(lane), "resume may select only retained candidate lanes")
          assert(laneState(warp)(lane) === sTerminal,
            "resume lane must be terminal after a published candidate completion")
          laneState(warp)(lane) := sResumePending
        }
      }
    }
  }

  // Before any AS issue, install the software-assigned parent address or
  // restore its already-published continuation. Physical lanes may differ
  // from the persistent parent locations selected by the group locator.
  RtSchedulerContinuation.connect(this)

  // Software callbacks already committed their accepted hit in Local SRAM.
  // LocalState reloads that HitT and updates its private bound on this
  // handshake only for ACCEPT. Resume never copies our stale candidate over
  // the shader's record; IGNORE/TERMINATE do not update the bound.
  local.io.resumeOut.ready := true.B
  val resumeLane = local.io.resumeOut.bits.laneId
  val resumeDecision = local.io.resumeOut.bits.callbackDecision
  val resumeAccept = resumeDecision === RtAbiLayout.CallbackAccept.U
  val resumeIgnore = resumeDecision === RtAbiLayout.CallbackIgnore.U
  val resumeTerminate = resumeDecision === RtAbiLayout.CallbackTerminate.U
  when(local.io.resumeOut.fire) {
    assert(laneState(0)(resumeLane) === sResumePending)
    assert(resumeAccept || resumeIgnore || resumeTerminate,
      "candidate resume requires ACCEPT, IGNORE, or TERMINATE control decision")
    when(resumeAccept || resumeIgnore) {
      laneState(0)(resumeLane) := sStackReady
    }.otherwise {
      laneState(0)(resumeLane) := sTerminal
      laneStatus(0)(resumeLane) := RtAbiLayout.TraversalTerminated.U
    }
    // Track the shader's commit so empty-stack completion reports HIT.
    when(resumeAccept) { committedValid(0)(resumeLane) := true.B }
    candidateValid(0)(resumeLane) := false.B
  }

  // World rays no longer traverse an eager LocalState -> RaySetup mirror.
  // RtWorldRayContexts supplies raw rays to Box/Instance on demand; the
  // retained setup unit is reserved for the direct-BLAS triangle fallback.
  raySetup.io.in.valid := directSetupRequest
  raySetup.io.in.bits := 0.U.asTypeOf(new RtRaySetupInput)
  raySetup.io.in.bits.laneId := directSetupLane
  raySetup.io.in.bits.rayState.origin := directSetupRay.origin
  raySetup.io.in.bits.rayState.tmin := directSetupRay.originalTmin
  raySetup.io.in.bits.rayState.dir := directSetupRay.dir
  raySetup.io.in.bits.rayState.tmax := directSetupRay.extent
  raySetup.io.in.bits.rayState.inv := directSetupRay.inv
  // The diagnostic record includes the logical warp ID as well as the
  // physical resident slot, so offline cohorts never mix same-numbered lanes.
  if (coherenceRecordTrace) {
    when(raySetup.io.in.fire) {
      printf(p"[RTCORE coherence-ray] warp=${Decimal(residentOwnerWarpId)} epoch=${Decimal(coherenceLaneEpoch.get(0)(directSetupLane))}" +
        p" lane=${Decimal(directSetupLane)} depth=${Decimal(coherenceLaneDepth.get(0)(directSetupLane))}" +
        p" active_level=${Decimal(coherenceLaneDepth.get(0)(directSetupLane))}" +
        p" source=triangle o=[${Hexadecimal(directSetupRay.origin.x)},${Hexadecimal(directSetupRay.origin.y)},${Hexadecimal(directSetupRay.origin.z)}]" +
        p" d=[${Hexadecimal(directSetupRay.dir.x)},${Hexadecimal(directSetupRay.dir.y)},${Hexadecimal(directSetupRay.dir.z)}]" +
        p" tmin=${Hexadecimal(directSetupRay.originalTmin)} tmax=${Hexadecimal(directSetupRay.extent)}\n")
    }
  }

  // Stack operation defaults.  Header initialisation, deferred Box4 pushes,
  // and node pop are ordered by the stack module; scheduler only offers one
  // candidate of each kind per cycle.
  stack.io.init.valid := false.B
  stack.io.init.bits := 0.U.asTypeOf(new RtTraversalStackInit)
  stack.io.push.valid := false.B
  stack.io.push.bits := 0.U.asTypeOf(new RtTraversalStackPush)
  stack.io.pop.valid := false.B
  stack.io.pop.bits := 0.U.asTypeOf(new RtTraversalStackPop)
  stack.io.clear.valid := false.B
  stack.io.clear.bits := 0.U.asTypeOf(new RtTraversalStackClear)

  raySetup.io.out.ready := true.B
  when(raySetup.io.out.fire) {
    val lane = raySetup.io.out.bits.laneId
    val dst = directPrepared(0)(lane)
    dst.valid := true.B
    dst.currentTmax := raySetup.io.out.bits.ray.extent
    dst.originalTmin := raySetup.io.out.bits.ray.originalTmin
    dst.origin := raySetup.io.out.bits.ray.origin
    dst.dir := raySetup.io.out.bits.ray.dir
    dst.inv := raySetup.io.out.bits.ray.inv
    dst.shear := raySetup.io.out.bits.ray.shear
    dst.kx := raySetup.io.out.bits.ray.kx
    dst.ky := raySetup.io.out.bits.ray.ky
    dst.kz := raySetup.io.out.bits.ray.kz
  }
  // Several fixed SRAM banks may complete headers in one cycle.  Advance all
  // of them; a priority-encoded lane would otherwise leave sibling lanes in
  // RayLoad after their only AccelLo response has already arrived.
  for (lane <- 0 until num_thread) {
    when(local.io.worldBaseArrivedMask(lane) && laneState(0)(lane) === sRayLoad) {
      laneState(0)(lane) := Mux(spillEnabled, sContinuationInit, sHeaderRequest)
    }
    if (coherenceRecordTrace) {
      when(local.io.worldBaseArrivedMask(lane)) {
        val meta1 = local.io.coherenceTraceMeta1.get(lane)
        val depth = meta1(RtAbiLayout.TraceMeta1ActiveLevelShift + RtAbiLayout.TraceMeta1ActiveLevelBits - 1,
          RtAbiLayout.TraceMeta1ActiveLevelShift)
        coherenceLaneDepth.get(0)(lane) := depth
        printf(p"[RTCORE coherence-meta] warp=${Decimal(residentOwnerWarpId)} epoch=${Decimal(coherenceLaneEpoch.get(0)(lane))}" +
          p" issue=${Decimal(warpIssueId(0))} lane=${Decimal(lane.U)}" +
          p" depth=${Decimal(depth)} active_level=${Decimal(depth)}" +
          p" mask=0x${Hexadecimal(commandMask(0))}" +
          p"\n")
      }
    }
  }

  // The Instance datapath is deliberately separate from the RT Local gather:
  // its output is scheduler-private and is only selected for object-tagged
  // stack entries.  Reuse the normal setup block for kx/ky/kz and shear.
  objectRaySetup.io.in.valid := instanceTransform.io.out.valid
  objectRaySetup.io.in.bits := 0.U.asTypeOf(new RtRaySetupInput)
  objectRaySetup.io.in.bits.warpId := 0.U
  objectRaySetup.io.in.bits.laneId := instanceTransform.io.out.bits.laneId
  objectRaySetup.io.in.bits.rayState := instanceTransform.io.out.bits.rayState
  instanceTransform.io.out.ready := objectRaySetup.io.in.ready
  objectRaySetup.io.out.ready := true.B
  when(objectRaySetup.io.out.fire) {
    val lane = objectRaySetup.io.out.bits.laneId
    val dst = objectPrepared(0)(lane)
    dst.valid := true.B
    // tmax remains owned by the world RT Local row; the object copy is only
    // an IU coordinate-space cache.
    dst.currentTmax := objectRaySetup.io.out.bits.ray.extent
    dst.originalTmin := objectRaySetup.io.out.bits.ray.originalTmin
    dst.origin := objectRaySetup.io.out.bits.ray.origin
    dst.dir := objectRaySetup.io.out.bits.ray.dir
    dst.inv := objectRaySetup.io.out.bits.ray.inv
    dst.shear := objectRaySetup.io.out.bits.ray.shear
    dst.kx := objectRaySetup.io.out.bits.ray.kx
    dst.ky := objectRaySetup.io.out.bits.ray.ky
    dst.kz := objectRaySetup.io.out.bits.ray.kz
    if (sys.env.get("VENTUS_RTCORE_DEEP_TRACE").contains("1") ||
        sys.env.get("VENTUS_RTCORE_RECORD_TRACE").contains("1")) {
      when(sys.env.get("VENTUS_RTCORE_RECORD_TRACE").contains("1").B || lane < 4.U || lane >= 12.U) {
        val ray = objectRaySetup.io.out.bits.ray
        printf(p"[RTCORE trace] object-ray lane=${Decimal(lane)}" +
          p" base=0x${Hexadecimal(objectBase(0)(lane))}" +
          p" o=[${Hexadecimal(ray.origin.x)},${Hexadecimal(ray.origin.y)},${Hexadecimal(ray.origin.z)}]" +
          p" d=[${Hexadecimal(ray.dir.x)},${Hexadecimal(ray.dir.y)},${Hexadecimal(ray.dir.z)}]" +
          p" inv=[${Hexadecimal(ray.inv.x)},${Hexadecimal(ray.inv.y)},${Hexadecimal(ray.inv.z)}]" +
          p" tmin=${Hexadecimal(ray.originalTmin)} tmax=${Hexadecimal(ray.extent)}\\n")
      }
    }
    laneState(0)(lane) := Mux(rebuildingInstance(lane), sStackReady, sBlasHeaderRequest)
    rebuildingInstance(lane) := false.B
  }

  // A setup-complete header request and a stack pop share the AS acquire port.
  // Header fetches are preferred so newly admitted rays do not wait behind traversal.
  val headerMasks = Wire(Vec(residentWarpCount, UInt(num_thread.W)))
  for (warp <- 0 until residentWarpCount) {
    headerMasks(warp) := VecInit((0 until num_thread).map(lane =>
      laneState(warp)(lane) === sHeaderRequest ||
        laneState(warp)(lane) === sBlasHeaderRequest)).asUInt
  }
  val hasHeaderRequest = headerMasks.map(_.orR).reduce(_ || _)
  val headerWarp = PriorityEncoder(VecInit(headerMasks.map(_.orR)).asUInt)
  val headerLane = PriorityEncoder(headerMasks(headerWarp))
  val readyMasks = Wire(Vec(residentWarpCount, UInt(num_thread.W)))
  for (warp <- 0 until residentWarpCount) {
    readyMasks(warp) := VecInit((0 until num_thread).map(lane =>
      laneState(warp)(lane) === sStackReady)).asUInt
  }
  val hasStackReady = readyMasks.map(_.orR).reduce(_ || _)
  val popWarp = 0.U(residentWarpIdWidth.W)
  val popEligibleMask = readyMasks(popWarp) & ~pendingNodeValid(popWarp).asUInt
  val dispatchLineMask = VecInit((0 until num_thread).map { lane =>
    val kind = table.io.asReadyMeta(lane).bits.nodeKind
    table.io.asReadyMeta(lane).valid && (kind === RtVtasNodeKind.box4 ||
      kind === RtVtasNodeKind.aabb || kind === RtVtasNodeKind.triangle)
  }).asUInt
  // Header/instance handling remains scalar in this phase; Box/Triangle lines
  // are consumed by their fixed IU groups below.
  val asReadyLaneMask = VecInit((0 until num_thread).map { lane =>
    table.io.asReadyMeta(lane).valid && !dispatchLineMask(lane)
  }).asUInt
  val asReadyLane = PriorityEncoder(asReadyLaneMask)
  val selectedAsMeta = table.io.asReadyMeta(asReadyLane)
  val headerLineActive = selectedAsMeta.valid &&
    (selectedAsMeta.bits.nodeKind === RtVtasNodeKind.header ||
      selectedAsMeta.bits.nodeKind === RtVtasNodeKind.blasHeader)
  val pendingMask = pendingNodeValid(popWarp).asUInt
  val pendingLane = PriorityEncoder(pendingMask)
  val pendingRef = pendingNodeRef(popWarp)(pendingLane)
  val pendingObjectRay = pendingNodeObjectRay(popWarp)(pendingLane)
  val pendingType = nodeType(pendingRef)
  // Stack writes/initialisation are serialized ahead of a pop.  In
  // particular, never let an AS acquire consume a node unless the matching pop
  // can fire in the same cycle.
  local.io.worldBaseLookup.laneId := Mux(hasHeaderRequest, headerLane, pendingLane)
  val headerObject = laneState(headerWarp)(headerLane) === sBlasHeaderRequest
  val headerBase = Mux(headerObject, objectBase(headerWarp)(headerLane), local.io.worldBase.asBase)
  val headerValid = Mux(headerObject, objectPrepared(headerWarp)(headerLane).valid,
    local.io.worldBase.valid)
  val nodeBase = Mux(pendingObjectRay, objectBase(popWarp)(pendingLane), local.io.worldBase.asBase)
  val nodeBaseValid = Mux(pendingObjectRay, objectPrepared(popWarp)(pendingLane).valid,
    local.io.worldBase.valid)
  val headerAcquire = hasHeaderRequest && headerValid && !initializingContinuation
  // A Box child push belongs to its own lane.  It may proceed alongside a
  // different lane's pop; only headers and an active returned header line
  // retain global priority.
  // Header acquires own the shared AS port, not the lane stacks.  Other lanes
  // may still pop into their pending request slots in the same cycle.
  val popMayFire = !headerLineActive && !initializingContinuation
  val worldContextNeeded = !pendingObjectRay &&
    (pendingType === RtVtasLayout.NodeBox4.U || pendingType === RtVtasLayout.NodeAabb.U ||
      pendingType === RtVtasLayout.NodeInstance.U || pendingType === RtVtasLayout.NodeTriangle.U)
  worldContexts.io.prefetch.valid := !initializingContinuation && !headerAcquire &&
    pendingMask.orR && nodeBaseValid && worldContextNeeded
  worldContexts.io.prefetch.bits.laneId := pendingLane
  worldContexts.io.prefetch.bits.purpose := Mux(pendingType === RtVtasLayout.NodeInstance.U,
    RtWorldRayPurpose.instance, Mux(pendingType === RtVtasLayout.NodeTriangle.U,
      RtWorldRayPurpose.triangle, RtWorldRayPurpose.box))
  val nodeAcquire = !initializingContinuation && !headerAcquire && pendingMask.orR && nodeBaseValid &&
    (!worldContextNeeded || worldContexts.io.prefetch.ready)

  table.io.asAcquire.valid := headerAcquire || nodeAcquire
  table.io.asAcquire.bits := 0.U.asTypeOf(new RtAsTableAcquire)
  when(headerAcquire) {
    table.io.asAcquire.bits.lineAddress := headerBase
    table.io.asAcquire.bits.wordMask := Fill(RtMemTableParameters.lineWordCount, 1.U(1.W))
    table.io.asAcquire.bits.is64B := false.B
    table.io.asAcquire.bits.waiter.warpId := headerWarp
    table.io.asAcquire.bits.waiter.laneId := headerLane
    table.io.asAcquire.bits.waiter.nodeKind := Mux(headerObject,
      RtVtasNodeKind.blasHeader, RtVtasNodeKind.header)
    table.io.asAcquire.bits.waiter.objectRay := headerObject
  }.elsewhen(nodeAcquire) {
    table.io.asAcquire.bits.lineAddress := nodeBase + nodeOffset(pendingRef)
    table.io.asAcquire.bits.wordMask := Mux(pendingType === RtVtasLayout.NodeTriangle.U ||
      pendingType === RtVtasLayout.NodeAabb.U,
      Cat(0.U(16.W), Fill(16, 1.U(1.W))), Fill(32, 1.U(1.W)))
    table.io.asAcquire.bits.is64B := pendingType === RtVtasLayout.NodeTriangle.U ||
      pendingType === RtVtasLayout.NodeAabb.U
    table.io.asAcquire.bits.waiter.warpId := popWarp
    table.io.asAcquire.bits.waiter.laneId := pendingLane
    table.io.asAcquire.bits.waiter.nodeKind := MuxLookup(pendingType,
      RtVtasNodeKind.box4)(Seq(
        RtVtasLayout.NodeBox4.U -> RtVtasNodeKind.box4,
        RtVtasLayout.NodeTriangle.U -> RtVtasNodeKind.triangle,
        RtVtasLayout.NodeInstance.U -> RtVtasNodeKind.instance,
        RtVtasLayout.NodeAabb.U -> RtVtasNodeKind.aabb))
    table.io.asAcquire.bits.waiter.objectRay := pendingObjectRay
  }
  when(table.io.asAcquire.fire && headerAcquire) {
    // Elaborated only in the dedicated trace build; keep normal RTL free of
    // diagnostic print logic.
    if (sys.env.get("VENTUS_RTCORE_DEEP_TRACE").contains("1")) {
      when(headerLane < 4.U || headerLane >= 12.U) {
        printf(p"[RTCORE trace] header-req lane=${Decimal(headerLane)}" +
          p" object=${Decimal(headerObject)} base=0x${Hexadecimal(headerBase)}\\n")
      }
    }
    laneState(headerWarp)(headerLane) := Mux(headerObject, sBlasHeaderPending, sHeaderPending)
  }
  when(table.io.asAcquire.fire && nodeAcquire) {
    when(pendingType === RtVtasLayout.NodeInstance.U) {
      instanceNodeRef(pendingLane) := pendingRef
    }
    if (coherenceRecordTrace) {
      printf(p"[RTCORE coherence-line] warp=${Decimal(residentOwnerWarpId)} epoch=${Decimal(coherenceLaneEpoch.get(popWarp)(pendingLane))}" +
        p" lane=${Decimal(pendingLane)} addr=0x${Hexadecimal(table.io.asAcquire.bits.lineAddress)}" +
        p" is64=${Decimal(table.io.asAcquire.bits.is64B)} object=${Decimal(pendingObjectRay)}\n")
    }
    pendingNodeValid(popWarp)(pendingLane) := false.B
  }

  // An empty stack terminates a lane without an LSU request.  Corrupt/unknown
  // refs are skipped like the software reference, not treated as a traversal
  // failure that hides an already discovered sibling hit.
  when(popMayFire) {
    for (lane <- 0 until num_thread) when(popEligibleMask(lane) && stack.io.laneEmpty(lane)) {
      laneState(popWarp)(lane) := sTerminal
      laneStatus(popWarp)(lane) := Mux(committedValid(popWarp)(lane),
        RtAbiLayout.TraversalCompleteHit.U, RtAbiLayout.TraversalCompleteMiss.U)
    }
  }
  val popMask = VecInit((0 until num_thread).map { lane =>
    popEligibleMask(lane) && !stack.io.laneEmpty(lane)
  }).asUInt
  stack.io.pop.valid := popMayFire && popMask.orR
  stack.io.pop.bits.laneMask := popMask
  when(stack.io.pop.fire) {
    for (lane <- 0 until num_thread) when(popMask(lane)) {
      val ref = stack.io.popNodeRef(lane)
      val kind = nodeType(ref)
      val unsupported = kind =/= RtVtasLayout.NodeBox4.U && kind =/= RtVtasLayout.NodeTriangle.U &&
        kind =/= RtVtasLayout.NodeInstance.U && kind =/= RtVtasLayout.NodeAabb.U
      when(ref === RtVtasLayout.InvalidNodeRef.U || unsupported) {
        laneState(popWarp)(lane) := sStackReady
      }.otherwise {
        if (sys.env.get("VENTUS_RTCORE_RECORD_TRACE").contains("1")) {
          printf(p"[RTCORE node] warp=${Decimal(residentOwnerWarpId)}" +
            p" epoch=${Decimal(coherenceLaneEpoch.get(popWarp)(lane))}" +
            p" issue=${Decimal(warpIssueId(popWarp))} lane=${Decimal(lane.U)}" +
            p" kind=${Decimal(kind)} ref=0x${Hexadecimal(ref)}" +
            p" object=${Decimal(stack.io.popObjectRay(lane))}\\n")
        }
        pendingNodeValid(popWarp)(lane) := true.B
        pendingNodeRef(popWarp)(lane) := ref
        pendingNodeObjectRay(popWarp)(lane) := stack.io.popObjectRay(lane)
        laneState(popWarp)(lane) := sNodePending
      }
    }
  }

  // AS ready-line fan-out: header -> stack, Box4 -> any ready Box IU,
  // Triangle -> any ready Triangle IU.  The table owns response lifetime until
  // this handshake, so only the selected IU can backpressure its line.
  // Only the scalar path materializes a full cacheline here.  Box/Triangle
  // paths below use their own selected table read ports.
  val asReady = Wire(Decoupled(new RtAsTableReadyLine))
  // `asReadyLane` falls back to lane zero when no scalar-compatible line
  // exists.  Do not let that fallback consume a Box/Triangle waiter through
  // the legacy scalar path; only the fixed IU groups may consume those.
  asReady.valid := asReadyLaneMask.orR
  asReady.bits := 0.U.asTypeOf(new RtAsTableReadyLine)
  asReady.bits.waiter.warpId := 0.U
  asReady.bits.waiter.laneId := asReadyLane
  asReady.bits.waiter.nodeKind := selectedAsMeta.bits.nodeKind
  asReady.bits.waiter.objectRay := selectedAsMeta.bits.objectRay
  table.io.asLineRead(0).entryId := selectedAsMeta.bits.entryId
  asReady.bits.data := table.io.asLineRead(0).data
  val asConsumedLanes = WireDefault(0.U(num_thread.W))
  table.io.asConsumeLanes := asConsumedLanes
  val asUsesObjectRay = asReady.bits.waiter.objectRay
  val headerRoot = asReady.bits.data((RtVtasLayout.AsHeaderRootNodeRef / 4).toInt)
  val headerInvalid = headerRoot === RtVtasLayout.InvalidNodeRef.U(32.W)
  val asIsHeader = asReady.bits.waiter.nodeKind === RtVtasNodeKind.header
  val asIsBlasHeader = asReady.bits.waiter.nodeKind === RtVtasNodeKind.blasHeader
  val asIsInstance = asReady.bits.waiter.nodeKind === RtVtasNodeKind.instance
  // The command-AS header starts a new lane stack.  A BLAS header arrives
  // while that same lane still owns its TLAS DFS continuation, so replacing
  // the stack here would either deadlock on `live` or discard siblings.  Push
  // its root below as an object-ray continuation instead.
  stack.io.init.valid := asReady.valid && asIsHeader && !headerInvalid
  stack.io.init.bits.laneMask := UIntToOH(asReady.bits.waiter.laneId, num_thread)
  for (lane <- 0 until num_thread) {
    stack.io.init.bits.rootNodeRef(lane) := headerRoot
    stack.io.init.bits.objectRay(lane) := false.B
  }
  val boxConsumed = Wire(Vec(boxUnitCount, Bool()))
  val boxWorldIssue = Wire(Vec(boxUnitCount, Bool()))
  for (unit <- 0 until boxUnitCount) {
    // A world-space Instance is served by the scalar path but uses this same
    // bank's sole ordinary context lookup/consume port.  Do not also issue a
    // Box4 from the bank in that cycle: otherwise the Instance lookup wins,
    // while the Box4 AS waiter is consumed and its own context leaks.
    val instanceOwnsContextPort = asReady.valid && asIsInstance && !asUsesObjectRay &&
      asReady.bits.waiter.laneId(log2Ceil(boxUnitCount) - 1, 0) === unit.U
    val groupMask = VecInit((0 until num_thread).map { lane =>
      (if (lane % boxUnitCount == unit) dispatchLineMask(lane) &&
        (table.io.asReadyMeta(lane).bits.nodeKind === RtVtasNodeKind.box4 ||
          table.io.asReadyMeta(lane).bits.nodeKind === RtVtasNodeKind.aabb) &&
          !instanceOwnsContextPort else false.B)
    }).asUInt
    val lane = PriorityEncoder(groupMask)
    val port = unit + 1
    val meta = table.io.asReadyMeta(lane)
    table.io.asLineRead(port).entryId := meta.bits.entryId
    val line = Wire(Valid(new RtAsTableReadyLine))
    line.valid := groupMask.orR
    line.bits := 0.U.asTypeOf(new RtAsTableReadyLine)
    line.bits.waiter.warpId := 0.U
    line.bits.waiter.laneId := lane
    line.bits.waiter.nodeKind := meta.bits.nodeKind
    line.bits.waiter.objectRay := meta.bits.objectRay
    line.bits.data := table.io.asLineRead(port).data
    val ray = Wire(new Ray(recorded_float = false))
    ray := 0.U.asTypeOf(new Ray(recorded_float = false))
    worldContexts.io.lookupLane(unit) := Mux(asReady.valid && asIsInstance && !asUsesObjectRay &&
      asReady.bits.waiter.laneId(2, 0) === unit.U, asReady.bits.waiter.laneId, lane)
    when(!line.bits.waiter.objectRay) { ray := worldContexts.io.result(unit).ray }
    when(line.bits.waiter.objectRay) {
      ray.origin := objectPrepared(0)(lane).origin
      ray.dir := objectPrepared(0)(lane).dir
      ray.inv := objectPrepared(0)(lane).inv
      ray.shear := objectPrepared(0)(lane).shear
      ray.originalTmin := objectPrepared(0)(lane).originalTmin
      ray.extent := Mux(worldTmaxValid(0)(lane), worldTmax(0)(lane),
        objectPrepared(0)(lane).currentTmax)
      ray.kx := objectPrepared(0)(lane).kx; ray.ky := objectPrepared(0)(lane).ky; ray.kz := objectPrepared(0)(lane).kz
    }
    val rayValid = Mux(line.bits.waiter.objectRay, objectPrepared(0)(lane).valid,
      worldContexts.io.result(unit).valid)
    val isAabb = line.bits.waiter.nodeKind === RtVtasNodeKind.aabb
    boxIssue(unit).valid := groupMask.orR && rayValid
    boxIssue(unit).bits := 0.U.asTypeOf(new RtBox4IntersectionInput)
    boxIssue(unit).bits.requestId := requestId(line.bits.waiter.warpId, lane)
    boxIssue(unit).bits.ray := ray
    for (child <- 0 until 4) {
      boxIssue(unit).bits.aabb(child).x_min := Mux(isAabb,
        line.bits.data((RtVtasLayout.AabbMin / 4).toInt), line.bits.data((RtVtasLayout.Box4MinX / 4).toInt + child))
      boxIssue(unit).bits.aabb(child).y_min := Mux(isAabb,
        line.bits.data((RtVtasLayout.AabbMin / 4 + 1).toInt), line.bits.data((RtVtasLayout.Box4MinY / 4).toInt + child))
      boxIssue(unit).bits.aabb(child).z_min := Mux(isAabb,
        line.bits.data((RtVtasLayout.AabbMin / 4 + 2).toInt), line.bits.data((RtVtasLayout.Box4MinZ / 4).toInt + child))
      boxIssue(unit).bits.aabb(child).x_max := Mux(isAabb,
        line.bits.data((RtVtasLayout.AabbMax / 4).toInt), line.bits.data((RtVtasLayout.Box4MaxX / 4).toInt + child))
      boxIssue(unit).bits.aabb(child).y_max := Mux(isAabb,
        line.bits.data((RtVtasLayout.AabbMax / 4 + 1).toInt), line.bits.data((RtVtasLayout.Box4MaxY / 4).toInt + child))
      boxIssue(unit).bits.aabb(child).z_max := Mux(isAabb,
        line.bits.data((RtVtasLayout.AabbMax / 4 + 2).toInt), line.bits.data((RtVtasLayout.Box4MaxZ / 4).toInt + child))
    }
    boxConsumed(unit) := boxIssue(unit).fire
    boxWorldIssue(unit) := boxIssue(unit).fire && !line.bits.waiter.objectRay
    if (coherenceRecordTrace) {
      when(boxWorldIssue(unit)) {
        printf(p"[RTCORE coherence-ray] warp=${Decimal(residentOwnerWarpId)} epoch=${Decimal(coherenceLaneEpoch.get(0)(lane))}" +
          p" lane=${Decimal(lane)} depth=${Decimal(coherenceLaneDepth.get(0)(lane))}" +
          p" active_level=${Decimal(coherenceLaneDepth.get(0)(lane))}" +
          p" source=box o=[${Hexadecimal(ray.origin.x)},${Hexadecimal(ray.origin.y)},${Hexadecimal(ray.origin.z)}]" +
          p" d=[${Hexadecimal(ray.dir.x)},${Hexadecimal(ray.dir.y)},${Hexadecimal(ray.dir.z)}]" +
          p" tmin=${Hexadecimal(ray.originalTmin)} tmax=${Hexadecimal(ray.extent)}\n")
      }
    }
    when(instanceOwnsContextPort) {
      assert(!boxIssue(unit).fire,
        "world Instance and Box4 must not consume one context port in the same bank/cycle")
    }
    when(boxIssue(unit).fire) {
      when(!line.bits.waiter.objectRay) { worldContexts.io.consume(unit) := true.B }
      laneState(0)(lane) := sBoxInflight
      for (child <- 0 until 4) boxChildRefs(0)(lane)(child) := line.bits.data(child)
      boxObjectRay(0)(lane) := line.bits.waiter.objectRay
      boxIsAabb(0)(lane) := isAabb
      when(isAabb) {
        candidateData(0)(lane)(RtAbiLayout.HitRecordMeta0) :=
          (1 | (RtAbiLayout.HitRecordStatusValid << RtAbiLayout.HitRecordMeta0StatusShift)).U |
            (objectCustomIndex(0)(lane) << RtAbiLayout.HitRecordMeta0InstanceCustomIndexShift)
        candidateData(0)(lane)(RtAbiLayout.HitRecordMeta1) := objectSbtRecordOffset(0)(lane)
        candidateData(0)(lane)(RtAbiLayout.HitRecordHitT) := 0.U
        candidateData(0)(lane)(RtAbiLayout.HitRecordBarycentricsX) := 0.U
        candidateData(0)(lane)(RtAbiLayout.HitRecordBarycentricsY) := 0.U
        candidateData(0)(lane)(RtAbiLayout.HitRecordPrimitiveId) :=
          line.bits.data((RtVtasLayout.AabbPrimitiveId / 4).toInt)
        candidateData(0)(lane)(RtAbiLayout.HitRecordInstanceId) := objectInstanceId(0)(lane)
        candidateData(0)(lane)(RtAbiLayout.HitRecordGeometryId) :=
          line.bits.data((RtVtasLayout.AabbGeometryId / 4).toInt)
        candidateData(0)(lane)(RtAbiLayout.HitRecordHitKind) :=
          line.bits.data((RtVtasLayout.AabbHitKind / 4).toInt)
      }
    }
  }
  val triangleConsumed = Wire(Vec(triangleUnitCount, Bool()))
  val directSetupEligible = Wire(Vec(triangleUnitCount, Bool()))
  val directSetupLaneCandidate = Wire(Vec(triangleUnitCount, UInt(log2Ceil(num_thread).W)))
  val directSetupRayCandidate = Wire(Vec(triangleUnitCount, new Ray(recorded_float = false)))
  for (unit <- 0 until triangleUnitCount) {
    val groupMask = VecInit((0 until num_thread).map { lane =>
      if (lane % triangleUnitCount == unit) dispatchLineMask(lane) &&
        table.io.asReadyMeta(lane).bits.nodeKind === RtVtasNodeKind.triangle else false.B
    }).asUInt
    val lane = PriorityEncoder(groupMask)
    val port = boxUnitCount + unit + 1
    val meta = table.io.asReadyMeta(lane)
    table.io.asLineRead(port).entryId := meta.bits.entryId
    val line = Wire(Valid(new RtAsTableReadyLine))
    line.valid := groupMask.orR
    line.bits := 0.U.asTypeOf(new RtAsTableReadyLine)
    line.bits.waiter.warpId := 0.U
    line.bits.waiter.laneId := lane
    line.bits.waiter.nodeKind := meta.bits.nodeKind
    line.bits.waiter.objectRay := meta.bits.objectRay
    line.bits.data := table.io.asLineRead(port).data
    val directContext = worldContexts.io.setupResult(lane(2, 0))
    for (bank <- 0 until boxUnitCount) {
      when(groupMask.orR && !line.bits.waiter.objectRay && lane(2, 0) === bank.U) {
        worldContexts.io.setupLookupLane(bank) := lane
      }
    }
    directSetupLaneCandidate(unit) := lane
    directSetupRayCandidate(unit) := directContext.ray
    directSetupEligible(unit) := groupMask.orR && !line.bits.waiter.objectRay &&
      !directPrepared(0)(lane).valid && !directSetupStarted(0)(lane) &&
      directContext.valid && directContext.purpose === RtWorldRayPurpose.triangle
    val ray = Wire(new Ray(recorded_float = false))
    ray := 0.U.asTypeOf(new Ray(recorded_float = false))
    when(line.bits.waiter.objectRay) {
      ray.origin := objectPrepared(0)(lane).origin; ray.dir := objectPrepared(0)(lane).dir
      ray.inv := objectPrepared(0)(lane).inv; ray.shear := objectPrepared(0)(lane).shear
      ray.originalTmin := objectPrepared(0)(lane).originalTmin
      ray.extent := Mux(worldTmaxValid(0)(lane), worldTmax(0)(lane),
        objectPrepared(0)(lane).currentTmax)
      ray.kx := objectPrepared(0)(lane).kx; ray.ky := objectPrepared(0)(lane).ky; ray.kz := objectPrepared(0)(lane).kz
    }
    when(!line.bits.waiter.objectRay) {
      ray.origin := directPrepared(0)(lane).origin; ray.dir := directPrepared(0)(lane).dir
      ray.inv := directPrepared(0)(lane).inv; ray.shear := directPrepared(0)(lane).shear
      ray.originalTmin := directPrepared(0)(lane).originalTmin
      ray.extent := Mux(worldTmaxValid(0)(lane), worldTmax(0)(lane), directPrepared(0)(lane).currentTmax)
      ray.kx := directPrepared(0)(lane).kx; ray.ky := directPrepared(0)(lane).ky; ray.kz := directPrepared(0)(lane).kz
    }
    val rayValid = Mux(line.bits.waiter.objectRay, objectPrepared(0)(lane).valid,
      directPrepared(0)(lane).valid)
    triangleIssue(unit).valid := groupMask.orR && rayValid
    triangleIssue(unit).bits := 0.U.asTypeOf(new RtTriangleIntersectionInput)
    triangleIssue(unit).bits.requestId := requestId(line.bits.waiter.warpId, lane)
    triangleIssue(unit).bits.ray := ray
    triangleIssue(unit).bits.triangle.A.x := line.bits.data((RtVtasLayout.TriangleV0 / 4).toInt)
    triangleIssue(unit).bits.triangle.A.y := line.bits.data((RtVtasLayout.TriangleV0 / 4 + 1).toInt)
    triangleIssue(unit).bits.triangle.A.z := line.bits.data((RtVtasLayout.TriangleV0 / 4 + 2).toInt)
    triangleIssue(unit).bits.triangle.B.x := line.bits.data((RtVtasLayout.TriangleV1 / 4).toInt)
    triangleIssue(unit).bits.triangle.B.y := line.bits.data((RtVtasLayout.TriangleV1 / 4 + 1).toInt)
    triangleIssue(unit).bits.triangle.B.z := line.bits.data((RtVtasLayout.TriangleV1 / 4 + 2).toInt)
    triangleIssue(unit).bits.triangle.C.x := line.bits.data((RtVtasLayout.TriangleV2 / 4).toInt)
    triangleIssue(unit).bits.triangle.C.y := line.bits.data((RtVtasLayout.TriangleV2 / 4 + 1).toInt)
    triangleIssue(unit).bits.triangle.C.z := line.bits.data((RtVtasLayout.TriangleV2 / 4 + 2).toInt)
    triangleConsumed(unit) := triangleIssue(unit).fire
    when(triangleIssue(unit).fire) {
      laneState(0)(lane) := sTriangleInflight
      triangleMeta(0)(lane).objectRay := line.bits.waiter.objectRay
      triangleMeta(0)(lane).opaque := line.bits.data((RtVtasLayout.TriangleFlags / 4).toInt)(0)
      triangleMeta(0)(lane).primitiveId := line.bits.data((RtVtasLayout.TrianglePrimitiveId / 4).toInt)
      triangleMeta(0)(lane).geometryId := line.bits.data((RtVtasLayout.TriangleGeometryId / 4).toInt)
      triangleMeta(0)(lane).instanceId := objectInstanceId(0)(lane)
      triangleMeta(0)(lane).instanceCustomIndex := objectCustomIndex(0)(lane)
      triangleMeta(0)(lane).instanceSbtRecordOffset := objectSbtRecordOffset(0)(lane)
    }
  }
  val directSetupSelect = PriorityEncoder(directSetupEligible.asUInt)
  directSetupRequest := directSetupEligible.asUInt.orR
  directSetupLane := directSetupLaneCandidate(directSetupSelect)
  directSetupRay := directSetupRayCandidate(directSetupSelect)
  for (unit <- 0 until triangleUnitCount) {
    when(raySetup.io.in.fire && directSetupSelect === unit.U) {
      val lane = directSetupLaneCandidate(unit)
      directSetupStarted(0) := directSetupStarted(0) | UIntToOH(lane, num_thread)
      worldContexts.io.setupConsume(lane(2, 0)) := true.B
    }
  }
  val dispatchedLanes = VecInit((0 until num_thread).map { lane =>
    boxConsumed.zipWithIndex.map { case (fire, unit) => fire && lane.U === PriorityEncoder(VecInit((0 until num_thread).map { candidate =>
      (if (candidate % boxUnitCount == unit) dispatchLineMask(candidate) &&
        (table.io.asReadyMeta(candidate).bits.nodeKind === RtVtasNodeKind.box4 ||
          table.io.asReadyMeta(candidate).bits.nodeKind === RtVtasNodeKind.aabb) else false.B)
    }).asUInt) }.reduce(_ || _) ||
      triangleConsumed.zipWithIndex.map { case (fire, unit) => fire && lane.U === PriorityEncoder(VecInit((0 until num_thread).map { candidate =>
        if (candidate % triangleUnitCount == unit) dispatchLineMask(candidate) &&
          table.io.asReadyMeta(candidate).bits.nodeKind === RtVtasNodeKind.triangle else false.B
      }).asUInt) }.reduce(_ || _)
  }).asUInt
  asConsumedLanes := Mux(asReady.fire, UIntToOH(asReadyLane, num_thread), 0.U) | dispatchedLanes
  val instanceContext = worldContexts.io.result(asReady.bits.waiter.laneId(2, 0))
  val instanceTraceMetaReady = instanceContext.valid && instanceContext.purpose === RtWorldRayPurpose.instance
  val instanceMaskMatch = instanceContext.traceMeta0(15, 8) &
    asReady.bits.data((RtVtasLayout.InstanceMask / 4).toInt)(7, 0)
  instanceTransform.io.in.valid := asReady.valid && asIsInstance && !asUsesObjectRay &&
    instanceTraceMetaReady && instanceMaskMatch.orR
  instanceTransform.io.in.bits := 0.U.asTypeOf(new RtInstanceTransformInput)
  instanceTransform.io.in.bits.laneId := asReady.bits.waiter.laneId
  instanceTransform.io.in.bits.ray := instanceContext.ray
  if (coherenceRecordTrace) {
    when(instanceTransform.io.in.fire) {
      val lane = asReady.bits.waiter.laneId
      val ray = instanceTransform.io.in.bits.ray
      printf(p"[RTCORE coherence-ray] warp=${Decimal(residentOwnerWarpId)} epoch=${Decimal(coherenceLaneEpoch.get(0)(lane))}" +
        p" lane=${Decimal(lane)} depth=${Decimal(coherenceLaneDepth.get(0)(lane))}" +
        p" active_level=${Decimal(coherenceLaneDepth.get(0)(lane))}" +
        p" source=instance o=[${Hexadecimal(ray.origin.x)},${Hexadecimal(ray.origin.y)},${Hexadecimal(ray.origin.z)}]" +
        p" d=[${Hexadecimal(ray.dir.x)},${Hexadecimal(ray.dir.y)},${Hexadecimal(ray.dir.z)}]" +
        p" tmin=${Hexadecimal(ray.originalTmin)} tmax=${Hexadecimal(ray.extent)}\n")
    }
  }
  for (word <- 0 until 12)
    instanceTransform.io.in.bits.matrix(word) := asReady.bits.data((RtVtasLayout.InstanceWorldToObject / 4).toInt + word)
  val asHeaderReady = asReady.valid && asIsHeader && (headerInvalid || stack.io.init.ready)
  val blasHeaderPush = asReady.valid && asIsBlasHeader && !headerInvalid
  val asBlasHeaderReady = asReady.valid && asIsBlasHeader &&
    (headerInvalid || stack.io.pushAcceptedMask(asReady.bits.waiter.laneId))
  val asInstanceReady = asReady.valid && asIsInstance &&
    instanceTraceMetaReady && (!instanceMaskMatch.orR || instanceTransform.io.in.ready)
  asReady.ready := asHeaderReady || asBlasHeaderReady || asInstanceReady ||
    (asReady.valid && !asIsHeader && !asIsBlasHeader && !asIsInstance)
  // A masked-out instance consumes no transform input, but its short-lived
  // context is still finished once the AS line has been consumed.
  when(asReady.fire && asIsInstance && !asUsesObjectRay) {
    worldContexts.io.consume(asReady.bits.waiter.laneId(2, 0)) := true.B
  }
  // Several fixed Box4 IUs may initialise different lanes in one cycle.
  // Merge those writes by lane instead of relying on connection order.
  val instanceTmaxSeed = asReady.fire && asIsInstance && !asUsesObjectRay
  val worldTmaxSeedMask = VecInit((0 until num_thread).map { lane =>
    val fromBox = (0 until boxUnitCount).map(unit => boxWorldIssue(unit) &&
      requestLane(boxIssue(unit).bits.requestId) === lane.U).reduce(_ || _)
    fromBox || (instanceTmaxSeed && asReady.bits.waiter.laneId === lane.U)
  }).asUInt & ~worldTmaxValid(0)
  when(worldTmaxSeedMask.orR) {
    worldTmaxValid(0) := worldTmaxValid(0) | worldTmaxSeedMask
    for (lane <- 0 until num_thread) when(worldTmaxSeedMask(lane)) {
      val boxWriters = (0 until boxUnitCount).map(unit => boxWorldIssue(unit) &&
        requestLane(boxIssue(unit).bits.requestId) === lane.U)
      worldTmax(0)(lane) := Mux(instanceTmaxSeed && asReady.bits.waiter.laneId === lane.U,
        instanceContext.ray.extent, Mux1H(boxWriters.zipWithIndex.map { case (write, unit) =>
          write -> boxIssue(unit).bits.ray.extent
        }))
    }
  }
  when(asReady.fire) {
    val waiter = asReady.bits.waiter
    if (sys.env.get("VENTUS_RTCORE_DEEP_TRACE").contains("1")) {
      // The 4x4 mismatch is confined to lanes 12--15.  Once Box4 input has
      // been validated, node kinds and refs explain the remaining control
      // flow far more compactly than repeating all four AABBs.
      when(waiter.laneId < 4.U || waiter.laneId >= 12.U) {
        printf(p"[RTCORE trace] as lane=${Decimal(waiter.laneId)}" +
          p" kind=${Decimal(waiter.nodeKind)} object=${Decimal(waiter.objectRay)}" +
          p" data=[${Hexadecimal(asReady.bits.data(0))},${Hexadecimal(asReady.bits.data(1))}," +
          p"${Hexadecimal(asReady.bits.data(2))},${Hexadecimal(asReady.bits.data(3))}]\\n")
        when(asIsHeader || asIsBlasHeader) {
          printf(p"[RTCORE trace] header-rsp lane=${Decimal(waiter.laneId)}" +
            p" object=${Decimal(waiter.objectRay)} root=0x${Hexadecimal(headerRoot)}" +
            p" nodes=${Decimal(asReady.bits.data((RtVtasLayout.AsHeaderNodeCount / 4).toInt))}\\n")
        }
        when(asIsInstance) {
          printf(p"[RTCORE trace] instance lane=${Decimal(waiter.laneId)}" +
            p" w2o=[${Hexadecimal(asReady.bits.data(20))},${Hexadecimal(asReady.bits.data(21))},${Hexadecimal(asReady.bits.data(22))},${Hexadecimal(asReady.bits.data(23))};" +
            p"${Hexadecimal(asReady.bits.data(24))},${Hexadecimal(asReady.bits.data(25))},${Hexadecimal(asReady.bits.data(26))},${Hexadecimal(asReady.bits.data(27))};" +
            p"${Hexadecimal(asReady.bits.data(28))},${Hexadecimal(asReady.bits.data(29))},${Hexadecimal(asReady.bits.data(30))},${Hexadecimal(asReady.bits.data(31))}]\\n")
        }
      }
    }
    when(asIsHeader || asIsBlasHeader) {
      when(headerInvalid) {
        laneState(0)(waiter.laneId) := sTerminal
        laneStatus(0)(waiter.laneId) := RtAbiLayout.TraversalCompleteMiss.U
      }.otherwise {
        laneState(0)(waiter.laneId) := sStackReady
      }
    }.elsewhen(asIsInstance) {
      when(!instanceMaskMatch.orR) {
        laneState(0)(waiter.laneId) := sStackReady
      }.otherwise {
        objectBase(0)(waiter.laneId) := asReady.bits.data((RtVtasLayout.InstanceBlasAddrLo / 4).toInt)
        objectInstanceId(0)(waiter.laneId) := asReady.bits.data((RtVtasLayout.InstanceInstanceId / 4).toInt)
        objectCustomIndex(0)(waiter.laneId) := asReady.bits.data((RtVtasLayout.InstanceCustomIndex / 4).toInt)(23, 0)
        objectSbtRecordOffset(0)(waiter.laneId) := asReady.bits.data((RtVtasLayout.InstanceSbtRecordOffset / 4).toInt)(23, 0)
        laneState(0)(waiter.laneId) := sInstanceTransform
      }
    }.otherwise {
      laneState(0)(waiter.laneId) := sStackReady
    }
  }

  // Box results are sorted near-to-far.  Push them in reverse order so the
  // subsequent lane-local stack push continues DFS at the nearest child.
  def acceptBoxResult(result: RtBox4IntersectionOutput): Unit = {
    val warp = requestWarp(result.requestId)
    val lane = requestLane(result.requestId)
    if (sys.env.get("VENTUS_RTCORE_DEEP_TRACE").contains("1")) {
      when(lane < 4.U || lane >= 12.U) {
        printf(p"[RTCORE trace] box lane=${Decimal(lane)} intersect=0x${Hexadecimal(result.isIntersect.asUInt)}" +
          p" tmin=[${Hexadecimal(result.tmin(0))},${Hexadecimal(result.tmin(1))}," +
          p"${Hexadecimal(result.tmin(2))},${Hexadecimal(result.tmin(3))}]\\n")
      }
    }
    val count = PopCount(result.isIntersect)
    when(boxIsAabb(warp)(lane)) {
      when(count === 0.U) {
        laneState(warp)(lane) := sStackReady
      }.otherwise {
        // AABB overlap is only a procedural candidate.  The shader's later
        // reportIntersection result replaces this broad-phase t/hit-kind.
        candidateData(warp)(lane)(RtAbiLayout.HitRecordHitT) := result.tmin(0)
        laneState(warp)(lane) := sAabbCandidate
      }
    }.otherwise {
      laneState(warp)(lane) := Mux(count === 0.U, sStackReady, sBoxPush)
      boxDeferredCount(warp)(lane) := count
      boxPushedCount(warp)(lane) := 0.U
      for (index <- 0 until 4) {
        boxChildRefs(warp)(lane)(index) := boxChildRefs(warp)(lane)(result.boxIndex(index))
      }
    }
  }
  for (unit <- 0 until boxUnitCount) {
    boxResult(unit).ready := true.B
    when(boxResult(unit).fire) {
      assert(requestLane(boxResult(unit).bits.requestId)(log2Ceil(boxUnitCount) - 1, 0) === unit.U,
        "Box result returned through a unit outside its fixed lane group")
      acceptBoxResult(boxResult(unit).bits)
    }
  }

  val pushMasks = Wire(Vec(residentWarpCount, UInt(num_thread.W)))
  for (warp <- 0 until residentWarpCount) {
    pushMasks(warp) := VecInit((0 until num_thread).map(lane =>
      laneState(warp)(lane) === sBoxPush)).asUInt
  }
  val hasPush = pushMasks.map(_.orR).reduce(_ || _)
  val pushWarp = 0.U(residentWarpIdWidth.W)
  // A BLAS root is a continuation beneath the already-live TLAS stack.  Give
  // it its own lane slot; deferred Box4 children in other lanes can progress
  // in the same cycle.
  stack.io.push.valid := blasHeaderPush || hasPush
  val blasPushMask = Mux(blasHeaderPush, UIntToOH(asReady.bits.waiter.laneId, num_thread), 0.U)
  val deferredPushMask = pushMasks(pushWarp)
  stack.io.push.bits.laneMask := blasPushMask | deferredPushMask
  for (lane <- 0 until num_thread) {
    val pushIndex = boxDeferredCount(pushWarp)(lane) - boxPushedCount(pushWarp)(lane) - 1.U
    val isBlasRoot = blasHeaderPush && asReady.bits.waiter.laneId === lane.U
    stack.io.push.bits.nodeRef(lane) := Mux(isBlasRoot, headerRoot,
      boxChildRefs(pushWarp)(lane)(pushIndex(1, 0)))
    stack.io.push.bits.objectRay(lane) := Mux(isBlasRoot, true.B,
      boxObjectRay(pushWarp)(lane))
  }
  when(stack.io.push.fire) {
    for (lane <- 0 until num_thread) when(stack.io.pushAcceptedMask(lane)) {
      when(blasHeaderPush && asReady.bits.waiter.laneId === lane.U) {
        laneState(0)(lane) := sStackReady
      }.otherwise {
        when(boxPushedCount(pushWarp)(lane) + 1.U === boxDeferredCount(pushWarp)(lane)) {
          laneState(pushWarp)(lane) := sStackReady
        }.otherwise {
          boxPushedCount(pushWarp)(lane) := boxPushedCount(pushWarp)(lane) + 1.U
        }
      }
    }
  }

  // Compare tNum/tDenom against immutable originalTmin and mutable
  // currentTmax before invoking a divider.  A positive denominator lets us
  // compare tNum directly with each bound multiplied by tDenom, so rejected
  // candidates do not consume the three final IEEE divisions.
  val triangleResultMask = VecInit(triangleResult.map(_.valid)).asUInt
  val triangleResultSelect = roundRobin(triangleResultMask, nextTriangleResult, triangleUnitCount)
  val selectedTriangleResult = Mux1H((0 until triangleUnitCount).map(unit =>
    (triangleResultSelect === unit.U) -> triangleResult(unit).bits))
  val selectedTriangleWarp = requestWarp(selectedTriangleResult.requestId)
  val selectedTriangleLane = requestLane(selectedTriangleResult.requestId)
  val triangleUsesObjectRay = triangleMeta(selectedTriangleWarp)(selectedTriangleLane).objectRay
  val trianglePrepared = Wire(new RtPreparedRay)
  trianglePrepared := Mux(triangleUsesObjectRay,
    objectPrepared(selectedTriangleWarp)(selectedTriangleLane),
    directPrepared(selectedTriangleWarp)(selectedTriangleLane))
  val tBoundProduct = Module(new MulRecFN(8, 24))
  tBoundProduct.io.a := recFNFromFN(8, 24, Mux(worldTmaxValid(selectedTriangleWarp)(selectedTriangleLane),
    worldTmax(selectedTriangleWarp)(selectedTriangleLane), trianglePrepared.currentTmax))
  tBoundProduct.io.b := recFNFromFN(8, 24, selectedTriangleResult.tDenom)
  tBoundProduct.io.roundingMode := consts.round_near_even
  tBoundProduct.io.detectTininess := consts.tininess_beforeRounding
  val tMinBoundProduct = Module(new MulRecFN(8, 24))
  tMinBoundProduct.io.a := recFNFromFN(8, 24, trianglePrepared.originalTmin)
  tMinBoundProduct.io.b := recFNFromFN(8, 24, selectedTriangleResult.tDenom)
  tMinBoundProduct.io.roundingMode := consts.round_near_even
  tMinBoundProduct.io.detectTininess := consts.tininess_beforeRounding
  val denominatorPositive = Module(new CompareRecFN(8, 24))
  denominatorPositive.io.a := recFNFromFN(8, 24, selectedTriangleResult.tDenom)
  denominatorPositive.io.b := recFNFromFN(8, 24, 0.U(32.W))
  denominatorPositive.io.signaling := false.B
  val withinCurrentTmax = Module(new CompareRecFN(8, 24))
  withinCurrentTmax.io.a := recFNFromFN(8, 24, selectedTriangleResult.tNum)
  withinCurrentTmax.io.b := tBoundProduct.io.out
  withinCurrentTmax.io.signaling := false.B
  val aboveOriginalTmin = Module(new CompareRecFN(8, 24))
  aboveOriginalTmin.io.a := recFNFromFN(8, 24, selectedTriangleResult.tNum)
  aboveOriginalTmin.io.b := tMinBoundProduct.io.out
  aboveOriginalTmin.io.signaling := false.B
  val acceptedTriangleResult = selectedTriangleResult.hit && trianglePrepared.valid &&
    denominatorPositive.io.gt &&
    (aboveOriginalTmin.io.gt || aboveOriginalTmin.io.eq) && withinCurrentTmax.io.lt
  when(triangleResultMask.orR && selectedTriangleResult.hit) {
    assert(denominatorPositive.io.gt,
      "RtTrianglePipeline hit must carry a positive tDenom for tmax prefilter")
  }
  triangleResolve.io.in.valid := triangleResultMask.orR && acceptedTriangleResult
  triangleResolve.io.in.bits := selectedTriangleResult
  for (unit <- 0 until triangleUnitCount) {
    val selected = triangleResultMask.orR && triangleResultSelect === unit.U
    triangleResult(unit).ready := selected && (!selectedTriangleResult.hit ||
      (trianglePrepared.valid && (!acceptedTriangleResult || triangleResolve.io.in.ready)))
  }
  when(triangleResultMask.orR && triangleResult(triangleResultSelect).fire) {
    nextTriangleResult := triangleResultSelect + 1.U
    if (sys.env.get("VENTUS_RTCORE_DEEP_TRACE").contains("1")) {
      when(selectedTriangleLane < 4.U || selectedTriangleLane >= 12.U) {
        printf(p"[RTCORE trace] triangle lane=${Decimal(selectedTriangleLane)}" +
          p" hit=${Decimal(selectedTriangleResult.hit)}" +
          p" tnum=0x${Hexadecimal(selectedTriangleResult.tNum)}" +
          p" tden=0x${Hexadecimal(selectedTriangleResult.tDenom)}\\n")
      }
    }
    when(!acceptedTriangleResult) {
      laneState(selectedTriangleWarp)(selectedTriangleLane) := sStackReady
    }
  }

  val resultWarp = requestWarp(triangleResolve.io.out.bits.requestId)
  val resultLane = requestLane(triangleResolve.io.out.bits.requestId)
  val triangleIsOpaque = triangleMeta(resultWarp)(resultLane).opaque
  val triangleHitUpdate = Wire(new RtLocalHitRecordUpdate)
  triangleHitUpdate := 0.U.asTypeOf(new RtLocalHitRecordUpdate)
  triangleHitUpdate.laneId := resultLane
  triangleHitUpdate.committed := triangleIsOpaque
  triangleHitUpdate.dirty := Fill(RtAbiLayout.HitRecordWordCount, 1.U(1.W))
  triangleHitUpdate.data(RtAbiLayout.HitRecordMeta0) :=
    (1 | (RtAbiLayout.HitRecordStatusValid << RtAbiLayout.HitRecordMeta0StatusShift)).U |
      (triangleResolve.io.out.bits.frontFace.asUInt << RtAbiLayout.HitRecordMeta0FrontFaceShift) |
      (triangleIsOpaque.asUInt << RtAbiLayout.HitRecordMeta0OpaqueShift) |
      (triangleMeta(resultWarp)(resultLane).instanceCustomIndex <<
        RtAbiLayout.HitRecordMeta0InstanceCustomIndexShift)
  triangleHitUpdate.data(RtAbiLayout.HitRecordMeta1) :=
    triangleMeta(resultWarp)(resultLane).instanceSbtRecordOffset
  triangleHitUpdate.data(RtAbiLayout.HitRecordHitT) := triangleResolve.io.out.bits.t
  triangleHitUpdate.data(RtAbiLayout.HitRecordBarycentricsX) := triangleResolve.io.out.bits.u
  triangleHitUpdate.data(RtAbiLayout.HitRecordBarycentricsY) := triangleResolve.io.out.bits.v
  triangleHitUpdate.data(RtAbiLayout.HitRecordPrimitiveId) := triangleMeta(resultWarp)(resultLane).primitiveId
  triangleHitUpdate.data(RtAbiLayout.HitRecordInstanceId) := triangleMeta(resultWarp)(resultLane).instanceId
  triangleHitUpdate.data(RtAbiLayout.HitRecordGeometryId) := triangleMeta(resultWarp)(resultLane).geometryId
  triangleHitUpdate.data(RtAbiLayout.HitRecordHitKind) := "hfe".U
  val aabbCandidateMask = VecInit((0 until num_thread).map(lane =>
    laneState(0)(lane) === sAabbCandidate)).asUInt
  val aabbCandidateLane = PriorityEncoder(aabbCandidateMask)
  val aabbHitUpdate = Wire(new RtLocalHitRecordUpdate)
  aabbHitUpdate := 0.U.asTypeOf(new RtLocalHitRecordUpdate)
  aabbHitUpdate.laneId := aabbCandidateLane
  aabbHitUpdate.committed := false.B
  aabbHitUpdate.dirty := Fill(RtAbiLayout.HitRecordWordCount, 1.U(1.W))
  aabbHitUpdate.data := candidateData(0)(aabbCandidateLane)
  local.io.hitUpdate.valid := aabbCandidateMask.orR || triangleResolve.io.out.valid
  local.io.hitUpdate.bits := Mux(aabbCandidateMask.orR, aabbHitUpdate, triangleHitUpdate)
  if (sys.env.get("VENTUS_RTCORE_RECORD_TRACE").contains("1")) {
    when(local.io.hitUpdate.fire) {
      val update = local.io.hitUpdate.bits
      printf(p"[RTCORE record] lane=${Decimal(update.laneId)} committed=${Decimal(update.committed)}" +
        p" dirty=0x${Hexadecimal(update.dirty)} data=[${Hexadecimal(update.data(0))}," +
        p"${Hexadecimal(update.data(1))},${Hexadecimal(update.data(2))}," +
        p"${Hexadecimal(update.data(3))},${Hexadecimal(update.data(4))}," +
        p"${Hexadecimal(update.data(5))},${Hexadecimal(update.data(6))}," +
        p"${Hexadecimal(update.data(7))},${Hexadecimal(update.data(8))}]\n")
    }
  }
  triangleResolve.io.out.ready := !aabbCandidateMask.orR && local.io.hitUpdate.ready
  when(triangleResolve.io.out.fire) {
    when(triangleIsOpaque) {
      worldTmax(resultWarp)(resultLane) := triangleResolve.io.out.bits.t
      // The scalar resolve may coincide with one or more Box/Instance first
      // uses. Preserve those same-cycle seed bits instead of overwriting the
      // whole per-warp valid mask with only the resolved lane.
      worldTmaxValid(resultWarp) := worldTmaxValid(resultWarp) |
        UIntToOH(resultLane, num_thread) |
        Mux(resultWarp === 0.U, worldTmaxSeedMask, 0.U)
      committedValid(resultWarp)(resultLane) := true.B
      laneState(resultWarp)(resultLane) := sStackReady
    }.otherwise {
      laneState(resultWarp)(resultLane) := sTerminal
      laneStatus(resultWarp)(resultLane) := RtAbiLayout.TraversalCandidateNonOpaqueTriangle.U
      candidateValid(resultWarp)(resultLane) := true.B
    }
  }
  when(local.io.hitUpdate.fire && aabbCandidateMask.orR) {
    laneState(0)(aabbCandidateLane) := sTerminal
    laneStatus(0)(aabbCandidateLane) := RtAbiLayout.TraversalCandidateProceduralAabb.U
    candidateValid(0)(aabbCandidateLane) := true.B
  }

  RtSchedulerCompletion.connect(this)

  private def connectInstrumentation(): Unit = {
    // Export only physical handshakes.  In particular, local SRAM is private to
    // RTcore: these events must not be relabelled as generic LSU traffic.
    val narrowLocalReads = io.localNarrowRequest.map(port => port.fire && !port.bits.write)
    val narrowLocalWrites = io.localNarrowRequest.map(port => port.fire && port.bits.write)
    val narrowLocalResponses = io.localNarrowResponse.map(_.fire)
    val narrowLocalBackpressure = io.localNarrowRequest.map(port => port.valid && !port.ready)
    io.perf.localSramReadRequest := (io.localRequest.fire && !io.localRequest.bits.write) ||
      narrowLocalReads.reduce(_ || _)
    io.perf.localSramWriteRequest := (io.localRequest.fire && io.localRequest.bits.write) ||
      narrowLocalWrites.reduce(_ || _)
    io.perf.localSramReadResponse := io.localResponse.fire || narrowLocalResponses.reduce(_ || _)
    io.perf.localSramBackpressure := (io.localRequest.valid && !io.localRequest.ready) ||
      narrowLocalBackpressure.reduce(_ || _)
    io.perf.raySetupOutput := raySetup.io.out.fire
    io.perf.asLineAcquire := table.io.asAcquire.fire
    io.perf.asLineReady := PopCount(asConsumedLanes)
    io.perf.asAcquireBackpressure := table.io.asAcquire.valid && !table.io.asAcquire.ready
    io.perf.asCacheResidentHit := table.io.asCacheResidentHit
    io.perf.asCachePendingMerge := table.io.asCachePendingMerge
    io.perf.asCacheMiss := table.io.asCacheMiss
    io.perf.asCacheRefill := table.io.asCacheRefill
    io.perf.asCacheEviction := table.io.asCacheEviction
    io.perf.box4Issue := PopCount(boxIssue.map(_.fire))
    io.perf.box4Result := PopCount(boxResult.map(_.fire))
    io.perf.triangleIssue := PopCount(triangleIssue.map(_.fire))
    io.perf.triangleResult := PopCount(triangleResult.map(_.fire))
    io.perf.triangleResolveIssue := triangleResolve.io.in.fire
    io.perf.triangleResolveResult := triangleResolve.io.out.fire
    io.perf.stackPush := PopCount(stack.io.pushAcceptedMask)
    io.perf.stackPop := Mux(stack.io.pop.fire, PopCount(stack.io.pop.bits.laneMask), 0.U)
    if (PMU_DETAILED) {
      require(RtCorePortParameters.residentWarpCount == 1 && num_thread == 32)
      val d = io.perf.detailed.get
      d.enabled := false.B // parent applies the common PMU window
      d.clear := reset.asBool
      d.session := sessionActive(0)
      d.activeMask := activeMask(0)
      d.command := io.command.fire
      d.commandMask := io.command.bits.activeMask
      d.commandKind := Mux(freshCommand, 0.U, Mux(resumeCommand, 1.U, 2.U))
      // Detailed reuse windows follow physical ownership, including automatic
      // reclamation. The basic software-release counter remains separate.
      d.release := local.io.retire.fire
      d.laneState := laneState(0)
      // Observe only valid node pops (not empty-stack termination or headers).
      d.nodePopLanes := VecInit((0 until num_thread).map { lane =>
        val ref = stack.io.popNodeRef(lane)
        val kind = nodeType(ref)
        stack.io.pop.fire && popMask(lane) && ref =/= RtVtasLayout.InvalidNodeRef.U &&
          (kind === RtVtasLayout.NodeBox4.U || kind === RtVtasLayout.NodeTriangle.U ||
            kind === RtVtasLayout.NodeInstance.U || kind === RtVtasLayout.NodeAabb.U)
      }).asUInt
      for (lane <- 0 until num_thread) d.nodePopKinds(lane) := nodeType(stack.io.popNodeRef(lane))
      d.nodeAcquire := table.io.asAcquire.fire && nodeAcquire
      d.stackReady := PopCount(readyMasks(0))
      d.stackSelected := stack.io.pop.fire
      // Overlapping blockers, not an additive partition of stall cycles.
      d.popBlockedHeader := hasStackReady && headerAcquire
      d.popBlockedPush := false.B
      d.popBlockedHeaderLine := hasStackReady && headerLineActive
      d.asBlocked := table.io.asAcquire.valid && !table.io.asAcquire.ready
      d.readyBlockedRay := false.B
      d.readyBlockedBox := false.B
      d.readyBlockedTriangle := false.B
      d.rayLoad := local.io.detailed.get
      d.asTable := table.io.detailed.get
      def observe(dst: RtUnitObservation, in: DecoupledIO[_], out: DecoupledIO[_]): Unit = {
        dst.inValid := in.valid; dst.inReady := in.ready
        dst.outValid := out.valid; dst.outReady := out.ready
      }
      observe(d.lsu, io.lsuRequest, io.lsuResponse)
      d.lsuRequestTag := io.lsuRequest.bits.clientTag
      d.lsuResponseTag := io.lsuResponse.bits.clientTag
      for (i <- 0 until boxUnitCount) observe(d.units(i), boxIssue(i), boxResult(i))
      for (i <- 0 until triangleUnitCount) observe(d.units(boxUnitCount + i), triangleIssue(i), triangleResult(i))
      observe(d.units(boxUnitCount + triangleUnitCount), triangleResolve.io.in, triangleResolve.io.out)
    }
  }
  connectInstrumentation()
}

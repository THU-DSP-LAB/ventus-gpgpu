package rtcore

import chisel3._
import chisel3.util._
import hardfloat._
import top.parameters._

object RtCoreSchedulerParameters {
  val boxUnitCount = 8
  val triangleUnitCount = 2
  require(RtCorePortParameters.requestIdWidth >= log2Ceil(num_thread))
}

private[rtcore] object RtVtasNodeKind {
  val header = 0.U(3.W)
  val box4 = 1.U(3.W)
  val triangle = 2.U(3.W)
  val instance = 3.U(3.W)
  val aabb = 4.U(3.W)
  // Same on-wire header format, but it must initialise an object-tagged
  // stack entry after an Instance transform.
  val blasHeader = 5.U(3.W)
}

private[rtcore] class RtTriangleDispatchMeta extends Bundle {
  val opaque = Bool()
  val primitiveId = UInt(xLen.W)
  val geometryId = UInt(xLen.W)
  val sbtRecordOffset = UInt(xLen.W)
  val instanceId = UInt(xLen.W)
  val instanceCustomIndex = UInt(24.W)
  val instanceSbtRecordOffset = UInt(24.W)
  val instanceFlags = UInt(xLen.W)
}

private[rtcore] class RtAabbDispatchMeta extends Bundle {
  val primitiveId = UInt(xLen.W)
  val geometryId = UInt(xLen.W)
  val sbtRecordOffset = UInt(xLen.W)
  val hitKind = UInt(xLen.W)
  val instanceId = UInt(xLen.W)
  val instanceCustomIndex = UInt(24.W)
  val instanceSbtRecordOffset = UInt(24.W)
  val instanceFlags = UInt(xLen.W)
}

/**
  * Warp/lane traversal controller.
  *
  * It is the RTcore control boundary: commands start RT Local SRAM gather,
  * local-ready notifications feed setup and traversal, and only this module
  * chooses an IU destination.  RtMemTable remains the sole LSU client.  The
  * stack carries a world/object tag so one lane can return from a BLAS to a
  * TLAS sibling without ever storing object rays in RT Local SRAM.
  */
class RtCoreScheduler extends Module {
  import RtCoreSchedulerParameters._

  private val residentWarpCount = RtCorePortParameters.residentWarpCount
  private val residentWarpIdWidth = RtCorePortParameters.residentWarpIdWidth

  val io = IO(new RtCoreIO)
  val boxIssue = IO(Vec(boxUnitCount, Decoupled(new RtBox4IntersectionInput)))
  val boxResult = IO(Flipped(Vec(boxUnitCount, Decoupled(new RtBox4IntersectionOutput))))
  val triangleIssue = IO(Vec(triangleUnitCount, Decoupled(new RtTriangleIntersectionInput)))
  val triangleResult = IO(Flipped(Vec(triangleUnitCount, Decoupled(new RtTriangleIntersectionOutput))))

  val table = Module(new RtMemTable)
  val local = Module(new RtLocalRayState)
  val stack = Module(new RtTraversalStack)
  val raySetup = Module(new RtRaySetup)
  val triangleResolve = Module(new RtTriangleResolve)
  io.lsuRequest <> table.io.lsuRequest
  table.io.lsuResponse <> io.lsuResponse
  io.localRequest <> local.io.request
  local.io.response <> io.localResponse

  val Seq(sIdle, sRayLoad, sHeaderRequest, sHeaderPending, sStackReady,
    sNodePending, sBoxInflight, sBoxPush, sTriangleInflight, sTerminal,
    sResumePending, sInstanceTransform, sBlasHeaderRequest, sBlasHeaderPending,
    sAabbCandidate) = Enum(15)
  val laneState = RegInit(VecInit(Seq.fill(residentWarpCount)(VecInit(Seq.fill(num_thread)(sIdle)))))
  val laneStatus = Reg(Vec(residentWarpCount, Vec(num_thread, UInt(xLen.W))))
  val committedValid = RegInit(VecInit(Seq.fill(residentWarpCount)(VecInit(Seq.fill(num_thread)(false.B)))))
  val activeMask = RegInit(VecInit(Seq.fill(residentWarpCount)(0.U(num_thread.W))))
  // `activeMask` is the session ownership mask.  A completion only reports
  // the command mask, so a subset of retained candidate lanes may resume.
  val commandMask = RegInit(VecInit(Seq.fill(residentWarpCount)(0.U(num_thread.W))))
  val warpIssueId = Reg(Vec(residentWarpCount, UInt(RtCorePortParameters.issueIdWidth.W)))
  val warpDestination = Reg(Vec(residentWarpCount, UInt(RtCorePortParameters.regidx_totalwidth.W)))
  val sessionActive = RegInit(VecInit(Seq.fill(residentWarpCount)(false.B)))
  val finishIssued = RegInit(VecInit(Seq.fill(residentWarpCount)(false.B)))
  // Completion has been handed to pipe, but the retained RT session still
  // owns its RT Local buffer/stack state until resume or final release.
  val completionPublished = RegInit(VecInit(Seq.fill(residentWarpCount)(false.B)))
  val boxChildRefs = Reg(Vec(residentWarpCount, Vec(num_thread, Vec(4, UInt(32.W)))))
  val boxDeferredRefs = Reg(Vec(residentWarpCount, Vec(num_thread, Vec(4, UInt(32.W)))))
  val boxDeferredCount = Reg(Vec(residentWarpCount, Vec(num_thread, UInt(3.W))))
  val boxPushedCount = Reg(Vec(residentWarpCount, Vec(num_thread, UInt(3.W))))
  val boxObjectRay = RegInit(VecInit(Seq.fill(residentWarpCount)(VecInit(Seq.fill(num_thread)(false.B)))))
  val boxIsAabb = RegInit(VecInit(Seq.fill(residentWarpCount)(VecInit(Seq.fill(num_thread)(false.B)))))
  val triangleMeta = Reg(Vec(residentWarpCount, Vec(num_thread, new RtTriangleDispatchMeta)))
  val aabbMeta = Reg(Vec(residentWarpCount, Vec(num_thread, new RtAabbDispatchMeta)))
  val candidateValid = RegInit(VecInit(Seq.fill(residentWarpCount)(VecInit(Seq.fill(num_thread)(false.B)))))
  val candidateData = Reg(Vec(residentWarpCount, Vec(num_thread,
    Vec(RtMemTableParameters.hitRecordStorageWordCount, UInt(xLen.W)))))
  // One private object context per lane is sufficient for VTAS's TLAS->BLAS
  // nesting.  Object-tagged descendants are exhausted before DFS returns to
  // another world-tagged Instance, at which point this entry may be replaced.
  val objectPrepared = RegInit(VecInit(Seq.fill(residentWarpCount)(VecInit(
    Seq.fill(num_thread)(0.U.asTypeOf(new RtPreparedRay))))))
  val objectBase = Reg(Vec(residentWarpCount, Vec(num_thread, UInt(memReqLen.W))))
  val objectMatrix = Reg(Vec(residentWarpCount, Vec(num_thread, Vec(12, UInt(32.W)))))
  val objectInstanceId = Reg(Vec(residentWarpCount, Vec(num_thread, UInt(xLen.W))))
  val objectCustomIndex = Reg(Vec(residentWarpCount, Vec(num_thread, UInt(24.W))))
  val objectSbtRecordOffset = Reg(Vec(residentWarpCount, Vec(num_thread, UInt(24.W))))
  val objectFlags = Reg(Vec(residentWarpCount, Vec(num_thread, UInt(xLen.W))))
  val traceMeta0 = Reg(Vec(residentWarpCount, Vec(num_thread, UInt(32.W))))
  // This is control state, unlike the lane payload arrays above.  It gates
  // retained-session resume/release admission, so its reset value must be
  // defined even while `sessionActive` is false.
  val residentOwnerWarpId = RegInit(0.U(depth_warp.W))

  val instanceTransform = Module(new RtInstanceTransform)
  val objectRaySetup = Module(new RtRaySetup)

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
  val nextStackLane = RegInit(VecInit(Seq.fill(residentWarpCount)(0.U(log2Ceil(num_thread).W))))
  val nextPushLane = RegInit(VecInit(Seq.fill(residentWarpCount)(0.U(log2Ceil(num_thread).W))))
  val nextBoxIssue = RegInit(0.U(log2Ceil(boxUnitCount).W))
  val nextTriangleIssue = RegInit(0.U(log2Ceil(triangleUnitCount).W))
  val nextBoxResult = RegInit(0.U(log2Ceil(boxUnitCount).W))
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
  local.io.admission.valid := io.command.valid && freshCommand
  local.io.admission.bits.warpId := io.command.bits.warpId
  local.io.admission.bits.activeMask := io.command.bits.activeMask
  local.io.restart.valid := io.command.valid && restartCommand
  local.io.restart.bits.warpId := io.command.bits.warpId
  local.io.restart.bits.activeMask := io.command.bits.activeMask
  local.io.resume.valid := io.command.valid && resumeCommand
  local.io.resume.bits.activeMask := io.command.bits.activeMask
  val restartStackClearRequest = io.command.valid && restartCommand && local.io.restart.ready
  io.command.ready := commandAllowed && Mux(freshCommand,
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
    commandMask(warp) := io.command.bits.activeMask
    warpIssueId(warp) := io.command.bits.issueId
    warpDestination(warp) := io.command.bits.destinationVd
    finishIssued(warp) := false.B
    when(freshCommand) {
      sessionActive(warp) := true.B
      completionPublished(warp) := false.B
      activeMask(warp) := io.command.bits.activeMask
      for (lane <- 0 until num_thread) {
        when(io.command.bits.activeMask(lane)) {
          laneState(warp)(lane) := sRayLoad
          laneStatus(warp)(lane) := RtAbiLayout.TraversalCompleteMiss.U
          committedValid(warp)(lane) := false.B
          candidateValid(warp)(lane) := false.B
        }.otherwise {
          laneState(warp)(lane) := sIdle
        }
      }
    }.elsewhen(restartCommand) {
      assert((io.command.bits.activeMask & ~activeMask(warp)) === 0.U,
        "nested traversal mask must be a subset of the resident RT session")
      completionPublished(warp) := false.B
      for (lane <- 0 until num_thread) {
        when(io.command.bits.activeMask(lane)) {
          laneState(warp)(lane) := sRayLoad
          laneStatus(warp)(lane) := RtAbiLayout.TraversalCompleteMiss.U
          committedValid(warp)(lane) := false.B
          candidateValid(warp)(lane) := false.B
          boxDeferredCount(warp)(lane) := 0.U
          boxPushedCount(warp)(lane) := 0.U
          boxObjectRay(warp)(lane) := false.B
        }
      }
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

  // A gathered local row is the input transaction of RaySetup.  Retirement
  // happens only after the final local writeback/completion boundary.
  raySetup.io.in.valid := local.io.laneOut.valid
  raySetup.io.in.bits.warpId := 0.U
  raySetup.io.in.bits.laneId := local.io.laneOut.bits.laneId
  raySetup.io.in.bits.accelLo := local.io.laneOut.bits.data(0)
  raySetup.io.in.bits.accelHi := local.io.laneOut.bits.data(1)
  raySetup.io.in.bits.traceMeta0 := local.io.laneOut.bits.data(2)
  raySetup.io.in.bits.traceMeta1 := local.io.laneOut.bits.data(3)
  raySetup.io.in.bits.rayState.origin.x := local.io.laneOut.bits.data(4)
  raySetup.io.in.bits.rayState.origin.y := local.io.laneOut.bits.data(5)
  raySetup.io.in.bits.rayState.origin.z := local.io.laneOut.bits.data(6)
  raySetup.io.in.bits.rayState.tmin := local.io.laneOut.bits.data(7)
  raySetup.io.in.bits.rayState.dir.x := local.io.laneOut.bits.data(8)
  raySetup.io.in.bits.rayState.dir.y := local.io.laneOut.bits.data(9)
  raySetup.io.in.bits.rayState.dir.z := local.io.laneOut.bits.data(10)
  raySetup.io.in.bits.rayState.tmax := local.io.laneOut.bits.data(11)
  raySetup.io.in.bits.rayState.inv.x := local.io.laneOut.bits.data(12)
  raySetup.io.in.bits.rayState.inv.y := local.io.laneOut.bits.data(13)
  raySetup.io.in.bits.rayState.inv.z := local.io.laneOut.bits.data(14)
  local.io.laneOut.ready := raySetup.io.in.ready
  when(local.io.laneOut.fire) { assert(laneState(0)(local.io.laneOut.bits.laneId) === sRayLoad) }
  if (sys.env.get("VENTUS_RTCORE_DEEP_TRACE").contains("1") ||
      sys.env.get("VENTUS_RTCORE_RECORD_TRACE").contains("1")) {
    when(local.io.laneOut.fire) {
      val row = local.io.laneOut.bits.data
      printf(p"[RTCORE deep] world-ray lane=${Decimal(local.io.laneOut.bits.laneId)}" +
        p" accel=[${Hexadecimal(row(0))},${Hexadecimal(row(1))}]" +
        p" meta=[${Hexadecimal(row(2))},${Hexadecimal(row(3))}]" +
        p" o=[${Hexadecimal(row(4))},${Hexadecimal(row(5))},${Hexadecimal(row(6))}]" +
        p" tmin=${Hexadecimal(row(7))}" +
        p" d=[${Hexadecimal(row(8))},${Hexadecimal(row(9))},${Hexadecimal(row(10))}]" +
        p" tmax=${Hexadecimal(row(11))}" +
        p" inv=[${Hexadecimal(row(12))},${Hexadecimal(row(13))},${Hexadecimal(row(14))}]\\n")
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

  // RaySetup writes the derived ray into the local prepared-ray buffer.  It
  // then supplies the ray to AS/IU paths without further SRAM accesses.
  local.io.raySetup <> raySetup.io.out
  when(raySetup.io.out.fire) {
    traceMeta0(0)(raySetup.io.out.bits.laneId) := raySetup.io.out.bits.traceMeta0
    laneState(0)(raySetup.io.out.bits.laneId) := sHeaderRequest
  }

  // The Instance datapath is deliberately separate from the RT Local gather:
  // its output is scheduler-private and is only selected for object-tagged
  // stack entries.  Reuse the normal setup block for kx/ky/kz and shear.
  objectRaySetup.io.in.valid := instanceTransform.io.out.valid
  objectRaySetup.io.in.bits := 0.U.asTypeOf(new RtRaySetupInput)
  objectRaySetup.io.in.bits.warpId := 0.U
  objectRaySetup.io.in.bits.laneId := instanceTransform.io.out.bits.laneId
  objectRaySetup.io.in.bits.accelLo := objectBase(0)(instanceTransform.io.out.bits.laneId)
  objectRaySetup.io.in.bits.accelHi := 0.U
  objectRaySetup.io.in.bits.rayState := instanceTransform.io.out.bits.rayState
  instanceTransform.io.out.ready := objectRaySetup.io.in.ready
  objectRaySetup.io.out.ready := true.B
  when(objectRaySetup.io.out.fire) {
    val lane = objectRaySetup.io.out.bits.laneId
    val dst = objectPrepared(0)(lane)
    dst.valid := true.B
    dst.asBase := objectBase(0)(lane)
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
    laneState(0)(lane) := sBlasHeaderRequest
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
  val popLane = roundRobin(readyMasks(popWarp), nextStackLane(popWarp), num_thread)
  val selectedEmpty = stack.io.laneEmpty(popLane)
  val selectedNodeRef = stack.io.popNodeRef(popLane)
  val selectedObjectRay = stack.io.popObjectRay(popLane)
  val selectedNodeType = nodeType(selectedNodeRef)
  val selectedUnsupported = selectedNodeType =/= RtVtasLayout.NodeBox4.U &&
    selectedNodeType =/= RtVtasLayout.NodeTriangle.U &&
    selectedNodeType =/= RtVtasLayout.NodeInstance.U &&
    selectedNodeType =/= RtVtasLayout.NodeAabb.U
  val selectedInvalid = selectedNodeRef === RtVtasLayout.InvalidNodeRef.U(32.W)
  val anyBoxPush = (0 until residentWarpCount).map { warp =>
    (0 until num_thread).map(lane => laneState(warp)(lane) === sBoxPush).reduce(_ || _)
  }.reduce(_ || _)
  val headerLineActive = table.io.asReadyLine.valid &&
    (table.io.asReadyLine.bits.waiter.nodeKind === RtVtasNodeKind.header ||
      table.io.asReadyLine.bits.waiter.nodeKind === RtVtasNodeKind.blasHeader)
  val popNeedsAs = hasStackReady && !selectedEmpty && !selectedInvalid && !selectedUnsupported
  // Stack writes/initialisation are serialized ahead of a pop.  In
  // particular, never let an AS acquire consume a node unless the matching pop
  // can fire in the same cycle.
  local.io.nodeRayLookup.laneId := Mux(hasHeaderRequest, headerLane, popLane)
  val headerObject = laneState(headerWarp)(headerLane) === sBlasHeaderRequest
  val headerBase = Mux(headerObject, objectBase(headerWarp)(headerLane), local.io.nodeRay.asBase)
  val headerValid = Mux(headerObject, objectPrepared(headerWarp)(headerLane).valid,
    local.io.nodeRay.valid)
  val nodeBase = Mux(selectedObjectRay, objectBase(popWarp)(popLane), local.io.nodeRay.asBase)
  val headerAcquire = hasHeaderRequest && headerValid
  val popMayFire = !headerAcquire && !anyBoxPush && !headerLineActive
  val nodeAcquire = popMayFire && popNeedsAs

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
    table.io.asAcquire.bits.lineAddress := nodeBase + nodeOffset(selectedNodeRef)
    table.io.asAcquire.bits.wordMask := Mux(selectedNodeType === RtVtasLayout.NodeTriangle.U ||
      selectedNodeType === RtVtasLayout.NodeAabb.U,
      Cat(0.U(16.W), Fill(16, 1.U(1.W))), Fill(32, 1.U(1.W)))
    table.io.asAcquire.bits.is64B := selectedNodeType === RtVtasLayout.NodeTriangle.U ||
      selectedNodeType === RtVtasLayout.NodeAabb.U
    table.io.asAcquire.bits.waiter.warpId := popWarp
    table.io.asAcquire.bits.waiter.laneId := popLane
    table.io.asAcquire.bits.waiter.nodeKind := MuxLookup(selectedNodeType,
      RtVtasNodeKind.box4)(Seq(
        RtVtasLayout.NodeBox4.U -> RtVtasNodeKind.box4,
        RtVtasLayout.NodeTriangle.U -> RtVtasNodeKind.triangle,
        RtVtasLayout.NodeInstance.U -> RtVtasNodeKind.instance,
        RtVtasLayout.NodeAabb.U -> RtVtasNodeKind.aabb))
    table.io.asAcquire.bits.waiter.objectRay := selectedObjectRay
  }
  when(table.io.asAcquire.fire && headerAcquire) {
    if (sys.env.get("VENTUS_RTCORE_DEEP_TRACE").contains("1")) {
      when(headerLane < 4.U || headerLane >= 12.U) {
        printf(p"[RTCORE trace] header-req lane=${Decimal(headerLane)}" +
          p" object=${Decimal(headerObject)} base=0x${Hexadecimal(headerBase)}\\n")
      }
    }
    laneState(headerWarp)(headerLane) := Mux(headerObject, sBlasHeaderPending, sHeaderPending)
  }

  // An empty stack terminates a lane without an LSU request.  Corrupt/unknown
  // refs are skipped like the software reference, not treated as a traversal
  // failure that hides an already discovered sibling hit.
  when(popMayFire && hasStackReady && selectedEmpty) {
    laneState(popWarp)(popLane) := sTerminal
    laneStatus(popWarp)(popLane) := Mux(committedValid(popWarp)(popLane),
      RtAbiLayout.TraversalCompleteHit.U, RtAbiLayout.TraversalCompleteMiss.U)
  }
  stack.io.pop.valid := popMayFire && hasStackReady && !selectedEmpty &&
    (selectedInvalid || selectedUnsupported || table.io.asAcquire.ready)
  stack.io.pop.bits.laneMask := UIntToOH(popLane, num_thread)
  when(stack.io.pop.fire) {
    if (sys.env.get("VENTUS_RTCORE_DEEP_TRACE").contains("1")) {
      when(popLane < 4.U || popLane >= 12.U) {
        printf(p"[RTCORE trace] pop lane=${Decimal(popLane)}" +
          p" ref=0x${Hexadecimal(selectedNodeRef)} type=${Decimal(selectedNodeType)}" +
          p" object=${Decimal(selectedObjectRay)} invalid=${Decimal(selectedInvalid)}" +
          p" unsupported=${Decimal(selectedUnsupported)}\\n")
      }
    }
    nextStackLane(popWarp) := popLane + 1.U
    when(selectedInvalid || selectedUnsupported) {
      laneState(popWarp)(popLane) := sStackReady
    }.otherwise {
      laneState(popWarp)(popLane) := sNodePending
    }
  }

  // AS ready-line fan-out: header -> stack, Box4 -> any ready Box IU,
  // Triangle -> any ready Triangle IU.  The table owns response lifetime until
  // this handshake, so only the selected IU can backpressure its line.
  val boxReadyMask = VecInit(boxIssue.map(_.ready)).asUInt
  val triangleReadyMask = VecInit(triangleIssue.map(_.ready)).asUInt
  val boxSelect = roundRobin(boxReadyMask, nextBoxIssue, boxUnitCount)
  val triangleSelect = roundRobin(triangleReadyMask, nextTriangleIssue, triangleUnitCount)
  val asReady = table.io.asReadyLine
  local.io.issueRayLookup.laneId := asReady.bits.waiter.laneId
  val asUsesObjectRay = asReady.bits.waiter.objectRay
  val selectedIssueRay = Wire(new Ray(recorded_float = false))
  selectedIssueRay := local.io.issueRay.ray
  when(asUsesObjectRay) {
    selectedIssueRay.origin := objectPrepared(0)(asReady.bits.waiter.laneId).origin
    selectedIssueRay.dir := objectPrepared(0)(asReady.bits.waiter.laneId).dir
    selectedIssueRay.inv := objectPrepared(0)(asReady.bits.waiter.laneId).inv
    selectedIssueRay.shear := objectPrepared(0)(asReady.bits.waiter.laneId).shear
    selectedIssueRay.originalTmin := objectPrepared(0)(asReady.bits.waiter.laneId).originalTmin
    // tmax is world-ray traversal state even while the IU works in object
    // space; an accepted hit must constrain both spaces identically.
    selectedIssueRay.extent := local.io.issueRay.currentTmax
    selectedIssueRay.kx := objectPrepared(0)(asReady.bits.waiter.laneId).kx
    selectedIssueRay.ky := objectPrepared(0)(asReady.bits.waiter.laneId).ky
    selectedIssueRay.kz := objectPrepared(0)(asReady.bits.waiter.laneId).kz
  }
  val selectedIssueRayValid = local.io.issueRay.valid &&
    (!asUsesObjectRay || objectPrepared(0)(asReady.bits.waiter.laneId).valid)
  val headerRoot = asReady.bits.data((RtVtasLayout.AsHeaderRootNodeRef / 4).toInt)
  val headerInvalid = headerRoot === RtVtasLayout.InvalidNodeRef.U(32.W)
  val asIsHeader = asReady.bits.waiter.nodeKind === RtVtasNodeKind.header
  val asIsBlasHeader = asReady.bits.waiter.nodeKind === RtVtasNodeKind.blasHeader
  val asIsBox = asReady.bits.waiter.nodeKind === RtVtasNodeKind.box4
  val asIsTriangle = asReady.bits.waiter.nodeKind === RtVtasNodeKind.triangle
  val asIsInstance = asReady.bits.waiter.nodeKind === RtVtasNodeKind.instance
  val asIsAabb = asReady.bits.waiter.nodeKind === RtVtasNodeKind.aabb
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
  for (unit <- 0 until boxUnitCount) {
    boxIssue(unit).valid := asReady.valid && (asIsBox || asIsAabb) && selectedIssueRayValid && boxReadyMask.orR && boxSelect === unit.U
    boxIssue(unit).bits := 0.U.asTypeOf(new RtBox4IntersectionInput)
    boxIssue(unit).bits.requestId := requestId(asReady.bits.waiter.warpId, asReady.bits.waiter.laneId)
    boxIssue(unit).bits.ray := selectedIssueRay
    for (child <- 0 until 4) {
      boxIssue(unit).bits.aabb(child).x_min := Mux(asIsAabb,
        asReady.bits.data((RtVtasLayout.AabbMin / 4).toInt),
        asReady.bits.data((RtVtasLayout.Box4MinX / 4).toInt + child))
      boxIssue(unit).bits.aabb(child).y_min := Mux(asIsAabb,
        asReady.bits.data((RtVtasLayout.AabbMin / 4 + 1).toInt),
        asReady.bits.data((RtVtasLayout.Box4MinY / 4).toInt + child))
      boxIssue(unit).bits.aabb(child).z_min := Mux(asIsAabb,
        asReady.bits.data((RtVtasLayout.AabbMin / 4 + 2).toInt),
        asReady.bits.data((RtVtasLayout.Box4MinZ / 4).toInt + child))
      boxIssue(unit).bits.aabb(child).x_max := Mux(asIsAabb,
        asReady.bits.data((RtVtasLayout.AabbMax / 4).toInt),
        asReady.bits.data((RtVtasLayout.Box4MaxX / 4).toInt + child))
      boxIssue(unit).bits.aabb(child).y_max := Mux(asIsAabb,
        asReady.bits.data((RtVtasLayout.AabbMax / 4 + 1).toInt),
        asReady.bits.data((RtVtasLayout.Box4MaxY / 4).toInt + child))
      boxIssue(unit).bits.aabb(child).z_max := Mux(asIsAabb,
        asReady.bits.data((RtVtasLayout.AabbMax / 4 + 2).toInt),
        asReady.bits.data((RtVtasLayout.Box4MaxZ / 4).toInt + child))
    }
  }
  for (unit <- 0 until triangleUnitCount) {
    triangleIssue(unit).valid := asReady.valid && asIsTriangle && selectedIssueRayValid && triangleReadyMask.orR && triangleSelect === unit.U
    triangleIssue(unit).bits := 0.U.asTypeOf(new RtTriangleIntersectionInput)
    triangleIssue(unit).bits.requestId := requestId(asReady.bits.waiter.warpId, asReady.bits.waiter.laneId)
    triangleIssue(unit).bits.ray := selectedIssueRay
    triangleIssue(unit).bits.triangle.A.x := asReady.bits.data((RtVtasLayout.TriangleV0 / 4).toInt)
    triangleIssue(unit).bits.triangle.A.y := asReady.bits.data((RtVtasLayout.TriangleV0 / 4 + 1).toInt)
    triangleIssue(unit).bits.triangle.A.z := asReady.bits.data((RtVtasLayout.TriangleV0 / 4 + 2).toInt)
    triangleIssue(unit).bits.triangle.B.x := asReady.bits.data((RtVtasLayout.TriangleV1 / 4).toInt)
    triangleIssue(unit).bits.triangle.B.y := asReady.bits.data((RtVtasLayout.TriangleV1 / 4 + 1).toInt)
    triangleIssue(unit).bits.triangle.B.z := asReady.bits.data((RtVtasLayout.TriangleV1 / 4 + 2).toInt)
    triangleIssue(unit).bits.triangle.C.x := asReady.bits.data((RtVtasLayout.TriangleV2 / 4).toInt)
    triangleIssue(unit).bits.triangle.C.y := asReady.bits.data((RtVtasLayout.TriangleV2 / 4 + 1).toInt)
    triangleIssue(unit).bits.triangle.C.z := asReady.bits.data((RtVtasLayout.TriangleV2 / 4 + 2).toInt)
  }
  val instanceMaskMatch = traceMeta0(0)(asReady.bits.waiter.laneId)(15, 8) &
    asReady.bits.data((RtVtasLayout.InstanceMask / 4).toInt)(7, 0)
  instanceTransform.io.in.valid := asReady.valid && asIsInstance && !asUsesObjectRay && local.io.issueRay.valid && instanceMaskMatch.orR
  instanceTransform.io.in.bits := 0.U.asTypeOf(new RtInstanceTransformInput)
  instanceTransform.io.in.bits.laneId := asReady.bits.waiter.laneId
  instanceTransform.io.in.bits.ray := local.io.issueRay.ray
  for (word <- 0 until 12)
    instanceTransform.io.in.bits.matrix(word) := asReady.bits.data((RtVtasLayout.InstanceWorldToObject / 4).toInt + word)
  val asHeaderReady = asReady.valid && asIsHeader && (headerInvalid || stack.io.init.ready)
  val blasHeaderPush = asReady.valid && asIsBlasHeader && !headerInvalid
  val asBlasHeaderReady = asReady.valid && asIsBlasHeader &&
    (headerInvalid || stack.io.push.ready)
  val asBoxReady = asReady.valid && (asIsBox || asIsAabb) && selectedIssueRayValid && boxReadyMask.orR && boxIssue(boxSelect).ready
  val asTriangleReady = asReady.valid && asIsTriangle && selectedIssueRayValid && triangleReadyMask.orR && triangleIssue(triangleSelect).ready
  val asInstanceReady = asReady.valid && asIsInstance &&
    (!instanceMaskMatch.orR || instanceTransform.io.in.ready)
  table.io.asReadyLine.ready := asHeaderReady || asBlasHeaderReady || asBoxReady || asTriangleReady || asInstanceReady ||
    (asReady.valid && !asIsHeader && !asIsBlasHeader && !asIsBox && !asIsTriangle && !asIsInstance && !asIsAabb)
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
        when(asIsBox && waiter.objectRay) {
          printf(p"[RTCORE trace] box-bounds lane=${Decimal(waiter.laneId)}" +
            p" minX=[${Hexadecimal(asReady.bits.data(4))},${Hexadecimal(asReady.bits.data(5))},${Hexadecimal(asReady.bits.data(6))},${Hexadecimal(asReady.bits.data(7))}]" +
            p" minY=[${Hexadecimal(asReady.bits.data(8))},${Hexadecimal(asReady.bits.data(9))},${Hexadecimal(asReady.bits.data(10))},${Hexadecimal(asReady.bits.data(11))}]" +
            p" minZ=[${Hexadecimal(asReady.bits.data(12))},${Hexadecimal(asReady.bits.data(13))},${Hexadecimal(asReady.bits.data(14))},${Hexadecimal(asReady.bits.data(15))}]\\n")
          printf(p"[RTCORE trace] box-bounds lane=${Decimal(waiter.laneId)}" +
            p" maxX=[${Hexadecimal(asReady.bits.data(16))},${Hexadecimal(asReady.bits.data(17))},${Hexadecimal(asReady.bits.data(18))},${Hexadecimal(asReady.bits.data(19))}]" +
            p" maxY=[${Hexadecimal(asReady.bits.data(20))},${Hexadecimal(asReady.bits.data(21))},${Hexadecimal(asReady.bits.data(22))},${Hexadecimal(asReady.bits.data(23))}]" +
            p" maxZ=[${Hexadecimal(asReady.bits.data(24))},${Hexadecimal(asReady.bits.data(25))},${Hexadecimal(asReady.bits.data(26))},${Hexadecimal(asReady.bits.data(27))}]\\n")
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
    }.elsewhen(asIsBox || asIsAabb) {
      nextBoxIssue := boxSelect + 1.U
      laneState(0)(waiter.laneId) := sBoxInflight
      for (child <- 0 until 4) {
        boxChildRefs(0)(waiter.laneId)(child) := asReady.bits.data(child)
      }
      boxObjectRay(0)(waiter.laneId) := waiter.objectRay
      boxIsAabb(0)(waiter.laneId) := asIsAabb
      when(asIsAabb) {
        aabbMeta(0)(waiter.laneId).primitiveId := asReady.bits.data((RtVtasLayout.AabbPrimitiveId / 4).toInt)
        aabbMeta(0)(waiter.laneId).geometryId := asReady.bits.data((RtVtasLayout.AabbGeometryId / 4).toInt)
        aabbMeta(0)(waiter.laneId).sbtRecordOffset := asReady.bits.data((RtVtasLayout.AabbSbtRecordOffset / 4).toInt)
        aabbMeta(0)(waiter.laneId).hitKind := asReady.bits.data((RtVtasLayout.AabbHitKind / 4).toInt)
        aabbMeta(0)(waiter.laneId).instanceId := objectInstanceId(0)(waiter.laneId)
        aabbMeta(0)(waiter.laneId).instanceCustomIndex := objectCustomIndex(0)(waiter.laneId)
        aabbMeta(0)(waiter.laneId).instanceSbtRecordOffset := objectSbtRecordOffset(0)(waiter.laneId)
        aabbMeta(0)(waiter.laneId).instanceFlags := objectFlags(0)(waiter.laneId)
      }
    }.elsewhen(asIsTriangle) {
      nextTriangleIssue := triangleSelect + 1.U
      laneState(0)(waiter.laneId) := sTriangleInflight
      triangleMeta(0)(waiter.laneId).opaque := asReady.bits.data((RtVtasLayout.TriangleFlags / 4).toInt)(0)
      triangleMeta(0)(waiter.laneId).primitiveId := asReady.bits.data((RtVtasLayout.TrianglePrimitiveId / 4).toInt)
      triangleMeta(0)(waiter.laneId).geometryId := asReady.bits.data((RtVtasLayout.TriangleGeometryId / 4).toInt)
      triangleMeta(0)(waiter.laneId).sbtRecordOffset := asReady.bits.data((RtVtasLayout.TriangleSbtRecordOffset / 4).toInt)
      triangleMeta(0)(waiter.laneId).instanceId := objectInstanceId(0)(waiter.laneId)
      triangleMeta(0)(waiter.laneId).instanceCustomIndex := objectCustomIndex(0)(waiter.laneId)
      triangleMeta(0)(waiter.laneId).instanceSbtRecordOffset := objectSbtRecordOffset(0)(waiter.laneId)
      triangleMeta(0)(waiter.laneId).instanceFlags := objectFlags(0)(waiter.laneId)
    }.elsewhen(asIsInstance) {
      when(!instanceMaskMatch.orR) {
        laneState(0)(waiter.laneId) := sStackReady
      }.otherwise {
        objectBase(0)(waiter.laneId) := asReady.bits.data((RtVtasLayout.InstanceBlasAddrLo / 4).toInt)
        objectInstanceId(0)(waiter.laneId) := asReady.bits.data((RtVtasLayout.InstanceInstanceId / 4).toInt)
        objectCustomIndex(0)(waiter.laneId) := asReady.bits.data((RtVtasLayout.InstanceCustomIndex / 4).toInt)(23, 0)
        objectSbtRecordOffset(0)(waiter.laneId) := asReady.bits.data((RtVtasLayout.InstanceSbtRecordOffset / 4).toInt)(23, 0)
        objectFlags(0)(waiter.laneId) := asReady.bits.data((RtVtasLayout.InstanceFlags / 4).toInt)
        for (word <- 0 until 12)
          objectMatrix(0)(waiter.laneId)(word) := asReady.bits.data((RtVtasLayout.InstanceWorldToObject / 4).toInt + word)
        laneState(0)(waiter.laneId) := sInstanceTransform
      }
    }.otherwise {
      laneState(0)(waiter.laneId) := sStackReady
    }
  }

  // Box results are sorted near-to-far.  Push them in reverse order so the
  // subsequent scalar stack pop continues DFS at the nearest child.
  val boxResultMask = VecInit(boxResult.map(_.valid)).asUInt
  val boxResultSelect = roundRobin(boxResultMask, nextBoxResult, boxUnitCount)
  for (unit <- 0 until boxUnitCount) {
    boxResult(unit).ready := boxResultMask.orR && boxResultSelect === unit.U
  }
  when(boxResultMask.orR && boxResult(boxResultSelect).fire) {
    nextBoxResult := boxResultSelect + 1.U
    val result = boxResult(boxResultSelect).bits
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
        candidateData(warp)(lane)(RtAbiLayout.HitRecordMeta0) :=
          (1 | (RtAbiLayout.HitRecordStatusValid << RtAbiLayout.HitRecordMeta0StatusShift)).U
        candidateData(warp)(lane)(RtAbiLayout.HitRecordMeta1) :=
          aabbMeta(warp)(lane).instanceSbtRecordOffset
        candidateData(warp)(lane)(RtAbiLayout.HitRecordHitT) := result.tmin(0)
        candidateData(warp)(lane)(RtAbiLayout.HitRecordBarycentricsX) := 0.U
        candidateData(warp)(lane)(RtAbiLayout.HitRecordBarycentricsY) := 0.U
        candidateData(warp)(lane)(RtAbiLayout.HitRecordPrimitiveId) := aabbMeta(warp)(lane).primitiveId
        candidateData(warp)(lane)(RtAbiLayout.HitRecordInstanceId) := aabbMeta(warp)(lane).instanceId
        candidateData(warp)(lane)(RtAbiLayout.HitRecordGeometryId) := aabbMeta(warp)(lane).geometryId
        candidateData(warp)(lane)(RtAbiLayout.HitRecordHitKind) := aabbMeta(warp)(lane).hitKind
        laneState(warp)(lane) := sAabbCandidate
      }
    }.otherwise {
      laneState(warp)(lane) := Mux(count === 0.U, sStackReady, sBoxPush)
      boxDeferredCount(warp)(lane) := count
      boxPushedCount(warp)(lane) := 0.U
      for (index <- 0 until 4) {
        boxDeferredRefs(warp)(lane)(index) := boxChildRefs(warp)(lane)(result.boxIndex(index))
      }
    }
  }

  val pushMasks = Wire(Vec(residentWarpCount, UInt(num_thread.W)))
  for (warp <- 0 until residentWarpCount) {
    pushMasks(warp) := VecInit((0 until num_thread).map(lane =>
      laneState(warp)(lane) === sBoxPush)).asUInt
  }
  val hasPush = pushMasks.map(_.orR).reduce(_ || _)
  val pushWarp = 0.U(residentWarpIdWidth.W)
  val pushLane = roundRobin(pushMasks(pushWarp), nextPushLane(pushWarp), num_thread)
  val pushIndex = boxDeferredCount(pushWarp)(pushLane) - boxPushedCount(pushWarp)(pushLane) - 1.U
  // A BLAS root is a continuation beneath the already-live TLAS stack.  Give
  // it priority over deferred Box4 children so the AS response cannot hold the
  // table while waiting for a later traversal action.
  stack.io.push.valid := blasHeaderPush || hasPush
  stack.io.push.bits.laneMask := Mux(blasHeaderPush,
    UIntToOH(asReady.bits.waiter.laneId, num_thread), UIntToOH(pushLane, num_thread))
  for (lane <- 0 until num_thread) {
    stack.io.push.bits.nodeRef(lane) := Mux(blasHeaderPush, headerRoot,
      boxDeferredRefs(pushWarp)(pushLane)(pushIndex(1, 0)))
    stack.io.push.bits.objectRay(lane) := Mux(blasHeaderPush, true.B,
      boxObjectRay(pushWarp)(pushLane))
  }
  when(stack.io.push.fire) {
    assert(!stack.io.pushWouldOverflow, "RtTraversalStack overflow requires spill support")
    when(blasHeaderPush) {
      laneState(0)(asReady.bits.waiter.laneId) := sStackReady
    }.otherwise {
      nextPushLane(pushWarp) := pushLane + 1.U
      when(boxPushedCount(pushWarp)(pushLane) + 1.U === boxDeferredCount(pushWarp)(pushLane)) {
        laneState(pushWarp)(pushLane) := sStackReady
      }.otherwise {
        boxPushedCount(pushWarp)(pushLane) := boxPushedCount(pushWarp)(pushLane) + 1.U
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
  local.io.resultRayLookup.laneId := selectedTriangleLane
  val tBoundProduct = Module(new MulRecFN(8, 24))
  tBoundProduct.io.a := recFNFromFN(8, 24, local.io.resultRay.currentTmax)
  tBoundProduct.io.b := recFNFromFN(8, 24, selectedTriangleResult.tDenom)
  tBoundProduct.io.roundingMode := consts.round_near_even
  tBoundProduct.io.detectTininess := consts.tininess_beforeRounding
  val tMinBoundProduct = Module(new MulRecFN(8, 24))
  tMinBoundProduct.io.a := recFNFromFN(8, 24, local.io.resultRay.ray.originalTmin)
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
  val acceptedTriangleResult = selectedTriangleResult.hit && local.io.resultRay.valid &&
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
      (local.io.resultRay.valid && (!acceptedTriangleResult || triangleResolve.io.in.ready)))
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
  triangleHitUpdate.dirty := Fill(RtMemTableParameters.hitRecordStorageWordCount, 1.U(1.W))
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
  aabbHitUpdate.dirty := Fill(RtMemTableParameters.hitRecordStorageWordCount, 1.U(1.W))
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
  val triangleTmaxUpdate = Wire(new RtLocalRayTmaxUpdate)
  triangleTmaxUpdate.laneId := resultLane
  triangleTmaxUpdate.tmax := triangleResolve.io.out.bits.t
  local.io.rayTmaxUpdate.valid := triangleResolve.io.out.valid && triangleIsOpaque
  local.io.rayTmaxUpdate.bits := triangleTmaxUpdate
  triangleResolve.io.out.ready := !aabbCandidateMask.orR && local.io.hitUpdate.ready &&
    (!triangleIsOpaque || local.io.rayTmaxUpdate.ready)
  when(triangleResolve.io.out.fire) {
    when(triangleIsOpaque) {
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
  local.io.drained.ready := !completionPending && !finalRetirePending
  when(local.io.drained.fire) {
    completionBits.warpId := residentOwnerWarpId
    completionBits.issueId := warpIssueId(0)
    completionBits.destinationVd := warpDestination(0)
    completionBits.completionMask := commandMask(0)
    completionBits.laneStatus := laneStatus(0)
    completionPending := true.B
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
  io.completion.valid := completionPending
  io.completion.bits := completionBits
  when(io.completion.fire) {
    completionPending := false.B
    completionPublished(0) := true.B
    finishIssued(0) := false.B
  }

  // `release` always destroys the whole retained session.  The software has
  // already decided that every originally active lane is terminal; the mask
  // is retained solely to catch ABI/control-flow mistakes at this boundary.
  val releasePending = RegInit(false.B)
  val releaseStackCleared = RegInit(false.B)
  val releaseLanesInvalidated = RegInit(false.B)
  retiring := releasePending || finalRetirePending || io.release.valid
  val releaseAllowed = sessionActive(0) && completionPublished(0) && !completionPending &&
    io.release.bits.warpId === residentOwnerWarpId &&
    io.release.bits.activeMask === activeMask(0)
  io.release.ready := releaseAllowed && !releasePending && !finalRetirePending
  when(io.release.fire) {
    releasePending := true.B
    releaseStackCleared := false.B
    releaseLanesInvalidated := false.B
  }
  when(io.release.fire) {
    assert(io.release.bits.warpId === residentOwnerWarpId,
      "release warp must match the resident RT session")
    assert(io.release.bits.activeMask === activeMask(0),
      "release must cover the complete original RT session mask")
    for (lane <- 0 until num_thread) when(activeMask(0)(lane)) {
      assert(laneState(0)(lane) === sTerminal,
        "release may retire only a completed retained lane")
    }
  }
  // A nested traversal starts from the shader's newly written local ray and
  // must not inherit stale DFS entries from the preceding opaque/miss trace.
  // Its Local gather begins in the following cycle, so this same-cycle clear
  // is complete before the new header can initialise a root node.
  stack.io.clear.valid := (releasePending && !releaseStackCleared) || restartStackClearRequest
  stack.io.clear.bits.laneMask := Mux(releasePending, activeMask(0), io.command.bits.activeMask)
  when(stack.io.clear.fire && releasePending) { releaseStackCleared := true.B }
  when(releasePending && releaseStackCleared && !releaseLanesInvalidated) {
    releaseLanesInvalidated := true.B
    finalRetirePending := true.B
    for (lane <- 0 until num_thread) when(activeMask(0)(lane)) {
      laneState(0)(lane) := sIdle
      candidateValid(0)(lane) := false.B
      committedValid(0)(lane) := false.B
      laneStatus(0)(lane) := 0.U
      boxDeferredCount(0)(lane) := 0.U
      boxPushedCount(0)(lane) := 0.U
    }
  }
  local.io.retire.valid := finalRetirePending
  local.io.retire.bits := residentOwnerWarpId
  io.releaseAck.valid := finalRetirePending && local.io.retire.ready && table.io.quiescent
  io.releaseAck.bits.warpId := residentOwnerWarpId
  io.releaseAck.bits.releasedMask := activeMask(0)
  when(io.releaseAck.fire) {
    releasePending := false.B
    releaseStackCleared := false.B
    releaseLanesInvalidated := false.B
    sessionActive(0) := false.B
    finishIssued(0) := false.B
    completionPublished(0) := false.B
    activeMask(0) := 0.U
    commandMask(0) := 0.U
    finalRetirePending := false.B
    for (lane <- 0 until num_thread) { laneState(0)(lane) := sIdle }
  }

  // Export only physical handshakes.  In particular, local SRAM is private to
  // RTcore: these events must not be relabelled as generic LSU traffic.
  io.perf.localSramReadRequest := io.localRequest.fire && !io.localRequest.bits.write
  io.perf.localSramWriteRequest := io.localRequest.fire && io.localRequest.bits.write
  io.perf.localSramReadResponse := io.localResponse.fire
  io.perf.localSramBackpressure := io.localRequest.valid && !io.localRequest.ready
  io.perf.raySetupOutput := raySetup.io.out.fire
  io.perf.asLineAcquire := table.io.asAcquire.fire
  io.perf.asLineReady := table.io.asReadyLine.fire
  io.perf.asAcquireBackpressure := table.io.asAcquire.valid && !table.io.asAcquire.ready
  io.perf.asCacheResidentHit := table.io.asCacheResidentHit
  io.perf.asCachePendingMerge := table.io.asCachePendingMerge
  io.perf.asCacheMiss := table.io.asCacheMiss
  io.perf.asCacheRefill := table.io.asCacheRefill
  io.perf.asCacheEviction := table.io.asCacheEviction
  io.perf.box4Issue := boxIssue.map(_.fire).reduce(_ || _)
  io.perf.box4Result := boxResult.map(_.fire).reduce(_ || _)
  io.perf.triangleIssue := triangleIssue.map(_.fire).reduce(_ || _)
  io.perf.triangleResult := triangleResult.map(_.fire).reduce(_ || _)
  io.perf.triangleResolveIssue := triangleResolve.io.in.fire
  io.perf.triangleResolveResult := triangleResolve.io.out.fire
  io.perf.stackPush := stack.io.push.fire
  io.perf.stackPop := stack.io.pop.fire
}

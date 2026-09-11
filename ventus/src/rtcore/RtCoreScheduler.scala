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
  val header = 0.U(2.W)
  val box4 = 1.U(2.W)
  val triangle = 2.U(2.W)
}

private[rtcore] class RtTriangleDispatchMeta extends Bundle {
  val opaque = Bool()
  val primitiveId = UInt(xLen.W)
  val geometryId = UInt(xLen.W)
  val sbtRecordOffset = UInt(xLen.W)
}

/**
  * Warp/lane traversal controller.
  *
  * It is the RTcore control boundary: commands start RT Local SRAM gather,
  * local-ready notifications feed setup and traversal, and only this module
  * chooses an IU destination.  RtMemTable remains the sole LSU client.  This
  * baseline intentionally traverses BLAS Box4/Triangle nodes only.  It does
  * not access software lifecycle status, but re-reads callback control when
  * a retained candidate is resumed.
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
    sResumePending) = Enum(11)
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
  val triangleMeta = Reg(Vec(residentWarpCount, Vec(num_thread, new RtTriangleDispatchMeta)))
  val candidateValid = RegInit(VecInit(Seq.fill(residentWarpCount)(VecInit(Seq.fill(num_thread)(false.B)))))
  val candidateData = Reg(Vec(residentWarpCount, Vec(num_thread,
    Vec(RtMemTableParameters.hitRecordStorageWordCount, UInt(xLen.W)))))
  val residentOwnerWarpId = Reg(UInt(depth_warp.W))

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

  // A new command either creates a physical session or resumes retained
  // candidate lanes.  RT Local control is the callback authority on resume.
  val commandSlot = 0.U(residentWarpIdWidth.W)
  val freshCommand = io.command.bits.activeMask.orR && !sessionActive(commandSlot)
  val resumeCommand = io.command.bits.activeMask.orR && sessionActive(commandSlot) && completionPublished(commandSlot)
  val commandAllowed = freshCommand || resumeCommand
  local.io.admission.valid := io.command.valid && freshCommand
  local.io.admission.bits.warpId := io.command.bits.warpId
  local.io.admission.bits.activeMask := io.command.bits.activeMask
  local.io.resume.valid := io.command.valid && resumeCommand
  local.io.resume.bits.activeMask := io.command.bits.activeMask
  io.command.ready := Mux(freshCommand,
    local.io.admission.ready, local.io.resume.ready)
  when(io.command.fire) {
    val warp = commandSlot
    residentOwnerWarpId := io.command.bits.warpId
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

  // Resume reloads only the software callback decision.  Ray-state tmax is
  // immutable for a session; accepting a candidate narrows the private bound
  // to that candidate's hitT, while ignoring one leaves the bound unchanged.
  // Candidate acceptance updates the same local hit-record buffer as an IU hit.  It
  // receives priority because the software callback is the explicit resume
  // boundary; Triangle output remains back-pressured for that one cycle.
  val resumeCommit = local.io.resumeOut.valid &&
    local.io.resumeOut.bits.callbackDecision === RtAbiLayout.CallbackAccept.U
  local.io.resumeOut.ready := !resumeCommit ||
    (local.io.hitUpdate.ready && local.io.rayTmaxUpdate.ready)
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
    when(resumeAccept || resumeTerminate) { candidateValid(0)(resumeLane) := false.B }
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
  local.io.laneOut.ready := raySetup.io.in.ready
  when(local.io.laneOut.fire) { assert(laneState(0)(local.io.laneOut.bits.laneId) === sRayLoad) }

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
    laneState(0)(raySetup.io.out.bits.laneId) := sHeaderRequest
  }

  // A setup-complete header request and a stack pop share the AS acquire port.
  // Header fetches are preferred so newly admitted rays do not wait behind traversal.
  val headerMasks = Wire(Vec(residentWarpCount, UInt(num_thread.W)))
  for (warp <- 0 until residentWarpCount) {
    headerMasks(warp) := VecInit((0 until num_thread).map(lane =>
      laneState(warp)(lane) === sHeaderRequest)).asUInt
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
  val selectedNodeType = nodeType(selectedNodeRef)
  val selectedUnsupported = selectedNodeType =/= RtVtasLayout.NodeBox4.U &&
    selectedNodeType =/= RtVtasLayout.NodeTriangle.U
  val selectedInvalid = selectedNodeRef === RtVtasLayout.InvalidNodeRef.U(32.W)
  val anyBoxPush = (0 until residentWarpCount).map { warp =>
    (0 until num_thread).map(lane => laneState(warp)(lane) === sBoxPush).reduce(_ || _)
  }.reduce(_ || _)
  val headerLineActive = table.io.asReadyLine.valid &&
    table.io.asReadyLine.bits.waiter.nodeKind === RtVtasNodeKind.header
  val popNeedsAs = hasStackReady && !selectedEmpty && !selectedInvalid && !selectedUnsupported
  // Stack writes/initialisation are serialized ahead of a pop.  In
  // particular, never let an AS acquire consume a node unless the matching pop
  // can fire in the same cycle.
  local.io.nodeRayLookup.laneId := Mux(hasHeaderRequest, headerLane, popLane)
  val headerAcquire = hasHeaderRequest && local.io.nodeRay.valid
  val popMayFire = !headerAcquire && !anyBoxPush && !headerLineActive
  val nodeAcquire = popMayFire && popNeedsAs

  table.io.asAcquire.valid := headerAcquire || nodeAcquire
  table.io.asAcquire.bits := 0.U.asTypeOf(new RtAsTableAcquire)
  when(headerAcquire) {
    table.io.asAcquire.bits.lineAddress := local.io.nodeRay.asBase
    table.io.asAcquire.bits.wordMask := Fill(RtMemTableParameters.lineWordCount, 1.U(1.W))
    table.io.asAcquire.bits.is64B := false.B
    table.io.asAcquire.bits.waiter.warpId := headerWarp
    table.io.asAcquire.bits.waiter.laneId := headerLane
    table.io.asAcquire.bits.waiter.nodeKind := RtVtasNodeKind.header
  }.elsewhen(nodeAcquire) {
    table.io.asAcquire.bits.lineAddress := local.io.nodeRay.asBase + nodeOffset(selectedNodeRef)
    table.io.asAcquire.bits.wordMask := Mux(selectedNodeType === RtVtasLayout.NodeTriangle.U,
      Cat(0.U(16.W), Fill(16, 1.U(1.W))), Fill(32, 1.U(1.W)))
    table.io.asAcquire.bits.is64B := selectedNodeType === RtVtasLayout.NodeTriangle.U
    table.io.asAcquire.bits.waiter.warpId := popWarp
    table.io.asAcquire.bits.waiter.laneId := popLane
    table.io.asAcquire.bits.waiter.nodeKind := Mux(selectedNodeType === RtVtasLayout.NodeBox4.U,
      RtVtasNodeKind.box4, RtVtasNodeKind.triangle)
  }
  when(table.io.asAcquire.fire && headerAcquire) {
    laneState(headerWarp)(headerLane) := sHeaderPending
  }

  // An empty stack terminates a lane without an LSU request.  Invalid and
  // unsupported node refs are terminal in this BLAS-only baseline.
  when(popMayFire && hasStackReady && selectedEmpty) {
    laneState(popWarp)(popLane) := sTerminal
    laneStatus(popWarp)(popLane) := Mux(committedValid(popWarp)(popLane),
      RtAbiLayout.TraversalCompleteHit.U, RtAbiLayout.TraversalCompleteMiss.U)
  }
  stack.io.pop.valid := popMayFire && hasStackReady && !selectedEmpty &&
    (selectedInvalid || selectedUnsupported || table.io.asAcquire.ready)
  stack.io.pop.bits.laneMask := UIntToOH(popLane, num_thread)
  when(stack.io.pop.fire) {
    nextStackLane(popWarp) := popLane + 1.U
    when(selectedInvalid || selectedUnsupported) {
      laneState(popWarp)(popLane) := sTerminal
      laneStatus(popWarp)(popLane) := Mux(selectedInvalid,
        Mux(committedValid(popWarp)(popLane), RtAbiLayout.TraversalCompleteHit.U,
          RtAbiLayout.TraversalCompleteMiss.U), RtAbiLayout.TraversalTerminated.U)
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
  val headerRoot = asReady.bits.data((RtVtasLayout.AsHeaderRootNodeRef / 4).toInt)
  val headerInvalid = headerRoot === RtVtasLayout.InvalidNodeRef.U(32.W)
  val asIsHeader = asReady.bits.waiter.nodeKind === RtVtasNodeKind.header
  val asIsBox = asReady.bits.waiter.nodeKind === RtVtasNodeKind.box4
  val asIsTriangle = asReady.bits.waiter.nodeKind === RtVtasNodeKind.triangle
  stack.io.init.valid := asReady.valid && asIsHeader && !headerInvalid
  stack.io.init.bits.laneMask := UIntToOH(asReady.bits.waiter.laneId, num_thread)
  for (lane <- 0 until num_thread) {
    stack.io.init.bits.rootNodeRef(lane) := headerRoot
  }
  for (unit <- 0 until boxUnitCount) {
    boxIssue(unit).valid := asReady.valid && asIsBox && local.io.issueRay.valid && boxReadyMask.orR && boxSelect === unit.U
    boxIssue(unit).bits := 0.U.asTypeOf(new RtBox4IntersectionInput)
    boxIssue(unit).bits.requestId := requestId(asReady.bits.waiter.warpId, asReady.bits.waiter.laneId)
    boxIssue(unit).bits.ray := local.io.issueRay.ray
    for (child <- 0 until 4) {
      boxIssue(unit).bits.aabb(child).x_min := asReady.bits.data((RtVtasLayout.Box4MinX / 4).toInt + child)
      boxIssue(unit).bits.aabb(child).y_min := asReady.bits.data((RtVtasLayout.Box4MinY / 4).toInt + child)
      boxIssue(unit).bits.aabb(child).z_min := asReady.bits.data((RtVtasLayout.Box4MinZ / 4).toInt + child)
      boxIssue(unit).bits.aabb(child).x_max := asReady.bits.data((RtVtasLayout.Box4MaxX / 4).toInt + child)
      boxIssue(unit).bits.aabb(child).y_max := asReady.bits.data((RtVtasLayout.Box4MaxY / 4).toInt + child)
      boxIssue(unit).bits.aabb(child).z_max := asReady.bits.data((RtVtasLayout.Box4MaxZ / 4).toInt + child)
    }
  }
  for (unit <- 0 until triangleUnitCount) {
    triangleIssue(unit).valid := asReady.valid && asIsTriangle && local.io.issueRay.valid && triangleReadyMask.orR && triangleSelect === unit.U
    triangleIssue(unit).bits := 0.U.asTypeOf(new RtTriangleIntersectionInput)
    triangleIssue(unit).bits.requestId := requestId(asReady.bits.waiter.warpId, asReady.bits.waiter.laneId)
    triangleIssue(unit).bits.ray := local.io.issueRay.ray
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
  val asHeaderReady = asReady.valid && asIsHeader && (headerInvalid || stack.io.init.ready)
  val asBoxReady = asReady.valid && asIsBox && local.io.issueRay.valid && boxReadyMask.orR && boxIssue(boxSelect).ready
  val asTriangleReady = asReady.valid && asIsTriangle && local.io.issueRay.valid && triangleReadyMask.orR && triangleIssue(triangleSelect).ready
  table.io.asReadyLine.ready := asHeaderReady || asBoxReady || asTriangleReady ||
    (asReady.valid && !asIsHeader && !asIsBox && !asIsTriangle)
  when(asReady.fire) {
    val waiter = asReady.bits.waiter
    when(asIsHeader) {
      when(headerInvalid) {
        laneState(0)(waiter.laneId) := sTerminal
        laneStatus(0)(waiter.laneId) := RtAbiLayout.TraversalCompleteMiss.U
      }.otherwise {
        laneState(0)(waiter.laneId) := sStackReady
      }
    }.elsewhen(asIsBox) {
      nextBoxIssue := boxSelect + 1.U
      laneState(0)(waiter.laneId) := sBoxInflight
      for (child <- 0 until 4) {
        boxChildRefs(0)(waiter.laneId)(child) := asReady.bits.data(child)
      }
    }.elsewhen(asIsTriangle) {
      nextTriangleIssue := triangleSelect + 1.U
      laneState(0)(waiter.laneId) := sTriangleInflight
      triangleMeta(0)(waiter.laneId).opaque := asReady.bits.data((RtVtasLayout.TriangleFlags / 4).toInt)(0)
      triangleMeta(0)(waiter.laneId).primitiveId := asReady.bits.data((RtVtasLayout.TrianglePrimitiveId / 4).toInt)
      triangleMeta(0)(waiter.laneId).geometryId := asReady.bits.data((RtVtasLayout.TriangleGeometryId / 4).toInt)
      triangleMeta(0)(waiter.laneId).sbtRecordOffset := asReady.bits.data((RtVtasLayout.TriangleSbtRecordOffset / 4).toInt)
    }.otherwise {
      laneState(0)(waiter.laneId) := sTerminal
      laneStatus(0)(waiter.laneId) := RtAbiLayout.TraversalTerminated.U
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
    val count = PopCount(result.isIntersect)
    laneState(warp)(lane) := Mux(count === 0.U, sStackReady, sBoxPush)
    boxDeferredCount(warp)(lane) := count
    boxPushedCount(warp)(lane) := 0.U
    for (index <- 0 until 4) {
      boxDeferredRefs(warp)(lane)(index) := boxChildRefs(warp)(lane)(result.boxIndex(index))
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
  stack.io.push.valid := hasPush
  stack.io.push.bits.laneMask := UIntToOH(pushLane, num_thread)
  for (lane <- 0 until num_thread) {
    stack.io.push.bits.nodeRef(lane) := boxDeferredRefs(pushWarp)(pushLane)(pushIndex(1, 0))
  }
  when(stack.io.push.fire) {
    nextPushLane(pushWarp) := pushLane + 1.U
    assert(!stack.io.pushWouldOverflow, "RtTraversalStack overflow requires spill support")
    when(boxPushedCount(pushWarp)(pushLane) + 1.U === boxDeferredCount(pushWarp)(pushLane)) {
      laneState(pushWarp)(pushLane) := sStackReady
    }.otherwise {
      boxPushedCount(pushWarp)(pushLane) := boxPushedCount(pushWarp)(pushLane) + 1.U
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
      (triangleIsOpaque.asUInt << RtAbiLayout.HitRecordMeta0OpaqueShift)
  triangleHitUpdate.data(RtAbiLayout.HitRecordMeta1) := triangleMeta(resultWarp)(resultLane).sbtRecordOffset
  triangleHitUpdate.data(RtAbiLayout.HitRecordHitT) := triangleResolve.io.out.bits.t
  triangleHitUpdate.data(RtAbiLayout.HitRecordBarycentricsX) := triangleResolve.io.out.bits.u
  triangleHitUpdate.data(RtAbiLayout.HitRecordBarycentricsY) := triangleResolve.io.out.bits.v
  triangleHitUpdate.data(RtAbiLayout.HitRecordPrimitiveId) := triangleMeta(resultWarp)(resultLane).primitiveId
  triangleHitUpdate.data(RtAbiLayout.HitRecordInstanceId) := 0.U
  triangleHitUpdate.data(RtAbiLayout.HitRecordGeometryId) := triangleMeta(resultWarp)(resultLane).geometryId
  triangleHitUpdate.data(RtAbiLayout.HitRecordHitKind) := "hfe".U
  val resumeHitUpdate = Wire(new RtLocalHitRecordUpdate)
  resumeHitUpdate := 0.U.asTypeOf(new RtLocalHitRecordUpdate)
  resumeHitUpdate.laneId := resumeLane
  resumeHitUpdate.committed := true.B
  resumeHitUpdate.dirty := Fill(RtMemTableParameters.hitRecordStorageWordCount, 1.U(1.W))
  resumeHitUpdate.data := candidateData(0)(resumeLane)
  local.io.hitUpdate.valid := resumeCommit || triangleResolve.io.out.valid
  local.io.hitUpdate.bits := Mux(resumeCommit, resumeHitUpdate, triangleHitUpdate)
  val triangleTmaxUpdate = Wire(new RtLocalRayTmaxUpdate)
  triangleTmaxUpdate.laneId := resultLane
  triangleTmaxUpdate.tmax := triangleResolve.io.out.bits.t
  val resumeTmaxUpdate = Wire(new RtLocalRayTmaxUpdate)
  resumeTmaxUpdate.laneId := resumeLane
  resumeTmaxUpdate.tmax := candidateData(0)(resumeLane)(RtAbiLayout.HitRecordHitT)
  local.io.rayTmaxUpdate.valid := resumeCommit ||
    (triangleResolve.io.out.valid && triangleIsOpaque)
  local.io.rayTmaxUpdate.bits := Mux(resumeCommit, resumeTmaxUpdate, triangleTmaxUpdate)
  triangleResolve.io.out.ready := !resumeCommit && local.io.hitUpdate.ready &&
    (!triangleIsOpaque || local.io.rayTmaxUpdate.ready)
  when(triangleResolve.io.out.fire) {
    when(triangleIsOpaque) {
      committedValid(resultWarp)(resultLane) := true.B
      laneState(resultWarp)(resultLane) := sStackReady
    }.otherwise {
      laneState(resultWarp)(resultLane) := sTerminal
      laneStatus(resultWarp)(resultLane) := RtAbiLayout.TraversalCandidateNonOpaqueTriangle.U
      candidateValid(resultWarp)(resultLane) := true.B
      candidateData(resultWarp)(resultLane) := triangleHitUpdate.data
    }
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
  val releaseBits = Reg(new RtReleaseCommand)
  val releaseStackCleared = RegInit(false.B)
  val releaseLanesInvalidated = RegInit(false.B)
  val releaseAllowed = sessionActive(0) && completionPublished(0) && !completionPending &&
    io.release.bits.activeMask === activeMask(0)
  io.release.ready := releaseAllowed && !releasePending && !finalRetirePending
  when(io.release.fire) {
    releasePending := true.B
    releaseBits := io.release.bits
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
  stack.io.clear.valid := releasePending && !releaseStackCleared
  stack.io.clear.bits.laneMask := activeMask(0)
  when(stack.io.clear.fire) { releaseStackCleared := true.B }
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
}

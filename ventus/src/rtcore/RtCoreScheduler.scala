package rtcore

import chisel3._
import chisel3.util._
import hardfloat._
import top.parameters._

object RtCoreSchedulerParameters {
  val boxUnitCount = 8
  val triangleUnitCount = 8
  require(RtCorePortParameters.requestIdWidth == depth_warp + log2Ceil(num_thread))
}

private[rtcore] object RtVtasNodeKind {
  val header = 0.U(2.W)
  val box4 = 1.U(2.W)
  val triangle = 2.U(2.W)
  val unsupported = 3.U(2.W)
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
  * It is the RTcore control boundary: commands admit PDS read/write state,
  * table-ready notifications feed setup and traversal, and only this module
  * chooses an IU destination.  RtMemTable remains the sole LSU client.  This
  * baseline intentionally traverses BLAS Box4/Triangle nodes only; status and
  * control PDS fields are not read or written here.
  */
class RtCoreScheduler extends Module {
  import RtCoreSchedulerParameters._

  val io = IO(new RtCoreIO)
  val boxIssue = IO(Vec(boxUnitCount, Decoupled(new RtBox4IntersectionInput)))
  val boxResult = IO(Flipped(Vec(boxUnitCount, Decoupled(new RtBox4IntersectionOutput))))
  val triangleIssue = IO(Vec(triangleUnitCount, Decoupled(new RtTriangleIntersectionInput)))
  val triangleResult = IO(Flipped(Vec(triangleUnitCount, Decoupled(new RtTriangleIntersectionOutput))))

  val table = Module(new RtMemTable)
  val stack = Module(new RtTraversalStack)
  val raySetup = Module(new RtRaySetup)
  val triangleResolve = Module(new RtTriangleResolve)
  io.rtLsuReport <> table.io.rtLsuReport
  table.io.rtLsuRespond <> io.rtLsuRespond

  val Seq(sIdle, sPdsRead, sHeaderPending, sStackReady, sNodePending,
    sBoxInflight, sBoxPush, sTriangleInflight, sTerminal) = Enum(9)
  val laneState = RegInit(VecInit(Seq.fill(num_warp)(VecInit(Seq.fill(num_thread)(sIdle)))))
  val laneStatus = Reg(Vec(num_warp, Vec(num_thread, UInt(xLen.W))))
  val laneRay = Reg(Vec(num_warp, Vec(num_thread, new Ray(recorded_float = false))))
  val laneAsBase = Reg(Vec(num_warp, Vec(num_thread, UInt(memReqLen.W))))
  val laneTmax = Reg(Vec(num_warp, Vec(num_thread, UInt(32.W))))
  val committedValid = RegInit(VecInit(Seq.fill(num_warp)(VecInit(Seq.fill(num_thread)(false.B)))))
  val activeMask = RegInit(VecInit(Seq.fill(num_warp)(0.U(num_thread.W))))
  val warpIssueId = Reg(Vec(num_warp, UInt(RtCorePortParameters.issueIdWidth.W)))
  val warpDestination = Reg(Vec(num_warp, UInt(RtCorePortParameters.regidx_totalwidth.W)))
  val warpBusy = RegInit(VecInit(Seq.fill(num_warp)(false.B)))
  val finishIssued = RegInit(VecInit(Seq.fill(num_warp)(false.B)))
  val boxChildRefs = Reg(Vec(num_warp, Vec(num_thread, Vec(4, UInt(32.W)))))
  val boxDeferredRefs = Reg(Vec(num_warp, Vec(num_thread, Vec(4, UInt(32.W)))))
  val boxDeferredCount = Reg(Vec(num_warp, Vec(num_thread, UInt(3.W))))
  val boxPushedCount = Reg(Vec(num_warp, Vec(num_thread, UInt(3.W))))
  val triangleMeta = Reg(Vec(num_warp, Vec(num_thread, new RtTriangleDispatchMeta)))

  private def requestId(warp: UInt, lane: UInt): UInt = Cat(warp, lane)
  private def requestWarp(id: UInt): UInt = id(RtCorePortParameters.requestIdWidth - 1, log2Ceil(num_thread))
  private def requestLane(id: UInt): UInt = id(log2Ceil(num_thread) - 1, 0)
  private def nodeType(nodeRef: UInt): UInt = nodeRef(2, 0)
  private def nodeOffset(nodeRef: UInt): UInt = nodeRef & RtVtasLayout.NodeRefOffsetMask.U(32.W)
  private def roundRobin(mask: UInt, start: UInt, count: Int): UInt = {
    require(count > 1 && (count & (count - 1)) == 0)
    val rotated = (Cat(mask, mask) >> start)(count - 1, 0)
    (start + PriorityEncoder(rotated))(log2Ceil(count) - 1, 0)
  }
  val nextStackWarp = RegInit(0.U(depth_warp.W))
  val nextStackLane = RegInit(VecInit(Seq.fill(num_warp)(0.U(log2Ceil(num_thread).W))))
  val nextPushWarp = RegInit(0.U(depth_warp.W))
  val nextPushLane = RegInit(VecInit(Seq.fill(num_warp)(0.U(log2Ceil(num_thread).W))))
  val nextBoxIssue = RegInit(0.U(log2Ceil(boxUnitCount).W))
  val nextTriangleIssue = RegInit(0.U(log2Ceil(triangleUnitCount).W))
  val nextBoxResult = RegInit(0.U(log2Ceil(boxUnitCount).W))
  val nextTriangleResult = RegInit(0.U(log2Ceil(triangleUnitCount).W))

  // Command admission is intentionally fresh-only for this baseline.  A lane
  // cannot be reused until its PDS records have drained and read context retires.
  val commandWarpBusy = warpBusy(io.command.bits.warpId)
  val commandAllowed = io.command.bits.activeMask.orR && !commandWarpBusy
  table.io.pdsReadAdmission.valid := io.command.valid && commandAllowed
  table.io.pdsReadAdmission.bits.warpId := io.command.bits.warpId
  table.io.pdsReadAdmission.bits.pds := io.command.bits.pds
  table.io.pdsReadAdmission.bits.activeMask := io.command.bits.activeMask
  table.io.pdsWriteAdmission.valid := io.command.valid && commandAllowed
  table.io.pdsWriteAdmission.bits.warpId := io.command.bits.warpId
  table.io.pdsWriteAdmission.bits.pds := io.command.bits.pds
  table.io.pdsWriteAdmission.bits.activeMask := io.command.bits.activeMask
  io.command.ready := commandAllowed && table.io.pdsReadAdmission.ready && table.io.pdsWriteAdmission.ready
  when(io.command.fire) {
    val warp = io.command.bits.warpId
    warpBusy(warp) := true.B
    activeMask(warp) := io.command.bits.activeMask
    warpIssueId(warp) := io.command.bits.issueId
    warpDestination(warp) := io.command.bits.destinationVd
    finishIssued(warp) := false.B
    for (lane <- 0 until num_thread) {
      when(io.command.bits.activeMask(lane)) {
        laneState(warp)(lane) := sPdsRead
        laneStatus(warp)(lane) := RtAbiLayout.TraversalCompleteMiss.U
        committedValid(warp)(lane) := false.B
      }.otherwise {
        laneState(warp)(lane) := sIdle
      }
    }
  }

  // A PDS read row is the input transaction of RaySetup.  The table releases
  // neither its data nor context at this point; retirement happens after write
  // drain/completion so the lane remains an owned traversal context.
  raySetup.io.in.valid := table.io.pdsReadLane.valid
  raySetup.io.in.bits.warpId := table.io.pdsReadLane.bits.warpId
  raySetup.io.in.bits.laneId := table.io.pdsReadLane.bits.laneId
  raySetup.io.in.bits.accelLo := table.io.pdsReadLane.bits.data(0)
  raySetup.io.in.bits.accelHi := table.io.pdsReadLane.bits.data(1)
  raySetup.io.in.bits.traceMeta0 := table.io.pdsReadLane.bits.data(2)
  raySetup.io.in.bits.traceMeta1 := table.io.pdsReadLane.bits.data(3)
  raySetup.io.in.bits.rayState.origin.x := table.io.pdsReadLane.bits.data(4)
  raySetup.io.in.bits.rayState.origin.y := table.io.pdsReadLane.bits.data(5)
  raySetup.io.in.bits.rayState.origin.z := table.io.pdsReadLane.bits.data(6)
  raySetup.io.in.bits.rayState.tmin := table.io.pdsReadLane.bits.data(7)
  raySetup.io.in.bits.rayState.dir.x := table.io.pdsReadLane.bits.data(8)
  raySetup.io.in.bits.rayState.dir.y := table.io.pdsReadLane.bits.data(9)
  raySetup.io.in.bits.rayState.dir.z := table.io.pdsReadLane.bits.data(10)
  raySetup.io.in.bits.rayState.tmax := table.io.pdsReadLane.bits.data(11)
  table.io.pdsReadLane.ready := raySetup.io.in.ready
  when(table.io.pdsReadLane.fire) {
    assert(laneState(table.io.pdsReadLane.bits.warpId)(table.io.pdsReadLane.bits.laneId) === sPdsRead)
  }

  table.io.pdsFault.ready := true.B
  when(table.io.pdsFault.fire) {
    val fault = table.io.pdsFault.bits
    laneState(fault.warpId)(fault.laneId) := sTerminal
    laneStatus(fault.warpId)(fault.laneId) := RtAbiLayout.TraversalTerminated.U
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

  // RaySetup's output and a stack pop share the AS acquire port.  Header
  // fetches are preferred so newly admitted rays do not wait behind traversal.
  val headerAcquire = raySetup.io.out.valid
  val readyMasks = Wire(Vec(num_warp, UInt(num_thread.W)))
  for (warp <- 0 until num_warp) {
    readyMasks(warp) := VecInit((0 until num_thread).map(lane =>
      laneState(warp)(lane) === sStackReady)).asUInt
  }
  val hasStackReady = readyMasks.map(_.orR).reduce(_ || _)
  val popWarp = roundRobin(VecInit(readyMasks.map(_.orR)).asUInt, nextStackWarp, num_warp)
  val popLane = roundRobin(readyMasks(popWarp), nextStackLane(popWarp), num_thread)
  stack.io.statusWarpId := popWarp
  val selectedEmpty = stack.io.laneEmpty(popLane)
  val selectedNodeRef = stack.io.popNodeRef(popLane)
  val selectedNodeType = nodeType(selectedNodeRef)
  val selectedUnsupported = selectedNodeType =/= RtVtasLayout.NodeBox4.U &&
    selectedNodeType =/= RtVtasLayout.NodeTriangle.U
  val selectedInvalid = selectedNodeRef === RtVtasLayout.InvalidNodeRef.U(32.W)
  val anyBoxPush = (0 until num_warp).map { warp =>
    (0 until num_thread).map(lane => laneState(warp)(lane) === sBoxPush).reduce(_ || _)
  }.reduce(_ || _)
  val headerLineActive = table.io.asReadyLine.valid &&
    table.io.asReadyLine.bits.waiter.nodeKind === RtVtasNodeKind.header
  val popNeedsAs = hasStackReady && !selectedEmpty && !selectedInvalid && !selectedUnsupported
  // Stack writes/initialisation are serialized ahead of a pop.  In
  // particular, never let an AS acquire consume a node unless the matching pop
  // can fire in the same cycle.
  val popMayFire = !headerAcquire && !anyBoxPush && !headerLineActive
  val nodeAcquire = popMayFire && popNeedsAs

  table.io.asAcquire.valid := headerAcquire || nodeAcquire
  table.io.asAcquire.bits := 0.U.asTypeOf(new RtAsTableAcquire)
  when(headerAcquire) {
    table.io.asAcquire.bits.lineAddress := raySetup.io.out.bits.accelLo
    table.io.asAcquire.bits.wordMask := Fill(RtMemTableParameters.lineWordCount, 1.U(1.W))
    table.io.asAcquire.bits.is64B := false.B
    table.io.asAcquire.bits.waiter.warpId := raySetup.io.out.bits.warpId
    table.io.asAcquire.bits.waiter.laneId := raySetup.io.out.bits.laneId
    table.io.asAcquire.bits.waiter.nodeKind := RtVtasNodeKind.header
    table.io.asAcquire.bits.waiter.nodeRef := 0.U
  }.elsewhen(nodeAcquire) {
    table.io.asAcquire.bits.lineAddress := laneAsBase(popWarp)(popLane) + nodeOffset(selectedNodeRef)
    table.io.asAcquire.bits.wordMask := Mux(selectedNodeType === RtVtasLayout.NodeTriangle.U,
      Cat(0.U(16.W), Fill(16, 1.U(1.W))), Fill(32, 1.U(1.W)))
    table.io.asAcquire.bits.is64B := selectedNodeType === RtVtasLayout.NodeTriangle.U
    table.io.asAcquire.bits.waiter.warpId := popWarp
    table.io.asAcquire.bits.waiter.laneId := popLane
    table.io.asAcquire.bits.waiter.nodeKind := Mux(selectedNodeType === RtVtasLayout.NodeBox4.U,
      RtVtasNodeKind.box4, RtVtasNodeKind.triangle)
    table.io.asAcquire.bits.waiter.nodeRef := selectedNodeRef
  }
  raySetup.io.out.ready := headerAcquire && table.io.asAcquire.ready
  when(raySetup.io.out.fire) {
    val result = raySetup.io.out.bits
    laneRay(result.warpId)(result.laneId) := result.ray
    laneAsBase(result.warpId)(result.laneId) := result.accelLo
    laneTmax(result.warpId)(result.laneId) := result.ray.extent.asUInt
    laneState(result.warpId)(result.laneId) := sHeaderPending
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
  stack.io.pop.bits.warpId := popWarp
  stack.io.pop.bits.laneMask := UIntToOH(popLane, num_thread)
  when(stack.io.pop.fire) {
    nextStackWarp := popWarp + 1.U
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
  val headerRoot = asReady.bits.data((RtVtasLayout.AsHeaderRootNodeRef / 4).toInt)
  val headerInvalid = headerRoot === RtVtasLayout.InvalidNodeRef.U(32.W)
  val asIsHeader = asReady.bits.waiter.nodeKind === RtVtasNodeKind.header
  val asIsBox = asReady.bits.waiter.nodeKind === RtVtasNodeKind.box4
  val asIsTriangle = asReady.bits.waiter.nodeKind === RtVtasNodeKind.triangle
  stack.io.init.valid := asReady.valid && asIsHeader && !headerInvalid
  stack.io.init.bits.warpId := asReady.bits.waiter.warpId
  stack.io.init.bits.laneMask := UIntToOH(asReady.bits.waiter.laneId, num_thread)
  for (lane <- 0 until num_thread) {
    stack.io.init.bits.rootNodeRef(lane) := headerRoot
  }
  for (unit <- 0 until boxUnitCount) {
    boxIssue(unit).valid := asReady.valid && asIsBox && boxReadyMask.orR && boxSelect === unit.U
    boxIssue(unit).bits := 0.U.asTypeOf(new RtBox4IntersectionInput)
    boxIssue(unit).bits.requestId := requestId(asReady.bits.waiter.warpId, asReady.bits.waiter.laneId)
    boxIssue(unit).bits.ray := laneRay(asReady.bits.waiter.warpId)(asReady.bits.waiter.laneId)
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
    triangleIssue(unit).valid := asReady.valid && asIsTriangle && triangleReadyMask.orR && triangleSelect === unit.U
    triangleIssue(unit).bits := 0.U.asTypeOf(new RtTriangleIntersectionInput)
    triangleIssue(unit).bits.requestId := requestId(asReady.bits.waiter.warpId, asReady.bits.waiter.laneId)
    triangleIssue(unit).bits.ray := laneRay(asReady.bits.waiter.warpId)(asReady.bits.waiter.laneId)
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
  val asBoxReady = asReady.valid && asIsBox && boxReadyMask.orR && boxIssue(boxSelect).ready
  val asTriangleReady = asReady.valid && asIsTriangle && triangleReadyMask.orR && triangleIssue(triangleSelect).ready
  table.io.asReadyLine.ready := asHeaderReady || asBoxReady || asTriangleReady ||
    (asReady.valid && !asIsHeader && !asIsBox && !asIsTriangle)
  when(asReady.fire) {
    val waiter = asReady.bits.waiter
    when(asIsHeader) {
      when(headerInvalid) {
        laneState(waiter.warpId)(waiter.laneId) := sTerminal
        laneStatus(waiter.warpId)(waiter.laneId) := RtAbiLayout.TraversalCompleteMiss.U
      }.otherwise {
        laneState(waiter.warpId)(waiter.laneId) := sStackReady
      }
    }.elsewhen(asIsBox) {
      nextBoxIssue := boxSelect + 1.U
      laneState(waiter.warpId)(waiter.laneId) := sBoxInflight
      for (child <- 0 until 4) {
        boxChildRefs(waiter.warpId)(waiter.laneId)(child) := asReady.bits.data(child)
      }
    }.elsewhen(asIsTriangle) {
      nextTriangleIssue := triangleSelect + 1.U
      laneState(waiter.warpId)(waiter.laneId) := sTriangleInflight
      triangleMeta(waiter.warpId)(waiter.laneId).opaque := asReady.bits.data((RtVtasLayout.TriangleFlags / 4).toInt)(0)
      triangleMeta(waiter.warpId)(waiter.laneId).primitiveId := asReady.bits.data((RtVtasLayout.TrianglePrimitiveId / 4).toInt)
      triangleMeta(waiter.warpId)(waiter.laneId).geometryId := asReady.bits.data((RtVtasLayout.TriangleGeometryId / 4).toInt)
      triangleMeta(waiter.warpId)(waiter.laneId).sbtRecordOffset := asReady.bits.data((RtVtasLayout.TriangleSbtRecordOffset / 4).toInt)
    }.otherwise {
      laneState(waiter.warpId)(waiter.laneId) := sTerminal
      laneStatus(waiter.warpId)(waiter.laneId) := RtAbiLayout.TraversalTerminated.U
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

  val pushMasks = Wire(Vec(num_warp, UInt(num_thread.W)))
  for (warp <- 0 until num_warp) {
    pushMasks(warp) := VecInit((0 until num_thread).map(lane =>
      laneState(warp)(lane) === sBoxPush)).asUInt
  }
  val hasPush = pushMasks.map(_.orR).reduce(_ || _)
  val pushWarp = roundRobin(VecInit(pushMasks.map(_.orR)).asUInt, nextPushWarp, num_warp)
  val pushLane = roundRobin(pushMasks(pushWarp), nextPushLane(pushWarp), num_thread)
  val pushIndex = boxDeferredCount(pushWarp)(pushLane) - boxPushedCount(pushWarp)(pushLane) - 1.U
  stack.io.push.valid := hasPush
  stack.io.push.bits.warpId := pushWarp
  stack.io.push.bits.laneMask := UIntToOH(pushLane, num_thread)
  for (lane <- 0 until num_thread) {
    stack.io.push.bits.nodeRef(lane) := boxDeferredRefs(pushWarp)(pushLane)(pushIndex(1, 0))
  }
  when(stack.io.push.fire) {
    nextPushWarp := pushWarp + 1.U
    nextPushLane(pushWarp) := pushLane + 1.U
    assert(!stack.io.pushWouldOverflow, "RtTraversalStack overflow requires spill support")
    when(boxPushedCount(pushWarp)(pushLane) + 1.U === boxDeferredCount(pushWarp)(pushLane)) {
      laneState(pushWarp)(pushLane) := sStackReady
    }.otherwise {
      boxPushedCount(pushWarp)(pushLane) := boxPushedCount(pushWarp)(pushLane) + 1.U
    }
  }

  // One triangle result at a time enters the shared final-division engine.
  val triangleResultMask = VecInit(triangleResult.map(_.valid)).asUInt
  val triangleResultSelect = roundRobin(triangleResultMask, nextTriangleResult, triangleUnitCount)
  triangleResolve.io.in.valid := triangleResultMask.orR
  triangleResolve.io.in.bits := Mux1H((0 until triangleUnitCount).map(unit =>
    (triangleResultSelect === unit.U) -> triangleResult(unit).bits))
  for (unit <- 0 until triangleUnitCount) {
    triangleResult(unit).ready := triangleResultMask.orR && triangleResultSelect === unit.U && triangleResolve.io.in.ready
  }
  when(triangleResultMask.orR && triangleResult(triangleResultSelect).fire) {
    nextTriangleResult := triangleResultSelect + 1.U
  }

  val resultWarp = requestWarp(triangleResolve.io.out.bits.requestId)
  val resultLane = requestLane(triangleResolve.io.out.bits.requestId)
  val compareT = Module(new CompareRecFN(8, 24))
  compareT.io.a := recFNFromFN(8, 24, triangleResolve.io.out.bits.t)
  compareT.io.b := recFNFromFN(8, 24, laneTmax(resultWarp)(resultLane))
  compareT.io.signaling := false.B
  val acceptedTriangle = triangleResolve.io.out.bits.hit && compareT.io.lt
  val triangleIsOpaque = triangleMeta(resultWarp)(resultLane).opaque
  val writesHit = acceptedTriangle
  table.io.pdsHitUpdate.valid := triangleResolve.io.out.valid && writesHit
  table.io.pdsHitUpdate.bits := 0.U.asTypeOf(new RtPdsHitRecordUpdate)
  table.io.pdsHitUpdate.bits.warpId := resultWarp
  table.io.pdsHitUpdate.bits.laneId := resultLane
  table.io.pdsHitUpdate.bits.committed := triangleIsOpaque
  table.io.pdsHitUpdate.bits.dirty := Fill(RtMemTableParameters.hitRecordStorageWordCount, 1.U(1.W))
  table.io.pdsHitUpdate.bits.data(RtAbiLayout.HitRecordMeta0) :=
    (1 | (RtAbiLayout.HitRecordStatusValid << RtAbiLayout.HitRecordMeta0StatusShift)).U |
      (triangleIsOpaque.asUInt << RtAbiLayout.HitRecordMeta0OpaqueShift)
  table.io.pdsHitUpdate.bits.data(RtAbiLayout.HitRecordMeta1) := triangleMeta(resultWarp)(resultLane).sbtRecordOffset
  table.io.pdsHitUpdate.bits.data(RtAbiLayout.HitRecordHitT) := triangleResolve.io.out.bits.t
  table.io.pdsHitUpdate.bits.data(RtAbiLayout.HitRecordBarycentricsX) := triangleResolve.io.out.bits.u
  table.io.pdsHitUpdate.bits.data(RtAbiLayout.HitRecordBarycentricsY) := triangleResolve.io.out.bits.v
  table.io.pdsHitUpdate.bits.data(RtAbiLayout.HitRecordPrimitiveId) := triangleMeta(resultWarp)(resultLane).primitiveId
  table.io.pdsHitUpdate.bits.data(RtAbiLayout.HitRecordInstanceId) := 0.U
  table.io.pdsHitUpdate.bits.data(RtAbiLayout.HitRecordGeometryId) := triangleMeta(resultWarp)(resultLane).geometryId
  table.io.pdsHitUpdate.bits.data(RtAbiLayout.HitRecordHitKind) := "hfe".U
  triangleResolve.io.out.ready := !writesHit || table.io.pdsHitUpdate.ready
  when(triangleResolve.io.out.fire) {
    when(acceptedTriangle && triangleIsOpaque) {
      committedValid(resultWarp)(resultLane) := true.B
      laneTmax(resultWarp)(resultLane) := triangleResolve.io.out.bits.t
      laneState(resultWarp)(resultLane) := sStackReady
    }.elsewhen(acceptedTriangle) {
      laneState(resultWarp)(resultLane) := sTerminal
      laneStatus(resultWarp)(resultLane) := RtAbiLayout.TraversalCandidateNonOpaqueTriangle.U
    }.otherwise {
      laneState(resultWarp)(resultLane) := sStackReady
    }
  }

  // A completed warp first flushes both hit-record planes.  The write table's
  // drain acknowledgement is the point at which completion may be published.
  val terminalMasks = Wire(Vec(num_warp, UInt(num_thread.W)))
  for (warp <- 0 until num_warp) {
    terminalMasks(warp) := VecInit((0 until num_thread).map(lane =>
      laneState(warp)(lane) === sTerminal)).asUInt
  }
  val finishMasks = VecInit((0 until num_warp).map { warp =>
    warpBusy(warp) && !finishIssued(warp) &&
      (terminalMasks(warp) & activeMask(warp)) === activeMask(warp)
  }).asUInt
  val finishWarp = PriorityEncoder(finishMasks)
  table.io.pdsWriteFinish.valid := finishMasks.orR
  table.io.pdsWriteFinish.bits.warpId := finishWarp
  table.io.pdsWriteFinish.bits.terminalMask := activeMask(finishWarp)
  when(table.io.pdsWriteFinish.fire) { finishIssued(finishWarp) := true.B }

  val completionPending = RegInit(false.B)
  val completionBits = Reg(new RtWarpCompletion)
  val retirePending = RegInit(false.B)
  val retireWarp = Reg(UInt(depth_warp.W))
  table.io.pdsWriteDrained.ready := !completionPending && !retirePending
  when(table.io.pdsWriteDrained.fire) {
    val warp = table.io.pdsWriteDrained.bits.warpId
    completionBits.warpId := warp
    completionBits.issueId := warpIssueId(warp)
    completionBits.destinationVd := warpDestination(warp)
    completionBits.completionMask := activeMask(warp)
    completionBits.laneStatus := laneStatus(warp)
    completionPending := true.B
  }
  io.completion.valid := completionPending
  io.completion.bits := completionBits
  when(io.completion.fire) {
    retirePending := true.B
    retireWarp := completionBits.warpId
    completionPending := false.B
  }
  table.io.pdsReadRetire.valid := retirePending && stack.io.clear.ready
  table.io.pdsReadRetire.bits.warpId := retireWarp
  stack.io.clear.valid := retirePending && table.io.pdsReadRetire.ready
  stack.io.clear.bits.warpId := retireWarp
  stack.io.clear.bits.laneMask := activeMask(retireWarp)
  when(table.io.pdsReadRetire.fire && stack.io.clear.fire) {
    warpBusy(retireWarp) := false.B
    finishIssued(retireWarp) := false.B
    retirePending := false.B
    for (lane <- 0 until num_thread) { laneState(retireWarp)(lane) := sIdle }
  }
}

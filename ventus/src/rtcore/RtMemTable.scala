package rtcore

import chisel3._
import chisel3.util._
import lsu.{LsuClientRequest, LsuClientResponse}
import top.parameters._

object RtMemTableParameters {
  val pdsRayStateFieldCount = 12
  // Keep raw rows indexed relative to SlotAccelLo. Traversal needs accelLo
  // plus origin, tmin, direction, and tmax: nine cache lines in total.
  // accelHi and trace metadata remain unfetched until a later traversal mode
  // needs them.
  val pdsTraversalReadFields = Seq(
    RtAbiLayout.SlotAccelLo,
    RtAbiLayout.SlotOriginX,
    RtAbiLayout.SlotOriginY,
    RtAbiLayout.SlotOriginZ,
    RtAbiLayout.SlotTmin,
    RtAbiLayout.SlotDirectionX,
    RtAbiLayout.SlotDirectionY,
    RtAbiLayout.SlotDirectionZ,
    RtAbiLayout.SlotTmax
  ).map(_ - RtAbiLayout.SlotAccelLo)
  val pdsTraversalReadMask = pdsTraversalReadFields.foldLeft(BigInt(0)) {
    case (mask, field) => mask | (BigInt(1) << field)
  }
  val pdsWriteWarpEntryCount = RtCorePortParameters.residentWarpCount
  val hitRecordStorageWordCount = RtAbiLayout.HitRecordWordCount
  val asEntryCount = 32
  val maxAsWaitersPerEntry = 2
  val lineWordCount = RtCorePortParameters.lineWordCount
  require(lineWordCount == num_thread)
  require(RtAbiLayout.AbiCandidateHitRecordSizeBytes / RtAbiLayout.AbiWordBytes == hitRecordStorageWordCount)
  require(RtAbiLayout.AbiCommittedHitRecordSizeBytes / RtAbiLayout.AbiWordBytes == hitRecordStorageWordCount)
  require(RtAbiLayout.SlotAccelLo + pdsRayStateFieldCount <= RtAbiLayout.SlotWordCount)
  require(RtAbiLayout.AbiCandidateHitRecordBaseBytes / RtAbiLayout.AbiWordBytes + hitRecordStorageWordCount == RtAbiLayout.AbiCommittedHitRecordBaseBytes / RtAbiLayout.AbiWordBytes)
  require(RtAbiLayout.AbiCommittedHitRecordBaseBytes / RtAbiLayout.AbiWordBytes + hitRecordStorageWordCount == RtAbiLayout.AbiPdsFieldMajorFixedHeaderWordCount)
}

class RtPdsReadAdmission extends Bundle { val warpId = UInt(RtCorePortParameters.residentWarpIdWidth.W); val pds = new RtPdsContext; val activeMask = UInt(num_thread.W) }
class RtPdsRayStateFault extends Bundle { val warpId = UInt(RtCorePortParameters.residentWarpIdWidth.W); val laneId = UInt(log2Ceil(num_thread).W) }
class RtPdsReadLane extends Bundle { val warpId = UInt(RtCorePortParameters.residentWarpIdWidth.W); val laneId = UInt(log2Ceil(num_thread).W); val data = Vec(RtMemTableParameters.pdsRayStateFieldCount, UInt(xLen.W)) }
class RtPdsReadRetire extends Bundle { val warpId = UInt(RtCorePortParameters.residentWarpIdWidth.W) }
class RtPdsRayTmaxUpdate extends Bundle { val warpId = UInt(RtCorePortParameters.residentWarpIdWidth.W); val laneId = UInt(log2Ceil(num_thread).W); val tmax = UInt(32.W) }
class RtPdsRayLookup extends Bundle { val warpId = UInt(RtCorePortParameters.residentWarpIdWidth.W); val laneId = UInt(log2Ceil(num_thread).W) }
class RtPdsPreparedRay extends Bundle {
  val valid = Bool()
  val asBase = UInt(memReqLen.W)
  val currentTmax = UInt(32.W)
  val originalTmin = UInt(32.W)
  val inv = new Float3(recorded_float = false)
  val shear = new Float3(recorded_float = false)
  val kx = UInt(2.W)
  val ky = UInt(2.W)
  val kz = UInt(2.W)
}
class RtPdsRayLookupResult extends Bundle {
  val valid = Bool()
  val asBase = UInt(memReqLen.W)
  val currentTmax = UInt(32.W)
  val ray = new Ray(recorded_float = false)
}
private[rtcore] class RtPdsReadContext extends Bundle {
  val valid = Bool(); val pds = new RtPdsContext; val activeMask = UInt(num_thread.W)
  val issued = UInt(RtMemTableParameters.pdsRayStateFieldCount.W)
  val received = UInt(RtMemTableParameters.pdsRayStateFieldCount.W)
  val delivered = UInt(num_thread.W); val faultMask = UInt(num_thread.W)
}

class RtPdsReadTable extends Module {
  private val p = RtMemTableParameters
  private val traversalFields = p.pdsTraversalReadMask.U(p.pdsRayStateFieldCount.W)
  val io = IO(new Bundle {
    val admission = Flipped(Decoupled(new RtPdsReadAdmission)); val laneOut = Decoupled(new RtPdsReadLane)
    val faultOut = Decoupled(new RtPdsRayStateFault); val retire = Flipped(Decoupled(new RtPdsReadRetire))
    val raySetup = Flipped(Decoupled(new RtRaySetupOutput)); val rayTmaxUpdate = Flipped(Decoupled(new RtPdsRayTmaxUpdate))
    val nodeRayLookup = Input(new RtPdsRayLookup); val nodeRay = Output(new RtPdsRayLookupResult)
    val issueRayLookup = Input(new RtPdsRayLookup); val issueRay = Output(new RtPdsRayLookupResult)
    val resultRayLookup = Input(new RtPdsRayLookup); val resultRay = Output(new RtPdsRayLookupResult)
    val candidate = Decoupled(new RtLsuCandidate); val injection = Flipped(Decoupled(new RtPdsReadInjection))
  })
  val context = RegInit(0.U.asTypeOf(new RtPdsReadContext))
  val rows = Reg(Vec(num_thread, Vec(p.pdsRayStateFieldCount, UInt(xLen.W))))
  val prepared = RegInit(VecInit(Seq.fill(num_thread)(0.U.asTypeOf(new RtPdsPreparedRay))))
  val admit = context
  io.admission.ready := io.admission.bits.activeMask.orR && !admit.valid
  when(io.admission.fire) {
    admit.valid := true.B; admit.pds := io.admission.bits.pds; admit.activeMask := io.admission.bits.activeMask; admit.issued := 0.U; admit.received := 0.U; admit.delivered := 0.U; admit.faultMask := 0.U
    for (lane <- 0 until num_thread) { prepared(lane).valid := false.B }
  }
  val hasNeeded = context.valid && (context.issued & traversalFields) =/= traversalFields
  val warp = 0.U(RtCorePortParameters.residentWarpIdWidth.W)
  val field = PriorityEncoder((~context.issued).asUInt & traversalFields)
  val wordsPerField = context.pds.numWarps * context.pds.numThreads
  val physicalWord = (RtAbiLayout.SlotAccelLo.U + field) * wordsPerField + context.pds.pdsWarpTidBase
  io.candidate.valid := hasNeeded
  io.candidate.bits := 0.U.asTypeOf(new RtLsuCandidate)
  when(hasNeeded) {
    io.candidate.bits.source := RtLsuSource.pdsRead; io.candidate.bits.cacheLineAddress := context.pds.baseAddress + (physicalWord << 2)
    io.candidate.bits.wordMask := Fill(p.lineWordCount, 1.U(1.W)); io.candidate.bits.locator.tableLocation := field
    assert(context.pds.numThreads === num_thread.U,
      "RT PDS field reads require one 32-word cacheline per warp")
    assert(context.pds.pdsWarpTidBase(log2Ceil(num_thread) - 1, 0) === 0.U,
      "RT PDS warp TID base must start at a warp boundary")
    assert(io.candidate.bits.cacheLineAddress(6, 0) === 0.U,
      "RT PDS field reads must start on a 128-byte cacheline boundary")
  }
  when(io.candidate.fire) { context.issued := context.issued | UIntToOH(field, p.pdsRayStateFieldCount) }
  val returned = context
  io.injection.ready := returned.valid && returned.issued(io.injection.bits.field) && !returned.received(io.injection.bits.field)
  when(io.injection.fire) {
    returned.received := returned.received | UIntToOH(io.injection.bits.field, p.pdsRayStateFieldCount)
    when(io.injection.bits.fault) { returned.faultMask := returned.faultMask | returned.activeMask }
    for (lane <- 0 until num_thread) when(returned.activeMask(lane)) { rows(lane)(io.injection.bits.field) := io.injection.bits.data(lane) }
  }
  val completed = context.valid && (context.received & traversalFields) === traversalFields
  val left = context.activeMask & (~context.delivered).asUInt
  val laneAvailable = Mux(completed, left & (~context.faultMask).asUInt, 0.U)
  val faultAvailable = Mux(completed, left & context.faultMask, 0.U)
  val hasFault = faultAvailable.orR; val hasLane = laneAvailable.orR
  val laneWarp = warp; val faultWarp = warp
  val lane = PriorityEncoder(laneAvailable); val faultLane = PriorityEncoder(faultAvailable)
  io.laneOut.valid := hasLane && !hasFault; io.laneOut.bits := 0.U.asTypeOf(new RtPdsReadLane)
  when(io.laneOut.valid) { io.laneOut.bits.warpId := laneWarp; io.laneOut.bits.laneId := lane; io.laneOut.bits.data := rows(lane) }
  io.faultOut.valid := hasFault; io.faultOut.bits.warpId := faultWarp; io.faultOut.bits.laneId := faultLane
  when(io.laneOut.fire) { context.delivered := context.delivered | UIntToOH(lane, num_thread) }
  when(io.faultOut.fire) { context.delivered := context.delivered | UIntToOH(faultLane, num_thread) }
  val setupContext = context
  val setupPrepared = prepared(io.raySetup.bits.laneId)
  io.raySetup.ready := setupContext.valid && setupContext.activeMask(io.raySetup.bits.laneId) &&
    setupContext.delivered(io.raySetup.bits.laneId) && !setupPrepared.valid
  when(io.raySetup.fire) {
    setupPrepared.valid := true.B
    setupPrepared.asBase := io.raySetup.bits.accelLo
    setupPrepared.currentTmax := io.raySetup.bits.ray.extent
    setupPrepared.originalTmin := io.raySetup.bits.ray.originalTmin
    setupPrepared.inv := io.raySetup.bits.ray.inv
    setupPrepared.shear := io.raySetup.bits.ray.shear
    setupPrepared.kx := io.raySetup.bits.ray.kx
    setupPrepared.ky := io.raySetup.bits.ray.ky
    setupPrepared.kz := io.raySetup.bits.ray.kz
  }
  val tmaxContext = context
  val tmaxPrepared = prepared(io.rayTmaxUpdate.bits.laneId)
  io.rayTmaxUpdate.ready := tmaxContext.valid && tmaxContext.activeMask(io.rayTmaxUpdate.bits.laneId) && tmaxPrepared.valid
  when(io.rayTmaxUpdate.fire) { tmaxPrepared.currentTmax := io.rayTmaxUpdate.bits.tmax }
  def driveRayLookup(lookup: RtPdsRayLookup, output: RtPdsRayLookupResult): Unit = {
    val entry = prepared(lookup.laneId)
    output.valid := entry.valid
    output.asBase := entry.asBase
    output.currentTmax := entry.currentTmax
    output.ray.origin.x := rows(lookup.laneId)(4)
    output.ray.origin.y := rows(lookup.laneId)(5)
    output.ray.origin.z := rows(lookup.laneId)(6)
    output.ray.dir.x := rows(lookup.laneId)(8)
    output.ray.dir.y := rows(lookup.laneId)(9)
    output.ray.dir.z := rows(lookup.laneId)(10)
    output.ray.inv := entry.inv
    output.ray.originalTmin := entry.originalTmin
    output.ray.extent := entry.currentTmax
    output.ray.kx := entry.kx
    output.ray.ky := entry.ky
    output.ray.kz := entry.kz
    output.ray.shear := entry.shear
  }
  driveRayLookup(io.nodeRayLookup, io.nodeRay)
  driveRayLookup(io.issueRayLookup, io.issueRay)
  driveRayLookup(io.resultRayLookup, io.resultRay)
  val retire = context; io.retire.ready := retire.valid && (retire.delivered & retire.activeMask) === retire.activeMask
  when(io.retire.fire) { retire.valid := false.B }
}

class RtPdsHitRecordUpdate extends Bundle { val warpId = UInt(RtCorePortParameters.residentWarpIdWidth.W); val laneId = UInt(log2Ceil(num_thread).W); val committed = Bool(); val dirty = UInt(RtMemTableParameters.hitRecordStorageWordCount.W); val data = Vec(RtMemTableParameters.hitRecordStorageWordCount, UInt(xLen.W)) }
class RtPdsWriteAdmission extends Bundle { val warpId = UInt(RtCorePortParameters.residentWarpIdWidth.W); val pds = new RtPdsContext; val activeMask = UInt(num_thread.W) }
class RtPdsWriteFinish extends Bundle { val warpId = UInt(RtCorePortParameters.residentWarpIdWidth.W); val terminalMask = UInt(num_thread.W) }
class RtPdsWriteDrained extends Bundle { val warpId = UInt(RtCorePortParameters.residentWarpIdWidth.W) }
private[rtcore] class RtPdsWriteContext extends Bundle { val valid = Bool(); val pds = new RtPdsContext; val activeMask = UInt(num_thread.W); val flushing = Bool(); val drained = Bool(); val waiting = Bool(); val record = Bool(); val word = UInt(log2Ceil(RtMemTableParameters.hitRecordStorageWordCount).W); val upperHalf = Bool() }

class RtPdsWriteTable(warpEntryCount: Int = RtMemTableParameters.pdsWriteWarpEntryCount) extends Module {
  require(warpEntryCount == 1,
    "RtPdsWriteTable currently owns one physical RTcore warp")
  private val p = RtMemTableParameters; private val words = p.hitRecordStorageWordCount; private val half = RtCorePortParameters.pdsWriteWordCount; private val indexWidth = log2Ceil(warpEntryCount)
  private def index(warp: UInt): UInt = if (warpEntryCount == 1) 0.U else warp(indexWidth - 1, 0)
  val io = IO(new Bundle { val admission = Flipped(Decoupled(new RtPdsWriteAdmission)); val update = Flipped(Decoupled(new RtPdsHitRecordUpdate)); val finish = Flipped(Decoupled(new RtPdsWriteFinish)); val candidate = Decoupled(new RtLsuCandidate); val injection = Flipped(Decoupled(new RtPdsWriteInjection)); val drained = Decoupled(new RtPdsWriteDrained) })
  val contexts = RegInit(VecInit(Seq.fill(warpEntryCount)(0.U.asTypeOf(new RtPdsWriteContext))))
  val data = Reg(Vec(warpEntryCount, Vec(num_thread, Vec(2, Vec(words, UInt(xLen.W))))))
  val dirty = RegInit(VecInit(Seq.fill(warpEntryCount)(VecInit(Seq.fill(num_thread)(VecInit(Seq.fill(2)(VecInit(Seq.fill(words)(false.B)))))))))
  val admit = contexts(index(io.admission.bits.warpId)); io.admission.ready := io.admission.bits.activeMask.orR && !admit.valid
  when(io.admission.fire) { admit.valid := true.B; admit.pds := io.admission.bits.pds; admit.activeMask := io.admission.bits.activeMask; admit.flushing := false.B; admit.drained := false.B; admit.waiting := false.B; admit.record := false.B; admit.word := 0.U; admit.upperHalf := false.B; for (l <- 0 until num_thread; r <- 0 until 2; w <- 0 until words) { dirty(index(io.admission.bits.warpId))(l)(r)(w) := false.B } }
  val update = contexts(index(io.update.bits.warpId)); io.update.ready := update.valid && !update.flushing && update.activeMask(io.update.bits.laneId)
  when(io.update.fire) { for (w <- 0 until words) when(io.update.bits.dirty(w)) { data(index(io.update.bits.warpId))(io.update.bits.laneId)(io.update.bits.committed)(w) := io.update.bits.data(w); dirty(index(io.update.bits.warpId))(io.update.bits.laneId)(io.update.bits.committed)(w) := true.B } }
  val finish = contexts(index(io.finish.bits.warpId)); io.finish.ready := finish.valid && !finish.flushing && io.finish.bits.terminalMask === finish.activeMask
  when(io.finish.fire) { finish.flushing := true.B; finish.record := false.B; finish.word := 0.U; finish.upperHalf := false.B }
  def advance(c: RtPdsWriteContext): Unit = when(!c.upperHalf) { c.upperHalf := true.B }.elsewhen(c.word === (words - 1).U) { c.upperHalf := false.B; c.word := 0.U; when(!c.record) { c.record := true.B }.otherwise { c.drained := true.B } }.otherwise { c.upperHalf := false.B; c.word := c.word + 1.U }
  val eligible = VecInit(contexts.map(c => c.valid && c.flushing && !c.drained && !c.waiting)).asUInt; val hasEligible = eligible.orR; val warp = PriorityEncoder(eligible); val context = contexts(warp); val halfBase = Mux(context.upperHalf, half.U, 0.U)
  val dirtyHalf = Wire(Vec(half, Bool())); for (w <- 0 until half) { val lane = halfBase + w.U; dirtyHalf(w) := context.activeMask(lane) && dirty(warp)(lane)(context.record)(context.word) }
  val hasDirty = dirtyHalf.asUInt.orR; when(hasEligible && !hasDirty) { advance(context) }
  io.candidate.valid := hasEligible && hasDirty; io.candidate.bits := 0.U.asTypeOf(new RtLsuCandidate)
  when(io.candidate.valid) {
    val fieldBase = Mux(context.record, (RtAbiLayout.AbiCommittedHitRecordBaseBytes / RtAbiLayout.AbiWordBytes).U, (RtAbiLayout.AbiCandidateHitRecordBaseBytes / RtAbiLayout.AbiWordBytes).U)
    val physicalWord = (fieldBase + context.word) * (context.pds.numWarps * context.pds.numThreads) + context.pds.pdsWarpTidBase + halfBase
    io.candidate.bits.source := RtLsuSource.pdsWrite; io.candidate.bits.cacheLineAddress := context.pds.baseAddress + (physicalWord << 2); io.candidate.bits.write := true.B; io.candidate.bits.is64B := true.B; io.candidate.bits.wordMask := Cat(0.U(half.W), dirtyHalf.asUInt)
    for (w <- 0 until half) { io.candidate.bits.writeData(w) := data(warp)(halfBase + w.U)(context.record)(context.word) }
    io.candidate.bits.locator.tableLocation := Cat(context.record, context.word, context.upperHalf)
  }
  when(io.candidate.fire) { assert(io.candidate.bits.cacheLineAddress(5, 0) === 0.U); context.waiting := true.B }
  val returned = contexts(0); io.injection.ready := returned.valid && returned.flushing && returned.waiting && returned.record === io.injection.bits.record && returned.word === io.injection.bits.word && returned.upperHalf === io.injection.bits.upperHalf
  when(io.injection.fire) { assert(!io.injection.bits.fault); returned.waiting := false.B; advance(returned) }
  val drained = VecInit(contexts.map(c => c.valid && c.drained)).asUInt; io.drained.valid := drained.orR; io.drained.bits.warpId := PriorityEncoder(drained)
  when(io.drained.fire) { contexts(PriorityEncoder(drained)).valid := false.B }
}

class RtAsLineWaiter extends Bundle { val warpId = UInt(RtCorePortParameters.residentWarpIdWidth.W); val laneId = UInt(log2Ceil(num_thread).W); val nodeKind = UInt(2.W) }
class RtAsTableAcquire extends Bundle { val lineAddress = UInt(memReqLen.W); val wordMask = UInt(RtMemTableParameters.lineWordCount.W); val is64B = Bool(); val waiter = new RtAsLineWaiter }
class RtAsTableReadyLine extends Bundle { val waiter = new RtAsLineWaiter; val data = Vec(RtMemTableParameters.lineWordCount, UInt(xLen.W)) }
private[rtcore] class RtAsEntry extends Bundle { val valid = Bool(); val sent = Bool(); val returned = Bool(); val lineAddress = UInt(memReqLen.W); val wordMask = UInt(RtMemTableParameters.lineWordCount.W); val is64B = Bool(); val waiterValid = Vec(RtMemTableParameters.maxAsWaitersPerEntry, Bool()); val waiters = Vec(RtMemTableParameters.maxAsWaitersPerEntry, new RtAsLineWaiter); val data = Vec(RtMemTableParameters.lineWordCount, UInt(xLen.W)) }

class RtAsTable extends Module {
  private val p = RtMemTableParameters
  val io = IO(new Bundle { val acquire = Flipped(Decoupled(new RtAsTableAcquire)); val readyLine = Decoupled(new RtAsTableReadyLine); val candidate = Decoupled(new RtLsuCandidate); val injection = Flipped(Decoupled(new RtAsInjection)) })
  val entries = RegInit(VecInit(Seq.fill(p.asEntryCount)(0.U.asTypeOf(new RtAsEntry))))
  val merge = VecInit(entries.map(e => e.valid && !e.sent && e.lineAddress === io.acquire.bits.lineAddress && e.is64B === io.acquire.bits.is64B && !e.waiterValid.asUInt.andR)).asUInt; val free = VecInit(entries.map(!_.valid)).asUInt
  val hasMerge = merge.orR; val place = Mux(hasMerge, PriorityEncoder(merge), PriorityEncoder(free)); io.acquire.ready := hasMerge || free.orR
  when(io.acquire.fire) {
    assert(Mux(io.acquire.bits.is64B, io.acquire.bits.lineAddress(5, 0) === 0.U, io.acquire.bits.lineAddress(6, 0) === 0.U))
    val e = entries(place); when(!hasMerge) { e.valid := true.B; e.sent := false.B; e.returned := false.B; e.lineAddress := io.acquire.bits.lineAddress; e.wordMask := io.acquire.bits.wordMask; e.is64B := io.acquire.bits.is64B; e.waiterValid.foreach(_ := false.B) }.otherwise { e.wordMask := e.wordMask | io.acquire.bits.wordMask }; val w = PriorityEncoder((~e.waiterValid.asUInt).asUInt); e.waiterValid(w) := true.B; e.waiters(w) := io.acquire.bits.waiter
  }
  val unsent = VecInit(entries.map(e => e.valid && !e.sent)).asUInt; val request = PriorityEncoder(unsent); io.candidate.valid := unsent.orR; io.candidate.bits := 0.U.asTypeOf(new RtLsuCandidate)
  when(io.candidate.valid) { io.candidate.bits.source := RtLsuSource.as; io.candidate.bits.cacheLineAddress := entries(request).lineAddress; io.candidate.bits.is64B := entries(request).is64B; io.candidate.bits.wordMask := entries(request).wordMask; io.candidate.bits.locator.tableLocation := request }
  when(io.candidate.fire) { entries(request).sent := true.B }
  val response = entries(io.injection.bits.entry); io.injection.ready := response.valid && response.sent && !response.returned
  when(io.injection.fire) { assert(!io.injection.bits.fault); response.returned := true.B; response.data := io.injection.bits.data }
  val ready = VecInit(entries.map(e => e.valid && e.returned && e.waiterValid.asUInt.orR)).asUInt; val entry = PriorityEncoder(ready); val waiter = PriorityEncoder(entries(entry).waiterValid.asUInt)
  io.readyLine.valid := ready.orR; io.readyLine.bits := 0.U.asTypeOf(new RtAsTableReadyLine)
  when(io.readyLine.valid) { io.readyLine.bits.waiter := entries(entry).waiters(waiter); io.readyLine.bits.data := entries(entry).data }
  when(io.readyLine.fire) { val left = entries(entry).waiterValid.asUInt & (~UIntToOH(waiter, p.maxAsWaitersPerEntry)).asUInt; entries(entry).waiterValid(waiter) := false.B; when(left === 0.U) { entries(entry).valid := false.B } }
}

class RtMemTable extends Module {
  val io = IO(new Bundle {
    val pdsReadAdmission = Flipped(Decoupled(new RtPdsReadAdmission)); val pdsReadLane = Decoupled(new RtPdsReadLane); val pdsFault = Decoupled(new RtPdsRayStateFault); val pdsReadRetire = Flipped(Decoupled(new RtPdsReadRetire))
    val pdsRaySetup = Flipped(Decoupled(new RtRaySetupOutput)); val pdsRayTmaxUpdate = Flipped(Decoupled(new RtPdsRayTmaxUpdate))
    val nodeRayLookup = Input(new RtPdsRayLookup); val nodeRay = Output(new RtPdsRayLookupResult)
    val issueRayLookup = Input(new RtPdsRayLookup); val issueRay = Output(new RtPdsRayLookupResult)
    val resultRayLookup = Input(new RtPdsRayLookup); val resultRay = Output(new RtPdsRayLookupResult)
    val pdsWriteAdmission = Flipped(Decoupled(new RtPdsWriteAdmission)); val pdsHitUpdate = Flipped(Decoupled(new RtPdsHitRecordUpdate)); val pdsWriteFinish = Flipped(Decoupled(new RtPdsWriteFinish)); val pdsWriteDrained = Decoupled(new RtPdsWriteDrained)
    val asAcquire = Flipped(Decoupled(new RtAsTableAcquire)); val asReadyLine = Decoupled(new RtAsTableReadyLine)
    val lsuRequest = Decoupled(new LsuClientRequest); val lsuResponse = Flipped(Decoupled(new LsuClientResponse))
  })
  val read = Module(new RtPdsReadTable); val write = Module(new RtPdsWriteTable); val as = Module(new RtAsTable); val mshr = Module(new RtLsuMshr)
  read.io.admission <> io.pdsReadAdmission; io.pdsReadLane <> read.io.laneOut; io.pdsFault <> read.io.faultOut; read.io.retire <> io.pdsReadRetire
  read.io.raySetup <> io.pdsRaySetup; read.io.rayTmaxUpdate <> io.pdsRayTmaxUpdate
  read.io.nodeRayLookup := io.nodeRayLookup; io.nodeRay := read.io.nodeRay
  read.io.issueRayLookup := io.issueRayLookup; io.issueRay := read.io.issueRay
  read.io.resultRayLookup := io.resultRayLookup; io.resultRay := read.io.resultRay
  write.io.admission <> io.pdsWriteAdmission; write.io.update <> io.pdsHitUpdate; write.io.finish <> io.pdsWriteFinish; io.pdsWriteDrained <> write.io.drained
  as.io.acquire <> io.asAcquire; io.asReadyLine <> as.io.readyLine
  mshr.io.pdsRead <> read.io.candidate; mshr.io.pdsWrite <> write.io.candidate; mshr.io.as <> as.io.candidate
  read.io.injection <> mshr.io.pdsReadInjection; write.io.injection <> mshr.io.pdsWriteInjection; as.io.injection <> mshr.io.asInjection
  io.lsuRequest <> mshr.io.request; mshr.io.response <> io.lsuResponse
}

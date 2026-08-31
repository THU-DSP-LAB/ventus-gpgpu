package rtcore

import chisel3._
import chisel3.util._
import top.parameters._

object RtMemTableParameters {
  val pdsRayStateFieldCount = 12
  val pdsReadWarpEntryCount = num_warp
  val pdsWriteWarpEntryCount = num_warp
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

class RtPdsReadAdmission extends Bundle { val warpId = UInt(depth_warp.W); val pds = new RtPdsContext; val activeMask = UInt(num_thread.W) }
class RtPdsRayStateFault extends Bundle { val warpId = UInt(depth_warp.W); val laneId = UInt(log2Ceil(num_thread).W) }
class RtPdsReadLane extends Bundle { val warpId = UInt(depth_warp.W); val laneId = UInt(log2Ceil(num_thread).W); val data = Vec(RtMemTableParameters.pdsRayStateFieldCount, UInt(xLen.W)) }
class RtPdsReadRetire extends Bundle { val warpId = UInt(depth_warp.W) }
private[rtcore] class RtPdsReadContext extends Bundle {
  val valid = Bool(); val pds = new RtPdsContext; val activeMask = UInt(num_thread.W)
  val issued = UInt(RtMemTableParameters.pdsRayStateFieldCount.W)
  val received = UInt(RtMemTableParameters.pdsRayStateFieldCount.W)
  val delivered = UInt(num_thread.W); val faultMask = UInt(num_thread.W)
}

class RtPdsReadTable extends Module {
  private val p = RtMemTableParameters
  private val allFields = ((BigInt(1) << p.pdsRayStateFieldCount) - 1).U(p.pdsRayStateFieldCount.W)
  val io = IO(new Bundle {
    val admission = Flipped(Decoupled(new RtPdsReadAdmission)); val laneOut = Decoupled(new RtPdsReadLane)
    val faultOut = Decoupled(new RtPdsRayStateFault); val retire = Flipped(Decoupled(new RtPdsReadRetire))
    val candidate = Decoupled(new RtLsuCandidate); val injection = Flipped(Decoupled(new RtPdsReadInjection))
  })
  val contexts = RegInit(VecInit(Seq.fill(p.pdsReadWarpEntryCount)(0.U.asTypeOf(new RtPdsReadContext))))
  val rows = Reg(Vec(p.pdsReadWarpEntryCount, Vec(num_thread, Vec(p.pdsRayStateFieldCount, UInt(xLen.W)))))
  val admit = contexts(io.admission.bits.warpId)
  io.admission.ready := io.admission.bits.activeMask.orR && !admit.valid
  when(io.admission.fire) { admit.valid := true.B; admit.pds := io.admission.bits.pds; admit.activeMask := io.admission.bits.activeMask; admit.issued := 0.U; admit.received := 0.U; admit.delivered := 0.U; admit.faultMask := 0.U }
  val needed = VecInit(contexts.map(c => c.valid && c.issued =/= allFields)).asUInt
  val hasNeeded = needed.orR; val warp = PriorityEncoder(needed); val context = contexts(warp)
  val field = PriorityEncoder((~context.issued).asUInt & allFields)
  val wordsPerField = context.pds.numWarps * context.pds.numThreads
  val physicalWord = (RtAbiLayout.SlotAccelLo.U + field) * wordsPerField + context.pds.pdsWarpTidBase
  io.candidate.valid := hasNeeded
  io.candidate.bits := 0.U.asTypeOf(new RtLsuCandidate)
  when(hasNeeded) {
    io.candidate.bits.source := RtLsuSource.pdsRead; io.candidate.bits.cacheLineAddress := context.pds.baseAddress + (physicalWord << 2)
    io.candidate.bits.wordMask := Fill(p.lineWordCount, 1.U(1.W)); io.candidate.bits.locator.warpId := warp; io.candidate.bits.locator.field := field
  }
  when(io.candidate.fire) { assert(io.candidate.bits.cacheLineAddress(6, 0) === 0.U); context.issued := context.issued | UIntToOH(field, p.pdsRayStateFieldCount) }
  val returned = contexts(io.injection.bits.warpId)
  io.injection.ready := returned.valid && returned.issued(io.injection.bits.field) && !returned.received(io.injection.bits.field)
  when(io.injection.fire) {
    returned.received := returned.received | UIntToOH(io.injection.bits.field, p.pdsRayStateFieldCount)
    when(io.injection.bits.fault) { returned.faultMask := returned.faultMask | returned.activeMask }
    for (lane <- 0 until num_thread) when(returned.activeMask(lane)) { rows(io.injection.bits.warpId)(lane)(io.injection.bits.field) := io.injection.bits.data(lane) }
  }
  val completed = VecInit(contexts.map(c => c.valid && c.received === allFields))
  val laneAvailable = Wire(Vec(p.pdsReadWarpEntryCount, UInt(num_thread.W))); val faultAvailable = Wire(Vec(p.pdsReadWarpEntryCount, UInt(num_thread.W)))
  for (i <- 0 until p.pdsReadWarpEntryCount) { val left = contexts(i).activeMask & (~contexts(i).delivered).asUInt; laneAvailable(i) := Mux(completed(i), left & (~contexts(i).faultMask).asUInt, 0.U); faultAvailable(i) := Mux(completed(i), left & contexts(i).faultMask, 0.U) }
  val hasFault = faultAvailable.map(_.orR).reduce(_ || _); val hasLane = laneAvailable.map(_.orR).reduce(_ || _)
  val laneWarp = PriorityEncoder(VecInit(laneAvailable.map(_.orR)).asUInt); val faultWarp = PriorityEncoder(VecInit(faultAvailable.map(_.orR)).asUInt)
  val lane = PriorityEncoder(laneAvailable(laneWarp)); val faultLane = PriorityEncoder(faultAvailable(faultWarp))
  io.laneOut.valid := hasLane && !hasFault; io.laneOut.bits := 0.U.asTypeOf(new RtPdsReadLane)
  when(io.laneOut.valid) { io.laneOut.bits.warpId := laneWarp; io.laneOut.bits.laneId := lane; io.laneOut.bits.data := rows(laneWarp)(lane) }
  io.faultOut.valid := hasFault; io.faultOut.bits.warpId := faultWarp; io.faultOut.bits.laneId := faultLane
  when(io.laneOut.fire) { contexts(laneWarp).delivered := contexts(laneWarp).delivered | UIntToOH(lane, num_thread) }
  when(io.faultOut.fire) { contexts(faultWarp).delivered := contexts(faultWarp).delivered | UIntToOH(faultLane, num_thread) }
  val retire = contexts(io.retire.bits.warpId); io.retire.ready := retire.valid && (retire.delivered & retire.activeMask) === retire.activeMask
  when(io.retire.fire) { retire.valid := false.B }
}

class RtPdsHitRecordUpdate extends Bundle { val warpId = UInt(depth_warp.W); val laneId = UInt(log2Ceil(num_thread).W); val committed = Bool(); val dirty = UInt(RtMemTableParameters.hitRecordStorageWordCount.W); val data = Vec(RtMemTableParameters.hitRecordStorageWordCount, UInt(xLen.W)) }
class RtPdsWriteAdmission extends Bundle { val warpId = UInt(depth_warp.W); val pds = new RtPdsContext; val activeMask = UInt(num_thread.W) }
class RtPdsWriteFinish extends Bundle { val warpId = UInt(depth_warp.W); val terminalMask = UInt(num_thread.W) }
class RtPdsWriteDrained extends Bundle { val warpId = UInt(depth_warp.W) }
private[rtcore] class RtPdsWriteContext extends Bundle { val valid = Bool(); val pds = new RtPdsContext; val activeMask = UInt(num_thread.W); val flushing = Bool(); val drained = Bool(); val waiting = Bool(); val record = Bool(); val word = UInt(log2Ceil(RtMemTableParameters.hitRecordStorageWordCount).W); val upperHalf = Bool() }

class RtPdsWriteTable(warpEntryCount: Int = RtMemTableParameters.pdsWriteWarpEntryCount) extends Module {
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
    io.candidate.bits.locator.warpId := warp; io.candidate.bits.locator.record := context.record; io.candidate.bits.locator.word := context.word; io.candidate.bits.locator.upperHalf := context.upperHalf
  }
  when(io.candidate.fire) { assert(io.candidate.bits.cacheLineAddress(5, 0) === 0.U); context.waiting := true.B }
  val returned = contexts(index(io.injection.bits.warpId)); io.injection.ready := returned.valid && returned.flushing && returned.waiting && returned.record === io.injection.bits.record && returned.word === io.injection.bits.word && returned.upperHalf === io.injection.bits.upperHalf
  when(io.injection.fire) { assert(!io.injection.bits.fault); returned.waiting := false.B; advance(returned) }
  val drained = VecInit(contexts.map(c => c.valid && c.drained)).asUInt; io.drained.valid := drained.orR; io.drained.bits.warpId := PriorityEncoder(drained)
  when(io.drained.fire) { contexts(PriorityEncoder(drained)).valid := false.B }
}

class RtAsLineWaiter extends Bundle { val warpId = UInt(depth_warp.W); val laneId = UInt(log2Ceil(num_thread).W); val nodeKind = UInt(2.W); val nodeRef = UInt(32.W) }
class RtAsTableAcquire extends Bundle { val lineAddress = UInt(memReqLen.W); val wordMask = UInt(RtMemTableParameters.lineWordCount.W); val is64B = Bool(); val waiter = new RtAsLineWaiter }
class RtAsTableReadyLine extends Bundle { val waiter = new RtAsLineWaiter; val data = Vec(RtMemTableParameters.lineWordCount, UInt(xLen.W)); val fault = Bool() }
private[rtcore] class RtAsEntry extends Bundle { val valid = Bool(); val sent = Bool(); val returned = Bool(); val lineAddress = UInt(memReqLen.W); val wordMask = UInt(RtMemTableParameters.lineWordCount.W); val is64B = Bool(); val waiterValid = Vec(RtMemTableParameters.maxAsWaitersPerEntry, Bool()); val waiters = Vec(RtMemTableParameters.maxAsWaitersPerEntry, new RtAsLineWaiter); val data = Vec(RtMemTableParameters.lineWordCount, UInt(xLen.W)); val fault = Bool() }

class RtAsTable extends Module {
  private val p = RtMemTableParameters
  val io = IO(new Bundle { val acquire = Flipped(Decoupled(new RtAsTableAcquire)); val readyLine = Decoupled(new RtAsTableReadyLine); val candidate = Decoupled(new RtLsuCandidate); val injection = Flipped(Decoupled(new RtAsInjection)) })
  val entries = RegInit(VecInit(Seq.fill(p.asEntryCount)(0.U.asTypeOf(new RtAsEntry))))
  val merge = VecInit(entries.map(e => e.valid && !e.sent && e.lineAddress === io.acquire.bits.lineAddress && e.is64B === io.acquire.bits.is64B && !e.waiterValid.asUInt.andR)).asUInt; val free = VecInit(entries.map(!_.valid)).asUInt
  val hasMerge = merge.orR; val place = Mux(hasMerge, PriorityEncoder(merge), PriorityEncoder(free)); io.acquire.ready := hasMerge || free.orR
  when(io.acquire.fire) {
    assert(Mux(io.acquire.bits.is64B, io.acquire.bits.lineAddress(5, 0) === 0.U, io.acquire.bits.lineAddress(6, 0) === 0.U))
    val e = entries(place); when(!hasMerge) { e.valid := true.B; e.sent := false.B; e.returned := false.B; e.lineAddress := io.acquire.bits.lineAddress; e.wordMask := io.acquire.bits.wordMask; e.is64B := io.acquire.bits.is64B; e.waiterValid.foreach(_ := false.B); e.fault := false.B }.otherwise { e.wordMask := e.wordMask | io.acquire.bits.wordMask }; val w = PriorityEncoder((~e.waiterValid.asUInt).asUInt); e.waiterValid(w) := true.B; e.waiters(w) := io.acquire.bits.waiter
  }
  val unsent = VecInit(entries.map(e => e.valid && !e.sent)).asUInt; val request = PriorityEncoder(unsent); io.candidate.valid := unsent.orR; io.candidate.bits := 0.U.asTypeOf(new RtLsuCandidate)
  when(io.candidate.valid) { io.candidate.bits.source := RtLsuSource.as; io.candidate.bits.cacheLineAddress := entries(request).lineAddress; io.candidate.bits.is64B := entries(request).is64B; io.candidate.bits.wordMask := entries(request).wordMask; io.candidate.bits.locator.asEntry := request }
  when(io.candidate.fire) { entries(request).sent := true.B }
  val response = entries(io.injection.bits.entry); io.injection.ready := response.valid && response.sent && !response.returned
  when(io.injection.fire) { assert(!io.injection.bits.fault); response.returned := true.B; response.data := io.injection.bits.data; response.fault := io.injection.bits.fault }
  val ready = VecInit(entries.map(e => e.valid && e.returned && e.waiterValid.asUInt.orR)).asUInt; val entry = PriorityEncoder(ready); val waiter = PriorityEncoder(entries(entry).waiterValid.asUInt)
  io.readyLine.valid := ready.orR; io.readyLine.bits := 0.U.asTypeOf(new RtAsTableReadyLine)
  when(io.readyLine.valid) { io.readyLine.bits.waiter := entries(entry).waiters(waiter); io.readyLine.bits.data := entries(entry).data; io.readyLine.bits.fault := entries(entry).fault }
  when(io.readyLine.fire) { val left = entries(entry).waiterValid.asUInt & (~UIntToOH(waiter, p.maxAsWaitersPerEntry)).asUInt; entries(entry).waiterValid(waiter) := false.B; when(left === 0.U) { entries(entry).valid := false.B } }
}

class RtMemTable extends Module {
  val io = IO(new Bundle {
    val pdsReadAdmission = Flipped(Decoupled(new RtPdsReadAdmission)); val pdsReadLane = Decoupled(new RtPdsReadLane); val pdsFault = Decoupled(new RtPdsRayStateFault); val pdsReadRetire = Flipped(Decoupled(new RtPdsReadRetire))
    val pdsWriteAdmission = Flipped(Decoupled(new RtPdsWriteAdmission)); val pdsHitUpdate = Flipped(Decoupled(new RtPdsHitRecordUpdate)); val pdsWriteFinish = Flipped(Decoupled(new RtPdsWriteFinish)); val pdsWriteDrained = Decoupled(new RtPdsWriteDrained)
    val asAcquire = Flipped(Decoupled(new RtAsTableAcquire)); val asReadyLine = Decoupled(new RtAsTableReadyLine)
    val rtLsuReport = Decoupled(new RtLsuReport); val rtLsuRespond = Flipped(Decoupled(new RtLsuRespond))
  })
  val read = Module(new RtPdsReadTable); val write = Module(new RtPdsWriteTable); val as = Module(new RtAsTable); val mshr = Module(new RtLsuMshr)
  read.io.admission <> io.pdsReadAdmission; io.pdsReadLane <> read.io.laneOut; io.pdsFault <> read.io.faultOut; read.io.retire <> io.pdsReadRetire
  write.io.admission <> io.pdsWriteAdmission; write.io.update <> io.pdsHitUpdate; write.io.finish <> io.pdsWriteFinish; io.pdsWriteDrained <> write.io.drained
  as.io.acquire <> io.asAcquire; io.asReadyLine <> as.io.readyLine
  mshr.io.pdsRead <> read.io.candidate; mshr.io.pdsWrite <> write.io.candidate; mshr.io.as <> as.io.candidate
  read.io.injection <> mshr.io.pdsReadInjection; write.io.injection <> mshr.io.pdsWriteInjection; as.io.injection <> mshr.io.asInjection
  io.rtLsuReport <> mshr.io.report; mshr.io.respond <> io.rtLsuRespond
}

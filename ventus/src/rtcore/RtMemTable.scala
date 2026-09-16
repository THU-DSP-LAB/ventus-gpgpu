package rtcore

import chisel3._
import chisel3.util._
import lsu.{LsuClientRequest, LsuClientResponse}
import top.parameters._

/** Structures retained only for BVH/AS cacheline traffic.  Ray state and hit
  * records live in RtLocalSram and never enter this path. */
object RtMemTableParameters {
  val hitRecordStorageWordCount = RtAbiLayout.HitRecordWordCount
  val asEntryCount = 32
  val maxAsWaitersPerEntry = num_thread
  val lineWordCount = RtCorePortParameters.lineWordCount
  require(lineWordCount == num_thread)
}

class RtAsLineWaiter extends Bundle {
  val warpId = UInt(RtCorePortParameters.residentWarpIdWidth.W)
  val laneId = UInt(log2Ceil(num_thread).W)
  val nodeKind = UInt(3.W)
  val objectRay = Bool()
}
class RtAsTableAcquire extends Bundle { val lineAddress = UInt(memReqLen.W); val wordMask = UInt(RtMemTableParameters.lineWordCount.W); val is64B = Bool(); val waiter = new RtAsLineWaiter }
class RtAsTableReadyLine extends Bundle { val waiter = new RtAsLineWaiter; val data = Vec(RtMemTableParameters.lineWordCount, UInt(xLen.W)) }
private[rtcore] class RtAsEntry extends Bundle { val valid = Bool(); val sent = Bool(); val returned = Bool(); val lineAddress = UInt(memReqLen.W); val wordMask = UInt(RtMemTableParameters.lineWordCount.W); val is64B = Bool(); val waiterValid = Vec(RtMemTableParameters.maxAsWaitersPerEntry, Bool()); val waiters = Vec(RtMemTableParameters.maxAsWaitersPerEntry, new RtAsLineWaiter); val data = Vec(RtMemTableParameters.lineWordCount, UInt(xLen.W)) }

class RtAsTable extends Module {
  private val p = RtMemTableParameters
  val io = IO(new Bundle {
    val acquire = Flipped(Decoupled(new RtAsTableAcquire)); val readyLine = Decoupled(new RtAsTableReadyLine)
    val candidate = Decoupled(new RtLsuCandidate); val injection = Flipped(Decoupled(new RtAsInjection)); val idle = Output(Bool())
    val clear = Flipped(Decoupled(Bool()))
    val residentHit = Output(Bool()); val pendingMerge = Output(Bool()); val miss = Output(Bool())
    val refill = Output(Bool()); val eviction = Output(Bool())
    val detailed = if (PMU_DETAILED) Some(Output(new RtAsObservation)) else None
  })
  val entries = RegInit(VecInit(Seq.fill(p.asEntryCount)(0.U.asTypeOf(new RtAsEntry))))
  private val indexWidth = log2Ceil(p.asEntryCount)
  require(isPow2(p.asEntryCount), "Reverse replacement wraps at table size")
  val victimCursor = RegInit((p.asEntryCount - 1).U(indexWidth.W))
  val matching = VecInit(entries.map(e => e.valid && e.lineAddress === io.acquire.bits.lineAddress &&
    e.is64B === io.acquire.bits.is64B)).asUInt
  val merge = matching & VecInit(entries.map(e => !e.waiterValid.asUInt.andR)).asUInt
  val free = VecInit(entries.map(!_.valid)).asUInt
  val evictable = VecInit(entries.map(e => e.valid && e.returned && !e.waiterValid.asUInt.orR)).asUInt
  val reverseEligible = VecInit((0 until p.asEntryCount).map { offset =>
    evictable((victimCursor - offset.U)(indexWidth - 1, 0))
  }).asUInt
  val victim = (victimCursor - PriorityEncoder(reverseEligible))(indexWidth - 1, 0)
  val hasMerge = merge.orR
  val place = Mux(hasMerge, PriorityEncoder(merge), Mux(free.orR, PriorityEncoder(free), victim))
  // Never duplicate a full matching entry, and never evict a pending refill or waiter.
  io.acquire.ready := !io.clear.valid && (hasMerge || (!matching.orR && (free.orR || reverseEligible.orR)))
  io.residentHit := io.acquire.fire && hasMerge && entries(place).returned
  io.pendingMerge := io.acquire.fire && hasMerge && !entries(place).returned
  io.miss := io.acquire.fire && !hasMerge
  io.eviction := io.miss && !free.orR
  when(io.acquire.fire) {
    assert(Mux(io.acquire.bits.is64B, io.acquire.bits.lineAddress(5, 0) === 0.U, io.acquire.bits.lineAddress(6, 0) === 0.U))
    val e = entries(place)
    when(io.eviction) { victimCursor := victim - 1.U }
    when(!hasMerge) { e.valid := true.B; e.sent := false.B; e.returned := false.B; e.lineAddress := io.acquire.bits.lineAddress; e.wordMask := io.acquire.bits.wordMask; e.is64B := io.acquire.bits.is64B; e.waiterValid.foreach(_ := false.B) }.otherwise { e.wordMask := e.wordMask | io.acquire.bits.wordMask }
    val w = PriorityEncoder((~e.waiterValid.asUInt).asUInt); e.waiterValid(w) := true.B; e.waiters(w) := io.acquire.bits.waiter
  }
  val unsent = VecInit(entries.map(e => e.valid && !e.sent)).asUInt; val request = PriorityEncoder(unsent)
  io.candidate.valid := unsent.orR; io.candidate.bits := 0.U.asTypeOf(new RtLsuCandidate)
  when(io.candidate.valid) { io.candidate.bits.source := RtLsuSource.as; io.candidate.bits.cacheLineAddress := entries(request).lineAddress; io.candidate.bits.is64B := entries(request).is64B; io.candidate.bits.wordMask := entries(request).wordMask; io.candidate.bits.locator.tableLocation := request }
  when(io.candidate.fire) { entries(request).sent := true.B }
  io.refill := io.candidate.fire
  val response = entries(io.injection.bits.entry); io.injection.ready := response.valid && response.sent && !response.returned
  when(io.injection.fire) { assert(!io.injection.bits.fault); response.returned := true.B; response.data := io.injection.bits.data }
  val ready = VecInit(entries.map(e => e.valid && e.returned && e.waiterValid.asUInt.orR)).asUInt; val entry = PriorityEncoder(ready); val waiter = PriorityEncoder(entries(entry).waiterValid.asUInt)
  io.readyLine.valid := ready.orR; io.readyLine.bits := 0.U.asTypeOf(new RtAsTableReadyLine)
  when(io.readyLine.valid) { io.readyLine.bits.waiter := entries(entry).waiters(waiter); io.readyLine.bits.data := entries(entry).data }
  // Acquire chooses a pre-edge free slot, so simultaneous serve/acquire cannot
  // clobber one another even when they target the same resident line.
  when(io.readyLine.fire) { entries(entry).waiterValid(waiter) := false.B }
  val busy = entries.map(e => e.valid && (!e.returned || e.waiterValid.asUInt.orR)).reduce(_ || _)
  io.clear.ready := !busy
  when(io.clear.fire) {
    entries.foreach(_.valid := false.B)
    victimCursor := (p.asEntryCount - 1).U
  }
  io.idle := !VecInit(entries.map(_.valid)).asUInt.orR
  if (PMU_DETAILED) {
    require(p.asEntryCount == 32 && num_thread == 32, "Update detailed observation index widths")
    val d = io.detailed.get
    d.occupied := PopCount(entries.map(_.valid))
    d.returned := PopCount(entries.map(e => e.valid && e.returned))
    d.readyEntries := PopCount(ready)
    d.acquire := io.acquire.fire
    d.allocate := io.miss
    d.acquireEntry := place
    d.address := io.acquire.bits.lineAddress
    d.is64B := io.acquire.bits.is64B
    d.acquireLane := io.acquire.bits.waiter.laneId
    d.refill := io.candidate.fire
    d.refillEntry := request
    d.injection := io.injection.fire
    d.injectionEntry := io.injection.bits.entry
    d.serve := io.readyLine.fire
    d.serveEntry := entry
    d.serveLane := io.readyLine.bits.waiter.laneId
    d.retire := false.B
    d.eviction := io.eviction
    d.clear := io.clear.fire
  }
}

class RtMemTable extends Module {
  val io = IO(new Bundle {
    val asAcquire = Flipped(Decoupled(new RtAsTableAcquire)); val asReadyLine = Decoupled(new RtAsTableReadyLine)
    val clear = Flipped(Decoupled(Bool()))
    val quiescent = Output(Bool())
    val asCacheResidentHit = Output(Bool()); val asCachePendingMerge = Output(Bool()); val asCacheMiss = Output(Bool())
    val asCacheRefill = Output(Bool()); val asCacheEviction = Output(Bool())
    val lsuRequest = Decoupled(new LsuClientRequest); val lsuResponse = Flipped(Decoupled(new LsuClientResponse))
    val detailed = if (PMU_DETAILED) Some(Output(new RtAsObservation)) else None
  })
  val as = Module(new RtAsTable); val mshr = Module(new RtLsuMshr)
  as.io.acquire <> io.asAcquire; io.asReadyLine <> as.io.readyLine
  as.io.clear <> io.clear
  mshr.io.as <> as.io.candidate; as.io.injection <> mshr.io.asInjection
  io.lsuRequest <> mshr.io.request; mshr.io.response <> io.lsuResponse
  io.quiescent := as.io.idle && mshr.io.idle
  io.asCacheResidentHit := as.io.residentHit
  io.asCachePendingMerge := as.io.pendingMerge
  io.asCacheMiss := as.io.miss
  io.asCacheRefill := as.io.refill
  io.asCacheEviction := as.io.eviction
  if (PMU_DETAILED) { io.detailed.get := as.io.detailed.get }
}

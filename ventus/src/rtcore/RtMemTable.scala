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
  val asEntryIndexWidth = log2Ceil(asEntryCount)
  val asReadPortCount = 1 + RtCoreSchedulerParameters.boxUnitCount + RtCoreSchedulerParameters.triangleUnitCount
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
private[rtcore] class RtAsWaiterMeta extends Bundle { val nodeKind = UInt(3.W); val objectRay = Bool() }
private[rtcore] class RtAsReadyMeta extends Bundle { val entryId = UInt(RtMemTableParameters.asEntryIndexWidth.W); val nodeKind = UInt(3.W); val objectRay = Bool() }
private[rtcore] class RtAsLineReadPort extends Bundle {
  val entryId = Input(UInt(RtMemTableParameters.asEntryIndexWidth.W))
  val data = Output(Vec(RtMemTableParameters.lineWordCount, UInt(xLen.W)))
}
private[rtcore] class RtAsEntry extends Bundle {
  val valid = Bool(); val sent = Bool(); val returned = Bool()
  val lineAddress = UInt(memReqLen.W); val wordMask = UInt(RtMemTableParameters.lineWordCount.W); val is64B = Bool()
  // Slot i belongs permanently to lane i.  The scheduler permits at most one
  // unconsumed AS request per lane, so this is a bitmap rather than a CAM.
  val waiterValid = UInt(num_thread.W)
  val waiterMeta = Vec(num_thread, new RtAsWaiterMeta)
  val data = Vec(RtMemTableParameters.lineWordCount, UInt(xLen.W))
}

class RtAsTable extends Module {
  private val p = RtMemTableParameters
  val io = IO(new Bundle {
    val acquire = Flipped(Decoupled(new RtAsTableAcquire))
    // One small readiness record per lane.  Full cacheline data is read only
    // by a selected IU/scalar consumer through lineRead below.
    val readyMeta = Output(Vec(num_thread, Valid(new RtAsReadyMeta)))
    val lineRead = Vec(p.asReadPortCount, new RtAsLineReadPort)
    val consumeLanes = Input(UInt(num_thread.W))
    val candidate = Decoupled(new RtLsuCandidate); val injection = Flipped(Decoupled(new RtAsInjection)); val idle = Output(Bool())
    val clear = Flipped(Decoupled(Bool()))
    val residentHit = Output(Bool()); val pendingMerge = Output(Bool()); val miss = Output(Bool())
    val refill = Output(Bool()); val eviction = Output(Bool())
    val detailed = if (PMU_DETAILED) Some(Output(new RtAsObservation)) else None
  })
  val entries = RegInit(VecInit(Seq.fill(p.asEntryCount)(0.U.asTypeOf(new RtAsEntry))))
  private val indexWidth = p.asEntryIndexWidth
  require(isPow2(p.asEntryCount), "Reverse replacement wraps at table size")
  val victimCursor = RegInit((p.asEntryCount - 1).U(indexWidth.W))
  val matching = VecInit(entries.map(e => e.valid && e.lineAddress === io.acquire.bits.lineAddress &&
    e.is64B === io.acquire.bits.is64B)).asUInt
  val merge = matching & VecInit(entries.map(e => !e.waiterValid(io.acquire.bits.waiter.laneId))).asUInt
  val free = VecInit(entries.map(!_.valid)).asUInt
  val evictable = VecInit(entries.map(e => e.valid && e.returned && !e.waiterValid.orR)).asUInt
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
    when(!hasMerge) { e.valid := true.B; e.sent := false.B; e.returned := false.B; e.lineAddress := io.acquire.bits.lineAddress; e.wordMask := io.acquire.bits.wordMask; e.is64B := io.acquire.bits.is64B }.otherwise { e.wordMask := e.wordMask | io.acquire.bits.wordMask }
    e.waiterMeta(io.acquire.bits.waiter.laneId).nodeKind := io.acquire.bits.waiter.nodeKind
    e.waiterMeta(io.acquire.bits.waiter.laneId).objectRay := io.acquire.bits.waiter.objectRay
  }
  val unsent = VecInit(entries.map(e => e.valid && !e.sent)).asUInt; val request = PriorityEncoder(unsent)
  io.candidate.valid := unsent.orR; io.candidate.bits := 0.U.asTypeOf(new RtLsuCandidate)
  when(io.candidate.valid) { io.candidate.bits.cacheLineAddress := entries(request).lineAddress; io.candidate.bits.is64B := entries(request).is64B; io.candidate.bits.wordMask := entries(request).wordMask; io.candidate.bits.entryId := request }
  when(io.candidate.fire) { entries(request).sent := true.B }
  io.refill := io.candidate.fire
  val response = entries(io.injection.bits.entry); io.injection.ready := response.valid && response.sent && !response.returned
  // The tagged entry retains format metadata until its reply arrives. Normalize
  // only the requested half; LSU words outside coverageMask need not be valid.
  when(io.injection.fire) {
    assert(!io.injection.bits.fault)
    response.returned := true.B
    for (word <- 0 until p.lineWordCount) {
      if (word < p.lineWordCount / 2) {
        response.data(word) := Mux(response.is64B && response.lineAddress(6),
          io.injection.bits.data(word + p.lineWordCount / 2), io.injection.bits.data(word))
      } else {
        response.data(word) := Mux(response.is64B, 0.U, io.injection.bits.data(word))
      }
    }
  }
  val ready = VecInit(entries.map(e => e.valid && e.returned && e.waiterValid.orR)).asUInt
  val laneReady = Wire(Vec(num_thread, Bool()))
  val laneEntry = Wire(Vec(num_thread, UInt(indexWidth.W)))
  for (lane <- 0 until num_thread) {
    val matchingWaiter = VecInit(entries.map(e => e.valid && e.returned && e.waiterValid(lane))).asUInt
    val selectedEntry = PriorityEncoder(matchingWaiter)
    laneReady(lane) := matchingWaiter.orR
    laneEntry(lane) := selectedEntry
    io.readyMeta(lane).valid := matchingWaiter.orR
    io.readyMeta(lane).bits := 0.U.asTypeOf(new RtAsReadyMeta)
    when(matchingWaiter.orR) {
      io.readyMeta(lane).bits.entryId := selectedEntry
      io.readyMeta(lane).bits.nodeKind := entries(selectedEntry).waiterMeta(lane).nodeKind
      io.readyMeta(lane).bits.objectRay := entries(selectedEntry).waiterMeta(lane).objectRay
    }
  }
  for (port <- 0 until p.asReadPortCount)
    io.lineRead(port).data := entries(io.lineRead(port).entryId).data
  // Each entry owns one lane bitmap update, so several lanes can consume one
  // returned line in the same cycle without dynamic indexed register writes.
  for (entry <- 0 until p.asEntryCount) {
    val consumeMask = VecInit((0 until num_thread).map { lane =>
      io.consumeLanes(lane) && laneReady(lane) && laneEntry(lane) === entry.U
    }).asUInt
    val acquireHere = io.acquire.fire && place === entry.U
    val acquireMask = UIntToOH(io.acquire.bits.waiter.laneId, num_thread)
    when(acquireHere) {
      entries(entry).waiterValid := Mux(hasMerge,
        (entries(entry).waiterValid & ~consumeMask) | acquireMask, acquireMask)
    }.elsewhen(consumeMask.orR) {
      entries(entry).waiterValid := entries(entry).waiterValid & ~consumeMask
    }
  }
  val busy = entries.map(e => e.valid && (!e.returned || e.waiterValid.orR)).reduce(_ || _)
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
    d.residentHit := io.residentHit
    d.readyLanes := laneReady.asUInt
    d.acquireEntry := place
    d.address := io.acquire.bits.lineAddress
    d.is64B := io.acquire.bits.is64B
    d.acquireLane := io.acquire.bits.waiter.laneId
    d.refill := io.candidate.fire
    d.refillEntry := request
    d.injection := io.injection.fire
    d.injectionEntry := io.injection.bits.entry
    d.serve := io.consumeLanes.orR
    d.serveEntry := PriorityEncoder(ready)
    d.serveLane := PriorityEncoder(io.consumeLanes)
    d.serveLanes := io.consumeLanes
    for (lane <- 0 until num_thread) d.serveEntries(lane) := laneEntry(lane)
    d.eviction := io.eviction
    d.clear := io.clear.fire
  }
}

class RtMemTable extends Module {
  private val p = RtMemTableParameters
  val io = IO(new Bundle {
    val asAcquire = Flipped(Decoupled(new RtAsTableAcquire))
    val asReadyMeta = Output(Vec(num_thread, Valid(new RtAsReadyMeta)))
    val asLineRead = Vec(p.asReadPortCount, new RtAsLineReadPort)
    val asConsumeLanes = Input(UInt(num_thread.W))
    val clear = Flipped(Decoupled(Bool()))
    val quiescent = Output(Bool())
    val asCacheResidentHit = Output(Bool()); val asCachePendingMerge = Output(Bool()); val asCacheMiss = Output(Bool())
    val asCacheRefill = Output(Bool()); val asCacheEviction = Output(Bool())
    val lsuRequest = Decoupled(new LsuClientRequest); val lsuResponse = Flipped(Decoupled(new LsuClientResponse))
    val detailed = if (PMU_DETAILED) Some(Output(new RtAsObservation)) else None
  })
  val as = Module(new RtAsTable); val adapter = Module(new RtLsuAdapter)
  as.io.acquire <> io.asAcquire
  io.asReadyMeta := as.io.readyMeta
  io.asLineRead <> as.io.lineRead
  as.io.consumeLanes := io.asConsumeLanes
  as.io.clear <> io.clear
  adapter.io.as <> as.io.candidate; as.io.injection <> adapter.io.asInjection
  io.lsuRequest <> adapter.io.request; adapter.io.response <> io.lsuResponse
  // Pending transactions keep their AS entry valid and prevent clear.
  io.quiescent := as.io.idle
  io.asCacheResidentHit := as.io.residentHit
  io.asCachePendingMerge := as.io.pendingMerge
  io.asCacheMiss := as.io.miss
  io.asCacheRefill := as.io.refill
  io.asCacheEviction := as.io.eviction
  if (PMU_DETAILED) { io.detailed.get := as.io.detailed.get }
}

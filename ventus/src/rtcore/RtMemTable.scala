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
  val maxAsWaitersPerEntry = 2
  val lineWordCount = RtCorePortParameters.lineWordCount
  require(lineWordCount == num_thread)
}

class RtAsLineWaiter extends Bundle { val warpId = UInt(RtCorePortParameters.residentWarpIdWidth.W); val laneId = UInt(log2Ceil(num_thread).W); val nodeKind = UInt(2.W) }
class RtAsTableAcquire extends Bundle { val lineAddress = UInt(memReqLen.W); val wordMask = UInt(RtMemTableParameters.lineWordCount.W); val is64B = Bool(); val waiter = new RtAsLineWaiter }
class RtAsTableReadyLine extends Bundle { val waiter = new RtAsLineWaiter; val data = Vec(RtMemTableParameters.lineWordCount, UInt(xLen.W)) }
private[rtcore] class RtAsEntry extends Bundle { val valid = Bool(); val sent = Bool(); val returned = Bool(); val lineAddress = UInt(memReqLen.W); val wordMask = UInt(RtMemTableParameters.lineWordCount.W); val is64B = Bool(); val waiterValid = Vec(RtMemTableParameters.maxAsWaitersPerEntry, Bool()); val waiters = Vec(RtMemTableParameters.maxAsWaitersPerEntry, new RtAsLineWaiter); val data = Vec(RtMemTableParameters.lineWordCount, UInt(xLen.W)) }

class RtAsTable extends Module {
  private val p = RtMemTableParameters
  val io = IO(new Bundle { val acquire = Flipped(Decoupled(new RtAsTableAcquire)); val readyLine = Decoupled(new RtAsTableReadyLine); val candidate = Decoupled(new RtLsuCandidate); val injection = Flipped(Decoupled(new RtAsInjection)); val idle = Output(Bool()) })
  val entries = RegInit(VecInit(Seq.fill(p.asEntryCount)(0.U.asTypeOf(new RtAsEntry))))
  val merge = VecInit(entries.map(e => e.valid && !e.sent && e.lineAddress === io.acquire.bits.lineAddress && e.is64B === io.acquire.bits.is64B && !e.waiterValid.asUInt.andR)).asUInt
  val free = VecInit(entries.map(!_.valid)).asUInt
  val hasMerge = merge.orR; val place = Mux(hasMerge, PriorityEncoder(merge), PriorityEncoder(free)); io.acquire.ready := hasMerge || free.orR
  when(io.acquire.fire) {
    assert(Mux(io.acquire.bits.is64B, io.acquire.bits.lineAddress(5, 0) === 0.U, io.acquire.bits.lineAddress(6, 0) === 0.U))
    val e = entries(place)
    when(!hasMerge) { e.valid := true.B; e.sent := false.B; e.returned := false.B; e.lineAddress := io.acquire.bits.lineAddress; e.wordMask := io.acquire.bits.wordMask; e.is64B := io.acquire.bits.is64B; e.waiterValid.foreach(_ := false.B) }.otherwise { e.wordMask := e.wordMask | io.acquire.bits.wordMask }
    val w = PriorityEncoder((~e.waiterValid.asUInt).asUInt); e.waiterValid(w) := true.B; e.waiters(w) := io.acquire.bits.waiter
  }
  val unsent = VecInit(entries.map(e => e.valid && !e.sent)).asUInt; val request = PriorityEncoder(unsent)
  io.candidate.valid := unsent.orR; io.candidate.bits := 0.U.asTypeOf(new RtLsuCandidate)
  when(io.candidate.valid) { io.candidate.bits.source := RtLsuSource.as; io.candidate.bits.cacheLineAddress := entries(request).lineAddress; io.candidate.bits.is64B := entries(request).is64B; io.candidate.bits.wordMask := entries(request).wordMask; io.candidate.bits.locator.tableLocation := request }
  when(io.candidate.fire) { entries(request).sent := true.B }
  val response = entries(io.injection.bits.entry); io.injection.ready := response.valid && response.sent && !response.returned
  when(io.injection.fire) { assert(!io.injection.bits.fault); response.returned := true.B; response.data := io.injection.bits.data }
  val ready = VecInit(entries.map(e => e.valid && e.returned && e.waiterValid.asUInt.orR)).asUInt; val entry = PriorityEncoder(ready); val waiter = PriorityEncoder(entries(entry).waiterValid.asUInt)
  io.readyLine.valid := ready.orR; io.readyLine.bits := 0.U.asTypeOf(new RtAsTableReadyLine)
  when(io.readyLine.valid) { io.readyLine.bits.waiter := entries(entry).waiters(waiter); io.readyLine.bits.data := entries(entry).data }
  when(io.readyLine.fire) { val left = entries(entry).waiterValid.asUInt & (~UIntToOH(waiter, p.maxAsWaitersPerEntry)).asUInt; entries(entry).waiterValid(waiter) := false.B; when(left === 0.U) { entries(entry).valid := false.B } }
  io.idle := !VecInit(entries.map(_.valid)).asUInt.orR
}

class RtMemTable extends Module {
  val io = IO(new Bundle {
    val asAcquire = Flipped(Decoupled(new RtAsTableAcquire)); val asReadyLine = Decoupled(new RtAsTableReadyLine)
    val quiescent = Output(Bool())
    val lsuRequest = Decoupled(new LsuClientRequest); val lsuResponse = Flipped(Decoupled(new LsuClientResponse))
  })
  val as = Module(new RtAsTable); val mshr = Module(new RtLsuMshr)
  as.io.acquire <> io.asAcquire; io.asReadyLine <> as.io.readyLine
  mshr.io.as <> as.io.candidate; as.io.injection <> mshr.io.asInjection
  io.lsuRequest <> mshr.io.request; mshr.io.response <> io.lsuResponse
  io.quiescent := as.io.idle && mshr.io.idle
}

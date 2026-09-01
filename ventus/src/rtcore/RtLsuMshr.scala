package rtcore

import chisel3._
import chisel3.util._
import lsu.{LsuClientId, LsuClientRequest, LsuClientResponse}
import top.parameters._

object RtLsuSource {
  val width = 2
  val pdsRead = 0.U(width.W)
  val pdsWrite = 1.U(width.W)
  val as = 2.U(width.W)
}

/** The minimum table location retained while an LSU transaction is in flight. */
class RtLsuLocator extends Bundle {
  val warpId = UInt(RtCorePortParameters.residentWarpIdWidth.W)
  val field = UInt(log2Ceil(RtMemTableParameters.pdsRayStateFieldCount).W)
  val record = Bool()
  val word = UInt(log2Ceil(RtMemTableParameters.hitRecordStorageWordCount).W)
  val upperHalf = Bool()
  val asEntry = UInt(log2Ceil(RtMemTableParameters.asEntryCount).W)
}

/** A table-owned request payload.  It is a wire into RtLsuMshr, never MSHR state. */
class RtLsuCandidate extends Bundle {
  val source = UInt(RtLsuSource.width.W)
  val cacheLineAddress = UInt(memReqLen.W)
  val write = Bool()
  val is64B = Bool()
  val wordMask = UInt(RtCorePortParameters.lineWordCount.W)
  val writeData = Vec(RtCorePortParameters.lineWordCount, UInt(xLen.W))
  val locator = new RtLsuLocator
}

class RtPdsReadInjection extends Bundle {
  val warpId = UInt(RtCorePortParameters.residentWarpIdWidth.W)
  val field = UInt(log2Ceil(RtMemTableParameters.pdsRayStateFieldCount).W)
  val data = Vec(RtCorePortParameters.lineWordCount, UInt(xLen.W))
  val fault = Bool()
}

class RtPdsWriteInjection extends Bundle {
  val warpId = UInt(RtCorePortParameters.residentWarpIdWidth.W)
  val record = Bool()
  val word = UInt(log2Ceil(RtMemTableParameters.hitRecordStorageWordCount).W)
  val upperHalf = Bool()
  val fault = Bool()
}

class RtAsInjection extends Bundle {
  val entry = UInt(log2Ceil(RtMemTableParameters.asEntryCount).W)
  val data = Vec(RtCorePortParameters.lineWordCount, UInt(xLen.W))
  val fault = Bool()
}

private[rtcore] class RtLsuMshrEntry extends Bundle {
  val valid = Bool()
  // One leader owns the LSU request.  Other entries are read waiters and use
  // this index to share that request's completion.
  val leader = UInt(log2Ceil(RtCorePortParameters.rtLsuMshrEntryCount).W)
  val issued = Bool()
  val source = UInt(RtLsuSource.width.W)
  val lineBase = UInt(memReqLen.W)
  val coverageMask = UInt(RtCorePortParameters.lineWordCount.W)
  val requestMask = UInt(RtCorePortParameters.lineWordCount.W)
  val write = Bool()
  val writeData = Vec(RtCorePortParameters.lineWordCount, UInt(xLen.W))
  val wordOffset = UInt(log2Ceil(RtCorePortParameters.lineWordCount).W)
  val locator = new RtLsuLocator
}

/**
  * Transaction directory between RTcore's three resident tables and RtLsu.
  * A leader represents one externally visible cacheline request; read entries
  * may join an unissued leader on the same cacheline, so a 64B lower/upper
  * pair becomes one 128B request without an intentional dispatch delay.
  */
class RtLsuMshr extends Module {
  private val entryCount = RtCorePortParameters.rtLsuMshrEntryCount
  val io = IO(new Bundle {
    val pdsRead = Flipped(Decoupled(new RtLsuCandidate))
    val pdsWrite = Flipped(Decoupled(new RtLsuCandidate))
    val as = Flipped(Decoupled(new RtLsuCandidate))
    val request = Decoupled(new LsuClientRequest)
    val response = Flipped(Decoupled(new LsuClientResponse))
    val pdsReadInjection = Decoupled(new RtPdsReadInjection)
    val pdsWriteInjection = Decoupled(new RtPdsWriteInjection)
    val asInjection = Decoupled(new RtAsInjection)
  })

  val entries = RegInit(VecInit(Seq.fill(entryCount)(
    0.U.asTypeOf(new RtLsuMshrEntry)
  )))
  val free = VecInit(entries.map(!_.valid)).asUInt
  val hasFree = free.orR
  val freeIndex = PriorityEncoder(free)

  val selectPdsRead = io.pdsRead.valid
  val selectPdsWrite = !selectPdsRead && io.pdsWrite.valid
  val selectAs = !selectPdsRead && !selectPdsWrite && io.as.valid
  val hasCandidate = selectPdsRead || selectPdsWrite || selectAs
  val selected = WireDefault(0.U.asTypeOf(new RtLsuCandidate))
  when(selectPdsRead) {
    selected := io.pdsRead.bits
  }.elsewhen(selectPdsWrite) {
    selected := io.pdsWrite.bits
  }.elsewhen(selectAs) {
    selected := io.as.bits
  }

  val lineBase = Cat(selected.cacheLineAddress(memReqLen - 1, 7), 0.U(7.W))
  val wordOffset = Mux(selected.is64B, selected.cacheLineAddress(6, 2), 0.U)
  val coverageMask = Mux(selected.is64B,
    (selected.wordMask << wordOffset)(RtCorePortParameters.lineWordCount - 1, 0),
    selected.wordMask)

  val selectedWriteData = Wire(Vec(RtCorePortParameters.lineWordCount, UInt(xLen.W)))
  for (lineWord <- 0 until RtCorePortParameters.lineWordCount) {
    selectedWriteData(lineWord) := 0.U
    for (sourceWord <- 0 until RtCorePortParameters.lineWordCount) {
      when((sourceWord.U + wordOffset) === lineWord.U) {
        selectedWriteData(lineWord) := selected.writeData(sourceWord)
      }
    }
  }

  val leaderPending = VecInit(entries.zipWithIndex.map { case (entry, index) =>
    entry.valid && entry.leader === index.U && !entry.issued
  }).asUInt
  val hasPendingLeader = leaderPending.orR
  val pendingLeaderIndex = PriorityEncoder(leaderPending)
  val pendingLeader = entries(pendingLeaderIndex)

  // A read can join only a leader which has not reached LSU yet.  If that
  // leader fires this cycle, it is no longer safe to widen its mask.
  val mergeTargets = VecInit(entries.zipWithIndex.map { case (entry, index) =>
    entry.valid && entry.leader === index.U && !entry.issued && !entry.write &&
      !selected.write && entry.lineBase === lineBase
  }).asUInt
  val hasMergeTarget = mergeTargets.orR
  val mergeTargetIndex = PriorityEncoder(mergeTargets)

  // Writes preserve program order within a cacheline.  Once a read request is
  // issued, a same-mask duplicate is held until its completion because this
  // protocol intentionally has no per-transaction ID on the client boundary.
  val activeSameLine = VecInit(entries.map(entry =>
    entry.valid && entry.lineBase === lineBase
  )).asUInt.orR
  val activeSameLineWrite = VecInit(entries.map(entry =>
    entry.valid && entry.write && entry.lineBase === lineBase
  )).asUInt.orR
  val issuedExactRead = VecInit(entries.zipWithIndex.map { case (entry, index) =>
    entry.valid && entry.leader === index.U && entry.issued && !entry.write &&
      !selected.write && entry.lineBase === lineBase &&
      entry.requestMask === coverageMask
  }).asUInt.orR
  val mergeThisCycle = hasMergeTarget && !io.request.ready
  val candidateBlocked = (selected.write && activeSameLine) ||
    (!selected.write && activeSameLineWrite) || issuedExactRead
  val candidateAccept = hasCandidate && hasFree && !candidateBlocked &&
    (!hasMergeTarget || mergeThisCycle)

  io.pdsRead.ready := selectPdsRead && candidateAccept
  io.pdsWrite.ready := selectPdsWrite && candidateAccept
  io.as.ready := selectAs && candidateAccept
  val candidateFire = io.pdsRead.fire || io.pdsWrite.fire || io.as.fire

  // Existing buffered work takes priority.  Otherwise issue the incoming
  // candidate in the same cycle it is accepted; a back-pressured request is
  // retained as an unissued leader for possible same-line read merging.
  io.request.valid := hasPendingLeader || candidateAccept
  io.request.bits := 0.U.asTypeOf(new LsuClientRequest)
  io.request.bits.clientId := LsuClientId.RtCore
  when(hasPendingLeader) {
    io.request.bits.lineBase := pendingLeader.lineBase
    io.request.bits.coverageMask := pendingLeader.requestMask
    io.request.bits.write := pendingLeader.write
    io.request.bits.writeData := pendingLeader.writeData
  }.otherwise {
    io.request.bits.lineBase := lineBase
    io.request.bits.coverageMask := coverageMask
    io.request.bits.write := selected.write
    io.request.bits.writeData := selectedWriteData
  }

  when(candidateFire) {
    val entry = entries(freeIndex)
    entry.valid := true.B
    entry.leader := Mux(hasMergeTarget, mergeTargetIndex, freeIndex)
    entry.issued := !hasMergeTarget && io.request.fire && !hasPendingLeader
    entry.source := selected.source
    entry.lineBase := lineBase
    entry.coverageMask := coverageMask
    entry.requestMask := coverageMask
    entry.write := selected.write
    entry.writeData := selectedWriteData
    entry.wordOffset := wordOffset
    entry.locator := selected.locator
    when(hasMergeTarget) {
      entries(mergeTargetIndex).requestMask :=
        entries(mergeTargetIndex).requestMask | coverageMask
    }
  }
  when(io.request.fire && hasPendingLeader) {
    entries(pendingLeaderIndex).issued := true.B
  }

  val responseRegValid = RegInit(false.B)
  val responseReg = Reg(new LsuClientResponse)
  val responsePending = RegInit(0.U(entryCount.W))
  val responseMatches = VecInit(entries.zipWithIndex.map { case (entry, index) =>
    entry.valid && entry.leader === index.U && entry.issued &&
      entry.lineBase === io.response.bits.lineBase &&
      entry.requestMask === io.response.bits.coverageMask
  }).asUInt
  io.response.ready := !responseRegValid &&
    io.response.bits.clientId === LsuClientId.RtCore && responseMatches.orR
  when(io.response.fire) {
    responseReg := io.response.bits
    val responseLeader = PriorityEncoder(responseMatches)
    responsePending := VecInit(entries.map(entry =>
      entry.valid && entry.leader === responseLeader
    )).asUInt
    responseRegValid := true.B
  }

  val responseIndex = PriorityEncoder(responsePending)
  val routeEntry = entries(responseIndex)
  val routedData = Wire(Vec(RtCorePortParameters.lineWordCount, UInt(xLen.W)))
  for (targetWord <- 0 until RtCorePortParameters.lineWordCount) {
    routedData(targetWord) := 0.U
    for (lineWord <- 0 until RtCorePortParameters.lineWordCount) {
      when((targetWord.U + routeEntry.wordOffset) === lineWord.U) {
        routedData(targetWord) := responseReg.data(lineWord)
      }
    }
  }
  io.pdsReadInjection.valid := responseRegValid &&
    routeEntry.source === RtLsuSource.pdsRead
  io.pdsReadInjection.bits := 0.U.asTypeOf(new RtPdsReadInjection)
  when(io.pdsReadInjection.valid) {
    io.pdsReadInjection.bits.warpId := routeEntry.locator.warpId
    io.pdsReadInjection.bits.field := routeEntry.locator.field
    io.pdsReadInjection.bits.data := routedData
    io.pdsReadInjection.bits.fault := responseReg.fault
  }
  io.pdsWriteInjection.valid := responseRegValid &&
    routeEntry.source === RtLsuSource.pdsWrite
  io.pdsWriteInjection.bits := 0.U.asTypeOf(new RtPdsWriteInjection)
  when(io.pdsWriteInjection.valid) {
    io.pdsWriteInjection.bits.warpId := routeEntry.locator.warpId
    io.pdsWriteInjection.bits.record := routeEntry.locator.record
    io.pdsWriteInjection.bits.word := routeEntry.locator.word
    io.pdsWriteInjection.bits.upperHalf := routeEntry.locator.upperHalf
    io.pdsWriteInjection.bits.fault := responseReg.fault
  }
  io.asInjection.valid := responseRegValid && routeEntry.source === RtLsuSource.as
  io.asInjection.bits := 0.U.asTypeOf(new RtAsInjection)
  when(io.asInjection.valid) {
    io.asInjection.bits.entry := routeEntry.locator.asEntry
    io.asInjection.bits.data := routedData
    io.asInjection.bits.fault := responseReg.fault
  }

  val injectionFire = io.pdsReadInjection.fire || io.pdsWriteInjection.fire ||
    io.asInjection.fire
  when(injectionFire) {
    entries(responseIndex).valid := false.B
    val remaining = responsePending & (~UIntToOH(responseIndex, entryCount)).asUInt
    responsePending := remaining
    when(remaining === 0.U) {
      responseRegValid := false.B
    }
  }
}

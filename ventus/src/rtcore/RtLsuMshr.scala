package rtcore

import chisel3._
import chisel3.util._
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
  val source = UInt(RtLsuSource.width.W)
  val cacheLineAddress = UInt(memReqLen.W)
  val locator = new RtLsuLocator
}

/**
  * Transaction directory between RTcore's three resident tables and RtLsu.
  * It retains routing metadata only; all request payload comes directly from
  * the selected table on the RtLsuReport handshake.
  */
class RtLsuMshr extends Module {
  private val entryCount = RtCorePortParameters.rtLsuMshrEntryCount
  val io = IO(new Bundle {
    val pdsRead = Flipped(Decoupled(new RtLsuCandidate))
    val pdsWrite = Flipped(Decoupled(new RtLsuCandidate))
    val as = Flipped(Decoupled(new RtLsuCandidate))
    val report = Decoupled(new RtLsuReport)
    val respond = Flipped(Decoupled(new RtLsuRespond))
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

  io.report.valid := hasFree && hasCandidate
  io.report.bits := 0.U.asTypeOf(new RtLsuReport)
  when(io.report.valid) {
    io.report.bits.mshrId := freeIndex
    io.report.bits.cacheLineAddress := selected.cacheLineAddress
    io.report.bits.write := selected.write
    io.report.bits.is64B := selected.is64B
    io.report.bits.wordMask := selected.wordMask
    io.report.bits.writeData := selected.writeData
  }
  io.pdsRead.ready := selectPdsRead && hasFree && io.report.ready
  io.pdsWrite.ready := selectPdsWrite && hasFree && io.report.ready
  io.as.ready := selectAs && hasFree && io.report.ready
  when(io.report.fire) {
    val entry = entries(freeIndex)
    entry.valid := true.B
    entry.source := selected.source
    entry.cacheLineAddress := selected.cacheLineAddress
    entry.locator := selected.locator
  }

  val responseRegValid = RegInit(false.B)
  val responseReg = Reg(new RtLsuRespond)
  val responseIndex = io.respond.bits.mshrId
  val responseEntry = entries(responseIndex)
  io.respond.ready := !responseRegValid && responseEntry.valid
  when(io.respond.fire) {
    assert(!io.respond.bits.fault, "RtLsu fault handling is not enabled")
    responseReg := io.respond.bits
    responseRegValid := true.B
  }

  val routeEntry = entries(responseReg.mshrId)
  io.pdsReadInjection.valid := responseRegValid &&
    routeEntry.source === RtLsuSource.pdsRead
  io.pdsReadInjection.bits := 0.U.asTypeOf(new RtPdsReadInjection)
  when(io.pdsReadInjection.valid) {
    io.pdsReadInjection.bits.warpId := routeEntry.locator.warpId
    io.pdsReadInjection.bits.field := routeEntry.locator.field
    io.pdsReadInjection.bits.data := responseReg.data
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
    io.asInjection.bits.data := responseReg.data
    io.asInjection.bits.fault := responseReg.fault
  }

  val injectionFire = io.pdsReadInjection.fire || io.pdsWriteInjection.fire ||
    io.asInjection.fire
  when(injectionFire) {
    entries(responseReg.mshrId).valid := false.B
    responseRegValid := false.B
  }
}

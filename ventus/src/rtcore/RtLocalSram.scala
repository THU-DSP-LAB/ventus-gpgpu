package rtcore

import chisel3._
import chisel3.util._
import top.parameters._

/** One physical RT Local transaction: the same row is accessed in all 32
  * lane banks, yielding one field-major 128-byte vector. */
class RtLocalSramRequest extends Bundle {
  val warpId = UInt(depth_warp.W)
  val fieldId = UInt(6.W)
  val write = Bool()
  val laneMask = UInt(num_thread.W)
  val writeData = Vec(num_thread, UInt(xLen.W))
}

class RtLocalSramResponse extends Bundle {
  val readData = Vec(num_thread, UInt(xLen.W))
}

/** One lane-word transaction used internally by RTcore.  Software RT-local
  * instructions retain the vector request above; only traversal state uses
  * this narrow path. */
class RtLocalSramNarrowRequest extends Bundle {
  val warpId = UInt(depth_warp.W)
  /* Internal physical row base.  Normal traversal derives it from warpId;
   * group traversal resolves its logical (member,lane) locator through the
   * scheduler-owned member-to-physical-warp map before reaching this port. */
  val rowBase = UInt(log2Ceil(RtLocalSramParameters.bankDepth).W)
  val laneId = UInt(log2Ceil(num_thread).W)
  val fieldId = UInt(6.W)
  val write = Bool()
  val writeData = UInt(xLen.W)
}

class RtLocalSramNarrowResponse extends Bundle {
  val readData = UInt(xLen.W)
}

object RtLocalSramParameters {
  final val warpCount = num_warp
  final val fieldCount = RtAbiLayout.LocalFieldMajorFixedHeaderWordCount
  final val bankDepth = warpCount * fieldCount
  // One bank maps directly to one Box4 IU: lanes i, i+8, i+16 and i+24
  // always use bank i.  This preserves the scheduler's fixed laneId % 8 IU
  // routing and limits the core-side selector to four local lanes.
  final val bankCount = RtCoreSchedulerParameters.boxUnitCount
  final val lanesPerBank = num_thread / bankCount
  final val corePortCount = bankCount
  require(num_thread == 32)
  require(xLen == 32)
  require(bankDepth == 320)
  require(num_thread % bankCount == 0)
}

/** Behavioural replacement for eight single-port 320x128 SRAM macros.
  *
  * Each macro stores four IU-local lanes.  The external ABI remains
  * field-major so vector RT-local shader loads/stores retain one-request
  * behaviour; partial writes use the macro's eight 32-bit write enables.
 */
class RtLocalSram extends Module {
  import RtLocalSramParameters._
  val io = IO(new Bundle {
    // Wide software path.  It is deliberately kept separate from the core
    // path so vector RT local load/store instructions keep their ABI.
    val request = Flipped(Decoupled(new RtLocalSramRequest))
    val response = Decoupled(new RtLocalSramResponse)
    val coreRequest = Flipped(Vec(corePortCount, Decoupled(new RtLocalSramNarrowRequest)))
    val coreResponse = Vec(corePortCount, Decoupled(new RtLocalSramNarrowResponse))
  })

  val banks = Seq.fill(bankCount)(SyncReadMem(bankDepth, Vec(lanesPerBank, UInt(xLen.W))))
  val readPending = RegInit(false.B)
  val narrowReadPending = RegInit(VecInit(Seq.fill(bankCount)(false.B)))
  val narrowReadPort = Reg(Vec(bankCount, UInt(log2Ceil(corePortCount).W)))
  val narrowReadSlot = Reg(Vec(bankCount, UInt(log2Ceil(lanesPerBank).W)))
  val narrowReadAddress = Reg(Vec(bankCount, UInt(log2Ceil(bankDepth).W)))
  val address = io.request.bits.warpId * fieldCount.U + io.request.bits.fieldId
  val wideIdle = !readPending && !narrowReadPending.asUInt.orR
  io.request.ready := wideIdle
  val wideFire = io.request.fire
  val readEnable = wideFire && !io.request.bits.write
  // CIRCT's inferred SRAM invalidates rdata after a disabled read cycle.
  // Keep re-reading the accepted address while its consumer is stalled;
  // simply blocking new requests does not keep a SyncReadMem result valid.
  val responseAddress = RegEnable(address, readEnable)
  val holdRead = readPending && !io.response.ready
  val bankAddress = Mux(readPending, responseAddress, address)
  val bankWrite = !readPending && io.request.bits.write
  val writeEnable = wideFire && io.request.bits.write

  val corePortReadPending = Wire(Vec(corePortCount, Bool()))
  for (port <- 0 until corePortCount) {
    corePortReadPending(port) := VecInit((0 until bankCount).map(bank =>
      narrowReadPending(bank) && narrowReadPort(bank) === port.U)).asUInt.orR
  }
  val coreSelected = Wire(Vec(bankCount, Bool()))
  val coreSelectedPort = Wire(Vec(bankCount, UInt(log2Ceil(corePortCount).W)))
  for (bank <- 0 until bankCount) {
    val candidates = VecInit((0 until corePortCount).map(port =>
      io.coreRequest(port).valid && !corePortReadPending(port) &&
        io.coreRequest(port).bits.laneId(log2Ceil(bankCount) - 1, 0) === bank.U)).asUInt
    coreSelected(bank) := !io.request.valid && !readPending && !narrowReadPending(bank) && candidates.orR
    coreSelectedPort(bank) := PriorityEncoder(candidates)
  }
  for (port <- 0 until corePortCount) {
    val selected = VecInit((0 until bankCount).map(bank =>
      coreSelected(bank) && coreSelectedPort(bank) === port.U)).asUInt.orR
    io.coreRequest(port).ready := selected
    io.coreResponse(port).valid := false.B
    io.coreResponse(port).bits.readData := 0.U
  }

  val readData = Wire(Vec(num_thread, UInt(xLen.W)))
  for (bank <- 0 until bankCount) {
    val writeData = Wire(Vec(lanesPerBank, UInt(xLen.W)))
    val writeMask = Wire(Vec(lanesPerBank, Bool()))
    val selectedPort = coreSelectedPort(bank)
    val selectedRequest = io.coreRequest(selectedPort)
    val selectedSlot = selectedRequest.bits.laneId >> log2Ceil(bankCount)
    val selectedAddress = selectedRequest.bits.rowBase + selectedRequest.bits.fieldId
    for (slot <- 0 until lanesPerBank) {
      val lane = slot * bankCount + bank
      writeData(slot) := Mux(coreSelected(bank), selectedRequest.bits.writeData,
        io.request.bits.writeData(lane))
      writeMask(slot) := Mux(coreSelected(bank),
        selectedRequest.bits.write && selectedSlot === slot.U,
        io.request.bits.laneMask(lane))
    }
    // Keep one explicit read/write port per macro. A masked vector write is
    // still one physical macro operation, rather than a read-modify-write.
    val narrowHold = narrowReadPending(bank) &&
      !io.coreResponse(narrowReadPort(bank)).ready
    val coreRead = coreSelected(bank) && !selectedRequest.bits.write
    val coreWrite = coreSelected(bank) && selectedRequest.bits.write
    val enabled = readEnable || holdRead || narrowHold || coreSelected(bank) ||
      (writeEnable && writeMask.asUInt.orR)
    val accessAddress = Mux(readPending, responseAddress,
      Mux(narrowReadPending(bank), narrowReadAddress(bank),
        Mux(coreSelected(bank), selectedAddress, address)))
    val accessWrite = Mux(readPending || narrowReadPending(bank), false.B,
      Mux(coreSelected(bank), coreWrite, bankWrite))
    val bankRead = banks(bank).readWrite(accessAddress, writeData, writeMask,
      enabled, accessWrite)
    for (slot <- 0 until lanesPerBank) {
      val lane = slot * bankCount + bank
      readData(lane) := bankRead(slot)
    }
  }

  // A SyncReadMem result is exposed directly during the response cycle.  Do
  // not replace the address in that cycle: generated SRAM output can advance
  // before a Decoupled consumer observes the prior data.
  io.response.valid := readPending
  io.response.bits.readData := readData

  for (bank <- 0 until bankCount) {
    val pendingPort = narrowReadPort(bank)
    when(narrowReadPending(bank) && io.coreResponse(pendingPort).ready) {
      narrowReadPending(bank) := false.B
    }
    when(coreSelected(bank) && !io.coreRequest(coreSelectedPort(bank)).bits.write) {
      narrowReadPending(bank) := true.B
      narrowReadPort(bank) := coreSelectedPort(bank)
      narrowReadSlot(bank) := io.coreRequest(coreSelectedPort(bank)).bits.laneId >> log2Ceil(bankCount)
      narrowReadAddress(bank) := io.coreRequest(coreSelectedPort(bank)).bits.rowBase +
        io.coreRequest(coreSelectedPort(bank)).bits.fieldId
    }
  }

  // Reconstruct a pending narrow read from its bank and lane slot.  The
  // assignments above are intentionally overridden here per port so the
  // response does not expose the full 256-bit bank row.
  for (port <- 0 until corePortCount) {
    val responseBank = PriorityEncoder(VecInit((0 until bankCount).map(bank =>
      narrowReadPending(bank) && narrowReadPort(bank) === port.U)).asUInt)
    val responseSlot = narrowReadSlot(responseBank)
    val responseBankData = Wire(Vec(bankCount, UInt(xLen.W)))
    for (bank <- 0 until bankCount) {
      responseBankData(bank) := MuxLookup(responseSlot, readData(bank))(
        (0 until lanesPerBank).map(slot => slot.U -> readData(slot * bankCount + bank)))
    }
    io.coreResponse(port).valid := VecInit((0 until bankCount).map(bank =>
      narrowReadPending(bank) && narrowReadPort(bank) === port.U)).asUInt.orR
    io.coreResponse(port).bits.readData := responseBankData(responseBank)
  }

  when(wideFire) {
    assert(io.request.bits.warpId < warpCount.U, "RT Local warp index out of range")
    assert(io.request.bits.fieldId < fieldCount.U, "RT Local field index out of range")
    when(!io.request.bits.write) {
      readPending := true.B
    }
  }
  when(io.response.fire && !io.request.fire) { readPending := false.B }
}

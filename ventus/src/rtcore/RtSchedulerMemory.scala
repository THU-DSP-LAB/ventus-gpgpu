package rtcore

import chisel3._
import chisel3.util._
import lsu.{LsuClientRequest, LsuClientResponse}

/** Narrow RT Local bank arbitration, independent of traversal state.
  *
  * Priority is spill-parent access, world-context readers, then LocalState.
  * Each bank remembers the accepted read's source until its response is consumed.
  * No queues or pipeline stages are added: this is the original scheduler wiring.
  */
class RtLocalReadArbiter extends Module {
  private val bankCount = RtLocalSramParameters.corePortCount
  private val bankIdWidth = log2Ceil(bankCount)
  val io = IO(new Bundle {
    val parentRequest = Flipped(Decoupled(new RtLocalSramNarrowRequest))
    val parentResponse = Decoupled(new RtLocalSramNarrowResponse)
    val contextRequest = Flipped(Vec(bankCount, Decoupled(new RtLocalSramNarrowRequest)))
    val contextResponse = Vec(bankCount, Decoupled(new RtLocalSramNarrowResponse))
    val localRequest = Flipped(Vec(bankCount, Decoupled(new RtLocalSramNarrowRequest)))
    val localResponse = Vec(bankCount, Decoupled(new RtLocalSramNarrowResponse))
    val bankRequest = Vec(bankCount, Decoupled(new RtLocalSramNarrowRequest))
    val bankResponse = Flipped(Vec(bankCount, Decoupled(new RtLocalSramNarrowResponse)))
    // Only the redirected readers are tracked here; LocalState owns its drain.
    val quiescent = Output(Bool())
  })
  val contextReadOutstanding = RegInit(VecInit(Seq.fill(bankCount)(false.B)))
  val contextReadSource = Reg(Vec(bankCount, UInt(bankIdWidth.W)))
  val contextUse = Wire(Vec(bankCount, Bool()))
  val contextSource = Wire(Vec(bankCount, UInt(bankIdWidth.W)))
  val parentReadOutstanding = RegInit(VecInit(Seq.fill(bankCount)(false.B)))
  val parentUse = Wire(Vec(bankCount, Bool()))
  val parentBank = io.parentRequest.bits.laneId(bankIdWidth - 1, 0)
  io.parentRequest.ready := false.B
  io.parentResponse.valid := false.B
  io.parentResponse.bits := Mux1H((0 until bankCount).map(bank =>
    parentReadOutstanding(bank) -> io.bankResponse(bank).bits))
  for (bank <- 0 until bankCount) {
    parentUse(bank) := io.parentRequest.valid && parentBank === bank.U &&
      !contextReadOutstanding(bank) && !parentReadOutstanding(bank)
    val candidates = VecInit((0 until bankCount).map(source =>
      io.contextRequest(source).valid &&
        io.contextRequest(source).bits.laneId(bankIdWidth - 1, 0) === bank.U)).asUInt
    contextUse(bank) := candidates.orR && !contextReadOutstanding(bank) &&
      !parentReadOutstanding(bank) && !parentUse(bank)
    contextSource(bank) := PriorityEncoder(candidates)
    io.bankRequest(bank).valid := parentUse(bank) || contextUse(bank) || io.localRequest(bank).valid
    io.bankRequest(bank).bits := Mux(parentUse(bank), io.parentRequest.bits,
      Mux(contextUse(bank), io.contextRequest(contextSource(bank)).bits, io.localRequest(bank).bits))
    io.localRequest(bank).ready := io.bankRequest(bank).ready && !contextUse(bank) && !parentUse(bank)
    io.localResponse(bank).valid := io.bankResponse(bank).valid && !contextReadOutstanding(bank) &&
      !parentReadOutstanding(bank)
    io.localResponse(bank).bits := io.bankResponse(bank).bits
    io.bankResponse(bank).ready := Mux(parentReadOutstanding(bank), io.parentResponse.ready,
      Mux(contextReadOutstanding(bank), io.contextResponse(contextReadSource(bank)).ready,
        io.localResponse(bank).ready))
    when(parentUse(bank)) { io.parentRequest.ready := io.bankRequest(bank).ready }
    when(parentReadOutstanding(bank)) { io.parentResponse.valid := io.bankResponse(bank).valid }
    when(io.bankRequest(bank).fire && parentUse(bank) && !io.parentRequest.bits.write) {
      parentReadOutstanding(bank) := true.B
    }
    when(io.bankResponse(bank).fire && parentReadOutstanding(bank)) { parentReadOutstanding(bank) := false.B }
    when(io.bankRequest(bank).fire && contextUse(bank)) {
      contextReadOutstanding(bank) := true.B
      contextReadSource(bank) := contextSource(bank)
    }
    when(io.bankResponse(bank).fire && contextReadOutstanding(bank)) {
      contextReadOutstanding(bank) := false.B
    }
  }
  for (source <- 0 until bankCount) {
    val responseFromBank = VecInit((0 until bankCount).map(bank =>
      contextReadOutstanding(bank) && contextReadSource(bank) === source.U)).asUInt
    io.contextRequest(source).ready := VecInit((0 until bankCount).map(bank =>
      contextUse(bank) && contextSource(bank) === source.U && io.bankRequest(bank).ready)).asUInt.orR
    io.contextResponse(source).valid := VecInit((0 until bankCount).map(bank =>
      responseFromBank(bank) && io.bankResponse(bank).valid)).asUInt.orR
    io.contextResponse(source).bits := Mux1H((0 until bankCount).map(bank =>
      responseFromBank(bank) -> io.bankResponse(bank).bits))
  }

  io.quiescent := !contextReadOutstanding.asUInt.orR && !parentReadOutstanding.asUInt.orR
}

/** Exclusive LSU handoff, not a request queue or tag allocator.
  * AS owns the port normally. A drained table lends it to one spill/restore
  * transaction until its result is consumed. Both clients keep the original
  * handshake timing and tag namespace.
  */
class RtSpillLsuArbiter extends Module {
  val io = IO(new Bundle {
    val begin = Input(Bool())
    val end = Input(Bool())
    val tableQuiescent = Input(Bool())
    val asRequest = Flipped(Decoupled(new LsuClientRequest))
    val asResponse = Decoupled(new LsuClientResponse)
    val spillRequest = Flipped(Decoupled(new LsuClientRequest))
    val spillResponse = Decoupled(new LsuClientResponse)
    val request = Decoupled(new LsuClientRequest)
    val response = Flipped(Decoupled(new LsuClientResponse))
  })
  val spillActive = RegInit(false.B)
  when(io.begin) {
    assert(io.tableQuiescent)
    spillActive := true.B
  }
  when(io.end) { spillActive := false.B }
  io.request.valid := Mux(spillActive, io.spillRequest.valid, io.asRequest.valid)
  io.request.bits := Mux(spillActive, io.spillRequest.bits, io.asRequest.bits)
  io.spillRequest.ready := io.request.ready && spillActive
  io.asRequest.ready := io.request.ready && !spillActive
  io.spillResponse.valid := io.response.valid && spillActive
  io.spillResponse.bits := io.response.bits
  io.asResponse.valid := io.response.valid && !spillActive
  io.asResponse.bits := io.response.bits
  io.response.ready := Mux(spillActive, io.spillResponse.ready, io.asResponse.ready)
}

package rtcore

import chisel3._
import chisel3.util._
import top.parameters._

class RtSpillParentCommand extends Bundle {
  val rowBase = UInt(log2Ceil(RtLocalSramParameters.bankDepth).W)
  val laneId = UInt(log2Ceil(num_thread).W)
  val write = Bool()
  val pointerOnly = Bool()
  val pointer = UInt(64.W)
}

class RtSpillParentResult extends Bundle {
  val pointer = UInt(64.W)
  val callbackDecision = UInt(xLen.W)
  val committedMeta0 = UInt(xLen.W)
  val committedHitT = UInt(xLen.W)
}

/** Parent location is already resolved through LocalState's group locator.
  * Narrow SRAM writes commit at request.fire (there is no write response).
  * Write high then low when installing the driver's fixed parent address.
  * Continuation validity is separate: a nonzero address alone does not mean
  * that a stack has been spilled. Resume must never clear this address. */
class RtSpillParentAccess extends Module {
  val io = IO(new Bundle {
    val command = Flipped(Decoupled(new RtSpillParentCommand))
    val result = Decoupled(new RtSpillParentResult)
    val request = Decoupled(new RtLocalSramNarrowRequest)
    val response = Flipped(Decoupled(new RtLocalSramNarrowResponse))
  })
  val idle :: request :: waitResponse :: complete :: Nil = Enum(4)
  val state = RegInit(idle)
  val saved = Reg(new RtSpillParentCommand)
  val result = Reg(new RtSpillParentResult)
  val field = Reg(UInt(3.W))
  val fields = VecInit(Seq(
    RtAbiLayout.SlotReservedLo,
    RtAbiLayout.SlotReservedHi,
    RtAbiLayout.LocalControlBaseBytes / RtAbiLayout.AbiWordBytes,
    RtAbiLayout.LocalCommittedHitRecordBaseBytes / RtAbiLayout.AbiWordBytes + RtAbiLayout.HitRecordMeta0,
    RtAbiLayout.LocalCommittedHitRecordBaseBytes / RtAbiLayout.AbiWordBytes + RtAbiLayout.HitRecordHitT
  ).map(_.U(6.W)))

  io.command.ready := state === idle
  when(io.command.fire) {
    when(io.command.bits.write) { assert(io.command.bits.pointer =/= 0.U) }
    saved := io.command.bits
    result := 0.U.asTypeOf(new RtSpillParentResult)
    when(io.command.bits.write) { result.pointer := io.command.bits.pointer }
    field := 0.U
    state := request
  }
  io.request.valid := state === request
  io.request.bits := 0.U.asTypeOf(new RtLocalSramNarrowRequest)
  io.request.bits.rowBase := saved.rowBase
  io.request.bits.laneId := saved.laneId
  io.request.bits.write := saved.write
  io.request.bits.fieldId := Mux(saved.write,
    Mux(field === 0.U, RtAbiLayout.SlotReservedHi.U, RtAbiLayout.SlotReservedLo.U), fields(field))
  io.request.bits.writeData := Mux(field === 0.U, saved.pointer(63, 32), saved.pointer(31, 0))
  when(io.request.fire) {
    when(saved.write) {
      when(field === 1.U) { state := complete }
        .otherwise { field := 1.U }
    }.otherwise { state := waitResponse }
  }
  io.response.ready := state === waitResponse
  when(io.response.fire) {
    switch(field) {
      is(0.U) { result.pointer := Cat(0.U(32.W), io.response.bits.readData) }
      is(1.U) { result.pointer := Cat(io.response.bits.readData, result.pointer(31, 0)) }
      is(2.U) { result.callbackDecision := io.response.bits.readData }
      is(3.U) { result.committedMeta0 := io.response.bits.readData }
      is(4.U) { result.committedHitT := io.response.bits.readData }
    }
    val noContinuation = field === 1.U && result.pointer(31, 0) === 0.U &&
      io.response.bits.readData === 0.U
    when(field === 4.U || noContinuation || (field === 1.U && saved.pointerOnly)) { state := complete }
      .otherwise { field := field + 1.U; state := request }
  }
  io.result.valid := state === complete
  io.result.bits := result
  when(io.result.fire) { state := idle }
}

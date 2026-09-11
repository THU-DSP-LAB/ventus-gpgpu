package rtcore

import chisel3._
import chisel3.util._
import lsu.{LsuClientId, LsuClientRequest, LsuClientResponse}
import top.parameters._

object RtLsuSource { val as = 0.U(1.W) }
class RtLsuLocator extends Bundle { val tableLocation = UInt(6.W) }
class RtLsuCandidate extends Bundle {
  val source = UInt(1.W); val cacheLineAddress = UInt(memReqLen.W); val write = Bool(); val is64B = Bool()
  val wordMask = UInt(RtCorePortParameters.lineWordCount.W); val writeData = Vec(RtCorePortParameters.lineWordCount, UInt(xLen.W)); val locator = new RtLsuLocator
}
class RtAsInjection extends Bundle { val entry = UInt(log2Ceil(RtMemTableParameters.asEntryCount).W); val data = Vec(RtCorePortParameters.lineWordCount, UInt(xLen.W)); val fault = Bool() }

/** Single outstanding AS cacheline transaction.  PDS traffic deliberately has
  * no representation here: RT Local SRAM owns ray state and hit records. */
class RtLsuMshr extends Module {
  val io = IO(new Bundle {
    val as = Flipped(Decoupled(new RtLsuCandidate))
    val request = Decoupled(new LsuClientRequest)
    val response = Flipped(Decoupled(new LsuClientResponse))
    val asInjection = Decoupled(new RtAsInjection)
    val idle = Output(Bool())
  })
  val pending = RegInit(false.B); val issued = RegInit(false.B)
  val requestBits = Reg(new RtLsuCandidate)
  val responseValid = RegInit(false.B); val responseBits = Reg(new LsuClientResponse)

  io.as.ready := !pending && !responseValid
  when(io.as.fire) { pending := true.B; issued := false.B; requestBits := io.as.bits }
  io.request.valid := pending && !issued
  io.request.bits := 0.U.asTypeOf(new LsuClientRequest)
  io.request.bits.clientId := LsuClientId.RtCore
  io.request.bits.lineBase := Cat(requestBits.cacheLineAddress(memReqLen - 1, 7), 0.U(7.W))
  io.request.bits.coverageMask := Mux(requestBits.is64B,
    (requestBits.wordMask << requestBits.cacheLineAddress(6, 2))(RtCorePortParameters.lineWordCount - 1, 0), requestBits.wordMask)
  io.request.bits.write := requestBits.write; io.request.bits.writeData := requestBits.writeData
  when(io.request.fire) { issued := true.B }

  io.response.ready := pending && issued && !responseValid
  when(io.response.fire) { responseBits := io.response.bits; responseValid := true.B }
  io.asInjection.valid := responseValid
  io.asInjection.bits.entry := requestBits.locator.tableLocation
  // LSU responses are a complete 128-byte cache line.  A Triangle/AABB
  // request is a 64-byte half-line, for which `coverageMask` is already
  // shifted above.  Normalize that selected half back to word zero before it
  // reaches the AS table: all node consumers index their payload relative to
  // the requested node, not to the containing cache line.
  val responseWordOffset = Mux(requestBits.is64B,
    requestBits.cacheLineAddress(6, 2), 0.U(5.W))
  for (word <- 0 until RtCorePortParameters.lineWordCount) {
    val source = (word.U(5.W) + responseWordOffset)(4, 0)
    io.asInjection.bits.data(word) := Mux(requestBits.is64B && word.U >= 16.U,
      0.U, responseBits.data(source))
  }
  io.asInjection.bits.fault := responseBits.fault
  when(io.asInjection.fire) { pending := false.B; issued := false.B; responseValid := false.B }
  io.idle := !pending && !responseValid
}

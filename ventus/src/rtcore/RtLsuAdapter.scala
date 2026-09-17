package rtcore

import chisel3._
import chisel3.util._
import lsu.{LsuClientId, LsuClientRequest, LsuClientResponse}
import top.parameters._

/** Read-only AS fetch. The shared LSU interface still supports other clients' writes. */
class RtLsuCandidate extends Bundle {
  val entryId = UInt(log2Ceil(RtMemTableParameters.asEntryCount).W)
  val cacheLineAddress = UInt(memReqLen.W)
  val is64B = Bool()
  val wordMask = UInt(RtCorePortParameters.lineWordCount.W)
}
class RtAsInjection extends Bundle { val entry = UInt(log2Ceil(RtMemTableParameters.asEntryCount).W); val data = Vec(RtCorePortParameters.lineWordCount, UInt(xLen.W)); val fault = Bool() }

/** Stateless AS/LSU format adapter. Shared LSU MSHRs own in-flight transactions;
  * the opaque client tag identifies the protected AS entry on completion. */
class RtLsuAdapter extends Module {
  val io = IO(new Bundle {
    val as = Flipped(Decoupled(new RtLsuCandidate))
    val request = Decoupled(new LsuClientRequest)
    val response = Flipped(Decoupled(new LsuClientResponse))
    val asInjection = Decoupled(new RtAsInjection)
  })
  require(log2Ceil(RtMemTableParameters.asEntryCount) <= lsu.LsuClientTag.width)
  val requestBits = io.as.bits
  io.as.ready := io.request.ready
  io.request.valid := io.as.valid
  io.request.bits := 0.U.asTypeOf(new LsuClientRequest)
  io.request.bits.clientId := LsuClientId.RtCore
  io.request.bits.clientTag := requestBits.entryId
  io.request.bits.lineBase := Cat(requestBits.cacheLineAddress(memReqLen - 1, 7), 0.U(7.W))
  io.request.bits.coverageMask := Mux(requestBits.is64B,
    (requestBits.wordMask << requestBits.cacheLineAddress(6, 2))(RtCorePortParameters.lineWordCount - 1, 0), requestBits.wordMask)
  // write/writeData remain zero from the default assignment: AS traffic is read-only.
  io.response.ready := io.asInjection.ready
  io.asInjection.valid := io.response.valid
  io.asInjection.bits.entry := io.response.bits.clientTag
  io.asInjection.bits.data := io.response.bits.data
  io.asInjection.bits.fault := io.response.bits.fault
  when(io.response.valid) { assert(io.response.bits.clientId === LsuClientId.RtCore) }
}

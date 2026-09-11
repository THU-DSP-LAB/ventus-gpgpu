package rtcore

import chisel3._
import chisel3.util._
import top.parameters._

/** Admission reservation held from pre-operand issue through release ack.
  * Gating at the scheduler alone lets a foreign warp block the shared FIFO.
  */
class RtSessionReservation extends Module {
  val io = IO(new Bundle {
    val issued = Flipped(Valid(UInt(depth_warp.W)))
    val releaseIssued = Flipped(Valid(UInt(depth_warp.W)))
    val released = Flipped(Valid(UInt(depth_warp.W)))
    val allowed = Output(Vec(num_warp, Bool()))
    val reserved = Output(Bool())
    val owner = Output(UInt(depth_warp.W))
  })
  val reserved = RegInit(false.B)
  val owner = Reg(UInt(depth_warp.W))
  val closing = RegInit(false.B)
  io.reserved := reserved
  io.owner := owner
  for (i <- 0 until num_warp) io.allowed(i) := !closing && (!reserved || owner === i.U)
  when(io.issued.valid) {
    assert(!closing, "a new traversal must wait for the previous release acknowledgement")
    assert(!reserved || owner === io.issued.bits,
      "only the reserved warp may issue a retained RT traversal")
    reserved := true.B
    owner := io.issued.bits
  }
  when(io.releaseIssued.valid) {
    assert(reserved && owner === io.releaseIssued.bits && !closing,
      "only the reserved warp may issue its final RT release")
    closing := true.B
  }
  when(io.released.valid) {
    assert(reserved && owner === io.released.bits,
      "RT release acknowledgement must retire the reserved warp")
    assert(!io.issued.valid, "release and nested traversal cannot issue together")
    reserved := false.B
    closing := false.B
  }
}

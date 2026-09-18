package rtcore

import chisel3._
import chisel3.util._
import top.parameters._

/** Admission reservation held until context-releasing completion writeback,
  * or software release ack for a retained candidate context.
  *
  * A traverse has no final SIMT lane mask at this point. A newly admitted
  * traversal is therefore provisional until RT issue: an empty one is
  * cancelled, while a non-empty one becomes the session owner. This prevents
  * another warp from entering operand collection in the intervening cycles.
  */
class RtSessionReservation extends Module {
  val io = IO(new Bundle {
    val issued = Flipped(Valid(UInt(depth_warp.W)))
    val traverseResolved = Flipped(Valid(new Bundle {
      val warpId = UInt(depth_warp.W)
      val wasEmpty = Bool()
    }))
    val releaseIssued = Flipped(Valid(UInt(depth_warp.W)))
    val released = Flipped(Valid(UInt(depth_warp.W)))
    val completed = Flipped(Valid(new Bundle {
      val warpId = UInt(depth_warp.W)
      val contextReleased = Bool()
    }))
    val allowed = Output(Vec(num_warp, Bool()))
    val reserved = Output(Bool())
    val owner = Output(UInt(depth_warp.W))
  })
  val reserved = RegInit(false.B)
  // `owner` is compared by the admission guard.  Although `reserved` masks
  // that comparison before the first claim, give the externally visible
  // ownership state a defined reset value as well: simulation must not rely
  // on the elaboration-dependent initial value of a control register.
  val owner = RegInit(0.U(depth_warp.W))
  val closing = RegInit(false.B)
  val provisional = RegInit(false.B)
  val traversing = RegInit(false.B)
  io.reserved := reserved
  io.owner := owner
  for (i <- 0 until num_warp) {
    io.allowed(i) := !closing && !provisional && !traversing && (!reserved || owner === i.U)
  }
  when(io.issued.valid) {
    assert(!traversing, "a traversal must wait for the previous completion writeback")
    assert(!closing, "a new traversal must wait for the previous release acknowledgement")
    assert(!reserved || owner === io.issued.bits,
      "only the reserved warp may issue a retained RT traversal")
    traversing := true.B
    when(!reserved) {
      reserved := true.B
      owner := io.issued.bits
      provisional := true.B
    }
  }
  when(io.traverseResolved.valid) {
    assert(reserved && owner === io.traverseResolved.bits.warpId,
      "only the reserved warp may resolve an RT traversal")
    when(io.traverseResolved.bits.wasEmpty) { traversing := false.B }
    // A same-owner resume/nested traverse resolves while the session is
    // already established. Only the first, provisional admission may cancel
    // its reservation on an empty effective lane mask.
    when(provisional) {
      provisional := false.B
      when(io.traverseResolved.bits.wasEmpty) {
        reserved := false.B
      }
    }
  }
  when(io.completed.valid) {
    assert(reserved && traversing && !provisional && !closing &&
      owner === io.completed.bits.warpId,
      "completion writeback must belong to the executing RT owner")
    assert(!io.issued.valid, "completion and new traversal admission must not overlap")
    traversing := false.B
    when(io.completed.bits.contextReleased) { reserved := false.B }
  }
  when(io.releaseIssued.valid) {
    assert(reserved && owner === io.releaseIssued.bits && !closing && !provisional && !traversing,
      "only the reserved warp may issue its final RT release")
    closing := true.B
  }
  when(io.released.valid) {
    assert(reserved && owner === io.released.bits,
      "RT release acknowledgement must retire the reserved warp")
    assert(!io.issued.valid, "release and nested traversal cannot issue together")
    reserved := false.B
    closing := false.B
    provisional := false.B
    traversing := false.B
  }
}

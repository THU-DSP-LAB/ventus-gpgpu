package rtcore

import chisel3._
import chisel3.util._
import top.parameters._

class RtSpillParentLocation extends Bundle {
  val rowBase = UInt(log2Ceil(RtLocalSramParameters.bankDepth).W)
  val laneId = UInt(log2Ceil(num_thread).W)
}

class RtSpillValidityUpdate extends Bundle {
  val parent = new RtSpillParentLocation
  val suspended = Bool()
}

/** Driver assigns one immutable slot to each persistent parent warp/lane.
  * Address materialisation uses that software layout, not allocation state.
  * Only 256 continuation-valid bits survive physical scheduler release.
  * Pointer words remain assigned across resume; they are NOT valid flags. */
class RtSpillBindings extends Module {
  require(num_warp * num_thread == RtSpillSlotLayout.slotsPerCore)
  val io = IO(new Bundle {
    val descriptor = Input(new RtSpillPoolDescriptor)
    val coreId = Input(UInt(32.W))
    val query = Flipped(Valid(new RtSpillParentLocation))
    val pointer = Output(UInt(64.W))
    val suspended = Output(Bool())
    val update = Flipped(Valid(new RtSpillValidityUpdate))
  })
  val epoch = RegInit(0.U(32.W))
  val valid = RegInit(VecInit(Seq.fill(RtSpillSlotLayout.slotsPerCore)(false.B)))
  val epochChanged = epoch =/= io.descriptor.dispatchEpoch
  val configured = io.descriptor.dispatchEpoch =/= 0.U &&
    io.descriptor.spillPoolBase =/= 0.U
  def parentIndex(parent: RtSpillParentLocation): (UInt, Bool) = {
    // Match the persistent row explicitly; never use the proxy execution
    // warp/lane, and avoid a general hardware divider by the 40-word row.
    val rows = VecInit((0 until num_warp).map(warp =>
      parent.rowBase === (warp * RtLocalSramParameters.fieldCount).U)).asUInt
    (Cat(PriorityEncoder(rows), parent.laneId), rows.orR)
  }
  val (queryIndex, queryMapped) = parentIndex(io.query.bits)
  val queryAssigned = configured && queryMapped && queryIndex < io.descriptor.slotsPerCore
  val partition = io.descriptor.spillPoolBase +& io.coreId * io.descriptor.perCoreStride
  io.pointer := Mux(queryAssigned,
    partition +& queryIndex * RtSpillSlotLayout.slotBytes.U, 0.U)
  io.suspended := io.query.valid && queryAssigned && !epochChanged && valid(queryIndex)
  when(io.query.valid) { assert(queryMapped, "spill lookup must address a persistent parent row") }
  when(epochChanged) {
    epoch := io.descriptor.dispatchEpoch
    valid.foreach(_ := false.B)
  }
  val (updateIndex, updateMapped) = parentIndex(io.update.bits.parent)
  when(io.update.valid) {
    assert(configured && !epochChanged && updateMapped && updateIndex < io.descriptor.slotsPerCore)
    assert(valid(updateIndex) =/= io.update.bits.suspended,
      "publish only after spill, consume each continuation exactly once")
    valid(updateIndex) := io.update.bits.suspended
  }
}

package rtcore

import chisel3._
import chisel3.util._
import lsu.{LsuClientId, LsuClientRequest, LsuClientResponse, LsuClientTag}
import top.parameters._

/** Dispatch-only descriptor; addresses are never part of shader metadata. */
class RtSpillPoolDescriptor extends Bundle {
  val spillPoolBase = UInt(memReqLen.W)
  val slotsPerCore = UInt(32.W)
  val perCoreStride = UInt(32.W)
  val dispatchEpoch = UInt(32.W)
}

/** Driver-private, fixed-size continuation image; not shader payload/PDS. */
object RtSpillSlotLayout {
  final val pageBytes = 128
  final val wordsPerPage = 32
  final val slotBytes = 384
  final val slotsPerCore = 256
  final val spillBytesPerCore = slotsPerCore * slotBytes
  final val poolBytesPerCore = spillBytesPerCore

  final val page0Offset = 0x000
  final val page1Offset = 0x080
  final val page2Offset = 0x100
  final val baseObjectTagsWord = 0
  final val overflowObjectTagsWord = 1
  final val stackDepthWord = 2
  final val validWord = 3
  final val instanceNodeRefWord = 4
}

class RtSpillSlotImage extends Bundle {
  val baseNodeRef = Vec(RtSpillSlotLayout.wordsPerPage, UInt(32.W))
  val overflowNodeRef = Vec(RtSpillSlotLayout.wordsPerPage, UInt(32.W))
  val baseObjectTags = UInt(32.W)
  val overflowObjectTags = UInt(32.W)
  val stackDepth = UInt(32.W)
  val valid = Bool()
  // InvalidNodeRef denotes no enclosing Instance.
  val instanceNodeRef = UInt(32.W)
}

/** Separate encode/decode ports model independent global write/read data. */
class RtSpillSlotCodec extends Module {
  import RtSpillSlotLayout._
  val io = IO(new Bundle {
    val image = Input(new RtSpillSlotImage)
    val encoded = Output(Vec(3, Vec(wordsPerPage, UInt(32.W))))
    val readPages = Input(Vec(3, Vec(wordsPerPage, UInt(32.W))))
    val decoded = Output(new RtSpillSlotImage)
  })

  io.encoded(0) := io.image.baseNodeRef
  io.encoded(1) := VecInit(Seq.fill(wordsPerPage)(0.U(32.W)))
  io.encoded(1)(baseObjectTagsWord) := io.image.baseObjectTags
  io.encoded(1)(overflowObjectTagsWord) := io.image.overflowObjectTags
  io.encoded(1)(stackDepthWord) := io.image.stackDepth
  io.encoded(1)(validWord) := io.image.valid
  io.encoded(1)(instanceNodeRefWord) := io.image.instanceNodeRef
  io.encoded(2) := io.image.overflowNodeRef

  io.decoded.baseNodeRef := io.readPages(0)
  io.decoded.overflowNodeRef := io.readPages(2)
  io.decoded.baseObjectTags := io.readPages(1)(baseObjectTagsWord)
  io.decoded.overflowObjectTags := io.readPages(1)(overflowObjectTagsWord)
  io.decoded.stackDepth := io.readPages(1)(stackDepthWord)
  io.decoded.valid := io.readPages(1)(validWord)(0)
  io.decoded.instanceNodeRef := io.readPages(1)(instanceNodeRefWord)
}

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

class RtSpillTransferControl extends Bundle {
  val pointer = UInt(64.W)
  val restore = Bool()
  val terminate = Bool()
}

class RtSpillTransferCommand extends RtSpillTransferControl {
  val image = new RtSpillSlotImage
}

/** Fixed-page memory transfer, not logical lane release. After a write result
  * the scheduler must mark the persistent parent's continuation valid before
  * candidate completion; after restore it consumes that validity, while the
  * driver's assigned pointer remains unchanged for the next spill. */
class RtSpillTransfer extends Module {
  val io = IO(new Bundle {
    val command = Flipped(Decoupled(new RtSpillTransferCommand))
    val result = Decoupled(new RtSpillTransferCommand)
    val request = Decoupled(new LsuClientRequest)
    val response = Flipped(Decoupled(new LsuClientResponse))
  })
  val idle :: request :: waitResponse :: complete :: Nil = Enum(4)
  val state = RegInit(idle)
  val saved = Reg(new RtSpillTransferControl)
  // Spill and restore are mutually exclusive transactions. Keep their image
  // in one page buffer, not both saved.image and a separate restore buffer.
  val pages = Reg(Vec(3, Vec(32, UInt(32.W))))
  val page = Reg(UInt(2.W))
  val codec = Module(new RtSpillSlotCodec)
  codec.io.image := io.command.bits.image
  codec.io.readPages := pages

  io.command.ready := state === idle
  when(io.command.fire) {
    assert(io.command.bits.pointer =/= 0.U)
    assert(io.command.bits.pointer(6, 0) === 0.U)
    assert(!io.command.bits.terminate || io.command.bits.restore)
    when(!io.command.bits.restore) {
      assert(io.command.bits.image.valid, "spill must snapshot a live traversal stack")
    }
    if (memReqLen < 64)
      assert(io.command.bits.pointer(63, memReqLen) === 0.U)
    saved.pointer := io.command.bits.pointer
    saved.restore := io.command.bits.restore
    saved.terminate := io.command.bits.terminate
    pages := Mux(io.command.bits.restore, 0.U.asTypeOf(pages), codec.io.encoded)
    page := 0.U
    state := Mux(io.command.bits.terminate, complete, request)
  }

  io.request.valid := state === request
  io.request.bits := 0.U.asTypeOf(new LsuClientRequest)
  io.request.bits.clientId := LsuClientId.RtCore
  io.request.bits.clientTag := 30.U(LsuClientTag.width.W)
  io.request.bits.lineBase := saved.pointer + (page << 7)
  io.request.bits.coverageMask := Fill(dcache_BlockWords, 1.U(1.W))
  io.request.bits.write := !saved.restore
  io.request.bits.writeData := pages(page)
  when(io.request.fire) { state := waitResponse }
  io.response.ready := state === waitResponse
  when(io.response.fire) {
    assert(io.response.bits.clientId === LsuClientId.RtCore)
    assert(io.response.bits.clientTag === 30.U)
    assert(io.response.bits.lineBase === saved.pointer + (page << 7),
      "spill response must match the outstanding page address")
    assert(!io.response.bits.fault, "driver-private spill slot memory fault")
    when(saved.restore) { pages(page) := io.response.bits.data }
    val baseOnly = saved.restore && page === 1.U &&
      io.response.bits.data(RtSpillSlotLayout.stackDepthWord) <= 32.U
    when(saved.restore && page === 1.U) {
      when(!io.response.bits.data(RtSpillSlotLayout.validWord)(0)) {
        printf(p"[RT spill invariant] invalid restore ptr=0x${Hexadecimal(saved.pointer)}" +
          p" response=0x${Hexadecimal(io.response.bits.lineBase)}" +
          p" tags=0x${Hexadecimal(io.response.bits.data(0))}" +
          p" overflowTags=0x${Hexadecimal(io.response.bits.data(1))}" +
          p" depth=0x${Hexadecimal(io.response.bits.data(2))}" +
          p" valid=0x${Hexadecimal(io.response.bits.data(3))}" +
          p" instance=0x${Hexadecimal(io.response.bits.data(4))}\n")
      }
      assert(io.response.bits.data(RtSpillSlotLayout.validWord)(0),
        "a published spill pointer must name a valid stack image")
      assert(io.response.bits.data(RtSpillSlotLayout.stackDepthWord) <= 64.U)
    }
    when(page === 2.U || baseOnly) { state := complete }
      .otherwise { page := page + 1.U; state := request }
  }

  io.result.valid := state === complete
  io.result.bits.pointer := saved.pointer
  io.result.bits.restore := saved.restore
  io.result.bits.terminate := saved.terminate
  io.result.bits.image := codec.io.decoded
  when(io.result.fire) { state := idle }
}

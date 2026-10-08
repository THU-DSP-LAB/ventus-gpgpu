package rtcore

import chisel3._
import chisel3.util._
import lsu.{LsuClientId, LsuClientRequest, LsuClientResponse, LsuClientTag}
import top.parameters._

class RtSpillTransferCommand extends Bundle {
  val pointer = UInt(64.W)
  val restore = Bool()
  val terminate = Bool()
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
  val saved = Reg(new RtSpillTransferCommand)
  val pages = Reg(Vec(3, Vec(32, UInt(32.W))))
  val page = Reg(UInt(2.W))
  val codec = Module(new RtSpillSlotCodec)
  codec.io.image := saved.image
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
    saved := io.command.bits
    pages := 0.U.asTypeOf(pages)
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
  io.request.bits.writeData := codec.io.encoded(page)
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
  io.result.bits := saved
  when(saved.restore) { io.result.bits.image := codec.io.decoded }
  when(io.result.fire) { state := idle }
}

package rtcore

import chisel3._
import top.parameters.memReqLen

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

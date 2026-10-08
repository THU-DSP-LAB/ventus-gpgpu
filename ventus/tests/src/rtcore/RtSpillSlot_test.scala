package rtcore

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class RtSpillSlotTest extends AnyFreeSpec with ChiselScalatestTester {
  "slot and partition sizes stay fixed independently of stack occupancy" in {
    assert(RtSpillSlotLayout.slotBytes == 384)
    assert(RtSpillSlotLayout.spillBytesPerCore == 96 * 1024)
    assert(RtSpillSlotLayout.poolBytesPerCore == 96 * 1024)
  }

  "all refs, tags and metadata encode and decode independently" in {
    test(new RtSpillSlotCodec).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      import RtSpillSlotLayout._
      // Read pages intentionally differ from the encoded image: restoration
      // must consume memory responses, not stale snapshot registers.
      for (i <- 0 until wordsPerPage) {
        dut.io.image.baseNodeRef(i).poke((0x1000 + i).U)
        dut.io.image.overflowNodeRef(i).poke((0x2000 + i).U)
        dut.io.readPages(0)(i).poke((0x3000 + i).U)
        dut.io.readPages(1)(i).poke(0.U)
        dut.io.readPages(2)(i).poke((0x4000 + i).U)
      }
      dut.io.image.baseObjectTags.poke("h80000021".U)
      dut.io.image.overflowObjectTags.poke("h00008004".U)
      dut.io.image.stackDepth.poke(49.U)
      dut.io.image.valid.poke(true.B)
      dut.io.image.instanceNodeRef.poke("h12345678".U)
      dut.io.readPages(1)(baseObjectTagsWord).poke("h40000012".U)
      dut.io.readPages(1)(overflowObjectTagsWord).poke("h80000001".U)
      dut.io.readPages(1)(stackDepthWord).poke(64.U)
      dut.io.readPages(1)(validWord).poke(1.U)
      dut.io.readPages(1)(instanceNodeRefWord).poke("h76543210".U)
      dut.clock.step()
      for (i <- 0 until wordsPerPage) {
        dut.io.encoded(0)(i).expect((0x1000 + i).U)
        dut.io.encoded(2)(i).expect((0x2000 + i).U)
        dut.io.decoded.baseNodeRef(i).expect((0x3000 + i).U)
        dut.io.decoded.overflowNodeRef(i).expect((0x4000 + i).U)
      }
      dut.io.encoded(1)(baseObjectTagsWord).expect("h80000021".U)
      dut.io.encoded(1)(overflowObjectTagsWord).expect("h00008004".U)
      dut.io.encoded(1)(stackDepthWord).expect(49.U)
      dut.io.encoded(1)(validWord).expect(1.U)
      dut.io.encoded(1)(instanceNodeRefWord).expect("h12345678".U)
      dut.io.encoded(1).drop(instanceNodeRefWord + 1).foreach(_.expect(0.U))
      dut.io.decoded.baseObjectTags.expect("h40000012".U)
      dut.io.decoded.overflowObjectTags.expect("h80000001".U)
      dut.io.decoded.stackDepth.expect(64.U)
      dut.io.decoded.valid.expect(true.B)
      dut.io.decoded.instanceNodeRef.expect("h76543210".U)
    }
  }

  "base-only and empty continuations do not require overflow contents" in {
    test(new RtSpillSlotCodec).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      import RtSpillSlotLayout._
      dut.io.image.baseNodeRef.foreach(_.poke(0.U))
      dut.io.image.overflowNodeRef.foreach(_.poke(0.U))
      dut.io.image.baseObjectTags.poke(0.U)
      dut.io.image.overflowObjectTags.poke(0.U)
      dut.io.image.stackDepth.poke(0.U)
      dut.io.image.valid.poke(false.B)
      dut.io.image.instanceNodeRef.poke(RtVtasLayout.InvalidNodeRef.U)
      dut.io.readPages.foreach(_.foreach(_.poke(0.U)))
      dut.io.readPages(1)(instanceNodeRefWord).poke(RtVtasLayout.InvalidNodeRef.U)
      for (depth <- Seq(0, 1, 32)) {
        dut.io.image.stackDepth.poke(depth.U)
        dut.io.image.valid.poke(true.B)
        dut.io.readPages(1)(stackDepthWord).poke(depth.U)
        dut.io.readPages(1)(validWord).poke(1.U)
        dut.clock.step()
        dut.io.encoded(1)(stackDepthWord).expect(depth.U)
        dut.io.encoded(2).foreach(_.expect(0.U))
        dut.io.decoded.stackDepth.expect(depth.U)
        dut.io.decoded.instanceNodeRef.expect(RtVtasLayout.InvalidNodeRef.U)
      }
      dut.io.readPages(1)(validWord).poke(0.U)
      dut.io.decoded.valid.expect(false.B)
    }
  }
}

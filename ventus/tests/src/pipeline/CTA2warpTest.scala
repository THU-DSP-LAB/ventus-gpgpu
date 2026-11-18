package pipeline

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import top.parameters._

class CTA2warpTest extends AnyFreeSpec with ChiselScalatestTester {

  "CTA2warp preserves completed warp tag in FIFO across new allocations during backpressure" in {
    test(new CTA2warp) { dut =>
      dut.reset.poke(true.B)
      dut.clock.step(2)
      dut.reset.poke(false.B)

      // Initialize IO
      dut.io.CTAreq.valid.poke(false.B)
      dut.io.CTArsp.ready.poke(false.B)
      dut.io.warpRsp.valid.poke(false.B)
      dut.io.warpReq.ready.poke(true.B)
      dut.io.wg_id_lookup.poke(0.U)

      dut.clock.step(1)
      assert(dut.io.CTAreq.ready.peek().litToBoolean, "CTAreq should be ready when all warps are idle")

      // Step 1: Dispatch Warp 0 with Tag 0x15 (21, fits in 6 bits)
      val tagA = 0x15
      dut.io.CTAreq.valid.poke(true.B)
      dut.io.CTAreq.bits.dispatch2cu_wf_tag_dispatch.poke(tagA.U)
      dut.clock.step(1)
      dut.io.CTAreq.valid.poke(false.B)

      // Verify Warp 0 lookup
      dut.io.wg_id_lookup.poke(0.U)
      assert(dut.io.wg_id_tag.peek().litValue == tagA, "Lookup for warp 0 must return tagA")

      // Step 2: Warp 0 completes execution, warpRsp fires with wid = 0
      // But CTArsp.ready is held LOW (dispatcher backpressure)
      dut.io.CTArsp.ready.poke(false.B)
      dut.io.warpRsp.valid.poke(true.B)
      dut.io.warpRsp.bits.wid.poke(0.U)
      dut.clock.step(1)
      dut.io.warpRsp.valid.poke(false.B)

      // Step 3: Now warp 0 has been freed in idx_using.
      // Dispatch a NEW CTA which re-allocates warp 0 with Tag 0x2A (42, fits in 6 bits)
      val tagB = 0x2a
      dut.io.CTAreq.valid.poke(true.B)
      dut.io.CTAreq.bits.dispatch2cu_wf_tag_dispatch.poke(tagB.U)
      dut.clock.step(1)
      dut.io.CTAreq.valid.poke(false.B)

      // Live table now holds tagB for warp 0
      dut.io.wg_id_lookup.poke(0.U)
      assert(dut.io.wg_id_tag.peek().litValue == tagB, "Lookup for newly dispatched warp 0 must return tagB")

      // Step 4: Now release dispatcher backpressure (CTArsp.ready := true)
      // The queued response MUST carry tagA (the completed warp), NOT tagB!
      dut.io.CTArsp.ready.poke(true.B)
      assert(dut.io.CTArsp.valid.peek().litToBoolean, "CTArsp must be valid for the queued completion")
      val doneTag1 = dut.io.CTArsp.bits.cu2dispatch_wf_tag_done.peek().litValue
      assert(doneTag1 == tagA, s"First completion must report tagA ($tagA), but got $doneTag1")

      dut.clock.step(1)

      // Queue is now empty
      assert(!dut.io.CTArsp.valid.peek().litToBoolean, "CTArsp must be empty after popping tagA")

      // Step 5: Second warp completes with wid = 0
      dut.io.warpRsp.valid.poke(true.B)
      dut.io.warpRsp.bits.wid.poke(0.U)
      dut.clock.step(1)
      dut.io.warpRsp.valid.poke(false.B)

      // Should now report tagB
      assert(dut.io.CTArsp.valid.peek().litToBoolean, "CTArsp must be valid for second completion")
      val doneTag2 = dut.io.CTArsp.bits.cu2dispatch_wf_tag_done.peek().litValue
      assert(doneTag2 == tagB, s"Second completion must report tagB ($tagB), but got $doneTag2")

      println("[PASS] CTA2warp tag FIFO isolation under backpressure verified cleanly on master")
    }
  }
}

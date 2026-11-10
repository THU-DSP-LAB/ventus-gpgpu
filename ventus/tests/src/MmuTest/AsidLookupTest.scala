package MmuTest

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import mmu._

class AsidLookupTest extends AnyFreeSpec with ChiselScalatestTester {

  "AsidLookup & PTW 34-bit Physical Address (Issue #104)" - {
    "bundle widths should match SV32 paLen (34 bits)" in {
      val entry = new AsidLookupEntry(SV32)
      assert(entry.ptbr.getWidth == 34, s"Expected ptbr width 34, got ${entry.ptbr.getWidth}")

      val ptwReq = new PTW_Req(SV32)
      assert(ptwReq.paddr.getWidth == 34, s"Expected paddr width 34, got ${ptwReq.paddr.getWidth}")

      val cacheReq = new Cache_Req(SV32)
      assert(cacheReq.addr.getWidth == 34, s"Expected addr width 34, got ${cacheReq.addr.getWidth}")
    }

    "should store and retrieve 34-bit physical address exceeding 4GB" in {
      test(new AsidLookup(SV32, nBanks = 2, nEntries = 8)) { dut =>
        val highPtbr = BigInt("212345000", 16) // > 4GB (bit 33 asserted)

        // Fill entry for ASID 1
        dut.io.fill_in.valid.poke(true.B)
        dut.io.fill_in.bits.asid.poke(1.U)
        dut.io.fill_in.bits.ptbr.poke(highPtbr.U)
        dut.io.fill_in.bits.valid.poke(true.B)

        dut.clock.step(1)
        dut.io.fill_in.valid.poke(false.B)

        // Lookup bank 0: ASID 1, bank 1: ASID 2 (miss)
        dut.io.lookup_req(0).poke(1.U)
        dut.io.lookup_req(1).poke(2.U)

        dut.clock.step(1)

        val hit0 = dut.io.lookup_rsp(0).valid.peek().litToBoolean
        val ptbr0 = dut.io.lookup_rsp(0).bits.peek().litValue
        assert(hit0, "Expected hit on bank 0 for ASID 1")
        assert(ptbr0 == highPtbr, s"Expected 0x${highPtbr.toString(16)} but got 0x${ptbr0.toString(16)}")

        val hit1 = dut.io.lookup_rsp(1).valid.peek().litToBoolean
        assert(!hit1, "Expected miss on bank 1 for non-existent ASID 2")
      }
    }

    "should update existing ASID with 34-bit PTBR and assert flush_tlb" in {
      test(new AsidLookup(SV32, nBanks = 2, nEntries = 8)) { dut =>
        val ptbr1 = BigInt("100000000", 16) // exactly 4GB
        val ptbr2 = BigInt("3abcd0000", 16) // ~14.7GB

        // Initial fill
        dut.io.fill_in.valid.poke(true.B)
        dut.io.fill_in.bits.asid.poke(5.U)
        dut.io.fill_in.bits.ptbr.poke(ptbr1.U)
        dut.io.fill_in.bits.valid.poke(true.B)
        dut.clock.step(1)

        // Update same ASID
        dut.io.fill_in.bits.ptbr.poke(ptbr2.U)
        dut.clock.step(1)
        dut.io.fill_in.valid.poke(false.B)

        val flushValid = dut.io.flush_tlb.valid.peek().litToBoolean
        val flushAsid = dut.io.flush_tlb.bits.peek().litValue
        assert(flushValid, "Expected flush_tlb to be asserted when updating existing ASID")
        assert(flushAsid == 5, s"Expected flush ASID 5, got $flushAsid")

        // Verify updated lookup
        dut.io.lookup_req(0).poke(5.U)
        dut.clock.step(1)
        val res = dut.io.lookup_rsp(0).bits.peek().litValue
        assert(res == ptbr2, s"Expected updated ptbr 0x${ptbr2.toString(16)} but got 0x${res.toString(16)}")
      }
    }
  }
}


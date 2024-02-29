package MmuTest

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._
import chisel3.experimental.VecLiterals._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import pipeline._
import pipeline.mmu._
import top.DecoupledPipe
import MemboxS._
import play.TestUtils._
import MmuTestUtils._

class L1Tlb_test extends AnyFreeSpec
  with ChiselScalatestTester
  with MMUHelpers {
  "L1TLB Main" in {
    test(new L1TLB(SV32.device, 1)).withAnnotations(Seq(WriteVcdAnnotation)){ d =>
      d.io.in.setSourceClock(d.clock)
      d.io.out.setSinkClock(d.clock)
      d.io.l2_req.setSinkClock(d.clock)
      d.io.l2_rsp.setSourceClock(d.clock)
      d.io.invalidate.setSourceClock(d.clock)

      d.clock.step(3)
    }
  }
}

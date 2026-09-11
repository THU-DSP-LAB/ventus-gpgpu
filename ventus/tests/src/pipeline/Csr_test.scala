package pipeline

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

/** Regression for CSRRSV of CSR.threadid.
  *
  * The scalar CSR issue pipe must nevertheless write a vector result: RT uses
  * this broadcast first-lane ID as the base of private payload frames. */
class CsrTest extends AnyFreeSpec with ChiselScalatestTester {
  private def zero(data: Data): Unit = data match {
    case bool: Bool => bool.poke(false.B)
    case uint: UInt => uint.poke(0.U)
    case aggregate: Aggregate => aggregate.getElements.foreach(zero)
  }

  "CSR.threadid broadcasts the hardware warp first lane through vector writeback" in {
    test(new CSRFile) { dut =>
      zero(dut.io.ctrl)
      dut.io.in1.poke(0.U)
      dut.io.write.poke(false.B)
      zero(dut.io.CTA2csr.bits)
      dut.io.CTA2csr.valid.poke(true.B)
      // Warp tag 2 with the default 32-lane hardware warp has first lane 64.
      dut.io.CTA2csr.bits.CTAdata.dispatch2cu_wf_tag_dispatch.poke(2.U)
      dut.clock.step()
      dut.io.CTA2csr.valid.poke(false.B)

      // CSRRSV v0, CSR.threadid, x0 (the opcode selected by the RT intrinsic).
      dut.io.ctrl.inst.poke("h80002072".U)
      dut.io.wb_isvec.expect(true.B)
      dut.io.wb_wvd_rd.foreach(_.expect(64.U))
    }
  }
}

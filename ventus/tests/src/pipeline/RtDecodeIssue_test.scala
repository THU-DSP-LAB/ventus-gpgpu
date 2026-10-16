package pipeline

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import top.parameters._

/** Keeps RT-local instructions on the normal vector dependency path while
  * routing their execution to the RT-local SRAM port. */
class RtDecodeIssueTest extends AnyFreeSpec with ChiselScalatestTester {
  private def zero(data: Data): Unit = data match {
    case bool: Bool => bool.poke(false.B)
    case uint: UInt => uint.poke(0.U)
    case aggregate: Aggregate => aggregate.getElements.foreach(zero)
  }

  private def initDecode(dut: InstrDecodeV2): Unit = {
    dut.io.inst.foreach(_.poke(0.U))
    dut.io.inst_mask.foreach(_.poke(false.B))
    dut.io.pc.poke(0.U); dut.io.wid.poke(0.U); dut.io.sm_id.poke(0.U)
    dut.io.flush_wid.valid.poke(false.B); dut.io.flush_wid.bits.poke(0.U)
    dut.io.ibuffer_ready.foreach(_.poke(true.B))
  }

  "decodes RT-local load/store with their vector dependency semantics" in {
    test(new InstrDecodeV2) { dut =>
      initDecode(dut)

      // vlrt.w v8, field4; the field is bits [31:26].
      dut.io.inst(0).poke("h1200345f".U); dut.io.inst_mask(0).poke(true.B)
      dut.io.control_mask(0).expect(true.B)
      dut.io.control(0).rtOp.expect(RtOp.localLoad)
      dut.io.control(0).isvec.expect(true.B)
      dut.io.control(0).wvd.expect(true.B)
      dut.io.control(0).reg_idxw.expect(8.U)

      // vsrt.w v8, field4 must read v8 as source two, not lose its RAW edge.
      dut.io.inst(0).poke("h1280405f".U)
      dut.io.control(0).rtOp.expect(RtOp.localStore)
      dut.io.control(0).isvec.expect(true.B)
      dut.io.control(0).wvd.expect(false.B)
      dut.io.control(0).sel_alu2.expect(IDecode.A2_VRS2)
      dut.io.control(0).reg_idx2.expect(8.U)
    }
  }

  "routes every RT-local operation exclusively to the RT issue output" in {
    test(new Issue) { dut =>
      zero(dut.io.in.bits); dut.io.in.valid.poke(false.B)
      Seq(dut.io.out_sALU, dut.io.out_vALU, dut.io.out_vFPU, dut.io.out_LSU,
        dut.io.out_SFU, dut.io.out_SIMT, dut.io.out_warpscheduler, dut.io.out_CSR,
        dut.io.out_MUL, dut.io.out_TC, dut.io.out_RT).foreach(_.ready.poke(true.B))

      dut.io.in.bits.ctrl.rtOp.poke(RtOp.localStore)
      dut.io.in.bits.ctrl.isvec.poke(true.B)
      dut.io.in.valid.poke(true.B)
      dut.io.out_RT.valid.expect(true.B)
      dut.io.out_vALU.valid.expect(false.B)
      dut.io.out_LSU.valid.expect(false.B)
      dut.clock.step()

      dut.io.in.bits.ctrl.rtOp.poke(RtOp.none)
      dut.io.out_vALU.valid.expect(true.B)
      dut.io.out_RT.valid.expect(false.B)
    }
  }
}

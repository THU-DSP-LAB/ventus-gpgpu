package pipeline

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import top.parameters._
import pipeline.IDecode._

class AmoDecodeTest extends AnyFreeSpec with ChiselScalatestTester {
  "DecodeUnit correctly decodes RV32A atomic instructions" in {
    test(new InstrDecodeV2()).withAnnotations(Seq()) { dut =>
      // Test cases: (instruction binary, instruction name, expected alu_fn, expected mem_cmd)
      // RV32A standard format: funct5(5) | aq(1) | rl(1) | rs2(5) | rs1(5) | funct3(3)=010 | rd(5) | opcode(7)=0101111
      // rd=x1 (00001), rs1=x2 (00010), rs2=x3 (00011)
      def encodeAmo(funct5: Int, rs2: Int = 3, rs1: Int = 2, rd: Int = 1, aq: Int = 0, rl: Int = 0): BigInt = {
        val op = 0x2f // 0101111
        val funct3 = 0x2 // 010
        ((BigInt(funct5) & 0x1f) << 27) |
        ((BigInt(aq) & 1) << 26) |
        ((BigInt(rl) & 1) << 25) |
        ((BigInt(rs2) & 0x1f) << 20) |
        ((BigInt(rs1) & 0x1f) << 15) |
        ((BigInt(funct3) & 0x7) << 12) |
        ((BigInt(rd) & 0x1f) << 7) |
        (BigInt(op) & 0x7f)
      }

      val testCases = Seq(
        ("lr.w",      encodeAmo(0x02, rs2 = 0), FN_ADD,    M_XRD),
        ("sc.w",      encodeAmo(0x03),          FN_ADD,    M_XWR),
        ("amoswap.w", encodeAmo(0x01),          FN_SWAP,   M_XWR),
        ("amoadd.w",  encodeAmo(0x00),          FN_AMOADD, M_XWR),
        ("amoxor.w",  encodeAmo(0x04),          FN_XOR,    M_XWR),
        ("amoand.w",  encodeAmo(0x0c),          FN_AND,    M_XWR),
        ("amoor.w",   encodeAmo(0x08),          FN_OR,     M_XWR),
        ("amomin.w",  encodeAmo(0x10),          FN_MIN,    M_XWR),
        ("amomax.w",  encodeAmo(0x14),          FN_MAX,    M_XWR),
        ("amominu.w", encodeAmo(0x18),          FN_MINU,   M_XWR),
        ("amomaxu.w", encodeAmo(0x1c),          FN_MAXU,   M_XWR)
      )

      dut.io.pc.poke(0x1000.U)
      dut.io.wid.poke(0.U)
      dut.io.sm_id.poke(0.U)
      dut.io.flush_wid.valid.poke(false.B)
      dut.io.flush_wid.bits.poke(0.U)
      for (w <- 0 until num_warp) {
        dut.io.ibuffer_ready(w).poke(true.B)
      }

      for ((name, instVal, expectedAluFn, expectedMemCmd) <- testCases) {
        dut.io.inst(0).poke(instVal.U)
        dut.io.inst(1).poke(0.U)
        dut.io.inst_mask(0).poke(true.B)
        dut.io.inst_mask(1).poke(false.B)
        dut.clock.step(1)

        val ctrl = dut.io.control(0)
        val isAtomic = ctrl.atomic.peek().litToBoolean
        val isMem = ctrl.mem.peek().litToBoolean
        val isWxd = ctrl.wxd.peek().litToBoolean
        val memCmd = ctrl.mem_cmd.peek().litValue
        val aluFn = ctrl.alu_fn.peek().litValue
        val isVec = ctrl.isvec.peek().litToBoolean
        val isFp = ctrl.fp.peek().litToBoolean

        assert(isAtomic, s"[$name] atomic flag was false! Inst: 0x${instVal.toString(16)}")
        assert(isMem, s"[$name] mem flag was false!")
        assert(isWxd, s"[$name] wxd flag was false!")
        assert(memCmd == expectedMemCmd.litValue, s"[$name] mem_cmd mismatch: expected $expectedMemCmd, got $memCmd")
        assert(aluFn == expectedAluFn.litValue, s"[$name] alu_fn mismatch: expected $expectedAluFn, got $aluFn")
        assert(!isVec, s"[$name] isvec must be false for scalar AMO")
        assert(!isFp, s"[$name] fp must be false for scalar AMO")
        println(s"[PASS] $name correctly decoded (atomic=true, mem_cmd=$memCmd, alu_fn=$aluFn)")
      }

      // Also verify acquire and release flags
      val amoAcqRel = encodeAmo(0x00, aq = 1, rl = 1)
      dut.io.inst(0).poke(amoAcqRel.U)
      dut.clock.step(1)
      assert(dut.io.control(0).aq.peek().litToBoolean, "aq flag not set for amoadd.w.aq")
      assert(dut.io.control(0).rl.peek().litToBoolean, "rl flag not set for amoadd.w.rl")
      println("[PASS] aq/rl flags correctly decoded")
    }
  }
}


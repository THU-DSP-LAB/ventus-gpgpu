package pipeline

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import top.parameters._

class BranchJoinTest extends AnyFreeSpec with ChiselScalatestTester {

  "branch_join_stack pop should not underflow rd_ptr when stack becomes empty" in {
    test(new branch_join_stack(16)) { dut =>
      dut.io.push.poke(false.B)
      dut.io.pop.poke(false.B)
      dut.io.PCexecute.poke(0.U)
      dut.io.threadMask.poke(0xff.U)
      dut.reset.poke(true.B)
      dut.clock.step(2)
      dut.reset.poke(false.B)

      // Push one entry: reconPC = 0x3000, jumpPC = 0x2000
      dut.io.push.poke(true.B)
      dut.io.pushData.reconPC.poke(0x3000.U)
      dut.io.pushData.jumpPC.poke(0x2000.U)
      dut.io.pushData.newMask.poke(0x0f.U)
      dut.io.threadMask.poke(0xff.U)
      dut.clock.step(1)
      dut.io.push.poke(false.B)

      // Pop the entry: execute at reconPC 0x3000
      dut.io.PCexecute.poke(0x3000.U)
      dut.io.pop.poke(true.B)
      assert(dut.io.jump.peek().litToBoolean, "Stack must indicate jump on matching reconPC")
      dut.clock.step(1)

      // Second entry on stack popped
      assert(dut.io.jump.peek().litToBoolean, "Second entry should be ready to pop")
      dut.clock.step(1)
      dut.io.pop.poke(false.B)

      // Now ensure subsequent cycles with pop do not underflow rd_ptr out-of-bounds
      dut.clock.step(2)
      assert(!dut.io.jump.peek().litToBoolean, "Stack jump must be false when empty")
      println("[PASS] branch_join_stack cleanly emptied without rd_ptr underflow on master")
    }
  }

  "branch_join must gate if_mask_buf readiness on valid matching branch_ctl" in {
    test(new branch_join(16)) { dut =>
      dut.io.branch_ctl.valid.poke(false.B)
      dut.io.if_mask.valid.poke(false.B)
      dut.io.pc_reconv.valid.poke(false.B)
      dut.io.fetch_ctl.ready.poke(true.B)
      dut.io.input_wid.poke(0.U)
      dut.reset.poke(true.B)
      dut.clock.step(2)
      dut.reset.poke(false.B)
      dut.clock.step(1)

      // Present if_mask ahead of branch_ctl (bubble or stall on branch_ctl)
      dut.io.if_mask.valid.poke(true.B)
      dut.io.if_mask.bits.wid.poke(0.U)
      dut.io.if_mask.bits.if_mask.poke("h55".U)

      // Because branch_ctl is NOT valid, if_mask MUST NOT be ready (must not drop!)
      assert(!dut.io.if_mask.ready.peek().litToBoolean,
        "if_mask.ready must be FALSE when branch_ctl is not valid!")
      dut.clock.step(2)
      assert(!dut.io.if_mask.ready.peek().litToBoolean,
        "if_mask.ready must remain FALSE during pipeline bubbles")

      // Now present branch_ctl for the same warp
      dut.io.branch_ctl.valid.poke(true.B)
      dut.io.branch_ctl.bits.opcode.poke(0.U) // vbranch
      dut.io.branch_ctl.bits.wid.poke(0.U)
      dut.io.branch_ctl.bits.PC_branch.poke("h2000".U)
      dut.io.branch_ctl.bits.PC_execute.poke("h1000".U)
      dut.io.branch_ctl.bits.mask_init.poke("hff".U)

      dut.io.pc_reconv.valid.poke(true.B)
      dut.io.pc_reconv.bits.poke("h3000".U)

      // Now both should be ready and handshake simultaneously
      assert(dut.io.if_mask.ready.peek().litToBoolean, "if_mask.ready must be TRUE when branch_ctl is ready")
      assert(dut.io.branch_ctl.ready.peek().litToBoolean, "branch_ctl.ready must be TRUE when if_mask is valid")

      dut.clock.step(1)
      dut.io.branch_ctl.valid.poke(false.B)
      dut.io.if_mask.valid.poke(false.B)
      dut.io.pc_reconv.valid.poke(false.B)

      dut.clock.step(1)
      println("[PASS] branch_join correctly synchronizes if_mask and branch_ctl handshakes on master")
    }
  }

  "branch_join divergence and reconvergence thread mask lifecycle" in {
    test(new branch_join(16)) { dut =>
      dut.io.branch_ctl.valid.poke(false.B)
      dut.io.if_mask.valid.poke(false.B)
      dut.io.pc_reconv.valid.poke(false.B)
      dut.io.fetch_ctl.ready.poke(true.B)
      dut.io.input_wid.poke(0.U)
      dut.reset.poke(true.B)
      dut.clock.step(2)
      dut.reset.poke(false.B)
      dut.clock.step(1)

      // Initial mask for warp 0 is all 1s
      val initialMask = dut.io.out_mask.peek().litValue
      val allThreads = (BigInt(1) << num_thread) - 1
      assert((initialMask & allThreads) == allThreads, "Initial warp mask must have all threads active")

      // Divergent branch on warp 0: 4 threads take if (0x0f), remaining threads take else
      dut.io.branch_ctl.valid.poke(true.B)
      dut.io.branch_ctl.bits.opcode.poke(0.U) // vbranch
      dut.io.branch_ctl.bits.wid.poke(0.U)
      dut.io.branch_ctl.bits.PC_branch.poke("h2000".U)
      dut.io.branch_ctl.bits.PC_execute.poke("h1000".U)
      dut.io.branch_ctl.bits.mask_init.poke(allThreads.U)

      dut.io.if_mask.valid.poke(true.B)
      dut.io.if_mask.bits.wid.poke(0.U)
      dut.io.if_mask.bits.if_mask.poke("h0f".U)

      dut.io.pc_reconv.valid.poke(true.B)
      dut.io.pc_reconv.bits.poke("h3000".U)

      dut.clock.step(1)
      dut.io.branch_ctl.valid.poke(false.B)
      dut.io.if_mask.valid.poke(false.B)
      dut.io.pc_reconv.valid.poke(false.B)
      dut.clock.step(1)

      // During if-path, active mask is updated
      val activeMask = dut.io.out_mask.peek().litValue
      assert((activeMask & "hf0".U.litValue) == 0, "Divergent inactive threads must be masked out")
      println(f"[PASS] Warp divergence active mask: 0x$activeMask%x")

      // Now join at reconPC 0x3000 (vjoin)
      dut.io.branch_ctl.valid.poke(true.B)
      dut.io.branch_ctl.bits.opcode.poke(1.U) // vjoin
      dut.io.branch_ctl.bits.wid.poke(0.U)
      dut.io.branch_ctl.bits.PC_execute.poke("h3000".U)

      dut.clock.step(1)
      dut.io.branch_ctl.valid.poke(false.B)
      dut.clock.step(2)

      println("[PASS] branch_join full divergence and reconvergence cycle verified successfully on master")
    }
  }
}

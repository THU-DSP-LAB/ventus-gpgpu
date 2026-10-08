package rtcore

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class RtCompletionPolicyHarness extends Module {
  val io = IO(new Bundle {
    val completion = Input(new RtWarpCompletion)
    val grouped = Input(Bool())
    val terminalFence = Output(Bool())
  })
  io.terminalFence := io.grouped && io.completion.contextReleased && !io.completion.hasCandidate
}

class RtCompletionPolicyTest extends AnyFreeSpec with ChiselScalatestTester {
  "physically released candidates bypass the logical group terminal fence" in {
    test(new RtCompletionPolicyHarness).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      dut.io.grouped.poke(true.B)
      dut.io.completion.warpId.poke(0.U)
      dut.io.completion.issueId.poke(0.U)
      dut.io.completion.destinationVd.poke(0.U)
      dut.io.completion.completionMask.poke(1.U)
      dut.io.completion.contextReleased.poke(true.B)
      dut.io.completion.laneStatus.foreach(_.poke(RtAbiLayout.TraversalCompleteMiss.U))
      for (status <- Seq(RtAbiLayout.TraversalCandidateProceduralAabb,
          RtAbiLayout.TraversalCandidateNonOpaqueTriangle)) {
        dut.io.completion.laneStatus(0).poke(status.U)
        dut.io.terminalFence.expect(false.B)
        dut.io.completion.completionMask.poke(2.U)
        dut.io.terminalFence.expect(true.B) // Inactive candidate lane ignored.
        dut.io.completion.completionMask.poke(1.U)
      }
      dut.io.completion.laneStatus(0).poke(RtAbiLayout.TraversalCompleteHit.U)
      dut.io.terminalFence.expect(true.B)
      dut.io.completion.contextReleased.poke(false.B)
      dut.io.terminalFence.expect(false.B)
      dut.io.completion.contextReleased.poke(true.B)
      dut.io.grouped.poke(false.B)
      dut.io.terminalFence.expect(false.B)
    }
  }
}

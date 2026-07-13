package pipeline

import chisel3._
import chisel3.util._

class PipeExecutionResultFabricIO(numSubcores: Int) extends Bundle {
  val aluBranchIn = Flipped(Vec(numSubcores, DecoupledIO(new BranchCtrl)))
  val lifecycleIn = Flipped(Vec(numSubcores, DecoupledIO(new warpSchedulerExeData)))

  val aluBranchOut = DecoupledIO(new BranchCtrl)
  val lifecycleOut = DecoupledIO(new warpSchedulerExeData)
}

/** Keeps all cross-subcore execution arbitration behind one stable hierarchy. */
class PipeExecutionResultFabric(numSubcores: Int) extends Module {
  val io = IO(new PipeExecutionResultFabricIO(numSubcores))

  private def arbitrate[T <: Data](in: Vec[DecoupledIO[T]], out: DecoupledIO[T], gen: T): Unit = {
    val arb = Module(new Arbiter(gen, numSubcores))
    arb.io.in <> in
    out <> arb.io.out
  }

  arbitrate(io.aluBranchIn, io.aluBranchOut, new BranchCtrl)
  arbitrate(io.lifecycleIn, io.lifecycleOut, new warpSchedulerExeData)
}

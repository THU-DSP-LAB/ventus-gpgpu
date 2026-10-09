package rtcore

import circt.stage.ChiselStage
import org.scalatest.freespec.AnyFreeSpec

class RtCoreStructureTest extends AnyFreeSpec {
  "structural defaults have one source and remain compatible with fixed spill pages" in {
    assert(RtCorePortParameters.residentWarpCount == RtCoreSchedulerParameters.physicalSessionCount)
    assert(RtTraversalStackParameters.baseDepth == RtCoreSchedulerParameters.stackBaseDepth)
    assert(RtTraversalStackParameters.overflowStacks == RtCoreSchedulerParameters.stackOverflowBanks)
    assert(RtTraversalStackParameters.overflowDepth == RtCoreSchedulerParameters.stackOverflowDepth)
    assert(RtMemTableParameters.asEntryCount == RtCoreSchedulerParameters.asEntryCount)
    assert(RtLocalSramParameters.bankCount == RtCoreSchedulerParameters.boxUnitCount)
    assert(RtTraversalStackParameters.nodeRefWidth == 32)
    assert(RtTraversalStackParameters.baseDepth <= RtSpillSlotLayout.wordsPerPage)
    assert(RtTraversalStackParameters.overflowDepth <= RtSpillSlotLayout.wordsPerPage)
  }

  "scheduler elaborates with separated transport and observation wiring" in {
    val rtl = ChiselStage.emitSystemVerilog(new RtCoreScheduler,
      firtoolOpts = Array("--disable-all-randomization", "--strip-debug-info"))
    assert(rtl.contains("module RtCoreScheduler"))
    assert(rtl.contains("module RtLocalReadArbiter"))
    assert(rtl.contains("module RtSpillLsuArbiter"))
  }

  "spill transfer retains control separately from its single shared image buffer" in {
    val ir = ChiselStage.emitCHIRRTL(new RtSpillTransfer)
    val saved = ir.linesIterator.find(_.trim.startsWith("reg saved :"))
      .getOrElse(fail("missing spill transfer control register"))
    assert(saved.contains("pointer"))
    assert(!saved.contains("image"))
  }
}

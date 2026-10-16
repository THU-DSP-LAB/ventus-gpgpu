package rtcore

import chisel3.util._
import top.parameters._

/** Structural hardware choices. Changing a value is not proof that every
  * fixed-bank datapath supports it; the constraints below document this baseline.
  * Shader/driver ABI constants remain in RtAbiLayout/RtVtasLayout/RtSpillSlotLayout.
  */
object RtCoreSchedulerParameters {
  val boxUnitCount = 8
  val triangleUnitCount = 2
  val physicalSessionCount = 1
  val taskBatchingEnabled = true
  val stackBaseDepth = 32
  val stackOverflowBanks = 4
  val stackOverflowDepth = 32
  val asEntryCount = 32
  require(num_thread == 32 && xLen == 32)
  require(boxUnitCount == 8 && triangleUnitCount == 2,
    "Current IU/context routing assumes eight Box4 and two Triangle banks")
  require(physicalSessionCount == 1,
    "Multi-parent admission needs explicit slot ownership and completion tracking")
  require(isPow2(asEntryCount))
}

object RtCorePortParameters {
  /* Validate the Vulkan-selected group size.  Admission uses the physical
   * RT Local row map, so multiple 1/2/4/8-warp groups occupy the available
   * rows naturally instead of relying on a separate static slot counter. */
  val groupWarps = sys.env.get("VENTUS_VK_RT_GROUP_WARPS").flatMap(_.toIntOption).getOrElse(1)
  require(Set(1, 2, 4, 8).contains(groupWarps),
    s"VENTUS_VK_RT_GROUP_WARPS must be 1, 2, 4, or 8 (got $groupWarps)")
  // RTcore accepts one warp command at a time.  This is deliberately
  // independent of the number of resident SIMT warps in the enclosing SM.
  val residentWarpCount = RtCoreSchedulerParameters.physicalSessionCount
  val residentWarpIdWidth = math.max(1, log2Ceil(residentWarpCount))
  val issueIdWidth = 8
  // Intersection pipelines use this as an internal ownership tag; it is not
  // an LSU request identifier.
  val requestIdWidth = 8
  require(requestIdWidth >= log2Ceil(num_thread))
  val lineWordCount = dcache_BlockWords
  val lineBytes = lineWordCount * (xLen / 8)
  require(lineBytes == 128, "RTcore reads use one 128-byte DCache line")
  val regidx_totalwidth = regidx_width + regext_width
}

object RtTraversalStackParameters {
  val nodeRefWidth = 32
  val baseDepth = RtCoreSchedulerParameters.stackBaseDepth
  val overflowStacks = RtCoreSchedulerParameters.stackOverflowBanks
  val overflowDepth = RtCoreSchedulerParameters.stackOverflowDepth
}

object RtMemTableParameters {
  val asEntryCount = RtCoreSchedulerParameters.asEntryCount
  val asEntryIndexWidth = log2Ceil(asEntryCount)
  val asReadPortCount = 1 + RtCoreSchedulerParameters.boxUnitCount + RtCoreSchedulerParameters.triangleUnitCount
  val lineWordCount = RtCorePortParameters.lineWordCount
  require(lineWordCount == num_thread)
}

object RtLocalSramParameters {
  final val warpCount = num_warp
  final val fieldCount = RtAbiLayout.LocalFieldMajorFixedHeaderWordCount
  final val bankDepth = warpCount * fieldCount
  // One bank maps directly to one Box4 IU: lanes i, i+8, i+16 and i+24
  // always use bank i.  This preserves the scheduler's fixed laneId % 8 IU
  // routing and limits the core-side selector to four local lanes.
  final val bankCount = RtCoreSchedulerParameters.boxUnitCount
  final val lanesPerBank = num_thread / bankCount
  final val corePortCount = bankCount
  require(num_thread == 32)
  require(xLen == 32)
  require(bankDepth == 320)
  require(num_thread % bankCount == 0)
}

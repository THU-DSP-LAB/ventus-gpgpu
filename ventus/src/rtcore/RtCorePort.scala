/*
 * RTcore module boundary for the evolving Ventus SM pipeline.
 *
 * This file intentionally defines bundles only: it lets the decoder/issue path,
 * RTcore implementation, RT Local SRAM service, and cache adapter evolve independently.
 * The interface is wired into `pipe.scala`; its local-SRAM and performance
 * event ports deliberately stay distinct from generic GPGPU LSU traffic.  The
 * interface is described by
 * docs/rtcore/microarchitecture/rtcore-hardware-microarchitecture.md.
 */
package rtcore

import chisel3._
import chisel3.util._
import lsu.{LsuClientRequest, LsuClientResponse, LsuClientTag}
import top.parameters._
import rtcore.RtCorePortParameters.{regidx_totalwidth => regidx_totalwidth}

/** Warp-granular representation of a decoded `vt.rt.traverse`. */
class RtWarpCommand extends Bundle {
  val spillPool = new RtSpillPoolDescriptor
  val warpId = UInt(depth_warp.W)
  // Resident CTA slot plus the warp's position in that CTA.  `warpId` is a
  // reusable physical register-file slot and therefore must never identify a
  // group-traverse collective by itself.
  val workgroupTag = UInt((TAG_WIDTH - log2Ceil(num_warp_in_a_block)).W)
  val workgroupMember = UInt(log2Ceil(num_warp_in_a_block).W)
  val workgroupWarps = UInt(log2Ceil(num_warp_in_a_block + 1).W)
  // Scheduler-private allocation map for this workgroup.  Software locators
  // name logical members; only this table names reusable physical warp rows.
  val workgroupMemberWarp = Vec(num_warp_in_a_block, UInt(depth_warp.W))
  val workgroupMemberValid = UInt(num_warp_in_a_block.W)
  val issueId = UInt(RtCorePortParameters.issueIdWidth.W)
  val destinationVd = UInt(regidx_totalwidth.W)
  val activeMask = UInt(num_thread.W)
  // Hardware admission hint from already-published RT Local ray headers.
  // No ray payload or shader-register values are carried in this hint.
  val batchEligible = Bool()
  /* At the pipeline boundary `grouped` denotes the group-traverse ISA form.
   * RtTaskBatcher also uses this locator path for an internal proxy batch;
   * its member table maps directly to parent rows, without a shader collective.
   * The scheduler resolves every locator through the supplied member table
   * before touching RT Local SRAM. This bit does not imply context ownership. */
  val grouped = Bool()
  val locator = Vec(num_thread, UInt(xLen.W))
}

/** Completion consumed by vector writeback and the warp scoreboard. */
class RtWarpCompletion extends Bundle {
  val warpId = UInt(depth_warp.W)
  val issueId = UInt(RtCorePortParameters.issueIdWidth.W)
  val destinationVd = UInt(regidx_totalwidth.W)
  val completionMask = UInt(num_thread.W)
  val laneStatus = Vec(num_thread, UInt(xLen.W))
  // Physical traversal state has been drained and reclaimed. This is not
  // the software session's rt.release; its per-warp Local SRAM remains live.
  val contextReleased = Bool()
  // A released physical context may still need a shader callback/resume.
  // Only logical terminal completions participate in the group end fence.
  def hasCandidate: Bool = VecInit((0 until num_thread).map(lane =>
    completionMask(lane) &&
      (laneStatus(lane) === RtAbiLayout.TraversalCandidateNonOpaqueTriangle.U ||
       laneStatus(lane) === RtAbiLayout.TraversalCandidateProceduralAabb.U))).asUInt.orR
}

/** A masked end-of-session acknowledgement request from `vt.rt.release`. */
class RtReleaseCommand extends Bundle {
  val warpId = UInt(depth_warp.W)
  val activeMask = UInt(num_thread.W)
}

/** Release retires the complete original mask of the retained RT session. */
class RtReleaseAck extends Bundle {
  val warpId = UInt(depth_warp.W)
  val releasedMask = UInt(num_thread.W)
}

/**
  * One-cycle RTcore-internal PMU events.  They deliberately expose completed
  * handshakes rather than inferred state so `pipe` can keep the common
  * performance-window/reset policy without confusing RT Local SRAM traffic
  * with the generic pipeline LSU.
  */
class RtCorePerfEvents extends Bundle {
  val detailed = if (PMU_DETAILED) Some(new RtDetailedSignals) else None
  val localSramReadRequest = Bool()
  val localSramWriteRequest = Bool()
  val localSramReadResponse = Bool()
  val localSramBackpressure = Bool()
  val raySetupOutput = Bool()
  val asLineAcquire = Bool()
  val asLineReady = UInt(log2Ceil(num_thread + 1).W)
  val asAcquireBackpressure = Bool()
  val asCacheResidentHit = Bool()
  val asCachePendingMerge = Bool()
  val asCacheMiss = Bool()
  val asCacheRefill = Bool()
  val asCacheEviction = Bool()
  val box4Issue = UInt(log2Ceil(RtCoreSchedulerParameters.boxUnitCount + 1).W)
  val box4Result = UInt(log2Ceil(RtCoreSchedulerParameters.boxUnitCount + 1).W)
  val triangleIssue = UInt(log2Ceil(RtCoreSchedulerParameters.triangleUnitCount + 1).W)
  val triangleResult = UInt(log2Ceil(RtCoreSchedulerParameters.triangleUnitCount + 1).W)
  val triangleResolveIssue = Bool()
  val triangleResolveResult = Bool()
  val stackPush = UInt(log2Ceil(num_thread + 1).W)
  val stackPop = UInt(log2Ceil(num_thread + 1).W)
}

/**
 * All externally visible RTcore ports.
 *
 * RtLsu is the sole RTcore-to-LSU boundary.  It is a cacheline client: DCache
 * tags remain LSU-private; opaque client tags identify AS entries on return.
 */
class RtCoreIO extends Bundle {
  val coreId = Input(UInt(32.W))
  val command = Flipped(Decoupled(new RtWarpCommand))
  val completion = Decoupled(new RtWarpCompletion)
  val release = Flipped(Decoupled(new RtReleaseCommand))
  val releaseAck = Decoupled(new RtReleaseAck)

  val localRequest = Decoupled(new RtLocalSramRequest)
  val localResponse = Flipped(Decoupled(new RtLocalSramResponse))
  val localNarrowRequest = Vec(RtLocalSramParameters.corePortCount,
    Decoupled(new RtLocalSramNarrowRequest))
  val localNarrowResponse = Flipped(Vec(RtLocalSramParameters.corePortCount,
    Decoupled(new RtLocalSramNarrowResponse)))

  val lsuRequest = Decoupled(new LsuClientRequest)
  val lsuResponse = Flipped(Decoupled(new LsuClientResponse))
  val perf = Output(new RtCorePerfEvents)
}

/** Scheduler-private decoded node kinds and per-ray primitive metadata.
  * These are not shader ABI records or transferable shader register state. */
private[rtcore] object RtVtasNodeKind {
  val header = 0.U(3.W)
  val box4 = 1.U(3.W)
  val triangle = 2.U(3.W)
  val instance = 3.U(3.W)
  val aabb = 4.U(3.W)
  // Same on-wire header format, but it must initialise an object-tagged
  // stack entry after an Instance transform.
  val blasHeader = 5.U(3.W)
}

private[rtcore] class RtTriangleDispatchMeta extends Bundle {
  val objectRay = Bool()
  val opaque = Bool()
  val primitiveId = UInt(xLen.W)
  val geometryId = UInt(xLen.W)
  val instanceId = UInt(xLen.W)
  val instanceCustomIndex = UInt(24.W)
  val instanceSbtRecordOffset = UInt(24.W)
}

/** Simulation-only observation ports. No counters or feedback into control. */
class RtAsObservation extends Bundle {
  val occupied = UInt(6.W)
  val returned = UInt(6.W)
  val readyEntries = UInt(6.W)
  val acquire = Bool()
  val allocate = Bool()
  val residentHit = Bool()
  val readyLanes = UInt(num_thread.W)
  val acquireEntry = UInt(5.W)
  val address = UInt(memReqLen.W)
  val is64B = Bool()
  val acquireLane = UInt(5.W)
  val refill = Bool()
  val refillEntry = UInt(5.W)
  val injection = Bool()
  val injectionEntry = UInt(5.W)
  val serve = Bool()
  val serveEntry = UInt(5.W)
  val serveLane = UInt(5.W)
  // Multiple fixed IU ports may consume returned lines in one cycle.  Keep
  // the old scalar fields as a representative for simple timeline readers,
  // and export the complete per-lane mapping for the detailed host observer.
  val serveLanes = UInt(num_thread.W)
  val serveEntries = Vec(num_thread, UInt(5.W))
  val eviction = Bool()
  val clear = Bool()
}

class RtUnitObservation extends Bundle {
  val inValid = Bool()
  val inReady = Bool()
  val outValid = Bool()
  val outReady = Bool()
}

/** Ray-load boundaries observed without feeding back into its control path. */
class RtRayLoadObservation extends Bundle {
  val gatherStart = Bool()
  val gatherReadRequest = Bool()
  val gatherReadResponse = Bool()
  val gatherField = UInt(4.W)
  val gatherDone = Bool()
  val laneOut = Bool()
  val laneOutLane = UInt(5.W)
  val setupWrite = Bool()
  val setupWriteLane = UInt(5.W)
}

class RtDetailedSignals extends Bundle {
  val enabled = Bool()
  val clear = Bool()
  val session = Bool()
  val activeMask = UInt(num_thread.W)
  val command = Bool()
  val commandMask = UInt(num_thread.W)
  val commandKind = UInt(2.W) // fresh=0, resume=1, restart=2
  val release = Bool() // physical context retirement, not software session count
  val laneState = Vec(num_thread, UInt(4.W))
  val nodePopLanes = UInt(num_thread.W)
  val nodePopKinds = Vec(num_thread, UInt(3.W))
  val nodeAcquire = Bool()
  val stackReady = UInt(6.W)
  val stackSelected = Bool()
  val popBlockedHeader = Bool()
  val popBlockedPush = Bool()
  val popBlockedHeaderLine = Bool()
  val asBlocked = Bool()
  val readyBlockedRay = Bool()
  val readyBlockedBox = Bool()
  val readyBlockedTriangle = Bool()
  val rayLoad = new RtRayLoadObservation
  val lsu = new RtUnitObservation // request on input, response on output
  val lsuRequestTag = UInt(LsuClientTag.width.W)
  val lsuResponseTag = UInt(LsuClientTag.width.W)
  val units = Vec(RtCoreSchedulerParameters.boxUnitCount +
    RtCoreSchedulerParameters.triangleUnitCount + 1, new RtUnitObservation)
  val asTable = new RtAsObservation
}

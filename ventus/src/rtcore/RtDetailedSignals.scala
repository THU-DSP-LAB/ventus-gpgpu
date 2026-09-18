package rtcore

import chisel3._
import lsu.LsuClientTag
import top.parameters._

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
  val lsu = new RtUnitObservation // request on input, response on output
  val lsuRequestTag = UInt(LsuClientTag.width.W)
  val lsuResponseTag = UInt(LsuClientTag.width.W)
  val units = Vec(RtCoreSchedulerParameters.boxUnitCount +
    RtCoreSchedulerParameters.triangleUnitCount + 1, new RtUnitObservation)
  val asTable = new RtAsObservation
}

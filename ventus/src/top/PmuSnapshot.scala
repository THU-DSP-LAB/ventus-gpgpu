package top

import L1Cache.DCache.DCachePerfCounters
import chisel3._
import pipeline.{InstClassPerfCounters, PipelinePerfCounters, RtCorePerfCounters}
import top.parameters.{num_sm, num_l2cache, PMU_DETAILED}

class L2Observation extends Bundle {
  val enabled = Bool()
  val clear = Bool()
  val request = Bool()
  val requestBlocked = Bool()
  val response = Bool()
  val responseBlocked = Bool()
  val memoryRequest = Bool()
  val memoryRequestBlocked = Bool()
  val memoryResponse = Bool()
  val memoryResponseBlocked = Bool()
  val directoryHit = Bool()
  val directoryMiss = Bool()
  val directoryBlocked = Bool()
}

class GpuPmuSnapshot(val includeDCache: Boolean) extends Bundle {
  val pipeline = Vec(num_sm, new PipelinePerfCounters)
  val instClass = Vec(num_sm, new InstClassPerfCounters)
  val rtcore = Vec(num_sm, new RtCorePerfCounters)
  val dcache = if (includeDCache) Some(Vec(num_sm, new DCachePerfCounters)) else None
  val l2 = if (includeDCache && PMU_DETAILED) Some(Vec(num_l2cache, new L2Observation)) else None
}

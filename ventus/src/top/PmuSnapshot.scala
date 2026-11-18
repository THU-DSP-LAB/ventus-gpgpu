package top

import L1Cache.DCache.DCachePerfCounters
import chisel3._
import pipeline.{InstClassPerfCounters, PipelinePerfCounters}
import top.parameters.num_sm

class GpuPmuSnapshot(val includeDCache: Boolean) extends Bundle {
  val pipeline = Vec(num_sm, new PipelinePerfCounters)
  val instClass = Vec(num_sm, new InstClassPerfCounters)
  val dcache = if (includeDCache) Some(Vec(num_sm, new DCachePerfCounters)) else None
}

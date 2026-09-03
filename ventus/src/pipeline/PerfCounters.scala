/*
 * Copyright (c) 2021-2022 International Innovation Center of Tsinghua University, Shanghai
 * Ventus is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details. */
package pipeline

import chisel3._

class PipelinePerfCounters extends Bundle {
  val activeCycles = UInt(64.W)
  val totalScalarIssued = UInt(64.W)
  val totalVectorIssued = UInt(64.W)

  val execStructuralHazardCyclesX = UInt(64.W)
  val execStructuralHazardCyclesV = UInt(64.W)
  val dataDepStallCycles = UInt(64.W)
  val barrierStallCycles = UInt(64.W)
  val controlHazardFlushCount = UInt(64.W)
  val frontendStallCycles = UInt(64.W)
  val lsuBackpressureCycles = UInt(64.W)
  val ibufferFullCycles = UInt(64.W)
}

class InstClassPerfCounters extends Bundle {
  val computeIssued = UInt(64.W)
  val memIssued = UInt(64.W)
  val ctrlIssued = UInt(64.W)
}

/**
  * RTcore boundary counters.
  *
  * These counters deliberately observe only the instruction-pipeline and LSU
  * boundary.  They remain valid while the RTcore's private SRAM implementation
  * evolves, and can later be complemented with table- and unit-internal PMUs.
  */
class RtCorePerfCounters extends Bundle {
  val traverseIssued = UInt(64.W)
  val traverseActiveLanes = UInt(64.W)
  val completionCount = UInt(64.W)
  val completionLanes = UInt(64.W)
  val releaseIssued = UInt(64.W)
  val activeSessionCycles = UInt(64.W)
  val commandBackpressureCycles = UInt(64.W)
  val completionBackpressureCycles = UInt(64.W)
  val releaseBackpressureCycles = UInt(64.W)
  val lsuRequests = UInt(64.W)
  val lsuResponses = UInt(64.W)
  val lsuBackpressureCycles = UInt(64.W)
}

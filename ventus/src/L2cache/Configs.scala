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

package L2cache

import chisel3._
import org.chipsalliance.cde.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tile._
import freechips.rocketchip.rocket._
import freechips.rocketchip.tilelink._
import sifive.blocks.inclusivecache._
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.subsystem.{CBUS, CacheBlockBytes, SBUS}
import freechips.rocketchip.util._

case class InclusiveCacheParams(
  ways: Int,
  sets: Int,
  writeBytes: Int, // backing store update granularity
  portFactor: Int, // numSubBanks = (widest TL port * portFactor) / writeBytes
  memCycles: Int,  // # of L2 clock cycles for a memory round-trip (50ns @ 800MHz)
  physicalFilter: Option[PhysicalFilterParams] = None,
  // Interior/Exterior refer to placement either inside the Scheduler or outside it
  // Inner/Outer refer to buffers on the front (towards cores) or back (towards DDR) of the L2
  bufInnerInterior: InclusiveCachePortParameters = InclusiveCachePortParameters.fullC,
  bufInnerExterior: InclusiveCachePortParameters = InclusiveCachePortParameters.flowAD,
  bufOuterInterior: InclusiveCachePortParameters = InclusiveCachePortParameters.full,
  bufOuterExterior: InclusiveCachePortParameters = InclusiveCachePortParameters.none)

case object InclusiveCacheKey extends Field[InclusiveCacheParams]


/*
 * Copyright 2019 SiFive, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You should have received a copy of LICENSE.Apache2 along with
 * this software. If not, you may obtain a copy at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package L2cache

import Chisel._
import freechips.rocketchip.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tile._
import freechips.rocketchip.rocket._
import freechips.rocketchip.tilelink._
import sifive.blocks.inclusivecache._
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.subsystem.{BankedL2Key, CBUS, CacheBlockBytes, SBUS}
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


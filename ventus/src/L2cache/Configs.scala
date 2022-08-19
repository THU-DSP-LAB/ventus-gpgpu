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
case object LJZKey extends Field [LJZhnsP]
case class LJZhnsP()

class LJZdshuaibi() extends Config((site, here, up) => {case LJZKey => new LJZhnsP})
/*
class WithInclusiveCache(
  nWays: Int = 8,
  capacityKB: Int = 512,
  outerLatencyCycles: Int = 40,
  subBankingFactor: Int = 4
) extends Config((site, here, up) => {
  case InclusiveCacheKey => InclusiveCacheParams(
      sets = 2,//(capacityKB * 1024)/(site(CacheBlockBytes) * nWays * up(BankedL2Key, site).nBanks),
      ways = nWays,
      memCycles = outerLatencyCycles,
      writeBytes = 1,//site(XLen)/8,
      portFactor = subBankingFactor)
//  case BankedL2Key => up(BankedL2Key, site).copy(coherenceManager = { context =>
//    implicit val p = context.p
//    val sbus = context.tlBusWrapperLocationMap(SBUS)
//    val cbus = context.tlBusWrapperLocationMap.lift(CBUS).getOrElse(sbus)
//    val InclusiveCacheParams(
//      ways,
//      sets,
//      writeBytes,
//      portFactor,
//      memCycles,
//      physicalFilter,
//      bufInnerInterior,
//      bufInnerExterior,
//      bufOuterInterior,
//      bufOuterExterior) = p(InclusiveCacheKey)
    val l2 = LazyModule(new InclusiveCache(
      CacheParameters(
        level = 2,
        ways = ways,
        sets = sets,
        blockBytes = sbus.blockBytes,
        beatBytes = sbus.beatBytes),
      InclusiveCacheMicroParameters(
        writeBytes = writeBytes,
        portFactor = portFactor,
        memCycles = memCycles,
        innerBuf = bufInnerInterior,
        outerBuf = bufOuterInterior),
      Some(InclusiveCacheControlParameters(
        address = InclusiveCacheParameters.L2ControlAddress,
        beatBytes = cbus.beatBytes))))

    context.addLogicalTreeNode(l2.logicalTreeNode)

    def skipMMIO(x: TLClientParameters) = {
      val dcacheMMIO =
        x.requestFifo &&
        x.sourceId.start % 2 == 1 && // 1 => dcache issues acquires from another master
        x.nodePath.last.name == "dcache.node"
      if (dcacheMMIO) None else Some(x)
    }

    val filter = LazyModule(new TLFilter(cfilter = skipMMIO))
    val l2_inner_buffer = bufInnerExterior()
    val l2_outer_buffer = bufOuterExterior()
    val cork = LazyModule(new TLCacheCork)
    val lastLevelNode = cork.node

    l2_inner_buffer.suggestName("InclusiveCache_inner_TLBuffer")
    l2_outer_buffer.suggestName("InclusiveCache_outer_TLBuffer")

    l2_inner_buffer.node :*= filter.node
    l2.node :*= l2_inner_buffer.node
    l2_outer_buffer.node :*= l2.node

    /* PhysicalFilters need to be on the TL-C side of a CacheCork to prevent Acquire.NtoB -> Grant.toT */
    physicalFilter match {
      case None => lastLevelNode :*= l2_outer_buffer.node
      case Some(fp) => {
        val physicalFilter = LazyModule(new PhysicalFilter(fp.copy(controlBeatBytes = cbus.beatBytes)))
        lastLevelNode :*= physicalFilter.node :*= l2_outer_buffer.node
        physicalFilter.controlNode := cbus.coupleTo("physical_filter") {
          TLBuffer(1) := TLFragmenter(cbus) := _
        }
      }
    }

    l2.ctlnode.foreach {
      _ := cbus.coupleTo("l2_ctrl") { TLBuffer(1) := TLFragmenter(cbus) := _ }
    }

   // ElaborationArtefacts.add("l2.json", l2.module.json)
    (filter.node, lastLevelNode, None)
  })
})*/

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
package L1Cache.DCache

import chisel3._
import chisel3.util._
import config.config._
import L1Cache.{HasL1CacheParameters, RVGParameters, RVGParamsKey}
import pipeline.parameters._
case object DCacheParamsKey extends Field [DCacheParameters]

case class DCacheParameters
(
  NSets: Int = dcache_NSets,
  NWays: Int = dcache_NWays,
  //BlockWords: Int = dcache_BlockWords,
  NMshrEntry: Int = dcache_MshrEntry,
  NMshrSubEntry: Int = dcache_MshrSubEntry,
  NWshrEntry: Int = dcache_wshr_entry,
)

trait HasDCacheParameter extends HasL1CacheParameters {
  implicit val p: Parameters

  val dcacheParams = p(DCacheParamsKey)

  override def NSets: Int = dcacheParams.NSets
  override def NWays: Int = dcacheParams.NWays
  //override def BlockWords: Int = BlockWords
  override def NMshrEntry: Int = dcacheParams.NMshrEntry
  override def NMshrSubEntry: Int = dcacheParams.NMshrSubEntry
  def NWshrEntry: Int = dcacheParams.NWshrEntry
  def NBanks = NLanes
  //                                       |   blockOffset  |
  //                                     bankOffset       wordOffset
  // |32      tag       22|21   setIdx   11|10 9|8 bankIdx 2|1 0|

  //TL params
  def TLAOp_Get: UInt = 4.U(3.W)
  def TLAOp_PutFull: UInt = 0.U(3.W)
  def TLAOp_PutPart: UInt = 1.U(3.W)
  def TLAOp_Flush: UInt = 5.U(3.W)
  def TLAParam_Flush : UInt = 0.U(3.W)
  def TLAParam_Inv : UInt = 1.U(3.W)
}
abstract class DCacheBundle(implicit val p: Parameters) extends Bundle with HasDCacheParameter
abstract class DCacheModule(implicit val p: Parameters) extends Module with HasDCacheParameter
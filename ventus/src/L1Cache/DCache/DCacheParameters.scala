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
import top.parameters._
case object DCacheParamsKey extends Field [DCacheParameters]

case class DCacheParameters
(
  NSets: Int = dcache_NSets,
  NWays: Int = dcache_NWays,
  //BlockWords: Int = dcache_BlockWords,
  NMshrEntry: Int = dcache_MshrEntry,
  NMshrSubEntry: Int = dcache_MshrSubEntry,
  NWshrEntry: Int = dcache_wshr_entry,
  NMshrSet: Int = dcache_MshrSet,
  NMshrWay: Int = dcache_MshrWay
)

trait HasDCacheParameter extends HasL1CacheParameters {
  implicit val p: Parameters

  val dcacheParams = p(DCacheParamsKey)

  override def NSets: Int = dcacheParams.NSets
  override def NWays: Int = dcacheParams.NWays
  //override def BlockWords: Int = BlockWords
  override def NMshrEntry: Int = dcacheParams.NMshrEntry
  override def NMshrSubEntry: Int = dcacheParams.NMshrSubEntry
  def NMshrSet: Int = dcacheParams.NMshrSet
  def NMshrWay: Int = dcacheParams.NMshrWay
  def NWshrEntry: Int = dcacheParams.NWshrEntry
  def NBanks = NLanes
  //                                       |   blockOffset  |
  //                                     bankOffset       wordOffset
  // |32      tag       22|21   setIdx   11|10 9|8 bankIdx 2|1 0|
 //For RTAB request
  def NRTABs: Int = 4
  def UCacheHitDirty: UInt = 1.U(4.W)
  def SubEntryFull: UInt = 2.U(4.W)
  def HitSMSHR: UInt = 3.U(4.W)
  def EntryFull: UInt = 4.U(4.W)
  def readHitWSHR: UInt = 5.U(4.W)
  def writeMissHitMSHR: UInt = 6.U(4.W)
  def writeMissHitWSHR: UInt = 7.U(4.W)
  def hitRTAB: UInt = 8.U(4.W)
  def SCLRexist: UInt = 9.U(4.W)
  // bfs4096-006 fix: hit-read 与 fill write 同拍撞同 dA row (Cat(set, way))。
  // dA SRAM bypassWrite=true 会把 fill data forward 给 hit-read，造成跨 cacheline 数据污染
  // (cost tag 命中但拿到 visited 数据)。CoreReqPipe ST1 检测到此条件即 enq RTAB 等 fill 完再 replay。
  // 见 bugs/bfs4096-006/phase_4_report.md §III.2 / §VI.4。
  def fillConflict: UInt = 10.U(4.W)
  // bfs4096-009 fix: read-miss 撞上同 block in-flight fill(撞 missRspIn 释放窗口、被 L1MSHR:177
  // entry_valid 过滤降级 primary）→ 挂 RTAB 等本 block fill commit 再 replay，re-probe tag hit，
  // 全程零第二条 memReq → 无 dup-tag。见 bugs/bfs4096-009/phase_3_analysis_v7_replay_design.md。
  def ReadMissFillWait: UInt = 11.U(4.W)
  //TL params
  def TLAOp_Get       : UInt = 4.U(3.W)
  def TLAOp_PutFull   : UInt = 0.U(3.W)
  def TLAOp_PutPart   : UInt = 1.U(3.W)
  def TLAOp_Flush     : UInt = 5.U(3.W)
  def TLAParam_Flush  : UInt = 0.U(3.W)
  def TLAParam_Inv    : UInt = 1.U(3.W)
  def TLAOp_Arith     : UInt = 2.U(3.W)
  def TLAOp_Logic     : UInt = 3.U(3.W)

  def TLAParam_ArithMin   : UInt = 0.U(3.W)
  def TLAParam_ArithMax   : UInt = 1.U(3.W)
  def TLAParam_ArithMinu  : UInt = 2.U(3.W)
  def TLAParam_ArithMaxu  : UInt = 3.U(3.W)
  def TLAParam_ArithAdd   : UInt = 4.U(3.W)
  def TLAParam_LogicXor   : UInt = 0.U(3.W)
  def TLAParam_LogicOr    : UInt = 1.U(3.W)
  def TLAParam_LogicAnd   : UInt = 2.U(3.W)
  def TLAParam_LogicSwap  : UInt = 3.U(3.W)
  def TLAParam_LRSC       : UInt = 1.U(3.W)

  def TLDOp_AccessAck : UInt = 0.U(3.W)
  def TLDOp_AccessAckData :UInt = 1.U(3.W)
  def TLDOp_HintAck :UInt = 2.U(3.W)
  //LSU Param
  def LSUSwapParam   : UInt =15.U(4.W)
  def LSUAddParam    : UInt = 0.U(4.W)
  def LSUXorParam    : UInt = 1.U(4.W)
  def LSUAndParam    : UInt = 3.U(4.W)
  def LSUOrParam     : UInt = 2.U(4.W)
  def LSUMinParam    : UInt = 4.U(4.W)
  def LSUMaxParam    : UInt = 5.U(4.W)
  def LSUMinuParam   : UInt = 6.U(4.W)
  def LSUMaxuParam   : UInt = 7.U(4.W)

  //MSHR status
  def PrimaryAvail : UInt = 0.U(3.W)
  def PrimaryFull  : UInt = 1.U(3.W)
  def SecondaryAvail : UInt = 2.U(3.W)
  def SecondaryFull : UInt = 3.U(3.W)
  def SecondaryFullRet : UInt = 4.U(3.W)

}
abstract class DCacheBundle(implicit val p: Parameters) extends Bundle with HasDCacheParameter
abstract class DCacheModule(implicit val p: Parameters) extends Module with HasDCacheParameter
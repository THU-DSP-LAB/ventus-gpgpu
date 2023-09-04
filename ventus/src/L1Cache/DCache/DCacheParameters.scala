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
  NMshrEntry: Int = 4,
  NMshrSubEntry: Int = 4,
  //NBanks: Int = 2,
  WdbDepth: Int = 4,
)

trait HasDCacheParameter extends HasL1CacheParameters {
  implicit val p: Parameters

  val dcacheParams = p(DCacheParamsKey)

  override def NSets: Int = dcacheParams.NSets
  override def NWays: Int = dcacheParams.NWays
  //override def BlockWords: Int = BlockWords
  override def NMshrEntry: Int = dcacheParams.NMshrEntry
  override def NMshrSubEntry: Int = dcacheParams.NMshrSubEntry
  def NBanks = NLanes//TODO after support, decouple 2 params
  def WdbDepth: Int = dcacheParams.WdbDepth
  //                                       |   blockOffset  |
  //                                     bankOffset       wordOffset
  // |32      tag       22|21   setIdx   11|10 9|8 bankIdx 2|1 0|
  require(BlockWords>=NBanks,"# of Banks can't be smaller than # of words in a block")
  //thus BankOffsetBits is smaller than or equal to WordOffsetBits
  def BankIdxBits = log2Up(NBanks)
  def get_bankIdx(addr: UInt)= addr(BankIdxBits + WordOffsetBits-1,WordOffsetBits)

  def BankOffsetBits = BlockOffsetBits - BankIdxBits
  def BankWords = BlockWords/NBanks

  //TL params
  def TLAOp_Get: UInt = 4.U(3.W)
  def TLAOp_PutFull: UInt = 0.U(3.W)
  def TLAOp_PutPart: UInt = 1.U(3.W)
  def TLAOp_Arith: UInt = 2.U(3.W)
  def TLAOp_Logic: UInt = 3.U(3.W)

  def TLAParam_ArithMin : UInt = 0.U(3.W)
  def TLAParam_ArithMax : UInt = 1.U(3.W)
  def TLAParam_ArithMinu : UInt = 2.U(3.W)
  def TLAParam_ArithMaxu : UInt = 3.U(3.W)
  def TLAParam_ArithAdd : UInt = 4.U(3.W)
  def TLAParam_LogicXor : UInt = 0.U(3.W)
  def TLAParam_LogicOr : UInt = 1.U(3.W)
  def TLAParam_LogicAnd : UInt = 2.U(3.W)
  def TLAParam_LogicSwap : UInt = 3.U(3.W)
  def TLAParam_LRSC : UInt = 1.U(3.W)
}
abstract class DCacheBundle(implicit val p: Parameters) extends Bundle with HasDCacheParameter
abstract class DCacheModule(implicit val p: Parameters) extends Module with HasDCacheParameter
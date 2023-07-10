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
package L1Cache.ICache

import L1Cache.HasL1CacheParameters
import chisel3._
import config.config._
import top.parameters._
case object ICacheParamsKey extends Field [ICacheParameters]

case class ICacheParameters
(
  nSets: Int = dcache_NSets,
  nWays: Int = dcache_NWays,
  //blockWords: Int = 32,
  nMshrEntry: Int = 4,
  nMshrSubEntry: Int = 4
)//extends L1CacheParameters

trait HasICacheParameter extends HasL1CacheParameters {
  implicit val p: Parameters

  val icacheParams = p(ICacheParamsKey)

  override def NSets: Int = icacheParams.nSets
  override def NWays: Int = icacheParams.nWays
  //override def BlockWords: Int = BlockWords
  override def NMshrEntry: Int = icacheParams.nMshrEntry
  override def NMshrSubEntry: Int = icacheParams.nMshrSubEntry

  override def tIBits = WIdBits//+BlockOffsetBits+WordOffsetBits
}
abstract class ICacheBundle(implicit val p: Parameters) extends Bundle with HasICacheParameter

abstract class ICacheModule(implicit val p: Parameters) extends Module with HasICacheParameter

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
package L1Cache
import chisel3._
import L1Cache.ICache.ICacheParameters
import chisel3.util.log2Up
import config.config.{Field, Parameters}
import top.parameters._


case object RVGParamsKey extends Field [RVGParameters]

case class RVGParameters
(
  NSms: Int = num_sm,
  NLanes: Int = num_thread,
  NWarps: Int = num_warp,
  NSmInCluster: Int = num_sm_in_cluster,
  NCluster: Int = num_cluster,
  WordLength: Int = xLen,
  BlockWords: Int = dcache_BlockWords,
  NCacheInSM: Int = num_cache_in_sm,
  NL2Cache: Int = num_l2cache
)

trait HasRVGParameters {
  implicit val p: Parameters
  val RVGParams = p(RVGParamsKey)

  def NSms = RVGParams.NSms
  def NLanes = RVGParams.NLanes
  def NWarps = RVGParams.NWarps
  def WordLength = RVGParams.WordLength
  def WIdBits = log2Up(NWarps)
  def BytesOfWord = WordLength/8
  def BlockWords = RVGParams.BlockWords

  def NCacheInSM = RVGParams.NCacheInSM

  def NSmInCluster = RVGParams.NSmInCluster
  def NCluster = RVGParams.NCluster
  def NL2Cache = RVGParams.NL2Cache

  def NResTabEntry = 16
  def NInfWriteEntry = 16
}

abstract class RVGBundle(implicit val p: Parameters) extends Bundle with HasRVGParameters
abstract class RVGModule(implicit val p: Parameters) extends Module with HasRVGParameters
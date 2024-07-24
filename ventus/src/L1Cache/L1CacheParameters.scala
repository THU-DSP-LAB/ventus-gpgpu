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

import L1Cache.DCache.{DCacheParameters, DCacheParamsKey}
import L1Cache.ICache.{ICacheParameters, ICacheParamsKey}
import L1Cache.ShareMem.{ShareMemParameters, ShareMemParamsKey}
import chisel3._
import chisel3.util._
import config.config.Config
import top.parameters._

class MyConfig extends Config((site, here, up) =>
{
  case DCacheParamsKey => new DCacheParameters
  case ICacheParamsKey => new ICacheParameters
  case RVGParamsKey => new RVGParameters
  case ShareMemParamsKey => new ShareMemParameters
})


trait HasL1CacheParameters extends HasRVGParameters{
  //val cacheParams: L1CacheParameters

  def NSets: Int = dcache_NSets// replace
  def NWays: Int = dcache_NWays// replace
  //def BlockWords: Int = 32// replace
  def BlockBytes = BlockWords * 4
  def BlockBits = BlockBytes * 8

  def CacheSizeBytes = NSets * NWays * BlockBytes

  def WordOffsetBits = log2Up(BytesOfWord)//a Word has 4 Bytes
  def BlockOffsetBits = log2Up(BlockWords)// select word in block
  def SetIdxBits = log2Up(NSets)
  def TagBits = WordLength - (SetIdxBits + BlockOffsetBits + WordOffsetBits)

  def WayIdxBits = log2Up(NWays)

  def NMshrEntry: Int = 4// replace
  def NMshrSubEntry: Int = 4// replace
  def bABits = TagBits+SetIdxBits
  def tIBits = WIdBits+NLanes*(BlockOffsetBits+1+BytesOfWord)

  //for addr with full width or just block addr
  def get_tag(addr: UInt) = (addr >> (addr.getWidth-TagBits)).asUInt
  def get_setIdx(addr: UInt) = if (addr.getWidth == WordLength) {
    addr(SetIdxBits + BlockOffsetBits + WordOffsetBits-1,BlockOffsetBits + WordOffsetBits)
  } else if (addr.getWidth == bABits) {//blockAddr
    addr(SetIdxBits-1,0)
  } else {
    Fill(addr.getWidth,1.U)
  }

  def get_blockOffset(addr: UInt)= addr(BlockOffsetBits + WordOffsetBits-1,WordOffsetBits)

  def get_offsets(addr: UInt)= addr(BlockOffsetBits + WordOffsetBits-1,0)//blockOffset + workOffset
  def get_blockAddr(addr: UInt) = (addr >> (WordLength-(TagBits+SetIdxBits))).asUInt//tag + setIdx
}

abstract class L1CacheModule extends Module with HasL1CacheParameters
abstract class L1CacheBundle extends Module with HasL1CacheParameters
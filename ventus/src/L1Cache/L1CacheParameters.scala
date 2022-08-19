package L1Cache

import L1Cache.DCache.{DCacheParameters, DCacheParamsKey}
import L1Cache.ICache.{ICacheParameters, ICacheParamsKey}
import L1Cache.ShareMem.{ShareMemParameters, ShareMemParamsKey}
import chisel3._
import chisel3.util._
import config.config.Config

class MyConfig extends Config((site, here, up) =>
{
  case DCacheParamsKey => new DCacheParameters
  case ICacheParamsKey => new ICacheParameters
  case RVGParamsKey => new RVGParameters
  case ShareMemParamsKey => new ShareMemParameters
})


trait HasL1CacheParameters extends HasRVGParameters{
  //val cacheParams: L1CacheParameters

  def NSets: Int = 32// replace
  def NWays: Int = 2// replace
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
  def tIBits = WIdBits+BlockOffsetBits+WordOffsetBits

  //for addr with full width or just block addr
  def get_tag(addr: UInt) = (addr >> (addr.getWidth-TagBits)).asUInt()
  def get_setIdx(addr: UInt) = if (addr.getWidth == WordLength) {
    addr(SetIdxBits + BlockOffsetBits + WordOffsetBits-1,BlockOffsetBits + WordOffsetBits)
  } else if (addr.getWidth == bABits) {//blockAddr
    addr(SetIdxBits-1,0)
  } else {
    Fill(addr.getWidth,1.U)
  }

  def get_blockOffset(addr: UInt)= addr(BlockOffsetBits + WordOffsetBits-1,WordOffsetBits)

  def get_offsets(addr: UInt)= addr(BlockOffsetBits + WordOffsetBits-1,0)//blockOffset + workOffset
  def get_blockAddr(addr: UInt) = (addr >> (WordLength-(TagBits+SetIdxBits))).asUInt()//tag + setIdx
}

abstract class L1CacheModule extends Module with HasL1CacheParameters
abstract class L1CacheBundle extends Module with HasL1CacheParameters
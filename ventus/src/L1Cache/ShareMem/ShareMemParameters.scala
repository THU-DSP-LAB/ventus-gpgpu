package L1Cache.ShareMem

import chisel3._
import chisel3.util._
import config.config._
import L1Cache.{HasL1CacheParameters, RVGParameters, RVGParamsKey}
import pipeline.parameters._
case object ShareMemParamsKey extends Field [ShareMemParameters]

case class ShareMemParameters
(
  NSets: Int = sharedmem_depth,//*block 取对数 -> 地址分段
//  NWays: Int = 2,
  BlockWords: Int = sharedmem_BlockWords,
//  NMshrEntry: Int = 4,
//  NMshrSubEntry: Int = 4,
  //NBanks: Int = 2,
)

trait HasShareMemParameter extends HasL1CacheParameters {
  implicit val p: Parameters

  val shareMemParams = p(ShareMemParamsKey)


  override def NSets: Int = shareMemParams.NSets
  override def NWays: Int = 1//share mem is a direct mapped memory
  override def BlockWords: Int = shareMemParams.BlockWords

  def NBanks = NLanes//TODO after support, decouple 2 params

  //                                       |   blockOffset  |
  //                                     bankOffset       wordOffset
  // |32      tag       22|21   setIdx   11|10 9|8 bankIdx 2|1 0|
  require(BlockWords>=NBanks,"# of Banks can't be smaller than # of words in a block")
  //thus BankOffsetBits is smaller than or equal to WordOffsetBits
  def BankIdxBits = log2Up(NBanks)
  def get_bankIdx(addr: UInt)= addr(BankIdxBits + WordOffsetBits-1,WordOffsetBits)

  def BankOffsetBits = BlockOffsetBits - BankIdxBits
  def BankWords = BlockWords/NBanks
}
abstract class ShareMemBundle(implicit val p: Parameters) extends Bundle with HasShareMemParameter
abstract class ShareMemModule(implicit val p: Parameters) extends Module with HasShareMemParameter
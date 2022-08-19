package L1Cache.ICache

import L1Cache.HasL1CacheParameters
import chisel3._
import config.config._
import pipeline.parameters._
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

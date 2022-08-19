package L1Cache
import chisel3._
import L1Cache.ICache.ICacheParameters
import chisel3.util.log2Up
import config.config.{Field, Parameters}
import pipeline.parameters._


case object RVGParamsKey extends Field [RVGParameters]

case class RVGParameters
(
  NSms: Int = num_sm,
  NLanes: Int = num_thread,
  NWarps: Int = num_warp,
  WordLength: Int = xLen,
  BlockWords: Int = dcache_BlockWords,
  NCacheInSM: Int = num_cache_in_sm
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
}

abstract class RVGBundle(implicit val p: Parameters) extends Bundle with HasRVGParameters
abstract class RVGModule(implicit val p: Parameters) extends Module with HasRVGParameters
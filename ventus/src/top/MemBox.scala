package top
import chisel3._

import scala.io.Source
import chisel3.experimental.BundleLiterals._
import chisel3.experimental.VecLiterals._
import parameters._
import MemboxS._

import scala.collection.mutable.ArrayBuffer

object helper{
  def BigInt2ByteArray(n: BigInt, len: Int): Array[Byte] = n.toByteArray.takeRight(len).reverse.padTo(len, 0.toByte)
  def Hex2ByteArray(hex: String, len: Int): Array[Byte] = BigInt("00" ++ hex, 16).toByteArray.takeRight(len).reverse.padTo(len, 0.toByte)
  def ByteArray2BigInt(ba: Array[Byte]) = BigInt(0.toByte +: ba.reverse)
}

class MetaData{
  var kernel_id: BigInt = 0
  var kernel_size: Array[BigInt] = Array(0, 0, 0)
  var wf_size: BigInt = 0
  var wg_size: BigInt = 0
  var metaDataBaseAddr: BigInt = 0
  var ldsSize: BigInt = 0
  var pdsSize: BigInt = 0
  var sgprUsage: BigInt = 0
  var vgprUsage: BigInt = 0
  var pdsBaseAddr: BigInt = 0
  var num_buffer: BigInt = 1
  var buffer_base = new Array[BigInt](0)
  var buffer_size = new Array[BigInt](0)
  // var lds_mem_base = new Array[BigInt](0)
  // var lds_mem_size = new Array[BigInt](0)
    var lds_mem_index = new Array[Int](0)

  def generateHostReq(i: BigInt, j: BigInt, k: BigInt) = {
    val blockID = (i * kernel_size(1) + j) * kernel_size(2) + k
    (new host2CTA_data).Lit(
      _.host_wg_id -> ("b" + blockID.toString(2) + "0" * CU_ID_WIDTH).U,
      _.host_num_wf -> wg_size.U,
      _.host_wf_size -> wf_size.U,
      _.host_start_pc -> "h80000000".U,
      _.host_vgpr_size_total -> (wg_size * vgprUsage).U,
      _.host_sgpr_size_total -> (wg_size * sgprUsage).U,
      _.host_lds_size_total -> 128.U, // TODO: fix // ldsSize
      _.host_gds_size_total -> 0.U,
      _.host_vgpr_size_per_wf -> vgprUsage.U,
      _.host_sgpr_size_per_wf -> sgprUsage.U,
      _.host_gds_baseaddr -> 0.U,
      _.host_pds_baseaddr -> (pdsBaseAddr + blockID * pdsSize * wf_size * wg_size).U,
      _.host_csr_knl -> metaDataBaseAddr.U,
      _.host_kernel_size_3d -> Vec(3, UInt(WG_SIZE_X_WIDTH.W)).Lit(0 -> i.U, 1 -> j.U, 2 -> k.U)
    )
  }
}

object MetaData{
  def parseHex(buf: Iterator[String], len: Int = 32): BigInt = { // only support 32b and 64b hex
    val lo = if (buf.hasNext) buf.next() else "0"
    val hi = if (buf.hasNext && len > 32) buf.next() else ""
    BigInt(hi ++ lo, 16)
  }
  def apply(metafile: String) = {
    val buf = {
      val file = Source.fromFile(metafile)
      file.getLines()
    }
    new MetaData{
      parseHex(buf, 64) // skip start_pc = 0x80000000
      kernel_id = parseHex(buf, 64)
      kernel_size = kernel_size.map{ _ => parseHex(buf, 64) }
      wf_size = parseHex(buf, 64)
      wg_size = parseHex(buf, 64)
      metaDataBaseAddr = parseHex(buf, 64)
      ldsSize = parseHex(buf, 64)
      pdsSize = parseHex(buf, 64)
      sgprUsage = parseHex(buf, 64)
      vgprUsage = parseHex(buf, 64)
      pdsBaseAddr = parseHex(buf, 64)
      num_buffer = parseHex(buf, 64)
      for( i <- 0 until num_buffer.toInt){
        val parsed = parseHex(buf, 64)
        if(parsed < BigInt("80000000", 16) && parsed >= BigInt("70000000", 16))
          lds_mem_index = lds_mem_index :+ i
        buffer_base = buffer_base :+ parsed
      }
      for (i <- 0 until num_buffer.toInt) {
        val parsed = (parseHex(buf, 64) + 3) / 4 * 4 // padding buffer size to 4byte alignment
        // if(i < lds_mem_base.length)
        //   lds_mem_size = lds_mem_size :+ parsed
        // else
        buffer_size = buffer_size :+ parsed
      }
    }
  }
}

class DynamicMem(val stepSize: Int = 4096){
  class Page(val startAddr: BigInt) {
    val data = Array.fill(stepSize)(0.toByte)
  }
  var pages = new scala.collection.mutable.ArrayBuffer[Page](0)
  def insertPage(startAddr: BigInt): Unit = {
    if(pages.isEmpty) {
      pages = pages :+ new Page(startAddr)
      return
    }
    else if(pages.exists(_.startAddr == startAddr)) return

    val i = pages.lastIndexWhere(_.startAddr < startAddr) + 1
    pages.insert(i, new Page(startAddr))
  }

  def readMem(addr: BigInt, len: Int): Array[Byte] = {
    val lower = addr - addr % stepSize
    val upper = (addr + len) - (addr + len) % stepSize
    var res = new Array[Byte](0)
    for(currentPageBase <- lower to upper by stepSize){
      if(!pages.exists(_.startAddr == currentPageBase))
        insertPage(currentPageBase)
      val slice = pages.filter(_.startAddr == currentPageBase)(0).data.slice(
        if(addr < currentPageBase) 0 else (addr - currentPageBase).toInt,
        if(addr + len > currentPageBase + stepSize) stepSize else (addr + len - currentPageBase).toInt
      )
      res = res ++ slice
    }
    res
  }

  def writeMem(addr: BigInt, len: Int, data: Array[Byte], mask: IndexedSeq[Boolean]): Unit = {
    val lower = addr - addr % stepSize
    val upper = (addr + len) - (addr + len) % stepSize
    for(currentPageBase <- lower to upper by stepSize){
      if (!pages.exists(_.startAddr == currentPageBase))
        insertPage(currentPageBase)
      val idx = pages.lastIndexWhere(_.startAddr == currentPageBase)
      val offset_L = if(addr > currentPageBase) (addr % stepSize).toInt else 0
      val offset_R = if(addr + len < currentPageBase + stepSize) ((addr + len) % stepSize).toInt else stepSize
      for(i <- offset_L until offset_R)
        if(mask(i - offset_L))
          pages(idx).data(i) = data(i - offset_L)
    }
  }
}

class MemBox[T <: BaseSV](SV: T) extends Memory(SV.MaxPhyRange, SV) {

  import helper._

  val bytesPerLine = 4

  //val memory = new Memory(SV.MaxPhyRange, SV)

  def loadfile(ptbr: BigInt, metaData: MetaData, datafile: String): MetaData = {
    val file = Source.fromFile(datafile)
    var fileBytes = file.getLines().map(Hex2ByteArray(_, 4)).reduce(_ ++ _)
    for (i <- metaData.buffer_base.indices) {
      val lower = metaData.buffer_base(i)
      val real_size = metaData.buffer_size(i).toInt
      val upper = lower + real_size - (lower + real_size) % SV.PageSize

      if(real_size > 0){
        if (metaData.lds_mem_index.contains(i)) { // dynamic
          tryAllocate(ptbr, lower, real_size)
          writeDataVirtual(ptbr, lower, real_size, fileBytes.take(real_size))
        }
        else { // static
          val alloc_pa = allocateMemory(ptbr, lower, real_size)
          writeDataPhysical(alloc_pa, real_size, fileBytes.take(real_size))
        }
      }
      fileBytes = fileBytes.drop(real_size)
    }
    metaData
  }
}

case class DelayFIFOEntry(
  val opcode: Int,
  val data: BigInt,
  val source: BigInt,
  val size: BigInt,
  val param: BigInt
)
class DelayFIFO[T<: DelayFIFOEntry](val latency: Int, val depth: Int){
  class EntryWithTime(var ttl: Int, val entry: T){
    def step(minpos: Int): Unit = {
      if(ttl < 0) return
      if(ttl > minpos) ttl = ttl - 1 else ttl = minpos
    }
  }

  var ram: Seq[EntryWithTime] = Seq.empty
  def step() = {
    ram.zipWithIndex.foreach{ case(entry, idx) =>
      entry.step(idx)
    }
  }
  def isFull = ram.length == depth
  def canPop = ram.nonEmpty && ram.head.ttl == 0
  def push(in: T) = {
    ram = ram :+ new EntryWithTime(latency, in)
  }
  def pop: T = {
    val out = ram.head
    ram = ram.tail
    out.entry
  }
}

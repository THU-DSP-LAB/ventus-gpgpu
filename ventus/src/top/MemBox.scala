package top
import chisel3._
import scala.io.Source

import chisel3.experimental.BundleLiterals._
import chisel3.experimental.VecLiterals._
import parameters._

object helper{
  def BigInt2ByteArray(n: BigInt, len: Int): Array[Byte] = n.toByteArray.takeRight(len).reverse
  def Hex2ByteArray(hex: String, len: Int): Array[Byte] = BigInt("00" ++ hex, 16).toByteArray.takeRight(len).reverse
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
  var buffer_base = new Array[BigInt](1)
  var buffer_size = new Array[BigInt](1)

  def generateHostReq(i: BigInt, j: BigInt, k: BigInt) = {
    val blockID = (i * kernel_size(1) + j) * kernel_size(2) + k
    (new host2CTA_data).Lit(
      _.host_wg_id -> ("b" + blockID.toString(2) + "0" * CU_ID_WIDTH).U,
      _.host_num_wf -> wg_size.U,
      _.host_wf_size -> wf_size.U,
      _.host_start_pc -> 0.U,
      _.host_vgpr_size_total -> (wg_size * vgprUsage).U,
      _.host_sgpr_size_total -> (wg_size * sgprUsage).U,
      _.host_lds_size_total -> ldsSize.U,
      _.host_gds_size_total -> 0.U,
      _.host_vgpr_size_per_wf -> vgprUsage.U,
      _.host_sgpr_size_per_wf -> sgprUsage.U,
      _.host_gds_baseaddr -> 0.U,
      _.host_pds_baseaddr -> (blockID * pdsSize * wf_size * wg_size).U,
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
      buffer_base = new Array[BigInt](num_buffer.toInt)
      buffer_base.map(_ => parseHex(buf, 64))
      buffer_size = new Array[BigInt](num_buffer.toInt)
      buffer_size.map(_ => parseHex(buf, 64))
    }
  }
}

class MemBox(metafile: String, datafile: String){
  import helper._
  val bytesPerLine = 4
  val metaData = MetaData(metafile)
  val memory = { // 32bit per line file -> byte array memory
    val file = Source.fromFile(datafile)
    file.getLines().map(Hex2ByteArray(_, 4)).reduce(_ ++ _)
  }
  val mem_size = metaData.buffer_size
  val mem_base = mem_size.indices.toArray.map(mem_size.slice(0, _).sum)
  /*
  Word Map:
  0x00    0x04030201
  0x04    0x08070605
  ... ...
  Byte Map:
  [7]   [6]   [5]   [4]   [3]   [2]   [1]   [0]   | Index
 0x08  0x07  0x06  0x05  0x04  0x03  0x02  0x01   | Byte Value

  */
  def readMem(addr: BigInt, len: Int): Array[Byte] = {
    val findBuf = (0 until metaData.num_buffer.toInt).filter(i =>
      addr >= metaData.buffer_base(i) && addr + len <= metaData.buffer_base(i) + metaData.buffer_size(i)
    )
    if(findBuf.isEmpty){
      Array.fill(len)(0.toByte)
    }
    else{
      val paddr = mem_base(findBuf.head) + addr - metaData.buffer_base(findBuf.head)
      memory.slice(paddr.toInt, paddr.toInt + len)
    }
  }
  def writeMem(addr: BigInt, len: Int, data: Array[Byte], mask: IndexedSeq[Boolean]): Unit = { // 1-bit mask <-> 1 byte data
    val findBuf = (0 until metaData.num_buffer.toInt).filter(i =>
      addr >= metaData.buffer_base(i) && addr + len <= metaData.buffer_base(i) + metaData.buffer_size(i)
    )
    if(findBuf.nonEmpty){
      val paddr = mem_base(findBuf.head) + addr - metaData.buffer_base(findBuf.head)
      for (i <- 0 until len){
        if(mask(i))
          memory(paddr.toInt + i) = data(i)
      }
    }
  }
}

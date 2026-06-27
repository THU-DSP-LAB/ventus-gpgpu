/*
 * Copyright (c) 2021-2022 International Innovation Center of Tsinghua University, Shanghai
 * Ventus is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details. */
package L1Cache.ICache

import SRAMTemplate.SRAMTemplate
import chisel3._
import chisel3.util._
import config.config.Parameters

object ICacheDataArray {
  val RefillSliceCount = 8

  def refillSliceWords(blockWords: Int): Int = blockWords / RefillSliceCount
  def refillSliceBits(blockWords: Int, wordLength: Int): Int =
    refillSliceWords(blockWords) * wordLength
}

class ICacheDataReadReq(implicit p: Parameters) extends ICacheBundle {
  val setIdx = UInt(log2Up(NSets).W)
  val sliceIdx = UInt(log2Ceil(ICacheDataArray.RefillSliceCount).W)
}

class ICacheDataReadResp(implicit p: Parameters) extends ICacheBundle {
  val data = Output(Vec(NWays, UInt(ICacheDataArray.refillSliceBits(BlockWords, WordLength).W)))
}

class ICacheDataReadBus(implicit p: Parameters) extends ICacheBundle {
  val req = Decoupled(new ICacheDataReadReq)
  val resp = Flipped(new ICacheDataReadResp)
}

class ICacheDataWriteReq(implicit p: Parameters) extends ICacheBundle {
  val setIdx = UInt(log2Up(NSets).W)
  val sliceIdx = UInt(log2Ceil(ICacheDataArray.RefillSliceCount).W)
  val data = UInt(ICacheDataArray.refillSliceBits(BlockWords, WordLength).W)
  val waymask = if (NWays > 1) Some(UInt(NWays.W)) else None
}

class ICacheDataWriteBus(implicit p: Parameters) extends ICacheBundle {
  val req = Decoupled(new ICacheDataWriteReq)
}

class ICacheDataArray(implicit p: Parameters) extends ICacheModule {
  import ICacheDataArray._

  require(BlockWords % RefillSliceCount == 0, "ICache refill data slices must evenly divide a cache block")
  require((RefillSliceCount & (RefillSliceCount - 1)) == 0, "ICache refill slice count must be power of two")

  private val refillSliceWords = ICacheDataArray.refillSliceWords(BlockWords)
  private val refillSliceBits = ICacheDataArray.refillSliceBits(BlockWords, WordLength)
  require((refillSliceWords & (refillSliceWords - 1)) == 0, "ICache refill slice words must be power of two")

  val io = IO(new Bundle {
    val r = Flipped(new ICacheDataReadBus)
    val w = Flipped(new ICacheDataWriteBus)
  })

  private val dataRows = NSets * RefillSliceCount

  private def dataRowAddr(setIdx: UInt, sliceIdx: UInt): UInt =
    Cat(setIdx, sliceIdx)

  private val wayMems = Seq.tabulate(NWays) { wayIdx =>
    val wayMem = Module(new SRAMTemplate(
      gen = UInt(refillSliceBits.W),
      set = dataRows,
      way = 1,
      shouldReset = false,
      holdRead = false,
      singlePort = false
    ))
    wayMem.suggestName(s"dataWay_$wayIdx")
    wayMem
  }

  private val readRow = dataRowAddr(io.r.req.bits.setIdx, io.r.req.bits.sliceIdx)
  private val writeRow = dataRowAddr(io.w.req.bits.setIdx, io.w.req.bits.sliceIdx)
  private val writeWaymask = io.w.req.bits.waymask.getOrElse(1.U(NWays.W))

  io.r.req.ready := wayMems.map(_.io.r.req.ready).reduce(_ && _)
  io.w.req.ready := (0 until NWays).map { wayIdx =>
    !writeWaymask(wayIdx) || wayMems(wayIdx).io.w.req.ready
  }.reduce(_ && _)

  for (wayIdx <- 0 until NWays) {
    val wayMem = wayMems(wayIdx)

    wayMem.io.r.req.valid := io.r.req.valid
    wayMem.io.r.req.bits.setIdx := readRow

    wayMem.io.w.req.valid := io.w.req.fire && writeWaymask(wayIdx)
    wayMem.io.w.req.bits.setIdx := writeRow
    wayMem.io.w.req.bits.data := VecInit(Seq(io.w.req.bits.data))
  }

  io.r.resp.data := VecInit(wayMems.map(_.io.r.resp.data(0)))
}

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

import SRAMTemplate.{SRAMTemplate, SRAMWriteBus}
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

class ICacheDataArray(implicit p: Parameters) extends ICacheModule {
  import ICacheDataArray._

  require(BlockWords % RefillSliceCount == 0, "ICache refill data slices must evenly divide a cache block")
  require((RefillSliceCount & (RefillSliceCount - 1)) == 0, "ICache refill slice count must be power of two")

  private val refillSliceWords = ICacheDataArray.refillSliceWords(BlockWords)
  private val refillSliceBits = ICacheDataArray.refillSliceBits(BlockWords, WordLength)
  require((refillSliceWords & (refillSliceWords - 1)) == 0, "ICache refill slice words must be power of two")

  val io = IO(new Bundle {
    val r = Flipped(new ICacheDataReadBus)
    val w = Flipped(new SRAMWriteBus(UInt(BlockBits.W), NSets, NWays))
  })

  private val slices = Seq.tabulate(RefillSliceCount) { sliceIdx =>
    val slice = Module(new SRAMTemplate(
      gen = UInt(refillSliceBits.W),
      set = NSets,
      way = NWays,
      shouldReset = false,
      holdRead = false,
      singlePort = false
    ))
    slice.suggestName(s"refillDataSlice_$sliceIdx")
    slice
  }

  io.r.req.ready := VecInit(slices.map(_.io.r.req.ready))(io.r.req.bits.sliceIdx)
  io.w.req.ready := slices.map(_.io.w.req.ready).reduce(_ && _)

  for (sliceIdx <- 0 until RefillSliceCount) {
    val slice = slices(sliceIdx)
    slice.io.r.req.valid := io.r.req.valid && io.r.req.bits.sliceIdx === sliceIdx.U
    slice.io.r.req.bits.setIdx := io.r.req.bits.setIdx
    slice.io.w.req.valid := io.w.req.valid
    slice.io.w.req.bits.setIdx := io.w.req.bits.setIdx
    (slice.io.w.req.bits.waymask, io.w.req.bits.waymask) match {
      case (Some(dst), Some(src)) => dst := src
      case _ =>
    }

    for (wayIdx <- 0 until NWays) {
      val blockWords = io.w.req.bits.data(wayIdx).asTypeOf(Vec(BlockWords, UInt(WordLength.W)))
      val firstWord = sliceIdx * refillSliceWords
      val sliceWords = Seq.tabulate(refillSliceWords) { wordIdx =>
        blockWords(firstWord + wordIdx)
      }
      slice.io.w.req.bits.data(wayIdx) := VecInit(sliceWords).asUInt
    }
  }

  private val readSliceIdx = RegEnable(io.r.req.bits.sliceIdx, 0.U(log2Ceil(RefillSliceCount).W), io.r.req.fire)
  private val readDataBySlice = VecInit(slices.map(_.io.r.resp.data))
  io.r.resp.data := readDataBySlice(readSliceIdx)
}

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

import SRAMTemplate.{SRAMReadBus, SRAMTemplate, SRAMWriteBus}
import chisel3._
import config.config.Parameters

private object ICacheDataArray {
  val RefillSliceCount = 8
}

class ICacheDataArray(implicit p: Parameters) extends ICacheModule {
  import ICacheDataArray._

  require(BlockWords % RefillSliceCount == 0, "ICache refill data slices must evenly divide a cache block")

  private val refillSliceWords = BlockWords / RefillSliceCount
  private val refillSliceBits = refillSliceWords * WordLength

  val io = IO(new Bundle {
    val r = Flipped(new SRAMReadBus(UInt(BlockBits.W), NSets, NWays))
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

  io.r.req.ready := slices.map(_.io.r.req.ready).reduce(_ && _)
  io.w.req.ready := slices.map(_.io.w.req.ready).reduce(_ && _)

  for (sliceIdx <- 0 until RefillSliceCount) {
    val slice = slices(sliceIdx)
    slice.io.r.req.valid := io.r.req.valid
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

  private val readBlocksByWay = Seq.tabulate(NWays) { wayIdx =>
    VecInit(slices.map(_.io.r.resp.data(wayIdx))).asUInt
  }
  io.r.resp.data := VecInit(readBlocksByWay)
}

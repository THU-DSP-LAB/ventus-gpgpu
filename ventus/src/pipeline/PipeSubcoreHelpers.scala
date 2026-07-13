/*
 * Copyright (c) 2021-2022 International Innovation Center of Tsinghua University, Shanghai
 * Ventus is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package pipeline

import chisel3._
import chisel3.util._
import top.parameters._

object PipeSubcoreHelpers {
  def globalWarpIndex(sc: Int, local: Int): Int = {
    if (num_warp_per_subcore == 1) sc else (local << subcore_sel_bits) | sc
  }

  def localWidValue(internalWid: UInt): UInt = {
    if (num_warp_per_subcore == 1) 0.U(1.W)
    else internalWid(depth_warp - 1, subcore_sel_bits)
  }

  def subcoreValue(internalWid: UInt): UInt = {
    if (subcore_sel_bits == 0) 0.U(1.W)
    else internalWid(subcore_sel_bits - 1, 0)
  }

  def internalWid(sc: Int, localWid: UInt): UInt = {
    val localDepthWarp =
      if (num_warp_per_subcore == 1) 1 else log2Ceil(num_warp_per_subcore)
    if (num_warp_per_subcore == 1) sc.U(depth_warp.W)
    else Cat(localWid(localDepthWarp - 1, 0), sc.U(subcore_sel_bits.W))
  }

  def internalWid(sc: UInt, localWid: UInt): UInt = {
    val localDepthWarp =
      if (num_warp_per_subcore == 1) 1 else log2Ceil(num_warp_per_subcore)
    if (num_warp_per_subcore == 1) sc(depth_warp - 1, 0)
    else if (subcore_sel_bits == 0) localWid(localDepthWarp - 1, 0)
    else Cat(localWid(localDepthWarp - 1, 0), sc(subcore_sel_bits - 1, 0))
  }
}

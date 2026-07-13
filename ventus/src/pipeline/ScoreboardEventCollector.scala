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

class ScoreboardSubcoreEvents extends Bundle {
  private val localDepthWarp =
    if (num_warp_per_subcore == 1) 1 else log2Ceil(num_warp_per_subcore)

  val branch_fire = Bool()
  val branch_wid = UInt(localDepthWarp.W)
  val simt_complete_fire = Bool()
  val simt_complete_wid = UInt(localDepthWarp.W)
  val op_col_v_in_fire = Bool()
  val op_col_v_in_wid = UInt(localDepthWarp.W)
  val op_col_v_out_fire = Bool()
  val op_col_v_out_wid = UInt(localDepthWarp.W)
  val op_col_x_in_fire = Bool()
  val op_col_x_in_wid = UInt(localDepthWarp.W)
  val op_col_x_out_fire = Bool()
  val op_col_x_out_wid = UInt(localDepthWarp.W)
  val endprg_fire = Bool()
  val endprg_wid = UInt(localDepthWarp.W)
}

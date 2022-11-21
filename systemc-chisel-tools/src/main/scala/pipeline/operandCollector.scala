/*
 * Copyright (c) 2021-2022 International Innovation Center of Tsinghua University, Shanghai
 * Ventus is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details. */
package pipeline

import chisel3._
import chisel3.util._
import parameters._
import IDecode._

class WriteVecCtrl extends Bundle{
  val wb_wfd_rd=(Vec(num_thread,UInt(xLen.W)))
  val wfd_mask=Vec(num_thread,Bool())
  val wfd=Bool()
  val reg_idxw=UInt(5.W)
  val warp_id=UInt(depth_warp.W)
}
class WriteScalarCtrl extends Bundle{
  val wb_wxd_rd=(UInt(xLen.W))
  val wxd=Bool()
  val reg_idxw=UInt(5.W)
  val warp_id=UInt(depth_warp.W)
}

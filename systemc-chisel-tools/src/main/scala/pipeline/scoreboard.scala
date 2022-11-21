
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
import chisel3.util.{Decoupled, DecoupledIO, MuxLookup, Queue, UIntToOH}
import parameters._
import IDecode._


class CtrlSigs extends Bundle {
  val inst = UInt(32.W)
  val wid = UInt(depth_warp.W)
  val fp = Bool()
  val branch = UInt(2.W)
  val simt_stack = Bool()
  val simt_stack_op = Bool()
  val barrier = Bool()
  val csr = UInt(2.W)
  val reverse = Bool()
  val sel_alu2 = UInt(2.W)
  val sel_alu1 = UInt(2.W)
  val isvec = Bool()
  val sel_alu3 = UInt(2.W)
  val mask=Bool()
  val sel_imm = UInt(3.W)
  val mem_whb = UInt(2.W)
  val mem_unsigned = Bool()
  val alu_fn = UInt(6.W)
  val mem = Bool()
  val mul = Bool()
  val mem_cmd = UInt(2.W)
  val mop = UInt(2.W)
  val reg_idx1 = UInt(5.W)
  val reg_idx2 = UInt(5.W)
  val reg_idx3 = UInt(5.W)
  val reg_idxw = UInt(5.W)
  val wfd = Bool()
  val fence = Bool()
  val sfu = Bool()
  val readmask = Bool()
  val writemask = Bool()
  val wxd = Bool()
  val pc=UInt(32.W)
  //override def cloneType: CtrlSigs.this.type = new CtrlSigs().asInstanceOf[this.type]
}
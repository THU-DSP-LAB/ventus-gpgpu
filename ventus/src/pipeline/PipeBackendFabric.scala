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

class SubcoreWritebackFabricIO extends Bundle {
  val alu_scalar = Flipped(DecoupledIO(new WriteScalarCtrl))
  val fpu_scalar = Flipped(DecoupledIO(new WriteScalarCtrl))
  val lsu_scalar = Flipped(DecoupledIO(new WriteScalarCtrl))
  val csr_scalar = Flipped(DecoupledIO(new WriteScalarCtrl))
  val sfu_scalar = Flipped(DecoupledIO(new WriteScalarCtrl))
  val mul_scalar = Flipped(DecoupledIO(new WriteScalarCtrl))

  val valu_vec = Flipped(DecoupledIO(new WriteVecCtrl))
  val fpu_vec = Flipped(DecoupledIO(new WriteVecCtrl))
  val lsu_vec = Flipped(DecoupledIO(new WriteVecCtrl))
  val sfu_vec = Flipped(DecoupledIO(new WriteVecCtrl))
  val mul_vec = Flipped(DecoupledIO(new WriteVecCtrl))
  val tc_vec = Flipped(DecoupledIO(new WriteVecCtrl))
  val csr_vec = Flipped(DecoupledIO(new WriteVecCtrl))

  val write_scalar = DecoupledIO(new WriteScalarCtrl)
  val write_vec = DecoupledIO(new WriteVecCtrl)
}

/** Preserves the existing 6 scalar / 7 vector slot contract within one subcore. */
class SubcoreWritebackFabric(subcoreId: Int) extends Module {
  val io = IO(new SubcoreWritebackFabricIO)
  private val wb = Module(new Writeback(6, 7, subcoreId))

  private val scalarInputs = Seq(
    io.alu_scalar,
    io.fpu_scalar,
    io.lsu_scalar,
    io.csr_scalar,
    io.sfu_scalar,
    io.mul_scalar
  )
  private val vectorInputs = Seq(
    io.valu_vec,
    io.fpu_vec,
    io.lsu_vec,
    io.sfu_vec,
    io.mul_vec,
    io.tc_vec,
    io.csr_vec
  )

  scalarInputs.zip(wb.io.in_x).foreach { case (in, slot) => slot <> in }
  vectorInputs.zip(wb.io.in_v).foreach { case (in, slot) => slot <> in }
  io.write_scalar <> wb.io.out_x
  io.write_vec <> wb.io.out_v
}

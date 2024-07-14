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

class Classify(expWidth: Int, fracWidth: Int) extends Module {
  val io = IO(new Bundle {
    val in = Input(UInt((1 + expWidth + fracWidth).W))
    val isNegInf = Output(Bool())
    val isNegNormal = Output(Bool())
    val isNegSubnormal = Output(Bool())
    val isNegZero = Output(Bool())
    val isPosZero = Output(Bool())
    val isPosSubnormal = Output(Bool())
    val isPosNormal = Output(Bool())
    val isPosInf = Output(Bool())
    val isSNaN = Output(Bool())
    val isQNaN = Output(Bool())

    val isNaN = Output(Bool())
    val isInf = Output(Bool())
    val isInfOrNaN = Output(Bool())

    val isSubnormal = Output(Bool())
    val isZero = Output(Bool())
    val isSubnormalOrZero = Output(Bool())
  })
  val flpt = io.in.asTypeOf(new FloatPoint(expWidth, fracWidth))
  val (sign, exp, frac) = (flpt.sign, flpt.exp, flpt.frac)

  val isSubnormalOrZero = exp === 0.U
  val fracIsZero = frac === 0.U
  val isInfOrNaN = (~exp).asUInt === 0.U

  io.isNegInf := sign && io.isInf
  io.isNegNormal := sign && !isSubnormalOrZero && !isInfOrNaN
  io.isNegSubnormal := sign && io.isSubnormal
  io.isNegZero := sign && io.isZero

  io.isPosInf := !sign && io.isInf
  io.isPosNormal := !sign && !isSubnormalOrZero && !isInfOrNaN
  io.isPosSubnormal := !sign && io.isSubnormal
  io.isPosZero := !sign && io.isZero

  io.isSNaN := io.isNaN && !frac.head(1)
  io.isQNaN := io.isNaN && frac.head(1).asBool

  io.isNaN := isInfOrNaN && !fracIsZero
  io.isInf := isInfOrNaN && fracIsZero
  io.isInfOrNaN := isInfOrNaN

  io.isSubnormal := isSubnormalOrZero && !fracIsZero
  io.isZero := isSubnormalOrZero && fracIsZero
  io.isSubnormalOrZero := isSubnormalOrZero
}

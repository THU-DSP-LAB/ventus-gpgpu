// This file is part of raytracer.
// Licensed under the BSD 3-clause License.
// See the LICENSE.txt file for details.

package rtcore

import chisel3._
import hardfloat._

/** The compare-and-swap unit is a special case of the RecFNCompareSelect
  * module. It attempts to return the following when no NaN is involved:
  *
  * x_out = max(x_in, y_in) y_out = min(x_in, y_in)
  *
  * TODO: when one of x and y is NaN, what to pass through? Java's Math.min and
  * Math.max propagate NaNs. IEEE-754 specified min/max to suppress NaNs and
  * return a number if possible. SSE is quirky: when one of the operands to
  * min/max is NaN, it returns the RHS
  *
  * Maybe we can add some Chisel elaboration-time configuration to this module
  * to determine which behavior to use.
  */
class CompareAndSwapRecFN extends Module {
  val io = IO(new Bundle {
    val x_in = Input(Bits(33.W))
    val y_in = Input(Bits(33.W))

    val x_out = Output(Bits(33.W))
    val y_out = Output(Bits(33.W))
  })

  val compare_select_unit = Module(
    new RecFNCompareSelect(
      option = true,
      passthrough_type = Bits(33.W)
    )
  )

  compare_select_unit.io.a := io.x_in
  compare_select_unit.io.c := io.x_in

  compare_select_unit.io.b := io.y_in
  compare_select_unit.io.d := io.y_in

  io.x_out := compare_select_unit.io.c_out
  io.y_out := compare_select_unit.io.d_out
}

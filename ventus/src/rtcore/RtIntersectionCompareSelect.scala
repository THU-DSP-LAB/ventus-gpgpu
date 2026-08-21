// This file is part of raytracer.
// Licensed under the BSD 3-clause License.
// See the LICENSE.txt file for details.

package rtcore

import chisel3._
import hardfloat._

/** Given two input Recorded-format Floats `a` and `b`, output either `c` or `d`
  * depending on the result of comparison:
  *
  * c_out = (a > b || (a==b && option)) ? c : d d_out = "the other of c and d"
  *
  * This is a combinatorial circuit. Parameter "option" controls the behavior
  * when a===b.
  */
class RecFNCompareSelect(
    val option: Boolean,
    val passthrough_type: Bits
) extends Module {
  val io = IO(new Bundle {
    // compare these two inputs
    val a = Input(Bits(33.W))
    val b = Input(Bits(33.W))

    // pass-through with/without swapping these two inputs
    val c = Input(passthrough_type)
    val d = Input(passthrough_type)

    // outputs
    val c_out = Output(passthrough_type)
    val d_out = Output(passthrough_type)
  })

  val fu = Module(new CompareRecFN(8, 24))
  fu.io.a := this.io.a
  fu.io.b := this.io.b
  fu.io.signaling := true.B

  when(fu.io.gt) {
    this.io.c_out := this.io.c
    this.io.d_out := this.io.d
  }.elsewhen(option.B && fu.io.eq) {
    this.io.c_out := this.io.c
    this.io.d_out := this.io.d
  }.otherwise {
    this.io.c_out := this.io.d
    this.io.d_out := this.io.c
  }

}

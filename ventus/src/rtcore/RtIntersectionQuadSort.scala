// This file is part of raytracer.
// Licensed under the BSD 3-clause License.
// See the LICENSE.txt file for details.

package rtcore

import chisel3._
import hardfloat._

/** Implements a combinatorial 4-element recorded-format float sorting network
  *
  * The floats are expected to have 33-bits (8+1 bits of exponent, 1 bit of
  * sign, and 23 bits of fraction / 24 bits of significand.)
  *
  * The 0-th element of "out" is largest.
  *
  * Built with the CompareAndSwapRecFN unit.
  */
class QuadSortRecFN extends Module {
  val io = IO(new Bundle {
    val in = Input(Vec(4, Bits(33.W)))
    val out = Output(Vec(4, Bits(33.W)))

    // aliases to out(0) and out(3)
    val largest = Output(Bits(33.W))
    val smallest = Output(Bits(33.W))
  })
  val _input_width = 4

  val cas_sequence = Seq(
    (0, 2),
    (1, 3),
    (0, 1),
    (2, 3),
    (1, 2)
  )

  // the way we're gonna generate the sorting network is as follows:
  // 1. Serialize the compare-and-swap ("cas") sequence (despite some steps
  //    can be parallelized into the same stage). We're building a comb.
  //    circuit so this does not matter
  // 2. For a cas-sequence of length N, declare N-1 echelons of intermediate
  //    value wires, each echelone as wide as the input vector and the output
  //    vector. We also create echelons for the input and output wires, so we
  //    have a total of N+1 echelons
  // 3. The transformation between echelon k and (k+1) is indicated by the
  //    k-th element of the cas-sequence. E.g cas_sequence(0) = (0,2) means
  //      (echelon_1(0), echelon_1(2)) = CAS(echelon_0(0), echelon_0(2))
  //      echelon_1(1) = echelon_0(1)
  //      echelon_1(3) = echelon_0(3)

  class StageElements extends Bundle {
    val stage_elements = Vec(_input_width, Bits(33.W))
  }
  val stages = cas_sequence.length

  val echelons = Wire(Vec(stages + 1, new StageElements()))

  echelons(0).stage_elements := io.in
  io.out := echelons(stages).stage_elements
  io.largest := io.out(0)
  io.smallest := io.out(_input_width - 1)

  cas_sequence.zipWithIndex // (pair_0, 0), (pair_1, 1), ...
    .foreach { case ((cas_0, cas_1), input_stage_idx) =>
      // connect the specified elements to the CAS module
      val cas_unit = Module(new CompareAndSwapRecFN())
      cas_unit.io.x_in := echelons(input_stage_idx).stage_elements(cas_0)
      cas_unit.io.y_in := echelons(input_stage_idx).stage_elements(cas_1)

      echelons(input_stage_idx + 1).stage_elements(cas_0) := cas_unit.io.x_out
      echelons(input_stage_idx + 1).stage_elements(cas_1) := cas_unit.io.y_out

      // printf(cf"stage ${input_stage_idx}: CAS ${cas_0} and ${cas_1}\n")

      // connect irrelevant element directly between echelons
      (0 until _input_width).foreach {
        case n if n == cas_0 || n == cas_1 => // do nothing
        case n => {
          echelons(input_stage_idx + 1).stage_elements(n) := echelons(
            input_stage_idx
          ).stage_elements(n)
          // printf(cf"stage ${input_stage_idx}: directly connecting ${n}\n")
        }
      }
    }

}

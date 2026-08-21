// This file is part of raytracer.
// Licensed under the BSD 3-clause License.
// See the LICENSE.txt file for details.

package rtcore

import chisel3._
import hardfloat._
import chisel3.experimental.VecLiterals._

/** Implements a combinatorial 4-element recorded-format float sorting network
  *
  * The floats are expected to have 33-bits (8+1 bits of exponent, 1 bit of
  * sign, and 23 bits of fraction / 24 bits of significand.)
  *
  * The 0-th element of "out" is largest.
  *
  * Built with the CompareAndSwapRecFN unit.
  */
class QuadSortRecFNWithIndex extends Module {
  val io = IO(new Bundle {
    val in = Input(Vec(4, Bits(33.W)))
    val sorted_indices = Output(Vec(4, UInt(2.W)))
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
    val shuffled_indices = Vec(_input_width, Bits(2.W))
  }
  val stages = cas_sequence.length

  val echelons = Wire(Vec(stages + 1, new StageElements()))

  echelons(0).stage_elements := io.in
  echelons(0).shuffled_indices := Vec.Lit(0.U, 1.U, 2.U, 3.U)
  io.sorted_indices := echelons(stages).shuffled_indices

  cas_sequence.zipWithIndex // (pair_0, 0), (pair_1, 1), ...
    .foreach { case ((cas_0, cas_1), input_stage_idx) =>
      /* Sort the indices */
      val index_unit = Module(new RecFNCompareSelect(true, Bits(35.W)))
      index_unit.io.a := echelons(input_stage_idx).stage_elements(cas_0)
      index_unit.io.b := echelons(input_stage_idx).stage_elements(cas_1)
      index_unit.io.c := echelons(input_stage_idx).shuffled_indices(
        cas_0
      ) ## echelons(input_stage_idx).stage_elements(cas_0)
      index_unit.io.d := echelons(input_stage_idx).shuffled_indices(
        cas_1
      ) ## echelons(input_stage_idx).stage_elements(cas_1)

      echelons(input_stage_idx + 1)
        .shuffled_indices(cas_0) := index_unit.io.c_out(34, 33)
      echelons(input_stage_idx + 1)
        .shuffled_indices(cas_1) := index_unit.io.d_out(34, 33)

      echelons(input_stage_idx + 1)
        .stage_elements(cas_0) := index_unit.io.c_out(32, 0)
      echelons(input_stage_idx + 1)
        .stage_elements(cas_1) := index_unit.io.d_out(32, 0)

      (0 until _input_width).foreach {
        case n if n == cas_0 || n == cas_1 => // do nothing
        case n => {
          echelons(input_stage_idx + 1).shuffled_indices(n) := echelons(
            input_stage_idx
          ).shuffled_indices(n)
          echelons(input_stage_idx + 1).stage_elements(n) := echelons(
            input_stage_idx
          ).stage_elements(n)
        }
      }
    }

}

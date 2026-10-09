// RayFlex-derived floating-point comparison and four-element sorting helpers.
// Consolidated from RtIntersectionFnHelpers.scala, RtIntersectionCompareSelect.scala, RtIntersectionCompareAndSwap.scala, RtIntersectionQuadSort.scala, RtIntersectionQuadSortWithIndex.scala
// Licensed under the BSD 3-clause License; see RAYFLEX_LICENSE.
// Algorithms and public module names are unchanged.
package rtcore

import chisel3._
import hardfloat._
import chisel3.experimental.VecLiterals._

/** Flip the sign bit (MSB) for a floating point or recorded floating point
  * number alike. They both use the MSB to represent sign.
  */
object FNFlipSign {
  def apply(in: Bits): Bits = {
    assert(in.widthKnown)
    val w = in.getWidth

    (~in(w - 1)) ## in(w - 2, 0)
  }
}

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

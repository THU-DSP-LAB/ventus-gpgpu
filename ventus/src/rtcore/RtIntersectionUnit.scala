// Derived from RayFlex RtIntersectionUnit at
// b835680cf66dae2b332bbe5ab8c2cdf16f2edb45.
// Licensed under the BSD 3-clause License; see RAYFLEX_LICENSE.
//
// RtIntersectionUnit is the RTcore entry point for the baseline disjoint
// box/triangle pipeline: each operation receives private functional units.

package rtcore

import chisel3._
import hardfloat._
import chisel3.experimental.VecLiterals._ // for VecLit
import chisel3.util._
import chisel3.experimental.dataview._

case class RtIntersectionParams(
    io_recorded_float: Boolean = false,
    internal_recorded_float: Boolean = true
) {
  val support_euclidean: Option[Int] = None
  val disjointPipes: Boolean = true
}

object RtIntersectionConstants {
  def _zero_RecFN = recFNFromFN(8, 24, 0x0.U)
  def _neg_1_0_RecFN = recFNFromFN(8, 24, 0xbf800000.S)
  def _pos_1_0_RecFN = recFNFromFN(8, 24, 0x3f800000.S)
  def _positive_inf_RecFN = recFNFromFN(8, 24, 0x7f800000.S)
}

class RtIntersectionUnit(p: RtIntersectionParams) extends Module {
  import RtIntersectionConstants._

  assert(
    p.io_recorded_float == false,
    " we don't support recorded-format Input/Output"
  )
  assert(
    p.internal_recorded_float == true,
    " we don't support standard-format intermediates"
  )
  assert(
    p.support_euclidean.isEmpty,
    " baseline disjoint intersection unit does not include euclidean or angular operations"
  )

  // input is guaranteed to be registered by this module
  val in = IO(
    Flipped(
      Decoupled(new EnhancedInputBundle(p))
    )
  )

  // output is also registered by the last stage's output buffer
  val out = IO(
    Decoupled(new EnhancedOutputBundle(p))
  )

  // A cycle counter which we can use when debugging the module
  // It shall disappear in synthesis, so no PPA impact (provided we don't expose
  // _time when instantiating the top-level module)
  val (_time, _) = Counter(true.B, (1 << 31) - 1)
  dontTouch(_time)

  /////////////////
  // SOME CONSTANTS
  /////////////
  val _max_stage_count = 12;
  val _box_plurality =
    in.bits.aabb.length // this is a variable known at Chisel elaboration time
  val _rounding_rule = consts.round_near_even
  val _tininess_rule = consts.tininess_beforeRounding

  /////////////////
  // STAGE BEHAVIOR
  //////////////////

  // Stage dataflow (how to transform input to output) is decoupled from stage control
  // (whether to block for a cycle).

  // Below, the dataflow logic is specified. The stage control is implemented by
  // GeneralizedSkidBufferStage.

  // Each element of this array is an Option, which wraps a function object that
  // maps an input ExtendedPipelineBundle to an output ExtendedPipelineBundle
  val stage_functions = collection.mutable.ArrayBuffer.fill[Option[
    ExtendedPipelineBundle => ExtendedPipelineBundle
  ]](_max_stage_count)(None)

  // Define the functions!
  // stage 0 does not exist
  // stage 1 converts float32 to float33 (specified separately at the bottom, because the
  // function signature is different)

  // stage 2 performs 24 adds for ray-box, or 9 adds for ray-triangle, to
  // translate the geometries to the origin of the ray, or 16 adds for euclidean
  // to find the element-wise difference
  stage_functions(2) = Some({ intake =>
    val emit = Wire(new ExtendedPipelineBundle(p))

    // By default, copy the input. Code below overwrites fields of the bundle.
    emit := intake

    // a list of AddRecFN modules, long enough to support any operation of this stage
    val fu_list: List[AddRecFN] = List.fill(if(!p.disjointPipes) 24 else 400) {
      val fu = Module(new AddRecFN(8, 24))
      fu.io.a := 0.U
      fu.io.b := 0.U
      fu.io.detectTininess := _tininess_rule
      fu.io.roundingMode := _rounding_rule
      fu.io.subOp := false.B
      fu
    }

    switch(intake.opcode) {
      is(RtIntersectionUnitOpCode.OpTriangle) {
        // ray-triangle
        val _dest = Seq(
          emit.triangle.A.x,
          emit.triangle.A.y,
          emit.triangle.A.z,
          emit.triangle.B.x,
          emit.triangle.B.y,
          emit.triangle.B.z,
          emit.triangle.C.x,
          emit.triangle.C.y,
          emit.triangle.C.z
        )

        val _src1 = Seq(
          intake.triangle.A.x,
          intake.triangle.A.y,
          intake.triangle.A.z,
          intake.triangle.B.x,
          intake.triangle.B.y,
          intake.triangle.B.z,
          intake.triangle.C.x,
          intake.triangle.C.y,
          intake.triangle.C.z
        )

        val _src2 = Seq(
          intake.ray.origin.x,
          intake.ray.origin.y,
          intake.ray.origin.z,
          intake.ray.origin.x,
          intake.ray.origin.y,
          intake.ray.origin.z,
          intake.ray.origin.x,
          intake.ray.origin.y,
          intake.ray.origin.z
        )

        val op_fu_list = if (!p.disjointPipes) {
          fu_list
        } else {
          fu_list.drop(0)
        }

        assert(_dest.length <= op_fu_list.length)

        (_dest zip _src1 zip _src2 zip op_fu_list) foreach {
          case (((_1, _2), _3), fu) =>
            fu.io.subOp := true.B
            fu.io.a := _2
            fu.io.b := _3
            _1 := fu.io.out
          // TODO: handle exception flags!
        }
      }
      is(RtIntersectionUnitOpCode.OpQuadbox) {
        // ray-box
        // the following implements the C-code:
        /*
        child.x_min = child.x_min - ray.origin.x;
        child.y_min = child.y_min - ray.origin.y;
        child.z_min = child.z_min - ray.origin.z;
        child.x_max = child.x_max - ray.origin.x;
        child.y_max = child.y_max - ray.origin.y;
        child.z_max = child.z_max - ray.origin.z;
         */
        val _dest = (0 until _box_plurality).flatMap { box_idx =>
          Seq(
            emit.aabb(box_idx).x_min,
            emit.aabb(box_idx).y_min,
            emit.aabb(box_idx).z_min,
            emit.aabb(box_idx).x_max,
            emit.aabb(box_idx).y_max,
            emit.aabb(box_idx).z_max
          )
        }
        val _src1 = (0 until _box_plurality).flatMap { box_idx =>
          Seq(
            intake.aabb(box_idx).x_min,
            intake.aabb(box_idx).y_min,
            intake.aabb(box_idx).z_min,
            intake.aabb(box_idx).x_max,
            intake.aabb(box_idx).y_max,
            intake.aabb(box_idx).z_max
          )
        }
        val _src2 = (0 until _box_plurality).flatMap { box_idx =>
          Seq(
            intake.ray.origin.x,
            intake.ray.origin.y,
            intake.ray.origin.z,
            intake.ray.origin.x,
            intake.ray.origin.y,
            intake.ray.origin.z
          )
        }

        val op_fu_list = if (!p.disjointPipes) {
          fu_list
        } else {
          // in case of disjoint pipes, use own FUs
          fu_list.drop(100)
        }

        assert(_dest.length <= op_fu_list.length)

        (_dest zip _src1 zip _src2 zip op_fu_list) foreach {
          case (((_1, _2), _3), fu) =>
            fu.io.subOp := true.B
            fu.io.a := _2
            fu.io.b := _3
            _1 := fu.io.out
          // TODO: handle exception flags!
        }

      }
      is(RtIntersectionUnitOpCode.OpEuclidean) {
        // if the elaboration parameter `p` does not support euclidean, this
        // block will just be empty.
        if (p.support_euclidean.isDefined) {
          assert(intake.vec_a.length == p.support_euclidean.get)
          // save the difference in euclidean_a
          val _dest = emit.vec_a.getElements
          val _src1 = intake.vec_a.getElements
          val _src2 = intake.vec_b.getElements
          val _mask = intake.vec_mask(0).asBools

          val op_fu_list = if (!p.disjointPipes) {
            fu_list
          } else {
            fu_list.drop(200)
          }

          assert(_dest.length <= op_fu_list.length)
          assert(_mask.length == _dest.length)

          (_src1 zip _src2 zip _dest zip op_fu_list zip _mask) foreach {
            case ((((_1, _2), _3), fu), m) =>
              fu.io.subOp := true.B
              fu.io.a := _1
              fu.io.b := _2

              // output zero if this element is masked-away
              _3 := Mux(m, fu.io.out, _zero_RecFN)
            // TODO: handle exception flags!
          }
        }
      }
      is(RtIntersectionUnitOpCode.OpAngular) {
        if (p.support_euclidean.isDefined) {
          assert(intake.vec_a.length == p.support_euclidean.get)
          val angular_bundle = intake.getAngularJobBundleView()
          val angular_element_count = angular_bundle._angular_element_count
          assert(
            angular_element_count == 8,
            "currently only support 8 elements each in the candidate and query points"
          )
          val _dest = Seq(emit.vec_a.take(8), emit.vec_b.take(8)).flatten
          val _src = Seq(
            angular_bundle.angular_query.getElements,
            angular_bundle.angular_candidate.getElements
          ).flatten
          val _sel =
            angular_bundle.angular_mask.asBools :++ angular_bundle.angular_mask.asBools
          assert(_dest.length == _src.length)
          assert(_dest.length == _sel.length)
          (_dest zip _src zip _sel).foreach { case ((_d, _s), _m) =>
            _d := Mux(_m, _s, _zero_RecFN)
          }
        }
      }
    }

    emit
  })

  // stage 3 performs 24 muls for ray-box to find out the time intersection
  // intervals, or 9 mul for ray-triangle to perform the multiplication step of shearing and scaling of
  // triangle vertices, or 16 muls for euclidean to calculate the square of
  // diffs, or 16 muls for angular to calculate the element-wise product between
  // query point and candidate point, and the query point and itself.
  stage_functions(3) = Some({ intake =>
    val emit = Wire(new ExtendedPipelineBundle(p))
    emit := intake

    val fu_list: List[MulRecFN] = List.fill(if(!p.disjointPipes) 24 else 400) {
      val fu = Module(new MulRecFN(8, 24))
      fu.io.a := 0.U
      fu.io.b := 0.U
      fu.io.detectTininess := _tininess_rule
      fu.io.roundingMode := _rounding_rule
      fu
    }

    switch(intake.opcode) {
      is(RtIntersectionUnitOpCode.OpTriangle) {
        // references to wires from the intake
        val kx = intake.ray.kx
        val ky = intake.ray.ky
        val kz = intake.ray.kz

        val _dest = Seq(
          emit.A.x, // (-ray.shear.x) * A.at(kz)
          emit.A.y, // (-ray.shear.y) * A.at(kz)
          emit.A.z, // ray.shear.z * A.at(kz)
          emit.B.x, // (-ray.shear.x) * B.at(kz)
          emit.B.y, // (-ray.shear.y) * B.at(kz)
          emit.B.z, // ray.shear.z * B.at(kz)
          emit.C.x, // (-ray.shear.x) * C.at(kz)
          emit.C.y, // (-ray.shear.y) * C.at(kz)
          emit.C.z // ray.shear.z * C.at(kz)
        )

        val _src1 = Seq(
          FNFlipSign(intake.ray.shear.x),
          FNFlipSign(intake.ray.shear.y),
          intake.ray.shear.z,
          FNFlipSign(intake.ray.shear.x),
          FNFlipSign(intake.ray.shear.y),
          intake.ray.shear.z,
          FNFlipSign(intake.ray.shear.x),
          FNFlipSign(intake.ray.shear.y),
          intake.ray.shear.z
        )

        val _src2 = Seq(
          intake.triangle.A.at(kz),
          intake.triangle.A.at(kz),
          intake.triangle.A.at(kz),
          intake.triangle.B.at(kz),
          intake.triangle.B.at(kz),
          intake.triangle.B.at(kz),
          intake.triangle.C.at(kz),
          intake.triangle.C.at(kz),
          intake.triangle.C.at(kz)
        )

        val op_fu_list = if (!p.disjointPipes) {
          fu_list
        } else {
          fu_list.drop(0)
        }

        assert(_dest.length <= op_fu_list.length)
        // _dest = _src1 * _src2
        (_src1 zip _src2 zip _dest zip op_fu_list).map {
          case (((_1, _2), _3), fu) =>
            fu.io.a := _1
            fu.io.b := _2
            _3 := fu.io.out
          // handle exception flags!
        }

      }
      is(RtIntersectionUnitOpCode.OpQuadbox) {
        // the following implements the C-code
        /*
        float tp_min_x = child.x_min * ray.inv.x;
        float tp_min_y = child.y_min * ray.inv.y;
        float tp_min_z = child.z_min * ray.inv.z;
        float tp_max_x = child.x_max * ray.inv.x;
        float tp_max_y = child.y_max * ray.inv.y;
        float tp_max_z = child.z_max * ray.inv.z;
         */
        val _dest = (0 until _box_plurality).flatMap { box_idx =>
          Seq(
            emit.t_min(box_idx).x,
            emit.t_min(box_idx).y,
            emit.t_min(box_idx).z,
            emit.t_max(box_idx).x,
            emit.t_max(box_idx).y,
            emit.t_max(box_idx).z
          )
        }
        val _src1 = (0 until _box_plurality).flatMap { box_idx =>
          Seq(
            intake.aabb(box_idx).x_min,
            intake.aabb(box_idx).y_min,
            intake.aabb(box_idx).z_min,
            intake.aabb(box_idx).x_max,
            intake.aabb(box_idx).y_max,
            intake.aabb(box_idx).z_max
          )
        }

        val _src2 = (0 until _box_plurality).flatMap { box_idx =>
          Seq(
            intake.ray.inv.x,
            intake.ray.inv.y,
            intake.ray.inv.z,
            intake.ray.inv.x,
            intake.ray.inv.y,
            intake.ray.inv.z
          )
        }

        val op_fu_list = if (!p.disjointPipes) {
          fu_list
        } else {
          fu_list.drop(100)
        }

        assert(_dest.length <= op_fu_list.length)
        (_src1 zip _src2 zip _dest zip op_fu_list) foreach {
          case (((_1, _2), _3), fu) =>
            fu.io.a := _1
            fu.io.b := _2

            _3 := fu.io.out
          // TODO: handle exception flags!
        }

      }
      is(RtIntersectionUnitOpCode.OpEuclidean) {
        // if the elaboration parameter `p` does not support euclidean, this
        // block will just be empty.
        if (p.support_euclidean.isDefined) {
          assert(intake.vec_a.length == p.support_euclidean.get)

          // save the squares in euclidean_a
          val _dest = emit.vec_a.getElements
          val _src1 = intake.vec_a.getElements
          val _src2 = _src1

          val op_fu_list = if (!p.disjointPipes) {
            fu_list
          } else {
            fu_list.drop(200)
          }

          assert(_dest.length <= op_fu_list.length)

          (_src1 zip _src2 zip _dest zip op_fu_list) foreach {
            case (((_1, _2), _3), fu) =>
              fu.io.a := _1
              fu.io.b := _2
              _3 := fu.io.out
            // TODO: handle exception flags!
          }
        }
      }
      is(RtIntersectionUnitOpCode.OpAngular) {
        if (p.support_euclidean.isDefined) {
          assert(intake.vec_a.length == p.support_euclidean.get)
          val angular_bundle = intake.getAngularJobBundleView()
          val angular_element_count = angular_bundle._angular_element_count
          assert(
            angular_element_count == 8,
            "currently only support 8 elements each in the candidate and query points"
          )

          // elements of query*candidate goes to vec_a, elements of
          // candidate*candidate goes to vec_b
          val _dest = Seq(
            emit.vec_a.take(angular_element_count),
            emit.vec_b.take(angular_element_count)
          ).flatten
          val _src1 = Seq(
            angular_bundle.angular_query.getElements,
            angular_bundle.angular_candidate.getElements
          ).flatten
          val _src2 = Seq(
            angular_bundle.angular_candidate.getElements,
            angular_bundle.angular_candidate.getElements
          ).flatten

          val op_fu_list = if (!p.disjointPipes) {
            fu_list
          } else {
            fu_list.drop(300)
          }

          assert(_dest.length <= op_fu_list.length)

          (_src1 zip _src2 zip _dest zip op_fu_list).foreach {
            case (((_1, _2), _3), fu) =>
              fu.io.a := _1
              fu.io.b := _2
              _3 := fu.io.out
            // TODO: handle exceptions!
          }
        }
      }
    }

    emit
  })

  // stage 4 performs 6 adds for ray-triangle to perform the addition step of
  // shearing and scaling of triangle vertices, 12 CAS and 4 comparisons for ray-box
  // intersection to identify intersecting boxes, or 8 adds for euclidean to
  // reduce the partial sums, or 8 adds for angular to reduce the partial sums
  stage_functions(4) = Some({ intake =>
    val emit = Wire(new ExtendedPipelineBundle(p))
    emit := intake

    val fu_list: List[AddRecFN] = List.fill(if(!p.disjointPipes) 8 else 400) {
      val fu = Module(new AddRecFN(8, 24))
      fu.io.a := 0.U
      fu.io.b := 0.U
      fu.io.detectTininess := _tininess_rule
      fu.io.roundingMode := _rounding_rule
      fu.io.subOp := false.B
      fu
    }

    val comparator_fu_list = Seq.fill(4) {
      val fu = Module(new CompareRecFN(8, 24))
      fu.io.a := 0.U
      fu.io.b := 0.U
      fu.io.signaling := true.B
      fu
    }

    switch(intake.opcode) {
      is(RtIntersectionUnitOpCode.OpTriangle) {
        // the z-components don't need to add anything. In fact, the following
        // 3 lines are redundant since we already let emit:=intake
        emit.A.z := intake.A.z
        emit.B.z := intake.B.z
        emit.C.z := intake.C.z

        // reference to intake wires
        val kx = intake.ray.kx
        val ky = intake.ray.ky
        val kz = intake.ray.kz

        val _dest = Seq(
          emit.A.x, // A.at(kx) + (-ray.shear.x) * A.at(kz)
          emit.A.y, // A.at(ky) + (-ray.shear.y) * A.at(kz)
          emit.B.x, // B.at(kx) + (-ray.shear.x) * B.at(kz)
          emit.B.y, // B.at(ky) + (-ray.shear.y) * B.at(kz)
          emit.C.x, // C.at(kx) + (-ray.shear.x) * C.at(kz)
          emit.C.y // C.at(ky) + (-ray.shear.y) * C.at(kz)
        )

        val _src1 = Seq(
          intake.triangle.A.at(kx),
          intake.triangle.A.at(ky),
          intake.triangle.B.at(kx),
          intake.triangle.B.at(ky),
          intake.triangle.C.at(kx),
          intake.triangle.C.at(ky)
        )

        val _src2 = Seq(
          intake.A.x,
          intake.A.y,
          intake.B.x,
          intake.B.y,
          intake.C.x,
          intake.C.y
        )

        val op_fu_list = if (!p.disjointPipes) {
          fu_list
        } else {
          fu_list.drop(0)
        }

        assert(_dest.length <= op_fu_list.length)
        (_src1 zip _src2 zip _dest zip op_fu_list).map {
          case (((_1, _2), _3), fu) =>
            fu.io.a := _1
            fu.io.b := _2
            _3 := fu.io.out
            fu.io.subOp := false.B
        }
      }
      is(RtIntersectionUnitOpCode.OpQuadbox) {
        // ray-box: find t_min for each box and mark non-intersects with
        // posInfty
        def flip_intervals_if_dir_is_neg(
            c_out: Bits,
            d_out: Bits,
            a: Bits,
            b: Bits,
            c: Bits,
            d: Bits,
            dont_flip_if_equal: Boolean // scala type boolean, not Chisel type!
        ) = {
          val fu = Module(
            new RecFNCompareSelect(
              option = dont_flip_if_equal,
              passthrough_type = Bits(33.W)
            )
          )
          c_out := fu.io.c_out
          d_out := fu.io.d_out
          fu.io.a := a
          fu.io.b := b
          fu.io.c := c
          fu.io.d := d
        }

        val t_min_intermediate =
          Wire(Vec(_box_plurality, new Float3(p.internal_recorded_float)))
        val t_max_intermediate =
          Wire(Vec(_box_plurality, new Float3(p.internal_recorded_float)))
        val tmin_intermediate = Wire(Vec(_box_plurality, Bits(33.W)))
        val tmax_intermediate = Wire(Vec(_box_plurality, Bits(33.W)))

        for (box_idx <- 0 until _box_plurality) {
          flip_intervals_if_dir_is_neg(
            t_min_intermediate(box_idx).x,
            t_max_intermediate(box_idx).x,
            intake.ray.dir.x,
            _zero_RecFN,
            intake.t_min(box_idx).x,
            intake.t_max(box_idx).x,
            true
          )
          flip_intervals_if_dir_is_neg(
            t_min_intermediate(box_idx).y,
            t_max_intermediate(box_idx).y,
            intake.ray.dir.y,
            _zero_RecFN,
            intake.t_min(box_idx).y,
            intake.t_max(box_idx).y,
            true
          )
          flip_intervals_if_dir_is_neg(
            t_min_intermediate(box_idx).z,
            t_max_intermediate(box_idx).z,
            intake.ray.dir.z,
            _zero_RecFN,
            intake.t_min(box_idx).z,
            intake.t_max(box_idx).z,
            true
          )

          // We are using a QuadSort network simply to find the mimimum among
          // four values. Each QuadSort network has 5 comparators, but the
          // compiler will eliminate all unused nodes, so each circuit ends up with
          // just 3 comparators that help identify the minimum input!
          val quad_sort_for_tmin = Module(new QuadSortRecFN())
          quad_sort_for_tmin.io.in(0) := t_min_intermediate(box_idx).x
          quad_sort_for_tmin.io.in(1) := t_min_intermediate(box_idx).y
          quad_sort_for_tmin.io.in(2) := t_min_intermediate(box_idx).z
          quad_sort_for_tmin.io.in(3) := _zero_RecFN
          tmin_intermediate(box_idx) := quad_sort_for_tmin.io.largest

          val quad_sort_for_tmax = Module(new QuadSortRecFN())
          quad_sort_for_tmax.io.in(0) := t_max_intermediate(box_idx).x
          quad_sort_for_tmax.io.in(1) := t_max_intermediate(box_idx).y
          quad_sort_for_tmax.io.in(2) := t_max_intermediate(box_idx).z
          quad_sort_for_tmax.io.in(3) := intake.ray.extent
          tmax_intermediate(box_idx) := quad_sort_for_tmax.io.smallest

          // if there's overlap between [tmin, inf) and (-inf, tmax], we say ray-box
          // intersection happens

          // a reference to one of the comparators
          val comp_tmin_tmax = comparator_fu_list(box_idx)
          comp_tmin_tmax.io.a := tmin_intermediate(box_idx)
          comp_tmin_tmax.io.b := tmax_intermediate(box_idx)
          comp_tmin_tmax.io.signaling := true.B

          emit.isIntersect(box_idx) := comp_tmin_tmax.io.lt
        }

        emit.tmin :=
          tmin_intermediate.zip(emit.isIntersect).map { case (rec_fn, b) =>
            Mux(b, rec_fn, _positive_inf_RecFN)
          }

      }
      is(RtIntersectionUnitOpCode.OpEuclidean) {
        if (p.support_euclidean.isDefined) {
          assert(intake.vec_a.length == p.support_euclidean.get)

          // we add a(idx) and a(idx+8), output the result to a(idx)
          val _dest = emit.vec_a.take(8)
          val _src1 = intake.vec_a.take(8)
          val _src2 = intake.vec_a.drop(8)

          val op_fu_list = if (!p.disjointPipes) {
            fu_list
          } else {
            fu_list.drop(200)
          }

          assert(_dest.length <= op_fu_list.length)
          (_src1 zip _src2 zip _dest zip op_fu_list).foreach {
            case (((_1, _2), _3), fu) =>
              fu.io.a := _1
              fu.io.b := _2
              _3 := fu.io.out
              fu.io.subOp := false.B
          }
        }
      }
      is(RtIntersectionUnitOpCode.OpAngular) {
        if (p.support_euclidean.isDefined) {
          assert(intake.vec_a.length == p.support_euclidean.get)
          val angular_bundle = intake.getAngularJobBundleView()
          val _dest = Seq(
            emit.vec_a.take(4),
            emit.vec_b.take(4)
          ).flatten
          val _src1 = Seq(
            angular_bundle.angular_query.take(4),
            angular_bundle.angular_candidate.take(4)
          ).flatten
          val _src2 = Seq(
            angular_bundle.angular_query.slice(4, 8),
            angular_bundle.angular_candidate.slice(4, 8)
          ).flatten
          assert(_dest.length == _src1.length)
          assert(_dest.length == _src2.length)

          val op_fu_list = if (!p.disjointPipes) {
            fu_list
          } else {
            fu_list.drop(300)
          }

          assert(_dest.length <= op_fu_list.length)

          (_src1 zip _src2 zip _dest zip op_fu_list).foreach {
            case (((_1, _2), _3), fu) =>
              fu.io.a := _1
              fu.io.b := _2
              _3 := fu.io.out
              fu.io.subOp := false.B
          }
        }
      }
    }

    emit
  })

  // stage 5 performs 6 muls for ray-triangle test to figure out the minuends and
  // subtrahends of U, V, W
  stage_functions(5) = Some({ intake =>
    val emit = Wire(new ExtendedPipelineBundle(p))
    emit := intake
    switch(intake.opcode) {
      is(RtIntersectionUnitOpCode.OpTriangle) {
        val _dest = Seq(
          emit.U,
          emit.V,
          emit.W,
          emit.U_subtrahend,
          emit.V_subtrahend,
          emit.W_subtrahend
        )

        val _src1 = Seq(
          intake.C.x,
          intake.A.x,
          intake.B.x,
          intake.C.y,
          intake.A.y,
          intake.B.y
        )

        val _src2 = Seq(
          intake.B.y,
          intake.C.y,
          intake.A.y,
          intake.B.x,
          intake.C.x,
          intake.A.x
        )

        (_dest zip _src1 zip _src2).map { case ((_1, _2), _3) =>
          val fu = Module(new MulRecFN(8, 24))
          fu.io.a := _2
          fu.io.b := _3
          fu.io.roundingMode := _rounding_rule
          fu.io.detectTininess := _tininess_rule

          _1 := fu.io.out
        // handle exception flags!
        }
      }
      is(RtIntersectionUnitOpCode.OpQuadbox) {}
      is(RtIntersectionUnitOpCode.OpEuclidean) {}
      is(RtIntersectionUnitOpCode.OpAngular) {}
    }

    emit
  })

  // stage 6 performs 3 adds for ray-triangle test to find the value of U, V, W,
  // or 4 adds for euclidean, or 4 adds for angular
  stage_functions(6) = Some({ intake =>
    val emit = Wire(new ExtendedPipelineBundle(p))
    emit := intake

    val fu_list = List.fill[AddRecFN](if(!p.disjointPipes) 4 else 400) {
      val fu = Module(new AddRecFN(8, 24))
      fu.io.subOp := false.B
      fu.io.a := 0.U
      fu.io.b := 0.U
      fu.io.roundingMode := _rounding_rule
      fu.io.detectTininess := _tininess_rule

      fu
    }

    switch(intake.opcode) {
      is(RtIntersectionUnitOpCode.OpTriangle) {
        val _dest = Seq(
          emit.U,
          emit.V,
          emit.W
        )
        val _src1 = Seq(
          intake.U,
          intake.V,
          intake.W
        )
        val _src2 = Seq(
          intake.U_subtrahend,
          intake.V_subtrahend,
          intake.W_subtrahend
        )

        val op_fu_list = if (!p.disjointPipes) {
          fu_list
        } else {
          fu_list.drop(0)
        }

        (_dest zip _src1 zip _src2 zip op_fu_list).map {
          case (((_1, _2), _3), fu) =>
            fu.io.subOp := true.B
            fu.io.a := _2
            fu.io.b := _3
            fu.io.roundingMode := _rounding_rule
            fu.io.detectTininess := _tininess_rule

            _1 := fu.io.out
          // handle exception flags!
        }

      }
      is(RtIntersectionUnitOpCode.OpQuadbox) {}
      is(RtIntersectionUnitOpCode.OpEuclidean) {
        if (p.support_euclidean.isDefined) {
          assert(intake.vec_a.length == p.support_euclidean.get)

          // we add a(idx) and a(idx+4), output the result to a(idx)
          val _dest = emit.vec_a.take(4)
          val _src1 = intake.vec_a.take(4)
          val _src2 = intake.vec_a.drop(4)

          val op_fu_list = if (!p.disjointPipes) {
            fu_list
          } else {
            fu_list.drop(200)
          }

          assert(_dest.length <= op_fu_list.length)

          // the zipped seq will be limited in length by the shorted component,
          // so it's 4 elements long
          (_src1 zip _src2 zip _dest zip op_fu_list).foreach {
            case (((_1, _2), _3), fu) =>
              fu.io.a := _1
              fu.io.b := _2
              _3 := fu.io.out
              fu.io.subOp := false.B
          }
        }
      }
      is(RtIntersectionUnitOpCode.OpAngular) {
        if (p.support_euclidean.isDefined) {
          assert(intake.vec_a.length == p.support_euclidean.get)
          val angular_bundle = intake.getAngularJobBundleView()
          val _dest = Seq(
            emit.vec_a.take(2),
            emit.vec_b.take(2)
          ).flatten
          val _src1 = Seq(
            angular_bundle.angular_query.take(2),
            angular_bundle.angular_candidate.take(2)
          ).flatten
          val _src2 = Seq(
            angular_bundle.angular_query.slice(2, 4),
            angular_bundle.angular_candidate.slice(2, 4)
          ).flatten
          assert(_dest.length == _src1.length)
          assert(_dest.length == _src2.length)

          val op_fu_list = if (!p.disjointPipes) {
            fu_list
          } else {
            fu_list.drop(300)
          }

          (_src1 zip _src2 zip _dest zip op_fu_list).foreach {
            case (((_1, _2), _3), fu) =>
              fu.io.a := _1
              fu.io.b := _2
              _3 := fu.io.out
              fu.io.subOp := false.B
            // printf(cf"stage 7 adder a: ${fNFromRecFN(8, 24, fu.io.a)},b: ${fNFromRecFN(8, 24, fu.io.b)},out: ${fNFromRecFN(8, 24, fu.io.out)}\n")

          }
        }
      }

    }

    emit
  })

  // stage 7 calculates U*Az, V*Bz and W*Cz for ray-triangle test
  stage_functions(7) = Some({ intake =>
    val emit = Wire(new ExtendedPipelineBundle(p))
    emit := intake
    switch(intake.opcode) {
      is(RtIntersectionUnitOpCode.OpTriangle) {
        val _dest = Seq(
          emit.U_Az,
          emit.V_Bz,
          emit.W_Cz
        )
        val _src1 = Seq(
          intake.U,
          intake.V,
          intake.W
        )
        val _src2 = Seq(
          intake.A.z,
          intake.B.z,
          intake.C.z
        )
        (_dest zip _src1 zip _src2).map { case ((_1, _2), _3) =>
          val fu = Module(new MulRecFN(8, 24))
          fu.io.a := _2
          fu.io.b := _3
          fu.io.roundingMode := _rounding_rule
          fu.io.detectTininess := _tininess_rule

          _1 := fu.io.out
        // handle exception flags

        }
      }
      is(RtIntersectionUnitOpCode.OpQuadbox) {}
      is(RtIntersectionUnitOpCode.OpEuclidean) {}
      is(RtIntersectionUnitOpCode.OpAngular) {}
    }

    emit
  })

  // stage 8 does two adds for ray-triangle test to find out the partial sum for
  // t_denom and t_num, or two adds for euclidean, or two adds for angular
  stage_functions(8) = Some({ intake =>
    val emit = Wire(new ExtendedPipelineBundle(p))
    emit := intake

    val fu_list = List.fill[AddRecFN](if(!p.disjointPipes) 2 else 400) {
      val fu = Module(new AddRecFN(8, 24))
      fu.io.subOp := false.B
      fu.io.a := 0.U
      fu.io.b := 0.U
      fu.io.roundingMode := _rounding_rule
      fu.io.detectTininess := _tininess_rule

      fu
    }

    switch(intake.opcode) {
      is(RtIntersectionUnitOpCode.OpTriangle) {
        val fu_denom = fu_list(0)
        fu_denom.io.subOp := false.B
        fu_denom.io.a := intake.U
        fu_denom.io.b := intake.V
        emit.t_denom := fu_denom.io.out
        // handle exception flags

        val fu_num = fu_list(1)
        fu_num.io.subOp := false.B
        fu_num.io.a := intake.U_Az
        fu_num.io.b := intake.V_Bz
        emit.t_num := fu_num.io.out
        // handle exception flags

      }
      is(RtIntersectionUnitOpCode.OpQuadbox) {}
      is(RtIntersectionUnitOpCode.OpEuclidean) {
        if (p.support_euclidean.isDefined) {
          assert(intake.vec_a.length == p.support_euclidean.get)

          // we add a(idx) and a(idx+2), output the result to a(idx)
          val _dest = emit.vec_a.take(2)
          val _src1 = intake.vec_a.take(2)
          val _src2 = intake.vec_a.drop(2)

          val op_fu_list = if (!p.disjointPipes) {
            fu_list
          } else {
            fu_list.drop(200)
          }

          assert(_dest.length <= op_fu_list.length)

          // the zipped seq will be limited in length by the shortest component,
          // so it's 2 elements long
          (_src1 zip _src2 zip _dest zip op_fu_list).foreach {
            case (((_1, _2), _3), fu) =>
              fu.io.a := _1
              fu.io.b := _2
              _3 := fu.io.out
              fu.io.subOp := false.B
          }
        }
      }
      is(RtIntersectionUnitOpCode.OpAngular) {
        if (p.support_euclidean.isDefined) {
          // after this stage, the elements in this single beat of angular jobs
          // should have been be fully reduced. The next stage will perform accumulation
          assert(intake.vec_a.length == p.support_euclidean.get)
          val angular_bundle = intake.getAngularJobBundleView()
          val _dest = Seq(
            emit.vec_a.take(1),
            emit.vec_b.take(1)
          ).flatten
          val _src1 = Seq(
            angular_bundle.angular_query.take(1),
            angular_bundle.angular_candidate.take(1)
          ).flatten
          val _src2 = Seq(
            angular_bundle.angular_query.slice(1, 2),
            angular_bundle.angular_candidate.slice(1, 2)
          ).flatten
          assert(_dest.length == _src1.length)
          assert(_dest.length == _src2.length)

          val op_fu_list = if (!p.disjointPipes) {
            fu_list
          } else {
            fu_list.drop(300)
          }

          (_src1 zip _src2 zip _dest zip op_fu_list).foreach {
            case (((_1, _2), _3), fu) =>
              fu.io.a := _1
              fu.io.b := _2
              _3 := fu.io.out
              fu.io.subOp := false.B
          }
        }
      }

    }

    emit
  })

  // stage 9 performs two adds for ray-triangle test, to complete the summation
  // of t_denom and t_num, or a single add for euclidean, or two accumulations
  // for angular
  stage_functions(9) = Some({ intake =>
    val emit = Wire(new ExtendedPipelineBundle(p))
    emit := intake

    val fu_list = List.fill[AddRecFN](if(!p.disjointPipes) 2 else 400) {
      val fu = Module(new AddRecFN(8, 24))
      fu.io.subOp := false.B
      fu.io.a := 0.U
      fu.io.b := 0.U
      fu.io.roundingMode := _rounding_rule
      fu.io.detectTininess := _tininess_rule

      fu
    }

    switch(intake.opcode) {
      is(RtIntersectionUnitOpCode.OpTriangle) {
        val fu_denom = fu_list(0)
        fu_denom.io.subOp := false.B
        fu_denom.io.a := intake.t_denom
        fu_denom.io.b := intake.W
        emit.t_denom := fu_denom.io.out
        // handle exception flags

        val fu_num = fu_list(1)
        fu_num.io.subOp := false.B
        fu_num.io.a := intake.t_num
        fu_num.io.b := intake.W_Cz
        emit.t_num := fu_num.io.out
        // handle exception flags

      }
      is(RtIntersectionUnitOpCode.OpQuadbox) {}
      is(RtIntersectionUnitOpCode.OpEuclidean) {
        if (p.support_euclidean.isDefined) {
          assert(intake.vec_a.length == p.support_euclidean.get)

          // we add a(idx) and a(idx+1), output the result to a(idx)
          val _dest = emit.vec_a.take(1)
          val _src1 = intake.vec_a.take(1)
          val _src2 = intake.vec_a.drop(1)

          val op_fu_list = if (!p.disjointPipes) {
            fu_list
          } else {
            fu_list.drop(200)
          }

          assert(_dest.length <= op_fu_list.length)

          // the zipped seq will be limited in length by the shortest component,
          // so it's 1 element long
          (_src1 zip _src2 zip _dest zip op_fu_list).foreach {
            case (((_1, _2), _3), fu) =>
              fu.io.a := _1
              fu.io.b := _2
              _3 := fu.io.out
              fu.io.subOp := false.B
          }
        }
      }
      is(RtIntersectionUnitOpCode.OpAngular) {
        if (p.support_euclidean.isDefined) {
          assert(intake.vec_a.length == p.support_euclidean.get)
          val dot_product_accumulator = RegInit(0.U.asTypeOf(intake.vec_a(0)))
          val norm_accumulator = RegInit(0.U.asTypeOf(intake.vec_a(0)))

          val angular_bundle = intake.getAngularJobBundleView()
          val _dest = Seq(
            emit.angular_dot_product_accum_val(0),
            emit.angular_norm_accum_val(0)
          )
          val _src1 = Seq(dot_product_accumulator, norm_accumulator)
          val _src2 = Seq(
            angular_bundle.angular_query(0),
            angular_bundle.angular_candidate(0)
          )
          val op_fu_list = if (!p.disjointPipes) {
            fu_list
          } else {
            fu_list.drop(300)
          }
          assert(_dest.length <= op_fu_list.length)
          (_src1 zip _src2 zip _dest zip op_fu_list).foreach {
            case (((_1, _2), _3), fu) =>
              fu.io.a := _1
              fu.io.b := _2
              fu.io.subOp := false.B
              _3 := fu.io.out

            // handle exceptions!
          }

          // reset the accumulators if needed
          when(angular_bundle.angular_reset_accum) {
            dot_product_accumulator := 0.U
            norm_accumulator := 0.U
          }.otherwise {
            dot_product_accumulator := emit.angular_dot_product_accum_val(0)
            norm_accumulator := emit.angular_norm_accum_val(0)
          }
        }
      }
    }

    emit
  })

  // stage 10 performs 2 quad-sort ray-box
  // intersection to sort the order of intersections; or 5
  // comparisons for ray-triangle test to determine validity of intersection; or
  // one add for euclidean to accumulate total sum
  stage_functions(10) = Some({ intake =>
    val emit = Wire(new ExtendedPipelineBundle(p))
    emit := intake

    val comparator_fu_list = Seq.fill(5) {
      val fu = Module(new CompareRecFN(8, 24))
      fu.io.a := 0.U
      fu.io.b := 0.U
      fu.io.signaling := true.B
      fu
    }

    switch(intake.opcode) {
      is(RtIntersectionUnitOpCode.OpTriangle) {
        val _src1 = Seq(
          intake.U,
          intake.V,
          intake.W,
          intake.t_denom,
          intake.t_num
        )
        val _src2 = Seq.fill(5)(_zero_RecFN)

        // a reference to the comparators' list
        val _fu = comparator_fu_list

        (_src1 zip _src2 zip _fu).map { case ((_1, _2), _3) =>
          _3.io.a := _1
          _3.io.b := _2
          _3.io.signaling := true.B
        }

        when(_fu(0).io.lt || _fu(1).io.lt || _fu(2).io.lt || _fu(4).io.lt) {
          // miss if U/V/W/t_num < 0.0f
          emit.triangle_hit := false.B
        }.elsewhen(_fu(3).io.eq) {
          // miss if t_denom == 0.0f
          emit.triangle_hit := false.B
        }.otherwise {
          emit.triangle_hit := true.B
        }

      }
      is(RtIntersectionUnitOpCode.OpQuadbox) {
        // ray-box test
        // sorter outputs from biggest to smallest, so reverse output
        val quad_sort_for_box_index = Module(new QuadSortRecFNWithIndex)
        quad_sort_for_box_index.io.in := intake.tmin
        emit.boxIndex := quad_sort_for_box_index.io.sorted_indices.reverse

        val quad_sort_for_t = Module(new QuadSortRecFN)
        quad_sort_for_t.io.in := intake.tmin
        emit.tmin := quad_sort_for_t.io.out.reverse

        emit.isIntersect.zip(emit.boxIndex).foreach { case (b, idx) =>
          b := intake.isIntersect(idx)
        }

      }
      is(RtIntersectionUnitOpCode.OpEuclidean) {
        if (p.support_euclidean.isDefined) {
          assert(p.support_euclidean.get == intake.vec_a.length)
          val accumulator = RegInit(0.U.asTypeOf(intake.vec_a(0)))
          val accum_adder = Module(new AddRecFN(8, 24))
          accum_adder.io.a := accumulator
          accum_adder.io.b := intake.vec_a(0)
          accum_adder.io.subOp := false.B
          accum_adder.io.detectTininess := _tininess_rule
          accum_adder.io.roundingMode := _rounding_rule

          emit.vec_accum_val(0) := accum_adder.io.out

          // the accumulator is cleared on when intake.vec_reset_accum is
          // asserted
          when(intake.vec_reset_accum(0)) {
            accumulator := 0.U
          }.otherwise {
            accumulator := accum_adder.io.out
          }
        }
      }
      is(RtIntersectionUnitOpCode.OpAngular) {}
    }

    emit
  })

  //////////////////
  // STAGE REGISTERS
  ///////////////////

  // Instantiate the skid-capable pipeline stages and wrap the dataflow logic
  // specified above, or wrap the identity function by default.

  val stage_modules: Seq[
    SkidBufferStageModule[ExtendedPipelineBundle, ExtendedPipelineBundle]
  ] = stage_functions.toSeq.zipWithIndex.map { case (optF, idx) =>
    val stage = optF match {
      case Some(f) =>
        Module(SkidBufferStage(new ExtendedPipelineBundle(p), f))
      case None => Module(SkidBufferStage(new ExtendedPipelineBundle(p)))
    }
    stage.suggestName(s"stage_${idx}")
    stage
  }

  // Chain up the stages and (literally) tie the loose end.
  // We feed all-zero to stage 0. It's okay, because we will be overriding
  // the connections by routing the module input to stage_1_actual, and route
  // stage_1_actual to stage 2
  val _last_stage_emit_port = stage_modules.foldLeft(
    WireDefault(0.U.asTypeOf(Decoupled(new ExtendedPipelineBundle(p))))
  ) { case (w, s) =>
    s.intake :<>= w
    s.emit
  }

  // Need to specify some value for this input signal otherwise the FIRRTL
  // compiler will complain of a "not fully initialized" error.
  // However, this assignment will either be overwritten below (if this is
  // exactly the last meaningful stage), or be optimized away (if this is a
  // stage beyond the stage from which we tap the output)
  _last_stage_emit_port.ready := true.B

  // now that all stages are chained up, overwrite the first stage
  // stage 1 converts FN to RecFN, so we need a more generic SkidBufferStage
  val stage_1_actual_module = Module(
    GenerializedSkidBufferStage(
      new EnhancedInputBundle(p),
      new ExtendedPipelineBundle(p),
      { (input: EnhancedInputBundle) =>
        val output = WireDefault(0.U.asTypeOf(new ExtendedPipelineBundle(p)))
        output.opcode := input.opcode
        output.ray := RayConvertFNtoRecFN(input.ray)
        // output.triangle := Mux(input.opcode===RtIntersectionUnitOpCode.OpTriangle, TriangleConvertFNtoRecFN(input.triangle), 0.U.asTypeOf(output.triangle))
        output.triangle := TriangleConvertFNtoRecFN(Mux(input.opcode===RtIntersectionUnitOpCode.OpTriangle, input.triangle, 0.U.asTypeOf(input.triangle)))
        (output.aabb zip input.aabb).map { case (reg_o, reg_i) =>
          reg_o := AABBConvertFNtoRecFN(Mux(input.opcode===RtIntersectionUnitOpCode.OpQuadbox, reg_i, 0.U.asTypeOf(reg_i)))
        }
        if (p.support_euclidean.isDefined) {
          println("datapath supports euclidean!")
          for (idx <- 0 until p.support_euclidean.get) {
            output.vec_a(idx) := recFNFromFN(8, 24, Mux(input.opcode===RtIntersectionUnitOpCode.OpAngular || input.opcode===RtIntersectionUnitOpCode.OpEuclidean , input.euclidean_a(idx), 0.U))
            output.vec_b(idx) := recFNFromFN(8, 24, Mux(input.opcode===RtIntersectionUnitOpCode.OpAngular || input.opcode===RtIntersectionUnitOpCode.OpEuclidean , input.euclidean_b(idx), 0.U))
          }
          // the two signals (vec_mask and vec_reset_accum) are Vec(1, XXX)
          output.vec_mask := input.euclidean_mask
          output.vec_reset_accum := input.euclidean_reset_accum
        } else println("datapath does not support euclidean!")
        output
      }
    )
  ).suggestName(s"stage_1_actual")

  stage_1_actual_module.intake :<>= in
  stage_modules(2).intake :<>= stage_1_actual_module.emit

  ////////////////////////
  // TAP OUTPUT FROM LAST MEANINGFUL STAGE'S OUTPUT BUFFER
  /////////////////////////

  // the pre-output stage contains special logic: to convert RecFN to FN
  val pre_output_conv_stage = Module(
    GenerializedSkidBufferStage(
      new ExtendedPipelineBundle(p),
      new EnhancedOutputBundle(p),
      { (input: ExtendedPipelineBundle) =>
        val output = Wire(new EnhancedOutputBundle(p))
        output.opcode := input.opcode
        output.tmin_out := input.tmin.map(fNFromRecFN(8, 24, _))
        output.isIntersect := input.isIntersect
        output.boxIndex := input.boxIndex
        output.t_denom := fNFromRecFN(8, 24, input.t_denom)
        output.t_num := fNFromRecFN(8, 24, input.t_num)
        output.triangle_hit := input.triangle_hit
        if (p.support_euclidean.isDefined) {
          output.euclidean_accumulator(0) := fNFromRecFN(
            8,
            24,
            input.vec_accum_val(0)
          )
          output.euclidean_reset_accum(0) := input.vec_reset_accum(0)

          // angular
          output.angular_dot_product(0) := fNFromRecFN(
            8,
            24,
            input.angular_dot_product_accum_val(0)
          )
          output.angular_norm(0) := fNFromRecFN(
            8,
            24,
            input.angular_norm_accum_val(0)
          )
          val angular_bundle = input.getAngularJobBundleView()
          output.angular_reset_accum(0) := angular_bundle.angular_reset_accum
        }
        output
      }
    )
  ).suggestName("stage_for_pre_output_conv")

  // the output stage is simply a skid buffer
  val output_stage = Module(
    SkidBufferStage(
      new EnhancedOutputBundle(p)
    )
  ).suggestName(("stage_for_output"))

  pre_output_conv_stage.intake :<>= stage_modules(10).emit
  output_stage.intake :<>= pre_output_conv_stage.emit
  out :<>= output_stage.emit
}

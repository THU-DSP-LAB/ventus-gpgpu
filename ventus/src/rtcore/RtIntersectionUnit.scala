// This file is part of raytracer.
// Licensed under the BSD 3-clause License; see RAYFLEX_LICENSE.
//
// The RTcore entry point has independent Box4 and Triangle pipelines. They
// retain the RayFlex arithmetic sequence without a shared opcode bundle.

package rtcore

import chisel3._
import chisel3.util._
import hardfloat._

private object RtIntersectionConstants {
  def zero = recFNFromFN(8, 24, 0x0.U)
  def positiveInfinity = recFNFromFN(8, 24, 0x7f800000.S)
  val roundingMode = consts.round_near_even
  val tininessMode = consts.tininess_beforeRounding
}

private[rtcore] class RtBox4PipelineState extends Bundle {
  val ray = new Ray(recorded_float = true)
  val aabb = Vec(4, new AABB(recorded_float = true))
  val tMin = Vec(4, new Float3(recorded_float = true))
  val tMax = Vec(4, new Float3(recorded_float = true))
  val tmin = Vec(4, Bits(33.W))
  val isIntersect = Vec(4, Bool())
  val boxIndex = Vec(4, UInt(2.W))
}

private[rtcore] class RtTrianglePipelineState extends Bundle {
  val ray = new Ray(recorded_float = true)
  val triangle = new Triangle(recorded_float = true)
  val A = new Float3(recorded_float = true)
  val B = new Float3(recorded_float = true)
  val C = new Float3(recorded_float = true)
  val U = Bits(33.W)
  val V = Bits(33.W)
  val W = Bits(33.W)
  val USubtrahend = Bits(33.W)
  val VSubtrahend = Bits(33.W)
  val WSubtrahend = Bits(33.W)
  val UAz = Bits(33.W)
  val VBz = Bits(33.W)
  val WCz = Bits(33.W)
  val tDenom = Bits(33.W)
  val tNum = Bits(33.W)
  val hit = Bool()
}

/** Dedicated four-box pipeline. Box4 skips triangle-only stages. */
class RtBox4Pipeline extends Module {
  import RtIntersectionConstants._

  val in = IO(Flipped(Decoupled(new RtBox4IntersectionInput)))
  val out = IO(Decoupled(new RtBox4IntersectionOutput))

  val convert = Module(
    GenerializedSkidBufferStage(
      new RtBox4IntersectionInput,
      new RtBox4PipelineState,
      { input: RtBox4IntersectionInput =>
        val emit = WireDefault(0.U.asTypeOf(new RtBox4PipelineState))
        emit.ray := RayConvertFNtoRecFN(input.ray)
        emit.aabb.zip(input.aabb).foreach { case (outBox, inBox) =>
          outBox := AABBConvertFNtoRecFN(inBox)
        }
        emit
      }
    )
  ).suggestName("convert")

  val translate = Module(
    SkidBufferStage(new RtBox4PipelineState, { intake: RtBox4PipelineState =>
      val emit = Wire(new RtBox4PipelineState)
      emit := intake
      val units = List.fill(24)(Module(new AddRecFN(8, 24)))
      units.foreach { unit =>
        unit.io.a := 0.U
        unit.io.b := 0.U
        unit.io.subOp := true.B
        unit.io.detectTininess := tininessMode
        unit.io.roundingMode := roundingMode
      }
      val destinations = (0 until 4).flatMap { index =>
        Seq(
          emit.aabb(index).x_min, emit.aabb(index).y_min, emit.aabb(index).z_min,
          emit.aabb(index).x_max, emit.aabb(index).y_max, emit.aabb(index).z_max
        )
      }
      val sources = (0 until 4).flatMap { index =>
        Seq(
          intake.aabb(index).x_min, intake.aabb(index).y_min, intake.aabb(index).z_min,
          intake.aabb(index).x_max, intake.aabb(index).y_max, intake.aabb(index).z_max
        )
      }
      val origins = (0 until 4).flatMap { _ =>
        Seq(
          intake.ray.origin.x, intake.ray.origin.y, intake.ray.origin.z,
          intake.ray.origin.x, intake.ray.origin.y, intake.ray.origin.z
        )
      }
      (destinations zip sources zip origins zip units).foreach {
        case (((destination, source), origin), unit) =>
          unit.io.a := source
          unit.io.b := origin
          destination := unit.io.out
      }
      emit
    })
  ).suggestName("translate")

  val intervals = Module(
    SkidBufferStage(new RtBox4PipelineState, { intake: RtBox4PipelineState =>
      val emit = Wire(new RtBox4PipelineState)
      emit := intake
      val units = List.fill(24)(Module(new MulRecFN(8, 24)))
      units.foreach { unit =>
        unit.io.a := 0.U
        unit.io.b := 0.U
        unit.io.detectTininess := tininessMode
        unit.io.roundingMode := roundingMode
      }
      val destinations = (0 until 4).flatMap { index =>
        Seq(
          emit.tMin(index).x, emit.tMin(index).y, emit.tMin(index).z,
          emit.tMax(index).x, emit.tMax(index).y, emit.tMax(index).z
        )
      }
      val bounds = (0 until 4).flatMap { index =>
        Seq(
          intake.aabb(index).x_min, intake.aabb(index).y_min, intake.aabb(index).z_min,
          intake.aabb(index).x_max, intake.aabb(index).y_max, intake.aabb(index).z_max
        )
      }
      val inverseDirections = (0 until 4).flatMap { _ =>
        Seq(
          intake.ray.inv.x, intake.ray.inv.y, intake.ray.inv.z,
          intake.ray.inv.x, intake.ray.inv.y, intake.ray.inv.z
        )
      }
      (destinations zip bounds zip inverseDirections zip units).foreach {
        case (((destination, bound), inverseDirection), unit) =>
          unit.io.a := bound
          unit.io.b := inverseDirection
          destination := unit.io.out
      }
      emit
    })
  ).suggestName("intervals")

  val reduce = Module(
    SkidBufferStage(new RtBox4PipelineState, { intake: RtBox4PipelineState =>
      val emit = Wire(new RtBox4PipelineState)
      emit := intake

      def selectInterval(
          lowerOut: Bits,
          upperOut: Bits,
          direction: Bits,
          lower: Bits,
          upper: Bits
      ): Unit = {
        val select = Module(new RecFNCompareSelect(true, Bits(33.W)))
        select.io.a := direction
        select.io.b := zero
        select.io.c := lower
        select.io.d := upper
        lowerOut := select.io.c_out
        upperOut := select.io.d_out
      }

      val selectedMin = Wire(Vec(4, new Float3(recorded_float = true)))
      val selectedMax = Wire(Vec(4, new Float3(recorded_float = true)))
      val tmin = Wire(Vec(4, Bits(33.W)))
      val tmax = Wire(Vec(4, Bits(33.W)))
      val comparisons = Seq.fill(4)(Module(new CompareRecFN(8, 24)))

      for (index <- 0 until 4) {
        selectInterval(selectedMin(index).x, selectedMax(index).x, intake.ray.dir.x, intake.tMin(index).x, intake.tMax(index).x)
        selectInterval(selectedMin(index).y, selectedMax(index).y, intake.ray.dir.y, intake.tMin(index).y, intake.tMax(index).y)
        selectInterval(selectedMin(index).z, selectedMax(index).z, intake.ray.dir.z, intake.tMin(index).z, intake.tMax(index).z)

        val minSort = Module(new QuadSortRecFN)
        minSort.io.in(0) := selectedMin(index).x
        minSort.io.in(1) := selectedMin(index).y
        minSort.io.in(2) := selectedMin(index).z
        minSort.io.in(3) := zero
        tmin(index) := minSort.io.largest

        val maxSort = Module(new QuadSortRecFN)
        maxSort.io.in(0) := selectedMax(index).x
        maxSort.io.in(1) := selectedMax(index).y
        maxSort.io.in(2) := selectedMax(index).z
        maxSort.io.in(3) := intake.ray.extent
        tmax(index) := maxSort.io.smallest

        comparisons(index).io.a := tmin(index)
        comparisons(index).io.b := tmax(index)
        comparisons(index).io.signaling := true.B
        emit.isIntersect(index) := comparisons(index).io.lt
        emit.tmin(index) := Mux(comparisons(index).io.lt, tmin(index), positiveInfinity)
      }
      emit
    })
  ).suggestName("reduce")

  val sort = Module(
    SkidBufferStage(new RtBox4PipelineState, { intake: RtBox4PipelineState =>
      val emit = Wire(new RtBox4PipelineState)
      emit := intake
      val indexSort = Module(new QuadSortRecFNWithIndex)
      indexSort.io.in := intake.tmin
      emit.boxIndex := indexSort.io.sorted_indices.reverse
      val timeSort = Module(new QuadSortRecFN)
      timeSort.io.in := intake.tmin
      emit.tmin := timeSort.io.out.reverse
      emit.isIntersect.zip(emit.boxIndex).foreach { case (intersect, index) =>
        intersect := intake.isIntersect(index)
      }
      emit
    })
  ).suggestName("sort")

  val convertOutput = Module(
    GenerializedSkidBufferStage(
      new RtBox4PipelineState,
      new RtBox4IntersectionOutput,
      { intake: RtBox4PipelineState =>
        val emit = Wire(new RtBox4IntersectionOutput)
        emit.tmin := intake.tmin.map(fNFromRecFN(8, 24, _))
        emit.isIntersect := intake.isIntersect
        emit.boxIndex := intake.boxIndex
        emit
      }
    )
  ).suggestName("convert_output")
  val outputBuffer = Module(SkidBufferStage(new RtBox4IntersectionOutput)).suggestName("output")

  convert.intake :<>= in
  translate.intake :<>= convert.emit
  intervals.intake :<>= translate.emit
  reduce.intake :<>= intervals.emit
  sort.intake :<>= reduce.emit
  convertOutput.intake :<>= sort.emit
  outputBuffer.intake :<>= convertOutput.emit
  out :<>= outputBuffer.emit
}

/** Dedicated triangle pipeline retaining the original ten arithmetic stages. */
class RtTrianglePipeline extends Module {
  import RtIntersectionConstants._

  val in = IO(Flipped(Decoupled(new RtTriangleIntersectionInput)))
  val out = IO(Decoupled(new RtTriangleIntersectionOutput))

  val convert = Module(
    GenerializedSkidBufferStage(
      new RtTriangleIntersectionInput,
      new RtTrianglePipelineState,
      { input: RtTriangleIntersectionInput =>
        val emit = WireDefault(0.U.asTypeOf(new RtTrianglePipelineState))
        emit.ray := RayConvertFNtoRecFN(input.ray)
        emit.triangle := TriangleConvertFNtoRecFN(input.triangle)
        emit
      }
    )
  ).suggestName("convert")

  val translate = Module(
    SkidBufferStage(new RtTrianglePipelineState, { intake: RtTrianglePipelineState =>
      val emit = Wire(new RtTrianglePipelineState)
      emit := intake
      val units = List.fill(9)(Module(new AddRecFN(8, 24)))
      units.foreach { unit =>
        unit.io.a := 0.U
        unit.io.b := 0.U
        unit.io.subOp := true.B
        unit.io.detectTininess := tininessMode
        unit.io.roundingMode := roundingMode
      }
      val destinations = Seq(emit.triangle.A.x, emit.triangle.A.y, emit.triangle.A.z, emit.triangle.B.x, emit.triangle.B.y, emit.triangle.B.z, emit.triangle.C.x, emit.triangle.C.y, emit.triangle.C.z)
      val vertices = Seq(intake.triangle.A.x, intake.triangle.A.y, intake.triangle.A.z, intake.triangle.B.x, intake.triangle.B.y, intake.triangle.B.z, intake.triangle.C.x, intake.triangle.C.y, intake.triangle.C.z)
      val origins = Seq(intake.ray.origin.x, intake.ray.origin.y, intake.ray.origin.z, intake.ray.origin.x, intake.ray.origin.y, intake.ray.origin.z, intake.ray.origin.x, intake.ray.origin.y, intake.ray.origin.z)
      (destinations zip vertices zip origins zip units).foreach {
        case (((destination, vertex), origin), unit) =>
          unit.io.a := vertex
          unit.io.b := origin
          destination := unit.io.out
      }
      emit
    })
  ).suggestName("translate")

  val shearScale = Module(
    SkidBufferStage(new RtTrianglePipelineState, { intake: RtTrianglePipelineState =>
      val emit = Wire(new RtTrianglePipelineState)
      emit := intake
      val units = List.fill(9)(Module(new MulRecFN(8, 24)))
      units.foreach { unit =>
        unit.io.a := 0.U
        unit.io.b := 0.U
        unit.io.detectTininess := tininessMode
        unit.io.roundingMode := roundingMode
      }
      val destinations = Seq(emit.A.x, emit.A.y, emit.A.z, emit.B.x, emit.B.y, emit.B.z, emit.C.x, emit.C.y, emit.C.z)
      val shears = Seq(FNFlipSign(intake.ray.shear.x), FNFlipSign(intake.ray.shear.y), intake.ray.shear.z, FNFlipSign(intake.ray.shear.x), FNFlipSign(intake.ray.shear.y), intake.ray.shear.z, FNFlipSign(intake.ray.shear.x), FNFlipSign(intake.ray.shear.y), intake.ray.shear.z)
      val depths = Seq(intake.triangle.A.at(intake.ray.kz), intake.triangle.A.at(intake.ray.kz), intake.triangle.A.at(intake.ray.kz), intake.triangle.B.at(intake.ray.kz), intake.triangle.B.at(intake.ray.kz), intake.triangle.B.at(intake.ray.kz), intake.triangle.C.at(intake.ray.kz), intake.triangle.C.at(intake.ray.kz), intake.triangle.C.at(intake.ray.kz))
      (destinations zip shears zip depths zip units).foreach {
        case (((destination, shear), depth), unit) =>
          unit.io.a := shear
          unit.io.b := depth
          destination := unit.io.out
      }
      emit
    })
  ).suggestName("shear_scale")

  val addShear = Module(
    SkidBufferStage(new RtTrianglePipelineState, { intake: RtTrianglePipelineState =>
      val emit = Wire(new RtTrianglePipelineState)
      emit := intake
      val units = List.fill(6)(Module(new AddRecFN(8, 24)))
      units.foreach { unit =>
        unit.io.a := 0.U
        unit.io.b := 0.U
        unit.io.subOp := false.B
        unit.io.detectTininess := tininessMode
        unit.io.roundingMode := roundingMode
      }
      emit.A.z := intake.A.z
      emit.B.z := intake.B.z
      emit.C.z := intake.C.z
      val destinations = Seq(emit.A.x, emit.A.y, emit.B.x, emit.B.y, emit.C.x, emit.C.y)
      val vertices = Seq(intake.triangle.A.at(intake.ray.kx), intake.triangle.A.at(intake.ray.ky), intake.triangle.B.at(intake.ray.kx), intake.triangle.B.at(intake.ray.ky), intake.triangle.C.at(intake.ray.kx), intake.triangle.C.at(intake.ray.ky))
      val offsets = Seq(intake.A.x, intake.A.y, intake.B.x, intake.B.y, intake.C.x, intake.C.y)
      (destinations zip vertices zip offsets zip units).foreach {
        case (((destination, vertex), offset), unit) =>
          unit.io.a := vertex
          unit.io.b := offset
          destination := unit.io.out
      }
      emit
    })
  ).suggestName("add_shear")

  val edgeProducts = Module(
    SkidBufferStage(new RtTrianglePipelineState, { intake: RtTrianglePipelineState =>
      val emit = Wire(new RtTrianglePipelineState)
      emit := intake
      val units = List.fill(6)(Module(new MulRecFN(8, 24)))
      units.foreach { unit =>
        unit.io.a := 0.U
        unit.io.b := 0.U
        unit.io.detectTininess := tininessMode
        unit.io.roundingMode := roundingMode
      }
      val destinations = Seq(emit.U, emit.V, emit.W, emit.USubtrahend, emit.VSubtrahend, emit.WSubtrahend)
      val left = Seq(intake.C.x, intake.A.x, intake.B.x, intake.C.y, intake.A.y, intake.B.y)
      val right = Seq(intake.B.y, intake.C.y, intake.A.y, intake.B.x, intake.C.x, intake.A.x)
      (destinations zip left zip right zip units).foreach {
        case (((destination, lhs), rhs), unit) =>
          unit.io.a := lhs
          unit.io.b := rhs
          destination := unit.io.out
      }
      emit
    })
  ).suggestName("edge_products")

  val edgeSubtract = Module(
    SkidBufferStage(new RtTrianglePipelineState, { intake: RtTrianglePipelineState =>
      val emit = Wire(new RtTrianglePipelineState)
      emit := intake
      val units = List.fill(3)(Module(new AddRecFN(8, 24)))
      units.foreach { unit =>
        unit.io.a := 0.U
        unit.io.b := 0.U
        unit.io.subOp := true.B
        unit.io.detectTininess := tininessMode
        unit.io.roundingMode := roundingMode
      }
      val destinations = Seq(emit.U, emit.V, emit.W)
      val products = Seq(intake.U, intake.V, intake.W)
      val subtrahends = Seq(intake.USubtrahend, intake.VSubtrahend, intake.WSubtrahend)
      (destinations zip products zip subtrahends zip units).foreach {
        case (((destination, product), subtrahend), unit) =>
          unit.io.a := product
          unit.io.b := subtrahend
          destination := unit.io.out
      }
      emit
    })
  ).suggestName("edge_subtract")

  val depthProducts = Module(
    SkidBufferStage(new RtTrianglePipelineState, { intake: RtTrianglePipelineState =>
      val emit = Wire(new RtTrianglePipelineState)
      emit := intake
      val units = List.fill(3)(Module(new MulRecFN(8, 24)))
      units.foreach { unit =>
        unit.io.a := 0.U
        unit.io.b := 0.U
        unit.io.detectTininess := tininessMode
        unit.io.roundingMode := roundingMode
      }
      val destinations = Seq(emit.UAz, emit.VBz, emit.WCz)
      val weights = Seq(intake.U, intake.V, intake.W)
      val depths = Seq(intake.A.z, intake.B.z, intake.C.z)
      (destinations zip weights zip depths zip units).foreach {
        case (((destination, weight), depth), unit) =>
          unit.io.a := weight
          unit.io.b := depth
          destination := unit.io.out
      }
      emit
    })
  ).suggestName("depth_products")

  val partialSums = Module(
    SkidBufferStage(new RtTrianglePipelineState, { intake: RtTrianglePipelineState =>
      val emit = Wire(new RtTrianglePipelineState)
      emit := intake
      val units = List.fill(2)(Module(new AddRecFN(8, 24)))
      units.foreach { unit =>
        unit.io.a := 0.U
        unit.io.b := 0.U
        unit.io.subOp := false.B
        unit.io.detectTininess := tininessMode
        unit.io.roundingMode := roundingMode
      }
      units(0).io.a := intake.U
      units(0).io.b := intake.V
      emit.tDenom := units(0).io.out
      units(1).io.a := intake.UAz
      units(1).io.b := intake.VBz
      emit.tNum := units(1).io.out
      emit
    })
  ).suggestName("partial_sums")

  val finalSums = Module(
    SkidBufferStage(new RtTrianglePipelineState, { intake: RtTrianglePipelineState =>
      val emit = Wire(new RtTrianglePipelineState)
      emit := intake
      val units = List.fill(2)(Module(new AddRecFN(8, 24)))
      units.foreach { unit =>
        unit.io.a := 0.U
        unit.io.b := 0.U
        unit.io.subOp := false.B
        unit.io.detectTininess := tininessMode
        unit.io.roundingMode := roundingMode
      }
      units(0).io.a := intake.tDenom
      units(0).io.b := intake.W
      emit.tDenom := units(0).io.out
      units(1).io.a := intake.tNum
      units(1).io.b := intake.WCz
      emit.tNum := units(1).io.out
      emit
    })
  ).suggestName("final_sums")

  val classify = Module(
    SkidBufferStage(new RtTrianglePipelineState, { intake: RtTrianglePipelineState =>
      val emit = Wire(new RtTrianglePipelineState)
      emit := intake
      val comparisons = Seq.fill(5)(Module(new CompareRecFN(8, 24)))
      val values = Seq(intake.U, intake.V, intake.W, intake.tDenom, intake.tNum)
      comparisons.zip(values).foreach { case (comparison, value) =>
        comparison.io.a := value
        comparison.io.b := zero
        comparison.io.signaling := true.B
      }
      emit.hit := !(comparisons(0).io.lt || comparisons(1).io.lt || comparisons(2).io.lt || comparisons(4).io.lt || comparisons(3).io.eq)
      emit
    })
  ).suggestName("classify")

  val convertOutput = Module(
    GenerializedSkidBufferStage(
      new RtTrianglePipelineState,
      new RtTriangleIntersectionOutput,
      { intake: RtTrianglePipelineState =>
        val emit = Wire(new RtTriangleIntersectionOutput)
        emit.tNum := fNFromRecFN(8, 24, intake.tNum)
        emit.tDenom := fNFromRecFN(8, 24, intake.tDenom)
        emit.hit := intake.hit
        emit
      }
    )
  ).suggestName("convert_output")
  val outputBuffer = Module(SkidBufferStage(new RtTriangleIntersectionOutput)).suggestName("output")

  convert.intake :<>= in
  translate.intake :<>= convert.emit
  shearScale.intake :<>= translate.emit
  addShear.intake :<>= shearScale.emit
  edgeProducts.intake :<>= addShear.emit
  edgeSubtract.intake :<>= edgeProducts.emit
  depthProducts.intake :<>= edgeSubtract.emit
  partialSums.intake :<>= depthProducts.emit
  finalSums.intake :<>= partialSums.emit
  classify.intake :<>= finalSums.emit
  convertOutput.intake :<>= classify.emit
  outputBuffer.intake :<>= convertOutput.emit
  out :<>= outputBuffer.emit
}

/** Physically independent Box4 and Triangle intersection entry point. */
class RtIntersectionUnit extends Module {
  val boxIn = IO(Flipped(Decoupled(new RtBox4IntersectionInput)))
  val triangleIn = IO(Flipped(Decoupled(new RtTriangleIntersectionInput)))
  val boxOut = IO(Decoupled(new RtBox4IntersectionOutput))
  val triangleOut = IO(Decoupled(new RtTriangleIntersectionOutput))

  val boxPipeline = Module(new RtBox4Pipeline).suggestName("box4")
  val trianglePipeline = Module(new RtTrianglePipeline).suggestName("triangle")

  boxPipeline.in :<>= boxIn
  boxOut :<>= boxPipeline.out
  trianglePipeline.in :<>= triangleIn
  triangleOut :<>= trianglePipeline.out
}

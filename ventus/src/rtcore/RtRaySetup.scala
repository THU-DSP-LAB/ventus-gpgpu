package rtcore

import chisel3._
import chisel3.util._
import hardfloat._
import top.parameters._

/** Raw per-lane state consumed from the fifteen RT Local SRAM ray-state fields. */
class RtRaySetupInput extends Bundle {
  val warpId = UInt(RtCorePortParameters.residentWarpIdWidth.W)
  val laneId = UInt(log2Ceil(num_thread).W)
  val rayState = new RtRayState(recorded_float = false)
}

/** RayFlex-compatible ray parameters derived once for an RT Local SRAM row. */
class RtRaySetupOutput extends Bundle {
  val warpId = UInt(RtCorePortParameters.residentWarpIdWidth.W)
  val laneId = UInt(log2Ceil(num_thread).W)
  val ray = new Ray(recorded_float = false)
}

/**
  * Converts raw RT Local SRAM ray state into the prepared ray consumed by the two
 * intersection pipelines.  Shader SFUs precompute the three reciprocal
 * values and store them in RT Local SRAM before publishing the trace request;
 * this module only performs axis selection and the two shear multiplies.
  */
class RtRaySetup extends Module {
  private val rounding = consts.round_near_even
  private val tininess = consts.tininess_beforeRounding

  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new RtRaySetupInput))
    val out = Decoupled(new RtRaySetupOutput)
  })

  val saved = Reg(new RtRaySetupInput)
  val savedValid = RegInit(false.B)
  val outValid = RegInit(false.B)
  val outBits = Reg(new RtRaySetupOutput)
  // `saved` is the arithmetic stage; `outBits` is its elastic writeback
  // stage.  A vacated writeback slot admits the next lane in the same cycle,
  // so the unit has a one-cycle initiation interval without duplicating its
  // two floating-point multipliers.
  val advance = !outValid || io.out.ready
  io.in.ready := !savedValid || advance
  io.out.valid := outValid
  io.out.bits := outBits
  when(io.in.fire) {
    saved := io.in.bits
  }

  val direction = Wire(Vec(3, UInt(32.W)))
  direction(0) := saved.rayState.dir.x.asUInt
  direction(1) := saved.rayState.dir.y.asUInt
  direction(2) := saved.rayState.dir.z.asUInt
  val magnitude = VecInit(direction.map(_(30, 0)))
  val kz = Wire(UInt(2.W))
  kz := Mux(magnitude(0) >= magnitude(1) && magnitude(0) >= magnitude(2), 0.U,
    Mux(magnitude(1) >= magnitude(2), 1.U, 2.U))
  val positiveKx = Mux(kz === 2.U, 0.U(2.W), kz + 1.U)
  val positiveKy = Mux(positiveKx === 2.U, 0.U(2.W), positiveKx + 1.U)
  val negativeDirection = direction(kz)(31)
  val kx = Mux(negativeDirection, positiveKy, positiveKx)
  val ky = Mux(negativeDirection, positiveKx, positiveKy)
  val reciprocalVec = Wire(Vec(3, UInt(32.W)))
  reciprocalVec(0) := saved.rayState.inv.x
  reciprocalVec(1) := saved.rayState.inv.y
  reciprocalVec(2) := saved.rayState.inv.z

  val shearX = Module(new MulRecFN(8, 24))
  val shearY = Module(new MulRecFN(8, 24))
  Seq(shearX, shearY).foreach { multiply =>
    multiply.io.detectTininess := tininess
    multiply.io.roundingMode := rounding
  }
  shearX.io.a := recFNFromFN(8, 24, direction(kx))
  shearX.io.b := recFNFromFN(8, 24, reciprocalVec(kz))
  shearY.io.a := recFNFromFN(8, 24, direction(ky))
  shearY.io.b := recFNFromFN(8, 24, reciprocalVec(kz))

  when(advance) {
    outValid := savedValid
    savedValid := io.in.fire
  }.otherwise {
    savedValid := savedValid || io.in.fire
  }
  when(advance && savedValid) {
    outBits.warpId := saved.warpId
    outBits.laneId := saved.laneId
    outBits.ray.origin := saved.rayState.origin
    outBits.ray.dir := saved.rayState.dir
    outBits.ray.inv.x := reciprocalVec(0)
    outBits.ray.inv.y := reciprocalVec(1)
    outBits.ray.inv.z := reciprocalVec(2)
    outBits.ray.originalTmin := saved.rayState.tmin
    outBits.ray.extent := saved.rayState.tmax
    outBits.ray.kx := kx
    outBits.ray.ky := ky
    outBits.ray.kz := kz
    outBits.ray.shear.x := fNFromRecFN(8, 24, shearX.io.out)
    outBits.ray.shear.y := fNFromRecFN(8, 24, shearY.io.out)
    outBits.ray.shear.z := reciprocalVec(kz)
  }
}

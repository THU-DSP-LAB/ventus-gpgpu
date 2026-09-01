package rtcore

import chisel3._
import chisel3.util._
import hardfloat._
import top.parameters._

/** Raw per-lane state consumed from the twelve RT PDS ray-state fields. */
class RtRaySetupInput extends Bundle {
  val warpId = UInt(RtCorePortParameters.residentWarpIdWidth.W)
  val laneId = UInt(log2Ceil(num_thread).W)
  val accelLo = UInt(xLen.W)
  val accelHi = UInt(xLen.W)
  val traceMeta0 = UInt(xLen.W)
  val traceMeta1 = UInt(xLen.W)
  val rayState = new RtRayState(recorded_float = false)
}

/** RayFlex-compatible ray parameters derived once for a PDS ray-state row. */
class RtRaySetupOutput extends Bundle {
  val warpId = UInt(RtCorePortParameters.residentWarpIdWidth.W)
  val laneId = UInt(log2Ceil(num_thread).W)
  val accelLo = UInt(xLen.W)
  val accelHi = UInt(xLen.W)
  val traceMeta0 = UInt(xLen.W)
  val traceMeta1 = UInt(xLen.W)
  val ray = new Ray(recorded_float = false)
}

/**
  * Converts raw PDS ray state into the prepared ray consumed by the two
  * intersection pipelines.  The three reciprocal operations share one input
  * transaction and complete together; the remaining axis selection and shear
  * multiplies are combinational from those reciprocal results.
  */
class RtRaySetup extends Module {
  private val rounding = consts.round_near_even
  private val tininess = consts.tininess_beforeRounding
  private val one = recFNFromFN(8, 24, "h3f800000".U(32.W))

  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new RtRaySetupInput))
    val out = Decoupled(new RtRaySetupOutput)
  })

  val saved = Reg(new RtRaySetupInput)
  val busy = RegInit(false.B)
  val outValid = RegInit(false.B)
  val outBits = Reg(new RtRaySetupOutput)
  val reciprocalBits = Reg(Vec(3, UInt(32.W)))
  val reciprocalReturned = RegInit(0.U(3.W))

  val dividers = Seq.fill(3)(Module(new DivSqrtRecFN_small(8, 24, 0)))
  val dividerReady = dividers.map(_.io.inReady).reduce(_ && _)
  dividers.zip(Seq(io.in.bits.rayState.dir.x, io.in.bits.rayState.dir.y,
    io.in.bits.rayState.dir.z)).foreach { case (divider, direction) =>
    divider.io.inValid := io.in.fire
    divider.io.sqrtOp := false.B
    divider.io.a := one
    divider.io.b := recFNFromFN(8, 24, direction)
    divider.io.roundingMode := rounding
    divider.io.detectTininess := tininess
  }

  io.in.ready := !busy && !outValid && dividerReady
  io.out.valid := outValid
  io.out.bits := outBits
  when(io.out.fire) { outValid := false.B }
  when(io.in.fire) {
    saved := io.in.bits
    busy := true.B
    reciprocalReturned := 0.U
  }

  val reciprocalValid = VecInit(dividers.map(_.io.outValid_div)).asUInt
  val reciprocal = dividers.map(divider => fNFromRecFN(8, 24, divider.io.out))
  when(busy && reciprocalValid.orR) {
    reciprocalReturned := reciprocalReturned | reciprocalValid
  }
  for (index <- reciprocal.indices) {
    when(busy && reciprocalValid(index)) {
      reciprocalBits(index) := reciprocal(index)
    }
  }
  val reciprocalComplete = reciprocalReturned | reciprocalValid
  val completedReciprocal = Wire(Vec(3, UInt(32.W)))
  for (index <- completedReciprocal.indices) {
    completedReciprocal(index) := Mux(reciprocalValid(index), reciprocal(index), reciprocalBits(index))
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
  reciprocalVec := completedReciprocal

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

  when(busy && reciprocalComplete.andR) {
    outBits.warpId := saved.warpId
    outBits.laneId := saved.laneId
    outBits.accelLo := saved.accelLo
    outBits.accelHi := saved.accelHi
    outBits.traceMeta0 := saved.traceMeta0
    outBits.traceMeta1 := saved.traceMeta1
    outBits.ray.origin := saved.rayState.origin
    outBits.ray.dir := saved.rayState.dir
    outBits.ray.inv.x := reciprocal(0)
    outBits.ray.inv.y := reciprocal(1)
    outBits.ray.inv.z := reciprocal(2)
    outBits.ray.extent := saved.rayState.tmax
    outBits.ray.kx := kx
    outBits.ray.ky := ky
    outBits.ray.kz := kz
    outBits.ray.shear.x := fNFromRecFN(8, 24, shearX.io.out)
    outBits.ray.shear.y := fNFromRecFN(8, 24, shearY.io.out)
    outBits.ray.shear.z := reciprocalVec(kz)
    busy := false.B
    outValid := true.B
  }
}

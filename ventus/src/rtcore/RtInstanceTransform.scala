package rtcore

import chisel3._
import chisel3.util._
import hardfloat._
import top.parameters._

/** Input to the TLAS Instance boundary transform.
  *
  * The matrix is the VTAS 3x4 world-to-object matrix in row-major order.
  * This datapath deliberately owns only three multipliers: one complete dot
  * product is issued per cycle, so origin rows 0..2 and direction rows 0..2
  * take six cycles.  The three reciprocal engines then run in parallel.
  */
class RtInstanceTransformInput extends Bundle {
  val laneId = UInt(log2Ceil(num_thread).W)
  val ray = new Ray(recorded_float = false)
  val matrix = Vec(12, UInt(32.W))
}

class RtInstanceTransformOutput extends Bundle {
  val laneId = UInt(log2Ceil(num_thread).W)
  val rayState = new RtRayState(recorded_float = false)
}

class RtInstanceTransform extends Module {
  private val rounding = consts.round_near_even
  private val tininess = consts.tininess_beforeRounding
  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new RtInstanceTransformInput))
    val out = Decoupled(new RtInstanceTransformOutput)
  })

  val saved = Reg(new RtInstanceTransformInput)
  val transformedOrigin = Reg(new Float3(recorded_float = false))
  val transformedDirection = Reg(new Float3(recorded_float = false))
  val reciprocal = Reg(Vec(3, UInt(32.W)))
  val reciprocalReturned = RegInit(0.U(3.W))
  val phase = RegInit(0.U(3.W))
  val busy = RegInit(false.B)
  val divisorsIssued = RegInit(false.B)
  val outValid = RegInit(false.B)
  val outBits = Reg(new RtInstanceTransformOutput)

  val mul = Seq.fill(3)(Module(new MulRecFN(8, 24)))
  mul.foreach { unit =>
    unit.io.roundingMode := rounding
    unit.io.detectTininess := tininess
  }
  val add0 = Module(new AddRecFN(8, 24))
  val add1 = Module(new AddRecFN(8, 24))
  val addTranslation = Module(new AddRecFN(8, 24))
  Seq(add0, add1, addTranslation).foreach { unit =>
    unit.io.roundingMode := rounding
    unit.io.detectTininess := tininess
    unit.io.subOp := false.B
  }

  // phase 0..2 are origin rows, 3..5 direction rows.  Each multiplier is
  // reused for columns 0/1/2 of the selected row.
  // Phase 3 is binary 011, so its MSB alone is not a direction selector.
  // Use the phase boundary itself: 0..2 are origin rows, 3..5 direction.
  val directionRow = phase >= 3.U
  // phase 0..2 visits origin rows directly.  Direction starts at phase 3,
  // so it must be rebased before indexing the same three matrix rows:
  // phases 3/4/5 map to rows 0/1/2, not to the low-bit sequence 3/0/1.
  // The former accidentally indexed past this 3x4 matrix for phase 3.
  val row = Mux(directionRow, phase - 3.U, phase)(1, 0)
  val vector = Wire(Vec(3, UInt(32.W)))
  vector(0) := Mux(directionRow, saved.ray.dir.x, saved.ray.origin.x)
  vector(1) := Mux(directionRow, saved.ray.dir.y, saved.ray.origin.y)
  vector(2) := Mux(directionRow, saved.ray.dir.z, saved.ray.origin.z)
  val matrixBase = (row * 4.U)(3, 0)
  for (column <- 0 until 3) {
    mul(column).io.a := recFNFromFN(8, 24, saved.matrix((matrixBase + column.U)(3, 0)))
    mul(column).io.b := recFNFromFN(8, 24, vector(column))
  }
  add0.io.a := mul(0).io.out
  add0.io.b := mul(1).io.out
  add1.io.a := add0.io.out
  add1.io.b := mul(2).io.out
  addTranslation.io.a := add1.io.out
  addTranslation.io.b := recFNFromFN(8, 24, saved.matrix((matrixBase + 3.U)(3, 0)))
  val dotResult = Mux(directionRow, fNFromRecFN(8, 24, add1.io.out),
    fNFromRecFN(8, 24, addTranslation.io.out))

  val dividers = Seq.fill(3)(Module(new RtFpDiv))
  dividers.zip(Seq(transformedDirection.x, transformedDirection.y,
    transformedDirection.z)).foreach { case (divider, direction) =>
    divider.io.in.valid := busy && phase === 6.U && !divisorsIssued
    divider.io.in.bits.a := "h3f800000".U
    divider.io.in.bits.b := direction
    divider.io.out.ready := true.B
  }
  val returnedNow = VecInit(dividers.map(_.io.out.valid)).asUInt

  io.in.ready := !busy && !outValid
  io.out.valid := outValid
  io.out.bits := outBits
  when(io.out.fire) { outValid := false.B }
  when(io.in.fire) {
    saved := io.in.bits
    phase := 0.U
    busy := true.B
    divisorsIssued := false.B
    reciprocalReturned := 0.U
  }
  when(busy && phase <= 5.U) {
    when(directionRow) {
      when(row === 0.U) { transformedDirection.x := dotResult }
      when(row === 1.U) { transformedDirection.y := dotResult }
      when(row === 2.U) { transformedDirection.z := dotResult }
    }.otherwise {
      when(row === 0.U) { transformedOrigin.x := dotResult }
      when(row === 1.U) { transformedOrigin.y := dotResult }
      when(row === 2.U) { transformedOrigin.z := dotResult }
    }
    phase := phase + 1.U
  }
  when(busy && phase === 6.U) {
    when(!divisorsIssued && dividers.map(_.io.in.ready).reduce(_ && _)) {
      divisorsIssued := true.B
    }
    when(returnedNow.orR) {
      reciprocalReturned := reciprocalReturned | returnedNow
      for (index <- 0 until 3) when(returnedNow(index)) {
        reciprocal(index) := dividers(index).io.out.bits
      }
    }
    when((reciprocalReturned | returnedNow).andR) {
      outBits.laneId := saved.laneId
      outBits.rayState.origin := transformedOrigin
      outBits.rayState.dir := transformedDirection
      outBits.rayState.tmin := saved.ray.originalTmin
      outBits.rayState.tmax := saved.ray.extent
      outBits.rayState.inv.x := Mux(returnedNow(0), dividers(0).io.out.bits, reciprocal(0))
      outBits.rayState.inv.y := Mux(returnedNow(1), dividers(1).io.out.bits, reciprocal(1))
      outBits.rayState.inv.z := Mux(returnedNow(2), dividers(2).io.out.bits, reciprocal(2))
      busy := false.B
      outValid := true.B
    }
  }
}

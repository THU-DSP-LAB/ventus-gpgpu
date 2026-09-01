package rtcore

import chisel3._
import chisel3.util._
import hardfloat._

/** Final IEEE divisions for an accepted triangle hit. */
class RtTriangleResolveResult extends Bundle {
  val requestId = UInt(RtCorePortParameters.requestIdWidth.W)
  val u = UInt(32.W)
  val v = UInt(32.W)
  val t = UInt(32.W)
}

/**
  * Triangle intersection deliberately leaves barycentric/depth division out of
  * the high-throughput RayFlex pipeline.  The scheduler sends only accepted
  * hits here; this single resolver is backpressured by the hit-record update
  * path instead of by every Triangle IU.
  */
class RtTriangleResolve extends Module {
  private val rounding = consts.round_near_even
  private val tininess = consts.tininess_beforeRounding

  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new RtTriangleIntersectionOutput))
    val out = Decoupled(new RtTriangleResolveResult)
  })

  val saved = Reg(new RtTriangleIntersectionOutput)
  val busy = RegInit(false.B)
  val outValid = RegInit(false.B)
  val outBits = Reg(new RtTriangleResolveResult)
  val divisionBits = Reg(Vec(3, UInt(32.W)))
  val divisionReturned = RegInit(0.U(3.W))
  val dividers = Seq.fill(3)(Module(new DivSqrtRecFN_small(8, 24, 0)))
  val dividerReady = dividers.map(_.io.inReady).reduce(_ && _)
  val numerators = Seq(io.in.bits.uNum, io.in.bits.vNum, io.in.bits.tNum)
  dividers.zip(numerators).foreach { case (divider, numerator) =>
    divider.io.inValid := io.in.fire
    divider.io.sqrtOp := false.B
    divider.io.a := recFNFromFN(8, 24, numerator)
    divider.io.b := recFNFromFN(8, 24, io.in.bits.tDenom)
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
    divisionReturned := 0.U
  }

  val resultValid = VecInit(dividers.map(_.io.outValid_div)).asUInt
  val result = dividers.map(divider => fNFromRecFN(8, 24, divider.io.out))
  when(busy && resultValid.orR) {
    divisionReturned := divisionReturned | resultValid
  }
  for (index <- result.indices) {
    when(busy && resultValid(index)) {
      divisionBits(index) := result(index)
    }
  }
  val divisionComplete = divisionReturned | resultValid
  val completedDivision = Wire(Vec(3, UInt(32.W)))
  for (index <- completedDivision.indices) {
    completedDivision(index) := Mux(resultValid(index), result(index), divisionBits(index))
  }
  when(busy && divisionComplete.andR) {
    outBits.requestId := saved.requestId
    outBits.u := completedDivision(0)
    outBits.v := completedDivision(1)
    outBits.t := completedDivision(2)
    busy := false.B
    outValid := true.B
  }
}

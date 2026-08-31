package rtcore

import chisel3._
import chisel3.util._
import hardfloat._

/** Final IEEE divisions for a triangle hit returned by RtTrianglePipeline. */
class RtTriangleResolveResult extends Bundle {
  val requestId = UInt(RtCorePortParameters.requestIdWidth.W)
  val hit = Bool()
  val u = UInt(32.W)
  val v = UInt(32.W)
  val t = UInt(32.W)
}

/**
  * Triangle intersection deliberately leaves barycentric/depth division out of
  * the high-throughput RayFlex pipeline.  This single resolver is backpressured
  * by the scheduler's hit-record update path instead of by every Triangle IU.
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
  val dividers = Seq.fill(3)(Module(new DivSqrtRecFN_small(8, 24, 0)))
  val dividerReady = dividers.map(_.io.inReady).reduce(_ && _)
  val numerators = Seq(io.in.bits.uNum, io.in.bits.vNum, io.in.bits.tNum)
  dividers.zip(numerators).foreach { case (divider, numerator) =>
    divider.io.inValid := io.in.fire && io.in.bits.hit
    divider.io.sqrtOp := false.B
    divider.io.a := recFNFromFN(8, 24, numerator)
    divider.io.b := recFNFromFN(8, 24, io.in.bits.tDenom)
    divider.io.roundingMode := rounding
    divider.io.detectTininess := tininess
  }

  io.in.ready := !busy && !outValid && (!io.in.bits.hit || dividerReady)
  io.out.valid := outValid
  io.out.bits := outBits
  when(io.out.fire) { outValid := false.B }

  when(io.in.fire) {
    when(io.in.bits.hit) {
      saved := io.in.bits
      busy := true.B
    }.otherwise {
      outBits.requestId := io.in.bits.requestId
      outBits.hit := false.B
      outBits.u := 0.U
      outBits.v := 0.U
      outBits.t := 0.U
      outValid := true.B
    }
  }

  val resultValid = dividers.map(_.io.outValid_div).reduce(_ && _)
  when(busy && resultValid) {
    outBits.requestId := saved.requestId
    outBits.hit := true.B
    outBits.u := fNFromRecFN(8, 24, dividers(0).io.out)
    outBits.v := fNFromRecFN(8, 24, dividers(1).io.out)
    outBits.t := fNFromRecFN(8, 24, dividers(2).io.out)
    busy := false.B
    outValid := true.B
  }
}

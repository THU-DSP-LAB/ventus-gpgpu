package rtcore

import chisel3._
import chisel3.util._

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
  val dividers = Seq.fill(3)(Module(new RtFpDiv))
  val dividerReady = dividers.map(_.io.in.ready).reduce(_ && _)
  val numerators = Seq(io.in.bits.uNum, io.in.bits.vNum, io.in.bits.tNum)
  dividers.zip(numerators).foreach { case (divider, numerator) =>
    divider.io.in.valid := io.in.fire
    divider.io.in.bits.a := numerator
    divider.io.in.bits.b := io.in.bits.tDenom
    divider.io.out.ready := true.B
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

  val resultValid = VecInit(dividers.map(_.io.out.valid)).asUInt
  val result = dividers.map(_.io.out.bits)
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

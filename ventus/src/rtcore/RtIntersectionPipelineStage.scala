package rtcore

import chisel3._
import chisel3.util._

/** Fixed-latency arithmetic stage. Only use behind result-space reservation:
  * once accepted, a token advances every cycle without internal backpressure.
  */
private[rtcore] trait RtPipelineStageModule[T <: Data, V <: Data] extends Module {
  val intake: DecoupledIO[T]
  val emit: DecoupledIO[V]
}

private[rtcore] object RtPipelineStage {
  def apply[T <: Data](gen: T, logic: T => T = (x: T) => x): RtPipelineStageModule[T, T] =
    RtMappedPipelineStage(gen, gen, logic)
}

private[rtcore] object RtMappedPipelineStage {
  def apply[T <: Data, V <: Data](input: T, output: V, logic: T => V): RtPipelineStageModule[T, V] =
    new RtPipelineStageModule[T, V] {
      val intake = IO(Flipped(Decoupled(input)))
      val emit = IO(Decoupled(output))
      intake.ready := true.B
      emit.valid := RegNext(intake.valid, false.B)
      emit.bits := RegEnable(logic(intake.bits), intake.valid)
      when(emit.valid) {
        assert(emit.ready, "Fixed IU pipeline lost reserved output capacity")
      }
    }
}

/** Reserve one narrow result slot for every accepted request, including all
  * tokens still in the arithmetic stages. Registered occupancy breaks the
  * downstream-ready path; no ready chain crosses the arithmetic pipeline.
  */
private[rtcore] object RtPipelineResultCredits {
  def connect[T <: Data, V <: Data](in: DecoupledIO[T], first: DecoupledIO[T],
      last: DecoupledIO[V], out: DecoupledIO[V], depth: Int): Unit = {
    val results = Module(new Queue(chiselTypeOf(out.bits), depth, pipe = false, flow = false))
    val outstanding = RegInit(0.U(log2Ceil(depth + 1).W))
    in.ready := outstanding < depth.U
    first.valid := in.valid && in.ready
    first.bits := in.bits
    results.io.enq <> last
    out <> results.io.deq
    when(in.fire =/= out.fire) {
      outstanding := Mux(in.fire, outstanding + 1.U, outstanding - 1.U)
    }
    assert(outstanding <= depth.U)
    when(out.fire) { assert(outstanding =/= 0.U) }
  }
}

package cta_scheduler.cta_util

import chisel3._
import chisel3.util._

/** Round-Robin-Priority Binary Encoder
 * @param n: length of the input Bool-Vector
 * @IO out.fire: A valid encoding result is accepted by outer module, and prioriy should be updated in RR method
 */
class RRPriorityEncoder(n: Int) extends Module {
  val io = IO(new Bundle {
    val in = Input(Vec(n, Bool()))
    val out = DecoupledIO(Output(UInt(log2Ceil(n).W)))
  })

  val last = RegInit(0.U(log2Ceil(n).W))
  val in, in_RR = Wire(UInt(n.W))
  in := io.in.asUInt

  val shift = last + 1.U
  in_RR := (in >> shift) | (in << (n.U - shift))   // Circular shift right `shift = last+1`

  io.out.valid := in.orR
  io.out.bits := Mux(in.orR, PriorityEncoder(in_RR) + shift, DontCare)

  when(io.out.fire) {
    last := PriorityEncoder(in_RR) + shift        // update priority
  }
}

/** Round-Robin-Priority Binary Encoder
 */
object RRPriorityEncoder {
  def apply(in: Vec[Bool]): DecoupledIO[UInt]= {
    val inst = Module(new RRPriorityEncoder(in.size))
    inst.io.in := in
    inst.io.out
  }
}

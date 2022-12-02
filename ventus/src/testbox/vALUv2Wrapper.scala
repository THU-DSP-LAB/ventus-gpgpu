package testbox

import chisel3._
import chisel3.util._
import pipeline._

class vALUv2TestInput(softThread: Int) extends Bundle{
  val in1 = Vec(softThread, UInt(32.W))
  val in2 = Vec(softThread, UInt(32.W))
  val in3 = Vec(softThread, UInt(32.W))
  val op = UInt(6.W)
  val count = UInt(5.W)
}
class vALUv2TestWrapper(softThread: Int, hardThread: Int) extends Module {
  val testModule = Module(new vALUv2(softThread, hardThread))

  val io = IO(new Bundle {
    val in = Flipped(DecoupledIO(new vALUv2TestInput(softThread)))
    val out = testModule.io.out.cloneType
    val out2simt_stack = testModule.io.out2simt_stack.cloneType
  })
  //val send = Decoupled(new testModule.vExeData2)
  val fifo = Module(new Queue(new testModule.vExeData2, 1, true))
  fifo.io.enq.bits.in1 := io.in.bits.in1
  fifo.io.enq.bits.in2 := io.in.bits.in2
  fifo.io.enq.bits.in3 := io.in.bits.in3
  fifo.io.enq.bits.mask.foreach( _ := true.B)
  fifo.io.enq.bits.ctrl := 0.U.asTypeOf(fifo.io.enq.bits.ctrl.cloneType)
  fifo.io.enq.bits.ctrl.alu_fn := io.in.bits.op
  fifo.io.enq.bits.ctrl.reg_idxw := io.in.bits.count
  fifo.io.enq.bits.ctrl.wvd := true.B

  fifo.io.enq.valid := io.in.valid
  io.in.ready := fifo.io.enq.ready
  fifo.io.deq <> testModule.io.in

  testModule.io.out <> io.out
  testModule.io.out2simt_stack <> io.out2simt_stack
}

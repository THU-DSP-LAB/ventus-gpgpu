package testbox

import chisel3._
import chisel3.util._
import pipeline._

class vMULv2TestInput(softThread: Int) extends Bundle{
  val in1 = Vec(softThread, UInt(32.W))
  val in2 = Vec(softThread, UInt(32.W))
  val in3 = Vec(softThread, UInt(32.W))
  val wvd = Bool()
  val count = UInt(5.W)
}

class vMULv2TestWrapper(softThread: Int, hardThread: Int) extends Module {
  val testModule = Module(new vMULv2(softThread, hardThread))

  val io = IO(new Bundle{
    val in = Flipped(DecoupledIO(new vMULv2TestInput(softThread)))
    val out_v = testModule.io.out_v.cloneType
    val out_x = testModule.io.out_x.cloneType
  })

  val fifo = Module(new Queue(new testModule.vExeData2(softThread), 1, true))
  fifo.io.enq.bits.in1 := io.in.bits.in1
  fifo.io.enq.bits.in2 := io.in.bits.in2
  fifo.io.enq.bits.in3 := io.in.bits.in3
  fifo.io.enq.bits.mask.foreach( _ := true.B)
  fifo.io.enq.bits.ctrl := 0.U.asTypeOf(fifo.io.enq.bits.ctrl.cloneType)
  fifo.io.enq.bits.ctrl.reg_idxw := io.in.bits.count
  fifo.io.enq.bits.ctrl.wvd := io.in.bits.wvd
  fifo.io.enq.bits.ctrl.wxd := !io.in.bits.wvd

  fifo.io.enq.valid := io.in.valid
  io.in.ready := fifo.io.enq.ready

  fifo.io.deq <> testModule.io.in
  io.out_v <> testModule.io.out_v
  io.out_x <> testModule.io.out_x
}

package TensorCore

import FPUv2.utils._
import chisel3._
import chisel3.util._
import fudian._

class NaiveMultiplier(len: Int, pipeAt: Seq[Int]) extends Module {
  val io = IO(new Bundle() {
    val a, b = Input(UInt(len.W))
    val regEnables = Input(Vec(pipeAt.size, Bool()))
    val result = Output(UInt((2 * len).W))
    val sum = Output(UInt(len.W))
    val carry = Output(UInt(len.W))
  })
  io.result := RegEnable(io.a, io.regEnables(0)) * RegEnable(io.b, io.regEnables(0))
  io.sum := 0.U
  io.carry := 0.U
}

abstract class TCPipelineModule(len: Int, ctrlGen: Data)
  extends FPUPipelineModule(len, ctrlGen)

class TCAddPipe(expWidth: Int, precision: Int, ctrlGen: Data = EmptyFPUCtrl())
  extends TCPipelineModule(expWidth + precision, ctrlGen){

  override def latency = 2
  val len = expWidth + precision
  val s1 = Module(new FCMA_ADD_s1(expWidth, precision, precision))
  val s2 = Module(new FCMA_ADD_s2(expWidth, precision, precision))

  s1.io.a := S1Reg(io.in.bits.a)
  s1.io.b := S1Reg(io.in.bits.b)
  s1.io.rm := S1Reg(io.in.bits.rm)

  s1.io.b_inter_valid := false.B
  s1.io.b_inter_flags := 0.U.asTypeOf(s1.io.b_inter_flags)

  s2.io.in := S2Reg(s1.io.out)
  io.out.bits.result := s2.io.result
  io.out.bits.fflags := s2.io.fflags
  io.out.bits.ctrl.foreach( _ := S2Reg(S1Reg(io.in.bits.ctrl.get)))
}

class TCAddPipe_Int(len: Int, ctrlGen: Data = EmptyFPUCtrl())
  extends TCPipelineModule(len, ctrlGen){
  override def latency = 1

  io.out.bits.result := io.in.bits.a + io.in.bits.b
  io.out.bits.fflags := 0.U
  io.out.bits.ctrl.foreach( _ := io.in.bits.ctrl.get)
}

class TCMulPipe(expWidth: Int, precision: Int, ctrlGen: Data = EmptyFPUCtrl())
  extends TCPipelineModule(expWidth + precision, ctrlGen){

  override def latency = 2
  val len = expWidth + precision

  //val multiplier = Module(new Multiplier(precision + 1, pipeAt = Seq(1)))
  val multiplier = Module(new NaiveMultiplier(precision + 1, pipeAt = Seq(1)))

  val s1 = Module(new FMUL_s1(expWidth, precision))
  val s2 = Module(new FMUL_s2(expWidth, precision))
  val s3 = Module(new FMUL_s3(expWidth, precision))

  s1.io.a := io.in.bits.a
  s1.io.b := io.in.bits.b
  s1.io.rm := io.in.bits.rm

  s2.io.in := S1Reg(s1.io.out)
  s2.io.prod := multiplier.io.result
  s3.io.in := S2Reg(s2.io.out)

  val raw_a = RawFloat.fromUInt(s1.io.a, s1.expWidth, s1.precision)
  val raw_b = RawFloat.fromUInt(s1.io.b, s1.expWidth, s1.precision)

  multiplier.io.a := raw_a.sig
  multiplier.io.b := raw_b.sig
  multiplier.io.regEnables(0) := regEnable(1)

  io.out.bits.result := s3.io.result
  io.out.bits.fflags := s3.io.fflags
  io.out.bits.ctrl.foreach( _ := S2Reg(S1Reg(io.in.bits.ctrl.get)) )
}

class MUL_reuseInput(len: Int, ctrlGen: Data = EmptyFPUCtrl(), topInput: Boolean = false) extends Bundle {
  val op = if(topInput) UInt(6.W) else UInt(3.W)
  val a, b, c = UInt(len.W)
  val rm = UInt(3.W)
  //val ctrl = if (hasCtrl) new FPUCtrl else new FPUCtrl(false)
  val INTmode0 = Flipped(DecoupledIO(Input(Bool())))
  val ctrl = FPUCtrlFac(ctrlGen)
}

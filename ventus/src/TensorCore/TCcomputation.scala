package TensorCore

import FPUv2.utils._
import chisel3._
import chisel3.util._
import fudian._
import pipeline.TCCtrlv2
import FPUv2.TCCtrl

//################################## TCComputation ##################################
class TCComputationInput(DimM:Int,DimN:Int,DimK:Int, len: Int, tcCtrl: TCCtrl) extends Bundle{
  val A = Vec(DimM*DimK, UInt(len.W))
  val B = Vec(DimN*DimK, UInt(len.W))
  val C = Vec(DimM*DimN, UInt(len.W))
  val rm = UInt(3.W)
  val ctrl = tcCtrl.cloneType
}

class TCComputationInput_MixedPrecision(DimM:Int,DimN:Int,DimK:Int, len: Int, tcCtrl: TCCtrl_mix_mul_slot) extends Bundle{
  val A = Vec(DimM*DimK, UInt(len.W))//FP16
  val B = Vec(DimN*DimK, UInt(len.W))//FP16
  val C = Vec(DimM*DimN, UInt((2*len).W))//mixedPrecision FP32
  val rm = UInt(3.W)
  val ctrl = tcCtrl.cloneType
}

class TensorCoreOutput_mix_mult_slot(vl:Int, len: Int,tcCtrl: TCCtrl_mix_mul_slot) extends Bundle{
  val data = Vec(vl, new FPUOutput(len, EmptyFPUCtrl()))
  val ctrl = tcCtrl.cloneType
}

class TC_ComputationArray_MixedPrecision(xDatalen: Int=16, DimM: Int=8, DimN: Int=4, DimK:Int=8, tcCtrl: TCCtrl_mix_mul_slot) extends Module {
  //  mnk defined as cuda.
  //  m8n4k8
  //  xDatalen: data bit len. Here: A\B=FP16; C\D=FP32
  //  TC_ComputationArray_MixedPrecision(16,8,4,8)
  //  Compute: D[m8n4] = A[m8k8] * B[k8n4] + C[m8n4]

  val io = IO(new Bundle{
    val in = Flipped(DecoupledIO(new TCComputationInput_MixedPrecision(DimM, DimN, DimK, xDatalen, tcCtrl)))
    val out = DecoupledIO(new TensorCoreOutput_mix_mult_slot(DimM * DimN, 2*xDatalen, tcCtrl))// out matrix dim=[8,4]
  })
  dontTouch(io.in.bits)

  val TCArray = Seq(Module(new TCDotProduct_MixedPrecision(DimK, 5, 11, tcCtrl))) ++
    Seq.fill(DimM*DimN-1)(Module(new TCDotProduct_MixedPrecision(DimK, 5, 11,tcCtrl)))
  // control sig only claim 1 times
  for (i <- 0 until  DimM * DimN) {
    io.out.bits.data(i).result := 0.U
    io.out.bits.data(i).fflags := 0.U
  }
  io.in.ready := TCArray.head.io.in.ready
  io.out.valid := TCArray.head.io.out.valid

  for(m <- 0 until DimM){
    for(n <- 0 until DimN){
      for(k <- 0 until DimK){
        TCArray(m * DimN + n).io.in.bits.a(k) := io.in.bits.A(m*DimK+k)
        TCArray(m * DimN + n).io.in.bits.b(k) := io.in.bits.B(n*DimK+k)//col first
      }
      TCArray(m * DimN + n).io.in.bits.c := io.in.bits.C(m * DimN + n)

      TCArray(m * DimN + n).io.in.bits.rm := io.in.bits.rm
      TCArray(m * DimN + n).io.in.bits.ctrl := io.in.bits.ctrl
      TCArray(m * DimN + n).io.in.valid := io.in.valid
      TCArray(m * DimN + n).io.out.ready := io.out.ready

      io.out.bits.data(m * DimN + n).result := TCArray(m * DimN + n).io.out.bits.result
      io.out.bits.data(m * DimN + n).fflags := TCArray(m * DimN + n).io.out.bits.fflags
    }
  }
  io.out.bits.ctrl := TCArray.head.io.out.bits.ctrl.get
}

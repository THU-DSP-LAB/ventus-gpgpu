package TensorCore

import FPUv2.utils.{EmptyFPUCtrl, TestFPUCtrl}
import chisel3._
import chisel3.util._
import top.parameters._
import pipeline.vExeData
import FPUv2.{TC_ComputationArray_848_Binary, TC_ComputationArray_848_FP16, TC_ComputationArray_848_INT8FP16_Reuse}


class TC_MMA1688Input(tcCtrl: TCCtrl) extends Bundle{
  val data_in = new vExeData
  val dtype = UInt(3.W)
  val dshpe = UInt(2.W)
  val rm = UInt(3.W)
  val ctrl = tcCtrl.cloneType
}

class TC_MMA1688Output(DimM:Int, DimN:Int, dataLen:Int, tcCtrl:TCCtrl) extends Bundle{
//  val data_out = Vec(DimM*DimN,UInt(dataLen.W))
  // ??? d=a*b+d
  // write back to D
  val data_out = Vec(num_thread,new vExeData)
  val ctrl = tcCtrl.cloneType
}

class TC_MMA1688(DimM: Int, DimN: Int, DimK: Int,xDatalen:Int, tcCtrl: TCCtrl) extends Module {
  //  mnk defined as cuda.
  //  m16n8k8
  //  xDatalen: data bit len === xLen. FP16: xDatalen=16
  //  Compute: D[m16n8] = A[m16k8] * B[k8n8] + C[m16n8]

  val io = IO(new Bundle {
    val in = Flipped(DecoupledIO(new TC_MMA1688Input(tcCtrl)))
    val out = DecoupledIO(new TC_MMA1688Output(DimM, DimN, xDatalen, tcCtrl))
  })

  val TCComputation = new TC_ComputationArray_848_FP16(tcCtrl = tcCtrl)
  TCComputation.io.in.bits.rm := io.in.bits.rm
  TCComputation.io.in.bits.ctrl := io.in.bits.ctrl

  val sIdle :: sSet1 :: sSet2 :: sSet3 :: sSet4 :: Nil = Enum(5)

  // 定义状态寄存器
  val stateReg = RegInit(sIdle)

  // 根据当前状态，设置下一个状态和输出
  switch(stateReg) {
    is(sIdle) {
      TCComputation.io.out.ready := true.B
      for (m <- 0 until 8) {
        for (n <- 0 until 4) {
          if ((m * 8 + n) % 2 == 0) {
            TCComputation.io.in.bits.A(m * 8 + n) := io.in.bits.data_in.in1((m * 8 + n) / 2)(15, 0)
          } else {
            TCComputation.io.in.bits.A(m * 8 + n) := io.in.bits.data_in.in1((m * 8 + n) / 2)(31, 16)
          }
        }
      }

      for (m <- 0 until 8) {
        for (n <- 0 until 4) {
          if ((m * 8 + n) % 2 == 0) {
            TCComputation.io.in.bits.A(m * 8 + n) := io.in.bits.data_in.in1((m * 8 + n) / 2)(15, 0)
          } else {
            TCComputation.io.in.bits.A(m * 8 + n) := io.in.bits.data_in.in1((m * 8 + n) / 2)(31, 16)
          }
        }
      }


      TCComputation.io.in.valid := true.B
      stateReg := sSet1
      }
      is(sSet1) {
        when(TCComputation.io.out.valid) {
          stateReg := sSet2
        }
      }
      is(sSet2) {
        when(TCComputation.io.out.valid) {
          stateReg := sSet3
        }
      }
      is(sSet3) {
        when(TCComputation.io.out.valid) {
          stateReg := sSet4
        }
      }
      is(sSet4) {
        when(TCComputation.io.out.valid) {
          stateReg := sIdle
        }
      }
    }

}


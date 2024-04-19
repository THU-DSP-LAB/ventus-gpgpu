package TensorCore

import FPUv2.utils.{EmptyFPUCtrl, TestFPUCtrl}
import chisel3._
import chisel3.util._
import top.parameters._
import pipeline.vExeData

class vExeData_t extends Bundle{
  val in1=Vec(num_thread,UInt(xLen.W))
  val in2=Vec(num_thread,UInt(xLen.W))
  val in3=Vec(num_thread,UInt(xLen.W))
  val mask=Vec(num_thread,Bool())
}
class TC_MMA1688Input(tcCtrl: TCCtrl) extends Bundle{
  val data_in = new vExeData
//  val dtype = UInt(3.W)
//  val dshpe = UInt(2.W)
  val rm = UInt(3.W)
  val ctrl = tcCtrl.cloneType
}

class TC_MMA1688Output(DimM:Int, DimN:Int, dataLen:Int, tcCtrl:TCCtrl) extends Bundle{
  //  val data_out = Vec(DimM*DimN,UInt(dataLen.W))
  // ??? d=a*b+d
  // write back to D

    val data_out = new vExeData_t
    val ctrl = tcCtrl.cloneType
}

class TC_MMA1688(DimM: Int, DimN: Int, DimK: Int, xDatalen:Int, tcCtrl: TCCtrl) extends Module {
  //  mnk defined as cuda.
  //  m16n8k8
  //  xDatalen: data bit len === xLen. FP16: xDatalen=16
  //  Compute: D[m16n8] = A[m16k8] * B[k8n8] + C[m16n8]

  val io = IO(new Bundle {
    val in = Flipped(DecoupledIO(new TC_MMA1688Input(tcCtrl)))
    val out = DecoupledIO(new TC_MMA1688Output(DimM, DimN, xDatalen, tcCtrl))
  })

  // Init data_out
  for (i <- 0 until num_thread) {
    io.out.bits.data_out.in1(i) := 0.U
    io.out.bits.data_out.in2(i) := 0.U
    io.out.bits.data_out.in3(i) := 0.U
    io.out.bits.data_out.mask(i) := io.in.bits.data_in.mask(i)
  }

  // Init TC Computation Array
  val TCComputation = Module(new TC_ComputationArray_848_FP16(16,8,4,8,tcCtrl = tcCtrl))
  dontTouch(TCComputation.io.out)
  //A 8*8 row
  for (m <- 0 until 8) {
    for (n <- 0 until 4) {
      TCComputation.io.in.bits.A(m * 8 + n * 2) := io.in.bits.data_in.in1(m * 4 + n)(15, 0)
      TCComputation.io.in.bits.A(m * 8 + n * 2 + 1) := io.in.bits.data_in.in1(m * 4 + n)(31, 16)
    }
  }
  //B 8*4 col
  for (m <- 0 until 4) {
    for (n <- 0 until 4) {
      TCComputation.io.in.bits.B(m * 8 + n * 2) := io.in.bits.data_in.in2(m * 4 + n)(15, 0)
      TCComputation.io.in.bits.B(m * 8 + n * 2 + 1) := io.in.bits.data_in.in2(m * 4 + n)(31, 16)
    }
  }
  //C 8*4 row First
  for (m <- 0 until 8) {
    TCComputation.io.in.bits.C(m * 4) := io.in.bits.data_in.in3(m * 4)(15, 0)
    TCComputation.io.in.bits.C(m * 4 + 1) := io.in.bits.data_in.in3(m * 4)(31, 16)
    TCComputation.io.in.bits.C(m * 4 + 2) := io.in.bits.data_in.in3(m * 4 + 1)(15, 0)
    TCComputation.io.in.bits.C(m * 4 + 3) := io.in.bits.data_in.in3(m * 4 + 1)(31, 16)
  }

  TCComputation.io.in.bits.rm := io.in.bits.rm
  TCComputation.io.in.bits.ctrl := io.in.bits.ctrl
  TCComputation.io.out.ready := io.out.ready//io.in.ready
  TCComputation.io.in.valid := false.B//io.in.valid

  io.in.ready := TCComputation.io.in.ready
  io.out.valid := TCComputation.io.out.valid

  val sIdle :: sSet1 :: sSet2 :: sSet3 :: sSet4 :: Nil = Enum(5)

  // 定义状态寄存器
  val stateReg = RegInit(sIdle)

  // 根据当前状态，设置下一个状态和输出
  switch(stateReg) {
    is(sIdle) {
      when(io.in.fire) {
        printf("Set1 Data Done. To Set1\n")
        //        TCComputation.io.out.ready := io.in.ready
        TCComputation.io.in.valid := io.in.valid
        //A 8*8 row
        for (m <- 0 until 8) {
          for (n <- 0 until 4) {
            TCComputation.io.in.bits.A(m * 8 + n * 2) := io.in.bits.data_in.in1(m * 4 + n)(15, 0)
            TCComputation.io.in.bits.A(m * 8 + n * 2 + 1) := io.in.bits.data_in.in1(m * 4 + n)(31, 16)
          }
        }
        //B 8*4 col
        for (m <- 0 until 4) {
          for (n <- 0 until 4) {
            TCComputation.io.in.bits.B(m * 8 + n * 2) := io.in.bits.data_in.in2(m * 4 + n)(15, 0)
            TCComputation.io.in.bits.B(m * 8 + n * 2 + 1) := io.in.bits.data_in.in2(m * 4 + n)(31, 16)
          }
        }
        //C 8*4 row First
        for (m <- 0 until 8) {
          TCComputation.io.in.bits.C(m * 4) := io.in.bits.data_in.in3(m * 4)(15, 0)
          TCComputation.io.in.bits.C(m * 4 + 1) := io.in.bits.data_in.in3(m * 4)(31, 16)
          TCComputation.io.in.bits.C(m * 4 + 2) := io.in.bits.data_in.in3(m * 4 + 1)(15, 0)
          TCComputation.io.in.bits.C(m * 4 + 3) := io.in.bits.data_in.in3(m * 4 + 1)(31, 16)
        }
        stateReg := sSet1
      }
    }
    is(sSet1) {
      when(TCComputation.io.out.valid) {
//        //        get set1 Result
//        for (m <- 0 until 8) {
//          io.out.bits.data_out.in3(m * 4)(15, 0) := TCComputation.io.out.bits.data(m * 4)
//          io.out.bits.data_out.in3(m * 4)(31, 16) := TCComputation.io.out.bits.data(m * 4+1)
//          io.out.bits.data_out.in3(m * 4 + 1)(15, 0) := TCComputation.io.out.bits.data(m * 4+2)
//          io.out.bits.data_out.in3(m * 4 + 1)(31, 16) := TCComputation.io.out.bits.data(m * 4+3)
//        }

        //        TCComputation.io.out.ready := io.out.ready
        TCComputation.io.in.valid := true.B//io.in.valid
        //A 8*8 row
        for (m <- 0 until 8) {
          for (n <- 0 until 4) {
            TCComputation.io.in.bits.A(m * 8 + n*2) := io.in.bits.data_in.in1(m * 4 + n)(15, 0)
            TCComputation.io.in.bits.A(m * 8 + n*2 + 1) := io.in.bits.data_in.in1(m * 4 + n)(31, 16)
          }
        }
        //B 8*4 col
        for (m <- 0 until 4) {
          for (n <- 0 until 4) {
            TCComputation.io.in.bits.B(m * 8 + n*2) := io.in.bits.data_in.in2(16+m * 4 + n)(15, 0)
            TCComputation.io.in.bits.B(m * 8 + n*2 + 1) := io.in.bits.data_in.in2(16+m * 4 + n)(31, 16)
          }
        }
        //C 8*4 row First
        for (m <- 0 until 8) {
          TCComputation.io.in.bits.C(m * 4 ) := io.in.bits.data_in.in3(2+m * 4 )(15, 0)
          TCComputation.io.in.bits.C(m * 4 +1) := io.in.bits.data_in.in3(2+m * 4)(31, 16)
          TCComputation.io.in.bits.C(m * 4 +2) := io.in.bits.data_in.in3(2+m * 4 + 1)(15, 0)
          TCComputation.io.in.bits.C(m * 4 +3) := io.in.bits.data_in.in3(2+m * 4 + 1)(31, 16)
        }
        printf("Set2 Data Done. To Set2\n")
        stateReg := sSet2
      }
    }
    is(sSet2) {
      when(TCComputation.io.out.valid) {
//        //        get set2 Result
//        for (m <- 0 until 8) {
//          io.out.bits.data_out.in3(2+m * 4)(15, 0):= TCComputation.io.out.bits.data(m * 4)
//          io.out.bits.data_out.in3(2+m * 4)(31, 16) := TCComputation.io.out.bits.data(m * 4+1)
//          io.out.bits.data_out.in3(2+m * 4 + 1)(15, 0) := TCComputation.io.out.bits.data(m * 4+2)
//          io.out.bits.data_out.in3(2+m * 4 + 1)(31, 16) := TCComputation.io.out.bits.data(m * 4+3)
//        }

        //        TCComputation.io.out.ready := io.out.ready
        TCComputation.io.in.valid := true.B//io.in.valid
        //A 8*8 row
        for (m <- 0 until 8) {
          for (n <- 0 until 4) {
            TCComputation.io.in.bits.A(m * 8 + n*2) := io.in.bits.data_in.in1(m * 4 + n)(47, 32)
            TCComputation.io.in.bits.A(m * 8 + n*2 + 1) := io.in.bits.data_in.in1(m * 4 + n)(63, 48)
          }
        }
        //B 8*4 col
        for (m <- 0 until 4) {
          for (n <- 0 until 4) {
            TCComputation.io.in.bits.B(m * 8 + n*2) := io.in.bits.data_in.in2(m * 4 + n)(15, 0)
            TCComputation.io.in.bits.B(m * 8 + n*2 + 1) := io.in.bits.data_in.in2(m * 4 + n)(31, 16)
          }
        }
        //C 8*4 row First
        for (m <- 0 until 8) {
          TCComputation.io.in.bits.C(m * 4 ) := io.in.bits.data_in.in3(m * 4 )(47, 32)
          TCComputation.io.in.bits.C(m * 4 +1) := io.in.bits.data_in.in3(m * 4)(63, 48)
          TCComputation.io.in.bits.C(m * 4 +2) := io.in.bits.data_in.in3(m * 4 + 1)(47, 32)
          TCComputation.io.in.bits.C(m * 4 +3) := io.in.bits.data_in.in3(m * 4 + 1)(63, 48)
        }
        printf("Set3 Data Done. To Set3\n")
        stateReg := sSet3
      }
    }
    is(sSet3) {
      when(TCComputation.io.out.valid) {
//        //        get set3 Result
//        for (m <- 0 until 8) {
//          io.out.bits.data_out.in3(m * 4)(47, 32):= TCComputation.io.out.bits.data(m * 4)
//          io.out.bits.data_out.in3(m * 4)(63, 48) := TCComputation.io.out.bits.data(m * 4+1)
//          io.out.bits.data_out.in3(m * 4 + 1)(47, 32) := TCComputation.io.out.bits.data(m * 4+2)
//          io.out.bits.data_out.in3(m * 4 + 1)(63, 48) := TCComputation.io.out.bits.data(m * 4+3)
//        }

        //        TCComputation.io.out.ready := io.out.ready
        TCComputation.io.in.valid := true.B//io.in.valid
        //A 8*8 row
        for (m <- 0 until 8) {
          for (n <- 0 until 4) {
            TCComputation.io.in.bits.A(m * 8 + n*2) := io.in.bits.data_in.in1(m * 4 + n)(47, 32)
            TCComputation.io.in.bits.A(m * 8 + n*2 + 1) := io.in.bits.data_in.in1(m * 4 + n)(63, 48)
          }
        }
        //B 8*4 col
        for (m <- 0 until 4) {
          for (n <- 0 until 4) {
            TCComputation.io.in.bits.B(m * 8 + n*2) := io.in.bits.data_in.in2(16+m * 4 + n)(15, 0)
            TCComputation.io.in.bits.B(m * 8 + n*2 + 1) := io.in.bits.data_in.in2(16+m * 4 + n)(31, 16)
          }
        }
        //C 8*4 row First
        for (m <- 0 until 8) {
          TCComputation.io.in.bits.C(m * 4 ) := io.in.bits.data_in.in3(2+m * 4 )(47, 32)
          TCComputation.io.in.bits.C(m * 4 +1) := io.in.bits.data_in.in3(2+m * 4)(63, 48)
          TCComputation.io.in.bits.C(m * 4 +2) := io.in.bits.data_in.in3(2+m * 4 + 1)(47, 32)
          TCComputation.io.in.bits.C(m * 4 +3) := io.in.bits.data_in.in3(2+m * 4 + 1)(63, 48)
        }
        printf("Set4 Data Done. To Set4")
        stateReg := sSet4
      }
    }
    is(sSet4) {
      when(TCComputation.io.out.valid) {
//        //        get set4 Result
//        for (m <- 0 until 8) {
//          io.out.bits.data_out.in3(2+m * 4)(47, 32):= TCComputation.io.out.bits.data(m * 4)
//          io.out.bits.data_out.in3(2+m * 4)(63, 48) := TCComputation.io.out.bits.data(m * 4+1)
//          io.out.bits.data_out.in3(2+m * 4 + 1)(47, 32) := TCComputation.io.out.bits.data(m * 4+2)
//          io.out.bits.data_out.in3(2+m * 4 + 1)(63, 48) := TCComputation.io.out.bits.data(m * 4+3)
//        }

        //        io.out.ready := TCComputation.io.out.ready
        io.out.valid := TCComputation.io.out.valid

        stateReg := sIdle
      }
    }
  }
  io.out.bits.ctrl := TCComputation.io.out.bits.ctrl

}
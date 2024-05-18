package TensorCore

import FPUv2.utils.{EmptyFPUCtrl, TestFPUCtrl}
import chisel3._
import chisel3.util._
import top.parameters._
import pipeline.{vExeData,WriteVecCtrl,LSUexe}

// D=A*B+D
// Matrix D comes from Register.
// Matrix A\B comes from SMEM.

class TC_MMAInput(tcCtrl: TCCtrl) extends Bundle{
  val data_in = new vExeData
  val rm = UInt(3.W)
  val ctrl = tcCtrl.cloneType
}

class TC_MMAOutput(tcCtrl:TCCtrl) extends Bundle{
  // d=a*b+d
  // write back to D
  //    val data_out = Vec(num_thread,UInt(xLen.W))//pipeline.WriteVecCtrl.wb_wvd_rd
  val data_out = Vec(num_thread,UInt(xLen.W)) //new WriteVecCtrl
  val ctrl = tcCtrl.cloneType
}


class TC_MMA888(DimM: Int, DimN: Int, DimK: Int, xDatalen:Int=16, tcCtrl: TCCtrl) extends Module {
  //  mnk defined as cuda.
  //  m8n8k8
  //  xDatalen: data bit len === xLen. FP16: xDatalen=16
  //  Compute: C[m8n8] = A[m8k8] * B[k8n8] + C[m8n8]
  //  AB from SMEM, C from Register.

  val io = IO(new Bundle {
    val in = Flipped(DecoupledIO(new TC_MMAInput(tcCtrl)))
    val out = DecoupledIO(new TC_MMAOutput(tcCtrl))
  })
  val set_num = 2 //2 := (888/848)

  // Init Register/FIFO(depth=1), Data output cache.
  val regArray1 = Reg(Vec(DimN*DimM/set_num, UInt(xDatalen.W)))
  val regArray2 = Reg(Vec(DimN*DimM/set_num, UInt(xDatalen.W)))

  // Init Register/FIFO(depth=1), Data Input cache(AB).
  val regArray_A = Reg(Vec(num_thread, UInt(xLen.W)))
  val regArray_B = Reg(Vec(num_thread, UInt(xLen.W)))

  // Init data_out
  for (i <- 0 until num_thread) {
    io.out.bits.data_out(i) := 0.U
    //    io.out.bits.data_out.wvd_mask(i) := io.in.bits.data_in.mask(i)
  }
  //  for(m<-0 until 8){
  //    io.out.bits.data_out(m*4) := 0.U//Cat(regArray3(m*4+1),regArray3(m*4),regArray1(m*4+1),regArray1(m*4))
  //    io.out.bits.data_out(m*4+1) := 0.U//Cat(regArray3(m*4+3),regArray3(m*4+2),regArray1(m*4+3),regArray1(m*4+2))
  //    io.out.bits.data_out(m*4+2) := 0.U//Cat(regArray4(m*4+1),regArray4(m*4),regArray2(m*4+1),regArray2(m*4))
  //    io.out.bits.data_out(m*4+3) := 0.U//Cat(regArray4(m*4+3),regArray4(m*4+2),regArray2(m*4+3),regArray2(m*4+2))
  //  }
  for(m<-0 until 8){
    io.out.bits.data_out(m*4) := Cat(regArray1(m*4+1),regArray1(m*4))
    io.out.bits.data_out(m*4+1) := Cat(regArray1(m*4+3),regArray1(m*4+2))
    io.out.bits.data_out(m*4+2) := Cat(regArray2(m*4+1),regArray2(m*4))
    io.out.bits.data_out(m*4+3) := Cat(regArray2(m*4+3),regArray2(m*4+2))
  }

  // Init TC Computation Array
  val TCComputation = Module(new TC_ComputationArray_848_FP16(16,8,4,8,tcCtrl = tcCtrl))
  dontTouch(TCComputation.io.out)

  // Init TC Computation Array Input IO
  //A 8*8 row
  for (i <- 0 until 64) TCComputation.io.in.bits.A(i) := 0.U
  //B 8*4 col
  for (i <- 0 until 32) TCComputation.io.in.bits.A(i) := 0.U
  //C 8*4 row First
  for (i <- 0 until 32) TCComputation.io.in.bits.A(i) := 0.U

  TCComputation.io.in.bits.rm := io.in.bits.rm
  TCComputation.io.in.bits.ctrl := io.in.bits.ctrl
  TCComputation.io.out.ready := io.out.ready//io.in.ready
  TCComputation.io.in.valid := false.B//io.in.valid

  io.in.ready := TCComputation.io.in.ready
  io.out.valid := false.B//TCComputation.io.out.valid

  // Init LSU.
  val LSU = Module(new getData4SMEM)

  val sIdle :: sSMEMseqA :: sSMEMseqB :: sSet1 :: sSet2 :: sDataOut:: Nil = Enum(6)

  // 定义状态寄存器
  val stateReg = RegInit(sIdle)

  // 根据当前状态，设置下一个状态和输出
  switch(stateReg) {
    is(sIdle) {
      when(io.in.fire) {
        LSU.io.in.bits := 0.U//WIP
        regArray_A := LSU.io.out //WIP
        stateReg := sSMEMseqA
      }
    }
    is(sSMEMseqA){
      when(LSU.io.out.valid){
        LSU.io.in.bits := 0.U//WIP
        regArray_B := LSU.io.out //WIP
        stateReg := sSMEMseqB
      }
    }
    is(sSMEMseqB) {
      when(LSU.io.out.valid) {
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
        //        get set1 Result
        for (m<- 0 until DimN*DimM/set_num){
          regArray1(m) := TCComputation.io.out.bits.data(m).result
        }

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
        //        get set2 Result
        for (m<- 0 until DimN*DimM/set_num){
          regArray2(m) := TCComputation.io.out.bits.data(m).result
        }

        stateReg := sDataOut
      }
    }
    is(sDataOut) {
      // Transfer Data from Register to Output.
      for(m<-0 until 8){
        io.out.bits.data_out(m*4) := Cat(regArray1(m*4+1),regArray1(m*4))
        io.out.bits.data_out(m*4+1) := Cat(regArray1(m*4+3),regArray1(m*4+2))
        io.out.bits.data_out(m*4+2) := Cat(regArray2(m*4+1),regArray2(m*4))
        io.out.bits.data_out(m*4+3) := Cat(regArray2(m*4+3),regArray2(m*4+2))
      }
      //        val validSignal = RegInit(true.B)
      io.out.valid := true.B//RegNext(validSignal)
      //        io.out.valid := RegNext(true.B)//TCComputation.io.out.valid
      stateReg := sIdle
    }
  }
  io.out.bits.ctrl := TCComputation.io.out.bits.ctrl

}
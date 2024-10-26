package TensorCore

import FPUv2.utils.{EmptyFPUCtrl, TestFPUCtrl}
import chisel3._
import chisel3.util._
import top.parameters._
import pipeline.{CtrlSigs, DCacheCoreRsp_np, LSUexe, ShareMemCoreReq_np, TCCtrlv2, WriteVecCtrl, vExeData}
import FPUv2.TCCtrl
// D=A*B+D
// Matrix D comes from Register.
// Matrix A\B comes from SMEM.
class vTCData extends Bundle{
  val in1=Vec(num_thread,UInt(xLen.W))
  val in2=Vec(num_thread,UInt(xLen.W))
  val in3=Vec(num_thread,UInt(xLen.W))
  val mask=Vec(num_thread,Bool())
}

class TC_MMAInput(tcCtrl:TCCtrl) extends Bundle{
  val data_in = new vTCData
  val rm = UInt(3.W)
  val ctrl = tcCtrl.cloneType
}

class TC_MMAInput_MixedPrecision(tcCtrl:TCCtrl) extends Bundle{
  val data_in = new vTCData
  val rm = UInt(3.W)
  val isMixedPrecisionMode = Bool()
  val ctrl = tcCtrl.cloneType
}

class TC_MMAOutput(tcCtrl:TCCtrl) extends Bundle{
  // d=a*b+d
  // write back to D
  //    val data_out = Vec(num_thread,UInt(xLen.W))//pipeline.WriteVecCtrl.wb_wvd_rd
  val data_out = Vec(num_thread,UInt(xLen.W)) //new WriteVecCtrl
  val ctrl = tcCtrl.cloneType
}
class TC_MMAInput_MixedPrecision2(tcCtrl:pipeline.TCCtrl_mulslot_v2) extends Bundle{
  val data_in = new vTCData
  val rm = UInt(3.W)
  val isMixedPrecisionMode = Bool()
  val ctrl = tcCtrl.cloneType
}

class TC_MMAOutput2(tcCtrl:TCCtrl_mix_mul_slot) extends Bundle{
  // d=a*b+d
  // write back to D
  //    val data_out = Vec(num_thread,UInt(xLen.W))//pipeline.WriteVecCtrl.wb_wvd_rd
  val data_out = Vec(num_thread,UInt(xLen.W)) //new WriteVecCtrl
  val ctrl = tcCtrl.cloneType
}

class TC_MMA888(DimM: Int, DimN: Int, DimK: Int, xDatalen:Int = 16, tcctrl:TCCtrl) extends Module {
  //  mnk defined as cuda.
  //  m8n8k8
  //  xDatalen: data bit len === xLen. FP16: xDatalen=16
  //  Compute: C[m8n8] = A[m8k8] * B[k8n8] + C[m8n8]
  //  A\B\C from Register.

  val io = IO(new Bundle {
    val in = Flipped(DecoupledIO(new TC_MMAInput(tcctrl)))
    val out = DecoupledIO(new TC_MMAOutput(tcctrl))
//    val shared_req = DecoupledIO(new ShareMemCoreReq_np())
//    val shared_rsp = Flipped(DecoupledIO(new DCacheCoreRsp_np))
  })
  val set_num = 2 //2 := (888/848)

  // Init Register/FIFO(depth=1), Data output cache.
  val regArray1 = Reg(Vec(DimN*DimM/set_num, UInt(xDatalen.W)))
  val regArray2 = Reg(Vec(DimN*DimM/set_num, UInt(xDatalen.W)))

  // Init Register/FIFO(depth=1) for data set2.
  val regSet2_A = Reg(Vec(DimM*DimK, UInt(xDatalen.W)))
  val regSet2_B = Reg(Vec(DimN/set_num*DimK, UInt(xDatalen.W)))
  val regSet2_C = Reg(Vec(DimM*DimN/set_num, UInt(xDatalen.W)))

  val reg_rm = Reg(UInt(3.W))
  val reg_ctrl = Reg(tcctrl.cloneType)

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
  val TCComputation = Module(new TC_ComputationArray_848_FP16(16,8,4,8,tcCtrl = tcctrl))
  dontTouch(TCComputation.io.out)

  // Init TC Computation Array Input IO
  //A 8*8 row
  for (i <- 0 until 64) TCComputation.io.in.bits.A(i) := 0.U
  //B 8*4 col
  for (i <- 0 until 32) TCComputation.io.in.bits.B(i) := 0.U
  //C 8*4 row First
  for (i <- 0 until 32) TCComputation.io.in.bits.C(i) := 0.U

//  val Ready_tc = Wire(Bool())
//  Ready_tc := TCComputation.io.in.ready
  TCComputation.io.out.ready :=  io.out.ready//Ready_tc//

  TCComputation.io.in.valid := false.B// io.in.valid
  TCComputation.io.in.bits.rm := io.in.bits.rm
  TCComputation.io.in.bits.ctrl := io.in.bits.ctrl

  io.in.ready := TCComputation.io.in.ready
  io.out.valid := false.B// TCComputation.io.out.valid

  val sIdle :: sSet1 :: sSet2 :: sDataOut:: Nil = Enum(4)

  // 定义状态寄存器
  val stateReg = RegInit(sIdle)

  // 根据当前状态，设置下一个状态和输出
  switch(stateReg) {
    is(sIdle) {
      when(io.in.fire) {
        // store set2 data
        //A 8*8 row
        for (m <- 0 until 8) {
          for (n <- 0 until 4) {
            regSet2_A(m * 8 + n*2) := io.in.bits.data_in.in1(m * 4 + n)(15, 0)
            regSet2_A(m * 8 + n*2 + 1) := io.in.bits.data_in.in1(m * 4 + n)(31, 16)
          }
        }
        //B 8*4 col
        for (m <- 0 until 4) {
          for (n <- 0 until 4) {
            regSet2_B(m * 8 + n*2) := io.in.bits.data_in.in2(16+m * 4 + n)(15, 0)
            regSet2_B(m * 8 + n*2 + 1) := io.in.bits.data_in.in2(16+m * 4 + n)(31, 16)
          }
        }
        //C 8*4 row First
        for (m <- 0 until 8) {
          regSet2_C(m * 4 ) := io.in.bits.data_in.in3(2+m * 4 )(15, 0)
          regSet2_C(m * 4 +1) := io.in.bits.data_in.in3(2+m * 4)(31, 16)
          regSet2_C(m * 4 +2) := io.in.bits.data_in.in3(2+m * 4 + 1)(15, 0)
          regSet2_C(m * 4 +3) := io.in.bits.data_in.in3(2+m * 4 + 1)(31, 16)
        }
        printf("Set2 Data Store Done\n")

        printf("Set1 Data Done. To Set1\n")
        //        TCComputation.io.out.ready := io.in.ready
        TCComputation.io.in.bits.rm := io.in.bits.rm
        TCComputation.io.in.bits.ctrl := io.in.bits.ctrl

        // save  ctrl and rm
        reg_rm := io.in.bits.rm
        reg_ctrl := io.in.bits.ctrl

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
      when(TCComputation.io.out.fire) {
        //        get set1 Result
        for (m<- 0 until DimN*DimM/set_num){
          regArray1(m) := TCComputation.io.out.bits.data(m).result
        }

        TCComputation.io.in.bits.rm := reg_rm
        TCComputation.io.in.bits.ctrl := reg_ctrl

        TCComputation.io.in.valid := true.B//io.in.valid
        //A 8*8 row
        for (i <- 0 until 64){
          TCComputation.io.in.bits.A(i) := regSet2_A(i)
        }
        //B 8*4 col
        for (i <- 0 until 32){
          TCComputation.io.in.bits.B(i) := regSet2_B(i)
        }
        //C 8*4 row First
        for (i <- 0 until 32){
          TCComputation.io.in.bits.C(i) := regSet2_C(i)
        }

        printf("Set2 Data Done. To Set2\n")
        stateReg := sSet2
      }
    }
    is(sSet2) {
      when(TCComputation.io.out.fire) {
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
      // val validSignal = RegInit(true.B)
      io.out.valid := true.B//RegNext(validSignal)
      // io.out.valid := RegNext(true.B)//TCComputation.io.out.valid
      when(io.out.fire) {
        stateReg := sIdle
      }
    }
  }
  io.out.bits.ctrl := TCComputation.io.out.bits.ctrl
}

//from TC_MMA888, change original one state machine to 2. And pipeline the computation array.
class TC_MMA888_V2(DimM: Int, DimN: Int, DimK: Int, xDatalen:Int = 16, tcctrl:TCCtrl) extends Module {
  //  mnk defined as cuda.
  //  m8n8k8
  //  xDatalen: data bit len === xLen. FP16: xDatalen=16
  //  Compute: C[m8n8] = A[m8k8] * B[k8n8] + C[m8n8]
  //  A\B\C from Register.

  val io = IO(new Bundle {
    val in = Flipped(DecoupledIO(new TC_MMAInput(tcctrl)))
    val out = DecoupledIO(new TC_MMAOutput(tcctrl))
    //    val shared_req = DecoupledIO(new ShareMemCoreReq_np())
    //    val shared_rsp = Flipped(DecoupledIO(new DCacheCoreRsp_np))
  })
  val set_num = 2 //2 := (888/848)
  // Init Register/FIFO(depth=1), Data output cache.
  val regArray1 = Reg(Vec(DimN*DimM/set_num, UInt(xDatalen.W)))
  val regArray2 = Reg(Vec(DimN*DimM/set_num, UInt(xDatalen.W)))

  // Init Register/FIFO(depth=1) for data set2.
  val regSet2_A = Reg(Vec(DimM*DimK, UInt(xDatalen.W)))
  val regSet2_B = Reg(Vec(DimN/set_num*DimK, UInt(xDatalen.W)))
  val regSet2_C = Reg(Vec(DimM*DimN/set_num, UInt(xDatalen.W)))

  val reg_rm = Reg(UInt(3.W))
  val reg_ctrl = Reg(tcctrl.cloneType)

  // Init data_out
//  for (i <- 0 until num_thread) {
//    io.out.bits.data_out(i) := 0.U
//    //    io.out.bits.data_out.wvd_mask(i) := io.in.bits.data_in.mask(i)
//  }
  for(m<-0 until 8){
    io.out.bits.data_out(m*4) := Cat(regArray1(m*4+1),regArray1(m*4))
    io.out.bits.data_out(m*4+1) := Cat(regArray1(m*4+3),regArray1(m*4+2))
    io.out.bits.data_out(m*4+2) := Cat(regArray2(m*4+1),regArray2(m*4))
    io.out.bits.data_out(m*4+3) := Cat(regArray2(m*4+3),regArray2(m*4+2))
  }

  // Init TC Computation Array
  val TCComputation = Module(new TC_ComputationArray_848_FP16(16,8,4,8,tcCtrl = tcctrl))
  dontTouch(TCComputation.io.out)

  // Init TC Computation Array Input IO
  //A 8*8 row
  for (i <- 0 until 64) TCComputation.io.in.bits.A(i) := 0.U
  //B 8*4 col
  for (i <- 0 until 32) TCComputation.io.in.bits.B(i) := 0.U
  //C 8*4 row First
  for (i <- 0 until 32) TCComputation.io.in.bits.C(i) := 0.U

//  regArray1 := 0.U.asTypeOf(regArray1)
//  regArray2 := 0.U.asTypeOf(regArray2)
  //TODO to implement:
  // new implement.
  val maxIter = set_num

  val sendNS = WireInit(0.U(log2Ceil(maxIter+1).W))//WireInit(0.U(2.W))
  val sendCS = RegNext(sendNS)
//  val send = Wire(Decoupled(UInt(0.W)))
//  send.bits := DontCare
//  io.in.ready := sendCS === 0.U

  val recvNS = WireInit(0.U(log2Ceil(maxIter+1).W))//WireInit(0.U(2.W))//WireInit(0.U(log2Ceil(maxIter+1).W))
  val recvCS = RegNext(recvNS)

  TCComputation.io.in.bits.rm := io.in.bits.rm
  TCComputation.io.in.bits.ctrl := io.in.bits.ctrl
//  assert((sendCS === 0.U) === (TCComputation.io.in.ready && sendNS =/= 1.U))
  TCComputation.io.out.ready := io.out.ready
  TCComputation.io.in.valid := sendCS === 1.U || io.in.fire //|| sendCS === 1.U//false.B// io.in.valid

  io.in.ready := TCComputation.io.in.ready && sendCS =/= 1.U //&& sendCS =/= 2.U//sendCS === 0.U && //TCComputation.io.in.ready && sendNS =/= 1.U
  io.out.valid := recvCS === 2.U//false.B// TCComputation.io.out.valid

  switch(sendCS){
    is(0.U){
      when(io.in.fire){
        sendNS := 1.U
      }.otherwise{
        sendNS := 0.U//sendCS
      }
    }
    is(1.U){//1
      when(TCComputation.io.in.fire && recvCS === 0.U){
        sendNS := 2.U//sendCS +% 1.U
      }.otherwise{
        sendNS := 1.U//sendCS
      }
    }
    is(2.U){//2
      when(io.in.fire){
        sendNS := 1.U
      }.otherwise{
        sendNS := 2.U//sendCS
      }
    }
  }

  switch(sendNS) {
    is(0.U) {
      //Init.
      regSet2_A := 0.U.asTypeOf(regSet2_A)
      regSet2_B := 0.U.asTypeOf(regSet2_B)
      regSet2_C := 0.U.asTypeOf(regSet2_C)
      reg_rm := 0.U.asTypeOf(reg_rm)
      reg_ctrl := 0.U.asTypeOf(reg_ctrl)

//      io.in.ready := TCComputation.io.in.ready
    }
    is(1.U) {
      //      when(io.in.fire) {
//      io.in.ready := false.B
      assert(io.in.valid === true.B)
//      TCComputation.io.in.valid := true.B//io.in.valid
      // store set2 data
      //A 8*8 row
      for (m <- 0 until 8) {
        for (n <- 0 until 4) {
          regSet2_A(m * 8 + n * 2) := io.in.bits.data_in.in1(m * 4 + n)(15, 0)
          regSet2_A(m * 8 + n * 2 + 1) := io.in.bits.data_in.in1(m * 4 + n)(31, 16)
        }
      }
      //B 8*4 col
      for (m <- 0 until 4) {
        for (n <- 0 until 4) {
          regSet2_B(m * 8 + n * 2) := io.in.bits.data_in.in2(16 + m * 4 + n)(15, 0)
          regSet2_B(m * 8 + n * 2 + 1) := io.in.bits.data_in.in2(16 + m * 4 + n)(31, 16)
        }
      }
      //C 8*4 row First
      for (m <- 0 until 8) {
        regSet2_C(m * 4) := io.in.bits.data_in.in3(2 + m * 4)(15, 0)
        regSet2_C(m * 4 + 1) := io.in.bits.data_in.in3(2 + m * 4)(31, 16)
        regSet2_C(m * 4 + 2) := io.in.bits.data_in.in3(2 + m * 4 + 1)(15, 0)
        regSet2_C(m * 4 + 3) := io.in.bits.data_in.in3(2 + m * 4 + 1)(31, 16)
      }
      // save  ctrl and rm
      reg_rm := io.in.bits.rm
      reg_ctrl := io.in.bits.ctrl

      //get set1 data
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
      //      }
    }
    is(2.U) {
//      io.in.ready := TCComputation.io.in.ready
//      TCComputation.io.in.valid := true.B
      //      when(send.fire){
      //A 8*8 row
      for (i <- 0 until 64) {
        TCComputation.io.in.bits.A(i) := regSet2_A(i)
      }
      //B 8*4 col
      for (i <- 0 until 32) {
        TCComputation.io.in.bits.B(i) := regSet2_B(i)
      }
      //C 8*4 row First
      for (i <- 0 until 32) {
        TCComputation.io.in.bits.C(i) := regSet2_C(i)
      }
      // save  ctrl and rm

//      reg_rm := io.in.bits.rm
//      reg_ctrl := io.in.bits.ctrl
      TCComputation.io.in.bits.rm := reg_rm
      TCComputation.io.in.bits.ctrl := reg_ctrl
      //    }
      //    when(io.in.fire){ // won't trigger if sendCS===maxIter-1 (since io.in.ready===false)
      //      // store set2 data
      //      //A 8*8 row
      //      for (m <- 0 until 8) {
      //        for (n <- 0 until 4) {
      //          regSet2_A(m * 8 + n * 2) := io.in.bits.data_in.in1(m * 4 + n)(15, 0)
      //          regSet2_A(m * 8 + n * 2 + 1) := io.in.bits.data_in.in1(m * 4 + n)(31, 16)
      //        }
      //      }
      //      //B 8*4 col
      //      for (m <- 0 until 4) {
      //        for (n <- 0 until 4) {
      //          regSet2_B(m * 8 + n * 2) := io.in.bits.data_in.in2(16 + m * 4 + n)(15, 0)
      //          regSet2_B(m * 8 + n * 2 + 1) := io.in.bits.data_in.in2(16 + m * 4 + n)(31, 16)
      //        }
      //      }
      //      //C 8*4 row First
      //      for (m <- 0 until 8) {
      //        regSet2_C(m * 4) := io.in.bits.data_in.in3(2 + m * 4)(15, 0)
      //        regSet2_C(m * 4 + 1) := io.in.bits.data_in.in3(2 + m * 4)(31, 16)
      //        regSet2_C(m * 4 + 2) := io.in.bits.data_in.in3(2 + m * 4 + 1)(15, 0)
      //        regSet2_C(m * 4 + 3) := io.in.bits.data_in.in3(2 + m * 4 + 1)(31, 16)
      //      }
      //      // save  ctrl and rm
      //      reg_rm := io.in.bits.rm
      //      reg_ctrl := io.in.bits.ctrl
      ////      }
      //    }
    }
  }
//  send.valid := sendCS =/= 0.U
//  send.ready := TCComputation.io.in.ready
//  val recv = Wire(Decoupled(UInt(0.W)))
//  recv.bits := DontCare

  switch(recvCS){
    is(0.U){
      when(TCComputation.io.out.fire){
        recvNS := 1.U
      }.otherwise{
        recvNS := recvCS//0.U
      }
    }
    is(1.U){
      when(TCComputation.io.out.fire){
        recvNS := 2.U//recvCS +% 1.U
      }.otherwise{
        recvNS := recvCS //1.U
      }
    }
    is(2.U){
      when(io.out.fire){
        recvNS := 0.U
      }.otherwise{
        recvNS := recvCS
      }
    }
  }

  switch(recvNS){
    is(0.U){
      //Init.
      regArray1 := 0.U.asTypeOf(regArray1)
      regArray2 := 0.U.asTypeOf(regArray2)
//      io.out.valid := false.B
    }
    is(1.U){
      //        get set1 Result
      for (m<- 0 until DimN*DimM/set_num){
        regArray1(m) := TCComputation.io.out.bits.data(m).result
      }
//      io.out.valid := false.B
    }
    is(2.U){

      //        get set2 Result
      for (m<- 0 until DimN*DimM/set_num){
        regArray2(m) := TCComputation.io.out.bits.data(m).result
      }
    }
  }
//  recv.ready := recvCS =/= maxIter.U || (recvCS===maxIter.U && io.out.fire)
//  recv.valid := TCComputation.io.out.valid
//  TCComputation.io.out.ready := recv.ready

  io.out.bits.ctrl := TCComputation.io.out.bits.ctrl
}

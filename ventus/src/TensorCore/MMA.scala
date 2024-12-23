package TensorCore

import FPUv2.TCCtrl
import chisel3._
import chisel3.util._
import freechips.rocketchip.util.SeqBoolBitwiseOps
import pipeline.{DCacheCoreRsp_np, ShareMemCoreReq_np, TCCtrl_mulslot_v2}
import top.parameters._

// from TensorCore_MixedPrecision. Here we will depart them.
class TensorCore_MixedPrecision_multslot_simple(DimM: Int, DimN: Int, DimK: Int,slot_num:Int = num_warp, xDatalen:Int = 16, tcctrl:TCCtrl_mulslot_v2) extends Module {
  //  mnk defined as cuda.
  //  m8n8k8
  //  xDatalen: data bit len === xLen. FP16: xDatalen=16
  //  Compute: C[m8n8] = A[m8k8] * B[k8n8] + C[m8n8]
  //  A\B\C from Register.
  //  Now, this module can process FP16 data type(A\B\C: FP16) and mixed precision data type(A\B: FP16 C:FP32).

  val io = IO(new Bundle {
    val in = Flipped(DecoupledIO(new TC_MMAInput_MixedPrecision2(tcctrl)))
    val out = DecoupledIO(new TC_MMAOutput2(tcctrl))
  })
  // TODO: like GPGPU-sim tensor core, assert inst.active_count() != MAX_WARP_SIZE(32)
  val set_num = 2 //2 := (888/848)
  val dataWidth = log2Ceil(num_warp+1) // slot_num数据宽度
  //  val slot_num = num_warp
  // Init Register/FIFO(depth=1), Data output cache.
  val regArray1 = Reg(Vec(slot_num,Vec(DimN*DimM/set_num, UInt(xDatalen.W))))
  val regArray2 = Reg(Vec(slot_num,Vec(DimN*DimM/set_num, UInt(xDatalen.W))))

  // Init Register/FIFO(depth=1) for data set2.
  val regSet2_A = Reg(Vec(slot_num,Vec(DimM*DimK, UInt(xDatalen.W))))
  val regSet2_B = Reg(Vec(slot_num,Vec(DimN/set_num*DimK, UInt(xDatalen.W))))
  val regSet2_C = Reg(Vec(slot_num,Vec(DimM*DimN/set_num, UInt(xDatalen.W))))

  // Init data_out
  for(m<-0 until 32){
    io.out.bits.data_out(m) := 0.U
  }

  val maxIter = set_num
  val sendNS = VecInit(Seq.fill(slot_num)(WireInit(0.U(log2Ceil(maxIter+1).W))))//WireInit(Vec(slot_num,0.U(log2Ceil(maxIter+1).W)))//0.U(2.W)))//
  val sendCS = VecInit((0 until slot_num).map(i => RegNext(sendNS(i))))
  val recvNS = VecInit(Seq.fill(slot_num)(WireInit(0.U(log2Ceil(maxIter+1).W))))
  val recvCS = VecInit((0 until slot_num).map(i => RegNext(recvNS(i))))
  //  val recvNS = WireInit(0.U(log2Ceil(maxIter+1).W))
  //  val recvCS = RegNext(recvNS)

  // 6.U mean slot_num width.
  val slot = Wire(Vec(slot_num, Valid(UInt(dataWidth.W))))
  (0 until slot_num).foreach { iofL =>
    slot(iofL).valid := false.B//Mux(io.in.fire, io.in.valid, RegEnable(io.in.valid,io.in.fire))//reg_slot_valid(iofL)//false.B// io.in.valid & selSlotIdx1H(iofL)
    slot(iofL).bits := num_warp.U//Mux(io.in.fire, io.in.bits.ctrl.warpID, RegEnable(io.in.bits.ctrl.warpID,io.in.fire))//reg_slot_state(iofL)// // Mux(selSlotIdx1H(iofL), io.in.bits.ctrl.warpID, num_warp.U)
  }
  val selSlotIdx = WireInit(slot_num.U)

  //in-> slot
  selSlotIdx := Mux(io.in.fire,io.in.bits.ctrl.warpID,RegEnable(io.in.bits.ctrl.warpID,io.in.fire))
  slot(selSlotIdx).bits := Mux(io.in.fire,io.in.bits.ctrl.warpID,RegEnable(io.in.bits.ctrl.warpID,io.in.fire))
  when(io.in.fire) {
    slot(selSlotIdx).valid := true.B
  }

  // Init mode's lifelong mixedPrecision sigs.
  val sel_slot = selSlotIdx
  val reg_sel_slot = RegInit(num_warp.U)
  reg_sel_slot := sel_slot

  val tcctrl_mslot = Wire(new TCCtrl_mulslot_v2(xLen, depth_warp))
  tcctrl_mslot.isMixedPrecisionMode := io.in.bits.ctrl.isMixedPrecisionMode
  tcctrl_mslot.tc_ReLU := io.in.bits.ctrl.tc_ReLU
  tcctrl_mslot.tc_shape := io.in.bits.ctrl.tc_shape

  tcctrl_mslot.warpID := io.in.bits.ctrl.warpID
  tcctrl_mslot.reg_idxw := io.in.bits.ctrl.reg_idxw
  tcctrl_mslot.sel_slot_num := selSlotIdx
  if (SPIKE_OUTPUT){
    tcctrl_mslot.spike_info.get := io.in.bits.ctrl.spike_info.get
  }

  // Init TC Computation Array
  val TCComputation = Module(new TC_ComputationArray_MixedPrecision(16,8,4,8,tcCtrl = tcctrl_mslot))
  dontTouch(TCComputation.io.out)

  val reg_rm = Reg(Vec(slot_num,UInt(3.W)))
  val reg_ctrl = Reg(Vec(slot_num, tcctrl_mslot.cloneType))

  val reg_isMixSendMode = Reg(Vec(slot_num,Bool()))
  (0 until slot_num).foreach { iofL =>
    reg_isMixSendMode(iofL) := false.B
  }
  when(io.in.fire){
    reg_isMixSendMode(reg_sel_slot) := io.in.bits.ctrl.isMixedPrecisionMode//Mux(io.in.fire, io.in.bits.ctrl.isMixedPrecisionMode, RegEnable(io.in.bits.ctrl.isMixedPrecisionMode,io.in.fire))
  }

  val isMixSendMode = Mux(io.in.fire, io.in.bits.ctrl.isMixedPrecisionMode, RegEnable(io.in.bits.ctrl.isMixedPrecisionMode,io.in.fire))//RegInit(false.B)//Wire(false.B)//
//  val reg_ismix = RegNext(io.in.bits.ctrl.isMixedPrecisionMode)

  // Init TC Computation Array Input IO
  //A 8*8 row
  for (i <- 0 until 64) TCComputation.io.in.bits.A(i) := 0.U
  //B 8*4 col
  for (i <- 0 until 32) TCComputation.io.in.bits.B(i) := 0.U
  //C 8*4 row First
  for (i <- 0 until 32) TCComputation.io.in.bits.C(i) := 0.U

  //io.in.bits.isMixedPrecisionMode//
  TCComputation.io.in.bits.rm := io.in.bits.rm
  TCComputation.io.in.bits.ctrl := tcctrl_mslot//io.in.bits.ctrl
  //  TCComputation.io.in.bits.ctrl.isMixedPrecisionMode := isMixedPrec(sel_slot)
  //  TCComputation.io.in.bits.ctrl.sel_slot_num := sel_slot

  TCComputation.io.out.ready := io.out.ready
  io.out.valid := Mux(TCComputation.io.out.bits.ctrl.isMixedPrecisionMode || TCComputation.io.out.bits.ctrl.tc_shape===2.U,
    TCComputation.io.out.valid, recvCS(TCComputation.io.out.bits.ctrl.sel_slot_num) === 2.U)
  TCComputation.io.in.valid := Mux(isMixSendMode, slot.map(_.valid).reduce(_ || _), (sendCS(reg_sel_slot) === 1.U && reg_isMixSendMode(reg_sel_slot) === false.B) || slot.map(_.valid).reduce(_ || _))

//  val is1stage = VecInit(Seq.fill(slot_num)(WireInit(false.B)))
//  for (i <- 0 until slot_num) {
//    is1stage(i) := sendCS(i) === 1.U
//  }
//  // 如果有匹配的 slot，则选择该 slot
//  val is1stage_all = is1stage.reduce(_ || _)

  // 定义一个寄存器，标记是否要在下一个周期将 ready 置为 false
  val readyDelayed = RegInit(false.B)
  io.in.ready := !readyDelayed
  // 当输入握手完成，且 isMixedPrecisionMode 为 false 时，设置 readyDelayed 为 true
  when(io.in.fire && !io.in.bits.ctrl.isMixedPrecisionMode && io.in.bits.ctrl.tc_shape =/= 2.U) {
    readyDelayed := true.B
  }.elsewhen(readyDelayed) {
    // 在下一个周期将 readyDelayed 置回 false
    readyDelayed := false.B
  }

  // select output
  for(m<-0 until 8){
    io.out.bits.data_out(m*4) := Cat(regArray1(sel_slot)(m*4+1),regArray1(sel_slot)(m*4))
    io.out.bits.data_out(m*4+1) := Cat(regArray1(sel_slot)(m*4+3),regArray1(sel_slot)(m*4+2))
    io.out.bits.data_out(m*4+2) := Cat(regArray2(sel_slot)(m*4+1),regArray2(sel_slot)(m*4))
    io.out.bits.data_out(m*4+3) := Cat(regArray2(sel_slot)(m*4+3),regArray2(sel_slot)(m*4+2))
  }

  switch(sendCS(sel_slot)) {
    is(0.U) {
      //      mixed-Prec is not relative to this state
      when(io.in.fire && io.in.bits.ctrl.tc_shape =/= 2.U) {
        sendNS(sel_slot) := 1.U
      }.otherwise {
        sendNS(sel_slot) := 0.U
      }
    }
    is(1.U) { //1
      when(io.in.bits.ctrl.isMixedPrecisionMode ) {
        when(io.in.fire && io.in.bits.ctrl.tc_shape =/= 2.U) {
          sendNS(sel_slot) := 2.U //sendCS +% 1.U
//          isMixSendMode := true.B
        }.otherwise {
          sendNS(sel_slot) := 1.U //sendCS
//          isMixSendMode := true.B
        }
      }.otherwise {
        when(TCComputation.io.in.fire && recvCS(sel_slot) === 0.U && io.in.bits.ctrl.tc_shape =/= 2.U) {
          sendNS(sel_slot) := 2.U //sendCS +% 1.U
        }.otherwise {
          sendNS(sel_slot) := 1.U //sendCS
        }
      }
    }
    is(2.U) { //2
      //      mixed-Prec is not relative to this state
      when(io.in.fire) {
        sendNS(sel_slot) := 1.U
      }.otherwise {
        sendNS(sel_slot) := 2.U //sendCS
      }
    }
  }

  switch(sendNS(sel_slot)) {
    is(0.U) {
      //Init.
      regSet2_A(sel_slot) := 0.U.asTypeOf(regSet2_A(sel_slot))
      regSet2_B(sel_slot) := 0.U.asTypeOf(regSet2_B(sel_slot))
      regSet2_C(sel_slot) := 0.U.asTypeOf(regSet2_C(sel_slot))
      reg_rm(sel_slot) := 0.U.asTypeOf(reg_rm(sel_slot))
      reg_ctrl(sel_slot) := 0.U.asTypeOf(reg_ctrl(sel_slot))

    }
    is(1.U) {
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
      // store set2 data
      //A 8*8 row
      for (m <- 0 until 8) {
        for (n <- 0 until 4) {
          regSet2_A(sel_slot)(m * 8 + n * 2) := io.in.bits.data_in.in1(m * 4 + n)(15, 0)
          regSet2_A(sel_slot)(m * 8 + n * 2 + 1) := io.in.bits.data_in.in1(m * 4 + n)(31, 16)
        }
      }
      //B 8*4 col
      for (m <- 0 until 4) {
        for (n <- 0 until 4) {
          regSet2_B(sel_slot)(m * 8 + n * 2) := io.in.bits.data_in.in2(16 + m * 4 + n)(15, 0)
          regSet2_B(sel_slot)(m * 8 + n * 2 + 1) := io.in.bits.data_in.in2(16 + m * 4 + n)(31, 16)
        }
      }

      when(io.in.bits.ctrl.isMixedPrecisionMode) {
        // now, we don't need to store set2 data.
        // A B will be provided in vExeData.
        //C 8*4 row First
        for (m <- 0 until 32) {
          TCComputation.io.in.bits.C(m) := io.in.bits.data_in.in3(m)
        }
//        TCComputation.io.in.bits.ctrl := io.in.bits.ctrl
//        TCComputation.io.in.bits.rm := io.in.bits.rm
      }.otherwise {
//        assert(io.in.valid === true.B)
        //C 8*4 row First
        for (m <- 0 until 8) {
          regSet2_C(sel_slot)(m * 4) := io.in.bits.data_in.in3(2 + m * 4)(15, 0)
          regSet2_C(sel_slot)(m * 4 + 1) := io.in.bits.data_in.in3(2 + m * 4)(31, 16)
          regSet2_C(sel_slot)(m * 4 + 2) := io.in.bits.data_in.in3(2 + m * 4 + 1)(15, 0)
          regSet2_C(sel_slot)(m * 4 + 3) := io.in.bits.data_in.in3(2 + m * 4 + 1)(31, 16)
        }
        // save  ctrl and rm
        reg_rm(sel_slot) := io.in.bits.rm
        reg_ctrl(sel_slot) := tcctrl_mslot//io.in.bits.ctrl
        //C 8*4 row First
        for (m <- 0 until 8) {
          TCComputation.io.in.bits.C(m * 4) := Cat(0.U(16.W), io.in.bits.data_in.in3(m * 4)(15, 0))
          TCComputation.io.in.bits.C(m * 4 + 1) := Cat(0.U(16.W), io.in.bits.data_in.in3(m * 4)(31, 16))
          TCComputation.io.in.bits.C(m * 4 + 2) := Cat(0.U(16.W), io.in.bits.data_in.in3(m * 4 + 1)(15, 0))
          TCComputation.io.in.bits.C(m * 4 + 3) := Cat(0.U(16.W), io.in.bits.data_in.in3(m * 4 + 1)(31, 16))
        }
      }
    }
    is(2.U) {
      //      get A B from store data.
      //A 8*8 row
      for (k <- 0 until 64) {
        TCComputation.io.in.bits.A(k) := regSet2_A(sel_slot)(k)
      }
      //B 8*4 col
      for (k <- 0 until 32) {
        TCComputation.io.in.bits.B(k) := regSet2_B(sel_slot)(k)
      }
      when(io.in.bits.ctrl.isMixedPrecisionMode) {
        //C 8*4 row First
        for (m <- 0 until 32) {
          TCComputation.io.in.bits.C(m) := io.in.bits.data_in.in3(m)
        }
        // set ctrl and rm
//        TCComputation.io.in.bits.rm := io.in.bits.rm
//        TCComputation.io.in.bits.ctrl := tcctrl_mslot//io.in.bits.ctrl
      }.otherwise {
        //C 8*4 row First
        for (k <- 0 until 32) {
          TCComputation.io.in.bits.C(k) := regSet2_C(sel_slot)(k)
        }
        // save  ctrl and rm
        TCComputation.io.in.bits.rm := reg_rm(sel_slot)
        TCComputation.io.in.bits.ctrl := reg_ctrl(sel_slot)
      }
    }
  }

  // 848 shape
  when(io.in.fire && io.in.bits.ctrl.tc_shape === 2.U) {
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
    when(io.in.bits.ctrl.isMixedPrecisionMode) {
      // now, we don't need to store set2 data.
      // A B will be provided in vExeData.
      //C 8*4 row First
      for (m <- 0 until 32) {
        TCComputation.io.in.bits.C(m) := io.in.bits.data_in.in3(m)
      }
    }.otherwise {
      //C 8*4 row First
      for (m <- 0 until 8) {
        TCComputation.io.in.bits.C(m * 4) := Cat(0.U(16.W), io.in.bits.data_in.in3(m * 4)(15, 0))
        TCComputation.io.in.bits.C(m * 4 + 1) := Cat(0.U(16.W), io.in.bits.data_in.in3(m * 4)(31, 16))
        TCComputation.io.in.bits.C(m * 4 + 2) := Cat(0.U(16.W), io.in.bits.data_in.in3(m * 4 + 1)(15, 0))
        TCComputation.io.in.bits.C(m * 4 + 3) := Cat(0.U(16.W), io.in.bits.data_in.in3(m * 4 + 1)(31, 16))
      }
    }
  }

  switch(recvCS(TCComputation.io.out.bits.ctrl.sel_slot_num)) {
    is(0.U) {
      when(TCComputation.io.out.fire && TCComputation.io.out.bits.ctrl.tc_shape =/= 2.U) {
        recvNS(TCComputation.io.out.bits.ctrl.sel_slot_num) := 1.U
//      }.elsewhen(TCComputation.io.out.fire && TCComputation.io.out.bits.ctrl.tc_shape === 2.U){
//        recvNS(TCComputation.io.out.bits.ctrl.sel_slot_num) := 2.U
//      }.elsewhen(io.out.fire && TCComputation.io.out.bits.ctrl.tc_shape === 2.U){
//        recvNS(TCComputation.io.out.bits.ctrl.sel_slot_num) := 0.U
      }.otherwise {
        recvNS(TCComputation.io.out.bits.ctrl.sel_slot_num) := recvCS(TCComputation.io.out.bits.ctrl.sel_slot_num) //0.U
      }
    }
    is(1.U) {
      when(TCComputation.io.out.bits.ctrl.isMixedPrecisionMode && TCComputation.io.out.bits.ctrl.tc_shape =/= 2.U) {
        when(io.out.fire) {
          recvNS(TCComputation.io.out.bits.ctrl.sel_slot_num) := 2.U //recvCS +% 1.U
        }.otherwise {
          recvNS(TCComputation.io.out.bits.ctrl.sel_slot_num) := recvCS(TCComputation.io.out.bits.ctrl.sel_slot_num) //1.U
        }
      }.otherwise {
        when(TCComputation.io.out.fire && TCComputation.io.out.bits.ctrl.tc_shape =/= 2.U) {
          recvNS(TCComputation.io.out.bits.ctrl.sel_slot_num) := 2.U //recvCS +% 1.U
        }.otherwise {
          recvNS(TCComputation.io.out.bits.ctrl.sel_slot_num) := recvCS(TCComputation.io.out.bits.ctrl.sel_slot_num) //1.U
        }
      }
    }
    is(2.U) {
      when(io.out.fire && TCComputation.io.out.bits.ctrl.tc_shape =/= 2.U) {
        recvNS(TCComputation.io.out.bits.ctrl.sel_slot_num) := 0.U
//      }.elsewhen(TCComputation.io.out.fire && TCComputation.io.out.bits.ctrl.tc_shape === 2.U){
//        recvNS(TCComputation.io.out.bits.ctrl.sel_slot_num) := 0.U
      }.otherwise {
        recvNS(TCComputation.io.out.bits.ctrl.sel_slot_num) := recvCS(TCComputation.io.out.bits.ctrl.sel_slot_num)
      }
    }
  }

  switch(recvNS(TCComputation.io.out.bits.ctrl.sel_slot_num)) {
    is(0.U) {
      //Init.
      regArray1(TCComputation.io.out.bits.ctrl.sel_slot_num) := 0.U.asTypeOf(regArray1(TCComputation.io.out.bits.ctrl.sel_slot_num))
      regArray2(TCComputation.io.out.bits.ctrl.sel_slot_num) := 0.U.asTypeOf(regArray2(TCComputation.io.out.bits.ctrl.sel_slot_num))
      //      io.out.valid := false.B
    }
    is(1.U) {
      when(TCComputation.io.out.bits.ctrl.isMixedPrecisionMode) {
        for (m <- 0 until 32) {
          io.out.bits.data_out(m) := TCComputation.io.out.bits.data(m).result
        }
      } otherwise {
        for (m <- 0 until DimN * DimM / set_num) {
          regArray1(TCComputation.io.out.bits.ctrl.sel_slot_num)(m) := TCComputation.io.out.bits.data(m).result
        }
        // here out is init.
      }
    }
    is(2.U) {
//      restore the slot status.
      slot(TCComputation.io.out.bits.ctrl.sel_slot_num).bits := num_warp.U
      when(TCComputation.io.out.bits.ctrl.isMixedPrecisionMode) {
        for (m <- 0 until 32) {
          io.out.bits.data_out(m) := TCComputation.io.out.bits.data(m).result
        }
      }. otherwise {
        //  get set2 Result
        for (m <- 0 until DimN * DimM / set_num) {
          regArray2(TCComputation.io.out.bits.ctrl.sel_slot_num)(m) := TCComputation.io.out.bits.data(m).result
        }
        // here out is init.
      }
    }
  }

  when(TCComputation.io.out.fire && TCComputation.io.out.bits.ctrl.tc_shape === 2.U){
   slot(TCComputation.io.out.bits.ctrl.sel_slot_num).bits := num_warp.U
//    io.out.valid := true.B
   when(TCComputation.io.out.bits.ctrl.isMixedPrecisionMode){
     for (m <- 0 until 32) {
       io.out.bits.data_out(m) := TCComputation.io.out.bits.data(m).result
     }
   }.otherwise{
    for(m<-0 until 8){
      io.out.bits.data_out(m*4) := Cat(TCComputation.io.out.bits.data(m*4+1).result,TCComputation.io.out.bits.data(m*4).result)
      io.out.bits.data_out(m*4+1) := Cat(TCComputation.io.out.bits.data(m*4+3).result,TCComputation.io.out.bits.data(m*4+2).result)
      io.out.bits.data_out(m*4+2) := 0.U//Cat(regArray2(sel_slot)(m*4+1),regArray2(sel_slot)(m*4))
      io.out.bits.data_out(m*4+3) := 0.U//Cat(regArray2(sel_slot)(m*4+3),regArray2(sel_slot)(m*4+2))
    }
   }
  }

  io.out.bits.ctrl := TCComputation.io.out.bits.ctrl
}

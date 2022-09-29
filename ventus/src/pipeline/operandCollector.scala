/*
 * Copyright (c) 2021-2022 International Innovation Center of Tsinghua University, Shanghai
 * Ventus is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details. */
package pipeline

import chisel3._
import chisel3.util._
import parameters._
import IDecode._

class WriteVecCtrl extends Bundle{
  val wb_wfd_rd=(Vec(num_thread,UInt(xLen.W)))
  val wfd_mask=Vec(num_thread,Bool())
  val wfd=Bool()
  val reg_idxw=UInt(5.W)
  val warp_id=UInt(depth_warp.W)
}
class WriteScalarCtrl extends Bundle{
  val wb_wxd_rd=(UInt(xLen.W))
  val wxd=Bool()
  val reg_idxw=UInt(5.W)
  val warp_id=UInt(depth_warp.W)
}

// One of the number of num_warp collector Units, instantiating this class in operand collector for num_warps.
class CtrlSigsInputCU extends Bundle{
  val wid = Input(UInt(depth_warp.W))
  val decode = Input(UInt(6.W))
  val regId = Input(Vec(4, UInt(5.W)))
}
class collectorUnit extends Module{
  val io = IO(new Bundle{
    val ctrlSigsInput = Flipped(Decoupled(new CtrlSigsInputCU))
    val bankIn = Flipped(Decoupled(UInt(xLen.W)))
    val issue = Decoupled(Vec(4, UInt(xLen.W)))
    val outArbiterIO = Decoupled(new Bundle{
      val rsAddr = Vec(4, UInt(depth_regBank.W))
      val bankID = Vec(4, UInt(log2Ceil(num_bank).W))
    })

    //    val ready = Input(Bool())
  })
  val wid = Reg(UInt(depth_warp.W))
  //  val decode = Reg(UInt(6.W))
  val ready = RegInit(0.U(3.W))
  val valid = RegInit(0.U(3.W))
  val regId = Vec(4, RegInit(UInt(5.W), 0.U))
  val rsReg = Vec(4, RegInit(UInt(xLen.W), 0.U))


  val s_idle :: s_add :: s_out :: Nil = Enum(3)
  val state = RegInit(s_idle)

  io.bankIn.ready := state===s_add
  io.outArbiterIO.ready := (state===s_idle && io.ctrlSigsInput.fire)
  io.issue.valid := state===s_out
  io.ctrlSigsInput.ready := (state===s_idle && !valid.asUInt.orR)

  val bankIdLookup = (0 until num_warp+32).map { x =>
    (x -> x % num_bank)
  }.map { x => (x._1.U -> x._2.U)
  }
  val addrLookup = (0 until 32).map { x =>
    (x -> x % (num_bank))
  }.map {x => (x._1.U -> x._2.U)}

  when(state === s_idle) {
    when(io.ctrlSigsInput.fire && !valid.asUInt.orR){
      state := s_add
    }.otherwise{state := s_idle}
  }.elsewhen (state === s_add) {
    when(valid.asUInt =/= ready.asUInt ) {
      state := s_add
      //    }.elsewhen(io.bankIn.fire){
      //      state := s_out
    }.otherwise{state := s_idle}
  }.elsewhen(state === s_out) {
    when(io.issue.ready){
      state := s_idle
    }.otherwise{state := s_out}
  }.otherwise{state := s_idle}

  switch(state){
    is(s_idle){
      when(io.ctrlSigsInput.fire){
        wid := io.ctrlSigsInput.bits.wid
        regId := io.ctrlSigsInput.bits.regId
        //        decode := io.ctrlSigsInput.bits.decode
        valid := "b111".U
        ready := "b000".U
        for(i <- 0 until 4){
          io.outArbiterIO.bits.bankID(i) := MuxLookup(io.ctrlSigsInput.bits.regId(i), 0.U, bankIdLookup)
          io.outArbiterIO.bits.rsAddr(i) := MuxLookup(io.ctrlSigsInput.bits.regId(i), 0.U, addrLookup) + wid*(32/num_bank).U
        }
      }
    }
    is(s_add){
      when(io.bankIn.fire){
        ready := Cat((ready << 1)(2,1), 1.U(1.W))
        rsReg(PriorityEncoder(~ready)) := io.bankIn.bits
      }
    }
    is(s_out){
      when(io.issue.fire){
        io.issue.bits := rsReg
      }
    }
  }
}

class operandArbiter extends Module{
  val io = IO(new Bundle{
    val readArbiterIO = Vec(num_collectorUnit, Flipped(Decoupled(new Bundle {
      val rsAddr = Vec(4, UInt(depth_regBank.W))
      val bankID = Vec(4, UInt(log2Ceil(num_bank).W))
    })))
    val readAddr = Output(Vec(num_bank, UInt(depth_regBank.W))) //address of registers to be read that in bank
    val readchosen = Output(Vec(num_bank, UInt((4*num_collectorUnit).W)))// which operand read request is chosen
    //    val writeArbiterIO = Decoupled(/*write arbiter, TBD   */)

  })

  //  val bankArbiter = Seq.fill(num_bank, new RRArbiter(UInt(depth_regBank.W), 3*num_collectorUnit))
  val bankArbiter = for(i<-0 until num_bank)yield{
    val x = Module(new RRArbiter(UInt(depth_regBank.W), 4*num_collectorUnit))
    x
  }
  val vecRsIO = Vec(4* num_collectorUnit, UInt(depth_regBank.W))

  val vecBankIDIO = Vec(4*num_collectorUnit, UInt(log2Ceil(num_bank).W))
  //flatten readArbiterIO
  for (i <-0 until num_collectorUnit) for (j<-0 until 4){
    vecRsIO(i*4+j) := io.readArbiterIO(i).bits.rsAddr(j)
    vecBankIDIO(i*4+j) := io.readArbiterIO(i).bits.bankID(j)
  }

  for (i <- 0 until num_bank){
    //    mapping input signals from collector units to inputs of Arbiters
    bankArbiter(i).io.in := vecRsIO.map(x => Decoupled(x))
  }
  //elaborate valid port of readArbiters
  bankArbiter.foreach(x => {
    for(i <- 0 until 4*num_collectorUnit) {
      x.io.in(i).valid := io.readArbiterIO(i).valid && vecBankIDIO(i).asBool()
    }
  })
  io.readAddr := VecInit(bankArbiter.map(x => x.io.out.bits))
  io.readchosen := VecInit(bankArbiter.map(x => x.io.chosen))

}

class operandCollector extends Module{
  val io=IO(new Bundle {
    val control=Input(new CtrlSigs())
    //val inst=Input(UInt(32.W))
    val alu_src1=Output(Vec(num_thread,UInt(xLen.W)))
    val alu_src2=Output(Vec(num_thread,UInt(xLen.W)))
    val alu_src3=Output(Vec(num_thread,UInt(xLen.W)))
    val mask=Output(Vec(num_thread,Bool()))
    val writeScalarCtrl=Flipped(DecoupledIO(new WriteScalarCtrl)) //should be used as decoupledIO
    val writeVecCtrl=Flipped(DecoupledIO(new WriteVecCtrl))
  })





  val vectorRegFile=VecInit(Seq.fill(num_bank)(Module(new FloatRegFileBank).io))
  val scalarRegFile=VecInit(Seq.fill(num_bank)(Module(new RegFileBank).io))
  val imm=Module(new ImmGen())

  val collectorUnitIO=VecInit(Seq.fill(num_warp)(new collectorUnit))

  imm.io.inst:=io.control.inst
  imm.io.sel:=io.control.sel_imm

  vectorRegFile.zipWithIndex.foreach((x, i) => {
    x._1.rsidx:=io.control.reg_idx1
    x.rs2idx:=io.control.reg_idx2
    x.rs3idx:=io.control.reg_idx3
    x.rdidx:=io.writeVecCtrl.bits.reg_idxw
    x.rd:=io.writeVecCtrl.bits.wb_wfd_rd
    x.rdwen:=false.B
    x.rdwmask:=io.writeVecCtrl.bits.wfd_mask
    //y.rdwen:=io.writeCtrl.wfd
  })
  scalarRegFile.foreach(y=>{
    y.rs1idx:=io.control.reg_idx1
    y.rs2idx:=io.control.reg_idx2
    y.rs3idx:=io.control.reg_idx3
    y.rdIdx:=io.writeScalarCtrl.bits.reg_idxw
    y.rd:=io.writeScalarCtrl.bits.wb_wxd_rd
    y.rdwen:=false.B
    //y.rdwen:=io.writeCtrl.wxd
  })
  vectorRegFile(io.writeVecCtrl.bits.warp_id).rdwen:=io.writeVecCtrl.bits.wfd&io.writeVecCtrl.valid
  scalarRegFile(io.writeScalarCtrl.bits.warp_id).rdwen:=io.writeScalarCtrl.bits.wxd&io.writeScalarCtrl.valid
  io.writeScalarCtrl.ready:=true.B
  io.writeVecCtrl.ready:=true.B

  (0 until num_thread).foreach(x=>{
    io.alu_src1(x):=MuxLookup(io.control.sel_alu1,0.U,Array(A1_RS1->scalarRegFile(io.control.wid).rs1,A1_VRS1->(vectorRegFile(io.control.wid).rs1(x)),A1_IMM->imm.io.out,A1_PC->io.control.pc))//io.control.reg_idx1))
    io.alu_src2(x):=MuxLookup(io.control.sel_alu2,0.U,Array(A2_RS2->scalarRegFile(io.control.wid).rs2,A2_IMM->imm.io.out,A2_VRS2->vectorRegFile(io.control.wid).rs2(x),A2_SIZE->4.U))
    io.alu_src3(x):=MuxLookup(io.control.sel_alu3,0.U,Array(A3_PC->Mux(io.control.branch===B_R,(imm.io.out+scalarRegFile(io.control.wid).rs1),(io.control.pc+imm.io.out)),A3_VRS3->vectorRegFile(io.control.wid).rs3(x),A3_SD->Mux(io.control.isvec,vectorRegFile(io.control.wid).rs3(x),scalarRegFile(io.control.wid).rs2),A3_FRS3->(scalarRegFile(io.control.wid).rs3)))
    io.mask(x):=Mux(io.control.mask,vectorRegFile(io.control.wid).v0(0).apply(x),Mux(io.control.isvec,true.B,!x.asUInt.orR))

  })

}


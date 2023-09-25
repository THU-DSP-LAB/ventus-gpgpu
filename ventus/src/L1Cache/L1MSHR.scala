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
package L1Cache

import L1Cache.{HasL1CacheParameters, L1CacheModule}
import chisel3._
import chisel3.util._
import top.parameters._

//abstract class MSHRBundle extends Bundle with L1CacheParameters

class MSHRprobe(val bABits: Int) extends Bundle {
  val blockAddr = UInt(bABits.W)
}
class MSHRprobeOut(val NEntry:Int, val NSub:Int) extends Bundle {
  val probeStatus = UInt(3.W)
  val a_source = UInt(log2Up(NEntry).W)
}
class MSHRmissReq(val bABits: Int, val tIWdith: Int, val WIdBits: Int) extends Bundle {// Use this bundle when handle miss issued from pipeline
  val blockAddr = UInt(bABits.W)
  val instrId = UInt(WIdBits.W)
  val targetInfo = UInt(tIWdith.W)
}
class MSHRmissRspIn(val NEntry: Int) extends Bundle {//Use this bundle when a block return from Lower cache
  val instrId = UInt(log2Up(NEntry).W)
}
class MSHRmissRspOut[T <: Data](val bABits: Int, val tIWdith: Int, val WIdBits: Int) extends Bundle {
  val targetInfo = UInt(tIWdith.W)
  val blockAddr = UInt(bABits.W)
  val instrId = UInt(WIdBits.W)
  //val burst = Bool()//This bit indicate the Rsp transaction comes from subentry
  //val last = Bool()
}

class getEntryStatusReq(nEntry: Int) extends Module{
  val io = IO(new Bundle{
    val valid_list = Input(UInt(nEntry.W))
    val alm_full = Output(Bool())
    val full = Output(Bool())
    val next = Output(UInt(log2Up(nEntry).W))
    //val used = Output(UInt())
  })

  val used: UInt = PopCount(io.valid_list)
  io.alm_full := used === (nEntry.U-1.U)
  io.full := io.valid_list.andR
  io.next := VecInit(io.valid_list.asBools).indexWhere(_ === false.B)
}

class getEntryStatusRsp(nEntry: Int) extends Module{
  val io = IO(new Bundle{
    val valid_list = Input(UInt(nEntry.W))
    val next2cancel = Output(UInt(log2Up(nEntry).W))
    val used = Output(UInt((log2Up(nEntry)+1).W))
  })
  io.next2cancel := VecInit(io.valid_list.asBools).indexWhere(_ === true.B)
  io.used := PopCount(io.valid_list)

}


class MSHR(val bABits: Int, val tIWidth: Int, val WIdBits: Int, val NMshrEntry:Int, val NMshrSubEntry:Int) extends Module{
  val io = IO(new Bundle{
    val probe = Flipped(ValidIO(new MSHRprobe(bABits)))
    val probeOut_st1 = Output(new MSHRprobeOut(NMshrEntry,NMshrSubEntry))
    val missReq = Flipped(Decoupled(new MSHRmissReq(bABits,tIWidth,WIdBits)))
    val missRspIn = Flipped(Decoupled(new MSHRmissRspIn(NMshrEntry)))
    val missRspOut = ValidIO(new MSHRmissRspOut(bABits,tIWidth,WIdBits))
    //For InOrFlu
    val empty = Output(Bool())
    val probestatus = Output(Bool())
  })
  // head of entry, for comparison
  val blockAddr_Access = RegInit(VecInit(Seq.fill(NMshrEntry)(0.U(bABits.W))))
  val instrId_Access = RegInit(VecInit(Seq.fill(NMshrEntry)(0.U(WIdBits.W))))//TODO remove this
  val targetInfo_Accesss = RegInit(VecInit(Seq.fill(NMshrEntry)(VecInit(Seq.fill(NMshrSubEntry)(0.U(tIWidth.W))))))

  val subentry_valid = RegInit(VecInit(Seq.fill(NMshrEntry)(VecInit(Seq.fill(NMshrSubEntry)(false.B)))))
  val entry_valid = Reverse(Cat(subentry_valid.map(Cat(_).orR)))
  val probestatus = RegInit(false.B)
  io.empty := !entry_valid.orR
  io.probestatus := probestatus
  /*Structure Diagram
  * bA  : blockAddr
  * tI  : targetInfo
  * iI  : instrId
  * e_v : entry_valid, which is the first column of s_v
  * s_v : subentry_valid
  * N : NMshrEntry
  * n : NMshrSubEntry
  *
  * reg     +reg +reg    || SRAM
  * s_v(0,0)+bA#0+iI#0   || tI#0 | s_v(0,1)+tI#1 | ... | s_v(N,n)+tI#n
  * s_v(1,0)+bA#1+iI#1   || tI#0 | s_v(1,1)+tI#1 | ... | s_v(N,n)+tI#n
  * .
  * .
  * .
  * s_v(N,0)+bA#N+iI#N   || tI#0 | s_v(N,1)+tI#1 | ... | s_v(N,n)+tI#n
  *
  * for dcache, every missRep tI include iI, but only useful when this request is primary miss
  * this iI will be recorded as iI for this missing cache line fetch request to L2
  * */

  //  ******     missReq decide selected subentries are full or not     ******
  val entryMatchMissRsp = Wire(UInt(log2Up(NMshrEntry).W))
  val entryMatchProbe = Wire(UInt(NMshrEntry.W))
  val subentrySelectedForReq = subentry_valid(OHToUInt(entryMatchProbe))
  val subentryStatus = Module(new getEntryStatusReq(NMshrSubEntry)) // Output: alm_full, full, next
  subentryStatus.io.valid_list := Reverse(Cat(subentrySelectedForReq))

  //  ******     missRsp status      ******
  val subentryStatusForRsp = Module(new getEntryStatusRsp(NMshrSubEntry))

  //  ******     missReq decide MSHR is full or not     ******
  val entryStatus = Module(new getEntryStatusReq(NMshrEntry))
  entryStatus.io.valid_list := entry_valid

  // ******     enum vec_mshr_status     ******
  val mshrStatus_st1_r = RegInit(0.U(3.W))
  val mshrStatus_st1_w = Wire(UInt(3.W))
  /*PRIMARY_AVAIL         000
  * PRIMARY_FULL          001
  * SECONDARY_AVAIL       010
  * SECONDARY_FULL        011
  * SECONDARY_FULL_RETURN 100
  * PRIMARY_ALM_FULL      101
  * SECONDARY_ALM_FULL    111
  * see as always valid, validity relies on external procedures
  * */
  // ******      mshr::probe_vec    ******
  entryMatchProbe := Reverse(Cat(blockAddr_Access.map(_ === io.probe.bits.blockAddr))) & entry_valid
  assert(PopCount(entryMatchProbe) <= 1.U)
  val entryMatchProbeid_reg = RegNext(OHToUInt(entryMatchProbe))
  val secondaryMiss = RegNext(entryMatchProbe.orR)
  val primaryMiss = !secondaryMiss
  val mainEntryFull = entryStatus.io.full
  val mainEntryAlmFull = entryStatus.io.alm_full
  val subEntryFull = subentryStatus.io.full
  val subEntryAlmFull = subentryStatus.io.alm_full
  when(io.missReq.fire){
    when(primaryMiss && mainEntryAlmFull) {
      mshrStatus_st1_r := 1.U //PRIMARY_FULL
    }.elsewhen(secondaryMiss && subEntryAlmFull) {
      mshrStatus_st1_r := 3.U //SECONDARY_FULL
    }
  }.elsewhen(io.probe.valid){
    when(primaryMiss) {
      when(mainEntryFull) {
        mshrStatus_st1_r := 1.U //PRIMARY_FULL
        //}.elsewhen(mainEntryAlmFull) {
        //  mshrStatus_st1 := 5.U //PRIMARY_ALM_FULL
      }.otherwise {
        mshrStatus_st1_r := 0.U //PRIMARY_AVAIL
      }
    }.otherwise {
      when(subEntryFull) {
        mshrStatus_st1_r := 3.U //SECONDARY_FULL
        //}.elsewhen(subEntryAlmFull) {
        //  mshrStatus_st1 := 7.U //SECONDARY_ALM_FULL
      }.otherwise {
        mshrStatus_st1_r := 2.U //SECONDARY_AVAIL
      }
    }
  }.elsewhen(io.missRspIn.valid){
    //assert(!(mshrStatus_st1_r === 4.U),"mshr set SECONDARY_FULL_RETURN incorrectly")
    when(mshrStatus_st1_r === 1.U || mshrStatus_st1_r === 2.U){
      mshrStatus_st1_r := 0.U //PRIMARY_AVAIL
    }.elsewhen(mshrStatus_st1_r === 3.U && subentryStatusForRsp.io.used === 1.U){
      mshrStatus_st1_r := 4.U //SECONDARY_FULL_RETURN
    }.elsewhen(mshrStatus_st1_r === 4.U && subentryStatusForRsp.io.used === 0.U) {
      mshrStatus_st1_r := 2.U //SECONDARY_AVAIL
    }
  }
  val entryMatchProbe_st1 = RegEnable(entryMatchProbe,io.probe.valid)
  //mshrStatus依赖primaryMiss和SecondaryMiss，它们依赖entryValid。
  //mshrStatus必须是寄存器，需要在probe valid的下个周期正确显示。entryValid更新的下一个周期已经来不及。
  //所以用组合逻辑加工一次mshrStatus。
  when(secondaryMiss && (mshrStatus_st1_r === 0.U || mshrStatus_st1_r === 1.U)){
    when(subEntryFull) {
      mshrStatus_st1_w := 3.U //SECONDARY_FULL
    }.otherwise {
      mshrStatus_st1_w := 2.U //SECONDARY_AVAIL
    }
  }.otherwise{
    mshrStatus_st1_w := mshrStatus_st1_r
  }
  io.probeOut_st1.probeStatus := mshrStatus_st1_w
  when(io.probe.valid && !probestatus){
    probestatus := true.B
  }.elsewhen(probestatus){
    when(io.missReq.valid ){
      probestatus := false.B
    }
  }
  //  ******     mshr::allocate_vec_sub/allocate_vec_main     ******
  /*0:PRIMARY_AVAIL 1:PRIMARY_FULL 2:SECONDARY_AVAIL 3:SECONDARY_FULL*/
  io.missReq.ready := !(mshrStatus_st1_w === 1.U || mshrStatus_st1_w === 3.U || io.missRspIn.valid) //&& probestatus
  assert(!io.missReq.fire || (io.missReq.fire && !io.missRspIn.fire),"MSHR cant have Req & Rsp valid in same cycle, later the prior")
  val real_SRAMAddrUp = Mux(secondaryMiss,OHToUInt(entryMatchProbe_st1),entryStatus.io.next)
  val real_SRAMAddrDown = Mux(secondaryMiss,subentryStatus.io.next,0.U)
  when (io.missReq.fire){
    targetInfo_Accesss(real_SRAMAddrUp)(real_SRAMAddrDown) := io.missReq.bits.targetInfo
  }

  when(io.missReq.fire && mshrStatus_st1_w === 0.U && probestatus) {//PRIMARY_AVAIL
    blockAddr_Access(entryStatus.io.next) := io.missReq.bits.blockAddr
    instrId_Access(entryStatus.io.next) := io.missReq.bits.instrId
  }

  io.probeOut_st1.a_source := real_SRAMAddrUp

  //  ******      mshr::vec_arrange_core_rsp    ******
  subentryStatusForRsp.io.valid_list := Reverse(Cat(subentry_valid(entryMatchMissRsp)))
  // priority: missRspIn > missReq
  //assert(!io.missRspIn.fire || (io.missRspIn.fire && subentryStatus.io.used >= 1.U))
  //This version allow missRspIn fire when no subentry are left
  //如果后面发现missRspOut端口这一级不能取消，使用这段注释掉的代码
  //io.missRspIn.ready := !(subentryStatusForRsp.io.used >= 2.U ||
  //  (subentryStatusForRsp.io.used === 1.U && !io.missRspOut.ready))
  io.missRspIn.ready := !((subentryStatusForRsp.io.used >= 2.U) ||
    ((mshrStatus_st1_w === 4.U || mshrStatus_st1_w === 3.U) && subentryStatusForRsp.io.used === 1.U))

  entryMatchMissRsp := io.missRspIn.bits.instrId
  //entryMatchMissRsp := Reverse(Cat(instrId_Access.map(_ === io.missRspIn.bits.instrId))) & entry_valid
  //assert(PopCount(entryMatchMissRsp) <= 1.U,"MSHR missRspIn, cant match multiple entries")
  val subentry_next2cancel = Wire(UInt(log2Up(NMshrSubEntry).W))
  subentry_next2cancel := subentryStatusForRsp.io.next2cancel

  val missRspTargetInfo_st0 = targetInfo_Accesss(entryMatchMissRsp)(subentry_next2cancel)
  val missRspBlockAddr_st0 = blockAddr_Access(entryMatchMissRsp)

  io.missRspOut.bits.targetInfo := RegNext(missRspTargetInfo_st0)
  io.missRspOut.bits.blockAddr := RegNext(missRspBlockAddr_st0)
  io.missRspOut.bits.instrId := io.missRspIn.bits.instrId
  io.missRspOut.valid := RegNext(io.missRspIn.valid)
  //io.missRspOut := RegNext(io.missRspIn.valid) &&
  //  subentryStatusForRsp.io.used >= 1.U//如果上述Access中改出SRAM，本信号需要延迟一个周期

  //  ******     maintain subentries    ******
  /*0:PRIMARY_AVAIL 1:PRIMARY_FULL 2:SECONDARY_AVAIL 3:SECONDARY_FULL*/
  for (iofEn <- 0 until NMshrEntry){
    for (iofSubEn <- 0 until NMshrSubEntry){
      when(iofEn.asUInt===entryStatus.io.next &&
        iofSubEn.asUInt===0.U && io.missReq.fire && primaryMiss && probestatus){
        subentry_valid(iofEn)(iofSubEn) := true.B
      }.elsewhen(iofEn.asUInt===entryMatchMissRsp){
        when(iofSubEn.asUInt===subentry_next2cancel &&
          io.missRspIn.valid){
          subentry_valid(iofEn)(iofSubEn) := false.B
        }
      }.elsewhen(iofSubEn.asUInt === subentryStatus.io.next &&
        io.missReq.fire && secondaryMiss && probestatus && iofEn.asUInt === entryMatchProbeid_reg) {
        subentry_valid(iofEn)(iofSubEn) := true.B
      }//order of when & elsewhen matters, as elsewhen cover some cases of when, but no op to them
    }
  }
}

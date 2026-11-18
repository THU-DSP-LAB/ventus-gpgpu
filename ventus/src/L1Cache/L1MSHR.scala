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
import chisel3.DontCare.:=
import chisel3._
import chisel3.util._
import top.cache_spike_info
import top.parameters._

//abstract class MSHRBundle extends Bundle with L1CacheParameters

class MSHRprobe(val bABits: Int, val AsidBits: Int) extends Bundle {
  val blockAddr = UInt(bABits.W)
  //val ASID = UInt(AsidBits.W)
}
class MSHRprobeOut(val NEntry:Int, val NSub:Int) extends Bundle {
  val probeStatus = UInt(3.W)
  val a_source = UInt(log2Up(NEntry).W)
}
class MSHRmissReq(val bABits: Int, val tIWdith: Int, val InstrIdBits: Int, val AsidBits: Int) extends Bundle {// Use this bundle when handle miss issued from pipeline
  val blockAddr = UInt(bABits.W)
  val instrId = UInt(InstrIdBits.W)
  val targetInfo = UInt(tIWdith.W)
  //val ASID = UInt(AsidBits.W)
}
class SMSHRmissReq (val bABits: Int, val tIWdith: Int, val InstrIdBits: Int, val AsidBits: Int) extends Bundle{
  val blockAddr = UInt(bABits.W)
  val instrId = UInt(InstrIdBits.W)
  val targetInfo = UInt(tIWdith.W)
  val wordOffset = UInt(dcache_BlockOffsetBits.W)
  val Type = UInt(2.W) // 1-lr 2- sc 3-amo 0- normal
}
class SMSHRprobeOut(val NEntry: Int) extends Bundle{
  val hitblockIdx = UInt(log2Up(NEntry).W)
  val hitblock = Bool()
  val LRexist = Bool()
  val a_source = UInt(log2Up(NEntry).W)
}
class MSHRmissRspIn(val NEntry: Int) extends Bundle {//Use this bundle when a block return from Lower cache
  val instrId = UInt(log2Up(NEntry).W)
}
class MSHRmissRspOut[T <: Data](val bABits: Int, val tIWdith: Int, val InstrIdBits: Int, val AsidBits: Int) extends Bundle {
  val targetInfo = UInt(tIWdith.W)
  val blockAddr = UInt(bABits.W)
  val instrId = UInt(InstrIdBits.W)
  val UncacheRsp = Bool()
  //val ASID = UInt(AsidBits.W)
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

class MSHRpipe1Reg(WidthMatchProbe: Int, SubEntryNext: Int) extends Bundle{
  val entryMatchProbe = UInt(WidthMatchProbe.W)
  val subEntryIdx = UInt(SubEntryNext.W)
  val full = Bool()
}

object MSHRStatus{
  def PrimaryAvail : UInt = 0.U(3.W)
  def PrimaryFull : UInt = 1.U(3.W)
  def SecondaryAvail : UInt = 2.U(3.W)
  def SecondaryFull : UInt = 3.U(3.W)
  def ReturnMatch : UInt = 4.U(3.W)
}

class MSHR(val bABits: Int, val tIWidth: Int, val InstrIdBits: Int, val NMshrEntry:Int, val NMshrSubEntry:Int, val AsidBits:Int) extends Module {
  val io = IO(new Bundle {
    val probe = Flipped(ValidIO(new MSHRprobe(bABits,AsidBits)))
    val probeAsid = if(MMU_ENABLED) {Some(Input(UInt(AsidBits.W)))} else None
    val probeOut_st1 = Output(new MSHRprobeOut(NMshrEntry, NMshrSubEntry))
    val missReq = Flipped(Decoupled(new MSHRmissReq(bABits, tIWidth, InstrIdBits, AsidBits)))
    val missCached_st1 = Input(Bool()) // 0-cached 1-no cache
    val UncacheRsp = Output(Bool())//0-cached 1-no cache
    val missReqAsid = if(MMU_ENABLED) {Some(Input(UInt(AsidBits.W)))} else None
    val missRspIn = Flipped(Decoupled(new MSHRmissRspIn(NMshrEntry)))
    val missRspOut = Decoupled(new MSHRmissRspOut(bABits, tIWidth, InstrIdBits,AsidBits))
    val missRspOutAsid = if(MMU_ENABLED) {Some(Output(UInt(AsidBits.W)))} else None
    //For InOrFlu
    val empty = Output(Bool())
    val full  = Output(Bool())
    val usedEntries = Output(UInt((log2Up(NMshrEntry)+1).W))
    val probestatus = Output(Bool())
    val mshrStatus_st0 = Output(UInt(3.W))
    val stage2_ready = Input(Bool())
    val stage1_ready = Input(Bool())
    // stall the core req when releasing the mshr primary entry
    //TODO 真正的需要stall的场景时当release的primary entry和当前coreReq的blockAddr相同
    val releasing_stall = Output(Bool())
    // missRspIn 处理期间的“原子态”指示：同拍 mshrStatus/valid 尚未更新，外部不应插入同块的 secondary miss
    val releasing_valid = Output(Bool())
    val releasing_blockAddr = Output(UInt(bABits.W))
    val releasing_asid = if(MMU_ENABLED) Some(Output(UInt(AsidBits.W))) else None
  })
  // head of entry, for comparison
  val blockAddr_Access = RegInit(VecInit(Seq.fill(NMshrEntry)(0.U(bABits.W))))
  val instrId_Access = RegInit(VecInit(Seq.fill(NMshrEntry)(0.U(InstrIdBits.W)))) //TODO remove this
  val targetInfo_Accesss = RegInit(VecInit(Seq.fill(NMshrEntry)(VecInit(Seq.fill(NMshrSubEntry)(0.U(tIWidth.W))))))
  val cacheStatus_Access = RegInit(VecInit(Seq.fill(NMshrEntry)(false.B)))

  val subentry_valid = RegInit(VecInit(Seq.fill(NMshrEntry)(VecInit(Seq.fill(NMshrSubEntry)(false.B)))))
  val entry_valid = Reverse(Cat(subentry_valid.map(Cat(_).orR)))
  val probestatus = RegInit(false.B)
  val MSHR_st1 = Module(new Queue(new MSHRpipe1Reg(NMshrEntry,log2Up(NMshrSubEntry)+1),1,true,false))
  val releasing_stall = RegInit(VecInit(Seq.fill(NMshrEntry)(false.B)))

  io.releasing_stall := releasing_stall.asUInt.orR
  io.releasing_valid := io.missRspIn.valid
  io.releasing_blockAddr := blockAddr_Access(io.missRspIn.bits.instrId)
  io.empty := !entry_valid.orR
  io.usedEntries := PopCount(entry_valid)
  io.probestatus := MSHR_st1.io.deq.valid//probestatus
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
  val entryMatchProbeid_reg = Wire(UInt(NMshrEntry.W))
  val probeMatchMissReq = Wire(Bool())
  val allfalse_subentryvalidtype = Wire(Vec(NMshrSubEntry,Bool()))
  val entryMatchProbe_st1_raw = MSHR_st1.io.deq.bits.entryMatchProbe // st0 probe 结果
  // st1 可能因 stall 跨过 missRsp 释放窗口：需要用当前 entry_valid 过滤掉已释放 entry 的陈旧 one-hot
  val entryMatchProbe_st1 = entryMatchProbe_st1_raw & entry_valid

  val subentrySelectedForReq = Mux(entryMatchProbe_st1===0.U,allfalse_subentryvalidtype, subentry_valid(OHToUInt(entryMatchProbe_st1)))
  val subentryStatus = Module(new getEntryStatusReq(NMshrSubEntry)) // Output: alm_full, full, next
  subentryStatus.io.valid_list := Reverse(Cat(subentrySelectedForReq))
  val subEntryIdx_st1 = subentryStatus.io.next//MSHR_st1.io.deq.bits.subEntryIdx
  for (i<-0 until NMshrSubEntry){
    allfalse_subentryvalidtype(i) := false.B
  }
  //  ******     missRsp status      ******
  val subentryStatusForRsp = Module(new getEntryStatusRsp(NMshrSubEntry))
  val missRspprobeReqSameBlock = Wire(Bool())

  //  ******     missReq decide MSHR is full or not     ******
  val entryStatus = Module(new getEntryStatusReq(NMshrEntry))
  entryStatus.io.valid_list := entry_valid

  // ******     enum vec_mshr_status     ******
  val mshrStatus_st1_r = RegInit(0.U(3.W))
  val mshrStatus_st1_w = Wire(UInt(3.W))
  val mshrStatus_st0 = Wire(UInt(3.W))
  io.mshrStatus_st0 := mshrStatus_st0
  // missRspIn 优先：同拍有 missRspIn 时禁止 missReq 参与分配/探测，避免 ready 语义被 missReq.valid 阻塞
  val missRspHasPriority = io.missRspIn.valid
  val missReqValid = io.missReq.valid && !missRspHasPriority
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
  if(MMU_ENABLED){
    val ASID_Access = RegInit(VecInit(Seq.fill(NMshrEntry)(0.U(AsidBits.W))))
    val missRspASID_st0 = ASID_Access(entryMatchMissRsp)
    io.releasing_asid.get := ASID_Access(io.missRspIn.bits.instrId)
    probeMatchMissReq := (io.probe.bits.blockAddr === io.missReq.bits.blockAddr) && (io.probeAsid.get === io.missReqAsid.get) && io.probe.valid && missReqValid
    entryMatchProbe := Mux(probeMatchMissReq,UIntToOH(entryStatus.io.next),
    Reverse(Cat(blockAddr_Access.map(_ === io.probe.bits.blockAddr))) & entry_valid & Reverse(Cat(ASID_Access.map(_ === io.probeAsid.get))))
        entryMatchProbeid_reg := OHToUInt(Reverse(Cat(blockAddr_Access.map(_ === io.missReq.bits.blockAddr))) & entry_valid & Reverse(Cat(ASID_Access.map(_ === io.missReqAsid.get))))
    when(io.missReq.fire && MSHR_st1.io.deq.ready && mshrStatus_st1_w === 0.U) { //PRIMARY_AVAIL
      blockAddr_Access(entryStatus.io.next) := io.missReq.bits.blockAddr
      instrId_Access(entryStatus.io.next) := io.missReq.bits.instrId
      ASID_Access(entryStatus.io.next) := io.missReqAsid.get
    }
    val missRspOutAsid_st1 = Module(new Queue(UInt(AsidBits.W),1,true,false))
    missRspOutAsid_st1.io.enq.bits := missRspASID_st0
    missRspOutAsid_st1.io.enq.valid := io.missRspIn.valid && !(subentryStatusForRsp.io.used===0.U)
    missRspOutAsid_st1.io.deq.ready := io.missRspOut.ready
    io.missRspOutAsid.foreach(_ := missRspOutAsid_st1.io.deq.bits)
    missRspprobeReqSameBlock := (io.probe.bits.blockAddr === io.missRspOut.bits.blockAddr) && (io.probeAsid.get === io.missRspOutAsid.get)
  } else {
    entryMatchProbe :=  Mux(probeMatchMissReq,UIntToOH(entryStatus.io.next),
      Reverse(Cat(blockAddr_Access.map(_ === io.probe.bits.blockAddr))) & entry_valid)
    probeMatchMissReq := (io.probe.bits.blockAddr === io.missReq.bits.blockAddr) && io.probe.valid && missReqValid
    when(io.missReq.fire && MSHR_st1.io.deq.ready && mshrStatus_st1_w === 0.U) { //PRIMARY_AVAIL
      blockAddr_Access(entryStatus.io.next) := io.missReq.bits.blockAddr
      instrId_Access(entryStatus.io.next) := io.missReq.bits.instrId
    }
    entryMatchProbeid_reg := OHToUInt(Reverse(Cat(blockAddr_Access.map(_ === io.missReq.bits.blockAddr))) & entry_valid)
    missRspprobeReqSameBlock := (io.probe.bits.blockAddr === io.missRspOut.bits.blockAddr)
  }

  assert(PopCount(entryMatchProbe) <= 1.U)
//RegEnable(OHToUInt(entryMatchProbe),io.missReq.fire)
  val secondaryMiss = entryMatchProbe_st1.orR
  val secondaryMiss_st0 = entryMatchProbe.orR
  val primaryMiss_st0 = !secondaryMiss_st0
  val primaryMiss = !secondaryMiss
  val mainEntryFull = entryStatus.io.full
  val mainEntryAlmFull = entryStatus.io.alm_full
  val subEntryFull = subentryStatus.io.full
  val subEntryAlmFull = subentryStatus.io.alm_full
  //MSHR pipe reg st1, input
  MSHR_st1.io.enq.valid := io.probe.valid
  MSHR_st1.io.enq.bits.entryMatchProbe := entryMatchProbe
  MSHR_st1.io.enq.bits.subEntryIdx := subentryStatus.io.next // todo delect this
  MSHR_st1.io.enq.bits.full  := mainEntryFull && subEntryFull
  MSHR_st1.io.deq.ready := io.stage1_ready

  when(io.missReq.fire && !io.probe.valid && io.stage2_ready) {
    when(primaryMiss && mainEntryAlmFull) {
      mshrStatus_st1_r := 1.U //PRIMARY_FULL
    }.elsewhen(secondaryMiss && subEntryAlmFull) {
      mshrStatus_st1_r := 3.U //SECONDARY_FULL
    }
  }.elsewhen(io.probe.valid) {
    when(primaryMiss_st0) {
      when(mainEntryFull || (mainEntryAlmFull && io.missReq.fire)) {
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
  }.elsewhen(io.missRspIn.valid) {
    //assert(!(mshrStatus_st1_r === 4.U),"mshr set SECONDARY_FULL_RETURN incorrectly")
    when(mshrStatus_st1_r === 1.U || mshrStatus_st1_r === 2.U) {
      mshrStatus_st1_r := 0.U //PRIMARY_AVAIL
    }.elsewhen(mshrStatus_st1_r === 3.U && subentryStatusForRsp.io.used === 1.U) {
      mshrStatus_st1_r := 4.U //SECONDARY_FULL_RETURN
    }.elsewhen(mshrStatus_st1_r === 4.U && subentryStatusForRsp.io.used === 0.U) {
      mshrStatus_st1_r := 0.U //SECONDARY_AVAIL
    }
  }

  when(primaryMiss_st0) {
    when(mainEntryFull || missReqValid && mainEntryAlmFull) {
      mshrStatus_st0 := 1.U //PRIMARY_FULL
      //}.elsewhen(mainEntryAlmFull) {
      //  mshrStatus_st1 := 5.U //PRIMARY_ALM_FULL
    }.otherwise {
      mshrStatus_st0 := 0.U //PRIMARY_AVAIL
    }
  }.otherwise {
    when(subEntryFull || missReqValid && subEntryAlmFull) {
      mshrStatus_st0 := 3.U //SECONDARY_FULL
      //}.elsewhen(subEntryAlmFull) {
      //  mshrStatus_st1 := 7.U //SECONDARY_ALM_FULL
    }.elsewhen(missRspprobeReqSameBlock && io.missRspOut.valid){
      mshrStatus_st0 := 4.U // miss rsp return
    }.otherwise {
      mshrStatus_st0 := 2.U //SECONDARY_AVAIL
    }
  }


  // mshrStatus_st0 := mshrStatus_st1_w
  val subentryFull_sel = subentryStatus.io.full
  val subentryAvail_sel = !subentryStatus.io.full
  //mshrStatus依赖primaryMiss和SecondaryMiss，它们依赖entryValid。
  //mshrStatus必须是寄存器，需要在probe valid的下个周期正确显示。entryValid更新的下一个周期已经来不及。
  //所以用组合逻辑加工一次mshrStatus。
  when(mainEntryFull){
    mshrStatus_st1_w := MSHRStatus.PrimaryFull
  }.elsewhen(subentryFull_sel && secondaryMiss){
    mshrStatus_st1_w := MSHRStatus.SecondaryFull
  }.elsewhen(subentryAvail_sel && secondaryMiss){
    mshrStatus_st1_w := MSHRStatus.SecondaryAvail
  }.otherwise{
    mshrStatus_st1_w := MSHRStatus.PrimaryAvail
  }
  io.probeOut_st1.probeStatus := mshrStatus_st1_w
  when(io.probe.fire && !probestatus) {
    probestatus := true.B
  }.elsewhen(probestatus) {
    when(io.probe.bits.blockAddr =/= io.missReq.bits.blockAddr && io.missReq.fire) {
      probestatus := io.probe.fire
    }.elsewhen(io.missReq.fire) {
      probestatus := false.B
    }
  }
  io.full := entryStatus.io.full
  //  ******     mshr::allocate_vec_sub/allocate_vec_main     ******
  /*0:PRIMARY_AVAIL 1:PRIMARY_FULL 2:SECONDARY_AVAIL 3:SECONDARY_FULL*/
  io.missReq.ready := !(mshrStatus_st1_w === 1.U || mshrStatus_st1_w === 3.U ) && !missRspHasPriority
  assert(!io.missReq.fire || (io.missReq.fire && !io.missRspIn.fire), "MSHR cant have Req & Rsp valid in same cycle, later the prior")
  val real_SRAMAddrUp = Mux(secondaryMiss, OHToUInt(entryMatchProbe_st1), entryStatus.io.next)
  val real_SRAMAddrDown = Mux(secondaryMiss,subEntryIdx_st1, 0.U)
  when(io.missReq.fire && MSHR_st1.io.deq.ready) {
    targetInfo_Accesss(real_SRAMAddrUp)(real_SRAMAddrDown) := io.missReq.bits.targetInfo
    cacheStatus_Access(real_SRAMAddrUp) := io.missCached_st1
  }

 /* when(io.missReq.fire && MSHR_st1.io.deq.ready && mshrStatus_st1_w === 0.U) { //PRIMARY_AVAIL
    blockAddr_Access(entryStatus.io.next) := io.missReq.bits.blockAddr
    instrId_Access(entryStatus.io.next) := io.missReq.bits.instrId
    ASID_Access(entryStatus.io.next) := io.missReq.bits.ASID
  }*/

  io.probeOut_st1.a_source := Mux(missReqValid,real_SRAMAddrUp,entryMatchProbeid_reg)

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
  //val missRspASID_st0 = ASID_Access(entryMatchMissRsp)
  val missRspOut_st1 = Module(new Queue(new MSHRmissRspOut(bABits, tIWidth, InstrIdBits, AsidBits),1,true,false))
  missRspOut_st1.io.enq.valid := io.missRspIn.valid && !(subentryStatusForRsp.io.used===0.U)
  missRspOut_st1.io.enq.bits.targetInfo := missRspTargetInfo_st0
  missRspOut_st1.io.enq.bits.blockAddr := missRspBlockAddr_st0
  missRspOut_st1.io.enq.bits.instrId := io.missRspIn.bits.instrId
  missRspOut_st1.io.enq.bits.UncacheRsp := cacheStatus_Access(entryMatchMissRsp)

  //missRspOut_st1.io.enq.bits.ASID := missRspASID_st0

  io.UncacheRsp := missRspOut_st1.io.deq.bits.UncacheRsp

  io.missRspOut.bits.targetInfo := RegNext(missRspTargetInfo_st0)
  io.missRspOut.bits.blockAddr := RegNext(missRspBlockAddr_st0)
  io.missRspOut.bits.instrId := io.missRspIn.bits.instrId
  io.missRspOut.valid := RegNext(io.missRspIn.valid ) && !(RegNext(subentryStatusForRsp.io.used)===0.U)
  io.missRspOut <> missRspOut_st1.io.deq
  //io.missRspOut := RegNext(io.missRspIn.valid) &&
  //  subentryStatusForRsp.io.used >= 1.U//如果上述Access中改出SRAM，本信号需要延迟一个周期

  //  ******     maintain subentries    ******
  /*0:PRIMARY_AVAIL 1:PRIMARY_FULL 2:SECONDARY_AVAIL 3:SECONDARY_FULL*/
  for (iofEn <- 0 until NMshrEntry) {
    for (iofSubEn <- 0 until NMshrSubEntry) {
      when(iofEn.asUInt === entryStatus.io.next &&
        iofSubEn.asUInt === 0.U && io.missReq.fire  && MSHR_st1.io.deq.fire && primaryMiss) {
        subentry_valid(iofEn)(iofSubEn) := true.B
      }.elsewhen(iofEn.asUInt === entryMatchMissRsp && iofSubEn.asUInt === subentry_next2cancel &&
        io.missRspIn.valid && missRspOut_st1.io.enq.ready) {
        subentry_valid(iofEn)(iofSubEn) := false.B
      }
    }.elsewhen(iofSubEn.asUInt === subEntryIdx_st1 &&
      io.missReq.fire && secondaryMiss && MSHR_st1.io.deq.fire && iofEn.asUInt === entryMatchProbeid_reg) {
      subentry_valid(iofEn)(iofSubEn) := true.B
    } //order of when & elsewhen matters, as elsewhen cover some cases of when, but no op to them
  }

  for (iofEn <- 0 until NMshrEntry) {
    when(iofEn.asUInt === entryMatchMissRsp && io.missRspIn.fire && releasing_stall(iofEn)) {
      releasing_stall(iofEn) := false.B
    }.elsewhen(iofEn.asUInt === entryMatchMissRsp && io.missRspIn.valid && !releasing_stall(iofEn)) {
      releasing_stall(iofEn) := true.B
    }
  }

  // Debug counter: peak MSHR (read-miss tracking) occupancy. Surfaced via
  // dontTouch so verilator emits it to the FST trace. Used together with
  // wshrMaxUsed (DCacheWSHR.scala) to validate the
  // dcache_MshrEntry / dcache_wshr_entry budgets after the bfs4096-003
  // livelock fix (see top/parameters.scala comments).
  val mshrMaxUsed = RegInit(0.U(log2Ceil(NMshrEntry + 1).W))
  when(io.usedEntries > mshrMaxUsed){ mshrMaxUsed := io.usedEntries }
  dontTouch(mshrMaxUsed)
}
class SpecialMSHR(val bABits: Int, val tIWidth: Int, val InstrIdBits: Int, val NMshrEntry:Int, val AsidBits:Int) extends Module {
  val io = IO(new Bundle {
    val missReq = Flipped(Decoupled(new SMSHRmissReq(bABits, tIWidth, InstrIdBits, AsidBits)))
    val missReqAsid = if(MMU_ENABLED) {Some(Input(UInt(AsidBits.W)))} else None
    val missRspIn = Flipped(Decoupled(new MSHRmissRspIn(NMshrEntry)))
    val missRspOut = Decoupled(new MSHRmissRspOut(bABits, tIWidth, InstrIdBits,AsidBits))
    val missRspOutAsid = if(MMU_ENABLED) {Some(Output(UInt(AsidBits.W)))} else None
    val empty = Output(Bool())
    val probeOut_st1 = Output(new SMSHRprobeOut(NMshrEntry))
    val full = Output(Bool())
    val stage2_ready = Input(Bool())
    val stage1_ready = Input(Bool())
  })
  val blockAddr_Access = RegInit(VecInit(Seq.fill(NMshrEntry)(0.U(bABits.W))))
  val entry_valid = RegInit(VecInit(Seq.fill(NMshrEntry)(false.B)))
  val entry_valid_uint = Reverse(Cat(entry_valid))
  val targetInfo_access = RegInit(VecInit(Seq.fill(NMshrEntry)(0.U(tIWidth.W))))
  val type_Access = RegInit(VecInit(Seq.fill(NMshrEntry)(0.U(2.W)))) // 0-lr 1- sc 2-amo
  val wordOffset = RegInit(VecInit(Seq.fill(NMshrEntry)(0.U(dcache_BlockOffsetBits.W))))
  val entryMatchProbe = Wire(UInt(NMshrEntry.W))
  val entryMatchProbeBlock = Wire(UInt(NMshrEntry.W))
  val entryStatus = Module(new getEntryStatusReq(NMshrEntry))
  val probeMatchRsp = Wire(Bool())
  entryStatus.io.valid_list := entry_valid_uint
  val ptr = entryStatus.io.next
  io.missReq.ready := io.stage1_ready
  if(MMU_ENABLED){
    val ASID_Access = RegInit(VecInit(Seq.fill(NMshrEntry)(0.U(AsidBits.W))))
    entryMatchProbe := Reverse(Cat(blockAddr_Access.map(_ === io.missReq.bits.blockAddr))) & entry_valid.asUInt & Reverse(Cat(ASID_Access.map(_ === io.missReqAsid.get)))& Reverse(Cat(wordOffset.map(_ === io.missReq.bits.wordOffset)))
    entryMatchProbeBlock := Reverse(Cat(blockAddr_Access.map(_ === io.missReq.bits.blockAddr))) & entry_valid.asUInt & Reverse(Cat(ASID_Access.map(_ === io.missReqAsid.get)))
    when(io.missReq.fire){
      ASID_Access(ptr) := io.missReqAsid.get
    }
  }
  else{
    entryMatchProbe := Reverse(Cat(blockAddr_Access.map(_ === io.missReq.bits.blockAddr))) & entry_valid.asUInt & Reverse(Cat(wordOffset.map(_ === io.missReq.bits.wordOffset)))
    entryMatchProbeBlock := Reverse(Cat(blockAddr_Access.map(_ === io.missReq.bits.blockAddr))) & entry_valid.asUInt
  }
  when(io.missReq.fire){
    blockAddr_Access(ptr) := io.missReq.bits.blockAddr
    targetInfo_access(ptr) := io.missReq.bits.targetInfo
    type_Access(ptr) := io.missReq.bits.Type
    wordOffset(ptr) := io.missReq.bits.wordOffset
  }
  val entryMatchMissRsp = io.missRspIn.bits.instrId
  val missRspTargetInfo_st0 = targetInfo_access(entryMatchMissRsp)
  val missRspBlockAddr_st0 = blockAddr_Access(entryMatchMissRsp)
  //val missRspASID_st0 = ASID_Access(entryMatchMissRsp)
  val missRspOut_st1 = Module(new Queue(new MSHRmissRspOut(bABits, tIWidth, InstrIdBits, AsidBits),1,true,false))
  io.missRspIn.ready := missRspOut_st1.io.enq.ready && !io.missReq.valid
  missRspOut_st1.io.enq.valid := io.missRspIn.valid && !io.missReq.valid
  missRspOut_st1.io.enq.bits.targetInfo := missRspTargetInfo_st0
  missRspOut_st1.io.enq.bits.blockAddr := missRspBlockAddr_st0
  missRspOut_st1.io.enq.bits.instrId := io.missRspIn.bits.instrId
  missRspOut_st1.io.enq.bits.UncacheRsp := true.B
  val conditionVec = Wire(Vec(NMshrEntry, Bool()))

  for (i <- 0 until NMshrEntry) {
    conditionVec(i) := entry_valid(i) && (type_Access(i) === 0.U) && !(i.asUInt === entryMatchMissRsp && io.missRspIn.valid)
  }
  probeMatchRsp := (OHToUInt(entryMatchProbeBlock ) === entryMatchMissRsp) && io.missRspIn.valid
  io.probeOut_st1.hitblock := entryMatchProbeBlock.orR && !probeMatchRsp
  io.probeOut_st1.hitblockIdx := OHToUInt(entryMatchProbeBlock)
  io.probeOut_st1.LRexist := conditionVec.reduce(_||_)
  io.probeOut_st1.a_source := ptr
  io.empty := !entry_valid.reduce(_||_)
  io.full := entry_valid.reduce(_&&_)
  io.missRspOut <> missRspOut_st1.io.deq
   for (iofEn <- 0 until NMshrEntry) {
      when(iofEn.asUInt === entryStatus.io.next  && io.missReq.fire && io.missReq.bits.Type =/= 0.U) {
        entry_valid(iofEn) := true.B
      }.elsewhen(iofEn.asUInt === entryMatchMissRsp  &&
        io.missRspIn.valid && missRspOut_st1.io.enq.ready) {
        entry_valid(iofEn) := false.B
      }
    }//order of when & elsewhen matters, as elsewhen cover some cases of when, but no op to them

  // Debug counter: peak MSHR occupancy seen during the run. Surfaced via
  // dontTouch so verilator emits it to the FST trace. Used together with
  // wshrMaxUsed (DCacheWSHR.scala) to validate the
  // dcache_MshrEntry / dcache_wshr_entry budgets after the bfs4096-003
  // livelock fix (see top/parameters.scala comments).
  val mshrUsedCnt = PopCount(entry_valid)
  val mshrMaxUsed = RegInit(0.U(log2Ceil(NMshrEntry + 1).W))
  when(mshrUsedCnt > mshrMaxUsed){ mshrMaxUsed := mshrUsedCnt }
  dontTouch(mshrMaxUsed)
}


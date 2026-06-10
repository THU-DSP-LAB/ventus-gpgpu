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

import L1Cache.DCache.HasDCacheParameter
import L1Cache._
import SRAMTemplate.{SRAMBundleA, SRAMBundleAW, SRAMTemplate}
import chisel3._
import chisel3.util._
import top.parameters._

//abstract class MSHRBundle extends Bundle with L1CacheParameters
/*
class MSHRprobe(val bABits: Int) extends Bundle {
  val blockAddr = UInt(bABits.W)
}

class MSHRprobeOut(val NSet:Int, val NWay:Int) extends Bundle {
  val probeStatus = UInt(3.W)
  val a_source = UInt((log2Ceil(NSet)+log2Ceil(NWay)).W)
}
class MSHRmissReq(val bABits: Int, val tIWdith: Int, val WIdBits: Int) extends Bundle {// Use this bundle when handle miss issued from pipeline
  val blockAddr = UInt(bABits.W)
  val instrId = UInt(WIdBits.W)
  val targetInfo = UInt(tIWdith.W)
}
class MSHRmissRspIn(val NSet:Int, val NWay:Int) extends Bundle {//Use this bundle when a block return from Lower cache
  val instrId = UInt((log2Ceil(NSet)+log2Ceil(NWay)).W)
}
class MSHRmissRspOut[T <: Data](val bABits: Int, val tIWdith: Int, val WIdBits: Int) extends Bundle {
  val targetInfo = UInt(tIWdith.W)
  val instrId = UInt(WIdBits.W)
  val blockAddr = UInt(bABits.W)
  //val burst = Bool()//This bit indicate the Rsp transaction comes from subentry
  //val last = Bool()
}
class MSHRID(val NSet:Int, val NWay:Int,val NSub: Int) extends Bundle{
  val setId = UInt((log2Ceil(NSet)).W)
  val wayMask = UInt(NWay.W)
  val subId = UInt((log2Ceil(NSub)).W)
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
}



class MSHRmissReqv2(val bABits: Int, val tIWdith: Int, val WIdBits: Int) extends Bundle {// Use this bundle when handle miss issued from pipeline
  val blockAddr = UInt(bABits.W)
  val instrId   = UInt(WIdBits.W)
  val targetInfo = UInt(tIWdith.W)
  val missUncached = Bool() // 0-cached 1-uncached
}

object MSHRStatus{
  def PrimaryAvail : UInt = 0.U(3.W)
  def PrimaryFull : UInt = 1.U(3.W)
  def SecondaryAvail : UInt = 2.U(3.W)
  def SecondaryFull : UInt = 3.U(3.W)
  def ReturnMatch : UInt = 4.U(3.W)
}

class MSHRv2(val bABits: Int, val MshrSet: Int,val MshrSetBits: Int, val MshrTagBits:Int, val tIWidth: Int, val WIdBits: Int, val MshrWay:Int, val MshrWayBits: Int, val NMshrSubEntry:Int) extends Module {
  val io = IO(new Bundle {
    val probe = Flipped(Decoupled(new MSHRprobe(bABits)))
    val probeOut_st1 = Output(new MSHRprobeOut(MshrWay, NMshrSubEntry))
    val missReq = Flipped(Decoupled(new MSHRmissReqv2(bABits, tIWidth, WIdBits)))
    val UncacheRsp = Output(Bool())//0-cached 1-no cache
    val missRspIn = Flipped(Decoupled(new MSHRmissRspIn(MshrSet,MshrWay)))
    val missRspOut = Decoupled(new MSHRmissRspOut(bABits, tIWidth, WIdBits))
    //For InOrFlu
    val empty = Output(Bool())

  })
  // use SRAM for MSHR
  // way associate

  // head of entry, for comparison
  //val blockAddr_Access = RegInit(VecInit(Seq.fill(NMshrEntry)(0.U(bABits.W))))
 // val targetInfo_Accesss = RegInit(VecInit(Seq.fill(NMshrEntry)(VecInit(Seq.fill(NMshrSubEntry)(0.U(tIWidth.W))))))

  //SRAM to store blockAddr
  val BlockAddrAccess = Module(new SRAMTemplate(
    UInt(MshrTagBits.W),
    set=MshrSet,
    way=MshrWay,
    shouldReset = false,
    holdRead = true,
    singlePort = false,
    bypassWrite = false
  ))
  val cacheStatus_Access = RegInit(VecInit(Seq.fill(MshrSet)(VecInit(Seq.fill(MshrWay)(false.B)))))
  val subentry_valid = RegInit(VecInit(Seq.fill(MshrSet*MshrWay)(VecInit(Seq.fill(NMshrSubEntry)(false.B)))))
  val entry_valid_vec = Reverse(Cat(subentry_valid.map(Cat(_).orR)))//in form of vec
  val entry_valid = WireInit(VecInit(Seq.fill(MshrSet)(VecInit(Seq.fill(MshrWay)(false.B))))) // array set * wat
  val BlockAddrRRsp = WireInit(VecInit(Seq.fill(MshrSet)(VecInit(Seq.fill(MshrWay)(0.U(MshrTagBits.W))))))

  val missRspIn_setIdx = Wire(UInt(MshrSetBits.W))
  val missRspIn_wayIdx = Wire(UInt(MshrWayBits.W))
  val missRspIn_wayMask = Wire(Vec(MshrWay,Bool()))

  io.empty := !entry_valid_vec.orR

  // entry_valid_form
  //      |  way0  | way1 | way2 | way3
  // set0 |        |      |      |
  // set1 |        |      |      |

  //subentry_valid form
  //      | sub0 | sub1
  // s0w0 |      |
  // s0w1 |      |
  // s0w2 |      |
  // ...
  // subentry_valid idx = (setIdx, wayIdx)
  for(i<-0 until MshrSet){
    for(j<-0 until MshrWay){
      entry_valid(i)(j) := entry_valid_vec(i*MshrWay+j)
    }
  }

  // cut probeRead bAbits into set and tag
  val bAset_st0 = if (MshrSetBits != 0) {io.probe.bits.bABits.asUInt(MshrSetBits -1,0)} else 0.U
  val bAtag_st0 = io.probe.bits.bABits.asUInt(bABits-1,MshrSetBits)
  val bAtag_st1 = io.missReq.bits.bABits.asUInt(bABits-1,MshrSetBits)
  val bAset_st1 = if (MshrSetBits != 0) {io.missReq.bits.bABits.asUInt(MshrSetBits -1,0)} else 0.U
  val itagChecker = Module(new tagChecker(MshrWay,MshrTagBits))
  // hit status in BlockAccess, aka hit in main entry
  val hitWayMask = Wire(Vec(MshrWay,Bool()))
  val hitMainEntry = Wire(Bool()) // indicate hit in blockAccess, req is secondary miss, but may not be handle due to secondary full
  val bAAccessRArb = Module(new Arbiter (new SRAMBundleA(MshrSet),2))
  // BlockAccess interface
  BlockAddrAccess.io.r.req <> bAAccessRArb.io.out//io.probe.valid
  //arbiter interface
  io.probe.ready := bAAccessRArb.io.in(1).ready
  bAAccessRArb.io.in(0).valid := io.missRspIn.fire
  bAAccessRArb.io.in(0).bits := missRspIn_setIdx
  bAAccessRArb.io.in(1).valid := io.probe.valid
  bAAccessRArb.io.in(1).bits := bAset_st0
 // BlockAddrAccess.io.r.req.bits.setIdx := //bAset_st0
  // hit in MSHR tag check, stage1
  BlockAddrRRsp := BlockAddrAccess.io.r.resp.data
  itagChecker.io.tag_of_set := BlockAddrRRsp
  itagChecker.io.tag_from_pipe := bAtag_st1
  itagChecker.io.way_valid := entry_valid(bAset_st1)
  hitWayMask := itagChecker.io.waymask
  hitMainEntry := itagChecker.io.cache_hit
  val hitWayIdx_st1 = OHToUInt(hitWayMask)
  val NextWay_Idx = Wire(UInt(log2Ceil(MshrWay).W)) // allocate next way

  // subentry status CAM, stage 1
  val subEntryValid_Idx_missReq = Cat(bAset_st1, hitWayIdx_st1)
  val subEntry_missReq_sel = Wire(Vec(NMshrSubEntry,Bool()))
  subEntry_missReq_sel := subentry_valid(subEntryValid_Idx_missReq) //select subentry status

  /*PRIMARY_AVAIL         000
  * PRIMARY_FULL          001
  * SECONDARY_AVAIL       010
  * SECONDARY_FULL        011
  * SECONDARY_FULL_RETURN 100
  * PRIMARY_ALM_FULL      101
  * SECONDARY_ALM_FULL    111
  * ========== st1 probe  =================
   */
  // ******      mshr::probe_vec    ******
  //  ******     missReq decide selected subentries are full or not     ******
  // subentryStatus.io.valid_list := Reverse(Cat(subentrySelectedForReq))

  //  ******     missRsp status      ******
  val subentryStatusForRsp = Module(new getEntryStatusRsp(NMshrSubEntry))

  val entryStatus = Module(new getEntryStatusReq(MshrWay))
  entryStatus.io.valid_list := entry_valid(bAset_st1) // choose target set, entry_valid(bAset_st1) will be in form of   set(idx) |way0|way1|way2|way3

  // ******     enum vec_mshr_status     ******
  val mshrStatus_st1_w = Wire(UInt(3.W))
  val mshrStatus_st0 = Wire(UInt(3.W))
  val allfalse_subentryvalidtype = Wire(Vec(NMshrSubEntry,Bool()))
  for (i<-0 until NMshrSubEntry){
    allfalse_subentryvalidtype(i) := false.B
  }
  val subentryStatus = Module(new getEntryStatusReq(NMshrSubEntry)) // Output: alm_full, full, next
  assert(PopCount(hitWayMask) <= 1.U)
  val entryMatchProbeid = subEntryValid_Idx_missReq//Cat(bAset_st1, OHToUInt(hitWayMask))
  val secondaryMiss = hitMainEntry
  val primaryMiss = !secondaryMiss
  val mainEntryFull = entryStatus.io.full
  subentryStatus.io.valid_list := subEntry_missReq_sel // vec(numsubentry, bool)
  val subentryFull_sel = subentryStatus.io.full
  val subentryAvail_sel = !subentryStatus.io.full
  val RspReqMatch = (io.missRspIn.bits.instrId === subEntryValid_Idx_missReq) && io.missReq.valid && io.missRspIn.valid && hitMainEntry
  // missReq hit MissRspIn, will hold pipe to wait

  when(mainEntryFull){
    mshrStatus_st1_w := MSHRStatus.PrimaryFull
  }.elsewhen(subentryFull_sel){
    mshrStatus_st1_w := MSHRStatus.SecondaryFull
  }.elsewhen(RspReqMatch){
    mshrStatus_st1_w := MSHRStatus.ReturnMatch
  }.elsewhen(subentryAvail_sel){
    mshrStatus_st1_w := MSHRStatus.SecondaryAvail
  }.otherwise{
    mshrStatus_st1_w := MSHRStatus.PrimaryAvail
  }

  // mshrStatus_st0 := mshrStatus_st1_w
  val subEntryIdx_st1 = subentryStatus.io.next
  val EntryWayIdx_st1 = entryStatus.io.next
  //mshrStatus依赖primaryMiss和SecondaryMiss，它们依赖entryValid。
  //mshrStatus必须是寄存器，需要在probe valid的下个周期正确显示。entryValid更新的下一个周期已经来不及。
  //所以用组合逻辑加工一次mshrStatus。
  io.probeOut_st1.probeStatus := mshrStatus_st1_w

  //  ******     mshr::allocate_vec_sub/allocate_vec_main     ******
  /*0:PRIMARY_AVAIL 1:PRIMARY_FULL 2:SECONDARY_AVAIL 3:SECONDARY_FULL*/
  io.missReq.ready := !(mshrStatus_st1_w === MSHRStatus.PrimaryFull || mshrStatus_st1_w === MSHRStatus.SecondaryFull || mshrStatus_st1_w === MSHRStatus.ReturnMatch )
  assert(!io.missReq.fire || (io.missReq.fire && !io.missRspIn.fire), "MSHR cant have Req & Rsp valid in same cycle, later the prior")

  // =======write bA and tA ========
  // req in st1, complete in st2
  val real_SRAMSetIdx = bAset_st1
  val real_SRAMWayIdx = Mux(secondaryMiss, hitWayIdx_st1, NextWay_Idx)
  val real_SRAMSubIdx = Mux(secondaryMiss, subEntryIdx_st1, 0.U)
  val real_SRAMSubIdxOH = UIntToOH(real_SRAMSubIdx).asTypeOf(Vec(NMshrSubEntry,Bool()))
  val entryMatchMissRsp = io.missRspIn.bits.instrId
  // write tA req
  val targetInfoSRAMWReq: Vec[SRAMBundleAW[UInt]] = Wire(Vec(NMshrSubEntry, new SRAMBundleAW(UInt(tIWidth.W), MshrSet, MshrWay)))
  val targetInfoSRAMWReq_valid = Wire(Vec(NMshrSubEntry,Bool()))
  val targetInfoSRAMRReq = Wire(Vec(NMshrSubEntry,new SRAMBundleA(MshrSet)))
  val targetInfoSRAMRReq_valid = Wire(Vec(NMshrSubEntry,Bool()))
  targetInfoSRAMWReq.foreach(_.setIdx := real_SRAMSetIdx)
  targetInfoSRAMWReq.foreach(_.waymask.get := UIntToOH(real_SRAMWayIdx))
  targetInfoSRAMWReq.foreach(_.data := io.missReq.bits.targetInfo)
  targetInfoSRAMWReq_valid := real_SRAMSubIdxOH.map(_ && io.missReq.fire) // todo define valid or fire
  //ta write and read port
  val targetInfoAccessRRsp: Seq[UInt] = (0 until NMshrSubEntry).map { i =>
    val targetInfoAccess = Module(new SRAMTemplate(
      gen=UInt(tIWidth.W),
      set=MshrSet,
      way=MshrWay,
      shouldReset = false,
      holdRead = false,
      singlePort = false
    ))
    // write port
    targetInfoAccess.io.w.req.valid := targetInfoSRAMWReq_valid(i)
    targetInfoAccess.io.w.req.bits := targetInfoSRAMWReq(i)
    // read port
    targetInfoAccess.io.r.req.bits := targetInfoSRAMRReq(i)
    targetInfoAccess.io.r.req.valid := targetInfoSRAMRReq_valid(i)
    Cat(targetInfoAccess.io.r.resp.data.reverse)
  }
  BlockAddrAccess.io.w.req.valid := io.missReq.fire && mshrStatus_st1_w === 0.U
  BlockAddrAccess.io.w.req.bits.setIdx := real_SRAMSetIdx
  BlockAddrAccess.io.w.req.bits.waymask.get := UIntToOH(real_SRAMWayIdx)
  BlockAddrAccess.io.w.req.bits.data := bAtag_st1

  when(io.missReq.fire){
    cacheStatus_Access(real_SRAMSetIdx)(real_SRAMWayIdx) := io.missReq.bits.missUncached
  }

  io.probeOut_st1.a_source := entryMatchProbeid//Mux(io.missReq.valid,real_SRAMAddrUp,entryMatchProbeid)

  //  ******      mshr::vec_arrange_core_rsp    ******

  subentryStatusForRsp.io.valid_list := Reverse(Cat(subentry_valid(entryMatchMissRsp)))
  // priority: missRspIn > missReq
  //assert(!io.missRspIn.fire || (io.missRspIn.fire && subentryStatus.io.used >= 1.U))
  //This version allow missRspIn fire when no subentry are left
  //如果后面发现missRspOut端口这一级不能取消，使用这段注释掉的代码
  //io.missRspIn.ready := !(subentryStatusForRsp.io.used >= 2.U ||
  //  (subentryStatusForRsp.io.used === 1.U && !io.missRspOut.ready))
  io.missRspIn.ready := !(subentryStatusForRsp.io.used >= 1.U)


  //entryMatchMissRsp := Reverse(Cat(instrId_Access.map(_ === io.missRspIn.bits.instrId))) & entry_valid
  //assert(PopCount(entryMatchMissRsp) <= 1.U,"MSHR missRspIn, cant match multiple entries")
  val subentry_next2cancel = Wire(UInt(log2Up(NMshrSubEntry).W))
  subentry_next2cancel := subentryStatusForRsp.io.next2cancel
  val missRspTargetInfo_st1 : Vec[UInt] = VecInit(targetInfoAccessRRsp)

  // pipe reg for missrspin and missrspout
  val missRspInIdQueue = Module(new Queue(new MSHRID(MshrSet,MshrWay,NMshrSubEntry), 1, true, false))
  val missRspBlockAddr_st1 = BlockAddrRRsp
  val targetInfoChoose = UInt(tIWidth.W)
  val missRspBlockAddr_st1_wayMask = Wire(UInt(MshrTagBits.W))
  val missRspOutBlockAddr = Wire(UInt(bABits.W))
  val targetInfoSubSelect = missRspTargetInfo_st1(missRspInIdQueue.io.deq.bits.subId).asTypeOf(Vec(MshrWay,UInt(tIWidth.W)))
  targetInfoSRAMRReq_valid.foreach(_ := io.missRspIn.valid)
  targetInfoSRAMRReq.foreach(_.setIdx := missRspIn_setIdx)
  missRspIn_setIdx := io.missRspIn.bits.instrId >> MshrWayBits
  missRspIn_wayIdx := io.missRspIn.bits.instrId(MshrWayBits-1,0)
  missRspIn_wayMask := UIntToOH(missRspIn_wayIdx).asTypeOf(Vec(MshrWay,Bool()))

  missRspInIdQueue.io.enq.valid := io.missRspIn.valid && !(subentryStatusForRsp.io.used===0.U)
  missRspInIdQueue.io.enq.bits.setId := missRspIn_setIdx
  missRspInIdQueue.io.enq.bits.wayMask := missRspIn_wayMask
  missRspInIdQueue.io.enq.bits.subId := subentry_next2cancel
  missRspInIdQueue.io.deq.ready := io.missRspOut.ready

  missRspBlockAddr_st1_wayMask := Mux1H(missRspInIdQueue.io.deq.bits.wayMask,BlockAddrRRsp)
  targetInfoChoose := Mux1H(missRspInIdQueue.io.deq.bits.wayMask,targetInfoSubSelect)
  missRspOutBlockAddr := Cat(missRspBlockAddr_st1_wayMask, missRspInIdQueue.io.deq.bits.setId)

  io.UncacheRsp := cacheStatus_Access(entryMatchMissRsp)(subentry_next2cancel)

  io.missRspOut.bits.targetInfo := targetInfoChoose
  io.missRspOut.bits.blockAddr := missRspOutBlockAddr//RegNext(missRspBlockAddr_st0)
  io.missRspOut.bits.instrId := io.missRspIn.bits.instrId
  io.missRspOut.valid := RegNext(io.missRspIn.valid ) && !(RegNext(subentryStatusForRsp.io.used)===0.U)
  //io.missRspOut := RegNext(io.missRspIn.valid) &&
  //  subentryStatusForRsp.io.used >= 1.U//如果上述Access中改出SRAM，本信号需要延迟一个周期

  //  ******     maintain subentries    ******
  /*0:PRIMARY_AVAIL 1:PRIMARY_FULL 2:SECONDARY_AVAIL 3:SECONDARY_FULL*/
  for (iofWay <- 0 until MshrWay) {
    for (iofSubEn <- 0 until NMshrSubEntry) {
      when(iofWay.asUInt === entryStatus.io.next &&
        iofSubEn.asUInt === 0.U && io.missReq.fire && primaryMiss) {
        subentry_valid(bAset_st1*MshrWay.asUInt + iofWay.asUInt)(iofSubEn) := true.B
      }.elsewhen(iofWay.asUInt === missRspIn_wayIdx && iofSubEn.asUInt === subentry_next2cancel &&
        io.missRspIn.fire) {
        subentry_valid(missRspIn_setIdx*MshrWay.asUInt + iofWay.asUInt)(iofSubEn) := false.B
      }
    }.elsewhen(iofSubEn.asUInt === subEntryIdx_st1 &&
      io.missReq.fire && secondaryMiss && iofWay.asUInt === hitWayIdx_st1) {
      subentry_valid(bAset_st1*MshrWay.asUInt + iofWay.asUInt)(iofSubEn) := true.B
    } //order of when & elsewhen matters, as elsewhen cover some cases of when, but no op to them
  }
}
*/

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

import SRAMTemplate._
import chisel3._
import chisel3.util._
import freechips.rocketchip.rocket.ASIdBits
import top.parameters._
import config.config.Parameters
class tagCheckerResult(way: Int) extends Bundle{
  val waymask = UInt(way.W)
  val sectorHitWayMask = UInt(way.W)
  val activeSectorMask = UInt(dcache_SectorCount.W)
  val writeByteMask = UInt((dcache_BlockWords * BytesOfWord).W)
  val hit = Bool()
}
class hitStatus(way: Int, tagBits: Int) extends Bundle{
  val waymask = UInt(way.W)
  val tag = UInt(tagBits.W)
  val isDirty = Bool()
  val hit = Bool()
}
//This module contain Tag memory, its valid bits, tag comparator, and Replacement Unit
class L1TagAccess(set: Int, way: Int, tagBits: Int, AsidBits: Int, readOnly: Boolean)(implicit p: Parameters)extends Module{
  val io = IO(new Bundle {
    //From coreReq_pipe0
    val probeRead = Flipped(Decoupled(new SRAMBundleA(set)))//Probe Channel
    val tagFromCore_st1 = Input(UInt(tagBits.W))
    val asidFromCore_st1 = if(MMU_ENABLED) Some(Input(UInt(AsidBits.W))) else None
    val probeIsWrite_st1 = if(!readOnly){Some(Input(Bool()))} else None
    val probeIsUncache_st1 = Input(Bool())
    //val coreReqReady = Input(Bool())//TODO try to replace with probeRead.fire
    //To coreReq_pipe1
    val hit_st1 = Output(Bool())
    val hitStatus_st1 = Output(new hitStatus(way, tagBits))
    val waymaskHit_st1 = Output(UInt(way.W))
    //From memRsp_pipe0
    val allocateWrite = Flipped(ValidIO(new SRAMBundleA(set)))//Allocate Channel
    val allocateWriteData_st1 = Input(UInt(tagBits.W))///todo dont need, unccache req will not give valid allocatewrite.valid
    val allocateWriteAsid_st1 = if(MMU_ENABLED) Some(Input(UInt(AsidBits.W))) else None
    //From memRsp_pipe1
    val allocateWriteTagSRAMWValid_st1 = Input(Bool())
    //To memRsp_pipe1
    val needReplace = if(!readOnly){
      Some(Output(Bool()))
    } else None
    val replaceValidVictim_st1 = if(!readOnly){
      Some(Output(Bool()))
    } else None
    val waymaskReplacement_st1 = Output(UInt(way.W))//one hot, for SRAMTemplate
    val a_addrReplacement_st1 = if (!readOnly) {
      Some(Output(UInt(xLen.W)))
    } else None
    val asidReplacement_st1 = if(MMU_ENABLED) Some{Output(UInt(AsidBits.W))} else None
    //For InvOrFlu
    val hasDirty_st0 = if (!readOnly) {Some(Output(Bool()))} else None
    val dirtySetIdx_st0 = if (!readOnly) {Some(Output(UInt(log2Up(set).W)))} else None
    val dirtyWayMask_st0 = if (!readOnly) {Some(Output(UInt(way.W)))} else None
    val dirtyTag_st1 = if (!readOnly) {Some(Output(UInt(tagBits.W)))} else None
    val dirtyASID_st1 = if(MMU_ENABLED) {Some(Output(UInt(AsidBits.W)))} else None
    //For InvOrFlu and LRSC
    val flushChoosen = if (!readOnly) {Some(Input(Bool()))} else None
    //For Inv
    val invalidateAll = Input(Bool())
    val tagready_st1 = Input(Bool())

    // 本次写入cacheline位置的信息
    val writeHitFire_st1 = Input(Bool())
    val writeSetIdx_st1 = Input(UInt(log2Up(set).W))
    val perLaneAddr_st1 = Input(Vec(num_lane, new DCachePerLaneAddr))
    val writeByteMask_st1 = Input(UInt((dcache_BlockWords * BytesOfWord).W))
    val allocateSectorMask_st1 = Input(UInt(dcache_SectorCount.W))
    val allocateDataSectorMask_st1 = Output(UInt(dcache_SectorCount.W))
    val dirtySectorMask_st1 = Output(UInt(dcache_SectorCount.W))
    val replace_dirty_sector_mask_st1 = Output(UInt(dcache_SectorCount.W))
    val hit_dirty_sector_mask_st1 = Output(UInt(dcache_SectorCount.W))
    val hit_dirty_mask_st1 = Output(UInt((dcache_BlockWords * BytesOfWord).W))
    // 全局无效化时，dirty cacheline的 dirty mask
    val dirtyMask_st1 = Output(UInt((dcache_BlockWords * BytesOfWord).W))
    // 分配写要替换的 cacheline 的 dirty mask
    val replace_dirty_mask_st1 = Output(UInt((dcache_BlockWords * BytesOfWord).W))
    val coreReq_st1_valid = Input(Bool())
  })
  //TagAccess internal parameters
  val Length_Replace_time_SRAM: Int = 10
  assert(!(io.probeRead.fire && io.allocateWrite.fire), s"tag probe and allocate in same cycle")
  val probeReadBuf = Queue(io.probeRead,1,pipe=true)
  probeReadBuf.ready := io.tagready_st1

  //access time counter
  val accessFire = io.probeRead.fire || io.allocateWrite.fire
  val (accessCount,accessCounterFull) = Counter(accessFire,1000)
  def wordOffsetToSectorIdx(wordOffset: UInt): UInt =
    wordOffset(dcache_BlockOffsetBits - 1, log2Ceil(dcache_SectorWords))

  def activeSectorMask(perLaneAddr: Vec[DCachePerLaneAddr]): UInt = {
    perLaneAddr
      .map(lane => Mux(lane.activeMask, UIntToOH(wordOffsetToSectorIdx(lane.blockOffset), dcache_SectorCount), 0.U(dcache_SectorCount.W)))
      .reduce(_ | _)
  }

  def dirtyBytesToSectorMask(dirtyBytes: UInt): UInt = {
    val dirtyWords = dirtyBytes.asTypeOf(Vec(dcache_BlockWords, UInt(BytesOfWord.W)))
    VecInit((0 until dcache_SectorCount).map { sector =>
      dirtyWords.slice(sector * dcache_SectorWords, (sector + 1) * dcache_SectorWords).map(_.orR).reduce(_ | _)
    }).asUInt
  }

  //For InvOrFlu
  val hasDirty_st0 = Wire(Bool())
  val choosenDirtySetIdx_st0 = Wire(UInt(log2Up(set).W))
  val way_valid = RegInit(VecInit(Seq.fill(set)(VecInit(Seq.fill(way)(0.U(1.W))))))
  // // ***** tag_array::has_dirty *****
  // //val hasDirty_st0 = Wire(Bool())
  // val setDirty = Wire(Vec(set, Bool()))
  // val way_dirtyAfterValid = Wire(Vec(set, Vec(way, Bool())))
  // //val choosenDirtySetIdx_st0 = Wire(UInt(log2Up(set).W))
  // val choosenDirtySetValid = Wire(Vec(way, Bool()))
  // val choosenDirtyWayMask_st0 = Wire(UInt(way.W))//OH
  // val choosenDirtyWayMask_st1 = Wire(UInt(way.W))
  // val choosenDirtyTag_st1 = Wire(UInt(tagBits.W))

    // ***** tag_array::has_dirty *****
  //val hasDirty_st0 = Wire(Bool())
  if(!readOnly) {
    val setDirty = Wire(Vec(set, Bool()))
    val way_dirtyAfterValid = Wire(Vec(set, Vec(way, Bool())))
    //val choosenDirtySetIdx_st0 = Wire(UInt(log2Up(set).W))
    val choosenDirtySetValid = Wire(Vec(way, Bool()))
    val choosenDirtyWayMask_st0 = Wire(UInt(way.W))//OH
    val choosenDirtyWayMask_st1 = Wire(UInt(way.W))
    val choosenDirtyTag_st1 = Wire(UInt(tagBits.W))

  //for Chisel coding convenience, dont set way_dirty to be optional
  val way_dirty = RegInit(VecInit(Seq.fill(set)(VecInit(Seq.fill(way)(0.U(1.W))))))
  val sector_valid = RegInit(VecInit(Seq.fill(set)(VecInit(Seq.fill(way)(0.U(dcache_SectorCount.W))))))
  val sector_dirty = RegInit(VecInit(Seq.fill(set)(VecInit(Seq.fill(way)(0.U(dcache_SectorCount.W))))))
  val way_tag = RegInit(VecInit(Seq.fill(set)(VecInit(Seq.fill(way)(0.U(tagBits.W))))))
  //if(!readOnly){Some()} else None
  // allocateWrite_st1
  val Replacement = Module(new ReplacementUnit(Length_Replace_time_SRAM, way))

  val allocateWrite_st1 = RegEnable(io.allocateWrite.bits, io.allocateWrite.fire)
  val allocateWriteData_st1 = RegEnable(io.allocateWriteData_st1, io.allocateWrite.fire)
  val allocateSectorMaskWrite_st1 = Wire(UInt(dcache_SectorCount.W))
  // ******      tag_array::probe    ******
  val iTagChecker = Module(new tagChecker(way=way,tagIdxBits=tagBits, AsidBits = AsidBits))
  val cachehit_hold = Module(new Queue(new tagCheckerResult(way),1))
  //SRAM to store tag
  val tagBodyAccess = Module(new SRAMTemplate(
    UInt(tagBits.W),
    set=set,
    way=way,
    shouldReset = false,
    holdRead = true,
    singlePort = false,
    bypassWrite = true
  ))
  if(readOnly){
    tagBodyAccess.io.r.req <> io.probeRead
  }else{
    val tagAccessRArb = Module(new Arbiter (new SRAMBundleA(set),3))
    tagBodyAccess.io.r.req <> tagAccessRArb.io.out
    //For probe
    tagAccessRArb.io.in(1)<> io.probeRead
    //For allocate
    tagAccessRArb.io.in(0).valid := io.allocateWrite.valid
    tagAccessRArb.io.in(0).bits.setIdx := io.allocateWrite.bits.setIdx
    //For hasDirty
    tagAccessRArb.io.in(2).valid := !io.probeRead.valid && !io.allocateWrite.valid
    tagAccessRArb.io.in(2).bits.setIdx := choosenDirtySetIdx_st0
    //io.allocateWrite.ready := tagAccessRArb.io.in(0).ready
  }

  val allocateTagHitWayMask_st1 = Reverse(Cat(way_tag(allocateWrite_st1.setIdx).zip(way_valid(allocateWrite_st1.setIdx)).map {
    case (tag, valid) => valid.asBool && (tag === allocateWriteData_st1)
  }))
  val allocateReuseWay_st1 = allocateTagHitWayMask_st1.orR
  val allocateWayMask_st1 = Mux(allocateReuseWay_st1, allocateTagHitWayMask_st1, Replacement.io.waymask_st1)
  val allocateWayIdx_st1 = OHToUInt(allocateWayMask_st1)
  val allocateStageValid_st1 = RegNext(io.allocateWrite.fire, false.B)
  val allocateWriteHoldValid_st1 = RegInit(false.B)
  val allocateWriteSetIdxHold_st1 = Reg(UInt(log2Up(set).W))
  val allocateWriteDataHold_st1 = Reg(UInt(tagBits.W))
  val allocateWayMaskHold_st1 = RegInit(0.U(way.W))
  val allocateReuseWayHold_st1 = RegInit(false.B)
  val allocateSectorMaskHold_st1 = Reg(UInt(dcache_SectorCount.W))
  val allocateWriteAsidHold_st1 = if (MMU_ENABLED) Some(Reg(UInt(AsidBits.W))) else None
  val allocateWriteCapture_st1 = allocateStageValid_st1 && !io.allocateWriteTagSRAMWValid_st1
  val allocateWriteSetIdxForWrite_st1 = Mux(allocateWriteHoldValid_st1, allocateWriteSetIdxHold_st1, allocateWrite_st1.setIdx)
  val allocateWriteDataForWrite_st1 = Mux(allocateWriteHoldValid_st1, allocateWriteDataHold_st1, allocateWriteData_st1)
  val allocateWayMaskWrite_st1 = Mux(allocateWriteHoldValid_st1, allocateWayMaskHold_st1, allocateWayMask_st1)
  val allocateWayIdxWrite_st1 = OHToUInt(allocateWayMaskWrite_st1)
  val allocateReuseWayWrite_st1 = Mux(allocateWriteHoldValid_st1, allocateReuseWayHold_st1, allocateReuseWay_st1)
  allocateSectorMaskWrite_st1 := Mux(allocateWriteHoldValid_st1, allocateSectorMaskHold_st1, io.allocateSectorMask_st1)
  val allocateOldDirtySectorMaskWrite_st1 =
    sector_dirty(allocateWriteSetIdxForWrite_st1)(allocateWayIdxWrite_st1)
  val allocatePreservedDirtySectorMaskWrite_st1 = Mux(
    allocateReuseWayWrite_st1,
    allocateOldDirtySectorMaskWrite_st1 & allocateSectorMaskWrite_st1,
    0.U(dcache_SectorCount.W)
  )
  val allocateDataSectorMaskWrite_st1 =
    allocateSectorMaskWrite_st1 & ~allocatePreservedDirtySectorMaskWrite_st1
  io.allocateDataSectorMask_st1 := allocateDataSectorMaskWrite_st1
  val allocateWriteAsidForWrite_st1 = if (MMU_ENABLED) {
    Some(Mux(allocateWriteHoldValid_st1, allocateWriteAsidHold_st1.get, io.allocateWriteAsid_st1.get))
  } else {
    None
  }
  when(allocateWriteCapture_st1 && !allocateWriteHoldValid_st1) {
    allocateWriteSetIdxHold_st1 := allocateWrite_st1.setIdx
    allocateWriteDataHold_st1 := allocateWriteData_st1
    allocateWayMaskHold_st1 := allocateWayMask_st1
    allocateReuseWayHold_st1 := allocateReuseWay_st1
    allocateSectorMaskHold_st1 := io.allocateSectorMask_st1
    if (MMU_ENABLED) {
      allocateWriteAsidHold_st1.get := io.allocateWriteAsid_st1.get
    }
  }
  when(io.allocateWriteTagSRAMWValid_st1) {
    allocateWriteHoldValid_st1 := false.B
  }.elsewhen(allocateWriteCapture_st1 && !allocateWriteHoldValid_st1) {
    allocateWriteHoldValid_st1 := true.B
  }

  // SRAM for storing ASID tag
if(MMU_ENABLED) {
  val ASIDAccess = Module(new SRAMTemplate(
    UInt(AsidBits.W),
    set = set,
    way = way,
    shouldReset = false,
    holdRead = true,
    singlePort = false,
    bypassWrite = true
  ))
  val ASIDAccessRArb = Module(new Arbiter(new SRAMBundleA(set), 2))
  ASIDAccess.io.r.req <> ASIDAccessRArb.io.out //io.probeRead
  ASIDAccessRArb.io.in(0) <> io.probeRead
  ASIDAccessRArb.io.in(1).valid := !io.probeRead.valid && !io.allocateWrite.valid
  ASIDAccessRArb.io.in(1).bits.setIdx := choosenDirtySetIdx_st0
  iTagChecker.io.ASID_of_set.get := ASIDAccess.io.r.resp.data
  iTagChecker.io.ASID_from_pipe.get := io.asidFromCore_st1.get
  val asidReplacement_st1 = ASIDAccess.io.r.resp.data(OHToUInt(Replacement.io.waymask_st1))
  val choosenDirtyASID_st1 = Wire(UInt(AsidBits.W))
  io.asidReplacement_st1.get := asidReplacement_st1
  choosenDirtyASID_st1 := ASIDAccess.io.r.resp.data(OHToUInt(choosenDirtyWayMask_st1))
  io.dirtyASID_st1.get := choosenDirtyASID_st1
  ASIDAccess.io.w.req.valid := io.allocateWriteTagSRAMWValid_st1 && !io.invalidateAll
  ASIDAccess.io.w.req.bits.apply(data = allocateWriteAsidForWrite_st1.get, setIdx = allocateWriteSetIdxForWrite_st1, waymask = allocateWayMaskWrite_st1)
}
  //SRAM for replacement policy
  //store last_access_time for LRU, or last_fill_time for FIFO
  val timeAccess = Module(new SRAMTemplate(
    UInt(Length_Replace_time_SRAM.W),
    set = set,
    way = way,
    shouldReset = false,
    holdRead = true,
    singlePort = false,
    bypassWrite = true
  ))
  timeAccess.io.r.req.valid := io.allocateWrite.fire
  timeAccess.io.r.req.bits.setIdx := io.allocateWrite.bits.setIdx
  //io.allocateWrite.ready := true.B
  //although use arb, src0 and src1 should not come in same cycle
  val timeAccessWArb = Module(new Arbiter (new SRAMBundleAW(UInt(Length_Replace_time_SRAM.W),set,way),2))
  val hitTimeUpdatePending = RegInit(false.B)
  val hitTimeUpdateDataReg = Reg(UInt(Length_Replace_time_SRAM.W))
  val hitTimeUpdateSetIdxReg = Reg(UInt(log2Up(set).W))
  val hitTimeUpdateWaymaskReg = Reg(UInt(way.W))
  val allocateTimeUpdateValid = RegNext(io.allocateWrite.fire, false.B)
  val hitTimeUpdateNowValid = io.hit_st1
  val hitTimeUpdateNowData = accessCount
  val hitTimeUpdateNowSetIdx = RegNext(io.probeRead.bits.setIdx)
  val hitTimeUpdateNowWaymask = io.waymaskHit_st1
  val hitTimeUpdateIssueValid = Wire(Bool())
  val hitTimeUpdateIssueData = Wire(UInt(Length_Replace_time_SRAM.W))
  val hitTimeUpdateIssueSetIdx = Wire(UInt(log2Up(set).W))
  val hitTimeUpdateIssueWaymask = Wire(UInt(way.W))

  hitTimeUpdateIssueValid := false.B
  hitTimeUpdateIssueData := hitTimeUpdateNowData
  hitTimeUpdateIssueSetIdx := hitTimeUpdateNowSetIdx
  hitTimeUpdateIssueWaymask := hitTimeUpdateNowWaymask

  when(allocateTimeUpdateValid){
    when(hitTimeUpdateNowValid){
      hitTimeUpdatePending := true.B
      hitTimeUpdateDataReg := hitTimeUpdateNowData
      hitTimeUpdateSetIdxReg := hitTimeUpdateNowSetIdx
      hitTimeUpdateWaymaskReg := hitTimeUpdateNowWaymask
    }
  }.otherwise{
    when(hitTimeUpdatePending){
      hitTimeUpdateIssueValid := true.B
      hitTimeUpdateIssueData := hitTimeUpdateDataReg
      hitTimeUpdateIssueSetIdx := hitTimeUpdateSetIdxReg
      hitTimeUpdateIssueWaymask := hitTimeUpdateWaymaskReg
      when(hitTimeUpdateNowValid){
        hitTimeUpdatePending := true.B
        hitTimeUpdateDataReg := hitTimeUpdateNowData
        hitTimeUpdateSetIdxReg := hitTimeUpdateNowSetIdx
        hitTimeUpdateWaymaskReg := hitTimeUpdateNowWaymask
      }.otherwise{
        hitTimeUpdatePending := false.B
      }
    }.elsewhen(hitTimeUpdateNowValid){
      hitTimeUpdateIssueValid := true.B
      hitTimeUpdateIssueData := hitTimeUpdateNowData
      hitTimeUpdateIssueSetIdx := hitTimeUpdateNowSetIdx
      hitTimeUpdateIssueWaymask := hitTimeUpdateNowWaymask
    }
  }

  //LRU replacement policy
  //timeAccessWArb.io.in(0) for regular R/W hit update access time
  timeAccessWArb.io.in(0).valid := hitTimeUpdateIssueValid
  timeAccessWArb.io.in(0).bits(
    data = hitTimeUpdateIssueData,
    setIdx = hitTimeUpdateIssueSetIdx,
    waymask = hitTimeUpdateIssueWaymask
  )
  //timeAccessWArb.io.in(1) for memRsp allocate
  timeAccessWArb.io.in(1).valid := RegNext(io.allocateWrite.fire, false.B)
  timeAccessWArb.io.in(1).bits(
    data = accessCount,
    setIdx = RegNext(io.allocateWrite.bits.setIdx),
    waymask = io.waymaskReplacement_st1
  )
  timeAccess.io.w.req <> timeAccessWArb.io.out//meta_entry_t::update_access_time

  // ******      dirty_mask_array    ******
  //! 不能使用寄存器阵列实现，因为会将verilog代码扩大10倍有余，编译压力太大
  //! 使用阵列的输出同样有个问题，就是阵列是不能一个clk清零的，同时也为了降低写端口的仲裁，直接将way_dirty寄存器用作阵列的valid信号
  // dirty_mask阵列用来记录cacheline中被修改的字节，在写回L2时，只写dirty_mask中为1的位
  // 这个阵列的设计是为了防止多个SM对同一个cacheline的不同部分进行写入时，导致的一致性问题
  // val dirty_mask = RegInit(VecInit(Seq.fill(set)(VecInit(Seq.fill(way)(VecInit(Seq.fill(dcache_BlockWords)(0.U(BytesOfWord.W))))))))  
  val dirtyMaskAccess = Module(new SRAMTemplate(
    UInt((dcache_BlockWords * BytesOfWord).W),
    set = set,
    way = way,
    shouldReset = true,
    holdRead = true,
    singlePort = false,
    bypassWrite = true
  ))
  // 读有3种情况，首先是每次needreplace时，需要读取被冲刷的dirty_mask；然后是无效化时，需要依次读取所有dirty cacheline的dirty_mask；
  // 最后是每次常规读写命中时，需要读取被命中cacheline的dirty_mask与本次写入的dirty_mask，相或得到本次的dirty_mask
  // 因为针对无效化的读是只要有dirty就会一直读，所以要将其优先级放至最低，否则常规写的命中就会一直读不到
  val dirtyMaskArb = Module(new Arbiter(new SRAMBundleA(set),3))
  dirtyMaskAccess.io.r.req <> dirtyMaskArb.io.out
  // 需要在发生分配写的时候就发起读请求，因为在分配写流程的st1阶段，已经能确定被冲刷的cacheline，且要返回给core内
  // 这时候再发起读请求就跟不上时序。所以可以提前于判断是否命中的时候就读出来，反正最后都由need_replace信号作为总的使能
  dirtyMaskArb.io.in(0).valid := io.allocateWrite.fire
  dirtyMaskArb.io.in(0).bits.setIdx := io.allocateWrite.bits.setIdx
  // 在常规写前命中前，需要读取这个set的dirty mask，如果写命中了，需要将读出来的值与本次写入的dirty mask相或
  dirtyMaskArb.io.in(1) <> io.probeRead
  // 只要有dirty时就会发起读请求，但这个读请求只会在顶层的 flushChoosen 信号拉高时才有效
  // 在无效化时，根据之前的流水级，当hasDirty_st0为真时发起读请求，直接将读到的值赋值给out即可
  dirtyMaskArb.io.in(2).valid := hasDirty_st0
  dirtyMaskArb.io.in(2).bits.setIdx := choosenDirtySetIdx_st0

  val holdValid_st1 = cachehit_hold.io.deq.valid
  val activeSectorsInput_st1 = activeSectorMask(io.perLaneAddr_st1)
  val writeByteMaskInput_st1 = io.writeByteMask_st1
  val activeSectors_st1 = Mux(holdValid_st1, cachehit_hold.io.deq.bits.activeSectorMask, activeSectorsInput_st1)
  val writeByteMask_st1 = Mux(holdValid_st1, cachehit_hold.io.deq.bits.writeByteMask, writeByteMaskInput_st1)

  // 从 perLaneAddr_st1 中构造要写入的dirty mask
  // WireInit不是只执行一次的初始化，而是每个时钟周期都会将Wire重置为初始值,这是组合逻辑，不是寄存器逻辑
  // 会产生类似这样的组合逻辑: assign dirtyMaskPerCL[0] = (条件0满足且blockOffset==0) ? 新值 : 0;
  // dirtyMaskPerCL_init 代表本次要写入的 dirty mask 的初始值，需要与原有的值相或得到本次写入的 dirty mask
  val dirtyMaskPerCL_init = WireInit(VecInit(Seq.fill(dcache_BlockWords)(0.U(BytesOfWord.W))))
  val dirtyMaskPerCL = WireInit(VecInit(Seq.fill(dcache_BlockWords)(0.U(BytesOfWord.W))))
  val hitWayIdxForDirty_st1 = Wire(UInt(log2Up(way).W))
  val hitWayMaskForDirty_st1 = Wire(UInt(way.W))
  val probeSetIdx_st1 = probeReadBuf.bits.setIdx
  val dirtyWriteSetIdx_st1 = io.writeSetIdx_st1
  val oldDirtyMaskValidForHit_st1 = way_dirty(dirtyWriteSetIdx_st1)(hitWayIdxForDirty_st1).asBool
  val oldDirtyMaskForHit_st1 = Mux(
    oldDirtyMaskValidForHit_st1,
    dirtyMaskAccess.io.r.resp.data(hitWayIdxForDirty_st1).asUInt,
    0.U((dcache_BlockWords * BytesOfWord).W)
  )
  dirtyMaskPerCL_init := writeByteMask_st1.asTypeOf(dirtyMaskPerCL_init)
  dirtyMaskPerCL := (dirtyMaskPerCL_init.asUInt | oldDirtyMaskForHit_st1).asTypeOf(dirtyMaskPerCL)

  // 一旦用到 dirtyMaskAccess 读出的值，就应该在下个周期将这个位置的 dirty mask 写0，所以写也需要一个仲裁器
  val dirtyMaskWriteArb = Module(new Arbiter(new SRAMBundleAW(UInt((dcache_BlockWords * BytesOfWord).W), set, way), 3))
  dirtyMaskAccess.io.w.req <> dirtyMaskWriteArb.io.out

  // 分配写在st1确定是否需要替换，如果要替换，读出的dirty mask就是被用到，需要在这个阶段发起写0请求
  // 默认 io.needReplace.get 只会拉高一个周期，且不会被阻塞
  dirtyMaskWriteArb.io.in(0).valid := io.needReplace.get
  dirtyMaskWriteArb.io.in(0).bits.apply(data = 0.U, setIdx = allocateWrite_st1.setIdx, waymask = Replacement.io.waymask_st1)
  // 在常规读写命中时，即第一级流水发起写请求，只有这个写请求是给阵列写实际值
  val probeSt1Fire = probeReadBuf.valid && probeReadBuf.ready
  dirtyMaskWriteArb.io.in(1).valid := io.writeHitFire_st1
  dirtyMaskWriteArb.io.in(1).bits.apply(data = dirtyMaskPerCL.asUInt, setIdx = dirtyWriteSetIdx_st1, waymask = hitWayMaskForDirty_st1)
  // 只有当 flushChoosen 拉高时，读出来 dirty mask 才会被用到，需要被写0
  // 这里的 valid 需要用 RegNext 延迟一周期是因为在dcache的顶层模块将 InvOrFluMemReqValid_st1 里也延了一个clk
  // 不使用dcache中的 InvOrFluMemReqValid_st1 是因为与tag的发出对齐
  dirtyMaskWriteArb.io.in(2).valid := RegNext(io.flushChoosen.get, false.B)
  dirtyMaskWriteArb.io.in(2).bits.apply(data = 0.U, setIdx = RegNext(choosenDirtySetIdx_st0), waymask = choosenDirtyWayMask_st1)

  val writeSectors_st1 = dirtyBytesToSectorMask(writeByteMask_st1)
  iTagChecker.io.tag_of_set := tagBodyAccess.io.r.resp.data//st1
  //iTagChecker.io.ASID_of_set := ASIDAccess.io.r.resp.data
  iTagChecker.io.tag_from_pipe := io.tagFromCore_st1
  //iTagChecker.io.ASID_from_pipe := io.asidFromCore_st1
  iTagChecker.io.way_valid := way_valid(RegEnable(io.probeRead.bits.setIdx,io.probeRead.fire))//st1
  ////st1

  val tagMatchWayMask_now = Reverse(Cat(tagBodyAccess.io.r.resp.data.zip(way_valid(probeSetIdx_st1)).map {
    case (tag, valid) => valid.asBool && (tag === io.tagFromCore_st1)
  }))
  val sectorCoveredWayMask_now = Reverse(Cat((0 until way).map { i =>
    !activeSectors_st1.orR ||
      ((sector_valid(probeSetIdx_st1)(i) & activeSectors_st1) === activeSectors_st1)
  }))
  val sectorHitWayMask_now = tagMatchWayMask_now & sectorCoveredWayMask_now

  cachehit_hold.io.enq.bits.hit := iTagChecker.io.cache_hit && !probeReadBuf.ready
  cachehit_hold.io.enq.bits.waymask := Mux(!probeReadBuf.ready, iTagChecker.io.waymask ,0.U)
  cachehit_hold.io.enq.bits.sectorHitWayMask := Mux(!probeReadBuf.ready, sectorHitWayMask_now, 0.U)
  cachehit_hold.io.enq.bits.activeSectorMask := activeSectorsInput_st1
  cachehit_hold.io.enq.bits.writeByteMask := writeByteMaskInput_st1
  cachehit_hold.io.enq.valid := probeReadBuf.valid && !probeReadBuf.ready
  cachehit_hold.io.deq.ready := probeReadBuf.ready
  //val cachehit_hold = RegNext(iTagChecker.io.cache_hit && probeReadBuf.valid && !probeReadBuf.ready)
  val hit_st1_raw = Mux(holdValid_st1, cachehit_hold.io.deq.bits.hit, iTagChecker.io.cache_hit)
  val waymask_st1_raw = Mux(holdValid_st1, cachehit_hold.io.deq.bits.waymask, iTagChecker.io.waymask)
  val tagMatchWayMask_st1 = Mux(holdValid_st1, cachehit_hold.io.deq.bits.waymask, tagMatchWayMask_now)
  val sectorCoveredWayMask_st1 = sectorCoveredWayMask_now
  val sectorHitWayMask_st1 = Mux(holdValid_st1, cachehit_hold.io.deq.bits.sectorHitWayMask, sectorHitWayMask_now)
  val waymask_st1 = PriorityEncoderOH(sectorHitWayMask_st1)
  val hitWayIdx_st1 = OHToUInt(waymask_st1)
  hitWayIdxForDirty_st1 := hitWayIdx_st1
  hitWayMaskForDirty_st1 := waymask_st1
  val sectorValidRaw_st1 = sector_valid(probeSetIdx_st1)(hitWayIdx_st1)
  val sectorValidHit_st1 =
    !activeSectors_st1.orR ||
      ((sectorValidRaw_st1 & activeSectors_st1) === activeSectors_st1)
  val hit_st1 = hit_st1_raw && sectorHitWayMask_st1.orR && sectorValidHit_st1 && probeReadBuf.valid
  val hitDirtyBytesRaw_st1 = dirtyMaskAccess.io.r.resp.data(hitWayIdx_st1).asUInt
  val hitDirtyBytes_st1 = Mux(
    way_dirty(probeSetIdx_st1)(hitWayIdx_st1).asBool,
    hitDirtyBytesRaw_st1,
    0.U((dcache_BlockWords * BytesOfWord).W)
  )
  val hitDirtySectorMask_st1 = sector_dirty(probeSetIdx_st1)(hitWayIdx_st1) | dirtyBytesToSectorMask(hitDirtyBytes_st1)
  io.hit_st1 := hit_st1//RegNext(io.probeRead.fire) //todo remove
  io.hitStatus_st1.hit := hit_st1
  io.hitStatus_st1.waymask := waymask_st1
  io.hitStatus_st1.isDirty := hitDirtySectorMask_st1.orR
  io.hitStatus_st1.tag := tagBodyAccess.io.r.resp.data(OHToUInt(io.hitStatus_st1.waymask))
  io.hit_dirty_sector_mask_st1 := hitDirtySectorMask_st1
  io.hit_dirty_mask_st1 := hitDirtyBytes_st1
  io.waymaskHit_st1 := waymask_st1
  if(!readOnly){//tag_array::write_hit_mark_dirty
    //assert(!(iTagChecker.io.cache_hit && io.probeIsWrite_st1.get && io.flushChoosen.get),"way_dirty write-in conflict!")
    when(io.writeHitFire_st1){////meta_entry_t::write_dirty
      val writeSetIdx = dirtyWriteSetIdx_st1
      val writeWayIdx = OHToUInt(io.hitStatus_st1.waymask)
      sector_dirty(writeSetIdx)(writeWayIdx) := sector_dirty(writeSetIdx)(writeWayIdx) | writeSectors_st1
      way_dirty(writeSetIdx)(writeWayIdx) := true.B
    }.elsewhen(io.flushChoosen.get){//tag_array::flush_one
      way_dirty(choosenDirtySetIdx_st0)(OHToUInt(choosenDirtyWayMask_st0)) := false.B
      sector_dirty(choosenDirtySetIdx_st0)(OHToUInt(choosenDirtyWayMask_st0)) := 0.U
    }.elsewhen(io.needReplace.get) {
      way_dirty(allocateWrite_st1.setIdx)(OHToUInt(Replacement.io.waymask_st1)) := false.B
      sector_dirty(allocateWrite_st1.setIdx)(OHToUInt(Replacement.io.waymask_st1)) := 0.U
    }.elsewhen(probeReadBuf.valid && iTagChecker.io.cache_hit && io.probeIsUncache_st1 && probeReadBuf.ready){
      way_dirty(probeSetIdx_st1)(OHToUInt(iTagChecker.io.waymask)) := false.B
      sector_dirty(probeSetIdx_st1)(OHToUInt(iTagChecker.io.waymask)) := 0.U
      sector_valid(probeSetIdx_st1)(OHToUInt(iTagChecker.io.waymask)) := 0.U
    }
  }




  // ******      tag_array::allocate    ******
  Replacement.io.validOfSet := Reverse(Cat(way_valid(allocateWrite_st1.setIdx)))//Reverse(Cat(way_valid(io.allocateWrite.bits.setIdx)))
  Replacement.io.timeOfSet_st1 := timeAccess.io.r.resp.data//meta_entry_t::get_access_time
  if (!readOnly) {
    io.needReplace.get := !allocateReuseWay_st1 && way_dirty(allocateWrite_st1.setIdx)(OHToUInt(Replacement.io.waymask_st1)).asBool && RegNext(io.allocateWrite.fire, false.B)
    io.replaceValidVictim_st1.get := RegNext(io.allocateWrite.fire, false.B) && !allocateReuseWay_st1 && Replacement.io.Set_is_full
  }
  io.waymaskReplacement_st1 := allocateWayMask_st1//tag_array::replace_choice
  val replacementWayIdx_st1 = OHToUInt(Replacement.io.waymask_st1)
  val tagnset = Cat(way_tag(allocateWrite_st1.setIdx)(replacementWayIdx_st1), //tag
    allocateWrite_st1.setIdx)

  if (!readOnly) {
    io.a_addrReplacement_st1.get := Cat(tagnset, //setIdx
      0.U((dcache_BlockOffsetBits + dcache_WordOffsetBits).W)) //blockOffset+wordOffset
  }
  // 需要将dirtyMaskAccess读出的数据与way_dirtyAfterValid相与，因为
  val replaceDirtyBytes_st1 = dirtyMaskAccess.io.r.resp.data(OHToUInt(Replacement.io.waymask_st1)).asUInt
  io.replace_dirty_mask_st1 := replaceDirtyBytes_st1
  io.replace_dirty_sector_mask_st1 :=
    sector_dirty(allocateWrite_st1.setIdx)(OHToUInt(Replacement.io.waymask_st1)) |
      dirtyBytesToSectorMask(replaceDirtyBytes_st1)

  tagBodyAccess.io.w.req.valid := io.allocateWriteTagSRAMWValid_st1 && !io.invalidateAll//meta_entry_t::allocate
  tagBodyAccess.io.w.req.bits.apply(data = allocateWriteDataForWrite_st1, setIdx = allocateWriteSetIdxForWrite_st1, waymask = allocateWayMaskWrite_st1)


  when(io.invalidateAll){//tag_array::invalidate_all()
    way_valid := VecInit(Seq.fill(set)(VecInit(Seq.fill(way)(false.B))))
    sector_valid := VecInit(Seq.fill(set)(VecInit(Seq.fill(way)(0.U(dcache_SectorCount.W)))))
    sector_dirty := VecInit(Seq.fill(set)(VecInit(Seq.fill(way)(0.U(dcache_SectorCount.W)))))
    way_tag := VecInit(Seq.fill(set)(VecInit(Seq.fill(way)(0.U(tagBits.W)))))
  }.elsewhen(io.allocateWriteTagSRAMWValid_st1){//meta_entry_t::allocate TODO
    way_tag(allocateWriteSetIdxForWrite_st1)(allocateWayIdxWrite_st1) := allocateWriteDataForWrite_st1
    when(!allocateReuseWayWrite_st1){
      way_valid(allocateWriteSetIdxForWrite_st1)(allocateWayIdxWrite_st1) := true.B
    }
    sector_valid(allocateWriteSetIdxForWrite_st1)(allocateWayIdxWrite_st1) := Mux(
      allocateReuseWayWrite_st1,
      sector_valid(allocateWriteSetIdxForWrite_st1)(allocateWayIdxWrite_st1) | allocateSectorMaskWrite_st1,
      allocateSectorMaskWrite_st1
    )
    sector_dirty(allocateWriteSetIdxForWrite_st1)(allocateWayIdxWrite_st1) := Mux(
      allocateReuseWayWrite_st1,
      sector_dirty(allocateWriteSetIdxForWrite_st1)(allocateWayIdxWrite_st1) & ~allocateDataSectorMaskWrite_st1,
      0.U(dcache_SectorCount.W)
    )
  }.elsewhen (iTagChecker.io.cache_hit && io.probeIsUncache_st1) {
    way_valid(probeReadBuf.bits.setIdx)(OHToUInt(iTagChecker.io.waymask)) := false.B
    sector_valid(probeReadBuf.bits.setIdx)(OHToUInt(iTagChecker.io.waymask)) := 0.U
    sector_dirty(probeReadBuf.bits.setIdx)(OHToUInt(iTagChecker.io.waymask)) := 0.U
  }
  assert(!(io.allocateWrite.valid && io.invalidateAll))


    //set一般值为128。
    //评估后，每set配priority mux的成本约为所有set普通mux后共用priority mux的5-6倍，
    //代价是普通 mux 7个2in1 mux的延迟。
    for (i <- 0 until set) {
      way_dirtyAfterValid(i) := VecInit(way_dirty(i).zip(way_valid(i)).map { case (v, d) => v & d })
      setDirty(i) := way_dirtyAfterValid(i).asUInt.orR
    }
    hasDirty_st0 := setDirty.asUInt.orR
    choosenDirtySetIdx_st0 := PriorityEncoder(setDirty)
    choosenDirtySetValid := way_dirtyAfterValid(choosenDirtySetIdx_st0)
    choosenDirtyWayMask_st0 := VecInit(PriorityEncoderOH(choosenDirtySetValid)).asUInt
    choosenDirtyWayMask_st1 := RegNext(choosenDirtyWayMask_st0, 0.U)
    choosenDirtyTag_st1 := way_tag(RegNext(choosenDirtySetIdx_st0))(OHToUInt(choosenDirtyWayMask_st1))

    //val choosenDirtySetIdx_st1 = RegNext(choosenDirtySetIdx_st0)
    //val choosenDirtyWayMask_st1 = RegNext(choosenDirtyWayMask_st0)
    val dirtyBytes_st1 = dirtyMaskAccess.io.r.resp.data(OHToUInt(choosenDirtyWayMask_st1)).asUInt
    io.dirtyTag_st1.get := choosenDirtyTag_st1
    io.dirtySetIdx_st0.get := choosenDirtySetIdx_st0
    io.dirtyMask_st1 := dirtyBytes_st1
    io.dirtySectorMask_st1 :=
      sector_dirty(RegNext(choosenDirtySetIdx_st0))(OHToUInt(choosenDirtyWayMask_st1)) |
        dirtyBytesToSectorMask(dirtyBytes_st1)

    io.dirtyWayMask_st0.get := choosenDirtyWayMask_st0
    io.hasDirty_st0.get := hasDirty_st0//RegNext(hasDirty_st0)

}}

class ReplacementUnit(timeLength:Int, way: Int, debug:Boolean=false) extends Module{
  val io = IO(new Bundle {
    val validOfSet = Input(UInt(way.W))//MSB at left
    val timeOfSet_st1 = Input(Vec(way,UInt(timeLength.W)))//MSB at right
    val waymask_st1 = Output(UInt(way.W))
    val Set_is_full = Output(Bool())
  })
  val wayIdxWidth = log2Ceil(way)
  val victimIdx = if (way>1) Wire(UInt(wayIdxWidth.W)) else Wire(UInt(1.W))
  io.Set_is_full := io.validOfSet === Fill(way,1.U)

  if (way>1) {
    val timeOfSetAfterValid = Wire(Vec(way,UInt(timeLength.W)))
    for (i <- 0 until way)
      timeOfSetAfterValid(i) := Mux(io.validOfSet(i),io.timeOfSet_st1(i),0.U)
    val minTimeChooser = Module(new minIdxTree(width=timeLength,numInput=way))
    minTimeChooser.io.candidateIn := timeOfSetAfterValid
    victimIdx := minTimeChooser.io.idxOfMin
  }else victimIdx := 0.U

  io.waymask_st1 := UIntToOH(Mux(io.Set_is_full, victimIdx, PriorityEncoder(~io.validOfSet)))
  // First case, set not full
  //Second case, full set, replacement happens

  //debug use
  if(debug){
    when(io.validOfSet.asBools.reduce(_ | _) === true.B) {
      printf(io.validOfSet.asBools.map{ x => p"${x} " }.reduceOption(_ + _).getOrElse(p"") + p"\n" +
             io.timeOfSet_st1.reverse.map{ x => p"${x} " }.reduceOption(_ + _).getOrElse(p"") +
             p"\noutput: ${io.waymask_st1}\n")
    }
  }
}
class tagChecker(way: Int, tagIdxBits: Int, AsidBits: Int) extends Module{
  val io = IO(new Bundle {
    val tag_of_set = Input(Vec(way,UInt(tagIdxBits.W)))//MSB the valid bit
    val ASID_of_set = if(MMU_ENABLED) Some{Input(Vec(way,UInt(AsidBits.W)))} else None
    //val valid_of_set = Input(Vec(way,Bool()))
    val tag_from_pipe = Input(UInt(tagIdxBits.W))
    val ASID_from_pipe = if(MMU_ENABLED) Some{Input(UInt(AsidBits.W))} else None
    val way_valid = Input(Vec(way,Bool()))

    val waymask = Output(UInt(way.W))//one hot
    val cache_hit = Output(Bool())
  })

  //io.waymask := Cat(io.tag_of_set.zip(io.way_valid).map{ case(tag,valid) => (tag === io.tag_from_pipe) && valid})
  val tagMatch = Wire(UInt(way.W))
  if(MMU_ENABLED){
    val ASIDMatch = Wire(UInt(way.W))
    ASIDMatch := Reverse(Cat(io.ASID_of_set.get.zip(io.way_valid).map{ case(tag,valid) => (tag === io.ASID_from_pipe.get) && valid}))
    io.waymask := tagMatch & ASIDMatch
  } else {
    io.waymask := tagMatch
  }

  tagMatch :=   Reverse(Cat(io.tag_of_set.zip(io.way_valid).map{ case(tag,valid) => (tag === io.tag_from_pipe) && valid}))


  //Reverse(Cat(io.tag_of_set.zip(io.way_valid).map{ case(tag,valid) => (tag === io.tag_from_pipe) && valid}))
  //io.waymask := Reverse(Cat(io.tag_of_set.map{ tag => (tag(tagIdxBits-1,0) === io.tag_from_pipe) && tag(tagIdxBits)}))
  //assert(PopCount(io.waymask) <= 1.U)//if waymask not one-hot, duplicate tags in one set, error
  io.cache_hit := io.waymask.orR
}

class minIdxTree(width: Int, numInput: Int) extends Module{
  val treeLevel = log2Ceil(numInput)
  val io = IO(new Bundle{
    val candidateIn = Input(Vec(numInput, UInt(width.W)))
    val idxOfMin = Output(UInt(treeLevel.W))
  })
  class candWithIdx extends Bundle{
    val candidate = UInt(width.W)
    var index = UInt(treeLevel.W)
  }
  def minWithIdx(a:candWithIdx, b:candWithIdx): candWithIdx = Mux(a.candidate < b.candidate,a,b)

  val candVec = Wire(Vec(numInput,new candWithIdx))
  for(i <- 0 until numInput){
    candVec(i).candidate := io.candidateIn(numInput-1-i)
    candVec(i).index := i.asUInt
  }

  io.idxOfMin := candVec.reduceTree(minWithIdx(_,_)).index
}
class L1TagAccess_ICache(set: Int, way: Int, tagBits: Int, AsidBits: Int)extends Module{
  //This module contain Tag memory, its valid bits, tag comparator, and Replacement Unit
  val io = IO(new Bundle {
    val r = Flipped(new SRAMReadBus(UInt(tagBits.W), set, way))
    val r_asid = if(MMU_ENABLED) Some{Flipped(new SRAMReadBus(UInt(AsidBits.W), set, way))} else None
    val tagFromCore_st1 = Input(UInt(tagBits.W))
    val asidFromCore_st1 = if(MMU_ENABLED) Some{Input(UInt(AsidBits.W))} else None
    val coreReqReady = Input(Bool())

    val w = Flipped(new SRAMWriteBus(UInt(tagBits.W), set, way))
    val w_asid = if(MMU_ENABLED) Some{Flipped(new SRAMWriteBus(UInt(AsidBits.W), set, way))} else None

    val waymaskReplacement = Output(UInt(way.W))//one hot, for SRAMTemplate
    val waymaskHit_st1 = Output(UInt(way.W))

    val hit_st1 = Output(Bool())

    val invalidate = Input(Bool())
  })
  val tagBodyAccess = Module(new SRAMTemplate(
    UInt(tagBits.W),
    set = set,
    way = way,
    shouldReset = false,
    holdRead = true,
    singlePort = false,
    bypassWrite = false
  ))
  tagBodyAccess.io.r <> io.r
  tagBodyAccess.io.r.req.valid := io.r.req.valid && !io.invalidate
  io.r.req.ready := tagBodyAccess.io.r.req.ready && !io.invalidate

  val way_valid = RegInit(VecInit(Seq.fill(set)(VecInit(Seq.fill(way)(0.U(1.W))))))
  //val way_valid = Mem(set, UInt(way.W))
  // ******      Replacement    ******
  val Replacement = Module(new ReplacementUnit_ICache(way))
  // ******      TagChecker    ******
  val iTagChecker = Module(new tagChecker(way = way, tagIdxBits = tagBits, AsidBits = AsidBits))
  iTagChecker.io.tag_of_set := tagBodyAccess.io.r.resp.data //st1
  iTagChecker.io.tag_from_pipe := io.tagFromCore_st1
  if(MMU_ENABLED) {
    val asidAccess = Module(new SRAMTemplate(
      UInt(AsidBits.W),
      set = set,
      way = way,
      shouldReset = false,
      holdRead = true,
      singlePort = false,
      bypassWrite = false
    ))

    asidAccess.io.r <> io.r_asid.get
    asidAccess.io.r.req.valid := io.r.req.valid && !io.invalidate
    io.r.req.ready := asidAccess.io.r.req.ready && !io.invalidate
    iTagChecker.io.ASID_of_set.get := asidAccess.io.r.resp.data
    iTagChecker.io.ASID_from_pipe.get := io.asidFromCore_st1.get
    asidAccess.io.w.req.valid := io.w_asid.get.req.valid && !io.invalidate
    io.w_asid.get.req.ready := asidAccess.io.w.req.ready && !io.invalidate
    asidAccess.io.w.req.bits.apply(data = io.w_asid.get.req.bits.data, setIdx = io.w_asid.get.req.bits.setIdx, waymask = Replacement.io.waymask)
  }
  iTagChecker.io.way_valid := way_valid(RegEnable(io.r.req.bits.setIdx, io.coreReqReady)) //st1
  io.waymaskHit_st1 := iTagChecker.io.waymask //st1
  io.hit_st1 := iTagChecker.io.cache_hit

  Replacement.io.validbits_of_set := Cat(way_valid(io.w.req.bits.setIdx))
  io.waymaskReplacement := Replacement.io.waymask
  tagBodyAccess.io.w.req.valid := io.w.req.valid && !io.invalidate

  io.w.req.ready := tagBodyAccess.io.w.req.ready && !io.invalidate
  tagBodyAccess.io.w.req.bits.apply(data = io.w.req.bits.data, setIdx = io.w.req.bits.setIdx, waymask = Replacement.io.waymask)

  when(io.invalidate) {
    way_valid := 0.U.asTypeOf(way_valid)
  } .elsewhen(io.w.req.valid && !Replacement.io.Set_is_full) {
    way_valid(io.w.req.bits.setIdx)(OHToUInt(Replacement.io.waymask)) := true.B
  }

}
class ReplacementUnit_ICache(way: Int) extends Module{
  val io = IO(new Bundle {
    val validbits_of_set = Input(UInt(way.W))
    val waymask = Output(UInt(way.W))//one hot
    val Set_is_full = Output(Bool())
  })
  val victim_1Hidx = if (way>1) RegInit(1.U(way.W)) else RegInit(0.U(1.W))
  io.Set_is_full := io.validbits_of_set === Fill(way,1.U)
  io.waymask := Mux(io.Set_is_full, victim_1Hidx, UIntToOH(VecInit(io.validbits_of_set.asBools).indexWhere(_===false.B)))
  // First case, set not full
  //Second case, full set, replacement happens
  if (way>1) victim_1Hidx := RegEnable(Cat(victim_1Hidx(way-2,0),victim_1Hidx(way-1)),io.Set_is_full)
}

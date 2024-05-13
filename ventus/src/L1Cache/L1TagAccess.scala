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
import top.parameters._
class tagCheckerResult(way: Int) extends Bundle{
  val waymask = UInt(way.W)
  val hit = Bool()
}
//This module contain Tag memory, its valid bits, tag comparator, and Replacement Unit
class L1TagAccess(set: Int, way: Int, tagBits: Int, readOnly: Boolean)extends Module{
  val io = IO(new Bundle {
    //From coreReq_pipe0
    val probeRead = Flipped(Decoupled(new SRAMBundleA(set)))//Probe Channel
    val tagFromCore_st1 = Input(UInt(tagBits.W))
    val probeIsWrite_st1 = if(!readOnly){Some(Input(Bool()))} else None
    //val coreReqReady = Input(Bool())//TODO try to replace with probeRead.fire
    //To coreReq_pipe1
    val hit_st1 = Output(Bool())
    val waymaskHit_st1 = Output(UInt(way.W))
    //From memRsp_pipe0
    val allocateWrite = Flipped(ValidIO(new SRAMBundleA(set)))//Allocate Channel
    val allocateWriteData_st1 = Input(UInt(tagBits.W))
    //From memRsp_pipe1
    val allocateWriteTagSRAMWValid_st1 = Input(Bool())
    //To memRsp_pipe1
    val needReplace = if(!readOnly){
      Some(Output(Bool()))
    } else None
    val waymaskReplacement_st1 = Output(UInt(way.W))//one hot, for SRAMTemplate
    val a_addrReplacement_st1 = if (!readOnly) {
      Some(Output(UInt(xLen.W)))
    } else None
    //For InvOrFlu
    val hasDirty_st0 = if (!readOnly) {Some(Output(Bool()))} else None
    val dirtySetIdx_st0 = if (!readOnly) {Some(Output(UInt(log2Up(set).W)))} else None
    val dirtyWayMask_st0 = if (!readOnly) {Some(Output(UInt(way.W)))} else None
    val dirtyTag_st1 = if (!readOnly) {Some(Output(UInt(tagBits.W)))} else None
    //For InvOrFlu and LRSC
    val flushChoosen = if (!readOnly) {Some(Flipped(ValidIO(UInt((log2Up(set)+way).W))))} else None
    //For Inv
    val invalidateAll = Input(Bool())
    val tagready_st1 = Input(Bool())
  })
  //TagAccess internal parameters
  val Length_Replace_time_SRAM: Int = 10
  assert(!(io.probeRead.fire && io.allocateWrite.fire), s"tag probe and allocate in same cycle")
  val probeReadBuf = Queue(io.probeRead,1,pipe=true)
  probeReadBuf.ready := io.tagready_st1

  //access time counter
  val accessFire = io.probeRead.fire || io.allocateWrite.fire
  val (accessCount,accessCounterFull) = Counter(accessFire,1000)

  //For InvOrFlu
  val hasDirty_st0 = Wire(Bool())
  val choosenDirtySetIdx_st0 = Wire(UInt(log2Up(set).W))

  //SRAM to store tag
  val tagBodyAccess = Module(new SRAMTemplate(
    UInt(tagBits.W),
    set=set,
    way=way,
    shouldReset = false,
    holdRead = true,
    singlePort = false,
    bypassWrite = false
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

  //SRAM for replacement policy
  //store last_access_time for LRU, or last_fill_time for FIFO
  val timeAccess = Module(new SRAMTemplate(
    UInt(Length_Replace_time_SRAM.W),
    set = set,
    way = way,
    shouldReset = false,
    holdRead = true,
    singlePort = false,
    bypassWrite = false
  ))
  timeAccess.io.r.req.valid := io.allocateWrite.fire
  timeAccess.io.r.req.bits.setIdx := io.allocateWrite.bits.setIdx
  //io.allocateWrite.ready := true.B
  //although use arb, src0 and src1 should not come in same cycle
  val timeAccessWArb = Module(new Arbiter (new SRAMBundleAW(UInt(Length_Replace_time_SRAM.W),set,way),2))
  val timeAccessWarbConflict = io.hit_st1 && RegNext(io.allocateWrite.fire)
  val timeAccessWarbConflictReg = RegNext(timeAccessWarbConflict)

  assert(!(timeAccessWArb.io.in(0).valid && timeAccessWArb.io.in(1).valid), s"tag probe and allocate in same cycle")
  //LRU replacement policy
  //timeAccessWArb.io.in(0) for regular R/W hit update access time
  timeAccessWArb.io.in(0).valid := Mux(timeAccessWarbConflictReg,RegNext(io.hit_st1),Mux(timeAccessWarbConflict,false.B,   io.hit_st1))//hit already contain probe fire
  timeAccessWArb.io.in(0).bits(
    data = Mux(timeAccessWarbConflictReg,RegNext(accessCount),accessCount),
    setIdx = Mux(timeAccessWarbConflictReg,RegNext(RegNext(io.probeRead.bits.setIdx)),RegNext(io.probeRead.bits.setIdx)),
    waymask = Mux(timeAccessWarbConflictReg,RegNext(io.waymaskHit_st1),io.waymaskHit_st1)
  )
  //timeAccessWArb.io.in(1) for memRsp allocate
  timeAccessWArb.io.in(1).valid := RegNext(io.allocateWrite.fire)
  timeAccessWArb.io.in(1).bits(
    data = accessCount,
    setIdx = RegNext(io.allocateWrite.bits.setIdx),
    waymask = io.waymaskReplacement_st1
  )
  timeAccess.io.w.req <> timeAccessWArb.io.out//meta_entry_t::update_access_time

  val way_valid = RegInit(VecInit(Seq.fill(set)(VecInit(Seq.fill(way)(0.U(1.W))))))
  //for Chisel coding convenience, dont set way_dirty to be optional
  val way_dirty = RegInit(VecInit(Seq.fill(set)(VecInit(Seq.fill(way)(0.U(1.W))))))
  //if(!readOnly){Some()} else None
  // allocateWrite_st1
  val Replacement = Module(new ReplacementUnit(Length_Replace_time_SRAM, way))

  val allocateWrite_st1 = RegEnable(io.allocateWrite.bits, io.allocateWrite.fire)
  // ******      tag_array::probe    ******
  val iTagChecker = Module(new tagChecker(way=way,tagIdxBits=tagBits))
  val cachehit_hold = Module(new Queue(new tagCheckerResult(way),1))
  iTagChecker.io.tag_of_set := tagBodyAccess.io.r.resp.data//st1
  iTagChecker.io.tag_from_pipe := io.tagFromCore_st1
  iTagChecker.io.way_valid := way_valid(RegEnable(io.probeRead.bits.setIdx,io.probeRead.fire))//st1
  ////st1

  cachehit_hold.io.enq.bits.hit := iTagChecker.io.cache_hit && !probeReadBuf.ready
  cachehit_hold.io.enq.bits.waymask := Mux(!probeReadBuf.ready, iTagChecker.io.waymask ,0.U)
  cachehit_hold.io.enq.valid := probeReadBuf.valid
  cachehit_hold.io.deq.ready := probeReadBuf.ready
  //val cachehit_hold = RegNext(iTagChecker.io.cache_hit && probeReadBuf.valid && !probeReadBuf.ready)
  io.hit_st1 := (iTagChecker.io.cache_hit || cachehit_hold.io.deq.bits.hit && cachehit_hold.io.deq.valid) && probeReadBuf.valid//RegNext(io.probeRead.fire)
  io.waymaskHit_st1 := Mux(cachehit_hold.io.deq.valid && cachehit_hold.io.deq.bits.hit,cachehit_hold.io.deq.bits.waymask,iTagChecker.io.waymask)
  if(!readOnly){//tag_array::write_hit_mark_dirty
    assert(!(iTagChecker.io.cache_hit && io.probeIsWrite_st1.get && io.flushChoosen.get.valid),"way_dirty write-in conflict!")
    when(iTagChecker.io.cache_hit && io.probeIsWrite_st1.get){////meta_entry_t::write_dirty
      way_dirty(RegNext(io.probeRead.bits.setIdx))(OHToUInt(iTagChecker.io.waymask)) := true.B
    }.elsewhen(io.flushChoosen.get.valid){//tag_array::flush_one
      way_dirty(io.flushChoosen.get.bits((log2Up(set)+way)-1,way))(OHToUInt(io.flushChoosen.get.bits(way-1,0))) := false.B
    }.elsewhen(io.needReplace.get) {
      way_dirty(allocateWrite_st1.setIdx)(OHToUInt(Replacement.io.waymask_st1)) := false.B
    }
  }




  if (!readOnly) {
    io.needReplace.get := way_dirty(allocateWrite_st1.setIdx)(OHToUInt(Replacement.io.waymask_st1)).asBool && RegNext(io.allocateWrite.fire())
  }
  // ******      tag_array::allocate    ******
  Replacement.io.validOfSet := Reverse(Cat(way_valid(allocateWrite_st1.setIdx)))//Reverse(Cat(way_valid(io.allocateWrite.bits.setIdx)))
  Replacement.io.timeOfSet_st1 := timeAccess.io.r.resp.data//meta_entry_t::get_access_time
  io.waymaskReplacement_st1 := Replacement.io.waymask_st1//tag_array::replace_choice
  val tagnset = Cat(tagBodyAccess.io.r.resp.data(OHToUInt(Replacement.io.waymask_st1)), //tag
    allocateWrite_st1.setIdx)
  if (!readOnly) {
    io.a_addrReplacement_st1.get := Cat(tagnset, //setIdx
      0.U((dcache_BlockOffsetBits + dcache_WordOffsetBits).W)) //blockOffset+wordOffset
  }
  tagBodyAccess.io.w.req.valid := io.allocateWriteTagSRAMWValid_st1//meta_entry_t::allocate
  tagBodyAccess.io.w.req.bits.apply(data = io.allocateWriteData_st1, setIdx = allocateWrite_st1.setIdx, waymask = Replacement.io.waymask_st1)
  when(RegNext(io.allocateWrite.fire) && !Replacement.io.Set_is_full){//meta_entry_t::allocate TODO
    way_valid(allocateWrite_st1.setIdx)(OHToUInt(Replacement.io.waymask_st1)) := true.B
  }.elsewhen(io.invalidateAll){//tag_array::invalidate_all()
    way_valid := VecInit(Seq.fill(set)(VecInit(Seq.fill(way)(false.B))))
  }
  assert(!(io.allocateWrite.valid && io.invalidateAll))

  // ***** tag_array::has_dirty *****
  //val hasDirty_st0 = Wire(Bool())
  if(!readOnly) {
    val setDirty = Wire(Vec(set, Bool()))
    val way_dirtyAfterValid = Wire(Vec(set, Vec(way, Bool())))
    //val choosenDirtySetIdx_st0 = Wire(UInt(log2Up(set).W))
    val choosenDirtySetValid = Wire(Vec(way, Bool()))
    val choosenDirtyWayMask_st0 = Wire(UInt(way.W))//OH
    val choosenDirtyTag_st1 = Wire(UInt(tagBits.W))
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
    choosenDirtyTag_st1 := tagBodyAccess.io.r.resp.data(OHToUInt(choosenDirtyWayMask_st0))
    //val choosenDirtySetIdx_st1 = RegNext(choosenDirtySetIdx_st0)
    //val choosenDirtyWayMask_st1 = RegNext(choosenDirtyWayMask_st0)
    io.dirtyTag_st1.get := choosenDirtyTag_st1
    io.dirtySetIdx_st0.get := choosenDirtySetIdx_st0
    io.dirtyWayMask_st0.get := choosenDirtyWayMask_st0
    io.hasDirty_st0.get := hasDirty_st0//RegNext(hasDirty_st0)
  }
}

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
      for (i <- 0 until way)
        printf("%d  ", io.validOfSet(i))
      printf("\n")
      for (i <- 0 until way)
        printf("%d ", io.timeOfSet_st1(way-1-i))
      printf("\noutput: %d\n", io.waymask_st1)
    }
  }
}
class tagChecker(way: Int, tagIdxBits: Int) extends Module{
  val io = IO(new Bundle {
    val tag_of_set = Input(Vec(way,UInt(tagIdxBits.W)))//MSB the valid bit
    //val valid_of_set = Input(Vec(way,Bool()))
    val tag_from_pipe = Input(UInt(tagIdxBits.W))
    val way_valid = Input(Vec(way,Bool()))

    val waymask = Output(UInt(way.W))//one hot
    val cache_hit = Output(Bool())
  })

  //io.waymask := Cat(io.tag_of_set.zip(io.way_valid).map{ case(tag,valid) => (tag === io.tag_from_pipe) && valid})

  io.waymask := Reverse(Cat(io.tag_of_set.zip(io.way_valid).map{ case(tag,valid) => (tag === io.tag_from_pipe) && valid}))
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
class L1TagAccess_ICache(set: Int, way: Int, tagBits: Int)extends Module{
  //This module contain Tag memory, its valid bits, tag comparator, and Replacement Unit
  val io = IO(new Bundle {
    val r = Flipped(new SRAMReadBus(UInt(tagBits.W), set, way))
    val tagFromCore_st1 = Input(UInt(tagBits.W))
    val coreReqReady = Input(Bool())

    val w = Flipped(new SRAMWriteBus(UInt(tagBits.W), set, way))

    val waymaskReplacement = Output(UInt(way.W))//one hot, for SRAMTemplate
    val waymaskHit_st1 = Output(UInt(way.W))

    val hit_st1 = Output(Bool())
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

  val way_valid = RegInit(VecInit(Seq.fill(set)(VecInit(Seq.fill(way)(0.U(1.W))))))
  //val way_valid = Mem(set, UInt(way.W))

  // ******      TagChecker    ******
  val iTagChecker = Module(new tagChecker(way = way, tagIdxBits = tagBits))
  iTagChecker.io.tag_of_set := tagBodyAccess.io.r.resp.data //st1
  iTagChecker.io.tag_from_pipe := io.tagFromCore_st1
  iTagChecker.io.way_valid := way_valid(RegEnable(io.r.req.bits.setIdx, io.coreReqReady)) //st1
  io.waymaskHit_st1 := iTagChecker.io.waymask //st1
  io.hit_st1 := iTagChecker.io.cache_hit

  // ******      Replacement    ******
  val Replacement = Module(new ReplacementUnit_ICache(way))
  Replacement.io.validbits_of_set := Cat(way_valid(io.w.req.bits.setIdx))
  io.waymaskReplacement := Replacement.io.waymask
  tagBodyAccess.io.w.req.valid := io.w.req.valid
  io.w.req.ready := tagBodyAccess.io.w.req.ready
  tagBodyAccess.io.w.req.bits.apply(data = io.w.req.bits.data, setIdx = io.w.req.bits.setIdx, waymask = Replacement.io.waymask)
  when(io.w.req.valid && !Replacement.io.Set_is_full) {
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

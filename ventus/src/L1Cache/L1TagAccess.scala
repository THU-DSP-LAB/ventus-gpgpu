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
import pipeline.parameters._

//This module contain Tag memory, its valid bits, tag comparator, and Replacement Unit
class L1TagAccess(set: Int, way: Int, tagBits: Int, readOnly: Boolean)extends Module{
  val io = IO(new Bundle {
    //From coreReq_pipe0
    val probeRead = Flipped(Decoupled(new SRAMBundleA(set)))//Probe Channel
    val tagFromCore_st1 = Input(UInt(tagBits.W))
    val probeIsWrite_st1 = if(!readOnly){Some(Bool())} else None
    //val coreReqReady = Input(Bool())//TODO try to replace with probeRead.fire
    //To coreReq_pipe1
    val hit_st1 = Output(Bool())
    val waymaskHit_st1 = Output(UInt(way.W))
    //From memRsp_pipe0
    val allocateWrite = Flipped(Decoupled(new SRAMBundleAW(UInt(tagBits.W), set, way)))//Allocate Channel
    //To memRsp_pipe1
    val waymaskReplacement_st1 = Output(UInt(way.W))//one hot, for SRAMTemplate
    //To MemReq_Q(memRsp_pipe1)
    val memReq = if(!readOnly) {
      Some(DecoupledIO(new L1CacheMemReq))
    } else None
  })
  //TagAccess internal parameters
  val Length_Replace_time_SRAM: Int = 10
  assert(!(io.probeRead.fire && io.allocateWrite.fire), s"tag probe and allocate in same cycle")

  //access time counter
  val accessFire = io.probeRead.fire || io.allocateWrite.fire
  val (accessCount,accessCounterFull) = Counter(accessFire,1000)

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
    val tagAccessRArb = Module(new Arbiter (new SRAMBundleA(set),2))
    tagBodyAccess.io.r.req <> tagAccessRArb.io.out
    tagAccessRArb.io.in(1) <> io.probeRead
    tagAccessRArb.io.in(0).valid := io.allocateWrite.valid
    tagAccessRArb.io.in(0).bits.setIdx := io.allocateWrite.bits.setIdx
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
  //io.allocateWrite.ready := true.B//TODO be conditional after add memReq_Q
  //although use arb, src0 and src1 should not come in same cycle
  val timeAccessWArb = Module(new Arbiter (new SRAMBundleAW(UInt(Length_Replace_time_SRAM.W),set,way),2))
  assert(!(timeAccessWArb.io.in(0).valid && timeAccessWArb.io.in(1).valid), s"tag probe and allocate in same cycle")
  //LRU replacement policy
  //timeAccessWArb.io.in(0) for regular R/W hit update access time
  timeAccessWArb.io.in(0).valid := io.hit_st1//hit already contain probe fire
  timeAccessWArb.io.in(0).bits(
    data = accessCount,
    setIdx = RegNext(io.probeRead.bits.setIdx),
    waymask = io.waymaskHit_st1
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

  // ******      tag_array::probe    ******
  val iTagChecker = Module(new tagChecker(way=way,tagIdxBits=tagBits))
  iTagChecker.io.tag_of_set := tagBodyAccess.io.r.resp.data//st1
  iTagChecker.io.tag_from_pipe := io.tagFromCore_st1
  iTagChecker.io.way_valid := way_valid(RegEnable(io.probeRead.bits.setIdx,io.probeRead.fire))//st1
  io.waymaskHit_st1 := iTagChecker.io.waymask//st1
  io.hit_st1 := iTagChecker.io.cache_hit
  if(!readOnly){//tag_array::write_hit_mark_dirty
    when(iTagChecker.io.cache_hit && io.probeIsWrite_st1.get){
      way_dirty(RegNext(io.probeRead.bits.setIdx))(iTagChecker.io.waymask) := true.B
    }//meta_entry_t::write_dirty
  }

  // allocateWrite_st1
  val Replacement = Module(new ReplacementUnit(Length_Replace_time_SRAM,way))

  val allocateWrite_st1 = RegEnable(io.allocateWrite.bits, io.allocateWrite.fire)
  val allocateWrite_st1_valid = Reg(Bool())
  if(!readOnly){
    when(io.allocateWrite.fire) {
      allocateWrite_st1_valid := true.B
    }.elsewhen(io.memReq.get.fire) {
      allocateWrite_st1_valid := false.B
    }
  }else{
    allocateWrite_st1_valid := RegNext(io.allocateWrite.fire)
  }

  val replaceIsDirty = if (!readOnly) {
    Some(way_dirty(allocateWrite_st1.setIdx)(OHToUInt(Replacement.io.waymask_st1)).asBool)
  } else None
  val replaceIsReady: Bool = if (!readOnly) {
    !Replacement.io.Set_is_full || (replaceIsDirty.get && io.memReq.get.ready)
  } else true.B
  io.allocateWrite.ready := !allocateWrite_st1_valid || replaceIsReady
  // ******      tag_array::allocate    ******
  Replacement.io.validOfSet := Cat(way_valid(io.allocateWrite.bits.setIdx))
  Replacement.io.timeOfSet_st1 := timeAccess.io.r.resp.data//meta_entry_t::get_access_time
  io.waymaskReplacement_st1 := Replacement.io.waymask_st1//tag_array::replace_choice
  if(!readOnly){//tag_array::issue_memReq_write
    io.memReq.get.valid := replaceIsDirty.get && allocateWrite_st1_valid
    io.memReq.get.bits.a_opcode := 0.U//PutFullData
    io.memReq.get.bits.a_param := 0.U//regular write
    io.memReq.get.bits.a_source := Cat("d2".U,allocateWrite_st1.setIdx)//refer to a_source确定机制 in onenote TODO
    io.memReq.get.bits.a_addr := Cat(Cat(tagBodyAccess.io.r.resp.data(Replacement.io.waymask_st1),//tag
      allocateWrite_st1.setIdx),//setIdx
      0.U((dcache_BlockOffsetBits+dcache_WordOffsetBits).W))//blockOffset+wordOffset
    io.memReq.get.bits.a_mask := VecInit(Seq.fill(dcache_BlockWords)(true.B))
    io.memReq.get.bits.a_data := DontCare//to be replaced by Data SRAM out
  }
  tagBodyAccess.io.w.req.valid := allocateWrite_st1_valid//meta_entry_t::allocate
  tagBodyAccess.io.w.req.bits.apply(data = allocateWrite_st1.data, setIdx = allocateWrite_st1.setIdx, waymask = Replacement.io.waymask_st1)
  when(RegNext(io.allocateWrite.fire) && !Replacement.io.Set_is_full){//meta_entry_t::allocate TODO
    way_valid(allocateWrite_st1.setIdx)(OHToUInt(Replacement.io.waymask_st1)) := true.B
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

  io.waymask_st1 := Mux(io.Set_is_full, victimIdx, PriorityEncoder(~io.validOfSet))
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

  io.waymask := Reverse(Cat(io.tag_of_set.zip(io.way_valid).map{ case(tag,valid) => (tag === io.tag_from_pipe) && valid}))
  //io.waymask := Reverse(Cat(io.tag_of_set.map{ tag => (tag(tagIdxBits-1,0) === io.tag_from_pipe) && tag(tagIdxBits)}))
  assert(PopCount(io.waymask) <= 1.U)//if waymask not one-hot, duplicate tags in one set, error
  io.cache_hit := io.waymask.orR
}

class minIdxTree(width: Int, numInput: Int) extends Module{//TODO turn this to be minIdxTree
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
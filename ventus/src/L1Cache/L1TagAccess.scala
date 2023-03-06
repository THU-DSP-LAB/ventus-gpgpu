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

class getEntryStatus(nEntry: Int) extends Module{
  val io = IO(new Bundle{
    val valid_list = Input(UInt(nEntry.W))
    //val alm_full = Output(Bool())
    val full = Output(Bool())
    val next = Output(UInt(log2Up(nEntry).W))
    val used = Output(UInt())
  })

  io.used := PopCount(io.valid_list)
  //io.alm_full := io.used === (nEntry.U-1.U)
  io.full := io.used === nEntry.U
  io.next := VecInit(io.valid_list.asBools).indexWhere(_ === false.B)
}

//This module contain Tag memory, its valid bits, tag comparator, and Replacement Unit
class L1TagAccess(set: Int, way: Int, tagBits: Int, readOnly: Boolean)extends Module{
  val io = IO(new Bundle {
    val probeRead = Flipped(Decoupled(new SRAMBundleA(set)))//Probe Channel
    val tagFromCore_st1 = Input(UInt(tagBits.W))
    val coreReqReady = Input(Bool())//TODO try to replace with probeRead.fire

    val allocateWrite = Flipped(Decoupled(new SRAMBundleAW(UInt(tagBits.W), set, way)))//Allocate Channel

    val waymaskReplacement_st1 = Output(UInt(way.W))//one hot, for SRAMTemplate
    val waymaskHit_st1 = Output(UInt(way.W))

    val hit_st1 = Output(Bool())
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
  tagBodyAccess.io.r.req <> io.probeRead

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
  io.allocateWrite.ready := true.B//TODO be conditional after add memReq_Q
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

  val way_valid = RegInit(VecInit(Seq.fill(set)(VecInit(Seq.fill(way)(0.U(1.W))))))
  val way_dirty = if(!readOnly){
    Some(RegInit(VecInit(Seq.fill(set)(VecInit(Seq.fill(way)(0.U(1.W)))))))
  } else None

  // ******      tag_array::probe    ******
  val iTagChecker = Module(new tagChecker(way=way,tagIdxBits=tagBits))
  iTagChecker.io.tag_of_set := tagBodyAccess.io.r.resp.data//st1
  iTagChecker.io.tag_from_pipe := io.tagFromCore_st1
  iTagChecker.io.way_valid := way_valid(RegEnable(io.probeRead.bits.setIdx,io.coreReqReady))//st1
  io.waymaskHit_st1 := iTagChecker.io.waymask//st1
  io.hit_st1 := iTagChecker.io.cache_hit

  // ******      tag_array::allocate    ******
  val Replacement = Module(new ReplacementUnit(Length_Replace_time_SRAM,way))
  Replacement.io.validOfSet := Cat(way_valid(io.allocateWrite.bits.setIdx))
  Replacement.io.timeOfSet_st1 := timeAccess.io.r.resp.data//meta_entry_t::get_access_time
  io.waymaskReplacement_st1 := Replacement.io.waymask_st1//tag_array::replace_choice
  val allocateWrite_st1 = RegEnable(io.allocateWrite.bits,io.allocateWrite.fire)
  tagBodyAccess.io.w.req.valid := RegNext(io.allocateWrite.fire)//meta_entry_t::allocate
  tagBodyAccess.io.w.req.bits.apply(data = allocateWrite_st1.data, setIdx = allocateWrite_st1.setIdx, waymask = Replacement.io.waymask_st1)
  when(RegNext(io.allocateWrite.fire) && !Replacement.io.Set_is_full){//meta_entry_t::allocate
    way_valid(io.allocateWrite.bits.setIdx)(OHToUInt(Replacement.io.waymask_st1)) := true.B
  }
  timeAccess.io.w.req <> timeAccessWArb.io.out//meta_entry_t::update_access_time
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
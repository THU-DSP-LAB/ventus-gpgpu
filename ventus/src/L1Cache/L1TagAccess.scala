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
class L1TagAccess(set: Int, way: Int, tagBits: Int)extends Module{
  val io = IO(new Bundle {
    val probeRead = Flipped(Decoupled(new SRAMBundleA(set)))//Probe Channel
    val tagFromCore_st1 = Input(UInt(tagBits.W))
    val coreReqReady = Input(Bool())

    val allocateWrite = Flipped(Decoupled(new SRAMBundleAW(UInt(tagBits.W), set, way)))//Allocate Channel

    val waymaskReplacement = Output(UInt(way.W))//one hot, for SRAMTemplate
    val waymaskHit_st1 = Output(UInt(way.W))

    val hit_st1 = Output(Bool())
  })
  //TagAccess internal parameters
  val Length_Replace_time_SRAM: Int = 10

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
  timeAccess.io.r.req <> io.allocateWrite

  val way_valid = RegInit(VecInit(Seq.fill(set)(VecInit(Seq.fill(way)(0.U(1.W))))))
  //val way_valid = Mem(set, UInt(way.W))

  // ******      tag_array::probe    ******
  val iTagChecker = Module(new tagChecker(way=way,tagIdxBits=tagBits))
  iTagChecker.io.tag_of_set := tagBodyAccess.io.r.resp.data//st1
  iTagChecker.io.tag_from_pipe := io.tagFromCore_st1
  iTagChecker.io.way_valid := way_valid(RegEnable(io.probeRead.bits.setIdx,io.coreReqReady))//st1
  io.waymaskHit_st1 := iTagChecker.io.waymask//st1
  io.hit_st1 := iTagChecker.io.cache_hit

  // ******      Replacement    ******
  val Replacement = Module(new ReplacementUnit(Length_Replace_time_SRAM,way))
  Replacement.io.validOfSet := Cat(way_valid(io.allocateWrite.bits.setIdx))
  io.waymaskReplacement := Replacement.io.waymask
  tagBodyAccess.io.w.req.valid := io.allocateWrite.valid
  io.allocateWrite.ready := tagBodyAccess.io.w.req.ready
  tagBodyAccess.io.w.req.bits.apply(data = io.allocateWrite.bits.data, setIdx = io.allocateWrite.bits.setIdx, waymask = Replacement.io.waymask)
  when(io.allocateWrite.valid && !Replacement.io.Set_is_full){
    way_valid(io.allocateWrite.bits.setIdx)(OHToUInt(Replacement.io.waymask)) := true.B
  }

}

class ReplacementUnit(timeLength:Int, way: Int) extends Module{
  val io = IO(new Bundle {
    val validOfSet = Input(UInt(way.W))
    val timeOfSet = Input(Vec(way,UInt(timeLength.W)))
    val waymask = Output(UInt(way.W))//one hot
    val Set_is_full = Output(Bool())
  })
  val victim_1Hidx = if (way>1) Wire(UInt(way.W)) else Wire(UInt(1.W))
  io.Set_is_full := io.validOfSet === Fill(way,1.U)
  io.waymask := Mux(io.Set_is_full, victim_1Hidx, UIntToOH(VecInit(io.validOfSet.asBools).indexWhere(_===false.B)))
  // First case, set not full
  //Second case, full set, replacement happens
  if (way>1) victim_1Hidx := 0.U
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


class maxIdxTree(width: Int, numInput: Int) extends Module{
  val treeLevel = log2Ceil(numInput)
  val io = IO(new Bundle{
    val candidateIn = Input(Vec(numInput, UInt(width.W)))
    val idxOfMax = Output(UInt(treeLevel.W))
  })
  class candWithIdx extends Bundle{
    val candidate = UInt(width.W)
    var index = UInt(treeLevel.W)
  }
  def maxWithIdx(a:candWithIdx, b:candWithIdx): candWithIdx = Mux(a.candidate > b.candidate,a,b)

  val candVec = Wire(Vec(numInput,new candWithIdx))
  for(i <- 0 to numInput-1){
    candVec(i).candidate := io.candidateIn(i)
    candVec(i).index := i.asUInt
  }

  io.idxOfMax := candVec.reduceTree(maxWithIdx(_,_)).index
}
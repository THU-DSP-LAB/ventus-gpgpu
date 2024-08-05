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
package L1Cache.ICache

import L1Cache.{HasL1CacheParameters, L1CacheModule, getEntryStatusReq}
import chisel3._
import chisel3.util._
import config.config.Parameters

trait MSHRParameters{
  def bABits: Int// = tagIdxBits+SetIdxBits   // search in this module for abbreviation meaning
  def tIBits: Int// = widBits+blockOffsetBits+wordOffsetBits
}

trait HasMshrParameters extends HasL1CacheParameters{

}
/*
* 这一版支持missReq和missRspIn在blockAddr不同时同时处理（包括missRsp处于busy状态）
* 当missReq和missRspIn的blockAddr相同时，在missReq握手之前拉低ready，cache当作reservation fail告知core
* */

class MSHRmissReq(val bABits: Int, val tIWidth: Int, val WIdBits: Int) extends Bundle {// Use this bundle when handle miss issued from pipeline
  val blockAddr = UInt(bABits.W)
  //val instrId = UInt(WIdBits.W)//included in targetInfo
  val targetInfo = UInt(tIWidth.W)
}
class MSHRmissRspIn(val bABits: Int) extends Bundle {//Use this bundle when a block return from Lower cache
  val blockAddr = UInt(bABits.W)
}
class MSHRmissRspOut[T <: Data](val bABits: Int, val tIWidth: Int) extends Bundle {//Use this bundle when a block return from Lower cache
  val targetInfo = UInt(tIWidth.W)
  val blockAddr = UInt(bABits.W)
  //val busy = Bool()
  //val last = Bool()
}
class MSHRmiss2mem(val bABits: Int, val WIdBits: Int) extends Bundle {//Use this bundle when a block return from Lower cache
  val blockAddr = UInt(bABits.W)
  val instrId = UInt(WIdBits.W)
}
/*class MSHRmiss2Mem(val bAWidth: Int) extends Bundle{
  val blockAddr = UInt(bAWidth.W)
}*/

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
  io.full := io.valid_list.andR
  io.next := VecInit(io.valid_list.asBools).indexWhere(_ === false.B)
}

class MSHR[T <: Data](val tIgen: T)(implicit val p: Parameters) extends L1CacheModule{
  //TODO parameterization method need improvement
  val tIWidth = tIgen.getWidth
  //val bAWidth = bABits.getWidth
  val io = IO(new Bundle{
    val missReq = Flipped(Decoupled(new MSHRmissReq(bABits,tIWidth,WIdBits)))
    val missRspIn = Flipped(Decoupled(new MSHRmissRspIn(bABits)))
    val missRspOut = Decoupled(new MSHRmissRspOut(bABits,tIWidth))
    val miss2mem = Decoupled(new MSHRmiss2mem(bABits,WIdBits))
  })
  // head of entry, for comparison
  val blockAddr_Access = RegInit(VecInit(Seq.fill(NMshrEntry)(0.U(bABits.W))))
  val targetInfo_Accesss = RegInit(VecInit(Seq.fill(NMshrEntry)(VecInit(Seq.fill(NMshrSubEntry)(0.U(tIWidth.W))))))

  /*Structure Diagram
  * bA  : blockAddr
  * tI  : targetInfo
  * e_v : entry_valid, which is the first column of s_v
  * s_v : subentry_valid
  * h_s : has_send2mem
  *
  * reg   +reg   +reg        || reg + reg array
  * h_s(0)+s_v(0)+bA#0       || tI#0 | s_v(1)+tI#1 | ... | s_v(n)+tI#NMshrSubEntry-1
  * h_s(1)+s_v(1)+bA#1       || tI#0 | s_v(1)+tI#1 | ... | s_v(n)+tI#NMshrSubEntry-1
  * .
  * h_s(n)+s_v(n)+bA#NMshrEntry  || tI#0 | s_v(1)+tI#1 | ... | s_v(n)+tI#NMshrSubEntry-1
  * */

  //  ******     decide selected subentries are full or not     ******
  val entryMatchMissRsp = Wire(UInt(NMshrEntry.W))
  val entryMatchMissReq = Wire(UInt(NMshrEntry.W))
  val subentry_valid = RegInit(VecInit(Seq.fill(NMshrEntry)(VecInit(Seq.fill(NMshrSubEntry)(0.U(1.W))))))
  val subentry_selected = subentry_valid(OHToUInt(entryMatchMissRsp))
  val subentryStatus = Module(new getEntryStatus(NMshrSubEntry))// Output: alm_full, full, next
  subentryStatus.io.valid_list := Reverse(Cat(subentry_selected))
  val subentry_next2cancel = Wire(UInt(log2Up(NMshrSubEntry).W))
  subentry_next2cancel := subentry_selected.indexWhere(_===true.B)

  //  ******     decide MSHR is full or not     ******
  val entry_valid = Reverse(Cat(subentry_valid.map(Cat(_).orR)))
  val entryStatus = Module(new getEntryStatus(NMshrEntry))
  entryStatus.io.valid_list := entry_valid

  val missRsqBusy = RegInit(false.B)
  val missRspInHoldingbA = Wire(UInt(bABits.W))
  missRspInHoldingbA := Mux(missRsqBusy,RegNext(missRspInHoldingbA),io.missRspIn.bits.blockAddr)
  entryMatchMissRsp := Reverse(Cat(blockAddr_Access.map(_===missRspInHoldingbA))) & entry_valid
  assert(PopCount(entryMatchMissRsp) <= 1.U)
  entryMatchMissReq := Reverse(Cat(blockAddr_Access.map(_===io.missReq.bits.blockAddr))) & entry_valid
  assert(PopCount(entryMatchMissReq) <= 1.U)
  //  ******     decide a primary miss or a secondary miss     ******
  val secondary_miss = entryMatchMissReq.orR
  val primary_miss = !secondary_miss

  //  ******     update MSHR when missReq     ******
  val ReqConflictWithRsp = Wire(Bool())
  io.missReq.ready := !((entryStatus.io.full && primary_miss) ||
    (subentryStatus.io.full && secondary_miss) || ReqConflictWithRsp)

  val tAEntryIdx = Mux(secondary_miss,OHToUInt(entryMatchMissReq),entryStatus.io.next)
  val tASubEntryIdx = Mux(secondary_miss,subentryStatus.io.next,0.U)

  when (io.missReq.fire){
    targetInfo_Accesss(tAEntryIdx)(tASubEntryIdx) := io.missReq.bits.targetInfo
  }

  //  ******      update MSHR when missRsp    ******
  assert(!io.missRspIn.fire || (io.missRspIn.fire && subentryStatus.io.used >= 1.U))

  when(io.missRspIn.fire && (subentryStatus.io.used =/= 1.U || !io.missRspOut.ready)){
    missRsqBusy := true.B
  }.elsewhen(missRsqBusy && subentryStatus.io.used === 1.U && io.missRspOut.ready){
    missRsqBusy := false.B
  }

  io.missRspIn.ready := !missRsqBusy && io.missRspOut.ready
  io.missRspOut.valid := io.missRspIn.fire || missRsqBusy

  io.missRspOut.bits.targetInfo := targetInfo_Accesss(OHToUInt(entryMatchMissRsp))(subentry_next2cancel)
  io.missRspOut.bits.blockAddr := blockAddr_Access(OHToUInt(entryMatchMissRsp))

  //  ******     maintain subentries    ******
  for (iofEn <- 0 until NMshrEntry){
    for (iofSubEn <- 0 until NMshrSubEntry){
      when(io.missReq.fire){
        when(iofEn.asUInt===entryStatus.io.next &&
          iofSubEn.asUInt===0.U && primary_miss){
          subentry_valid(iofEn)(iofSubEn) := true.B
        }.elsewhen(iofEn.asUInt===OHToUInt(entryMatchMissReq) &&
          iofSubEn.asUInt===subentryStatus.io.next && secondary_miss){
          subentry_valid(iofEn)(iofSubEn) := true.B
        }.elsewhen(io.missRspOut.fire &&
          iofEn.asUInt === OHToUInt(entryMatchMissRsp) &&
          iofSubEn.asUInt === subentry_next2cancel){
          subentry_valid(iofEn)(iofSubEn) := false.B
        }
      }.elsewhen(io.missRspOut.fire &&
        iofEn.asUInt===OHToUInt(entryMatchMissRsp) &&
        iofSubEn.asUInt===subentry_next2cancel){
        subentry_valid(iofEn)(iofSubEn) := false.B
      }
    }//order of when & elsewhen matters, as elsewhen cover some cases of when, but no op to them
  }

  //  ******   update blockAddr Reg *****
  when(io.missReq.fire && primary_miss){
    blockAddr_Access(entryStatus.io.next) := io.missReq.bits.blockAddr
  }

  //  ******    handle miss to lower mem    ******
  val has_send2mem = RegInit(VecInit(Seq.fill(NMshrEntry)(0.U(1.W))))
  val hasSendStatus = Module(new getEntryStatus(NMshrEntry))
  hasSendStatus.io.valid_list := ~(~Reverse(Cat(has_send2mem)) & entry_valid)
  io.miss2mem.valid := !has_send2mem(hasSendStatus.io.next) && entry_valid(hasSendStatus.io.next)
  val miss2mem_fire = io.miss2mem.valid && io.miss2mem.ready

  for (i <- 0 to NMshrEntry){
    when(miss2mem_fire && i.U === hasSendStatus.io.next){
      has_send2mem(i.U) := true.B
    }.elsewhen((missRsqBusy || io.missRspIn.fire) && io.missRspOut.fire && subentryStatus.io.used===1.U && i.U === OHToUInt(entryMatchMissRsp)){
      has_send2mem(i.U) := false.B//missRsp, L2 to core
    }
  }
  io.miss2mem.bits.blockAddr := blockAddr_Access(hasSendStatus.io.next)
  io.miss2mem.bits.instrId := targetInfo_Accesss(hasSendStatus.io.next)(0.U)

  //  ******    ReqConflictWithRsp
  //missRspIn fire或者busy时，missReq来了block addr相同的请求，置高此信号
  ReqConflictWithRsp := io.missReq.valid &&
    ((io.missRspIn.fire && io.missRspIn.bits.blockAddr === io.missReq.bits.blockAddr) ||
      (missRsqBusy && io.missRspOut.bits.blockAddr === io.missReq.bits.blockAddr))
}
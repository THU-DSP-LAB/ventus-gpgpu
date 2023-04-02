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

import L1Cache.{HasL1CacheParameters, L1CacheModule, getEntryStatus}
import chisel3._
import chisel3.util._
import pipeline.parameters._

//abstract class MSHRBundle extends Bundle with L1CacheParameters

class MSHRprobe(val bABits: Int) extends Bundle {
  val blockAddr = UInt(bABits.W)
}
class MSHRprobeOut(val NEntry:Int, val NSub:Int) extends Bundle {
  val probeStatus = UInt(2.W)
  val a_source = UInt((log2Up(NEntry)+log2Up(NSub)).W)
}
class MSHRmissReq(val bABits: Int, val tIWdith: Int, val WIdBits: Int) extends Bundle {// Use this bundle when handle miss issued from pipeline
  val blockAddr = UInt(bABits.W)
  val instrId = UInt(WIdBits.W)
  val targetInfo = UInt(tIWdith.W)
}
class MSHRmissRspIn(val bABits: Int) extends Bundle {//Use this bundle when a block return from Lower cache
  val instrId = UInt(bABits.W)
}
class MSHRmissRspOut[T <: Data](val bABits: Int, val tIWdith: Int, val WIdBits: Int) extends Bundle {
  val targetInfo = UInt(tIWdith.W)
  val blockAddr = UInt(bABits.W)
  val instrId = UInt(WIdBits.W)
  //val burst = Bool()//This bit indicate the Rsp transaction comes from subentry
  //val last = Bool()
}

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


class MSHR(val bABits: Int, val tIWidth: Int, val WIdBits: Int, val NMshrEntry:Int, val NMshrSubEntry:Int) extends Module{
  val io = IO(new Bundle{
    val probe = Flipped(ValidIO(new MSHRprobe(bABits)))
    val probeOut_st1 = Output(new MSHRprobeOut(NMshrEntry,NMshrSubEntry))
    val missReq = Flipped(Decoupled(new MSHRmissReq(bABits,tIWidth,WIdBits)))
    val missRspIn = Flipped(Decoupled(new MSHRmissRspIn(bABits)))
    val missRspOut = Decoupled(new MSHRmissRspOut(bABits,tIWidth,WIdBits))
    //val miss2mem = Decoupled(new MSHRmiss2mem(bABits,WIdBits))
  })
  // head of entry, for comparison
  val blockAddr_Access = RegInit(VecInit(Seq.fill(NMshrEntry)(0.U(bABits.W))))
  val instrId_Access = RegInit(VecInit(Seq.fill(NMshrEntry)(0.U(WIdBits.W))))//TODO remove this
  val targetInfo_Accesss = RegInit(VecInit(Seq.fill(NMshrEntry)(VecInit(Seq.fill(NMshrSubEntry)(0.U(tIWidth.W))))))

  val subentry_valid = RegInit(VecInit(Seq.fill(NMshrEntry)(VecInit(Seq.fill(NMshrSubEntry)(false.B)))))
  val entry_valid = Reverse(Cat(subentry_valid.map(Cat(_).orR)))
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

  //  ******     decide selected subentries are full or not     ******
  val entryMatchMissRsp = Wire(UInt(NMshrEntry.W))
  val entryMatchProbe = Wire(UInt(NMshrEntry.W))
  val subentrySelected = subentry_valid(Mux(io.missRspIn.valid,OHToUInt(entryMatchMissRsp),OHToUInt(entryMatchProbe)))
  val subentryStatus = Module(new getEntryStatus(NMshrSubEntry)) // Output: alm_full, full, next
  subentryStatus.io.valid_list := Reverse(Cat(subentrySelected))

  //  ******     decide MSHR is full or not     ******
  val entryStatus = Module(new getEntryStatus(NMshrEntry))
  entryStatus.io.valid_list := entry_valid

  // ******     enum vec_mshr_status     ******
  val mshrStatus_st1 = RegInit(0.U(2.W))
  /*PRIMARY_AVAIL   00
  * PRIMARY_FULL    01
  * SECONDARY_AVAIL 10
  * SECONDARY_FULL  11
  * see as always valid, validity relies on external procedures
  * */
  // ******      mshr::probe_vec    ******
  entryMatchProbe := Reverse(Cat(blockAddr_Access.map(_ === io.probe.bits.blockAddr))) & entry_valid
  assert(PopCount(entryMatchProbe) <= 1.U)
  val secondaryMiss = entryMatchProbe.orR
  val primaryMiss = !secondaryMiss
  val mainEntryFull = entryStatus.io.full
  val subEntryFull = subentryStatus.io.full
  when(io.probe.valid){
    when(primaryMiss){
      when(mainEntryFull){
        mshrStatus_st1 := 1.U//PRIMARY_FULL
      }.otherwise{
        mshrStatus_st1 := 0.U//PRIMARY_AVAIL
      }
    }.otherwise{
      when(subEntryFull){
        mshrStatus_st1 := 3.U//SECONDARY_FULL
      }.otherwise{
        mshrStatus_st1 := 2.U//SECONDARY_AVAIL
      }
    }
  }
  val entryMatchProbe_st1 = RegEnable(entryMatchProbe,io.probe.valid)
  io.probeOut_st1.probeStatus := mshrStatus_st1
  def mshrProbeAvail: Bool = this.io.probeOut_st1.probeStatus === 0.U || this.io.probeOut_st1.probeStatus === 2.U

  //  ******     mshr::allocate_vec_sub/allocate_vec_main     ******
  /*0:PRIMARY_AVAIL 1:PRIMARY_FULL 2:SECONDARY_AVAIL 3:SECONDARY_FULL*/
  io.missReq.ready := !(mshrStatus_st1 === 1.U || mshrStatus_st1 === 3.U)// || io.missRspIn.valid)
  assert(!io.missReq.valid || (io.missReq.valid && !io.missRspIn.valid),"MSHR的Req和Rsp禁止同时valid，后者优先")
  val real_SRAMAddrUp = Mux(mshrStatus_st1===2.U,OHToUInt(entryMatchProbe_st1),entryStatus.io.next)
  val real_SRAMAddrDown = Mux(mshrStatus_st1===2.U,subentryStatus.io.next,0.U)
  when (io.missReq.fire){
    targetInfo_Accesss(real_SRAMAddrUp)(real_SRAMAddrDown) := io.missReq.bits.targetInfo
  }

  when(io.missReq.fire && mshrStatus_st1 === 0.U) {//PRIMARY_AVAIL
    blockAddr_Access(entryStatus.io.next) := io.missReq.bits.blockAddr
    instrId_Access(entryStatus.io.next) := io.missReq.bits.instrId
  }

  io.probeOut_st1.a_source := real_SRAMAddrUp

  //  ******      mshr::vec_arrange_core_rsp    ******
  // priority: missRspIn > missReq
  //assert(!io.missRspIn.fire || (io.missRspIn.fire && subentryStatus.io.used >= 1.U))
  //This version allow missRspIn fire when no subentry are left
  io.missRspIn.ready := !(subentryStatus.io.used >= 2.U || (subentryStatus.io.used === 1.U && !io.missRspOut.ready))

  entryMatchMissRsp := Reverse(Cat(instrId_Access.map(_ === io.missRspIn.bits.instrId))) & entry_valid
  assert(PopCount(entryMatchMissRsp) <= 1.U,"MSHR missRspIn时，禁止多个entry比对instrId成功")
  val subentry_next2cancel = Wire(UInt(log2Up(NMshrSubEntry).W))
  subentry_next2cancel := subentrySelected.indexWhere(_ === true.B)

  io.missRspOut.bits.targetInfo := targetInfo_Accesss(OHToUInt(entryMatchMissRsp))(subentry_next2cancel)
  io.missRspOut.bits.blockAddr := blockAddr_Access(OHToUInt(entryMatchMissRsp))
  io.missRspOut.bits.instrId := instrId_Access(OHToUInt(entryMatchMissRsp))
  io.missRspOut.valid := io.missRspIn.valid && subentryStatus.io.used >= 1.U//如果上述Access中改出SRAM，本信号需要延迟一个周期

  //  ******     maintain subentries    ******
  /*0:PRIMARY_AVAIL 1:PRIMARY_FULL 2:SECONDARY_AVAIL 3:SECONDARY_FULL*/
  for (iofEn <- 0 until NMshrEntry){
    for (iofSubEn <- 0 until NMshrSubEntry){
      when(iofEn.asUInt===entryStatus.io.next &&
        iofSubEn.asUInt===0.U && io.missReq.fire && mshrStatus_st1 === 0.U){
        subentry_valid(iofEn)(iofSubEn) := true.B
      }.elsewhen(iofEn.asUInt===OHToUInt(entryMatchMissRsp)){
        when(iofSubEn.asUInt===subentry_next2cancel &&
          io.missRspOut.fire){
          subentry_valid(iofEn)(iofSubEn) := false.B
        }.elsewhen(iofSubEn.asUInt===subentryStatus.io.next &&
          io.missReq.fire && mshrStatus_st1 === 2.U){
          subentry_valid(iofEn)(iofSubEn) := true.B
        }
      }//order of when & elsewhen matters, as elsewhen cover some cases of when, but no op to them
    }
  }
}

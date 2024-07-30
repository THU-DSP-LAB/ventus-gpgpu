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
package L1Cache.ShareMem

import L1Cache.L1TagAccess
import SRAMTemplate.SRAMTemplate
import chisel3._
import chisel3.util._
import config.config.Parameters

/*Version Note
* DCacheCoreReq spec changed, shift some work to LSU
* //byteEn
//00 for byte
//01 for half word, alignment required
//11 for word, alignment required
* */

class ShareMemPerLaneAddr(implicit p: Parameters) extends ShareMemBundle{
  val activeMask = Bool()
  val blockOffset = UInt(BlockOffsetBits.W)
  val wordOffset1H = UInt(BytesOfWord.W)
}
class ShareMemCoreReq(implicit p: Parameters) extends ShareMemBundle{
  //val ctrlAddr = new Bundle{
  val instrId = UInt(WIdBits.W)//TODO length unsure
  val isWrite = Bool()//Vec(NLanes, Bool())
  //val tag = UInt(TagBits.W)
  val setIdx = UInt(SetIdxBits.W)
  val perLaneAddr = Vec(NLanes, new ShareMemPerLaneAddr)
  val data = Vec(NLanes, UInt(WordLength.W))
}

class ShareMemCoreRsp(implicit p: Parameters) extends ShareMemBundle{
  val instrId = UInt(WIdBits.W)
  val isWrite = Bool()
  val data = Vec(NLanes, UInt(WordLength.W))
  val activeMask = Vec(NLanes, Bool())//UInt(NLanes.W)
}

class SharedMemory(implicit p: Parameters) extends ShareMemModule{
  val io = IO(new Bundle{
    val coreReq = Flipped(DecoupledIO(new ShareMemCoreReq))
    val coreRsp = DecoupledIO(new ShareMemCoreRsp)
    })

  // ******     important submodules     ******
  val BankConfArb = Module(new BankConflictArbiter)
  //val TagAccess = Module(new L1TagAccess(set=NSets, way=NWays, tagBits=TagBits))
  val DataCorssBarForWrite = Module(new DataCrossbar)
  val DataCorssBarForRead = Module(new DataCrossbar)
  /*val EntryValidArray = RegInit(
    VecInit(Seq.fill(NSets)(
      VecInit(Seq.fill(BlockWords)(
        VecInit(Seq.fill(BytesOfWord)(false.B)))))))*/

  // ******     queues     ******
  val DepthCoreRsp_Q:Int=4
  val coreRsp_Q = Module(new Queue(new ShareMemCoreRsp,entries = DepthCoreRsp_Q,flow=false,pipe=true))
  //this queue also work as a pipeline reg, so cannot flow
  val coreRsp_QAlmstFull = Wire(Bool())

  // ******      Arbiter      ******
  val coreReq_st1 = RegEnable(io.coreReq.bits, io.coreReq.ready)
  BankConfArb.io.coreReqArb.enable := io.coreReq.fire
  BankConfArb.io.coreReqArb.isWrite := Mux(RegNext(BankConfArb.io.bankConflict),coreReq_st1.isWrite,io.coreReq.bits.isWrite)
  BankConfArb.io.coreReqArb.perLaneAddr := io.coreReq.bits.perLaneAddr

  // ******      valid write      ******
  // crossbar switch for perWord addr
  /*val wordIdx1H = Wire(Vec(NLanes,UInt(BlockWords.W)))
  val rawCoreReqPerLaneWordMask1H = Wire(Vec(NLanes,UInt(BytesOfWord.W)))
  (0 until NLanes).foreach { i =>
    wordIdx1H(i) := UIntToOH(io.coreReq.bits.perLaneAddr(i).blockOffset)
    rawCoreReqPerLaneWordMask1H(i) := io.coreReq.bits.perLaneAddr(i).wordOffset1H
  }
  val perWordReq1H = Wire(Vec(BlockWords,UInt(NLanes.W)))
  //注意每个coreReq或者说周期里每个word只能被一个lane访问，多的话违反独热
  //单个coreReq或者说周期里能处理的coalesce请求的范围是一个block
  val activeByteMaskArray = Wire(Vec(BlockWords,UInt(BytesOfWord.W)))
  (0 until BlockWords).foreach{ i =>
    perWordReq1H(i) := Reverse(Cat(wordIdx1H.map(_(i))))
    assert(PopCount(perWordReq1H(i))<=1.U)
    activeByteMaskArray(i) := Mux1H(perWordReq1H(i),rawCoreReqPerLaneWordMask1H)
  }
  // end crossbar switch for perWord addr
  for (iofS <- 0 until NSets) {
    for (iofW <- 0 until BlockWords) { //iofW index of word
      for (iofB <- 0 until BytesOfWord) { //iofB index of byte
        when(io.coreReq.fire && io.coreReq.bits.isWrite &&
          io.coreReq.bits.setIdx === iofS.asUInt) {
          when(activeByteMaskArray(iofW)(iofB)) {
            EntryValidArray(iofS)(iofW)(iofB) := true.B
          }
        }
        when(io.coreReq.fire && !io.coreReq.bits.isWrite &&
          io.coreReq.bits.setIdx === iofS.asUInt) {
          when(activeByteMaskArray(iofW)(iofB)) {
            assert(EntryValidArray(iofS)(iofW)(iofB) === true.B)
          }
        }
      }
    }
  }*/

  // ******     pipeline regs      ******
  val coreReqisValidWrite_st1 = RegInit(false.B)
  coreReqisValidWrite_st1 := (io.coreReq.fire && io.coreReq.bits.isWrite) || (coreReqisValidWrite_st1 && RegNext(BankConfArb.io.bankConflict))
  val coreReqisValidRead_st1  = RegInit(false.B)
  coreReqisValidRead_st1 := (io.coreReq.fire && !io.coreReq.bits.isWrite) || (coreReqisValidRead_st1 && RegNext(BankConfArb.io.bankConflict))//这个信号不是给Data Array用的哈
  val coreReqisValidRead_st2 = RegNext(coreReqisValidRead_st1)//TODO verification for bank conflict
  val coreReqisValidWrite_st2 = RegNext(coreReqisValidWrite_st1)

  val coreReqInstrId_st2 = RegNext(coreReq_st1.instrId)
  val coreReqActvMask_st2 = ShiftRegister(BankConfArb.io.activeLane,2)
  val coreReqIsWrite_st2 = RegNext(coreReq_st1.isWrite)

  val arbAddrCrsbarOut_st1 = RegNext(BankConfArb.io.addrCrsbarOut)
  val arbDataCrsbarSel1H_st1 = RegNext(BankConfArb.io.dataCrsbarSel1H)
  val arbDataCrsbarSel1H_st2 = RegNext(arbDataCrsbarSel1H_st1)

  val bankConflictHolding = Bool()
  // ******     DataAccess      ******
  //值得注意的是，当读写请求同时来临时，如果读写地址相同，读不应该直接传递写的内容，而是返回旧的内容
  //这是因为流水线设计里，读请求类型中访问Data array比写类型请求滞后一个流水级
  //因此读写请求同时发生的话，读请求肯定比写请求早一个周期进入cache
  val DataAccessesRRsp = (0 until NBanks).map {i =>
    val DataAccess = Module(new SRAMTemplate(
      gen=UInt(8.W),
      set=NSets*NWays*(BankWords),
      way=BytesOfWord,
      shouldReset = false,
      holdRead = false,
      singlePort = false
    ))
    DataAccess.io.w.req.valid := coreReqisValidWrite_st1
    DataAccess.io.w.req.bits.data := DataCorssBarForWrite.io.DataOut(i).asTypeOf(Vec(BytesOfWord,UInt(8.W)))
    //this setIdx = setIdx + wayIdx + bankOffset
    if(BlockOffsetBits-BankIdxBits>0) {
      DataAccess.io.w.req.bits.setIdx := Cat(coreReq_st1.setIdx,arbAddrCrsbarOut_st1(i).bankOffset.getOrElse(false.B))
    } else {
      DataAccess.io.w.req.bits.setIdx := coreReq_st1.setIdx
    }
    DataAccess.io.w.req.bits.waymask.foreach(_ :=
      arbAddrCrsbarOut_st1(i).wordOffset1H)

    DataAccess.io.r.req.valid := io.coreReq.fire && !io.coreReq.bits.isWrite
    if(BlockOffsetBits-BankIdxBits>0)
      DataAccess.io.r.req.bits.setIdx := Cat(
      io.coreReq.bits.setIdx,//setIdx
      BankConfArb.io.addrCrsbarOut(i).bankOffset.getOrElse(false.B))//bankOffset
    else DataAccess.io.r.req.bits.setIdx := io.coreReq.bits.setIdx
    Cat(DataAccess.io.r.resp.data.reverse)
  }
  val dataAccess_data_st2 = RegEnable(VecInit(DataAccessesRRsp),coreReqisValidRead_st1)

  // ******      data crossbar for write     ******
  DataCorssBarForWrite.io.DataIn := coreReq_st1.data
  DataCorssBarForWrite.io.Select1H := arbDataCrsbarSel1H_st1
  // ******      data crossbar for read     ******
  DataCorssBarForRead.io.DataIn := dataAccess_data_st2
  DataCorssBarForRead.io.Select1H := arbDataCrsbarSel1H_st2

  // ******      core rsp
  coreRsp_Q.io.deq <> io.coreRsp
  coreRsp_Q.io.enq.valid := coreReqisValidRead_st2 || coreReqisValidWrite_st2
  coreRsp_Q.io.enq.bits.isWrite := coreReqIsWrite_st2
  coreRsp_Q.io.enq.bits.data := DataCorssBarForRead.io.DataOut
  coreRsp_Q.io.enq.bits.instrId := coreReqInstrId_st2
  coreRsp_Q.io.enq.bits.activeMask := coreReqActvMask_st2
  coreRsp_QAlmstFull := coreRsp_Q.io.count === DepthCoreRsp_Q.asUInt - 2.U

  // ******      core req ready
  //coreReq_ok_to_in := MshrAccess.io.missReq.ready && !missRspFromMshr_st2 && !io.memRsp.valid && coreRsp_Q.io.enq.ready && !Arbiter.io.bankConflict
  io.coreReq.ready := !RegNext(BankConfArb.io.bankConflict) && !coreRsp_QAlmstFull && !coreReqisValidWrite_st1
}
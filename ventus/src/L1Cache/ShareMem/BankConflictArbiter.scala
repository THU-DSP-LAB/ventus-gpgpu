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

import chisel3._
import chisel3.util._
import config.config.Parameters

/*Version Note

* This version plan to implement support of
* merging non-conflict byte WRITE req from different Lane to same bank,
* but finally fail to achieve, means this req have to be arbitrated as
* conflict, thus conduct in 2 cycles.
*
* This version cant merge READ req to the same exact addr, these identical
* reqs would pitifully be split into multiple cycles
*
* This version suppose NLanes = NBanks, to change this
* DataCrossbar can be used as bidirectional
* modify assignments of bankOffset & perBankReqCount,
* modify ConflictBankReq_w
*/

class ByteEn1HConvertor(BytesOfWord:Int=4) extends Module{
  //wordOffset one-hot converter, based on Byte Enable signal and wordOffset
  def WordOffsetBits = log2Up(BytesOfWord)
  val io=IO(new Bundle{
    val wordOffset = Input(UInt(WordOffsetBits.W))
    val byteEn = Input(UInt(2.W))
    val wordOffset1H = Output(UInt(BytesOfWord.W))
    val alignmentError = Output(Bool())
  })
  io.alignmentError := true.B
  io.wordOffset1H := Fill(BytesOfWord,"b1".U)
  switch(io.byteEn){
    is(0.U){
      io.alignmentError := false.B
      io.wordOffset1H := UIntToOH(io.wordOffset)
    }
    is(1.U){
      io.alignmentError := io.wordOffset(0)// === true.B
      io.wordOffset1H := Cat(Fill(BytesOfWord/2,io.wordOffset(1)),Fill(BytesOfWord/2,!io.wordOffset(1)))
    }
    is(3.U){
      io.alignmentError := io.wordOffset.orR
      io.wordOffset1H := Fill(BytesOfWord,true.B)
    }
  }
}

class DataCrossbar(implicit p: Parameters) extends ShareMemModule {
  val io=IO(new Bundle {
    val DataIn = Input(Vec(NBanks, UInt(WordLength.W)))
    val DataOut = Output(Vec(NLanes, UInt(WordLength.W)))
    val Select1H = Input(Vec(NLanes, UInt(NBanks.W)))
  })
  (0 until NLanes).foreach{ iofL =>
      io.DataOut(iofL):= Mux1H(io.Select1H(iofL), io.DataIn)
  }
}

class AddrBundle1T(implicit p: Parameters) extends ShareMemBundle {
  val bankOffset = if(BlockOffsetBits-BankIdxBits>0) Some(UInt((BlockOffsetBits-BankIdxBits).W)) else None
  val wordOffset1H = UInt(BytesOfWord.W)
}
object L2BCrossbar {
  def apply[T <: Data](NLaneNBank: Int, sel: Vec[UInt], in: Vec[T]):
  Vec[T] = VecInit((0 until NLaneNBank).map(iofBk => Mux1H(sel(iofBk), in)))
}
/*class AddrCrossbar(implicit p: Parameters) extends DCacheModule {
  val io=IO(new Bundle {
    val AddrIn = Input(Vec(NLanes, new AddrBundle1T))
    val AddrOut = Output(Vec(NBanks, new AddrBundle1T))
    val Select1H = Input(Vec(NBanks, UInt(NLanes.W)))
  })
  (0 until NBanks).foreach{ iofBk =>
    io.AddrOut(iofBk).bankOffset := Mux1H(io.Select1H(iofBk),io.AddrIn.map(_.bankOffset))
    io.AddrOut(iofBk).wordOffset1H := Mux1H(io.Select1H(iofBk),io.AddrIn.map(_.wordOffset1H))
  }
}*/

class coreReqArb1T (implicit p:Parameters) extends ShareMemBundle{
  val activeMask = Bool()
  val bankIdx = UInt(BankIdxBits.W)
  val AddrBundle = new AddrBundle1T
}
class coreReqArb(implicit p:Parameters) extends ShareMemBundle{
  val isWrite = Bool()
  val enable = Bool()
  val perLaneAddr = Vec(NLanes,new ShareMemPerLaneAddr)//OT = One Thread
}

class BankConflictArbiter(implicit p: Parameters) extends ShareMemModule{
  val io = IO(new Bundle{
    val coreReqArb = Input(new coreReqArb)

    val dataCrsbarSel1H = Output(Vec(NBanks, UInt(NBanks.W)))
    val addrCrsbarOut = Output(Vec(NBanks, new AddrBundle1T))
    val dataArrayEn = Output(Vec(NBanks, Bool()))
    val activeLane = Output(Vec(NLanes,Bool()))

    val bankConflict = Output(Bool())
  })
  //announcement
  val bankConflict = Wire(Bool())
  val bankConflict_reg = RegNext(bankConflict,false.B)
  val conflictReqIsW_reg = Reg(Bool())
  val perLaneReq = Wire(Vec(NLanes, new coreReqArb1T))
  val perLaneConflictReq_reg = Reg(Vec(NLanes, new coreReqArb1T))
  val perLaneConflictReq = Wire(Vec(NLanes, new coreReqArb1T))
  (0 until NLanes).foreach{i =>
    perLaneReq(i).activeMask := io.coreReqArb.perLaneAddr(i).activeMask
    perLaneReq(i).bankIdx := io.coreReqArb.perLaneAddr(i).blockOffset(BankIdxBits-1,0)
    if(BlockOffsetBits>BankIdxBits){
      perLaneReq(i).AddrBundle.bankOffset.foreach(_:= io.coreReqArb.perLaneAddr(i).blockOffset(BlockOffsetBits-1,BankIdxBits))}
    else{
      //perLaneReq(i).AddrBundle.bankOffset :=Wire(UInt())
    }
    perLaneReq(i).AddrBundle.wordOffset1H := io.coreReqArb.perLaneAddr(i).wordOffset1H
  }

  //**************detect bank conflict**************
  val bankIdx = Wire(Vec(NLanes, UInt(BankIdxBits.W)))
  val isWrite: Bool = Mux(bankConflict_reg,conflictReqIsW_reg,io.coreReqArb.isWrite)
  val laneActiveMask = Wire(Vec(NLanes, Bool()))
  val bankIdx1H = Wire(Vec(NLanes, UInt(NBanks.W)))
  val bankIdxMasked = Wire(Vec(NLanes, UInt(NBanks.W)))
  (0 until NLanes).foreach{ i =>
    laneActiveMask(i) := perLaneConflictReq(i).activeMask
    bankIdx(i) := perLaneConflictReq(i).bankIdx
    bankIdx1H(i) := UIntToOH(bankIdx(i))
    bankIdxMasked(i) := bankIdx1H(i) & Fill(NLanes, laneActiveMask(i))}
  val perBankReq_Bin = Wire(Vec(NBanks, UInt(NLanes.W)))//transpose of bankIdxMasked
  val perBankReqCount = Wire(Vec(NBanks, UInt((log2Up(NLanes)+1).W)))
  (0 until NBanks).foreach{ i =>
    perBankReq_Bin(i) := Reverse(Cat(bankIdxMasked.map(_(i))))
    perBankReqCount(i) := PopCount(perBankReq_Bin(i))}
  /*
  *  perBankReq1H(j)
  *    |
  * 0 |1| 0  0  //bankIdx1H(i)
  *    +
  * 1 |0| 0  0  //bankIdx1H(i+1)
  *    =
  *    1 //perBankReqCount(i)
  */
  val perBankReqConflict = Wire(Vec(NBanks, Bool()))
  perBankReqConflict := perBankReqCount.map(_>1.U)
  bankConflict := Cat(perBankReqConflict).orR && (io.coreReqArb.enable || bankConflict_reg)
  //TODO is there a better logic detecting bankConflict?

  //**************reserve conflict req**************
  val perBankActiveLaneWhenConflict1H = Wire(Vec(NBanks, UInt(NLanes.W)))
  perBankActiveLaneWhenConflict1H := perBankReq_Bin.map(Cat(_)).map(PriorityEncoderOH(_))
  val ActiveLaneWhenConflict1H = Wire(Vec(NLanes, Bool()))
  (0 until NBanks).foreach{ i =>
    ActiveLaneWhenConflict1H(i) := Cat(perBankActiveLaneWhenConflict1H.map(_(i))).orR}
  val ReserveLaneWhenConflict1H = VecInit(((~Cat(ActiveLaneWhenConflict1H)).asUInt & Cat(laneActiveMask)).asBools.reverse)

  perLaneConflictReq := Mux(bankConflict_reg,perLaneConflictReq_reg,perLaneReq)
  //to determine current arb source: during a conflict, use Reg, otherwise use Input
  (0 until NLanes).foreach{ i =>
    when(ReserveLaneWhenConflict1H(i)){
      perLaneConflictReq_reg(i).bankIdx := perLaneConflictReq(i).bankIdx
      perLaneConflictReq_reg(i).AddrBundle := perLaneConflictReq(i).AddrBundle}
    perLaneConflictReq_reg(i).activeMask := ReserveLaneWhenConflict1H(i)
  }
  when(bankConflict){conflictReqIsW_reg := io.coreReqArb.isWrite}

  //**************output to AddrCrossbar**************
  io.addrCrsbarOut := L2BCrossbar(NBanks,perBankActiveLaneWhenConflict1H,VecInit(perLaneConflictReq.map(_.AddrBundle)))

  //**************output to DataCrossbar**************
  //do not merge req to same addr from different lane. assume this optimization to be done at register level
  io.dataCrsbarSel1H := Mux(isWrite,perBankActiveLaneWhenConflict1H,bankIdxMasked)

  //**************output to DataArray**************
  io.dataArrayEn := perBankActiveLaneWhenConflict1H.map(_.orR)
  io.bankConflict := bankConflict//to pull down coreReq.ready

  //**************output to coreRsp****************
  io.activeLane := ActiveLaneWhenConflict1H
}

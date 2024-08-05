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
package L1Cache.AtomicUnit

import L1Cache.DCache.{DCacheModule, DCacheParamsKey}
import L1Cache.{HasL1CacheParameters, L1CacheModule, RVGModule}
import L2cache.{InclusiveCacheParameters_lite, TLBundleA_lite, TLBundleD_lite_plus}
import chisel3._
import chisel3.util._
import config.config.Parameters
import top.parameters.{addrLen, num_thread, num_warp}

import scala.collection.immutable

/*resOpcode:
  3.U: LR success, there isn't conflict inflight write, can put LR in table
  1.U: isSC, is SC operation, restable need to check and clear, give output of whether LR exist,and clear the entry
  2.U: isWrite, is Write Operation, restable need to clear whether a LR exist
  */

// reservation table for LR/SC
class ResTable (implicit p:Parameters)extends DCacheModule{
  val io = IO(new Bundle{
    val checkAddr = Input(UInt(WordLength.W))
    val resOpcode = Input(UInt(2.W))
    val SCSuccess = Output(Bool())
  })

  val resTable = Mem(NResTabEntry,UInt(WordLength.W))
  val resTableValid = RegInit(VecInit(Seq.fill(NResTabEntry)(false.B)))
  val wrPtr = RegInit(0.U((log2Ceil(NResTabEntry)).W))
  val isMatch = Wire(Vec(NResTabEntry,Bool()))
  val matchEntry = Wire(UInt((log2Ceil(NResTabEntry)).W))
  val wrPtrn = Wire(UInt((log2Ceil(NResTabEntry)).W))
  val PtrGen = Module(new nxtPtrGen(NResTabEntry))
  PtrGen.io.validEntry := resTableValid
  PtrGen.io.curPtr := wrPtr
  wrPtrn := PtrGen.io.nxtPtr
  matchEntry := OHToUInt(isMatch)

  val i = 0
  for(i <- 0 until NResTabEntry){
    isMatch(i) := (io.checkAddr === resTable(i)) && resTableValid(i)
  }
  when(io.resOpcode === 3.U){
    resTable(wrPtr) := io.checkAddr
    resTableValid(wrPtr) := true.B
    wrPtr := wrPtrn
  }.elsewhen(io.resOpcode === 1.U || io.resOpcode ===2.U){
    resTableValid(matchEntry) := false.B
    wrPtr := wrPtr
  }
  io.SCSuccess := isMatch.reduce(_ | _) && (io.resOpcode === 1.U)
}

class nxtPtrGen (depth_tab: Int) extends Module{
  val io = IO(new Bundle() {
    val validEntry = Input(Vec(depth_tab,Bool()))
    val curPtr  = Input(UInt((log2Ceil(depth_tab)).W))
    val nxtPtr = Output(UInt((log2Ceil(depth_tab)).W))
  })
  val validEntryw = Wire(UInt(depth_tab.W))
  validEntryw := ~(io.validEntry.asUInt | UIntToOH(io.curPtr))
  io.nxtPtr := PriorityEncoder(validEntryw)
}

class inFlightWrite(implicit p:Parameters) extends DCacheModule{
  val io = IO(new Bundle() {
    val setEntry = Flipped(Valid(UInt(WordLength.W)))
    val checkEntry = Flipped(Valid(UInt(WordLength.W)))
    val remvEntry = Flipped(Valid(UInt(WordLength.W)))
    val conflict = Output(Bool())
  })
  val infTab = Mem(NInfWriteEntry, UInt(WordLength.W))
  val infTabValid = RegInit(VecInit(Seq.fill(NInfWriteEntry)(false.B)))
  val wrPtr = RegInit(0.U((log2Ceil(NInfWriteEntry)).W))
  val isMatch = Wire(Vec(NInfWriteEntry, Bool()))
  val remvMatch = Wire(Vec(NInfWriteEntry, Bool()))
  val remvEntry = Wire(UInt((log2Ceil(NResTabEntry)).W))
  val wrPtrn = Wire(UInt((log2Ceil(NInfWriteEntry)).W))
  val PtrGen = Module(new nxtPtrGen(NInfWriteEntry))
  PtrGen.io.validEntry := infTabValid
  PtrGen.io.curPtr := wrPtr
  wrPtrn := PtrGen.io.nxtPtr
  remvEntry := OHToUInt(remvMatch)
  val i = 0
  for (i <- 0 until NResTabEntry) {
    isMatch(i) := (io.checkEntry.bits === infTab(i)) && infTabValid(i)
    remvMatch(i) := (io.remvEntry.bits === infTab(i)) && infTabValid(i)
  }
  io.conflict := io.checkEntry.valid && isMatch.reduce(_ | _)
  when(io.setEntry.valid){
    infTab(wrPtr) := io.setEntry.bits
    infTabValid(wrPtr) := true.B
    wrPtr := wrPtrn
  }
  when(io.remvEntry.valid){
    when(remvMatch.reduce(_|_)){
      infTabValid(remvEntry) := false.B
      wrPtr := wrPtr
    }
  }
}

object atomicALUCtl {
  def min = 0.U(4.W)
  def max = 1.U(4.W)
  def minu = 2.U(4.W)
  def maxu = 3.U(4.W)
  def add = 4.U(4.W)
  def xor = 8.U(4.W)
  def or = 9.U(4.W)
  def and = 10.U(4.W)
  def swap = 11.U(4.W)
}
class AtomicCtrl (params : InclusiveCacheParameters_lite)(implicit p: Parameters) extends RVGModule{
  val io = IO(new Bundle() {
    val isAtomic = Input(Bool())
    val L12ATUmemReq = Flipped(Decoupled(new TLBundleA_lite(params)))
    val L22ATUmemRsp = Flipped(Decoupled(new TLBundleD_lite_plus(params)))
    val ATU2L2memReq = Decoupled(new TLBundleA_lite(params))
    val ATU2L1memRsp = Decoupled(new TLBundleD_lite_plus(params))
  })
  val idle :: issueGet :: issuePut :: issueAAD :: Nil = Enum(4)

  val state = RegInit(idle)
  val oprandA = Wire(UInt(WordLength.W))
  val oprandB = Wire(UInt(WordLength.W))
  val ALUresult = Wire(UInt(WordLength.W))
  val opcode = Wire(UInt(4.W)) // 3 for param, 1 for Arith or Logic
  val a_data = Wire(Vec(BlockWords, UInt(WordLength.W)))
  // store the req message for future use, will be some information that is not useful or overlap with other part ?
  val memReqBuf = Module(new Queue(new TLBundleA_lite(params),1))
  val takePipeOp = Module(new maskdata(BlockWords,WordLength))
  val takeMemOp = Module(new maskdata(BlockWords,WordLength))

  memReqBuf.io.enq.valid := io.isAtomic && io.L12ATUmemReq.valid
  memReqBuf.io.enq.bits := io.L12ATUmemReq.bits
  io.L12ATUmemReq.ready := state === idle
  memReqBuf.io.deq.ready := state === issueAAD && io.ATU2L1memRsp.fire
  takePipeOp.io.din := memReqBuf.io.deq.bits.data
  takePipeOp.io.mask := memReqBuf.io.deq.bits.mask
  oprandA := takePipeOp.io.dout
  takeMemOp.io.din := io.L22ATUmemRsp.bits.data
  takeMemOp.io.mask := memReqBuf.io.deq.bits.mask
  oprandB := takeMemOp.io.dout

  //opcode := Cat(memReqBuf.io.deq.bits.opcode === 2.U,memReqBuf.io.deq.bits.param)

  /*
  idle state - wait for the control unit decode and issue atomic ctrl module
  issueGet - Arith or logical has been translate to TLUL and issue req to L2C, waiting for resp data
  issuePut - use resp data and data store in AC and calculate the data to put back to L2C
  issueADD - L2C resp AA, means the PutPartialData op succeed
   */
  switch(state){
    is(idle){
      when(io.isAtomic){
        state := issueGet
      }.otherwise{
        state := idle
      }
    }
    is(issueGet){
      when(io.L22ATUmemRsp.valid && (io.L22ATUmemRsp.bits.address === memReqBuf.io.deq.bits.address)){
        state := issuePut
      }.otherwise{
        state := issueGet
      }
    }
    is(issuePut){
      when(io.L22ATUmemRsp.valid && (io.L22ATUmemRsp.bits.address === memReqBuf.io.deq.bits.address)) {
        state := issueAAD
      }.otherwise{
        state := issuePut
      }
    }
    is(issueAAD){
      when(io.ATU2L1memRsp.ready){
        state := idle
      }.otherwise{
        state := issueAAD
      }
    }
  }
}

class maskdata (width:Int, length:Int) extends Module{
  val io = IO(new Bundle() {
    val din = Input(UInt((width*length).W))
    val mask = Input(UInt(width.W))
    val dout = Output(UInt(length.W))
  })
  val a_data = Wire(Vec(width, UInt(length.W)))
  var i = 0
  for (i <- 0 until width) {
    a_data(i) := Mux(io.mask(i), io.din(i * length + length - 1, i * length), 0.U)
  }
  io.dout := a_data.reduce(_ | _)
}

class putdata (width : Int, length : Int) extends Module{
  val io =  IO(new Bundle() {
    val din = Input(UInt(length.W))
    val mask = Input(UInt(width.W))
    val dout = Output(UInt((width*length).W))
  })
  var i = 0
  for (i<-0 until width){
    io.dout(i * length + length - 1, i * length) := Mux(io.mask(i),io.din,0.U)
  }
}










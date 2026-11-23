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

import L1Cache.DCache.{DCacheModule, DCacheParamsKey,HasDCacheParameter, DCacheBundle}
import L1Cache.{HasL1CacheParameters, L1CacheModule, RVGModule}
import L2cache.{InclusiveCacheParameters_lite, TLBundleA_lite, TLBundleD_lite_plus, TLBundleA_lite_custom, TLBundleD_lite_plus_custom}
import chisel3._
import chisel3.util._
import config.config.Parameters
import top.parameters._

import scala.collection.immutable
import L1Cache.ReplacementUnit_ICache
import L2cache.InclusiveCacheParameters_lite_custom
import L1Cache.DCache.HasDCacheParameter
import MemboxS.SimpleTest.x

/*resOpcode:
  3.U: LR success, there isn't conflict inflight write, can put LR in table
  1.U: isSC, is SC operation, restable need to check and clear, give output of whether LR exist,and clear the entry
  2.U: isWrite, is Write Operation, restable need to clear whether a LR exist
  */

  class ReqAddr extends Bundle{
    val a_addr = UInt(memReqLen.W)
    val a_mask = Vec(dcache_BlockWords, UInt(BytesOfWord.W))
  }

  class AtomicReq(implicit p:Parameters) extends DCacheBundle{
    val a_addr = UInt(memReqLen.W)
    val a_mask = Vec(dcache_BlockWords, UInt(BytesOfWord.W))
    val a_data = UInt(xLen.W)
    val a_opcode = UInt(3.W)
    val a_param = UInt(3.W)

  def computeAtomicResult(newData: UInt): UInt = {
    val result = Wire(UInt(xLen.W))
    result := a_data
    when(a_opcode === TLAOp_Arith) {
      when(a_param === TLAParam_ArithAdd) { result := a_data + newData 
      }.elsewhen(a_param === TLAParam_ArithMin) { result := Mux(a_data.asSInt < newData.asSInt, a_data.asSInt, newData.asSInt).asUInt 
      }.elsewhen(a_param === TLAParam_ArithMax) { result := Mux(a_data.asSInt > newData.asSInt, a_data.asSInt, newData.asSInt).asUInt 
      }.elsewhen(a_param === TLAParam_ArithMinu) { result := Mux(a_data < newData, a_data, newData) 
      }.elsewhen(a_param === TLAParam_ArithMaxu) { result := Mux(a_data > newData, a_data, newData) }
      
    }.elsewhen(a_opcode === TLAOp_Logic) {
      when(a_param === TLAParam_LogicAnd ) { result := a_data & newData 
      }.elsewhen(a_param === TLAParam_LogicOr) { result := a_data | newData 
      }.elsewhen(a_param === TLAParam_LogicXor) { result := a_data ^ newData 
      }.elsewhen(a_param === TLAParam_LogicSwap) { result := newData }
    }.otherwise {
      result := a_data
    }
    
    result
  }
}


// reservation table for LR/SC
class ResTable (implicit p:Parameters)extends DCacheModule{
  val io = IO(new Bundle{
    val checkAddr = Flipped(Valid(new ReqAddr))
    val resOpcode = Input(UInt(2.W))
    val SCSuccess = Output(Bool())
  })

  val resTable = Mem(NResTabEntry,UInt(memReqLen.W))
  val resTableMask = RegInit(VecInit(Seq.fill(NResTabEntry)(0.U(dcache_BlockWords.W))))
  val ReqMaskPerWord = VecInit(Seq.fill(dcache_BlockWords)(false.B))
  val resTableValid = WireInit(VecInit(Seq.fill(NResTabEntry)(false.B)))
  val wrPtr = RegInit(0.U((log2Ceil(NResTabEntry)).W))
  val blockAddrMatch = Wire(Vec(NResTabEntry,Bool())) // block address match, will check the mask(SC req), will merge req (LR req), will clear the whole entry(write or AMO)
  val isoverlapMask = Wire(Bool())
  val matchEntry = Wire(UInt((log2Ceil(NResTabEntry)).W))  
  val wrPtrn = Wire(UInt((log2Ceil(NResTabEntry)).W))
  val PtrGen = Module(new nxtPtrGen(NResTabEntry))
  resTableValid := resTableMask.map(_.orR)
  PtrGen.io.validEntry := resTableValid
  PtrGen.io.curPtr := wrPtr
  wrPtrn := PtrGen.io.nxtPtr
  matchEntry := OHToUInt(blockAddrMatch)
  ReqMaskPerWord := io.checkAddr.bits.a_mask.map(_.orR) // Mask for each word
  // entry in restab is like: clear Op -> clear the mask bit, set Op -> set addr and mask, check N clear Op -> check addr and mask, clear the mask bit
  // +----------+---+---+---+----+------+
  // | blockAddr|m0 |m1 |m2 |... |valid |
  // +----------+---+---+---+----+------+

  val i = 0
  for(i <- 0 until NResTabEntry){
    blockAddrMatch(i) := (io.checkAddr.bits.a_addr === resTable(i)) && resTableValid(i)
  }
  isoverlapMask := (ReqMaskPerWord.asUInt & resTableMask(matchEntry)).orR
  when(io.resOpcode === 3.U && io.checkAddr.valid){ // LR
    when(blockAddrMatch.reduce(_|_)){
      resTableMask(matchEntry) := ReqMaskPerWord.asUInt | resTableMask(matchEntry) // merge the mask
      wrPtr := wrPtr
    }.otherwise{
      resTable(wrPtr) := io.checkAddr.bits.a_addr
      resTableMask(wrPtr) := ReqMaskPerWord.asUInt
      wrPtr := wrPtrn
    }
  }.elsewhen((io.resOpcode === 1.U || io.resOpcode === 2.U) && io.checkAddr.valid){ // SC
    when(blockAddrMatch.reduce(_|_)){
      resTableMask(matchEntry) := ~ReqMaskPerWord.asUInt & resTableMask(matchEntry) // clear the mask
      wrPtr := wrPtr
    }.otherwise{
      wrPtr := wrPtr
    }
  }
  io.SCSuccess := RegNext(isoverlapMask && (io.resOpcode === 1.U)) || (isoverlapMask && (io.resOpcode === 1.U))
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
    val setEntry = Flipped(Valid(new ReqAddr)) //write req
    val setEntryisSC = Input(Bool())
    val SetIdx   = Output(UInt(InfWriteEntryBits.W))
    val checkEntry = Input(new ReqAddr) //LR req
    val remvEntry = Flipped(Valid(UInt(InfWriteEntryBits.W))) // write rsp
    val remvEntryisSC = Output(Bool())
    val conflict = Output(Bool())
    val full = Output(Bool())
  })
  val infTab = Mem(NInfWriteEntry, UInt(memReqLen.W))
  val infTabValid = RegInit(VecInit(Seq.fill(NInfWriteEntry)(false.B)))
  val infTabisSC = RegInit(VecInit(Seq.fill(NInfWriteEntry)(false.B)))
  val wrPtr = RegInit(0.U(InfWriteEntryBits.W))
  val isMatch = Wire(Vec(NInfWriteEntry, Bool()))
  val wrPtrn = Wire(UInt((log2Ceil(NInfWriteEntry)).W))
  val PtrGen = Module(new nxtPtrGen(NInfWriteEntry))
  PtrGen.io.validEntry := infTabValid
  PtrGen.io.curPtr := wrPtr
  wrPtrn := PtrGen.io.nxtPtr
  io.full := infTabValid.reduce(_ & _)
  val i = 0
  for (i <- 0 until NInfWriteEntry) {
    isMatch(i) := (io.checkEntry.a_addr === infTab(i)) && infTabValid(i)
  }
  io.conflict := isMatch.reduce(_ | _)
  when(io.setEntry.valid){
    infTab(wrPtr) := io.setEntry.bits.a_addr
    infTabValid(wrPtr) := true.B
    infTabisSC(wrPtr) := io.setEntryisSC
    wrPtr := wrPtrn
  }
  when(io.remvEntry.valid){
      infTabValid(io.remvEntry.bits) := false.B
      wrPtr := wrPtr
  }
  io.remvEntryisSC := infTabisSC(io.remvEntry.bits) && infTabValid(io.remvEntry.bits)
  io.SetIdx := wrPtr
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
class AtomicUnit (params_in : InclusiveCacheParameters_lite)(implicit p: Parameters) extends RVGModule with HasDCacheParameter{
  val io = IO(new Bundle() {
    val L12ATUmemReq = Flipped(Decoupled(new TLBundleA_lite(params_in)))
    val L22ATUmemRsp = Flipped(Decoupled(new TLBundleD_lite_plus_custom(params_in)))
    val ATU2L2memReq = Decoupled(new TLBundleA_lite_custom(params_in))
    val ATU2L1memRsp = Decoupled(new TLBundleD_lite_plus(params_in))
  })
  val memReq_s_ready = Wire(Bool()) // READY for L1 req
  val memReq_m_valid = Wire(Bool()) // VALID for L2 req
  val memRsp_s_ready = Wire(Bool()) 
  val memRsp_m_valid = Wire(Bool()) 
  val SCFailRsp_valid = Wire(Bool())
  val memRsp_m = Wire(new TLBundleD_lite_plus(params_in))
  val SCFailRsp = Wire(new TLBundleD_lite_plus(params_in))
  val memReq_m = Wire(new TLBundleA_lite_custom(params_in))
  val idle :: issueGet :: issuePut :: Nil = Enum(3)
  val state = RegInit(idle)
  val nextState = Wire(UInt(2.W))
  // modules
  val InfTab = Module(new inFlightWrite)
  val ResTab = Module(new ResTable)

  val LRhitInfWrite = Wire(Bool())
  val ReqhitAtomicReq = Wire(Bool())
  val a_source = Wire(UInt(params_in.source_bits_custom.W))
  val a_source_up = Wire(UInt((params_in.source_bits_custom-params_in.source_bits).W))
  val resTabAddr = Wire(UInt(memReqLen.W))
  val resTabMask = Wire(Vec(dcache_BlockWords, UInt(BytesOfWord.W)))
  val resTabOpcode = Wire(UInt(2.W))
  val resTabValid = Wire(Bool())

  val InfWriteSetValid = Wire(Bool())
  val InfWriteRmvValid = Wire(Bool())
  val atomicReqValid = Wire(Bool())
  val memRspAtomicReady = Wire(Bool())
  val atomicReq = Reg(new AtomicReq)
  val atomicL2Req = Wire(new TLBundleA_lite_custom(params_in))
  val atomicRspValid = Wire(Bool())
  val atomicL1Rsp = Wire(new TLBundleD_lite_plus(params_in))
  // mask datas for atomic operation
  val atomicDataInMask = Module(new maskdata(dcache_BlockWords, xLen, BytesOfWord))
  atomicDataInMask.io.din := io.L12ATUmemReq.bits.data
  atomicDataInMask.io.mask := io.L12ATUmemReq.bits.mask
  val L2DataInMask = Module(new maskdata(dcache_BlockWords, xLen, BytesOfWord))
  L2DataInMask.io.din := io.L22ATUmemRsp.bits.data
  L2DataInMask.io.mask := atomicL2Req.mask
  val atomicDataOutMask = Module(new putdata(dcache_BlockWords, xLen,BytesOfWord))
  val atomicOpResult = atomicReq.computeAtomicResult(L2DataInMask.io.dout)
  atomicDataOutMask.io.din := atomicOpResult
  atomicDataOutMask.io.mask := atomicReq.a_mask.asUInt
  val atomicOpResultDataPut = atomicDataOutMask.io.dout

  // functions
  def isAtomicRequest(req: TLBundleA_lite): Bool = {
    (req.opcode === TLAOp_Arith) ||  // Arithmetic AMO
    (req.opcode === TLAOp_Logic)     // Logical AMO
  }
  def isAtomicResponse(resp: TLBundleD_lite_plus_custom): Bool = {
    (resp.source.head(2) === "b11".U)
  }

  a_source_up := 0.U
  a_source := Cat(a_source_up,io.L12ATUmemReq.bits.source) // cat source_up to the MSB bits of source
 resTabValid := false.B

  memRsp_m := io.L22ATUmemRsp.bits
  SCFailRsp := io.L22ATUmemRsp.bits
  InfWriteSetValid := false.B
  InfWriteRmvValid := false.B
  // ----- L2 req connection -----
  io.ATU2L2memReq.valid := memReq_m_valid || atomicReqValid
  io.ATU2L2memReq.bits := Mux(atomicReqValid, atomicL2Req, memReq_m)
  // ----- L1 req connection -----
  io.L12ATUmemReq.ready := memReq_s_ready
  // ----- L1 rsp connection -----
  io.ATU2L1memRsp.valid := memRsp_m_valid || SCFailRsp_valid || atomicRspValid
  io.ATU2L1memRsp.bits := Mux(atomicRspValid, atomicL1Rsp, Mux(memRsp_m_valid, memRsp_m, SCFailRsp))// atomic rsp has the highest priority
  // ----- L2 rsp ready connection -----
  io.L22ATUmemRsp.ready := memRsp_s_ready
  // ----- inftab connection -----
  LRhitInfWrite := InfTab.io.conflict
  ReqhitAtomicReq := (io.L12ATUmemReq.bits.address === atomicReq.a_addr) && (state =/= idle)
  InfTab.io.checkEntry.a_mask := io.L12ATUmemReq.bits.mask.asTypeOf(Vec(dcache_BlockWords, UInt(BytesOfWord.W)))
  InfTab.io.checkEntry.a_addr := io.L12ATUmemReq.bits.address
  InfTab.io.setEntry.bits.a_addr := io.L12ATUmemReq.bits.address
  InfTab.io.setEntry.bits.a_mask := io.L12ATUmemReq.bits.mask.asTypeOf(Vec(dcache_BlockWords, UInt(BytesOfWord.W)))
  InfTab.io.setEntry.valid := InfWriteSetValid
  InfTab.io.remvEntry.valid := InfWriteRmvValid
  InfTab.io.remvEntry.bits := io.L22ATUmemRsp.bits.source(params_in.source_bits_custom - 3, params_in.source_bits_custom - 2 - InfWriteEntryBits)
  InfTab.io.setEntryisSC := io.L12ATUmemReq.bits.param === 1.U
  // ----- ResTab connection -----
  ResTab.io.checkAddr.bits.a_mask := resTabMask
  ResTab.io.checkAddr.bits.a_addr := resTabAddr
  ResTab.io.resOpcode := resTabOpcode
  ResTab.io.checkAddr.valid := resTabValid

  

  // ----- atomic FSM -----
  state := nextState
  nextState := idle
  atomicReqValid := false.B
  atomicRspValid := false.B
  memRspAtomicReady := false.B
  atomicL2Req := io.L12ATUmemReq.bits
  atomicL1Rsp := io.L22ATUmemRsp.bits
  switch(state){
    is(idle){
      memRspAtomicReady := false.B
      when(isAtomicRequest(io.L12ATUmemReq.bits) && io.L12ATUmemReq.valid && io.ATU2L2memReq.ready){
        nextState := issueGet
      }.otherwise{
        nextState := idle
      }
      atomicReqValid      := isAtomicRequest(io.L12ATUmemReq.bits) && io.L12ATUmemReq.valid
      atomicReq.a_addr    := io.L12ATUmemReq.bits.address
      atomicReq.a_mask    := io.L12ATUmemReq.bits.mask.asTypeOf(Vec(dcache_BlockWords, UInt(BytesOfWord.W)))
      atomicReq.a_data    := atomicDataInMask.io.dout
      atomicReq.a_opcode  := io.L12ATUmemReq.bits.opcode
      atomicReq.a_param   := io.L12ATUmemReq.bits.param
      atomicL2Req.opcode  := TLAOp_Get
      atomicL2Req.address := io.L12ATUmemReq.bits.address
      atomicL2Req.mask    := io.L12ATUmemReq.bits.mask
      atomicL2Req.param   := 0.U
      atomicL2Req.data    := DontCare
      atomicL2Req.source  := Cat("b11".U,0.U(InfWriteEntryBits.W),io.L12ATUmemReq.bits.source)
      atomicL2Req.source  := Cat("b11".U,0.U(InfWriteEntryBits.W),io.L12ATUmemReq.bits.source)   
      when(nextState === issueGet){ 
       memRspAtomicReady := true.B           
      }
    }
    is(issueGet){
      when(io.L22ATUmemRsp.valid && (isAtomicResponse(io.L22ATUmemRsp.bits)) && io.ATU2L2memReq.ready && io.ATU2L1memRsp.ready){
        nextState := issuePut
    }.otherwise{
      nextState := issueGet
    }
    memRspAtomicReady := io.ATU2L1memRsp.ready
    
    atomicReqValid := io.L22ATUmemRsp.valid && (isAtomicResponse(io.L22ATUmemRsp.bits))
    atomicL1Rsp.opcode  := 1.U
    atomicL1Rsp.data    := io.L22ATUmemRsp.bits.data
    atomicL1Rsp.source  := io.L22ATUmemRsp.bits.source.tail(2+InfWriteEntryBits)
    atomicL1Rsp.param   := 2.U //indicate atomic Rsp

    atomicL2Req.opcode  := TLAOp_PutPart
    atomicL2Req.address := atomicReq.a_addr
    atomicL2Req.mask    := atomicReq.a_mask.asUInt
    atomicL2Req.param   := 0.U
    atomicL2Req.data    := atomicOpResultDataPut
    atomicL2Req.source  := io.L22ATUmemRsp.bits.source
    atomicRspValid := io.L22ATUmemRsp.valid && (isAtomicResponse(io.L22ATUmemRsp.bits)) && io.ATU2L2memReq.ready
  //  when(nextState === issuePut){
  //    atomicRspValid := true.B
  //}
}
  is(issuePut){
    when(io.L22ATUmemRsp.valid && (isAtomicResponse(io.L22ATUmemRsp.bits))){
      nextState := idle
    }.otherwise{
      nextState := issuePut
    }
    memRspAtomicReady := true.B
  }
}
// memReq handler
  SCFailRsp_valid := false.B
  memReq_s_ready := io.ATU2L2memReq.ready
  memReq_m_valid := io.L12ATUmemReq.valid
  memRsp_s_ready := io.ATU2L1memRsp.ready
  memRsp_m_valid := io.L22ATUmemRsp.valid && (!isAtomicResponse(io.L22ATUmemRsp.bits))
  memReq_m := io.L12ATUmemReq.bits
  memReq_m.source := a_source
  resTabValid := 0.U
  resTabOpcode := 0.U
  resTabAddr := 0.U
  resTabMask := VecInit(Seq.fill(dcache_BlockWords)(0.U(BytesOfWord.W)))
  val memReq_s_fire = memReq_s_ready && io.L12ATUmemReq.valid
  when(io.L12ATUmemReq.valid && !atomicReqValid ){ // continue issue request when L2 req is not occupied by atomic req
    when(io.L12ATUmemReq.bits.opcode === TLAOp_Get){ // read and LR
      memReq_s_ready := io.ATU2L2memReq.ready
      memReq_m_valid := true.B
      a_source_up := 0.U
      when(io.L12ATUmemReq.bits.param === 1.U){ // LR
        when(LRhitInfWrite || ReqhitAtomicReq){
          resTabValid := false.B
      }.otherwise{
        resTabValid := memReq_s_fire
        resTabOpcode := 3.U
        resTabAddr := io.L12ATUmemReq.bits.address
        resTabMask := io.L12ATUmemReq.bits.mask.asTypeOf(Vec(dcache_BlockWords, UInt(BytesOfWord.W)))
      }
    }
  }.elsewhen(io.L12ATUmemReq.bits.opcode === TLAOp_PutPart || io.L12ATUmemReq.bits.opcode === TLAOp_PutFull){ //write and SC
    resTabAddr := io.L12ATUmemReq.bits.address
    resTabMask := io.L12ATUmemReq.bits.mask.asTypeOf(Vec(dcache_BlockWords, UInt(BytesOfWord.W)))
    when(io.L12ATUmemReq.bits.param === 1.U){//SC
      resTabOpcode := 1.U
      memReq_m.param := 0.U
      when(ResTab.io.SCSuccess){
         when(InfTab.io.full || ReqhitAtomicReq){
              memReq_s_ready := false.B
              memReq_m_valid := false.B
            }.otherwise{
              memReq_s_ready := io.ATU2L2memReq.ready
              memReq_m_valid := io.L12ATUmemReq.valid
        }
        InfWriteSetValid := memReq_s_fire

      }.otherwise{
        memReq_s_ready := io.ATU2L1memRsp.ready && !atomicRspValid && !memRsp_m_valid
        memReq_m_valid := false.B
        SCFailRsp_valid := true.B
        SCFailRsp.address := io.L12ATUmemReq.bits.address
        SCFailRsp.data := ~0.U((dcache_BlockWords*xLen).W) //SC fail return all one
        SCFailRsp.source := io.L12ATUmemReq.bits.source
        SCFailRsp.param := 1.U
        SCFailRsp.opcode := 1.U
        InfWriteRmvValid := false.B
      }
     }.otherwise{//write
      resTabOpcode := 2.U
      when(InfTab.io.full || ReqhitAtomicReq){
        memReq_s_ready := false.B
        memReq_m_valid := false.B
      }.otherwise{
        memReq_s_ready := io.ATU2L2memReq.ready
        memReq_m_valid := true.B
      }
      InfWriteSetValid := memReq_s_fire
     }
     resTabValid := memReq_s_fire
    a_source_up := Cat("b10".U,InfTab.io.SetIdx)
  }
  }.elsewhen(io.L12ATUmemReq.valid && atomicReqValid){
    memReq_s_ready := io.ATU2L2memReq.ready && (state === idle)
    memReq_m_valid := false.B
  }.otherwise{ // flush inv
    memReq_s_ready := io.ATU2L2memReq.ready
    memReq_m_valid := io.L12ATUmemReq.valid
  }
  //memRsp handler
  when(io.L22ATUmemRsp.valid && !isAtomicResponse(io.L22ATUmemRsp.bits)){
    memRsp_s_ready := io.ATU2L1memRsp.ready && !atomicRspValid
    memRsp_m_valid := true.B
    when(io.L22ATUmemRsp.bits.opcode === 0.U){ //write
      InfWriteRmvValid := memRsp_s_ready
      when(InfTab.io.remvEntryisSC){
        memRsp_m.data := 0.U
        memRsp_m.opcode := 1.U
        memRsp_m.param := 1.U
      }
    }
  }.elsewhen(io.L22ATUmemRsp.valid && isAtomicResponse(io.L22ATUmemRsp.bits)){
    memRsp_s_ready := memRspAtomicReady
  }
}

class maskDataUtil {
    def maskOrMerge(mask: UInt, n: Int): UInt = {
      val width = mask.getWidth
      val resultWidth = (width + n - 1) / n  // 向上取整
      
      VecInit(Seq.tabulate(resultWidth) { i =>
        val start = i * n
        val end = math.min(start + n, width)
        mask(end - 1, start).orR
      }).asUInt
    }
}

// mask the only word in a_data
class maskdata (width:Int, length:Int, nByte:Int) extends Module{
  val io = IO(new Bundle() {
    val din = Input(UInt((width*length).W))
    val mask = Input(UInt((width*nByte).W))
    val dout = Output(UInt(length.W))
  })

  val a_data = Wire(Vec(width, UInt(length.W)))
  val maskUtil = new maskDataUtil
  val maskOrMerge = maskUtil.maskOrMerge(io.mask, nByte)
  for (i <- 0 until width) {
    a_data(i) := Mux(maskOrMerge(i), io.din(i * length + length - 1, i * length), 0.U)
  }
  io.dout := a_data.reduce(_ | _)
}

// put the only word into d_data
class putdata (width : Int, length : Int, nByte:Int) extends Module{
  val io =  IO(new Bundle() {
    val din = Input(UInt(length.W))
    val mask = Input(UInt((width*nByte).W))
    val dout = Output(UInt((width*length).W))
  })
  val maskUtil = new maskDataUtil
  val maskOrMerge = maskUtil.maskOrMerge(io.mask, nByte)
  
  io.dout := VecInit(Seq.tabulate(width) { i =>
    Mux(maskOrMerge(i), io.din, 0.U)
  }).asUInt
}










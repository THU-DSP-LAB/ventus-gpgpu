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

import config.config.Parameters
import L1Cache.{L1TagAccess, L1TagAccess_ICache, RVGParameters}
import chisel3.DontCare.:=
import chisel3._
import chisel3.util._
import mmu.{L1TlbIO, L1TlbReq}
import mmu.SV32.{asidLen, paLen, vaLen}
import top.parameters._
import top.cache_spike_info

class ICachePipeReq(SV: mmu.SVParam)(implicit p: Parameters) extends ICacheBundle{
  val addr = UInt(WordLength.W)
  val mask = UInt(num_fetch.W)
  val warpid = UInt(WIdBits.W)
  val asid = if(MMU_ENABLED) Some(UInt(asidLen.W)) else None
  val spike_info=if(SPIKE_OUTPUT) Some(new cache_spike_info(SV)) else None
}
class ICachePipeFlush(implicit p: Parameters) extends ICacheBundle{
  val warpid = UInt(WIdBits.W)
}
class ICachePipeRsp(implicit p: Parameters) extends ICacheBundle{
  val addr = UInt(WordLength.W)
  val data = UInt((num_fetch*WordLength).W)
  val mask = UInt(num_fetch.W)
  val warpid = UInt(WIdBits.W)
  val status = UInt(2.W)//目前只有LSB投入使用，1表示MISS，0表示HIT
}
/*
* code | status
*  00  |  HIT
*  01  |  MISS accepted
*  11  | MISS unaccepted
*  10  | invalidate coreReq
* */
class ICacheMemRsp(implicit p: Parameters) extends ICacheBundle{
  val d_source = UInt((WIdBits+log2Up(NMshrEntry)).W)
  val d_addr = UInt(paLen.W)
  val d_data = Vec(BlockWords, UInt(WordLength.W))
}
class ICacheMemReq(implicit p: Parameters) extends ICacheBundle{
  val a_source = UInt((WIdBits+log2Up(NMshrEntry)).W)
  val a_addr = UInt(WordLength.W)
}
class ICacheMemReq_p(SV: mmu.SVParam)(implicit p: Parameters) extends ICacheBundle{
  val a_source = UInt((WIdBits+log2Up(NMshrEntry)).W)
  val a_addr = if(MMU_ENABLED) Some(UInt(paLen.W)) else Some(UInt(vaLen.W))
  val spike_info=if(SPIKE_OUTPUT) Some(new cache_spike_info(SV)) else None
}

class ICacheExtInf(SV: Option[mmu.SVParam] = None)(implicit p: Parameters) extends ICacheBundle{
  val coreReq = Flipped(DecoupledIO(new ICachePipeReq(SV.getOrElse(mmu.SV32))))
  val externalFlushPipe = Flipped(ValidIO(new ICachePipeFlush))
  val coreRsp = DecoupledIO(new ICachePipeRsp)
  val memRsp = Flipped(DecoupledIO(new ICacheMemRsp))
  val memReq = DecoupledIO(new ICacheMemReq_p(SV.getOrElse(mmu.SV32)))
  val TLBRsp = if(MMU_ENABLED) Some(Flipped(DecoupledIO(new mmu.L1TlbRsp(SV.getOrElse(mmu.SV32))))) else None
  val TLBReq = if(MMU_ENABLED) Some(DecoupledIO(new mmu.L1TlbReq(SV.getOrElse(mmu.SV32)))) else None
  val invalidate = Input(Bool())
}

class ASourceRegBundle(SV: Option[mmu.SVParam] = None)(implicit p: Parameters) extends ICacheBundle {
  val instrId = UInt(log2Up(NMshrEntry).W)
  val spike_info=if(SPIKE_OUTPUT) Some(new cache_spike_info(SV.getOrElse(mmu.SV32))) else None
}

class InstructionCache(SV: Option[mmu.SVParam] = None)(implicit p: Parameters) extends ICacheModule{
  val io = IO(new ICacheExtInf(SV))

  // ****** submodules ******
  val tagAccess = Module(new L1TagAccess_ICache(set=NSets, way=NWays, tagBits=TagBits, AsidBits = asidLen))
  val dataAccess = Module(new ICacheDataArray)

  // TODO: need to deal with MSHR. Clear it or wait for it?
  tagAccess.io.invalidate := io.invalidate

  //val mshrMissReq_Q = Module(new Queue(new MSHRmissReq(bABits,tIBits,WIdBits),1,pipe=true,flow=true))
  //flow选项开启是必须的。
  val mshrAccess = Module(new MSHR(UInt(tIBits.W))(p))

  val memRsp_Q = Module(new Queue(new ICacheMemRsp,2, pipe = true))
  // ****** submodules ******

  // ******     hit pipeline regs      ******
  // external flush violate causality
  //val ShouldFlushCoreRsp_st2 = Wire(Bool())
  val ShouldFlushCoreRsp_st1 = Wire(Bool())
  val ShouldFlushCoreRsp_st0 = Wire(Bool())
  val coreReqFire_st1 = RegNext(io.coreReq.fire && !ShouldFlushCoreRsp_st0, false.B)
  val coreReqFire_st2 = RegNext(coreReqFire_st1 && !ShouldFlushCoreRsp_st1, false.B)
  //ljz: need to know if cachemiss is sent
  val coreRespFire_st2 =io.coreRsp.fire
  val coreRespFire_st3 =RegNext(coreRespFire_st2)


  val cacheHit_st1 = tagAccess.io.hit_st1 && coreReqFire_st1
  val cacheMiss_st1 = !tagAccess.io.hit_st1 && coreReqFire_st1

  val wayidx_hit_st1 = Wire(UInt(WayIdxBits.W))
  wayidx_hit_st1 := OHToUInt(tagAccess.io.waymaskHit_st1)
  val waymask_replace_st0 = tagAccess.io.waymaskReplacement
  val warpid_st1 = RegEnable(io.coreReq.bits.warpid, io.coreReq.ready)
  val mask_st1 = RegEnable(io.coreReq.bits.mask, io.coreReq.ready)
  val warpid_st2 = RegNext(warpid_st1)
  val mask_st2 = RegNext(mask_st1)
  val addr_st1 = RegEnable(io.coreReq.bits.addr, io.coreReq.ready)
  val addr_st2 = RegNext(addr_st1)
  if(MMU_ENABLED){
    val ASID_st2 = RegEnable(io.coreReq.bits.asid.get, io.coreReq.ready)
    val pipeReqAsid_st1 = RegEnable(io.coreReq.bits.asid.get, 0.U(io.coreReq.bits.asid.get.getWidth.W), io.coreReq.fire)
    tagAccess.io.r_asid.get.req.valid := io.coreReq.fire && !ShouldFlushCoreRsp_st0
    tagAccess.io.r_asid.get.req.bits.setIdx := get_setIdx(io.coreReq.bits.addr)
    tagAccess.io.asidFromCore_st1.get := pipeReqAsid_st1
    mshrAccess.io.missReq.bits.ASID.get := pipeReqAsid_st1
  }

  // ******     external flushPipeline
  //ShouldFlushCoreRsp_st2 := warpid_st2 === io.externalFlushPipe.bits.warpid && io.externalFlushPipe.valid
  ShouldFlushCoreRsp_st1 := warpid_st1 === io.externalFlushPipe.bits.warpid && io.externalFlushPipe.valid
  ShouldFlushCoreRsp_st0 := io.coreReq.bits.warpid === io.externalFlushPipe.bits.warpid && io.externalFlushPipe.valid

  val pipeReqAddr_st1 = RegEnable(io.coreReq.bits.addr, 0.U(io.coreReq.bits.addr.getWidth.W), io.coreReq.fire)

  // ******      tag read, to handle mem rsp st1 & pipe req st1      ******
  tagAccess.io.r.req.valid := io.coreReq.fire && !ShouldFlushCoreRsp_st0
  tagAccess.io.r.req.bits.setIdx := get_setIdx(io.coreReq.bits.addr)

  tagAccess.io.tagFromCore_st1 := get_tag(pipeReqAddr_st1)

  tagAccess.io.coreReqReady := io.coreReq.ready
  // ******     missReq Queue enqueue     ******
  memRsp_Q.io.enq <> io.memRsp
  val refillSliceWords = ICacheDataArray.refillSliceWords(BlockWords)
  val refillSliceBits = ICacheDataArray.refillSliceBits(BlockWords, WordLength)
  val refillSliceOffsetBits = log2Ceil(refillSliceWords)
  require(refillSliceWords >= num_fetch, "ICache data slice must contain one fetch group")
  require(num_fetch % refillSliceWords == 0 || refillSliceWords % num_fetch == 0,
    "ICache fetch group must not cross data slices")

  val memRsp_QDataBySlice = Wire(Vec(ICacheDataArray.RefillSliceCount, UInt(refillSliceBits.W)))
  for (sliceIdx <- 0 until ICacheDataArray.RefillSliceCount) {
    val firstWord = sliceIdx * refillSliceWords
    val sliceWords = VecInit(Seq.tabulate(refillSliceWords) { wordIdx =>
      memRsp_Q.io.deq.bits.d_data(firstWord + wordIdx)
    })
    memRsp_QDataBySlice(sliceIdx) := sliceWords.asUInt
  }

  val refillActive = RegInit(false.B)
  val refillSliceIdx = RegInit(0.U(log2Ceil(ICacheDataArray.RefillSliceCount).W))
  val refillDataBySlice = Reg(Vec(ICacheDataArray.RefillSliceCount, UInt(refillSliceBits.W)))
  val refillBlockAddr = Reg(UInt(bABits.W))
  val refillWaymask = Reg(UInt(NWays.W))
  val refillLastSlice = refillSliceIdx === (ICacheDataArray.RefillSliceCount - 1).U
  val refillAsidWriteReady = if(MMU_ENABLED) tagAccess.io.w_asid.get.req.ready else true.B
  val refillTagWriteReady = tagAccess.io.w.req.ready && refillAsidWriteReady
  val refillCommitReady = !refillLastSlice || refillTagWriteReady
  val invalidateDrain = RegInit(false.B)
  val invalidateActive = io.invalidate || invalidateDrain
  val refillBlocksCoreReq = refillActive || memRsp_Q.io.deq.valid || invalidateActive

  //deq coupled with mshr missRsp
  // ******     mshrAccess      ******
  mshrAccess.io.missReq.valid := cacheMiss_st1 && !invalidateActive
  mshrAccess.io.missReq.bits.blockAddr := get_blockAddr(pipeReqAddr_st1)

  mshrAccess.io.missReq.bits.targetInfo := Cat(warpid_st1,get_offsets(pipeReqAddr_st1))
  //mshrAccess.io.missReq <> mshrMissReq_Q.io.deq

  when(io.invalidate) {
    invalidateDrain := true.B
  }.elsewhen(invalidateDrain && mshrAccess.io.empty && !memRsp_Q.io.deq.valid && !refillActive) {
    invalidateDrain := false.B
  }

  memRsp_Q.io.deq.ready := !refillActive && mshrAccess.io.missRspIn.ready
  mshrAccess.io.missRspIn.valid := memRsp_Q.io.deq.valid && !refillActive
  mshrAccess.io.missRspIn.bits.EntryIdx := memRsp_Q.io.deq.bits.d_source

  mshrAccess.io.missRspOut.ready := true.B
  //coreRsp_Q.io.enq.ready TODO 将来版本可能重新启用信号，如果core前端需要MSHR返回信息的话

  val refillAccept = memRsp_Q.io.deq.fire && !invalidateDrain && !io.invalidate
  val refillWriteFire = dataAccess.io.w.req.fire
  when(io.invalidate) {
    refillActive := false.B
    refillSliceIdx := 0.U
  }.elsewhen(refillAccept) {
    refillActive := true.B
    refillSliceIdx := 0.U
    refillDataBySlice := memRsp_QDataBySlice
    refillBlockAddr := mshrAccess.io.missRspOut.bits.blockAddr
    refillWaymask := waymask_replace_st0
  }.elsewhen(refillWriteFire) {
    when(refillLastSlice) {
      refillActive := false.B
      refillSliceIdx := 0.U
    }.otherwise {
      refillSliceIdx := refillSliceIdx + 1.U
    }
  }

  val refillWriteBlockAddr = Mux(refillActive, refillBlockAddr, mshrAccess.io.missRspOut.bits.blockAddr)
  tagAccess.io.w.req.valid := refillActive && !io.invalidate && refillLastSlice &&
    dataAccess.io.w.req.ready && refillAsidWriteReady
  tagAccess.io.w.req.bits(
    data = get_tag(refillWriteBlockAddr),
    setIdx = get_setIdx(refillWriteBlockAddr),
    waymask = refillWaymask)

  if(MMU_ENABLED) {
    val refillAsid = Reg(UInt(asidLen.W))
    when(refillAccept) {
      refillAsid := mshrAccess.io.missRspOut.bits.ASID.get
    }
    tagAccess.io.w_asid.get.req.valid := refillActive && !io.invalidate && refillLastSlice &&
      dataAccess.io.w.req.ready && tagAccess.io.w.req.ready
    tagAccess.io.w_asid.get.req.bits(
      data = refillAsid,
      setIdx = get_setIdx(refillBlockAddr),
      waymask = refillWaymask)
  }

  // ******      data write, to handle mem rsp refill slices      ******
  dataAccess.io.w.req.valid := refillActive && !io.invalidate && refillCommitReady
  dataAccess.io.w.req.bits.data := refillDataBySlice(refillSliceIdx)
  dataAccess.io.w.req.bits.setIdx := get_setIdx(refillBlockAddr)
  dataAccess.io.w.req.bits.sliceIdx := refillSliceIdx
  dataAccess.io.w.req.bits.waymask.foreach(_ := refillWaymask)

  // ******      data read, to handle pipe req st2     ******
  dataAccess.io.r.req.valid := io.coreReq.fire && !ShouldFlushCoreRsp_st0
  dataAccess.io.r.req.bits.setIdx := get_setIdx(io.coreReq.bits.addr)
  dataAccess.io.r.req.bits.sliceIdx :=
    get_blockOffset(io.coreReq.bits.addr)(BlockOffsetBits - 1, refillSliceOffsetBits)
  val dataAccess_data = dataAccess.io.r.resp.data
  val data_after_wayidx_st1 = dataAccess_data(wayidx_hit_st1)
  val blockOffset_sel_st1 = get_blockOffset(pipeReqAddr_st1)
  val wordOffsetInSlice_st1 = blockOffset_sel_st1(refillSliceOffsetBits - 1, 0)
  if(num_fetch>1){
    assert(blockOffset_sel_st1(log2Ceil(num_fetch)-1,0).orR === false.B)
  }
  val data_to_blockOffset_st1 = data_after_wayidx_st1
  val data_after_blockOffset_st1 = Wire(UInt((num_fetch*xLen).W))//(data_to_blockOffset_st1 >> (blockOffset_sel_st1 << 5))//(num_fetch*xLen,0)
  if(num_fetch * xLen == refillSliceBits){
    data_after_blockOffset_st1 := (data_to_blockOffset_st1 >> (wordOffsetInSlice_st1 << 5))
  } else{
    data_after_blockOffset_st1 := (data_to_blockOffset_st1 >> (wordOffsetInSlice_st1 << 5))(num_fetch*xLen - 1,0)
  }
  //val data_after_blockOffset_st1 = (data_to_blockOffset_st1 >> (blockOffset_sel_st1 << 5))//(num_fetch*xLen,0)
  //val data_after_blockOffset_st1 = (data_to_blockOffset_st1 >> (blockOffset_sel_st1*32))(31,0)
  val data_after_blockOffset_st2 = RegNext(data_after_blockOffset_st1)

  // ******      core rsp
  val OrderViolation_st1 = Wire(Bool())
  val OrderViolation_st2 = RegNext(OrderViolation_st1, false.B)
  io.coreRsp.valid := coreReqFire_st2 && !OrderViolation_st2 //&& !ShouldFlushCoreRsp_st2// || missRsp_from_mshr
  io.coreRsp.bits.data := data_after_blockOffset_st2
  io.coreRsp.bits.warpid := warpid_st2
  io.coreRsp.bits.mask := mask_st2
  /*Mux(missRsp_from_mshr,
    //miss Rsp
    mshrAccess.io.missRspOut.bits.targetInfo>>(BlockOffsetBits+WordOffsetBits),
    //hit
    warpid_st1)*/
  io.coreRsp.bits.addr := addr_st2
  /*Mux(missRsp_from_mshr,
    //miss Rsp
    Cat(memRsp_QAddr_st2,
      mshrAccess.io.missRspOut.bits.targetInfo(BlockOffsetBits+WordOffsetBits-1,0)),
    //hit
    addr_st1)*/
  val Status_st1 = Mux(RegNext(ShouldFlushCoreRsp_st0),"b10".U, Cat(
    (cacheMiss_st1 && !mshrAccess.io.missReq.fire) || OrderViolation_st1,
    cacheMiss_st1))//Reg数量看起来虚多，但这样才可以让st0信号能把b10传递过来
  val Status_st2 = Mux(RegNext(ShouldFlushCoreRsp_st1),"b10".U,RegNext(Status_st1))
  io.coreRsp.bits.status := Status_st2//Mux(ShouldFlushCoreRsp_st2,"b10".U,Status_st2)

 // io.memReq.valid := mshrAccess.io.miss2mem.valid
 // mshrAccess.io.miss2mem.ready := io.memReq.ready
 // io.memReq.bits.a_addr := Cat(mshrAccess.io.miss2mem.bits.blockAddr,0.U((32-bABits).W))
 // io.memReq.bits.a_source := mshrAccess.io.miss2mem.bits.instrId
  if(MMU_ENABLED){
    io.TLBReq.get.valid := mshrAccess.io.miss2mem.valid
    io.TLBReq.get.bits.vaddr := Cat(mshrAccess.io.miss2mem.bits.blockAddr,0.U((32-bABits).W))
    io.TLBReq.get.bits.asid := mshrAccess.io.miss2mem.bits.ASID.get
    mshrAccess.io.miss2mem.ready := io.TLBReq.get.ready
    val a_source_reg = Module(new Queue(new ASourceRegBundle(SV),1,true,false))
    a_source_reg.io.enq.valid := io.TLBReq.get.fire
    a_source_reg.io.enq.bits.instrId := mshrAccess.io.miss2mem.bits.instrId
    a_source_reg.io.enq.bits.spike_info.foreach{ left =>
      left.pc := 0.U
      left.vaddr := mshrAccess.io.miss2mem.bits.blockAddr
    }
    a_source_reg.io.deq.ready := io.memReq.ready && io.TLBRsp.get.valid
    io.TLBRsp.get.ready := io.memReq.ready
    io.memReq.bits.a_addr.get := io.TLBRsp.get.bits.paddr
    io.memReq.bits.a_source := a_source_reg.io.deq.bits.instrId
    io.memReq.valid := io.TLBRsp.get.valid && a_source_reg.io.deq.valid
    io.memReq.bits.spike_info.foreach( _ := a_source_reg.io.deq.bits.spike_info.getOrElse(0.U) )

  } else {
    mshrAccess.io.miss2mem.ready := io.memReq.ready
    io.memReq.bits.a_addr.get := Cat(mshrAccess.io.miss2mem.bits.blockAddr,0.U((32-bABits).W))
    io.memReq.bits.a_source := mshrAccess.io.miss2mem.bits.instrId
    io.memReq.valid := mshrAccess.io.miss2mem.valid
    io.memReq.bits.spike_info.foreach{ left =>
      left.pc := 0.U
      left.vaddr := mshrAccess.io.miss2mem.bits.blockAddr
    }

  }



  // ******      core req ready
  //val coreRsp_QAlmstFull = coreRsp_Q.io.count === 2.U
  io.coreReq.ready := !refillBlocksCoreReq//!memRsp_Q.io.deq.valid && !mshrAccess.io.missRspOut.valid//mshrAccess.io.missReq.ready //&& !coreRsp_QAlmstFull

  // ******    self generate flushPipeline
  //保存两个周期的warp id，与当前输入id对比。
  //如果对比一致，并且在前两个周期发生过MISS，那么当前请求无效化。
  //val OrderViolation_st2 = RegNext(OrderViolation_st1)
  val OrderViolation_st3 = RegNext(OrderViolation_st2)
  val warpid_st3 = RegNext(warpid_st2)
  val cacheMiss_st2 = RegNext(Mux(ShouldFlushCoreRsp_st1,false.B,cacheMiss_st1))
  val cacheMiss_st3 = RegNext(cacheMiss_st2)

  val warpIdMatch2_st1 = warpid_st1 === warpid_st2
  val warpIdMatch3_st1 = warpid_st1 === warpid_st3

  OrderViolation_st1 := (warpIdMatch2_st1 && cacheMiss_st2  && !OrderViolation_st2) || (warpIdMatch3_st1 && cacheMiss_st3 && !OrderViolation_st3)
}

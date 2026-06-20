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
package L1Cache.DCache

import L1Cache.DCache.DCacheParameters._
import L1Cache._
import SRAMTemplate._
import chisel3._
import chisel3.util._
import config.config.Parameters
import firrtl.Utils._
import top.parameters.{MMU_ENABLED, NUMBER_CU, dcache_BlockOffsetBits, dcache_BlockWords, dcache_MshrEntry, dcache_NSets, dcache_WordOffsetBits, num_block, num_thread}
import mmu.SV32.{asidLen, paLen, vaLen}
import top.parameters.DCACHE_DEBUG
import scala.tools.nsc.interpreter.Repl

class WshrMemReqV2 extends DCacheMemReq {
  val hasCoreRsp = Bool()
  val coreRspInstrId = UInt(32.W)
  val activeMask = Vec(num_thread, Bool())
  val Asid = if(MMU_ENABLED) Some(UInt(asidLen.W)) else None
}

class DCachePerfCounters extends Bundle {
  val totalReq = UInt(64.W)
  val readReq = UInt(64.W)
  val writeReq = UInt(64.W)
  val readMiss = UInt(64.W)
  val writeMiss = UInt(64.W)
  val readPrimaryMiss = UInt(64.W)
  val readSecondaryMiss = UInt(64.W)
  val readPrimaryFullMiss = UInt(64.W)
  val readSecondaryFullMiss = UInt(64.W)
  val writeFreshMiss = UInt(64.W)
  val writeInflightMiss = UInt(64.W)
  val replacements = UInt(64.W)
  val dirtyWritebacks = UInt(64.W)
  val mshrFullCycles = UInt(64.W)
  val rtabReplays = UInt(64.W)
  val bankConflictCycles = UInt(64.W)
  // Pipelining-effectiveness counters — direct evidence that v2's flow
  // restructuring is firing. v1 has no equivalent structures so these
  // tie to 0 there.
  val coreReqPipePipelinedCycles = UInt(64.W) // st0_valid && st1_valid in CoreReqPipe
  val memRspPipeDecoupledCycles  = UInt(64.W) // dcache write && coreRsp emit same cycle in MemRspPipe
}

class DataCachev2(SV: Option[mmu.SVParam] = None)(implicit p: Parameters) extends DCacheModule{
  val io = IO(new Bundle{
    val coreReq = Flipped(DecoupledIO(new DCacheCoreReq(SV)))
    val coreRsp = DecoupledIO(new DCacheCoreRsp)
    val memRsp = Flipped(DecoupledIO(new DCacheMemRsp))
    val memReq = if(MMU_ENABLED) Some(DecoupledIO(new DCacheMemReq_p)) else Some(DecoupledIO(new DCacheMemReq))
    val TLBRsp = if(MMU_ENABLED) Some(Flipped(DecoupledIO(new mmu.L1TlbRsp(SV.getOrElse(mmu.SV32))))) else None
    val TLBReq = if(MMU_ENABLED) Some(DecoupledIO(new mmu.L1TlbReq(SV.getOrElse(mmu.SV32)))) else None
    val perfEnable = Input(Bool())
    val perfReset = Input(Bool())
    val perf = Output(new DCachePerfCounters)
  })
  // submodules
  val TagAccess = Module(new L1TagAccess(set=NSets, way=NWays, tagBits=TagBits,AsidBits = asidLen,readOnly=false))
  val WshrAccess = Module(new DCacheWSHR(Depth = NWshrEntry))
  val ReplayTable = Module(new L1RTAB())
  // The MSHR class's `InstrIdBits` parameter (formerly `WIdBits` in V1 cache era) sets
  // the width of MSHRmissReq/RspOut.instrId. V2 carries an MSHR index in this field, not
  // a warp id; see L1MSHR.scala and bugs/bfs4096-003/phase_4_report.md. Pass
  // max(WIdBits, log2Up(NMshrEntry)) so NMshrEntry can exceed num_warp.
  val MshrAccess = Module(new MSHR(bABits = bABits, tIWidth = tIBits, InstrIdBits = math.max(WIdBits, log2Up(NMshrEntry)), NMshrEntry, NMshrSubEntry, asidLen))
  val SMshrAccess = Module(new SpecialMSHR(bABits = bABits, tIWidth = tIBits, InstrIdBits = math.max(WIdBits, log2Up(NMshrEntry)), NMshrEntry, asidLen))
  val DataAccesses = Seq.tabulate(BlockWords) { i =>
    Module(new SRAMTemplate(
      gen=UInt(8.W),
      set=NSets*NWays,
      way=BytesOfWord,
      shouldReset = false,
      holdRead = false,
      singlePort = false,
      bypassWrite = true
    ))
  }
    // pipelines
  val coreReqPipe = Module(new CoreReqPipe)
  val memRspPipe = Module(new MemRspPipe)
  // [bfs4096-005] memRsp_Q 2→8, memReq_Q 8→32: L1↔L2 dead embrace 缓解
  // 因果链 (run_d_postfix hang @3_750_265 ps): memRsp_Q 满(2)+ tagRequestStatus
  // FSM 卡 memReq state + WSHR slot 9 read 序列化 → memReq_Q 反压 → MemRspPipe
  // 永不让出 → fill 永不前进 → 死锁。memReq_Q=32 对齐 per-SM source identity
  // 上限 (WSHR16+MSHR16)，memRsp_Q=8 给 fill 流水线足够 burst 缓冲。
  // 注: 治标 (降低触发概率)，不治本 (MemRspPipe FSM 缺陷封存)。
  val memRsp_Q = Module(new Queue(new DCacheMemRsp,entries = 8,flow=false,pipe=false))
  val memReq_Q = Module(new Queue(new WshrMemReqV2,entries = 32,flow=false,pipe=false))
  val RTAB_pushedIdx_st2 = Module(new Queue(UInt(NRTABs.W),entries = 8,flow=false,pipe=false))
  val MemReqArb = Module(new Arbiter(new WshrMemReqV2, 2))
  val CoreReqArb = Module(new Arbiter(new DCacheCoreReq, 2))
  val dirtyReplaceMemReq = Wire(new WshrMemReqV2)
  val coreWriteHitFire = coreReqPipe.io.st1_valid && coreReqPipe.io.st1_ready && coreReqPipe.io.WriteHit_st1
  val totalReqCnt = RegInit(0.U(64.W))
  val readReqCnt = RegInit(0.U(64.W))
  val writeReqCnt = RegInit(0.U(64.W))
  val readMissCnt = RegInit(0.U(64.W))
  val writeMissCnt = RegInit(0.U(64.W))
  val readPrimaryMissCnt = RegInit(0.U(64.W))
  val readSecondaryMissCnt = RegInit(0.U(64.W))
  val readPrimaryFullMissCnt = RegInit(0.U(64.W))
  val readSecondaryFullMissCnt = RegInit(0.U(64.W))
  val writeFreshMissCnt = RegInit(0.U(64.W))
  val writeInflightMissCnt = RegInit(0.U(64.W))
  val replacementCnt = RegInit(0.U(64.W))
  val dirtyWritebackCnt = RegInit(0.U(64.W))
  val mshrFullCyclesCnt = RegInit(0.U(64.W))
  val rtabReplayCnt = RegInit(0.U(64.W))
  val bankConflictCyclesCnt = RegInit(0.U(64.W))
  val coreReqPipePipelinedCnt = RegInit(0.U(64.W))
  val memRspPipeDecoupledCnt  = RegInit(0.U(64.W))
  for(i <- 0 until BlockWords){
    DataAccesses(i).io.r.req.valid := coreReqPipe.io.read_Req_dA.valid || memRspPipe.io.dAReplace_rReq_valid
    DataAccesses(i).io.r.req.bits := Mux(memRspPipe.io.dAReplace_rReq_valid,
      memRspPipe.io.dAReplace_rReq(i), coreReqPipe.io.read_Req_dA.bits(i))
    DataAccesses(i).io.w.req.valid := (coreWriteHitFire && coreReqPipe.io.WriteReq_dA_valid(i)) || memRspPipe.io.dAmemRsp_wReq_valid
    DataAccesses(i).io.w.req.bits := Mux(memRspPipe.io.dAmemRsp_wReq_valid,
      memRspPipe.io.dAmemRsp_wReq(i), coreReqPipe.io.WriteReq_dA(i))
  }
  val DataAccessRRsp = DataAccesses.map(d => d.io.r.resp.data)
  val DataAccessReadSRAMRRsp = DataAccessRRsp.map(d => Cat(d.reverse))
  val replaceMemReqFire = Wire(Bool())
  val perfBankEn = Module(new getDataAccessBankEn(NBank = BlockWords, NLane = NLanes))
  val replaceReadResp = RegNext(memRspPipe.io.dAReplace_rReq_valid, false.B)
  val replaceDataValid = RegInit(false.B)
  val replaceDataReg = Reg(Vec(BlockWords, UInt(WordLength.W)))
  val replaceAddrReg = Reg(UInt(WordLength.W))
  // bfs4096-001 partial-write clobber fix: 与 data / addr 一起 hold victim 的字节级 dirty mask。
  val replaceMaskReg = Reg(UInt((BlockWords * BytesOfWord).W))
  // L1TagAccess.io.{a_addrReplacement_st1, replace_dirty_mask_st1, asidReplacement_st1}
  // 后缀虽是 _st1 实为 SRAM r.resp.data 直出 wire，与 replaceReadResp 同步在 cycle N+1
  // 出值，本 when 块直接抓即可——若再 RegNext 等于退到 cycle N（SRAM resp 未出，
  // holdRead=true 让信号保留旧值）→ stale。
  // 历史成因：backprop1024-001 (commit 08e296c8) 给 addr 加 RegNext 是 latent bug，在
  // backprop 数据集未显现；bfs4096-001 patch 机械模仿到 mask 才让 stale 显形。
  // 本次一并修正 addr / mask / asid。详见 bugs/bfs4096-001/checkpoint_3.md。
  when(replaceReadResp){
    replaceDataReg := VecInit(DataAccessReadSRAMRRsp)
    replaceAddrReg := TagAccess.io.a_addrReplacement_st1.get
    replaceMaskReg := TagAccess.io.replace_dirty_mask_st1
    replaceDataValid := true.B
  }
  when(replaceMemReqFire){
    replaceDataValid := false.B
  }
  perfBankEn.io.perLaneBlockIdx := coreReqPipe.io.perLaneAddr_st1.map(_.blockOffset)
  perfBankEn.io.perLaneValid := coreReqPipe.io.perLaneAddr_st1.map(_.activeMask)
  // core request arbiter
  // source: RTAB top request / core request from io
  val blockCoreReq = memRspPipe.io.blockCoreReq
  CoreReqArb.io.in(0).valid := ReplayTable.io.coreReq_replay.valid && !blockCoreReq
  CoreReqArb.io.in(0).bits  := ReplayTable.io.coreReq_replay.bits
  ReplayTable.io.coreReq_replay.ready := CoreReqArb.io.in(0).ready && !blockCoreReq
  // IMPORTANT：保证 Decoupled 握手语义一致。
  // RTAB 满时不仅要拉低顶层 ready，也必须 gate 掉 in(1).valid，否则外部未 fire 但 arbiter 可能内部 fire，
  // 导致同一条外部请求被重复注入 st0。
  // 另外：当 RTAB 仅剩 1 个空位（almost_full）且本拍 st1 仍会向 RTAB 入队时，也要拉低 ready，
  // 否则可能出现“本拍占掉最后一个空位 + 同拍再接收下一条 coreReq，下一拍该 coreReq 也要入 RTAB -> 溢出”的情况。
  // nn64k-009 fix: 上面的"为 st1 入队预留最后一格"必须用 CoreReqPipe 的 raw intent
  // Req_st1_RTAB_reserve，而不是被 st1_ready 二次 gate 的 Req_st1_RTAB.valid。后者在 st1 因
  // mshrReleasingSameBlock_st1(CoreReqPipe:660-662)等被 hold 的拍为 0，漏挡外部 in(1) 的
  // st0-hitRTAB 快路径(CoreReqPipe:194 + L1RTAB:145-154)，让它占掉最后一格 → RTAB_full →
  // st1 被 L649 永久 hold → replay 只能回注被该 st1 堵死的 st0 → ptr_r 不前进 → RTAB_full 保持
  // = nn64k-009 的 st1⇄RTAB 循环资源死锁(true-LRU 触发, anti-LRU 掩盖)。用 raw intent 后，st1 持
  // parkable 请求期间 RTAB 永远停在 almost_full(≤NRTABs-1)，给该请求留住落位槽，环在首跳前即消除。
  val reserveRTABSlotForSt1 =
    ReplayTable.io.RTAB_almost_full && coreReqPipe.io.Req_st1_RTAB_reserve
  val allowIn1 =
    !ReplayTable.io.RTAB_full &&
      !reserveRTABSlotForSt1 &&
      !blockCoreReq
  CoreReqArb.io.in(1).valid := io.coreReq.valid && allowIn1
  CoreReqArb.io.in(1).bits  := io.coreReq.bits
  io.coreReq.ready := CoreReqArb.io.in(1).ready && allowIn1
  //---------coreReqPipe input connection------------
  // st0
  coreReqPipe.io.CoreReq                <> CoreReqArb.io.out
  coreReqPipe.io.RTABHit                := ReplayTable.io.checkRTABhit
  coreReqPipe.io.RTAB_full              := ReplayTable.io.RTAB_full
  coreReqPipe.io.hasDirty               := TagAccess.io.hasDirty_st0.get
  coreReqPipe.io.MSHREmpty              := MshrAccess.io.empty
  coreReqPipe.io.fillPipeDrained        := memRspPipe.io.fillPipeDrained   // bfs4096-008 §9 drain-before-invalidate
  coreReqPipe.io.SMSHREmpty             := SMshrAccess.io.empty
  coreReqPipe.io.tA_dirtySetIdx_st0     := TagAccess.io.dirtySetIdx_st0.get
  coreReqPipe.io.tA_dirtyWayMask_st0    := TagAccess.io.dirtyWayMask_st0.get
  coreReqPipe.io.reqSource              := CoreReqArb.io.out.valid && ReplayTable.io.coreReq_replay.valid
  coreReqPipe.io.Probe_tA_ready         := TagAccess.io.probeRead.ready
  coreReqPipe.io.blockCoreReq           := blockCoreReq
  // bfs4096-006 fix: refillWrite_valid 透给 coreReqPipe / RTAB 时用 intent (=memRspPipe.st1_valid)，
  // 而不是 dAmemRsp_wReq_valid (=st1_valid && st1_ready)——后者经过 st1_ready 与 coreReqPipe
  // memRsp_coreRsp.ready 形成 combinational cycle。intent 路径破环代价：fill stall 时 hit 保守 replay。
  coreReqPipe.io.refillWrite_valid      := memRspPipe.io.dAmemRsp_wReq_intent
  coreReqPipe.io.refillWrite_blockAddr  := memRspPipe.io.dAmemRsp_wReq_blockAddr
  // bfs4096-006 fix: 透传 fill 的精确 dA row id (Cat(set, victim_way))，CoreReqPipe ST1 用以检测
  // hit-read 与 fill 同拍撞同 dA row (bypassWrite=true 跨 cacheline 数据污染场景)。
  coreReqPipe.io.refillWrite_setIdx     := memRspPipe.io.dAmemRsp_wReq_setIdx
  if(MMU_ENABLED){
    coreReqPipe.io.refillWrite_asid.get := memRspPipe.io.dAmemRsp_wReq_asid.get
  }
  // bfs4096-009 fix: 真实 fill commit (=dAmemRsp_wReq_valid, 含 st1_ready) 给 CoreReqPipe commit-seen latch。
  // 区别于上面 refillWrite_valid 用的 intent —— commit-seen 要"真写进去了"，且只进 Reg 不组合回 ready/valid。
  coreReqPipe.io.fillCommit_valid     := memRspPipe.io.dAmemRsp_wReq_valid
  coreReqPipe.io.fillCommit_blockAddr := memRspPipe.io.dAmemRsp_wReq_blockAddr
  coreReqPipe.io.mshrReleasing_valid     := MshrAccess.io.releasing_valid
  coreReqPipe.io.mshrReleasing_blockAddr := MshrAccess.io.releasing_blockAddr
  if(MMU_ENABLED){
    coreReqPipe.io.mshrReleasing_asid.get := MshrAccess.io.releasing_asid.get
  }
  // st1
  coreReqPipe.io.tA_Hit_st1             := TagAccess.io.hitStatus_st1
  if(MMU_ENABLED){
    coreReqPipe.io.tA_dirtyAsid_st1.get := TagAccess.io.dirtyASID_st1.get
  }
  coreReqPipe.io.tA_dirtyTag_st1   := TagAccess.io.dirtyTag_st1.get
  // bfs4096-001 fix: 透传 byte 级 dirty mask 给 CoreReqPipe（flush/invalidate 走 PutPartialData）。
  coreReqPipe.io.tA_dirtyMask_st1  := TagAccess.io.dirtyMask_st1
  coreReqPipe.io.MSHR_ProbeStatus  := MshrAccess.io.probeOut_st1
  coreReqPipe.io.SMSHR_ProbeStatus := SMshrAccess.io.probeOut_st1
  coreReqPipe.io.WSHR_CheckResult  := WshrAccess.io.checkresult
  coreReqPipe.io.Mshr_st1_ready    := MshrAccess.io.missReq.ready
  coreReqPipe.io.memRsp_coreRsp    <> memRspPipe.io.memRsp_coreRsp
 // st2
  coreReqPipe.io.dA_data          := DataAccessReadSRAMRRsp
  coreReqPipe.io.memRspIsFlu      := memRspPipe.io.memRspIsFlu
  //-----------core req pipe output connection------------
  // st0
  val st0_fire = coreReqPipe.io.st0_valid && coreReqPipe.io.st0_ready
  MshrAccess.io.probe.bits            := coreReqPipe.io.Probe_MSHR
  MshrAccess.io.probe.valid           := st0_fire
  if(MMU_ENABLED){
    MshrAccess.io.probeAsid.get  := coreReqPipe.io.probeAsid.get
  }
  TagAccess.io.probeRead.bits         := coreReqPipe.io.Probe_tA
  TagAccess.io.probeRead.valid        := st0_fire
  ReplayTable.io.RTABReq_st0     <> coreReqPipe.io.Req_st0_RTAB
  TagAccess.io.invalidateAll     := coreReqPipe.io.invalidate_tA
  TagAccess.io.flushChoosen.get  := coreReqPipe.io.flushDirty_tA
  // st1
  TagAccess.io.tagFromCore_st1        := coreReqPipe.io.tagFromCore_tA_st1
  TagAccess.io.probeIsWrite_st1.get       := coreReqPipe.io.coreReq_Control_st1.isWrite
  TagAccess.io.probeIsUncache_st1       := coreReqPipe.io.coreReq_Control_st1.isUncached
  TagAccess.io.tagready_st1    := coreReqPipe.io.st1_ready
  TagAccess.io.perLaneAddr_st1 := coreReqPipe.io.perLaneAddr_st1
  // bfs4096-002 fix: 把 ST1 队头有效信号（CoreReq_pipeReg_st0_st1.deq.valid）接入 TagAccess，
  // 用作 dirtyMaskWriteArb.in(1).valid 的严格 gate。否则 cache_hit 与 probeIsWrite_st1
  // 在两次 dispatch 之间持续保持高电平，导致 in(1) 长期重写同一 set 的 dirty mask。
  // 详见 L1TagAccess.scala:312 注释 + bugs/bfs4096-002/checkpoint_3.md 迭代 1。
  TagAccess.io.coreReq_st1_valid := coreReqPipe.io.st1_valid
  if(MMU_ENABLED){
    TagAccess.io.asidFromCore_st1.get := coreReqPipe.io.asidFromCore_tA_st1.get
  }
  ReplayTable.io.RTABReq_st1   <> coreReqPipe.io.Req_st1_RTAB
  WshrAccess.io.checkReq       := coreReqPipe.io.CheckReq_WSHR
  SMshrAccess.io.missReq       <> coreReqPipe.io.Probe_SMSHR
  MshrAccess.io.missReq        <> coreReqPipe.io.MissReq_MSHR
  MshrAccess.io.missCached_st1 := coreReqPipe.io.MissCached_MSHR
  MshrAccess.io.stage1_ready  := coreReqPipe.io.st1_ready
  SMshrAccess.io.stage1_ready := coreReqPipe.io.st1_ready

  io.coreRsp <> coreReqPipe.io.CoreRsp

  // ------memRspPipe input connection------
  memRsp_Q.io.enq <> io.memRsp
  memRspPipe.io.memRsp <> memRsp_Q.io.deq
  memRspPipe.io.MSHRMissRspOutUCached := MshrAccess.io.UncacheRsp
  memRspPipe.io.MSHRMissRspOut    <> MshrAccess.io.missRspOut
  memRspPipe.io.SMSHRMissRspOut   <> SMshrAccess.io.missRspOut
  if(MMU_ENABLED){
    memRspPipe.io.MSHRMissRspOutAsid.get := MshrAccess.io.missRspOutAsid.get
    memRspPipe.io.SMSHRMissRspOutAsid.get := SMshrAccess.io.missRspOutAsid.get
  }
  memRspPipe.io.tAWayMask   := TagAccess.io.waymaskReplacement_st1
  memRspPipe.io.needReplace := TagAccess.io.needReplace.get
  memRspPipe.io.memReq_ready := MemReqArb.io.in(0).ready

  //mem Rsp pipe output connection
  TagAccess.io.allocateWrite      <> memRspPipe.io.tAAllocateWriteReq
  ReplayTable.io.RTABUpdate       <> memRspPipe.io.RTABUpdateReq
  MshrAccess.io.missRspIn         <> memRspPipe.io.MSHRMissRsp
  SMshrAccess.io.missRspIn        <> memRspPipe.io.SMSHRMissRsp
  WshrAccess.io.popReq            <> memRspPipe.io.WSHRPopReq
  //rtab
  ReplayTable.io.mshrFull := MshrAccess.io.full
  ReplayTable.io.LRexist  := SMshrAccess.io.probeOut_st1.LRexist
  // bfs4096-006 fix: 让 L1RTAB 看到 fill timing。fillConflict 类型的 replay 必须等
  // fill 完 (refillWrite_intent=0) 才 inject，避免 fill 多拍写 dA 时反复 livelock。
  // 用 intent (=st1_valid) 而非 valid (=st1_valid && st1_ready)，与 coreReqPipe 一致破组合环。
  ReplayTable.io.refillWrite_valid := memRspPipe.io.dAmemRsp_wReq_intent
  // bfs4096-009 fix: 真实 fill commit 给 RTAB ReadMissFillWait per-entry sticky fillCommitted
  ReplayTable.io.fillCommit_valid     := memRspPipe.io.dAmemRsp_wReq_valid
  ReplayTable.io.fillCommit_blockAddr := memRspPipe.io.dAmemRsp_wReq_blockAddr
  ReplayTable.io.pushedWSHRIdxUpdate.valid := WshrAccess.io.pushReq.valid
  ReplayTable.io.pushedWSHRIdxUpdate.bits.wshrIdx  := WshrAccess.io.pushedIdx
  ReplayTable.io.pushedWSHRIdxUpdate.bits.RTABIdx  := RTAB_pushedIdx_st2.io.deq.bits


  // dAmemRsp_wReq       
  // dAmemRsp_wReq_valid 
  // dAReplace_rReq      
  // dAReplace_rReq_valid
  // memReq_valid        
  // tAWayMask           
  // needReplace         
  // memReq_ready

  TagAccess.io.allocateWriteTagSRAMWValid_st1 := memRspPipe.io.dAmemRsp_wReq_valid
 TagAccess.io.allocateWriteData_st1 := get_tag(memRspPipe.io.dAmemRsp_wReq_blockAddr)
  // tag access
  //mshr
  MshrAccess.io.stage2_ready  := MemReqArb.io.in(1).ready
  SMshrAccess.io.stage2_ready := MemReqArb.io.in(1).ready

  // memory request arbiter
  // in(0): 来自 MemRspPipe 的 dirty replace 写回
  // in(1): 来自 CoreReqPipe 的 miss / uncached evict / flush / invalidate
  // 两路统一仲裁后进入 memReq_Q，再由 st3 作为单发射缓冲送到 io.memReq。
  MemReqArb.io.in(1) <> coreReqPipe.io.MissReq_Mem
  MemReqArb.io.in(0).valid := memRspPipe.io.memReq_valid
  MemReqArb.io.in(0).bits := dirtyReplaceMemReq
  replaceMemReqFire := MemReqArb.io.in(0).fire
  memReq_Q.io.enq <> MemReqArb.io.out
  // todo NOT right!!
  RTAB_pushedIdx_st2.io.enq.valid := MemReqArb.io.out.valid
  RTAB_pushedIdx_st2.io.enq.bits  := ReplayTable.io.RTABpushedIdx
  RTAB_pushedIdx_st2.io.deq.ready := memReq_Q.io.deq.ready
  // bfs4096-001 partial-write clobber fix: 改用 PutPartialData + 字节级 mask。
  // 旧实现用 PutFullData + 全 1 mask，会让某 SM 的整行写回覆盖别的 SM 在同一
  // cacheline 不同 byte 上合法写入的内容（多 SM 共享 cacheline 时累积污染）。
  dirtyReplaceMemReq.a_opcode := TLAOp_PutPart
  dirtyReplaceMemReq.a_param := 0.U//regular write
  dirtyReplaceMemReq.a_source := DontCare//wait for WSHR
  // Sel 的 false 分支：hold 寄存器尚未 valid 时直接走 live 信号。
  // 同上注释——_st1 信号已是 SRAM resp 那拍的 wire，三件套都不能 RegNext。
  val replaceDataSel = Mux(replaceDataValid, replaceDataReg, VecInit(DataAccessReadSRAMRRsp))
  val replaceAddrSel = Mux(replaceDataValid, replaceAddrReg, TagAccess.io.a_addrReplacement_st1.get)
  val replaceMaskSel = Mux(replaceDataValid, replaceMaskReg, TagAccess.io.replace_dirty_mask_st1)
  dirtyReplaceMemReq.a_addr.get := replaceAddrSel
  if(MMU_ENABLED){
    // asidReplacement_st1 同样是 SRAM resp 直出 wire，与 addr/mask 同时机更新，不能 RegNext。
    dirtyReplaceMemReq.Asid.get := TagAccess.io.asidReplacement_st1.get
  }
  dirtyReplaceMemReq.a_mask := replaceMaskSel.asTypeOf(Vec(BlockWords, UInt(BytesOfWord.W)))
  dirtyReplaceMemReq.a_data := replaceDataSel//wait for data SRAM in next cycle
  dirtyReplaceMemReq.hasCoreRsp := false.B
  dirtyReplaceMemReq.coreRspInstrId := DontCare
  dirtyReplaceMemReq.activeMask := VecInit(Seq.fill(NLanes)(false.B))
  dirtyReplaceMemReq.spike_info.foreach{ _ := DontCare }

  // mem request
  // memReq_Q 是内部请求队列，deq.bits 是“当前队头”的 live 视图；
  // io.memReq.get 则是对外真正发射的 1-entry launch buffer。
  // 因此 deq.valid 只表示“队头存在”，不代表“这条请求已经作为当前待发请求锁存到 st3”。
  val coreRsp_st2_valid_from_memReq = Wire(Bool())
  val waitTLB = if(MMU_ENABLED) Some(RegInit(0.U(2.W))) else None
  val waitTLBnext = if(MMU_ENABLED) Some(Wire(UInt(2.W))) else None
  val memReq_st3 = Reg(new DCacheMemReq)
  val memReq_st3_ready_tlb = if(MMU_ENABLED) Some(Wire(Bool())) else None
  val memReq_st3_valid_tlb = if(MMU_ENABLED) Some(Wire(Bool())) else None

  // memReq_st3: 暂存这条待发 memReq 的主体字段（opcode / param / data / mask / spike_info）。
  // memReq_st3_addr: 最终真正发到总线上的地址。
  // 在开启 TLB 时，主体字段可先锁存，物理地址要等 TLB 返回后再写入该寄存器；
  // 在不开 TLB 时，它与当前拍锁存到 memReq_st3.a_addr 的值保持一致，但仍单独保留以统一两种路径。
  val memReq_st3_addr = if(MMU_ENABLED) Some(Reg(UInt(paLen.W))) else Some(Reg(UInt(vaLen.W)))
  // memReq_st3_source: 表示与 memReq_st3 / memReq_st3_addr 对应的“同一条 staged memReq”的 source。
  // 在不开 TLB 时，如果继续把 source 放在 memReq_st3.a_source 中，并且同样只在 memReq_Q.io.deq.fire 时锁存，
  // 功能上也可以等价；这里独立成寄存器，是为了明确它和 a_addr 一样属于 st3 已锁存请求，
  // 避免从 memReq_Q.io.deq.bits 这个 live queue head 直接读取 source，造成地址 / source 串线。
  // Keep TL source aligned with the separately staged request address.
  val memReq_st3_source = Reg(UInt(io.memReq.get.bits.a_source.getWidth.W))
  val a_op_st3 = memReq_Q.io.deq.bits.a_opcode//memReq_Q.io.deq.bits.a_opcode
  val a_op_st3_isFlush = a_op_st3 === 5.U
  val memReqIsWrite_st3 = (a_op_st3 === TLAOp_PutFull) || ((a_op_st3 === TLAOp_PutPart) && memReq_Q.io.deq.bits.a_param === 0.U)
  val memReqIsRead_st3 = (a_op_st3 === TLAOp_Get) && memReq_Q.io.deq.bits.a_param === 0.U

  //memReq_Q.io.deq.bits.a_addr >> (WordLength - TagBits - SetIdxBits)
  val wshrProtect = WshrAccess.io.conflict && (memReqIsWrite_st3 || memReqIsRead_st3) && memReq_Q.io.deq.valid// && io.memReq.ready
  val cRspBlockedOrWshrFull = ((!coreReqPipe.io.st2_ready && memReq_Q.io.deq.bits.hasCoreRsp)
    || !WshrAccess.io.pushReq.ready) && memReqIsWrite_st3
  val wshrPass = !wshrProtect && !cRspBlockedOrWshrFull
  val PushWshrValid = wshrPass && memReq_Q.io.deq.fire && memReqIsWrite_st3
  // val WshrPushPopConflict = PushWshrValid && WshrAccess.io.popReq.valid
  // val wshrPushPopConflictReg = RegNext(WshrPushPopConflict)
    val pushReqbA = Wire(UInt((paLen - dcache_BlockOffsetBits - dcache_WordOffsetBits).W))//memReq_st3_addr.get >> (dcache_WordOffsetBits+dcache_BlockOffsetBits)
  // val pushReqbAReg = RegNext(pushReqbA)
  WshrAccess.io.pushReq.bits.blockAddr := pushReqbA//Mux(wshrPushPopConflictReg, pushReqbAReg, pushReqbA)
  if(MMU_ENABLED){
    pushReqbA := io.TLBRsp.get.bits.paddr  >> (dcache_WordOffsetBits+dcache_BlockOffsetBits)
  }
  else{
     pushReqbA := memReq_Q.io.deq.bits.a_addr.get  >> (dcache_WordOffsetBits+dcache_BlockOffsetBits)
  }
  WshrAccess.io.pushReq.valid := PushWshrValid//Mux(wshrPushPopConflictReg,true.B,Mux(WshrPushPopConflict,false.B,PushWshrValid))//wshrPass && memReq_Q.io.deq.fire() && memReqIsWrite_st3
  coreRsp_st2_valid_from_memReq := WshrAccess.io.pushReq.valid && memReq_Q.io.deq.bits.hasCoreRsp && !memRspPipe.io.memRsp_coreRsp.valid
  val perfCoreReqFire =
    coreReqPipe.io.perfReqFire &&
      !coreReqPipe.io.perfReqFromReplay &&
      !coreReqPipe.io.perfIsUncached
  val perfReadFire = perfCoreReqFire && coreReqPipe.io.perfIsRead
  val perfWriteFire = perfCoreReqFire && coreReqPipe.io.perfIsWrite
  val perfReadMissFire = perfReadFire && !coreReqPipe.io.perfIsHit
  val perfWriteMissFire = perfWriteFire && !coreReqPipe.io.perfIsHit
  val perfMshrStatus = MshrAccess.io.probeOut_st1.probeStatus
  val perfWshrHit = WshrAccess.io.checkresult.Hit
  val perfReadPrimaryMissFire =
    perfReadMissFire &&
      !perfWshrHit &&
      (perfMshrStatus === MSHRStatus.PrimaryAvail)
  val perfReadSecondaryMissFire =
    perfReadMissFire &&
      ((perfMshrStatus === MSHRStatus.SecondaryAvail) || perfWshrHit)
  val perfReadPrimaryFullMissFire =
    perfReadMissFire &&
      (perfMshrStatus === MSHRStatus.PrimaryFull)
  val perfReadSecondaryFullMissFire =
    perfReadMissFire &&
      (perfMshrStatus === MSHRStatus.SecondaryFull)
  val perfWriteFreshMissFire =
    perfWriteMissFire &&
      !perfWshrHit &&
      (perfMshrStatus === MSHRStatus.PrimaryAvail)
  val perfWriteInflightMissFire =
    perfWriteMissFire &&
      (perfWshrHit ||
        (perfMshrStatus === MSHRStatus.SecondaryAvail) ||
        (perfMshrStatus === MSHRStatus.SecondaryFull))
  val perfMissBlockedByMSHRFull =
    io.perfEnable &&
      coreReqPipe.io.st1_valid &&
      !coreReqPipe.io.perfReqFromReplay &&
      !coreReqPipe.io.coreReq_Control_st1.isUncached &&
      (coreReqPipe.io.coreReq_Control_st1.isRead || coreReqPipe.io.coreReq_Control_st1.isWrite) &&
      !coreReqPipe.io.CacheHit_st1 &&
      ((MshrAccess.io.probeOut_st1.probeStatus === MSHRStatus.PrimaryFull) ||
        (MshrAccess.io.probeOut_st1.probeStatus === MSHRStatus.SecondaryFull))
  val perfDistinctBanks = PopCount(Cat(perfBankEn.io.perBankValid))
  val perfActiveLanes = PopCount(Cat(coreReqPipe.io.perLaneAddr_st1.map(_.activeMask)))
  val perfBankConflictFire =
    perfCoreReqFire &&
      coreReqPipe.io.perfIsHit &&
      (perfActiveLanes > perfDistinctBanks)
  when(io.perfReset){
    totalReqCnt := 0.U
    readReqCnt := 0.U
    writeReqCnt := 0.U
    readMissCnt := 0.U
    writeMissCnt := 0.U
    readPrimaryMissCnt := 0.U
    readSecondaryMissCnt := 0.U
    readPrimaryFullMissCnt := 0.U
    readSecondaryFullMissCnt := 0.U
    writeFreshMissCnt := 0.U
    writeInflightMissCnt := 0.U
    replacementCnt := 0.U
    dirtyWritebackCnt := 0.U
    mshrFullCyclesCnt := 0.U
    rtabReplayCnt := 0.U
    bankConflictCyclesCnt := 0.U
    coreReqPipePipelinedCnt := 0.U
    memRspPipeDecoupledCnt := 0.U
  }.otherwise{
    when(perfCoreReqFire){
      totalReqCnt := totalReqCnt + 1.U
    }
    when(perfReadFire){
      readReqCnt := readReqCnt + 1.U
    }
    when(perfWriteFire){
      writeReqCnt := writeReqCnt + 1.U
    }
    when(perfReadMissFire){
      readMissCnt := readMissCnt + 1.U
    }
    when(perfWriteMissFire){
      writeMissCnt := writeMissCnt + 1.U
    }
    when(perfReadPrimaryMissFire){
      readPrimaryMissCnt := readPrimaryMissCnt + 1.U
    }
    when(perfReadSecondaryMissFire){
      readSecondaryMissCnt := readSecondaryMissCnt + 1.U
    }
    when(perfReadPrimaryFullMissFire){
      readPrimaryFullMissCnt := readPrimaryFullMissCnt + 1.U
    }
    when(perfReadSecondaryFullMissFire){
      readSecondaryFullMissCnt := readSecondaryFullMissCnt + 1.U
    }
    when(perfWriteFreshMissFire){
      writeFreshMissCnt := writeFreshMissCnt + 1.U
    }
    when(perfWriteInflightMissFire){
      writeInflightMissCnt := writeInflightMissCnt + 1.U
    }
    when(TagAccess.io.replaceValidVictim_st1.get){
      replacementCnt := replacementCnt + 1.U
    }
    when(replaceMemReqFire){
      dirtyWritebackCnt := dirtyWritebackCnt + 1.U
    }
    when(perfMissBlockedByMSHRFull){
      mshrFullCyclesCnt := mshrFullCyclesCnt + 1.U
    }
    when(ReplayTable.io.coreReq_replay.fire){
      rtabReplayCnt := rtabReplayCnt + 1.U
    }
    when(perfBankConflictFire){
      bankConflictCyclesCnt := bankConflictCyclesCnt + 1.U
    }
    // CoreReqPipe pipelining engaged: ≥2 stages busy at once (st0 enqueueing
    // while st1 is processing the previous request). v1 had a single
    // combined probe stage so this couldn't happen.
    when(io.perfEnable && coreReqPipe.io.st0_valid && coreReqPipe.io.st1_valid){
      coreReqPipePipelinedCnt := coreReqPipePipelinedCnt + 1.U
    }
    // MemRspPipe decoupling engaged: same cycle the side-pipe writes the
    // refilled block into the data SRAM AND emits a coreRsp. In v1 these
    // shared a stage and could not co-occur.
    when(io.perfEnable && memRspPipe.io.dAmemRsp_wReq_valid && memRspPipe.io.memRsp_coreRsp.valid){
      memRspPipeDecoupledCnt := memRspPipeDecoupledCnt + 1.U
    }
  }
  MMU_ENABLED match{
    case true =>{
      memReq_Q.io.deq.ready := Mux(a_op_st3_isFlush,io.memReq.get.ready && !memRspPipe.io.memRsp_coreRsp.valid,(waitTLB.get === 2.U) && (waitTLBnext.get === 0.U))
      waitTLBnext.get := waitTLB.get
      waitTLB.get := waitTLBnext.get
      when(waitTLB.get === 0.U){

        when(memReq_Q.io.deq.valid  && !a_op_st3_isFlush && io.TLBReq.get.ready){
          waitTLBnext.get := 1.U
        }.otherwise{
          waitTLBnext.get := waitTLB.get
        }
      }.elsewhen(waitTLB.get === 1.U){
        when(io.TLBRsp.get.valid){
          waitTLBnext.get := 2.U
        }.otherwise{
          waitTLBnext.get := waitTLB.get
        }
      }.elsewhen(waitTLB.get === 2.U){
        when(wshrPass && io.memReq.get.ready && !memRspPipe.io.memRsp_coreRsp.valid){
          waitTLBnext.get := 0.U
        }.otherwise{
          waitTLBnext.get := waitTLB.get
        }
      }.otherwise{
        waitTLBnext.get := 0.U
      }
      io.TLBRsp.get.ready := waitTLB.get === 1.U
      io.TLBReq.get.valid := memReq_Q.io.deq.valid && waitTLB.get === 0.U && !a_op_st3_isFlush
      io.TLBReq.get.bits.vaddr := memReq_Q.io.deq.bits.a_addr.get
      io.TLBReq.get.bits.asid := memReq_Q.io.deq.bits.Asid.get
      memReq_st3_ready_tlb.get := io.TLBReq.get.ready && waitTLB.get === 0.U
      memReq_st3_valid_tlb.get := io.TLBRsp.get.valid && waitTLB.get === 1.U

      when(memReq_Q.io.deq.valid && (memReq_st3_ready_tlb.get || a_op_st3_isFlush)) {
        memReq_st3.a_data := memReq_Q.io.deq.bits.a_data
        memReq_st3.a_param := memReq_Q.io.deq.bits.a_param
        memReq_st3.a_addr.get := memReq_Q.io.deq.bits.a_addr.get
        memReq_st3.a_mask := memReq_Q.io.deq.bits.a_mask
        memReq_st3.a_opcode := memReq_Q.io.deq.bits.a_opcode
        memReq_st3_source := memReq_Q.io.deq.bits.a_source
        memReq_st3.spike_info.foreach{ _ := memReq_Q.io.deq.bits.spike_info.getOrElse(0.U) }
      }
      when(memReq_st3_valid_tlb.get){
        // 开 TLB 时，最终发出的地址来自这里的物理地址，而不是之前暂存的虚地址。
        memReq_st3_addr.get := io.TLBRsp.get.bits.paddr
      }


    }
    case false => {
      memReq_Q.io.deq.ready := wshrPass && io.memReq.get.ready && !memRspPipe.io.memRsp_coreRsp.valid
      when(wshrPass && memReq_Q.io.deq.fire) {
        // 不开 TLB 时，st3 主体字段、st3_addr、st3_source 都在 deq.fire 当拍锁存，
        // 三者描述的是同一条请求，只是输出时仍显式使用独立的 addr/source 寄存器。
        memReq_st3.a_data := memReq_Q.io.deq.bits.a_data
        memReq_st3.a_param := memReq_Q.io.deq.bits.a_param
        memReq_st3.a_addr.get := memReq_Q.io.deq.bits.a_addr.get
        memReq_st3.a_mask := memReq_Q.io.deq.bits.a_mask
        memReq_st3.a_opcode := memReq_Q.io.deq.bits.a_opcode
        memReq_st3_source := memReq_Q.io.deq.bits.a_source
        memReq_st3.spike_info.foreach{ _ := memReq_Q.io.deq.bits.spike_info.getOrElse(0.U) }
        memReq_st3_addr.get := memReq_Q.io.deq.bits.a_addr.get
      }
    }
  }
  //memReq_Q.io.deq.ready := Mux(a_op_st3_isFlush,io.memReq.get.ready && !coreRsp_st2_valid_from_memRsp,(waitTLB === 2.U) && (waitTLBnext === 0.U))//wshrPass && io.memReq.ready && !coreRsp_st2_valid_from_memRsp

  // FSM for TLB handle and memreq transmit
  // 0-idle 1-wait TLB resp 2-issue memreq

  val memReqSetIdx_st2 = memReq_Q.io.deq.bits.a_addr.get(WordLength - TagBits -1,WordLength - TagBits - SetIdxBits)
  when(memReqIsWrite_st3 && memReq_Q.io.deq.fire){
    // write miss 的 TL source 要等真正 deq.fire、并拿到 WSHR pushedIdx 后才能最终确定。
    memReq_st3_source := Cat("d0".U, WshrAccess.io.pushedIdx, memReqSetIdx_st2)
    //memReq_st3.a_source := Cat("d0".U, 0.U((log2Up(NMshrEntry)-log2Up(NWshrEntry)).W), WshrAccess.io.pushedIdx, coreReq_st1.setIdx)
  }.elsewhen(memReqIsRead_st3 && memReq_Q.io.deq.fire){
    // read miss / special miss 的 source 在入 memReq_Q 前就已确定；
    // 这里在真正 deq.fire 时锁存，保证它和当前 staged 的 addr / data 对齐。
    memReq_st3_source := memReq_Q.io.deq.bits.a_source
  }

  val coreRspFromMemReq = Wire(new DCacheCoreRsp)
  coreReqPipe.io.memReq_coreRsp.bits := coreRspFromMemReq
  coreReqPipe.io.memReq_coreRsp.valid := coreRsp_st2_valid_from_memReq
  coreRspFromMemReq.data := DontCare
  coreRspFromMemReq.isWrite := true.B
  //st指令的regIdx对SM流水线提交级无意义，且memReq_Q没有传输该数据的通道
  coreRspFromMemReq.instrId := memReq_Q.io.deq.bits.coreRspInstrId
  coreRspFromMemReq.activeMask := memReq_Q.io.deq.bits.activeMask
  // memReq(st3)
  io.memReq.get.bits := memReq_st3
  io.memReq.get.bits.a_addr.get := memReq_st3_addr.get
  io.memReq.get.bits.a_source := memReq_st3_source

  // memReq_valid 表示 st3 launch buffer 当前是否持有一条尚未从 io.memReq 发走的请求。
  // memReq_Q.io.deq.fire 和 io.memReq.get.fire 可能错拍，因此不能直接把 deq.valid 透传到 io.memReq.get.valid。
  val memReq_valid = RegInit(false.B)
  when(memReq_Q.io.deq.fire ^ io.memReq.get.fire){
    memReq_valid := memReq_Q.io.deq.fire
  }
  io.memReq.get.valid := memReq_valid
  io.perf.totalReq := totalReqCnt
  io.perf.readReq := readReqCnt
  io.perf.writeReq := writeReqCnt
  io.perf.readMiss := readMissCnt
  io.perf.writeMiss := writeMissCnt
  io.perf.readPrimaryMiss := readPrimaryMissCnt
  io.perf.readSecondaryMiss := readSecondaryMissCnt
  io.perf.readPrimaryFullMiss := readPrimaryFullMissCnt
  io.perf.readSecondaryFullMiss := readSecondaryFullMissCnt
  io.perf.writeFreshMiss := writeFreshMissCnt
  io.perf.writeInflightMiss := writeInflightMissCnt
  io.perf.replacements := replacementCnt
  io.perf.dirtyWritebacks := dirtyWritebackCnt
  io.perf.mshrFullCycles := mshrFullCyclesCnt
  io.perf.rtabReplays := rtabReplayCnt
  io.perf.bankConflictCycles := bankConflictCyclesCnt
  io.perf.coreReqPipePipelinedCycles := coreReqPipePipelinedCnt
  io.perf.memRspPipeDecoupledCycles := memRspPipeDecoupledCnt
  // print 
  if(DCACHE_DEBUG){
    when(io.coreReq.fire){
      printf(p"---- REQ: \n instrId = ${io.coreReq.bits.instrId}, opcode = ${io.coreReq.bits.opcode},tag=${Hexadecimal(io.coreReq.bits.tag)},")
      printf(p"set=${Hexadecimal(io.coreReq.bits.setIdx)},data0 = ${Hexadecimal(io.coreReq.bits.data(0))}\n")
    }    
    when(io.coreRsp.valid){
      printf(p"++++ RSP: \n instrId = ${io.coreRsp.bits.instrId}, data0 = ${Hexadecimal(io.coreRsp.bits.data(0))}\n")
    }
    when(io.memReq.get.fire){
      printf(p"===mem Req from cache addr = ${Hexadecimal(io.memReq.get.bits.a_addr.get)}, opcode = ${io.memReq.get.bits.a_opcode}, param = ${io.memReq.get.bits.a_param}\n")
    }
  }
}

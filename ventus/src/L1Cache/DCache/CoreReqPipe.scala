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

import L1Cache.DCache._
import SRAMTemplate._
import chisel3._
import chisel3.util._
import config.config.Parameters
import top.parameters._
import mmu.SV32.{asidLen, paLen, vaLen}

class CoreReqPipe_st1(implicit p: Parameters) extends DCacheBundle{
  val Req  = new DCacheCoreReq
  val Ctrl = new DCacheControl
  val fromReplay = Bool()
}
class CoreRspPipe_st2(implicit p: Parameters) extends DCacheBundle{
  val Rsp = new DCacheCoreRsp_d
  val perLaneAddr = Vec(NLanes, new DCachePerLaneAddr)
  val validFromCoreReq = Bool()
  val readHitSnapshotValid = Bool()
  val readHitSnapshotData = Vec(BlockWords, UInt(WordLength.W))
}
class CoreReqPipe(implicit p: Parameters) extends DCacheModule{
  val io = IO(new Bundle{
    //st0
    val CoreReq        = Flipped(DecoupledIO(new DCacheCoreReq))
    val RTABHit        = Input(Bool())
    val hasDirty       = Input(Bool())
    val MSHREmpty      = Input(Bool())
    val SMSHREmpty     = Input(Bool())
    val fillPipeDrained = Input(Bool())   // bfs4096-008 §9 drain-before-invalidate (cached-read fill pipe 已空)
    val tA_dirtySetIdx_st0 = Input(UInt(dcache_SetIdxBits.W))
    val tA_dirtyWayMask_st0= Input(UInt(dcache_NWays.W))
    val reqSource      = Input(Bool()) // 1- from RTAB 0 - from io
    // dirty replace 期间暂停 coreReqPipe，避免 victim line write-hit 干扰写回数据
    val blockCoreReq   = Input(Bool())
    // memRspPipe 正在对 dA 写回(refill)时的 blockAddr，用于规避与 st0 同拍访问同一 cacheline
    val refillWrite_valid = Input(Bool())
    val refillWrite_blockAddr = Input(UInt(bABits.W))
    // bfs4096-009 fix: 真实 fill commit (=memRspPipe.dAmemRsp_wReq_valid，含 st1_ready，非上面 intent 的 refillWrite_valid)
    // + blockAddr。给 ReadMissFillWait 的 commit-seen latch(覆盖 RTAB_full / st1 stall 期间已 commit 的 edge)。
    val fillCommit_valid = Input(Bool())
    val fillCommit_blockAddr = Input(UInt(bABits.W))
    // bfs4096-006 fix: refill 写入的精确 dA row id = Cat(set, victim_way)。
    // 现有 refillWrite_blockAddr 是 tag+set 只挡同 cacheline，但本 bug 是
    // 跨 cacheline (不同 tag) 同 (set, way) 物理 row 撞 → 必须按 row 比较。
    val refillWrite_setIdx = Input(UInt(log2Ceil(NSets * NWays).W))
    val refillWrite_asid = if(MMU_ENABLED) Some(Input(UInt(asidLen.W))) else None
    // MSHR missRspIn 处理期间的“原子态”指示：同拍 mshrStatus 尚未更新，外部不应插入同块的 secondary miss
    val mshrReleasing_valid = Input(Bool())
    val mshrReleasing_blockAddr = Input(UInt(bABits.W))
    val mshrReleasing_asid = if(MMU_ENABLED) Some(Input(UInt(asidLen.W))) else None

    val Probe_MSHR     = Output(new MSHRprobe(bABits, asidLen))
    val probeAsid      = if(MMU_ENABLED) {Some(Output(UInt(asidLen.W)))} else None
    val Probe_tA       = Output(new SRAMBundleA(NSets))  // todo have ready issue
    val Probe_tA_ready = Input(Bool())
    val Req_st0_RTAB   = Valid(new RTABReq())
    val flushDirty_tA  = Output(Bool())

    val st0_ready      = Output(Bool())
    val st0_valid      = Output(Bool())

    //st1
    
    val tA_Hit_st1          = Input(new hitStatus(NWays, TagBits))
    val tA_dirtyTag_st1     = Input(UInt(TagBits.W))
    // bfs4096-001 partial-write clobber fix: byte-level dirty mask of the chosen flush victim
    val tA_dirtyMask_st1    = Input(UInt((BlockWords * BytesOfWord).W))
    val tA_dirtyAsid_st1    = if(MMU_ENABLED) {Some(Input(UInt(asidLen.W)))} else None
    val MSHR_ProbeStatus    = Input(new MSHRprobeOut(NMshrEntry, NMshrSubEntry))
    val SMSHR_ProbeStatus   = Input(new SMSHRprobeOut(NMshrEntry))
    val WSHR_CheckResult    = Input(new WSHRCheckResult(NWshrEntry))
    val Mshr_st1_ready      = Input(Bool())
    val RTAB_full           = Input(Bool())
    val memRsp_coreRsp      = Flipped(DecoupledIO(new CoreRspPipe_st2))

    val tagFromCore_tA_st1  = Output(UInt(dcache_TagBits.W))
    val asidFromCore_tA_st1 = if(MMU_ENABLED) {Some(Output(UInt(asidLen.W)))} else None
    val coreReq_Control_st1 = Output(new DCacheControl)
    val perLaneAddr_st1     = Output(Vec(NLanes, new DCachePerLaneAddr))
    val read_Req_dA         = ValidIO(Vec(BlockWords,new SRAMBundleA(NSets*NWays)))
    val CacheHit_st1        = Output(Bool())
    val Req_st1_RTAB        = ValidIO(new RTABReq())
    // nn64k-009 fix: st1 的 raw RTAB-slot 占用意图，不带 st1_ready gate。
    // DCachev2.allowIn1 用它在 RTAB almost_full 时为当前 st1 请求预留最后一格。
    // 不能用 Req_st1_RTAB.valid(=Req_RTAB_st1_valid && st1_ready)：st1_ready 在
    // mshrReleasingSameBlock_st1 等 hold 拍被拉低(L660-662)，会把真实 park 意图掩盖成 0，
    // 让外部 in(1) 的 st0-hitRTAB 路径占掉最后一格 → 闭合 st1⇄RTAB 死锁环。
    val Req_st1_RTAB_reserve = Output(Bool())
    val CheckReq_WSHR       = Output(new WSHRreq)
    // {S}MSHRmissReq.instrId width = max(WIdBits, log2Up(NMshrEntry)) to allow
    // NMshrEntry > num_warp; see bugs/bfs4096-003/phase_4_report.md.
    val Probe_SMSHR         = DecoupledIO(new SMSHRmissReq(bABits, tIBits, math.max(WIdBits, log2Up(NMshrEntry)), asidLen))//TODO add special MSHR
    val MissReq_MSHR        = DecoupledIO(new MSHRmissReq(bABits, tIBits, math.max(WIdBits, log2Up(NMshrEntry)), asidLen))
    val MissCached_MSHR     = Output(Bool())
    val st1_valid           = Output(Bool())
    val st1_ready           = Output(Bool())
    // missReq_Mem for read write miss and flu inv dirty write back
    val MissReq_Mem         = DecoupledIO(new WshrMemReqV2)
    val WriteReq_dA         = Output(Vec(BlockWords, new SRAMBundleAW(UInt(8.W), NSets * NWays, BytesOfWord)))
    val WriteReq_dA_valid   = Output(Vec(BlockWords,Bool()))
    val WriteHit_st1        = Output(Bool())

    //st2
    val dA_data        = Input(Vec(BlockWords, UInt(WordLength.W)))
    val memReq_coreRsp = Flipped(DecoupledIO(new DCacheCoreRsp))

    val CoreRsp = DecoupledIO(new DCacheCoreRsp)

    val memRspIsFlu = Input(Bool())
    val st2_ready = Output(Bool())
    val invalidate_tA = Output(Bool())

    val perfReqFire       = Output(Bool())
    val perfReqFromReplay = Output(Bool())
    val perfIsRead        = Output(Bool())
    val perfIsWrite       = Output(Bool())
    val perfIsUncached    = Output(Bool())
    val perfIsHit         = Output(Bool())
  })

  //====== st0 =======
  val st0_valid = Wire(Bool()) // for enqueue st1 pipe reg
  val st0_ready = Wire(Bool()) // for dequeue st0 pipe reg
  //Control Generate
  val Control = Module(new genControl)

  //====== st1 =======
  val CoreReq_pipeReg_st0_st1 = Module(new Queue(new CoreReqPipe_st1,entries = 1,flow=false,pipe=true)).io
  val ReplayType = Wire(UInt(4.W))
  val MshrIdx = Wire(UInt(log2Up(NMshrEntry).W))
  val WshrIdx = Wire(UInt(log2Up(NWshrEntry).W))
  val MshrStatus = Wire(UInt(3.W))
  val st1_valid = Wire(Bool())
  val st1_ready = Wire(Bool())
  // missReq st1
  val missMemReq_st1   = Wire(new WshrMemReqV2)
  val evictMemReq_st1  = Wire(new WshrMemReqV2)
  val FluInvMemReq_st1 = Wire(new WshrMemReqV2)
  val missMemReq_valid    = Wire(Bool())
  val evictMemReq_valid   = Wire(Bool())
  val FluInvMemReq_valid  = Wire(Bool())

  //====== st2 ======
  val CoreRsp_pipeReg_st1_st2 = Module(new Queue(new CoreRspPipe_st2,entries = 1, pipe = true,flow = false)).io
  val DataMemOrder_st2 = Wire(Vec(BlockWords, UInt(WordLength.W)))
  val DataCoreOrder_st2 = Wire(Vec(NLanes, UInt(WordLength.W)))
  val coreReq_st2_ready = Wire(Bool())
  // st3
  val coreRsp_Q_entries: Int = NLanes
  val CoreRsp_st3 = Module(new Queue(new DCacheCoreRsp,entries = coreRsp_Q_entries,flow=false,pipe=false))

  // submodule
  val OpcodeGen = Module(new ProConver)
  val addrGen = Module(new dataReqCrossBar)

  //=== st0 ===
  // tagaccess: proberead request, for tA(SRAM) to get the tags in the corresponding set and way
  // MSHR: probe, get the MSHR status, will decide the behavier in st1
  // RTAB: check hit status, if hit in RTAB, will go directly into RTAB without enqueue the st1 pipe reg
  val CoreReqControl_st0 = Control.io.control
  Control.io.opcode := io.CoreReq.bits.opcode
  Control.io.param  := io.CoreReq.bits.param

  val BlockAddr_st0 = Cat(io.CoreReq.bits.tag, io.CoreReq.bits.setIdx)
  val BlockAddr_st1 = Cat(CoreReq_pipeReg_st0_st1.deq.bits.Req.tag, CoreReq_pipeReg_st0_st1.deq.bits.Req.setIdx)
  val refillSameBlock_st0 =
    io.refillWrite_valid &&
      (io.refillWrite_blockAddr === BlockAddr_st0) &&
      (if(MMU_ENABLED) (io.refillWrite_asid.get === io.CoreReq.bits.asid.get) else true.B)
  val pendingReadMissSameBlock_st0 =
    CoreReq_pipeReg_st0_st1.deq.valid &&
      CoreReq_pipeReg_st0_st1.deq.bits.Ctrl.isRead &&
      !io.tA_Hit_st1.hit &&
      (BlockAddr_st0 === BlockAddr_st1) &&
      (if(MMU_ENABLED) (io.CoreReq.bits.asid.get === CoreReq_pipeReg_st0_st1.deq.bits.Req.asid.get) else true.B)

  //output bits
  io.Probe_MSHR.blockAddr := BlockAddr_st0
  if(MMU_ENABLED){
    io.probeAsid.get :=io.CoreReq.bits.asid.get
  }
  io.Probe_tA.setIdx := io.CoreReq.bits.setIdx
  io.Req_st0_RTAB.bits.CoreReqData := io.CoreReq.bits
  io.Req_st0_RTAB.bits.ReqType     := DontCare
  io.Req_st0_RTAB.bits.mshrIdx     := DontCare
  io.Req_st0_RTAB.bits.wshrIdx     := DontCare
  io.Req_st0_RTAB.bits.fillAlreadyCommitted := false.B   // bfs4096-009: st0/hitRTAB 路径不消费此字段
  io.Req_st0_RTAB.valid            := io.RTABHit && io.CoreReq.valid && !io.reqSource
  io.CoreReq.ready := st0_ready
  //Flush L2 FSM
  val idle :: flushing :: responding :: Nil = Enum(3)
  val FlushInvstateReg = RegInit(idle)
  val FlushInvstateReg_next = WireInit(FlushInvstateReg)
  val fluInvReq_st0 = io.CoreReq.valid && (CoreReqControl_st0.isFlush || CoreReqControl_st0.isInvalidate)
  val fluInvStartOk_st0 = fluInvReq_st0 && io.MSHREmpty && io.SMSHREmpty && io.fillPipeDrained//bfs4096-008 §9: invalidate 启动须 cached-read fill pipe 已 drain (覆盖 MSHREmpty 漏的 W1/st1 窗口)
  val flushDirtyReq_st0 = fluInvStartOk_st0 && io.hasDirty && (FlushInvstateReg === idle)
  io.flushDirty_tA := flushDirtyReq_st0
  val FluInv_st1 = CoreReq_pipeReg_st0_st1.deq.bits.Ctrl.isFlush || CoreReq_pipeReg_st0_st1.deq.bits.Ctrl.isInvalidate
  val FluInvReq_st1_valid = CoreReq_pipeReg_st0_st1.deq.valid && FluInv_st1
  val FluInvIsPut_st1 = CoreReq_pipeReg_st0_st1.deq.valid && (FlushInvstateReg === idle) && FluInv_st1
  val FluInvIsFluL2_st1 =  CoreReq_pipeReg_st0_st1.deq.valid && (FlushInvstateReg === flushing) && FluInv_st1
  val FluInvL2MemReqIssuedReg = RegInit(false.B)
  val FluInvRspPendingReg = RegInit(false.B)
  val FluInvRspReqReg = Reg(chiselTypeOf(CoreReq_pipeReg_st0_st1.deq.bits.Req))
  val FluInvRspCtrlReg = Reg(chiselTypeOf(CoreReq_pipeReg_st0_st1.deq.bits.Ctrl))

  // valid ready
  // st0 st1 pipe reg enq valid
  st0_valid := false.B
  st0_ready := false.B
  when(!(io.RTABHit && !io.reqSource)){
    // probe SMSHR MSHR and tag
    when(CoreReqControl_st0.isRead || CoreReqControl_st0.isWrite|| CoreReqControl_st0.isAMO || CoreReqControl_st0.isLR || CoreReqControl_st0.isSC){
      // 避免 refill / 前一条 read miss 与当前同块请求在 st0 同拍推进，导致后续 MSHR 可见性错位
      // vecadd4096-001/nn64k-008 fix (v4 主窗口 gate, 收窄自 v2): flush/invalidate FSM 离 idle 期间, 只挡写类
      //   (isWrite 直接置 L1 way_dirty:L1TagAccess:388; isAMO/isSC 保守纳入=memory-ordering, 非 L1 dirty producer) 进 st0
      //   → 防新写 write-hit 把已 sweep 行置 dirty, 被 responding invalidateAll(L1TagAccess:446) 裸清丢 → L2 stale。
      //   read/LR 放行: 不写 way_dirty 不制造 dirty, 挡它们对 flush 原子性无益, 反 stall bfs 合法 read 引入 lost-update
      //   回归(二分坐实 v2 全挡炸 bfs 7/10; codex §1.1(e) 建议只挡写)。caveat: read-miss fill 若与 invalidateAll 同/后拍
      //   到达, allocate-valid 优先(L1TagAccess:444) 可能留 clean-valid line —— 不丢 dirty, 但 invalidate 后非保证空。
      //   ★注入本分支内(非外层覆盖) → 保留 RTAB-hit 吸收/flush 指令/WaitMSHR 控制握手(v1 外层覆盖破坏 RTAB 堵死教训)。
      st0_valid  := io.CoreReq.valid && io.Probe_tA_ready && !refillSameBlock_st0 && !pendingReadMissSameBlock_st0 && (FlushInvstateReg === idle || !(CoreReqControl_st0.isWrite || CoreReqControl_st0.isAMO || CoreReqControl_st0.isSC))
      st0_ready := CoreReq_pipeReg_st0_st1.enq.ready && io.Probe_tA_ready && !refillSameBlock_st0 && !pendingReadMissSameBlock_st0 && (FlushInvstateReg === idle || !(CoreReqControl_st0.isWrite || CoreReqControl_st0.isAMO || CoreReqControl_st0.isSC))
    }.elsewhen(CoreReqControl_st0.isWaitMSHR){
      //wait until MSHR empty
      st0_valid  := io.CoreReq.valid && io.MSHREmpty && io.SMSHREmpty
      st0_ready := CoreReq_pipeReg_st0_st1.enq.ready && io.MSHREmpty && io.SMSHREmpty
    }.elsewhen(CoreReqControl_st0.isFlush || CoreReqControl_st0.isInvalidate){
        when(FlushInvstateReg === idle){
          when(!io.MSHREmpty || !io.SMSHREmpty || !io.fillPipeDrained){//bfs4096-008 §9: fill pipe 未 drain 时 invalidate 停 st0 (仅 idle 态 gate, FSM 离 idle 后不再 gate=死锁免疫)
            st0_valid := false.B
            st0_ready := false.B
          }.elsewhen(io.hasDirty){
            //write back dirty cacheline
            st0_valid  := io.CoreReq.valid
            st0_ready := false.B
          }.otherwise{
            st0_valid := io.CoreReq.valid
            st0_ready := CoreReq_pipeReg_st0_st1.enq.ready
          }
        }.otherwise{
          st0_valid := false.B
          st0_ready := false.B
        }
    }
  }.otherwise{
    st0_valid := false.B
    st0_ready := true.B
  }
  when(io.blockCoreReq){
    st0_valid := false.B
    st0_ready := false.B
  }
  io.invalidate_tA :=
    (FlushInvstateReg === responding) && (
      (FluInvRspPendingReg && FluInvRspCtrlReg.isInvalidate) ||
        (FluInvReq_st1_valid && CoreReq_pipeReg_st0_st1.deq.bits.Ctrl.isInvalidate)
    )
  io.st0_valid := st0_valid
  io.st0_ready := st0_ready

  val fluInvCleanFire_st0 =
    st0_valid && st0_ready && fluInvReq_st0 && !io.hasDirty
  val fluInvL2MemReqFire_st1 =
    FluInvIsFluL2_st1 && !FluInvL2MemReqIssuedReg && io.MissReq_Mem.ready
  val fluInvRspPendingFire =
    FluInvRspPendingReg && (FlushInvstateReg === responding) &&
      CoreRsp_pipeReg_st1_st2.enq.ready && !io.memRsp_coreRsp.valid
  val fluInvCoreRspFire =
    fluInvRspPendingFire || (CoreReq_pipeReg_st0_st1.deq.fire && FluInv_st1 && (FlushInvstateReg === responding))
  FlushInvstateReg_next := FlushInvstateReg
  FlushInvstateReg := FlushInvstateReg_next
  when(FlushInvstateReg === idle){
    FluInvL2MemReqIssuedReg := false.B
  }.elsewhen(fluInvL2MemReqFire_st1){
    FluInvL2MemReqIssuedReg := true.B
  }.elsewhen(FlushInvstateReg === responding && fluInvCoreRspFire){
    FluInvL2MemReqIssuedReg := false.B
  }
  when(fluInvL2MemReqFire_st1){
    FluInvRspPendingReg := true.B
    FluInvRspReqReg := CoreReq_pipeReg_st0_st1.deq.bits.Req
    FluInvRspCtrlReg := CoreReq_pipeReg_st0_st1.deq.bits.Ctrl
  }.elsewhen(fluInvRspPendingFire){
    FluInvRspPendingReg := false.B
  }
  when(FlushInvstateReg === idle){
    when(fluInvCleanFire_st0 && CoreReqControl_st0.isFluInvL2){
      FlushInvstateReg_next := flushing
    }.elsewhen(fluInvCleanFire_st0){
      FlushInvstateReg_next := responding
    }
  }.elsewhen(FlushInvstateReg === flushing){
    when(io.memRspIsFlu && FluInvL2MemReqIssuedReg){
      FlushInvstateReg_next := responding
    }
  }.elsewhen(FlushInvstateReg === responding){
    when(fluInvCoreRspFire){
      FlushInvstateReg_next := idle
    }
  }

  //st0_ready := io.Probe_tA_ready && CoreReq_pipeReg_st0_st1.enq.ready
  // =============
  // st1 pipe reg
  val mshrReleasingSameBlock_st1 =
    io.mshrReleasing_valid &&
      (io.mshrReleasing_blockAddr === BlockAddr_st1) &&
      (if(MMU_ENABLED) (io.mshrReleasing_asid.get === CoreReq_pipeReg_st0_st1.deq.bits.Req.asid.get) else true.B)
  CoreReq_pipeReg_st0_st1.enq.valid := st0_valid
  CoreReq_pipeReg_st0_st1.enq.bits.Req := io.CoreReq.bits
  CoreReq_pipeReg_st0_st1.enq.bits.Ctrl := CoreReqControl_st0
  CoreReq_pipeReg_st0_st1.enq.bits.fromReplay := io.reqSource
  //=== st1 ===

  io.tagFromCore_tA_st1 := CoreReq_pipeReg_st0_st1.deq.bits.Req.tag // check the tag from core with tag from tA block
  if(MMU_ENABLED){
    io.asidFromCore_tA_st1.get := CoreReq_pipeReg_st0_st1.deq.bits.Req.asid.get
  }
  io.perLaneAddr_st1 := CoreReq_pipeReg_st0_st1.deq.bits.Req.perLaneAddr
  io.coreReq_Control_st1 := CoreReq_pipeReg_st0_st1.deq.bits.Ctrl
  val Control_st1 = CoreReq_pipeReg_st0_st1.deq.bits.Ctrl
  val fromReplay_st1 = CoreReq_pipeReg_st0_st1.deq.bits.fromReplay
  io.read_Req_dA.bits.foreach(_.setIdx := Cat(CoreReq_pipeReg_st0_st1.deq.bits.Req.setIdx,OHToUInt(io.tA_Hit_st1.waymask))) // dA r req addr
  when(flushDirtyReq_st0){
    io.read_Req_dA.bits.foreach(_.setIdx := Cat(io.tA_dirtySetIdx_st0,OHToUInt(io.tA_dirtyWayMask_st0)))
  }
  io.Req_st1_RTAB.bits.CoreReqData := CoreReq_pipeReg_st0_st1.deq.bits.Req
  io.Req_st1_RTAB.bits.ReqType     := ReplayType
  io.Req_st1_RTAB.bits.mshrIdx     := MshrIdx
  io.Req_st1_RTAB.bits.wshrIdx     := WshrIdx
  io.CheckReq_WSHR.blockAddr := BlockAddr_st1
  // FSM for evict
  val evictidle :: evictrsp:: Nil = Enum(2)
  val evictstateReg = RegInit(evictidle)
  val evictReg_next = WireInit(evictstateReg)
// 对外 memReq 一共有三类来源，优先级从高到低：
// 1. FluInvMemReq_st1: flush / invalidate 触发的 PutFull 或 Flush
// 2. evictMemReq_st1: uncached 请求命中 dirty line 时，先把 victim line 写回
// 3. missMemReq_st1: 普通 cached miss / uncached clean request / special request
// 这里先在 st1 侧统一整理成 WshrMemReqV2，再由 DCachev2 的 memReq_Q + st3 发射到外部总线。
// mem request io connection
  when(FluInvMemReq_valid){
    io.MissReq_Mem.bits := FluInvMemReq_st1
  }.elsewhen(evictMemReq_valid){
    io.MissReq_Mem.bits := evictMemReq_st1
  }.otherwise{
    io.MissReq_Mem.bits := missMemReq_st1
  }
  io.MissReq_Mem.valid := missMemReq_valid || FluInvMemReq_valid || evictMemReq_valid


  MshrIdx := io.MSHR_ProbeStatus.a_source
  WshrIdx := io.WSHR_CheckResult.HitIdx

  MshrStatus := io.MSHR_ProbeStatus.probeStatus
  //important signals
  val ReadHit_st1   = io.tA_Hit_st1.hit  && Control_st1.isRead
  val ReadMiss_st1  = !io.tA_Hit_st1.hit && Control_st1.isRead
  val WriteHit_st1  = io.tA_Hit_st1.hit  && Control_st1.isWrite
  val WriteMiss_st1 = !io.tA_Hit_st1.hit && Control_st1.isWrite
  val AMO_LR_SC_st1 = Control_st1.isAMO || Control_st1.isLR || Control_st1.isSC
  val CacheHit_st1 = io.tA_Hit_st1.hit
  val CacheMiss_st1 = !io.tA_Hit_st1.hit
  val CacheHitDirty_st1 = io.tA_Hit_st1.hit && io.tA_Hit_st1.isDirty
  val UCReqHitNDirty = io.tA_Hit_st1.hit && !io.tA_Hit_st1.isDirty && CoreReq_pipeReg_st0_st1.deq.bits.Ctrl.isUncached
  val UCReqHitDirty  = io.tA_Hit_st1.hit && io.tA_Hit_st1.isDirty && CoreReq_pipeReg_st0_st1.deq.bits.Ctrl.isUncached
  io.CacheHit_st1 := CacheHit_st1
  io.WriteHit_st1 := WriteHit_st1
  missMemReq_valid := (CacheMiss_st1 && !FluInv_st1 || UCReqHitNDirty) && CoreReq_pipeReg_st0_st1.deq.fire && !io.Req_st1_RTAB.valid && io.MSHR_ProbeStatus.probeStatus === 0.U
  // bfs4096-006 fix: 检测 hit-read 与 fill write 同拍撞同 dA row。
  // 物理上 dA SRAM 的 row 索引是 Cat(set, way)；当 hit-read 命中 way A、fill 正在写 way A，
  // 即使 tag 不同 (两条不同 cacheline)，dA SRAM 物理位置的 ownership 已经被 fill 抢走。
  // dA SRAM bypassWrite=true 会把 fresh fill data forward 给 hit-read，造成跨 cacheline 数据污染
  // (cost tag 0x90034 命中却拿到 visited 数据 0x01010101)。
  // 触发条件 [A] hit-read [B] fill 同拍 [C] Cat(set, hit_way) == Cat(set, victim_way) [D] bypassWrite=true。
  // 必须 qualify deq.valid && ReadHit_st1：否则非 hit 场景 OHToUInt(0.U)=0 会与 fill_setIdx 任何 way=0 撞误报。
  // wire 定义提到 RTAB elsewhen 链之前，使下方 elsewhen 分支可直接引用 fillConflictSt1。
  // 见 bugs/bfs4096-006/phase_4_report.md §III + §VI.4。
  val hitReadSetIdx_st1 = Cat(
    CoreReq_pipeReg_st0_st1.deq.bits.Req.setIdx,
    OHToUInt(io.tA_Hit_st1.waymask)
  )
  val fillConflictSt1 =
    CoreReq_pipeReg_st0_st1.deq.valid &&
    ReadHit_st1 &&
    io.refillWrite_valid &&
    (io.refillWrite_setIdx === hitReadSetIdx_st1)
  // bfs4096-009 fix: ReadMissFillWait trigger —— 当前 st1 read-miss 在驻留 st1 期间撞过同 block missRspIn 释放。
  // clear 分支 (!deq.valid || deq.fire) 优先 → 排除 st1 空时 stale deq.bits 误触发(codex round5/6 §A.1)；
  // mshrReleasingSameBlock_st1(CoreReqPipe:300) 本身不带 deq.valid，靠这里 + ReadMiss_st1 qualify；
  // latch straddleBlockAddr 绑定到所属 req，trigger 再复查 blockAddr 匹配(跨 req 兜底)。
  val straddledRelease_st1  = RegInit(false.B)
  val straddleBlockAddr_st1 = Reg(UInt(bABits.W))
  when(!CoreReq_pipeReg_st0_st1.deq.valid || CoreReq_pipeReg_st0_st1.deq.fire){
    straddledRelease_st1 := false.B
  }.elsewhen(ReadMiss_st1 && mshrReleasingSameBlock_st1){
    straddledRelease_st1  := true.B
    straddleBlockAddr_st1 := BlockAddr_st1
  }
  val readMissFillWait_st1 = CoreReq_pipeReg_st0_st1.deq.valid && ReadMiss_st1 &&
    ( mshrReleasingSameBlock_st1 ||
      (straddledRelease_st1 && (straddleBlockAddr_st1 === BlockAddr_st1)) )
  // bfs4096-009 fix: commit-seen —— 当前 st1 req 驻留期间见过本 block 真实 fill commit。clear 同上优先。
  // 随 RTABReq.fillAlreadyCommitted 带入(同拍 commit 用组合 OR 补)，覆盖 commit 与 enq 同拍 / commit 在 enq 前两种 edge。
  val fillCommitSeen_st1 = RegInit(false.B)
  when(!CoreReq_pipeReg_st0_st1.deq.valid || CoreReq_pipeReg_st0_st1.deq.fire){
    fillCommitSeen_st1 := false.B
  }.elsewhen(io.fillCommit_valid && (io.fillCommit_blockAddr === BlockAddr_st1)){
    fillCommitSeen_st1 := true.B
  }
  io.Req_st1_RTAB.bits.fillAlreadyCommitted := fillCommitSeen_st1 ||
    (io.fillCommit_valid && (io.fillCommit_blockAddr === BlockAddr_st1))
  // RTABReqType req
  val Req_RTAB_st1_valid = Wire(Bool())
  Req_RTAB_st1_valid := false.B
  io.Req_st1_RTAB.valid := Req_RTAB_st1_valid && st1_ready // request RTAB when st1_ready
  ReplayType := 0.U
  when(Control_st1.isUncached && CacheHitDirty_st1 && (Control_st1.isWrite || Control_st1.isAMO || Control_st1.isSC)){
    Req_RTAB_st1_valid :=CoreReq_pipeReg_st0_st1.deq.valid && (evictstateReg === evictrsp)
    ReplayType := UCacheHitDirty
  }.elsewhen(MshrStatus === SecondaryFull){
    Req_RTAB_st1_valid := CoreReq_pipeReg_st0_st1.deq.valid
    ReplayType := SubEntryFull
  }.elsewhen(ReadMiss_st1 && MshrStatus === PrimaryFull){
    Req_RTAB_st1_valid := CoreReq_pipeReg_st0_st1.deq.valid
    ReplayType := EntryFull
  }.elsewhen(Control_st1.isRead && io.WSHR_CheckResult.Hit){
    Req_RTAB_st1_valid := CoreReq_pipeReg_st0_st1.deq.valid
    ReplayType := readHitWSHR
  }.elsewhen((WriteMiss_st1 || AMO_LR_SC_st1) && (MshrStatus === SecondaryAvail || MshrStatus === SecondaryFull)){
    Req_RTAB_st1_valid := CoreReq_pipeReg_st0_st1.deq.valid
    ReplayType := writeMissHitMSHR
  }.elsewhen(WriteMiss_st1 && io.WSHR_CheckResult.Hit){
    Req_RTAB_st1_valid := CoreReq_pipeReg_st0_st1.deq.valid
    ReplayType := writeMissHitWSHR
  }.elsewhen(io.SMSHR_ProbeStatus.hitblock){
    Req_RTAB_st1_valid := CoreReq_pipeReg_st0_st1.deq.valid
    ReplayType := HitSMSHR
  }.elsewhen(Control_st1.isSC && io.SMSHR_ProbeStatus.LRexist){
    Req_RTAB_st1_valid := CoreReq_pipeReg_st0_st1.deq.valid
    ReplayType := SCLRexist
  }.elsewhen(fillConflictSt1){
    // bfs4096-006 fix: 接入 elsewhen 链最末，与上方分支 (多为 miss/UC/SC) 互斥。
    // 理论上 fillConflictSt1 ⇒ ReadHit_st1，与 readHitWSHR (Read+WSHR Hit) 也互斥
    // (WSHR hit 要求 same blockAddr 在 WSHR；fillConflict 要求 dA row ownership 已转走)。
    Req_RTAB_st1_valid := CoreReq_pipeReg_st0_st1.deq.valid
    ReplayType := fillConflict
  }.elsewhen(readMissFillWait_st1){
    // bfs4096-009 fix: read-miss 撞同 block in-flight fill → 挂 RTAB 等 commit replay re-probe hit。
    // 与 fillConflictSt1(要求 ReadHit)天然互斥; 抑制第二条 memReq(L364 !Req_st1_RTAB.valid)+ MSHR alloc(L533 !Req_RTAB_st1_valid)。
    Req_RTAB_st1_valid := CoreReq_pipeReg_st0_st1.deq.valid
    ReplayType := ReadMissFillWait
  }
  // nn64k-009 fix: 导出 st1 raw RTAB 预留意图(不经 st1_ready gate)。等价于 L599 "requesting RTAB"
  // 分支谓词 (Req_RTAB_st1_valid || ReplayType===UCacheHitDirty)——凡这条 st1 请求最终必须进 RTAB
  // 才能 drain 的情形都纳入预留，使 DCachev2 在 almost_full 时给它留住最后一格，防外部 st0-hitRTAB
  // 抢槽把 RTAB 填满后 st1 被 L649 永久 hold(死锁环首跳)。UCacheHitDirty 在 evict 等待阶段
  // Req_RTAB_st1_valid 可能尚为 0 但该请求已占 st1 且后续必进 RTAB，故一并纳入(对齐 L599 谓词)。
  io.Req_st1_RTAB_reserve :=
    Req_RTAB_st1_valid ||
    (CoreReq_pipeReg_st0_st1.deq.valid && (ReplayType === UCacheHitDirty))
  // bfs4096-006 fix: gate 掉 dA read 避免被 bypass 污染。replay 出来后 tag 已 update，
  // 同 way 的 tag 已经是 fill 后新 tag (例: visited 0x90002)，原 hit-read 的 tag (cost 0x90034) 不再 match → miss
  // → 走 MSHR 重新 fetch cost cacheline，落到 LRU 选的另一 way (visited 此时是 MRU 不会被选中)。
  val realReadHit_st1 = ReadHit_st1 && !fillConflictSt1
  io.read_Req_dA.valid := realReadHit_st1 || UCReqHitDirty || flushDirtyReq_st0

  //missReq 2 mem, request type and data generator
  OpcodeGen.io.coreReqCtrl := Control_st1
  OpcodeGen.io.coreReqParam := CoreReq_pipeReg_st0_st1.deq.bits.Req.param
  OpcodeGen.io.hit_dirty := io.tA_Hit_st1.hit && io.tA_Hit_st1.isDirty
  addrGen.io.dataIn := CoreReq_pipeReg_st0_st1.deq.bits.Req.data
  addrGen.io.perLaneAddrIn := CoreReq_pipeReg_st0_st1.deq.bits.Req.perLaneAddr
  // missMemReq_st1: 来自当前 coreReq 的 miss 请求。
  // 对 read / LR / SC / AMO，a_source 在这里就能确定，因为对应的 MSHR / SMSHR entry 已知；
  // 对 cached write miss，最终 a_source 需要等 DCachev2 在真正发 memReq 时拿到 WSHR pushedIdx 后再生成。
  // 因此 write miss 在这里先把主体字段整理好，a_source 仅作为占位。
  // cache miss mem Req
  missMemReq_st1.a_opcode := OpcodeGen.io.memReq_a_opcode
  missMemReq_st1.a_addr.get := Cat(CoreReq_pipeReg_st0_st1.deq.bits.Req.tag, CoreReq_pipeReg_st0_st1.deq.bits.Req.setIdx, 0.U((WordLength - TagBits - SetIdxBits).W))
  missMemReq_st1.a_param  := OpcodeGen.io.memReq_a_param
  missMemReq_st1.a_data := addrGen.io.dataOut
  missMemReq_st1.hasCoreRsp := Control_st1.isWrite
  missMemReq_st1.coreRspInstrId := CoreReq_pipeReg_st0_st1.deq.bits.Req.instrId
  missMemReq_st1.activeMask := VecInit(CoreReq_pipeReg_st0_st1.deq.bits.Req.perLaneAddr.map(_.activeMask))
  missMemReq_st1.spike_info.foreach( left =>
    left := CoreReq_pipeReg_st0_st1.deq.bits.Req.spike_info.getOrElse(0.U)
  )
  when(Control_st1.isRead){
    missMemReq_st1.a_source := Cat("d1".U, io.MSHR_ProbeStatus.a_source, CoreReq_pipeReg_st0_st1.deq.bits.Req.setIdx)
  }.elsewhen(Control_st1.isLR || Control_st1.isSC || Control_st1.isAMO){
    missMemReq_st1.a_source := Cat("d2".U, io.SMSHR_ProbeStatus.a_source, CoreReq_pipeReg_st0_st1.deq.bits.Req.setIdx)
  }.otherwise{
    missMemReq_st1.a_source := DontCare
  }
  if(MMU_ENABLED){
    missMemReq_st1.Asid.get := CoreReq_pipeReg_st0_st1.deq.bits.Req.asid.get
    FluInvMemReq_st1.Asid.get := io.tA_dirtyAsid_st1.get
    evictMemReq_st1.Asid.get := CoreReq_pipeReg_st0_st1.deq.bits.Req.asid.get
  }
  //regular read miss req mask is all 1
  missMemReq_st1.a_mask := Mux(missMemReq_st1.a_opcode === TLAOp_Get &&missMemReq_st1.a_param === 0.U,VecInit(Seq.fill(BlockWords)(Fill(BytesOfWord,1.U))),addrGen.io.MaskOut)
  // FluInvMemReq_st1: flush / invalidate 产生的对外请求。
  // dirty line 需要 PutFull 把 victim line 写回；否则发 Flush / Invalidate hint 到下层。
  // 这类请求不需要给 core 立即返回普通 load/store coreRsp。
  // flu or inv mem req
  // bfs4096-001 partial-write clobber fix: PutPartialData on dirty writeback (not PutFullData)
  FluInvMemReq_st1.a_opcode := Mux(FluInvIsPut_st1,TLAOp_PutPart,TLAOp_Flush)
  FluInvMemReq_st1.a_param := Mux(FluInvIsPut_st1, 0.U, Mux(CoreReq_pipeReg_st0_st1.deq.bits.Ctrl.isFlush, TLAParam_Flush, TLAParam_Inv))
  val dirtySetIdx_st1 = RegNext(io.tA_dirtySetIdx_st0)
  // === backprop1024-001 fix: FluInvMemReq identity snapshot ===
  // flush sweep 期间 io.dA_data / io.tA_dirtyTag_st1 / dirtySetIdx_st1 是 live 信号，
  // 当 memReq_Q 反压、FluInvMemReq 不能 fire 时，下一个 sub-flush 的迭代会让这些
  // live 信号继续推进，导致 holding 中的 writeback 的 (a_addr, a_data) 不再属于
  // 同一条 dirty cacheline，最终把别的 line（甚至全 0）写回 PMEM。
  // 同 gaussian fix #6 ReadHit snapshot 的形态，扩展到 flush 写回路径。
  val fluInvLiveAddr = Cat(io.tA_dirtyTag_st1, dirtySetIdx_st1, 0.U((WordLength - TagBits - SetIdxBits).W))
  val fluInvSnapData = Reg(Vec(BlockWords, UInt(WordLength.W)))
  val fluInvSnapAddr = Reg(UInt(WordLength.W))
  // bfs4096-001 partial-write clobber fix: snapshot dirty byte-mask alongside addr/data
  val fluInvSnapMask = Reg(UInt((BlockWords * BytesOfWord).W))
  val fluInvSnapValid = RegInit(false.B)
  when(FluInvMemReq_valid && FluInvIsPut_st1 && !fluInvSnapValid){
    fluInvSnapData := io.dA_data
    fluInvSnapAddr := fluInvLiveAddr
    fluInvSnapMask := io.tA_dirtyMask_st1
    fluInvSnapValid := true.B
  }
  when(io.MissReq_Mem.fire && FluInvMemReq_valid){
    fluInvSnapValid := false.B
  }
  FluInvMemReq_st1.a_addr.get := Mux(fluInvSnapValid, fluInvSnapAddr, fluInvLiveAddr)
  FluInvMemReq_st1.a_data := Mux(fluInvSnapValid, fluInvSnapData, io.dA_data)
  FluInvMemReq_st1.a_source := DontCare
  FluInvMemReq_st1.hasCoreRsp := false.B
  FluInvMemReq_st1.coreRspInstrId := DontCare
  FluInvMemReq_st1.activeMask := VecInit(Seq.fill(NLanes)(false.B))
  FluInvMemReq_st1.a_mask :=
    Mux(fluInvSnapValid, fluInvSnapMask, io.tA_dirtyMask_st1).asTypeOf(Vec(BlockWords, UInt(BytesOfWord.W)))
  FluInvMemReq_valid :=
    (FluInvIsPut_st1 || (FluInvIsFluL2_st1 && !FluInvL2MemReqIssuedReg)) &&
      CoreReq_pipeReg_st0_st1.deq.valid
  FluInvMemReq_st1.spike_info.foreach(_ := DontCare )
  // evictMemReq_st1: uncached 请求如果命中 dirty cacheline，需要先把当前 cacheline 写回，
  // 再通过 RTAB / replay 机制重放原始 uncached 请求。
  // uncache hit dirty cacheline evict request
  evictMemReq_st1.a_opcode := TLAOp_PutFull
  evictMemReq_st1.a_param  := 0.U
  evictMemReq_st1.a_addr.get := Cat(CoreReq_pipeReg_st0_st1.deq.bits.Req.tag, CoreReq_pipeReg_st0_st1.deq.bits.Req.setIdx, 0.U((WordLength - TagBits - SetIdxBits).W))
  evictMemReq_st1.a_data := io.dA_data
  evictMemReq_st1.hasCoreRsp := false.B
  evictMemReq_st1.a_source := DontCare
  evictMemReq_st1.a_mask := VecInit(Seq.fill(BlockWords)(Fill(BytesOfWord,1.U)))
  evictMemReq_st1.spike_info.foreach(_ := DontCare )
  evictstateReg := evictReg_next
  evictMemReq_st1.coreRspInstrId := DontCare
  evictMemReq_st1.activeMask := VecInit(Seq.fill(NLanes)(false.B))
  evictReg_next := evictstateReg
  // FSM for read da
  when(evictstateReg === evictidle){
    when(CoreReq_pipeReg_st0_st1.deq.valid && UCReqHitDirty){
      evictReg_next := evictrsp
    }.otherwise{
      evictReg_next := evictstateReg
    }
  }.elsewhen(evictstateReg === evictrsp && io.MissReq_Mem.ready){
    evictReg_next := evictidle
  }
  evictMemReq_valid := evictstateReg === evictrsp

  //MSHR miss Req
  val mshrMissReqTI = Wire(new VecMshrTargetInfo)
  mshrMissReqTI.instrId := CoreReq_pipeReg_st0_st1.deq.bits.Req.instrId
  mshrMissReqTI.perLaneAddr := CoreReq_pipeReg_st0_st1.deq.bits.Req.perLaneAddr
  // 只在 st1 真正握手（deq.fire）时才允许向 MSHR 注入 missReq，避免 st1 停住但 MSHR 侧发生握手导致状态不一致
  io.MissReq_MSHR.valid := ReadMiss_st1 && CoreReq_pipeReg_st0_st1.deq.fire && !Req_RTAB_st1_valid
  io.MissReq_MSHR.bits.blockAddr := BlockAddr_st1
  io.MissReq_MSHR.bits.targetInfo := mshrMissReqTI.asUInt
  io.MissReq_MSHR.bits.instrId := CoreReq_pipeReg_st0_st1.deq.bits.Req.instrId
  io.MissCached_MSHR := Control_st1.isUncached
  io.Probe_SMSHR.bits.blockAddr := BlockAddr_st1
  io.Probe_SMSHR.bits.instrId := CoreReq_pipeReg_st0_st1.deq.bits.Req.instrId
  io.Probe_SMSHR.bits.targetInfo := mshrMissReqTI.asUInt
  io.Probe_SMSHR.bits.wordOffset := CoreReq_pipeReg_st0_st1.deq.bits.Req.perLaneAddr.map(lane => 
  Mux(lane.activeMask, lane.blockOffset, 0.U)
).reduce(_ | _)
  io.Probe_SMSHR.bits.Type := 0.U
  // todo add probe type
  io.Probe_SMSHR.valid := CoreReq_pipeReg_st0_st1.deq.valid && !Req_RTAB_st1_valid
  when(Control_st1.isAMO){
    io.Probe_SMSHR.bits.Type := 3.U
  }.elsewhen(Control_st1.isLR){
    io.Probe_SMSHR.bits.Type := 1.U
  }.elsewhen(Control_st1.isSC){
    io.Probe_SMSHR.bits.Type := 2.U
  }

  //st1 ready
  st1_ready := false.B
  when(!(Req_RTAB_st1_valid || ReplayType === UCacheHitDirty)) { // when not request RTAB
    when(Control_st1.isRead || Control_st1.isWrite) {
      when(io.tA_Hit_st1.hit) {
          // 命中路径不应被 MSHR miss 分配 ready 阻塞。
          // 当前 io.Mshr_st1_ready 由 MSHR.missReq.ready 驱动，只反映 miss 分配能力，不代表 hit 处理能力。
          when(CoreRsp_pipeReg_st1_st2.enq.ready) { //todo check ready condition
            when(UCReqHitDirty){ // uncached read hit dirty will write back to mem and rsp to core
              st1_ready := !io.memRsp_coreRsp.valid && io.MissReq_Mem.ready && (evictstateReg === evictrsp)
            }.otherwise{
              st1_ready := !io.memRsp_coreRsp.valid //true.B
            }
          }.otherwise{
            st1_ready := false.B
          }
      }.otherwise { //Miss
        when(Control_st1.isRead) {
          when(io.MissReq_MSHR.ready && (MshrStatus === PrimaryAvail || MshrStatus === SecondaryAvail) //即memReq_Q.io.enq.ready
            && io.Mshr_st1_ready) {
              when(MshrStatus === SecondaryAvail){
                st1_ready := true.B //true.B
              }.otherwise{
                st1_ready := io.MissReq_Mem.ready
              }
          }.otherwise{
            st1_ready := false.B
          }
        }.otherwise { //isWrite
         when(!io.memRsp_coreRsp.valid && CoreRsp_pipeReg_st1_st2.enq.ready && io.MissReq_Mem.ready && io.Mshr_st1_ready) { //memReq_Q.io.enq.ready
            st1_ready := true.B
          }.otherwise{
            st1_ready := false.B
          }
        }
      }
    }.elsewhen(Control_st1.isAMO){
      st1_ready := io.MissReq_Mem.ready
    }.elsewhen(Control_st1.isFlush || Control_st1.isInvalidate){
      when(FlushInvstateReg === idle || FlushInvstateReg === flushing){
        // After the L2 flush/invalidate memReq is accepted, we keep the response context
        // in FluInvRspPendingReg instead of holding the request in TagAccess st1.
        st1_ready := io.MissReq_Mem.ready
      }.otherwise{
        st1_ready := !FluInvRspPendingReg && CoreRsp_pipeReg_st1_st2.enq.ready && !io.memRsp_coreRsp.valid
      }
    }.otherwise{st1_ready := true.B}
  }.otherwise{// when requesting RTAB
    // nn64k-006-pathA-hang fix: hold st1 when RTAB has no room. Without this,
    // an in-flight st1 request that was admitted past the DCachev2 allowIn1
    // gate before RTAB filled up could overwrite a live entry and bump ptr,
    // breaking the strict-FIFO invariant ptr_w == ptr_r at empty.
    when(io.RTAB_full){
      st1_ready := false.B
    }.elsewhen(ReplayType === UCacheHitDirty){ // when hit in UCache and dirty, will write back to memory
      st1_ready := io.MissReq_Mem.ready && (evictstateReg === evictrsp)
    }.otherwise{
      st1_ready := true.B
    }
  }
  when(io.blockCoreReq){
    st1_ready := false.B
  }
  when(CoreReq_pipeReg_st0_st1.deq.valid && mshrReleasingSameBlock_st1){
    st1_ready := false.B
  }
  io.perfReqFire := CoreReq_pipeReg_st0_st1.deq.fire
  io.perfReqFromReplay := fromReplay_st1
  io.perfIsRead := Control_st1.isRead
  io.perfIsWrite := Control_st1.isWrite
  io.perfIsUncached := Control_st1.isUncached
  io.perfIsHit := CacheHit_st1
  //write hit
  val getBankEn = Module(new getDataAccessBankEn(NBank = BlockWords, NLane = NLanes))
  getBankEn.io.perLaneBlockIdx :=  CoreReq_pipeReg_st0_st1.deq.bits.Req.perLaneAddr.map(_.blockOffset)
  getBankEn.io.perLaneValid :=  CoreReq_pipeReg_st0_st1.deq.bits.Req.perLaneAddr.map(_.activeMask)

  // ******      dataAccess write hit      ******
  val DataAccessWriteHitSRAMWReq: Vec[SRAMBundleAW[UInt]] = Wire(Vec(BlockWords,new SRAMBundleAW(UInt(8.W), NSets*NWays, BytesOfWord)))
  //this setIdx = setIdx + wayIdx
  DataAccessWriteHitSRAMWReq.foreach(_.setIdx := Cat( CoreReq_pipeReg_st0_st1.deq.bits.Req.setIdx,OHToUInt(io.tA_Hit_st1.waymask)))
  for (i <- 0 until BlockWords){
    DataAccessWriteHitSRAMWReq(i).waymask.get := addrGen.io.MaskOut(i)
    io.WriteReq_dA_valid(i) := addrGen.io.MaskOut(i).orR
    DataAccessWriteHitSRAMWReq(i).data := addrGen.io.dataOut(i).asTypeOf(Vec(BytesOfWord,UInt(8.W)))
  }
  io.WriteReq_dA := DataAccessWriteHitSRAMWReq
  //st1 valid: enqueue st1 st2 pipe reg for coreRsp
  // indicating coreRsp is valid from core Req
  // case: regular read/write hit, uncached read hit, uncache write hit undirty, flush invalidate complete
  st1_valid := false.B
  when(!Req_RTAB_st1_valid ){
    when(Control_st1.isRead && io.tA_Hit_st1.hit){
      st1_valid := CoreReq_pipeReg_st0_st1.deq.valid
    }.elsewhen(Control_st1.isWrite && io.tA_Hit_st1.hit){
      st1_valid := CoreReq_pipeReg_st0_st1.deq.valid
    
  }.elsewhen(Control_st1.isFlush || Control_st1.isInvalidate){
    st1_valid := CoreReq_pipeReg_st0_st1.deq.valid && (FlushInvstateReg === responding) && !FluInvRspPendingReg
  }.otherwise{
    st1_valid := false.B
  }
}

  //RTAB req will deq st1 but not enq st2: except for uncache read hit dirty
  CoreReq_pipeReg_st0_st1.deq.ready := st1_ready
  io.st1_valid := CoreReq_pipeReg_st0_st1.deq.valid
  io.st1_ready := CoreReq_pipeReg_st0_st1.deq.ready

  //==========
  // st2 pipe reg
  // 对 read-hit 来说，dA_data 对应“上一拍发出的 data SRAM 读请求”。
  // 如果 st1 被 memRsp_coreRsp 等 backpressure 卡住，当前 hit 请求虽然还停在 st1，
  // 但后续周期 DataAccess 可能已经被 refill 改写；因此需要把“stall 期间第一次可见的旧数据”
  // 和这条 coreReq 一起带到 st2，而不是等到 st2 再去读 live 的 io.dA_data。
  
  val st1ReadHitSnapshot = Reg(Vec(dcache_BlockWords, UInt(WordLength.W)))
  val st1ReadHitSnapshotValid = RegInit(false.B)
  val st1ReadHitSnapshotPending = RegInit(false.B)
  val readHitStall_st1 = CoreReq_pipeReg_st0_st1.deq.valid && ReadHit_st1 && !st1_ready
  val readHitFire_st1 = CoreReq_pipeReg_st0_st1.deq.fire && ReadHit_st1
  when(readHitStall_st1 && !st1ReadHitSnapshotValid && !st1ReadHitSnapshotPending){
    st1ReadHitSnapshotPending := true.B
  }
  when(st1ReadHitSnapshotPending && !readHitFire_st1){
    st1ReadHitSnapshot := io.dA_data
    st1ReadHitSnapshotValid := true.B
    st1ReadHitSnapshotPending := false.B
  }
  when(readHitFire_st1 || !CoreReq_pipeReg_st0_st1.deq.valid){
    st1ReadHitSnapshotValid := false.B
    st1ReadHitSnapshotPending := false.B
  }
  val st1ReadHitSnapshotAvail = st1ReadHitSnapshotValid || st1ReadHitSnapshotPending
  val st1ReadHitSnapshotData = Mux(st1ReadHitSnapshotPending, io.dA_data, st1ReadHitSnapshot)

  // coreRsp 第 1 类：可由当前 core request 直接生成响应
  // 情况：cache hit 的应答，数据会在 st2 从 data access 路径中选出
  // write miss 不在这里返回，而是走 memReq_coreRsp 旁路，表示写请求已被下游接收
  when(st1_valid && st1_ready && CacheHit_st1){
    CoreRsp_pipeReg_st1_st2.enq.valid            := true.B
    CoreRsp_pipeReg_st1_st2.enq.bits.Rsp.isWrite := CoreReq_pipeReg_st0_st1.deq.bits.Ctrl.isWrite
    CoreRsp_pipeReg_st1_st2.enq.bits.Rsp.data    := DontCare
    CoreRsp_pipeReg_st1_st2.enq.bits.Rsp.instrId := CoreReq_pipeReg_st0_st1.deq.bits.Req.instrId
    CoreRsp_pipeReg_st1_st2.enq.bits.Rsp.activeMask := CoreReq_pipeReg_st0_st1.deq.bits.Req.perLaneAddr.map(_.activeMask)
    CoreRsp_pipeReg_st1_st2.enq.bits.validFromCoreReq := true.B
    CoreRsp_pipeReg_st1_st2.enq.bits.perLaneAddr := CoreReq_pipeReg_st0_st1.deq.bits.Req.perLaneAddr
    CoreRsp_pipeReg_st1_st2.enq.bits.readHitSnapshotValid := ReadHit_st1 && st1ReadHitSnapshotAvail
    CoreRsp_pipeReg_st1_st2.enq.bits.readHitSnapshotData := st1ReadHitSnapshotData
  // coreRsp 第 2 类：响应来自下层 memory 回包后的 memRsp 路径，而不是当前 core request 直接生成
  // 典型场景：
  // 1. cached read miss 从下层取回整条 cacheline 后，既要回填 dA，也要把对应 lane 的数据返回给 core
  // 2. uncached read 从下层取回数据后直接返回给 core，不经过本地 cache hit 路径
  // 3. special 请求（如 LR/SC/AMO）经 SMSHR/特殊返回路径整理后，再回到 coreRsp
  // 这类响应的共同点是 validFromCoreReq = false，st2 取数时使用这里携带的 Rsp.data，而不是 dA_data。
  }.elsewhen(io.memRsp_coreRsp.valid){
    CoreRsp_pipeReg_st1_st2.enq.valid            := true.B
    CoreRsp_pipeReg_st1_st2.enq.bits.Rsp.isWrite := false.B
    CoreRsp_pipeReg_st1_st2.enq.bits.Rsp.data    := io.memRsp_coreRsp.bits.Rsp.data
    CoreRsp_pipeReg_st1_st2.enq.bits.Rsp.instrId := io.memRsp_coreRsp.bits.Rsp.instrId
    CoreRsp_pipeReg_st1_st2.enq.bits.Rsp.activeMask := io.memRsp_coreRsp.bits.perLaneAddr.map(_.activeMask)
    CoreRsp_pipeReg_st1_st2.enq.bits.validFromCoreReq := false.B
    CoreRsp_pipeReg_st1_st2.enq.bits.perLaneAddr := io.memRsp_coreRsp.bits.perLaneAddr
    CoreRsp_pipeReg_st1_st2.enq.bits.readHitSnapshotValid := false.B
    CoreRsp_pipeReg_st1_st2.enq.bits.readHitSnapshotData := DontCare
  // coreRsp 第 3 类：flush / invalidate 请求完成后返回给 core 的应答
  }.elsewhen(fluInvRspPendingFire){
    CoreRsp_pipeReg_st1_st2.enq.valid            := true.B
    CoreRsp_pipeReg_st1_st2.enq.bits.Rsp.isWrite := FluInvRspCtrlReg.isWrite
    CoreRsp_pipeReg_st1_st2.enq.bits.Rsp.data    := DontCare
    CoreRsp_pipeReg_st1_st2.enq.bits.Rsp.instrId := FluInvRspReqReg.instrId
    CoreRsp_pipeReg_st1_st2.enq.bits.Rsp.activeMask := FluInvRspReqReg.perLaneAddr.map(_.activeMask)
    CoreRsp_pipeReg_st1_st2.enq.bits.validFromCoreReq := true.B
    CoreRsp_pipeReg_st1_st2.enq.bits.perLaneAddr := FluInvRspReqReg.perLaneAddr
    CoreRsp_pipeReg_st1_st2.enq.bits.readHitSnapshotValid := false.B
    CoreRsp_pipeReg_st1_st2.enq.bits.readHitSnapshotData := DontCare
  }.elsewhen(st1_valid && st1_ready && (FlushInvstateReg === responding)){
    CoreRsp_pipeReg_st1_st2.enq.valid            := true.B
    CoreRsp_pipeReg_st1_st2.enq.bits.Rsp.isWrite := CoreReq_pipeReg_st0_st1.deq.bits.Ctrl.isWrite
    CoreRsp_pipeReg_st1_st2.enq.bits.Rsp.data    := DontCare
    CoreRsp_pipeReg_st1_st2.enq.bits.Rsp.instrId := CoreReq_pipeReg_st0_st1.deq.bits.Req.instrId
    CoreRsp_pipeReg_st1_st2.enq.bits.Rsp.activeMask := CoreReq_pipeReg_st0_st1.deq.bits.Req.perLaneAddr.map(_.activeMask)
    CoreRsp_pipeReg_st1_st2.enq.bits.validFromCoreReq := true.B
    CoreRsp_pipeReg_st1_st2.enq.bits.perLaneAddr := CoreReq_pipeReg_st0_st1.deq.bits.Req.perLaneAddr
    CoreRsp_pipeReg_st1_st2.enq.bits.readHitSnapshotValid := false.B
    CoreRsp_pipeReg_st1_st2.enq.bits.readHitSnapshotData := DontCare
  }.otherwise{
    CoreRsp_pipeReg_st1_st2.enq.valid := false.B
    CoreRsp_pipeReg_st1_st2.enq.bits := DontCare
    CoreRsp_pipeReg_st1_st2.enq.bits.Rsp.activeMask := CoreReq_pipeReg_st0_st1.deq.bits.Req.perLaneAddr.map(_.activeMask)
  }
  // 由 core request 自身产生的响应，在同一个周期内会阻止 memRsp_coreRsp 占用 st2 的入队口。
  val coreRspPipeEnqFromCoreReq =
    (st1_valid && st1_ready && CacheHit_st1) ||
      fluInvRspPendingFire ||
      (st1_valid && st1_ready && (FlushInvstateReg === responding))
  io.memRsp_coreRsp.ready := CoreRsp_pipeReg_st1_st2.enq.ready && !coreRspPipeEnqFromCoreReq
  coreReq_st2_ready := CoreRsp_st3.io.enq.ready && !io.memReq_coreRsp.valid
  CoreRsp_pipeReg_st1_st2.deq.ready := coreReq_st2_ready
  //== st2 ==
  val dADataForCoreReq_st2 = Mux(
    CoreRsp_pipeReg_st1_st2.deq.bits.readHitSnapshotValid,
    CoreRsp_pipeReg_st1_st2.deq.bits.readHitSnapshotData,
    io.dA_data
  )
  //hold dataaccess data when there is conflict
  val coreRsp_st2_coreRsp_data_hold = Module(new Queue(Vec(dcache_BlockWords, UInt(WordLength.W)),1,true,false))
  coreRsp_st2_coreRsp_data_hold.io.enq.valid := CoreRsp_pipeReg_st1_st2.deq.valid && !coreReq_st2_ready && CoreRsp_pipeReg_st1_st2.deq.bits.validFromCoreReq
  coreRsp_st2_coreRsp_data_hold.io.enq.bits := dADataForCoreReq_st2
  coreRsp_st2_coreRsp_data_hold.io.deq.ready := coreReq_st2_ready
  val DataAccessReadHit = Mux(coreRsp_st2_coreRsp_data_hold.io.deq.valid,coreRsp_st2_coreRsp_data_hold.io.deq.bits,dADataForCoreReq_st2)
  // st2 第 1 类：coreRsp 来自 core request 路径，数据来自 dA_data/DataAccessReadHit。
  val coreRspFromCoreReq_st2 = CoreRsp_pipeReg_st1_st2.deq.valid && CoreRsp_pipeReg_st1_st2.deq.bits.validFromCoreReq && !io.memReq_coreRsp.valid
  // st2 第 2 类：coreRsp 来自 memRsp 路径。
  // 此时响应已经由 MemRspPipe 基于 MSHR/SMSHR 的 missRspOut 整理完成，
  // 数据直接保存在 pipeReg_st1_st2.bits.Rsp.data 中，st2 不再从 dA_data 取数。
  val coreRspFromMemRsp_st2 = CoreRsp_pipeReg_st1_st2.deq.valid && !CoreRsp_pipeReg_st1_st2.deq.bits.validFromCoreReq && !io.memReq_coreRsp.valid
  // st2 总的 valid 包含三种情况：coreReq 产生的 rsp、memRsp 产生的 rsp、以及直接旁路的 memReq_coreRsp。
  val st2_valid =  coreRspFromCoreReq_st2 || coreRspFromMemRsp_st2 || io.memReq_coreRsp.valid
  DataMemOrder_st2 := Mux(CoreRsp_pipeReg_st1_st2.deq.bits.validFromCoreReq, DataAccessReadHit, CoreRsp_pipeReg_st1_st2.deq.bits.Rsp.data)
  //data covertion
  for (i <- 0 until NLanes) {
    DataCoreOrder_st2(i) := DataMemOrder_st2(CoreRsp_pipeReg_st1_st2.deq.bits.perLaneAddr(i).blockOffset)
  }
  io.memReq_coreRsp.ready := CoreRsp_st3.io.enq.ready
  //== st3 enq
  CoreRsp_st3.io.enq.valid := io.memReq_coreRsp.valid || CoreRsp_pipeReg_st1_st2.deq.valid
  CoreRsp_st3.io.enq.bits.data    := DataCoreOrder_st2
  CoreRsp_st3.io.enq.bits.instrId := Mux(io.memReq_coreRsp.valid, io.memReq_coreRsp.bits.instrId, CoreRsp_pipeReg_st1_st2.deq.bits.Rsp.instrId)
  CoreRsp_st3.io.enq.bits.isWrite := Mux(io.memReq_coreRsp.valid, io.memReq_coreRsp.bits.isWrite, CoreRsp_pipeReg_st1_st2.deq.bits.Rsp.isWrite)
  CoreRsp_st3.io.enq.bits.activeMask := Mux(
    io.memReq_coreRsp.valid,
    io.memReq_coreRsp.bits.activeMask,
    CoreRsp_pipeReg_st1_st2.deq.bits.Rsp.activeMask
  )
  //
  io.CoreRsp <> CoreRsp_st3.io.deq
  io.st2_ready := CoreRsp_st3.io.enq.ready
}

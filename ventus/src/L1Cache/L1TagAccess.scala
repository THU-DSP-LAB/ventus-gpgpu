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
import freechips.rocketchip.rocket.ASIdBits
import top.parameters._
import config.config.Parameters
class tagCheckerResult(way: Int) extends Bundle{
  val waymask = UInt(way.W)
  val hit = Bool()
}
class hitStatus(way: Int, tagBits: Int) extends Bundle{
  val waymask = UInt(way.W)
  val tag = UInt(tagBits.W)
  val isDirty = Bool()
  val hit = Bool()
}
//This module contain Tag memory, its valid bits, tag comparator, and Replacement Unit
class L1TagAccess(set: Int, way: Int, tagBits: Int, AsidBits: Int, readOnly: Boolean)(implicit p: Parameters)extends Module{
  val io = IO(new Bundle {
    //From coreReq_pipe0
    val probeRead = Flipped(Decoupled(new SRAMBundleA(set)))//Probe Channel
    val tagFromCore_st1 = Input(UInt(tagBits.W))
    val asidFromCore_st1 = if(MMU_ENABLED) Some(Input(UInt(AsidBits.W))) else None
    val probeIsWrite_st1 = if(!readOnly){Some(Input(Bool()))} else None
    val probeIsUncache_st1 = Input(Bool())
    //val coreReqReady = Input(Bool())//TODO try to replace with probeRead.fire
    //To coreReq_pipe1
    val hit_st1 = Output(Bool())
    val hitStatus_st1 = Output(new hitStatus(way, tagBits))
    val waymaskHit_st1 = Output(UInt(way.W))
    //From memRsp_pipe0
    val allocateWrite = Flipped(ValidIO(new SRAMBundleA(set)))//Allocate Channel
    val allocateWriteData_st1 = Input(UInt(tagBits.W))///todo dont need, unccache req will not give valid allocatewrite.valid
    val allocateWriteAsid_st1 = if(MMU_ENABLED) Some(Input(UInt(AsidBits.W))) else None
    //From memRsp_pipe1
    val allocateWriteTagSRAMWValid_st1 = Input(Bool())
    //To memRsp_pipe1
    val needReplace = if(!readOnly){
      Some(Output(Bool()))
    } else None
    val replaceValidVictim_st1 = if(!readOnly){
      Some(Output(Bool()))
    } else None
    val waymaskReplacement_st1 = Output(UInt(way.W))//one hot, for SRAMTemplate
    val a_addrReplacement_st1 = if (!readOnly) {
      Some(Output(UInt(xLen.W)))
    } else None
    val asidReplacement_st1 = if(MMU_ENABLED) Some{Output(UInt(AsidBits.W))} else None
    //For InvOrFlu
    val hasDirty_st0 = if (!readOnly) {Some(Output(Bool()))} else None
    val dirtySetIdx_st0 = if (!readOnly) {Some(Output(UInt(log2Up(set).W)))} else None
    val dirtyWayMask_st0 = if (!readOnly) {Some(Output(UInt(way.W)))} else None
    val dirtyTag_st1 = if (!readOnly) {Some(Output(UInt(tagBits.W)))} else None
    val dirtyASID_st1 = if(MMU_ENABLED) {Some(Output(UInt(AsidBits.W)))} else None
    //For InvOrFlu and LRSC
    val flushChoosen = if (!readOnly) {Some(Input(Bool()))} else None
    // lud-001 v6 Path A (改动 3c): flush PutPart 真出站 fire + 清 way_dirty/dirtyMask 的 identity (CoreReqPipe 传入, 与 a_addr 同源)
    val flushPutFire = if (!readOnly) {Some(Input(Bool()))} else None
    val flushClrSetIdx = if (!readOnly) {Some(Input(UInt(log2Up(set).W)))} else None
    val flushClrWayMask = if (!readOnly) {Some(Input(UInt(way.W)))} else None
    //For Inv
    val invalidateAll = Input(Bool())
    val tagready_st1 = Input(Bool())

    // 本次写入cacheline位置的信息
    val perLaneAddr_st1 = Input(Vec(num_lane, new DCachePerLaneAddr))
    // 全局无效化时，dirty cacheline的 dirty mask
    val dirtyMask_st1 = Output(UInt((dcache_BlockWords * BytesOfWord).W))
    // 分配写要替换的 cacheline 的 dirty mask
    val replace_dirty_mask_st1 = Output(UInt((dcache_BlockWords * BytesOfWord).W))
    // bfs4096-002 fix iter1: ST1 cycle 真有新 dispatch 的严格 gate（= deq.valid）。
    // 用于 dirtyMaskWriteArb.in(1).valid，防止 cache_hit/probeIsWrite_st1 在 deq.bits
    // 持续保持高电平时长期重写同一 set。详见下方对应注释 + checkpoint_3.md 迭代 1。
    val coreReq_st1_valid = Input(Bool())
  })
  //TagAccess internal parameters
  val Length_Replace_time_SRAM: Int = 10
  assert(!(io.probeRead.fire && io.allocateWrite.fire), s"tag probe and allocate in same cycle")
  val probeReadBuf = Queue(io.probeRead,1,pipe=true)
  probeReadBuf.ready := io.tagready_st1

  //access time counter
  val accessFire = io.probeRead.fire || io.allocateWrite.fire
  val (accessCount,accessCounterFull) = Counter(accessFire,1000)

  //For InvOrFlu
  val hasDirty_st0 = Wire(Bool())
  val choosenDirtySetIdx_st0 = Wire(UInt(log2Up(set).W))
  val way_valid = RegInit(VecInit(Seq.fill(set)(VecInit(Seq.fill(way)(0.U(1.W))))))
  // // ***** tag_array::has_dirty *****
  // //val hasDirty_st0 = Wire(Bool())
  // val setDirty = Wire(Vec(set, Bool()))
  // val way_dirtyAfterValid = Wire(Vec(set, Vec(way, Bool())))
  // //val choosenDirtySetIdx_st0 = Wire(UInt(log2Up(set).W))
  // val choosenDirtySetValid = Wire(Vec(way, Bool()))
  // val choosenDirtyWayMask_st0 = Wire(UInt(way.W))//OH
  // val choosenDirtyWayMask_st1 = Wire(UInt(way.W))
  // val choosenDirtyTag_st1 = Wire(UInt(tagBits.W))

    // ***** tag_array::has_dirty *****
  //val hasDirty_st0 = Wire(Bool())
  if(!readOnly) {
    val setDirty = Wire(Vec(set, Bool()))
    val way_dirtyAfterValid = Wire(Vec(set, Vec(way, Bool())))
    //val choosenDirtySetIdx_st0 = Wire(UInt(log2Up(set).W))
    val choosenDirtySetValid = Wire(Vec(way, Bool()))
    val choosenDirtyWayMask_st0 = Wire(UInt(way.W))//OH
    val choosenDirtyWayMask_st1 = Wire(UInt(way.W))
    val choosenDirtyTag_st1 = Wire(UInt(tagBits.W))

  //for Chisel coding convenience, dont set way_dirty to be optional
  val way_dirty = RegInit(VecInit(Seq.fill(set)(VecInit(Seq.fill(way)(0.U(1.W))))))
  //if(!readOnly){Some()} else None
  // allocateWrite_st1
  val Replacement = Module(new ReplacementUnit(Length_Replace_time_SRAM, way))

  val allocateWrite_st1 = RegEnable(io.allocateWrite.bits, io.allocateWrite.fire)
  // ====== bfs4096-008 fillWayMask 修复 (要素①+②, codex v6 审过逻辑; dup-tag 诊断 build 用 counter) ======
  // 要素①: allocate 决策拍(T+1)锁存 victim waymask + Set_is_full, 冻结到 commit,
  //   切断 "way_valid 早置 → validOfSet 翻 → live waymask 自反馈漂移" 环 (early-valid 空壳根因)。
  //   T+1 = RegNext(allocateWrite.fire): allocateWrite_st1.setIdx 此拍才持有本 fill 目标 set,
  //   Replacement.io.waymask_st1 此拍才首次描述本 fill (timeAccess holdRead resp 同拍出)。
  //   T+1 用 raw (已对齐本 fill); T+2.. 用 held (冻结对抗反压窗口漂移)。
  val allocateLatchEn = RegNext(io.allocateWrite.fire, false.B)
  val lockedWayMask   = Mux(allocateLatchEn,
                            Replacement.io.waymask_st1,
                            RegEnable(Replacement.io.waymask_st1, 0.U(way.W), allocateLatchEn))
  val lockedSetIsFull = Mux(allocateLatchEn,
                            Replacement.io.Set_is_full,
                            RegEnable(Replacement.io.Set_is_full, false.B, allocateLatchEn))
  // ******      tag_array::probe    ******
  val iTagChecker = Module(new tagChecker(way=way,tagIdxBits=tagBits, AsidBits = AsidBits))
  val cachehit_hold = Module(new Queue(new tagCheckerResult(way),1))
  // bfs4096-007 v15: hit_st1_raw 前置 Wire 声明, 给下方 in(1).valid 用 (实际赋值在 L355)
  val hit_st1_raw = Wire(Bool())
  //SRAM to store tag
  val tagBodyAccess = Module(new SRAMTemplate(
    UInt(tagBits.W),
    set=set,
    way=way,
    shouldReset = false,
    holdRead = true,
    singlePort = false,
    bypassWrite = true
  ))
  if(readOnly){
    tagBodyAccess.io.r.req <> io.probeRead
  }else{
    val tagAccessRArb = Module(new Arbiter (new SRAMBundleA(set),3))
    tagBodyAccess.io.r.req <> tagAccessRArb.io.out
    //For probe
    tagAccessRArb.io.in(1)<> io.probeRead
    //For allocate
    tagAccessRArb.io.in(0).valid := io.allocateWrite.valid
    tagAccessRArb.io.in(0).bits.setIdx := io.allocateWrite.bits.setIdx
    //For hasDirty
    tagAccessRArb.io.in(2).valid := !io.probeRead.valid && !io.allocateWrite.valid
    tagAccessRArb.io.in(2).bits.setIdx := choosenDirtySetIdx_st0
    //io.allocateWrite.ready := tagAccessRArb.io.in(0).ready
  }

  // SRAM for storing ASID tag
if(MMU_ENABLED) {
  val ASIDAccess = Module(new SRAMTemplate(
    UInt(AsidBits.W),
    set = set,
    way = way,
    shouldReset = false,
    holdRead = true,
    singlePort = false,
    bypassWrite = true
  ))
  val ASIDAccessRArb = Module(new Arbiter(new SRAMBundleA(set), 2))
  ASIDAccess.io.r.req <> ASIDAccessRArb.io.out //io.probeRead
  ASIDAccessRArb.io.in(0) <> io.probeRead
  ASIDAccessRArb.io.in(1).valid := !io.probeRead.valid && !io.allocateWrite.valid
  ASIDAccessRArb.io.in(1).bits.setIdx := choosenDirtySetIdx_st0
  iTagChecker.io.ASID_of_set.get := ASIDAccess.io.r.resp.data
  iTagChecker.io.ASID_from_pipe.get := io.asidFromCore_st1.get
  val asidReplacement_st1 = ASIDAccess.io.r.resp.data(OHToUInt(lockedWayMask))//要素①
  val choosenDirtyASID_st1 = Wire(UInt(AsidBits.W))
  io.asidReplacement_st1.get := asidReplacement_st1
  choosenDirtyASID_st1 := ASIDAccess.io.r.resp.data(OHToUInt(choosenDirtyWayMask_st1))
  io.dirtyASID_st1.get := choosenDirtyASID_st1
  ASIDAccess.io.w.req.valid := io.allocateWriteTagSRAMWValid_st1
  ASIDAccess.io.w.req.bits.apply(data = io.allocateWriteAsid_st1.get, setIdx = allocateWrite_st1.setIdx, waymask = lockedWayMask)//要素①
}
  //SRAM for replacement policy
  //store last_access_time for LRU, or last_fill_time for FIFO
  val timeAccess = Module(new SRAMTemplate(
    UInt(Length_Replace_time_SRAM.W),
    set = set,
    way = way,
    shouldReset = false,
    holdRead = true,
    singlePort = false,
    bypassWrite = true
  ))
  timeAccess.io.r.req.valid := io.allocateWrite.fire
  timeAccess.io.r.req.bits.setIdx := io.allocateWrite.bits.setIdx
  //io.allocateWrite.ready := true.B
  //although use arb, src0 and src1 should not come in same cycle
  val timeAccessWArb = Module(new Arbiter (new SRAMBundleAW(UInt(Length_Replace_time_SRAM.W),set,way),2))
  val hitTimeUpdatePending = RegInit(false.B)
  val hitTimeUpdateDataReg = Reg(UInt(Length_Replace_time_SRAM.W))
  val hitTimeUpdateSetIdxReg = Reg(UInt(log2Up(set).W))
  val hitTimeUpdateWaymaskReg = Reg(UInt(way.W))
  val allocateTimeUpdateValid = RegNext(io.allocateWrite.fire, false.B)
  val hitTimeUpdateNowValid = io.hit_st1
  val hitTimeUpdateNowData = accessCount
  val hitTimeUpdateNowSetIdx = RegNext(io.probeRead.bits.setIdx)
  val hitTimeUpdateNowWaymask = io.waymaskHit_st1
  val hitTimeUpdateIssueValid = Wire(Bool())
  val hitTimeUpdateIssueData = Wire(UInt(Length_Replace_time_SRAM.W))
  val hitTimeUpdateIssueSetIdx = Wire(UInt(log2Up(set).W))
  val hitTimeUpdateIssueWaymask = Wire(UInt(way.W))

  hitTimeUpdateIssueValid := false.B
  hitTimeUpdateIssueData := hitTimeUpdateNowData
  hitTimeUpdateIssueSetIdx := hitTimeUpdateNowSetIdx
  hitTimeUpdateIssueWaymask := hitTimeUpdateNowWaymask

  when(allocateTimeUpdateValid){
    when(hitTimeUpdateNowValid){
      hitTimeUpdatePending := true.B
      hitTimeUpdateDataReg := hitTimeUpdateNowData
      hitTimeUpdateSetIdxReg := hitTimeUpdateNowSetIdx
      hitTimeUpdateWaymaskReg := hitTimeUpdateNowWaymask
    }
  }.otherwise{
    when(hitTimeUpdatePending){
      hitTimeUpdateIssueValid := true.B
      hitTimeUpdateIssueData := hitTimeUpdateDataReg
      hitTimeUpdateIssueSetIdx := hitTimeUpdateSetIdxReg
      hitTimeUpdateIssueWaymask := hitTimeUpdateWaymaskReg
      when(hitTimeUpdateNowValid){
        hitTimeUpdatePending := true.B
        hitTimeUpdateDataReg := hitTimeUpdateNowData
        hitTimeUpdateSetIdxReg := hitTimeUpdateNowSetIdx
        hitTimeUpdateWaymaskReg := hitTimeUpdateNowWaymask
      }.otherwise{
        hitTimeUpdatePending := false.B
      }
    }.elsewhen(hitTimeUpdateNowValid){
      hitTimeUpdateIssueValid := true.B
      hitTimeUpdateIssueData := hitTimeUpdateNowData
      hitTimeUpdateIssueSetIdx := hitTimeUpdateNowSetIdx
      hitTimeUpdateIssueWaymask := hitTimeUpdateNowWaymask
    }
  }

  //LRU replacement policy
  //timeAccessWArb.io.in(0) for regular R/W hit update access time
  timeAccessWArb.io.in(0).valid := hitTimeUpdateIssueValid
  timeAccessWArb.io.in(0).bits(
    data = hitTimeUpdateIssueData,
    setIdx = hitTimeUpdateIssueSetIdx,
    waymask = hitTimeUpdateIssueWaymask
  )
  //timeAccessWArb.io.in(1) for memRsp allocate
  timeAccessWArb.io.in(1).valid := RegNext(io.allocateWrite.fire, false.B)
  timeAccessWArb.io.in(1).bits(
    data = accessCount,
    setIdx = RegNext(io.allocateWrite.bits.setIdx),
    waymask = io.waymaskReplacement_st1
  )
  timeAccess.io.w.req <> timeAccessWArb.io.out//meta_entry_t::update_access_time

  // ******      dirty_mask_array    ******
  //! 不能使用寄存器阵列实现，因为会将verilog代码扩大10倍有余，编译压力太大
  //! 使用阵列的输出同样有个问题，就是阵列是不能一个clk清零的，同时也为了降低写端口的仲裁，直接将way_dirty寄存器用作阵列的valid信号
  // dirty_mask阵列用来记录cacheline中被修改的字节，在写回L2时，只写dirty_mask中为1的位
  // 这个阵列的设计是为了防止多个SM对同一个cacheline的不同部分进行写入时，导致的一致性问题
  // val dirty_mask = RegInit(VecInit(Seq.fill(set)(VecInit(Seq.fill(way)(VecInit(Seq.fill(dcache_BlockWords)(0.U(BytesOfWord.W))))))))  
  val dirtyMaskAccess = Module(new SRAMTemplate(
    UInt((dcache_BlockWords * BytesOfWord).W),
    set = set,
    way = way,
    shouldReset = true,
    holdRead = true,
    singlePort = false,
    bypassWrite = true
  ))
  // 读有3种情况，首先是每次needreplace时，需要读取被冲刷的dirty_mask；然后是无效化时，需要依次读取所有dirty cacheline的dirty_mask；
  // 最后是每次常规读写命中时，需要读取被命中cacheline的dirty_mask与本次写入的dirty_mask，相或得到本次的dirty_mask
  // 因为针对无效化的读是只要有dirty就会一直读，所以要将其优先级放至最低，否则常规写的命中就会一直读不到
  val dirtyMaskArb = Module(new Arbiter(new SRAMBundleA(set),3))
  dirtyMaskAccess.io.r.req <> dirtyMaskArb.io.out
  // 需要在发生分配写的时候就发起读请求，因为在分配写流程的st1阶段，已经能确定被冲刷的cacheline，且要返回给core内
  // 这时候再发起读请求就跟不上时序。所以可以提前于判断是否命中的时候就读出来，反正最后都由need_replace信号作为总的使能
  dirtyMaskArb.io.in(0).valid := io.allocateWrite.fire
  dirtyMaskArb.io.in(0).bits.setIdx := io.allocateWrite.bits.setIdx
  // 在常规写前命中前，需要读取这个set的dirty mask，如果写命中了，需要将读出来的值与本次写入的dirty mask相或
  dirtyMaskArb.io.in(1) <> io.probeRead
  // 只要有dirty时就会发起读请求，但这个读请求只会在顶层的 flushChoosen 信号拉高时才有效
  // 在无效化时，根据之前的流水级，当hasDirty_st0为真时发起读请求，直接将读到的值赋值给out即可
  dirtyMaskArb.io.in(2).valid := hasDirty_st0
  dirtyMaskArb.io.in(2).bits.setIdx := choosenDirtySetIdx_st0


  // bfs4096-002 fix iter4: dirtyMaskPerCL_init 多 lane 同 word coalescing 漏写 bug。
  // 旧 for + `:=` 对 dynamic-index Vec slot 是 last-write-wins——多 lane 同 blockOffset
  // 不同 wordOffset1H 时仅最大 idx 落地，其它 byte dirty bit 静默丢失（247 条 mismatch
  // 之根：sm 0 wg 80 lane 18+19 同 word 漏 byte 18 → 880 µs 后 stale read）。
  // 修法：per-word OR-reduce 所有同 blockOffset active lane 的 wordOffset1H，与
  // DCache.scala:188-190 dataReqCrossBar.MaskOut 数据路径设计对称。
  // 与 iter1/iter2 正交可叠加。详见 bugs/bfs4096-002/checkpoint_3_iter4.md。

  // 从 perLaneAddr_st1 中构造要写入的 dirty mask（per-word OR-reduce 同 blockOffset 所有 active lane）
  val dirtyMaskPerCL_init = Wire(Vec(dcache_BlockWords, UInt(BytesOfWord.W)))
  val dirtyMaskPerCL = WireInit(VecInit(Seq.fill(dcache_BlockWords)(0.U(BytesOfWord.W))))
  for (j <- 0 until dcache_BlockWords) {
    val perWordContrib = VecInit((0 until num_lane).map { i =>
      Mux(
        io.perLaneAddr_st1(i).activeMask && io.perLaneAddr_st1(i).blockOffset === j.U,
        io.perLaneAddr_st1(i).wordOffset1H,
        0.U(BytesOfWord.W)
      )
    })
    dirtyMaskPerCL_init(j) := perWordContrib.reduce(_ | _)
  }
  // btree-004 P1′/P1″: dirtyMaskPerCL 的 OR-base 计算重排至 waymask_st1_raw 声明之后(L447+, 见下方 "btree-004 P1′" block),
  //   改用 held ST1 identity 做 valid-gate, 消除 live iTagChecker.io.waymask 在 stall 期漂移(codex Phase3.5 维度(3)打回)。
  //   ★build-friendly: dirtyMaskPerCL 是 Wire(L323 WireInit 默认 0), 下方 in(1).bits.apply() 前向引用它(Chisel last-connect 解析),
  //    不 reorder .apply() 本身(approach-b 那样挪 .apply() 触发 Verilator 5.034 V3TSP crash)。

  // 一旦用到 dirtyMaskAccess 读出的值，就应该在下个周期将这个位置的 dirty mask 写0，所以写也需要一个仲裁器
  val dirtyMaskWriteArb = Module(new Arbiter(new SRAMBundleAW(UInt((dcache_BlockWords * BytesOfWord).W), set, way), 3))
  dirtyMaskAccess.io.w.req <> dirtyMaskWriteArb.io.out

  // 分配写在st1确定是否需要替换，如果要替换，读出的dirty mask就是被用到，需要在这个阶段发起写0请求
  // 默认 io.needReplace.get 只会拉高一个周期，且不会被阻塞
  dirtyMaskWriteArb.io.in(0).valid := io.needReplace.get
  dirtyMaskWriteArb.io.in(0).bits.apply(data = 0.U, setIdx = allocateWrite_st1.setIdx, waymask = lockedWayMask)//要素①
  // 在常规读写命中时，即第一级流水发起写请求，只有这个写请求是给阵列写实际值
  // bfs4096-002 fix iter1: 加 io.coreReq_st1_valid gate (cache_hit && probeIsWrite_st1 之外)。
  // cache_hit 与 probeIsWrite_st1 都来自 Queue.deq.bits（不随 deq.valid 归零），
  // 两次 dispatch 之间持续高电平，让 in(1).valid 长期重写 RegNext(setIdx) 的 set，
  // 把上次写入的 dirty bit 抹掉 → evict 用错 mask → byte 级 mismatch。
  // 用 deq.valid 严格 gate 让 valid 仅在真有新 dispatch 拉高。
  // E 方案 (下方 replace_dirty_mask_st1) 修 evict 输出端，与本修法互不冲突。
  // 详见 bugs/bfs4096-002/checkpoint_3.md 迭代 1。
  // bfs4096-007 v15 fix: iter1 的 coreReq_st1_valid gate 只保证 valid 不溢出 dispatch
  // 间, 但 iTagChecker.io.cache_hit (live combinational on SRAM r.resp.data) 在 ST1
  // stall 多拍时 SRAM raddr 被 in(0)/in(2) 偷走 → resp 漂到无关 set → cache_hit 反映
  // ST0 而非 ST1 entry → ST1 实际 miss 时 in(1).valid 误拉高 → dirtyMaskAccess 错位写
  // → bfs4096-007 byte mismatch. 改用 hit_st1_raw (cachehit_hold 出来的 ST1-held hit).
  // L335 setIdx/waymask 在 valid=0 时永不写 SRAM, 不需联动改 (root cause 在 valid 源).
  // 详见 bugs/bfs4096-007/checkpoint_15_valid_gate.md。
  // [hotspot3d512-001 记录, 未修, 与本 fix 无关] dirtyMask 的 OR-base(下方 data=dirtyMaskPerCL, 其 L330 读
  //   dirtyMaskAccess.io.r.resp 是 live)在多拍 stall 期会被"不同 set 的 fill"(in(0)=allocateWrite.fire, L299 抢共享读口)
  //   覆盖 → 漏掉本行已脏字节 → 写回 L2 丢 store。这是 pre-existing 通用洞(baseline 同, 非本 fix 引入), 且 hotspot3d
  //   目标 store 被件2 转 miss 根本不写 dirtyMask, 故收口时不动 dirtyMask, 单列待修: bugs/hotspot3d512-001/dirtyMask_orbase_latent.md。
  dirtyMaskWriteArb.io.in(1).valid :=
    io.coreReq_st1_valid && hit_st1_raw && io.probeIsWrite_st1.get
  dirtyMaskWriteArb.io.in(1).bits.apply(data = dirtyMaskPerCL.asUInt, setIdx = RegNext(io.probeRead.bits.setIdx), waymask = iTagChecker.io.waymask)
  // 只有当 flushChoosen 拉高时，读出来 dirty mask 才会被用到，需要被写0
  // 这里的 valid 需要用 RegNext 延迟一周期是因为在dcache的顶层模块将 InvOrFluMemReqValid_st1 里也延了一个clk
  // 不使用dcache中的 InvOrFluMemReqValid_st1 是因为与tag的发出对齐
  // lud-001 v6 Path A (改动 3e): dirtyMask 清 0 与 way_dirty 清(改动 3d)用同一 gate flushPutFire + 同 identity。
  //   in(2) 最低优先(被 needReplace in0 / write-hit in1 抢则 dirtyMask 这拍不清, 残留旧 mask), 但 way_dirty(3d)已清=0
  //   → 该 (set,way) 永不再被 flush/needReplace 选中 → 残留成 dead state, 在 single-writer/no-external-partial-write 前提下
  //   dead-safe(checkpoint_8 §2.5; 跨 SM partial-write+tag-reuse 移交 dirtyMask facet 3)。P4=M2 实测 lud preempt 应 ==0。
  dirtyMaskWriteArb.io.in(2).valid := io.flushPutFire.get
  dirtyMaskWriteArb.io.in(2).bits.apply(data = 0.U, setIdx = io.flushClrSetIdx.get, waymask = io.flushClrWayMask.get)

  iTagChecker.io.tag_of_set := tagBodyAccess.io.r.resp.data//st1
  //iTagChecker.io.ASID_of_set := ASIDAccess.io.r.resp.data
  iTagChecker.io.tag_from_pipe := io.tagFromCore_st1
  //iTagChecker.io.ASID_from_pipe := io.asidFromCore_st1
  iTagChecker.io.way_valid := way_valid(RegEnable(io.probeRead.bits.setIdx,io.probeRead.fire))//st1
  ////st1

  cachehit_hold.io.enq.bits.hit := iTagChecker.io.cache_hit && !probeReadBuf.ready
  cachehit_hold.io.enq.bits.waymask := Mux(!probeReadBuf.ready, iTagChecker.io.waymask ,0.U)
  cachehit_hold.io.enq.valid := probeReadBuf.valid && !probeReadBuf.ready
  cachehit_hold.io.deq.ready := probeReadBuf.ready
  //val cachehit_hold = RegNext(iTagChecker.io.cache_hit && probeReadBuf.valid && !probeReadBuf.ready)
  val holdValid_st1 = cachehit_hold.io.deq.valid
  // ===== hotspot3d512-001 root-fix v2 / 件2: write-hit vs fill-evict 原子化 (rootfix_design_v2.md) =====
  // 根因: cachehit_hold 在 st1 stall 期(probeReadBuf.ready=0)冻结 hit/waymask, 当 held 命中的 way 在 stall 期间被并发
  //   fill 选作 victim 替换(anti-LRU 保证 victim=刚命中的 MRU way)时, 冻结 hit 不失效 → 出队时 stalled write 承接 stale
  //   hit: 数据落错 way 又不发 L2(被当 write-HIT)。本 fix(件2)把这种 stale-held write 转 write-miss → write-through
  //   把数据送 L2(write no-allocate, 不需 RTAB replay)。配套件1(置脏加 commit 门, 见下方 way_dirty 写)堵 stale 写回。
  // codex Phase3.5 v1-reject 修正: ①detection 覆盖 first-stall —— holdValid 还没起的那拍用 live iTagChecker 值
  //   (st1HitEff_raw/st1WaymaskEff_raw 的 Mux 在 holdValid=0 时取 live), 不再漏掉 fill 与首拍进 stall 同拍的情形;
  //   ②sticky latch 改 clear 优先(见下方), 出队即清永不 stuck → 关 v1 的 set/clear 同拍 stuck-latch dup-tag 隐患;
  //   st1 in-order 单入口 ⇒ latch 天然 request-scoped。(原 v2 用 !ready 互斥, 引入组合环 → 改 probeReadBuf.valid+clear优先。)
  // 只动 write(probeIsWrite): read-hit 撞 fill 有 bfs4096-006/009 fillConflict→replay; cachehit_hold 内部不动 → 不回归
  //   bfs4096-007。AMO/SC/LR 不在 probeIsWrite 内, 本 fix 不覆盖(与 WG-flush 同属另案 D1 暴露面)。
  val st1HitEff_raw     = Mux(holdValid_st1, cachehit_hold.io.deq.bits.hit,     iTagChecker.io.cache_hit)
  val st1WaymaskEff_raw = Mux(holdValid_st1, cachehit_hold.io.deq.bits.waymask, iTagChecker.io.waymask)
  val st1IsWrite_gated  = io.probeIsWrite_st1.getOrElse(false.B)
  // 冲突诞生: write-hit 在 st1(probeReadBuf.valid)且其命中 way == 本拍 fill 替换的 victim (set,way)。
  //   ★用 probeReadBuf.valid 而非 !probeReadBuf.ready 是关键两点★:
  //   ① 断组合环: 原 st1Stalling 含 !probeReadBuf.ready(=!st1_ready), 而 st1_ready 组合依赖 io.tA_Hit_st1.hit
  //      =hit_st1_raw → hit_st1_raw→...→st1_ready→!ready→_set→hit_st1_raw 成环(firtool detected combinational cycle)。
  //      probeReadBuf.valid 是 Queue 寄存输出, 不依赖 ready/hit → 无环。
  //   ② 覆盖 first-stall: probeReadBuf.valid 当拍即真, 含 "store 入 st1 与 fill 决策(allocateLatchEn)同拍" 这种
  //      首拍即 stall 的情形(此时 wasStalling/holdValid 等寄存信号都漏 → 会退回 stale-hit 回归面)。
  //   不需 !ready 限定: write-hit 撞 fill 驱逐它命中的 way, 无论 stall 与否都是冲突(写口 fill 优先 DCachev2:127 →
  //   store 数据落不下 → 本就该当 miss), gate 成 write-miss 恒正确, 不会误伤合法 write-hit(_set 含 allocateLatchEn+同way)。
  // 件2-§5 (codex final-reject §5 修正): fill 选定 victim(allocateLatchEn 脉冲)到真正提交(改 victim tag)之间有一段
  //   pending 窗口 —— 干净 victim(needReplace=0 不拉 blockCoreReq)时 fill 会停在 MemRspPipe 等 memRsp_coreRsp.ready。
  //   这窗口里 victim way 的 tag 还是旧 line, 一条写旧 line 的 store 此时才进 st1 会探成 HIT(allocateLatchEn 已过 → 漏检)
  //   → fill 提交后 held write 落到新 line(污染新 line + 旧 line 写丢失)。读路径有持久保护(fillConflictSt1 用持久
  //   refillWrite_valid, CoreReqPipe:390); 写路径原缺。补 fillVictimPending: victim 选定持久到 fill tag 提交
  //   (io.allocateWriteTagSRAMWValid_st1)→ 覆盖整个冲突窗口。allocateWrite_st1.setIdx/lockedWayMask 窗口内本就冻结
  //   (RegEnable, L125/133-138), 直接复用。全寄存/输入信号 → 不引入组合环。
  val fillVictimPending = RegInit(false.B)
  // ★clear 优先★ (codex §5-review §1/§4 修正): clean victim 且 fill 不停顿时 allocateLatchEn 与 allocateWriteTagSRAMWValid_st1
  //   可同拍(clean→needReplace=0→不拉 blockCoreReq→选定拍直接提交)。set 优先会让 pending 在提交后下一拍才 true,
  //   那拍 way 已装新 line B → 写 B 的合法 write-hit 被错降成 miss → L1 留 B clean 而 write-through 发 L2 → L1/L2 分叉(正确性 bug)。
  //   改 clear 优先: 提交后不留尾窗。select 拍本身的 gate 由 _set 里的组合 (allocateLatchEn && lockedSetIsFull) 兜住(不靠
  //   latch next-state), 故 clear 优先不丢 select 拍覆盖。多 fill 不重叠(codex §3 证: MemRspPipe 单 entry 串行, fill1-clear
  //   与 fill2-set 不同拍)→ clear 优先不会误清别的 fill 窗口。
  when(io.allocateWriteTagSRAMWValid_st1) {                                   // clear 优先: fill 提交(victim tag 改写, 旧 line 不再 hit)
    fillVictimPending := false.B
  }.elsewhen(allocateLatchEn && lockedSetIsFull) {                           // set: 新 victim 选定那拍
    fillVictimPending := true.B
  }
  val writeHitFillConflict_set =
    probeReadBuf.valid && st1HitEff_raw && st1IsWrite_gated &&
    ((allocateLatchEn && lockedSetIsFull) || fillVictimPending) &&            // victim 选定那拍 + 整个 pending 窗口
    (probeReadBuf.bits.setIdx === allocateWrite_st1.setIdx) &&               // 同 set
    (st1WaymaskEff_raw === lockedWayMask)                                     // 同 way ← 命门: 命中的 way 正被驱逐
  val writeHitFillConflictHeld = RegInit(false.B)                            // sticky: 跨多拍 stall 锁住冲突到出队
  // ★clear 优先★: 用 probeReadBuf.valid(非 !ready)后 set 与 clear 可同拍(store 出队 ready=1 时正撞 fill 驱逐它的 way)。
  //   clear 优先使 latch 出队即清、永不 stuck → 关 codex(e) 的 dup-tag 隐患(stuck latch 误 gate 下一条无关请求)。
  //   出队那拍若仍冲突, 由组合 _set 兜住当拍 gate(hit_st1_raw 用 ...||_set), 故 clear 优先不漏当拍 → 安全。
  when(probeReadBuf.valid && probeReadBuf.ready) {                           // clear(优先): held req 出队
    writeHitFillConflictHeld := false.B
  }.elsewhen(writeHitFillConflict_set) {                                     // set: stall 中(含首拍)冲突
    writeHitFillConflictHeld := true.B
  }
  // bfs4096-007 v15: hit_st1_raw 已在前置 Wire 声明, 此处 := 赋值
  hit_st1_raw := st1HitEff_raw && !(writeHitFillConflictHeld || writeHitFillConflict_set)  // ★gate: 冲突→hit 强制 0→write-miss
  val waymask_st1_raw = st1WaymaskEff_raw
  // ===== btree-004 P1′ (read 侧 dirtyMask OR-base held-identity valid-gate) =====
  // btree-004 fix: 用 way_dirty 做 dirtyMask OR-base 的 valid-gate(落实 L283 设计本意: way_dirty=阵列 valid)。
  //   clean way(way_dirty=0)的 dirtyMask SRAM 残留视为无效, 不 OR 进首写 mask; 已脏 way(way_dirty=1)仍正常累积。
  // ★索引用 held ST1 identity(probeReadBuf.bits.setIdx + waymask_st1_raw, 与 L451 isDirty 同源),
  //   不用 RegEnable(set)+live way 混合索引(codex Phase3.5 维度(3)打回: stall 期 live way / dirtyMaskAccess holdRead 读口漂移)。
  val orBaseHeldWay        = OHToUInt(waymask_st1_raw)                          // held way (= L451 isDirty 同源)
  val orBaseHeldSet        = probeReadBuf.bits.setIdx                           // held set (= L451 isDirty 同源)
  val hitWayCurrentlyDirty = way_dirty(orBaseHeldSet)(orBaseHeldWay).asBool
  val dirtyMaskOrBase      = Mux(hitWayCurrentlyDirty,
                                 dirtyMaskAccess.io.r.resp.data(orBaseHeldWay), // OR-base 用同一 held way 选
                                 0.U)
  dirtyMaskPerCL := (dirtyMaskPerCL_init.asUInt | dirtyMaskOrBase).asTypeOf(dirtyMaskPerCL)
  // ★btree-004 P1″ (写回端口 in(1) 也改 held identity) 已实测引入回归(command-j 路径 bid=-1 + btree-003 reclength=0 复发,
  //   isolated-serial 铁实, codex 设计审被 build-trial 推翻), 故 P1′ 只保留上方读侧 OR-base held valid-gate,
  //   in(1) 写回沿用原 live 信号(empirically 正确)。写回端口 identity latent = codex 标记的理论概念(无 failing seed),
  //   held-write-back 闭合法错误, 残留待人类用不同 approach 或接受 live。
  io.hit_st1 := hit_st1_raw && probeReadBuf.valid//RegNext(io.probeRead.fire) //todo remove
  io.hitStatus_st1.hit := hit_st1_raw && probeReadBuf.valid
  io.hitStatus_st1.waymask := waymask_st1_raw
  io.hitStatus_st1.isDirty := way_dirty(probeReadBuf.bits.setIdx)(OHToUInt(waymask_st1_raw))
  io.hitStatus_st1.tag := tagBodyAccess.io.r.resp.data(OHToUInt(io.hitStatus_st1.waymask))
  io.waymaskHit_st1 := waymask_st1_raw
  if(!readOnly){//tag_array::write_hit_mark_dirty
    //assert(!(iTagChecker.io.cache_hit && io.probeIsWrite_st1.get && io.flushChoosen.get),"way_dirty write-in conflict!")
    // 件1 (hotspot3d512-001 root-fix v2): 置脏加 commit 门 probeReadBuf.ready(=st1_ready), 与数据落盘 coreWriteHitFire
    //   (=st1_valid && st1_ready && WriteHit, DCachev2:104) 同拍 → dirty 与 data 原子。堵根因: 原条件一命中就置脏(不管
    //   数据落没落), stall 期 hit=1 & ready=0 → dirty 早置 1 拍、数据没落 → needReplace(L425, 组合读 way_dirty)在
    //   allocateLatchEn 拍读到这个脏 victim → fill 驱逐写回 pre-store 旧值腐败 L2。必须"不让它置上"(清不掉: 同步写要下
    //   一拍才 visible, 而 needReplace 当拍已读完)。正常 write-hit ready=1 同拍 → 行为不变。
    when(io.hitStatus_st1.hit && io.probeIsWrite_st1.get && probeReadBuf.ready){////meta_entry_t::write_dirty
      // 件1a (root-fix v3, codex v2-reject §1 修正): set index 用 held probeReadBuf.bits.setIdx(与 L409 脏位读对齐),
      //   不用 RegNext(io.probeRead.bits.setIdx)(live 探针, 多拍 stall 漂到别 set → 数据落 setA、脏位写 setB → committed store 被丢)。
      way_dirty(probeReadBuf.bits.setIdx)(OHToUInt(io.hitStatus_st1.waymask)) := true.B
    }.elsewhen(io.flushPutFire.get){//tag_array::flush_one (lud-001 v6 Path A 改动 3d: 清脏移到 PutPart fire + 传入 identity)
      // ★治本(batch-loss): 反压没 fire → flushPutFire=0 → way_dirty 不清 → 该脏行不丢; fire 才清且清的恒是这笔 Put 自己。
      //   gate 从 live flushChoosen(st0 cursor 选中拍, 不等出站) 改 flushPutFire; set/way 从 live choosenDirty*_st0 改传入
      //   flushClrSetIdx/WayMask(snap/live Mux 与 a_addr 同源 → 无 clear-index drift)。不经 in(2) 仲裁(Reg 直接赋)→ 根除 retry。
      way_dirty(io.flushClrSetIdx.get)(OHToUInt(io.flushClrWayMask.get)) := false.B
    }.elsewhen(io.needReplace.get) {
      way_dirty(allocateWrite_st1.setIdx)(OHToUInt(lockedWayMask)) := false.B//要素①
    }.elsewhen(iTagChecker.io.cache_hit && io.probeIsUncache_st1 && probeReadBuf.ready){
      way_dirty(RegNext(io.probeRead.bits.setIdx))(OHToUInt(iTagChecker.io.waymask)) := false.B
    }
    // lud-001 v6 Path A (改动 4) assert/monitor (L1TagAccess 侧, if(!readOnly) 内):
    // A1: way_dirty 清的 waymask 必 one-hot (codex (e) clear identity)
    assert(!io.flushPutFire.get || PopCount(io.flushClrWayMask.get) === 1.U,
           "lud-001: flush dirty-clear waymask not one-hot")
    // M2: dirtyMask clear in(2) 被 in(0)needReplace/in(1)write-hit 抢频率 (P4 数据源, 关 facet 3; ==0 → residual 不产生 → moot)
    val dirtyClearPreemptCount = RegInit(0.U(32.W))
    when(io.flushPutFire.get && !dirtyMaskWriteArb.io.in(2).fire){
      dirtyClearPreemptCount := dirtyClearPreemptCount + 1.U
    }
    dontTouch(dirtyClearPreemptCount)
  }




  if (!readOnly) {
    io.needReplace.get := way_dirty(allocateWrite_st1.setIdx)(OHToUInt(lockedWayMask)).asBool && RegNext(io.allocateWrite.fire, false.B)//要素①
    io.replaceValidVictim_st1.get := RegNext(io.allocateWrite.fire, false.B) && lockedSetIsFull//要素①
  }
  // ******      tag_array::allocate    ******
  Replacement.io.validOfSet := Reverse(Cat(way_valid(allocateWrite_st1.setIdx)))//Reverse(Cat(way_valid(io.allocateWrite.bits.setIdx)))
  Replacement.io.timeOfSet_st1 := timeAccess.io.r.resp.data//meta_entry_t::get_access_time
  io.waymaskReplacement_st1 := lockedWayMask//tag_array::replace_choice (要素①: 锁存 way 导出 → 透传 MemRspPipe data-write/victim-read)
  val tagnset = Cat(tagBodyAccess.io.r.resp.data(OHToUInt(lockedWayMask)), //tag 要素①
    allocateWrite_st1.setIdx)

  // bfs4096-002 fix iter2: a_addrReplacement_st1 与 mask 路径对称的 RegEnable 锁存。
  // 旧版纯组合 Cat：tagAccessRArb.in(2) "port 空就用" 默认填充让 R 端口在 P+1 拍切到
  // choosenDirtySetIdx_st0，r.resp.data 漂到无关 set，组合 a_addr 跟漂；DCachev2:146
  // 在 P+1 拍 when(replaceReadResp){...} 采样时已漂走 → PutPart 写错 cacheline。
  // 修法：P 拍 RegEnable(io.needReplace.get) 锁住 raw 组合值，与 mask 路径 (E 方案) 同款。
  // 同型 latent: choosenDirtyTag_st1 也直出 r.resp.data，flush 路径未覆盖（待回视）。
  // 详见 bugs/bfs4096-002/checkpoint_4_5_iter2.md + checkpoint_3_iter2.md。
  val a_addrReplacement_st1_raw = Cat(tagnset, //setIdx
    0.U((dcache_BlockOffsetBits + dcache_WordOffsetBits).W)) //blockOffset+wordOffset

  if (!readOnly) {
    io.a_addrReplacement_st1.get :=
      RegEnable(a_addrReplacement_st1_raw, 0.U, io.needReplace.get)
  }
  // bfs4096-002 fix (E 方案): 在 needReplace=1 (T0) 那拍把 dirtyMaskAccess 的 read
  // resp 锁进 reg；T1 SRAM 内部 bypassWrite 污染成 0 不再有任何后果（下游 DCachev2
  // L149 replaceMaskReg / L314 replaceMaskSel fallback 都消费 latched 值）。
  // 不动 in(0) 写时机 / in(1) WriteHit / in(2) 预读 / flush 路径，只保护 evict 输出端。
  // 详见 bugs/bfs4096-002/checkpoint_3.md (迭代 2) + checkpoint_4_5_D_failed.md。
  val replace_dirty_mask_st1_raw =
    dirtyMaskAccess.io.r.resp.data(OHToUInt(lockedWayMask)).asUInt//要素①
  io.replace_dirty_mask_st1 :=
    RegEnable(replace_dirty_mask_st1_raw, 0.U, io.needReplace.get)

  tagBodyAccess.io.w.req.valid := io.allocateWriteTagSRAMWValid_st1//meta_entry_t::allocate
  tagBodyAccess.io.w.req.bits.apply(data = io.allocateWriteData_st1, setIdx = allocateWrite_st1.setIdx, waymask = lockedWayMask)//要素①


  // 要素②: way_valid gate 从 RegNext(allocateWrite.fire)(T+1 早置) 改为 allocateWriteTagSRAMWValid_st1
  //   (= dAmemRsp_wReq_valid, 与 tag/data 同 commit 拍) → way_valid 与 tag/data 原子提交, 消除 early-valid 窗口。
  //   waymask 用 lockedWayMask(要素①), !Set_is_full 守卫换 lockedSetIsFull(snapshot 一致)。
  when(io.allocateWriteTagSRAMWValid_st1 && !lockedSetIsFull){//meta_entry_t::allocate TODO
    way_valid(allocateWrite_st1.setIdx)(OHToUInt(lockedWayMask)) := true.B
  }.elsewhen(io.invalidateAll){//tag_array::invalidate_all()
    way_valid := VecInit(Seq.fill(set)(VecInit(Seq.fill(way)(false.B))))
  }.elsewhen (iTagChecker.io.cache_hit && io.probeIsUncache_st1) {
    way_valid(probeReadBuf.bits.setIdx)(OHToUInt(iTagChecker.io.waymask)) := false.B
  }
  assert(!(io.allocateWrite.valid && io.invalidateAll))
  // ====== bfs4096-008 诊断 build: 4 个不变量降级 non-fatal counter (codex v7: 保留可观测性, 不 $stop, 跑完看趋势) ======
  // #13 要素① 锁存的 victim way 必须 one-hot (factor① 正确性 ⇒ count 应=0)
  val lockedWayMaskBadCount = RegInit(0.U(32.W))
  when(allocateLatchEn && (PopCount(lockedWayMask) =/= 1.U)){ lockedWayMaskBadCount := lockedWayMaskBadCount + 1.U }
  dontTouch(lockedWayMaskBadCount)
  // #18 §9 验收: way_valid commit 与 invalidateAll 不得同拍 (drain 门槛奏效 ⇒ count 应=0)
  val commitVsInvCount = RegInit(0.U(32.W))
  when(io.allocateWriteTagSRAMWValid_st1 && io.invalidateAll){ commitVsInvCount := commitVsInvCount + 1.U }
  dontTouch(commitVsInvCount)
  // #19 §9.6 uncache benign: uncache 清 valid 不得命中 pending fill victim way (benign ⇒ count 应=0)
  val uncacheVictimCount = RegInit(0.U(32.W))
  when(iTagChecker.io.cache_hit && io.probeIsUncache_st1 && allocateLatchEn && (iTagChecker.io.waymask === lockedWayMask)){ uncacheVictimCount := uncacheVictimCount + 1.U }
  dontTouch(uncacheVictimCount)
  // #14 dup-tag (核心观测): 命中端同 set ≥2 way 同 tag 都 valid → PopCount(waymask)>1。
  //   双要素+§9 应消除 ⇒ count 应=0。首例 printf 现场 (setIdx/tag/waymask) 供修根因 (codex v7 §4.3)。
  // bfs4096-010 fix (诊断 counter 自身误报修复): 原检测用裸 live iTagChecker.cache_hit/waymask (无 hold 保护) 会假阳性 —
  //   tagAccessRArb in(0)=allocateWrite 优先级 > in(1)=probe (L157-166), allocateWrite.valid 拍抢 tag SRAM raddr,
  //   iTagChecker.tag_of_set (=tagBodyAccess live r.resp.data, holdRead=true, L364) 反映 allocateWrite.setIdx 的 tag,
  //   与 iTagChecker.way_valid (registered probe setIdx, L368) 错配 → raddr 切换 cross-cycle 瞬态凑出虚假 waymask=0b11。
  //   实证 bugs/bfs4096-010: 物理 cell Mem[5]=0x9000290007 (仅 way1=0x90002), DUPTAG fire 拍 R0_addr=31≠probe set5,
  //   R0_data=0x9000290002 不等任何真实 cell = 纯瞬态 (waveform-analyst v2 物理 cell vs live resp 对照)。
  //   修法 gate (waveform-analyst v3 现有 fst 实测确认实际起效机制, 更正 "hold 洗 waymask" 的初始假设):
  //     ① probeReadBuf.valid: glitch 拍 probe buf 空(无真实 probe in st1) → 不检测。消除已知两拍误报的主因。
  //        (注: glitch 拍 holdValid_st1=0, hit_st1_raw/waymask_st1_raw 回落 live 仍 0b11, 并未被 hold 洗净 → 不能只靠 waymask_st1_raw。)
  //     ② !io.allocateWrite.valid: 挡 allocateWrite(tagAccessRArb in0 优先) 抢 raddr 的拍(v2 实证 glitch 主源), 覆盖 probeReadBuf.valid=1 的 corner。
  //   残留: SRAM holdRead 下"上拍 allocate read 滞留 resp"corner 未必全挡 → rebuild 重跑实测 DUPTAG 是否归零(真实 dup-tag 才该 fire)。
  val dupTagCount = RegInit(0.U(32.W))
  val dupTagFirst = RegInit(true.B)
  when((hit_st1_raw && probeReadBuf.valid && !io.allocateWrite.valid) && (PopCount(waymask_st1_raw) > 1.U)){
    dupTagCount := dupTagCount + 1.U
    when(dupTagFirst){
      dupTagFirst := false.B
      printf(p"DUPTAG set=${probeReadBuf.bits.setIdx} tag=${io.tagFromCore_st1} waymask=${waymask_st1_raw}\n")
    }
  }
  dontTouch(dupTagCount)


    //set一般值为128。
    //评估后，每set配priority mux的成本约为所有set普通mux后共用priority mux的5-6倍，
    //代价是普通 mux 7个2in1 mux的延迟。
    for (i <- 0 until set) {
      way_dirtyAfterValid(i) := VecInit(way_dirty(i).zip(way_valid(i)).map { case (v, d) => v & d })
      setDirty(i) := way_dirtyAfterValid(i).asUInt.orR
    }
    hasDirty_st0 := setDirty.asUInt.orR
    choosenDirtySetIdx_st0 := PriorityEncoder(setDirty)
    choosenDirtySetValid := way_dirtyAfterValid(choosenDirtySetIdx_st0)
    choosenDirtyWayMask_st0 := VecInit(PriorityEncoderOH(choosenDirtySetValid)).asUInt
    choosenDirtyWayMask_st1 := RegNext(choosenDirtyWayMask_st0, 0.U)
    choosenDirtyTag_st1 := tagBodyAccess.io.r.resp.data(OHToUInt(choosenDirtyWayMask_st1))//todo:check correctness

    //val choosenDirtySetIdx_st1 = RegNext(choosenDirtySetIdx_st0)
    //val choosenDirtyWayMask_st1 = RegNext(choosenDirtyWayMask_st0)
    io.dirtyTag_st1.get := choosenDirtyTag_st1
    io.dirtySetIdx_st0.get := choosenDirtySetIdx_st0
    io.dirtyMask_st1 := dirtyMaskAccess.io.r.resp.data(OHToUInt(choosenDirtyWayMask_st1)).asUInt

    io.dirtyWayMask_st0.get := choosenDirtyWayMask_st0
    io.hasDirty_st0.get := hasDirty_st0//RegNext(hasDirty_st0)

}}

class ReplacementUnit(timeLength:Int, way: Int, debug:Boolean=false) extends Module{
  val io = IO(new Bundle {
    val validOfSet = Input(UInt(way.W))//MSB at left
    val timeOfSet_st1 = Input(Vec(way,UInt(timeLength.W)))//MSB at right
    val waymask_st1 = Output(UInt(way.W))
    val Set_is_full = Output(Bool())
  })
  val wayIdxWidth = log2Ceil(way)
  val victimIdx = if (way>1) Wire(UInt(wayIdxWidth.W)) else Wire(UInt(1.W))
  io.Set_is_full := io.validOfSet === Fill(way,1.U)

  if (way>1) {
    val timeOfSetAfterValid = Wire(Vec(way,UInt(timeLength.W)))
    for (i <- 0 until way)
      timeOfSetAfterValid(i) := Mux(io.validOfSet(i),io.timeOfSet_st1(i),0.U)
    val minTimeChooser = Module(new minIdxTree(width=timeLength,numInput=way))
    minTimeChooser.io.candidateIn := timeOfSetAfterValid
    victimIdx := minTimeChooser.io.idxOfMin
  }else victimIdx := 0.U

  io.waymask_st1 := UIntToOH(Mux(io.Set_is_full, victimIdx, PriorityEncoder(~io.validOfSet)))
  // First case, set not full
  //Second case, full set, replacement happens

  //debug use
  if(debug){
    when(io.validOfSet.asBools.reduce(_ | _) === true.B) {
      printf(io.validOfSet.asBools.map{ x => p"${x} " }.reduceOption(_ + _).getOrElse(p"") + p"\n" +
             io.timeOfSet_st1.reverse.map{ x => p"${x} " }.reduceOption(_ + _).getOrElse(p"") +
             p"\noutput: ${io.waymask_st1}\n")
    }
  }
}
class tagChecker(way: Int, tagIdxBits: Int, AsidBits: Int) extends Module{
  val io = IO(new Bundle {
    val tag_of_set = Input(Vec(way,UInt(tagIdxBits.W)))//MSB the valid bit
    val ASID_of_set = if(MMU_ENABLED) Some{Input(Vec(way,UInt(AsidBits.W)))} else None
    //val valid_of_set = Input(Vec(way,Bool()))
    val tag_from_pipe = Input(UInt(tagIdxBits.W))
    val ASID_from_pipe = if(MMU_ENABLED) Some{Input(UInt(AsidBits.W))} else None
    val way_valid = Input(Vec(way,Bool()))

    val waymask = Output(UInt(way.W))//one hot
    val cache_hit = Output(Bool())
  })

  //io.waymask := Cat(io.tag_of_set.zip(io.way_valid).map{ case(tag,valid) => (tag === io.tag_from_pipe) && valid})
  val tagMatch = Wire(UInt(way.W))
  if(MMU_ENABLED){
    val ASIDMatch = Wire(UInt(way.W))
    ASIDMatch := Reverse(Cat(io.ASID_of_set.get.zip(io.way_valid).map{ case(tag,valid) => (tag === io.ASID_from_pipe.get) && valid}))
    io.waymask := tagMatch & ASIDMatch
  } else {
    io.waymask := tagMatch
  }

  tagMatch :=   Reverse(Cat(io.tag_of_set.zip(io.way_valid).map{ case(tag,valid) => (tag === io.tag_from_pipe) && valid}))


  //Reverse(Cat(io.tag_of_set.zip(io.way_valid).map{ case(tag,valid) => (tag === io.tag_from_pipe) && valid}))
  //io.waymask := Reverse(Cat(io.tag_of_set.map{ tag => (tag(tagIdxBits-1,0) === io.tag_from_pipe) && tag(tagIdxBits)}))
  //assert(PopCount(io.waymask) <= 1.U)//if waymask not one-hot, duplicate tags in one set, error
  io.cache_hit := io.waymask.orR
}

class minIdxTree(width: Int, numInput: Int) extends Module{
  val treeLevel = log2Ceil(numInput)
  val io = IO(new Bundle{
    val candidateIn = Input(Vec(numInput, UInt(width.W)))
    val idxOfMin = Output(UInt(treeLevel.W))
  })
  class candWithIdx extends Bundle{
    val candidate = UInt(width.W)
    var index = UInt(treeLevel.W)
  }
  def minWithIdx(a:candWithIdx, b:candWithIdx): candWithIdx = Mux(a.candidate < b.candidate,a,b)

  val candVec = Wire(Vec(numInput,new candWithIdx))
  for(i <- 0 until numInput){
    // ===== true-LRU 替换策略 (hotspot3d512-001 D2 + nn64k-009 死锁修复配套启用) =====
    // 原 anti-LRU: candidateIn(numInput-1-i) 镜像下标 + 消费端 UIntToOH(idxOfMin) 无补偿
    //   ⇒ victim 恒取 time 较大方(MRU) = textbook-wrong 的 anti-LRU(网表 dut.v idxOfMin=
    //   candidateIn_1>=candidateIn_0 实锤, 见 nn64k-009 manifest netlist_audit), 伤命中率。
    // 改正序 candidateIn(i) ⇒ idxOfMin 与 way 索引同空间 ⇒ victim=最小 time=最久未用=true-LRU。
    // ★前置依赖★: true-LRU 触发 L1 DCache st1⇄RTAB 循环资源死锁(nn_64k HANG, anti-LRU 一直掩盖),
    //   必须与 nn64k-009 的 RTAB-reservation 修复(CoreReqPipe.Req_st1_RTAB_reserve + DCachev2.allowIn1)
    //   一同启用; 单独翻 true-LRU 不修死锁会在 nn_64k seed 631470333 确定性 HANG@7862815。
    candVec(i).candidate := io.candidateIn(i)
    candVec(i).index := i.asUInt
  }

  io.idxOfMin := candVec.reduceTree(minWithIdx(_,_)).index
}
class L1TagAccess_ICache(set: Int, way: Int, tagBits: Int, AsidBits: Int)extends Module{
  //This module contain Tag memory, its valid bits, tag comparator, and Replacement Unit
  val io = IO(new Bundle {
    val r = Flipped(new SRAMReadBus(UInt(tagBits.W), set, way))
    val r_asid = if(MMU_ENABLED) Some{Flipped(new SRAMReadBus(UInt(AsidBits.W), set, way))} else None
    val tagFromCore_st1 = Input(UInt(tagBits.W))
    val asidFromCore_st1 = if(MMU_ENABLED) Some{Input(UInt(AsidBits.W))} else None
    val coreReqReady = Input(Bool())

    val w = Flipped(new SRAMWriteBus(UInt(tagBits.W), set, way))
    val w_asid = if(MMU_ENABLED) Some{Flipped(new SRAMWriteBus(UInt(AsidBits.W), set, way))} else None

    val waymaskReplacement = Output(UInt(way.W))//one hot, for SRAMTemplate
    val waymaskHit_st1 = Output(UInt(way.W))

    val hit_st1 = Output(Bool())

    val invalidate = Input(Bool())
  })
  val tagBodyAccess = Module(new SRAMTemplate(
    UInt(tagBits.W),
    set = set,
    way = way,
    shouldReset = false,
    holdRead = true,
    singlePort = false,
    bypassWrite = false
  ))
  tagBodyAccess.io.r <> io.r
  tagBodyAccess.io.r.req.valid := io.r.req.valid && !io.invalidate
  io.r.req.ready := tagBodyAccess.io.r.req.ready && !io.invalidate

  val way_valid = RegInit(VecInit(Seq.fill(set)(VecInit(Seq.fill(way)(0.U(1.W))))))
  //val way_valid = Mem(set, UInt(way.W))
  // ******      Replacement    ******
  val Replacement = Module(new ReplacementUnit_ICache(way))
  // ******      TagChecker    ******
  val iTagChecker = Module(new tagChecker(way = way, tagIdxBits = tagBits, AsidBits = AsidBits))
  iTagChecker.io.tag_of_set := tagBodyAccess.io.r.resp.data //st1
  iTagChecker.io.tag_from_pipe := io.tagFromCore_st1
  if(MMU_ENABLED) {
    val asidAccess = Module(new SRAMTemplate(
      UInt(AsidBits.W),
      set = set,
      way = way,
      shouldReset = false,
      holdRead = true,
      singlePort = false,
      bypassWrite = false
    ))

    asidAccess.io.r <> io.r_asid.get
    asidAccess.io.r.req.valid := io.r.req.valid && !io.invalidate
    io.r.req.ready := asidAccess.io.r.req.ready && !io.invalidate
    iTagChecker.io.ASID_of_set.get := asidAccess.io.r.resp.data
    iTagChecker.io.ASID_from_pipe.get := io.asidFromCore_st1.get
    asidAccess.io.w.req.valid := io.w_asid.get.req.valid && !io.invalidate
    io.w_asid.get.req.ready := asidAccess.io.w.req.ready && !io.invalidate
    asidAccess.io.w.req.bits.apply(data = io.w_asid.get.req.bits.data, setIdx = io.w_asid.get.req.bits.setIdx, waymask = Replacement.io.waymask)
  }
  iTagChecker.io.way_valid := way_valid(RegEnable(io.r.req.bits.setIdx, io.coreReqReady)) //st1
  io.waymaskHit_st1 := iTagChecker.io.waymask //st1
  io.hit_st1 := iTagChecker.io.cache_hit

  Replacement.io.validbits_of_set := Cat(way_valid(io.w.req.bits.setIdx))
  io.waymaskReplacement := Replacement.io.waymask
  tagBodyAccess.io.w.req.valid := io.w.req.valid && !io.invalidate

  io.w.req.ready := tagBodyAccess.io.w.req.ready && !io.invalidate
  tagBodyAccess.io.w.req.bits.apply(data = io.w.req.bits.data, setIdx = io.w.req.bits.setIdx, waymask = Replacement.io.waymask)

  when(io.invalidate) {
    way_valid := 0.U.asTypeOf(way_valid)
  } .elsewhen(io.w.req.valid && !Replacement.io.Set_is_full) {
    way_valid(io.w.req.bits.setIdx)(OHToUInt(Replacement.io.waymask)) := true.B
  }

}
class ReplacementUnit_ICache(way: Int) extends Module{
  val io = IO(new Bundle {
    val validbits_of_set = Input(UInt(way.W))
    val waymask = Output(UInt(way.W))//one hot
    val Set_is_full = Output(Bool())
  })
  val victim_1Hidx = if (way>1) RegInit(1.U(way.W)) else RegInit(0.U(1.W))
  io.Set_is_full := io.validbits_of_set === Fill(way,1.U)
  io.waymask := Mux(io.Set_is_full, victim_1Hidx, UIntToOH(VecInit(io.validbits_of_set.asBools).indexWhere(_===false.B)))
  // First case, set not full
  //Second case, full set, replacement happens
  if (way>1) victim_1Hidx := RegEnable(Cat(victim_1Hidx(way-2,0),victim_1Hidx(way-1)),io.Set_is_full)
}

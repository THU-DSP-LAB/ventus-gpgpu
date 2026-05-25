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
import L1Cache.DCache.DCacheParameters._
import SRAMTemplate._
import chisel3._
import chisel3.util._
import config.config.Parameters
import top.parameters._
import mmu.SV32.{asidLen, paLen, vaLen}
import L1Cache._

class memRspPipe_st1(implicit p: Parameters) extends DCacheBundle{
  val Rsp  = new DCacheMemRsp
  val isRead = Bool()
  val isSpecial = Bool()
  val isCached = Bool()
}

class MemRspPipe(implicit p: Parameters) extends DCacheModule{
    val io = IO(new Bundle{
         val memRsp = Flipped(DecoupledIO(new DCacheMemRsp))
         val memRspIsFlu = Output(Bool())
         //st0
         val tAAllocateWriteReq = ValidIO(new SRAMBundleA(NSets)) // ta allocate write
         val RTABUpdateReq       = ValidIO(new RTABUpdate) // Update RTAB
         val MSHRMissRsp        = Decoupled(new MSHRmissRspIn(NMshrEntry))
         val SMSHRMissRsp       = Decoupled(new MSHRmissRspIn(NMshrEntry))
         val WSHRPopReq         = ValidIO(UInt(log2Up(NMshrEntry).W))         
         val MSHRMissRspOutUCached = Input(Bool())

         //st1
         val memRsp_coreRsp     = DecoupledIO(new CoreRspPipe_st2)
         // MSHRmissRspOut.instrId width = max(WIdBits, log2Up(NMshrEntry)) to allow
         // NMshrEntry > num_warp; see bugs/bfs4096-003/phase_4_report.md.
         val MSHRMissRspOut     = Flipped(DecoupledIO(new MSHRmissRspOut(bABits, tIBits, math.max(WIdBits, log2Up(NMshrEntry)), asidLen)))
         val MSHRMissRspOutAsid = if(MMU_ENABLED) Some(Input(UInt(asidLen.W))) else None

         val SMSHRMissRspOut     = Flipped(DecoupledIO(new MSHRmissRspOut(bABits, tIBits, math.max(WIdBits, log2Up(NMshrEntry)), asidLen)))
         val SMSHRMissRspOutAsid = if(MMU_ENABLED) Some(Input(UInt(asidLen.W))) else None

         val dAmemRsp_wReq         = Output(Vec(BlockWords, new SRAMBundleAW(UInt(8.W), NSets * NWays, BytesOfWord)))
         val dAmemRsp_wReq_valid   = Output(Bool())
         // dAmemRsp_wReq_valid 拉高时，对应的写回 cacheline blockAddr（tag+set），用于 coreReqPipe 做同拍冲突规避
         val dAmemRsp_wReq_blockAddr = Output(UInt(bABits.W))
         // bfs4096-006 fix: 暴露 fill 写入的精确 dA row = Cat(set, victim_way)。
         // 现有 dAmemRsp_wReq_blockAddr 只含 tag+set 不含 way，无法区分"跨 cacheline 同 (set, way) 撞"的场景。
         // CoreReqPipe ST1 用这个比对 hit-read 命中的 Cat(set, hit_way) 检测 fillConflictSt1。
         val dAmemRsp_wReq_setIdx    = Output(UInt(log2Ceil(NSets * NWays).W))
         // bfs4096-006 fix: fill intent (= st1_valid, 不含 st1_ready 反馈)。
         // dAmemRsp_wReq_valid = st1_valid && st1_ready 与下游 memRsp_coreRsp.ready 形成组合环
         // (FIRRTL detected combinational cycle)。给 coreReqPipe 用 intent 破环，代价是 fill stall
         // 时 hit 被保守 replay (fill stall 没写 dA 不会污染，replay 走 1 轮 latency 影响极小)。
         val dAmemRsp_wReq_intent    = Output(Bool())
         val dAmemRsp_wReq_asid = if(MMU_ENABLED) Some(Output(UInt(asidLen.W))) else None
         val dAReplace_rReq        = Output(Vec(BlockWords, new SRAMBundleA(NSets * NWays)))
         val dAReplace_rReq_valid  = Output(Bool())
         val memReq_valid          = Output(Bool())
         val tAWayMask             = Input(UInt(NWays.W))
         val needReplace           = Input(Bool())
         val memReq_ready          = Input(Bool())
         // dirty replace 期间禁止 core 新请求进入，避免 victim line 仍可 write-hit
         val blockCoreReq          = Output(Bool())

    })
    // st0
    val MemRsp_pipeReg_st0_st1 = Module(new Queue(new memRspPipe_st1,entries = 1,flow=false,pipe=true)).io

    val st0_ready = Wire(Bool())
    val st0_valid = Wire(Bool())
    val RTABUpdateReq_valid = Wire(Bool())
    val idx_st0 = get_ID(io.memRsp.bits.d_source)
    val missRspEntryIdx_st1 = Wire(UInt(log2Up(NMshrEntry).W))
    // regular read 的整条回包数据按 instrId 暂存。
    // 这样在 MSHRMissRspOut 给出 targetInfo 时，coreRsp 可以取到与当前 miss 身份绑定的 line data。
    val missRspDataByInstrId = Reg(Vec(NMshrEntry, Vec(BlockWords, UInt(WordLength.W))))
    val memRspisRead = io.memRsp.bits.d_opcode === 1.U && io.memRsp.bits.d_param === 0.U
    val memRspisWrite = io.memRsp.bits.d_opcode === 0.U
    val memRspisFlushOrInv = io.memRsp.bits.d_opcode === 2.U
    val memRspisLRSC = io.memRsp.bits.d_opcode === 1.U && (get_Type(io.memRsp.bits.d_source) === 2.U)
    val memRspisAMO  = io.memRsp.bits.d_opcode === 1.U && (get_Type(io.memRsp.bits.d_source) === 2.U)
    val memRspisSpecial = memRspisLRSC || memRspisAMO
    io.memRsp.ready := st0_ready
    // st1
    val missRspTI_st1 = Wire(new VecMshrTargetInfo)
    val memRsp_st1_isRead = MemRsp_pipeReg_st0_st1.deq.bits.isRead
    val memRsp_st1_isSpecial = MemRsp_pipeReg_st0_st1.deq.bits.isSpecial && MemRsp_pipeReg_st0_st1.deq.valid
    val dAReq_valid = Wire(Bool())
    val st1_ready = Wire(Bool())
    val tAAllocateWriteReq_valid = WireInit(false.B)

    // -----st0-----
    io.memRspIsFlu := memRspisFlushOrInv && io.memRsp.valid
    io.tAAllocateWriteReq.valid := tAAllocateWriteReq_valid
    io.tAAllocateWriteReq.bits.setIdx := io.memRsp.bits.d_source(SetIdxBits-1,0)
    io.MSHRMissRsp.valid := io.memRsp.valid && memRspisRead
    io.MSHRMissRsp.bits.instrId := idx_st0
    io.SMSHRMissRsp.valid := io.memRsp.valid && memRspisSpecial
    io.SMSHRMissRsp.bits.instrId := idx_st0

    io.RTABUpdateReq.bits.mshrIdx := idx_st0
    io.RTABUpdateReq.bits.wshrIdx := idx_st0
    io.RTABUpdateReq.bits.blockAddr := get_blockAddr(io.memRsp.bits.d_addr)
    io.RTABUpdateReq.bits.updateType := 0.U
    io.RTABUpdateReq.valid := RTABUpdateReq_valid
    io.WSHRPopReq.bits := idx_st0
    // write response 在 st0 一定可接收，因此 WSHR pop 只需要跟随 valid。
    // 如果这里改成 memRsp.fire，会把 ready 反向带进 valid，形成 ready->valid 组合回环。
    io.WSHRPopReq.valid := io.memRsp.valid && memRspisWrite

    when(memRspisLRSC || memRspisAMO){
        io.RTABUpdateReq.bits.updateType := 2.U
    }.elsewhen(memRspisWrite){
        io.RTABUpdateReq.bits.updateType := 1.U
    }
    st0_ready := false.B
    when(memRspisFlushOrInv){
        st0_ready := true.B      
    }.elsewhen(memRspisWrite){
        st0_ready := true.B
    }.elsewhen(memRspisSpecial){
        st0_ready := io.SMSHRMissRsp.ready && MemRsp_pipeReg_st0_st1.enq.ready
    }.elsewhen(memRspisRead){
        st0_ready := io.MSHRMissRsp.ready && MemRsp_pipeReg_st0_st1.enq.ready
    }
    RTABUpdateReq_valid := io.memRsp.valid && st0_ready
    st0_valid := false.B
    when(io.memRsp.valid){
        when(memRspisFlushOrInv){
           st0_valid :=  false.B      
       }.elsewhen(memRspisWrite){
           st0_valid := false.B
       }.elsewhen(memRspisSpecial){
           st0_valid := io.memRsp.valid // to check
       }.elsewhen(memRspisRead){
           st0_valid := io.memRsp.valid
       }
    }
    val memRspMetaReady_st0 = Mux(memRspisSpecial, io.SMSHRMissRsp.ready, io.MSHRMissRsp.ready)
    MemRsp_pipeReg_st0_st1.enq.valid := st0_valid && memRspMetaReady_st0
    MemRsp_pipeReg_st0_st1.enq.bits.Rsp := io.memRsp.bits
    MemRsp_pipeReg_st0_st1.enq.bits.isRead := memRspisRead
    MemRsp_pipeReg_st0_st1.enq.bits.isSpecial := memRspisLRSC || memRspisAMO
    MemRsp_pipeReg_st0_st1.enq.bits.isCached := Mux(memRspisRead,!io.MSHRMissRspOutUCached,false.B)
    // val memRspEnqFire = st0_valid && MemRsp_pipeReg_st0_st1.enq.ready
    // allocateWrite 只能在 memRsp 真正进入 st1 pipeReg 后发起。
    // 否则当前一笔 memRsp 还在处理时，队头上已经露出来的下一笔 memRsp 会提前覆盖 allocate 上下文。
    val memRspEnqFire = MemRsp_pipeReg_st0_st1.enq.fire
    tAAllocateWriteReq_valid := memRspEnqFire && memRspisRead && !io.MSHRMissRspOutUCached // cached read response
    // regular read 的 line data 可能要先于 st1 pipeReg 被 MSHR missRspOut 消费，
    // 因此只要 memRsp 到了队头，就先把整条 line 记下来，避免后面 metadata/data 错位。
    when(io.memRsp.valid && memRspisRead && !memRspisSpecial){
        missRspDataByInstrId(idx_st0) := io.memRsp.bits.d_data
    }

    missRspTI_st1 := Mux(memRsp_st1_isSpecial,
        io.SMSHRMissRspOut.bits.targetInfo.asTypeOf(new VecMshrTargetInfo),
        io.MSHRMissRspOut.bits.targetInfo.asTypeOf(new VecMshrTargetInfo))
    val missRspAsid_st1 = if(MMU_ENABLED) Some(UInt(asidLen.W)) else None
    if(MMU_ENABLED){
        missRspAsid_st1.get := Mux(memRsp_st1_isSpecial,
            io.SMSHRMissRspOutAsid.get,
            io.MSHRMissRspOutAsid.get)
    }
    // ---st1---
    val memRspMetaValid_st1 = Mux(memRsp_st1_isSpecial,
        io.SMSHRMissRspOut.valid,
        io.MSHRMissRspOut.valid)
    missRspEntryIdx_st1 := io.MSHRMissRspOut.bits.instrId(log2Up(NMshrEntry)-1,0)
    // 第一拍 missRspOut 有效时，当前 memRsp 的 d_data 可能正好就是这笔 miss 需要返回的 line。
    // 命中时直接旁路 live data；否则退回到按 instrId 暂存的数据，确保 metadata/data 属于同一笔事务。
    val liveMissRspDataMatch_st1 =
      io.memRsp.valid && memRspisRead && !memRspisSpecial && (idx_st0 === missRspEntryIdx_st1)
    val missRspData_st1 = Mux(liveMissRspDataMatch_st1, io.memRsp.bits.d_data, missRspDataByInstrId(missRspEntryIdx_st1))
    // coreRsp
    io.memRsp_coreRsp.valid := memRspMetaValid_st1
    io.memRsp_coreRsp.bits.Rsp.data := Mux(memRsp_st1_isSpecial, MemRsp_pipeReg_st0_st1.deq.bits.Rsp.d_data, missRspData_st1)
    io.memRsp_coreRsp.bits.Rsp.isWrite := false.B
    io.memRsp_coreRsp.bits.Rsp.instrId := missRspTI_st1.instrId
    io.memRsp_coreRsp.bits.Rsp.activeMask := missRspTI_st1.perLaneAddr.map(_.activeMask)
    io.memRsp_coreRsp.bits.perLaneAddr := missRspTI_st1.perLaneAddr
    io.memRsp_coreRsp.bits.validFromCoreReq := false.B
    io.memRsp_coreRsp.bits.readHitSnapshotValid := false.B
    io.memRsp_coreRsp.bits.readHitSnapshotData := DontCare
    // write to dA sram
    io.dAmemRsp_wReq.foreach(_.waymask.get := Fill(BytesOfWord, true.B))
    io.dAmemRsp_wReq.foreach(_.setIdx := Cat(MemRsp_pipeReg_st0_st1.deq.bits.Rsp.d_source(SetIdxBits-1,0),OHToUInt(io.tAWayMask)))
    for (i <- 0 until BlockWords) {
      io.dAmemRsp_wReq(i).data := MemRsp_pipeReg_st0_st1.deq.bits.Rsp.d_data(i).asTypeOf(Vec(BytesOfWord, UInt(8.W)))
    }
    val idle :: dAread :: memReq :: Nil = Enum(3)
    val tagRequestStatus = RegInit(idle)
    val tagRequestStatus_next = WireInit(tagRequestStatus)
    val st1_valid = MemRsp_pipeReg_st0_st1.deq.valid && MemRsp_pipeReg_st0_st1.deq.bits.isRead && MemRsp_pipeReg_st0_st1.deq.bits.isCached
    val needReplace_pulse = io.needReplace && st1_valid
    val needReplace_pending = RegInit(false.B)
    val needReplace_eff = needReplace_pending || needReplace_pulse
    // dirty replace 一旦开始，到本次 allocate 写回(tag/data)完成前，屏蔽新的 coreReq 进入。
    // 否则 victim line 仍可能被新的 hit 请求读/写，破坏 replace 期间采样到的旧数据。
    val blockCoreReqReg = RegInit(false.B)
    when(needReplace_pulse){
      blockCoreReqReg := true.B
    }.elsewhen(blockCoreReqReg && dAReq_valid){
      blockCoreReqReg := false.B
    }
    io.blockCoreReq := blockCoreReqReg || needReplace_pulse
    val allocateSetIdx_st1 = RegInit(0.U(SetIdxBits.W))
    when(io.tAAllocateWriteReq.valid){
      allocateSetIdx_st1 := io.tAAllocateWriteReq.bits.setIdx
    }
    val replaceSetIdx = RegInit(0.U(SetIdxBits.W))
    val replaceWayMask = RegInit(0.U(NWays.W))
    val replaceSetIdx_eff = Mux(needReplace_pending, replaceSetIdx, allocateSetIdx_st1)
    val replaceWayMask_eff = Mux(needReplace_pending, replaceWayMask, io.tAWayMask)
    st1_ready := io.memRsp_coreRsp.ready
    tagRequestStatus := tagRequestStatus_next
    tagRequestStatus_next := tagRequestStatus
    switch(tagRequestStatus){
        is(idle){
            when(needReplace_eff && st1_valid){
                tagRequestStatus_next := dAread
            }.elsewhen(st1_valid && !needReplace_eff){
                tagRequestStatus_next := tagRequestStatus
            }
            when(needReplace_eff){
                st1_ready := false.B
            }.otherwise{
                st1_ready := io.memRsp_coreRsp.ready
            }
        }
        is(dAread){
                tagRequestStatus_next := memReq
                st1_ready := false.B
        }
        is(memReq){
            when(needReplace_eff && st1_valid && io.memReq_ready){
                tagRequestStatus_next := dAread
                st1_ready := false.B
            }.elsewhen(io.memReq_ready){
                tagRequestStatus_next := idle
                st1_ready := io.memRsp_coreRsp.ready
            }.otherwise{
                st1_ready := false.B
            }
        }
    }
    io.MSHRMissRspOut.ready := io.memRsp_coreRsp.ready
    io.SMSHRMissRspOut.ready := io.memRsp_coreRsp.ready
    when(tagRequestStatus_next === dAread && st1_valid){
        needReplace_pending := false.B
    }.elsewhen(needReplace_pulse){
        needReplace_pending := true.B
    }
    when(needReplace_pulse && !needReplace_pending){
        replaceSetIdx := allocateSetIdx_st1
        replaceWayMask := io.tAWayMask
    }
    MemRsp_pipeReg_st0_st1.deq.ready := st1_ready
    io.dAmemRsp_wReq_valid := dAReq_valid
    io.dAmemRsp_wReq_blockAddr := get_blockAddr(MemRsp_pipeReg_st0_st1.deq.bits.Rsp.d_addr)
    // bfs4096-006 fix: 所有 BlockWords 共享同一 setIdx (= Cat(d_source.setIdx, OHToUInt(tAWayMask)))，
    // 取第 0 个 word 的 setIdx 即可代表本次 fill 的目标 dA row。
    io.dAmemRsp_wReq_setIdx := io.dAmemRsp_wReq(0).setIdx
    // bfs4096-006 fix: 不含 st1_ready 反馈的 intent 信号，给 coreReqPipe / L1RTAB 用以破组合环。
    io.dAmemRsp_wReq_intent := st1_valid
    if(MMU_ENABLED){
      io.dAmemRsp_wReq_asid.get := missRspAsid_st1.get
    }
    io.dAReplace_rReq.foreach(_.setIdx := Cat(replaceSetIdx_eff, OHToUInt(replaceWayMask_eff)))
    dAReq_valid := st1_valid && st1_ready
    io.dAReplace_rReq_valid := tagRequestStatus_next === dAread && st1_valid
    io.memReq_valid := tagRequestStatus === memReq && st1_valid
}

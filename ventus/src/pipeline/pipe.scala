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
package pipeline

import L1Cache.ICache._
import chisel3._
import chisel3.experimental.hierarchy.{Definition, Instance}
import chisel3.util._
import top.parameters._
import gvm._

class ICachePipeReq_np extends Bundle {
  private val sourceWidth = if (num_subcore <= 1) 1 else log2Ceil(num_subcore)
  val addr = UInt(32.W)
  val mask = UInt(num_fetch.W)
  val warpid = UInt(depth_warp.W)
  val source = UInt(sourceWidth.W)
  val wf_tag = UInt(TAG_WIDTH.W)
  val frontend_gen = UInt(frontend_gen_width.W)
  val asid = if(MMU_ENABLED) Some(UInt(KNL_ASID_WIDTH.W)) else None
}
class ICachePipeRsp_np extends Bundle{
  private val sourceWidth = if (num_subcore <= 1) 1 else log2Ceil(num_subcore)
  val addr = UInt(32.W)
  val data = UInt((num_fetch*32).W)
  val mask = UInt(num_fetch.W)
  val warpid = UInt(depth_warp.W)
  val source = UInt(sourceWidth.W)
  val wf_tag = UInt(TAG_WIDTH.W)
  val frontend_gen = UInt(frontend_gen_width.W)
  val status = UInt(2.W)
}

class pipe() extends Module{
  val sm_id = IO(Input(UInt(8.W)))
  val io = IO(new Bundle{
    val icache_req = (DecoupledIO(new ICachePipeReq_np))
    val icache_rsp = Flipped(DecoupledIO(new ICachePipeRsp_np))
    val externalFlushPipe = ValidIO(UInt(depth_warp.W))
    val dcache_req = DecoupledIO(new DCacheCoreReq_np)
    val dcache_rsp = Flipped(DecoupledIO(new DCacheCoreRsp_np))
    val shared_req = DecoupledIO(new ShareMemCoreReq_np)
    val shared_rsp = Flipped(DecoupledIO(new DCacheCoreRsp_np))
    val pc_reset = Input(Bool())
    val warpReq=Flipped(Decoupled(new warpReqData))
    val warpRsp=(Decoupled(new warpRspData))
    val wg_id_lookup=Output(UInt(depth_warp.W))
    val wg_id_tag=Input(UInt(TAG_WIDTH.W))
    val inst = if (SINGLE_INST) Some(Flipped(DecoupledIO(UInt(32.W)))) else None
    val inst_cnt = if(INST_CNT) Some(Output(UInt(32.W))) else if(INST_CNT_2) Some(Output(Vec(2, UInt(32.W)))) else None
    val inst_cnt2 = if(INST_CNT_2) Some(Output(Vec(2, UInt(32.W)))) else None
    val perfEnable = Input(Bool())
    val perfReset = Input(Bool())
    val perf_pipeline = if(PMU_PIPELINE) Some(Output(new PipelinePerfCounters)) else None
    val perf_inst_class = if(PMU_INST_CLASS) Some(Output(new InstClassPerfCounters)) else None
  })
  val issue_stall=Wire(Bool())
  val flush=Wire(Bool())


  val warpSchedulerDefinition =
    Definition(new warp_scheduler(num_warp_per_subcore))
  val warpSchedulers = Seq.fill(num_subcore)(Instance(warpSchedulerDefinition))
  //val pcfifo=Module(new PCfifo)
  val control=Module(new InstrDecodeV2)
  control.io.sm_id := sm_id
  val gvmOperandCollector =
    if (GVM_ENABLED) Some(Module(new operandCollector)) else None
  if (GVM_ENABLED) {
    val operandCollector = gvmOperandCollector.get
    val gvm_xreg_init = Module(new GvmDutWarpXRegInit)
    val gvm_vreg_init = Module(new GvmDutWarpVRegInit)
    gvm_xreg_init.io.clock := clock
    gvm_xreg_init.io.reset := reset.asBool
    gvm_xreg_init.io.fire := io.warpReq.fire
    gvm_xreg_init.io.sm_id := sm_id
    gvm_xreg_init.io.hardware_warp_id := io.warpReq.bits.wid.pad(32)
    gvm_xreg_init.io.xregs := operandCollector.io.gvmWarpXRegs.get.asUInt
    gvm_vreg_init.io.clock := clock
    gvm_vreg_init.io.reset := reset.asBool
    gvm_vreg_init.io.fire := io.warpReq.fire
    gvm_vreg_init.io.sm_id := sm_id
    gvm_vreg_init.io.hardware_warp_id := io.warpReq.bits.wid.pad(32)
    gvm_vreg_init.io.vregs := operandCollector.io.gvmWarpVRegs.get.asUInt

    operandCollector.io.gvmWarpHwId.get := io.warpReq.bits.wid
    operandCollector.io.gvmWarpSgprBase.get := io.warpReq.bits.CTAdata.dispatch2cu_sgpr_base_dispatch
    operandCollector.io.gvmWarpVgprBase.get := io.warpReq.bits.CTAdata.dispatch2cu_vgpr_base_dispatch
    operandCollector.io.controlX.valid := false.B
    operandCollector.io.controlX.bits := 0.U.asTypeOf(new CtrlSigs)
    operandCollector.io.controlV.valid := false.B
    operandCollector.io.controlV.bits := 0.U.asTypeOf(new CtrlSigs)
    operandCollector.io.writeVecCtrl.valid := false.B
    operandCollector.io.writeVecCtrl.bits := 0.U.asTypeOf(new WriteVecCtrl)
    operandCollector.io.writeScalarCtrl.valid := false.B
    operandCollector.io.writeScalarCtrl.bits := 0.U.asTypeOf(new WriteScalarCtrl)
    operandCollector.io.sgpr_base := VecInit(Seq.fill(num_warp)(0.U((SGPR_ID_WIDTH + 1).W)))
    operandCollector.io.vgpr_base := VecInit(Seq.fill(num_warp)(0.U((VGPR_ID_WIDTH + 1).W)))
    operandCollector.io.out.foreach(_.ready := true.B)
  }
  val lsu2wb=Module(new LSU2WB)

  val inst_cnt_xv = RegInit(VecInit(0.U(32.W), 0.U(32.W)))
  val localScoreboards = Seq.fill(num_subcore)(Module(new SubcoreScoreboardBank(num_warp_per_subcore)))
  val ibuffers = Seq.tabulate(num_subcore)(sc => Module(new InstrBufferV2(num_warp_per_subcore, sc)))
  val ibufferRspOwner = PipeSubcoreHelpers.subcoreValue(io.icache_rsp.bits.warpid)
  val ibufferInReady = MuxLookup(ibufferRspOwner, false.B)(
    (0 until num_subcore).map(sc => sc.U -> ibuffers(sc).io.in.ready)
  )
  val ibufferReadyGlobal = VecInit((0 until num_warp).map { globalWid =>
    val sc = globalWid & (num_subcore - 1)
    val localWid = globalWid >> subcore_sel_bits
    ibuffers(sc).io.ibuffer_ready(localWid)
  })
  val subcoreIbuffer2issue = Seq.tabulate(num_subcore)(_ =>
    Module(new ibuffer2issue(num_warp_per_subcore))
  )
  if(INST_CNT) {
    io.inst_cnt.foreach(_ := subcoreIbuffer2issue.map(_.io.cnt.getOrElse(0.U)).reduce(_ +& _))
  }
  if(INST_CNT_2){
    io.inst_cnt2.foreach( _ := inst_cnt_xv)
  }
  else{
    io.inst_cnt.foreach( _ := 0.U)
  }
  val frontendCtrlScaffold = Module(new FrontendCtrl)
  val lifecycleCtrlScaffold = Module(new WarpLifecycleCtrl)
  val localWritebacks = Seq.tabulate(num_subcore)(sc => Module(new SubcoreWritebackFabric(sc)))
  private val activeSubcoreCount = num_subcore
  // Full backend array. Non-memory execution and final writeback stay local;
  // memory resources and cross-subcore control arbitration remain SM-shared.
  val subcoreBackends = Seq.fill(activeSubcoreCount)(Module(new SubcoreBackendSlice))
  private val subcoreIdxWidth = if (num_subcore <= 1) 1 else log2Ceil(num_subcore)
  private val localDepthWarp = if (num_warp_per_subcore == 1) 1 else log2Ceil(num_warp_per_subcore)
  private def globalWarpIndex(sc: Int, local: Int): Int =
    PipeSubcoreHelpers.globalWarpIndex(sc, local)
  private def localizeCtrl(in: CtrlSigs): CtrlSigs = {
    val out = Wire(new CtrlSigs)
    out := in
    out.wid := PipeSubcoreHelpers.localWidValue(in.wid)
    out
  }
  private def localizeWarpReq(in: warpReqData): warpReqData = {
    val out = Wire(new warpReqData)
    out := in
    out.wid := PipeSubcoreHelpers.localWidValue(in.wid)
    out
  }
  private def localizeBranch(in: BranchCtrl): BranchCtrl = {
    val out = Wire(new BranchCtrl)
    out := in
    out.wid := PipeSubcoreHelpers.localWidValue(in.wid)
    out
  }
  private def localizeWarpLifecycle(in: warpSchedulerExeData): warpSchedulerExeData = {
    val out = Wire(new warpSchedulerExeData)
    out := in
    out.ctrl.wid := PipeSubcoreHelpers.localWidValue(in.ctrl.wid)
    out
  }
  private def localizeICacheRsp(in: ICachePipeRsp_np): ICachePipeRsp_np = {
    val out = Wire(new ICachePipeRsp_np)
    out := in
    out.warpid := PipeSubcoreHelpers.localWidValue(in.warpid)
    out
  }
  private def restoreWarpRsp(sc: Int, in: warpRspData): warpRspData = {
    val out = Wire(new warpRspData)
    out := in
    out.wid := PipeSubcoreHelpers.internalWid(sc, in.wid)
    out
  }
  private def restoreICacheReq(sc: Int, in: ICachePipeReq_np): ICachePipeReq_np = {
    val out = Wire(new ICachePipeReq_np)
    out := in
    out.warpid := PipeSubcoreHelpers.internalWid(sc, in.warpid)
    out.source := sc.U
    out
  }
  private def localizeWriteScalar(in: WriteScalarCtrl): WriteScalarCtrl = {
    val out = Wire(new WriteScalarCtrl)
    out := in
    out.warp_id := PipeSubcoreHelpers.localWidValue(in.warp_id)
    out
  }
  private def localizeWriteVec(in: WriteVecCtrl): WriteVecCtrl = {
    val out = Wire(new WriteVecCtrl)
    out := in
    out.warp_id := PipeSubcoreHelpers.localWidValue(in.warp_id)
    out
  }
  private def restoreBranch(sc: Int, in: BranchCtrl): BranchCtrl = {
    val out = Wire(new BranchCtrl)
    out := in
    out.wid := PipeSubcoreHelpers.internalWid(sc, in.wid)
    out
  }
  private def restoreWarpLifecycle(sc: Int, in: warpSchedulerExeData): warpSchedulerExeData = {
    val out = Wire(new warpSchedulerExeData)
    out := in
    out.ctrl.wid := PipeSubcoreHelpers.internalWid(sc, in.ctrl.wid)
    out
  }
  private val lsuLocalInstrIdWidth = log2Up(lsu_nMshrEntry)
  private val lsuFabricSourceWidth = subcoreIdxWidth

  val acceptedBranchWid = Wire(UInt(depth_warp.W))
  val acceptedBranchRedirect = Wire(Bool())
  val acceptedBarrierWid = Wire(UInt(depth_warp.W))
  val acceptedBarrier = Wire(Bool())
  val acceptedEndprgWid = Wire(UInt(depth_warp.W))
  val acceptedEndprg = Wire(Bool())
  acceptedBranchWid := 0.U
  acceptedBranchRedirect := false.B
  acceptedBarrierWid := 0.U
  acceptedBarrier := false.B
  acceptedEndprgWid := 0.U
  acceptedEndprg := false.B
  for (sc <- 0 until num_subcore) {
    frontendCtrlScaffold.io.flush(sc).valid := false.B
    frontendCtrlScaffold.io.flush(sc).bits := 0.U
    frontendCtrlScaffold.io.flush_cache(sc).valid := false.B
    frontendCtrlScaffold.io.flush_cache(sc).bits := 0.U

    lifecycleCtrlScaffold.io.branch_jump(sc).valid :=
      acceptedBranchRedirect && PipeSubcoreHelpers.subcoreValue(acceptedBranchWid) === sc.U
    lifecycleCtrlScaffold.io.branch_jump(sc).bits := acceptedBranchWid
    lifecycleCtrlScaffold.io.barrier_arrive(sc).valid :=
      acceptedBarrier && PipeSubcoreHelpers.subcoreValue(acceptedBarrierWid) === sc.U
    lifecycleCtrlScaffold.io.barrier_arrive(sc).bits := acceptedBarrierWid
    lifecycleCtrlScaffold.io.endprg(sc).valid :=
      acceptedEndprg && PipeSubcoreHelpers.subcoreValue(acceptedEndprgWid) === sc.U
    lifecycleCtrlScaffold.io.endprg(sc).bits := acceptedEndprgWid

  }
  for (sc <- 0 until activeSubcoreCount) {
    subcoreBackends(sc).io.subcore_id := sc.U
    subcoreBackends(sc).io.frontend_control_v.valid := false.B
    subcoreBackends(sc).io.frontend_control_v.bits := 0.U.asTypeOf(new CtrlSigs)
    subcoreBackends(sc).io.frontend_control_x.valid := false.B
    subcoreBackends(sc).io.frontend_control_x.bits := 0.U.asTypeOf(new CtrlSigs)
    subcoreBackends(sc).io.branch_out.ready := false.B
    subcoreBackends(sc).io.warp_lifecycle.ready := false.B
    subcoreBackends(sc).io.lsu_dcache_req.ready := false.B
    subcoreBackends(sc).io.lsu_dcache_rsp.valid := false.B
    subcoreBackends(sc).io.lsu_dcache_rsp.bits := 0.U.asTypeOf(new DCacheCoreRsp_np)
    subcoreBackends(sc).io.lsu_shared_req.ready := false.B
    subcoreBackends(sc).io.lsu_shared_rsp.valid := false.B
    subcoreBackends(sc).io.lsu_shared_rsp.bits := 0.U.asTypeOf(new DCacheCoreRsp_np)
    subcoreBackends(sc).io.lsu_result_rsp.ready := false.B
    subcoreBackends(sc).io.flush_dcache.valid := false.B
    subcoreBackends(sc).io.flush_dcache.bits := false.B
    subcoreBackends(sc).io.compute_valu_vec.ready := false.B
    subcoreBackends(sc).io.compute_mul_scalar.ready := false.B
    subcoreBackends(sc).io.compute_mul_vec.ready := false.B
    subcoreBackends(sc).io.warp_init.valid := false.B
    subcoreBackends(sc).io.warp_init.bits := 0.U.asTypeOf(new warpReqData)
  }
  frontendCtrlScaffold.io.launch_flush.valid := io.warpReq.fire
  frontendCtrlScaffold.io.launch_flush.bits := io.warpReq.bits.wid
  frontendCtrlScaffold.io.ibuffer_ready_for_rsp := ibufferInReady
  val frontendFlushGlobalValid = Wire(Vec(activeSubcoreCount, Bool()))
  val frontendFlushGlobalBits = Wire(Vec(activeSubcoreCount, UInt(depth_warp.W)))
  val frontendFlushCacheGlobalValid = Wire(Vec(activeSubcoreCount, Bool()))
  val frontendFlushCacheGlobalBits = Wire(Vec(activeSubcoreCount, UInt(depth_warp.W)))
  for (sc <- 0 until activeSubcoreCount) {
    frontendFlushGlobalValid(sc) := false.B
    frontendFlushGlobalBits(sc) := 0.U
    frontendFlushCacheGlobalValid(sc) := false.B
    frontendFlushCacheGlobalBits(sc) := 0.U
  }
  val anyFrontendFlushGlobal = frontendFlushGlobalValid.asUInt.orR
  val anyFrontendFlushCacheGlobal = frontendFlushCacheGlobalValid.asUInt.orR
  val frontendFlushGlobalWid = PriorityMux(
    (0 until activeSubcoreCount).map(sc => frontendFlushGlobalValid(sc) -> frontendFlushGlobalBits(sc))
  )
  val frontendFlushCacheGlobalWid = PriorityMux(
    (0 until activeSubcoreCount).map(sc => frontendFlushCacheGlobalValid(sc) -> frontendFlushCacheGlobalBits(sc))
  )
  val frontendRspKillFlushValid = RegNext(anyFrontendFlushGlobal, false.B)
  val frontendRspKillFlushBits = RegNext(frontendFlushGlobalWid, 0.U(depth_warp.W))
  frontendCtrlScaffold.io.rsp_kill.valid := frontendRspKillFlushValid
  frontendCtrlScaffold.io.rsp_kill.bits := frontendRspKillFlushBits
  frontendCtrlScaffold.io.wg_tag_lookup := lifecycleCtrlScaffold.io.wg_tag_lookup
  frontendCtrlScaffold.io.frontend_gen_lookup := lifecycleCtrlScaffold.io.frontend_gen_lookup
  frontendCtrlScaffold.io.recently_freed := lifecycleCtrlScaffold.io.recently_freed

  lifecycleCtrlScaffold.io.launch.valid := io.warpReq.fire
  lifecycleCtrlScaffold.io.launch.bits.wid := io.warpReq.bits.wid
  lifecycleCtrlScaffold.io.launch.bits.wf_tag := io.warpReq.bits.CTAdata.dispatch2cu_wf_tag_dispatch
  lifecycleCtrlScaffold.io.launch.bits.wg_wf_count := io.warpReq.bits.CTAdata.dispatch2cu_wg_wf_count
  lifecycleCtrlScaffold.io.complete.valid := io.warpRsp.fire
  lifecycleCtrlScaffold.io.complete.bits := io.warpRsp.bits.wid

  for (sc <- 0 until activeSubcoreCount) {
    val warpInit = subcoreBackends(sc).io.warp_init
    warpInit.valid := warpSchedulers(sc).io.CTA2csr.valid
    warpInit.bits := warpSchedulers(sc).io.CTA2csr.bits
  }

  val resultFabric = Module(new PipeExecutionResultFabric(activeSubcoreCount))

  for (sc <- 0 until activeSubcoreCount) {
    resultFabric.io.lifecycleIn(sc).valid := subcoreBackends(sc).io.warp_lifecycle.valid
    resultFabric.io.lifecycleIn(sc).bits := restoreWarpLifecycle(sc, subcoreBackends(sc).io.warp_lifecycle.bits)
    subcoreBackends(sc).io.warp_lifecycle.ready := resultFabric.io.lifecycleIn(sc).ready

    resultFabric.io.aluBranchIn(sc).valid := subcoreBackends(sc).io.branch_out.valid
    resultFabric.io.aluBranchIn(sc).bits := restoreBranch(sc, subcoreBackends(sc).io.branch_out.bits)
    subcoreBackends(sc).io.branch_out.ready := resultFabric.io.aluBranchIn(sc).ready

    val wb = localWritebacks(sc)
    wb.io.alu_scalar <> subcoreBackends(sc).io.compute_alu_scalar
    wb.io.fpu_scalar <> subcoreBackends(sc).io.compute_fpu_scalar
    wb.io.csr_scalar <> subcoreBackends(sc).io.csr_scalar
    wb.io.sfu_scalar <> subcoreBackends(sc).io.compute_sfu_scalar
    wb.io.mul_scalar <> subcoreBackends(sc).io.compute_mul_scalar
    wb.io.valu_vec <> subcoreBackends(sc).io.compute_valu_vec
    wb.io.fpu_vec <> subcoreBackends(sc).io.compute_fpu_vec
    wb.io.sfu_vec <> subcoreBackends(sc).io.compute_sfu_vec
    wb.io.mul_vec <> subcoreBackends(sc).io.compute_mul_vec
    wb.io.tc_vec <> subcoreBackends(sc).io.compute_tc_vec
    wb.io.csr_vec <> subcoreBackends(sc).io.csr_vec
    subcoreBackends(sc).io.write_scalar <> wb.io.write_scalar
    subcoreBackends(sc).io.write_vec <> wb.io.write_vec
  }

  val sharedLsuFenceEnd = VecInit((0 until num_warp).map { globalWid =>
    val sc = globalWid & (num_subcore - 1)
    val localWid = globalWid >> subcore_sel_bits
    subcoreBackends(sc).io.lsu_fence_end(localWid)
  }).asUInt

  io.externalFlushPipe.valid:=anyFrontendFlushGlobal|anyFrontendFlushCacheGlobal
  io.externalFlushPipe.bits:=Mux(anyFrontendFlushGlobal,frontendFlushGlobalWid,frontendFlushCacheGlobalWid)

  io.icache_req<>frontendCtrlScaffold.io.icache_req
  frontendCtrlScaffold.io.icache_rsp<>io.icache_rsp
  val scoreboardBusy = VecInit((0 until num_warp).map { globalWid =>
    val sc = globalWid & (num_subcore - 1)
    val localWid = globalWid >> subcore_sel_bits
    localScoreboards(sc).io.warp(localWid).delay
  }).asUInt
  val warpReqOwner = PipeSubcoreHelpers.subcoreValue(io.warpReq.bits.wid)
  val localWarpReq = localizeWarpReq(io.warpReq.bits)
  val branchOwner = PipeSubcoreHelpers.subcoreValue(resultFabric.io.aluBranchOut.bits.wid)
  val lifecycleOwner = PipeSubcoreHelpers.subcoreValue(resultFabric.io.lifecycleOut.bits.ctrl.wid)
  val lifecycleIsEndprg = resultFabric.io.lifecycleOut.bits.ctrl.simt_stack_op
  val warpRspArb = Module(new RRArbiter(new warpRspData, activeSubcoreCount))

  for (sc <- 0 until activeSubcoreCount) {
    val sched = warpSchedulers(sc)
    sched.io.pc_reset := io.pc_reset
    sched.io.warpReq.valid := io.warpReq.valid && warpReqOwner === sc.U
    sched.io.warpReq.bits := localWarpReq
    sched.io.branch.valid := resultFabric.io.aluBranchOut.valid && branchOwner === sc.U
    sched.io.branch.bits := localizeBranch(resultFabric.io.aluBranchOut.bits)
    sched.io.warp_control.valid := resultFabric.io.lifecycleOut.valid &&
      lifecycleIsEndprg && lifecycleOwner === sc.U
    sched.io.warp_control.bits := localizeWarpLifecycle(resultFabric.io.lifecycleOut.bits)
    sched.io.pc_ibuffer_ready := VecInit((0 until num_warp_per_subcore).map { local =>
      ibuffers(sc).io.ibuffer_ready(local).asUInt
    })
    sched.io.scoreboard_busy := VecInit((0 until num_warp_per_subcore).map { local =>
      localScoreboards(sc).io.warp(local).delay ||
        lifecycleCtrlScaffold.io.global_barrier_wait(globalWarpIndex(sc, local))
    }).asUInt
    sched.io.exe_busy := 0.U
    sched.io.issued_warp.valid := false.B
    sched.io.issued_warp.bits := 0.U
    sched.io.pc_rsp.valid := frontendCtrlScaffold.io.pc_rsp(sc).valid
    sched.io.pc_rsp.bits := localizeICacheRsp(frontendCtrlScaffold.io.pc_rsp(sc).bits)
    sched.io.pc_rsp.bits.status := Mux(
      ibuffers(sc).io.in.ready,
      frontendCtrlScaffold.io.pc_rsp(sc).bits.status,
      1.U(2.W)
    )
    val restoredPcReq = restoreICacheReq(sc, sched.io.pc_req.bits)
    frontendCtrlScaffold.io.pc_req(sc).valid := sched.io.pc_req.valid
    frontendCtrlScaffold.io.pc_req(sc).bits := restoredPcReq
    sched.io.pc_req.ready := frontendCtrlScaffold.io.pc_req(sc).ready
    frontendCtrlScaffold.io.flush(sc) := sched.io.flush
    frontendCtrlScaffold.io.flush_cache(sc) := sched.io.flushCache
    frontendFlushGlobalValid(sc) := sched.io.flush.valid
    frontendFlushGlobalBits(sc) := PipeSubcoreHelpers.internalWid(sc, sched.io.flush.bits)
    frontendFlushCacheGlobalValid(sc) := sched.io.flushCache.valid
    frontendFlushCacheGlobalBits(sc) := PipeSubcoreHelpers.internalWid(sc, sched.io.flushCache.bits)
    sched.io.wg_id_tag := lifecycleCtrlScaffold.io.wg_tag_lookup(
      PipeSubcoreHelpers.internalWid(sc, sched.io.wg_id_lookup)
    )
    sched.io.flushDCache.ready := false.B

    warpRspArb.io.in(sc).valid := sched.io.warpRsp.valid
    warpRspArb.io.in(sc).bits := restoreWarpRsp(sc, sched.io.warpRsp.bits)
    sched.io.warpRsp.ready := warpRspArb.io.in(sc).ready
  }
  io.warpReq.ready := MuxLookup(warpReqOwner, false.B)(
    (0 until activeSubcoreCount).map(sc => sc.U -> warpSchedulers(sc).io.warpReq.ready)
  )
  resultFabric.io.aluBranchOut.ready := MuxLookup(branchOwner, false.B)(
    (0 until activeSubcoreCount).map(sc => sc.U -> warpSchedulers(sc).io.branch.ready)
  )
  resultFabric.io.lifecycleOut.ready := Mux(
    lifecycleIsEndprg,
    MuxLookup(lifecycleOwner, false.B)(
      (0 until activeSubcoreCount).map(sc => sc.U -> warpSchedulers(sc).io.warp_control.ready)
    ),
    true.B
  )
  io.warpRsp <> warpRspArb.io.out

  acceptedBranchWid := resultFabric.io.aluBranchOut.bits.wid
  acceptedBranchRedirect := resultFabric.io.aluBranchOut.fire && resultFabric.io.aluBranchOut.bits.jump
  acceptedBarrierWid := resultFabric.io.lifecycleOut.bits.ctrl.wid
  acceptedBarrier := resultFabric.io.lifecycleOut.fire && !lifecycleIsEndprg
  acceptedEndprgWid := resultFabric.io.lifecycleOut.bits.ctrl.wid
  acceptedEndprg := resultFabric.io.lifecycleOut.fire && lifecycleIsEndprg
  io.wg_id_lookup := lifecycleCtrlScaffold.io.wg_id_lookup

  val init_thread_mask = (1.U(num_thread.W) << io.warpReq.bits.CTAdata.dispatch2cu_wf_size_dispatch).asUInt - 1.U
  when(io.warpReq.fire){
    printf(p"sm ${sm_id} warp ${Decimal(io.warpReq.bits.wid)} init thread mask 0x${Hexadecimal(init_thread_mask)}\n")
  }
  val activeSubcoreFlushReady = VecInit((0 until activeSubcoreCount).map { sc =>
    subcoreBackends(sc).io.flush_dcache.ready
  }).asUInt.andR
  for (sc <- 0 until activeSubcoreCount) {
    subcoreBackends(sc).io.flush_dcache.valid :=
      lifecycleCtrlScaffold.io.flush_dcache.valid && activeSubcoreFlushReady
    subcoreBackends(sc).io.flush_dcache.bits := lifecycleCtrlScaffold.io.flush_dcache.bits
  }
  lifecycleCtrlScaffold.io.flush_dcache.ready := activeSubcoreFlushReady

  //flush:=(warp_sche.io.branch.fire&warp_sche.io.branch.bits.jump) | ()
  flush:=anyFrontendFlushGlobal

  control.io.sm_id := sm_id
  control.io.pc:=io.icache_rsp.bits.addr
  control.io.inst.zipWithIndex.foreach{ case (ins, i) =>
    ins := (io.icache_rsp.bits.data >> (xLen * i))(xLen - 1, 0)
  }
  control.io.wid:=io.icache_rsp.bits.warpid
  control.io.inst_mask:=Mux(
    frontendCtrlScaffold.io.accept_icache_rsp & !io.icache_rsp.bits.status(0),
    io.icache_rsp.bits.mask.asTypeOf(control.io.inst_mask),
    0.U.asTypeOf(control.io.inst_mask)
  )
  val frontendFlushForDecode = frontendCtrlScaffold.io.frontend_flush_mask
  control.io.flush_wid := frontendFlushForDecode
  control.io.ibuffer_ready := ibufferReadyGlobal
  for (sc <- 0 until activeSubcoreCount) {
    ibuffers(sc).io.in.bits.control := VecInit(control.io.control.map(localizeCtrl))
    ibuffers(sc).io.in.bits.control_mask := control.io.control_mask
    ibuffers(sc).io.in.valid := frontendCtrlScaffold.io.accept_icache_rsp &&
      !io.icache_rsp.bits.status(0) && ibufferRspOwner === sc.U
  }
  if(MMU_ENABLED) {
    for (sc <- 0 until activeSubcoreCount) {
      for (i <- 0 until num_fetch) {
        ibuffers(sc).io.in.bits.control(i).asid.get := warpSchedulers(sc).io.asid.get
      }
    }
  }
  for (sc <- 0 until activeSubcoreCount) {
    ibuffers(sc).io.flush_wid := VecInit((0 until num_warp_per_subcore).map { localWid =>
      frontendFlushForDecode(globalWarpIndex(sc, localWid))
    }).asUInt
  }

  // 进入ibuffer的指令不一定会被实际发射执行，这里不应检测undefined instruction
  // 例如，指令段之后的一些无用数据也可能进入ibuffer，但实际上因跳转/endprg而不会发射
  // (control.io.control zip control.io.control_mask).foreach{ case (ctrl, mask) =>
  //   when(ctrl.alu_fn === 63.U & ibuffer.io.in.valid & mask) {
  //     printf(p"sm ${sm_id} warp ${Decimal(ctrl.wid)} ")
  //     printf(p"undefined @ 0x${Hexadecimal(ctrl.pc)}: 0x${Hexadecimal(ctrl.inst)}\n")
  //   }
  //   assert (!(ctrl.alu_fn === 63.U & ibuffer.io.in.valid & mask), s"undefined instruction")
  // }



  if(SINGLE_INST){
    control.io.inst(0) := VecInit(io.inst.get.bits, 0.U(xLen.W))
    control.io.inst_mask := VecInit(1.U(1.W), 0.U(1.W))
    control.io.wid:=0.U
    io.inst.map(_.ready := ibuffers(0).io.in.ready)
    for (sc <- 0 until activeSubcoreCount) {
      ibuffers(sc).io.in.valid := Mux(sc.U === 0.U, io.inst.get.valid, false.B)
    }
    when(io.inst.get.fire) {printf(p"${Hexadecimal(control.io.inst(0))}\n")}
  }

  //val ibuffer_ready=Wire(Vec(num_warp,Bool()))
  val branchScoreboardClearMask = Mux(
    resultFabric.io.aluBranchOut.fire,
    UIntToOH(resultFabric.io.aluBranchOut.bits.wid, num_warp),
    0.U(num_warp.W)
  )
  val warpControlScoreboardClearMask = Mux(
    resultFabric.io.lifecycleOut.fire,
    UIntToOH(resultFabric.io.lifecycleOut.bits.ctrl.wid, num_warp),
    0.U(num_warp.W)
  )
  for (sc <- 0 until activeSubcoreCount) {
    val backendEvents = subcoreBackends(sc).io.scoreboard
    val localIbuf = subcoreIbuffer2issue(sc)
    val localWriteScalar = localWritebacks(sc).io.write_scalar
    val localWriteVec = localWritebacks(sc).io.write_vec
    for (localWid <- 0 until num_warp_per_subcore) {
      val globalWid = globalWarpIndex(sc, localWid)
      val scoreboard = localScoreboards(sc).io.warp(localWid)
      val issueXFire = localIbuf.io.out_x.fire && localIbuf.io.out_x.bits.wid === localWid.U
      val issueVFire = localIbuf.io.out_v.fire && localIbuf.io.out_v.bits.wid === localWid.U

      scoreboard.ibuffer_if_ctrl := ibuffers(sc).io.out(localWid).bits
      scoreboard.if_ctrl := Mux(issueXFire, localIbuf.io.out_x.bits, localIbuf.io.out_v.bits)
      scoreboard.wb_v_ctrl := localWriteVec.bits
      scoreboard.wb_x_ctrl := localWriteScalar.bits
      scoreboard.fence_end := sharedLsuFenceEnd(globalWid)
      scoreboard.if_fire := issueXFire || issueVFire
      scoreboard.wb_v_fire := localWriteVec.fire && localWriteVec.bits.warp_id === localWid.U
      scoreboard.wb_x_fire := localWriteScalar.fire && localWriteScalar.bits.warp_id === localWid.U
      scoreboard.br_ctrl := branchScoreboardClearMask(globalWid) ||
        warpControlScoreboardClearMask(globalWid) ||
        (backendEvents.simt_complete_fire && backendEvents.simt_complete_wid === localWid.U)
      scoreboard.op_colX_in_fire :=
        backendEvents.op_col_x_in_fire && backendEvents.op_col_x_in_wid === localWid.U
      scoreboard.op_colX_out_fire :=
        backendEvents.op_col_x_out_fire && backendEvents.op_col_x_out_wid === localWid.U
      scoreboard.op_colV_in_fire :=
        backendEvents.op_col_v_in_fire && backendEvents.op_col_v_in_wid === localWid.U
      scoreboard.op_colV_out_fire :=
        backendEvents.op_col_v_out_fire && backendEvents.op_col_v_out_wid === localWid.U
    }
  }

  // Per-subcore backend main path.
  for (sc <- 0 until activeSubcoreCount) {
    val localIbuf = subcoreIbuffer2issue(sc)
    val backend = subcoreBackends(sc)
    for (localWid <- 0 until num_warp_per_subcore) {
      val globalWid = globalWarpIndex(sc, localWid)
      localIbuf.io.in(localWid).bits := ibuffers(sc).io.out(localWid).bits
      localIbuf.io.in(localWid).valid :=
        ibuffers(sc).io.out(localWid).valid && warpSchedulers(sc).io.warp_ready(localWid)
      ibuffers(sc).io.out(localWid).ready :=
        localIbuf.io.in(localWid).ready && warpSchedulers(sc).io.warp_ready(localWid)
    }
    backend.io.frontend_control_x <> localIbuf.io.out_x
    backend.io.frontend_control_v <> localIbuf.io.out_v
    Seq(localIbuf.io.out_x, localIbuf.io.out_v).foreach { issued =>
      when(issued.fire) {
        assert(issued.bits.alu_fn =/= 63.U,
          p"UNDEFINED INSTRUCTION @ SM ${sm_id} warp ${Decimal(issued.bits.wid)} " +
          p"PC 0x${Hexadecimal(issued.bits.pc)}: 0x${Hexadecimal(issued.bits.inst)}")
      }
    }
  }

  when(io.icache_req.fire&(io.icache_req.bits.warpid===2.U)){
    //printf(p"wid=${io.icache_req.bits.warpid},pc=0x${Hexadecimal(io.icache_req.bits.addr)}\n")
  }
  when(io.icache_rsp.fire&(io.icache_rsp.bits.warpid===2.U)){
    //printf(p"wid=${io.icache_rsp.bits.warpid},pc=0x${Hexadecimal(io.icache_rsp.bits.addr)},inst=0x${Hexadecimal(io.icache_rsp.bits.data)}\n")
  }


  //输出所有write mem的操作
  //val wid_to_check = 2.U //exe_data.io.deq.bits.ctrl.wid===wid_to_check&
  //  when( exe_data.io.deq.fire&exe_data.io.deq.bits.ctrl.mem_cmd===2.U){
  //    when(exe_data.io.deq.bits.ctrl.isvec){
  //      printf(p"warp${exe_data.io.deq.bits.ctrl.wid} 0x${Hexadecimal(exe_data.io.deq.bits.ctrl.pc)} 0x${Hexadecimal(exe_data.io.deq.bits.ctrl.inst)} w v${exe_data.io.deq.bits.ctrl.reg_idx3} ")
  //      exe_data.io.deq.bits.in3.reverse.foreach(x => printf(p"${Hexadecimal(x.asUInt)} "))
  //      printf(p"mask ${Binary(exe_data.io.deq.bits.mask.asUInt)} @")
  //      (exe_data.io.deq.bits.in1 zip exe_data.io.deq.bits.in2).reverse.foreach(x => printf(p" ${Hexadecimal(x._1)}+${Hexadecimal(x._2)}"))
  //      printf("\n")
  //    }.otherwise{
  //      printf(p"warp${exe_data.io.deq.bits.ctrl.wid} 0x${Hexadecimal(exe_data.io.deq.bits.ctrl.pc)} 0x${Hexadecimal(exe_data.io.deq.bits.ctrl.inst)} w x${exe_data.io.deq.bits.ctrl.reg_idxw} ")
  //      printf(p"${Hexadecimal(exe_data.io.deq.bits.in3(0))} ")
  //      printf(p"@ ${Hexadecimal(exe_data.io.deq.bits.in1(0))}+${Hexadecimal(exe_data.io.deq.bits.in2(0))}\n")
  //    }
  //  }
  //输出所有发射的指令
  //when( exe_data.io.deq.fire){
  //  printf(p"${exe_data.io.deq.bits.ctrl.wid},0x${Hexadecimal(exe_data.io.deq.bits.ctrl.pc)},writedata=")
  //  //exe_data.io.deq.bits.in3.foreach(x=>{printf(p"${Hexadecimal(x.asUInt)} ")})
  //  printf(p"mask ${exe_data.io.deq.bits.mask} with${Hexadecimal(exe_data.io.deq.bits.in1(0))},${Hexadecimal(exe_data.io.deq.bits.in2(0))}")
  //  printf(p"\n")
  //}

  //输出特定指令的操作数
  //when((exe_data.io.deq.bits.ctrl.wid===wid_to_check)& exe_data.io.deq.fire ){
  //    printf(p"0x${Hexadecimal(exe_data.io.deq.bits.ctrl.pc+0x348.U-0xc.U) },${exe_data.io.deq.bits.ctrl.wid} operand is =")
  //  exe_data.io.deq.bits.in2.foreach(x=>{printf(p"${Hexadecimal(x.asUInt)} ")})
  //  exe_data.io.deq.bits.in1.foreach(x=>{printf(p"${Hexadecimal(x.asUInt)} ")})
  //  exe_data.io.deq.bits.in3.foreach(x=>{printf(p"${Hexadecimal(x.asUInt)} ")})
  //  exe_data.io.deq.bits.mask.foreach(x=>{printf(p"${Hexadecimal(x.asUInt)} ")})
  //  printf(p"\n")
  //}
  //输出写入向量寄存器的
  //  when(wb.io.out_v.fire&wb.io.out_v.bits.warp_id===wid_to_check){
  //    printf(p"write v${wb.io.out_v.bits.reg_idxw} ")
  //    wb.io.out_v.bits.wb_wvd_rd.foreach(x=>printf(p"${Hexadecimal(x.asUInt)} "))
  //    printf(p"mask ${wb.io.out_v.bits.wvd_mask}\n")
  //  }
  //  ////输出写入标量寄存器的
  //  when(wb.io.out_x.fire&wb.io.out_x.bits.warp_id===wid_to_check){
  //    printf(p"write x${wb.io.out_x.bits.reg_idxw} 0x${Hexadecimal(wb.io.out_x.bits.wb_wxd_rd)}\n")
  //  }

  val sharedLsuFabric = Module(new PipeSharedLsuFabric(activeSubcoreCount, lsuFabricSourceWidth, lsuLocalInstrIdWidth))
  for (sc <- 0 until activeSubcoreCount) {
    sharedLsuFabric.io.subcore(sc).dcache_req <> subcoreBackends(sc).io.lsu_dcache_req
    subcoreBackends(sc).io.lsu_dcache_rsp <> sharedLsuFabric.io.subcore(sc).dcache_rsp
    sharedLsuFabric.io.subcore(sc).shared_req <> subcoreBackends(sc).io.lsu_shared_req
    subcoreBackends(sc).io.lsu_shared_rsp <> sharedLsuFabric.io.subcore(sc).shared_rsp
    sharedLsuFabric.io.subcore(sc).lsu_rsp <> subcoreBackends(sc).io.lsu_result_rsp
  }

  io.dcache_req.valid := sharedLsuFabric.io.sm_dcache_req.valid
  io.dcache_req.bits := sharedLsuFabric.io.sm_dcache_req.bits
  sharedLsuFabric.io.sm_dcache_req.ready := io.dcache_req.ready
  sharedLsuFabric.io.sm_dcache_rsp.valid := io.dcache_rsp.valid
  sharedLsuFabric.io.sm_dcache_rsp.bits := io.dcache_rsp.bits
  io.dcache_rsp.ready := sharedLsuFabric.io.sm_dcache_rsp.ready
  io.shared_req.valid := sharedLsuFabric.io.sm_shared_req.valid
  io.shared_req.bits := sharedLsuFabric.io.sm_shared_req.bits
  sharedLsuFabric.io.sm_shared_req.ready := io.shared_req.ready
  sharedLsuFabric.io.sm_shared_rsp.valid := io.shared_rsp.valid
  sharedLsuFabric.io.sm_shared_rsp.bits := io.shared_rsp.bits
  io.shared_rsp.ready := sharedLsuFabric.io.sm_shared_rsp.ready
  sharedLsuFabric.io.lsu2wb_lsu_rsp <> lsu2wb.io.lsu_rsp

  // LSU2WB remains SM-shared. Its global wid selects exactly one local
  // writeback LSU slot; all ordinary compute results already stayed local.
  val lsuScalarOwner = PipeSubcoreHelpers.subcoreValue(lsu2wb.io.out_x.bits.warp_id)
  val lsuVecOwner = PipeSubcoreHelpers.subcoreValue(lsu2wb.io.out_v.bits.warp_id)
  for (sc <- 0 until activeSubcoreCount) {
    val wb = localWritebacks(sc)
    wb.io.lsu_scalar.valid := lsu2wb.io.out_x.valid && lsuScalarOwner === sc.U
    wb.io.lsu_scalar.bits := localizeWriteScalar(lsu2wb.io.out_x.bits)
    wb.io.lsu_vec.valid := lsu2wb.io.out_v.valid && lsuVecOwner === sc.U
    wb.io.lsu_vec.bits := localizeWriteVec(lsu2wb.io.out_v.bits)
  }
  lsu2wb.io.out_x.ready := MuxLookup(lsuScalarOwner, false.B)(
    (0 until activeSubcoreCount).map(sc => sc.U -> localWritebacks(sc).io.lsu_scalar.ready)
  )
  lsu2wb.io.out_v.ready := MuxLookup(lsuVecOwner, false.B)(
    (0 until activeSubcoreCount).map(sc => sc.U -> localWritebacks(sc).io.lsu_vec.ready)
  )

  val activeCycles = RegInit(0.U(64.W))
  val totalScalarIssued = RegInit(0.U(64.W))
  val totalVectorIssued = RegInit(0.U(64.W))
  val execStructuralHazardCyclesX = RegInit(0.U(64.W))
  val execStructuralHazardCyclesV = RegInit(0.U(64.W))
  val dataDepStallCycles = RegInit(0.U(64.W))
  val barrierStallCycles = RegInit(0.U(64.W))
  val controlHazardFlushCount = RegInit(0.U(64.W))
  val frontendStallCycles = RegInit(0.U(64.W))
  val lsuBackpressureCycles = RegInit(0.U(64.W))
  val ibufferFullCycles = RegInit(0.U(64.W))

  val computeIssued = RegInit(0.U(64.W))
  val memIssued = RegInit(0.U(64.W))
  val ctrlIssued = RegInit(0.U(64.W))

  def isCtrlInst(ctrl: CtrlSigs): Bool = {
    ctrl.csr.orR || ctrl.barrier || ctrl.simt_stack || ctrl.branch.orR
  }

  val localIssueX = subcoreIbuffer2issue.map(_.io.out_x)
  val localIssueV = subcoreIbuffer2issue.map(_.io.out_v)
  val issueXFireCount = PopCount(VecInit(localIssueX.map(_.fire)))
  val issueVFireCount = PopCount(VecInit(localIssueV.map(_.fire)))
  val issueXMemFireCount = PopCount(VecInit(localIssueX.map(in => in.fire && in.bits.mem)))
  val issueVMemFireCount = PopCount(VecInit(localIssueV.map(in => in.fire && in.bits.mem)))
  val issueXCtrlFireCount = PopCount(VecInit(localIssueX.map(in => in.fire && !in.bits.mem && isCtrlInst(in.bits))))
  val issueVCtrlFireCount = PopCount(VecInit(localIssueV.map(in => in.fire && !in.bits.mem && isCtrlInst(in.bits))))
  val anyIssueFire = issueXFireCount.orR || issueVFireCount.orR
  val anyIssueInput = VecInit((localIssueX ++ localIssueV).map(_.valid)).asUInt.orR
  val anyIssueXBlocked = VecInit(localIssueX.map(in => in.valid && !in.ready)).asUInt.orR
  val anyIssueVBlocked = VecInit(localIssueV.map(in => in.valid && !in.ready)).asUInt.orR

  if (INST_CNT_2) {
    inst_cnt_xv(0) := inst_cnt_xv(0) + issueXFireCount
    inst_cnt_xv(1) := inst_cnt_xv(1) + issueVFireCount
  }

  val anyBufferedInst = VecInit(ibuffers.flatMap(_.io.out.map(_.valid))).asUInt.orR
  val anySchedulableInst = VecInit((0 until num_warp).map { i =>
    val sc = i & (num_subcore - 1)
    val local = i >> subcore_sel_bits
    ibuffers(sc).io.out(local).valid && warpSchedulers(sc).io.warp_ready(local)
  }).asUInt.orR
  val anyScoreExeBlockedInst = VecInit((0 until num_warp).map(i =>
    ibuffers(i & (num_subcore - 1)).io.out(i >> subcore_sel_bits).valid && scoreboardBusy(i)
  )).asUInt.orR
  val anyBarrierBlockedInst = VecInit((0 until num_warp).map(i =>
    ibuffers(i & (num_subcore - 1)).io.out(i >> subcore_sel_bits).valid &&
      lifecycleCtrlScaffold.io.global_barrier_wait(i)
  )).asUInt.orR
  val noIssueFire = !anyIssueFire
  val noIssueInput = !anyIssueInput
  val dataDepStall = noIssueFire && noIssueInput && anyBufferedInst && !anySchedulableInst && anyScoreExeBlockedInst
  val barrierStall = noIssueFire && noIssueInput && anyBufferedInst && !anySchedulableInst &&
    !anyScoreExeBlockedInst && anyBarrierBlockedInst
  val frontendStall = noIssueFire && noIssueInput && !dataDepStall && !barrierStall
  val flushEvent = anyFrontendFlushGlobal && !RegNext(anyFrontendFlushGlobal, false.B)

  when(io.perfReset){
    activeCycles := 0.U
    totalScalarIssued := 0.U
    totalVectorIssued := 0.U
    execStructuralHazardCyclesX := 0.U
    execStructuralHazardCyclesV := 0.U
    dataDepStallCycles := 0.U
    barrierStallCycles := 0.U
    controlHazardFlushCount := 0.U
    frontendStallCycles := 0.U
    lsuBackpressureCycles := 0.U
    ibufferFullCycles := 0.U
    computeIssued := 0.U
    memIssued := 0.U
    ctrlIssued := 0.U
  }.elsewhen(io.perfEnable){
    activeCycles := activeCycles + 1.U

    totalScalarIssued := totalScalarIssued + issueXFireCount
    totalVectorIssued := totalVectorIssued + issueVFireCount
    memIssued := memIssued + issueXMemFireCount + issueVMemFireCount
    ctrlIssued := ctrlIssued + issueXCtrlFireCount + issueVCtrlFireCount
    computeIssued := computeIssued + issueXFireCount + issueVFireCount -
      issueXMemFireCount - issueVMemFireCount - issueXCtrlFireCount - issueVCtrlFireCount

    when(anyIssueXBlocked){
      execStructuralHazardCyclesX := execStructuralHazardCyclesX + 1.U
    }
    when(anyIssueVBlocked){
      execStructuralHazardCyclesV := execStructuralHazardCyclesV + 1.U
    }
    when(dataDepStall){
      dataDepStallCycles := dataDepStallCycles + 1.U
    }
    when(barrierStall){
      barrierStallCycles := barrierStallCycles + 1.U
    }
    when(frontendStall){
      frontendStallCycles := frontendStallCycles + 1.U
    }
    when(flushEvent){
      controlHazardFlushCount := controlHazardFlushCount + 1.U
    }
    when(VecInit(localIssueV.map(in => in.valid && in.bits.mem && !in.ready)).asUInt.orR){
      lsuBackpressureCycles := lsuBackpressureCycles + 1.U
    }
    when(VecInit(ibuffers.map(bank => bank.io.in.valid && !bank.io.in.ready)).asUInt.orR){
      ibufferFullCycles := ibufferFullCycles + 1.U
    }
  }

  io.perf_pipeline.foreach { perf =>
    perf.activeCycles := activeCycles
    perf.totalScalarIssued := totalScalarIssued
    perf.totalVectorIssued := totalVectorIssued
    perf.execStructuralHazardCyclesX := execStructuralHazardCyclesX
    perf.execStructuralHazardCyclesV := execStructuralHazardCyclesV
    perf.dataDepStallCycles := dataDepStallCycles
    perf.barrierStallCycles := barrierStallCycles
    perf.controlHazardFlushCount := controlHazardFlushCount
    perf.frontendStallCycles := frontendStallCycles
    perf.lsuBackpressureCycles := lsuBackpressureCycles
    perf.ibufferFullCycles := ibufferFullCycles
  }

  io.perf_inst_class.foreach { perf =>
    perf.computeIssued := computeIssued
    perf.memIssued := memIssued
    perf.ctrlIssued := ctrlIssued
  }

  issue_stall := anyIssueXBlocked || anyIssueVBlocked
}

package top
import chisel3._
import chisel3.util._
import parameters._
import L1Cache.MyConfig
import L1Cache.ICache.{InstructionCache, ICacheMemReq_p, ICacheMemRsp, ICacheBundle}
import pipeline.{CTAreqData, CTArspData, CTA2warp, pipe}
import pipeline.{ICachePipeReq_np, ICachePipeRsp_np, DCacheCoreReq_np, DCacheCoreRsp_np}
import pipeline.{InstClassPerfCounters, PipelinePerfCounters}
import L1Cache.ShareMem.SharedMemory
import chisel3.experimental.hierarchy.{Definition, Instance, instantiable, public, Instantiate}
import config.config.Parameters
import gvm._
import mmu.L1TlbAutoReflect

class SMIO_icache(SV: Option[mmu.SVParam] = None)(implicit p: Parameters) extends ICacheBundle {
  val req = DecoupledIO(new ICacheMemReq_p(SV.getOrElse(mmu.SV32)))
  val rsp = Flipped(DecoupledIO(new ICacheMemRsp()))
}

@instantiable
class SM_wrapper_nocache() extends Module {
  val param = (new MyConfig).toInstance
  @public val sm_id = IO(Input(UInt(8.W)))
  @public val io = IO(new Bundle{
    val CTAreq = Flipped(Decoupled(new CTAreqData))
    val CTArsp = Decoupled(new CTArspData)
    val icache = new SMIO_icache()(param)
    val dcache_req = DecoupledIO(new DCacheCoreReq_np)
    val dcache_rsp = Flipped(DecoupledIO(new DCacheCoreRsp_np))
    val perfEnable = Input(Bool())
    val perfReset = Input(Bool())
    val pipeline_perf = if(PMU_PIPELINE) Some(Output(new PipelinePerfCounters)) else None
    val inst_class_perf = if(PMU_INST_CLASS) Some(Output(new InstClassPerfCounters)) else None
    val icache_invalidate = Input(Bool())
  })

  val cta2warp = Module(new CTA2warp)
  cta2warp.io.CTAreq :<>= io.CTAreq
  io.CTArsp :<>= cta2warp.io.CTArsp

  val pipe = Module(new pipe())
  pipe.sm_id := sm_id
  pipe.io.perfEnable := io.perfEnable
  pipe.io.perfReset := io.perfReset
  io.pipeline_perf.foreach(_ := pipe.io.perf_pipeline.getOrElse(0.U.asTypeOf(new PipelinePerfCounters)))
  io.inst_class_perf.foreach(_ := pipe.io.perf_inst_class.getOrElse(0.U.asTypeOf(new InstClassPerfCounters)))

  val cnt = Counter(10)
  when(cnt.value < 5.U) { cnt.inc() }
  pipe.io.pc_reset := (cnt.value < 5.U) // Reset the pipe for the first 5 cycles

  pipe.io.warpReq :<>= cta2warp.io.warpReq
  cta2warp.io.warpRsp :<>= pipe.io.warpRsp
  cta2warp.io.wg_id_lookup := pipe.io.wg_id_lookup
  pipe.io.wg_id_tag:=cta2warp.io.wg_id_tag

  val sharedmem = Module(new SharedMemory()(param))
  sharedmem.io.coreReq.bits.data:=pipe.io.shared_req.bits.data
  sharedmem.io.coreReq.bits.instrId:=pipe.io.shared_req.bits.instrId
  sharedmem.io.coreReq.bits.isWrite:=pipe.io.shared_req.bits.isWrite
  sharedmem.io.coreReq.bits.setIdx:=pipe.io.shared_req.bits.setIdx
  sharedmem.io.coreReq.bits.perLaneAddr:=pipe.io.shared_req.bits.perLaneAddr
  sharedmem.io.coreReq.bits.sourceTag:=false.B // No DMA in nocache variant
  sharedmem.io.coreReq.valid:=pipe.io.shared_req.valid
  pipe.io.shared_req.ready:=sharedmem.io.coreReq.ready
  sharedmem.io.coreRsp.ready:=pipe.io.shared_rsp.ready
  pipe.io.shared_rsp.valid:=sharedmem.io.coreRsp.valid
  pipe.io.shared_rsp.bits.data:=sharedmem.io.coreRsp.bits.data
  pipe.io.shared_rsp.bits.instrId:=sharedmem.io.coreRsp.bits.instrId
  pipe.io.shared_rsp.bits.activeMask:=sharedmem.io.coreRsp.bits.activeMask

  val icache = Module(new InstructionCache()(param))
  icache.io.invalidate := io.icache_invalidate
  // **** icache coreReq ****
  pipe.io.icache_req.ready:=icache.io.coreReq.ready
  icache.io.coreReq.valid:=pipe.io.icache_req.valid
  icache.io.coreReq.bits.addr:=pipe.io.icache_req.bits.addr
  icache.io.coreReq.bits.warpid:=pipe.io.icache_req.bits.warpid
  icache.io.coreReq.bits.mask:=pipe.io.icache_req.bits.mask
  if(MMU_ENABLED){ icache.io.coreReq.bits.asid.get := pipe.io.icache_req.bits.asid.get }
  icache.io.coreReq.bits.spike_info.foreach( _ := DontCare )
  // **** icache coreRsp ****
  pipe.io.icache_rsp.valid:=icache.io.coreRsp.valid
  pipe.io.icache_rsp.bits.warpid:=icache.io.coreRsp.bits.warpid
  pipe.io.icache_rsp.bits.data:=icache.io.coreRsp.bits.data
  pipe.io.icache_rsp.bits.addr:=icache.io.coreRsp.bits.addr
  pipe.io.icache_rsp.bits.status:=icache.io.coreRsp.bits.status
  pipe.io.icache_rsp.bits.mask:=icache.io.coreRsp.bits.mask
  icache.io.coreRsp.ready:=pipe.io.icache_rsp.ready
  icache.io.externalFlushPipe.bits.warpid :=pipe.io.externalFlushPipe.bits
  icache.io.externalFlushPipe.valid :=pipe.io.externalFlushPipe.valid

  io.icache.req :<>= icache.io.memReq
  icache.io.memRsp :<>= io.icache.rsp
  io.dcache_req :<>= pipe.io.dcache_req
  pipe.io.dcache_rsp :<>= io.dcache_rsp

  if(GVM_ENABLED){
    val WF_ID_WIDTH = log2Ceil(num_warp_in_a_block)
    val gvm_cta2warp = Module(new GvmDutCta2Warp)
    gvm_cta2warp.io.clock := clock
    gvm_cta2warp.io.reset := reset.asBool
    gvm_cta2warp.io.warp_req_fire := cta2warp.io.warpReq.fire
    gvm_cta2warp.io.software_wg_id := cta2warp.io.warpReq.bits.CTAdata.dispatch2cu_wg_id.pad(32)
    gvm_cta2warp.io.software_warp_id :=
      cta2warp.io.warpReq.bits.CTAdata.dispatch2cu_wf_tag_dispatch(WF_ID_WIDTH - 1, 0).pad(32)
    gvm_cta2warp.io.sm_id := sm_id
    gvm_cta2warp.io.hardware_warp_id := cta2warp.io.warpReq.bits.wid.pad(32)
    gvm_cta2warp.io.sgpr_base := cta2warp.io.warpReq.bits.CTAdata.dispatch2cu_sgpr_base_dispatch.pad(32)
    gvm_cta2warp.io.vgpr_base := cta2warp.io.warpReq.bits.CTAdata.dispatch2cu_vgpr_base_dispatch.pad(32)
    gvm_cta2warp.io.wg_slot_id_in_warp_sche :=
      cta2warp.io.warpReq.bits.CTAdata.dispatch2cu_wf_tag_dispatch(TAG_WIDTH - 1, WF_ID_WIDTH)
    gvm_cta2warp.io.lds_base := Cat(
      LDS_BASE.U(32.W)(31, LDS_ID_WIDTH + 1),
      cta2warp.io.warpReq.bits.CTAdata.dispatch2cu_lds_base_dispatch
    )
    gvm_cta2warp.io.rtl_num_thread := cta2warp.io.warpReq.bits.CTAdata.dispatch2cu_wf_size_dispatch.pad(32)
  }

  // DMA ports: tie off for nocache variant (no L2 arbiter in this config)
  // DMA L2 cache requests are sunk; L2 responses never arrive.
  pipe.io.dma_cache_req.ready := false.B
  pipe.io.dma_cache_rsp.valid := false.B
  pipe.io.dma_cache_rsp.bits := DontCare
  // DMA shared memory requests are sunk.
  pipe.io.dma_shared_req.ready := false.B
  pipe.io.dma_shared_rsp.valid := false.B
  pipe.io.dma_shared_rsp.bits := DontCare
  // fence_end_dma: consumed inside pipe by warp scheduler; sink for observability.
  pipe.io.fence_end_dma.ready := true.B
  // DMA TLB: identity-mapping bypass (nocache variant has no L1 TLB)
  val dma_tlb_state = RegInit(false.B) // false=idle, true=reply
  val dma_tlb_paddr = Reg(UInt(mmu.SV32.paLen.W))
  when(!dma_tlb_state && pipe.io.dma_tlb_req.valid) {
    dma_tlb_state := true.B
    dma_tlb_paddr := pipe.io.dma_tlb_req.bits.vaddr
  }
  when(dma_tlb_state && pipe.io.dma_tlb_rsp.ready) {
    dma_tlb_state := false.B
  }
  pipe.io.dma_tlb_req.ready      := !dma_tlb_state
  pipe.io.dma_tlb_rsp.valid      := dma_tlb_state
  pipe.io.dma_tlb_rsp.bits.paddr := dma_tlb_paddr
  // Deadlock detection: DMA instructions should never reach pipe in nocache builds.
  when(!reset.asBool) {
    assert(!pipe.io.dma_cache_req.valid,
      "DMA L2 request reached nocache build - DMA instructions not supported in this configuration")
  }
}

class GPGPU_top_nocache() extends Module {
  val num_sm = parameters.num_sm
  val param = (new MyConfig).toInstance
  val io = IO(new Bundle {
    val host_req=Flipped(DecoupledIO(new host2CTA_data))
    val host_rsp=DecoupledIO(new CTA2host_data)
    val icache = Vec(num_sm, new SMIO_icache()(param))
    val dcache_req = Vec(num_sm, DecoupledIO(new DCacheCoreReq_np))
    val dcache_rsp = Vec(num_sm, Flipped(DecoupledIO(new DCacheCoreRsp_np)))
    val perfDump = Input(Bool())
    val perfDumpSummary = Input(Bool())
    val icache_invalidate = Input(Bool())
  })
  val cta = Module(new CTAinterface)
  val sm_wrapper_inst = Seq.tabulate(num_sm) { i => Instantiate(new SM_wrapper_nocache()) }
  sm_wrapper_inst.zipWithIndex.foreach { case (sm, i) => sm.sm_id := i.U }
  val sm_wrapper = VecInit.tabulate(num_sm) { i => sm_wrapper_inst(i).io }

  cta.io.host2CTA :<>= io.host_req
  io.host_rsp :<>= cta.io.CTA2host

  def sumPipelinePerfCounter(select: PipelinePerfCounters => UInt): UInt = {
    if (PMU_PIPELINE) sm_wrapper.map(sm => select(sm.pipeline_perf.get)).reduce(_ + _) else 0.U(64.W)
  }
  def sumInstClassPerfCounter(select: InstClassPerfCounters => UInt): UInt = {
    if (PMU_INST_CLASS) sm_wrapper.map(sm => select(sm.inst_class_perf.get)).reduce(_ + _) else 0.U(64.W)
  }

  val pmuActiveCycles = sumPipelinePerfCounter(_.activeCycles)
  val pmuTotalScalarIssued = sumPipelinePerfCounter(_.totalScalarIssued)
  val pmuTotalVectorIssued = sumPipelinePerfCounter(_.totalVectorIssued)
  val pmuExecHazardX = sumPipelinePerfCounter(_.execStructuralHazardCyclesX)
  val pmuExecHazardV = sumPipelinePerfCounter(_.execStructuralHazardCyclesV)
  val pmuDataDepStall = sumPipelinePerfCounter(_.dataDepStallCycles)
  val pmuBarrierStall = sumPipelinePerfCounter(_.barrierStallCycles)
  val pmuCtrlFlushCnt = sumPipelinePerfCounter(_.controlHazardFlushCount)
  val pmuFrontendStall = sumPipelinePerfCounter(_.frontendStallCycles)
  val pmuLsuBackpressure = sumPipelinePerfCounter(_.lsuBackpressureCycles)
  val pmuIbufferFullCycles = sumPipelinePerfCounter(_.ibufferFullCycles)
  val pmuComputeIssued = sumInstClassPerfCounter(_.computeIssued)
  val pmuMemIssued = sumInstClassPerfCounter(_.memIssued)
  val pmuCtrlIssued = sumInstClassPerfCounter(_.ctrlIssued)
  val pmuTotalIssued = pmuTotalScalarIssued + pmuTotalVectorIssued

  val perfWindowStarted = RegInit(false.B)
  val perfWindowPrinted = RegInit(false.B)
  val programId = RegInit(0.U(32.W))
  val totalProgramWindows = RegInit(0.U(32.W))
  val totalActiveCycles = RegInit(0.U(64.W))
  val totalScalarIssued = RegInit(0.U(64.W))
  val totalVectorIssued = RegInit(0.U(64.W))
  val totalExecHazardX = RegInit(0.U(64.W))
  val totalExecHazardV = RegInit(0.U(64.W))
  val totalDataDepStall = RegInit(0.U(64.W))
  val totalBarrierStall = RegInit(0.U(64.W))
  val totalCtrlFlushCnt = RegInit(0.U(64.W))
  val totalFrontendStall = RegInit(0.U(64.W))
  val totalLsuBackpressure = RegInit(0.U(64.W))
  val totalIbufferFullCycles = RegInit(0.U(64.W))
  val totalComputeIssued = RegInit(0.U(64.W))
  val totalMemIssued = RegInit(0.U(64.W))
  val totalCtrlIssued = RegInit(0.U(64.W))
  val perfStartPulse = io.host_req.fire && !perfWindowStarted
  val perfDumpPulse = io.perfDump && perfWindowStarted && !perfWindowPrinted
  when(perfStartPulse){
    perfWindowStarted := true.B
    perfWindowPrinted := false.B
  }.elsewhen(perfDumpPulse){
    perfWindowStarted := false.B
    perfWindowPrinted := true.B
    programId := programId + 1.U
    totalProgramWindows := totalProgramWindows + 1.U
    totalActiveCycles := totalActiveCycles + pmuActiveCycles
    totalScalarIssued := totalScalarIssued + pmuTotalScalarIssued
    totalVectorIssued := totalVectorIssued + pmuTotalVectorIssued
    totalExecHazardX := totalExecHazardX + pmuExecHazardX
    totalExecHazardV := totalExecHazardV + pmuExecHazardV
    totalDataDepStall := totalDataDepStall + pmuDataDepStall
    totalBarrierStall := totalBarrierStall + pmuBarrierStall
    totalCtrlFlushCnt := totalCtrlFlushCnt + pmuCtrlFlushCnt
    totalFrontendStall := totalFrontendStall + pmuFrontendStall
    totalLsuBackpressure := totalLsuBackpressure + pmuLsuBackpressure
    totalIbufferFullCycles := totalIbufferFullCycles + pmuIbufferFullCycles
    totalComputeIssued := totalComputeIssued + pmuComputeIssued
    totalMemIssued := totalMemIssued + pmuMemIssued
    totalCtrlIssued := totalCtrlIssued + pmuCtrlIssued
  }

  for (i <- 0 until num_sm) {
    sm_wrapper(i).CTAreq :<>= cta.io.CTA2warp(i)
    cta.io.warp2CTA(i) :<>= sm_wrapper(i).CTArsp

    sm_wrapper(i).perfEnable := perfWindowStarted || perfStartPulse
    sm_wrapper(i).perfReset := perfStartPulse
    io.icache(i).req :<>= sm_wrapper(i).icache.req
    sm_wrapper(i).icache.rsp :<>= io.icache(i).rsp
    sm_wrapper(i).icache_invalidate := io.icache_invalidate

    io.dcache_req(i) :<>= sm_wrapper(i).dcache_req
    sm_wrapper(i).dcache_rsp :<>= io.dcache_rsp(i)
  }

  def includeCurrentWindow(total: UInt, current: UInt): UInt = {
    total + Mux(perfDumpPulse, current, 0.U(total.getWidth.W))
  }

  val summaryProgramWindows = includeCurrentWindow(totalProgramWindows, 1.U(32.W))
  val summaryActiveCycles = includeCurrentWindow(totalActiveCycles, pmuActiveCycles)
  val summaryScalarIssued = includeCurrentWindow(totalScalarIssued, pmuTotalScalarIssued)
  val summaryVectorIssued = includeCurrentWindow(totalVectorIssued, pmuTotalVectorIssued)
  val summaryExecHazardX = includeCurrentWindow(totalExecHazardX, pmuExecHazardX)
  val summaryExecHazardV = includeCurrentWindow(totalExecHazardV, pmuExecHazardV)
  val summaryDataDepStall = includeCurrentWindow(totalDataDepStall, pmuDataDepStall)
  val summaryBarrierStall = includeCurrentWindow(totalBarrierStall, pmuBarrierStall)
  val summaryCtrlFlushCnt = includeCurrentWindow(totalCtrlFlushCnt, pmuCtrlFlushCnt)
  val summaryFrontendStall = includeCurrentWindow(totalFrontendStall, pmuFrontendStall)
  val summaryLsuBackpressure = includeCurrentWindow(totalLsuBackpressure, pmuLsuBackpressure)
  val summaryIbufferFullCycles = includeCurrentWindow(totalIbufferFullCycles, pmuIbufferFullCycles)
  val summaryComputeIssued = includeCurrentWindow(totalComputeIssued, pmuComputeIssued)
  val summaryMemIssued = includeCurrentWindow(totalMemIssued, pmuMemIssued)
  val summaryCtrlIssued = includeCurrentWindow(totalCtrlIssued, pmuCtrlIssued)
  val summaryTotalIssued = summaryScalarIssued + summaryVectorIssued
  val summaryTotalClassIssued = summaryComputeIssued + summaryMemIssued + summaryCtrlIssued

  when(perfDumpPulse){
    printf(p"\n[PROGRAM ${programId}] [PMU] first-kernel-start -> last-kernel-end summary (nocache)\n")
    if (PMU_PIPELINE) {
      printf(p"[PROGRAM ${programId}] [INST+CYCLE] active cycles       : ${pmuActiveCycles}\n")
      printf(p"[PROGRAM ${programId}] [INST+CYCLE] scalar issued       : ${pmuTotalScalarIssued}\n")
      printf(p"[PROGRAM ${programId}] [INST+CYCLE] vector issued       : ${pmuTotalVectorIssued}\n")
      printf(p"[PROGRAM ${programId}] [INST+CYCLE] total issued        : ${pmuTotalIssued}\n")
      printf(p"[PROGRAM ${programId}] [STALL] exec hazard X          : ${pmuExecHazardX}\n")
      printf(p"[PROGRAM ${programId}] [STALL] exec hazard V          : ${pmuExecHazardV}\n")
      printf(p"[PROGRAM ${programId}] [STALL] data dependency        : ${pmuDataDepStall}\n")
      printf(p"[PROGRAM ${programId}] [STALL] barrier stall cycles   : ${pmuBarrierStall}\n")
      printf(p"[PROGRAM ${programId}] [STALL] control flush events   : ${pmuCtrlFlushCnt}\n")
      printf(p"[PROGRAM ${programId}] [STALL] frontend stall cycles  : ${pmuFrontendStall}\n")
      printf(p"[PROGRAM ${programId}] [STALL] lsu backpressure cyc   : ${pmuLsuBackpressure}\n")
      printf(p"[PROGRAM ${programId}] [STALL] ibuffer full cycles    : ${pmuIbufferFullCycles}\n")
    }
    if (PMU_INST_CLASS) {
      printf(p"[PROGRAM ${programId}] [INST CLASS] compute issued    : ${pmuComputeIssued}\n")
      printf(p"[PROGRAM ${programId}] [INST CLASS] mem issued        : ${pmuMemIssued}\n")
      printf(p"[PROGRAM ${programId}] [INST CLASS] ctrl issued       : ${pmuCtrlIssued}\n")
      printf(p"[PROGRAM ${programId}] [INST CLASS] total class issued: ${pmuComputeIssued + pmuMemIssued + pmuCtrlIssued}\n")
    }
  }
  when((perfDumpPulse || io.perfDumpSummary) && summaryProgramWindows =/= 0.U){
    printf(p"\n[TESTCASE TOTAL] [PMU] accumulated summary across ${summaryProgramWindows} program windows (nocache)\n")
    if (PMU_PIPELINE) {
      printf(p"[TESTCASE TOTAL] [INST+CYCLE] active cycles       : ${summaryActiveCycles}\n")
      printf(p"[TESTCASE TOTAL] [INST+CYCLE] scalar issued       : ${summaryScalarIssued}\n")
      printf(p"[TESTCASE TOTAL] [INST+CYCLE] vector issued       : ${summaryVectorIssued}\n")
      printf(p"[TESTCASE TOTAL] [INST+CYCLE] total issued        : ${summaryTotalIssued}\n")
      printf(p"[TESTCASE TOTAL] [STALL] exec hazard X          : ${summaryExecHazardX}\n")
      printf(p"[TESTCASE TOTAL] [STALL] exec hazard V          : ${summaryExecHazardV}\n")
      printf(p"[TESTCASE TOTAL] [STALL] data dependency        : ${summaryDataDepStall}\n")
      printf(p"[TESTCASE TOTAL] [STALL] barrier stall cycles   : ${summaryBarrierStall}\n")
      printf(p"[TESTCASE TOTAL] [STALL] control flush events   : ${summaryCtrlFlushCnt}\n")
      printf(p"[TESTCASE TOTAL] [STALL] frontend stall cycles  : ${summaryFrontendStall}\n")
      printf(p"[TESTCASE TOTAL] [STALL] lsu backpressure cyc   : ${summaryLsuBackpressure}\n")
      printf(p"[TESTCASE TOTAL] [STALL] ibuffer full cycles    : ${summaryIbufferFullCycles}\n")
    }
    if (PMU_INST_CLASS) {
      printf(p"[TESTCASE TOTAL] [INST CLASS] compute issued    : ${summaryComputeIssued}\n")
      printf(p"[TESTCASE TOTAL] [INST CLASS] mem issued        : ${summaryMemIssued}\n")
      printf(p"[TESTCASE TOTAL] [INST CLASS] ctrl issued       : ${summaryCtrlIssued}\n")
      printf(p"[TESTCASE TOTAL] [INST CLASS] total class issued: ${summaryTotalClassIssued}\n")
    }
  }

  import top.ParametersToJson
  ParametersToJson.saveToJson("sim-verilator-nocache/parameters.json")
}

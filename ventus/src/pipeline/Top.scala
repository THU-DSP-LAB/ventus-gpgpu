package pipeline

import chisel3._
import chisel3.util._
import chisel3.util.experimental.loadMemoryFromFile
import pipeline.parameters._
import L1Cache.ICache._
import L1Cache.MyConfig
import L1Cache.DCache._
import L1Cache.ShareMem._
import L1Cache._
import config.config._
import CTA._
import top._


class host2CTA_data extends Bundle{
  val host_wg_id            = (UInt(WG_ID_WIDTH.W))
  val host_num_wf           = (UInt(WF_COUNT_WIDTH.W))
  val host_wf_size          = (UInt(WAVE_ITEM_WIDTH.W))
  val host_start_pc         = (UInt(MEM_ADDR_WIDTH.W))
  val host_vgpr_size_total  = (UInt((VGPR_ID_WIDTH + 1).W))
  val host_sgpr_size_total  = (UInt((SGPR_ID_WIDTH + 1).W))
  val host_lds_size_total   = (UInt((LDS_ID_WIDTH + 1).W))
  val host_gds_size_total   = (UInt((GDS_ID_WIDTH + 1).W))
  val host_vgpr_size_per_wf = (UInt((VGPR_ID_WIDTH + 1).W))
  val host_sgpr_size_per_wf = (UInt((SGPR_ID_WIDTH + 1).W))
  val host_gds_baseaddr = UInt(MEM_ADDR_WIDTH.W)
}
class CTA2host_data extends Bundle{
  val inflight_wg_buffer_host_wf_done_wg_id = (UInt(WG_ID_WIDTH.W))
}

class CTAinterface extends Module{
  val io=IO(new Bundle{
    val host2CTA=Flipped(DecoupledIO(new host2CTA_data))
    val CTA2host=DecoupledIO(new CTA2host_data)
    val CTA2warp=Vec(NUMBER_CU,DecoupledIO(new CTAreqData))
    val warp2CTA=Vec(NUMBER_CU,Flipped(DecoupledIO(new CTArspData)))
  })
  val cta_sche=Module(new cta_scheduler( NUMBER_CU: Int,  CU_ID_WIDTH: Int,  RES_TABLE_ADDR_WIDTH: Int,  VGPR_ID_WIDTH: Int,  NUMBER_VGPR_SLOTS: Int,  SGPR_ID_WIDTH: Int,  NUMBER_SGPR_SLOTS: Int,  LDS_ID_WIDTH: Int,  NUMBER_LDS_SLOTS: Int,  WG_ID_WIDTH: Int,  WF_COUNT_WIDTH: Int,  WG_SLOT_ID_WIDTH: Int,  NUMBER_WF_SLOTS: Int,  WF_COUNT_MAX: Int,  NUMBER_RES_TABLE: Int,  GDS_ID_WIDTH: Int,  GDS_SIZE: Int,  ENTRY_ADDR_WIDTH: Int,  NUMBER_ENTRIES: Int,  WAVE_ITEM_WIDTH: Int,  MEM_ADDR_WIDTH: Int,  TAG_WIDTH: Int,  INIT_MAX_WG_COUNT: Int,WF_COUNT_WIDTH_PER_WG: Int))
  cta_sche.io.host_wg_valid         := io.host2CTA.valid
  cta_sche.io.host_wg_id            := io.host2CTA.bits.host_wg_id
  cta_sche.io.host_num_wf           := io.host2CTA.bits.host_num_wf
  cta_sche.io.host_wf_size          := io.host2CTA.bits.host_wf_size
  cta_sche.io.host_start_pc         := io.host2CTA.bits.host_start_pc
  cta_sche.io.host_vgpr_size_total  := io.host2CTA.bits.host_vgpr_size_total
  cta_sche.io.host_sgpr_size_total  := io.host2CTA.bits.host_sgpr_size_total
  cta_sche.io.host_lds_size_total   := io.host2CTA.bits.host_lds_size_total
  cta_sche.io.host_gds_size_total   := io.host2CTA.bits.host_gds_size_total
  cta_sche.io.host_vgpr_size_per_wf := io.host2CTA.bits.host_vgpr_size_per_wf
  cta_sche.io.host_sgpr_size_per_wf := io.host2CTA.bits.host_sgpr_size_per_wf
  cta_sche.io.host_gds_baseaddr := io.host2CTA.bits.host_gds_baseaddr
  io.host2CTA.ready:=cta_sche.io.inflight_wg_buffer_host_rcvd_ack

  val wf_done_interface=Module(new wf_done_interface_single(WG_ID_WIDTH:Int,NUM_SCHEDULER: Int,WG_NUM_MAX: Int))
  wf_done_interface.io.wf_done:=cta_sche.io.inflight_wg_buffer_host_wf_done
  wf_done_interface.io.wf_done_wg_id:=cta_sche.io.inflight_wg_buffer_host_wf_done_wg_id

  io.CTA2host.bits.inflight_wg_buffer_host_wf_done_wg_id:=wf_done_interface.io.host_wf_done_wg_id
  io.CTA2host.valid:=wf_done_interface.io.host_wf_done_valid
  wf_done_interface.io.host_wf_done_ready:=io.CTA2host.ready

  val cu2dispatch_wf_done=Wire(Vec(NUMBER_CU,Bool()))
  for (i <- 0 until NUMBER_CU){
    io.CTA2warp(i).valid:=cta_sche.io.dispatch2cu_wf_dispatch(i)
    io.CTA2warp(i).bits.dispatch2cu_wg_wf_count:= cta_sche.io.dispatch2cu_wg_wf_count
    io.CTA2warp(i).bits.dispatch2cu_wf_size_dispatch   := cta_sche.io.dispatch2cu_wf_size_dispatch
    io.CTA2warp(i).bits.dispatch2cu_sgpr_base_dispatch := cta_sche.io.dispatch2cu_sgpr_base_dispatch
    io.CTA2warp(i).bits.dispatch2cu_vgpr_base_dispatch := cta_sche.io.dispatch2cu_vgpr_base_dispatch
    io.CTA2warp(i).bits.dispatch2cu_wf_tag_dispatch    := cta_sche.io.dispatch2cu_wf_tag_dispatch
    io.CTA2warp(i).bits.dispatch2cu_lds_base_dispatch  := cta_sche.io.dispatch2cu_lds_base_dispatch
    io.CTA2warp(i).bits.dispatch2cu_start_pc_dispatch  := cta_sche.io.dispatch2cu_start_pc_dispatch
    io.CTA2warp(i).bits.dispatch2cu_gds_base_dispatch :=cta_sche.io.dispatch2cu_gds_base_dispatch
    cta_sche.io.cu2dispatch_ready_for_dispatch(i):=io.CTA2warp(i).ready

    cta_sche.io.cu2dispatch_wf_tag_done(i):=io.warp2CTA(i).bits.cu2dispatch_wf_tag_done
    cu2dispatch_wf_done(i):=io.warp2CTA(i).valid
    io.warp2CTA(i).ready:=true.B
  }
  cta_sche.io.cu2dispatch_wf_done:=cu2dispatch_wf_done.asUInt

  //val sm=VecInit(Seq.fill(NUMBER_CU)(Module(new SM_wrapper).io))
  //for (i <- 0 until NUMBER_CU){}
}

class SM_wrapper extends Module{
  val param = (new MyConfig).toInstance
  val io = IO(new Bundle{
    val CTAreq=Flipped(Decoupled(new CTAreqData))
    val CTArsp=(Decoupled(new CTArspData))
    val memRsp = Flipped(DecoupledIO(new L1CacheMemRsp()(param)))
    val memReq = DecoupledIO(new L1CacheMemReq()(param))
    val inst = if (SINGLE_INST) Some(Flipped(DecoupledIO(UInt(32.W)))) else None
  })
  val cta2warp=Module(new CTA2warp)
  cta2warp.io.CTAreq<>io.CTAreq
  cta2warp.io.CTArsp<>io.CTArsp
  val pipe=Module(new pipe)
  pipe.io.pc_reset:=true.B
  val cnt=Counter(10)
  when(cnt.value<5.U){cnt.inc()}
  when(cnt.value===5.U){pipe.io.pc_reset:=false.B}
  pipe.io.warpReq<>cta2warp.io.warpReq
  pipe.io.warpRsp<>cta2warp.io.warpRsp
  pipe.io.wg_id_tag:=cta2warp.io.wg_id_tag
  cta2warp.io.wg_id_lookup:=pipe.io.wg_id_lookup
  val l1Cache2L2Arb = Module(new L1Cache2L2Arbiter()(param))
  io.memReq <> l1Cache2L2Arb.io.memReqOut
  l1Cache2L2Arb.io.memRspIn <> io.memRsp

  val icache = Module(new InstructionCache()(param))
  // **** icache memRsp ****
  icache.io.memRsp.valid := l1Cache2L2Arb.io.memRspVecOut(0).valid
  icache.io.memRsp.bits.d_addr := l1Cache2L2Arb.io.memRspVecOut(0).bits.d_addr
  icache.io.memRsp.bits.d_data := l1Cache2L2Arb.io.memRspVecOut(0).bits.d_data
  icache.io.memRsp.bits.d_source := l1Cache2L2Arb.io.memRspVecOut(0).bits.d_source
  l1Cache2L2Arb.io.memRspVecOut(0).ready := icache.io.memRsp.ready
  // ***********************
  // **** icache memReq ****
  l1Cache2L2Arb.io.memReqVecIn(0).valid := icache.io.memReq.valid
  l1Cache2L2Arb.io.memReqVecIn(0).bits.a_opcode := 4.U(3.W)
  //TODO changed to TLAOp_Get when L1param system established
  l1Cache2L2Arb.io.memReqVecIn(0).bits.a_addr := icache.io.memReq.bits.a_addr
  l1Cache2L2Arb.io.memReqVecIn(0).bits.a_source := icache.io.memReq.bits.a_source
  l1Cache2L2Arb.io.memReqVecIn(0).bits.a_data := 0.U.asTypeOf(new L1CacheMemReq()(param).a_data)
  l1Cache2L2Arb.io.memReqVecIn(0).bits.a_mask.foreach{_ := true.B}
  icache.io.memReq.ready := l1Cache2L2Arb.io.memReqVecIn(0).ready
  // ***********************
  // **** icache coreReq ****
  pipe.io.icache_req.ready:=icache.io.coreReq.ready
  icache.io.coreReq.valid:=pipe.io.icache_req.valid
  icache.io.coreReq.bits.addr:=pipe.io.icache_req.bits.addr
  icache.io.coreReq.bits.warpid:=pipe.io.icache_req.bits.warpid
  // ***********************
  // **** icache coreRsp ****
  pipe.io.icache_rsp.valid:=icache.io.coreRsp.valid
  pipe.io.icache_rsp.bits.warpid:=icache.io.coreRsp.bits.warpid
  pipe.io.icache_rsp.bits.data:=icache.io.coreRsp.bits.data
  pipe.io.icache_rsp.bits.addr:=icache.io.coreRsp.bits.addr
  pipe.io.icache_rsp.bits.status:=icache.io.coreRsp.bits.status
  icache.io.coreRsp.ready:=pipe.io.icache_rsp.ready
  // ***********************
  icache.io.externalFlushPipe.bits.warpid :=pipe.io.externalFlushPipe.bits
  icache.io.externalFlushPipe.valid :=pipe.io.externalFlushPipe.valid

  val dcache = Module(new DataCache()(param))
  // **** dcache memRsp ****
  dcache.io.memRsp.valid := l1Cache2L2Arb.io.memRspVecOut(1).valid
  dcache.io.memRsp.bits.d_source := l1Cache2L2Arb.io.memRspVecOut(1).bits.d_source
  dcache.io.memRsp.bits.d_addr := l1Cache2L2Arb.io.memRspVecOut(1).bits.d_addr
  dcache.io.memRsp.bits.d_data := l1Cache2L2Arb.io.memRspVecOut(1).bits.d_data
  l1Cache2L2Arb.io.memRspVecOut(1).ready := dcache.io.memRsp.ready
  // ***********************
  // **** dcache memReq ****
  l1Cache2L2Arb.io.memReqVecIn(1) <> dcache.io.memReq
  // **** dcache coreReq ****
  pipe.io.dcache_req.ready:=dcache.io.coreReq.ready
  dcache.io.coreReq.valid:=pipe.io.dcache_req.valid
  dcache.io.coreReq.bits.data:=pipe.io.dcache_req.bits.data
  dcache.io.coreReq.bits.instrId:=pipe.io.dcache_req.bits.instrId
  dcache.io.coreReq.bits.setIdx:=pipe.io.dcache_req.bits.setIdx
  dcache.io.coreReq.bits.isWrite:=pipe.io.dcache_req.bits.isWrite
  dcache.io.coreReq.bits.perLaneAddr:=pipe.io.dcache_req.bits.perLaneAddr
  dcache.io.coreReq.bits.tag:=pipe.io.dcache_req.bits.tag
  // **** dcache coreRsp ****
  pipe.io.dcache_rsp.valid:=dcache.io.coreRsp.valid
  pipe.io.dcache_rsp.bits.instrId:=dcache.io.coreRsp.bits.instrId
  pipe.io.dcache_rsp.bits.data:=dcache.io.coreRsp.bits.data
  pipe.io.dcache_rsp.bits.activeMask:=dcache.io.coreRsp.bits.activeMask
  pipe.io.dcache_rsp.bits.isWrite:=dcache.io.coreRsp.bits.isWrite
  dcache.io.coreRsp.ready:=pipe.io.dcache_rsp.ready

  val sharedmem = Module(new SharedMemory()(param))
  sharedmem.io.coreReq.bits.data:=pipe.io.shared_req.bits.data
  sharedmem.io.coreReq.bits.instrId:=pipe.io.shared_req.bits.instrId
  sharedmem.io.coreReq.bits.isWrite:=pipe.io.shared_req.bits.isWrite
  sharedmem.io.coreReq.bits.setIdx:=pipe.io.shared_req.bits.setIdx
  sharedmem.io.coreReq.bits.perLaneAddr:=pipe.io.shared_req.bits.perLaneAddr
  sharedmem.io.coreReq.valid:=pipe.io.shared_req.valid
  pipe.io.shared_req.ready:=sharedmem.io.coreReq.ready

  sharedmem.io.coreRsp.ready:=pipe.io.shared_rsp.ready
  pipe.io.shared_rsp.valid:=sharedmem.io.coreRsp.valid
  pipe.io.shared_rsp.bits.data:=sharedmem.io.coreRsp.bits.data
  pipe.io.shared_rsp.bits.instrId:=sharedmem.io.coreRsp.bits.instrId
  pipe.io.shared_rsp.bits.activeMask:=sharedmem.io.coreRsp.bits.activeMask
  pipe.io.shared_rsp.bits.isWrite:=sharedmem.io.coreRsp.bits.isWrite
}




class CPUtest extends Module{
  val io=IO(new Bundle{
    val host2cta=Decoupled(new host2CTA_data)
    val cta2host=Flipped(Decoupled(new CTA2host_data))
  })
  val num_of_block = 2.U
  io.host2cta.valid:=false.B
  io.host2cta.bits.host_wg_id:=0.U
  io.host2cta.bits.host_num_wf:=2.U // two warps
  io.host2cta.bits.host_wf_size:=num_thread.asUInt()
  io.host2cta.bits.host_start_pc:=0.U // start pc
  io.host2cta.bits.host_vgpr_size_total:= 64.U
  io.host2cta.bits.host_sgpr_size_total:= 64.U
  io.host2cta.bits.host_lds_size_total:= 128.U
  io.host2cta.bits.host_gds_size_total:= 128.U
  io.host2cta.bits.host_vgpr_size_per_wf:=32.U
  io.host2cta.bits.host_sgpr_size_per_wf:=32.U
  io.host2cta.bits.host_gds_baseaddr := sharemem_size.U + 4.U

  val cnt=Counter(16)
  io.host2cta.bits.host_wg_id:=Cat(cnt.value + 3.U,0.U(CU_ID_WIDTH.W))
  when(cnt.value < num_of_block){
    io.host2cta.valid:=true.B
    when(io.host2cta.ready){cnt.inc()}
  }
  io.cta2host.ready:=true.B
  when(io.cta2host.valid){
    printf(p"finish a wg ${io.cta2host.bits.inflight_wg_buffer_host_wf_done_wg_id}\n")
  }
  //io.cta2host<>DontCare
}

class TopForTest_CTA extends Module {
  val io = IO(new Bundle{
  })
  val param=(new MyConfig).toInstance
  val CPU = Module(new CPUtest)
  val cta = Module(new CTAinterface)
  val sm=VecInit(Seq.fill(NUMBER_CU)(Module(new SM_wrapper).io))
  val inst_filepath="./txt/example_vid_sum.vmem"
  val data_filepath="./txt/single_read.data"
  val mem = Module(new L2ModelWithName(inst_filepath,data_filepath,5)(param))
  val sm2l2model = Module(new SM2L2ModelArbiter()(param))
  for (i <- 0 until NUMBER_CU){
    cta.io.CTA2warp(i)<>sm(i).CTAreq
    cta.io.warp2CTA(i)<>sm(i).CTArsp
    sm(i).memReq<>sm2l2model.io.memReqVecIn(i)
    sm(i).memRsp<>sm2l2model.io.memRspVecOut(i)
  }
  sm2l2model.io.memReqOut<>mem.io.memReq
  sm2l2model.io.memRspIn<>mem.io.memRsp
  mem.io.memReq_ready:=true.B
  CPU.io.cta2host<>cta.io.CTA2host
  CPU.io.host2cta<>cta.io.host2CTA
}
class CPUtest_SingleSM extends Module{
  val io=IO(new Bundle{
    val host2sm=Decoupled(new CTAreqData)
    val sm2host=Flipped(Valid(new CTArspData))
  })
  val num_of_warp = 8.U                                  // num_of_warp for your task
  io.host2sm.valid:=false.B
  io.host2sm.bits.dispatch2cu_wg_wf_count        := num_of_warp
  io.host2sm.bits.dispatch2cu_wf_size_dispatch   := num_thread.U
  io.host2sm.bits.dispatch2cu_sgpr_base_dispatch := 0.U
  io.host2sm.bits.dispatch2cu_vgpr_base_dispatch := 0.U
  io.host2sm.bits.dispatch2cu_wf_tag_dispatch    := 0.U
  io.host2sm.bits.dispatch2cu_lds_base_dispatch  := 0.U
  io.host2sm.bits.dispatch2cu_start_pc_dispatch  := 0.U //base addr for this task
  io.host2sm.bits.dispatch2cu_gds_base_dispatch := sharemem_size.U
  val cnt=Counter(16)
  //wf_tag:WG_SLOT_ID_WIDTH + WF_COUNT_WIDTH_PER_WG
  //block id(max num in a warp) + warp_in_block id，
  io.host2sm.bits.dispatch2cu_wf_tag_dispatch:=cnt.value
  when(cnt.value < num_of_warp){
    io.host2sm.valid:=true.B
    when(io.host2sm.ready){cnt.inc()}
  }

  when(io.sm2host.valid){
    printf(p"${io.sm2host.bits.cu2dispatch_wf_tag_done}\n")
  }
  //io.cta2host<>DontCare
}
class TopForTest_SingleSM extends Module {
  val io = IO(new Bundle{})

  val param=(new MyConfig).toInstance
  val CPU = Module(new CPUtest_SingleSM)
  val inst_filepath="./txt/example_vid_sum.vmem"
  val data_filepath="./txt/gaussian.data"
  val NUMBER_CU=1
  val sm=VecInit(Seq.fill(NUMBER_CU)(Module(new SM_wrapper).io))
  val mem = Module(new L2ModelWithName(inst_filepath,data_filepath,5)(param))
  val sm2l2model = Module(new SM2L2ModelArbiter()(param))
  for (i <- 0 until NUMBER_CU){
  sm(i).memReq<>sm2l2model.io.memReqVecIn(i)
  sm(i).memRsp<>sm2l2model.io.memRspVecOut(i)
}
  sm(0).CTAreq<>CPU.io.host2sm
  CPU.io.sm2host.bits:=sm(0).CTArsp.bits
  CPU.io.sm2host.valid:=sm(0).CTArsp.valid
  sm(0).CTArsp.ready:=true.B
  sm2l2model.io.memReqVecIn(1)<>DontCare
  sm2l2model.io.memRspVecOut(1)<>DontCare
  sm2l2model.io.memReqOut<>mem.io.memReq
  sm2l2model.io.memRspIn<>mem.io.memRsp
  mem.io.memReq_ready:=true.B
}

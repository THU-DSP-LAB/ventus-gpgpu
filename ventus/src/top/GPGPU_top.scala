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
package top

import chisel3._
import chisel3.util._
import chisel3.util.experimental.loadMemoryFromFile
import parameters._
import L1Cache.ICache._
import L1Cache._
import L1Cache.DCache._
import L1Cache.ShareMem._
import config.config._
import pipeline._
import L2cache._
import CTA._
import axi._
import freechips.rocketchip.amba.axi4._

class host2CTA_data extends Bundle{
  val host_wg_id            = (UInt(WG_ID_WIDTH.W))
  val host_num_wf           = (UInt(WF_COUNT_WIDTH.W))
  val host_wf_size          = (UInt(WAVE_ITEM_WIDTH.W))
  val host_start_pc         = (UInt(MEM_ADDR_WIDTH.W))
  val host_kernel_size_3d   = Vec(3, UInt(WG_SIZE_X_WIDTH.W))
  val host_pds_baseaddr     = (UInt(MEM_ADDR_WIDTH.W))
  val host_csr_knl          = (UInt(MEM_ADDR_WIDTH.W))
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
  cta_sche.io.host_kernel_size_3d   := io.host2CTA.bits.host_kernel_size_3d
  cta_sche.io.host_pds_baseaddr     := io.host2CTA.bits.host_pds_baseaddr
  cta_sche.io.host_csr_knl          := io.host2CTA.bits.host_csr_knl
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
    io.CTA2warp(i).bits.dispatch2cu_gds_base_dispatch := cta_sche.io.dispatch2cu_gds_base_dispatch
    io.CTA2warp(i).bits.dispatch2cu_pds_base_dispatch := cta_sche.io.dispatch2cu_pds_baseaddr_dispatch
    io.CTA2warp(i).bits.dispatch2cu_csr_knl_dispatch := cta_sche.io.dispatch2cu_csr_knl_dispatch
    io.CTA2warp(i).bits.dispatch2cu_wgid_x_dispatch := cta_sche.io.dispatch2cu_kernel_size_3d_dispatch(0)
    io.CTA2warp(i).bits.dispatch2cu_wgid_y_dispatch := cta_sche.io.dispatch2cu_kernel_size_3d_dispatch(1)
    io.CTA2warp(i).bits.dispatch2cu_wgid_z_dispatch := cta_sche.io.dispatch2cu_kernel_size_3d_dispatch(2)
    io.CTA2warp(i).bits.dispatch2cu_wg_id := 0.U
    cta_sche.io.cu2dispatch_ready_for_dispatch(i):=io.CTA2warp(i).ready

    cta_sche.io.cu2dispatch_wf_tag_done(i):=io.warp2CTA(i).bits.cu2dispatch_wf_tag_done
    cu2dispatch_wf_done(i):=io.warp2CTA(i).valid
    io.warp2CTA(i).ready:=true.B
  }
  cta_sche.io.cu2dispatch_wf_done:=cu2dispatch_wf_done.asUInt

  //val sm=VecInit(Seq.fill(NUMBER_CU)(Module(new SM_wrapper).io))
  //for (i <- 0 until NUMBER_CU){}
}
class GPGPU_axi_top extends Module{
  val l2cache_axi_params=AXI4BundleParameters(32,64,log2Up(l2cache_micro.num_SM)+log2Up(l2cache_micro.num_warp)+1)
  val l2_axi_params=InclusiveCacheParameters_lite_withAXI(l2cache_params,l2cache_axi_params)

  val io=IO(new Bundle{
    val s=Flipped(new AXI4Lite(32, 32))
    val m=(new AXI4Bundle(l2cache_axi_params))
  })
  val l1param = (new MyConfig).toInstance

  val gpgpu_top=Module(new GPGPU_top()(l1param))
  val axi_lite_adapter=Module(new AXI4Lite2CTA(32,32))
  val axi_adapter=Module(new AXI4Adapter(l2_axi_params))
  axi_lite_adapter.io.ctl<>io.s
  axi_adapter.io.AXI_master_bundle<>io.m
  gpgpu_top.io.out_a<>axi_adapter.io.l2cache_outa
  gpgpu_top.io.out_d<>axi_adapter.io.l2cache_outd
  gpgpu_top.io.host_req<>axi_lite_adapter.io.data
  gpgpu_top.io.host_rsp<>axi_lite_adapter.io.rsp
}
class GPGPU_axi_adapter_top extends Module{
  val l2cache_axi_params=AXI4BundleParameters(32,64,log2Up(l2cache_micro.num_SM)+log2Up(l2cache_micro.num_warp)+1)
  val l2_axi_params=InclusiveCacheParameters_lite_withAXI(l2cache_params,l2cache_axi_params)
  val io=IO(new Bundle{
    val s=Flipped(new AXI4Lite(32, 32))
    val m=(new AXI4Bundle(l2cache_axi_params))
  })
  val gpgpu_axi_top=Module(new GPGPU_axi_top)
  io.s<>gpgpu_axi_top.io.s
  io.m<>gpgpu_axi_top.io.m
}

class GPGPU_top(implicit p: Parameters, FakeCache: Boolean = false) extends RVGModule{
    val io = IO(new Bundle{
    val host_req=Flipped(DecoupledIO(new host2CTA_data))
    val host_rsp=DecoupledIO(new CTA2host_data)
    val out_a =Decoupled(new TLBundleA_lite(l2cache_params))
    val out_d=Flipped(Decoupled(new TLBundleD_lite(l2cache_params)))
  })
  val cta = Module(new CTAinterface)
  val sm_wrapper=VecInit(Seq.fill(NSms)(Module(new SM_wrapper(FakeCache)).io))
  val l2cache= if(FakeCache) Module(new FakeL2Cache(l2cache_params)) else Module(new Scheduler(l2cache_params))
  val sm2L2Arb = Module(new SM2L2Arbiter(l2cache_params))

  for (i<- 0 until NSms) {
    cta.io.CTA2warp(i)<>sm_wrapper(i).CTAreq
    cta.io.warp2CTA(i)<>sm_wrapper(i).CTArsp
    sm_wrapper(i).memRsp <> sm2L2Arb.io.memRspVecOut(i)
    sm2L2Arb.io.memReqVecIn(i) <> sm_wrapper(i).memReq
  }
  l2cache.io.flush.bits := DontCare
  l2cache.io.flush.valid := false.B
  l2cache.io.invalidate.bits := DontCare
  l2cache.io.invalidate.valid := false.B
  l2cache.io.in_a <> sm2L2Arb.io.memReqOut
  l2cache.io.out_a <> io.out_a
  l2cache.io.out_d <> io.out_d
  sm2L2Arb.io.memRspIn <> l2cache.io.in_d
  io.host_rsp<>cta.io.CTA2host
  io.host_req<>cta.io.host2CTA
}

class SM_wrapper(FakeCache: Boolean = false) extends Module{
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
  icache.io.coreReq.bits.mask:=pipe.io.icache_req.bits.mask
  // ***********************
  // **** icache coreRsp ****
  pipe.io.icache_rsp.valid:=icache.io.coreRsp.valid
  pipe.io.icache_rsp.bits.warpid:=icache.io.coreRsp.bits.warpid
  pipe.io.icache_rsp.bits.data:=icache.io.coreRsp.bits.data
  pipe.io.icache_rsp.bits.addr:=icache.io.coreRsp.bits.addr
  pipe.io.icache_rsp.bits.status:=icache.io.coreRsp.bits.status
  pipe.io.icache_rsp.bits.mask:=icache.io.coreRsp.bits.mask
  icache.io.coreRsp.ready:=pipe.io.icache_rsp.ready
  // ***********************
  icache.io.externalFlushPipe.bits.warpid :=pipe.io.externalFlushPipe.bits
  icache.io.externalFlushPipe.valid :=pipe.io.externalFlushPipe.valid

  val dcache = if(FakeCache) Module(new FakeL1DCache()(param)) else Module(new DataCache()(param))
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

class SM2L2ArbiterIO(L2param: InclusiveCacheParameters_lite)(implicit p: Parameters) extends RVGBundle{
  val memReqVecIn = Flipped(Vec(NSms, Decoupled(new L1CacheMemReq())))
  val memReqOut = Decoupled(new TLBundleA_lite(L2param))
  val memRspIn = Flipped(Decoupled(new TLBundleD_lite_plus(L2param)))
  val memRspVecOut = Vec(NSms, Decoupled(new L1CacheMemRsp()))
}


class SM2L2Arbiter(L2param: InclusiveCacheParameters_lite)(implicit p: Parameters) extends RVGModule {
  val io = IO(new SM2L2ArbiterIO(L2param)(p))

  // **** memReq ****
  val memReqArb = Module(new Arbiter(new TLBundleA_lite(L2param),NSms))
  //memReqArb.io.in <> io.memReqVecIn
  for(i <- 0 until NSms) {
    memReqArb.io.in(i).bits.opcode := io.memReqVecIn(i).bits.a_opcode
    memReqArb.io.in(i).bits.source := Cat(i.asUInt,io.memReqVecIn(i).bits.a_source)
    memReqArb.io.in(i).bits.address := io.memReqVecIn(i).bits.a_addr
    memReqArb.io.in(i).bits.mask := (io.memReqVecIn(i).bits.a_mask).asUInt
    memReqArb.io.in(i).bits.data := io.memReqVecIn(i).bits.a_data.asUInt
    memReqArb.io.in(i).bits.size := 0.U//log2Up(BlockWords*BytesOfWord).U
    memReqArb.io.in(i).valid := io.memReqVecIn(i).valid
    io.memReqVecIn(i).ready:=memReqArb.io.in(i).ready
  }
  io.memReqOut <> memReqArb.io.out
  // ****************

  // **** memRsp ****
  for(i <- 0 until NSms) {
    io.memRspVecOut(i).bits.d_data:=io.memRspIn.bits.data.asTypeOf(Vec(dcache_BlockWords,UInt(32.W)))
    io.memRspVecOut(i).bits.d_source:=io.memRspIn.bits.source
    io.memRspVecOut(i).bits.d_addr:=io.memRspIn.bits.address
    io.memRspVecOut(i).valid :=
      io.memRspIn.bits.source(log2Up(NSms)+log2Up(NCacheInSM)+WIdBits-1,WIdBits+log2Up(NCacheInSM))===i.asUInt && io.memRspIn.valid
  }
  io.memRspIn.ready := Mux1H(UIntToOH(io.memRspIn.bits.source(log2Up(NSms)+log2Up(NCacheInSM)+WIdBits-1,WIdBits+log2Up(NCacheInSM))),
    Reverse(Cat(io.memRspVecOut.map(_.ready))))//TODO check order in test
  // ****************
}
class TestCase(val name: String, inst: String, data: String, warp: Int, thread: Int, start_pc: Int, cycles: Int){
  //val props = ("./txt/" + name + "/" + inst, "./txt/" + name + "/" + data, warp, thread, start_pc))
  class Props{
    val inst_filepath = "./ventus/txt/" + name + "/" + inst
    val data_filepath = "./ventus/txt/" + name + "/" + data
    val num_warp = TestCase.this.warp
    val num_thread = TestCase.this.thread
    val start_pc = TestCase.this.start_pc
    val cycles = TestCase.this.cycles
  }
  val props = new Props
}
class CPUtest(C: TestCase#Props) extends Module{
  val io=IO(new Bundle{
    val host2cta=Decoupled(new host2CTA_data)
    val cta2host=Flipped(Decoupled(new CTA2host_data))
  })
  val num_of_block = 1.U
  io.host2cta.valid:=false.B
  io.host2cta.bits.host_wg_id:=0.U
  io.host2cta.bits.host_num_wf:=C.num_warp.U
  io.host2cta.bits.host_wf_size:=num_thread.asUInt()
  io.host2cta.bits.host_start_pc:=0.U // start pc
  io.host2cta.bits.host_vgpr_size_total:= (C.num_warp*32).U
  io.host2cta.bits.host_sgpr_size_total:= (C.num_warp*32).U
  io.host2cta.bits.host_lds_size_total:= 128.U
  io.host2cta.bits.host_gds_size_total:= 128.U
  io.host2cta.bits.host_vgpr_size_per_wf:=32.U
  io.host2cta.bits.host_sgpr_size_per_wf:=32.U
  io.host2cta.bits.host_gds_baseaddr := sharemem_size.U
  io.host2cta.bits.host_pds_baseaddr := sharemem_size.U
  io.host2cta.bits.host_csr_knl:=10.U
  io.host2cta.bits.host_kernel_size_3d:=0.U.asTypeOf(io.host2cta.bits.host_kernel_size_3d)

  val cnt=Counter(16)
  io.host2cta.bits.host_wg_id:=Cat(cnt.value + 3.U,0.U(CU_ID_WIDTH.W))
  //io.host2cta.bits.host_pds_baseaddr:=cnt.value << 10
  io.host2cta.bits.host_csr_knl:=cnt.value
  io.host2cta.bits.host_kernel_size_3d:=VecInit(Seq(cnt.value,cnt.value+1.U,cnt.value+2.U))
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
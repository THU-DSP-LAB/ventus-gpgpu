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

import chisel3._
import chisel3.util._
import top.parameters._

class CTAreqData extends Bundle{
  val dispatch2cu_wg_wf_count        = UInt(log2Ceil(CTA_SCHE_CONFIG.WG.NUM_WF_MAX+1).W)      // num of WF in this WG
  val dispatch2cu_wf_size_dispatch   = UInt(log2Ceil(CTA_SCHE_CONFIG.GPU.NUM_THREAD+1).W)     // num of thread in this WF
  val dispatch2cu_sgpr_base_dispatch = UInt(log2Ceil(CTA_SCHE_CONFIG.WG.NUM_SGPR_MAX+1).W)    // sGPR base addr of this WF
  val dispatch2cu_vgpr_base_dispatch = UInt(log2Ceil(CTA_SCHE_CONFIG.WG.NUM_VGPR_MAX+1).W)    // vGPR base addr of this WF
  val dispatch2cu_lds_base_dispatch  = UInt(log2Ceil(CTA_SCHE_CONFIG.WG.NUM_LDS_MAX+1).W)     // LDS  base addr of this WF
  val dispatch2cu_wf_tag_dispatch    = UInt(CTA_SCHE_CONFIG.WG.WF_TAG_WIDTH)    // WF tag = cat(wg_slot_id_in_cu, wf_id_in_wg)
  val dispatch2cu_start_pc_dispatch  = UInt(CTA_SCHE_CONFIG.GPU.MEM_ADDR_WIDTH) // Where program starts, 0x80000000
  val dispatch2cu_pds_base_dispatch  = UInt(CTA_SCHE_CONFIG.GPU.MEM_ADDR_WIDTH)
  val dispatch2cu_gds_base_dispatch  = UInt(CTA_SCHE_CONFIG.GPU.MEM_ADDR_WIDTH)
  val dispatch2cu_csr_knl_dispatch   = UInt(CTA_SCHE_CONFIG.GPU.MEM_ADDR_WIDTH)
  val dispatch2cu_wgid_x_dispatch    = UInt(log2Ceil(CTA_SCHE_CONFIG.KERNEL.NUM_WG_MAX+1).W)
  val dispatch2cu_wgid_y_dispatch    = UInt(log2Ceil(CTA_SCHE_CONFIG.KERNEL.NUM_WG_MAX+1).W)
  val dispatch2cu_wgid_z_dispatch    = UInt(log2Ceil(CTA_SCHE_CONFIG.KERNEL.NUM_WG_MAX+1).W)
  val dispatch2cu_wg_id              = UInt(CTA_SCHE_CONFIG.WG.WG_ID_WIDTH)
  val dispatch2cu_knl_asid           = if(CTA_SCHE_CONFIG.GPU.MMU_ENABLE) Some(UInt(CTA_SCHE_CONFIG.GPU.ASID_WIDTH)) else None
  val dispatch2cu_threadIdx_local_x  = Vec(CTA_SCHE_CONFIG.GPU.NUM_THREAD, UInt(log2Ceil(CTA_SCHE_CONFIG.WG.NUM_THREAD_PER_WG_MAX).W))
  val dispatch2cu_threadIdx_local_y  = Vec(CTA_SCHE_CONFIG.GPU.NUM_THREAD, UInt(log2Ceil(CTA_SCHE_CONFIG.WG.NUM_THREAD_PER_WG_MAX).W))
  val dispatch2cu_threadIdx_local_z  = Vec(CTA_SCHE_CONFIG.GPU.NUM_THREAD, UInt(log2Ceil(CTA_SCHE_CONFIG.WG.NUM_THREAD_PER_WG_MAX).W))
  val dispatch2cu_threadIdx_global_x = Vec(CTA_SCHE_CONFIG.GPU.NUM_THREAD, UInt(log2Ceil(CTA_SCHE_CONFIG.KERNEL.NUM_THREAD_PER_KNL_MAX).W))
  val dispatch2cu_threadIdx_global_y = Vec(CTA_SCHE_CONFIG.GPU.NUM_THREAD, UInt(log2Ceil(CTA_SCHE_CONFIG.KERNEL.NUM_THREAD_PER_KNL_MAX).W))
  val dispatch2cu_threadIdx_global_z = Vec(CTA_SCHE_CONFIG.GPU.NUM_THREAD, UInt(log2Ceil(CTA_SCHE_CONFIG.KERNEL.NUM_THREAD_PER_KNL_MAX).W))
  val dispatch2cu_threadIdx_global_linear = Vec(CTA_SCHE_CONFIG.GPU.NUM_THREAD, UInt(log2Ceil(CTA_SCHE_CONFIG.KERNEL.NUM_THREAD_PER_KNL_MAX).W))
}
class CTArspData extends Bundle{
  val cu2dispatch_wf_tag_done = UInt(CTA_SCHE_CONFIG.WG.WF_TAG_WIDTH)
}
class warpReqData extends Bundle{
  val CTAdata = new CTAreqData
  val wid = UInt(depth_warp.W)
}
class warpRspData extends Bundle{
  val wid = UInt(depth_warp.W)
}

class CTA2warp extends Module{
  val io=IO(new Bundle{
    val CTAreq=Flipped(Decoupled(new CTAreqData))
    val CTArsp=Decoupled(new CTArspData)
    val warpReq=Decoupled(new warpReqData)    // to warp scheduler
    val warpRsp=Flipped(Decoupled(new warpRspData))
    val wg_id_lookup=Input(UInt(depth_warp.W))
    val wg_id_tag=Output(UInt(TAG_WIDTH.W))
  })
  val idx_using = RegInit(0.U(num_warp.W))  // current active warps in sm

  val data = RegInit(VecInit(Seq.fill(num_warp)(0.U(TAG_WIDTH.W)))) // every hw_warp record its wg&wf id
  io.wg_id_tag:=data(io.wg_id_lookup)
  val has_free_slot = !idx_using.andR
  val idx_next_allocate = PriorityEncoder(~idx_using)

  io.warpReq.valid := io.CTAreq.valid && has_free_slot
  io.warpReq.bits.CTAdata := io.CTAreq.bits
  io.warpReq.bits.wid := idx_next_allocate
  io.CTAreq.ready := has_free_slot && io.warpReq.ready

  val launch_fire = io.warpReq.fire
  val alloc_mask = (1.U << idx_next_allocate).asUInt & Fill(num_warp, launch_fire)
  val free_mask = (1.U << io.warpRsp.bits.wid).asUInt & Fill(num_warp, io.warpRsp.fire)
  idx_using := (idx_using | alloc_mask) & (~free_mask).asUInt
  when(launch_fire) {
    data(idx_next_allocate):=io.CTAreq.bits.dispatch2cu_wf_tag_dispatch
  }

  // TODO: Fix warp_scheduler warpRsp IO logic, which always requires ready=1
  // WorkAround: warp_scheduler requires io.wrapRsp.ready=1, use a large enough FIFO to satisfy it temporarily
  val CTArsp_fifo_enq = Wire(Decoupled(new CTArspData))
  CTArsp_fifo_enq.valid := io.warpRsp.valid
  CTArsp_fifo_enq.bits.cu2dispatch_wf_tag_done := data(io.warpRsp.bits.wid)
  io.warpRsp.ready := CTArsp_fifo_enq.ready
  val CTArsp_fifo = Queue(CTArsp_fifo_enq, 16)
  assert(io.warpRsp.ready, "warpRsp port requires ready=1, this FIFO is used to satisfy it, but not enough")

  CTArsp_fifo.ready := io.CTArsp.ready
  io.CTArsp.bits := CTArsp_fifo.bits
  io.CTArsp.valid := CTArsp_fifo.valid
}

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
  val dispatch2cu_start_pc_dispatch  = UInt(CTA_SCHE_CONFIG.GPU.MEM_ADDR_WIDTH)
  val dispatch2cu_pds_base_dispatch  = UInt(CTA_SCHE_CONFIG.GPU.MEM_ADDR_WIDTH)
  val dispatch2cu_gds_base_dispatch  = UInt(CTA_SCHE_CONFIG.GPU.MEM_ADDR_WIDTH)
  val dispatch2cu_csr_knl_dispatch   = UInt(CTA_SCHE_CONFIG.GPU.MEM_ADDR_WIDTH)
  val dispatch2cu_wgid_x_dispatch    = UInt(log2Ceil(CTA_SCHE_CONFIG.KERNEL.NUM_WG_MAX+1).W)
  val dispatch2cu_wgid_y_dispatch    = UInt(log2Ceil(CTA_SCHE_CONFIG.KERNEL.NUM_WG_MAX+1).W)
  val dispatch2cu_wgid_z_dispatch    = UInt(log2Ceil(CTA_SCHE_CONFIG.KERNEL.NUM_WG_MAX+1).W)
  val dispatch2cu_wg_id              = UInt(CTA_SCHE_CONFIG.WG.WG_ID_WIDTH)
  val dispatch2cu_knl_asid           = if(CTA_SCHE_CONFIG.GPU.MMU_ENABLE) Some(UInt(CTA_SCHE_CONFIG.GPU.ASID_WIDTH)) else None
  val dispatch2cu_global_id          = Vec(CTA_SCHE_CONFIG.WG.NUM_THREAD_MAX,UInt(CTA_SCHE_CONFIG.GPU.MEM_ADDR_WIDTH))
  val dispatch2cu_local_id           = Vec(CTA_SCHE_CONFIG.WG.NUM_THREAD_MAX,UInt(CTA_SCHE_CONFIG.GPU.MEM_ADDR_WIDTH))
  val dispatch2cu_global_linear_id   = Vec(CTA_SCHE_CONFIG.WG.NUM_THREAD_MAX,UInt(CTA_SCHE_CONFIG.GPU.MEM_ADDR_WIDTH))
  val dispatch2cu_local_linear_id    = Vec(CTA_SCHE_CONFIG.WG.NUM_THREAD_MAX,UInt(CTA_SCHE_CONFIG.GPU.MEM_ADDR_WIDTH))
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

  io.CTAreq.ready:=(~idx_using.andR)
  val data = Reg(Vec(num_warp,UInt(TAG_WIDTH.W))) // every hw_warp record its wg&wf id
  io.wg_id_tag:=data(io.wg_id_lookup)
  val idx_next_allocate = PriorityEncoder(~idx_using)
  //idx_using:=Mux(io.warpRsp.fire&io.CTAreq.fire,idx_using&(~(1.U<<io.warpRsp.bits.wid)).asUInt&(1.U<<idx_next_allocate).asUInt,
  //  Mux(io.warpRsp.fire,idx_using&(~(1.U<<io.warpRsp.bits.wid)).asUInt,
  //  Mux(io.CTAreq.fire,idx_using&(1.U<<idx_next_allocate).asUInt,idx_using)))
  idx_using:=(idx_using | ((1.U<<idx_next_allocate).asUInt & Fill(num_warp,io.CTAreq.fire))) & (~((Fill(num_warp,io.warpRsp.fire)).asUInt & ((1.U<<io.warpRsp.bits.wid)).asUInt)).asUInt
  when(io.CTAreq.fire) {
    data(idx_next_allocate):=io.CTAreq.bits.dispatch2cu_wf_tag_dispatch
  }
  io.warpReq.valid:=io.CTAreq.fire
  io.warpReq.bits.CTAdata:=io.CTAreq.bits
  io.warpReq.bits.wid:=idx_next_allocate

  // TODO: Fix warp_scheduler warpRsp IO logic, which always requires ready=1
  // WorkAround: warp_scheduler requires io.wrapRsp.ready=1, use a large enough FIFO to satisfy it temporarily
  val CTArsp_fifo = Queue(io.warpRsp, 16)
  assert(io.warpRsp.ready, "warpRsp port requires ready=1, this FIFO is used to satisfy it, but not enough")

  CTArsp_fifo.ready := io.CTArsp.ready
  io.CTArsp.bits.cu2dispatch_wf_tag_done := data(CTArsp_fifo.bits.wid)
  io.CTArsp.valid := CTArsp_fifo.valid
}

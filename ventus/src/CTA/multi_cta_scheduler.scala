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
package CTA

import chisel3._

class multi_cta_scheduler(val NUM_SCHEDULER: Int, val NUMBER_CU: Int, val CU_ID_WIDTH: Int, val RES_TABLE_ADDR_WIDTH: Int, val VGPR_ID_WIDTH: Int, val NUMBER_VGPR_SLOTS: Int, val SGPR_ID_WIDTH: Int, val NUMBER_SGPR_SLOTS: Int, val LDS_ID_WIDTH: Int, val NUMBER_LDS_SLOTS: Int, val WG_ID_WIDTH: Int, val WF_COUNT_WIDTH: Int, val WG_SLOT_ID_WIDTH: Int, val NUMBER_WF_SLOTS: Int, val WF_COUNT_MAX: Int, val NUMBER_RES_TABLE: Int, val GDS_ID_WIDTH: Int, val GDS_SIZE: Int, val ENTRY_ADDR_WIDTH: Int, val NUMBER_ENTRIES: Int, val WAVE_ITEM_WIDTH: Int, val MEM_ADDR_WIDTH: Int, val TAG_WIDTH: Int, val INIT_MAX_WG_COUNT: Int, val NUM_SCHEDULER_WIDTH: Int, val WF_COUNT_WIDTH_PER_WG: Int) extends Module{
    val io = IO(new Bundle{
        val host_wg_valid = Input(Bool())
        val host_wg_id = Input(UInt(WG_ID_WIDTH.W))
        val host_num_wf = Input(UInt(WF_COUNT_WIDTH_PER_WG.W))
        val host_wf_size = Input(UInt(WAVE_ITEM_WIDTH.W))
        val host_start_pc = Input(UInt(MEM_ADDR_WIDTH.W))
        val host_vgpr_size_total = Input(UInt((VGPR_ID_WIDTH + 1).W))
        val host_sgpr_size_total = Input(UInt((SGPR_ID_WIDTH + 1).W))
        val host_lds_size_total = Input(UInt((LDS_ID_WIDTH + 1).W))
        val host_gds_size_total = Input(UInt((GDS_ID_WIDTH + 1).W))
        val host_vgpr_size_per_wf = Input(UInt((VGPR_ID_WIDTH + 1).W))
        val host_sgpr_size_per_wf = Input(UInt((SGPR_ID_WIDTH + 1).W))

        val host_gds_baseaddr = Input(UInt(MEM_ADDR_WIDTH.W))
        val host_kernel_size_3d = Input(Vec(3, UInt(top.parameters.WG_SIZE_X_WIDTH.W)))
        val host_csr_knl = Input(UInt(MEM_ADDR_WIDTH.W))
        val host_pds_baseaddr = Input(UInt(MEM_ADDR_WIDTH.W))

        val inflight_wg_buffer_host_rcvd_ack = Output(Bool())
        val inflight_wg_buffer_host_wf_done = Vec(NUM_SCHEDULER, Output(Bool()))
        val inflight_wg_buffer_host_wf_done_wg_id = Vec(NUM_SCHEDULER, Output(UInt(WG_ID_WIDTH.W)))

        val dispatch2cu_wf_dispatch = Vec(NUM_SCHEDULER, Output(UInt(NUMBER_CU.W)))
        val dispatch2cu_wg_wf_count = Vec(NUM_SCHEDULER, Output(UInt(WF_COUNT_WIDTH_PER_WG.W)))
        val dispatch2cu_wf_size_dispatch = Vec(NUM_SCHEDULER, Output(UInt(WAVE_ITEM_WIDTH.W)))
        val dispatch2cu_sgpr_base_dispatch = Vec(NUM_SCHEDULER, Output(UInt((SGPR_ID_WIDTH + 1).W)))
        val dispatch2cu_vgpr_base_dispatch = Vec(NUM_SCHEDULER, Output(UInt((VGPR_ID_WIDTH + 1).W)))
        val dispatch2cu_wf_tag_dispatch = Vec(NUM_SCHEDULER, Output(UInt(TAG_WIDTH.W)))
        val dispatch2cu_lds_base_dispatch = Vec(NUM_SCHEDULER, Output(UInt((LDS_ID_WIDTH + 1).W)))
        val dispatch2cu_start_pc_dispatch = Vec(NUM_SCHEDULER, Output(UInt(MEM_ADDR_WIDTH.W)))

        val dispatch2cu_kernel_size_3d_dispatch = Vec(NUM_SCHEDULER, Output(Vec(3, UInt(top.parameters.WG_SIZE_X_WIDTH.W))))
        val dispatch2cu_pds_baseaddr_dispatch = Vec(NUM_SCHEDULER, Output(UInt(MEM_ADDR_WIDTH.W)))
        val dispatch2cu_csr_knl_dispatch = Vec(NUM_SCHEDULER, Output(UInt(MEM_ADDR_WIDTH.W)))
        val dispatch2cu_gds_base_dispatch = Vec(NUM_SCHEDULER, Output(UInt(MEM_ADDR_WIDTH.W)))

        val cu2dispatch_wf_done = Vec(NUM_SCHEDULER, Input(UInt(NUMBER_CU.W)))
        val cu2dispatch_wf_tag_done = Vec(NUM_SCHEDULER, Vec(NUMBER_CU, Input(UInt(TAG_WIDTH.W))))
        val cu2dispatch_ready_for_dispatch = Vec(NUM_SCHEDULER, Vec(NUMBER_CU, Input(Bool())))
    })
    val interface = Module(new multi_cta_scheduler_interface(WG_ID_WIDTH, WF_COUNT_WIDTH, WAVE_ITEM_WIDTH, MEM_ADDR_WIDTH, VGPR_ID_WIDTH, SGPR_ID_WIDTH, LDS_ID_WIDTH, GDS_ID_WIDTH, NUM_SCHEDULER, NUM_SCHEDULER_WIDTH, WF_COUNT_WIDTH_PER_WG))
    interface.io.host_wg_valid := io.host_wg_valid
    interface.io.host_wg_id := io.host_wg_id
    interface.io.host_num_wf := io.host_num_wf
    interface.io.host_wf_size := io.host_wf_size
    interface.io.host_start_pc := io.host_start_pc
    interface.io.host_vgpr_size_total := io.host_vgpr_size_total
    interface.io.host_sgpr_size_total := io.host_sgpr_size_total
    interface.io.host_lds_size_total := io.host_lds_size_total
    interface.io.host_gds_size_total := io.host_gds_size_total
    interface.io.host_vgpr_size_per_wf := io.host_vgpr_size_per_wf
    interface.io.host_sgpr_size_per_wf := io.host_sgpr_size_per_wf

    interface.io.host_csr_knl := io.host_csr_knl
    interface.io.host_kernel_size_3d := io.host_kernel_size_3d
    interface.io.host_pds_baseaddr := io.host_pds_baseaddr
    interface.io.host_gds_baseaddr := io.host_gds_baseaddr

    io.inflight_wg_buffer_host_rcvd_ack := interface.io.inflight_wg_buffer_host_rcvd_ack
    io.inflight_wg_buffer_host_wf_done := interface.io.inflight_wg_buffer_host_wf_done
    io.inflight_wg_buffer_host_wf_done_wg_id := interface.io.inflight_wg_buffer_host_wf_done_wg_id
    for(i <- 0 until NUM_SCHEDULER){
        val scheduler = Module(new cta_scheduler(NUMBER_CU, CU_ID_WIDTH, RES_TABLE_ADDR_WIDTH, VGPR_ID_WIDTH, NUMBER_VGPR_SLOTS, SGPR_ID_WIDTH, NUMBER_SGPR_SLOTS, LDS_ID_WIDTH, NUMBER_LDS_SLOTS, WG_ID_WIDTH, WF_COUNT_WIDTH, WG_SLOT_ID_WIDTH, NUMBER_WF_SLOTS, WF_COUNT_MAX, NUMBER_RES_TABLE, GDS_ID_WIDTH, GDS_SIZE, ENTRY_ADDR_WIDTH, NUMBER_ENTRIES, WAVE_ITEM_WIDTH, MEM_ADDR_WIDTH, TAG_WIDTH, INIT_MAX_WG_COUNT, WF_COUNT_WIDTH_PER_WG))
        scheduler.io.host_wg_valid := interface.io.host_wg_valid_s(i)
        scheduler.io.host_wg_id := interface.io.host_wg_id_s(i)
        scheduler.io.host_num_wf := interface.io.host_num_wf_s(i)
        scheduler.io.host_wf_size := interface.io.host_wf_size_s(i)
        scheduler.io.host_start_pc := interface.io.host_start_pc_s(i)
        scheduler.io.host_vgpr_size_total := interface.io.host_vgpr_size_total_s(i)
        scheduler.io.host_sgpr_size_total := interface.io.host_sgpr_size_total_s(i)
        scheduler.io.host_lds_size_total := interface.io.host_lds_size_total_s(i)
        scheduler.io.host_gds_size_total := interface.io.host_gds_size_total_s(i)
        scheduler.io.host_vgpr_size_per_wf := interface.io.host_vgpr_size_per_wf_s(i)
        scheduler.io.host_sgpr_size_per_wf := interface.io.host_sgpr_size_per_wf_s(i)

        scheduler.io.host_csr_knl := interface.io.host_csr_knl_s(i)
        scheduler.io.host_kernel_size_3d := interface.io.host_kernel_size_3d_s(i)
        scheduler.io.host_pds_baseaddr := interface.io.host_pds_baseaddr_s(i)
        scheduler.io.host_gds_baseaddr := interface.io.host_gds_baseaddr_s(i)

        interface.io.inflight_wg_buffer_scheduler_rcvd_ack_s(i) := scheduler.io.inflight_wg_buffer_host_rcvd_ack
        interface.io.inflight_wg_buffer_scheduler_wf_done_s(i) := scheduler.io.inflight_wg_buffer_host_wf_done
        interface.io.inflight_wg_buffer_scheduler_wf_done_wg_id_s(i) := scheduler.io.inflight_wg_buffer_host_wf_done_wg_id

        io.dispatch2cu_wf_dispatch(i) := scheduler.io.dispatch2cu_wf_dispatch
        io.dispatch2cu_wg_wf_count(i) := scheduler.io.dispatch2cu_wg_wf_count
        io.dispatch2cu_wf_size_dispatch(i) := scheduler.io.dispatch2cu_wf_size_dispatch
        io.dispatch2cu_sgpr_base_dispatch(i) := scheduler.io.dispatch2cu_sgpr_base_dispatch
        io.dispatch2cu_vgpr_base_dispatch(i) := scheduler.io.dispatch2cu_vgpr_base_dispatch
        io.dispatch2cu_wf_tag_dispatch(i) := scheduler.io.dispatch2cu_wf_tag_dispatch
        io.dispatch2cu_lds_base_dispatch(i) := scheduler.io.dispatch2cu_lds_base_dispatch
        io.dispatch2cu_start_pc_dispatch(i) := scheduler.io.dispatch2cu_start_pc_dispatch

        io.dispatch2cu_csr_knl_dispatch(i) := scheduler.io.dispatch2cu_csr_knl_dispatch
        io.dispatch2cu_kernel_size_3d_dispatch(i) := scheduler.io.dispatch2cu_kernel_size_3d_dispatch
        io.dispatch2cu_pds_baseaddr_dispatch(i) := scheduler.io.dispatch2cu_pds_baseaddr_dispatch
        io.dispatch2cu_gds_base_dispatch(i) := scheduler.io.dispatch2cu_gds_base_dispatch

        scheduler.io.cu2dispatch_wf_done := io.cu2dispatch_wf_done(i)
        scheduler.io.cu2dispatch_wf_tag_done := io.cu2dispatch_wf_tag_done(i)
        scheduler.io.cu2dispatch_ready_for_dispatch := io.cu2dispatch_ready_for_dispatch(i)
    }
}
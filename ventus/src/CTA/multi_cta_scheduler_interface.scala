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
import chisel3.util._
class multi_cta_scheduler_interface(val WG_ID_WIDTH: Int, val WF_COUNT_WIDTH: Int, val WAVE_ITEM_WIDTH: Int, val MEM_ADDR_WIDTH: Int, val VGPR_ID_WIDTH: Int, val SGPR_ID_WIDTH: Int, val LDS_ID_WIDTH: Int, val GDS_ID_WIDTH: Int, val NUM_SCHEDULER: Int, val NUM_SCHEDULER_WIDTH: Int, val WF_COUNT_WIDTH_PER_WG: Int) extends Module {
    val io = IO(new Bundle{
        //Interface to the host
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
        val inflight_wg_buffer_host_rcvd_ack = Output(Bool())
        val inflight_wg_buffer_host_wf_done = Vec(NUM_SCHEDULER, Output(Bool()))
        val inflight_wg_buffer_host_wf_done_wg_id = Vec(NUM_SCHEDULER, Output(UInt(WG_ID_WIDTH.W)))
        //Interface to the scheduler
        val host_wg_valid_s = Vec(NUM_SCHEDULER, Output(Bool()))
        val host_wg_id_s = Vec(NUM_SCHEDULER, Output(UInt(WG_ID_WIDTH.W)))
        val host_num_wf_s = Vec(NUM_SCHEDULER, Output(UInt(WF_COUNT_WIDTH_PER_WG.W)))
        val host_wf_size_s = Vec(NUM_SCHEDULER, Output(UInt(WAVE_ITEM_WIDTH.W)))
        val host_start_pc_s = Vec(NUM_SCHEDULER, Output(UInt(MEM_ADDR_WIDTH.W)))
        val host_vgpr_size_total_s = Vec(NUM_SCHEDULER, Output(UInt((VGPR_ID_WIDTH + 1).W)))
        val host_sgpr_size_total_s = Vec(NUM_SCHEDULER, Output(UInt((SGPR_ID_WIDTH + 1).W)))
        val host_lds_size_total_s = Vec(NUM_SCHEDULER, Output(UInt((LDS_ID_WIDTH + 1).W)))
        val host_gds_size_total_s = Vec(NUM_SCHEDULER, Output(UInt((GDS_ID_WIDTH + 1).W)))
        val host_vgpr_size_per_wf_s = Vec(NUM_SCHEDULER, Output(UInt((VGPR_ID_WIDTH + 1).W)))
        val host_sgpr_size_per_wf_s = Vec(NUM_SCHEDULER, Output(UInt((SGPR_ID_WIDTH + 1).W)))
        val inflight_wg_buffer_scheduler_rcvd_ack_s = Vec(NUM_SCHEDULER, Input(Bool()))
        val inflight_wg_buffer_scheduler_wf_done_s = Vec(NUM_SCHEDULER, Input(Bool()))
        val inflight_wg_buffer_scheduler_wf_done_wg_id_s = Vec(NUM_SCHEDULER, Input(UInt(WG_ID_WIDTH.W)))
    })
    //wg done signals to host
    for(i <- 0 until NUM_SCHEDULER){
        io.inflight_wg_buffer_host_wf_done(i) := io.inflight_wg_buffer_scheduler_wf_done_s(i)
        io.inflight_wg_buffer_host_wf_done_wg_id(i) := io.inflight_wg_buffer_scheduler_wf_done_wg_id_s(i)
    }
    val STATE_WIDTH = 2
    val INTERFACE_IDLE = 0.U(STATE_WIDTH.W)
    val INTERFACE_SENDING = 1.U(STATE_WIDTH.W)
    val INTERFACE_ACK = 2.U(STATE_WIDTH.W)
    val interface_state = RegInit(INTERFACE_IDLE)
    val inflight_wg_buffer_host_rcvd_ack_i = RegInit(false.B)
    io.inflight_wg_buffer_host_rcvd_ack := inflight_wg_buffer_host_rcvd_ack_i
    val rcvd_array = RegInit(VecInit(Seq.fill(NUM_SCHEDULER)(false.B)))
    for(i <- 0 until NUM_SCHEDULER){
        when(io.inflight_wg_buffer_scheduler_rcvd_ack_s(i)){
            rcvd_array(i) := true.B
        }
    }
    val host_wg_valid_s_i = RegInit(VecInit(Seq.fill(NUM_SCHEDULER)(false.B)))
    val sche_init = RegInit(VecInit(Seq.fill(NUM_SCHEDULER)(false.B)))
    val found = Wire(Bool())
    val found_id = Wire(UInt(NUM_SCHEDULER_WIDTH.W))
    found := false.B
    found_id := 0.U
    for(i <- 0 until NUM_SCHEDULER){
        when(rcvd_array(i) || !sche_init(i)){
            found := true.B
            found_id := i.U
            host_wg_valid_s_i(i) := false.B
        }
    }
    val select_id = RegInit(0.U(NUM_SCHEDULER_WIDTH.W))
    select_id := found_id
    val select_id_valid = RegInit(false.B)
    select_id_valid := found
    val host_wg_id_i = RegInit(0.U(WG_ID_WIDTH.W))
    val host_num_wf_i = RegInit(0.U(WF_COUNT_WIDTH_PER_WG.W))
    val host_wf_size_i = RegInit(0.U(WAVE_ITEM_WIDTH.W))
    val host_start_pc_i = RegInit(0.U(MEM_ADDR_WIDTH.W))
    val host_vgpr_size_total_i = RegInit(0.U((VGPR_ID_WIDTH + 1).W))
    val host_sgpr_size_total_i = RegInit(0.U((SGPR_ID_WIDTH + 1).W))
    val host_lds_size_total_i = RegInit(0.U((LDS_ID_WIDTH + 1).W))
    val host_gds_size_total_i = RegInit(0.U((GDS_ID_WIDTH + 1).W))
    val host_vgpr_size_per_wf_i = RegInit(0.U((VGPR_ID_WIDTH + 1).W))
    val host_sgpr_size_per_wf_i = RegInit(0.U((SGPR_ID_WIDTH + 1).W))
    for(i <- 0 until NUM_SCHEDULER){
        io.host_wg_valid_s(i) := host_wg_valid_s_i(i)
    }
    val host_wg_id_s_i = RegInit(VecInit(Seq.fill(NUM_SCHEDULER){0.U(WG_ID_WIDTH.W)}))
    for(i <- 0 until NUM_SCHEDULER){
        io.host_wg_id_s(i) := host_wg_id_s_i(i)
    }
    val host_num_wf_s_i = RegInit(VecInit(Seq.fill(NUM_SCHEDULER){0.U(WF_COUNT_WIDTH_PER_WG.W)}))
    for(i <- 0 until NUM_SCHEDULER){
        io.host_num_wf_s(i) := host_num_wf_s_i(i)
    }
    val host_wf_size_s_i = RegInit(VecInit(Seq.fill(NUM_SCHEDULER){0.U(WAVE_ITEM_WIDTH.W)}))
    for(i <- 0 until NUM_SCHEDULER){
        io.host_wf_size_s(i) := host_wf_size_s_i(i)
    }
    val host_start_pc_s_i = RegInit(VecInit(Seq.fill(NUM_SCHEDULER){0.U(MEM_ADDR_WIDTH.W)}))
    for(i <- 0 until NUM_SCHEDULER){
        io.host_start_pc_s(i) := host_start_pc_s_i(i)
    }
    val host_vgpr_size_total_s_i = RegInit(VecInit(Seq.fill(NUM_SCHEDULER){0.U((VGPR_ID_WIDTH + 1).W)}))
    for(i <- 0 until NUM_SCHEDULER){
        io.host_vgpr_size_total_s(i) := host_vgpr_size_total_s_i(i)
    }
    val host_sgpr_size_total_s_i = RegInit(VecInit(Seq.fill(NUM_SCHEDULER){0.U((SGPR_ID_WIDTH + 1).W)}))
    for(i <- 0 until NUM_SCHEDULER){
        io.host_sgpr_size_total_s(i) := host_sgpr_size_total_s_i(i)
    }
    val host_lds_size_total_s_i = RegInit(VecInit(Seq.fill(NUM_SCHEDULER){0.U((LDS_ID_WIDTH + 1).W)}))
    for(i <- 0 until NUM_SCHEDULER){
        io.host_lds_size_total_s(i) := host_lds_size_total_s_i(i)
    }
    val host_gds_size_total_s_i = RegInit(VecInit(Seq.fill(NUM_SCHEDULER){0.U((GDS_ID_WIDTH + 1).W)}))
    for(i <- 0 until NUM_SCHEDULER){
        io.host_gds_size_total_s(i) := host_gds_size_total_s_i(i)
    }
    val host_vgpr_size_per_wf_s_i = RegInit(VecInit(Seq.fill(NUM_SCHEDULER){0.U((VGPR_ID_WIDTH + 1).W)}))
    for(i <- 0 until NUM_SCHEDULER){
        io.host_vgpr_size_per_wf_s(i) := host_vgpr_size_per_wf_s_i(i)
    }
    val host_sgpr_size_per_wf_s_i = RegInit(VecInit(Seq.fill(NUM_SCHEDULER){0.U((SGPR_ID_WIDTH + 1).W)}))
    for(i <- 0 until NUM_SCHEDULER){
        io.host_sgpr_size_per_wf_s(i) := host_sgpr_size_per_wf_s_i(i)
    }
    switch(interface_state){
        is(INTERFACE_IDLE){
            when(io.host_wg_valid){
                inflight_wg_buffer_host_rcvd_ack_i := false.B
                host_wg_id_i := io.host_wg_id
                host_num_wf_i := io.host_num_wf
                host_wf_size_i := io.host_wf_size
                host_start_pc_i := io.host_start_pc
                host_vgpr_size_total_i := io.host_vgpr_size_total
                host_sgpr_size_total_i := io.host_sgpr_size_total
                host_lds_size_total_i := io.host_lds_size_total
                host_gds_size_total_i := io.host_gds_size_total
                host_vgpr_size_per_wf_i := io.host_vgpr_size_per_wf
                host_sgpr_size_per_wf_i := io.host_sgpr_size_per_wf
                interface_state := INTERFACE_SENDING
            }
        }
        is(INTERFACE_SENDING){
            when(select_id_valid){
                sche_init(select_id) := true.B
                rcvd_array(select_id) := false.B
                host_wg_valid_s_i(select_id) := true.B
                host_wg_id_s_i(select_id) := host_wg_id_i
                host_num_wf_s_i(select_id) := host_num_wf_i 
                host_wf_size_s_i(select_id) := host_wf_size_i
                host_start_pc_s_i(select_id) := host_start_pc_i
                host_vgpr_size_total_s_i(select_id) := host_vgpr_size_total_i
                host_sgpr_size_total_s_i(select_id) := host_sgpr_size_total_i
                host_lds_size_total_s_i(select_id) := host_lds_size_total_i
                host_gds_size_total_s_i(select_id) := host_gds_size_total_i
                host_vgpr_size_per_wf_s_i(select_id) := host_vgpr_size_per_wf_i
                host_sgpr_size_per_wf_s_i(select_id) := host_sgpr_size_per_wf_i
                interface_state := INTERFACE_ACK
            }
        }
        is(INTERFACE_ACK){
            //host_wg_valid_s_i := 0.U
            when(found){
                inflight_wg_buffer_host_rcvd_ack_i := true.B
                interface_state := INTERFACE_IDLE
            }
        }
    }
}
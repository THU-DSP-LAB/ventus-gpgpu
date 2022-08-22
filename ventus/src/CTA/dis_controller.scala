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

//?? = require further check (bit width, etc.)

class dis_controller(val NUMBER_CU: Int, val CU_ID_WIDTH: Int, val RES_TABLE_ADDR_WIDTH: Int) extends Module{
    val NUMBER_RES_TABLE = 1 << RES_TABLE_ADDR_WIDTH
    val CU_PER_RES_TABLE = NUMBER_CU / NUMBER_RES_TABLE
    val ALLOC_NUM_STATES = 4
    val ST_AL_IDLE = 0
    val ST_AL_ALLOC = 2
    val ST_AL_HANDLE_RESULT = 4
    val ST_AL_ACK_PROPAGATION = 8
    val io = IO(new Bundle{
        //Output
        val dis_controller_start_alloc = Output(Bool())
        val dis_controller_alloc_ack = Output(Bool())
        val dis_controller_wg_alloc_valid = Output(Bool())
        val dis_controller_wg_dealloc_valid = Output(Bool())
        val dis_controller_wg_rejected_valid = Output(Bool())
        val dis_controller_cu_busy = Output(UInt(NUMBER_CU.W))
        //Input
        val inflight_wg_buffer_alloc_valid = Input(Bool())
        val inflight_wg_buffer_alloc_available = Input(Bool())
        val allocator_cu_valid = Input(Bool())
        val allocator_cu_rejected = Input(Bool())
        val allocator_cu_id_out = Input(UInt(CU_ID_WIDTH.W))
        val grt_wg_alloc_done = Input(Bool())
        val grt_wg_dealloc_done = Input(Bool())
        val grt_wg_alloc_cu_id = Input(UInt(CU_ID_WIDTH.W))
        val grt_wg_dealloc_cu_id = Input(UInt(CU_ID_WIDTH.W))
        val gpu_interface_alloc_available = Input(Bool())
        val gpu_interface_dealloc_available = Input(Bool())
        val gpu_interface_cu_id = Input(UInt(CU_ID_WIDTH.W))
    })
    val alloc_st = RegInit(ST_AL_IDLE.U(ALLOC_NUM_STATES.W))
    val cus_allocating = WireInit(VecInit(Seq.fill(NUMBER_CU)(false.B)))
    val cu_groups_allocating = RegInit(VecInit(Seq.fill(NUMBER_RES_TABLE)(false.B)))
    val alloc_waiting_cu_id = RegInit(0.U(CU_ID_WIDTH.W))
    val alloc_waiting = RegInit(false.B)
    val dis_controller_start_alloc_i = RegInit(false.B)
    val dis_controller_alloc_ack_i = RegInit(false.B)
    val dis_controller_wg_alloc_valid_i = RegInit(false.B)
    val dis_controller_wg_dealloc_valid_i = RegInit(false.B)
    val dis_controller_wg_rejected_valid_i = RegInit(false.B)
    io.dis_controller_start_alloc := dis_controller_start_alloc_i
    io.dis_controller_alloc_ack := dis_controller_alloc_ack_i
    io.dis_controller_wg_alloc_valid := dis_controller_wg_alloc_valid_i
    io.dis_controller_wg_dealloc_valid := dis_controller_wg_dealloc_valid_i
    io.dis_controller_wg_rejected_valid := dis_controller_wg_rejected_valid_i
    val gpu_interface_cu_res_tbl_addr = Wire(UInt(RES_TABLE_ADDR_WIDTH.W))
    gpu_interface_cu_res_tbl_addr := io.gpu_interface_cu_id(CU_ID_WIDTH - 1, CU_ID_WIDTH - RES_TABLE_ADDR_WIDTH)


    dis_controller_start_alloc_i := false.B
    dis_controller_alloc_ack_i := false.B
    // State Machine 1
    // Waits until allocator input can be handled by the grt to acknowledge a allocated wg
    switch(alloc_st){
        is(ST_AL_IDLE.U){
            when(io.inflight_wg_buffer_alloc_valid && cu_groups_allocating.contains(false.B)){
                dis_controller_start_alloc_i := true.B
                alloc_st := ST_AL_ALLOC.U
            }
        }
        is(ST_AL_ALLOC.U){
            when(io.allocator_cu_valid){
                alloc_waiting := true.B
                alloc_waiting_cu_id := io.allocator_cu_id_out
                alloc_st := ST_AL_HANDLE_RESULT.U
            }
        }
        is(ST_AL_HANDLE_RESULT.U){
            when(!alloc_waiting){
                dis_controller_alloc_ack_i := true.B
                alloc_st := ST_AL_ACK_PROPAGATION.U
            }
        }
        is(ST_AL_ACK_PROPAGATION.U){
            alloc_st := ST_AL_IDLE.U
        }
    }

    // Handles the grt
	// Deallocations are always handled first
    dis_controller_wg_dealloc_valid_i := false.B
	dis_controller_wg_alloc_valid_i := false.B
	dis_controller_wg_rejected_valid_i := false.B
    when(io.gpu_interface_dealloc_available && !cu_groups_allocating(gpu_interface_cu_res_tbl_addr)){
        dis_controller_wg_dealloc_valid_i := true.B
        cu_groups_allocating(gpu_interface_cu_res_tbl_addr) := true.B
    }
    .elsewhen(alloc_waiting && !cu_groups_allocating(alloc_waiting_cu_id(CU_ID_WIDTH - 1, CU_ID_WIDTH - RES_TABLE_ADDR_WIDTH))){
        when(io.allocator_cu_rejected){
            alloc_waiting := false.B
            dis_controller_wg_rejected_valid_i := true.B
        }
        .elsewhen(io.gpu_interface_alloc_available && io.inflight_wg_buffer_alloc_available){
            alloc_waiting := false.B
            dis_controller_wg_alloc_valid_i := true.B
            cu_groups_allocating(alloc_waiting_cu_id(CU_ID_WIDTH - 1, CU_ID_WIDTH - RES_TABLE_ADDR_WIDTH)) := true.B
        }
    }

    when(io.grt_wg_alloc_done){
        cu_groups_allocating(io.grt_wg_alloc_cu_id(CU_ID_WIDTH - 1, CU_ID_WIDTH - RES_TABLE_ADDR_WIDTH)) := false.B
    }
    .elsewhen(io.grt_wg_dealloc_done){
        cu_groups_allocating(io.grt_wg_dealloc_cu_id(CU_ID_WIDTH - 1, CU_ID_WIDTH - RES_TABLE_ADDR_WIDTH)) := false.B
    }

    for(i <- 0 until NUMBER_CU){
        cus_allocating(i) := cu_groups_allocating(i.U >> (CU_ID_WIDTH - RES_TABLE_ADDR_WIDTH))
    }
    io.dis_controller_cu_busy := cus_allocating.asUInt
}
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

//?? Require discussion
class gpu_interface(val WG_ID_WIDTH: Int, val WF_COUNT_WIDTH: Int, val WG_SLOT_ID_WIDTH: Int, val NUMBER_WF_SLOTS: Int, val NUMBER_CU: Int, val CU_ID_WIDTH: Int, val VGPR_ID_WIDTH: Int, val SGPR_ID_WIDTH: Int, val LDS_ID_WIDTH: Int, val TAG_WIDTH: Int, val MEM_ADDR_WIDTH: Int, val WAVE_ITEM_WIDTH: Int, val WF_COUNT_WIDTH_PER_WG: Int) extends Module{
    val io = IO(new Bundle{
        val inflight_wg_buffer_gpu_valid = Input(Bool())
        val inflight_wg_buffer_gpu_wf_size = Input(UInt(WAVE_ITEM_WIDTH.W))
        val inflight_wg_buffer_start_pc = Input(UInt(MEM_ADDR_WIDTH.W))
        val inflight_wg_buffer_kernel_size_3d = Input(Vec(3, UInt(top.parameters.WG_SIZE_X_WIDTH.W)))
        val inflight_wg_buffer_pds_baseaddr = Input(UInt(MEM_ADDR_WIDTH.W))
        val inflight_wg_buffer_csr_knl = Input(UInt(MEM_ADDR_WIDTH.W))
        val inflight_wg_buffer_gds_base_dispatch = Input(UInt(MEM_ADDR_WIDTH.W))
        val inflight_wg_buffer_gpu_vgpr_size_per_wf = Input(UInt(VGPR_ID_WIDTH.W))
        val inflight_wg_buffer_gpu_sgpr_size_per_wf = Input(UInt(SGPR_ID_WIDTH.W))

        val allocator_wg_id_out = Input(UInt(WG_ID_WIDTH.W))
        val allocator_cu_id_out = Input(UInt(CU_ID_WIDTH.W))
        val allocator_wf_count = Input(UInt(WF_COUNT_WIDTH_PER_WG.W))
        val allocator_vgpr_start_out = Input(UInt(VGPR_ID_WIDTH.W))
        val allocator_sgpr_start_out = Input(UInt(SGPR_ID_WIDTH.W))
        val allocator_lds_start_out = Input(UInt(LDS_ID_WIDTH.W))

        val dis_controller_wg_alloc_valid = Input(Bool())
        val dis_controller_wg_dealloc_valid = Input(Bool())

        val gpu_interface_alloc_available = Output(Bool())
        val gpu_interface_dealloc_available = Output(Bool())
        val gpu_interface_cu_id = Output(UInt(CU_ID_WIDTH.W))
        val gpu_interface_dealloc_wg_id = Output(UInt(WG_ID_WIDTH.W))

        val dispatch2cu_wf_dispatch = Output(UInt(NUMBER_CU.W))
        val dispatch2cu_wg_wf_count = Output(UInt(WF_COUNT_WIDTH_PER_WG.W))
        val dispatch2cu_wf_size_dispatch = Output(UInt(WAVE_ITEM_WIDTH.W))
        val dispatch2cu_sgpr_base_dispatch = Output(UInt((SGPR_ID_WIDTH + 1).W))
        val dispatch2cu_vgpr_base_dispatch = Output(UInt((VGPR_ID_WIDTH + 1).W))
        val dispatch2cu_wf_tag_dispatch = Output(UInt(TAG_WIDTH.W))
        val dispatch2cu_lds_base_dispatch = Output(UInt((LDS_ID_WIDTH + 1).W))
        val dispatch2cu_start_pc_dispatch = Output(UInt(MEM_ADDR_WIDTH.W))
        val dispatch2cu_kernel_size_3d_dispatch = Output(Vec(3, UInt(top.parameters.WG_SIZE_X_WIDTH.W)))
        val dispatch2cu_pds_baseaddr_dispatch = Output(UInt(MEM_ADDR_WIDTH.W))
        val dispatch2cu_csr_knl_dispatch = Output(UInt(MEM_ADDR_WIDTH.W))
        val dispatch2cu_gds_base_dispatch = Output(UInt(MEM_ADDR_WIDTH.W))

        val cu2dispatch_wf_done = Input(UInt(NUMBER_CU.W))
        val cu2dispatch_wf_tag_done = Vec(NUMBER_CU, Input(UInt(TAG_WIDTH.W)))
        val cu2dispatch_ready_for_dispatch = Vec(NUMBER_CU, Input(Bool()))
    })
    // Incomming finished wf -> increment finished count until all wf retire
    // Communicate back to utd
    // Deallocation registers
    val gpu_interface_dealloc_available_i = RegInit(false.B)
    val dis_controller_wg_dealloc_valid_i = RegInit(false.B)
    val handler_wg_done_ack = RegInit(VecInit(Seq.fill(NUMBER_CU){false.B}))
    val chosen_done_cu_valid = RegInit(false.B)
    val chosen_done_cu_valid_comb = Wire(Bool())
    val chosen_done_cu_id = RegInit(0.U(CU_ID_WIDTH.W))
    val chosen_done_cu_id_comb = Wire(UInt(CU_ID_WIDTH.W))
    val handler_wg_done_valid = RegInit(VecInit(Seq.fill(NUMBER_CU){false.B}))
    val handler_wg_done_wg_id = RegInit(VecInit(Seq.fill(NUMBER_CU){0.U(WG_ID_WIDTH.W)}))
    val cu2dispatch_wf_done_i = RegInit(VecInit(Seq.fill(NUMBER_CU){false.B}))
    val cu2dispatch_wf_tag_done_i = RegInit(VecInit(Seq.fill(NUMBER_CU){0.U(TAG_WIDTH.W)}))

    val NUM_DEALLOC_ST = 5
    val ST_DEALLOC_IDLE = 1<<0
    val ST_DEALLOC_WAIT_ACK = 1<<1

    val dealloc_st = RegInit(ST_DEALLOC_IDLE.U(NUM_DEALLOC_ST.W))

    // Incomming alloc wg -> get them a tag (find a free slot on the vector),
    // store wgid and wf count,
    // disparch them, one wf at a time
    // Allocation registters
    val dis_controller_wg_alloc_valid_i = RegInit(false.B)
    val inflight_wg_buffer_gpu_valid_i = RegInit(false.B)
    val inflight_wg_buffer_gpu_wf_size_i = RegInit(0.U(WAVE_ITEM_WIDTH.W))
    val inflight_wg_buffer_start_pc_i = RegInit(0.U(MEM_ADDR_WIDTH.W))
    val inflight_wg_buffer_kernel_size_3d_i = RegInit(VecInit(Seq.fill(3)(0.U(top.parameters.WG_SIZE_X_WIDTH.W))))
    val inflight_wg_buffer_pds_baseaddr_i = RegInit(0.U(MEM_ADDR_WIDTH.W))
    val inflight_wg_buffer_csr_knl = RegInit(0.U(MEM_ADDR_WIDTH.W))
    val inflight_wg_buffer_gds_base_dispatch_i = RegInit(0.U(MEM_ADDR_WIDTH.W))
    val inflight_wg_buffer_gpu_vgpr_size_per_wf_i = RegInit(0.U(VGPR_ID_WIDTH.W))
    val inflight_wg_buffer_gpu_sgpr_size_per_wf_i = RegInit(0.U(SGPR_ID_WIDTH.W))

    val allocator_wg_id_out_i = RegInit(0.U(WG_ID_WIDTH.W))
    val allocator_cu_id_out_i = RegInit(0.U(CU_ID_WIDTH.W))
    val allocator_wf_count_i = RegInit(0.U(WF_COUNT_WIDTH_PER_WG.W))
    val allocator_vgpr_start_out_i = RegInit(0.U(VGPR_ID_WIDTH.W))
    val allocator_sgpr_start_out_i = RegInit(0.U(SGPR_ID_WIDTH.W))
    val allocator_lds_start_out_i = RegInit(0.U(LDS_ID_WIDTH.W))

    val gpu_interface_alloc_available_i = RegInit(false.B)
    val gpu_interface_cu_id_i = RegInit(0.U(CU_ID_WIDTH.W))
    val gpu_interface_dealloc_wg_id_i = RegInit(0.U(WG_ID_WIDTH.W))

    val dispatch2cu_wf_dispatch_handlers = RegInit(VecInit(Seq.fill(NUMBER_CU)(false.B)))
    val invalid_due_to_not_ready_handlers = RegInit(VecInit(Seq.fill(NUMBER_CU)(false.B)))
    val dispatch2cu_wf_tag_dispatch_handlers = RegInit(VecInit(Seq.fill(NUMBER_CU)(0.U(TAG_WIDTH.W))))

    val handler_wg_alloc_en = RegInit(VecInit(Seq.fill(NUMBER_CU)(false.B)))
    val handler_wg_alloc_wg_id = RegInit(VecInit(Seq.fill(NUMBER_CU)(0.U(WG_ID_WIDTH.W))))
    val handler_wg_alloc_wf_count = RegInit(VecInit(Seq.fill(NUMBER_CU)(0.U(WF_COUNT_WIDTH_PER_WG.W))))

    val dispatch2cu_wf_dispatch_i = RegInit(0.U(NUMBER_CU.W))
    val dispatch2cu_wg_wf_count_i = RegInit(0.U(WF_COUNT_WIDTH_PER_WG.W))
    val dispatch2cu_wf_size_dispatch_i = RegInit(0.U(WAVE_ITEM_WIDTH.W))
    val dispatch2cu_sgpr_base_dispatch_i = RegInit(0.U((SGPR_ID_WIDTH + 1).W))
    val dispatch2cu_vgpr_base_dispatch_i = RegInit(0.U((VGPR_ID_WIDTH + 1).W))
    val dispatch2cu_wf_tag_dispatch_i = RegInit(0.U(TAG_WIDTH.W))
    val dispatch2cu_lds_base_dispatch_i = RegInit(0.U((LDS_ID_WIDTH + 1).W))
    val dispatch2cu_start_pc_dispatch_i = RegInit(0.U(MEM_ADDR_WIDTH.W))
    val dispatch2cu_kernel_size_3d_dispatch_i = RegInit(VecInit(Seq.fill(3)(0.U(top.parameters.WG_SIZE_X_WIDTH.W))))
    val dispatch2cu_pds_baseaddr_dispatch_i = RegInit(0.U(MEM_ADDR_WIDTH.W))
    val dispatch2cu_csr_knl_dispatch_i = RegInit(0.U(MEM_ADDR_WIDTH.W))
    val dispatch2cu_gds_base_dispatch_i = RegInit(0.U(MEM_ADDR_WIDTH.W))

    val NUM_ALLOC_ST = 4
    val ST_ALLOC_IDLE = 1<<0
    val ST_ALLOC_WAIT_BUFFER = 1<<1
    val ST_ALLOC_WAIT_HANDLER = 1<<2
    val ST_ALLOC_PASS_WF = 1<<3
    val alloc_st = RegInit(ST_ALLOC_IDLE.U(NUM_ALLOC_ST.W))
    //val handler_ready_for_dispatch2cu = RegInit(VecInit(Seq.fill(NUMBER_CU){false.B}))
    //handler_ready_for_dispatch2cu := io.cu2dispatch_ready_for_dispatch
    for(i <- 0 until NUMBER_CU){
        val cu_handler = Module(new cu_handler(WF_COUNT_WIDTH, WG_ID_WIDTH, WG_SLOT_ID_WIDTH, NUMBER_WF_SLOTS, TAG_WIDTH, WF_COUNT_WIDTH_PER_WG))
        cu_handler.io.wg_alloc_en := handler_wg_alloc_en(i)
        cu_handler.io.wg_alloc_wg_id := handler_wg_alloc_wg_id(i)
        cu_handler.io.wg_alloc_wf_count := handler_wg_alloc_wf_count(i)
        cu_handler.io.cu2dispatch_wf_done_i := cu2dispatch_wf_done_i(i)
        cu_handler.io.cu2dispatch_wf_tag_done_i := cu2dispatch_wf_tag_done_i(i)
        cu_handler.io.wg_done_ack := handler_wg_done_ack(i)
        cu_handler.io.ready_for_dispatch2cu := io.cu2dispatch_ready_for_dispatch(i)
        dispatch2cu_wf_dispatch_handlers(i) := cu_handler.io.dispatch2cu_wf_dispatch
        dispatch2cu_wf_tag_dispatch_handlers(i) := cu_handler.io.dispatch2cu_wf_tag_dispatch
        handler_wg_done_valid(i) := cu_handler.io.wg_done_valid
        handler_wg_done_wg_id(i) := cu_handler.io.wg_done_wg_id
        invalid_due_to_not_ready_handlers(i) := cu_handler.io.invalid_due_to_not_ready
    }
    dis_controller_wg_alloc_valid_i := io.dis_controller_wg_alloc_valid
    inflight_wg_buffer_gpu_valid_i := io.inflight_wg_buffer_gpu_valid
    when(io.inflight_wg_buffer_gpu_valid){
        inflight_wg_buffer_gpu_wf_size_i := io.inflight_wg_buffer_gpu_wf_size
        inflight_wg_buffer_start_pc_i := io.inflight_wg_buffer_start_pc
        inflight_wg_buffer_kernel_size_3d_i := io.inflight_wg_buffer_kernel_size_3d
        inflight_wg_buffer_pds_baseaddr_i := io.inflight_wg_buffer_pds_baseaddr
        inflight_wg_buffer_csr_knl := io.inflight_wg_buffer_csr_knl
        inflight_wg_buffer_gds_base_dispatch_i := io.inflight_wg_buffer_gds_base_dispatch
        inflight_wg_buffer_gpu_vgpr_size_per_wf_i := io.inflight_wg_buffer_gpu_vgpr_size_per_wf
        inflight_wg_buffer_gpu_sgpr_size_per_wf_i := io.inflight_wg_buffer_gpu_sgpr_size_per_wf
    }
    when(io.dis_controller_wg_alloc_valid){
        allocator_wg_id_out_i := io.allocator_wg_id_out
        allocator_cu_id_out_i := io.allocator_cu_id_out
        allocator_wf_count_i := io.allocator_wf_count
        allocator_vgpr_start_out_i := io.allocator_vgpr_start_out
        allocator_sgpr_start_out_i := io.allocator_sgpr_start_out
        allocator_lds_start_out_i := io.allocator_lds_start_out
    }
	// On allocation
	// Receives wg_id, waits for sizes per wf
	// Pass values to cu_handler -> for each dispatch in 1, pass one wf to cus
	// after passing, sets itself as available again  
    handler_wg_alloc_en := (0.U(NUMBER_CU.W)).asBools
    dispatch2cu_wf_dispatch_i := (0.U(NUMBER_CU.W))
    //allocation state machine
    switch(alloc_st){
        is(ST_ALLOC_IDLE.U){
            gpu_interface_alloc_available_i := true.B
            when(dis_controller_wg_alloc_valid_i){
                when(inflight_wg_buffer_gpu_valid_i){
                    handler_wg_alloc_en(allocator_cu_id_out_i) := true.B
                    handler_wg_alloc_wg_id(allocator_cu_id_out_i) := allocator_wg_id_out_i
                    handler_wg_alloc_wf_count(allocator_cu_id_out_i) := allocator_wf_count_i
                    gpu_interface_alloc_available_i := false.B
                    alloc_st := ST_ALLOC_WAIT_HANDLER.U
                }
                .otherwise{
                    gpu_interface_alloc_available_i := false.B
                    alloc_st := ST_ALLOC_WAIT_BUFFER.U
                }
            }
        }
        is(ST_ALLOC_WAIT_BUFFER.U){
            when(inflight_wg_buffer_gpu_valid_i){
                handler_wg_alloc_en(allocator_cu_id_out_i) := true.B
                handler_wg_alloc_wg_id(allocator_cu_id_out_i) := allocator_wg_id_out_i
                handler_wg_alloc_wf_count(allocator_cu_id_out_i) := allocator_wf_count_i
                gpu_interface_alloc_available_i := false.B
                alloc_st := ST_ALLOC_WAIT_HANDLER.U
            }
        }
        is(ST_ALLOC_WAIT_HANDLER.U){
            when(dispatch2cu_wf_dispatch_handlers(allocator_cu_id_out_i)){
                dispatch2cu_wf_dispatch_i := 1.U << allocator_cu_id_out_i
                dispatch2cu_wf_tag_dispatch_i := dispatch2cu_wf_tag_dispatch_handlers(allocator_cu_id_out_i)
                dispatch2cu_wg_wf_count_i := allocator_wf_count_i
                //?? Maybe _i The source code doesn't use _i
                dispatch2cu_wf_size_dispatch_i := inflight_wg_buffer_gpu_wf_size_i
                dispatch2cu_start_pc_dispatch_i := inflight_wg_buffer_start_pc_i
                dispatch2cu_kernel_size_3d_dispatch_i := inflight_wg_buffer_kernel_size_3d_i
                dispatch2cu_pds_baseaddr_dispatch_i := inflight_wg_buffer_pds_baseaddr_i
                dispatch2cu_csr_knl_dispatch_i := inflight_wg_buffer_csr_knl
                dispatch2cu_gds_base_dispatch_i := inflight_wg_buffer_gds_base_dispatch_i
                dispatch2cu_vgpr_base_dispatch_i := allocator_vgpr_start_out_i
                dispatch2cu_sgpr_base_dispatch_i := allocator_sgpr_start_out_i
                dispatch2cu_lds_base_dispatch_i := allocator_lds_start_out_i
                alloc_st := ST_ALLOC_PASS_WF.U
            }
        }
        is(ST_ALLOC_PASS_WF.U){
            when(dispatch2cu_wf_dispatch_handlers(allocator_cu_id_out_i)){
                dispatch2cu_wf_dispatch_i := 1.U << allocator_cu_id_out_i
                dispatch2cu_wf_tag_dispatch_i := dispatch2cu_wf_tag_dispatch_handlers(allocator_cu_id_out_i)
                dispatch2cu_sgpr_base_dispatch_i := dispatch2cu_sgpr_base_dispatch_i + inflight_wg_buffer_gpu_sgpr_size_per_wf_i
                dispatch2cu_vgpr_base_dispatch_i := dispatch2cu_vgpr_base_dispatch_i + inflight_wg_buffer_gpu_vgpr_size_per_wf_i
            }
            .elsewhen(invalid_due_to_not_ready_handlers(allocator_cu_id_out_i)){
                dispatch2cu_wf_dispatch_i := 0.U
            }
            .otherwise{
                gpu_interface_alloc_available_i := true.B
                alloc_st := ST_ALLOC_IDLE.U
            }
        }
    }
    cu2dispatch_wf_done_i := io.cu2dispatch_wf_done.asBools

    for(i <- 0 until NUMBER_CU){
        cu2dispatch_wf_tag_done_i(i) := io.cu2dispatch_wf_tag_done(i)
    }

	// On dealloc
	// Ack to the handler
	// pass info to the dispatcher
    dis_controller_wg_dealloc_valid_i := io.dis_controller_wg_dealloc_valid
    chosen_done_cu_valid := chosen_done_cu_valid_comb
    chosen_done_cu_id := chosen_done_cu_id_comb

    gpu_interface_dealloc_available_i := false.B
    handler_wg_done_ack := (0.U(NUMBER_CU.W)).asBools

    //deallocation state machine
    switch(dealloc_st){
        is(ST_DEALLOC_IDLE.U){
            when(chosen_done_cu_valid){
                gpu_interface_dealloc_available_i := true.B
                gpu_interface_cu_id_i := chosen_done_cu_id
                handler_wg_done_ack(chosen_done_cu_id) := true.B
                gpu_interface_dealloc_wg_id_i := handler_wg_done_wg_id(chosen_done_cu_id)
                dealloc_st := ST_DEALLOC_WAIT_ACK.U
            }
        }
        is(ST_DEALLOC_WAIT_ACK.U){
            when(dis_controller_wg_dealloc_valid_i){
                dealloc_st := ST_DEALLOC_IDLE.U
            }
            .otherwise{
                gpu_interface_dealloc_available_i := true.B
            }
        }
    }

    val cu_found_valid = Wire(Bool())
    val cu_found = Wire(UInt(CU_ID_WIDTH.W))
    cu_found_valid := false.B
    cu_found := 0.U
    for(i <- NUMBER_CU - 1 to 0 by -1){
        when(handler_wg_done_valid(i)){
            cu_found_valid := true.B
            cu_found := i.U
        }
    }
    chosen_done_cu_valid_comb := cu_found_valid
    chosen_done_cu_id_comb := cu_found
    io.gpu_interface_dealloc_available := gpu_interface_dealloc_available_i
    io.gpu_interface_dealloc_wg_id := gpu_interface_dealloc_wg_id_i
    io.gpu_interface_cu_id := gpu_interface_cu_id_i
    io.gpu_interface_alloc_available := gpu_interface_alloc_available_i
    io.dispatch2cu_wf_dispatch := dispatch2cu_wf_dispatch_i.asUInt
    io.dispatch2cu_wf_tag_dispatch := dispatch2cu_wf_tag_dispatch_i
    io.dispatch2cu_wg_wf_count := dispatch2cu_wg_wf_count_i
    io.dispatch2cu_wf_size_dispatch := dispatch2cu_wf_size_dispatch_i
    io.dispatch2cu_sgpr_base_dispatch := dispatch2cu_sgpr_base_dispatch_i
    io.dispatch2cu_vgpr_base_dispatch := dispatch2cu_vgpr_base_dispatch_i
    io.dispatch2cu_lds_base_dispatch := dispatch2cu_lds_base_dispatch_i
    io.dispatch2cu_start_pc_dispatch := dispatch2cu_start_pc_dispatch_i
    io.dispatch2cu_kernel_size_3d_dispatch := dispatch2cu_kernel_size_3d_dispatch_i
    io.dispatch2cu_pds_baseaddr_dispatch := dispatch2cu_pds_baseaddr_dispatch_i
    io.dispatch2cu_csr_knl_dispatch := dispatch2cu_csr_knl_dispatch_i
    io.dispatch2cu_gds_base_dispatch := dispatch2cu_gds_base_dispatch_i
}
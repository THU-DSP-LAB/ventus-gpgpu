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


//?? means: need discussion
//Conflict register assignment
class inflight_wg_buffer(val WG_ID_WIDTH: Int, val WF_COUNT_WIDTH: Int, val CU_ID_WIDTH: Int, val VGPR_ID_WIDTH: Int, val SGPR_ID_WIDTH: Int, val LDS_ID_WIDTH: Int, val GDS_ID_WIDTH: Int, val ENTRY_ADDR_WIDTH: Int, val NUMBER_ENTRIES: Int, val WAVE_ITEM_WIDTH: Int, val MEM_ADDR_WIDTH: Int, val WF_COUNT_WIDTH_PER_WG: Int) extends Module {
  // Shared index between two tables
  val SGPR_SIZE_L = 0;
  val SGPR_SIZE_H = SGPR_SIZE_L + SGPR_ID_WIDTH;
   
  val VGPR_SIZE_L = SGPR_SIZE_H + 1;
  val VGPR_SIZE_H = VGPR_SIZE_L + VGPR_ID_WIDTH;

  val WG_ID_L = VGPR_SIZE_H + 1;
  val WG_ID_H = WG_ID_L + WG_ID_WIDTH - 1;

  // Index for table with waiting wg
  val GDS_SIZE_L = WG_ID_H + 1;
  val GDS_SIZE_H = GDS_SIZE_L + GDS_ID_WIDTH;
   
  val LDS_SIZE_L = GDS_SIZE_H + 1;
  val LDS_SIZE_H = LDS_SIZE_L + LDS_ID_WIDTH;
   

  val WG_COUNT_L = LDS_SIZE_H + 1;
  val WG_COUNT_H = WG_COUNT_L + WF_COUNT_WIDTH_PER_WG-1;

  // Index for table with read wg
  val WF_SIZE_L = WG_ID_H + 1;
  val WF_SIZE_H = WF_SIZE_L + WAVE_ITEM_WIDTH - 1;
  val GDS_BASEADDR_L = WF_SIZE_H + 1;
  val GDS_BASEADDR_H = GDS_BASEADDR_L + MEM_ADDR_WIDTH -1;
  val START_PC_L = GDS_BASEADDR_H + 1;
  val START_PC_H = START_PC_L + MEM_ADDR_WIDTH -1;
  val KNL_SZ_3D_L = START_PC_H + 1
  val KNL_SZ_3D_H = KNL_SZ_3D_L + 3 * top.parameters.WG_SIZE_X_WIDTH - 1
  val PDS_BASEADDR_L = KNL_SZ_3D_H + 1
  val PDS_BASEADDR_H = PDS_BASEADDR_L + MEM_ADDR_WIDTH - 1;
  val CSR_KNL_L = PDS_BASEADDR_H + 1
  val CSR_KNL_H = CSR_KNL_L + MEM_ADDR_WIDTH - 1
  val io = IO(new Bundle {
    //host inputs
    val host_wg_valid = Input(Bool())
    val host_wg_id = Input(UInt(WG_ID_WIDTH.W))
    val host_num_wf = Input(UInt(WF_COUNT_WIDTH_PER_WG.W))
    val host_wf_size = Input(UInt(WAVE_ITEM_WIDTH.W))
    val host_start_pc = Input(UInt(MEM_ADDR_WIDTH.W))
    val host_kernel_size_3d = Input(Vec(3, UInt(top.parameters.WG_SIZE_X_WIDTH.W)))
    val host_pds_baseaddr = Input(UInt(MEM_ADDR_WIDTH.W))
    val host_csr_knl = Input(UInt(MEM_ADDR_WIDTH.W))
    val host_gds_baseaddr = Input(UInt(MEM_ADDR_WIDTH.W))
    val host_vgpr_size_total = Input(UInt((VGPR_ID_WIDTH + 1).W))
    val host_sgpr_size_total = Input(UInt((SGPR_ID_WIDTH + 1).W))
    val host_lds_size_total = Input(UInt((LDS_ID_WIDTH + 1).W))
    val host_gds_size_total = Input(UInt((GDS_ID_WIDTH + 1).W))
    val host_vgpr_size_per_wf = Input(UInt((VGPR_ID_WIDTH + 1).W))
    val host_sgpr_size_per_wf = Input(UInt((SGPR_ID_WIDTH + 1).W))
    //Dispatch controller inputs
    val dis_controller_wg_alloc_valid = Input(Bool())
    val dis_controller_start_alloc = Input(Bool())
    val dis_controller_wg_dealloc_valid = Input(Bool())
    val dis_controller_wg_rejected_valid = Input(Bool())

    val allocator_wg_id_out = Input(UInt(WG_ID_WIDTH.W))
    val gpu_interface_dealloc_wg_id = Input(UInt(WG_ID_WIDTH.W))

    //Outputs to the host
    val inflight_wg_buffer_host_rcvd_ack = Output(Bool())
    val inflight_wg_buffer_host_wf_done = Output(Bool())
    val inflight_wg_buffer_host_wf_done_wg_id = Output(UInt(WG_ID_WIDTH.W))

    
    // Allocator informs that there are valid wg and then the gpu ask for it
    // after a wg is passed to the gpu, it is cleared from the table
    // the resource table has all information on running wg resources

    //Outputs to the allocator
    val inflight_wg_buffer_alloc_valid = Output(Bool())
    val inflight_wg_buffer_alloc_available = Output(Bool())
    val inflight_wg_buffer_alloc_wg_id = Output(UInt(WG_ID_WIDTH.W))
    val inflight_wg_buffer_alloc_num_wf = Output(UInt(WF_COUNT_WIDTH_PER_WG.W))
    val inflight_wg_buffer_alloc_vgpr_size = Output(UInt((VGPR_ID_WIDTH + 1).W))
    val inflight_wg_buffer_alloc_sgpr_size = Output(UInt((SGPR_ID_WIDTH + 1).W))
    val inflight_wg_buffer_alloc_lds_size = Output(UInt((LDS_ID_WIDTH + 1).W))
    val inflight_wg_buffer_alloc_gds_size = Output(UInt((GDS_ID_WIDTH + 1).W))

    //Outputs to the GPU interface
    val inflight_wg_buffer_gpu_valid = Output(Bool())
    val inflight_wg_buffer_gpu_vgpr_size_per_wf = Output(UInt((VGPR_ID_WIDTH + 1).W))
    val inflight_wg_buffer_gpu_sgpr_size_per_wf = Output(UInt((SGPR_ID_WIDTH + 1).W))
    val inflight_wg_buffer_gpu_wf_size = Output(UInt(WAVE_ITEM_WIDTH.W))
    val inflight_wg_buffer_start_pc = Output(UInt(MEM_ADDR_WIDTH.W))
    val inflight_wg_buffer_kernel_size_3d = Output(Vec(3, UInt(top.parameters.WG_SIZE_X_WIDTH.W)))
    val inflight_wg_buffer_pds_baseaddr = Output(UInt(MEM_ADDR_WIDTH.W))
    val inflight_wg_buffer_csr_knl = Output(UInt(MEM_ADDR_WIDTH.W))
    val inflight_wg_buffer_gds_baseaddr = Output(UInt(MEM_ADDR_WIDTH.W))
  })
    val waiting_tbl_valid_rotated = Wire(Vec(NUMBER_ENTRIES, Bool()))
    val valid_not_pending = Wire(Vec(NUMBER_ENTRIES, Bool()))
    // Such parameters also show the content of two tables
    val WAIT_ENTRY_WIDTH = ( WG_ID_WIDTH + WF_COUNT_WIDTH_PER_WG + (VGPR_ID_WIDTH +1) + (SGPR_ID_WIDTH + 1) + (LDS_ID_WIDTH +1) + (GDS_ID_WIDTH + 1) )
    val READY_ENTRY_WIDTH = (MEM_ADDR_WIDTH + MEM_ADDR_WIDTH + 3*top.parameters.WG_SIZE_X_WIDTH + MEM_ADDR_WIDTH // csr_knl, pds_base, (z,y,x), start_pc
      + MEM_ADDR_WIDTH + WAVE_ITEM_WIDTH + WG_ID_WIDTH + (VGPR_ID_WIDTH +1) + (SGPR_ID_WIDTH + 1))
    val inflight_wg_buffer_alloc_wg_id_reg = RegInit(0.U(WG_ID_WIDTH.W))
    // Table1: wait_entry size + start pc + id + alloc attemp
    // w port: host/alloc attemp 
    // r port: allocator/cu (arbiter)
    val ram_wg_waiting_allocation = Module(new RAM(WAIT_ENTRY_WIDTH, ENTRY_ADDR_WIDTH, NUMBER_ENTRIES))
    // Table2: ready_entry wg_id + starts
    // w port: host 
    // r port: cu
    val ram_wg_ready_start = Module(new RAM(READY_ENTRY_WIDTH, ENTRY_ADDR_WIDTH, NUMBER_ENTRIES))
    //State number of host
    val INFLIGHT_TB_RD_HOST_NUM_STATES = 4
    val ST_RD_HOST_IDLE = 1<<0
    val ST_RD_HOST_GET_FROM_HOST = 1<<1
    val ST_RD_HOST_ACK_TO_HOST = 1<<2
    val ST_RD_HOST_IDLE_BUBBLE = 1<<3
    //State number of allocator
    val INFLIGHT_TB_ALLOC_NUM_STATES = 8
    val ST_ALLOC_IDLE = 1<<0
    val ST_ALLOC_WAIT_RESULT = 1<<1
    val ST_ALLOC_FIND_ACCEPTED = 1<<2
    val ST_ALLOC_CLEAR_ACCEPTED = 1<<3
    val ST_ALLOC_FIND_REJECTED = 1<<4
    val ST_ALLOC_CLEAR_REJECTED = 1<<5
    val ST_ALLOC_GET_ALLOC_WG = 1<<6
    val ST_ALLOC_UP_ALLOC_WG = 1<<7
    /**
    receives wg from host -> puts into waiting table
    if there are waiting wg -> signals to controller
    controller picks a wg -> clears entry from waiting table
    controllers rejects a picked wg -> increment rejected counter, puts back into waiting table
    cu finishes wg -> informs host
    
    table to convert wgid to index!! - think about this (table walker??)
    
    2 priority encoders:
    1 to find free index
    2 to find wg to chose - round robin on this
    **/
   // allocator -> state machine that choses between updating the allocated 
   //              wf or reading the done wf
   //interact with host. Use these registers to record host inputs
    val host_wg_valid_i = RegInit(false.B)
    host_wg_valid_i := io.host_wg_valid
    val host_wg_id_i = RegInit(0.U(WG_ID_WIDTH.W))
    host_wg_id_i := io.host_wg_id
    val host_num_wf_i = RegInit(0.U(WF_COUNT_WIDTH_PER_WG.W))
    host_num_wf_i := io.host_num_wf
    val host_wf_size_i = RegInit(0.U(WAVE_ITEM_WIDTH.W))
    host_wf_size_i := io.host_wf_size
    val host_start_pc_i = RegInit(0.U(MEM_ADDR_WIDTH.W))
    host_start_pc_i := io.host_start_pc
    val host_kernel_size_3d_i = RegInit(VecInit(Seq.fill(3)(0.U(top.parameters.WG_SIZE_X_WIDTH.W))))
    host_kernel_size_3d_i := io.host_kernel_size_3d
    val host_pds_baseaddr_i = RegInit(0.U(MEM_ADDR_WIDTH.W))
    host_pds_baseaddr_i := io.host_pds_baseaddr
    val host_csr_knl_i = RegInit(0.U(MEM_ADDR_WIDTH.W))
    host_csr_knl_i := io.host_csr_knl
    val host_gds_baseaddr_i = RegInit(0.U(MEM_ADDR_WIDTH.W))
    host_gds_baseaddr_i := io.host_gds_baseaddr
    val host_vgpr_size_total_i = RegInit(0.U((VGPR_ID_WIDTH + 1).W))
    host_vgpr_size_total_i := io.host_vgpr_size_total
    val host_sgpr_size_total_i = RegInit(0.U((SGPR_ID_WIDTH + 1).W))
    host_sgpr_size_total_i := io.host_sgpr_size_total
    val host_vgpr_size_per_wf_i = RegInit(0.U((VGPR_ID_WIDTH + 1).W))
    host_vgpr_size_per_wf_i := io.host_vgpr_size_per_wf
    val host_sgpr_size_per_wf_i = RegInit(0.U((SGPR_ID_WIDTH + 1).W))
    host_sgpr_size_per_wf_i := io.host_sgpr_size_per_wf
    val host_lds_size_total_i = RegInit(0.U((LDS_ID_WIDTH + 1).W))
    host_lds_size_total_i := io.host_lds_size_total
    val host_gds_size_total_i = RegInit(0.U((GDS_ID_WIDTH + 1).W))
    host_gds_size_total_i := io.host_gds_size_total
    val dis_controller_start_alloc_i = RegInit(false.B)
    dis_controller_start_alloc_i := io.dis_controller_start_alloc
    val dis_controller_wg_alloc_valid_i = RegInit(false.B)
    val dis_controller_wg_dealloc_valid_i = RegInit(false.B)
    val dis_controller_wg_rejected_valid_i = RegInit(false.B)
    val allocator_wg_alloc_id_i = RegInit(0.U(WG_ID_WIDTH.W))
    dis_controller_wg_dealloc_valid_i := io.dis_controller_wg_dealloc_valid
    val gpu_interface_dealloc_wg_id_i = RegInit(0.U(WG_ID_WIDTH.W))
    gpu_interface_dealloc_wg_id_i := io.gpu_interface_dealloc_wg_id
    val inflight_tbl_alloc_st = RegInit(ST_ALLOC_IDLE.U(INFLIGHT_TB_ALLOC_NUM_STATES.W))
    val inflight_tbl_rd_host_st = RegInit(ST_RD_HOST_IDLE.U(INFLIGHT_TB_RD_HOST_NUM_STATES.W))
    val allocator_wg_id_out_i = RegInit(0.U(WG_ID_WIDTH.W))
    when(io.dis_controller_wg_alloc_valid){
      dis_controller_wg_alloc_valid_i := true.B
      allocator_wg_id_out_i := io.allocator_wg_id_out
    }
    when(io.dis_controller_wg_rejected_valid){
      dis_controller_wg_rejected_valid_i := true.B
      allocator_wg_id_out_i := io.allocator_wg_id_out
    }
    val inflight_wg_buffer_host_wf_done_i = RegInit(false.B)
    io.inflight_wg_buffer_host_wf_done := inflight_wg_buffer_host_wf_done_i
    val inflight_wg_buffer_host_wf_done_wg_id_i = RegInit(0.U(WG_ID_WIDTH.W))
    io.inflight_wg_buffer_host_wf_done_wg_id := inflight_wg_buffer_host_wf_done_wg_id_i
    inflight_wg_buffer_host_wf_done_i := false.B
    // De-allocation logic
    when(dis_controller_wg_dealloc_valid_i){
      inflight_wg_buffer_host_wf_done_i := true.B
      inflight_wg_buffer_host_wf_done_wg_id_i := gpu_interface_dealloc_wg_id_i
    }

    // waiting table valid: reflect whether there is a value in the table
    val waiting_tbl_valid = RegInit(VecInit(Seq.fill(NUMBER_ENTRIES)(false.B)))
    val new_index_wr_en = RegInit(false.B)
    ram_wg_waiting_allocation.io.wr_en := new_index_wr_en
    ram_wg_ready_start.io.wr_en := new_index_wr_en
    val new_entry_wg_reg = RegInit(0.U(WAIT_ENTRY_WIDTH.W))
    ram_wg_waiting_allocation.io.wr_word := new_entry_wg_reg
    val ready_tbl_wr_reg = RegInit(0.U(READY_ENTRY_WIDTH.W))
    ram_wg_ready_start.io.wr_word := ready_tbl_wr_reg
    val inflight_wg_buffer_host_rcvd_ack_i = RegInit(false.B)
    io.inflight_wg_buffer_host_rcvd_ack := inflight_wg_buffer_host_rcvd_ack_i
    val new_index = RegInit(0.U(ENTRY_ADDR_WIDTH.W))
    ram_wg_waiting_allocation.io.wr_addr := new_index
    ram_wg_ready_start.io.wr_addr := new_index


    inflight_wg_buffer_host_rcvd_ack_i := false.B
    new_index_wr_en := false.B


    //State Machine of host tag
    switch(inflight_tbl_rd_host_st){
      is(ST_RD_HOST_IDLE.U){
        /*
          The first stage:
          Prepare to receive the wg from host
        */
        when(host_wg_valid_i){
          when(waiting_tbl_valid.contains(false.B)){
            inflight_tbl_rd_host_st := ST_RD_HOST_GET_FROM_HOST.U
          }
        }
      }
      is(ST_RD_HOST_GET_FROM_HOST.U){
        /*
          The second stage:
          Get the wg from host
        */
        new_index_wr_en := true.B
        new_entry_wg_reg := Cat(host_num_wf_i, host_lds_size_total_i, host_gds_size_total_i, host_wg_id_i, host_vgpr_size_total_i, host_sgpr_size_total_i)
        ready_tbl_wr_reg := Cat(host_csr_knl_i, host_pds_baseaddr_i, host_kernel_size_3d_i.asUInt, host_start_pc_i, host_gds_baseaddr_i, host_wf_size_i, host_wg_id_i, host_vgpr_size_per_wf_i, host_sgpr_size_per_wf_i)
        inflight_tbl_rd_host_st := ST_RD_HOST_ACK_TO_HOST.U
        inflight_wg_buffer_host_rcvd_ack_i := true.B
      }
      is(ST_RD_HOST_ACK_TO_HOST.U){
        /*
          The third stage:
          waiting_tbl_valid[new_index] = 1'b1;
        */
        waiting_tbl_valid(new_index) := true.B
        inflight_tbl_rd_host_st := ST_RD_HOST_IDLE_BUBBLE.U
      }
      is(ST_RD_HOST_IDLE_BUBBLE.U){
        /*
          The fourth stage: a bubble for handshake signal
         */
        inflight_tbl_rd_host_st := ST_RD_HOST_IDLE.U
      }
    }

    // The wg considered to be allocated
    val waiting_tbl_pending = RegInit(VecInit(Seq.fill(NUMBER_ENTRIES)(false.B)))
    val chosen_entry = RegInit(0.U(ENTRY_ADDR_WIDTH.W))
    val chosen_entry_by_allocator = RegInit(0.U(ENTRY_ADDR_WIDTH.W))
    val chosen_entry_is_valid = RegInit(false.B)
    val wait_tbl_busy = RegInit(false.B)
    wait_tbl_busy := true.B
    io.inflight_wg_buffer_alloc_available := !wait_tbl_busy

    for(i <- 0 until NUMBER_ENTRIES){
      valid_not_pending(i) := waiting_tbl_valid(i) & !waiting_tbl_pending(i)
    }

    val tbl_walk_wg_id_searched = RegInit(0.U(WG_ID_WIDTH.W))
    val table_walk_rd_reg = Wire(UInt(WAIT_ENTRY_WIDTH.W))
    table_walk_rd_reg := ram_wg_waiting_allocation.io.rd_word
    val ready_tbl_rd_reg = Wire(UInt(READY_ENTRY_WIDTH.W))
    ready_tbl_rd_reg := ram_wg_ready_start.io.rd_word
    val tbl_walk_rd_en = RegInit(false.B)
    ram_wg_waiting_allocation.io.rd_en := tbl_walk_rd_en
    ram_wg_ready_start.io.rd_en := tbl_walk_rd_en
    val tbl_walk_idx = RegInit(0.U(ENTRY_ADDR_WIDTH.W))
    ram_wg_waiting_allocation.io.rd_addr := tbl_walk_idx
    ram_wg_ready_start.io.rd_addr := tbl_walk_idx
    val tbl_walk_rd_valid = RegInit(false.B)
    tbl_walk_rd_valid := tbl_walk_rd_en
    //??
    tbl_walk_rd_en := false.B

    //Output to gpu registers
    val inflight_wg_buffer_gpu_valid_i = RegInit(false.B)
    io.inflight_wg_buffer_gpu_valid := inflight_wg_buffer_gpu_valid_i
    val inflight_wg_buffer_gpu_vgpr_size_per_wf_i = RegInit(0.U((VGPR_ID_WIDTH + 1).W))
    io.inflight_wg_buffer_gpu_vgpr_size_per_wf := inflight_wg_buffer_gpu_vgpr_size_per_wf_i
    val inflight_wg_buffer_gpu_sgpr_size_per_wf_i = RegInit(0.U((SGPR_ID_WIDTH + 1).W))
    io.inflight_wg_buffer_gpu_sgpr_size_per_wf := inflight_wg_buffer_gpu_sgpr_size_per_wf_i
    val inflight_wg_buffer_gpu_wf_size_i = RegInit(0.U(WAVE_ITEM_WIDTH.W))
    io.inflight_wg_buffer_gpu_wf_size := inflight_wg_buffer_gpu_wf_size_i
    val inflight_wg_buffer_start_pc_i = RegInit(0.U(MEM_ADDR_WIDTH.W))
    io.inflight_wg_buffer_start_pc := inflight_wg_buffer_start_pc_i
    val inflight_wg_buffer_kernel_size_3d_i = RegInit(VecInit(Seq.fill(3)(0.U(top.parameters.WG_SIZE_X_WIDTH.W))))
    io.inflight_wg_buffer_kernel_size_3d := inflight_wg_buffer_kernel_size_3d_i
    val inflight_wg_buffer_pds_baseaddr_i = RegInit(0.U(MEM_ADDR_WIDTH.W))
    io.inflight_wg_buffer_pds_baseaddr := inflight_wg_buffer_pds_baseaddr_i
    val inflight_wg_buffer_csr_knl = RegInit(0.U(MEM_ADDR_WIDTH.W))
    io.inflight_wg_buffer_csr_knl := inflight_wg_buffer_csr_knl
    val inflight_wg_buffer_gds_baseaddr_i = RegInit(0.U(MEM_ADDR_WIDTH.W))
    io.inflight_wg_buffer_gds_baseaddr := inflight_wg_buffer_gds_baseaddr_i

    //Output to allocator registers
    val inflight_wg_buffer_alloc_wg_id_i = RegInit(0.U(WG_ID_WIDTH.W))
    io.inflight_wg_buffer_alloc_wg_id := inflight_wg_buffer_alloc_wg_id_i
    val inflight_wg_buffer_alloc_num_wf_i = RegInit(0.U(WF_COUNT_WIDTH_PER_WG.W))
    io.inflight_wg_buffer_alloc_num_wf := inflight_wg_buffer_alloc_num_wf_i
    val inflight_wg_buffer_alloc_vgpr_size_i = RegInit(0.U((VGPR_ID_WIDTH + 1).W))
    io.inflight_wg_buffer_alloc_vgpr_size := inflight_wg_buffer_alloc_vgpr_size_i
    val inflight_wg_buffer_alloc_sgpr_size_i = RegInit(0.U((SGPR_ID_WIDTH + 1).W))
    io.inflight_wg_buffer_alloc_sgpr_size := inflight_wg_buffer_alloc_sgpr_size_i
    val inflight_wg_buffer_alloc_lds_size_i = RegInit(0.U((LDS_ID_WIDTH + 1).W))
    io.inflight_wg_buffer_alloc_lds_size := inflight_wg_buffer_alloc_lds_size_i
    val inflight_wg_buffer_alloc_gds_size_i = RegInit(0.U((GDS_ID_WIDTH + 1).W))
    io.inflight_wg_buffer_alloc_gds_size := inflight_wg_buffer_alloc_gds_size_i
    val wg_waiting_alloc_valid = RegInit(false.B)
    io.inflight_wg_buffer_alloc_valid := wg_waiting_alloc_valid
    val last_chosen_entry_rr = RegInit((~0.U(ENTRY_ADDR_WIDTH.W)).asUInt)
    when(dis_controller_start_alloc_i){
      last_chosen_entry_rr := chosen_entry_by_allocator
    }
    //State Machine of allocator interface
    inflight_wg_buffer_gpu_valid_i := false.B
    switch(inflight_tbl_alloc_st){
      is(ST_ALLOC_IDLE.U){
        /*
          The first stage:
          Prepare to allocate a wg
        */
        when(dis_controller_start_alloc_i){
          waiting_tbl_pending(chosen_entry_by_allocator) := true.B
          wg_waiting_alloc_valid := false.B
          inflight_tbl_alloc_st := ST_ALLOC_WAIT_RESULT.U
        }
        .elsewhen(!wg_waiting_alloc_valid && valid_not_pending.contains(true.B)){
          inflight_tbl_alloc_st := ST_ALLOC_GET_ALLOC_WG.U
        }
        .otherwise{
          wait_tbl_busy := false.B
        }
      }
      is(ST_ALLOC_WAIT_RESULT.U){
        /*
          The second stage:
          Wait for the result of allocation
        */
        wait_tbl_busy := false.B
        when(dis_controller_wg_alloc_valid_i){
          dis_controller_wg_alloc_valid_i := false.B
          tbl_walk_wg_id_searched := allocator_wg_id_out_i
          tbl_walk_rd_en := true.B
          //incremental find
          //?? any improvement?
          tbl_walk_idx := 0.U
          inflight_tbl_alloc_st := ST_ALLOC_FIND_ACCEPTED.U
        }
        .elsewhen(dis_controller_wg_rejected_valid_i){
          dis_controller_wg_rejected_valid_i := false.B
          tbl_walk_wg_id_searched := allocator_wg_id_out_i
          tbl_walk_rd_en := true.B
          //incremental find
          tbl_walk_idx := 0.U
          inflight_tbl_alloc_st := ST_ALLOC_FIND_REJECTED.U
        }
      }
      is(ST_ALLOC_FIND_ACCEPTED.U){
        /*
          The third stage:
          Find the accepted wg
        */
        when(tbl_walk_rd_valid){
          when((table_walk_rd_reg(WG_ID_H, WG_ID_L) === tbl_walk_wg_id_searched) && waiting_tbl_pending(tbl_walk_idx)){
            //printf(p"table_walk_rd_reg(WG_ID_H, WG_ID_L): ${table_walk_rd_reg(WG_ID_H, WG_ID_L)}")
            inflight_tbl_alloc_st := ST_ALLOC_CLEAR_ACCEPTED.U
          }
          .otherwise{
            tbl_walk_idx := tbl_walk_idx + 1.U
            tbl_walk_rd_en := true.B
          }
        }
      }
      is(ST_ALLOC_CLEAR_ACCEPTED.U){
        //printf(p"c: clear accepted\n")
        waiting_tbl_valid(tbl_walk_idx) := false.B //clear the accepted wg
        waiting_tbl_pending(tbl_walk_idx) := false.B
        inflight_wg_buffer_gpu_valid_i := true.B
        inflight_wg_buffer_gpu_vgpr_size_per_wf_i := ready_tbl_rd_reg(VGPR_SIZE_H, VGPR_SIZE_L)
        inflight_wg_buffer_gpu_sgpr_size_per_wf_i := ready_tbl_rd_reg(SGPR_SIZE_H, SGPR_SIZE_L)
        inflight_wg_buffer_gpu_wf_size_i := ready_tbl_rd_reg(WF_SIZE_H, WF_SIZE_L)
        inflight_wg_buffer_start_pc_i := ready_tbl_rd_reg(START_PC_H, START_PC_L)
        inflight_wg_buffer_kernel_size_3d_i.zipWithIndex.foreach{ case(x, i) =>
          x := (ready_tbl_rd_reg(KNL_SZ_3D_H, KNL_SZ_3D_L)>>(i*top.parameters.WG_SIZE_X_WIDTH))(top.parameters.WG_SIZE_X_WIDTH-1, 0) }
        inflight_wg_buffer_pds_baseaddr_i := ready_tbl_rd_reg(PDS_BASEADDR_H, PDS_BASEADDR_L)
        inflight_wg_buffer_csr_knl := ready_tbl_rd_reg(CSR_KNL_H, CSR_KNL_L)
        inflight_wg_buffer_gds_baseaddr_i := ready_tbl_rd_reg(GDS_BASEADDR_H,GDS_BASEADDR_L)
        inflight_tbl_alloc_st := ST_ALLOC_GET_ALLOC_WG.U
      }
      is(ST_ALLOC_FIND_REJECTED.U){
        /*
          The third stage:
          Find the rejected wg
        */
        when(tbl_walk_rd_valid){
          when((table_walk_rd_reg(WG_ID_H, WG_ID_L) === tbl_walk_wg_id_searched) && waiting_tbl_pending(tbl_walk_idx)){
            inflight_tbl_alloc_st := ST_ALLOC_CLEAR_REJECTED.U
          }
          .otherwise{
            tbl_walk_idx := tbl_walk_idx + 1.U
            tbl_walk_rd_en := true.B
          }
        }
      }
      is(ST_ALLOC_CLEAR_REJECTED.U){
        waiting_tbl_pending(tbl_walk_idx) := false.B
        inflight_tbl_alloc_st := ST_ALLOC_GET_ALLOC_WG.U
      }
      is(ST_ALLOC_GET_ALLOC_WG.U){
        /*
          The fourth stage:
          Get the wg to be allocated
        */
        when(chosen_entry_is_valid){
          tbl_walk_rd_en := true.B
          tbl_walk_idx := chosen_entry
          chosen_entry_by_allocator := chosen_entry
          inflight_tbl_alloc_st := ST_ALLOC_UP_ALLOC_WG.U
        }
        .otherwise{
          inflight_tbl_alloc_st := ST_ALLOC_IDLE.U
        }
      }
      is(ST_ALLOC_UP_ALLOC_WG.U){
        /*
          The fifth stage:
        */
        when(tbl_walk_rd_valid){
          wg_waiting_alloc_valid := true.B
          inflight_wg_buffer_alloc_wg_id_i := table_walk_rd_reg(WG_ID_H, WG_ID_L)
          inflight_wg_buffer_alloc_num_wf_i := table_walk_rd_reg(WG_COUNT_H, WG_COUNT_L)
          inflight_wg_buffer_alloc_vgpr_size_i := table_walk_rd_reg(VGPR_SIZE_H, VGPR_SIZE_L)
          inflight_wg_buffer_alloc_sgpr_size_i := table_walk_rd_reg(SGPR_SIZE_H, SGPR_SIZE_L)
          inflight_wg_buffer_alloc_lds_size_i := table_walk_rd_reg(LDS_SIZE_H, LDS_SIZE_L)
          inflight_wg_buffer_alloc_gds_size_i := table_walk_rd_reg(GDS_SIZE_H, GDS_SIZE_L)
          inflight_tbl_alloc_st := ST_ALLOC_IDLE.U
        }
      }
    }
    val new_index_comb = Wire(UInt(ENTRY_ADDR_WIDTH.W))
    val chosen_entry_comb = Wire(UInt(ENTRY_ADDR_WIDTH.W))
    val chosen_entry_is_valid_comb = Wire(Bool())
    new_index := new_index_comb
    chosen_entry := chosen_entry_comb
    chosen_entry_is_valid := chosen_entry_is_valid_comb
    val idx_found_entry = Wire(UInt(ENTRY_ADDR_WIDTH.W))
    val found_entry_valid = Wire(Bool())
    found_entry_valid := false.B
    idx_found_entry := 0.U
    for(i <- NUMBER_ENTRIES - 1 to 0 by -1){
      when(!waiting_tbl_valid(i)){
        found_entry_valid := true.B
        idx_found_entry := i.U
      }
    }
    new_index_comb := idx_found_entry

    //roundly find entry

    val idx_found_entry_c = Wire(UInt(ENTRY_ADDR_WIDTH.W))
    val found_entry_valid_c = Wire(Bool())
    found_entry_valid_c := false.B
    idx_found_entry_c := 0.U
    val left_degree = WireInit(NUMBER_ENTRIES.U - (1.U + last_chosen_entry_rr))
    val right_degree = WireInit(1.U + last_chosen_entry_rr)
    for(i <- 0 until NUMBER_ENTRIES){
      when(i.U >= left_degree){
        waiting_tbl_valid_rotated(i) := valid_not_pending(i.U - left_degree)
      }
      .otherwise{
        waiting_tbl_valid_rotated(i) := valid_not_pending(i.U + right_degree)
      }
    }
    for(i <- NUMBER_ENTRIES - 1 to 0 by -1){
      when(waiting_tbl_valid_rotated(i)){
        found_entry_valid_c := true.B
        idx_found_entry_c := i.U + last_chosen_entry_rr + 1.U
      }
    }
    chosen_entry_is_valid_comb := found_entry_valid_c
    chosen_entry_comb := idx_found_entry_c

    
    //debug output
    //printf(p"c: dis_controller_wg_rejected_valid_i: ${dis_controller_wg_rejected_valid_i}\n")
    //printf(p"c: alloc state: $inflight_tbl_alloc_st\n")
    //printf(p"c: waiting_tbl_valid: ${waiting_tbl_valid}\n")
    //printf(p"c: waiting_tbl_pending: ${waiting_tbl_pending}\n")
    //printf(p"c: waiting_tbl_valid_rotated: ${waiting_tbl_valid_rotated}\n")
    //printf(p"c: valid_not_pending: ${valid_not_pending}\n")
    //printf(p"c: tbl_walk_wg_id_searched: $tbl_walk_wg_id_searched\n")
    //printf(p"c: chosen_entry_by_allocator: $chosen_entry_by_allocator\n")
    //printf(p"c: last_chosen_entry_rr: $last_chosen_entry_rr\n")
}

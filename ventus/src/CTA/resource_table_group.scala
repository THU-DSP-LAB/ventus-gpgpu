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

class resource_table_group(val NUMBER_CU: Int, val CU_ID_WIDTH: Int, val RES_TABLE_ADDR_WIDTH: Int, val VGPR_ID_WIDTH: Int, val NUMBER_VGPR_SLOTS: Int, val SGPR_ID_WIDTH: Int, val NUMBER_SGPR_SLOTS: Int, val LDS_ID_WIDTH: Int, val NUMBER_LDS_SLOTS: Int, val WG_ID_WIDTH: Int, val WF_COUNT_WIDTH: Int, val WG_SLOT_ID_WIDTH: Int, val NUMBER_WF_SLOTS: Int, val WF_COUNT_MAX: Int, val INIT_MAX_WG_COUNT: Int, val WF_COUNT_WIDTH_PER_WG: Int) extends Module{
    val io = IO(new Bundle{
        val alloc_en = Input(Bool())
        val dealloc_en = Input(Bool())
        val wg_id = Input(UInt(WG_ID_WIDTH.W))
        val sub_cu_id = Input(UInt((CU_ID_WIDTH - RES_TABLE_ADDR_WIDTH).W))
        val res_tbl_done = Output(Bool())
        val lds_start = Output(UInt(LDS_ID_WIDTH.W))
        val lds_size = Output(UInt(LDS_ID_WIDTH.W))
        val vgpr_start = Output(UInt(VGPR_ID_WIDTH.W))
        val vgpr_size = Output(UInt(VGPR_ID_WIDTH.W))
        val sgpr_start = Output(UInt(SGPR_ID_WIDTH.W))
        val sgpr_size = Output(UInt(SGPR_ID_WIDTH.W))
        val wf_count = Output(UInt(WF_COUNT_WIDTH.W))
        val wg_count = Output(UInt((WG_SLOT_ID_WIDTH + 1).W))
        val lds_start_in = Input(UInt(LDS_ID_WIDTH.W))
        val lds_size_in = Input(UInt(LDS_ID_WIDTH.W))
        val vgpr_start_in = Input(UInt(VGPR_ID_WIDTH.W))
        val vgpr_size_in = Input(UInt(VGPR_ID_WIDTH.W))
        val sgpr_start_in = Input(UInt(SGPR_ID_WIDTH.W))
        val sgpr_size_in = Input(UInt(SGPR_ID_WIDTH.W))
        val wf_count_in = Input(UInt(WF_COUNT_WIDTH_PER_WG.W))
        val done_cancelled = Input(Bool())
    })
    val lds_res_tbl = Module(new resource_table(CU_ID_WIDTH - RES_TABLE_ADDR_WIDTH, NUMBER_CU, WG_SLOT_ID_WIDTH, NUMBER_WF_SLOTS, LDS_ID_WIDTH, NUMBER_LDS_SLOTS))
    val vgpr_res_tbl = Module(new resource_table(CU_ID_WIDTH - RES_TABLE_ADDR_WIDTH, NUMBER_CU, WG_SLOT_ID_WIDTH, NUMBER_WF_SLOTS, VGPR_ID_WIDTH, NUMBER_VGPR_SLOTS))
    val sgpr_res_tbl = Module(new resource_table(CU_ID_WIDTH - RES_TABLE_ADDR_WIDTH, NUMBER_CU, WG_SLOT_ID_WIDTH, NUMBER_WF_SLOTS, SGPR_ID_WIDTH, NUMBER_SGPR_SLOTS))
    val wf_res_tbl = Module(new wg_resource_table_neo(WF_COUNT_MAX, NUMBER_CU, CU_ID_WIDTH - RES_TABLE_ADDR_WIDTH, WF_COUNT_WIDTH, WG_SLOT_ID_WIDTH, NUMBER_WF_SLOTS, WF_COUNT_WIDTH_PER_WG))
    val wf_slot_id_gen = Module(new wg_slot_id_convert_opt(NUMBER_CU, CU_ID_WIDTH - RES_TABLE_ADDR_WIDTH, WG_ID_WIDTH, WG_SLOT_ID_WIDTH))
    val wg_throttling = Module(new throttling_engine(NUMBER_CU, CU_ID_WIDTH - RES_TABLE_ADDR_WIDTH, WG_SLOT_ID_WIDTH, INIT_MAX_WG_COUNT))
    //Step1: get slot id for wg
    val alloc_en_1 = RegInit(false.B)
    alloc_en_1 := io.alloc_en
    val dealloc_en_1 = RegInit(false.B)
    dealloc_en_1 := io.dealloc_en
    wf_slot_id_gen.io.wg_id := io.wg_id
    wf_slot_id_gen.io.cu_id := io.sub_cu_id
    wf_slot_id_gen.io.generate := io.alloc_en
    wf_slot_id_gen.io.find_and_cancel := io.dealloc_en
    val cu_id_1 = RegInit(0.U((CU_ID_WIDTH - RES_TABLE_ADDR_WIDTH).W))
    val lds_size1 = RegInit(0.U(LDS_ID_WIDTH.W))
    val vgpr_size1 = RegInit(0.U(VGPR_ID_WIDTH.W))
    val sgpr_size1 = RegInit(0.U(SGPR_ID_WIDTH.W))
    val wf_count1 = RegInit(0.U(WF_COUNT_WIDTH.W))
    val lds_start1 = RegInit(0.U(LDS_ID_WIDTH.W))
    val vgpr_start1 = RegInit(0.U(VGPR_ID_WIDTH.W))
    val sgpr_start1 = RegInit(0.U(SGPR_ID_WIDTH.W))
    val lds_done_out = RegInit(false.B)
    val vgpr_done_out = RegInit(false.B)
    val sgpr_done_out = RegInit(false.B)
    //val wf_counter_table = RegInit(VecInit(Seq.fill(1 << WG_ID_WIDTH)(0.U(WF_COUNT_WIDTH.W))))
    io.res_tbl_done := lds_done_out && vgpr_done_out && sgpr_done_out
    when(io.alloc_en || io.dealloc_en){
        cu_id_1 := io.sub_cu_id
        lds_size1 := io.lds_size_in
        vgpr_size1 := io.vgpr_size_in
        sgpr_size1 := io.sgpr_size_in
        lds_start1 := io.lds_start_in
        vgpr_start1 := io.vgpr_start_in
        sgpr_start1 := io.sgpr_start_in
        lds_done_out := false.B
        vgpr_done_out := false.B
        sgpr_done_out := false.B
    }
    when(io.alloc_en){
        wf_count1 := io.wf_count_in
        //wf_counter_table(io.wg_id) := io.wf_count_in
    }
    .elsewhen(io.dealloc_en){
        wf_count1 := 0.U //wf_counter_table(io.wg_id)
    }
    //Step2: Pass Data to 4 tables
    lds_res_tbl.io.alloc_res_en := alloc_en_1
    lds_res_tbl.io.dealloc_res_en := dealloc_en_1
    lds_res_tbl.io.alloc_cu_id := cu_id_1
    lds_res_tbl.io.dealloc_cu_id := cu_id_1
    lds_res_tbl.io.alloc_wg_slot_id := wf_slot_id_gen.io.wg_slot_id_gen
    lds_res_tbl.io.dealloc_wg_slot_id := wf_slot_id_gen.io.wg_slot_id_find
    lds_res_tbl.io.alloc_res_size := lds_size1
    lds_res_tbl.io.alloc_res_start := lds_start1

    vgpr_res_tbl.io.alloc_res_en := alloc_en_1
    vgpr_res_tbl.io.dealloc_res_en := dealloc_en_1
    vgpr_res_tbl.io.alloc_cu_id := cu_id_1
    vgpr_res_tbl.io.dealloc_cu_id := cu_id_1
    vgpr_res_tbl.io.alloc_wg_slot_id := wf_slot_id_gen.io.wg_slot_id_gen
    vgpr_res_tbl.io.dealloc_wg_slot_id := wf_slot_id_gen.io.wg_slot_id_find
    vgpr_res_tbl.io.alloc_res_size := vgpr_size1
    vgpr_res_tbl.io.alloc_res_start := vgpr_start1

    sgpr_res_tbl.io.alloc_res_en := alloc_en_1
    sgpr_res_tbl.io.dealloc_res_en := dealloc_en_1
    sgpr_res_tbl.io.alloc_cu_id := cu_id_1
    sgpr_res_tbl.io.dealloc_cu_id := cu_id_1
    sgpr_res_tbl.io.alloc_wg_slot_id := wf_slot_id_gen.io.wg_slot_id_gen
    sgpr_res_tbl.io.dealloc_wg_slot_id := wf_slot_id_gen.io.wg_slot_id_find
    sgpr_res_tbl.io.alloc_res_size := sgpr_size1
    sgpr_res_tbl.io.alloc_res_start := sgpr_start1

    wf_res_tbl.io.cu_id := cu_id_1
    wf_res_tbl.io.alloc_en := alloc_en_1
    wf_res_tbl.io.dealloc_en := dealloc_en_1
    wf_res_tbl.io.wf_count_in := wf_count1
    wf_res_tbl.io.alloc_wg_slot_id := wf_slot_id_gen.io.wg_slot_id_gen
    wf_res_tbl.io.dealloc_wg_slot_id := wf_slot_id_gen.io.wg_slot_id_find

    wg_throttling.io.cu_id := cu_id_1
    wg_throttling.io.alloc_en := alloc_en_1
    wg_throttling.io.dealloc_en := dealloc_en_1
    /*
    TODO:
    update throttling table according to cu contention info
    */
    wg_throttling.io.wg_max_update_all_cu := false.B
    wg_throttling.io.wg_max_update := 0.U
    wg_throttling.io.wg_max_update_cu_id := 0.U
    wg_throttling.io.wg_max_update_valid := false.B


    //Step3:gather info from 4 tables
    io.wf_count := wf_res_tbl.io.wf_count_out
    io.wg_count := wg_throttling.io.wg_count_available
    val lds_size_out = RegInit(0.U(LDS_ID_WIDTH.W))
    val vgpr_size_out = RegInit(0.U(VGPR_ID_WIDTH.W))
    val sgpr_size_out = RegInit(0.U(SGPR_ID_WIDTH.W))
    val lds_start_out = RegInit(0.U(LDS_ID_WIDTH.W))
    val vgpr_start_out = RegInit(0.U(VGPR_ID_WIDTH.W))
    val sgpr_start_out = RegInit(0.U(SGPR_ID_WIDTH.W))
    when(lds_res_tbl.io.res_table_done_o){
        lds_done_out := true.B
        lds_size_out := lds_res_tbl.io.cam_biggest_space_size
        lds_start_out := lds_res_tbl.io.cam_biggest_space_addr
    }
    when(vgpr_res_tbl.io.res_table_done_o){
        vgpr_done_out := true.B
        vgpr_size_out := vgpr_res_tbl.io.cam_biggest_space_size
        vgpr_start_out := vgpr_res_tbl.io.cam_biggest_space_addr
    }
    when(sgpr_res_tbl.io.res_table_done_o){
        sgpr_done_out := true.B
        sgpr_size_out := sgpr_res_tbl.io.cam_biggest_space_size
        sgpr_start_out := sgpr_res_tbl.io.cam_biggest_space_addr
    }
    io.lds_size := lds_size_out
    io.vgpr_size := vgpr_size_out
    io.sgpr_size := sgpr_size_out
    io.lds_start := lds_start_out
    io.vgpr_start := vgpr_start_out
    io.sgpr_start := sgpr_start_out
    when(io.done_cancelled){
        //printf(p"done cancelled\n")
        lds_done_out := false.B
        vgpr_done_out := false.B
        sgpr_done_out := false.B
    }
    /*
    //print wf count
    printf(p"wf_count:${wf_count1}\n")
    //print dealloc_en_1
    printf(p"dealloc_en_1:${dealloc_en_1}\n")
    //print alloc_en_1
    printf(p"alloc_en_1:${alloc_en_1}\n")
    val debug_wg_id = RegInit(0.U(WG_ID_WIDTH.W))
    debug_wg_id := io.wg_id
    //print wg id
    printf(p"wg_id:${debug_wg_id}\n")
    printf(p"lds_done_out: ${lds_done_out}\n")
    printf(p"vgpr_done_out: ${vgpr_done_out}\n")
    printf(p"sgpr_done_out: ${sgpr_done_out}\n")
    when(lds_done_out && vgpr_done_out && sgpr_done_out){
        printf(p"RES TBL DONE\n")
        printf(p"wf_count:${wf_res_tbl.io.wf_count_out}\n")
    }
    
    
    when(lds_done_out && vgpr_done_out && sgpr_done_out){
        printf(p"RES TBL DONE\n")
        printf(p"wf_count:${wf_res_tbl.io.wf_count_out}\n")
    }
    when(lds_done_out){
        printf(p"LDS RES TBL DONE\n")
        printf(p"LDS_size_out:${lds_size_out}\n")
        printf(p"LDS_start_out:${lds_start_out}\n")
    }
    when(vgpr_done_out){
        printf(p"VGPR RES TBL DONE\n")
        printf(p"VGPR_size_out:${vgpr_size_out}\n")
        printf(p"VGPR_start_out:${vgpr_start_out}\n")
    }
    when(sgpr_done_out){
        printf(p"SGPR RES TBL DONE\n")
        printf(p"SGPR_size_out:${sgpr_size_out}\n")
        printf(p"SGPR_start_out:${sgpr_start_out}\n")
    }
    */   
}
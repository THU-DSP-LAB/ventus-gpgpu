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

class wg_resource_table_neo(val WF_COUNT_MAX: Int, val NUMBER_CU: Int, val CU_ID_WIDTH: Int, val WF_COUNT_WIDTH: Int, val WG_SLOT_ID_WIDTH: Int, val NUMBER_WF_SLOTS: Int, val WF_COUNT_WIDTH_PER_WG: Int) extends Module{
    val io = IO(new Bundle{
        val wf_count_out = Output(UInt(WF_COUNT_WIDTH.W))
        val cu_id = Input(UInt(CU_ID_WIDTH.W))
        val alloc_en = Input(Bool())
        val dealloc_en = Input(Bool())
        val wf_count_in = Input(UInt(WF_COUNT_WIDTH_PER_WG.W))
        val alloc_wg_slot_id = Input(UInt(WG_SLOT_ID_WIDTH.W))
        val dealloc_wg_slot_id = Input(UInt(WG_SLOT_ID_WIDTH.W))
    })
    val wf_count_out_i = RegInit(VecInit(Seq.fill(NUMBER_CU){WF_COUNT_MAX.U(WF_COUNT_WIDTH.W)}))
    val wf_count_per_wg_slot = RegInit{
        val data = Wire(Vec(NUMBER_CU, Vec(1 << WG_SLOT_ID_WIDTH, UInt(WF_COUNT_WIDTH_PER_WG.W))))
        for(i <- 0 until NUMBER_CU){
            for(j <- 0 until (1 << WG_SLOT_ID_WIDTH)){
                data(i)(j) := 0.U(WF_COUNT_WIDTH_PER_WG.W)
            }
        }
        data
    }
    when(io.alloc_en){
        wf_count_out_i(io.cu_id) := wf_count_out_i(io.cu_id) - io.wf_count_in
        wf_count_per_wg_slot(io.cu_id)(io.alloc_wg_slot_id) := io.wf_count_in
        //printf(p"prev: ${wf_count_out_i(io.cu_id)}\n")
    }
    .elsewhen(io.dealloc_en){
        wf_count_out_i(io.cu_id) := wf_count_out_i(io.cu_id) + wf_count_per_wg_slot(io.cu_id)(io.dealloc_wg_slot_id)
        //printf(p"wf count prev: ${wf_count_out_i(io.cu_id)}\n")
    }
    val cu_id_delay = RegInit(0.U(CU_ID_WIDTH.W))
    cu_id_delay := io.cu_id
    io.wf_count_out := wf_count_out_i(cu_id_delay)

/*
    
    val debug_alloc_en = RegInit(false.B)
    debug_alloc_en := io.alloc_en
    when(debug_alloc_en){
        printf(p"wf count after: ${wf_count_out_i(cu_id_delay)}\n")
    }
    */
}
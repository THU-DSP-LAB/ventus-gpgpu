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

class throttling_engine(val NUMBER_CU: Int, val CU_ID_WIDTH: Int, val WG_SLOT_ID_WIDTH: Int, val INIT_MAX_WG_COUNT: Int) extends Module{
    val io = IO(new Bundle{
        val cu_id = Input(UInt(CU_ID_WIDTH.W))
        val alloc_en = Input(Bool())
        val dealloc_en = Input(Bool())
        val wg_max_update = Input(UInt((WG_SLOT_ID_WIDTH + 1).W))
        val wg_max_update_valid = Input(Bool())
        val wg_max_update_all_cu = Input(Bool())
        val wg_max_update_cu_id = Input(UInt(CU_ID_WIDTH.W))
        val wg_count_available = Output(UInt((WG_SLOT_ID_WIDTH + 1).W))
    })
    val wg_count_max_array = RegInit(VecInit(Seq.fill(NUMBER_CU){INIT_MAX_WG_COUNT.U((WG_SLOT_ID_WIDTH + 1).W)}))
    val actual_wg_count_array = RegInit(VecInit(Seq.fill(NUMBER_CU){0.U((WG_SLOT_ID_WIDTH + 1).W)}))
    when(io.wg_max_update_valid){
        wg_count_max_array(io.wg_max_update_cu_id) := io.wg_max_update
        when(io.wg_max_update_all_cu){
            for(i <- 0 until NUMBER_CU){
                wg_count_max_array(i) := io.wg_max_update
            }
        }
    }
    val cu_id_i = RegInit(0.U(CU_ID_WIDTH.W))
    val alloc_en_i = RegInit(false.B)
    val dealloc_en_i = RegInit(false.B)
    alloc_en_i := io.alloc_en
    dealloc_en_i := io.dealloc_en
    when(io.alloc_en || io.dealloc_en){
        cu_id_i := io.cu_id
    }
    val wg_count_available_i = RegInit(0.U((WG_SLOT_ID_WIDTH + 1).W))
    io.wg_count_available := wg_count_available_i
    when(wg_count_max_array(cu_id_i) > actual_wg_count_array(cu_id_i)){
        wg_count_available_i := wg_count_max_array(cu_id_i) - actual_wg_count_array(cu_id_i)
    }
    .otherwise{
        wg_count_available_i := 0.U
    }
    when(alloc_en_i){
        actual_wg_count_array(cu_id_i) := actual_wg_count_array(cu_id_i) + 1.U
    }
    when(dealloc_en_i){
        actual_wg_count_array(cu_id_i) := actual_wg_count_array(cu_id_i) - 1.U
    }
    //debug print actual_wg_count_array
    //printf(p"actual_wg_count_array: ${actual_wg_count_array}\n")
}
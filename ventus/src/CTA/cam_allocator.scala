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

class cam_allocator(val CU_ID_WIDTH: Int, val NUMBER_CU: Int, val RES_ID_WIDTH: Int) extends Module{
    val io = IO(new Bundle{
        val res_search_out = Output(UInt(NUMBER_CU.W))
        val res_search_en = Input(Bool())
        //??
        val res_search_size = Input(UInt((RES_ID_WIDTH + 1).W))
        val cam_wr_en = Input(Bool())
        val cam_wr_addr = Input(UInt(CU_ID_WIDTH.W))
        //??
        val cam_wr_data = Input(UInt((RES_ID_WIDTH + 1).W))
    })
    val res_search_en_i = RegInit(false.B)
    val res_search_size_i = RegInit(0.U((RES_ID_WIDTH + 1).W))
    val cam_valid_entry = RegInit(VecInit(Seq.fill(NUMBER_CU)(false.B)))
    val cam_ram = RegInit(VecInit(Seq.fill(NUMBER_CU)(0.U((RES_ID_WIDTH + 1).W))))
    val decoded_output = Wire(Vec(NUMBER_CU, Bool()))
    res_search_en_i := io.res_search_en
    res_search_size_i := io.res_search_size
    for(i <- 0 until NUMBER_CU){
        when(!res_search_en_i){
            decoded_output(i) := false.B
        }
        .otherwise{
            when(!cam_valid_entry(i)){
                decoded_output(i) := true.B
            }
            .otherwise{
                when(cam_ram(i) >= res_search_size_i){
                    decoded_output(i) := true.B
                }
                .otherwise{
                    decoded_output(i) := false.B
                }
            }
        }
    }
    when(io.cam_wr_en){
        cam_ram(io.cam_wr_addr) := io.cam_wr_data
        cam_valid_entry(io.cam_wr_addr) := true.B
    }
    io.res_search_out := decoded_output.asUInt
}
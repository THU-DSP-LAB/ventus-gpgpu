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
import chisel3.util.log2Ceil

class prefer_select(val RANGE: Int, val ID_WIDTH: Int) extends RawModule{
    val io = IO(new Bundle{
        val signal = Vec(RANGE, Input(Bool()))
        val prefer = Input(UInt(ID_WIDTH.W))
        val valid = Output(Bool())
        val id = Output(UInt(ID_WIDTH.W))
    })
    val found = Wire(Bool())
    val found_id = Wire(UInt((ID_WIDTH + 1).W))
    val prefer_pre = if (RANGE == 1) 0.U else Wire(UInt(log2Ceil(RANGE).W))


    if (RANGE > 1) {
        prefer_pre := io.prefer(log2Ceil(RANGE)-1, 0)
    }

    found := false.B
    found_id := 0.U
//    for(i <- 1 until RANGE + 1){
//        when(i.U((ID_WIDTH + 1).W) + io.prefer >= RANGE.U((ID_WIDTH + 1).W)){
//            when(io.signal(i.U((ID_WIDTH + 1).W) + io.prefer - RANGE.U((ID_WIDTH + 1).W))){
//                found := true.B
//                found_id := i.U((ID_WIDTH + 1).W) + io.prefer - RANGE.U((ID_WIDTH + 1).W)
//            }
//        }
//        .otherwise{
//            when(io.signal(i.U + io.prefer)){
//                found := true.B
//                found_id := i.U + io.prefer
//            }
//        }
//    }
        // for(i <- 0 until RANGE ){
        //         when(io.signal((i.U + prefer_pre)(log2Ceil(RANGE)-1,0))){
        //             found := true.B
        //             found_id := (i.U + prefer_pre)(log2Ceil(RANGE)-1,0)
        //         }
        // }
        if (RANGE > 1) {
            for (i <- 0 until RANGE) {
                when(io.signal((i.U + prefer_pre)(log2Ceil(RANGE) - 1, 0))) {
                    found := true.B
                    found_id := (i.U + prefer_pre)(log2Ceil(RANGE) - 1, 0)
                }
            }
        } else {
            // When RANGE is 1, simply check the only possible index 0
            when(io.signal(0.U)) {
                found := true.B
                found_id := 0.U
            }
        }


    io.valid := found
    io.id := found_id(ID_WIDTH - 1, 0)
}
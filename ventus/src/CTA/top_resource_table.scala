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

class top_resource_table(val NUMBER_CU: Int, val CU_ID_WIDTH: Int, val RES_TABLE_ADDR_WIDTH: Int, val VGPR_ID_WIDTH: Int, val NUMBER_VGPR_SLOTS: Int, val SGPR_ID_WIDTH: Int, val NUMBER_SGPR_SLOTS: Int, val LDS_ID_WIDTH: Int, val NUMBER_LDS_SLOTS: Int, val WG_ID_WIDTH: Int, val WF_COUNT_WIDTH: Int, val WG_SLOT_ID_WIDTH: Int, val NUMBER_WF_SLOTS: Int, val WF_COUNT_MAX: Int, val NUMBER_RES_TABLE: Int, val INIT_MAX_WG_COUNT: Int, val WF_COUNT_WIDTH_PER_WG: Int) extends Module{
    val io = IO(new Bundle{
        val grt_cam_up_valid = Output(Bool())
        val grt_cam_up_wf_count = Output(UInt(WF_COUNT_WIDTH.W))
        val grt_cam_up_cu_id = Output(UInt(CU_ID_WIDTH.W))
        val grt_cam_up_vgpr_strt = Output(UInt((VGPR_ID_WIDTH - 1).W))
        val grt_cam_up_vgpr_size = Output(UInt(VGPR_ID_WIDTH.W))
        val grt_cam_up_sgpr_strt = Output(UInt((SGPR_ID_WIDTH - 1).W))
        val grt_cam_up_sgpr_size = Output(UInt(SGPR_ID_WIDTH.W))
        val grt_cam_up_lds_strt = Output(UInt((LDS_ID_WIDTH - 1).W))
        val grt_cam_up_lds_size = Output(UInt(LDS_ID_WIDTH.W))
        val grt_cam_up_wg_count = Output(UInt((WG_SLOT_ID_WIDTH + 1).W))
        val grt_wg_alloc_done = Output(Bool())
        val grt_wg_alloc_wg_id = Output(UInt(WG_ID_WIDTH.W))
        val grt_wg_alloc_cu_id = Output(UInt(CU_ID_WIDTH.W))
        val grt_wg_dealloc_done = Output(Bool())
        val grt_wg_dealloc_wg_id = Output(UInt(WG_ID_WIDTH.W))
        val grt_wg_dealloc_cu_id = Output(UInt(CU_ID_WIDTH.W))
        val gpu_interface_cu_id = Input(UInt(CU_ID_WIDTH.W))
        val gpu_interface_dealloc_wg_id = Input(UInt(WG_ID_WIDTH.W))
        val dis_controller_wg_alloc_valid = Input(Bool())
        val dis_controller_wg_dealloc_valid = Input(Bool())
        val allocator_wg_id_out = Input(UInt(WG_ID_WIDTH.W))
        val allocator_wf_count = Input(UInt(WF_COUNT_WIDTH_PER_WG.W))
        val allocator_cu_id_out = Input(UInt(CU_ID_WIDTH.W))
        val allocator_vgpr_start_out = Input(UInt((VGPR_ID_WIDTH - 1).W))
        val allocator_vgpr_size_out = Input(UInt(VGPR_ID_WIDTH.W))
        val allocator_sgpr_start_out = Input(UInt((SGPR_ID_WIDTH - 1).W))
        val allocator_sgpr_size_out = Input(UInt(SGPR_ID_WIDTH.W))
        val allocator_lds_start_out = Input(UInt((LDS_ID_WIDTH - 1).W))
        val allocator_lds_size_out = Input(UInt(LDS_ID_WIDTH.W))
    })
    val done_array = RegInit(VecInit(Seq.fill(NUMBER_RES_TABLE){false.B}))
    val wf_count_array = RegInit(VecInit(Seq.fill(NUMBER_RES_TABLE){0.U(WF_COUNT_WIDTH.W)}))
    val wg_count_array = RegInit(VecInit(Seq.fill(NUMBER_RES_TABLE){0.U((WG_SLOT_ID_WIDTH + 1).W)}))
    val vgpr_start_array = RegInit(VecInit(Seq.fill(NUMBER_RES_TABLE){0.U(VGPR_ID_WIDTH.W)}))
    val vgpr_size_array = RegInit(VecInit(Seq.fill(NUMBER_RES_TABLE){0.U(VGPR_ID_WIDTH.W)}))
    val sgpr_start_array = RegInit(VecInit(Seq.fill(NUMBER_RES_TABLE){0.U(SGPR_ID_WIDTH.W)}))
    val sgpr_size_array = RegInit(VecInit(Seq.fill(NUMBER_RES_TABLE){0.U(SGPR_ID_WIDTH.W)}))
    val lds_start_array = RegInit(VecInit(Seq.fill(NUMBER_RES_TABLE){0.U(LDS_ID_WIDTH.W)}))
    val lds_size_array = RegInit(VecInit(Seq.fill(NUMBER_RES_TABLE){0.U(LDS_ID_WIDTH.W)}))
    val wg_id_array = RegInit(VecInit(Seq.fill(NUMBER_RES_TABLE){0.U(WG_ID_WIDTH.W)}))
    val cu_id_array = RegInit(VecInit(Seq.fill(NUMBER_RES_TABLE){0.U(CU_ID_WIDTH.W)}))
    val serviced_array = RegInit(VecInit(Seq.fill(NUMBER_RES_TABLE){false.B}))
    val is_alloc_array = RegInit(VecInit(Seq.fill(NUMBER_RES_TABLE){false.B}))
    val done_cancelled_array = RegInit(VecInit(Seq.fill(NUMBER_RES_TABLE){false.B}))
    val command_serviced_array_cancelled = RegInit(VecInit(Seq.fill(NUMBER_RES_TABLE){false.B}))
    for(i <- 0 until NUMBER_RES_TABLE){
        val rt_group = Module(new resource_table_group(NUMBER_CU / NUMBER_RES_TABLE, CU_ID_WIDTH, RES_TABLE_ADDR_WIDTH, VGPR_ID_WIDTH, NUMBER_VGPR_SLOTS, SGPR_ID_WIDTH, NUMBER_SGPR_SLOTS, LDS_ID_WIDTH, NUMBER_LDS_SLOTS, WG_ID_WIDTH, WF_COUNT_WIDTH, WG_SLOT_ID_WIDTH, NUMBER_WF_SLOTS, WF_COUNT_MAX, INIT_MAX_WG_COUNT, WF_COUNT_WIDTH_PER_WG))
        rt_group.io.lds_start_in := Cat(0.U(1.W), io.allocator_lds_start_out)
        rt_group.io.lds_size_in := io.allocator_lds_size_out
        rt_group.io.vgpr_start_in := Cat(0.U(1.W), io.allocator_vgpr_start_out)
        rt_group.io.vgpr_size_in := io.allocator_vgpr_size_out
        rt_group.io.sgpr_start_in := Cat(0.U(1.W), io.allocator_sgpr_start_out)
        rt_group.io.sgpr_size_in := io.allocator_sgpr_size_out
        rt_group.io.wf_count_in := io.allocator_wf_count
        rt_group.io.done_cancelled := done_cancelled_array(i)
        when(io.dis_controller_wg_alloc_valid){
            rt_group.io.wg_id := io.allocator_wg_id_out
            rt_group.io.sub_cu_id := io.allocator_cu_id_out(CU_ID_WIDTH - RES_TABLE_ADDR_WIDTH - 1, 0)
        }
        .otherwise{
            rt_group.io.wg_id := io.gpu_interface_dealloc_wg_id
            rt_group.io.sub_cu_id := io.gpu_interface_cu_id(CU_ID_WIDTH - RES_TABLE_ADDR_WIDTH - 1, 0)
        }
        when(io.dis_controller_wg_alloc_valid && io.allocator_cu_id_out(CU_ID_WIDTH - 1, CU_ID_WIDTH - RES_TABLE_ADDR_WIDTH) === i.U){
            rt_group.io.alloc_en := true.B
            wg_id_array(i) := io.allocator_wg_id_out
            cu_id_array(i) := io.allocator_cu_id_out
            command_serviced_array_cancelled(i) := true.B
            done_cancelled_array(i) := false.B
            is_alloc_array(i) := true.B
        }
        .otherwise{
            rt_group.io.alloc_en := false.B
        }
        when(io.dis_controller_wg_dealloc_valid && io.gpu_interface_cu_id(CU_ID_WIDTH - 1, CU_ID_WIDTH - RES_TABLE_ADDR_WIDTH) === i.U){
            rt_group.io.dealloc_en := true.B
            wg_id_array(i) := io.gpu_interface_dealloc_wg_id
            cu_id_array(i) := io.gpu_interface_cu_id
            command_serviced_array_cancelled(i) := true.B
            done_cancelled_array(i) := false.B
            is_alloc_array(i) := false.B
        }
        .otherwise{
            rt_group.io.dealloc_en := false.B
        }
        done_array(i) := rt_group.io.res_tbl_done
        wf_count_array(i) := rt_group.io.wf_count
        wg_count_array(i) := rt_group.io.wg_count
        vgpr_start_array(i) := rt_group.io.vgpr_start
        vgpr_size_array(i) := rt_group.io.vgpr_size
        sgpr_start_array(i) := rt_group.io.sgpr_start
        sgpr_size_array(i) := rt_group.io.sgpr_size
        lds_start_array(i) := rt_group.io.lds_start
        lds_size_array(i) := rt_group.io.lds_size
        when(command_serviced_array_cancelled(i)){
            serviced_array(i) := false.B
            command_serviced_array_cancelled(i) := false.B
        }
    }
    val serviced_id = Wire(UInt(RES_TABLE_ADDR_WIDTH.W))
    val serviced_id_i = RegInit(0.U(RES_TABLE_ADDR_WIDTH.W))
    val serviced_id_valid = Wire(Bool())
    val serviced_id_valid_i = RegInit(false.B)
    serviced_id_valid := false.B
    serviced_id := 0.U
    for(i <- NUMBER_RES_TABLE - 1 to 0 by -1){
        when(done_array(i) && ~serviced_array(i)){
            serviced_id := i.U
            serviced_id_valid := true.B
        }
    }
    serviced_id_i := serviced_id
    serviced_id_valid_i := serviced_id_valid
    io.grt_cam_up_vgpr_strt := vgpr_start_array(serviced_id_i)(VGPR_ID_WIDTH - 1, 0)
    io.grt_cam_up_vgpr_size := vgpr_size_array(serviced_id_i)
    io.grt_cam_up_sgpr_strt := sgpr_start_array(serviced_id_i)(SGPR_ID_WIDTH - 1, 0)
    io.grt_cam_up_sgpr_size := sgpr_size_array(serviced_id_i)
    io.grt_cam_up_lds_strt := lds_start_array(serviced_id_i)(LDS_ID_WIDTH - 1, 0)
    io.grt_cam_up_lds_size := lds_size_array(serviced_id_i)
    io.grt_cam_up_wf_count := wf_count_array(serviced_id_i)
    io.grt_cam_up_wg_count := wg_count_array(serviced_id_i)
    io.grt_cam_up_cu_id := cu_id_array(serviced_id_i)
    io.grt_wg_alloc_cu_id := cu_id_array(serviced_id_i)
    io.grt_wg_alloc_wg_id := wg_id_array(serviced_id_i)
    io.grt_wg_dealloc_cu_id := cu_id_array(serviced_id_i)
    io.grt_wg_dealloc_wg_id := wg_id_array(serviced_id_i)
    val grt_cam_up_valid_i = RegInit(false.B)
    val grt_wg_alloc_done = RegInit(false.B)
    val grt_wg_dealloc_done = RegInit(false.B)
    when(serviced_id_valid){
        serviced_array(serviced_id) := true.B
        grt_cam_up_valid_i := true.B
        done_cancelled_array(serviced_id) := true.B
        //printf(p"ARR, serviced_id = $serviced_id\n")
        when(is_alloc_array(serviced_id)){
            grt_wg_alloc_done := true.B
            grt_wg_dealloc_done := false.B
        }
        .otherwise{
            grt_wg_dealloc_done := true.B
            grt_wg_alloc_done := false.B
        }
    }
    .otherwise{
        grt_cam_up_valid_i := false.B
        grt_wg_alloc_done := false.B
        grt_wg_dealloc_done := false.B
    }
    io.grt_cam_up_valid := grt_cam_up_valid_i
    io.grt_wg_alloc_done := grt_wg_alloc_done
    io.grt_wg_dealloc_done := grt_wg_dealloc_done

    /*
    printf(p"wf_count_array: ${wf_count_array}\n")
    when(grt_cam_up_valid_i){
        printf(p"ARR, get_cam_up_valid\n")
        printf(p"ARR, grt_cam_up_vgpr_size = ${io.grt_cam_up_vgpr_size}\n")
    }
    //print done_array and serviced_array
    printf(p"done_array: ${done_array}\n")
    printf(p"serviced_array: ${serviced_array}\n")
    */
}
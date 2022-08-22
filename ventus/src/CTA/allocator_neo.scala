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

//?? Require discussion
class allocator_neo(val WG_ID_WIDTH: Int, val CU_ID_WIDTH: Int, val NUMBER_CU: Int, val VGPR_ID_WIDTH: Int, val NUMBER_VGPR_SLOTS: Int, val SGPR_ID_WIDTH: Int, val NUMBER_SGPR_SLOTS: Int, val LDS_ID_WIDTH: Int, val NUMBER_LDS_SLOTS: Int, val NUMBER_WF_SLOTS: Int, val WF_COUNT_WIDTH: Int, val WG_SLOT_ID_WIDTH: Int, val WF_COUNT_WIDTH_PER_WG: Int) extends Module{
    val RAM_SIZE_WIDTH = VGPR_ID_WIDTH + SGPR_ID_WIDTH + LDS_ID_WIDTH
    val io = IO(new Bundle{
        val allocator_cu_valid = Output(Bool())
        val allocator_cu_rejected = Output(Bool())
        val allocator_wg_id_out = Output(UInt(WG_ID_WIDTH.W))
        val allocator_cu_id_out = Output(UInt(CU_ID_WIDTH.W))
        val allocator_wf_count = Output(UInt(WF_COUNT_WIDTH_PER_WG.W))
        val allocator_vgpr_size_out = Output(UInt((VGPR_ID_WIDTH + 1).W))
        val allocator_sgpr_size_out = Output(UInt((SGPR_ID_WIDTH + 1).W))
        val allocator_lds_size_out = Output(UInt((LDS_ID_WIDTH + 1).W))
        val allocator_vgpr_start_out = Output(UInt(VGPR_ID_WIDTH.W))
        val allocator_sgpr_start_out = Output(UInt(SGPR_ID_WIDTH.W))
        val allocator_lds_start_out = Output(UInt(LDS_ID_WIDTH.W))

        val inflight_wg_buffer_alloc_wg_id = Input(UInt(WG_ID_WIDTH.W))
        val inflight_wg_buffer_alloc_num_wf = Input(UInt(WF_COUNT_WIDTH_PER_WG.W))
        val inflight_wg_buffer_alloc_vgpr_size = Input(UInt((VGPR_ID_WIDTH + 1).W))
        val inflight_wg_buffer_alloc_sgpr_size = Input(UInt((SGPR_ID_WIDTH + 1).W))
        val inflight_wg_buffer_alloc_lds_size = Input(UInt((LDS_ID_WIDTH + 1).W))

        val dis_controller_cu_busy = Input(UInt(NUMBER_CU.W))
        val dis_controller_alloc_ack = Input(Bool())
        val dis_controller_start_alloc = Input(Bool())

        val grt_cam_up_valid = Input(Bool())
        val grt_cam_up_cu_id = Input(UInt(CU_ID_WIDTH.W))
        val grt_cam_up_vgpr_strt = Input(UInt(VGPR_ID_WIDTH.W))
        val grt_cam_up_vgpr_size = Input(UInt((VGPR_ID_WIDTH + 1).W))
        val grt_cam_up_sgpr_strt = Input(UInt(SGPR_ID_WIDTH.W))
        val grt_cam_up_sgpr_size = Input(UInt((SGPR_ID_WIDTH + 1).W))
        val grt_cam_up_lds_strt = Input(UInt(LDS_ID_WIDTH.W))
        val grt_cam_up_lds_size = Input(UInt((LDS_ID_WIDTH + 1).W))
        val grt_cam_up_wf_count = Input(UInt(WF_COUNT_WIDTH.W))
        val grt_cam_up_wg_count = Input(UInt((WG_SLOT_ID_WIDTH + 1).W))
    })
    val alloc_valid_i = RegInit(false.B)
    val alloc_wg_id_i = RegInit(0.U(WG_ID_WIDTH.W))
    val alloc_num_wf_i = RegInit(0.U(WF_COUNT_WIDTH_PER_WG.W))
    val alloc_vgpr_size_i = RegInit(0.U((VGPR_ID_WIDTH + 1).W))
    val alloc_sgpr_size_i = RegInit(0.U((SGPR_ID_WIDTH + 1).W))
    val alloc_lds_size_i = RegInit(0.U((LDS_ID_WIDTH + 1).W))
    val dis_controller_cu_busy_i = RegInit(0.U(NUMBER_CU.W))

    val cam_up_valid_i = RegInit(false.B)
    val cam_up_cu_id_i = RegInit(0.U(CU_ID_WIDTH.W))
    val cam_up_vgpr_strt_i = RegInit(0.U(VGPR_ID_WIDTH.W))
    val cam_up_vgpr_size_i = RegInit(0.U((VGPR_ID_WIDTH + 1).W))
    val cam_up_sgpr_strt_i = RegInit(0.U(SGPR_ID_WIDTH.W))
    val cam_up_sgpr_size_i = RegInit(0.U((SGPR_ID_WIDTH + 1).W))
    val cam_up_lds_strt_i = RegInit(0.U(LDS_ID_WIDTH.W))
    val cam_up_lds_size_i = RegInit(0.U((LDS_ID_WIDTH + 1).W))
    val cam_up_wf_count_i = RegInit(0.U(WF_COUNT_WIDTH.W))
    val cam_up_wg_count_i = RegInit(0.U((WG_SLOT_ID_WIDTH + 1).W))

    // cam outputs
    val cam_out_valid = RegInit(false.B)
    val vgpr_search_out = Wire(UInt(NUMBER_CU.W))
    val sgpr_search_out = Wire(UInt(NUMBER_CU.W))
    val lds_search_out = Wire(UInt(NUMBER_CU.W))
    val wf_search_out = Wire(UInt(NUMBER_CU.W))
    val wg_search_out = Wire(UInt(NUMBER_CU.W))
    
    // Signals that bypass the cam
    val cam_wait_valid = RegInit(false.B)
    val cam_wait_wg_id = RegInit(0.U(WG_ID_WIDTH.W))
    val cam_wait_wf_count = RegInit(0.U(WF_COUNT_WIDTH_PER_WG.W))
    val cam_wait_vgpr_size = RegInit(0.U((VGPR_ID_WIDTH + 1).W))
    val cam_wait_sgpr_size = RegInit(0.U((SGPR_ID_WIDTH + 1).W))
    val cam_wait_lds_size = RegInit(0.U((LDS_ID_WIDTH + 1).W))
    val cam_wait_dis_controller_cu_busy = RegInit(0.U(NUMBER_CU.W))

    // And cam outputs to check if there is anything we can use, choose the right cu
    val anded_cam_out_valid = RegInit(false.B)
    val anded_cam_out = RegInit(0.U(NUMBER_CU.W))
    val anded_cam_wg_id = RegInit(0.U(WG_ID_WIDTH.W))
    val anded_cam_wf_count = RegInit(0.U(WF_COUNT_WIDTH_PER_WG.W))
    val anded_cam_vgpr_size = RegInit(0.U((VGPR_ID_WIDTH + 1).W))
    val anded_cam_sgpr_size = RegInit(0.U((SGPR_ID_WIDTH + 1).W))
    val anded_cam_lds_size = RegInit(0.U((LDS_ID_WIDTH + 1).W))

    // Output encoder and find if we can use any cu, also addr the res start ram
    val encoded_cu_out_valid = RegInit(false.B)
    val encoded_cu_found_valid = RegInit(false.B)
    val encoded_cu_found_valid_comb = Wire(Bool())
    val encoded_cu_id = RegInit(0.U(CU_ID_WIDTH.W))
    val encoded_cu_id_comb = Wire(UInt(CU_ID_WIDTH.W))
    val encoded_cu_wg_id = RegInit(0.U(WG_ID_WIDTH.W))
    val encoded_wf_count = RegInit(0.U(WF_COUNT_WIDTH_PER_WG.W))
    val encoded_vgpr_size = RegInit(0.U((VGPR_ID_WIDTH + 1).W))
    val encoded_sgpr_size = RegInit(0.U((SGPR_ID_WIDTH + 1).W))
    val encoded_lds_size = RegInit(0.U((LDS_ID_WIDTH + 1).W))
    val encoded_vgpr_start = RegInit(0.U(VGPR_ID_WIDTH.W))
    val encoded_sgpr_start = RegInit(0.U(SGPR_ID_WIDTH.W))
    val encoded_lds_start = RegInit(0.U(LDS_ID_WIDTH.W))

    // res size ram lookup
    val size_ram_valid = RegInit(false.B)
    val size_ram_cu_id_found = RegInit(false.B)
    val cu_id_out = RegInit(0.U(CU_ID_WIDTH.W))
    val vgpr_start_out = RegInit(0.U(VGPR_ID_WIDTH.W))
    val sgpr_start_out = RegInit(0.U(SGPR_ID_WIDTH.W))
    val lds_start_out = RegInit(0.U(LDS_ID_WIDTH.W))
    val wg_id_out = RegInit(0.U(WG_ID_WIDTH.W))
    val wf_count_out = RegInit(0.U(WF_COUNT_WIDTH_PER_WG.W))
    val vgpr_size_out = RegInit(0.U((VGPR_ID_WIDTH + 1).W))
    val sgpr_size_out = RegInit(0.U((SGPR_ID_WIDTH + 1).W))
    val lds_size_out = RegInit(0.U((LDS_ID_WIDTH + 1).W))


    val RES_SIZE_VGPR_START = 0
    val RES_SIZE_VGPR_END = RES_SIZE_VGPR_START+ VGPR_ID_WIDTH-1

    val RES_SIZE_SGPR_START = RES_SIZE_VGPR_END + 1
    val RES_SIZE_SGPR_END = RES_SIZE_SGPR_START+ SGPR_ID_WIDTH-1

    val RES_SIZE_LDS_START = RES_SIZE_SGPR_END + 1
    val RES_SIZE_LDS_END = RES_SIZE_LDS_START+ LDS_ID_WIDTH-1

    val cu_initialized = RegInit(VecInit(Seq.fill(NUMBER_CU)(false.B)))

    val pipeline_waiting = RegInit(false.B)

    val vgpr_cam = Module(new cam_allocator_neo(CU_ID_WIDTH, NUMBER_CU, VGPR_ID_WIDTH))
    val sgpr_cam = Module(new cam_allocator_neo(CU_ID_WIDTH, NUMBER_CU, SGPR_ID_WIDTH))
    val lds_cam = Module(new cam_allocator_neo(CU_ID_WIDTH, NUMBER_CU, LDS_ID_WIDTH))
    val wf_cam = Module(new cam_allocator(CU_ID_WIDTH, NUMBER_CU, WF_COUNT_WIDTH))
    val wg_cam = Module(new cam_allocator(CU_ID_WIDTH, NUMBER_CU, WG_SLOT_ID_WIDTH + 1))
    val vgpr_cam_start_vec = Wire(Vec(NUMBER_CU, UInt(VGPR_ID_WIDTH.W)))
    val sgpr_cam_start_vec = Wire(Vec(NUMBER_CU, UInt(SGPR_ID_WIDTH.W)))
    val lds_cam_start_vec = Wire(Vec(NUMBER_CU, UInt(LDS_ID_WIDTH.W)))

    vgpr_cam.io.res_search_en := alloc_valid_i
    vgpr_cam.io.res_search_size := alloc_vgpr_size_i

    vgpr_search_out := vgpr_cam.io.res_search_out
    vgpr_cam_start_vec := vgpr_cam.io.res_search_out_start

    vgpr_cam.io.cam_wr_en := cam_up_valid_i
    vgpr_cam.io.cam_wr_addr := cam_up_cu_id_i
    vgpr_cam.io.cam_wr_data := cam_up_vgpr_size_i
    vgpr_cam.io.cam_wr_start := cam_up_vgpr_strt_i

    sgpr_cam.io.res_search_en := alloc_valid_i
    sgpr_cam.io.res_search_size := alloc_sgpr_size_i

    sgpr_search_out := sgpr_cam.io.res_search_out
    sgpr_cam_start_vec := sgpr_cam.io.res_search_out_start

    sgpr_cam.io.cam_wr_en := cam_up_valid_i
    sgpr_cam.io.cam_wr_addr := cam_up_cu_id_i
    sgpr_cam.io.cam_wr_data := cam_up_sgpr_size_i
    sgpr_cam.io.cam_wr_start := cam_up_sgpr_strt_i

    lds_cam.io.res_search_en := alloc_valid_i
    lds_cam.io.res_search_size := alloc_lds_size_i

    lds_search_out := lds_cam.io.res_search_out
    lds_cam_start_vec := lds_cam.io.res_search_out_start

    lds_cam.io.cam_wr_en := cam_up_valid_i
    lds_cam.io.cam_wr_addr := cam_up_cu_id_i
    lds_cam.io.cam_wr_data := cam_up_lds_size_i
    lds_cam.io.cam_wr_start := cam_up_lds_strt_i

    wf_cam.io.res_search_en := alloc_valid_i
    //?? Cat
    wf_cam.io.res_search_size := alloc_num_wf_i

    wf_search_out := wf_cam.io.res_search_out

    wf_cam.io.cam_wr_en := cam_up_valid_i
    wf_cam.io.cam_wr_addr := cam_up_cu_id_i
    wf_cam.io.cam_wr_data := cam_up_wf_count_i

    wg_cam.io.res_search_en := alloc_valid_i
    wg_cam.io.res_search_size := 1.U
    wg_search_out := wg_cam.io.res_search_out
    wg_cam.io.cam_wr_en := cam_up_valid_i
    wg_cam.io.cam_wr_addr := cam_up_cu_id_i
    wg_cam.io.cam_wr_data := cam_up_wg_count_i


    val encoded_vgpr_start_comb = Wire(UInt(VGPR_ID_WIDTH.W))
    val encoded_sgpr_start_comb = Wire(UInt(SGPR_ID_WIDTH.W))
    val encoded_lds_start_comb = Wire(UInt(LDS_ID_WIDTH.W))

    when(encoded_cu_found_valid && !pipeline_waiting){
        pipeline_waiting := true.B
    }
    when(io.dis_controller_alloc_ack){
        pipeline_waiting := false.B
    }

    when(!pipeline_waiting){

        alloc_valid_i := io.dis_controller_start_alloc
        alloc_wg_id_i := io.inflight_wg_buffer_alloc_wg_id
        alloc_num_wf_i := io.inflight_wg_buffer_alloc_num_wf
        alloc_vgpr_size_i := io.inflight_wg_buffer_alloc_vgpr_size
        alloc_sgpr_size_i := io.inflight_wg_buffer_alloc_sgpr_size
        alloc_lds_size_i := io.inflight_wg_buffer_alloc_lds_size
        dis_controller_cu_busy_i := io.dis_controller_cu_busy

        //pipeline, Wait for cam search
        cam_wait_valid := alloc_valid_i
        cam_wait_wg_id := alloc_wg_id_i
        cam_wait_wf_count := alloc_num_wf_i
        cam_wait_vgpr_size := alloc_vgpr_size_i
        cam_wait_sgpr_size := alloc_sgpr_size_i
        cam_wait_lds_size := alloc_lds_size_i
        cam_wait_dis_controller_cu_busy := dis_controller_cu_busy_i

        // AND all cam outs
        anded_cam_out_valid := cam_wait_valid
        anded_cam_out := vgpr_search_out & sgpr_search_out & lds_search_out & wf_search_out & wg_search_out & ~cam_wait_dis_controller_cu_busy
        anded_cam_wg_id := cam_wait_wg_id
        anded_cam_wf_count := cam_wait_wf_count
        anded_cam_vgpr_size := cam_wait_vgpr_size
        anded_cam_sgpr_size := cam_wait_sgpr_size
        anded_cam_lds_size := cam_wait_lds_size

        // Use the encoded output to find the start of the resources
        encoded_cu_out_valid := anded_cam_out_valid
        encoded_cu_found_valid := encoded_cu_found_valid_comb
        encoded_cu_id := encoded_cu_id_comb
        encoded_wf_count := anded_cam_wf_count
        encoded_cu_wg_id := anded_cam_wg_id
        encoded_vgpr_size := anded_cam_vgpr_size
        encoded_sgpr_size := anded_cam_sgpr_size
        encoded_lds_size := anded_cam_lds_size
        encoded_lds_start := encoded_lds_start_comb
        encoded_vgpr_start := encoded_vgpr_start_comb
        encoded_sgpr_start := encoded_sgpr_start_comb


        // Output the starts and the cu id
        size_ram_valid := encoded_cu_out_valid
        size_ram_cu_id_found := encoded_cu_found_valid
        cu_id_out := encoded_cu_id
        wf_count_out := encoded_wf_count
        wg_id_out := encoded_cu_wg_id
        vgpr_size_out := encoded_vgpr_size
        sgpr_size_out := encoded_sgpr_size
        lds_size_out := encoded_lds_size
        lds_start_out := encoded_lds_start
        vgpr_start_out := encoded_vgpr_start
        sgpr_start_out := encoded_sgpr_start
    }

    cam_up_valid_i := io.grt_cam_up_valid
    cam_up_cu_id_i := io.grt_cam_up_cu_id
    cam_up_wf_count_i := io.grt_cam_up_wf_count
    cam_up_wg_count_i := io.grt_cam_up_wg_count
    cam_up_vgpr_strt_i := io.grt_cam_up_vgpr_strt
    cam_up_sgpr_strt_i := io.grt_cam_up_sgpr_strt
    cam_up_lds_strt_i := io.grt_cam_up_lds_strt
    when(cam_up_valid_i){
        cu_initialized(cam_up_cu_id_i) := true.B
    }
    cam_up_lds_size_i := io.grt_cam_up_lds_size
    cam_up_vgpr_size_i := io.grt_cam_up_vgpr_size
    cam_up_sgpr_size_i := io.grt_cam_up_sgpr_size

    io.allocator_cu_valid := size_ram_valid
    io.allocator_cu_rejected := !size_ram_cu_id_found
    io.allocator_cu_id_out := cu_id_out
    io.allocator_wg_id_out := wg_id_out
    io.allocator_wf_count := wf_count_out

    io.allocator_vgpr_size_out := vgpr_size_out
    io.allocator_sgpr_size_out := sgpr_size_out
    io.allocator_lds_size_out := lds_size_out

    when(!cu_initialized(cu_id_out)){
        io.allocator_vgpr_start_out := 0.U
        io.allocator_sgpr_start_out := 0.U
        io.allocator_lds_start_out := 0.U
    }
    .otherwise{
        io.allocator_vgpr_start_out := vgpr_start_out
        io.allocator_sgpr_start_out := sgpr_start_out
        io.allocator_lds_start_out := lds_start_out
    }
    /*
    val found_valid = Wire(Bool())
    found_valid := false.B
    encoded_cu_id_comb := 0.U
    encoded_cu_found_valid_comb := false.B

    for(i <- NUMBER_CU - 1 to 0 by -1){
        when(anded_cam_out(i) === 1.U){
            encoded_cu_id_comb := i.U
            found_valid := true.B
        }
    }
    */
    val prefer_select = Module(new prefer_select(NUMBER_CU, CU_ID_WIDTH))
    prefer_select.io.signal := anded_cam_out.asBools
    prefer_select.io.prefer := anded_cam_wg_id(CU_ID_WIDTH - 1, 0)

    encoded_sgpr_start_comb := sgpr_cam_start_vec(encoded_cu_id_comb)
    encoded_vgpr_start_comb := vgpr_cam_start_vec(encoded_cu_id_comb)
    encoded_lds_start_comb := lds_cam_start_vec(encoded_cu_id_comb)
    encoded_cu_found_valid_comb := prefer_select.io.valid
    encoded_cu_id_comb := prefer_select.io.id
    
    //debug output
    //printf(p"c: anded_cam_out: $anded_cam_out\n")
    //printf(p"c: vgpr_search_out: $vgpr_search_out\n")
    //printf(p"c: sgpr_search_out: $sgpr_search_out\n")
    //printf(p"c: lds_search_out: $lds_search_out\n")
    //printf(p"c: wf_search_out: $wf_search_out\n")
    //printf(p"c: wg_search_out: $wg_search_out\n")
    //printf(p"c: encoded cu id: $encoded_cu_id\n")
    //printf(p"c: prefer: ${anded_cam_wg_id(CU_ID_WIDTH - 1, 0)}\n")
    //printf(p"c: anded_cam_wg_id: $anded_cam_wg_id\n")
    //when(size_ram_valid && size_ram_cu_id_found){
    //    printf(p"c: out: cu_id_out: $cu_id_out\n")
    //    printf(p"c: out: wg_id_out: $wg_id_out\n")
    //}
}
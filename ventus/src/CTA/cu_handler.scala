package CTA

import chisel3._
import chisel3.util._

//?? Require discussion
class cu_handler(val WF_COUNT_WIDTH: Int, val WG_ID_WIDTH: Int, val WG_SLOT_ID_WIDTH: Int, val NUMBER_WF_SLOTS: Int, val TAG_WIDTH: Int, val WF_COUNT_WIDTH_PER_WG: Int) extends Module {
    val io = IO(new Bundle{
        val wg_alloc_en = Input(Bool())
        val wg_alloc_wg_id = Input(UInt(WG_ID_WIDTH.W))
        val wg_alloc_wf_count = Input(UInt(WF_COUNT_WIDTH_PER_WG.W))

        val ready_for_dispatch2cu = Input(Bool())
        val dispatch2cu_wf_dispatch = Output(Bool())
        val dispatch2cu_wf_tag_dispatch = Output(UInt(TAG_WIDTH.W))

        val cu2dispatch_wf_done_i = Input(Bool())
        val cu2dispatch_wf_tag_done_i = Input(UInt(TAG_WIDTH.W))

        val wg_done_ack = Input(Bool())
        val wg_done_valid = Output(Bool())
        val wg_done_wg_id = Output(UInt(WG_ID_WIDTH.W))
        val invalid_due_to_not_ready = Output(Bool())
    })
    val TAG_WF_COUNT_L = 0
    val TAG_WF_COUNT_H = TAG_WF_COUNT_L + WF_COUNT_WIDTH_PER_WG - 1
    val TAG_WG_SLOT_ID_L = TAG_WF_COUNT_H + 1
    val TAG_WG_SLOT_ID_H = TAG_WG_SLOT_ID_L + WG_SLOT_ID_WIDTH - 1

    // On alloc:
    // Get first wf free slot, slot of first wf is slot of cu
    // zero counter, store wf_id and wf_count
    // outpus tag of each cu

    val next_free_slot = RegInit(0.U(WG_SLOT_ID_WIDTH.W))
    val next_free_slot_comb = Wire(UInt(WG_SLOT_ID_WIDTH.W))
    val used_slot_bitmap = RegInit(VecInit(Seq.fill(NUMBER_WF_SLOTS)(false.B)))
    val pending_wg_bitmap = RegInit(VecInit(Seq.fill(NUMBER_WF_SLOTS)(false.B)))
    /*val pending_wf_bitmap = RegInit{
        val structure = Wire(Vec(NUMBER_WF_SLOTS, Vec(1 << WF_COUNT_WIDTH, Bool())))
        for(i <- 0 until NUMBER_WF_SLOTS){
            for(j <- 0 until 1 << WF_COUNT_WIDTH){
                structure(i)(j) := false.B
            }
        }
        structure
    }*/
    val pending_wf_count = RegInit(VecInit(Seq.fill(NUMBER_WF_SLOTS)(0.U(WF_COUNT_WIDTH_PER_WG.W))))
    val curr_alloc_wf_count = RegInit(0.U(WF_COUNT_WIDTH_PER_WG.W))
    val curr_alloc_wf_slot = RegInit(0.U(WG_SLOT_ID_WIDTH.W))
    val dispatch2cu_wf_dispatch_i = RegInit(false.B)
    val dispatch2cu_wf_tag_dispatch_i = RegInit(0.U(TAG_WIDTH.W))

    // On dealloc:
    // Look up counter
    // Check if wg finished
    // Notify gpu_interface

    val next_served_dealloc_valid = RegInit(false.B)
    val next_served_dealloc_valid_comb = Wire(Bool())
    val next_served_dealloc = RegInit(0.U(WG_SLOT_ID_WIDTH.W))
    val next_served_dealloc_comb = Wire(UInt(WG_SLOT_ID_WIDTH.W))


    val INFO_RAM_WORD_WIDTH = WG_ID_WIDTH + WF_COUNT_WIDTH_PER_WG
    val INFO_RAM_WG_COUNT_L = 0
    val INFO_RAM_WG_COUNT_H = INFO_RAM_WG_COUNT_L + WF_COUNT_WIDTH_PER_WG - 1
    val INFO_RAM_WG_ID_L = INFO_RAM_WG_COUNT_H + 1
    val INFO_RAM_WG_ID_H = INFO_RAM_WG_ID_L + WG_ID_WIDTH - 1

    val curr_dealloc_wg_slot = RegInit(0.U(WG_SLOT_ID_WIDTH.W))
    val curr_dealloc_wf_counter = RegInit(0.U(WF_COUNT_WIDTH_PER_WG.W))
    val curr_dealloc_wf_id = RegInit(0.U(WG_ID_WIDTH.W))
    val info_ram_rd_en = RegInit(false.B)
    val info_ram_rd_valid = RegInit(false.B)
    val info_ram_rd_reg = Wire(UInt(INFO_RAM_WORD_WIDTH.W))
    val info_ram_wr_en = RegInit(false.B)
    val info_ram_wr_addr = RegInit(0.U(WG_SLOT_ID_WIDTH.W))
    val info_ram_wr_reg = RegInit(0.U(INFO_RAM_WORD_WIDTH.W))
    val wg_done_valid_i = RegInit(false.B)
    val wg_done_wg_id_i = RegInit(0.U(WG_ID_WIDTH.W))
    val curr_wf_count = RegInit(0.U(WF_COUNT_WIDTH_PER_WG.W))

    val NUM_ALLOC_ST = 2
    val ST_ALLOC_IDLE = 1 << 0
    val ST_ALLOCATING = 1 << 1
    val alloc_st = RegInit(ST_ALLOC_IDLE.U(NUM_ALLOC_ST.W))

    val NUM_DEALLOC_ST = 3
    val ST_DEALLOC_IDLE = 1 << 0
    val ST_DEALLOC_READ_RAM = 1 << 1
    val ST_DEALLOC_PROPAGATE = 1 << 2
    val dealloc_st = RegInit(ST_DEALLOC_IDLE.U(NUM_DEALLOC_ST.W))

    /*
    def get_wf_mask(wf_count: UInt): UInt = {
        val wf_mask = WireInit(VecInit(Seq.fill(1 << WF_COUNT_WIDTH){false.B}))
        for(i <- 0 until (1 << WF_COUNT_WIDTH)){
            when(wf_count <= i.U){
                wf_mask(i) := true.B
            }
            .otherwise{
                wf_mask(i) := false.B
            }
        }
        wf_mask.asUInt
    }*/
    val info_ram = Module(new RAM(INFO_RAM_WORD_WIDTH, WG_SLOT_ID_WIDTH, NUMBER_WF_SLOTS))
    info_ram.io.wr_en := info_ram_wr_en
    info_ram.io.wr_addr := info_ram_wr_addr
    info_ram.io.wr_word := info_ram_wr_reg
    info_ram.io.rd_en := info_ram_rd_en
    info_ram.io.rd_addr := curr_dealloc_wg_slot
    info_ram_rd_reg := info_ram.io.rd_word

    val invalid_due_to_not_ready_i = RegInit(false.B)

    info_ram_wr_en := false.B
    next_free_slot := next_free_slot_comb
    val found_free_slot_valid2 = WireInit(false.B)
    val next_free_slot_valid = RegInit(false.B)
    next_free_slot_valid := found_free_slot_valid2
    dispatch2cu_wf_dispatch_i := false.B
    invalid_due_to_not_ready_i := false.B

    //alloc state machine
    switch(alloc_st){
        is(ST_ALLOC_IDLE.U){
            when(io.wg_alloc_en && next_free_slot_valid){
                info_ram_wr_en := true.B
                info_ram_wr_addr := next_free_slot
                info_ram_wr_reg := Cat(io.wg_alloc_wg_id, io.wg_alloc_wf_count)
                curr_alloc_wf_count := io.wg_alloc_wf_count
                curr_alloc_wf_slot := next_free_slot
                used_slot_bitmap(next_free_slot) := true.B
                //pending_wf_bitmap(next_free_slot) := get_wf_mask(io.wg_alloc_wf_count).asBools
                pending_wf_count(next_free_slot) := io.wg_alloc_wf_count
                alloc_st := ST_ALLOCATING.U
            }
        }
        is(ST_ALLOCATING.U){
            when(curr_alloc_wf_count =/= 0.U){
                when(io.ready_for_dispatch2cu){
                    dispatch2cu_wf_dispatch_i := true.B
                    // Send the counter just to make sure the cu does not have
		            // two wf with the same tag
                    dispatch2cu_wf_tag_dispatch_i := Cat(curr_alloc_wf_slot, curr_alloc_wf_count - 1.U)
                    curr_alloc_wf_count := curr_alloc_wf_count - 1.U
                    invalid_due_to_not_ready_i := false.B
                }
                .otherwise{
                    dispatch2cu_wf_dispatch_i := false.B
                    invalid_due_to_not_ready_i := true.B
                }
            }
            .otherwise{
                alloc_st := ST_ALLOC_IDLE.U
                invalid_due_to_not_ready_i := false.B
            }
        }
    }
    next_served_dealloc_valid := next_served_dealloc_valid_comb
    next_served_dealloc := next_served_dealloc_comb
    info_ram_rd_en := false.B
    info_ram_rd_valid := info_ram_rd_en
    wg_done_valid_i := false.B

    //dealloc state machine
    switch(dealloc_st){
        is(ST_DEALLOC_IDLE.U){
            when(next_served_dealloc_valid){
                info_ram_rd_en := true.B
                curr_dealloc_wg_slot := next_served_dealloc
                curr_wf_count := pending_wf_count(next_served_dealloc)
                pending_wg_bitmap(next_served_dealloc) := false.B
                dealloc_st := ST_DEALLOC_READ_RAM.U
            }
        }
        is(ST_DEALLOC_READ_RAM.U){
            when(info_ram_rd_valid){
                when(curr_wf_count === 0.U){
                    wg_done_valid_i := true.B
                    wg_done_wg_id_i := info_ram_rd_reg(INFO_RAM_WG_ID_H, INFO_RAM_WG_ID_L)
                    used_slot_bitmap(curr_dealloc_wg_slot) := false.B
                    dealloc_st := ST_DEALLOC_PROPAGATE.U
                }
                .otherwise{
                    dealloc_st := ST_DEALLOC_IDLE.U
                }
            }
        }
        is(ST_DEALLOC_PROPAGATE.U){
            when(io.wg_done_ack){
                dealloc_st := ST_DEALLOC_IDLE.U
            }
            .otherwise{
                wg_done_valid_i := true.B
            }
        }
    }
    when(io.cu2dispatch_wf_done_i){
        pending_wg_bitmap(io.cu2dispatch_wf_tag_done_i(TAG_WG_SLOT_ID_H, TAG_WG_SLOT_ID_L)) := true.B
        //pending_wf_bitmap(io.cu2dispatch_wf_tag_done_i(TAG_WG_SLOT_ID_H, TAG_WG_SLOT_ID_L))(io.cu2dispatch_wf_tag_done_i(TAG_WF_COUNT_H, TAG_WF_COUNT_L)) := true.B
        pending_wf_count(io.cu2dispatch_wf_tag_done_i(TAG_WG_SLOT_ID_H, TAG_WG_SLOT_ID_L)) := pending_wf_count(io.cu2dispatch_wf_tag_done_i(TAG_WG_SLOT_ID_H, TAG_WG_SLOT_ID_L)) - 1.U
    }
    io.dispatch2cu_wf_dispatch := dispatch2cu_wf_dispatch_i
    io.dispatch2cu_wf_tag_dispatch := dispatch2cu_wf_tag_dispatch_i

    io.wg_done_valid := wg_done_valid_i
    io.wg_done_wg_id := wg_done_wg_id_i

    //finds the next served deallocation
    val found_free_slot_valid = WireInit(false.B)
    val found_free_slot_id = WireInit(0.U(WG_SLOT_ID_WIDTH.W))
    found_free_slot_valid := false.B
    found_free_slot_id := 0.U
    for(i <- NUMBER_WF_SLOTS - 1 to 0 by -1){
        when(pending_wg_bitmap(i)){
            found_free_slot_valid := true.B
            found_free_slot_id := i.U
        }
    }
    next_served_dealloc_valid_comb := found_free_slot_valid
    next_served_dealloc_comb := found_free_slot_id

    //finds next free slot
    
    val found_free_slot_id2 = WireInit(0.U(WG_SLOT_ID_WIDTH.W))
    found_free_slot_valid2 := false.B
    found_free_slot_id2 := 0.U
    for(i <- NUMBER_WF_SLOTS - 1 to 0 by -1){
        when(!used_slot_bitmap(i)){
            found_free_slot_valid2 := true.B
            found_free_slot_id2 := i.U
        }
    }
    next_free_slot_comb := found_free_slot_id2
    io.invalid_due_to_not_ready := invalid_due_to_not_ready_i
}
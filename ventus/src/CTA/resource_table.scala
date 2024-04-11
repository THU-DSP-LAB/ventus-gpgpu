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


//?? Require further check
class resource_table(val CU_ID_WIDTH: Int, val NUMBER_CU: Int, val WG_SLOT_ID_WIDTH: Int, val NUMBER_WF_SLOTS_PER_CU: Int, val RES_ID_WIDTH: Int, val NUMBER_RES_SLOTS: Int) extends Module{
  val io = IO(new Bundle{
    val res_table_done_o = Output(Bool())
    val cam_biggest_space_size = Output(UInt((RES_ID_WIDTH + 1).W))
    val cam_biggest_space_addr = Output(UInt(RES_ID_WIDTH.W))

    val alloc_res_en = Input(Bool())
    val dealloc_res_en = Input(Bool())
    val alloc_cu_id = Input(UInt(CU_ID_WIDTH.W))
    val dealloc_cu_id = Input(UInt(CU_ID_WIDTH.W))
    val alloc_wg_slot_id = Input(UInt(WG_SLOT_ID_WIDTH.W))
    val dealloc_wg_slot_id = Input(UInt(WG_SLOT_ID_WIDTH.W))
    val alloc_res_size = Input(UInt((RES_ID_WIDTH + 1).W))
    val alloc_res_start = Input(UInt(RES_ID_WIDTH.W))
  })

  val res_table_done = RegInit(false.B)
  io.res_table_done_o := res_table_done

  val WG_SLOT_ID_WIDTH_TABLE = WG_SLOT_ID_WIDTH + 1

  val TABLE_ADDR_WIDTH = WG_SLOT_ID_WIDTH_TABLE + CU_ID_WIDTH
  val TABLE_ENTRY_WIDTH = 2 * WG_SLOT_ID_WIDTH_TABLE + 2 * RES_ID_WIDTH + 1

  val RES_STRT_L = 0
  val RES_STRT_H = RES_ID_WIDTH-1
  val RES_SIZE_L = RES_STRT_H+1
  val RES_SIZE_H = RES_SIZE_L+RES_ID_WIDTH
  val PREV_ENTRY_L = RES_SIZE_H+1
  val PREV_ENTRY_H = PREV_ENTRY_L + WG_SLOT_ID_WIDTH_TABLE-1
  val NEXT_ENTRY_L = PREV_ENTRY_H + 1
  val NEXT_ENTRY_H = NEXT_ENTRY_L + WG_SLOT_ID_WIDTH_TABLE-1

  val alloc_res_en_i = RegInit(false.B)
  val dealloc_res_en_i = RegInit(false.B)
  val alloc_cu_id_i = RegInit(0.U(CU_ID_WIDTH.W))
  val dealloc_cu_id_i = RegInit(0.U(CU_ID_WIDTH.W))
  val alloc_wg_slot_id_i = RegInit(0.U(WG_SLOT_ID_WIDTH_TABLE.W))
  val dealloc_wg_slot_id_i = RegInit(0.U(WG_SLOT_ID_WIDTH_TABLE.W))
  //??
  val alloc_res_size_i = RegInit(0.U((RES_ID_WIDTH + 1).W))
  val alloc_res_start_i = RegInit(0.U(RES_ID_WIDTH.W))
  def get_new_entry(res_start: UInt,  res_size: UInt, prev_entry: UInt, next_entry: UInt): UInt = Cat(next_entry, prev_entry, res_size, res_start)
  def calc_table_addr(cu_id: UInt, wg_slot_id: UInt): UInt = {
    NUMBER_WF_SLOTS_PER_CU.U * cu_id + wg_slot_id
  }
  def get_prev_item_wg_slot(table_entry: UInt): UInt = {
    table_entry(PREV_ENTRY_H, PREV_ENTRY_L)
  }
  def get_next_item_wg_slot(table_entry: UInt): UInt = {
    table_entry(NEXT_ENTRY_H, NEXT_ENTRY_L)
  }
  def get_res_start(table_entry: UInt): UInt = {
    table_entry(RES_STRT_H, RES_STRT_L)
  }
  def get_res_size(table_entry: UInt): UInt = {
    table_entry(RES_SIZE_H, RES_SIZE_L)
  }
  def set_prev_item_wg_slot(prev_item_wg_slot: UInt, table_entry: UInt): UInt = Cat(table_entry(NEXT_ENTRY_H, NEXT_ENTRY_L), prev_item_wg_slot, table_entry(RES_SIZE_H, RES_SIZE_L), table_entry(RES_STRT_H, RES_STRT_L))
  def set_next_item_wg_slot(next_item_wg_slot: UInt, table_entry: UInt): UInt = Cat(next_item_wg_slot, table_entry(PREV_ENTRY_H, PREV_ENTRY_L), table_entry(RES_SIZE_H, RES_SIZE_L), table_entry(RES_STRT_H, RES_STRT_L))
  def get_free_res_start(table_entry: UInt): UInt = {
    get_res_start(table_entry) + get_res_size(table_entry)
  }
  def get_free_res_size(last_table_entry: UInt, table_entry: UInt): UInt = {
    table_entry(RES_STRT_H, RES_STRT_L) - (last_table_entry(RES_STRT_H, RES_STRT_L) + last_table_entry(RES_SIZE_H, RES_SIZE_L))
  }
  def get_free_res_size_last(table_entry: UInt):UInt = {
    NUMBER_RES_SLOTS.U - (table_entry(RES_STRT_H, RES_STRT_L) + table_entry(RES_SIZE_H, RES_SIZE_L))
  }
  val NUM_ENTRIES = NUMBER_CU * (NUMBER_WF_SLOTS_PER_CU + 1)
  val RES_TABLE_END_TABLE_U = (((1 << WG_SLOT_ID_WIDTH_TABLE) - 1).U)(WG_SLOT_ID_WIDTH_TABLE.W)
  val RES_TABLE_HEAD_POINTER_U = (((1 << WG_SLOT_ID_WIDTH_TABLE) - 2).U)(WG_SLOT_ID_WIDTH_TABLE.W)

  //RAM of resource table
  val resource_table_ram = RegInit(VecInit(Seq.fill(NUM_ENTRIES)(0.U(TABLE_ENTRY_WIDTH.W))))
  val table_head_pointer = RegInit(VecInit(Seq.fill(NUMBER_CU)(0.U(WG_SLOT_ID_WIDTH_TABLE.W))))
  val table_head_pointer_i = RegInit(0.U(WG_SLOT_ID_WIDTH_TABLE.W))

  val rtwr_res_strt = Wire(UInt(RES_ID_WIDTH.W))
  val rtrr_res_strt = Wire(UInt(RES_ID_WIDTH.W))
  val rtlrr_res_strt = Wire(UInt(RES_ID_WIDTH.W))
  val rtwr_res_size = Wire(UInt((RES_ID_WIDTH + 1).W))
  val rtrr_res_size = Wire(UInt((RES_ID_WIDTH + 1).W))
  val rtlrr_res_size = Wire(UInt((RES_ID_WIDTH + 1).W))
  val rtwr_prev_item = Wire(UInt(TABLE_ENTRY_WIDTH.W))
  val rtrr_prev_item = Wire(UInt(TABLE_ENTRY_WIDTH.W))
  val rtlrr_prev_item = Wire(UInt(TABLE_ENTRY_WIDTH.W))
  val rtwr_next_item = Wire(UInt(RES_ID_WIDTH.W))
  val rtrr_next_item = Wire(UInt(RES_ID_WIDTH.W))
  val rtlrr_next_item = Wire(UInt(RES_ID_WIDTH.W))

  // The states of four state machine
  // Main state machine
  val ST_M_IDLE = 1
  val ST_M_ALLOC = 2
  val ST_M_DEALLOC = 4
  val ST_M_FIND_MAX = 8
  val m_state = RegInit(ST_M_IDLE.U(4.W))

  // Alloc state machine
  val ST_A_IDLE = 1
  val ST_A_FIND_POSITION = 2
  val ST_A_UPDATE_PREV_ENTRY = 4
  val ST_A_WRITE_NEW_ENTRY = 8
  val a_state = RegInit(ST_A_IDLE.U(4.W))

  // Dealloc state machine
  val ST_D_IDLE = 1
  val ST_D_READ_PREV_ENTRY = 2
  val ST_D_READ_NEXT_ENTRY = 4
  val ST_D_UPDATE_PREV_ENTRY = 8
  val ST_D_UPDATE_NEXT_ENTRY = 16
  val d_state = RegInit(ST_D_IDLE.U(5.W))


  // Find max state machine
  val ST_F_IDLE = 1
  val ST_F_FIRST_ITEM = 2
  val ST_F_SEARCHING = 4
  val ST_F_LAST_ITEM = 8
  val f_state = RegInit(ST_F_IDLE.U(4.W))

  // Datapath regs
  val res_table_wr_reg = RegInit(0.U(TABLE_ENTRY_WIDTH.W))
  val res_table_rd_reg = RegInit(0.U(TABLE_ENTRY_WIDTH.W))
  val res_table_last_rd_reg = RegInit(0.U(TABLE_ENTRY_WIDTH.W))
  val res_addr_cu_id = RegInit(0.U(CU_ID_WIDTH.W))
  val res_addr_wg_slot = RegInit(0.U(WG_SLOT_ID_WIDTH_TABLE.W))
  val res_table_rd_en = RegInit(false.B)
  val res_table_wr_en = RegInit(false.B)
  val res_table_rd_valid = RegInit(false.B)
  val res_table_max_size = RegInit(0.U((RES_ID_WIDTH + 1).W))
  val res_table_max_start = RegInit(0.U(RES_ID_WIDTH.W))

  // Control signals
  val alloc_start = RegInit(false.B)
  val dealloc_start = RegInit(false.B)
  val find_max_start = RegInit(false.B)
  val alloc_done = RegInit(false.B)
  val dealloc_done = RegInit(false.B)
  val find_max_done = RegInit(false.B)
  val new_entry_is_last = RegInit(false.B)
  val new_entry_is_first = RegInit(false.B)
  val rem_entry_is_last = RegInit(false.B)
  val rem_entry_is_first = RegInit(false.B)
  val cu_initialized = RegInit(VecInit(Seq.fill(NUMBER_CU)(false.B)))
  val cu_initialized_i = RegInit(false.B)
  rtwr_res_strt := get_res_start(res_table_wr_reg)
  rtrr_res_strt := get_res_start(res_table_rd_reg)
  rtlrr_res_strt := get_res_start(res_table_last_rd_reg)

  rtwr_res_size := get_res_size(res_table_wr_reg)
  rtrr_res_size := get_res_size(res_table_rd_reg)
  rtlrr_res_size := get_res_size(res_table_last_rd_reg)

  rtwr_prev_item := get_prev_item_wg_slot(res_table_wr_reg)
  rtrr_prev_item := get_prev_item_wg_slot(res_table_rd_reg)
  rtlrr_prev_item := get_prev_item_wg_slot(res_table_last_rd_reg)

  rtwr_next_item := get_next_item_wg_slot(res_table_wr_reg)
  rtrr_next_item := get_next_item_wg_slot(res_table_rd_reg)
  rtlrr_next_item := get_next_item_wg_slot(res_table_last_rd_reg)

  alloc_res_en_i := io.alloc_res_en
  when(io.alloc_res_en){
    alloc_cu_id_i := io.alloc_cu_id
    alloc_wg_slot_id_i := Cat(0.U,io.alloc_wg_slot_id)
    alloc_res_size_i := io.alloc_res_size
    alloc_res_start_i := io.alloc_res_start
    res_addr_cu_id := io.alloc_cu_id
  }

  dealloc_res_en_i := io.dealloc_res_en
  when(io.dealloc_res_en){
    dealloc_cu_id_i := io.dealloc_cu_id
    dealloc_wg_slot_id_i := Cat(0.U,io.dealloc_wg_slot_id)
    res_addr_cu_id := io.dealloc_cu_id
  }

  // Main state machine of the resource table
  alloc_start := false.B
  dealloc_start := false.B
  find_max_start := false.B
  res_table_done := false.B
  switch(m_state){
    is(ST_M_IDLE.U){
      when(alloc_res_en_i){
        alloc_start := true.B
        m_state := ST_M_ALLOC.U
      }
        .elsewhen(dealloc_res_en_i){
          dealloc_start := true.B
          m_state := ST_M_DEALLOC.U
        }
    }
    is(ST_M_ALLOC.U){
      when(alloc_done){
        find_max_start := true.B
        m_state := ST_M_FIND_MAX.U
      }
    }
    is(ST_M_DEALLOC.U){
      when(dealloc_done){
        find_max_start := true.B
        m_state := ST_M_FIND_MAX.U
      }
    }
    is(ST_M_FIND_MAX.U){
      when(find_max_done){
        res_table_done := true.B
        m_state := ST_M_IDLE.U
      }
    }
  }

  // All state machines share the same resource (the table) so,
  // there can be onle one machine out of IDLE state at a given time.
  // Alloc state machine
  res_table_rd_en := false.B
  res_table_wr_en := false.B
  alloc_done := false.B

  switch(a_state){
    is(ST_A_IDLE.U){
      when(alloc_start){
        // Start looking for the new entry positon on
        // head_position

        // Table is clear or cu was not initialized
        when(table_head_pointer_i === RES_TABLE_END_TABLE_U || !cu_initialized_i){
          new_entry_is_first := true.B
          new_entry_is_last := true.B
          a_state := ST_A_WRITE_NEW_ENTRY.U
        }
          .otherwise{
            new_entry_is_first := false.B
            new_entry_is_last := false.B
            res_table_rd_en := true.B
            res_addr_wg_slot := table_head_pointer_i
            a_state := ST_A_FIND_POSITION.U
          }
      }
    }
    is(ST_A_FIND_POSITION.U){
      //Look for the entry position
      when(res_table_rd_valid){
        // Found the entry that will be after the new one
        when(get_res_start(res_table_rd_reg) > alloc_res_start_i){
          // if new entry will be the first entry
          when(get_prev_item_wg_slot(res_table_rd_reg) === RES_TABLE_HEAD_POINTER_U){
            new_entry_is_first := true.B
            res_table_wr_en := true.B
            res_table_wr_reg := set_prev_item_wg_slot(alloc_wg_slot_id_i, res_table_rd_reg)
            a_state := ST_A_WRITE_NEW_ENTRY.U
          }
            .otherwise{
              res_table_wr_en := true.B
              res_table_wr_reg := set_prev_item_wg_slot(alloc_wg_slot_id_i, res_table_rd_reg)
              a_state := ST_A_UPDATE_PREV_ENTRY.U
            }
        }
          .elsewhen(get_next_item_wg_slot(res_table_rd_reg) === RES_TABLE_END_TABLE_U){
            // if new entry will be the last entry
            res_table_wr_en := true.B
            res_table_wr_reg := set_next_item_wg_slot(alloc_wg_slot_id_i, res_table_rd_reg)
            new_entry_is_last := true.B
            a_state := ST_A_WRITE_NEW_ENTRY.U
          }
          .otherwise{
            // Keep looking for the entry postion
            res_table_rd_en := true.B
            res_addr_wg_slot := get_next_item_wg_slot(res_table_rd_reg)
          }
      }
    }
    is(ST_A_UPDATE_PREV_ENTRY.U){
      // Update the previous entry
      res_table_wr_en := true.B
      res_table_wr_reg := set_next_item_wg_slot(alloc_wg_slot_id_i, res_table_last_rd_reg)
      res_addr_wg_slot := get_prev_item_wg_slot(res_table_rd_reg)
      a_state := ST_A_WRITE_NEW_ENTRY.U
    }
    is(ST_A_WRITE_NEW_ENTRY.U){
      when(new_entry_is_first){
        table_head_pointer_i := alloc_wg_slot_id_i
      }

      // Write the new entry
      res_table_wr_en := true.B
      res_addr_wg_slot := alloc_wg_slot_id_i

      when(new_entry_is_first && new_entry_is_last){
        res_table_wr_reg := get_new_entry(alloc_res_start_i, alloc_res_size_i, RES_TABLE_HEAD_POINTER_U, RES_TABLE_END_TABLE_U)
      }
        .elsewhen(new_entry_is_last){
          res_table_wr_reg := get_new_entry(alloc_res_start_i, alloc_res_size_i, res_addr_wg_slot, RES_TABLE_END_TABLE_U)
        }
        .elsewhen(new_entry_is_first){
          res_table_wr_reg := get_new_entry(alloc_res_start_i, alloc_res_size_i, RES_TABLE_HEAD_POINTER_U, res_addr_wg_slot)
        }
        .otherwise{
          res_table_wr_reg := get_new_entry(alloc_res_start_i, alloc_res_size_i, res_addr_wg_slot, get_next_item_wg_slot(res_table_last_rd_reg))
        }
      alloc_done := true.B
      a_state := ST_A_IDLE.U
    }
  }
  // Dealloc state machine
  dealloc_done := false.B
  switch(d_state){
    is(ST_D_IDLE.U){
      when(dealloc_start){
        rem_entry_is_first := false.B
        rem_entry_is_last := false.B
        res_table_rd_en := true.B
        res_addr_wg_slot := dealloc_wg_slot_id_i
        d_state := ST_D_READ_PREV_ENTRY.U
      }
    }
    is(ST_D_READ_PREV_ENTRY.U){
      // Read the previous entry
      when(res_table_rd_valid){
        // We are removing the last remaining entry on the table
        when(get_prev_item_wg_slot(res_table_rd_reg) === RES_TABLE_HEAD_POINTER_U && get_next_item_wg_slot(res_table_rd_reg) === RES_TABLE_END_TABLE_U){
          table_head_pointer_i := RES_TABLE_END_TABLE_U
          dealloc_done := true.B
          d_state := ST_D_IDLE.U
        }
          // We are removing the first entry on the table
          .elsewhen(get_prev_item_wg_slot(res_table_rd_reg) === RES_TABLE_HEAD_POINTER_U){
            rem_entry_is_first := true.B
            d_state := ST_D_READ_NEXT_ENTRY.U
          }
          // We are removing the last entry on the table
          .elsewhen(get_next_item_wg_slot(res_table_rd_reg) === RES_TABLE_END_TABLE_U){
            rem_entry_is_last := true.B
            res_table_rd_en := true.B
            res_addr_wg_slot := get_prev_item_wg_slot(res_table_rd_reg)
            d_state := ST_D_UPDATE_PREV_ENTRY.U
          }
          // We are removing an entry in the middle of the table
          .otherwise{
            res_table_rd_en := true.B
            res_addr_wg_slot := get_prev_item_wg_slot(res_table_rd_reg)
            d_state := ST_D_READ_NEXT_ENTRY.U
          }
      }
    }
    is(ST_D_READ_NEXT_ENTRY.U){
      res_table_rd_en := true.B
      res_addr_wg_slot := get_next_item_wg_slot(res_table_rd_reg)
      d_state := ST_D_UPDATE_PREV_ENTRY.U
    }
    is(ST_D_UPDATE_PREV_ENTRY.U){
      // In this cycle it is reading the next entry, so we can use the
      // the addr_reg to get our the next entry addr
      // Single cycle delay to complete reading if the entry if the entry
      // is the first or the last
      when(rem_entry_is_first){
        d_state := ST_D_UPDATE_NEXT_ENTRY.U
      }
        .elsewhen(rem_entry_is_last){
          d_state := ST_D_UPDATE_NEXT_ENTRY.U
        }
        .otherwise{
          res_addr_wg_slot := get_prev_item_wg_slot(res_table_last_rd_reg)
          res_table_wr_en := true.B
          res_table_wr_reg := set_next_item_wg_slot(res_addr_wg_slot, res_table_rd_reg)
          d_state := ST_D_UPDATE_NEXT_ENTRY.U
        }
    }
    is(ST_D_UPDATE_NEXT_ENTRY.U){
      // In this cycle it is writing the previous entry, so we can use the
      // the addr_reg to get our the next entry addr
      res_table_wr_en := true.B
      when(rem_entry_is_first){
        table_head_pointer_i := res_addr_wg_slot
        res_table_wr_reg := set_prev_item_wg_slot(RES_TABLE_HEAD_POINTER_U, res_table_rd_reg)
      }
        .elsewhen(rem_entry_is_last){
          res_table_wr_en := true.B
          // No need to update addr, we are writing the
          // entry we just read
          res_table_wr_reg := set_next_item_wg_slot(RES_TABLE_END_TABLE_U, res_table_rd_reg)
        }
        .otherwise{
          res_addr_wg_slot := get_next_item_wg_slot(res_table_wr_reg)
          res_table_wr_reg := set_prev_item_wg_slot(res_addr_wg_slot, res_table_rd_reg)
        }
      dealloc_done := true.B
      d_state := ST_D_IDLE.U
    }
  }

  // Find max state machine
  find_max_done := false.B
  switch(f_state){
    is(ST_F_IDLE.U){
      when(find_max_start){
        // Zero the max res size reg
        res_table_max_size := 0.U
        // In case table is clear, return 0 and finish
        when(table_head_pointer_i === RES_TABLE_END_TABLE_U){
          find_max_done := true.B
          res_table_max_size := NUMBER_RES_SLOTS.U
          res_table_max_start := 0.U
        }
          .otherwise{
            // otherwise start searching
            res_table_rd_en := true.B
            res_addr_wg_slot := table_head_pointer_i
            f_state := ST_F_FIRST_ITEM.U
          }

      }
    }
    is(ST_F_FIRST_ITEM.U){
      // Read the first item
      // only read first item. If it is alst the last, skip
      // the searching state
      when(res_table_rd_valid){
        res_table_max_size := get_res_start(res_table_rd_reg)
        res_table_max_start := 0.U
        // check if end of the table
        when(get_next_item_wg_slot(res_table_rd_reg) =/= RES_TABLE_END_TABLE_U){
          res_table_rd_en := true.B
          res_addr_wg_slot := get_next_item_wg_slot(res_table_rd_reg)
          f_state := ST_F_SEARCHING.U
        }
          .otherwise{
            f_state := ST_F_LAST_ITEM.U
          }
      }
    }
    is(ST_F_SEARCHING.U){
      when(res_table_rd_valid){
        // check if the next item is the last
        when(get_next_item_wg_slot(res_table_rd_reg) =/= RES_TABLE_END_TABLE_U){
          res_table_rd_en := true.B
          res_addr_wg_slot := get_next_item_wg_slot(res_table_rd_reg)
        }
          .otherwise{
            f_state := ST_F_LAST_ITEM.U
          }
        // check if this is the max res size
        when(get_free_res_size(res_table_last_rd_reg, res_table_rd_reg) > res_table_max_size){
          res_table_max_size := get_free_res_size(res_table_last_rd_reg, res_table_rd_reg)
          res_table_max_start := get_free_res_start(res_table_last_rd_reg)
        }
      }
    }
    is(ST_F_LAST_ITEM.U){
      // calculate the free space for the last item
      when(get_free_res_size_last(res_table_rd_reg) > res_table_max_size){
        res_table_max_size := get_free_res_size_last(res_table_rd_reg)
        res_table_max_start := get_free_res_start(res_table_rd_reg)
      }
      find_max_done := true.B
      f_state := ST_F_IDLE.U
    }
  }
  // Data path of the resource table
  when(alloc_res_en_i || dealloc_res_en_i){
    // Read the head pointer at the start
    cu_initialized_i := cu_initialized(res_addr_cu_id)
    table_head_pointer_i := table_head_pointer(res_addr_cu_id)
  }
    .elsewhen(alloc_done || dealloc_done){
      // Write the head pointer at the end
      table_head_pointer(res_addr_cu_id) := table_head_pointer_i
      cu_initialized(res_addr_cu_id) := true.B
    }
  res_table_rd_valid := res_table_rd_en
  when(res_table_rd_en){
    res_table_rd_reg := resource_table_ram(calc_table_addr(res_addr_cu_id, res_addr_wg_slot))
    res_table_last_rd_reg := res_table_rd_reg
  }
    .elsewhen(res_table_wr_en){
      resource_table_ram(calc_table_addr(res_addr_cu_id, res_addr_wg_slot)) := res_table_wr_reg
    }
  io.cam_biggest_space_size := res_table_max_size
  io.cam_biggest_space_addr := res_table_max_start

  /*
  //debug prints
  printf(p"c: alloc state: $a_state, dealloc state: $d_state, find max state: $f_state\n")
  printf(p"c: cu_initialized: $cu_initialized_i\n")
  //print alloc_start
  printf(p"c: alloc_start: $alloc_start\n")
  //print alloc_res_en
  printf(p"c: alloc_res_en: $alloc_res_en_i\n")
  //print res_table_rd_reg(RES_STRT_H, RES_STRT_L)
  val print_res_table_rd_reg_strt = Wire(UInt((RES_STRT_H - RES_STRT_L + 1).W))
  print_res_table_rd_reg_strt := res_table_rd_reg(RES_STRT_H, RES_STRT_L)
  printf(p"c: res_table_rd_reg(RES_STRT_H, RES_STRT_L): $print_res_table_rd_reg_strt\n")
  //print res_table_rd_reg(RES_SIZE_H, RES_SIZE_L)
  val print_res_table_rd_reg_size = Wire(UInt((RES_SIZE_H - RES_SIZE_L + 1).W))
  print_res_table_rd_reg_size := res_table_rd_reg(RES_SIZE_H, RES_SIZE_L)
  printf(p"c: res_table_rd_reg(RES_SIZE_H, RES_SIZE_L): $print_res_table_rd_reg_size\n")
  //print res_table_rd_reg(NEXT_ENTRY_H, NEXT_ENTRY_L)
  val print_res_table_rd_reg_next = Wire(UInt((NEXT_ENTRY_H - NEXT_ENTRY_L + 1).W))
  print_res_table_rd_reg_next := res_table_rd_reg(NEXT_ENTRY_H, NEXT_ENTRY_L)
  printf(p"c: res_table_rd_reg(NEXT_ENTRY_H, NEXT_ENTRY_L): $print_res_table_rd_reg_next\n")
  when(print_res_table_rd_reg_next === RES_TABLE_END_TABLE_U){
      printf(p"THE END OF TABLE\n")
  }
  //print res_table_wr_reg(RES_STRT_H, RES_STRT_L)
  val print_res_table_wr_reg_strt = Wire(UInt((RES_STRT_H - RES_STRT_L + 1).W))
  print_res_table_wr_reg_strt := res_table_wr_reg(RES_STRT_H, RES_STRT_L)
  printf(p"c: res_table_wr_reg(RES_STRT_H, RES_STRT_L): $print_res_table_wr_reg_strt\n")
  //print res_table_wr_reg(RES_SIZE_H, RES_SIZE_L)
  val print_res_table_wr_reg_size = Wire(UInt((RES_SIZE_H - RES_SIZE_L + 1).W))
  print_res_table_wr_reg_size := res_table_wr_reg(RES_SIZE_H, RES_SIZE_L)
  printf(p"c: res_table_wr_reg(RES_SIZE_H, RES_SIZE_L): $print_res_table_wr_reg_size\n")
  //print alloc_res_start_i
  printf(p"c: alloc_res_start_i: $alloc_res_start_i\n")
  //print wg slot id
  printf(p"c: alloc wg slot id: $alloc_wg_slot_id_i\n")
  printf(p"c: dealloc wg slot id: $dealloc_wg_slot_id_i\n")
  //print cu id
  printf(p"c: alloc cu id: $alloc_cu_id_i\n")
  printf(p"c: dealloc cu id: $dealloc_cu_id_i\n")
  //print res tbl done
  printf(p"c: res tbl done: $res_table_done\n")
  //print max size and start
  printf(p"c: max size: $res_table_max_size\n")
  printf(p"c: max start: $res_table_max_start\n")
  */
  /*
  when(d_state =/= 1.U){
      printf(p"c: dealloc state: $d_state\n")
  }
  when(f_state =/= 1.U){
      printf(p"c: find max state: $f_state\n")
  }
  */
  /*
  //table_entry(RES_STRT_H, RES_STRT_L) - (last_table_entry(RES_STRT_H, RES_STRT_L) + last_table_entry(RES_SIZE_H, RES_SIZE_L))
  printf(p"a_state: $a_state, d_state: $d_state, f_state: $f_state\n")
  printf(p"alloc_done: $alloc_done, dealloc_done: $dealloc_done, find_max_done: $find_max_done, res_table_done: $res_table_done\n")
  when(f_state =/= ST_F_IDLE.U){
      printf(p"START FIND\n")
      printf(p"c: res_table_max_size: $res_table_max_size\n")
      printf(p"c: res_table_max_start: $res_table_max_start\n")
      printf(p"c: Current find: ${get_free_res_size(res_table_last_rd_reg, res_table_rd_reg)}\n")
      printf(p"c: Last strt: ${res_table_last_rd_reg(RES_STRT_H, RES_STRT_L)}\n")
      printf(p"c: Last size: ${res_table_last_rd_reg(RES_SIZE_H, RES_SIZE_L)}\n")
      printf(p"c: Current strt: ${res_table_rd_reg(RES_STRT_H, RES_STRT_L)}\n")
      printf(p"c: res_addr_wg_slot: $res_addr_wg_slot\n")
  }
  */
}


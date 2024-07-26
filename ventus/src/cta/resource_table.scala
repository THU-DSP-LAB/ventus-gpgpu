package cta

/**
 * @see docs/cta_scheduler/Resource_table.md
 */

import chisel3._
import chisel3.util._
import cta.utils.sort3
import cta.utils.DecoupledIO_1_to_3

// =
// Abbreviations:
// rt = resource table
// ⬑ = continuing from above, 接续上文
// =

class io_cuinterface2rt extends Bundle with ctainfo_alloc_to_cuinterface {
  val wg_id: Option[UInt] = if(CONFIG.DEBUG) Some(UInt(CONFIG.WG.WG_ID_WIDTH)) else None
}

class io_ram[T <: Data](LEN: Int, gen: T) extends Bundle {
  val rd = new Bundle {
    val en = Output(Bool())
    val addr = Output(UInt(log2Ceil(LEN).W))        // if LEN==1, this won't be used
    val data = Input(gen.cloneType)
  }
  val wr = new Bundle {
    val en = Output(Bool())
    val addr = Output(UInt(log2Ceil(LEN).W))        // if LEN==1, this won't be used
    val data = Output(gen.cloneType)
  }
  def apply(addr: UInt, rd_en: Bool = true.B): T= {
    rd.en := rd_en
    rd.addr := addr
    rd.data
  }
}

class io_reg[T <: Data](LEN: Int, gen: T) extends Bundle {
  val rd = new Bundle {
    val addr = Output(UInt(log2Ceil(LEN).W))        // if LEN==1, this won't be used
    val data = Input(gen.cloneType)
  }
  val wr = new Bundle {
    val en = Output(Bool())
    val addr = Output(UInt(log2Ceil(LEN).W))        // if LEN==1, this won't be used
    val data = Output(gen.cloneType)
  }
  def apply(addr: UInt): T = {
    rd.addr := addr
    rd.data
  }
  def apply(): T = {
    rd.data
  }
}


class io_rtram(NUM_RESOURCE: Int, NUM_WG_SLOT: Int = CONFIG.GPU.NUM_WG_SLOT) extends Bundle {
  // SyncReadMem
  val prev  = new io_ram(NUM_WG_SLOT, UInt(log2Ceil(NUM_WG_SLOT).W))  // linked-list pointer to the previous WG, in order of resource address
  val next  = new io_ram(NUM_WG_SLOT, UInt(log2Ceil(NUM_WG_SLOT).W))  // linked-list pointer to the next WG, in order of resource address
  val addr1 = new io_ram(NUM_WG_SLOT, UInt(log2Ceil(NUM_RESOURCE).W)) // base address of resource used by this WG
  val addr2 = new io_ram(NUM_WG_SLOT, UInt(log2Ceil(NUM_RESOURCE).W)) // last address of resource used by this WG
  // Reg or Mem
  val cnt  = new io_reg(1, UInt(log2Ceil(NUM_WG_SLOT+1).W))// number of WG in the linked-list
  val head = new io_reg(1, UInt(log2Ceil(NUM_WG_SLOT).W))  // the first WG in the linked-list
  val tail = new io_reg(1, UInt(log2Ceil(NUM_WG_SLOT).W))  // the last  WG in the linked-list
  // For debug
  val wgid = if(CONFIG.DEBUG) Some(new io_reg(NUM_WG_SLOT, UInt(CONFIG.WG.WG_ID_WIDTH))) else None
  val valid = if(CONFIG.DEBUG) Some(new io_reg(NUM_WG_SLOT, Bool())) else None
}

class resource_table_handler(NUM_CU_LOCAL: Int, NUM_RESOURCE: Int, NUM_RT_RESULT: Int = CONFIG.RESOURCE_TABLE.NUM_RESULT) extends Module {
  // Constants
  val NUM_WG_SLOT = CONFIG.GPU.NUM_WG_SLOT
  val NUM_CU = CONFIG.GPU.NUM_CU
  // IO
  val io = IO(new Bundle {
    val dealloc = Flipped(DecoupledIO(new Bundle {          // dealloc request from CU interface
      val cu_id_local = UInt(log2Ceil(NUM_CU_LOCAL).W)      // CU ID of WG in this handler-rtram group
      val cu_id = UInt(log2Ceil(NUM_CU).W)                  // Global CU ID of this WG
      val wg_slot_id = UInt(log2Ceil(NUM_WG_SLOT).W)
      val wg_id: Option[UInt] = if(CONFIG.DEBUG) Some(UInt(CONFIG.WG.WG_ID_WIDTH)) else None
    }))
    val alloc = Flipped(DecoupledIO(new Bundle {            // alloc request from Allocator
      val cu_id_local = UInt(log2Ceil(NUM_CU_LOCAL).W)      // CU ID of WG in this handler-rtram group
      val cu_id = UInt(log2Ceil(NUM_CU).W)                  // Global CU ID of this WG
      val wg_slot_id = UInt(log2Ceil(NUM_WG_SLOT).W)
      val num_resource = UInt(log2Ceil(NUM_RESOURCE+1).W)
      val wg_id: Option[UInt] = if(CONFIG.DEBUG) Some(UInt(CONFIG.WG.WG_ID_WIDTH)) else None
    }))
    val baseaddr = DecoupledIO(new Bundle {                 // result of alloc req, resource base address of the allocating WG
      val cu_id = UInt(log2Ceil(NUM_CU).W)
      val addr = UInt(log2Ceil(NUM_RESOURCE).W)             // the found base address
      val wg_id: Option[UInt] = if(CONFIG.DEBUG) Some(UInt(CONFIG.WG.WG_ID_WIDTH)) else None
    })
    val rtcache_update = DecoupledIO(new io_rt2cache(NUM_RESOURCE))   // update rtcache in Allocator, result of rtram scan
    val rtram_sel = DecoupledIO(new Bundle {                // control the Bus-MUX of this handler-rtram group
      val sel = UInt(log2Ceil(NUM_CU_LOCAL).W)              // cu_id_local
    })
    val rtram_data = new io_rtram(NUM_RESOURCE = NUM_RESOURCE, NUM_WG_SLOT = NUM_WG_SLOT)   // IO to the selected rtram
  })

  // =
  // Main FSM - define
  // =
  object FSM extends ChiselEnum {
    val IDLE, CONNECT_ALLOC, ALLOC, CONNECT_DEALLOC, DEALLOC, SCAN, OUTPUT = Value
  }
  val fsm_next = Wire(FSM())
  val fsm = RegNext(fsm_next, FSM.IDLE)

  // sub-fsm finish signals
  val alloc_ok = Wire(Bool())
  val dealloc_ok = Wire(Bool())
  val scan_ok = Wire(Bool())
  val output_ok = RegInit(false.B)

  // =
  // IO output driver: alloc, dealloc, rtram
  // =

  // rtram select
  val cu_sel = Reg(UInt(log2Ceil(NUM_CU_LOCAL).W))
  val cu_sel_en = RegInit(false.B)
  io.rtram_sel.valid := cu_sel_en
  io.rtram_sel.bits.sel := cu_sel

  // alloc/dealloc ready control
  // CU resource table handler can only change its target CU in IDLE state
  // ready to receive a new alloc request <=> IDLE || ( (req CU == current CU) && (able to preempt) )
  // ready to receive a new dealloc request <=> (IDLE && no alloc) || (no_same_cu_alloc && (req CU == current CU) && (able to preempt) )
  io.alloc.ready := (fsm === FSM.IDLE) || (cu_sel === io.alloc.bits.cu_id_local && (
    (fsm === FSM.ALLOC && alloc_ok) || (fsm === FSM.DEALLOC && dealloc_ok) ||
    (fsm === FSM.SCAN) || (fsm === FSM.OUTPUT)
  ))

  val alloc_same_cu_valid = io.alloc.valid && (cu_sel === io.alloc.bits.cu_id_local) // There is a alloc req which is able to preempt
  io.dealloc.ready := (!io.alloc.valid && fsm === FSM.IDLE) || (!alloc_same_cu_valid && cu_sel === io.dealloc.bits.cu_id_local && (
    (fsm === FSM.ALLOC && alloc_ok) || (fsm === FSM.DEALLOC && dealloc_ok) ||
    (fsm === FSM.SCAN)
  ))

  // rtram data
  // All these three sub-fsm need to read/write io.rtram during their active time, respectively
  // The main FSM promises that at most one of these three sub-fsm is active at any moment
  // To avoid potential multi-driver problem, each sub-fsm drives their own cluster of rtram signals,
  //  and use MUX to triple-select one cluster of signal to drive io.rtram
  //                           fsm                        │
  //                        ┌───┴───┐                     │
  //    fsm_alloc    ─────→ ┤       │                     │    fsm_alloc    ←──────┐
  //                        │       │         (output)    │                        │         (input)
  //    fsm_dealloc  ─────→ ┤  MUX  ├ ──────→ io.rtram    │    fsm_dealloc  ←──────┼─────── io.rtram
  //                        │       │                     │                        │
  //    fsm_scan     ─────→ ┤       │                     │    fsm_scan     ←──────┘
  //                        └───────┘                     │
  // TODO: in chisel3.6, we can use something like this
  //  io.rtram_data :<= MuxLookup(fsm, DontCare, Seq(
  //    FSM.ALLOC -> rtram_alloc,
  //    FSM.DEALLOC -> rtram_dealloc,
  //    FSM.SCAN -> rtram_scan,
  //  ))
  //  io.rtram_data :>= rtram_alloc
  //  io.rtram_data :>= rtram_dealloc
  //  io.rtram_data :>= rtram_scan
  val rtram_alloc   = WireInit(new io_rtram(NUM_RESOURCE = NUM_RESOURCE, NUM_WG_SLOT = NUM_WG_SLOT), DontCare)
  val rtram_dealloc = WireInit(new io_rtram(NUM_RESOURCE = NUM_RESOURCE, NUM_WG_SLOT = NUM_WG_SLOT), DontCare)
  val rtram_scan    = WireInit(new io_rtram(NUM_RESOURCE = NUM_RESOURCE, NUM_WG_SLOT = NUM_WG_SLOT), DontCare)
  when(fsm === FSM.SCAN) {
    io.rtram_data <> rtram_scan
  } .elsewhen(fsm === FSM.DEALLOC) {
    io.rtram_data <> rtram_dealloc
  } .otherwise {
    io.rtram_data <> rtram_alloc
  }

  // =
  // Main FSM - actions
  // =

  // Action 1: get WG info when a new alloc/dealloc request is received
  val wgsize = Reg(UInt(log2Ceil(NUM_RESOURCE+1).W))
  val wgslot = Reg(UInt(log2Ceil(NUM_WG_SLOT).W))
  val wg_cu = Reg(UInt(log2Ceil(CONFIG.GPU.NUM_CU).W))
  assert(!(io.alloc.fire && io.dealloc.fire)) // only one request can be received in a same cycle

  wgsize := Mux(io.alloc.fire, io.alloc.bits.num_resource, wgsize)
  wgslot := Mux1H(Seq(
    io.alloc.fire -> io.alloc.bits.wg_slot_id,
    io.dealloc.fire -> io.dealloc.bits.wg_slot_id,
    (!io.alloc.fire && !io.dealloc.fire) -> wgslot,
  ))
  wg_cu := Mux1H(Seq(
    io.alloc.fire -> io.alloc.bits.cu_id,
    io.dealloc.fire -> io.dealloc.bits.cu_id,
    (!io.alloc.fire && !io.dealloc.fire) -> wg_cu,
  ))

  val wg_id = if(CONFIG.DEBUG) Some(Reg(UInt(CONFIG.WG.WG_ID_WIDTH))) else None
  if(CONFIG.DEBUG){
    wg_id.get := Mux1H(Seq(
      io.alloc.fire -> io.alloc.bits.wg_id.get,
      io.dealloc.fire -> io.dealloc.bits.wg_id.get,
      (!io.alloc.fire && !io.dealloc.fire) -> wg_id.get,
    ))
  }

  // Action 2: get new target CU when a different CU's request is received in IDLE state
  // But there is no need to check whether fsm===IDLE,
  //  since if (fsm=/=IDLE) && (req.fire), there must be (cu_sel===new_cu_id), new assignment has no effect (see the next assert)
  cu_sel := Mux1H(Seq(
    io.alloc.fire -> io.alloc.bits.cu_id_local,
    io.dealloc.fire -> io.dealloc.bits.cu_id_local,
    (!io.alloc.fire && !io.dealloc.fire) -> cu_sel,
  ))
  // In non-IDLE state, only request of the same CU will be received
  when(fsm =/= FSM.IDLE) {
    when(io.alloc.fire){
      assert(cu_sel === io.alloc.bits.cu_id_local)
    }
    when(io.dealloc.fire){
      assert(cu_sel === io.dealloc.bits.cu_id_local)
    }
  }
  // In this implement, no need to disconnect
  cu_sel_en := cu_sel_en || io.alloc.fire || io.dealloc.fire

  // =
  // Sub FSM ALLOC (Main FSM action 3)
  // =

  object FSM_A extends ChiselEnum {
    val IDLE, FIND, WRITE_OUTPUT = Value
  }
  val fsm_a_next = Wire(FSM_A())
  val fsm_a = RegNext(fsm_a_next, FSM_A.IDLE)

  // Sub-FSM ALLOC action 1:
  // Iterate over the linked-list, and find a proper base address for the new alloc request
  // Number of WG in the linked-list: rtram.cnt
  // Number of idle resource segment which need to be checked: rtram.cnt + 1, so 0 ≤ fsm_a_cnt ≤ rtram.cnt
  val fsm_a_cnt = RegInit(0.U(log2Ceil(NUM_WG_SLOT+2).W))     // number of idle resource segment which has already been checked
  val fsm_a_found_ok = WireInit(Bool(), DontCare)             // final result will be given out next cycle, so it's ready to jump to the next state
  val fsm_a_found_size = Reg(UInt(log2Ceil(NUM_RESOURCE+1).W))// size of the currently selected resource segment
  val fsm_a_found_addr = Reg(UInt(log2Ceil(NUM_RESOURCE).W))  // base address of the currently selected resource segment
  val fsm_a_found_ptr1 = Reg(UInt(log2Ceil(NUM_WG_SLOT).W))   // pointer to the WG which locates before the currently selected resource segment
  val fsm_a_found_ptr2 = Reg(UInt(log2Ceil(NUM_WG_SLOT).W))   // pointer to the WG which locates after  the currently selected resource segment
  val fsm_a_ptr1 = Reg(UInt(log2Ceil(NUM_WG_SLOT).W))         // pointer to the WG which locates before the currently checked resource segment
  val fsm_a_ptr2 = Wire(UInt(log2Ceil(NUM_WG_SLOT).W))        // pointer to the WG which locates after  the currently checked resource segment
  val fsm_a_head_flag, fsm_a_tail_flag = Reg(Bool())          // if the currently selected segment will be the new head/tail node of the linked-list
  val fsm_a_valid_p1 = RegInit(false.B)                       // valid  signal of pipeline stage 1
  val fsm_a_init_p1 = RegInit(false.B)                        // init   signal of pipeline stage 1
  val fsm_a_finish_p1 = RegInit(false.B)                      // finish signal of pipeline stage 1
  // if you want to send control/data signal to io.rtram, use rtram_alloc
  // if you want to get signal value from io.rtram, use rtram_alloc(recommended) or io.rtram, both ok
  fsm_a_ptr2 := DontCare  // switch default
  switch(fsm_a) {
    is(FSM_A.IDLE) {  // prepare for FSM_A.FIND
      // Even before the iteration begins, we have already know that we can successfully find a resource segment which is large enough
      // What we want to find is the smallest segment that can contain the WG (large but not too large)
      // At the beginning, assume we will find the largest segment (the whole resource, NUM_RESOURCE)
      //  then, step by step we will find smaller segments
      // In the first iteration step, we will check the resource segment before the first WG, even if it may not exist
      // In the last iteration step, we will check the resource segment after the last WG, even if it may not exist
      fsm_a_cnt := 0.U                    // Iteration range: 0 to rtram.cnt
      fsm_a_found_size := NUM_RESOURCE.U  // Initial assumption: assume we select the whole resource.
      fsm_a_found_addr := 0.U             // ⬑ This will be changed to the right segment later, unless rtram.cnt==0 and the initial assumption is correct
      fsm_a_found_ptr1 := DontCare        // In our initial assumption, the linked-list is empty, so we don't care this pointer
      fsm_a_found_ptr2 := DontCare
      fsm_a_ptr1 := DontCare              // Before the first resource segment locates nothing
      fsm_a_ptr2 := DontCare              // After the first resource segment locates the first WG
      fsm_a_head_flag := true.B
      fsm_a_tail_flag := true.B           // In our initial assumption, the linked-list is empty, so the new WG will become head as well as tail
      fsm_a_valid_p1 := false.B
      fsm_a_init_p1 := false.B
      fsm_a_finish_p1 := false.B
    }
    is(FSM_A.FIND) {
      // Since rtram.next,addr1,addr2 use SyncReadMem, we need at least 2 cycles to deal with 1 resource segment
      //  cycle 1: update prt1 & ptr2
      //  cycle 2: get WG addr2 & addr1 from rtram
      //  cycle 3: calculate the size of this resource segment, compare size, give out the new result (registered)
      // We can build a pipeline: update ptr -> fetch data (addr1 & addr2) from rtram -> calc result

      // pipeline stage 0: WG pointers
      fsm_a_ptr1 := fsm_a_ptr2
      rtram_alloc.next(fsm_a_ptr2)  // rtram.next.rd.data is a register of pipeline stage 0
      // pipeline stage 2: fetch data from rtram (addr1 & addr2)
      fsm_a_ptr2 := Mux(fsm_a_cnt === 0.U, rtram_alloc.head(), rtram_alloc.next.rd.data)
      rtram_alloc.addr2(fsm_a_ptr1) // rtram.addr2.rd.data is a register of pipeline stage 1
      rtram_alloc.addr1(fsm_a_ptr2) // rtram.addr1.rd.data is a register of pipeline stage 1
      fsm_a_init_p1 := (fsm_a_cnt === 0.U)
      fsm_a_finish_p1 := (fsm_a_cnt === rtram_alloc.cnt())
      fsm_a_valid_p1 := (fsm_a_valid_p1 && !fsm_a_finish_p1) || (fsm_a_cnt === 0.U)
      val fsm_a_ptr1_p1 = RegNext(fsm_a_ptr1)
      val fsm_a_ptr2_p1 = RegNext(fsm_a_ptr2)
      // pipeline stage 3: resource segment size calc & found result update
      val addr1, addr2 = Wire(UInt(log2Ceil(NUM_RESOURCE+1).W)) // addr1 = this resource segment's start addr - 1, addr2 = resource segment's end addr
      addr1 := Mux(fsm_a_init_p1, (~0.U(log2Ceil(NUM_RESOURCE+1).W)).asUInt, rtram_alloc.addr2.rd.data)
      addr2 := Mux(fsm_a_finish_p1, (NUM_RESOURCE-1).U, rtram_alloc.addr1.rd.data.pad(log2Ceil(NUM_RESOURCE+1)) - 1.U)
      val size = WireInit(UInt(log2Ceil(NUM_RESOURCE+1).W), addr2 - addr1) // resource segment size
      val result_update = (size >= wgsize) && (size < fsm_a_found_size)
      fsm_a_found_size := Mux(fsm_a_valid_p1 && result_update, size, fsm_a_found_size)
      fsm_a_found_addr := Mux(fsm_a_valid_p1 && result_update, addr1 + 1.U, fsm_a_found_addr)
      fsm_a_found_ptr1 := Mux(fsm_a_valid_p1 && result_update, fsm_a_ptr1_p1, fsm_a_found_ptr1)
      fsm_a_found_ptr2 := Mux(fsm_a_valid_p1 && result_update, fsm_a_ptr2_p1, fsm_a_found_ptr2)
      fsm_a_tail_flag := Mux(fsm_a_valid_p1 && result_update, fsm_a_finish_p1, fsm_a_tail_flag)
      fsm_a_head_flag := Mux(fsm_a_valid_p1 && result_update, fsm_a_init_p1, fsm_a_head_flag)
      // Iteration step
      fsm_a_cnt := fsm_a_cnt + 1.U
      // pipeline will give out the final result in the next cycle, so it's ready to jump to the next state
      fsm_a_found_ok := fsm_a_init_p1
      if(CONFIG.DEBUG) {
        val fsm_a_found = RegInit(false.B)
        fsm_a_found := MuxCase(fsm_a_found, Seq(
          fsm_a_init_p1 -> result_update,
          fsm_a_valid_p1 -> (fsm_a_found || result_update),
        ))
        // After ALLOC scan finishes, a new baseaddr should already be found, unless there are no WG in the linked-list
        assert(!fsm_a_finish_p1 || (fsm_a_found || result_update) || (rtram_alloc.cnt() === 0.U))
      }
    }
    is(FSM_A.WRITE_OUTPUT) {  // keep useful data for FSM_A.WRITE_OUTPUT, and prepare for FSM_A.FIND
      // Useless for WRITE & OUTPUT, initialize them for next FSM_A.FIND
      fsm_a_cnt := 0.U
      fsm_a_ptr1 := DontCare
      fsm_a_ptr2 := DontCare
      fsm_a_valid_p1 := false.B
      fsm_a_init_p1 := false.B
      fsm_a_finish_p1 := false.B
      fsm_a_found_size := NUM_RESOURCE.U
      // Useful for WRITE & OUTPUT, keep their value unchanged until finishing
      fsm_a_found_addr := Mux(fsm_a_next =/= fsm_a, 0.U, fsm_a_found_addr)
      fsm_a_found_ptr1 := Mux(fsm_a_next =/= fsm_a, DontCare, fsm_a_found_ptr1)
      fsm_a_found_ptr2 := Mux(fsm_a_next =/= fsm_a, DontCare, fsm_a_found_ptr2)
      fsm_a_head_flag := Mux(fsm_a_next =/= fsm_a, true.B, fsm_a_head_flag)
      fsm_a_tail_flag := Mux(fsm_a_next =/= fsm_a, true.B, fsm_a_tail_flag)
    }
  }

  // Sub-FSM ALLOC action 2 (FSM_A.WRITE_OUTPUT):
  // Write the new WG to the location just found in FSM_A.FIND
  val fsm_a_write_cnt = Reg(UInt(2.W))  // you can also reuse fsm_a_cnt
  fsm_a_write_cnt := MuxCase(fsm_a_write_cnt, Seq(
    (fsm_a =/= FSM_A.WRITE_OUTPUT) -> 0.U,
    (fsm_a_write_cnt <= 1.U) -> (fsm_a_write_cnt + 1.U),
  ))
  // if wgsize==0, this WG will not be inserted to the linked-list
  rtram_alloc.head.wr.en := (fsm_a === FSM_A.WRITE_OUTPUT) && (fsm_a_write_cnt === 0.U) && fsm_a_head_flag && !(wgsize === 0.U)
  rtram_alloc.head.wr.data := wgslot
  rtram_alloc.tail.wr.en := (fsm_a === FSM_A.WRITE_OUTPUT) && (fsm_a_write_cnt === 0.U) && fsm_a_tail_flag && !(wgsize === 0.U)
  rtram_alloc.tail.wr.data := wgslot
  rtram_alloc.cnt.wr.en := (fsm_a === FSM_A.WRITE_OUTPUT) && (fsm_a_write_cnt === 0.U) && !(wgsize === 0.U)
  rtram_alloc.cnt.wr.data := rtram_alloc.cnt() + 1.U
  rtram_alloc.prev.wr.en := (fsm_a === FSM_A.WRITE_OUTPUT) && ((fsm_a_write_cnt === 0.U && !fsm_a_tail_flag) || fsm_a_write_cnt === 1.U) && !(wgsize === 0.U)
  rtram_alloc.prev.wr.addr := Mux(fsm_a_write_cnt === 0.U, fsm_a_found_ptr2, wgslot)
  rtram_alloc.prev.wr.data := Mux(fsm_a_write_cnt === 0.U, wgslot, fsm_a_found_ptr1)
  rtram_alloc.next.wr.en := (fsm_a === FSM_A.WRITE_OUTPUT) && ((fsm_a_write_cnt === 0.U && !fsm_a_head_flag) || fsm_a_write_cnt === 1.U) && !(wgsize === 0.U)
  rtram_alloc.next.wr.addr := Mux(fsm_a_write_cnt === 0.U, fsm_a_found_ptr1, wgslot)
  rtram_alloc.next.wr.data := Mux(fsm_a_write_cnt === 0.U, wgslot, fsm_a_found_ptr2)
  rtram_alloc.addr1.wr.en := (fsm_a === FSM_A.WRITE_OUTPUT) && (fsm_a_write_cnt === 0.U) && !(wgsize === 0.U)
  rtram_alloc.addr1.wr.addr := wgslot
  rtram_alloc.addr1.wr.data := fsm_a_found_addr
  rtram_alloc.addr2.wr.en := (fsm_a === FSM_A.WRITE_OUTPUT) && (fsm_a_write_cnt === 0.U) && !(wgsize === 0.U)
  rtram_alloc.addr2.wr.addr := wgslot
  rtram_alloc.addr2.wr.data := fsm_a_found_addr + wgsize - 1.U
  if(CONFIG.DEBUG) {
    rtram_alloc.valid.get.rd.addr := wgslot
    when(fsm_a === FSM_A.WRITE_OUTPUT && fsm_a_write_cnt === 0.U) {
      assert(rtram_alloc.valid.get.rd.data === false.B)
    }
    rtram_alloc.wgid.get.wr.en := (fsm_a === FSM_A.WRITE_OUTPUT) && (fsm_a_write_cnt === 0.U) && !(wgsize === 0.U)
    rtram_alloc.wgid.get.wr.addr := wgslot
    rtram_alloc.wgid.get.wr.data := wg_id.get
    rtram_alloc.valid.get.wr.en := (fsm_a === FSM_A.WRITE_OUTPUT) && (fsm_a_write_cnt === 0.U) && !(wgsize === 0.U)
    rtram_alloc.valid.get.wr.addr := wgslot
    rtram_alloc.valid.get.wr.data := true.B
  }
  val fsm_a_write_ok = (fsm_a_write_cnt >= 1.U)

  // if the new WG becomes the head node of the linked-list, base address must be 0
  assert((fsm_a =/= FSM_A.WRITE_OUTPUT) || fsm_a_head_flag === (fsm_a_found_addr === 0.U))

  // Sub-FSM ALLOC action 3 (FSM_A.WRITE_OUTPUT):
  // Output the found base address to CU Interface (may be there is a FIFO in resource_table_top)
  val fsm_a_output_valid = RegInit(true.B)
  fsm_a_output_valid := Mux(fsm_a === FSM_A.WRITE_OUTPUT, fsm_a_output_valid && !io.baseaddr.fire, true.B)
  io.baseaddr.valid := (fsm_a === FSM_A.WRITE_OUTPUT) && fsm_a_output_valid
  io.baseaddr.bits.cu_id := wg_cu
  io.baseaddr.bits.addr := fsm_a_found_addr
  if(CONFIG.DEBUG) {io.baseaddr.bits.wg_id.get := wg_id.get}
  val fsm_a_output_ok = !fsm_a_output_valid || io.baseaddr.fire

  // Sub-FSM ALLOC state transition logic
  fsm_a_next := MuxLookup(fsm_a.asUInt, FSM_A.IDLE)(Seq(
    FSM_A.IDLE.asUInt -> Mux(fsm_next === FSM.ALLOC && fsm_next =/= fsm, FSM_A.FIND, fsm_a),
    FSM_A.FIND.asUInt -> Mux(wgsize === 0.U || fsm_a_cnt > io.rtram_data.cnt(), FSM_A.WRITE_OUTPUT, fsm_a),   // if wgsize==0, no need to scan the linked-list
    FSM_A.WRITE_OUTPUT.asUInt -> MuxCase(FSM_A.IDLE, Seq(
      !alloc_ok -> fsm_a,
      io.alloc.fire -> FSM_A.FIND,
    ))
  ))

  alloc_ok := (fsm_a === FSM_A.WRITE_OUTPUT) && fsm_a_write_ok && fsm_a_output_ok

  // =
  // FSM.DEALLOC action (Main FSM action 4)
  // =
  val fsm_d = RegInit(0.U(2.W))
  fsm_d := Mux(fsm =/= FSM.DEALLOC || io.dealloc.fire, 0.U, Mux(!fsm_d.andR, fsm_d + 1.U, fsm_d))
  val fsm_d_prev, fsm_d_next = Wire(UInt(log2Ceil(NUM_WG_SLOT).W))
  fsm_d_prev := rtram_dealloc.prev(wgslot)
  fsm_d_next := rtram_dealloc.next(wgslot)
  rtram_dealloc.next.wr.en := (fsm_d === 1.U) && (wgslot =/= rtram_dealloc.head())
  rtram_dealloc.next.wr.addr := fsm_d_prev
  rtram_dealloc.next.wr.data := fsm_d_next
  rtram_dealloc.prev.wr.en := (fsm_d === 1.U) && (wgslot =/= rtram_dealloc.tail())
  rtram_dealloc.prev.wr.addr := fsm_d_next
  rtram_dealloc.prev.wr.data := fsm_d_prev
  rtram_dealloc.cnt.wr.en := (fsm_d === 1.U)
  rtram_dealloc.cnt.wr.data := rtram_dealloc.cnt() - 1.U
  rtram_dealloc.head.wr.en := (fsm_d === 1.U) && (wgslot === rtram_dealloc.head())
  rtram_dealloc.head.wr.data := fsm_d_next
  rtram_dealloc.tail.wr.en := (fsm_d === 1.U) && (wgslot === rtram_dealloc.tail())
  rtram_dealloc.tail.wr.data := fsm_d_prev
  rtram_dealloc.addr2.wr.en := false.B
  rtram_dealloc.addr2.wr.en := false.B

  if(CONFIG.DEBUG) {
    rtram_dealloc.wgid.get.rd.addr := wgslot
    rtram_dealloc.valid.get.rd.addr := wgslot
    when(fsm === FSM.DEALLOC) {
      assert(rtram_dealloc.wgid.get.rd.data === wg_id.get)
      assert(rtram_dealloc.valid.get.rd.data)
    }
    rtram_dealloc.valid.get.wr.en := (fsm_d === 1.U)
    rtram_dealloc.valid.get.wr.addr := wgslot
    rtram_dealloc.valid.get.wr.data := false.B
  }

  dealloc_ok := (fsm === FSM.DEALLOC) && (fsm_d >= 1.U)

  // =
  // Sub-FSM SCAN (Main FSM action 5)
  // =
  val fsm_s_ptr1 = Reg(UInt(log2Ceil(NUM_WG_SLOT).W))   // pointer to the WG which locate before the currently checked resource segments
  val fsm_s_ptr2 = Wire(UInt(log2Ceil(NUM_WG_SLOT).W))  // pointer to the WG which locate after  the currently checked resource segments
  val fsm_s_init_p1 = RegInit(false.B)
  val fsm_s_finish_p1 = RegInit(false.B)
  val fsm_s_valid_p1 = RegInit(false.B)
  val fsm_s_cnt = RegInit(0.U(log2Ceil(NUM_WG_SLOT+2).W))   // you can also reuse fsm_a_cnt
  fsm_s_cnt := Mux(fsm =/= FSM.SCAN || fsm_next =/= FSM.SCAN, 0.U, fsm_s_cnt + 1.U)
  val rtcache_data = RegInit(VecInit.fill(NUM_RT_RESULT)(0.U(log2Ceil(NUM_RESOURCE+1).W)))  // scan result, rtcache_data[0] is the largest resource segment
  when(fsm === FSM.SCAN){
    // pipeline stage 0: pointer step
    fsm_s_ptr1 := fsm_s_ptr2
    rtram_scan.next(fsm_s_ptr2)       // rtram_scan.next.rd.data is a pipeline stage 0 register
    // pipeline stage 1: rtram data fetch - addr1 & addr2
    fsm_s_ptr2 := Mux(fsm_s_cnt === 0.U, rtram_scan.head(), rtram_scan.next.rd.data)
    rtram_scan.addr2(fsm_s_ptr1)      // rtram_scan.addr2.rd.data is a pipeline stage 1 register
    rtram_scan.addr1(fsm_s_ptr2)      // rtram_scan.addr1.rd.data is a pipeline stage 1 register
    fsm_s_init_p1 := (fsm_s_cnt === 0.U)
    fsm_s_finish_p1 := (fsm_s_cnt === rtram_scan.cnt())
    fsm_s_valid_p1 := (fsm_s_valid_p1 && !fsm_s_finish_p1) || (fsm_s_cnt === 0.U)
    // pipeline stage 2: resource segment size calc & sort
    val addr1 = WireInit(UInt(log2Ceil(NUM_RESOURCE+1).W), Mux(fsm_s_init_p1, (~0.U(log2Ceil(NUM_RESOURCE+1).W)).asUInt, rtram_scan.addr2.rd.data.pad(log2Ceil(NUM_RESOURCE+1))))
    val addr2 = WireInit(UInt(log2Ceil(NUM_RESOURCE+1).W), Mux(fsm_s_finish_p1, NUM_RESOURCE.U - 1.U, rtram_scan.addr1.rd.data.pad(log2Ceil(NUM_RESOURCE+1)) - 1.U))
    val thissize = WireInit(UInt(log2Ceil(NUM_RESOURCE+1).W), addr2 - addr1)
    val result = Wire(Vec(NUM_RT_RESULT, UInt(log2Ceil(NUM_RESOURCE+1).W)))
    if(NUM_RT_RESULT == 1) {
      val rtcache_data_tmp = Mux(fsm_s_init_p1, 0.U, rtcache_data(0))
      result := VecInit(Mux(rtcache_data_tmp > thissize, rtcache_data_tmp, thissize))
    } else if (NUM_RT_RESULT == 2) {
      // This implement only support NUM_RT_RESULT=2. Replace `sort3()` to support other values
      val result_tmp = sort3(Mux(fsm_s_init_p1, 0.U, rtcache_data(0)), Mux(fsm_s_init_p1, 0.U, rtcache_data(1)), thissize)
      result(0) := result_tmp(0)
      result(1) := result_tmp(1)
    } else assert(NUM_RT_RESULT == 1 || NUM_RT_RESULT == 2)
    for(i <- 0 until NUM_RT_RESULT) {
      rtcache_data(i) := result(i)
    }
    // pipeline debug
    if(CONFIG.DEBUG) {
      rtram_scan.valid.get.rd.addr := fsm_s_ptr1
      rtram_scan.wgid.get.rd.addr := fsm_s_ptr1
      val checked_wg_valid = RegInit(0.U(NUM_WG_SLOT.W))
      val checked_wg = ShiftRegisters(rtram_scan.wgid.get.rd.data, NUM_WG_SLOT)
      checked_wg_valid := Mux(fsm_s_init_p1, 1.U, (checked_wg_valid << 1).asUInt | (fsm_s_valid_p1 && !fsm_s_finish_p1).asUInt)
      when(fsm_s_finish_p1) {
        for(i <- 0 until NUM_WG_SLOT) {
          when(checked_wg_valid(i)) {
            for(j <- 0 until NUM_WG_SLOT) {
              // A same linked-list node should only be visited once
              assert(!checked_wg_valid(j) || (i == j).asBool || checked_wg(i) =/= checked_wg(j))
            }
          }
        }
      }
      when(fsm_s_valid_p1) {
        assert(fsm_s_finish_p1 || rtram_scan.valid.get.rd.data) // The linked-list node we are visiting should stores valid data
        for(i <- 0 until NUM_RT_RESULT) {
          assert(fsm_s_init_p1 || rtcache_data(i) <= result(i))
        }
      }
    }
  } .otherwise { // prepare for FSM.SCAN
    fsm_s_ptr1 := DontCare
    fsm_s_ptr2 := DontCare
    fsm_s_init_p1 := false.B
    fsm_s_finish_p1 := false.B
    fsm_s_valid_p1 := false.B
    // keep rtcache_data untouched
  }
  scan_ok := (fsm === FSM.SCAN) && fsm_s_finish_p1

  // =
  // Sub-FSM OUTPUT (Main FSM action 6)
  // =
  output_ok := Mux(fsm === FSM.OUTPUT, output_ok || io.rtcache_update.fire, false.B)
  io.rtcache_update.valid := (fsm === FSM.OUTPUT) && !output_ok
  io.rtcache_update.bits.cu_id := wg_cu
  io.rtcache_update.bits.size := rtcache_data

  // =
  // Main FSM - state transition logic
  // =

  fsm_next := FSM.IDLE    // switch default
  switch(fsm) {
    is(FSM.IDLE) {
      fsm_next := MuxCase(fsm, Seq(
        io.alloc.fire -> Mux(cu_sel === io.alloc.bits.cu_id_local && io.rtram_sel.fire, FSM.ALLOC, FSM.CONNECT_ALLOC),
        io.dealloc.fire -> Mux(cu_sel === io.dealloc.bits.cu_id_local && io.rtram_sel.fire, FSM.DEALLOC, FSM.CONNECT_DEALLOC),
      ))
    }
    is(FSM.CONNECT_ALLOC, FSM.CONNECT_DEALLOC) {
      fsm_next := MuxCase(fsm, Seq(
        io.rtram_sel.fire -> Mux(fsm === FSM.CONNECT_ALLOC, FSM.ALLOC, FSM.DEALLOC),
      ))
    }
    is(FSM.ALLOC) {
      fsm_next := MuxCase(FSM.SCAN, Seq(
        !alloc_ok -> fsm,
        io.alloc.fire -> FSM.ALLOC,
        io.dealloc.fire -> FSM.DEALLOC,
      ))
    }
    is(FSM.DEALLOC) {
      fsm_next := MuxCase(FSM.SCAN, Seq(
        !dealloc_ok -> fsm,
        io.alloc.fire -> FSM.ALLOC,
        io.dealloc.fire -> FSM.DEALLOC,
      ))
    }
    is(FSM.SCAN) {
      fsm_next := MuxCase(fsm, Seq(
        io.alloc.fire -> FSM.ALLOC,
        io.dealloc.fire -> FSM.DEALLOC,
        scan_ok -> FSM.OUTPUT
      ))
    }
    is(FSM.OUTPUT) {
      fsm_next := MuxCase(fsm, Seq(
        io.alloc.fire -> FSM.ALLOC,
        output_ok -> FSM.IDLE,
      ))
    }
  }
}

class resource_table_ram(NUM_RESOURCE: Int, NUM_WG_SLOT: Int = CONFIG.GPU.NUM_WG_SLOT) extends Module {
  val io = IO(new Bundle {
    val en = Input(Bool())  // if en==false.B, write operation is prohibited
    val data = Flipped(new io_rtram(NUM_RESOURCE, NUM_WG_SLOT))
  })

  val cnt = RegInit(0.U(log2Ceil(NUM_WG_SLOT+1).W))
  val head = Reg(UInt(log2Ceil(NUM_WG_SLOT).W))
  val tail = Reg(UInt(log2Ceil(NUM_WG_SLOT).W))
  val prev = SyncReadMem(NUM_WG_SLOT, UInt(log2Ceil(NUM_WG_SLOT).W))
  val next = SyncReadMem(NUM_WG_SLOT, UInt(log2Ceil(NUM_WG_SLOT).W))
  val addr1 = SyncReadMem(NUM_WG_SLOT, UInt(log2Ceil(NUM_RESOURCE).W))
  val addr2 = SyncReadMem(NUM_WG_SLOT, UInt(log2Ceil(NUM_RESOURCE).W))

  io.data.cnt.rd.data := cnt
  when(io.en && io.data.cnt.wr.en) {cnt := io.data.cnt.wr.data}
  io.data.head.rd.data := head
  when(io.en && io.data.head.wr.en) {head := io.data.head.wr.data}
  io.data.tail.rd.data := tail
  when(io.en && io.data.tail.wr.en) {tail := io.data.tail.wr.data}

  io.data.prev.rd.data := prev.read(io.data.prev.rd.addr, io.data.prev.rd.en)
  when(io.en && io.data.prev.wr.en) {prev.write(io.data.prev.wr.addr, io.data.prev.wr.data)}
  io.data.next.rd.data := next.read(io.data.next.rd.addr, io.data.next.rd.en)
  when(io.en && io.data.next.wr.en) {next.write(io.data.next.wr.addr, io.data.next.wr.data)}
  io.data.addr1.rd.data := addr1.read(io.data.addr1.rd.addr, io.data.addr1.rd.en)
  when(io.en && io.data.addr1.wr.en) {addr1.write(io.data.addr1.wr.addr, io.data.addr1.wr.data)}
  io.data.addr2.rd.data := addr2.read(io.data.addr2.rd.addr, io.data.addr2.rd.en)
  when(io.en && io.data.addr2.wr.en) {addr2.write(io.data.addr2.wr.addr, io.data.addr2.wr.data)}

  // For debug
  val wgid = if(CONFIG.DEBUG) Some(Reg(Vec(NUM_WG_SLOT, UInt(CONFIG.WG.WG_ID_WIDTH)))) else None
  val valid = if(CONFIG.DEBUG) Some(RegInit(VecInit.fill(NUM_WG_SLOT)(false.B))) else None
  if(CONFIG.DEBUG) {
    when(io.en && io.data.wgid.get.wr.en) {(wgid.get)(io.data.wgid.get.wr.addr) := io.data.wgid.get.wr.data}
    io.data.wgid.get.rd.data := (wgid.get)(io.data.wgid.get.rd.addr)
    when(io.en && io.data.valid.get.wr.en) {(valid.get)(io.data.valid.get.wr.addr) := io.data.valid.get.wr.data}
    io.data.valid.get.rd.data := (valid.get)(io.data.valid.get.rd.addr)

    assert(PopCount(valid.get) === cnt)
  }
}

/**
 * It is assumed that once alloc/dealloc.valid = true, it will keep valid until alloc/dealloc.fire
 */
class resource_table_top(val NUM_CU_PER_GROUP: Int = 2) extends Module {
  // Constants used in IO
  val NUM_LDS = CONFIG.WG.NUM_LDS_MAX
  val NUM_SGPR = CONFIG.WG.NUM_SGPR_MAX
  val NUM_VGPR = CONFIG.WG.NUM_VGPR_MAX
  // IO
  val io = IO(new Bundle{
    val alloc = Flipped(DecoupledIO(new io_alloc2rt))           // alloc   request from Allocator
    val dealloc = Flipped(DecoupledIO(new io_cuinterface2rt))   // dealloc request from CU interface
    val slot_dealloc = DecoupledIO(new io_rt2dealloc)           // dealloc request of WG/WF slot to Allocator
    val rtcache_lds  = DecoupledIO(new io_rt2cache(NUM_LDS ))   // rtcache update request to Allocator
    val rtcache_sgpr = DecoupledIO(new io_rt2cache(NUM_SGPR))
    val rtcache_vgpr = DecoupledIO(new io_rt2cache(NUM_VGPR))
    val cuinterface_wg_new = DecoupledIO(new io_rt2cuinterface) // The just-allocated WG info (resource baseaddr) to CU interface
  })
  // Constants
  val NUM_CU = CONFIG.GPU.NUM_CU
  val NUM_HANDLER_PER_GROUP = 1
  val NUM_HANDLER = NUM_CU / NUM_CU_PER_GROUP * NUM_HANDLER_PER_GROUP
  assert(NUM_CU >= NUM_CU_PER_GROUP && NUM_CU % NUM_CU_PER_GROUP == 0)
  assert(NUM_HANDLER_PER_GROUP == 1)  // This implement (direct mapping from handler to rtram) only supports NUM_HANDLER_PER_CU=1
  val NUM_WG_SLOT = CONFIG.GPU.NUM_WG_SLOT
  val NUM_RT_RESULT = CONFIG.RESOURCE_TABLE.NUM_RESULT

  // Auxiliary function
  // Convert alloc/dealloc request CU ID to RT_handler group ID and local CU ID
  def convert_cu_id(cu_id_global: UInt): (UInt, UInt) = {
    val cu_id_reversed = Reverse(cu_id_global)
    val cu_id_local = cu_id_reversed(log2Ceil(NUM_CU_PER_GROUP)-1, 0)
    val cu_id_group = if(NUM_CU > NUM_CU_PER_GROUP) cu_id_reversed(log2Ceil(NUM_CU)-1, log2Ceil(NUM_CU_PER_GROUP)) else WireInit(UInt(0.W), 0.U)
    (cu_id_group, cu_id_local)
  }

  // =
  // Hardware: resource table Handler and its resource table ram
  // =

  // hardware instantiate
  // We cannot index Seq[hardware] dynamically
  // If that is needed, we have to export hardware IO as Wire(Vec[IO])
  val handler_lds  = Seq.fill(NUM_HANDLER)(Module(new resource_table_handler(NUM_CU_PER_GROUP, NUM_LDS, NUM_RT_RESULT)))
  val handler_sgpr = Seq.fill(NUM_HANDLER)(Module(new resource_table_handler(NUM_CU_PER_GROUP, NUM_SGPR, NUM_RT_RESULT)))
  val handler_vgpr = Seq.fill(NUM_HANDLER)(Module(new resource_table_handler(NUM_CU_PER_GROUP, NUM_VGPR, NUM_RT_RESULT)))
  val rtram_lds  = Seq.fill(NUM_HANDLER, NUM_CU_PER_GROUP)(Module(new resource_table_ram(NUM_LDS , NUM_WG_SLOT)))
  val rtram_sgpr = Seq.fill(NUM_HANDLER, NUM_CU_PER_GROUP)(Module(new resource_table_ram(NUM_SGPR, NUM_WG_SLOT)))
  val rtram_vgpr = Seq.fill(NUM_HANDLER, NUM_CU_PER_GROUP)(Module(new resource_table_ram(NUM_VGPR, NUM_WG_SLOT)))
  // export IO signals of Seq[hardware] as Wire(Vec[IO])
  // these Wires are automatically connected to hardware.io, for example: handler_lds_io(0) <> handler_lds(0).io
  val handler_lds_io  = VecInit(handler_lds.map(_.io))
  val handler_sgpr_io = VecInit(handler_sgpr.map(_.io))
  val handler_vgpr_io = VecInit(handler_vgpr.map(_.io))
  val rtram_lds_io  = VecInit.tabulate(NUM_HANDLER, NUM_CU_PER_GROUP)((x, y) => rtram_lds(x)(y).io.data)
  val rtram_sgpr_io = VecInit.tabulate(NUM_HANDLER, NUM_CU_PER_GROUP)((x, y) => rtram_sgpr(x)(y).io.data)
  val rtram_vgpr_io = VecInit.tabulate(NUM_HANDLER, NUM_CU_PER_GROUP)((x, y) => rtram_vgpr(x)(y).io.data)

  for(i <- 0 until NUM_HANDLER; j <- 0 until NUM_CU_PER_GROUP) {
    // handler_lds_io(i).rtram_data, rtram_lds_io(i)(j) are both internal wires, we cannot use `<>` to connect them
    // In this implement, there are only 1 handler in a group, dynamic MUX of handler.rtram_data is not necessary
    // As a result of these two points, we use handler(i).rtram_data instead of handler_io(i).rtram_data
    handler_lds_io(i).rtram_data := DontCare
    handler_sgpr_io(i).rtram_data := DontCare
    handler_vgpr_io(i).rtram_data := DontCare
  }

  // Dynamic Mux within a group, between handler and rtram
  for(i <- 0 until NUM_HANDLER) {
    for(j <- 0 until NUM_CU_PER_GROUP) {
      rtram_lds(i)(j).io.en := (handler_lds(i).io.rtram_sel.bits.sel === j.U)
      handler_lds(i).io.rtram_data <> rtram_lds_io(i)(j)
    }
    handler_lds(i).io.rtram_data <> rtram_lds_io(i)(handler_lds(i).io.rtram_sel.bits.sel)
    handler_lds_io(i).rtram_sel.ready := true.B
    // in this implement, we use MUX to connect handler and the target rtram. Pure combinational logic is always ready
  }
  for(i <- 0 until NUM_HANDLER) {
    for(j <- 0 until NUM_CU_PER_GROUP) {
      rtram_sgpr(i)(j).io.en := (handler_sgpr(i).io.rtram_sel.bits.sel === j.U)
      handler_sgpr(i).io.rtram_data <> rtram_sgpr_io(i)(j)
    }
    handler_sgpr(i).io.rtram_data <> rtram_sgpr_io(i)(handler_sgpr(i).io.rtram_sel.bits.sel)
    handler_sgpr_io(i).rtram_sel.ready := true.B
  }
  for(i <- 0 until NUM_HANDLER) {
    for(j <- 0 until NUM_CU_PER_GROUP) {
      rtram_vgpr(i)(j).io.en := (handler_vgpr(i).io.rtram_sel.bits.sel === j.U)
      handler_vgpr(i).io.rtram_data <> rtram_vgpr_io(i)(j)
    }
    handler_vgpr(i).io.rtram_data <> rtram_vgpr_io(i)(handler_vgpr(i).io.rtram_sel.bits.sel)
    handler_vgpr_io(i).rtram_sel.ready := true.B
  }

  // =
  // ALLOC Router: At most one alloc request is allowed to stay within the whole resource table
  // =
  val (alloc_cuid_group, alloc_cuid_local) = convert_cu_id(io.alloc.bits.cu_id)
  val alloc_decoupledio = Module(new DecoupledIO_1_to_3(handler_lds_io(0).alloc.bits, handler_sgpr_io(0).alloc.bits, handler_vgpr_io(0).alloc.bits))
  // Alloc request recorder
  val alloc_record = RegInit(false.B)
  alloc_record := MuxCase(alloc_record, Seq(
    io.alloc.fire -> true.B,
    io.cuinterface_wg_new.fire -> false.B,
  ))
  val alloc_allowed = WireInit(!alloc_record || io.cuinterface_wg_new.fire)
  // Valid-ready
  alloc_decoupledio.io.in.valid := io.alloc.valid && alloc_allowed
  io.alloc.ready := alloc_decoupledio.io.in.ready && alloc_allowed
  // LDS
  alloc_decoupledio.io.in.bits.data0.cu_id := io.alloc.bits.cu_id
  alloc_decoupledio.io.in.bits.data0.wg_slot_id := io.alloc.bits.wg_slot_id
  alloc_decoupledio.io.in.bits.data0.num_resource := io.alloc.bits.num_lds
  alloc_decoupledio.io.in.bits.data0.cu_id_local := alloc_cuid_local
  // SGPR
  alloc_decoupledio.io.in.bits.data1.cu_id := io.alloc.bits.cu_id
  alloc_decoupledio.io.in.bits.data1.wg_slot_id := io.alloc.bits.wg_slot_id
  alloc_decoupledio.io.in.bits.data1.num_resource := io.alloc.bits.num_sgpr
  alloc_decoupledio.io.in.bits.data1.cu_id_local := alloc_cuid_local
  // VGPR
  alloc_decoupledio.io.in.bits.data2.cu_id := io.alloc.bits.cu_id
  alloc_decoupledio.io.in.bits.data2.wg_slot_id := io.alloc.bits.wg_slot_id
  alloc_decoupledio.io.in.bits.data2.num_resource := io.alloc.bits.num_vgpr
  alloc_decoupledio.io.in.bits.data2.cu_id_local := alloc_cuid_local
  // DEBUG
  if(CONFIG.DEBUG) {
    alloc_decoupledio.io.in.bits.data0.wg_id.get := io.alloc.bits.wg_id.get
    alloc_decoupledio.io.in.bits.data1.wg_id.get := io.alloc.bits.wg_id.get
    alloc_decoupledio.io.in.bits.data2.wg_id.get := io.alloc.bits.wg_id.get
  }
  // resource table handler
  for(i <- 0 until NUM_HANDLER) {
    handler_lds_io(i).alloc.valid := false.B
    handler_lds_io(i).alloc.bits := DontCare  // default value if it is not connected by MUX
    handler_sgpr_io(i).alloc.valid := false.B
    handler_sgpr_io(i).alloc.bits := DontCare
    handler_vgpr_io(i).alloc.valid := false.B
    handler_vgpr_io(i).alloc.bits := DontCare
  }
  // Bi-direction MUX
  alloc_decoupledio.io.out0 <> handler_lds_io(alloc_cuid_group).alloc
  alloc_decoupledio.io.out1 <> handler_sgpr_io(alloc_cuid_group).alloc
  alloc_decoupledio.io.out2 <> handler_vgpr_io(alloc_cuid_group).alloc


  // =
  // DEALLOC: (DecoupledIO 2-to-1)
  // 1. Router to RT handler (DecoupledIO 3-to-1)
  // 2. WG slot & WF slot dealloc (to allocator)
  // =

  val dealloc_decoupledio = Module(new DecoupledIO_1_to_3(handler_lds.head.io.dealloc.bits, handler_sgpr.head.io.dealloc.bits, handler_vgpr.head.io.dealloc.bits, IGNORE = true))
  val slot_dealloc = Module(new Queue(new io_rt2dealloc, 2))

  // DecoupledIO 2-to-1 valid-ready
  dealloc_decoupledio.io.in.valid := io.dealloc.valid && slot_dealloc.io.enq.ready
  slot_dealloc.io.enq.valid := io.dealloc.valid && dealloc_decoupledio.io.in.ready
  io.dealloc.ready := dealloc_decoupledio.io.in.ready && slot_dealloc.io.enq.ready

  // WG slot & WF slot dealloc
  slot_dealloc.io.enq.bits := io.dealloc.bits
  io.slot_dealloc <> slot_dealloc.io.deq

  val (dealloc_cuid_group, dealloc_cuid_local) = convert_cu_id(io.dealloc.bits.cu_id)
  // LDS
  dealloc_decoupledio.io.in.bits.data0.cu_id := io.dealloc.bits.cu_id
  dealloc_decoupledio.io.in.bits.data0.wg_slot_id := io.dealloc.bits.wg_slot_id
  dealloc_decoupledio.io.in.bits.data0.cu_id_local := dealloc_cuid_local
  dealloc_decoupledio.io.in.bits.ign0.get := !io.dealloc.bits.lds_dealloc_en  // if num_lds=0, no need to dealloc this WG
  // SGPR
  dealloc_decoupledio.io.in.bits.data1.cu_id := io.dealloc.bits.cu_id
  dealloc_decoupledio.io.in.bits.data1.wg_slot_id := io.dealloc.bits.wg_slot_id
  dealloc_decoupledio.io.in.bits.data1.cu_id_local := dealloc_cuid_local
  dealloc_decoupledio.io.in.bits.ign1.get := !io.dealloc.bits.sgpr_dealloc_en
  // VGPR
  dealloc_decoupledio.io.in.bits.data2.cu_id := io.dealloc.bits.cu_id
  dealloc_decoupledio.io.in.bits.data2.wg_slot_id := io.dealloc.bits.wg_slot_id
  dealloc_decoupledio.io.in.bits.data2.cu_id_local := dealloc_cuid_local
  dealloc_decoupledio.io.in.bits.ign2.get := !io.dealloc.bits.vgpr_dealloc_en
  // DEBUG
  if(CONFIG.DEBUG) {
    dealloc_decoupledio.io.in.bits.data0.wg_id.get := io.dealloc.bits.wg_id.get
    dealloc_decoupledio.io.in.bits.data1.wg_id.get := io.dealloc.bits.wg_id.get
    dealloc_decoupledio.io.in.bits.data2.wg_id.get := io.dealloc.bits.wg_id.get
  }
  // resource table handler
  for(i <- 0 until NUM_HANDLER) {
    handler_lds_io(i).dealloc.valid := false.B
    handler_lds_io(i).dealloc.bits := DontCare  // default value if it is not connected by MUX
    handler_sgpr_io(i).dealloc.valid := false.B
    handler_sgpr_io(i).dealloc.bits := DontCare
    handler_vgpr_io(i).dealloc.valid := false.B
    handler_vgpr_io(i).dealloc.bits := DontCare
  }
  // Bi-direction MUX
  dealloc_decoupledio.io.out0 <> handler_lds_io(dealloc_cuid_group).dealloc
  dealloc_decoupledio.io.out1 <> handler_sgpr_io(dealloc_cuid_group).dealloc
  dealloc_decoupledio.io.out2 <> handler_vgpr_io(dealloc_cuid_group).dealloc

  // =
  // resource table result (rtcache update) Router
  // =
  val rtcache_lds_valid = Wire(Vec(NUM_HANDLER, Bool()))
  val rtcache_sgpr_valid = Wire(Vec(NUM_HANDLER, Bool()))
  val rtcache_vgpr_valid = Wire(Vec(NUM_HANDLER, Bool()))
  for(i <- 0 until NUM_HANDLER) {
    rtcache_lds_valid(i) := handler_lds(i).io.rtcache_update.valid
    rtcache_sgpr_valid(i) := handler_sgpr(i).io.rtcache_update.valid
    rtcache_vgpr_valid(i) := handler_vgpr(i).io.rtcache_update.valid
  }

  for(i <- 0 until NUM_HANDLER) {
    handler_lds_io(i).rtcache_update.ready := false.B
    handler_sgpr_io(i).rtcache_update.ready := false.B
    handler_vgpr_io(i).rtcache_update.ready := false.B
  }
  io.rtcache_lds <> handler_lds_io(PriorityEncoder(rtcache_lds_valid)).rtcache_update
  io.rtcache_sgpr <> handler_sgpr_io(PriorityEncoder(rtcache_sgpr_valid)).rtcache_update
  io.rtcache_vgpr <> handler_vgpr_io(PriorityEncoder(rtcache_vgpr_valid)).rtcache_update

  // =
  // Base address router
  // =
  val baseaddr_lds_valid = Wire(Vec(NUM_HANDLER, Bool()))
  val baseaddr_sgpr_valid = Wire(Vec(NUM_HANDLER, Bool()))
  val baseaddr_vgpr_valid = Wire(Vec(NUM_HANDLER, Bool()))
  for(i <- 0 until NUM_HANDLER) {
    baseaddr_lds_valid(i)  := handler_lds(i).io.baseaddr.valid
    baseaddr_sgpr_valid(i) := handler_sgpr(i).io.baseaddr.valid
    baseaddr_vgpr_valid(i) := handler_vgpr(i).io.baseaddr.valid
  }
  for(i <- 0 until NUM_HANDLER) {
    handler_lds_io(i).baseaddr.ready  := false.B
    handler_sgpr_io(i).baseaddr.ready := false.B
    handler_vgpr_io(i).baseaddr.ready := false.B
  }
  val baseaddr_lds_reg  = Queue(handler_lds_io(PriorityEncoder(baseaddr_lds_valid)).baseaddr  , entries=1, pipe=true, flow=true)
  val baseaddr_sgpr_reg = Queue(handler_sgpr_io(PriorityEncoder(baseaddr_sgpr_valid)).baseaddr, entries=1, pipe=true, flow=true)
  val baseaddr_vgpr_reg = Queue(handler_vgpr_io(PriorityEncoder(baseaddr_vgpr_valid)).baseaddr, entries=1, pipe=true, flow=true)
  io.cuinterface_wg_new.valid := baseaddr_lds_reg.valid && baseaddr_sgpr_reg.valid && baseaddr_vgpr_reg.valid
  io.cuinterface_wg_new.bits.lds_base  := baseaddr_lds_reg.bits.addr
  io.cuinterface_wg_new.bits.sgpr_base := baseaddr_sgpr_reg.bits.addr
  io.cuinterface_wg_new.bits.vgpr_base := baseaddr_vgpr_reg.bits.addr
  baseaddr_lds_reg.ready  := io.cuinterface_wg_new.fire
  baseaddr_sgpr_reg.ready := io.cuinterface_wg_new.fire
  baseaddr_vgpr_reg.ready := io.cuinterface_wg_new.fire

  if(CONFIG.DEBUG) {
    when(io.cuinterface_wg_new.fire) {
      assert(baseaddr_lds_reg.bits.wg_id.get === baseaddr_sgpr_reg.bits.wg_id.get)
      assert(baseaddr_lds_reg.bits.wg_id.get === baseaddr_vgpr_reg.bits.wg_id.get)
    }
    io.cuinterface_wg_new.bits.wg_id.get := baseaddr_lds_reg.bits.wg_id.get
  }

  // Since at most 1 alloc request is allowed to allowed to stay in resource_table_top,
  //  there are at most 1 valid baseaddr result
  assert(PopCount(baseaddr_lds_valid)  <= 1.U)
  assert(PopCount(baseaddr_sgpr_valid) <= 1.U)
  assert(PopCount(baseaddr_vgpr_valid) <= 1.U)
}

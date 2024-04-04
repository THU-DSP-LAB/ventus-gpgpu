package cta_scheduler

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util._
import cta_util.sort3

// =
// Abbreviations:
// rt = resource table
// ⬑ = continuing from above, 接续上文
// =

class io_cuinterface2rt extends Bundle {
  val cu_id = UInt(log2Ceil(CONFIG.GPU.NUM_CU).W)
  val wg_slot_id = UInt(log2Ceil(CONFIG.GPU.NUM_WG_SLOT).W)
}

class io_ram(LEN: Int, DATA_WIDTH: Int) extends Bundle {
  val rd = new Bundle {
    val en = Bool()
    val addr = UInt(log2Ceil(LEN).W)        // if LEN==1, this won't be used
    val data = Flipped(UInt(DATA_WIDTH.W))
  }
  val wr = new Bundle {
    val en = Bool()
    val addr = UInt(log2Ceil(LEN).W)        // if LEN==1, this won't be used
    val data = UInt(DATA_WIDTH.W)
  }
  def apply(addr: UInt, rd_en: Bool = true.B): UInt = {
    rd.en := rd_en
    rd.addr := addr
    rd.data
  }
}

class io_reg(LEN: Int, DATA_WIDTH: Int) extends Bundle {
  val rd = new Bundle {
    val addr = UInt(log2Ceil(LEN).W)        // if LEN==1, this won't be used
    val data = Flipped(UInt(DATA_WIDTH.W))
  }
  val wr = new Bundle {
    val en = Bool()
    val addr = UInt(log2Ceil(LEN).W)        // if LEN==1, this won't be used
    val data = UInt(DATA_WIDTH.W)
  }
  def apply(addr: UInt): UInt = {
    rd.addr := addr
    rd.data
  }
  def apply(): UInt = {
    rd.data
  }
}


class io_rtram(NUM_RESOURCE: Int, NUM_WG_SLOT: Int = CONFIG.GPU.NUM_WG_SLOT) extends Bundle {
  // SyncReadMem
  val prev = new io_ram(NUM_WG_SLOT, log2Ceil(NUM_WG_SLOT))   // linked-list pointer to the previous WG, in order of resource address
  val next = new io_ram(NUM_WG_SLOT, log2Ceil(NUM_WG_SLOT))   // linked-list pointer to the next WG, in order of resource address
  val addr1 = new io_ram(NUM_WG_SLOT, log2Ceil(NUM_RESOURCE)) // base address of resource used by this WG
  val addr2 = new io_ram(NUM_WG_SLOT, log2Ceil(NUM_RESOURCE)) // last address of resource used by this WG
  // Reg or Mem
  val cnt  = new io_reg(1, log2Ceil(NUM_WG_SLOT+1))// number of WG in the linked-list
  val head = new io_reg(1, log2Ceil(NUM_WG_SLOT))  // the first WG in the linked-list
  val tail = new io_reg(1, log2Ceil(NUM_WG_SLOT))  // the last  WG in the linked-list
}

class resource_table_handler(NUM_CU_LOCAL: Int, NUM_RESOURCE: Int, NUM_RT_RESULT: Int = CONFIG.RESOURCE_TABLE.NUM_RESULT) extends Module {
  val io = IO(new Bundle {
    val dealloc = Flipped(DecoupledIO(new Bundle {
      val cu_id_local = UInt(log2Ceil(NUM_CU_LOCAL).W)
      val wg_slot_id = UInt(log2Ceil(CONFIG.GPU.NUM_WG_SLOT).W)
    }))
    val alloc = Flipped(DecoupledIO(new Bundle {
      val cu_id_local = UInt(log2Ceil(NUM_CU_LOCAL).W)
      val wg_slot_id = UInt(log2Ceil(CONFIG.GPU.NUM_WG_SLOT).W)
      val num_resource = UInt(log2Ceil(NUM_RESOURCE+1).W)
    }))
    val rtcache_update = DecoupledIO(new io_rt2cache(NUM_RESOURCE))
    val baseaddr = DecoupledIO(new Bundle {
      val cu_id_local = UInt(log2Ceil(NUM_CU_LOCAL).W)
      val addr = UInt(log2Ceil(NUM_RESOURCE).W)
    })
    val rtram = DecoupledIO(new Bundle {
      val sel = UInt(log2Ceil(NUM_CU_LOCAL).W)
      val data = new io_rtram(NUM_RESOURCE = NUM_RESOURCE, NUM_WG_SLOT = CONFIG.GPU.NUM_WG_SLOT)
    })
  })

  // =
  // Constants
  // =
  val NUM_WG_SLOT = CONFIG.GPU.NUM_WG_SLOT

  // =
  // Main FSM - define
  // =

  object FSM extends ChiselEnum {
    val IDLE, CONNECT_ALLOC, ALLOC, CONNECT_DEALLOC, DEALLOC, SCAN, OUTPUT = Value
  }
  val fsm_next = Wire(FSM.IDLE)
  val fsm = RegNext(fsm_next, FSM.IDLE)

  // sub-fsm finish signal
  val alloc_ok = Wire(Bool())
  val dealloc_ok = Wire(Bool())
  val scan_ok = Wire(Bool())
  val output_ok = Wire(Bool())

  // =
  // IO
  // =

  val cu_sel = Reg(UInt(log2Ceil(NUM_CU_LOCAL).W))
  val cu_sel_en = RegInit(false.B)
  io.rtram.valid := cu_sel_en
  io.rtram.bits.sel := cu_sel

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
    (fsm === FSM.SCAN) || (fsm === FSM.OUTPUT && output_ok)
  ))

  // @note:
  // io.rtram.bits.data.* may be driven by multiple sub-fsm: alloc, dealloc, scan.
  // We should have made each of these sub-fsm drive their own group of signals,
  // ⬑ and finally use MUX to select one out of these three groups of signals, becoming io.rtram.bits.data
  // But that makes the code too long to read.
  //                             fsm
  //                        ┌─────┴─────┐
  //    fsm_alloc    ─────→ ┤           │
  //                        │           │
  //    fsm_dealloc  ─────→ ┤    MUX    ├ ──────→ io.rtram.bits.data (output)
  //                        │           │
  //    fsm_scan     ─────→ ┤           │
  //                        └───────────┘
  //    fsm_alloc    ←──────┐
  //                        │ (No need to implement any hardware, just read io.rtram.bits.data)
  //    fsm_dealloc  ←──────┼────────── io.rtram.bits.data (input)
  //                        │
  //    fsm_scan     ←──────┘
  //
  // Here, we let this multi-driven problem exist.
  // Chisel will automatically merge these multiple driven-sources into one, using PriorityMux.
  // What we really need is Mux1H, since at most one sub-fsm is active, but that's ok

  // TODO: in chisel3.6, we can use something link this
  //  io.rtram.bits.data :<= MuxLookup(fsm, DontCare, Seq(
  //    FSM.ALLOC -> rtram_alloc,
  //    FSM.DEALLOC -> rtram_dealloc,
  //    FSM.SCAN -> rtram_scan,
  //  ))
  //  io.rtram.bits.data :>= rtram_alloc
  //  io.rtram.bits.data :>= rtram_dealloc
  //  io.rtram.bits.data :>= rtram_scan
  val rtram_alloc   = WireInit(new io_rtram(NUM_RESOURCE = NUM_RESOURCE, NUM_WG_SLOT = CONFIG.GPU.NUM_WG_SLOT), DontCare)
  val rtram_dealloc = WireInit(new io_rtram(NUM_RESOURCE = NUM_RESOURCE, NUM_WG_SLOT = CONFIG.GPU.NUM_WG_SLOT), DontCare)
  val rtram_scan    = WireInit(new io_rtram(NUM_RESOURCE = NUM_RESOURCE, NUM_WG_SLOT = CONFIG.GPU.NUM_WG_SLOT), DontCare)
  when(fsm === FSM.SCAN) {
    io.rtram.bits.data <> rtram_scan
  } .elsewhen(fsm === FSM.DEALLOC) {
    io.rtram.bits.data <> rtram_dealloc
  } .otherwise {
    io.rtram.bits.data <> rtram_alloc
  }

  // =
  // Main FSM - actions
  // =

  // get WG info when a new alloc/dealloc request is received
  val wgsize = Reg(io.alloc.bits.num_resource)
  val wgslot = Reg(io.alloc.bits.wg_slot_id)

  assert(io.alloc.fire && io.dealloc.fire === false.B) // only one request can be received in a same cycle
  wgsize := Mux(io.alloc.fire, io.alloc.bits.num_resource, wgsize)
  wgslot := Mux1H(Seq(
    io.alloc.fire -> io.alloc.bits.wg_slot_id,
    io.dealloc.fire -> io.dealloc.bits.wg_slot_id,
    (!io.alloc.fire && !io.dealloc.fire) -> wgslot,
  ))

  // get new target CU when a different CU's request is received in IDLE state
  // But there is no need to check whether fsm===IDLE
  // Since if (fsm=/=IDLE) && (req.fire), there must be (cu_sel===new_cu_id), new assignment has no effect (see the next assert)
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
  // Sub FSM ALLOC
  // =

  object FSM_A extends ChiselEnum {
    val IDLE, FIND, WRITE_OUTPUT = Value
  }
  val fsm_a = RegInit(FSM_A.IDLE)

  // Sub-FSM ALLOC action 1:
  // Iterate over the linked-list, and find a proper base address for the new alloc request
  // Number of WG in the linked-list: rtram.cnt
  // Number of idle resource segment which need to be checked: rtram.cnt + 1, so 0 ≤ fsm_a_cnt ≤ rtram.cnt
  val fsm_a_cnt = RegInit(0.U(log2Ceil(NUM_WG_SLOT+2).W))     // number of idle resource segment which has already been checked
  val fsm_a_found_ok = WireInit(Bool(), DontCare)             // final result will be given out next cycle, so it's ready to jump to the next state
  val fsm_a_found_size = Reg(UInt(log2Ceil(NUM_RESOURCE+1).W))// size of the currently selected resource segment
  val fsm_a_found_addr = Reg(UInt(log2Ceil(NUM_RESOURCE).W))  // base address of the currently selected resource segment
  val fsm_a_found_ptr1 = Reg(UInt(log2Ceil(NUM_WG_SLOT).W))   // pointer to the WG which locates before the currently selected resource segment
  val fsm_a_found_ptr2 = Reg(UInt(log2Ceil(NUM_WG_SLOT).W))   // pointer to the WG which locates after the currently selected resource segment
  val fsm_a_ptr1 = Reg(UInt(log2Ceil(NUM_WG_SLOT).W))         // pointer to the WG which locate before the currently checked resource segments
  val fsm_a_ptr2 = Wire(UInt(log2Ceil(NUM_WG_SLOT).W))        // pointer to the WG which locate after  the currently checked resource segments
  val fsm_a_head_flag, fsm_a_tail_flag = Reg(Bool())          // if the currently selected segment will be the head/tail node of the linked-list
  // if you want to send control/data signal to io.rtram, use rtram_alloc
  // if you want to get signal value from io.rtram, use rtram_alloc(recommended) or io.rtram.bits.data, both ok
  switch(fsm_a) {
    is(FSM_A.IDLE) {  // prepare for FSM_A.FIND
      // Even before the iteration begins, we have already know that we can successfully find a resource segment which is large enough
      // What we want to find is the smallest segment that can contain the WG (large but not too large)
      // At the beginning, assume we will find the largest segment (the whole resource, NUM_RESOURCE)
      // ⬑ then, step by step we will find smaller segments
      // In the first iteration step, we will check the resource segment before the first WG, even if it may not exist
      // In the last iteration step, we will check the resource segment after the last WG, even if it may not exist
      fsm_a_cnt := 0.U                        // Iteration range: 0 to rtram.cnt
      fsm_a_found_size := NUM_RESOURCE.U      // Initial assumption: assume we select the whole resource.
      fsm_a_found_addr := 0.U                 // ⬑ This will be changed to the right segment later, unless rtram.cnt==0 and the initial assumption is correct
      fsm_a_found_ptr1 := DontCare            // In our initial assumption, the linked-list is empty, so we don't care this pointer
      fsm_a_found_ptr2 := DontCare
      fsm_a_ptr1 := DontCare                  // Before the first resource segment locates nothing
      fsm_a_ptr2 := rtram_alloc.head()        // After the first resource segment locates the first WG
      fsm_a_head_flag := true.B
      fsm_a_tail_flag := true.B               // In our initial assumption, the linked-list is empty, so the new WG will become head as well as tail
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
      val init_p1 = RegNext(fsm_a_cnt === rtram_alloc.cnt())
      val finish_p1 = RegNext(fsm_a_cnt === rtram_scan.cnt())
      // pipeline stage 3: resource segment size calc & found result update
      val addr1, addr2 = Wire(UInt(log2Ceil(NUM_RESOURCE).W)) // addr1 = this resource segment's start addr - 1, addr2 = resource segment's end addr
      addr1 := Mux(init_p1, (-1).U(log2Ceil(NUM_RESOURCE).W), rtram_alloc.addr2.rd.data)
      addr2 := Mux(finish_p1, (NUM_RESOURCE-1).U, rtram_alloc.addr1.rd.data - 1.U)
      val size = WireInit(UInt(log2Ceil(NUM_RESOURCE+1).W), addr2 - addr1) // resource segment size
      val result_update = (size >= wgsize) && (size < fsm_a_found_size)
      fsm_a_found_size := Mux(result_update, size, fsm_a_found_size)
      fsm_a_found_addr := Mux(result_update, addr1 + 1.U, fsm_a_found_addr)
      fsm_a_found_ptr1 := Mux(result_update, fsm_a_ptr1, fsm_a_found_ptr1)
      fsm_a_found_ptr2 := Mux(result_update, fsm_a_ptr2, fsm_a_found_ptr2)
      fsm_a_tail_flag := result_update && (fsm_a_cnt === rtram_alloc.cnt())
      fsm_a_head_flag := result_update && (fsm_a_cnt === 0.U)
      // Iteration step
      fsm_a_cnt := fsm_a_cnt + 1.U
      // pipeline will give out the final result in the next cycle, so it's ready to jump to the next state
      fsm_a_found_ok := finish_p1
    }
  }

  // Sub-FSM ALLOC action 2 (FSM_A.WRITE_OUTPUT):
  // Write the new WG to the location just found in FSM_A.FIND
  val fsm_a_write_cnt = Reg(UInt(1.W))  // you can also reuse fsm_a_cnt
  fsm_a_write_cnt := MuxCase(fsm_a_write_cnt, Seq(
    (fsm_a =/= FSM_A.WRITE_OUTPUT) -> 0.U,
    (fsm_a_write_cnt <= 1.U) -> (fsm_a_write_cnt + 1.U),
  ))
  rtram_alloc.head.wr.en := (fsm_a === FSM_A.WRITE_OUTPUT) && (fsm_a_write_cnt === 0.U) && fsm_a_head_flag
  rtram_alloc.head.wr.data := wgslot
  rtram_alloc.tail.wr.en := (fsm_a === FSM_A.WRITE_OUTPUT) && (fsm_a_write_cnt === 0.U) && fsm_a_tail_flag
  rtram_alloc.tail.wr.data := wgslot
  rtram_alloc.cnt.wr.en := (fsm_a === FSM_A.WRITE_OUTPUT) && (fsm_a_write_cnt === 0.U)
  rtram_alloc.cnt.wr.data := rtram_alloc.cnt() + 1.U
  rtram_alloc.prev.wr.en := (fsm_a === FSM_A.WRITE_OUTPUT) && (fsm_a_write_cnt <= 1.U)
  rtram_alloc.prev.wr.addr := (Mux(fsm_a_write_cnt === 0.U, fsm_a_found_ptr2, wgslot))
  rtram_alloc.prev.wr.data := (Mux(fsm_a_write_cnt === 0.U, wgslot, fsm_a_found_ptr1))
  rtram_alloc.next.wr.en := (fsm_a === FSM_A.WRITE_OUTPUT) && (fsm_a_write_cnt <= 1.U)
  rtram_alloc.next.wr.addr := (Mux(fsm_a_write_cnt === 0.U, fsm_a_found_ptr1, wgslot))
  rtram_alloc.next.wr.data := (Mux(fsm_a_write_cnt === 0.U, wgslot, fsm_a_found_ptr2))
  rtram_alloc.addr1.wr.en := (fsm_a === FSM_A.WRITE_OUTPUT) && (fsm_a_write_cnt === 0.U)
  rtram_alloc.addr1.wr.addr := wgslot
  rtram_alloc.addr1.wr.data := fsm_a_found_addr
  rtram_alloc.addr2.wr.en := (fsm_a === FSM_A.WRITE_OUTPUT) && (fsm_a_write_cnt === 0.U)
  rtram_alloc.addr1.wr.addr := wgslot
  rtram_alloc.addr1.wr.data := fsm_a_found_addr + wgsize - 1.U
  val fsm_a_write_ok = fsm_a_write_cnt.orR

  // if the new WG becomes the head node of the linked-list, base address must be 0
  assert(!(fsm_a =/= FSM_A.WRITE_OUTPUT) || fsm_a_head_flag === (fsm_a_found_addr === 0.U))

  // Sub-FSM ALLOC action 3 (FSM_A.WRITE_OUTPUT):
  // Output the found base address to CU Interface (may be there is a FIFO in resource_table_top)
  val fsm_a_output_ok = RegInit(false.B)
  fsm_a_output_ok := Mux(fsm_a === FSM_A.WRITE_OUTPUT, fsm_a_output_ok || io.baseaddr.fire, false.B)
  io.baseaddr.valid := (fsm_a === FSM_A.WRITE_OUTPUT) && !fsm_a_output_ok
  io.baseaddr.bits.cu_id_local := cu_sel
  io.baseaddr.bits.addr := fsm_a_found_addr

  // Sub-FSM state transition logic
  fsm_a := MuxLookup(fsm_a.asUInt, FSM_A.IDLE, Seq(
    FSM_A.IDLE.asUInt -> Mux(fsm_next === FSM.ALLOC && fsm_next =/= fsm, FSM_A.FIND, fsm_a),
    FSM_A.FIND.asUInt -> Mux(fsm_a_cnt > io.rtram.bits.data.cnt(), FSM_A.WRITE_OUTPUT, fsm_a),
    FSM_A.WRITE_OUTPUT.asUInt -> Mux(fsm_a_write_ok && fsm_a_output_ok, FSM_A.IDLE, fsm_a),
  ))

  alloc_ok := (fsm_a === FSM_A.WRITE_OUTPUT) && fsm_a_write_ok && fsm_a_output_ok

  // =
  // Sub-FSM DEALLOC
  // =
  val fsm_d = RegInit(0.U(2.W))
  fsm_d := Mux(fsm =/= FSM.DEALLOC, 0.U, Mux(!fsm_d.andR, fsm_d + 1.U, fsm_d))
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
  rtram_dealloc.head.wr.en := (fsm_d === 1.U) && (wgslot === rtram_dealloc.tail())
  rtram_dealloc.head.wr.data := fsm_d_prev
  rtram_dealloc.addr2.wr.en := false.B
  rtram_dealloc.addr2.wr.en := false.B

  dealloc_ok := (fsm === FSM.DEALLOC) && (fsm_d >= 1.U)

  // =
  // Sub-FSM SCAN
  // =
  val fsm_s_ok = RegInit(false.B)
  val fsm_s_cnt = RegInit(0.U(log2Ceil(NUM_WG_SLOT+2).W))   // you can also reuse fsm_a_cnt
  fsm_s_cnt := MuxCase(fsm_s_cnt, Seq(
    (fsm =/= FSM.SCAN) -> 0.U,
    (!fsm_s_ok) -> (fsm_s_cnt + 1.U),
  ))
  val fsm_s_ptr1 = Reg(UInt(log2Ceil(NUM_WG_SLOT).W))         // pointer to the WG which locate before the currently checked resource segments
  val fsm_s_ptr2 = Wire(UInt(log2Ceil(NUM_WG_SLOT).W))        // pointer to the WG which locate after  the currently checked resource segments
  val rtcache_data = RegInit(VecInit.fill(NUM_RT_RESULT)(0.U(log2Ceil(NUM_RESOURCE).W)))
  when(fsm === FSM.SCAN){
    when(!fsm_s_ok) {
      // pipeline stage 0: pointer step
      fsm_s_ptr1 := fsm_s_ptr2
      rtram_scan.next(fsm_s_ptr2)       // rtram_scan.next.rd.data is a pipeline stage 0 register
      // pipeline stage 1: rtram data fetch - addr1 & addr2
      fsm_s_ptr2 := Mux(fsm_s_cnt === 0.U, rtram_scan.head(), rtram_scan.next.rd.data)
      rtram_scan.addr2(fsm_s_ptr1)      // rtram_scan.addr2.rd.data is a pipeline stage 1 register
      rtram_scan.addr1(fsm_s_ptr2)      // rtram_scan.addr1.rd.data is a pipeline stage 1 register
      val init_p1 = RegNext(fsm_s_cnt === 0.U)
      val finish_p1 = RegNext(fsm_s_cnt === rtram_scan.cnt())
      // pipeline stage 2: resource segment size calc & sort
      val addr1 = WireInit(UInt(log2Ceil(NUM_RESOURCE).W), Mux(init_p1, (-1).U(log2Ceil(NUM_RESOURCE).W), rtram_scan.addr2.rd.data))
      val addr2 = WireInit(UInt(log2Ceil(NUM_RESOURCE).W), Mux(finish_p1, NUM_RESOURCE.U - 1.U, rtram_scan.addr1.rd.data - 1.U))
      val thissize = WireInit(UInt(log2Ceil(NUM_RESOURCE).W), addr2 - addr1)
      assert(NUM_RT_RESULT == 2)   // Current implement only support NUM_RT_RESULT=2. Replace `sort3()` to support other values
      val result = sort3(rtcache_data(0), rtcache_data(1), thissize)
      rtcache_data(0) := result(0)
      rtcache_data(1) := result(1)
      fsm_s_ok := finish_p1
    }
  } .otherwise { // prepare for FSM.SCAN
    fsm_s_ptr1 := DontCare
    fsm_s_ptr2 := rtram_scan.cnt()
    rtcache_data(0) := 0.U
    rtcache_data(1) := 0.U
    fsm_s_ok := false.B
  }
  scan_ok := (fsm === FSM.SCAN) && fsm_s_ok

  // =
  // Main FSM - state transition logic
  // =

  switch(fsm) {
    is(FSM.IDLE) {
      fsm_next := MuxCase(fsm, Seq(
        io.alloc.fire -> Mux(cu_sel === io.alloc.bits.cu_id_local && io.rtram.fire, FSM.ALLOC, FSM.CONNECT_ALLOC),
        io.dealloc.fire -> Mux(cu_sel === io.dealloc.bits.cu_id_local && io.rtram.fire, FSM.ALLOC, FSM.CONNECT_DEALLOC),
      ))
    }
    is(FSM.CONNECT_ALLOC, FSM.CONNECT_DEALLOC) {
      fsm_next := MuxCase(fsm, Seq(
        io.rtram.fire -> Mux(fsm === FSM.CONNECT_ALLOC, FSM.ALLOC, FSM.DEALLOC),
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

class resource_table_top extends Module {
  val io = IO(new Bundle{
    val dealloc = Flipped(DecoupledIO(new io_cuinterface2rt))
    val alloc = Flipped(DecoupledIO(new io_alloc2rt))
    val rtcache_lds  = DecoupledIO(new io_rt2cache(CONFIG.WG.NUM_LDS_MAX ))
    val rtcache_sgpr = DecoupledIO(new io_rt2cache(CONFIG.WG.NUM_SGPR_MAX))
    val rtcache_vgpr = DecoupledIO(new io_rt2cache(CONFIG.WG.NUM_VGPR_MAX))
    val cuinterface_wg_new = DecoupledIO(new io_rt2cuinterface)
  })

  val NUM_CU = CONFIG.GPU.NUM_CU
  val NUM_CU_PER_HANDLER = 2
  val NUM_HANDLER = NUM_CU / NUM_CU_PER_HANDLER
  assert(NUM_CU % NUM_CU_PER_HANDLER == 0)

  val NUM_WG_SLOT = CONFIG.GPU.NUM_WG_SLOT

  // =
  // Resouce table RAM
  // =



}


//trait rt_datatype extends Bundle {
//  def NUM_RESOURCE: Int
//  val size = UInt(log2Ceil(NUM_RESOURCE).W)
//}

//class io_cache2rt(NUM_RESOURCE: Int) extends Bundle with rt_datatype {
//  override def NUM_RESOURCE: Int = NUM_RESOURCE
//  val cu_id = UInt(log2Ceil(CONFIG.GPU.NUM_CU-1).W)
//  val wgslot_id = UInt(log2Ceil(CONFIG.GPU.NUM_WG_SLOT-1).W)
//}

//class rtcache(NUM_RESOURCE_MAX: Int, NUM_RT_RESULT: Int = CONFIG.RESOURCE_TABLE.NUM_RESULT) extends Module {
//  val io = IO(new Bundle {
//    val check = new Bundle{
//      val size = Input(UInt(log2Ceil(NUM_RESOURCE_MAX).W))
//      val cu_valid = Output(Vec(CONFIG.GPU.NUM_CU, Bool()))
//      val
//    }
//    val alloc = Flipped(DecoupledIO(new io_cache2rt(NUM_RESOURCE_MAX)))
//    val rt_alloc = DecoupledIO(new io_cache2rt(NUM_RESOURCE_MAX))
//    val rt_result = Flipped(DecoupledIO(new io_rt2cache(NUM_RESOURCE_MAX, NUM_RT_RESULT)))
//  })
//
//  val NUM_CU = CONFIG.GPU.NUM_CU
//
//  val cache = RegInit(VecInit.fill(NUM_CU, NUM_RT_RESULT)(0.U(log2Ceil(NUM_RESOURCE_MAX-1).W)))
//  val cacheValid = RegInit(VecInit.tabulate(NUM_CU, NUM_RT_RESULT){ (cu_id, result_id) =>
//    if(result_id == 0) {true.B}
//    else {false.B}
//  })
//
//  // =
//  // Main function 1
//  // WG resource check, combinational data path
//  // =
//
//  for(i <- 0 until NUM_CU) {
//    val resource_valid = Wire(Vec(NUM_RT_RESULT, Bool()))
//    for(j <- 0 until NUM_RT_RESULT) {
//      resource_valid(i) := cacheValid(i)(j) && (cache(i)(j) >= io.check.size)
//    }
//    io.check.cu_valid(i) := resource_valid(i).orR
//    io.check
//  }
//}

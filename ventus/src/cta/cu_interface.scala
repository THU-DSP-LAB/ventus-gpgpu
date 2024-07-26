package cta

import chisel3._
import chisel3.util._
import chisel3.experimental.dataview._

/**
 * CU interface:
 * 1. split WG into WF and send them to CU one-by-one
 * 2. gather WF of WG. After all WF of a WG finish, send dealloc request to ResourceTable and report to Host
 * @see docs/cta_scheduler/CU_interface.md
 */
class cu_interface extends Module {
  val NUM_CU: Int = CONFIG.GPU.NUM_CU
  val io = IO(new Bundle{
    val init_ok = Output(Bool())    // indicate if this Module has finished its init
    val wgbuffer_wg_new = Flipped(DecoupledIO(new io_buffer2cuinterface))     // new WG info from WGram2
    val alloc_wg_new = Flipped(DecoupledIO(new io_alloc2cuinterface))         // new WG info from Allocator
    val rt_wg_new = Flipped(DecoupledIO(new io_rt2cuinterface))               // new WG info from ResourceTable
    val cu_wf_new = Vec(NUM_CU, DecoupledIO(new io_cuinterface2cu))           // new WF to CU
    val cu_wf_done = Vec(NUM_CU, Flipped(DecoupledIO(new io_cu2cuinterface))) // WF that finishes its execution
    val rt_dealloc = DecoupledIO(new io_cuinterface2rt)                       // dealloc request to ResourceTable
    val host_wg_done = DecoupledIO(new io_cta2host)                           // report to host which WG has finished its execution
  })
  val NUM_WG_SLOT = CONFIG.GPU.NUM_WG_SLOT
  val NUM_WF_MAX = CONFIG.WG.NUM_WF_MAX
  val NUM_LDS  = CONFIG.WG.NUM_LDS_MAX
  val NUM_SGPR = CONFIG.WG.NUM_SGPR_MAX
  val NUM_VGPR = CONFIG.WG.NUM_VGPR_MAX
  val DEBUG = CONFIG.DEBUG
  class wftag_datatype extends Bundle {
    val wg_slot_id = UInt(log2Ceil(NUM_WG_SLOT).W)
    val wf_id = UInt(log2Ceil(NUM_WF_MAX).W)
  }

  // =
  // Main function 1: gather WG info from Allocator, Resource table and WGram2
  // =

  class cta_data extends Bundle with ctainfo_alloc_to_cuinterface with ctainfo_alloc_to_cu with ctainfo_host_to_cuinterface with ctainfo_host_to_cu {
    val wg_id = UInt(CONFIG.WG.WG_ID_WIDTH)
  }
  val fifo = Module(new Queue(new cta_data, 2))

  if(DEBUG) {
    when(fifo.io.enq.fire) {
      assert(io.wgbuffer_wg_new.bits.wg_id === io.alloc_wg_new.bits.wg_id.get)
      assert(io.rt_wg_new.bits.wg_id.get === io.alloc_wg_new.bits.wg_id.get)
    }
  }
  // DecoupledIO 3-to-1
  io.wgbuffer_wg_new.ready := io.alloc_wg_new.valid && io.rt_wg_new.valid && fifo.io.enq.ready
  io.alloc_wg_new.ready := io.wgbuffer_wg_new.valid && io.rt_wg_new.valid && fifo.io.enq.ready
  io.rt_wg_new.ready := io.wgbuffer_wg_new.valid && io.alloc_wg_new.valid && fifo.io.enq.ready
  fifo.io.enq.valid := io.wgbuffer_wg_new.valid && io.alloc_wg_new.valid && io.rt_wg_new.valid
  fifo.io.enq.bits.wg_id := io.wgbuffer_wg_new.bits.wg_id
  fifo.io.enq.bits.viewAsSupertype(new ctainfo_host_to_cuinterface {}) := io.wgbuffer_wg_new.bits
  fifo.io.enq.bits.viewAsSupertype(new ctainfo_host_to_cu {}) := io.wgbuffer_wg_new.bits
  fifo.io.enq.bits.viewAsSupertype(new ctainfo_alloc_to_cuinterface {}) := io.alloc_wg_new.bits
  fifo.io.enq.bits.viewAsSupertype(new ctainfo_alloc_to_cu {}) := io.rt_wg_new.bits

  // =
  // Main function 2: split WG into WF
  // =

  // the value of splitter_cnt means how many WF is waiting for being sent to CU
  // splitter_cnt==0 means no WF is waiting to be sent, we are waiting for the next WG
  val splitter_cnt = RegInit(0.U(log2Ceil(NUM_WF_MAX + 1).W))
  val splitter_lds_addr = WireInit(fifo.io.deq.bits.lds_base)
  val splitter_sgpr_addr = Reg(UInt(log2Ceil(NUM_SGPR).W)) // sgpr base of WF, its value steps num_sgpr_per_wf every time
  val splitter_vgpr_addr = Reg(UInt(log2Ceil(NUM_VGPR).W)) // vgpr base of WF, its value steps num_vgpr_per_wf every time
  val splitter_load_new = (splitter_cnt === 0.U) && fifo.io.deq.valid   // A new WG will be loaded to splitter
  fifo.io.deq.ready := (splitter_cnt === 1.U) && io.cu_wf_new(fifo.io.deq.bits.cu_id).ready

  splitter_cnt := MuxCase(0.U, Seq(
    (splitter_cnt =/= 0.U) -> Mux(io.cu_wf_new(fifo.io.deq.bits.cu_id).fire, splitter_cnt - 1.U, splitter_cnt),
    (splitter_load_new) -> (fifo.io.deq.bits.num_wf),
  ))
  splitter_sgpr_addr := MuxCase(splitter_sgpr_addr, Seq(
    splitter_load_new -> fifo.io.deq.bits.sgpr_base,
    io.cu_wf_new(fifo.io.deq.bits.cu_id).fire -> (splitter_sgpr_addr + fifo.io.deq.bits.num_sgpr_per_wf),
  ))
  splitter_vgpr_addr := MuxCase(splitter_vgpr_addr, Seq(
    splitter_load_new -> fifo.io.deq.bits.vgpr_base,
    io.cu_wf_new(fifo.io.deq.bits.cu_id).fire -> (splitter_vgpr_addr + fifo.io.deq.bits.num_vgpr_per_wf),
  ))
  assert(splitter_cnt <= 1.U || NUM_SGPR.U - splitter_sgpr_addr > fifo.io.deq.bits.num_sgpr_per_wf)
  assert(splitter_cnt <= 1.U || NUM_VGPR.U - splitter_vgpr_addr > fifo.io.deq.bits.num_vgpr_per_wf)
  assert(splitter_cnt === 0.U || fifo.io.deq.valid)

  for(i <- 0 until NUM_CU) {
    io.cu_wf_new(i).valid := (splitter_cnt =/= 0.U) && (fifo.io.deq.bits.cu_id === i.U)
    io.cu_wf_new(i).bits.viewAsSupertype(new ctainfo_host_to_cu {}) := fifo.io.deq.bits
    io.cu_wf_new(i).bits.lds_base := splitter_lds_addr
    io.cu_wf_new(i).bits.sgpr_base := splitter_sgpr_addr
    io.cu_wf_new(i).bits.vgpr_base := splitter_vgpr_addr
    io.cu_wf_new(i).bits.wg_id := fifo.io.deq.bits.wg_id
    io.cu_wf_new(i).bits.num_wf := fifo.io.deq.bits.num_wf
    io.cu_wf_new(i).bits.wf_tag := { val wftag = Wire(new wftag_datatype)
      wftag.wg_slot_id := fifo.io.deq.bits.wg_slot_id
      wftag.wf_id := fifo.io.deq.bits.num_wf - splitter_cnt
      wftag.asUInt
    }
  }

  // =
  // Main function 3: store WG info for finishing check and dealloc
  // =

  // data that needed by WF_gather, dealloc, or Host
  class cta_data_1 extends Bundle {
    val num_wf = UInt(log2Ceil(NUM_WF_MAX+1).W) // Number of wavefront in this cta
    val wg_id = UInt(CONFIG.WG.WG_ID_WIDTH)
    val lds_dealloc_en = Bool()   // whether LDS needs dealloc. When num_lds==0, lds do not need dealloc
    val sgpr_dealloc_en = Bool()
    val vgpr_dealloc_en = Bool()
  }
  val wf_gather_ram = SyncReadMem(NUM_CU * NUM_WG_SLOT, new cta_data_1)
  val wf_gather_ram_valid = if(DEBUG) Some(RegInit(VecInit.fill(NUM_CU, NUM_WG_SLOT)(false.B))) else None
  def global_wgslot_calc(cu: UInt, wgslot: UInt): UInt = {cu * NUM_WG_SLOT.U + wgslot}

  when(splitter_load_new) {
    if(DEBUG) { (wf_gather_ram_valid.get)(fifo.io.deq.bits.cu_id)(fifo.io.deq.bits.wg_slot_id) := true.B}
    val wf_gather_ram_data = Wire(new cta_data_1)
    wf_gather_ram_data := fifo.io.deq.bits
    wf_gather_ram.write(global_wgslot_calc(fifo.io.deq.bits.cu_id, fifo.io.deq.bits.wg_slot_id), wf_gather_ram_data)
  }

  // =
  // FSM: gather WF from CU, generate dealloc to Resource table, generate host_wg_done (Main function 4~6)
  // =

  object FSM extends ChiselEnum {
    val GET_WF = Value    // receive WF_done from CU (arbiter), and read WF counter for how many WF of this WG has already finished
    val UPDATE = Value    // update WF counter. If all WF of this WG finished, try to send dealloc request and Host report
    val DEALLOC = Value   // if dealloc request or Host report wasn't sent successfully in FSM.UPDATE, try it again
  }
  val fsm = RegInit(FSM.GET_WF)

  // =
  // Main function 4 (FSM action 1): gather WF from CU
  // =

  // select a CU whose wf_done is valid
  val arb_inst = Module(new RRArbiter[io_cu2cuinterface](new io_cu2cuinterface, NUM_CU))
  for(i <- 0 until NUM_CU) { arb_inst.io.in(i) <> io.cu_wf_done(i) }
  arb_inst.io.out.ready := (fsm === FSM.GET_WF)

  // get new info of wf_done
  val wf_cu_wire = arb_inst.io.chosen
  val wf_cu_reg = RegEnable(arb_inst.io.chosen, arb_inst.io.out.fire)
  val wf_wgslot_wire = arb_inst.io.out.bits.wf_tag.asTypeOf(new wftag_datatype).wg_slot_id
  val wf_wgslot_reg = RegEnable(wf_wgslot_wire, arb_inst.io.out.fire)

  // WF counter of how many WF of a WG has finished, this should be init to all zero
  val wf_gather_cnt = SyncReadMem(NUM_CU * NUM_WG_SLOT, UInt(log2Ceil(NUM_WF_MAX+1).W))
  val init_wf_gather_cnt_idx = RegInit(0.U(log2Ceil(NUM_CU * NUM_WG_SLOT + 1).W))
  init_wf_gather_cnt_idx := Mux(init_wf_gather_cnt_idx === (NUM_CU * NUM_WG_SLOT).U, init_wf_gather_cnt_idx, init_wf_gather_cnt_idx + 1.U)
  io.init_ok := (init_wf_gather_cnt_idx === (NUM_CU * NUM_WG_SLOT).U)

  // when a new wf_done req is received, read related WG info from SyncReadMem
  val wf_gather_cnt_read_data = wf_gather_cnt.read(global_wgslot_calc(wf_cu_wire, wf_wgslot_wire), arb_inst.io.out.fire)
  val wf_gather_ram_read_data = { // The read-out data will be used in the following several cycles, we need to hold the last read-out data
    val tmp = RegNext(arb_inst.io.out.fire, false.B)
    val data = wf_gather_ram.read(global_wgslot_calc(wf_cu_wire, wf_wgslot_wire), arb_inst.io.out.fire)
    Mux(tmp, data, RegEnable(data, tmp))
  }

  // after finishing reading WG info of the new wf_done, update WF counter
  val wf_gather_finish = (wf_gather_cnt_read_data + 1.U === wf_gather_ram_read_data.num_wf)
  when(init_wf_gather_cnt_idx =/= (NUM_CU * NUM_WG_SLOT).U) {
    wf_gather_cnt.write(init_wf_gather_cnt_idx, 0.U)    // Module init
  } .elsewhen(fsm === FSM.UPDATE) {
    wf_gather_cnt.write(global_wgslot_calc(wf_cu_reg, wf_wgslot_reg),
      // if all WF of this WG has finished, reset WF counter for next-time using
      Mux(wf_gather_finish, 0.U, wf_gather_cnt_read_data + 1.U)
    )
    if(DEBUG) {
      assert((wf_gather_ram_valid.get)(wf_cu_reg)(wf_wgslot_reg))
      (wf_gather_ram_valid.get)(wf_cu_reg)(wf_wgslot_reg) := !wf_gather_finish
    }
  }

  // =
  // Main function 5 (FSM action 2): generate dealloc request to ResourceTable for newly finished WG
  // =

  val rt_dealloc_ok = { val reg = RegInit(false.B)
    reg := Mux(fsm === FSM.UPDATE, io.rt_dealloc.fire, reg || io.rt_dealloc.fire)
    reg
  }
  io.rt_dealloc.valid := (fsm === FSM.UPDATE && wf_gather_finish) || (fsm === FSM.DEALLOC && !rt_dealloc_ok)
  io.rt_dealloc.bits.cu_id := wf_cu_reg
  io.rt_dealloc.bits.wg_slot_id := wf_wgslot_reg
  io.rt_dealloc.bits.num_wf := wf_gather_ram_read_data.num_wf
  io.rt_dealloc.bits.lds_dealloc_en := wf_gather_ram_read_data.lds_dealloc_en
  io.rt_dealloc.bits.sgpr_dealloc_en := wf_gather_ram_read_data.sgpr_dealloc_en
  io.rt_dealloc.bits.vgpr_dealloc_en := wf_gather_ram_read_data.vgpr_dealloc_en
  if(DEBUG) { io.rt_dealloc.bits.wg_id.get := wf_gather_ram_read_data.wg_id }

  // =
  // Main function 6 (FSM action 3): generate host_wg_done request for newly finished WG
  // =

  val host_wg_done_ok = { val reg = RegInit(false.B)
    reg := Mux(fsm === FSM.UPDATE, io.host_wg_done.fire, reg || io.host_wg_done.fire)
    reg
  }
  io.host_wg_done.valid := (fsm === FSM.UPDATE && wf_gather_finish) || (fsm === FSM.DEALLOC && !host_wg_done_ok)
  io.host_wg_done.bits.wg_id := wf_gather_ram_read_data.wg_id
  io.host_wg_done.bits.cu_id := wf_cu_reg

  // =
  // FSM state transition logic
  // =

  fsm := MuxLookup(fsm.asUInt, FSM.GET_WF)(Seq(
    FSM.GET_WF.asUInt -> Mux(arb_inst.io.out.fire, FSM.UPDATE, fsm),
    FSM.UPDATE.asUInt -> MuxCase(FSM.DEALLOC, Seq(
      !wf_gather_finish -> FSM.GET_WF,
      (io.rt_dealloc.fire && io.host_wg_done.fire) -> FSM.GET_WF,
    )),
    FSM.DEALLOC.asUInt -> Mux(rt_dealloc_ok && host_wg_done_ok, FSM.GET_WF, fsm),
  ))
}
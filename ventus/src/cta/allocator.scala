package cta

/**
 * @see docs/cta_schedulers/Allocator.md
 */

import chisel3._
import chisel3.experimental.BundleLiterals.AddBundleLiteralConstructor
import chisel3.experimental.VecLiterals.{AddObjectLiteralConstructor, AddVecLiteralConstructor}
import chisel3.util._

// =
// Abbreviations:
// rt = resource table
// =

class io_alloc2cuinterface extends Bundle with ctainfo_alloc_to_cuinterface {
  val wg_id: Option[UInt] = if(CONFIG.DEBUG) Some(UInt(CONFIG.WG.WG_ID_WIDTH)) else None
}
class io_rt2cuinterface extends Bundle with ctainfo_alloc_to_cu {
  val wg_id: Option[UInt] = if(CONFIG.DEBUG) Some(UInt(CONFIG.WG.WG_ID_WIDTH)) else None
}

trait datatype_rtcache_trait extends Bundle {
  def NUM_RESOURCE: Int
  def NUM_RT_RESULT: Int = CONFIG.RESOURCE_TABLE.NUM_RESULT
  val size = Vec(NUM_RT_RESULT, UInt(log2Ceil(NUM_RESOURCE+1).W))
}
class datatype_rtcache(val NUM_RESOURCE: Int, NUM_RT_RESULT: Int = CONFIG.RESOURCE_TABLE.NUM_RESULT) extends datatype_rtcache_trait

class io_rt2cache(val NUM_RESOURCE: Int, NUM_RT_RESULT: Int = CONFIG.RESOURCE_TABLE.NUM_RESULT) extends datatype_rtcache_trait {
  val cu_id = UInt(log2Ceil(CONFIG.GPU.NUM_CU).W)
}

class io_alloc2rt extends Bundle {
  val cu_id = UInt(log2Ceil(CONFIG.GPU.NUM_CU).W)
  val wg_slot_id = UInt(log2Ceil(CONFIG.GPU.NUM_WG_SLOT).W)
  val num_lds = UInt(log2Ceil(CONFIG.WG.NUM_LDS_MAX+1).W)
  val num_sgpr = UInt(log2Ceil(CONFIG.WG.NUM_SGPR_MAX+1).W)
  val num_vgpr = UInt(log2Ceil(CONFIG.WG.NUM_VGPR_MAX+1).W)
  val wg_id: Option[UInt] = if(CONFIG.DEBUG) Some(UInt(CONFIG.WG.WG_ID_WIDTH)) else None
}

// IO from Resource table to Allocator, dealloc request for WG/WF slot
class io_rt2dealloc extends Bundle with ctainfo_alloc_to_cuinterface

/**
 * Resource table cache writer in allocator
 * At posedge(alloc_en), the allocation result is updated to rtcache of `alloc_cuid`.
 * When alloc_en == true, the rtcache of `alloc_cuid` is locked and cannot be written with a new rt_result.
 * At other moments, rt_result is accepted and written to corresponding CU rtcache
 * @see the main state machine of allocator
 * @see doc/allocator.md
 *
 * @param NUM_RESOURCE: total amount of the hardware resource managed by this module
 * @param NUM_RT_RESULT: number of the results given by resource table, also the number of rtcache entries
 * @note This module is currently implemented as almost-pure combinational logic
 * @todo Check if the combinational logic path is too long
 */
class rtcache_writer(NUM_RESOURCE: Int, NUM_RT_RESULT: Int = CONFIG.RESOURCE_TABLE.NUM_RESULT) extends Module {
  val io  = IO(new Bundle {
    val rt_result = Flipped(DecoupledIO(new io_rt2cache(NUM_RESOURCE, NUM_RT_RESULT = NUM_RT_RESULT)))
    val alloc_en = Input(Bool())
    val alloc_sel = Input(UInt(log2Ceil(NUM_RT_RESULT).W))
    val alloc_cuid = Input(UInt(log2Ceil(CONFIG.GPU.NUM_CU).W))
    val alloc_size = Input(UInt(log2Ceil(NUM_RESOURCE+1).W))
    val alloc_rawdata = Input(new datatype_rtcache(NUM_RESOURCE, NUM_RT_RESULT = NUM_RT_RESULT))
    val rtcache_wr_en = Output(Bool())
    val rtcache_wr_cuid = Output(UInt(log2Ceil(CONFIG.GPU.NUM_CU).W))
    val rtcache_wr_data = Output(new datatype_rtcache(NUM_RESOURCE, NUM_RT_RESULT = NUM_RT_RESULT))
  })

  val alloc_en_r1 = RegNext(io.alloc_en)
  val alloc_wr = !alloc_en_r1 && io.alloc_en  // posedge(io.alloc_en)

  // if the allocator is not in STATE_ALLOC, it is ok to get a rt_result, else:
  // - if this module is busy with a new allocation action, it is not ready to get a rt_result
  // - if the CU of rt_result is in STATE_ALLOC, it is not ready to get this rt_result
  io.rt_result.ready := !io.alloc_en || (!alloc_wr && (io.rt_result.bits.cu_id =/= io.alloc_cuid))

  // =
  // Main function: rtcache_wr = Mux(cond, alloc, rt_result)
  // =

  io.rtcache_wr_en := io.rt_result.fire || alloc_wr
  when(alloc_wr) {
    io.rtcache_wr_cuid := io.alloc_cuid
    for(i <- 0 until NUM_RT_RESULT) {
      io.rtcache_wr_data.size(i) := io.alloc_rawdata.size(i) - Mux(i.U === io.alloc_sel, io.alloc_size, 0.U)
      assert(io.alloc_rawdata.size(io.alloc_sel) >= io.alloc_size)
    }
  } .elsewhen(io.rt_result.fire) {
    io.rtcache_wr_data.size := io.rt_result.bits.size
    io.rtcache_wr_cuid := io.rt_result.bits.cu_id
  } .otherwise {
    io.rtcache_wr_data.size := DontCare
    io.rtcache_wr_cuid := DontCare
  }
}

class allocator extends Module {
  // Constants used in IO
  val NUM_LDS = CONFIG.WG.NUM_LDS_MAX
  val NUM_SGPR = CONFIG.WG.NUM_SGPR_MAX
  val NUM_VGPR = CONFIG.WG.NUM_VGPR_MAX
  // IO
  val io = IO(new Bundle {
    val wgbuffer_wg_new = Flipped(DecoupledIO(new io_buffer2alloc))   // get new WG from WG buffer
    val wgbuffer_result = DecoupledIO(new io_alloc2buffer)            // allocation result to WG buffer, wgram2 read request
    val cuinterface_wg_new = DecoupledIO(new io_alloc2cuinterface)    // allocation result, WG info to CU interface
    val rt_alloc = DecoupledIO(new io_alloc2rt)                       // allocation result, alloc request to Resource table
    val rt_dealloc = Flipped(DecoupledIO(new io_rt2dealloc))          // dealloc request for WG/WF slot from Resource table
    val rt_result_lds  = Flipped(DecoupledIO(new io_rt2cache(NUM_LDS )))    // rtcache update from Resource table
    val rt_result_sgpr = Flipped(DecoupledIO(new io_rt2cache(NUM_SGPR)))
    val rt_result_vgpr = Flipped(DecoupledIO(new io_rt2cache(NUM_VGPR)))
  })

  // =
  // Constants
  // =
  val NUM_CU = CONFIG.GPU.NUM_CU
  val NUM_RT_RESULT = CONFIG.RESOURCE_TABLE.NUM_RESULT
  val RESOURCE_CHECK_CU_STEP = 2  // how many CU is check in one clock cycle (FSM.RESOURCE_CHECK)
  assert(NUM_CU % RESOURCE_CHECK_CU_STEP == 0)

  // =
  // Auxiliary function 1: resource table cache & its writer
  // =

  val rtcache_lds = RegInit(
    VecInit.fill(NUM_CU)((new datatype_rtcache(NUM_RESOURCE = CONFIG.WG.NUM_LDS_MAX , NUM_RT_RESULT = NUM_RT_RESULT)).Lit(
      c => c.size -> Vec.Lit((CONFIG.WG.NUM_LDS_MAX.U +: Seq.fill(NUM_RT_RESULT-1)(0.U(log2Ceil(CONFIG.WG.NUM_LDS_MAX+1).W))):_*),
    ))
  )
  val rtcache_sgpr = RegInit(
    VecInit.fill(NUM_CU)(new datatype_rtcache(NUM_RESOURCE = CONFIG.WG.NUM_SGPR_MAX, NUM_RT_RESULT = NUM_RT_RESULT).Lit(
      c => c.size -> Vec.Lit((CONFIG.WG.NUM_SGPR_MAX.U +: Seq.fill(NUM_RT_RESULT-1)(0.U(log2Ceil(CONFIG.WG.NUM_SGPR_MAX+1).W))):_*),
    ))
  )
  val rtcache_vgpr = RegInit(
    VecInit.fill(NUM_CU)(new datatype_rtcache(NUM_RESOURCE = CONFIG.WG.NUM_VGPR_MAX, NUM_RT_RESULT = NUM_RT_RESULT).Lit(
      c => c.size -> Vec.Lit((CONFIG.WG.NUM_VGPR_MAX.U +: Seq.fill(NUM_RT_RESULT-1)(0.U(log2Ceil(CONFIG.WG.NUM_VGPR_MAX+1).W))):_*),
    ))
  )

  val writer_lds = Module(new rtcache_writer(NUM_RESOURCE = CONFIG.WG.NUM_LDS_MAX, NUM_RT_RESULT = NUM_RT_RESULT))
  val writer_sgpr = Module(new rtcache_writer(NUM_RESOURCE = CONFIG.WG.NUM_SGPR_MAX, NUM_RT_RESULT = NUM_RT_RESULT))
  val writer_vgpr = Module(new rtcache_writer(NUM_RESOURCE = CONFIG.WG.NUM_VGPR_MAX, NUM_RT_RESULT = NUM_RT_RESULT))
  when(writer_lds.io.rtcache_wr_en)  { rtcache_lds(writer_lds.io.rtcache_wr_cuid)   := writer_lds.io.rtcache_wr_data  }
  when(writer_sgpr.io.rtcache_wr_en) { rtcache_sgpr(writer_sgpr.io.rtcache_wr_cuid) := writer_sgpr.io.rtcache_wr_data }
  when(writer_vgpr.io.rtcache_wr_en) { rtcache_vgpr(writer_vgpr.io.rtcache_wr_cuid) := writer_vgpr.io.rtcache_wr_data }
  writer_lds.io.rt_result  <> io.rt_result_lds
  writer_sgpr.io.rt_result <> io.rt_result_sgpr
  writer_vgpr.io.rt_result <> io.rt_result_vgpr

  // =
  // Auxiliary function 2: WG slot & WF slot recorder
  // =

  // resource table of WG/WF slot
  val wgslot = RegInit(VecInit.fill(NUM_CU)(0.U(CONFIG.GPU.NUM_WG_SLOT.W)))
  val wfslot = RegInit(VecInit.fill(NUM_CU)(0.U(log2Ceil(CONFIG.GPU.NUM_WF_SLOT+1).W)))

  val wgslot_id = Reg(UInt(log2Ceil(CONFIG.GPU.NUM_WG_SLOT).W)) // generated WG slot ID
  val wgslot_id_1H = Reg(UInt(CONFIG.GPU.NUM_WG_SLOT.W)) // generated WG slot ID, 1-hot encoded

  // =
  // Main FSM - define
  // =

  object FSM extends ChiselEnum {
    val IDLE, CU_PREFER, RESOURCE_CHECK, ALLOC, REJECT= Value
  }
  val fsm_next = Wire(FSM())
  val fsm = RegNext(fsm_next, FSM.IDLE)
  val fsm_r1 = RegNext(fsm, FSM.IDLE)

  // =
  // Main FSM - actions
  // =

  // used in sub-fsm RESOURCE_CHECK, presenting the resource check result in this cycle
  val resource_check_result = Wire(Vec(RESOURCE_CHECK_CU_STEP, Bool()))

  // IDLE -> CU_PREFER: get WG from wg_buffer
  val wg = RegEnable(io.wgbuffer_wg_new.bits, io.wgbuffer_wg_new.fire)
  io.wgbuffer_wg_new.ready := WireInit(fsm === FSM.IDLE)

  // used in sub-fsm RESOURCE_CHECK, counting how many CU have already been checked
  val cu_cnt = RegInit(0.U(log2Ceil(NUM_CU + RESOURCE_CHECK_CU_STEP).W))
  switch(fsm) {
    is(FSM.CU_PREFER) {
      // CU_PREFER -> RESOURCE_CHECK: clear cu_cnt
      cu_cnt := Mux(fsm_next === FSM.RESOURCE_CHECK, 0.U, DontCare)
    }
    is(FSM.RESOURCE_CHECK) {
      // RESOURCE_CHECK: cu_cnt += step
      cu_cnt := cu_cnt + RESOURCE_CHECK_CU_STEP.U
    }
  }

  // used in sub-fsm CU_PREFER, presenting the most preferred CU of this WG
  // used in sub-fsm RESOURCE_CHECK, presenting the CU that being checked this cycle
  //                                 after this sub-fsm exit, it becomes the CU that finally being selected
  val cu = RegInit((NUM_CU-1).U(log2Ceil(NUM_CU).W))
  val cu_next = Wire(UInt(log2Ceil(NUM_CU + RESOURCE_CHECK_CU_STEP).W))

  // used in sub-fsm RESOURCE_CHECK, if rtram of currently-being-checked CU is updated, in which case we should check again
  def cu_check(cu_updated: UInt, cu_now: UInt, cu_step: UInt): Bool = {
    val cu_begin = WireInit(UInt(log2Ceil(NUM_CU).W), cu_now)
    val cu_end_raw = cu_now + cu_step - 1.U
    val cu_end = WireInit(UInt(log2Ceil(NUM_CU).W), Mux(cu_end_raw >= NUM_CU.U, cu_end_raw - NUM_CU.U, cu_end_raw)) // Round
    Mux(cu_end > cu_begin, cu_updated >= cu_begin && cu_updated <= cu_end, cu_updated <= cu_end || cu_updated >= cu_begin)
  }
  val resource_check_repeat =
    (io.rt_result_lds.fire  && cu_check(io.rt_result_lds.bits.cu_id , cu, RESOURCE_CHECK_CU_STEP.U)) ||
    (io.rt_result_sgpr.fire && cu_check(io.rt_result_sgpr.bits.cu_id, cu, RESOURCE_CHECK_CU_STEP.U)) ||
    (io.rt_result_vgpr.fire && cu_check(io.rt_result_vgpr.bits.cu_id, cu, RESOURCE_CHECK_CU_STEP.U))

  cu_next := cu // switch default
  switch(fsm) {
    is(FSM.CU_PREFER) {
      // FSM.CU_PREFER: generate preferred CU ID. Currently we prefer CU after the last allocated CU
      cu_next := cu + 1.U
    }
    is(FSM.RESOURCE_CHECK) {
      when(fsm_next === FSM.ALLOC) {
        // RESOURCE_CHECK -> ALLOC:  generated selected CU ID
        cu_next := cu + PriorityEncoder(resource_check_result.asUInt)
      } .otherwise {
        // RESOURCE_CHECK -> REJECT: DontCare
        // RESOURCE_CHECK: if(cache_updated) {do nothing} else {CU step}
        cu_next := Mux(resource_check_repeat, cu, cu + RESOURCE_CHECK_CU_STEP.U)
      }
    }
  }
  val cu_next_rounded = WireInit(UInt(log2Ceil(NUM_CU).W), Mux(cu_next >= NUM_CU.U, cu_next - NUM_CU.U, cu_next)) // Round
  cu := cu_next_rounded

  // resource check result generating, combinational logic
  val resource_check_result_rtcache_sel_lds  = Wire(Vec(RESOURCE_CHECK_CU_STEP, UInt(log2Ceil(NUM_RT_RESULT).W)))
  val resource_check_result_rtcache_sel_sgpr = Wire(Vec(RESOURCE_CHECK_CU_STEP, UInt(log2Ceil(NUM_RT_RESULT).W)))
  val resource_check_result_rtcache_sel_vgpr = Wire(Vec(RESOURCE_CHECK_CU_STEP, UInt(log2Ceil(NUM_RT_RESULT).W)))
  for(i <- 0 until RESOURCE_CHECK_CU_STEP) {
    val cuid = Mux(cu + i.U >= NUM_CU.U, cu + i.U - NUM_CU.U, cu + i.U)
    val result_line_lds  = Wire(Vec(NUM_RT_RESULT, Bool()))
    val result_line_sgpr = Wire(Vec(NUM_RT_RESULT, Bool()))
    val result_line_vgpr = Wire(Vec(NUM_RT_RESULT, Bool()))
    for(j <- 0 until NUM_RT_RESULT) { // higher rt cache line has higher priority
      result_line_lds(j)  := rtcache_lds(cuid).size(j)  >= wg.num_lds
      result_line_sgpr(j) := rtcache_sgpr(cuid).size(j) >= wg.num_sgpr
      result_line_vgpr(j) := rtcache_vgpr(cuid).size(j) >= wg.num_vgpr
    }

    // Priority Encoder, MSB has the highest priority
    resource_check_result_rtcache_sel_lds(i)  := PriorityMux(result_line_lds.reverse , (NUM_RT_RESULT-1 to 0 by -1).map(_.asUInt))
    resource_check_result_rtcache_sel_sgpr(i) := PriorityMux(result_line_sgpr.reverse, (NUM_RT_RESULT-1 to 0 by -1).map(_.asUInt))
    resource_check_result_rtcache_sel_vgpr(i) := PriorityMux(result_line_vgpr.reverse, (NUM_RT_RESULT-1 to 0 by -1).map(_.asUInt))

    resource_check_result(i) := result_line_lds.asUInt.orR && result_line_sgpr.asUInt.orR && result_line_vgpr.asUInt.orR &&
                               (!wgslot(cuid).andR) && (CONFIG.GPU.NUM_WF_SLOT.U - wfslot(cuid) >= wg.num_wf)
  }

  // wg slot id generation
  // RESOURCE_CHECK: DontCare
  // RESOURCE_CHECK -> REJECT: DontCare
  // RESOURCE_CHECK -> ALLOC : This WG's slot ID in the selected CU
  when(fsm === FSM.RESOURCE_CHECK) {
    wgslot_id := PriorityEncoder(~wgslot(cu_next_rounded))
    wgslot_id_1H := PriorityEncoderOH(~wgslot(cu_next_rounded))
  }

  // resource table cache line select
  // RESOURCE_CHECK: DontCare
  // RESOURCE_CHECK -> REJECT: DontCare
  // RESOURCE_CHECK -> ALLOC : The resource table cache line selection results for three kinds of resources
  val rtcache_lds_sel  = RegEnable(resource_check_result_rtcache_sel_lds(PriorityEncoder(resource_check_result.asUInt)) , fsm === FSM.RESOURCE_CHECK)
  val rtcache_sgpr_sel = RegEnable(resource_check_result_rtcache_sel_sgpr(PriorityEncoder(resource_check_result.asUInt)), fsm === FSM.RESOURCE_CHECK)
  val rtcache_vgpr_sel = RegEnable(resource_check_result_rtcache_sel_vgpr(PriorityEncoder(resource_check_result.asUInt)), fsm === FSM.RESOURCE_CHECK)

  // ALLOC tasks
  val alloc_task_rt = Wire(Bool())          // send alloc request to Resource table
  val alloc_task_wgram2 = Wire(Bool())      // send wgram read request to wgbuffer
  val alloc_task_cuinterface = Wire(Bool()) // send newly-allocated WG info to CU interface
  val alloc_task_ok = alloc_task_rt && alloc_task_wgram2 && alloc_task_cuinterface

  // ALLOC task0: wgslot & wfslot update, always finishes in the first cycle of FSM.ALLOC
  io.rt_dealloc.ready := !(fsm === FSM.ALLOC && fsm =/= fsm_r1) || (io.rt_dealloc.bits.cu_id === cu)
  val wgslot_alloc_bitmask = Mux(fsm === FSM.ALLOC && fsm =/= fsm_r1, wgslot_id_1H, 0.U)
  val wgslot_dealloc_bitmask = Mux(io.rt_dealloc.fire, UIntToOH(io.rt_dealloc.bits.wg_slot_id), 0.U)
  val wfslot_alloc_num = Mux(fsm === FSM.ALLOC && fsm =/= fsm_r1, wg.num_wf, 0.U)
  val wfslot_dealloc_num = Mux(io.rt_dealloc.fire, io.rt_dealloc.bits.num_wf, 0.U)
  val cu_tmp = Mux(fsm === FSM.ALLOC && fsm =/= fsm_r1, cu, io.rt_dealloc.bits.cu_id)
  wgslot(cu_tmp) := wgslot(cu_tmp) & (~wgslot_dealloc_bitmask).asUInt | wgslot_alloc_bitmask
  wfslot(cu_tmp) := wfslot(cu_tmp) + wfslot_alloc_num - wfslot_dealloc_num

  // ALLOC task1: rtcache update, always finishes in the first cycle, then waiting for FSM.ALLOC ends
  writer_lds.io.alloc_en := (fsm === FSM.ALLOC)
  writer_lds.io.alloc_rawdata := rtcache_lds(cu)
  writer_lds.io.alloc_size := wg.num_lds
  writer_lds.io.alloc_cuid := cu
  writer_lds.io.alloc_sel := rtcache_lds_sel
  writer_sgpr.io.alloc_en := (fsm === FSM.ALLOC)
  writer_sgpr.io.alloc_rawdata := rtcache_sgpr(cu)
  writer_sgpr.io.alloc_size := wg.num_sgpr
  writer_sgpr.io.alloc_cuid := cu
  writer_sgpr.io.alloc_sel := rtcache_sgpr_sel
  writer_vgpr.io.alloc_en := (fsm === FSM.ALLOC)
  writer_vgpr.io.alloc_rawdata := rtcache_vgpr(cu)
  writer_vgpr.io.alloc_size := wg.num_vgpr
  writer_vgpr.io.alloc_cuid := cu
  writer_vgpr.io.alloc_sel := rtcache_vgpr_sel

  // ALLOC task2: resource table allocation request
  val alloc_task_rt_reg = RegInit(false.B)
  io.rt_alloc.valid := (fsm === FSM.ALLOC) && !alloc_task_rt_reg
  io.rt_alloc.bits.cu_id := cu
  io.rt_alloc.bits.wg_slot_id := wgslot_id
  io.rt_alloc.bits.num_lds := wg.num_lds
  io.rt_alloc.bits.num_sgpr := wg.num_sgpr
  io.rt_alloc.bits.num_vgpr := wg.num_vgpr
  if(CONFIG.DEBUG) { io.rt_alloc.bits.wg_id.get := wg.wg_id.get }
  alloc_task_rt_reg := Mux(fsm === FSM.ALLOC, alloc_task_rt_reg || (io.rt_alloc.fire && !alloc_task_ok), false.B)
  alloc_task_rt := alloc_task_rt_reg || io.rt_alloc.fire

  // ALLOC task 3: send wg info to the next pipeline stage, which will finally send it to CU interface
  val alloc_task_cuinterface_reg = RegInit(false.B)
  val cuinterface_buf = Module(new Queue(new io_alloc2cuinterface, entries=1, pipe=true))
  cuinterface_buf.io.enq.valid := (fsm === FSM.ALLOC) && !alloc_task_cuinterface_reg
  cuinterface_buf.io.enq.bits.cu_id := cu
  cuinterface_buf.io.enq.bits.wg_slot_id := wgslot_id
  cuinterface_buf.io.enq.bits.num_wf := wg.num_wf
  cuinterface_buf.io.enq.bits.lds_dealloc_en := (wg.num_lds =/= 0.U)
  cuinterface_buf.io.enq.bits.sgpr_dealloc_en := (wg.num_sgpr =/= 0.U)
  cuinterface_buf.io.enq.bits.vgpr_dealloc_en := (wg.num_vgpr =/= 0.U)
  if(CONFIG.DEBUG) {cuinterface_buf.io.enq.bits.wg_id.get := wg.wg_id.get}
  alloc_task_cuinterface_reg := Mux(fsm === FSM.ALLOC, alloc_task_cuinterface_reg || (cuinterface_buf.io.enq.fire && !alloc_task_ok), false.B )
  alloc_task_cuinterface := alloc_task_cuinterface_reg || cuinterface_buf.io.enq.fire
  io.cuinterface_wg_new <> cuinterface_buf.io.deq

  // ALLOC task 4: wgram2 read request
  // REJECT task
  val alloc_task_wgram2_reg = RegInit(false.B)
  io.wgbuffer_result.valid := (fsm === FSM.ALLOC || fsm === FSM.REJECT) && !alloc_task_wgram2_reg
  io.wgbuffer_result.bits.accept := (fsm === FSM.ALLOC)
  io.wgbuffer_result.bits.wgram_addr := wg.wgram_addr
  if(CONFIG.DEBUG) { io.wgbuffer_result.bits.wg_id.get :=  wg.wg_id.get }
  alloc_task_wgram2_reg := MuxLookup(fsm.asUInt, false.B)(Seq(
    FSM.ALLOC.asUInt  -> (alloc_task_wgram2_reg || (io.wgbuffer_result.fire && !alloc_task_ok)),
    FSM.REJECT.asUInt -> (alloc_task_wgram2_reg || (io.wgbuffer_result.fire && !alloc_task_ok)),
  ))
  alloc_task_wgram2 := alloc_task_wgram2_reg || io.wgbuffer_result.fire

  // =
  // Main FSM - state transition
  // =

  // fsm next state logic, combinational
  fsm_next := FSM.IDLE  // default
  switch(fsm) {
    is(FSM.IDLE) {
      fsm_next := Mux(io.wgbuffer_wg_new.fire, FSM.CU_PREFER, fsm)
    }
    is(FSM.CU_PREFER) {
      fsm_next := FSM.RESOURCE_CHECK
    }
    is(FSM.RESOURCE_CHECK) {
      fsm_next := MuxCase(fsm, Seq(
        resource_check_repeat -> fsm,
        resource_check_result.asUInt.orR -> FSM.ALLOC,
        (cu_cnt + RESOURCE_CHECK_CU_STEP.U >= NUM_CU.U) -> FSM.REJECT,
      ))
    }
    is(FSM.REJECT) {
      fsm_next := Mux(io.wgbuffer_result.fire, FSM.IDLE, fsm)
    }
    is(FSM.ALLOC) {
      fsm_next := Mux(alloc_task_ok, FSM.IDLE, fsm)
    }
  }

}
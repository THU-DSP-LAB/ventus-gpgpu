package cta

import chisel3._
import chisel3.util._
import chisel3.experimental.dataview._
import pipeline.ArrayMulDataModule
import top.parameters.{CTA_SCHE_CONFIG => CONFIG}

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
  val NUM_PDS  = CONFIG.WG.NUM_PDS_MAX
  val NUM_THREAD_HW = CONFIG.GPU.NUM_THREAD
  val DEBUG = CONFIG.DEBUG
  class wftag_datatype extends Bundle {
    val wg_slot_id = UInt(log2Ceil(NUM_WG_SLOT).W)
    val wf_id = UInt(log2Ceil(NUM_WF_MAX).W)
  }

  // =
  // Main function 1: gather WG info from Allocator, Resource table and WGram2
  // =

  class cta_data extends Bundle with ctainfo_alloc_to_cuinterface with ctainfo_alloc_to_cu with ctainfo_host_to_cuinterface with ctainfo_host_to_cu with ctainfo_host_to_alloc_to_cu {
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
  fifo.io.enq.bits.viewAsSupertype(new ctainfo_host_to_alloc_to_cu {}) := io.alloc_wg_new.bits
  fifo.io.enq.bits.viewAsSupertype(new ctainfo_alloc_to_cu {}) := io.rt_wg_new.bits

  // =
  // Main function 2: split WG into WF
  // =
  val wf_sent = io.cu_wf_new(fifo.io.deq.bits.cu_id).fire

  // the value of splitter_cnt means how many WF is waiting for being sent to CU
  // splitter_cnt==0 means no WF is waiting to be sent, we are waiting for the next WG
  val splitter_cnt = RegInit(0.U(log2Ceil(NUM_WF_MAX + 1).W)) // how many WF not dispatched in this WG
  val splitter_lds_addr = WireInit(fifo.io.deq.bits.lds_base)
  val splitter_sgpr_addr = Reg(UInt(log2Ceil(NUM_SGPR).W)) // sgpr base of WF, its value steps num_sgpr_per_wf every time
  val splitter_vgpr_addr = Reg(UInt(log2Ceil(NUM_VGPR).W)) // vgpr base of WF, its value steps num_vgpr_per_wf every time
  val splitter_pds_addr  = Reg(UInt(log2Ceil(NUM_PDS ).W)) // pds  base of WF, its value steps num_pds_per_wf  every time
  val splitter_load_new = (splitter_cnt === 0.U) && fifo.io.deq.valid   // A new WG will be loaded to splitter
  fifo.io.deq.ready := (splitter_cnt === 1.U) && wf_sent
  assert(splitter_cnt === 0.U || fifo.io.deq.valid)
  if(DEBUG) { // It's required that since fifo.deq.valid, fifo.deq will not change until fire
    val fifo_deq = Reg(fifo.io.deq.bits.cloneType)
    val fifo_deq_no_data = RegInit(true.B)
    fifo_deq_no_data := MuxCase(fifo_deq_no_data, Seq(
      fifo.io.deq.fire  -> true.B,
      fifo.io.deq.valid -> false.B,
    ))
    when(fifo_deq_no_data && fifo.io.deq.valid) { fifo_deq := fifo.io.deq.bits }
    when(!fifo_deq_no_data) {
      assert(fifo.io.deq.valid)
      assert(fifo.io.deq.bits === fifo_deq)
    }
  }
  // to be continued

  //
  // Function 2.1: ThreadIdx-Local (in WG) generation, 3D and linear
  //
  import cta.utils.cnt_varRadix_multiStep
  import scala.math.{max,min}
  val THREADIDX_STEP = 2   // the counter will generate threadIndex-Local for $STEP threads each cycle
  val THREADIDX_CTRL_EXTRA = min(CONFIG.GPU.NUM_THREAD, 3*THREADIDX_STEP)
  val THREADIDX_CTRL_MAX = NUM_THREAD_HW + THREADIDX_CTRL_EXTRA

  // instantiate
  val threadIdxL_cnt3d = Module(new cnt_varRadix_multiStep(N=3, WIDTH=log2Ceil(CONFIG.WG.NUM_THREAD_PER_WG_MAX), STEP=THREADIDX_STEP))
  val threadIdxL_cnt1d = Reg(UInt(log2Ceil(CONFIG.WG.NUM_THREAD_PER_WG_MAX).W))
  val threadIdxL_result_x  = Reg(Vec(THREADIDX_CTRL_MAX, UInt(log2Ceil(CONFIG.WG.NUM_THREAD_PER_WG_MAX).W)))
  val threadIdxL_result_y  = Reg(Vec(THREADIDX_CTRL_MAX, UInt(log2Ceil(CONFIG.WG.NUM_THREAD_PER_WG_MAX).W)))
  val threadIdxL_result_z  = Reg(Vec(THREADIDX_CTRL_MAX, UInt(log2Ceil(CONFIG.WG.NUM_THREAD_PER_WG_MAX).W)))
  val threadIdxL_result_1d = Reg(Vec(THREADIDX_CTRL_MAX, UInt(log2Ceil(CONFIG.WG.NUM_THREAD_PER_WG_MAX).W)))

  // control logic
  val threadIdxL_ctrl_cnt = Reg(UInt(log2Ceil(THREADIDX_CTRL_MAX + THREADIDX_STEP).W))
  val threadIdxL_ctrl_ok = threadIdxL_ctrl_cnt >= CONFIG.GPU.NUM_THREAD.U
  val threadIdxL_ctrl_full = threadIdxL_ctrl_cnt === THREADIDX_CTRL_MAX.U
  threadIdxL_ctrl_cnt := Mux(splitter_cnt === 0.U, 0.U,
                             threadIdxL_ctrl_cnt - Mux(wf_sent, NUM_THREAD_HW.U, 0.U) + Mux(!threadIdxL_ctrl_full, THREADIDX_STEP.U, 0.U) )
  assert(!wf_sent || threadIdxL_ctrl_cnt >= NUM_THREAD_HW.U)

  // cnt3d clear
  threadIdxL_cnt3d.io.radix.bits(0) := fifo.io.deq.bits.num_thread_per_wg_x
  threadIdxL_cnt3d.io.radix.bits(1) := fifo.io.deq.bits.num_thread_per_wg_y
  threadIdxL_cnt3d.io.radix.bits(2) := fifo.io.deq.bits.num_thread_per_wg_z
  threadIdxL_cnt3d.io.radix.valid := splitter_load_new
  // cnt3d enable
  val threadIdxL_cnt_enable = (splitter_cnt =/= 0.U) && !threadIdxL_ctrl_full // Still at least one WF remain to be dispatch && this WF unfinish
  threadIdxL_cnt3d.io.cnt.ready := threadIdxL_cnt_enable
  // cnt1d clear & enable
  threadIdxL_cnt1d := MuxCase(threadIdxL_cnt1d, Seq(
    (splitter_load_new)     -> 0.U,
    (threadIdxL_cnt_enable) -> (threadIdxL_cnt1d + THREADIDX_STEP.U),
  ))

  for(i <- 0 until THREADIDX_CTRL_MAX) {
    if(i < THREADIDX_CTRL_EXTRA) {
      when(wf_sent) {
        threadIdxL_result_x(i)  := threadIdxL_result_x(i + NUM_THREAD_HW)
        threadIdxL_result_y(i)  := threadIdxL_result_y(i + NUM_THREAD_HW)
        threadIdxL_result_z(i)  := threadIdxL_result_z(i + NUM_THREAD_HW)
        threadIdxL_result_1d(i) := threadIdxL_result_1d(i + NUM_THREAD_HW)
      }
    }
    val select_base = threadIdxL_ctrl_cnt - Mux(wf_sent, NUM_THREAD_HW.U, 0.U)
    val select_this = (i.U >= select_base && i.U < select_base + THREADIDX_STEP.U)
    when(threadIdxL_cnt_enable && select_this) {
      threadIdxL_result_x(i) := threadIdxL_cnt3d.io.cnt.bits(i.U-threadIdxL_ctrl_cnt)(0)
      threadIdxL_result_y(i) := threadIdxL_cnt3d.io.cnt.bits(i.U-threadIdxL_ctrl_cnt)(1)
      threadIdxL_result_z(i) := threadIdxL_cnt3d.io.cnt.bits(i.U-threadIdxL_ctrl_cnt)(2)
      threadIdxL_result_1d(i) := threadIdxL_cnt1d + (i.U - threadIdxL_ctrl_cnt)
    }
  }

  //
  // Function 2.2: ThreadIdx-Global (in WG) generation, 3D and linear
  //

  // the threadIdxG_1d calculator has 2-cycles latency, with combinational output
  // control logic
  val threadIdxG_ctrl_input_cnt = Reg(UInt(log2Ceil(THREADIDX_CTRL_MAX + THREADIDX_STEP).W))
  val threadIdxG_ctrl_output_cnt = Reg(UInt(log2Ceil(THREADIDX_CTRL_MAX + THREADIDX_STEP).W))
  val threadIdxG_ctrl_input_en = (threadIdxG_ctrl_input_cnt =/= THREADIDX_CTRL_MAX.U) && (threadIdxG_ctrl_input_cnt < threadIdxL_ctrl_cnt)
  val threadIdxG_ctrl_output_en = RegNext(Mux(splitter_cnt =/= 0.U, RegNext(threadIdxG_ctrl_input_en), false.B))
  val threadIdxG_ctrl_ok = (threadIdxG_ctrl_input_cnt >= NUM_THREAD_HW.U) && (threadIdxG_ctrl_output_cnt >= NUM_THREAD_HW.U)
  threadIdxG_ctrl_input_cnt := Mux(splitter_cnt === 0.U, 0.U,
                                 threadIdxG_ctrl_input_cnt - Mux(wf_sent, NUM_THREAD_HW.U, 0.U) + Mux(threadIdxG_ctrl_input_en, THREADIDX_STEP.U, 0.U) )
  threadIdxG_ctrl_output_cnt := Mux(splitter_cnt === 0.U, 0.U,
                                 threadIdxG_ctrl_output_cnt - Mux(wf_sent, NUM_THREAD_HW.U, 0.U) + Mux(threadIdxG_ctrl_output_en, THREADIDX_STEP.U, 0.U) )

  // calculate 1: threadIdxG_base_{x,y,z} + threadIdxL_{x,y,z} = calc1_{x,y,z},     calc1_{x,y,z} + offset_{x,y,z} = threadIdxG_{x,y,z}
  // calculate 2: calc1_y * num_thread_per_wg_x = calc2_y,    calc1_z * num_thread_per_wg_{x * y} = calc2_z
  // calculate 3: calc1_x + calc2_y + calc2_z = threadIdxG_1d
  val threadIdxG_calc1_x = Wire(Vec(THREADIDX_STEP, UInt(log2Ceil(CONFIG.KERNEL.NUM_THREAD_PER_KNL_MAX).W)))
  val threadIdxG_calc1_y = Wire(Vec(THREADIDX_STEP, UInt(log2Ceil(CONFIG.KERNEL.NUM_THREAD_PER_KNL_MAX).W)))
  val threadIdxG_calc1_z = Wire(Vec(THREADIDX_STEP, UInt(log2Ceil(CONFIG.KERNEL.NUM_THREAD_PER_KNL_MAX).W)))
  val threadIdxG_calc2_x = RegNext(threadIdxG_calc1_x)
  val threadIdxG_calc2_y = Wire(Vec(THREADIDX_STEP, UInt(log2Ceil(CONFIG.KERNEL.NUM_THREAD_PER_KNL_MAX).W)))
  val threadIdxG_calc2_z = Wire(Vec(THREADIDX_STEP, UInt(log2Ceil(CONFIG.KERNEL.NUM_THREAD_PER_KNL_MAX).W)))
  val threadIdxG_calc3_1d = Wire(Vec(THREADIDX_STEP, UInt(log2Ceil(CONFIG.KERNEL.NUM_THREAD_PER_KNL_MAX).W)))
  val threadIdxG_calc2_mul_y = VecInit.fill(THREADIDX_STEP)(Module(new ArrayMulDataModule(log2Ceil(CONFIG.KERNEL.NUM_THREAD_PER_KNL_MAX) + 1)).io)
  val threadIdxG_calc2_mul_z = VecInit.fill(THREADIDX_STEP)(Module(new ArrayMulDataModule(log2Ceil(CONFIG.KERNEL.NUM_THREAD_PER_KNL_MAX) + 1)).io)
  for(i <- 0 until THREADIDX_STEP) {
    threadIdxG_calc1_x(i) := fifo.io.deq.bits.threadIdx_in_grid_base_x + threadIdxL_result_x(threadIdxG_ctrl_input_cnt + i.U)
    threadIdxG_calc1_y(i) := fifo.io.deq.bits.threadIdx_in_grid_base_y + threadIdxL_result_y(threadIdxG_ctrl_input_cnt + i.U)
    threadIdxG_calc1_z(i) := fifo.io.deq.bits.threadIdx_in_grid_base_z + threadIdxL_result_z(threadIdxG_ctrl_input_cnt + i.U)
    threadIdxG_calc2_mul_y(i).a := RegNext(threadIdxG_calc1_y(i))
    threadIdxG_calc2_mul_y(i).b := fifo.io.deq.bits.num_thread_per_grid_x
    threadIdxG_calc2_mul_z(i).a := RegNext(threadIdxG_calc1_z(i))
    threadIdxG_calc2_mul_z(i).b := fifo.io.deq.bits.num_thread_per_grid_xy
    threadIdxG_calc2_y(i) := threadIdxG_calc2_mul_y(i).result
    threadIdxG_calc2_z(i) := threadIdxG_calc2_mul_z(i).result
    threadIdxG_calc3_1d(i) := threadIdxG_calc2_x(i) + threadIdxG_calc2_y(i) + threadIdxG_calc2_z(i)

    threadIdxG_calc2_mul_y(i).regEnables(0) := true.B
    threadIdxG_calc2_mul_y(i).regEnables(1) := false.B // Check: what is this used for? Chisel optimize this io-port out
    threadIdxG_calc2_mul_z(i).regEnables(0) := true.B
    threadIdxG_calc2_mul_z(i).regEnables(1) := false.B // Check: what is this used for? Chisel optimize this io-port out
  }

  // calc result store
  val threadIdxG_result_x  = Reg(Vec(THREADIDX_CTRL_MAX, UInt(log2Ceil(CONFIG.KERNEL.NUM_THREAD_PER_KNL_MAX).W)))
  val threadIdxG_result_y  = Reg(Vec(THREADIDX_CTRL_MAX, UInt(log2Ceil(CONFIG.KERNEL.NUM_THREAD_PER_KNL_MAX).W)))
  val threadIdxG_result_z  = Reg(Vec(THREADIDX_CTRL_MAX, UInt(log2Ceil(CONFIG.KERNEL.NUM_THREAD_PER_KNL_MAX).W)))
  val threadIdxG_result_1d = Reg(Vec(THREADIDX_CTRL_MAX, UInt(log2Ceil(CONFIG.KERNEL.NUM_THREAD_PER_KNL_MAX).W)))
  for(i <- 0 until THREADIDX_CTRL_MAX) {
    if(i < THREADIDX_CTRL_EXTRA) {
      when(wf_sent) {
        threadIdxG_result_x(i) := threadIdxG_result_x(i + NUM_THREAD_HW)
        threadIdxG_result_y(i) := threadIdxG_result_y(i + NUM_THREAD_HW)
        threadIdxG_result_z(i) := threadIdxG_result_z(i + NUM_THREAD_HW)
        threadIdxG_result_1d(i) := threadIdxG_result_1d(i + NUM_THREAD_HW)
      }
    }
    val input_select_base  = threadIdxG_ctrl_input_cnt  - Mux(wf_sent, NUM_THREAD_HW.U, 0.U)
    val output_select_base = threadIdxG_ctrl_output_cnt - Mux(wf_sent, NUM_THREAD_HW.U, 0.U)
    val input_select_this  = (i.U >= threadIdxG_ctrl_input_cnt  && i.U < threadIdxG_ctrl_input_cnt  + THREADIDX_CTRL_EXTRA.U)
    val output_select_this = (i.U >= threadIdxG_ctrl_output_cnt && i.U < threadIdxG_ctrl_output_cnt + THREADIDX_CTRL_EXTRA.U)
    when(threadIdxG_ctrl_input_en && input_select_this) { // threadIdxG_3d calc is combinational, reuse ctrl_input_en
      threadIdxG_result_x(i) := threadIdxG_calc1_x(i.U - input_select_base) + fifo.io.deq.bits.threadIdx_in_grid_offset_x
      threadIdxG_result_y(i) := threadIdxG_calc1_y(i.U - input_select_base) + fifo.io.deq.bits.threadIdx_in_grid_offset_y
      threadIdxG_result_z(i) := threadIdxG_calc1_z(i.U - input_select_base) + fifo.io.deq.bits.threadIdx_in_grid_offset_z
    }
    when(threadIdxG_ctrl_output_en && output_select_this) {
      threadIdxG_result_1d(i) := threadIdxG_calc3_1d(i.U - output_select_base)
    }
  }

  //
  // back to main function 2: splitter main
  //
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
  splitter_pds_addr := MuxCase(splitter_pds_addr, Seq(
    splitter_load_new -> fifo.io.deq.bits.pds_base,
    io.cu_wf_new(fifo.io.deq.bits.cu_id).fire -> (splitter_pds_addr + fifo.io.deq.bits.num_pds_per_wf),
  ))
  assert(splitter_cnt <= 1.U || NUM_SGPR.U - splitter_sgpr_addr > fifo.io.deq.bits.num_sgpr_per_wf)
  assert(splitter_cnt <= 1.U || NUM_VGPR.U - splitter_vgpr_addr > fifo.io.deq.bits.num_vgpr_per_wf)

  for(i <- 0 until NUM_CU) {
    io.cu_wf_new(i).valid := (splitter_cnt =/= 0.U) && threadIdxL_ctrl_ok && threadIdxG_ctrl_ok && (fifo.io.deq.bits.cu_id === i.U)
    io.cu_wf_new(i).bits.viewAsSupertype(new ctainfo_host_to_cu {}) := fifo.io.deq.bits
    io.cu_wf_new(i).bits.viewAsSupertype(new ctainfo_host_to_alloc_to_cu {}) := fifo.io.deq.bits.viewAsSupertype(new ctainfo_host_to_alloc_to_cu {})
    io.cu_wf_new(i).bits.pds_base := splitter_pds_addr
    io.cu_wf_new(i).bits.lds_base := splitter_lds_addr
    io.cu_wf_new(i).bits.sgpr_base := splitter_sgpr_addr
    io.cu_wf_new(i).bits.vgpr_base := splitter_vgpr_addr
    io.cu_wf_new(i).bits.wg_id := fifo.io.deq.bits.wg_id
    io.cu_wf_new(i).bits.num_wf := fifo.io.deq.bits.num_wf
    io.cu_wf_new(i).bits.threadIdx_in_wg_x := threadIdxL_result_x.slice(0, NUM_THREAD_HW)
    io.cu_wf_new(i).bits.threadIdx_in_wg_y := threadIdxL_result_y.slice(0, NUM_THREAD_HW)
    io.cu_wf_new(i).bits.threadIdx_in_wg_z := threadIdxL_result_z.slice(0, NUM_THREAD_HW)
    io.cu_wf_new(i).bits.threadIdx_in_wg  := threadIdxL_result_1d.slice(0, NUM_THREAD_HW)
    io.cu_wf_new(i).bits.threadIdx_in_grid_x := threadIdxG_result_x.slice(0, NUM_THREAD_HW)
    io.cu_wf_new(i).bits.threadIdx_in_grid_y := threadIdxG_result_y.slice(0, NUM_THREAD_HW)
    io.cu_wf_new(i).bits.threadIdx_in_grid_z := threadIdxG_result_z.slice(0, NUM_THREAD_HW)
    io.cu_wf_new(i).bits.threadIdx_in_grid  := threadIdxG_result_1d.slice(0, NUM_THREAD_HW)
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
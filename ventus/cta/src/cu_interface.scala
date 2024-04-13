package cta_scheduler

import chisel3._
import chisel3.util._
import chisel3.experimental.dataview._

class cu_interface extends Module {
  val NUM_CU: Int = CONFIG.GPU.NUM_CU
  val io = IO(new Bundle{
    val wgbuffer_wg_new = Flipped(DecoupledIO(new io_buffer2cuinterface))
    val alloc_wg_new = Flipped(DecoupledIO(new io_alloc2cuinterface))
    val rt_wg_new = Flipped(DecoupledIO(new io_rt2cuinterface))
    val cu_wf_new = Vec(NUM_CU, DecoupledIO(new io_cuinterface2cu))
    val cu_wf_done = Vec(NUM_CU, Flipped(DecoupledIO(new io_cu2cuinterface)))
    val rt_dealloc = DecoupledIO(new io_cuinterface2rt)
    val host_wg_done = DecoupledIO(new io_cta2host)
  })
  val NUM_WG_SLOT = CONFIG.GPU.NUM_WG_SLOT
  val NUM_WF_MAX = CONFIG.WG.NUM_WF_MAX
  val DEBUG = CONFIG.DEBUG

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
  io.wgbuffer_wg_new.ready := io.alloc_wg_new.valid && io.rt_wg_new.valid && fifo.io.enq.ready
  io.alloc_wg_new.ready := io.wgbuffer_wg_new.valid && io.rt_wg_new.valid && fifo.io.enq.ready
  io.rt_wg_new.ready := io.wgbuffer_wg_new.valid && io.alloc_wg_new.valid && fifo.io.enq.ready
  fifo.io.enq.valid := io.wgbuffer_wg_new.valid && io.alloc_wg_new.valid && io.rt_wg_new.valid
  fifo.io.enq.bits.wg_id := io.wgbuffer_wg_new.bits.wg_id
  fifo.io.enq.bits.viewAsSupertype(new ctainfo_host_to_cuinterface {}) := io.wgbuffer_wg_new.bits
  fifo.io.enq.bits.viewAsSupertype(new ctainfo_host_to_cu {}) := io.wgbuffer_wg_new.bits
  fifo.io.enq.bits.viewAsSupertype(new ctainfo_alloc_to_cuinterface {}) := io.alloc_wg_new.bits
  fifo.io.enq.bits.viewAsSupertype(new ctainfo_alloc_to_cu {}) := io.rt_wg_new.bits

  fifo.io.deq.ready := io.cu_wf_new(fifo.io.deq.bits.cu_id).ready
  for(i <- 0 until NUM_CU) {
    io.cu_wf_new(i).valid := fifo.io.deq.valid && (fifo.io.deq.bits.cu_id === i.U)
    io.cu_wf_new(i).bits.viewAsSupertype(new ctainfo_host_to_cu {}) := fifo.io.deq.bits
    io.cu_wf_new(i).bits.viewAsSupertype(new ctainfo_alloc_to_cu {}) := fifo.io.deq.bits
    io.cu_wf_new(i).bits.wg_id := fifo.io.deq.bits.wg_id
    io.cu_wf_new(i).bits.wf_tag := Cat(fifo.io.deq.bits.wg_slot_id, 0.U(log2Ceil(NUM_WF_MAX).W))
  }

  class cta_data_1 extends Bundle {
    val valid = if(DEBUG) Some(Bool()) else None
    val wg_id = UInt(CONFIG.WG.WG_ID_WIDTH)
    val num_wf = UInt(log2Ceil(NUM_WF_MAX+1).W)                 // Number of wavefront in this cta
    val lds_dealloc_en = Bool()   // if LDS needs dealloc. When num_lds==0, lds do not need dealloc
    val sgpr_dealloc_en = Bool()
    val vgpr_dealloc_en = Bool()
  }
  val wf_gather_ram = Reg(Vec(NUM_CU, Vec(NUM_WG_SLOT, new cta_data_1)))
  when(fifo.io.deq.fire) {
    if(DEBUG) { wf_gather_ram(fifo.io.deq.bits.cu_id)(fifo.io.deq.bits.wg_slot_id).valid.get := true.B }
    wf_gather_ram(fifo.io.deq.bits.cu_id)(fifo.io.deq.bits.wg_slot_id).wg_id := fifo.io.deq.bits.wg_id
    wf_gather_ram(fifo.io.deq.bits.cu_id)(fifo.io.deq.bits.wg_slot_id).num_wf := fifo.io.deq.bits.num_wf
    wf_gather_ram(fifo.io.deq.bits.cu_id)(fifo.io.deq.bits.wg_slot_id).lds_dealloc_en  := fifo.io.deq.bits.lds_dealloc_en
    wf_gather_ram(fifo.io.deq.bits.cu_id)(fifo.io.deq.bits.wg_slot_id).sgpr_dealloc_en := fifo.io.deq.bits.sgpr_dealloc_en
    wf_gather_ram(fifo.io.deq.bits.cu_id)(fifo.io.deq.bits.wg_slot_id).vgpr_dealloc_en := fifo.io.deq.bits.vgpr_dealloc_en
  }

  val arb_inst = Module(new RRArbiter[io_cu2cuinterface](new io_cu2cuinterface, NUM_CU))
  for(i <- 0 until NUM_CU) { arb_inst.io.in(i) <> io.cu_wf_done(i) }

  val fifo_dealloc_rt = Module(new Queue(new io_cuinterface2rt, entries = 128))
  val fifo_host_wg_done = Module(new Queue(new io_cta2host, entries = 128))
  io.host_wg_done <> fifo_host_wg_done.io.deq
  io.rt_dealloc <> fifo_dealloc_rt.io.deq

  val wf_tag = arb_inst.io.out.bits.wf_tag.asTypeOf(new Bundle {
    val wg_slot_id = UInt(log2Ceil(NUM_WG_SLOT).W)
    val wf_id = UInt(log2Ceil(NUM_WF_MAX).W)
  })
  val cu_id = arb_inst.io.chosen
  if(DEBUG) {
    when(arb_inst.io.out.fire) {
      wf_gather_ram(cu_id)(wf_tag.wg_slot_id).valid.get := false.B
    }
  }
  arb_inst.io.out.ready := true.B

  fifo_dealloc_rt.io.enq.valid := arb_inst.io.out.fire
  fifo_dealloc_rt.io.enq.bits.cu_id := cu_id
  fifo_dealloc_rt.io.enq.bits.wg_slot_id := wf_tag.wg_slot_id
  fifo_dealloc_rt.io.enq.bits.num_wf := wf_gather_ram(cu_id)(wf_tag.wg_slot_id).num_wf
  fifo_dealloc_rt.io.enq.bits.lds_dealloc_en  := wf_gather_ram(cu_id)(wf_tag.wg_slot_id).lds_dealloc_en
  fifo_dealloc_rt.io.enq.bits.sgpr_dealloc_en := wf_gather_ram(cu_id)(wf_tag.wg_slot_id).sgpr_dealloc_en
  fifo_dealloc_rt.io.enq.bits.vgpr_dealloc_en := wf_gather_ram(cu_id)(wf_tag.wg_slot_id).vgpr_dealloc_en
  fifo_dealloc_rt.io.enq.bits.wg_id.get := wf_gather_ram(cu_id)(wf_tag.wg_slot_id).wg_id
  fifo_dealloc_rt.io.deq <> io.rt_dealloc

  fifo_host_wg_done.io.enq.valid := arb_inst.io.out.fire
  fifo_host_wg_done.io.enq.bits.wg_id := wf_gather_ram(cu_id)(wf_tag.wg_slot_id).wg_id
  fifo_host_wg_done.io.deq <> io.host_wg_done

  if(DEBUG) {
    assert(!arb_inst.io.out.fire || wf_gather_ram(cu_id)(wf_tag.wg_slot_id).wg_id === arb_inst.io.out.bits.wg_id.get)
  }
  assert(fifo_dealloc_rt.io.enq.ready)
  assert(fifo_host_wg_done.io.enq.ready)
}
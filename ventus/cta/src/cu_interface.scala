package cta_scheduler

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util._
import chisel3.experimental.dataview._
import cta_scheduler.cta_util.{RRPriorityEncoder, skid_valid}

class cu_interface(NUM_CU: Int = CONFIG.GPU.NUM_CU) extends Module {
  val io = IO(new Bundle{
    val wgbuffer_wg_new = Flipped(DecoupledIO(new io_buffer2cuinterface))
    val alloc_wg_new = Flipped(DecoupledIO(new io_alloc2cuinterface))
    val rt_wg_new = Flipped(DecoupledIO(new io_rt2cuinterface))
    //val allocator_dealloc = DecoupledIO(new io_cuinterface2alloc)
    val rt_dealloc = DecoupledIO(new io_cuinterface2rt)
    val host_wg_done = DecoupledIO(new io_cta2host)
  })

  val fifo = Module(new Queue(new io_cta2host, 8))

  if(CONFIG.DEBUG) {
    when(fifo.io.enq.fire) {
      assert(io.wgbuffer_wg_new.bits.wg_id === io.alloc_wg_new.bits.wg_id)
      assert(io.rt_wg_new.bits.wg_id.get === io.alloc_wg_new.bits.wg_id)
    }
  }

  //val data1 = RegEnable(io.wgbuffer_wg_new.bits, io.wgbuffer_wg_new.fire)
  //val data2 = RegEnable(io.alloc_wg_new.bits, io.alloc_wg_new.fire)
  io.wgbuffer_wg_new.ready := io.alloc_wg_new.valid && io.rt_wg_new.valid && fifo.io.enq.ready
  io.alloc_wg_new.ready := io.wgbuffer_wg_new.valid && io.rt_wg_new.valid && fifo.io.enq.ready
  io.rt_wg_new.ready := io.wgbuffer_wg_new.valid && io.alloc_wg_new.valid && fifo.io.enq.ready
  fifo.io.enq.valid := io.wgbuffer_wg_new.valid && io.alloc_wg_new.valid && io.rt_wg_new.valid
  fifo.io.enq.bits.wgslot := io.alloc_wg_new.bits.wg_slot_id
  fifo.io.enq.bits.cu_id := io.alloc_wg_new.bits.cu_id
  fifo.io.enq.bits.lds_base := io.rt_wg_new.bits.lds_base
  fifo.io.enq.bits.sgpr_base := io.rt_wg_new.bits.sgpr_base
  fifo.io.enq.bits.vgpr_base := io.rt_wg_new.bits.vgpr_base
  fifo.io.enq.bits.csr_kernel := io.wgbuffer_wg_new.bits.csr_kernel
  fifo.io.enq.bits.wg_id := io.wgbuffer_wg_new.bits.wg_id
  fifo.io.enq.bits.num_wf := io.alloc_wg_new.bits.num_wf

  io.host_wg_done <> fifo.io.deq

  val fifo_dealloc_rt = Module(new Queue(new io_cuinterface2rt, entries = 128))
  fifo_dealloc_rt.io.enq.valid := fifo.io.deq.fire
  fifo_dealloc_rt.io.enq.bits.wg_slot_id := fifo.io.deq.bits.wgslot
  fifo_dealloc_rt.io.enq.bits.cu_id := fifo.io.deq.bits.cu_id
  fifo_dealloc_rt.io.enq.bits.num_wf := fifo.io.deq.bits.num_wf
  if(CONFIG.DEBUG) {fifo_dealloc_rt.io.enq.bits.wg_id.get := fifo.io.deq.bits.wg_id}

  io.rt_dealloc <> fifo_dealloc_rt.io.deq

  //io.rt_dealloc.valid := fifo_dealloc_rt.io.deq.valid
  //io.rt_dealloc.bits.wg_slot_id := fifo_dealloc_rt.io.deq.bits.wg_slot_id
  //io.rt_dealloc.bits.cu_id := fifo_dealloc_rt.io.deq.bits.cu_id
  //if(CONFIG.DEBUG) {io.rt_dealloc.bits.wg_id.get := fifo_dealloc_rt.io.deq.bits.wg_id.get}
  //fifo_dealloc_rt.io.deq.ready := io.rt_dealloc.ready

  //val fifo_dealloc_allocator = Module(new Queue(new io_cuinterface2alloc, entries = 128))
  //fifo_dealloc_allocator.io.enq.valid := fifo.io.deq.fire
  //fifo_dealloc_allocator.io.enq.bits.wg_slot_id := fifo.io.deq.bits.wgslot
  //fifo_dealloc_allocator.io.enq.bits.cu_id := fifo.io.deq.bits.cu_id
  //fifo_dealloc_allocator.io.enq.bits.num_wf := fifo.io.deq.bits.num_wf
  //io.allocator_dealloc <> fifo_dealloc_allocator.io.deq

}
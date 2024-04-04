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
    val host_wg_done = DecoupledIO(new io_cta2host)
  })

  val fifo = Module(new Queue(new io_cta2host, 4))

  val data1 = RegEnable(io.wgbuffer_wg_new.bits, io.wgbuffer_wg_new.fire)
  val data2 = RegEnable(io.alloc_wg_new.bits, io.alloc_wg_new.fire)
  io.wgbuffer_wg_new.ready := io.alloc_wg_new.valid && fifo.io.enq.ready
  io.alloc_wg_new.ready := io.wgbuffer_wg_new.valid && fifo.io.enq.ready
  fifo.io.enq.valid := io.wgbuffer_wg_new.valid && io.alloc_wg_new.valid
  fifo.io.enq.bits.wg_id := io.alloc_wg_new.bits.wg_id
  //fifo.io.enq.bits.lds_base := io.alloc_wg_new.bits.lds_base
  fifo.io.enq.bits.csr_kernel := io.wgbuffer_wg_new.bits.csr_kernel

  io.host_wg_done <> fifo.io.deq
}
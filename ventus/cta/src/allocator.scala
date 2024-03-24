package cta_scheduler

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util._
import chisel3.experimental.dataview._
import cta_scheduler.cta_util.RRPriorityEncoder

class io_alloc2cuinterface extends Bundle with ctainfo_alloc_to_cuinterface with ctainfo_alloc_to_cu {
  val wg_id = UInt(CONFIG.WG.WG_ID_WIDTH)
}

class allocator(NUM_CU: Int = CONFIG.GPU.NUM_CU) extends Module {
  val io = IO(new Bundle {
    val wgbuffer_wg_new = Flipped(DecoupledIO(new io_buffer2alloc))
    val wgbuffer_result = DecoupledIO(new io_alloc2buffer)
    val cuinterface_wg_new = DecoupledIO(new io_alloc2cuinterface)
  })

  class tmp1 extends Bundle {
    val wgbuffer = new io_alloc2buffer
    val cuinterface = new io_alloc2cuinterface
  }
  val fifo = Module(new Queue(new tmp1, 4))

  object FSM_ST extends ChiselEnum {
    val IDLE = Value
    val RESOURCE_CHECK = Value  //
    val CU_SELECT = Value
  }
  val fsm = RegInit(FSM_ST.IDLE)
  val wg = RegEnable(io.wgbuffer_wg_new.bits, io.wgbuffer_wg_new.fire)
  val resource_check = RegInit(VecInit(Seq.fill(NUM_CU)(false.B)))

  io.wgbuffer_wg_new.ready := WireInit(fsm === FSM_ST.IDLE)

  switch(fsm) {
    is(FSM_ST.IDLE) {
      when(io.wgbuffer_wg_new.fire){
        fsm := FSM_ST.RESOURCE_CHECK
      }
    }
    is(FSM_ST.RESOURCE_CHECK){
      fsm := FSM_ST.CU_SELECT
    }
    is(FSM_ST.CU_SELECT){
      fsm := Mux(fifo.io.enq.fire, FSM_ST.IDLE, fsm)
    }
  }

  io.wgbuffer_wg_new.ready := (fsm === FSM_ST.IDLE)

  when(fsm === FSM_ST.RESOURCE_CHECK){
    for(i <- 0 until NUM_CU){
      resource_check(i) := wg.num_lds.xorR ^ random.LFSR(16).xorR
    }
  }

  val cu_sel = RRPriorityEncoder(resource_check)
  when(fsm === FSM_ST.CU_SELECT){   // Combinational logic
    cu_sel.ready := true.B
    fifo.io.enq.valid := true.B
    fifo.io.enq.bits.wgbuffer.accept := cu_sel.valid
    fifo.io.enq.bits.wgbuffer.wgram_addr := wg.wgram_addr
    fifo.io.enq.bits.cuinterface.wg_id := wg.wg_id
    fifo.io.enq.bits.cuinterface.num_wf := wg.num_wf
    fifo.io.enq.bits.cuinterface.cu_id := cu_sel.bits
    fifo.io.enq.bits.cuinterface.lds_base := wg.num_lds // TODO: for test
    fifo.io.enq.bits.cuinterface.sgpr_base := 0.U
    fifo.io.enq.bits.cuinterface.vgpr_base := 0.U
    fifo.io.enq.bits.cuinterface.wg_slot_id := 0.U
  } .otherwise {
    cu_sel.ready := false.B
    fifo.io.enq.valid := false.B
    fifo.io.enq.bits := DontCare
  }

  // =
  // Output stage
  // if wg rejected: allocation result is sent back to wg_buffer
  // if wg accepted: some wg info is sent to CU-interface, well others is sent back to wg_buffer for reading wgram2
  //                 CU interface receives wg info from allocator and wg_buffer using a 2-to-1 DecoupledIO
  //                 To ensure wg info from these 2 data path matches up,
  //                 an accepted wg info get valid for 2 wg_buffer and cu_interface in the same clock cycle
  // =
  val dataValid2, dataValid1 = RegInit(false.B)
  val data1 = RegEnable(fifo.io.deq.bits.wgbuffer, fifo.io.deq.fire)
  val data2 = RegEnable(fifo.io.deq.bits.cuinterface, fifo.io.deq.fire)
  // if accept: wg info is pop into data1 and data2. Ready to pop data <=> both data path is ready
  // if reject: wg info is only pop into data1.      Ready to pop data <=> data path 1 is ready, data path 2 don't care
  fifo.io.deq.ready := (!dataValid1 || io.wgbuffer_result.ready) && (!dataValid2 || io.cuinterface_wg_new.ready || !fifo.io.deq.bits.wgbuffer.accept)
  dataValid1 := fifo.io.deq.fire || (!io.wgbuffer_result.ready && dataValid1)
  dataValid2 := ((fifo.io.deq.fire && fifo.io.deq.bits.wgbuffer.accept) || (!io.cuinterface_wg_new.ready && dataValid2))
  io.wgbuffer_result.bits := data1
  io.cuinterface_wg_new.bits := data2
  io.wgbuffer_result.valid := dataValid1
  io.cuinterface_wg_new.valid := dataValid2
}
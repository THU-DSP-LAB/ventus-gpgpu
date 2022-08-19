package CTA

import chisel3._
import chisel3.util._
class wf_done_interface(val WG_ID_WIDTH: Int, val NUM_SCHEDULER: Int, val WG_NUM_MAX: Int) extends Module{
    val io = IO(new Bundle{
        val wf_done = Vec(NUM_SCHEDULER, Input(Bool()))
        val wf_done_wg_id = Vec(NUM_SCHEDULER, Input(UInt(WG_ID_WIDTH.W)))
        val host_wf_done_ready = Input(Bool())
        val host_wf_done_valid = Output(Bool())
        val host_wf_done_wg_id = Output(UInt(WG_ID_WIDTH.W))
    })
    val SEARCH_LENGTH = Math.max(WG_NUM_MAX >>> 5,4)
    val host_wf_done_valid_reg = RegInit(false.B)
    val host_wf_done_wg_id_reg = RegInit(0.U(WG_ID_WIDTH.W))
    io.host_wf_done_valid := host_wf_done_valid_reg
    io.host_wf_done_wg_id := host_wf_done_wg_id_reg
    val bitmap = RegInit(VecInit(Seq.fill(WG_NUM_MAX)(false.B)))
    for(i <- 0 until NUM_SCHEDULER){
        when(io.wf_done(i)){
            bitmap(io.wf_done_wg_id(i)) := true.B
        }
    }
    val search_start = RegInit(0.U(WG_ID_WIDTH.W))
    search_start := search_start + SEARCH_LENGTH.U
    //WG_NUM_MAX must be times of SEARCH_LENGTH
    val found = Wire(Bool())
    val found_wg_id = Wire(UInt(WG_ID_WIDTH.W))
    found := false.B
    found_wg_id := 0.U
    for(i <- 0 until SEARCH_LENGTH){
        when(bitmap(search_start + i.U)){
            found := true.B
            found_wg_id := search_start + i.U
        }
    }
    host_wf_done_valid_reg := found
    host_wf_done_wg_id_reg := found_wg_id
    when(io.host_wf_done_ready && host_wf_done_valid_reg){
        bitmap(host_wf_done_wg_id_reg) := false.B
    }
}
class wf_done_interface_single(val WG_ID_WIDTH: Int, val NUM_SCHEDULER: Int, val WG_NUM_MAX: Int) extends Module{
    val io = IO(new Bundle{
        val wf_done = Input(Bool())
        val wf_done_wg_id = Input(UInt(WG_ID_WIDTH.W))
        val host_wf_done_ready = Input(Bool())
        val host_wf_done_valid = Output(Bool())
        val host_wf_done_wg_id = Output(UInt(WG_ID_WIDTH.W))
    })
    val buffer = Module(new Queue(UInt(WG_ID_WIDTH.W),WG_NUM_MAX))
    buffer.io.enq.bits:=io.wf_done_wg_id
    buffer.io.enq.valid:=io.wf_done
    io.host_wf_done_wg_id:=buffer.io.deq.bits
    io.host_wf_done_valid:=buffer.io.deq.valid
    buffer.io.deq.ready:=io.host_wf_done_ready
}
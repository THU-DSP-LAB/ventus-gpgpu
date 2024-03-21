package cta_scheduler

import chisel3._
import chisel3.util._

trait ctrlinfo_alloc_to_wgbuffer extends Bundle {
  val accept = Bool()   // true.B: it is ok to send this wg to CU.      false.B: rejected
  val wg_id = UInt(CONFIG.WG.WG_ID_WIDTH)
}

class wg_buffer(NUM_ENTRIES: Int = CONFIG.WG_BUFFER.NUM_ENTRIES) extends Module {
  val io = IO(new Bundle{
    val host_wg_new = Flipped(DecoupledIO(new io_host2cta))                     // Get new wg from host
    val host_wg_done = DecoupledIO(new io_cta2host)                             // Tell host that a wg finished its execution
    val alloc_wg_new = DecoupledIO(new ctainfo_host_to_alloc {})                // Request allocator to determine if the given wg is ok to allocate
    val alloc_result = Flipped(DecoupledIO(new ctrlinfo_alloc_to_wgbuffer {}))  // Determination result from allocator
  })

  // =
  // wg_ram(addr) stores wg information sent by host
  // wg_ram_valid(addr) stores if wg_ram(addr) is valid information
  // =
  val wg_ram = Mem(NUM_ENTRIES, new io_host2cta)    // combinational/asynchronous-read memory
  val wg_ram_valid = RegInit(VecInit(Seq.fill(NUM_ENTRIES)(false.B)))

  // Next preferred writable/readable address of wg_ram
  val wg_ram_wr_next = cta_util.RRPriorityEncoder(VecInit(wg_ram_valid.map(~_)))
  val wg_ram_rd_next = cta_util.RRPriorityEncoder(wg_ram_valid)

  val wg_ram_wr_act = Wire(Bool())           // Take a write operation to   wg_ram and wg_ram_valid
  val wg_ram_rd_act = Wire(Bool())           // Take a read  operation from wg_ram
  val wg_ram_clear_act = Wire(Bool())        // Take a clear operation to   wg_ram_valid
  val wg_ram_clear_addr = RegEnable(wg_ram_rd_next.bits, 0.U(log2Ceil(NUM_ENTRIES).W), wg_ram_rd_act)

  // =
  // write new WG into wg_ram, host_wg_new interface
  // =

  // new wg from host is accepted  <=>  wg info written into wg_ram(next_valid_wr_addr)
  //                               <=>  privious found valid writable address is consumed
  io.host_wg_new.ready := wg_ram_wr_next.valid // new wg from host is accepted  <=>  found a valid writable address in wg_ram
  wg_ram_wr_next.ready := io.host_wg_new.valid // privious found writable address is consumed  <=>  new wg from host available

  // new wg available && writable address in wg_ram found  =>  write operation takes effect
  wg_ram_wr_act := wg_ram_wr_next.fire
  when(wg_ram_wr_act){
    wg_ram.write(wg_ram_wr_next.bits, io.host_wg_new.bits)
  }

  // =
  // read WG info from wg_ram, send them to allocator, alloc_wg_new interface
  // =

  val alloc_wg_new_valid_r = RegInit(false.B)
  io.alloc_wg_new.valid := alloc_wg_new_valid_r
  alloc_wg_new_valid_r := Mux(wg_ram_rd_act, true.B,
                          Mux(io.alloc_wg_new.ready, false.B, alloc_wg_new_valid_r))

  // When to read new wg info? (valid == false) || (valid == ready == true)
  // No wg is waiting for being sent to allocator, or the only waiting wg is currently being sent to allocator
  wg_ram_rd_next.ready := (~alloc_wg_new_valid_r || io.alloc_wg_new.ready)   // read operation is allowed
  wg_ram_rd_act := wg_ram_rd_next.fire                                       // read operation takes effect

  val wg_ram_rd_data = RegEnable(wg_ram.read(wg_ram_rd_next.bits), wg_ram_rd_act)
  io.alloc_wg_new.bits <> wg_ram_rd_data    // Is it right? ctainfo_host_to_alloc <> io_host2cta // TODO: check

  val allocating = RegInit(false.B)   // wg was already sent to allocator, waiting for alloc_result from allocator
  val allocating_ctainfo = RegEnable(wg_ram_rd_data, io.alloc_wg_new.fire)




  // =
  // wg_ram_valid set and reset
  // =

  when(wg_ram_wr_act && wg_ram_clear_act){
    assert(wg_ram_clear_addr =/= wg_ram_wr_next.bits)  // mutually exclusive signals, they should never equal to each other
  }
  when(wg_ram_clear_act) {
    wg_ram_valid(wg_ram_clear_addr) := false.B
  }
  when(wg_ram_wr_act) {
    wg_ram_valid(wg_ram_wr_next.bits) := true.B
  }


  //
  // TODO
  //
  io.host_wg_done.bits.wg_id := wg_ram_rd_data
  io.host_wg_done.valid := alloc_wg_new_valid_r

}
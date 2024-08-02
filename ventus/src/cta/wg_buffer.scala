package cta

import chisel3._
import chisel3.util._
import chisel3.experimental.dataview._
import cta.utils.RRPriorityEncoder

class io_alloc2buffer(NUM_ENTRIES: Int = CONFIG.WG_BUFFER.NUM_ENTRIES) extends Bundle {
  val accept = Bool()   // true.B: it is ok to send this wg to CU.      false.B: rejected
  val wgram_addr = UInt(log2Ceil(NUM_ENTRIES).W)
  val wg_id: Option[UInt] = if(CONFIG.DEBUG) Some(UInt(CONFIG.WG.WG_ID_WIDTH)) else None
}

class io_buffer2alloc(NUM_ENTRIES: Int = CONFIG.WG_BUFFER.NUM_ENTRIES) extends ctainfo_host_to_alloc {
  val wg_id: Option[UInt] = if(CONFIG.DEBUG) Some(UInt(CONFIG.WG.WG_ID_WIDTH)) else None
  val wgram_addr = UInt(log2Ceil(NUM_ENTRIES).W)
}

class io_buffer2cuinterface extends Bundle with ctainfo_host_to_cu with ctainfo_host_to_cuinterface {
  val wg_id = UInt(CONFIG.WG.WG_ID_WIDTH)
}

/** WG buffer that receives WG info from host and select proper WG for allocator
 * Main function:
 * 1. Write new wg info from host into wgram. Proper address is selected in Round-Robin method.
 * 2. Select and send wg to allocator. Proper address is selected in Round-Robin method. Only necessary wg info is sent.
 * 3.1 If allocator accepts a wg, remaining info of that wg is sent to cu-interface.
 * 3.2 If allocator rejects a wg, that wg is allowed to be sent to allocator in the next turn.
 * @param NUM_ENTRIES wg buffer depth
 * @see doc/wg_buffer.md
 * @note DecoupledIO.ready outputs are not registered
 */
class wg_buffer(NUM_ENTRIES: Int = CONFIG.WG_BUFFER.NUM_ENTRIES) extends Module {
  val io = IO(new Bundle{
    val host_wg_new = Flipped(DecoupledIO(new io_host2cta))             // Get new wg from host
    val alloc_wg_new = DecoupledIO(new io_buffer2alloc(NUM_ENTRIES))    // Request allocator to determine if the given wg is ok to allocate
    val alloc_result = Flipped(DecoupledIO(new io_alloc2buffer()))      // Determination result from allocator
    val cuinterface_wg_new = DecoupledIO(new io_buffer2cuinterface)
  })
  val DEBUG = CONFIG.DEBUG

  // =
  // wgram1(addr) stores wg information which is used by allocator
  // wgram2(addr) stores wg information which is used by CU-interface or CU
  //  wgram(addr) refers to the set of {wgram1(addr), wgram2(addr)}, not an actual hardware
  // wgram_valid(addr) stores if wg_ram(addr) is valid information
  // wgram_alloc(addr) stores if wg in wg_ram(addr) is now being processed by allocator
  // =
  class ram1datatype extends ctainfo_host_to_alloc{
    val wg_id = if(DEBUG) Some(UInt(CONFIG.WG.WG_ID_WIDTH)) else None
  }

  val wgram1 = Mem(NUM_ENTRIES, new ram1datatype)              // combinational/asynchronous-read memory
  val wgram2 = Mem(NUM_ENTRIES, new  io_buffer2cuinterface)    // combinational/asynchronous-read memory
  val wgram_valid = RegInit(Bits(NUM_ENTRIES.W), 0.U)
  val wgram_alloc = RegInit(Bits(NUM_ENTRIES.W), 0.U)

  val wgram1_wr_data = Wire(new ram1datatype)
  val wgram2_wr_data = Wire(new io_buffer2cuinterface)
  wgram1_wr_data := io.host_wg_new.bits    // TODO: check if the connection is right
  wgram2_wr_data := io.host_wg_new.bits

  // Next preferred writable/readable address of wg_ram
  val wgram_wr_next = RRPriorityEncoder(~wgram_valid)
  val wgram1_rd_next = RRPriorityEncoder(wgram_valid & ~wgram_alloc)

  val wgram_wr_act = Wire(Bool())          // Take a write operation to   wg_ram and wg_ram_valid
  val wgram1_rd_act = Wire(Bool())         // Take a read  operation from wg_ram1

  // =
  // Main function 1
  // write new WG into wg_ram, host_wg_new interface
  // =

  // new wg from host is accepted  <=>  wg info written into wg_ram(next_valid_wr_addr)
  //                               <=>  privious found valid writable address is consumed
  io.host_wg_new.ready := wgram_wr_next.valid // new wg from host is accepted  <=>  found a valid writable address in wg_ram
  wgram_wr_next.ready := io.host_wg_new.valid // previous found writable address is consumed  <=>  new wg from host available

  // new wg available && writable address in wg_ram found  =>  write operation takes effect
  wgram_wr_act := wgram_wr_next.fire
  when(wgram_wr_act){
    wgram1.write(wgram_wr_next.bits, wgram1_wr_data)
    wgram2.write(wgram_wr_next.bits, wgram2_wr_data)
  }

  // =
  // Main function 2
  // read WG info from wgram1, send them to allocator, alloc_wg_new interface
  // =

  val alloc_wg_new_valid_r = RegInit(false.B)
  io.alloc_wg_new.valid := alloc_wg_new_valid_r
  alloc_wg_new_valid_r := Mux(wgram1_rd_act, true.B,
                          Mux(io.alloc_wg_new.ready, false.B, alloc_wg_new_valid_r))

  // When to read new wg info for allocator? (valid == false) || (valid == ready == true)
  // No wg is waiting for being sent to allocator, or the only waiting wg is currently being sent to allocator
  wgram1_rd_next.ready := (!alloc_wg_new_valid_r || io.alloc_wg_new.ready)   // read operation is allowed
  wgram1_rd_act := wgram1_rd_next.fire                                      // read operation takes effect

  val wgram1_rd_data = Wire(new ram1datatype)
  wgram1_rd_data := RegEnable(wgram1.read(wgram1_rd_next.bits), wgram1_rd_act)
  val wgram1_rd_data_addr = RegEnable(wgram1_rd_next.bits, wgram1_rd_act)

  io.alloc_wg_new.bits.viewAsSupertype(new ctainfo_host_to_alloc {}) :=
    wgram1_rd_data.viewAsSupertype(new ctainfo_host_to_alloc {})    // TODO: check if the connection is right
  io.alloc_wg_new.bits.wgram_addr := wgram1_rd_data_addr
  if(DEBUG) io.alloc_wg_new.bits.wg_id.get := wgram1_rd_data.wg_id.get

  // =
  // Main function 3
  // read WG info from wgram2 and clear wgram when allocator requests
  // alloc_result & cuinterface_wg_new interface
  // =

  val wgram_rd2_clear_act = Wire(Bool())
  val wgram_rd2_clear_addr = WireInit(io.alloc_result.bits.wgram_addr)

  val cuinterface_new_wg_valid_r = RegInit(false.B)
  io.cuinterface_wg_new.valid := cuinterface_new_wg_valid_r
  cuinterface_new_wg_valid_r := Mux(wgram_rd2_clear_act, true.B,
                                Mux(io.cuinterface_wg_new.ready, false.B, cuinterface_new_wg_valid_r))
  val wgram2_rd_data = RegEnable(wgram2.read(wgram_rd2_clear_addr), wgram_rd2_clear_act)
  io.cuinterface_wg_new.bits := wgram2_rd_data

  if(DEBUG) {
    val alloc_result_wgid = RegEnable(io.alloc_result.bits.wg_id.get, wgram_rd2_clear_act)
    val wgram_rd2_clear_act_r1 = RegNext(wgram_rd2_clear_act, false.B)
    when(wgram_rd2_clear_act_r1) {
      assert(wgram2_rd_data.wg_id === alloc_result_wgid)
    }
  }

  // operation is allowed by internal of this Module when downstream datapath is not blocked: (valid == false) || (valid == ready == true)
  // operation is requested by external Module when (valid && allocation accepted)
  // operation takes effect <=> (allowed && requested)
  val wgram_rd2_clear_ready = !cuinterface_new_wg_valid_r || io.cuinterface_wg_new.ready   // clear is always allowed, just consider read wg_ram2
  wgram_rd2_clear_act := (io.alloc_result.fire && io.alloc_result.bits.accept) && wgram_rd2_clear_ready

  val wgram_alloc_clear_ready = true.B
  val wgram_alloc_clear_act = io.alloc_result.fire && !io.alloc_result.bits.accept && wgram_alloc_clear_ready
  val wgram_alloc_clear_addr = WireInit(io.alloc_result.bits.wgram_addr)

  io.alloc_result.ready := Mux(io.alloc_result.bits.accept, wgram_rd2_clear_ready, wgram_alloc_clear_ready)

  // =
  // wg_ram_valid set and reset
  // =

  val wgram_valid_setmask = WireInit(Bits(NUM_ENTRIES.W), wgram_wr_act << wgram_wr_next.bits)
  val wgram_valid_rstmask = WireInit(Bits(NUM_ENTRIES.W), wgram_rd2_clear_act << wgram_rd2_clear_addr)
  // Different set/reset request mustn't act on the same register
  // Mutual exclusivity of different write operations is guaranteed by wgram_valid and wgram_wr_next
  assert((wgram_valid_setmask & wgram_valid_rstmask).orR === false.B) // mutually exclusivity check
  wgram_valid := wgram_valid & ~wgram_valid_rstmask | wgram_valid_setmask

  val wgram_alloc_rstmask1 = WireInit(Bits(NUM_ENTRIES.W), wgram_wr_act << wgram_wr_next.bits)
  val wgram_alloc_rstmask2 = WireInit(Bits(NUM_ENTRIES.W), wgram_alloc_clear_act << wgram_alloc_clear_addr)
  val wgram_alloc_setmask = WireInit(Bits(NUM_ENTRIES.W), wgram1_rd_act << wgram1_rd_next.bits)
  // Different set/reset request mustn't act on the same register
  // Mutual exclusivity of different write operations is guaranteed by wgram_valid and wgram_alloc
  assert((wgram_alloc_rstmask1 & wgram_alloc_rstmask2 & wgram_alloc_setmask).orR === false.B) // mutually exclusivity check
  wgram_alloc := wgram_alloc & ~wgram_alloc_rstmask1 & ~wgram_alloc_rstmask2 | wgram_alloc_setmask
}
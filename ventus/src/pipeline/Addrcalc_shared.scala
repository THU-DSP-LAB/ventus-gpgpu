package pipeline

import top.parameters._
import chisel3._
import chisel3.util.{log2Up, _}
import IDecode._
import L2cache.{TLBundleA_lite, TLBundleD_lite_plus}
import org.scalactic.TypeCheckedTripleEquals.convertToCheckingEqualizer

//class tma2shared extends Module{
//  val io = IO(new Bundle{
//    val from_l2cache = Flipped(DecoupledIO(new TLBundleD_lite_plus(l2cache_params)))
//    val to_temp = DecoupledIO(new l2cache_transform(l2cache_params))
//  })
//  io.to_temp.valid := io.from_l2cache.valid
//  io.from_l2cache.ready := io.to_temp.ready
//  (0 until(numgroupl2cache)).foreach( x=> {
//    io.to_temp.bits.data(x) := io.from_l2cache.bits.data((x + 1) * (tma_aligned * 8) - 1, x * (tma_aligned * 8))
//  })
//  io.to_temp.bits.source := io.from_l2cache.bits.source
//}

class AddrCalc_shared() extends Module{
  val io = IO(new Bundle{
    val from_fifo = Flipped(DecoupledIO(new vExeDataTMA))
    val from_temp = Flipped(DecoupledIO(new SharedInput))
    //    val csr_wid = Output(UInt(depth_warp.W))
    //    val csr_pds = Input(UInt(xLen.W))
    //    val csr_numw = Input(UInt(xLen.W))
    //    val csr_tid = Input(UInt(xLen.W))
//    val to_mshr = DecoupledIO(new Bundle{
//      val tag = new MshrTag
//    })
    val idx_entry = Input(UInt(log2Up(tma_nMshrEntry).W))
    //    val to_l2cache = DecoupledIO(new DCacheCoreReq_np)
    val to_shared = DecoupledIO(new ShareMemCoreReq_np)
  })
  val s_idle :: s_save :: s_shared :: Nil = Enum(3)
  val state = RegInit(init = s_idle)
  val reg_save = Reg(new vExeDataTMA)
  val current_numdata = reg_save.ctrl.imm_ext.asUInt /(xLen/8).U
//  val cnt = new Counter(n = num_thread)
  val reg_entryID = RegInit(0.U(log2Up(tma_nMshrEntry).W))
  val addr = Wire(Vec(num_thread,UInt(xLen.W)))
  (0 until(num_thread)).foreach(x => { // assume the data stored in memory continuously
    addr(x) := Mux(x < current_numdata, reg_save.in2(0) + 4 * x, 0.U(xLen.W))
  })
  val addr_wire=Wire(UInt(xLen.W))
  addr_wire:= addr(PriorityEncoder(reg_save.mask.asUInt))
  val current_tag = Mux(reg_save.mask.asUInt=/=0.U, addr_wire(xLen-1, xLen-1-dcache_TagBits+1), 0.U(dcache_TagBits.W))
  val current_setIdx = Mux(reg_save.mask.asUInt=/=0.U, addr_wire(xLen-1-dcache_TagBits, xLen-1-dcache_TagBits-dcache_SetIdxBits+1), 0.U(dcache_SetIdxBits.W))
  val blockOffset = Wire(Vec(num_thread, UInt(dcache_BlockOffsetBits.W)))
  (0 until num_thread).foreach( x => blockOffset(x) := addr(x)(10, 2) )
  val wordOffset1H = Wire(Vec(num_thread, UInt(BytesOfWord.W)))

//  io.to_mshr.bits.tag.mask := reg_save.mask
//  io.to_mshr.bits.tag.reg_idxw := reg_save.ctrl.reg_idxw
//  io.to_mshr.bits.tag.warp_id := reg_save.ctrl.wid
//  io.to_mshr.bits.tag.wxd:=reg_save.ctrl.wxd
//  io.to_mshr.bits.tag.wfd:=reg_save.ctrl.wvd
//  io.to_mshr.bits.tag.isvec := reg_save.ctrl.isvec
//  io.to_mshr.bits.tag.unsigned := reg_save.ctrl.mem_unsigned
//  io.to_mshr.bits.tag.wordOffset1H := wordOffset1H
//  io.to_mshr.valid := state===s_save & (reg_save.ctrl.mem_cmd.orR)
//  io.to_mshr.bits.tag.isWrite := 0.U
  io.to_shared.bits.instrId := reg_entryID
  // |reg_save| -> |addr & mask| -> |PriorityEncoder| -> |tag & idx| -> |io.to_dcache.bits|
  //io.to_shared.bits.tag := tag
  // 128bytes
  io.to_shared.bits.setIdx := current_setIdx
  (0 until num_thread).foreach(x => {
    io.to_shared.bits.perLaneAddr(x).blockOffset := blockOffset(x)
    io.to_shared.bits.perLaneAddr(x).wordOffset1H := wordOffset1H(x)
    io.to_shared.bits.perLaneAddr(x).activeMask := reg_save.mask(x) && (addr(x)(xLen-1, xLen-1-dcache_TagBits+1)===current_tag && addr(x)(xLen-1-dcache_TagBits, xLen-1-dcache_TagBits-dcache_SetIdxBits+1)===current_setIdx)
  })
  io.to_shared.bits.data := io.from_temp.bits.data
  io.to_shared.bits.isWrite := 1.U
  io.to_shared.valid := state===s_shared
  val mask_next = Wire(Vec(num_thread, Bool()))

  (0 until num_thread).foreach( x => {                          // update mask
    mask_next(x) := reg_save.mask(x) && !(addr(x)(xLen-1, xLen-1-dcache_TagBits+1)===current_tag && addr(x)(xLen-1-dcache_TagBits, xLen-1-dcache_TagBits-dcache_SetIdxBits+1)===current_setIdx
      )})
  // End of Addr Logic


  io.from_fifo.ready := state === s_idle

  // FSM State Transfer
  switch(state){
    is (s_idle){
      when(io.from_fifo.valid){ state := s_save }.otherwise{ state := state }
      cnt.reset()
    }
    is (s_save){
//      when(reg_save.ctrl.mem_cmd.orR){ // read or write
        when(io.to_shared.fire){state := s_shared}.otherwise{state := s_save}
//      }.otherwise{ state := s_idle }
    }
    is (s_shared){
      when(io.to_shared.fire){
        when (cnt.value >= current_numdata || mask_next.asUInt===0.U){
          cnt.reset(); state := s_idle
        }.otherwise{
          cnt.inc(); state := s_shared
        }
      }.otherwise{state := s_shared}
    }
  }
  // FSM Operation
  switch(state){
    is (s_idle){
      when(io.from_fifo.fire){ // Next: s_save
//        reg_save := io.from_fifo.bits
        instructmem.write()
        (0 until num_thread).foreach(x => {
          reg_save.mask := Mux(x < current_numdata, true, false)  //initialize the first copynum bit to 1
        })
      }.otherwise{reg_save := RegInit(0.U.asTypeOf(new vExeData))}
    }
    is (s_save){
      when(reg_save.ctrl.mem_cmd.orR){//===1.U){  // read
        when(io.from_temp.fire){reg_entryID := io.idx_entry}  // get entryID from MSHR
      }
    }
    is (s_shared){
      when(io.to_shared.fire){                                      // request is sent
        reg_save.mask := mask_next
      }.otherwise{
        reg_save.mask := reg_save.mask
      }
    }
  }



}
package pipeline

import top.parameters._
import chisel3._
import chisel3.util.{log2Up, _}
import IDecode._
import L2cache.{InclusiveCacheParameters_lite, TLBundleA_lite, TLBundleD_lite, TLBundleD_lite_plus}
import org.scalactic.TypeCheckedTripleEquals.convertToCheckingEqualizer
import top.parameters._
import chisel3._
import chisel3.util.{log2Up, _}
import IDecode._
import org.scalactic.TypeCheckedTripleEquals.convertToCheckingEqualizer
import chisel3.util._

import scala.math.Ordered.orderingToOrdered

var tma_aligned = 4 // bytes
var maxcopysize = 128 //bytes  all 5D multiply together
var max_tma_inst = 4
var max_l2cacheline = 6
def numgroupinstmax = maxcopysize / tma_aligned
def numgroupsharedmax = maxcopysize / 4
class vExeDataTMA extends Bundle {
  //  val mode = Bool()
  //  val used = Bool()
  //  val complete = Bool()
  val in1 = UInt(xLen.W)
  val in2 = UInt(xLen.W)
  val srcsize = UInt(3.W)
  val interleave = Bool()
  //  val im2col = Vec(2,)
  val ctrl = new CtrlSigs()
}
class TMATag extends Bundle{
  val copysize = UInt(log2Up(maxcopysize).W)
//  val l2cachetag = Vec((numgroupinstmax), UInt(addr_tag_bits.W))
//  val addrinit = Vec((numgroupinstmax), UInt((xLen - addr_tag_bits).W))
//  val addrlast = Vec((numgroupinstmax), UInt((xLen - addr_tag_bits).W))
  val instruinfo = new vExeDataTMA()
}
class ShareMemCoreRsp_np_transform extends Bundle{
  val instrId = UInt(log2Up(max_tma_inst).W)
  //  val data = Vec(num_thread, UInt(xLen.W))
  val activeMask = Vec(numgroupshared, Bool())//UInt(NLanes.W)
}
class TempOutput extends Bundle{
  val entry_index = UInt(log2Up(max_tma_inst).W)
  val mask = Vec(numgroupl2cache, Bool())
  val data = Vec(numgroupl2cache, UInt((tma_aligned * 8).W))
  val l2cacheTag = UInt(l2cachetagbits.W)
  val instinfo = new TMATag()
}
class SharedInput extends Bundle{
  val entry_index = UInt(log2Up(max_tma_inst).W)
  val mask = Vec(num_thread, Bool())
  val data = Vec(num_thread, UInt((xLen).W))
  val l2cacheTag = UInt(l2cachetagbits.W)
  val instinfo = new vExeDataTMA()
}
class l2cache2temp extends Module {
  val io = IO(new Bundle{
    val from_l2cache = Flipped(DecoupledIO(new TLBundleD_lite_plus(l2cache_params)))
    val l22temp = DecoupledIO(new l2cache_transform(l2cache_params))
  })
  io.l22temp.valid := io.from_l2cache.valid
  io.from_l2cache.ready := io.l22temp.ready
  (0 until(numgroupl2cache)).foreach( x=> {
    io.l22temp.bits.data(x) := io.from_l2cache.bits.data((x + 1) * (tma_aligned * 8) - 1, x * (tma_aligned * 8))
  })
  io.l22temp.bits.source := io.from_l2cache.bits.source
}
class shared2temp extends Module {
  val io = IO(new Bundle{
    val from_shared = Flipped(DecoupledIO(new ShareMemCoreRsp_np))
    val sm2temp = DecoupledIO(new ShareMemCoreRsp_np_transform)
  })
  io.sm2temp.valid := io.from_shared.valid
  io.from_shared.ready := io.sm2temp.ready
  //  io.sm2temp.bits.data := io.from_shared.bits.data
  io.sm2temp.bits.activeMask := io.from_shared.bits.activeMask
}


class Temp_mem extends Module {
  val io = IO(new Bundle {
    val from_addr = Flipped(DecoupledIO(new Bundle {
      val tag = Input(new TMATag)
    }))
    val idx_entry = Output(UInt(log2Up(max_tma_inst).W))
    val from_l2cache = Flipped(DecoupledIO(new l2cache_transform(l2cache_params)))
    val from_shared = Flipped(DecoupledIO(new ShareMemCoreRsp_np_transform()))
    val to_shared = DecoupledIO(new TempOutput)
  })
  val dataFIFO =  Mem(max_l2cacheline, UInt(io.from_l2cache.bits.data.getWidth.W))
  val instMem =   Mem(max_tma_inst, UInt(io.from_addr.bits.tag.getWidth.W))
  val shared_cnt = RegInit(VecInit(Seq.fill(max_tma_inst)(0.U((log2Up(numgroupinstmax)).W))))
  val finish_cnt = RegInit(VecInit(Seq.fill(max_tma_inst)(0.U((log2Up(numgroupinstmax)).W))))
  val used_inst = RegInit(0.U(max_tma_inst.W))
  val used_mem = RegInit(0.U(max_l2cacheline.W))
  val complete = Wire(UInt(max_tma_inst.W))
  (0 until (max_tma_inst)).foreach(x => {
    complete(x) := finish_cnt(x) === 0.U
  })
  val entry_index_reg = RegInit(VecInit(Seq.fill(max_l2cacheline)(0.U(log2Up(max_tma_inst).W))))
  val valid_inst_entry = Mux(used_inst.andR, 0.U, PriorityEncoder(~used_inst))
  val valid_mem_entry = Mux(used_mem.andR, 0.U, PriorityEncoder(~used_mem))
  val output_entry = Mux(complete.orR, PriorityEncoder(complete), 0.U)

  //l2cache logic
  val current_inst_entry_index = io.from_l2cache.bits.source(log2Up(max_tma_inst) - 1, 0).asUInt

  val source_width = output_data.source.getWidth
//  val current_inst_entry_index = io.from_l2cache.bits.source(log2Up(max_tma_inst) - 1, 0).asUInt
  val current_tag = output_data.source(source_width - 1, source_width - 1 - addr_tag_bits + 1)
  val tag_reg = RegInit(0.U(addr_tag_bits.W))
  val current_inst_entry_index_reg = RegInit(0.U(log2Up(max_tma_inst).W))
  //  val current_tag_index = PriorityEncoder(reg_req(current_inst_entry_index).l2cachetag.map(_===current_tag))
  val current_mask_l2cache = RegInit(VecInit(Seq.fill(numgroupinstmax)(false.B)))
  val reg_req = Reg(new TMATag)
  val output_data = RegInit(new l2cache_transform(l2cache_params))
  val s_idle ::s_l2cache :: s_shared :: s_reset :: Nil = Enum(5)
  val state = RegInit(s_idle)
  io.from_l2cache.ready := state === s_idle && !(used_mem.andR)
  io.from_shared.ready := state === s_idle
  io.from_addr.ready := state === s_idle && !(used_inst.andR)
  io.idx_entry := Mux(io.from_addr.fire, valid_inst_entry, 0.U)
  when(state===s_idle){
    when(io.to_shared.ready && used_mem.orR){
      state := s_l2cache
    }.elsewhen(complete.orR){
      state := s_reset
    }.otherwise{
      state := s_idle
    }
  }.elsewhen(state===s_reset){
    state:=s_idle
  }.elsewhen(state===s_l2cache){
//    when(io.to_shared.ready){state:=s_shared}.otherwise{state:=s_l2cache}
    state:=s_shared
  }.elsewhen(state===s_shared){
    when(complete.orR){state:=s_reset}.otherwise{state:=s_idle}
  }.otherwise{state:=s_idle}

  switch(state){
    is(s_idle) {
      current_mask_l2cache := false.B
      when(io.from_addr.fire) {
        used_inst := used_inst.bitSet(valid_inst_entry, true.B)
        instMem.write(valid_inst_entry, io.from_addr.bits.asUInt)
        finish_cnt(valid_inst_entry) := io.from_addr.bits.tag.copysize / tma_aligned.asUInt
        shared_cnt(valid_inst_entry) := io.from_addr.bits.tag.copysize / tma_aligned.asUInt
      }
      when(io.from_l2cache.fire) {
        used_mem := used_mem.bitSet(valid_mem_entry, true.B)
        dataFIFO.write(valid_mem_entry,io.from_l2cache.bits.asUInt)
        entry_index_reg(valid_mem_entry) := current_inst_entry_index
//        reg_req := instMem.read(current_inst_entry_index)
//        tag_reg := current_tag
//        current_inst_entry_index_reg := current_inst_entry_index
      }
      when(io.from_shared.fire){
        finish_cnt(io.from_shared.bits.instrId) := finish_cnt(io.from_shared.bits.instrId) - PopCount(io.from_shared.bits.activeMask)
      }
      when(io.to_shared.ready){
        output_data := dataFIFO.read(entry_index_reg(PriorityEncoder(used_mem)))
        reg_req := instMem.read(entry_index_reg(PriorityEncoder(used_mem)))
        current_inst_entry_index_reg := entry_index_reg(PriorityEncoder(used_mem))
      }
    }
    is(s_l2cache){
      (0 until (numgroupl2cache)).foreach(x => {
        when(Cat(tag_reg, 0.U(xLen - addr_tag_bits)).asUInt + (x * tma_aligned.asUInt) >= reg_req.instruinfo.in1 && (Cat(tag_reg, 0.U(xLen - addr_tag_bits)).asUInt + (x * tma_aligned.asUInt)) < reg_req.instruinfo.in1 + reg_req.copysize) {
          current_mask_l2cache(x) := true.B
        }.otherwise {
          current_mask_l2cache(x) := false.B
        }
      })
    }
    is(s_shared){
      when(io.to_shared.fire){
        shared_cnt(current_inst_entry_index_reg) := shared_cnt(current_inst_entry_index_reg) - PopCount(current_mask_l2cache.asUInt)
//        dataFIFO.write(0.U,0.U)
        current_mask_l2cache := false.B
        reg_req :=  0.U
        tag_reg :=  0.U
        current_inst_entry_index_reg :=  0.U
      }
    }
    is(s_reset){
      used_inst := used_inst.bitSet(output_entry, false.B)
//      instMem.write(output_entry,0.U)
    }
  }
  io.to_shared.valid := state === s_shared
  io.to_shared.bits.data := output_data.data
  io.to_shared.bits.entry_index := current_inst_entry_index_reg
  io.to_shared.bits.mask := current_mask_l2cache
  io.to_shared.bits.instinfo := reg_req
  io.to_shared.bits.l2cacheTag := tag_reg
}
class AddrCalc_l2cache() extends Module{
  val io = IO(new Bundle{
    val from_fifo = Flipped(DecoupledIO(new vExeData))
    val to_tempmem = DecoupledIO(new Bundle{
      val tag = new TMATag
    })
    val idx_entry = Input(UInt(log2Up(max_tma_inst).W))
    val to_l2cache = DecoupledIO(new TLBundleA_lite(l2cache_params))
    //    val to_shared = DecoupledIO(new ShareMemCoreReq_np)
  })
  //  vExeData.in1: Dcache base addr
  //  vExeData.in2: Sharedmem base addr
  //  vExeData.ctrl.imm_ext: copysize
  //  maybe 4*4*2 byte one cp.async
  //  vExeData.ctrl.memw/h/b: data type : determine the mask sent to L1 cache
  val s_idle :: s_save :: s_l2cache :: Nil = Enum(3)
  val state = RegInit(init = s_idle)
  val reg_save = Reg(new vExeData)
  val current_numgroup = reg_save.ctrl.imm_ext.asUInt /tma_aligned.asUInt
  val cnt = new Counter(n = numgroupinstmax)
  val reg_entryID = RegInit(0.U(log2Up(max_tma_inst).W))
  val addr = Wire(Vec(numgroupinstmax,UInt(xLen.W)))
  (0 until(numgroupinstmax)).foreach(x => { // assume the data stored in memory continuously
    addr(x) := Mux(x < current_numgroup, reg_save.in1(0) + x * tma_aligned, 0.U(xLen.W))
  })
  val addr_wire=Wire(UInt(xLen.W))
  addr_wire:=addr(PriorityEncoder(reg_save.mask.asUInt))
  val current_tag = Mux(reg_save.mask.asUInt =/=0.U, addr_wire(xLen-1, xLen-1-l2cachetagbits+1), 0.U(l2cachetagbits.W))
  val setIdx = Mux(reg_save.mask.asUInt=/=0.U, addr_wire(xLen-1-l2cachetagbits, xLen-1-l2cachetagbits-dcache_SetIdxBits+1), 0.U(dcache_SetIdxBits.W))
  val blockOffset = Wire(Vec(numgroupinstmax, UInt(dcache_BlockOffsetBits.W)))

  (0 until numgroupinstmax).foreach( x => blockOffset(x) := addr(x)(10, 2) )
  val wordOffset1H = Wire(Vec(numgroupinstmax, UInt(BytesOfWord.W)))
  (0 until numgroupinstmax).foreach( x => {
    //DONE: Add Control Signals in vExeData.ctrl and define lw lh lb
    wordOffset1H(x) := 15.U(4.W)
    switch(reg_save.ctrl.mem_whb){
      is(MEM_W) { wordOffset1H(x) := 15.U }
      is(MEM_H) { wordOffset1H(x) :=
        Mux(addr(x)(1)===0.U,
          3.U,
          12.U
        )
      }
      is(MEM_B) { wordOffset1H(x) := 1.U << addr(x)(1,0) }
    }
  })
  val srcsize = Wire(UInt(3.W))
  switch(reg_save.ctrl.mem_whb){
      is(MEM_W){
        srcsize := 4.U
      }
      is(MEM_H){
        srcsize := 2.U
      }
      is(MEM_B){
        srcsize := 1.U
      }
    }
  val l2cachetag = Wire(Vec(numgroupinstmax, UInt(l2cachetagbits.W)))
  // assume all data in the same group has the same tag
  l2cachetag := addr.map(_(xLen-1, xLen - l2cachetagbits))
  io.to_tempmem.valid := state===s_save// & (reg_save.ctrl.mem_cmd.orR)
  io.to_tempmem.bits.tag.copysize := reg_save.ctrl.imm_ext
  io.to_tempmem.bits.tag.instruinfo.in1 := reg_save.in1
  io.to_tempmem.bits.tag.instruinfo.in2 := reg_save.in2
  io.to_tempmem.bits.tag.instruinfo.srcsize := srcsize
//  io.to_tempmem.bits.tag.addrinit := addrinit
//  io.to_tempmem.bits.tag.addrlast := addrlast
  //  io.to_tempmem.bits.tag.instruinfo :=reg_save
  io.to_l2cache.bits.opcode := 4.U
  io.to_l2cache.bits.size   := 0.U
  io.to_l2cache.bits.source :=  Cat(current_tag, reg_entryID)
  // temporarily set it as reg_entryID, for mshr; cat with wid, for shiftboard; jingguo ceshi hui fangzai hou 6 bit
  io.to_l2cache.bits.address := Cat(addr_wire(xLen-1, xLen-1-l2cachetagbits-dcache_SetIdxBits+1),0.U((xLen - l2cachetagbits-dcache_SetIdxBits).W))
  io.to_l2cache.bits.mask   := 0.U
  io.to_l2cache.bits.data   :=  0.U
  io.to_l2cache.bits.param  :=  0.U
  io.to_l2cache.valid := (state===s_l2cache)
  val mask_next = Wire(Vec(numgroupinstmax, Bool()))
  (0 until numgroupinstmax).foreach( x => {                          // update mask
    mask_next(x) := reg_save.mask(x) && !(addr(x)(xLen-1, xLen-1-l2cachetagbits+1)===current_tag && addr(x)(xLen-1-l2cachetagbits, xLen-1-l2cachetagbits-dcache_SetIdxBits+1)===setIdx
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
      when(io.to_tempmem.fire){state := s_l2cache}.otherwise{state := s_save}
      //      }.otherwise{ state := s_idle }
    }
    is (s_l2cache){
      when(io.to_l2cache.fire){
        when (cnt.value >= current_numgroup || mask_next.asUInt===0.U){
          cnt.reset(); state := s_idle
        }.otherwise{
          cnt.inc(); state := s_l2cache
        }
      }.otherwise{state := s_l2cache}
    }
  }
  // FSM Operation
  switch(state){
    is (s_idle){
      when(io.from_fifo.fire){ // Next: s_save
        reg_save := io.from_fifo.bits   // *save data
        (0 until numgroupinstmax).foreach(x => {
          reg_save.mask := Mux(x < current_numgroup, true, false)  //initialize the first copynum bit to 1
        })
      }.otherwise{reg_save := RegInit(0.U.asTypeOf(new vExeData))}
    }
    is (s_save){
      //      when(reg_save.ctrl.mem_cmd.orR){//===1.U){  // read
      when(io.to_tempmem.fire){reg_entryID := io.idx_entry}  // get entryID from MSHR
      //      }
    }
    is (s_l2cache){
      when(io.to_l2cache.fire){                                      // request is sent
        reg_save.mask := mask_next
      }.otherwise{
        reg_save.mask := reg_save.mask
      }
    }
  }
}

class Addrcalc_shared() extends Module {
  val io = IO(new Bundle {
    val from_temp = Flipped(DecoupledIO(new SharedInput))
    val to_shared = DecoupledIO(new ShareMemCoreReq_np)
  })
//  val s_idle :: s_save :: s_shared :: Nil = Enum(3)
  val s_idle :: s_shared :: Nil = Enum(2)
  val state = RegInit(init = s_idle)
  val reg_save = Reg(new SharedInput)
  val current_numgroup = PopCount(reg_save.mask)
  val cnt = new Counter(n = maxcopysize / 4.U)
//  val reg_entryID = RegInit(0.U(log2Up(max_tma_inst).W))
  val addr = Wire(Vec(numgroupsharedmax,UInt(xLen.W)))
//  val addr_init = reg_save.l2cacheTag - reg_save.instinfo.in1 + reg_save.instinfo.in2
  when(reg_save.l2cacheTag < reg_save.instinfo.in1){
    (0 until(numgroupsharedmax)).foreach(x => { // assume the data stored in memory continuously
      addr(x) := Mux(x < current_numgroup, reg_save.instinfo.in2 + x * 4.U, 0.U(xLen.W))
    })
  }.otherwise{
    (0 until(numgroupsharedmax)).foreach(x => { // assume the data stored in memory continuously
      addr(x) := Mux(x < current_numgroup, reg_save.l2cacheTag - reg_save.instinfo.in1 + reg_save.instinfo.in2 + x * 4.U, 0.U(xLen.W))
    })
  }

  val addr_wire=Wire(UInt(xLen.W))
  addr_wire:=addr(PriorityEncoder(reg_save.mask.asUInt))
  val current_tag = Mux(reg_save.mask.asUInt =/=0.U, addr_wire(xLen-1, xLen-1-l2cachetagbits+1), 0.U(l2cachetagbits.W))
  val setIdx = Mux(reg_save.mask.asUInt=/=0.U, addr_wire(xLen-1-l2cachetagbits, xLen-1-l2cachetagbits-dcache_SetIdxBits+1), 0.U(dcache_SetIdxBits.W))
  val blockOffset = Wire(Vec(numgroupinstmax, UInt(dcache_BlockOffsetBits.W)))

  (0 until numgroupinstmax).foreach( x => blockOffset(x) := addr(x)(10, 2) )
  val wordOffset1H = Wire(Vec(numgroupinstmax, UInt(BytesOfWord.W)))
  val mask_next = Wire(Vec(numgroupinstmax, Bool()))
  (0 until numgroupinstmax).foreach( x => {                          // update mask
    mask_next(x) := reg_save.mask(x) && !(addr(x)(xLen-1, xLen-1-l2cachetagbits+1)===current_tag && addr(x)(xLen-1-l2cachetagbits, xLen-1-l2cachetagbits-dcache_SetIdxBits+1)===setIdx
      )})
  var cnt_group = 0
  (0 until(numgroupinstmax)).foreach(x =>{
    when(reg_save.mask(x)){
      io.to_shared.bits.perLaneAddr(cnt_group).blockOffset := blockOffset(x)
      io.to_shared.bits.perLaneAddr(cnt_group).wordOffset1H := wordOffset1H(x)
      io.to_shared.bits.perLaneAddr(cnt_group).activeMask := reg_save.mask(x) && (addr(x)(xLen-1, xLen-1-dcache_TagBits+1)===current_tag && addr(x)(xLen-1-dcache_TagBits, xLen-1-dcache_TagBits-dcache_SetIdxBits+1)===setIdx)
      io.to_shared.bits.data(cnt_group) := reg_save.data(x)
      cnt_group = cnt_group + 1
    }
  })
  io.to_shared.valid := state===s_shared
  io.to_shared.bits.instrId := reg_save.entry_index


  io.from_temp.ready := state === s_idle

  // FSM State Transfer
  switch(state){
    is(s_idle){
      when(io.from_temp.valid){ state := s_shared}.otherwise{state := state}
      cnt.reset()
    }
//    is(s_save){
//      when(io.to_shared.fire){state := s_shared}.otherwise{state := s_save}
//    }
    is(s_shared){
      when(io.to_shared.fire){
        when(cnt.value >= current_numgroup || mask_next.asUInt === 0.U){
          cnt.reset()
          state := s_idle
        }.otherwise{
          cnt.inc();state := s_shared
        }
      }.otherwise{state := s_shared}
    }
  }

  switch(state){
    is(s_idle){
      when(io.from_temp.fire){
        reg_save := io.from_temp.bits
//        reg_save.mask :=  io.from_temp.bits.mask
      }.otherwise{ reg_save := RegInit(0.U.asTypeOf((new SharedInput)))}
    }
    is(s_shared){
      when(io.to_shared.fire){
        reg_save.mask := mask_next
      }.otherwise{
        reg_save.mask := reg_save.mask
      }
    }
  }
//  (0 until numgroupinstmax).foreach( x => {
//    //DONE: Add Control Signals in vExeData.ctrl and define lw lh lb
//    wordOffset1H(x) := 15.U(4.W)
//    switch(reg_save.ctrl.mem_whb){
//      is(MEM_W) { wordOffset1H(x) := 15.U }
//      is(MEM_H) { wordOffset1H(x) :=
//        Mux(addr(x)(1)===0.U,
//          3.U,
//          12.U
//        )
//      }
//      is(MEM_B) { wordOffset1H(x) := 1.U << addr(x)(1,0) }
//    }
//  })
//  val srcsize = Wire(UInt(3.W))
//  switch(reg_save.ctrl.mem_whb){
//    is(MEM_W){
//      srcsize := 4.U
//    }
//    is(MEM_H){
//      srcsize := 2.U
//    }
//    is(MEM_B){
//      srcsize := 1.U
//    }
//  }

}
class TMA_Copysize extends Module{
  val io = IO(new Bundle{
    //input
    val tma_req = Flipped(DecoupledIO(new vExeData()))
    /*usage:
    vExeData.in1: Dcache base addr
    vExeData.in2: Sharedmem base addr
    vExeData.ctrl.imm_ext: copysize
                           maybe 4*4*2 byte one cp.async
    vExeData.ctrl.memw/h/b: data type : determine the mask sent to L1 cache
    */
    val l2cache_rsp = Flipped(DecoupledIO(new TLBundleD_lite_plus(l2cache_params)))
    val shared_rsp = Flipped(DecoupledIO(new ShareMemCoreRsp_np()))
    //output
    val l2cache_req = Decoupled( new TLBundleA_lite(l2cache_params))
    val shared_req = DecoupledIO(new ShareMemCoreReq_np())
    val fence_end_tma = Output(UInt(num_warp.W))
  })
  val InputFIFO = Module(new Queue(new vExeData, entries=1, pipe=true))
  InputFIFO.io.enq <> io.tma_req
  val addrCalc_l2cache = Module(new AddrCalc_l2cache)
  addrCalc_l2cache.io.from_fifo <> InputFIFO.io.deq
  addrCalc_l2cache.io.idx_entry := tempmem.io.idx_entry
  io.l2ache_req <> addrCalc_l2cache.io.to_l2cache



  val tempmem = Module(new Temp_mem)
  tempmem.io.from_addr <> addrCalc_l2cache.io.to_tempmem
//  tempmem.io.from_l2cache <> io.l2cache_rsp
  tempmem.io.from_shared <> io.shared_rsp
  val addrCalc_shared = Module(new Addrcalc_shared)
  addrCalc_shared.io.from_temp <> tempmem.io.to_shared
  io.shared_req <> addrCalc_shared.io.to_shared

  val L2cache2Temp = Module(new l2cache2temp)
  L2cache2Temp.io.from_l2cache <> io.l2cache_rsp
  tempmem.io.from_l2cache <> L2cache2Temp.io.l22temp

  val Shared2Temp = Module(new shared2temp)
  Shared2Temp.io.from_shared <> io.shared_rsp
  tempmem.io.from_shared <> Shared2Temp.io.sm2temp

}
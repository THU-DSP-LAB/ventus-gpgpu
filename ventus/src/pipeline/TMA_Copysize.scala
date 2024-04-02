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


class regSave extends Bundle{
  val in1=UInt(xLen.W)
  val in2=UInt(xLen.W)
  val in3=UInt(xLen.W)
  val mask=Vec(numgroupinstmax,Bool())
  val ctrl=new CtrlSigs()
}
class vExeDataTMA extends Bundle {
  //  val mode = Bool()
  //  val used = Bool()
  //  val complete = Bool()
  val in1 = UInt(xLen.W)
  val in2 = UInt(xLen.W)
//  val in3 = UInt(xLen.W)
//  val copysize = UInt(log2Up(maxcopysize).W)
  val copysize = UInt(xLen.W)
//  val srcsize = UInt(3.W)
//  val interleave = Bool()
  //  val im2col = Vec(2,)
  val ctrl = new CtrlSigs()
}
//class TMATag extends Bundle{
//  val copysize = UInt(log2Up(maxcopysize).W)
////  val l2cachetag = Vec((numgroupinstmax), UInt(addr_tag_bits.W))
////  val addrinit = Vec((numgroupinstmax), UInt((xLen - addr_tag_bits).W))
////  val addrlast = Vec((numgroupinstmax), UInt((xLen - addr_tag_bits).W))
//  val instruinfo = new vExeDataTMA()
//}
class ShareMemCoreRsp_np_transform extends Bundle{
  val instrId = UInt(log2Up(max_tma_inst).W)
  val data = Vec(num_thread, UInt(xLen.W))
//  val activeMask = Vec(numgroupshared, Bool())//UInt(NLanes.W)
  val activeMask = Vec(num_thread, Bool())//UInt(NLanes.W)
}
class l2cache_transform(params: InclusiveCacheParameters_lite) extends Bundle
{
  val opcode=UInt(params.op_bits.W)
  val size=UInt(params.size_bits.W)
  val source=UInt(params.source_bits.W)
  val data  = Vec(numgroupl2cache, UInt((tma_aligned * 8).W))
  val param =UInt(3.W)
}
class TempOutput extends Bundle{
  val entry_index = UInt(log2Up(max_tma_inst).W)
  val mask = Vec(numgroupl2cache, Bool())
  val data = Vec(numgroupl2cache, UInt((tma_aligned * 8).W))
  val l2cacheTag = UInt(xLen.W)
  val instinfo = new vExeDataTMA()
}
//class SharedInput extends Bundle{
//  val entry_index = UInt(log2Up(max_tma_inst).W)
//  val mask = Vec(num_thread, Bool())
//  val data = Vec(num_thread, UInt((xLen).W))
//  val l2cacheTag = UInt(l2cachetagbits.W)
//  val instinfo = new TMATag()
//}
class l2cache2temp extends Module {
  val io = IO(new Bundle{
    val from_l2cache = Flipped(DecoupledIO(new TLBundleD_lite(l2cache_params)))
    val l22temp = DecoupledIO(new l2cache_transform(l2cache_params))
  })
  io.l22temp.valid := io.from_l2cache.valid
  io.from_l2cache.ready := io.l22temp.ready
  (0 until(numgroupl2cache)).foreach( x=> {
    io.l22temp.bits.data(x) := io.from_l2cache.bits.data((x + 1) * (tma_aligned * 8) - 1, x * (tma_aligned * 8))
  })
  io.l22temp.bits.source := io.from_l2cache.bits.source
  io.l22temp.bits.param := io.from_l2cache.bits.param
  io.l22temp.bits.size := io.from_l2cache.bits.size
  io.l22temp.bits.opcode := io.from_l2cache.bits.opcode
//  printf("l2rsp data: %d \n",io.from_l2cache.bits.data(128,0).asUInt)
//  printf("l22temp data: %d \n",io.l22temp.bits.data(0).asUInt)
//  printf("l22temp data: %d \n",io.l22temp.bits.data(1).asUInt)
//  printf("l22temp data: %d \n",io.l22temp.bits.data(2).asUInt)
//  printf("l22temp data: %d \n",io.l22temp.bits.data(3).asUInt)
}
class shared2temp extends Module {
  val io = IO(new Bundle{
    val from_shared = Flipped(DecoupledIO(new ShareMemCoreRsp_np))
    val sm2temp = DecoupledIO(new ShareMemCoreRsp_np_transform)
  })
//  io.sm2temp.valid := io.from_shared.valid
//  io.from_shared.ready := io.sm2temp.ready
//  io.sm2temp.bits.data := io.from_shared.bits.data
//  io.sm2temp.bits.activeMask := io.from_shared.bits.activeMask

  io.sm2temp <> io.from_shared
}

class AddrCalc_l2cache() extends Module{
  val io = IO(new Bundle{
    val from_fifo = Flipped(DecoupledIO(new vExeData))
    val to_tempmem = DecoupledIO(new Bundle{
      val tag = new vExeDataTMA
    })
    val idx_entry = Input(UInt(log2Up(max_tma_inst).W))
    val to_l2cache = DecoupledIO(new TLBundleA_lite(l2cache_params))
    //    val to_shared = DecoupledIO(new ShareMemCoreReq_np)
  })

  //  vExeData.in1(0): Dcache base addr
  //  vExeData.in2(0): Sharedmem base addr
  //  vExeData.in3(0): copysize
  //  maybe 4*4*2 byte one cp.async
  //  vExeData.ctrl.memw/h/b: data type : determine the mask sent to L1 cache
  val s_idle :: s_save :: s_l2cache :: Nil = Enum(3)
  val state = RegInit(init = s_idle)
  val reg_save = Reg(new regSave)
  val current_numgroup = io.from_fifo.bits.in3(0).asUInt /tma_aligned.asUInt
  val cnt = new Counter(n = numgroupinstmax)
  val reg_entryID = RegInit(0.U(log2Up(max_tma_inst).W))
  val addr = Wire(Vec(numgroupinstmax,UInt(xLen.W)))
  (0 until(numgroupinstmax)).foreach(x => { // assume the data stored in memory continuously
    addr(x) := Mux(x.asUInt < current_numgroup, reg_save.in1 + x.asUInt * tma_aligned.asUInt, 0.U(xLen.W))
  })
  val addr_wire=Wire(UInt(xLen.W))
  addr_wire:=addr(PriorityEncoder(reg_save.mask.asUInt))
  val current_tag = Mux(reg_save.mask.asUInt =/=0.U, addr_wire(xLen-1, xLen-1-l2cachetagbits+1), 0.U(l2cachetagbits.W))
  val setIdx = Mux(reg_save.mask.asUInt=/=0.U, addr_wire(xLen-1-l2cachetagbits, xLen-1-l2cachetagbits-l2cachesetbits
    +1), 0.U(l2cachesetbits
    .W))
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
  //  val srcsize = Wire(UInt(3.W))
  //  switch(reg_save.ctrl.mem_whb){
  //      is(MEM_W){
  //        srcsize := 4.U
  //      }
  //      is(MEM_H){
  //        srcsize := 2.U
  //      }
  //      is(MEM_B){
  //        srcsize := 1.U
  //      }
  //    }
  //  val l2cachetag = Wire(Vec(numgroupinstmax, UInt(l2cachetagbits.W)))
  // assume all data in the same group has the same tag
  //  l2cachetag := addr.map(_(xLen-1, xLen - l2cachetagbits))
  io.to_tempmem.valid := state===s_save// & (reg_save.ctrl.mem_cmd.orR)
  io.to_tempmem.bits.tag.in1 := reg_save.in1
  io.to_tempmem.bits.tag.in2 := reg_save.in2
//  io.to_tempmem.bits.tag.in3 := reg_save.in3
  io.to_tempmem.bits.tag.copysize := reg_save.in3
  io.to_tempmem.bits.tag.ctrl := reg_save.ctrl
  //  io.to_tempmem.bits.tag.srcsize := srcsize
  //  io.to_tempmem.bits.tag.addrinit := addrinit
  //  io.to_tempmem.bits.tag.addrlast := addrlast
  //  io.to_tempmem.bits.tag.instruinfo :=reg_save
  io.to_l2cache.bits.opcode := 4.U
  io.to_l2cache.bits.size   := 0.U
  io.to_l2cache.bits.source :=  Cat(Cat(current_tag, setIdx),reg_entryID)
  // temporarily set it as reg_entryID, for mshr; cat with wid, for shiftboard; jingguo ceshi hui fangzai hou 6 bit
  io.to_l2cache.bits.address := Cat(addr_wire(xLen-1, xLen-1-l2cachetagbits-l2cachesetbits+1),0.U((xLen - l2cachetagbits-l2cachesetbits).W))

  io.to_l2cache.bits.mask   := 0.U
  io.to_l2cache.bits.data   :=  0.U
  io.to_l2cache.bits.param  :=  0.U
  io.to_l2cache.valid := (state===s_l2cache)
  val mask_next = Wire(Vec(numgroupinstmax, Bool()))
  (0 until numgroupinstmax).foreach( x => {                          // update mask
    mask_next(x) := reg_save.mask(x) && !(addr(x)(xLen-1, xLen-1-l2cachetagbits+1)===current_tag && addr(x)(xLen-1-l2cachetagbits, xLen-1-l2cachetagbits-l2cachesetbits+1)===setIdx
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
          cnt.inc(); state := state
        }
      }.otherwise{state := state}
    }
  }
  // FSM Operation
  switch(state){
    is (s_idle){
      when(io.from_fifo.fire){ // Next: s_save
        reg_save.in1 := io.from_fifo.bits.in1(0)
        reg_save.in2 := io.from_fifo.bits.in2(0)
        reg_save.in3 := io.from_fifo.bits.in3(0)
        //        reg_save.mask := ZeroExt(io.from_fifo.bits.mask)
        reg_save.ctrl := io.from_fifo.bits.ctrl
        (0 until numgroupinstmax).foreach(x => {
          reg_save.mask(x) := Mux((x.asUInt < current_numgroup.asUInt).asBool, true.B, false.B)  //initialize the first copynum bit to 1
        })
      }.otherwise{reg_save := RegInit(0.U.asTypeOf(new regSave))}
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
  //  printf("state: %b\n",state.asUInt)
//    printf("copysize: %b\n",io.to_tempmem.bits.tag.copysize.asUInt)
//    printf("in3: %d\n",io.from_fifo.bits.in3(0).asUInt)
//    printf("reg_save_in3: %d\n",reg_save.in3.asUInt)
  //  printf("l2req:address: %b\n",io.to_l2cache.bits.address.asUInt)
}

class Temp_mem() extends Module {
  val io = IO(new Bundle {
    val from_addr = Flipped(DecoupledIO(new Bundle {
      val tag = Input(new vExeDataTMA)
    }))
    val idx_entry = Output(UInt(log2Up(max_tma_inst).W))
    val from_l2cache = Flipped(DecoupledIO(new l2cache_transform(l2cache_params)))
    val from_shared = Flipped(DecoupledIO(new ShareMemCoreRsp_np_transform()))
    val to_shared = DecoupledIO(new TempOutput)
    val inst_complete = DecoupledIO(UInt(32.W))
  })
//  val dataFIFO =  Mem(max_l2cacheline, UInt(io.from_l2cache.bits.getWidth.W))
  val dataFIFO =  Mem(max_l2cacheline, new l2cache_transform(l2cache_params))
  val instMem =   Mem(max_tma_inst, new vExeDataTMA)
//  val shared_cnt = RegInit(VecInit(Seq.fill(max_tma_inst)((numgroupinstmax-1).U((log2Up(numgroupinstmax)).W))))
//  val finish_cnt = RegInit(VecInit(Seq.fill(max_tma_inst)((numgroupinstmax-1).U((log2Up(numgroupinstmax)).W))))
  val shared_cnt = RegInit(VecInit(Seq.fill(max_tma_inst)((numgroupinstmax-1).U((xLen).W))))
  val finish_cnt = RegInit(VecInit(Seq.fill(max_tma_inst)((numgroupinstmax-1).U((xLen).W))))
  val used_inst = RegInit(0.U(max_tma_inst.W))
  val used_mem = RegInit(0.U(max_l2cacheline.W))
  val complete = Wire(Vec(max_tma_inst,Bool()))
  (0 until (max_tma_inst)).foreach(x => {
    complete(x) := finish_cnt(x) === 0.U
  })
  val entry_index_reg = RegInit(VecInit(Seq.fill(max_l2cacheline)(0.U(log2Up(max_tma_inst).W))))
  val valid_inst_entry = Mux(used_inst.andR, 0.U, PriorityEncoder(~used_inst))
  val valid_mem_entry = Mux(used_mem.andR, 0.U, PriorityEncoder(~used_mem))
  val output_inst_entry = Mux(complete.asUInt.orR, PriorityEncoder(complete), 0.U)
  val output_data_entry = Reg(UInt(log2Up(max_l2cacheline).W))

  //l2cache logic
  val current_inst_entry_index = io.from_l2cache.bits.source(log2Up(max_tma_inst) - 1, 0).asUInt

//  val current_inst_entry_index = io.from_l2cache.bits.source(log2Up(max_tma_inst) - 1, 0).asUInt

  val current_inst_entry_index_reg = RegInit(0.U(log2Up(max_tma_inst).W))
  //  val current_tag_index = PriorityEncoder(inst_to_shared(current_inst_entry_index).l2cachetag.map(_===current_tag))
  val current_mask_l2cache = RegInit(VecInit(Seq.fill(numgroupl2cache)(false.B)))
  val inst_to_shared = Reg(new vExeDataTMA)
  val output_inst = Reg(new vExeDataTMA)
  val output_data = Reg(new l2cache_transform(l2cache_params))
  val source_width = output_data.source.getWidth
//  val tag_reg = RegInit(0.U(addr_tag_bits.W))
  val tag_wire = Wire(UInt(xLen.W))
  tag_wire := Cat(output_data.source(source_width - 1, source_width - 1 - addr_tag_bits + 1),0.U((xLen - addr_tag_bits).W))

  val s_idle ::s_getdata :: s_shared :: s_reset :: Nil = Enum(4)
  val state = RegInit(s_idle)
  io.from_l2cache.ready := state === s_idle && !(used_mem.andR)
  io.from_shared.ready := state === s_idle //&& used_mem.orR
  io.from_addr.ready := state === s_idle && !(used_inst.andR)
  io.to_shared.valid := state === s_idle && used_mem.orR
  io.inst_complete.valid := state === s_reset
  io.idx_entry := Mux(io.from_addr.fire, valid_inst_entry, 0.U)

  switch(state){
    is(s_idle){
      when(io.to_shared.ready && used_mem.orR && !io.from_l2cache.fire){
        state := s_getdata
      }.elsewhen(complete.asUInt.orR){
        state := s_reset
      }.otherwise{
        state := s_idle
      }
    }
    is(s_getdata){
      //    when(io.to_shared.ready){state:=s_shared}.otherwise{state:=s_getdata}
      state:=s_shared
    }
    is(s_shared){
      when(complete.asUInt.orR){state:=s_reset}.otherwise{
        when(io.to_shared.fire) {
          state := s_idle
        }.otherwise{
          state := state
        }
      }
    }
    is(s_reset){
      state:=s_idle
    }
  }

  switch(state){
    is(s_idle) {
      current_mask_l2cache := VecInit(Seq.fill(numgroupl2cache)(false.B))
      output_data_entry := 0.U
//      output_data := 0.U
      when(io.from_addr.fire) {
        used_inst := used_inst.bitSet(valid_inst_entry, true.B)
        instMem.write(valid_inst_entry, io.from_addr.bits.tag)
        finish_cnt(valid_inst_entry) := io.from_addr.bits.tag.copysize >> log2Up(tma_aligned)
        shared_cnt(valid_inst_entry) := io.from_addr.bits.tag.copysize >> log2Up(tma_aligned)
      }
      when(io.from_l2cache.fire) {
        used_mem := used_mem.bitSet(valid_mem_entry, true.B)
        dataFIFO.write(valid_mem_entry,io.from_l2cache.bits)
        entry_index_reg(valid_mem_entry) := current_inst_entry_index
//        inst_to_shared := instMem.read(current_inst_entry_index)
//        tag_reg := io.from_l2cache.bits.source(source_width - 1, source_width - 1 - addr_tag_bits + 1)
//        current_inst_entry_index_reg := current_inst_entry_index
      }
      when(io.from_shared.fire){
        finish_cnt(io.from_shared.bits.instrId) := finish_cnt(io.from_shared.bits.instrId) - PopCount(io.from_shared.bits.activeMask)
      }
      when(io.to_shared.ready && used_mem.orR){
        output_data := dataFIFO.read(PriorityEncoder(used_mem))
        inst_to_shared := instMem.read(entry_index_reg(PriorityEncoder(used_mem)))
        current_inst_entry_index_reg := entry_index_reg(PriorityEncoder(used_mem))
        output_data_entry := PriorityEncoder(used_mem)
      }
      when(complete.asUInt.orR){
        output_inst := instMem(PriorityEncoder(complete.asUInt))
      }
    }
    is(s_getdata){
//      tag_reg := tag_wire

      (0 until (numgroupl2cache)).foreach(x => {
        when((tag_wire.asUInt + (x.asUInt * tma_aligned.asUInt) >= inst_to_shared.in1) && (tag_wire.asUInt + (x.asUInt * tma_aligned.asUInt) < inst_to_shared.in1 + inst_to_shared.copysize)) {
          current_mask_l2cache(x) := true.B
        }.otherwise {
          current_mask_l2cache(x) := false.B
        }
      })
    }
    is(s_shared){
      when(io.to_shared.fire){
        used_mem := used_mem.bitSet(output_data_entry, false.B)
        printf("output_data_entry : %d",output_data_entry)
        output_data_entry := 0.U
        shared_cnt(current_inst_entry_index_reg) := shared_cnt(current_inst_entry_index_reg) - PopCount(current_mask_l2cache.asUInt)
//        dataFIFO.write(0.U,0.U)
        current_mask_l2cache := VecInit(Seq.fill(numgroupinstmax)(false.B))
        inst_to_shared :=  0.U.asTypeOf(new vExeDataTMA)
//        tag_reg :=  0.U(addr_tag_bits.W)
        current_inst_entry_index_reg :=  0.U(log2Up(max_tma_inst).W)
      }
    }
    is(s_reset){
      used_inst := used_inst.bitSet(output_inst_entry, false.B)
      finish_cnt(PriorityEncoder(complete)) := 1.U
//      instMem.write(output_inst_entry,0.U)
    }
  }
  io.to_shared.valid := state === s_shared
  io.to_shared.bits.entry_index := current_inst_entry_index_reg
  io.to_shared.bits.mask := current_mask_l2cache
  io.to_shared.bits.data := output_data.data
  io.to_shared.bits.l2cacheTag := tag_wire
  io.to_shared.bits.instinfo := inst_to_shared
  io.inst_complete.bits := output_inst.ctrl.pc


//  printf("state: %d \n",state.asUInt)
//  printf("usedmem: %b \n",used_mem.asUInt)
//  printf("to_shared: %b \n",io.to_shared.ready.asUInt)
//  printf("l22temp data: %d \n",io.from_l2cache.bits.data(0).asUInt)
//  printf("l22temp data: %d \n",io.from_l2cache.bits.data(1).asUInt)
//  printf("l22temp data: %d \n",io.from_l2cache.bits.data(2).asUInt)
//  printf("l22temp data: %d \n",io.from_l2cache.bits.data(3).asUInt)

}






class Addrcalc_shared() extends Module {
  val io = IO(new Bundle {
    val from_temp = Flipped(DecoupledIO(new TempOutput))
    val to_shared = DecoupledIO(new ShareMemCoreReq_np)
  })
//  val s_idle :: s_save :: s_shared :: Nil = Enum(3)
  val s_idle :: s_shared1 ::s_shared2:: s_shared3::Nil = Enum(4)
  val state = RegInit(init = s_idle)
  val reg_save = Reg(new TempOutput)
  val current_numgroup = PopCount(reg_save.mask)
  val output_reg = Reg(new ShareMemCoreReq_np)
  val cnt = new Counter(n = numgroupl2cache)
//  val reg_entryID = RegInit(0.U(log2Up(max_tma_inst).W))
  val addr = Wire(Vec(numgroupl2cache,UInt(xLen.W)))
  (0 until(numgroupl2cache)).foreach(x => { // assume the data stored in memory continuously
    addr(x) := reg_save.l2cacheTag - reg_save.instinfo.in1 + reg_save.instinfo.in2 + x.asUInt * 4.U
  })
//  val addr_init = reg_save.l2cacheTag - reg_save.instinfo.in1 + reg_save.instinfo.in2
//  when(reg_save.l2cacheTag < reg_save.instinfo.in1){
//    (0 until(numgroupl2cache)).foreach(x => { // assume the data stored in memory continuously
//      addr(x) := Mux(x.asUInt < current_numgroup, reg_save.instinfo.in2 + x.asUInt * 4.U, 0.U(xLen.W))
//    })
//  }.otherwise{
//    (0 until(numgroupl2cache)).foreach(x => { // assume the data stored in memory continuously
//      addr(x) := Mux(x.asUInt < current_numgroup, reg_save.l2cacheTag - reg_save.instinfo.in1 + reg_save.instinfo.in2 + x.asUInt * 4.U, 0.U(xLen.W))
//    })
//  }

  val addr_wire=Wire(UInt(xLen.W))
  addr_wire:=addr(PriorityEncoder(reg_save.mask.asUInt))
  val current_tag = Mux(reg_save.mask.asUInt =/=0.U, addr_wire(xLen-1, xLen-1-dcache_TagBits+1), 0.U(dcache_TagBits.W))
  val setIdx = Mux(reg_save.mask.asUInt=/=0.U, addr_wire(xLen-1-dcache_TagBits, xLen-1-dcache_TagBits-dcache_SetIdxBits+1), 0.U(dcache_SetIdxBits.W))
  val blockOffset = Wire(Vec(numgroupl2cache, UInt(dcache_BlockOffsetBits.W)))
  (0 until numgroupl2cache).foreach( x => blockOffset(x) := addr(x)(10, 2) )
  val wordOffset1H = Wire(Vec(numgroupl2cache, UInt(BytesOfWord.W)))
  (0 until numgroupl2cache).foreach( x => {
    //DONE: Add Control Signals in vExeData.ctrl and define lw lh lb
    wordOffset1H(x) := 15.U(4.W)
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
  })
  val current_mask = Wire(Vec(numgroupl2cache, Bool()))
  (0 until numgroupl2cache).foreach(x => {
    current_mask(x) := reg_save.mask(x) && (addr(x)(xLen-1, xLen-1-dcache_TagBits+1)===current_tag && addr(x)(xLen-1-dcache_TagBits, xLen-1-dcache_TagBits-dcache_SetIdxBits+1)===setIdx)
  })
  val mask_next = Wire(Vec(numgroupl2cache, Bool()))
  (0 until numgroupl2cache).foreach( x => {
    mask_next(x) := reg_save.mask(x) && !(addr(x)(xLen-1, xLen-1-dcache_TagBits+1)===current_tag && addr(x)(xLen-1-dcache_TagBits, xLen-1-dcache_TagBits-dcache_SetIdxBits+1)===setIdx)
    })
  //找到current_mask的全部索引，只使用前16个，提取l2cacheline的数据
  val current_mask_index = Reg(Vec(numgroupl2cache, UInt(log2Up(numgroupl2cache).W)))

  io.to_shared.bits := output_reg
  io.to_shared.valid := state===s_shared3

  io.from_temp.ready := state === s_idle
  var cnt_mask = 0
  // FSM State Transfer
  switch(state){
    is(s_idle){
      when(io.from_temp.valid){ state := s_shared1}.otherwise{state := state}
      cnt.reset()
    }
//    is(s_save){
//      when(io.to_shared.fire){state := s_shared}.otherwise{state := s_save}
//    }
    is(s_shared1){
      when(io.to_shared.ready){
        state := s_shared2
      }.otherwise{
        state := s_shared1
      }
    }
    is(s_shared2){
      state := s_shared3
    }
    is(s_shared3){
      when(io.to_shared.fire){
        when(cnt.value >= current_numgroup || mask_next.asUInt === 0.U){
          cnt.reset()
          state := s_idle
        }.otherwise{
          cnt.inc();state := s_shared1
        }
      }.otherwise{state := s_shared3}
    }
  }

  switch(state){
    is(s_idle){
      when(io.from_temp.fire){
        reg_save := io.from_temp.bits
//        reg_save.mask :=  io.from_temp.bits.mask
        output_reg := RegInit(0.U.asTypeOf((new ShareMemCoreReq_np)))
        current_mask_index := RegInit(VecInit(Seq.fill(numgroupl2cache)(0.U(log2Up(numgroupl2cache).W))))
      }.otherwise{
        reg_save := RegInit(0.U.asTypeOf((new TempOutput)))
        output_reg := RegInit(0.U.asTypeOf((new ShareMemCoreReq_np)))
        current_mask_index := RegInit(VecInit(Seq.fill(numgroupl2cache)(0.U(log2Up(numgroupl2cache).W))))
      }
    }
    is(s_shared1){
      output_reg := RegInit(0.U.asTypeOf((new ShareMemCoreReq_np)))
      current_mask_index := RegInit(VecInit(Seq.fill(numgroupl2cache)(0.U(log2Up(numgroupl2cache).W))))
      cnt_mask = 0
      (0 until numgroupl2cache).foreach( x => {
        when(current_mask(x)){
          current_mask_index(cnt_mask) := x.asUInt
          cnt_mask = cnt_mask + 1
        }
      })
    }
    is(s_shared2){
//      var cnt_group = 0
      (0 until(num_thread)).foreach(x =>{
        when(current_mask_index(x).asUInt === 0.U){
          when(x.asUInt === 0.U){
            output_reg.perLaneAddr(x).blockOffset := blockOffset(current_mask_index(x))
            output_reg.perLaneAddr(x).wordOffset1H := wordOffset1H(current_mask_index(x))
            output_reg.perLaneAddr(x).activeMask := true.B
            output_reg.data(x) := reg_save.data(current_mask_index(x))
//            cnt_group = cnt_group + 1
          }.otherwise{
            output_reg.perLaneAddr(x).blockOffset := 0.U
            output_reg.perLaneAddr(x).wordOffset1H := 0.U
            output_reg.perLaneAddr(x).activeMask := false.B
            output_reg.data(x) := 0.U
          }
        }.otherwise{
          output_reg.perLaneAddr(x).blockOffset := blockOffset(current_mask_index(x))
          output_reg.perLaneAddr(x).wordOffset1H := wordOffset1H(current_mask_index(x))
          output_reg.perLaneAddr(x).activeMask := true.B
          output_reg.data(x) := reg_save.data(current_mask_index(x))
        }

      })

      output_reg.instrId := reg_save.entry_index
      output_reg.isWrite := true.B
      output_reg.setIdx := setIdx
    }
    is(s_shared3){
      when(io.to_shared.fire){
        reg_save.mask := mask_next
      }.otherwise{
        reg_save.mask := reg_save.mask
      }
    }
  }
//  printf("state: %d \n",state)
//  printf("mask: %b \n",mask_next.asUInt)
//  (0 until numgroupl2cache).foreach( x => {
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
    val l2cache_rsp = Flipped(DecoupledIO(new TLBundleD_lite(l2cache_params)))
    val shared_rsp = Flipped(DecoupledIO(new ShareMemCoreRsp_np()))
    //output
    val l2cache_req = Decoupled( new TLBundleA_lite(l2cache_params))
    val shared_req = DecoupledIO(new ShareMemCoreReq_np())
    val fence_end_tma = DecoupledIO(UInt(32.W))
  })
//  printf("l2req: %d \n",io.l2cache_req.bits.address.asUInt)
//  printf("l2req.valid: %b \n",io.l2cache_req.valid.asUInt)
//  printf("l2rsp data: %d \n",io.l2cache_rsp.bits.data(128,0).asUInt)
//  printf("l2rsp source: %d \n",io.l2cache_rsp.bits.source.asUInt)



  val InputFIFO = Module(new Queue(new vExeData, entries=1, pipe=true))
  InputFIFO.io.enq <> io.tma_req
  val addrCalc_l2cache = Module(new AddrCalc_l2cache)
  addrCalc_l2cache.io.from_fifo <> InputFIFO.io.deq
  io.l2cache_req <> addrCalc_l2cache.io.to_l2cache



  val tempmem = Module(new Temp_mem)
  addrCalc_l2cache.io.idx_entry := tempmem.io.idx_entry
  tempmem.io.from_addr <> addrCalc_l2cache.io.to_tempmem
//  tempmem.io.from_l2cache <> io.l2cache_rsp
//  tempmem.io.from_shared <> io.shared_rsp
  io.fence_end_tma <> tempmem.io.inst_complete
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
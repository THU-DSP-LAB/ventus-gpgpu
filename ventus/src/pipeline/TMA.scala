package pipeline

import top.parameters._
import chisel3._
import chisel3.util.{log2Up, _}
import IDecode._
import L2cache.{TLBundleA_lite, TLBundleD_lite_plus}
import org.scalactic.TypeCheckedTripleEquals.convertToCheckingEqualizer
/*
* TMA
* usage: asynchronously copy data from Dcache(L1 cache)
* to sharedmem
* assume all thread in a warp carry the same instruction: cp.async, s
* */
var l2cachetagbits = 16
var l2cacheline = 128
var tma_aligned = 4 //bytes
var maxcopysize = 128 //bytes
var maxsrcsize = 4 //bytes
var minsrcsize = 1 //bytes
var addr_size_per_num = 4//bytes
def maxcopynum: Int = maxcopysize / minsrcsize
def tma_nMshrEntry = num_warp // less than num_warp
class vExeDataTMA extends Bundle{
  val mode = Bool()
  val data = Vec(maxcopynum,UInt(xLen.W))
//  val used = Bool()
//  val complete = Bool()
  val in1 = UInt(xLen.W)
  val in2 = UInt(xLen.W)
  val ctrl=new CtrlSigs()
}
class MshrTMATag extends Bundle{
  val warp_id = UInt(depth_warp.W)
  val line_mask = Vec(l2cacheline/ tma_aligned, Bool())
  val tag_mask = Vec(maxcopynum, Bool())
  val reg_idxw = UInt((regidx_width + regext_width).W)
  val tag = UInt((dcache_TagBits+dcache_SetIdxBits).W)
  val wfd = Bool()
  val wxd = Bool()
  val wordOffset1H = Vec(num_thread, UInt(BytesOfWord.W))
  val isWrite = Bool()
  val spike_info = if (SPIKE_OUTPUT) Some(new InstWriteBack) else None
}


// upon this line, there is the new parameter definition

class AddrCalc_l2cache() extends Module{
  val io = IO(new Bundle{
    val from_fifo = Flipped(DecoupledIO(new vExeData))
    //    val csr_wid = Output(UInt(depth_warp.W))
    //    val csr_pds = Input(UInt(xLen.W))
    //    val csr_numw = Input(UInt(xLen.W))
    //    val csr_tid = Input(UInt(xLen.W))
    val to_mshr = DecoupledIO(new Bundle{
      val tag = new MshrTMATag
    })
    val idx_entry = Input(UInt(log2Up(tma_nMshrEntry).W))
    val to_l2cache = DecoupledIO(new TLBundleA_lite(l2cache_params))
    )
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
  val srcsize = Wire(UInt(4.W)) // define srcsize based on MEM_WHB
  val copynum = reg_save.ctrl.imm_ext / srcsize
  val cnt = new Counter(n = maxcopynum)
  val reg_entryID = RegInit(0.U(log2Up(tma_nMshrEntry).W))
  val addr = Wire(Vec(maxcopynum,UInt(xLen.W)))
  (0 until(maxcopynum)).foreach(x => { // assume the data stored in memory continuously
    addr(x) := Mux(x < copynum, reg_save.in1(0) + srcsize * x, 0.U(xLen.W))
  })
  val addr_wire=Wire(UInt(xLen.W))
  addr_wire:=addr(PriorityEncoder(reg_save.mask.asUInt))
  val tag = Mux(reg_save.mask.asUInt =/=0.U, addr_wire(xLen-1, xLen-1-dcache_TagBits+1), 0.U(dcache_TagBits.W))
  val setIdx = Mux(reg_save.mask.asUInt=/=0.U, addr_wire(xLen-1-dcache_TagBits, xLen-1-dcache_TagBits-dcache_SetIdxBits+1), 0.U(dcache_SetIdxBits.W))
  val blockOffset = Wire(Vec(maxcopynum, UInt(dcache_BlockOffsetBits.W)))
  (0 until maxcopynum).foreach( x => blockOffset(x) := addr(x)(10, 2) )
  val wordOffset1H = Wire(Vec(maxcopynum, UInt(BytesOfWord.W)))
  (0 until maxcopynum).foreach( x => {
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
  io.to_mshr.bits.tag.tag := tag
  io.to_mshr.bits.tag.line_mask := line_mask
  io.to_mshr.bits.tag.tag_mask := tag_mask
  io.to_mshr.bits.tag.reg_idxw := reg_save.ctrl.reg_idxw
  io.to_mshr.bits.tag.warp_id := reg_save.ctrl.wid
  io.to_mshr.bits.tag.wxd:=reg_save.ctrl.wxd
  io.to_mshr.bits.tag.wfd:=reg_save.ctrl.wvd
//  io.to_mshr.bits.tag.isvec := reg_save.ctrl.isvec
//  io.to_mshr.bits.tag.unsigned := reg_save.ctrl.mem_unsigned
  io.to_mshr.bits.tag.wordOffset1H := wordOffset1H
  io.to_mshr.valid := state===s_save & (reg_save.ctrl.mem_cmd.orR)
  io.to_mshr.bits.tag.isWrite := 0.U
  io.to_l2cache.bits.opcode := 4.U
  io.to_l2cache.bits.size   := 0.U
  io.to_l2cache.bits.source :=  Cat(tag, reg_entryID)
  // temporarily set it as reg_entryID, for mshr; cat with wid, for shiftboard; jingguo ceshi hui fangzai hou 6 bit
  io.to_l2cache.bits.address := Cat(addr_wire(xLen-1, xLen-1-dcache_TagBits-dcache_SetIdxBits+1),0.U((xLen - dcache_TagBits-dcache_SetIdxBits).W))
  io.to_l2cache.bits.mask   := 0.U
  io.to_l2cache.bits.data   :=  0.U
  io.to_l2cache.bits.param  :=  0.U
  io.to_l2cache.valid := (state===s_l2cache)
  val mask_next = Wire(Vec(maxcopynum, Bool()))

  (0 until maxcopynum).foreach( x => {                          // update mask
    mask_next(x) := reg_save.mask(x) && !(addr(x)(xLen-1, xLen-1-dcache_TagBits+1)===tag && addr(x)(xLen-1-dcache_TagBits, xLen-1-dcache_TagBits-dcache_SetIdxBits+1)===setIdx
    )})
  // deal with addr, create signal send to mshr
  val tag_mask = Wire(Vec(maxcopynum, UInt(log2Up(maxcopynum).W)))
  val tag_table = Wire(Vec(maxcopynum,UInt()))
  val tagCount = UInt(log2Up(maxcopynum).W)
  (0 until(maxcopynum)).foreach( x=>{
    when(x < copynum){
      when(x.U === 0.U){
        tagCount := 1.U
      }.otherwise{
        val currentTag = addr(x) >> (32 - l2cachetagbits)
        val previousTag = addr(x - 1) >> (32 - l2cachetagbits)
        when((currentTag == previousTag).asBool){
          tagCount := tag_mask(x-1)
        }.otherwise{
          tagCount:= tag_mask(x-1) + 1.U
        }
      }
      tag_mask(x) := tagCount
    }.otherwise{
      tag_mask(x) := 0.U
    }
  })
  val addr_init = UInt((xLen - l2cachetagbits).W)
  val addr_last = UInt((xLen - l2cachetagbits).W)
  val last_index = xLen.asUInt -PriorityEncoder(tag_mask.reverse)
  val init_index = PriorityEncoder(tag_mask)
  addr_init := addr(init_index)(xLen - 1 - l2cachetagbits,0)
  addr_last := addr(last_index)(xLen - 1 - l2cachetagbits,0)
  val line_mask = Wire(Vec(l2cacheline/ tma_aligned, Bool()))
  (0 until(l2cacheline/ tma_aligned - 1)).foreach( x=>{
    line_mask(x) := Mux((((addr_init >= x *tma_aligned .asUInt)&&(addr_init < (x + 1) *tma_aligned.asUInt))||((addr_last >= x *tma_aligned .asUInt)&&(addr_last < (x + 1) *tma_aligned .asUInt))),true,false)
  })
//  val index_to_mshr = RegInit(Vec(maxcopynum,0.U(log2Up(maxcopynum).W)))
//  val addr_init_last_to_mshr = RegInit(Vec(maxcopynum * 2,0.U(log2Up(maxcopynum).W)))
  // End of Addr Logic


  io.from_fifo.ready := state === s_idle

  // FSM State Transfer
  switch(state){
    is (s_idle){
      when(io.from_fifo.valid){ state := s_save }.otherwise{ state := state }
      cnt.reset()
    }
    is (s_save){
      when(reg_save.ctrl.mem_cmd.orR){ // read or write
          when(io.to_mshr.fire){state := s_l2cache}.otherwise{state := s_save}
      }.otherwise{ state := s_idle }
    }
    is (s_l2cache){
      when(io.to_l2cache.fire){
        when (cnt.value >= copynum || mask_next.asUInt===0.U){
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
        reg_save := io.from_fifo.bits   // save data
        (0 until maxcopynum).foreach(x => {
          reg_save.mask := Mux(x < copynum, true, false)  //initialize the first copynum bit to 1
        })
      }.otherwise{reg_save := RegInit(0.U.asTypeOf(new vExeData))}
    }
    is (s_save){
      when(reg_save.ctrl.mem_cmd.orR){//===1.U){  // read
        when(io.to_mshr.fire){reg_entryID := io.idx_entry}  // get entryID from MSHR
      }
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
class AddrCalc_shared() extends Module{
  val io = IO(new Bundle{
    val from_temp = Flipped(DecoupledIO(new vExeDataTMA))
    //    val csr_wid = Output(UInt(depth_warp.W))
    //    val csr_pds = Input(UInt(xLen.W))
    //    val csr_numw = Input(UInt(xLen.W))
    //    val csr_tid = Input(UInt(xLen.W))
    val to_mshr = DecoupledIO(new Bundle{
      val tag = new MshrTag
    })
    val idx_entry = Input(UInt(log2Up(tma_nMshrEntry).W))
    //    val to_l2cache = DecoupledIO(new DCacheCoreReq_np)
    val to_shared = DecoupledIO(new ShareMemCoreReq_np)
  })
  val s_idle :: s_save :: s_shared :: Nil = Enum(3)
  val state = RegInit(init = s_idle)
  val reg_save = Reg(new vExeData)
  val srcsize = WireInit(UInt(4.W)) // define srcsize based on MEM_WHB
  val copynum = reg_save.ctrl.imm_ext / srcsize
  val cnt = new Counter(n = (maxcopynum))
  val reg_entryID = RegInit(0.U(log2Up(tma_nMshrEntry).W))
  val addr = Wire(Vec(maxcopynum,UInt(xLen.W)))
  (0 until(maxcopynum)).foreach(x => { // assume the data stored in memory continuously
    addr(x) := Mux(x < copynum, reg_save.in1(0) + srcsize * x, 0.U(xLen.W))
  })
  val addr_wire=Wire(UInt(xLen.W))
  addr_wire:=addr(PriorityEncoder(reg_save.mask.asUInt))
  val tag = Mux(reg_save.mask.asUInt=/=0.U, addr_wire(xLen-1, xLen-1-dcache_TagBits+1), 0.U(dcache_TagBits.W))
  val setIdx = Mux(reg_save.mask.asUInt=/=0.U, addr_wire(xLen-1-dcache_TagBits, xLen-1-dcache_TagBits-dcache_SetIdxBits+1), 0.U(dcache_SetIdxBits.W))
  val blockOffset = Wire(Vec(maxcopynum, UInt(dcache_BlockOffsetBits.W)))
  (0 until maxcopynum).foreach( x => blockOffset(x) := addr(x)(10, 2) )
  val wordOffset1H = Wire(Vec(maxcopynum, UInt(BytesOfWord.W)))
  (0 until maxcopynum).foreach( x => {
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
  switch(reg_save.ctrl.mem_whb){
    is(MEM_W){
      srcsize :=  4.U
    }
    is(MEM_H){
      srcsize :=  2.U
    }
    is(MEM_B){
      srcsize := 1.U
    }
  }

  io.to_mshr.bits.tag.mask := reg_save.mask
  io.to_mshr.bits.tag.reg_idxw := reg_save.ctrl.reg_idxw
  io.to_mshr.bits.tag.warp_id := reg_save.ctrl.wid
  io.to_mshr.bits.tag.wxd:=reg_save.ctrl.wxd
  io.to_mshr.bits.tag.wfd:=reg_save.ctrl.wvd
  io.to_mshr.bits.tag.isvec := reg_save.ctrl.isvec
  io.to_mshr.bits.tag.unsigned := reg_save.ctrl.mem_unsigned
  io.to_mshr.bits.tag.wordOffset1H := wordOffset1H
  io.to_mshr.valid := state===s_save & (reg_save.ctrl.mem_cmd.orR)
  io.to_mshr.bits.tag.isWrite := 0.U
  io.to_shared.bits.instrId := reg_entryID
  // |reg_save| -> |addr & mask| -> |PriorityEncoder| -> |tag & idx| -> |io.to_dcache.bits|
  //io.to_shared.bits.tag := tag
  io.to_shared.bits.setIdx := setIdx
  (0 until num_thread).foreach(x => {
    io.to_shared.bits.perLaneAddr(x).blockOffset := blockOffset(x)
    io.to_shared.bits.perLaneAddr(x).wordOffset1H := wordOffset1H(x)
    io.to_shared.bits.perLaneAddr(x).activeMask := reg_save.mask(x) && (addr(x)(xLen-1, xLen-1-dcache_TagBits+1)===tag && addr(x)(xLen-1-dcache_TagBits, xLen-1-dcache_TagBits-dcache_SetIdxBits+1)===setIdx)
  })
  io.to_shared.bits.data := io.from_temp.bits.data
  io.to_shared.bits.isWrite := reg_save.ctrl.mem_cmd(1)
  io.to_shared.valid := state===s_shared
  val mask_next = Wire(Vec(maxcopynum, Bool()))

  (0 until maxcopynum).foreach( x => {                          // update mask
    mask_next(x) := reg_save.mask(x) && !(addr(x)(xLen-1, xLen-1-dcache_TagBits+1)===tag && addr(x)(xLen-1-dcache_TagBits, xLen-1-dcache_TagBits-dcache_SetIdxBits+1)===setIdx
      )})
  // End of Addr Logic


  io.from_temp.ready := state === s_idle

  // FSM State Transfer
  switch(state){
    is (s_idle){
      when(io.from_temp.valid){ state := s_save }.otherwise{ state := state }
      cnt.reset()
    }
    is (s_save){
      when(reg_save.ctrl.mem_cmd.orR){ // read or write
        when(io.to_mshr.fire){state := s_shared}.otherwise{state := s_save}
      }.otherwise{ state := s_idle }
    }
    is (s_shared){
      when(io.to_shared.fire){
        when (cnt.value >= copynum || mask_next.asUInt===0.U){
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
      when(io.from_temp.fire){ // Next: s_save
        reg_save := io.from_temp.bits
        reg_save := io.from_temp.bits   // save data
        (0 until maxcopynum).foreach(x => {
          reg_save.mask := Mux(x < copynum, true, false)  //initialize the first copynum bit to 1
        })
      }.otherwise{reg_save := RegInit(0.U.asTypeOf(new vExeData))}
    }
    is (s_save){
      when(reg_save.ctrl.mem_cmd.orR){//===1.U){  // read
        when(io.to_mshr.fire){reg_entryID := io.idx_entry}  // get entryID from MSHR
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

class MSHRv2TMA extends Module{
  val io = IO(new Bundle{
    val from_addr = Flipped(DecoupledIO(new Bundle{
      val tag = Input(new MshrTag)
    }))
    val idx_entry = Output(UInt(log2Up(lsu_nMshrEntry).W))
    val from_l2cache = Flipped(DecoupledIO(new TLBundleD_lite_plus(l2cache_params)))
    val to_pipe = DecoupledIO(new MSHROutput)
  })

  val data = Mem(lsu_nMshrEntry, Vec(maxcopynum, UInt(xLen.W)))
  val tag = Mem(lsu_nMshrEntry, UInt((io.from_addr.bits.tag.getWidth).W))
  //val targetMask = RegInit(VecInit(Seq.fill(lsu_nMshrEntry)(0.U(num_thread.W))))
  val currentMask = RegInit(VecInit(Seq.fill(lsu_nMshrEntry)(0.U(num_thread.W)))) // 0: complete
//  val inv_activeMask = VecInit(io.from_l2cache.bits.activeMask.map(!_)).asUInt
  val used = RegInit(0.U(lsu_nMshrEntry.W))
  val complete = VecInit(currentMask.map{_===0.U}).asUInt & used
  val output_entry = Mux(complete.orR, PriorityEncoder(complete), 0.U)
  val valid_entry = Mux(used.andR, 0.U, PriorityEncoder(~used))
  val reg_req = Reg(new MshrTag)

  val s_idle :: s_add  :: s_out :: Nil = Enum(3)
  val state = RegInit(s_idle)

  io.from_l2cache.ready := state===s_idle// && used.orR
  io.from_addr.ready := state===s_idle && !(used.andR)
  io.idx_entry := Mux(io.from_addr.fire, valid_entry, 0.U) // return the MSHR entry

  when(state===s_idle){
    when(io.from_l2cache.fire){
      when(io.from_addr.fire){
        state := s_add
      }.elsewhen(io.to_pipe.ready && currentMask(io.from_l2cache.bits.source(log2Up(tma_nMshrEntry)-1.U,0))===io.from_l2cache.bits.activeMask.asUInt){
        state := s_out
      }.otherwise{state := s_idle}
    }.elsewhen(complete.orR&&io.to_pipe.ready){
      state := s_out
    }.otherwise{
      state := s_idle
    }
  }.elsewhen(state===s_out){
    when(io.to_pipe.ready && complete.bitSet(valid_entry, false.B).orR){state:=s_out}.otherwise{state:=s_idle}
  }.elsewhen(state===s_add){
    when(io.to_pipe.ready && complete.orR){state:=s_out}.otherwise{state:=s_idle}
  }.otherwise{state:=s_idle}

  switch(state){
    is(s_idle){
      when(io.from_l2cache.fire){ // deal with update request immediately
        data.write(io.from_l2cache.bits.instrId, io.from_l2cache.bits.data, io.from_l2cache.bits.activeMask) // data update
        currentMask(io.from_l2cache.bits.instrId) := currentMask(io.from_l2cache.bits.instrId) & inv_activeMask // mask update
        when(io.from_addr.fire){reg_req := io.from_addr.bits.tag} // both input valid: save the add request, and deal with it in the next cycle
      }.elsewhen(io.from_addr.fire){// deal with add request immediately
        used := used.bitSet(valid_entry, true.B) // set MSHR entry used
        tag.write(valid_entry,
          //Cat(io.from_addr.bits.tag.warp_id, io.from_addr.bits.tag.reg_idxw, io.from_addr.bits.tag.mask.asUInt)
          io.from_addr.bits.tag.asUInt
        )
        data.write(valid_entry, VecInit(Seq.fill(num_thread)(0.U)))    // data initialize
        currentMask(valid_entry) := io.from_addr.bits.tag.mask.asUInt()   // mask initialize
      }
    }
    is(s_add){
      used := used.bitSet(valid_entry, true.B)
      tag.write(valid_entry,
        //Cat(reg_req.warp_id, reg_req.reg_idxw, reg_req.mask.asUInt)
        io.from_addr.bits.tag.asUInt
      )
      data.write(valid_entry, VecInit(Seq.fill(num_thread)(0.U)))
      currentMask(valid_entry) := reg_req.mask.asUInt
    }
    is(s_out){ // release MSHR line
      when(io.to_pipe.fire){used := used.bitSet(output_entry, false.B)}
    }
  }
  val output_tag = tag.read(output_entry).asTypeOf(new MshrTag)
  val raw_data = data.read(output_entry)
  val output_data = Wire(Vec(num_thread, UInt(xLen.W)))
  (0 until num_thread).foreach{ x =>
    output_data(x) := Mux(output_tag.mask(x),
      ByteExtract(output_tag.unsigned, raw_data(x), output_tag.wordOffset1H(x)),
      0.U(xLen.W)
    )
  }
  io.to_pipe.valid := complete.orR && state===s_out
  io.to_pipe.bits.tag := output_tag.asTypeOf(new MshrTag)
  io.to_pipe.bits.data := output_data
}

class TMA() extends Module{
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
    val dcache_req = Decoupled( new TLBundleA_lite(l2cache_params))
    val shared_req = DecoupledIO(new ShareMemCoreReq_np())
    val fence_end_dcache = Output(UInt(num_warp.W)) //todo pass to barrier to fence
//    val fence_end_shared = Output(UInt(num_warp.W)) //todo numwarp pass to barrier to fence
    // about csr, maybe no use
//    val csr_wid = Output(UInt(depth_warp.W))
//    val csr_pds = Input(UInt(xLen.W))
//    val csr_numw = Input(UInt(xLen.W))
//    val csr_tid = Input(UInt(xLen.W))
  })
  /*
  * design: like lsu:
  * 1. InputFIFO: collect outer req, and send it to AddrCalculator
  * 2. AddrCalc_l2cache: use in1 ,copysize and srcsize to Calculate the addr in L1cache
  * 3. AddrCalc_shared: use in2 ,copysize and srcsize to Calculate the addr in Sharedmem
  * 4. MSHR_dcache: accept tag from AddrCalc_fetch and data from Arbiter. send req to L1cache
  * 5. MSHR_shared: accept tag from AddrCalc_store and data from Arbiter, sent req to Sharedmem
  * 5. ShiftBoard_dcache: shiftright or shiftleft based on dcache_req.fire and dcache_rsp.fire. if all warps' req are responsed, set the fence_end(i) 1 accordingly
  *                define in LSU.scala
  * 6. Temp_Data: store instr info and Dcache data accordingly
  *
  * */
  val tma_req_transfer = new vExeData
  val InputFIFO = Module(new Queue(new vExeData, entries = 1, pipe = true))
  val AddrCalc_l2cache = Module(new AddrCalc_l2cache())
  val AddrCalc_shared = Module(new AddrCalc_shared())
  val MSHR_dcache = Module(new MSHRv2TMA)
  val MSHR_shared = Module(new MSHRv2TMA)
  val ShiftBoard_dcache = VecInit(Seq.fill(num_warp)(Module(new ShiftBoard(lsu_num_entry_each_warp)).io))
//  val ShiftBoard_shared = VecInit(Seq.fill(num_warp)(Module(new ShiftBoard(lsu_num_entry_each_warp)).io))
  val InstStore = Mem(num_warp, new vExeData())
  val TempData = RegInit(VecInit(Seq.fill(maxcopysize)(0.U(xLen.W))))

  //  //InputFIFO's io
  //  InputFIFO.io.enq <> io.tma_req
  //  InputFIFO.io.enq.bits.src_global := io.tma_req.bits.in1(0)
  //  InputFIFO.io.enq.bits.dst_shared := io.tma_req.bits.in2(0)
  //  InputFIFO.io.enq.bits.in3 := io.tma_req.bits.in3(0) // no use
  //  InputFIFO.io.enq.bits.copysize := io.tma_req.bits.ctrl.imm_ext(0)
  ////  InputFIFO.io.enq.bits.srcsize := io.tma_req.bits.ctrl.sel_imm(0)
  //  io.tma_req.ready := InputFIFO.io.enq.ready(0)

  InputFIFO.io.enq.valid := Mux(ShiftBoard_dcache(io.tma_req.bits.ctrl.wid).full,false.B,io.tma_req.valid)

  //AddrCalc_l2cache AddrCalc_shared
  AddrCalc_l2cache.io.from_fifo <> InputFIFO.io.deq
  io.dcache_req <> AddrCalc_l2cache.io.to_l2cache
  AddrCalc_shared.io.from_temp = TempData.read()
  io.shared_req <> AddrCalc_shared.io.to_shared

  //MSHR_dcache MSHR_shared
    MSHR_dcache.io.from_l2cache <> io.l2cache_rsp
    MSHR_dcache.io.from_addr <> AddrCalc_l2cache.io.to_mshr
    AddrCalc_l2cache.io.idx_entry := MSHR_dcache.io.idx_entry
    io.shared_req <> MSHR_dcache.io.mshr2shared
  //
    MSHR_shared.io.from_l2cache <> io.shared_rsp
    MSHR_shared.io.from_addr <> AddrCalc_shared.io.to_mshr
    AddrCalc_shared.io.idx_entry := MSHR_shared.io.idx_entry
    io.shared_req <> MSHR_shared.io.mshr2shared

  //shiftboard
    ShiftBoard_dcache.zipWithIndex.foreach{case(a,b)=> {
    a.left:=io.tma_req.fire & io.tma_req.bits.ctrl.wid===b.asUInt
    a.right:=io.l2cache_rsp.fire & io.l2cache_rsp.bits.tag.warp_id===b.asUInt
  }}
//  ShiftBoard_shared.zipWithIndex.foreach{case(a,b)=> {
//    a.left:=io.shared_req.fire & io.shared_req.bits.ctrl.wid===b.asUInt
//    a.right:=io.shared_rsp.fire & io.shared_rsp.bits.tag.warp_id===b.asUInt
//  }}

  io.fence_end_dcache:=VecInit(ShiftBoard_dcache.map(x=>x.empty)).asUInt
//  io.fence_end_shared:=VecInit(ShiftBoard_shared.map(x=>x.empty)).asUInt

  io.tma_req.ready:=Mux(ShiftBoard_dcache(io.tma_req.bits.ctrl.wid).full,false.B,InputFIFO.io.enq.ready)

//  io.csr_wid:=AddrCalcTMA.io.csr_wid
//  AddrCalcTMA.io.csr_tid:=io.csr_tid
//  AddrCalcTMA.io.csr_pds:=io.csr_pds
//  AddrCalcTMA.io.csr_numw:=io.csr_numw
}
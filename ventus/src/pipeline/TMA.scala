package pipeline

import top.parameters._
import chisel3._
import chisel3.util._
import IDecode._
/*
* TMA
* usage: asynchronously copy data from Dcache(L1 cache)
* to sharedmem
* assume all thread in a warp carry the same instruction: cp.async, s
* */
var maxcopysize = 128 //bytes
var maxsrcsize = 4 //bytes
var minsrcsize = 1 //bytes
var addr_size_per_num = 4//bytes
def maxcopynum: Int = maxcopysize / minsrcsize



// upon this line, there is the new parameter definition

class AddrCalc_dcache() extends Module{
  val io = IO(new Bundle{
    val from_fifo = Flipped(DecoupledIO(new vExeData))
    //    val csr_wid = Output(UInt(depth_warp.W))
    //    val csr_pds = Input(UInt(xLen.W))
    //    val csr_numw = Input(UInt(xLen.W))
    //    val csr_tid = Input(UInt(xLen.W))
    val to_mshr = DecoupledIO(new Bundle{
      val tag = new MshrTag
    })
    val idx_entry = Input(UInt(log2Up(lsu_nMshrEntry).W))
    val to_dcache = DecoupledIO(new DCacheCoreReq_np)
    //    val to_shared = DecoupledIO(new ShareMemCoreReq_np)
  })
  //  vExeData.in1: Dcache base addr
  //  vExeData.in2: Sharedmem base addr
  //  vExeData.ctrl.imm_ext: copysize
  //  maybe 4*4*2 byte one cp.async
  //  vExeData.ctrl.memw/h/b: data type : determine the mask sent to L1 cache
  val s_idle :: s_save :: s_dcache :: Nil = Enum(3)
  val state = RegInit(init = s_idle)
  val reg_save = Reg(new vExeData)
  val srcsize = WireInit(UInt(4.W)) // define srcsize based on MEM_WHB
  val copynum = reg_save.ctrl.imm_ext / srcsize
  val cnt = new Counter(n = (copynum).asUInt)
  val reg_entryID = RegInit(0.U(log2Up(lsu_nMshrEntry).W))
  val addr = Wire(Vec(maxcopynum,UInt(xLen.W)))
  (0 until(maxcopynum)).foreach(x => {
    addr(x) := Mux(x <= copynum, reg_save.ctrl.imm_ext + addr_size_per_num * x,0.U(xLen.W))
  })
  val tag = Wire(Vec(maxcopynum, UInt(dcache_BlockOffsetBits.W)))
  (0 until maxcopynum).foreach( x => tag(x) := addr(x)(xLen-1, xLen-1-dcache_TagBits+1))
  val setIdx = Wire(Vec(maxcopynum, UInt(dcache_SetIdxBits.W)))
  (0 until(maxcopysize)).foreach(x => setIdx(x) := addr(x)(xLen-1-dcache_TagBits, xLen-1-dcache_TagBits-dcache_SetIdxBits+1))
  val same_tag = Wire(Vec(maxcopynum, Bool()))
  (0 until maxcopynum).foreach( x =>
    same_tag(x) := Mux(x <= copynum,  addr(x)(xLen-1, xLen-1-dcache_TagBits-dcache_SetIdxBits+1) === Cat(tag, setIdx),false.B )
  ) //todo: should do with zero in tag and addr later
  val blockOffset = Wire(Vec(maxcopynum, UInt(dcache_BlockOffsetBits.W)))
  (0 until maxcopynum).foreach( x => blockOffset(x) := addr(x)(10, 2) )
  val wordOffset1H = Wire(Vec(maxcopynum, UInt(BytesOfWord.W)))
  (0 until maxcopynum).foreach( x => {
    //DONE: Add Control Signals in vExeData.ctrl and define lw lh lb
    wordOffset1H(x) := 0.U(4.W)
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
  srcsize := switch(reg_save.ctrl.mem_whb){
    is(MEM_W){
      4.U
    }
    is(MEM_H){
      2.U
    }
    is(MEM_B){
      1.U
    }
  }

  // todo: just copy ,you should verify and change below, based on your understanding of MSHR
  io.to_mshr.bits.tag.mask := reg_save.mask
  io.to_mshr.bits.tag.reg_idxw := reg_save.ctrl.reg_idxw
  io.to_mshr.bits.tag.warp_id := reg_save.ctrl.wid
  io.to_mshr.bits.tag.wxd:=reg_save.ctrl.wxd
  io.to_mshr.bits.tag.wfd:=reg_save.ctrl.wvd
  io.to_mshr.bits.tag.isvec := reg_save.ctrl.isvec
  io.to_mshr.bits.tag.unsigned := reg_save.ctrl.mem_unsigned
  io.to_mshr.bits.tag.wordOffset1H := wordOffset1H
  io.to_mshr.valid := state===s_save & (reg_save.ctrl.mem_cmd.orR)
  io.to_mshr.bits.tag.isWrite := reg_save.ctrl.mem_cmd(1)



  io.from_fifo.ready := state === s_idle

  // FSM State Transfer
  switch(state){
    is(s_idle){
      when(io.from_fifo.valid){state := s_save}.otherwise{state := state}
      cnt.reset()
    }
    is(s_save){
      when(reg_save.ctrl.mem_cmd.orR){ // read or write
        when(io.to_mshr.fire()){state := s_dcache}.otherwise{state := s_save}
      }.otherwise{ state := s_idle }
    }
    is(s_dcache){
      when(io.to_dcache.fire()){
        when(cnt.value >= copynum){
          cnt.reset()
          state := s_idle
        }.otherwise{
          cnt.inc()
          state := s_dcache
        }
      }.otherwise{
        state := s_dcache
      }
    }
  }
  // FSM Operation
  switch(state){
    is(s_idle){
      when(io.from_fifo.fire()){
        reg_save := io.from_fifo.bits
        //        reg_save.mask := Mux(io.from_fifo.bits.ctrl.isvec, io.from_fifo.bits.mask, VecInit((1.U(num_thread.W)).asBools))
      }.otherwise{reg_save := RegInit(0.U.asTypeOf(new vExeData))}
    }
    is(s_save){
      when(reg_save.ctrl.mem_cmd.orR){
        when(io.to_mshr.fire()){reg_entryID := io.idx_entry}
      }
    }
    is(s_dcache){
      //      when(io.to_dcache.fire()){                                      // request is sent
      //        reg_save.mask := mask_next
      //      }.otherwise{
      //        reg_save.mask := reg_save.mask
    }
  }
}

}
class AddrCalc_shared() extends Module{
  val io = IO(new Bundle{
    val from_fifo = Flipped(DecoupledIO(new vExeData))
    //    val csr_wid = Output(UInt(depth_warp.W))
    //    val csr_pds = Input(UInt(xLen.W))
    //    val csr_numw = Input(UInt(xLen.W))
    //    val csr_tid = Input(UInt(xLen.W))
    val to_mshr = DecoupledIO(new Bundle{
      val tag = new MshrTag
    })
    val idx_entry = Input(UInt(log2Up(lsu_nMshrEntry).W))
    //    val to_dcache = DecoupledIO(new DCacheCoreReq_np)
    val to_shared = DecoupledIO(new ShareMemCoreReq_np)
  })
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
    val dcache_rsp = Flipped(DecoupledIO(new DCacheCoreRsp_np()))
    val shared_rsp = Flipped(DecoupledIO(new ShareMemCoreRsp_np()))
    //output
    val dcache_req = DecoupledIO(new DCacheCoreReq_np())
    val shared_req = DecoupledIO(new ShareMemCoreReq_np())
    val fence_end_dcache = Output(UInt(num_warp.W)) //todo pass to barrier to fence
    val fence_end_shared = Output(UInt(num_warp.W)) //todo numwarp pass to barrier to fence
    // about csr, maybe no use
    val csr_wid = Output(UInt(depth_warp.W))
    val csr_pds = Input(UInt(xLen.W))
    val csr_numw = Input(UInt(xLen.W))
    val csr_tid = Input(UInt(xLen.W))
  })
  /*
  * design: like lsu:
  * 1. InputFIFO: collect outer req, and send it to AddrCalculator
  * 2. AddrCalc_dcache: use in1 ,copysize and srcsize to Calculate the addr in L1cache
  * 3. AddrCalc_shared: use in2 ,copysize and srcsize to Calculate the addr in Sharedmem
  * 4. MSHR_dcache: accept tag from AddrCalc_fetch and data from Arbiter. send req to L1cache
  * 5. MSHR_shared: accept tag from AddrCalc_store and data from Arbiter, sent req to Sharedmem
  * 5. ShiftBoard_dcache: shiftright or shiftleft based on dcache_req.fire and dcache_rsp.fire. if all warps' req are responsed, set the fence_end(i) 1 accordingly
  *                define in LSU.scala
  * 6. ShiftBoard_shared: shiftright or shiftleft based on shared_req.fire and shared_rsp.fire. if all warps' req are responsed, set the fence_end(i) 1 accordingly
  *                define in LSU.scala
  *
  * */
  val tma_req_transfer = new vExeData
  val InputFIFO = Module(new Queue(new vExeData, entries = 1, pipe = true))
  val AddrCalc_dcache = Module(new AddrCalc_dcache())
  val AddrCalc_shared = Module(new AddrCalc_shared())
  val MSHR_dcache = Module(new MSHRv2)
  val MSHR_shared = Module(new MSHRv2)
  val ShiftBoard_dcache = VecInit(Seq.fill(num_warp)(Module(new ShiftBoard(lsu_num_entry_each_warp)).io))
  val ShiftBoard_shared = VecInit(Seq.fill(num_warp)(Module(new ShiftBoard(lsu_num_entry_each_warp)).io))

  //  //InputFIFO's io
  //  InputFIFO.io.enq <> io.tma_req
  //  InputFIFO.io.enq.bits.src_global := io.tma_req.bits.in1(0)
  //  InputFIFO.io.enq.bits.dst_shared := io.tma_req.bits.in2(0)
  //  InputFIFO.io.enq.bits.in3 := io.tma_req.bits.in3(0) // no use
  //  InputFIFO.io.enq.bits.copysize := io.tma_req.bits.ctrl.imm_ext(0)
  ////  InputFIFO.io.enq.bits.srcsize := io.tma_req.bits.ctrl.sel_imm(0)
  //  io.tma_req.ready := InputFIFO.io.enq.ready(0)

  InputFIFO.io.enq.valid := Mux(ShiftBoard_dcache(io.tma_req.bits.ctrl.wid).full,false.B,io.tma_req.valid) //todo add not full of shiftboard_shared

  //AddrCalc_dcache AddrCalc_shared
  AddrCalc_dcache.io.from_fifo <> InputFIFO.io.deq
  io.dcache_req <> AddrCalc_dcache.io.to_dcache
  AddrCalc_shared.io.from_fifo <> InputFIFO.io.deq
  io.shared_req <> AddrCalc_shared.io.to_shared

  //MSHR_dcache MSHR_shared
  //  MSHR_dcache.io.from_dcache <> io.dcache_rsp
  //  MSHR_dcache.io.from_addr <> AddrCalc_dcache.io.to_mshr
  //  AddrCalc_dcache.io.idx_entry := MSHR_dcache.io.idx_entry
  //  io.shared_req <> MSHR_dcache.io.to_pipe
  //
  //  MSHR_shared.io.from_shared <> io.shared_rsp
  //  MSHR_shared.io.from_addr <> AddrCalc_shared.io.to_mshr
  //  AddrCalc_shared.io.idx_entry := MSHR_shared.io.idx_entry
  //  io.shared_req <> MSHR_shared.io.to_pipe

  //shiftboard
  shiftBoard.zipWithIndex.foreach{case(a,b)=> {
    a.left:=io.tma_req.fire & io.tma_req.bits.ctrl.wid===b.asUInt
    a.right:=io.shared_req.fire & io.shared_req.bits.tag.warp_id===b.asUInt
  }}
  io.fence_end:=VecInit(shiftBoard.map(x=>x.empty)).asUInt()
  io.tma_req.ready:=Mux(shiftBoard(io.tma_req.bits.ctrl.wid).full,false.B,InputFIFO.io.enq.ready)

  io.csr_wid:=AddrCalcTMA.io.csr_wid
  AddrCalcTMA.io.csr_tid:=io.csr_tid
  AddrCalcTMA.io.csr_pds:=io.csr_pds
  AddrCalcTMA.io.csr_numw:=io.csr_numw
}
/*
 * Copyright (c) 2021-2022 International Innovation Center of Tsinghua University, Shanghai
 * Ventus is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details. */
package pipeline

import top.parameters._
import chisel3._
import chisel3.util._
import IDecode._

class toShared extends Bundle{
  val instrId = UInt(log2Up(lsu_nMshrEntry).W)
  val addr = Vec(num_thread, UInt(xLen.W))
  val data = Vec(num_thread, UInt(xLen.W))
  val ctrl = new Bundle{
    val mem_cmd = UInt(2.W)
    val mop = UInt(2.W)
    val isvec = Bool()
  }
  val mask = Vec(num_thread, Bool())
}
class DCachePerLaneAddr extends Bundle{
  val activeMask = Bool()
  val blockOffset = UInt(dcache_BlockOffsetBits.W)
  val wordOffset1H = UInt(BytesOfWord.W)
}

class DCacheCoreReq_np extends Bundle{
  val instrId = UInt(log2Up(lsu_nMshrEntry).W)
  val isWrite = Bool()
  val tag = UInt(dcache_TagBits.W)
  val setIdx = UInt(dcache_SetIdxBits.W)
  val perLaneAddr = Vec(num_thread, new DCachePerLaneAddr)
  val data = Vec(num_thread, UInt(xLen.W))
}

class DCacheCoreRsp_np extends Bundle{
  val instrId = UInt(log2Up(lsu_nMshrEntry).W)
  val data = Vec(num_thread, UInt(xLen.W))
//  val ctrl = new Bundle{
//    val mem_cmd = UInt(2.W)
//    val mop = UInt(2.W)
//  }
  val activeMask = Vec(num_thread, Bool())
  val isWrite = Bool()
}

class ShareMemPerLaneAddr_np extends Bundle{
  val activeMask = Bool()
  val blockOffset = UInt(dcache_BlockOffsetBits.W)
  val wordOffset1H = UInt(BytesOfWord.W)
}
class ShareMemCoreReq_np extends Bundle{
  //val ctrlAddr = new Bundle{
  val instrId = UInt(log2Up(lsu_nMshrEntry).W)
  val isWrite = Bool()//Vec(NLanes, Bool())
  //val tag = UInt(dcache_TagBits.W)
  val setIdx = UInt(dcache_SetIdxBits.W)
  val perLaneAddr = Vec(num_thread, new ShareMemPerLaneAddr_np)
  val data = Vec(num_thread, UInt(xLen.W))
}

class ShareMemCoreRsp_np extends Bundle{
  val instrId = UInt(log2Up(lsu_nMshrEntry).W)
  val data = Vec(num_thread, UInt(xLen.W))
  val activeMask = Vec(num_thread, Bool())//UInt(NLanes.W)
}

class MshrTag extends Bundle{  // AddrCalculate向MSHR添加记录并获取Tag的接口
  val warp_id = UInt(depth_warp.W)
  val wfd = Bool()
  val wxd = Bool()
  val reg_idxw = UInt(5.W)
  val mask = Vec(num_thread, Bool())
  val unsigned = Bool()
  val isvec = Bool()
  val wordOffset1H = Vec(num_thread, UInt(BytesOfWord.W))
  val isWrite = Bool()
  val spike_info = if (SPIKE_OUTPUT) Some(new InstWriteBack) else None
}

object ByteExtract{
  def apply(isUInt: Bool = true.B, in: UInt = 0.U(xLen.W), sel: UInt = "hf".U(4.W)): UInt = {
    val result = Wire(UInt(32.W))
    result := MuxCase(
      in, Array(
        (sel==="hf".U) -> in,
        (sel==="hc".U) -> Mux(!in(31)||isUInt, Cat(0.U(16.W),in(31,16)), Cat("hffff".U(16.W),in(31,16))),
        (sel==="h3".U) -> Mux(!in(15)||isUInt, Cat(0.U(16.W),in(15,0)), Cat("hffff".U(16.W),in(15,0))),
        (sel==="h8".U) -> Mux(!in(31)||isUInt, Cat(0.U(24.W),in(31,24)), Cat("hffffff".U(24.W),in(31,24))),
        (sel==="h4".U) -> Mux(!in(23)||isUInt, Cat(0.U(24.W),in(23,16)), Cat("hffffff".U(24.W),in(23,16))),
        (sel==="h2".U) -> Mux(!in(15)||isUInt, Cat(0.U(24.W),in(15,8)), Cat("hffffff".U(24.W),in(15,8))),
        (sel==="h1".U) -> Mux(!in(7)||isUInt, Cat(0.U(24.W),in(7,0)), Cat("hffffff".U(24.W),in(7,0)))
      )
    )
    result
  }
}

class AddrCalculate(val sharedmemory_addr_max: UInt = 4096.U(32.W)) extends Module{
  val io = IO(new Bundle{
    val from_fifo = Flipped(DecoupledIO(new vExeData))
    val csr_wid = Output(UInt(depth_warp.W))
    val csr_pds = Input(UInt(xLen.W))
    val csr_numw = Input(UInt(xLen.W))
    val csr_tid = Input(UInt(xLen.W))
    val to_mshr = DecoupledIO(new Bundle{
      val tag = new MshrTag
    })
    val idx_entry = Input(UInt(log2Up(lsu_nMshrEntry).W))
    val to_dcache = DecoupledIO(new DCacheCoreReq_np)
    val to_shared = DecoupledIO(new ShareMemCoreReq_np)
  })
  val s_idle :: s_save :: s_shared :: s_dcache :: Nil = Enum(4)
  val cnt = new Counter(n = num_thread)
  val state = RegInit(init = s_idle)

  val reg_save = Reg(new vExeData)
  io.csr_wid:=reg_save.ctrl.wid
  //val rdy_fromFIFO = Reg(Bool())
  io.from_fifo.ready := state===s_idle
  val reg_entryID = RegInit(0.U(log2Up(lsu_nMshrEntry).W))

  val addr = Wire(Vec(num_thread, UInt(xLen.W)))
  val is_shared = Wire(Vec(num_thread, Bool()))
  val all_shared = Wire(Bool())


  // Address Calculate & Analyze, Comb Logic @reg_save
  (0 until num_thread).foreach( x => {
    addr(x) :=  Mux(reg_save.ctrl.isvec & reg_save.ctrl.disable_mask,
                  Mux(reg_save.ctrl.is_vls12,
                    reg_save.in1(x)+reg_save.in2(x),
                    (reg_save.in1(x) + reg_save.in2(x))(1,0) + (Cat((io.csr_tid + x.asUInt),0.U(2.W) ) ) + io.csr_pds + (((Cat((reg_save.in1(x)+reg_save.in2(x))(31,2),0.U(2.W)))*io.csr_numw)<<depth_thread) 
                  ),
                  Mux(reg_save.ctrl.isvec,
                    reg_save.in1(x) + Mux(reg_save.ctrl.mop===0.U,
                      x.asUInt()<<2,
                      Mux(reg_save.ctrl.mop===3.U,
                        reg_save.in2(x),
                        x.asUInt*reg_save.in2(x))
                    ),
                    reg_save.in1(0) + reg_save.in2(0)
                  )
                )
    is_shared(x) := !reg_save.mask(x) || addr(x)<sharedmemory_addr_max
  })
  all_shared := Mux(reg_save.ctrl.isvec,
    is_shared.asUInt.andR, // "AND" Reduce
    is_shared(0)
  )
  val addr_wire=Wire(UInt(xLen.W))
  addr_wire:=addr(PriorityEncoder(reg_save.mask.asUInt))
  val tag = Mux(reg_save.mask.asUInt()=/=0.U, addr_wire(xLen-1, xLen-1-dcache_TagBits+1), 0.U(dcache_TagBits.W))
  val setIdx = Mux(reg_save.mask.asUInt()=/=0.U, addr_wire(xLen-1-dcache_TagBits, xLen-1-dcache_TagBits-dcache_SetIdxBits+1), 0.U(dcache_SetIdxBits.W))
  val same_tag = Wire(Vec(num_thread, Bool()))
    (0 until num_thread).foreach( x =>
      same_tag(x) := Mux(reg_save.mask(x), addr(x)(xLen-1, xLen-1-dcache_TagBits-dcache_SetIdxBits+1)===Cat(tag, setIdx), false.B)
    )
  val blockOffset = Wire(Vec(num_thread, UInt(dcache_BlockOffsetBits.W)))
    (0 until num_thread).foreach( x => blockOffset(x) := addr(x)(10, 2) )
  val wordOffset1H = Wire(Vec(num_thread, UInt(BytesOfWord.W)))
    (0 until num_thread).foreach( x => {
  //DONE: Add Control Signals in vExeData.ctrl and define lw lh lb
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
      wordOffset1H(x) := 15.U(4.W)
    })
  //val reg_toMSHR = Reg(new MshrTag)
  //val vld_toMSHR = Reg(Bool())
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
  if(SPIKE_OUTPUT){
    io.to_mshr.bits.tag.spike_info.get:=reg_save.ctrl.spike_info.get
  }
  //val reg_toShared = Reg(new toShared)
  //val vld_toShared = Reg(Bool())
  val data_next=reg_save.in3

  io.to_shared.bits.instrId := reg_entryID
  // |reg_save| -> |addr & mask| -> |PriorityEncoder| -> |tag & idx| -> |io.to_dcache.bits|
  //io.to_shared.bits.tag := tag
  io.to_shared.bits.setIdx := setIdx
  (0 until num_thread).foreach(x => {
    io.to_shared.bits.perLaneAddr(x).blockOffset := blockOffset(x)
    io.to_shared.bits.perLaneAddr(x).wordOffset1H := wordOffset1H(x)
    io.to_shared.bits.perLaneAddr(x).activeMask := reg_save.mask(x) && (addr(x)(xLen-1, xLen-1-dcache_TagBits+1)===tag && addr(x)(xLen-1-dcache_TagBits, xLen-1-dcache_TagBits-dcache_SetIdxBits+1)===setIdx)
  })
  io.to_shared.bits.data := data_next//Mux(reg_save.ctrl.mem_cmd(0).asBool(), VecInit(Seq.fill(num_thread)(0.U(xLen.W))), reg_save.in3)
  io.to_shared.bits.isWrite := reg_save.ctrl.mem_cmd(1)
  io.to_shared.valid := state===s_shared

  //val vld_toDCache = Reg(Bool())
  io.to_dcache.bits.instrId := reg_entryID
  // |reg_save| -> |addr & mask| -> |PriorityEncoder| -> |tag & idx| -> |io.to_dcache.bits|
  io.to_dcache.bits.tag := tag
  io.to_dcache.bits.setIdx := setIdx
  (0 until num_thread).foreach(x => {
    io.to_dcache.bits.perLaneAddr(x).blockOffset := blockOffset(x)
    io.to_dcache.bits.perLaneAddr(x).wordOffset1H := wordOffset1H(x)
    io.to_dcache.bits.perLaneAddr(x).activeMask := reg_save.mask(x) && (addr(x)(xLen-1, xLen-1-dcache_TagBits+1)===tag && addr(x)(xLen-1-dcache_TagBits, xLen-1-dcache_TagBits-dcache_SetIdxBits+1)===setIdx)
  })
  io.to_dcache.bits.data := data_next//Mux(reg_save.ctrl.mem_cmd(0).asBool(), VecInit(Seq.fill(num_thread)(0.U(xLen.W))), reg_save.in3)
  io.to_dcache.bits.isWrite := reg_save.ctrl.mem_cmd(1)
  io.to_dcache.valid := state===s_dcache
  val mask_next = Wire(Vec(num_thread, Bool()))
  (0 until num_thread).foreach( x => {                          // update mask
    mask_next(x) := reg_save.mask(x) && !(addr(x)(xLen-1, xLen-1-dcache_TagBits+1)===tag && addr(x)(xLen-1-dcache_TagBits, xLen-1-dcache_TagBits-dcache_SetIdxBits+1)===setIdx)
  })
  // End of Addr Logic

  // FSM State Transfer
  switch(state){
    is (s_idle){
      when(io.from_fifo.valid){ state := s_save }.otherwise{ state := state }
      cnt.reset()
    }
    is (s_save){
      when(reg_save.ctrl.mem_cmd.orR){ // read or write
        when(all_shared){ // shared memory
          when(io.to_mshr.fire()){state := s_shared}.otherwise{state := s_save}
        }.otherwise{ // dcache
          when(io.to_mshr.fire()){state := s_dcache}.otherwise{state := s_save}
        }
      }.otherwise{ state := s_idle }
    }
    is (s_shared){
      when(io.to_shared.fire()){
        when(cnt.value>=num_thread.U || mask_next.asUInt()===0.U){
          cnt.reset(); state := s_idle
        }.otherwise{
          cnt.inc(); state := s_shared
        }
      }.otherwise{state := s_shared}
    }
    is (s_dcache){
      when(io.to_dcache.fire()){
        when(cnt.value>=num_thread.U || mask_next.asUInt()===0.U){
          cnt.reset(); state := s_idle
        }.otherwise{
          cnt.inc(); state := s_dcache
        }
      }.otherwise{state := s_dcache}
    }
  }
  // FSM Operation
  switch(state){
    is (s_idle){
      when(io.from_fifo.fire()){ // Next: s_save
        reg_save := io.from_fifo.bits   // save data
        reg_save.mask := Mux(io.from_fifo.bits.ctrl.isvec, io.from_fifo.bits.mask, VecInit((1.U(num_thread.W)).asBools))
      }.otherwise{reg_save := RegInit(0.U.asTypeOf(new vExeData))}
    }
    is (s_save){
      when(reg_save.ctrl.mem_cmd.orR){//===1.U){  // read
        when(io.to_mshr.fire()){reg_entryID := io.idx_entry}  // get entryID from MSHR
      }
    }
    is (s_shared){
      // Maybe Nothing here :-)
      when(io.to_shared.fire()){                                      // request is sent
        reg_save.mask := mask_next
      }.otherwise{
        reg_save.mask := reg_save.mask
      }
    }
    is (s_dcache){
      when(io.to_dcache.fire()){                                      // request is sent
        reg_save.mask := mask_next
      }.otherwise{
        reg_save.mask := reg_save.mask
      }
    }
  }
}
class LSU2WB extends Module{
  val io = IO(new Bundle{
    val lsu_rsp=Flipped(DecoupledIO(new MSHROutput))
    val out_x = DecoupledIO(new WriteScalarCtrl())
    val out_v = DecoupledIO(new WriteVecCtrl)
  })
  io.out_x.bits.warp_id:=io.lsu_rsp.bits.tag.warp_id
  io.out_x.bits.reg_idxw:=io.lsu_rsp.bits.tag.reg_idxw
  io.out_x.bits.wxd:=io.lsu_rsp.bits.tag.wxd
  io.out_x.bits.wb_wxd_rd:=io.lsu_rsp.bits.data(0)
  io.out_v.bits.warp_id:=io.lsu_rsp.bits.tag.warp_id
  io.out_v.bits.reg_idxw:=io.lsu_rsp.bits.tag.reg_idxw
  io.out_v.bits.wvd:=io.lsu_rsp.bits.tag.wfd
  io.out_v.bits.wvd_mask:=io.lsu_rsp.bits.tag.mask
  io.out_v.bits.wb_wvd_rd:=io.lsu_rsp.bits.data
  if(SPIKE_OUTPUT){
    io.out_x.bits.spike_info.get:=io.lsu_rsp.bits.tag.spike_info.get
    io.out_v.bits.spike_info.get:=io.lsu_rsp.bits.tag.spike_info.get
  }
  when(io.lsu_rsp.bits.tag.wxd){
    io.out_x.valid:=io.lsu_rsp.valid
    io.out_v.valid:=false.B
    io.lsu_rsp.ready:=io.out_x.ready
  }.elsewhen(io.lsu_rsp.bits.tag.wfd){
    io.out_v.valid:=io.lsu_rsp.valid
    io.out_x.valid:=false.B
    io.lsu_rsp.ready:=io.out_v.ready
  }.otherwise({
    io.out_v.valid:=false.B
    io.out_x.valid:=false.B
    io.lsu_rsp.ready:=io.lsu_rsp.bits.tag.isWrite//true.B // CONNECTION OF io.lsu_rsp.bits.tag.isWrite
  })
}
class LSUexe() extends Module{
// default size: 128 * (num_thread=8) * (xlen/8=4) = 4KByte
  val io = IO(new Bundle{
    val lsu_req = Flipped(DecoupledIO(new vExeData()))
    val dcache_rsp = Flipped(DecoupledIO(new DCacheCoreRsp_np()))
    //val lsu_rsp = DecoupledIO(new WriteBackControl())
    val lsu_rsp = DecoupledIO(new MSHROutput)
    val dcache_req = DecoupledIO(new DCacheCoreReq_np())
    val shared_req = DecoupledIO(new ShareMemCoreReq_np())
    val shared_rsp = Flipped(DecoupledIO(new DCacheCoreRsp_np))
    val fence_end = Output(UInt(num_warp.W))

    val csr_wid = Output(UInt(depth_warp.W))
    val csr_pds = Input(UInt(xLen.W))
    val csr_numw = Input(UInt(xLen.W))
    val csr_tid = Input(UInt(xLen.W))
  })
  val sharedmemory_addr_max = sharemem_size.U(32.W)
  //val sharedmemory = Module(new SharedMemoryV2(nSharedMemoryEntry, num_thread, xLen, lsu_nMshrEntry)) // default: 128

  val InputFIFO = Module(new Queue(new vExeData, entries=1, pipe=true))
  InputFIFO.io.enq <> io.lsu_req

  val AddrCalc = Module(new AddrCalculate(sharedmemory_addr_max))
  AddrCalc.io.from_fifo <> InputFIFO.io.deq
  io.dcache_req <> AddrCalc.io.to_dcache
  io.shared_req <> AddrCalc.io.to_shared

  val rspArbiter = Module(new Arbiter(new DCacheCoreRsp_np, n = 2))
  rspArbiter.io.in(1) <> io.shared_rsp
  rspArbiter.io.in(0) <> io.dcache_rsp

  // SOME MSHR INTERFACE
  val Coalscer = Module(new MSHRv2)
  //val outputFIFO = Module(new Queue(new MSHROutput, num_warp+1, pipe=true))
  Coalscer.io.from_dcache <> rspArbiter.io.out
  Coalscer.io.from_addr <> AddrCalc.io.to_mshr
  AddrCalc.io.idx_entry:=Coalscer.io.idx_entry
  io.lsu_rsp <> Coalscer.io.to_pipe

  val shiftBoard=VecInit(Seq.fill(num_warp)(Module(new ShiftBoard(lsu_num_entry_each_warp)).io))
  shiftBoard.zipWithIndex.foreach{case(a,b)=> {
    a.left:=io.lsu_req.fire & io.lsu_req.bits.ctrl.wid===b.asUInt
    a.right:=io.lsu_rsp.fire & io.lsu_rsp.bits.tag.warp_id===b.asUInt
    }}
  io.fence_end:=VecInit(shiftBoard.map(x=>x.empty)).asUInt()
  io.lsu_req.ready:=Mux(shiftBoard(io.lsu_req.bits.ctrl.wid).full,false.B,InputFIFO.io.enq.ready)
  InputFIFO.io.enq.valid:=Mux(shiftBoard(io.lsu_req.bits.ctrl.wid).full,false.B,io.lsu_req.valid)

  io.csr_wid:=AddrCalc.io.csr_wid
  AddrCalc.io.csr_tid:=io.csr_tid
  AddrCalc.io.csr_pds:=io.csr_pds
  AddrCalc.io.csr_numw:=io.csr_numw
}

class ShiftBoard(val depth:Int) extends Module{
  val io=IO(new Bundle{
    val left=Input(Bool())
    val right=Input(Bool())
    val full=Output(Bool())
    val empty=Output(Bool())
  })
  val taps=(Seq.fill(depth)(RegInit(false.B)))
  val left_move=io.left
  val right_move=io.right

  taps.zipWithIndex.foreach {case(a,b)=>
    if(b==0) a:= Mux(left_move^right_move,Mux(left_move,true.B,taps(b+1)),a)
    else if(b==depth-1) a:= Mux(left_move^right_move,Mux(left_move,taps(b-1),false.B),a)
    else a:=Mux(left_move^right_move,Mux(left_move,taps(b-1),taps(b+1)),a)
  }
  io.full:= taps(depth-1)//Mux(left_move^right_move,Mux(left_move,taps(depth-2),false.B),taps(depth-1))
  io.empty:= !Mux(left_move^right_move,Mux(left_move,true.B,taps(1)),taps(0))
}
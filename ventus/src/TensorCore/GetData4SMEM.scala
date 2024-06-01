package TensorCore

import chisel3._
import chisel3.util._
import pipeline.IDecode._
import top.parameters._
import pipeline.{ByteExtract,LSU2WB, CtrlSigs, DCacheCoreRsp_np, IDecode, InstWriteBack, LSU2WB, LSUexe, MSHROutput, MSHRv2, MshrTag, ShareMemCoreReq_np, ShareMemCoreRsp_np, ShareMemPerLaneAddr_np, ShiftBoard, WriteScalarCtrl, WriteVecCtrl, toShared, vExeData}
class Matrix_descriptor(dtype:Int) extends Bundle{
  //  val data_in = new vExeData
  //  val rm = UInt(3.W)
  val mat_des = UInt((dtype-33).W)
  val isRowMajor = UInt(1.W)
  val addrBase = UInt(32.W)

  val mask = Vec(num_thread, Bool())
  val ctrl = new CtrlSigs()
}

class Matrix_dataout() extends Bundle{
  //  val mat_des = UInt(dtype.W)
  // output data will be reshape as register vExedata.in1/in2 type
  val data = Vec(num_thread, UInt(xLen.W))
}

class getData4SMEM extends Module{
  val io = IO(new Bundle {
    val in = Flipped(DecoupledIO(new Matrix_descriptor(64)))
    val out = DecoupledIO(new Matrix_dataout)
    val shared_req = DecoupledIO(new ShareMemCoreReq_np())
    val shared_rsp = Flipped(DecoupledIO(new DCacheCoreRsp_np))
  })

  // use LSU | get data from SMEM
  val LoadSMEM = Module(new Load4SMEM())
  LoadSMEM.io.lsu_req.bits.mask <> io.in.bits.mask

  LoadSMEM.io.shared_req <> io.shared_req
  LoadSMEM.io.shared_rsp <> io.shared_rsp

  for (x <- 0 until num_thread) {
    LoadSMEM.io.lsu_req.bits.in1(x) := io.in.bits.addrBase.asUInt()
    LoadSMEM.io.lsu_req.bits.in2(x) := 0.U // 地址偏移量
    LoadSMEM.io.lsu_req.bits.in3(x) := 0.U // 要写入的数据
  }
  LoadSMEM.io.lsu_req.bits.ctrl <> io.in.bits.ctrl
  LoadSMEM.io.lsu_req.valid := true.B

  io.out.valid := LoadSMEM.io.lsu_rsp.valid
  io.out.bits.data <> LoadSMEM.io.lsu_rsp.bits.data
}

class AddrCalculate_SMEM(val sharedmemory_addr_max: UInt = 4096.U(32.W)) extends Module{
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
    val flush_dcache = Flipped(DecoupledIO(Bool()))
    val to_shared = DecoupledIO(new ShareMemCoreReq_np)
  })
  val s_idle :: s_save :: s_shared :: Nil = Enum(3)
  val cnt = new Counter(n = num_thread)
  val state = RegInit(init = s_idle)

  val reg_save = Reg(new vExeData)
  val is_flush = RegInit(false.B)
  io.csr_wid:=reg_save.ctrl.wid
  // val rdy_fromFIFO = Reg(Bool())
  io.from_fifo.ready := state===s_idle && !io.flush_dcache.valid
  io.flush_dcache.ready := state === s_idle
  val reg_entryID = RegInit(0.U(log2Up(lsu_nMshrEntry).W))

  val addr = Wire(Vec(num_thread, UInt(xLen.W)))
  val is_shared = Wire(Vec(num_thread, Bool()))
  val all_shared = Wire(Bool())


  // Address Calculate & Analyze, Comb Logic @reg_save
  (0 until num_thread).foreach( x => {
//    addr(x) :=  Mux(reg_save.ctrl.isvec & reg_save.ctrl.disable_mask,
//      Mux(reg_save.ctrl.is_vls12,
//        reg_save.in1(x)+reg_save.in2(x),
//        (reg_save.in1(x) + reg_save.in2(x))(1,0) + (Cat((io.csr_tid + x.asUInt),0.U(2.W) ) ) + io.csr_pds + (((Cat((reg_save.in1(x)+reg_save.in2(x))(31,2),0.U(2.W)))*io.csr_numw)<<depth_thread)
//      ),
//      Mux(reg_save.ctrl.isvec,
//        reg_save.in1(x) + Mux(reg_save.ctrl.mop===0.U,
//          x.asUInt()<<2,
//          Mux(reg_save.ctrl.mop===3.U,
//            reg_save.in2(x),
//            x.asUInt*reg_save.in2(x))
//        ),
//        reg_save.in1(0) + reg_save.in2(0)
//      )
//    )
//  modified to unit-stride寻址模式	rs1 + i*4
//    reg_save.in1(0) + x.asUInt()<<2!!!!
//    not reg_save.in1(x) + x.asUInt()<<2
    addr(x) := reg_save.in1(x) + x.asUInt()<<2
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
    io.to_mshr.bits.tag.spike_info.get := reg_save.ctrl.spike_info.get
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
//  io.to_dcache.bits.instrId := reg_entryID

//  // |reg_save| -> |addr & mask| -> |PriorityEncoder| -> |tag & idx| -> |io.to_dcache.bits|
//  io.to_dcache.bits.tag := tag
//  io.to_dcache.bits.setIdx := setIdx
//  val opcode_wire =Wire(UInt(3.W))
//  val param_wire_alt =Wire(UInt(4.W))
//  param_wire_alt:= Mux(reg_save.ctrl.alu_fn===FN_SWAP,16.U,Mux(reg_save.ctrl.alu_fn===FN_AMOADD,0.U,Mux(reg_save.ctrl.alu_fn===FN_XOR,1.U,
//    Mux(reg_save.ctrl.alu_fn===FN_AND,3.U,Mux(reg_save.ctrl.alu_fn===FN_OR,2.U,Mux(reg_save.ctrl.alu_fn===FN_MIN,4.U,
//      Mux(reg_save.ctrl.alu_fn===FN_MAX,5.U,Mux(reg_save.ctrl.alu_fn===FN_MINU,6.U,Mux(reg_save.ctrl.alu_fn===FN_MAXU,7.U,1.U)))))))))
//  val param_wire=Wire(UInt(4.W))
// SMEM get data don't have atomic(reg_save.ctrl.atomic)

  val mask_next = Wire(Vec(num_thread, Bool()))

  // End of Addr Logic

  // FSM State Transfer
  switch(state){
    is (s_idle){
      when(io.from_fifo.valid || io.flush_dcache.valid){ state := s_save }.otherwise{ state := state }
      cnt.reset()
    }
    is (s_save){
      when(reg_save.ctrl.mem_cmd.orR){ // read or write
        when(all_shared && !is_flush) { // shared memory
          when(io.to_mshr.fire()) {
            state := s_shared
          }.otherwise {
            state := s_save
          }
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
  }
  // FSM Operation
  switch(state){
    is (s_idle){
      when(io.flush_dcache.fire()){
        reg_save := RegInit(0.U.asTypeOf(new vExeData))
        reg_save.ctrl.mem_cmd := 1.U
      }.elsewhen(io.from_fifo.fire()){ // Next: s_save
        reg_save := io.from_fifo.bits   // save data
        when(io.from_fifo.bits.ctrl.atomic){
          reg_save.ctrl.aq := true.B
        }.otherwise{
          reg_save.ctrl.aq:=io.from_fifo.bits.ctrl.aq
        }
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
  }

  switch(state){
    is (s_idle){
      when(io.flush_dcache.fire){
        is_flush := true.B
      }.otherwise{
        is_flush := false.B
      }
    }
  }

  if (SPIKE_OUTPUT){
    when( state===s_save && io.to_mshr.fire && reg_save.ctrl.mem/*&&reg_save.ctrl.wid===wid_to_check.U*/){
      printf(p"sm ${reg_save.ctrl.spike_info.get.sm_id} warp ${Decimal(reg_save.ctrl.wid)} ")
      printf(p"0x${Hexadecimal(reg_save.ctrl.spike_info.get.pc)} 0x${Hexadecimal(reg_save.ctrl.spike_info.get.inst)}")
      when(reg_save.ctrl.mem_cmd === IDecode.M_XRD){
        printf(p" lsu.r ")
      }.elsewhen(reg_save.ctrl.mem_cmd === IDecode.M_XWR){
        printf(p" lsu.w ")
      }
      when(!reg_save.ctrl.isvec){
        when(reg_save.ctrl.mem_cmd === IDecode.M_XWR){
          printf(p"x ${reg_save.ctrl.reg_idx2} op ${reg_save.ctrl.mop} ")
          printf(p"${Hexadecimal(reg_save.in3(0))} ")
        }.elsewhen(reg_save.ctrl.mem_cmd === IDecode.M_XRD){
          printf(p"x ${reg_save.ctrl.reg_idxw} op ${reg_save.ctrl.mop} ")
        }
        printf(p"@ ${Hexadecimal(reg_save.in1(0))}+${Hexadecimal(reg_save.in2(0))}\n")
      }.otherwise{
        when(reg_save.ctrl.mem_cmd === IDecode.M_XWR) {
          // vsw12 uses inst[24:20] as src
          when(reg_save.ctrl.disable_mask){
            printf(p"v${reg_save.ctrl.reg_idx2} op ${reg_save.ctrl.mop} ")
          }.otherwise{
            printf(p"v${reg_save.ctrl.reg_idxw} op ${reg_save.ctrl.mop} ")
          }
          printf(p"mask ${Binary(reg_save.mask.asUInt)} ")
          reg_save.in3.reverse.foreach { x => printf(p"${Hexadecimal(x)} ") }
        }.elsewhen(reg_save.ctrl.mem_cmd === IDecode.M_XRD){
          printf(p"v${reg_save.ctrl.reg_idx3} op ${reg_save.ctrl.mop} ")
        }
        printf(p"@")
        when(false.B){
          //(reg_save.in1(x) + reg_save.in2(x))(1,0) + (Cat((io.csr_tid + x.asUInt),0.U(2.W) ) ) + io.csr_pds + (((Cat((reg_save.in1(x)+reg_save.in2(x))(31,2),0.U(2.W)))*io.csr_numw)<<depth_thread)
        }.otherwise{
          //(reg_save.in1 zip reg_save.in2).reverse.foreach(x => printf(p" ${Hexadecimal(x._1)}+${Hexadecimal(x._2)}"))
          addr.reverse.foreach{ x =>
            printf(p" ${Hexadecimal(x)}")
          }
        }
        printf(p"\n")
      }
    }
  }
}
class Load4SMEM() extends Module{
  // default size: 128 * (num_thread=8) * (xlen/8=4) = 4KByte
  val io = IO(new Bundle{
    val lsu_req = Flipped(DecoupledIO(new vExeData()))
    //val lsu_rsp = DecoupledIO(new WriteBackControl())
    val lsu_rsp = DecoupledIO(new MSHROutput)
    val shared_req = DecoupledIO(new ShareMemCoreReq_np())
    val shared_rsp = Flipped(DecoupledIO(new DCacheCoreRsp_np))
    val fence_end = Output(UInt(num_warp.W))
    val flush_dcache = Flipped(DecoupledIO(Bool()))

    val csr_wid = Output(UInt(depth_warp.W))
    val csr_pds = Input(UInt(xLen.W))
    val csr_numw = Input(UInt(xLen.W))
    val csr_tid = Input(UInt(xLen.W))
  })
  val sharedmemory_addr_max = sharemem_size.U(32.W)
  //val sharedmemory = Module(new SharedMemoryV2(nSharedMemoryEntry, num_thread, xLen, lsu_nMshrEntry)) // default: 128

  val InputFIFO = Module(new Queue(new vExeData, entries=1, pipe=true))
  InputFIFO.io.enq <> io.lsu_req

  val AddrCalc = Module(new AddrCalculate_SMEM(sharedmemory_addr_max))
  AddrCalc.io.from_fifo <> InputFIFO.io.deq
  io.shared_req <> AddrCalc.io.to_shared
  AddrCalc.io.flush_dcache <> io.flush_dcache

  // SOME MSHR INTERFACE
  val Coalscer = Module(new MSHRv2)
  //val outputFIFO = Module(new Queue(new MSHROutput, num_warp+1, pipe=true))
  Coalscer.io.from_dcache <> io.shared_rsp
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
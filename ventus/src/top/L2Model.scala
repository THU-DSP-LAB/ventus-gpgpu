package top

import L1Cache.DCache.{DCacheMemReq, DCacheMemRsp, DCacheModule}
import L1Cache.{L1CacheMemReq, L1CacheMemRsp, RVGBundle, RVGModule}
import chisel3._
import chisel3.util._
import chisel3.util.experimental.loadMemoryFromFile
import config.config.Parameters

// NOTICE: these models are only for test
class L2ModelL1Req(implicit p: Parameters) extends DCacheMemReq{
  override val a_source = UInt((log2Up(NSms)+log2Up(NCacheInSM)+WIdBits).W)
}
class L2ModelL1Rsp(implicit p: Parameters) extends DCacheMemRsp{
  override val d_source = UInt((log2Up(NSms)+log2Up(NCacheInSM)+WIdBits).W)
}

class SM2L2ModelArbiterIO(implicit p: Parameters) extends RVGBundle{
  val memReqVecIn = Flipped(Vec(NSms, Decoupled(new L1CacheMemReq)))
  val memReqOut = Decoupled(new L2ModelL1Req)
  val memRspIn = Flipped(Decoupled(new L2ModelL1Rsp))
  val memRspVecOut = Vec(NSms, Decoupled(new L1CacheMemRsp))
}

class SM2L2ModelArbiter(implicit p: Parameters) extends RVGModule {
  val io = IO(new SM2L2ModelArbiterIO)

  // **** memReq ****
  val memReqArb = Module(new Arbiter(new L2ModelL1Req,NSms))
  //memReqArb.io.in <> io.memReqVecIn
  for(i <- 0 until NSms) {
    memReqArb.io.in(i).bits.a_opcode := io.memReqVecIn(i).bits.a_opcode
    memReqArb.io.in(i).bits.a_source := Cat(i.asUInt,io.memReqVecIn(i).bits.a_source)
    memReqArb.io.in(i).bits.a_addr := io.memReqVecIn(i).bits.a_addr
    memReqArb.io.in(i).bits.a_mask := io.memReqVecIn(i).bits.a_mask
    memReqArb.io.in(i).bits.a_data := io.memReqVecIn(i).bits.a_data
    //memReqArb.io.in(i).bits.a_ := 0.U//log2Up(BlockWords*BytesOfWord).U
    memReqArb.io.in(i).valid := io.memReqVecIn(i).valid
    io.memReqVecIn(i).ready:=memReqArb.io.in(i).ready
  }
  io.memReqOut <> memReqArb.io.out
  // ****************

  // **** memRsp ****
  for(i <- 0 until NSms) {
    io.memRspVecOut(i).bits.d_data:=io.memRspIn.bits.d_data
    io.memRspVecOut(i).bits.d_source:=io.memRspIn.bits.d_source
    io.memRspVecOut(i).bits.d_addr:=io.memRspIn.bits.d_addr
    io.memRspVecOut(i).valid :=
      io.memRspIn.bits.d_source(log2Up(NSms)+log2Up(NCacheInSM)+WIdBits-1,WIdBits+log2Up(NCacheInSM))===i.asUInt && io.memRspIn.valid
  }
  io.memRspIn.ready := Mux1H(UIntToOH(io.memRspIn.bits.d_source(log2Up(NSms)+log2Up(NCacheInSM)+WIdBits-1,WIdBits+log2Up(NCacheInSM))),
    Reverse(Cat(io.memRspVecOut.map(_.ready))))//TODO check order in test
  // ****************
}

class L2Model(implicit p: Parameters) extends DCacheModule {
  val io = IO(new Bundle{
    val memReq = Flipped(DecoupledIO(new L2ModelL1Req))
    val memRsp= DecoupledIO(new L2ModelL1Rsp)

    //ports under tb control
    val memReq_ready = Input(Bool())
  })
  io.memReq.ready := io.memReq_ready
  // ***** model params *****

  //整个External Memory model划分成了两个独立的存储空间
  //为了方便ICache和DCache各自使用不同的txt
  val L2Base1 = 0
  val L2Size1 = pipeline.parameters.sharedmem_depth//Unit: block(typical 32 words)
  val L2Base2 = pipeline.parameters.sharedmem_depth
  val L2Size2 = 128//Unit: block(typical 32 words)
  assert(L2Base1 <= L2Base2,"ExtMemBase1 > ExtMemBase2")
  assert(L2Base1 + L2Size1 <= L2Base2,"space overlap in ExtMem")

  val ExtMemLatency = 20//TODO 还没投入使用

  val memory1 = Mem(L2Size1*BlockWords,UInt(WordLength.W))
  loadMemoryFromFile(memory1,"./txt/reduction5.vmem")//TODO ykx notice here
  val memory2 = Mem(L2Size2*BlockWords,UInt(WordLength.W))
  loadMemoryFromFile(memory2,"./txt/DataRom.txt")//TODO ykx notice here

  val readVec = Wire(Vec(BlockWords,UInt(WordLength.W)))
  val writeVec = Wire(Vec(BlockWords,UInt(WordLength.W)))
  writeVec := io.memReq.bits.a_data

  val BlockAddr = Wire(UInt((WordLength-log2Up(BlockWords)-2).W))
  BlockAddr := get_blockAddr(io.memReq.bits.a_addr)
  val perMemoryBlockAddr = Wire(UInt((WordLength-log2Up(BlockWords)-2).W))
  val isSpace1 = Wire(Bool())
  val isSpace2 = Wire(Bool())
  when (BlockAddr-L2Base1.asUInt < L2Size1.asUInt){
    isSpace1 := true.B
    isSpace2 := false.B
    perMemoryBlockAddr := BlockAddr
  }.elsewhen(BlockAddr-L2Base2.asUInt < L2Size2.asUInt){
    isSpace2 := true.B
    isSpace1 := false.B
    perMemoryBlockAddr := BlockAddr - L2Base2.asUInt
  }.otherwise{
    isSpace1 := false.B
    isSpace2 := false.B
    perMemoryBlockAddr:= BlockAddr
    //assert(cond = false,"[ExtMem]: incoming addr out of range"+BlockAddr)
  }

  val wordAddr = Wire(Vec(BlockWords,UInt((WordLength-2).W)))

  for (i<- 0 until BlockWords){
    wordAddr(i) := Cat(perMemoryBlockAddr,i.U(log2Up(BlockWords).W))
    when(io.memReq.fire() && io.memReq.bits.a_opcode === TLAOp_PutFull){
      when(isSpace1){
        memory1(wordAddr(i)) := writeVec(i)
      }.elsewhen(isSpace2){
        memory2(wordAddr(i)) := writeVec(i)
      }
    }.elsewhen(io.memReq.fire() && io.memReq.bits.a_opcode === TLAOp_PutPart &&
      io.memReq.bits.a_mask(i)){//TODO check order
      when(isSpace1) {
        memory1(wordAddr(i)) := writeVec(i)
      }.elsewhen(isSpace2){
        memory2(wordAddr(i)) := writeVec(i)
      }
    }
    when(isSpace1) {
      readVec(i) := memory1.read(wordAddr(i))
    }.elsewhen(isSpace2){
      readVec(i) := memory2.read(wordAddr(i))
    }.otherwise(
      readVec(i) := 0.U
    )
}

  //val RspOpCode = Wire(UInt(3.W))
  //RspOpCode := Mux(io.memReq.bits.a_opcode===4.U(4.W),1.U(3.W),0.U(3.W))

  val memRsp_Q = Module(new Queue(new L2ModelL1Rsp, 4))
  memRsp_Q.io.enq.valid := (io.memReq.fire()) & io.memReq.bits.a_opcode===4.U
  //memRsp_Q.io.enq.bits.d_opcode := RegNext(RspOpCode)
  memRsp_Q.io.enq.bits.d_addr := io.memReq.bits.a_addr
  memRsp_Q.io.enq.bits.d_data := readVec
  memRsp_Q.io.enq.bits.d_source := (io.memReq.bits.a_source)
  //memRsp_Q.io.enq.bits.size := 0.U
  io.memReq.ready := memRsp_Q.io.enq.ready

  io.memRsp <> memRsp_Q.io.deq
}


class L2ModelWithName(val inst_filepath:String,val data_filepath:String, val access_time:Int)(implicit p: Parameters) extends DCacheModule {
  val io = IO(new Bundle{
    val memReq = Flipped(DecoupledIO(new L2ModelL1Req))
    val memRsp= DecoupledIO(new L2ModelL1Rsp)

    //ports under tb control
    val memReq_ready = Input(Bool())
  })
  // ***** model params *****

  //整个External Memory model划分成了两个独立的存储空间
  //为了方便ICache和DCache各自使用不同的txt
  val L2Base1 = 0
  val L2Size1 = pipeline.parameters.sharedmem_depth//Unit: block(typical 32 words)
  val L2Base2 = pipeline.parameters.sharedmem_depth
  val L2Size2 = 128//Unit: block(typical 32 words)
  assert(L2Base1 <= L2Base2,"ExtMemBase1 > ExtMemBase2")
  assert(L2Base1 + L2Size1 <= L2Base2,"space overlap in ExtMem")

  val ExtMemLatency = 20//TODO 还没投入使用

  val memory1 = Mem(L2Size1*BlockWords,UInt(WordLength.W))
  loadMemoryFromFile(memory1,inst_filepath)//TODO ykx notice here
  val memory2 = Mem(L2Size2*BlockWords,UInt(WordLength.W))
  loadMemoryFromFile(memory2,data_filepath)//TODO ykx notice here

  val readVec = Wire(Vec(BlockWords,UInt(WordLength.W)))
  val writeVec = Wire(Vec(BlockWords,UInt(WordLength.W)))
  writeVec := io.memReq.bits.a_data

  val BlockAddr = Wire(UInt((WordLength-log2Up(BlockWords)-2).W))
  BlockAddr := get_blockAddr(io.memReq.bits.a_addr)
  val perMemoryBlockAddr = Wire(UInt((WordLength-log2Up(BlockWords)-2).W))
  val isSpace1 = Wire(Bool())
  val isSpace2 = Wire(Bool())
  when (BlockAddr-L2Base1.asUInt < L2Size1.asUInt){
    isSpace1 := true.B
    isSpace2 := false.B
    perMemoryBlockAddr := BlockAddr
  }.elsewhen(BlockAddr-L2Base2.asUInt < L2Size2.asUInt){
    isSpace2 := true.B
    isSpace1 := false.B
    perMemoryBlockAddr := BlockAddr - L2Base2.asUInt
  }.otherwise{
    isSpace1 := false.B
    isSpace2 := false.B
    perMemoryBlockAddr:= BlockAddr
    //assert(cond = false,"[ExtMem]: incoming addr out of range"+BlockAddr)
  }

  val wordAddr = Wire(Vec(BlockWords,UInt((WordLength-2).W)))

  for (i<- 0 until BlockWords){
    wordAddr(i) := Cat(perMemoryBlockAddr,i.U(log2Up(BlockWords).W))
    when(io.memReq.fire() && io.memReq.bits.a_opcode === TLAOp_PutFull){
      when(isSpace1){
        memory1(wordAddr(i)) := writeVec(i)
      }.elsewhen(isSpace2){
        memory2(wordAddr(i)) := writeVec(i)
      }
    }.elsewhen(io.memReq.fire() && io.memReq.bits.a_opcode === TLAOp_PutPart &&
      io.memReq.bits.a_mask(i)){//TODO check order
      when(isSpace1) {
        memory1(wordAddr(i)) := writeVec(i)
      }.elsewhen(isSpace2){
        memory2(wordAddr(i)) := writeVec(i)
      }
    }
    when(isSpace1) {
      readVec(i) := memory1.read(wordAddr(i))
    }.elsewhen(isSpace2){
      readVec(i) := memory2.read(wordAddr(i))
    }.otherwise(
      readVec(i) := 0.U
    )
  }

  //val RspOpCode = Wire(UInt(3.W))
  //RspOpCode := Mux(io.memReq.bits.a_opcode===4.U(4.W),1.U(3.W),0.U(3.W))
  //memRsp_Q.io.enq.bits.d_opcode := RegNext(RspOpCode)

  val memRsp_Q = Module(new Queue(new L2ModelL1Rsp, 4))
  val memRspBefore = Wire(new L2ModelL1Rsp)
  memRspBefore.d_addr := io.memReq.bits.a_addr
  memRspBefore.d_data := readVec
  memRspBefore.d_source := io.memReq.bits.a_source
  val memRspAfter = Wire(new L2ModelL1Rsp)
  memRspAfter := ShiftRegister(memRspBefore, access_time, en=memRsp_Q.io.enq.ready)

  memRsp_Q.io.enq.valid := ShiftRegister(io.memReq.fire() & io.memReq.bits.a_opcode===4.U,access_time,en=memRsp_Q.io.enq.ready)
  memRsp_Q.io.enq.bits := memRspAfter
  io.memReq.ready := io.memReq_ready && memRsp_Q.io.enq.ready

  io.memRsp <> memRsp_Q.io.deq
}
package top
import chisel3._
import chisel3.util._
import L1Cache.DCache._
import L1Cache._
import L2cache._
import chisel3.experimental.dataview._
import config.config.Parameters

/*class FakeL1DCache(implicit p: Parameters) extends DataCacheIO {
  assert(NLanes == BlockWords)
  // request
  val coreReq_save = Reg(new DCacheCoreReq)
  val memRsp_save = Reg(new DCacheMemRsp)
  val s_idle::s_send_mem::s_wait_mem::s_reply::Nil = Enum(4)
  val state = RegInit(s_idle)

// wire: ready-valids
  io.coreReq.ready := state === s_idle
  io.memReq.valid := state === s_send_mem
  io.memRsp.ready := state === s_wait_mem
  io.coreRsp.valid := state === s_reply
// wire: memReq connection
  io.memReq.bits.a_source := coreReq_save.instrId
  io.memReq.bits.a_addr := Cat(coreReq_save.tag, coreReq_save.setIdx, 0.U(BlockOffsetBits.W), 0.U(2.W))
  io.memReq.bits.a_data.foreach{ _ := 0.U }
  io.memReq.bits.a_mask.foreach{ _ := false.B }
  (0 until NLanes).foreach{ i =>
    when(coreReq_save.perLaneAddr(i).activeMask){ // sorting: thread view -> cacheline view
      io.memReq.bits.a_data(coreReq_save.perLaneAddr(i).blockOffset) := coreReq_save.data(i)
      io.memReq.bits.a_mask(coreReq_save.perLaneAddr(i).blockOffset) := true.B
    }
  }
  io.memReq.bits.a_opcode := Mux(coreReq_save.isWrite,
                                Mux(coreReq_save.perLaneAddr.map(_.activeMask).reduce(_ && _), TLAOp_PutFull, TLAOp_PutPart),
                                TLAOp_Get)
// wire: coreRsp connection
  io.coreRsp.bits.instrId := memRsp_save.d_source
  io.coreRsp.bits.data.foreach{ _ := 0.U }
  (0 until NLanes).foreach{ i =>
    when(coreReq_save.perLaneAddr(i).activeMask){ // sorting: cacheline view -> thread view
      io.coreRsp.bits.data(i) := memRsp_save.d_data(coreReq_save.perLaneAddr(i).blockOffset)
    }
  }
  io.coreRsp.bits.isWrite := coreReq_save.isWrite
  io.coreRsp.bits.activeMask := VecInit(coreReq_save.perLaneAddr.map(_.activeMask))

// reg: state & data_save
  when(state === s_idle){
    when(io.coreReq.fire){
      coreReq_save := io.coreReq.bits
      state := s_send_mem
    }
  }.elsewhen(state === s_send_mem){
    when(io.memReq.fire){
      state := s_wait_mem
    }
  }.elsewhen(state === s_wait_mem){
    when(io.memRsp.fire){
      memRsp_save := io.memRsp.bits
      state := s_reply
    }
  }.elsewhen(state === s_reply){
    when(io.coreRsp.fire){
      coreReq_save := 0.U.asTypeOf(coreReq_save)
      memRsp_save := 0.U.asTypeOf(memRsp_save)
      state := s_idle
    }
  }.otherwise{}
}

//class FakeL1Cache2L2ArbiterIO(implicit p: Parameters) extends DCacheBundle {
//  val memReqVecIn = Flipped(Vec(NCacheInSM, Decoupled(new DCacheMemReq())))
//  val memReqOut = Decoupled(new L1CacheMemReq)
//  val memRspIn = Flipped(Decoupled(new L1CacheMemRsp))
//  val memRspVecOut = Vec(NCacheInSM, Decoupled(new DCacheMemRsp()))
//}
//class FakeL1CacheL2Arbiter(implicit p: Parameters) extends DCacheModule{
//  val io = IO(new FakeL1Cache2L2ArbiterIO)
//  val memReqArb = Module(new Arbiter(new L1CacheMemReq, NCacheInSM))
//  memReqArb.io.in <> io.memReqVecIn
//  (0 until NCacheInSM).foreach{ i =>
//    memReqArb.io.in(i).bits.a_source := Cat(i.asUInt,io.memReqVecIn(i).bits.a_source)
//
//    io.memRspVecOut(i).bits <> io.memRspIn.bits
//    io.memRspVecOut(i).valid :=
//      io.memRspIn.bits.d_source(log2Up(NCacheInSM)+WIdBits-1, WIdBits) === i.asUInt && io.memRspIn.valid
//  }
//  io.memReqOut <> memReqArb.io.out
//  io.memRspIn.ready := Mux1H(UIntToOH(io.memRspIn.bits.d_source(log2Up(NCacheInSM) + WIdBits - 1, WIdBits)),
//    Reverse(Cat(io.memRspVecOut.map(_.ready))))
//}
//
//class FakeSM2L2ArbiterIO(L2param: InclusiveCacheParameters_lite)(implicit p: Parameters) extends L1Cache.RVGBundle{
//  val memReqVecIn = Flipped(Vec(NSms, Decoupled(new L1CacheMemReq)))
//  val memReqOut = Decoupled(new TLBundleA_lite(L2param))
//  val memRspIn = Flipped(Decoupled(new TLBundleD_lite_plus(L2param)))
//  val memRspVecOut = Vec(NSms, Decoupled(new L1CacheMemRsp))
//}
//class FakeSM2L2Arbiter(L2param: InclusiveCacheParameters_lite)(implicit p: Parameters) extends L1Cache.RVGModule{
//  val io = IO(new FakeSM2L2ArbiterIO(L2param))
//  val memReqArb = Module(new Arbiter(new TLBundleA_lite(L2param), NSms))
//  (0 until NSms).foreach{ i =>
//    memReqArb.io.in(i).bits.opcode := io.memReqVecIn(i).bits.a_opcode
//    memReqArb.io.in(i).bits.source := Cat(i.asUInt, io.memReqVecIn(i).bits.a_source)
//    memReqArb.io.in(i).bits.address := io.memReqVecIn(i).bits.a_addr
//    memReqArb.io.in(i).bits.data := io.memReqVecIn(i).bits.a_data
//    memReqArb.io.in(i).bits.mask := io.memReqVecIn(i).bits.a_mask.asUInt
//    memReqArb.io.in(i).bits.size := 0.U //log2Up(BlockWords*BytesOfWord).U
//    memReqArb.io.in(i).valid := io.memReqVecIn(i).valid
//    io.memReqVecIn(i).ready := memReqArb.io.in(i).ready
//  }
//  io.memReqOut <> memReqArb.io.out
//
//  (0 until NSms).foreach{ i =>
//    io.memRspVecOut(i).bits.d_data := io.memRspIn.bits.data
//    io.memRspVecOut(i).bits.d_source := io.memRspIn.bits.source
//    io.memRspVecOut(i).bits.d_addr := io.memRspIn.bits.address
//    io.memRspVecOut(i).bits.d_addr := DontCare
//    io.memRspVecOut(i).valid :=
//      io.memRspIn.bits.source(log2Up(NSms) + log2Up(NCacheInSM) + WIdBits - 1, WIdBits + log2Up(NCacheInSM)) === i.asUInt && io.memRspIn.valid
//  }
//  io.memRspIn.ready := Mux1H(UIntToOH(io.memRspIn.bits.source(log2Up(NSms) + log2Up(NCacheInSM) + WIdBits - 1, WIdBits + log2Up(NCacheInSM))),
//    Reverse(Cat(io.memRspVecOut.map(_.ready))))
//}
class FakeL2Cache(params: InclusiveCacheParameters_lite) extends L2cache.L2CacheIO(params){
  val cnt = new Counter(5)
  val memReq_save = RegInit(0.U.asTypeOf(new TLBundleA_lite(params)))
  val memRsp_save = RegInit(0.U.asTypeOf(new TLBundleD_lite_plus(params)))
  //val coreRsp = Wire(new TLBundleD_lite(params))

  val s_idle::s_send_mem::s_wait_mem::s_reply::Nil = Enum(4)
  val state = RegInit(s_idle)
  io.in_a.ready := state === s_idle
  io.out_a.valid := state === s_send_mem
  io.out_d.ready := state === s_wait_mem
  io.in_d.valid := state === s_reply

  io.out_a.bits := memReq_save
  io.in_d.bits.viewAsSupertype(new TLBundleD_lite(params)) := memRsp_save
  io.in_d.bits.address := memReq_save.address
  when(state === s_idle){
    when(io.in_a.fire) {
      state := s_send_mem
      memReq_save := io.in_a.bits
    }
  }.elsewhen(state === s_send_mem){
    when(io.out_a.fire){
      state := s_wait_mem
    }
  }.elsewhen(state === s_wait_mem){
    when(io.out_d.fire){
      state := s_reply
      memRsp_save.viewAsSupertype(new TLBundleD_lite(params)) := io.out_d.bits
      memRsp_save.address := memReq_save.address // io.in_a.bits.address
    }
  }.elsewhen(state === s_reply){
    when(io.in_d.fire){
      memReq_save := 0.U.asTypeOf(memReq_save)
      memRsp_save := 0.U.asTypeOf(memRsp_save)
      state := s_idle
    }
  }.otherwise{}
  io.flush.ready := false.B
  io.invalidate.ready := false.B
}*/

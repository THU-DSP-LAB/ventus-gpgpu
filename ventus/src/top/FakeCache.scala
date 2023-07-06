package top
import chisel3._
import chisel3.util._
import L1Cache.DCache._
import L2cache._
import chisel3.experimental.dataview.BundleUpcastable
import config.config.Parameters

class FakeL1DCache(implicit p: Parameters) extends DCacheModule {
  val io = IO(new Bundle {
    val coreReq = Flipped(DecoupledIO(new DCacheCoreReq))
    val coreRsp = DecoupledIO(new DCacheCoreRsp)
    val memRsp = Flipped(DecoupledIO(new DCacheMemRsp))
    val memReq = DecoupledIO(new DCacheMemReq)
  })
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
  io.memReq.bits.a_data := coreReq_save.data
  io.memReq.bits.a_addr := VecInit(coreReq_save.perLaneAddr.map { lane =>
    Cat(coreReq_save.setIdx, lane.blockOffset, 0.U(2.W)) // 2 = log2(BytesOfWord)
  })
  io.memReq.bits.a_opcode := Mux(coreReq_save.isWrite,
                                Mux(coreReq_save.perLaneAddr.map(_.activeMask).reduce(_ && _), TLAOp_PutFull, TLAOp_PutPart),
                                TLAOp_Get)
  io.memReq.bits.a_mask := VecInit(coreReq_save.perLaneAddr.map(_.activeMask))
// wire: coreRsp connection
  io.coreRsp.bits.instrId := memRsp_save.d_source
  io.coreRsp.bits.data := memRsp_save.d_data
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

class FakeL2Cache(params: InclusiveCacheParameters_lite) extends Module{
  val io = IO(new Bundle {
    val in_a = Flipped(Decoupled(new TLBundleA_lite(params)))
    val in_d = Decoupled(new TLBundleD_lite_plus(params))
    val out_a = Decoupled(new TLBundleA_lite(params))
    val out_d = Flipped(Decoupled(new TLBundleD_lite(params)))
    val flush = Flipped(Decoupled(Bool()))
    val invalidate = Flipped(Decoupled(Bool()))
  })
  val cnt = new Counter(5)
  val memReq_save = RegInit(0.U.asTypeOf(new TLBundleA_lite(params)))
  val memRsp_save = RegInit(0.U.asTypeOf(new TLBundleD_lite_plus(params)))
  val coreRsp = Wire(new TLBundleD_lite(params))

  val s_idle::s_send_mem::s_wait_mem::s_reply::Nil = Enum(4)

  io.in_a.ready := state === s_idle
  io.out_a.valid := state === s_send_mem
  io.out_d.ready := state === s_wait_mem
  io.in_d.valid := state === s_reply

  io.out_a.bits := memReq_save
  io.in_d.bits.viewAsSupertype(new TLBundleD_lite(params)) := memRsp_save
  io.in_d.bits.address := memReq_save.address
  val state = RegInit(s_idle)
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
      memRsp_save := io.out_d.bits
    }
  }.elsewhen(state === s_reply){
    when(io.in_d.fire){
      memReq_save := 0.U.asTypeOf(memReq_save)
      memRsp_save := 0.U.asTypeOf(memRsp_save)
      state := s_idle
    }
  }.otherwise{}
}

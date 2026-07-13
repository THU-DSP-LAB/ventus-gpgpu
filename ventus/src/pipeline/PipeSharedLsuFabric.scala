package pipeline

import chisel3._
import chisel3.experimental.hierarchy.{instantiable, public}
import chisel3.util._

class PipeSharedLsuSourceIO extends Bundle {
  val dcache_req = Flipped(DecoupledIO(new DCacheCoreReq_np))
  val dcache_rsp = DecoupledIO(new DCacheCoreRsp_np)
  val shared_req = Flipped(DecoupledIO(new ShareMemCoreReq_np))
  val shared_rsp = DecoupledIO(new DCacheCoreRsp_np)
  val lsu_rsp = Flipped(DecoupledIO(new MSHROutput))
}

class PipeSharedLsuFabricIO(numSources: Int) extends Bundle {
  val subcore = Vec(numSources, new PipeSharedLsuSourceIO)

  val sm_dcache_req = DecoupledIO(new DCacheCoreReq_np)
  val sm_dcache_rsp = Flipped(DecoupledIO(new DCacheCoreRsp_np))
  val sm_shared_req = DecoupledIO(new ShareMemCoreReq_np)
  val sm_shared_rsp = Flipped(DecoupledIO(new DCacheCoreRsp_np))
  val lsu2wb_lsu_rsp = DecoupledIO(new MSHROutput)
}

@instantiable
class PipeSharedLsuFabric(numSources: Int, sourceWidth: Int, localInstrIdWidth: Int) extends Module {
  @public val io = IO(new PipeSharedLsuFabricIO(numSources))

  require(numSources >= 1)

  private val dcacheInstrIdWidth = (new DCacheCoreReq_np).instrId.getWidth

  private def encodeInstrId(source: UInt, localInstrId: UInt): UInt = {
    val encoded = Wire(UInt(dcacheInstrIdWidth.W))
    encoded := Cat(source(sourceWidth - 1, 0), localInstrId(localInstrIdWidth - 1, 0))
    encoded
  }

  private def decodeSource(fabricInstrId: UInt): UInt =
    fabricInstrId(sourceWidth + localInstrIdWidth - 1, localInstrIdWidth)

  private def decodeLocalInstrId(fabricInstrId: UInt): UInt =
    fabricInstrId(localInstrIdWidth - 1, 0)

  for (source <- 0 until numSources) {
    io.subcore(source).dcache_req.ready := false.B
    io.subcore(source).dcache_rsp.valid := false.B
    io.subcore(source).dcache_rsp.bits := 0.U.asTypeOf(new DCacheCoreRsp_np)
    io.subcore(source).shared_req.ready := false.B
    io.subcore(source).shared_rsp.valid := false.B
    io.subcore(source).shared_rsp.bits := 0.U.asTypeOf(new DCacheCoreRsp_np)
    io.subcore(source).lsu_rsp.ready := false.B
  }

  val dcacheReqArb = Module(new RRArbiter(new DCacheCoreReq_np, numSources))
  val sharedReqArb = Module(new RRArbiter(new ShareMemCoreReq_np, numSources))
  val lsuRspArb = Module(new RRArbiter(new MSHROutput, numSources))

  for (source <- 0 until numSources) {
    val sourceId = source.U(sourceWidth.W)
    val dcacheReq = Wire(new DCacheCoreReq_np)
    val sharedReq = Wire(new ShareMemCoreReq_np)
    dcacheReq := io.subcore(source).dcache_req.bits
    sharedReq := io.subcore(source).shared_req.bits
    dcacheReq.instrId := encodeInstrId(sourceId, io.subcore(source).dcache_req.bits.instrId)
    sharedReq.instrId := encodeInstrId(sourceId, io.subcore(source).shared_req.bits.instrId)

    dcacheReqArb.io.in(source).valid := io.subcore(source).dcache_req.valid
    dcacheReqArb.io.in(source).bits := dcacheReq
    io.subcore(source).dcache_req.ready := dcacheReqArb.io.in(source).ready
    sharedReqArb.io.in(source).valid := io.subcore(source).shared_req.valid
    sharedReqArb.io.in(source).bits := sharedReq
    io.subcore(source).shared_req.ready := sharedReqArb.io.in(source).ready
    val restoredLsuRsp = Wire(new MSHROutput)
    restoredLsuRsp := io.subcore(source).lsu_rsp.bits
    restoredLsuRsp.tag.warp_id :=
      PipeSubcoreHelpers.internalWid(source, io.subcore(source).lsu_rsp.bits.tag.warp_id)
    lsuRspArb.io.in(source).valid := io.subcore(source).lsu_rsp.valid
    lsuRspArb.io.in(source).bits := restoredLsuRsp
    io.subcore(source).lsu_rsp.ready := lsuRspArb.io.in(source).ready
  }

  io.sm_dcache_req <> dcacheReqArb.io.out
  io.sm_shared_req <> sharedReqArb.io.out

  when(dcacheReqArb.io.out.valid) {
    assert(dcacheReqArb.io.chosen < numSources.U)
  }
  when(sharedReqArb.io.out.valid) {
    assert(sharedReqArb.io.chosen < numSources.U)
  }
  when(lsuRspArb.io.out.valid) {
    assert(lsuRspArb.io.chosen < numSources.U)
  }
  assert(PopCount(dcacheReqArb.io.in.map(_.fire)) <= 1.U)
  assert(PopCount(sharedReqArb.io.in.map(_.fire)) <= 1.U)
  assert(PopCount(lsuRspArb.io.in.map(_.fire)) <= 1.U)

  val lsuRspOut = Module(new Queue(new MSHROutput, 2))
  lsuRspOut.io.enq <> lsuRspArb.io.out
  io.lsu2wb_lsu_rsp <> lsuRspOut.io.deq

  val dcacheRspIn = Module(new Queue(new DCacheCoreRsp_np, 4))
  val sharedRspIn = Module(new Queue(new DCacheCoreRsp_np, 4))
  dcacheRspIn.io.enq <> io.sm_dcache_rsp
  sharedRspIn.io.enq <> io.sm_shared_rsp

  val dcacheRspSource = decodeSource(dcacheRspIn.io.deq.bits.instrId)
  val dcacheRspLocalInstrId = decodeLocalInstrId(dcacheRspIn.io.deq.bits.instrId)
  val sharedRspSource = decodeSource(sharedRspIn.io.deq.bits.instrId)
  val sharedRspLocalInstrId = decodeLocalInstrId(sharedRspIn.io.deq.bits.instrId)
  val dcacheRsp = Wire(new DCacheCoreRsp_np)
  val sharedRsp = Wire(new DCacheCoreRsp_np)
  dcacheRsp := dcacheRspIn.io.deq.bits
  sharedRsp := sharedRspIn.io.deq.bits
  dcacheRsp.instrId := dcacheRspLocalInstrId
  sharedRsp.instrId := sharedRspLocalInstrId

  val dcacheRspReady = Wire(Vec(numSources, Bool()))
  val sharedRspReady = Wire(Vec(numSources, Bool()))
  val dcacheRspSourceMatch = Wire(Vec(numSources, Bool()))
  val sharedRspSourceMatch = Wire(Vec(numSources, Bool()))
  for (source <- 0 until numSources) {
    val sourceId = source.U(sourceWidth.W)
    dcacheRspSourceMatch(source) := dcacheRspSource === sourceId
    io.subcore(source).dcache_rsp.valid := dcacheRspIn.io.deq.valid && dcacheRspSourceMatch(source)
    io.subcore(source).dcache_rsp.bits := dcacheRsp
    dcacheRspReady(source) := io.subcore(source).dcache_rsp.ready
    sharedRspSourceMatch(source) := sharedRspSource === sourceId
    io.subcore(source).shared_rsp.valid := sharedRspIn.io.deq.valid && sharedRspSourceMatch(source)
    io.subcore(source).shared_rsp.bits := sharedRsp
    sharedRspReady(source) := io.subcore(source).shared_rsp.ready
  }

  dcacheRspIn.io.deq.ready := Mux1H((0 until numSources).map { source =>
    dcacheRspSourceMatch(source) -> dcacheRspReady(source)
  })
  sharedRspIn.io.deq.ready := Mux1H((0 until numSources).map { source =>
    sharedRspSourceMatch(source) -> sharedRspReady(source)
  })
}

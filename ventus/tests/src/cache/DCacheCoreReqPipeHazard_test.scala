package play.cache

import L1Cache._
import L1Cache.DCache._
import chisel3._
import chisel3.util._
import chiseltest._
import config.config.Parameters
import org.scalatest.freespec.AnyFreeSpec
import top.parameters._

class DCacheCoreReqPipeHazardHarness(implicit p: Parameters) extends Module {
  val io = IO(new Bundle {
    val request = Flipped(Decoupled(new DCacheCoreReq))
    val tagHit = Input(Bool())
    val tagDirty = Input(Bool())
    val tagWaymask = Input(UInt(dcache_NWays.W))
    val hasDirty = Input(Bool())
    val coreRspReady = Input(Bool())

    val st1Valid = Output(Bool())
    val reserveRTABSlot = Output(Bool())
    val flushDirtyRead = Output(Bool())
    val dataReadValid = Output(Bool())
    val dataReadSetIdx = Output(UInt(log2Ceil(dcache_NSets * dcache_NWays).W))
  })

  val pipe = Module(new CoreReqPipe)
  pipe.io.CoreReq <> io.request
  pipe.io.RTABHit := false.B
  pipe.io.hasDirty := io.hasDirty
  pipe.io.MSHREmpty := true.B
  pipe.io.SMSHREmpty := true.B
  pipe.io.fillPipeDrained := true.B
  pipe.io.tA_dirtySetIdx_st0 := 0.U
  pipe.io.tA_dirtyWayMask_st0 := 1.U
  pipe.io.reqSource := false.B
  pipe.io.blockCoreReq := false.B
  pipe.io.refillWrite_valid := false.B
  pipe.io.refillWrite_blockAddr := 0.U
  pipe.io.fillCommit_valid := false.B
  pipe.io.fillCommit_blockAddr := 0.U
  pipe.io.refillWrite_setIdx := 0.U
  pipe.io.mshrReleasing_valid := false.B
  pipe.io.mshrReleasing_blockAddr := 0.U
  pipe.io.Probe_tA_ready := true.B

  pipe.io.tA_Hit_st1.hit := io.tagHit
  pipe.io.tA_Hit_st1.isDirty := io.tagDirty
  pipe.io.tA_Hit_st1.waymask := io.tagWaymask
  pipe.io.tA_Hit_st1.tag := 0.U
  pipe.io.tA_dirtyTag_st1 := 0.U
  pipe.io.tA_dirtyMask_st1 := Fill(dcache_BlockWords * BytesOfWord, 1.U)
  pipe.io.MSHR_ProbeStatus := 0.U.asTypeOf(pipe.io.MSHR_ProbeStatus)
  pipe.io.SMSHR_ProbeStatus := 0.U.asTypeOf(pipe.io.SMSHR_ProbeStatus)
  pipe.io.WSHR_CheckResult := 0.U.asTypeOf(pipe.io.WSHR_CheckResult)
  pipe.io.Mshr_st1_ready := true.B
  pipe.io.RTAB_full := false.B

  pipe.io.memRsp_coreRsp.valid := false.B
  pipe.io.memRsp_coreRsp.bits := 0.U.asTypeOf(pipe.io.memRsp_coreRsp.bits)
  pipe.io.Probe_SMSHR.ready := true.B
  pipe.io.MissReq_MSHR.ready := true.B
  pipe.io.MissReq_Mem.ready := true.B
  pipe.io.dA_data.foreach(_ := 0.U)
  pipe.io.memReq_coreRsp.valid := false.B
  pipe.io.memReq_coreRsp.bits := 0.U.asTypeOf(pipe.io.memReq_coreRsp.bits)
  pipe.io.CoreRsp.ready := io.coreRspReady
  pipe.io.memRspIsFlu := false.B

  io.st1Valid := pipe.io.st1_valid
  io.reserveRTABSlot := pipe.io.Req_st1_RTAB_reserve
  io.flushDirtyRead := pipe.io.flushDirty_tA
  io.dataReadValid := pipe.io.read_Req_dA.valid
  io.dataReadSetIdx := pipe.io.read_Req_dA.bits.head.setIdx
}

class DCacheCoreReqPipeHazardTest extends AnyFreeSpec with ChiselScalatestTester {
  implicit val p: Parameters = (new MyConfig).toInstance

  private def initialize(dut: DCacheCoreReqPipeHazardHarness): Unit = {
    dut.io.request.valid.poke(false.B)
    clearRequest(dut)
    dut.io.tagHit.poke(true.B)
    dut.io.tagDirty.poke(false.B)
    dut.io.tagWaymask.poke(1.U)
    dut.io.hasDirty.poke(false.B)
    dut.io.coreRspReady.poke(true.B)

    dut.reset.poke(true.B)
    dut.clock.step(2)
    dut.reset.poke(false.B)
  }

  private def clearRequest(dut: DCacheCoreReqPipeHazardHarness): Unit = {
    dut.io.request.bits.instrId.poke(0.U)
    dut.io.request.bits.opcode.poke(0.U)
    dut.io.request.bits.param.poke(0.U)
    dut.io.request.bits.isKernelFlush.poke(false.B)
    dut.io.request.bits.tag.poke(0.U)
    dut.io.request.bits.setIdx.poke(0.U)
    dut.io.request.bits.perLaneAddr.foreach { lane =>
      lane.activeMask.poke(false.B)
      lane.blockOffset.poke(0.U)
      lane.wordOffset1H.poke(0.U)
    }
    dut.io.request.bits.data.foreach(_.poke(0.U))
  }

  private def driveRequest(
    dut: DCacheCoreReqPipeHazardHarness,
    opcode: BigInt,
    param: BigInt,
    setIdx: BigInt = 3
  ): Unit = {
    clearRequest(dut)
    dut.io.request.bits.opcode.poke(opcode.U)
    dut.io.request.bits.param.poke(param.U)
    dut.io.request.bits.tag.poke(1.U)
    dut.io.request.bits.setIdx.poke(setIdx.U)
    dut.io.request.bits.perLaneAddr.head.activeMask.poke(true.B)
    dut.io.request.bits.perLaneAddr.head.wordOffset1H.poke("hf".U)
    dut.io.request.valid.poke(true.B)
  }

  "every resident st1 request reserves the final RTAB slot" in {
    test(new DCacheCoreReqPipeHazardHarness) { dut =>
      initialize(dut)
      driveRequest(dut, opcode = 0, param = 0)
      dut.io.request.ready.expect(true.B)
      dut.clock.step()
      dut.io.request.valid.poke(false.B)

      dut.io.st1Valid.expect(true.B)
      dut.io.reserveRTABSlot.expect(true.B)
    }
  }

  Seq(
    ("cached read", BigInt(0), BigInt(0), false),
    ("dirty uncached read", BigInt(0), BigInt(2), true)
  ).foreach { case (name, opcode, param, dirty) =>
    s"dirty flush waits for an older st1 $name data read" in {
      test(new DCacheCoreReqPipeHazardHarness) { dut =>
        initialize(dut)
        dut.io.tagDirty.poke(dirty.B)
        dut.io.tagWaymask.poke(2.U)
        driveRequest(dut, opcode, param, setIdx = 3)
        dut.io.request.ready.expect(true.B)
        dut.clock.step()

        // Present a dirty flush while the older request occupies st1.
        dut.io.hasDirty.poke(true.B)
        driveRequest(dut, opcode = 3, param = 1, setIdx = 0)

        dut.io.st1Valid.expect(true.B)
        dut.io.flushDirtyRead.expect(false.B)
        dut.io.dataReadValid.expect(true.B)
        dut.io.dataReadSetIdx.expect((3 * dcache_NWays + 1).U)
        dut.io.request.ready.expect(false.B)
      }
    }
  }
}

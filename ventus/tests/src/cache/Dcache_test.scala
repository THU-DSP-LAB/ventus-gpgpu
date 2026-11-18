package play.cache

import chisel3._
import chisel3.experimental.BundleLiterals.AddBundleLiteralConstructor
import chisel3.util._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import top.parameters._
import L1Cache.DCache._
import L1Cache.{ MyConfig}
import chiseltest.{ChiselScalatestTester, WriteVcdAnnotation}
import org.scalatest.freespec.AnyFreeSpec
/*
class cache_MSHR_test extends AnyFreeSpec with ChiselScalatestTester{
  "mshr_test" in {
    test(new MSHRv2(25,2,1,24,10,3,4,2,2)).withAnnotations(Seq(WriteVcdAnnotation)){ dut =>
      dut.clock.step(5)
      //probe in st0
      dut.io.probe.valid.poke(true.B)
      dut.io.probe.bits.blockAddr.poke(2.U)//set 0
      dut.clock.step(1)
      //missreq
      dut.io.probe.valid.poke(false.B)
      dut.io.missReq.valid.poke(true.B)
      dut.io.missReq.bits.blockAddr.poke(2.U)
      dut.io.missReq.bits.instrId.poke(0.U)
      dut.io.missReq.bits.targetInfo.poke(1.U)
      dut.io.missReq.bits.missUncached.poke(false.B)
    }
  }
}*/
class L1RTABTest extends AnyFreeSpec with ChiselScalatestTester {
  val param = (new MyConfig).toInstance
  "RTAB_test" in {
    test(new L1RTAB()(param)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      // Initialize signals
      dut.io.RTABReq_st0.valid.poke(false.B)
      dut.io.RTABReq_st1.valid.poke(false.B)
      dut.io.RTABUpdate.valid.poke(false.B)
      dut.io.pushedWSHRIdxUpdate.valid.poke(false.B)
      dut.io.coreReq_replay.ready.poke(true.B)
      dut.io.mshrFull.poke(false.B)
      dut.clock.step(1)
      // Step 1: Send a stage 0 request

      //st0Req.CoreReqData.tag.poke(0x123.U)
      //st0Req.CoreReqData.setIdx.poke(0x456.U)
      //st0Req.wshrIdx.poke(0.U)
      //st0Req.mshrIdx.poke(0.U)
      //dut.io.RTABReq_st0.bits.poke(st0Req)
      //dut.io.RTABReq_st0.valid.poke(true.B)
      //dut.clock.step(1)
      //dut.io.RTABReq_st0.valid.poke(false.B)
      // Step 2: Send a stage 1 request
      dut.io.RTABReq_st1.bits.CoreReqData.tag.poke(0x1234.U)
      dut.io.RTABReq_st1.bits.CoreReqData.setIdx.poke(0x56.U)
      dut.io.RTABReq_st1.bits.wshrIdx.poke(1.U)
      dut.io.RTABReq_st1.bits.mshrIdx.poke(1.U)
      dut.io.RTABReq_st1.bits.ReqType.poke(6.U)
      dut.io.RTABReq_st1.valid.poke(true.B)

      dut.io.RTABReq_st0.bits.CoreReqData.tag.poke(0x1234.U)
      dut.io.RTABReq_st0.bits.CoreReqData.setIdx.poke(0x56.U)
      dut.io.RTABReq_st0.valid.poke(true.B)
      dut.clock.step(1)
      dut.io.RTABReq_st1.valid.poke(false.B)
      dut.io.RTABReq_st0.valid.poke(false.B)
      // new req st1
      //dut.clock.step(1)
      //dut.io.RTABReq_st1.bits.CoreReqData.tag.poke(0x2345.U)
      //dut.io.RTABReq_st1.bits.CoreReqData.setIdx.poke(0x67.U)
      //dut.io.RTABReq_st1.bits.wshrIdx.poke(2.U)
      //dut.io.RTABReq_st1.bits.mshrIdx.poke(2.U)
      //dut.io.RTABReq_st1.bits.ReqType.poke(2.U)
      //dut.io.RTABReq_st1.valid.poke(true.B)



      // Step 3: Update RTAB entry
      dut.clock.step(3)
      dut.io.RTABUpdate.bits.blockAddr.poke(0x123456.U)
      dut.io.RTABUpdate.bits.wshrIdx.poke(2.U)
      dut.io.RTABUpdate.bits.mshrIdx.poke(1.U)
      dut.io.RTABUpdate.bits.updateType.poke(0.U)
      dut.io.RTABUpdate.valid.poke(true.B)
      dut.clock.step(1)
      dut.io.RTABUpdate.valid.poke(false.B)
      //dut.clock.step(3)
      //dut.io.RTABUpdate.bits.blockAddr.poke(0x234567.U)
      //dut.io.RTABUpdate.bits.wshrIdx.poke(1.U)
      //dut.io.RTABUpdate.bits.mshrIdx.poke(2.U)
      //dut.io.RTABUpdate.bits.updateType.poke(0.U)
      //dut.io.RTABUpdate.valid.poke(true.B)
      //dut.clock.step(1)
      //dut.io.RTABUpdate.valid.poke(false.B)
      dut.clock.step(5)

      // Step 4: Update WSHR index
      //val wshrUpdate = new WSHRIdxUpdate()(param)
      //wshrUpdate.wshrIdx.poke(2.U)
      //wshrUpdate.RTABIdx.poke(0.U)
      //dut.io.pushedWSHRIdxUpdate.bits.poke(wshrUpdate)
      //dut.io.pushedWSHRIdxUpdate.valid.poke(true.B)
      //dut.clock.step(1)
      //dut.io.pushedWSHRIdxUpdate.valid.poke(false.B)

      // Step 5: Check replay request
      //dut.clock.step(5) // Allow time for replay logic to process
      //dut.io.coreReq_replay.valid.expect(true.B)
      //dut.io.coreReq_replay.bits.tag.expect(0x123.U)
      //dut.io.coreReq_replay.bits.setIdx.expect(0x456.U)
    }
  }
}
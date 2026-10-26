package play.cache

import chisel3._
import chiseltest._
import config.config.Parameters
import L1Cache.MyConfig
import L1Cache.DCache._
import org.scalatest.freespec.AnyFreeSpec

class L1RTABReplayOrderingTest extends AnyFreeSpec with ChiselScalatestTester {
  implicit private val p: Parameters = (new MyConfig).toInstance

  for ((preLinked, full, waitWSHR) <- Seq((true, false, false),
      (false, false, false), (true, true, false), (true, false, true))) {
  s"a load waits for replay completion (pre-linked=$preLinked, full=$full, WSHR=$waitWSHR)" in {
    test(new L1RTAB) { dut =>
      dut.io.coreReq_replay.ready.poke(false.B)
      dut.io.replayResult.get.valid.poke(false.B)
      dut.io.replayResult.get.bits.poke(false.B)
      dut.io.RTABReq_st0.valid.poke(false.B)
      dut.io.RTABReq_st1.valid.poke(false.B)
      dut.io.RTABUpdate.valid.poke(false.B)
      dut.io.RTABUpdate.bits.blockAddr.poke(0.U)
      dut.io.RTABUpdate.bits.wshrIdx.poke(0.U)
      dut.io.RTABUpdate.bits.mshrIdx.poke(0.U)
      dut.io.RTABUpdate.bits.updateType.poke(0.U)
      dut.io.mshrFull.poke(false.B)
      dut.io.LRexist.poke(false.B)
      dut.io.refillWrite_valid.poke(false.B)
      dut.io.fillCommit_valid.poke(false.B)
      dut.io.fillCommit_blockAddr.poke(0.U)
      dut.io.pushedWSHRIdxUpdate.valid.poke(false.B)
      dut.io.pushedWSHRIdxUpdate.bits.wshrIdx.poke(0.U)
      dut.io.pushedWSHRIdxUpdate.bits.RTABIdx.poke(0.U)
      for (port <- Seq(dut.io.RTABReq_st0, dut.io.RTABReq_st1)) {
        port.bits.ReqType.poke(0.U)
        port.bits.wshrIdx.poke(0.U)
        port.bits.mshrIdx.poke(0.U)
        port.bits.fillAlreadyCommitted.poke(false.B)
        val req = port.bits.CoreReqData
        req.instrId.poke(1.U)
        req.opcode.poke(1.U)
        req.param.poke(0.U)
        req.tag.poke("h90218".U)
        req.setIdx.poke(25.U)
        req.data.foreach(_.poke("hc0000000".U))
        req.perLaneAddr.zipWithIndex.foreach { case (lane, i) =>
          lane.activeMask.poke(true.B)
          lane.blockOffset.poke(i.U)
          lane.wordOffset1H.poke(15.U)
        }
      }
      dut.clock.step()
      dut.io.RTABReq_st1.valid.poke(true.B)
      dut.clock.step()
      dut.io.RTABReq_st1.valid.poke(false.B)
      dut.io.RTABReq_st0.bits.CoreReqData.instrId.poke(2.U)
      dut.io.RTABReq_st0.bits.CoreReqData.opcode.poke(0.U)
      dut.io.checkRTABhit.expect(true.B)
      dut.io.RTABReq_st0.valid.poke(preLinked.B)
      dut.clock.step()
      dut.io.RTABReq_st0.valid.poke(false.B)
      if (full) {
        dut.io.RTABReq_st1.valid.poke(true.B)
        for (id <- 3 to 4) {
          dut.io.RTABReq_st1.bits.CoreReqData.instrId.poke(id.U)
          dut.io.RTABReq_st1.bits.CoreReqData.tag.poke((BigInt("90218", 16) + id).U)
          dut.clock.step()
        }
        dut.io.RTABReq_st1.valid.poke(false.B)
        dut.io.RTABReq_st1.bits.CoreReqData.instrId.poke(1.U)
        dut.io.RTABReq_st1.bits.CoreReqData.tag.poke("h90218".U)
        dut.io.RTAB_full.expect(true.B)
      }
      dut.io.coreReq_replay.valid.expect(true.B)
      dut.io.coreReq_replay.bits.instrId.expect(1.U)
      dut.io.coreReq_replay.ready.poke(true.B)
      dut.clock.step()
      dut.io.coreReq_replay.ready.poke(false.B)
      dut.io.coreReq_replay.valid.expect(false.B)
      dut.clock.step(2)
      dut.io.coreReq_replay.valid.expect(false.B)

      dut.io.RTABReq_st1.valid.poke(true.B)
      dut.io.RTABReq_st1.bits.ReqType.poke((if (waitWSHR) 7 else 12).U)
      dut.io.RTABReq_st1.bits.wshrIdx.poke(1.U)
      dut.io.replayResult.get.valid.poke(true.B)
      dut.io.replayResult.get.bits.poke(true.B)
      dut.io.RTABpushedIdx.expect(0.U)
      dut.clock.step()
      dut.io.RTABReq_st1.valid.poke(false.B)
      dut.io.replayResult.get.valid.poke(false.B)
      dut.io.coreReq_replay.valid.expect(false.B)
      if (waitWSHR) {
        // An unrelated outgoing Put must not overwrite this waiter's index.
        dut.io.pushedWSHRIdxUpdate.valid.poke(true.B)
        dut.io.pushedWSHRIdxUpdate.bits.RTABIdx.poke(0.U)
        dut.io.pushedWSHRIdxUpdate.bits.wshrIdx.poke(2.U)
        dut.clock.step()
        dut.io.pushedWSHRIdxUpdate.valid.poke(false.B)
        dut.io.RTABUpdate.valid.poke(true.B)
        dut.io.RTABUpdate.bits.updateType.poke(1.U)
        dut.io.RTABUpdate.bits.wshrIdx.poke(1.U)
        dut.clock.step()
        dut.io.RTABUpdate.valid.poke(false.B)
      } else {
        dut.io.fillCommit_blockAddr.poke(((BigInt("90218", 16) << 5) | 25).U)
        dut.io.fillCommit_valid.poke(true.B)
        dut.clock.step()
        dut.io.fillCommit_valid.poke(false.B)
      }
      dut.io.coreReq_replay.valid.expect(true.B)
      dut.io.coreReq_replay.bits.instrId.expect(1.U)
      dut.io.coreReq_replay.ready.poke(true.B)
      dut.clock.step()
      dut.io.coreReq_replay.ready.poke(false.B)
      dut.io.replayResult.get.valid.poke(true.B)
      dut.io.replayResult.get.bits.poke(false.B)
      dut.io.RTABReq_st0.valid.poke((!preLinked).B)
      dut.clock.step()
      dut.io.RTABReq_st0.valid.poke(false.B)
      dut.io.replayResult.get.valid.poke(false.B)
      dut.io.coreReq_replay.valid.expect(true.B)
      dut.io.coreReq_replay.bits.instrId.expect(2.U)
    }
  }
  }
}

package play.cache

import chisel3._
import chiseltest._
import config.config.Parameters
import L1Cache.MyConfig
import L1Cache.DCache._
import org.scalatest.freespec.AnyFreeSpec
import top.parameters._

/**
  * A sampled image load reaches DCache as a sparse vector request: only the
  * lanes which reached closest-hit are active and each selects a different
  * word of one cache line.  Check both the miss response and its later hit
  * instead of relying on a full-warp contiguous-load test.
  */
class DCacheSparseLoadTest extends AnyFreeSpec with ChiselScalatestTester {
  implicit private val p: Parameters = (new MyConfig).toInstance

  private val activeLanes = Map(1 -> 0, 3 -> 7, 5 -> 19, 12 -> 31)
  private val lineBase = BigInt("90001000", 16)
  private def driveRequest(dut: DataCachev2, instrId: Int = 3,
      base: BigInt = lineBase, lanes: Map[Int, Int] = activeLanes): Unit = {
    dut.io.coreReq.bits.instrId.poke(instrId.U)
    dut.io.coreReq.bits.opcode.poke(0.U)
    dut.io.coreReq.bits.param.poke(0.U)
    dut.io.coreReq.bits.tag.poke((base >> 12).U)
    dut.io.coreReq.bits.setIdx.poke(((base >> 7) & 0x1f).U)
    for (lane <- 0 until num_thread) {
      dut.io.coreReq.bits.perLaneAddr(lane).activeMask.poke(lanes.contains(lane).B)
      dut.io.coreReq.bits.perLaneAddr(lane).blockOffset.poke(lanes.getOrElse(lane, 0).U)
      dut.io.coreReq.bits.perLaneAddr(lane).wordOffset1H.poke(15.U)
      dut.io.coreReq.bits.data(lane).poke(0.U)
    }
  }

  private def driveMemoryIdle(dut: DataCachev2): Unit = {
    dut.io.memRsp.valid.poke(false.B)
    dut.io.memRsp.bits.d_opcode.poke(0.U)
    dut.io.memRsp.bits.d_param.poke(0.U)
    dut.io.memRsp.bits.d_source.poke(0.U)
    dut.io.memRsp.bits.d_addr.poke(0.U)
    dut.io.memRsp.bits.d_data.foreach(_.poke(0.U))
  }

  private def waitFor(dut: DataCachev2, condition: => Boolean, what: String): Unit = {
    var cycles = 0
    while (!condition && cycles < 200) {
      dut.clock.step()
      cycles += 1
    }
    assert(condition, s"timed out waiting for $what")
  }

  private def expectSparseResponse(dut: DataCachev2, instrId: Int = 3,
      lanes: Map[Int, Int] = activeLanes, dataBase: BigInt = BigInt("55000000", 16)): Unit = {
    dut.io.coreRsp.bits.instrId.expect(instrId.U)
    for (lane <- 0 until num_thread) {
      dut.io.coreRsp.bits.activeMask(lane).expect(lanes.contains(lane).B)
      // DCache's payload for an inactive lane is intentionally don't-care;
      // downstream writeback must use activeMask before observing it.
      lanes.get(lane).foreach { word =>
        dut.io.coreRsp.bits.data(lane).expect((dataBase + word).U)
      }
    }
  }

  private def sendRequestAndCaptureMiss(dut: DataCachev2, instrId: Int,
      base: BigInt, lanes: Map[Int, Int]): BigInt = {
    driveRequest(dut, instrId, base, lanes)
    dut.io.coreReq.valid.poke(true.B)
    waitFor(dut, dut.io.coreReq.ready.peek().litToBoolean, s"request $instrId acceptance")
    dut.clock.step()
    dut.io.coreReq.valid.poke(false.B)
    waitFor(dut, dut.io.memReq.get.valid.peek().litToBoolean, s"request $instrId miss")
    dut.io.memReq.get.bits.a_addr.get.expect(base.U)
    val source = dut.io.memReq.get.bits.a_source.peek().litValue
    dut.clock.step()
    source
  }

  private def returnMiss(dut: DataCachev2, source: BigInt, base: BigInt,
      dataBase: BigInt): Unit = {
    dut.io.memRsp.bits.d_opcode.poke(1.U)
    dut.io.memRsp.bits.d_param.poke(0.U)
    dut.io.memRsp.bits.d_source.poke(source.U)
    dut.io.memRsp.bits.d_addr.poke(base.U)
    dut.io.memRsp.bits.d_data.zipWithIndex.foreach { case (word, index) =>
      word.poke((dataBase + index).U)
    }
    dut.io.memRsp.valid.poke(true.B)
    waitFor(dut, dut.io.memRsp.ready.peek().litToBoolean, s"response for ${base.toString(16)}")
    dut.clock.step()
    driveMemoryIdle(dut)
  }

  "DCache preserves sparse lane-to-word mapping across a miss and later hit" in {
    test(new DataCachev2(None)).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      dut.io.coreReq.valid.poke(false.B)
      dut.io.coreRsp.ready.poke(true.B)
      dut.io.memReq.get.ready.poke(true.B)
      driveMemoryIdle(dut)
      dut.clock.step(2)

      driveRequest(dut)
      dut.io.coreReq.valid.poke(true.B)
      waitFor(dut, dut.io.coreReq.ready.peek().litToBoolean, "sparse request acceptance")
      dut.clock.step()
      dut.io.coreReq.valid.poke(false.B)

      waitFor(dut, dut.io.memReq.get.valid.peek().litToBoolean, "cache-line miss request")
      val source = dut.io.memReq.get.bits.a_source.peek().litValue
      dut.io.memReq.get.bits.a_addr.get.expect(lineBase.U)
      dut.clock.step()

      dut.io.memRsp.bits.d_opcode.poke(1.U)
      dut.io.memRsp.bits.d_param.poke(0.U)
      dut.io.memRsp.bits.d_source.poke(source.U)
      dut.io.memRsp.bits.d_addr.poke(lineBase.U)
      dut.io.memRsp.bits.d_data.zipWithIndex.foreach { case (word, index) =>
        word.poke((BigInt("55000000", 16) + index).U)
      }
      dut.io.memRsp.valid.poke(true.B)
      waitFor(dut, dut.io.memRsp.ready.peek().litToBoolean, "cache-line response acceptance")
      dut.clock.step()
      driveMemoryIdle(dut)

      waitFor(dut, dut.io.coreRsp.valid.peek().litToBoolean, "sparse miss response")
      expectSparseResponse(dut)
      dut.clock.step()

      driveRequest(dut)
      dut.io.coreReq.valid.poke(true.B)
      waitFor(dut, dut.io.coreReq.ready.peek().litToBoolean, "sparse hit request acceptance")
      dut.clock.step()
      dut.io.coreReq.valid.poke(false.B)
      waitFor(dut, dut.io.coreRsp.valid.peek().litToBoolean, "sparse hit response")
      expectSparseResponse(dut)
    }
  }

  "DCache replicates one fetched word to every sparse lane selecting it" in {
    test(new DataCachev2(None)).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      val duplicateWords = Map(1 -> 7, 3 -> 7, 5 -> 19, 12 -> 19)
      dut.io.coreReq.valid.poke(false.B)
      dut.io.coreRsp.ready.poke(true.B)
      dut.io.memReq.get.ready.poke(true.B)
      driveMemoryIdle(dut)
      dut.clock.step(2)

      driveRequest(dut, lanes = duplicateWords)
      dut.io.coreReq.valid.poke(true.B)
      waitFor(dut, dut.io.coreReq.ready.peek().litToBoolean, "duplicate sparse request acceptance")
      dut.clock.step()
      dut.io.coreReq.valid.poke(false.B)
      waitFor(dut, dut.io.memReq.get.valid.peek().litToBoolean, "duplicate sparse cache-line miss")
      val source = dut.io.memReq.get.bits.a_source.peek().litValue
      dut.clock.step()
      returnMiss(dut, source, lineBase, BigInt("33000000", 16))
      waitFor(dut, dut.io.coreRsp.valid.peek().litToBoolean, "duplicate sparse miss response")
      expectSparseResponse(dut, lanes = duplicateWords, dataBase = BigInt("33000000", 16))
      dut.clock.step()

      driveRequest(dut, instrId = 4, lanes = duplicateWords)
      dut.io.coreReq.valid.poke(true.B)
      waitFor(dut, dut.io.coreReq.ready.peek().litToBoolean, "duplicate sparse hit request acceptance")
      dut.clock.step()
      dut.io.coreReq.valid.poke(false.B)
      waitFor(dut, dut.io.coreRsp.valid.peek().litToBoolean, "duplicate sparse hit response")
      expectSparseResponse(dut, instrId = 4, lanes = duplicateWords,
        dataBase = BigInt("33000000", 16))
    }
  }

  "DCache preserves independent sparse masks for two outstanding cache-line misses" in {
    test(new DataCachev2(None)).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      val lowerBase = BigInt("90002000", 16)
      val upperBase = BigInt("90002080", 16)
      val lowerLanes = Map(1 -> 2, 4 -> 11)
      val upperLanes = Map(3 -> 5, 12 -> 29)
      dut.io.coreReq.valid.poke(false.B)
      dut.io.coreRsp.ready.poke(true.B)
      dut.io.memReq.get.ready.poke(true.B)
      driveMemoryIdle(dut)
      dut.clock.step(2)

      val lowerSource = sendRequestAndCaptureMiss(dut, 2, lowerBase, lowerLanes)
      val upperSource = sendRequestAndCaptureMiss(dut, 5, upperBase, upperLanes)

      // Complete in the opposite order, so no response may borrow the other
      // request's lane mask or per-lane block offset.
      returnMiss(dut, upperSource, upperBase, BigInt("77000000", 16))
      waitFor(dut, dut.io.coreRsp.valid.peek().litToBoolean, "upper sparse response")
      expectSparseResponse(dut, 5, upperLanes, BigInt("77000000", 16))
      dut.clock.step()

      returnMiss(dut, lowerSource, lowerBase, BigInt("66000000", 16))
      waitFor(dut, dut.io.coreRsp.valid.peek().litToBoolean, "lower sparse response")
      expectSparseResponse(dut, 2, lowerLanes, BigInt("66000000", 16))
    }
  }

  "DCache keeps four sampled-load masks with their out-of-order cache-line replies" in {
    test(new DataCachev2(None)).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      val requests = Seq(
        (1, BigInt("90003000", 16), Map(0 -> 1, 4 -> 9), BigInt("11000000", 16)),
        (2, BigInt("90003080", 16), Map(1 -> 7, 5 -> 7, 12 -> 25), BigInt("22000000", 16)),
        (3, BigInt("90003100", 16), Map(3 -> 2, 6 -> 18), BigInt("33000000", 16)),
        (4, BigInt("90003180", 16), Map(2 -> 31, 7 -> 11, 15 -> 11), BigInt("44000000", 16)))
      dut.io.coreReq.valid.poke(false.B)
      dut.io.coreRsp.ready.poke(true.B)
      dut.io.memReq.get.ready.poke(true.B)
      driveMemoryIdle(dut)
      dut.clock.step(2)

      val pending = requests.map { case (instrId, base, lanes, dataBase) =>
        (instrId, base, lanes, dataBase, sendRequestAndCaptureMiss(dut, instrId, base, lanes))
      }
      // This permutation deliberately crosses both arrival order and masks;
      // it models the independent texel loads issued for bilinear filtering.
      Seq(2, 0, 3, 1).foreach { index =>
        val (instrId, base, lanes, dataBase, source) = pending(index)
        returnMiss(dut, source, base, dataBase)
        waitFor(dut, dut.io.coreRsp.valid.peek().litToBoolean,
          s"sampled-load response for request $instrId")
        expectSparseResponse(dut, instrId, lanes, dataBase)
        dut.clock.step()
      }
    }
  }
}

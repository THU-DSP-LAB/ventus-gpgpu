package rtcore

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class RtPdsTableTest extends AnyFreeSpec with ChiselScalatestTester {
  private val fieldCount = RtMemTableParameters.pdsRayStateFieldCount

  private def clearPdsContext(context: RtPdsContext): Unit = {
    context.baseAddress.poke(0.U)
    context.numWarps.poke(0.U)
    context.numThreads.poke(0.U)
    context.pdsWarpTidBase.poke(0.U)
  }

  private def initialize(dut: RtPdsTable): Unit = {
    dut.io.rayStateAdmission.valid.poke(false.B)
    clearPdsContext(dut.io.rayStateAdmission.bits.pds)
    dut.io.rayStateAdmission.bits.workId.poke(0.U)
    dut.io.rayStateAdmission.bits.warpId.poke(0.U)
    dut.io.rayStateAdmission.bits.activeMask.poke(0.U)

    dut.io.hitRecordWrite.valid.poke(false.B)
    dut.io.hitRecordWrite.bits.blockAddress.poke(0.U)
    dut.io.hitRecordWrite.bits.wordMask.poke(0.U)
    dut.io.hitRecordWrite.bits.data.foreach(_.poke(0.U))

    dut.io.pdsRequest.ready.poke(true.B)
    dut.io.pdsResponse.valid.poke(false.B)
    dut.io.pdsResponse.bits.requestId.poke(0.U)
    dut.io.pdsResponse.bits.laneMask.poke(0.U)
    dut.io.pdsResponse.bits.readData.foreach(_.poke(0.U))
    dut.io.pdsResponse.bits.faultMask.poke(0.U)

    dut.io.rayStateOut.ready.poke(false.B)
    dut.io.faultOut.ready.poke(true.B)
  }

  private def admit(
      dut: RtPdsTable,
      workId: Int,
      activeMask: BigInt,
      warpId: Int = 0,
      baseAddress: BigInt = 0x1000,
      pdsWarpTidBase: Int = 64
  ): Unit = {
    dut.io.rayStateAdmission.bits.workId.poke(workId.U)
    dut.io.rayStateAdmission.bits.warpId.poke(warpId.U)
    dut.io.rayStateAdmission.bits.activeMask.poke(activeMask.U)
    dut.io.rayStateAdmission.bits.pds.baseAddress.poke(baseAddress.U)
    dut.io.rayStateAdmission.bits.pds.numWarps.poke(4.U)
    dut.io.rayStateAdmission.bits.pds.numThreads.poke(32.U)
    dut.io.rayStateAdmission.bits.pds.pdsWarpTidBase.poke(pdsWarpTidBase.U)
    dut.io.rayStateAdmission.valid.poke(true.B)
  }

  private def respond(
      dut: RtPdsTable,
      requestId: BigInt,
      laneMask: BigInt,
      field: Int,
      faultMask: BigInt = 0
  ): Unit = {
    dut.io.pdsResponse.bits.requestId.poke(requestId.U)
    dut.io.pdsResponse.bits.laneMask.poke(laneMask.U)
    dut.io.pdsResponse.bits.faultMask.poke(faultMask.U)
    for (lane <- 0 until 32) {
      dut.io.pdsResponse.bits.readData(lane).poke((field * 100 + lane).U)
    }
    dut.io.pdsResponse.valid.poke(true.B)
    dut.io.pdsResponse.ready.expect(true.B)
    dut.clock.step()
    dut.io.pdsResponse.valid.poke(false.B)
  }

  private def enqueueWrite(dut: RtPdsTable, blockAddress: Int, seed: Int): Unit = {
    dut.io.hitRecordWrite.bits.blockAddress.poke(blockAddress.U)
    dut.io.hitRecordWrite.bits.wordMask.poke("hffff".U)
    for (word <- 0 until RtCorePortParameters.pdsWriteWordCount) {
      dut.io.hitRecordWrite.bits.data(word).poke((seed + word).U)
    }
    dut.io.hitRecordWrite.valid.poke(true.B)
    dut.io.hitRecordWrite.ready.expect(true.B)
    dut.clock.step()
    dut.io.hitRecordWrite.valid.poke(false.B)
  }

  "PDS transposes sparse ray-state reads into lane rows and frees rows on issue" in {
    test(new RtPdsTable) { dut =>
      initialize(dut)

      admit(dut, workId = 7, activeMask = 0)
      dut.io.rayStateAdmission.ready.expect(true.B)
      dut.clock.step()
      dut.io.rayStateAdmission.valid.poke(false.B)
      dut.io.pdsRequest.valid.expect(false.B)

      val activeMask = (BigInt(1) << 1) | (BigInt(1) << 4)
      admit(dut, workId = 9, activeMask)
      dut.io.rayStateAdmission.ready.expect(true.B)
      dut.clock.step()

      admit(dut, workId = 10, BigInt(1) << 1)
      dut.io.rayStateAdmission.ready.expect(false.B)
      dut.io.rayStateAdmission.valid.poke(false.B)

      val requestIds = Array.fill[BigInt](fieldCount)(-1)
      for (field <- 0 until fieldCount) {
        dut.io.pdsRequest.valid.expect(true.B)
        dut.io.pdsRequest.bits.write.expect(false.B)
        dut.io.pdsRequest.bits.fieldWordOffset.expect(field.U)
        dut.io.pdsRequest.bits.readLaneMask.expect(activeMask.U)
        dut.io.pdsRequest.bits.pds.baseAddress.expect("h1000".U)
        requestIds(field) = dut.io.pdsRequest.bits.requestId.peek().litValue
        dut.clock.step()
      }

      for (field <- 0 until fieldCount) {
        respond(dut, requestIds(field), activeMask, field)
      }

      dut.io.rayStateOut.valid.expect(true.B)
      dut.io.rayStateOut.bits.workId.expect(9.U)
      dut.io.rayStateOut.bits.laneId.expect(1.U)
      for (field <- 0 until fieldCount) {
        dut.io.rayStateOut.bits.data(field).expect((field * 100 + 1).U)
      }
      dut.io.rayStateOut.ready.poke(true.B)
      dut.clock.step()

      dut.io.rayStateOut.bits.laneId.expect(4.U)
      for (field <- 0 until fieldCount) {
        dut.io.rayStateOut.bits.data(field).expect((field * 100 + 4).U)
      }
      dut.clock.step()
      dut.io.rayStateOut.ready.poke(false.B)

      admit(dut, workId = 11, activeMask)
      dut.io.rayStateAdmission.ready.expect(true.B)
    }
  }

  "PDS reports lane faults and emits native 64-byte writes" in {
    test(new RtPdsTable) { dut =>
      initialize(dut)
      val activeMask = BigInt(1) << 2
      admit(dut, workId = 19, activeMask)
      dut.io.rayStateAdmission.ready.expect(true.B)
      dut.clock.step()
      dut.io.rayStateAdmission.valid.poke(false.B)

      val requestIds = Array.fill[BigInt](fieldCount)(-1)
      for (field <- 0 until fieldCount) {
        dut.io.pdsRequest.valid.expect(true.B)
        requestIds(field) = dut.io.pdsRequest.bits.requestId.peek().litValue
        dut.clock.step()
      }
      for (field <- 0 until fieldCount) {
        val fault = if (field == fieldCount - 1) activeMask else BigInt(0)
        respond(dut, requestIds(field), activeMask, field, fault)
      }

      dut.io.faultOut.valid.expect(true.B)
      dut.io.faultOut.bits.workId.expect(19.U)
      dut.io.faultOut.bits.laneId.expect(2.U)
      dut.io.rayStateOut.valid.expect(false.B)
      dut.clock.step()

      dut.io.hitRecordWrite.bits.blockAddress.poke("h80".U)
      dut.io.hitRecordWrite.bits.wordMask.poke("h00ff".U)
      for (word <- 0 until RtCorePortParameters.pdsWriteWordCount) {
        dut.io.hitRecordWrite.bits.data(word).poke((word + 1000).U)
      }
      dut.io.hitRecordWrite.valid.poke(true.B)
      dut.io.hitRecordWrite.ready.expect(true.B)
      dut.clock.step()
      dut.io.hitRecordWrite.valid.poke(false.B)

      dut.io.pdsRequest.valid.expect(true.B)
      dut.io.pdsRequest.bits.write.expect(true.B)
      dut.io.pdsRequest.bits.writeBlockAddress.expect("h80".U)
      dut.io.pdsRequest.bits.writeWordMask.expect("h00ff".U)
      for (word <- 0 until RtCorePortParameters.pdsWriteWordCount) {
        dut.io.pdsRequest.bits.writeData(word).expect((word + 1000).U)
      }
      dut.clock.step()
      dut.io.pdsRequest.valid.expect(false.B)
    }
  }

  "PDS alternates a pending read and write on its shared request port" in {
    test(new RtPdsTable) { dut =>
      initialize(dut)
      dut.io.pdsRequest.ready.poke(false.B)
      admit(dut, workId = 23, activeMask = 1)
      dut.io.rayStateAdmission.ready.expect(true.B)
      dut.clock.step()
      dut.io.rayStateAdmission.valid.poke(false.B)

      dut.io.hitRecordWrite.bits.blockAddress.poke("hc0".U)
      dut.io.hitRecordWrite.bits.wordMask.poke("hffff".U)
      dut.io.hitRecordWrite.bits.data.foreach(_.poke("h55".U))
      dut.io.hitRecordWrite.valid.poke(true.B)
      dut.io.hitRecordWrite.ready.expect(true.B)
      dut.clock.step()
      dut.io.hitRecordWrite.valid.poke(false.B)

      dut.io.pdsRequest.ready.poke(true.B)
      dut.io.pdsRequest.valid.expect(true.B)
      dut.io.pdsRequest.bits.write.expect(true.B)
      dut.clock.step()
      dut.io.pdsRequest.valid.expect(true.B)
      dut.io.pdsRequest.bits.write.expect(false.B)
    }
  }

  "PDS retains one address context per warp" in {
    test(new RtPdsTable) { dut =>
      initialize(dut)
      dut.io.pdsRequest.ready.poke(false.B)

      admit(
        dut,
        workId = 27,
        activeMask = BigInt(1),
        warpId = 1,
        baseAddress = 0x1000,
        pdsWarpTidBase = 64
      )
      dut.io.rayStateAdmission.ready.expect(true.B)
      dut.clock.step()
      dut.io.rayStateAdmission.valid.poke(false.B)

      admit(
        dut,
        workId = 28,
        activeMask = BigInt(1) << 2,
        warpId = 2,
        baseAddress = 0x2000,
        pdsWarpTidBase = 96
      )
      dut.io.rayStateAdmission.ready.expect(true.B)
      dut.clock.step()
      dut.io.rayStateAdmission.valid.poke(false.B)

      dut.io.pdsRequest.ready.poke(true.B)
      for (_ <- 0 until fieldCount) {
        dut.io.pdsRequest.valid.expect(true.B)
        dut.io.pdsRequest.bits.pds.baseAddress.expect(0x1000.U)
        dut.io.pdsRequest.bits.pds.pdsWarpTidBase.expect(64.U)
        dut.clock.step()
      }
      for (_ <- 0 until fieldCount) {
        dut.io.pdsRequest.valid.expect(true.B)
        dut.io.pdsRequest.bits.pds.baseAddress.expect(0x2000.U)
        dut.io.pdsRequest.bits.pds.pdsWarpTidBase.expect(96.U)
        dut.clock.step()
      }
    }
  }

  "PDS accepts disjoint works and demultiplexes out-of-order responses" in {
    test(new RtPdsTable) { dut =>
      initialize(dut)
      val firstMask = BigInt(1) << 1
      val secondMask = BigInt(1) << 5
      dut.io.pdsRequest.ready.poke(false.B)

      admit(dut, workId = 31, firstMask)
      dut.io.rayStateAdmission.ready.expect(true.B)
      dut.clock.step()
      dut.io.rayStateAdmission.valid.poke(false.B)
      admit(dut, workId = 32, secondMask)
      dut.io.rayStateAdmission.ready.expect(true.B)
      dut.clock.step()
      dut.io.rayStateAdmission.valid.poke(false.B)

      dut.io.pdsRequest.ready.poke(true.B)

      val requests = Array.fill[(BigInt, Int, BigInt)](2 * fieldCount)(
        (BigInt(-1), -1, BigInt(-1))
      )
      for (index <- requests.indices) {
        dut.io.pdsRequest.valid.expect(true.B)
        dut.io.pdsRequest.bits.write.expect(false.B)
        requests(index) = (
          dut.io.pdsRequest.bits.requestId.peek().litValue,
          dut.io.pdsRequest.bits.fieldWordOffset.peek().litValue.toInt,
          dut.io.pdsRequest.bits.readLaneMask.peek().litValue
        )
        dut.clock.step()
      }
      requests.take(fieldCount).foreach { case (_, _, laneMask) =>
        assert(laneMask == firstMask)
      }
      requests.drop(fieldCount).foreach { case (_, _, laneMask) =>
        assert(laneMask == secondMask)
      }

      requests.reverse.foreach { case (requestId, field, laneMask) =>
        respond(dut, requestId, laneMask, field)
      }

      dut.io.rayStateOut.valid.expect(true.B)
      dut.io.rayStateOut.bits.workId.expect(31.U)
      dut.io.rayStateOut.bits.laneId.expect(1.U)
      dut.io.rayStateOut.ready.poke(true.B)
      dut.clock.step()
      dut.io.rayStateOut.bits.workId.expect(32.U)
      dut.io.rayStateOut.bits.laneId.expect(5.U)
    }
  }

  "PDS stalls at tracker capacity and reuses a returned request id" in {
    test(new RtPdsTable) { dut =>
      initialize(dut)
      val masks = Seq(BigInt(1) << 0, BigInt(1) << 1, BigInt(1) << 2)
      dut.io.pdsRequest.ready.poke(false.B)
      masks.zipWithIndex.foreach { case (mask, index) =>
        admit(dut, workId = 40 + index, mask)
        dut.io.rayStateAdmission.ready.expect(true.B)
        dut.clock.step()
        dut.io.rayStateAdmission.valid.poke(false.B)
      }

      dut.io.pdsRequest.ready.poke(true.B)

      val requests = Array.fill[(BigInt, Int, BigInt)](
        RtMemTableParameters.pdsReadTrackerCount
      )((BigInt(-1), -1, BigInt(-1)))
      for (index <- requests.indices) {
        dut.io.pdsRequest.valid.expect(true.B)
        requests(index) = (
          dut.io.pdsRequest.bits.requestId.peek().litValue,
          dut.io.pdsRequest.bits.fieldWordOffset.peek().litValue.toInt,
          dut.io.pdsRequest.bits.readLaneMask.peek().litValue
        )
        dut.clock.step()
      }
      dut.io.pdsRequest.valid.expect(false.B)

      val released = requests.head
      respond(dut, released._1, released._3, released._2)
      dut.io.pdsRequest.valid.expect(true.B)
      dut.io.pdsRequest.bits.requestId.expect(released._1.U)
      dut.io.pdsRequest.bits.fieldWordOffset.expect(8.U)
      dut.io.pdsRequest.bits.readLaneMask.expect(masks(2).U)
    }
  }

  "PDS holds ray-state and fault outputs until their consumers fire" in {
    test(new RtPdsTable) { dut =>
      initialize(dut)
      val activeMask = BigInt(1) << 3
      admit(dut, workId = 51, activeMask)
      dut.io.rayStateAdmission.ready.expect(true.B)
      dut.clock.step()
      dut.io.rayStateAdmission.valid.poke(false.B)

      val requestIds = Array.fill[BigInt](fieldCount)(-1)
      for (field <- 0 until fieldCount) {
        requestIds(field) = dut.io.pdsRequest.bits.requestId.peek().litValue
        dut.clock.step()
      }
      for (field <- 0 until fieldCount) {
        respond(dut, requestIds(field), activeMask, field)
      }

      for (_ <- 0 until 4) {
        dut.io.rayStateOut.valid.expect(true.B)
        dut.io.rayStateOut.bits.workId.expect(51.U)
        dut.io.rayStateOut.bits.laneId.expect(3.U)
        dut.clock.step()
      }
      admit(dut, workId = 52, activeMask)
      dut.io.rayStateAdmission.ready.expect(false.B)
      dut.io.rayStateAdmission.valid.poke(false.B)
      dut.io.rayStateOut.ready.poke(true.B)
      dut.clock.step()
      dut.io.rayStateOut.ready.poke(false.B)
      admit(dut, workId = 52, activeMask)
      dut.io.rayStateAdmission.ready.expect(true.B)
      dut.clock.step()
    }

    test(new RtPdsTable) { dut =>
      initialize(dut)
      dut.io.faultOut.ready.poke(false.B)
      val activeMask = BigInt(1) << 4
      admit(dut, workId = 53, activeMask)
      dut.io.rayStateAdmission.ready.expect(true.B)
      dut.clock.step()
      dut.io.rayStateAdmission.valid.poke(false.B)

      val requestIds = Array.fill[BigInt](fieldCount)(-1)
      for (field <- 0 until fieldCount) {
        requestIds(field) = dut.io.pdsRequest.bits.requestId.peek().litValue
        dut.clock.step()
      }
      for (field <- 0 until fieldCount) {
        val faultMask = if (field == fieldCount - 1) activeMask else BigInt(0)
        respond(dut, requestIds(field), activeMask, field, faultMask)
      }

      for (_ <- 0 until 4) {
        dut.io.faultOut.valid.expect(true.B)
        dut.io.faultOut.bits.workId.expect(53.U)
        dut.io.faultOut.bits.laneId.expect(4.U)
        dut.io.rayStateOut.valid.expect(false.B)
        dut.clock.step()
      }
      admit(dut, workId = 54, activeMask)
      dut.io.rayStateAdmission.ready.expect(false.B)
      dut.io.rayStateAdmission.valid.poke(false.B)
      dut.io.faultOut.ready.poke(true.B)
      dut.clock.step()
      admit(dut, workId = 54, activeMask)
      dut.io.rayStateAdmission.ready.expect(true.B)
    }
  }

  "PDS backpressures full ray-state and write row banks" in {
    test(new RtPdsTable) { dut =>
      initialize(dut)
      dut.io.pdsRequest.ready.poke(false.B)
      val fullMask = (BigInt(1) << RtMemTableParameters.pdsRowCount) - 1
      admit(dut, workId = 61, fullMask)
      dut.io.rayStateAdmission.ready.expect(true.B)
      dut.clock.step()
      dut.io.rayStateAdmission.valid.poke(false.B)

      admit(dut, workId = 62, BigInt(1))
      dut.io.rayStateAdmission.ready.expect(false.B)
      admit(dut, workId = 63, BigInt(0))
      dut.io.rayStateAdmission.ready.expect(true.B)
      dut.io.rayStateAdmission.valid.poke(false.B)
      dut.io.hitRecordWrite.valid.poke(true.B)
      dut.io.hitRecordWrite.ready.expect(false.B)
      dut.io.hitRecordWrite.valid.poke(false.B)
    }

    test(new RtPdsTable) { dut =>
      initialize(dut)
      dut.io.pdsRequest.ready.poke(false.B)
      for (row <- 0 until RtMemTableParameters.pdsRowCount) {
        enqueueWrite(dut, row * RtCorePortParameters.pdsWriteBytes, row * 100)
      }
      dut.io.hitRecordWrite.valid.poke(true.B)
      dut.io.hitRecordWrite.ready.expect(false.B)
      dut.io.hitRecordWrite.valid.poke(false.B)

      dut.io.pdsRequest.ready.poke(true.B)
      dut.io.pdsRequest.valid.expect(true.B)
      dut.io.pdsRequest.bits.write.expect(true.B)
      dut.clock.step()
      dut.io.hitRecordWrite.ready.expect(true.B)
    }
  }

  "PDS repeatedly alternates reads and writes while both remain pending" in {
    test(new RtPdsTable) { dut =>
      initialize(dut)
      dut.io.pdsRequest.ready.poke(false.B)
      admit(dut, workId = 71, activeMask = 1)
      dut.io.rayStateAdmission.ready.expect(true.B)
      dut.clock.step()
      dut.io.rayStateAdmission.valid.poke(false.B)
      for (write <- 0 until 4) {
        enqueueWrite(
          dut,
          (write + 1) * RtCorePortParameters.pdsWriteBytes,
          2000 + write * 100
        )
      }

      dut.io.pdsRequest.ready.poke(true.B)
      val expected = Seq(true, false, true, false, true, false, true, false)
      expected.foreach { isWrite =>
        dut.io.pdsRequest.valid.expect(true.B)
        dut.io.pdsRequest.bits.write.expect(isWrite.B)
        dut.clock.step()
      }
    }
  }
}

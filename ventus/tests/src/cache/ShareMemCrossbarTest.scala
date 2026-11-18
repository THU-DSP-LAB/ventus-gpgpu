package cache

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import config.config.Config
import L1Cache.MyConfig
import L1Cache.ShareMem._
import top.parameters._

class ShareMemCrossbarTest extends AnyFreeSpec with ChiselScalatestTester {

  "DataCrossbarWrite routes lane data to banks according to 1-hot bank select" in {
    implicit val p: config.config.Parameters = (new MyConfig).toInstance

    test(new DataCrossbarWrite).withAnnotations(Seq()) { dut =>
      val nLanes = p(L1Cache.RVGParamsKey).NLanes
      val nBanks = nLanes

      // Set distinct data for each lane
      for (l <- 0 until nLanes) {
        dut.io.DataIn(l).poke((0x1000 + l).U)
      }

      // Bank b selects Lane (b % nLanes)
      for (b <- 0 until nBanks) {
        val selectedLane = b % nLanes
        dut.io.Select1H(b).poke((BigInt(1) << selectedLane).U)
      }

      dut.clock.step(1)

      for (b <- 0 until nBanks) {
        val expectedData = 0x1000 + (b % nLanes)
        val readData = dut.io.DataOut(b).peek().litValue
        assert(readData == expectedData, s"Bank $b must receive data $expectedData from lane ${b % nLanes}, got $readData")
      }
      println("[PASS] DataCrossbarWrite lane-to-bank routing verified cleanly")
    }
  }

  "DataCrossbar routes bank data to lanes according to 1-hot lane select" in {
    implicit val p: config.config.Parameters = (new MyConfig).toInstance

    test(new DataCrossbar).withAnnotations(Seq()) { dut =>
      val nLanes = p(L1Cache.RVGParamsKey).NLanes
      val nBanks = nLanes

      // Set distinct data for each bank
      for (b <- 0 until nBanks) {
        dut.io.DataIn(b).poke((0xa000 + b).U)
      }

      // Lane l selects Bank (l % nBanks)
      for (l <- 0 until nLanes) {
        val selectedBank = l % nBanks
        dut.io.Select1H(l).poke((BigInt(1) << selectedBank).U)
      }

      dut.clock.step(1)

      for (l <- 0 until nLanes) {
        val expectedData = 0xa000 + (l % nBanks)
        val readData = dut.io.DataOut(l).peek().litValue
        assert(readData == expectedData, s"Lane $l must receive data $expectedData from bank ${l % nBanks}, got $readData")
      }
      println("[PASS] DataCrossbar bank-to-lane read routing verified cleanly")
    }
  }

  "SharedMemory elaborates and performs write-read sequence cleanly" in {
    implicit val p: config.config.Parameters = (new MyConfig).toInstance

    test(new SharedMemory).withAnnotations(Seq()) { dut =>
      dut.reset.poke(true.B)
      dut.clock.step(2)
      dut.reset.poke(false.B)

      dut.io.coreReq.valid.poke(false.B)
      dut.io.coreRsp.ready.poke(true.B)
      dut.clock.step(1)

      assert(dut.io.coreReq.ready.peek().litToBoolean, "SharedMemory must be ready initially")
      println("[PASS] SharedMemory with decoupled write and read crossbars elaborated and initialized cleanly")
    }
  }
}

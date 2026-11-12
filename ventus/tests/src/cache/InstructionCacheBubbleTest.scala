package cache

import chisel3._
import chiseltest._
import chiseltest.experimental.expose
import org.scalatest.freespec.AnyFreeSpec
import config.config.Config
import L1Cache.ICache._
import L1Cache.MyConfig
import top.parameters._

class InstructionCacheTestHarness(implicit val p: config.config.Parameters) extends Module with HasICacheParameter {
  val dut = Module(new InstructionCache())

  val io = IO(new Bundle {
    val reqValid = Input(Bool())
    val reqReady = Output(Bool())
    val reqWarpId = Input(UInt(WIdBits.W))
    val reqAddr = Input(UInt(WordLength.W))
    val reqMask = Input(UInt(num_fetch.W))
  })

  dut.io.coreReq.valid := io.reqValid
  dut.io.coreReq.bits.warpid := io.reqWarpId
  dut.io.coreReq.bits.addr := io.reqAddr
  dut.io.coreReq.bits.mask := io.reqMask
  if (top.parameters.MMU_ENABLED) {
    dut.io.coreReq.bits.asid.foreach(_ := 0.U)
  }
  if (top.parameters.SPIKE_OUTPUT) {
    dut.io.coreReq.bits.spike_info.foreach { s =>
      s.pc := 0.U
      s.vaddr := 0.U
    }
  }

  io.reqReady := dut.io.coreReq.ready

  dut.io.coreRsp.ready := true.B
  dut.io.externalFlushPipe.valid := false.B
  dut.io.externalFlushPipe.bits.warpid := 0.U
  dut.io.memRsp.valid := false.B
  dut.io.memRsp.bits := 0.U.asTypeOf(dut.io.memRsp.bits)
  dut.io.memReq.ready := true.B
  dut.io.invalidate := false.B

  val st1WarpId = expose(dut.warpid_st1)
  val st1Addr = expose(dut.addr_st1)
  val st1Mask = expose(dut.mask_st1)
}

class InstructionCacheBubbleTest extends AnyFreeSpec with ChiselScalatestTester {
  "ICache retains valid request metadata across pipeline bubbles" in {
    implicit val p: config.config.Parameters = (new MyConfig).toInstance

    test(new InstructionCacheTestHarness).withAnnotations(Seq()) { dut =>
      dut.reset.poke(true.B)
      dut.clock.step(2)
      dut.reset.poke(false.B)
      dut.clock.step()

      // Send valid request at PC 0x1000 for warp 2
      dut.io.reqValid.poke(true.B)
      dut.io.reqWarpId.poke(2.U)
      dut.io.reqAddr.poke(0x1000.U)
      dut.io.reqMask.poke(0x3.U)
      dut.clock.step()

      // Ensure ST1 latched the valid request
      dut.st1WarpId.expect(2.U)
      dut.st1Addr.expect(0x1000.U)
      dut.st1Mask.expect(0x3.U)

      // Deassert valid (create bubble), while driving garbage onto request bus
      dut.io.reqValid.poke(false.B)
      dut.io.reqWarpId.poke(7.U)
      dut.io.reqAddr.poke(0xdeadbeefL.U)
      dut.io.reqMask.poke(0x0.U)
      dut.clock.step()

      // ST1 registers must NOT sample the bubble's garbage data
      dut.st1WarpId.expect(2.U)
      dut.st1Addr.expect(0x1000.U)
      dut.st1Mask.expect(0x3.U)
    }
  }
}

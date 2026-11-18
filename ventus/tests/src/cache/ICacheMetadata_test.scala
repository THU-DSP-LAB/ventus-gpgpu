package play.cache

import L1Cache.ICache._
import L1Cache.MyConfig
import chisel3._
import chisel3.util._
import chiseltest._
import chiseltest.experimental.expose
import config.config.Parameters
import org.scalatest.freespec.AnyFreeSpec

class ICacheMetadataHarness(implicit p: Parameters) extends Module {
  val io = IO(new Bundle {
    val request = Flipped(Valid(new ICachePipeReq(mmu.SV32)))
    val requestReady = Output(Bool())
    val response = Valid(new ICachePipeRsp)
  })

  val cache = Module(new InstructionCache(None))
  cache.io.coreReq.valid := io.request.valid
  cache.io.coreReq.bits := io.request.bits
  io.requestReady := cache.io.coreReq.ready

  io.response.valid := cache.io.coreRsp.valid
  io.response.bits := cache.io.coreRsp.bits
  cache.io.coreRsp.ready := true.B

  cache.io.externalFlushPipe.valid := false.B
  cache.io.externalFlushPipe.bits := 0.U.asTypeOf(cache.io.externalFlushPipe.bits)
  cache.io.memRsp.valid := false.B
  cache.io.memRsp.bits := 0.U.asTypeOf(cache.io.memRsp.bits)
  cache.io.memReq.ready := true.B
  cache.io.invalidate := false.B

  val warpidSt1 = expose(cache.warpid_st1)
  val maskSt1 = expose(cache.mask_st1)
  val addrSt1 = expose(cache.addr_st1)
}

class ICacheMetadataTest extends AnyFreeSpec with ChiselScalatestTester {
  implicit val p: Parameters = (new MyConfig).toInstance

  private def pokeRequest(
    dut: ICacheMetadataHarness,
    valid: Boolean,
    addr: BigInt,
    mask: BigInt,
    warpid: BigInt
  ): Unit = {
    dut.io.request.valid.poke(valid.B)
    dut.io.request.bits.addr.poke(addr.U)
    dut.io.request.bits.mask.poke(mask.U)
    dut.io.request.bits.warpid.poke(warpid.U)
  }

  "invalid bubbles do not overwrite request or response metadata" in {
    test(new ICacheMetadataHarness) { dut =>
      pokeRequest(dut, valid = false, addr = 0, mask = 0, warpid = 0)
      dut.reset.poke(true.B)
      dut.clock.step(2)
      dut.reset.poke(false.B)

      val addrA = BigInt("00000100", 16)
      pokeRequest(dut, valid = true, addrA, mask = 1, warpid = 3)
      dut.io.requestReady.expect(true.B)
      dut.clock.step()
      dut.warpidSt1.expect(3.U)
      dut.maskSt1.expect(1.U)
      dut.addrSt1.expect(addrA.U)

      pokeRequest(dut, valid = false, addr = BigInt("deadbe00", 16), mask = 2, warpid = 7)
      dut.clock.step()
      dut.warpidSt1.expect(3.U)
      dut.maskSt1.expect(1.U)
      dut.addrSt1.expect(addrA.U)

      pokeRequest(dut, valid = false, addr = BigInt("cafeba00", 16), mask = 0, warpid = 6)
      dut.clock.step()
      dut.warpidSt1.expect(3.U)
      dut.maskSt1.expect(1.U)
      dut.addrSt1.expect(addrA.U)

      val addrB = BigInt("00000200", 16)
      pokeRequest(dut, valid = true, addrB, mask = 3, warpid = 5)
      dut.io.requestReady.expect(true.B)
      dut.clock.step()
      dut.warpidSt1.expect(5.U)
      dut.maskSt1.expect(3.U)
      dut.addrSt1.expect(addrB.U)

      pokeRequest(dut, valid = false, addr = BigInt("ffffffff", 16), mask = 0, warpid = 7)
      dut.clock.step()
      dut.io.response.valid.expect(true.B)
      dut.io.response.bits.warpid.expect(5.U)
      dut.io.response.bits.mask.expect(3.U)
      dut.io.response.bits.addr.expect(addrB.U)
      dut.warpidSt1.expect(5.U)
      dut.maskSt1.expect(3.U)
      dut.addrSt1.expect(addrB.U)
    }
  }
}

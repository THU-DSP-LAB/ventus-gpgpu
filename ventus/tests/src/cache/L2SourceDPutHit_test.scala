package play.cache

import L2cache._
import chisel3._
import chiseltest._
import freechips.rocketchip.tilelink.TLMessages.{PutFullData, PutPartialData}
import org.scalatest.freespec.AnyFreeSpec

class L2SourceDPutHitTest extends AnyFreeSpec with ChiselScalatestTester {
  private val params = InclusiveCacheParameters_lite(
    cache = CacheParameters(
      level = 2,
      ways = 2,
      sets = 2,
      l2cs = 1,
      blockBytes = 16,
      beatBytes = 16
    ),
    micro = InclusiveCacheMicroParameters(
      writeBytes = 1,
      memCycles = 2,
      portFactor = 2,
      num_warp = 2,
      num_sm = 1,
      num_sm_in_cluster = 1,
      num_cluster = 1,
      NMshrEntry = 2,
      NSets = 2,
      NInfWriteEntry = 2
    ),
    control = false,
    mmu = false
  )

  private def initialize(dut: SourceD): Unit = {
    dut.io.req.valid.poke(false.B)
    dut.io.req.bits.opcode.poke(0.U)
    dut.io.req.bits.size.poke(0.U)
    dut.io.req.bits.source.poke(0.U)
    dut.io.req.bits.tag.poke(0.U)
    dut.io.req.bits.offset.poke(0.U)
    dut.io.req.bits.put.poke(0.U)
    dut.io.req.bits.data.poke(0.U)
    dut.io.req.bits.mask.poke(0.U)
    dut.io.req.bits.param.poke(0.U)
    dut.io.req.bits.set.poke(0.U)
    dut.io.req.bits.l2cidx.poke(0.U)
    dut.io.req.bits.hit.poke(false.B)
    dut.io.req.bits.way.poke(0.U)
    dut.io.req.bits.dirty.poke(false.B)
    dut.io.req.bits.flush.poke(false.B)
    dut.io.req.bits.last_flush.poke(false.B)
    dut.io.req.bits.from_mem.poke(false.B)

    dut.io.d.ready.poke(true.B)
    dut.io.pb_pop.ready.poke(true.B)
    dut.io.pb_beat.data.poke(0.U)
    dut.io.pb_beat.mask.poke(0.U)
    dut.io.bs_radr.ready.poke(true.B)
    dut.io.bs_rdat.data.poke(0.U)
    dut.io.bs_wadr.ready.poke(true.B)
    dut.io.a.ready.poke(true.B)
  }

  private def driveHitPut(
    dut: SourceD,
    opcode: UInt,
    data: BigInt,
    mask: BigInt
  ): Unit = {
    dut.io.req.bits.opcode.poke(opcode)
    dut.io.req.bits.size.poke(2.U)
    dut.io.req.bits.source.poke(1.U)
    dut.io.req.bits.tag.poke(BigInt("80004", 16).U)
    dut.io.req.bits.offset.poke(0.U)
    dut.io.req.bits.put.poke(0.U)
    dut.io.req.bits.data.poke(data.U)
    dut.io.req.bits.mask.poke(mask.U)
    dut.io.req.bits.param.poke(0.U)
    dut.io.req.bits.set.poke(0.U)
    dut.io.req.bits.l2cidx.poke(0.U)
    dut.io.req.bits.hit.poke(true.B)
    dut.io.req.bits.way.poke(1.U)
    dut.io.req.bits.dirty.poke(false.B)
    dut.io.req.bits.flush.poke(false.B)
    dut.io.req.bits.last_flush.poke(false.B)
    dut.io.req.bits.from_mem.poke(false.B)
    dut.io.pb_beat.data.poke(data.U)
    dut.io.pb_beat.mask.poke(mask.U)
  }

  private def reset(dut: SourceD): Unit = {
    dut.reset.poke(true.B)
    dut.clock.step(2)
    dut.reset.poke(false.B)
  }

  "a hit PutPartial writes BankedStore once and does not repeat after its ack" in {
    test(new SourceD(params)) { dut =>
      initialize(dut)
      reset(dut)

      driveHitPut(dut, PutPartialData, 0, 0xf)
      dut.io.req.valid.poke(true.B)
      dut.io.req.ready.expect(true.B)
      dut.io.bs_wadr.valid.expect(true.B)
      dut.io.bs_wadr.bits.set.expect(0.U)
      dut.io.bs_wadr.bits.way.expect(1.U)
      dut.io.bs_wadr.bits.mask.expect(0xf.U)
      dut.clock.step()

      dut.io.req.valid.poke(false.B)
      dut.io.bs_wadr.valid.expect(false.B)
      dut.io.d.valid.expect(true.B)
      dut.clock.step()

      // Refill arbitration can deassert ready after the transaction completed.
      // The stale request register must not re-arm the old partial write.
      dut.io.bs_wadr.ready.poke(false.B)
      dut.clock.step()
      dut.io.bs_wadr.ready.poke(true.B)
      dut.io.bs_wadr.valid.expect(false.B)
      dut.clock.step(2)
      dut.io.bs_wadr.valid.expect(false.B)
    }
  }

  "a blocked hit Put remains pending and writes once when arbitration releases" in {
    test(new SourceD(params)) { dut =>
      initialize(dut)
      reset(dut)

      val data = BigInt("0123456789abcdef0123456789abcdef", 16)
      driveHitPut(dut, PutFullData, data, 0xffff)
      dut.io.bs_wadr.ready.poke(false.B)
      dut.io.req.valid.poke(true.B)
      dut.io.req.ready.expect(true.B)
      dut.io.bs_wadr.valid.expect(true.B)
      dut.clock.step()

      dut.io.req.valid.poke(false.B)
      dut.io.bs_wadr.valid.expect(true.B)
      dut.io.bs_wdat.data.expect(data.U)
      dut.clock.step(2)
      dut.io.bs_wadr.valid.expect(true.B)

      dut.io.bs_wadr.ready.poke(true.B)
      dut.io.bs_wadr.valid.expect(true.B)
      dut.clock.step()
      dut.io.bs_wadr.valid.expect(false.B)
      dut.io.d.valid.expect(true.B)
      dut.clock.step()

      dut.io.bs_wadr.ready.poke(false.B)
      dut.clock.step()
      dut.io.bs_wadr.ready.poke(true.B)
      dut.io.bs_wadr.valid.expect(false.B)
    }
  }
}

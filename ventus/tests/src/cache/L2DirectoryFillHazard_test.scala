package play.cache

import L2cache._
import chisel3._
import chiseltest._
import freechips.rocketchip.tilelink.TLMessages.Get
import org.scalatest.freespec.AnyFreeSpec

class MSHROwnershipHarness(params: InclusiveCacheParameters_lite) extends Module {
  val io = IO(new Bundle {
    val allocate = Input(Bool())
    val refillArrived = Input(Bool())
    val requestValid = Input(Bool())
    val directoryReady = Input(Bool())
    val directoryValid = Output(Bool())
    val owned = Output(Bool())
  })

  val mshr = Module(new MSHR(params))
  mshr.io.allocate.valid := io.allocate
  mshr.io.allocate.bits := 0.U.asTypeOf(new Status(params))
  mshr.io.allocate.bits.opcode := Get
  mshr.io.valid := io.requestValid
  mshr.io.mshr_wait := false.B
  mshr.io.mixed := false.B
  mshr.io.merge.valid := false.B
  mshr.io.merge.bits := 0.U.asTypeOf(new Merge_meta(params))
  mshr.io.sinkd.valid := io.refillArrived
  mshr.io.sinkd.bits := 0.U.asTypeOf(new SinkDResponse(params))
  mshr.io.schedule.a.ready := true.B
  mshr.io.schedule.d.ready := false.B
  mshr.io.schedule.dir.ready := io.directoryReady

  io.directoryValid := mshr.io.schedule.dir.valid
  io.owned := MSHROwnership(
    io.requestValid,
    mshr.io.schedule.a.valid,
    mshr.io.schedule.dir.valid,
    false.B
  )
}

class L2DirectoryFillHazardTest extends AnyFreeSpec with ChiselScalatestTester {
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

  "a refill remains blocked until an outstanding hit releases its way" in {
    test(new Directory_test(params)) { dut =>
      dut.io.write.valid.poke(false.B)
      dut.io.write.bits.way.poke(0.U)
      dut.io.write.bits.set.poke(0.U)
      dut.io.write.bits.data.tag.poke(0.U)
      dut.io.read.valid.poke(false.B)
      dut.io.read.bits.opcode.poke(0.U)
      dut.io.read.bits.size.poke(0.U)
      dut.io.read.bits.source.poke(0.U)
      dut.io.read.bits.tag.poke(0.U)
      dut.io.read.bits.offset.poke(0.U)
      dut.io.read.bits.put.poke(0.U)
      dut.io.read.bits.data.poke(0.U)
      dut.io.read.bits.mask.poke(0.U)
      dut.io.read.bits.param.poke(0.U)
      dut.io.read.bits.set.poke(0.U)
      dut.io.read.bits.l2cidx.poke(0.U)
      dut.io.result.ready.poke(true.B)
      dut.io.flush.poke(false.B)
      dut.io.invalidate.poke(false.B)
      dut.io.tag_match.poke(false.B)
      dut.io.flush_invalidate_src.poke(0.U)
      dut.io.resv_clear.valid.poke(false.B)
      dut.io.resv_clear.bits.set.poke(0.U)
      dut.io.resv_clear.bits.way.poke(0.U)
      dut.io.hit_resv_clear.valid.poke(false.B)
      dut.io.hit_resv_clear.bits.set.poke(0.U)
      dut.io.hit_resv_clear.bits.way.poke(0.U)

      dut.reset.poke(true.B)
      dut.clock.step(2)
      dut.reset.poke(false.B)

      var wipeCycles = 0
      while (!dut.io.ready.peek().litToBoolean && wipeCycles < 8) {
        dut.clock.step()
        wipeCycles += 1
      }
      dut.io.ready.expect(true.B)

      val residentTag = BigInt("90002", 16)
      val refillTag = BigInt("90001", 16)

      dut.io.write.bits.set.poke(0.U)
      dut.io.write.bits.way.poke(0.U)
      dut.io.write.bits.data.tag.poke(residentTag.U)
      dut.io.write.valid.poke(true.B)
      dut.io.write.ready.expect(true.B)
      dut.clock.step()
      dut.io.write.valid.poke(false.B)

      dut.io.read.bits.tag.poke(residentTag.U)
      dut.io.read.bits.set.poke(0.U)
      dut.io.read.bits.opcode.poke(Get)
      dut.io.read.valid.poke(true.B)
      dut.io.read.ready.expect(true.B)
      dut.clock.step()
      dut.io.read.valid.poke(false.B)

      // Scheduler presents the pending refill target on write.bits while
      // keeping write.valid low until this hazard output permits the commit.
      dut.io.write.bits.set.poke(0.U)
      dut.io.write.bits.way.poke(0.U)
      dut.io.write.bits.data.tag.poke(refillTag.U)

      var resultCycles = 0
      while (!dut.io.result.valid.peek().litToBoolean && resultCycles < 4) {
        dut.clock.step()
        resultCycles += 1
      }
      dut.io.result.valid.expect(true.B)
      dut.io.result.bits.hit.expect(true.B)
      dut.io.result.bits.way.expect(0.U)
      dut.io.write_hit_hazard.expect(true.B)

      dut.clock.step()
      dut.io.write_hit_hazard.expect(true.B)

      dut.io.hit_resv_clear.bits.set.poke(0.U)
      dut.io.hit_resv_clear.bits.way.poke(0.U)
      dut.io.hit_resv_clear.valid.poke(true.B)
      dut.clock.step()
      dut.io.hit_resv_clear.valid.poke(false.B)
      dut.io.write_hit_hazard.expect(false.B)
    }
  }

  "an MSHR remains owned after its request list drains until the directory fill commits" in {
    test(new MSHROwnershipHarness(params)) { dut =>
      dut.io.allocate.poke(false.B)
      dut.io.refillArrived.poke(false.B)
      dut.io.requestValid.poke(false.B)
      dut.io.directoryReady.poke(false.B)

      dut.io.allocate.poke(true.B)
      dut.io.requestValid.poke(true.B)
      dut.clock.step()
      dut.io.allocate.poke(false.B)

      // Retire the memory request, then deliver its refill.
      dut.clock.step()
      dut.io.refillArrived.poke(true.B)
      dut.clock.step()
      dut.io.refillArrived.poke(false.B)

      // The response list can drain while the directory write is blocked.
      dut.io.requestValid.poke(false.B)
      dut.io.directoryValid.expect(true.B)
      dut.io.owned.expect(true.B)
      dut.clock.step(2)
      dut.io.owned.expect(true.B)

      dut.io.directoryReady.poke(true.B)
      dut.clock.step()
      dut.io.directoryReady.poke(false.B)
      dut.io.directoryValid.expect(false.B)
      dut.io.owned.expect(false.B)
    }
  }
}

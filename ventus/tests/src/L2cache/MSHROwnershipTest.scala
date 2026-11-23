package L2cache

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

class MSHROwnershipTest extends AnyFreeSpec with ChiselScalatestTester {
  private val testParams = InclusiveCacheParameters_lite(
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
      NSets = 2
    ),
    control = false,
    mmu = false
  )

  "an MSHR remains owned after its request list drains until the directory fill commits" in {
    test(new MSHROwnershipHarness(testParams)) { dut =>
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


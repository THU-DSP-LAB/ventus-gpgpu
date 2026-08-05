package play.cache

import L2cache._
import chisel3._
import chiseltest._
import chiseltest.experimental.expose
import freechips.rocketchip.tilelink.TLMessages.Get
import org.scalatest.freespec.AnyFreeSpec

class DirectoryFlushIndexHarness(params: InclusiveCacheParameters_lite) extends Module {
  val io = IO(new Bundle {
    val invalidate = Input(Bool())
    val ready = Output(Bool())
  })

  val directory = Module(new Directory_test(params))
  directory.io.write.valid := false.B
  directory.io.write.bits := 0.U.asTypeOf(directory.io.write.bits)
  directory.io.read.valid := false.B
  directory.io.read.bits := 0.U.asTypeOf(directory.io.read.bits)
  directory.io.result.ready := true.B
  directory.io.flush := false.B
  directory.io.invalidate := io.invalidate
  directory.io.tag_match := false.B
  directory.io.flush_invalidate_src := 0.U
  directory.io.resv_clear.valid := false.B
  directory.io.resv_clear.bits := 0.U.asTypeOf(directory.io.resv_clear.bits)
  directory.io.hit_resv_clear.valid := false.B
  directory.io.hit_resv_clear.bits := 0.U.asTypeOf(directory.io.hit_resv_clear.bits)

  io.ready := directory.io.ready

  val flushSet = expose(directory.flush_set)
  val flushWay = expose(directory.flush_way)
  val flushPipelineActive = expose(directory.flush_issue_regnext)
}

class L2DirectoryAdmissionRegressionTest extends AnyFreeSpec with ChiselScalatestTester {
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

  private def clearRead(dut: Directory_test): Unit = {
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
  }

  private def initialize(dut: Directory_test): Unit = {
    dut.io.write.valid.poke(false.B)
    dut.io.write.bits.way.poke(0.U)
    dut.io.write.bits.set.poke(0.U)
    dut.io.write.bits.data.tag.poke(0.U)
    dut.io.read.valid.poke(false.B)
    clearRead(dut)
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

    var cycles = 0
    while (!dut.io.ready.peek().litToBoolean && cycles < 8) {
      dut.clock.step()
      cycles += 1
    }
    dut.io.ready.expect(true.B)
  }

  private def pokeRead(dut: Directory_test, tag: BigInt, set: BigInt = 0): Unit = {
    clearRead(dut)
    dut.io.read.bits.opcode.poke(Get)
    dut.io.read.bits.size.poke(4.U)
    dut.io.read.bits.source.poke(1.U)
    dut.io.read.bits.tag.poke(tag.U)
    dut.io.read.bits.mask.poke(BigInt("ffff", 16).U)
    dut.io.read.bits.set.poke(set.U)
    dut.io.read.valid.poke(true.B)
  }

  private def writeLine(
    dut: Directory_test,
    way: BigInt,
    tag: BigInt,
    set: BigInt = 0
  ): Unit = {
    dut.io.write.bits.way.poke(way.U)
    dut.io.write.bits.set.poke(set.U)
    dut.io.write.bits.data.tag.poke(tag.U)
    dut.io.write.valid.poke(true.B)
    dut.io.write.ready.expect(true.B)
    dut.clock.step()
    dut.io.write.valid.poke(false.B)
  }

  private def allocateMiss(
    dut: Directory_test,
    tag: BigInt,
    set: BigInt = 0
  ): BigInt = {
    pokeRead(dut, tag, set)
    dut.io.read.ready.expect(true.B)
    dut.clock.step()
    dut.io.read.valid.poke(false.B)

    var cycles = 0
    while (!dut.io.result.valid.peek().litToBoolean && cycles < 4) {
      dut.clock.step()
      cycles += 1
    }
    dut.io.result.valid.expect(true.B)
    dut.io.result.bits.hit.expect(false.B)
    val way = dut.io.result.bits.way.peek().litValue
    dut.clock.step()
    way
  }

  "a write bypass cannot evade victim-capacity admission" in {
    test(new Directory_test(params)) { dut =>
      initialize(dut)

      val way0 = allocateMiss(dut, tag = 0x101)
      val way1 = allocateMiss(dut, tag = 0x102)
      assert(way0 != way1, "the setup must reserve both invalid ways")

      pokeRead(dut, tag = 0x201)
      dut.io.read.ready.expect(false.B)

      // A same-set, same-tag fill creates the write-bypass condition. The
      // bypass must not override victim_stall while every invalid way is owned.
      dut.io.write.bits.way.poke(way0.U)
      dut.io.write.bits.set.poke(0.U)
      dut.io.write.bits.data.tag.poke(0x201.U)
      dut.io.write.valid.poke(true.B)
      dut.io.write.ready.expect(true.B)
      dut.io.read.ready.expect(false.B)
    }
  }

  "a miss invalidating its victim blocks a same-cycle second admission" in {
    test(new Directory_test(params)) { dut =>
      initialize(dut)
      writeLine(dut, way = 0, tag = 0x301)
      writeLine(dut, way = 1, tag = 0x302)

      pokeRead(dut, tag = 0x401)
      dut.io.read.ready.expect(true.B)
      dut.clock.step()

      // The first miss result fires in this cycle. Its victim becomes invalid
      // and reserved at the edge, so a second same-set miss cannot be admitted.
      pokeRead(dut, tag = 0x402)
      dut.io.result.valid.expect(true.B)
      dut.io.result.bits.hit.expect(false.B)
      dut.io.read.ready.expect(false.B)
    }
  }

  "small-configuration flush indices remain bounded through the terminal pipeline cycle" in {
    test(new DirectoryFlushIndexHarness(params)) { dut =>
      dut.io.invalidate.poke(false.B)
      dut.reset.poke(true.B)
      dut.clock.step(2)
      dut.reset.poke(false.B)

      var cycles = 0
      while (!dut.io.ready.peek().litToBoolean && cycles < 8) {
        dut.clock.step()
        cycles += 1
      }
      dut.io.ready.expect(true.B)

      dut.io.invalidate.poke(true.B)
      dut.clock.step()
      dut.io.invalidate.poke(false.B)

      var sawFlushPipeline = false
      cycles = 0
      while ((!dut.io.ready.peek().litToBoolean ||
              dut.flushPipelineActive.peek().litToBoolean) && cycles < 16) {
        if (dut.flushPipelineActive.peek().litToBoolean) {
          sawFlushPipeline = true
          assert(dut.flushSet.peek().litValue < params.cache.sets,
            s"flush set escaped the configured range: ${dut.flushSet.peek().litValue}")
          assert(dut.flushWay.peek().litValue < params.cache.ways,
            s"flush way escaped the configured range: ${dut.flushWay.peek().litValue}")
        }
        dut.clock.step()
        cycles += 1
      }

      assert(sawFlushPipeline, "the test did not observe the flush pipeline")
      assert(cycles < 16, "the small-configuration invalidate sweep did not finish")
      dut.io.ready.expect(true.B)
    }
  }
}

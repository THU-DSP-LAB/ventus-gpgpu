package L2cache

import chisel3._
import chiseltest._
import chiseltest.experimental.expose
import org.scalatest.freespec.AnyFreeSpec
import top.parameters._

class DirectoryFlushHarness(params: InclusiveCacheParameters_lite) extends Module {
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

  io.ready := directory.io.ready

  val flushSet = expose(directory.flush_set)
  val flushWay = expose(directory.flush_way)
  val flushPipelineActive = expose(directory.flush_issue_regnext)
}

class DirectoryFlushIndexTest extends AnyFreeSpec with ChiselScalatestTester {
  "L2 directory flush indices remain strictly bounded to configured sets and ways" in {
    val testCache = l2cache_cache.copy(sets = 2, ways = 2)
    val testMicro = l2cache_micro.copy(NSets = 2)
    val testParams = InclusiveCacheParameters_lite(testCache, testMicro, false, MMU_ENABLED)

    test(new DirectoryFlushHarness(testParams)).withAnnotations(Seq()) { dut =>
      dut.io.invalidate.poke(false.B)
      dut.reset.poke(true.B)
      dut.clock.step(2)
      dut.reset.poke(false.B)

      // Wait for directory reset-wipe to complete (2 sets => 2 cycles)
      var cycles = 0
      val wipeLimit = testParams.cache.sets + 8
      while (!dut.io.ready.peek().litToBoolean && cycles < wipeLimit) {
        dut.clock.step()
        cycles += 1
      }
      assert(dut.io.ready.peek().litToBoolean, s"Directory did not become ready after reset wipe (cycles=$cycles)")

      // Trigger invalidate sweep
      dut.io.invalidate.poke(true.B)
      dut.clock.step()
      dut.io.invalidate.poke(false.B)

      var sawFlush = false
      cycles = 0
      val totalLines = testParams.cache.sets * testParams.cache.ways
      val maxCycles = totalLines + 16
      while (cycles < maxCycles) {
        if (dut.flushPipelineActive.peek().litToBoolean) {
          sawFlush = true
          val s = dut.flushSet.peek().litValue.toInt
          val w = dut.flushWay.peek().litValue.toInt
          assert(s >= 0 && s < testParams.cache.sets, s"Flush set $s out of range [0, ${testParams.cache.sets})")
          assert(w >= 0 && w < testParams.cache.ways, s"Flush way $w out of range [0, ${testParams.cache.ways})")
        }
        dut.clock.step()
        cycles += 1
      }
      assert(sawFlush, "Flush operation did not activate pipeline during sweep")
    }
  }
}

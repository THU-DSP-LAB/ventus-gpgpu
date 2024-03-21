package cta_test

import chisel3._
import chisel3.experimental.BundleLiterals.AddBundleLiteralConstructor
import chisel3.util._
import chiseltest._
import cta_scheduler._
import org.scalatest.freespec.AnyFreeSpec


class test1 extends AnyFreeSpec with ChiselScalatestTester {

  println("class test1 begin")
  "Passed: wg_buffer test 1" in {
    test(new wg_buffer()).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.io.host_wg_new.initSource().setSourceClock(dut.clock)
      dut.io.host_wg_done.initSink().setSinkClock(dut.clock)

      val testSeqIn = Seq.tabulate(200){i => (new io_host2cta).Lit(_.data -> i.U) }
      val testSeqOut = Seq.tabulate(200){i => (new io_cta2host).Lit(_.wg_id -> i.U) }

      fork{
        dut.io.host_wg_new.enqueueSeq(testSeqIn)
      } .fork {
        for(i <- 0 until 200){
          dut.io.host_wg_done.expectDequeue(testSeqOut(i))
          dut.clock.step(1+(new scala.util.Random).nextInt(6))
        }
      } .join()
    }
  }
}
package play

import L1Cache.MyConfig
import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import chiseltest.simulator.WriteVcdAnnotation
import top._

class hello_test2 extends AnyFreeSpec with ChiselScalatestTester{
  "first_test" in {
    test(new GPGPU_ExtMemWrapper()).withAnnotations(Seq(WriteVcdAnnotation)) { c =>


      c.clock.step(600)
    }
  }
}
/*
class hello_test1 extends AnyFreeSpec with ChiselScalatestTester{
  "first_test" in {
    val L1param = (new MyConfig).toInstance
    test(new GPGPU_axi_top).withAnnotations(Seq(WriteVcdAnnotation)) { c =>

      c.io.m.aw.ready.poke(true.B)
      c.io.m.w.ready.poke(true.B)
      c.io.m.ar.ready.poke(true.B)


      c.io.s.aw.awvalid.poke(true.B)
      c.io.s.aw.awaddr.poke(4.U)
      c.clock.step(5)
      c.io.s.aw.awvalid.poke(false.B)
      c.io.s.w.wvalid.poke(true.B)
      c.io.s.w.wdata.poke(12.U)
      c.clock.step(5)
      c.io.s.w.wvalid.poke(false.B)
      c.io.s.b.bready.poke(true.B)
      c.clock.step(5)

      c.io.s.aw.awvalid.poke(true.B)
      c.io.s.aw.awaddr.poke(8.U)
      c.clock.step(5)
      c.io.s.aw.awvalid.poke(false.B)
      c.io.s.w.wvalid.poke(true.B)
      c.io.s.w.wdata.poke(4.U)
      c.clock.step(5)
      c.io.s.w.wvalid.poke(false.B)
      c.io.s.b.bready.poke(true.B)
      c.clock.step(5)

      c.io.s.aw.awvalid.poke(true.B)
      c.io.s.aw.awaddr.poke(12.U)
      c.clock.step(5)
      c.io.s.aw.awvalid.poke(false.B)
      c.io.s.w.wvalid.poke(true.B)
      c.io.s.w.wdata.poke(8.U)
      c.clock.step(5)
      c.io.s.w.wvalid.poke(false.B)
      c.io.s.b.bready.poke(true.B)
      c.clock.step(5)

      c.io.s.aw.awvalid.poke(true.B)
      c.io.s.aw.awaddr.poke(20.U)
      c.clock.step(5)
      c.io.s.aw.awvalid.poke(false.B)
      c.io.s.w.wvalid.poke(true.B)
      c.io.s.w.wdata.poke(32.U)
      c.clock.step(5)
      c.io.s.w.wvalid.poke(false.B)
      c.io.s.b.bready.poke(true.B)
      c.clock.step(5)

      c.io.s.aw.awvalid.poke(true.B)
      c.io.s.aw.awaddr.poke(24.U)
      c.clock.step(5)
      c.io.s.aw.awvalid.poke(false.B)
      c.io.s.w.wvalid.poke(true.B)
      c.io.s.w.wdata.poke(32.U)
      c.clock.step(5)
      c.io.s.w.wvalid.poke(false.B)
      c.io.s.b.bready.poke(true.B)
      c.clock.step(5)

      c.io.s.aw.awvalid.poke(true.B)
      c.io.s.aw.awaddr.poke(28.U)
      c.clock.step(5)
      c.io.s.aw.awvalid.poke(false.B)
      c.io.s.w.wvalid.poke(true.B)
      c.io.s.w.wdata.poke(128.U)
      c.clock.step(5)
      c.io.s.w.wvalid.poke(false.B)
      c.io.s.b.bready.poke(true.B)
      c.clock.step(5)

      c.io.s.aw.awvalid.poke(true.B)
      c.io.s.aw.awaddr.poke(0.U)
      c.clock.step(5)
      c.io.s.aw.awvalid.poke(false.B)
      c.io.s.w.wvalid.poke(true.B)
      c.io.s.w.wdata.poke(1.U)
      c.clock.step(5)
      c.io.s.w.wvalid.poke(false.B)
      c.io.s.b.bready.poke(true.B)
      c.clock.step(5)




      c.clock.step(200)
    }
  }
}
*/
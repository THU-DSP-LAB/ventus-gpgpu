package pipeline

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import pipeline.mmu._
import top.DecoupledPipe
import MemboxS._

class L2TlbWrapper(SV: SVParam) extends Module{
  val io = IO(new Bundle {
    val in = Flipped(DecoupledIO(new Bundle {
      val asid = UInt(SV.asidLen.W)
      val ptbr = UInt(SV.xLen.W)
      val vpn = UInt(SV.vpnLen.W)
      val id = UInt(8.W) // L1's id
    }))
    //  val invalidate = Flipped(ValidIO(new Bundle {
    //    val asid = UInt(SV.asidLen.W)
    //  }))
    val out = DecoupledIO(new Bundle {
      val id = UInt(8.W)
      val ppn = UInt(SV.ppnLen.W)
      val flag = UInt(8.W)
    })
    // Request Always Read!
    val mem_req = DecoupledIO(new Cache_Req(SV))
    val mem_rsp = Flipped(DecoupledIO(new Cache_Rsp(SV)))
  })

  val internal = Module(new L2Tlb(SV))

  internal.io.in <> io.in
  io.out <> internal.io.out

  internal.io.invalidate.bits.asid := 0.U
  internal.io.invalidate.valid := false.B

  val pipe_req = Module(new DecoupledPipe(io.mem_req.cloneType, 1))
  val pipe_rsp = Module(new DecoupledPipe(io.mem_rsp.cloneType, 1))
  pipe_req.io.enq <> internal.io.mem_req
  io.mem_req <> pipe_req.io.deq

  pipe_rsp.io.enq <> io.mem_rsp
  internal.io.mem_rsp <> pipe_rsp.io.deq
}

class L2Tlb_test extends AnyFreeSpec with ChiselScalatestTester {
  test(new L2TlbWrapper(SV32)).withAnnotations(Seq(WriteVcdAnnotation)){ d =>
    val memory = new Memory(BigInt("10000000", 16), SV32)
    val ptbr = memory.createRootPageTable()

    d.io.in.setSourceClock(d.clock)
    d.io.out.setSinkClock(d.clock)
    d.io.mem_req.setSinkClock(d.clock)
    d.io.mem_rsp.setSourceClock(d.clock)
  }
}

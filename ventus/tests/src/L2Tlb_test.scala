package pipeline

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._
import chisel3.experimental.VecLiterals._
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
  test(new L2TlbWrapper(mmu.SV32)).withAnnotations(Seq(WriteVcdAnnotation)){ d =>
    val memory = new Memory(BigInt("10000000", 16), MemboxS.SV32)
    val ptbr = memory.createRootPageTable()

    d.io.in.setSourceClock(d.clock)
    d.io.out.setSinkClock(d.clock)
    d.io.mem_req.setSinkClock(d.clock)
    d.io.mem_rsp.setSourceClock(d.clock)
  }
}

class L2TlbStorage_test extends AnyFreeSpec with ChiselScalatestTester {
  "L2TLB Storage" in {
    test(new L2TlbStorage(mmu.SV32)).withAnnotations(Seq(WriteVcdAnnotation)){ d =>
      println("L2TLB Test: Storage")
      println(s"${d.nSets} Sets, ${d.nWays} Ways, ${d.nSectors} Sectors per item.")

      def write(windex: Int, waymask: Int, data: L2TlbEntryA) = {
        d.io.write.valid.poke(true.B)
        d.io.write.bits.windex.poke(windex.U)
        d.io.write.bits.waymask.poke(waymask.U)
        d.io.write.bits.wdata.poke(data)
      }

      d.clock.step(2)
      write(1, 1, (new L2TlbEntryA(mmu.SV32)).Lit(
        _.asid -> 1.U,
        _.vpn -> "h80101".U,
        _.level -> 1.U,
        _.ppns -> Vec(d.nSectors, UInt(mmu.SV32.ppnLen.W)).Lit(
          (0 until d.nSectors).map{ i =>
            i -> "h3ccccc".U
          }: _*
        ),
        _.flags -> Vec(d.nSectors, UInt(8.W)).Lit(
          (0 until d.nSectors).map{ i =>
            i -> 7.U
          }: _*
        )
      ))
      d.io.rindex.poke(1.U)
      d.clock.step(1)
      d.io.write.valid.poke(false.B)

      write(2, 4, (new L2TlbEntryA(mmu.SV32)).Lit(
        _.asid -> 4.U,
        _.vpn -> "h84444".U,
        _.level -> 1.U,
        _.ppns -> Vec(d.nSectors, UInt(mmu.SV32.ppnLen.W)).Lit(
          (0 until d.nSectors).map{ i =>
            i -> "h3ababa".U
          }: _*
        ),
        _.flags -> Vec(d.nSectors, UInt(8.W)).Lit(
          (0 until d.nSectors).map{ i =>
            i -> 7.U
          }: _*
        )
      ))
      d.clock.step(1)
      d.io.rindex.poke(2.U)
      d.io.write.valid.poke(false.B)

      d.clock.step(2)
      d.io.invalidate.valid.poke(true.B)
      d.io.invalidate.bits.poke(4.U)
      d.clock.step(1)
      d.io.invalidate.valid.poke(false.B)
      d.clock.step(20)
    }
  }
}
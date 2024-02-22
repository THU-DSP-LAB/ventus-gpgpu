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



class L2Tlb_test extends AnyFreeSpec with ChiselScalatestTester {
  "L2TLB Main" in {
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
    test(new L2TlbWrapper(mmu.SV32)).withAnnotations(Seq(WriteVcdAnnotation)){ d =>
      val memory = new Memory(BigInt("10000000", 16), MemboxS.SV32)
      val ptbr = memory.createRootPageTable()
      memory.allocateMemory(ptbr, BigInt("080000000", 16), MemboxS.SV32.PageSize * 4)

      d.io.in.setSourceClock(d.clock)
      d.io.out.setSinkClock(d.clock)
      d.io.mem_req.setSinkClock(d.clock)
      d.io.mem_rsp.setSourceClock(d.clock)

      d.clock.step(3)
    }
  }
}

class L2TlbComponentTest extends AnyFreeSpec with ChiselScalatestTester {
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
  "L2TLB PTW" in {
    class PTWWrapper(SV: SVParam) extends Module{
      val io = IO(new Bundle{
        val ptw_req = Flipped(DecoupledIO(new PTW_Req(SV)))
        val ptw_rsp = DecoupledIO(new PTW_Rsp(SV))
        val mem_req = DecoupledIO(new Cache_Req(SV))
        val mem_rsp = Flipped(DecoupledIO(new Cache_Rsp(SV)))
      })
      val internal = Module(new PTW(SV))
      internal.io.ptw_req <> io.ptw_req
      io.ptw_rsp <> internal.io.ptw_rsp

      val pipe_req = Module(new DecoupledPipe(io.mem_req.bits.cloneType, 1))
      val pipe_rsp = Module(new DecoupledPipe(io.mem_rsp.bits.cloneType, 1))
      io.mem_req <> pipe_req.io.deq
      pipe_req.io.enq <> internal.io.mem_req
      pipe_rsp.io.enq <> io.mem_rsp
      internal.io.mem_rsp <> pipe_rsp.io.deq
    }

    test(new PTWWrapper(mmu.SV32)).withAnnotations(Seq(WriteVcdAnnotation)){ d =>
      val memory = new Memory(BigInt("10000000", 16), MemboxS.SV32)
      val ptbr = memory.createRootPageTable()
      val vaddr1 = BigInt("080000000", 16)
      memory.allocateMemory(ptbr, vaddr1, MemboxS.SV32.PageSize)
      println(f"V: $vaddr1%08x -> P: ${memory.addrConvert(ptbr, vaddr1)}%08x")
      var clock_cnt = 0
      d.io.ptw_req.setSourceClock(d.clock)
      d.io.ptw_rsp.setSinkClock(d.clock)
      d.io.mem_req.setSinkClock(d.clock)
      d.io.mem_rsp.setSourceClock(d.clock)
      d.clock.step(2); clock_cnt += 2

      d.io.ptw_req.enqueueNow((new PTW_Req(mmu.SV32)).Lit(
        _.vpn -> "h80000".U,
        _.ptbr -> ptbr.U,
        _.source -> 1.U
      ))
      d.clock.step(); clock_cnt += 1
      d.io.ptw_req.valid.poke(false.B)

      def checkForValid[T <: Data](port: DecoupledIO[T]): Boolean = port.valid.peek().litToBoolean
      def checkForReady[T <: Data](port: DecoupledIO[T]): Boolean = port.ready.peek().litToBoolean
      var addr: BigInt = 0; var source: BigInt = 0; var data: Seq[BigInt] = Nil
      var mem_rsp_event: Boolean = false
      d.io.ptw_rsp.ready.poke(true.B)
      while(!checkForValid(d.io.ptw_rsp) && clock_cnt <= 30){
        if(!mem_rsp_event && checkForValid(d.io.mem_req)) {
          addr = d.io.mem_req.bits.addr.peek().litValue
          source = d.io.mem_req.bits.source.peek().litValue
          data = memory.readWordsPhysical(addr, d.internal.nSectors, Array.fill(2)(true))._2.map(_.toBigInt)
          mem_rsp_event = true
          if (checkForReady(d.io.mem_req)){
            d.io.mem_req.ready.poke(false.B)
            d.io.mem_rsp.valid.poke(true.B)
          }
          else{
            d.io.mem_rsp.valid.poke(false.B)
            d.io.mem_req.ready.poke(true.B)
          }
        }
        if(mem_rsp_event && checkForReady(d.io.mem_rsp)){
          d.io.mem_rsp.enqueueNow((new Cache_Rsp(mmu.SV32)).Lit(
            _.data -> Vec(d.internal.nSectors, UInt(mmu.SV32.xLen.W)).Lit((0 until d.internal.nSectors).map{i =>
              i -> data(i).U
            }: _*),
            _.source -> source.U
          ))
          mem_rsp_event = false
        }
        d.clock.step(1); clock_cnt += 1
      }
      d.clock.step(3)
    }
  }
}
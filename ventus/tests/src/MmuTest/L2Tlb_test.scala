package MmuTest

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._
import chisel3.experimental.VecLiterals._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import pipeline._
import mmu._
import top.DecoupledPipe
import MemboxS._
import MmuTestUtils._

class L2Tlb_test extends AnyFreeSpec
  with ChiselScalatestTester
  with MMUHelpers {
  import play.TestUtils.RequestSender

  implicit val L1C: Option[L1Cache.HasRVGParameters] = None

  "L2TLB Main" in {
    class L2TlbWrapper(SV: SVParam) extends Module with L2TlbParam {
      val io = IO(new Bundle {
        val in = Vec(nBanks, Flipped(DecoupledIO(new L2TlbReq(SV))))
//        val invalidate = Flipped(ValidIO(new Bundle{
//          val asid = UInt(SV.asidLen.W)
//        }))
        val out = Vec(nBanks, DecoupledIO(new L2TlbRsp(SV)))
        // Request Always Read!
        val mem_req = Vec(nBanks, DecoupledIO(new Cache_Req(SV)))
        val mem_rsp = Vec(nBanks, Flipped(DecoupledIO(new Cache_Rsp(SV))))
        val fill_in = Flipped(ValidIO(new AsidLookupEntry(SV)))
      })

      val internal = Module(new L2TLB(SV, Debug = true))
      val lookup = Module(new AsidLookup(SV, nBanks, 16))
      (0 until nBanks).foreach{ i =>
        internal.io.in(i) <> Queue(io.in(i), 1)
        io.out(i) <> Queue(internal.io.out(i), 1)
      }

      lookup.io.lookup_req <> internal.io.asid_req
      lookup.io.lookup_rsp <> internal.io.ptbr_rsp
      lookup.io.fill_in <> io.fill_in
      internal.io.invalidate.bits.asid := 0.U
      internal.io.invalidate.valid := false.B

      val pipe_mem_req = io.mem_req.map{ req => Module(new DecoupledPipe(req.bits.cloneType, 0, insulate = true)) }
      val pipe_mem_rsp = io.mem_rsp.map{ rsp => Module(new DecoupledPipe(rsp.bits.cloneType, 0, insulate = true)) }
      (pipe_mem_req zip internal.io.mem_req).foreach{ x => x._1.io.enq <> x._2 }
      (io.mem_req zip pipe_mem_req).foreach{ x => x._1 <> x._2.io.deq }

      (pipe_mem_rsp zip io.mem_rsp).foreach{ x => x._1.io.enq <> x._2 }
      (internal.io.mem_rsp zip pipe_mem_rsp).foreach{ x => x._1 <> x._2.io.deq }
    }
    test(new L2TlbWrapper(SV32.device)).withAnnotations(Seq(WriteVcdAnnotation)){ d =>
      val nBanks = d.nBanks
      val memory = new Memory(BigInt("10000000", 16), SV32.host)
      val ptbr = memory.createRootPageTable()
      memory.allocateMemory(ptbr, BigInt("080000000", 16), SV32.host.PageSize*4)
      memory.allocateMemory(ptbr, BigInt("080400000", 16), SV32.host.PageSize*4)

      var clock_cnt = 0; var tlb_cnt = 0;
      val req_list = Seq(BigInt("080000", 16), BigInt("080400", 16), BigInt("080000", 16))
      val mem_driver = (d.io.mem_req zip d.io.mem_rsp).map{ case(req, rsp) =>
        new MMUMemPortDriverDelay(SV32)(req, rsp, memory, 5, 5)
      }
      val tlb_sender = (d.io.in zip d.io.out).map { case (i, o) =>
        new RequestSender(i, o)
      }
      req_list.foreach{ r =>
        tlb_sender((r % nBanks).toInt).add(
          (new L2TlbReq(SV32.device)).Lit(
            _.vpn -> r.U, _.asid -> 1.U, _.id -> (r % nBanks).U
          )
        )
      }

      d.io.in.foreach{_.setSourceClock(d.clock)}
      d.io.out.foreach{_.setSinkClock(d.clock)}
      d.io.mem_req.foreach{_.setSinkClock(d.clock)}
      d.io.mem_rsp.foreach{_.setSourceClock(d.clock)}
      d.io.fill_in.setSourceClock(d.clock)

      d.clock.step(); clock_cnt += 1
      d.io.fill_in.valid.poke(true.B)
      timescope {
        d.io.fill_in.bits.poke(new AsidLookupEntry(SV32.device).Lit(
          _.ptbr -> ptbr.U,
          _.asid -> 1.U,
          _.valid -> true.B
        ))
        d.clock.step(); clock_cnt += 1
      }

      while(tlb_sender.map{_.send_list.nonEmpty}.reduce(_ || _) && clock_cnt <= 100){
        tlb_sender.foreach{_.eval()}
        mem_driver.foreach{_.eval()}
        d.clock.step(); clock_cnt += 1;
      }
      d.clock.step(3)
    }
  }
}

class L2TlbComponentTest extends AnyFreeSpec
  with ChiselScalatestTester
  with MMUHelpers {
  import play.TestUtils._
  "L2TLB Storage" in {
    test(new L2TlbStorage(SV32.device)).withAnnotations(Seq(WriteVcdAnnotation)){ d =>
      println("L2TLB Test: Storage")
      println(s"${d.nSets} Sets, ${d.nWays} Ways, ${d.nSectors} Sectors per item.")

      def write(windex: Int, waymask: Int, data: L2TlbEntryA) = {
        d.io.write.valid.poke(true.B)
        d.io.write.bits.windex.poke(windex.U)
        d.io.write.bits.waymask.poke(waymask.U)
        d.io.write.bits.wdata.poke(data)
      }

      d.clock.step(2)
      write(1, 1, (new L2TlbEntryA(SV32.device)).Lit(
        _.asid -> 1.U,
        _.vpn -> "h80101".U,
        _.level -> 1.U,
        _.ppns -> Vec(d.nSectors, UInt(SV32.device.ppnLen.W)).Lit(
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

      write(2, 4, (new L2TlbEntryA(SV32.device)).Lit(
        _.asid -> 4.U,
        _.vpn -> "h84444".U,
        _.level -> 1.U,
        _.ppns -> Vec(d.nSectors, UInt(SV32.device.ppnLen.W)).Lit(
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
    class PTWWrapper(SV: SVParam, val Ways: Int) extends Module{
      val io = IO(new Bundle{
        val ptw_req = Vec(Ways, Flipped(DecoupledIO(new PTW_Req(SV))))
        val ptw_rsp = Vec(Ways, DecoupledIO(new PTW_Rsp(SV)))
        val mem_req = Vec(Ways, DecoupledIO(new Cache_Req(SV)))
        val mem_rsp = Vec(Ways, Flipped(DecoupledIO(new Cache_Rsp(SV))))
      })
      val internal = Module(new PTW(SV, Ways, debug = true))
      (0 until Ways).foreach{ i =>
        internal.io.ptw_req(i) <> Queue(io.ptw_req(i), 1)
        io.ptw_rsp(i) <> Queue(internal.io.ptw_rsp(i), 1)
      }

      val pipe_mem_req = io.mem_req.map{ req => Module(new DecoupledPipe(req.bits.cloneType, 0, insulate = true))}
      val pipe_mem_rsp = io.mem_rsp.map{ rsp => Module(new DecoupledPipe(rsp.bits.cloneType, 0, insulate = true)) }
      (io.mem_req zip pipe_mem_req).foreach{ x => x._1 <> x._2.io.deq }
      (pipe_mem_req zip internal.io.mem_req).foreach{ x => x._1.io.enq <> x._2 }
      (pipe_mem_rsp zip io.mem_rsp).foreach{ x => x._1.io.enq <> x._2 }
      (internal.io.mem_rsp zip pipe_mem_rsp).foreach{ x => x._1 <> x._2.io.deq }
    }

    test(new PTWWrapper(SV32.device, Ways = 2)).withAnnotations(Seq(WriteVcdAnnotation)){ d =>
      val Ways = d.Ways
      def makeReq(vpn: UInt, ptbr: UInt, source: UInt): PTW_Req = (new PTW_Req(SV32.device)).Lit(
        _.vpn -> vpn, _.paddr -> ptbr, _.source -> source, _.curlevel -> (SV32.device.levels-1).U
      )
      var clock_cnt = 0
      d.io.ptw_req.foreach{_.setSourceClock(d.clock)}
      d.io.ptw_rsp.foreach{_.setSinkClock(d.clock)}
      d.io.mem_req.foreach(_.setSinkClock(d.clock))
      d.io.mem_rsp.foreach(_.setSourceClock(d.clock))
      d.clock.step(2); clock_cnt += 2

      //d.io.ptw_req.enqueueNow(req)
      d.clock.step(); clock_cnt += 1
      d.io.ptw_req.foreach{_.valid.poke(false.B)}
      d.io.mem_req.foreach{_.ready.poke(true.B)}

      d.io.ptw_rsp.foreach{_.ready.poke(true.B)}

      val memory = new Memory(BigInt("10000000", 16), SV32.host)
      val ptbr = memory.createRootPageTable()
      val vaddr1 = BigInt("080000000", 16)
      memory.allocateMemory(ptbr, vaddr1, SV32.host.PageSize * 4)
      (0 until 4).foreach{ i =>
        println(f"V: ${vaddr1 + i*SV32.host.PageSize}%08x -> P: ${memory.addrConvert(ptbr, vaddr1 + i*SV32.host.PageSize)}%08x")
      }

      val mem_driver = (d.io.mem_req zip d.io.mem_rsp).map{ case (memreq, memrsp) =>
        new MMUMemPortDriverDelay(SV32)(memreq, memrsp, memory, 5, 5)
      }
      //val mem_driver = new MemPortDriver(SV32)(d.io.mem_req, d.io.mem_rsp, memory)

      val tlb_requestor = (d.io.ptw_req zip d.io.ptw_rsp).map{ case (req, rsp) =>
        new RequestSender(req, rsp)
      }
      tlb_requestor(0).add(makeReq("h80000".U, ptbr.U, 0.U))
      tlb_requestor(0).add(makeReq("h80002".U, ptbr.U, 0.U))
      tlb_requestor(1).add(makeReq("h80003".U, ptbr.U, 1.U))
      var timestamp_rsp = Array.fill(Ways)(0)

      while(tlb_requestor.map(_.send_list.nonEmpty).reduce(_ || _)
            && clock_cnt <= 100){
        if(clock_cnt == 7){
          tlb_requestor(1).add(makeReq("h80001".U, ptbr.U, 1.U))
        }
        (0 until Ways).foreach{ i =>
          if(checkForReady(tlb_requestor(i).rspPort) && checkForValid(tlb_requestor(i).rspPort))
            timestamp_rsp(i) = clock_cnt
          tlb_requestor(i).pause = if(clock_cnt <= timestamp_rsp(i) + 5) true else false
          tlb_requestor(i).eval()
        }
        mem_driver.foreach{_.eval()}
        d.clock.step(1); clock_cnt += 1
      }
      d.clock.step(3)
    }
  }
}

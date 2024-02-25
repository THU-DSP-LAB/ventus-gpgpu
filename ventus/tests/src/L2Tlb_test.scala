package play

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._
import chisel3.experimental.VecLiterals._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import pipeline._
import pipeline.mmu._
import top.DecoupledPipe
import MemboxS._

case class SVPair[A <: MemboxS.BaseSV, B <: mmu.SVParam](
  val host : A,
  val device : B
)

object IOHelpers {
  def checkForValid[T <: Data](port: DecoupledIO[T]): Boolean = port.valid.peek().litToBoolean
  def checkForReady[T <: Data](port: DecoupledIO[T]): Boolean = port.ready.peek().litToBoolean
  val SV32 = SVPair(host = MemboxS.SV32, device = mmu.SV32)
  val SV39 = SVPair(host = MemboxS.SV39, device = mmu.SV39)
}

class RequestSender[A <: Data, B <: Data](
  val reqPort: DecoupledIO[A],
  val rspPort: DecoupledIO[B]
){
  import IOHelpers._
  var send_list: Seq[A] = Nil
  val Idle = 0; val SendingReq = 1; val WaitingRsp = 2
  var state = Idle; var next_state = Idle

  def add(req: A): Unit = send_list = send_list :+ req
  def add(req: Seq[A]): Unit = send_list = send_list ++ req

  def eval(): Unit = {
    state = next_state
    state match {
      case Idle =>
        send_list match {
          case Nil =>
            reqPort.valid.poke(false.B)
            next_state = Idle
          case _ =>
            reqPort.valid.poke(true.B)
            reqPort.bits.poke(send_list.head)
            next_state = SendingReq
        }
      case SendingReq =>
        if (checkForReady(reqPort)){
          reqPort.valid.poke(false.B)
          rspPort.ready.poke(true.B)
          next_state = WaitingRsp
        }
      case WaitingRsp =>
        if (checkForValid(rspPort)){
          rspPort.ready.poke(false.B)
          send_list = send_list.drop(1)
          next_state = Idle
        }
    }
  }
}

class MemPortDriver[A <: MemboxS.BaseSV, B <: mmu.SVParam](SV: SVPair[A, B])(
  val reqPort: DecoupledIO[Cache_Req],
  val rspPort: DecoupledIO[Cache_Rsp],
  val memBox: Memory[A]
){
  import IOHelpers._

  val WaitingReq = 1
  val SendingRsp = 2
  var state = WaitingReq
  var addr: BigInt = 0; var source: BigInt = 0; var data: Seq[BigInt] = Nil
  val data_num = rspPort.bits.data.size

  def eval(): Unit = {
    var next_state = state
    state match{
      case WaitingReq => {
        // reqPort fires
        if(checkForValid(reqPort) && checkForReady(reqPort)){
          next_state = SendingRsp
          addr = reqPort.bits.addr.peek().litValue
          source = reqPort.bits.source.peek().litValue
          data = memBox.readWordsPhysical(addr, data_num, Array.fill(data_num)(true))._2.map(_.toBigInt)
        }
      }
      case SendingRsp => {
        if(checkForValid(reqPort) && checkForReady(rspPort)){
          next_state = WaitingReq
          addr = 0; source = 0; data = Nil
        }
      }
      case _ => {}
    }
    next_state match{
      case WaitingReq => {
        reqPort.ready.poke(true.B)
        rspPort.valid.poke(false.B)
      }
      case SendingRsp => {
        reqPort.ready.poke(false.B)
        rspPort.valid.poke(true.B)
        rspPort.bits.poke(new Cache_Rsp(SV.device).Lit(
          _.source -> source.U,
          _.data -> Vec(data_num, UInt(SV.device.xLen.W)).Lit(
            (0 until data_num).map{i => i -> data(i).U}: _*
          )
        ))
      }
    }
    state = next_state
  }
}

class L2Tlb_test extends AnyFreeSpec with ChiselScalatestTester {
  import IOHelpers._
  "L2TLB Main" in {
    class L2TlbWrapper(SV: SVParam) extends Module{
      val io = IO(new Bundle {
        val in = Flipped(DecoupledIO(new L2TlbReq(SV)))
//        val invalidate = Flipped(ValidIO(new Bundle{
//          val asid = UInt(SV.asidLen.W)
//        }))
        val out = DecoupledIO(new L2TlbRsp(SV))
        // Request Always Read!
        val mem_req = DecoupledIO(new Cache_Req(SV))
        val mem_rsp = Flipped(DecoupledIO(new Cache_Rsp(SV)))
      })

      val internal = Module(new L2Tlb(SV, debug = true))

      val pipe_tlb_req = Module(new Queue(new L2TlbReq(SV), 1))
      val pipe_tlb_rsp = Module(new Queue(new L2TlbRsp(SV), 1))
      internal.io.in <> pipe_tlb_req.io.deq
      pipe_tlb_req.io.enq <> io.in
      io.out <> pipe_tlb_rsp.io.deq
      pipe_tlb_rsp.io.enq <> internal.io.out

      internal.io.invalidate.bits.asid := 0.U
      internal.io.invalidate.valid := false.B

      val pipe_mem_req = Module(new DecoupledPipe(io.mem_req.bits.cloneType, 0, insulate = true))
      val pipe_mem_rsp = Module(new DecoupledPipe(io.mem_rsp.bits.cloneType, 0, insulate = true))
      pipe_mem_req.io.enq <> internal.io.mem_req
      io.mem_req <> pipe_mem_req.io.deq

      pipe_mem_rsp.io.enq <> io.mem_rsp
      internal.io.mem_rsp <> pipe_mem_rsp.io.deq
    }
    test(new L2TlbWrapper(SV32.device)).withAnnotations(Seq(WriteVcdAnnotation)){ d =>
      val memory = new Memory(BigInt("10000000", 16), SV32.host)
      val ptbr = memory.createRootPageTable()
      memory.allocateMemory(ptbr, BigInt("080000000", 16), SV32.host.PageSize*4)
      memory.allocateMemory(ptbr, BigInt("090000000", 16), SV32.host.PageSize*4)

      var clock_cnt = 0; var tlb_cnt = 0;
      val req_list = Seq(BigInt("080000", 16), BigInt("080001", 16))
      val mem_driver = new MemPortDriver(SV32)(d.io.mem_req, d.io.mem_rsp, memory)
      val tlb_sender = new RequestSender(d.io.in, d.io.out)
      tlb_sender.add(req_list.map{a =>
        (new L2TlbReq(SV32.device)).Lit(
          _.ptbr -> ptbr.U, _.vpn -> a.U, _.asid -> 1.U, _.id -> 1.U
        )
      })

      d.io.in.setSourceClock(d.clock)
      d.io.out.setSinkClock(d.clock)
      d.io.mem_req.setSinkClock(d.clock)
      d.io.mem_rsp.setSourceClock(d.clock)

      while(tlb_sender.send_list.nonEmpty && clock_cnt <= 30){
        tlb_sender.eval()
        mem_driver.eval()
        d.clock.step(); clock_cnt += 1;
      }
      d.clock.step(3)
    }
  }
}

class L2TlbComponentTest extends AnyFreeSpec with ChiselScalatestTester {
  import IOHelpers._
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

      val pipe_ptw_req = Module(new Queue(io.ptw_req.bits.cloneType, 1, pipe = true))
      val pipe_ptw_rsp = Module(new Queue(io.ptw_rsp.bits.cloneType, 1, pipe = true))
      pipe_ptw_req.io.enq <> io.ptw_req
      internal.io.ptw_req <> pipe_ptw_req.io.deq
      pipe_ptw_rsp.io.enq <> internal.io.ptw_rsp
      io.ptw_rsp <> pipe_ptw_rsp.io.deq

      val pipe_mem_req = Module(new DecoupledPipe(io.mem_req.bits.cloneType, 0, insulate = true))
      val pipe_mem_rsp = Module(new DecoupledPipe(io.mem_rsp.bits.cloneType, 0, insulate = true))
      io.mem_req <> pipe_mem_req.io.deq
      pipe_mem_req.io.enq <> internal.io.mem_req
      pipe_mem_rsp.io.enq <> io.mem_rsp
      internal.io.mem_rsp <> pipe_mem_rsp.io.deq
    }

    test(new PTWWrapper(SV32.device)).withAnnotations(Seq(WriteVcdAnnotation)){ d =>
      def makeReq(vpn: UInt, ptbr: UInt, source: UInt): PTW_Req = (new PTW_Req(SV32.device)).Lit(
        _.vpn -> vpn, _.ptbr -> ptbr, _.source -> source
      )
      val memory = new Memory(BigInt("10000000", 16), SV32.host)
      val ptbr = memory.createRootPageTable()
      val vaddr1 = BigInt("080000000", 16)
      memory.allocateMemory(ptbr, vaddr1, SV32.host.PageSize * 2)
      println(f"V: $vaddr1%08x -> P: ${memory.addrConvert(ptbr, vaddr1)}%08x")
      var clock_cnt = 0
      d.io.ptw_req.setSourceClock(d.clock)
      d.io.ptw_rsp.setSinkClock(d.clock)
      d.io.mem_req.setSinkClock(d.clock)
      d.io.mem_rsp.setSourceClock(d.clock)
      d.clock.step(2); clock_cnt += 2

      //d.io.ptw_req.enqueueNow(req)
      d.clock.step(); clock_cnt += 1
      d.io.ptw_req.valid.poke(false.B)
      d.io.mem_req.ready.poke(true.B)

      d.io.ptw_rsp.ready.poke(true.B)
      val mem_driver = new MemPortDriver(SV32)(d.io.mem_req, d.io.mem_rsp, memory)
      val tlb_requestor = new RequestSender(d.io.ptw_req, d.io.ptw_rsp)
      tlb_requestor.add(makeReq("h80000".U, ptbr.U, 1.U))
      tlb_requestor.add(makeReq("h80001".U, ptbr.U, 1.U))
      while(tlb_requestor.send_list.nonEmpty && clock_cnt <= 30){
        tlb_requestor.eval()
        mem_driver.eval()
        d.clock.step(1); clock_cnt += 1
      }
      d.clock.step(3)
    }
  }
}
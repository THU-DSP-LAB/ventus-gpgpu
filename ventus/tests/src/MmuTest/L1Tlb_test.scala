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
import play.TestUtils._
import MmuTestUtils._

class L1TlbReq(SV: SVParam) extends Bundle {
  val asid = UInt(SV.asidLen.W)
  //val ptbr = UInt(SV.xLen.W)
  val vaddr = UInt(SV.vaLen.W)
}

class L2TlbReq_Single(SV: SVParam) extends Bundle {
  val asid = UInt(SV.asidLen.W)
  //val ptbr = UInt(SV.xLen.W)
  val vpn = UInt(SV.vpnLen.W)
}

class L2TlbRsp_Single(SV: SVParam) extends Bundle {
  val ppn = UInt(SV.ppnLen.W)
  val flags = UInt(8.W)
}

class L2TlbReqDriver[A <: MemboxS.BaseSV, B <: mmu.SVParam](SV: SVPair[A, B])(
    val reqPort: DecoupledIO[L2TlbReq_Single],
    val rspPort: DecoupledIO[L2TlbRsp_Single],
    val memory: Memory[A]
) extends MMUHelpers {

  val WaitingReq = 1
  val SendingRsp = 2
  var state = WaitingReq
  var vpn: BigInt = 0
  var ppn: BigInt = 0
  var asid: BigInt = 0
  def eval(): Unit = {
    var next_state = state
    state match {
      case WaitingReq => {
        // reqPort fires
        if (checkForValid(reqPort) && checkForReady(reqPort)) {
          next_state = SendingRsp
          vpn = reqPort.bits.vpn.peek().litValue
          asid = reqPort.bits.asid.peek().litValue
          println(
            f"L2 TLB请求: VPN = ${vpn.toString(16)}, ASID = ${asid.toString(16)}"
          )
        }
      }
      case SendingRsp => {
        if (checkForValid(reqPort) && checkForReady(rspPort)) {
          next_state = WaitingReq
          vpn = 0; asid = 0
        }
      }
      case _ => {}
    }
    next_state match {
      case WaitingReq => {
        reqPort.ready.poke(true.B)
        rspPort.valid.poke(false.B)
      }
      case SendingRsp => {
        reqPort.ready.poke(false.B)
        rspPort.valid.poke(true.B)
        rspPort.bits.poke(
          new L2TlbRsp_Single(SV32.device).Lit(
            _.ppn -> ppn.U,
            _.flags -> 1.U
          )
        )
      }
    }
    state = next_state
  }
}

class L1Tlb_test
    extends AnyFreeSpec
    with ChiselScalatestTester
    with MMUHelpers {
  "L1TLB Main" in {
    class L1TlbWrapper(SV: SVParam, nWays: Int) extends Module {
      val io = IO(new Bundle() {
        val in = Flipped(DecoupledIO(new L1TlbReq(SV)))
        val invalidate = Flipped(ValidIO(new Bundle {
          val asid = UInt(SV.asidLen.W)
        }))
        val out = DecoupledIO(new Bundle {
          val paddr = UInt(SV.paLen.W)
        })
        val l2_req = DecoupledIO(new L2TlbReq_Single(SV))
        val l2_rsp = Flipped(DecoupledIO(new L2TlbRsp_Single(SV)))
      })
      val internal = Module(new L1TLB(SV, nWays))
      internal.io.in <> Queue(io.in, 1)
      io.out <> Queue(internal.io.out, 1)

      internal.io.invalidate.bits.asid := 0.U
      internal.io.invalidate.valid := false.B

      val pipe_l2_req =
        Module(new DecoupledPipe(io.l2_req.bits.cloneType, 0, insulate = true))
      val pipe_l2_rsp =
        Module(new DecoupledPipe(io.l2_rsp.bits.cloneType, 0, insulate = true))
      pipe_l2_req.io.enq <> internal.io.l2_req
      io.l2_req <> pipe_l2_req.io.deq
      pipe_l2_rsp.io.enq <> io.l2_rsp
      internal.io.l2_rsp <> pipe_l2_rsp.io.deq

    }
    var asid_ptbr = scala.collection.mutable.Map.empty[BigInt, BigInt]
    def handleL2TlbReq[T <: BaseSV](
        d: L1TlbWrapper,
        memory: Memory[T]
    ): Unit = {
      if (d.io.l2_req.valid.peek.litToBoolean) {
        // 模拟计算物理地址的过程
        val vpn: BigInt = d.io.l2_req.bits.vpn.peek.litValue
        val asid: BigInt = d.io.l2_req.bits.asid.peek.litValue
        val ppn: BigInt = memory.addrConvert(asid_ptbr(asid), vpn << 12) >> 12

        println(
          f"检测到L2 TLB请求: VPN = ${vpn.toString(16)}, PPN = ${ppn.toString(16)}"
        )

        // 构造响应
        d.io.l2_rsp.valid.poke(true.B)
        d.io.l2_rsp.bits.ppn.poke(ppn.U)
        d.io.l2_rsp.bits.flags.poke(1.U) // 假设flags为0
      } else {
        d.io.l2_rsp.valid.poke(false.B)
      }
      d.io.l2_req.ready.poke(true.B)
    }

    test(new L1TlbWrapper(SV32.device, 4))
      .withAnnotations(Seq(WriteVcdAnnotation)) { d =>
        d.io.in.setSourceClock(d.clock)
        d.io.out.setSinkClock(d.clock)
        d.io.l2_req.setSinkClock(d.clock)
        d.io.l2_rsp.setSourceClock(d.clock)
        d.io.invalidate.setSourceClock(d.clock)

        val memory = new Memory(BigInt("10000000", 16), SV32.host)
        val ptbr = memory.createRootPageTable()
        asid_ptbr += (BigInt(1) -> ptbr)
        memory.allocateMemory(
          ptbr,
          BigInt("080000000", 16),
          SV32.host.PageSize * 4
        )
        memory.allocateMemory(
          ptbr,
          BigInt("090000000", 16),
          SV32.host.PageSize * 4
        )

        // 初始化请求发送器和内存驱动
        var clock_cnt = 0
        val req_list = Seq(
          BigInt("080000000", 16),
          BigInt("080001000", 16),
          BigInt("080002000", 16),
          BigInt("080003000", 16),
          BigInt("090002000", 16),
          BigInt("080000000", 16),
          BigInt("080001000", 16),
          BigInt("080002000", 16),
          BigInt("080003000", 16),
        ) // 测试两个不同的虚拟地址
        val tlb_sender = new RequestSender(d.io.in, d.io.out)
        val l2tlb_driver =
          new L2TlbReqDriver(SV32)(d.io.l2_req, d.io.l2_rsp, memory)

        tlb_sender.add(req_list.map { a =>
          (new L1TlbReq(SV32.device))
            .Lit(_.asid -> 1.U, _.vaddr -> a.U)
        })

        while (tlb_sender.send_list.nonEmpty && clock_cnt <= 200) {
          println(s"At cycle $clock_cnt:")
          tlb_sender.eval()

          handleL2TlbReq(d, memory)
          d.clock.step()
          clock_cnt += 1
        }

        d.clock.step(3)
      }
  }
}

package TmaTest

import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._
import chisel3.experimental.VecLiterals._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import pipeline._
import top.DecoupledPipe
import MemboxS._
import play.TestUtils._

class Tma_test
  extends AnyFreeSpec
    with ChiselScalatestTester {
  "L1TLB Main" in {
    class TmaWrapper() extends Module {
      val io = IO(new Bundle() {
        val in = Flipped(DecoupledIO(new TmaIn()))
        val out = DecoupledIO(new TmaOut())
        val l2_req = DecoupledIO(new TLBundleA_lite())
        val l2_rsp = Flipped(DecoupledIO(new TLBundleD_lite()))
      })
      val internal = Module(new Tma())
      internal.io.in <> Queue(io.in, 1)
      io.out <> Queue(internal.io.out, 1)

      val pipe_l2_req =
        Module(new DecoupledPipe(io.l2_req.bits.cloneType, 0, insulate = true))
      val pipe_l2_rsp =
        Module(new DecoupledPipe(io.l2_rsp.bits.cloneType, 0, insulate = true))
      pipe_l2_req.io.enq <> internal.io.l2_req
      io.l2_req <> pipe_l2_req.io.deq
      pipe_l2_rsp.io.enq <> io.l2_rsp
      internal.io.l2_rsp <> pipe_l2_rsp.io.deq

    }

    def handleL2Req[T <: BaseSV](
                                  d: TmaWrapper,
                                  memory: Memory[T]
                                ): Unit = {
      if (d.io.l2_req.valid.peek.litToBoolean) {
        println(f"检测到L2请求.")

        // 构造响应
        d.io.l2_rsp.valid.poke(true.B)
        // l2_rsp添加其他数据
      } else {
        d.io.l2_rsp.valid.poke(false.B)
      }
      d.io.l2_req.ready.poke(true.B)
    }

    test(new TmaWrapper())
      .withAnnotations(Seq(WriteVcdAnnotation)) { d =>
        d.io.in.setSourceClock(d.clock)
        d.io.out.setSinkClock(d.clock)
        d.io.l2_req.setSinkClock(d.clock)
        d.io.l2_rsp.setSourceClock(d.clock)

        val memory = new Memory(BigInt("10000000", 16), SV32.host)

        val mem_driver = new MemPortDriverDelay(d.io.l2_req, d.io.l2_rsp, memory, 0, 5)

        // 初始化请求发送器和内存驱动
        var clock_cnt = 0
        val req_list = Seq(

        )
        val tma_sender = new RequestSender(d.io.in, d.io.out)

        tma_sender.add(req_list.map { a =>
          (new TmaIn())
            .Lit(_.inst -> 1.U, _.source -> 2.U)
        })

        while (tma_sender.send_list.nonEmpty && clock_cnt <= 1000) {
          println(s"At cycle $clock_cnt:")
          tma_sender.eval()

          handleL2Req(d, memory)
          d.clock.step()
          clock_cnt += 1
        }

        d.clock.step(3)
      }
  }
}

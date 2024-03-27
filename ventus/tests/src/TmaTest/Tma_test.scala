package TmaTest

import top.parameters._
import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._
import chisel3.experimental.VecLiterals._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import pipeline._
import top.{DecoupledPipe, MemBox}
import MemboxS._
import play.TestUtils._
import L2cache._
import scala.collection.immutable.Seq

class Tma_test
  extends AnyFreeSpec
    with ChiselScalatestTester {
  "TMA Main" in {
    class TmaRsp2pipe extends Bundle{}
    class TmaWrapper() extends Module {
      val io = IO(new Bundle() {
        val in = Flipped(DecoupledIO(new vExeData()))
        val out = DecoupledIO(new TmaRsp2pipe())
        val l2_req = DecoupledIO(new TLBundleA_lite(l2cache_params))
        val l2_rsp = Flipped(DecoupledIO(new TLBundleD_lite_plus(l2cache_params)))
        val shared_req = DecoupledIO(new ShareMemCoreReq_np())
        val shared_rsp = Flipped(DecoupledIO(new ShareMemCoreRsp_np()))
      })
      val internal = Module(new TMA_Copysize())
      internal.io.tma_req <> Queue(io.in, 1)
      io.shared_req <> Queue(internal.io.shared_req, 1)
      io.shared_rsp <> Queue(internal.io.shared_rsp, 1)

      val pipe_l2_req =
        Module(new DecoupledPipe(io.l2_req.bits.cloneType, 0))
      val pipe_l2_rsp =
        Module(new DecoupledPipe(io.l2_rsp.bits.cloneType, 0))
      pipe_l2_req.io.enq <> internal.io.l2cache_req
      io.l2_req <> pipe_l2_req.io.deq
      pipe_l2_rsp.io.enq <> io.l2_rsp
      internal.io.l2cache_rsp <> pipe_l2_rsp.io.deq
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

    val metaFileDir = "./ventus/txt/DMA_test_temp/kernel1.metadata"
    val dataFileDir = "./ventus/txt/DMA_test_temp/kernel1.data"
    val metas = top.MetaData(metaFileDir)

    test(new TmaWrapper())
      .withAnnotations(Seq(WriteVcdAnnotation)) { d =>
        d.io.in.setSourceClock(d.clock)
        d.io.l2_req.setSinkClock(d.clock)
        d.io.l2_rsp.setSourceClock(d.clock)
        d.io.shared_rsp.setSourceClock(d.clock)
        d.io.shared_req.setSinkClock(d.clock)

        val memory = new MemBox(MemboxS.Bare32)
        memory.loadfile(0, metas, dataFileDir)

        val mem_driver = new MemPortDriverDelay(d.io.l2_req, d.io.l2_rsp, memory, 0, 5)


        case class vExeData_Soft(
                                in1: Seq[BigInt],
                                in2: Seq[BigInt],
                                in3: Seq[BigInt],
                                )
        def makeData(in: vExeData_Soft): vExeData = {
          (new vExeData).Lit(
            _.in1 -> Vec(num_thread, UInt(xLen.W)).Lit(in.in1.zipWithIndex.map{case (d, i) => (i, d.U)}:_*)
          )
        }
//        def f(a: Int, b: Int, c:Int), then f(1,2,3) 等价于 f(Seq(1,2,3):_*)
//        Seq('a', 'b').zipWithIndex 相当于 Seq(0 -> 'a', 1 -> 'b')

        var clock_cnt = 0

        val myData = vExeData_Soft(
          in1 = Seq(BigInt(1), BigInt(2), BigInt(3), BigInt(4)),
          in2 = Seq(BigInt(5), BigInt(6), BigInt(7), BigInt(8)),
          in3 = Seq(BigInt(9), BigInt(10), BigInt(11), BigInt(12))
        )

        val req_list = Seq(
          makeData(myData),
          // 根据需要添加更多 vExeData 实例
        )
        val tma_sender = new RequestSender[vExeData, TmaRsp2pipe](d.io.in, d.io.out)
        val temp = req_list.map { a =>
          (new vExeData())
            .Lit(_.in1 -> a.in1, _.in2 -> a.in2, _.in3 -> a.in3, _.ctrl -> a.ctrl, _.mask -> a.mask)
        }
        tma_sender.add(temp)

        while (tma_sender.send_list.nonEmpty && clock_cnt <= 1000) {

          tma_sender.eval()

//          handleL2Req(d, memory)
          mem_driver.eval()
          d.clock.step()
          clock_cnt += 1
        }

        d.clock.step(3)
      }
  }
}

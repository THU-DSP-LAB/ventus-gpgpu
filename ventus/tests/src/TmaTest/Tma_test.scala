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
    class TmaRsp2pipe extends Bundle {}
    class TmaWrapper() extends Module {
      val io = IO(new Bundle() {
        val in = Flipped(DecoupledIO(new vExeData()))
        //        val out = DecoupledIO(new TmaRsp2pipe())
        val out = DecoupledIO(UInt(32.W))
        val l2_req = DecoupledIO(new TLBundleA_lite(l2cache_params))
        val l2_rsp = Flipped(DecoupledIO(new TLBundleD_lite(l2cache_params)))
        val shared_req = DecoupledIO(new ShareMemCoreReq_np())
        val shared_rsp = Flipped(DecoupledIO(new ShareMemCoreRsp_np()))
        //        val fence_end_tma = DecoupledIO(UInt(32.W))
      })

      val internal = Module(new TMA_Copysize())
      internal.io.tma_req <> Queue(io.in, 1)
      io.out <> Queue(internal.io.fence_end_tma, 1)
      io.shared_req <> Queue(internal.io.shared_req, 1)
      internal.io.shared_rsp <> Queue(io.shared_rsp, 1)

      //      Queue(internal.io.shared_rsp, 1) <> io.shared_rsp

      val pipe_l2_req =
        Module(new DecoupledPipe(io.l2_req.bits.cloneType, 0, insulate = true))
      val pipe_l2_rsp =
        Module(new DecoupledPipe(io.l2_rsp.bits.cloneType, 0, insulate = true))
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
        d.io.out.setSinkClock(d.clock)
        d.io.l2_req.setSinkClock(d.clock)
        d.io.l2_rsp.setSourceClock(d.clock)
        d.io.shared_rsp.setSourceClock(d.clock)
        d.io.shared_req.setSinkClock(d.clock)

        val memory = new MemBox(MemboxS.Bare32)
        memory.loadfile(0, metas, dataFileDir)

        val mem_driver = new MemPortDriverDelay(d.io.l2_req, d.io.l2_rsp, memory, 8, 5)


        case class vExeData_Soft(
                                  in1: Seq[BigInt],
                                  in2: Seq[BigInt],
                                  in3: Seq[BigInt],
                                  mask: Seq[Bool],
                                  ctrl: CtrlSigs
                                )

        def makeData(in: vExeData_Soft): vExeData = {
          (new vExeData).Lit(
            _.in1 -> Vec(num_thread, UInt(xLen.W)).Lit(in.in1.zipWithIndex.map { case (d, i) => (i, d.U) }: _*),
            _.in2 -> Vec(num_thread, UInt(xLen.W)).Lit(in.in2.zipWithIndex.map { case (d, i) => (i, d.U) }: _*),
            _.in3 -> Vec(num_thread, UInt(xLen.W)).Lit(in.in3.zipWithIndex.map { case (d, i) => (i, d.U) }: _*),
            _.mask -> Vec(num_thread, Bool()).Lit(in.mask.zipWithIndex.map { case (d, i) => (i, d) }: _*),
            _.ctrl -> in.ctrl
          )
        }
        //        def f(a: Int, b: Int, c:Int), then f(1,2,3) 等价于 f(Seq(1,2,3):_*)
        //        Seq('a', 'b').zipWithIndex 相当于 Seq(0 -> 'a', 1 -> 'b')

        var clock_cnt = 0

        //        val myData = vExeData_Soft(
        //          in1 = Seq(BigInt("90000000",16), BigInt("90000000",16), BigInt("90000000",16), BigInt("90000000",16)),
        //          in2 = Seq(BigInt("70000000",16), BigInt("70000000",16), BigInt("70000000",16), BigInt("70000000",16)),
        //          in3 = Seq(BigInt("00000010",16), BigInt("00000010",16), BigInt("00000010",16), BigInt("00000010",16)),
        //          mask = Seq(false.B,false.B,false.B,false.B)
        //        )
        def genBundle_zero(): CtrlSigs = {
          val ctrlsigs = (new CtrlSigs).Lit(
            _.inst -> 0.U,
            _.wid -> 0.U,
            _.fp -> false.B,
            _.branch -> 0.U,
            _.simt_stack -> false.B,
            _.simt_stack_op -> false.B,
            _.barrier -> false.B,
            _.csr -> 0.U,
            _.reverse -> false.B,
            _.sel_alu2 -> 0.U,
            _.sel_alu1 -> 0.U,
            _.isvec -> false.B,
            _.sel_alu3 -> 0.U,
            _.mask -> false.B,
            _.sel_imm -> 0.U,
            _.mem_whb -> 0.U,
            _.mem_unsigned -> false.B,
            _.alu_fn -> 0.U,
            _.force_rm_rtz -> false.B,
            _.is_vls12 -> false.B,
            _.mem -> false.B,
            _.mul -> false.B,
            _.tc -> false.B,
            _.disable_mask -> false.B,
            _.custom_signal_0 -> false.B,
            _.mem_cmd -> 0.U,
            _.mop -> 0.U,
            _.reg_idx1 -> 0.U,
            _.reg_idx2 -> 0.U,
            _.reg_idx3 -> 0.U,
            _.reg_idxw -> 0.U,
            _.wvd -> false.B,
            _.fence -> false.B,
            _.sfu -> false.B,
            _.readmask -> false.B,
            _.writemask -> false.B,
            _.wxd -> false.B,
            _.pc -> 0.U,
            _.imm_ext -> 0.U,
            _.atomic -> false.B,
            _.aq -> false.B,
            _.rl -> false.B,
            _.spike_info.get -> (new InstWriteBack).Lit(_.pc -> 0.U, _.inst -> 0.U)
          )
          ctrlsigs

        }

        val myData = vExeData_Soft(
          in1 = Seq.fill(num_thread)(BigInt("90000000", 16)),
          in2 = Seq.fill(num_thread)(BigInt("70000000", 16)),
          in3 = Seq.fill(num_thread)(BigInt("00000100", 16)),
          mask = Seq.fill(num_thread)(true.B),
          ctrl = genBundle_zero()
        )

        val hw_data = makeData(myData)

        val req_list = Seq(
          hw_data
          //          makeData(myData2),
          // 根据需要添加更多 vExeData 实例
        )

        val tma_sender = new RequestSender[vExeData, UInt](d.io.in, d.io.out)
        //        val temp = req_list.map { a =>
        //          (new vExeData())
        //            .Lit(_.in1 -> a.in1, _.in2 -> a.in2, _.in3 -> a.in3, _.ctrl -> a.ctrl, _.mask -> a.mask)
        //        }
        tma_sender.add(req_list)

        while (tma_sender.send_list.nonEmpty && clock_cnt <= 100) {
          //        while (clock_cnt <= 100000) {

          tma_sender.eval()

          //          handleL2Req(d, memory)
          mem_driver.eval()
          d.clock.step()
          clock_cnt += 1
        }

        d.clock.step(30)
      }
  }
}

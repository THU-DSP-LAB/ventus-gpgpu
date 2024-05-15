//package DmaTest
//
//import top.parameters._
//import chisel3._
//import chisel3.util._
//import chisel3.experimental.BundleLiterals._
//import chisel3.experimental.VecLiterals._
//import chiseltest._
//import org.scalatest.freespec.AnyFreeSpec
//import pipeline._
//import top.{DecoupledPipe, MemBox}
//import MemboxS._
//import play.TestUtils._
//import L2cache._
//import play.MemPortDriverDelay_shared
//
//import scala.collection.immutable.Seq
//
//class Dma_test
//  extends AnyFreeSpec
//    with ChiselScalatestTester {
//  "DMA Main" in {
//    class dmaRsp2pipe extends Bundle {}
//    class dmaWrapper() extends Module {
//      val io = IO(new Bundle() {
//        val in = Flipped(DecoupledIO(new vExeData()))
//        //        val out = DecoupledIO(new dmaRsp2pipe())
//        val out = DecoupledIO(UInt(32.W))
//        val l2_req = DecoupledIO(new TLBundleA_lite(l2cache_params))
//        val l2_rsp = Flipped(DecoupledIO(new TLBundleD_lite(l2cache_params)))
//        val shared_req = DecoupledIO(new ShareMemCoreReq_np())
//        val shared_rsp = Flipped(DecoupledIO(new ShareMemCoreRsp_np()))
//        //        val fence_end_dma = DecoupledIO(UInt(32.W))
//      })
//
//      val internal = Module(new DMA_core())
//      internal.io.dma_req <> Queue(io.in, 1)
//      io.out <> Queue(internal.io.fence_end_dma, 1)
////      io.shared_req <> Queue(internal.io.shared_req, 1)
////      internal.io.shared_rsp <> Queue(io.shared_rsp, 1)
//      //shared io
//      val pipe_shared_req =
//        Module(new DecoupledPipe(io.shared_req.bits.cloneType, 0, insulate = true))
//      val pipe_shared_rsp =
//        Module(new DecoupledPipe(io.shared_rsp.bits.cloneType, 0, insulate = true))
//      pipe_shared_req.io.enq <> internal.io.shared_req
//      io.shared_req <> pipe_shared_req.io.deq
//      //      io.l2_req <> Queue(internal.io.l2cache_req, 1)
//      pipe_shared_rsp.io.enq <> io.shared_rsp
//      internal.io.shared_rsp <> pipe_shared_rsp.io.deq
//
//      val pipe_l2_req =
//        Module(new DecoupledPipe(io.l2_req.bits.cloneType, 0, insulate = true))
//      val pipe_l2_rsp =
//        Module(new DecoupledPipe(io.l2_rsp.bits.cloneType, 0, insulate = true))
//      pipe_l2_req.io.enq <> internal.io.l2cache_req
//      io.l2_req <> pipe_l2_req.io.deq
////      io.l2_req <> Queue(internal.io.l2cache_req, 1)
//      pipe_l2_rsp.io.enq <> io.l2_rsp
//      internal.io.l2cache_rsp <> pipe_l2_rsp.io.deq
//    }
//
//    def handleL2Req[T <: BaseSV](
//                                  d: dmaWrapper,
//                                  memory: Memory[T]
//                                ): Unit = {
//      if (d.io.l2_req.valid.peek.litToBoolean) {
//        println(f"检测到L2请求.")
//
//        // 构造响应
//        d.io.l2_rsp.valid.poke(true.B)
//        // l2_rsp添加其他数据
//      } else {
//        d.io.l2_rsp.valid.poke(false.B)
//      }
//      d.io.l2_req.ready.poke(true.B)
//    }
//
//    val metaFileDir = "./ventus/txt/DMA_test_temp/kernel1.metadata"
//    val dataFileDir = "./ventus/txt/DMA_test_temp/kernel1.data"
//    val metas = top.MetaData(metaFileDir)
//
//    test(new dmaWrapper())
//      .withAnnotations(Seq(WriteVcdAnnotation, VerilatorBackendAnnotation)) { d =>
//        d.io.in.setSourceClock(d.clock)
//        d.io.out.setSinkClock(d.clock)
//        d.io.l2_req.setSinkClock(d.clock)
//        d.io.l2_rsp.setSourceClock(d.clock)
//        d.io.shared_rsp.setSourceClock(d.clock)
//        d.io.shared_req.setSinkClock(d.clock)
//
//        val memory = new MemBox()
//        memory.loadfile(metas, dataFileDir)
//
//        val mem_driver = new MemPortDriverDelay(d.io.l2_req, d.io.l2_rsp, memory, 4, 1)
//        val mem_driver_shared = new MemPortDriverDelay_shared(d.io.shared_req, d.io.shared_rsp, memory, 6, 1)
//
//        case class vExeData_Soft(
//                                  in1: Seq[BigInt],
//                                  in2: Seq[BigInt],
//                                  in3: Seq[BigInt],
//                                  mask: Seq[Bool],
//                                  ctrl: CtrlSigs
//                                )
//
//        def makeData(in: vExeData_Soft): vExeData = {
//          (new vExeData).Lit(
//            _.in1 -> Vec(num_thread, UInt(xLen.W)).Lit(in.in1.zipWithIndex.map { case (d, i) => (i, d.U) }: _*),
//            _.in2 -> Vec(num_thread, UInt(xLen.W)).Lit(in.in2.zipWithIndex.map { case (d, i) => (i, d.U) }: _*),
//            _.in3 -> Vec(num_thread, UInt(xLen.W)).Lit(in.in3.zipWithIndex.map { case (d, i) => (i, d.U) }: _*),
//            _.mask -> Vec(num_thread, Bool()).Lit(in.mask.zipWithIndex.map { case (d, i) => (i, d) }: _*),
//            _.ctrl -> in.ctrl
//          )
//        }
//        //        def f(a: Int, b: Int, c:Int), then f(1,2,3) 等价于 f(Seq(1,2,3):_*)
//        //        Seq('a', 'b').zipWithIndex 相当于 Seq(0 -> 'a', 1 -> 'b')
//
//        var clock_cnt = 0
//
//        //        val myData = vExeData_Soft(
//        //          in1 = Seq(BigInt("90000000",16), BigInt("90000000",16), BigInt("90000000",16), BigInt("90000000",16)),
//        //          in2 = Seq(BigInt("70000000",16), BigInt("70000000",16), BigInt("70000000",16), BigInt("70000000",16)),
//        //          in3 = Seq(BigInt("00000010",16), BigInt("00000010",16), BigInt("00000010",16), BigInt("00000010",16)),
//        //          mask = Seq(false.B,false.B,false.B,false.B)
//        //        )
//        def genBundle_bulk(): CtrlSigs = {
//          val ctrlsigs = (new CtrlSigs).Lit(
//            _.inst -> 0.U,
//            _.wid -> 0.U,
//            _.fp -> false.B,
//            _.branch -> 0.U,
//            _.simt_stack -> false.B,
//            _.simt_stack_op -> false.B,
//            _.barrier -> false.B,
//            _.csr -> 0.U,
//            _.reverse -> false.B,
//            _.sel_alu2 -> 0.U,
//            _.sel_alu1 -> 0.U,
//            _.isvec -> false.B,
//            _.sel_alu3 -> 0.U,
//            _.mask -> false.B,
//            _.sel_imm -> 0.U,
//            _.mem_whb -> 0.U,
//            _.mem_unsigned -> false.B,
//            _.alu_fn -> 0.U,
//            _.force_rm_rtz -> false.B,
//            _.is_vls12 -> false.B,
//            _.mem -> false.B,
//            _.mul -> false.B,
//            _.tc -> false.B,
//            _.disable_mask -> false.B,
//            _.custom_signal_0 -> false.B,
//            _.mem_cmd -> 0.U,
//            _.mop -> 0.U,
//            _.reg_idx1 -> 0.U,
//            _.reg_idx2 -> 0.U,
//            _.reg_idx3 -> 0.U,
//            _.reg_idxw -> 0.U,
//            _.wvd -> false.B,
//            _.fence -> false.B,
//            _.sfu -> false.B,
//            _.readmask -> false.B,
//            _.writemask -> false.B,
//            _.wxd -> false.B,
//            _.pc -> 4096.U,
//            _.imm_ext -> 0.U,
//            _.spike_info.get -> (new InstWriteBack).Lit(_.sm_id -> 0.U,_.pc -> 0.U, _.inst -> 0.U),
//            _.atomic -> false.B,
//            _.aq -> false.B,
//            _.rl -> false.B,
//            _.funct -> 1.U,
//            _.copysize -> 0.U,
//            _.dma -> true.B
//          )
//          ctrlsigs
//
//        }
//
//        def genBundle_copysize(): CtrlSigs = {
//          val ctrlsigs = (new CtrlSigs).Lit(
//            _.inst -> 0.U,
//            _.wid -> 0.U,
//            _.fp -> false.B,
//            _.branch -> 0.U,
//            _.simt_stack -> false.B,
//            _.simt_stack_op -> false.B,
//            _.barrier -> false.B,
//            _.csr -> 0.U,
//            _.reverse -> false.B,
//            _.sel_alu2 -> 0.U,
//            _.sel_alu1 -> 0.U,
//            _.isvec -> false.B,
//            _.sel_alu3 -> 0.U,
//            _.mask -> false.B,
//            _.sel_imm -> 0.U,
//            _.mem_whb -> 0.U,
//            _.mem_unsigned -> false.B,
//            _.alu_fn -> 0.U,
//            _.force_rm_rtz -> false.B,
//            _.is_vls12 -> false.B,
//            _.mem -> false.B,
//            _.mul -> false.B,
//            _.tc -> false.B,
//            _.disable_mask -> false.B,
//            _.custom_signal_0 -> false.B,
//            _.mem_cmd -> 0.U,
//            _.mop -> 0.U,
//            _.reg_idx1 -> 0.U,
//            _.reg_idx2 -> 0.U,
//            _.reg_idx3 -> 0.U,
//            _.reg_idxw -> 0.U,
//            _.wvd -> false.B,
//            _.fence -> false.B,
//            _.sfu -> false.B,
//            _.readmask -> false.B,
//            _.writemask -> false.B,
//            _.wxd -> false.B,
//            _.pc -> 4096.U,
//            _.imm_ext -> 0.U,
//            _.spike_info.get -> (new InstWriteBack).Lit(_.sm_id -> 0.U,_.pc -> 0.U, _.inst -> 0.U),
//            _.atomic -> false.B,
//            _.aq -> false.B,
//            _.rl -> false.B,
//
//            _.funct -> 0.U,
//            _.copysize -> 2.U,
//            _.dma -> true.B
//          )
//          ctrlsigs
//        }
//
//        def genBundle_tensor(): CtrlSigs = {
//          val ctrlsigs = (new CtrlSigs).Lit(
//            _.inst -> 0.U,
//            _.wid -> 0.U,
//            _.fp -> false.B,
//            _.branch -> 0.U,
//            _.simt_stack -> false.B,
//            _.simt_stack_op -> false.B,
//            _.barrier -> false.B,
//            _.csr -> 0.U,
//            _.reverse -> false.B,
//            _.sel_alu2 -> 0.U,
//            _.sel_alu1 -> 0.U,
//            _.isvec -> false.B,
//            _.sel_alu3 -> 0.U,
//            _.mask -> false.B,
//            _.sel_imm -> 0.U,
//            _.mem_whb -> 0.U,
//            _.mem_unsigned -> false.B,
//            _.alu_fn -> 0.U,
//            _.force_rm_rtz -> false.B,
//            _.is_vls12 -> false.B,
//            _.mem -> false.B,
//            _.mul -> false.B,
//            _.tc -> false.B,
//            _.disable_mask -> false.B,
//            _.custom_signal_0 -> false.B,
//            _.mem_cmd -> 0.U,
//            _.mop -> 0.U,
//            _.reg_idx1 -> 0.U,
//            _.reg_idx2 -> 0.U,
//            _.reg_idx3 -> 0.U,
//            _.reg_idxw -> 0.U,
//            _.wvd -> false.B,
//            _.fence -> false.B,
//            _.sfu -> false.B,
//            _.readmask -> false.B,
//            _.writemask -> false.B,
//            _.wxd -> false.B,
//            _.pc -> 4096.U,
//            _.imm_ext -> 0.U,
//            _.spike_info.get -> (new InstWriteBack).Lit(_.sm_id -> 0.U, _.pc -> 0.U, _.inst -> 0.U),
//            _.atomic -> false.B,
//            _.aq -> false.B,
//            _.rl -> false.B,
//
//            _.funct -> 3.U,
//            _.copysize -> 2.U,
//            _.dma -> true.B
//          )
//          ctrlsigs
//        }
//
//        val myData1 = vExeData_Soft(
//          in1 = Seq.fill(num_thread)(BigInt("90000000", 16)),
//          in3 = Seq.fill(num_thread)(BigInt("70000000", 16)),
//          in2 = Seq.fill(num_thread)(BigInt("00000010", 16)),
//          mask = Seq.fill(num_thread)(true.B),
//          ctrl = genBundle_bulk()
//        )
//        val myData2 = vExeData_Soft(
//          in1 = Seq.fill(num_thread)(BigInt("90000000", 16)),
//          in3 = Seq.fill(num_thread)(BigInt("70000000", 16)),
//          in2 = Seq.fill(num_thread)(BigInt("00000420", 16)),
//          mask = Seq.fill(num_thread)(true.B),
//          ctrl = genBundle_bulk()
//        )
//        val myData3 = vExeData_Soft(
//          in1 = Seq.fill(num_thread)(BigInt("90002270", 16)),
//          in3 = Seq.fill(num_thread)(BigInt("70001850", 16)),
//          in2 = Seq.fill(num_thread)(BigInt("00000230", 16)),
//          mask = Seq.fill(num_thread)(true.B),
//          ctrl = genBundle_bulk()
//        )
//        val myData4 = vExeData_Soft(
//          in1 = Seq.fill(num_thread)(BigInt("9000007D", 16)),
//          in3 = Seq.fill(num_thread)(BigInt("70000011", 16)),
//          in2 = Seq.fill(num_thread)(BigInt("0000000F", 16)),
//          mask = Seq.fill(num_thread)(true.B),
//          ctrl = genBundle_copysize() // copysize = 16
//        )
//        var seq_in1 = Seq(BigInt("00000006", 16))   //datatype
//        seq_in1 = seq_in1 :+ BigInt("00000004",16)  //tensorRank
//        seq_in1 = seq_in1 :+ BigInt("90000000",16)  //globalAddress
//        seq_in1 = seq_in1 :+ BigInt("00000080",16)  //globalDim1
//        seq_in1 = seq_in1 :+ BigInt("00000080",16)  //globalDim2
//        seq_in1 = seq_in1 :+ BigInt("00000080",16)  //globalDim3
//        seq_in1 = seq_in1 :+ BigInt("00000080",16)  //globalDim4
//        seq_in1 = seq_in1 :+ BigInt("00000000",16)  //globalDim5
//        seq_in1 = seq_in1 :+ BigInt("00000200",16)  //globalStrides1
//        seq_in1 = seq_in1 :+ BigInt("00002000",16)  //globalStrides2
//        seq_in1 = seq_in1 :+ BigInt("00080000",16)  //globalStrides3
//        seq_in1 = seq_in1 :+ BigInt("00200000",16)  //globalStrides4
//        seq_in1 = seq_in1 :+ BigInt("00000000",16)  //globalStrides5
//        (0 until(num_thread - 13)).foreach( x =>{
//          seq_in1 = seq_in1 :+ BigInt("00000000", 16)
//        })
//
//        var seq_in2 = Seq(BigInt("90000010", 16))   //boxdim
//        seq_in2 = seq_in2 :+  BigInt("00000120",16) //boxDim1
//        seq_in2 = seq_in2 :+  BigInt("00000010",16) //boxDim2
//        seq_in2 = seq_in2 :+  BigInt("00000010",16) //boxDim3
//        seq_in2 = seq_in2 :+  BigInt("00000010",16) //boxDim4
//        seq_in2 = seq_in2 :+  BigInt("00000000",16) //boxDim5
//        seq_in2 = seq_in2 :+  BigInt("00000001", 16) //elementStrides1
//        seq_in2 = seq_in2 :+  BigInt("00000008", 16) //elementStrides2
//        seq_in2 = seq_in2 :+  BigInt("00000008", 16) //elementStrides3
//        seq_in2 = seq_in2 :+  BigInt("00000008", 16) //elementStrides4
//        seq_in2 = seq_in2 :+  BigInt("00000000", 16) //elementStrides5
//        seq_in2 = seq_in2 :+  BigInt("00000000", 16)  //interleave
//        seq_in2 = seq_in2 :+  BigInt("00000000", 16)  //swizzle
//        seq_in2 = seq_in2 :+  BigInt("00000000", 16)  //l2promotion
//        seq_in2 = seq_in2 :+  BigInt("00000001", 16)  //oobfill
//        (0 until (num_thread - 15)).foreach(x => {
//          seq_in2 = seq_in2 :+ BigInt("00000000", 16)
//        })
//
//        val myData5 = vExeData_Soft(
//          in1 = seq_in1,
//          in3 = Seq.fill(num_thread)(BigInt("70000000", 16)),
//          in2 = seq_in2,
//          mask = Seq.fill(num_thread)(true.B),
//          ctrl = genBundle_tensor() // copysize = 16
//        )
//
//        val hw_data1 = makeData(myData1)
//        val hw_data2 = makeData(myData2)
//        val hw_data3 = makeData(myData3)
//        val hw_data4 = makeData(myData4)
//        val hw_data5 = makeData(myData5)
//
//        val req_list = Seq(
////          hw_data1,
////          hw_data2,
////          hw_data4,
////          hw_data3,
//          hw_data5
//          //          makeData(myData2),
//          // 根据需要添加更多 vExeData 实例
//        )
//
//        val dma_sender = new RequestSender[vExeData, UInt](d.io.in, d.io.out)
//        //        val temp = req_list.map { a =>
//        //          (new vExeData())
//        //            .Lit(_.in1 -> a.in1, _.in2 -> a.in2, _.in3 -> a.in3, _.ctrl -> a.ctrl, _.mask -> a.mask)
//        //        }
//        dma_sender.add(req_list)
//        d.clock.setTimeout(0)
//        while (clock_cnt <= 1500) {
//          //        while (clock_cnt <= 100000) {
//
//          dma_sender.eval()
//
//          //          handleL2Req(d, memory)
//          mem_driver.eval()
//          mem_driver_shared.eval()
//          d.clock.step()
//          clock_cnt += 1
//        }
//
//        d.clock.step(30)
//      }
//  }
//}

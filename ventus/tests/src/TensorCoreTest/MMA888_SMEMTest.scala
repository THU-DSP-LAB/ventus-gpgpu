//package TensorCoreTest
//
//import FPUv2.utils.RoundingModes
//import TensorCore.{TCCtrl, TC_MMA888,TC_MMAInput,TC_MMAOutput}//, TC_MMA1688Input, TC_MMA1688Output}
//import chisel3._
//import chisel3.experimental.BundleLiterals._
//import chisel3.experimental.VecLiterals.AddVecLiteralConstructor
//import chiseltest._
//import org.scalatest.flatspec.AnyFlatSpec
//import pipeline.{CtrlSigs, InstWriteBack, vExeData}
//import play.TestUtils.RequestSender
////import chisel3.util.Hexadecimal
//import top.parameters._
//
//import scala.io.Source
//
//class MMA888_SMEMTest extends AnyFlatSpec with ChiselScalatestTester {
////  import FPUv2.TestArgs._
//  object TCMMA888Input{
//    var count = 0
//    def reset = { count = 0 }
//    def readTxtFileToOneDimensionalArray(fileName: String): Array[String] = {
//      // 使用Source从文件中读取文本
//      val lines: Iterator[String] = Source.fromFile(fileName).getLines()
//      // 将每一行根据空格分割成单词，并将所有单词收集到一个数组中
//      val wordsArray = lines.flatMap(_.split("\\s+")).toArray
////      // 关闭Source
//      //      Source.close(lines)
////      println(wordsArray)
//      wordsArray
//    }
//    def apply() = {
//      count = (count + 1) % 32
//      val RA = readTxtFileToOneDimensionalArray("ventus/tests/src/TensorCoreTest/testData/RA.txt")
//      val RB = readTxtFileToOneDimensionalArray("ventus/tests/src/TensorCoreTest/testData/RB.txt")
//      val RC = readTxtFileToOneDimensionalArray("ventus/tests/src/TensorCoreTest/testData/RC.txt")
//      new TC_MMAInput(new TCCtrl(32, 1)).Lit(
//        _.data_in -> new vExeData().Lit(
//          _.in1 -> Vec(32, UInt(num_thread.W)).Lit(
//            (0 until 32).map{i => i-> RA(i).U}:_*
//          ),
//          _.in2 -> Vec(32, UInt(num_thread.W)).Lit(
//            (0 until 32).map{i => i-> RB(i).U}:_*
//          ),
//          _.in3 -> Vec(32, UInt(num_thread.W)).Lit(
//            (0 until 32).map{i => i-> RC(i).U}:_*
//          ),
//          _.mask -> Vec(32,Bool()).Lit(
//            (0 until 32).map{ i => i -> false.B}:_*
//          ),
//          _.ctrl -> genBundle_bulk()
//        ),
//        _.rm -> RoundingModes.RNE,
////        _.rm -> RoundingModes.RTZ,
////        _.rm -> RoundingModes.RDN,
////        _.rm -> RoundingModes.RUP,
////        _.rm -> RoundingModes.RMM,
//        _.ctrl -> new TCCtrl(32, 1).Lit(
//          _.reg_idxw -> count.U,
//          _.warpID -> 0.U
//        )
//      )
//    }
//  def genBundle_bulk(): CtrlSigs = {
//    val ctrlsigs = (new CtrlSigs).Lit(
//      _.inst -> 0.U,
//      _.wid -> 0.U,
//      _.fp -> false.B,
//      _.branch -> 0.U,
//      _.simt_stack -> false.B,
//      _.simt_stack_op -> false.B,
//      _.barrier -> false.B,
//      _.csr -> 0.U,
//      _.reverse -> false.B,
//      _.sel_alu2 -> 0.U,
//      _.sel_alu1 -> 0.U,
//      _.isvec -> false.B,
//      _.sel_alu3 -> 0.U,
//      _.mask -> false.B,
//      _.sel_imm -> 0.U,
//      _.mem_whb -> 0.U,
//      _.mem_unsigned -> false.B,
//      _.alu_fn -> 0.U,
//      _.force_rm_rtz -> false.B,
//      _.is_vls12 -> false.B,
//      _.mem -> false.B,
//      _.mul -> false.B,
//      _.tc -> true.B,
//      _.disable_mask -> false.B,
//      _.custom_signal_0 -> false.B,
//      _.mem_cmd -> 0.U,
//      _.mop -> 0.U,
//      _.reg_idx1 -> 0.U,
//      _.reg_idx2 -> 0.U,
//      _.reg_idx3 -> 0.U,
//      _.reg_idxw -> 0.U,
//      _.wvd -> false.B,
//      _.fence -> false.B,
//      _.sfu -> false.B,
//      _.readmask -> false.B,
//      _.writemask -> false.B,
//      _.wxd -> false.B,
//      _.pc -> 4096.U,
//      _.imm_ext -> 0.U,
//      _.spike_info.get -> (new InstWriteBack).Lit(_.sm_id -> 0.U,_.pc -> 0.U, _.inst -> 0.U),
//      _.atomic -> false.B,
//      _.aq -> false.B,
//      _.rl -> false.B,
//    )
//    ctrlsigs
//  }
//  }
//
//  behavior of "Tensor Core"
//  it should "TCComputationArray 1688 FP16" in {
//    test(new TC_MMA888(8,8,8,16,new TCCtrl(32, 1))).withAnnotations(Seq(WriteVcdAnnotation)) { d =>
//      TCMMA888Input.reset
//      d.io.in.initSource()
//      d.io.in.setSourceClock(d.clock)
//      d.io.out.initSink()
//      d.io.out.setSinkClock(d.clock)
//      d.clock.setTimeout(80)
//
//      val input_list = Seq(TCMMA888Input())
//      val TC_input_sender = new RequestSender[TC_MMAInput, TC_MMAOutput](d.io.in, d.io.out)
//      TC_input_sender.add(input_list)
//
//      d.clock.setTimeout(0)
//      d.clock.step(4)
//      var clock_cnt = 0
//
//      while(clock_cnt <= 100){
//        TC_input_sender.eval()
//        d.io.out.ready.poke(true.B)
//        d.clock.step()
//        clock_cnt += 1
//      }
////      d.clock.step(600)
//      println("\n")
//      val Rd = TCMMA888Input.readTxtFileToOneDimensionalArray("ventus/tests/src/TensorCoreTest/testData/RD.txt")
//      val Rd_torch = TCMMA888Input.readTxtFileToOneDimensionalArray("ventus/tests/src/TensorCoreTest/testData/RD_torch.txt")
//      for (i <- 0 until 32) {
//        val elementValue = d.io.out.bits.data_out(i).peek()
//        val intValue: Int = elementValue(15,0).litValue().toInt
//        val intValue2: Int = elementValue(31,16).litValue().toInt
//        val intValue3: Int = elementValue(47,32).litValue().toInt
//        val intValue4: Int = elementValue(63,48).litValue().toInt
//        val hexString: String = f"$intValue%04x" // %016x 表示至少16位的16进制数，不足的前面补零
//        val hexString2: String = f"$intValue2%04x" // %016x 表示至少16位的16进制数，不足的前面补零
//        val hexString3: String = f"$intValue3%04x" // %016x 表示至少16位的16进制数，不足的前面补零
//        val hexString4: String = f"$intValue4%04x" // %016x 表示至少16位的16进制数，不足的前面补零
////        println(s"data_out($i) = hex:$hexString")
//        val std: String = Rd(i)
//        val std_torch: String = Rd_torch(i)
//        println(s"$i $hexString4$hexString3$hexString2$hexString, $std, $std_torch")
//      }
////      d.clock.step(30)
//    }
//  }
//}
//

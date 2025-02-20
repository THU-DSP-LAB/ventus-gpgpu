package TensorCoreTest

import FPUv2.TCCtrl
import FPUv2.utils.RoundingModes
import TensorCore._
import chisel3._
import chisel3.experimental.BundleLiterals._
import chisel3.experimental.VecLiterals.AddVecLiteralConstructor
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import pipeline.{CtrlSigs, InstWriteBack, TCCtrl_mulslot_v2}
import play.TestUtils.RequestSender
import top.parameters._

import scala.io.Source

class MMA888MixedPrec_Test extends AnyFlatSpec with ChiselScalatestTester {
//  import FPUv2.TestArgs._
  object TCMMA888Input{
    var count = 0
    def reset = { count = 0 }
    def readTxtFileToOneDimensionalArray(fileName: String): Array[String] = {
      // 使用Source从文件中读取文本
      val lines: Iterator[String] = Source.fromFile(fileName).getLines()
      // 将每一行根据空格分割成单词，并将所有单词收集到一个数组中
      val wordsArray = lines.flatMap(_.split("\\s+")).toArray
//      // 关闭Source
      //      Source.close(lines)
//      println(wordsArray)
      wordsArray
    }
    def apply(pa:Int) = {
      count = (count + 1) % 32
      val RA = readTxtFileToOneDimensionalArray("ventus/tests/src/TensorCoreTest/testData_888/RA.txt")
      val RB = readTxtFileToOneDimensionalArray("ventus/tests/src/TensorCoreTest/testData_888/RB.txt")
      val RC = readTxtFileToOneDimensionalArray("ventus/tests/src/TensorCoreTest/testData_888/RC.txt")

      // 根据 pa 的值决定是否将 in1, in2, in3 赋值为 0
      val data_in = if (pa == 0) {
        new vTCData().Lit(
          _.in1 -> Vec(num_thread, UInt(xLen.W)).Lit((0 until num_thread).map{_ -> 0.U}:_*) ,
          _.in2 -> Vec(num_thread, UInt(xLen.W)).Lit((0 until num_thread).map{_ -> 0.U}:_*) ,
          _.in3 -> Vec(num_thread, UInt(xLen.W)).Lit((0 until num_thread).map{_ -> 0.U}:_*) ,
          _.mask -> Vec(num_thread, Bool()).Lit((0 until num_thread).map{ _ -> false.B}:_*)
        )
      } else {
        new vTCData().Lit(
          _.in1 -> Vec(num_thread, UInt(xLen.W)).Lit((0 until num_thread).map{i => i-> RA(i).U}:_*) ,
          _.in2 -> Vec(num_thread, UInt(xLen.W)).Lit((0 until num_thread).map{i => i-> RB(i).U}:_*) ,
          _.in3 -> Vec(num_thread, UInt(xLen.W)).Lit((0 until num_thread).map{i => i-> RC(i).U}:_*) ,
          _.mask -> Vec(num_thread, Bool()).Lit((0 until num_thread).map{ i => i -> false.B}:_*)
        )
      }

      new TC_MMAInput_MixedPrecision2(new TCCtrl_mulslot_v2(32, 1)).Lit(
        _.data_in -> data_in,
        _.rm -> RoundingModes.RNE,
//        _.rm -> RoundingModes.RTZ,
//        _.rm -> RoundingModes.RDN,
//        _.rm -> RoundingModes.RUP,
//        _.rm -> RoundingModes.RMM,
//        _.ctrl -> true.B,
        _.ctrl -> new TCCtrl_mulslot_v2(32, 1).Lit(
          _.reg_idxw -> count.U,
          _.warpID -> 0.U,
          _.isMixedPrecisionMode -> true.B,
          _.tc_ReLU -> true.B,
          _.tc_shape -> 0.U,
          _.sel_slot_num -> 0.U,
//          _.spike_info -> 0.U.asTypeOf(InstWriteBack)//DontCare
          //_.spike_info -> if(SPIKE_OUTPUT) Some(new InstWriteBack) else None
        )
      )
    }
  }
//
  behavior of "Tensor Core MMA888 V2 mix prec"
  it should "TCComputationArray 888 FP16" in {
    test(new TensorCore_MixedPrecision_multslot_simple(8,8,8,num_warp,16,new TCCtrl_mulslot_v2(32, 1))).withAnnotations(Seq(WriteVcdAnnotation)) { d =>
      TCMMA888Input.reset
      d.io.in.initSource()
      d.io.in.setSourceClock(d.clock)
      d.io.out.initSink()
      d.io.out.setSinkClock(d.clock)
      d.clock.setTimeout(80)

      val input_list = Seq(TCMMA888Input(1))//,TCMMA888Input(0))//,TCMMA888Input(1),TCMMA888Input(0))
      val TC_input_sender = new RequestSender[TC_MMAInput_MixedPrecision2, TC_MMAOutput2](d.io.in, d.io.out)
      TC_input_sender.add(input_list)

      d.clock.setTimeout(0)
      d.clock.step(4)
      var clock_cnt = 0

//      d.clock.step()
//      TC_input_sender.eval()
//      d.io.out.ready.poke(true.B)
//      d.io.in.valid.poke(false.B)
//      d.clock.step(2)
//      d.io.in.valid.poke(true.B)
//      clock_cnt += 1

      while(clock_cnt <= 100){
        d.clock.step(1)
        TC_input_sender.eval()
        d.io.out.ready.poke(true.B)
        clock_cnt += 1
        d.clock.step(1)
      }
//      d.io.in.bits.isMixedPrecisionMode.poke(false.B)

//      d.clock.step(600)
      println("\n")
      val Rd = TCMMA888Input.readTxtFileToOneDimensionalArray("ventus/tests/src/TensorCoreTest/testData_888/RD.txt")
//      val Rd_torch = TCMMA888Input.readTxtFileToOneDimensionalArray("ventus/tests/src/TensorCoreTest/testData_888/RD_torch.txt")
      for (i <- 0 until 32) {
        val elementValue = d.io.out.bits.data_out(i).peek()
        val intValue: Int = elementValue(15,0).litValue.toInt
        val intValue2: Int = elementValue(31,16).litValue.toInt
//        val intValue3: Int = elementValue(47,32).litValue().toInt
//        val intValue4: Int = elementValue(63,48).litValue().toInt
        val hexString: String = f"$intValue%04x" // %016x 表示至少16位的16进制数，不足的前面补零
        val hexString2: String = f"$intValue2%04x" // %016x 表示至少16位的16进制数，不足的前面补零
//        val hexString3: String = f"$intValue3%04x" // %016x 表示至少16位的16进制数，不足的前面补零
//        val hexString4: String = f"$intValue4%04x" // %016x 表示至少16位的16进制数，不足的前面补零
//        println(s"data_out($i) = hex:$hexString")
        val std: String = Rd(i)
//        val std_torch: String = Rd_torch(i)
//        println(s"$i $hexString4$hexString3$hexString2$hexString, $std, $std_torch")
        println(s"$i $hexString2$hexString, $std")
      }
//      d.clock.step(30)
    }
  }
}


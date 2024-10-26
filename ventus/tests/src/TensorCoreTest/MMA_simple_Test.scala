//package TensorCoreTest
//
//import FPUv2.TCCtrl
//import FPUv2.utils.RoundingModes
//import TensorCore.{TC_MMAInput_MixedPrecision, TC_MMAOutput, TensorCore_MixedPrecision_multslot, TensorCore_MixedPrecision_multslot_simple, vTCData}
//import chisel3._
//import chisel3.experimental.BundleLiterals._
//import chisel3.experimental.VecLiterals.AddVecLiteralConstructor
//import chiseltest._
//import org.scalatest.flatspec.AnyFlatSpec
//import play.TestUtils.RequestSender
////import chisel3.util.Hexadecimal
//import top.parameters._
//
//import scala.io.Source
//
//class MMA_simple_Test extends AnyFlatSpec with ChiselScalatestTester {
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
//    def apply(pa:Int,isMix:Boolean,dep:Int) = {
//      count = (count + 1) % 32
//      val RA = readTxtFileToOneDimensionalArray("ventus/tests/src/TensorCoreTest/testData_888/RA.txt")
//      val RB = readTxtFileToOneDimensionalArray("ventus/tests/src/TensorCoreTest/testData_888/RB.txt")
//      val RC = readTxtFileToOneDimensionalArray("ventus/tests/src/TensorCoreTest/testData_888/RC.txt")
//
//      // 根据 pa 的值决定是否将 in1, in2, in3 赋值为 0
//      val data_in = if (pa == 0) {
//        new vTCData().Lit(
//          _.in1 -> Vec(num_thread, UInt(xLen.W)).Lit((0 until num_thread).map{_ -> 0.U}:_*) ,
//          _.in2 -> Vec(num_thread, UInt(xLen.W)).Lit((0 until num_thread).map{_ -> 0.U}:_*) ,
//          _.in3 -> Vec(num_thread, UInt(xLen.W)).Lit((0 until num_thread).map{_ -> 0.U}:_*) ,
//          _.mask -> Vec(num_thread, Bool()).Lit((0 until num_thread).map{ _ -> false.B}:_*)
//        )
//      } else {
//        new vTCData().Lit(
//          _.in1 -> Vec(num_thread, UInt(xLen.W)).Lit((0 until num_thread).map{i => i-> RA(i).U}:_*) ,
//          _.in2 -> Vec(num_thread, UInt(xLen.W)).Lit((0 until num_thread).map{i => i-> RB(i).U}:_*) ,
//          _.in3 -> Vec(num_thread, UInt(xLen.W)).Lit((0 until num_thread).map{i => i-> RC(i).U}:_*) ,
//          _.mask -> Vec(num_thread, Bool()).Lit((0 until num_thread).map{ i => i -> false.B}:_*)
//        )
//      }
//
//      new TC_MMAInput_MixedPrecision(new TCCtrl(32, dep)).Lit(
//        _.data_in -> data_in,
//        _.rm -> RoundingModes.RNE,
////        _.rm -> RoundingModes.RTZ,
////        _.rm -> RoundingModes.RDN,
////        _.rm -> RoundingModes.RUP,
////        _.rm -> RoundingModes.RMM,
//        _.isMixedPrecisionMode -> (if(isMix) true.B else false.B),
//        _.ctrl -> new TCCtrl(32, dep).Lit(
//          _.reg_idxw -> count.U,
//          _.warpID -> 3.U
//        )
//      )
//    }
//  }
//
//  behavior of "Tensor Core MMA mult-slot simple brand"
////  it should "TCComputationArray 888 FP16" in {
////    test(new TensorCore_MixedPrecision_multslot_simple(8,8,8,num_warp,16,new TCCtrl(32, 3))).withAnnotations(Seq(WriteVcdAnnotation)) { d =>
////      TCMMA888Input.reset
////      d.io.in.initSource()
////      d.io.in.setSourceClock(d.clock)
////
////      d.clock.setTimeout(80)
////
////      val input_list = Seq(TCMMA888Input(1,false,3))//,TCMMA888Input(0))//,TCMMA888Input(1),TCMMA888Input(0))
////      val TC_input_sender = new RequestSender[TC_MMAInput_MixedPrecision, TC_MMAOutput](d.io.in, d.io.out)
////      TC_input_sender.add(input_list)
////
////      d.clock.setTimeout(0)
////      d.clock.step(4)
////      var clock_cnt = 0
////
////      while(clock_cnt <= 100){
////        TC_input_sender.eval()
////        d.io.out.ready.poke(true.B)
////        clock_cnt += 1
////        d.clock.step(1)
////      }
////
////      println("\n")
////      val Rd = TCMMA888Input.readTxtFileToOneDimensionalArray("ventus/tests/src/TensorCoreTest/testData_888/RD.txt")
//////      val Rd_torch = TCMMA888Input.readTxtFileToOneDimensionalArray("ventus/tests/src/TensorCoreTest/testData_888/RD_torch.txt")
////      for (i <- 0 until 32) {
////        val elementValue = d.io.out.bits.data_out(i).peek()
////        val intValue: Int = elementValue(15,0).litValue().toInt
////        val intValue2: Int = elementValue(31,16).litValue().toInt
//////        val intValue3: Int = elementValue(47,32).litValue().toInt
//////        val intValue4: Int = elementValue(63,48).litValue().toInt
////        val hexString: String = f"$intValue%04x" // %016x 表示至少16位的16进制数，不足的前面补零
////        val hexString2: String = f"$intValue2%04x" // %016x 表示至少16位的16进制数，不足的前面补零
//////        val hexString3: String = f"$intValue3%04x" // %016x 表示至少16位的16进制数，不足的前面补零
//////        val hexString4: String = f"$intValue4%04x" // %016x 表示至少16位的16进制数，不足的前面补零
//////        println(s"data_out($i) = hex:$hexString")
////        val std: String = Rd(i)
//////        val std_torch: String = Rd_torch(i)
//////        println(s"$i $hexString4$hexString3$hexString2$hexString, $std, $std_torch")
////        println(s"$i $hexString2$hexString, $std")
////      }
//////      d.clock.step(30)
////    }
////  }
////
////  it should "TCComputationArray 888 FP16 mix prcision" in {
////    test(new TensorCore_MixedPrecision_multslot_simple(8,8,8,num_warp,16,new TCCtrl(32, 3))).withAnnotations(Seq(WriteVcdAnnotation)) { d =>
////      TCMMA888Input.reset
////      d.io.in.initSource()
////      d.io.in.setSourceClock(d.clock)
////      d.io.out.initSink()
////      d.io.out.setSinkClock(d.clock)
////      d.clock.setTimeout(80)
////
////      val input_list = Seq(TCMMA888Input(1,true,3))//,TCMMA888Input(0))//,TCMMA888Input(1),TCMMA888Input(0))
////      val TC_input_sender = new RequestSender[TC_MMAInput_MixedPrecision, TC_MMAOutput](d.io.in, d.io.out)
////      TC_input_sender.add(input_list)
////
////      d.clock.setTimeout(0)
////      d.clock.step(4)
////      var clock_cnt = 0
////
////      while(clock_cnt <= 100){
////        d.clock.step(1)
////        TC_input_sender.eval()
////        d.io.out.ready.poke(true.B)
////        clock_cnt += 1
////        d.clock.step(1)
////      }
////      println("\n")
////      val Rd = TCMMA888Input.readTxtFileToOneDimensionalArray("ventus/tests/src/TensorCoreTest/testData_888/RD.txt")
////      //      val Rd_torch = TCMMA888Input.readTxtFileToOneDimensionalArray("ventus/tests/src/TensorCoreTest/testData_888/RD_torch.txt")
////      for (i <- 0 until 32) {
////        val elementValue = d.io.out.bits.data_out(i).peek()
////        val intValue: Int = elementValue(15,0).litValue().toInt
////        val intValue2: Int = elementValue(31,16).litValue().toInt
////        //        val intValue3: Int = elementValue(47,32).litValue().toInt
////        //        val intValue4: Int = elementValue(63,48).litValue().toInt
////        val hexString: String = f"$intValue%04x" // %016x 表示至少16位的16进制数，不足的前面补零
////        val hexString2: String = f"$intValue2%04x" // %016x 表示至少16位的16进制数，不足的前面补零
////        //        val hexString3: String = f"$intValue3%04x" // %016x 表示至少16位的16进制数，不足的前面补零
////        //        val hexString4: String = f"$intValue4%04x" // %016x 表示至少16位的16进制数，不足的前面补零
////        //        println(s"data_out($i) = hex:$hexString")
////        val std: String = Rd(i)
////        //        val std_torch: String = Rd_torch(i)
////        //        println(s"$i $hexString4$hexString3$hexString2$hexString, $std, $std_torch")
////        println(s"$i $hexString2$hexString, $std")
////      }
////      //      d.clock.step(30)
////    }
////  }
//}
//

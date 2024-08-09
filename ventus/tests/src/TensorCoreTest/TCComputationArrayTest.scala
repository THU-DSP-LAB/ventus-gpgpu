package FPUv2

import FPUv2.utils.{FPUInput, RoundingModes, TestFPUCtrl}
import TensorCore.{TCComputationBinaryInput, TCComputationInput, TCComputationInput_MixedPrecision,TCComputationInput_Reuse, TCDotProduct, TC_ComputationArray_848_Binary, TC_ComputationArray_MixedPrecision,TC_ComputationArray_848_FP16, TC_ComputationArray_848_INT8FP16_Reuse}
import chisel3._
import chisel3.experimental.BundleLiterals._
import chisel3.experimental.VecLiterals.AddVecLiteralConstructor
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

import scala.io.Source

class TCComputationArrayTest extends AnyFlatSpec with ChiselScalatestTester {
  import TestArgs._
  object TCComputationInput {
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
    def apply(DimM:Int, DimN:Int, DimK:Int, xDatalen:Int) = {
      count = (count + 1) % 32
      val RA = readTxtFileToOneDimensionalArray("ventus/tests/src/TensorCoreTest/testData_848/RA.txt")
      val RB = readTxtFileToOneDimensionalArray("ventus/tests/src/TensorCoreTest/testData_848/RB.txt")
      val RC = readTxtFileToOneDimensionalArray("ventus/tests/src/TensorCoreTest/testData_848/RC.txt")

      new TCComputationInput(DimM,DimN,DimK, xDatalen, new TCCtrl(32, 1)).Lit(
        _.A -> Vec(DimM*DimK, UInt(xDatalen.W)).Lit(
//          (0 until DimN).map{ i => i -> toUInt(a(i)).U}:_*
//          (0 until DimM*DimK).map{ i => i -> "b0011110000000000".U}:_*
          (0 until DimM*DimK).map{i => i-> RA(i).U}:_*
        ),
        _.B -> Vec(DimN*DimK, UInt(xDatalen.W)).Lit(
//          (0 until DimN).map{ i => i -> toUInt(b(i)).U}:_*
//        (0 until DimN*DimK).map{ i => i -> "b0011110000000000".U}:_*
          (0 until DimN*DimK).map{i => i-> RB(i).U}:_*
        ),
        _.C -> Vec(DimN*DimM, UInt(xDatalen.W)).Lit(
          //          (0 until DimN).map{ i => i -> toUInt(b(i)).U}:_*
//          (0 until DimN*DimM).map{ i => i -> "b0011110000000000".U}:_*
          (0 until DimN*DimM).map{i => i-> RC(i).U}:_*
            ),
        _.rm -> RoundingModes.RNE,
        _.ctrl -> new TCCtrl(32, 1).Lit(
          _.reg_idxw -> count.U,
          _.warpID -> 0.U
        )
      )
    }

  }

  object TCComputationInput_mix {
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
    def apply(DimM:Int, DimN:Int, DimK:Int, xDatalen:Int) = {
      count = (count + 1) % 32
      val RA = readTxtFileToOneDimensionalArray("ventus/tests/src/TensorCoreTest/testData_848_mix/RA.txt")
      val RB = readTxtFileToOneDimensionalArray("ventus/tests/src/TensorCoreTest/testData_848_mix/RB.txt")
      val RC = readTxtFileToOneDimensionalArray("ventus/tests/src/TensorCoreTest/testData_848_mix/RC.txt")

      new TCComputationInput_MixedPrecision(DimM,DimN,DimK, xDatalen, new TCCtrl(32, 1)).Lit(
        _.A -> Vec(DimM*DimK, UInt(xDatalen.W)).Lit(
          //          (0 until DimN).map{ i => i -> toUInt(a(i)).U}:_*
          //          (0 until DimM*DimK).map{ i => i -> "b0011110000000000".U}:_*
          (0 until DimM*DimK).map{i => i-> RA(i).U}:_*
        ),
        _.B -> Vec(DimN*DimK, UInt(xDatalen.W)).Lit(
          //          (0 until DimN).map{ i => i -> toUInt(b(i)).U}:_*
          //        (0 until DimN*DimK).map{ i => i -> "b0011110000000000".U}:_*
          (0 until DimN*DimK).map{i => i-> RB(i).U}:_*
        ),
        _.C -> Vec(DimN*DimM, UInt(32.W)).Lit(
          //          (0 until DimN).map{ i => i -> toUInt(b(i)).U}:_*
          //          (0 until DimN*DimM).map{ i => i -> "b0011110000000000".U}:_*
          (0 until DimN*DimM).map{i => i-> RC(i).U(32.W)}:_*
        ),
        _.rm -> RoundingModes.RNE,
        _.isMixedPrecisionMode-> true.B,
        _.ctrl -> new TCCtrl(32, 1).Lit(
          _.reg_idxw -> count.U,
          _.warpID -> 0.U
        )
      )
    }

  }
  object TCComputationInput_mix_fp16 {
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
    def apply(DimM:Int, DimN:Int, DimK:Int, xDatalen:Int) = {
      count = (count + 1) % 32
      val RA = readTxtFileToOneDimensionalArray("ventus/tests/src/TensorCoreTest/testData_848/RA.txt")
      val RB = readTxtFileToOneDimensionalArray("ventus/tests/src/TensorCoreTest/testData_848/RB.txt")
      val RC = readTxtFileToOneDimensionalArray("ventus/tests/src/TensorCoreTest/testData_848/RC.txt")

      new TCComputationInput_MixedPrecision(DimM,DimN,DimK, xDatalen, new TCCtrl(32, 1)).Lit(
        _.A -> Vec(DimM*DimK, UInt(xDatalen.W)).Lit(
          //          (0 until DimN).map{ i => i -> toUInt(a(i)).U}:_*
          //          (0 until DimM*DimK).map{ i => i -> "b0011110000000000".U}:_*
          (0 until DimM*DimK).map{i => i-> RA(i).U}:_*
        ),
        _.B -> Vec(DimN*DimK, UInt(xDatalen.W)).Lit(
          //          (0 until DimN).map{ i => i -> toUInt(b(i)).U}:_*
          //        (0 until DimN*DimK).map{ i => i -> "b0011110000000000".U}:_*
          (0 until DimN*DimK).map{i => i-> RB(i).U}:_*
        ),
        _.C -> Vec(DimN*DimM, UInt(32.W)).Lit(
          //          (0 until DimN).map{ i => i -> toUInt(b(i)).U}:_*
          //          (0 until DimN*DimM).map{ i => i -> "b0011110000000000".U}:_*
          (0 until DimN*DimM).map{i => i-> RC(i).U(32.W)}:_*
        ),
        _.rm -> RoundingModes.RNE,
        _.isMixedPrecisionMode-> false.B,
        _.ctrl -> new TCCtrl(32, 1).Lit(
          _.reg_idxw -> count.U,
          _.warpID -> 0.U
        )
      )
    }

  }

  object TCComputationBinaryInput {
    var count = 0
    def reset = { count = 0 }
    def apply(DimM:Int, DimN:Int, DimK:Int) = {
      count = (count + 1) % 32
      new TCComputationBinaryInput(DimM, DimN, DimK, new TCCtrl(32, 1)).Lit(
        _.A -> Vec(DimM, UInt(DimK.W)).Lit(
          (0 until DimM).map{ i => i -> "b00111100".U}:_*
        ),
        _.B -> Vec(DimN, UInt(DimK.W)).Lit(
          (0 until DimN).map{ i => i -> "b00000000".U}:_*
        ),
        _.C -> Vec(DimN*DimM, UInt(DimK.W)).Lit(
          (0 until DimN*DimM).map{ i => i -> "b00000001".U}:_*
        ),
        _.rm -> RoundingModes.RNE,
        _.ctrl -> new TCCtrl(32, 1).Lit(
          _.reg_idxw -> count.U,
          _.warpID -> 0.U
        )
      )
    }

  }

  object TCComputationInput_INT8Reuse {
    var count = 0

    def reset = {
      count = 0
    }

    def apply(DimM: Int, DimN: Int, DimK: Int, xDatalen:Int) = {
      count = (count + 1) % 32
      new TCComputationInput_Reuse(DimM, DimN, DimK, xDatalen, new TCCtrl(32, 1)).Lit(
        _.A -> Vec(DimM * DimK, UInt(xDatalen.W)).Lit(
          //          (0 until DimN).map{ i => i -> toUInt(a(i)).U}:_*
          (0 until DimM * DimK).map { i => i -> "b0000000000010000".U }: _*
        ),
        _.B -> Vec(DimN * DimK, UInt(xDatalen.W)).Lit(
          //          (0 until DimN).map{ i => i -> toUInt(b(i)).U}:_*
          (0 until DimN * DimK).map { i => i -> "b0000000000100000".U }: _*
        ),
        _.C -> Vec(DimN * DimM, UInt(xDatalen.W)).Lit(
          //          (0 until DimN).map{ i => i -> toUInt(b(i)).U}:_*
          (0 until DimN * DimM).map { i => i -> 1.U }: _*
        ),
        _.rm -> RoundingModes.RNE,
        _.isInt -> true.B,
        _.ctrl -> new TCCtrl(32, 1).Lit(
          _.reg_idxw -> count.U,
          _.warpID -> 0.U
        )
      )
    }
  }

  object TCComputationInput_FP16Reuse {
      var count = 0

      def reset = {
        count = 0
      }

      def apply(DimM: Int, DimN: Int, DimK: Int, xDatalen:Int) = {
        count = (count + 1) % 32
        new TCComputationInput_Reuse(DimM, DimN, DimK, xDatalen, new TCCtrl(32, 1)).Lit(
          _.A -> Vec(DimM * DimK, UInt(xDatalen.W)).Lit(
            (0 until DimM * DimK).map { i => i -> "b0011110000000000".U }: _*
          ),
          _.B -> Vec(DimN * DimK, UInt(xDatalen.W)).Lit(
            (0 until DimN * DimK).map { i => i -> "b0011110000000000".U }: _*
          ),
          _.C -> Vec(DimN * DimM, UInt(xDatalen.W)).Lit(
            (0 until DimN * DimM).map { i => i -> "b0011110000000000".U }: _*
          ),
          _.rm -> RoundingModes.RNE,
          _.isInt -> false.B,
          _.ctrl -> new TCCtrl(32, 1).Lit(
            _.reg_idxw -> count.U,
            _.warpID -> 0.U
          )
        )
      }
    }


  behavior of "Tensor Core Computation Array"
  it should "TCComputationArray 848 FP16 new data" in {
    test(new TC_ComputationArray_848_FP16(16,8,4,8,new TCCtrl(32, 1))).withAnnotations(Seq(WriteVcdAnnotation)) { d =>
      TCComputationInput.reset
      d.io.in.initSource()
      d.io.in.setSourceClock(d.clock)
      d.io.out.initSink()
      d.io.out.setSinkClock(d.clock)
      d.clock.setTimeout(20)
      d.io.out.ready.poke(true.B)
      d.io.in.valid.poke(true.B)
      fork{
        d.io.in.enqueueSeq(Seq(
//          TCComputationInput(8,4,8),
          TCComputationInput(8,4,8,16)
        ))
      }.fork {
        d.io.out.ready.poke(true.B)
      }.join()
      d.clock.step(10)
//      for(i <- 0 until 32) {
//        println(i,d.io.out.bits.data(i).result.peek(),d.io.out.bits.data(i).fflags.peek())
//      }
      println("\n")
      val Rd = TCComputationInput.readTxtFileToOneDimensionalArray("ventus/tests/src/TensorCoreTest/testData_848/RD.txt")
      val Rd_torch = TCComputationInput.readTxtFileToOneDimensionalArray("ventus/tests/src/TensorCoreTest/testData_848/RD_torch.txt")
      for (i <- 0 until 32) {
        val elementValue = d.io.out.bits.data(i).result.peek()
        val intValue: Int = elementValue.litValue().toInt
//        val intValue2: Int = elementValue(63,32).litValue().toInt
        val hexString: String = f"$intValue%04x" // %016x 表示至少16位的16进制数，不足的前面补零
//        val hexString2: String = f"$intValue2%08x" // %016x 表示至少16位的16进制数，不足的前面补零
        //        println(s"data_out($i) = hex:$hexString")
        val std: String = Rd(i)
        val std_torch: String = Rd_torch(i)
        println(s"$i $hexString, $std, $std_torch")
      }
    }
  }
  it should "TCComputationArray 848 mixed Precision" in {
    test(new TC_ComputationArray_MixedPrecision(16,8,4,8,new TCCtrl(32, 1))).withAnnotations(Seq(WriteVcdAnnotation)) { d =>
      TCComputationInput.reset
      d.io.in.initSource()
      d.io.in.setSourceClock(d.clock)
      d.io.out.initSink()
      d.io.out.setSinkClock(d.clock)
      d.clock.setTimeout(20)
      d.io.out.ready.poke(true.B)
      d.io.in.valid.poke(true.B)
//      d.io.in.bits.isMixedPrecisionMode.poke(false.B)
      fork{
        d.io.in.enqueueSeq(Seq(
          //          TCComputationInput(8,4,8),
          TCComputationInput_mix(8,4,8,16)
        ))
      }.fork {
        d.io.out.ready.poke(true.B)
      }.join()
      d.io.in.valid.poke(false.B)
      d.clock.step(1)

      d.clock.step(10)
      //      for(i <- 0 until 32) {
      //        println(i,d.io.out.bits.data(i).result.peek(),d.io.out.bits.data(i).fflags.peek())
      //      }
      println("\n")
      val Rd = TCComputationInput.readTxtFileToOneDimensionalArray("ventus/tests/src/TensorCoreTest/testData_848_mix/RD.txt")
      val Rd_torch = TCComputationInput.readTxtFileToOneDimensionalArray("ventus/tests/src/TensorCoreTest/testData_848_mix/RD.txt")
      for (i <- 0 until 32) {
        val elementValue = d.io.out.bits.data(i).result.peek()
        val intValue: Int = elementValue(15,0).litValue().toInt
        val intValue2: Int = elementValue(31,16).litValue().toInt
        val hexString: String = f"$intValue%04x" // %016x 表示至少16位的16进制数，不足的前面补零
        val hexString2: String = f"$intValue2%04x" // %016x 表示至少16位的16进制数，不足的前面补零

        val std: String = Rd(i)
        val std_torch: String = Rd_torch(i)
        println(s"$i $hexString2$hexString, $std")
      }
    }
  }
  it should "TCComputationArray 848 mixed Precision fp16 mode" in {
    test(new TC_ComputationArray_MixedPrecision(16,8,4,8,new TCCtrl(32, 1))).withAnnotations(Seq(WriteVcdAnnotation)) { d =>
      TCComputationInput.reset
      d.io.in.initSource()
      d.io.in.setSourceClock(d.clock)
      d.io.out.initSink()
      d.io.out.setSinkClock(d.clock)
      d.clock.setTimeout(20)
      d.io.out.ready.poke(true.B)
      d.io.in.valid.poke(true.B)
      //      d.io.in.bits.isMixedPrecisionMode.poke(false.B)
      fork{
        d.io.in.enqueueSeq(Seq(
          //          TCComputationInput(8,4,8),
          TCComputationInput_mix_fp16(8,4,8,16)
        ))
      }.fork {
        d.io.out.ready.poke(true.B)
      }.join()
      d.io.in.valid.poke(false.B)
      d.clock.step(1)

      d.clock.step(10)
      //      for(i <- 0 until 32) {
      //        println(i,d.io.out.bits.data(i).result.peek(),d.io.out.bits.data(i).fflags.peek())
      //      }
      println("\n")
      val Rd = TCComputationInput.readTxtFileToOneDimensionalArray("ventus/tests/src/TensorCoreTest/testData_848/RD.txt")
      val Rd_torch = TCComputationInput.readTxtFileToOneDimensionalArray("ventus/tests/src/TensorCoreTest/testData_848/RD.txt")
      for (i <- 0 until 32) {
        val elementValue = d.io.out.bits.data(i).result.peek()
        val intValue: Int = elementValue(15,0).litValue().toInt
        val intValue2: Int = elementValue(31,16).litValue().toInt
        val hexString: String = f"$intValue%04x" // %016x 表示至少16位的16进制数，不足的前面补零
        val hexString2: String = f"$intValue2%04x" // %016x 表示至少16位的16进制数，不足的前面补零

        val std: String = Rd(i)
        val std_torch: String = Rd_torch(i)
        println(s"$i $hexString2$hexString, $std")
      }
    }
  }

  it should "TCComputationArray 848 Binary" in {
    test(new TC_ComputationArray_848_Binary(8,4,8,new TCCtrl(32, 1))).withAnnotations(Seq(WriteVcdAnnotation)) { d =>
      TCComputationInput.reset
      d.io.in.initSource()
      d.io.in.setSourceClock(d.clock)
      d.io.out.initSink()
      d.io.out.setSinkClock(d.clock)
      d.clock.setTimeout(20)
      d.io.out.ready.poke(true.B)
      d.io.in.valid.poke(true.B)
      fork{
        d.io.in.enqueueSeq(Seq(
          TCComputationBinaryInput(8,4,8)
        ))
      }.fork {
        d.io.out.ready.poke(true.B)
      }.join()
      d.clock.step(10)
      for(i <- 0 until 32) {
        println(i,d.io.out.bits.data(i).result.peek(),d.io.out.bits.data(i).fflags.peek())
      }
    }
  }
  it should "TCComputationArray 848 INT8 Reuse" in {
    test(new TC_ComputationArray_848_INT8FP16_Reuse(16,8,4,8,new TCCtrl(32, 1))).withAnnotations(Seq(WriteVcdAnnotation)) { d =>
      TCComputationInput_INT8Reuse.reset
      d.io.in.initSource()
      d.io.in.setSourceClock(d.clock)
      d.io.out.initSink()
      d.io.out.setSinkClock(d.clock)
      d.clock.setTimeout(20)
      d.io.out.ready.poke(true.B)
      d.io.in.valid.poke(true.B)
      fork{
        d.io.in.enqueueSeq(Seq(
          TCComputationInput_INT8Reuse(8,4,8,16)
        ))
      }.fork {
        d.io.out.ready.poke(true.B)
      }.join()
      d.clock.step(10)
      for(i <- 0 until 32) {
        println(i,d.io.out.bits.data(i).result.peek(),d.io.out.bits.data(i).fflags.peek())
      }
    }
  }
  it should "TCComputationArray 848 FP16 Reuse" in {
    test(new TC_ComputationArray_848_INT8FP16_Reuse(16,8,4,8,new TCCtrl(32, 1))).withAnnotations(Seq(WriteVcdAnnotation)) { d =>
      TCComputationInput_FP16Reuse.reset
      d.io.in.initSource()
      d.io.in.setSourceClock(d.clock)
      d.io.out.initSink()
      d.io.out.setSinkClock(d.clock)
      d.clock.setTimeout(20)
      d.io.out.ready.poke(true.B)
      d.io.in.valid.poke(true.B)
      fork{
        d.io.in.enqueueSeq(Seq(
          TCComputationInput_FP16Reuse(8,4,8,16)
        ))
      }.fork {
        d.io.out.ready.poke(true.B)
      }.join()
      d.clock.step(10)
      for(i <- 0 until 32) {
        println(i,d.io.out.bits.data(i).result.peek(),d.io.out.bits.data(i).fflags.peek())
      }
    }
  }

}


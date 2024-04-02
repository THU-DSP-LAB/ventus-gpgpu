import chisel3._
import chisel3.util._
import chiseltest._
import testparameters._
import org.scalatest.flatspec.AnyFlatSpec

object testparameters {
  def numgroup: Int = 4
}
class SimpleModule extends Module {
  val io = IO(new Bundle {
    val current_mask = Input(UInt(4.W))
    val current_mask_index = Output(Vec(4, UInt(2.W)))
  })

  // 初始化输出数组
  val initialMaskIndices = VecInit(Seq.fill(4)(0.U(2.W)))

  // 使用foldLeft进行遍历和条件判断
  val (maskIndices, _) = (0 until 4).foldLeft((initialMaskIndices, 0.U(2.W))) {
    case ((indices, cnt_mask), i) =>
      val newIndices = Wire(Vec(4, UInt(2.W)))
      var newCntMask = Wire(UInt(2.W))

      // Copy previous indices to newIndices
      for (j <- 0 until 4) { newIndices(j) := indices(j) }

      // Conditionally update the index and count
      when(io.current_mask(i) === 1.U) {
        newIndices(cnt_mask) := i.U
        newCntMask := cnt_mask + 1.U
      }.otherwise { newCntMask := cnt_mask }

      (newIndices, newCntMask)
  }

  // Assign computed indices to output
  io.current_mask_index := maskIndices
}



// 生成Verilog代码
object SimpleModuleVerilog extends App {
  println("Generating the Verilog code for SimpleModule")
  val targetDir = "ventus/tests/src/masktest"
  (new chisel3.stage.ChiselStage).emitVerilog(new SimpleModule, Array("--target-dir", targetDir) ++ args)
}

class SimpleModuleTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "SimpleModule"

  it should "print the current_mask_index for various masks in reverse order" in {
    test(new SimpleModule) { c =>
      // 设置mask并按倒序打印index结果
      val masks = Seq("b0001", "b0010", "b0101", "b0110", "b1100", "b0111", "b1111")

      masks.foreach { mask =>
        c.io.current_mask.poke(mask.U)
        c.clock.step(1)

        // 分别获取Vec中每个元素的值，然后倒序
        val indices = (0 until 4).map(i => c.io.current_mask_index(i).peek().litValue).reverse

        // 打印结果
        println(s"Mask: $mask, Index: ${indices.mkString(" ")}")
      }
    }
  }
}
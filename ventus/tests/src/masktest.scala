import chisel3._
import chisel3.util._
import chiseltest._
import testparameters._

// 假设numgroup是一个预先定义的参数
object testparameters {
  def numgroup: Int = 4 // 仅作为示例，实际中你应该根据需求设置
}

class SimpleModule extends Module {
  val io = IO(new Bundle {
    val current_mask = Input(Vec(numgroup, Bool()))
    val current_mask_index = Output(Vec(numgroup, UInt(log2Ceil(numgroup).W)))
  })

  // 初始化输出向量，以避免未定义行为
  io.current_mask_index.foreach(_ := 0.U)
  // 初始cnt_mask为0
  var cnt_mask = 0

  // 遍历current_mask，对于每个为true的位，记录其索引
  for (x <- 0 until numgroup) {
    when(io.current_mask(x)) {
      io.current_mask_index(cnt_mask) := x.U
      cnt_mask = cnt_mask + 1
    }
  }
}

// 生成Verilog代码
object SimpleModuleVerilog extends App {
  println("Generating the Verilog code for SimpleModule")
  (new chisel3.stage.ChiselStage).emitVerilog(new SimpleModule, args)
}
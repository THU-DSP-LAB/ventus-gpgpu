package pipeline

import chisel3._
import chisel3.util._
import top.parameters._

class OperandReadReturnStage extends Module {
  private val readChoiceWidth = log2Ceil(4 * num_collectorUnit)

  val io = IO(new Bundle {
    val chosenScalarIn = Input(Vec(num_bank, UInt(readChoiceWidth.W)))
    val chosenVectorIn = Input(Vec(num_bank, UInt(readChoiceWidth.W)))
    val validScalarIn = Input(Vec(num_bank, Bool()))
    val validVectorIn = Input(Vec(num_bank, Bool()))
    val dataScalarIn = Input(Vec(num_bank, UInt(xLen.W)))
    val dataVectorIn = Input(Vec(num_bank, Vec(num_thread, UInt(xLen.W))))

    val chosenScalarOut = Output(Vec(num_bank, UInt(readChoiceWidth.W)))
    val chosenVectorOut = Output(Vec(num_bank, UInt(readChoiceWidth.W)))
    val validScalarOut = Output(Vec(num_bank, Bool()))
    val validVectorOut = Output(Vec(num_bank, Bool()))
    val dataScalarOut = Output(Vec(num_bank, UInt(xLen.W)))
    val dataVectorOut = Output(Vec(num_bank, Vec(num_thread, UInt(xLen.W))))
  })

  private val validScalarSt1 = RegNext(io.validScalarIn, VecInit.fill(num_bank)(false.B))
  private val validVectorSt1 = RegNext(io.validVectorIn, VecInit.fill(num_bank)(false.B))
  io.validScalarOut := RegNext(validScalarSt1, VecInit.fill(num_bank)(false.B))
  io.validVectorOut := RegNext(validVectorSt1, VecInit.fill(num_bank)(false.B))

  io.chosenScalarOut := RegNext(RegNext(io.chosenScalarIn))
  io.chosenVectorOut := RegNext(RegNext(io.chosenVectorIn))

  private val scalarDataSt2 = Reg(Vec(num_bank, UInt(xLen.W)))
  private val vectorDataSt2 = Reg(Vec(num_bank, Vec(num_thread, UInt(xLen.W))))

  for (bankIdx <- 0 until num_bank) {
    when(validScalarSt1(bankIdx)) {
      scalarDataSt2(bankIdx) := io.dataScalarIn(bankIdx)
    }
    when(validVectorSt1(bankIdx)) {
      vectorDataSt2(bankIdx) := io.dataVectorIn(bankIdx)
    }
  }

  io.dataScalarOut := scalarDataSt2
  io.dataVectorOut := vectorDataSt2
}

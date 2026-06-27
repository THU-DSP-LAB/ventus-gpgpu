package pipeline

import chisel3._
import chisel3.util._
import top.parameters._

class OperandBankReadReturnStage extends Module {
  private val readChoiceWidth = log2Ceil(num_collectorUnit).max(1)

  val io = IO(new Bundle {
    val chosenScalarIn = Input(UInt(readChoiceWidth.W))
    val chosenVectorIn = Input(UInt(readChoiceWidth.W))
    val regOrderScalarIn = Input(UInt(2.W))
    val regOrderVectorIn = Input(UInt(2.W))
    val validScalarIn = Input(Bool())
    val validVectorIn = Input(Bool())
    val dataScalarIn = Input(UInt(xLen.W))
    val dataVectorIn = Input(Vec(num_thread, UInt(xLen.W)))

    val chosenScalarOut = Output(UInt(readChoiceWidth.W))
    val chosenVectorOut = Output(UInt(readChoiceWidth.W))
    val regOrderScalarOut = Output(UInt(2.W))
    val regOrderVectorOut = Output(UInt(2.W))
    val validScalarOut = Output(Bool())
    val validVectorOut = Output(Bool())
    val dataScalarOut = Output(UInt(xLen.W))
    val dataVectorOut = Output(Vec(num_thread, UInt(xLen.W)))
  })

  private val validScalarSt1 = RegNext(io.validScalarIn, false.B)
  private val validVectorSt1 = RegNext(io.validVectorIn, false.B)
  io.validScalarOut := RegNext(validScalarSt1, false.B)
  io.validVectorOut := RegNext(validVectorSt1, false.B)

  io.chosenScalarOut := RegNext(RegNext(io.chosenScalarIn, 0.U), 0.U)
  io.chosenVectorOut := RegNext(RegNext(io.chosenVectorIn, 0.U), 0.U)
  io.regOrderScalarOut := RegNext(RegNext(io.regOrderScalarIn, 0.U), 0.U)
  io.regOrderVectorOut := RegNext(RegNext(io.regOrderVectorIn, 0.U), 0.U)

  private val scalarDataSt2 = RegInit(0.U(xLen.W))
  private val vectorDataSt2 = RegInit(VecInit(Seq.fill(num_thread)(0.U(xLen.W))))

  when(validScalarSt1) {
    scalarDataSt2 := io.dataScalarIn
  }
  when(validVectorSt1) {
    vectorDataSt2 := io.dataVectorIn
  }

  io.dataScalarOut := scalarDataSt2
  io.dataVectorOut := vectorDataSt2
}

class OperandReadReturnStage extends Module {
  private val readChoiceWidth = log2Ceil(num_collectorUnit).max(1)

  val io = IO(new Bundle {
    val chosenScalarIn = Input(Vec(num_bank, UInt(readChoiceWidth.W)))
    val chosenVectorIn = Input(Vec(num_bank, UInt(readChoiceWidth.W)))
    val regOrderScalarIn = Input(Vec(num_bank, UInt(2.W)))
    val regOrderVectorIn = Input(Vec(num_bank, UInt(2.W)))
    val validScalarIn = Input(Vec(num_bank, Bool()))
    val validVectorIn = Input(Vec(num_bank, Bool()))
    val dataScalarIn = Input(Vec(num_bank, UInt(xLen.W)))
    val dataVectorIn = Input(Vec(num_bank, Vec(num_thread, UInt(xLen.W))))

    val chosenScalarOut = Output(Vec(num_bank, UInt(readChoiceWidth.W)))
    val chosenVectorOut = Output(Vec(num_bank, UInt(readChoiceWidth.W)))
    val regOrderScalarOut = Output(Vec(num_bank, UInt(2.W)))
    val regOrderVectorOut = Output(Vec(num_bank, UInt(2.W)))
    val validScalarOut = Output(Vec(num_bank, Bool()))
    val validVectorOut = Output(Vec(num_bank, Bool()))
    val dataScalarOut = Output(Vec(num_bank, UInt(xLen.W)))
    val dataVectorOut = Output(Vec(num_bank, Vec(num_thread, UInt(xLen.W))))
  })

  for (bankIdx <- 0 until num_bank) {
    val bankStage = Module(new OperandBankReadReturnStage)
    bankStage.suggestName(s"bankReadReturnStage_$bankIdx")
    bankStage.io.chosenScalarIn := io.chosenScalarIn(bankIdx)
    bankStage.io.chosenVectorIn := io.chosenVectorIn(bankIdx)
    bankStage.io.regOrderScalarIn := io.regOrderScalarIn(bankIdx)
    bankStage.io.regOrderVectorIn := io.regOrderVectorIn(bankIdx)
    bankStage.io.validScalarIn := io.validScalarIn(bankIdx)
    bankStage.io.validVectorIn := io.validVectorIn(bankIdx)
    bankStage.io.dataScalarIn := io.dataScalarIn(bankIdx)
    bankStage.io.dataVectorIn := io.dataVectorIn(bankIdx)
    io.chosenScalarOut(bankIdx) := bankStage.io.chosenScalarOut
    io.chosenVectorOut(bankIdx) := bankStage.io.chosenVectorOut
    io.regOrderScalarOut(bankIdx) := bankStage.io.regOrderScalarOut
    io.regOrderVectorOut(bankIdx) := bankStage.io.regOrderVectorOut
    io.validScalarOut(bankIdx) := bankStage.io.validScalarOut
    io.validVectorOut(bankIdx) := bankStage.io.validVectorOut
    io.dataScalarOut(bankIdx) := bankStage.io.dataScalarOut
    io.dataVectorOut(bankIdx) := bankStage.io.dataVectorOut
  }
}

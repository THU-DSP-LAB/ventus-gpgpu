package play

import chisel3._
import chisel3.util._
import chisel3.util.experimental.BoringUtils


class Adder extends Module {
	val io = IO(new Bundle{
		val src0 = Input(UInt(64.W))
		val src1 = Input(UInt(64.W))
		val result = Output(UInt(64.W))
	})
	io.result := io.src0 + io.src1
}

//object hello_gen extends App{
//  (new chisel3.stage.ChiselStage).emitVerilog(new Adder())
//}

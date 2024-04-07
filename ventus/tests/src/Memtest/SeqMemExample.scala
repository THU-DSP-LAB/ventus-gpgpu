package Memtest

import chisel3._
import chisel3.util._

class SeqMemExample(depth: Int) extends Module {
  val io = IO(new Bundle {
    val addr = Input(UInt(log2Ceil(depth).W))
    val writeData = Input(UInt(8.W))
    val writeEnable = Input(Bool())
    val readData = Output(UInt(8.W))
  })

  val memory = SyncReadMem(depth, UInt(8.W))

  when(io.writeEnable) {
    memory.write(io.addr, io.writeData)
  }

  io.readData := memory.read(io.addr, io.writeEnable)
}

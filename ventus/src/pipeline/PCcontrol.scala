package pipeline

import chisel3._

class PCcontrol() extends Module{
  val io=IO(new Bundle{
    val New_PC=Input(UInt(32.W))
    val PC_replay=Input(Bool())
    val PC_src=Input(UInt(2.W))
    val PC_next=Output(UInt(32.W))
    //val warpnum=Output(UInt(1.W))
  })
  val pout=RegInit(0.U(32.W))

  //val warpID=RegInit(ID.U(1.W))//PC_src=0:PC+4  PC_src=1:new PC PC_src=2:PC
  when(io.PC_replay){
    pout:=pout
  }.elsewhen(io.PC_src===2.U){
    pout:=pout+4.U
  }.elsewhen(io.PC_src===1.U){
    pout:=io.New_PC
  }.elsewhen(io.PC_src===3.U){
    pout:=pout-8.U
  }.otherwise{
    pout:=pout
  }
  io.PC_next:=pout
  //io.warpnum:=warpID
}

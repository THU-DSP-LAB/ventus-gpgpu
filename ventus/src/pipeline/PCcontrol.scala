/*
 * Copyright (c) 2021-2022 International Innovation Center of Tsinghua University, Shanghai
 * Ventus is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details. */
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

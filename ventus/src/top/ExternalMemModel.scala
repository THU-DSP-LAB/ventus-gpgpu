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
package top

import chisel3._
import chisel3.util._
import chisel3.util.experimental.loadMemoryFromFile
import L1Cache.DCache.DCacheModule
import L2cache.{InclusiveCacheParameters_lite, TLBundleA_lite, TLBundleD_lite}
import config.config.Parameters

class ExternalMemModel(C: TestCase#Props, params: InclusiveCacheParameters_lite)(implicit p: Parameters) extends DCacheModule {
  val io = IO(new Bundle{
    val memReq = Flipped(DecoupledIO(new TLBundleA_lite(params)))
    val memRsp= DecoupledIO(new TLBundleD_lite(params))

    //ports under tb control
    val memReq_ready = Input(Bool())
  })
  io.memReq.ready := io.memReq_ready

  // ***** model params *****
  val ExtMemBlockWords = BlockWords
  val ExtMemWordLength = WordLength

  //整个External Memory model划分成了两个独立的存储空间
  //为了方便ICache和DCache各自使用不同的txt
  val ExtMemBase1 = 0
  val ExtMemSize1 = parameters.sharedmem_depth//Unit: block(typical 32 words)
  val ExtMemBase2 = top.parameters.sharedmem_depth
  val ExtMemSize2 = 128//Unit: block(typical 32 words)
  assert(ExtMemBase1 <= ExtMemBase2,"ExtMemBase1 > ExtMemBase2")
  assert(ExtMemBase1 + ExtMemSize1 <= ExtMemBase2,"space overlap in ExtMem")

  def get_ExtMemBlockAddr(addr: UInt) = (addr >> (log2Up(ExtMemBlockWords)+2)).asUInt
  val ExtMemLatency = 20

  val memory1 = Mem(ExtMemSize1*ExtMemBlockWords,UInt(ExtMemWordLength.W))
  loadMemoryFromFile(memory1,C.inst_filepath)
  val memory2 = Mem(ExtMemSize2*ExtMemBlockWords,UInt(ExtMemWordLength.W))
  loadMemoryFromFile(memory2,C.data_filepath)

  val readVec = Wire(Vec(ExtMemBlockWords,UInt(ExtMemWordLength.W)))
  val writeVec = Wire(Vec(ExtMemBlockWords,UInt(ExtMemWordLength.W)))
  writeVec := io.memReq.bits.data.asTypeOf(writeVec)

  val BlockAddr = Wire(UInt((ExtMemWordLength-log2Up(ExtMemBlockWords)-2).W))
  BlockAddr := get_ExtMemBlockAddr(io.memReq.bits.address)
  val perMemoryBlockAddr = Wire(UInt((ExtMemWordLength-log2Up(ExtMemBlockWords)-2).W))
  val isSpace1 = Wire(Bool())
  val isSpace2 = Wire(Bool())
  when (BlockAddr-ExtMemBase1.asUInt < ExtMemSize1.asUInt){
    isSpace1 := true.B
    isSpace2 := false.B
    perMemoryBlockAddr := BlockAddr - ExtMemBase1.asUInt
  }.elsewhen(BlockAddr-ExtMemBase2.asUInt < ExtMemSize2.asUInt){
    isSpace1 := false.B
    isSpace2 := true.B
    perMemoryBlockAddr := BlockAddr - ExtMemBase2.asUInt
  }.otherwise{
    isSpace1 := false.B
    isSpace2 := false.B
    perMemoryBlockAddr := BlockAddr
    printf(p"[ExtMem]: incoming addr out of range+${BlockAddr}\n")
    //assert(cond = false,"[ExtMem]: incoming addr out of range"+"${BlockAddr}")
  }

  val wordAddr = Wire(Vec(ExtMemBlockWords,UInt((ExtMemWordLength-2).W)))

  for (i<- 0 until ExtMemBlockWords){
    wordAddr(i) := Cat(perMemoryBlockAddr,i.U(log2Up(ExtMemBlockWords).W))
    when(io.memReq.fire && io.memReq.bits.opcode === TLAOp_PutFull){
      when(isSpace1){
        memory1(wordAddr(i)) := writeVec(i)
      }.elsewhen(isSpace2){
        memory2(wordAddr(i)) := writeVec(i)
      }
    }.elsewhen(io.memReq.fire && io.memReq.bits.opcode === TLAOp_PutPart &&
      io.memReq.bits.mask(i)){//TODO check order
      when(isSpace1) {
        memory1(wordAddr(i)) := writeVec(i)
      }.elsewhen(isSpace2){
        memory2(wordAddr(i)) := writeVec(i)
      }
    }
    when(isSpace1) {
      readVec(i) := memory1.read(wordAddr(i))
    }.elsewhen(isSpace2){
      readVec(i) := memory2.read(wordAddr(i))
    }.otherwise(
      readVec(i) := 0.U
    )
}

val RspOpCode = Wire(UInt(3.W))
  RspOpCode := Mux(io.memReq.bits.opcode===4.U(4.W),1.U(3.W),0.U(3.W))

  val memRsp_Q = Module(new Queue(new TLBundleD_lite(params), 4))
  memRsp_Q.io.enq.valid := (io.memReq.fire)
  memRsp_Q.io.enq.bits.opcode := (RspOpCode)
  memRsp_Q.io.enq.bits.data := readVec.asUInt
  memRsp_Q.io.enq.bits.source := (io.memReq.bits.source)
  memRsp_Q.io.enq.bits.size := 0.U
  io.memReq.ready := memRsp_Q.io.enq.ready

  io.memRsp <> memRsp_Q.io.deq
}
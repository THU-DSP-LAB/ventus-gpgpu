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
package L1Cache.DCache

import chisel3._
import chisel3.internal.naming.chiselName
import chisel3.util._
import config.config.Parameters

class WriteDataBufferIn(implicit p:Parameters) extends DCacheBundle{
  val mask = Vec(BlockWords,UInt(BytesOfWord.W))
  val addr   =  UInt(WordLength.W)//TODO can be cut to block Addr as well

  val data   =  Vec(BlockWords,Vec(BytesOfWord,UInt(8.W)))
  val instrId=  UInt(WIdBits.W)

  //val bankConflictLast = Bool()//high at the last cycle of a Req having bankConflict
  val bankConflict = Bool()
  val subWordMissReq = Bool()
  val subWordMissRsp = Bool()
}

class WriteDataBufferOut(implicit p:Parameters) extends DCacheBundle{
  val mask   =  Vec(BlockWords,UInt(1.W))
  val addr   =  UInt(WordLength.W)

  val data   =  Vec(BlockWords,Vec(BytesOfWord,UInt(8.W)))
  val instrId=  UInt(WIdBits.W)
}

class WdbEnqPtrGen (implicit p:Parameters) extends DCacheModule {
  /*cyclically find the next empty entry of valid list
  * */
  val io = IO(new Bundle{
    val entryValidList = Input(Vec(WdbDepth,Bool()))
    val enqPtr_cs = Input(UInt(log2Up(WdbDepth).W))

    val enqPtr_ns = Output(UInt(log2Up(WdbDepth).W))
  })
  val cyclicValidList = Wire(Vec(WdbDepth,Bool()))

  (0 until WdbDepth).foreach{ i =>
    val muxSel = Wire(UInt(log2Up(WdbDepth).W))// different val for each i
    muxSel := i.U + io.enqPtr_cs + 1.U//overflow conducts cyclic
    cyclicValidList(i) := Mux1H(UIntToOH(muxSel),io.entryValidList)
  }
  io.enqPtr_ns := PriorityEncoder(cyclicValidList.map{!_}) + 1.U + io.enqPtr_cs
}
object WdbEnqPtrGen{
  @chiselName
  def apply
  (entryValidList: Vec[Bool],
   enqPtr_cs: UInt
  )(implicit p:Parameters): UInt = {
    val Gen = Module(new WdbEnqPtrGen)
    Gen.io.entryValidList := entryValidList
    Gen.io.enqPtr_cs := enqPtr_cs
    Gen.io.enqPtr_ns
  }
}

class WdbDeqPtrGen (implicit p:Parameters) extends DCacheModule {
  /*cyclically find the next occupied unfrozen entry of valid list
  * overhead of this
  * */
  val io = IO(new Bundle{
    val entryValidList = Input(Vec(WdbDepth,Bool()))
    val entryFrozenList = Input(Vec(WdbDepth,Bool()))
    val deqPtr_cs = Input(UInt(log2Up(WdbDepth).W))

    val deqPtr_ns = Output(UInt(log2Up(WdbDepth).W))
  })
  val UnfrozenValidList = Reverse(Cat(io.entryValidList) ^ Cat(io.entryFrozenList))

  val cyclicValidList = Wire(Vec(WdbDepth,Bool()))
  (0 until WdbDepth).foreach{ i =>
    val muxSel = Wire(UInt(log2Up(WdbDepth).W))// different val for each i
    muxSel := i.U + io.deqPtr_cs + 1.U//overflow conducts cyclic

    cyclicValidList(i) := Mux1H(UIntToOH(muxSel),UnfrozenValidList)
  }
  io.deqPtr_ns := PriorityEncoder(cyclicValidList) + 1.U + io.deqPtr_cs
}
object WdbDeqPtrGen{
  @chiselName
  def apply
  (entryValidList: Vec[Bool],
   entryFrozenList: Vec[Bool],
   deqPtr_cs: UInt
  )(implicit p:Parameters): UInt = {
    val Gen = Module(new WdbDeqPtrGen)
    Gen.io.entryValidList := entryValidList
    Gen.io.entryFrozenList := entryFrozenList
    Gen.io.deqPtr_cs := deqPtr_cs
    Gen.io.deqPtr_ns
  }
}

class WDB (implicit p:Parameters) extends DCacheModule {
  val io = IO(new Bundle {
    val inputBus = Flipped(Decoupled(new WriteDataBufferIn))
    val outputBus = Decoupled(new WriteDataBufferOut)
    val wdbAlmostFull = Output(Bool())
  })

  val mask_ram = RegInit(VecInit(Seq.fill(WdbDepth)(VecInit(Seq.fill(BlockWords)(false.B)))))
  val addr_ram = RegInit(VecInit(Seq.fill(WdbDepth)(0.U(WordLength.W))))
  val data_ram = RegInit(VecInit(Seq.fill(WdbDepth)
  (VecInit(Seq.fill(BlockWords)
  (VecInit(Seq.fill(BytesOfWord)(0.U(8.W))))))))
  val instrId_ram = RegInit(VecInit(Seq.fill(WdbDepth)(0.U(WIdBits.W))))

  val enqPtr = RegInit(0.U(log2Up(WdbDepth).W))//Counter(entries)
  val deqPtr = RegInit(0.U(log2Up(WdbDepth).W))//Counter(entries)
  //val meltPtr = RegInit(0.U(log2Up(WdbDepth).W))
  val enqPtr_w = Wire(UInt(log2Up(WdbDepth).W))
  val deqPtr_w = Wire(UInt(log2Up(WdbDepth).W))
  val meltPtr_w = Wire(UInt(log2Up(WdbDepth).W))

  val entryValid = RegInit(VecInit(Seq.fill(WdbDepth)(false.B)))
  val entryFrozen = RegInit(VecInit(Seq.fill(WdbDepth)(false.B)))

  //val bankConflictLast = io.inputBus.bits.bankConflictLast
  val bankConflict = io.inputBus.bits.bankConflict
  val subWordMissReq = io.inputBus.bits.subWordMissReq
  val subWordMissRsp = io.inputBus.bits.subWordMissRsp
  val doEnq = io.inputBus.fire()
  val doDeq = io.outputBus.fire()

  val almFull = PopCount(Cat(entryValid)) === (WdbDepth-1).asUInt()
  //EDIT:when there's only one empty entry, ptr need to change in advance
  //val enqPtrOp  = Mux(almFull,enqPtr_w,enqPtr)//& doEnq & doDeqR
  // ***** enqPtr gen *****
  enqPtr_w := WdbEnqPtrGen(entryValid,enqPtr)
  when (doEnq && !bankConflict) {
    enqPtr := enqPtr_w
  }
  // ***** deqPtr(to L2 mem) gen *****
  deqPtr_w := WdbDeqPtrGen(entryValid,entryFrozen,deqPtr)
  when (doDeq){
    deqPtr := deqPtr_w
  }
  // ***** meltPtr(to L2 mem) gen *****
  val meltMatch = Wire(Vec(WdbDepth,Bool()))
  (0 until WdbDepth).foreach{ i =>
    meltMatch(i) := addr_ram(i) === io.inputBus.bits.addr
  }
  assert((PopCount(Cat(meltMatch))===1.U && subWordMissRsp) || !subWordMissRsp)
  meltPtr_w := OHToUInt(meltMatch)
  assert((entryFrozen(meltPtr_w)===true.B && subWordMissRsp) || !subWordMissRsp)

  // ***** valid & frozen management *****
  (0 until WdbDepth).foreach{ i =>
    when(i.asUInt===deqPtr_w && doDeq){
      //when enqPtr catch up deqPtr, full, no enqPtr
      entryValid(i) := false.B
    }.elsewhen(doEnq && !bankConflict) {
        when(i.asUInt === enqPtr_w) {
            entryValid(i) := true.B
          when(subWordMissReq) {
            entryFrozen(i) := true.B
          }
        }
      when(i.asUInt===meltPtr_w && subWordMissRsp){
        entryFrozen(i) := false.B
      }
    }
  }

  // ***** enqueue(ram fill) *****
  when (doEnq){
    (0 until BlockWords).foreach{ iofW =>
      when(subWordMissReq){
        mask_ram(enqPtr_w)(iofW) := Mux(io.inputBus.bits.mask(iofW).andR,true.B,false.B)
      }.elsewhen(subWordMissRsp){//no priority from subWordMissReq,
        mask_ram(meltPtr_w)(iofW) := Mux(io.inputBus.bits.mask(iofW).andR,true.B,false.B)
      }.otherwise{//general miss + hit
        mask_ram(enqPtr_w)(iofW) := Mux(io.inputBus.bits.mask(iofW).andR,true.B,false.B)
        assert(io.inputBus.bits.mask(iofW).orR === io.inputBus.bits.mask(iofW).andR)
      }
    }
  }
  val dataIn = io.inputBus.bits.data
  when (doEnq){
    (0 until BlockWords).foreach{ iofW =>
      (0 until BytesOfWord).foreach{ iinW =>
        when (io.inputBus.bits.mask(iofW)(iinW)){
          data_ram(enqPtr_w)(iofW)(iinW) := dataIn(iofW)(iinW)//data_ram(enqPtr)(iofW)(iinW) := Mux(io.inputBus.bits.mask(iofW)(iinW),dataIn(iofW)(iinW),data_ram(enqPtr)(iofW)(iinW))}}
        }
      }
    }
    instrId_ram(enqPtr_w) := io.inputBus.bits.instrId
    when (!subWordMissRsp){
      addr_ram(enqPtr_w) := io.inputBus.bits.addr
    }
  }

  // ***** dequeue *****
  io.outputBus.bits.mask := mask_ram(deqPtr_w)
  io.outputBus.bits.addr := addr_ram(deqPtr_w)
  io.outputBus.bits.data := data_ram(deqPtr_w)
  io.outputBus.bits.instrId := instrId_ram(deqPtr_w)

  // ***** input ready *****
  val full = PopCount(Cat(entryValid)) === WdbDepth.asUInt()//ptr_match && maybe_full
  io.inputBus.ready := !full

  // ***** output valid *****
  val unfrozenCount = PopCount(Cat(entryValid)^Cat(entryFrozen))
  val empty = unfrozenCount === 0.U//ptr_match && !maybe_full
  io.outputBus.valid := !empty
  io.wdbAlmostFull := unfrozenCount === (WdbDepth.asUInt()-2.U)
}



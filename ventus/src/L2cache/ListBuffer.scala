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

package L2cache

import chisel3._
import chisel3.util._
import freechips.rocketchip.util._

case class ListBufferParameters[T <: Data](gen: T, queues: Int, entries: Int, bypass: Boolean, singleport:Boolean)
{
  val data_width=gen.getWidth
  val queueBits:Int  = log2Up(queues)
  val entryBits:Int = log2Up(entries)
}

class ListBufferPush[T <: Data](params: ListBufferParameters[T]) extends Bundle//GenericParameterizedBundle(params)
{

  val index = Output(UInt(params.queueBits.W))
  val data  = Output(params.gen)
  //override def cloneType: ListBufferPush.this.type = new ListBufferPush(params).asInstanceOf[this.type]
}

class ListBuffer[T <: Data](params: ListBufferParameters[T]) extends Module
{
  val io = IO(new Bundle {
    // push is visible on the same cycle; flow queues
    val push  = Flipped(Decoupled(new ListBufferPush(params)))
    val valid = Output(UInt(params.queues.W))
    val pop   = Flipped(Valid(UInt(params.queueBits.W)))
    val data  = Output(params.gen)
    val pop2  =if(!params.singleport) Some(Flipped(Valid(UInt(params.queueBits.W))))else None
    val data2 =if(!params.singleport) Some(Output(params.gen))else None
  })
  val valid = RegInit(0.U(params.queues.W))
  val head  = Mem(params.queues, UInt(params.entryBits.W))
  val tail  = Mem(params.queues, UInt(params.entryBits.W))
  val used  = RegInit(0.U(params.entries.W))
  val next  = Mem(params.entries, UInt(params.entryBits.W))
  val data  = Mem(params.entries, UInt(params.data_width.W))//????????????entries

  val freeOH = (~(leftOR((~used).asUInt()) << 1)).asUInt() & (~used).asUInt()
  val freeIdx = OHToUInt(freeOH)

  val valid_set = WireInit(0.U(params.queues.W))
  val valid_clr = WireInit(0.U(params.queues.W))
  val used_set  = WireInit(0.U(params.entries.W))
  val used_clr  = WireInit(0.U(params.entries.W))


  val valid_clr_2 = WireInit(0.U(params.queues.W))
  val used_clr_2  = WireInit(0.U(params.entries.W))

  val push_tail = tail.read(io.push.bits.index)//??????tail???????????????
  val push_valid = valid(io.push.bits.index) //??????index???????????????push???

  io.push.ready := !used.andR() //???????????????entry
  when (io.push.fire()) {
    valid_set := UIntToOH(io.push.bits.index, params.queues) //????????????push?????????
    used_set := freeOH //
    data.write(freeIdx, io.push.bits.data.asUInt())
    when (push_valid) {
      next.write(push_tail, freeIdx)//?????????????????????next
    } .otherwise {
      head.write(io.push.bits.index, freeIdx)
    }
    tail.write(io.push.bits.index, freeIdx)  //???????????????
  }

  val pop_head = head.read(io.pop.bits)
  val pop_head2 =WireInit(0.U(params.entryBits.W))
  val pop_valid = valid(io.pop.bits)
  val pop_valid2 =WireInit(0.U(params.queues.W))
  if (!params.singleport) {
    pop_head2 := head.read(io.pop2.get.bits)
    pop_valid2 :=valid(io.pop2.get.bits)
    io.data2.get := (if (!params.bypass) data.read(pop_head2).asTypeOf(params.gen) else Mux(!pop_valid2, io.push.bits.data.asTypeOf(params.gen), data.read(pop_head2).asTypeOf(params.gen)))
  }

  // Bypass push data to the peek port
  io.data := (if (!params.bypass) data.read(pop_head).asTypeOf(params.gen) else Mux(!pop_valid, io.push.bits.data.asTypeOf(params.gen), data.read(pop_head).asTypeOf(params.gen)))

  io.valid := (if (!params.bypass) valid else (valid | valid_set))  //????????????bypass???????????????

  // It is an error to pop something that is not valid
  assert (!io.pop.fire() || (io.valid)(io.pop.bits))
  if(!params.singleport){
    assert(!io.pop2.get.fire()||(io.valid)(io.pop2.get.bits))
  }

  when (io.pop.fire()) {
    used_clr := UIntToOH(pop_head, params.entries)
    when (pop_head === tail.read(io.pop.bits)) {
      valid_clr := UIntToOH(io.pop.bits, params.queues)
    }
    head.write(io.pop.bits, Mux(io.push.fire() && push_valid && push_tail === pop_head, freeIdx, next.read(pop_head))) //??????????????????????????????push??????pop???????????????head??????freeidx??????
  }
  if(!params.singleport) {
    when(io.pop2.get.fire()) {
      used_clr_2 := UIntToOH(pop_head2, params.entries)
      when(pop_head2 === tail.read(io.pop2.get.bits)) {
        valid_clr_2 := UIntToOH(io.pop2.get.bits, params.queues)
      }
      head.write(io.pop2.get.bits, Mux(io.push.fire() && push_valid && push_tail === pop_head2, freeIdx, next.read(pop_head2)))
    } 
  }
  // Empty bypass changes no state
  when ((!params.bypass).asBool() || !io.pop.valid || pop_valid || pop_valid2.orR()) {
    used  := (used  & (~used_clr).asUInt() &(~used_clr_2).asUInt())  | used_set
    valid := (valid & (~valid_clr).asUInt()&(~valid_clr_2).asUInt())  | valid_set
  }
}

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
import freechips.rocketchip.tilelink._
import freechips.rocketchip.util._

class PutBufferAEntry(params: InclusiveCacheParameters_lite) extends Bundle
{
  val data = UInt(params.data_bits.W)  //这里面正是写回的data
  val mask= UInt(params.mask_bits.W)
  //override def cloneType: PutBufferAEntry.this.type = new PutBufferAEntry(params).asInstanceOf[this.type]
}

class PutBufferPop(params: InclusiveCacheParameters_lite) extends Bundle
{
  val index = UInt(params.putBits.W)
  //override def cloneType: PutBufferPop.this.type = new PutBufferPop(params).asInstanceOf[this.type]
}


class SinkA(params: InclusiveCacheParameters_lite) extends Module
{
  val io = IO(new Bundle {
    val req = Decoupled(new FullRequest(params))
    val a = Flipped(Decoupled(new TLBundleA_lite(params)))
    // for use by SourceD:
    //若顺利写回，pop掉sinka的buffer里面的数据
//    val index =Input(UInt(params.putBits.W))
    val pb_pop  = Flipped(Decoupled(new PutBufferPop(params)))
    val pb_beat =Output( new PutBufferAEntry(params))
//    val pb_pop2  =Flipped( Decoupled(new PutBufferPop(params)))
//    val pb_beat2 =Output( new PutBufferAEntry(params))
    val empty =Output(Bool())
  })
  // No restrictions on the type of buffer
  val a = params.micro.innerBuf.a(io.a)

  val putbuffer = Module(new ListBuffer(ListBufferParameters(new PutBufferAEntry(params), params.putLists, params.putBeats, false,true)))
  val lists = RegInit(0.U(params.putLists.W)) //和putbuffer里面的valid功能一样

  val lists_set = WireInit(0.U(params.putLists.W))
  val lists_clr = WireInit(0.U(params.putLists.W))
  lists := (lists | lists_set) & (~lists_clr).asUInt

  val free = !lists.andR
  val freeOH = Wire(UInt(params.putLists.W))
  freeOH :=(~(leftOR((~lists).asUInt) << 1)).asUInt & (~lists).asUInt
  val freeIdx = OHToUInt(freeOH)

  val hasData = params.hasData(a)

  // We need to split the A input to three places:
  //   If it is the first beat, it must go to req
  //   If it has Data, it must go to the putbuffer
  //   If it has Data AND is the first beat, it must claim a list
  val req_block = !io.req.ready
  val buf_block = hasData && !putbuffer.io.push.ready
  val set_block = hasData && !free


  a.ready := Mux(a.bits.opcode===5.U,!req_block && !buf_block && !set_block && io.empty,!req_block && !buf_block && !set_block)
  io.req.valid := Mux(a.bits.opcode===5.U,a.valid && !buf_block && !set_block&&io.empty,a.valid && !buf_block && !set_block)
  putbuffer.io.push.valid := a.valid && hasData && !req_block && !set_block
  when (a.valid && hasData && !req_block && !buf_block) { lists_set := freeOH }

  val (tag, l2cidx, set, offset) = params.parseAddress(a.bits.address)
  val put = freeIdx

  io.req.bits.opcode := a.bits.opcode
  io.req.bits.size := a.bits.size
  io.req.bits.source := a.bits.source
  io.req.bits.offset := offset
  io.req.bits.set    := set
  io.req.bits.tag    := tag
  io.req.bits.l2cidx := l2cidx
  io.req.bits.put    := put
  io.req.bits.mask   := a.bits.mask
  io.req.bits.data   :=a.bits.data
  io.req.bits.param :=a.bits.param

  putbuffer.io.push.bits.index := put
  putbuffer.io.push.bits.data.data := a.bits.data
  putbuffer.io.push.bits.data.mask := a.bits.mask
  // Grant access to pop the data
  putbuffer.io.pop.bits := io.pb_pop.bits.index
  putbuffer.io.pop.valid := io.pb_pop.fire
//  putbuffer.io.pop2.get.bits:=io.pb_pop2.bits.index
//  putbuffer.io.pop2.get.valid:=io.pb_pop2.fire
  io.pb_pop.ready := putbuffer.io.valid(io.pb_pop.bits.index)
//  io.pb_pop2.ready:= putbuffer.io.valid(io.pb_pop2.bits.index)
  io.pb_beat := putbuffer.io.data
//  io.pb_beat2:=putbuffer.io.data2.get
//  putbuffer.io.index.get := io.index
  io.empty := !((lists | lists_set) & (~lists_clr).asUInt)
  when (io.pb_pop.fire) {
        lists_clr := UIntToOH(io.pb_pop.bits.index, params.putLists)
  }
}

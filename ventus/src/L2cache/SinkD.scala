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
import TLMessages._
import freechips.rocketchip.util.leftOR
class SinkDResponse(params: InclusiveCacheParameters_lite) extends Bundle
{
  val opcode = UInt(params.op_bits.W)
  val source = UInt(params.source_bits.W)
  val data   =UInt(params.data_bits.W)
  //override def cloneType: SinkDResponse.this.type = new SinkDResponse(params).asInstanceOf[this.type]
}
class refill_data(params:InclusiveCacheParameters_lite) extends Bundle
{
  val data=UInt(params.data_bits.W)
  val set=UInt(params.setBits.W)
  val way=UInt(params.wayBits.W)
  val opcode =UInt(params.op_bits.W)
  val put =UInt(params.putBits.W)
}


class SinkD(params: InclusiveCacheParameters_lite) extends Module
{
  val io = IO(new Bundle {
    val resp = Valid(new SinkDResponse(params))
    val d =Flipped( Decoupled(new TLBundleD_lite(params)))
    // Lookup the set+way from MSHRs
    val source = Output(UInt(params.source_bits.W))
    val way    = Input(UInt(params.wayBits.W)) //用来放数据到L2的具体位置，先访问MSHR拿到way和set数据，后跟d的data一并送进bankstore
    val set    = Input(UInt(params.setBits.W))
    val opcode =Input(UInt(params.op_bits.W))
    val put =Input(UInt(params.putBits.W))
    val index =Output(UInt(params.putBits.W))
    val sche_dir_fire =Flipped(Valid(UInt(params.source_bits.W)))
    // Banked Store port
    val bs_adr = Decoupled(new BankedStoreOuterAddress(params))
    val bs_dat = Output(new BankedStoreOuterPoison(params))
    //merge with sinkA data
    val pb_pop = Decoupled(new PutBufferPop(params))
    val pb_beat = Input(new PutBufferAEntry(params))
  })

  // No restrictions on buffer
  val d = io.d
  io.index :=io.put //choose correct data to merge
  io.source := Mux(d.valid, d.bits.source, RegEnable(d.bits.source, d.valid))
  val full_mask=FillInterleaved(params.micro.writeBytes*8,io.pb_beat.mask)
  val merge_data=( io.pb_beat.data & full_mask) |(d.bits.data & (~full_mask).asUInt())
  val refill_buffer =Module(new ListBuffer(ListBufferParameters(new refill_data(params),params.mshrs,params.mshrs,false,true)))
  refill_buffer.io.push.bits.data.data:=Mux((io.opcode ===PutPartialData || io.opcode===PutFullData), merge_data,d.bits.data)
  refill_buffer.io.push.bits.data.set:=io.set
  refill_buffer.io.push.bits.data.way:=io.way
  refill_buffer.io.push.bits.data.opcode:=io.opcode
  refill_buffer.io.push.bits.data.put:=io.put
  refill_buffer.io.push.valid:=d.fire && (d.bits.opcode===AccessAckData)
  refill_buffer.io.push.bits.index:= io.source
  refill_buffer.io.pop.valid:=io.sche_dir_fire.valid
  refill_buffer.io.pop.bits:= io.sche_dir_fire.bits //具体pop哪个mshr


  io.resp.valid       := RegNext(d.fire())
  d.ready             := !(refill_buffer.io.valid.andR) //可以把数据存进来了

  io.resp.bits.opcode := RegNext(d.bits.opcode)
  io.resp.bits.source := RegNext(d.bits.source)
  io.resp.bits.data   := RegNext(Mux((io.opcode ===PutPartialData ||io.opcode===PutFullData), merge_data,d.bits.data))

  io.bs_adr.valid     :=  io.sche_dir_fire.valid && (refill_buffer.io.data.opcode===Get) //当成功写dir时候再写实际数据
  io.bs_adr.bits.way  := refill_buffer.io.data.way
  io.bs_adr.bits.set  := refill_buffer.io.data.set
  io.bs_dat.data      := refill_buffer.io.data.data
  io.bs_adr.bits.mask := ~(0.U(params.mask_bits.W))
  io.pb_pop.valid     :=  (refill_buffer.io.data.opcode===PutPartialData  || refill_buffer.io.data.opcode ===PutFullData) && io.sche_dir_fire.valid
  io.pb_pop.bits.index:=  refill_buffer.io.data.put
}

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
//    val bs_adr = Decoupled(new BankedStoreOuterAddress(params))
//    val bs_dat = Output(new BankedStoreOuterPoison(params))
    //merge with sinkA data
//    val pb_pop = Decoupled(new PutBufferPop(params))
//    val pb_beat = Input(new PutBufferAEntry(params))
  })

  // No restrictions on buffer
  val d = io.d
  io.index :=io.put //choose correct data to merge
  io.source := Mux(d.valid, d.bits.source, RegEnable(d.bits.source, d.valid))



  io.resp.valid       := RegNext(d.fire)
  d.ready             := true.B//!(refill_buffer.io.valid.andR) //可以把数据存进来了

  io.resp.bits.opcode := RegNext(d.bits.opcode)
  io.resp.bits.source := RegNext(d.bits.source)
  io.resp.bits.data   := RegNext(d.bits.data)

//  io.bs_adr.valid     :=  io.sche_dir_fire.valid && (refill_buffer.io.data.opcode===Get) //当成功写dir时候再写实际数据
//  io.bs_adr.bits.way  := refill_buffer.io.data.way
//  io.bs_adr.bits.set  := refill_buffer.io.data.set
//  io.bs_dat.data      := refill_buffer.io.data.data
//  io.bs_adr.bits.mask := ~(0.U(params.mask_bits.W))
//  io.pb_pop.valid     :=  (refill_buffer.io.data.opcode===PutPartialData  || refill_buffer.io.data.opcode ===PutFullData) && io.sche_dir_fire.valid
//  io.pb_pop.bits.index:=  refill_buffer.io.data.put
}

/*
 * Copyright 2019 SiFive, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You should have received a copy of LICENSE.Apache2 along with
 * this software. If not, you may obtain a copy at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package L2cache


import chisel3._
import chisel3.util._
import freechips.rocketchip.tilelink._
import TLMessages._
class SinkDResponse(params: InclusiveCacheParameters_lite) extends Bundle
{
  val opcode = UInt(params.op_bits.W)
  val source = UInt(params.source_bits.W)
  val data   =UInt(params.data_bits.W)
  //override def cloneType: SinkDResponse.this.type = new SinkDResponse(params).asInstanceOf[this.type]
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
    // Banked Store port
    val bs_adr = Decoupled(new BankedStoreOuterAddress(params))
    val bs_dat = Output(new BankedStoreOuterPoison(params))
    //merge with sinkA data
    val pb_pop = Decoupled(new PutBufferPop(params))
    val pb_beat = Input(new PutBufferAEntry(params))
  })

  // No restrictions on buffer
  val d = io.d

  io.source := Mux(d.valid, d.bits.source, RegEnable(d.bits.source, d.valid))
  val full_mask=FillInterleaved(params.micro.writeBytes*8,io.pb_beat.mask)
  val merge_data=( io.pb_beat.data & full_mask) |(d.bits.data & (~full_mask).asUInt())

  io.resp.valid       := d.fire()
  d.ready             := io.bs_adr.ready //可以把数据存进来了

  io.resp.bits.opcode := d.bits.opcode
  io.resp.bits.source := d.bits.source
  io.resp.bits.data   := Mux(io.opcode ===PutPartialData, merge_data,d.bits.data)

  io.bs_adr.valid     :=  d.valid
  io.bs_adr.bits.way  := io.way
  io.bs_adr.bits.set  := io.set
  io.bs_dat.data      := Mux(io.opcode ===PutPartialData, merge_data,d.bits.data)
  io.bs_adr.bits.mask := ~(0.U(params.mask_bits.W))
  io.pb_pop.valid     :=  (io.opcode===PutPartialData  || io.opcode ===PutFullData) && d.valid
  io.pb_pop.bits.index:=  io.put
}

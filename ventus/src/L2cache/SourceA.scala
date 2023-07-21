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
class TLBundleA_lite(params: InclusiveCacheParameters_lite) extends Bundle
{
  val opcode = UInt(params.op_bits.W)
  val size =UInt(params.size_bits.W)
  val source =UInt(params.source_bits.W)
  val address= UInt(params.addressBits.W)
  val mask =UInt((params.cache.beatBytes/params.micro.writeBytes).W)
  val data=UInt(params.data_bits.W)
 // val param = UInt(params.param_bits.W)

}




class SourceA(params: InclusiveCacheParameters_lite) extends Module
{
  val io = IO(new Bundle() {
    val req = Flipped(Decoupled(new FullRequest(params)))
    val a = Decoupled(new TLBundleA_lite(params))
  })



  io.req.ready := io.a.ready
  io.a.valid := io.req.valid
  io.a.bits.opcode  := io.req.bits.opcode

  io.a.bits.source  := io.req.bits.source
  io.a.bits.address := params.expandAddress(io.req.bits.tag, io.req.bits.l2cidx,io.req.bits.set, io.req.bits.offset)
  io.a.bits.mask    :=io.req.bits.mask
  io.a.bits.data    := io.req.bits.data
  io.a.bits.size    :=io.req.bits.size
}

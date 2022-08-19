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
class TLBundleA_lite(params: InclusiveCacheParameters_lite) extends Bundle
{
  val opcode = UInt(params.op_bits.W)
  val size =UInt(params.size_bits.W)
  val source =UInt(params.source_bits.W)
  val address= UInt(params.addressBits.W)
  val mask =UInt((params.cache.beatBytes/params.micro.writeBytes).W)
  val data=UInt(params.data_bits.W)
  //override def cloneType: TLBundleA_lite.this.type = new TLBundleA_lite(params).asInstanceOf[this.type]
}



//如果要去主存找数据，block=true，data输入，parameter都需要关注
class SourceA(params: InclusiveCacheParameters_lite) extends Module
{
  val io = IO(new Bundle() {
    val req = Flipped(Decoupled(new FullRequest(params)))
    val a = Decoupled(new TLBundleA_lite(params))
  })

  // ready must be a register, because we derive valid from ready
//  require (!params.micro.outerBuf.a.pipe && params.micro.outerBuf.a.isDefined)

//  val a = Wire(new TLBundleA_lite(params))
//  io.a <> a

  io.req.ready := io.a.ready
  io.a.valid := io.req.valid
  io.a.bits.opcode  := io.req.bits.opcode
//io.  a.bits.param   := io.req.bits.param
//io. a.bits.size    := UInt(params.offsetBits)
  io.a.bits.source  := io.req.bits.source
  io.a.bits.address := params.expandAddress(io.req.bits.tag, io.req.bits.set, io.req.bits.offset) //offset
  io.a.bits.mask    :=FillInterleaved(params.micro.writeBytes,io.req.bits.mask) //mask
  io.a.bits.data    := io.req.bits.data
  io.a.bits.size    :=io.req.bits.size
}

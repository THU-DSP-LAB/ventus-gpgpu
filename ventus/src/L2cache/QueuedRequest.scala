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

class QueuedRequest(params: InclusiveCacheParameters_lite) extends Bundle
{
  val opcode = UInt(params.op_bits.W)
  val size   = UInt(params.size_bits.W)
  val source = UInt(params.source_bits.W)
  val tag    = UInt(params.tagBits.W)
  val offset = UInt(params.offsetBits.W)
  val put    = UInt(params.putBits.W)
  val data   = UInt(params.data_bits.W)
  val mask   = UInt(params.mask_bits.W)
}

class ListBufferRequest(params: InclusiveCacheParameters_lite)extends Bundle
{
  val source = UInt(3.W)
}
class DirWriteRequest(params: InclusiveCacheParameters_lite)extends Bundle
{
  val set = UInt(params.setBits.W)
}

class FullRequest(params: InclusiveCacheParameters_lite) extends QueuedRequest(params)
{
  val set = UInt(params.setBits.W)
  //override def cloneType: FullRequest.this.type = new FullRequest(params).asInstanceOf[this.type]
}



class AllocateRequest(params: InclusiveCacheParameters_lite) extends Bundle
{
  val way     =UInt(params.wayBits.W)
  val set    = UInt(params.setBits.W)
  val tag    = UInt(params.tagBits.W)
  //override def cloneType: AllocateRequest.this.type = new AllocateRequest(params).asInstanceOf[this.type]
}
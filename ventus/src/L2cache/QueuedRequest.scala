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
  val l2cidx = UInt(params.l2cBits.W)
  //override def cloneType: FullRequest.this.type = new FullRequest(params).asInstanceOf[this.type]
}



class AllocateRequest(params: InclusiveCacheParameters_lite) extends Bundle
{
  val way     =UInt(params.wayBits.W)
  val set    = UInt(params.setBits.W)
  val tag    = UInt(params.tagBits.W)
  //override def cloneType: AllocateRequest.this.type = new AllocateRequest(params).asInstanceOf[this.type]
}
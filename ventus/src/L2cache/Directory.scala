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

import Chisel._
import freechips.rocketchip.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.util.ReplacementPolicy
import TLMessages._
import MetaData._

class DirectoryEntry(params: InclusiveCacheParameters_lite) extends Bundle
{
//  val state   = UInt(width = params.stateBits)
//  val clients = UInt(width = params.clientBits)
  val tag     = UInt(width = params.tagBits.W)
  val dirty   = Bool()
}

class DirectoryWrite(params: InclusiveCacheParameters_lite) extends DirWriteRequest(params) //
{
  val way  = UInt(width = params.wayBits.W)
  val data = new DirectoryEntry(params)
}

class DirectoryRead(params: InclusiveCacheParameters_lite) extends FullRequest(params)

class DirectoryResult(params: InclusiveCacheParameters_lite) extends FullRequest(params)
{
  val hit = Bool()
  val way = UInt(width = params.wayBits.W)
  val dirty   = Bool()
}

class Directory(params: InclusiveCacheParameters_lite) extends Module
{
  val io = IO(new Bundle {
    val write  = Decoupled(new DirectoryWrite(params)).flip
    val read   = Valid(new DirectoryRead(params)).flip // sees same-cycle write
    val result = Decoupled(new DirectoryResult(params))
    val ready  = Output(Bool() ) // reset complete; can enable access
  })

  // dump



  val codeBits = new DirectoryEntry(params).getWidth

  val singlePort = false
  val cc_dir = Module(new SRAMTemplate(UInt(width = codeBits), set=params.cache.sets, way=params.cache.ways,
    shouldReset=false, holdRead=false, singlePort=singlePort,bypassWrite = true))

  // must inspect contents => max size 1
  // a flow Q creates a WaR hazard... this MIGHT not cause a problem
  // a pipe Q causes combinational loop through the scheduler

  // Wiping the Directory with 0s on reset has ultimate priority
  val wipeCount = RegInit(UInt(0, width = params.setBits + 1))
  val wipeOff = RegNext(Bool(false), Bool(true)) // don't wipe tags during reset
  val wipeDone = wipeCount(params.setBits)
  val wipeSet = wipeCount(params.setBits - 1,0)
  io.ready:=wipeDone


  when (!wipeDone && !wipeOff) { wipeCount := wipeCount + UInt(1) }
  assert (wipeDone || !io.read.valid)

  val ren = io.read.valid
  val wen = (!wipeDone && !wipeOff) || io.write.valid
  assert (!io.read.valid || wipeDone)

  require (codeBits <= 256)







  val ren1 = RegInit(Bool(false))
  ren1 := ren
  val wen1=RegInit(Bool(false))
  wen1:=wen


  cc_dir.io.r.req.valid := ren
  cc_dir.io.r.req.bits.apply(setIdx=io.read.bits.set)  //读了一个set的所有数据
  val regout = cc_dir.io.r.resp.data //

  val tag = RegEnable(io.read.bits.tag, ren)
  val set = RegEnable(io.read.bits.set, ren)
  val writethrough =RegEnable(io.read.bits.opcode===PutFullData,ren)
  // Compute the victim way in case of an evicition
  val replacer_array = Array.fill(params.cache.sets){
      ReplacementPolicy.fromString(params.cache.replacement, params.cache.ways)
  }
  val victimWay = Vec(replacer_array.map(_.way))(set)

  val setQuash = wen1 && io.write.bits.set === set //表示write到上次读出来的set
  val tagMatch = io.write.bits.data.tag === tag //这是之前打算read的tag
  val writeWay1 = RegNext(io.write.bits.way)
  val ways = Vec(regout.map(d => new DirectoryEntry(params).fromBits(d)))
  // 这边作为LLC，没有块儿权限之说，这里hit，不用检查权限
  val hits = Cat(ways.map { case (w) =>
    w.tag === tag  && (!setQuash) //这个相当于read到了read出来的tag，但是不是bypass情况

  }.reverse)
  val dirtys =Cat(ways.map { case (w) =>
    w.dirty=== Bool(true) //这个相当于read到了read出来的tag，但是不是bypass情况

  }.reverse)
  val dirty= dirtys(victimWay) //todo




  val hit = hits.orR()
  val hitWay = Valid(OHToUInt(hits))
  val writeSet1 = RegNext(io.write.bits.set)
  val writeWay2 =Valid(writeWay1)
  writeWay2.valid:=Bool(true)
  hitWay.valid:=Bool(true)
  for((repl, i) <- replacer_array.zipWithIndex){
    when(wen1&&i.U===writeSet1){
      when(ren1&&i.U===set&&hit){
        repl.access(Seq(writeWay2,hitWay))
      }.otherwise{
        repl.access(writeWay1)
      }

    }
  }

  cc_dir.io.w.req.valid :=  wen
  cc_dir.io.w.req.bits.apply(
    setIdx=Mux(wipeDone, io.write.bits.set, wipeSet),
    data=Mux(wipeDone, io.write.bits.data.asUInt, UInt(0)),
    waymask=UIntToOH(io.write.bits.way, params.cache.ways) | Fill(params.cache.ways, !wipeDone))//就是写对应的way，如果reset全写


  io.result.valid := ren1
  io.result.bits.hit  := hit ||(setQuash && tagMatch)
  io.result.bits.way  := Mux(hit, OHToUInt(hits), Mux(setQuash && tagMatch,io.write.bits.way,victimWay))
  io.result.bits.dirty:= !writethrough&& !(hit||(setQuash && tagMatch))&& dirty



}

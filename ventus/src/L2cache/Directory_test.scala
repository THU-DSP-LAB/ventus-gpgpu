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

import chisel3._
import chisel3.util._
import freechips.rocketchip.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.util.ReplacementPolicy
import TLMessages._
import MetaData._

class DirectoryEntry_lite (params: InclusiveCacheParameters_lite)extends Bundle
{
  val tag     = UInt(width = params.tagBits.W)
//  val valid   = Bool()
//  val dirty  =Bool()
  //override def cloneType: DirectoryEntry_lite.this.type = new DirectoryEntry_lite(params).asInstanceOf[this.type]
}
class Directory_status(params: InclusiveCacheParameters_lite) extends Bundle{
  val valid = Vec(params.cache.ways,Bool())
  val dirty = Vec(params.cache.ways,Bool())
}
class DirectoryWrite_lite(params: InclusiveCacheParameters_lite) extends Bundle //
{
  val way  = UInt(width = params.wayBits.W)
  val data = new DirectoryEntry_lite(params)
  val set =UInt(width=params.setBits.W)
  val is_writemiss= Bool()
  //override def cloneType: DirectoryWrite_lite.this.type = new DirectoryWrite_lite(params).asInstanceOf[this.type]
}

class DirectoryRead_lite(params: InclusiveCacheParameters_lite) extends FullRequest(params)
{
  //override def cloneType: DirectoryRead_lite.this.type = new DirectoryRead_lite(params).asInstanceOf[this.type]
}

class DirectoryResult_lite(params: InclusiveCacheParameters_lite) extends DirectoryRead_lite(params)
{

  val hit = Bool()
  val way = UInt(width=params.wayBits.W)
  val dirty =Bool()
  val flush =Bool()
  val last_flush =Bool()

  //override def cloneType: DirectoryResult_lite.this.type = new DirectoryResult_lite(params).asInstanceOf[this.type]
}

class DirectoryResult_lite_victim(params: InclusiveCacheParameters_lite) extends DirectoryResult_lite(params)
{
  val victim_tag =UInt(params.tagBits.W)
}

class Directory_test(params: InclusiveCacheParameters_lite) extends Module
{
  val io = IO(new Bundle {
    val write  =Flipped(Decoupled(new DirectoryWrite_lite(params)))
    val read   = Flipped(Decoupled(new FullRequest(params))) // sees same-cycle write
    val result = Decoupled(new DirectoryResult_lite_victim(params))
    val ready  = Output(Bool() ) // reset complete; can enable access
    val flush  = Flipped(Valid(Bool()))
    val invalidate =Flipped(Valid(Bool()))
 //   val finish_issue =Output(Bool())
  })

  // dump



  val codeBits = new DirectoryEntry_lite(params).getWidth

  val singlePort = false
  val cc_dir = Module(new SRAMTemplate(UInt(codeBits.W), set=params.cache.sets, way=params.cache.ways,
    shouldReset=true, holdRead=false, singlePort=singlePort,bypassWrite = true))

  // must inspect contents => max size 1
  // a flow Q creates a WaR hazard... this MIGHT not cause a problem
  // a pipe Q causes combinational loop through the scheduler

  // Wiping the Directory with 0s on reset has ultimate priority
  val wipeCount = RegInit(0.U((params.setBits+1).W))
  val wipeOff   = RegNext(false.B,true.B) // don't wipe tags during reset
  val wipeDone  = wipeCount(params.setBits)
  val wipeSet   = wipeCount(params.setBits-1,0)






  when (!wipeDone && !wipeOff) {
    wipeCount := wipeCount + 1.U
  }



  val flush_issue_reg =RegInit(false.B)
  val flush_issue =Mux(io.flush.fire || io.invalidate.fire, true.B,flush_issue_reg)
  val is_invalidate_reg =RegInit(false.B)
  val is_invalidate =Mux(io.invalidate.fire, io.invalidate.bits,is_invalidate_reg)



  val flushCount =RegInit(0.U((params.setBits+params.wayBits+1).W))
  val flushDone = flushCount===((params.cache.sets*params.cache.ways).asUInt-1.U)

  when((io.flush.fire&& io.flush.bits) || (io.invalidate.bits && io.invalidate.fire) || (flush_issue)){
    flushCount := flushCount +1.U
  }.elsewhen(flushDone){
    flushCount :=0.U
  }
  //todo not sure

  when(io.flush.fire || io.invalidate.fire){
    flush_issue_reg:= true.B
    is_invalidate_reg:= io.invalidate.bits && io.invalidate.fire
  }.elsewhen(flushDone){
    flush_issue_reg:= false.B
    is_invalidate_reg :=false.B
  }






  val ren = io.read.fire() || flush_issue
  val wen_new = (!wipeDone && !wipeOff) || io.write.fire()
  val wen =io.write.fire()
  require (codeBits <= 256)




val status_reg =Reg(Vec(params.cache.sets,new Directory_status(params)))
  for(i <-0 until params.cache.sets) {
    for (j <-0 until params.cache.ways) {
      when(!wipeDone) {
        status_reg(i).valid(j) := false.B
        status_reg(i).dirty(j) := false.B
      }.elsewhen(flush_issue && ((i*params.cache.ways+j).asUInt === flushCount)){
        when(is_invalidate){
          status_reg(i).valid(j) :=false.B
        }
        status_reg(i).dirty(j) := false.B

      }.elsewhen(io.result.valid && io.result.bits.hit && (io.result.bits.opcode === PutPartialData || io.result.bits.opcode === PutFullData) && io.result.bits.way===j.asUInt && io.result.bits.set===i.asUInt) {
        status_reg(i).dirty(j) := true.B
      }.elsewhen(io.result.valid && !io.result.bits.hit && io.result.bits.way===j.asUInt && io.result.bits.set===i.asUInt) {
        status_reg(i).valid(j) := false.B
        status_reg(i).dirty(j) := false.B
      }.elsewhen(io.write.valid && io.write.bits.set===i.asUInt && io.write.bits.way===j.asUInt) {
        status_reg(i).valid(j) := true.B//(status_reg(i).valid.asUInt | (1.U << io.write.bits.way).asUInt).asBools
        status_reg(i).dirty(j) := io.write.bits.is_writemiss //(status_reg(i).dirty.asUInt | (0.U << io.write.bits.way).asUInt).asBools
      }
    }
  }
//todo victim way valid bits should be invalidated


  val ren1 = RegInit(false.B)
  ren1 := ren
  val wen1=RegInit(false.B)
  wen1:=wen





  val regout = cc_dir.io.r.resp.data //

  val tag = RegEnable(io.read.bits.tag, ren)
  val set = RegEnable(io.read.bits.set, ren)
 // val writethrough =RegEnable(io.read.bits.opcode===PutFullData,ren)
  // Compute the victim way in case of an evicition
  val replacer_array = RegInit(VecInit(Seq.fill(params.cache.sets)(0.U(log2Ceil(params.cache.ways).W))))//ReplacementPolicy.fromString("plru", n_ways = params.cache.ways)
for(i<- 0 until params.cache.sets){
  replacer_array(i):=Mux(ren1&& i.asUInt===set ,1.U+replacer_array(i),replacer_array(i))
}
  val victimWay = replacer_array(set)

  val setQuash_1 = wen && io.write.bits.set === io.read.bits.set //表示write到上次读出来的set
  val setQuash=wen1 && io.write.bits.set === set
  val tagMatch_1= io.write.bits.data.tag===io.read.bits.tag
  val tagMatch = io.write.bits.data.tag === tag //这是之前打算read的tag
  val writeWay1 = RegInit(0.U(params.wayBits.W))
  writeWay1:=io.write.bits.way

  val ways = regout.asTypeOf(Vec(params.cache.ways,new DirectoryEntry_lite(params)))
  val status = status_reg(set)
  // 这边作为LLC，没有块儿权限之说，这里hit，不用检查权限
  val hits = Cat(ways.zip(status.valid).map { case (w,s) =>
    w.tag === tag  && (!setQuash) && s//这个相当于read到了read出来的tag，但是不是bypass情况

  }.reverse)


  val flush_set =flushCount/params.cache.ways.asUInt
  val flush_way =(flushCount%params.cache.ways.asUInt)
  val flush_tag =ways(flush_way).tag
  cc_dir.io.r.req.valid := ren && (!(setQuash_1&&tagMatch_1)) //在非bypass情况下fire才会读
  cc_dir.io.r.req.bits.apply(setIdx=Mux(flush_issue,flush_set,io.read.bits.set))  //读了一个set的所有数据



  val hit = hits.orR()
  val hitWay = Wire(UInt(params.cache.ways.W))
  hitWay:= OHToUInt(hits)
  val writeSet1 = RegNext(io.write.bits.set)

//  for((repl, i) <- replacer_array.zipWithIndex){
////    when(wen1&&i.U===writeSet1){
////      repl.access(writeWay1)
////    }.else
//    when(ren1&& i.U===set){//&&hit){
//      repl.access(hitWay)
//    }
//  }

  cc_dir.io.w.req.valid :=  wen_new
  cc_dir.io.w.req.bits.apply(
    setIdx=Mux(wipeDone, io.write.bits.set, wipeSet),
    data=Mux(wipeDone, io.write.bits.data.asUInt, 0.U),
    waymask=UIntToOH(io.write.bits.way, params.cache.ways) | Fill(params.cache.ways, !wipeDone))//就是写对应的way，如果reset全写


  io.ready:=wipeDone && !flush_issue
  io.write.ready:=wipeDone && !flush_issue

  io.read.ready:= ((wipeDone&& !io.write.fire()) ||(setQuash_1&&tagMatch_1) )&& !flush_issue     //also fire when bypass
  io.result.valid := Mux(flush_issue,RegNext(status_reg(flush_set).dirty(flush_way) && flush_issue),ren1)
  io.result.bits.hit  := Mux(flush_issue,false.B, hit ||(setQuash && tagMatch))
  io.result.bits.way  := Mux(flush_issue, RegNext(flush_way),Mux(hit, OHToUInt(hits), Mux(setQuash && tagMatch,io.write.bits.way,victimWay)))
  io.result.bits.put    :=Mux(flush_issue,0.U,RegNext(io.read.bits.put))
  io.result.bits.data   :=Mux(flush_issue,0.U,RegNext(io.read.bits.data))
  io.result.bits.offset :=Mux(flush_issue,0.U,RegNext(io.read.bits.offset))
  io.result.bits.size   :=Mux(flush_issue,log2Up(params.cache.beatBytes).asUInt,RegNext(io.read.bits.size))
  io.result.bits.set    :=Mux(flush_issue,RegNext(flush_set),RegNext(io.read.bits.set))
  io.result.bits.source :=Mux(flush_issue,0.U,RegNext(io.read.bits.source))
  io.result.bits.tag    :=Mux(flush_issue,RegNext(flush_tag),RegNext(io.read.bits.tag))
  //victim tag should be transfered when miss dirty
  io.result.bits.opcode :=Mux(flush_issue,PutFullData,RegNext(io.read.bits.opcode))
  io.result.bits.mask   :=Mux(flush_issue,Fill(params.mask_bits,1.U),RegNext(io.read.bits.mask))
  io.result.bits.dirty  :=Mux(flush_issue,RegNext(status_reg(flush_set).dirty(flush_way)), (status_reg(set).dirty(io.result.bits.way)).asBool)
  io.result.bits.last_flush :=Mux(flush_issue,RegNext(flushDone),false.B)
  io.result.bits.flush  := RegNext(flush_issue)
  io.result.bits.victim_tag:= ways(io.result.bits.way).tag
  //todo what's the function of flush
  io.result.bits.l2cidx := Mux(flush_issue,0.U,RegNext(io.read.bits.l2cidx))
}

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
import org.chipsalliance.cde.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.util.ReplacementPolicy
import TLMessages._
import MetaData._
import freechips.rocketchip.regmapper.LFSR16Seed
import top.parameters.SPIKE_OUTPUT

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
//  val is_writemiss= Bool()
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
    val flush  = Input(Bool())
    val invalidate =Input(Bool())
    val tag_match = Input(Bool())
    val flush_invalidate_src=Input(UInt(params.source_bits.W))
 //   val finish_issue =Output(Bool())
  })

  // dump



  val codeBits = new DirectoryEntry_lite(params).getWidth

  val singlePort = false
  val cc_dir = Module(new SRAMTemplate(UInt(codeBits.W), set=params.cache.sets, way=params.cache.ways,
    shouldReset=true, holdRead=true, singlePort=singlePort,bypassWrite = true))

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
  val flush_issue =Mux(io.flush || io.invalidate, true.B,flush_issue_reg)
  val is_invalidate_reg =RegInit(false.B)
  val is_invalidate =Mux(io.invalidate, io.invalidate,is_invalidate_reg)

 val flush_source_reg=RegInit(0.U(params.source_bits.W))

  val flushCount =RegInit(0.U((params.setBits+params.wayBits+1).W))
  val flushDone = flushCount===((params.cache.sets*params.cache.ways).asUInt-1.U)

  val status_reg =Reg(Vec(params.cache.sets,new Directory_status(params)))
  val flush_set =flushCount/params.cache.ways.asUInt
  val flush_way =(flushCount%params.cache.ways.asUInt)
  val regout = cc_dir.io.r.resp.data //
  val ways = regout.asTypeOf(Vec(params.cache.ways,new DirectoryEntry_lite(params)))
  val flush_tag =ways(flush_way).tag
  when(io.flush || io.invalidate  || (flush_issue_reg&& (io.result.fire || !RegNext(status_reg(flush_set).dirty(flush_way), false.B)))){
    flushCount := flushCount +1.U
  }.elsewhen(flushDone){
    flushCount :=0.U
  }
  //todo not sure

  when(io.flush || io.invalidate){
    flush_issue_reg:= true.B
    flush_source_reg:=io.flush_invalidate_src
    is_invalidate_reg:=  io.invalidate
  }.elsewhen(flushDone){
    flush_issue_reg:= false.B
    is_invalidate_reg :=false.B
    flush_source_reg:=0.U
  }






  val ren = io.read.fire || flush_issue

  val wen_new = (!wipeDone && !wipeOff) || io.write.fire
  val wen =io.write.fire
  require (codeBits <= 256)

  val not_replace= ((io.result.bits.opcode===PutFullData ||io.result.bits.opcode===PutPartialData) && !io.result.bits.hit) ||io.tag_match
  //not replace victim when write miss or when multi mergeable miss


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

      // bfs4096-002 iter5 fix #2: status_reg 推进改用 io.result.fire（不再看 .valid）。
      // ready=0 反压时若按 .valid 推进，会让 status_reg.dirty 在 N 拍清零 → N+1 拍
      // result.bits.dirty (组合读 status_reg) 跟塌 → enq.valid 自塌 → evict 漏 enq。
      // 改 fire 让推进只在握手成功那拍发生。hit+write 路径同改保对称。
      // 配套：Scheduler.scala:255 (#1) + Scheduler.scala:217 (#3)。详见 checkpoint_3_iter5.md。
      }.elsewhen(io.result.fire && io.result.bits.hit && (io.result.bits.opcode === PutPartialData || io.result.bits.opcode === PutFullData) && io.result.bits.way===j.asUInt && io.result.bits.set===i.asUInt) {
        status_reg(i).dirty(j) := true.B
      }.elsewhen(io.result.fire && !io.result.bits.hit && io.result.bits.way===j.asUInt && io.result.bits.set===i.asUInt && !not_replace) {
        status_reg(i).valid(j) := false.B
        status_reg(i).dirty(j) := false.B
      }.elsewhen(io.write.valid && io.write.bits.set===i.asUInt && io.write.bits.way===j.asUInt) {
        status_reg(i).valid(j) := true.B//(status_reg(i).valid.asUInt | (1.U << io.write.bits.way).asUInt).asBools
        status_reg(i).dirty(j) := false.B //io.write.bits.is_writemiss //(status_reg(i).dirty.asUInt | (0.U << io.write.bits.way).asUInt).asBools
      }
    }
  }
//todo victim way valid bits should be invalidated


  val ren1 = RegInit(false.B)
  ren1 := ren
  val wen1=RegInit(false.B)
  wen1:=wen






  val tag = RegEnable(io.read.bits.tag, ren)
  val set = RegEnable(io.read.bits.set, ren)
 // val writethrough =RegEnable(io.read.bits.opcode===PutFullData,ren)
  // Compute the victim way in case of an evicition
  val replacer_array = RegInit(VecInit(Seq.fill(params.cache.sets)(0.U(log2Ceil(params.cache.ways).W))))//ReplacementPolicy.fromString("plru", n_ways = params.cache.ways)
for(i<- 0 until params.cache.sets){
  replacer_array(i):=Mux(ren1&& i.asUInt===set ,1.U+replacer_array(i),replacer_array(i))
}
  val lfsr=RegInit(0.U(16.W)) //todo need to be configurable
  val xor = lfsr(0) ^ lfsr(1) ^ lfsr(3) ^ lfsr(4)
  when (io.result.fire) {
    lfsr := Mux(lfsr === 0.U, 1.U, Cat(lfsr(16-2,0),xor))
  }
  val victim_LFSR = lfsr


  val victimWay = victim_LFSR(params.wayBits-1,0)//replacer_array(set)

  val setQuash_1 = wen && io.write.bits.set === io.read.bits.set //表示write到上次读出来的set

  val setQuash=RegNext(setQuash_1, false.B)
  val tagMatch_1= io.write.bits.data.tag===io.read.bits.tag
  val tagMatch = RegNext(tagMatch_1, false.B) //这是之前打算read的tag
  val writeWay1 = RegInit(0.U(params.wayBits.W))
  writeWay1:=io.write.bits.way

  val status = status_reg(set)
  // 这边作为LLC，没有块儿权限之说，这里hit，不用检查权限
  val hits = Cat(ways.zip(status.valid).map { case (w,s) =>
    w.tag === tag   && s//这个相当于read到了read出来的tag，但是不是bypass情况

  }.reverse)


  cc_dir.io.r.req.valid := ren && (!(setQuash_1&&tagMatch_1)) //在非bypass情况下fire才会读
  cc_dir.io.r.req.bits.apply(setIdx=Mux(flush_issue,flush_set,io.read.bits.set))  //读了一个set的所有数据



  val hit = hits.orR
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


  io.ready:=wipeDone && !flush_issue_reg
  io.write.ready:=wipeDone && !flush_issue_reg
  val valid_reg =RegInit(false.B)
  when(ren1 && !io.result.ready){
    valid_reg:=true.B
  }.elsewhen(io.result.fire){
    valid_reg:=false.B
  }
  val valid_signal =Mux(ren1,ren1,valid_reg)
  val read_bits_reg =RegInit(0.U.asTypeOf(new FullRequest(params)))
  when(io.read.fire){
    read_bits_reg:= io.read.bits
  }
  val about_replace = (io.write.bits.set === io.result.bits.set) && (io.write.bits.way === io.result.bits.way) && io.write.fire

  val timely_hit = (RegNext(io.read.bits.tag) ===io.write.bits.data.tag) && io.write.fire && (RegNext(io.read.bits.set)===io.write.bits.set)

  val flush_issue_regnext = RegNext(flush_issue, false.B)
  io.read.ready := ((wipeDone && !io.write.fire) || (setQuash_1 && tagMatch_1)) && !flush_issue_reg  && io.result.ready//also fire when bypass
  io.result.valid := Mux(flush_issue_regnext, io.result.bits.last_flush|| RegNext(status_reg(flush_set).dirty(flush_way) && flush_issue, false.B), valid_signal)

  // bfs4096-004 fix: 把 io.result.bits 的 {hit, way, dirty, victim_tag} 4 个字段从纯组合
  // 改为 RegEnable(comb, ren1) 锁存版本，避开 Decoupled 反压窗口里 directory.write
  // 改写 ways / hits / status_reg 导致 result.bits 中途突变的 race。
  //
  // 现场（anchor v3 fst @bfs_4096，N=16）：
  //   t=2_945_435  visited lookup 命中 way=1，result.bits.way=1，result.ready=0（dir_result_buffer 反压）
  //   t=2_945_505  另一 MSHR_5 的 dir write fire (set=28, way=15, tag=0x90030)；与上面同 set
  //   t=2_945_514  组合表达式 OHToUInt(hits) 因 ways(28)/hits 被刷新而变成 15，
  //                io.result.bits.way 在反压期间从 1 突变到 15
  //   t=2_945_605  dir_result_buffer 终于 enq fire，way=15 入队（不再是 lookup 时的 1）
  //   t=2_945_635  sourceD HIT 路径用 way=15 读 BankedStore (set=28, way=15)，
  //                拿到 12 拍前 MSHR_5 刚 fill 的 grEdges 数据，串污染回 L1 dcache
  //
  // 修法采用 Mux(ren1, comb, reg) 风格而不是直接 reg：RegEnable 在 ren1=1 那拍 reg 还是
  // 上次锁存值（要等下一个 clock edge 才更新），但 valid_signal 在 ren1=1 当周期就是 1
  // (line 284)。若同拍 ready=1（零反压 fire），下游会拿到 stale reg。Mux on ren1 让
  // 当拍直接走 comb 表达式（与 valid 同步），反压期间走 reg（稳定）。
  //
  // 范围最小化：flush 路径（flush_issue_regnext=1 分支）完全保持原样——flush 时
  // io.write.ready=0（line 277），dir.write 不可能 fire，没有反压期被改写的 race。
  // about_replace（line 289）也不动——它仍每拍重算，但只在 ren1 那拍被 sample 到
  // result_hit_reg，反压期出现的新 about_replace=1 不再传到下游，正向修复。
  val result_hit_comb        = (hit || (setQuash && tagMatch) || timely_hit) && (!about_replace)
  val normal_way_comb        = Mux(setQuash && tagMatch, RegNext(io.write.bits.way),
                                   Mux(timely_hit, io.write.bits.way,
                                       Mux(hit, OHToUInt(hits), victimWay)))
  val normal_dirty_comb      = Mux(not_replace, false.B, (status_reg(set).dirty(normal_way_comb)).asBool)
  val normal_victim_tag_comb = ways(normal_way_comb).tag

  val result_hit_reg         = RegEnable(result_hit_comb,        ren1)
  val normal_way_reg         = RegEnable(normal_way_comb,        ren1)
  val normal_dirty_reg       = RegEnable(normal_dirty_comb,      ren1)
  val normal_victim_tag_reg  = RegEnable(normal_victim_tag_comb, ren1)

  io.result.bits.hit := Mux(ren1, result_hit_comb, result_hit_reg)
  io.result.bits.way  := Mux(flush_issue_regnext, RegNext(flush_way, false.B),
                             Mux(ren1, normal_way_comb, normal_way_reg))
  io.result.bits.put    :=Mux(flush_issue_regnext, 0.U ,read_bits_reg.put)
  io.result.bits.data   :=Mux(flush_issue_regnext, 0.U ,read_bits_reg.data)
  io.result.bits.offset :=Mux(flush_issue_regnext, 0.U ,read_bits_reg.offset)
  io.result.bits.size   :=Mux(flush_issue_regnext, log2Up(params.cache.beatBytes).asUInt, read_bits_reg.size)
  io.result.bits.set    :=Mux(flush_issue_regnext, RegNext(flush_set, false.B), read_bits_reg.set)
  io.result.bits.source :=Mux(flush_issue_regnext, RegNext(flush_source_reg, false.B), read_bits_reg.source)
  io.result.bits.tag    :=Mux(flush_issue_regnext, ways(RegNext(flush_way, false.B)).tag, read_bits_reg.tag)
  //victim tag should be transfered when miss dirty
  io.result.bits.opcode :=Mux(flush_issue_regnext, Hint, read_bits_reg.opcode)

  io.result.bits.mask   :=Mux(flush_issue_regnext, Fill(params.mask_bits,1.U),read_bits_reg.mask)
  io.result.bits.dirty  :=Mux(flush_issue_regnext, RegNext(status_reg(flush_set).dirty(flush_way), false.B),
                              Mux(ren1, normal_dirty_comb, normal_dirty_reg))
  io.result.bits.last_flush :=Mux(flush_issue_regnext, RegNext(flushDone, false.B),false.B)
  io.result.bits.flush  := RegNext(flush_issue, false.B)
  // bfs4096-004 fix: victim_tag 原本隐式依赖 io.result.bits.way 已经按 flush_issue_regnext 分支选过，
  // 现在 way 已改为锁存版本，必须在这里显式 mux 出 flush 分支（用 ways(RegNext(flush_way)).tag），
  // 否则反压期间 ways(...) 仍是组合读，会被 dir.write 改 ways 后污染 victim_tag。
  io.result.bits.victim_tag := Mux(flush_issue_regnext, ways(RegNext(flush_way, false.B)).tag,
                                   Mux(ren1, normal_victim_tag_comb, normal_victim_tag_reg))
  //todo what's the function of flush
  io.result.bits.l2cidx := Mux(flush_issue_regnext, 0.U, read_bits_reg.l2cidx)
  io.result.bits.param  := Mux(flush_issue_regnext, 0.U, read_bits_reg.param)
  io.result.bits.spike_info.foreach( _ := read_bits_reg.spike_info.getOrElse(0.U) )
}

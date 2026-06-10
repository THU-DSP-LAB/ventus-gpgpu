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

// nn64k-007 Phase 4.5 迭代6: directory-local reservation 的 mixed 撤销线 payload。
// Scheduler 在某 MSHR 被标 mixed 那拍（primary Get 被 secondary 撞，sche_dir_valid 被抑制 →
// 注定不再 io.write.fire → reservation 永不自然清 → 孤儿）把该 MSHR 的 (set,way) 喂回来，
// 主动撤销其 reservation。详见 bugs/nn64k-007/checkpoint_3.md 迭代6。
class ResvClear_lite(params: InclusiveCacheParameters_lite) extends Bundle {
  val set = UInt(params.setBits.W)
  val way = UInt(params.wayBits.W)
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
    // nn64k-007 Phase 4.5 迭代6: mixed 落空撤销线（Scheduler→Directory），见 ResvClear_lite。
    val resv_clear = Flipped(Valid(new ResvClear_lite(params)))
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
      }.elsewhen(io.write.fire && io.write.bits.set===i.asUInt && io.write.bits.way===j.asUInt) {  // nn64k-007 迭代6: .valid→.fire, 与 reservation clear / BankedStore 写同拍原子交接
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

  // nn64k-007 Phase 4.5 迭代6: directory-local reservation bitmap（per-set 每 way 1 位）。
  // 根因: victim invalid-first 用 ~status_reg(set).valid 选 way，但 status_reg.valid 置位
  //   (io.write.fire = fill commit) 晚于 victim 决策整个 DRAM round-trip(~14cyc)，期间背靠背
  //   同 set Get miss 都看到 victim way valid=0 → 确定性撞同一 way → 后一个 fill 覆盖前一个
  //   (BankedStore set/way 错位 → SM 读出邻 cacheline 数据 → 函数指针腐败 → 跳 data 段崩溃)。
  // 修法: victim 选中 way 那拍(will_alloc_victim) set reservation; 在两个"确定到达"的终结点清:
  //   (a) 兑现 = io.write.fire(refill commit, 同拍拉高 valid + 写 BankedStore 那个 way, 三位一体)
  //   (b) 落空 = io.resv_clear(该 Get 被 secondary 撞成 mixed, MSHR 抑制 sche_dir_valid 注定不 fire)
  //   victim 选择避开 valid∪reserved; 无真空 way 时 read.ready 用 victim_stall 挡新 read。
  //   will_alloc_victim 已带 !not_replace ⇒ 纯 Put miss / secondary miss(tag_match) 天然不 set
  //   (它们不写 BankedStore/directory、不占 way)，故只剩 mixed 一条孤儿路径需 (b) 撤销。
  val reservation = RegInit(VecInit(Seq.fill(params.cache.sets)(0.U(params.cache.ways.W))))
  val will_alloc_victim = io.result.fire && !io.result.bits.hit && !io.result.bits.flush && !not_replace

  // bfs4096-010 fix A（触发层）: victim 选择优先填 invalid way（invalid-first）。
  //   根因: 原 `victimWay = victim_LFSR(...)` 纯 LFSR 随机, 不查 status_reg(set).valid,
  //         即便 set 还有空闲(invalid) way 也可能盲选中 valid+dirty way 当 victim →
  //         触发 L2 evict-vs-fill WAR(dirty victim readback 被同事务 fill 写早覆盖) →
  //         把新住户数据当 victim 写回旧地址(cost DRAM 0x90004100 整行被 graph 污染)。
  //   改法: 有 invalid way 时优先选第一个 invalid(不 evict, 不踢 dirty victim);
  //         set 全 valid 时才退回原 LFSR 随机。
  //   注: 触发层缓解, 非治本——set 满时 LFSR 仍可选 dirty victim, WAR 仍 latent,
  //       根治需 fix B(保证 evict readback 早于同事务 fill write)。
  //       详见 bugs/bfs4096-010/checkpoint_3_rtl_dive.md + phase_4_report.md §8。
  // nn64k-007 迭代6: victim 避开 valid∪reserved(occupied)。四态(5-D 与 victim_stall 精确等价):
  //   (1) effInvalidVec≠0      : 有真空 way(非 valid 且非 reserved) → PriorityEncoder 选它
  //   (2) effInvalid=0,invalid≠0: 空 way 全被 reserved → 占位 0.U(被 victim_stall 挡, 永不采纳)
  //   (3) set 真满(invalid=0),有非 reserved valid → lfsrPick(mask-safe) evict
  //   (4) 全 reserved          : 占位 0.U(被 victim_stall 挡)
  val validVec      = status_reg(set).valid.asUInt
  val invalidVec    = (~validVec).asUInt
  val resvVec       = reservation(set)
  val effInvalidVec = invalidVec & (~resvVec).asUInt
  val nonResvValid  = validVec   & (~resvVec).asUInt
  // mask-safe lfsr pick: lfsr∩mask 非空则选交集首位(伪随机), 否则 mask 首位; 输出恒 ∈ mask
  val lfsrMaskedFull = victim_LFSR(params.cache.ways-1,0) & nonResvValid
  val fullPick       = Mux(lfsrMaskedFull.orR, PriorityEncoder(lfsrMaskedFull), PriorityEncoder(nonResvValid))
  val victimWay = Mux(effInvalidVec.orR, PriorityEncoder(effInvalidVec),
                      Mux(invalidVec.orR, 0.U,
                          Mux(nonResvValid.orR, fullPick, 0.U)))

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
  // nn64k-007 迭代6: victim_stall(codex#3 精确公式 + next-state)。occSetThisCyc 并入同拍刚预定的 way
  // (修 same-cycle hole); rdInvalidVec.orR 项修 mixed-state hole(有空 way 但全 reserved 时不踢 valid, 等填回)。
  val rdSet         = io.read.bits.set
  val occSetThisCyc = Mux(will_alloc_victim && (io.result.bits.set === rdSet),
                          UIntToOH(io.result.bits.way, params.cache.ways), 0.U)
  val rdValidVec    = status_reg(rdSet).valid.asUInt
  val rdResvNext    = reservation(rdSet) | occSetThisCyc
  val rdInvalidVec  = (~rdValidVec).asUInt
  val rdEffInvalid  = rdInvalidVec & (~rdResvNext).asUInt
  val rdNonResvVal  = rdValidVec   & (~rdResvNext).asUInt
  val victim_stall  = (rdEffInvalid === 0.U) && (rdInvalidVec.orR || (rdNonResvVal === 0.U))
  io.read.ready := (((wipeDone && !io.write.fire) && !victim_stall) || (setQuash_1 && tagMatch_1)) && !flush_issue_reg  && io.result.ready//also fire when bypass
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

  // nn64k-007 Phase 4.5 迭代6: reservation set/clear。
  //   set:   will_alloc_victim 那拍预定选中的 victim way(只 primary Get miss, 见 will_alloc_victim 定义)
  //   clear: (a) io.write.fire = refill commit 兑现   (b) io.resv_clear = mixed 落空撤销
  //   set 与 clear 不同 way(set 是新 alloc 的 way, clear 是已 in-flight MSHR 的 way), 不冲突。
  val resv_set_oh   = Mux(will_alloc_victim,    UIntToOH(io.result.bits.way,     params.cache.ways), 0.U)
  val resv_clr_w_oh = Mux(io.write.fire,        UIntToOH(io.write.bits.way,      params.cache.ways), 0.U)
  val resv_clr_m_oh = Mux(io.resv_clear.valid,  UIntToOH(io.resv_clear.bits.way, params.cache.ways), 0.U)
  for (i <- 0 until params.cache.sets) {
    val s   = Mux(io.result.bits.set     === i.asUInt, resv_set_oh,   0.U)
    val c_w = Mux(io.write.bits.set      === i.asUInt, resv_clr_w_oh, 0.U)
    val c_m = Mux(io.resv_clear.bits.set === i.asUInt, resv_clr_m_oh, 0.U)
    when(!wipeDone) {
      reservation(i) := 0.U
    }.otherwise {
      reservation(i) := (reservation(i) & (~(c_w | c_m)).asUInt) | s
    }
  }

  // nn64k-007 迭代6 诊断(non-fatal, 留痕不中断): victim_stall 若漏挡, will_alloc_victim 会落到
  // placeholder 分支(2)(effInvalid=0 但 invalidVec≠0) → 把 reserved way 当 victim alloc。正确则永不打印。
  when(will_alloc_victim && !effInvalidVec.orR && invalidVec.orR) {
    printf(p"[RESV_PLACEHOLDER_ALLOC] set=${io.result.bits.set} way=${io.result.bits.way} (victim_stall leak)\n")
  }
}

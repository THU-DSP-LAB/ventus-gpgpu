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
import freechips.rocketchip.util._
import TLMessages._
import TLPermissions._


class SourceDRequest_lite(params: InclusiveCacheParameters_lite) extends DirectoryResult_lite(params){

}
class TLBundleD_lite(params: InclusiveCacheParameters_lite) extends Bundle
{
  val opcode=UInt(params.op_bits.W)
  val size=UInt(params.size_bits.W)
  val source=UInt(params.source_bits.W)
  val data  = UInt(params.data_bits.W)
  val param =UInt(3.W)
}
class TLBundleD_lite_plus(params: InclusiveCacheParameters_lite)extends TLBundleD_lite(params)
{
  val address=UInt(params.addressBits.W)
}
class TLBundleD_lite_plus_custom(params: InclusiveCacheParameters_lite)extends TLBundleD_lite_plus(params){
  override val source=UInt(params.source_bits_custom.W)
}
class TLBundleD_lite_withid(params: InclusiveCacheParameters_lite)extends DirectoryResult_lite(params)
{
  val from_mem = Bool()
}

class SourceD(params: InclusiveCacheParameters_lite) extends Module
{
  val io = IO(new Bundle {

    val req = Flipped(Decoupled(new TLBundleD_lite_withid(params))) 
    val d = Decoupled(new TLBundleD_lite_plus(params))

    val pb_pop = Decoupled(new PutBufferPop(params))
    val pb_beat = Input(new PutBufferAEntry(params)) 
 
    val bs_radr = Decoupled(new BankedStoreInnerAddress(params))
    val bs_rdat = Input(  new BankedStoreInnerDecoded(params))
    val bs_wadr = Decoupled(new BankedStoreInnerAddress(params))
    val bs_wdat = Output(new BankedStoreInnerPoison(params))
    val a       = Decoupled(new FullRequest(params))
    val mshr_wait = Output(Bool())

    val finish_issue = Output(Bool())
    // srad-004 Phase 4.5 A''': hit-reservation one-shot clear（io.d.fire 时回传 (set,way)→Directory 递减 hitRefCount）。
    val hit_done = Valid(new ResvClear_lite(params))
    // srad-007 Bug3 fix(B): dirty-victim writeback A 请求发出那拍回传 (set,way)，给 Scheduler 清 evictReadPending。
    val evict_read_done = Valid(new ResvClear_lite(params))
    // btree-002 fix: L2 hit Put 的 BankedStore 写已完成且 D ack 已 fire。
    // Scheduler 用该事件 retire L2 write-through scoreboard 的一个 pending count；
    // miss/no-allocate Put 的 DRAM commit 点不走这里，而由 wt_miss_a 随 write_buffer entry 到 out_a.fire retire。
    val wt_hit_commit = Valid(UInt(params.addressBits.W))
    // btree-002 fix v3.2: 当前 io.a beat 是否为真正的 write-through miss/no-allocate Put。
    // dirty-victim writeback 虽然 io.a.bits.opcode 也会被改成 PutFullData，但该标志保持 false。
    val wt_miss_a = Output(Bool())
    val maintenance_writes_drained = Input(Bool())
  })


  io.pb_pop.valid:=  io.req.fire&& (io.req.bits.opcode===PutFullData|| io.req.bits.opcode===PutPartialData)  //&& !io.req.bits.from_mem && io.req.bits.hit  //all write acknowledgement response are from source D
  io.pb_pop.bits.index:=io.req.bits.put //sink D also support pop,source D only considers write hit pop
  val pb_beat_reg_init=WireInit(0.U.asTypeOf(new PutBufferAEntry(params)))
  val pb_beat_reg=RegInit(pb_beat_reg_init)
  //stage
  val stage_1 :: stage_2 :: stage_3 ::stage_4::stage_5::stage_6::stage_7::stage_8::Nil =Enum(8)
  val stateReg= RegInit(stage_1)
  val s1_req_reg_init=WireInit(0.U.asTypeOf(new TLBundleD_lite_withid(params)))
  s1_req_reg_init.opcode:=5.U

  val s1_req_reg = RegInit(s1_req_reg_init)
  when(io.req.fire){
    s1_req_reg:=io.req.bits
    pb_beat_reg:=io.pb_beat
  }
  val busy = RegInit(false.B)

  val pb_beat =Mux(io.req.fire, io.pb_beat, pb_beat_reg)
  val s1_req =Mux(io.req.fire, io.req.bits, s1_req_reg)  //stall if busy
  val s_final_req=RegNext(s1_req)  ///
  val s1_need_w =(s1_req.opcode===PutFullData || s1_req.opcode===PutPartialData) && !s1_req.from_mem &&s1_req.hit

  val s1_need_r =((s1_req.opcode===Get) && s1_req.hit) || ((!s1_req.hit ||s1_req.opcode===Hint) && s1_req.dirty)//&& s1_req.hit  //read hit or miss dirty


  val s1_valid_r = s1_need_r


  val read_sent_reg=RegInit(false.B)
  when((s1_valid_r&&io.bs_radr.ready)){
    read_sent_reg:=true.B
  }.otherwise{
    read_sent_reg:=false.B
  }
  val read_sent=Mux(io.req.fire,false.B,read_sent_reg)
  io.bs_radr.valid     :=s1_valid_r &&(!read_sent)   //第一个周期就送过去了
  io.bs_radr.bits.way  := s1_req.way
  io.bs_radr.bits.set  := s1_req.set
  io.bs_radr.bits.mask := s1_req.mask

  val about_to_not_busy = ((stateReg === stage_3) && io.a.fire) || ((stateReg === stage_4) && Mux((!s_final_req.hit && (s_final_req.opcode === PutFullData || s_final_req.opcode === PutPartialData)), io.a.fire && io.d.fire, io.d.fire)) || ((stateReg === stage_8) && io.d.fire) ||
    ((stateReg === stage_7) && io.a.fire)
  io.req.ready  := !busy// || about_to_not_busy

  val s1_w_valid=s1_need_w
  val sourceA_sent_reg=RegInit(false.B)
  when ((s1_req.opcode===PutFullData ||s1_req.opcode===PutPartialData) &&io.a.ready){
    sourceA_sent_reg:=true.B
  }.otherwise{
    sourceA_sent_reg:=false.B
  }
  val sourceA_sent=Mux(io.req.fire,false.B,sourceA_sent_reg)


  val write_sent_reg=RegInit(false.B)
  // A hit Put owns exactly one BankedStore write. Keep completion sticky until
  // the next request; port backpressure after the write must not re-arm it.
  when(io.bs_wadr.fire) {
    write_sent_reg:=true.B
  }.elsewhen(io.req.fire) {
    write_sent_reg:=false.B
  }
  val write_sent=Mux(io.req.fire,false.B,write_sent_reg)



val tobedone=RegInit(false.B) //all resources not ready
val mshr_wait_reg =RegInit(false.B)
  switch(stateReg){
    is(stage_1){
      busy:=false.B
      mshr_wait_reg :=false.B
      when (io.req.fire || tobedone) {
        mshr_wait_reg := false.B
        when(!s1_req.hit) {
          when(s1_req.dirty) {
            mshr_wait_reg := true.B //used for kicking out victim way, to block premature potential miss request of victim way
          }.otherwise { //miss but not dirty?? from mshr
            stateReg := stage_4
            busy := true.B
            tobedone := false.B
            // srad-005 root-fix: 原为 `&&` 恒假（同一 opcode 不可能同时 ==PutFullData(0) 且 ==PutPartialData(1)）→
            // write-miss-no-allocate(write-through) 路径 mshr_wait_reg 永不置位 → io.mshr_wait≡0 → MSHR.scala:97 闸常开 →
            // 同块后续 Get refill 抢在 in-flight write-through Put 落 DRAM 前发往 DRAM → 读 pre-Put stale(0xa0000000) →
            // 腐败 soft-float sign-mask spill/restore → vor Inf/NaN → __truncdfsf2 错 → INT_MIN col39/40。
            // 改 `||` 恢复设计意图（注释即"wait write miss no allocate"；对照 line 142 dirty-victim 用单条件置 mshr_wait 同序化机制）。
            when(s1_req.opcode === PutFullData || s1_req.opcode === PutPartialData) { //wait write miss no allocate
              mshr_wait_reg := true.B
            }
          }
        }
        //        when(!s1_req.hit && s1_req.opcode===Get ){
        //          stateReg := stage_4
        //          busy := true.B
        //          tobedone:=false.B
        //        }.else
        when(s1_valid_r && io.bs_radr.ready) { //for read hit or miss dirty
          when(((!s1_req.hit && s1_req.opcode=/=Hint) || (s1_req.opcode === Hint && !s1_req.last_flush)) && s1_req.dirty) { //miss dirty should read bankstore, then to source A
            stateReg := stage_3
            busy := true.B
            tobedone := false.B

          }.otherwise { // read hit
            stateReg := stage_4
            busy := true.B
            tobedone := false.B
          }
        }.elsewhen((s1_req.opcode === PutFullData || s1_req.opcode === PutPartialData) && !s1_req.from_mem && s1_req.hit) { // for write hit,should write bankstore
          when(io.bs_wadr.ready) {
            stateReg := stage_4
            busy := true.B
            tobedone := false.B
          }.otherwise {
            stateReg := stage_2 //need to wait to write
            busy := true.B
            tobedone := false.B
          }
          //            when(io.a.ready) {
          //              when(io.bs_wadr.ready ||  !s1_need_w) {
          //                stateReg := stage_4
          //                busy := true.B
          //                tobedone:=false.B
          //              }.otherwise {
          //                stateReg := stage_2
          //                busy := true.B
          //                tobedone:=false.B
          //              }
          //            }.otherwise {
          //              when(io.bs_wadr.ready||  !s1_need_w) {
          //                stateReg := stage_3
          //                busy := true.B
          //                tobedone:=false.B
          //              }
          //            }


        }.elsewhen(s1_req.opcode===Hint && s1_req.last_flush){ //last flush but not dirty.
          stateReg:= stage_8
          busy := false.B
          tobedone := false.B
          mshr_wait_reg := false.B
        }.otherwise {
          busy := true.B
          tobedone := true.B

        }
      }
    }
    is(stage_2){
      when(io.bs_wadr.ready){
        stateReg:=stage_4
      }
    }
    is(stage_3) { //writeback dirty cache line
      when(io.a.fire) {
        stateReg := stage_1
        busy := false.B
        tobedone := false.B
        mshr_wait_reg := false.B
      }
    }
    is(stage_4) { //ack for miss and hit
      when((!s_final_req.hit && (s_final_req.opcode===PutFullData ||s_final_req.opcode===PutPartialData)) || s_final_req.opcode===Hint) { //ack for write miss no allocate
        when(io.d.fire && io.a.fire) {
          busy := false.B
          stateReg := stage_1
          tobedone := false.B //todo may cause fault
          mshr_wait_reg := false.B
        }.elsewhen(io.d.fire) {
          stateReg := stage_7
        }.elsewhen(io.a.fire) {
          stateReg := stage_8
        }
      }.otherwise{ //ack for read miss from mshr or read hit
        when(io.d.fire) {
          stateReg := stage_1
          busy := false.B
          tobedone := false.B
          mshr_wait_reg := false.B
        }
      }
    }

    is(stage_7){
      when(io.a.fire){
        stateReg := stage_1
        busy := false.B
        tobedone := false.B
        mshr_wait_reg := false.B
      }
    }
    is(stage_8){ //wait for d ready
      when(io.d.fire){
        stateReg := stage_1
        busy := false.B
        tobedone := false.B
        mshr_wait_reg := false.B
      }
    }
  }
  io.mshr_wait      :=mshr_wait_reg
  // A write is active only on initial request acceptance or while stage_2 is
  // retrying a write that lost BankedStore arbitration to a refill.
  val hitPutWriteActive = io.req.fire || stateReg === stage_2
  io.bs_wadr.valid   :=    s1_w_valid && hitPutWriteActive &&(!write_sent)
  io.bs_wadr.bits.set:=    s1_req.set
  io.bs_wadr.bits.way:=    s1_req.way
  io.bs_wdat.data    :=    pb_beat.data
  io.bs_wadr.bits.mask:=   pb_beat.mask

  assert(!(stateReg === stage_1 && !io.req.fire && io.bs_wadr.valid),
    "SourceD emitted a stale hit-Put BankedStore write while idle")

  ///将读取数据输出d
  val finalMaintenanceRsp = s_final_req.opcode === Hint && s_final_req.last_flush
  io.d.valid        :=((stateReg===stage_4 || stateReg===stage_8)&& !(s_final_req.opcode===Hint && !s_final_req.last_flush) &&
    (!finalMaintenanceRsp || io.maintenance_writes_drained)) //&& s1_req.opcode===Get)//数据读出来之后准备输出
  io.d.bits.source  :=s_final_req.source
  io.d.bits.opcode  :=Mux(s_final_req.opcode===Get,AccessAckData,Mux(s_final_req.last_flush,HintAck,AccessAck))
  io.d.bits.size    := s_final_req.size
  io.d.bits.data    :=Mux(s_final_req.opcode===Get,Mux(s_final_req.hit, io.bs_rdat.data,s_final_req.data),0.U.asTypeOf(io.bs_rdat.data)) //Mux(s_final_req.opcode===Get,io.bs_rdat.data,0.U.asTypeOf(io.bs_rdat.data)) //要求应该是读的情况，写的情况直接返回0
  io.d.bits.address      := params.expandAddress(s_final_req.tag,s_final_req.l2cidx, s_final_req.set,s_final_req.offset)
////将读出的数据返回给sourceA
  io.d.bits.param := 0.U


  io.a.valid      :=(stateReg===stage_4 || stateReg===stage_7 || stateReg===stage_3) && ((!s_final_req.hit ||s_final_req.opcode===Hint) && (s_final_req.dirty || s_final_req.opcode===PutFullData ||s_final_req.opcode===PutPartialData))// !s_final_req.hit && (!sourceA_sent) && s_final_req.dirty//(s1_req.opcode===PutFullData ||s1_req.opcode=== PutPartialData)  &&(!sourceA_sent)  //todo for miss kickout dirty cacheline no writethrough , write/read miss kickout dirty ,write allocate
  io.a.bits       := s_final_req

  io.a.bits.data  := Mux((s_final_req.opcode===PutFullData ||s_final_req.opcode=== PutPartialData),s_final_req.data,io.bs_rdat.data) // should be victim data 写miss的数据也经过这个地方转给sourceA
  io.a.bits.opcode:= PutFullData
  // srad-006 root-fix: dirty-victim writeback 强制 opcode=PutFullData(上行)却**没 override mask** →
  // 沿用触发它的 refill-Get 的 don't-care mask（0x1111=每 word 仅 byte0）→ 写回 WG49 脏 sign-mask
  // (payload 在 byte3) 退化成只写 byte0=0x00 = 语义 no-op → DRAM 保持 pre-write stale → refill 读 stale(srad output=1)。
  // 镜像上面 L287 的 data Mux：write-through(PutFull/Part) 留真 partial byte-enables（其 data 只在 masked lane 有效）；
  // dirty-victim writeback / flush（Get/Hint 触发，data 取 bs_rdat 全行）用 all-1s 全字节写回。
  // 实现：Fill(N, !is_put_op) 取代 ~(0.U(N.W))；后者在 Verilog 生成 128'hFFFF... 大常量，触发
  // Verilator v5.034 V3FuncOpt.cpp:162 内部断言（语义等价：OR全1 = all-1s mask）。
  io.a.bits.mask  := s_final_req.mask | Fill(params.mask_bits, !(s_final_req.opcode===PutFullData || s_final_req.opcode===PutPartialData))

  // btree-002 fix v3.2: SourceD 保留原始 s_final_req.opcode，可在这里区分 write-through miss Put
  // 与 dirty-victim writeback。Scheduler 把该位随 write_buffer 保存，直到真实 out_a.fire 才 retire count。
  io.wt_miss_a := (stateReg === stage_4 || stateReg === stage_7) && !s_final_req.hit &&
    (s_final_req.opcode === PutFullData || s_final_req.opcode === PutPartialData)

  io.finish_issue := io.d.fire && s_final_req.last_flush

  // srad-004 Phase 4.5 A''': hit-reservation clear（one-shot，统一到 io.d.fire）。
  // io.d.fire 在 stage_4/stage_8 各自最多一次（SourceD entry 释放条件），保证 1:1 对应 will_resv_hit。
  val hitClrOpcode = s_final_req.opcode === Get ||
                     s_final_req.opcode === PutFullData ||
                     s_final_req.opcode === PutPartialData
  io.hit_done.valid     := io.d.fire && s_final_req.hit && hitClrOpcode
  io.hit_done.bits.set  := s_final_req.set
  io.hit_done.bits.way  := s_final_req.way

  // srad-007 Bug3 root-fix(B): L2 BankedStore evict-vs-fill WAR — dirty-victim eviction 的 BankedStore 读
  // (SourceD stage_3 bs_radr) 必须早于 refill 经 sinkD 写同 (set,way)。dirtyVictimWbFire = 该 victim writeback
  // A 请求发出那拍(io.a.fire 且 s_final_req 是 dirty miss 的读路径 Get/Hint-evict，非 write-through Put)，
  // 此拍 evict 读已完成(stage_3 先读后发)，回传 (set,way) 给 Scheduler 清 evictReadPending → 放行 fill 写。
  // codex 4.5.5: 收窄到 opcode===Get（refill-eviction 才置 pending）；flush-dirty writeback(Hint)也读 bs_rdat 写回
  // 但非 refill-eviction，不该按 (set,way) 误清 pending（flush 可与 MSHR 重叠）。
  val dirtyVictimWbFire = io.a.fire && s_final_req.dirty && !s_final_req.hit && (s_final_req.opcode === Get)
  io.evict_read_done.valid    := dirtyVictimWbFire
  io.evict_read_done.bits.set := s_final_req.set
  io.evict_read_done.bits.way := s_final_req.way

  // btree-002 fix: hit Put 的真正 L2 commit 点。
  // SourceD 只有在 BankedStore 写端口已经被接受后才会进入 stage_4 并发 D ack；
  // 因此 io.d.fire && hit && Put 表示该 Put 的 bytes 已经在 L2 BankedStore 可见。
  val wtHitCommitFire = io.d.fire && s_final_req.hit &&
    (s_final_req.opcode === PutFullData || s_final_req.opcode === PutPartialData)
  io.wt_hit_commit.valid := wtHitCommitFire
  io.wt_hit_commit.bits  := params.expandAddress(s_final_req.tag, s_final_req.l2cidx, s_final_req.set, s_final_req.offset)
}

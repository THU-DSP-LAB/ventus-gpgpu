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

import freechips.rocketchip.diplomacy.AddressSet
import freechips.rocketchip.tilelink._
import freechips.rocketchip.util._
import TLPermissions._
import TLMessages._
import chisel3._
import chisel3.util._
class TLBundle_AD (params: InclusiveCacheParameters_lite)extends Bundle{
  val a =new  TLBundleA_lite(params)
  val d = new  TLBundleD_lite(params)

}

class WriteBufferEntry_lite(params: InclusiveCacheParameters_lite) extends Bundle
{
  val req = new FullRequest(params)
  val wt_miss = Bool()
}

class Scheduler(params: InclusiveCacheParameters_lite) extends Module
{
  val io = IO(new Bundle {

    val in_a =Flipped(Decoupled( new TLBundleA_lite(params))) 
    val in_d =Decoupled(new TLBundleD_lite_plus(params))
    val out_a =Decoupled(new TLBundleA_lite(params))
    val out_d=Flipped(Decoupled(new TLBundleD_lite(params)))
  })


 
  val sourceA = Module(new SourceA(params))

  val sourceD = Module(new SourceD(params))

  val sinkA = Module(new SinkA(params))
  val sinkD = Module(new SinkD(params))
  io.out_a.valid := sourceA.io.a.valid 
  io.out_a.bits:=sourceA.io.a.bits
  sourceA.io.a.ready:=io.out_a.ready


//  sinkA.io.pb_pop2<>sinkD.io.pb_pop
//  sinkD.io.pb_beat<>sinkA.io.pb_beat2
  sourceD.io.pb_pop<>sinkA.io.pb_pop
  sourceD.io.pb_beat<>sinkA.io.pb_beat

  sinkD.io.d.bits:=io.out_d.bits
  sinkD.io.d.valid:=io.out_d.valid
  io.out_d.ready:=sinkD.io.d.ready

  val request = Wire(Decoupled(new FullRequest(params)))

  val issue_flush_invalidate= RegInit(false.B)
  when(request.fire && request.bits.opcode===Hint ){
    issue_flush_invalidate :=true.B
  }.elsewhen(sourceD.io.finish_issue){
    issue_flush_invalidate :=false.B
  }// sourceD will decide when will finish flush/invalidate


  sinkA.io.a.bits:= io.in_a.bits
  sinkA.io.a.valid:=io.in_a.valid
//  sinkA.io.index:= sinkD.io.index


  io.in_a.ready:=sinkA.io.a.ready

  // btree-002 fix: sourceD.io.d 在下方 write-through scoreboard 定义后连接。
  // AccessAckData 发回 L1 前必须做 L2-side byte merge，避免 stale refill 被 L1 install。





  val directory = Module(new Directory_test(params))

  val bankedStore = Module(new BankedStore(params))


  val requests = Module(new ListBuffer(ListBufferParameters(new Merge_meta(params), params.mshrs, params.secondary, false,true)))

  val mshrs = Seq.fill(params.mshrs) { Module(new MSHR(params)) }

  // srad-007 Bug3 root-fix(B): L2 BankedStore evict-vs-fill WAR — per-MSHR 标志。dirty-victim 分配时置位，
  // SourceD 发出 victim writeback(=evict 读已完成)时清。置位期间 block 该 MSHR 的 fill commit
  // (schedule.d/dir + pop + bankedStore sinkD 写)，保证 evict 读早于 fill 写同 (set,way) → 消除 WAR。
  val evictReadPending = RegInit(VecInit(Seq.fill(params.mshrs)(false.B)))

  // lud-002/srad Bug C 修(B-2 + codex 3.5 修): flush 写回抢占 sourceD 的优先信号(前置 Wire, 实赋值在 dir_result_buffer 定义后)。
  // 抢占那拍须一并 gate mshr_request 的 sourceD 项 + m.io.schedule.d.ready + pop.valid, 否则被选 MSHR 看到
  // spurious schedule.d.fire(以为自己 D 已发, 实则 sourceD 在发 flush) → refill D 永久丢。
  val flush_wb_priority = Wire(Bool())





  val mshr_request = Cat(mshrs.zipWithIndex.map {  case (m, i) =>
    ((sourceA.io.req.ready  &&m.io.schedule.a.valid) ||
      (sourceD.io.req.ready &&m.io.schedule.d.valid && !flush_wb_priority && !evictReadPending(i)) ||  // B-2 + srad-007 Bug3 fix(B): evict 读未完成不放 fill 完成路
      (m.io.schedule.dir.valid&&directory.io.write.ready && !evictReadPending(i)))  // srad-007 Bug3 fix(B)
  }.reverse)

  val robin_filter = RegInit(0.U(params.mshrs.W))  
  val robin_request = Cat(mshr_request, mshr_request & robin_filter)
  val mshr_selectOH2 = (~(leftOR(robin_request) << 1)).asUInt & robin_request
  val mshr_selectOH = mshr_selectOH2(2*params.mshrs-1, params.mshrs) | mshr_selectOH2(params.mshrs-1, 0)
  val mshr_select = OHToUInt(mshr_selectOH)
  // srad-007 Bug3 fix(B): 被选中 MSHR 是否在 evict-read-pending — 用于 gate fill commit(dir write/pop/sinkD 写)。
  val selectedEvictReadPending = (mshr_selectOH & evictReadPending.asUInt).orR
  val fillCommitAllowed = !selectedEvictReadPending && !directory.io.write_hit_hazard

 
  val schedule    = Mux1H (mshr_selectOH, mshrs.map(_.io.schedule))


  sinkD.io.way := VecInit(mshrs.map(_.io.status.way))(sinkD.io.source)
  sinkD.io.set := VecInit(mshrs.map(_.io.status.set))(sinkD.io.source)
  sinkD.io.opcode := VecInit(mshrs.map(_.io.status.opcode))(sinkD.io.source)
  sinkD.io.put := VecInit(mshrs.map(_.io.status.put))(sinkD.io.source)
  sinkD.io.sche_dir_fire.valid := schedule.dir.fire
  sinkD.io.sche_dir_fire.bits :=mshr_select


  when (mshr_request.orR) { robin_filter := ~rightOR(mshr_selectOH) }

  
  schedule.a.bits.source := mshr_select
  val writeBufferEntries = 8
  val write_buffer =Module(new Queue(new WriteBufferEntry_lite(params),writeBufferEntries,false,true))
  mshrs.zipWithIndex.foreach { case (m, i) =>
    m.io.sinkd.valid := sinkD.io.resp.valid && (sinkD.io.resp.bits.source === i.asUInt)&&(sinkD.io.resp.bits.opcode===AccessAckData)
    m.io.sinkd.bits  := sinkD.io.resp.bits
    m.io.schedule.a.ready  := sourceA.io.req.ready&&(mshr_select===i.asUInt) && !write_buffer.io.deq.valid
    m.io.schedule.d.ready  := sourceD.io.req.ready&&(mshr_select===i.asUInt)&& requests.io.valid(i) && !flush_wb_priority && !evictReadPending(i)  // B-2 + srad-007 Bug3 fix(B): evict 读未完成不 pop(否则丢 refill D)
    m.io.schedule.dir.ready:= directory.io.write.ready && (mshr_select===i.asUInt) &&
      !evictReadPending(i) && fillCommitAllowed
    m.io.valid      := requests.io.valid(i) //用于在refill的时候拉低mshr的sourced
    m.io.mshr_wait  := sourceD.io.mshr_wait
    m.io.merge.valid:= m.io.schedule.d.valid && ((requests.io.data.opcode===PutFullData) ||(requests.io.data.opcode===PutPartialData)) &&(mshr_select===i.asUInt)
    m.io.merge.bits := requests.io.data
  }

 

  write_buffer.io.enq.valid:=sourceD.io.a.valid
  write_buffer.io.enq.bits.req:=sourceD.io.a.bits
  write_buffer.io.enq.bits.wt_miss:=sourceD.io.wt_miss_a
  write_buffer.io.deq.ready:= sourceA.io.req.ready
  sourceA.io.req.bits:=Mux(write_buffer.io.deq.valid,write_buffer.io.deq.bits.req,schedule.a.bits)

  sourceA.io.req.valid:=Mux(write_buffer.io.deq.valid,write_buffer.io.deq.valid,schedule.a.valid)
  sourceD.io.a.ready:= write_buffer.io.enq.ready

  // A maintenance HintAck is a completion fence, not merely a directory-sweep
  // marker.  Hold the final response until every dirty victim generated by the
  // sweep, including the final victim itself, has left the L2 write path.
  sourceD.io.maintenance_writes_drained :=
    !write_buffer.io.deq.valid && !sourceD.io.a.valid &&
      !sourceA.io.req.valid && !io.out_a.valid

  // btree-002 fix: L2-side pending write-through data scoreboard.
  // 目的：PutPartial/PutFull 进入共享 L2 后，记录同 cacheline 的 pending bytes；
  // 后续 stale read refill / GrantData 离开 L2 前按 byte mask 覆盖，覆盖 cross-SM 与 WSHR-pop-early。
  //
  // 设计约束：
  // - scoreboard lookup 只改 data，不参与 mshr_request/sourceD.req 仲裁；
  // - 只有 scoreboard 满且 incoming Put 是新 line 时才背压 SinkA；
  // - 同 line 多个 Put 用 pending count 计数，避免 committed bit 在多 Put 合并时提前清。
  // btree-002 fix v3.2: scoreboard 容量从 putLists+mshrs+8 增到 putLists+mshrs+8+1。
  //   性能回归根因(hotspot3D_512x2 timeout): lingering entry = 在途 Put(count>0,≤putLists)
  //   + count==0 但同 line read-miss MSHR 活跃(≤mshrs);旧 mshrs+8 在重 stencil 下被填满 →
  //   wtScoreboardCanAccept 背压 request.ready → throttle 整个 L2 → 2x 慢 → timeout。
  //   v3.2 额外 +1 覆盖 SourceD 单入口驻留项：SinkA putbuffer list 已 pop、但 hit-D/out-A 尚未
  //   完成的 Put，或 MSHR 已 pop、但 AccessAckData 被 D-channel 反压滞留的 read D。
  //   因 SourceD.io.req.ready := !busy，二者不可能同时占两个 SourceD 槽；容量上界为
  //   putLists + writeBufferEntries + 1 + mshrs，scoreboard 不应满。
  val wtScoreboardEntries = params.putLists + params.mshrs + writeBufferEntries + 1
  val wtScoreboardCountBits = log2Ceil(params.putLists + params.secondary + params.mshrs + 16) + 1

  val wtValid = RegInit(VecInit(Seq.fill(wtScoreboardEntries)(false.B)))
  val wtTag = RegInit(VecInit(Seq.fill(wtScoreboardEntries)(0.U(params.tagBits.W))))
  val wtL2cidx = RegInit(VecInit(Seq.fill(wtScoreboardEntries)(0.U(params.l2cBits.W))))
  val wtSet = RegInit(VecInit(Seq.fill(wtScoreboardEntries)(0.U(params.setBits.W))))
  val wtData = RegInit(VecInit(Seq.fill(wtScoreboardEntries)(0.U(params.data_bits.W))))
  val wtMask = RegInit(VecInit(Seq.fill(wtScoreboardEntries)(0.U(params.mask_bits.W))))
  val wtCount = RegInit(VecInit(Seq.fill(wtScoreboardEntries)(0.U(wtScoreboardCountBits.W))))

  def wtIsPut(opcode: UInt): Bool = {
    (opcode === PutFullData) || (opcode === PutPartialData)
  }

  def wtMergeBytes(base: UInt, patch: UInt, mask: UInt): UInt = {
    val fullMask = FillInterleaved(params.micro.writeBytes * 8, mask)
    (patch & fullMask) | (base & (~fullMask).asUInt)
  }

  def wtRegLineMatch(tag: UInt, l2cidx: UInt, set: UInt): UInt = {
    VecInit((0 until wtScoreboardEntries).map { i =>
      wtValid(i) && (wtTag(i) === tag) && (wtL2cidx(i) === l2cidx) && (wtSet(i) === set)
    }).asUInt
  }

  val wtPutReq = wtIsPut(request.bits.opcode)
  val wtPutMatchOH = wtRegLineMatch(request.bits.tag, request.bits.l2cidx, request.bits.set)
  val wtFreeVec = (~(wtValid.asUInt)).asUInt
  val wtFreeOH = (~(leftOR(wtFreeVec) << 1)).asUInt & wtFreeVec
  val wtScoreboardCanAccept = !wtPutReq || wtPutMatchOH.orR || wtFreeOH.orR
  val wtPutFire = request.fire && wtPutReq
  val wtPutAllocOH = Mux(wtPutMatchOH.orR, 0.U(wtScoreboardEntries.W), wtFreeOH)
  val wtPutTargetOH = Mux(wtPutMatchOH.orR, wtPutMatchOH, wtPutAllocOH)

  def wtMergeLine(tag: UInt, l2cidx: UInt, set: UInt, base: UInt): UInt = {
    val hitOH = wtRegLineMatch(tag, l2cidx, set)
    val hit = hitOH.orR
    val regData = Mux1H((0 until wtScoreboardEntries).map(i => hitOH(i) -> wtData(i)))
    val regMask = Mux1H((0 until wtScoreboardEntries).map(i => hitOH(i) -> wtMask(i)))
    // btree-002 fix: Reg-based lookup only. Same-cycle bypass removed to avoid combinational
    // cycle through AtomicUnit: io_in_d_bits_data -> ATU L22ATUmemRsp -> ATU2L2memReq ->
    // io_in_a_bits_data -> request.bits.data -> wtMergeLine -> io_in_d_bits_data.
    // For btree-002: Put fires at #574285, fill at #574425 (140 cycles later);
    // same-cycle case is unreachable for this bug, Reg-based coverage is sufficient.
    Mux(hit, wtMergeBytes(base, regData, regMask), base)
  }

  val (wtHitCommitTag, wtHitCommitL2cidx, wtHitCommitSet, _) =
    params.parseAddress(sourceD.io.wt_hit_commit.bits)
  // btree-002 fix v3.2: 只 retire 真正的 write-through miss/no-allocate Put。
  // SourceD 把 dirty-victim writeback 的 A opcode 也强制成 PutFullData；因此不能再用
  // io.out_a.fire && opcode=Put 粗判。wt_miss 标记随 write_buffer 保存，到真实 out_a fire 再减 count。
  val wtMissCommitFire = write_buffer.io.deq.fire && write_buffer.io.deq.bits.wt_miss
  val wtMissCommitTag = write_buffer.io.deq.bits.req.tag
  val wtMissCommitL2cidx = write_buffer.io.deq.bits.req.l2cidx
  val wtMissCommitSet = write_buffer.io.deq.bits.req.set
  val (wtSourceDTag, wtSourceDL2cidx, wtSourceDSet, _) = params.parseAddress(sourceD.io.d.bits.address)
  val wtSourceDHeldReadD = sourceD.io.d.valid && (sourceD.io.d.bits.opcode === AccessAckData)

  for (i <- 0 until wtScoreboardEntries) {
    val putThis = wtPutFire && wtPutTargetOH(i)
    val hitCommitThis = sourceD.io.wt_hit_commit.valid && wtValid(i) &&
      (wtTag(i) === wtHitCommitTag) && (wtL2cidx(i) === wtHitCommitL2cidx) && (wtSet(i) === wtHitCommitSet)
    val missCommitThis = wtMissCommitFire && wtValid(i) &&
      (wtTag(i) === wtMissCommitTag) && (wtL2cidx(i) === wtMissCommitL2cidx) && (wtSet(i) === wtMissCommitSet)

    val commitDecRaw = PopCount(Seq(hitCommitThis, missCommitThis))
    val commitDecExt = Wire(UInt(wtScoreboardCountBits.W))
    commitDecExt := commitDecRaw
    val commitDec = Mux(wtCount(i) < commitDecExt, wtCount(i), commitDecExt)
    val putInc = Mux(putThis, 1.U(wtScoreboardCountBits.W), 0.U(wtScoreboardCountBits.W))
    val nextCount = (wtCount(i) +& putInc - commitDec)(wtScoreboardCountBits-1, 0)

    val sameLineMshrActive = VecInit(mshrs.zipWithIndex.map { case (m, j) =>
      (requests.io.valid(j) || m.io.schedule.dir.valid) &&
        (m.io.status.tag === wtTag(i)) &&
        (m.io.status.l2cidx === wtL2cidx(i)) &&
        (m.io.status.set === wtSet(i))
    }).asUInt.orR
    val sameLineSourceDHeldReadD = wtSourceDHeldReadD &&
      (wtSourceDTag === wtTag(i)) && (wtSourceDL2cidx === wtL2cidx(i)) && (wtSourceDSet === wtSet(i))
    // btree-002 fix v3.2: stale AccessAckData 只要还驻留 SourceD，就继续保留 scoreboard entry。
    // 即使对应 MSHR/dir schedule 已经 pop，最终 D-channel merge 仍需要这份 pending byte mask。
    // sourceD.io.d.valid 只进入寄存器 clear 条件，不反馈 D valid/ready，避免组合环。
    val clearThis = wtValid(i) && (wtCount(i) === 0.U) && !sameLineMshrActive && !sameLineSourceDHeldReadD

    when(putThis || commitDec.orR) {
      when(putThis) {
        wtValid(i) := true.B
        wtTag(i) := request.bits.tag
        wtL2cidx(i) := request.bits.l2cidx
        wtSet(i) := request.bits.set
        wtData(i) := Mux(wtValid(i), wtMergeBytes(wtData(i), request.bits.data, request.bits.mask), request.bits.data)
        wtMask(i) := Mux(wtValid(i), wtMask(i) | request.bits.mask, request.bits.mask)
      }
      wtCount(i) := nextCount
    }.elsewhen(clearThis) {
      wtValid(i) := false.B
      wtData(i) := 0.U
      wtMask(i) := 0.U
      wtCount(i) := 0.U
    }

    assert(!(putThis && !(commitDec.orR) && (wtCount(i) === ((BigInt(1) << wtScoreboardCountBits) - 1).U)),
      "btree-002 write-through scoreboard pending count overflow")
  }

  // btree-002 fix: final SourceD->L1 D-channel merge.
  // 这是最晚的 GrantData 修正点：即使 stale DRAM response 已进入 SourceD pipeline，
  // 只要还没 fire 给 L1，pending write-through bytes 仍会覆盖出去。
  val wtGrantMergedData = wtMergeLine(wtSourceDTag, wtSourceDL2cidx, wtSourceDSet, sourceD.io.d.bits.data)
  io.in_d.valid := sourceD.io.d.valid
  io.in_d.bits := sourceD.io.d.bits
  io.in_d.bits.data := Mux(sourceD.io.d.bits.opcode === AccessAckData, wtGrantMergedData, sourceD.io.d.bits.data)
  sourceD.io.d.ready := io.in_d.ready

  // The request list may drain before a delayed refill installs its directory
  // entry. Keep that MSHR owned until every operation carrying its status has
  // retired, otherwise a new miss can overwrite the pending refill.
  val mshr_ownedOH = VecInit(mshrs.zipWithIndex.map { case (m, i) =>
    MSHROwnership(
      requests.io.valid(i),
      m.io.schedule.a.valid,
      m.io.schedule.dir.valid,
      evictReadPending(i)
    )
  }).asUInt

  val mshr_free = (~mshr_ownedOH).asUInt.orR
  val mshr_empty = (~mshr_ownedOH).asUInt.andR.asBool
  val putbuffer_empty= sinkA.io.empty
  // Assigned after all maintenance-visible pipelines are defined below.
  val flush_ready = Wire(Bool())
  val invalidate_ready = Wire(Bool())

  request.valid := sinkA.io.req.valid
  request.bits := sinkA.io.req.bits  
 
  sinkA.io.req.ready := request.ready   //if mshr still have entries and if dir ready








  //directory.io.write.bits.data.valid:=schedule.dir.bits.data.valid




  
  val tagMatches = Cat(mshrs.zipWithIndex.map { case(m,i) =>   mshr_ownedOH(i)&&(m.io.status.tag === directory.io.result.bits.tag)&&(m.io.status.set ===directory.io.result.bits.set)&&
    (!directory.io.result.bits.hit) }.reverse)




  val alloc = !(tagMatches.orR )//write miss after write miss is not allowed to alloc, WRW, RWR also not,
  val is_pending = tagMatches.orR && alloc// write miss can alloc but need to wait read miss finish and vice versa.
  val pending_index = OHToUInt(Mux(is_pending,tagMatches,0.U))


  val mshr_insertOH_init=( (~(leftOR((~mshr_ownedOH).asUInt)<< 1)).asUInt & (~mshr_ownedOH ).asUInt)
  val mshr_insertOH =mshr_insertOH_init
  (mshr_insertOH.asBools zip mshrs).zipWithIndex map { case ((s, m), i) =>{
    m.io.allocate.valid:=false.B
    m.io.allocate.bits:=0.U.asTypeOf(new Status(params))
    // bfs4096-005 Phase 4.5 fix: directory.io.result.valid 改 .fire, 跟 line 221 requests.io.push 对称
    // 避免反压多周期内 mshr_insertOH 漂移导致 zombie MSHR (上游 Get 发出但 ListBuffer 没落账,
    // 旧响应数据按 source ID 广播污染换主后的 MSHR data_reg). 详见 phase_4_report.md.
    when (directory.io.result.fire && alloc && s && !directory.io.result.bits.hit && !directory.io.result.bits.flush){
      assert(!mshr_ownedOH(i),
             "MSHR allocation selected an entry that is still owned")
      m.io.allocate.valid := true.B
      m.io.allocate.bits.set := directory.io.result.bits.set
      m.io.allocate.bits.tag := directory.io.result.bits.tag
      m.io.allocate.bits.way := directory.io.result.bits.way
      m.io.allocate.bits.opcode := directory.io.result.bits.opcode
      m.io.allocate.bits.data := directory.io.result.bits.data
      m.io.allocate.bits.dirty := directory.io.result.bits.dirty
      m.io.allocate.bits.mask := directory.io.result.bits.mask
      m.io.allocate.bits.hit := directory.io.result.bits.hit
      m.io.allocate.bits.size := directory.io.result.bits.size
      m.io.allocate.bits.put := directory.io.result.bits.put
      m.io.allocate.bits.last_flush := directory.io.result.bits.last_flush
      m.io.allocate.bits.offset := directory.io.result.bits.offset
      m.io.allocate.bits.source:= directory.io.result.bits.source
      m.io.allocate.bits.flush := directory.io.result.bits.flush

      m.io.allocate.bits.l2cidx:= directory.io.result.bits.l2cidx

      m.io.allocate.bits.spike_info.foreach{ _ := directory.io.result.bits.spike_info.getOrElse(0.U) }
    }}
  }

  // srad-007 Bug3 fix(B): dirty-victim 分配置 evictReadPending；SourceD 发出 victim writeback(=evict 读已完成)时清。
  mshrs.zipWithIndex.foreach { case (m, i) =>
    val dirtyVictimAlloc = directory.io.result.fire && alloc && mshr_insertOH.asBools(i) &&
      !directory.io.result.bits.hit && directory.io.result.bits.dirty && !directory.io.result.bits.flush
    when(dirtyVictimAlloc) {
      evictReadPending(i) := true.B
    }.elsewhen(sourceD.io.evict_read_done.valid &&
               (m.io.status.set === sourceD.io.evict_read_done.bits.set) &&
               (m.io.status.way === sourceD.io.evict_read_done.bits.way)) {
      evictReadPending(i) := false.B
    }
  }

  // nn64k-007 Phase 4.5 迭代6: 算 mixed 的同时收集 mixedVec, 用于驱动 directory.io.resv_clear(撤销孤儿)。
  val mixedVec = Wire(Vec(params.mshrs, Bool()))
  mshrs.zipWithIndex.foreach { case (m, i) =>
    mixedVec(i) := directory.io.result.valid && (OHToUInt(tagMatches)===i.asUInt) && (directory.io.result.bits.opcode =/= m.io.status.opcode)
    m.io.mixed := mixedVec(i)
  }
  // mixed 落空撤销线: tagMatches one-hot ⇒ 至多一个 MSHR 被标 mixed; 把它的 (set,way) 喂回 Directory
  // 清其 reservation(该 MSHR 注定不再 io.write.fire, 见 MSHR.scala:121-123 mixed 抑制 sche_dir_valid)。
  val mixedOH = mixedVec.asUInt
  directory.io.resv_clear.valid    := mixedOH.orR
  directory.io.resv_clear.bits.set := Mux1H(mixedOH, mshrs.map(_.io.status.set))
  directory.io.resv_clear.bits.way := Mux1H(mixedOH, mshrs.map(_.io.status.way))
  // srad-004 Phase 4.5 A''': SourceD hit-done → Directory hit_resv_clear（one-shot hitRefCount decrement）。
  directory.io.hit_resv_clear := sourceD.io.hit_done

  // bfs4096-002 iter5 fix #3: push.valid 看 result.fire 而非 result.valid。
  // ListBuffer 流式 push（push.ready 不满就持续 1）+ result.valid hold（#1 反压触发后）
  // 会让 push.fire 多拍 → 重复 push 同 mshr_index 链表。配套 #1 + #2 一并使用。
  // 详见 bugs/bfs4096-002/checkpoint_3_iter5.md。
  requests.io.push.valid      := directory.io.result.fire && (!directory.io.result.bits.hit) && !directory.io.result.bits.flush
  requests.io.push.bits.data.data  := directory.io.result.bits.data
  requests.io.push.bits.data.mask  := directory.io.result.bits.mask
  requests.io.push.bits.data.put   := directory.io.result.bits.put
  requests.io.push.bits.data.opcode:= directory.io.result.bits.opcode
  requests.io.push.bits.data.source:= directory.io.result.bits.source
  requests.io.push.bits.index := OHToUInt(Mux(alloc,mshr_insertOH,tagMatches))

  // btree-002 fix v3.2: request.ready 与 directory read 使用同一个 scoreboard accept gate。
  // 即使未来 scoreboard 满，新-line Put 也不会出现 Directory 消费而 SinkA 未出队的不一致。
  // A pending primary-miss result already owns the free MSHR selected by
  // mshr_insertOH, even before its fire edge updates the registered ownership
  // state. Do not admit another lookup using that same apparent free entry.
  // Holding an extra result instead would deadlock a same-set refill against
  // Directory's write/result stability interlock.
  val resultReservesMshr = directory.io.result.valid &&
    !directory.io.result.bits.hit && !directory.io.result.bits.flush && alloc
  val resultMshrOH = Mux(resultReservesMshr, mshr_insertOH, 0.U(params.mshrs.W))
  val mshrFreeAfterResult = ((~mshr_ownedOH).asUInt & (~resultMshrOH).asUInt).orR
  val requestCanIssue = mshrFreeAfterResult && requests.io.push.ready && directory.io.ready &&
    wtScoreboardCanAccept && !(issue_flush_invalidate)
  directory.io.read.valid := request.valid && !(request.bits.opcode === Hint) && requestCanIssue
  directory.io.read.bits := request.bits
  directory.io.write.valid := schedule.dir.valid && fillCommitAllowed
  directory.io.tag_match :=tagMatches.orR
  directory.io.write.bits.way := schedule.dir.bits.way
  directory.io.write.bits.set := schedule.dir.bits.set
  directory.io.write.bits.data.tag := schedule.dir.bits.data.tag
  directory.io.invalidate := request.fire && (request.bits.opcode === Hint) && (request.bits.param === 1.U) //will issue until all resource is ready(i.e. MSHR & Put Buffer Drain)
  directory.io.flush := request.fire && (request.bits.opcode === Hint) && (request.bits.param === 0.U)
  directory.io.flush_invalidate_src:= request.bits.source
  requests.io.pop.valid := requests.io.valid(mshr_select)&&schedule.d.valid&&sourceD.io.req.ready && !selectedEvictReadPending  // srad-007 Bug3 fix(B): evict 读未完成不 pop
  requests.io.pop.bits  := mshr_select


  // Maintenance requests sweep the complete directory, so they use the
  // stronger drain condition assigned below. Normal requests keep the
  // existing admission path.
  val maintenanceRequest = request.bits.opcode === Hint
  val maintenanceRequestReady = Mux(request.bits.param === 1.U,
                                    invalidate_ready, flush_ready)
  request.ready := Mux(maintenanceRequest, maintenanceRequestReady,
                       requestCanIssue && directory.io.read.ready)




  // 允许在同一拍同时出队+入队时保持 1/cycle 吞吐（pipe=true）。
  // 这样在 invalidate/flush 扫描出现连续 dirty victim、且 directory.result 只拉高一拍的情况下，
  // 不会因为 1-depth 且 pipe=false 的 Queue 满队列阻塞而导致后一个结果握手失败/丢失。
  // lud-002/srad Bug C 修(B-2): depth 1→4 吸收 invalidate/flush-sweep 的 dirty-victim burst
  //   (drain dive 实证最长 burst=2, depth-1 撞 2-burst 即丢; depth-4 留余量)。配下方 flush-优先 drain + fire-gated clear。
  val dir_result_buffer = Module(new Queue(new DirectoryResult_lite_victim(params), 4, pipe = true))

  dir_result_buffer.io.enq.valid:= directory.io.result.valid && (directory.io.result.bits.hit || directory.io.result.bits.dirty || directory.io.result.bits.last_flush) //hit or miss dirty, sourceD don't care if dirty when hit
  dir_result_buffer.io.enq.bits:=directory.io.result.bits

  // lud-002/srad Bug C 修(B-2): flush 写回优先于 refill(schedule.d) 占 sourceD。
  //   drain dive 实证: 原 refill 结构性优先(deq.ready 仅 !schedule.d.valid 时拉高)让脏 victim 撞 refill 占 buf 那拍即丢
  //   (3 drops 全 = refill 占 buf + sourceD 反压同拍), 且是 approach-P kmeans livelock(refill 饿死 flush drain)的根。
  //   flush 队头时强占 sourceD; 同拍 gate 掉 schedule.d 的消费(requests.io.pop.valid 下方 last-connect 覆盖 L251 原值)避免误 pop MSHR。
  flush_wb_priority := dir_result_buffer.io.deq.valid && dir_result_buffer.io.deq.bits.flush
  val take_dir = flush_wb_priority || !schedule.d.valid
  requests.io.pop.valid := requests.io.valid(mshr_select) && schedule.d.valid && sourceD.io.req.ready && !flush_wb_priority && !selectedEvictReadPending  // srad-007 Bug3 fix(B): evict 读未完成不 pop(last-connect 有效赋值, 必须带 gate)

  dir_result_buffer.io.deq.ready:= take_dir && sourceD.io.req.ready


  // bfs4096-002 iter5 fix #1: directory.result fork 点反压补齐（Mux 改 needPush/needEnq AND）。
  // 原 Mux(hit, enq.ready, push.ready) 在 miss+dirty / flush+last_flush / flush+dirty
  // 路径漏 enq.ready：dir_result_buffer 满 + 新 dirty victim 时 result.fire=1 但
  // enq.fire=0，evict event 静默丢失 → SourceD 永不写回 mem → 后续 GetBlock 拿 stale
  // (cacheline 0x90000200 9 byte mismatch)。改 AND 后 fork 必须双 ready。
  // 配套：#2 (Directory_test.scala status_reg → fire) + #3 (push.valid → fire)。
  // 详见 bugs/bfs4096-002/checkpoint_3_iter5.md。
  val needPush = !directory.io.result.bits.hit && !directory.io.result.bits.flush
  val needEnq  = directory.io.result.bits.hit ||
                 directory.io.result.bits.dirty ||
                 directory.io.result.bits.last_flush
  // ListBuffer capacity and MSHR ownership are separate resources. A primary
  // miss needs both: push.ready alone can remain high when all MSHRs are owned
  // because the request list has secondary entries beyond the MSHR count.
  // Hold that result until an MSHR is free; a secondary miss can still merge
  // into its matching owner without consuming a new MSHR.
  val primaryMissCanTrack = !needPush || !alloc || mshr_free
  directory.io.result.ready := (!needPush || requests.io.push.ready) &&
                               (!needEnq  || dir_result_buffer.io.enq.ready) &&
                               primaryMissCanTrack

  when(directory.io.result.fire && needPush && alloc) {
    assert(mshr_insertOH.orR,
           "primary L2 miss retired without a free MSHR owner")
  }

  // A directory sweep must not overlap responses which still reference a
  // BankedStore way. In particular, mshr_empty alone does not cover L2 hits:
  // hit results can still reside in dir_result_buffer or SourceD after the
  // request list has drained. Starting invalidate in that window can clear or
  // reuse a way before its AccessAckData is returned to L1.
  val mshrScheduleBusy = VecInit(mshrs.map { m =>
    m.io.schedule.a.valid || m.io.schedule.d.valid || m.io.schedule.dir.valid
  }).asUInt.orR
  val maintenanceDrained = mshr_empty &&
    !mshrScheduleBusy && !evictReadPending.asUInt.orR &&
    directory.io.ready && !directory.io.result.valid &&
    !dir_result_buffer.io.deq.valid &&
    sourceD.io.req.ready && !sourceD.io.req.valid &&
    !sourceD.io.d.valid && !sourceD.io.a.valid &&
    !write_buffer.io.deq.valid && !sourceA.io.req.valid &&
    !io.out_a.valid && !wtValid.asUInt.orR

  flush_ready := !issue_flush_invalidate && maintenanceDrained
  invalidate_ready := !issue_flush_invalidate && maintenanceDrained

  when(request.fire && maintenanceRequest) {
    // SinkA admits Hint only while its put buffer is empty. Keep that
    // upstream contract as an assertion instead of feeding io.empty back
    // into request.ready, which would form a combinational cycle.
    assert(maintenanceDrained && putbuffer_empty,
           "L2 maintenance request accepted before prior traffic drained")
  }


  val full_mask = FillInterleaved(params.micro.writeBytes * 8, requests.io.data.mask)
  val merge_data = (requests.io.data.data & full_mask) | (schedule.d.bits.data & (~full_mask).asUInt)
  // lud-002/srad Bug C 修(B-2): 下列 sourceD fork 的选择键 !schedule.d.valid 全改 take_dir
  //   (= flush 队头优先 || !schedule.d.valid), 使 flush 写回优先占 sourceD; 非 flush 时退化为原 refill 优先语义。
  sourceD.io.req.bits.way:=Mux(take_dir ,dir_result_buffer.io.deq.bits.way,schedule.d.bits.way)
  sourceD.io.req.bits.data:=Mux(take_dir ,dir_result_buffer.io.deq.bits.data,Mux((requests.io.data.opcode===PutPartialData)||(requests.io.data.opcode===PutFullData),merge_data,schedule.d.bits.data))
  sourceD.io.req.bits.from_mem:=Mux(take_dir ,false.B,true.B)
  sourceD.io.req.bits.hit:=Mux(take_dir ,dir_result_buffer.io.deq.bits.hit,schedule.d.bits.hit)
  sourceD.io.req.bits.set:=Mux(take_dir ,dir_result_buffer.io.deq.bits.set,schedule.d.bits.set)
  sourceD.io.req.bits.tag:=Mux(take_dir ,Mux(!dir_result_buffer.io.deq.bits.hit,dir_result_buffer.io.deq.bits.victim_tag,dir_result_buffer.io.deq.bits.tag),schedule.d.bits.tag)
  sourceD.io.req.bits.mask:=Mux(take_dir ,dir_result_buffer.io.deq.bits.mask,requests.io.data.mask)
  sourceD.io.req.bits.offset:=Mux(take_dir ,Mux(!dir_result_buffer.io.deq.bits.hit,0.U,dir_result_buffer.io.deq.bits.offset),schedule.d.bits.offset)
  sourceD.io.req.bits.opcode:=Mux(take_dir ,dir_result_buffer.io.deq.bits.opcode,requests.io.data.opcode)
  sourceD.io.req.bits.put:=Mux(take_dir ,dir_result_buffer.io.deq.bits.put,requests.io.data.put)
  sourceD.io.req.bits.size:=Mux(take_dir ,dir_result_buffer.io.deq.bits.size,schedule.d.bits.size)
  sourceD.io.req.valid:=Mux(take_dir ,dir_result_buffer.io.deq.valid,schedule.d.valid)
  sourceD.io.req.bits.source:=Mux(take_dir,dir_result_buffer.io.deq.bits.source,requests.io.data.source) //pop the source of subentry
  sourceD.io.req.bits.last_flush:= Mux(take_dir ,dir_result_buffer.io.deq.bits.last_flush,schedule.d.bits.last_flush)
  sourceD.io.req.bits.flush:= Mux(take_dir ,dir_result_buffer.io.deq.bits.flush,schedule.d.bits.flush)
  sourceD.io.req.bits.dirty :=Mux(take_dir ,dir_result_buffer.io.deq.bits.dirty,schedule.d.bits.dirty)
  sourceD.io.req.bits.param :=Mux(take_dir ,dir_result_buffer.io.deq.bits.param,schedule.d.bits.param)
  sourceD.io.req.bits.l2cidx :=Mux(take_dir ,dir_result_buffer.io.deq.bits.l2cidx,schedule.d.bits.l2cidx)
  sourceD.io.req.bits.spike_info.foreach( _ := DontCare )
  // Directory and BankedStore are one refill commit. In addition to waiting
  // for a dirty-victim read, do not overwrite a way until every hit which
  // references its current contents has returned through SourceD.
  bankedStore.io.sinkD_adr.valid := directory.io.write.fire
  bankedStore.io.sinkD_adr.bits.set := schedule.dir.bits.set
  bankedStore.io.sinkD_adr.bits.way := schedule.dir.bits.way
  bankedStore.io.sinkD_adr.bits.mask:= ~(0.U(params.mask_bits.W))

  // btree-002 fix: refill install 到 L2 BankedStore 前同步 merge pending write-through bytes。
  // schedule.dir.bits 只有 set/way/tag，l2cidx 从同一个 selected MSHR 的 schedule.d.bits 取。
  val wtBankedStoreFillData = wtMergeLine(schedule.d.bits.tag, schedule.d.bits.l2cidx, schedule.d.bits.set, schedule.data)
  bankedStore.io.sinkD_dat.data := wtBankedStoreFillData
  bankedStore.io.sourceD_radr <> sourceD.io.bs_radr
  bankedStore.io.sourceD_wadr <> sourceD.io.bs_wadr
  bankedStore.io.sourceD_wdat := sourceD.io.bs_wdat
  sourceD.io.bs_rdat := bankedStore.io.sourceD_rdat

}

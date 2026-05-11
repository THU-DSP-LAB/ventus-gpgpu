package L1Cache.DCache

import L1Cache._
import chisel3._
import chisel3.util._
import mmu.SV32.paLen
import top.parameters._

class WSHRreq extends Bundle{
  val blockAddr = UInt((paLen - dcache_BlockOffsetBits - dcache_WordOffsetBits).W)//UInt((dcache_SetIdxBits+dcache_TagBits).W)
}
class WSHRCheckResult(Depth:Int) extends Bundle{
  val Hit = Bool()
  val HitIdx = UInt(log2Up(Depth).W)
}
class DCacheWSHR(Depth:Int) extends Module{
  val io = IO(new Bundle{
    //push
    val pushReq = Flipped(DecoupledIO(new WSHRreq))
    val conflict = Output(Bool())
    val pushedIdx = Output(UInt(log2Up(Depth).W))
    //for invOrFlu
    val empty = Output(Bool())
    val usedEntries = Output(UInt((log2Up(Depth)+1).W))
    //pop
    val popReq = Flipped(ValidIO(UInt(log2Up(Depth).W)))
    //check
    val checkReq = Input(new WSHRreq)
    val checkresult = Output(new WSHRCheckResult(Depth))

  })
 // assert(!(io.pushReq.valid && io.popReq.valid),"WSHR cant pop and push in same cycle")

  val blockAddrEntries = RegInit(VecInit(Seq.fill(Depth)(0.U((paLen - dcache_BlockOffsetBits - dcache_WordOffsetBits).W))))
  val valid: Vec[Bool] = RegInit(VecInit(Seq.fill(Depth)(false.B)))
  io.empty := !valid.reduceTree(_|_)
  io.usedEntries := PopCount(valid)

  //following circuit are same to L1TagAccess.scala tagChecker
  val pushMatchMask = Wire(UInt(Depth.W))
  pushMatchMask := Reverse(Cat(blockAddrEntries.zip(valid).map{
    case(bA,valid) => (bA === io.pushReq.bits.blockAddr) && valid}))
  assert(PopCount(pushMatchMask) <= 1.U,"WSHR dont store same bA in 2 entries")
  io.conflict := pushMatchMask.orR
  //check entry
  val checkMatchPush = (io.pushReq.bits.blockAddr === io.checkReq.blockAddr) && io.pushReq.valid
  val checkMatchMask = Wire(UInt(Depth.W))
  checkMatchMask := Reverse(Cat(blockAddrEntries.zip(valid).map{
    case(bA,valid) => (bA === io.checkReq.blockAddr) && valid}))
  val checkmatchIdx = OHToUInt(checkMatchMask)
  val checkMatchPop = (checkmatchIdx === io.popReq.bits) && io.popReq.valid
  io.checkresult.Hit := (checkMatchMask.orR && !checkMatchPop) || checkMatchPush
  io.checkresult.HitIdx := Mux(checkMatchPush, io.pushedIdx, checkmatchIdx)

  //pushReq.ready := !full
  io.pushReq.ready := !valid.reduceTree(_ & _)

  val nextEntryIdx = valid.indexWhere((x: Bool) => x === false.B)
  val PopPushInSameCycle = io.pushReq.fire && io.popReq.fire
  io.pushedIdx := Mux(PopPushInSameCycle,io.popReq.bits,nextEntryIdx)

  when(io.pushReq.fire && io.popReq.fire){
    blockAddrEntries(io.popReq.bits) := io.pushReq.bits.blockAddr
    valid(io.popReq.bits) := true.B
  }.elsewhen(io.pushReq.fire){
    blockAddrEntries(nextEntryIdx) := io.pushReq.bits.blockAddr
    valid(nextEntryIdx) := true.B
  }.elsewhen(io.popReq.valid){
    valid(io.popReq.bits) := false.B
  }

  // Debug counter: peak WSHR occupancy seen during the run. Surfaced via
  // dontTouch so verilator emits it to the FST trace and post-run analysis
  // can read it directly (without rebuilding the design). Used to validate
  // that the Depth budget is sufficient -- if `wshrMaxUsed == Depth` on a
  // workload, the WSHR is saturating and the livelock documented in
  // bugs/bfs4096-003/phase_4_report.md may re-emerge.
  val wshrMaxUsed = RegInit(0.U(log2Ceil(Depth + 1).W))
  when(io.usedEntries > wshrMaxUsed){ wshrMaxUsed := io.usedEntries }
  dontTouch(wshrMaxUsed)
}


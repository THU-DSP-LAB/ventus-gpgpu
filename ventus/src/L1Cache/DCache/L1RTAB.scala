package L1Cache.DCache

import L1Cache._
import chisel3._
import chisel3.util._
import config.config.Parameters

class RTABReq (implicit p: Parameters) extends DCacheBundle{
  val ReqType = UInt(4.W)
  val CoreReqData = new DCacheCoreReq
  val wshrIdx = UInt(log2Up(NWshrEntry).W)
  val mshrIdx = UInt(log2Up(NMshrEntry).W)
  // bfs4096-009 fix: ReadMissFillWait enq 时本 block 的 fill 是否已 commit
  // (覆盖 enq 同拍 / pre-enq=RTAB_full 期间 commit 这两种 edge，否则 entry 入表后 watch 不到唯一 pulse)
  val fillAlreadyCommitted = Bool()
}

class RTABUpdate (implicit p: Parameters) extends DCacheBundle{
  val blockAddr = UInt(bABits.W)
  val wshrIdx = UInt(log2Up(NWshrEntry).W)
  val mshrIdx = UInt(log2Up(NMshrEntry).W)
  val updateType = UInt(2.W) //0-from vector MSHR, 1- from WSHR, 2-from special MSHR
}
class WSHRIdxUpdate (implicit p: Parameters) extends DCacheBundle{
  val wshrIdx = UInt(log2Up(NWshrEntry).W)
  val RTABIdx = UInt(log2Up(NRTABs).W)
}

class L1RTAB(implicit p: Parameters) extends DCacheModule {
  val io = IO(new Bundle {
    //to coreReq_pipe0
    val coreReq_replay      = DecoupledIO(new DCacheCoreReq) // insert core Req to pipe line st0
    //to coreReq-io
    val RTAB_full           = Output(Bool())
    val RTAB_almost_full    = Output(Bool()) // can't take st0 request if there is st1 request
    //From coreReq_pipe1
    // write miss hit in MSHR:
    // include 1 - probestatus = 3, secondary full, read/write miss, hit in mshr, need to wait for missrspin
    //         2- probestatus = 2/3 write miss, hit in mshr
    val RTABReq_st1         = Flipped(ValidIO(new RTABReq)) // pipeline st1 req
    val RTABUpdate          =  Flipped(ValidIO(new RTABUpdate))
    val RTABReq_st0         = Flipped(ValidIO(new RTABReq)) // pipeline st0 req, for RTAB hit check
    // read miss hit in WSHR (NoC may cause MC problem)
    // to coreReqst1
    val checkRTABhit        = Output(Bool())
    val mshrFull            = Input(Bool())
    val LRexist             = Input(Bool())
    // bfs4096-006 fix: fill 写 dA 的 valid (来自 memRspPipe.io.dAmemRsp_wReq_valid)。
    // fillConflict 类型的 replay 必须等 fill 完才 inject (refillWrite_valid 0→0 稳定后)，
    // 否则下一拍 fill 余拍仍在写 → 又触发 fillConflictSt1 → 又 enq RTAB → livelock。
    val refillWrite_valid   = Input(Bool())
    // bfs4096-009 fix: 真实 fill commit (=memRspPipe.dAmemRsp_wReq_valid，含 st1_ready，非 intent) + blockAddr，
    // 给 ReadMissFillWait per-entry sticky fillCommitted 置位。只进 Reg 不参与 inject 组合放行 → loop-free。
    val fillCommit_valid    = Input(Bool())
    val fillCommit_blockAddr= Input(UInt(bABits.W))
    val RTABpushedIdx       = Output(UInt(log2Up(NRTABs).W))
    val pushedWSHRIdxUpdate = Flipped(ValidIO(new WSHRIdxUpdate))
  })
  val Req_access = Reg(Vec(NRTABs, new DCacheCoreReq))
  val Replay_type = RegInit(VecInit(Seq.fill(NRTABs)(0.U(4.W))))
  val wshr_idx   = RegInit(VecInit(Seq.fill(NRTABs)(0.U(log2Up(NWshrEntry).W))))
  val mshr_idx   = RegInit(VecInit(Seq.fill(NRTABs)(0.U(log2Up(NMshrEntry).W))))
  val RTABlink_idx = RegInit(VecInit(Seq.fill(NRTABs)(0.U((log2Up(NRTABs)+1).W)))) //highest bit for valid
  val EntryValid = RegInit(VecInit(Seq.fill(NRTABs)(false.B)))
  // bfs4096-009 fix: ReadMissFillWait entry 等的 block 是否已 commit。enq 按 fillAlreadyCommitted 初始化，
  // 入表后再 per-entry 地址匹配 watch 后续 commit pulse。inject 只看本 Reg(loop-free)。
  val fillCommitted = RegInit(VecInit(Seq.fill(NRTABs)(false.B)))
  val ptr = RegInit(0.U(log2Up(NRTABs).W))
  val ptr_r = RegInit(0.U(log2Up(NRTABs).W))// when update and request is for same 
  //val seq_Q = Module(new Queue(UInt(log2Up(NRTABs).W),NRTABs,false,false)) // hold the new ptr idx for pop req
  io.RTAB_full := (EntryValid.reduce(_ & _))
  io.RTAB_almost_full := PopCount(EntryValid) === (NRTABs-1).U
  val ptr_w = Wire(UInt(log2Up(NRTABs).W))
  ptr_w := ptr//PriorityEncoder(Reverse(Cat(EntryValid.map(!_))))
  io.RTABpushedIdx := ptr_w
  val ptr_w_2 = Wire(UInt(log2Up(NRTABs).W)) // when need to fill 2 entry in one cycle
  ptr_w_2 := ptr + 1.U//PriorityEncoder(Reverse(Cat(EntryValid.map(!_))) ^ ptr_wOH)
  val ptrEnqValid = Wire(Bool())

  val blockAddrMatchInRTAB = Wire(Vec(NRTABs, Bool()))
  val mshrIdxMatch  =  Wire(Vec(NRTABs, Bool()))
  val wshrIdxMatch  =  Wire(Vec(NRTABs, Bool()))

//st0 req hit in RTAB Entry
  for(i<-0 until NRTABs){
    blockAddrMatchInRTAB(i) := (Cat(Req_access(i).tag,Req_access(i).setIdx) === Cat(io.RTABReq_st0.bits.CoreReqData.tag,io.RTABReq_st0.bits.CoreReqData.setIdx))  &&
      EntryValid(i) && (RTABlink_idx(i).head(1) === 0.U)
      //(Req_access(i).tag === io.coreReq_st1.bits.tag) && (Req_access(i).setIdx === io.coreReq_st1.bits.setIdx) &&  EntryValid(i)
  }
  val bAMatch_st0 = Cat(blockAddrMatchInRTAB).orR // st0 request match in RTAB
  val bAMatch_st1 = (Cat(io.RTABReq_st0.bits.CoreReqData.tag,io.RTABReq_st0.bits.CoreReqData.setIdx) === Cat(io.RTABReq_st1.bits.CoreReqData.tag,io.RTABReq_st1.bits.CoreReqData.setIdx)) &&
    io.RTABReq_st1.valid // st0 request match in st1 request
  val bAMatchIdx = NRTABs.asUInt - 1.U - PriorityEncoder(Cat(blockAddrMatchInRTAB))

  io.checkRTABhit := bAMatch_st0 || bAMatch_st1//will ready st0, but not valid st1 <- request will be in RTAB


  ptrEnqValid := false.B
  // bfs4096-009 fix: st1 enq 写 ptr_w 那条 entry 的 fillCommitted 初值 ——
  // ReadMissFillWait 用入表时的 fillAlreadyCommitted(覆盖同拍/pre-enq commit)，其它 type 清 0。
  val enqFillCommitted_init = Mux(io.RTABReq_st1.valid && io.RTABReq_st1.bits.ReqType === ReadMissFillWait,
                                  io.RTABReq_st1.bits.fillAlreadyCommitted, false.B)
  // RTAB push req st1
  when(io.RTABReq_st1.valid && !io.RTABReq_st0.valid){ //st1 request but no st0 hit
    Req_access(ptr_w) := io.RTABReq_st1.bits.CoreReqData
    Replay_type(ptr_w) := io.RTABReq_st1.bits.ReqType
    wshr_idx(ptr_w) := io.RTABReq_st1.bits.wshrIdx
    mshr_idx(ptr_w) := io.RTABReq_st1.bits.mshrIdx
    EntryValid(ptr_w) := true.B
    fillCommitted(ptr_w) := enqFillCommitted_init   // bfs4096-009: st1 enq 写 ptr_w → 按 RMFW? 初始化(覆盖三入队分支)
    ptr := ptr_w + 1.U
  }.elsewhen(io.RTABReq_st1.valid && (bAMatch_st0) ){ //st1 request and st0 hit in RTAB entry
    //st1
    Req_access(ptr_w) := io.RTABReq_st1.bits.CoreReqData
    Replay_type(ptr_w) := io.RTABReq_st1.bits.ReqType
    wshr_idx(ptr_w) := io.RTABReq_st1.bits.wshrIdx
    mshr_idx(ptr_w) := io.RTABReq_st1.bits.mshrIdx
    EntryValid(ptr_w) := true.B
    fillCommitted(ptr_w) := enqFillCommitted_init   // bfs4096-009: st1 enq 写 ptr_w → 按 RMFW? 初始化(覆盖三入队分支)
    ptr := ptr_w_2 + 1.U
    //st0
    Req_access(ptr_w_2) := io.RTABReq_st0.bits.CoreReqData
    Replay_type(ptr_w_2) := hitRTAB
    wshr_idx(ptr_w_2) := DontCare
    mshr_idx(ptr_w_2) := DontCare
    RTABlink_idx(bAMatchIdx) := Cat(1.U,ptr_w_2)
    EntryValid(ptr_w_2) := true.B
    fillCommitted(ptr_w_2) := false.B   // bfs4096-009: ptr_w_2 恒为 st0 hitRTAB link entry，非 RMFW，清 0
  }.elsewhen(io.RTABReq_st1.valid && (bAMatch_st1) && io.RTABReq_st0.valid){ //st1 request and st0 hit in st1 req
    Req_access(ptr_w) := io.RTABReq_st1.bits.CoreReqData
    Replay_type(ptr_w) := io.RTABReq_st1.bits.ReqType
    wshr_idx(ptr_w) := io.RTABReq_st1.bits.wshrIdx
    mshr_idx(ptr_w) := io.RTABReq_st1.bits.mshrIdx
    EntryValid(ptr_w) := true.B
    fillCommitted(ptr_w) := enqFillCommitted_init   // bfs4096-009: st1 enq 写 ptr_w → 按 RMFW? 初始化(覆盖三入队分支)
    ptr := ptr_w_2 + 1.U
    //st0
    Req_access(ptr_w_2) := io.RTABReq_st0.bits.CoreReqData
    Replay_type(ptr_w_2) := hitRTAB
    wshr_idx(ptr_w_2) := DontCare
    mshr_idx(ptr_w_2) := DontCare
    RTABlink_idx(ptr_w) := Cat(1.U,ptr_w_2)
    EntryValid(ptr_w_2) := true.B
    fillCommitted(ptr_w_2) := false.B   // bfs4096-009: ptr_w_2 恒为 st0 hitRTAB link entry，非 RMFW，清 0
  }.elsewhen(bAMatch_st0 && io.RTABReq_st0.valid){ // only st0 request
    Req_access(ptr_w) := io.RTABReq_st0.bits.CoreReqData
    Replay_type(ptr_w) := hitRTAB
    wshr_idx(ptr_w) := DontCare
    mshr_idx(ptr_w) := DontCare
    RTABlink_idx(bAMatchIdx) := Cat(1.U,ptr_w)
    EntryValid(ptr_w) := true.B
    fillCommitted(ptr_w) := enqFillCommitted_init   // bfs4096-009: st1 enq 写 ptr_w → 按 RMFW? 初始化(覆盖三入队分支)
    ptr := ptr_w + 1.U
  }

  //update wshrIdx when Replay type is Ucached hit dirty
  when(io.pushedWSHRIdxUpdate.valid){
    wshr_idx(io.pushedWSHRIdxUpdate.bits.RTABIdx) := io.pushedWSHRIdxUpdate.bits.wshrIdx
  }

  //update match idx
  for(i<-0 until NRTABs){
    when((mshr_idx(i) === io.RTABUpdate.bits.mshrIdx) && ((((Replay_type(i) === SubEntryFull) || (Replay_type(i) === writeMissHitMSHR)) && (io.RTABUpdate.bits.updateType === 0.U)) ||
      ((Replay_type(i)===HitSMSHR) && (io.RTABUpdate.bits.updateType === 2.U)))){
      mshrIdxMatch(i) := true.B
    }.otherwise{
      mshrIdxMatch(i) := false.B
    }
    when((wshr_idx(i) === io.RTABUpdate.bits.wshrIdx) && (((Replay_type(i) === UCacheHitDirty) || (Replay_type(i) === readHitWSHR)||
      (Replay_type(i) === writeMissHitWSHR)) && (io.RTABUpdate.bits.updateType === 1.U))){
      wshrIdxMatch(i) := true.B
    }.otherwise{
      wshrIdxMatch(i) := false.B
    }
  }


  // RTAB Update Req
  for(i<-0 until NRTABs){
    when(io.RTABUpdate.valid){
      when(mshrIdxMatch(i)){
        mshr_idx(i) := 0.U
        Replay_type(i) := 0.U
      }.elsewhen(wshrIdxMatch(i)){
        wshr_idx(i) := 0.U
        Replay_type(i) := 0.U
      }
    }
  }
  //replay request
  val popPtr = ptr_r
  val LinkPtr = Wire(UInt(log2Up(NRTABs).W))
  LinkPtr := RTABlink_idx(popPtr)(log2Up(NRTABs)-1,0)
  //val popReqAllClear = !(hit_mshr(popPtr) || hit_wshr(popPtr) || hit_RTAB(popPtr) || mshr_full(popPtr)) && EntryValid(popPtr)
  when(io.coreReq_replay.fire){
    ptr_r := ptr_r+1.U
    EntryValid(popPtr) := false.B
    when(RTABlink_idx(popPtr).head(1) === 1.U){
      Replay_type(LinkPtr) := 0.U
      RTABlink_idx(popPtr) := 0.U
    }

  }
  // bfs4096-009 fix: 入表后 per-entry watch 本 block fill commit pulse(真实 commit）。
  // guard !(本拍 enq 到该 entry)：本拍 enq 的 entry 由 enqFillCommitted_init 负责，避免对同一 fillCommitted(i)
  // last-connect 双写歧义(codex round6 §B 要求 guard 必须 per-entry，不能全局屏蔽本拍所有 watch)。
  for(i <- 0 until NRTABs){
    when(io.fillCommit_valid && EntryValid(i) && (Replay_type(i) === ReadMissFillWait) &&
         (io.fillCommit_blockAddr === Cat(Req_access(i).tag, Req_access(i).setIdx)) &&
         !(io.RTABReq_st1.valid && (ptr_w === i.U))){
      fillCommitted(i) := true.B
    }
  }
  val injectCoreReq_valid = Wire(Bool())
  when(Replay_type(popPtr) === 0.U && EntryValid(popPtr)){
    injectCoreReq_valid := true.B
  }.elsewhen(Replay_type(popPtr) === EntryFull && EntryValid(popPtr) && !io.mshrFull){
    injectCoreReq_valid := true.B
  }.elsewhen(Replay_type(popPtr) === SCLRexist && EntryValid(popPtr) && !io.LRexist){
    injectCoreReq_valid := true.B
  }.elsewhen(Replay_type(popPtr) === fillConflict && EntryValid(popPtr) && !io.refillWrite_valid){
    // bfs4096-006 fix: 等 fill 完 (refillWrite_valid=0) 才 replay。fill 完后 tag SRAM 已 commit 新 tag,
    // cost req replay 在 ST1 会 tag miss → 走 MSHR 重新 fetch cost cacheline，落到 LRU 选的另一 way。
    injectCoreReq_valid := true.B
  }.elsewhen(Replay_type(popPtr) === ReadMissFillWait && EntryValid(popPtr) && fillCommitted(popPtr)){
    // bfs4096-009 fix: 本 block fill 已 commit(fillCommitted Reg)→ 放出去 re-probe 必 tag hit。
    // 只看 Reg 旧值，不把 io.fillCommit_valid 组合并入 injectCoreReq_valid → loop-free(codex round6 §B)。
    injectCoreReq_valid := true.B
  }.otherwise{
    injectCoreReq_valid := false.B
  }
  io.coreReq_replay.valid := injectCoreReq_valid
  io.coreReq_replay.bits := Req_access(popPtr)

}
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
package L1Cache.DCache

import L1Cache.DCache.DCacheParameters._
import L1Cache._
import SRAMTemplate._
import chisel3._
import chisel3.util._
import config.config.Parameters
//import pipeline.parameters._

class VecMshrTargetInfo(implicit p: Parameters)extends DCacheBundle{
  val instrId = UInt(WIdBits.W)
  //val isWrite = Bool()
  val perLaneAddr = Vec(NLanes, new DCachePerLaneAddr)
}

class DCacheControl extends Bundle{
  val isRead = Bool()
  val isWrite = Bool()
  val isLR = Bool()
  val isSC = Bool()
  val isAMO = Bool()
  val isFlush = Bool()
  val isInvalidate = Bool()
}

class genControl extends Module{
  val io = IO(new Bundle{
    val opcode = Input(UInt(3.W))
    val param = Input(UInt(4.W))
    val control = Output(new DCacheControl())
  })
  io.control.isRead := false.B
  io.control.isWrite := false.B
  io.control.isLR := false.B
  io.control.isSC := false.B
  io.control.isAMO := false.B
  io.control.isFlush := false.B
  io.control.isInvalidate := false.B

  switch(io.opcode){
    is(0.U){
      when(io.param === 0.U){
        io.control.isRead := true.B
      }.elsewhen(io.param === 1.U) {
        io.control.isLR := true.B
      }
    }
    is(1.U){
      when(io.param === 0.U) {
        io.control.isWrite := true.B
      }.elsewhen(io.param === 1.U) {
        io.control.isSC := true.B
      }
    }
    is(2.U){
      io.control.isAMO := true.B
    }
    is(3.U){
      when(io.param === 0.U) {
        io.control.isInvalidate := true.B
      }.elsewhen(io.param === 1.U) {
        io.control.isFlush := true.B
      }
    }
  }
}

class DataCache(implicit p: Parameters) extends DCacheModule{
  val io = IO(new Bundle{
    val coreReq = Flipped(DecoupledIO(new DCacheCoreReq))
    val coreRsp = DecoupledIO(new DCacheCoreRsp)
    val memRsp = Flipped(DecoupledIO(new DCacheMemRsp))
    val memReq = DecoupledIO(new DCacheMemReq)})

  // ******     important submodules     ******
  val MshrAccess = Module(new MSHR(bABits = bABits, tIWidth = tIBits, WIdBits = WIdBits, NMshrEntry, NMshrSubEntry))
  //val missRspFromMshr_st1 = Wire(Bool())
  val missRspTI_st1 = Wire(new VecMshrTargetInfo)
  val TagAccess = Module(new L1TagAccess(set=NSets, way=NWays, tagBits=TagBits,readOnly=false))
  //val DataCrsCore2Mem = Module(new DataCrossbar(num_thread,BlockWords))
  //val DataCrsMem2Core = Module(new DataCrossbar(BlockWords,num_thread))

  // ******     queues     ******
  val coreRsp_Q_entries :Int = NLanes
  val coreRsp_Q = Module(new Queue(new DCacheCoreRsp,entries = coreRsp_Q_entries,flow=false,pipe=false))
  //this queue also work as a pipeline reg, so cannot flow
  //val coreRsp_QAlmstFull = Wire(Bool())
  val memRsp_Q = Module(new Queue(new DCacheMemRsp,entries = 2,flow=false,pipe=false))
  //flow will make predecessor read hit conflict with successor memRsp
  val memRsp_Q_st0 = memRsp_Q.io.deq.bits

  val memReq_Q = Module(new Queue(new DCacheMemReq,entries = 8,flow=false,pipe=false))
  val MemReqArb = Module(new Arbiter(new DCacheMemReq, 2))

  // ******     pipeline regs      ******
  val coreReq_st1 = RegEnable(io.coreReq.bits, io.coreReq.fire)
  val coreReq_st1_ready = Wire(Bool())
  val coreReq_st1_valid_pre = RegInit(false.B)
  val coreReq_st1_valid = Wire(Bool())
  val memRsp_st1_valid = RegInit(false.B)//early definition
  val coreReq_st1_fire = coreReq_st1_ready && coreReq_st1_valid_pre
  //is a 1-bit 2-status FSM
  when(io.coreReq.fire ^ coreReq_st1_fire){
    coreReq_st1_valid_pre := io.coreReq.fire
  }
  coreReq_st1_valid := coreReq_st1_valid_pre && !memRsp_st1_valid
  val coreReqControl_st0 = Wire(new DCacheControl)
  val coreReqControl_st1: DCacheControl = RegEnable(coreReqControl_st0, io.coreReq.fire)
  val cacheHit_st1 = Wire(Bool())
  cacheHit_st1 := TagAccess.io.hit_st1 && RegNext(io.coreReq.fire)
  val cacheMiss_st1 = !TagAccess.io.hit_st1 && RegNext(io.coreReq.fire)

  val readHit_st1 = cacheHit_st1 & coreReqControl_st1.isRead
  val readMiss_st1 = cacheMiss_st1 & coreReqControl_st1.isRead
  val writeHit_st1 = cacheHit_st1 & coreReqControl_st1.isWrite
  val writeMiss_st1 = cacheMiss_st1 & coreReqControl_st1.isWrite

  val coreRsp_st2 =Reg(new DCacheCoreRsp)
  val coreRsp_st2_valid =Wire(Bool())

  /*val cacheHit_st2 = RegInit(false.B)
  cacheHit_st2 := cacheHit_st1 || (cacheHit_st2 && RegNext(BankConfArb.io.bankConflict))*/
  // bankConflict置高的周期比coreRsp需要输出的周期少一个，而其置高的第一个周期有cacheHit_st1做控制。所以这里使用RegNext(bankConflict)做控制
  val readHit_st2 = RegNext(readHit_st1)
  //val readHit_st3 = RegNext(readHit_st2)
  //val writeHit_st2 = RegNext(writeHit_st1)//cacheHit_st2 && coreReq_st2.isWrite
  //val writeHit_st3 = RegNext(writeHit_st2)
  //val arbArrayEn_st2 = RegNext(BankConfArb.io.dataArrayEn)

  // ******      l1_data_cache::coreReq_pipe1_cycle      ******
  // ******      tag probe      ******
  //val missRspWriteEnable = Wire(Bool())
  TagAccess.io.probeRead.valid := io.coreReq.fire
  TagAccess.io.probeRead.bits.setIdx := io.coreReq.bits.setIdx
  TagAccess.io.tagFromCore_st1 := coreReq_st1.tag

  // ******      mshr probe      ******
  MshrAccess.io.probe.valid := io.coreReq.fire
  MshrAccess.io.probe.bits.blockAddr := Cat(io.coreReq.bits.tag,io.coreReq.bits.setIdx)
  val mshrProbeAvail = MshrAccess.io.probeOut_st1.probeStatus === 0.U || MshrAccess.io.probeOut_st1.probeStatus === 2.U

  val genCtrl = Module(new genControl)
  genCtrl.io.opcode := io.coreReq.bits.opcode
  genCtrl.io.param := io.coreReq.bits.param
  coreReqControl_st0 := genCtrl.io.control

  io.coreReq.ready := coreReq_st1_ready

  // ******      l1_data_cache::coreReq_pipe2_cycle      ******
  TagAccess.io.probeIsWrite_st1.get := writeHit_st1

  // ******      mshr missReq      ******
  MshrAccess.io.missReq.valid := readMiss_st1
  val mshrMissReqTI = Wire(new VecMshrTargetInfo)
  //mshrMissReqTI.isWrite := coreReqControl_st1.isWrite
  mshrMissReqTI.instrId := coreReq_st1.instrId
  mshrMissReqTI.perLaneAddr := coreReq_st1.perLaneAddr
  MshrAccess.io.missReq.bits.instrId := coreReq_st1.instrId
  MshrAccess.io.missReq.bits.blockAddr := Cat(coreReq_st1.tag, coreReq_st1.setIdx)
  MshrAccess.io.missReq.bits.targetInfo := mshrMissReqTI.asUInt

  // ******      memReq_Q enq      ******
  val missMemReq = Wire(new DCacheMemReq) //writeMiss_st1 || readMiss_st1
  val writeMissReq = Wire(new DCacheMemReq)
  val readMissReq = Wire(new DCacheMemReq)
  writeMissReq.a_opcode := 1.U //PutPartialData:Get
  writeMissReq.a_param := 0.U //regular write
  writeMissReq.a_source := Cat("d0".U, coreReq_st1.instrId)
  writeMissReq.a_addr := Cat(coreReq_st1.tag, coreReq_st1.setIdx, 0.U((WordLength - TagBits - SetIdxBits).W))
  writeMissReq.a_mask := coreReq_st1.perLaneAddr.map(_.activeMask)
  writeMissReq.a_data := coreReq_st1.data

  readMissReq.a_opcode := 4.U //Get
  readMissReq.a_param := 0.U //regular read
  readMissReq.a_source := Cat("d1".U, MshrAccess.io.probeOut_st1.a_source, coreReq_st1.setIdx)//setIdx for memRsp tag access in 1st stage
  readMissReq.a_addr := Cat(coreReq_st1.tag, coreReq_st1.setIdx, 0.U((WordLength - TagBits - SetIdxBits).W))
  readMissReq.a_mask := coreReq_st1.perLaneAddr.map(_.activeMask)
  readMissReq.a_data := DontCare

  missMemReq := Mux(writeMiss_st1, writeMissReq, readMissReq)

  // ******      dataAccess bank enable     ******
  val getBankEn = Module(new getDataAccessBankEn(NBank = BlockWords, NLane = NLanes))
  getBankEn.io.perLaneBlockIdx := coreReq_st1.perLaneAddr.map(_.blockOffset)
  getBankEn.io.perLaneValid := coreReq_st1.perLaneAddr.map(_.activeMask)

  // ******      dataAccess write hit      ******
  val DataAccessWriteHitSRAMWReq: Vec[SRAMBundleAW[UInt]] = Wire(Vec(BlockWords,new SRAMBundleAW(UInt(8.W), NSets*NWays, BytesOfWord)))
  //this setIdx = setIdx + wayIdx
  DataAccessWriteHitSRAMWReq.foreach(_.setIdx := Cat(coreReq_st1.setIdx,TagAccess.io.waymaskHit_st1))
  for (i <- 0 until BlockWords){
    DataAccessWriteHitSRAMWReq(i).waymask.get := coreReq_st1.perLaneAddr(getBankEn.io.perBankBlockIdx(i)).wordOffset1H
    DataAccessWriteHitSRAMWReq(i).data := coreReq_st1.data(getBankEn.io.perBankBlockIdx(i)).asTypeOf(Vec(BytesOfWord,UInt(8.W)))//TODO check order
  }

  // ******      dataAccess read hit      ******
  val DataAccessReadHitSRAMRReq = Wire(Vec(BlockWords,new SRAMBundleA(NSets*NWays)))
  DataAccessReadHitSRAMRReq.foreach(_.setIdx := Cat(coreReq_st1.setIdx,TagAccess.io.waymaskHit_st1))

  coreReq_st1_ready := false.B
  when(coreReqControl_st1.isRead || coreReqControl_st1.isWrite){
    when(TagAccess.io.hit_st1) {
      when(coreRsp_Q.io.enq.ready) {
        coreReq_st1_ready := true.B
      }
    }.otherwise{
    //}.elsewhen(!TagAccess.io.hit_st1){
      //assert(cacheMiss_st1,s"when coreReq_st1 valid, hit/miss cant invalid in same cycle")
      when(coreReqControl_st1.isRead){
        //when(MshrAccess.io.probeOut_st1.probeStatus(0).asBool//PRIMARY_AVAIL|SECONDARY_AVAIL
        when(mshrProbeAvail && memReq_Q.io.enq.ready){
          coreReq_st1_ready := true.B
        }
      }.otherwise{//isWrite
        //TODO before 6.30: add hit in-flight miss
        when(coreRsp_Q.io.enq.ready && memReq_Q.io.enq.ready){
          coreReq_st1_ready := true.B
        }
      }
    }//.otherwise{//coreReq is not valid
    //  coreReq_st1_ready := true.B
    //}
  }.otherwise{
    coreReq_st1_ready := true.B
  }

  // ******     l1_data_cache::memRsp_pipe1_cycle      ******
  val memRsp_st1 = Reg(new DCacheMemRsp)
  //val memRsp_st1_valid = RegInit(false.B) early definition
  val memRsp_st1_ready = Wire(Bool())
  val memRsp_st1_fire = memRsp_st1_ready && memRsp_st1_valid
  val tagReqCurrentMissRspHasSent = RegInit(true.B)//before 7.30 TODO add logic for this
  //is a 1-bit 2-status FSM
  when(memRsp_Q.io.deq.fire ^ memRsp_st1_fire) {
    memRsp_st1_valid := memRsp_Q.io.deq.fire
  }

  memRsp_Q.io.enq <> io.memRsp
  memRsp_Q.io.deq.ready := memRsp_st1_ready//MshrAccess.io.missRspIn.ready && memRsp_st1_ready && TagAccess.io.allocateWrite.ready
  // && !cacheHit_st2 && !ShiftRegister(io.coreReq.bits.isWrite&&io.coreReq.fire(),2)
  when(memRsp_Q.io.deq.fire){
    memRsp_st1 := memRsp_Q_st0
  }
  //val memRspData_st1 = Wire(Vec(BlockWords,UInt(WordLength.W)))
  //val memRspData_st2 = Wire(Vec(BankWords,Vec(NBanks,UInt(WordLength.W))))
  /*(0 until BankWords).foreach{ iinB =>
    (0 until NBanks).foreach{iofB =>
      memRspData_st1(iinB)(iofB) := memRsp_Q_st1.d_data((iinB*NBanks+iofB).asUInt)
    }
  }*/
  //memRspData_st2 := RegEnable(memRspData_st1,memRsp_Q.io.deq.fire() || (memRsp_Q.io.deq.valid && BankConfArb.io.bankConflict))

  // ******     missRspIn      ******
  MshrAccess.io.missRspIn.valid := memRsp_Q.io.deq.valid// && !cacheHit_st2 && !ShiftRegister(io.coreReq.bits.isWrite&&io.coreReq.fire(),2)
  MshrAccess.io.missRspIn.bits.instrId := memRsp_Q.io.deq.bits.d_source(SetIdxBits+log2Up(NMshrEntry)-1,SetIdxBits)

  // ******      tag write      ******
  TagAccess.io.allocateWrite.valid := memRsp_Q.io.deq.valid && tagReqCurrentMissRspHasSent
  TagAccess.io.allocateWrite.bits.setIdx := memRsp_Q.io.deq.bits.d_source(SetIdxBits-1,0)
  //TagAccess.io.allocateWriteData_st1 to be connected in memRsp_pipe2_cycle

  // ******     l1_data_cache::memRsp_pipe2_cycle      ******
  //missRspFromMshr_st1 := MshrAccess.io.missRspOut.valid//suffix _st2 is on another path comparing to cacheHit
  missRspTI_st1 := MshrAccess.io.missRspOut.bits.targetInfo.asTypeOf(new VecMshrTargetInfo)
  val missRspBA_st1 = MshrAccess.io.missRspOut.bits.blockAddr
  //val missRspTILaneMask_st2 = RegNext(BankConfArb.io.activeLane)
  //val memRspInstrId_st1 = MshrAccess.io.missRspOut.bits.instrId
  //val readMissRsp_st2 = missRspFromMshr_st2 & !missRspTI.isWrite
  //val readMissRspCnter = if(BankOffsetBits!=0) RegInit(0.U((BankOffsetBits+1).W)) else Reg(UInt())
  //MshrAccess.io.missRspOut.ready := coreRsp_Q.io.enq.ready//TODO check

  TagAccess.io.allocateWriteData_st1 := get_tag(MshrAccess.io.missRspOut.bits.blockAddr)

  // ******      dataAccess missRsp      ******
  val DataAccessMissRspSRAMWReq: Vec[SRAMBundleAW[UInt]] = Wire(Vec(BlockWords, new SRAMBundleAW(UInt(8.W), NSets*NWays, BytesOfWord)))
  DataAccessMissRspSRAMWReq.foreach(_.setIdx := Cat(get_setIdx(missRspBA_st1),TagAccess.io.waymaskReplacement_st1))
  DataAccessMissRspSRAMWReq.foreach(_.waymask.get := Fill(BytesOfWord,true.B))
  for (i <- 0 until BlockWords) {
    DataAccessMissRspSRAMWReq(i).data := memRsp_st1.d_data(i).asTypeOf(Vec(BytesOfWord,UInt(8.W)))
  }

  memRsp_st1_ready := tagReqCurrentMissRspHasSent && MshrAccess.io.missRspIn.ready && coreRsp_Q.io.enq.ready

  //val mshrMissRspStrobe = !RegNext(MshrAccess.io.missRspOut.valid) |
   // RegNext(RegNext(MshrAccess.io.missRspOut.ready) && MshrAccess.io.missRspOut.valid)
  //val readMissRsp_st1 = (MshrAccess.io.missRspOut.fire() || (MshrAccess.io.missRspOut.valid && BankConfArb.io.bankConflict)) & !missRspTI_st1.isWrite
  //val writeMissRsp_st1 = MshrAccess.io.missRspOut.fire() & missRspTI_st1.isWrite
  //only on subword miss
  //val readMissRsp_st2 = RegNext(readMissRsp_st1)
  //val writeMissRsp_st2 = RegNext(writeMissRsp_st1)
  /*when(readMissRspCnter === (BankWords-1).asUInt &&
    (((!RegNext(missRspFromMshr_st1) && missRspFromMshr_st1) ||
    (RegNext(missRspBA_st1) =/= missRspBA_st1)) && missRspFromMshr_st1)){
    readMissRspCnter := 0.U
  }.elsewhen(missRspFromMshr_st1 && readMissRspCnter =/= BankWords.asUInt){
    readMissRspCnter := readMissRspCnter+1.U
  }
  missRspWriteEnable := (((!RegNext(missRspFromMshr_st1) && missRspFromMshr_st1) ||
    (RegNext(missRspBA_st1) =/= missRspBA_st1)) && missRspFromMshr_st1) ||
    readMissRspCnter=/=BankWords.asUInt*/
  //以NLane=16，BlockSize=32为例，Cnter需要两个不同的状态来表示两个Enable的周期，又需要一个额外的状态来表示不Enable的，所以Cnter的位宽，至少要2

  // ******     DataAccess      ******
  //val DataFromCrsbarOrMemRspQ = Wire(Vec(BlockWords, UInt(WordLength.W)))
  val DataAccessesRRsp: Seq[UInt] = (0 until BlockWords).map { i =>
    val DataAccess = Module(new SRAMTemplate(
      gen=UInt(8.W),
      set=NSets*NWays,
      way=BytesOfWord,
      shouldReset = false,
      holdRead = false,
      singlePort = false
    ))
    DataAccess.io.w.req.valid := Mux(memRsp_st1_valid && !tagReqCurrentMissRspHasSent,
      true.B,  //READ miss resp
      writeHit_st1 & getBankEn.io.perBankValid(i))       //WRITE hit
    /*val readMissRspCnter_if = if(BlockOffsetBits-BankIdxBits>0){
      readMissRspCnter(BankOffsetBits-1,0)
    } else{
      0.U(1.W)
    }*/
    //DataFromCrsbarOrMemRspQ := Mux(missRspWriteEnable,memRspData_st1(readMissRspCnter_if),DataCrsCore2Mem.io.DataOut)
    DataAccess.io.w.req.bits := Mux(memRsp_st1_valid,DataAccessMissRspSRAMWReq(i),DataAccessWriteHitSRAMWReq(i))
    /*DataAccess.io.w.req.bits.data := Mux(missRspFromMshr_st1,
      memRsp_st1.d_data(i),//READ miss resp
      coreReq_st1.data(getBankEn.io.perBankBlockIdx(i)))
    //DataFromCrsbarOrMemRspQ(i).asTypeOf(Vec(BytesOfWord,UInt(8.W)))
    //this setIdx = setIdx + wayIdx
    val DAWtSetIdxMissRspCase_st1 = Cat(get_setIdx(memRsp_Q_st0.d_addr),//setIdx
        TagAccess.io.waymaskReplacement_st1)//wayIdx
    val DAWtSetIdxWtHitCase_st2 = Cat(coreReq_st1.setIdx,//setIdx
        TagAccess.io.waymaskHit_st1)//wayIdx
    DataAccess.io.w.req.bits.setIdx := Mux(missRspFromMshr_st1,
      DAWtSetIdxMissRspCase_st1,//missRsp case
      //write hit case
      DAWtSetIdxWtHitCase_st2)
    DataAccess.io.w.req.bits.waymask.foreach(_ := Mux(missRspWriteEnable,
      //missRsp case
      Fill(BytesOfWord,true.B),
      //write hit case
      coreReq_st1.perLaneAddr(getBankEn.io.perBankBlockIdx(i)).wordOffset1H))*/

    DataAccess.io.r.req.valid := readHit_st1
    //& BankConfArb.io.dataArrayEn(i)) |(writeHitSubWord_st1 & writeSubWordBank_st1(i))
    /*val DARdSetIdx_st1 = if(BlockOffsetBits-BankIdxBits>0){
      Cat(coreReq_st1.setIdx,//setIdx
        wayIdxAtHit_st1,//wayIdx
        BankConfArb.io.addrCrsbarOut(i).bankOffset.getOrElse(false.B))//bankOffset
    } else{*/
    DataAccess.io.r.req.bits := DataAccessReadHitSRAMRReq(i)
    Cat(DataAccess.io.r.resp.data.reverse)
  }
  val DataAccessReadHitSRAMRRsp: Vec[UInt] = VecInit(DataAccessesRRsp)

  // ******      data crossbar     ******

  // ******      core rsp
  when(cacheHit_st1) {
    //coreRsp_st2_valid := true.B
    coreRsp_st2.data := DontCare
    coreRsp_st2.isWrite := coreReqControl_st1.isWrite
    coreRsp_st2.instrId := coreReq_st1.instrId
    coreRsp_st2.activeMask := coreReq_st1.perLaneAddr.map(_.activeMask)
  }.elsewhen(MshrAccess.io.missRspOut.valid){
    //coreRsp_st2_valid := true.B
    coreRsp_st2.data := memRsp_st1.d_data//TODO data crossbar
    coreRsp_st2.isWrite := false.B
    coreRsp_st2.instrId := missRspTI_st1.instrId
    coreRsp_st2.activeMask := missRspTI_st1.perLaneAddr.map(_.activeMask)
  }//TODO add memReq st2

  //assert(!(coreReq_st1_valid && missRspFromMshr_st1),s"when coreReq_st1 valid, hit/miss cant invalid in same cycle")
  val coreRsp_st2_valid_from_coreReq = Wire(Bool())
  val coreRsp_st2_valid_from_memRsp = Wire(Bool())

  coreRsp_st2_valid_from_coreReq := RegNext(coreReq_st1_valid &&
    (readHit_st1 || writeHit_st1))//(coreReqControl_st1.isFlush && )
  coreRsp_st2_valid_from_memRsp := RegNext(MshrAccess.io.missRspOut.valid)
  assert (!(coreRsp_st2_valid_from_coreReq && coreRsp_st2_valid_from_memRsp), s"cRsp from cReq and mRsp conflict")
  coreRsp_st2_valid := coreRsp_st2_valid_from_coreReq || coreRsp_st2_valid_from_memRsp

  coreRsp_Q.io.deq <> io.coreRsp
  coreRsp_Q.io.enq.valid := coreRsp_st2_valid
  coreRsp_Q.io.enq.bits := coreRsp_st2
  when(readHit_st2){
    coreRsp_Q.io.enq.bits.data := DataAccessReadHitSRAMRRsp
  }

  // ******      m_memReq_Q.m_Q.push_back      ******
  memReq_Q.io.enq <> MemReqArb.io.out
  MemReqArb.io.in(0) <> TagAccess.io.memReq.get
  MemReqArb.io.in(1).valid := writeMiss_st1 || readMiss_st1
  MemReqArb.io.in(1).bits := missMemReq
  //TODO MemReqArb.io.in(1).ready need to be used

  io.memReq <> memReq_Q.io.deq
}

/** coreReq hit场景中DataAccess以word为粒度的SRAM bank使能信号
  */
class getDataAccessBankEn(NBank:Int, NLane:Int) extends Module{
  val io = IO(new Bundle{
    val perLaneBlockIdx = Input(Vec(NLane,UInt(log2Up(NBank).W)))
    val perLaneValid = Input(Vec(NLane,Bool()))
    val perBankValid = Output(Vec(NBank,Bool()))
    val perBankBlockIdx = Output(Vec(NBank,UInt(log2Up(NLane).W)))
  })
  val blockIdx1H = Wire(Vec(NLane, UInt(NBank.W)))
  val blockIdxMasked = Wire(Vec(NLane, UInt(NBank.W)))
  for(i <- 0 until NLane){
    blockIdx1H(i) := UIntToOH(io.perLaneBlockIdx(i))
    blockIdxMasked(i) := blockIdx1H(i) & Fill(NLane, io.perLaneValid(i))
  }
  val perBankReq_Bin: Vec[UInt] = Wire(Vec(NBank, UInt(NLane.W)))//transpose of blockIdxMasked
  for(i <- 0 until NBank){
    perBankReq_Bin(i) := Reverse(Cat(blockIdxMasked.map(_(i))))
  }
  io.perBankValid := perBankReq_Bin.map(_.orR)
  io.perBankBlockIdx := perBankReq_Bin.map(PriorityEncoder(_))
}
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

import L1Cache._
import SRAMTemplate._
import chisel3._
import chisel3.util._
import config.config.Parameters
import pipeline.parameters._

class VecMshrTargetInfo(implicit p: Parameters)extends DCacheBundle{
  //val instrId = UInt(WIdBits.W)
  val isWrite = Bool()
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
  //val BankConfArb = Module(new BankConflictArbiter)
  //val bankConflict_reg = Reg(Bool())
  val MshrAccess = Module(new MSHR(bABits = bABits, tIWidth = tIBits, WIdBits = WIdBits, NMshrEntry, NMshrSubEntry))
  val missRspFromMshr_st1 = Wire(Bool())
  val missRspTI_st1 = Wire(new VecMshrTargetInfo)
  val TagAccess = Module(new L1TagAccess(set=NSets, way=NWays, tagBits=TagBits,readOnly=false))
  val DataCorssBar = Module(new DataCrossbar)

  // ******     queues     ******
  val coreRsp_Q_entries :Int = NLanes
  val coreRsp_Q = Module(new Queue(new DCacheCoreRsp,entries = coreRsp_Q_entries,flow=false,pipe=false))
  //this queue also work as a pipeline reg, so cannot flow
  val coreRsp_QAlmstFull = Wire(Bool())
  val memRsp_Q = Module(new Queue(new DCacheMemRsp,entries = 2,flow=false,pipe=false))
  //flow will make predecessor read hit conflict with successor memRsp
  val memRsp_Q_st1 = memRsp_Q.io.deq.bits

  val memReq_Q = Module(new Queue(new DCacheMemReq,entries = 8,flow=false,pipe=false))
  val MemReqArb = Module(new Arbiter(new DCacheMemReq, 2))

  //val coreReqHolding = RegInit(false.B)
  /*val coreReq_st2 = RegNext(coreReq_st1)
  val coreReqInstrId_st3 = RegNext(coreReq_st2.instrId)
  val writeMiss_st3 = Reg(Bool())
  val coreReqActvMask_st3 = Mux(writeMiss_st3,
    //WRITE Miss case
    RegNext(VecInit(coreReq_st2.perLaneAddr.map(_.activeMask))),
    //READ/WRITE Hit case
    ShiftRegister(BankConfArb.io.activeLane,2))
  val coreReqIsWrite_st3 = RegNext(coreReq_st2.isWrite)*/

  // ******     pipeline regs      ******
  val coreReq_st1 = RegEnable(io.coreReq.bits, io.coreReq.fire)
  val coreReq_st1_ready = Wire(Bool())
  val coreReqControl_st0 = Wire(new DCacheControl)
  val coreReqControl_st1: DCacheControl = RegEnable(coreReqControl_st0, io.coreReq.fire)
  val cacheHit_st1 = Wire(Bool())
  cacheHit_st1 := TagAccess.io.hit_st1 && RegEnable(io.coreReq.valid,io.coreReq.fire)
  val cacheMiss_st1 = !TagAccess.io.hit_st1 && RegEnable(io.coreReq.valid,io.coreReq.fire)
  //RegEnable indicate whether the signal is consumed
  val wayIdxAtHit_st1 = Wire(UInt(WayIdxBits.W))
  wayIdxAtHit_st1 := OHToUInt(TagAccess.io.waymaskHit_st1)
  //val wayIdxAtHit_st2 = RegNext(wayIdxAtHit_st1)
  val wayIdxReplace_st0 = Wire(UInt(WayIdxBits.W))
  wayIdxReplace_st0 := OHToUInt(TagAccess.io.waymaskReplacement_st1)

  /*val writeFullWordBank_st1 = Cat(BankConfArb.io.addrCrsbarOut.map(_.wordOffset1H.andR)) //mem Order
  val writeTouchBank_st1 = Cat(BankConfArb.io.addrCrsbarOut.map(_.wordOffset1H.orR)) //mem Order
  val writeSubWordBank_st1 = writeFullWordBank_st1 ^ writeTouchBank_st1 //mem Order*/
  //val byteEn_st1 : Bool = writeFullWordBank_st1 =/= writeTouchBank_st1

  val readHit_st1 = cacheHit_st1 & coreReqControl_st1.isRead
  val readMiss_st1 = cacheMiss_st1 & coreReqControl_st1.isRead
  val writeHit_st1 = cacheHit_st1 & coreReqControl_st1.isWrite
  val writeMiss_st1 = cacheMiss_st1 & coreReqControl_st1.isWrite
  //val writeHitSubWord_st1 = writeHit_st1 & byteEn_st1
  //val writeMissSubWord_st1 = writeMiss_st1 & byteEn_st1

  /*val writeMiss_st2 = RegNext(writeMiss_st1)
  //val writeMissSubWord_st2 = RegNext(writeMissSubWord_st1)
  writeMiss_st3 := writeMiss_st2*/

  val coreRsp_st2 =Reg(new DCacheCoreRsp)
  val coreRsp_st2_valid =Reg(Bool())

  /*val cacheHit_st2 = RegInit(false.B)
  cacheHit_st2 := cacheHit_st1 || (cacheHit_st2 && RegNext(BankConfArb.io.bankConflict))*/
  // bankConflict置高的周期比coreRsp需要输出的周期少一个，而其置高的第一个周期有cacheHit_st1做控制。所以这里使用RegNext(bankConflict)做控制
  val readHit_st2 = RegNext(readHit_st1)
  //val readHit_st3 = RegNext(readHit_st2)
  val writeHit_st2 = RegNext(writeHit_st1)//cacheHit_st2 && coreReq_st2.isWrite
  //val writeHit_st3 = RegNext(writeHit_st2)
  //val arbArrayEn_st2 = RegNext(BankConfArb.io.dataArrayEn)

  // ******      l1_data_cache::coreReq_pipe1_cycle      ******
  // ******      tag probe      ******
  val missRspWriteEnable = Wire(Bool())
  TagAccess.io.probeRead.valid := io.coreReq.fire
  TagAccess.io.probeRead.bits.setIdx := io.coreReq.bits.setIdx
  TagAccess.io.tagFromCore_st1 := coreReq_st1.tag

  // ******      mshr probe      ******
  MshrAccess.io.probe.valid := io.coreReq.fire
  MshrAccess.io.probe.bits.blockAddr := Cat(io.coreReq.bits.tag,io.coreReq.bits.setIdx)

  // ******      tag write      ******
  TagAccess.io.allocateWrite.valid := missRspWriteEnable//multiple for secondary miss
  TagAccess.io.allocateWrite.bits(
    data=get_tag(memRsp_Q_st1.d_addr),
    setIdx=get_setIdx(memRsp_Q_st1.d_addr),
    waymask = 1.U)

  val genCtrl = Module(new genControl)
  genCtrl.io.opcode := io.coreReq.bits.opcode
  genCtrl.io.param := io.coreReq.bits.param
  coreReqControl_st0 := genCtrl.io.control

  io.coreReq.ready := coreReq_st1_ready

  // ******      l1_data_cache::coreReq_pipe2_cycle      ******
  // ******      mshr missReq      ******
  MshrAccess.io.missReq.valid := readMiss_st1
  val mshrMissReqTI = Wire(new VecMshrTargetInfo)
  mshrMissReqTI.isWrite := coreReqControl_st1.isWrite
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
  writeMissReq.a_source := Cat("d3".U, coreReq_st1.instrId)
  writeMissReq.a_addr := Cat(coreReq_st1.tag, coreReq_st1.setIdx, 0.U((WordLength - TagBits - SetIdxBits).W))
  writeMissReq.a_mask := coreReq_st1.perLaneAddr.map(_.activeMask)
  writeMissReq.a_data := coreReq_st1.data

  readMissReq.a_opcode := 4.U //Get
  readMissReq.a_param := 0.U //regular read
  readMissReq.a_source := Cat("d2".U, MshrAccess.io.probeOut_st1.a_source)
  readMissReq.a_addr := Cat(coreReq_st1.tag, coreReq_st1.setIdx, 0.U((WordLength - TagBits - SetIdxBits).W))
  readMissReq.a_mask := coreReq_st1.perLaneAddr.map(_.activeMask)
  readMissReq.a_data := DontCare

  missMemReq := Mux(writeMiss_st1, writeMissReq, readMissReq)

  // ******      dataAccess bank enable     ******
  val getBankEn = Module(new getDataAccessBankEn(NBank = BlockWords, NLane = num_thread))
  getBankEn.io.perLaneBlockIdx := coreReq_st1.perLaneAddr.map(_.blockOffset)
  getBankEn.io.perLaneValid := coreReq_st1.perLaneAddr.map(_.activeMask)

  // ******      dataAccess write hit      ******
  val DataAccessWriteHitSRAMWReq: Vec[SRAMBundleAW[UInt]] = Wire(Vec(BlockWords,new SRAMBundleAW(UInt(xLen.W), NSets, NWays)))
  DataAccessWriteHitSRAMWReq.foreach(_.setIdx := coreReq_st1.setIdx)
  DataAccessWriteHitSRAMWReq.foreach(_.waymask.get := TagAccess.io.waymaskHit_st1)
  for (i <- 0 until BlockWords){
    DataAccessWriteHitSRAMWReq(i).data := coreReq_st1.data(getBankEn.io.perBankBlockIdx(i))
  }

  // ******      dataAccess read hit      ******
  val DataAccessReadHitSRAMWReq = Wire(Vec(BlockWords,new SRAMBundleA(NSets)))
  DataAccessReadHitSRAMWReq.foreach(_.setIdx := coreReq_st1.setIdx)

  /*!(BankConfArb.io.bankConflict && (cacheHit_st1 || cacheHit_st2)) &
    !missRspFromMshr_st1 & !(io.memRsp.valid && !ShiftRegister(io.coreReq.bits.isWrite && io.coreReq.fire(), 2)) &
    !(cacheHit_st1 & coreRsp_QAlmstFull) &
    !(coreReq_st1.isWrite & WriteDataBuf.io.wdbAlmostFull) &
    !(readHit_st2 & coreReq_st1.isWrite) &
    (MshrAccess.io.missReq.ready || (!MshrAccess.io.missReq.ready && io.coreReq.bits.isWrite))*/
  coreReq_st1_ready := false.B
  when(coreReqControl_st1.isRead || coreReqControl_st1.isWrite){
    when(cacheHit_st1){
      when(coreRsp_Q.io.enq.ready){
        coreReq_st1_ready := true.B
      }
    }.otherwise{
      assert(cacheMiss_st1,s"coreReq_st1有效时，hit和miss总有一个有效")
      when(coreReqControl_st1.isRead){
        //when(MshrAccess.io.probeOut_st1.probeStatus(0).asBool//PRIMARY_AVAIL|SECONDARY_AVAIL
        when(MshrAccess.mshrProbeAvail && memReq_Q.io.enq.ready){
          coreReq_st1_ready := true.B
        }
      }.otherwise{//isWrite
        when(coreRsp_Q.io.enq.ready && memReq_Q.io.enq.ready){
          coreReq_st1_ready := true.B
        }
      }
    }
  }

  when(cacheHit_st1){//TODO consider memRsp
    coreRsp_st2_valid := true.B
    coreRsp_st2.data := DontCare
    coreRsp_st2.isWrite := coreReqControl_st1.isWrite
    coreRsp_st2.instrId := coreReq_st1.instrId
    coreRsp_st2.activeMask := coreReq_st1.perLaneAddr.map(_.activeMask)
  }

  // ******     mem rsp      ******
  memRsp_Q.io.enq <> io.memRsp
  memRsp_Q.io.deq.ready := MshrAccess.io.missRspIn.ready && !cacheHit_st2 && !ShiftRegister(io.coreReq.bits.isWrite&&io.coreReq.fire(),2)
  val memRspData_st1 = Wire(Vec(BankWords,Vec(NBanks,UInt(WordLength.W))))
  val memRspData_st2 = Wire(Vec(BankWords,Vec(NBanks,UInt(WordLength.W))))
  (0 until BankWords).foreach{ iinB =>
    (0 until NBanks).foreach{iofB =>
      memRspData_st1(iinB)(iofB) := memRsp_Q_st1.d_data((iinB*NBanks+iofB).asUInt)
    }
  }
  memRspData_st2 := RegEnable(memRspData_st1,memRsp_Q.io.deq.fire() || (memRsp_Q.io.deq.valid && BankConfArb.io.bankConflict))

  // ******     mshrAccess      ******
  MshrAccess.io.missRspIn.valid := memRsp_Q.io.deq.valid && !cacheHit_st2 && !ShiftRegister(io.coreReq.bits.isWrite&&io.coreReq.fire(),2)
  MshrAccess.io.missRspIn.bits.blockAddr := get_blockAddr(memRsp_Q.io.deq.bits.d_addr)

  missRspFromMshr_st1 := MshrAccess.io.missRspOut.valid//suffix _st2 is on another path comparing to cacheHit
  missRspTI_st1 := MshrAccess.io.missRspOut.bits.targetInfo.asTypeOf(new VecMshrTargetInfo)
  val missRspBA_st1 = MshrAccess.io.missRspOut.bits.blockAddr
  val missRspTILaneMask_st2 = RegNext(BankConfArb.io.activeLane)
  val memRspInstrId_st2 = RegNext(MshrAccess.io.missRspOut.bits.instrId)
  //val readMissRsp_st2 = missRspFromMshr_st2 & !missRspTI.isWrite
  val readMissRspCnter = if(BankOffsetBits!=0) RegInit(0.U((BankOffsetBits+1).W)) else Reg(UInt())//TODO use Counter
  MshrAccess.io.missRspOut.ready := coreRsp_Q.io.enq.ready

  //val mshrMissRspStrobe = !RegNext(MshrAccess.io.missRspOut.valid) |
   // RegNext(RegNext(MshrAccess.io.missRspOut.ready) && MshrAccess.io.missRspOut.valid)
  val readMissRsp_st1 = (MshrAccess.io.missRspOut.fire() || (MshrAccess.io.missRspOut.valid && BankConfArb.io.bankConflict)) & !missRspTI_st1.isWrite
  val writeMissRsp_st1 = MshrAccess.io.missRspOut.fire() & missRspTI_st1.isWrite
  //only on subword miss
  val readMissRsp_st2 = RegNext(readMissRsp_st1)
  val writeMissRsp_st2 = RegNext(writeMissRsp_st1)
  when(readMissRspCnter === (BankWords-1).asUInt &&
    (((!RegNext(missRspFromMshr_st1) && missRspFromMshr_st1) ||
    (RegNext(missRspBA_st1) =/= missRspBA_st1)) && missRspFromMshr_st1)){
    readMissRspCnter := 0.U
  }.elsewhen(missRspFromMshr_st1 && readMissRspCnter =/= BankWords.asUInt){
    readMissRspCnter := readMissRspCnter+1.U
  }
  missRspWriteEnable := (((!RegNext(missRspFromMshr_st1) && missRspFromMshr_st1) ||
    (RegNext(missRspBA_st1) =/= missRspBA_st1)) && missRspFromMshr_st1) ||
    readMissRspCnter=/=BankWords.asUInt
  //以NLane=16，BlockSize=32为例，Cnter需要两个不同的状态来表示两个Enable的周期，又需要一个额外的状态来表示不Enable的，所以Cnter的位宽，至少要2

  // ******     DataAccess      ******
  val DataFromCrsbarOrMemRspQ = Wire(Vec(NBanks, UInt(WordLength.W)))
  val DataAccessesRRsp: Seq[UInt] = (0 until NBanks).map { i =>
    val DataAccess = Module(new SRAMTemplate(
      gen=UInt(8.W),
      set=NSets*NWays*(BankWords),
      way=BytesOfWord,
      shouldReset = false,
      holdRead = false,
      singlePort = false
    ))
    DataAccess.io.w.req.valid := Mux(missRspWriteEnable,true.B,  //READ miss resp
      writeHit_st2 & arbArrayEn_st2(i))               //WRITE hit subWord
    val readMissRspCnter_if = if(BlockOffsetBits-BankIdxBits>0){
      readMissRspCnter(BankOffsetBits-1,0)
    } else{
      0.U(1.W)
    }
    DataFromCrsbarOrMemRspQ := Mux(missRspWriteEnable,memRspData_st1(readMissRspCnter_if),DataCorssBar.io.DataOut)
    DataAccess.io.w.req.bits.data := DataFromCrsbarOrMemRspQ(i).asTypeOf(Vec(BytesOfWord,UInt(8.W)))
    //this setIdx = setIdx + wayIdx + bankOffset
    val DAWtSetIdxMissRspCase_st1 = if(BlockOffsetBits-BankIdxBits>0){
      Cat(get_setIdx(memRsp_Q_st1.d_addr),//setIdx
        wayIdxReplace_st0,//wayIdx TODO check
        readMissRspCnter_if)//bankOffset
    } else{
      Cat(get_setIdx(memRsp_Q_st1.d_addr),//setIdx
        wayIdxReplace_st0)//wayIdx TODO check
    }
    val DAWtSetIdxWtHitCase_st2 = if(BlockOffsetBits-BankIdxBits>0){
      Cat(coreReq_st2.setIdx,//setIdx
        wayIdxAtHit_st2,//wayIdx
        arbAddrCrsbarOut_st2(i).bankOffset.getOrElse(false.B))//bankOffset
    } else{
      Cat(coreReq_st2.setIdx,//setIdx
        wayIdxAtHit_st2)//wayIdx
    }
    DataAccess.io.w.req.bits.setIdx := Mux(missRspWriteEnable,
      //missRsp case
      DAWtSetIdxMissRspCase_st1,
      //write hit case
      DAWtSetIdxWtHitCase_st2)
    DataAccess.io.w.req.bits.waymask.foreach(_ := Mux(missRspWriteEnable,
      //missRsp case
      Fill(BytesOfWord,true.B),
      //write hit case
      arbAddrCrsbarOut_st2(i).wordOffset1H))

    DataAccess.io.r.req.valid := (readHit_st1 & BankConfArb.io.dataArrayEn(i)) |
      (writeHitSubWord_st1 & writeSubWordBank_st1(i))
    val DARdSetIdx_st1 = if(BlockOffsetBits-BankIdxBits>0){
      Cat(coreReq_st1.setIdx,//setIdx
        wayIdxAtHit_st1,//wayIdx
        BankConfArb.io.addrCrsbarOut(i).bankOffset.getOrElse(false.B))//bankOffset
    } else{
      Cat(coreReq_st1.setIdx,//setIdx
        wayIdxAtHit_st1)//wayIdx
    }
    DataAccess.io.r.req.bits.setIdx := DARdSetIdx_st1
    Cat(DataAccess.io.r.resp.data.reverse)
  }
  val dataAccess_data_st3 = RegEnable(VecInit(DataAccessesRRsp),readHit_st2)

  // ******      data crossbar     ******
  DataCorssBar.io.DataIn := Mux(readMissRsp_st2,
    VecInit((0 until NBanks).map{i => memRspData_st2(arbAddrCrsbarOut_st2(i).bankOffset.getOrElse(0.U))(i)}),//READ missRsp case
    Mux(coreReq_st2.isWrite,
      coreReq_st2.data,//WRITE case(all 3
      dataAccess_data_st3//READ hit case
    ))
  //Sel from st2 on each WRITE flow, and READ missRsp
  DataCorssBar.io.Select1H := Mux(coreReq_st2.isWrite | readMissRsp_st2,
    arbDataCrsbarSel1H_st2,
    arbDataCrsbarSel1H_st3)

  // ******      core rsp
  coreRsp_Q.io.deq <> io.coreRsp
  coreRsp_Q.io.enq.valid := coreRsp_st2_valid
  coreRsp_Q.io.enq.bits := coreRsp_st2
  coreRsp_Q.io.enq.bits.data := Mux(writeMissRsp_st2,memReq_Q.io.deq.bits.a_data,VecInit(DataAccessesRRsp))
  /*.isWrite := Mux(writeMissRsp_st2,
    //WRITE miss Rsp
    true.B,
    //WRITE hit
    coreReqIsWrite_st3)
  coreRsp_Q.io.enq.bits.data := DataCorssBar.io.DataOut
  coreRsp_Q.io.enq.bits.instrId := Mux(readMissRsp_st2,
    //READ miss Rsp
    memRspInstrId_st2,
    //READ hit
    coreReqInstrId_st3)
  coreRsp_Q.io.enq.bits.activeMask := Mux(readMissRsp_st2,
    //READ miss Rsp
    missRspTILaneMask_st2,
    //Mux of READ/WRITE hit or WRITE miss case
    coreReqActvMask_st3)//refer to generation of this signal
  coreRsp_QAlmstFull := coreRsp_Q.io.count === coreRsp_Q_entries.asUInt - 2.U*/

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
    val perBankBlockIdx = Input(Vec(NBank,UInt(log2Up(NLane).W)))
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
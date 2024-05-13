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
import top.parameters.{NUMBER_CU, dcache_BlockWords, dcache_MshrEntry, dcache_NSets, num_thread}
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
  val isWaitMSHR = Bool()
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
  io.control.isWaitMSHR := false.B

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
      }.elsewhen(io.param === 2.U){
        io.control.isWaitMSHR := true.B
      }
    }
  }
}

class genDataMapPerByte(numdata:Int, NLanes:Int) extends Module{
  val io = IO(new Bundle{
    val OriData = Input(Vec(numdata,UInt(NLanes.W)))
    val offsetMask = Input(Vec(numdata,UInt(4.W)))
    val DataOut = Output(Vec(numdata,UInt(NLanes.W)))
  })
  for(i <- 0 until numdata){
    when(io.offsetMask(i).andR){
      io.DataOut(i) := io.OriData(i)
    }.elsewhen(PopCount(io.offsetMask(i))=== 1.U){
      io.DataOut(i) := io.OriData(i) << (OHToUInt(io.offsetMask(i)) << 3)
    }.elsewhen(io.offsetMask(i)(3,2) === 3.U){
      io.DataOut(i) := io.OriData(i) << 16
    }.otherwise{
      io.DataOut(i) := io.OriData(i)
    }
  }
}

class genDataMapSameWord(implicit p: Parameters) extends DCacheModule{
  val io = IO(new Bundle {
    val perLaneAddr = Input(Vec(NLanes, new DCachePerLaneAddr))
    val datain = Input(Vec(NLanes,UInt(WordLength.W)))
    val perLaneAddrRemap = Output(Vec(NLanes, new DCachePerLaneAddr))
    val dataout = Output(Vec(NLanes,UInt(WordLength.W)))
  })
  val blockOffsetMatch = Wire(Vec(NLanes,Vec(NLanes,Bool())))
  val wordOffsetRemap = Wire(Vec(NLanes,Vec(NLanes,UInt(BytesOfWord.W))))
  val dataRemap = Wire(Vec(NLanes,Vec(NLanes,UInt(WordLength.W))))
  for(i<- 0 until NLanes){
    for(j<- 0 until NLanes){
      when(io.perLaneAddr(i).activeMask && io.perLaneAddr(j).activeMask){
        blockOffsetMatch(i)(j) := io.perLaneAddr(i).blockOffset === io.perLaneAddr(j).blockOffset
      }.otherwise{
        blockOffsetMatch(i)(j) := false.B
      }
    }
  }
  for(i<- 0 until NLanes){
    io.perLaneAddrRemap(i).blockOffset := io.perLaneAddr(i).blockOffset
    io.perLaneAddrRemap(i).activeMask := io.perLaneAddr(i).activeMask
    io.perLaneAddrRemap(i).wordOffset1H := wordOffsetRemap(i).reduce(_|_)
    io.dataout(i) := dataRemap(i).reduce(_|_)
    for(j <-0 until NLanes){
      wordOffsetRemap(i)(j) := Mux(blockOffsetMatch(i)(j),io.perLaneAddr(j).wordOffset1H,0.U)
      dataRemap(i)(j) := Mux(blockOffsetMatch(i)(j),io.datain(j),0.U)
    }

  }
}

class relocateDataByte(numdata:Int, NLanes:Int) extends Module{
  val io = IO(new Bundle{
    val OriData = Input(Vec(numdata,UInt(NLanes.W)))
    val offsetMask = Input(Vec(numdata,UInt(4.W)))
    val DataOut = Output(Vec(numdata,UInt(NLanes.W)))
  })
  for(i <- 0 until numdata){
    when(io.offsetMask(i).andR){
      io.DataOut(i) := io.OriData(i)
    }.elsewhen(PopCount(io.offsetMask(i))=== 1.U){
      io.DataOut(i) := io.OriData(i) >> (OHToUInt(io.offsetMask(i)) << 3)
    }.elsewhen(io.offsetMask(i)(3,2) === 3.U){
      io.DataOut(i) := io.OriData(i) >> 16
    }.otherwise{
      io.DataOut(i) := io.OriData(i)
    }
  }
}


class WshrMemReq extends DCacheMemReq{
  val hasCoreRsp = Bool()
  val coreRspInstrId = UInt(32.W)
}

class WshrMemReq_withMask(NLanes: Int) extends DCacheMemReq{
  val wshrMemReq = new WshrMemReq
  val activeMask = Vec(NLanes, Bool())
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
  val readmiss_sameadd = Wire(Bool())
  val coreReq_st1_ready = Wire(Bool())

  val TagAccess = Module(new L1TagAccess(set=NSets, way=NWays, tagBits=TagBits,readOnly=false))
  val WshrAccess = Module(new DCacheWSHR(Depth = NWshrEntry))
  val mshrProbeStatus = MshrAccess.io.probeOut_st1.probeStatus//Alias
  // ******     queues     ******
  val coreReq_Q = Module(new Queue(new DCacheCoreReq,entries = 1,flow=false,pipe=true))
  //comb ready exist, be careful the latency!
  val coreRsp_Q_entries :Int = NLanes
  val coreRsp_Q = Module(new Queue(new DCacheCoreRsp,entries = coreRsp_Q_entries,flow=false,pipe=false))
  //this queue also work as a pipeline reg, so cannot flow
  //val coreRsp_QAlmstFull = Wire(Bool())
  val memRsp_Q = Module(new Queue(new DCacheMemRsp,entries = 2,flow=false,pipe=false))
  //flow will make predecessor read hit conflict with successor memRsp
  val memRsp_Q_st0 = memRsp_Q.io.deq.bits

  val memReq_Q = Module(new Queue(new WshrMemReq,entries = 8,flow=false,pipe=false))
  val MemReqArb = Module(new Arbiter(new WshrMemReq, 3))
  val genData = Module(new genDataMapPerByte(NLanes,WordLength))
  val remapDataPerWord = Module(new genDataMapSameWord)
  val coreReqControl_st0_noen = Wire(new DCacheControl)
  val coreRsp_st2_valid_from_coreReq_Reg = Module(new Queue(Bool(),1,true,false))
  //val recData = Module(new relocateDataByte(BlockWords,WordLength))
  //val recDataMemRsp = Module(new relocateDataByte(BlockWords,WordLength))
  val waitforL2flush = RegInit(false.B)
  val probereadAllocateWriteConflict = Wire(Bool())
  val inflightReadWriteMiss = RegInit(false.B)
  val inflightreadwritemiss_w = (coreReqControl_st0_noen.isWrite && MshrAccess.io.mshrStatus_st0 =/= 0.U) || inflightReadWriteMiss
  // ******     pipeline regs      ******
  coreReq_Q.io.enq.valid := io.coreReq.valid && !probereadAllocateWriteConflict && TagAccess.io.probeRead.ready  && (MshrAccess.io.mshrStatus_st0 =/= 3.U) && (MshrAccess.io.mshrStatus_st0 =/= 1.U)
  val coreReq_st0_ready =  coreReq_Q.io.enq.ready && !probereadAllocateWriteConflict && !inflightreadwritemiss_w && !readmiss_sameadd && TagAccess.io.probeRead.ready && (MshrAccess.io.mshrStatus_st0 =/= 3.U)&& (MshrAccess.io.mshrStatus_st0 =/= 1.U)
  io.coreReq.ready := coreReq_Q.io.enq.ready && !probereadAllocateWriteConflict && !inflightreadwritemiss_w &&  !readmiss_sameadd && TagAccess.io.probeRead.ready && (MshrAccess.io.mshrStatus_st0 =/= 3.U)&& (MshrAccess.io.mshrStatus_st0 =/= 1.U)
  coreReq_Q.io.enq.bits := io.coreReq.bits

  val coreReq_st1 = coreReq_Q.io.deq.bits

  //val coreReq_st1_valid_pre = RegInit(false.B)
  val coreReq_st1_valid = Wire(Bool())
  //val memRsp_st1_valid = RegInit(false.B)//early definition
  //secondaryFullReturn时cReq_st1可以valid，也可以fire。是missRspOut期间唯一例外
  val secondaryFullReturn = RegNext(MshrAccess.io.probeOut_st1.probeStatus === 4.U)
  val coreReqControl_st0 = Wire(new DCacheControl)

  //val coreReqControl_st1: DCacheControl = RegEnable(coreReqControl_st0,io.coreReq.fire())
  val coreReqControl_st1_Q = Module(new Queue(new DCacheControl,1,true,false))
  coreReqControl_st1_Q.io.enq.bits := coreReqControl_st0
  coreReqControl_st1_Q.io.enq.valid := io.coreReq.fire()
  coreReqControl_st1_Q.io.deq.ready := coreReq_st1_ready//coreRsp_st2_valid_from_coreReq_Reg.io.enq.ready
  val cacheHit_st1 = Wire(Bool())
  cacheHit_st1 := TagAccess.io.hit_st1
  val cacheMiss_st1 = !TagAccess.io.hit_st1


  val readHit_st1 = cacheHit_st1 & coreReqControl_st1_Q.io.deq.bits.isRead & coreReqControl_st1_Q.io.deq.fire()
  val readMiss_st1 = cacheMiss_st1 & coreReqControl_st1_Q.io.deq.bits.isRead & coreReqControl_st1_Q.io.deq.valid//coreReqControl_st1_Q.io.deq.fire()
  val writeHit_st1 = cacheHit_st1 & coreReqControl_st1_Q.io.deq.bits.isWrite & coreReqControl_st1_Q.io.deq.fire()
  val writeMiss_st1 = cacheMiss_st1 & coreReqControl_st1_Q.io.deq.bits.isWrite & coreReqControl_st1_Q.io.deq.fire()

  val coreRsp_st2 =Module(new Queue(new DCacheCoreRsp_d,1,true,false))//Reg(new DCacheCoreRsp)
  val coreRsp_st2_valid =Wire(Bool())
  val coreRsp_st2_perLaneAddr = Reg(Vec(NLanes, new DCachePerLaneAddr))
  val readHit_st2 = Module(new Queue(Bool(),1,true,false))
  readHit_st2.io.enq.bits := readHit_st1 //|| (readHit_st2 && (!coreRsp_Q.io.enq.fire()))
  readHit_st2.io.enq.valid := coreReq_st1_valid
  val readHit_st2_valid = Wire(Bool())
  readHit_st2_valid :=  readHit_st2.io.deq.fire && readHit_st2.io.deq.bits.asUInt.asBool

  //val readHit_st2 = RegNext(readHit_st1 )
  val injectTagProbe = inflightReadWriteMiss ^ RegNext(inflightReadWriteMiss)//RegInit(false.B)//inflightReadWriteMiss && (mshrProbeStatus === 0.U)
  readmiss_sameadd := MshrAccess.io.missReq.valid && (MshrAccess.io.probe.bits.blockAddr === MshrAccess.io.missReq.bits.blockAddr) &&
    io.coreReq.valid  && coreReq_Q.io.deq.valid
  // ******      l1_data_cache::coreReq_pipe0_cycle      ******
  coreReq_Q.io.deq.ready := coreReq_st1_ready &&
    !(coreReq_Q.io.deq.bits.opcode === 3.U && readHit_st1 && coreReq_st1_valid) //InvOrFlu希望在st0读Data SRAM，检查资源冲突
  // ******      tag probe      ******
  //val missRspWriteEnable = Wire(Bool())
  TagAccess.io.probeRead.valid := io.coreReq.fire || injectTagProbe
  TagAccess.io.probeRead.bits.setIdx := Mux(injectTagProbe,coreReq_st1.setIdx,io.coreReq.bits.setIdx)
  TagAccess.io.tagFromCore_st1 := coreReq_st1.tag
  TagAccess.io.tagready_st1 := coreReq_st1_ready//coreRsp_st2_valid_from_coreReq_Reg.io.enq.ready


  // ******      mshr probe      ******
  MshrAccess.io.probe.valid := io.coreReq.valid && coreReq_st0_ready
  MshrAccess.io.probe.bits.blockAddr := Cat(io.coreReq.bits.tag,io.coreReq.bits.setIdx)
  MshrAccess.io.stage1_ready := coreReq_st1_ready

  val genCtrl = Module(new genControl)

  genCtrl.io.opcode := io.coreReq.bits.opcode
  genCtrl.io.param := io.coreReq.bits.param
  coreReqControl_st0 := Mux(io.coreReq.fire,genCtrl.io.control,0.U.asTypeOf(genCtrl.io.control))
  coreReqControl_st0_noen := genCtrl.io.control
  val InvOrFluAlreadyflush = RegInit(true.B)
  // ******    l1_data_cache::coreReq_probe_evict_for_invORFlu     *******
  val coreReqTagHasDirty_st1 = RegInit(false.B)
  coreReqTagHasDirty_st1 := TagAccess.io.hasDirty_st0.get
  val coreReqInvOrFluValid_st0 = Wire(Bool())
  val coreReqInvOrFluValid_st1 = Wire(Bool())
  val coreReqInv_st0 : Bool = coreReq_Q.io.deq.valid &&
    coreReq_Q.io.deq.bits.opcode === 3.U && coreReq_Q.io.deq.bits.param === 0.U
  coreReqInvOrFluValid_st0 := coreReq_Q.io.deq.valid &&
    coreReq_Q.io.deq.bits.opcode === 3.U && coreReq_Q.io.deq.bits.param =/= 2.U
  coreReqInvOrFluValid_st1 := coreReq_st1_valid &&
    (coreReqControl_st1_Q.io.deq.bits.isInvalidate || coreReqControl_st1_Q.io.deq.bits.isFlush)
  val coreReqInv_st1: Bool = coreReqControl_st1_Q.io.deq.bits.isInvalidate
  TagAccess.io.flushChoosen.get.valid := (coreReqInvOrFluValid_st0 || coreReqInvOrFluValid_st1) &&
    TagAccess.io.hasDirty_st0.get//TODO add LRSC cond
  TagAccess.io.flushChoosen.get.bits := //TODO add LRSC cond
    Cat(TagAccess.io.dirtySetIdx_st0.get,TagAccess.io.dirtyWayMask_st0.get)
  val DataAccessInvOrFluSRAMRReq = Wire(Vec(BlockWords,new SRAMBundleA(NSets*NWays)))
  val dataAccessInvOrFluRValid = (coreReqInvOrFluValid_st0 || coreReqInvOrFluValid_st1) &&
    TagAccess.io.hasDirty_st0.get//same to TagAccess.io.flushChoosen.get.valid
  DataAccessInvOrFluSRAMRReq.foreach(_.setIdx := Cat(TagAccess.io.dirtySetIdx_st0.get,
    OHToUInt(TagAccess.io.dirtyWayMask_st0.get)))

  // ******      l1_data_cache::coreReq_pipe1_cycle      ******
  coreReq_st1_valid := coreReq_Q.io.deq.valid && !(MshrAccess.io.missRspOut.valid && !secondaryFullReturn)
  TagAccess.io.probeIsWrite_st1.get := coreReqControl_st1_Q.io.deq.bits.isWrite && coreReqControl_st1_Q.io.deq.valid &&cacheHit_st1//writeHit_st1

  // ******      mshr missReq      ******
  //val secondaryFullReturn = RegNext(MshrAccess.io.probeOut_st1.probeStatus === 4.U) early definition
  MshrAccess.io.missReq.valid := readMiss_st1 && !MshrAccess.io.missRspOut.valid && coreReq_st1_valid  && MshrAccess.io.probestatus
  val mshrMissReqTI = Wire(new VecMshrTargetInfo)
  //mshrMissReqTI.isWrite := coreReqControl_st1.isWrite
  mshrMissReqTI.instrId := coreReq_st1.instrId
  mshrMissReqTI.perLaneAddr := coreReq_st1.perLaneAddr
  MshrAccess.io.missReq.bits.instrId := coreReq_st1.instrId
  MshrAccess.io.missReq.bits.blockAddr := Cat(coreReq_st1.tag, coreReq_st1.setIdx)
  MshrAccess.io.missReq.bits.targetInfo := mshrMissReqTI.asUInt
  MshrAccess.io.stage2_ready := MemReqArb.io.in(1).ready

  // ******      memReq_Q enq      ******
  val missMemReq = Wire(new WshrMemReq) //writeMiss_st1 || readMiss_st1
  val writeMissReq = Wire(new WshrMemReq)
  val readMissReq = Wire(new WshrMemReq)
  val LaneAddrOH = coreReq_st1.perLaneAddr.map( a => Mux(a.activeMask,UIntToOH(a.blockOffset),0.U))
  val activeLaneAddr = coreReq_st1.perLaneAddr.map( a => Mux(a.activeMask,a.blockOffset,0.U))
  //val activeLaneMask = coreReq_st1.perLaneAddr.map( a => Mux(a.activeMask,1.U,0.U))
  val activeLaneMask = Wire(Vec(num_thread,UInt(1.W)))
  activeLaneMask := coreReq_st1.perLaneAddr.map( a => Mux(a.activeMask,1.U,0.U))
  val activeLaneMaskPress = activeLaneMask.asTypeOf(UInt(num_thread.W))
  val blockaddr_ = activeLaneAddr.reduce(_ | _)
  val blockaddr_1 = LaneAddrOH.reduce(_ | _)
  val LaneBlockConv = Wire(Vec(dcache_BlockWords,Vec(NLanes,UInt(1.W))))
  val WordOffsetConv = Wire(Vec(dcache_BlockWords,UInt(BytesOfWord.W)))
  val coreReq_st1_data_map_byte = Wire(Vec(NLanes,UInt(WordLength.W)))
  val coreReq_st1_data_map_sameword = Wire(Vec(NLanes,UInt(WordLength.W)))
  val coreReq_st1_PerLaneAddr_Remap = Wire(Vec(NLanes,new DCachePerLaneAddr))

  genData.io.OriData := coreReq_st1.data
  genData.io.offsetMask := coreReq_st1.perLaneAddr.map(a => a.wordOffset1H)
  coreReq_st1_data_map_byte := genData.io.DataOut
  remapDataPerWord.io.datain := coreReq_st1_data_map_byte
  remapDataPerWord.io.perLaneAddr := coreReq_st1.perLaneAddr
  coreReq_st1_PerLaneAddr_Remap := remapDataPerWord.io.perLaneAddrRemap
  coreReq_st1_data_map_sameword := remapDataPerWord.io.dataout

  writeMissReq.a_opcode := 1.U //PutPartialData:Get
  writeMissReq.a_param := 0.U //regular write
  writeMissReq.a_source := DontCare//wait for WSHR
  writeMissReq.a_addr := Cat(coreReq_st1.tag, coreReq_st1.setIdx, 0.U((WordLength - TagBits - SetIdxBits).W))
  //writeMissReq.a_mask := blockaddr_1.asTypeOf(writeMissReq.a_mask)//coreReq_st1.perLaneAddr.map(_.activeMask)
  for(j<-0 until dcache_BlockWords) {
    for (i <- 0 until NLanes) {
      when(coreReq_st1.perLaneAddr(i).blockOffset === j.asUInt && coreReq_st1.perLaneAddr(i).activeMask) {
        LaneBlockConv(j)(i) := 1.U
      }.otherwise {
        LaneBlockConv(j)(i) := 0.U
      }
    }
  }
  for(j<-0 until dcache_BlockWords){
    writeMissReq.a_data(j) :=coreReq_st1_data_map_sameword.zip(LaneBlockConv(j)).map{case(a,b) => Mux(b.asBool,a, 0.U)}.reduce(_ | _)
    WordOffsetConv(j) := coreReq_st1_PerLaneAddr_Remap.zip(LaneBlockConv(j)).map{case(a,b) => Mux(b.asBool, a.wordOffset1H,0.U)}.reduce(_|_)
    writeMissReq.a_mask(j) := WordOffsetConv(j)
  }
  writeMissReq.hasCoreRsp := true.B
  writeMissReq.coreRspInstrId := coreReq_st1.instrId

  readMissReq.a_opcode := 4.U //Get
  readMissReq.a_param := 0.U //regular read
  readMissReq.a_source := Cat("d1".U, MshrAccess.io.probeOut_st1.a_source, coreReq_st1.setIdx)//setIdx for memRsp tag access in 1st stage
  readMissReq.a_addr := Cat(coreReq_st1.tag, coreReq_st1.setIdx, 0.U((WordLength - TagBits - SetIdxBits).W))
  readMissReq.a_mask := VecInit(Seq.fill(BlockWords)(Fill(BytesOfWord,1.U)))//lockaddr_1H.asTypeOf(writeMissReq.a_mask)//coreReq_st1.perLaneAddr.map(_.activeMask)
  readMissReq.a_data := DontCare
  readMissReq.hasCoreRsp := false.B
  readMissReq.coreRspInstrId := DontCare

  missMemReq := Mux(writeMiss_st1, writeMissReq, readMissReq)
  // ******      dataAccess bank enable     ******
  val getBankEn = Module(new getDataAccessBankEn(NBank = BlockWords, NLane = NLanes))
  getBankEn.io.perLaneBlockIdx := coreReq_st1.perLaneAddr.map(_.blockOffset)
  getBankEn.io.perLaneValid := coreReq_st1.perLaneAddr.map(_.activeMask)

  // ******      dataAccess write hit      ******
  val DataAccessWriteHitSRAMWReq: Vec[SRAMBundleAW[UInt]] = Wire(Vec(BlockWords,new SRAMBundleAW(UInt(8.W), NSets*NWays, BytesOfWord)))
  //this setIdx = setIdx + wayIdx
  DataAccessWriteHitSRAMWReq.foreach(_.setIdx := Cat(coreReq_st1.setIdx,OHToUInt(TagAccess.io.waymaskHit_st1)))
  for (i <- 0 until BlockWords){
    DataAccessWriteHitSRAMWReq(i).waymask.get := coreReq_st1_PerLaneAddr_Remap(getBankEn.io.perBankBlockIdx(i)).wordOffset1H
    DataAccessWriteHitSRAMWReq(i).data := coreReq_st1_data_map_sameword(getBankEn.io.perBankBlockIdx(i)).asTypeOf(Vec(BytesOfWord,UInt(8.W)))//TODO check order
  }
  val memRspIsFluOrInv: Bool = memRsp_Q.io.deq.bits.d_opcode === 2.U //hintAck
  // ******      dataAccess read hit      ******
  val DataAccessReadHitSRAMRReq = Wire(Vec(BlockWords,new SRAMBundleA(NSets*NWays)))
  DataAccessReadHitSRAMRReq.foreach(_.setIdx := Cat(coreReq_st1.setIdx,OHToUInt(TagAccess.io.waymaskHit_st1)))

  val waitforL2flush_st2 = RegInit(false.B)
  val flushstall = coreReqControl_st0_noen.isFlush || coreReqControl_st0_noen.isInvalidate || waitforL2flush
  //todo cannot handle when there still exist inflight L2 rsp

  when(io.coreReq.fire() && (coreReqControl_st0_noen.isInvalidate || coreReqControl_st0_noen.isFlush)){
    waitforL2flush := true.B
  }.elsewhen(memRspIsFluOrInv){
    waitforL2flush := false.B
  }
  val invalidatenodirty = coreReq_st1_valid && (coreReqControl_st1_Q.io.deq.bits.isInvalidate || coreReqControl_st1_Q.io.deq.bits.isFlush) && !coreReqTagHasDirty_st1
  when(invalidatenodirty && waitforL2flush){
    waitforL2flush_st2 := true.B
  }.elsewhen (memRspIsFluOrInv) {
    waitforL2flush_st2 := false.B
  }

  when(coreReqControl_st0.isWrite && MshrAccess.io.mshrStatus_st0 =/= 0.U){
    inflightReadWriteMiss := true.B
  }.elsewhen(inflightReadWriteMiss && MshrAccess.io.mshrStatus_st0 === 0.U ){
    inflightReadWriteMiss := false.B
  }

  coreReq_st1_ready := false.B
  when(coreReqControl_st1_Q.io.deq.bits.isRead || coreReqControl_st1_Q.io.deq.bits.isWrite){
    when(TagAccess.io.hit_st1) {
      when(coreRsp_st2.io.enq.ready && coreRsp_st2_valid_from_coreReq_Reg.io.enq.ready&& !(MshrAccess.io.missRspOut.valid && !secondaryFullReturn)) {
        coreReq_st1_ready := true.B
      }
    }.otherwise{//Miss
      when(coreReqControl_st1_Q.io.deq.bits.isRead){
        when(MshrAccess.io.missReq.ready && MemReqArb.io.in(1).ready && (mshrProbeStatus === 0.U || mshrProbeStatus === 2.U)//即memReq_Q.io.enq.ready
          && !(MshrAccess.io.missRspOut.valid && !secondaryFullReturn)){
          coreReq_st1_ready := true.B
        }
      }.otherwise{//isWrite
        //TODO before 7.30: add hit in-flight miss
        when(coreRsp_Q.io.enq.ready && MemReqArb.io.in(1).ready && !(MshrAccess.io.missRspOut.valid && !secondaryFullReturn)&& !inflightreadwritemiss_w){//memReq_Q.io.enq.ready
          coreReq_st1_ready := true.B
        }
      }
    }//.otherwise{//coreReq is not valid
    //  coreReq_st1_ready := true.B
    //}
  }.elsewhen(coreReqControl_st1_Q.io.deq.bits.isInvalidate){
    when(!coreReqTagHasDirty_st1 && MshrAccess.io.empty && WshrAccess.io.empty&& !flushstall){
      coreReq_st1_ready := true.B
    }
  }.elsewhen(coreReqControl_st1_Q.io.deq.bits.isFlush){
    when(!coreReqTagHasDirty_st1 && WshrAccess.io.empty && !flushstall){
      coreReq_st1_ready := true.B
    }
  }.elsewhen(coreReqControl_st1_Q.io.deq.bits.isWaitMSHR){
    when(MshrAccess.io.empty){
      coreReq_st1_ready := true.B
    }
  }.otherwise{//TODO: amo
    coreReq_st1_ready := true.B
  }

  // ******     l1_data_cache::coreReq_pipe1_invORflu      ******
  val waitMSHRCoreRsp_st1 = coreReq_st1_valid && coreReqControl_st1_Q.io.deq.bits.isWaitMSHR && MshrAccess.io.empty
  val fluCoreRsp_st1 = coreReq_st1_valid && coreReqControl_st1_Q.io.deq.bits.isFlush &&
    !coreReqTagHasDirty_st1 && WshrAccess.io.empty
  val invCOreRsp_st1 = coreReq_st1_valid && coreReqControl_st1_Q.io.deq.bits.isInvalidate &&
    !coreReqTagHasDirty_st1 && MshrAccess.io.empty && WshrAccess.io.empty


  val InvOrFluMemReqValid_st1 = coreReq_st1_valid && (coreReqControl_st1_Q.io.deq.bits.isInvalidate || coreReqControl_st1_Q.io.deq.bits.isFlush) && coreReqTagHasDirty_st1//&& !InvOrFluAlreadyflush

  val InvOrFluMemReq = Wire(new WshrMemReq)
  val L2flush = Wire(new WshrMemReq)
  InvOrFluMemReq.a_opcode := Mux(invalidatenodirty,L2flush.a_opcode, TLAOp_PutFull)//PutFullData:Get
  InvOrFluMemReq.a_param := Mux(invalidatenodirty,L2flush.a_param,0.U) //regular write
  InvOrFluMemReq.a_source := DontCare //wait for WSHR
  InvOrFluMemReq.a_addr := RegNext(Cat(TagAccess.io.dirtyTag_st1.get,
    TagAccess.io.dirtySetIdx_st0.get, 0.U((WordLength - TagBits - SetIdxBits).W)))
  InvOrFluMemReq.a_mask := VecInit(Seq.fill(BlockWords)(Fill(BytesOfWord,1.U)))
  //InvOrFluMemReq.a_data :=
  InvOrFluMemReq.hasCoreRsp := waitforL2flush_st2
  InvOrFluMemReq.coreRspInstrId := coreReq_st1.instrId

  L2flush.a_opcode := TLAOp_Flush
  L2flush.a_param := Mux(coreReqControl_st1_Q.io.deq.bits.isInvalidate,TLAParam_Inv,TLAParam_Flush)
  L2flush.a_source := DontCare
  L2flush.a_addr := Cat(TagAccess.io.dirtyTag_st1.get,
    RegNext(TagAccess.io.dirtySetIdx_st0.get), 0.U((WordLength - TagBits - SetIdxBits).W))
  L2flush.a_mask := VecInit(Seq.fill(BlockWords)(Fill(BytesOfWord,1.U)))
  L2flush.hasCoreRsp := true.B
  L2flush.coreRspInstrId := coreReq_st1.instrId
  L2flush.a_data := DontCare

  TagAccess.io.invalidateAll := coreReq_st1_valid && coreReqControl_st1_Q.io.deq.bits.isInvalidate && !coreReqTagHasDirty_st1
  // ******     l1_data_cache::memRsp_pipe0_cycle      ******
  memRsp_Q.io.enq <> io.memRsp
  //val memRsp_st1_ready = Wire(Bool())
  // tagAllocateWrite fork hand shake
  val tagReqValidCtrl = RegInit(true.B)
  val tagReqReadyCtrl = RegInit(false.B)
  val tagAllocateWriteReady = Wire(Bool())
  val tagAllocateWriteReady_mod = Mux(tagReqReadyCtrl, true.B, tagAllocateWriteReady)
  val tagAllocateWriteFire: Bool = TagAccess.io.allocateWrite.valid && tagAllocateWriteReady_mod
  when(tagAllocateWriteFire && !memRsp_Q.io.deq.fire) {
    tagReqValidCtrl := false.B
  }.elsewhen(!tagReqValidCtrl && memRsp_Q.io.deq.fire) {
    tagReqValidCtrl := true.B
  }
  when(tagAllocateWriteFire && !memRsp_Q.io.deq.fire) {
    tagReqReadyCtrl := true.B
  }.elsewhen(tagReqReadyCtrl && memRsp_Q.io.deq.fire) {
    tagReqReadyCtrl := false.B
  }

  val memRspIsWrite: Bool = memRsp_Q.io.deq.bits.d_opcode === 0.U//AccessAck
  val memRspIsRead: Bool = memRsp_Q.io.deq.bits.d_opcode === 1.U//AccessAckData


  memRsp_Q.io.deq.ready := memRspIsWrite || memRspIsFluOrInv ||
    (memRspIsRead && tagAllocateWriteReady_mod && MshrAccess.io.missRspIn.ready && coreRsp_Q.io.enq.ready)

  WshrAccess.io.popReq.valid := memRsp_Q.io.deq.valid && memRspIsWrite
  WshrAccess.io.popReq.bits := memRsp_Q.io.deq.bits.d_source(SetIdxBits+log2Up(NWshrEntry)-1,SetIdxBits)

  // ******     l1_data_cache::memRsp_pipe1_cycle      ******
  val memRsp_st1 = Reg(new DCacheMemRsp)
  //val memRsp_st1_valid = RegInit(false.B) early definition
  //val memRsp_st1_fire = memRsp_st1_ready && memRsp_st1_valid

  //when(memRsp_Q.io.deq.fire ^ memRsp_st1_fire) {
  //  memRsp_st1_valid := memRsp_Q.io.deq.fire
  //}
  //1-bit FSM
  val tagReplaceStatus = RegInit(false.B)
  when(tagReplaceStatus === false.B){
    when(TagAccess.io.needReplace.get){
      tagReplaceStatus := true.B
    }
  }.otherwise{//tagReplaceStatus === true.B
    when(MemReqArb.io.in(0).ready ){//memReq_Q.io.enq.ready){
      tagReplaceStatus := false.B
    }
  }

  when(tagReplaceStatus === false.B){
    tagAllocateWriteReady := !TagAccess.io.needReplace.get
  }.otherwise{//tagReplaceStatus === true.B
    tagAllocateWriteReady := MemReqArb.io.in(0).ready //memReq_Q.io.enq.ready
  }

  val missRspSetIdx_st1 = memRsp_st1.d_source(SetIdxBits-1,0)
  val dataReplaceReadValid = RegNext(TagAccess.io.allocateWrite.valid) &&
    tagReplaceStatus === false.B &&
    TagAccess.io.needReplace.get
  val DataAccessReplaceReadSRAMRReq = Wire(Vec(BlockWords, new SRAMBundleA(NSets * NWays)))
  DataAccessReplaceReadSRAMRReq.foreach(_.setIdx := Cat(missRspSetIdx_st1,OHToUInt(TagAccess.io.waymaskReplacement_st1)))

  val dataFillVaild = RegNext(TagAccess.io.allocateWrite.valid) &&
    tagReplaceStatus === false.B &&
    !TagAccess.io.needReplace.get//This place diff from dataReplaceReadValid
  val DataAccessMissRspSRAMWReq: Vec[SRAMBundleAW[UInt]] = Wire(Vec(BlockWords, new SRAMBundleAW(UInt(8.W), NSets * NWays, BytesOfWord)))
  DataAccessMissRspSRAMWReq.foreach(_.setIdx := Cat(missRspSetIdx_st1, OHToUInt(TagAccess.io.waymaskReplacement_st1)))
  DataAccessMissRspSRAMWReq.foreach(_.waymask.get := Fill(BytesOfWord, true.B))
  for (i <- 0 until BlockWords) {
    DataAccessMissRspSRAMWReq(i).data := memRsp_st1.d_data(i).asTypeOf(Vec(BytesOfWord, UInt(8.W)))
  }

  //为了data SRAM的读出周期，这个寄存器搭配dirtyReplace_st2使用
  val dirtyReplace_st1 = Wire(new WshrMemReq)
  dirtyReplace_st1.a_opcode := 0.U//PutFullData
  dirtyReplace_st1.a_param := 0.U//regular write
  dirtyReplace_st1.a_source := DontCare//wait for WSHR in next next cycle
  dirtyReplace_st1.a_addr := RegNext(TagAccess.io.a_addrReplacement_st1.get)
  dirtyReplace_st1.a_mask := VecInit(Seq.fill(BlockWords)(Fill(BytesOfWord,1.U)))
  dirtyReplace_st1.a_data := DontCare//wait for data SRAM in next cycle
  dirtyReplace_st1.hasCoreRsp := false.B
  dirtyReplace_st1.coreRspInstrId := DontCare

  when(memRsp_Q.io.deq.valid && memRspIsRead){
    memRsp_st1 := memRsp_Q_st0
  }

  // ******     missRspIn      ******
  MshrAccess.io.missRspIn.valid := memRsp_Q.io.deq.valid && memRspIsRead && coreRsp_Q.io.enq.ready
  MshrAccess.io.missRspIn.bits.instrId := memRsp_Q.io.deq.bits.d_source(SetIdxBits+log2Up(NMshrEntry)-1,SetIdxBits)

  // ******      tag write      ******
  TagAccess.io.allocateWrite.valid := Mux(tagReqValidCtrl,memRsp_Q.io.deq.valid && memRspIsRead,false.B)
  TagAccess.io.allocateWrite.bits.setIdx := memRsp_Q.io.deq.bits.d_source(SetIdxBits-1,0)
  //TagAccess.io.allocateWriteData_st1 to be connected in memRsp_pipe2_cycle
  probereadAllocateWriteConflict := io.coreReq.valid && RegNext(TagAccess.io.allocateWrite.valid)
  // ******     l1_data_cache::memRsp_pipe2_cycle      ******
  //missRspFromMshr_st1 := MshrAccess.io.missRspOut.valid//suffix _st2 is on another path comparing to cacheHit
  missRspTI_st1 := MshrAccess.io.missRspOut.bits.targetInfo.asTypeOf(new VecMshrTargetInfo)
  val missRspBA_st1 = MshrAccess.io.missRspOut.bits.blockAddr

  TagAccess.io.allocateWriteData_st1 := get_tag(MshrAccess.io.missRspOut.bits.blockAddr)
  TagAccess.io.allocateWriteTagSRAMWValid_st1 := RegNext(TagAccess.io.allocateWrite.valid) && tagAllocateWriteReady

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
    DataAccess.io.w.req.valid := Mux(dataFillVaild,
      true.B,  //READ miss resp
      writeHit_st1 & getBankEn.io.perBankValid(i))       //WRITE hit
    DataAccess.io.w.req.bits := Mux(RegNext(memRsp_Q.io.deq.valid && memRspIsRead),DataAccessMissRspSRAMWReq(i),DataAccessWriteHitSRAMWReq(i))
    DataAccess.io.r.req.valid := readHit_st1 || dataReplaceReadValid || dataAccessInvOrFluRValid
    when(dataReplaceReadValid){
      DataAccess.io.r.req.bits := DataAccessReplaceReadSRAMRReq(i)
    }.elsewhen(dataAccessInvOrFluRValid){
      DataAccess.io.r.req.bits := DataAccessInvOrFluSRAMRReq(i)
    }.otherwise{
      DataAccess.io.r.req.bits := DataAccessReadHitSRAMRReq(i)
    }
    Cat(DataAccess.io.r.resp.data.reverse)
  }
  val DataAccessReadSRAMRRsp: Vec[UInt] = VecInit(DataAccessesRRsp)
  // ******      data crossbar(Mem order to Core order)     ******
  val coreRsp_st2_dataMemOrder = Wire(Vec(BlockWords, UInt(WordLength.W)))
  val coreRsp_st2_dataCoreOrder = Wire(Vec(NLanes, UInt(WordLength.W)))

  coreRsp_st2_dataMemOrder := Mux(readHit_st2_valid, DataAccessReadSRAMRRsp, coreRsp_st2.io.deq.bits.data) //memRsp for latter
  for (i <- 0 until NLanes) {
    coreRsp_st2_dataCoreOrder(i) := coreRsp_st2_dataMemOrder(coreRsp_st2_perLaneAddr(i).blockOffset)
  }

  //val coreRsp_data_map_per_byte = Wire(Vec(NLanes,UInt(WordLength.W)))
  //recData.io.OriData := coreRsp_st2_dataCoreOrder
  //recData.io.offsetMask := coreRsp_st2_perLaneAddr.map(a => a.wordOffset1H)
  //coreRsp_data_map_per_byte := recData.io.DataOut
  //val coreRsp_data_map_per_byte_memrsp = Wire(Vec(NLanes,UInt(WordLength.W)))
  //recDataMemRsp.io.OriData := memRsp_st1.d_data
  //recDataMemRsp.io.offsetMask := missRspTI_st1.perLaneAddr.map(a => a.wordOffset1H)
  //coreRsp_data_map_per_byte_memrsp := recDataMemRsp.io.DataOut

  // ******      core rsp
  when(cacheHit_st1 && ((coreReq_Q.io.deq.fire) || injectTagProbe) && coreRsp_st2_valid_from_coreReq_Reg.io.enq.ready) {
    //coreRsp_st2_valid := true.B
    coreRsp_st2.io.enq.valid := true.B
    coreRsp_st2.io.enq.bits.data := DontCare
    coreRsp_st2.io.enq.bits.isWrite := coreReqControl_st1_Q.io.deq.bits.isWrite
  }.elsewhen(MshrAccess.io.missRspOut.valid){
    //coreRsp_st2_valid := true.B
    coreRsp_st2.io.enq.valid := true.B
    coreRsp_st2.io.enq.bits.data := memRsp_st1.d_data
    coreRsp_st2.io.enq.bits.isWrite := false.B
  }.elsewhen(waitMSHRCoreRsp_st1 || fluCoreRsp_st1 || invCOreRsp_st1){
    coreRsp_st2.io.enq.valid := true.B
    coreRsp_st2.io.enq.bits.data := DontCare
    coreRsp_st2.io.enq.bits.isWrite := false.B
  }.elsewhen(readHit_st2_valid && !coreRsp_Q.io.enq.ready){
    coreRsp_st2.io.enq.valid := true.B
    coreRsp_st2.io.enq.bits.data := DataAccessesRRsp
    coreRsp_st2.io.enq.bits.isWrite := false.B
  }.otherwise{
    coreRsp_st2.io.enq.valid := false.B
    coreRsp_st2.io.enq.bits.data := DontCare
    coreRsp_st2.io.enq.bits.isWrite := false.B
  }
  when(((cacheHit_st1) && (coreReq_Q.io.deq.fire) ) ||
    waitMSHRCoreRsp_st1 || fluCoreRsp_st1 || invCOreRsp_st1){

    coreRsp_st2.io.enq.bits.instrId := coreReq_st1.instrId
    coreRsp_st2.io.enq.bits.activeMask := coreReq_st1.perLaneAddr.map(_.activeMask)
    coreRsp_st2_perLaneAddr := coreReq_st1.perLaneAddr
  }.elsewhen(MshrAccess.io.missRspOut.valid){
    coreRsp_st2.io.enq.bits.instrId := missRspTI_st1.instrId
    coreRsp_st2.io.enq.bits.activeMask := missRspTI_st1.perLaneAddr.map(_.activeMask)
    coreRsp_st2_perLaneAddr := missRspTI_st1.perLaneAddr
  }.otherwise{
    coreRsp_st2.io.enq.bits.instrId := missRspTI_st1.instrId
    coreRsp_st2.io.enq.bits.activeMask := missRspTI_st1.perLaneAddr.map(_.activeMask)
    coreRsp_st2_perLaneAddr := coreRsp_st2_perLaneAddr
  }

  //assert(!(coreReq_st1_valid && missRspFromMshr_st1),s"when coreReq_st1 valid, hit/miss cant invalid in same cycle")
  val coreRsp_st2_valid_from_coreReq = Wire(Bool())
  val coreRsp_st2_valid_from_memRsp = Wire(Bool())
  val coreRsp_st2_valid_from_memReq = Wire(Bool())
  //RegEnable(coreReq_st1_valid &&
    //(readHit_st1 || writeHit_st1), coreRsp_Q.io.enq.ready)//, coreRsp_Q.io.enq.fire)//(coreReqControl_st1.isFlush && )
  coreRsp_st2_valid_from_coreReq_Reg.io.enq.valid := coreReq_st1_valid
  coreRsp_st2_valid_from_coreReq_Reg.io.enq.bits := (readHit_st1 || writeHit_st1)
  coreRsp_st2_valid_from_coreReq_Reg.io.deq.ready := !(coreRsp_st2_valid_from_memReq || coreRsp_st2_valid_from_memRsp) && coreRsp_Q.io.enq.ready


  val coreRspFromMemReq = Wire(new DCacheCoreRsp)
  //val coreReq_Reg = RegNext(coreRsp_st2_valid_from_coreReq_Reg)
  val coreReqmemConflict_Reg = RegInit(false.B)
  readHit_st2.io.deq.ready := !(coreRsp_st2_valid_from_memReq || coreRsp_st2_valid_from_memRsp) && coreRsp_Q.io.enq.ready


  //if coreReq and memRsp happened in one cycle, corereq will hold for one more cycle

  coreRsp_st2_valid_from_coreReq := coreRsp_st2_valid_from_coreReq_Reg.io.deq.bits && coreRsp_st2_valid_from_coreReq_Reg.io.deq.fire()

  coreRsp_st2_valid_from_memRsp := RegEnable(MshrAccess.io.missRspOut.valid , coreRsp_Q.io.enq.ready)
  coreRsp_st2.io.deq.ready := coreRsp_Q.io.enq.ready && !coreRsp_st2_valid_from_memReq//(coreRsp_st2_valid_from_memRsp || coreRsp_st2_valid_from_coreReq || coreRsp_st2_valid_from_memReq) && coreRsp_Q.io.enq.ready
  assert (!(coreRsp_st2_valid_from_coreReq && coreRsp_st2_valid_from_memRsp), s"cRsp from cReq and mRsp conflict")
  assert (!(coreRsp_st2_valid_from_coreReq && coreRsp_st2_valid_from_memReq), "cRsp from cReq and mReq conflict")
  assert (!(coreRsp_st2_valid_from_memReq && coreRsp_st2_valid_from_memRsp), "cRsp from mRsp and mReq conflict")
  coreRsp_st2_valid := coreRsp_st2_valid_from_coreReq || coreRsp_st2_valid_from_memRsp || coreRsp_st2_valid_from_memReq

  coreRsp_Q.io.deq <> io.coreRsp
  coreRsp_Q.io.enq.valid := coreRsp_st2_valid || (memRspIsFluOrInv && memRsp_Q.io.deq.fire())
  coreRsp_Q.io.enq.bits.isWrite := Mux(coreRsp_st2_valid_from_memReq,coreRspFromMemReq.isWrite,coreRsp_st2.io.deq.bits.isWrite)
  coreRsp_Q.io.enq.bits.activeMask := Mux(coreRsp_st2_valid_from_memReq,coreRspFromMemReq.activeMask,coreRsp_st2.io.deq.bits.activeMask)
  coreRsp_Q.io.enq.bits.instrId := Mux(coreRsp_st2_valid_from_memReq,coreRspFromMemReq.instrId,coreRsp_st2.io.deq.bits.instrId)

  coreRsp_Q.io.enq.bits.data := coreRsp_st2_dataCoreOrder//coreRsp_data_map_per_byte

  // ******      m_memReq_Q.m_Q.push_back      ******
  val dirtyReplace_st2 = Reg(new WshrMemReq)
  dirtyReplace_st2 := dirtyReplace_st1
  dirtyReplace_st1.a_data := DataAccessReadSRAMRRsp

  InvOrFluMemReq.a_data := DataAccessReadSRAMRRsp
  val flushL2 = Wire(Bool())
  val flushL2_Reg = RegEnable(flushL2,MemReqArb.io.in(2).fire())
  flushL2 :=  (invalidatenodirty && MemReqArb.io.in(2).ready && !flushL2_Reg)
  when(flushL2){
    InvOrFluAlreadyflush := true.B
  }.elsewhen(coreReqInvOrFluValid_st0){
    InvOrFluAlreadyflush := false.B
  }
  TagAccess.io.invalidateAll := flushL2 && coreReqControl_st1_Q.io.deq.bits.isInvalidate

  memReq_Q.io.enq <> MemReqArb.io.out
  MemReqArb.io.in(0).valid := tagReplaceStatus
  MemReqArb.io.in(0).bits := dirtyReplace_st1
  MemReqArb.io.in(1).valid := coreReq_st1_valid  && coreReq_Q.io.deq.fire() && ((writeMiss_st1 || readMiss_st1) && mshrProbeStatus === 0.U) && !injectTagProbe
  MemReqArb.io.in(1).bits := missMemReq
  MemReqArb.io.in(2).valid := Mux(invalidatenodirty,flushL2 && WshrAccess.io.empty,RegNext(InvOrFluMemReqValid_st1))
  MemReqArb.io.in(2).bits := InvOrFluMemReq

  // ******      l1_data_cache::memReq_pipe2_cycle      ******
  val memReq_st3 = Reg(new DCacheMemReq)
  val a_op_st3 = memReq_Q.io.deq.bits.a_opcode
  val memReqIsWrite_st3 = (a_op_st3 === TLAOp_PutFull) || ((a_op_st3 === TLAOp_PutPart) && memReq_Q.io.deq.bits.a_param === 0.U)
  val memReqIsRead_st3 = (a_op_st3 === TLAOp_Get) && memReq_Q.io.deq.bits.a_param === 0.U

  //memReq_Q.io.deq.bits.a_addr >> (WordLength - TagBits - SetIdxBits)
  val wshrProtect = WshrAccess.io.conflict && (memReqIsWrite_st3 || memReqIsRead_st3) && memReq_Q.io.deq.valid// && io.memReq.ready
  val cRspBlockedOrWshrFull = ((!coreRsp_Q.io.enq.ready && memReq_Q.io.deq.bits.hasCoreRsp)
    || !WshrAccess.io.pushReq.ready) && memReqIsWrite_st3
  val wshrPass = !wshrProtect && !cRspBlockedOrWshrFull
  val PushWshrValid = wshrPass && memReq_Q.io.deq.fire() && memReqIsWrite_st3
  // val WshrPushPopConflict = PushWshrValid && WshrAccess.io.popReq.valid
  // val wshrPushPopConflictReg = RegNext(WshrPushPopConflict)
  val pushReqbA = memReq_Q.io.deq.bits.a_addr >> (WordLength - TagBits - SetIdxBits)
  // val pushReqbAReg = RegNext(pushReqbA)
  WshrAccess.io.pushReq.bits.blockAddr := pushReqbA//Mux(wshrPushPopConflictReg, pushReqbAReg, pushReqbA)

  WshrAccess.io.pushReq.valid := PushWshrValid//Mux(wshrPushPopConflictReg,true.B,Mux(WshrPushPopConflict,false.B,PushWshrValid))//wshrPass && memReq_Q.io.deq.fire() && memReqIsWrite_st3
  coreRsp_st2_valid_from_memReq := WshrAccess.io.pushReq.valid && memReq_Q.io.deq.bits.hasCoreRsp && !coreRsp_st2_valid_from_memRsp

  memReq_Q.io.deq.ready := wshrPass && io.memReq.ready && !coreRsp_st2_valid_from_memRsp

  when(wshrPass && memReq_Q.io.deq.fire()) {
    memReq_st3 := memReq_Q.io.deq.bits
  }

  assert(NMshrEntry >= NWshrEntry,"MshrEntry should be more than NWshrEntry")
  val memReqSetIdx_st2 = memReq_Q.io.deq.bits.a_addr(WordLength - TagBits -1,WordLength - TagBits - SetIdxBits)
  when(memReqIsWrite_st3 && memReq_Q.io.deq.fire()){
    memReq_st3.a_source := Cat("d0".U, WshrAccess.io.pushedIdx, memReqSetIdx_st2)
    //memReq_st3.a_source := Cat("d0".U, 0.U((log2Up(NMshrEntry)-log2Up(NWshrEntry)).W), WshrAccess.io.pushedIdx, coreReq_st1.setIdx)
  }
  val coreRspFromMemReqMask_st1 = coreReq_st1.perLaneAddr.map(_.activeMask)
  val coreReqMask_Q = Module(new Queue(Vec(NLanes, Bool()),8,false ,false))
  coreReqMask_Q.io.enq.bits := coreRspFromMemReqMask_st1
  coreReqMask_Q.io.enq.valid := memReq_Q.io.enq.valid//writeMiss_st1 && MemReqArb.io.in(1).ready
  coreReqMask_Q.io.deq.ready := memReq_Q.io.deq.ready && coreRsp_Q.io.enq.ready

  coreRspFromMemReq.data := DontCare
  coreRspFromMemReq.isWrite := true.B
  //st指令的regIdx对SM流水线提交级无意义，且memReq_Q没有传输该数据的通道
  coreRspFromMemReq.instrId := memReq_Q.io.deq.bits.coreRspInstrId
  coreRspFromMemReq.activeMask := coreReqMask_Q.io.deq.bits//coreRsp_st2.io.deq.bits.activeMask//VecInit(Seq.fill(NLanes)(true.B))
  // memReq(st3)
  io.memReq.bits := memReq_st3

  val memReq_valid = RegInit(false.B)
  when(memReq_Q.io.deq.fire ^ io.memReq.fire){
    memReq_valid := memReq_Q.io.deq.fire
  }
  io.memReq.valid := memReq_valid
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
    blockIdxMasked(i) := Mux(io.perLaneValid(i),blockIdx1H(i),0.U)//blockIdx1H(i) & Fill(NLane, io.perLaneValid(i))
  }
  val perBankReq_Bin: Vec[UInt] = Wire(Vec(NBank, UInt(NLane.W)))//transpose of blockIdxMasked
  for(i <- 0 until NBank){
    perBankReq_Bin(i) := Reverse(Cat(blockIdxMasked.map(_(i))))
  }
  io.perBankValid := perBankReq_Bin.map(_.orR)
  io.perBankBlockIdx := perBankReq_Bin.map(PriorityEncoder(_))
}
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

import L1Cache.L1TagAccess
import SRAMTemplate.SRAMTemplate
import chisel3._
import chisel3.util._
import config.config.Parameters

/*Version Note
* DCacheCoreReq spec changed, shift some work to LSU
* //byteEn
//00 for byte
//01 for half word, alignment required
//11 for word, alignment required
*
* TL memReq port adapted
*
* this design havent take NBanks = BlockWords simplification in to account
* this design consider only NBanks = BlockWords case at miss rsp to data bank transition
*
* TagAccess no need to acquire data from _st1 reg, as it can hold result with SRAMTemplate
* */

class DCachePerLaneAddr(implicit p: Parameters) extends DCacheBundle{
  val activeMask = Bool()
  val blockOffset = UInt(BlockOffsetBits.W)
  val wordOffset1H = UInt(BytesOfWord.W)
}
class DCacheCoreReq(implicit p: Parameters) extends DCacheBundle{
  //val ctrlAddr = new Bundle{
  val instrId = UInt(WIdBits.W)//TODO length unsure
  val isWrite = Bool()//Vec(NLanes, Bool())
  val tag = UInt(TagBits.W)
  val setIdx = UInt(SetIdxBits.W)
  val perLaneAddr = Vec(NLanes, new DCachePerLaneAddr)
  val data = Vec(NLanes, UInt(WordLength.W))
}

class DCacheCoreRsp(implicit p: Parameters) extends DCacheBundle{
  val instrId = UInt(WIdBits.W)
  val isWrite = Bool()
  val data = Vec(NLanes, UInt(WordLength.W))
  val activeMask = Vec(NLanes, Bool())//UInt(NLanes.W)
}

class DCacheMemRsp(implicit p: Parameters) extends DCacheBundle{
  //val d_opcode = UInt(3.W)// AccessAckData only
  //val d_param
  //val d_size
  val d_source = UInt(WIdBits.W)//cut off head log2Up(NSms) bits at outside
  val d_addr = UInt(WordLength.W)
  val d_data = Vec(BlockWords, UInt(WordLength.W))//UInt((WordLength * BlockWords).W)
}

class DCacheMemReq(implicit p: Parameters)extends DCacheBundle{
  val a_opcode = UInt(3.W)
  //val a_param = UInt(3.W)
  //val a_size
  val a_source = UInt(WIdBits.W)
  val a_addr = UInt(WordLength.W)
  //val isWrite = Bool()//Merged into a_opcode
  val a_data = Vec(BlockWords, UInt(WordLength.W))
  //there is BW waste, only at most NLanes of a_data elements would be filled, BlockWords is usually larger than NLanes
  val a_mask = Vec(BlockWords,Bool())
}
/*class DCacheMshrBlockAddr(implicit p: Parameters)extends DCacheBundle{
  val instrId = UInt(WIdBits.W)
  val blockAddr = UInt(bABits.W)
}*/

class DCacheMshrTargetInfo(implicit p: Parameters)extends DCacheBundle{
  //val instrId = UInt(WIdBits.W)
  val isWrite = Bool()
  val perLaneAddr = Vec(NLanes, new DCachePerLaneAddr)
}

abstract class DataCacheIO(implicit p: Parameters) extends DCacheModule{
  val io = IO(new Bundle {
    val coreReq = Flipped(DecoupledIO(new DCacheCoreReq))
    val coreRsp = DecoupledIO(new DCacheCoreRsp)
    val memRsp = Flipped(DecoupledIO(new DCacheMemRsp))
    val memReq = DecoupledIO(new DCacheMemReq)
  })
}
class DataCache(implicit p: Parameters) extends DataCacheIO{
  // ******     important submodules     ******
  val BankConfArb = Module(new BankConflictArbiter)
  //val bankConflict_reg = Reg(Bool())
  val MshrAccess = Module(new MSHR(new DCacheMshrTargetInfo)(p))
  val missRspFromMshr_st1 = Wire(Bool())
  val missRspTI_st1 = Wire(new DCacheMshrTargetInfo)
  val TagAccess = Module(new L1TagAccess(set=NSets, way=NWays, tagBits=TagBits))
  val DataCorssBar = Module(new DataCrossbar)
  val WriteDataBuf = Module(new WDB)

  // ******     queues     ******
  val coreRsp_Q_entries :Int = NLanes
  val coreRsp_Q = Module(new Queue(new DCacheCoreRsp,entries = coreRsp_Q_entries,flow=false,pipe=false))
  //this queue also work as a pipeline reg, so cannot flow
  val coreRsp_QAlmstFull = Wire(Bool())
  val memRsp_Q = Module(new Queue(new DCacheMemRsp,entries = 2,flow=false,pipe=false))
  //flow will make predecessor read hit conflict with successor memRsp
  val memRsp_Q_st1 = memRsp_Q.io.deq.bits

  //val coreReqHolding = RegInit(false.B)
  val coreReq_st1 = RegEnable(io.coreReq.bits, io.coreReq.fire())
  val coreReq_st2 = RegNext(coreReq_st1)
  val coreReqInstrId_st3 = RegNext(coreReq_st2.instrId)
  val writeMiss_st3 = Reg(Bool())
  val coreReqActvMask_st3 = Mux(writeMiss_st3,
    //WRITE Miss case
    RegNext(VecInit(coreReq_st2.perLaneAddr.map(_.activeMask))),
    //READ/WRITE Hit case
    ShiftRegister(BankConfArb.io.activeLane,2))
  val coreReqIsWrite_st3 = RegNext(coreReq_st2.isWrite)

  // ******      Arbiter      ******
  val arbReqMux_CoreOrMshr = Wire(new coreReqArb)
  val arbReqMshr = Wire(new coreReqArb)
  arbReqMshr.perLaneAddr := missRspTI_st1.perLaneAddr
  arbReqMshr.isWrite := missRspTI_st1.isWrite
  arbReqMshr.enable := missRspFromMshr_st1
  val arbReqCore = Wire(new coreReqArb)
  arbReqCore.perLaneAddr := coreReq_st1.perLaneAddr
  arbReqCore.isWrite := coreReq_st1.isWrite
  val cacheHit_st1 = Wire(Bool())
  arbReqCore.enable := cacheHit_st1
  arbReqMux_CoreOrMshr := Mux(missRspFromMshr_st1, arbReqMshr, arbReqCore)
  BankConfArb.io.coreReqArb := arbReqMux_CoreOrMshr

  // ******      tag read      ******
  val missRspWriteEnable = Wire(Bool())

  TagAccess.io.r.req.valid := io.coreReq.fire()// || (coreReqHolding && coreReq_ok_to_in)
  TagAccess.io.r.req.bits.setIdx := io.coreReq.bits.setIdx
  TagAccess.io.tagFromCore_st1 := coreReq_st1.tag//Mux(coreReqHolding, coreReqTag_st2, coreReqCtrlAddr_st1.tag)
  TagAccess.io.coreReqReady := io.coreReq.ready
  // ******      tag write      ******
  TagAccess.io.w.req.valid := missRspWriteEnable//multiple for secondary miss
  TagAccess.io.w.req.bits(
    data=get_tag(memRsp_Q_st1.d_addr),
    setIdx=get_setIdx(memRsp_Q_st1.d_addr),
    waymask = 1.U)
  //???woc, why := dont work, but .apply does

  // ******     pipeline regs      ******
  cacheHit_st1 := TagAccess.io.hit_st1 && RegNext(io.coreReq.fire())
  val cacheMiss_st1 = !TagAccess.io.hit_st1 && RegEnable(io.coreReq.fire(),MshrAccess.io.missReq.ready)
  //RegEnable indicate whether the signal is consumed
  val wayIdxAtHit_st1 = Wire(UInt(WayIdxBits.W))
  wayIdxAtHit_st1 := OHToUInt(TagAccess.io.waymaskHit_st1)
  val wayIdxAtHit_st2 = RegNext(wayIdxAtHit_st1)
  val wayIdxReplace_st0 = Wire(UInt(WayIdxBits.W))
  wayIdxReplace_st0 := OHToUInt(TagAccess.io.waymaskReplacement)

  val writeFullWordBank_st1 = Cat(BankConfArb.io.addrCrsbarOut.map(_.wordOffset1H.andR))  //mem Order
  val writeTouchBank_st1 =    Cat(BankConfArb.io.addrCrsbarOut.map(_.wordOffset1H.orR))   //mem Order
  val writeSubWordBank_st1 = writeFullWordBank_st1 ^ writeTouchBank_st1               //mem Order
  val byteEn_st1 : Bool = writeFullWordBank_st1 =/= writeTouchBank_st1

  val readHit_st1 = cacheHit_st1 & !coreReq_st1.isWrite
  val writeHit_st1 = cacheHit_st1 & coreReq_st1.isWrite
  val writeMiss_st1 = cacheMiss_st1 & coreReq_st1.isWrite//TODO extend with bankConflict?
  val writeHitSubWord_st1 = writeHit_st1 & byteEn_st1
  val writeMissSubWord_st1 = writeMiss_st1 & byteEn_st1

  val writeMiss_st2 = RegNext(writeMiss_st1)
  val writeMissSubWord_st2 = RegNext(writeMissSubWord_st1)
  writeMiss_st3 := writeMiss_st2

  val cacheHit_st2 = RegInit(false.B)
  cacheHit_st2 := cacheHit_st1 || (cacheHit_st2 && RegNext(BankConfArb.io.bankConflict))
  // bankConflict置高的周期比coreRsp需要输出的周期少一个，而其置高的第一个周期有cacheHit_st1做控制。所以这里使用RegNext(bankConflict)做控制
  val readHit_st2 = cacheHit_st2 && !coreReq_st2.isWrite
  val readHit_st3 = RegNext(readHit_st2)
  val writeHit_st2 = cacheHit_st2 && coreReq_st2.isWrite
  val writeHit_st3 = RegNext(writeHit_st2)

  val bankConflict_st2 = RegNext(BankConfArb.io.bankConflict)
  val arbDataCrsbarSel1H_st2 = RegNext(BankConfArb.io.dataCrsbarSel1H)//st2 both in cc path and mc path
  val arbAddrCrsbarOut_st2 = RegNext(BankConfArb.io.addrCrsbarOut)
  val arbArrayEn_st2 = RegNext(BankConfArb.io.dataArrayEn)

  val arbDataCrsbarSel1H_st3 = RegNext(arbDataCrsbarSel1H_st2)

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
  MshrAccess.io.missReq.valid := cacheMiss_st1 &
    (!coreReq_st1.isWrite | (coreReq_st1.isWrite & byteEn_st1))

  val mshrMissReqTI = Wire(new DCacheMshrTargetInfo)
  mshrMissReqTI.isWrite := coreReq_st1.isWrite
  mshrMissReqTI.perLaneAddr := coreReq_st1.perLaneAddr
  MshrAccess.io.missReq.bits.instrId := coreReq_st1.instrId
  MshrAccess.io.missReq.bits.blockAddr := Cat(coreReq_st1.tag,coreReq_st1.setIdx)
  MshrAccess.io.missReq.bits.targetInfo := mshrMissReqTI.asUInt()

  MshrAccess.io.missRspIn.valid := memRsp_Q.io.deq.valid && !cacheHit_st2 && !ShiftRegister(io.coreReq.bits.isWrite&&io.coreReq.fire(),2)
  MshrAccess.io.missRspIn.bits.blockAddr := get_blockAddr(memRsp_Q.io.deq.bits.d_addr)

  missRspFromMshr_st1 := MshrAccess.io.missRspOut.valid//suffix _st2 is on another path comparing to cacheHit
  missRspTI_st1 := MshrAccess.io.missRspOut.bits.targetInfo.asTypeOf(new DCacheMshrTargetInfo)
  val missRspBA_st1 = MshrAccess.io.missRspOut.bits.blockAddr
  val missRspTILaneMask_st2 = RegNext(BankConfArb.io.activeLane)
  val memRspInstrId_st2 = RegNext(MshrAccess.io.missRspOut.bits.instrId)
  //val readMissRsp_st2 = missRspFromMshr_st2 & !missRspTI.isWrite
  val readMissRspCnter = if(BankOffsetBits!=0) RegInit(0.U((BankOffsetBits+1).W)) else Reg(UInt())//TODO use Counter
  MshrAccess.io.missRspOut.ready :=
  //READ miss Rsp
    (!missRspTI_st1.isWrite & !BankConfArb.io.bankConflict & !coreRsp_QAlmstFull
      && (if(BankOffsetBits!=0) !missRspWriteEnable else true.B)) ||
  //WRITE miss subword Rsp
    (missRspTI_st1.isWrite & !WriteDataBuf.io.wdbAlmostFull)

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
  val DataAccessesRRsp = (0 until NBanks).map {i =>
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
  // only serve for READ hit flow and READ miss Rsp flow
  coreRsp_Q.io.deq <> io.coreRsp
  coreRsp_Q.io.enq.valid := readHit_st3 || readMissRsp_st2 || writeMiss_st3 || writeHit_st3
  coreRsp_Q.io.enq.bits.isWrite := Mux(writeMissRsp_st2,
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
  coreRsp_QAlmstFull := coreRsp_Q.io.count === coreRsp_Q_entries.asUInt - 2.U

  // ******      core req ready
  //coreReq_ok_to_in := MshrAccess.io.missReq.ready && !missRspFromMshr_st2 && !io.memRsp.valid && coreRsp_Q.io.enq.ready && !Arbiter.io.bankConflict
  io.coreReq.ready := !(BankConfArb.io.bankConflict && (cacheHit_st1 || cacheHit_st2)) &
    !missRspFromMshr_st1 & !(io.memRsp.valid && !ShiftRegister(io.coreReq.bits.isWrite&&io.coreReq.fire(),2)) &
    !(cacheHit_st1 & coreRsp_QAlmstFull) &
    !(coreReq_st1.isWrite & WriteDataBuf.io.wdbAlmostFull) &
    !(readHit_st2 & coreReq_st1.isWrite) &
    (MshrAccess.io.missReq.ready || (!MshrAccess.io.missReq.ready && io.coreReq.bits.isWrite))

  // ******      wdb input
  WriteDataBuf.io.inputBus.valid := writeMiss_st2 | writeHit_st2 |
    (writeMissRsp_st2 & missRspTI_st1.isWrite)//TODO 好像没写missRsp这块
  val DataCrsbarToWdb_st2 = Wire(Vec(NLanes,Vec(BytesOfWord,UInt(8.W))))
  DataCrsbarToWdb_st2 := DataCorssBar.io.DataOut.asTypeOf(DataCrsbarToWdb_st2)
  val perWordByteMask = Wire(Vec(BlockWords,UInt(BytesOfWord.W)))
  (0 until NBanks).foreach{ iofB =>
    (0 until BankWords).foreach { iinB =>
      perWordByteMask(iinB*NBanks+iofB) :=
        Mux(arbArrayEn_st2(iofB),
          Mux(arbAddrCrsbarOut_st2(iofB).bankOffset.getOrElse(0.U)===iinB.asUInt,//getElse的时候这里恒为真
            arbAddrCrsbarOut_st2(iofB).wordOffset1H,
            Fill(BytesOfWord,false.B)),
          Fill(BytesOfWord,false.B))
      WriteDataBuf.io.inputBus.bits.mask(iinB*NBanks+iofB) :=
        Mux(arbArrayEn_st2(iofB),
          Mux(arbAddrCrsbarOut_st2(iofB).bankOffset.getOrElse(0.U)===iinB.asUInt,
            Mux(writeMissSubWord_st2,arbAddrCrsbarOut_st2(iofB).wordOffset1H,Fill(BytesOfWord,true.B)),
            Fill(BytesOfWord,false.B)),
          Fill(BytesOfWord,false.B))
      (0 until BytesOfWord).foreach{ iofb =>
        WriteDataBuf.io.inputBus.bits.data(iinB*NBanks+iofB)(iofb) :=
          Mux(arbArrayEn_st2(iofB),
            Mux(arbAddrCrsbarOut_st2(iofB).bankOffset.getOrElse(0.U)===iinB.asUInt,
              Mux(perWordByteMask(iinB*NBanks+iofB)(iofb),
                DataCrsbarToWdb_st2(iofB)(iofb),DataAccessesRRsp(iofB)(iofb)),
              0.U),//TODO Dont care
            0.U)//TODO Dont care
      }
    }
  }
  WriteDataBuf.io.inputBus.bits.instrId := coreReq_st2.instrId
  WriteDataBuf.io.inputBus.bits.addr := Cat(coreReq_st2.tag,coreReq_st2.setIdx,0.U((WordLength-TagBits-SetIdxBits).W))
  WriteDataBuf.io.inputBus.bits.bankConflict := bankConflict_st2
  WriteDataBuf.io.inputBus.bits.subWordMissReq := writeMissSubWord_st2
  WriteDataBuf.io.inputBus.bits.subWordMissRsp := writeMissRsp_st2 & missRspTI_st1.isWrite

  // ******      mem req
  val MemReqArb = Module(new Arbiter(new DCacheMemReq, 2))

  val MshrMiss2Mem = MshrAccess.io.miss2mem.bits
  val MshrMemReq = Wire(new DCacheMemReq)
  MshrMemReq.a_opcode := TLAOp_Get
  MshrMemReq.a_addr := Cat(MshrMiss2Mem.blockAddr,0.U((WordLength-bABits).W))
  MshrMemReq.a_data := VecInit(Seq.fill(BlockWords)(0.U))//TODO Dont care
  MshrMemReq.a_source := MshrMiss2Mem.instrId
  MshrMemReq.a_mask := VecInit(Seq.fill(BlockWords)(true.B))//useless port
  MshrAccess.io.miss2mem.ready := MemReqArb.io.in(0).ready

  val wdbMemReq = Wire(new DCacheMemReq)
  val wDataBufOut = WriteDataBuf.io.outputBus.bits
  wdbMemReq.a_opcode := Mux(Cat(wDataBufOut.mask).andR,TLAOp_PutFull,TLAOp_PutPart)
  wdbMemReq.a_addr := wDataBufOut.addr
  wdbMemReq.a_data := wDataBufOut.data.asTypeOf(Vec(BlockWords,UInt(WordLength.W)))
  wdbMemReq.a_source := wDataBufOut.instrId
  wdbMemReq.a_mask := wDataBufOut.mask
  WriteDataBuf.io.outputBus.ready := MemReqArb.io.in(1).ready


  io.memReq <> MemReqArb.io.out
  MemReqArb.io.in(0).valid := MshrAccess.io.miss2mem.valid
  MemReqArb.io.in(0).bits := MshrMemReq
  MemReqArb.io.in(1).valid := WriteDataBuf.io.outputBus.valid
  MemReqArb.io.in(1).bits := wdbMemReq
}
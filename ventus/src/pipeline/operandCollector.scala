package pipeline

import chisel3._
import chisel3.util._
import parameters._
import IDecode._

class WriteVecCtrl extends Bundle{
  val wb_wvd_rd=(Vec(num_thread,UInt(xLen.W)))
  val wvd_mask=Vec(num_thread,Bool())
  val wvd=Bool()
  val reg_idxw=UInt(5.W)
  val warp_id=UInt(depth_warp.W)
}
class WriteScalarCtrl extends Bundle{
  val wb_wxd_rd=(UInt(xLen.W))
  val wxd=Bool()
  val reg_idxw=UInt(5.W)
  val warp_id=UInt(depth_warp.W)
}

class crossbar2CU extends Bundle{
  //val CUId = UInt(log2Ceil(num_collectorUnit).W)
  val regOrder = UInt(2.W)
  val data = Vec(num_thread, UInt(xLen.W))
  val v0 = Vec(num_thread, UInt(xLen.W))
}
class readArbiterInnerIO extends Bundle{
  val rsAddr = UInt(depth_regBank.W)
  val bankID = UInt(log2Ceil(num_bank).W)
}

class CU2Arbiter extends readArbiterInnerIO {
  val rsType = UInt(2.W)
}

class issueIO extends Bundle{
  val alu_src1 = Vec(num_thread, UInt(xLen.W))
  val alu_src2 = Vec(num_thread, UInt(xLen.W))
  val alu_src3 = Vec(num_thread, UInt(xLen.W))
  val mask = Vec(num_thread, Bool())
  val control = new CtrlSigs
}
/**
 *One of the number of num_warp collector Units, instantiating this class in operand collector for num_warps.
 */
class collectorUnit extends Module{
  val io = IO(new Bundle{
    val control = Flipped(Decoupled(new CtrlSigs))
    val bankIn = Vec(4, Flipped(Decoupled(new crossbar2CU)))
    //operand to be issued, alternatively vector and scalar
    val issue = Decoupled(new issueIO)
    val outArbiterIO = Vec(4, Decoupled(new CU2Arbiter))

  })
  val controlReg = Reg(new CtrlSigs)
  io.issue.bits.control := controlReg

  // rsType == 0: PC or mask(for op4)
  // rsType == 1: scalar
  // rsType == 2: Vec
  // rsType == 3: Imm
  val rsType = Reg(Vec(4, UInt(2.W)))
  val ready = Reg(Vec(4, Bool()))
  val valid = Reg(Vec(4, Bool()))
  val regIdx = Reg(Vec(4, UInt(5.W)))
  val rsReg = RegInit(VecInit(Seq.fill(3)(VecInit(Seq.fill(num_thread)(0.U(xLen.W)))))) //op1, op2 and op3
  val mask = Reg(Vec(num_thread, Bool()))

  val rsTypeWire = Wire(Vec(4, UInt(2.W)))
  val readyWire = Wire(Vec(4, Bool()))
  val regIdxWire = Wire(Vec(4, UInt(5.W)))

  val imm = Module(new ImmGen)

  val s_idle :: s_add :: s_out :: Nil = Enum(3)
  val state = RegInit(s_idle)

  //Lookup table for address transformation
  val bankIdLookup = (0 until num_warp + 32).map { x =>
    (x -> x % num_bank)
  }.map { x => (x._1.U -> x._2.U) }
  val addrLookup = (0 until 32).map { x =>
    (x -> x / (num_bank))
  }.map { x => (x._1.U -> x._2.U) }

  //reading the register bank for those operand which type is not an immediate
  /*
  TODO:
    收集单元到仲裁器的请求提前一拍的话,bank conflict 情况下应该保持仲裁器的输入正确
   */
  for (i <- 0 until 4) {
//    when(readyWire(i) === 0.U) {
      io.outArbiterIO(i).bits.bankID := MuxLookup(Mux(io.control.fire&&(state===s_idle), io.control.bits.wid+regIdxWire(i), controlReg.wid+regIdx(i)), 0.U, bankIdLookup)
      io.outArbiterIO(i).bits.rsAddr := MuxLookup(Mux(io.control.fire&&(state===s_idle), regIdxWire(i), regIdx(i)), 0.U, addrLookup) +
        Mux(io.control.fire&&(state===s_idle),io.control.bits.wid, controlReg.wid) * (32 / num_bank).U
      io.outArbiterIO(i).bits.rsType := Mux(io.control.fire&&(state===s_idle), rsTypeWire(i), rsType(i))

  }
  (0 until 4).foreach(i => {
    io.bankIn(i).ready := (state === s_add) && (ready(i)===0.U)
  })
  for (i <- 0 until 4) {
    io.outArbiterIO(i).valid :=
      MuxLookup(state, false.B,
        Array(s_idle->(io.control.fire && (readyWire(i)===0.U)),
              s_add->((valid(i) === true.B) && (ready(i)===false.B))
        ))
  }
//  io.issue.valid := (valid.asUInt === ready.asUInt) && ready.asUInt.andR
  io.issue.valid := state===s_out
  io.control.ready := (state===s_idle && !valid.asUInt.orR)

  when(state === s_idle) {
    when(io.control.fire && !valid.asUInt.orR ){
      state := s_add
    }.otherwise{state := s_idle}
  }.elsewhen (state === s_add) {
    when(valid.asUInt =/= ready.asUInt ) {
      state := s_add
      //    }.elsewhen(io.bankIn.fire){
      //      state := s_out
    }.otherwise{state := s_out}
  }.elsewhen(state === s_out) {
    when(io.issue.fire){
      state := s_idle
    }.otherwise{state := s_out}
  }.otherwise{state := s_idle}

  regIdxWire := 0.U.asTypeOf(regIdxWire)
  rsTypeWire := 0.U.asTypeOf(rsTypeWire)
  readyWire := 0.U.asTypeOf(readyWire)
  imm.io.inst := MuxLookup(state, 0.U,
    Array(s_idle->io.control.bits.inst,
          s_add->controlReg.inst
    ))
  imm.io.sel := MuxLookup(state, 0.U,
    Array(s_idle -> io.control.bits.sel_imm,
      s_add -> controlReg.sel_imm
    ))
  switch(state){
    is(s_idle){
//      valid.foreach(_:=false.B)
//      ready.foreach(_:=false.B)
      when(io.control.fire){
        controlReg := io.control.bits
        //using an iterable variable to indicate reg_idx signals
        regIdxWire(0) := io.control.bits.reg_idx1
        regIdxWire(1) := io.control.bits.reg_idx2
        regIdxWire(2) := MuxLookup(io.control.bits.sel_alu3, 0.U,
          Array(
            A3_PC -> Mux(io.control.bits.branch===B_R, io.control.bits.reg_idx1, io.control.bits.reg_idx3),
            A3_VRS3 -> io.control.bits.reg_idx3,
            A3_SD -> Mux(io.control.bits.isvec, io.control.bits.reg_idx3, io.control.bits.reg_idx2),
            A3_FRS3 -> io.control.bits.reg_idx3
          ))
        regIdxWire(3) := 0.U // mask of vector instructions
        regIdx(0) := regIdxWire(0)
        regIdx(1) := regIdxWire(1)
        regIdx(2) := regIdxWire(2)
        regIdx(3) := 0.U // mask of vector instructions
        valid.foreach(_:= true.B)
        ready.foreach(_:= false.B)
        //using an iterable variable to indicate sel_alu signals
        rsTypeWire(0) := io.control.bits.sel_alu1
        rsTypeWire(1) := io.control.bits.sel_alu2
        rsTypeWire(2) := MuxLookup(io.control.bits.sel_alu3, 0.U,
          Array(
            A3_PC -> Mux(io.control.bits.branch===B_R, 1.U, 3.U),
            A3_VRS3 -> 2.U,
            A3_SD -> Mux(io.control.bits.isvec, 2.U, 1.U),
            A3_FRS3 -> 1.U
          ))
        rsTypeWire(3) := 0.U(2.W) //mask
        rsType(0) := rsTypeWire(0)
        rsType(1) := rsTypeWire(1)
        rsType(2) := rsTypeWire(2)
        rsType(3) := rsTypeWire(3)
        //if the operand1 or operand2 is an immediate, elaborate it and enable the ready bit
        //op1 is immediate or don't care
        when(io.control.bits.sel_alu1 === A1_IMM /*|| io.control.bits.sel_alu1 === A1_X*/) {
          rsReg(0).foreach(_:= imm.io.out)
          ready(0) := 1.U
          readyWire(0) := 1.U
        }.elsewhen(io.control.bits.sel_alu1===A1_PC){
          rsReg(0).foreach(_:= io.control.bits.pc)
          ready(0) := 1.U
          readyWire(0) := 1.U
        }
        //op2 is immediate or don't care
        when(io.control.bits.sel_alu2===A2_IMM /*|| io.control.bits.sel_alu2 === A2_X*/){
          rsReg(1).foreach(_:= imm.io.out)
          ready(1) := 1.U
          readyWire(1) := 1.U
        }.elsewhen(io.control.bits.sel_alu2===A2_SIZE){
          rsReg(1).foreach(_ := 4.U)
          ready(1) := 1.U
          readyWire(1) := 1.U
        }
        //When op3 is not cared. See DecodeUnit.scala
        when((io.control.bits.sel_alu3===A3_PC /*|| io.control.bits.sel_alu3===A3_X*/) && io.control.bits.branch=/=B_R){
          rsReg(2).foreach(_:= imm.io.out+io.control.bits.pc)
          ready(2) := 1.U
          readyWire(2) := 1.U
        }
        when(!io.control.bits.mask){
          (0 until num_thread).foreach(x=>{
           mask(x) := Mux(io.control.bits.isvec,true.B, !x.asUInt.orR)//this instruction is a Vector inst without mask or a Scalar inst.
          })
          ready(3) := 1.U
          readyWire(3) := 1.U
        }
//        readyWire(0) := (io.control.bits.sel_alu1===A1_IMM) || (io.control.bits.sel_alu1===A1_PC)
//        readyWire(1) := (io.control.bits.sel_alu2===A2_IMM) || (io.control.bits.sel_alu2===A2_SIZE)
//        readyWire(3) := !io.control.bits.mask
      }
    }
    is(s_add) {
      for (i <- 0 until 4) {
        when(io.bankIn(i).fire) {
          when(io.bankIn(i).bits.regOrder === 0.U) { //operand1
            rsReg(0) := MuxLookup(rsType(0), VecInit.fill(num_thread)(0.U(xLen.W)),
              Array(
//                A1_RS1 -> VecInit(Seq(io.bankIn(i).bits.data(0)) ++ Seq.fill(num_thread - 1)(0.U(xLen.W))),
                A1_RS1 -> VecInit.fill(num_thread)(io.bankIn(i).bits.data(0)),
                A1_VRS1 -> io.bankIn(i).bits.data)
            )
            ready(0) := 1.U
          }.elsewhen(io.bankIn(i).bits.regOrder === 1.U) { //operand2
            rsReg(1) := MuxLookup(rsType(1), VecInit.fill(num_thread)(0.U(xLen.W)),
              Array(
//                A2_RS2 -> VecInit(Seq(io.bankIn(i).bits.data(0)) ++ Seq.fill(num_thread - 1)(0.U(xLen.W))),
                A2_RS2 -> VecInit.fill(num_thread)(io.bankIn(i).bits.data(0)),
                A2_VRS2 -> io.bankIn(i).bits.data)
            )
            ready(1) := 1.U
          }.elsewhen(io.bankIn(i).bits.regOrder === 2.U) { //operand3
            rsReg(2) := MuxLookup(controlReg.sel_alu3, VecInit.fill(num_thread)(0.U(xLen.W)),
              Array(A3_PC -> VecInit.fill(num_thread)(imm.io.out + io.bankIn(i).bits.data(0)),
                A3_VRS3 -> io.bankIn(i).bits.data,
                A3_SD -> Mux(controlReg.isvec, io.bankIn(i).bits.data, VecInit.fill(num_thread)(io.bankIn(i).bits.data(0))),
                A3_FRS3 -> VecInit.fill(num_thread)(io.bankIn(i).bits.data(0))
              )
            )
            ready(2) := 1.U
          }.elsewhen(io.bankIn(i).bits.regOrder === 3.U) {
            (0 until num_thread).foreach(x => {
              mask(x) := io.bankIn(i).bits.v0(0).apply(x) //this instruction is an Vector with mask, the mask is read from vector register bank
            })
            ready(3) := 1.U
          }
        }
      }
    }
    is(s_out) {
      valid.foreach(_ := false.B)
      ready.foreach(_ := false.B)
    }
  }
  io.issue.bits.alu_src1 := rsReg(0)
  io.issue.bits.alu_src2 := rsReg(1)
  io.issue.bits.alu_src3 := rsReg(2)
  io.issue.bits.mask := mask
}

/**
 * Arbitrating which reading (TO DO: writing) request should
 * be send to register files
 */
class operandArbiter extends Module{
  val io = IO(new Bundle{
    val readArbiterIO = Vec(num_collectorUnit, Vec(4, Flipped(Decoupled(new CU2Arbiter))))
    val readArbiterOut = Vec(num_bank, Decoupled(new CU2Arbiter)) //address of registers to be read that in bank
    val readchosen = Output(Vec(num_bank, UInt((log2Ceil(4*num_collectorUnit)).W)))// which operand read request is chosen
    //    val writeArbiterIO = Decoupled(/*write arbiter, TBD   */)

  })
  val bankArbiterScalar = for(i<-0 until num_bank)yield{
    val x = Module(new RRArbiter(new CU2Arbiter, 4*num_collectorUnit))
    x
  }
//  val bankArbiterVector = for (i <- 0 until num_bank) yield {
//    val x = Module(new RRArbiter(new CU2Arbiter, 4 * num_collectorUnit))
//    x
//  }

  for (i <- 0 until num_bank) {
    //    mapping input signals from collector units to inputs of Arbiters
    for (j <- 0 until num_collectorUnit){
      for (k <- 0 until 4){
        bankArbiterScalar(i).io.in(j*4+k) <> io.readArbiterIO(j)(k)
      }
    }
  }

  //elaborate valid port of readArbiters
  for (i <- 0 until num_bank){
    for(j <- 0 until num_collectorUnit)
      for(k <- 0 until 4){
        bankArbiterScalar(i).io.in(j*4+k).valid := io.readArbiterIO(j)(k).valid &&
          (io.readArbiterIO(j)(k).bits.bankID === i.U) //&& (io.readArbiterIO(j)(k).bits.rsType === 1.U)
//        bankArbiterVector(i).io.in(j*4+k).valid := io.readArbiterIO(j)(k).valid &&
//          (io.readArbiterIO(j)(k).bits.bankID === i.U) //&& (io.readArbiterIO(j)(k).bits.rsType === 2.U)
        io.readArbiterIO(j)(k).ready := bankArbiterScalar(i).io.in(j*4+k).ready
      }
  }
  (0 until num_bank).foreach(x =>{
    io.readArbiterOut(x) <> bankArbiterScalar(x).io.out
    io.readchosen(x) <> bankArbiterScalar(x).io.chosen
  })

  //Address of writeback transformation

}

class crossBar extends Module{
  val io = IO(new Bundle {
    val chosen = Input(Vec(num_bank, UInt(log2Ceil(4 * num_collectorUnit).W)))
    val validArbiter = Input(Vec(num_bank, Bool()))
    val dataIn = Input(new Bundle{
      val rs = Vec(num_bank, Vec(num_thread, UInt(xLen.W)))
      val v0 = Vec(num_bank, Vec(num_thread,UInt((xLen).W)))
    })
    val out = Vec(num_collectorUnit, Vec(4, Decoupled(new crossbar2CU)))
  })
  val CUId = Wire(Vec(num_bank, UInt(log2Ceil(num_collectorUnit).W)))
  val regOrder = Wire(Vec(num_bank, UInt(2.W)))
  // There is not conflict from crossbar to collector units, so don't need to deal with stall.
  // However, in situation bank conflict occurs, some banks may have invalid output.
  (0 until num_bank).foreach(i=>{
    CUId(i) := io.chosen(i) >> 2.U
    regOrder(i) := io.chosen(i) % 4.U
  })
//  (0 until num_collectorUnit).foreach(i=>{
//    (0 until 4).foreach(j=>{
//      io.out(i)(j).valid := (CUId(i)===i.U) && io.validArbiter(i) && (regOrder(i)===j.U)
//      io.out(i)(j).bits.data := io.dataIn.rs(i)
//      io.out(i)(j).bits.v0 := io.dataIn.v0(i)
//      io.out(i)(j).bits.regOrder := regOrder(i)
//    })
//  })
  io.out.foreach(_.foreach(_.bits.data := 0.U.asTypeOf(Vec(num_thread, UInt(xLen.W)))))
  io.out.foreach(_.foreach(_.bits.v0 := 0.U.asTypeOf(Vec(num_thread, UInt(xLen.W)))))
  io.out.foreach(_.foreach(_.valid := false.B))
  io.out.foreach(_.foreach(_.bits.regOrder := 0.U))
  for( i <- 0 until num_bank){
    for(j <- 0 until num_collectorUnit){
      for(k <- 0 until 4){
        when((CUId(i)===j.U) && io.validArbiter(i) &&(regOrder(i)===k.U)){
          io.out(j)(k).bits.data := io.dataIn.rs(i)
          io.out(j)(k).bits.v0 := io.dataIn.v0(i)
          io.out(j)(k).valid := true.B
          io.out(j)(k).bits.regOrder := regOrder(i)
        }
      }
    }
  }
}

/**
 * Allocating the collector unit to new input instruction
 */
class instDemux extends Module{
  val io = IO(new Bundle{
    val in = Flipped(Decoupled(new CtrlSigs))
    val out = Vec(num_collectorUnit, Decoupled(new CtrlSigs))
    val widCmp = Input(Vec(num_collectorUnit, Bool()))
  })
    //Each data on out port is identical
    io.out.foreach(_.bits := io.in.bits)
  //For those out port which is ready, selecting one by bitwise priority.
  val outReady = VecInit(io.out.map(_.ready))
  for((v, i) <- io.out.zipWithIndex){
    v.valid := Mux(PriorityEncoder(outReady)===i.U, true.B, false.B) && io.in.valid
  }
  //If there isn't any warp id as same as input instruction, the instruction can be allocated a CU
  io.in.ready := !io.widCmp.reduce(_ | _) && outReady.asUInt.orR
}
class operandCollector extends Module{
  val io=IO(new Bundle {
    val control=Flipped(Decoupled(new CtrlSigs()))
    val out=Decoupled(new issueIO)
    val writeScalarCtrl=Flipped(DecoupledIO(new WriteScalarCtrl)) //should be used as decoupledIO
    val writeVecCtrl=Flipped(DecoupledIO(new WriteVecCtrl))
  })
  val collectorUnits = VecInit(Seq.fill(num_collectorUnit)(Module(new collectorUnit).io))
  val Arbiter = Module(new operandArbiter)
  val vectorBank = VecInit(Seq.fill(num_bank)(Module(new FloatRegFileBank).io))
  val scalarBank = VecInit(Seq.fill(num_bank)(Module(new RegFileBank).io))
  val crossBar = Module(new crossBar)
  val Demux = Module(new instDemux)
  //connecting Arbiters and banks
  (0 until num_collectorUnit).foreach(i => {collectorUnits(i).outArbiterIO <> Arbiter.io.readArbiterIO(i)})
  (0 until num_bank).foreach(i=>{
    vectorBank(i).rsidx := Arbiter.io.readArbiterOut(i).bits.rsAddr
    scalarBank(i).rsidx := Arbiter.io.readArbiterOut(i).bits.rsAddr
    Arbiter.io.readArbiterOut(i).ready := true.B
  })
  //connecting crossbar and banks, as well as signal readchosen. Readchosen needs to delay one tik to match bank reading
  crossBar.io.chosen := RegNext(Arbiter.io.readchosen)
  crossBar.io.validArbiter := RegNext(VecInit(Arbiter.io.readArbiterOut.map(_.valid)))
  for( i <- 0 until num_bank){
    when(RegNext(Arbiter.io.readArbiterOut(i).bits.rsType===1.U)){ //scalar
      crossBar.io.dataIn.rs(i) := VecInit.fill(num_thread)(scalarBank(i).rs)
      crossBar.io.dataIn.v0(i) := VecInit.fill(num_thread)(0.U(xLen.W))
    }.elsewhen(RegNext(Arbiter.io.readArbiterOut(i).bits.rsType===2.U)){//vector
      crossBar.io.dataIn.rs(i) := vectorBank(i).rs
      crossBar.io.dataIn.v0(i) := vectorBank(i).v0
    }.otherwise{
      crossBar.io.dataIn.rs(i) := 0.U.asTypeOf(crossBar.io.dataIn.rs(0))
      crossBar.io.dataIn.v0(i) := 0.U.asTypeOf(crossBar.io.dataIn.v0(0))
    }
  }
  //connecting crossbar and collector units
  (0 until num_collectorUnit).foreach(i => {collectorUnits(i).bankIn <> crossBar.io.out(i)})

  //CU allocation
  val widReg = RegInit(VecInit.fill(num_collectorUnit)(0.U(log2Ceil(num_collectorUnit).W)))
  val widCmp = Wire(Vec(num_collectorUnit, Bool()))
  //Since arbitration has finished in scoreboard, each instruction in this stage should fetch operand unless there is no more collect unit.
  (0 until num_collectorUnit).foreach( i => {
//    widCmp(i) := io.control.bits.wid===collectorUnits(i).wid
    widCmp(i) := 0.U
  })
  Demux.io.widCmp := widCmp
  Demux.io.in <> io.control
  for(i <- 0 until num_collectorUnit){collectorUnits(i).control <> Demux.io.out(i)}


  //writeback control
  val bankIdLookup = (0 until num_warp + 32).map { x =>
    (x -> x % num_bank)
  }.map { x => (x._1.U -> x._2.U)}
  val addrLookup = (0 until 32).map { x =>
    (x -> x / (num_bank))
  }.map { x => (x._1.U -> x._2.U) }
  val wbVecBankId = Wire(UInt(2.W))
  val wbScaBankId = Wire(UInt(2.W))
  val wbVecBankAddr = Wire(UInt(depth_regBank.W))
  val wbScaBankAddr = Wire(UInt(depth_regBank.W))

  wbVecBankId := MuxLookup(io.writeVecCtrl.bits.reg_idxw+io.writeVecCtrl.bits.warp_id, 0.U, bankIdLookup)
  wbVecBankAddr := MuxLookup(io.writeVecCtrl.bits.reg_idxw, 0.U, addrLookup) + io.writeVecCtrl.bits.warp_id*(32/num_bank).U
  wbScaBankId := MuxLookup(io.writeScalarCtrl.bits.reg_idxw+io.writeScalarCtrl.bits.warp_id, 0.U, bankIdLookup)
  wbScaBankAddr := MuxLookup(io.writeScalarCtrl.bits.reg_idxw, 0.U, addrLookup) + io.writeScalarCtrl.bits.warp_id*(32/num_bank).U

  vectorBank.foreach(x=>{
    x.rdidx := wbVecBankAddr
    x.rd := io.writeVecCtrl.bits.wb_wvd_rd
    x.rdwen := false.B
    x.rdwmask := io.writeVecCtrl.bits.wvd_mask
  })
  scalarBank.foreach(x=>{
    x.rdidx := wbScaBankAddr
    x.rd := io.writeScalarCtrl.bits.wb_wxd_rd
    x.rdwen := false.B
  })
  vectorBank(wbVecBankId).rdwen := io.writeVecCtrl.bits.wvd & io.writeVecCtrl.valid
  scalarBank(wbScaBankId).rdwen := io.writeScalarCtrl.bits.wxd & io.writeScalarCtrl.valid
  io.writeScalarCtrl.ready := true.B
  io.writeVecCtrl.ready := true.B

  //when all operands of an instruction has prepared, issue it.
  val issueArbiter = Module(new Arbiter((new issueIO), num_collectorUnit))
  issueArbiter.io.in <> VecInit(collectorUnits.map(_.issue))
  io.out <> issueArbiter.io.out

  //  //old code
  //  val vectorRegFile=VecInit(Seq.fill(num_bank)(Module(new FloatRegFileBank).io))
  //  val scalarRegFile=VecInit(Seq.fill(num_bank)(Module(new RegFileBank).io))
  //  val imm=Module(new ImmGen())
  //
  //  imm.io.inst:=io.control.inst
  //  imm.io.sel:=io.control.sel_imm
  //
  //  vectorRegFile.foreach(x => {
  //    x.rsidx:=io.control.reg_idx1
  //    x.rs2idx:=io.control.reg_idx2
  //    x.rs3idx:=io.control.reg_idx3
  //    x.rdidx:=io.writeVecCtrl.bits.reg_idxw
  //    x.rd:=io.writeVecCtrl.bits.wb_wfd_rd
  //    x.rdwen:=false.B
  //    x.rdwmask:=io.writeVecCtrl.bits.wfd_mask
  //    //y.rdwen:=io.writeCtrl.wfd
  //  })
  //  scalarRegFile.foreach(y=>{
  //    y.rs1idx:=io.control.reg_idx1
  //    y.rs2idx:=io.control.reg_idx2
  //    y.rs3idx:=io.control.reg_idx3
  //    y.rdIdx:=io.writeScalarCtrl.bits.reg_idxw
  //    y.rd:=io.writeScalarCtrl.bits.wb_wxd_rd
  //    y.rdwen:=false.B
  //    //y.rdwen:=io.writeCtrl.wxd
  //  })
  //  vectorRegFile(io.writeVecCtrl.bits.warp_id).rdwen:=io.writeVecCtrl.bits.wfd&io.writeVecCtrl.valid
  //  scalarRegFile(io.writeScalarCtrl.bits.warp_id).rdwen:=io.writeScalarCtrl.bits.wxd&io.writeScalarCtrl.valid
  //  io.writeScalarCtrl.ready:=true.B
  //  io.writeVecCtrl.ready:=true.B
  //
  //  (0 until num_thread).foreach(x=>{
  //    io.alu_src1(x):=MuxLookup(io.control.sel_alu1,0.U,Array(A1_RS1->scalarRegFile(io.control.wid).rs1,A1_VRS1->(vectorRegFile(io.control.wid).rs1(x)),A1_IMM->imm.io.out,A1_PC->io.control.pc))//io.control.reg_idx1))
  //    io.alu_src2(x):=MuxLookup(io.control.sel_alu2,0.U,Array(A2_RS2->scalarRegFile(io.control.wid).rs2,A2_IMM->imm.io.out,A2_VRS2->vectorRegFile(io.control.wid).rs2(x),A2_SIZE->4.U))
//      io.alu_src3(x):=MuxLookup(io.control.sel_alu3,0.U,
//        Array(
//          A3_PC->Mux(io.control.branch===B_R,(imm.io.out+scalarRegFile(io.control.wid).rs1),(io.control.pc+imm.io.out)),
//          A3_VRS3->vectorRegFile(io.control.wid).rs3(x),
//          A3_SD->Mux(io.control.isvec,vectorRegFile(io.control.wid).rs3(x),scalarRegFile(io.control.wid).rs2),
//          A3_FRS3->(scalarRegFile(io.control.wid).rs3)))
  //
  //    io.mask(x):=Mux(io.control.mask,vectorRegFile(io.control.wid).v0(0).apply(x),Mux(io.control.isvec,true.B,!x.asUInt.orR))
  //
  //  })

}


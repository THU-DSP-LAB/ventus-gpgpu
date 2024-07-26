package pipeline

import chisel3._
import chisel3.util._
import top.parameters._
import IDecode._

class WriteVecCtrl extends Bundle{
  val wb_wvd_rd=(Vec(num_thread,UInt(xLen.W)))
  val wvd_mask=Vec(num_thread,Bool())
  val wvd=Bool()
  val reg_idxw=UInt((regidx_width + regext_width).W)
  val warp_id=UInt(depth_warp.W)
  val spike_info=if(SPIKE_OUTPUT) Some(new InstWriteBack) else None
}
class WriteScalarCtrl extends Bundle{
  val wb_wxd_rd=(UInt(xLen.W))
  val wxd=Bool()
  val reg_idxw=UInt((regidx_width + regext_width).W)
  val warp_id=UInt(depth_warp.W)
  val spike_info=if(SPIKE_OUTPUT) Some(new InstWriteBack) else None
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
    val sgpr_base = Input(Vec(num_warp, UInt((SGPR_ID_WIDTH + 1).W)))
    val vgpr_base = Input(Vec(num_warp, UInt((VGPR_ID_WIDTH + 1).W)))

  })
  val controlReg = Reg(new CtrlSigs)
  io.issue.bits.control := controlReg

  // rsType == 0: PC or mask(for op4)
  // rsType == 1: scalar
  // rsType == 2: Vec
  // rsType == 3: Imm
  val rsType = Reg(Vec(4, UInt(2.W)))
  val ready = RegInit(VecInit.fill(4)(false.B))
  val valid = RegInit(VecInit.fill(4)(false.B))
  val regIdx = Reg(Vec(4, UInt((regidx_width + regext_width).W)))
  val rsReg = RegInit(VecInit(Seq.fill(3)(VecInit(Seq.fill(num_thread)(0.U(xLen.W)))))) //op1, op2 and op3
  val mask = Reg(Vec(num_thread, Bool()))
  val rsRead = Wire(Vec(num_thread,UInt(xLen.W)))

  val rsTypeWire = Wire(Vec(4, UInt(2.W)))
  val readyWire = Wire(Vec(4, Bool()))
  val regIdxWire = Wire(Vec(4, UInt(8.W)))
  val validWire = Wire(Vec(4, Bool()))

  val customCtrlReg = RegInit(false.B)
  val customCtrlWire = Wire(Bool())

  val imm = Module(new ImmGen)

  val s_idle :: s_add :: s_out :: Nil = Enum(3)
  val state = RegInit(s_idle)
  // Lookup table for address transformation
  val bankIdLookup = (0 until num_warp + 256).map { x =>
    (x -> x % num_bank)
  }.map { x => (x._1.U -> x._2.U) }
  val addrLookupScalar = (0 until num_warp).map { x =>
    (x -> (io.sgpr_base(x) >> log2Ceil(num_bank)).asUInt)
  }.map { x => (x._1.U -> x._2) }
  val addrLookupVector = (0 until num_warp).map { x =>
    (x -> (io.vgpr_base(x) >> log2Ceil(num_bank)).asUInt)
  }.map { x => (x._1.U -> x._2) }

  //reading the register bank for those operand which type is not an immediate
  for (i <- 0 until 4) {

    io.outArbiterIO(i).bits.bankID := Mux(io.control.fire && (state === s_idle),
      io.control.bits.wid(log2Ceil(num_bank)-1, 0) + regIdxWire(i)(log2Ceil(num_bank)-1, 0),
      controlReg.wid(log2Ceil(num_bank)-1, 0) + regIdx(i)(log2Ceil(num_bank)-1, 0))
    io.outArbiterIO(i).bits.rsType := Mux(io.control.fire && (state === s_idle), rsTypeWire(i), rsType(i))

    when(Mux(io.control.fire && (state === s_idle), rsTypeWire(i), rsType(i)) === 1.U) {
      when(io.control.fire && (state === s_idle)) {
        io.outArbiterIO(i).bits.rsAddr := (io.sgpr_base(io.control.bits.wid) >> log2Ceil(num_bank).U).asUInt + (regIdxWire(i) >> log2Ceil(num_bank).U)
      }.otherwise {
        io.outArbiterIO(i).bits.rsAddr := (io.sgpr_base(controlReg.wid) >> log2Ceil(num_bank).U).asUInt + (regIdx(i) >> log2Ceil(num_bank).U)
      }
    }.elsewhen(Mux(io.control.fire && (state === s_idle), rsTypeWire(i), rsType(i)) === 2.U) {
      when(io.control.fire && (state === s_idle)) {
        io.outArbiterIO(i).bits.rsAddr := (io.vgpr_base(io.control.bits.wid) >> log2Ceil(num_bank).U).asUInt + (regIdxWire(i) >> log2Ceil(num_bank).U)
      }.otherwise {
        io.outArbiterIO(i).bits.rsAddr := (io.vgpr_base(controlReg.wid) >> log2Ceil(num_bank).U).asUInt + (regIdx(i) >> log2Ceil(num_bank).U)
      }
    }.otherwise {
      when(io.control.fire && (state === s_idle)) {
        io.outArbiterIO(i).bits.rsAddr := (io.sgpr_base(io.control.bits.wid) >> log2Ceil(num_bank).U).asUInt + (regIdxWire(i) >> log2Ceil(num_bank).U)
      }.otherwise {
        io.outArbiterIO(i).bits.rsAddr := (io.sgpr_base(controlReg.wid) >> log2Ceil(num_bank).U).asUInt + (regIdx(i) >> log2Ceil(num_bank).U)
      }
    }
  }
  (0 until 4).foreach(i => {
    io.bankIn(i).ready := (state === s_add  && (ready(i)===0.U)) || (io.control.fire && (readyWire(i)===0.U))
  })
  for (i <- 0 until 4) {
    io.outArbiterIO(i).valid :=
      MuxLookup(state, false.B)(
        Array(s_idle->(io.control.fire && (readyWire(i)===0.U)),
          s_add->((valid(i) === true.B) && (ready(i)===false.B))
        ))
  }
  //  io.issue.valid := (valid.asUInt === ready.asUInt) && ready.asUInt.andR
  io.issue.valid := state===s_out
  io.control.ready := (state===s_idle && !valid.asUInt.orR)

  when(state === s_idle) {

    when(io.control.fire){
      when(!readyWire.asUInt.andR) {state := s_add}
        .elsewhen(readyWire.asUInt.andR) {state := s_out}
        .otherwise{state := s_idle}
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

  validWire := 0.U.asTypeOf(validWire)
  regIdxWire := 0.U.asTypeOf(regIdxWire)
  rsTypeWire := 0.U.asTypeOf(rsTypeWire)
  readyWire := 0.U.asTypeOf(readyWire)

  customCtrlWire := false.B

  imm.io.inst := MuxLookup(state, 0.U)(
    Array(s_idle->io.control.bits.inst,
      s_add->controlReg.inst
    ))
  imm.io.imm_ext := MuxLookup(state, 0.U)(
    Array(s_idle -> io.control.bits.imm_ext,
      s_add -> controlReg.imm_ext
    ))
  imm.io.sel := MuxLookup(state, 0.U)(
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
        regIdxWire(2) := MuxLookup(io.control.bits.sel_alu3, 0.U)(
          Array(
            A3_PC -> Mux(io.control.bits.branch===B_R, io.control.bits.reg_idx1, io.control.bits.reg_idx3),
            A3_VRS3 -> io.control.bits.reg_idx3,
            A3_SD -> Mux(io.control.bits.isvec & (!io.control.bits.readmask), io.control.bits.reg_idx3, io.control.bits.reg_idx2),
            A3_FRS3 -> io.control.bits.reg_idx3
          ))
        regIdxWire(3) := 0.U // mask of vector instructions
        regIdx(0) := regIdxWire(0)
        regIdx(1) := regIdxWire(1)
        regIdx(2) := regIdxWire(2)
        regIdx(3) := 0.U // mask of vector instructions
        valid.foreach(_:= true.B)
        validWire.foreach(_:=true.B)
        ready.foreach(_:= false.B)
        //using an iterable variable to indicate sel_alu signals
        rsTypeWire(0) := io.control.bits.sel_alu1
        rsTypeWire(1) := io.control.bits.sel_alu2
        rsTypeWire(2) := MuxLookup(io.control.bits.sel_alu3, 0.U)(
          Array(
            A3_PC -> Mux(io.control.bits.branch===B_R, 1.U, 3.U),
            A3_VRS3 -> 2.U,
            A3_SD -> Mux(io.control.bits.isvec, 2.U, 1.U),
            A3_FRS3 -> 1.U
          ))
        rsTypeWire(3) := 0.U(2.W) //mask
        customCtrlWire := io.control.bits.custom_signal_0
        rsType(0) := rsTypeWire(0)
        rsType(1) := rsTypeWire(1)
        rsType(2) := rsTypeWire(2)
        rsType(3) := rsTypeWire(3)
        customCtrlReg := customCtrlWire
        //if the operand1 or operand2 is an immediate, elaborate it and enable the ready bit
        //op1 is immediate or don't care
        when(io.control.bits.sel_alu1 === A1_IMM) {
          rsReg(0).foreach(_:= imm.io.out)
          ready(0) := 1.U
          readyWire(0) := 1.U
        }.elsewhen(io.control.bits.sel_alu1===A1_PC){
          rsReg(0).foreach(_:= io.control.bits.pc)
          ready(0) := 1.U
          readyWire(0) := 1.U
        }
        //op2 is immediate or don't care
        when(io.control.bits.sel_alu2===A2_IMM){
          rsReg(1).foreach(_:= imm.io.out)
          ready(1) := 1.U
          readyWire(1) := 1.U
        }.elsewhen(io.control.bits.sel_alu2===A2_SIZE){
          rsReg(1).foreach(_ := 4.U)
          ready(1) := 1.U
          readyWire(1) := 1.U
        }
        //When op3 is not cared. See DecodeUnit.scala
        when((io.control.bits.sel_alu3===A3_PC) && io.control.bits.branch=/=B_R){
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
      }
    }

    is(s_out) {
      valid.foreach(_ := false.B)
      validWire.foreach(_:=false.B)
      ready.foreach(_ := false.B)
    }
  }
  rsRead.foreach(_ := 0.U)
  for (i <- 0 until 4) {
    when(io.bankIn(i).fire) {
      when(io.bankIn(i).bits.regOrder === 0.U) { //operand1
        rsRead := MuxLookup(Mux(io.control.fire, rsTypeWire(0), rsType(0)), VecInit.fill(num_thread)(0.U(xLen.W)))(
          Array(
            A1_RS1 -> Mux(
              MuxLookup(state, false.B)(
                Array(
                  s_idle -> regIdxWire(0).orR,
                  s_add -> regIdx(0).orR
                )), 
              VecInit.fill(num_thread)(io.bankIn(i).bits.data(0)),
              VecInit.fill(num_thread)(0.U(xLen.W))
            ),
            A1_VRS1 -> io.bankIn(i).bits.data)
        )
        when(Mux(io.control.fire,customCtrlWire,customCtrlReg)){
          rsReg(0).foreach( _:= rsRead(0) + imm.io.out)
        }.otherwise{
          rsReg(0) := rsRead
        }
        ready(0) := 1.U
      }.elsewhen(io.bankIn(i).bits.regOrder === 1.U) { //operand2
        rsReg(1) := MuxLookup(Mux(io.control.fire, rsTypeWire(1), rsType(1)), VecInit.fill(num_thread)(0.U(xLen.W)))(
          Array(
            A2_RS2 -> Mux(
              MuxLookup(state, false.B)(
                Array(
                  s_idle -> regIdxWire(1).orR,
                  s_add -> regIdx(1).orR
                )), 
              VecInit.fill(num_thread)(io.bankIn(i).bits.data(0)),
              VecInit.fill(num_thread)(0.U(xLen.W))
            ),
            A2_VRS2 -> io.bankIn(i).bits.data)
        )
        ready(1) := 1.U
      }.elsewhen(io.bankIn(i).bits.regOrder === 2.U) { //operand3
        rsReg(2) := MuxLookup(controlReg.sel_alu3, VecInit.fill(num_thread)(0.U(xLen.W)))(
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
    val readArbiterOutScalar = Vec(num_bank, Decoupled(new CU2Arbiter)) //address of registers to be read that in Scalar bank
    val readArbiterOutVector = Vec(num_bank, Decoupled(new CU2Arbiter)) //address of registers to be read that in Vector bank
    val readchosenScalar = Output(Vec(num_bank, UInt((log2Ceil(4*num_collectorUnit)).W)))// which operand read request is chosen
    val readchosenVector = Output(Vec(num_bank, UInt((log2Ceil(4*num_collectorUnit)).W)))// which operand read request is chosen
    //    val writeArbiterIO = Decoupled(/*write arbiter, TBD   */)

  })
  val bankArbiterScalar = for(i<-0 until num_bank)yield{
    val x = Module(new RRArbiter(new CU2Arbiter, 4*num_collectorUnit))
    x
  }
  val bankArbiterVector = for (i <- 0 until num_bank) yield {
    val x = Module(new RRArbiter(new CU2Arbiter, 4 * num_collectorUnit))
    x
  }

  for (i <- 0 until num_bank) {
    //    mapping input signals from collector units to inputs of Arbiters
    for (j <- 0 until num_collectorUnit){
      for (k <- 0 until 4){
        bankArbiterScalar(i).io.in(j*4+k) <> io.readArbiterIO(j)(k)
        bankArbiterVector(i).io.in(j*4+k) <> io.readArbiterIO(j)(k)
      }
    }
  }

  //elaborate valid port of readArbiters
  for (i <- 0 until num_bank){
    for(j <- 0 until num_collectorUnit)
      for(k <- 0 until 4){
        bankArbiterScalar(i).io.in(j*4+k).valid := io.readArbiterIO(j)(k).valid &&
          (io.readArbiterIO(j)(k).bits.bankID === i.U) && (io.readArbiterIO(j)(k).bits.rsType === 1.U)
        bankArbiterVector(i).io.in(j*4+k).valid := io.readArbiterIO(j)(k).valid &&
          (io.readArbiterIO(j)(k).bits.bankID === i.U) && (io.readArbiterIO(j)(k).bits.rsType === 2.U)
        io.readArbiterIO(j)(k).ready := bankArbiterScalar(i).io.in(j*4+k).ready
      }
  }
  (0 until num_bank).foreach(x =>{
    io.readArbiterOutScalar(x) <> bankArbiterScalar(x).io.out
    io.readArbiterOutVector(x) <> bankArbiterVector(x).io.out
    io.readchosenScalar(x) <> bankArbiterScalar(x).io.chosen
    io.readchosenVector(x) <> bankArbiterVector(x).io.chosen
  })

  //Address of writeback transformation

}

class crossBar  extends Module{
  val io = IO(new Bundle {
    val chosenScalar = Input(Vec(num_bank, UInt(log2Ceil(4 * num_collectorUnit).W)))
    val chosenVector = Input(Vec(num_bank, UInt(log2Ceil(4 * num_collectorUnit).W)))
    val validArbiterScalar = Input(Vec(num_bank, Bool()))
    val validArbiterVector = Input(Vec(num_bank, Bool()))
    val dataInScalar = Input(new Bundle{
      val rs = Vec(num_bank, UInt(xLen.W))
    })
    val dataInVector = Input(new Bundle {
      val rs = Vec(num_bank, Vec(num_thread, UInt(xLen.W)))
      val v0 = Vec(num_bank, Vec(num_thread, UInt((xLen).W)))
    })
    val out = Vec(num_collectorUnit, Vec(4, Decoupled(new crossbar2CU)))
  })
  val CUIdScalar = Wire(Vec(num_bank, UInt(log2Ceil(num_collectorUnit).W)))
  val CUIdVector = Wire(Vec(num_bank, UInt(log2Ceil(num_collectorUnit).W)))
  val regOrderScalar = Wire(Vec(num_bank, UInt(2.W)))
  val regOrderVector = Wire(Vec(num_bank, UInt(2.W)))

  // There is not conflict from crossbar to collector units, so don't need to deal with stall.
  // However, in situation bank conflict occurs, some banks may have invalid output.
  (0 until num_bank).foreach(i=>{
    CUIdScalar(i) := io.chosenScalar(i) >> 2.U
    regOrderScalar(i) := io.chosenScalar(i) % 4.U
    CUIdVector(i) := io.chosenVector(i) >> 2.U
    regOrderVector(i) := io.chosenVector(i) % 4.U
  })
  io.out.foreach(_.foreach(_.bits.data := 0.U.asTypeOf(Vec(num_thread, UInt(xLen.W)))))
  io.out.foreach(_.foreach(_.bits.v0 := 0.U.asTypeOf(Vec(num_thread, UInt(xLen.W)))))
  //  validDelay.foreach(_.foreach(_ := false.B))
  io.out.foreach(_.foreach(_.valid := (false.B)))
  io.out.foreach(_.foreach(_.bits.regOrder := 0.U))
  for( i <- 0 until num_bank){
    for(j <- 0 until num_collectorUnit){
      for(k <- 0 until 4){
        when((CUIdScalar(i)===j.U) && io.validArbiterScalar(i) &&(regOrderScalar(i)===k.U)){
          io.out(j)(k).bits.data := VecInit.fill(num_thread)(io.dataInScalar.rs(i))
          //          validDelay(j)(k) := true.B
          //          io.out(j)(k).valid := validDelay
          io.out(j)(k).valid := true.B
          io.out(j)(k).bits.regOrder := regOrderScalar(i)
        }
        when((CUIdVector(i) === j.U) && io.validArbiterVector(i) && (regOrderVector(i) === k.U)) {
          io.out(j)(k).bits.data := io.dataInVector.rs(i)
          io.out(j)(k).bits.v0 := io.dataInVector.v0(i)
          //          validDelay(j)(k) := true.B
          //          io.out(j)(k).valid := validDelay
          io.out(j)(k).valid := true.B
          io.out(j)(k).bits.regOrder := regOrderVector(i)
        }
        //      io.out(j)(k).valid := RegNext(validDelay(j)(k))
      }
    }
  }
}

/**
 * Allocating the collector unit to new input instruction
 */
class instDemux extends Module{
  val io = IO(new Bundle{
    val in = Vec(2, Flipped(Decoupled(new CtrlSigs)))
    val sgpr_baseIn = Input(Vec(num_warp, UInt((SGPR_ID_WIDTH + 1).W)))
    val vgpr_baseIn = Input(Vec(num_warp, UInt((VGPR_ID_WIDTH + 1).W)))
    val out = Vec(num_collectorUnit, Decoupled(new CtrlSigs))
    val sgpr_baseOut = Output(Vec(num_warp, UInt((SGPR_ID_WIDTH + 1).W)))
    val vgpr_baseOut = Output(Vec(num_warp, UInt((VGPR_ID_WIDTH + 1).W)))
    val widCmp = Input(Vec(num_collectorUnit, Bool()))
  })

  // Each data on out port is identical
  io.out.foreach(_.bits := io.in(0).bits)
  io.out.foreach(_.valid := 0.U)

  // For those out port ready, selecting one by bitwise priority.
  val outReady1 = VecInit(io.out.map(_.ready)).asUInt
  val outV_sel_oh = Wire(UInt(num_collectorUnit.W))
  outV_sel_oh :=  PriorityEncoderOH(outReady1)
  val outV_sel = OHToUInt(outV_sel_oh)
  val outReady2 = outReady1 & (~outV_sel_oh)
  val outX_sel = Wire(UInt(num_collectorUnit.W))
  outX_sel := PriorityEncoder(outReady2)
  io.in(0).ready := outReady1.orR
  io.in(1).ready := outReady2.orR



  for (i <- (0 until num_collectorUnit).reverse) {
    when(outReady1.asUInt.orR) {
      io.out(outV_sel).bits :=  io.in(0).bits
      io.out(outV_sel).valid :=  io.in(0).valid

    }
    when(outReady2.asUInt.orR) {
      io.out(outX_sel).bits :=  io.in(1).bits
      io.out(outX_sel).valid :=  io.in(1).valid

    }
  }

  io.sgpr_baseOut := io.sgpr_baseIn
  io.vgpr_baseOut := io.vgpr_baseIn

}
class operandCollector extends Module{
  val io=IO(new Bundle {
    val controlX=Flipped(Decoupled(new CtrlSigs()))
    val controlV=Flipped(Decoupled(new CtrlSigs()))
    val out=Vec(2, Decoupled(new issueIO))
    val writeScalarCtrl=Flipped(DecoupledIO(new WriteScalarCtrl)) //should be used as decoupledIO
    val writeVecCtrl=Flipped(DecoupledIO(new WriteVecCtrl))
    val sgpr_base = Input(Vec(num_warp,UInt((SGPR_ID_WIDTH+1).W)))
    val vgpr_base = Input(Vec(num_warp,UInt((VGPR_ID_WIDTH+1).W)))
  })
  val collectorUnits = VecInit(Seq.fill(num_collectorUnit)(Module(new collectorUnit).io))
  val Arbiter = Module(new operandArbiter)
  val vectorBank = VecInit(Seq.fill(num_bank)(Module(new FloatRegFileBank).io))
  val scalarBank = VecInit(Seq.fill(num_bank)(Module(new RegFileBank).io))
  val crossBar = Module(new crossBar)
  val Demux = Module(new instDemux)
  // connecting Arbiters and banks
  (0 until num_collectorUnit).foreach(i => {collectorUnits(i).outArbiterIO <> Arbiter.io.readArbiterIO(i)})
  (0 until num_bank).foreach(i=>{
    vectorBank(i).rsidx := Arbiter.io.readArbiterOutVector(i).bits.rsAddr
    scalarBank(i).rsidx := Arbiter.io.readArbiterOutScalar(i).bits.rsAddr
    Arbiter.io.readArbiterOutVector(i).ready := true.B
    Arbiter.io.readArbiterOutScalar(i).ready := true.B
  })
  // connecting crossbar and banks, as well as signal readchosen. Readchosen needs to delay one tick to match bank reading
  crossBar.io.chosenScalar := RegNext(Arbiter.io.readchosenScalar)
  crossBar.io.validArbiterScalar := RegNext(VecInit(Arbiter.io.readArbiterOutScalar.map(_.valid)))
  crossBar.io.chosenVector := RegNext(Arbiter.io.readchosenVector)
  crossBar.io.validArbiterVector := RegNext(VecInit(Arbiter.io.readArbiterOutVector.map(_.valid)))
  for( i <- 0 until num_bank){
    crossBar.io.dataInScalar.rs(i) := scalarBank(i).rs
    crossBar.io.dataInVector.rs(i) := vectorBank(i).rs
    crossBar.io.dataInVector.v0(i) := vectorBank(i).v0
  }
  // connecting crossbar and collector units
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
  Demux.io.in(0) <> io.controlV
  Demux.io.in(1) <> io.controlX
  Demux.io.sgpr_baseIn := io.sgpr_base
  Demux.io.vgpr_baseIn := io.vgpr_base
  for(i <- 0 until num_collectorUnit){
    collectorUnits(i).control <> Demux.io.out(i)
    collectorUnits(i).sgpr_base := Demux.io.sgpr_baseOut
    collectorUnits(i).vgpr_base := Demux.io.vgpr_baseOut
  }

  // writeback control
  // bankID = (wid + regIdx) % num_bank
  // rsAddr =  [gpr_base(i) + regIdx)] / num_bank
  val bankIdLookup = (0 until num_warp + 256).map { x =>
    (x -> x % num_bank)
  }.map { x => (x._1.U -> x._2.U) }
  val addrLookupScalar = (0 until num_warp).map { x =>
    (x -> (io.sgpr_base(x) >> log2Ceil(num_bank)).asUInt)
  }.map { x => (x._1.U -> x._2) }
  val addrLookupVector = (0 until num_warp).map { x =>
    (x -> (io.vgpr_base(x) >> log2Ceil(num_bank)).asUInt)
  }.map { x => (x._1.U -> x._2) }

  val wbVecBankId = Wire(UInt(2.W))
  val wbScaBankId = Wire(UInt(2.W))
  val wbVecBankAddr = Wire(UInt(depth_regBank.W))
  val wbScaBankAddr = Wire(UInt(depth_regBank.W))

  val sgprW = Wire(UInt(depth_regBank.W))
  sgprW := io.sgpr_base(io.writeScalarCtrl.bits.warp_id) >> log2Ceil(num_bank).U
  val regW = Wire(UInt(7.W))
  regW := io.writeScalarCtrl.bits.reg_idxw >> log2Ceil(num_bank).U

  wbVecBankId := io.writeVecCtrl.bits.reg_idxw(log2Ceil(num_bank)-1,0)+io.writeVecCtrl.bits.warp_id(log2Ceil(num_bank)-1,0)
  wbVecBankAddr := (io.vgpr_base(io.writeVecCtrl.bits.warp_id) >> log2Ceil(num_bank).U).asUInt + (io.writeVecCtrl.bits.reg_idxw >> log2Ceil(num_bank).U)
  wbScaBankId := io.writeScalarCtrl.bits.reg_idxw(log2Ceil(num_bank)-1,0)+io.writeScalarCtrl.bits.warp_id(log2Ceil(num_bank)-1,0)
  wbScaBankAddr := (io.sgpr_base(io.writeScalarCtrl.bits.warp_id) >> log2Ceil(num_bank).U).asUInt + (io.writeScalarCtrl.bits.reg_idxw >> log2Ceil(num_bank).U)
  val wbScaBankAddrtest = Wire(UInt(depth_regBank.W))
  wbScaBankAddrtest := sgprW + regW

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
  scalarBank(wbScaBankId).rdwen := io.writeScalarCtrl.bits.wxd & io.writeScalarCtrl.valid & io.writeScalarCtrl.bits.reg_idxw.orR
  io.writeScalarCtrl.ready := true.B
  io.writeVecCtrl.ready := true.B

  // when all operands of an instruction has prepared, issue it.
  class DualIssueIO(num_CU: Int) extends Module {
    val io = IO(new Bundle {
      val in = Flipped(Vec(num_CU, Decoupled(Output(new issueIO))))
      val out_x = Decoupled(Output(new issueIO))
      val out_v = Decoupled(Output(new issueIO))
    })

    def inst_is_vec(in: issueIO): Bool = {
      val out = Wire(new Bool)
      // sALU | CSR | warpscheduler
      // vFPU | vSFU | vALU&SIMT | vMUL | vTC | LSU
      when(in.control.tc || in.control.fp || in.control.mul || in.control.sfu || in.control.mem) {
        out := true.B
      }.elsewhen(in.control.csr.orR || in.control.barrier) {
        out := false.B
      }.elsewhen(in.control.isvec) {
        out := true.B
      }.otherwise {
        out := false.B
      }
      out
    }

    val arb_x = Module(new RRArbiter(new issueIO, num_CU))
    val arb_v = Module(new RRArbiter(new issueIO, num_CU))

    (0 until num_CU).foreach { i =>
      val in_isvec = inst_is_vec(io.in(i).bits)
      io.in(i).ready := Mux(in_isvec, arb_v.io.in(i).ready, arb_x.io.in(i).ready)

      arb_x.io.in(i).valid := io.in(i).valid && !in_isvec
      arb_x.io.in(i).bits := io.in(i).bits
      arb_v.io.in(i).valid := io.in(i).valid && in_isvec
      arb_v.io.in(i).bits := io.in(i).bits
    }
    io.out_x <> arb_x.io.out
    io.out_v <> arb_v.io.out
  }

  val issueUnit = Module(new DualIssueIO(num_collectorUnit))
  (0 until num_collectorUnit).foreach{ i =>
    issueUnit.io.in(i) <> collectorUnits(i).issue
  }
  io.out(0) <> issueUnit.io.out_v
  io.out(1) <> issueUnit.io.out_x
}


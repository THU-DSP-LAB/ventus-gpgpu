package pipeline

import chisel3._
import chisel3.util._
import parameters._
import IDecode._

class WriteVecCtrl extends Bundle{
  val wb_wfd_rd=(Vec(num_thread,UInt(xLen.W)))
  val wfd_mask=Vec(num_thread,Bool())
  val wfd=Bool()
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
class CU2Arbiter extends Bundle{
  val rsAddr = UInt(depth_regBank.W)
  val bankID = UInt(log2Ceil(num_bank).W)
  val rsType = UInt(2.W)
}

class issueIO extends Bundle{
  val alu_src1 = Vec(num_thread, UInt(xLen.W))
  val alu_src2 = Vec(num_thread, UInt(xLen.W))
  val alu_src3 = Vec(num_thread, UInt(xLen.W))
  val mask = Vec(num_thread, Bool())
}
/**
 *One of the number of num_warp collector Units, instantiating this class in operand collector for num_warps.
 */
class collectorUnit extends Module{
  val io = IO(new Bundle{
    val control = Flipped(Decoupled(new CtrlSigs))
    val bankIn = Flipped(Decoupled(new crossbar2CU))
    //operand to be issued, alternatively vector and scalar
    val issue = Decoupled(new issueIO)
    val outArbiterIO = Vec(4, Decoupled(new CU2Arbiter))
    val idle = Output(Bool())
    val wid = Output(UInt(depth_warp.W))

  })
  val wid = Reg(UInt(depth_warp.W))
  io.idle := !(valid.orR)
  io.wid := wid

  // rsType == 0: PC or mask(for op4)
  // rsType == 1: scalar
  // rsType == 2: Vec
  // rsType == 3: Imm
  val rsType = RegInit(Vec(4, UInt(2.W)))
  val ready = RegInit(0.U(4.W))
  val valid = RegInit(0.U(4.W))
  val regIdx = RegInit(Vec(4, 0.U(5.W)))
  val rsReg = RegInit(VecInit(Seq.fill(3)(Vec(num_thread, UInt(xLen.W))))) //op1, op2 and op3
  val mask = Vec(num_thread, Bool())

  val rsTypeWire = Wire(Vec(4, UInt(2.W)))
  val readyWire = Wire(UInt(4.W))
  val regIdxWire = Wire(Vec(4, UInt(5.W)))

  val imm = Module(new ImmGen)

  val s_idle :: s_add :: s_out :: Nil = Enum(3)
  val state = RegInit(s_idle)

  io.bankIn.ready := state===s_add
  //  io.outArbiterIO.foreach(x => {x. valid := (state===s_idle && io.ctrlSigsInput.fire && )})
  for(i <- 0 until 4) {
    io.outArbiterIO(i).valid := (state===s_idle && io.control.fire) ||
      (valid(i)===1.U && ready(i)===0.U)
  }
  io.issue.valid := (valid.asUInt === ready.asUInt) && ready.andR
  io.control.ready := (state===s_idle && !valid.asUInt.orR)
  //Lookup table for address transformation
  val bankIdLookup = (0 until num_warp+32).map { x =>
    (x -> x % num_bank)
  }.map { x => (x._1.U -> x._2.U)}
  val addrLookup = (0 until 32).map { x =>
    (x -> x % (num_bank))
  }.map {x => (x._1.U -> x._2.U)}

  when(state === s_idle) {
    when(io.control.fire && !valid.asUInt.orR){
      state := s_add
    }.otherwise{state := s_idle}
  }.elsewhen (state === s_add) {
    when(valid.asUInt =/= ready.asUInt ) {
      state := s_add
      //    }.elsewhen(io.bankIn.fire){
      //      state := s_out
    }.otherwise{state := s_out}
  }.elsewhen(state === s_out) {
    when(io.issue.ready){
      state := s_idle
    }.otherwise{state := s_out}
  }.otherwise{state := s_idle}

  switch(state){
    is(s_idle){
      when(io.control.fire){
        wid := io.control.bits.wid
        //using an iterable variable to indicate reg_idx signals
        regIdxWire(0) := io.control.bits.reg_idx1
        regIdxWire(1) := io.control.bits.reg_idx2
        regIdxWire(2) := io.control.bits.reg_idx3
        regIdxWire(3) := 0.U // mask of vector instructions
        regIdx(0) := io.control.bits.reg_idx1
        regIdx(1) := io.control.bits.reg_idx2
        regIdx(2) := io.control.bits.reg_idx3
        regIdx(3) := 0.U // mask of vector instructions
        valid := "b1111".U
        ready := "b0000".U
        //using an iterable variable to indicate sel_alu signals
        rsTypeWire(0) := io.control.bits.sel_alu1
        rsTypeWire(1) := io.control.bits.sel_alu2
        rsTypeWire(2) := io.control.bits.sel_alu3
        rsTypeWire(3) := 0.U(2.W) //mask
        rsType(0) := io.control.bits.sel_alu1
        rsType(1) := io.control.bits.sel_alu2
        rsType(2) := io.control.bits.sel_alu3
        rsType(3) := 0.U(2.W) //mask
        imm.io.inst := io.control.bits.inst
        imm.io.sel := io.control.bits.sel_imm
        //if the operand1 or operand2 is an immediate, elaborate it and enable the ready bit
        when(io.control.bits.sel_alu1 === A1_IMM) {
          rsReg(0) := imm.out
          ready(0) := 1.U
        }.elsewhen(io.control.bits.sel_alu1===A1_PC){
          rsReg(0) := io.control.bits.pc
          ready(0) := 1.U
        }
        when(io.control.bits.sel_alu2===A2_IMM){
          rsReg(1) := imm.out
          ready(1) := 1.U
        }.elsewhen(io.control.bits.sel_alu2===A2_SIZE){
          rsReg(1) := 4.U
          ready(1) := 1.U
        }
        when(!io.control.bits.mask){
          (0 until num_thread).foreach(x=>{
            rsReg(3)(x) := Mux(io.control.bits.isvec,true.B, !x.asUInt.orR)//this instruction is a Vector inst without mask or a Scalar inst.
          })
          ready(3) := 1.U
        }
        readyWire(0) := (io.control.bits.sel_alu1===A1_IMM) || (io.control.bits.sel_alu1===A1_PC)
        readyWire(1) := (io.control.bits.sel_alu2===A2_IMM) || (io.control.bits.sel_alu2===A2_SIZE)
        readyWire(2) := 0.U
        readyWire(3) := 0.U
        //reading the register bank for those operand which type is not an immediate
        for(i <- 0 until 4){
          when(readyWire(i) === 0.U){
            io.outArbiterIO(i).bits.bankID := MuxLookup(regIdxWire(i), 0.U, bankIdLookup)
            io.outArbiterIO(i).bits.rsAddr := MuxLookup(regIdxWire(i), 0.U, addrLookup) + wid*(32/num_bank).U
            io.outArbiterIO(i).bits.rsType := rsTypeWire(i)
          }
        }
      }
    }
    is(s_add){
      when(io.bankIn.fire){
        when(io.bankIn.bits.regOrder === 0.U){//operand1
          rsReg(0) := MuxLookup(rsType(0), 0.U, Array(A1_RS1->Cat(0.U(((num_thread-1)*xLen).W), io.bankIn.bits.data(0)), A1_VRS1->io.bankIn.bits.data))
          ready(0) := 1.U
        }.elsewhen(io.bankIn.bits.regOrder===1.U){//operand2
          rsReg(1) := MuxLookup(rsType(1), 0.U, Array(A2_RS2->Cat(0.U(((num_thread-1)*xLen).W), io.bankIn.bits.data(0)), A2_VRS2->io.bankIn.bits.data))
          ready(1) := 1.U
        }.elsewhen(io.bankIn.bits.regOrder===2.U){//operand3
          rsReg(2) := (rsType(2), 0.U, Array(A3_PC->Mux(io.control.bits.branch ===B_R,(imm.io.out+io.bankIn.bits.data(0)),(io.control.bits.pc+imm.io.out)),
            A3_VRS3->io.bankIn.bits.data, A3_SD->Mux(io.control.bits.isvec,io.bankIn.bits.data, io.bankIn.bits.data(0)), A3_FRS3->io.bankIn.bits.data(0)))
          ready(2) := 1.U
        }.elsewhen(io.bankIn.bits.regOrder===3.U){
          (0 until num_thread).foreach( x=>{
            mask(x) := io.bankIn.bits.v0(0).apply(x) //this instruction is an Vector with mask, the mask is read from vector register bank
          })
          ready(3) := 1.U
        }
      }
      // generate immediate
      (0 until 2).foreach(x=>{
        when(rsType(x) === A1_IMM){rsReg(x) := imm.out}
      })
    }
    is(s_out){
      when(io.issue.fire){
        io.issue.bits.alu_src1 := rsReg(0)
        io.issue.bits.alu_src2 := rsReg(1)
        io.issue.bits.alu_src3 := rsReg(2)
        io.issue.bits.mask := mask
      }
    }
  }
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
    val x = Module(new RRArbiter(UInt(depth_regBank.W), 4*num_collectorUnit))
    x
  }
  val bankArbiterVector = for (i <- 0 until num_bank) yield {
    val x = Module(new RRArbiter(UInt(depth_regBank.W), 4 * num_collectorUnit))
    x
  }
  val vecRsIO = RegInit(VecInit(Seq.fill(4*num_collectorUnit)(UInt(depth_regBank.W))))
  val vecBankIDIO = RegInit(VecInit(Seq.fill(4*num_collectorUnit)(UInt(log2Ceil(num_bank).W))))
  val vecRsType = RegInit(VecInit(Seq.fill(4*num_collectorUnit)(UInt(2.W))))

  //flatten readArbiterIO
  for (i <-0 until num_collectorUnit) {
    for (j<-0 until 4){
      vecRsIO(i*4+j) := io.readArbiterIO(i)(j).bits.rsAddr
      vecBankIDIO(i*4+j) := io.readArbiterIO(i)(j).bits.bankID
      vecRsType(i*4+j) := io.readArbiterIO(i)(j).bits.rsType
    }
  }

  for (i <- 0 until num_bank){
    //    mapping input signals from collector units to inputs of Arbiters
    bankArbiterScalar(i).io.in := VecInit(io.readArbiterIO.flatten.map(x => Flipped(Decoupled(new Bundle {
      val rsAddr = Wire(x.bits.rsAddr)
      val bankID = Wire(x.bits.bankID)
    }))))
    bankArbiterVector(i).io.in := bankArbiterScalar(i).io.in
  }
  //elaborate valid port of readArbiters
  for (i <- 0 until num_bank){
    for(j <- 0 until num_collectorUnit)
      for(k <- 0 until 4){
        bankArbiterScalar(i).io.in(j*4+k).valid := io.readArbiterIO(j)(k).valid &&
          (io.readArbiterIO(j)(k).bits.bankID === i.U) && (io.readArbiterIO(j)(k).bits.rsType === 1.U)
        bankArbiterVector(i).io.in(j*4+k).valid := io.readArbiterIO(j)(k).valid &&
          (io.readArbiterIO(j)(k).bits.bankID === i.U) && (io.readArbiterIO(j)(k).bits.rsType === 2.U)
      }
  }
  io.readArbiterOut := VecInit(bankArbiterScalar.map(x => x.io.out))
  io.readchosen := VecInit(bankArbiterScalar.map(x => x.io.chosen))

  //Address of writeback transformation

}

class crossBar extends Module{
  val io = IO(new Bundle {
    val chosen = Input(Vec(num_bank, UInt(log2Ceil(4 * num_collectorUnit).W)))
    val dataIn = Input(new Bundle{
      val rs = Vec(num_bank, Vec(num_thread, UInt(xLen.W)))
      val v0 = Vec(num_bank, Vec(num_thread,UInt((xLen).W)))
    })
    val out = Vec(num_collectorUnit, Decoupled(new crossbar2CU))
  })
  val CUId = Wire(Vec(num_bank, UInt(log2Ceil(num_collectorUnit).W)))
  val regOrder = Wire(Vec(num_bank, UInt(2.W)))
  (0 until num_bank).foreach(i=>{
    CUId(i) := io.chosen(i) >> 2.U
    regOrder(i) := io.chosen(i) % 4.U
  })
  (0 until num_collectorUnit).foreach(i=>{
    io.out(i).valid := (CUId(i)===i.U)
    io.out(i).bits.data := io.dataIn.rs
    io.out(i).bits.v0 := io.dataIn.v0
    io.out(i).bits.regOrder := regOrder
  })

}

/**
 * Allocating the collector unit to new input instruction
 */
class instDemux extends Module{
  val io = IO(new Bundle{
    val in = Flipped(Decoupled(new CtrlSigs))
    val out = Vec(num_collectorUnit, Decoupled(new CtrlSigs))
    val widCmp = Vec(num_collectorUnit, Bool())
  })
  //If there isn't any warp id as same as input instruction, the instruction can be allocated a CU
  io.in.ready := !io.widCmp.reduce(_|_)
  when(io.in.fire){
    //Each data on out port is identical
    io.out.foreach(_.bits := io.in.bits)
  }
  //For those out port which is ready, selecting one by bitwise priority.
  val outReady = VecInit(io.out.map(_.valid))
  for((v, i) <- io.out.zipWithIndex){
    v.valid := Mux(PriorityEncoder(outReady)===i.U, true.B, false.B)
  }
}
class operandCollector extends Module{
  val io=IO(new Bundle {
    val control=Flipped(Decoupled(new CtrlSigs()))
    //val inst=Input(UInt(32.W))
    val out=Decoupled(new issueIO)
    val writeScalarCtrl=Flipped(DecoupledIO(new WriteScalarCtrl)) //should be used as decoupledIO
    val writeVecCtrl=Flipped(DecoupledIO(new WriteVecCtrl))
  })
  val collectorUnits = VecInit(Seq.fill(num_collectorUnit)(Module(new collectorUnit).io))
  val Arbiter = new operandArbiter
  val vectorBank = VecInit(Seq.fill(num_bank)(Module(new FloatRegFileBank).io))
  val scalarBank = VecInit(Seq.fill(num_bank)(Module(new RegFileBank).io))
  val crossBar = new crossBar
  val Demux = new instDemux
  //connecting Arbiters and banks
  Arbiter.io.readArbiterIO <> VecInit(collectorUnits.map(_.outArbiterIO))
  Arbiter.io.readArbiterOut <> vectorBank
  Arbiter.io.readArbiterOut <> scalarBank
  //connecting crossbar and banks, as well as signal readchosen. Readchosen needs to delay one tik to match bank reading
  crossBar.io.chosen := RegNext(Arbiter.io.readchosen)
  for( i <- 0 until num_bank){
    when(Arbiter.io.readArbiterOut(i).bits.rsType===1.U){ //scalar
      crossBar.io.dataIn.rs(i)(0) := scalarBank(i).rs
      crossBar.io.dataIn.v0(i) := Vec(num_bank, 0.U(xLen.W))
    }.elsewhen((Arbiter.io.readArbiterOut(i).bits.rsType===2.U)){//vector
      crossBar.io.dataIn.rs(i) := vectorBank(i).rs
      crossBar.io.dataIn.v0(i) := vectorBank(i).v0
    }
  }
  //connecting crossbar and collector units
  crossBar.io.out := VecInit(collectorUnits.map(_.bankIn))
  //CU allocation
  val widReg = RegInit(VecInit.fill(num_collectorUnit)(0.U(log2Ceil(num_collectorUnit).W)))
  val widCmp = Wire(Vec(num_collectorUnit, Bool()))
  (0 until num_collectorUnit).foreach( i => {
    widCmp(i) := io.control.bits.wid===collectorUnits(i).wid
  })
  Demux.io.widCmp := widCmp
  Demux.io.in <> io.control
  Demux.io.out <> VecInit(collectorUnits.map(_.control))
  //writeback control
  val bankIdLookup = (0 until num_warp + 32).map { x =>
    (x -> x % num_bank)
  }.map { x => (x._1.U -> x._2.U)}
  val addrLookup = (0 until 32).map { x =>
    (x -> x % (num_bank))
  }.map { x => (x._1.U -> x._2.U) }
  val wbVecBankId = UInt(2.W)
  val wbScaBankId = UInt(2.W)
  val wbVecBankAddr = UInt(depth_regBank.W)
  val wbScaBankAddr = UInt(depth_regBank.W)

  wbVecBankId := MuxLookup(io.writeVecCtrl.bits.reg_idxw, 0.U, bankIdLookup)
  wbVecBankAddr := MuxLookup(io.writeVecCtrl.bits.reg_idxw, 0.U, addrLookup) + io.writeVecCtrl.bits.warp_id*(32/num_bank).U
  wbScaBankId := MuxLookup(io.writeScalarCtrl.bits.reg_idxw, 0.U, bankIdLookup)
  wbScaBankAddr := MuxLookup(io.writeScalarCtrl.bits.reg_idxw, 0.U, addrLookup) + io.writeScalarCtrl.bits.warp_id*(32/num_bank).U

  vectorBank.foreach(x=>{
    x.rdidx := wbVecBankAddr
    x.rd := io.writeVecCtrl.bits.wb_wfd_rd
    x.rdwen := false.B
    x.rdwmask := io.writeVecCtrl.bits.wfd_mask
  })
  scalarBank.foreach(x=>{
    x.rdidx := wbScaBankAddr
    x.rd := io.writeScalarCtrl.bits.wb_wxd_rd
    x.rdwen := false.B
  })
  vectorBank(wbVecBankId).rdwen := io.writeVecCtrl.bits.wfd & io.writeVecCtrl.valid
  scalarBank(wbScaBankId).rdwen := io.writeScalarCtrl.bits.wxd & io.writeScalarCtrl.valid
  io.writeScalarCtrl.ready := true.B
  io.writeVecCtrl.ready := true.B

  //when all operands of an instruction has prepared, issue it.
  val issueArbiter = Module(new Arbiter((new issueIO), num_collectorUnit))
  issueArbiter.io.in := VecInit(collectorUnits.map(_.issue))
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
  //    io.alu_src3(x):=MuxLookup(io.control.sel_alu3,0.U,Array(A3_PC->Mux(io.control.branch===B_R,(imm.io.out+scalarRegFile(io.control.wid).rs1),(io.control.pc+imm.io.out)),A3_VRS3->vectorRegFile(io.control.wid).rs3(x),A3_SD->Mux(io.control.isvec,vectorRegFile(io.control.wid).rs3(x),scalarRegFile(io.control.wid).rs2),A3_FRS3->(scalarRegFile(io.control.wid).rs3)))
  //    io.mask(x):=Mux(io.control.mask,vectorRegFile(io.control.wid).v0(0).apply(x),Mux(io.control.isvec,true.B,!x.asUInt.orR))
  //
  //  })

}


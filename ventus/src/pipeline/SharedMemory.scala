package pipeline

import chisel3._
import chisel3.util._
import chisel3.util.experimental.loadMemoryFromFile
import parameters._

class SharedMemoryV2 (size: Int, elem: Int=num_thread, xlen: Int=xLen, nMshrEntry: Int=lsu_nMshrEntry) extends Module{
//  val io = DecoupledIO(new Bundle{
//    val en = Input(Bool())
//    val we = Input(Bool())
//    val isUnitStride = Input(Bool())  //
//    val maskIn = Input(UInt(elem.W))
//    val addr = Input(Vec(elem, UInt(xlen.W)))
//    val wData = Input(Vec(elem, UInt(xlen.W)))
//    val rData = Output(Vec(elem, UInt(xlen.W)))
//    val maskOut = Output(UInt(elem.W))
//    val idle = Output(Bool())
//  })
  val io = IO(new Bundle{
    val in = Flipped(DecoupledIO(new toShared))
    val out = DecoupledIO(new DCacheCoreRsp_np)
  })
  val sharedMem = Mem(size, Vec(elem, UInt(xlen.W)))

  val s_idle :: s_rbusy :: s_wbusy :: s_out :: Nil = Enum(4)
  val state = RegInit(init = s_idle)

  val reg_Addr = RegInit(VecInit(Seq.fill(elem)(0.U(xlen.W))))
  val reg_Data = RegInit(VecInit(Seq.fill(elem)(0.U(xlen.W))))
  val reg_ExtMask = RegInit(VecInit(Seq.fill(elem)(false.B)))
  val reg_isvec = RegInit(true.B)
  val counter = RegInit(0.U((log2Up(elem)+1).W))
  val wire_Data = Wire(Vec(elem, UInt(xlen.W)))
  val reg_UnitStride = RegInit(false.B)
  val reg_Entry = RegInit(0.U(log2Up(nMshrEntry).W))

  val baseline = reg_Addr(counter-1.U).asUInt()/(elem*xlen/8).asUInt()
  val offset = (reg_Addr(counter-1.U)/(xlen/8).asUInt()) % elem.asUInt()
  val w_baseline = io.in.bits.addr(0).asUInt()/(elem*xlen/8).asUInt()
  val w_offset = (io.in.bits.addr(0)/(xlen/8).asUInt()) % elem.asUInt()
  val internalMask = Wire(Vec(elem, Bool()))
  val deVecData = Wire(Vec(elem, UInt(xlen.W)))
  val isUnitStride = io.in.bits.ctrl.mop===0.U && io.in.bits.ctrl.isvec && !io.in.bits.addr(0)(depth_thread-1,0).orR()//标量操作视作indexed

  for (i <- 0 until elem){
    internalMask(i) := i.U === offset
    //deVecData(i) := Mux(offset===i.U, reg_Data(counter-1.U), 0.U)
    deVecData(i) := reg_Data(counter-1.U)
    wire_Data(i) := 0.U
  }

  // state transfer
  switch(state){
    is (s_idle){
      when(io.in.fire() && io.in.bits.ctrl.mem_cmd(1) && !isUnitStride){
        state := s_wbusy
      }.elsewhen(io.in.fire() && !io.in.bits.ctrl.mem_cmd(1) && !isUnitStride){
        state := s_rbusy
      }.elsewhen(io.in.fire() && !io.in.bits.ctrl.mem_cmd(1) && isUnitStride){
        state := s_out
      }.otherwise{
        state := s_idle
      }
    }
    is (s_rbusy){
      when(counter===elem.U || !reg_isvec){ //对于标量，停留一拍就可以了
        state := s_out
      }.otherwise{
        state := s_rbusy
      }
    }
    is (s_wbusy){
      when(counter===elem.U || !reg_isvec){
        state := s_idle
      }.otherwise{
        state := s_wbusy
      }
    }
    is (s_out){
      when(io.out.ready){ state := s_idle }
    }
  }
  // operations
  switch(state){
    is (s_idle){
      when(io.in.fire() && isUnitStride){
        counter := 0.U
        reg_ExtMask := io.in.bits.mask
        when(!io.in.bits.ctrl.mem_cmd(1)){ // next: Seq Read Mode | s_out
          //reg_Data.foreach{_ := 0.U}
          reg_Data := sharedMem.read(w_baseline)
          reg_UnitStride := isUnitStride
          //wire_Data := sharedMem.read(w_baseline)
          reg_Entry := io.in.bits.instrId
        }.otherwise{  // next: Seq Write Mode
          for (i <- 0 until elem){internalMask(i) := io.in.bits.mask(i).asBool()}
          sharedMem.write(w_baseline, io.in.bits.data, internalMask)
        }
      }.elsewhen(io.in.fire() && !isUnitStride){
        counter := 1.U
        reg_Addr := io.in.bits.addr
        reg_isvec := io.in.bits.ctrl.isvec
        when(!io.in.bits.ctrl.mem_cmd(1)){ // next: Idx Read Mode
          reg_Data.foreach{_ := 0.U}
          reg_UnitStride := isUnitStride
          reg_Entry := io.in.bits.instrId
        }.otherwise{ // next: Idx Write Mode
          reg_Data := io.in.bits.data
          reg_ExtMask := io.in.bits.mask
        }
      }.otherwise{ // next: Idle
        reg_UnitStride := 0.U
        reg_Entry := 0.U
        counter := 0.U
        reg_Addr.foreach{_ := 0.U}
      }
    }
    is (s_rbusy){ // Idx Read Mode
      counter := Mux(counter===elem.U, 0.U, counter+1.U)
      reg_Data(counter-1.U) := sharedMem.read(baseline)(offset)
      when(counter===elem.U){reg_Addr.foreach{_ := 0.U}}
    }
    is (s_wbusy){ // Idx Write Mode
      counter := Mux(counter===elem.U, 0.U, counter+1.U)
      when(reg_ExtMask(counter-1.U).asBool){
        sharedMem.write(baseline, deVecData, internalMask)
      }
    }
    is (s_out){
      counter := 0.U
      when(io.out.fire()){ reg_Data.foreach( _ := 0.U ) }
    }
  }
  io.out.bits.data := reg_Data//Mux(io.in.valid&&isUnitStride, wire_Data, reg_Data)
  io.out.bits.activeMask := reg_ExtMask
  io.out.bits.instrId := reg_Entry
  //io.out.bits.ctrl.mop := Mux(reg_UnitStride, 0.U, 1.U)
  //io.out.bits.ctrl.mem_cmd := 1.U
  io.out.valid := state===s_out
  io.in.ready := state===s_idle
}
package pipeline

import chisel3._
import chisel3.util.{Cat, MuxLookup}
import IDecode._
import parameters._

class RegFileIO extends Bundle  {
  val x1     = Output(UInt(xLen.W))//x1 CSR
  val rs1    = Output(UInt(xLen.W))
  val rs2    = Output(UInt(xLen.W))
  val rs3    = Output(UInt(xLen.W))
  val rs1idx = Input(UInt(5.W))
  val rs2idx = Input(UInt(5.W))
  val rs3idx = Input(UInt(5.W))
  val rd     = Input(UInt(xLen.W))
  val rdidx  = Input(UInt(5.W))
  val rdwen  = Input(Bool())
}

class RegFile extends Module  {
  val io = IO(new RegFileIO())
  val regs = Mem(32, UInt(xLen.W))
  io.rs1 := Mux(((io.rs1idx===io.rdidx)&io.rdwen),io.rd,Mux(io.rs1idx.orR, regs(io.rs1idx), 0.U))
  io.rs2 := Mux(((io.rs2idx===io.rdidx)&io.rdwen),io.rd,Mux(io.rs2idx.orR, regs(io.rs2idx), 0.U))
  io.rs3 := Mux(((io.rs3idx===io.rdidx)&io.rdwen),io.rd,Mux(io.rs3idx.orR, regs(io.rs3idx), 0.U))
  io.x1 := regs(1.U)
  when (io.rdwen & io.rdidx.orR) {
    regs(io.rdidx) := io.rd
  }
}

class FloatRegFileIO extends Bundle  {
  val v0     = Output(Vec(num_thread,UInt((xLen).W)))//mask v0
  val rs1    = Output(Vec(num_thread,UInt((xLen).W)))
  val rs2    = Output(Vec(num_thread,UInt((xLen).W)))
  val rs3    = Output(Vec(num_thread,UInt((xLen).W)))
  val rs1idx = Input(UInt(5.W))
  val rs2idx = Input(UInt(5.W))
  val rs3idx = Input(UInt(5.W))
  val rd     = Input(Vec(num_thread,UInt((xLen).W)))
  val rdidx  = Input(UInt(5.W))
  val rdwen  = Input(Bool())
  val rdwmask = Input(Vec(num_thread,Bool()))
}

class FloatRegFile extends Module  {
  val io = IO(new FloatRegFileIO)
  val regs = Mem(32, Vec(num_thread,UInt(xLen.W)))
  val internalMask = Wire(Vec(num_thread, Bool()))
  io.rs1 := Mux(((io.rs1idx===io.rdidx)&io.rdwen),io.rd,regs(io.rs1idx))
  io.rs2 := Mux(((io.rs2idx===io.rdidx)&io.rdwen),io.rd,regs(io.rs2idx))
  io.rs3 := Mux(((io.rs3idx===io.rdidx)&io.rdwen),io.rd,regs(io.rs3idx))
  io.v0 := regs(0.U)
  //for (i <- 0 until num_thread){internalMask(i) := io.rdwmask(i).asBool()}
  internalMask:=io.rdwmask
  when (io.rdwen ) {
    //regs(io.rdidx) := io.rd
    regs.write(io.rdidx, io.rd, internalMask)
  }
}

class ImmGenIO extends Bundle {
  val inst = Input(UInt(32.W))
  val sel  = Input(UInt(3.W))
  val out  = Output(UInt(32.W))
}

class ImmGen extends Module {
  val io = IO(new ImmGenIO)

  val Iimm = io.inst(31, 20).asSInt // load, arithmetic, logic, jalr
  val Simm = Cat(io.inst(31, 25), io.inst(11, 7)).asSInt  // store
  val Bimm = Cat(io.inst(31), io.inst(7), io.inst(30, 25), io.inst(11, 8), 0.U(1.W)).asSInt // branch
  val Uimm = Cat(io.inst(31, 12), 0.U(12.W)).asSInt // lui, auipc
  val Jimm = Cat(io.inst(31), io.inst(19, 12), io.inst(20), io.inst(30, 21), 0.U(1.W)).asSInt // jal
  val Zimm = Cat(0.U(27.W),io.inst(19, 15)).asSInt // CSR I
  val Imm2 = io.inst(24,20).asSInt
  val Vimm = io.inst(19,15).asSInt

  val out = WireInit(0.S(32.W))

  out := MuxLookup(io.sel, Iimm & -2.S, Seq(IMM_I -> Iimm,IMM_J->Jimm, IMM_S -> Simm, IMM_B -> Bimm, IMM_U -> Uimm, IMM_2 -> Imm2,IMM_Z -> Zimm,IMM_V->Vimm))
  io.out:=out.asUInt
}
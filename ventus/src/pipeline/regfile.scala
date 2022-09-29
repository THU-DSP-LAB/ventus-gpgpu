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
package pipeline

import chisel3._
import chisel3.util.{Cat, MuxLookup}
import IDecode._
import parameters._

class RegFileBankIO extends Bundle  {
  val x1     = Output(UInt(xLen.W))//x1 CSR
  val rs     = Output(UInt(xLen.W))
  val rsIdx  = Input(UInt(5.W))
  val rd     = Input(UInt(xLen.W))
  val rdIdx  = Input(UInt(5.W))
  val rdwen  = Input(Bool())
  val ready  = Output(Bool())
}

class RegFileBank extends Module  {
  val io = IO(new RegFileBankIO())
  val regs = Mem(32*num_warp/num_bank, UInt(xLen.W))
  io.rs := Mux(((io.rsIdx===io.rdIdx)&io.rdwen),io.rd,Mux(io.rsIdx.orR, regs(io.rsIdx), 0.U))
  io.ready := true.B
  io.x1 := regs(1.U)
  when (io.rdwen & io.rdIdx.orR) {
    regs(io.rdIdx) := io.rd
  }
}

class FloatRegFileBankIO extends Bundle  {
  val v0     = Output(Vec(num_thread,UInt((xLen).W)))//mask v0
  val rs     = Output(Vec(num_thread,UInt((xLen).W)))
  val rsidx  = Input(UInt(5.W))
  val rd     = Input(Vec(num_thread,UInt((xLen).W)))
  val rdidx  = Input(UInt(5.W))
  val rdwen  = Input(Bool())
  val rdwmask = Input(Vec(num_thread,Bool()))
}




class FloatRegFileBank extends Module  {
  val io = IO(new FloatRegFileBankIO)
  val regs = SyncReadMem(32*num_warp/num_bank, Vec(num_thread,UInt(xLen.W)))  //Register files of all warps are divided to number of bank
  val internalMask = Wire(Vec(num_thread, Bool()))

  io.rs := regs.read(io.rsidx)
  //  io.rs := Mux(((io.rsIdx===io.rdIdx)&io.rdwen),io.rd,regs(io.rsIdx))
  io.v0 := regs(0.U)
  //for (i <- 0 until num_thread){internalMask(i) := io.rdwmask(i).asBool()}
  internalMask:=io.rdwmask
  when (io.rdwen ) {
    //regs(io.rdIdx) := io.rd
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
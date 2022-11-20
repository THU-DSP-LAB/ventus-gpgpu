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
import chisel3.util.BitPat
import Instructions._
import chisel3.util.ListLookup
import parameters._

object IDecode //extends DecodeConstants
{
  def Y = true.B//BitPat("b1")
  def N = false.B//BitPat("b0")
  def X = false.B//BitPat("b?")
  val B_N = 0.U(2.W)
  val B_B = 1.U(2.W)
  val B_J = 2.U(2.W)
  val B_R = 3.U(2.W)
  val A1_RS1 = 1.U(2.W)
  val A1_VRS1 = 2.U(2.W)
  val A1_IMM =3.U(2.W)
  val A1_PC = 0.U(2.W)
  def A3_X=1.U(2.W)
  val A3_VRS3=1.U(2.W)
  val A3_SD=3.U(2.W)
  val A3_FRS3=2.U(2.W)//for float(not vector). jalr use b_r to distinguish
  val A3_PC=0.U(2.W)
  def A1_X=1.U(2.W)//BitPat("b??")
  def A2_X=1.U(2.W)//BitPat("b??")
  val A2_RS2 = 1.U(2.W)
  val A2_IMM =3.U(2.W)
  val A2_VRS2 = 2.U(2.W)
  val A2_SIZE = 0.U(2.W)
  val IMM_I = 0.U(3.W)
  val IMM_S = 1.U(3.W)
  val IMM_B = 2.U(3.W)
  val IMM_U = 3.U(3.W)
  val IMM_2 = 4.U(3.W)//for rs2 as imm2
  val IMM_J = 5.U(3.W)
  val IMM_X = 0.U(3.W)//BitPat("b???")
  val IMM_V = 6.U(3.W)
  val IMM_Z = 7.U(3.W)
  val MEM_W = 3.U(2.W)
  val MEM_B = 2.U(2.W)
  val MEM_H = 1.U(2.W)//half word
  val MEM_X= 0.U(2.W)
  val M_XRD =1.U(2.W)
  val M_XWR =2.U(2.W)
  val M_X =0.U(2.W)//BitPat("b??")
  def FN_X    = 63.U(6.W)//BitPat("b????")
  def FN_ADD  = 0.U(6.W)
  def FN_SL   = 1.U(6.W)
  def FN_SEQ  = 2.U(6.W)
  def FN_SNE  = 3.U(6.W)
  def FN_XOR  = 4.U(6.W)
  def FN_SR   = 5.U(6.W)
  def FN_OR   = 6.U(6.W)
  def FN_AND  = 7.U(6.W)
  def FN_SUB  = 10.U(6.W)
  def FN_SRA  = 11.U(6.W)
  def FN_SLT  = 12.U(6.W)
  def FN_SGE  = 13.U(6.W)
  def FN_SLTU = 14.U(6.W)
  def FN_SGEU = 15.U(6.W)
  def FN_MAX=16.U(6.W)
  def FN_MIN=17.U(6.W)
  def FN_MAXU=18.U(6.W)
  def FN_MINU=19.U(6.W)
  def FN_A1ZERO = 8.U(6.W)
  def FN_MUL = 20.U(6.W)
  def FN_MULH = 21.U(6.W)
  def FN_MULHU = 22.U(6.W)
  def FN_MULHSU = 23.U(6.W)
  def FN_MACC = 24.U(6.W)
  def FN_NMSAC = 25.U(6.W)
  def FN_MADD = 26.U(6.W)
  def FN_NMSUB = 27.U(6.W)

  //pseudo inst
  def FN_VMNOR=54.U(6.W)
  def FN_VMNAND=55.U(6.W)
  def FN_VMXNOR=56.U(6.W)
  def FN_VMORNOT=58.U(6.W)
  def FN_VMANDNOT=59.U(6.W)
  def FN_VID = 57.U(6.W)
  def FN_VMERGE = 51.U(6.W)


  def FN_FADD     = 0.U(6.W)  // 000 000
  def FN_FSUB     = 1.U(6.W)  // 000 001
  def FN_FMUL     = 2.U(6.W)  // 000 010
  def FN_FMADD    = 4.U(6.W)  // 000 100
  def FN_FMSUB    = 5.U(6.W)  // 000 101
  def FN_FNMSUB   = 6.U(6.W)  // 000 110
  def FN_FNMADD   = 7.U(6.W)  // 000 111

  //pseudo inst
  def FN_VFMADD = 14.U(6.W) //vfmadd
  def FN_VFMSUB = 15.U(6.W) //vfmsub
  def FN_VFNMSUB = 16.U(6.W) //vfnmsub
  def FN_VFNMADD = 17.U(6.W) //vfnmadd


  def FN_FMIN      = 8.U(6.W)  // 001 000
  def FN_FMAX      = 9.U(6.W)  // 001 001
  def FN_FLE      = 10.U(6.W) // 001 010
  def FN_FLT      = 11.U(6.W) // 001 011
  def FN_FEQ      = 12.U(6.W) // 001 100
  def FN_FNE      = 13.U(6.W) //vmfne 001 101

  def FN_FCLASS   = 18.U(6.W) // 010 010
  def FN_FSGNJ    = 22.U(6.W) // 010 110
  def FN_FSGNJN   = 21.U(6.W) // 010 101
  def FN_FSGNJX   = 20.U(6.W) // 010 100

  def FN_F2I      = 24.U(6.W) // 011 000
  def FN_F2IU     = 25.U(6.W) // 011 001
  def FN_I2F      = 32.U(6.W) // 100 000
  def FN_IU2F     = 33.U(6.W) // 100 001

  // for SFU
  def FN_DIV      = 0.U(6.W)
  def FN_REM      = 1.U(6.W)
  def FN_DIVU     = 2.U(6.W)
  def FN_REMU     = 3.U(6.W)
  def FN_FDIV     = 0.U(6.W)
  def FN_FSQRT    = 1.U(6.W)

  val default = List(N,X,X,B_N,X,X,X,X,A3_X,A2_X,   A1_X,   IMM_X, MEM_X,  FN_X,     N,M_X,        X,X,X,X,X,X,X,X,X,X,X)

  val table=Array(//: Array[(BitPat, List[BitPat])] = Array(
    VBNE->   List(Y,N,N,B_B,Y,N,CSR.N,Y,A3_PC,A2_VRS2,A1_VRS1,IMM_B,MEM_X,FN_SNE,N,M_X,N,N,N,N,N,N,N,N,N,N,N),
    VBEQ->   List(Y,N,N,B_B,Y,N,CSR.N,Y,A3_PC,A2_VRS2,A1_VRS1,IMM_B,MEM_X,FN_SEQ,N,M_X,N,N,N,N,N,N,N,N,N,N,N),
    VBLT->   List(Y,N,N,B_B,Y,N,CSR.N,Y,A3_PC,A2_VRS2,A1_VRS1,IMM_B,MEM_X,FN_SLT,N,M_X,N,N,N,N,N,N,N,N,N,N,N),
    VBLTU->  List(Y,N,N,B_B,Y,N,CSR.N,Y,A3_PC,A2_VRS2,A1_VRS1,IMM_B,MEM_X,FN_SLTU,N,M_X,N,N,N,N,N,N,N,N,N,N,N),
    VBGE->   List(Y,N,N,B_B,Y,N,CSR.N,Y,A3_PC,A2_VRS2,A1_VRS1,IMM_B,MEM_X,FN_SGE,N,M_X,N,N,N,N,N,N,N,N,N,N,N),
    VBGEU->  List(Y,N,N,B_B,Y,N,CSR.N,Y,A3_PC,A2_VRS2,A1_VRS1,IMM_B,MEM_X,FN_SGEU,N,M_X,N,N,N,N,N,N,N,N,N,N,N),
    JOIN->   List(Y,N,N,B_B,Y,Y,CSR.N,N,A3_PC,A2_X,A1_X,IMM_B,MEM_X,FN_ADD,N,M_X,N,N,N,N,N,N,N,N,N,N,N),
    BARRIER->List(N,N,Y,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_IMM,IMM_Z,MEM_X,FN_ADD,N,M_X,N,N,N,N,N,N,N,N,N,N,N),
    ENDPRG-> List(N,N,Y,B_N,N,Y,CSR.N,N,A3_X,A2_X,A1_X,IMM_X,MEM_X,FN_ADD,N,M_X,N,N,N,N,N,N,N,N,N,N,N),
    BNE->    List(N,N,N,B_B,N,N,CSR.N,N,A3_PC,A2_RS2,A1_RS1,IMM_B,MEM_X,FN_SNE,N,M_X,N,N,N,N,N,N,N,N,N,N,N),
    BEQ->    List(N,N,N,B_B,N,N,CSR.N,N,A3_PC,A2_RS2,A1_RS1,IMM_B,MEM_X,FN_SEQ,N,M_X,N,N,N,N,N,N,N,N,N,N,N),
    BLT->    List(N,N,N,B_B,N,N,CSR.N,N,A3_PC,A2_RS2,A1_RS1,IMM_B,MEM_X,FN_SLT,N,M_X,N,N,N,N,N,N,N,N,N,N,N),
    BLTU->   List(N,N,N,B_B,N,N,CSR.N,N,A3_PC,A2_RS2,A1_RS1,IMM_B,MEM_X,FN_SLTU,N,M_X,N,N,N,N,N,N,N,N,N,N,N),
    BGE->    List(N,N,N,B_B,N,N,CSR.N,N,A3_PC,A2_RS2,A1_RS1,IMM_B,MEM_X,FN_SGE,N,M_X,N,N,N,N,N,N,N,N,N,N,N),
    BGEU->   List(N,N,N,B_B,N,N,CSR.N,N,A3_PC,A2_RS2,A1_RS1,IMM_B,MEM_X,FN_SGEU,N,M_X,N,N,N,N,N,N,N,N,N,N,N),
    JAL->    List(N,N,N,B_J,N,N,CSR.N,N,A3_PC,A2_SIZE,A1_PC,IMM_J,MEM_X,FN_ADD,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    JALR->   List(N,N,N,B_R,N,N,CSR.N,N,A3_PC,A2_SIZE,A1_PC,IMM_I,MEM_X,FN_ADD,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    AUIPC->  List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_IMM,A1_PC,IMM_U,MEM_X,FN_ADD,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    //A2_SIZE=>for C extension 2, for others 4, used for PC+4 to reg. in Rocketchip it's rvc

    CSRRW->  List(N,N,N,B_N,N,N,CSR.W,N,A3_X,A2_X,A1_RS1,IMM_X,MEM_X,FN_ADD,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    CSRRS->  List(N,N,N,B_N,N,N,CSR.S,N,A3_X,A2_X,A1_RS1,IMM_X,MEM_X,FN_ADD,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    CSRRC->  List(N,N,N,B_N,N,N,CSR.C,N,A3_X,A2_X,A1_RS1,IMM_X,MEM_X,FN_ADD,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    CSRRWI-> List(N,N,N,B_N,N,N,CSR.W,N,A3_X,A2_X,A1_IMM,IMM_Z,MEM_X,FN_ADD,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    CSRRSI-> List(N,N,N,B_N,N,N,CSR.S,N,A3_X,A2_X,A1_IMM,IMM_Z,MEM_X,FN_ADD,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    CSRRCI-> List(N,N,N,B_N,N,N,CSR.C,N,A3_X,A2_X,A1_IMM,IMM_Z,MEM_X,FN_ADD,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),

    FENCE->  List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_X,A1_X,IMM_I,MEM_X,FN_ADD,N,M_X,N,Y,N,N,N,N,Y,N,N,N,N),
    LW->     List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_IMM,A1_RS1,IMM_I,MEM_W,FN_ADD,N,M_XRD,N,N,N,N,N,N,Y,N,N,N,N),
    LH->     List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_IMM,A1_RS1,IMM_I,MEM_H,FN_ADD,N,M_XRD,N,N,N,N,N,N,Y,N,N,N,N),
    LB->     List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_IMM,A1_RS1,IMM_I,MEM_B,FN_ADD,N,M_XRD,N,N,N,N,N,N,Y,N,N,N,N),
    LHU->    List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_IMM,A1_RS1,IMM_I,MEM_H,FN_ADD,N,M_XRD,Y,N,N,N,N,N,Y,N,N,N,N),
    LBU->    List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_IMM,A1_RS1,IMM_I,MEM_B,FN_ADD,N,M_XRD,Y,N,N,N,N,N,Y,N,N,N,N),
    SW->     List(N,N,N,B_N,N,N,CSR.N,N,A3_SD,A2_IMM,A1_RS1,IMM_S,MEM_W,FN_ADD,N,M_XWR,N,N,N,N,N,N,N,N,N,N,N),
    SH->     List(N,N,N,B_N,N,N,CSR.N,N,A3_SD,A2_IMM,A1_RS1,IMM_S,MEM_H,FN_ADD,N,M_XWR,N,N,N,N,N,N,N,N,N,N,N),
    SB->     List(N,N,N,B_N,N,N,CSR.N,N,A3_SD,A2_IMM,A1_RS1,IMM_S,MEM_B,FN_ADD,N,M_XWR,N,N,N,N,N,N,N,N,N,N,N),
    LUI->    List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_IMM,A1_X,IMM_U,MEM_X,FN_A1ZERO,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    ADDI->   List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_IMM,A1_RS1,IMM_I,MEM_X,FN_ADD,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    SLTI->   List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_IMM,A1_RS1,IMM_I,MEM_X,FN_SLT,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    SLTIU->  List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_IMM,A1_RS1,IMM_I,MEM_X,FN_SLTU,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    ANDI->   List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_IMM,A1_RS1,IMM_I,MEM_X,FN_AND,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    ORI->    List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_IMM,A1_RS1,IMM_I,MEM_X,FN_OR,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    XORI->   List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_IMM,A1_RS1,IMM_I,MEM_X,FN_XOR,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    ADD->    List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_X,FN_ADD,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    SUB->    List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_X,FN_SUB,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    SLT->    List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_X,FN_SLT,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    SLTU->   List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_X,FN_SLTU,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    AND->    List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_X,FN_AND,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    OR->     List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_X,FN_OR,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    XOR->    List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_X,FN_XOR,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    SLL->    List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_X,FN_SL,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    SRL->    List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_X,FN_SR,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    SRA->    List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_X,FN_SRA,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    SLLI->   List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_IMM,A1_RS1,IMM_2,MEM_X,FN_SL,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    SRLI->   List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_IMM,A1_RS1,IMM_2,MEM_X,FN_SR,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    SRAI->   List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_IMM,A1_RS1,IMM_2,MEM_X,FN_SRA,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),

    ADDIW->  List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_IMM,A1_RS1,IMM_I,MEM_X,FN_ADD,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),

    MUL->    List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_X,FN_MUL,Y,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    MULH->   List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_X,FN_MULH,Y,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    MULHSU-> List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_X,FN_MULHSU,Y,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    MULHU->  List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_X,FN_MULHU,Y,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    DIV->    List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_X,FN_DIV,N,M_X,N,N,Y,N,N,N,Y,N,N,N,N),
    DIVU->   List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_X,FN_DIVU,N,M_X,N,N,Y,N,N,N,Y,N,N,N,N),
    REM->    List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_X,FN_REM,N,M_X,N,N,Y,N,N,N,Y,N,N,N,N),
    REMU->   List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_X,FN_REMU,N,M_X,N,N,Y,N,N,N,Y,N,N,N,N),

    FMADD_S -> List(N,Y,N,B_N,N,N,CSR.N,N,A3_FRS3,A2_RS2,A1_RS1,IMM_X,MEM_X,FN_FMADD,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    FMSUB_S -> List(N,Y,N,B_N,N,N,CSR.N,N,A3_FRS3,A2_RS2,A1_RS1,IMM_X,MEM_X,FN_FMSUB,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    FNMSUB_S-> List(N,Y,N,B_N,N,N,CSR.N,N,A3_FRS3,A2_RS2,A1_RS1,IMM_X,MEM_X,FN_FNMSUB,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    FNMADD_S-> List(N,Y,N,B_N,N,N,CSR.N,N,A3_FRS3,A2_RS2,A1_RS1,IMM_X,MEM_X,FN_FNMADD,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    FADD_S->   List(N,Y,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_X,FN_FADD,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    FSUB_S->   List(N,Y,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_X,FN_FSUB,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    FMUL_S->   List(N,Y,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_X,FN_FMUL,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    FDIV_S->   List(N,Y,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_X,FN_FDIV,N,M_X,N,N,Y,N,N,N,Y,N,N,N,N),
    FSQRT_S->  List(N,Y,N,B_N,N,N,CSR.N,N,A3_X,A2_X,A1_RS1,IMM_X,MEM_X,FN_FSQRT,N,M_X,N,N,Y,N,N,N,Y,N,N,N,N),
    FSGNJ_S->  List(N,Y,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_X,FN_FSGNJ,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    FSGNJN_S-> List(N,Y,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_X,FN_FSGNJN,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    FSGNJX_S-> List(N,Y,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_X,FN_FSGNJX,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    FMIN_S->   List(N,Y,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_X,FN_FMIN,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    FMAX_S->   List(N,Y,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_X,FN_FMAX,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    FCVT_W_S-> List(N,Y,N,B_N,N,N,CSR.N,N,A3_X,A2_X,A1_RS1,IMM_X,MEM_X,FN_F2I,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    FCVT_WU_S->List(N,Y,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_X,FN_F2IU,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    FSGNJ_S->  List(N,Y,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_X,FN_FSGNJ,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    FEQ_S->    List(N,Y,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_X,FN_FEQ,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    FLT_S->    List(N,Y,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_X,FN_FLT,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    FLE_S->    List(N,Y,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_X,FN_FLE,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    FCLASS_S-> List(N,Y,N,B_N,N,N,CSR.N,N,A3_X,A2_X,A1_RS1,IMM_X,MEM_X,FN_FCLASS,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    FCVT_S_W-> List(N,Y,N,B_N,N,N,CSR.N,N,A3_X,A2_X,A1_RS1,IMM_X,MEM_X,FN_I2F,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    FCVT_S_WU->List(N,Y,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_X,FN_IU2F,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),

    //zfinx: FLW FSW FMV.W.X FMV.X.W C.FLW[SP] C.FSW[SP]
    //FLW->      List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_IMM,A1_RS1,IMM_I,MEM_W,FN_ADD,Y,M_XRD,N,N,N,N,N,N,Y,N,N,N,N),
    //FSW->      List(N,N,N,B_N,N,N,CSR.N,N,A3_SD,A2_IMM,A1_RS1,IMM_S,MEM_W,FN_ADD,Y,M_XWR,N,N,N,N,N,N,N,N,N,N,N),
    //FMV_W_X->  List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_I,MEM_X,FN_ADD,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    //FMV_X_W->  List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_I,MEM_X,FN_ADD,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    //VFMERGE_VVM-> List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_VMERGE,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    //VFMERGE_VFM-> List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_VMERGE,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    //VFMERGE_VIM-> List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_IMM,IMM_V,MEM_X,FN_VMERGE,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),

    VLE32_V->   List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_X,A1_RS1,IMM_X,MEM_W,FN_ADD,N,M_XRD,N,N,N,Y,N,N,N,N,N,N,N),
    VLSE32_V->  List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_W,FN_ADD,N,M_XRD,N,N,N,Y,N,N,N,N,N,N,N),
    VLOXEI32_V->List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_W,FN_ADD,N,M_XRD,N,N,N,Y,N,N,N,N,N,N,N),
    VSE32_V->   List(Y,N,N,B_N,N,N,CSR.N,N,A3_SD,A2_X,A1_RS1,IMM_X,MEM_W,FN_ADD,N,M_XWR,N,N,N,N,N,N,N,N,N,N,N),
    VSSE32_V->  List(Y,N,N,B_N,N,N,CSR.N,N,A3_SD,A2_RS2,A1_RS1,IMM_X,MEM_W,FN_ADD,N,M_XWR,N,N,N,N,N,N,N,N,N,N,N),
    VSOXEI32_V->List(Y,N,N,B_N,N,N,CSR.N,N,A3_SD,A2_VRS2,A1_RS1,IMM_X,MEM_W,FN_ADD,N,M_XWR,N,N,N,N,N,N,N,N,N,N,N),

    VFMUL_VV->  List(Y,Y,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_FMUL,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VFMUL_VF->  List(Y,Y,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_FMUL,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VFMADD_VV-> List(Y,Y,N,B_N,N,N,CSR.N,N,A3_VRS3,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_VFMADD,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VFMADD_VF-> List(Y,Y,N,B_N,N,N,CSR.N,N,A3_VRS3,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_VFMADD,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VFNMADD_VV->List(Y,Y,N,B_N,N,N,CSR.N,N,A3_VRS3,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_VFNMADD,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VFNMADD_VF->List(Y,Y,N,B_N,N,N,CSR.N,N,A3_VRS3,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_VFNMADD,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VFMSUB_VV-> List(Y,Y,N,B_N,N,N,CSR.N,N,A3_VRS3,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_VFMSUB,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VFMSUB_VF-> List(Y,Y,N,B_N,N,N,CSR.N,N,A3_VRS3,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_VFMSUB,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VFNMSUB_VV->List(Y,Y,N,B_N,N,N,CSR.N,N,A3_VRS3,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_VFNMSUB,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VFNMSUB_VF->List(Y,Y,N,B_N,N,N,CSR.N,N,A3_VRS3,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_VFNMSUB,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),

    VFMACC_VV-> List(Y,Y,N,B_N,N,N,CSR.N,Y,A3_VRS3,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_FMADD,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VFMACC_VF-> List(Y,Y,N,B_N,N,N,CSR.N,Y,A3_VRS3,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_FMADD,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VFNMACC_VV->List(Y,Y,N,B_N,N,N,CSR.N,Y,A3_VRS3,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_FNMADD,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VFNMACC_VF->List(Y,Y,N,B_N,N,N,CSR.N,Y,A3_VRS3,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_FNMADD,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VFMSAC_VV-> List(Y,Y,N,B_N,N,N,CSR.N,Y,A3_VRS3,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_FMSUB,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VFMSAC_VF-> List(Y,Y,N,B_N,N,N,CSR.N,Y,A3_VRS3,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_FMSUB,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VFNMSAC_VV->List(Y,Y,N,B_N,N,N,CSR.N,Y,A3_VRS3,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_FNMSUB,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VFNMSAC_VF->List(Y,Y,N,B_N,N,N,CSR.N,Y,A3_VRS3,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_FNMSUB,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VADD_VV->   List(Y,N,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_ADD,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VADD_VX->   List(Y,N,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_ADD,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VADD_VI->   List(Y,N,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_IMM,IMM_V,MEM_X,FN_ADD,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VFADD_VV->  List(Y,Y,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_FADD,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VFADD_VF->  List(Y,Y,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_FADD,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VFSUB_VV->  List(Y,Y,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_FSUB,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VFSUB_VF->  List(Y,Y,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_FSUB,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VFRSUB_VF-> List(Y,Y,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_FSUB,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VSUB_VV->   List(Y,N,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_SUB,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VSUB_VX->   List(Y,N,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_SUB,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VRSUB_VX->  List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_SUB,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VRSUB_VI->  List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_IMM,IMM_V,MEM_X,FN_SUB,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VFMIN_VV->  List(Y,Y,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_FMIN,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VFMIN_VF->  List(Y,Y,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_FMIN,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VFMAX_VV->  List(Y,Y,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_FMAX,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VFMAX_VF->  List(Y,Y,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_FMAX,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VAND_VV->   List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_AND,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VAND_VX->   List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_AND,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VAND_VI->   List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_IMM,IMM_V,MEM_X,FN_AND,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VOR_VV->    List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_OR,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VOR_VX->    List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_OR,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VOR_VI->    List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_IMM,IMM_V,MEM_X,FN_OR,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VXOR_VV->   List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_XOR,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VXOR_VX->   List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_XOR,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VXOR_VI->   List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_IMM,IMM_V,MEM_X,FN_XOR,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMSEQ_VV->  List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_SEQ,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMSEQ_VX->  List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_SEQ,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMSEQ_VI->  List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_IMM,IMM_V,MEM_X,FN_SEQ,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMSNE_VV->  List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_SNE,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMSNE_VX->  List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_SNE,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMSNE_VI->  List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_IMM,IMM_V,MEM_X,FN_SNE,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMFEQ_VV->  List(Y,Y,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_FEQ,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMFEQ_VF->  List(Y,Y,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_FEQ,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMFNE_VV->  List(Y,Y,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_FNE,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMFNE_VF->  List(Y,Y,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_FNE,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMFLE_VV->  List(Y,Y,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_FLE,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMFLE_VF->  List(Y,Y,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_FLE,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMSLTU_VV-> List(Y,N,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_SLTU,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMSLTU_VX-> List(Y,N,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_SLTU,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMSLT_VV->  List(Y,N,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_SLT,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMSLT_VX->  List(Y,N,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_SLT,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMFLT_VV->  List(Y,Y,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_FLT,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMFLT_VF->  List(Y,Y,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_FLT,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMFGT_VF->  List(Y,Y,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_FLT,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMFGE_VF->  List(Y,Y,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_FLE,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VSLL_VV->   List(Y,N,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_SL,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VSLL_VX->   List(Y,N,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_SL,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VSLL_VI->   List(Y,N,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_IMM,IMM_V,MEM_X,FN_SL,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VSRL_VV->   List(Y,N,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_SR,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VSRL_VX->   List(Y,N,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_SR,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VSRL_VI->   List(Y,N,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_IMM,IMM_V,MEM_X,FN_SR,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VSRA_VV->   List(Y,N,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_SRA,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VSRA_VX->   List(Y,N,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_SRA,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VSRA_VI->   List(Y,N,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_IMM,IMM_V,MEM_X,FN_SRA,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),

    VMSLEU_VV-> List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_SGEU,N,M_X,N,N,N,Y,N,Y,N,N,N,N,N),
    VMSLEU_VI-> List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_IMM,IMM_Z,MEM_X,FN_SGEU,N,M_X,N,N,N,Y,N,Y,N,N,N,N,N),
    VMSLEU_VX-> List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_SGEU,N,M_X,N,N,N,Y,N,Y,N,N,N,N,N),
    VMSLE_VV->  List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_SGE,N,M_X,N,N,N,Y,N,Y,N,N,N,N,N),
    VMSLE_VI->  List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_IMM,IMM_V,MEM_X,FN_SGE,N,M_X,N,N,N,Y,N,Y,N,N,N,N,N),
    VMSLE_VX->  List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_SGE,N,M_X,N,N,N,Y,N,Y,N,N,N,N,N),
    VMSGTU_VI-> List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_IMM,IMM_Z,MEM_X,FN_SLTU,N,M_X,N,N,N,Y,N,Y,N,N,N,N,N),
    VMSGTU_VX-> List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_SLTU,N,M_X,N,N,N,Y,N,Y,N,N,N,N,N),
    VMSGT_VI->  List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_IMM,IMM_V,MEM_X,FN_SLTU,N,M_X,N,N,N,Y,N,Y,N,N,N,N,N),
    VMSGT_VX->  List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_SLTU,N,M_X,N,N,N,Y,N,Y,N,N,N,N,N),

    VMAND_MM->   List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_AND,N,M_X,N,N,N,Y,Y,Y,N,N,N,N,N),
    VMOR_MM->    List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_OR,N,M_X,N,N,N,Y,Y,Y,N,N,N,N,N),
    VMXOR_MM->   List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_XOR,N,M_X,N,N,N,Y,Y,Y,N,N,N,N,N),
    VMANDNOT_MM->List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_VMANDNOT,N,M_X,N,N,N,Y,Y,Y,N,N,N,N,N),
    VMORNOT_MM-> List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_VMORNOT,N,M_X,N,N,N,Y,Y,Y,N,N,N,N,N),
    VMNAND_MM->  List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_VMNAND,N,M_X,N,N,N,Y,Y,Y,N,N,N,N,N),
    VMNOR_MM->   List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_VMNOR,N,M_X,N,N,N,Y,Y,Y,N,N,N,N,N),
    VMXNOR_MM->  List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_VMXNOR,N,M_X,N,N,N,Y,Y,Y,N,N,N,N,N),

    VID_V->      List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_X,A1_X,IMM_X,MEM_X,FN_VID,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMERGE_VVM-> List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_VMERGE,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMERGE_VXM-> List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_VMERGE,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMERGE_VIM-> List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_IMM,IMM_V,MEM_X,FN_VMERGE,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),

    VMUL_VV->   List(Y,N,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_MUL,Y,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMUL_VX->   List(Y,N,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_MUL,Y,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMULH_VV->  List(Y,N,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_MULH,Y,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMULH_VX->  List(Y,N,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_MULH,Y,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMULHU_VV-> List(Y,N,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_MULHU,Y,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMULHU_VX-> List(Y,N,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_MULHU,Y,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMULHSU_VV->List(Y,N,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_MULHSU,Y,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMULHSU_VX->List(Y,N,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_MULHSU,Y,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMACC_VV->  List(Y,N,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_MACC,Y,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMACC_VX->  List(Y,N,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_MACC,Y,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VNMSAC_VV-> List(Y,N,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_NMSAC,Y,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VNMSAC_VX-> List(Y,N,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_NMSAC,Y,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMADD_VV->  List(Y,N,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_MADD,Y,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMADD_VX->  List(Y,N,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_MADD,Y,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VNMSUB_VV-> List(Y,N,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_NMSUB,Y,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VNMSUB_VX-> List(Y,N,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_NMSUB,Y,M_X,N,N,N,Y,N,N,N,N,N,N,N),

    //VMULH
    VREM_VV->   List(Y,N,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_REM,N,M_X,N,N,Y,Y,N,N,N,N,N,N,N),
    VREM_VX->   List(Y,N,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_REM,N,M_X,N,N,Y,Y,N,N,N,N,N,N,N),
    VREMU_VV->  List(Y,N,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_REMU,N,M_X,N,N,Y,Y,N,N,N,N,N,N,N),
    VREMU_VX->  List(Y,N,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_REMU,N,M_X,N,N,Y,Y,N,N,N,N,N,N,N),
    VDIV_VV->   List(Y,N,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_DIV,N,M_X,N,N,Y,Y,N,N,N,N,N,N,N),
    VDIV_VX->   List(Y,N,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_DIV,N,M_X,N,N,Y,Y,N,N,N,N,N,N,N),
    VDIVU_VV->  List(Y,N,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_DIVU,N,M_X,N,N,Y,Y,N,N,N,N,N,N,N),
    VDIVU_VX->  List(Y,N,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_DIVU,N,M_X,N,N,Y,Y,N,N,N,N,N,N,N),
    VFDIV_VV->  List(Y,Y,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_FDIV,N,M_X,N,N,Y,Y,N,N,N,N,N,N,N),
    VFDIV_VF->  List(Y,Y,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_FDIV,N,M_X,N,N,Y,Y,N,N,N,N,N,N,N),
    VFRDIV_VF-> List(Y,Y,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_FDIV,N,M_X,N,N,Y,Y,N,N,N,N,N,N,N),
    VFSQRT_V->  List(Y,Y,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_X,IMM_X,MEM_X,FN_FSQRT,N,M_X,N,N,Y,Y,N,N,N,N,N,N,N),


    VMINU_VV->  List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_MINU,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMAXU_VV->  List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_MAXU,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMIN_VV->   List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_MIN,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMAX_VV->   List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_MAX,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMINU_VX->  List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_MINU,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMAXU_VX->  List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_MAXU,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMIN_VX->   List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_MIN,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMAX_VX->   List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_MAX,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VFSGNJ_VV-> List(Y,Y,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_FSGNJ,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VFSGNJ_VF-> List(Y,Y,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_FSGNJ,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VFSGNJN_VV->List(Y,Y,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_FSGNJN,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VFSGNJN_VF->List(Y,Y,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_FSGNJN,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VFSGNJX_VV->List(Y,Y,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_FSGNJX,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VFSGNJX_VF->List(Y,Y,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_FSGNJX,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VFSGNJ_VV-> List(Y,Y,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_FSGNJ,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VFSGNJ_VF-> List(Y,Y,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_FSGNJ,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VFCVT_XU_F_V->List(Y,Y,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_X,IMM_X,MEM_X,FN_F2IU,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VFCVT_X_F_V-> List(Y,Y,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_X,IMM_X,MEM_X,FN_F2I,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VFCVT_F_XU_V->List(Y,Y,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_X,IMM_X,MEM_X,FN_IU2F,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VFCVT_F_X_V-> List(Y,Y,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_X,IMM_X,MEM_X,FN_I2F,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VFCLASS_V-> List(Y,Y,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_X,IMM_X,MEM_X,FN_FCLASS,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMV_V_V->   List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_VRS1,IMM_X,MEM_X,FN_ADD,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VFMV_V_F->  List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_X,FN_ADD,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMV_V_I->   List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_IMM,IMM_V,MEM_X,FN_ADD,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMV_V_X->   List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_X,FN_ADD,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMV_X_S->   List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_ADD,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    VSETVLI->   List(Y,N,N,B_N,N,N,CSR.S,N,A3_X,A2_X,A1_RS1,IMM_X,MEM_X,FN_ADD,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    VSETIVLI->  List(Y,N,N,B_N,N,N,CSR.S,N,A3_X,A2_X,A1_IMM,IMM_Z,MEM_X,FN_ADD,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    VSETVL->    List(Y,N,N,B_N,N,N,CSR.S,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_X,FN_ADD,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),

  )
}

class Control extends Module{
  val io=IO(new Bundle(){
    val inst=Input(UInt(32.W))
    val pc=Input(UInt(32.W))
    val wid=Input(UInt(depth_warp.W))
    val control=Output(new CtrlSigs())
  })
  val ctrlsignals=ListLookup(io.inst,IDecode.default,IDecode.table)
  io.control.inst:=io.inst
  io.control.wid:=io.wid
  io.control.pc:=io.pc
  io.control.mop:=io.inst(27,26)
  io.control.fp:=ctrlsignals(1)//fp=1->vFPU
  io.control.barrier:=ctrlsignals(2)//barrier or endprg->to warp_scheduler
  io.control.branch:=ctrlsignals(3)
  io.control.simt_stack:=ctrlsignals(4)
  io.control.simt_stack_op:=ctrlsignals(5)
  io.control.csr:=ctrlsignals(6)
  io.control.reverse:=ctrlsignals(7)//for some vector inst,change in1 and in2, e.g. subr
  io.control.isvec:=ctrlsignals(0)//isvec=1->vALU/vFPU
  io.control.sel_alu3:=ctrlsignals(8)
  io.control.mask:=((~io.inst(25)).asBool() | io.control.alu_fn===pipeline.IDecode.FN_VMERGE ) & io.control.isvec & !io.control.branch.orR() //一旦启用mask就会去读v0，所以必须这么写，避免标量指令也不小心读v0
  io.control.sel_alu2:=ctrlsignals(9)
  io.control.sel_alu1:=ctrlsignals(10)
  io.control.sel_imm:=ctrlsignals(11)
  io.control.mem_whb:=ctrlsignals(12)
  io.control.alu_fn:=ctrlsignals(13)
  io.control.mul:=ctrlsignals(14)
  io.control.mem:=io.control.mem_cmd.orR
  io.control.mem_cmd:=ctrlsignals(15)
  io.control.mem_unsigned:=ctrlsignals(16)
  io.control.fence:=ctrlsignals(17)
  io.control.sfu:=ctrlsignals(18)
  io.control.wfd:=ctrlsignals(19)
  io.control.readmask:=ctrlsignals(20) //read mode is mask - for mask bitwise opcode
  io.control.writemask:=ctrlsignals(21)//write mode is mask - for mask bitwise opcode
  io.control.wxd:=ctrlsignals(22)
  io.control.reg_idx1:=io.inst(19,15)
  io.control.reg_idx2:=io.inst(24,20)
  io.control.reg_idx3:=Mux(io.control.fp & !io.control.isvec,io.inst(31,27),io.inst(11,7))
  io.control.reg_idxw:=io.inst(11,7)

}

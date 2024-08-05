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
import chisel3.util._
import Instructions._
import top.parameters._

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
  def A3_X=0.U(2.W)
  val A3_VRS3=1.U(2.W)
  val A3_SD=3.U(2.W)
  val A3_FRS3=2.U(2.W)//for float(not vector). jalr use b_r to distinguish
  val A3_PC=0.U(2.W)
  def A1_X=0.U(2.W)//BitPat("b??")
  def A2_X=0.U(2.W)//BitPat("b??")
  val A2_RS2 = 1.U(2.W)
  val A2_IMM =3.U(2.W)
  val A2_VRS2 = 2.U(2.W)
  val A2_SIZE = 0.U(2.W)
  val IMM_I = 0.U(4.W)
  val IMM_S = 1.U(4.W)
  val IMM_B = 2.U(4.W)
  val IMM_U = 3.U(4.W)
  val IMM_2 = 4.U(4.W)//for rs2 as imm2
  val IMM_J = 5.U(4.W)
  val IMM_X = 0.U(4.W)//BitPat("b???")
  val IMM_V = 6.U(4.W)
  val IMM_Z = 7.U(4.W)
  val IMM_S11 = 8.U(4.W)
  val IMM_L11 = 9.U(4.W)

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
  def FN_A2ZERO = 9.U(6.W)
  def FN_MUL = 20.U(6.W)
  def FN_MULH = 21.U(6.W)
  def FN_MULHU = 22.U(6.W)
  def FN_MULHSU = 23.U(6.W)
  def FN_MACC = 24.U(6.W)
  def FN_NMSAC = 25.U(6.W)
  def FN_MADD = 26.U(6.W)
  def FN_NMSUB = 27.U(6.W)

  //vls12 inst
  def FN_VLS12 = 30.U(6.W)
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

  def FN_F2IU      = 24.U(6.W) // 011 000
  def FN_F2I     = 25.U(6.W) // 011 001
  def FN_IU2F      = 32.U(6.W) // 100 000
  def FN_I2F     = 33.U(6.W) // 100 001

  // for SFU
  def FN_DIV      = 0.U(6.W)
  def FN_REM      = 1.U(6.W)
  def FN_DIVU     = 2.U(6.W)
  def FN_REMU     = 3.U(6.W)
  def FN_FDIV     = 0.U(6.W)
  def FN_FSQRT    = 1.U(6.W)
  def FN_EXP      = 4.U(6.W)

  def FN_TTF = 1.U(6.W)
  def FN_TTH = 2.U(6.W)
  def FN_TTB = 3.U(6.W)

  //for atomic swap
  def FN_SWAP = 28.U(6.W)
  def FN_AMOADD = 29.U(6.W)
  val default = List(N,X,X,B_N,X,X,X,X,A3_X,A2_X,   A1_X,   IMM_X, MEM_X,  FN_X,     N,M_X,        X,X,X,X,X,X,X,X,X,X,X)

  //val table=Array(//: Array[(BitPat, List[BitPat])] = Array(
    //zfinx: FLW FSW FMV.W.X FMV.X.W C.FLW[SP] C.FSW[SP]
    //FLW->      List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_IMM,A1_RS1,IMM_I,MEM_W,FN_ADD,Y,M_XRD,N,N,N,N,N,N,Y,N,N,N,N),
    //FSW->      List(N,N,N,B_N,N,N,CSR.N,N,A3_SD,A2_IMM,A1_RS1,IMM_S,MEM_W,FN_ADD,Y,M_XWR,N,N,N,N,N,N,N,N,N,N,N),
    //FMV_W_X->  List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_I,MEM_X,FN_ADD,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    //FMV_X_W->  List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_I,MEM_X,FN_ADD,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    //VFMERGE_VVM-> List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_VMERGE,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    //VFMERGE_VFM-> List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_VMERGE,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    //VFMERGE_VIM-> List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_IMM,IMM_V,MEM_X,FN_VMERGE,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),

    // Moving V:Arithmetic -> IDecodeLUT_V
    // Moving V:LoadStore -> IDecodeLUT_VL
 // )
}

object IDecodeLUT_IMF{
  import IDecode._
  val table = Array(
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
    FEQ_S->    List(N,Y,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_X,FN_FEQ,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    FLT_S->    List(N,Y,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_X,FN_FLT,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    FLE_S->    List(N,Y,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_X,FN_FLE,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    FCLASS_S-> List(N,Y,N,B_N,N,N,CSR.N,N,A3_X,A2_X,A1_RS1,IMM_X,MEM_X,FN_FCLASS,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    FCVT_S_W-> List(N,Y,N,B_N,N,N,CSR.N,N,A3_X,A2_X,A1_RS1,IMM_X,MEM_X,FN_I2F,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    FCVT_S_WU->List(N,Y,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_X,FN_IU2F,N,M_X,N,N,N,N,N,N,Y,N,N,N,N)
  )
}

object IDecodeLUT_V{
  import IDecode._
  // with code 1010111
  val table = Array(
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
    VMSLE_VI->  List(Y,N,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_IMM,IMM_V,MEM_X,FN_SGE,N,M_X,N,N,N,Y,N,Y,N,N,N,N,N),//VMSLE_VI->  List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_IMM,IMM_V,MEM_X,FN_SGE,N,M_X,N,N,N,Y,N,Y,N,N,N,N,N),
    VMSLE_VX->  List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_SGE,N,M_X,N,N,N,Y,N,Y,N,N,N,N,N),
    VMSGTU_VI-> List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_IMM,IMM_Z,MEM_X,FN_SLTU,N,M_X,N,N,N,Y,N,Y,N,N,N,N,N),
    VMSGTU_VX-> List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_SLTU,N,M_X,N,N,N,Y,N,Y,N,N,N,N,N),
    VMSGT_VI->  List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_IMM,IMM_V,MEM_X,FN_SLTU,N,M_X,N,N,N,Y,N,Y,N,N,N,N,N),
    VMSGT_VX->  List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_SLTU,N,M_X,N,N,N,Y,N,Y,N,N,N,N,N),

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

    VMAND_MM->   List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_AND,N,M_X,N,N,N,Y,Y,Y,N,N,N,N,N),
    VMOR_MM->    List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_OR,N,M_X,N,N,N,Y,Y,Y,N,N,N,N,N),
    VMXOR_MM->   List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_XOR,N,M_X,N,N,N,Y,Y,Y,N,N,N,N,N),
    VMANDN_MM->  List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_VMANDNOT,N,M_X,N,N,N,Y,Y,Y,N,N,N,N,N),
    VMORN_MM->   List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_VMORNOT,N,M_X,N,N,N,Y,Y,Y,N,N,N,N,N),
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
    VMACC_VV->  List(Y,N,N,B_N,N,N,CSR.N,Y,A3_VRS3,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_MACC,Y,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMACC_VX->  List(Y,N,N,B_N,N,N,CSR.N,Y,A3_VRS3,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_MACC,Y,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VNMSAC_VV-> List(Y,N,N,B_N,N,N,CSR.N,Y,A3_VRS3,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_NMSAC,Y,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VNMSAC_VX-> List(Y,N,N,B_N,N,N,CSR.N,Y,A3_VRS3,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_NMSAC,Y,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMADD_VV->  List(Y,N,N,B_N,N,N,CSR.N,Y,A3_VRS3,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_MADD,Y,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMADD_VX->  List(Y,N,N,B_N,N,N,CSR.N,Y,A3_VRS3,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_MADD,Y,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VNMSUB_VV-> List(Y,N,N,B_N,N,N,CSR.N,Y,A3_VRS3,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_NMSUB,Y,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VNMSUB_VX-> List(Y,N,N,B_N,N,N,CSR.N,Y,A3_VRS3,A2_VRS2,A1_RS1,IMM_X,MEM_X,FN_NMSUB,Y,M_X,N,N,N,Y,N,N,N,N,N,N,N),

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
    VFCVT_XU_F_V->List(Y,Y,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_X,IMM_X,MEM_X,FN_F2IU,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VFCVT_X_F_V-> List(Y,Y,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_X,IMM_X,MEM_X,FN_F2I,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VFCVT_RTZ_XU_F_V->List(Y,Y,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_X,IMM_X,MEM_X,FN_F2IU,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VFCVT_RTZ_X_F_V-> List(Y,Y,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_X,IMM_X,MEM_X,FN_F2I,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VFCVT_F_XU_V->List(Y,Y,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_X,IMM_X,MEM_X,FN_IU2F,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VFCVT_F_X_V-> List(Y,Y,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_X,IMM_X,MEM_X,FN_I2F,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VFCLASS_V-> List(Y,Y,N,B_N,N,N,CSR.N,Y,A3_X,A2_VRS2,A1_X,IMM_X,MEM_X,FN_FCLASS,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMV_V_V->   List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_X,A1_VRS1,IMM_X,MEM_X,FN_A2ZERO,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VFMV_V_F->  List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_X,A1_RS1,IMM_X,MEM_X,FN_A2ZERO,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMV_V_I->   List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_IMM,A1_X,IMM_V,MEM_X,FN_A1ZERO,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMV_V_X->   List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_X,A1_RS1,IMM_X,MEM_X,FN_A2ZERO,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VMV_X_S->   List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_X,IMM_X,MEM_X,FN_A1ZERO,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),//TODO:
    VMV_S_X->   List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_X,A1_RS1,IMM_X,MEM_X,FN_A2ZERO,N,M_X,N,N,N,Y,N,N,N,N,N,N,N),
    VFMV_F_S -> List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_X,IMM_X,MEM_X,FN_A1ZERO,N,M_X,N,N,N,N,N,N,Y,N,N,N,N),
    VFMV_S_F -> List(Y, N, N, B_N, N, N, CSR.N, N, A3_X, A2_X, A1_RS1, IMM_X, MEM_X, FN_A2ZERO, N, M_X, N, N, N, Y, N, N, N, N, N, N, N),
    VSETVLI->   List(Y,N,N,B_N,N,N,CSR.S,N,A3_X,A2_X,A1_RS1,IMM_X,MEM_X,FN_ADD,N,M_X,N,N,N,N,N,N,Y,N,Y,N,N),
    VSETIVLI->  List(Y,N,N,B_N,N,N,CSR.S,N,A3_X,A2_X,A1_IMM,IMM_Z,MEM_X,FN_ADD,N,M_X,N,N,N,N,N,N,Y,N,Y,N,N),
    VSETVL->    List(Y,N,N,B_N,N,N,CSR.S,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_X,FN_ADD,N,M_X,N,N,N,N,N,N,Y,N,Y,N,N)
  )
}

object IDecodeLUT_VL{
  import IDecode._
  // with code 0000111, 0100111, 0101011
  val table = Array(

    VLE32_V -> List(Y, N, N, B_N, N, N, CSR.N, N, A3_X, A2_X, A1_RS1, IMM_X, MEM_W, FN_ADD, N, M_XRD, N, N, N, Y, N, N, N, N, N, N, N),
    VLSE32_V -> List(Y, N, N, B_N, N, N, CSR.N, N, A3_X, A2_RS2, A1_RS1, IMM_X, MEM_W, FN_ADD, N, M_XRD, N, N, N, Y, N, N, N, N, N, N, N),
    VLOXEI32_V -> List(Y, N, N, B_N, N, N, CSR.N, N, A3_X, A2_VRS2, A1_RS1, IMM_X, MEM_W, FN_ADD, N, M_XRD, N, N, N, Y, N, N, N, N, N, N, N),
    VSE32_V -> List(Y, N, N, B_N, N, N, CSR.N, N, A3_SD, A2_X, A1_RS1, IMM_X, MEM_W, FN_ADD, N, M_XWR, N, N, N, N, N, N, N, N, N, N, N),
    VSSE32_V -> List(Y, N, N, B_N, N, N, CSR.N, N, A3_SD, A2_RS2, A1_RS1, IMM_X, MEM_W, FN_ADD, N, M_XWR, N, N, N, N, N, N, N, N, N, N, N),
    VSOXEI32_V -> List(Y, N, N, B_N, N, N, CSR.N, N, A3_SD, A2_VRS2, A1_RS1, IMM_X, MEM_W, FN_ADD, N, M_XWR, N, N, N, N, N, N, N, N, N, N, N),

    VLW_V->   List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_IMM,A1_VRS1,IMM_L11,MEM_W,FN_ADD,N,M_XRD,N,N,N,Y,Y,N,N,N,Y,N,N),
    VLH_V->   List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_IMM,A1_VRS1,IMM_L11,MEM_H,FN_ADD,N,M_XRD,N,N,N,Y,Y,N,N,N,Y,N,N),
    VLB_V->   List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_IMM,A1_VRS1,IMM_L11,MEM_B,FN_ADD,N,M_XRD,N,N,N,Y,Y,N,N,N,Y,N,N),
    VLHU_V->  List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_IMM,A1_VRS1,IMM_L11,MEM_H,FN_ADD,N,M_XRD,Y,N,N,Y,Y,N,N,N,Y,N,N),
    VLBU_V->  List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_IMM,A1_VRS1,IMM_L11,MEM_B,FN_ADD,N,M_XRD,Y,N,N,Y,Y,N,N,N,Y,N,N),
    VSW_V->   List(Y,N,N,B_N,N,N,CSR.N,N,A3_SD,A2_IMM,A1_VRS1,IMM_S11,MEM_W,FN_ADD,N,M_XWR,N,N,N,N,Y,N,N,N,Y,N,N),
    VSH_V->   List(Y,N,N,B_N,N,N,CSR.N,N,A3_SD,A2_IMM,A1_VRS1,IMM_S11,MEM_H,FN_ADD,N,M_XWR,N,N,N,N,Y,N,N,N,Y,N,N),
    VSB_V->   List(Y,N,N,B_N,N,N,CSR.N,N,A3_SD,A2_IMM,A1_VRS1,IMM_S11,MEM_B,FN_ADD,N,M_XWR,N,N,N,N,Y,N,N,N,Y,N,N),
    VLW12_V-> List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_IMM,A1_VRS1,IMM_I,MEM_W,FN_VLS12,N,M_XRD,N,N,N,Y,Y,N,N,N,Y,N,N),
    VLH12_V-> List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_IMM,A1_VRS1,IMM_I,MEM_H,FN_VLS12,N,M_XRD,N,N,N,Y,Y,N,N,N,Y,N,N),
    VLB12_V-> List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_IMM,A1_VRS1,IMM_I,MEM_B,FN_VLS12,N,M_XRD,N,N,N,Y,Y,N,N,N,Y,N,N),
    VLHU12_V->List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_IMM,A1_VRS1,IMM_I,MEM_H,FN_VLS12,N,M_XRD,Y,N,N,Y,Y,N,N,N,Y,N,N),
    VLBU12_V->List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_IMM,A1_VRS1,IMM_I,MEM_B,FN_VLS12,N,M_XRD,Y,N,N,Y,Y,N,N,N,Y,N,N),
    VSW12_V-> List(Y,N,N,B_N,N,N,CSR.N,N,A3_SD,A2_IMM,A1_VRS1,IMM_S,MEM_W,FN_VLS12,N,M_XWR,N,N,N,N,Y,N,N,N,Y,N,N),
    VSH12_V-> List(Y,N,N,B_N,N,N,CSR.N,N,A3_SD,A2_IMM,A1_VRS1,IMM_S,MEM_H,FN_VLS12,N,M_XWR,N,N,N,N,Y,N,N,N,Y,N,N),
    VSB12_V-> List(Y,N,N,B_N,N,N,CSR.N,N,A3_SD,A2_IMM,A1_VRS1,IMM_S,MEM_B,FN_VLS12,N,M_XWR,N,N,N,N,Y,N,N,N,Y,N,N)
  )
}
object IDecodeLUT_A{ //The last element of the list indicates whether the instruction is atomic.
  import IDecode._
  val table= Array(
    LR_W -> List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_X,A1_RS1,IMM_X,MEM_W,FN_ADD,N,M_XRD,N,N,N,N,N,N,Y,N,N,N,Y),
    SC_W ->     List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_W,FN_ADD,N,M_XWR,N,N,N,N,N,N,Y,N,N,N,Y),
    AMOSWAP_W-> List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_W,FN_SWAP,N,M_XWR,N,N,N,N,N,N,Y,N,N,N,Y),
    AMOADD_W -> List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_W,FN_AMOADD,N,M_XWR,N,N,N,N,N,N,Y,N,N,N,Y),
    AMOXOR_W-> List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_W,FN_XOR,N,M_XWR,N,N,N,N,N,N,Y,N,N,N,Y),
    AMOAND_W-> List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_W,FN_AND,N,M_XWR,N,N,N,N,N,N,Y,N,N,N,Y),
    AMOOR_W -> List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_W,FN_OR,N,M_XWR,N,N,N,N,N,N,Y,N,N,N,Y),
    AMOMIN_W -> List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_W,FN_MIN,N,M_XWR,N,N,N,N,N,N,Y,N,N,N,Y),
    AMOMAX_W ->List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_W,FN_MAX,N,M_XWR,N,N,N,N,N,N,Y,N,N,N,Y),
    AMOMINU_W -> List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_W,FN_MINU,N,M_XWR,Y,N,N,N,N,N,Y,N,N,N,Y),
    AMOMAXU_W ->List(N,N,N,B_N,N,N,CSR.N,N,A3_X,A2_RS2,A1_RS1,IMM_X,MEM_W,FN_MAXU,N,M_XWR,Y,N,N,N,N,N,Y,N,N,N,Y)
  )
}
object IDecodeLUT_VC{
  import IDecode._
  // with code 1011011, 0001011
  val table = Array(
    VBNE->   List(Y,N,N,B_B,Y,N,CSR.N,Y,A3_PC,A2_VRS2,A1_VRS1,IMM_B,MEM_X,FN_SNE,N,M_X,N,N,N,N,N,N,N,N,Y,N,N),
    VBEQ->   List(Y,N,N,B_B,Y,N,CSR.N,Y,A3_PC,A2_VRS2,A1_VRS1,IMM_B,MEM_X,FN_SEQ,N,M_X,N,N,N,N,N,N,N,N,Y,N,N),
    VBLT->   List(Y,N,N,B_B,Y,N,CSR.N,Y,A3_PC,A2_VRS2,A1_VRS1,IMM_B,MEM_X,FN_SLT,N,M_X,N,N,N,N,N,N,N,N,Y,N,N),
    VBLTU->  List(Y,N,N,B_B,Y,N,CSR.N,Y,A3_PC,A2_VRS2,A1_VRS1,IMM_B,MEM_X,FN_SLTU,N,M_X,N,N,N,N,N,N,N,N,Y,N,N),
    VBGE->   List(Y,N,N,B_B,Y,N,CSR.N,Y,A3_PC,A2_VRS2,A1_VRS1,IMM_B,MEM_X,FN_SGE,N,M_X,N,N,N,N,N,N,N,N,Y,N,N),
    VBGEU->  List(Y,N,N,B_B,Y,N,CSR.N,Y,A3_PC,A2_VRS2,A1_VRS1,IMM_B,MEM_X,FN_SGEU,N,M_X,N,N,N,N,N,N,N,N,Y,N,N),
    JOIN->   List(Y,N,N,B_B,Y,Y,CSR.N,N,A3_PC,A2_X,A1_X,IMM_B,MEM_X,FN_ADD,N,M_X,N,N,N,N,N,N,N,N,Y,N,N),
    SETRPC-> List(N,N,N,B_N,N,N,CSR.W,N,A3_X,A2_IMM,A1_RS1,IMM_I,MEM_X,FN_ADD,N,M_X,N,N,N,N,N,N,Y,N,N,Y,N),
    BARRIER->List(N,N,Y,B_N,N,N,CSR.N,N,A3_X,A2_X,A1_IMM,IMM_Z,MEM_X,FN_ADD,N,M_X,N,N,N,N,N,N,N,N,N,N,N),
    BARRIERSUB->List(N,N,Y,B_N,N,N,CSR.N,N,A3_X,A2_X,A1_IMM,IMM_Z,MEM_X,FN_ADD,N,M_X,N,N,N,N,N,N,N,N,N,N,N),
    ENDPRG-> List(N,N,Y,B_N,N,Y,CSR.N,N,A3_X,A2_X,A1_X,IMM_X,MEM_X,FN_ADD,N,M_X,N,N,N,N,N,N,N,N,N,N,N),

    VADD12_VI->   List(Y,N,N,B_N,N,N,CSR.N,Y,A3_X,A2_IMM,A1_VRS1,IMM_I,MEM_X,FN_ADD,N,M_X,N,N,N,Y,N,N,N,N,Y,N,N),
    VSUB12_VI->   List(Y,N,N,B_N,N,N,CSR.N,N,A3_X,A2_IMM,A1_VRS1,IMM_I,MEM_X,FN_SUB,N,M_X,N,N,N,Y,N,N,N,N,Y,N,N),
    VFTTA_VV->List(Y,Y,N,B_N,N,N,CSR.N,N,A3_VRS3,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_TTF,N,M_X,N,N,N,Y,N,N,N,Y,N,N,N),
    VFEXP_V ->List(Y,Y,N,B_N,N,N,CSR.N,N,A3_X,A2_VRS2,A1_X,IMM_X,MEM_X,FN_EXP,N,M_X,N,N,Y,Y,N,N,N,N,N,N,N)
    //VHTTA_VV->List(Y,Y,N,B_N,N,N,CSR.N,N,A3_VRS3,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_TTH,N,M_X,N,N,N,Y,N,N,N,Y,N,N,N),
    //VBTTA_VV->List(Y,Y,N,B_N,N,N,CSR.N,N,A3_VRS3,A2_VRS2,A1_VRS1,IMM_X,MEM_X,FN_TTB,N,M_X,N,N,N,Y,N,N,N,Y,N,N,N),

  )
}

//trait DecodeParameters{}

// NOW: num_fetch = 2, num_issue = 1
class InstrDecodeV2 extends Module {
  val io = IO(new Bundle{
    val inst = Input(Vec(num_fetch, UInt(instLen.W)))
    val inst_mask = Input(Vec(num_fetch, Bool()))
    val pc = Input(UInt(addrLen.W))
    val wid = Input(UInt(depth_warp.W))
    val sm_id = Input(UInt(8.W))
    val flush_wid = Flipped(ValidIO(UInt(depth_warp.W)))
    val control = Output(Vec(num_fetch, new CtrlSigs))
    val control_mask = Output(Vec(num_fetch, Bool()))
    val ibuffer_ready = Input(Vec(num_warp, Bool()))
  })
  class regext extends Bundle{
    val isExt = Bool() // regext
    val isExtI = Bool() // regexti
    val immHigh = UInt(6.W)
    val regPrefix = Vec(4, UInt(3.W)) // 0 -> rd, 1 -> rs1, 2 -> rs2, 3 -> rs3
  }
  val regextInfo_pre = Wire(Vec(num_fetch, new regext))
  regextInfo_pre.zipWithIndex.foreach{ case(r, i) =>
    when(!io.inst_mask(i)){
      r := 0.U.asTypeOf(r)
    }.otherwise{
      r.isExt := io.inst(i) === REGEXT
      r.isExtI := io.inst(i) === REGEXTI
      r.immHigh := Mux(r.isExtI, io.inst(i)(31,26), 0.U)
      when(r.isExt){
        r.regPrefix := VecInit(io.inst(i)(22,20), io.inst(i)(25,23), io.inst(i)(28,26), io.inst(i)(31,29))
      }.elsewhen(r.isExtI){
        r.regPrefix := VecInit(io.inst(i)(22,20), 0.U(3.W), io.inst(i)(25,23), 0.U(3.W))
      }.otherwise{
        r.regPrefix := 0.U.asTypeOf(r.regPrefix)
      }
    }
  }
  // eg1.               0    1    2    3                    |2.  0    1    2    3                       |3. 0    1    2    3
  // inst               0  EXT>   A    B  Scratch: N        |    A  EXT>  B   EXT> Scratch: EXT         |   0    A    B    EXT> Scratch: EXT
  // inst_mask          0    1    1    1                    |    1    1    1    1                       |   0    1    1    0
  // regextInfo_pre     N  EXT    N    N                    |   [N  EXT    N  EXT]                      |  [N    N    N    N]
  // regextInfo         N    N  EXT    N  N->Scratch        |  EXT   [N  EXT    N  EXT]->Scratch        | EXT   [N    N    N   NC]->Scratch // NoChange
  // maskAfterExt       0    0    1    1                    |    1    0    1    0                       |   0    1    1    0
  // result             0    0   EA    B                    |   EA    0   EB    0                       |   0    A    B    0
  val scratchPads = RegInit(VecInit(Seq.fill(num_warp)(0.U.asTypeOf(new regext))))
  when(io.flush_wid.valid){
    scratchPads(io.flush_wid.bits) := 0.U.asTypeOf(new regext)
    when(io.flush_wid.bits =/= io.wid && io.inst_mask.last && io.ibuffer_ready(io.wid)){
      scratchPads(io.wid) := regextInfo_pre.last
    }
  }.otherwise{
    when(io.inst_mask.last && io.ibuffer_ready(io.wid)){ scratchPads(io.wid) := regextInfo_pre.last }
  }
  // regextInfo: 0<>Scratchpad, 1<>decode_0, 2<>decode_1, 3<>decode_2
  val regextInfo = VecInit(Seq(scratchPads(io.wid)) ++ regextInfo_pre.take(num_fetch-1))

  val maskAfterExt = Wire(Vec(num_fetch, Bool()))
  (0 until num_fetch).foreach{ i =>
    maskAfterExt(i) := io.inst_mask(i) && !(regextInfo_pre(i).isExtI || regextInfo_pre(i).isExt) // is Instr but not EXTs
  }

  val ctrlSignals = (0 until num_fetch).map( i => {
    val lut = Seq(IDecodeLUT_IMF.table, IDecodeLUT_V.table, IDecodeLUT_VL.table, IDecodeLUT_VC.table).map { t =>
      ListLookup(io.inst(i), IDecode.default, t)
    }
    ListLookup(io.inst(i)(6, 0), lut(0),
      Array(
        BitPat("b1010111") -> lut(1),
        BitPat("b1111011") -> lut(2),
        BitPat("b0?00111") -> lut(2),
        BitPat("b0101011") -> lut(2),
        BitPat("b1011011") -> lut(3),
        BitPat("b0001011") -> lut(3)
      ))
  })
  (ctrlSignals zip io.control).zipWithIndex.foreach{ case((s, c), i) =>
    c.inst := io.inst(i)
    c.wid := io.wid
    c.pc := io.pc + (i.U << 2.U) // for multi-fetching
    c.mop :=  Mux(c.readmask,3.U(2.W),io.inst(i)(27,26))
    c.fp := s(1) //fp=1->vFPU
    c.barrier := s(2) //barrier or endprg->to warp_scheduler
    c.branch := s(3)
    c.simt_stack := s(4)
    c.simt_stack_op := s(5)
    c.csr := s(6)
    c.reverse := s(7) //for some vector inst,change in1 and in2, e.g. subr
    c.isvec := s(0) //isvec=1->vALU/vFPU
    c.sel_alu3 := s(8)
    c.mask := ((~io.inst(i)(25)).asBool | c.alu_fn === pipeline.IDecode.FN_VMERGE) & c.isvec & !c.disable_mask //一旦启用mask就会去读v0，所以必须这么写，避免标量指令也不小心读v0
    c.sel_alu2 := s(9)
    c.sel_alu1 := s(10)
    c.sel_imm := s(11)
    c.mem_whb := s(12)
    c.alu_fn := s(13)
    c.force_rm_rtz := io.inst(i) === Instructions.VFCVT_RTZ_X_F_V || io.inst(i) === pipeline.Instructions.VFCVT_RTZ_XU_F_V
    c.is_vls12 := s(13) === pipeline.IDecode.FN_VLS12
    c.mul := s(14)
    c.mem := c.mem_cmd.orR
    c.mem_cmd := s(15)
    c.mem_unsigned := s(16)
    c.fence := s(17)
    c.sfu := s(18)
    c.wvd := s(19)
    c.readmask := s(20) //read mode is mask - for mask bitwise opcode ; for custom load/store -> addr add type & opc A3_SD type
    c.writemask := 0.U//s(21) //write mode is mask - for mask bitwise opcode// c.writemask := s(21) //write mode is mask - for mask bitwise opcode
    c.wxd := s(22)
    c.tc := s(23)
    c.disable_mask := s(24)
    c.custom_signal_0 := s(25)
    c.reg_idx1 := Cat(regextInfo(i).regPrefix(1), io.inst(i)(19, 15))
    c.reg_idx2 := Cat(regextInfo(i).regPrefix(2), io.inst(i)(24, 20))
    c.reg_idx3 := Mux(c.fp & !c.isvec, Cat(0.U(3.W),io.inst(i)(31, 27)), Cat(regextInfo(i).regPrefix(0) ,io.inst(i)(11, 7)))
    c.reg_idxw := Cat(regextInfo(i).regPrefix(0), io.inst(i)(11, 7))
    c.imm_ext := Cat(regextInfo(i).isExtI, regextInfo(i).immHigh) // pack exti valid bit at MSB
    if (SPIKE_OUTPUT) {
      c.spike_info.get.sm_id := io.sm_id
      c.spike_info.get.inst := io.inst(i)
      c.spike_info.get.pc := io.pc+ (i.U << 2.U)
    }
    c.atomic :=s(26)
    c.aq :=s(26) & io.inst(i)(26)
    c.rl:=s(26) & io.inst(i)(25)
  }
  io.control_mask := maskAfterExt
}

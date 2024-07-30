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

object FPUOps{
  val SZ_FPU_FUNC = 6
  def FN_FX       = BitPat("b??????")
  def FN_FADD     = 0.U(6.W)  // 000 000      io.(b + a * 1)
  def FN_FSUB     = 1.U(6.W)  // 000 001      io.(b * 1 - a)
  def FN_FMUL     = 2.U(6.W)  // 000 010      io.(a * b + 0)
  def FN_FMADD    = 4.U(6.W)  // 000 100      io.(a * b + c)
  def FN_FMSUB    = 5.U(6.W)  // 000 101      io.(a * b - c)
  def FN_FNMSUB   = 6.U(6.W)  // 000 110      io.(-a * b + c)
  def FN_FNMADD   = 7.U(6.W)  // 000 111      io.(-a * b - c)

  def FN_MIN      = 8.U(6.W)  // 001 000
  def FN_MAX      = 9.U(6.W)  // 001 001
  def FN_FLE      = 10.U(6.W) // 001 010
  def FN_FLT      = 11.U(6.W) // 001 011
  def FN_FEQ      = 12.U(6.W) // 001 100
  def FN_FNE      = 13.U(6.W) // 001 101

  def FN_FCLASS   = 18.U(6.W) // 010 010
  def FN_FSGNJ    = 22.U(6.W) // 010 110
  def FN_FSGNJN   = 21.U(6.W) // 010 101
  def FN_FSGNJX   = 20.U(6.W) // 010 100

  def FN_F2IU = 24.U(6.W) // 011 000
  def FN_F2I = 25.U(6.W) // 011 001
  def FN_IU2F = 32.U(6.W) // 100 000
  def FN_I2F = 33.U(6.W) // 100 001
}

class FloatPoint(val expWidth: Int, val fracWidth: Int) extends Bundle{
  val sign        = Bool()
  val exp         = UInt(expWidth.W)
  val frac        = UInt(fracWidth.W)
  def defaultNaN: UInt = Cat(0.U(1.W), Fill(expWidth+1, 1.U(1.W)), Fill(fracWidth-1, 0.U(1.W)))
  def posInf:     UInt = Cat(0.U(1.W), Fill(expWidth, 1.U(1.W)), 0.U(fracWidth.W))
  def negInf:     UInt = Cat(1.U(1.W), Fill(expWidth, 1.U(1.W)), 0.U(fracWidth.W))
  def maxNorm:    UInt = Cat(0.U(1.W), Fill(expWidth-1, 1.U(1.W)), 0.U(1.W), Fill(fracWidth,1.U(1.W)))
  def expBias:    UInt = Fill(expWidth-1, 1.U(1.W))
  def expBiasInt: Int = (1 << (expWidth-1)) - 1       //  f32 -> b0111_1111 = 127
  def fracExt:    UInt = Cat(exp=/=0.U, frac)
  def apply(x: UInt): FloatPoint = x.asTypeOf(new FloatPoint(expWidth, fracWidth))
}
class Fflags extends Bundle{
  val invalid = Bool()
  val infinite = Bool()
  val overflow = Bool()
  val underflow = Bool()
  val inexact = Bool()
}

object RoundingMode{
  val RNE = 0.U(3.W)
  val RTZ = 1.U(3.W)
  val RDN = 2.U(3.W)
  val RUP = 3.U(3.W)
  val RMM = 4.U(3.W)
}

object Float32 extends FloatPoint(8, 23)

/*object SignExt{
    def apply(a: UInt, len: UInt) = {
        val alen = a.getWidth
        val signBit = a(alen-1)
        if (alen == len) a else Cat(Fill(len-alen, signBit), a)
    }
}*/

trait HasUIntToSIntHelper {
  implicit class UIntToSIntHelper(x: UInt){
    def toSInt: SInt = Cat(0.U(1.W), x).asSInt
  }
}
class FPUoutput extends Bundle{
  val result              = Output(UInt(32.W))
  val flags               = Output(new Fflags)
}
class FPUinput extends Bundle{
  val op                  = Input(UInt(3.W))
  val a, b, c             = Input(UInt(32.W))
  val rm                  = Input(UInt(3.W))
}
abstract class FPUSubModule extends Module with HasUIntToSIntHelper{
  val io = IO(new Bundle{
    val in = Flipped(DecoupledIO(new FPUinput))
    val out = DecoupledIO(new FPUoutput)
  })
}

class RoundingUnit(fracWidth: Int) extends Module{
  import RoundingMode._
  val io = IO(new Bundle{
    val in = Input(new Bundle{
      val rm = UInt(3.W)
      val frac = UInt(fracWidth.W)
      val sign, guard, round, sticky = Bool()
    })
    val out = Output(new Bundle{
      val fracRounded = UInt(fracWidth.W)
      val inexact = Bool()
      val fracCout = Bool()
      val roundUp = Bool()
    })
  })

  val inexact = io.in.guard | io.in.round | io.in.sticky
  val lsb = io.in.frac(0)
  val roundUp = MuxLookup(io.in.rm, false.B)(Seq(
    RNE -> (io.in.guard && (io.in.round | io.in.sticky | lsb)),
    RTZ -> false.B,
    RUP -> (inexact & (!io.in.sign)),
    RDN -> (inexact & io.in.sign),
    RMM -> io.in.guard
  ))
  val fracRoundUp = io.in.frac +& 1.U
  val cout = fracRoundUp(fracWidth)
  val fracRounded = Mux(roundUp, fracRoundUp(fracWidth-1,0), io.in.frac)
  io.out.inexact := inexact
  io.out.fracRounded := fracRounded
  io.out.fracCout := cout & roundUp
  io.out.roundUp := roundUp
}
//fake Array Multiplier
class ArrayMultiplier1(width: Int) extends Module{   //width = 25
  val io = IO(new Bundle{
    val a, b = Input(UInt(width.W))
    val carry, sum = Output(UInt((2*width).W))
  })
  val (a, b) = (io.a, io.b)
  val (sum, carry) = (a*b, 0.U)
  io.sum := sum
  io.carry := carry
}


object ShiftRightJam{
  def apply(x: UInt, shiftAmt: UInt, w: Int): UInt = {
    val xLen = if(x.getWidth < w) w else x.getWidth
    val x_ext = Wire(UInt(xLen.W))
    x_ext := (if(x.getWidth < w) Cat(x, 0.U((w-x.getWidth).W)) else x)
    val realShiftAmt = Mux(shiftAmt > (w-1).U,
      w.U,
      shiftAmt(log2Up(w)-1,0)
    )
    val mask = ((-1).S(xLen.W).asUInt >> (w.U-realShiftAmt)).asUInt
    val x_shifted = Wire(UInt(xLen.W))
    x_shifted := x_ext >> realShiftAmt
    x_shifted.head(w) | ((mask & x_ext).orR)
  }
}
object ShiftLeftJam{
  def apply(x: UInt, shiftAmt: UInt, w: Int): UInt = {
    val xLen =if(x.getWidth < w) w else x.getWidth
    val x_shifted = Wire(UInt(xLen.W))
    x_shifted := Mux(shiftAmt > (xLen-1).U,
      0.U,
      x << shiftAmt(log2Up(xLen)-1, 0)
    )
    x_shifted.head(w) | (x_shifted.tail(w).orR)
  }
}

class SrtTable extends Module{
  val io = IO(new Bundle{
    val d = Input(UInt(3.W))
    val y = Input(UInt(8.W))
    val q = Output(SInt(3.W))
  })
  val qSelTable = Array(
    Array(24, 8, -8, -26),
    Array(28, 8, -10, -28),
    Array(32, 8, -12, -32),
    Array(32, 8, -12, -34),
    Array(36, 12, -12, -36),
    Array(40, 12, -16, -40),
    Array(40, 16, -16, -44),
    Array(48, 16, -16, -46)
  )
  var ge = Map[Int, Bool]()
  for(row <- qSelTable){
    for(k <- row){
      if(!ge.contains(k)) ge = ge + (k -> (io.y.asSInt >= k.S(8.W)))
    }
  }
  io.q := MuxLookup(io.d, 0.S)(
    qSelTable.map(x =>
      MuxCase((-2).S(3.W), Seq(
        ge(x(0)) -> 2.S(3.W),
        ge(x(1)) -> 1.S(3.W),
        ge(x(2)) -> 0.S(3.W),
        ge(x(3)) -> (-1).S(3.W)
      ))
    ).zipWithIndex.map({case(v, i) => i.U -> v})
  )
}

class OnTheFlyConv(len: Int) extends Module{    // len = 28 + 3 = 31
  val io = IO(new Bundle{
    val resetSqrt = Input(Bool())
    val resetDiv = Input(Bool())
    val enable = Input(Bool())
    val qi = Input(SInt(3.W))
    val QM = Output(UInt(len.W))
    val Q = Output(UInt(len.W))
    val F = Output(UInt(len.W))
  })
  val Q, QM = Reg(UInt(len.W))

  val mask = Reg(SInt(len.W))
  val b_111, b_1100 = Reg(UInt(len.W))
  when(io.resetSqrt){
    mask := Cat("b1".U(1.W), 0.U((len-1).W)).asSInt
    b_111 := "b111".U(3.W) << (len-5)   // 001.11....
    b_1100 := "b1100".U(4.W) << (len-5) // 011.00....
  }.elsewhen(io.enable){
    mask := mask >> 2
    b_111 := b_111 >> 2
    b_1100 := b_1100 >> 2
  }
  val b_00, b_01, b_10, b_11 = Reg(UInt((len-3).W))
  b_00 := 0.U
  when(io.resetSqrt || io.resetDiv){
    b_01 := Cat("b01".U(2.W), 0.U((len-5).W)) // 01....
    b_10 := Cat("b10".U(2.W), 0.U((len-5).W)) // 10....
    b_11 := Cat("b11".U(2.W), 0.U((len-5).W)) // 11....
  }.elsewhen(io.enable){
    b_01 := b_01 >> 2
    b_10 := b_10 >> 2
    b_11 := b_11 >> 2
  }

  val negQ = ~Q
  val sqrtToCsaMap = Seq(
    1 ->  (negQ, b_111),
    2 ->  (negQ, b_1100),
    -1 -> (QM, b_111),
    -2 -> (QM, b_1100)
  ).map(
    m => m._1.S(3.W).asUInt -> (
      ( (m._2._1 << Mux(io.qi(0), 1.U, 2.U)).asUInt & (mask >> io.qi(0)).asUInt ) | m._2._2
      )
  )
  val sqrtToCsa = MuxLookup(io.qi.asUInt, 0.U)(sqrtToCsaMap)

  val Q_load_00 = Q | b_00
  val Q_load_01 = Q | b_01
  val Q_load_10 = Q | b_10
  val QM_load_01 = QM | b_01
  val QM_load_10 = QM | b_10
  val QM_load_11 = QM | b_11

  when(io.resetSqrt){
    Q := Cat(1.U(3.W), 0.U((len-3).W))  // 001.00...
    QM := 0.U
  }.elsewhen(io.resetDiv){
    Q := 0.U
    QM := 0.U
  }.elsewhen(io.enable){
    val QConvMap = Seq(
      0 -> Q_load_00,   // A[j+1] = A[j] ::: q
      1 -> Q_load_01,
      2 -> Q_load_10,
      -1 -> QM_load_11,   // A[j+1] = B[j] ::: (r-|q|)
      -2 -> QM_load_10
    ).map(m => m._1.S(3.W).asUInt -> m._2)
    val QMConvMap = Seq(
      1 -> Q_load_00,   // B[j+1] = A[j] ::: (q-1)
      2 -> Q_load_01,
      0 -> QM_load_11,
      -1 -> QM_load_10, // B[j+1] = B[j] ::: (r-|q|-1)
      -2 -> QM_load_01
    ).map(m => m._1.S(3.W).asUInt -> m._2)
    Q := MuxLookup(io.qi.asUInt, 0.U)(QConvMap)
    QM := MuxLookup(io.qi.asUInt, 0.U)(QMConvMap)
  }
  io.F := sqrtToCsa
  io.QM := QM
  io.Q := Q
}

class CSA32(len: Int) extends Module{
  val io = IO(new Bundle{
    val in = Input(Vec(3, UInt(len.W)))
    val out = Output(Vec(2, UInt(len.W)))
  })
  val (a, b, ci) = (io.in(0), io.in(1), io.in(2))
  val s = a ^ b ^ ci
  val co = (a&b) | ((a^b)&ci)
  io.out(0) := s
  io.out(1) := co
}

object FPUDebug {
  // don't care GTimer in FPU tests
  def apply(flag: Boolean = false, cond: Bool = true.B)(body: => Unit): Any =
    if (flag) { when (cond) { body } }
}
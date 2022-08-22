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


// FPU design comes from https://github.com/ljwljwljwljw/FPU
// Multiplier is replaced with 2-stage ArrayMultiplier by XiangShan

package pipeline

import chisel3._
import chisel3.util._

/*
==== Supported FPUOps ====
arithmetic: add sub mul    | vf{}.{vv, vf}
compare: eq le lt | vmf{}.{vv, vf}
convert: vfcvt._
move: fmv
optional? :(fsgn{}, mad ...)
*/


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
  val roundUp = MuxLookup(io.in.rm, false.B, Seq(
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

class FPU_FMA extends FPUSubModule with HasPipelineReg1{
  def latency = 5
  import RoundingMode._
  def F_EXP_WIDTH:    Int = Float32.expWidth + 2      //  value: 8 + 2 = 10
  def F_FRAC_WIDTH:   Int = Float32.fracWidth + 1     //  value: 23 + 1 = 24
  def ADD_WIDTH:      Int = 3*F_FRAC_WIDTH + 2        //  value: 3 * 24 + 2 = 74
  def INITIAL_EXP_DIFF:   Int = Float32.fracWidth + 4 //  value: 23 + 4 = 27

  def expOverflow(sexp: SInt, expWidth: Int): Bool = sexp >= Cat(0.U(1.W), Fill(expWidth, 1.U(1.W))).asSInt()
  def expOverflow(uexp: UInt, expWidth: Int): Bool = expOverflow(Cat(0.U(1.W), uexp).asSInt(), expWidth)
  /* STAGE1 *****************************************************************************/
  val rm = io.in.bits.rm
  val op = io.in.bits.op
  val rs0 = io.in.bits.a
  val rs1 = io.in.bits.b
  val rs2 = io.in.bits.c
  val zero = 0.U(Float32.getWidth.W)
  val one = Cat(0.U(1.W), Float32.expBiasInt.U(Float32.expWidth.W), 0.U(Float32.fracWidth.W))

  val a = {
    val x = Mux(op(2),
      rs2,            //  isMAD -> b(rs0) * c(rs1) + a(rs2)
      Mux(op(1),
        zero,       //  isMUL -> b(rs0) * c(rs1) + a(zero)
        rs1         //  isADD -> b(rs0) * c(one) + a(rs1)
      )
    )
    val sign = x.head(1) ^ op(0)    //  isSUB -> b(rs0) * c(one) - a(rs1)
    Cat(sign, x.tail(1))
  }
  val b = rs0
  val c = Mux(op(2,1)==="b00".U, one, rs1)    //  isADD? -> b(rs0) * 1 + a

  val operands = Seq(a, b, c)
  val classify = Array.fill(3)(Module(new Classify(Float32.expWidth, Float32.fracWidth)).io)
  classify.zip(operands).foreach({case (cls, x) => (cls.in := x)})

  def decode(x: UInt, isSubnormal: Bool, isZero: Bool) = {
    val f32 = Float32(x)
    val exp = Mux(isSubnormal,
      (-Float32.expBiasInt+1).S,  //  f32 -> -126
      f32.exp.toSInt - Float32.expBias.toSInt
    )
    val fracExt = Mux(isZero, 0.U, Cat(!isSubnormal, f32.frac)) //   normal: extend: 1.{23bits} width = 24
    (f32.sign, exp, fracExt)
  }

  val signs   = Array.fill(3)(Wire(Bool()))
  val exps    = Array.fill(3)(Wire(SInt(F_EXP_WIDTH.W)))
  val fracs   = Array.fill(3)(Wire(UInt(F_FRAC_WIDTH.W)))
  for(i <- 0 until 3){
    val (s, e, f) = decode(operands(i), classify(i).isSubnormal, classify(i).isZero)
    signs(i)    := s
    exps(i)     := e    // width = 10
    fracs(i)    := f    // width = 24
  }

  val prodHasSubnormal= classify(1).isSubnormal || classify(2).isSubnormal
  val prodIsZero      = classify(1).isZero || classify(2).isZero
  val prodSign        = signs(1) ^ signs(2) ^ (op(2,1)==="b11".U)
  val prodExpRaw      = Mux(prodIsZero,
    (-Float32.expBiasInt).S,
    exps(1) + exps(2)
  )

  val zeroResultSign = Mux(op(2,1) === "b01".U,
    prodSign,
    (signs(0) & prodSign) | ((signs(0) | prodSign) & rm === RDN)
  )

  val hasNaN  = classify.map(_.isNaN).reduce(_||_)
  val hasSNaN = classify.map(_.isSNaN).reduce(_||_)

  val isInf       = classify.map(_.isInf)
  val hasInf      = isInf.reduce(_||_)
  val prodHasInf  = isInf(1) || isInf(2)

  val addInfInvalid   = (isInf(0) & prodHasInf & (signs(0) ^ prodSign)) & !(isInf(0) ^ prodHasInf)
  val zeroMulInf      = prodIsZero & prodHasInf       //  invalid:    0 * inf
  val infInvalid      = addInfInvalid || zeroMulInf
  val invalid         = hasSNaN || infInvalid
  val specialCase     = hasNaN || hasInf
  val specialOutput   = PriorityMux(Seq(
    (hasNaN || infInvalid) -> Float32.defaultNaN,
    isInf(0) -> Cat(signs(0), Float32.posInf.tail(1)),
    prodHasInf -> Cat(prodSign, Float32.posInf.tail(1))
  ))
  val prodExpAdj = prodExpRaw + INITIAL_EXP_DIFF.S
  val expDiff = prodExpAdj - exps(0)

  val mult = Module(new ArrayMulDataModule(F_FRAC_WIDTH+1))  // width = 24 + 1
  mult.io.a := fracs(1)   //25 := 24
  mult.io.b := fracs(2)   //25 := 24

  mult.io.regEnables := VecInit((1 to 2) map (i => regEnable(i)))

  val s1_rm = S1Reg(rm)
  val s1_zeroSign = S1Reg(zeroResultSign)
  val s1_specialCase = S1Reg(specialCase)
  val s1_specialOutput = S1Reg(specialOutput)
  val s1_aSign = S1Reg(signs(0))
  val s1_aExpRaw = S1Reg(exps(0))
  val s1_aFrac = S1Reg(fracs(0))
  val s1_prodSign = S1Reg(prodSign)
  val s1_prodExpAdj = S1Reg(prodExpAdj)
  val s1_expDiff = S1Reg(expDiff)
  val s1_discardProdFrac = S1Reg(prodIsZero || expDiff.head(1).asBool())
  val s1_discardAFrac = S1Reg(classify(0).isZero || expDiff > (ADD_WIDTH+3).S)
  val s1_invalid = S1Reg(invalid)
  //val s1_mult_carry = S1Reg(mult.io.carry)
  //val s1_mult_sum = S1Reg(mult.io.sum)
  /* STAGE2 ***********************************************************************************************/
  val alignedAFrac = Wire(UInt((ADD_WIDTH+4).W))
  alignedAFrac := Cat(
    0.U(1.W),
    ShiftRightJam(s1_aFrac.asUInt, Mux(s1_discardProdFrac, 0.U, s1_expDiff.asUInt), ADD_WIDTH+3)
  )
  val alignedAFracNeg = -alignedAFrac
  val effSub = s1_prodSign ^ s1_aSign
  val mul_prod = mult.io.result//s1_mult_carry.tail(1) + s1_mult_sum.tail(1)

  val expPreNorm = Mux(s1_discardAFrac || !s1_discardProdFrac, prodExpAdj, exps(0))
  val s2_rm = S2Reg(s1_rm)
  val s2_zeroSign = S2Reg(s1_zeroSign)
  val s2_specialCase = S2Reg(s1_specialCase)
  val s2_specialOutput = S2Reg(s1_specialOutput)
  val s2_aSign = S2Reg(s1_aSign)
  val s2_aExpRaw = S2Reg(s1_aExpRaw)
  val s2_prodSign = S2Reg(s1_prodSign)
  val s2_expPreNorm = S2Reg(Mux(s1_discardAFrac || !s1_discardProdFrac, s1_prodExpAdj, s1_aExpRaw))
  val s2_invalid = S2Reg(invalid)

  val s2_prod = S2Reg(mul_prod)
  val s2_aFracNeg = S2Reg(alignedAFracNeg)
  val s2_aFrac = S2Reg(alignedAFrac)
  val s2_effSub = S2Reg(effSub)
  /*STAGE 3 *****************************************************************************************/
  val prodMinusA = Cat(s2_prod, 0.U(3.W)) + s2_aFracNeg
  val prodMinusA_Sign = prodMinusA.head(1).asBool()
  val aMinusProd = -prodMinusA
  val prodAddA = Cat(s2_prod, 0.U(3.W)) + s2_aFrac

  val res = Mux(s2_effSub,
    Mux(prodMinusA_Sign,
      aMinusProd,
      prodMinusA
    ),
    prodAddA
  )
  val resSign = Mux(s2_prodSign,
    Mux(s2_aSign,
      true.B,             //  -(b * c) - a
      !prodMinusA_Sign    //  -(b * c) + a
    ),
    Mux(s2_aSign,
      prodMinusA_Sign,    //  (b * c) - a
      false.B             //  (b * c) + a
    )
  )
  val fracPreNorm = res.tail(1)
  // TO BE TESTED: effSub = 1 (prodMinusA) :
  val normShift = PriorityEncoder(res.tail(1).asBools().reverse)
  val roundingInc = MuxLookup(s2_rm, "b10".U(2.W), Seq(
    RDN -> Mux(resSign, "b11".U, "b00".U),
    RUP -> Mux(resSign, "b00".U, "b11".U),
    RTZ -> "b00".U
  ))
  val ovSetInf = s2_rm === RNE ||
    s2_rm === RMM ||
    (s2_rm === RDN && resSign) ||
    (s2_rm === RUP && !resSign)

  val s3_ovSetInf = S3Reg(ovSetInf)
  val s3_roundingInc = S3Reg(roundingInc)
  val s3_rm = S3Reg(s2_rm)
  val s3_zeroSign = S3Reg(s2_zeroSign)
  val s3_specialCase = S3Reg(s2_specialCase)
  val s3_specialOutput = S3Reg(s2_specialOutput)
  val s3_resSign = S3Reg(resSign)
  val s3_fracPreNorm = S3Reg(fracPreNorm)
  val s3_expPreNorm = S3Reg(s2_expPreNorm)
  val s3_normShift = S3Reg(normShift)
  val s3_invalid = S3Reg(s2_invalid)
  /*STAGE 4 *******************************************************************************************/
  val expPostNorm = s3_expPreNorm - s3_normShift.toSInt
  val denormShift = (-Float32.expBiasInt+1).S - expPostNorm

  val leftShift = s3_normShift.toSInt - Mux(denormShift.head(1).asBool(), 0.S, denormShift)
  val rightShift = denormShift - s3_normShift.toSInt
  val fracShifted = Mux(rightShift.head(1).asBool(),  // < 0
    ShiftLeftJam(s3_fracPreNorm, leftShift.asUInt(), F_FRAC_WIDTH+3),
    ShiftRightJam(s3_fracPreNorm, rightShift.asUInt(), F_FRAC_WIDTH+3)
  )

  val s4_rm = S4Reg(s3_rm)
  val s4_roundingInc = S4Reg(s3_roundingInc)
  val s4_zeroSign = S4Reg(s3_zeroSign)
  val s4_specialCase = S4Reg(s3_specialCase)
  val s4_specialOutput = S4Reg(s3_specialOutput)
  val s4_ovSetInf = S4Reg(s3_ovSetInf)
  val s4_resSign = S4Reg(s3_resSign)
  val s4_fracShifted = S4Reg(fracShifted)
  val s4_expPostNorm = S4Reg(expPostNorm)
  val s4_invalid = S4Reg(s3_invalid)

  val s4_denormShift = S4Reg(denormShift)
  /*STAGE 5 *********************************************************************************************/
  val fracUnrounded = s4_fracShifted.head(F_FRAC_WIDTH)
  val g = s4_fracShifted.tail(F_FRAC_WIDTH).head(1).asBool()
  val r = s4_fracShifted.tail(F_FRAC_WIDTH+1).head(1).asBool()
  val s = s4_fracShifted.tail(F_FRAC_WIDTH+2).orR()
  val rounding = Module(new RoundingUnit(F_FRAC_WIDTH))
  rounding.io.in.rm := s4_rm
  rounding.io.in.frac := fracUnrounded
  rounding.io.in.sign := s4_resSign
  rounding.io.in.guard := g
  rounding.io.in.round := r
  rounding.io.in.sticky := s
  val fracRounded = rounding.io.out.fracRounded
  val common_inexact = rounding.io.out.inexact
  val fracCout = Mux(!fracUnrounded(F_FRAC_WIDTH-1),
    fracRounded(F_FRAC_WIDTH-1),
    rounding.io.out.fracCout
  )
  //rounding.io.out.roundUp
  val isZeroResult = !(Cat(fracCout, fracRounded).orR())
  val expRounded = Mux(s4_denormShift > 0.S || isZeroResult,
    0.S,
    s4_expPostNorm + Float32.expBias.toSInt
  ) + fracCout.toSInt
  val isDenormalFrac = (Cat(fracUnrounded, g, r, s) + s4_roundingInc) < Cat(1.U(1.W), 0.U((F_FRAC_WIDTH+2).W))
  val common_underflow = (
    s4_denormShift > 1.S ||
      s4_denormShift===1.S && isDenormalFrac ||
      isZeroResult
    ) && common_inexact
  val common_overflow = expOverflow(expRounded, F_FRAC_WIDTH)
  val overflow = !specialCase && common_overflow
  val underflow = !specialCase && common_underflow
  val inexact = !specialCase && (common_inexact || common_overflow || common_underflow)

  val s5_sign = S5Reg(Mux(isZeroResult, s4_zeroSign, s4_resSign))
  val s5_exp = S5Reg(expRounded)
  val s5_frac = S5Reg(fracRounded)
  val s5_specialCase = S5Reg(s4_specialCase)
  val s5_specialOutput = S5Reg(s4_specialOutput)
  val s5_invalid = S5Reg(s4_invalid)
  val s5_overflow = S5Reg(overflow)
  val s5_underflow = S5Reg(underflow)
  val s5_inexact = S5Reg(inexact)
  val s5_ovSetInf = S5Reg(s4_ovSetInf)
  /****************************************************************************************************/
  val commonResult = Cat(
    s5_sign,
    s5_exp(Float32.expWidth-1,0),
    s5_frac(Float32.fracWidth-1,0)
  )
  val result = Mux(s5_specialCase,
    s5_specialOutput,
    Mux(s5_overflow,
      Cat(s5_sign, Mux(s5_ovSetInf, Float32.posInf, Float32.maxNorm).tail(1)),
      commonResult
    )
  )
  io.out.bits.flags:=Cat(s5_invalid,false.B,s5_overflow,s5_underflow,s5_inexact).asTypeOf(new Fflags)
  io.out.bits.result:=result


}

class FPU_CMP extends FPUSubModule{
  def latency = 0
  def F_EXP_WIDTH:    Int = Float32.expWidth + 2      //  value: 8 + 2 = 10
  def F_FRAC_WIDTH:   Int = Float32.fracWidth + 1     //  value: 23 + 1 = 24
  val src = Seq(io.in.bits.a, io.in.bits.b)
  val op = io.in.bits.op
  val sign = src.map(_(31))
  val subRes = src(0).toSInt - src(1).toSInt      // a - b
  val classify = Array.fill(2)(Module(new Classify(Float32.expWidth, Float32.fracWidth)).io)
  classify.zip(src).foreach({case (c, s) => c.in :=s})

  val srcIsNaN = classify.map(_.isNaN)
  val srcIsSNaN = classify.map(_.isSNaN)

  val hasNaN = srcIsNaN(0) || srcIsNaN(1)
  val bothNaN = srcIsNaN(0) && srcIsNaN(1)
  val bothZero = src(0).tail(1)===0.U && src(1).tail(1)===0.U
  val uintEq = subRes===0.S
  val uintLess = sign(0) ^ (subRes < 0.S)         // a < b ?

  val invalid = Mux(op(2) || !op(1), srcIsNaN(0)||srcIsNaN(1), hasNaN)

  val le, lt, eq = Wire(Bool())
  eq := uintEq || bothZero
  le := Mux(sign(0)=/=sign(1),
    sign(0) || bothZero,
    uintEq || uintLess
  )
  lt := Mux(sign(0)=/=sign(1),
    sign(0) && !bothZero,
    !uintEq && uintLess
  )
  val fcmpResult = Mux(hasNaN,
    0.U(32.W),
    Cat(0.U(31.W), Mux(op(2)&op(0),!eq,Mux(op(2), eq, Mux(op(0), lt, le))))
  )
  val min = Mux(bothNaN,
    Float32.defaultNaN,
    Mux( (lt || (eq && sign(0))) && !srcIsNaN(0),
      io.in.bits.a,
      io.in.bits.b
    )
  )
  val max = Mux(bothNaN,
    Float32.defaultNaN,
    Mux( !(lt || (eq && sign(0))) && !srcIsNaN(0),
      io.in.bits.a,
      io.in.bits.b
    )
  )
  val result=Module(new Queue(new FPUoutput,1))
  result.io.enq.bits.result := Mux(op===0.U, min, Mux(op===1.U, max, fcmpResult))
  result.io.enq.bits.flags.invalid := invalid
  result.io.enq.bits.flags.inexact := false.B
  result.io.enq.bits.flags.overflow := false.B
  result.io.enq.bits.flags.underflow := false.B
  result.io.enq.bits.flags.infinite := false.B
  result.io.enq.valid:=io.in.valid
  io.in.ready:=result.io.enq.ready
  // combinational logic:
  io.out.valid := result.io.deq.valid
  io.out.bits:=result.io.deq.bits
  result.io.deq.ready:=io.out.ready
}

class FPU_F2I extends FPUSubModule{
  def latency = 0
  def F_EXP_WIDTH:    Int = Float32.expWidth + 2      //  value: 8 + 2 = 10
  def F_FRAC_WIDTH:   Int = Float32.fracWidth + 1     //  value: 23 + 1 = 24
  val op = io.in.bits.op
  val rm = io.in.bits.rm
  val f32 = Float32(io.in.bits.a)
  val cls = Module(new Classify(Float32.expWidth, Float32.fracWidth))
  cls.io.in := io.in.bits.a
  val isNaN = cls.io.isNaN

  val sign = f32.sign
  val exp = Wire(SInt(F_EXP_WIDTH.W))
  exp := f32.exp.toSInt
  val frac = f32.fracExt

  val leftShiftAmt = exp - (Float32.expBiasInt + Float32.fracWidth).S
  val rightShiftAmt = -leftShiftAmt.asUInt()
  val needRightShift = leftShiftAmt.head(1).asBool() // exp - 23 < 0
  //val expOv = leftShiftAmt > Mux(op(1), 11.S, (-21).S)
  // ^ for 64bit-extended float
  val expOv = leftShiftAmt > 8.S

  val uintUnrounded = Wire(UInt((32+3).W))
  uintUnrounded := Mux(needRightShift,
    ShiftRightJam(Cat(frac, 0.U(3.W)), rightShiftAmt, Float32.fracWidth+4),
    Cat((frac << leftShiftAmt(3,0))(31,0), 0.U(3.W))
  )

  val rounding = Module(new RoundingUnit(32))
  rounding.io.in.rm := rm
  rounding.io.in.sign := sign
  rounding.io.in.frac := uintUnrounded.head(32)
  rounding.io.in.guard := uintUnrounded.tail(32).head(1)
  rounding.io.in.round := uintUnrounded.tail(33).head(1)
  rounding.io.in.sticky := uintUnrounded.tail(34).head(1)

  val uint = rounding.io.out.fracRounded
  val commonResult = Mux(sign, -uint, uint)

  val diffSign = (uint.orR()) && Mux(op(0),
    sign,
    commonResult(31) ^ sign
  )
  val max32 = Cat(op(0), Fill(31, 1.U(1.W)))
  val min32 = Cat(!op(0), 0.U(31.W))

  val specialResult = Mux(isNaN || !sign, max32, min32)
  val invalid = isNaN || expOv || diffSign

  val result_reg=Module(new Queue(new FPUoutput,1))
  result_reg.io.enq.bits.result := Mux(invalid, specialResult, commonResult)
  result_reg.io.enq.bits.flags.invalid := invalid
  result_reg.io.enq.bits.flags.inexact := (!invalid && rounding.io.out.inexact)
  result_reg.io.enq.bits.flags.overflow := false.B
  result_reg.io.enq.bits.flags.underflow := false.B
  result_reg.io.enq.bits.flags.infinite := false.B
  result_reg.io.enq.valid:=io.in.valid
  io.in.ready:=result_reg.io.enq.ready
  // combinational logic:
  io.out.valid := result_reg.io.deq.valid
  io.out.bits:=result_reg.io.deq.bits
  result_reg.io.deq.ready:=io.out.ready
}

class FPU_I2F extends FPUSubModule{
  def latency = 0
  val op = io.in.bits.op
  val a = io.in.bits.a
  val aNeg = (~a).asUInt()
  val aComp = aNeg + 1.U
  val aSign = Mux(op(0), false.B, a(31))

  val leadingZerosComp = PriorityEncoder(aComp.asBools().reverse)
  val leadingZerosNeg = PriorityEncoder(aNeg.asBools().reverse)
  val leadingZerosPos = PriorityEncoder(a.asBools().reverse)

  val aVal = Mux(aSign, aComp, a)
  val leadingZeros = Mux(aSign, leadingZerosNeg, leadingZerosPos)

  val expUnrounded = (32 - 1 + Float32.expBiasInt).U - leadingZeros
  val leadingZeroHasError = aSign && (leadingZerosComp=/=leadingZerosNeg)
  val aShifted = (aVal<<leadingZeros)(31,0)

  val aShiftedFix = Mux(leadingZeroHasError, aShifted(31,1), aShifted(30,0))
  val fracF = aShiftedFix(30, 30-22)
  val g = aShiftedFix(30-23)
  val r = aShiftedFix(30-24)
  val s = aShiftedFix(30-25,0).orR()

  val roundingUnit = Module(new RoundingUnit(Float32.fracWidth))
  roundingUnit.io.in.rm := io.in.bits.rm
  roundingUnit.io.in.frac := fracF
  roundingUnit.io.in.sign := aSign
  roundingUnit.io.in.guard := g
  roundingUnit.io.in.round := r
  roundingUnit.io.in.sticky := s

  val fracRounded = roundingUnit.io.out.fracRounded
  val expRounded = expUnrounded + roundingUnit.io.out.fracCout + leadingZeroHasError

  val resF = Cat(
    aSign,
    expRounded(Float32.expWidth-1, 0),
    fracRounded(Float32.fracWidth-1, 0)
  )

  val result_reg=Module(new Queue(new FPUoutput,1))
  result_reg.io.enq.bits.result := Mux(a===0.U, 0.U, resF)
  result_reg.io.enq.bits.flags.invalid := false.B
  result_reg.io.enq.bits.flags.inexact := roundingUnit.io.out.inexact
  result_reg.io.enq.bits.flags.overflow := false.B
  result_reg.io.enq.bits.flags.underflow := false.B
  result_reg.io.enq.bits.flags.infinite := false.B
  result_reg.io.enq.valid:=io.in.valid
  io.in.ready:=result_reg.io.enq.ready
  // combinational logic:
  io.out.valid := result_reg.io.deq.valid
  io.out.bits:=result_reg.io.deq.bits
  result_reg.io.deq.ready:=io.out.ready

}

class FPU_FMV extends FPUSubModule{
  def latency = 0
  val op = io.in.bits.op
  val sgnjSign = Mux(op(1),
    io.in.bits.b(31),
    Mux(op(0), !(io.in.bits.b(31)), io.in.bits.a(31) ^ io.in.bits.b(31))
  )
  val resSign = Mux(op(2), sgnjSign, io.in.bits.a(31))
  val cls = Module(new Classify(Float32.expWidth, Float32.fracWidth)).io
  cls.in := io.in.bits.a
  val classifyResult = Cat(
    cls.isQNaN,
    cls.isSNaN,
    cls.isPosInf,
    cls.isPosNormal,
    cls.isPosSubnormal,
    cls.isPosZero,
    cls.isNegZero,
    cls.isNegSubnormal,
    cls.isNegNormal,
    cls.isNegInf
  )
  val result = Mux(op==="b010".U,
    classifyResult,
    Cat(resSign, io.in.bits.a(30,0))
  )

  val result_reg=Module(new Queue(new FPUoutput,1))
  result_reg.io.enq.bits.result := result
  result_reg.io.enq.bits.flags:=0.U.asTypeOf(new Fflags)
  result_reg.io.enq.valid:=io.in.valid
  io.in.ready:=result_reg.io.enq.ready
  // combinational logic:
  io.out.valid := result_reg.io.deq.valid
  io.out.bits:=result_reg.io.deq.bits
  result_reg.io.deq.ready:=io.out.ready
}

class ScalarFPU extends Module{
  val io = IO(new Bundle{
    val in = Flipped(DecoupledIO(new Bundle{
      val fpuop               = Input(UInt(6.W))
      val a, b, c             = Input(UInt(32.W))
      val rm                  = Input(UInt(3.W))
    }))
    val out = DecoupledIO(new Bundle{
      val result              = Output(UInt(32.W))
      val flags               = Output(new Fflags)
    })
    val select = Output(UInt(3.W))
  })
  val fu = io.in.bits.fpuop(5,3)
  val op = io.in.bits.fpuop(2,0)
  val subModules = Array[FPUSubModule](
    Module(new FPU_FMA),
    Module(new FPU_CMP),
    Module(new FPU_FMV),
    Module(new FPU_F2I),
    Module(new FPU_I2F)
  )
  io.in.ready:=false.B
  for((module, idx) <- subModules.zipWithIndex){
    module.io.in.bits.op    := Mux(idx.U===fu, op, 0.U(3.W))
    module.io.in.bits.rm    := Mux(idx.U===fu, io.in.bits.rm, 0.U(3.W))
    module.io.in.bits.a     := Mux(idx.U===fu, io.in.bits.a, 0.U(32.W))
    module.io.in.bits.b     := Mux(idx.U===fu, io.in.bits.b, 0.U(32.W))
    module.io.in.bits.c     := Mux(idx.U===fu, io.in.bits.c, 0.U(32.W))
    module.io.in.valid      := io.in.fire() && idx.U===fu
    when(idx.U===fu){io.in.ready:=module.io.in.ready}
  }
  io.out.bits.result := MuxCase(0.U(32.W),
    subModules.zipWithIndex.map({case (module, idx) =>
      (idx.U === fu) -> module.io.out.bits.result
    })
  )
  io.out.bits.flags := MuxCase(("b00000".U).asTypeOf(new Fflags),
    subModules.zipWithIndex.map({case (module, idx) =>
      (idx.U === fu) -> module.io.out.bits.flags
    })
  )
  val outArbiter = Module(new Arbiter(new Bundle{
    val result              = Output(UInt(32.W))
    val flags               = Output(new Fflags)}, 5))
  for(idx <- 0 until subModules.length){
    outArbiter.io.in(idx) <> subModules(idx).io.out
  }
  io.out <> outArbiter.io.out
  io.select:=outArbiter.io.chosen
  /*printf(p"op:${(io.fpuop)}, " +
    p"a:0x${Hexadecimal(io.a)}, b:0x${Hexadecimal(io.b)}, c:0x${Hexadecimal(io.c)}\n" +
    p"res:0x${Hexadecimal(io.result)}\n")*/
}

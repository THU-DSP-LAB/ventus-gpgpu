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

class FracDivSqrt(len: Int) extends Module{ // len = 28 for FLOAT32
  val io = IO(new Bundle{
    val in = Flipped(DecoupledIO(new Bundle{
      val a, b = UInt(len.W)
      val isDiv = Bool()
    }))
    val out = DecoupledIO(new Bundle{
      val quot = UInt(len.W)
      val isZeroRem = Bool()
    })
  })
  val (a, b) = (io.in.bits.a, io.in.bits.b)
  val isDiv = io.in.bits.isDiv
  val isDivReg = RegEnable(isDiv, io.in.fire)
  val divisor = RegEnable(b, io.in.fire)

  val s_idle :: s_recurrence :: s_recovery :: s_finish :: Nil = Enum(4)
  val state = RegInit(s_idle)
  val cnt_next = Wire(UInt(log2Up((len+1)/2).W))
  val cnt = RegEnable(cnt_next, state===s_idle || state===s_recurrence)
  cnt_next := Mux(state===s_idle, (len/2).U, cnt - 1.U)

  val firstCycle = RegNext(io.in.fire)

  switch(state){
    is(s_idle){
      when(io.in.fire){ state := s_recurrence }
    }
    is(s_recurrence){
      when(cnt_next===0.U){ state := s_recovery }
    }
    is(s_recovery){ state := s_finish }
    is(s_finish){
      when(io.out.fire){ state := s_idle }
    }
  }

  val ws, wc = Reg(UInt((len+4).W))

  val table = Module(new SrtTable)
  val conv = Module(new OnTheFlyConv(len+3))    // len = 28
  val csa = Module(new CSA32(len+4))

  //sqrt
  val S = conv.io.Q >> 2
  val s0 :: s1 :: s2 :: s3 :: s4 :: Nil = S(len-2, len-6).asBools.reverse
  val sqrt_d = Mux(firstCycle, "b101".U(3.W), Mux(s0, "b111".U(3.W), Cat(s2, s3, s4)))
  val div_d = divisor(len-2, len-4)
  val sqrt_y = ws(len+3, len-4) + wc(len+3, len-4)
  val div_y = ws(len+2, len-5) + wc(len+2, len-5)

  table.io.d := Mux(isDivReg, div_d, sqrt_d)
  table.io.y := Mux(isDivReg, div_y, sqrt_y)

  conv.io.resetSqrt := io.in.fire && !isDiv
  conv.io.resetDiv := io.in.fire && isDiv
  conv.io.enable := state===s_recurrence
  conv.io.qi := table.io.q

  val dx1, dx2, neg_dx1, neg_dx2 = Wire(UInt((len+4).W))
  dx1 := divisor
  dx2 := divisor << 1
  neg_dx1 := ~dx1
  neg_dx2 := neg_dx1 << 1

  val divCsaIn = MuxLookup(table.io.q.asUInt, 0.U)(Seq(
    -1 -> dx1,
    -2 -> dx2,
    1 ->  neg_dx1,
    2 ->  neg_dx2
  ).map(m => m._1.S(3.W).asUInt -> m._2))

  csa.io.in(0) := ws
  csa.io.in(1) := Mux(isDivReg & !table.io.q(2), wc|table.io.q(1,0), wc)
  csa.io.in(2) := Mux(isDivReg, divCsaIn, conv.io.F)

  val divWsInit = a
  // DIV:         0 0 0 0 A A . . .
  val sqrtWsInit = Cat( Cat(0.U(2.W), a) - Cat(1.U(2.W), 0.U(len.W)), 0.U(2.W))
  // SQRT:        1 1 A A . . . . .

  when(io.in.fire){
    ws := Mux(isDiv, divWsInit, sqrtWsInit)
    wc := 0.U
  }.elsewhen(state===s_recurrence){
    ws := Mux(cnt_next===0.U, csa.io.out(0), csa.io.out(0)<<2)
    wc := Mux(cnt_next===0.U, csa.io.out(1)<<1, csa.io.out(1)<<3)
  }
  val rem = ws + wc
  val remSignReg = RegEnable(rem.head(1).asBool, state===s_recovery)
  val isZeroRemReg = RegEnable(rem===0.U, state===s_recovery)
  io.in.ready := state === s_idle
  io.out.valid := state === s_finish
  io.out.bits.quot := Mux(remSignReg, conv.io.QM, conv.io.Q) >> !isDivReg
  io.out.bits.isZeroRem := isZeroRemReg
}

class FloatDivSqrt extends FPUSubModule{
  def F_EXP_WIDTH: Int = Float32.expWidth + 2
  def F_FRAC_WIDTH: Int = Float32.fracWidth + 1
  def expOverflow(sexp: SInt, expWidth: Int): Bool = sexp >= Cat(0.U(1.W), Fill(expWidth, 1.U(1.W))).asSInt
  def expOverflow(uexp: UInt, expWidth: Int): Bool = expOverflow(Cat(0.U(1.W), uexp).asSInt, expWidth)

  val s_idle :: s_norm :: s_start :: s_compute :: s_round :: s_finish :: Nil = Enum(6)
  val state = RegInit(s_idle)

  val rm = io.in.bits.rm
  val rmReg = RegEnable(rm, io.in.fire)
  val isDiv = io.in.bits.op===0.U
  val isDivReg = RegEnable(isDiv, io.in.fire)

  val (a, b) = (io.in.bits.a, io.in.bits.b)

  val classify_a = Module(new Classify(Float32.expWidth, Float32.fracWidth))
  classify_a.io.in := a
  val aIsSubnormalOrZero = classify_a.io.isSubnormalOrZero
  val classify_b = Module(new Classify(Float32.expWidth, Float32.fracWidth))
  classify_b.io.in := b
  val bIsSubnormalOrZero = classify_b.io.isSubnormalOrZero

  def decode(x: UInt, expIsZero: Bool): (Bool, SInt, UInt) = {
    val f32 = Float32(x)
    val exp = Cat(0.U(1.W), f32.exp) - Float32.expBias
    val fracExt = Cat(!expIsZero, f32.frac)
    (f32.sign, exp.asSInt, fracExt)
  }
  val (aSign, aExp, aFrac) = decode(a, aIsSubnormalOrZero)
  val (bSign, bExp, bFrac) = decode(b, bIsSubnormalOrZero)

  val resSign = Mux(isDiv, aSign ^ bSign, false.B);
  val resSignReg = RegEnable(resSign, io.in.fire)
  val aExpReg = Reg(SInt(F_EXP_WIDTH.W))
  val aFracReg = Reg(UInt(F_FRAC_WIDTH.W))
  val aIsOddExp = aExpReg(0)
  val bExpReg = Reg(SInt(F_EXP_WIDTH.W))
  val bFracReg = Reg(UInt(F_FRAC_WIDTH.W))

  val aIsNaN = classify_a.io.isNaN
  val aIsSNaN = classify_a.io.isSNaN
  val aIsInf = classify_a.io.isInf
  val aIsPosInf = classify_a.io.isPosInf
  val aIsInfOrNaN = classify_a.io.isInfOrNaN
  val aIsSubnormal = classify_a.io.isSubnormal
  val aIsSubnormalReg = RegEnable(aIsSubnormal, io.in.fire)
  val aIsZero = classify_a.io.isZero

  val sel_NaN_OH = UIntToOH(2.U, 3)
  val sel_Zero_OH = UIntToOH(1.U, 3)
  val sel_Inf_OH = UIntToOH(0.U, 3)

  val bIsZero = classify_b.io.isZero
  val bIsNaN = classify_b.io.isNaN
  val bIsSNaN = classify_b.io.isSNaN
  val bIsSubnormal = classify_b.io.isSubnormal
  val bIsSubnormalReg = RegEnable(bIsSubnormal, io.in.fire)
  val bIsInf = classify_b.io.isInf

  val hasNaN = aIsNaN || bIsNaN
  val bothZero = aIsZero && bIsZero
  val bothInf = aIsInf && bIsInf

  val sqrtInvalid = ((aSign && !aIsNaN && !aIsZero) || aIsSNaN) && !isDiv
  val sqrtSpecial = (aSign || aIsInfOrNaN || aIsZero) && !isDiv
  val sqrtInvalidReg = RegEnable(sqrtInvalid, io.in.fire)
  val divInvalid = (bothZero || aIsSNaN || bIsSNaN || bothInf) && isDiv
  val divInf = (!divInvalid && !aIsNaN && bIsZero && !aIsInf) && isDiv
  val divSpecial = (aIsZero || bIsZero || hasNaN || bIsInf || aIsInf) && isDiv
  val divZeroReg = RegEnable(bIsZero, io.in.fire)
  val divInvalidReg = RegEnable(divInvalid, io.in.fire)
  val divInfReg = RegEnable(divInf, io.in.fire)

  val divSpecialResSel = PriorityMux(Seq(
    (divInvalid || hasNaN) -> sel_NaN_OH,
    bIsZero -> sel_Inf_OH,
    (aIsZero || bIsInf) -> sel_Zero_OH,
    aIsInf -> sel_Inf_OH
  ))
  val sqrtSpecialResSel = MuxCase(sel_NaN_OH, Seq(
    aIsZero -> sel_Zero_OH,
    aIsPosInf -> sel_Inf_OH
  ))
  val specialCase = divSpecial || sqrtSpecial
  val specialCaseReg = RegEnable(specialCase, io.in.fire)
  val specialResSel = Mux(sqrtSpecial, sqrtSpecialResSel, divSpecialResSel)
  val sel_NaN :: sel_Zero :: sel_Inf :: Nil = specialResSel.asBools.reverse
  val specialResult = RegEnable(
    Mux(sel_NaN,
      Float32.defaultNaN,
      Mux(sel_Zero,
        Cat(resSign, 0.U((Float32.getWidth-1).W)),
        Cat(resSign, Float32.posInf.tail(1))
      )
    ),
    io.in.fire
  )

  val aFracLEZ = PriorityEncoder(aFracReg(22, 0).asBools.reverse)
  val bFracLEZ = PriorityEncoder(bFracReg(22, 0).asBools.reverse)

  val fracDivSqrt = Module(new FracDivSqrt(F_FRAC_WIDTH+4))
  fracDivSqrt.io.out.ready := true.B
  fracDivSqrt.io.in.valid := state === s_start
  //                27 26 25 24 23
  // DIVA IN: 1.5 => 1. 1  0  0  0  .  .  .  .
  fracDivSqrt.io.in.bits.a := Mux(isDivReg || aIsOddExp, Cat(aFracReg, 0.U(4.W)), Cat(0.U(1.W), aFracReg, 0.U(3.W)))
  //                27 26 25 24 23
  // SQRT IN: 1.0 => 0  1. 0  0  0  .  .  .  .
  // SQRT IN: 2.5 => 1  0. 1  0  0  .  .  .  .
  fracDivSqrt.io.in.bits.b := Cat(bFracReg, 0.U(4.W))
  fracDivSqrt.io.in.bits.isDiv := isDivReg

  val fracDivSqrtResult = fracDivSqrt.io.out.bits.quot
  //                27 26 25 24 23 ...         03|02 01 00
  // DIV OUT: 3/4 => 0  0. 1  1  0  .  .  .  .  x| x  x  x
  val needNormalize = !fracDivSqrtResult(26)
  val fracNorm = Mux(needNormalize, fracDivSqrtResult<<1, fracDivSqrtResult)(26, 0)
  val expNorm = ( aExpReg.asUInt - Mux(isDivReg, Mux(needNormalize, 1.U, 0.U), Mux(needNormalize, 2.U, 1.U)) ).asSInt

  val denormShift = (-Float32.expBiasInt+1).S - expNorm
  val denormShiftReg = RegEnable(denormShift, fracDivSqrt.io.out.fire)
  val fracShifted = ShiftRightJam(fracNorm, Mux(denormShift.head(1).asBool, 0.U, denormShift.asUInt), F_FRAC_WIDTH+3)

  val fracPostNorm = fracShifted.head(F_FRAC_WIDTH)
  val g = fracShifted.tail(F_FRAC_WIDTH).head(1).asBool
  val r = fracShifted.tail(F_FRAC_WIDTH+1).head(1).asBool
  val s = !fracDivSqrt.io.out.bits.isZeroRem || fracShifted.tail(F_FRAC_WIDTH+2).orR
  val gReg = RegNext(g)
  val rReg = RegNext(r)
  val sReg = RegNext(s)

  import pipeline.RoundingMode._
  val rounding = Module(new RoundingUnit(F_FRAC_WIDTH))
  rounding.io.in.rm := rmReg
  rounding.io.in.frac := aFracReg
  rounding.io.in.sign := resSignReg
  rounding.io.in.guard := gReg
  rounding.io.in.round := rReg
  rounding.io.in.sticky := sReg

  val fracRounded = rounding.io.out.fracRounded
  val common_inexact = rounding.io.out.inexact
  val fracCout = Mux(!aFracReg(F_FRAC_WIDTH-1),
    fracRounded(F_FRAC_WIDTH-1),
    rounding.io.out.fracCout
  )
  val isZeroResult = !(Cat(fracCout, fracRounded).orR)
  val expRounded = Mux(denormShift > 0.S || isZeroResult,
    0.S,
    aExpReg + Float32.expBias.toSInt
  ) + fracCout.toSInt
  val roundingInc = MuxLookup(rmReg, "b10".U(2.W))(Seq(
    RDN -> Mux(resSign, "b11".U, "b00".U),
    RUP -> Mux(resSign, "b00".U, "b11".U),
    RTZ -> "b00".U
  ))
  val isDenormalFrac = (Cat(aFracReg, g, r, s) + roundingInc) < Cat(1.U(1.W), 0.U((F_FRAC_WIDTH+2).W))
  val common_underflow = (
    denormShift > 1.S ||
      denormShift===1.S && isDenormalFrac ||
      isZeroResult
    ) && common_inexact
  val common_overflow = expOverflow(expRounded, F_FRAC_WIDTH)
  val overflowReg = RegEnable(!specialCase && common_overflow, state===s_round)
  val underflowReg = RegEnable(!specialCase && common_underflow, state===s_round)
  val inexactReg = RegEnable(!specialCase && (common_inexact || common_overflow || common_underflow), state===s_round)
  val ovSetInf = rmReg === RNE || rmReg === RMM ||
    (rmReg === RDN && resSignReg) || (rmReg === RUP && !resSignReg)
  val ovSetInfReg = RegEnable(ovSetInf, state===s_round)

  switch(state){
    is(s_idle){
      when(io.in.fire){
        when(sqrtSpecial || divSpecial){
          state := s_finish
        }.elsewhen(aIsSubnormal || bIsSubnormal){
          state := s_norm
        }.otherwise{
          state:= s_start
        }
      }
    }
    is(s_norm){ state := s_start }
    is(s_start){ state := s_compute }
    is(s_compute){
      when(fracDivSqrt.io.out.fire){ state := s_round }
    }
    is(s_round){ state := s_finish }
    is(s_finish){ when(io.out.fire) {state := s_idle }}
  }

  switch(state){
    is(s_idle){
      when(io.in.fire){
        aExpReg := aExp
        aFracReg := aFrac
        bExpReg := bExp
        bFracReg := bFrac
      }
    }
    is(s_norm){
      when(aIsSubnormalReg){
        aExpReg := (aExpReg.asUInt - aFracLEZ).asSInt
        aFracReg := (aFracReg << aFracLEZ) << 1
      }
      when(bIsSubnormalReg){
        bExpReg := (bExpReg.asUInt - bFracLEZ).asSInt
        bFracReg := (bFracReg << bFracLEZ) << 1
      }
    }
    is(s_start){
      aExpReg := Mux(isDivReg, aExpReg - bExpReg, (aExpReg>>1).asSInt+1.S)
    }
    is(s_compute){
      when(fracDivSqrt.io.out.fire){
        aExpReg := expNorm
        aFracReg := fracPostNorm
      }
    }
    is(s_round){
      aExpReg := expRounded
      aFracReg := fracRounded
    }
  }

  val commonResult = Cat(resSignReg, aExpReg(Float32.expWidth-1, 0), aFracReg(Float32.fracWidth-1, 0))
  io.in.ready := (state===s_idle) //&& io.out.ready
  io.out.valid := state === s_finish
  io.out.bits.result := Mux(specialCaseReg,
    specialResult,
    Mux(overflowReg,
      Cat(resSignReg, Mux(ovSetInfReg, Float32.posInf.tail(1), Float32.maxNorm.tail(1))),
      commonResult
    )
  )
  io.out.bits.flags.invalid := Mux(isDivReg, divInvalidReg, sqrtInvalidReg)
  io.out.bits.flags.underflow := !specialCaseReg && underflowReg
  io.out.bits.flags.overflow := !specialCaseReg && overflowReg
  io.out.bits.flags.infinite := Mux(isDivReg, divInfReg, false.B)
  io.out.bits.flags.inexact := !specialCaseReg && (inexactReg || overflowReg || underflowReg)
}

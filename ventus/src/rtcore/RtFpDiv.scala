package rtcore

import chisel3._
import chisel3.util._

/** Division-only radix-4 SRT datapath for one IEEE-754 binary32 quotient. */
private class RtFpDivSrtTable extends Module {
  val io = IO(new Bundle {
    val d = Input(UInt(3.W))
    val y = Input(UInt(8.W))
    val q = Output(SInt(3.W))
  })

  private val selection = Array(
    Array(24, 8, -8, -26), Array(28, 8, -10, -28),
    Array(32, 8, -12, -32), Array(32, 8, -12, -34),
    Array(36, 12, -12, -36), Array(40, 12, -16, -40),
    Array(40, 16, -16, -44), Array(48, 16, -16, -46)
  )
  private var ge = Map.empty[Int, Bool]
  for (row <- selection; threshold <- row) {
    if (!ge.contains(threshold)) {
      ge += threshold -> (io.y.asSInt >= threshold.S(8.W))
    }
  }
  io.q := MuxLookup(io.d, 0.S)(selection.zipWithIndex.map { case (row, index) =>
    index.U -> MuxCase((-2).S(3.W), Seq(
      ge(row(0)) -> 2.S(3.W), ge(row(1)) -> 1.S(3.W),
      ge(row(2)) -> 0.S(3.W), ge(row(3)) -> (-1).S(3.W)
    ))
  })
}

private class RtFpDivOnTheFly(len: Int) extends Module {
  val io = IO(new Bundle {
    val reset = Input(Bool())
    val enable = Input(Bool())
    val qi = Input(SInt(3.W))
    val qm = Output(UInt(len.W))
    val q = Output(UInt(len.W))
  })

  val q = Reg(UInt(len.W))
  val qm = Reg(UInt(len.W))
  val b01 = Reg(UInt((len - 3).W))
  val b10 = Reg(UInt((len - 3).W))
  val b11 = Reg(UInt((len - 3).W))
  when(io.reset) {
    b01 := Cat("b01".U(2.W), 0.U((len - 5).W))
    b10 := Cat("b10".U(2.W), 0.U((len - 5).W))
    b11 := Cat("b11".U(2.W), 0.U((len - 5).W))
  }.elsewhen(io.enable) {
    b01 := b01 >> 2
    b10 := b10 >> 2
    b11 := b11 >> 2
  }

  when(io.reset) {
    q := 0.U
    qm := 0.U
  }.elsewhen(io.enable) {
    val qNext = MuxLookup(io.qi.asUInt, 0.U)(Seq(
      0.S(3.W).asUInt -> q,
      1.S(3.W).asUInt -> (q | b01),
      2.S(3.W).asUInt -> (q | b10),
      (-1).S(3.W).asUInt -> (qm | b11),
      (-2).S(3.W).asUInt -> (qm | b10)
    ))
    val qmNext = MuxLookup(io.qi.asUInt, 0.U)(Seq(
      1.S(3.W).asUInt -> q,
      2.S(3.W).asUInt -> (q | b01),
      0.S(3.W).asUInt -> (qm | b11),
      (-1).S(3.W).asUInt -> (qm | b10),
      (-2).S(3.W).asUInt -> (qm | b01)
    ))
    q := qNext
    qm := qmNext
  }
  io.q := q
  io.qm := qm
}

private class RtFpDivFrac(len: Int) extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new Bundle {
      val a = UInt(len.W)
      val b = UInt(len.W)
    }))
    val out = Decoupled(new Bundle {
      val quotient = UInt(len.W)
      val zeroRemainder = Bool()
    })
  })

  val divisor = RegEnable(io.in.bits.b, io.in.fire)
  val idle :: recurrence :: recovery :: finish :: Nil = Enum(4)
  val state = RegInit(idle)
  val countNext = Wire(UInt(log2Ceil((len + 1) / 2).W))
  val count = RegEnable(countNext, state === idle || state === recurrence)
  countNext := Mux(state === idle, (len / 2).U, count - 1.U)

  switch(state) {
    is(idle) { when(io.in.fire) { state := recurrence } }
    is(recurrence) { when(countNext === 0.U) { state := recovery } }
    is(recovery) { state := finish }
    is(finish) { when(io.out.fire) { state := idle } }
  }

  val ws = Reg(UInt((len + 4).W))
  val wc = Reg(UInt((len + 4).W))
  val table = Module(new RtFpDivSrtTable)
  val conversion = Module(new RtFpDivOnTheFly(len + 3))
  val dx1 = Wire(UInt((len + 4).W))
  val dx2 = Wire(UInt((len + 4).W))
  val negDx1 = Wire(UInt((len + 4).W))
  val negDx2 = Wire(UInt((len + 4).W))
  dx1 := divisor
  dx2 := divisor << 1
  negDx1 := ~dx1
  negDx2 := negDx1 << 1
  table.io.d := divisor(len - 2, len - 4)
  table.io.y := ws(len + 2, len - 5) + wc(len + 2, len - 5)
  conversion.io.reset := io.in.fire
  conversion.io.enable := state === recurrence
  conversion.io.qi := table.io.q

  val csaIn1 = Mux(!table.io.q(2), wc | table.io.q(1, 0), wc)
  val selected = MuxLookup(table.io.q.asUInt, 0.U)(Seq(
    (-1).S(3.W).asUInt -> dx1,
    (-2).S(3.W).asUInt -> dx2,
    1.S(3.W).asUInt -> negDx1,
    2.S(3.W).asUInt -> negDx2
  ))
  val nextSum = ws ^ csaIn1 ^ selected
  val nextCarry = (ws & csaIn1) | ((ws ^ csaIn1) & selected)
  when(io.in.fire) {
    ws := io.in.bits.a
    wc := 0.U
  }.elsewhen(state === recurrence) {
    ws := Mux(countNext === 0.U, nextSum, nextSum << 2)
    wc := Mux(countNext === 0.U, nextCarry << 1, nextCarry << 3)
  }

  val remainder = ws + wc
  val remainderNegative = RegEnable(remainder.head(1).asBool, state === recovery)
  val zeroRemainder = RegEnable(remainder === 0.U, state === recovery)
  io.in.ready := state === idle
  io.out.valid := state === finish
  io.out.bits.quotient := Mux(remainderNegative, conversion.io.qm, conversion.io.q)
  io.out.bits.zeroRemainder := zeroRemainder
}

/**
  * Fixed-rounding IEEE-754 binary32 divider for RTcore.
  *
  * This is the division-only portion of Ventus's SRT divider: no sqrt opcode,
  * no dynamic rounding mode and no floating-point exception flag interface.
  */
class RtFpDiv extends Module {
  private val expWidth = 8
  private val fracWidth = 23
  private val fracExtWidth = fracWidth + 1
  private val expBias = 127

  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new Bundle {
      val a = UInt(32.W)
      val b = UInt(32.W)
    }))
    val out = Decoupled(UInt(32.W))
  })

  def sign(x: UInt): Bool = x(31)
  def exponent(x: UInt): UInt = x(30, 23)
  def fraction(x: UInt): UInt = x(22, 0)
  def isZero(x: UInt): Bool = exponent(x) === 0.U && fraction(x) === 0.U
  def isSubnormal(x: UInt): Bool = exponent(x) === 0.U && fraction(x).orR
  def isNaN(x: UInt): Bool = exponent(x).andR && fraction(x).orR
  def isSignalingNaN(x: UInt): Bool = isNaN(x) && !fraction(x)(22)
  def isInf(x: UInt): Bool = exponent(x).andR && !fraction(x).orR
  def decode(x: UInt, expIsZero: Bool): (SInt, UInt) = {
    val exp = Cat(0.U(1.W), exponent(x)) - expBias.U
    (exp.asSInt, Cat(!expIsZero, fraction(x)))
  }
  def shiftRightJam(value: UInt, shift: UInt, width: Int): UInt = {
    val extended = Wire(UInt(width.W))
    extended := value
    val realShift = Mux(shift > (width - 1).U, width.U, shift(log2Ceil(width) - 1, 0))
    val mask = ((-1).S(width.W).asUInt >> (width.U - realShift)).asUInt
    (extended >> realShift) | ((mask & extended).orR)
  }

  val idle :: normalize :: start :: compute :: round :: finish :: Nil = Enum(6)
  val state = RegInit(idle)
  val a = io.in.bits.a
  val b = io.in.bits.b
  val aSubnormal = isSubnormal(a)
  val bSubnormal = isSubnormal(b)
  val aZero = isZero(a)
  val bZero = isZero(b)
  val aNaN = isNaN(a)
  val bNaN = isNaN(b)
  val aInf = isInf(a)
  val bInf = isInf(b)
  val invalid = (aZero && bZero) || isSignalingNaN(a) || isSignalingNaN(b) || (aInf && bInf)
  val special = aZero || bZero || aNaN || bNaN || aInf || bInf
  val specialResult = Mux(invalid || aNaN || bNaN, "h7fc00000".U,
    Mux(bZero || aInf, Cat(sign(a) ^ sign(b), "h7f800000".U(31.W)),
      Cat(sign(a) ^ sign(b), 0.U(31.W))))

  val (aExp, aFrac) = decode(a, exponent(a) === 0.U)
  val (bExp, bFrac) = decode(b, exponent(b) === 0.U)
  val resultSign = RegEnable(sign(a) ^ sign(b), io.in.fire)
  val specialResultReg = RegEnable(specialResult, io.in.fire)
  val specialReg = RegEnable(special, io.in.fire)
  val aSubnormalReg = RegEnable(aSubnormal, io.in.fire)
  val bSubnormalReg = RegEnable(bSubnormal, io.in.fire)
  val aExpReg = Reg(SInt((expWidth + 2).W))
  val bExpReg = Reg(SInt((expWidth + 2).W))
  val aFracReg = Reg(UInt(fracExtWidth.W))
  val bFracReg = Reg(UInt(fracExtWidth.W))

  val aLeadingZeros = PriorityEncoder(aFracReg(22, 0).asBools.reverse)
  val bLeadingZeros = PriorityEncoder(bFracReg(22, 0).asBools.reverse)
  private val fraction = Module(new RtFpDivFrac(fracExtWidth + 4))
  fraction.io.out.ready := true.B
  fraction.io.in.valid := state === start
  fraction.io.in.bits.a := Cat(aFracReg, 0.U(4.W))
  fraction.io.in.bits.b := Cat(bFracReg, 0.U(4.W))

  val quotient = fraction.io.out.bits.quotient
  val needNormalize = !quotient(26)
  val normalizedFraction = Mux(needNormalize, quotient << 1, quotient)(26, 0)
  val normalizedExponent = (aExpReg.asUInt - Mux(needNormalize, 1.U, 0.U)).asSInt
  val denormalShift = (-expBias + 1).S - normalizedExponent
  val shiftedFraction = shiftRightJam(normalizedFraction,
    Mux(denormalShift.head(1).asBool, 0.U, denormalShift.asUInt), fracExtWidth + 3)
  val postNormFraction = shiftedFraction.head(fracExtWidth)
  val guard = shiftedFraction.tail(fracExtWidth).head(1).asBool
  val roundBit = shiftedFraction.tail(fracExtWidth + 1).head(1).asBool
  val sticky = !fraction.io.out.bits.zeroRemainder || shiftedFraction.tail(fracExtWidth + 2).orR
  val guardReg = RegNext(guard)
  val roundReg = RegNext(roundBit)
  val stickyReg = RegNext(sticky)
  val roundUp = guardReg && (roundReg || stickyReg || aFracReg(0))
  val roundedWithCarry = aFracReg +& roundUp
  val roundedFraction = roundedWithCarry(fracExtWidth - 1, 0)
  val fractionCarry = Mux(!aFracReg(fracExtWidth - 1),
    roundedFraction(fracExtWidth - 1), roundedWithCarry(fracExtWidth))
  val resultIsZero = !(Cat(fractionCarry, roundedFraction).orR)
  val roundedExponent = Mux(denormalShift > 0.S || resultIsZero, 0.S,
    aExpReg + expBias.S) + fractionCarry.asSInt
  val overflow = roundedExponent >= 255.S((expWidth + 2).W)
  val overflowReg = RegEnable(!specialReg && overflow, state === round)
  val commonResult = Cat(resultSign, aExpReg(expWidth - 1, 0), aFracReg(fracWidth - 1, 0))

  switch(state) {
    is(idle) {
      when(io.in.fire) {
        when(special) { state := finish }
        .elsewhen(aSubnormal || bSubnormal) { state := normalize }
        .otherwise { state := start }
        aExpReg := aExp
        bExpReg := bExp
        aFracReg := aFrac
        bFracReg := bFrac
      }
    }
    is(normalize) {
      when(aSubnormalReg) {
        aExpReg := (aExpReg.asUInt - aLeadingZeros).asSInt
        aFracReg := (aFracReg << aLeadingZeros) << 1
      }
      when(bSubnormalReg) {
        bExpReg := (bExpReg.asUInt - bLeadingZeros).asSInt
        bFracReg := (bFracReg << bLeadingZeros) << 1
      }
      state := start
    }
    is(start) {
      aExpReg := aExpReg - bExpReg
      state := compute
    }
    is(compute) {
      when(fraction.io.out.fire) {
        aExpReg := normalizedExponent
        aFracReg := postNormFraction
        state := round
      }
    }
    is(round) {
      aExpReg := roundedExponent
      aFracReg := roundedFraction
      state := finish
    }
    is(finish) { when(io.out.fire) { state := idle } }
  }

  io.in.ready := state === idle
  io.out.valid := state === finish
  io.out.bits := Mux(specialReg, specialResultReg,
    Mux(overflowReg, Cat(resultSign, "h7f800000".U(31.W)), commonResult))
}

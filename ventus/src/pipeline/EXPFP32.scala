package pipeline 

import chisel3._
import chisel3.util._

import fudian.{FCMA_ADD_s1, FCMA_ADD_s2, FMUL_s1, FMUL_s2, FMUL_s3, FMULToFADD, RawFloat}
import fudian.utils.Multiplier

object EXPFP32Parameters {
  val LOG2E     = "h3FB8AA3B".U(32.W)
  val C0        = "h3F800000".U(32.W)
  val C1        = "h3F317218".U(32.W)
  val C2        = "h3E75FDF0".U(32.W)
  val MIN_INPUT = "hC2AE999A".U(32.W) // -87.3
  val MAX_INPUT = "h42B16666".U(32.W) // +88.7
  val ZERO      = 0.U(32.W)
  val INF       = "h7F800000".U(32.W)
  val NAN       = "h7FC00000".U(32.W)
}

object EXPFP32Utils {
  implicit class DecoupledPipe[T <: Data](val decoupledBundle: DecoupledIO[T]) extends AnyVal {
    def handshakePipeIf(en: Boolean): DecoupledIO[T] = {
      if (en) {
        val out = Wire(Decoupled(chiselTypeOf(decoupledBundle.bits)))
        val rValid = RegInit(false.B)
        val rBits  = Reg(chiselTypeOf(decoupledBundle.bits))

        decoupledBundle.ready  := !rValid || out.ready
        out.valid              := rValid
        out.bits               := rBits

        when(decoupledBundle.fire) {
          rBits  := decoupledBundle.bits
          rValid := true.B
        } .elsewhen(out.fire) {
          rValid := false.B
        }

        out
      } else {
        decoupledBundle
      }
    }
  }
}

import EXPFP32Utils._

// All module are not designed for resue
// I have no choice how to pass ctrl signals and bypass signals

// ======================================
//   MULFP32
//   输入： a, b, rm, ctrl
//   输出： result, toAdd, ctrl
// ======================================
class MULFP32[T <: Bundle](ctrlSignals: T) extends Module {
  val expWidth  = 8
  val precision = 24

  class InBundle extends Bundle {
    val a    = UInt(32.W)
    val b    = UInt(32.W)
    val rm   = UInt(3.W)
    val ctrl = ctrlSignals.cloneType.asInstanceOf[T]
  }
  class OutBundle extends Bundle {
    val result = UInt(32.W)
    val toAdd  = new FMULToFADD(expWidth, precision)
    val ctrl   = ctrlSignals.cloneType.asInstanceOf[T]
  }

  val io = IO(new Bundle {
    val in  = Flipped(Decoupled(new InBundle))
    val out = Decoupled(new OutBundle)
  })

  val mul   = Module(new Multiplier(precision + 1, pipeAt = Seq()))
  val mulS1 = Module(new FMUL_s1(expWidth, precision))
  val mulS2 = Module(new FMUL_s2(expWidth, precision))
  val mulS3 = Module(new FMUL_s3(expWidth, precision))

  mulS1.io.a  := io.in.bits.a
  mulS1.io.b  := io.in.bits.b
  mulS1.io.rm := io.in.bits.rm

  val rawA = RawFloat.fromUInt(io.in.bits.a, expWidth, precision)
  val rawB = RawFloat.fromUInt(io.in.bits.b, expWidth, precision)
  mul.io.a := rawA.sig
  mul.io.b := rawB.sig
  mul.io.regEnables.foreach(_ := true.B)

  val s1 = Wire(Decoupled(new Bundle {
    val mulS1Out = mulS1.io.out.cloneType
    val prod     = mul.io.result.cloneType
    val ctrl     = ctrlSignals.cloneType.asInstanceOf[T]
  }))
  val s1Pipe = s1.handshakePipeIf(true)
  s1.valid         := io.in.valid
  s1.bits.mulS1Out := mulS1.io.out
  s1.bits.prod     := mul.io.result
  s1.bits.ctrl     := io.in.bits.ctrl
  io.in.ready      := s1.ready

  mulS2.io.in   := s1Pipe.bits.mulS1Out
  mulS2.io.prod := s1Pipe.bits.prod

  val s2 = Wire(Decoupled(new Bundle {
    val mulS2Out = mulS2.io.out.cloneType
    val ctrl     = ctrlSignals.cloneType.asInstanceOf[T]
  }))
  val s2Pipe = s2.handshakePipeIf(true)
  s2.valid         := s1Pipe.valid
  s2.bits.mulS2Out := mulS2.io.out
  s2.bits.ctrl     := s1Pipe.bits.ctrl
  s1Pipe.ready     := s2.ready

  mulS3.io.in := s2Pipe.bits.mulS2Out

  val s3     = Wire(Decoupled(new OutBundle))
  val s3Pipe = s3.handshakePipeIf(true)
  s3.valid          := s2Pipe.valid
  s3.bits.result    := mulS3.io.result
  s3.bits.toAdd     := mulS3.io.to_fadd
  s3.bits.ctrl      := s2Pipe.bits.ctrl
  s2Pipe.ready      := s3.ready

  io.out <> s3Pipe
}

// ======================================
//   CMAFP32 = A*B + C
//   输入： a, b, c, rm, ctrl
//   输出： result, ctrl
// ======================================
class CMAFP32[T <: Bundle](ctrlSignals: T) extends Module {
  val expWidth  = 8
  val precision = 24

  class InBundle extends Bundle {
    val a     = UInt(32.W)
    val b     = UInt(32.W)
    val c     = UInt(32.W)
    val rm    = UInt(3.W)
    val ctrl  = ctrlSignals.cloneType.asInstanceOf[T]
  }
  class OutBundle extends Bundle {
    val result = UInt(32.W)
    val ctrl   = ctrlSignals.cloneType.asInstanceOf[T]
  }

  val io = IO(new Bundle {
    val in  = Flipped(Decoupled(new InBundle))
    val out = Decoupled(new OutBundle)
  })

  class MULToADD extends Bundle {
    val c       = UInt(32.W)
    val topCtrl = ctrlSignals.cloneType.asInstanceOf[T]
  }
  val mul   = Module(new MULFP32[MULToADD](new MULToADD))
  val addS1 = Module(new FCMA_ADD_s1(expWidth, precision * 2, precision))
  val addS2 = Module(new FCMA_ADD_s2(expWidth, precision * 2, precision))

  mul.io.in.valid             := io.in.valid
  mul.io.in.bits.a            := io.in.bits.a
  mul.io.in.bits.b            := io.in.bits.b
  mul.io.in.bits.rm           := io.in.bits.rm
  mul.io.in.bits.ctrl.c       := io.in.bits.c
  mul.io.in.bits.ctrl.topCtrl := io.in.bits.ctrl
  io.in.ready                 := mul.io.in.ready

  addS1.io.a             := Cat(mul.io.out.bits.ctrl.c, 0.U(precision.W))
  addS1.io.b             := mul.io.out.bits.toAdd.fp_prod.asUInt
  addS1.io.b_inter_valid := true.B
  addS1.io.b_inter_flags := mul.io.out.bits.toAdd.inter_flags
  addS1.io.rm            := mul.io.out.bits.toAdd.rm

  val s4 = Wire(Decoupled(new Bundle {
    val out  = addS1.io.out.cloneType
    val ctrl = ctrlSignals.cloneType.asInstanceOf[T]
  }))
  val s4Pipe = s4.handshakePipeIf(true)
  s4.valid         := mul.io.out.valid
  s4.bits.out      := addS1.io.out
  s4.bits.ctrl     := mul.io.out.bits.ctrl.topCtrl
  mul.io.out.ready := s4.ready

  addS2.io.in := s4Pipe.bits.out

  val s5     = Wire(Decoupled(new OutBundle))
  val s5Pipe = s5.handshakePipeIf(true)
  s5.valid       := s4Pipe.valid
  s5.bits.result := addS2.io.result
  s5.bits.ctrl   := s4Pipe.bits.ctrl
  s4Pipe.ready   := s5.ready

  io.out <> s5Pipe
}

// ======================================
//   CMAFP32LUTParallel = A*B + C && lut(index)
//   输入： a, b, c, rm, ctrl
//   输出： result, ctrl
// ======================================
class CMAFP32LUTParallel[T <: Bundle](ctrlSignals: T) extends Module {
  val expWidth  = 8
  val precision = 24

  class InBundle extends Bundle {
    val a     = UInt(32.W)
    val b     = UInt(32.W)
    val c     = UInt(32.W)
    val index = UInt(8.W)
    val rm    = UInt(3.W)
    val ctrl  = ctrlSignals.cloneType.asInstanceOf[T]
  }
  class OutBundle extends Bundle {
    val result = UInt(32.W)
    val value  = UInt(32.W)
    val ctrl   = ctrlSignals.cloneType.asInstanceOf[T]
  }

  val io = IO(new Bundle {
    val in  = Flipped(Decoupled(new InBundle))
    val out = Decoupled(new OutBundle)
  })

  class MULToADD extends Bundle {
    val c       = UInt(32.W)
    val index   = UInt(8.W)
    val topCtrl = ctrlSignals.cloneType.asInstanceOf[T]
  }
  val mul   = Module(new MULFP32[MULToADD](new MULToADD))
  val addS1 = Module(new FCMA_ADD_s1(expWidth, precision * 2, precision))
  val addS2 = Module(new FCMA_ADD_s2(expWidth, precision * 2, precision))

  mul.io.in.valid             := io.in.valid
  mul.io.in.bits.a            := io.in.bits.a
  mul.io.in.bits.b            := io.in.bits.b
  mul.io.in.bits.rm           := io.in.bits.rm
  mul.io.in.bits.ctrl.c       := io.in.bits.c
  mul.io.in.bits.ctrl.index   := io.in.bits.index
  mul.io.in.bits.ctrl.topCtrl := io.in.bits.ctrl
  io.in.ready                 := mul.io.in.ready

  addS1.io.a             := Cat(mul.io.out.bits.ctrl.c, 0.U(precision.W))
  addS1.io.b             := mul.io.out.bits.toAdd.fp_prod.asUInt
  addS1.io.b_inter_valid := true.B
  addS1.io.b_inter_flags := mul.io.out.bits.toAdd.inter_flags
  addS1.io.rm            := mul.io.out.bits.toAdd.rm

  val s4 = Wire(Decoupled(new Bundle {
    val out   = addS1.io.out.cloneType
    val index = UInt(8.W)
    val ctrl  = ctrlSignals.cloneType.asInstanceOf[T]
  }))
  val s4Pipe = s4.handshakePipeIf(true)
  s4.valid         := mul.io.out.valid
  s4.bits.out      := addS1.io.out
  s4.bits.index    := mul.io.out.bits.ctrl.index
  s4.bits.ctrl     := mul.io.out.bits.ctrl.topCtrl
  mul.io.out.ready := s4.ready

  addS2.io.in := s4Pipe.bits.out

  val s5     = Wire(Decoupled(new OutBundle))
  val s5Pipe = s5.handshakePipeIf(true)
  s5.valid       := s4Pipe.valid
  s5.bits.result := addS2.io.result
  s5.bits.ctrl   := s4Pipe.bits.ctrl
  s4Pipe.ready   := s5.ready

  // LUT stores 2^(fractional_part) for exp(x) = 2^(x * log2(e))
  val table = VecInit((0 until 256).map { i =>
    val sign = (i >> 7) & 1
    val mag  = i & 0x7f
    val x = if (sign == 1) -(mag.toDouble / 128.0) else (mag.toDouble / 128.0)
    val y = Math.pow(2.0, x)
    val bits = java.lang.Float.floatToIntBits(y.toFloat)
    bits.U(32.W)
  })

  s5.bits.value := table(s4Pipe.bits.index)

  io.out <> s5Pipe
}

class DecomposeFP32[T <: Bundle](ctrlSignals: T) extends Module {
  class InBundle extends Bundle {
    val y    = UInt(32.W)
    val ctrl = ctrlSignals.cloneType.asInstanceOf[T]
  }
  class OutBundle extends Bundle {
    val yi   = UInt(9.W)   // 符号 + 整数部分（8位）
    val yfi  = UInt(8.W)   // 符号 + 小数高7位
    val yfj  = UInt(32.W)  // 小数低位转FP32
    val ctrl = ctrlSignals.cloneType.asInstanceOf[T]
  }
  val io = IO(new Bundle {
    val in  = Flipped(Decoupled(new InBundle))
    val out = Decoupled(new OutBundle)
  })
 
  val expWidth  = 8
  val precision = 24
  
  val raw = RawFloat.fromUInt(io.in.bits.y, expWidth, precision)
  val sign = raw.sign
  val exp  = raw.exp
  val sig  = raw.sig
 
  val expSigned = (exp.zext - 127.S).asSInt

  val section1 = expSigned <= -8.S
 
  // ===========================
  // Section 1: expSigned <= -8
  // ===========================
  val yiS1  = Cat(sign, 0.U(8.W))
  val yfiS1 = Cat(sign, 0.U(7.W))
  val yfjS1 = io.in.bits.y
 
  // ===========================
  // Section 2: expSigned ∈ [-7, 7]
  // ===========================
  val sigExtended = Cat(0.U(7.W), sig, 0.U(7.W))
  val sigShifted = Mux(expSigned >= 0.S, sigExtended << expSigned.asUInt, sigExtended >> (-expSigned).asUInt)

  val intPart       = sigShifted(37, 30)
  val fracHigh      = sigShifted(29, 23)
  val fracLow       = sigShifted(22, 0)
  val fracLowIsZero = fracLow === 0.U
  val lzdCount      = PriorityEncoder(Reverse(fracLow))
  val expBiased     = Mux(fracLowIsZero, 0.U(8.W), (119.U(8.W) - lzdCount)(7, 0))
  val mantissa      = Mux(fracLowIsZero, 0.U(23.W), ((fracLow << (lzdCount + 1.U))(22, 0)))
  val yiS2          = Cat(sign, intPart)
  val yfiS2         = Cat(sign, fracHigh)
  val yfjS2         = Cat(sign, expBiased, mantissa)

  val s1     = Wire(Decoupled(new OutBundle))
  val s1Pipe = s1.handshakePipeIf(true)
 
  s1.valid     := io.in.valid
  s1.bits.yi   := Mux(section1, yiS1,  yiS2)
  s1.bits.yfi  := Mux(section1, yfiS1, yfiS2)
  s1.bits.yfj  := Mux(section1, yfjS1, yfjS2)
  s1.bits.ctrl := io.in.bits.ctrl
  io.in.ready  := s1.ready
 
  io.out <> s1Pipe
}

class FilterFP32[T <: Bundle](ctrlSignals: Bundle) extends Module {
  class InBundle extends Bundle {
    val in = UInt(32.W)
    val ctrl = ctrlSignals.cloneType.asInstanceOf[T]
  }
  class OutBundle extends Bundle {
    val out       = UInt(32.W)
    val bypass    = Bool()
    val bypassVal = UInt(32.W)
    val ctrl      = ctrlSignals.cloneType.asInstanceOf[T]
  }
  val io = IO(new Bundle {
    val in  = Flipped(Decoupled(new InBundle))
    val out = Decoupled(new OutBundle)
  })

  val s = io.in.bits.in(31)
  val e = io.in.bits.in(30, 23)
  val f = io.in.bits.in(22, 0)

  val isInfPos = (e === "hFF".U) && (f === 0.U) && (s === 0.U)
  val isInfNeg = (e === "hFF".U) && (f === 0.U) && (s === 1.U)
  val isNaN    = (e === "hFF".U) && (f =/= 0.U)

  val tooBig = (!s) && (io.in.bits.in > EXPFP32Parameters.MAX_INPUT) // +88.7
  val tooNeg =   s  && (io.in.bits.in > EXPFP32Parameters.MIN_INPUT) // -87.3

  val bypass = isNaN || isInfPos || isInfNeg || tooBig || tooNeg

  val bypassVal   = Wire(UInt(32.W))

  when (isNaN) {
    bypassVal   := EXPFP32Parameters.NAN
  }.elsewhen (isInfPos || tooBig) {
    bypassVal   := EXPFP32Parameters.INF
  }.elsewhen (isInfNeg || tooNeg) {
    bypassVal   := EXPFP32Parameters.ZERO
  }.otherwise {
    bypassVal   := EXPFP32Parameters.ZERO
  }

  val s1     = Wire(Decoupled(new OutBundle))
  val s1Pipe = s1.handshakePipeIf(true)
  s1.valid          := io.in.valid
  s1.bits.out       := Mux(bypass, s1Pipe.bits.out, io.in.bits.in)
  s1.bits.bypass    := bypass
  s1.bits.bypassVal := Mux(bypass, bypassVal, s1Pipe.bits.bypassVal)
  s1.bits.ctrl      := Mux(bypass, s1Pipe.bits.ctrl, io.in.bits.ctrl)
  io.in.ready       := s1.ready

  io.out <> s1Pipe
}

class EXPFP32 extends Module {
  class InBundle extends Bundle {
    val in = UInt(32.W)
    val rm = UInt(3.W)
  }
  class OutBundle extends Bundle {
    val out = UInt(32.W)
  }
  val io = IO(new Bundle {
    val in  = Flipped(Decoupled(new InBundle))
    val out = Decoupled(new OutBundle)
  })

  class FilterToMul0 extends Bundle {
    val rm = UInt(3.W)
  }
  val filter = Module(new FilterFP32[FilterToMul0](new FilterToMul0))
  io.in.ready               := filter.io.in.ready
  filter.io.in.valid        := io.in.valid
  filter.io.in.bits.in      := io.in.bits.in
  filter.io.in.bits.ctrl.rm := io.in.bits.rm

  class Mul0ToDecompose extends Bundle {
    val rm        = UInt(3.W)
    val bypass    = Bool()
    val bypassVal = UInt(32.W)
  }
  val mul0 = Module(new MULFP32[Mul0ToDecompose](new Mul0ToDecompose))
  filter.io.out.ready            := mul0.io.in.ready
  mul0.io.in.valid               := filter.io.out.valid
  mul0.io.in.bits.a              := filter.io.out.bits.out
  mul0.io.in.bits.b              := EXPFP32Parameters.LOG2E
  mul0.io.in.bits.rm             := filter.io.out.bits.ctrl.rm
  mul0.io.in.bits.ctrl.rm        := filter.io.out.bits.ctrl.rm
  mul0.io.in.bits.ctrl.bypass    := filter.io.out.bits.bypass
  mul0.io.in.bits.ctrl.bypassVal := filter.io.out.bits.bypassVal

  class DecomposeToCMA0 extends Bundle {
    val rm        = UInt(3.W)
    val bypass    = Bool()
    val bypassVal = UInt(32.W)
  }
  val decompose = Module(new DecomposeFP32[DecomposeToCMA0](new DecomposeToCMA0))
  mul0.io.out.ready                   := decompose.io.in.ready
  decompose.io.in.valid               := mul0.io.out.valid
  decompose.io.in.bits.y              := mul0.io.out.bits.result
  decompose.io.in.bits.ctrl.rm        := mul0.io.out.bits.ctrl.rm
  decompose.io.in.bits.ctrl.bypass    := mul0.io.out.bits.ctrl.bypass
  decompose.io.in.bits.ctrl.bypassVal := mul0.io.out.bits.ctrl.bypassVal

  class CMA0ToCMA1 extends Bundle {
    val rm        = UInt(3.W)
    val bypass    = Bool()
    val bypassVal = UInt(32.W)
    val yi        = UInt(9.W)
    val yfi       = UInt(8.W)
    val yfj       = UInt(32.W)
  }
  val cma0   = Module(new CMAFP32[CMA0ToCMA1](new CMA0ToCMA1))
  decompose.io.out.ready         := cma0.io.in.ready
  cma0.io.in.valid               := decompose.io.out.valid
  cma0.io.in.bits.a              := decompose.io.out.bits.yfj
  cma0.io.in.bits.b              := EXPFP32Parameters.C2
  cma0.io.in.bits.c              := EXPFP32Parameters.C1
  cma0.io.in.bits.rm             := decompose.io.out.bits.ctrl.rm
  cma0.io.in.bits.ctrl.rm        := decompose.io.out.bits.ctrl.rm
  cma0.io.in.bits.ctrl.bypass    := decompose.io.out.bits.ctrl.bypass
  cma0.io.in.bits.ctrl.bypassVal := decompose.io.out.bits.ctrl.bypassVal
  cma0.io.in.bits.ctrl.yi        := decompose.io.out.bits.yi
  cma0.io.in.bits.ctrl.yfi       := decompose.io.out.bits.yfi
  cma0.io.in.bits.ctrl.yfj       := decompose.io.out.bits.yfj

  class CMA1ToMUL1 extends Bundle {
    val rm        = UInt(3.W)
    val bypass    = Bool()
    val bypassVal = UInt(32.W)
    val yi        = UInt(9.W)
  }
  val cma1 = Module(new CMAFP32LUTParallel[CMA1ToMUL1](new CMA1ToMUL1))
  cma0.io.out.ready              := cma1.io.in.ready
  cma1.io.in.valid               := cma0.io.out.valid
  cma1.io.in.bits.a              := cma0.io.out.bits.ctrl.yfj
  cma1.io.in.bits.b              := cma0.io.out.bits.result
  cma1.io.in.bits.c              := EXPFP32Parameters.C0
  cma1.io.in.bits.index          := cma0.io.out.bits.ctrl.yfi
  cma1.io.in.bits.rm             := cma0.io.out.bits.ctrl.rm
  cma1.io.in.bits.ctrl.rm        := cma0.io.out.bits.ctrl.rm
  cma1.io.in.bits.ctrl.bypass    := cma0.io.out.bits.ctrl.bypass
  cma1.io.in.bits.ctrl.bypassVal := cma0.io.out.bits.ctrl.bypassVal
  cma1.io.in.bits.ctrl.yi        := cma0.io.out.bits.ctrl.yi

  class MUL1ToMux extends Bundle {
    val bypass    = Bool()
    val bypassVal = UInt(32.W)
    val yi        = UInt(9.W)
  }
  val mul1 = Module(new MULFP32[MUL1ToMux](new MUL1ToMux))
  cma1.io.out.ready              := mul1.io.in.ready
  mul1.io.in.valid               := cma1.io.out.valid
  mul1.io.in.bits.a              := cma1.io.out.bits.result
  mul1.io.in.bits.b              := cma1.io.out.bits.value
  mul1.io.in.bits.rm             := cma1.io.out.bits.ctrl.rm
  mul1.io.in.bits.ctrl.bypass    := cma1.io.out.bits.ctrl.bypass
  mul1.io.in.bits.ctrl.bypassVal := cma1.io.out.bits.ctrl.bypassVal
  mul1.io.in.bits.ctrl.yi        := cma1.io.out.bits.ctrl.yi

  val fResult   = mul1.io.out.bits.result
  val yi        = mul1.io.out.bits.ctrl.yi
  val bypass    = mul1.io.out.bits.ctrl.bypass
  val bypassVal = mul1.io.out.bits.ctrl.bypassVal
  val ef        = fResult(30,23)
  val mant      = fResult(22,0)
  val sign      = yi(8)
  val ei        = yi(7,0)
  val e         = (ef.zext.asSInt + Mux(sign === 1.U, -ei.zext.asSInt, ei.zext.asSInt))(7, 0)
  val mainOut   = Cat(0.U(1.W), e(7, 0), mant) // range cutting ensures no overflow and underflow in exponent
  val out       = Mux(bypass, bypassVal, mainOut)

  val s18 = Wire(Decoupled(new OutBundle))
  val s18Pipe = s18.handshakePipeIf(true)
  s18.valid         := mul1.io.out.valid
  s18.bits.out      := out
  mul1.io.out.ready := s18.ready

  io.out <> s18Pipe
}

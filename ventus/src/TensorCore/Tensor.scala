package TensorCore

import FPUv2.utils._
import chisel3._
import chisel3.util._
import fudian._
import pipeline.TCCtrlv2
import FPUv2.TCCtrl

abstract class TCPipelineModule(len: Int, ctrlGen: Data)
  extends FPUPipelineModule(len, ctrlGen)

class TCAddPipe(expWidth: Int, precision: Int, ctrlGen: Data = EmptyFPUCtrl())
  extends TCPipelineModule(expWidth + precision, ctrlGen){

  override def latency = 2
  val len = expWidth + precision
  val s1 = Module(new FCMA_ADD_s1(expWidth, precision, precision))
  val s2 = Module(new FCMA_ADD_s2(expWidth, precision))

  s1.io.a := S1Reg(io.in.bits.a)
  s1.io.b := S1Reg(io.in.bits.b)
  s1.io.rm := S1Reg(io.in.bits.rm)

  s1.io.b_inter_valid := false.B
  s1.io.b_inter_flags := 0.U.asTypeOf(s1.io.b_inter_flags)

  s2.io.in := S2Reg(s1.io.out)
  io.out.bits.result := s2.io.result
  io.out.bits.fflags := s2.io.fflags
  io.out.bits.ctrl.foreach( _ := S2Reg(S1Reg(io.in.bits.ctrl.get)))
}

class TCAddPipe_Int(len: Int, ctrlGen: Data = EmptyFPUCtrl())
  extends TCPipelineModule(len, ctrlGen){
  override def latency = 1

  io.out.bits.result := io.in.bits.a + io.in.bits.b
  io.out.bits.fflags := 0.U
  io.out.bits.ctrl.foreach( _ := io.in.bits.ctrl.get)
}

class TCMulPipe(expWidth: Int, precision: Int, ctrlGen: Data = EmptyFPUCtrl())
  extends TCPipelineModule(expWidth + precision, ctrlGen){

  override def latency = 2
  val len = expWidth + precision

  //val multiplier = Module(new Multiplier(precision + 1, pipeAt = Seq(1)))
  val multiplier = Module(new NaiveMultiplier(precision + 1, pipeAt = Seq(1)))

  val s1 = Module(new FMUL_s1(expWidth, precision))
  val s2 = Module(new FMUL_s2(expWidth, precision))
  val s3 = Module(new FMUL_s3(expWidth, precision))

  s1.io.a := io.in.bits.a
  s1.io.b := io.in.bits.b
  s1.io.rm := io.in.bits.rm

  s2.io.in := S1Reg(s1.io.out)
  s2.io.prod := multiplier.io.result
  s3.io.in := S2Reg(s2.io.out)

  val raw_a = RawFloat.fromUInt(s1.io.a, s1.expWidth, s1.precision)
  val raw_b = RawFloat.fromUInt(s1.io.b, s1.expWidth, s1.precision)

  multiplier.io.a := raw_a.sig
  multiplier.io.b := raw_b.sig
  multiplier.io.regEnables(0) := regEnable(1)

  io.out.bits.result := s3.io.result
  io.out.bits.fflags := s3.io.fflags
  io.out.bits.ctrl.foreach( _ := S2Reg(S1Reg(io.in.bits.ctrl.get)) )
}

class TCMulPipe_Reused(expWidth: Int, precision: Int, ctrlGen: Data = EmptyFPUCtrl())
  extends TCPipelineModule(expWidth + precision, ctrlGen){
//  only maintain FP16->2INT8
//  TCMulPipe_Reused(5, 11, new DotProdCtrl(len, tcCtrl))

  override def latency = 2
  val INTmode0 = RegNext(io.in.bits.c(0).asBool)//IO(Input(Bool())) //new
  val INTmode = RegNext(INTmode0)
//  val multiplierResult = IO(Output(UInt((2 * precision + 2).W))) //new add; FP16: precision=11

  val len = expWidth + precision

  //val multiplier = Module(new Multiplier(precision + 1, pipeAt = Seq(1)))
  val multiplier = Module(new NaiveMultiplier(precision + 1, pipeAt = Seq(1)))

  val s1 = Module(new FMUL_s1(expWidth, precision))
  val s2 = Module(new FMUL_s2(expWidth, precision))
  val s3 = Module(new FMUL_s3(expWidth, precision))

  s1.io.a := io.in.bits.a
  s1.io.b := io.in.bits.b
  s1.io.rm := io.in.bits.rm

  s2.io.in := S1Reg(s1.io.out)
  s2.io.prod := multiplier.io.result
  s3.io.in := S2Reg(s2.io.out)

  val raw_a = RawFloat.fromUInt(s1.io.a, s1.expWidth, s1.precision)
  val raw_b = RawFloat.fromUInt(s1.io.b, s1.expWidth, s1.precision)

  val processed_a = FloatPoint.fromUInt(io.in.bits.a, expWidth, precision)//divide UInt to sign;exp;sig(precision)
  val processed_b = FloatPoint.fromUInt(io.in.bits.b, expWidth, precision)

  multiplier.io.a := Mux(INTmode,Cat(processed_a.sign, processed_a.sig) ,raw_a.sig)
  multiplier.io.b := Mux(INTmode,Cat(processed_b.sign, processed_b.sig) ,raw_b.sig)
  multiplier.io.regEnables(0) := regEnable(1)

//  multiplierResult := multiplier.io.result

  io.out.bits.result := Mux(INTmode, RegNext(multiplier.io.result(23,8).asUInt()), s3.io.result)
  io.out.bits.fflags := s3.io.fflags
  io.out.bits.ctrl.foreach( _ := S2Reg(S1Reg(io.in.bits.ctrl.get)) )
}

//class TCCtrl(len: Int, depth_warp: Int) extends Bundle{
//  val reg_idxw = UInt(5.W)
//  val warpID = UInt(depth_warp.W)
//}

class DotProdCtrl(len: Int, tcCtrl: Data = EmptyFPUCtrl()) extends Bundle{
  val rm = UInt(3.W)
  val c = UInt(len.W)
  val ctrl = FPUCtrlFac(tcCtrl)
}

class DotProdCtrl_mix(len: Int, tcCtrl: Data = EmptyFPUCtrl()) extends Bundle{
  val rm = UInt(3.W)
  val c = UInt((2*len).W)
  val ctrl = FPUCtrlFac(tcCtrl)
}

class TCDotProductInput(DimN: Int, len: Int, tcCtrl: Data = EmptyFPUCtrl()) extends Bundle{
  val a = Vec(DimN, UInt(len.W))
  val b = Vec(DimN, UInt(len.W))
  //val ctrl = FPUCtrlFac(ctrlGen)
  val c = UInt(len.W)
  val rm = UInt(3.W)
  val ctrl = FPUCtrlFac(tcCtrl) // for TCCtrl
}

class TCDotProductInput_MixedPrecision(DimN: Int, len: Int, tcCtrl: Data = EmptyFPUCtrl()) extends Bundle{
  val a = Vec(DimN, UInt(len.W))
  val b = Vec(DimN, UInt(len.W))
  //val ctrl = FPUCtrlFac(ctrlGen)
  val c = UInt((2*len).W)
  val rm = UInt(3.W)
  val ctrl = FPUCtrlFac(tcCtrl) // for TCCtrl
  val isMixedPrecisionMode = Bool()
}

class TCDotProductBinaryInput(DimN: Int, tcCtrl: Data = EmptyFPUCtrl()) extends Bundle{
  val a = UInt(DimN.W)
  val b = UInt(DimN.W)
  val c = UInt(DimN.W)
  val ctrl = FPUCtrlFac(tcCtrl) // for TCCtrl
}

class TCDotProductInput_Reuse(DimN: Int, len: Int, tcCtrl: Data = EmptyFPUCtrl()) extends Bundle{
  val a = Vec(DimN, UInt(len.W))
  val b = Vec(DimN, UInt(len.W))
  //val ctrl = FPUCtrlFac(ctrlGen)
  val c = UInt(len.W)
  val rm = UInt(3.W)
  val ctrl = FPUCtrlFac(tcCtrl) // for TCCtrl
  val isInt = Bool()
}

class TCDotProductOutput(len: Int, ctrlGen: Data) extends FPUOutput(len, ctrlGen)

class FP16toFP32Converter extends Module {
  val io = IO(new Bundle {
    val in = Input(UInt(16.W)) // 输入FP16
    val out = Output(UInt(32.W)) // 输出FP32
  })

  // FP16格式: 1 bit sign, 5 bits exponent, 10 bits mantissa
  // FP32格式: 1 bit sign, 8 bits exponent, 23 bits mantissa

  // 提取FP16的各个部分
  val sign = io.in(15)
  val exp = io.in(14, 10)
  val frac = io.in(9, 0)

  // 检测零值
  val isZero = (exp === 0.U) && (frac === 0.U)

  // FP32的各个部分
  val fp32Sign = sign
  val fp32Exp = Mux(isZero, 0.U(8.W), (exp + 112.U).asUInt)
  val fp32Frac = Mux(isZero, 0.U(23.W), Cat(0.U(13.W), frac))

  // 构建FP32
  io.out := Cat(fp32Sign, fp32Exp, fp32Frac)
}

class TCDotProduct_MixedPrecision(DimN: Int, expWidth: Int, precision: Int,
                   tcCtrl: Data = EmptyFPUCtrl()) extends Module{
  assert(isPow2(DimN) && DimN > 1)

  val len = expWidth + precision
  val io = IO(new Bundle{
    val in = Flipped(DecoupledIO(new TCDotProductInput_MixedPrecision(DimN, len, tcCtrl)))
    val out = DecoupledIO(new TCDotProductOutput(2*len, tcCtrl))
  })

  def addTree = {
    var vl = DimN
    var adds: Seq[Seq[TCAddPipe]] = Nil
    while (vl > 1) {
      vl = vl / 2
      adds = adds :+ Seq(Module(new TCAddPipe(expWidth, precision, new DotProdCtrl_mix(len, tcCtrl)))) ++
        Seq.fill(vl - 1)(Module(new TCAddPipe(expWidth, precision)))
    }
    adds
  }

  val muls = Seq(Module(new TCMulPipe(expWidth, precision, new DotProdCtrl_mix(len, tcCtrl)))) ++
    Seq.fill(DimN - 1)(Module(new TCMulPipe(expWidth, precision)))
  // connect IN and MULS
  val mctrl = Wire(new DotProdCtrl_mix(len, tcCtrl))
  mctrl.rm := io.in.bits.rm
  mctrl.c := io.in.bits.c
  mctrl.ctrl.foreach( _ := io.in.bits.ctrl.get )
  (0 until DimN).foreach{ i =>
    muls(i).io.in.bits.a := io.in.bits.a(i)
    muls(i).io.in.bits.b := io.in.bits.b(i)
    muls(i).io.in.bits.c := DontCare
    muls(i).io.in.bits.op := DontCare
    muls(i).io.in.bits.rm := mctrl.rm
    muls(i).io.in.bits.ctrl.foreach{ _ := mctrl }
    muls(i).io.in.valid := io.in.valid
    io.in.ready := muls(i).io.in.ready
  }
  val adds = addTree
  val actrls = Seq.fill(log2Ceil(DimN))(Wire(new DotProdCtrl_mix(len, tcCtrl)))
  // connect MULS and ADDS
  (0 until DimN / 2).foreach{ i =>
    adds(0)(i).io.in.bits.a := muls(i).io.out.bits.result
    adds(0)(i).io.in.bits.b := muls(i + DimN/2).io.out.bits.result
    adds(0)(i).io.in.bits.c := DontCare
    adds(0)(i).io.in.bits.op := DontCare
    adds(0)(i).io.in.bits.rm := actrls(0).rm
    adds(0)(i).io.in.bits.ctrl.foreach( _ := muls(i).io.out.bits.ctrl.get )
    adds(0)(i).io.in.valid := muls(i).io.out.valid
    muls(i).io.out.ready := adds(0)(i).io.in.ready
    muls(i + DimN/2).io.out.ready := adds(0)(i).io.in.ready
  }
  private var vl = DimN; private var i = 0;
  while(vl > 1) {
    vl = vl / 2
    actrls(i) := adds(i)(0).io.in.bits.ctrl.get
    if (i != 0) {
      // connect ADDS
      (0 until vl).foreach { j =>
        adds(i)(j).io.in.bits.a := adds(i - 1)(j).io.out.bits.result
        adds(i)(j).io.in.bits.b := adds(i - 1)(j + vl).io.out.bits.result
        adds(i)(j).io.in.bits.c := DontCare
        adds(i)(j).io.in.bits.op := DontCare
        adds(i)(j).io.in.bits.rm := actrls(i).rm
        adds(i)(j).io.in.bits.ctrl.foreach(_ := adds(i - 1)(j).io.out.bits.ctrl.get)

        adds(i)(j).io.in.valid := adds(i - 1)(j).io.out.valid
        adds(i - 1)(j).io.out.ready := adds(i)(j).io.in.ready
        adds(i - 1)(j + vl).io.out.ready := adds(i)(j).io.in.ready
      }
    }
    i = i + 1
  }

  // TODO: transfer FP16->FP32.
  //  Done.
  val fp16to32 = Module(new FP16toFP32Converter)

  val finalAdd = Module(new TCAddPipe(8, 24, new DotProdCtrl_mix(len, tcCtrl)))
  val finalAdd_FP16 = Module(new TCAddPipe(5, 11, new DotProdCtrl_mix(len, tcCtrl)))

  val outpack = Wire(new DotProdCtrl_mix(len, tcCtrl))
  outpack := adds.last.head.io.out.bits.ctrl.get

  fp16to32.io.in := adds.last.head.io.out.bits.result
  finalAdd.io.in.bits.a := fp16to32.io.out//adds.last.head.io.out.bits.result

  finalAdd.io.in.bits.b := outpack.c
  finalAdd.io.in.bits.c := DontCare
  finalAdd.io.in.bits.op := DontCare
  finalAdd.io.in.bits.rm := outpack.rm
  finalAdd.io.in.bits.ctrl.foreach( _ := outpack )
  finalAdd.io.in.valid := adds.last.head.io.out.valid && io.in.bits.isMixedPrecisionMode

  finalAdd_FP16.io.in.bits.a := adds.last.head.io.out.bits.result
  finalAdd_FP16.io.in.bits.b := outpack.c(15,0)
  finalAdd_FP16.io.in.bits.c := DontCare
  finalAdd_FP16.io.in.bits.op := DontCare
  finalAdd_FP16.io.in.bits.rm := outpack.rm
  finalAdd_FP16.io.in.bits.ctrl.foreach( _ := outpack )
  finalAdd_FP16.io.in.valid := adds.last.head.io.out.valid  && (! io.in.bits.isMixedPrecisionMode)

  adds.last.head.io.out.ready := Mux(io.in.bits.isMixedPrecisionMode,finalAdd.io.in.ready,finalAdd_FP16.io.in.ready)

  val fifo = Module(new Queue(new TCDotProductOutput(len, tcCtrl), entries = 1, pipe = true))
  fifo.io.enq.bits.result := Mux(io.in.bits.isMixedPrecisionMode,finalAdd.io.out.bits.result,Cat(0.U(16.W),finalAdd_FP16.io.out.bits.result))
  fifo.io.enq.bits.fflags := Mux(io.in.bits.isMixedPrecisionMode,finalAdd.io.out.bits.fflags,finalAdd_FP16.io.out.bits.fflags)

  val outpack2 = Wire(new DotProdCtrl_mix(len, tcCtrl))
  outpack2 := Mux(io.in.bits.isMixedPrecisionMode,finalAdd.io.out.bits.ctrl.get,finalAdd_FP16.io.out.bits.ctrl.get)
  fifo.io.enq.bits.ctrl.foreach( _ := outpack2.ctrl.get )
  fifo.io.enq.valid := Mux(io.in.bits.isMixedPrecisionMode,finalAdd.io.out.valid,finalAdd_FP16.io.out.valid)
  finalAdd.io.out.ready := fifo.io.enq.ready
  finalAdd_FP16.io.out.ready := fifo.io.enq.ready
  io.out <> fifo.io.deq
}


class TCDotProduct(DimN: Int, expWidth: Int, precision: Int,
                   tcCtrl: Data = EmptyFPUCtrl()) extends Module{
  assert(isPow2(DimN) && DimN > 1)

  val len = expWidth + precision
  val io = IO(new Bundle{
    val in = Flipped(DecoupledIO(new TCDotProductInput(DimN, len, tcCtrl)))
    val out = DecoupledIO(new TCDotProductOutput(len, tcCtrl))
  })

  def addTree = {
    var vl = DimN
    var adds: Seq[Seq[TCAddPipe]] = Nil
    while (vl > 1) {
      vl = vl / 2
      adds = adds :+ Seq(Module(new TCAddPipe(expWidth, precision, new DotProdCtrl(len, tcCtrl)))) ++
        Seq.fill(vl - 1)(Module(new TCAddPipe(expWidth, precision)))
    }
    adds
  }

  val muls = Seq(Module(new TCMulPipe(expWidth, precision, new DotProdCtrl(len, tcCtrl)))) ++
    Seq.fill(DimN - 1)(Module(new TCMulPipe(expWidth, precision)))
  // connect IN and MULS
  val mctrl = Wire(new DotProdCtrl(len, tcCtrl))
  mctrl.rm := io.in.bits.rm
  mctrl.c := io.in.bits.c
  mctrl.ctrl.foreach( _ := io.in.bits.ctrl.get )
  (0 until DimN).foreach{ i =>
    muls(i).io.in.bits.a := io.in.bits.a(i)
    muls(i).io.in.bits.b := io.in.bits.b(i)
    muls(i).io.in.bits.c := DontCare
    muls(i).io.in.bits.op := DontCare
    muls(i).io.in.bits.rm := mctrl.rm
    muls(i).io.in.bits.ctrl.foreach{ _ := mctrl }
    muls(i).io.in.valid := io.in.valid
    io.in.ready := muls(i).io.in.ready
  }
  val adds = addTree
  val actrls = Seq.fill(log2Ceil(DimN))(Wire(new DotProdCtrl(len, tcCtrl)))
  // connect MULS and ADDS
  (0 until DimN / 2).foreach{ i =>
    adds(0)(i).io.in.bits.a := muls(i).io.out.bits.result
    adds(0)(i).io.in.bits.b := muls(i + DimN/2).io.out.bits.result
    adds(0)(i).io.in.bits.c := DontCare
    adds(0)(i).io.in.bits.op := DontCare
    adds(0)(i).io.in.bits.rm := actrls(0).rm
    adds(0)(i).io.in.bits.ctrl.foreach( _ := muls(i).io.out.bits.ctrl.get )
    adds(0)(i).io.in.valid := muls(i).io.out.valid
    muls(i).io.out.ready := adds(0)(i).io.in.ready
    muls(i + DimN/2).io.out.ready := adds(0)(i).io.in.ready
  }
  private var vl = DimN; private var i = 0;
  while(vl > 1) {
    vl = vl / 2
    actrls(i) := adds(i)(0).io.in.bits.ctrl.get
    if (i != 0) {
      // connect ADDS
      (0 until vl).foreach { j =>
        adds(i)(j).io.in.bits.a := adds(i - 1)(j).io.out.bits.result
        adds(i)(j).io.in.bits.b := adds(i - 1)(j + vl).io.out.bits.result
        adds(i)(j).io.in.bits.c := DontCare
        adds(i)(j).io.in.bits.op := DontCare
        adds(i)(j).io.in.bits.rm := actrls(i).rm
        adds(i)(j).io.in.bits.ctrl.foreach(_ := adds(i - 1)(j).io.out.bits.ctrl.get)

        adds(i)(j).io.in.valid := adds(i - 1)(j).io.out.valid
        adds(i - 1)(j).io.out.ready := adds(i)(j).io.in.ready
        adds(i - 1)(j + vl).io.out.ready := adds(i)(j).io.in.ready
      }
    }
    i = i + 1
  }
  val finalAdd = Module(new TCAddPipe(expWidth, precision, new DotProdCtrl(len, tcCtrl)))
  val outpack = Wire(new DotProdCtrl(len, tcCtrl))
  outpack := adds.last.head.io.out.bits.ctrl.get
  finalAdd.io.in.bits.a := adds.last.head.io.out.bits.result
  finalAdd.io.in.bits.b := outpack.c
  finalAdd.io.in.bits.c := DontCare
  finalAdd.io.in.bits.op := DontCare
  finalAdd.io.in.bits.rm := outpack.rm
  finalAdd.io.in.bits.ctrl.foreach( _ := outpack )
  finalAdd.io.in.valid := adds.last.head.io.out.valid
  adds.last.head.io.out.ready := finalAdd.io.in.ready

  val fifo = Module(new Queue(new TCDotProductOutput(len, tcCtrl), entries = 1, pipe = true))
  fifo.io.enq.bits.result := finalAdd.io.out.bits.result
  fifo.io.enq.bits.fflags := finalAdd.io.out.bits.fflags

  val outpack2 = Wire(new DotProdCtrl(len, tcCtrl))
  outpack2 := finalAdd.io.out.bits.ctrl.get
  fifo.io.enq.bits.ctrl.foreach( _ := outpack2.ctrl.get )
  fifo.io.enq.valid := finalAdd.io.out.valid
  finalAdd.io.out.ready := fifo.io.enq.ready
  io.out <> fifo.io.deq
}

class TCDotProduct_IntReuse(DimN: Int, expWidth: Int, precision: Int,
                   tcCtrl: Data = EmptyFPUCtrl()) extends Module{
  assert(isPow2(DimN) && DimN > 1)

  val len = expWidth + precision
  val io = IO(new Bundle{
    val in = Flipped(DecoupledIO(new TCDotProductInput_Reuse(DimN, len, tcCtrl)))
    val out = DecoupledIO(new TCDotProductOutput(len, tcCtrl))
  })

  def addTree = {
    var vl = DimN
    var adds: Seq[Seq[TCAddPipe]] = Nil
    while (vl > 1) {
      vl = vl / 2
      adds = adds :+ Seq(Module(new TCAddPipe(expWidth, precision, new DotProdCtrl(len, tcCtrl)))) ++
        Seq.fill(vl - 1)(Module(new TCAddPipe(expWidth, precision)))
    }
    adds
  }

  def addTree_Int = {
    var vl = DimN
    var adds: Seq[Seq[TCAddPipe_Int]] = Nil
    while (vl > 1) {
      vl = vl / 2
      adds = adds :+ Seq(Module(new TCAddPipe_Int(16, new DotProdCtrl(len, tcCtrl)))) ++
        Seq.fill(vl - 1)(Module(new TCAddPipe_Int(16)))
    }
    adds
  }

  val muls = Seq(Module(new TCMulPipe_Reused(expWidth, precision, new DotProdCtrl(len, tcCtrl)))) ++
    Seq.fill(DimN - 1)(Module(new TCMulPipe_Reused(expWidth, precision)))
  // connect IN and MULS
  val mctrl = Wire(new DotProdCtrl(len, tcCtrl))
  mctrl.rm := io.in.bits.rm
  mctrl.c := io.in.bits.c
  mctrl.ctrl.foreach( _ := io.in.bits.ctrl.get )
  (0 until DimN).foreach{ i =>
    muls(i).io.in.bits.a := io.in.bits.a(i)
    muls(i).io.in.bits.b := io.in.bits.b(i)
    muls(i).io.in.bits.c := DontCare
    muls(i).io.in.bits.op := DontCare
    muls(i).io.in.bits.rm := mctrl.rm
    muls(i).io.in.bits.ctrl.foreach{ _ := mctrl }
    muls(i).io.in.valid := io.in.valid
    muls(i).io.in.bits.c := io.in.bits.isInt.asUInt() //Note: 借路乘法器的第三个操作数最低位作为是否整型乘法标志
    io.in.ready := muls(i).io.in.ready
  }
  val adds = addTree//Mux(io.in.bits.isInt,addTree_Int,addTree)//Note
  val adds_Int = addTree_Int
  val actrls = Seq.fill(log2Ceil(DimN))(Wire(new DotProdCtrl(len, tcCtrl)))
  // connect MULS and ADDS
  (0 until DimN / 2).foreach{ i =>
    adds(0)(i).io.in.bits.a := muls(i).io.out.bits.result
    adds(0)(i).io.in.bits.b := muls(i + DimN/2).io.out.bits.result
    adds(0)(i).io.in.bits.c := DontCare
    adds(0)(i).io.in.bits.op := DontCare
    adds(0)(i).io.in.bits.rm := actrls(0).rm
    adds(0)(i).io.in.bits.ctrl.foreach( _ := muls(i).io.out.bits.ctrl.get )
    adds(0)(i).io.in.valid := muls(i).io.out.valid

    adds_Int(0)(i).io.in.bits.a := muls(i).io.out.bits.result
    adds_Int(0)(i).io.in.bits.b := muls(i + DimN/2).io.out.bits.result
    adds_Int(0)(i).io.in.bits.c := DontCare
    adds_Int(0)(i).io.in.bits.op := DontCare
    adds_Int(0)(i).io.in.bits.rm := actrls(0).rm
    adds_Int(0)(i).io.in.bits.ctrl.foreach( _ := muls(i).io.out.bits.ctrl.get )
    adds_Int(0)(i).io.in.valid := muls(i).io.out.valid

    muls(i).io.out.ready := Mux(io.in.bits.isInt,adds_Int(0)(i).io.in.ready,adds(0)(i).io.in.ready)
    muls(i + DimN/2).io.out.ready := Mux(io.in.bits.isInt,adds_Int(0)(i).io.in.ready,adds(0)(i).io.in.ready)
  }

  private var vl = DimN; private var i = 0;
  while(vl > 1) {
    vl = vl / 2
    actrls(i) := Mux(io.in.bits.isInt,adds_Int(i)(0).io.in.bits.ctrl.get,adds(i)(0).io.in.bits.ctrl.get)
    if (i != 0) {
      // connect ADDS
      (0 until vl).foreach { j =>
        adds(i)(j).io.in.bits.a := adds(i - 1)(j).io.out.bits.result
        adds(i)(j).io.in.bits.b := adds(i - 1)(j + vl).io.out.bits.result
        adds(i)(j).io.in.bits.c := DontCare
        adds(i)(j).io.in.bits.op := DontCare
        adds(i)(j).io.in.bits.rm := actrls(i).rm
        adds(i)(j).io.in.bits.ctrl.foreach(_ := adds(i - 1)(j).io.out.bits.ctrl.get)

        adds(i)(j).io.in.valid := adds(i - 1)(j).io.out.valid
        adds(i - 1)(j).io.out.ready := adds(i)(j).io.in.ready
        adds(i - 1)(j + vl).io.out.ready := adds(i)(j).io.in.ready

        //note
        adds_Int(i)(j).io.in.bits.a := adds_Int(i - 1)(j).io.out.bits.result
        adds_Int(i)(j).io.in.bits.b := adds_Int(i - 1)(j + vl).io.out.bits.result
        adds_Int(i)(j).io.in.bits.c := DontCare
        adds_Int(i)(j).io.in.bits.op := DontCare
        adds_Int(i)(j).io.in.bits.rm := actrls(i).rm
        adds_Int(i)(j).io.in.bits.ctrl.foreach(_ := adds_Int(i - 1)(j).io.out.bits.ctrl.get)

        adds_Int(i)(j).io.in.valid := adds_Int(i - 1)(j).io.out.valid
        adds_Int(i - 1)(j).io.out.ready := adds_Int(i)(j).io.in.ready
        adds_Int(i - 1)(j + vl).io.out.ready := adds_Int(i)(j).io.in.ready
      }
    }
    i = i + 1
  }

  val finalAdd = Module(new TCAddPipe(expWidth, precision, new DotProdCtrl(len, tcCtrl)))
  val finalAdd_Int = Module(new TCAddPipe_Int(16, new DotProdCtrl(len, tcCtrl)))

  //    Mux(io.in.bits.isInt,Module(new TCAddPipe_Int(16, new DotProdCtrl(len, tcCtrl))),
//    Module(new TCAddPipe(expWidth, precision, new DotProdCtrl(len, tcCtrl))))//Note
  val outpack = Wire(new DotProdCtrl(len, tcCtrl))
  outpack := Mux(io.in.bits.isInt,adds_Int.last.head.io.out.bits.ctrl.get,adds.last.head.io.out.bits.ctrl.get)
  finalAdd.io.in.bits.a := adds.last.head.io.out.bits.result
  finalAdd.io.in.bits.b := outpack.c
  finalAdd.io.in.bits.c := DontCare
  finalAdd.io.in.bits.op := DontCare
  finalAdd.io.in.bits.rm := outpack.rm
  finalAdd.io.in.bits.ctrl.foreach( _ := outpack )
  finalAdd.io.in.valid := adds.last.head.io.out.valid

  finalAdd_Int.io.in.bits.a := adds_Int.last.head.io.out.bits.result
  finalAdd_Int.io.in.bits.b := outpack.c
  finalAdd_Int.io.in.bits.c := DontCare
  finalAdd_Int.io.in.bits.op := DontCare
  finalAdd_Int.io.in.bits.rm := outpack.rm
  finalAdd_Int.io.in.bits.ctrl.foreach( _ := outpack )
  finalAdd_Int.io.in.valid := adds_Int.last.head.io.out.valid

  adds_Int.last.head.io.out.ready := finalAdd_Int.io.in.ready
  adds.last.head.io.out.ready := finalAdd.io.in.ready

  val fifo = Module(new Queue(new TCDotProductOutput(len, tcCtrl), entries = 1, pipe = true))
  fifo.io.enq.bits.result := Mux(io.in.bits.isInt,finalAdd_Int.io.out.bits.result,finalAdd.io.out.bits.result)
  fifo.io.enq.bits.fflags := Mux(io.in.bits.isInt,finalAdd_Int.io.out.bits.fflags,finalAdd.io.out.bits.fflags)

  val outpack2 = Wire(new DotProdCtrl(len, tcCtrl))
  outpack2 := Mux(io.in.bits.isInt,finalAdd_Int.io.out.bits.ctrl.get,finalAdd.io.out.bits.ctrl.get)
  fifo.io.enq.bits.ctrl.foreach( _ := outpack2.ctrl.get )
  fifo.io.enq.valid := Mux(io.in.bits.isInt,finalAdd_Int.io.out.valid,finalAdd.io.out.valid)
  finalAdd.io.out.ready := fifo.io.enq.ready
  finalAdd_Int.io.out.ready := fifo.io.enq.ready
  io.out <> fifo.io.deq
}

class TCDotProductBinary(DimN: Int,
                   tcCtrl: Data = EmptyFPUCtrl()) extends Module{
  val io = IO(new Bundle{
    val in = Flipped(DecoupledIO(new TCDotProductBinaryInput(DimN, tcCtrl)))
    val out = DecoupledIO(new TCDotProductOutput(DimN, tcCtrl))
  })
//  io.out.bits.result := PopCount(~(io.in.bits.a^io.in.bits.b)) ^io.in.bits.c
  // 使用寄存器来存储上一拍的计算结果
//  val regResult = Reg(UInt(DimN.W))
  // 当输入有效且输出准备好时，计算当前拍的结果并存储到寄存器中
  when (io.in.valid && io.out.ready) {
//    regResult := PopCount(~(io.in.bits.a ^ io.in.bits.b)) ^ io.in.bits.c
    io.out.valid := true.B
  } .otherwise {
    io.out.valid := false.B
  }
  io.out.bits.result := PopCount(~(io.in.bits.a ^ io.in.bits.b)) ^ io.in.bits.c
  io.out.bits.fflags := 0.U
  io.out.bits.ctrl.foreach(_ := io.in.bits.ctrl.get)
  // 输入的ready信号直接传递给输出，因为我们总是准备好接收新的输入
  io.in.ready := io.out.ready
}

//################################## TCComputation ##################################
class TCComputationInput(DimM:Int,DimN:Int,DimK:Int, len: Int, tcCtrl: TCCtrl) extends Bundle{
  val A = Vec(DimM*DimK, UInt(len.W))
  val B = Vec(DimN*DimK, UInt(len.W))
  val C = Vec(DimM*DimN, UInt(len.W))
//  val op = UInt(3.W)
//  val ctrl = FPUCtrlFac(new EmptyFPUCtrl())
  val rm = UInt(3.W)
  val ctrl = tcCtrl.cloneType
}

class TCComputationInput_MixedPrecision(DimM:Int,DimN:Int,DimK:Int, len: Int, tcCtrl: TCCtrl) extends Bundle{
  val A = Vec(DimM*DimK, UInt(len.W))//FP16
  val B = Vec(DimN*DimK, UInt(len.W))//FP16
  val C = Vec(DimM*DimN, UInt((2*len).W))//mixedPrecision FP32
  //  val op = UInt(3.W)
  //  val ctrl = FPUCtrlFac(new EmptyFPUCtrl())
  val rm = UInt(3.W)
  val ctrl = tcCtrl.cloneType
}

class TCComputationInput_Reuse(DimM:Int,DimN:Int,DimK:Int, len: Int, tcCtrl: TCCtrl) extends Bundle{
  val A = Vec(DimM*DimK, UInt(len.W))
  val B = Vec(DimN*DimK, UInt(len.W))
  val C = Vec(DimM*DimN, UInt(len.W))
  val isInt = Bool()
  //  val op = UInt(3.W)
  //  val ctrl = FPUCtrlFac(new EmptyFPUCtrl())
  val rm = UInt(3.W)
  val ctrl = tcCtrl.cloneType
}

class TCComputationBinaryInput(DimM:Int,DimN:Int,DimK:Int, tcCtrl: TCCtrl) extends Bundle{
  val A = Vec(DimM, UInt(DimK.W))
  val B = Vec(DimN, UInt(DimK.W))
  val C = Vec(DimM*DimN, UInt(DimK.W))
  //  val op = UInt(3.W)
  //  val ctrl = FPUCtrlFac(new EmptyFPUCtrl())
  val rm = UInt(3.W)
  val ctrl = tcCtrl.cloneType
}

class TensorCoreOutput(vl:Int, len: Int, tcCtrl: TCCtrl) extends Bundle{
  val data = Vec(vl, new FPUOutput(len, EmptyFPUCtrl()))
  val ctrl = tcCtrl.cloneType
}


class TC_ComputationArray_MixedPrecision(xDatalen: Int=16, DimM: Int=8, DimN: Int=4, DimK:Int=8, tcCtrl: TCCtrl) extends Module {
  //  mnk defined as cuda.
  //  m8n4k8
  //  xDatalen: data bit len. Here: A\B=FP16; C\D=FP32
  //  TC_ComputationArray_MixedPrecision(16,8,4,8)
  //  Compute: D[m8n4] = A[m8k8] * B[k8n4] + C[m8n4]

  val io = IO(new Bundle{
    val in = Flipped(DecoupledIO(new TCComputationInput_MixedPrecision(DimM, DimN, DimK, xDatalen, tcCtrl)))
    val out = DecoupledIO(new TensorCoreOutput(DimM * DimN, 2*xDatalen, tcCtrl))// out matrix dim=[8,4]
  })
  dontTouch(io.in.bits)
  val TCArray = Seq(Module(new TCDotProduct_MixedPrecision(DimK, 5, 11, tcCtrl))) ++
    Seq.fill(DimM*DimN-1)(Module(new TCDotProduct_MixedPrecision(DimK, 5, 11)))
  // control sig only claim 1 times
  for (i <- 0 until  DimM * DimN) {
    io.out.bits.data(i).result := 0.U
    io.out.bits.data(i).fflags := 0.U
  }
  io.in.ready := TCArray.head.io.in.ready
  io.out.valid := TCArray.head.io.out.valid

  for(m <- 0 until DimM){
    for(n <- 0 until DimN){
      for(k <- 0 until DimK){
        TCArray(m * DimN + n).io.in.bits.a(k) := io.in.bits.A(m*DimK+k)
        TCArray(m * DimN + n).io.in.bits.b(k) := io.in.bits.B(n*DimK+k)//col first
      }
      TCArray(m * DimN + n).io.in.bits.c := io.in.bits.C(m * DimN + n)

      TCArray(m * DimN + n).io.in.bits.rm := io.in.bits.rm
      TCArray(m * DimN + n).io.in.bits.ctrl.foreach(_ := io.in.bits.ctrl)
      TCArray(m * DimN + n).io.in.valid := io.in.valid
      TCArray(m * DimN + n).io.out.ready := io.out.ready

      io.out.bits.data(m * DimN + n).result := TCArray(m * DimN + n).io.out.bits.result
      io.out.bits.data(m * DimN + n).fflags := TCArray(m * DimN + n).io.out.bits.fflags
    }
  }
  io.out.bits.ctrl := TCArray.head.io.out.bits.ctrl.get
}


class TC_ComputationArray_848_FP16(xDatalen: Int=16, DimM: Int=8, DimN: Int=4, DimK:Int=8, tcCtrl: TCCtrl) extends Module {
//  mnk defined as cuda.
//  m8n4k8
//  xDatalen: data bit len
//  TC_ComputationArray_848_FP16(16,8,4,8)
//  Compute: D[m8n4] = A[m8k8] * B[k8n4] + C[m8n4]

  val io = IO(new Bundle{
    val in = Flipped(DecoupledIO(new TCComputationInput(DimM, DimN, DimK, xDatalen, tcCtrl)))
    val out = DecoupledIO(new TensorCoreOutput(DimM * DimN, xDatalen, tcCtrl))// out matrix dim=[8,4]
  })
  dontTouch(io.in.bits)
  val TCArray = Seq(Module(new TCDotProduct(DimK, 5, 11, tcCtrl))) ++
    Seq.fill(DimM*DimN-1)(Module(new TCDotProduct(DimK, 5, 11)))
// control sig only claim 1 times
  for (i <- 0 until  DimM * DimN) {
    io.out.bits.data(i).result := 0.U
    io.out.bits.data(i).fflags := 0.U
  }
  io.in.ready := TCArray.head.io.in.ready
  io.out.valid := TCArray.head.io.out.valid

  for(m <- 0 until DimM){
    for(n <- 0 until DimN){
      for(k <- 0 until DimK){
        TCArray(m * DimN + n).io.in.bits.a(k) := io.in.bits.A(m*DimK+k)
        TCArray(m * DimN + n).io.in.bits.b(k) := io.in.bits.B(n*DimK+k)//col first
//        TCArray(m * DimN + n).io.in.bits.b(k) := io.in.bits.B(k*DimN+n)//row first
      }
      TCArray(m * DimN + n).io.in.bits.c := io.in.bits.C(m * DimN + n)

      TCArray(m * DimN + n).io.in.bits.rm := io.in.bits.rm
      TCArray(m * DimN + n).io.in.bits.ctrl.foreach(_ := io.in.bits.ctrl)
      TCArray(m * DimN + n).io.in.valid := io.in.valid
      TCArray(m * DimN + n).io.out.ready := io.out.ready

      io.out.bits.data(m * DimN + n).result := TCArray(m * DimN + n).io.out.bits.result
      io.out.bits.data(m * DimN + n).fflags := TCArray(m * DimN + n).io.out.bits.fflags
    }
  }
  io.out.bits.ctrl := TCArray.head.io.out.bits.ctrl.get
}

class TC_ComputationArray_848_INT8FP16_Reuse(xDatalen: Int, DimM: Int, DimN: Int, DimK:Int, tcCtrl: TCCtrl) extends Module {
  //  mnk defined as cuda.
  //  m8n4k8
  //  xDatalen: data bit len
  //  TC_ComputationArray_848_FP16(16,8,4,8)
  //  Compute: D[m8n4] = A[m8k8] * B[k8n4] + C[m8n4]

  val io = IO(new Bundle{
    val in = Flipped(DecoupledIO(new TCComputationInput_Reuse(DimM, DimN, DimK, xDatalen, tcCtrl)))
    val out = DecoupledIO(new TensorCoreOutput(DimM * DimN, xDatalen, tcCtrl))// out matrix dim=[8,4]
  })
  val TCArray = Seq(Module(new TCDotProduct_IntReuse(DimK, 5, 11, tcCtrl))) ++
    Seq.fill(DimM*DimN-1)(Module(new TCDotProduct_IntReuse(DimK, 5, 11)))
  // control sig only claim 1 times
  for (i <- 0 until  DimM * DimN) {
    io.out.bits.data(i).result := 0.U
    io.out.bits.data(i).fflags := 0.U
  }
  io.in.ready := TCArray.head.io.in.ready
  io.out.valid := TCArray.head.io.out.valid

  for(m <- 0 until DimM){
    for(n <- 0 until DimN){
      for(k <- 0 until DimK){
        TCArray(m * DimN + n).io.in.bits.a(k) := io.in.bits.A(m*DimK+k)
        TCArray(m * DimN + n).io.in.bits.b(k) := io.in.bits.B(n*DimK+k)//col first
        //        TCArray(m * DimN + n).io.in.bits.b(k) := io.in.bits.B(k*DimN+n)//row first
      }
      TCArray(m * DimN + n).io.in.bits.c := io.in.bits.C(m * DimN + n)

      TCArray(m * DimN + n).io.in.bits.rm := io.in.bits.rm
      TCArray(m * DimN + n).io.in.bits.isInt := io.in.bits.isInt
      TCArray(m * DimN + n).io.in.bits.ctrl.foreach(_ := io.in.bits.ctrl)
      TCArray(m * DimN + n).io.in.valid := io.in.valid
      TCArray(m * DimN + n).io.out.ready := io.out.ready

      io.out.bits.data(m * DimN + n).result := TCArray(m * DimN + n).io.out.bits.result
      io.out.bits.data(m * DimN + n).fflags := TCArray(m * DimN + n).io.out.bits.fflags
    }
  }
  io.out.bits.ctrl := TCArray.head.io.out.bits.ctrl.get
}

class TC_ComputationArray_848_Binary(DimM: Int, DimN: Int, DimK:Int, tcCtrl: TCCtrl) extends Module {
  //  mnk defined as cuda.
  //  m8n4k8
  //  xDatalen: data bit len
  //  TC_ComputationArray_848_Binary(16,8,4,8)
  //  Compute: D[m8n4] = A[m8k8] * B[k8n4] + C[m8n4]

  val io = IO(new Bundle{
    val in = Flipped(DecoupledIO(new TCComputationBinaryInput(DimM, DimN, DimK, tcCtrl)))
    val out = DecoupledIO(new TensorCoreOutput(DimM * DimN, DimK, tcCtrl))
    // out matrix dim=[8,4], Data len is up to DimK.
  })
  val TCArray = Seq(Module(new TCDotProductBinary(DimK, tcCtrl))) ++
    Seq.fill(DimM*DimN-1)(Module(new TCDotProductBinary(DimK)))
  // control sig only claim 1 times
  for (i <- 0 until  DimM * DimN) {
    io.out.bits.data(i).result := 0.U
    io.out.bits.data(i).fflags := 0.U
  }
  io.in.ready := TCArray.head.io.in.ready
  io.out.valid := TCArray.head.io.out.valid

  for(m <- 0 until DimM){
    for(n <- 0 until DimN){
      TCArray(m * DimN + n).io.in.bits.a := io.in.bits.A(m)
      TCArray(m * DimN + n).io.in.bits.b := io.in.bits.B(n)
      TCArray(m * DimN + n).io.in.bits.c := io.in.bits.C(m * DimN + n)

      TCArray(m * DimN + n).io.in.bits.ctrl.foreach(_ := io.in.bits.ctrl)
      TCArray(m * DimN + n).io.in.valid := io.in.valid
      TCArray(m * DimN + n).io.out.ready := io.out.ready

      io.out.bits.data(m * DimN + n).result := TCArray(m * DimN + n).io.out.bits.result
      io.out.bits.data(m * DimN + n).fflags := TCArray(m * DimN + n).io.out.bits.fflags
    }
  }
  io.out.bits.ctrl := TCArray.head.io.out.bits.ctrl.get
}

////xd.Liu'brand. TensorCoreFP32 现已废弃
//class TensorCoreInput(vl: Int, len: Int, tcCtrl: TCCtrl) extends Bundle{
//  val data = Vec(vl, new FPUInput(len, EmptyFPUCtrl(), false))
//  val ctrl = tcCtrl.cloneType
//}

//class TensorCoreFP32(vl: Int, DimM: Int, DimN: Int, DimK:Int, tcCtrl: TCCtrl) extends Module {
//  assert(DimM * DimN <= vl)
//  assert(DimN * DimK <= vl)
//  assert(DimM * DimK <= vl)
//  val io = IO(new Bundle{
//    val in = Flipped(DecoupledIO(new TensorCoreInput(vl,32, tcCtrl)))
//    val out = DecoupledIO(new TensorCoreOutput(vl,32, tcCtrl))
//  })
//  val TCArray = Seq(Module(new TCDotProduct(DimN, 8, 24, tcCtrl))) ++
//    Seq.fill(DimM*DimK-1)(Module(new TCDotProduct(DimN, 8, 24)))
//
//  for (i <- 0 until vl) {
//    io.out.bits.data(i).result := 0.U
//    io.out.bits.data(i).fflags := 0.U
//  }
//  io.in.ready := TCArray.head.io.in.ready
//  io.out.valid := TCArray.head.io.out.valid
//
//  for(m <- 0 until DimM){
//    for(k <- 0 until DimK){
//      for(n <- 0 until DimN){
//        TCArray(m * DimK + k).io.in.bits.a(n) := io.in.bits.data(m * DimN + n).a
//        TCArray(m * DimK + k).io.in.bits.b(n) := io.in.bits.data(k * DimN + n).b
//        TCArray(m * DimK + k).io.in.bits.c := io.in.bits.data(m * DimK + k).c
//      }
//      TCArray(m * DimK + k).io.in.bits.rm := io.in.bits.data(0).rm
//      TCArray(m * DimK + k).io.in.bits.ctrl.foreach(_ := io.in.bits.ctrl)
//      TCArray(m * DimK + k).io.in.valid := io.in.valid
//      TCArray(m * DimK + k).io.out.ready := io.out.ready
//
//      io.out.bits.data(m * DimK + k).result := TCArray(m * DimK + k).io.out.bits.result
//      io.out.bits.data(m * DimK + k).fflags := TCArray(m * DimK + k).io.out.bits.fflags
//    }
//  }
//  io.out.bits.ctrl := TCArray.head.io.out.bits.ctrl.get
//}

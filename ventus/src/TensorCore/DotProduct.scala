package TensorCore

import FPUv2.utils._
import chisel3._
import chisel3.util._
import fudian._
import pipeline.TCCtrlv2
import FPUv2.TCCtrl

class TCCtrl_mix_mul_slot(len: Int,datawidth:Int, depth_warp: Int) extends Bundle{
  val reg_idxw = UInt(5.W)
  val warpID = UInt(depth_warp.W)
  val isMixedPrecisionMode = Bool()
  val tc_ReLU = Bool()
  val tc_shape = UInt(2.W)
  val sel_slot_num = UInt(datawidth.W)
}

class DotProdCtrl(len: Int, tcCtrl: Data = EmptyFPUCtrl()) extends Bundle{
  val rm = UInt(3.W)
  val c = UInt(len.W)
  val ctrl = FPUCtrlFac(tcCtrl)
}

class DotProdCtrl_mix(len: Int, tcCtrl: TCCtrl_mix_mul_slot) extends Bundle{
  val rm = UInt(3.W)
  val c = UInt((2*len).W)
  val ctrl = tcCtrl.cloneType//FPUCtrlFac(tcCtrl)//
}

class TCDotProductInput(DimN: Int, len: Int, tcCtrl: Data = EmptyFPUCtrl()) extends Bundle{
  val a = Vec(DimN, UInt(len.W))
  val b = Vec(DimN, UInt(len.W))
  //val ctrl = FPUCtrlFac(ctrlGen)
  val c = UInt(len.W)
  val rm = UInt(3.W)
  val ctrl = FPUCtrlFac(tcCtrl) // for TCCtrl
}

class TCDotProductInput_MixedPrecision(DimN: Int, len: Int, tcCtrl: TCCtrl_mix_mul_slot) extends Bundle{
  val a = Vec(DimN, UInt(len.W))
  val b = Vec(DimN, UInt(len.W))
  //val ctrl = FPUCtrlFac(ctrlGen)
  val c = UInt((2*len).W)
  val rm = UInt(3.W)
  val ctrl = tcCtrl.cloneType//FPUCtrlFac(tcCtrl) // for TCCtrl
//  val isMixedPrecisionMode = Bool()
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
  val exp = io.in(14, 10)//.asUInt()-15+127
  val frac = io.in(9, 0)

  // 检测零值
  val isZero = (exp === 0.U) && (frac === 0.U)

  // FP32的各个部分
  val fp32Sign = sign
  val fp32Exp = Mux(isZero, 0.U(8.W), exp.asTypeOf(UInt(8.W)) + 112.U)  //.asTypeOf(UInt(10.W))(7,0))
  val fp32Frac = Mux(isZero, 0.U(23.W), Cat(frac,0.U(13.W)))

  // 构建FP32
  io.out := Cat(fp32Sign, fp32Exp, fp32Frac)
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

class TCDotProduct_MixedPrecision(DimN: Int, expWidth: Int, precision: Int,
                   tcCtrl: TCCtrl_mix_mul_slot) extends Module{
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
  mctrl.ctrl := io.in.bits.ctrl
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

  val fp16to32 = Module(new FP16toFP32Converter)

  val finalAdd = Module(new TCAddPipe(8, 24, new DotProdCtrl_mix(len, tcCtrl)))
  val finalAdd_FP16 = Module(new TCAddPipe(5, 11, new DotProdCtrl_mix(len, tcCtrl)))

  val outpack = Wire(new DotProdCtrl_mix(len, tcCtrl))
  outpack := adds.last.head.io.out.bits.ctrl.get

  fp16to32.io.in := adds.last.head.io.out.bits.result
  finalAdd.io.in.bits.a := fp16to32.io.out

  finalAdd.io.in.bits.b := outpack.c
  finalAdd.io.in.bits.c := DontCare
  finalAdd.io.in.bits.op := DontCare
  finalAdd.io.in.bits.rm := outpack.rm
  finalAdd.io.in.bits.ctrl.foreach( _ := outpack )
  finalAdd.io.in.valid := adds.last.head.io.out.valid && outpack.ctrl.isMixedPrecisionMode//adds.last.head.io.out.bits.ctrl.get//isMix

  finalAdd_FP16.io.in.bits.a := adds.last.head.io.out.bits.result
  finalAdd_FP16.io.in.bits.b := outpack.c(15,0)
  finalAdd_FP16.io.in.bits.c := DontCare
  finalAdd_FP16.io.in.bits.op := DontCare
  finalAdd_FP16.io.in.bits.rm := outpack.rm
  finalAdd_FP16.io.in.bits.ctrl.foreach( _ := outpack )
  finalAdd_FP16.io.in.valid := adds.last.head.io.out.valid  && (! outpack.ctrl.isMixedPrecisionMode)

  adds.last.head.io.out.ready := Mux( outpack.ctrl.isMixedPrecisionMode,finalAdd.io.in.ready,finalAdd_FP16.io.in.ready)

  val fifo = Module(new Queue(new TCDotProductOutput(2*len, tcCtrl), entries = 1, pipe = true))
  // 使用 Mux 处理 tc_ReLU 标志位和符号位
  val out_fp32 = Wire(UInt(32.W))
  out_fp32 := Mux(io.in.bits.ctrl.tc_ReLU,
    Mux(finalAdd.io.out.bits.result(31), 0.U(32.W), finalAdd.io.out.bits.result), // 如果 tc_ReLU 为 1 且结果为负数，输出 0；否则输出结果
    finalAdd.io.out.bits.result) // 如果 tc_ReLU 不为 1，直接输出结果

  val out_fp16 = Wire(UInt(32.W))
  out_fp16 := Mux(io.in.bits.ctrl.tc_ReLU,
    Mux(finalAdd_FP16.io.out.bits.result(15), 0.U(32.W), Cat(0.U(16.W),finalAdd_FP16.io.out.bits.result)), // 如果 tc_ReLU 为 1 且结果为负数，输出 0；否则输出结果
    Cat(0.U(16.W),finalAdd_FP16.io.out.bits.result)) // 如果 tc_ReLU 不为 1，直接输出结果

//  fifo.io.enq.bits.result := Mux(outpack.ctrl.isMixedPrecisionMode,finalAdd.io.out.bits.result,Cat(0.U(16.W),finalAdd_FP16.io.out.bits.result))
  fifo.io.enq.bits.result := Mux(outpack.ctrl.isMixedPrecisionMode,out_fp32,out_fp16)
  fifo.io.enq.bits.fflags := Mux(outpack.ctrl.isMixedPrecisionMode,finalAdd.io.out.bits.fflags,finalAdd_FP16.io.out.bits.fflags)

  val outpack2 = Wire(new DotProdCtrl_mix(len, tcCtrl))
  outpack2 := Mux( outpack.ctrl.isMixedPrecisionMode,finalAdd.io.out.bits.ctrl.get,finalAdd_FP16.io.out.bits.ctrl.get)
  fifo.io.enq.bits.ctrl.foreach( _ := outpack2.ctrl)
//  fifo.io.enq.bits.ctrl := outpack2.ctrl
  fifo.io.enq.valid := Mux( outpack.ctrl.isMixedPrecisionMode,finalAdd.io.out.valid,finalAdd_FP16.io.out.valid)
  finalAdd.io.out.ready := fifo.io.enq.ready
  finalAdd_FP16.io.out.ready := fifo.io.enq.ready
  io.out <> fifo.io.deq
}





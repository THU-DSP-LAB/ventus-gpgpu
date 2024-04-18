package TensorCore

import FPUv2.utils.FPUOps._
import FPUv2.utils._
import chisel3._
import chisel3.util._
import fudian._

class FMULPipe_re(expWidth: Int, precision: Int, ctrlGen: Data = EmptyFPUCtrl())
  extends FPUPipelineModule(expWidth + precision, ctrlGen) {
  override def latency: Int = 2

  val INTmode = IO(Input(Bool())) //new
  val toAdd = IO(Output(new MulToAddIO(expWidth, precision, ctrlGen)))
  val multiplierResult = IO(Output(UInt((2 * precision + 2).W))) //new add; FP16: precision=11

  //val multiplier = Module(new Multiplier(precision + 1, pipeAt = Seq(1)))
  val multiplier = Module(new NaiveMultiplier(precision + 1, pipeAt = Seq(1)))
  val s1 = Module(new FMUL_s1(expWidth, precision))
  val s2 = Module(new FMUL_s2(expWidth, precision))
  val s3 = Module(new FMUL_s3(expWidth, precision))

  val invProd = withInvProd(io.in.bits.op)

  s1.io.a := io.in.bits.a
  s1.io.b := Mux(invProd, invertSign(io.in.bits.b), io.in.bits.b)
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

  multiplierResult := multiplier.io.result

  toAdd.ctrl.foreach( _ := S2Reg(S1Reg(io.in.bits.ctrl.get)))
  toAdd.addAnother := S2Reg(S1Reg(io.in.bits.c))
  toAdd.mulOutput := s3.io.to_fadd
  toAdd.op := S2Reg(S1Reg(io.in.bits.op))
  toAdd.rm := S2Reg(S1Reg(io.in.bits.rm))
  io.out.bits.result := s3.io.result
  io.out.bits.fflags := s3.io.fflags
  //io.out.bits.ctrl := toAdd.ctrl
  io.out.bits.ctrl.foreach( _ := toAdd.ctrl.get)
}

class NaiveMultiplier_Int8(len: Int, pipeAt: Seq[Int]) extends Module {
  val io = IO(new Bundle() {
    val a, b = Input(UInt(8.W))
    val regEnables = Input(Vec(pipeAt.size, Bool()))
    val result = Output(UInt((len*2).W))
    val result_16 = Output(UInt(16.W))
    val sum = Output(UInt(len.W))
    val carry = Output(UInt(len.W))
  })
  val int8Extender_a = Module(new Int8Extender_12)
  val int8Extender_b = Module(new Int8Extender_12)
  int8Extender_a.io.in := io.a
  int8Extender_b.io.in := io.b

  io.result := RegEnable(int8Extender_a.io.out, io.regEnables(0)) * RegEnable(int8Extender_b.io.out, io.regEnables(0))
  io.result_16 := io.result(23,8).asUInt()
  io.sum := 0.U
  io.carry := 0.U
}

class NaiveMultiplier_Int4(len: Int, pipeAt: Seq[Int]) extends Module {
  val io = IO(new Bundle() {
    val a, b, c = Input(UInt(4.W))
    val regEnables = Input(Vec(pipeAt.size, Bool()))
    val result = Output(UInt((len*2).W))
    val result_INT4_out_bc = Output(UInt(8.W))
    val result_INT4_out_ac = Output(UInt(8.W))
    val sum = Output(UInt(len.W))
    val carry = Output(UInt(len.W))
  })
  val int4Extender = Module(new Int4Extender_12)
//  val int4GetResult = Module(new Int4GetResult)

  int4Extender.io.inA := io.a
  int4Extender.io.inB := io.b
  int4Extender.io.inC := io.c

  io.result := RegEnable(int4Extender.io.out1, io.regEnables(0)) * RegEnable(int4Extender.io.out2, io.regEnables(0))

//  int4GetResult.io.inB := io.b
//  int4GetResult.io.inC := io.c
//  int4GetResult.io.in := RegEnable(int4Extender.io.out1, io.regEnables(0)) * RegEnable(int4Extender.io.out2, io.regEnables(0))

  io.result_INT4_out_bc := io.result(7,0).asUInt()//int4GetResult.io.out_bc
  io.result_INT4_out_ac := io.result(15,8).asUInt() //int4GetResult.io.out_ac
  io.sum := 0.U
  io.carry := 0.U
}

class Multiplier_Binary(len: Int, pipeAt: Seq[Int]) extends Module {
  val io = IO(new Bundle() {
    val a0, w = Input(UInt(len.W))
//    val regEnables = Input(Vec(pipeAt.size, Bool()))
    val a1 = Output(UInt(len.W))
  })
//  val temp = PopCount(~(io.a0^io.w))
  io.a1 := PopCount(~(io.a0^io.w))
//  io.a1 := RegEnable(PopCount((~(io.a0^io.w)).asBools), io.regEnables(0))

}

class Int8Extender_12 extends Module {
  val io = IO(new Bundle {
    val in = Input(UInt(8.W))  // 输入端口，8 位无符号整数
    val out = Output(UInt(12.W)) // 输出端口，12 位无符号整数
  })
  io.out := Cat(io.in, 0.U(4.W)).asUInt()
}

class Int8Extender_16 extends Module {
  val io = IO(new Bundle {
    val in = Input(UInt(8.W))  // 输入端口，8 位无符号整数
    val out = Output(UInt(16.W)) // 输出端口，16 位无符号整数
  })
  io.out := Cat(io.in(7), 0.U(4.W), io.in(6, 0), 0.U(4.W))
}

class Int4Extender_12 extends Module {
  val io = IO(new Bundle {
    val inA = Input(UInt(4.W))  // 输入端口，4 位无符号整数
    val inB = Input(UInt(4.W))  // 输入端口，4 位无符号整数
    val inC = Input(UInt(4.W))  // 输入端口，4 位无符号整数
    val out1 = Output(UInt(12.W)) // 输出端口，12 位无符号整数
    val out2 = Output(UInt(12.W)) // 输出端口，12 位无符号整数
  })

  io.out1 := Cat(io.inA,0.U(4.W), io.inB).asUInt()
  // 将io.inC的位放在low位，然后在high位附加一个12位的零值
  io.out2 := Cat(0.U(8.W),io.inC).asUInt()
}
class Int4Extender_16 extends Module {
  val io = IO(new Bundle {
    val inA = Input(UInt(4.W))  // 输入端口，4 位无符号整数
    val inB = Input(UInt(4.W))  // 输入端口，4 位无符号整数
    val inC = Input(UInt(4.W))  // 输入端口，4 位无符号整数
    val out1 = Output(UInt(16.W)) // 输出端口，16 位无符号整数
    val out2 = Output(UInt(16.W)) // 输出端口，16 位无符号整数
  })

  io.out1 := Cat(io.inA(3), 0.U(4.W), io.inA(2, 0),0.U(4.W), io.inB).asUInt()
  // 将io.inC的位放在高位，然后在低位附加一个12位的零值
  io.out2 := Cat(0.U(12.W), io.inC).asUInt()
}

class Int4GetResult extends Module {
  val io = IO(new Bundle {
    val in = Input(UInt(24.W))  // 输入端口，24 位无符号整数
    val out_bc = Output(UInt(8.W)) // 输出端口，8 位无符号整数（BC 相乘结果）
    val out_ac = Output(UInt(8.W)) // 输出端口，8 位无符号整数（AC 相乘结果）
  })

  // 提取 BC 相乘的结果（低 8 位）
  io.out_bc := io.in(7, 0)

  // 检查特定的位条件
//  val condition = (io.inB(3) & io.inC(3)) & (io.inB(2) & io.inC(2))

  // 根据条件计算 AC 相乘的结果
//  io.out_ac := Mux(condition, io.in(15, 8) - 1.U, io.in(15, 8))
  io.out_ac := io.in(15, 8) //Mux(condition, io.in(15, 8), io.in(15, 8))
}

class FMULReused(expWidth: Int, precision: Int, ctrlGen: Data = EmptyFPUCtrl())
  extends FPUSubModule(expWidth + precision, ctrlGen) {
  val INTmode = IO(Input(Bool()))
//  val InputINT8_A = IO(Input(UInt((precision+1).W)))
  val multiplierResult = IO(Output(UInt((2 * precision + 2).W)))

  val mulPipe = Module(new FMULPipe_re(expWidth, precision, ctrlGen))
  val addPipe = Module(new FADDPipe(expWidth, precision, ctrlGen))
//  val InputInt8_a = Module(new Int8Extender())
//  InputInt8_a.io.in := io.in.bits.a_UINT8
//  val InputInt8_b = Module(new Int8Extender())
//  InputInt8_b.io.in := io.in.bits.b_UINT8
//
//  mulPipe.io.in.bits.a := InputInt8_a.io.out
//  mulPipe.io.in.bits.b := InputInt8_b.io.out

  mulPipe.io.in.bits := io.in.bits
//  mulPipe.io.in.bits.rm := io.in.bits.rm

  mulPipe.INTmode := INTmode
  mulPipe.io.in.valid := io.in.valid && (FPUOps.isFMA(io.in.bits.op) || FPUOps.isFMUL(io.in.bits.op))

//  io.out := mulPipe.io.out
  multiplierResult := mulPipe.multiplierResult
  // 加法器从FMA输入端和乘法器输出端接收数据
  // 乘加和加法同时抵达时，乘加优先级更高: 0->输入来自乘法器输出, 1->输入来自外层输入
  class ArbiterIO extends Bundle {
    //val ctrl = if (hasCtrl) new FPUCtrl else new FPUCtrl(false)
    val ctrl = FPUCtrlFac(ctrlGen.cloneType)
    val op = UInt(3.W)
  }

  val toAddArbiter = Module(new Arbiter(new ArbiterIO, 2))
  val toAddArbiterFIFO = Seq.fill(2)(Module(new Queue(new ArbiterIO, entries = 1, pipe = true)))
  toAddArbiterFIFO(1).io.enq.bits.op := io.in.bits.op(2,0)
  toAddArbiterFIFO(1).io.enq.bits.ctrl.foreach( _ := io.in.bits.ctrl.get )
  toAddArbiterFIFO(0).io.enq.bits.op := mulPipe.toAdd.op
  toAddArbiterFIFO(0).io.enq.bits.ctrl.foreach( _ := mulPipe.toAdd.ctrl.get )
  toAddArbiterFIFO(1).io.enq.valid := FPUOps.isADDSUB(io.in.bits.op) && io.in.valid
  toAddArbiterFIFO(0).io.enq.valid := FPUOps.isFMA(mulPipe.toAdd.op) && mulPipe.io.out.valid
  toAddArbiter.io.in(0) <> toAddArbiterFIFO(0).io.deq
  toAddArbiter.io.in(1) <> toAddArbiterFIFO(1).io.deq
  addPipe.io.in.bits.ctrl.foreach{ _ := toAddArbiter.io.out.bits.ctrl.get }

  val inToAddFIFO = Module(new Queue(io.in.bits.cloneType, entries = 1, pipe = true))
  inToAddFIFO.io.enq.bits := io.in.bits
  inToAddFIFO.io.enq.valid := FPUOps.isADDSUB(io.in.bits.op) && io.in.valid
  addPipe.io.in.bits := inToAddFIFO.io.deq.bits
  addPipe.io.in.bits.op := toAddArbiter.io.out.bits.op
  inToAddFIFO.io.deq.ready := toAddArbiter.io.in(1).ready

  val mulToAddFIFO = Module(new Queue(new MulToAddIO(expWidth, precision, ctrlGen), entries = 1, pipe = true))
  mulToAddFIFO.io.enq.bits := mulPipe.toAdd
  mulToAddFIFO.io.enq.valid := toAddArbiterFIFO(0).io.enq.fire
  addPipe.fromMul := mulToAddFIFO.io.deq.bits
  mulToAddFIFO.io.deq.ready := toAddArbiter.io.in(0).ready
  //addPipe.fromMul := RegNext(mulPipe.toAdd, ArbiterInQueue(0).io.enq.fire)

  toAddArbiter.io.out.ready := addPipe.io.in.ready
  addPipe.io.in.valid := toAddArbiter.io.out.valid
  addPipe.io.in.bits.ctrl.foreach( _ := toAddArbiter.io.out.bits.ctrl.get )

  // 加法为乘加让行的同时也会阻塞FMA输入，确保自己之后能够进入流水线
  // 另一种阻塞FMA输入的情况是乘法器那边卡住了
  //io.in.ready := mulPipe.io.in.ready && !(ArbiterInQueue(1).io.enq.valid && !ArbiterInQueue(1).io.enq.ready)
  io.in.ready := Mux(FPUOps.isADDSUB(io.in.bits.op), toAddArbiterFIFO(1).io.enq.ready, mulPipe.io.in.ready)
  val mulFIFO = Module(new Queue(new FPUOutput(expWidth + precision, ctrlGen), entries = 1, pipe = true))
  val addFIFO = Module(new Queue(new FPUOutput(expWidth + precision, ctrlGen), entries = 1, pipe = true))
  mulFIFO.io.enq.bits := mulPipe.io.out.bits
  mulFIFO.io.enq.valid := mulPipe.io.out.valid && FPUOps.isFMUL(mulPipe.toAdd.op)
  addFIFO.io.enq <> addPipe.io.out

  mulPipe.io.out.ready := (toAddArbiterFIFO(0).io.enq.ready && FPUOps.isFMA(mulPipe.toAdd.op)) ||
    (mulFIFO.io.enq.ready && FPUOps.isFMUL(mulPipe.toAdd.op))

  // FMA输出端从乘法输出端和加法输出端接收数据，加法(乘加)优先级更高
  val toOutArbiter = Module(new Arbiter(new FPUOutput(expWidth + precision, ctrlGen), 2))
  toOutArbiter.io.in(0) <> addFIFO.io.deq
  toOutArbiter.io.in(1) <> mulFIFO.io.deq
  io.out <> toOutArbiter.io.out

  }

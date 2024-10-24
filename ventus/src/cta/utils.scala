package cta.utils

/**
 * @see docs/cta_scheduler/Utilities.md
 */

import chisel3._
import chisel3.util._
import scala.math.max

/** Round-Robin-Priority Binary Encoder
 * @param n: length of the input Bool-Vector
 * @IO out.fire: A valid encoding result is accepted by outer module, and priority should be updated in RR method
 */
class RRPriorityEncoder(n: Int, initsel: UInt = 0.U) extends Module {
  val io = IO(new Bundle {
    val in = Input(Vec(n, Bool()))
    val out = DecoupledIO(Output(UInt(log2Ceil(n).W)))
  })
  assert(initsel < n.U)
  val addrWidth = log2Ceil(n).W

  val last = RegInit(UInt(addrWidth), initsel - 1.U(addrWidth))
  val in, in_RR = Wire(UInt(n.W))
  in := io.in.asUInt

  val shift = last + 1.U
  in_RR := (in >> shift) | (in << (n.U - shift))   // Circular shift right `shift = last+1`

  io.out.valid := in.orR
  io.out.bits := Mux(in.orR, PriorityEncoder(in_RR) + shift, DontCare)

  when(io.out.fire) {
    last := PriorityEncoder(in_RR) + shift        // update priority
  }
}

object RRPriorityEncoder {
  def apply(in: Vec[Bool]): DecoupledIO[UInt]= {
    val inst = Module(new RRPriorityEncoder(in.size))
    inst.io.in := in
    inst.io.out
  }
  def apply(in: Bits): DecoupledIO[UInt]= {
    val inst = Module(new RRPriorityEncoder(in.getWidth))
    inst.io.in := VecInit(in.asBools)
    inst.io.out
  }
}

/**
 * Combinational sort circuit for three UInt
 * io.out(0) is the biggest, io.out(2) is the smallest
 * @param WIDTH width of the input/output UInt
 */
class sort3(val WIDTH: Int) extends Module {
  val io = IO(new Bundle{
    val in = Input(Vec(3, UInt(WIDTH.W)))
    val out = Output(Vec(3, UInt(WIDTH.W)))
  })
  val cmp0 = io.in(0) > io.in(1)
  val cmp1 = io.in(1) > io.in(2)
  val cmp2 = io.in(2) > io.in(0)
  val res = Wire(Vec(3, Vec(2, Bool())))
  res(0)(0) := cmp0
  res(0)(1) := !cmp2
  res(1)(0) := cmp1
  res(1)(1) := !cmp0
  res(2)(0) := cmp2
  res(2)(1) := !cmp1
  io.out(0) := Mux1H(Seq(
    res(0).asUInt.andR -> io.in(0),
    res(1).asUInt.andR -> io.in(1),
    res(2).asUInt.andR -> io.in(2),
    !(res(0)(0) || res(1)(0) || res(2)(0)) -> io.in(0), // in1 == in2 == in3
  ))
  io.out(1) := Mux1H(Seq(             // NOTE: here we assumes that when in(0)==in(1)==in(2)==data,
    res(0).asUInt.xorR -> io.in(0),   //       as a result of which res(i).xorR==true,
    res(1).asUInt.xorR -> io.in(1),   //       Mux1H will output `data` instead of something meaningless.
    res(2).asUInt.xorR -> io.in(2),   //       If it's not the truth, an extra Mux is needed
  ))
  io.out(2) := Mux1H(Seq(
    !res(0).asUInt.orR -> io.in(0),
    !res(1).asUInt.orR -> io.in(1),
    !res(2).asUInt.orR -> io.in(2),
    !(res(0)(0) || res(1)(0) || res(2)(0)) -> io.in(0),  // in1 == in2 == in3
  ))

  assert(io.out(0) >= io.out(1) && io.out(1) >= io.out(2))
  for(i <- 0 until 3) assert(io.out(i) === io.in(0) || io.out(i) === io.in(1) || io.out(i) === io.in(2) )
}

object sort3 {
  def apply(in: Vec[UInt]): Vec[UInt] = {
    val inst = Module(new sort3(in(0).getWidth))
    inst.io.in := in
    inst.io.out
  }
  def apply(in0: UInt, in1: UInt, in2: UInt): Vec[UInt] = {
    val width = max(in0.getWidth, max(in1.getWidth, in2.getWidth))
    val in = Wire(Vec(3, UInt(width.W)))
    in(0) := in0
    in(1) := in1
    in(2) := in2
    apply(in)
  }
}

/**
 * DecoupledIO 1-to-3
 * It's assumed that once in.valid=true, it will keep being true until io.fire
 */
class DecoupledIO_1_to_3[T0 <: Data, T1 <: Data, T2 <: Data](gen0: T0, gen1: T1, gen2: T2, IGNORE: Boolean = false) extends Module {
  val io = IO(new Bundle {
    val in = Flipped(DecoupledIO(new Bundle{
      val data0 = gen0.cloneType
      val data1 = gen1.cloneType
      val data2 = gen2.cloneType
      val ign0 = if(IGNORE) Some(Bool()) else None
      val ign1 = if(IGNORE) Some(Bool()) else None
      val ign2 = if(IGNORE) Some(Bool()) else None
    }))
    val out0 = DecoupledIO(gen0.cloneType)
    val out1 = DecoupledIO(gen1.cloneType)
    val out2 = DecoupledIO(gen2.cloneType)
  })
  io.out0.bits <> io.in.bits.data0
  io.out1.bits <> io.in.bits.data1
  io.out2.bits <> io.in.bits.data2

  val fire0, fire1, fire2 = RegInit(false.B)
  fire0 := Mux(io.in.fire, false.B, fire0 || io.out0.fire || (io.in.bits.ign0.getOrElse(false.B) && io.in.valid))
  fire1 := Mux(io.in.fire, false.B, fire1 || io.out1.fire || (io.in.bits.ign1.getOrElse(false.B) && io.in.valid))
  fire2 := Mux(io.in.fire, false.B, fire2 || io.out2.fire || (io.in.bits.ign2.getOrElse(false.B) && io.in.valid))
  io.in.ready := (fire0 || io.out0.fire || io.in.bits.ign0.getOrElse(false.B)) &&
    (fire1 || io.out1.fire || io.in.bits.ign1.getOrElse(false.B)) &&
    (fire2 || io.out2.fire || io.in.bits.ign2.getOrElse(false.B))
  io.out0.valid := io.in.valid && !fire0 && !io.in.bits.ign0.getOrElse(false.B)
  io.out1.valid := io.in.valid && !fire1 && !io.in.bits.ign1.getOrElse(false.B)
  io.out2.valid := io.in.valid && !fire2 && !io.in.bits.ign2.getOrElse(false.B)

  // It's assumed that once in.valid=true, it will keep being true until io.fire
  val in_fire_r = RegNext(io.in.fire, false.B)
  val in_valid_r = RegNext(io.in.valid, false.B)
  assert(in_fire_r || !(in_valid_r && !io.in.valid))
}

/** Skid buffer for DecoupledIO.ready
 *  io.in.ready is registered
 */
class skid_ready[T <: Data](gen: T) extends Module {
  val io = IO(new Bundle{
    val in = Flipped(DecoupledIO(gen))
    val out = DecoupledIO(gen)
  })

  val in_ready = RegInit(true.B)          // Initially, skidReg is empty, so at least 1 data can be received
  val dataValid = WireInit(!in_ready)

  val skid = io.in.fire && !io.out.ready  // new data received && downstream refuse to get new data <=> skid
  val data = RegEnable(io.in.bits, skid)  // skid => skidReg updated

  // @posedge(clk) in_ready := true   <=>   (it is newly set to true) || (it keeps its original true value)
  // original true kept <=> skidReg keeps clean <=> skidReg was clean && no data is written(!skid)
  // new true <=> skidReg cleared <=> skidReg wasn't clean && downstream is ok to accept new data
  in_ready := (in_ready && !skid) || (!in_ready && io.out.ready)

  io.in.ready := in_ready
  io.out.valid := dataValid || io.in.valid
  io.out.bits := Mux(dataValid, data, io.in.bits)   // bypass
}

object skid_ready {
  def apply[T <: Data](in: DecoupledIO[T]) : DecoupledIO[T] = {
    val inst = Module(new skid_ready(chiselTypeOf(in.bits)))
    inst.io.in := in
    inst.io.out
  }
}

/** Skid buffer for DecoupledIO.valid and DecoupledIO.bits
 */
class skid_valid[T <: Data](gen: T) extends Module {
  val io = IO(new Bundle() {
    val in = Flipped(DecoupledIO(gen))
    val out = DecoupledIO(gen)
    val in_en = Input(Bool())
  })
  val data = RegEnable(io.in.bits, io.in.fire)  // Newly received data is stored to dataReg

  val dataValid = RegInit(false.B)              // dataReg is empty initially

  //when(io.in.valid && io.in_en) { // upstream new data available
  //  dataValid := true.B
  //} .elsewhen(!io.out.ready) {    // upstream unavailable && downstream unready
  //  dataValid := dataValid
  //} .otherwise {                  // upstream unavailable && downstream ready
  //  dataValid := false.B
  //}
  dataValid := (io.in.valid && io.in_en) || (!io.out.ready && dataValid) // equivalent logic

  io.in.ready := !dataValid || io.out.ready && io.in_en  // dataReg is empty || downstream is ok to take data out of dataReg
  io.out.valid := dataValid
  io.out.bits := data
}

object skid_valid {
  /** skid buffer for DecoupledIO valid and data
   *
   * @param in: upstream DecoupledIO interface
   * @param in_en: external control logic which allows getting new data from upstream
   * @return downstream DecoupledIO interface
   */
  def apply[T <: Data](in: DecoupledIO[T], in_en: Bool = true.B): DecoupledIO[T] = {
    val inst = Module(new skid_valid(chiselTypeOf(in.bits)))
    inst.io.in := in
    inst.io.in_en := in_en
    inst.io.out
  }
}

/** Variable-Radix counter, eg. 24h-60m-60s clock counter
 *  Each place of the counter may have a different radix,
 *  eg. threadID in a 8z-16y-32x thread block
 * @param N: how many places does this counter have
 * @param width: how width each place of the counter is
 * @param io_radix: the new radix of each place. When radix.valid, the counter will be cleared and start from zero
 * @param io_cnt: the counter's value. When cnt.fire, the counter will step up 1
 *
 * @note io.cnt will cleared to zero the next cycle after radix.valid
 */
class cnt_varRadix(val N: Int, width: Int) extends Module {
  val io = IO(new Bundle {
    val radix = Flipped(ValidIO(Vec(N, UInt(width.W))))
    val cnt = DecoupledIO(Vec(N, UInt(width.W)))
  })
  val radix = Reg(Vec(N, UInt(width.W)))
  val cnt = Reg(Vec(N, UInt(width.W)))

  when (io.radix.valid) {
    radix := io.radix.bits
  }

  io.cnt.valid := true.B
  io.cnt.bits := cnt

  val carry = Vec(N, Bool())            // 每一位是否已满，加一后需要进位
  carry(0) := (cnt(0) + 1.U) === radix(0)
  for(i <- 1 until N) {
    carry(i) := ( (cnt(i) + 1.U) === radix(i) ) && carry(i-1)
  }

  for(i <- 0 until N) {
    cnt(i) := MuxCase(cnt(i), Seq(
      io.radix.fire -> 0.U,
      (io.cnt.fire && carry(i-1)) -> Mux(carry(i), 0.U, (cnt(i) + 1.U)),
    ))
  }
}

class cnt_varRadix_multiStep(val N: Int, WIDTH: Int, STEP: Int = 2) extends Module {
  val io = IO(new Bundle {
    val radix = Flipped(ValidIO(Vec(N, UInt(WIDTH.W))))
    val cnt = DecoupledIO(Vec(STEP, Vec(N, UInt(WIDTH.W))))
  })
  val radix = Reg(Vec(N, UInt(WIDTH.W)))
  val cnt_reg = Reg(Vec(N, UInt(WIDTH.W)))
  val cnt_res = Wire(Vec(STEP+1, Vec(N, UInt(WIDTH.W))))

  when (io.radix.valid) {
    radix := io.radix.bits
  }

  io.cnt.valid := true.B
  for(step <- 0 until STEP) {
    io.cnt.bits(step) := cnt_res(step)
  }

  val carry = Wire(Vec(STEP, Vec(N-1, Bool())))            // 每一位是否已满，加一后需要进位
  for(step <- 0 until STEP) {
    carry(step)(0) := (cnt_res(step)(0) + 1.U) === radix(0)
    for(i <- 1 until N-1) {
      carry(step)(i) := ( (cnt_res(step)(i) + 1.U) === radix(i) ) && carry(step)(i-1)
    }
  }

  cnt_res(0) := cnt_reg
  for(step <- 1 until STEP + 1; i <- 0 until N) {
    if(i > 0) {
      cnt_res(step)(i) := Mux(!carry(step-1)(i-1), cnt_res(step-1)(i),
                          Mux((cnt_res(step-1)(i) + 1.U) === radix(i), 0.U, cnt_res(step-1)(i) + 1.U))
    } else {
      cnt_res(step)(0) := Mux((cnt_res(step-1)(i) + 1.U) === radix(0), 0.U, cnt_res(step-1)(0) + 1.U)
    }
  }

  cnt_reg := MuxCase(cnt_reg, Seq(
    io.radix.fire -> 0.U.asTypeOf(cnt_reg.cloneType),
    io.cnt.fire -> cnt_res(STEP),
  ))
}

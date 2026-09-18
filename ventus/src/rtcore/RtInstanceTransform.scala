package rtcore

import chisel3._
import chisel3.util._
import hardfloat._
import top.parameters._

/** Input to the TLAS Instance boundary transform.
  *
  * The matrix is the VTAS 3x4 world-to-object matrix in row-major order.
  * Three multipliers issue one row per cycle into a four-stage dot pipeline.
  * Six rows per lane give an ideal matrix admission interval of six cycles.
  * A reserved two-entry result queue decouples matrix work from the three
  * parallel, iterative reciprocal engines; divider throughput still bounds
  * sustained lane throughput.
  */
class RtInstanceTransformInput extends Bundle {
  val laneId = UInt(log2Ceil(num_thread).W)
  val ray = new Ray(recorded_float = false)
  val matrix = Vec(12, UInt(32.W))
}

class RtInstanceTransformOutput extends Bundle {
  val laneId = UInt(log2Ceil(num_thread).W)
  val rayState = new RtRayState(recorded_float = false)
}

class RtInstanceTransform extends Module {
  private val rounding = consts.round_near_even
  private val tininess = consts.tininess_beforeRounding
  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new RtInstanceTransformInput))
    val out = Decoupled(new RtInstanceTransformOutput)
  })

  val saved = Reg(new RtInstanceTransformInput)
  val phase = RegInit(0.U(3.W))
  val issuing = RegInit(false.B)
  val issueSlot = RegInit(false.B)
  val nextSlot = RegInit(false.B)
  // Two alternating assembly slots cover overlapping tails of adjacent lanes.
  // Admission reserves queue space for the entire matrix transaction.
  val assembled = Reg(Vec(2, new RtInstanceTransformOutput))
  val matrixResults = Module(new Queue(new RtInstanceTransformOutput, 2,
    pipe = false, flow = false))
  val matrixOutstanding = RegInit(0.U(2.W))
  io.in.ready := (!issuing || phase === 5.U) && matrixOutstanding < 2.U
  when(issuing) {
    assert(phase <= 5.U)
    when(phase === 5.U) { issuing := false.B }
      .otherwise { phase := phase + 1.U }
  }
  // On the last row the old saved input is still used combinationally; this
  // edge may latch the next lane without mixing its metadata into the old tail.
  when(io.in.fire) {
    saved := io.in.bits
    issuing := true.B
    phase := 0.U
    issueSlot := nextSlot
    nextSlot := !nextSlot
    assembled(nextSlot).laneId := io.in.bits.laneId
    assembled(nextSlot).rayState.tmin := io.in.bits.ray.originalTmin
    assembled(nextSlot).rayState.tmax := io.in.bits.ray.extent
    assembled(nextSlot).rayState.inv := 0.U.asTypeOf(new Float3(recorded_float = false))
  }
  when(io.in.fire =/= matrixResults.io.deq.fire) {
    matrixOutstanding := Mux(io.in.fire, matrixOutstanding + 1.U, matrixOutstanding - 1.U)
  }
  assert(matrixOutstanding <= 2.U)

  val mul = Seq.fill(3)(Module(new MulRecFN(8, 24)))
  mul.foreach { unit =>
    unit.io.roundingMode := rounding
    unit.io.detectTininess := tininess
  }
  val add0 = Module(new AddRecFN(8, 24))
  val add1 = Module(new AddRecFN(8, 24))
  val addTranslation = Module(new AddRecFN(8, 24))
  Seq(add0, add1, addTranslation).foreach { unit =>
    unit.io.roundingMode := rounding
    unit.io.detectTininess := tininess
    unit.io.subOp := false.B
  }

  // phase 0..2 are origin rows, 3..5 direction rows.  Each multiplier is
  // reused for columns 0/1/2 of the selected row.
  // Phase 3 is binary 011, so its MSB alone is not a direction selector.
  // Use the phase boundary itself: 0..2 are origin rows, 3..5 direction.
  val directionRow = phase >= 3.U
  // phase 0..2 visits origin rows directly.  Direction starts at phase 3,
  // so it must be rebased before indexing the same three matrix rows:
  // phases 3/4/5 map to rows 0/1/2, not to the low-bit sequence 3/0/1.
  // The former accidentally indexed past this 3x4 matrix for phase 3.
  val row = Mux(directionRow, phase - 3.U, phase)(1, 0)
  val vector = Wire(Vec(3, UInt(32.W)))
  vector(0) := Mux(directionRow, saved.ray.dir.x, saved.ray.origin.x)
  vector(1) := Mux(directionRow, saved.ray.dir.y, saved.ray.origin.y)
  vector(2) := Mux(directionRow, saved.ray.dir.z, saved.ray.origin.z)
  val matrixBase = (row * 4.U)(3, 0)
  for (column <- 0 until 3) {
    mul(column).io.a := recFNFromFN(8, 24, saved.matrix((matrixBase + column.U)(3, 0)))
    mul(column).io.b := recFNFromFN(8, 24, vector(column))
  }
  // Preserve separate rounding and left-associated sums; do not introduce FMA.
  val productValid = RegNext(issuing, false.B)
  val productPhase = RegEnable(phase, issuing)
  val productSlot = RegEnable(issueSlot, issuing)
  val products = RegEnable(VecInit(mul.map(_.io.out)), issuing)
  val productTranslation = RegEnable(
    recFNFromFN(8, 24, saved.matrix((matrixBase + 3.U)(3, 0))), issuing)

  add0.io.a := products(0)
  add0.io.b := products(1)
  val sum01Valid = RegNext(productValid, false.B)
  val sum01Phase = RegEnable(productPhase, productValid)
  val sum01Slot = RegEnable(productSlot, productValid)
  val sum01 = RegEnable(add0.io.out, productValid)
  val product2 = RegEnable(products(2), productValid)
  val sum01Translation = RegEnable(productTranslation, productValid)

  add1.io.a := sum01
  add1.io.b := product2
  val sum012Valid = RegNext(sum01Valid, false.B)
  val sum012Phase = RegEnable(sum01Phase, sum01Valid)
  val sum012Slot = RegEnable(sum01Slot, sum01Valid)
  val sum012 = RegEnable(add1.io.out, sum01Valid)
  val sum012Translation = RegEnable(sum01Translation, sum01Valid)

  addTranslation.io.a := sum012
  addTranslation.io.b := sum012Translation
  val dotResult = Mux(sum012Phase >= 3.U, fNFromRecFN(8, 24, sum012),
    fNFromRecFN(8, 24, addTranslation.io.out))
  when(sum012Valid) {
    val dst = assembled(sum012Slot).rayState
    switch(sum012Phase) {
      is(0.U) { dst.origin.x := dotResult }
      is(1.U) { dst.origin.y := dotResult }
      is(2.U) { dst.origin.z := dotResult }
      is(3.U) { dst.dir.x := dotResult }
      is(4.U) { dst.dir.y := dotResult }
      is(5.U) { dst.dir.z := dotResult }
    }
  }
  matrixResults.io.enq.valid := sum012Valid && sum012Phase === 5.U
  matrixResults.io.enq.bits := assembled(sum012Slot)
  // Final row is being produced on this edge, not yet in the assembly register.
  matrixResults.io.enq.bits.rayState.dir.z := dotResult
  when(matrixResults.io.enq.valid) {
    assert(matrixResults.io.enq.ready, "Instance matrix result lost reserved capacity")
  }

  val reciprocal = Reg(Vec(3, UInt(32.W)))
  val reciprocalReturned = RegInit(0.U(3.W))
  val busy = RegInit(false.B)
  val outValid = RegInit(false.B)
  val outBits = Reg(new RtInstanceTransformOutput)
  val dividers = Seq.fill(3)(Module(new RtFpDiv))
  val dividerReady = dividers.map(_.io.in.ready).reduce(_ && _)
  matrixResults.io.deq.ready := !busy && !outValid && dividerReady
  val transformedDirection = matrixResults.io.deq.bits.rayState.dir
  dividers.zip(Seq(transformedDirection.x, transformedDirection.y,
    transformedDirection.z)).foreach { case (divider, direction) =>
    // All three engines accept the same lane on the same edge, even when
    // exceptional operands finish earlier than normal iterative divisions.
    divider.io.in.valid := matrixResults.io.deq.fire
    divider.io.in.bits.a := "h3f800000".U
    divider.io.in.bits.b := direction
    divider.io.out.ready := true.B
  }
  val returnedNow = VecInit(dividers.map(_.io.out.valid)).asUInt

  io.out.valid := outValid
  io.out.bits := outBits
  when(io.out.fire) { outValid := false.B }
  when(matrixResults.io.deq.fire) {
    outBits := matrixResults.io.deq.bits
    busy := true.B
    reciprocalReturned := 0.U
  }
  when(busy) {
    when(returnedNow.orR) {
      reciprocalReturned := reciprocalReturned | returnedNow
      for (index <- 0 until 3) when(returnedNow(index)) {
        reciprocal(index) := dividers(index).io.out.bits
      }
    }
    when((reciprocalReturned | returnedNow).andR) {
      outBits.rayState.inv.x := Mux(returnedNow(0), dividers(0).io.out.bits, reciprocal(0))
      outBits.rayState.inv.y := Mux(returnedNow(1), dividers(1).io.out.bits, reciprocal(1))
      outBits.rayState.inv.z := Mux(returnedNow(2), dividers(2).io.out.bits, reciprocal(2))
      busy := false.B
      outValid := true.B
    }
  }
}

package rtcore

import chisel3._
import chisel3.util._
import top.parameters._

/** The reason a short-lived world-ray context is being filled. */
object RtWorldRayPurpose {
  val box = 0.U(2.W)
  val instance = 1.U(2.W)
  // Direct-BLAS roots are uncommon, but retain an on-demand raw input for
  // their eventual world-space RaySetup instead of relying on eager setup.
  val triangle = 2.U(2.W)
}

class RtWorldRayPrefetch extends Bundle {
  val laneId = UInt(log2Ceil(num_thread).W)
  val purpose = UInt(2.W)
}

class RtWorldRayContextResult extends Bundle {
  val valid = Bool()
  val laneId = UInt(log2Ceil(num_thread).W)
  val purpose = UInt(2.W)
  val traceMeta0 = UInt(xLen.W)
  val ray = new Ray(recorded_float = false)
}

/**
  * Two short-lived ray contexts per fixed Box4-IU bank.
  *
  * Bank i owns exactly lanes i, i+8, i+16 and i+24.  Consequently each
  * reader has only a two-context choice and every returned ray has a fixed
  * consumer; no 32-lane ray crossbar is needed.  The module deliberately
  * exposes raw world rays: only the object-space path runs RtRaySetup.
  */
class RtWorldRayContexts extends Module {
  import RtCoreSchedulerParameters.boxUnitCount
  import RtLocalSramParameters.corePortCount

  private val contextsPerUnit = 2
  private val fieldCount = 11

  val io = IO(new Bundle {
    val clear = Input(Bool())
    // Physical Local-SRAM warp row, supplied by the owning LocalState.
    val warpId = Input(UInt(depth_warp.W))
    val prefetch = Flipped(Decoupled(new RtWorldRayPrefetch))
    val lookupLane = Input(Vec(boxUnitCount, UInt(log2Ceil(num_thread).W)))
    val result = Output(Vec(boxUnitCount, new RtWorldRayContextResult))
    val consume = Input(Vec(boxUnitCount, Bool()))
    // Direct-BLAS Triangle is the only world-space consumer that needs a
    // RaySetup result.  Give the scalar fallback a second two-context lookup
    // per bank, rather than reconstructing a 32-lane ray crossbar.
    val setupLookupLane = Input(Vec(boxUnitCount, UInt(log2Ceil(num_thread).W)))
    val setupResult = Output(Vec(boxUnitCount, new RtWorldRayContextResult))
    val setupConsume = Input(Vec(boxUnitCount, Bool()))
    val request = Vec(corePortCount, Decoupled(new RtLocalSramNarrowRequest))
    val response = Flipped(Vec(corePortCount, Decoupled(new RtLocalSramNarrowResponse)))
  })

  require(corePortCount == boxUnitCount)

  val valid = RegInit(VecInit(Seq.fill(boxUnitCount)(VecInit(Seq.fill(contextsPerUnit)(false.B)))))
  val ready = RegInit(VecInit(Seq.fill(boxUnitCount)(VecInit(Seq.fill(contextsPerUnit)(false.B)))))
  val lane = Reg(Vec(boxUnitCount, Vec(contextsPerUnit, UInt(log2Ceil(num_thread).W))))
  val purpose = Reg(Vec(boxUnitCount, Vec(contextsPerUnit, UInt(2.W))))
  val field = Reg(Vec(boxUnitCount, Vec(contextsPerUnit, UInt(4.W))))
  val origin = Reg(Vec(boxUnitCount, Vec(contextsPerUnit, new Float3(recorded_float = false))))
  val direction = Reg(Vec(boxUnitCount, Vec(contextsPerUnit, new Float3(recorded_float = false))))
  val inverse = Reg(Vec(boxUnitCount, Vec(contextsPerUnit, new Float3(recorded_float = false))))
  val tmin = Reg(Vec(boxUnitCount, Vec(contextsPerUnit, UInt(xLen.W))))
  val tmax = Reg(Vec(boxUnitCount, Vec(contextsPerUnit, UInt(xLen.W))))
  val traceMeta0 = Reg(Vec(boxUnitCount, Vec(contextsPerUnit, UInt(xLen.W))))
  val readPending = RegInit(VecInit(Seq.fill(boxUnitCount)(false.B)))
  val readContext = Reg(Vec(boxUnitCount, UInt(1.W)))

  private def fieldsFor(p: UInt): UInt = Mux(p === RtWorldRayPurpose.instance, 9.U, fieldCount.U)
  private def fieldId(p: UInt, index: UInt): UInt = {
    Mux(p === RtWorldRayPurpose.instance,
      Mux(index === 8.U, RtAbiLayout.SlotTraceMeta0.U,
        RtAbiLayout.SlotOriginX.U + index),
      RtAbiLayout.SlotOriginX.U + index)
  }

  val prefetchUnit = io.prefetch.bits.laneId(2, 0)
  val prefetchFree = VecInit((0 until contextsPerUnit).map(slot => !valid(prefetchUnit)(slot))).asUInt
  io.prefetch.ready := prefetchFree.orR && !io.clear
  when(io.prefetch.fire) {
    val slot = PriorityEncoder(prefetchFree)
    valid(prefetchUnit)(slot) := true.B
    ready(prefetchUnit)(slot) := false.B
    lane(prefetchUnit)(slot) := io.prefetch.bits.laneId
    purpose(prefetchUnit)(slot) := io.prefetch.bits.purpose
    field(prefetchUnit)(slot) := 0.U
  }

  for (unit <- 0 until boxUnitCount) {
    val filling = VecInit((0 until contextsPerUnit).map(slot =>
      valid(unit)(slot) && !ready(unit)(slot))).asUInt
    val slot = PriorityEncoder(filling)
    io.request(unit).valid := filling.orR && !readPending(unit) && !io.clear
    io.request(unit).bits.warpId := io.warpId
    io.request(unit).bits.laneId := lane(unit)(slot)
    io.request(unit).bits.fieldId := fieldId(purpose(unit)(slot), field(unit)(slot))
    io.request(unit).bits.write := false.B
    io.request(unit).bits.writeData := 0.U
    io.response(unit).ready := readPending(unit) && !io.clear
    when(io.request(unit).fire) {
      readPending(unit) := true.B
      readContext(unit) := slot
    }
    when(io.response(unit).fire) {
      val responseSlot = readContext(unit)
      val responseField = field(unit)(responseSlot)
      val responsePurpose = purpose(unit)(responseSlot)
      readPending(unit) := false.B
      when(responsePurpose === RtWorldRayPurpose.instance && responseField === 8.U) {
        traceMeta0(unit)(responseSlot) := io.response(unit).bits.readData
      }.otherwise {
        switch(responseField) {
          is(0.U) { origin(unit)(responseSlot).x := io.response(unit).bits.readData }
          is(1.U) { origin(unit)(responseSlot).y := io.response(unit).bits.readData }
          is(2.U) { origin(unit)(responseSlot).z := io.response(unit).bits.readData }
          is(3.U) { tmin(unit)(responseSlot) := io.response(unit).bits.readData }
          is(4.U) { direction(unit)(responseSlot).x := io.response(unit).bits.readData }
          is(5.U) { direction(unit)(responseSlot).y := io.response(unit).bits.readData }
          is(6.U) { direction(unit)(responseSlot).z := io.response(unit).bits.readData }
          is(7.U) { tmax(unit)(responseSlot) := io.response(unit).bits.readData }
          is(8.U) { inverse(unit)(responseSlot).x := io.response(unit).bits.readData }
          is(9.U) { inverse(unit)(responseSlot).y := io.response(unit).bits.readData }
          is(10.U) { inverse(unit)(responseSlot).z := io.response(unit).bits.readData }
        }
      }
      when(responseField === fieldsFor(responsePurpose) - 1.U) {
        ready(unit)(responseSlot) := true.B
      }.otherwise {
        field(unit)(responseSlot) := responseField + 1.U
      }
    }

    def driveResult(lookup: UInt, out: RtWorldRayContextResult): (UInt, UInt) = {
      val hits = VecInit((0 until contextsPerUnit).map(slot =>
        valid(unit)(slot) && ready(unit)(slot) && lane(unit)(slot) === lookup)).asUInt
      val hit = PriorityEncoder(hits)
      out.valid := hits.orR
      out.laneId := lane(unit)(hit)
      out.purpose := purpose(unit)(hit)
      out.traceMeta0 := traceMeta0(unit)(hit)
      out.ray.origin := origin(unit)(hit)
      out.ray.dir := direction(unit)(hit)
      out.ray.inv := inverse(unit)(hit)
      out.ray.originalTmin := tmin(unit)(hit)
      out.ray.extent := tmax(unit)(hit)
      out.ray.shear := 0.U.asTypeOf(new Float3(recorded_float = false))
      out.ray.kx := 0.U; out.ray.ky := 0.U; out.ray.kz := 0.U
      (hits, hit)
    }
    val (hits, hit) = driveResult(io.lookupLane(unit), io.result(unit))
    val (setupHits, setupHit) = driveResult(io.setupLookupLane(unit), io.setupResult(unit))
    when(io.consume(unit) && hits.orR) { valid(unit)(hit) := false.B; ready(unit)(hit) := false.B }
    when(io.setupConsume(unit) && setupHits.orR) {
      valid(unit)(setupHit) := false.B; ready(unit)(setupHit) := false.B
    }
  }

  when(io.clear) {
    for (unit <- 0 until boxUnitCount; slot <- 0 until contextsPerUnit) {
      valid(unit)(slot) := false.B
      ready(unit)(slot) := false.B
    }
    for (unit <- 0 until boxUnitCount) { readPending(unit) := false.B }
  }
}

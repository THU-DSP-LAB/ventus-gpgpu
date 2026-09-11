package rtcore

import chisel3._
import chisel3.util._
import top.parameters._

/** RTcore-owned state assembled from RT Local SRAM.  This is deliberately a
  * register buffer, not a cache/PDS table: SRAM has fixed one-cycle access
  * and all requests are field-major vectors. */
class RtLocalRayState extends Module {
  private val rayFieldCount = 15
  private val hitWords = RtAbiLayout.HitRecordWordCount
  val io = IO(new Bundle {
    val admission = Flipped(Decoupled(new RtLocalAdmission))
    // A closest-hit/miss shader may launch another trace before the warp's
    // eventual release.  It reuses the resident Local-SRAM allocation but
    // starts a fresh gather and hit-record lifetime.
    val restart = Flipped(Decoupled(new RtLocalAdmission))
    val laneOut = Decoupled(new RtLocalRayLane)
    val resume = Flipped(Decoupled(new RtLocalResume))
    val resumeOut = Decoupled(new RtLocalResumeLane)
    val raySetup = Flipped(Decoupled(new RtRaySetupOutput))
    val rayTmaxUpdate = Flipped(Decoupled(new RtLocalRayTmaxUpdate))
    val nodeRayLookup = Input(new RtLocalRayLookup)
    val nodeRay = Output(new RtLocalRayLookupResult)
    val issueRayLookup = Input(new RtLocalRayLookup)
    val issueRay = Output(new RtLocalRayLookupResult)
    val resultRayLookup = Input(new RtLocalRayLookup)
    val resultRay = Output(new RtLocalRayLookupResult)
    val hitUpdate = Flipped(Decoupled(new RtLocalHitRecordUpdate))
    val finish = Flipped(Decoupled(new RtLocalFinish))
    val drained = Decoupled(UInt(depth_warp.W))
    val retire = Flipped(Decoupled(UInt(depth_warp.W)))
    val request = Decoupled(new RtLocalSramRequest)
    val response = Flipped(Decoupled(new RtLocalSramResponse))
  })

  val valid = RegInit(false.B)
  val ownerWarp = Reg(UInt(depth_warp.W))
  val activeMask = Reg(UInt(num_thread.W))
  val rows = Reg(Vec(rayFieldCount, Vec(num_thread, UInt(xLen.W))))
  val prepared = RegInit(VecInit(Seq.fill(num_thread)(0.U.asTypeOf(new RtPreparedRay))))
  val recordData = Reg(Vec(num_thread, Vec(2, Vec(hitWords, UInt(xLen.W)))))
  val dirty = RegInit(VecInit(Seq.fill(num_thread)(VecInit(Seq.fill(2)(VecInit(Seq.fill(hitWords)(false.B)))))))
  val readField = Reg(UInt(6.W))
  val readOutstanding = RegInit(false.B)
  val loadIndex = RegInit(0.U(log2Ceil(rayFieldCount).W))
  val loaded = RegInit(false.B)
  val delivered = RegInit(0.U(num_thread.W))
  val resumeActive = RegInit(false.B)
  val resumeMask = Reg(UInt(num_thread.W))
  val resumeDelivered = RegInit(0.U(num_thread.W))
  val controlLoaded = RegInit(false.B)
  val control = Reg(Vec(num_thread, UInt(xLen.W)))
  val flushing = RegInit(false.B)
  val flushRecord = RegInit(false.B)
  val flushWord = RegInit(0.U(log2Ceil(hitWords).W))
  val drainPending = RegInit(false.B)

  private def traversalField(index: UInt): UInt = RtAbiLayout.SlotAccelLo.U + index
  private def advanceFlush(): Unit = {
    when(flushWord === (hitWords - 1).U) {
      flushWord := 0.U
      when(flushRecord) { flushing := false.B; drainPending := true.B }
        .otherwise { flushRecord := true.B }
    }.otherwise { flushWord := flushWord + 1.U }
  }

  io.admission.ready := !valid
  io.restart.ready := valid && !flushing && !readOutstanding && !resumeActive && !drainPending
  val startSession = io.admission.fire || io.restart.fire
  val startBits = Wire(new RtLocalAdmission)
  startBits := Mux(io.admission.fire, io.admission.bits, io.restart.bits)
  when(startSession) {
    valid := true.B; ownerWarp := startBits.warpId; activeMask := startBits.activeMask
    loadIndex := 0.U; loaded := false.B; delivered := 0.U; readOutstanding := false.B
    resumeActive := false.B; controlLoaded := false.B; flushing := false.B; drainPending := false.B
    for (lane <- 0 until num_thread) {
      prepared(lane).valid := false.B
      // The shader may inspect either hit-record plane after a traversal,
      // including the candidate plane after an opaque hit.  A fresh command
      // therefore must not expose the macro's power-up contents as ABI data.
      // Mark both planes dirty with zeroes; a later hitUpdate overwrites the
      // selected plane before finish flushes these field-major rows.
      for (record <- 0 until 2; word <- 0 until hitWords) {
        recordData(lane)(record)(word) := 0.U
        dirty(lane)(record)(word) := startBits.activeMask(lane)
      }
    }
  }

  // Finish owns the Local-SRAM port until both hit-record planes are flushed.
  // In particular, a stale/incomplete gather state must never override the
  // committed-record field ID in the request mux below.
  val loadRequest = valid && !flushing && !loaded && !readOutstanding
  val resumeRequest = valid && !flushing && resumeActive && !controlLoaded && !readOutstanding
  val flushMask = Wire(UInt(num_thread.W))
  flushMask := VecInit((0 until num_thread).map(lane =>
    activeMask(lane) && dirty(lane)(flushRecord)(flushWord))).asUInt
  val flushRequest = valid && flushing && flushMask.orR
  io.request.valid := loadRequest || resumeRequest || flushRequest
  io.request.bits := 0.U.asTypeOf(new RtLocalSramRequest)
  io.request.bits.warpId := ownerWarp
  io.request.bits.fieldId := Mux(loadRequest, traversalField(loadIndex),
    Mux(resumeRequest, (RtAbiLayout.LocalControlBaseBytes / RtAbiLayout.AbiWordBytes).U,
      Mux(flushRecord,
        (RtAbiLayout.LocalCommittedHitRecordBaseBytes / RtAbiLayout.AbiWordBytes).U(6.W) + flushWord,
        (RtAbiLayout.LocalCandidateHitRecordBaseBytes / RtAbiLayout.AbiWordBytes).U(6.W) + flushWord)))
  io.request.bits.write := flushRequest
  io.request.bits.laneMask := Mux(flushRequest, flushMask, 0.U)
  for (lane <- 0 until num_thread) io.request.bits.writeData(lane) := recordData(lane)(flushRecord)(flushWord)
  when(io.request.fire) {
    when(loadRequest || resumeRequest) { readOutstanding := true.B; readField := Mux(loadRequest, loadIndex, rayFieldCount.U) }
    when(flushRequest) { advanceFlush() }
  }
  when(flushing && !flushMask.orR && !flushRequest) { advanceFlush() }

  io.response.ready := readOutstanding
  when(io.response.fire) {
    readOutstanding := false.B
    when(readField === rayFieldCount.U) { control := io.response.bits.readData; controlLoaded := true.B }
      .otherwise {
        rows(readField(log2Ceil(rayFieldCount) - 1, 0)) := io.response.bits.readData
        when(readField === (rayFieldCount - 1).U) { loaded := true.B }
          .otherwise { loadIndex := readField + 1.U }
      }
  }

  val pendingLanes = activeMask & (~delivered).asUInt
  val lane = PriorityEncoder(pendingLanes)
  io.laneOut.valid := valid && loaded && !resumeActive && pendingLanes.orR
  io.laneOut.bits.laneId := lane
  io.laneOut.bits.data := VecInit((0 until rayFieldCount).map(field => rows(field)(lane)))
  when(io.laneOut.fire) { delivered := delivered | UIntToOH(lane, num_thread) }

  io.resume.ready := valid && loaded && !resumeActive && delivered === activeMask
  when(io.resume.fire) { resumeActive := true.B; controlLoaded := false.B; resumeMask := io.resume.bits.activeMask; resumeDelivered := 0.U }
  val resumePending = resumeMask & (~resumeDelivered).asUInt
  val resumeLane = PriorityEncoder(resumePending)
  io.resumeOut.valid := resumeActive && controlLoaded && resumePending.orR
  io.resumeOut.bits.laneId := resumeLane
  io.resumeOut.bits.callbackDecision := control(resumeLane)
  when(io.resumeOut.fire) {
    resumeDelivered := resumeDelivered | UIntToOH(resumeLane, num_thread)
    when((resumeDelivered | UIntToOH(resumeLane, num_thread)) === resumeMask) { resumeActive := false.B; controlLoaded := false.B; resumeDelivered := 0.U }
  }

  io.raySetup.ready := valid && activeMask(io.raySetup.bits.laneId) && !prepared(io.raySetup.bits.laneId).valid
  when(io.raySetup.fire) {
    val dst = prepared(io.raySetup.bits.laneId)
    dst.valid := true.B; dst.asBase := io.raySetup.bits.accelLo; dst.currentTmax := io.raySetup.bits.ray.extent
    dst.originalTmin := io.raySetup.bits.ray.originalTmin; dst.origin := io.raySetup.bits.ray.origin; dst.dir := io.raySetup.bits.ray.dir; dst.inv := io.raySetup.bits.ray.inv; dst.shear := io.raySetup.bits.ray.shear
    dst.kx := io.raySetup.bits.ray.kx; dst.ky := io.raySetup.bits.ray.ky; dst.kz := io.raySetup.bits.ray.kz
  }
  io.rayTmaxUpdate.ready := valid && prepared(io.rayTmaxUpdate.bits.laneId).valid
  when(io.rayTmaxUpdate.fire) { prepared(io.rayTmaxUpdate.bits.laneId).currentTmax := io.rayTmaxUpdate.bits.tmax }
  private def lookup(in: RtLocalRayLookup, out: RtLocalRayLookupResult): Unit = {
    val source = prepared(in.laneId); out.valid := source.valid; out.asBase := source.asBase; out.currentTmax := source.currentTmax
    out.ray.origin := source.origin; out.ray.dir := source.dir
    out.ray.inv := source.inv; out.ray.shear := source.shear; out.ray.originalTmin := source.originalTmin
    out.ray.extent := source.currentTmax; out.ray.kx := source.kx; out.ray.ky := source.ky; out.ray.kz := source.kz
  }
  lookup(io.nodeRayLookup, io.nodeRay); lookup(io.issueRayLookup, io.issueRay); lookup(io.resultRayLookup, io.resultRay)

  io.hitUpdate.ready := valid && !flushing && activeMask(io.hitUpdate.bits.laneId)
  when(io.hitUpdate.fire) {
    for (word <- 0 until hitWords) when(io.hitUpdate.bits.dirty(word)) {
      recordData(io.hitUpdate.bits.laneId)(io.hitUpdate.bits.committed)(word) := io.hitUpdate.bits.data(word)
      dirty(io.hitUpdate.bits.laneId)(io.hitUpdate.bits.committed)(word) := true.B
    }
  }
  io.finish.ready := valid && !flushing && io.finish.bits.terminalMask === activeMask
  when(io.finish.fire) { flushing := true.B; flushRecord := false.B; flushWord := 0.U }
  io.drained.valid := drainPending; io.drained.bits := ownerWarp
  when(io.drained.fire) { drainPending := false.B }
  io.retire.ready := valid && !flushing && !drainPending && !resumeActive
  when(io.retire.fire) { valid := false.B }
}

class RtLocalAdmission extends Bundle { val warpId = UInt(depth_warp.W); val activeMask = UInt(num_thread.W) }
class RtLocalRayLane extends Bundle { val laneId = UInt(log2Ceil(num_thread).W); val data = Vec(15, UInt(xLen.W)) }
class RtLocalResume extends Bundle { val activeMask = UInt(num_thread.W) }
class RtLocalResumeLane extends Bundle { val laneId = UInt(log2Ceil(num_thread).W); val callbackDecision = UInt(xLen.W) }
class RtLocalRayTmaxUpdate extends Bundle { val laneId = UInt(log2Ceil(num_thread).W); val tmax = UInt(32.W) }
class RtLocalRayLookup extends Bundle { val laneId = UInt(log2Ceil(num_thread).W) }
class RtPreparedRay extends Bundle { val valid = Bool(); val asBase = UInt(memReqLen.W); val currentTmax = UInt(32.W); val originalTmin = UInt(32.W); val origin = new Float3(recorded_float = false); val dir = new Float3(recorded_float = false); val inv = new Float3(recorded_float = false); val shear = new Float3(recorded_float = false); val kx = UInt(2.W); val ky = UInt(2.W); val kz = UInt(2.W) }
class RtLocalRayLookupResult extends Bundle { val valid = Bool(); val asBase = UInt(memReqLen.W); val currentTmax = UInt(32.W); val ray = new Ray(recorded_float = false) }
class RtLocalHitRecordUpdate extends Bundle { val laneId = UInt(log2Ceil(num_thread).W); val committed = Bool(); val dirty = UInt(RtAbiLayout.HitRecordWordCount.W); val data = Vec(RtAbiLayout.HitRecordWordCount, UInt(xLen.W)) }
class RtLocalFinish extends Bundle { val terminalMask = UInt(num_thread.W) }

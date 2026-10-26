package rtcore

import chisel3._
import chisel3.util._
import top.parameters._

/** RTcore-owned state assembled from RT Local SRAM.  This is deliberately a
  * register buffer, not a cache/PDS table: SRAM has fixed one-cycle access
  * and all requests are field-major vectors. */
class RtLocalRayState extends Module {
  // Coherence recording is a diagnostic elaboration only.  It reads
  // TraceMeta1 so the offline analyser can use the shader's recursion level;
  // ordinary RTL continues to fetch only AccelLo on the header path.
  private val coherenceRecordTrace = sys.env.get("VENTUS_RTCORE_RECORD_TRACE").contains("1")
  // Traversal control needs only AccelLo from the command record.  World-ray
  // payload is fetched on demand by RtWorldRayContexts; keeping a full
  // 15-word mirror here would reintroduce the duplicated state this SRAM
  // design is meant to remove.
  private val rayFieldCount = if (coherenceRecordTrace) 2 else 1
  private val hitWords = RtAbiLayout.HitRecordWordCount
  val io = IO(new Bundle {
    val admission = Flipped(Decoupled(new RtLocalAdmission))
    // A closest-hit/miss shader may launch another trace before the warp's
    // eventual release.  It reuses the resident Local-SRAM allocation but
    // starts a fresh gather and hit-record lifetime.
    val restart = Flipped(Decoupled(new RtLocalAdmission))
    val resume = Flipped(Decoupled(new RtLocalResume))
    val resumeOut = Decoupled(new RtLocalResumeLane)
    // A released physical context may resume shader-owned committed words
    // already present in persistent parent SRAM. Do not clear that plane.
    val preserveCommitted = Input(UInt(num_thread.W))
    // On-demand world-ray readers must use the same physical Local-SRAM row
    // as the header/record path; the resident scheduler slot is not a warp ID.
    val ownerWarpId = Output(UInt(depth_warp.W))
    // Mapping from a physical execution lane to its persistent RT Local SRAM
    // location. Group traversal resolves logical TIDs through scheduler-owned
    // workgroup allocation metadata; it never names PDS state.
    val targetRowBase = Output(Vec(num_thread,
      UInt(log2Ceil(RtLocalSramParameters.bankDepth).W)))
    val targetLane = Output(Vec(num_thread, UInt(log2Ceil(num_thread).W)))
    val worldBaseArrived = Output(Bool())
    val worldBaseArrivedLane = Output(UInt(log2Ceil(num_thread).W))
    // Several fixed SRAM banks may return an AccelLo word together.  Keep the
    // complete set so the scheduler cannot strand every lane but the first.
    val worldBaseArrivedMask = Output(UInt(num_thread.W))
    // Trace-only per-lane recursion metadata.  This port does not exist in
    // normal elaborations, so coherence observation does not affect the
    // functional model's area or timing.
    val coherenceTraceMeta1 = if (coherenceRecordTrace)
      Some(Output(Vec(num_thread, UInt(xLen.W)))) else None
    val worldBaseLookup = Input(new RtLocalRayLookup)
    val worldBase = Output(new RtLocalWorldBaseLookupResult)
    val hitUpdate = Flipped(Decoupled(new RtLocalHitRecordUpdate))
    val finish = Flipped(Decoupled(new RtLocalFinish))
    val drained = Decoupled(UInt(depth_warp.W))
    val retire = Flipped(Decoupled(UInt(depth_warp.W)))
    val request = Decoupled(new RtLocalSramRequest)
    val response = Flipped(Decoupled(new RtLocalSramResponse))
    val narrowRequest = Vec(RtLocalSramParameters.corePortCount,
      Decoupled(new RtLocalSramNarrowRequest))
    val narrowResponse = Flipped(Vec(RtLocalSramParameters.corePortCount,
      Decoupled(new RtLocalSramNarrowResponse)))
    val detailed = if (PMU_DETAILED) Some(Output(new RtRayLoadObservation)) else None
  })

  val valid = RegInit(false.B)
  // Keep session-control state deterministic across reset.  The ray and hit
  // record payload arrays intentionally remain resetless and are protected by
  // `valid`/`loaded`/`resumeActive`; resetting those arrays would create a
  // disproportionate reset network.
  val ownerWarp = RegInit(0.U(depth_warp.W))
  // Physical scheduler lanes are allowed to execute a ray whose persistent
  // RT Local storage belongs to another warp/lane.  Keep that mapping here;
  // later narrow reads select SRAM banks from targetLane, while all transient
  // scheduler state remains indexed by the physical lane.
  val targetRowBase = Reg(Vec(num_thread,
    UInt(log2Ceil(RtLocalSramParameters.bankDepth).W)))
  val targetLane = Reg(Vec(num_thread, UInt(log2Ceil(num_thread).W)))
  io.ownerWarpId := ownerWarp
  io.targetRowBase := targetRowBase
  io.targetLane := targetLane
  // Session ownership survives nested traces so final hit-record flush and
  // release still cover the original warp.  A restart may gather only the
  // divergent lanes which actually issued the child trace.
  val activeMask = RegInit(0.U(num_thread.W))
  val gatherMask = RegInit(0.U(num_thread.W))
  // A bounded fetch slot owns one lane while it reads its AccelLo header word
  // from the matching Local-SRAM bank.  All remaining world-ray fields stay
  // in SRAM until their fixed IU bank needs them.
  val fetchStarted = RegInit(0.U(num_thread.W))
  val fetchActive = RegInit(VecInit(Seq.fill(RtLocalSramParameters.corePortCount)(false.B)))
  val fetchReadOutstanding = RegInit(VecInit(Seq.fill(RtLocalSramParameters.corePortCount)(false.B)))
  val fetchLane = Reg(Vec(RtLocalSramParameters.corePortCount, UInt(log2Ceil(num_thread).W)))
  val fetchField = Reg(Vec(RtLocalSramParameters.corePortCount, UInt(log2Ceil(rayFieldCount).W)))
  // This is scheduler control state: retaining the AS base avoids a fresh
  // SRAM read on every stack pop.
  val worldBaseData = Reg(Vec(num_thread, UInt(memReqLen.W)))
  val worldBaseValid = RegInit(0.U(num_thread.W))
  val traceMeta1 = if (coherenceRecordTrace) Some(Reg(Vec(num_thread, UInt(xLen.W)))) else None
  // The last command-header response and worldBaseArrived occur together.
  // Record trace needs the new word in that cycle, rather than the prior
  // register contents which are only visible after the clock edge.
  val traceMeta1Observed = if (coherenceRecordTrace) Some(Wire(Vec(num_thread, UInt(xLen.W)))) else None
  if (coherenceRecordTrace) traceMeta1Observed.get := traceMeta1.get
  // Hit records write through to Local SRAM.  Keep only per-word ownership
  // bits so finish can clear untouched words; retaining both 9-word planes
  // for every lane here would duplicate 2.25 KiB of the SRAM payload.
  val recordWordWritten = RegInit(VecInit(Seq.fill(num_thread)(VecInit(Seq.fill(2)(VecInit(Seq.fill(hitWords)(false.B)))))))
  val hitWriteQueue = Module(new Queue(new RtLocalHitRecordUpdate, 4))
  val recordWriteActive = RegInit(false.B)
  val recordWriteLane = Reg(UInt(log2Ceil(num_thread).W))
  val recordWriteCommitted = Reg(Bool())
  val recordWriteMask = RegInit(0.U(hitWords.W))
  val recordWriteData = Reg(Vec(hitWords, UInt(xLen.W)))
  val readField = RegInit(0.U(6.W))
  val readOutstanding = RegInit(false.B)
  val loaded = RegInit(false.B)
  val resumeActive = RegInit(false.B)
  val resumeMask = RegInit(0.U(num_thread.W))
  val resumeDelivered = RegInit(0.U(num_thread.W))
  val controlLoaded = RegInit(false.B)
  val control = Reg(Vec(num_thread, UInt(xLen.W)))
  val resumeTLoaded = RegInit(false.B)
  val resumeHitT = Reg(Vec(num_thread, UInt(xLen.W)))
  val flushing = RegInit(false.B)
  val flushRecord = RegInit(false.B)
  val flushWord = RegInit(0.U(log2Ceil(hitWords).W))
  val drainPending = RegInit(false.B)

  for (port <- 0 until RtLocalSramParameters.corePortCount) {
    io.narrowRequest(port).valid := false.B
    io.narrowRequest(port).bits := 0.U.asTypeOf(new RtLocalSramNarrowRequest)
    io.narrowResponse(port).ready := true.B
  }

  private def traversalField(index: UInt): UInt = {
    if (coherenceRecordTrace)
      Mux(index === 0.U, RtAbiLayout.SlotAccelLo.U, RtAbiLayout.SlotTraceMeta1.U)
    else RtAbiLayout.SlotAccelLo.U
  }
  private def advanceFlush(): Unit = {
    when(flushWord === (hitWords - 1).U) {
      flushWord := 0.U
      when(flushRecord) { flushing := false.B; drainPending := true.B }
        .otherwise { flushRecord := true.B }
    }.otherwise { flushWord := flushWord + 1.U }
  }

  io.admission.ready := !valid
  io.restart.ready := valid && io.restart.bits.warpId === ownerWarp &&
    (io.restart.bits.activeMask & ~activeMask) === 0.U &&
    !flushing && !readOutstanding && !resumeActive && !drainPending
  val startSession = io.admission.fire || io.restart.fire
  val startBits = Wire(new RtLocalAdmission)
  startBits := Mux(io.admission.fire, io.admission.bits, io.restart.bits)
  when(startSession) {
    valid := true.B; gatherMask := startBits.activeMask
    when(io.admission.fire) { ownerWarp := startBits.warpId; activeMask := startBits.activeMask }
    when(io.admission.fire) {
      for (lane <- 0 until num_thread) {
        // Group locator is a software-supplied workgroup-local cross-warp
        // TID. Its high bits name a logical member; the scheduler-provided
        // table translates that member to the physical RT Local SRAM row.
        val groupMember = startBits.locator(lane)(
          log2Ceil(num_thread) + log2Ceil(num_warp_in_a_block) - 1,
          log2Ceil(num_thread))
        val groupRowBase = startBits.memberWarp(groupMember) *
          RtLocalSramParameters.fieldCount.U
        targetRowBase(lane) := Mux(startBits.grouped,
          groupRowBase,
          startBits.warpId * RtLocalSramParameters.fieldCount.U)
        targetLane(lane) := Mux(startBits.grouped,
          startBits.locator(lane)(log2Ceil(num_thread) - 1, 0), lane.U)
      }
      if (sys.env.get("VENTUS_RTCORE_TRACE").contains("1")) {
        printf(p"[RTLOCAL trace] admit owner=${Decimal(startBits.warpId)}" +
          p" grouped=${Decimal(startBits.grouped)}" +
          p" locator0=0x${Hexadecimal(startBits.locator(0))}" +
          p" locator31=0x${Hexadecimal(startBits.locator(num_thread - 1))}" +
          p" member0=${Decimal(startBits.locator(0)(log2Ceil(num_thread) + log2Ceil(num_warp_in_a_block) - 1, log2Ceil(num_thread)))}" +
          p" member31=${Decimal(startBits.locator(num_thread - 1)(log2Ceil(num_thread) + log2Ceil(num_warp_in_a_block) - 1, log2Ceil(num_thread)))}\n")
      }
    }
    loaded := false.B
    fetchStarted := 0.U; worldBaseValid := 0.U
    for (port <- 0 until RtLocalSramParameters.corePortCount) {
      fetchActive(port) := false.B
      fetchReadOutstanding(port) := false.B; fetchField(port) := 0.U
    }
    readOutstanding := false.B
    resumeActive := false.B; controlLoaded := false.B; resumeTLoaded := false.B
    flushing := false.B; drainPending := false.B
    for (lane <- 0 until num_thread) {
      for (record <- 0 until 2; word <- 0 until hitWords) {
        // A new trace must not expose either prior shader or hardware data at
        // completion.  Actual zero writes are delayed until finish so they do
        // not contend with initial ray-field fetches.
        recordWordWritten(lane)(record)(word) := false.B
      }
    }
  }

  // Start at most one physical execution lane per *target* SRAM bank.  The
  // response remains tagged with fetchLane, so a group permutation may read
  // arbitrary original RT Local slots without changing scheduler state IDs.
  val fetchStartMask = Wire(Vec(RtLocalSramParameters.corePortCount, UInt(num_thread.W)))
  val fetchRequest = Wire(Vec(RtLocalSramParameters.corePortCount, Bool()))
  for (port <- 0 until RtLocalSramParameters.corePortCount) {
    val candidates = VecInit((0 until num_thread).map(lane =>
      gatherMask(lane) && !fetchStarted(lane) &&
        targetLane(lane)(log2Ceil(RtLocalSramParameters.bankCount) - 1, 0) === port.U)).asUInt
    val start = valid && !flushing && !resumeActive &&
      !fetchActive(port) && !fetchReadOutstanding(port) && candidates.orR
    val lane = PriorityEncoder(candidates)
    fetchStartMask(port) := Mux(start, UIntToOH(lane, num_thread), 0.U)
    when(start) {
      fetchActive(port) := true.B
      fetchLane(port) := lane
      fetchField(port) := 0.U
    }
    fetchRequest(port) := fetchActive(port) && !fetchReadOutstanding(port)
  }
  when(fetchStartMask.map(_.orR).reduce(_ || _)) { fetchStarted := fetchStarted | fetchStartMask.reduce(_ | _) }

  // The wide port remains for shader-compatible record clearing and callback
  // reads. Initial ray gathering itself is entirely narrow now.
  // The shader owns callback commits. Only ACCEPT consumes committed HitT;
  // IGNORE/TERMINATE retain the current bound, even if committed is valid.
  val resumeNeedsT = VecInit((0 until num_thread).map(lane =>
    resumeMask(lane) && control(lane) === RtAbiLayout.CallbackAccept.U)).asUInt.orR
  val resumeRequest = valid && !flushing && resumeActive && !readOutstanding &&
    (!controlLoaded || (resumeNeedsT && !resumeTLoaded))
  hitWriteQueue.io.enq.valid := io.hitUpdate.valid && valid && !flushing &&
    activeMask(io.hitUpdate.bits.laneId)
  hitWriteQueue.io.enq.bits := io.hitUpdate.bits
  io.hitUpdate.ready := valid && !flushing && activeMask(io.hitUpdate.bits.laneId) &&
    hitWriteQueue.io.enq.ready
  hitWriteQueue.io.deq.ready := !recordWriteActive
  when(hitWriteQueue.io.deq.fire) {
    recordWriteActive := true.B
    recordWriteLane := hitWriteQueue.io.deq.bits.laneId
    recordWriteCommitted := hitWriteQueue.io.deq.bits.committed
    recordWriteMask := hitWriteQueue.io.deq.bits.dirty
    recordWriteData := hitWriteQueue.io.deq.bits.data
  }

  val hitWritesDrained = !recordWriteActive && !hitWriteQueue.io.deq.valid
  val flushMask = Wire(UInt(num_thread.W))
  flushMask := VecInit((0 until num_thread).map(lane =>
    activeMask(lane) && !recordWordWritten(lane)(flushRecord)(flushWord))).asUInt
  val flushScatterRequest = valid && flushing && hitWritesDrained && flushMask.orR
  val flushScatterLane = PriorityEncoder(flushMask)
  val flushScatterPort = targetLane(flushScatterLane)(log2Ceil(RtLocalSramParameters.bankCount) - 1, 0)
  val recordWriteWord = PriorityEncoder(recordWriteMask)
  val recordWriteRequest = valid && recordWriteActive && recordWriteMask.orR
  val recordWritePort = targetLane(recordWriteLane)(log2Ceil(RtLocalSramParameters.bankCount) - 1, 0)
  for (port <- 0 until RtLocalSramParameters.corePortCount) {
    val recordRequest = recordWriteRequest && recordWritePort === port.U && !fetchRequest(port)
    val flushRequest = flushScatterRequest && flushScatterPort === port.U && !fetchRequest(port) && !recordRequest
    io.narrowRequest(port).valid := fetchRequest(port) || recordRequest || flushRequest
    io.narrowRequest(port).bits.warpId := ownerWarp
    val narrowPhysicalLane = Mux(fetchRequest(port), fetchLane(port),
      Mux(flushRequest, flushScatterLane, recordWriteLane))
    io.narrowRequest(port).bits.rowBase := targetRowBase(narrowPhysicalLane)
    io.narrowRequest(port).bits.laneId := targetLane(narrowPhysicalLane)
    io.narrowRequest(port).bits.fieldId := Mux(fetchRequest(port),
      traversalField(fetchField(port)), Mux(flushRequest,
        Mux(flushRecord,
          (RtAbiLayout.LocalCommittedHitRecordBaseBytes / RtAbiLayout.AbiWordBytes).U(6.W) + flushWord,
          (RtAbiLayout.LocalCandidateHitRecordBaseBytes / RtAbiLayout.AbiWordBytes).U(6.W) + flushWord), Mux(recordWriteCommitted,
        (RtAbiLayout.LocalCommittedHitRecordBaseBytes / RtAbiLayout.AbiWordBytes).U(6.W) + recordWriteWord,
        (RtAbiLayout.LocalCandidateHitRecordBaseBytes / RtAbiLayout.AbiWordBytes).U(6.W) + recordWriteWord)))
    io.narrowRequest(port).bits.write := !fetchRequest(port)
    io.narrowRequest(port).bits.writeData := Mux(fetchRequest(port), 0.U,
      Mux(flushRequest, 0.U, recordWriteData(recordWriteWord)))
    io.narrowResponse(port).ready := fetchReadOutstanding(port)
    when(io.narrowRequest(port).fire && fetchRequest(port)) {
      fetchReadOutstanding(port) := true.B
    }
    when(io.narrowResponse(port).fire) {
      fetchReadOutstanding(port) := false.B
      if (coherenceRecordTrace) {
        when(fetchField(port) === 1.U) {
          traceMeta1.get(fetchLane(port)) := io.narrowResponse(port).bits.readData
          traceMeta1Observed.get(fetchLane(port)) := io.narrowResponse(port).bits.readData
        }
      }
      when(fetchField(port) === (rayFieldCount - 1).U) {
        fetchActive(port) := false.B
      }.otherwise {
        fetchField(port) := fetchField(port) + 1.U
      }
    }
  }
  val baseArrival = VecInit((0 until RtLocalSramParameters.corePortCount).map(port =>
    io.narrowResponse(port).fire && fetchField(port) === 0.U)).asUInt
  // In the record-only build arrival waits for TraceMeta1 as well, ensuring a
  // logged ray is labelled with its correct recursion level before traversal.
  val worldBaseArrival = VecInit((0 until RtLocalSramParameters.corePortCount).map(port =>
    io.narrowResponse(port).fire && fetchField(port) === (rayFieldCount - 1).U)).asUInt
  val worldBaseArrivalMask = (0 until RtLocalSramParameters.corePortCount).map(port =>
    Mux(worldBaseArrival(port), UIntToOH(fetchLane(port), num_thread), 0.U)).reduce(_ | _)
  when(worldBaseArrival.orR) { worldBaseValid := worldBaseValid | worldBaseArrivalMask }
  for (lane <- 0 until num_thread) {
    val baseWriters = (0 until RtLocalSramParameters.corePortCount).map(port =>
      baseArrival(port) && fetchLane(port) === lane.U)
    when(baseWriters.reduce(_ || _)) {
      worldBaseData(lane) := Mux1H(baseWriters.zipWithIndex.map { case (write, port) =>
        write -> io.narrowResponse(port).bits.readData
      })
    }
  }
  // Words which did not receive a hardware update in this trace are zeroed
  // before completion.  Once written (by either path), mark ownership so a
  // later completion cannot overwrite shader-side changes without a restart.
  io.request.valid := resumeRequest
  io.request.bits := 0.U.asTypeOf(new RtLocalSramRequest)
  io.request.bits.warpId := ownerWarp
  io.request.bits.fieldId := Mux(resumeRequest, Mux(controlLoaded,
      (RtAbiLayout.LocalCommittedHitRecordBaseBytes / RtAbiLayout.AbiWordBytes + RtAbiLayout.HitRecordHitT).U,
      (RtAbiLayout.LocalControlBaseBytes / RtAbiLayout.AbiWordBytes).U),
      Mux(flushRecord,
        (RtAbiLayout.LocalCommittedHitRecordBaseBytes / RtAbiLayout.AbiWordBytes).U(6.W) + flushWord,
        (RtAbiLayout.LocalCandidateHitRecordBaseBytes / RtAbiLayout.AbiWordBytes).U(6.W) + flushWord))
  io.request.bits.write := false.B
  io.request.bits.laneMask := 0.U
  for (lane <- 0 until num_thread) {
    io.request.bits.writeData(lane) := 0.U
  }
  when(io.request.fire && resumeRequest) {
      readOutstanding := true.B
      readField := Mux(controlLoaded, (rayFieldCount + 1).U, rayFieldCount.U)
  }
  // This bank may instead accept its higher-priority header gather. Advance
  // only the write actually selected, never an unrelated read on that port.
  when(recordWriteRequest && !fetchRequest(recordWritePort) &&
      io.narrowRequest(recordWritePort).fire) {
      recordWriteMask := recordWriteMask & ~UIntToOH(recordWriteWord, hitWords)
      when((recordWriteMask & ~UIntToOH(recordWriteWord, hitWords)) === 0.U) {
        recordWriteActive := false.B
      }
  }
  when(flushScatterRequest && !fetchRequest(flushScatterPort) &&
      !(recordWriteRequest && recordWritePort === flushScatterPort) &&
      io.narrowRequest(flushScatterPort).fire) {
    recordWordWritten(flushScatterLane)(flushRecord)(flushWord) := true.B
    when((flushMask & ~UIntToOH(flushScatterLane, num_thread)) === 0.U) { advanceFlush() }
  }
  when(flushing && hitWritesDrained && !flushMask.orR) { advanceFlush() }

  io.response.ready := readOutstanding
  when(io.response.fire) {
    readOutstanding := false.B
    when(readField === rayFieldCount.U) { control := io.response.bits.readData; controlLoaded := true.B }
      .elsewhen(readField === (rayFieldCount + 1).U) { resumeHitT := io.response.bits.readData; resumeTLoaded := true.B }
  }

  when(worldBaseArrival.orR && (worldBaseValid | worldBaseArrivalMask) === gatherMask) {
    loaded := true.B
  }

  io.resume.ready := valid && loaded && !resumeActive
  when(io.resume.fire) {
    resumeActive := true.B; controlLoaded := false.B; resumeTLoaded := false.B
    resumeMask := io.resume.bits.activeMask; resumeDelivered := 0.U
  }
  val resumePending = resumeMask & (~resumeDelivered).asUInt
  val resumeLane = PriorityEncoder(resumePending)
  io.resumeOut.valid := resumeActive && controlLoaded && (!resumeNeedsT || resumeTLoaded) && resumePending.orR
  io.resumeOut.bits.laneId := resumeLane
  io.resumeOut.bits.callbackDecision := control(resumeLane)
  when(io.resumeOut.fire) {
    resumeDelivered := resumeDelivered | UIntToOH(resumeLane, num_thread)
    when((resumeDelivered | UIntToOH(resumeLane, num_thread)) === resumeMask) { resumeActive := false.B; controlLoaded := false.B; resumeDelivered := 0.U }
  }

  if (PMU_DETAILED) {
    val d = io.detailed.get
    val gatherReadRequest = fetchRequest.zipWithIndex.map { case (request, port) =>
      request && io.narrowRequest(port).fire
    }.reduce(_ || _)
    val gatherReadResponse = io.narrowResponse.zipWithIndex.map { case (response, port) =>
      response.fire
    }.reduce(_ || _)
    val gatherResponsePort = PriorityEncoder(VecInit(io.narrowResponse.map(_.fire)).asUInt)
    d.gatherStart := startSession
    d.gatherReadRequest := gatherReadRequest
    d.gatherReadResponse := gatherReadResponse
    d.gatherField := Mux(gatherReadResponse, fetchField(gatherResponsePort), 0.U)
    d.gatherDone := gatherReadResponse && fetchField(gatherResponsePort) === (rayFieldCount - 1).U
    d.laneOut := false.B
    d.laneOutLane := 0.U
    d.setupWrite := false.B
    d.setupWriteLane := 0.U
  }
  io.worldBaseArrived := worldBaseArrival.orR
  io.worldBaseArrivedLane := fetchLane(PriorityEncoder(worldBaseArrival))
  io.worldBaseArrivedMask := worldBaseArrivalMask
  io.worldBase.valid := valid && worldBaseValid(io.worldBaseLookup.laneId) &&
    gatherMask(io.worldBaseLookup.laneId)
  io.worldBase.asBase := worldBaseData(io.worldBaseLookup.laneId)
  if (coherenceRecordTrace) io.coherenceTraceMeta1.get := traceMeta1Observed.get

  when(io.hitUpdate.fire) {
    for (word <- 0 until hitWords) when(io.hitUpdate.bits.dirty(word)) {
      recordWordWritten(io.hitUpdate.bits.laneId)(io.hitUpdate.bits.committed)(word) := true.B
    }
  }
  for (lane <- 0 until num_thread) when(io.preserveCommitted(lane)) {
    for (word <- 0 until hitWords) recordWordWritten(lane)(1)(word) := true.B
  }
  io.finish.ready := valid && !flushing && io.finish.bits.terminalMask === activeMask
  when(io.finish.fire) { flushing := true.B; flushRecord := false.B; flushWord := 0.U }
  io.drained.valid := drainPending; io.drained.bits := ownerWarp
  when(io.drained.fire) { drainPending := false.B }
  io.retire.ready := valid && !flushing && !drainPending && !resumeActive
  when(io.retire.fire) { valid := false.B }
}

class RtLocalAdmission extends Bundle {
  val warpId = UInt(depth_warp.W)
  val activeMask = UInt(num_thread.W)
  val grouped = Bool()
  val locator = Vec(num_thread, UInt(xLen.W))
  val memberWarp = Vec(num_warp_in_a_block, UInt(depth_warp.W))
}
class RtLocalResume extends Bundle { val activeMask = UInt(num_thread.W) }
class RtLocalResumeLane extends Bundle { val laneId = UInt(log2Ceil(num_thread).W); val callbackDecision = UInt(xLen.W) }
class RtLocalRayLookup extends Bundle { val laneId = UInt(log2Ceil(num_thread).W) }
class RtPreparedRay extends Bundle { val valid = Bool(); val currentTmax = UInt(32.W); val originalTmin = UInt(32.W); val origin = new Float3(recorded_float = false); val dir = new Float3(recorded_float = false); val inv = new Float3(recorded_float = false); val shear = new Float3(recorded_float = false); val kx = UInt(2.W); val ky = UInt(2.W); val kz = UInt(2.W) }
class RtLocalWorldBaseLookupResult extends Bundle { val valid = Bool(); val asBase = UInt(memReqLen.W) }
class RtLocalHitRecordUpdate extends Bundle { val laneId = UInt(log2Ceil(num_thread).W); val committed = Bool(); val dirty = UInt(RtAbiLayout.HitRecordWordCount.W); val data = Vec(RtAbiLayout.HitRecordWordCount, UInt(xLen.W)) }
class RtLocalFinish extends Bundle { val terminalMask = UInt(num_thread.W) }

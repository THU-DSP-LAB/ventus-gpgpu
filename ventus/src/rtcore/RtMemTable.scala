/*
 * RTcore PDS and AS transaction-table state.
 *
 * PDS keeps ray state in lane-indexed 64-byte rows. AS retains its independent
 * cache-line transaction metadata below; its controller is deliberately not
 * part of this file yet.
 */
package rtcore

import chisel3._
import chisel3.util._
import top.parameters._

object RtMemTableParameters {
  val pdsRowCount = num_thread
  val pdsRowIndexWidth = log2Ceil(pdsRowCount)
  val pdsRowWords = RtCorePortParameters.pdsWriteWordCount
  val pdsRayStateFieldCount = 12
  val pdsReadTrackerCount = 32
  val pdsWorkContextCount = 32
  val pdsWarpContextCount = num_warp

  // The resident traversal tables are warp-owned.  They deliberately do not
  // borrow rows from the transient cache-line transaction pool.
  val pdsReadWarpEntryCount = num_warp
  val pdsWriteWarpEntryCount = num_warp
  val hitRecordStorageWordCount = RtAbiLayout.HitRecordWordCount

  val asEntryCount = 32
  val maxAsWaitersPerEntry = 2
  val lineWordCount = dcache_BlockWords
  val lineBytes = lineWordCount * (xLen / 8)

  require(pdsRowCount == num_thread)
  require(pdsRowWords == 16)
  require(pdsRayStateFieldCount <= pdsRowWords)
  require(lineWordCount == num_thread)
  require(
    RtAbiLayout.AbiCandidateHitRecordSizeBytes / RtAbiLayout.AbiWordBytes ==
      hitRecordStorageWordCount
  )
  require(
    RtAbiLayout.AbiCommittedHitRecordSizeBytes / RtAbiLayout.AbiWordBytes ==
      hitRecordStorageWordCount
  )
  require(
    RtAbiLayout.AbiCandidateHitRecordBaseBytes / RtAbiLayout.AbiWordBytes +
      hitRecordStorageWordCount ==
      RtAbiLayout.AbiCommittedHitRecordBaseBytes / RtAbiLayout.AbiWordBytes
  )
  require(
    RtAbiLayout.AbiCommittedHitRecordBaseBytes / RtAbiLayout.AbiWordBytes +
      hitRecordStorageWordCount == RtAbiLayout.AbiPdsFieldMajorFixedHeaderWordCount
  )
}

object RtPdsRowState {
  val width = 3
  val free = 0.U(width.W)
  val rayState = 1.U(width.W)
  val readyToIssue = 2.U(width.W)
  val hitRecordWrite = 3.U(width.W)
  val faultPending = 4.U(width.W)
}

/** One 64-byte PDS row, fixed to row[laneId] while it holds ray state. */
class RtPdsRow extends Bundle {
  val state = UInt(RtPdsRowState.width.W)
  val workId = UInt(RtCorePortParameters.requestIdWidth.W)
  val receivedFields = UInt(RtMemTableParameters.pdsRayStateFieldCount.W)
  val data = Vec(RtMemTableParameters.pdsRowWords, UInt(xLen.W))
  val fault = Bool()

  val writeBlockAddress = UInt(memReqLen.W)
  val writeWordMask = UInt(RtMemTableParameters.pdsRowWords.W)
}

/** Tracks one admitted sparse ray-state work item. */
class RtPdsWorkContext extends Bundle {
  val valid = Bool()
  val workId = UInt(RtCorePortParameters.requestIdWidth.W)
  val warpId = UInt(depth_warp.W)
  val liveLaneMask = UInt(num_thread.W)
  val issuedFields = UInt(RtMemTableParameters.pdsRayStateFieldCount.W)
}

/** Metadata only: a field-major PDS read never occupies a 64-byte row. */
class RtPdsReadTracker extends Bundle {
  val valid = Bool()
  val requestId = UInt(RtCorePortParameters.requestIdWidth.W)
  val workId = UInt(RtCorePortParameters.requestIdWidth.W)
  val fieldIndex = UInt(log2Ceil(RtMemTableParameters.pdsRayStateFieldCount).W)
  val laneMask = UInt(num_thread.W)
}

/** Admission for one sparse, field-major ray-state fetch. */
class RtPdsRayStateAdmission extends Bundle {
  val workId = UInt(RtCorePortParameters.requestIdWidth.W)
  val warpId = UInt(depth_warp.W)
  val pds = new RtPdsContext
  val activeMask = UInt(num_thread.W)
}

/** A complete 12-word ray-state row supplied to later IU input preparation. */
class RtPdsRayStateRow extends Bundle {
  val workId = UInt(RtCorePortParameters.requestIdWidth.W)
  val laneId = UInt(RtMemTableParameters.pdsRowIndexWidth.W)
  val data = Vec(
    RtMemTableParameters.pdsRayStateFieldCount,
    UInt(xLen.W)
  )
}

/** Terminal PDS read fault for one lane; it must not be sent to an IU. */
class RtPdsRayStateFault extends Bundle {
  val workId = UInt(RtCorePortParameters.requestIdWidth.W)
  val laneId = UInt(RtMemTableParameters.pdsRowIndexWidth.W)
}

/** Mutable PDS-only state owned by RtPdsTable. */
class RtPdsTableStorage extends Bundle {
  val rows = Vec(RtMemTableParameters.pdsRowCount, new RtPdsRow)
  val warpContexts = Vec(
    RtMemTableParameters.pdsWarpContextCount,
    new RtPdsContext
  )
  val workContexts = Vec(
    RtMemTableParameters.pdsWorkContextCount,
    new RtPdsWorkContext
  )
  val readTrackers = Vec(
    RtMemTableParameters.pdsReadTrackerCount,
    new RtPdsReadTracker
  )
  val nextWriteRow = UInt(RtMemTableParameters.pdsRowIndexWidth.W)
}

/** One logical AS fetch waiting for a coalesced cache-line response. */
class RtAsLineWaiter extends Bundle {
  val requestId = UInt(RtCorePortParameters.requestIdWidth.W)
  val wordMask = UInt(RtMemTableParameters.lineWordCount.W)
}

/** One coalesced AS cache-line transaction. */
class RtAsMemTableEntry extends Bundle {
  val valid = Bool()
  val cacheRequestSent = Bool()
  val cacheResponseValid = Bool()

  val lineAddress = UInt(memReqLen.W)
  val cacheWordMask = UInt(RtMemTableParameters.lineWordCount.W)

  val waiterValid = Vec(RtMemTableParameters.maxAsWaitersPerEntry, Bool())
  val waiters = Vec(
    RtMemTableParameters.maxAsWaitersPerEntry,
    new RtAsLineWaiter
  )

  val cacheLineData = Vec(
    RtMemTableParameters.lineWordCount,
    UInt(xLen.W)
  )
  val responseFault = Bool()
}

/** Complete structural storage used by the legacy PDS table controller. */
class RtMemTableStorage extends Bundle {
  val pds = new RtPdsTableStorage
  val asEntries = Vec(
    RtMemTableParameters.asEntryCount,
    new RtAsMemTableEntry
  )
}

/**
 * PDS row-table controller.
 *
 * Ray-state reads are transposed directly into row[laneId]. Hit-record writes
 * reuse any free row as a 64-byte buffer. The only shared external resource is
 * the PDS request stream, where reads and writes arbitrate fairly.
 */
class RtPdsTable extends Module {
  private val params = RtMemTableParameters
  private val allRayStateFields =
    ((BigInt(1) << params.pdsRayStateFieldCount) - 1).U(
      params.pdsRayStateFieldCount.W
    )

  val io = IO(new Bundle {
    val rayStateAdmission = Flipped(Decoupled(new RtPdsRayStateAdmission))
    val rayStateOut = Decoupled(new RtPdsRayStateRow)
    val faultOut = Decoupled(new RtPdsRayStateFault)
    val hitRecordWrite = Flipped(Decoupled(new RtPreparedHitRecordWrite))

    val pdsRequest = Decoupled(new RtPdsRequest)
    val pdsResponse = Flipped(Decoupled(new RtPdsResponse))
  })

  val table = RegInit(0.U.asTypeOf(new RtPdsTableStorage))
  val preferWrite = RegInit(true.B)

  val workFree = Wire(Vec(params.pdsWorkContextCount, Bool()))
  val workNeedsRead = Wire(Vec(params.pdsWorkContextCount, Bool()))
  for (index <- 0 until params.pdsWorkContextCount) {
    workFree(index) := !table.workContexts(index).valid
    workNeedsRead(index) := table.workContexts(index).valid &&
      table.workContexts(index).issuedFields =/= allRayStateFields
  }
  val hasFreeWork = workFree.asUInt.orR
  val hasReadWork = workNeedsRead.asUInt.orR
  val selectedFreeWork = PriorityEncoder(workFree.asUInt)
  val selectedReadWork = PriorityEncoder(workNeedsRead.asUInt)

  val warpHasLiveWork = Wire(Vec(params.pdsWarpContextCount, Bool()))
  for (warp <- 0 until params.pdsWarpContextCount) {
    warpHasLiveWork(warp) := table.workContexts.map { work =>
      work.valid && work.warpId === warp.U
    }.reduce(_ || _)
  }
  val admissionWarpContext = table.warpContexts(
    io.rayStateAdmission.bits.warpId
  )
  val admissionContextMatches =
    admissionWarpContext.baseAddress === io.rayStateAdmission.bits.pds.baseAddress &&
      admissionWarpContext.numWarps === io.rayStateAdmission.bits.pds.numWarps &&
      admissionWarpContext.numThreads === io.rayStateAdmission.bits.pds.numThreads &&
      admissionWarpContext.pdsWarpTidBase === io.rayStateAdmission.bits.pds.pdsWarpTidBase
  val admissionContextAvailable =
    !warpHasLiveWork(io.rayStateAdmission.bits.warpId) || admissionContextMatches

  val admissionRowsFree = Wire(Vec(params.pdsRowCount, Bool()))
  for (lane <- 0 until params.pdsRowCount) {
    admissionRowsFree(lane) := !io.rayStateAdmission.bits.activeMask(lane) ||
      table.rows(lane).state === RtPdsRowState.free
  }
  val nonEmptyAdmission = io.rayStateAdmission.bits.activeMask.orR
  io.rayStateAdmission.ready := !nonEmptyAdmission ||
    (hasFreeWork && admissionRowsFree.asUInt.andR && admissionContextAvailable)

  when(io.rayStateAdmission.fire && nonEmptyAdmission) {
    val work = table.workContexts(selectedFreeWork)
    work.valid := true.B
    work.workId := io.rayStateAdmission.bits.workId
    work.warpId := io.rayStateAdmission.bits.warpId
    work.liveLaneMask := io.rayStateAdmission.bits.activeMask
    work.issuedFields := 0.U

    when(!warpHasLiveWork(io.rayStateAdmission.bits.warpId)) {
      table.warpContexts(io.rayStateAdmission.bits.warpId) :=
        io.rayStateAdmission.bits.pds
    }

    for (lane <- 0 until params.pdsRowCount) {
      when(io.rayStateAdmission.bits.activeMask(lane)) {
        val row = table.rows(lane)
        row.state := RtPdsRowState.rayState
        row.workId := io.rayStateAdmission.bits.workId
        row.receivedFields := 0.U
        row.fault := false.B
      }
    }
  }

  val trackerFree = Wire(Vec(params.pdsReadTrackerCount, Bool()))
  for (index <- 0 until params.pdsReadTrackerCount) {
    trackerFree(index) := !table.readTrackers(index).valid
  }
  val hasFreeTracker = trackerFree.asUInt.orR
  val selectedFreeTracker = PriorityEncoder(trackerFree.asUInt)
  val readContext = table.workContexts(selectedReadWork)
  val selectedReadField = PriorityEncoder(
    (~readContext.issuedFields).asUInt & allRayStateFields
  )
  val readRequestValid = hasReadWork && hasFreeTracker

  val writeRows = Wire(Vec(params.pdsRowCount, Bool()))
  for (row <- 0 until params.pdsRowCount) {
    writeRows(row) := table.rows(row).state === RtPdsRowState.hitRecordWrite
  }
  val hasPendingWrite = writeRows.asUInt.orR
  val selectedWriteRow = PriorityEncoder(writeRows.asUInt)

  val writeGrant = hasPendingWrite && (!readRequestValid || preferWrite)
  val readGrant = readRequestValid && (!hasPendingWrite || !preferWrite)

  io.pdsRequest.valid := writeGrant || readGrant
  io.pdsRequest.bits := 0.U.asTypeOf(new RtPdsRequest)
  when(readGrant) {
    io.pdsRequest.bits.requestId := selectedFreeTracker
    io.pdsRequest.bits.write := false.B
    io.pdsRequest.bits.fieldWordOffset := selectedReadField
    io.pdsRequest.bits.readLaneMask := readContext.liveLaneMask
    io.pdsRequest.bits.pds := table.warpContexts(readContext.warpId)
  }.elsewhen(writeGrant) {
    val row = table.rows(selectedWriteRow)
    io.pdsRequest.bits.write := true.B
    io.pdsRequest.bits.writeBlockAddress := row.writeBlockAddress
    io.pdsRequest.bits.writeWordMask := row.writeWordMask
    io.pdsRequest.bits.writeData := row.data
  }

  when(io.pdsRequest.fire && hasPendingWrite && readRequestValid) {
    preferWrite := !preferWrite
  }
  when(io.pdsRequest.fire && readGrant) {
    val tracker = table.readTrackers(selectedFreeTracker)
    tracker.valid := true.B
    tracker.requestId := selectedFreeTracker
    tracker.workId := readContext.workId
    tracker.fieldIndex := selectedReadField
    tracker.laneMask := readContext.liveLaneMask
    readContext.issuedFields := readContext.issuedFields |
      UIntToOH(selectedReadField, params.pdsRayStateFieldCount)
  }
  when(io.pdsRequest.fire && writeGrant) {
    table.rows(selectedWriteRow).state := RtPdsRowState.free
  }

  val freeRowByOffset = Wire(Vec(params.pdsRowCount, Bool()))
  for (offset <- 0 until params.pdsRowCount) {
    val rowIndex = (table.nextWriteRow + offset.U)(params.pdsRowIndexWidth - 1, 0)
    freeRowByOffset(offset) := table.rows(rowIndex).state === RtPdsRowState.free
  }
  val hasFreeRow = freeRowByOffset.asUInt.orR
  val selectedFreeRowOffset = PriorityEncoder(freeRowByOffset.asUInt)
  val selectedFreeRow =
    (table.nextWriteRow + selectedFreeRowOffset)(params.pdsRowIndexWidth - 1, 0)
  io.hitRecordWrite.ready := hasFreeRow
  when(io.hitRecordWrite.fire) {
    assert(
      io.hitRecordWrite.bits.blockAddress(5, 0) === 0.U,
      "PDS write blocks must be 64-byte aligned"
    )
    val row = table.rows(selectedFreeRow)
    row.state := RtPdsRowState.hitRecordWrite
    row.data := io.hitRecordWrite.bits.data
    row.writeBlockAddress := io.hitRecordWrite.bits.blockAddress
    row.writeWordMask := io.hitRecordWrite.bits.wordMask
    table.nextWriteRow := selectedFreeRow + 1.U
  }

  val responseTrackerIndex = io.pdsResponse.bits.requestId(
    log2Ceil(params.pdsReadTrackerCount) - 1,
    0
  )
  val responseTracker = table.readTrackers(responseTrackerIndex)
  val responseIdInRange = io.pdsResponse.bits.requestId <
    params.pdsReadTrackerCount.U
  val responseMatches = responseIdInRange && responseTracker.valid &&
    responseTracker.requestId === io.pdsResponse.bits.requestId
  io.pdsResponse.ready := responseMatches

  when(io.pdsResponse.fire) {
    responseTracker.valid := false.B
    assert(
      io.pdsResponse.bits.laneMask === responseTracker.laneMask,
      "PDS response lane mask must match its issued field read"
    )
    for (lane <- 0 until params.pdsRowCount) {
      when(responseTracker.laneMask(lane)) {
        val row = table.rows(lane)
        val received = row.receivedFields |
          UIntToOH(responseTracker.fieldIndex, params.pdsRayStateFieldCount)
        val fault = row.fault || io.pdsResponse.bits.faultMask(lane)
        assert(
          row.state === RtPdsRowState.rayState &&
            row.workId === responseTracker.workId,
          "PDS response must target its admitted lane row"
        )
        row.data(responseTracker.fieldIndex) := io.pdsResponse.bits.readData(lane)
        row.receivedFields := received
        row.fault := fault
        when(received === allRayStateFields) {
          row.state := Mux(
            fault,
            RtPdsRowState.faultPending,
            RtPdsRowState.readyToIssue
          )
        }
      }
    }
  }

  val faultRows = Wire(Vec(params.pdsRowCount, Bool()))
  val readyRows = Wire(Vec(params.pdsRowCount, Bool()))
  for (row <- 0 until params.pdsRowCount) {
    faultRows(row) := table.rows(row).state === RtPdsRowState.faultPending
    readyRows(row) := table.rows(row).state === RtPdsRowState.readyToIssue
  }
  val hasFault = faultRows.asUInt.orR
  val hasReadyRow = readyRows.asUInt.orR
  val selectedFaultRow = PriorityEncoder(faultRows.asUInt)
  val selectedReadyRow = PriorityEncoder(readyRows.asUInt)

  io.faultOut.valid := hasFault
  io.faultOut.bits := 0.U.asTypeOf(new RtPdsRayStateFault)
  when(hasFault) {
    io.faultOut.bits.workId := table.rows(selectedFaultRow).workId
    io.faultOut.bits.laneId := selectedFaultRow
  }

  io.rayStateOut.valid := hasReadyRow && !hasFault
  io.rayStateOut.bits := 0.U.asTypeOf(new RtPdsRayStateRow)
  when(hasReadyRow) {
    io.rayStateOut.bits.workId := table.rows(selectedReadyRow).workId
    io.rayStateOut.bits.laneId := selectedReadyRow
    for (field <- 0 until params.pdsRayStateFieldCount) {
      io.rayStateOut.bits.data(field) := table.rows(selectedReadyRow).data(field)
    }
  }

  val releasingFault = io.faultOut.fire
  val releasingRayState = io.rayStateOut.fire
  val releasingRow = Mux(releasingFault, selectedFaultRow, selectedReadyRow)
  val releasingWorkId = table.rows(releasingRow).workId
  when(releasingFault || releasingRayState) {
    table.rows(releasingRow).state := RtPdsRowState.free
    val releasedLaneMask = UIntToOH(releasingRow, num_thread)
    for (index <- 0 until params.pdsWorkContextCount) {
      val work = table.workContexts(index)
      when(work.valid && work.workId === releasingWorkId) {
        val remainingLanes = work.liveLaneMask & (~releasedLaneMask).asUInt
        work.liveLaneMask := remainingLanes
        when(remainingLanes === 0.U) {
          work.valid := false.B
        }
      }
    }
  }
}

/** Admission to the resident, field-major ray-state table. */
class RtPdsReadAdmission extends Bundle {
  val workId = UInt(RtCorePortParameters.requestIdWidth.W)
  val warpId = UInt(depth_warp.W)
  val pds = new RtPdsContext
  val activeMask = UInt(num_thread.W)
}

/** A resident ray-state row made visible to the traversal scheduler. */
class RtPdsReadLane extends Bundle {
  val workId = UInt(RtCorePortParameters.requestIdWidth.W)
  val warpId = UInt(depth_warp.W)
  val laneId = UInt(log2Ceil(num_thread).W)
  val data = Vec(RtMemTableParameters.pdsRayStateFieldCount, UInt(xLen.W))
}

class RtPdsReadRetire extends Bundle {
  val workId = UInt(RtCorePortParameters.requestIdWidth.W)
  val warpId = UInt(depth_warp.W)
}

private[rtcore] class RtPdsReadContext extends Bundle {
  val valid = Bool()
  val workId = UInt(RtCorePortParameters.requestIdWidth.W)
  val pds = new RtPdsContext
  val activeMask = UInt(num_thread.W)
  val issuedFields = UInt(RtMemTableParameters.pdsRayStateFieldCount.W)
  val receivedFields = UInt(RtMemTableParameters.pdsRayStateFieldCount.W)
  val deliveredMask = UInt(num_thread.W)
  val faultMask = UInt(num_thread.W)
}

private[rtcore] class RtPdsReadTrackerV2 extends Bundle {
  val valid = Bool()
  val warpId = UInt(depth_warp.W)
  val field = UInt(log2Ceil(RtMemTableParameters.pdsRayStateFieldCount).W)
  val laneMask = UInt(num_thread.W)
}

/**
  * Warp-resident PDS ray-state table.  A delivered lane remains allocated;
  * only the traversal owner may retire the whole warp context.
  */
class RtPdsReadTable extends Module {
  private val params = RtMemTableParameters
  private val allFields = ((BigInt(1) << params.pdsRayStateFieldCount) - 1).U(
    params.pdsRayStateFieldCount.W
  )

  val io = IO(new Bundle {
    val admission = Flipped(Decoupled(new RtPdsReadAdmission))
    val laneOut = Decoupled(new RtPdsReadLane)
    val faultOut = Decoupled(new RtPdsRayStateFault)
    val retire = Flipped(Decoupled(new RtPdsReadRetire))
    val request = Decoupled(new RtPdsReadRequest)
    val response = Flipped(Decoupled(new RtPdsResponse))
  })

  val contexts = RegInit(VecInit(Seq.fill(params.pdsReadWarpEntryCount)(
    0.U.asTypeOf(new RtPdsReadContext)
  )))
  val rows = Reg(Vec(
    params.pdsReadWarpEntryCount,
    Vec(num_thread, Vec(params.pdsRayStateFieldCount, UInt(xLen.W)))
  ))
  val trackers = RegInit(VecInit(Seq.fill(params.pdsReadTrackerCount)(
    0.U.asTypeOf(new RtPdsReadTrackerV2)
  )))

  val admissionContext = contexts(io.admission.bits.warpId)
  io.admission.ready := io.admission.bits.activeMask.orR && !admissionContext.valid
  when(io.admission.fire) {
    admissionContext.valid := true.B
    admissionContext.workId := io.admission.bits.workId
    admissionContext.pds := io.admission.bits.pds
    admissionContext.activeMask := io.admission.bits.activeMask
    admissionContext.issuedFields := 0.U
    admissionContext.receivedFields := 0.U
    admissionContext.deliveredMask := 0.U
    admissionContext.faultMask := 0.U
  }

  val needsRead = Wire(Vec(params.pdsReadWarpEntryCount, Bool()))
  for (warp <- 0 until params.pdsReadWarpEntryCount) {
    needsRead(warp) := contexts(warp).valid &&
      contexts(warp).issuedFields =/= allFields
  }
  val freeTrackers = Wire(Vec(params.pdsReadTrackerCount, Bool()))
  for (index <- 0 until params.pdsReadTrackerCount)
    freeTrackers(index) := !trackers(index).valid
  val hasRead = needsRead.asUInt.orR
  val hasTracker = freeTrackers.asUInt.orR
  val readWarp = PriorityEncoder(needsRead.asUInt)
  val trackerIndex = PriorityEncoder(freeTrackers.asUInt)
  val readContext = contexts(readWarp)
  val readField = PriorityEncoder((~readContext.issuedFields).asUInt & allFields)

  io.request.valid := hasRead && hasTracker
  io.request.bits := 0.U.asTypeOf(new RtPdsReadRequest)
  when(io.request.valid) {
    io.request.bits.requestId := trackerIndex
    io.request.bits.fieldWordOffset := RtAbiLayout.SlotAccelLo.U + readField
    io.request.bits.readLaneMask := readContext.activeMask
    io.request.bits.pds := readContext.pds
  }
  when(io.request.fire) {
    val tracker = trackers(trackerIndex)
    tracker.valid := true.B
    tracker.warpId := readWarp
    tracker.field := readField
    tracker.laneMask := readContext.activeMask
    readContext.issuedFields := readContext.issuedFields |
      UIntToOH(readField, params.pdsRayStateFieldCount)
  }

  val responseIndex = io.response.bits.requestId(
    log2Ceil(params.pdsReadTrackerCount) - 1,
    0
  )
  val responseInRange = io.response.bits.requestId < params.pdsReadTrackerCount.U
  val responseTracker = trackers(responseIndex)
  val responseMatches = responseInRange && responseTracker.valid
  io.response.ready := responseMatches
  when(io.response.fire) {
    val context = contexts(responseTracker.warpId)
    assert(context.valid, "PDS read response targets a live warp context")
    assert(io.response.bits.laneMask === responseTracker.laneMask,
      "PDS read response lane mask must match its request")
    responseTracker.valid := false.B
    context.receivedFields := context.receivedFields |
      UIntToOH(responseTracker.field, params.pdsRayStateFieldCount)
    context.faultMask := context.faultMask | io.response.bits.faultMask
    for (lane <- 0 until num_thread) {
      when(responseTracker.laneMask(lane)) {
        rows(responseTracker.warpId)(lane)(responseTracker.field) :=
          io.response.bits.readData(lane)
      }
    }
  }

  val completedWarps = Wire(Vec(params.pdsReadWarpEntryCount, Bool()))
  for (warp <- 0 until params.pdsReadWarpEntryCount) {
    completedWarps(warp) := contexts(warp).valid &&
      contexts(warp).receivedFields === allFields
  }
  val laneAvailable = Wire(Vec(params.pdsReadWarpEntryCount, UInt(num_thread.W)))
  val faultAvailable = Wire(Vec(params.pdsReadWarpEntryCount, UInt(num_thread.W)))
  for (warp <- 0 until params.pdsReadWarpEntryCount) {
    val undispatched = contexts(warp).activeMask & (~contexts(warp).deliveredMask).asUInt
    laneAvailable(warp) := Mux(completedWarps(warp),
      undispatched & (~contexts(warp).faultMask).asUInt, 0.U)
    faultAvailable(warp) := Mux(completedWarps(warp),
      undispatched & contexts(warp).faultMask, 0.U)
  }
  val hasLane = laneAvailable.map(_.orR).reduce(_ || _)
  val hasFault = faultAvailable.map(_.orR).reduce(_ || _)
  val laneWarpMask = VecInit(laneAvailable.map(_.orR)).asUInt
  val faultWarpMask = VecInit(faultAvailable.map(_.orR)).asUInt
  val laneWarp = PriorityEncoder(laneWarpMask)
  val faultWarp = PriorityEncoder(faultWarpMask)
  val laneId = PriorityEncoder(laneAvailable(laneWarp))
  val faultLaneId = PriorityEncoder(faultAvailable(faultWarp))

  io.laneOut.valid := hasLane && !hasFault
  io.laneOut.bits := 0.U.asTypeOf(new RtPdsReadLane)
  when(io.laneOut.valid) {
    io.laneOut.bits.workId := contexts(laneWarp).workId
    io.laneOut.bits.warpId := laneWarp
    io.laneOut.bits.laneId := laneId
    io.laneOut.bits.data := rows(laneWarp)(laneId)
  }
  io.faultOut.valid := hasFault
  io.faultOut.bits := 0.U.asTypeOf(new RtPdsRayStateFault)
  when(hasFault) {
    io.faultOut.bits.workId := contexts(faultWarp).workId
    io.faultOut.bits.laneId := faultLaneId
  }
  when(io.laneOut.fire) {
    contexts(laneWarp).deliveredMask := contexts(laneWarp).deliveredMask |
      UIntToOH(laneId, num_thread)
  }
  when(io.faultOut.fire) {
    contexts(faultWarp).deliveredMask := contexts(faultWarp).deliveredMask |
      UIntToOH(faultLaneId, num_thread)
  }

  val retireContext = contexts(io.retire.bits.warpId)
  io.retire.ready := retireContext.valid &&
    retireContext.workId === io.retire.bits.workId &&
    (retireContext.deliveredMask & retireContext.activeMask) === retireContext.activeMask
  when(io.retire.fire) {
    retireContext.valid := false.B
  }
}

/** A semantic hit-record update retained until its warp is ready to flush. */
class RtPdsHitRecordUpdate extends Bundle {
  val warpId = UInt(depth_warp.W)
  val laneId = UInt(log2Ceil(num_thread).W)
  val committed = Bool()
  val dirty = UInt(RtMemTableParameters.hitRecordStorageWordCount.W)
  val data = Vec(RtMemTableParameters.hitRecordStorageWordCount, UInt(xLen.W))
}

class RtPdsWriteAdmission extends Bundle {
  val workId = UInt(RtCorePortParameters.requestIdWidth.W)
  val warpId = UInt(depth_warp.W)
  val pds = new RtPdsContext
  val activeMask = UInt(num_thread.W)
}

class RtPdsWriteFinish extends Bundle {
  val workId = UInt(RtCorePortParameters.requestIdWidth.W)
  val warpId = UInt(depth_warp.W)
  val terminalMask = UInt(num_thread.W)
}

class RtPdsWriteDrained extends Bundle {
  val workId = UInt(RtCorePortParameters.requestIdWidth.W)
  val warpId = UInt(depth_warp.W)
}

private[rtcore] class RtPdsWriteContext extends Bundle {
  val valid = Bool()
  val workId = UInt(RtCorePortParameters.requestIdWidth.W)
  val pds = new RtPdsContext
  val activeMask = UInt(num_thread.W)
  val flushing = Bool()
  val drained = Bool()
  val record = Bool()
  val word = UInt(log2Ceil(RtMemTableParameters.hitRecordStorageWordCount).W)
  val upperHalf = Bool()
}

/**
  * Persistent hit-record write table.  Its internal organization is lane
  * local for IU updates; its only external representation is field-major PDS
  * blocks with a dirty word mask.
  */
class RtPdsWriteTable(
    warpEntryCount: Int = RtMemTableParameters.pdsWriteWarpEntryCount
) extends Module {
  private val params = RtMemTableParameters
  private val recordWords = params.hitRecordStorageWordCount
  private val halfWords = RtCorePortParameters.pdsWriteWordCount
  private val tableIndexWidth = log2Ceil(warpEntryCount)
  require(num_thread == 2 * halfWords)

  private def tableIndex(warpId: UInt): UInt = {
    if (warpEntryCount == 1) 0.U else warpId(tableIndexWidth - 1, 0)
  }

  val io = IO(new Bundle {
    val admission = Flipped(Decoupled(new RtPdsWriteAdmission))
    val update = Flipped(Decoupled(new RtPdsHitRecordUpdate))
    val finish = Flipped(Decoupled(new RtPdsWriteFinish))
    val write = Decoupled(new RtPdsWriteRequest)
    val drained = Decoupled(new RtPdsWriteDrained)
  })

  val contexts = RegInit(VecInit(Seq.fill(warpEntryCount)(
    0.U.asTypeOf(new RtPdsWriteContext)
  )))
  val data = Reg(Vec(warpEntryCount,
    Vec(num_thread, Vec(2, Vec(recordWords, UInt(xLen.W))))))
  val dirty = RegInit(VecInit(Seq.fill(warpEntryCount)(
    VecInit(Seq.fill(num_thread)(VecInit(Seq.fill(2)(
      VecInit(Seq.fill(recordWords)(false.B))
    ))))
  )))

  val admissionIndex = tableIndex(io.admission.bits.warpId)
  val admissionContext = contexts(admissionIndex)
  io.admission.ready := io.admission.bits.activeMask.orR && !admissionContext.valid
  when(io.admission.fire) {
    admissionContext.valid := true.B
    admissionContext.workId := io.admission.bits.workId
    admissionContext.pds := io.admission.bits.pds
    admissionContext.activeMask := io.admission.bits.activeMask
    admissionContext.flushing := false.B
    admissionContext.drained := false.B
    admissionContext.record := false.B
    admissionContext.word := 0.U
    admissionContext.upperHalf := false.B
    for (lane <- 0 until num_thread; record <- 0 until 2; word <- 0 until recordWords)
      dirty(admissionIndex)(lane)(record)(word) := false.B
  }

  val updateIndex = tableIndex(io.update.bits.warpId)
  val updateContext = contexts(updateIndex)
  io.update.ready := updateContext.valid && !updateContext.flushing &&
    updateContext.activeMask(io.update.bits.laneId)
  when(io.update.fire) {
    for (word <- 0 until recordWords) {
      when(io.update.bits.dirty(word)) {
        data(updateIndex)(io.update.bits.laneId)(io.update.bits.committed)(word) :=
          io.update.bits.data(word)
        dirty(updateIndex)(io.update.bits.laneId)(io.update.bits.committed)(word) := true.B
      }
    }
  }

  val finishContext = contexts(tableIndex(io.finish.bits.warpId))
  io.finish.ready := finishContext.valid && !finishContext.flushing &&
    finishContext.workId === io.finish.bits.workId &&
    io.finish.bits.terminalMask === finishContext.activeMask
  when(io.finish.fire) {
    finishContext.flushing := true.B
    finishContext.record := false.B
    finishContext.word := 0.U
    finishContext.upperHalf := false.B
  }

  val flushing = Wire(Vec(warpEntryCount, Bool()))
  for (warp <- 0 until warpEntryCount)
    flushing(warp) := contexts(warp).valid && contexts(warp).flushing && !contexts(warp).drained
  val hasFlush = flushing.asUInt.orR
  val writeWarp = PriorityEncoder(flushing.asUInt)
  val writeContext = contexts(writeWarp)
  val halfBase = Mux(writeContext.upperHalf, halfWords.U, 0.U)
  val halfDirty = Wire(Vec(halfWords, Bool()))
  for (word <- 0 until halfWords) {
    val lane = halfBase + word.U
    halfDirty(word) := writeContext.activeMask(lane) &&
      dirty(writeWarp)(lane)(writeContext.record)(writeContext.word)
  }
  val hasHalfDirty = halfDirty.asUInt.orR

  io.write.valid := hasFlush && hasHalfDirty
  io.write.bits := 0.U.asTypeOf(new RtPdsWriteRequest)
  when(io.write.valid) {
    val pdsThreadCount = writeContext.pds.numWarps * writeContext.pds.numThreads
    val fieldBase = Mux(
      writeContext.record,
      (RtAbiLayout.AbiCommittedHitRecordBaseBytes / RtAbiLayout.AbiWordBytes).U,
      (RtAbiLayout.AbiCandidateHitRecordBaseBytes / RtAbiLayout.AbiWordBytes).U
    )
    val field = fieldBase + writeContext.word
    val physicalWord = field * pdsThreadCount + writeContext.pds.pdsWarpTidBase + halfBase
    io.write.bits.blockAddress := writeContext.pds.baseAddress + (physicalWord << 2)
    io.write.bits.wordMask := halfDirty.asUInt
    for (word <- 0 until halfWords) {
      val lane = halfBase + word.U
      io.write.bits.data(word) := data(writeWarp)(lane)(writeContext.record)(writeContext.word)
    }
  }

  def advance(context: RtPdsWriteContext): Unit = {
    when(!context.upperHalf) {
      context.upperHalf := true.B
    }.elsewhen(context.word === (recordWords - 1).U) {
      context.upperHalf := false.B
      context.word := 0.U
      when(!context.record) {
        context.record := true.B
      }.otherwise {
        context.drained := true.B
      }
    }.otherwise {
      context.upperHalf := false.B
      context.word := context.word + 1.U
    }
  }
  when(hasFlush && !hasHalfDirty) {
    advance(writeContext)
  }
  when(io.write.fire) {
    assert(io.write.bits.blockAddress(5, 0) === 0.U,
      "PDS hit-record writes must be 64-byte aligned")
    advance(writeContext)
  }

  val drainedContexts = Wire(Vec(warpEntryCount, Bool()))
  for (warp <- 0 until warpEntryCount)
    drainedContexts(warp) := contexts(warp).valid && contexts(warp).drained
  val hasDrained = drainedContexts.asUInt.orR
  val drainedWarp = PriorityEncoder(drainedContexts.asUInt)
  io.drained.valid := hasDrained
  io.drained.bits := 0.U.asTypeOf(new RtPdsWriteDrained)
  when(hasDrained) {
    io.drained.bits.workId := contexts(drainedWarp).workId
    io.drained.bits.warpId := drainedWarp
  }
  when(io.drained.fire) {
    contexts(drainedWarp).valid := false.B
    contexts(drainedWarp).flushing := false.B
    contexts(drainedWarp).drained := false.B
  }
}

/** Identifies the lane operation waiting for an AS cache line. */
class RtAsLineWaiterV2 extends Bundle {
  val warpId = UInt(depth_warp.W)
  val laneId = UInt(log2Ceil(num_thread).W)
  val nodeKind = UInt(2.W)
  val nodeRef = UInt(32.W)
}

class RtAsTableAcquire extends Bundle {
  val lineAddress = UInt(memReqLen.W)
  val wordMask = UInt(RtMemTableParameters.lineWordCount.W)
  val waiter = new RtAsLineWaiterV2
}

class RtAsTableReadyLine extends Bundle {
  val waiter = new RtAsLineWaiterV2
  val data = Vec(RtMemTableParameters.lineWordCount, UInt(xLen.W))
  val fault = Bool()
}

private[rtcore] class RtAsTableEntry extends Bundle {
  val valid = Bool()
  val cacheRequestSent = Bool()
  val cacheResponseValid = Bool()
  val lineAddress = UInt(memReqLen.W)
  val cacheWordMask = UInt(RtMemTableParameters.lineWordCount.W)
  val waiterValid = Vec(RtMemTableParameters.maxAsWaitersPerEntry, Bool())
  val waiters = Vec(RtMemTableParameters.maxAsWaitersPerEntry, new RtAsLineWaiterV2)
  val cacheLineData = Vec(RtMemTableParameters.lineWordCount, UInt(xLen.W))
  val responseFault = Bool()
}

/**
  * Line-granular AS transaction table.  It only combines identical pending
  * lines; node decoding and traversal ownership remain outside the table.
  */
class RtAsTable extends Module {
  private val params = RtMemTableParameters
  val io = IO(new Bundle {
    val acquire = Flipped(Decoupled(new RtAsTableAcquire))
    val readyLine = Decoupled(new RtAsTableReadyLine)
    val request = Decoupled(new RtAsReadRequest)
    val response = Flipped(Decoupled(new RtAsReadResponse))
  })

  val entries = RegInit(VecInit(Seq.fill(params.asEntryCount)(
    0.U.asTypeOf(new RtAsTableEntry)
  )))
  val mergeable = Wire(Vec(params.asEntryCount, Bool()))
  val freeEntries = Wire(Vec(params.asEntryCount, Bool()))
  for (entry <- 0 until params.asEntryCount) {
    mergeable(entry) := entries(entry).valid && !entries(entry).cacheResponseValid &&
      entries(entry).lineAddress === io.acquire.bits.lineAddress &&
      !entries(entry).waiterValid.asUInt.andR
    freeEntries(entry) := !entries(entry).valid
  }
  val hasMerge = mergeable.asUInt.orR
  val hasFree = freeEntries.asUInt.orR
  val mergeIndex = PriorityEncoder(mergeable.asUInt)
  val freeIndex = PriorityEncoder(freeEntries.asUInt)
  val acquireIndex = Mux(hasMerge, mergeIndex, freeIndex)
  io.acquire.ready := hasMerge || hasFree
  when(io.acquire.fire) {
    val entry = entries(acquireIndex)
    when(!hasMerge) {
      entry.valid := true.B
      entry.cacheRequestSent := false.B
      entry.cacheResponseValid := false.B
      entry.lineAddress := io.acquire.bits.lineAddress
      entry.cacheWordMask := io.acquire.bits.wordMask
      entry.waiterValid.foreach(_ := false.B)
      entry.responseFault := false.B
    }.otherwise {
      entry.cacheWordMask := entry.cacheWordMask | io.acquire.bits.wordMask
    }
    val freeWaiter = PriorityEncoder((~entry.waiterValid.asUInt).asUInt)
    entry.waiterValid(freeWaiter) := true.B
    entry.waiters(freeWaiter) := io.acquire.bits.waiter
  }

  val unsent = Wire(Vec(params.asEntryCount, Bool()))
  for (entry <- 0 until params.asEntryCount)
    unsent(entry) := entries(entry).valid && !entries(entry).cacheRequestSent
  val hasUnsent = unsent.asUInt.orR
  val requestEntry = PriorityEncoder(unsent.asUInt)
  io.request.valid := hasUnsent
  io.request.bits := 0.U.asTypeOf(new RtAsReadRequest)
  when(hasUnsent) {
    io.request.bits.requestId := requestEntry
    io.request.bits.lineAddress := entries(requestEntry).lineAddress
    io.request.bits.wordMask := entries(requestEntry).cacheWordMask
  }
  when(io.request.fire) {
    entries(requestEntry).cacheRequestSent := true.B
  }

  val responseEntry = io.response.bits.requestId(log2Ceil(params.asEntryCount) - 1, 0)
  val responseInRange = io.response.bits.requestId < params.asEntryCount.U
  val responseMatches = responseInRange && entries(responseEntry).valid &&
    entries(responseEntry).cacheRequestSent && !entries(responseEntry).cacheResponseValid
  io.response.ready := responseMatches
  when(io.response.fire) {
    entries(responseEntry).cacheResponseValid := true.B
    entries(responseEntry).cacheLineData := io.response.bits.data
    entries(responseEntry).responseFault := io.response.bits.fault
  }

  val readyEntries = Wire(Vec(params.asEntryCount, Bool()))
  for (entry <- 0 until params.asEntryCount)
    readyEntries(entry) := entries(entry).valid && entries(entry).cacheResponseValid &&
      entries(entry).waiterValid.asUInt.orR
  val hasReady = readyEntries.asUInt.orR
  val readyEntry = PriorityEncoder(readyEntries.asUInt)
  val readyWaiter = PriorityEncoder(entries(readyEntry).waiterValid.asUInt)
  io.readyLine.valid := hasReady
  io.readyLine.bits := 0.U.asTypeOf(new RtAsTableReadyLine)
  when(hasReady) {
    io.readyLine.bits.waiter := entries(readyEntry).waiters(readyWaiter)
    io.readyLine.bits.data := entries(readyEntry).cacheLineData
    io.readyLine.bits.fault := entries(readyEntry).responseFault
  }
  when(io.readyLine.fire) {
    val remaining = entries(readyEntry).waiterValid.asUInt &
      (~UIntToOH(readyWaiter, params.maxAsWaitersPerEntry)).asUInt
    entries(readyEntry).waiterValid(readyWaiter) := false.B
    when(remaining === 0.U) {
      entries(readyEntry).valid := false.B
      entries(readyEntry).cacheResponseValid := false.B
    }
  }
}

/**
  * RTcore's memory-table top level.  It owns the independent resident PDS
  * read/write tables and the AS line table; consumers instantiate this module
  * rather than wiring those banks directly.
  */
class RtMemTable extends Module {
  val io = IO(new Bundle {
    val pdsReadAdmission = Flipped(Decoupled(new RtPdsReadAdmission))
    val pdsReadLane = Decoupled(new RtPdsReadLane)
    val pdsFault = Decoupled(new RtPdsRayStateFault)
    val pdsReadRetire = Flipped(Decoupled(new RtPdsReadRetire))
    val pdsWriteAdmission = Flipped(Decoupled(new RtPdsWriteAdmission))
    val pdsHitUpdate = Flipped(Decoupled(new RtPdsHitRecordUpdate))
    val pdsWriteFinish = Flipped(Decoupled(new RtPdsWriteFinish))
    val pdsWriteDrained = Decoupled(new RtPdsWriteDrained)
    val asAcquire = Flipped(Decoupled(new RtAsTableAcquire))
    val asReadyLine = Decoupled(new RtAsTableReadyLine)
    val pdsReadRequest = Decoupled(new RtPdsReadRequest)
    val pdsWriteRequest = Decoupled(new RtPdsWriteRequest)
    val pdsResponse = Flipped(Decoupled(new RtPdsResponse))
    val asRequest = Decoupled(new RtAsReadRequest)
    val asResponse = Flipped(Decoupled(new RtAsReadResponse))
  })

  val pdsRead = Module(new RtPdsReadTable)
  val pdsWrite = Module(new RtPdsWriteTable)
  val asTable = Module(new RtAsTable)

  pdsRead.io.admission <> io.pdsReadAdmission
  io.pdsReadLane <> pdsRead.io.laneOut
  io.pdsFault <> pdsRead.io.faultOut
  pdsRead.io.retire <> io.pdsReadRetire
  pdsWrite.io.admission <> io.pdsWriteAdmission
  pdsWrite.io.update <> io.pdsHitUpdate
  pdsWrite.io.finish <> io.pdsWriteFinish
  io.pdsWriteDrained <> pdsWrite.io.drained
  asTable.io.acquire <> io.asAcquire
  io.asReadyLine <> asTable.io.readyLine
  io.asRequest <> asTable.io.request
  asTable.io.response <> io.asResponse

  io.pdsReadRequest <> pdsRead.io.request
  io.pdsWriteRequest <> pdsWrite.io.write
  pdsRead.io.response <> io.pdsResponse
}

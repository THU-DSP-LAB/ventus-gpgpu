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

  val asEntryCount = 32
  val maxAsWaitersPerEntry = 2
  val lineWordCount = dcache_BlockWords
  val lineBytes = lineWordCount * (xLen / 8)

  require(pdsRowCount == num_thread)
  require(pdsRowWords == 16)
  require(pdsRayStateFieldCount <= pdsRowWords)
  require(lineWordCount == num_thread)
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

/** Complete structural storage for future combined PDS and AS control. */
class RtMemTable extends Bundle {
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

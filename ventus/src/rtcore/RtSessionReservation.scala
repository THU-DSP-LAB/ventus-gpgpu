package rtcore

import chisel3._
import chisel3.util._
import top.parameters._

/** Admission credits held until context-releasing completion,
  * or software release ack for a retained candidate context.
  *
  * A traverse has no final SIMT lane mask at this point. A newly admitted
  * traversal is therefore provisional until RT issue: an empty one is
  * cancelled, while a non-empty one holds its parent's credit. Driver-backed
  * commands may hold independent credits; legacy/group commands additionally
  * claim exclusive ownership before operand collection. Neither permits a
  * second outstanding request from the same parent.
  */
class RtSessionReservation(multiWarp: Boolean = false) extends Module {
  val io = IO(new Bundle {
    val issued = Flipped(Valid(UInt(depth_warp.W)))
    val traverseResolved = Flipped(Valid(new Bundle {
      val warpId = UInt(depth_warp.W)
      val wasEmpty = Bool()
    }))
    val releaseIssued = Flipped(Valid(UInt(depth_warp.W)))
    val released = Flipped(Valid(UInt(depth_warp.W)))
    val completed = Flipped(Valid(new Bundle {
      val warpId = UInt(depth_warp.W)
      val contextReleased = Bool()
    }))
    // Concurrent admission is only safe with driver-backed spill/release.
    val concurrent = if (multiWarp) Some(Input(Vec(num_warp, Bool()))) else None
    val issuedExclusive = if (multiWarp) Some(Input(Bool())) else None
    val busy = Output(Bool())
    val allowed = Output(Vec(num_warp, Bool()))
    val reserved = Output(Bool())
    val owner = Output(UInt(depth_warp.W))
  })
  if (multiWarp) {
    val traversing = RegInit(VecInit(Seq.fill(num_warp)(false.B)))
    val provisional = RegInit(VecInit(Seq.fill(num_warp)(false.B)))
    val reserved = RegInit(false.B)
    val owner = RegInit(0.U(depth_warp.W))
    val closing = RegInit(false.B)
    val exclusiveProvisional = RegInit(false.B)
    io.reserved := reserved
    io.owner := owner
    io.busy := reserved || traversing.asUInt.orR
    for (warp <- 0 until num_warp) {
      io.allowed(warp) := !closing && !traversing(warp) &&
        (!reserved || owner === warp.U) &&
        (io.concurrent.get(warp) || !traversing.asUInt.orR)
      when(io.issued.valid && io.issued.bits === warp.U) {
        assert(io.allowed(warp), "RT admission must respect per-parent credits")
        traversing(warp) := true.B
        provisional(warp) := true.B
      }
      when(io.traverseResolved.valid && io.traverseResolved.bits.warpId === warp.U) {
        assert(traversing(warp) && provisional(warp))
        provisional(warp) := false.B
        when(io.traverseResolved.bits.wasEmpty) { traversing(warp) := false.B }
      }
      when(io.completed.valid && io.completed.bits.warpId === warp.U) {
        assert(traversing(warp) && !provisional(warp),
          "completion must consume its own parent traversal")
        assert(!(io.issued.valid && io.issued.bits === warp.U))
        traversing(warp) := false.B
      }
    }
    when(io.issued.valid && (!io.concurrent.get(io.issued.bits) || io.issuedExclusive.get)) {
      assert(!traversing.asUInt.orR,
        "legacy/group traversal must drain concurrent parents before admission")
      reserved := true.B
      owner := io.issued.bits
      exclusiveProvisional := !reserved
    }
    when(io.traverseResolved.valid &&
      reserved && owner === io.traverseResolved.bits.warpId) {
      // An empty nested request does not destroy a retained context.
      when(io.traverseResolved.bits.wasEmpty && exclusiveProvisional) { reserved := false.B }
      exclusiveProvisional := false.B
    }
    when(io.completed.valid) {
      when(io.completed.bits.contextReleased) {
        when(reserved && owner === io.completed.bits.warpId) { reserved := false.B }
      }.otherwise {
        reserved := true.B
        owner := io.completed.bits.warpId
      }
    }
    when(io.releaseIssued.valid) {
      assert(reserved && owner === io.releaseIssued.bits && !closing &&
        !traversing(owner))
      closing := true.B
    }
    when(io.released.valid) {
      assert(reserved && owner === io.released.bits && closing)
      reserved := false.B
      closing := false.B
    }
  } else {
    val reserved = RegInit(false.B)
    // `owner` is compared by the admission guard.  Although `reserved` masks
    // that comparison before the first claim, give the externally visible
    // ownership state a defined reset value as well: simulation must not rely
    // on the elaboration-dependent initial value of a control register.
    val owner = RegInit(0.U(depth_warp.W))
    val closing = RegInit(false.B)
    val provisional = RegInit(false.B)
    val traversing = RegInit(false.B)
    io.reserved := reserved
    io.owner := owner
    for (i <- 0 until num_warp) {
      io.allowed(i) := !closing && !provisional && !traversing && (!reserved || owner === i.U)
    }
    when(io.issued.valid) {
      assert(!traversing, "a traversal must wait for the previous completion writeback")
      assert(!closing, "a new traversal must wait for the previous release acknowledgement")
      assert(!reserved || owner === io.issued.bits,
        "only the reserved warp may issue a retained RT traversal")
      traversing := true.B
      when(!reserved) {
        reserved := true.B
        owner := io.issued.bits
        provisional := true.B
      }
    }
    when(io.traverseResolved.valid) {
      assert(reserved && owner === io.traverseResolved.bits.warpId,
        "only the reserved warp may resolve an RT traversal")
      when(io.traverseResolved.bits.wasEmpty) { traversing := false.B }
      // A same-owner resume/nested traverse resolves while the session is
      // already established. Only the first, provisional admission may cancel
      // its reservation on an empty effective lane mask.
      when(provisional) {
        provisional := false.B
        when(io.traverseResolved.bits.wasEmpty) {
          reserved := false.B
        }
      }
    }
    when(io.completed.valid) {
      assert(reserved && traversing && !provisional && !closing &&
        owner === io.completed.bits.warpId,
        "completion writeback must belong to the executing RT owner")
      assert(!io.issued.valid, "completion and new traversal admission must not overlap")
      traversing := false.B
      when(io.completed.bits.contextReleased) { reserved := false.B }
    }
    when(io.releaseIssued.valid) {
      assert(reserved && owner === io.releaseIssued.bits && !closing && !provisional && !traversing,
        "only the reserved warp may issue its final RT release")
      closing := true.B
    }
    when(io.released.valid) {
      assert(reserved && owner === io.released.bits,
        "RT release acknowledgement must retire the reserved warp")
      assert(!io.issued.valid, "release and nested traversal cannot issue together")
      reserved := false.B
      closing := false.B
      provisional := false.B
      traversing := false.B
    }
    io.busy := reserved || traversing
  }
}

/** One narrow pending descriptor per parent warp. This is not a ray-data FIFO.
  * Opportunistic batches keep parent lane % 8 == physical lane % 8, so the
  * completion return path has four possible sources per lane, not 32.
  * Requests are never split; an incompatible/full bank starts a later batch.
  * The scheduler still owns one fixed physical session. No live stack moves.
  */
class RtTaskBatcher extends Module {
  private val banks = RtCoreSchedulerParameters.boxUnitCount
  private val columns = num_thread / banks
  require(num_warp == num_warp_in_a_block && isPow2(num_warp))
  require(banks == 8 && columns == 4)
  val io = IO(new Bundle {
    val command = Flipped(Decoupled(new RtWarpCommand))
    val completion = Decoupled(new RtWarpCompletion)
    val schedulerCommand = Decoupled(new RtWarpCommand)
    val schedulerCompletion = Flipped(Decoupled(new RtWarpCompletion))
  })
  class ParentTask extends Bundle {
    val spillPool = new RtSpillPoolDescriptor
    val mask = UInt(num_thread.W)
    val issueId = UInt(RtCorePortParameters.issueIdWidth.W)
    val destination = UInt(RtCorePortParameters.regidx_totalwidth.W)
    val eligible = Bool()
  }
  val tasks = Reg(Vec(num_warp, new ParentTask))
  val pending = RegInit(VecInit(Seq.fill(num_warp)(false.B)))
  val remaining = RegInit(0.U(num_warp.W))
  val cursor = RegInit(0.U(depth_warp.W))
  val idle :: send :: waitResult :: direct :: Nil = Enum(4)
  val state = RegInit(idle)
  val built = Reg(new RtWarpCommand)
  val builtMembers = Reg(UInt(num_warp.W))
  val slotOwner = Reg(Vec(num_thread, UInt(depth_warp.W)))
  // Bank is implicit: only the two-bit position within a bank needs storage.
  val slotColumn = Reg(Vec(num_thread, UInt(log2Ceil(columns).W)))
  val configured = io.command.bits.spillPool.spillPoolBase =/= 0.U &&
    io.command.bits.spillPool.dispatchEpoch =/= 0.U &&
    io.command.bits.spillPool.slotsPerCore >= RtSpillSlotLayout.slotsPerCore.U
  val noPending = !pending.asUInt.orR
  // Full primary commands and legacy/group commands keep the old direct path.
  val passthrough = io.command.bits.grouped || !configured ||
    (state === idle && noPending && !io.command.bits.batchEligible)
  io.command.ready := Mux(passthrough,
    state === idle && noPending && io.schedulerCommand.ready,
    !pending(io.command.bits.warpId) && !remaining(io.command.bits.warpId))
  io.schedulerCommand.valid := state === send ||
    (state === idle && noPending && passthrough && io.command.valid)
  io.schedulerCommand.bits := Mux(state === send, built, io.command.bits)
  when(io.command.fire && !passthrough) {
    val wid = io.command.bits.warpId
    assert(io.command.bits.activeMask.orR && !io.command.bits.grouped)
    tasks(wid).spillPool := io.command.bits.spillPool
    tasks(wid).mask := io.command.bits.activeMask
    tasks(wid).issueId := io.command.bits.issueId
    tasks(wid).destination := io.command.bits.destinationVd
    tasks(wid).eligible := io.command.bits.batchEligible
    pending(wid) := true.B
  }
  when(io.command.fire && passthrough) { state := direct }

  val rotated = (Cat(pending.asUInt, pending.asUInt) >> cursor)(num_warp - 1, 0)
  val first = (cursor + PriorityEncoder(rotated))(depth_warp - 1, 0)
  val selected = Wire(Vec(num_warp, Bool()))
  selected := VecInit(Seq.fill(num_warp)(false.B))
  var used = Seq.fill(banks)(0.U(3.W))
  for (offset <- 0 until num_warp) {
    val wid = (first + offset.U)(depth_warp - 1, 0)
    val counts = (0 until banks).map(bank =>
      PopCount(VecInit((0 until columns).map(col => tasks(wid).mask(bank + col * banks)))))
    val fit = (0 until banks).map(bank => (used(bank) +& counts(bank)) <= columns.U).reduce(_ && _)
    val compatible = wid === first || (tasks(first).eligible && tasks(wid).eligible &&
      tasks(wid).spillPool.asUInt === tasks(first).spillPool.asUInt)
    val choose = pending(wid) && fit && compatible
    selected(wid) := choose
    used = (0 until banks).map(bank => Mux(choose, (used(bank) + counts(bank))(2, 0), used(bank)))
  }
  val merged = PopCount(selected) > 1.U
  val selectedMask = selected.asUInt
  val next = Wire(new RtWarpCommand)
  next := 0.U.asTypeOf(new RtWarpCommand)
  next.warpId := first
  next.issueId := tasks(first).issueId
  next.destinationVd := tasks(first).destination
  next.spillPool := tasks(first).spillPool
  // Internal locator map names physical SRAM parent rows; it is not exposed
  // to shader code and never changes its workgroup/warp organisation.
  next.workgroupWarps := num_warp.U
  next.workgroupMemberValid := Fill(num_warp, 1.U(1.W))
  for (warp <- 0 until num_warp) next.workgroupMemberWarp(warp) := warp.U
  next.grouped := merged
  next.activeMask := Mux(merged,
    VecInit((0 until num_thread).map(lane => (lane / banks).U < used(lane % banks))).asUInt,
    tasks(first).mask)
  val owners = Wire(Vec(num_thread, UInt(depth_warp.W)))
  val cols = Wire(Vec(num_thread, UInt(log2Ceil(columns).W)))
  for (bank <- 0 until banks; physicalCol <- 0 until columns) {
    val lane = bank + physicalCol * banks
    val matches = Wire(Vec(num_warp * columns, Bool()))
    val parentOwners = Wire(Vec(num_warp * columns, UInt(depth_warp.W)))
    val parentCols = Wire(Vec(num_warp * columns, UInt(log2Ceil(columns).W)))
    var prefix = 0.U(3.W)
    for (offset <- 0 until num_warp) {
      val wid = (first + offset.U)(depth_warp - 1, 0)
      for (col <- 0 until columns) {
        val i = offset * columns + col
        val live = selected(wid) && tasks(wid).mask(bank + col * banks)
        matches(i) := live && prefix === physicalCol.U
        parentOwners(i) := wid
        parentCols(i) := col.U
        prefix = prefix + live.asUInt
      }
    }
    owners(lane) := Mux(merged, Mux1H(matches, parentOwners), first)
    cols(lane) := Mux(merged, Mux1H(matches, parentCols), physicalCol.U)
    next.locator(lane) := Cat(owners(lane), cols(lane), bank.U(3.W))
  }
  when(state === idle && !noPending) {
    assert(selected(first))
    built := next
    builtMembers := selectedMask
    slotOwner := owners
    slotColumn := cols
    state := send
  }
  when(state === send && io.schedulerCommand.fire) {
    for (warp <- 0 until num_warp) when(builtMembers(warp)) { pending(warp) := false.B }
    remaining := builtMembers
    cursor := built.warpId + 1.U
    state := waitResult
    if (sys.env.get("VENTUS_RTCORE_TRACE").contains("1")) {
      printf(p"[RTCORE batch] parents=0x${Hexadecimal(builtMembers)}" +
        p" lanes=${Decimal(PopCount(built.activeMask))} grouped=${Decimal(built.grouped)}\n")
    }
  }

  val resultWarp = PriorityEncoder(remaining)
  val result = Wire(new RtWarpCompletion)
  result := io.schedulerCompletion.bits
  when(state === waitResult) {
    result.warpId := resultWarp
    result.issueId := tasks(resultWarp).issueId
    result.destinationVd := tasks(resultWarp).destination
    result.completionMask := tasks(resultWarp).mask
    for (parentLane <- 0 until num_thread) {
      val bank = parentLane % banks
      val sources = (0 until columns).map(col => bank + col * banks)
      val matches = sources.map(slot => io.schedulerCompletion.bits.completionMask(slot) &&
        slotOwner(slot) === resultWarp && slotColumn(slot) === (parentLane / banks).U)
      result.laneStatus(parentLane) := Mux(built.grouped,
        Mux1H(matches.zip(sources.map(io.schedulerCompletion.bits.laneStatus(_)))),
        io.schedulerCompletion.bits.laneStatus(parentLane))
    }
  }
  io.completion.valid := (state === direct || state === waitResult) && io.schedulerCompletion.valid
  io.completion.bits := result
  val last = PopCount(remaining) === 1.U
  io.schedulerCompletion.ready := io.completion.ready &&
    (state === direct || (state === waitResult && last))
  when(io.completion.fire) {
    when(state === waitResult) {
      assert(io.schedulerCompletion.bits.contextReleased,
        "queued/batched tasks require global spill and physical context release")
      remaining := remaining & ~UIntToOH(resultWarp, num_warp)
      when(last) { state := idle }
    }.otherwise { state := idle }
  }
}

/** First published ray stays in its shader warp; later fresh rays may batch.
  * Publication is the software TraceRequest status store after header writes,
  * not recursion depth: raygen-loop reflections also have active level zero.
  * Candidate resume does not publish a header and retains the classification.
  */
class RtParentRayHistory extends Module {
  val io = IO(new Bundle {
    val allocated = Flipped(Valid(UInt(depth_warp.W)))
    val published = Flipped(Valid(new Bundle {
      val warpId = UInt(depth_warp.W)
      val mask = UInt(num_thread.W)
    }))
    val secondary = Output(Vec(num_warp, UInt(num_thread.W)))
  })
  val seen = RegInit(VecInit(Seq.fill(num_warp)(0.U(num_thread.W))))
  val secondary = RegInit(VecInit(Seq.fill(num_warp)(0.U(num_thread.W))))
  io.secondary := secondary
  when(io.published.valid) {
    val wid = io.published.bits.warpId
    val mask = io.published.bits.mask
    secondary(wid) := (secondary(wid) & ~mask) | (seen(wid) & mask)
    seen(wid) := seen(wid) | mask
  }
  when(io.allocated.valid) {
    seen(io.allocated.bits) := 0.U
    secondary(io.allocated.bits) := 0.U
  }
}

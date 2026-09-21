package rtcore

import chisel3._
import chisel3.util._
import top.parameters._

object RtTraversalStackParameters {
  val nodeRefWidth = 32
  val prototypeDepth = 32
  val overflowStacks = 4
  val overflowDepth = 32
}

class RtTraversalStackInit extends Bundle {
  val laneMask = UInt(num_thread.W)
  val rootNodeRef = Vec(num_thread, UInt(RtTraversalStackParameters.nodeRefWidth.W))
  // A root fetched from the command AS is always a world-ray node.  Keeping
  // this beside the ref (rather than one mutable lane-global bit) is what
  // lets DFS return from a BLAS to a sibling TLAS node correctly.
  val objectRay = Vec(num_thread, Bool())
}

class RtTraversalStackPush extends Bundle {
  val laneMask = UInt(num_thread.W)
  val nodeRef = Vec(num_thread, UInt(RtTraversalStackParameters.nodeRefWidth.W))
  val objectRay = Vec(num_thread, Bool())
}

class RtTraversalStackPop extends Bundle {
  val laneMask = UInt(num_thread.W)
}

class RtTraversalStackClear extends Bundle {
  val laneMask = UInt(num_thread.W)
}

/**
  * Per-lane RT traversal continuation SRAM.
  *
  * This is intentionally only a stack.  RtCoreScheduler owns node decoding,
  * DFS policy, and the multi-cycle Box4 deferred-child sequence.  An accepted
  * Push accepts a subset of requested lanes, reported by pushAcceptedMask.
  * Four shared 32-entry overflow stacks extend at most four lanes at a time;
  * each lane may own only one. Init, clear and pop remain masked atomic ops.
  */
class RtTraversalStack(stackDepth: Int = RtTraversalStackParameters.prototypeDepth)
    extends Module {
  require(stackDepth > 0)

  private val stackPointerWidth = log2Ceil(stackDepth + 1)
  private val stackEntryIndexWidth = math.max(1, log2Ceil(stackDepth))
  private val nodeRefWidth = RtTraversalStackParameters.nodeRefWidth
  private val overflowStacks = RtTraversalStackParameters.overflowStacks
  private val overflowDepth = RtTraversalStackParameters.overflowDepth

  val io = IO(new Bundle {
    val init = Flipped(Decoupled(new RtTraversalStackInit))
    val push = Flipped(Decoupled(new RtTraversalStackPush))
    val pop = Flipped(Decoupled(new RtTraversalStackPop))
    val clear = Flipped(Decoupled(new RtTraversalStackClear))

    // popNodeRef is valid while pop.valid is asserted and must be sampled on
    // pop.fire.  It is combinational because the scheduler already selects the
    // warp before issuing the pop.
    val popNodeRef = Output(Vec(num_thread, UInt(nodeRefWidth.W)))
    val popObjectRay = Output(Vec(num_thread, Bool()))

    val laneLive = Output(UInt(num_thread.W))
    val laneEmpty = Output(UInt(num_thread.W))
    val laneFull = Output(UInt(num_thread.W))
    val pushWouldOverflow = Output(Bool())
    // On push.fire only these lanes were written. The caller retries the rest.
    val pushAcceptedMask = Output(UInt(num_thread.W))
    val popWouldUnderflow = Output(Bool())
  })

  // At the default depth this is one warp x 32 lanes x 32 entries.  The tag
  // adds one bit per entry and prevents an object-space BLAS ray from being
  // accidentally reused for a later world-space TLAS sibling.
  val entries = Reg(Vec(num_thread, Vec(stackDepth, UInt(nodeRefWidth.W))))
  val objectEntries = Reg(Vec(num_thread, Vec(stackDepth, Bool())))
  val stackPointer = RegInit(VecInit(Seq.fill(num_thread)(0.U(stackPointerWidth.W))))
  val live = RegInit(VecInit(Seq.fill(num_thread)(false.B)))
  // Each bank has one owner, one write port and one top read. Store the tag
  // together with the node ref so pool reuse cannot change ray-space identity.
  val overflowData = Reg(Vec(overflowStacks, Vec(overflowDepth, UInt((nodeRefWidth + 1).W))))
  val overflowCount = RegInit(VecInit(Seq.fill(overflowStacks)(0.U(log2Ceil(overflowDepth + 1).W))))
  val overflowOwner = Reg(Vec(overflowStacks, UInt(log2Ceil(num_thread).W)))
  val owned = VecInit((0 until num_thread).map(lane => VecInit((0 until overflowStacks).map(bank =>
    overflowCount(bank) =/= 0.U && overflowOwner(bank) === lane.U)).asUInt))
  val baseRoom = VecInit((0 until num_thread).map(lane =>
    live(lane) && stackPointer(lane) < stackDepth.U)).asUInt
  val needsBank = VecInit((0 until num_thread).map(lane =>
    live(lane) && stackPointer(lane) === stackDepth.U && !owned(lane).orR)).asUInt
  // Fixed lane priority across the four free banks, without a 32-stage chain.
  val remaining = Wire(Vec(overflowStacks + 1, UInt(num_thread.W)))
  val bankPushLane = Wire(Vec(overflowStacks, UInt(log2Ceil(num_thread).W)))
  val bankPushMask = Wire(Vec(overflowStacks, UInt(num_thread.W)))
  remaining(0) := io.push.bits.laneMask & needsBank
  for (bank <- 0 until overflowStacks) {
    val free = overflowCount(bank) === 0.U
    val grant = Mux(free, PriorityEncoderOH(remaining(bank)), 0.U(num_thread.W))
    remaining(bank + 1) := remaining(bank) & ~grant
    bankPushLane(bank) := Mux(free, OHToUInt(grant), overflowOwner(bank))
    bankPushMask(bank) := Mux(free, grant,
      Mux(overflowCount(bank) < overflowDepth.U && io.push.bits.laneMask(overflowOwner(bank)),
        UIntToOH(overflowOwner(bank), num_thread), 0.U))
  }
  val pushEligible = (io.push.bits.laneMask & baseRoom) | bankPushMask.reduce(_ | _)

  val initLive = live.asUInt
  val popInvalid = VecInit((0 until num_thread).map { lane =>
    !live(lane) || stackPointer(lane) === 0.U
  }).asUInt

  val initAllowed = (io.init.bits.laneMask & initLive) === 0.U
  val popAllowed = (io.pop.bits.laneMask & popInvalid) === 0.U

  // Init/clear are whole-stack control operations.  Push and pop may proceed
  // together when they update disjoint lanes.
  val selectInit = io.init.valid && initAllowed
  val selectClear = !selectInit && io.clear.valid
  val controlBlocked = selectInit || selectClear
  val selectPush = !controlBlocked && io.push.valid &&
    (pushEligible.orR || !io.push.bits.laneMask.orR)
  io.pushAcceptedMask := Mux(selectPush, pushEligible, 0.U)
  // Push and pop are independent lane operations.  They may be accepted in
  // the same cycle when their lane masks do not overlap.
  val selectPop = !controlBlocked && io.pop.valid && popAllowed &&
    (!selectPush || (pushEligible & io.pop.bits.laneMask) === 0.U)
  io.init.ready := selectInit
  io.clear.ready := selectClear
  io.push.ready := selectPush
  io.pop.ready := selectPop

  for (lane <- 0 until num_thread) {
    val pointer = stackPointer(lane)
    if (stackDepth == 1) {
      io.popNodeRef(lane) := Mux(pointer === 0.U, 0.U,
        entries(lane)(0))
      io.popObjectRay(lane) := Mux(pointer === 0.U, false.B,
        objectEntries(lane)(0))
    } else {
      io.popNodeRef(lane) := Mux(pointer === 0.U, 0.U,
        entries(lane)(
          (pointer - 1.U)(stackEntryIndexWidth - 1, 0)
        ))
      io.popObjectRay(lane) := Mux(pointer === 0.U, false.B,
        objectEntries(lane)(
          (pointer - 1.U)(stackEntryIndexWidth - 1, 0)
        ))
    }
    when(owned(lane).orR) {
      val top = Mux1H((0 until overflowStacks).map(bank => owned(lane)(bank) ->
        overflowData(bank)((overflowCount(bank) - 1.U)(log2Ceil(overflowDepth) - 1, 0))))
      io.popNodeRef(lane) := top(nodeRefWidth - 1, 0)
      io.popObjectRay(lane) := top(nodeRefWidth)
    }
    assert(PopCount(owned(lane)) <= 1.U, "lane owns multiple overflow stacks")
  }

  when(io.init.fire) {
    for (lane <- 0 until num_thread) {
      when(io.init.bits.laneMask(lane)) {
        entries(lane)(0) := io.init.bits.rootNodeRef(lane)
        objectEntries(lane)(0) := io.init.bits.objectRay(lane)
        stackPointer(lane) := 1.U
        live(lane) := true.B
      }
    }
  }.elsewhen(io.clear.fire) {
    for (lane <- 0 until num_thread) {
      when(io.clear.bits.laneMask(lane)) {
        stackPointer(lane) := 0.U
        live(lane) := false.B
      }
    }
  }
  when(io.push.fire) {
    for (lane <- 0 until num_thread) {
      when(io.pushAcceptedMask(lane) && baseRoom(lane)) {
        val pointer = stackPointer(lane)
        if (stackDepth == 1) {
          entries(lane)(0) := io.push.bits.nodeRef(lane)
          objectEntries(lane)(0) := io.push.bits.objectRay(lane)
        } else {
          entries(lane)(
            pointer(stackEntryIndexWidth - 1, 0)
          ) := io.push.bits.nodeRef(lane)
          objectEntries(lane)(
            pointer(stackEntryIndexWidth - 1, 0)
          ) := io.push.bits.objectRay(lane)
        }
        stackPointer(lane) := pointer + 1.U
      }
    }
  }
  when(io.pop.fire) {
    for (lane <- 0 until num_thread) {
      when(io.pop.bits.laneMask(lane) && !owned(lane).orR) {
        stackPointer(lane) := stackPointer(lane) - 1.U
      }
    }
  }

  for (bank <- 0 until overflowStacks) {
    val owner = overflowOwner(bank)
    when(overflowCount(bank) =/= 0.U) {
      assert(live(owner) && stackPointer(owner) === stackDepth.U,
        "overflow stack must extend a live full base stack")
    }
    when(io.clear.fire && io.clear.bits.laneMask(owner)) {
      overflowCount(bank) := 0.U
    }.elsewhen(io.push.fire && bankPushMask(bank).orR) {
      val lane = bankPushLane(bank)
      overflowData(bank)(overflowCount(bank)(log2Ceil(overflowDepth) - 1, 0)) :=
        Cat(io.push.bits.objectRay(lane), io.push.bits.nodeRef(lane))
      overflowOwner(bank) := lane
      overflowCount(bank) := overflowCount(bank) + 1.U
    }.elsewhen(io.pop.fire && overflowCount(bank) =/= 0.U && io.pop.bits.laneMask(owner)) {
      // The last pop releases the bank; allocation sees it free next cycle.
      overflowCount(bank) := overflowCount(bank) - 1.U
    }
  }

  val statusLive = live.asUInt
  val statusEmpty = VecInit((0 until num_thread).map { lane =>
    live(lane) && stackPointer(lane) === 0.U
  }).asUInt
  val anyFreeBank = overflowCount.map(_ === 0.U).reduce(_ || _)
  val statusFull = VecInit((0 until num_thread).map(lane => live(lane) &&
    stackPointer(lane) === stackDepth.U && Mux(owned(lane).orR,
      (0 until overflowStacks).map(bank => owned(lane)(bank) && overflowCount(bank) === overflowDepth.U).reduce(_ || _),
      !anyFreeBank))).asUInt
  io.laneLive := statusLive
  io.laneEmpty := statusEmpty
  io.laneFull := statusFull
  io.pushWouldOverflow := (io.push.bits.laneMask & ~pushEligible).orR
  io.popWouldUnderflow := (io.pop.bits.laneMask & popInvalid).orR
}

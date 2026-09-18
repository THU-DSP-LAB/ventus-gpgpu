package rtcore

import chisel3._
import chisel3.util._
import top.parameters._

object RtTraversalStackParameters {
  val nodeRefWidth = 32
  val prototypeDepth = 32
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
  * operation affects one warp and any subset of its lanes atomically.
  */
class RtTraversalStack(stackDepth: Int = RtTraversalStackParameters.prototypeDepth)
    extends Module {
  require(stackDepth > 0)

  private val stackPointerWidth = log2Ceil(stackDepth + 1)
  private val stackEntryIndexWidth = math.max(1, log2Ceil(stackDepth))
  private val nodeRefWidth = RtTraversalStackParameters.nodeRefWidth

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
    val popWouldUnderflow = Output(Bool())
  })

  // At the default depth this is one warp x 32 lanes x 32 entries.  The tag
  // adds one bit per entry and prevents an object-space BLAS ray from being
  // accidentally reused for a later world-space TLAS sibling.
  val entries = Reg(Vec(num_thread, Vec(stackDepth, UInt(nodeRefWidth.W))))
  val objectEntries = Reg(Vec(num_thread, Vec(stackDepth, Bool())))
  val stackPointer = RegInit(VecInit(Seq.fill(num_thread)(0.U(stackPointerWidth.W))))
  val live = RegInit(VecInit(Seq.fill(num_thread)(false.B)))

  val initLive = live.asUInt
  val pushInvalid = VecInit((0 until num_thread).map { lane =>
    !live(lane) || stackPointer(lane) === stackDepth.U
  }).asUInt
  val popInvalid = VecInit((0 until num_thread).map { lane =>
    !live(lane) || stackPointer(lane) === 0.U
  }).asUInt

  val initAllowed = (io.init.bits.laneMask & initLive) === 0.U
  val pushAllowed = (io.push.bits.laneMask & pushInvalid) === 0.U
  val popAllowed = (io.pop.bits.laneMask & popInvalid) === 0.U

  // Init/clear are whole-stack control operations.  Push and pop may proceed
  // together when they update disjoint lanes.
  val selectInit = io.init.valid && initAllowed
  val selectClear = !selectInit && io.clear.valid
  val controlBlocked = selectInit || selectClear
  val selectPush = !controlBlocked && io.push.valid && pushAllowed
  // Push and pop are independent lane operations.  They may be accepted in
  // the same cycle when their lane masks do not overlap.
  val selectPop = !controlBlocked && io.pop.valid && popAllowed &&
    ((io.push.bits.laneMask & io.pop.bits.laneMask) === 0.U)
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
      when(io.push.bits.laneMask(lane)) {
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
      when(io.pop.bits.laneMask(lane)) {
        stackPointer(lane) := stackPointer(lane) - 1.U
      }
    }
  }

  val statusLive = live.asUInt
  val statusEmpty = VecInit((0 until num_thread).map { lane =>
    live(lane) && stackPointer(lane) === 0.U
  }).asUInt
  val statusFull = VecInit(stackPointer.map(_ === stackDepth.U)).asUInt
  io.laneLive := statusLive
  io.laneEmpty := statusEmpty
  io.laneFull := statusFull
  io.pushWouldOverflow := (io.push.bits.laneMask & pushInvalid).orR
  io.popWouldUnderflow := (io.pop.bits.laneMask & popInvalid).orR
}

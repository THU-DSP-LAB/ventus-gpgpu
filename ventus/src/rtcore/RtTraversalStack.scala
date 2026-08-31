package rtcore

import chisel3._
import chisel3.util._
import top.parameters._

object RtTraversalStackParameters {
  val nodeRefWidth = 32
  val prototypeDepth = 32
}

class RtTraversalStackInit extends Bundle {
  val warpId = UInt(depth_warp.W)
  val laneMask = UInt(num_thread.W)
  val rootNodeRef = Vec(num_thread, UInt(RtTraversalStackParameters.nodeRefWidth.W))
}

class RtTraversalStackPush extends Bundle {
  val warpId = UInt(depth_warp.W)
  val laneMask = UInt(num_thread.W)
  val nodeRef = Vec(num_thread, UInt(RtTraversalStackParameters.nodeRefWidth.W))
}

class RtTraversalStackPop extends Bundle {
  val warpId = UInt(depth_warp.W)
  val laneMask = UInt(num_thread.W)
}

class RtTraversalStackClear extends Bundle {
  val warpId = UInt(depth_warp.W)
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

    val statusWarpId = Input(UInt(depth_warp.W))
    val laneLive = Output(UInt(num_thread.W))
    val laneEmpty = Output(UInt(num_thread.W))
    val laneFull = Output(UInt(num_thread.W))
    val pushWouldOverflow = Output(Bool())
    val popWouldUnderflow = Output(Bool())
  })

  // At the default depth this is 8 warps x 32 lanes x 32 entries x 32 bits,
  // or 32 KiB of continuation payload storage.
  val entries = Reg(Vec(num_warp, Vec(num_thread, Vec(stackDepth, UInt(nodeRefWidth.W)))))
  val stackPointer = RegInit(VecInit(Seq.fill(num_warp)(
    VecInit(Seq.fill(num_thread)(0.U(stackPointerWidth.W)))
  )))
  val live = RegInit(VecInit(Seq.fill(num_warp)(
    VecInit(Seq.fill(num_thread)(false.B))
  )))

  val initLive = live(io.init.bits.warpId).asUInt
  val pushInvalid = VecInit((0 until num_thread).map { lane =>
    !live(io.push.bits.warpId)(lane) ||
      stackPointer(io.push.bits.warpId)(lane) === stackDepth.U
  }).asUInt
  val popInvalid = VecInit((0 until num_thread).map { lane =>
    !live(io.pop.bits.warpId)(lane) ||
      stackPointer(io.pop.bits.warpId)(lane) === 0.U
  }).asUInt

  val initAllowed = (io.init.bits.laneMask & initLive) === 0.U
  val pushAllowed = (io.push.bits.laneMask & pushInvalid) === 0.U
  val popAllowed = (io.pop.bits.laneMask & popInvalid) === 0.U

  // One vector operation per cycle.  Invalid higher-priority requests do not
  // block a valid lower-priority request; their ready remains low.
  val selectInit = io.init.valid && initAllowed
  val selectClear = !selectInit && io.clear.valid
  val selectPush = !selectInit && !selectClear && io.push.valid && pushAllowed
  val selectPop = !selectInit && !selectClear && !selectPush && io.pop.valid && popAllowed
  io.init.ready := selectInit
  io.clear.ready := selectClear
  io.push.ready := selectPush
  io.pop.ready := selectPop

  for (lane <- 0 until num_thread) {
    val pointer = stackPointer(io.pop.bits.warpId)(lane)
    if (stackDepth == 1) {
      io.popNodeRef(lane) := Mux(pointer === 0.U, 0.U,
        entries(io.pop.bits.warpId)(lane)(0))
    } else {
      io.popNodeRef(lane) := Mux(pointer === 0.U, 0.U,
        entries(io.pop.bits.warpId)(lane)(
          (pointer - 1.U)(stackEntryIndexWidth - 1, 0)
        ))
    }
  }

  when(io.init.fire) {
    for (lane <- 0 until num_thread) {
      when(io.init.bits.laneMask(lane)) {
        entries(io.init.bits.warpId)(lane)(0) := io.init.bits.rootNodeRef(lane)
        stackPointer(io.init.bits.warpId)(lane) := 1.U
        live(io.init.bits.warpId)(lane) := true.B
      }
    }
  }.elsewhen(io.clear.fire) {
    for (lane <- 0 until num_thread) {
      when(io.clear.bits.laneMask(lane)) {
        stackPointer(io.clear.bits.warpId)(lane) := 0.U
        live(io.clear.bits.warpId)(lane) := false.B
      }
    }
  }.elsewhen(io.push.fire) {
    for (lane <- 0 until num_thread) {
      when(io.push.bits.laneMask(lane)) {
        val pointer = stackPointer(io.push.bits.warpId)(lane)
        if (stackDepth == 1) {
          entries(io.push.bits.warpId)(lane)(0) := io.push.bits.nodeRef(lane)
        } else {
          entries(io.push.bits.warpId)(lane)(
            pointer(stackEntryIndexWidth - 1, 0)
          ) := io.push.bits.nodeRef(lane)
        }
        stackPointer(io.push.bits.warpId)(lane) := pointer + 1.U
      }
    }
  }.elsewhen(io.pop.fire) {
    for (lane <- 0 until num_thread) {
      when(io.pop.bits.laneMask(lane)) {
        stackPointer(io.pop.bits.warpId)(lane) :=
          stackPointer(io.pop.bits.warpId)(lane) - 1.U
      }
    }
  }

  val statusLive = live(io.statusWarpId).asUInt
  val statusEmpty = VecInit((0 until num_thread).map { lane =>
    live(io.statusWarpId)(lane) && stackPointer(io.statusWarpId)(lane) === 0.U
  }).asUInt
  val statusFull = VecInit(stackPointer(io.statusWarpId).map(_ === stackDepth.U)).asUInt
  io.laneLive := statusLive
  io.laneEmpty := statusEmpty
  io.laneFull := statusFull
  io.pushWouldOverflow := (io.push.bits.laneMask & pushInvalid).orR
  io.popWouldUnderflow := (io.pop.bits.laneMask & popInvalid).orR
}

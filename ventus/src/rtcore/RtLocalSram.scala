package rtcore

import chisel3._
import chisel3.util._
import top.parameters._

/** One physical RT Local transaction: the same row is accessed in all 32
  * lane banks, yielding one field-major 128-byte vector. */
class RtLocalSramRequest extends Bundle {
  val warpId = UInt(depth_warp.W)
  val fieldId = UInt(6.W)
  val write = Bool()
  val laneMask = UInt(num_thread.W)
  val writeData = Vec(num_thread, UInt(xLen.W))
}

class RtLocalSramResponse extends Bundle {
  val readData = Vec(num_thread, UInt(xLen.W))
}

object RtLocalSramParameters {
  final val warpCount = num_warp
  final val fieldCount = RtAbiLayout.LocalFieldMajorFixedHeaderWordCount
  final val bankDepth = warpCount * fieldCount
  require(num_thread == 32)
  require(xLen == 32)
  require(bankDepth == 320)
}

/** Behavioural replacement for 32 single-port 320x32 SRAM macros.
  *
  * A request consumes the sole field-wide port.  Reads are synchronous and
  * writes have no response, matching the intended macro wrapper contract.
  */
class RtLocalSram extends Module {
  import RtLocalSramParameters._
  val io = IO(new Bundle {
    val request = Flipped(Decoupled(new RtLocalSramRequest))
    val response = Decoupled(new RtLocalSramResponse)
  })

  val banks = Seq.fill(num_thread)(SyncReadMem(bankDepth, UInt(xLen.W)))
  val readPending = RegInit(false.B)
  val address = io.request.bits.warpId * fieldCount.U + io.request.bits.fieldId
  val readEnable = io.request.fire && !io.request.bits.write
  val readData = Wire(Vec(num_thread, UInt(xLen.W)))
  for (lane <- 0 until num_thread) {
    readData(lane) := banks(lane).read(address, readEnable)
  }

  // A SyncReadMem result is exposed directly during the response cycle.
  // Do not start another access in that cycle: a back-to-back read lets the
  // next address advance the macro output before the current consumer has a
  // stable field-major response.
  io.request.ready := !readPending
  io.response.valid := readPending
  io.response.bits.readData := readData

  when(io.request.fire) {
    assert(io.request.bits.warpId < warpCount.U, "RT Local warp index out of range")
    assert(io.request.bits.fieldId < fieldCount.U, "RT Local field index out of range")
    when(io.request.bits.write) {
      for (lane <- 0 until num_thread) {
        when(io.request.bits.laneMask(lane)) {
          banks(lane).write(address, io.request.bits.writeData(lane))
        }
      }
    }.otherwise {
      readPending := true.B
    }
  }
  when(io.response.fire && !io.request.fire) { readPending := false.B }
}

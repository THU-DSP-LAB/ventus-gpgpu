package play.cache

import L1Cache.MyConfig
import chisel3._
import chisel3.util.Decoupled
import chiseltest._
import config.config.Parameters
import org.scalatest.freespec.AnyFreeSpec
import rtcore.{RtSpillTransfer, RtSpillTransferCommand}
import scala.collection.mutable

/** Real spill engine -> LSU -> production DCache, not a synthetic LSU ACK. */
class RtSpillCacheHarness(implicit p: Parameters) extends LsuDcacheScatterHarness {
  val spill = IO(new Bundle {
    val command = Flipped(Decoupled(new RtSpillTransferCommand))
    val result = Decoupled(new RtSpillTransferCommand)
  })
  val transfer = Module(new RtSpillTransfer)
  transfer.io.command <> spill.command
  spill.result <> transfer.io.result
  // Override the parent harness's disabled cacheline client connections.
  lsu.io.cachelineClientReq <> transfer.io.request
  transfer.io.response <> lsu.io.cachelineClientRsp
}

class RtSpillCacheEvictionTest extends AnyFreeSpec with ChiselScalatestTester {
  implicit private val p: Parameters = (new MyConfig).toInstance
  private def zero(data: Data): Unit = data match {
    case bool: Bool => bool.poke(false.B)
    case uint: UInt => uint.poke(0.U)
    case aggregate: Aggregate => aggregate.getElements.foreach(zero)
  }

  "fixed spill images survive real cache misses and capacity replacement" in {
    test(new RtSpillCacheHarness).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      zero(dut.io.req.bits)
      dut.io.req.valid.poke(false.B)
      dut.io.rsp.ready.poke(true.B)
      dut.io.memReq.ready.poke(true.B)
      dut.io.memRsp.valid.poke(false.B)
      zero(dut.io.memRsp.bits)
      dut.spill.command.valid.poke(false.B)
      zero(dut.spill.command.bits)
      dut.spill.result.ready.poke(false.B)
      dut.clock.setTimeout(0)
      dut.clock.step(2)
      val memory = mutable.Map.empty[BigInt, Vector[BigInt]]
      case class Reply(due: Int, source: BigInt, address: BigInt, read: Boolean,
                       data: Vector[BigInt])
      val replies = mutable.Queue.empty[Reply]
      var cycle = 0
      var pipeSequence = 0
      var pipeOutstanding = 0
      var pipePending = false
      def step(): Unit = {
        // Shader/PDS-like full-lane stores compete for the same LSU MSHRs
        // and cache sets, but target a distinct address range.
        if (!pipePending && pipeOutstanding < 2 && pipeSequence < 1024) {
          val request = dut.io.req.bits
          request.ctrl.isvec.poke(true.B)
          request.ctrl.mem_cmd.poke(2.U)
          request.ctrl.mem_whb.poke(3.U)
          request.ctrl.mem_unsigned.poke(true.B)
          request.ctrl.mop.poke(1.U)
          for (lane <- 0 until 32) {
            request.mask(lane).poke(true.B)
            request.in1(lane).poke((BigInt("71000000", 16) +
              (pipeSequence % 96) * 128 + lane * 4).U)
            request.in3(lane).poke((pipeSequence * 32 + lane).U)
          }
          dut.io.req.valid.poke(true.B)
          pipePending = true
        }
        val pipeAccepted = pipePending && dut.io.req.ready.peek().litToBoolean
        val pipeCompleted = dut.io.rsp.valid.peek().litToBoolean
        val response = replies.headOption.filter(_.due <= cycle)
        dut.io.memRsp.valid.poke(response.nonEmpty.B)
        response.foreach { r =>
          dut.io.memRsp.bits.d_opcode.poke((if (r.read) 1 else 0).U)
          dut.io.memRsp.bits.d_param.poke(0.U)
          dut.io.memRsp.bits.d_source.poke(r.source.U)
          dut.io.memRsp.bits.d_addr.poke(r.address.U)
          dut.io.memRsp.bits.d_data.zip(r.data).foreach { case (port, value) => port.poke(value.U) }
        }
        val consumed = response.nonEmpty && dut.io.memRsp.ready.peek().litToBoolean
        val request = if (dut.io.memReq.valid.peek().litToBoolean) {
          val port = dut.io.memReq.bits
          val address = port.a_addr.get.peek().litValue
          val opcode = port.a_opcode.peek().litValue.toInt
          assert(opcode == 4 || opcode == 0 || opcode == 1, s"unexpected opcode $opcode")
          val old = memory.getOrElse(address, Vector.fill(32)(BigInt(0)))
          if (opcode != 4) {
            val updated = old.indices.map { word =>
              val mask = port.a_mask(word).peek().litValue.toInt
              val data = port.a_data(word).peek().litValue
              (0 until 4).foldLeft(old(word)) { (value, byte) =>
                val bits = BigInt(255) << (byte * 8)
                if ((mask & (1 << byte)) != 0) (value & ~bits) | (data & bits) else value
              }
            }.toVector
            memory(address) = updated
          }
          Some(Reply(cycle + 5, port.a_source.peek().litValue, address, opcode == 4, old))
        } else None
        dut.clock.step()
        if (pipeAccepted) {
          dut.io.req.valid.poke(false.B)
          pipePending = false
          pipeSequence += 1
          pipeOutstanding += 1
        }
        if (pipeCompleted) pipeOutstanding -= 1
        if (consumed) replies.dequeue()
        request.foreach(replies.enqueue(_))
        cycle += 1
      }
      def transfer(slot: Int, restore: Boolean, generation: Int = 0): Unit = {
        val command = dut.spill.command.bits
        zero(command)
        command.pointer.poke((BigInt("70000000", 16) + slot * 384).U)
        command.restore.poke(restore.B)
        command.image.valid.poke(true.B)
        command.image.stackDepth.poke(2.U)
        command.image.instanceNodeRef.poke("hffffffff".U)
        val reference = generation * 4096 + slot * 16
        command.image.baseNodeRef(0).poke((reference + 1).U)
        command.image.baseNodeRef(1).poke((reference + 2).U)
        dut.spill.command.valid.poke(true.B)
        val deadline = cycle + 3000
        while (!dut.spill.command.ready.peek().litToBoolean && cycle < deadline) step()
        assert(cycle < deadline, s"command stalled for slot $slot")
        step()
        dut.spill.command.valid.poke(false.B)
        while (!dut.spill.result.valid.peek().litToBoolean && cycle < deadline) step()
        assert(cycle < deadline, s"transfer stalled for slot $slot restore=$restore")
        if (restore) {
          dut.spill.result.bits.image.valid.expect(true.B)
          dut.spill.result.bits.image.stackDepth.expect(2.U)
          dut.spill.result.bits.image.baseNodeRef(0).expect((reference + 1).U)
          dut.spill.result.bits.image.baseNodeRef(1).expect((reference + 2).U)
        }
        dut.spill.result.ready.poke(true.B)
        step()
        dut.spill.result.ready.poke(false.B)
      }
      // More distinct restored lines than the 32-set, two-way L1 capacity.
      for (slot <- 0 until 64) transfer(slot, restore = false)
      for (_ <- 0 until 2; slot <- 0 until 64) transfer(slot, restore = true)
      // Reuse still-resident slots with DIFFERENT data, then evict their dirty
      // lines. Identical rewrite data would hide dropped write-hit updates.
      for (slot <- 48 until 64) {
        transfer(slot, restore = false, generation = 1)
        transfer(slot, restore = true, generation = 1)
      }
      for (slot <- 0 until 64)
        transfer(slot, restore = true, generation = if (slot >= 48) 1 else 0)
      for (slot <- (0 until 64).reverse) {
        transfer(slot, restore = false, generation = 2)
        transfer(slot, restore = true, generation = 2)
      }
      for (slot <- 0 until 64) transfer(slot, restore = true, generation = 2)
    }
  }
}

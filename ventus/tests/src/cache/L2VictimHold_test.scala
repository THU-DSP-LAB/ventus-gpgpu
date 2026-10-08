package play.cache

import L2cache._
import chisel3._
import chisel3.util.Decoupled
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import scala.collection.mutable

/** The victim read must outlive write-buffer/external-memory backpressure. */
class L2VictimHoldHarness extends Module {
  private val params = top.parameters.l2cache_params
  val io = IO(new Bundle {
    val request = Flipped(Decoupled(new TLBundleD_lite_withid(params)))
    val writeback = Decoupled(new FullRequest(params))
    val fill = Flipped(Decoupled(new BankedStoreOuterAddress(params)))
    val fillData = Input(new BankedStoreOuterPoison(params))
  })
  val source = Module(new SourceD(params))
  val store = Module(new BankedStore(params))
  source.io.req <> io.request
  io.writeback <> source.io.a
  source.io.d.ready := true.B
  source.io.pb_pop.ready := true.B
  source.io.pb_beat := 0.U.asTypeOf(source.io.pb_beat)
  store.io.sinkD_adr <> io.fill
  store.io.sinkD_dat := io.fillData
  store.io.sourceD_radr <> source.io.bs_radr
  source.io.bs_rdat := store.io.sourceD_rdat
  store.io.sourceD_wadr <> source.io.bs_wadr
  store.io.sourceD_wdat := source.io.bs_wdat
}

class L2VictimHoldTest extends AnyFreeSpec with ChiselScalatestTester {
  private def zero(data: Data): Unit = data match {
    case b: Bool => b.poke(false.B)
    case u: UInt => u.poke(0.U)
    case a: Aggregate => a.getElements.foreach(zero)
  }

  "a dirty victim stays intact while its writeback is stalled" in {
    test(new L2VictimHoldHarness).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      zero(dut.io.request.bits)
      zero(dut.io.fill.bits)
      zero(dut.io.fillData)
      dut.io.request.valid.poke(false.B)
      dut.io.fill.valid.poke(false.B)
      dut.io.writeback.ready.poke(false.B)
      dut.clock.step(2)
      val image = (0 until 32).foldLeft(BigInt(0)) { (bits, word) =>
        bits | (BigInt(word + 1) << (word * 32))
      }
      dut.io.fill.bits.mask.poke(((BigInt(1) << 128) - 1).U)
      dut.io.fillData.data.poke(image.U)
      dut.io.fill.valid.poke(true.B)
      dut.clock.step()
      dut.io.fill.valid.poke(false.B)
      dut.clock.step(2)
      dut.io.request.bits.opcode.poke(4.U) // Get causes dirty-victim writeback.
      dut.io.request.bits.dirty.poke(true.B)
      dut.io.request.bits.mask.poke(((BigInt(1) << 128) - 1).U)
      dut.io.request.valid.poke(true.B)
      dut.io.request.ready.expect(true.B)
      dut.clock.step()
      dut.io.request.valid.poke(false.B)
      for (_ <- 0 until 8) {
        dut.io.writeback.valid.expect(true.B)
        dut.io.writeback.bits.data.expect(image.U)
        dut.clock.step()
      }
      dut.io.writeback.ready.poke(true.B)
      dut.io.writeback.bits.data.expect(image.U)
      dut.clock.step()
    }
  }

  "the production L2 preserves rewritten spill lines across flush and eviction" in {
    val params = top.parameters.l2cache_params
    test(new Scheduler(params)).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      zero(dut.io.in_a.bits)
      zero(dut.io.out_d.bits)
      dut.io.in_a.valid.poke(false.B)
      dut.io.in_d.ready.poke(true.B)
      dut.io.out_a.ready.poke(true.B)
      dut.io.out_d.valid.poke(false.B)
      dut.clock.setTimeout(0)
      dut.clock.step(params.cache.sets + 4)
      val memory = mutable.Map.empty[BigInt, BigInt]
      case class Reply(due: Int, source: BigInt, read: Boolean, data: BigInt)
      val replies = mutable.Queue.empty[Reply]
      val completed = mutable.Queue.empty[BigInt]
      var cycle = 0
      def step(): Unit = {
        dut.io.out_a.ready.poke((replies.size < 5).B)
        val response = replies.headOption.filter(_.due <= cycle)
        dut.io.out_d.valid.poke(response.nonEmpty.B)
        response.foreach { r =>
          dut.io.out_d.bits.opcode.poke((if (r.read) 1 else 0).U)
          dut.io.out_d.bits.source.poke(r.source.U)
          dut.io.out_d.bits.data.poke(r.data.U)
        }
        val responseFire = response.nonEmpty && dut.io.out_d.ready.peek().litToBoolean
        val request = if (dut.io.out_a.valid.peek().litToBoolean &&
                          dut.io.out_a.ready.peek().litToBoolean) {
          val port = dut.io.out_a.bits
          val address = port.address.peek().litValue
          val read = port.opcode.peek().litValue == 4
          val old = memory.getOrElse(address, BigInt(0))
          if (!read) {
            val data = port.data.peek().litValue
            val mask = port.mask.peek().litValue
            memory(address) = (0 until 128).foldLeft(old) { (value, byte) =>
              val bits = BigInt(255) << (byte * 8)
              if (mask.testBit(byte)) (value & ~bits) | (data & bits) else value
            }
          }
          Some(Reply(cycle + 100, port.source.peek().litValue, read, old))
        } else None
        if (dut.io.in_d.valid.peek().litToBoolean)
          completed.enqueue(dut.io.in_d.bits.data.peek().litValue)
        dut.clock.step()
        if (responseFire) replies.dequeue()
        request.foreach(replies.enqueue(_))
        cycle += 1
      }
      def operation(address: BigInt, write: Boolean, data: BigInt,
                    flush: Boolean = false, invalidate: Boolean = false): Unit = {
        zero(dut.io.in_a.bits)
        dut.io.in_a.bits.address.poke(address.U)
        dut.io.in_a.bits.opcode.poke((if (flush) 5 else if (write) 1 else 4).U)
        dut.io.in_a.bits.param.poke((if (invalidate) 1 else 0).U)
        dut.io.in_a.bits.mask.poke(((BigInt(1) << 128) - 1).U)
        dut.io.in_a.bits.data.poke(data.U)
        dut.io.in_a.valid.poke(true.B)
        val deadline = cycle + (if (flush) 200000 else 3000)
        while (!dut.io.in_a.ready.peek().litToBoolean && cycle < deadline) step()
        assert(cycle < deadline, s"L2 request stalled at $address")
        step()
        dut.io.in_a.valid.poke(false.B)
        while (completed.isEmpty && cycle < deadline) step()
        assert(completed.nonEmpty, s"L2 response stalled at $address")
        val actual = completed.dequeue()
        if (!write && !flush)
          assert(actual == data, s"L2 corrupt data at $address: $actual != $data")
      }
      val base = BigInt("9007f000", 16)
      def image(line: Int, generation: Int): BigInt =
        BigInt(line + 1 + generation * 4096) | (BigInt(1) << 96)
      // Populate every L2 set/way, then update its resident lines (dirty hits).
      for (line <- 0 until 512) {
        operation(base + line * 128, write = true, image(line, 0))
        operation(base + line * 128, write = false, image(line, 0))
        operation(base + line * 128, write = true, image(line, 1))
      }
      operation(base, write = false, BigInt(0), flush = true)
      // HintAck may precede the final entries leaving the eight-entry write
      // buffer. Drain them against the five-entry, 100-cycle memory model.
      for (_ <- 0 until 1000) step()
      for (line <- 0 until 512)
        assert(memory(base + line * 128) == image(line, 1),
          s"flush overwrote dirty line $line with ${memory(base + line * 128)}")
      // Force replacement, including unrelated all-zero shader/PDS-like stores.
      for (line <- 512 until 1152) {
        operation(base + line * 128, write = true, BigInt(0))
        operation(base + line * 128, write = false, BigInt(0))
      }
      for (line <- 0 until 512)
        operation(base + line * 128, write = false, image(line, 1))
      // Sparse dirty rows model ordinary continuation: Page 2 is reserved
      // but not loaded. Flush+invalidate must not pair a clean/empty way's
      // zero data with a neighbouring valid spill page's address.
      for (slot <- 0 until 32; page <- 0 until 3) {
        val address = base + slot * 384 + page * 128
        val value = if (page == 2) BigInt(0) else image(slot * 3 + page, 2)
        operation(address, write = true, value)
        if (page != 2) operation(address, write = false, value)
      }
      operation(base, write = false, BigInt(0), flush = true, invalidate = true)
      for (_ <- 0 until 1000) step()
      for (slot <- 0 until 32; page <- 0 until 2)
        operation(base + slot * 384 + page * 128,
          write = false, image(slot * 3 + page, 2))
      // Repeat an all-clean scan, including its final HintAck marker.
      operation(base, write = false, BigInt(0), flush = true, invalidate = true)
    }
  }
}

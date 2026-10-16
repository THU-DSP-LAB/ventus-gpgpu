package rtcore

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import scala.collection.mutable

class RtInstanceTransformTest extends AnyFreeSpec with ChiselScalatestTester {
  private def raw(x: Float): BigInt = BigInt(java.lang.Float.floatToRawIntBits(x).toLong & 0xffffffffL)
  private case class Job(origin: Seq[Float], direction: Seq[Float], matrix: Seq[Float])
  private def transform(j: Job, direction: Boolean): Seq[Float] = (0 until 3).map { row =>
    val v = if (direction) j.direction else j.origin
    val a: Float = j.matrix(row * 4) * v(0)
    val b: Float = j.matrix(row * 4 + 1) * v(1)
    val c: Float = j.matrix(row * 4 + 2) * v(2)
    val ab: Float = a + b
    val abc: Float = ab + c
    if (direction) abc else abc + j.matrix(row * 4 + 3)
  }
  private def put(v: Float3, xs: Seq[Float]): Unit =
    Seq(v.x, v.y, v.z).zip(xs).foreach { case (port, x) => port.poke(raw(x).U) }
  private def check(v: Float3, xs: Seq[Float]): Unit =
    Seq(v.x, v.y, v.z).zip(xs).foreach { case (port, x) => port.expect(raw(x).U) }
  private def drive(dut: RtInstanceTransform, j: Job, id: Int): Unit = {
    dut.io.in.bits.laneId.poke((id % 32).U)
    put(dut.io.in.bits.ray.origin, j.origin)
    put(dut.io.in.bits.ray.dir, j.direction)
    put(dut.io.in.bits.ray.inv, Seq.fill(3)(0f))
    put(dut.io.in.bits.ray.shear, Seq.fill(3)(0f))
    dut.io.in.bits.ray.kx.poke(0.U); dut.io.in.bits.ray.ky.poke(1.U); dut.io.in.bits.ray.kz.poke(2.U)
    dut.io.in.bits.ray.originalTmin.poke(raw(id.toFloat / 16).U)
    dut.io.in.bits.ray.extent.poke(raw(1000f + id).U)
    dut.io.in.bits.matrix.zip(j.matrix).foreach { case (p, x) => p.poke(raw(x).U) }
  }

  "instance row pipeline preserves arithmetic, ownership and backpressure" in {
    test(new RtInstanceTransform).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      val random = new scala.util.Random(0x1a57L)
      def value(): Float = (random.nextFloat() - 0.5f) * 8f
      val identity = Seq(1f,0f,0f,0f, 0f,1f,0f,0f, 0f,0f,1f,0f)
      // Mix normal and zero denominators so the three dividers finish on
      // different cycles. Fuzz preserves the original separate-rounding order.
      val jobs = Seq(Job(Seq(1f,2f,3f),Seq(0f,1f,-2f),identity),
        Job(Seq(-3f,0.25f,7f),Seq(1f,0f,0f),identity.updated(3,5f))) ++
        Seq.fill(126)(Job(Seq.fill(3)(value()),Seq.fill(3)(value()),Seq.fill(12)(value())))
      val expected = mutable.Queue.empty[Int]
      val acceptedCycles = mutable.ArrayBuffer.empty[Int]
      var sent = 0; var received = 0; var cycle = 0
      dut.io.in.valid.poke(false.B); dut.io.out.ready.poke(false.B)
      dut.reset.poke(true.B); dut.clock.step(2); dut.reset.poke(false.B)
      while (received < jobs.size && cycle < 7000) {
        val ready = cycle >= 100 && (cycle >= 6000 || random.nextBoolean())
        dut.io.out.ready.poke(ready.B)
        dut.io.in.valid.poke((sent < jobs.size).B)
        if (sent < jobs.size) drive(dut, jobs(sent), sent)
        if (dut.io.out.valid.peek().litToBoolean) {
          assert(expected.nonEmpty, "unexpected or duplicated result")
          val id = expected.front; val j = jobs(id)
          val direction = transform(j, direction = true)
          dut.io.out.bits.laneId.expect((id % 32).U)
          check(dut.io.out.bits.rayState.origin, transform(j, direction = false))
          check(dut.io.out.bits.rayState.dir, direction)
          check(dut.io.out.bits.rayState.inv, direction.map(x => 1f / x))
          dut.io.out.bits.rayState.tmin.expect(raw(id.toFloat / 16).U)
          dut.io.out.bits.rayState.tmax.expect(raw(1000f + id).U)
          if (ready) { expected.dequeue(); received += 1 }
        }
        if (sent < jobs.size && dut.io.in.ready.peek().litToBoolean) {
          expected.enqueue(sent); acceptedCycles += cycle; sent += 1
        }
        if (cycle == 99) {
          assert(sent == 3, "one reciprocal owner plus two reserved matrix jobs")
          dut.io.in.ready.expect(false.B)
        }
        dut.clock.step(); cycle += 1
      }
      assert(sent == jobs.size && received == jobs.size && expected.isEmpty)
      assert(acceptedCycles.take(3).toSeq == Seq(0,6,12), "matrix rows must overlap adjacent lanes")
      dut.io.in.valid.poke(false.B); dut.io.out.ready.poke(true.B)
      dut.clock.step(3); dut.io.out.valid.expect(false.B)
      // Clear pending matrix rows, reserved FIFO entries and iterative divisions.
      drive(dut, jobs.head, 0); dut.io.in.valid.poke(true.B)
      dut.io.out.ready.poke(false.B); dut.clock.step(15)
      dut.reset.poke(true.B); dut.clock.step(2); dut.reset.poke(false.B)
      dut.io.in.valid.poke(false.B); dut.io.out.ready.poke(true.B)
      for (_ <- 0 until 50) { dut.io.out.valid.expect(false.B); dut.clock.step() }
    }
  }
}

package rtcore

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

import scala.util.Random

class RtFpUnitsTest extends AnyFreeSpec with ChiselScalatestTester {
  private def raw(value: Float): BigInt =
    BigInt(java.lang.Float.floatToRawIntBits(value).toLong & 0xffffffffL)

  private def float(rawValue: BigInt): Float =
    java.lang.Float.intBitsToFloat(rawValue.toInt)

  private def waitUntil(limit: Int)(condition: => Boolean)(step: => Unit): Unit = {
    var remaining = limit
    while (!condition && remaining > 0) {
      step
      remaining -= 1
    }
    assert(condition, s"condition did not become true within $limit cycles")
  }

  private def reset(dut: RtRaySetup): Unit = {
    dut.reset.poke(true.B)
    dut.clock.step(2)
    dut.reset.poke(false.B)
  }

  private def reset(dut: RtTriangleResolve): Unit = {
    dut.reset.poke(true.B)
    dut.clock.step(2)
    dut.reset.poke(false.B)
  }

  private def pokeSetup(
      dut: RtRaySetup,
      lane: Int,
      origin: Seq[Float],
      direction: Seq[Float],
      tmax: Float
  ): Unit = {
    dut.io.in.bits.warpId.poke(0.U)
    dut.io.in.bits.laneId.poke(lane.U)
    dut.io.in.bits.accelLo.poke(0x8000.U)
    dut.io.in.bits.accelHi.poke(0x9000.U)
    dut.io.in.bits.traceMeta0.poke(0x1111.U)
    dut.io.in.bits.traceMeta1.poke(0x2222.U)
    dut.io.in.bits.rayState.origin.x.poke(raw(origin(0)).U)
    dut.io.in.bits.rayState.origin.y.poke(raw(origin(1)).U)
    dut.io.in.bits.rayState.origin.z.poke(raw(origin(2)).U)
    dut.io.in.bits.rayState.dir.x.poke(raw(direction(0)).U)
    dut.io.in.bits.rayState.dir.y.poke(raw(direction(1)).U)
    dut.io.in.bits.rayState.dir.z.poke(raw(direction(2)).U)
    dut.io.in.bits.rayState.tmin.poke(raw(0.125f).U)
    dut.io.in.bits.rayState.tmax.poke(raw(tmax).U)
  }

  private def expectedAxes(direction: Seq[Float]): (Int, Int, Int) = {
    val magnitude = direction.map(value => raw(value) & 0x7fffffffL)
    val kz = if (magnitude(0) >= magnitude(1) && magnitude(0) >= magnitude(2)) 0
    else if (magnitude(1) >= magnitude(2)) 1 else 2
    val positiveKx = if (kz == 2) 0 else kz + 1
    val positiveKy = if (positiveKx == 2) 0 else positiveKx + 1
    if (java.lang.Float.floatToRawIntBits(direction(kz)) < 0)
      (positiveKy, positiveKx, kz)
    else
      (positiveKx, positiveKy, kz)
  }

  private def checkSetupResult(
      dut: RtRaySetup,
      lane: Int,
      origin: Seq[Float],
      direction: Seq[Float],
      tmax: Float
  ): Unit = {
    val (kx, ky, kz) = expectedAxes(direction)
    val reciprocal = direction.map(value => 1.0f / value)
    dut.io.out.bits.warpId.expect(0.U)
    dut.io.out.bits.laneId.expect(lane.U)
    dut.io.out.bits.accelLo.expect(0x8000.U)
    dut.io.out.bits.accelHi.expect(0x9000.U)
    dut.io.out.bits.traceMeta0.expect(0x1111.U)
    dut.io.out.bits.traceMeta1.expect(0x2222.U)
    dut.io.out.bits.ray.origin.x.expect(raw(origin(0)).U)
    dut.io.out.bits.ray.origin.y.expect(raw(origin(1)).U)
    dut.io.out.bits.ray.origin.z.expect(raw(origin(2)).U)
    dut.io.out.bits.ray.dir.x.expect(raw(direction(0)).U)
    dut.io.out.bits.ray.dir.y.expect(raw(direction(1)).U)
    dut.io.out.bits.ray.dir.z.expect(raw(direction(2)).U)
    dut.io.out.bits.ray.inv.x.expect(raw(reciprocal(0)).U)
    dut.io.out.bits.ray.inv.y.expect(raw(reciprocal(1)).U)
    dut.io.out.bits.ray.inv.z.expect(raw(reciprocal(2)).U)
    dut.io.out.bits.ray.extent.expect(raw(tmax).U)
    dut.io.out.bits.ray.kx.expect(kx.U)
    dut.io.out.bits.ray.ky.expect(ky.U)
    dut.io.out.bits.ray.kz.expect(kz.U)
    dut.io.out.bits.ray.shear.x.expect(raw(direction(kx) * reciprocal(kz)).U)
    dut.io.out.bits.ray.shear.y.expect(raw(direction(ky) * reciprocal(kz)).U)
    dut.io.out.bits.ray.shear.z.expect(raw(reciprocal(kz)).U)
  }

  private def submitSetup(
      dut: RtRaySetup,
      lane: Int,
      origin: Seq[Float],
      direction: Seq[Float],
      tmax: Float
  ): Unit = {
    pokeSetup(dut, lane, origin, direction, tmax)
    dut.io.in.valid.poke(true.B)
    waitUntil(4)(dut.io.in.ready.peek().litToBoolean)(dut.clock.step())
    dut.clock.step()
    dut.io.in.valid.poke(false.B)
  }

  "RaySetup matches IEEE reciprocal, axis selection, and shear" in {
    test(new RtRaySetup).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      reset(dut)
      dut.io.in.valid.poke(false.B)
      dut.io.out.ready.poke(true.B)
      val directed = Seq(
        (Seq(1.0f, -2.0f, 3.0f), Seq(4.0f, 1.0f, -2.0f), 12.0f),
        (Seq(-1.0f, 2.0f, 0.5f), Seq(-1.0f, 5.0f, 2.0f), 7.5f),
        (Seq(0.25f, -0.5f, 1.0f), Seq(1.0f, -2.0f, -8.0f), 16.0f)
      )
      val random = new Random(0x5eedc0deL)
      val vectors = directed ++ (0 until 64).map { _ =>
        def finiteNonZero(): Float = {
          val magnitude = 0.125f + random.nextFloat() * 31.0f
          if (random.nextBoolean()) magnitude else -magnitude
        }
        (Seq(finiteNonZero(), finiteNonZero(), finiteNonZero()),
          Seq(finiteNonZero(), finiteNonZero(), finiteNonZero()),
          0.25f + random.nextFloat() * 100.0f)
      }
      vectors.zipWithIndex.foreach { case ((origin, direction, tmax), lane) =>
        submitSetup(dut, lane & 31, origin, direction, tmax)
        waitUntil(300)(dut.io.out.valid.peek().litToBoolean)(dut.clock.step())
        checkSetupResult(dut, lane & 31, origin, direction, tmax)
        dut.clock.step()
      }
    }
  }

  "RaySetup holds its result under backpressure" in {
    test(new RtRaySetup).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      reset(dut)
      dut.io.in.valid.poke(false.B)
      dut.io.out.ready.poke(false.B)
      val origin = Seq(1.0f, 2.0f, 3.0f)
      val direction = Seq(2.0f, -4.0f, 8.0f)
      submitSetup(dut, 3, origin, direction, 20.0f)
      waitUntil(300)(dut.io.out.valid.peek().litToBoolean)(dut.clock.step())
      checkSetupResult(dut, 3, origin, direction, 20.0f)
      dut.clock.step(3)
      dut.io.out.valid.expect(true.B)
      checkSetupResult(dut, 3, origin, direction, 20.0f)
      dut.io.out.ready.poke(true.B)
      dut.clock.step()
      dut.io.out.valid.expect(false.B)
    }
  }

  private def pokeResolve(
      dut: RtTriangleResolve,
      requestId: Int,
      hit: Boolean,
      u: Float,
      v: Float,
      t: Float,
      denominator: Float
  ): Unit = {
    dut.io.in.bits.requestId.poke(requestId.U)
    dut.io.in.bits.hit.poke(hit.B)
    dut.io.in.bits.uNum.poke(raw(u).U)
    dut.io.in.bits.vNum.poke(raw(v).U)
    dut.io.in.bits.tNum.poke(raw(t).U)
    dut.io.in.bits.tDenom.poke(raw(denominator).U)
  }

  private def submitResolve(
      dut: RtTriangleResolve,
      requestId: Int,
      hit: Boolean,
      u: Float,
      v: Float,
      t: Float,
      denominator: Float
  ): Unit = {
    pokeResolve(dut, requestId, hit, u, v, t, denominator)
    dut.io.in.valid.poke(true.B)
    waitUntil(4)(dut.io.in.ready.peek().litToBoolean)(dut.clock.step())
    dut.clock.step()
    dut.io.in.valid.poke(false.B)
  }

  private def checkResolve(
      dut: RtTriangleResolve,
      requestId: Int,
      hit: Boolean,
      u: Float,
      v: Float,
      t: Float,
      denominator: Float
  ): Unit = {
    dut.io.out.bits.requestId.expect(requestId.U)
    dut.io.out.bits.hit.expect(hit.B)
    if (hit) {
      dut.io.out.bits.u.expect(raw(u / denominator).U)
      dut.io.out.bits.v.expect(raw(v / denominator).U)
      dut.io.out.bits.t.expect(raw(t / denominator).U)
    } else {
      dut.io.out.bits.u.expect(0.U)
      dut.io.out.bits.v.expect(0.U)
      dut.io.out.bits.t.expect(0.U)
    }
  }

  "TriangleResolve performs only accepted-hit divisions and preserves ownership" in {
    test(new RtTriangleResolve).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      reset(dut)
      dut.io.in.valid.poke(false.B)
      dut.io.out.ready.poke(true.B)
      val directed = Seq(
        (true, 1.0f, 2.0f, 3.0f, 4.0f),
        (true, -3.0f, 7.0f, 11.0f, 2.0f),
        (false, 9.0f, 8.0f, 7.0f, 6.0f)
      )
      val random = new Random(0x5eedbabeL)
      val cases = directed ++ (0 until 64).map { _ =>
        def signedFinite(): Float = {
          val magnitude = 0.125f + random.nextFloat() * 63.0f
          if (random.nextBoolean()) magnitude else -magnitude
        }
        (true, signedFinite(), signedFinite(), signedFinite(),
          0.125f + random.nextFloat() * 63.0f)
      }
      cases.zipWithIndex.foreach { case ((hit, u, v, t, denominator), id) =>
        submitResolve(dut, id, hit, u, v, t, denominator)
        waitUntil(300)(dut.io.out.valid.peek().litToBoolean)(dut.clock.step())
        checkResolve(dut, id, hit, u, v, t, denominator)
        dut.clock.step()
      }
    }
  }

  "TriangleResolve blocks a second hit and holds a divided result" in {
    test(new RtTriangleResolve).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      reset(dut)
      dut.io.in.valid.poke(false.B)
      dut.io.out.ready.poke(false.B)
      submitResolve(dut, 7, hit = true, 1.0f, 2.0f, 3.0f, 4.0f)
      pokeResolve(dut, 8, hit = true, 5.0f, 6.0f, 7.0f, 8.0f)
      dut.io.in.valid.poke(true.B)
      dut.io.in.ready.expect(false.B)
      dut.io.in.valid.poke(false.B)
      waitUntil(300)(dut.io.out.valid.peek().litToBoolean)(dut.clock.step())
      checkResolve(dut, 7, hit = true, 1.0f, 2.0f, 3.0f, 4.0f)
      dut.clock.step(3)
      dut.io.out.valid.expect(true.B)
      dut.io.out.ready.poke(true.B)
      dut.clock.step()
    }
  }
}

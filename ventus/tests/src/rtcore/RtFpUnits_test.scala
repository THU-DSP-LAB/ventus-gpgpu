package rtcore

import chisel3._
import chisel3.util._
import chiseltest._
import hardfloat._
import org.scalatest.freespec.AnyFreeSpec
import top.parameters._

import scala.util.Random

/** Test-only lockstep result collector for the retired HardFloat RT divider. */
class RtFpDivReferenceHarness extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new Bundle {
      val a = UInt(32.W)
      val b = UInt(32.W)
    }))
    val out = Decoupled(new Bundle {
      val result = UInt(32.W)
      val reference = UInt(32.W)
      val matches = Bool()
    })
  })

  val divider = Module(new RtFpDiv)
  val reference = Module(new DivSqrtRecFN_small(8, 24, 0))
  val pending = RegInit(false.B)
  val dividerSeen = RegInit(false.B)
  val referenceSeen = RegInit(false.B)
  val dividerResult = Reg(UInt(32.W))
  val referenceResult = Reg(UInt(32.W))

  io.in.ready := !pending && divider.io.in.ready && reference.io.inReady
  divider.io.in.valid := io.in.fire
  divider.io.in.bits.a := io.in.bits.a
  divider.io.in.bits.b := io.in.bits.b
  divider.io.out.ready := true.B
  reference.io.inValid := io.in.fire
  reference.io.sqrtOp := false.B
  reference.io.a := recFNFromFN(8, 24, io.in.bits.a)
  reference.io.b := recFNFromFN(8, 24, io.in.bits.b)
  reference.io.roundingMode := consts.round_near_even
  reference.io.detectTininess := consts.tininess_beforeRounding

  when(io.in.fire) {
    pending := true.B
    dividerSeen := false.B
    referenceSeen := false.B
  }
  when(divider.io.out.valid) {
    dividerResult := divider.io.out.bits
    dividerSeen := true.B
  }
  when(reference.io.outValid_div) {
    referenceResult := fNFromRecFN(8, 24, reference.io.out)
    referenceSeen := true.B
  }

  io.out.valid := pending && dividerSeen && referenceSeen
  io.out.bits.result := dividerResult
  io.out.bits.reference := referenceResult
  io.out.bits.matches := dividerResult === referenceResult
  when(io.out.fire) { pending := false.B }
}

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

  "RtFpDiv matches the retired HardFloat divider" in {
    test(new RtFpDivReferenceHarness).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      dut.reset.poke(true.B)
      dut.clock.step(2)
      dut.reset.poke(false.B)
      dut.io.in.valid.poke(false.B)
      dut.io.out.ready.poke(true.B)
      val directed = Seq(
        ("3f800000", "3f800000"), ("3f800000", "40800000"),
        ("c0600000", "40000000"), ("7f7fffff", "3f000000"),
        ("00800000", "40000000"), ("00000001", "3f800000"),
        ("00000000", "3f800000"), ("3f800000", "00000000"),
        ("00000000", "00000000"), ("7f800000", "3f800000"),
        ("3f800000", "7f800000"), ("7f800000", "7f800000"),
        ("7fc12345", "3f800000"), ("3f800000", "7fc12345")
      )
      val random = new Random(0x5eedf00dL)
      val randomCases = (0 until 256).map { _ =>
        def finiteNormal(): Int = {
          val sign = if (random.nextBoolean()) 0x80000000 else 0
          val exponent = 1 + random.nextInt(254)
          sign | (exponent << 23) | random.nextInt(1 << 23)
        }
        (f"${finiteNormal()}%08x", f"${finiteNormal()}%08x")
      }
      (directed ++ randomCases).foreach { case (a, b) =>
        dut.io.in.bits.a.poke(BigInt(a, 16).U)
        dut.io.in.bits.b.poke(BigInt(b, 16).U)
        dut.io.in.valid.poke(true.B)
        waitUntil(4)(dut.io.in.ready.peek().litToBoolean)(dut.clock.step())
        dut.clock.step()
        dut.io.in.valid.poke(false.B)
        waitUntil(300)(dut.io.out.valid.peek().litToBoolean)(dut.clock.step())
        dut.io.out.bits.matches.expect(true.B)
        dut.clock.step()
      }
    }
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
      tmax: Float,
      inverse: Seq[Float] = Seq.empty
  ): Unit = {
    val reciprocal = if (inverse.nonEmpty) inverse else direction.map(value => 1.0f / value)
    dut.io.in.bits.warpId.poke(0.U)
    dut.io.in.bits.laneId.poke(lane.U)
    dut.io.in.bits.rayState.origin.x.poke(raw(origin(0)).U)
    dut.io.in.bits.rayState.origin.y.poke(raw(origin(1)).U)
    dut.io.in.bits.rayState.origin.z.poke(raw(origin(2)).U)
    dut.io.in.bits.rayState.dir.x.poke(raw(direction(0)).U)
    dut.io.in.bits.rayState.dir.y.poke(raw(direction(1)).U)
    dut.io.in.bits.rayState.dir.z.poke(raw(direction(2)).U)
    dut.io.in.bits.rayState.tmin.poke(raw(0.125f).U)
    dut.io.in.bits.rayState.tmax.poke(raw(tmax).U)
    dut.io.in.bits.rayState.inv.x.poke(raw(reciprocal(0)).U)
    dut.io.in.bits.rayState.inv.y.poke(raw(reciprocal(1)).U)
    dut.io.in.bits.rayState.inv.z.poke(raw(reciprocal(2)).U)
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
      tmax: Float,
      inverse: Seq[Float] = Seq.empty
  ): Unit = {
    val (kx, ky, kz) = expectedAxes(direction)
    val reciprocal = if (inverse.nonEmpty) inverse else direction.map(value => 1.0f / value)
    dut.io.out.bits.warpId.expect(0.U)
    dut.io.out.bits.laneId.expect(lane.U)
    dut.io.out.bits.ray.origin.x.expect(raw(origin(0)).U)
    dut.io.out.bits.ray.origin.y.expect(raw(origin(1)).U)
    dut.io.out.bits.ray.origin.z.expect(raw(origin(2)).U)
    dut.io.out.bits.ray.dir.x.expect(raw(direction(0)).U)
    dut.io.out.bits.ray.dir.y.expect(raw(direction(1)).U)
    dut.io.out.bits.ray.dir.z.expect(raw(direction(2)).U)
    dut.io.out.bits.ray.inv.x.expect(raw(reciprocal(0)).U)
    dut.io.out.bits.ray.inv.y.expect(raw(reciprocal(1)).U)
    dut.io.out.bits.ray.inv.z.expect(raw(reciprocal(2)).U)
    dut.io.out.bits.ray.originalTmin.expect(raw(0.125f).U)
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
      tmax: Float,
      inverse: Seq[Float] = Seq.empty
  ): Unit = {
    pokeSetup(dut, lane, origin, direction, tmax, inverse)
    dut.io.in.valid.poke(true.B)
    waitUntil(4)(dut.io.in.ready.peek().litToBoolean)(dut.clock.step())
    dut.clock.step()
    dut.io.in.valid.poke(false.B)
  }

  "RaySetup passes through shader IEEE reciprocal, axis selection, and shear" in {
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

  "RaySetup accepts consecutive lanes while writing back prior lanes" in {
    test(new RtRaySetup).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      reset(dut)
      dut.io.out.ready.poke(true.B)
      val vectors = Seq(
        (Seq(1.0f, 2.0f, 3.0f), Seq(4.0f, -2.0f, 1.0f), 10.0f),
        (Seq(2.0f, 3.0f, 4.0f), Seq(-1.0f, 5.0f, 2.0f), 11.0f),
        (Seq(3.0f, 4.0f, 5.0f), Seq(2.0f, 1.0f, -8.0f), 12.0f),
        (Seq(4.0f, 5.0f, 6.0f), Seq(-3.0f, 2.0f, 7.0f), 13.0f)
      )
      vectors.zipWithIndex.foreach { case ((origin, direction, tmax), lane) =>
        pokeSetup(dut, lane, origin, direction, tmax)
        dut.io.in.valid.poke(true.B)
        dut.io.in.ready.expect(true.B)
        if (lane >= 2) {
          val (oldOrigin, oldDirection, oldTmax) = vectors(lane - 2)
          dut.io.out.valid.expect(true.B)
          checkSetupResult(dut, lane - 2, oldOrigin, oldDirection, oldTmax)
        }
        dut.clock.step()
      }
      dut.io.in.valid.poke(false.B)
      for (lane <- vectors.indices.drop(2)) {
        val (origin, direction, tmax) = vectors(lane)
        dut.io.out.valid.expect(true.B)
        checkSetupResult(dut, lane, origin, direction, tmax)
        dut.clock.step()
      }
      dut.io.out.valid.expect(false.B)
    }
  }

  "RaySetup consumes the reciprocal written by the shader SFU" in {
    test(new RtRaySetup).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      reset(dut)
      dut.io.in.valid.poke(false.B)
      dut.io.out.ready.poke(true.B)
      val origin = Seq(1.0f, 2.0f, 3.0f)
      val direction = Seq(8.0f, 2.0f, 1.0f)
      val suppliedInverse = Seq(0.5f, 0.25f, 1.0f)
      submitSetup(dut, 5, origin, direction, 20.0f, suppliedInverse)
      waitUntil(4)(dut.io.out.valid.peek().litToBoolean)(dut.clock.step())
      checkSetupResult(dut, 5, origin, direction, 20.0f, suppliedInverse)
      dut.clock.step()
    }
  }

  private def pokeResolve(
      dut: RtTriangleResolve,
      requestId: Int,
      u: Float,
      v: Float,
      t: Float,
      denominator: Float
  ): Unit = {
    dut.io.in.bits.requestId.poke(requestId.U)
    dut.io.in.bits.hit.poke(true.B)
    dut.io.in.bits.uNum.poke(raw(u).U)
    dut.io.in.bits.vNum.poke(raw(v).U)
    dut.io.in.bits.tNum.poke(raw(t).U)
    dut.io.in.bits.tDenom.poke(raw(denominator).U)
  }

  private def submitResolve(
      dut: RtTriangleResolve,
      requestId: Int,
      u: Float,
      v: Float,
      t: Float,
      denominator: Float
  ): Unit = {
    pokeResolve(dut, requestId, u, v, t, denominator)
    dut.io.in.valid.poke(true.B)
    waitUntil(4)(dut.io.in.ready.peek().litToBoolean)(dut.clock.step())
    dut.clock.step()
    dut.io.in.valid.poke(false.B)
  }

  private def checkResolve(
      dut: RtTriangleResolve,
      requestId: Int,
      u: Float,
      v: Float,
      t: Float,
      denominator: Float
  ): Unit = {
    dut.io.out.bits.requestId.expect(requestId.U)
    dut.io.out.bits.u.expect(raw(u / denominator).U)
    dut.io.out.bits.v.expect(raw(v / denominator).U)
    dut.io.out.bits.t.expect(raw(t / denominator).U)
  }

  "TriangleResolve performs only accepted-hit divisions and preserves ownership" in {
    test(new RtTriangleResolve).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      reset(dut)
      dut.io.in.valid.poke(false.B)
      dut.io.out.ready.poke(true.B)
      val directed = Seq(
        (1.0f, 2.0f, 3.0f, 4.0f),
        (-3.0f, 7.0f, 11.0f, 2.0f)
      )
      val random = new Random(0x5eedbabeL)
      val cases = directed ++ (0 until 64).map { _ =>
        def signedFinite(): Float = {
          val magnitude = 0.125f + random.nextFloat() * 63.0f
          if (random.nextBoolean()) magnitude else -magnitude
        }
        (signedFinite(), signedFinite(), signedFinite(),
          0.125f + random.nextFloat() * 63.0f)
      }
      cases.zipWithIndex.foreach { case ((u, v, t, denominator), id) =>
        submitResolve(dut, id, u, v, t, denominator)
        waitUntil(300)(dut.io.out.valid.peek().litToBoolean)(dut.clock.step())
        checkResolve(dut, id, u, v, t, denominator)
        dut.clock.step()
      }
    }
  }

  "TriangleResolve blocks a second hit and holds a divided result" in {
    test(new RtTriangleResolve).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      reset(dut)
      dut.io.in.valid.poke(false.B)
      dut.io.out.ready.poke(false.B)
      submitResolve(dut, 7, 1.0f, 2.0f, 3.0f, 4.0f)
      pokeResolve(dut, 8, 5.0f, 6.0f, 7.0f, 8.0f)
      dut.io.in.valid.poke(true.B)
      dut.io.in.ready.expect(false.B)
      dut.io.in.valid.poke(false.B)
      waitUntil(300)(dut.io.out.valid.peek().litToBoolean)(dut.clock.step())
      checkResolve(dut, 7, 1.0f, 2.0f, 3.0f, 4.0f)
      dut.clock.step(3)
      dut.io.out.valid.expect(true.B)
      dut.io.out.ready.poke(true.B)
      dut.clock.step()
    }
  }
}

package rtcore

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import scala.collection.mutable.ArrayBuffer
import scala.util.Random

/** Checks independent IU arithmetic and Decoupled transaction ownership. */
class RtIntersectionUnitParallelTest
    extends AnyFreeSpec
    with ChiselScalatestTester {
  private val zero = 0.U
  private val one = "h3f800000".U
  private val onePointFive = "h3fc00000".U
  private val two = "h40000000".U
  private val three = "h40400000".U
  private val four = "h40800000".U
  private val nine = "h41100000".U
  private val negativeOne = "hbf800000".U
  private val negativeTwo = "hc0000000".U
  private val negativeFour = "hc0800000".U
  private val negativeNine = "hc1100000".U
  private val positiveInfinity = "h7f800000".U

  private def pokeFloat3(vector: Float3, x: UInt, y: UInt, z: UInt): Unit = {
    vector.x.poke(x)
    vector.y.poke(y)
    vector.z.poke(z)
  }

  private def driveRay(
      ray: Ray,
      direction: UInt = one,
      inverseDirection: UInt = one,
      originalTmin: UInt = zero,
      extent: UInt = positiveInfinity,
      kx: Int = 0,
      ky: Int = 1,
      kz: Int = 2
  ): Unit = {
    pokeFloat3(ray.origin, zero, zero, zero)
    pokeFloat3(ray.dir, direction, direction, direction)
    pokeFloat3(ray.inv, inverseDirection, inverseDirection, inverseDirection)
    pokeFloat3(ray.shear, zero, zero, one)
    ray.originalTmin.poke(originalTmin)
    ray.extent.poke(extent)
    ray.kx.poke(kx.U)
    ray.ky.poke(ky.U)
    ray.kz.poke(kz.U)
  }

  private def driveBox(box: AABB, lower: UInt, upper: UInt): Unit = {
    box.x_min.poke(lower)
    box.y_min.poke(lower)
    box.z_min.poke(lower)
    box.x_max.poke(upper)
    box.y_max.poke(upper)
    box.z_max.poke(upper)
  }

  private def driveBoxJob(
      dut: RtIntersectionUnit,
      requestId: Int,
      lower: UInt,
      upper: UInt,
      direction: UInt = one,
      inverseDirection: UInt = one,
      originalTmin: UInt = zero,
      extent: UInt = positiveInfinity
  ): Unit = {
    dut.boxIn.bits.requestId.poke(requestId.U)
    driveRay(dut.boxIn.bits.ray, direction, inverseDirection, originalTmin, extent)
    dut.boxIn.bits.aabb.foreach(box => driveBox(box, lower, upper))
  }

  private def driveTriangleJob(
      dut: RtIntersectionUnit,
      requestId: Int,
      a: (UInt, UInt, UInt),
      b: (UInt, UInt, UInt),
      c: (UInt, UInt, UInt),
      kx: Int = 0,
      ky: Int = 1,
      kz: Int = 2
  ): Unit = {
    dut.triangleIn.bits.requestId.poke(requestId.U)
    driveRay(dut.triangleIn.bits.ray, kx = kx, ky = ky, kz = kz)
    pokeFloat3(dut.triangleIn.bits.triangle.A, a._1, a._2, a._3)
    pokeFloat3(dut.triangleIn.bits.triangle.B, b._1, b._2, b._3)
    pokeFloat3(dut.triangleIn.bits.triangle.C, c._1, c._2, c._3)
  }

  private def initialize(dut: RtIntersectionUnit): Unit = {
    dut.boxIn.valid.poke(false.B)
    dut.triangleIn.valid.poke(false.B)
    dut.boxOut.ready.poke(true.B)
    dut.triangleOut.ready.poke(true.B)
  }

  "box4 sorts its result and completes before a concurrent triangle" in {
    test(new RtIntersectionUnit) { dut =>
      initialize(dut)
      driveBoxJob(dut, 1, one, two)
      driveBox(dut.boxIn.bits.aabb(1), three, four)
      driveBox(dut.boxIn.bits.aabb(2), negativeFour, negativeTwo)
      driveBox(dut.boxIn.bits.aabb(3), two, three)
      driveTriangleJob(dut, 2, (zero, zero, one), (zero, one, one), (one, zero, one))

      dut.boxIn.valid.poke(true.B)
      dut.triangleIn.valid.poke(true.B)
      dut.boxIn.ready.expect(true.B)
      dut.triangleIn.ready.expect(true.B)
      dut.clock.step()
      dut.boxIn.valid.poke(false.B)
      dut.triangleIn.valid.poke(false.B)

      var boxCompletionCycle = -1
      var triangleCompletionCycle = -1
      for (cycle <- 0 until 20) {
        if (dut.boxOut.valid.peek().litToBoolean) {
          dut.boxOut.bits.requestId.expect(1.U)
          dut.boxOut.bits.tmin(0).expect(one)
          dut.boxOut.bits.tmin(1).expect(two)
          dut.boxOut.bits.tmin(2).expect(three)
          dut.boxOut.bits.tmin(3).expect(positiveInfinity)
          dut.boxOut.bits.isIntersect(0).expect(true.B)
          dut.boxOut.bits.isIntersect(1).expect(true.B)
          dut.boxOut.bits.isIntersect(2).expect(true.B)
          dut.boxOut.bits.isIntersect(3).expect(false.B)
          dut.boxOut.bits.boxIndex(0).expect(0.U)
          dut.boxOut.bits.boxIndex(1).expect(3.U)
          dut.boxOut.bits.boxIndex(2).expect(1.U)
          dut.boxOut.bits.boxIndex(3).expect(2.U)
          assert(boxCompletionCycle < 0, "box4 request was duplicated")
          boxCompletionCycle = cycle
        }
        if (dut.triangleOut.valid.peek().litToBoolean) {
          dut.triangleOut.bits.requestId.expect(2.U)
          dut.triangleOut.bits.tNum.expect(one)
          dut.triangleOut.bits.tDenom.expect(one)
          dut.triangleOut.bits.hit.expect(true.B)
          assert(triangleCompletionCycle < 0, "triangle request was duplicated")
          triangleCompletionCycle = cycle
        }
        dut.clock.step()
      }
      assert(boxCompletionCycle >= 0, "box4 did not complete within 20 cycles")
      assert(triangleCompletionCycle >= 0, "triangle did not complete within 20 cycles")
      assert(boxCompletionCycle < triangleCompletionCycle)
    }
  }

  "both pipelines preserve consecutive request order and ownership" in {
    test(new RtIntersectionUnit) { dut =>
      initialize(dut)
      driveBoxJob(dut, 11, one, two)
      driveTriangleJob(dut, 21, (zero, zero, one), (zero, one, one), (one, zero, one))
      dut.boxIn.valid.poke(true.B)
      dut.triangleIn.valid.poke(true.B)
      dut.boxIn.ready.expect(true.B)
      dut.triangleIn.ready.expect(true.B)
      dut.clock.step()

      driveBoxJob(dut, 12, two, three)
      driveTriangleJob(dut, 22, (zero, zero, one), (one, zero, one), (zero, one, one))
      dut.boxIn.ready.expect(true.B)
      dut.triangleIn.ready.expect(true.B)
      dut.clock.step()
      dut.boxIn.valid.poke(false.B)
      dut.triangleIn.valid.poke(false.B)

      val boxResults = ArrayBuffer.empty[(BigInt, BigInt)]
      val triangleResults = ArrayBuffer.empty[(BigInt, Boolean)]
      for (_ <- 0 until 24) {
        if (dut.boxOut.valid.peek().litToBoolean) {
          boxResults += ((dut.boxOut.bits.requestId.peek().litValue, dut.boxOut.bits.tmin(0).peek().litValue))
        }
        if (dut.triangleOut.valid.peek().litToBoolean) {
          triangleResults += ((dut.triangleOut.bits.requestId.peek().litValue, dut.triangleOut.bits.hit.peek().litToBoolean))
        }
        dut.clock.step()
      }
      assert(boxResults.toSeq == Seq((BigInt(11), one.litValue), (BigInt(12), two.litValue)))
      // Traversal is two-sided without an explicit cull flag: reversing the
      // winding must preserve the hit after the IU normalizes its numerators.
      assert(triangleResults.toSeq == Seq((BigInt(21), true), (BigInt(22), true)))
    }
  }

  "box4 handles negative directions and extent boundaries" in {
    test(new RtIntersectionUnit) { dut =>
      initialize(dut)
      driveBoxJob(dut, 31, negativeTwo, negativeOne, negativeOne, negativeOne)
      dut.boxIn.valid.poke(true.B)
      dut.boxIn.ready.expect(true.B)
      dut.clock.step()

      driveBoxJob(dut, 32, one, two, extent = one)
      dut.boxIn.ready.expect(true.B)
      dut.clock.step()
      driveBoxJob(dut, 33, one, two, originalTmin = onePointFive)
      dut.boxIn.ready.expect(true.B)
      dut.clock.step()
      dut.boxIn.valid.poke(false.B)

      var negativeDirectionSeen = false
      var extentBoundarySeen = false
      var tminSeen = false
      for (_ <- 0 until 24) {
        if (dut.boxOut.valid.peek().litToBoolean) {
          dut.boxOut.bits.requestId.peek().litValue match {
            case requestId if requestId == BigInt(31) =>
              dut.boxOut.bits.tmin.foreach(_.expect(one))
              dut.boxOut.bits.isIntersect.foreach(_.expect(true.B))
              negativeDirectionSeen = true
            case requestId if requestId == BigInt(32) =>
              // A closed slab may touch the ray's inclusive extent.
              dut.boxOut.bits.tmin.foreach(_.expect(one))
              dut.boxOut.bits.isIntersect.foreach(_.expect(true.B))
              extentBoundarySeen = true
            case requestId if requestId == BigInt(33) =>
              dut.boxOut.bits.tmin.foreach(_.expect(onePointFive))
              dut.boxOut.bits.isIntersect.foreach(_.expect(true.B))
              tminSeen = true
            case other => fail(s"unexpected Box4 requestId $other")
          }
        }
        dut.clock.step()
      }
      assert(negativeDirectionSeen)
      assert(extentBoundarySeen)
      assert(tminSeen)
    }
  }

  "box4 accepts the failing lane-13 object ray against a containing BLAS bound" in {
    test(new RtIntersectionUnit) { dut =>
      initialize(dut)
      dut.boxIn.bits.requestId.poke(61.U)
      // Exact IEEE32 ray fields traced from raytracingshadows lane 13 after
      // its identity world-to-object transform.  The four boxes deliberately
      // contain the large BLAS root AABB, so this isolates IU slab arithmetic
      // from VTAS child layout and scheduler state.
      pokeFloat3(dut.boxIn.bits.ray.origin, zero, "hc0400000".U, "h41200000".U)
      pokeFloat3(dut.boxIn.bits.ray.dir, "hbe067533".U, "h3ec9afcc".U, "hbf68e341".U)
      pokeFloat3(dut.boxIn.bits.ray.inv, "hc0f3b467".U, "h40227845".U, "hbf8cb3f8".U)
      pokeFloat3(dut.boxIn.bits.ray.shear, zero, zero, one)
      dut.boxIn.bits.ray.originalTmin.poke(zero)
      dut.boxIn.bits.ray.extent.poke(positiveInfinity)
      dut.boxIn.bits.ray.kx.poke(0.U)
      dut.boxIn.bits.ray.ky.poke(1.U)
      dut.boxIn.bits.ray.kz.poke(2.U)
      dut.boxIn.bits.aabb.foreach { box =>
        box.x_min.poke("hc0400000".U) // -3
        box.y_min.poke("hc0c00000".U) // -6
        box.z_min.poke("hc0000000".U) // -2
        box.x_max.poke("h40400000".U) // 3
        box.y_max.poke(one)
        box.z_max.poke("h40400000".U) // 3
      }
      dut.boxIn.valid.poke(true.B)
      dut.boxIn.ready.expect(true.B)
      dut.clock.step()
      dut.boxIn.valid.poke(false.B)

      var seen = false
      for (_ <- 0 until 20) {
        if (dut.boxOut.valid.peek().litToBoolean) {
          dut.boxOut.bits.requestId.expect(61.U)
          dut.boxOut.bits.isIntersect.foreach(_.expect(true.B))
          seen = true
        }
        dut.clock.step()
      }
      assert(seen, "lane-13 Box4 input did not complete")
    }
  }

  "box4 reproduces the lane-13 large-BLAS root miss" in {
    test(new RtIntersectionUnit) { dut =>
      initialize(dut)
      dut.boxIn.bits.requestId.poke(62.U)
      // Exact trace from raytracingshadows 4x4: lane 13, object BLAS root
      // 0x0028ac01.  Child refs 2/3 are invalid; their zero bounds are still
      // presented to the IU by the current scheduler and must not create a hit.
      pokeFloat3(dut.boxIn.bits.ray.origin, zero, "hc0400000".U, "h41200000".U)
      pokeFloat3(dut.boxIn.bits.ray.dir, "hbe067533".U, "h3ec9afcc".U, "hbf68e341".U)
      pokeFloat3(dut.boxIn.bits.ray.inv, "hc0f3b467".U, "h40227845".U, "hbf8cb3f8".U)
      pokeFloat3(dut.boxIn.bits.ray.shear, zero, zero, one)
      dut.boxIn.bits.ray.originalTmin.poke("h3a83126f".U)
      dut.boxIn.bits.ray.extent.poke("h461c4000".U)
      dut.boxIn.bits.ray.kx.poke(0.U)
      dut.boxIn.bits.ray.ky.poke(1.U)
      dut.boxIn.bits.ray.kz.poke(2.U)
      val minX = Seq("hc0193082".U, "hbffd4287".U, zero, zero)
      val minY = Seq("hc0a2b58d".U, "hc0a2c700".U, zero, zero)
      val minZ = Seq("hbfe3365d".U, "h3eacd87c".U, zero, zero)
      val maxX = Seq("h400b7d96".U, "h3fc2ae67".U, zero, zero)
      val maxY = Seq("h3ede3ccc".U, "h3e3f3890".U, zero, zero)
      val maxZ = Seq("h3fcb096e".U, "h4026a524".U, zero, zero)
      for (index <- 0 until 4) {
        dut.boxIn.bits.aabb(index).x_min.poke(minX(index))
        dut.boxIn.bits.aabb(index).y_min.poke(minY(index))
        dut.boxIn.bits.aabb(index).z_min.poke(minZ(index))
        dut.boxIn.bits.aabb(index).x_max.poke(maxX(index))
        dut.boxIn.bits.aabb(index).y_max.poke(maxY(index))
        dut.boxIn.bits.aabb(index).z_max.poke(maxZ(index))
      }
      dut.boxIn.valid.poke(true.B)
      dut.boxIn.ready.expect(true.B)
      dut.clock.step()
      dut.boxIn.valid.poke(false.B)

      var seen = false
      for (_ <- 0 until 20) {
        if (dut.boxOut.valid.peek().litToBoolean) {
          dut.boxOut.bits.requestId.expect(62.U)
          dut.boxOut.bits.isIntersect.foreach(_.expect(false.B))
          seen = true
        }
        dut.clock.step()
      }
      assert(seen, "lane-13 large-BLAS root input did not complete")
    }
  }

  "triangle handles interior, behind-ray, axis-permuted, and degenerate cases" in {
    test(new RtIntersectionUnit) { dut =>
      initialize(dut)
      driveTriangleJob(dut, 41, (negativeOne, negativeOne, one), (negativeOne, two, one), (two, negativeOne, one))
      dut.triangleIn.valid.poke(true.B)
      dut.triangleIn.ready.expect(true.B)
      dut.clock.step()

      driveTriangleJob(dut, 42, (negativeOne, negativeOne, negativeOne), (negativeOne, two, negativeOne), (two, negativeOne, negativeOne))
      dut.triangleIn.ready.expect(true.B)
      dut.clock.step()

      driveTriangleJob(dut, 43, (one, negativeOne, negativeOne), (one, negativeOne, two), (one, two, negativeOne), kx = 1, ky = 2, kz = 0)
      dut.triangleIn.ready.expect(true.B)
      dut.clock.step()

      driveTriangleJob(dut, 44, (zero, zero, one), (one, one, one), (two, two, one))
      dut.triangleIn.ready.expect(true.B)
      dut.clock.step()
      dut.triangleIn.valid.poke(false.B)

      val results = ArrayBuffer.empty[(BigInt, BigInt, BigInt, Boolean)]
      for (_ <- 0 until 28) {
        if (dut.triangleOut.valid.peek().litToBoolean) {
          results += ((
            dut.triangleOut.bits.requestId.peek().litValue,
            dut.triangleOut.bits.tNum.peek().litValue,
            dut.triangleOut.bits.tDenom.peek().litValue,
            dut.triangleOut.bits.hit.peek().litToBoolean
          ))
          if (dut.triangleOut.bits.requestId.peek().litValue == 41) {
            // At the origin-facing interior point of this right triangle,
            // RayFlex's B and C weights are both 1/3.  The RT ABI exports
            // exactly those two weights as hitAttributeEXT.xy (not A/B).
            dut.triangleOut.bits.uNum.expect(three)
            dut.triangleOut.bits.vNum.expect(three)
          }
        }
        dut.clock.step()
      }
      assert(results.toSeq == Seq(
        (BigInt(41), nine.litValue, nine.litValue, true),
        (BigInt(42), negativeNine.litValue, nine.litValue, false),
        (BigInt(43), nine.litValue, nine.litValue, true),
        (BigInt(44), zero.litValue, zero.litValue, false)
      ))
    }
  }

  "box4 output backpressure preserves its result and does not block triangle" in {
    test(new RtIntersectionUnit) { dut =>
      initialize(dut)
      dut.boxOut.ready.poke(false.B)
      driveBoxJob(dut, 51, one, two)
      driveTriangleJob(dut, 52, (zero, zero, one), (zero, one, one), (one, zero, one))
      dut.boxIn.valid.poke(true.B)
      dut.triangleIn.valid.poke(true.B)
      dut.boxIn.ready.expect(true.B)
      dut.triangleIn.ready.expect(true.B)
      dut.clock.step()
      dut.boxIn.valid.poke(false.B)
      dut.triangleIn.valid.poke(false.B)

      var boxObserved = false
      var triangleCompleted = false
      for (_ <- 0 until 24) {
        if (dut.boxOut.valid.peek().litToBoolean) {
          dut.boxOut.bits.requestId.expect(51.U)
          dut.boxOut.bits.tmin.foreach(_.expect(one))
          dut.boxOut.bits.isIntersect.foreach(_.expect(true.B))
          boxObserved = true
        }
        if (dut.triangleOut.valid.peek().litToBoolean) {
          dut.triangleOut.bits.requestId.expect(52.U)
          dut.triangleOut.bits.hit.expect(true.B)
          triangleCompleted = true
        }
        dut.clock.step()
      }
      assert(boxObserved, "box4 output never reached the blocked sink")
      assert(triangleCompleted, "triangle completion was blocked by box4 output")

      dut.boxOut.ready.poke(true.B)
      dut.clock.step()
      for (_ <- 0 until 4) {
        dut.boxOut.valid.expect(false.B)
        dut.clock.step()
      }
    }
  }

  "box4 fixed-seed finite cases sort all four near distances" in {
    test(new RtIntersectionUnit) { dut =>
      initialize(dut)
      val random = new Random(0x5eedb0L)
      def finiteInteger(value: Int): UInt =
        BigInt(java.lang.Float.floatToRawIntBits(value.toFloat).toLong & 0xffffffffL).U(32.W)

      for (requestId <- 0 until 4) {
        val base = 1 + random.nextInt(27)
        val lower = random.shuffle(Vector.tabulate(4)(base + _))
        dut.boxIn.bits.requestId.poke(requestId.U)
        driveRay(dut.boxIn.bits.ray)
        lower.zipWithIndex.foreach { case (near, index) =>
          driveBox(dut.boxIn.bits.aabb(index), finiteInteger(near), finiteInteger(near + 1))
        }
        dut.boxIn.valid.poke(true.B)
        dut.boxIn.ready.expect(true.B)
        dut.clock.step()
        dut.boxIn.valid.poke(false.B)

        var remaining = 24
        while (!dut.boxOut.valid.peek().litToBoolean && remaining > 0) {
          dut.clock.step()
          remaining -= 1
        }
        assert(remaining > 0, s"Box4 request $requestId did not complete")
        dut.boxOut.bits.requestId.expect(requestId.U)
        val expected = lower.zipWithIndex.sortBy { case (near, index) => (near, index) }
        expected.zipWithIndex.foreach { case ((near, index), output) =>
          dut.boxOut.bits.tmin(output).expect(finiteInteger(near))
          dut.boxOut.bits.boxIndex(output).expect(index.U)
          dut.boxOut.bits.isIntersect(output).expect(true.B)
        }
        dut.clock.step()
      }
    }
  }

  "box4 accepts the actual lanes 12 to 15 platform top and bottom faces" in {
    test(new RtIntersectionUnit) { dut =>
      initialize(dut)
      driveRay(dut.boxIn.bits.ray)
      pokeFloat3(dut.boxIn.bits.ray.origin, zero, "hc0400000".U, "h41200000".U)
      dut.boxIn.bits.ray.originalTmin.poke("h3a83126f".U)
      dut.boxIn.bits.ray.extent.poke("h461c4000".U)
      for (i <- 0 until 4) {
        val box = dut.boxIn.bits.aabb(i)
        box.x_min.poke("hc0efc2d7".U)
        box.x_max.poke("h40f03d29".U)
        val y = if (i < 2) "h3f0cfe02".U else "h3c8c64b0".U
        box.y_min.poke(y)
        box.y_max.poke(y)
        box.z_min.poke("hc0f4cd01".U)
        box.z_max.poke("h40eb32ff".U)
      }
      val rays = Seq(
        Seq("bebd118d", "3ebd118d", "bf5a514a", "c02d5013", "402d5013", "bf9617e3"),
        Seq("be067533", "3ec9afcc", "bf68e341", "c0f3b467", "40227845", "bf8cb3f8"),
        Seq("3e067533", "3ec9afcc", "bf68e341", "40f3b467", "40227845", "bf8cb3f8"),
        Seq("3ebd118d", "3ebd118d", "bf5a514a", "402d5013", "402d5013", "bf9617e3"))
      for ((ray, index) <- rays.zipWithIndex) {
        val words = ray.map(word => BigInt(word, 16).U)
        dut.boxIn.bits.requestId.poke((64 + index).U)
        pokeFloat3(dut.boxIn.bits.ray.dir, words(0), words(1), words(2))
        pokeFloat3(dut.boxIn.bits.ray.inv, words(3), words(4), words(5))
        dut.boxIn.valid.poke(true.B)
        dut.boxIn.ready.expect(true.B)
        dut.clock.step()
        dut.boxIn.valid.poke(false.B)
        var remaining = 24
        while (!dut.boxOut.valid.peek().litToBoolean && remaining > 0) {
          dut.clock.step()
          remaining -= 1
        }
        assert(remaining > 0)
        dut.boxOut.bits.requestId.expect((64 + index).U)
        dut.boxOut.bits.isIntersect.foreach(_.expect(true.B))
        dut.clock.step()
      }
    }
  }

  "box4 accepts a closed-interval tangent AABB" in {
    test(new RtIntersectionUnit) { dut =>
      initialize(dut)
      dut.boxIn.bits.requestId.poke(63.U)
      driveRay(dut.boxIn.bits.ray)
      // Each axis produces the singleton interval t = 1.  This is the
      // boundary case used by thin triangle BVH boxes: tmin == tmax must be
      // accepted, matching Spike's `tmin > tmax` rejection rule.
      dut.boxIn.bits.aabb.foreach(box => driveBox(box, one, one))
      dut.boxIn.valid.poke(true.B)
      dut.boxIn.ready.expect(true.B)
      dut.clock.step()
      dut.boxIn.valid.poke(false.B)

      var remaining = 24
      while (!dut.boxOut.valid.peek().litToBoolean && remaining > 0) {
        dut.clock.step()
        remaining -= 1
      }
      assert(remaining > 0, "tangent Box4 request did not complete")
      dut.boxOut.bits.requestId.expect(63.U)
      dut.boxOut.bits.isIntersect.foreach(_.expect(true.B))
      dut.boxOut.bits.tmin.foreach(_.expect(one))
    }
  }
}

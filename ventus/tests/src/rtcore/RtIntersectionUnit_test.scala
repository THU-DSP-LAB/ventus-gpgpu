package rtcore

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import scala.collection.mutable.ArrayBuffer

/** Checks independent IU arithmetic and Decoupled transaction ownership. */
class RtIntersectionUnitParallelTest
    extends AnyFreeSpec
    with ChiselScalatestTester {
  private val zero = 0.U
  private val one = "h3f800000".U
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
      extent: UInt = positiveInfinity,
      kx: Int = 0,
      ky: Int = 1,
      kz: Int = 2
  ): Unit = {
    pokeFloat3(ray.origin, zero, zero, zero)
    pokeFloat3(ray.dir, direction, direction, direction)
    pokeFloat3(ray.inv, inverseDirection, inverseDirection, inverseDirection)
    pokeFloat3(ray.shear, zero, zero, one)
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
      extent: UInt = positiveInfinity
  ): Unit = {
    dut.boxIn.bits.requestId.poke(requestId.U)
    driveRay(dut.boxIn.bits.ray, direction, inverseDirection, extent)
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
      assert(triangleResults.toSeq == Seq((BigInt(21), true), (BigInt(22), false)))
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
      dut.boxIn.valid.poke(false.B)

      var negativeDirectionSeen = false
      var extentBoundarySeen = false
      for (_ <- 0 until 24) {
        if (dut.boxOut.valid.peek().litToBoolean) {
          dut.boxOut.bits.requestId.peek().litValue match {
            case requestId if requestId == BigInt(31) =>
              dut.boxOut.bits.tmin.foreach(_.expect(one))
              dut.boxOut.bits.isIntersect.foreach(_.expect(true.B))
              negativeDirectionSeen = true
            case requestId if requestId == BigInt(32) =>
              dut.boxOut.bits.tmin.foreach(_.expect(positiveInfinity))
              dut.boxOut.bits.isIntersect.foreach(_.expect(false.B))
              extentBoundarySeen = true
            case other => fail(s"unexpected Box4 requestId $other")
          }
        }
        dut.clock.step()
      }
      assert(negativeDirectionSeen)
      assert(extentBoundarySeen)
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
}

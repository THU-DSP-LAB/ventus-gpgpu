package rtcore

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

/** Verifies that the RTcore front accepts independent box4 and triangle jobs. */
class RtIntersectionUnitParallelTest
    extends AnyFreeSpec
    with ChiselScalatestTester {
  private val zero = 0.U
  private val one = "h3f800000".U
  private val two = "h40000000".U
  private val three = "h40400000".U
  private val four = "h40800000".U
  private val negativeTwo = "hc0000000".U
  private val negativeFour = "hc0800000".U
  private val positiveInfinity = "h7f800000".U

  private def pokeFloat3(vector: Float3, x: UInt, y: UInt, z: UInt): Unit = {
    vector.x.poke(x)
    vector.y.poke(y)
    vector.z.poke(z)
  }

  private def driveRay(ray: Ray): Unit = {
    pokeFloat3(ray.origin, zero, zero, zero)
    pokeFloat3(ray.dir, one, one, one)
    pokeFloat3(ray.inv, one, one, one)
    pokeFloat3(ray.shear, zero, zero, one)
    ray.extent.poke("h7f800000".U)
    ray.kx.poke(0.U)
    ray.ky.poke(1.U)
    ray.kz.poke(2.U)
  }

  private def driveBox(box: AABB, lower: UInt, upper: UInt): Unit = {
    box.x_min.poke(lower)
    box.y_min.poke(lower)
    box.z_min.poke(lower)
    box.x_max.poke(upper)
    box.y_max.poke(upper)
    box.z_max.poke(upper)
  }

  "box4 sorts its result and completes before a concurrent triangle" in {
    test(new RtIntersectionUnit) { dut =>
      dut.boxOut.ready.poke(true.B)
      dut.triangleOut.ready.poke(true.B)

      driveRay(dut.boxIn.bits.ray)
      driveBox(dut.boxIn.bits.aabb(0), one, two)
      driveBox(dut.boxIn.bits.aabb(1), three, four)
      driveBox(dut.boxIn.bits.aabb(2), negativeFour, negativeTwo)
      driveBox(dut.boxIn.bits.aabb(3), two, three)

      driveRay(dut.triangleIn.bits.ray)
      pokeFloat3(dut.triangleIn.bits.triangle.A, zero, zero, one)
      pokeFloat3(dut.triangleIn.bits.triangle.B, zero, one, one)
      pokeFloat3(dut.triangleIn.bits.triangle.C, one, zero, one)

      dut.boxIn.valid.poke(true.B)
      dut.triangleIn.valid.poke(true.B)
      dut.clock.step()
      dut.boxIn.ready.expect(true.B)
      dut.triangleIn.ready.expect(true.B)
      dut.clock.step()
      dut.boxIn.valid.poke(false.B)
      dut.triangleIn.valid.poke(false.B)

      var boxCompletionCycle = -1
      var triangleCompletionCycle = -1
      for (cycle <- 0 until 20) {
        if (boxCompletionCycle < 0 && dut.boxOut.valid.peek().litToBoolean) {
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
          boxCompletionCycle = cycle
        }
        if (triangleCompletionCycle < 0 && dut.triangleOut.valid.peek().litToBoolean) {
          dut.triangleOut.bits.tNum.expect(one)
          dut.triangleOut.bits.tDenom.expect(one)
          dut.triangleOut.bits.hit.expect(true.B)
          triangleCompletionCycle = cycle
        }
        dut.clock.step()
      }
      assert(boxCompletionCycle >= 0, "box4 did not complete within 20 cycles")
      assert(triangleCompletionCycle >= 0, "triangle did not complete within 20 cycles")
      assert(boxCompletionCycle < triangleCompletionCycle)
    }
  }

  "triangle rejects a reversed winding" in {
    test(new RtIntersectionUnit) { dut =>
      dut.boxOut.ready.poke(true.B)
      dut.triangleOut.ready.poke(true.B)
      dut.boxIn.valid.poke(false.B)

      driveRay(dut.triangleIn.bits.ray)
      pokeFloat3(dut.triangleIn.bits.triangle.A, zero, zero, one)
      pokeFloat3(dut.triangleIn.bits.triangle.B, one, zero, one)
      pokeFloat3(dut.triangleIn.bits.triangle.C, zero, one, one)
      dut.triangleIn.valid.poke(true.B)
      dut.clock.step()
      dut.triangleIn.ready.expect(true.B)
      dut.clock.step()
      dut.triangleIn.valid.poke(false.B)

      var observed = false
      for (_ <- 0 until 20) {
        if (!observed && dut.triangleOut.valid.peek().litToBoolean) {
          dut.triangleOut.bits.hit.expect(false.B)
          observed = true
        }
        dut.clock.step()
      }
      assert(observed, "triangle miss did not complete within 20 cycles")
    }
  }

  "box4 output backpressure does not block triangle completion" in {
    test(new RtIntersectionUnit) { dut =>
      dut.boxOut.ready.poke(false.B)
      dut.triangleOut.ready.poke(true.B)

      driveRay(dut.boxIn.bits.ray)
      dut.boxIn.bits.aabb.foreach(box => driveBox(box, one, two))
      driveRay(dut.triangleIn.bits.ray)
      pokeFloat3(dut.triangleIn.bits.triangle.A, zero, zero, one)
      pokeFloat3(dut.triangleIn.bits.triangle.B, zero, one, one)
      pokeFloat3(dut.triangleIn.bits.triangle.C, one, zero, one)

      dut.boxIn.valid.poke(true.B)
      dut.triangleIn.valid.poke(true.B)
      dut.clock.step()
      dut.boxIn.ready.expect(true.B)
      dut.triangleIn.ready.expect(true.B)
      dut.clock.step()
      dut.boxIn.valid.poke(false.B)
      dut.triangleIn.valid.poke(false.B)

      var boxHeld = false
      var triangleCompleted = false
      for (_ <- 0 until 20) {
        boxHeld ||= dut.boxOut.valid.peek().litToBoolean
        triangleCompleted ||= dut.triangleOut.valid.peek().litToBoolean
        dut.clock.step()
      }
      assert(boxHeld, "box4 output never reached the blocked sink")
      assert(triangleCompleted, "triangle completion was blocked by box4 output")
    }
  }
}

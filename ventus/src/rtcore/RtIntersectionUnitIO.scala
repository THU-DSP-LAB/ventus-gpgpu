// This file is part of raytracer.
// Licensed under the BSD 3-clause License.
// See the LICENSE.txt file for details.

package rtcore

import chisel3._
import hardfloat.{recFNFromFN, fNFromRecFN}
import top.parameters._

class Float3(recorded_float: Boolean = false) extends Bundle {

  /** Defines a point in three-dimensional space. With "recorded_float" being
    * false, the expected format is IEEE-754 32-bit float With "recorded_float"
    * being true, the expected format is (1 bit sign + 9 bit exponent + 23 bit
    * fraction). This is the 33-bit long "recorded float" format used by the
    * HardFloat library, that corresponds to the 32-bit standard float.
    */
  val bit_width = if (recorded_float) 33.W else 32.W

  val x = Bits(bit_width)
  val y = Bits(bit_width)
  val z = Bits(bit_width)

  def isRecordedFloat(): Boolean = { recorded_float }
  def at(idx: UInt): Bits = {
    val output = Wire(Bits(bit_width))
    when(idx === 0.U) {
      output := x
    }.elsewhen(idx === 1.U) {
      output := y
    }.otherwise {
      output := z
    }
    output
  }
}

class Ray(recorded_float: Boolean = false) extends Bundle {

  /** A ray in three-dimensional space.
    */
  val origin = new Float3(recorded_float)
  val dir = new Float3(recorded_float)
  val inv = new Float3(recorded_float)

  val _bit_width = if (recorded_float) 33.W else 32.W
  // Immutable lower traversal bound supplied by traceRay.
  val originalTmin = Bits(_bit_width)
  // Mutable upper traversal bound; this is narrowed after committed hits.
  val extent = Bits(_bit_width)

  val kx = UInt(2.W)
  val ky = UInt(2.W)
  val kz = UInt(2.W)
  val shear = new Float3(recorded_float)

  def isRecordedFloat(): Boolean = { recorded_float }
}

class AABB(recorded_float: Boolean = false) extends Bundle {

  /** Defines an Axis-Aligned Bounding Box (AABB) in three-dimensional space.
    * All values are either IEEE-754 floats (!recorded_float) or 33-bit recorded
    * floats .
    */

  val bit_width = if (recorded_float) 33.W else 32.W

  val x_min = Bits(bit_width)
  val y_min = Bits(bit_width)
  val z_min = Bits(bit_width)

  val x_max = Bits(bit_width)
  val y_max = Bits(bit_width)
  val z_max = Bits(bit_width)

  def isRecordedFloat(): Boolean = { recorded_float }
}

class Triangle(recorded_float: Boolean = false) extends Bundle {
  val A = new Float3(recorded_float)
  val B = new Float3(recorded_float)
  val C = new Float3(recorded_float)

  def isRecordedFloat(): Boolean = { recorded_float }
}

/** PDS ray-state fields consumed by intersection setup before Ray derivation. */
class RtRayState(recorded_float: Boolean = false) extends Bundle {
  val origin = new Float3(recorded_float)
  val dir = new Float3(recorded_float)
  val tmin = Bits(if (recorded_float) 33.W else 32.W)
  val tmax = Bits(if (recorded_float) 33.W else 32.W)
}

/** Prepared geometry input for one four-box intersection operation. */
class RtBox4IntersectionInput(recorded_float: Boolean = false) extends Bundle {
  /** Identifies the owning RtMemTable entry until its result is consumed. */
  val requestId = UInt(RtCorePortParameters.requestIdWidth.W)
  val ray = new Ray(recorded_float)
  val aabb = Vec(4, new AABB(recorded_float))
}

/** Result of one four-box intersection operation. */
class RtBox4IntersectionOutput extends Bundle {
  val requestId = UInt(RtCorePortParameters.requestIdWidth.W)
  // Intersecting boxes precede non-intersecting boxes; the latter use +Inf.
  val tmin = Vec(4, Bits(32.W))
  val isIntersect = Vec(4, Bool())
  val boxIndex = Vec(4, UInt(2.W))
}

/** Prepared geometry input for one triangle intersection operation. */
class RtTriangleIntersectionInput(recorded_float: Boolean = false)
    extends Bundle {
  /** Identifies the owning RtMemTable entry until its result is consumed. */
  val requestId = UInt(RtCorePortParameters.requestIdWidth.W)
  val ray = new Ray(recorded_float)
  val triangle = new Triangle(recorded_float)
}

/** Result of one triangle intersection operation. */
class RtTriangleIntersectionOutput(recorded_float: Boolean = false)
    extends Bundle {
  val requestId = UInt(RtCorePortParameters.requestIdWidth.W)
  // Numerators share tDenom.  The traversal controller performs the final
  // IEEE-754 divisions only for an accepted closest hit.
  val uNum = Bits(if (recorded_float) 33.W else 32.W)
  val vNum = Bits(if (recorded_float) 33.W else 32.W)
  val tNum = Bits(if (recorded_float) 33.W else 32.W)
  val tDenom = Bits(if (recorded_float) 33.W else 32.W)
  val hit = Bool()
}

// Conversion circuits between 32-bit IEEE float and 33-bit recorded float

object Float3ConvertFNtoRecFN {
  def apply(in: Float3): Float3 = {
    if (in.isRecordedFloat()) {
      throw new Exception(
        "input must be in IEEE-754 format before it can be converted to recorded format"
      )
    }
    val out = Wire(new Float3(recorded_float = true))
    out.x := recFNFromFN(8, 24, in.x)
    out.y := recFNFromFN(8, 24, in.y)
    out.z := recFNFromFN(8, 24, in.z)
    out
  }
}

object RayConvertFNtoRecFN {
  def apply(in: Ray): Ray = {
    if (in.isRecordedFloat()) {
      throw new Exception(
        "Ray must be in IEEE-754 format before it can be converted to recorded format"
      )
    }

    val out = Wire(new Ray(recorded_float = true))
    out.origin.x := recFNFromFN(8, 24, in.origin.x)
    out.origin.y := recFNFromFN(8, 24, in.origin.y)
    out.origin.z := recFNFromFN(8, 24, in.origin.z)
    out.dir.x := recFNFromFN(8, 24, in.dir.x)
    out.dir.y := recFNFromFN(8, 24, in.dir.y)
    out.dir.z := recFNFromFN(8, 24, in.dir.z)
    out.inv.x := recFNFromFN(8, 24, in.inv.x)
    out.inv.y := recFNFromFN(8, 24, in.inv.y)
    out.inv.z := recFNFromFN(8, 24, in.inv.z)
    out.originalTmin := recFNFromFN(8, 24, in.originalTmin)
    out.extent := recFNFromFN(8, 24, in.extent)
    out.kx := in.kx
    out.ky := in.ky
    out.kz := in.kz
    out.shear.x := recFNFromFN(8, 24, in.shear.x)
    out.shear.y := recFNFromFN(8, 24, in.shear.y)
    out.shear.z := recFNFromFN(8, 24, in.shear.z)

    out
  }
}

object AABBConvertFNtoRecFN {
  def apply(in: AABB): AABB = {
    if (in.isRecordedFloat()) {
      throw new Exception(
        "AABB must be in IEEE-754 format before it can be converted to recorded format"
      )
    }

    val out = Wire(new AABB(recorded_float = true))
    out.x_min := recFNFromFN(8, 24, in.x_min)
    out.y_min := recFNFromFN(8, 24, in.y_min)
    out.z_min := recFNFromFN(8, 24, in.z_min)

    out.x_max := recFNFromFN(8, 24, in.x_max)
    out.y_max := recFNFromFN(8, 24, in.y_max)
    out.z_max := recFNFromFN(8, 24, in.z_max)

    out
  }
}

object TriangleConvertFNtoRecFN {
  def apply(in: Triangle): Triangle = {
    if (in.isRecordedFloat() == true) {
      throw new Exception(
        "AABB must be in recorded format before it can be converted to IEEE-754 format"
      )
    }

    val out = Wire(new Triangle(true))
    out.A := Float3ConvertFNtoRecFN(in.A)
    out.B := Float3ConvertFNtoRecFN(in.B)
    out.C := Float3ConvertFNtoRecFN(in.C)

    out
  }
}

package rtcore

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

/** Fast arithmetic replay for the RayFlex triangle pipe.
  *
  * This deliberately bypasses Scheduler/AS/LSU.  A failure here is an
  * arithmetic-order or HardFloat issue, not a traversal-control issue.
  */
class RtTrianglePipelineTest extends AnyFreeSpec with ChiselScalatestTester {
  private def raw(x: Float): BigInt =
    BigInt(java.lang.Float.floatToRawIntBits(x).toLong & 0xffffffffL)

  private def f32(bits: String): Float =
    java.lang.Float.intBitsToFloat(java.lang.Integer.parseUnsignedInt(bits, 16))

  private case class Result(hit: Boolean, u: Float, v: Float, t: Float, denom: Float,
                            frontFace: Boolean)
  private case class Setup(kx: Int, ky: Int, kz: Int, sx: Float, sy: Float, sz: Float)

  private def raySetup(d: Seq[Float], inv: Seq[Float]): Setup = {
    val magnitude = d.map(x => java.lang.Float.floatToRawIntBits(x) & 0x7fffffff)
    val kz = if (magnitude(0) >= magnitude(1) && magnitude(0) >= magnitude(2)) 0
      else if (magnitude(1) >= magnitude(2)) 1 else 2
    val positiveKx = if (kz == 2) 0 else kz + 1
    val positiveKy = if (positiveKx == 2) 0 else positiveKx + 1
    val negative = java.lang.Float.floatToRawIntBits(d(kz)) < 0
    val kx = if (negative) positiveKy else positiveKx
    val ky = if (negative) positiveKx else positiveKy
    Setup(kx, ky, kz, d(kx) * inv(kz), d(ky) * inv(kz), inv(kz))
  }

  // Exact scalar order used by the Spike watertight model and by the RTL
  // pipeline: translate, shear/scale, edge products, then left-associated sums.
  private def reference(o: Seq[Float], d: Seq[Float], inv: Seq[Float],
                        tri: Seq[Seq[Float]]): Result = {
    val setup = raySetup(d, inv)
    def project(p: Seq[Float]): (Float, Float, Float) = {
      val q = p.zip(o).map { case (a, b) => a - b }
      (q(setup.kx) + (-setup.sx) * q(setup.kz), q(setup.ky) + (-setup.sy) * q(setup.kz), setup.sz * q(setup.kz))
    }
    val (ax, ay, az) = project(tri(0)); val (bx, by, bz) = project(tri(1)); val (cx, cy, cz) = project(tri(2))
    var u = cx * by - cy * bx; var v = ax * cy - ay * cx; var w = bx * ay - by * ax
    var denom = (u + v) + w; var t = (u * az + v * bz) + w * cz
    val negative = denom < 0.0f
    val hit = denom != 0.0f && (if (negative) !(u > 0 || v > 0 || w > 0 || t > 0)
      else !(u < 0 || v < 0 || w < 0 || t < 0))
    if (negative) { u = -u; v = -v; w = -w; denom = -denom; t = -t }
    Result(hit, v, w, t, denom, negative)
  }

  private def poke(dut: RtTrianglePipeline, id: Int, o: Seq[Float], d: Seq[Float], inv: Seq[Float], tri: Seq[Seq[Float]]): Unit = {
    dut.in.bits.requestId.poke(id.U)
    Seq(dut.in.bits.ray.origin.x, dut.in.bits.ray.origin.y, dut.in.bits.ray.origin.z).zip(o).foreach { case (p, x) => p.poke(raw(x).U) }
    Seq(dut.in.bits.ray.dir.x, dut.in.bits.ray.dir.y, dut.in.bits.ray.dir.z).zip(d).foreach { case (p, x) => p.poke(raw(x).U) }
    Seq(dut.in.bits.ray.inv.x, dut.in.bits.ray.inv.y, dut.in.bits.ray.inv.z).zip(inv).foreach { case (p, x) => p.poke(raw(x).U) }
    val setup = raySetup(d, inv)
    dut.in.bits.ray.kx.poke(setup.kx.U); dut.in.bits.ray.ky.poke(setup.ky.U); dut.in.bits.ray.kz.poke(setup.kz.U)
    Seq(dut.in.bits.ray.shear.x, dut.in.bits.ray.shear.y, dut.in.bits.ray.shear.z)
      .zip(Seq(setup.sx, setup.sy, setup.sz)).foreach { case (p, x) => p.poke(raw(x).U) }
    dut.in.bits.ray.originalTmin.poke(raw(0.0f).U); dut.in.bits.ray.extent.poke(raw(1000.0f).U)
    val axes = Seq(dut.in.bits.triangle.A, dut.in.bits.triangle.B, dut.in.bits.triangle.C)
    axes.zip(tri).foreach { case (point, values) => Seq(point.x, point.y, point.z).zip(values).foreach { case (p, x) => p.poke(raw(x).U) } }
  }

  "RtTrianglePipeline matches scalar watertight replay" in {
    test(new RtTrianglePipeline).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      dut.reset.poke(true.B); dut.clock.step(2); dut.reset.poke(false.B)
      dut.in.valid.poke(false.B); dut.out.ready.poke(true.B)
      val triangles = Seq(
        Seq(Seq(-1.0f, -1.0f, 5.0f), Seq(1.0f, -1.0f, 5.0f), Seq(-1.0f, 1.0f, 5.0f)),
        Seq(Seq(-0.7f, -0.4f, 3.2f), Seq(0.9f, -0.6f, 4.1f), Seq(-0.2f, 1.1f, 4.8f)))
      val rays = Seq((Seq(0.1f, -0.2f, 0.0f), Seq(0.04f, 0.03f, 1.0f)),
        (Seq(0.0f, 0.0f, 0.0f), Seq(-0.08f, 0.12f, 1.0f)))
      val random = new scala.util.Random(0x51a7d00dL)
      def signed(scale: Float): Float = (random.nextFloat() * 2.0f - 1.0f) * scale
      // This is deliberately deterministic: failures are directly replayable
      // from the printed request ID and need no Vulkan setup.
      val fuzz = (0 until 240).map { _ =>
        val origin = Seq(signed(3.0f), signed(3.0f), signed(3.0f))
        val direction = Seq(signed(1.0f) + 0.03125f, signed(1.0f) - 0.046875f,
          signed(1.0f) + 0.0625f)
        val triangle = Seq.fill(3)(Seq(signed(12.0f), signed(12.0f), signed(12.0f)))
        (origin, direction, triangle)
      }
      // Primitive 0x173c and lane 5 of the historical 16x9 reflections
      // divergence.  Vertex words are after the sample's glTF node transform
      // and FlipY preprocessing, i.e. the exact build-AS vertex buffer.
      val sceneReplay = (
        Seq(f32("00000000"), f32("bf000000"), f32("40000000")),
        Seq(f32("be9c607d"), f32("00000000"), f32("bf73c477")),
        Seq(f32("c0518b86"), f32("7f800000"), f32("bf866c57")),
        Seq(Seq(f32("becf7f87"), f32("bef35f5d"), f32("3f2f5d7e")),
          Seq(f32("bed72703"), f32("bf00ab08"), f32("3f319cec")),
          Seq(f32("beea12bb"), f32("bf00ab08"), f32("3f1e862c"))))
      val cases = rays.zip(triangles).map { case ((o, d), t) => (o, d, d.map(x => 1.0f / x), t) } ++
        Seq(sceneReplay) ++ fuzz.map { case (o, d, t) => (o, d, d.map(x => 1.0f / x), t) }
      cases.zipWithIndex.foreach { case ((origin, direction, inverse, triangle), id) =>
        val expected = reference(origin, direction, inverse, triangle)
        if (id == 2) {
          // The third case is sceneReplay.  These committed-record words came
          // from the matching RTL trace and include the final IEEE divisions.
          assert(raw(expected.t / expected.denom) == BigInt("3fb009b7", 16))
          assert(raw(expected.u / expected.denom) == BigInt("3f5ec7f3", 16))
          assert(raw(expected.v / expected.denom) == BigInt("3d0b8eb0", 16))
        }
        poke(dut, id, origin, direction, inverse, triangle)
        dut.in.valid.poke(true.B)
        while (!dut.in.ready.peek().litToBoolean) dut.clock.step()
        dut.clock.step(); dut.in.valid.poke(false.B)
        var cycles = 0; while (!dut.out.valid.peek().litToBoolean && cycles < 40) { dut.clock.step(); cycles += 1 }
        assert(dut.out.valid.peek().litToBoolean, s"request $id did not leave the pipeline")
        dut.out.bits.requestId.expect(id.U); dut.out.bits.hit.expect(expected.hit.B)
        dut.out.bits.frontFace.expect(expected.frontFace.B)
        dut.out.bits.uNum.expect(raw(expected.u).U); dut.out.bits.vNum.expect(raw(expected.v).U)
        dut.out.bits.tNum.expect(raw(expected.t).U); dut.out.bits.tDenom.expect(raw(expected.denom).U)
        dut.clock.step()
      }
    }
  }
}

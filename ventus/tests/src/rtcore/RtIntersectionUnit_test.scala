package rtcore

import org.scalatest.freespec.AnyFreeSpec

/** Baseline configuration contract for the RTcore intersection unit. */
class RtIntersectionUnitConfigurationTest extends AnyFreeSpec {
  "baseline intersection configuration uses private box and triangle FUs" in {
    val p = RtIntersectionParams()

    assert(!p.io_recorded_float)
    assert(p.internal_recorded_float)
    assert(p.support_euclidean.isEmpty)
    assert(p.disjointPipes)
  }
}

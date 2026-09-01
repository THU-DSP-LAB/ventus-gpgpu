package rtcore

import org.scalatest.freespec.AnyFreeSpec

/** Guards the generated Mesa ABI assumptions consumed by RTcore tables. */
class RtAbiLayoutTest extends AnyFreeSpec {
  "PDS fixed header keeps the twelve ray-state fields and both hit records field-major" in {
    val rayEnd = RtAbiLayout.SlotAccelLo + RtMemTableParameters.pdsRayStateFieldCount
    val candidateBase = RtAbiLayout.AbiCandidateHitRecordBaseBytes / RtAbiLayout.AbiWordBytes
    val committedBase = RtAbiLayout.AbiCommittedHitRecordBaseBytes / RtAbiLayout.AbiWordBytes

    assert(RtAbiLayout.AbiWordBytes == 4)
    assert(rayEnd <= RtAbiLayout.SlotWordCount)
    assert(candidateBase + RtAbiLayout.HitRecordWordCount == committedBase)
    assert(committedBase + RtAbiLayout.HitRecordWordCount ==
      RtAbiLayout.AbiPdsFieldMajorFixedHeaderWordCount)
    assert(RtAbiLayout.HitRecordWordCount == RtMemTableParameters.hitRecordStorageWordCount)
  }

  "VTAS node references and cacheline node sizes match scheduler fetch rules" in {
    assert(RtVtasLayout.AsHeaderSize == RtCorePortParameters.lineBytes)
    assert(RtVtasLayout.AsBox4NodeSize == RtCorePortParameters.lineBytes)
    assert(RtVtasLayout.AsTriangleNodeSize == RtCorePortParameters.pdsWriteBytes)
    assert((RtVtasLayout.NodeRefTypeMask & RtVtasLayout.NodeRefOffsetMask) == 0)
    assert((RtVtasLayout.InvalidNodeRef & RtVtasLayout.NodeRefTypeMask) == RtVtasLayout.NodeRefTypeMask)
  }
}

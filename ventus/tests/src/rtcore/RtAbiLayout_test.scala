package rtcore

import org.scalatest.freespec.AnyFreeSpec

/** Guards the generated Mesa ABI assumptions consumed by RTcore tables. */
class RtAbiLayoutTest extends AnyFreeSpec {
  "RT Local SRAM keeps the fifteen ray-state fields and both hit records field-major" in {
    val rayEnd = RtAbiLayout.SlotInvZ + 1
    val candidateBase = RtAbiLayout.LocalCandidateHitRecordBaseBytes / RtAbiLayout.AbiWordBytes
    val committedBase = RtAbiLayout.LocalCommittedHitRecordBaseBytes / RtAbiLayout.AbiWordBytes

    assert(RtAbiLayout.AbiWordBytes == 4)
    assert(RtAbiLayout.AbiVersion == 13)
    assert(RtAbiLayout.SlotReservedLo == 16)
    assert(RtAbiLayout.SlotReservedHi == 17)
    assert(rayEnd <= RtAbiLayout.SlotWordCount)
    assert(candidateBase + RtAbiLayout.HitRecordWordCount == committedBase)
    assert(committedBase + RtAbiLayout.HitRecordWordCount ==
      RtAbiLayout.LocalFieldMajorFixedHeaderWordCount)
    assert(RtAbiLayout.HitRecordWordCount == RtMemTableParameters.hitRecordStorageWordCount)
  }

  "VTAS node references and cacheline node sizes match scheduler fetch rules" in {
    assert(RtVtasLayout.AsHeaderSize == RtCorePortParameters.lineBytes)
    assert(RtVtasLayout.AsBox4NodeSize == RtCorePortParameters.lineBytes)
    assert(RtVtasLayout.AsTriangleNodeSize == 64)
    assert((RtVtasLayout.NodeRefTypeMask & RtVtasLayout.NodeRefOffsetMask) == 0)
    assert((RtVtasLayout.InvalidNodeRef & RtVtasLayout.NodeRefTypeMask) == RtVtasLayout.NodeRefTypeMask)
  }
}

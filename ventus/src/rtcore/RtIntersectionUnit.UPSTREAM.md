# RayFlex provenance

The baseline box/triangle intersection pipeline in this directory is derived
from RayFlex's latest revision that contains the separate disjoint
implementation, `b835680cf66dae2b332bbe5ab8c2cdf16f2edb45`:

<https://github.com/purdue-aalp/rayflex>

RayFlex provenance is limited to `RtIntersectionUnit.scala`,
`RtIntersectionUnitIO.scala`,
`RtIntersectionCompareSelect.scala`, `RtIntersectionCompareAndSwap.scala`,
`RtIntersectionQuadSort.scala`, `RtIntersectionQuadSortWithIndex.scala`, and
`RtIntersectionFnHelpers.scala`. `RtCorePort.scala`, `RtMemTable.scala`, and
`RtAbiLayout.scala` are Ventus-owned RTcore interface and ABI work, not
RayFlex-derived source.

Imported source was renamed into the `rtcore` package and fixed to the
baseline disjoint configuration: four AABB candidates and one triangle per
transaction.  Euclidean and angular-distance operations are not enabled.
Box and triangle operations select separate private slices of the functional
unit arrays; no box/triangle FU is shared. The original per-stage skid buffers
have been replaced by Ventus-owned fixed-latency stages in
`RtIntersectionPipelineStage.scala`. Arithmetic stages never stall; each input
reserves a slot in a terminal result FIFO (8 Box4 / 16 Triangle). Registered
outstanding counts gate admission without a downstream-ready chain. The
interfaces retain 7/12-cycle unstalled latency and one-request-per-cycle
throughput. Finite result capacity still applies input backpressure when full.

RayFlex's `external/hardfloat` directory is deliberately not imported.  The
unit resolves `hardfloat` from Ventus's existing
`gpgpu/dependencies/berkeley-hardfloat` build dependency.  That dependency's
version and numerical behavior must be checked against the pinned RayFlex
source before claiming bit-for-bit equivalence.

`RAYFLEX_LICENSE` preserves the upstream BSD-3-Clause license.  The imported
file headers retain its attribution; local modifications must continue to
preserve both records.

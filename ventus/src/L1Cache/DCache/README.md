# DCache — Status

## TL;DR

- **Default (v2, new cache)**: `DCachev2.scala` — `DataCachev2` is what
  `GPGPU_top.scala` instantiates as the production L1 data cache.
- **Legacy (v1)**: `DCache.scala` — `DataCache` is retained only for the
  existing unit-test harness (`L1CacheTest.scala`). Do not introduce new
  usage. It is not wired into the SoC top anymore.

This file exists as a pointer: *the cache switchover to v2 is complete on
`develop`.* If you arrive at this repo and wonder "which one is live?", it
is `DCachev2`.

## Why v2

`DCachev2` was developed on the `yffcache` branch and then merged into
`develop` in a single fast-forward. The highlights over v1:

- Unified tag/data SRAM write source on refill (no more stale tags on
  replace).
- Reworked refill/replace alignment and MSHR/WSHR pipelining fixes.
- L2 flush tag-cycle alignment fix; invalidate pulse fix.
- Several MemRsp `allocate_valid` / `isSpecial` init fixes.

See individual commit messages on `develop` for full attribution.

## Pointers

- Top-level instantiation: `ventus/src/top/GPGPU_top.scala` — search for
  `new DataCachev2`.
- Parameter knobs: `DCacheParameters.scala`. If you change the set count,
  block size, or tag layout, remember that the no-cache simulator
  (`sim-verilator-nocache/ventus_rtlsim_impl.cpp`) decodes dcache requests
  on the host side using the same constants — keep them in sync (see
  `L1D_NUM_SET` / `L1D_BLOCK_NUM_WORD` there, which must match
  `rtl_parameters.dcache_NSets` / `dcache_BlockWords`).
- Deep-dive notes for anyone debugging DCachev2: `AGENT_CONTEXT/` — start
  from `00_arch_overview.md` and `DCachev2_AGENT_ENTRY.md`.

## Removing v1 later

`DCache.scala` can be deleted once `L1CacheTest` is either retired or
ported to drive `DataCachev2`. Until then the file stays; treat it as
read-only legacy.

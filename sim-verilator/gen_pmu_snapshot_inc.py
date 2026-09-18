#!/usr/bin/env python3
"""Generate Verilator PMU snapshot copy statements from RTL parameters.

The with-cache RTL exposes PMU counters as flattened top-level ports whose names
include the SM index. This script keeps the C++ sampling code synchronized with
the Chisel-exported num_sm parameter instead of hard-coding the number of SMs in
hand-written C++.
"""

import json
import sys
from pathlib import Path

PIPELINE_FIELDS = (
    ("active_cycles", "activeCycles"),
    ("total_scalar_issued", "totalScalarIssued"),
    ("total_vector_issued", "totalVectorIssued"),
    ("exec_structural_hazard_cycles_x", "execStructuralHazardCyclesX"),
    ("exec_structural_hazard_cycles_v", "execStructuralHazardCyclesV"),
    ("data_dep_stall_cycles", "dataDepStallCycles"),
    ("barrier_stall_cycles", "barrierStallCycles"),
    ("control_hazard_flush_count", "controlHazardFlushCount"),
    ("frontend_stall_cycles", "frontendStallCycles"),
    ("lsu_backpressure_cycles", "lsuBackpressureCycles"),
    ("ibuffer_full_cycles", "ibufferFullCycles"),
)

INST_CLASS_FIELDS = (
    ("compute_issued", "computeIssued"),
    ("mem_issued", "memIssued"),
    ("ctrl_issued", "ctrlIssued"),
)

DCACHE_FIELDS = (
    ("total_req", "totalReq"),
    ("read_req", "readReq"),
    ("write_req", "writeReq"),
    ("read_miss", "readMiss"),
    ("write_miss", "writeMiss"),
    ("read_primary_miss", "readPrimaryMiss"),
    ("read_secondary_miss", "readSecondaryMiss"),
    ("read_primary_full_miss", "readPrimaryFullMiss"),
    ("read_secondary_full_miss", "readSecondaryFullMiss"),
    ("write_fresh_miss", "writeFreshMiss"),
    ("write_inflight_miss", "writeInflightMiss"),
    ("replacements", "replacements"),
    ("dirty_writebacks", "dirtyWritebacks"),
    ("mshr_full_cycles", "mshrFullCycles"),
    ("rtab_replays", "rtabReplays"),
    ("bank_conflict_cycles", "bankConflictCycles"),
    ("core_req_pipe_pipelined_cycles", "coreReqPipePipelinedCycles"),
    ("mem_rsp_pipe_decoupled_cycles", "memRspPipeDecoupledCycles"),
)

RTCORE_FIELDS = (
    ("traverse_issued", "traverseIssued"),
    ("traverse_active_lanes", "traverseActiveLanes"),
    ("completion_count", "completionCount"),
    ("completion_lanes", "completionLanes"),
    ("release_issued", "releaseIssued"),
    ("active_session_cycles", "activeSessionCycles"),
    ("command_backpressure_cycles", "commandBackpressureCycles"),
    ("completion_backpressure_cycles", "completionBackpressureCycles"),
    ("release_backpressure_cycles", "releaseBackpressureCycles"),
    ("local_sram_read_requests", "localSramReadRequests"),
    ("local_sram_write_requests", "localSramWriteRequests"),
    ("local_sram_read_responses", "localSramReadResponses"),
    ("local_sram_backpressure_cycles", "localSramBackpressureCycles"),
    ("ray_setup_outputs", "raySetupOutputs"),
    ("as_line_acquires", "asLineAcquires"),
    ("as_line_ready", "asLineReady"),
    ("as_acquire_backpressure_cycles", "asAcquireBackpressureCycles"),
    ("as_cache_resident_hits", "asCacheResidentHits"),
    ("as_cache_pending_merges", "asCachePendingMerges"),
    ("as_cache_misses", "asCacheMisses"),
    ("as_cache_refills", "asCacheRefills"),
    ("as_cache_evictions", "asCacheEvictions"),
    ("box4_issued", "box4Issued"),
    ("box4_results", "box4Results"),
    ("triangle_issued", "triangleIssued"),
    ("triangle_results", "triangleResults"),
    ("triangle_resolve_issued", "triangleResolveIssued"),
    ("triangle_resolve_results", "triangleResolveResults"),
    ("stack_pushes", "stackPushes"),
    ("stack_pops", "stackPops"),
)


def parameter_u32(parameters: dict, name: str) -> int:
    value = parameters.get(name)
    if not isinstance(value, int) or value < 0:
        raise ValueError(f"RTL parameter {name} is missing or invalid")
    return value


def emit_sm_copy(sm: int, include_pipeline: bool) -> list[str]:
    lines: list[str] = []
    if include_pipeline:
        lines.extend([
            f"    {{",
            f"        auto& dst = pmu_snapshot.pipeline[{sm}];",
        ])
        for dst, src in PIPELINE_FIELDS:
            lines.append(f"        dst.{dst} = dut->io_pmu_pipeline_{sm}_{src};")
        lines.extend([
            f"    }}",
            f"    {{",
            f"        auto& dst = pmu_snapshot.inst_class[{sm}];",
        ])
        for dst, src in INST_CLASS_FIELDS:
            lines.append(f"        dst.{dst} = dut->io_pmu_instClass_{sm}_{src};")
        lines.append(f"    }}")
    lines.extend([
        f"    {{",
        f"        auto& dst = pmu_snapshot.dcache[{sm}];",
    ])
    for dst, src in DCACHE_FIELDS:
        lines.append(f"        dst.{dst} = dut->io_pmu_dcache_{sm}_{src};")
    lines.extend([
        f"    }}",
        f"    {{",
        f"        auto& dst = pmu_snapshot.rtcore[{sm}];",
    ])
    for dst, src in RTCORE_FIELDS:
        lines.append(f"        dst.{dst} = dut->io_pmu_rtcore_{sm}_{src};")
    lines.append(f"    }}")
    return lines


def main() -> int:
    if len(sys.argv) != 4 or sys.argv[3] not in {"off", "basic", "detailed"}:
        print("usage: gen_pmu_snapshot_inc.py parameters.json output.inc off|basic|detailed",
              file=sys.stderr)
        return 2

    params_path = Path(sys.argv[1])
    output_path = Path(sys.argv[2])
    parameters = json.loads(params_path.read_text(encoding="utf-8"))
    num_sm = parameter_u32(parameters, "num_sm")
    pmu_mode = sys.argv[3]

    lines = [
        "// Generated by gen_pmu_snapshot_inc.py; do not edit by hand.",
        f"static constexpr uint32_t GENERATED_PMU_NUM_SM = {num_sm};",
        "",
        "void ventus_rtlsim_t::copy_pmu_counters_from_dut() {",
    ]
    for sm in range(num_sm):
        lines.extend(emit_sm_copy(sm, pmu_mode != "off"))
    lines.append("}")
    lines.append("")

    lines.append("void ventus_rtlsim_t::sample_detailed_pmu() {")
    if pmu_mode == "detailed":
        lines.append(f"    if (detailed_pmu.empty()) detailed_pmu.resize({num_sm});")
        scalar_fields = "enabled clear session activeMask command commandMask commandKind release stackReady stackSelected popBlockedHeader popBlockedPush popBlockedHeaderLine asBlocked readyBlockedRay readyBlockedBox readyBlockedTriangle lsuRequestTag lsuResponseTag".split()
        as_fields = "occupied returned readyEntries acquire allocate acquireEntry address is64B acquireLane refill refillEntry injection injectionEntry serve serveEntry serveLane serveLanes eviction clear".split()
        unit_fields = "inValid inReady outValid outReady".split()
        for sm in range(num_sm):
            prefix = f"dut->io_pmu_rtcore_{sm}_detailed_"
            lines.extend(["    {", "        RtDetailedSample s;"])
            for field in scalar_fields:
                lines.append(f"        s.{field} = {prefix}{field};")
            for lane in range(32):
                lines.append(f"        s.laneState[{lane}] = {prefix}laneState_{lane};")
            for field in as_fields:
                lines.append(f"        s.asTable.{field} = {prefix}asTable_{field};")
            for lane in range(32):
                lines.append(f"        s.asTable.serveEntries[{lane}] = {prefix}asTable_serveEntries_{lane};")
            for field in unit_fields:
                lines.append(f"        s.lsu.{field} = {prefix}lsu_{field};")
                for unit in range(11):
                    lines.append(f"        s.units[{unit}].{field} = {prefix}units_{unit}_{field};")
            lines.extend([f"        detailed_pmu[{sm}].sample(s);", "    }"])
        num_l2 = parameter_u32(parameters, "num_l2cache")
        lines.append(f"    if (l2_pmu.empty()) l2_pmu.resize({num_l2});")
        l2_fields = "request requestBlocked response responseBlocked memoryRequest memoryRequestBlocked memoryResponse memoryResponseBlocked directoryHit directoryMiss directoryBlocked".split()
        for cache in range(num_l2):
            prefix = f"dut->io_pmu_l2_{cache}_"
            lines.append(f"    if ({prefix}clear) l2_pmu[{cache}] = RtPmuStatistics{{}};")
            lines.append(f"    else if ({prefix}enabled) {{")
            lines.append(f'        l2_pmu[{cache}].count("sampled_cycles");')
            for field in l2_fields:
                lines.append(f'        l2_pmu[{cache}].count("{field}", {prefix}{field});')
            lines.append("    }")
    lines.append("}")
    lines.append("int ventus_rtlsim_t::dump_extended_pmu(FILE* stream) const {")
    def put(value: str) -> None:
        lines.append(f"    std::fputs({json.dumps(value)}, stream);")
    put('{"schema_version":1,"mode":"' + pmu_mode + '","sm":[')
    for sm in range(num_sm):
        put(("," if sm else "") + '{"id":' + str(sm))
        for group, fields in (("pipeline", PIPELINE_FIELDS), ("inst_class", INST_CLASS_FIELDS), ("dcache", DCACHE_FIELDS)):
            if pmu_mode == "off":
                put(',"' + group + '":null')
                continue
            put(',"' + group + '":{')
            for index, (field, _) in enumerate(fields):
                fmt = ("," if index else "") + '"' + field + '":%llu'
                lines.append(f"    std::fprintf(stream, {json.dumps(fmt)}, static_cast<unsigned long long>(pmu_snapshot.{group}[{sm}].{field}));")
            put('}')
        put(',"rt_detailed":')
        if pmu_mode == "detailed":
            lines.append(f'    if (detailed_pmu.size() > {sm}) detailed_pmu[{sm}].dump(stream); else std::fputs("null", stream);')
        else:
            put('null')
        put('}')
    put('],"l2":')
    if pmu_mode == "detailed":
        put('[')
        lines.extend(['    for (size_t i = 0; i < l2_pmu.size(); ++i) {',
                      "        if (i) std::fputc(',', stream);",
                      '        l2_pmu[i].dump(stream);', '    }'])
        put(']')
    else:
        put('null')
    put('}')
    lines.extend(["    return std::ferror(stream) ? -1 : 0;", "}", ""])

    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text("\n".join(lines), encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

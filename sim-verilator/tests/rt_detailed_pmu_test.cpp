#include "rt_detailed_pmu.hpp"
#include <cassert>

int main() {
    // Three node visits share an entry: miss, pending merge, resident hit.
    // First-ready is independent of consumption; two lanes may serve together.
    {
        RtDetailedPmu nodes;
        auto sample = [] { RtDetailedSample s; s.enabled = true; return s; };
        auto s = sample(); s.nodePopLanes = 7;
        s.nodePopKinds[0] = 1; s.nodePopKinds[1] = 2; s.nodePopKinds[2] = 3;
        nodes.sample(s); // 0
        s = sample(); s.nodeAcquire = s.asTable.acquire = s.asTable.allocate = true;
        s.asTable.acquireEntry = 5; s.asTable.address = 128;
        nodes.sample(s); // 1: miss lane0
        s = sample(); s.nodeAcquire = s.asTable.acquire = true;
        s.asTable.acquireLane = 1; s.asTable.acquireEntry = 5; s.asTable.address = 128;
        nodes.sample(s); // 2: merge lane1
        s = sample(); s.asTable.injection = true; s.asTable.injectionEntry = 5;
        nodes.sample(s); // 3: injection alone is not ready metadata
        s = sample(); s.nodeAcquire = s.asTable.acquire = s.asTable.residentHit = true;
        s.asTable.acquireLane = 2; s.asTable.acquireEntry = 5; s.asTable.address = 128;
        s.asTable.readyLanes = 3; s.asTable.serveLanes = 2;
        s.asTable.serveEntries[0] = s.asTable.serveEntries[1] = 5;
        nodes.sample(s); // 4: lane1 ready and consumed, lane2 resident acquire
        s = sample(); s.asTable.readyLanes = 5;
        s.asTable.serveEntries[0] = s.asTable.serveEntries[2] = 5;
        nodes.sample(s); // 5: lane2 first-ready
        s.asTable.serveLanes = 5;
        nodes.sample(s); // 6: consume lane0 and lane2 simultaneously
        const auto& c = nodes.stats.counters;
        assert(c.at("node_completed") == 3);
        assert(c.at("node_pop_to_acquire_lane_cycles") == 7);
        assert(c.at("node_acquire_to_ready_lane_cycles") == 6);
        assert(c.at("node_ready_to_serve_lane_cycles") == 3);
        assert(c.at("node_total_lane_cycles") == 16);
        assert(nodes.stats.histograms["node_path_miss_ready_to_serve_cycles"][2] == 1);
        assert(nodes.stats.histograms["node_path_merge_ready_to_serve_cycles"][0] == 1);
        assert(nodes.stats.histograms["node_path_resident_acquire_to_ready_cycles"][1] == 1);
        assert(nodes.stats.counters["node_kind_3_path_resident_completed"] == 1);
        assert(nodes.stats.counters["node_unmatched_serves"] == 0);
        // New use of the same lane must not inherit the prior visit's timestamps.
        s = sample(); s.nodePopLanes = 1; s.nodePopKinds[0] = 4; nodes.sample(s);
        s = sample(); s.release = true; nodes.sample(s);
        assert(nodes.stats.counters["node_unfinished_at_release"] == 1);
        assert(nodes.stats.counters["node_completed"] == 3);
        s = sample(); s.clear = true; nodes.sample(s);
        assert(nodes.stats.counters.empty());
    }
    RtDetailedPmu pmu;
    RtDetailedSample s;
    pmu.sample(s); // disabled cycles must not be counted
    assert(pmu.stats.counters.empty());
    RtDetailedPmu tagged;
    s = RtDetailedSample{}; s.enabled = true;
    s.lsu = {true, true, false, true}; s.lsuRequestTag = 7;
    tagged.sample(s); // 0: A
    s.lsuRequestTag = 23; tagged.sample(s); // 1: B
    s.lsu = {false, true, true, false}; s.lsuResponseTag = 23;
    tagged.sample(s); // 2: B blocked
    s.lsu.outReady = true; tagged.sample(s); // 3: B latency 2
    s.lsu = {true, true, true, true};
    s.lsuRequestTag = 12; s.lsuResponseTag = 7;
    tagged.sample(s); // 4: A latency 4; C accepted simultaneously
    s.lsu = {false, true, true, true}; s.lsuResponseTag = 12;
    tagged.sample(s); // 5: C latency 1
    tagged.dump(stdout); std::fputc('\n', stdout);
    // A retained residency ends on eviction/clear, not on its last waiter.
    RtDetailedPmu retained;
    s = RtDetailedSample{}; s.enabled = true;
    s.asTable.acquire = s.asTable.allocate = true; s.asTable.address = 128;
    retained.sample(s);
    s = RtDetailedSample{}; s.enabled = true; s.asTable.serve = true; s.asTable.serveLanes = 1;
    retained.sample(s);
    assert(retained.stats.histograms["as_completed_entry_uses"].empty());
    s = RtDetailedSample{}; s.enabled = true;
    s.asTable.acquire = s.asTable.allocate = s.asTable.eviction = true;
    s.asTable.address = 256;
    retained.sample(s);
    s = RtDetailedSample{}; s.enabled = true; s.asTable.serve = true; s.asTable.serveLanes = 1;
    retained.sample(s);
    s = RtDetailedSample{}; s.enabled = true;
    s.asTable.acquire = s.asTable.allocate = s.asTable.eviction = true;
    s.asTable.address = 128;
    retained.sample(s);
    s = RtDetailedSample{}; s.enabled = true; s.asTable.serve = true; s.asTable.serveLanes = 1;
    retained.sample(s);
    s = RtDetailedSample{}; s.enabled = true; s.asTable.clear = true;
    retained.sample(s);
    assert(retained.stats.histograms["as_completed_entry_uses"][1] == 3);
    assert(retained.stats.counters["as_post_eviction_reaccesses"] == 1);
    assert(retained.stats.counters["as_evictions"] == 2);
    assert(retained.stats.counters["unexpected_live_entry_allocation"] == 0);
    assert(retained.stats.counters["unmatched_as_serves"] == 0);
    s = RtDetailedSample{};
    s.enabled = true; s.session = true; s.activeMask = 3;
    s.command = true; s.commandMask = 3;
    s.laneState[0] = 4; s.laneState[1] = 5;
    s.stackReady = 1; s.stackSelected = true;
    s.units[0] = {true, true, false, true};
    s.units[1] = {true, true, false, true}; // parallel issue must not collapse to OR
    s.asTable.acquire = s.asTable.allocate = true;
    s.asTable.address = 128;
    pmu.sample(s); // cycle 0: allocate entry 0, Box4 issue
    s = RtDetailedSample{}; s.enabled = true;
    s.asTable.refill = true;
    s.lsu = {true, true, false, true};
    pmu.sample(s); // cycle 1: refill and LSU request
    s = RtDetailedSample{}; s.enabled = true;
    s.units[0] = {false, true, true, false};
    s.asTable.acquire = true; s.asTable.address = 128;
    s.asTable.acquireLane = 1; // merge second waiter
    pmu.sample(s); // cycle 2: blocked result
    s = RtDetailedSample{}; s.enabled = true;
    s.units[0] = {true, true, true, true}; // old result + new issue same edge
    s.units[1] = {false, true, true, true};
    s.lsu = {false, true, true, true};
    s.asTable.injection = true;
    pmu.sample(s); // cycle 3
    s = RtDetailedSample{}; s.enabled = true;
    s.asTable.serve = true; s.asTable.serveLanes = 1;
    pmu.sample(s); // cycle 4: waiter 0
    s.asTable.serveLane = 1; s.asTable.serveLanes = 2; s.asTable.serveEntries[1] = 0;
    pmu.sample(s); // cycle 5: waiter 1, residency remains live
    assert(pmu.stats.histograms["as_completed_entry_uses"].empty());
    s = RtDetailedSample{}; s.enabled = true; s.asTable.clear = true;
    pmu.sample(s); // cycle 6: table clear closes the residency
    s = RtDetailedSample{}; s.enabled = true;
    s.asTable.acquire = s.asTable.allocate = true; s.asTable.address = 128;
    pmu.sample(s); // cycle 7: same address fetched again (not eviction)
    assert(pmu.stats.histograms["as_completed_entry_uses"][2] == 1);
    assert(pmu.stats.histograms["as_completed_entry_unique_lanes"][2] == 1);
    assert(pmu.stats.histograms["as_acquire_to_serve_cycles"][4] == 1);
    assert(pmu.stats.histograms["as_acquire_to_serve_cycles"][3] == 1);
    assert(pmu.stats.counters["as_repeat_allocations_in_session"] == 1);
    assert(pmu.stats.counters["state_5_lane_cycles"] == 1);
    assert(pmu.stats.histograms["iu_issues_per_cycle"][2] == 1);
    pmu.dump(stdout); std::fputc('\n', stdout);
    // Repeated snapshots must be read-only, including censored samples.
    pmu.dump(stdout); std::fputc('\n', stdout);
    s = RtDetailedSample{}; s.enabled = true; s.release = true;
    pmu.sample(s);
    assert(pmu.stats.histograms["as_fetches_per_address_per_session"][2] == 1);
    s = RtDetailedSample{}; s.enabled = true;
    s.asTable.acquire = s.asTable.allocate = true; s.asTable.address = 128; s.asTable.acquireEntry = 1;
    pmu.sample(s);
    assert(pmu.stats.counters["as_repeat_allocations_in_session"] == 1);
    s.clear = true; pmu.sample(s);
    assert(pmu.stats.counters.empty());
}

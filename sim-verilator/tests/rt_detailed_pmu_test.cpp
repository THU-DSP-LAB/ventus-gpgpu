#include "rt_detailed_pmu.hpp"
#include <cassert>

int main() {
    RtDetailedPmu pmu;
    RtDetailedSample s;
    pmu.sample(s); // disabled cycles must not be counted
    assert(pmu.stats.counters.empty());
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
    s.asTable.serve = true;
    pmu.sample(s); // cycle 4: waiter 0
    s.asTable.serveLane = 1; s.asTable.retire = true;
    pmu.sample(s); // cycle 5: waiter 1, retire
    s = RtDetailedSample{}; s.enabled = true;
    s.asTable.acquire = s.asTable.allocate = true; s.asTable.address = 128;
    pmu.sample(s); // cycle 6: same address fetched again (not eviction)
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

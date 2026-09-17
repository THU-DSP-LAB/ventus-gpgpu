#pragma once

// Host-only observers: sampled once before the active clock edge. Never drive DUT inputs.
#include <array>
#include <algorithm>
#include <cstdint>
#include <cstdio>
#include <deque>
#include <map>
#include <set>
#include <string>

struct RtUnitSample {
    bool inValid = false, inReady = false, outValid = false, outReady = false;
};
struct RtAsSample {
    uint32_t occupied = 0, returned = 0, readyEntries = 0;
    bool acquire = false, allocate = false, is64B = false;
    uint32_t acquireEntry = 0, address = 0, acquireLane = 0;
    bool refill = false, injection = false, serve = false;
    bool eviction = false, clear = false;
    uint32_t refillEntry = 0, injectionEntry = 0, serveEntry = 0, serveLane = 0;
};
struct RtDetailedSample {
    bool enabled = false, clear = false, session = false, command = false, release = false;
    uint32_t activeMask = 0, commandMask = 0, commandKind = 0;
    std::array<uint32_t, 32> laneState{};
    uint32_t stackReady = 0;
    bool stackSelected = false, popBlockedHeader = false, popBlockedPush = false;
    bool popBlockedHeaderLine = false, asBlocked = false;
    bool readyBlockedRay = false, readyBlockedBox = false, readyBlockedTriangle = false;
    RtUnitSample lsu;
    uint32_t lsuRequestTag = 0, lsuResponseTag = 0;
    std::array<RtUnitSample, 11> units{}; // 8 Box4, 2 Triangle, 1 resolve
    RtAsSample asTable;
};

struct RtPmuStatistics {
    std::map<std::string, uint64_t> counters;
    std::map<std::string, std::map<uint64_t, uint64_t>> histograms;
    void count(const std::string& key, uint64_t n = 1) { counters[key] += n; }
    void bin(const std::string& key, uint64_t value) { ++histograms[key][value]; }
    void dump(FILE* f) const {
        std::fputs("{\"counters\":{", f);
        bool first = true;
        for (const auto& kv : counters) {
            std::fprintf(f, "%s\"%s\":%llu", first ? "" : ",", kv.first.c_str(),
                         static_cast<unsigned long long>(kv.second));
            first = false;
        }
        std::fputs("},\"histograms\":{", f);
        first = true;
        for (const auto& h : histograms) {
            std::fprintf(f, "%s\"%s\":{", first ? "" : ",", h.first.c_str());
            first = false;
            bool first_bin = true;
            for (const auto& kv : h.second) {
                std::fprintf(f, "%s\"%llu\":%llu", first_bin ? "" : ",",
                             static_cast<unsigned long long>(kv.first),
                             static_cast<unsigned long long>(kv.second));
                first_bin = false;
            }
            std::fputc('}', f);
        }
        std::fputs("}}", f);
    }
};

class RtDetailedPmu {
    struct Unit {
        std::deque<std::pair<uint32_t, uint64_t>> accepted;
        RtPmuStatistics stats;
        void sample(const RtUnitSample& s, uint64_t cycle, bool tagged = false,
                    uint32_t requestTag = 0, uint32_t responseTag = 0) {
            stats.count("sampled_cycles");
            stats.count("busy_cycles", !accepted.empty());
            stats.count("ready_without_dispatch_cycles", s.inReady && !s.inValid);
            stats.count("empty_ready_without_dispatch_cycles", accepted.empty() && s.inReady && !s.inValid);
            stats.count("input_blocked_cycles", s.inValid && !s.inReady);
            stats.count("output_blocked_cycles", s.outValid && !s.outReady);
            stats.bin("outstanding", accepted.size());
            if (s.inValid && s.inReady) {
                if (tagged && std::any_of(accepted.begin(), accepted.end(),
                    [requestTag](const auto& p) { return p.first == requestTag; }))
                    stats.count("duplicate_request_tags");
                accepted.emplace_back(requestTag, cycle); stats.count("issues");
            }
            if (s.outValid && s.outReady) {
                stats.count("results");
                const auto it = tagged ? std::find_if(accepted.begin(), accepted.end(),
                    [responseTag](const auto& p) { return p.first == responseTag; }) : accepted.begin();
                if (it == accepted.end()) stats.count("unmatched_results");
                else { stats.bin("latency_cycles", cycle - it->second); accepted.erase(it); }
            }
        }
        void dump(FILE* f) const {
            auto snapshot = stats;
            snapshot.counters["outstanding_at_snapshot"] = accepted.size();
            snapshot.dump(f);
        }
    };
    struct Entry {
        bool live = false, sent = false;
        uint64_t uses = 0, acquired = 0, refill = 0, key = 0;
        uint32_t laneMask = 0;
    };
    uint64_t cycle = 0;
    std::array<Unit, 11> units;
    Unit lsu;
    std::array<Entry, 32> entries{};
    std::array<std::deque<uint64_t>, 32> laneAcquires;
    // Address + line size, scoped to a session (not reused virtual addresses in another kernel).
    std::map<uint64_t, uint64_t> fetches;
    std::set<uint64_t> evicted;
    void close_entry(Entry& e) {
        if (!e.live) return;
        stats.bin("as_completed_entry_uses", e.uses);
        stats.bin("as_completed_entry_unique_lanes", __builtin_popcount(e.laneMask));
        e = Entry{};
    }
    std::array<std::map<uint64_t, uint64_t>, 32> laneUses;
    void close_session() {
        for (const auto& kv : fetches) stats.bin("as_fetches_per_address_per_session", kv.second);
        fetches.clear();
        evicted.clear();
        for (unsigned i = 0; i < laneUses.size(); ++i) {
            auto& lane = laneUses[i];
            for (const auto& kv : lane) {
                stats.bin("as_acquires_per_lane_address_per_session", kv.second);
                stats.bin("lane_" + std::to_string(i) + "_as_acquires_per_address_per_session", kv.second);
            }
            lane.clear();
        }
    }
public:
    RtPmuStatistics stats;
    void sample(const RtDetailedSample& s) {
        if (s.clear) { *this = RtDetailedPmu{}; return; }
        if (!s.enabled) return;
        stats.count("sampled_cycles");
        stats.count("session_cycles", s.session);
        if (s.command) {
            stats.bin("command_active_lanes", __builtin_popcount(s.commandMask));
            stats.bin("command_kind", s.commandKind);
        }
        if (s.session) {
            std::array<unsigned, 15> occupancy{};
            unsigned live = 0;
            for (unsigned lane = 0; lane < 32; ++lane) {
                if (!(s.activeMask & (uint32_t(1) << lane))) continue;
                auto state = s.laneState[lane];
                if (state >= occupancy.size()) { stats.count("invalid_lane_state"); continue; }
                ++occupancy[state];
                if (state != 0 && state != 9 && state != 14) ++live;
            }
            stats.bin("session_active_lanes", __builtin_popcount(s.activeMask));
            stats.bin("traversing_lanes", live);
            for (unsigned state = 0; state < occupancy.size(); ++state) {
                stats.bin("state_" + std::to_string(state) + "_lanes", occupancy[state]);
                stats.count("state_" + std::to_string(state) + "_lane_cycles", occupancy[state]);
            }
            stats.bin("stack_ready_entries", s.stackReady);
            stats.count("stack_selected", s.stackSelected);
            stats.count("stack_ready_not_selected_lane_cycles", s.stackReady - (s.stackSelected && s.stackReady ? 1 : 0));
        }
        stats.count("pop_blocked_header_cycles", s.popBlockedHeader);
        stats.count("pop_blocked_push_cycles", s.popBlockedPush);
        stats.count("pop_blocked_header_line_cycles", s.popBlockedHeaderLine);
        stats.count("as_acquire_blocked_cycles", s.asBlocked);
        stats.count("ready_blocked_ray_cycles", s.readyBlockedRay);
        stats.count("ready_blocked_box_cycles", s.readyBlockedBox);
        stats.count("ready_blocked_triangle_cycles", s.readyBlockedTriangle);
        unsigned issues = 0, inputReady = 0;
        for (unsigned i = 0; i < units.size(); ++i) {
            units[i].sample(s.units[i], cycle);
            issues += s.units[i].inValid && s.units[i].inReady;
            inputReady += s.units[i].inReady;
        }
        stats.bin("iu_issues_per_cycle", issues);
        stats.bin("iu_ready_inputs_per_cycle", inputReady);
        lsu.sample(s.lsu, cycle, true, s.lsuRequestTag, s.lsuResponseTag);
        const auto& a = s.asTable;
        stats.count("as_acquires", a.acquire);
        stats.count("as_allocations", a.allocate);
        stats.count("as_refills", a.refill);
        stats.count("as_serves", a.serve);
        stats.bin("as_occupied_entries", a.occupied);
        stats.bin("as_returned_entries", a.returned);
        stats.bin("as_unreturned_entries", a.occupied - a.returned);
        stats.bin("as_ready_entries", a.readyEntries);
        stats.count("as_ready_not_selected_entry_cycles", a.readyEntries - (a.serve && a.readyEntries ? 1 : 0));
        stats.count("ready_blocked_other_cycles", a.readyEntries && !a.serve &&
                    !s.readyBlockedRay && !s.readyBlockedBox && !s.readyBlockedTriangle);
        if (a.acquire) {
            const uint64_t key = (uint64_t(a.address) << 1) | a.is64B;
            auto& e = entries.at(a.acquireEntry);
            if (a.allocate) {
                if (a.eviction) {
                    if (!e.live) stats.count("unmatched_evictions");
                    else evicted.insert(e.key);
                    close_entry(e);
                    stats.count("as_evictions");
                }
                if (e.live) stats.count("unexpected_live_entry_allocation");
                e = Entry{}; e.live = true; e.acquired = cycle; e.key = key;
                stats.count("as_post_eviction_reaccesses", evicted.count(key) != 0);
                auto& seen = fetches[key];
                stats.count("as_repeat_allocations_in_session", seen != 0);
                ++seen;
            }
            ++laneUses.at(a.acquireLane)[key];
            laneAcquires.at(a.acquireLane).push_back(cycle);
        }
        if (a.refill) {
            auto& e = entries.at(a.refillEntry);
            e.sent = true; e.refill = cycle;
            stats.bin("as_allocate_to_refill_cycles", cycle - e.acquired);
        }
        if (a.injection) {
            const auto& e = entries.at(a.injectionEntry);
            if (e.live && e.sent) stats.bin("as_refill_to_injection_cycles", cycle - e.refill);
            else stats.count("unmatched_injections");
        }
        if (a.serve) {
            auto& e = entries.at(a.serveEntry);
            ++e.uses; e.laneMask |= uint32_t(1) << a.serveLane;
            auto& pending = laneAcquires.at(a.serveLane);
            if (pending.empty()) stats.count("unmatched_as_serves");
            else { stats.bin("as_acquire_to_serve_cycles", cycle - pending.front()); pending.pop_front(); }
        }
        if (a.clear) for (auto& e : entries) close_entry(e);
        if (s.release) close_session();
        ++cycle;
    }
    void dump(FILE* f) const {
        auto snapshot = stats;
        uint64_t unfinished = 0, unserved = 0;
        for (const auto& e : entries) if (e.live) {
            ++unfinished; snapshot.bin("as_live_entry_uses_at_snapshot", e.uses);
        }
        for (const auto& pending : laneAcquires) unserved += pending.size();
        snapshot.counters["as_live_entries_at_snapshot"] = unfinished;
        snapshot.counters["as_unserved_acquires_at_snapshot"] = unserved;
        // Incomplete session histories must not be counted as completed distributions.
        snapshot.counters["as_open_session_unique_addresses"] = fetches.size();
        const auto reaccess = stats.counters.find("as_post_eviction_reaccesses");
        std::fprintf(f, "{\"as_policy\":\"retained_reverse_replace\",\"post_eviction_reaccess\":%llu,\"statistics\":",
                     static_cast<unsigned long long>(reaccess == stats.counters.end() ? 0 : reaccess->second));
        snapshot.dump(f);
        std::fputs(",\"lsu\":", f); lsu.dump(f);
        std::fputs(",\"units\":[", f);
        for (unsigned i = 0; i < units.size(); ++i) {
            std::fprintf(f, "%s{\"kind\":\"%s\",\"index\":%u,\"statistics\":", i ? "," : "",
                         i < 8 ? "box4" : i < 10 ? "triangle" : "resolve", i < 8 ? i : i < 10 ? i - 8 : 0);
            units[i].dump(f); std::fputc('}', f);
        }
        std::fputs("]}", f);
    }
};

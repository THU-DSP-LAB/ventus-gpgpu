#pragma once

#include "Vdut.h"
#include "cta_sche_wrapper.hpp"
#include "physical_mem.hpp"
#include "ventus_rtlsim.h"
#include <array>
#include <bitset>
#include <deque>
#include <memory>
#include <queue>
#include <string>
#include <unordered_map>
#include <vector>
#include <verilated.h>
#include <verilated_fst_c.h>

extern const std::unordered_map<std::string, int> rtl_parameters;
#ifdef ENABLE_GVM
#include "gvm.hpp"
#endif // ENABLE_GVM

#define SNAPSHOT_WAKEUP_SIGNAL SIGRTMIN
typedef struct {
    bool is_child;
    uint64_t main_exit_time;        // when does the main simulation process exit
    std::deque<pid_t> children_pid; // front is newest, back is oldest
} snapshot_t;

struct ventus_rtlsim_pmu_storage_t {
    uint32_t num_sm = 0;
    bool has_dcache = false;
    std::vector<ventus_rtlsim_pipeline_pmu_t> pipeline;
    std::vector<ventus_rtlsim_inst_class_pmu_t> inst_class;
    std::vector<ventus_rtlsim_dcache_pmu_t> dcache;
};

using vaddr_t = uint32_t;
constexpr unsigned NUM_THREAD = 32;
constexpr unsigned NUM_SM = 2;

struct dcache_reqrsp_t {
    uint8_t sm_id;
    uint8_t instrId;
    uint8_t opcode;
    uint8_t param;
    vaddr_t setIdx;
    vaddr_t tag;
    std::bitset<NUM_THREAD> mask;
    std::array<vaddr_t, NUM_THREAD> blockOffset;
    std::array<uint8_t, NUM_THREAD> wordOffset1H;
    std::array<uint32_t, NUM_THREAD> data;
};

struct icache_reqrsp_t {
    uint8_t sm_id;
    uint8_t source;
    paddr_t addr;
    uint32_t data[32];
};

extern "C" struct ventus_rtlsim_t {
    std::shared_ptr<spdlog::logger> logger;
    VerilatedContext* contextp;
    Vdut* dut;
    VerilatedFstC* tfp;
    Cta* cta;
    snapshot_t snapshots;
    ventus_rtlsim_config_t config;
    ventus_rtlsim_step_result_t step_status;
    ventus_rtlsim_pmu_storage_t pmu_snapshot;
    uint32_t pmu_num_sm = 0;
    uint64_t last_pmu_progress_time = 0;
    uint64_t last_pmu_progress_value = 0;
    std::unique_ptr<PhysicalMemory> pmem;
#ifdef ENABLE_GVM
    gvm_t gvm;
#endif // ENABLE_GVM
    std::queue<std::unique_ptr<dcache_reqrsp_t>> dcache_queue;
    std::queue<std::unique_ptr<icache_reqrsp_t>> icache_queue;
    bool need_icache_invalidate = false;
    bool need_perf_dump_summary = false;

    void constructor(const ventus_rtlsim_config_t* config);
    void dut_reset() const;
    const ventus_rtlsim_step_result_t* step();
    void destructor(bool snapshot_rollback_forcing);
    void dump_testcase_pmu_summary();
    ventus_rtlsim_pmu_t pmu_view() const;
    void sample_pmu_snapshot();
    void update_pmu_watchdog();

    void waveform_dump() const;
    void snapshot_fork();
    void snapshot_rollback(uint64_t time);
    void snapshot_kill_all();
};

inline static paddr_t pmem_get_page_base(paddr_t paddr, uint64_t pagesize) { return paddr - paddr % pagesize; }

#pragma once

#include "Vdut.h"
#include "cta_sche_wrapper.hpp"
#include "physical_mem.hpp"
#include "ventus_rtlsim.h"
#include <deque>
#include <memory>
#include <string>
#include <unordered_map>
#include <vector>
#include <verilated.h>
#include <verilated_fst_c.h>

#ifndef VENTUS_RTL_MODEL_THREADS
#define VENTUS_RTL_MODEL_THREADS 1
#endif
static_assert(VENTUS_RTL_MODEL_THREADS > 0, "RTL model thread count must be positive");
inline constexpr unsigned kVentusRtlModelThreads = VENTUS_RTL_MODEL_THREADS;

extern const std::unordered_map<std::string, int> rtl_parameters;
#ifdef ENABLE_GVM
#include "gvm.hpp"
#endif // ENABLE_GVM

#define SNAPSHOT_WAKEUP_SIGNAL SIGRTMIN
struct snapshot_record_t {
    pid_t pid;
    uint64_t time;
};

typedef struct {
    bool is_child;
    uint64_t main_exit_time;               // when does the main simulation process exit
    std::deque<snapshot_record_t> children; // front is newest, back is oldest
} snapshot_t;

struct ventus_rtlsim_pmu_storage_t {
    uint32_t num_sm = 0;
    bool has_dcache = false;
    std::vector<ventus_rtlsim_pipeline_pmu_t> pipeline;
    std::vector<ventus_rtlsim_inst_class_pmu_t> inst_class;
    std::vector<ventus_rtlsim_dcache_pmu_t> dcache;
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
    bool need_icache_invalidate = false;
    bool need_perf_dump_summary = false;

    void constructor(const ventus_rtlsim_config_t* config, bool initialize_dut = true);
    bool save_state_binary(const std::string& filename);
    bool restore_state_binary(const std::string& filename);
    void dut_reset() const;
    const ventus_rtlsim_step_result_t* step();
    int destructor(bool snapshot_rollback_forcing);
    void dump_testcase_pmu_summary();
    ventus_rtlsim_pmu_t pmu_view() const;
    void sample_pmu_snapshot();
    void update_pmu_watchdog();
    void copy_pmu_counters_from_dut();

    void waveform_dump() const;
    void snapshot_fork();
    int snapshot_rollback(uint64_t time);
    void snapshot_kill_all();
};

inline static paddr_t pmem_get_page_base(paddr_t paddr, uint64_t pagesize) { return paddr - paddr % pagesize; }

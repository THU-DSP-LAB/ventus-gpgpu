#pragma once

#include "Vdut.h"
#include "cta_sche_wrapper.hpp"
#include "ventus_rtlsim.h"
#include <map>
#include <verilated.h>
#include <verilated_fst_c.h>

#define SNAPSHOT_WAKEUP_SIGNAL SIGRTMIN
typedef struct {
    bool is_child;
    uint64_t main_exit_time;        // when does the main simulation process exit
    std::deque<pid_t> children_pid; // front is newest, back is oldest
} snapshot_t;

extern "C" struct ventus_rtlsim_t {
    VerilatedContext* contextp;
    Vdut* dut;
    VerilatedFstC* tfp;
    Cta* cta;
    snapshot_t snapshots;
    ventus_rtlsim_config_t config;
    std::map<paddr_t, uint8_t*> pmem_map;
    ventus_rtlsim_step_result_t step_status;

    void constructor(const ventus_rtlsim_config_t* config);
    const ventus_rtlsim_step_result_t* step();
    void destructor(bool snapshot_rollback_forcing);

    bool pmem_page_alloc(paddr_t paddr);
    bool pmem_page_free(paddr_t paddr);
    bool pmem_write(paddr_t paddr, const void* data, const bool mask[], uint64_t size);
    bool pmem_write(paddr_t paddr, const void* data, uint64_t size);
    bool pmem_read(paddr_t paddr, void* data, uint64_t size);

    void dut_reset() const;
    void waveform_dump() const;
    void snapshot_fork();
    void snapshot_rollback(uint64_t time);
    void snapshot_kill_all();
};

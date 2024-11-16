#include "ventus_rtlsim.h"
#include "log.h"
#include "ventus_rtlsim_impl.hpp"
#include <csignal>
#include <cstdint>
#include <memory>
#include <new>
#include <sys/wait.h>

extern "C" ventus_rtlsim_t* ventus_rtlsim_init(const ventus_rtlsim_config_t* config) {
    ventus_rtlsim_t* sim = new ventus_rtlsim_t();
    sim->constructor(config);
    return sim;
}
extern "C" void ventus_rtlsim_finish(ventus_rtlsim_t* sim, bool snapshot_rollback_forcing) {
    sim->destructor(snapshot_rollback_forcing);
    delete sim;
}
extern "C" const ventus_rtlsim_step_result_t* ventus_rtlsim_step(ventus_rtlsim_t* sim) { return sim->step(); }
extern "C" uint64_t ventus_rtlsim_get_time(const ventus_rtlsim_t* sim) { return sim->contextp->time(); }
extern "C" bool ventus_rtlsim_is_idle(const ventus_rtlsim_t* sim) { return sim->cta->is_idle(); }

extern "C" void ventus_rtlsim_add_kernel__delay_data_loading(ventus_rtlsim_t* sim,
    const ventus_kernel_metadata_t* metadata, void (*load_data_callback)(const ventus_kernel_metadata_t*),
    void (*finish_callback)(const ventus_kernel_metadata_t*)) {
    std::shared_ptr<Kernel> kernel = std::make_shared<Kernel>(metadata, load_data_callback, finish_callback);
    sim->cta->kernel_add(kernel);
}
extern "C" void ventus_rtlsim_add_kernel(ventus_rtlsim_t* sim, const ventus_kernel_metadata_t* metadata,
    void (*finish_callback)(const ventus_kernel_metadata_t*)) {
    ventus_rtlsim_add_kernel__delay_data_loading(sim, metadata, nullptr, finish_callback);
}

static paddr_t pmem_get_page_base(paddr_t paddr, uint64_t pagesize) { return paddr - paddr % pagesize; }

extern "C" bool ventus_rtlsim_pmem_page_alloc(ventus_rtlsim_t* sim, paddr_t base) { return sim->pmem_page_alloc(base); }
extern "C" bool ventus_rtlsim_pmem_page_free(ventus_rtlsim_t* sim, paddr_t base) { return sim->pmem_page_free(base); }
extern "C" bool ventus_rtlsim_pmemcpy_h2d(ventus_rtlsim_t* sim, paddr_t dst, const void* src, uint64_t size) {
    return sim->pmem_write(dst, src, size);
}
extern "C" bool ventus_rtlsim_pmemcpy_d2h(ventus_rtlsim_t* sim, void* dst, paddr_t src, uint64_t size) {
    return sim->pmem_read(src, dst, size);
}

void ventus_rtlsim_t::constructor(const ventus_rtlsim_config_t* config_) {
    // copy sim config
    config = *config_;
    config.verilator_argc = 0;
    config.verilator_argv = nullptr;

    // init Verilator simulation context
    contextp = new VerilatedContext;
    contextp->debug(0);
    contextp->randReset(0);
    contextp->traceEverOn(true);
    snapshots.is_child = false;
    snapshots.children_pid.clear();

    // load Verilator runtime arguments
    const char* verilator_runtime_args_default[] = { "+verilator+seed+10086" };
    contextp->commandArgsAdd(sizeof(verilator_runtime_args_default) / sizeof(verilator_runtime_args_default[0]),
        verilator_runtime_args_default);
    if (config_->verilator_argc > 0 && config_->verilator_argv)
        contextp->commandArgs(config_->verilator_argc, config_->verilator_argv);

    // instantiate hardware
    dut = new Vdut;
    cta = new Cta();
    pmem_map.clear();

    // waveform traces (FST)
    if (config.waveform.enable) {
        tfp = new VerilatedFstC;
        dut->trace(tfp, config.waveform.levels);
        if (config.waveform.filename == NULL) {
            log_error("waveform enabled but fst filename is NULL, set to default: obj_dir/Vdut.fst");
            config.waveform.filename = "obj_dir/Vdut.fst";
        }
        tfp->open(config.waveform.filename);
    } else {
        tfp = nullptr;
    }

    // get ready to run
    snapshot_fork(); // initial snapshot at sim_time = 0
    dut_reset();
}

const ventus_rtlsim_step_result_t* ventus_rtlsim_t::step() {
    step_status.error = contextp->gotFinish() || contextp->gotError();
    step_status.time_exceed = contextp->time() >= config.sim_time_max;
    step_status.idle = cta->is_idle();
    if (step_status.error || step_status.time_exceed) {
        return &step_status;
    }
    bool sim_got_error = false;

    //
    // clock step
    //
    contextp->timeInc(1);
    dut->clock = !dut->clock;

    //
    // Delta time before negedge(clk)
    // apply outside stimuli to DUT
    //
    if (dut->clock == 0) {
        // Thread-block dispatch to GPU (stimuli)
        cta->apply_to_dut(dut);

        // Thread-block return from GPU (stimuli)
        dut->io_host_rsp_ready = 1;

        // Assert Verilated memory IO type: must be VlWide
        static_assert(VlIsVlWide<std::decay<decltype(dut->io_mem_rd_data)>::type>::value, "Check io_mem type");
        static_assert(VlIsVlWide<std::decay<decltype(dut->io_mem_wr_data)>::type>::value, "Check io_mem type");
        static_assert(VlIsVlWide<std::decay<decltype(dut->io_mem_wr_mask)>::type>::value, "Check io_mem type");

        // Physical memory access - read
        if (dut->io_mem_rd_en) {
            uint64_t rd_addr = dut->io_mem_rd_addr;
            pmem_read(rd_addr, dut->io_mem_rd_data.data(), dut->io_mem_rd_data.Words * 4);
        }
        // Physical memory access - write
        if (dut->io_mem_wr_en) {
            uint64_t wr_addr = dut->io_mem_wr_addr;
            bool* mask = new bool[dut->io_mem_wr_mask.Words * 32];
            for (int idx = 0; idx < dut->io_mem_wr_mask.Words; idx++) {
                uint32_t mask_raw = dut->io_mem_wr_mask[idx];
                for (int bit = 0; bit < 32; bit++) {
                    mask[bit + idx * 32] = mask_raw & 0x1;
                    mask_raw >>= 1;
                }
            }
            if (!pmem_write(wr_addr, dut->io_mem_wr_data.data(), mask, dut->io_mem_wr_data.Words * 4)) {
                sim_got_error = true;
            }
            delete[] mask;
        }
    }

    //
    // Delta time before posedge(clk)
    // Check for Valid-Ready fire
    //
    if (dut->clock == 1) {
        // Thread-block dispatch to GPU (handshake OK)
        if (dut->io_host_req_valid && dut->io_host_req_ready) {
            uint32_t wg_id = dut->io_host_req_bits_host_wg_id;
            uint32_t wg_idx, kernel_id;
            std::string kernel_name;
            assert(cta->wg_get_info(kernel_name, kernel_id, wg_idx));
            cta->wg_dispatched();
            log_debug(
                "block%2d dispatched to GPU (kernel%2d %s block%2d) ", wg_id, kernel_id, kernel_name.c_str(), wg_idx);
        }
        // Thread-block return from GPU (handshake OK)
        if (dut->io_host_rsp_valid && dut->io_host_rsp_ready) {
            uint32_t wg_id = dut->io_host_rsp_bits_inflight_wg_buffer_host_wf_done_wg_id;
            cta->wg_finish(wg_id);
        }
    }

    //
    // Eval
    //
    dut->eval();
    waveform_dump();

    //
    // Clock output
    //
    if (contextp->time() % 10000 == 0)
        log_debug("");

    //
    // snapshot fork
    //
    step_status.error
        = sim_got_error || contextp->gotFinish() || contextp->gotError();
    step_status.time_exceed = contextp->time() >= config.sim_time_max;
    step_status.idle = cta->is_idle();
    if (!step_status.time_exceed && !step_status.error && contextp->time() % config.snapshot.time_interval == 0) {
        snapshot_fork();
    }

    return &step_status;
}

void ventus_rtlsim_t::destructor(bool snapshot_rollback_forcing) {
    uint64_t sim_end_time = contextp->time();
    bool need_rollback = snapshot_rollback_forcing || step_status.error || contextp->gotError() || contextp->gotFinish();

    // prints simulation result
    if (config.snapshot.enable && snapshots.is_child) { // This is the forked snapshot process
        if (need_rollback) {
            if (sim_end_time == snapshots.main_exit_time) {
                log_info("SNAPSHOT exited at time %d, OK", sim_end_time);
            } else {
                log_error("SNAPSHOT exited at time %d, which differs from the original process (time %d)", sim_end_time,
                    snapshots.main_exit_time);
            }
        } else {
            log_error("SNAPSHOT finished NORMALLY at time %d, which differs from the original process", sim_end_time);
        }
    } else { // This is the main simulation process
        if (need_rollback) {
            log_fatal("Simulation exited ABNORMALLY at time %d", sim_end_time);
        } else {
            log_info("Simulation finished in %d unit time", sim_end_time);
        }
    }

    tfp->close();
    dut->final();                  // Final model cleanup
    contextp->statsPrintSummary(); // Final simulation summary

    // invoke snapshot if needed
    if (config.snapshot.enable && !snapshots.is_child && snapshots.children_pid.size() != 0 && need_rollback) {
        snapshot_rollback(sim_end_time); // Exec snapshot
    }
    // clear snapshots
    if (config.snapshot.enable && snapshots.is_child) {
        log_info("SNAPSHOT process exit... wavefrom dumped as %s", config.snapshot.filename);
    } else {
        snapshot_kill_all(); // kill unused snapshots in the parent process
    }

    delete dut;
    delete cta;
    delete tfp;
    delete contextp; // log system use this to get time
}

bool ventus_rtlsim_t::pmem_page_alloc(paddr_t paddr) {
    if (paddr % config.pmem.pagesize != 0) {
        log_warn("PMEM address 0x%lx is not aligned to page! Align it...", paddr);
        paddr = pmem_get_page_base(paddr, config.pmem.pagesize);
    }
    if (!config.pmem.auto_alloc && pmem_map.find(paddr) != pmem_map.end()) {
        log_error("PMEM page at 0x%lx duplicate allocation", paddr);
        return false;
    }
    pmem_map[paddr] = new (std::align_val_t(4096)) uint8_t[config.pmem.pagesize];
    return true;
}

bool ventus_rtlsim_t::pmem_page_free(paddr_t paddr) {
    if (paddr % config.pmem.pagesize != 0) {
        log_warn("PMEM address 0x%lx is not aligned to page! Align it...", paddr);
        paddr = pmem_get_page_base(paddr, config.pmem.pagesize);
    }
    if (pmem_map.find(paddr) == pmem_map.end()) {
        log_error("PMEM page at 0x%lx not allocated", paddr);
        return false;
    }
    delete[] pmem_map[paddr];
    pmem_map.erase(paddr);
    return true;
}

bool ventus_rtlsim_t::pmem_write(paddr_t paddr, const void* data_, const bool mask[], uint64_t size) {
    const uint8_t* data = static_cast<const uint8_t*>(data_);
    paddr_t first_page_base = pmem_get_page_base(paddr, config.pmem.pagesize);
    paddr_t first_page_end = first_page_base + config.pmem.pagesize - 1;
    if (paddr + size - 1 > first_page_end) {
        uint64_t size_this_copy = first_page_end - paddr + 1;
        if (!pmem_write(first_page_end + 1, data + size_this_copy, mask + size_this_copy, size - size_this_copy))
            return false;
        size = size_this_copy;
    }
    if (pmem_map.find(first_page_base) == pmem_map.end()) {
        if (config.pmem.auto_alloc) {
            pmem_page_alloc(first_page_base);
        } else {
            log_fatal("PMEM page at 0x%lx not allocated, cannot write", paddr);
            return false;
        }
    }
    uint8_t* buf = pmem_map.at(first_page_base) + paddr - first_page_base;
    for (uint64_t i = 0; i < size; i++) {
        if (mask[i]) {
            buf[i] = data[i];
        }
    }
    return true;
}

bool ventus_rtlsim_t::pmem_write(paddr_t paddr, const void* data_, uint64_t size) {
    const uint8_t* data = static_cast<const uint8_t*>(data_);
    paddr_t first_page_base = pmem_get_page_base(paddr, config.pmem.pagesize);
    paddr_t first_page_end = first_page_base + config.pmem.pagesize - 1;
    if (paddr + size - 1 > first_page_end) {
        uint64_t size_this_copy = first_page_end - paddr + 1;
        if (!pmem_write(first_page_end + 1, data + size_this_copy, size - size_this_copy))
            return false;
        size = size_this_copy;
    }
    if (pmem_map.find(first_page_base) == pmem_map.end()) {
        if (config.pmem.auto_alloc) {
            pmem_page_alloc(first_page_base);
        } else {
            log_fatal("PMEM page at 0x%lx not allocated, cannot write", paddr);
            return false;
        }
    }
    uint8_t* buf = pmem_map.at(first_page_base) + paddr - first_page_base;
    std::memcpy(buf, data, size);
    return true;
}

bool ventus_rtlsim_t::pmem_read(paddr_t paddr, void* data_, uint64_t size) {
    bool success = true;
    uint8_t* data = static_cast<uint8_t*>(data_);
    paddr_t first_page_base = pmem_get_page_base(paddr, config.pmem.pagesize);
    paddr_t first_page_end = first_page_base + config.pmem.pagesize - 1;
    if (paddr + size - 1 > first_page_end) {
        uint64_t size_this_copy = first_page_end - paddr + 1;
        success = pmem_read(first_page_end + 1, data + size_this_copy, size - size_this_copy);
        size = size_this_copy;
    }
    if (pmem_map.find(first_page_base) == pmem_map.end()) {
        log_warn("PMEM page at 0x%lx not allocated, read as all zero", paddr);
        std::memset(data, 0, size);
        return false;
    }
    uint8_t* buf = pmem_map.at(first_page_base) + paddr - first_page_base;
    std::memcpy(data, buf, size);
    return success;
}

void ventus_rtlsim_t::snapshot_fork() {
    if (!config.snapshot.enable || snapshots.is_child)
        return;
    assert(dut && contextp);

    // delete oldest snapshot if needed
    if (snapshots.children_pid.size() >= config.snapshot.num_max) {
        pid_t oldest = snapshots.children_pid.back();
        kill(oldest, SIGKILL);
        waitpid(oldest, NULL, 0);
        snapshots.children_pid.pop_back();
    }
    // fork a new snapshot process
    // see https://verilator.org/guide/latest/connecting.html#process-level-clone-apis
    // see verilator/test_regress/t/t_wrapper_clone.cpp:48
    dut->prepareClone(); // prepareClone can be omitted if a little memory leak is ok
    pid_t child_pid = fork();
    dut->atClone(); // If prepareClone is omitted, call atClone() only in child process
    if (child_pid < 0) {
        log_error("SNAPSHOT: failed to fork new child process");
        return;
    }
    if (child_pid != 0) { // for the original process
        snapshots.children_pid.push_front(child_pid);
        log_info("SNAPSHOT created, pid=%d", child_pid);
    } else { // for the fork-child snapshot process
        snapshots.is_child = true;
        sigset_t set, oldset;
        siginfo_t info;
        sigemptyset(&set);
        sigaddset(&set, SNAPSHOT_WAKEUP_SIGNAL);
        sigprocmask(SIG_BLOCK, &set, &oldset);   // Block SIG for using sigwait
        sigwaitinfo(&set, &info);                // Wait for snapshot-rollback
        sigprocmask(SIG_SETMASK, &oldset, NULL); // Change signal blocking mask back
        assert(info.si_signo == SNAPSHOT_WAKEUP_SIGNAL);
        snapshots.main_exit_time = (uint64_t)(info.si_value.sival_ptr);
        log_info("SNAPSHOT is activated, sim_time = %llu, origin process exited at time %d", contextp->time(),
            snapshots.main_exit_time);
        // delete tfp;             // Cannot do this, or it will block the process
        // (maybe because Vdut.fst was already closed in the parent process?)
        tfp = new VerilatedFstC(); // This will cause memory leak for once, but not serious. How to fix it?
        dut->trace(tfp, 99);
        if (config.snapshot.filename == NULL) {
            log_error("snapshot enabled but snapshot.fst filename is NULL, set to default: obj_dir/Vdut.snapshot.fst");
            config.snapshot.filename = "obj_dir/Vdut.snapshot.fst";
        }
        tfp->open(config.snapshot.filename);
    }
}

void ventus_rtlsim_t::snapshot_rollback(uint64_t time) {
    if (!config.snapshot.enable || snapshots.is_child)
        return;
    if (snapshots.children_pid.empty()) {
        log_error("No snapshot for rolling back. Where is the initial snapshot?");
        return;
    }
    assert(dut && contextp);

    log_info("SNAPSHOT rollback to %d time-unit ago, pid=%d",
        time % config.snapshot.time_interval + (snapshots.children_pid.size() - 1) * config.snapshot.time_interval,
        snapshots.children_pid.front());
    assert(sizeof(sigval_t) >= sizeof(contextp->time()));
    sigval_t sigval;
    sigval.sival_ptr = (void*)(contextp->time());

    pid_t child = snapshots.children_pid.back();     // Choose the oldest snapshot
    sigqueue(child, SNAPSHOT_WAKEUP_SIGNAL, sigval); // Activate the snapshot
    waitpid(child, NULL, 0);                         // Wait for snapshot finished
    snapshots.children_pid.pop_back();
}

void ventus_rtlsim_t::snapshot_kill_all() {
    while (!snapshots.children_pid.empty()) {
        pid_t child = snapshots.children_pid.back();
        kill(child, SIGKILL);
        waitpid(child, NULL, 0);
        snapshots.children_pid.pop_back();
    }
    log_debug("All snapshot process are cleared, OK");
}

void ventus_rtlsim_t::waveform_dump() const {
    // snapshot child process always enables waveform dump
    bool is_snapshot = config.snapshot.enable && snapshots.is_child;
    if (!config.waveform.enable && !is_snapshot)
        return;

    assert(contextp && tfp);
    uint64_t time = contextp->time();
    if (is_snapshot || time >= config.waveform.time_begin && time < config.waveform.time_end) {
        tfp->dump(time);
    }
}

void ventus_rtlsim_t::dut_reset() const {
    assert(dut && contextp);
    contextp->time(0);
    dut->io_host_req_valid = 0;
    dut->io_host_rsp_ready = 0;
    dut->reset = 1;
    dut->clock = 0;
    dut->eval();
    waveform_dump();

    contextp->timeInc(1);
    dut->clock = 1;
    dut->eval();
    waveform_dump();

    contextp->timeInc(1);
    dut->clock = 0;
    dut->eval();
    waveform_dump();

    contextp->timeInc(1);
    dut->clock = 1;
    dut->eval();
    waveform_dump();

    contextp->timeInc(1);
    dut->clock = 0;
    dut->reset = 0;
    dut->eval();
    waveform_dump();
    log_trace("Hardware reset ok");
}

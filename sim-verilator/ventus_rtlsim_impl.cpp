#include "ventus_rtlsim_impl.hpp"
#include "Vdut.h"
#include "ventus_rtlsim.h"
#include "verilated.h"
#include <algorithm>
#include <cerrno>
#include <csignal>
#include <cstdint>
#include <cstdlib>
#include <cstring>
#include <fmt/core.h>
#include <functional>
#include <iostream>
#include <memory>
#include <optional>
#include <stdexcept>
#include <spdlog/common.h>
#include <spdlog/formatter.h>
#include <spdlog/sinks/basic_file_sink.h>
#include <spdlog/sinks/stdout_color_sinks.h>
#include <string>
#include <sys/prctl.h>
#include <sys/wait.h>
#include <utility>

#ifdef ENABLE_GVM
#include "gvm.hpp"
#endif
#include "pmu_snapshot_copy.inc"

constexpr uint64_t HALF_CYCLE_TIME = 5;

//
// cleanup at exit
//
static std::vector<ventus_rtlsim_t*> g_instances;

static uint32_t rtl_parameter_u32(const char* name) {
    auto it = rtl_parameters.find(name);
    if (it == rtl_parameters.end() || it->second < 0) {
        throw std::runtime_error(fmt::format("RTL parameter {} is missing or invalid", name));
    }
    return static_cast<uint32_t>(it->second);
}

// cleanup: mainly for Verilator FST waveform dump
// tfp->close() is necessary to save complete waveform to file
//  or the fst file may be corrupted, or lose some data at the end
//  in this case, a .fst.hier file appears. 
static void cleanup() {
    for (auto* sim : g_instances) {
#if VM_TRACE
        if (sim->tfp)
            sim->tfp->close(); // save waveform to file
#endif
        // No need to delete tfp, the process is exiting
        // delete sim->tfp; // This will cause segfault sometimes, why?
        sim->tfp = nullptr;
    }
    g_instances.clear();
}

// register cleanup function after g_instances is constructed
// so that cleanup() is called before g_instances is destructed
struct CleanupRegister {
    CleanupRegister() { std::atexit(cleanup); }
} _cleanup_register;

//
// cleanup at interrupt or abort
//

static volatile std::sig_atomic_t g_interrupt = false;
static volatile std::sig_atomic_t g_aborted = false;
static std::optional<struct sigaction> g_sigabort_old = std::nullopt;
void signal_interrupt_handler(int signum) { g_interrupt = true; }
void signal_abort_handler(int signum) { g_aborted = true; }

//
// Helpers
//

// convert log level string to spdlog level enum
static spdlog::level::level_enum get_log_level(const char* level) {
    if (level == nullptr) {
        // set to default level later
    } else if (strcmp(level, "trace") == 0) {
        return spdlog::level::trace;
    } else if (strcmp(level, "debug") == 0) {
        return spdlog::level::debug;
    } else if (strcmp(level, "info") == 0) {
        return spdlog::level::info;
    } else if (strcmp(level, "warn") == 0) {
        return spdlog::level::warn;
    } else if (strcmp(level, "error") == 0) {
        return spdlog::level::err;
    } else if (strcmp(level, "critical") == 0) {
        return spdlog::level::critical;
    }
    std::cerr << "Log level unrecognized: \"" << level << "\", set to default: \"trace\"" << std::endl;
    return spdlog::level::trace;
}

// log formatter
class Formatter_ventus_rtlsim : public spdlog::formatter {
public:
    Formatter_ventus_rtlsim(std::function<std::string()> callback)
        : m_callback(callback) {};

    void format(const spdlog::details::log_msg& msg, spdlog::memory_buf_t& dst) override {
        std::string basic_info = fmt::format("[RTL {0:>8}]", spdlog::level::to_string_view(msg.level));
        std::string cb_info = m_callback ? m_callback() : "";
        std::string newline = "\n";
        dst.append(basic_info.data(), basic_info.data() + basic_info.size());
        dst.append(cb_info.data(), cb_info.data() + cb_info.size());
        dst.append(msg.payload.begin(), msg.payload.end());
        dst.append(newline.data(), newline.data() + newline.size());
    }

    std::unique_ptr<spdlog::formatter> clone() const override {
        return std::make_unique<Formatter_ventus_rtlsim>(m_callback);
    }

private:
    std::function<std::string()> m_callback;
};

//
// RTLSIM implementation
//

void ventus_rtlsim_t::constructor(const ventus_rtlsim_config_t* config_, bool initialize_dut) {
    // copy and check sim config
    config = *config_;
    if (config.log.file.enable && config.log.file.filename == nullptr) {
        std::cerr << "Log file name not given, set to default: logs/ventus_rtlsim.log" << std::endl;
        config.log.file.filename = "logs/ventus_rtlsim.log";
    }
    if (config.waveform.enable && config.waveform.filename == NULL) {
        std::cerr << "waveform enabled but fst filename is NULL, set to default: logs/ventus_rtlsim.fst" << std::endl;
        config.waveform.filename = "logs/ventus_rtlsim.fst";
    }
    if (config.snapshot.enable && config.snapshot.filename == NULL) {
        std::cerr << "waveform enabled but fst filename is NULL, set to default: logs/ventus_rtlsim.snapshot.fst"
                  << std::endl;
        config.snapshot.filename = "logs/ventus_rtlsim.snapshot.fst";
    }
    config.verilator.argc = 0;
    config.verilator.argv = nullptr;

    // init logger
    try {
        std::vector<spdlog::sink_ptr> sinks;
        if (config.log.file.enable) {
            auto file_sink = std::make_shared<spdlog::sinks::basic_file_sink_mt>(config.log.file.filename);
            file_sink->set_level(get_log_level(config.log.file.level));
            sinks.push_back(file_sink);
        }
        if (config.log.console.enable) {
            auto console_sink = std::make_shared<spdlog::sinks::stdout_color_sink_mt>();
            console_sink->set_level(get_log_level(config.log.console.level));
            sinks.push_back(console_sink);
        }
        logger = std::make_shared<spdlog::logger>("VentusRTLsim_logger", sinks.begin(), sinks.end());
#ifdef ENABLE_GVM
        gvm.logger = logger;
#endif // ENABLE_GVM
        logger->set_level(get_log_level(config.log.level));
        logger->flush_on(spdlog::level::err);

        // set logger formatter
        auto func_log_prefix
            = [&contextp = std::as_const(contextp)]() -> std::string { return fmt::format("@{} ", contextp->time()); };
        auto formatter = std::make_unique<Formatter_ventus_rtlsim>(func_log_prefix);
        logger->set_formatter(std::move(formatter));

        // set logger error handler
        auto func_log_error_handler = [](const std::string& msg) {
            std::cerr << "VentusRTLsim_logger error: " << msg << std::endl;
            std::abort();
        };
        logger->set_error_handler(func_log_error_handler);
    } catch (const spdlog::spdlog_ex& ex) {
        std::cerr << "Log initialization failed: " << ex.what() << std::endl;
        exit(1);
    }

    // init Verilator simulation context
    contextp = new VerilatedContext;
    contextp->threads(1);
    contextp->debug(0);
    contextp->randReset(0);
    contextp->traceEverOn(VM_TRACE);
    snapshots.is_child = false;
    snapshots.children.clear();

    // load Verilator runtime arguments
    const char* verilator_runtime_args_default[] = { "+verilator+seed+10086" };
    contextp->commandArgsAdd(
        sizeof(verilator_runtime_args_default) / sizeof(verilator_runtime_args_default[0]),
        verilator_runtime_args_default
    );
    if (config_->verilator.argc > 0 && config_->verilator.argv)
        contextp->commandArgs(config_->verilator.argc, config_->verilator.argv);

    // instantiate hardware
    dut = new Vdut(contextp);
    cta = new Cta(logger);
    pmem = std::make_unique<PhysicalMemory>(config.pmem.auto_alloc, config.pmem.pagesize, logger);
    need_icache_invalidate = false;
    pmu_num_sm = rtl_parameter_u32("num_sm");
    if (pmu_num_sm != GENERATED_PMU_NUM_SM) {
        logger->critical(
            "PMU snapshot code was generated for {} SM counters, but RTL parameter num_sm={}. "
            "Regenerate this backend from the matching RTL parameters.",
            GENERATED_PMU_NUM_SM, pmu_num_sm
        );
        std::abort();
    }
    pmu_snapshot.num_sm = pmu_num_sm;
    pmu_snapshot.has_dcache = true;
    pmu_snapshot.pipeline.resize(pmu_num_sm);
    pmu_snapshot.inst_class.resize(pmu_num_sm);
    pmu_snapshot.dcache.resize(pmu_num_sm);

    // waveform traces (FST)
#if VM_TRACE
    if (config.waveform.enable) {
        tfp = new VerilatedFstC;
        dut->trace(tfp, config.waveform.levels);
        tfp->open(config.waveform.filename);
        // sig abort
        struct sigaction sa;
        sa.sa_handler = signal_abort_handler;
        sa.sa_flags = 0;
        sigemptyset(&sa.sa_mask);
        struct sigaction sa_old;
        sigaction(SIGABRT, &sa, &sa_old);
        g_sigabort_old = sa_old;
        // sig interrupt
        sa.sa_handler = signal_interrupt_handler;
        sigaction(SIGINT, &sa, nullptr);
    } else {
        tfp = nullptr;
    }
#else
    tfp = nullptr;
#endif

    // push into global instances, prepare cleanup at exit
    g_instances.push_back(this);

    // get ready to run
    last_pmu_progress_time = contextp->time();
    last_pmu_progress_value = 0;
    if (initialize_dut) {
        snapshot_fork(); // initial snapshot at sim_time = 0
        dut_reset();
    } else {
        step_status = {};
        step_status.idle = true;
    }
}

const ventus_rtlsim_step_result_t* ventus_rtlsim_t::step() {
    step_status.error = contextp->gotFinish() || contextp->gotError();
    step_status.time_exceed = contextp->time() >= config.sim_time_max;
    step_status.idle = cta->is_idle();
    step_status.hang = false;
    if (step_status.error || step_status.time_exceed) {
        return &step_status;
    }
    bool sim_got_error = false;

    //
    // clock step
    //
    contextp->timeInc(HALF_CYCLE_TIME);
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
        dut->io_perfDump = cta->is_idle();
        dut->io_perfDumpSummary = need_perf_dump_summary;

        // Assert Verilated memory IO type: must be VlWide
        static_assert(VlIsVlWide<std::decay<decltype(dut->io_mem_rd_data)>::type>::value, "Check io_mem type");
        static_assert(VlIsVlWide<std::decay<decltype(dut->io_mem_wr_data)>::type>::value, "Check io_mem type");
        static_assert(VlIsVlWide<std::decay<decltype(dut->io_mem_wr_mask)>::type>::value, "Check io_mem type");

        // Physical memory access - read
        if (dut->io_mem_rd_en) {
            uint64_t rd_addr = dut->io_mem_rd_addr;
            pmem->read(rd_addr, dut->io_mem_rd_data.data(), dut->io_mem_rd_data.Words * 4);
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
            if (!pmem->write(wr_addr, dut->io_mem_wr_data.data(), mask, dut->io_mem_wr_data.Words * 4)) {
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
            logger->debug(fmt::format(
                "block{0:<2} dispatched to GPU (kernel{1:<2} {2} block{3:<2})", wg_id, kernel_id, kernel_name, wg_idx
            ));
        }
        // Thread-block return from GPU (handshake OK)
        if (dut->io_host_rsp_valid && dut->io_host_rsp_ready) {
            uint32_t wg_id = dut->io_host_rsp_bits_inflight_wg_buffer_host_wf_done_wg_id;
            cta->wg_finish(wg_id);
        }
        dut->io_icache_invalidate = need_icache_invalidate;
        need_icache_invalidate = false;
    }

    //
    // Eval
    //
    dut->eval();
    waveform_dump();
    if (dut->clock == 1 && need_perf_dump_summary) {
        need_perf_dump_summary = false;
    }

    //
    // Abort?
    //
    if (g_interrupt) {
        destructor(false);
        std::exit(130);
    }
    if (g_aborted) {
        destructor(false);
        if (g_sigabort_old.has_value()) {
            sigaction(SIGABRT, &(g_sigabort_old.value()), nullptr);
            g_sigabort_old = std::nullopt;
            raise(SIGABRT); // 让进程按默认方式终止，允许Coredump
        } else {
            std::exit(EXIT_FAILURE);
        }
    }

    //
    // Clock output
    //
    if (contextp->time() % 10000 == 0) {
        logger->debug("");
    }

#ifdef ENABLE_GVM
    if (contextp->time() % 2 == 1) {
        gvm.getDut();
        if (gvm.gvmStep() != 0) {
            sim_got_error = true;
            logger->critical("GVM reported fatal mismatch, stopping simulation");
        }
    }
#endif // ENABLE_GVM

    //
    // snapshot fork
    //
    step_status.error = sim_got_error || contextp->gotFinish() || contextp->gotError();
    step_status.time_exceed = contextp->time() >= config.sim_time_max;
    step_status.idle = cta->is_idle();
    sample_pmu_snapshot();
    update_pmu_watchdog();
    if (config.snapshot.enable && !step_status.time_exceed && !step_status.error
        && contextp->time() % config.snapshot.time_interval == 0) {
        snapshot_fork();
    }

    return &step_status;
}

void ventus_rtlsim_t::sample_pmu_snapshot() {
    copy_pmu_counters_from_dut();
}

ventus_rtlsim_pmu_t ventus_rtlsim_t::pmu_view() const {
    return {
        pmu_snapshot.num_sm,
        pmu_snapshot.has_dcache,
        pmu_snapshot.pipeline.data(),
        pmu_snapshot.inst_class.data(),
        pmu_snapshot.has_dcache ? pmu_snapshot.dcache.data() : nullptr,
    };
}

void ventus_rtlsim_t::update_pmu_watchdog() {
    if (config.hang_timeout == 0 || step_status.error || step_status.time_exceed || step_status.idle) {
        last_pmu_progress_time = contextp->time();
        last_pmu_progress_value = 0;
        return;
    }

    uint64_t progress = 0;
    uint64_t issued = 0;
    uint64_t mem_issued = 0;
    uint64_t dcache_req = 0;
    for (uint32_t i = 0; i < pmu_snapshot.num_sm; i++) {
        issued += pmu_snapshot.pipeline[i].total_scalar_issued + pmu_snapshot.pipeline[i].total_vector_issued;
        mem_issued += pmu_snapshot.inst_class[i].mem_issued;
        dcache_req += pmu_snapshot.dcache[i].total_req;
    }
    progress = issued + mem_issued + dcache_req;

    if (progress != last_pmu_progress_value) {
        last_pmu_progress_value = progress;
        last_pmu_progress_time = contextp->time();
        return;
    }

    const uint64_t idle_time = contextp->time() - last_pmu_progress_time;
    if (idle_time < config.hang_timeout) {
        return;
    }

    step_status.hang = true;
    step_status.error = true;
    logger->critical(
        "PMU watchdog detected HANG: no issue/memory progress for {} time units, time={}, issued={}, mem_issued={}, "
        "l1d_req={}",
        idle_time, contextp->time(), issued, mem_issued, dcache_req
    );
}

int ventus_rtlsim_t::destructor(bool snapshot_rollback_forcing) {
    uint64_t sim_end_time = contextp->time();
    bool need_rollback
        = snapshot_rollback_forcing || step_status.error || contextp->gotError() || contextp->gotFinish();
    int result = 0;

    // prints simulation result
    if (config.snapshot.enable && snapshots.is_child) { // This is the forked snapshot process
        if (need_rollback) {
            if (sim_end_time == snapshots.main_exit_time) {
                logger->info("SNAPSHOT exited at time {}, OK", sim_end_time);
            } else {
                result = -1;
                logger->error(
                    "SNAPSHOT exited at time {}, which differs from the original process (time {})", sim_end_time,
                    snapshots.main_exit_time
                );
            }
        } else {
            result = -1;
            logger->error(
                "SNAPSHOT finished NORMALLY at time {}, which differs from the original process", sim_end_time
            );
        }
    } else { // This is the main simulation process
        if (need_rollback) {
            logger->critical("Simulation exited ABNORMALLY at time {}", sim_end_time);
        } else {
            logger->info("Simulation finished in {} unit time", sim_end_time);
        }
    }

#if VM_TRACE
    if (tfp)
        tfp->close();
#endif
    dut->final();                  // Final model cleanup
    contextp->statsPrintSummary(); // Final simulation summary

    // invoke snapshot if needed
    if (config.snapshot.enable && !snapshots.is_child && !snapshots.children.empty() && need_rollback) {
        result = snapshot_rollback(sim_end_time); // Exec snapshot
    } else if (config.snapshot.enable && !snapshots.is_child && need_rollback) {
        logger->error("SNAPSHOT rollback requested, but no snapshot is available");
        result = -1;
    } else if (!config.snapshot.enable && need_rollback) {
        result = -1;
    }
    // clear snapshots
    if (config.snapshot.enable && snapshots.is_child) {
        logger->info("SNAPSHOT process exit... wavefrom dumped as {}", config.snapshot.filename);
    } else {
        snapshot_kill_all(); // kill unused snapshots in the parent process
    }

    delete dut;
    delete cta;
    if (tfp)
        delete tfp;
    dut = nullptr;
    cta = nullptr;
    tfp = nullptr;
    delete contextp; // log system use this to get time
    contextp = nullptr;
    g_instances.erase(std::remove(g_instances.begin(), g_instances.end(), this), g_instances.end());
    return result;
}

void ventus_rtlsim_t::dump_testcase_pmu_summary() {
    if (!dut || !contextp) {
        return;
    }
    need_perf_dump_summary = true;
    for (int i = 0; i < 4 && need_perf_dump_summary; i++) {
        step();
    }
    dut->io_perfDumpSummary = 0;
}

void ventus_rtlsim_t::snapshot_fork() {
    if (!config.snapshot.enable || snapshots.is_child)
        return;
    assert(dut && contextp);

    // delete oldest snapshot if needed
    if (snapshots.children.size() >= static_cast<size_t>(config.snapshot.num_max)) {
        const snapshot_record_t oldest = snapshots.children.back();
        kill(oldest.pid, SIGKILL);
        waitpid(oldest.pid, NULL, 0);
        snapshots.children.pop_back();
    }
    // fork a new snapshot process
    // see https://verilator.org/guide/latest/connecting.html#process-level-clone-apis
    // see verilator/test_regress/t/t_wrapper_clone.cpp:48
    dut->prepareClone(); // prepareClone can be omitted if a little memory leak is ok
    pid_t child_pid = fork();
    dut->atClone(); // If prepareClone is omitted, call atClone() only in child process
    if (child_pid < 0) {
        logger->error("SNAPSHOT: failed to fork new child process");
        return;
    }
    if (child_pid != 0) { // for the original process
        snapshots.children.push_front({child_pid, contextp->time()});
        logger->info("SNAPSHOT created at time {}, pid={}", contextp->time(), child_pid);
    } else { // for the fork-child snapshot process
        snapshots.is_child = true;
        // child process should exit when parent process exits
        if (prctl(PR_SET_PDEATHSIG, SIGKILL) == -1) {
            perror("prctl(PR_SET_PDEATHSIG)");
            std::exit(EXIT_FAILURE);
        }
        if (getppid() == 1) { // parent process already exited
            std::exit(EXIT_FAILURE);
        }
        // wait for main process
        sigset_t set, oldset;
        siginfo_t info;
        sigemptyset(&set);
        sigaddset(&set, SNAPSHOT_WAKEUP_SIGNAL);
        sigprocmask(SIG_BLOCK, &set, &oldset);   // Block SIG for using sigwait
        sigwaitinfo(&set, &info);                // Wait for snapshot-rollback
        sigprocmask(SIG_SETMASK, &oldset, NULL); // Change signal blocking mask back
        assert(info.si_signo == SNAPSHOT_WAKEUP_SIGNAL);
        // main process invoked snapshot rollback
        snapshots.main_exit_time = (uint64_t)(info.si_value.sival_ptr);
        logger->info(
            "SNAPSHOT is activated, sim_time = {}, origin process exited at time {}", contextp->time(),
            snapshots.main_exit_time
        );
#if VM_TRACE
        // create a new waveform dump file
        //  delete tfp;             // Cannot do this, or it will block the process
        //  (maybe because Vdut.fst was already closed in the parent process?)
        tfp = new VerilatedFstC(); // This will cause memory leak for once, but not serious. How to fix it?
        dut->trace(tfp, 99);
        if (config.snapshot.filename == NULL) {
            logger->error(
                "snapshot enabled but snapshot.fst filename is NULL, set to default: logs/ventus_rtlsim.snapshot.fst"
            );
            config.snapshot.filename = "logs/ventus_rtlsim.snapshot.fst";
        }
        tfp->open(config.snapshot.filename);
#else
        logger->critical("snapshot replay requires a TRACE=1 library");
        std::exit(EXIT_FAILURE);
#endif
    }
}

int ventus_rtlsim_t::snapshot_rollback(uint64_t time) {
    if (!config.snapshot.enable || snapshots.is_child)
        return -1;
    if (snapshots.children.empty()) {
        logger->error("No snapshot for rolling back. Where is the initial snapshot?");
        return -1;
    }
    assert(dut && contextp);

    const snapshot_record_t snapshot = snapshots.children.back();
    logger->info(
        "SNAPSHOT rollback from time {} to time {} ({} time-units), pid={}", time, snapshot.time,
        time >= snapshot.time ? time - snapshot.time : 0, snapshot.pid
    );
    assert(sizeof(sigval_t) >= sizeof(contextp->time()));
    sigval_t sigval;
    sigval.sival_ptr = (void*)(contextp->time());

    if (sigqueue(snapshot.pid, SNAPSHOT_WAKEUP_SIGNAL, sigval) != 0) {
        logger->error("SNAPSHOT failed to wake pid={}: {}", snapshot.pid, std::strerror(errno));
        snapshots.children.pop_back();
        return -1;
    }

    int status = 0;
    pid_t waited;
    do {
        waited = waitpid(snapshot.pid, &status, 0);
    } while (waited < 0 && errno == EINTR);
    snapshots.children.pop_back();
    if (waited != snapshot.pid) {
        logger->error("SNAPSHOT waitpid failed for pid={}: {}", snapshot.pid, std::strerror(errno));
        return -1;
    }
    if (!WIFEXITED(status) || WEXITSTATUS(status) != EXIT_SUCCESS) {
        if (WIFSIGNALED(status)) {
            logger->error("SNAPSHOT replay pid={} terminated by signal {}", snapshot.pid, WTERMSIG(status));
        } else if (WIFEXITED(status)) {
            logger->error("SNAPSHOT replay pid={} exited with status {}", snapshot.pid, WEXITSTATUS(status));
        } else {
            logger->error("SNAPSHOT replay pid={} ended with wait status 0x{:x}", snapshot.pid, status);
        }
        return -1;
    }
    logger->info("SNAPSHOT replay pid={} completed successfully", snapshot.pid);
    return 0;
}

void ventus_rtlsim_t::snapshot_kill_all() {
    while (!snapshots.children.empty()) {
        const snapshot_record_t snapshot = snapshots.children.back();
        kill(snapshot.pid, SIGKILL);
        waitpid(snapshot.pid, NULL, 0);
        snapshots.children.pop_back();
    }
    logger->debug("All snapshot process are cleared, OK");
}

void ventus_rtlsim_t::waveform_dump() const {
#if VM_TRACE
    // snapshot child process always enables waveform dump
    bool is_snapshot = config.snapshot.enable && snapshots.is_child;
    if (!config.waveform.enable && !is_snapshot)
        return;

    assert(contextp && tfp);
    uint64_t time = contextp->time();
    if (is_snapshot || time >= config.waveform.time_begin && time < config.waveform.time_end) {
        tfp->dump(time);
    }
#endif
}

void ventus_rtlsim_t::dut_reset() const {
    assert(dut && contextp);
    contextp->time(0);
    dut->io_host_req_valid = 0;
    dut->io_host_rsp_ready = 0;
    dut->io_perfDump = 0;
    dut->io_perfDumpSummary = 0;
    dut->reset = 1;
    dut->clock = 0;
    dut->eval();
    waveform_dump();

    contextp->timeInc(HALF_CYCLE_TIME);
    dut->clock = 1;
    dut->eval();
    waveform_dump();

    contextp->timeInc(HALF_CYCLE_TIME);
    dut->clock = 0;
    dut->eval();
    waveform_dump();

    contextp->timeInc(HALF_CYCLE_TIME);
    dut->clock = 1;
    dut->eval();
    waveform_dump();

    contextp->timeInc(HALF_CYCLE_TIME);
    dut->clock = 0;
    dut->reset = 0;
    dut->eval();
    waveform_dump();
#ifdef ENABLE_GVM
    gvm_clear_global_trace_buffers();
#endif // ENABLE_GVM
    logger->trace("Hardware reset ok");
}

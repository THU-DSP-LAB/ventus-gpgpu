#include "ventus_rtlsim_impl.hpp"
#include "ventus_rtlsim.h"
#include <csignal>
#include <cstdint>
#include <fmt/core.h>
#include <functional>
#include <iostream>
#include <memory>
#include <new>
#include <spdlog/common.h>
#include <spdlog/formatter.h>
#include <spdlog/sinks/basic_file_sink.h>
#include <spdlog/sinks/stdout_color_sinks.h>
#include <string>
#include <sys/wait.h>
#include <utility>

// helper
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

void ventus_rtlsim_t::constructor(const ventus_rtlsim_config_t* config_) {
    // copy and check sim config
    config = *config_;
    if (config.log.file.enable && config.log.file.filename == nullptr) {
        std::cerr << "Log file name out given, set to default: logs/ventus_rtlsim.log" << std::endl;
        config.log.file.filename = "logs/ventus_rtlsim.log";
    }
    if (config.waveform.enable && config.waveform.filename == NULL) {
        std::cerr << "waveform enabled but fst filename is NULL, set to default: obj_dir/Vdut.fst" << std::endl;
        config.waveform.filename = "logs/ventus_rtlsim.fst";
    }
    if (config.snapshot.enable && config.snapshot.filename == NULL) {
        std::cerr << "waveform enabled but fst filename is NULL, set to default: obj_dir/Vdut.fst" << std::endl;
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
    contextp->debug(0);
    contextp->randReset(0);
    contextp->traceEverOn(true);
    snapshots.is_child = false;
    snapshots.children_pid.clear();

    // load Verilator runtime arguments
    const char* verilator_runtime_args_default[] = { "+verilator+seed+10086" };
    contextp->commandArgsAdd(
        sizeof(verilator_runtime_args_default) / sizeof(verilator_runtime_args_default[0]),
        verilator_runtime_args_default
    );
    if (config_->verilator.argc > 0 && config_->verilator.argv)
        contextp->commandArgs(config_->verilator.argc, config_->verilator.argv);

    // instantiate hardware
    dut = new Vdut;
    cta = new Cta(logger);
    pmem_map.clear();

    // waveform traces (FST)
    if (config.waveform.enable) {
        tfp = new VerilatedFstC;
        dut->trace(tfp, config.waveform.levels);
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
            logger->debug(fmt::format(
                "block{0:<2} dispatched to GPU (kernel{1:<2} {2} block{3:<2})", wg_id, kernel_id, kernel_name, wg_idx
            ));
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
    if (contextp->time() % 10000 == 0) {
        logger->debug("");
    }

    //
    // snapshot fork
    //
    step_status.error = sim_got_error || contextp->gotFinish() || contextp->gotError();
    step_status.time_exceed = contextp->time() >= config.sim_time_max;
    step_status.idle = cta->is_idle();
    if (!step_status.time_exceed && !step_status.error && contextp->time() % config.snapshot.time_interval == 0) {
        snapshot_fork();
    }

    return &step_status;
}

void ventus_rtlsim_t::destructor(bool snapshot_rollback_forcing) {
    uint64_t sim_end_time = contextp->time();
    bool need_rollback
        = snapshot_rollback_forcing || step_status.error || contextp->gotError() || contextp->gotFinish();

    // prints simulation result
    if (config.snapshot.enable && snapshots.is_child) { // This is the forked snapshot process
        if (need_rollback) {
            if (sim_end_time == snapshots.main_exit_time) {
                logger->info("SNAPSHOT exited at time {}, OK", sim_end_time);
            } else {
                logger->error(
                    "SNAPSHOT exited at time {}, which differs from the original process (time {})", sim_end_time,
                    snapshots.main_exit_time
                );
            }
        } else {
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

    tfp->close();
    dut->final();                  // Final model cleanup
    contextp->statsPrintSummary(); // Final simulation summary

    // invoke snapshot if needed
    if (config.snapshot.enable && !snapshots.is_child && snapshots.children_pid.size() != 0 && need_rollback) {
        snapshot_rollback(sim_end_time); // Exec snapshot
    }
    // clear snapshots
    if (config.snapshot.enable && snapshots.is_child) {
        logger->info("SNAPSHOT process exit... wavefrom dumped as {}", config.snapshot.filename);
    } else {
        snapshot_kill_all(); // kill unused snapshots in the parent process
    }

    // release pmem
    for (auto& it : pmem_map) {
        delete[] it.second;
    }

    delete dut;
    delete cta;
    delete tfp;
    delete contextp; // log system use this to get time
}

bool ventus_rtlsim_t::pmem_page_alloc(paddr_t paddr) {
    if (paddr % config.pmem.pagesize != 0) {
        logger->warn("PMEM address 0x{:x} is not aligned to page! Align it...", paddr);
        paddr = pmem_get_page_base(paddr, config.pmem.pagesize);
    }
    if (!config.pmem.auto_alloc && pmem_map.find(paddr) != pmem_map.end()) {
        logger->error("PMEM page at 0x{:x} duplicate allocation", paddr);
        return false;
    }
    pmem_map[paddr] = new (std::align_val_t(4096)) uint8_t[config.pmem.pagesize];
    return true;
}

bool ventus_rtlsim_t::pmem_page_free(paddr_t paddr) {
    if (paddr % config.pmem.pagesize != 0) {
        logger->warn("PMEM address 0x{:x} is not aligned to page! Align it...", paddr);
        paddr = pmem_get_page_base(paddr, config.pmem.pagesize);
    }
    if (pmem_map.find(paddr) == pmem_map.end()) {
        logger->error("PMEM page at 0x{:x} not allocated", paddr);
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
            logger->critical("PMEM page at 0x{:x} not allocated, cannot write", paddr);
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
            logger->critical("PMEM page at 0x{:x} not allocated, cannot write", paddr);
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
        logger->warn("PMEM page at 0x{:x} not allocated, read as all zero", paddr);
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
        logger->error("SNAPSHOT: failed to fork new child process");
        return;
    }
    if (child_pid != 0) { // for the original process
        snapshots.children_pid.push_front(child_pid);
        logger->info("SNAPSHOT created, pid={}", child_pid);
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
        logger->info(
            "SNAPSHOT is activated, sim_time = {}, origin process exited at time {}", contextp->time(),
            snapshots.main_exit_time
        );
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
    }
}

void ventus_rtlsim_t::snapshot_rollback(uint64_t time) {
    if (!config.snapshot.enable || snapshots.is_child)
        return;
    if (snapshots.children_pid.empty()) {
        logger->error("No snapshot for rolling back. Where is the initial snapshot?");
        return;
    }
    assert(dut && contextp);

    logger->info(
        "SNAPSHOT rollback to {} time-unit ago, pid={}",
        time % config.snapshot.time_interval + (snapshots.children_pid.size() - 1) * config.snapshot.time_interval,
        snapshots.children_pid.front()
    );
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
    logger->debug("All snapshot process are cleared, OK");
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
    logger->trace("Hardware reset ok");
}

#include "ventus_rtlsim_impl.hpp"
#include "Vdut.h"
#include "ventus_rtlsim.h"
#include "verilated.h"
#include <algorithm>
#include <bitset>
#include <csignal>
#include <cstdint>
#include <cstdlib>
#include <fmt/core.h>
#include <functional>
#include <iostream>
#include <memory>
#include <optional>
#include <queue>
#include <spdlog/common.h>
#include <spdlog/formatter.h>
#include <spdlog/sinks/basic_file_sink.h>
#include <spdlog/sinks/stdout_color_sinks.h>
#include <spdlog/spdlog.h>
#include <stdexcept>
#include <string>
#include <sys/prctl.h>
#include <sys/wait.h>
#include <utility>
#include <vector>

#ifdef ENABLE_GVM
#include "gvm.hpp"
#endif // ENABLE_GVM

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
        if (sim->tfp)
            sim->tfp->close(); // save waveform to file
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

constexpr unsigned log2Ceil(unsigned n) {
    unsigned log = 0;
    n--;
    while (n > 0) {
        log++;
        n >>= 1;
    }
    return log;
}

// 核心流水线与L1D之间的接口 opcode & param
// NOTE: 即使 nocache 版本物理上没有 L1D 硬件，SM↔mem 的接口仍是 cache-line 风格
//       (tag/setIdx/blockOffset)，host 在 get_dcache_req 之后必须按 RTL 实际参数反推
//       byte 地址。下面三个常量必须与 rtl_parameters.cpp 中 dcache_NSets / dcache_NWays /
//       dcache_BlockWords 一致；早期版本误把 NUM_SET 写成 256（RTL 实际为 32），
//       会让 (tag<<8)<<5<<2 在 uint32_t 内溢出，命中错误的物理地址（如目标 0x90004000
//       会被算成 0x80020000），表现为读到全 0 → 跳转到 PC=0 → UNDEFINED INSTRUCTION。
inline constexpr unsigned L1D_NUM_SET = 32;        // L1 D-cache的组数（= rtl_parameters.dcache_NSets）
inline constexpr unsigned L1D_NUM_WAY = 2;         // L1 D-cache的组相联度
inline constexpr unsigned L1D_BLOCK_NUM_WORD = 32; // 每个cache block包含多少个32bit
inline constexpr uint8_t L1D_OPCODE_READ = 0x0;
inline constexpr uint8_t L1D_OPCODE_WRITE = 0x1;
inline constexpr uint8_t L1D_OPCODE_ATOMIC = 0x2;
inline constexpr uint8_t L1D_OPCODE_CACHEOP = 0x3;
inline constexpr uint8_t L1D_PARAM_NORMAL = 0x0;     // 常规读写
inline constexpr uint8_t L1D_PARAM_PREFETCH = 0x1;   // 预留性读出
inline constexpr uint8_t L1D_PARAM_CONDWRITE = 0x1;  // 条件性写入
inline constexpr uint8_t L1D_PARAM_NONCACHE = 0x2;   // 不缓存读出/写入
inline constexpr uint8_t L1D_PARAM_INVALIDATE = 0x0; // 全局无效化
inline constexpr uint8_t L1D_PARAM_FLUSH = 0x1;      // 全局冲刷
inline constexpr uint8_t L1D_PARAM_FENCE = 0x2;      // 等待MSHR清空
inline constexpr uint8_t L1D_PARAM_ATOMIC_SWAP = 16;
inline constexpr uint8_t L1D_PARAM_ATOMIC_ADD = 0;
inline constexpr uint8_t L1D_PARAM_ATOMIC_XOR = 1;
inline constexpr uint8_t L1D_PARAM_ATOMIC_OR = 2;
inline constexpr uint8_t L1D_PARAM_ATOMIC_AND = 3;
inline constexpr uint8_t L1D_PARAM_ATOMIC_MIN = 4;
inline constexpr uint8_t L1D_PARAM_ATOMIC_MAX = 5;
inline constexpr uint8_t L1D_PARAM_ATOMIC_MINU = 6;
inline constexpr uint8_t L1D_PARAM_ATOMIC_MAXU = 7;

void dcache_rsp_sm0(Vdut* dut, const std::unique_ptr<dcache_reqrsp_t>& rsp);
void dcache_rsp_sm1(Vdut* dut, const std::unique_ptr<dcache_reqrsp_t>& rsp);
std::vector<std::unique_ptr<dcache_reqrsp_t>> get_dcache_req(Vdut* dut);
void icache_rsp_sm0(Vdut* dut, const std::unique_ptr<icache_reqrsp_t>& rsp);
void icache_rsp_sm1(Vdut* dut, const std::unique_ptr<icache_reqrsp_t>& rsp);
std::vector<std::unique_ptr<icache_reqrsp_t>> get_icache_req(Vdut* dut);

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

void ventus_rtlsim_t::constructor(const ventus_rtlsim_config_t* config_) {
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

    // init Verilator simulation context
    contextp = new VerilatedContext;
    contextp->debug(0);
    contextp->randReset(0);
    contextp->traceEverOn(true);

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

    SPDLOG_LOGGER_INFO(logger, "Ventus RTL simulation initialization started");
    SPDLOG_LOGGER_INFO(logger, "Note: you are running no-cache version RTL");

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
    dut = new Vdut();
    cta = new Cta(logger);
    pmem = std::make_unique<PhysicalMemory>(config.pmem.auto_alloc, config.pmem.pagesize, logger);
    need_icache_invalidate = false;
    pmu_num_sm = rtl_parameter_u32("num_sm");
    if (pmu_num_sm != NUM_SM) {
        logger->critical(
            "PMU snapshot code currently samples {} nocache SM counters, but RTL parameter num_sm={}. "
            "Update nocache PMU sampling for the matching RTL parameters.",
            NUM_SM, pmu_num_sm
        );
        std::abort();
    }
    pmu_snapshot.num_sm = pmu_num_sm;
    pmu_snapshot.has_dcache = false;
    pmu_snapshot.pipeline.resize(pmu_num_sm);
    pmu_snapshot.inst_class.resize(pmu_num_sm);

    // waveform traces (FST)
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
    g_instances.push_back(this); // for cleanup at exit

    // get ready to run
    snapshot_fork(); // initial snapshot at sim_time = 0
    last_pmu_progress_time = contextp->time();
    last_pmu_progress_value = 0;
    dut_reset();
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
        // static_assert(VlIsVlWide<std::decay<decltype(dut->io_mem_rd_data)>::type>::value, "Check io_mem type");
        // static_assert(VlIsVlWide<std::decay<decltype(dut->io_mem_wr_data)>::type>::value, "Check io_mem type");
        // static_assert(VlIsVlWide<std::decay<decltype(dut->io_mem_wr_mask)>::type>::value, "Check io_mem type");

        //
        // Dcache rsp
        //
        dut->io_dcache_rsp_0_valid = false;
        dut->io_dcache_rsp_1_valid = false;
        if (!dcache_queue.empty()) {
            auto& rsp = dcache_queue.front();
            if (rsp->sm_id == 0) {
                dcache_rsp_sm0(dut, rsp);
                if (dut->io_dcache_rsp_0_ready && dut->io_dcache_rsp_0_valid) {
                    dcache_queue.pop();
                }
            } else if (rsp->sm_id == 1) {
                dcache_rsp_sm1(dut, rsp);
                if (dut->io_dcache_rsp_1_ready && dut->io_dcache_rsp_1_valid) {
                    dcache_queue.pop();
                }
            } else {
                SPDLOG_LOGGER_ERROR(logger, "Unknown sm_id {} in dcache_response", rsp->sm_id);
                sim_got_error = true;
            }
        }

        //
        // Icache rsp
        //
        dut->io_icache_0_rsp_valid = false;
        dut->io_icache_1_rsp_valid = false;
        if (!icache_queue.empty()) {
            auto& rsp = icache_queue.front();
            if (rsp->sm_id == 0) {
                icache_rsp_sm0(dut, rsp);
                if (dut->io_icache_0_rsp_ready && dut->io_icache_0_rsp_valid) {
                    icache_queue.pop();
                }
            } else if (rsp->sm_id == 1) {
                icache_rsp_sm1(dut, rsp);
                if (dut->io_icache_1_rsp_ready && dut->io_icache_1_rsp_valid) {
                    icache_queue.pop();
                }
            } else {
                SPDLOG_LOGGER_ERROR(logger, "Unknown sm_id {} in icache_response", rsp->sm_id);
                sim_got_error = true;
            }
        }

        //
        // Get and serve new dcache requests
        //
        auto dcache_reqs = get_dcache_req(dut);
        for (auto& req : dcache_reqs) {
            paddr_t paddr_base = ((req->tag << log2Ceil(L1D_NUM_SET)) | req->setIdx)
                << log2Ceil(L1D_BLOCK_NUM_WORD) << 2;
            if (req->opcode == L1D_OPCODE_READ) {
                for (int i = 0; i < NUM_THREAD; i++) {
                    if (req->mask[i]) {
                        paddr_t paddr = paddr_base + (req->blockOffset[i] << 2);
                        if (!pmem->read(paddr, &req->data[i], sizeof(req->data[i]))) {
                            SPDLOG_LOGGER_ERROR(logger, "Failed to read from physical memory at address {:#x}", paddr);
                            sim_got_error = true;
                        }
                    }
                }
            } else if (req->opcode == L1D_OPCODE_WRITE) {
                // SPDLOG_LOGGER_DEBUG(logger,
                //     "L1D write: sm {} paddr=0x{:x}, mask=0x{:x}, data=0x{:x}",
                //     req->sm_id, paddr_base, req->mask.to_ulong(), fmt::join(req->data, ",")
                // );
                for (int i = 0; i < NUM_THREAD; i++) {
                    if (req->mask[i]) {
                        paddr_t paddr = paddr_base + (req->blockOffset[i] << 2);
                        std::bitset<4> wordOffset1H(req->wordOffset1H[i]);
                        const uint8_t* data = reinterpret_cast<const uint8_t*>(&req->data[i]);
                        for (paddr_t byteOffset_a = 0, byteOffset_d = 0; byteOffset_a < 4; byteOffset_a++) {
                            if (wordOffset1H[byteOffset_a]) {
                                if (!pmem->write(paddr + byteOffset_a, data + byteOffset_d, 1)) {
                                    SPDLOG_LOGGER_ERROR(
                                        logger, "Failed to write to physical memory at address {:#x}",
                                        paddr + byteOffset_a
                                    );
                                    sim_got_error = true;
                                }
                                byteOffset_d++;
                            }
                        }
                    }
                }
            } else if (req->opcode == L1D_OPCODE_CACHEOP) {
                // do nothing as here is no cache
                req.reset();
            } else { // TODO: not support atomic and cache flush yet
                SPDLOG_LOGGER_ERROR(logger, "Unsupported dcache request opcode {}, TODO", req->opcode);
                sim_got_error = true;
                assert(0);
            }
            if (req) {
                dcache_queue.push(std::move(req));
            }
        }

        //
        // Get and serve new icache requests
        //
        auto icache_reqs = get_icache_req(dut);
        for (auto& req : icache_reqs) {
            if (!pmem->read(req->addr, req->data, sizeof(req->data))) {
                SPDLOG_LOGGER_ERROR(logger, "Failed to read from physical memory at address {:#x}", req->addr);
                sim_got_error = true;
            }
            icache_queue.push(std::move(req));
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
    // Clock output
    //
    if (contextp->time() % 10000 == 0) {
        logger->debug("");
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
            raise(SIGABRT); // 让进程按默认方式终止
        } else {
            std::exit(EXIT_FAILURE);
        }
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
    if (!step_status.time_exceed && !step_status.error && contextp->time() % config.snapshot.time_interval == 0) {
        snapshot_fork();
    }

    return &step_status;
}

#define COPY_PIPELINE_PMU(SM)                                                                                         \
    do {                                                                                                              \
        auto& dst = pmu_snapshot.pipeline[SM];                                                                        \
        dst.active_cycles = dut->io_pmu_pipeline_##SM##_activeCycles;                                                 \
        dst.total_scalar_issued = dut->io_pmu_pipeline_##SM##_totalScalarIssued;                                      \
        dst.total_vector_issued = dut->io_pmu_pipeline_##SM##_totalVectorIssued;                                      \
        dst.exec_structural_hazard_cycles_x = dut->io_pmu_pipeline_##SM##_execStructuralHazardCyclesX;                \
        dst.exec_structural_hazard_cycles_v = dut->io_pmu_pipeline_##SM##_execStructuralHazardCyclesV;                \
        dst.data_dep_stall_cycles = dut->io_pmu_pipeline_##SM##_dataDepStallCycles;                                  \
        dst.barrier_stall_cycles = dut->io_pmu_pipeline_##SM##_barrierStallCycles;                                   \
        dst.control_hazard_flush_count = dut->io_pmu_pipeline_##SM##_controlHazardFlushCount;                        \
        dst.frontend_stall_cycles = dut->io_pmu_pipeline_##SM##_frontendStallCycles;                                 \
        dst.lsu_backpressure_cycles = dut->io_pmu_pipeline_##SM##_lsuBackpressureCycles;                             \
        dst.ibuffer_full_cycles = dut->io_pmu_pipeline_##SM##_ibufferFullCycles;                                     \
    } while (false)

#define COPY_INST_CLASS_PMU(SM)                                                                                       \
    do {                                                                                                              \
        auto& dst = pmu_snapshot.inst_class[SM];                                                                      \
        dst.compute_issued = dut->io_pmu_instClass_##SM##_computeIssued;                                             \
        dst.mem_issued = dut->io_pmu_instClass_##SM##_memIssued;                                                     \
        dst.ctrl_issued = dut->io_pmu_instClass_##SM##_ctrlIssued;                                                   \
    } while (false)

void ventus_rtlsim_t::sample_pmu_snapshot() {
    COPY_PIPELINE_PMU(0);
    COPY_PIPELINE_PMU(1);
    COPY_INST_CLASS_PMU(0);
    COPY_INST_CLASS_PMU(1);
}

void ventus_rtlsim_t::update_pmu_watchdog() {
    if (config.hang_timeout == 0 || step_status.error || step_status.time_exceed || step_status.idle) {
        last_pmu_progress_time = contextp->time();
        last_pmu_progress_value = 0;
        return;
    }

    uint64_t issued = 0;
    uint64_t mem_issued = 0;
    for (uint32_t i = 0; i < pmu_snapshot.num_sm; i++) {
        issued += pmu_snapshot.pipeline[i].total_scalar_issued + pmu_snapshot.pipeline[i].total_vector_issued;
        mem_issued += pmu_snapshot.inst_class[i].mem_issued;
    }
    const uint64_t progress = issued + mem_issued;

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
        "PMU watchdog detected HANG: no issue/memory progress for {} time units, time={}, issued={}, mem_issued={}",
        idle_time, contextp->time(), issued, mem_issued
    );
}

ventus_rtlsim_pmu_t ventus_rtlsim_t::pmu_view() const {
    return {
        pmu_snapshot.num_sm,
        pmu_snapshot.has_dcache,
        pmu_snapshot.pipeline.data(),
        pmu_snapshot.inst_class.data(),
        nullptr,
    };
}

#undef COPY_PIPELINE_PMU
#undef COPY_INST_CLASS_PMU

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

    if (tfp)
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
        sigprocmask(SIG_BLOCK, &set, &oldset); // Block SIG for using sigwait
        sigwaitinfo(&set, &info);              // Wait for snapshot-rollback
        // main process invoked snapshot rollback
        sigprocmask(SIG_SETMASK, &oldset, NULL); // Change signal blocking mask back
        assert(info.si_signo == SNAPSHOT_WAKEUP_SIGNAL);
        snapshots.main_exit_time = (uint64_t)(info.si_value.sival_ptr);
        logger->info(
            "SNAPSHOT is activated, sim_time = {}, origin process exited at time {}", contextp->time(),
            snapshots.main_exit_time
        );
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

std::vector<std::unique_ptr<icache_reqrsp_t>> get_icache_req(Vdut* dut) {
    auto reqs = std::vector<std::unique_ptr<icache_reqrsp_t>>();
    reqs.reserve(NUM_SM);
    dut->io_icache_0_req_ready = true;
    if (dut->io_icache_0_req_valid) {
        auto req = std::make_unique<icache_reqrsp_t>();
        req->sm_id = 0;
        req->source = dut->io_icache_0_req_bits_a_source;
        req->addr = dut->io_icache_0_req_bits_a_addr;
        reqs.push_back(std::move(req));
    }
    dut->io_icache_1_req_ready = true;
    if (dut->io_icache_1_req_valid) {
        auto req = std::make_unique<icache_reqrsp_t>();
        req->sm_id = 1;
        req->source = dut->io_icache_1_req_bits_a_source;
        req->addr = dut->io_icache_1_req_bits_a_addr;
        reqs.push_back(std::move(req));
    }
    return reqs;
}

#define SET_ICACHE_RSP_SM(sm_id_, dut, rsp)                                                                            \
    do {                                                                                                               \
        assert(dut&& rsp);                                                                                             \
        assert(rsp->sm_id == sm_id_);                                                                                  \
        dut->io_icache_##sm_id_##_rsp_valid = true;                                                                    \
        dut->io_icache_##sm_id_##_rsp_bits_d_source = rsp->source;                                                     \
        dut->io_icache_##sm_id_##_rsp_bits_d_addr = rsp->addr;                                                         \
        dut->io_icache_##sm_id_##_rsp_bits_d_data_0 = rsp->data[0];                                                    \
        dut->io_icache_##sm_id_##_rsp_bits_d_data_1 = rsp->data[1];                                                    \
        dut->io_icache_##sm_id_##_rsp_bits_d_data_2 = rsp->data[2];                                                    \
        dut->io_icache_##sm_id_##_rsp_bits_d_data_3 = rsp->data[3];                                                    \
        dut->io_icache_##sm_id_##_rsp_bits_d_data_4 = rsp->data[4];                                                    \
        dut->io_icache_##sm_id_##_rsp_bits_d_data_5 = rsp->data[5];                                                    \
        dut->io_icache_##sm_id_##_rsp_bits_d_data_6 = rsp->data[6];                                                    \
        dut->io_icache_##sm_id_##_rsp_bits_d_data_7 = rsp->data[7];                                                    \
        dut->io_icache_##sm_id_##_rsp_bits_d_data_8 = rsp->data[8];                                                    \
        dut->io_icache_##sm_id_##_rsp_bits_d_data_9 = rsp->data[9];                                                    \
        dut->io_icache_##sm_id_##_rsp_bits_d_data_10 = rsp->data[10];                                                  \
        dut->io_icache_##sm_id_##_rsp_bits_d_data_11 = rsp->data[11];                                                  \
        dut->io_icache_##sm_id_##_rsp_bits_d_data_12 = rsp->data[12];                                                  \
        dut->io_icache_##sm_id_##_rsp_bits_d_data_13 = rsp->data[13];                                                  \
        dut->io_icache_##sm_id_##_rsp_bits_d_data_14 = rsp->data[14];                                                  \
        dut->io_icache_##sm_id_##_rsp_bits_d_data_15 = rsp->data[15];                                                  \
        dut->io_icache_##sm_id_##_rsp_bits_d_data_16 = rsp->data[16];                                                  \
        dut->io_icache_##sm_id_##_rsp_bits_d_data_17 = rsp->data[17];                                                  \
        dut->io_icache_##sm_id_##_rsp_bits_d_data_18 = rsp->data[18];                                                  \
        dut->io_icache_##sm_id_##_rsp_bits_d_data_19 = rsp->data[19];                                                  \
        dut->io_icache_##sm_id_##_rsp_bits_d_data_20 = rsp->data[20];                                                  \
        dut->io_icache_##sm_id_##_rsp_bits_d_data_21 = rsp->data[21];                                                  \
        dut->io_icache_##sm_id_##_rsp_bits_d_data_22 = rsp->data[22];                                                  \
        dut->io_icache_##sm_id_##_rsp_bits_d_data_23 = rsp->data[23];                                                  \
        dut->io_icache_##sm_id_##_rsp_bits_d_data_24 = rsp->data[24];                                                  \
        dut->io_icache_##sm_id_##_rsp_bits_d_data_25 = rsp->data[25];                                                  \
        dut->io_icache_##sm_id_##_rsp_bits_d_data_26 = rsp->data[26];                                                  \
        dut->io_icache_##sm_id_##_rsp_bits_d_data_27 = rsp->data[27];                                                  \
        dut->io_icache_##sm_id_##_rsp_bits_d_data_28 = rsp->data[28];                                                  \
        dut->io_icache_##sm_id_##_rsp_bits_d_data_29 = rsp->data[29];                                                  \
        dut->io_icache_##sm_id_##_rsp_bits_d_data_30 = rsp->data[30];                                                  \
        dut->io_icache_##sm_id_##_rsp_bits_d_data_31 = rsp->data[31];                                                  \
    } while (0)
void icache_rsp_sm0(Vdut* dut, const std::unique_ptr<icache_reqrsp_t>& rsp) { SET_ICACHE_RSP_SM(0, dut, rsp); }
void icache_rsp_sm1(Vdut* dut, const std::unique_ptr<icache_reqrsp_t>& rsp) { SET_ICACHE_RSP_SM(1, dut, rsp); }

// clang-format off
std::vector<std::unique_ptr<dcache_reqrsp_t>> get_dcache_req(Vdut* dut) {
    auto reqs = std::vector<std::unique_ptr<dcache_reqrsp_t>>();
    reqs.reserve(NUM_SM);
#define GET_DCACHE_REQ_THREAD(req, sm_id, threadIdx) \
do { \
    req->mask[threadIdx] = dut->io_dcache_req_##sm_id##_bits_perLaneAddr_##threadIdx##_activeMask; \
    req->blockOffset[threadIdx] = dut->io_dcache_req_##sm_id##_bits_perLaneAddr_##threadIdx##_blockOffset; \
    req->wordOffset1H[threadIdx] = dut->io_dcache_req_##sm_id##_bits_perLaneAddr_##threadIdx##_wordOffset1H; \
    req->data[threadIdx] = dut->io_dcache_req_##sm_id##_bits_data_##threadIdx; \
} while(0)
#define GET_DCACHE_REQ_SM(sm_id_) \
do { \
    if (dut->io_dcache_req_##sm_id_##_valid == 0) { \
        break; \
    } \
    auto req = std::make_unique<dcache_reqrsp_t>(); \
    dut->io_dcache_req_##sm_id_##_ready = true; \
    req->instrId = dut->io_dcache_req_##sm_id_##_bits_instrId; \
    req->sm_id = sm_id_; \
    req->opcode = dut->io_dcache_req_##sm_id_##_bits_opcode; \
    req->param = dut->io_dcache_req_##sm_id_##_bits_param; \
    req->setIdx = dut->io_dcache_req_##sm_id_##_bits_setIdx; \
    req->tag = dut->io_dcache_req_##sm_id_##_bits_tag; \
    GET_DCACHE_REQ_THREAD(req, sm_id_, 0); \
    GET_DCACHE_REQ_THREAD(req, sm_id_, 1); \
    GET_DCACHE_REQ_THREAD(req, sm_id_, 2); \
    GET_DCACHE_REQ_THREAD(req, sm_id_, 3); \
    GET_DCACHE_REQ_THREAD(req, sm_id_, 4); \
    GET_DCACHE_REQ_THREAD(req, sm_id_, 5); \
    GET_DCACHE_REQ_THREAD(req, sm_id_, 6); \
    GET_DCACHE_REQ_THREAD(req, sm_id_, 7); \
    GET_DCACHE_REQ_THREAD(req, sm_id_, 8); \
    GET_DCACHE_REQ_THREAD(req, sm_id_, 9); \
    GET_DCACHE_REQ_THREAD(req, sm_id_, 10); \
    GET_DCACHE_REQ_THREAD(req, sm_id_, 11); \
    GET_DCACHE_REQ_THREAD(req, sm_id_, 12); \
    GET_DCACHE_REQ_THREAD(req, sm_id_, 13); \
    GET_DCACHE_REQ_THREAD(req, sm_id_, 14); \
    GET_DCACHE_REQ_THREAD(req, sm_id_, 15); \
    GET_DCACHE_REQ_THREAD(req, sm_id_, 16); \
    GET_DCACHE_REQ_THREAD(req, sm_id_, 17); \
    GET_DCACHE_REQ_THREAD(req, sm_id_, 18); \
    GET_DCACHE_REQ_THREAD(req, sm_id_, 19); \
    GET_DCACHE_REQ_THREAD(req, sm_id_, 20); \
    GET_DCACHE_REQ_THREAD(req, sm_id_, 21); \
    GET_DCACHE_REQ_THREAD(req, sm_id_, 22); \
    GET_DCACHE_REQ_THREAD(req, sm_id_, 23); \
    GET_DCACHE_REQ_THREAD(req, sm_id_, 24); \
    GET_DCACHE_REQ_THREAD(req, sm_id_, 25); \
    GET_DCACHE_REQ_THREAD(req, sm_id_, 26); \
    GET_DCACHE_REQ_THREAD(req, sm_id_, 27); \
    GET_DCACHE_REQ_THREAD(req, sm_id_, 28); \
    GET_DCACHE_REQ_THREAD(req, sm_id_, 29); \
    GET_DCACHE_REQ_THREAD(req, sm_id_, 30); \
    GET_DCACHE_REQ_THREAD(req, sm_id_, 31); \
    reqs.push_back(std::move(req)); \
} while(0)
    GET_DCACHE_REQ_SM(0);
    GET_DCACHE_REQ_SM(1);
    return reqs;
#undef GET_DCACHE_REQ_SM
#undef GET_DCACHE_REQ_THREAD
}
// clang-format on

// clang-format off
#define SET_DCACHE_RSP_THREAD(sm_id, threadIdx) \
do { \
    dut->io_dcache_rsp_##sm_id##_bits_activeMask_##threadIdx = rsp->mask[threadIdx]; \
    dut->io_dcache_rsp_##sm_id##_bits_data_##threadIdx = rsp->data[threadIdx]; \
} while(0)
#define SET_DCACHE_RSP_SM(sm_id_) \
do { \
    dut->io_dcache_rsp_##sm_id_##_valid = 1; \
    dut->io_dcache_rsp_##sm_id_##_bits_instrId = rsp->instrId; \
    SET_DCACHE_RSP_THREAD(sm_id_, 0); \
    SET_DCACHE_RSP_THREAD(sm_id_, 1); \
    SET_DCACHE_RSP_THREAD(sm_id_, 2); \
    SET_DCACHE_RSP_THREAD(sm_id_, 3); \
    SET_DCACHE_RSP_THREAD(sm_id_, 4); \
    SET_DCACHE_RSP_THREAD(sm_id_, 5); \
    SET_DCACHE_RSP_THREAD(sm_id_, 6); \
    SET_DCACHE_RSP_THREAD(sm_id_, 7); \
    SET_DCACHE_RSP_THREAD(sm_id_, 8); \
    SET_DCACHE_RSP_THREAD(sm_id_, 9); \
    SET_DCACHE_RSP_THREAD(sm_id_, 10); \
    SET_DCACHE_RSP_THREAD(sm_id_, 11); \
    SET_DCACHE_RSP_THREAD(sm_id_, 12); \
    SET_DCACHE_RSP_THREAD(sm_id_, 13); \
    SET_DCACHE_RSP_THREAD(sm_id_, 14); \
    SET_DCACHE_RSP_THREAD(sm_id_, 15); \
    SET_DCACHE_RSP_THREAD(sm_id_, 16); \
    SET_DCACHE_RSP_THREAD(sm_id_, 17); \
    SET_DCACHE_RSP_THREAD(sm_id_, 18); \
    SET_DCACHE_RSP_THREAD(sm_id_, 19); \
    SET_DCACHE_RSP_THREAD(sm_id_, 20); \
    SET_DCACHE_RSP_THREAD(sm_id_, 21); \
    SET_DCACHE_RSP_THREAD(sm_id_, 22); \
    SET_DCACHE_RSP_THREAD(sm_id_, 23); \
    SET_DCACHE_RSP_THREAD(sm_id_, 24); \
    SET_DCACHE_RSP_THREAD(sm_id_, 25); \
    SET_DCACHE_RSP_THREAD(sm_id_, 26); \
    SET_DCACHE_RSP_THREAD(sm_id_, 27); \
    SET_DCACHE_RSP_THREAD(sm_id_, 28); \
    SET_DCACHE_RSP_THREAD(sm_id_, 29); \
    SET_DCACHE_RSP_THREAD(sm_id_, 30); \
    SET_DCACHE_RSP_THREAD(sm_id_, 31); \
} while(0)
// clang-format on

void dcache_rsp_sm0(Vdut* dut, const std::unique_ptr<dcache_reqrsp_t>& rsp) {
    assert(dut && rsp);
    assert(rsp->sm_id == 0);
    SET_DCACHE_RSP_SM(0);
}
void dcache_rsp_sm1(Vdut* dut, const std::unique_ptr<dcache_reqrsp_t>& rsp) {
    assert(dut && rsp);
    assert(rsp->sm_id == 1);
    SET_DCACHE_RSP_SM(1);
}

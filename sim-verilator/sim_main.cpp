#include "Vdut.h"
#include "common.h"
#include "cta_sche_wrapper.hpp"
#include "kernel.hpp"
#include "log.h"
#include <bits/types/sigset_t.h>
#include <cinttypes>
#include <csignal>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <deque>
#include <filesystem>
#include <functional>
#include <iostream>
#include <memory> // For std::unique_ptr
#include <new>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>
#include <vector>
#include <verilated.h>
#include <verilated_fst_c.h>

// g_config is read-only commonly,
// bug it will be forced into writable while parsing cmd-args
const global_config_t g_config = {
    .sim_time_max = 800000,
    .waveform = { .enable = true , .time_begin = 20000, .time_end = 30000, },
    .snapshot = { .enable = true , .time_interval = 50000, .num_max = 1, },
};

typedef struct {
    bool is_child;
    std::deque<pid_t> children_pid; // front is newest, back is oldest
} snapshot_t;

static VerilatedFstC* tfp         = nullptr;
static VerilatedContext* contextp = nullptr;
static Vdut* dut                  = nullptr;
static snapshot_t snapshots       = { .is_child = false };

void waveform_dump() {
    // snapshot child process always enables waveform dump
    bool is_snapshot = g_config.snapshot.enable && snapshots.is_child;
    if (!g_config.waveform.enable && !is_snapshot)
        return;

    assert(contextp && tfp);
    uint64_t time = contextp->time();
    if (is_snapshot || time >= g_config.waveform.time_begin && time < g_config.waveform.time_end) {
        tfp->dump(time);
    }
}

void snapshot_fork(snapshot_t* snap) {
    if (!g_config.snapshot.enable || snap->is_child)
        return;

    // delete oldest snapshot if needed
    if (snap->children_pid.size() >= g_config.snapshot.num_max) {
        pid_t oldest = snap->children_pid.back();
        kill(oldest, SIGKILL);
        waitpid(oldest, NULL, 0);
        snap->children_pid.pop_back();
    }
    // fork a new snapshot process
    dut->prepareClone(); // see https://verilator.org/guide/latest/connecting.html#process-level-clone-apis
    pid_t child_pid = fork();
    dut->atClone();
    if (child_pid < 0) {
        log_error("SNAPSHOT: failed to fork new child process");
        return;
    }
    if (child_pid != 0) { // for the original process
        snap->children_pid.push_front(child_pid);
        log_debug("SNAPSHOT created, pid=%d", child_pid);
    } else { // for the fork-child snapshot process
        snap->is_child = true;
        sigset_t set;
        int sig;
        sigemptyset(&set);
        sigaddset(&set, SIGUSR2);
        sigprocmask(SIG_BLOCK, &set, NULL); // Block SIGUSR1 for using sigwait
        sigwait(&set, &sig);                // Wait for snapshot-rollback
        sigprocmask(SIG_UNBLOCK, &set, NULL);
        assert(sig == SIGUSR2); // This snapshot is activated
        log_info("SNAPSHOT is activated, sim_time = %llu", contextp->time());
        // delete tfp;                  // Cannot do this, or it will block the process
        // (maybe because Vdut.fst is closed in the parent process?)
        tfp = new VerilatedFstC(); // This will cause memory leak for once, but not serious. How to fix it?
        dut->trace(tfp, 99);
        tfp->open("obj_dir/Vdut_snapshot.fst");
    }
}
void snapshot_rollback(snapshot_t* snap) {
    if (!g_config.snapshot.enable || snap->is_child)
        return;

    pid_t child = snap->children_pid.back(); // Choose the oldest snapshot
    kill(child, SIGUSR2);                    // Activate the snapshot
    waitpid(child, NULL, 0);                 // Wait for snapshot finished
    snap->children_pid.pop_back();
}
void snapshot_kill_all(snapshot_t* snap) {
    while (!snap->children_pid.empty()) {
        pid_t child = snap->children_pid.back();
        kill(child, SIGKILL);
        waitpid(child, NULL, 0);
        snap->children_pid.pop_back();
    }
}

void dut_reset(Vdut* dut);
int parse_arg(std::vector<std::string> args, std::function<void(std::shared_ptr<Kernel>)> new_kernel);

int main(int argc, char** argv) {
    // Verilator simulation context init
    contextp = new VerilatedContext;
    contextp->debug(0);     // debug level, 0 is off, 9 is highest, may be overridden by commandArgs parsing
    contextp->randReset(0); // Randomization reset policy, may be overridden by commandArgs argument parsing
    contextp->traceEverOn(true);

    // Hardware construct
    dut         = new Vdut(contextp, "VENTUS_GPGPU");
    MemBox* mem = new MemBox;
    Cta cta(mem);

    // Parse ventus-sim cmd arguments
    std::vector<std::string> args;
    if (argc == 1) { // Default arguments
        std::cout << "[Info] using default cmdline arguments: -f ventus_args.txt" << std::endl;
        args.push_back("-f");
        args.push_back("ventus_args.txt");
    } else {
        for (int i = 1; i < argc; i++) {
            args.push_back(argv[i]);
        }
    }
    parse_arg(
        args, std::function<void(std::shared_ptr<Kernel>)>(std::bind(&Cta::kernel_add, &cta, std::placeholders::_1)));
    const char* verilator_runtime_args_default[] = { "+verilator+seed+10086" };
    contextp->commandArgsAdd(sizeof(verilator_runtime_args_default) / sizeof(verilator_runtime_args_default[0]),
        verilator_runtime_args_default);
    contextp->commandArgs(argc, argv);

    // waveform traces (FST)
    tfp = new VerilatedFstC;
    dut->trace(tfp, 99);
    tfp->open("obj_dir/Vdut.fst");

    // =
    // Simulation begin
    // =

    snapshot_fork(&snapshots);
    log_debug("SNAPSHOT: Init fork ok");

    // DUT initial reset
    dut_reset(dut);
    log_trace("Hardware reset ok");

    bool sim_got_error = false;
    //     |----硬件终止仿真----|    |仿真结果有误|    |-------------仿真超时终止-------------|    |-仿真成功完成-|
    while (!contextp->gotFinish() && !sim_got_error && contextp->time() < g_config.sim_time_max && !cta.is_idle()) {
        static bool is_child = false;
        contextp->timeInc(1); // 1 timeprecision period passes...
        dut->clock = !dut->clock;

        //
        // Delta time before clock edge
        //

        dut->io_host_rsp_ready = 1;
        if (dut->clock == 0) {
            // Input stimulus apply to hardware at negedge clk
            // Host WG new
            cta.apply_to_dut(dut);
        }

        //
        // Eval
        //
        dut->eval();
        waveform_dump();

        //
        // time after clock edge
        // DUT outputs will not change until next clock edge
        // React to new output & prepare for new input stimulus
        //
        if (dut->clock == 0) {
            if (dut->io_host_req_valid && dut->io_host_req_ready) {
                uint32_t wg_id = dut->io_host_req_bits_host_wg_id;
                uint32_t wg_idx, kernel_id;
                std::string kernel_name;
                assert(cta.wg_get_info(kernel_name, kernel_id, wg_idx));
                log_debug("block%2d dispatched to GPU (kernel%2d %s block%2d) ", wg_id, kernel_id, kernel_name.c_str(),
                    wg_idx);
                cta.wg_dispatched();
            }
            if (dut->io_host_rsp_valid && dut->io_host_rsp_ready) {
                uint32_t wg_id = dut->io_host_rsp_bits_inflight_wg_buffer_host_wf_done_wg_id;
                cta.wg_finish(wg_id);
            }
        } else {
            // Memory access: read
            if (dut->io_mem_rd_en) {
                volatile uint32_t rd_addr = dut->io_mem_rd_addr;
                uint8_t* rd_data          = new uint8_t[MEMACCESS_DATA_BYTE_SIZE];
                if (!mem->read(dut->io_mem_rd_addr, rd_data)) {
                    // std::cerr << "Read uninitialized memory: 0x" << std::hex << dut->io_mem_rd_addr << std::dec
                    //           << " @ time " << clk_cnt << std::endl;
                    // getchar();
                    // break;
                }
                memcpy(dut->io_mem_rd_data.data(), rd_data, MEMACCESS_DATA_BYTE_SIZE);
                delete[] rd_data;
            }
            // Memory access: write
            if (dut->io_mem_wr_en) {
                bool mask[MEMACCESS_DATA_BYTE_SIZE];
                for (int idx = 0; idx < 4; idx++) {
                    uint32_t mask_raw = dut->io_mem_wr_mask[idx];
                    for (int bit = 0; bit < 32; bit++) {
                        mask[bit + idx * 32] = mask_raw & 0x1;
                        mask_raw >>= 1;
                    }
                }
                mem->write(dut->io_mem_wr_addr, mask, reinterpret_cast<uint8_t*>(dut->io_mem_wr_data.data()));
            }
        }

        //
        // Clock output
        //
        if (contextp->time() % 10000 == 0)
            log_debug("");

        //
        // snapshot fork
        //
        if (contextp->time() % g_config.snapshot.time_interval == 0 && !sim_got_error && !contextp->gotFinish()) {
            snapshot_fork(&snapshots);
        }
    }

    uint64_t sim_end_time = contextp->time();
    sim_got_error         = sim_got_error || contextp->gotError() || contextp->gotFinish();

    if (g_config.snapshot.enable && snapshots.is_child) {
        if (sim_got_error) {
            log_info("SNAPSHOT exited at %d unit time", sim_end_time);
        } else {
            log_error("SNAPSHOT finished NORMALLY in %d unit time, which means it different from the original "
                      "simulation process",
                sim_end_time);
        }

    } else {
        if (sim_got_error) {
            log_fatal("Simulation exited ABNORMALLY at %d unit time", sim_end_time);
        } else {
            log_info("Simulation finished in %d unit time", sim_end_time);
        }
    }

    tfp->close();
    dut->final();                  // Final model cleanup
    contextp->statsPrintSummary(); // Final simulation summary
    delete dut;
    delete contextp;
    delete tfp;

    if (g_config.snapshot.enable && !snapshots.is_child && snapshots.children_pid.size() != 0 && sim_got_error) {
        log_info("SNAPSHOT rollback to %d time-unit ago, pid=%d",
            sim_end_time % g_config.snapshot.time_interval
                + (snapshots.children_pid.size() - 1) * g_config.snapshot.time_interval,
            snapshots.children_pid.front());
        snapshot_rollback(&snapshots); // Exec snapshot
    }
    if (g_config.snapshot.enable && snapshots.is_child) {
        log_info("SNAPSHOT process exit... wavefrom dumped as obj_dir/Vdut_snapshot.fst");
    } else {
        snapshot_kill_all(&snapshots);
    }
    return 0;
}

void dut_reset(Vdut* dut) {
    assert(dut && contextp);
    contextp->time(0);
    dut->io_host_req_valid = 0;
    dut->io_host_rsp_ready = 0;
    dut->reset             = 1;
    dut->clock             = 0;
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
}

// let the log system know the simulation time
extern "C" {
uint64_t log_get_time() {
    assert(contextp);
    return contextp->time();
}
}

// Legacy function required only so linking works on Cygwin and MSVC++
double sc_time_stamp() { return 0; }

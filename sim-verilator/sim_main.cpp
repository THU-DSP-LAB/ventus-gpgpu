#include "Vdut.h"
#include "common.h"
#include "cta_sche_wrapper.hpp"
#include "kernel.hpp"
#include "log.h"
#include <bits/types/siginfo_t.h>
#include <bits/types/sigset_t.h>
#include <csignal>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <deque>
#include <filesystem>
#include <functional>
#include <memory> // For std::unique_ptr
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>
#include <vector>
#include <verilated.h>
#include <verilated_fst_c.h>

int main_new(int argc, char* argv[]);
int main_old(int argc, char* argv[]);

int main(int argc, char* argv[]) {
    return main_new(argc, argv);
}

// real global config (only write in cmdarg.cpp)
global_config_t g_config_writable = {
    .sim_time_max = 800000,
    .waveform     = { .enable = true, .time_begin = 20000, .time_end = 30000, .filename = "obj_dir/Vdut.fst" },
    .snapshot     = { .enable = true, .time_interval = 50000, .num_max = 1, .filename = "obj_dir/Vdut.snapshot.fst" },
};
// read-only global config
const global_config_t& g_config = g_config_writable;

typedef struct {
    bool is_child;
    uint64_t main_exit_time;        // when does the main simulation process exit
    std::deque<pid_t> children_pid; // front is newest, back is oldest
} snapshot_t;

// global variables used in this file
#define SNAPSHOT_WAKEUP_SIGNAL SIGRTMIN
static VerilatedFstC* tfp         = nullptr;
static VerilatedContext* contextp = nullptr;
static Vdut* dut                  = nullptr;
static snapshot_t snapshots       = { .is_child = false };

void waveform_dump();
void snapshot_fork(snapshot_t* snap);
void snapshot_rollback(uint64_t time);
void snapshot_kill_all(snapshot_t* snap);
void dut_reset(Vdut* dut);
extern int parse_arg(std::vector<std::string> args, std::function<void(std::shared_ptr<Kernel>)> new_kernel);

int main_old(int argc, char** argv) {
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
        puts("[Info] using default cmdline arguments: -f ventus_args.txt");
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
    tfp->open(g_config.waveform.filename.c_str());

    // =
    // Simulation begin
    // =
    snapshot_fork(&snapshots); // initial snapshot at sim_time = 0

    // DUT initial reset
    dut_reset(dut);
    log_trace("Hardware reset ok");

    bool sim_got_error = false;
    //     |----硬件终止仿真----|    |仿真结果有误|    |-------------仿真超时终止-------------|    |-仿真成功完成-|
    while (!contextp->gotFinish() && !sim_got_error && contextp->time() < g_config.sim_time_max && !cta.is_idle()) {
        contextp->timeInc(1);
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
            // Check if io_mem is VlWide
            static_assert(VlIsVlWide<std::decay<decltype(dut->io_mem_rd_data)>::type>::value, "Check io_mem type");
            static_assert(VlIsVlWide<std::decay<decltype(dut->io_mem_wr_data)>::type>::value, "Check io_mem type");
            static_assert(VlIsVlWide<std::decay<decltype(dut->io_mem_wr_mask)>::type>::value, "Check io_mem type");
            // Memory access: read
            if (dut->io_mem_rd_en) {
                uint32_t rd_addr = dut->io_mem_rd_addr;
                uint8_t* rd_data = new uint8_t[dut->io_mem_rd_data.Words * 4];
                if (!mem->read(dut->io_mem_rd_addr, rd_data)) {
                    // std::cerr << "Read uninitialized memory: 0x" << std::hex << dut->io_mem_rd_addr << std::dec
                    //           << " @ time " << clk_cnt << std::endl;
                    // getchar();
                    // break;
                }
                memcpy(dut->io_mem_rd_data.data(), rd_data, dut->io_mem_rd_data.Words * 4);
                delete[] rd_data;
            }
            // Memory access: write
            if (dut->io_mem_wr_en) {
                bool* mask = new bool[dut->io_mem_wr_mask.Words * 32];
                for (int idx = 0; idx < dut->io_mem_wr_mask.Words; idx++) {
                    uint32_t mask_raw = dut->io_mem_wr_mask[idx];
                    for (int bit = 0; bit < 32; bit++) {
                        mask[bit + idx * 32] = mask_raw & 0x1;
                        mask_raw >>= 1;
                    }
                }
                mem->write(dut->io_mem_wr_addr, mask, reinterpret_cast<uint8_t*>(dut->io_mem_wr_data.data()));
                delete[] mask;
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

    // =
    // Simulation end
    // =

    uint64_t sim_end_time = contextp->time();
    sim_got_error         = sim_got_error || contextp->gotError() || contextp->gotFinish();

    // prints simulation result
    if (g_config.snapshot.enable && snapshots.is_child) {
        if (sim_got_error) {
            if (sim_end_time == snapshots.main_exit_time) {
                log_info("SNAPSHOT exited at time %d, OK", sim_end_time);
            } else {
                log_error("SNAPSHOT exited at time %d, which differs from the original process (time %d)", sim_end_time,
                    snapshots.main_exit_time);
            }
        } else {
            log_error("SNAPSHOT finished NORMALLY at time %d, which differs from the original process", sim_end_time);
        }

    } else {
        if (sim_got_error) {
            log_fatal("Simulation exited ABNORMALLY at time %d", sim_end_time);
        } else {
            log_info("Simulation finished in %d unit time", sim_end_time);
        }
    }

    tfp->close();
    dut->final();                  // Final model cleanup
    contextp->statsPrintSummary(); // Final simulation summary

    // invoke snapshot if needed
    if (g_config.snapshot.enable && !snapshots.is_child && snapshots.children_pid.size() != 0 && sim_got_error) {
        snapshot_rollback(sim_end_time); // Exec snapshot
    }
    // clear snapshots
    if (g_config.snapshot.enable && snapshots.is_child) {
        log_info("SNAPSHOT process exit... wavefrom dumped as %s", g_config.snapshot.filename.c_str());
    } else {
        snapshot_kill_all(&snapshots); // kill unused snapshots in the parent process
    }

    delete dut;
    delete tfp;
    delete contextp; // log system use this to get time
    return 0;
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
        snap->children_pid.push_front(child_pid);
        log_info("SNAPSHOT created, pid=%d", child_pid);
    } else { // for the fork-child snapshot process
        snap->is_child = true;
        sigset_t set, oldset;
        siginfo_t info;
        sigemptyset(&set);
        sigaddset(&set, SNAPSHOT_WAKEUP_SIGNAL);
        sigprocmask(SIG_BLOCK, &set, &oldset);   // Block SIG for using sigwait
        sigwaitinfo(&set, &info);                // Wait for snapshot-rollback
        sigprocmask(SIG_SETMASK, &oldset, NULL); // Change signal blocking mask back
        assert(info.si_signo == SNAPSHOT_WAKEUP_SIGNAL);
        snap->main_exit_time = (uint64_t)(info.si_value.sival_ptr);
        log_info("SNAPSHOT is activated, sim_time = %llu, origin process exited at time %d", contextp->time(),
            snap->main_exit_time);
        // delete tfp;             // Cannot do this, or it will block the process
        // (maybe because Vdut.fst was already closed in the parent process?)
        tfp = new VerilatedFstC(); // This will cause memory leak for once, but not serious. How to fix it?
        dut->trace(tfp, 99);
        tfp->open(g_config.snapshot.filename.c_str());
    }
}

// only used in the parent process, activating the oldest snapshot
void snapshot_rollback(uint64_t time) {
    if (!g_config.snapshot.enable || snapshots.is_child)
        return;
    log_info("SNAPSHOT rollback to %d time-unit ago, pid=%d",
        time % g_config.snapshot.time_interval + (snapshots.children_pid.size() - 1) * g_config.snapshot.time_interval,
        snapshots.children_pid.front());
    assert(sizeof(sigval_t) >= sizeof(contextp->time()));
    sigval_t sigval;
    sigval.sival_ptr = (void*)(contextp->time());

    pid_t child = snapshots.children_pid.back();     // Choose the oldest snapshot
    sigqueue(child, SNAPSHOT_WAKEUP_SIGNAL, sigval); // Activate the snapshot
    waitpid(child, NULL, 0);                         // Wait for snapshot finished
    snapshots.children_pid.pop_back();
}

// only used in the parent process, killing all remaining snapshot process
void snapshot_kill_all(snapshot_t* snap) {
    while (!snap->children_pid.empty()) {
        pid_t child = snap->children_pid.back();
        kill(child, SIGKILL);
        waitpid(child, NULL, 0);
        snap->children_pid.pop_back();
    }
    log_debug("All snapshot process are cleared, OK");
}

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
//extern "C" {
//uint64_t log_get_time() {
//    assert(contextp);
//    return contextp->time();
//}
//}

// Legacy function required only so linking works on Cygwin and MSVC++
double sc_time_stamp() { return 0; }

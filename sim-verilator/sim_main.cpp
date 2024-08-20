#include "Vdut.h"
#include "cta_sche_wrapper.hpp"
#include "kernel.hpp"
#include "log.h"
#include <cinttypes>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <functional>
#include <iostream>
#include <memory> // For std::unique_ptr
#include <new>
#include <vector>
#include <verilated.h>
#include <verilated_fst_c.h>

#ifndef SIM_WAVEFORM_FST
#define SIM_WAVEFORM_FST 0
#endif

uint64_t sim_time_max = 2000000;
#if (SIM_WAVEFORM_FST)
constexpr uint64_t SIM_WAVEFORM_TIME_BEGIN = 640000;
constexpr uint64_t SIM_WAVEFORM_TIME_END   = -1;
#endif
VerilatedFstC* tfp         = nullptr;
VerilatedContext* contextp = nullptr;
Vdut* dut                  = nullptr;

extern "C" {
uint64_t log_get_time() {
    assert(contextp);
    return contextp->time();
}
}

void waveform_dump(VerilatedFstC* tfp, VerilatedContext* contextp) {
#if (SIM_WAVEFORM_FST)
    assert(contextp && tfp);
    uint64_t time = contextp->time();
    if (time >= SIM_WAVEFORM_TIME_BEGIN && time <= SIM_WAVEFORM_TIME_END) {
        tfp->dump(time);
    }
#endif
}

// Legacy function required only so linking works on Cygwin and MSVC++
double sc_time_stamp() { return 0; }

void dut_reset(Vdut* dut, VerilatedContext* contextp, VerilatedFstC* tfp);
int parse_arg(
    std::vector<std::string> args, uint64_t& simtime, std::function<void(std::shared_ptr<Kernel>)> new_kernel);

int main(int argc, char** argv) {
    Verilated::mkdir("logs"); // Create logs/ directory in case we have traces to put under it

    // Verilator simulation context init
    contextp = new VerilatedContext;
    contextp->debug(0);     // debug level, 0 is off, 9 is highest, may be overridden by commandArgs parsing
    contextp->randReset(2); // Randomization reset policy, may be overridden by commandArgs argument parsing
    contextp->traceEverOn(true);

    // Hardware construct
    dut         = new Vdut(contextp, "DUT");
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
    parse_arg(args, sim_time_max,
        std::function<void(std::shared_ptr<Kernel>)>(std::bind(&Cta::kernel_add, &cta, std::placeholders::_1)));

#if (SIM_WAVEFORM_FST)
    // waveform traces (FST)
    tfp = new VerilatedFstC;
    dut->trace(tfp, 5);
    tfp->open("obj_dir/Vdut.fst");
#endif

    // DUT initial reset
    dut_reset(dut, contextp, tfp);

    while (!contextp->gotFinish() && contextp->time() < sim_time_max && !cta.is_idle()) {
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
        waveform_dump(tfp, contextp);

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
        // Clock output
        if (contextp->time() % 10000 == 0)
            log_debug("Simulation cycles: %lu", contextp->time());
    }

    log_info("Simulation finished in %d cycles", contextp->time());
#if (SIM_WAVEFORM_FST)
    tfp->close();
#endif
    dut->final();                  // Final model cleanup
    contextp->statsPrintSummary(); // Final simulation summary
    delete dut;
    delete contextp;
#if (SIM_WAVEFORM_FST)
    delete tfp;
#endif
    return 0;
}

void dut_reset(Vdut* dut, VerilatedContext* contextp, VerilatedFstC* tfp) {
    assert(dut && contextp);
    contextp->time(0);
    dut->io_host_req_valid = 0;
    dut->io_host_rsp_ready = 0;
    dut->reset             = 1;
    dut->clock             = 0;
    dut->eval();
    waveform_dump(tfp, contextp);

    contextp->timeInc(1);
    dut->clock = 1;
    dut->eval();
    waveform_dump(tfp, contextp);

    contextp->timeInc(1);
    dut->clock = 0;
    dut->eval();
    waveform_dump(tfp, contextp);

    contextp->timeInc(1);
    dut->clock = 1;
    dut->eval();
    waveform_dump(tfp, contextp);

    contextp->timeInc(1);
    dut->clock = 0;
    dut->reset = 0;
    dut->eval();
    waveform_dump(tfp, contextp);
}

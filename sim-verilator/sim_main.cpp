#include "Vdut.h"
#include "cta_sche_wrapper.hpp"
#include "kernel.hpp"
#include "log.h"
#include "testcase.hpp"
#include <cinttypes>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <iostream>
#include <memory> // For std::unique_ptr
#include <new>
#include <vector>
#include <verilated.h>
#include <verilated_fst_c.h>

#ifndef SIM_WAVEFORM_FST
#define SIM_WAVEFORM_FST 0
#endif
constexpr int CLK_MAX = 2000000;

// Legacy function required only so linking works on Cygwin and MSVC++
double sc_time_stamp() { return 0; }

void dut_reset(Vdut* dut, VerilatedContext* contextp
#if (SIM_WAVEFORM_FST)
    ,
    VerilatedFstC* tfp
#endif
);

int main(int argc, char** argv) {
    Verilated::mkdir("logs"); // Create logs/ directory in case we have traces to put under it

    // Verilator simulation context init
    const std::unique_ptr<VerilatedContext> contextp { new VerilatedContext };
    contextp->debug(0);     // debug level, 0 is off, 9 is highest, may be overridden by commandArgs parsing
    contextp->randReset(2); // Randomization reset policy, may be overridden by commandArgs argument parsing
    contextp->traceEverOn(true);
    contextp->commandArgs(argc, argv);

    // Hardware construct
    Vdut* dut   = new Vdut(contextp.get(), "DUT");
    MemBox* mem = new MemBox;
    Cta cta(mem);

#if (SIM_WAVEFORM_FST)
    // waveform traces (FST)
    VerilatedFstC* tfp = new VerilatedFstC;
    dut->trace(tfp, 5);
    tfp->open("obj_dir/Vdut.fst");
#endif

    // Load workload kernel
    cta.kernel_add(std::make_shared<Kernel>(tc_matadd.get_kernel(0)));
    //cta.kernel_add(std::make_shared<Kernel>(tc_vecadd.get_kernel(0)));
    //for (int i = 0; i < tc_gaussian.get_num_kernel(); i++) {
    //   cta.kernel_add(std::make_shared<Kernel>(tc_gaussian.get_kernel(i)));
    //}
    //for (int i = 0; i < tc_bfs.get_num_kernel(); i++) {
    //    cta.kernel_add(std::make_shared<Kernel>(tc_bfs.get_kernel(i)));
    //}

    // DUT initial reset
    dut_reset(dut, contextp.get()
#if (SIM_WAVEFORM_FST)
                       ,
        tfp
#endif
    );

    uint32_t clk_cnt = 0;
    log_set_timeptr(&clk_cnt);
    while (!contextp->gotFinish() && clk_cnt < CLK_MAX && !cta.is_idle()) {
        contextp->timeInc(1); // 1 timeprecision period passes...
        dut->clock = !dut->clock;
        clk_cnt += dut->clock ? 1 : 0;

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
#if (SIM_WAVEFORM_FST)
        if (clk_cnt >= 0)
            tfp->dump(contextp->time());
#endif

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
                log_debug(
                    "block%2d dispatched to GPU (kernel%2d %s block%2d) ", wg_id, kernel_id, kernel_name.c_str(), wg_idx);
                cta.wg_dispatched();
            }
            if (dut->io_host_rsp_valid && dut->io_host_rsp_ready) {
                uint32_t wg_id                       = dut->io_host_rsp_bits_inflight_wg_buffer_host_wf_done_wg_id;
                std::shared_ptr<const Kernel> kernel = cta.wg_finish(wg_id);
                assert(kernel);
                uint32_t wg_idx;
                kernel->is_wg_belonging(wg_id, &wg_idx);
                log_debug("block%2d finished (kernel%2d %s block%2d)", wg_id, kernel->get_kid(),
                    kernel->get_kname().c_str(), wg_idx);
                if (kernel->is_finished()) {
                    log_info("kernel%2d %s finished", kernel->get_kid(), kernel->get_kname().c_str());
                }
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
            // Clock output
            //    log_debug("Simulation cycles: %d", clk_cnt);
        }
    }

    log_info("Simulation finished in %d cycles", clk_cnt);
#if (SIM_WAVEFORM_FST)
    tfp->close();
#endif
    dut->final();                  // Final model cleanup
    contextp->statsPrintSummary(); // Final simulation summary
    delete dut;
    return 0;
}

void dut_reset(Vdut* dut, VerilatedContext* contextp
#if (SIM_WAVEFORM_FST)
    ,
    VerilatedFstC* tfp
#endif
) {
    dut->io_host_req_valid = 0;
    dut->io_host_rsp_ready = 0;
    dut->reset             = 1;
    dut->clock             = 0;
    dut->eval();
#if (SIM_WAVEFORM_FST)
    tfp->dump(contextp->time());
#endif
    contextp->timeInc(1);
    dut->clock = 1;
    dut->eval();
#if (SIM_WAVEFORM_FST)
    tfp->dump(contextp->time());
#endif
    contextp->timeInc(1);
    dut->clock = 0;
    dut->eval();
#if (SIM_WAVEFORM_FST)
    tfp->dump(contextp->time());
#endif
    contextp->timeInc(1);
    dut->clock = 1;
    dut->eval();
#if (SIM_WAVEFORM_FST)
    tfp->dump(contextp->time());
#endif
    contextp->timeInc(1);
    dut->clock = 0;
    dut->reset = 0;
    dut->eval();
#if (SIM_WAVEFORM_FST)
    tfp->dump(contextp->time());
#endif
}

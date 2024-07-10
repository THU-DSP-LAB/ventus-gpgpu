#include "Vdut.h"
#include "kernel.hpp"
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
#define SIM_WAVEFORM_FST 1
#endif

// Legacy function required only so linking works on Cygwin and MSVC++
double sc_time_stamp() { return 0; }

class Cta {
public:
    Cta()
        : valid(false) {};

    bool get_new(const Kernel& kernel) {
        valid = !kernel.no_more_ctas_to_run();
        if (valid) {
            id                    = kernel.get_next_cta_id_single();
            dim3_t kernel_size_3d = kernel.get_next_cta_id();
            num_wg_x              = kernel_size_3d.x;
            num_wg_y              = kernel_size_3d.y;
            num_wg_z              = kernel_size_3d.z;
            num_wf                = kernel.get_num_wf();
            num_thread            = kernel.get_num_thread();
            num_sgpr_per_thread   = kernel.get_num_sgpr_per_thread();
            num_vgpr_per_thread   = kernel.get_num_vgpr_per_thread();
            num_lds               = kernel.get_num_lds();
            num_sgpr              = num_sgpr_per_thread * num_wf;
            num_vgpr              = num_vgpr_per_thread * num_wf;
            start_pc              = kernel.get_start_pc();
            csr_baseaddr          = kernel.get_csr_baseaddr();
            pds_baseaddr          = kernel.get_gds_baseaddr();
            gds_baseaddr          = kernel.get_gds_baseaddr();
        }
        return valid;
    }

    void apply_to_dut(Vdut& dut) {
        dut.io_host_req_valid = valid;
        if (!valid)
            return;
        dut.io_host_req_bits_host_wg_id            = id;
        dut.io_host_req_bits_host_kernel_size_3d_0 = num_wg_x;
        dut.io_host_req_bits_host_kernel_size_3d_1 = num_wg_y;
        dut.io_host_req_bits_host_kernel_size_3d_2 = num_wg_z;
        dut.io_host_req_bits_host_num_wf           = num_wf;
        dut.io_host_req_bits_host_wf_size          = num_thread;
        dut.io_host_req_bits_host_lds_size_total   = num_lds;
        dut.io_host_req_bits_host_sgpr_size_total  = num_sgpr;
        dut.io_host_req_bits_host_vgpr_size_total  = num_vgpr;
        dut.io_host_req_bits_host_sgpr_size_per_wf = num_sgpr_per_thread;
        dut.io_host_req_bits_host_vgpr_size_per_wf = num_vgpr_per_thread;
        dut.io_host_req_bits_host_start_pc         = start_pc;
        dut.io_host_req_bits_host_csr_knl          = csr_baseaddr;
        dut.io_host_req_bits_host_pds_baseaddr     = pds_baseaddr;
        dut.io_host_req_bits_host_gds_baseaddr     = gds_baseaddr;
        dut.io_host_req_bits_host_gds_size_total   = 0; // useless
    }

private:
    // CTA info
    bool valid;
    uint32_t id;
    uint32_t num_wg_x;
    uint32_t num_wg_y;
    uint32_t num_wg_z;
    uint32_t num_wf;
    uint32_t num_thread;
    uint32_t num_lds;
    uint32_t num_sgpr;
    uint32_t num_vgpr;
    uint32_t num_sgpr_per_thread;
    uint32_t num_vgpr_per_thread;
    uint32_t start_pc;
    uint32_t csr_baseaddr;
    uint32_t pds_baseaddr;
    uint32_t gds_baseaddr;
};

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
    const std::unique_ptr<Vdut> dut { new Vdut { contextp.get(), "DUT" } };
    MemBox* mem = new MemBox;

#if (SIM_WAVEFORM_FST)
    // waveform traces (FST)
    VerilatedFstC* tfp = new VerilatedFstC;
    dut->trace(tfp, 5);
    tfp->open("obj_dir/Vdut.fst");
#endif

    // Load workload kernel
    // Kernel kernel1("kernel1", "testcase/matadd/matadd.metadata", "testcase/matadd/matadd.data", *mem);
    Kernel kernel1 = tc_matadd.get_kernel(0, *mem);
    // Kernel kernel1 = tc_vecadd.get_kernel(0, *mem);
    Cta cta;

    // DUT initial reset
    dut_reset(dut.get(), contextp.get()
#if (SIM_WAVEFORM_FST)
                             ,
        tfp
#endif
    );

    constexpr int CLK_MAX = 10000;
    int clk_cnt           = 0;
    while (!contextp->gotFinish() && clk_cnt < CLK_MAX && !kernel1.kernel_finished()) {
        contextp->timeInc(1); // 1 timeprecision period passes...
        dut->clock = !dut->clock;
        clk_cnt += dut->clock ? 1 : 0;

        //
        // Input stimulus apply to hardware
        //

        dut->io_host_rsp_ready = 1;
        if (dut->clock == 0) {
            // Host WG new
            if (cta.get_new(kernel1)) {
                cta.apply_to_dut(*dut);
            } else {
                dut->io_host_req_valid = false;
            }
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
        // React to new output & prepare for new input stimulus
        //
        if (dut->clock == 0) {
            if (dut->io_host_req_valid && dut->io_host_req_ready) {
                int cta_id = dut->io_host_req_bits_host_wg_id;
                std::cout << "CTA " << cta_id << " dispatched to GPU\n";
                kernel1.increment_cta_id();
            }
            if (dut->io_host_rsp_valid && dut->io_host_rsp_ready) {
                int cta_id = dut->io_host_rsp_bits_inflight_wg_buffer_host_wf_done_wg_id;
                std::cout << "CTA " << cta_id << " finished\n";
                kernel1.cta_finish(cta_id);
            }
        } else {
            // Memory access: read
            if (dut->io_mem_rd_en) {
                volatile uint32_t rd_addr = dut->io_mem_rd_addr;
                uint8_t* rd_data          = new uint8_t[MEMACCESS_DATA_BYTE_SIZE];
                if (!mem->read(dut->io_mem_rd_addr, rd_data)) {
                    //std::cerr << "Read uninitialized memory: 0x" << std::hex << dut->io_mem_rd_addr << std::dec
                    //          << " @ time " << clk_cnt << std::endl;
                    //getchar();
                    //break;
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
            if (clk_cnt % 5000 == 0)
                std::cout << "Simulation cycles: " << clk_cnt << std::endl;
        }
    }

    std::cout << "Simulation finished in " << clk_cnt << " cycles\n";
#if (SIM_WAVEFORM_FST)
    tfp->close();
#endif
    dut->final();                  // Final model cleanup
    contextp->statsPrintSummary(); // Final simulation summary
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

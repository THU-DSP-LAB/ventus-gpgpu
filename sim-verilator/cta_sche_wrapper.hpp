#pragma once
#include "MemBox.hpp"
#include "Vdut.h"
#include "kernel.hpp"
#include <memory>
#include <vector>

class Cta {
public:
    Cta(MemBox* mem);

    bool apply_to_dut(Vdut* dut); // DUT WG new IO port stimuli
    void wg_dispatched();

    void kernel_add(std::shared_ptr<Kernel> kernel);

    std::shared_ptr<Kernel> wg_finish(uint32_t wgid);

    bool is_idle() const;

    /*
    bool get_new(const Kernel& kernel) {
        valid = !kernel.no_more_wg_to_dispatch();
        if (valid) {
            uint32_t id_in_kernel = kernel.get_next_wg_idx_in_kernel();
            id                    = id_in_kernel;
            dim3_t kernel_size_3d = kernel.get_next_wg_idx3d_in_kernel();
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
            gds_baseaddr          = kernel.get_gds_baseaddr();
            pds_baseaddr          = kernel.get_pds_baseaddr()
                + id_in_kernel * kernel.get_num_wf() * kernel.get_num_thread() * kernel.get_num_pds_per_thread();
        }
        return valid;
    }

    void apply_to_dut_bak(Vdut& dut) {
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
    */

private:
    /*
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
    */

    std::vector<std::shared_ptr<Kernel>> m_kernels;
    int m_kernel_idx_dispatching;
    uint32_t m_kernel_id_next;
    uint32_t m_kernel_wgid_base_next;
    MemBox* m_mem;
};

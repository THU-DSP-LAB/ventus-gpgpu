#include "cta_sche_wrapper.hpp"
#include "MemBox.hpp"
#include "kernel.hpp"
#include <cassert>
#include <memory>
#include "log.h"

Cta::Cta(MemBox* mem)
    : m_kernel_idx_dispatching(-1)
    , m_kernel_id_next(0)
    , m_kernel_wgid_base_next(0)
    , m_mem(mem) {};

void Cta::kernel_add(std::shared_ptr<Kernel> kernel) {
    assert(kernel && !kernel->is_running());
    m_kernels.push_back(kernel);
    //log_debug("CTA_sche receive Kernel %s", kernel->get_kname().c_str());
}

void Cta::wg_dispatched() {
    assert(m_kernel_idx_dispatching >= 0 && m_kernel_idx_dispatching < m_kernels.size());
    std::shared_ptr<Kernel> kernel = m_kernels[m_kernel_idx_dispatching];
    assert(kernel);
    kernel->wg_dispatched();
}

bool Cta::wg_get_info(std::string& kernel_name, uint32_t &kernel_id, uint32_t& wg_idx_in_kernel) {
    assert(m_kernel_idx_dispatching < 0 || m_kernel_idx_dispatching < m_kernels.size());
    std::shared_ptr<Kernel> kernel = (m_kernel_idx_dispatching == -1) ? nullptr : m_kernels[m_kernel_idx_dispatching];
    assert(m_kernel_idx_dispatching == -1 || kernel);
    
    if (kernel == nullptr || !kernel->is_dispatching()) {
        return false;
    }

    kernel_name = kernel->get_kname();
    kernel_id = kernel->get_kid();
    wg_idx_in_kernel = kernel->get_next_wg_idx_in_kernel();
    return true;
}

bool Cta::is_idle() const { return m_kernels.size() == 0; }

bool Cta::apply_to_dut(Vdut* dut) {
    assert(m_kernel_idx_dispatching < 0 || m_kernel_idx_dispatching < m_kernels.size());
    std::shared_ptr<Kernel> kernel = (m_kernel_idx_dispatching == -1) ? nullptr : m_kernels[m_kernel_idx_dispatching];
    assert(m_kernel_idx_dispatching == -1 || kernel);

    // 当前kernel分派结束后，切换到下一个kernel
    if (kernel == nullptr || !kernel->is_dispatching()) {
        // TODO: 暂未实现虚拟内存，目前需要等待当前kernel完全结束才能开始分派下一个kernel
        // if (m_kernels.size() == m_kernel_idx_dispatching + 1) { // 无下一个kernel，不分派
        if (m_kernels.size() == m_kernel_idx_dispatching + 1 || (kernel && !kernel->is_finished())) {
            // 需等待当前kernel或者无下一个kernel，不分派WG
            dut->io_host_req_valid = false;
            return false;
        } else {
            // 激活下一个kernel，准备分派其线程块
            kernel = m_kernels[++m_kernel_idx_dispatching];
            assert(kernel && !kernel->is_activated() && !kernel->is_finished());
            assert(m_kernel_wgid_base_next <= 0xEFFFFFFF); // 当前实现中线程块ID不会回收，需防止其溢出
            kernel->activate(m_kernel_id_next++, m_kernel_wgid_base_next, m_mem);
            m_kernel_wgid_base_next += kernel->get_num_wg();
        }
    }

    // Get WG to dispatch and apply to DUT
    if (kernel && kernel->is_dispatching()) {
        dut->io_host_req_valid                      = true;
        uint32_t idx_in_kernel                      = kernel->get_next_wg_idx_in_kernel();
        dim3_t kernel_size_3d                       = kernel->get_num_wg_3d();
        dut->io_host_req_bits_host_wg_id            = kernel->get_next_wgid();
        dut->io_host_req_bits_host_kernel_size_3d_0 = kernel_size_3d.x;
        dut->io_host_req_bits_host_kernel_size_3d_1 = kernel_size_3d.y;
        dut->io_host_req_bits_host_kernel_size_3d_2 = kernel_size_3d.z;
        dut->io_host_req_bits_host_num_wf           = kernel->get_num_wf();
        dut->io_host_req_bits_host_wf_size          = kernel->get_num_thread();
        dut->io_host_req_bits_host_lds_size_total   = kernel->get_num_lds();
        dut->io_host_req_bits_host_sgpr_size_total  = kernel->get_num_sgpr_per_wf() * kernel->get_num_wf();
        dut->io_host_req_bits_host_vgpr_size_total  = kernel->get_num_vgpr_per_wf() * kernel->get_num_wf();
        dut->io_host_req_bits_host_sgpr_size_per_wf = kernel->get_num_sgpr_per_wf();
        dut->io_host_req_bits_host_vgpr_size_per_wf = kernel->get_num_vgpr_per_wf();
        dut->io_host_req_bits_host_pds_size_per_wf  = kernel->get_num_pds_per_thread() * kernel->get_num_thread();
        dut->io_host_req_bits_host_start_pc         = kernel->get_start_pc();
        dut->io_host_req_bits_host_csr_knl          = kernel->get_csr_baseaddr();
        dut->io_host_req_bits_host_gds_baseaddr     = kernel->get_gds_baseaddr();
        dut->io_host_req_bits_host_pds_baseaddr     = kernel->get_pds_baseaddr()
            + idx_in_kernel * kernel->get_num_wf() * kernel->get_num_thread() * kernel->get_num_pds_per_thread();
        dut->io_host_req_bits_host_gds_size_total = 0; // useless
        return true;
    }
    assert(0);
}

std::shared_ptr<const Kernel> Cta::wg_finish(uint32_t wgid) {
    for (auto it = m_kernels.begin(); it != m_kernels.end(); it++) {
        std::shared_ptr<Kernel> kernel = *it;
        if (kernel->is_running() && kernel->is_wg_belonging(wgid)) { // 寻找wg所属kernel
            assert(it <= m_kernels.begin() + m_kernel_idx_dispatching);
            kernel->wg_finish(wgid);
            if (kernel->is_finished()) { // 整个kernel已经结束，删除之
                kernel->deactivate(m_mem);
                m_kernels.erase(it);
                m_kernel_idx_dispatching--; // 可能会减至-1
            }
            return kernel;
        }
    }
    assert(0); // 总应当可以找到WG所属的Kernel
}

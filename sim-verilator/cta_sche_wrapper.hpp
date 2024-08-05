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

    // 获取当前正在向GPU分派的线程块信息
    // 若当前未在分派线程块，return false
    // 否则return true，并将线程块信息存储在两个引用参数中
    bool wg_get_info(std::string& kernel_name, uint32_t &kernel_id, uint32_t& wg_idx_in_kernel);

    void kernel_add(std::shared_ptr<Kernel> kernel);

    std::shared_ptr<const Kernel> wg_finish(uint32_t wgid);

    bool is_idle() const;

private:
    std::vector<std::shared_ptr<Kernel>> m_kernels;
    int m_kernel_idx_dispatching;
    uint32_t m_kernel_id_next;
    uint32_t m_kernel_wgid_base_next;
    MemBox* m_mem;
};

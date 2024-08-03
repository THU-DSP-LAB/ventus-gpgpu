#include "testcase.hpp"
#include "MemBox.hpp"
#include <cstdint>

const Testcase tc_matadd("matadd", 1, "matadd");
const Testcase tc_vecadd("vecadd", 1, "vecadd");

Testcase::Testcase(const char* name, uint32_t num_kernel_, ...)
    : testcase_name(name)
    , num_kernel(num_kernel_) {
    va_list args;
    va_start(args, num_kernel_);
    kernel_name.reserve(num_kernel_);
    for (uint32_t i = 0; i < num_kernel_; i++) {
        const char* kernel_name_ptr = va_arg(args, char*);
        kernel_name.push_back(std::string(kernel_name_ptr));
    }
}

Kernel Testcase::get_kernel(int kernel_idx) const {
    Kernel k(kernel_name[kernel_idx],
        std::string(TESTCASE_DIR) + kernel_name[kernel_idx] + std::string("/") + kernel_name[kernel_idx]
            + std::string(".metadata"),
        std::string(TESTCASE_DIR) + kernel_name[kernel_idx] + std::string("/") + kernel_name[kernel_idx]
            + std::string(".data"));
    return k;
}

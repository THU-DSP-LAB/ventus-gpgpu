#include "testcase.hpp"
#include "MemBox.hpp"

const Testcase tc_matadd("matadd", 1, "matadd");
const Testcase tc_vecadd("vecadd", 1, "vecadd");

Testcase::Testcase(const char* name, int num_kernel_, ...)
    : testcase_name(name)
    , num_kernel(num_kernel_) {
    va_list args;
    va_start(args, num_kernel_);
    kernel_name.reserve(num_kernel_);
    for (int i = 0; i < num_kernel_; i++) {
        const char* kernel_name_ptr = va_arg(args, char*);
        kernel_name.push_back(std::string(kernel_name_ptr));
    }
}

Kernel Testcase::get_kernel(int id, MemBox& mem) const {
    Kernel k(kernel_name[id],
        std::string(TESTCASE_DIR) + kernel_name[id] + std::string("/") + kernel_name[id] + std::string(".metadata"),
        std::string(TESTCASE_DIR) + kernel_name[id] + std::string("/") + kernel_name[id] + std::string(".data"), mem);
    return k;
}

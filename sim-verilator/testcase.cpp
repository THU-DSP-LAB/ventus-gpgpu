#include "testcase.hpp"
#include "MemBox.hpp"
#include <cstdint>

const Testcase tc_matadd("matadd", 1, "matadd");
const Testcase tc_vecadd("vecadd", 1, "vecadd");
const Testcase tc_bfs("bfs", 12,
        "BFS_1_0", "BFS_2_0",
        "BFS_1_1", "BFS_2_1",
        "BFS_1_2", "BFS_2_2",
        "BFS_1_3", "BFS_2_3",
        "BFS_1_4", "BFS_2_4",
        "BFS_1_5", "BFS_2_5" 
        );
const Testcase tc_gaussian("gaussian", 6,
        "Fan1_0", "Fan2_0",
        "Fan1_1", "Fan2_1",
        "Fan1_2", "Fan2_2"
        );

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
        std::string(TESTCASE_DIR) + testcase_name + std::string("/") + kernel_name[kernel_idx]
            + std::string(".metadata"),
        std::string(TESTCASE_DIR) + testcase_name + std::string("/") + kernel_name[kernel_idx]
            + std::string(".data"));
    return k;
}

#pragma once

#include "MemBox.hpp"
#include "kernel.hpp"
#include <cstdarg>
#include <cstdint>
#include <string>
#include <vector>

constexpr char TESTCASE_DIR[] = "testcase/";

class Testcase {
private:
    std::string testcase_name;
    int num_kernel;
    std::vector<std::string> kernel_name;

public:
    Testcase(const char* name, uint32_t num_kernel_, ...);
    Kernel get_kernel(int kernel_idx) const;
};

extern const Testcase tc_matadd;
extern const Testcase tc_vecadd;


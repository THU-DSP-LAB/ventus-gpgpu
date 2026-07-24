#include "kernel.hpp"
#include "ventus_rtlsim.h"
#include <cassert>
#include <cstring>
#include <fmt/core.h>
#include <fstream>
#include <memory>
#include <spdlog/spdlog.h>
#include <utility>
#include <vector>

extern int parse_arg(
    std::vector<std::string> args, ventus_rtlsim_config_t* config,
    std::function<void(std::shared_ptr<Kernel>)> new_kernel, std::vector<std::pair<paddr_t, paddr_t>>* dumpmem_ranges
);

typedef struct {
    std::filesystem::path datafile;
    ventus_rtlsim_t* sim;
} kernel_load_data_callback_t;

void kernel_load_data_callback(const metadata_t* metadata);

int main(int argc, char* argv[]) {
    spdlog::set_level(spdlog::level::trace);
    const char* verilator_argv[] = {
        "+verilator+seed+10086",
    };

    //
    // simulation config
    //
    ventus_rtlsim_config_t sim_config;
    ventus_rtlsim_get_default_config(&sim_config);

    sim_config.log.console.level = "trace";
    sim_config.sim_time_max = 200000;
    sim_config.pmem.auto_alloc = true;
    sim_config.waveform.enable = false;
    sim_config.waveform.time_begin = 0;
    sim_config.waveform.time_end = -1;
    sim_config.verilator.argc = sizeof(verilator_argv) / sizeof(verilator_argv[0]);
    sim_config.verilator.argv = verilator_argv;
    ventus_rtlsim_config_t sim_config_1 = sim_config; // not needed
    std::vector<std::pair<paddr_t, paddr_t>> dumpmem_ranges;

    // parse cmdline args, set sim-config
    std::vector<std::string> args;
    if (argc == 1) { // Default arguments
        puts("[Info] using default cmdline arguments: -f ventus_args.txt");
        args.push_back("-f");
        args.push_back("ventus_args.txt");
    } else {
        for (int i = 1; i < argc; i++) {
            args.push_back(argv[i]);
        }
    }
    parse_arg(args, &sim_config, nullptr, nullptr);

    //
    // init Ventus RTLSIM
    //
    ventus_rtlsim_t* sim = ventus_rtlsim_init(&sim_config);

    // parse cmdline arguments, generate kernels, and add them to Ventus RTLSIM
    std::vector<std::shared_ptr<Kernel>> kernels;
    std::function<void(std::shared_ptr<Kernel>)> f_new_kernel = [sim, &kernels](std::shared_ptr<Kernel> kernel) {
        metadata_t metadata = *kernel->get_metadata();
        metadata.data = new kernel_load_data_callback_t { .datafile = kernel->m_datafile, .sim = sim };
        ventus_rtlsim_add_kernel__delay_data_loading(sim, &metadata, kernel_load_data_callback, nullptr);
        kernels.push_back(std::move(kernel));
    };
    parse_arg(args, &sim_config_1, f_new_kernel, &dumpmem_ranges);

    //
    // Run simulation, each step stands for 1 simulation time-unit
    //
    const ventus_rtlsim_step_result_t* result;
    while (1) {
        result = ventus_rtlsim_step(sim);
        if (result->error || result->idle || result->time_exceed || result->hang) {
            break;
        }
    }

    //
    // Finish simulation, release resources
    //
    if (!result->error && !result->time_exceed && !result->hang && result->idle) {
        for (int i = 0; i < 10000; i++) {
            ventus_rtlsim_step(sim); // 额外运行一会儿，等待缓存invalidate结束
        }
    }
    for (const auto& range : dumpmem_ranges) { // 命令行参数要求输出的内存内容，每4字节输出1行
        paddr_t begin = range.first;
        paddr_t end = range.second;
        size_t len = (end - begin + 7) / 4 * 4;
        std::unique_ptr<uint8_t[]> buffer = std::make_unique<uint8_t[]>(len);
        if (!ventus_rtlsim_pmemcpy_d2h(sim, buffer.get(), begin, len)) {
            spdlog::error("Failed to dump physical memory [0x{:#x}, 0x{:#x}]", begin, end);
            continue;
        }
        for (paddr_t addr = begin; addr <= end; addr += 4) {
            uint32_t word = *(uint32_t*)(buffer.get() + (addr - begin));
            fmt::print("dump-mem: mem[0x{:08X} +: 4] = 0x{:08X}\n", addr, word);
        }
    }
    ventus_rtlsim_finish(sim, false);

    return 0;
}

void kernel_load_data_callback(const metadata_t* metadata) {
    kernel_load_data_callback_t* cb_data = (kernel_load_data_callback_t*)metadata->data;
    std::ifstream file(cb_data->datafile);
    if (!file.is_open()) {
        spdlog::critical("Failed to open .data file: {}", cb_data->datafile.c_str());
        assert(0);
    }

    std::string line;
    int bufferIndex = 0;
    std::vector<uint8_t> buffer;
    for (int bufferIndex = 0; bufferIndex < metadata->num_buffer; bufferIndex++) {
        assert(metadata->buffer_size[bufferIndex] % 4 == 0);
        buffer.reserve(metadata->buffer_size[bufferIndex]); // 提前分配空间
        int readbytes = 0;
        while (readbytes < metadata->buffer_size[bufferIndex]) {
            std::getline(file, line);
            for (int i = line.length(); i > 0; i -= 2) {
                std::string hexChars = line.substr(i - 2, 2);
                uint8_t byte = std::stoi(hexChars, nullptr, 16);
                buffer.push_back(byte);
            }
            readbytes += 4;
        }
        ventus_rtlsim_pmemcpy_h2d(cb_data->sim, metadata->buffer_base[bufferIndex], buffer.data(), readbytes);
        buffer.clear();
    }
    std::getline(file, line);
    for (const char* ptr = line.c_str(); *ptr != '\0'; ptr++) {
        assert(*ptr == '0');
    }
    assert(file.eof());

    file.close();
    spdlog::trace(fmt::format("kernel{} {} data loaded from file", metadata->kernel_id, metadata->name));
}

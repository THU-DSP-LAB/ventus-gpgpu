#include "kernel.hpp"
#include "log.h"
#include "ventus_rtlsim.h"
#include <cassert>
#include <cstring>
#include <fstream>
#include <memory>

extern int parse_arg(std::vector<std::string> args, std::function<void(std::shared_ptr<Kernel>)> new_kernel);

typedef struct {
    std::filesystem::path datafile;
    ventus_rtlsim_t* sim;
} kernel_load_data_callback_t;

uint64_t (*log_get_time)() = nullptr;
static ventus_rtlsim_t* g_sim = nullptr;
uint64_t log_get_time_callback() {
    return ventus_rtlsim_get_time(g_sim);
}

void kernel_load_data_callback(const metadata_t* metadata) {
    kernel_load_data_callback_t* cb_data = (kernel_load_data_callback_t*)metadata->data;
    std::ifstream file(cb_data->datafile);
    if (!file.is_open()) {
        log_fatal("Failed to open .data file: %s", cb_data->datafile.c_str());
        assert(0);
    }

    std::string line;
    int bufferIndex = 0;
    std::vector<uint8_t> buffer;
    for (int bufferIndex = 0; bufferIndex < metadata->num_buffer; bufferIndex++) {
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
    log_trace(
        "kernel%2d %s data loaded from file", metadata->kernel_id, metadata->kernel_name, cb_data->datafile.c_str());
}

int main_new(int argc, char* argv[]) {
    const char* verilator_argv[] = {
        "+verilator+seed+10086",
    };

    ventus_rtlsim_config_t sim_config = {
        .sim_time_max = 800000,
        .pmem = { .pagesize = 4096, .auto_alloc = true },
        .waveform
        = { .enable = true, .time_begin = 20000, .time_end = 30000, .levels = 99, .filename = "obj_dir/Vdut.fst" },
        .snapshot = { .enable = true, .time_interval = 50000, .num_max = 2, .filename = "obj_dir/Vdut.snapshot.fst" },
        .verilator_argc = sizeof(verilator_argv) / sizeof(verilator_argv[0]),
        .verilator_argv = verilator_argv,
    };
    ventus_rtlsim_t* sim = ventus_rtlsim_init(&sim_config);
    g_sim = sim;
    log_get_time = log_get_time_callback;

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
    std::function<void(std::shared_ptr<Kernel>)> f_new_kernel = [sim](std::shared_ptr<Kernel> kernel) {
        metadata_t metadata = *kernel->get_metadata();
        metadata.data = new kernel_load_data_callback_t { .datafile = kernel->m_datafile, .sim = sim };
        ventus_rtlsim_add_kernel__delay_data_loading(sim, &metadata, kernel_load_data_callback, nullptr);
    };
    parse_arg(args, f_new_kernel);

    const ventus_rtlsim_step_result_t* result;
    while (1) {
        result = ventus_rtlsim_step(sim);
        if (result->error || result->idle || result->time_exceed) {
            break;
        }
    }

    ventus_rtlsim_finish(sim, false);

    return 0;
}
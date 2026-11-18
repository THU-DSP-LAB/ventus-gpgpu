#include "ventus_rtlsim.h"
#include <cstdint>
#include <filesystem>
#include <iostream>
#include <stdexcept>
#include <string>
#include <unistd.h>

namespace {
void require(bool condition, const std::string& message) {
    if (!condition) {
        throw std::runtime_error(message);
    }
}

ventus_rtlsim_config_t test_config(const std::filesystem::path& fst_path) {
    ventus_rtlsim_config_t config;
    ventus_rtlsim_get_default_config(&config);
    config.sim_time_max = 1000;
    config.log.console.enable = false;
    config.log.file.enable = false;
    config.pmem.auto_alloc = true;
    config.waveform.enable = false;
    config.snapshot.enable = true;
    config.snapshot.time_interval = 50;
    config.snapshot.num_max = 1;
    static std::string filename;
    filename = fst_path.string();
    config.snapshot.filename = filename.c_str();
    return config;
}

void test_invalid_configs() {
    auto config = test_config("/tmp/ventus-fork-invalid.fst");
    config.snapshot.time_interval = 0;
    require(ventus_rtlsim_init(&config) == nullptr, "zero snapshot interval was accepted");

    config = test_config("/tmp/ventus-fork-invalid.fst");
    config.snapshot.num_max = 0;
    require(ventus_rtlsim_init(&config) == nullptr, "zero snapshot count was accepted");

    config = test_config("/tmp/ventus-fork-invalid.fst");
    config.snapshot.filename = "";
    require(ventus_rtlsim_init(&config) == nullptr, "empty snapshot filename was accepted");

    config = test_config("/tmp/ventus-fork-collision.fst");
    config.waveform.enable = true;
    config.waveform.filename = config.snapshot.filename;
    require(ventus_rtlsim_init(&config) == nullptr, "waveform filename collision was accepted");
}

void step_until(ventus_rtlsim_t* sim, uint64_t target) {
    while (ventus_rtlsim_get_time(sim) < target) {
        const auto* result = ventus_rtlsim_step(sim);
        require(result != nullptr && !result->error && !result->time_exceed, "simulation step failed");
    }
}

void test_replay(bool induce_mismatch) {
    const pid_t owner_pid = getpid();
    const std::filesystem::path fst_path
        = "/tmp/ventus-fork-" + std::to_string(owner_pid)
        + (induce_mismatch ? "-mismatch.fst" : "-success.fst");
    std::filesystem::remove(fst_path);
    auto config = test_config(fst_path);
    ventus_rtlsim_t* sim = ventus_rtlsim_init(&config);
    require(sim != nullptr, "valid snapshot config was rejected");

    constexpr paddr_t address = 0x90000000;
    constexpr uint64_t before = 0x1122334455667788;
    constexpr uint64_t after = 0x8877665544332211;
    require(ventus_rtlsim_pmemcpy_h2d(sim, address, &before, sizeof(before)), "initial PMEM write failed");
    step_until(sim, 50);

    const bool is_replay = getpid() != owner_pid;
    if (is_replay) {
        uint64_t observed = 0;
        require(ventus_rtlsim_pmemcpy_d2h(sim, &observed, address, sizeof(observed)), "PMEM read failed");
        require(observed == before, "fork snapshot did not preserve PMEM");
    } else {
        require(ventus_rtlsim_pmemcpy_h2d(sim, address, &after, sizeof(after)), "parent PMEM write failed");
    }

    step_until(sim, is_replay && induce_mismatch ? 90 : 95);
    const int result = ventus_rtlsim_finish_checked(sim, true);
    if (is_replay) {
        throw std::runtime_error("snapshot child returned from finish_checked");
    }
    if (induce_mismatch) {
        require(result != 0, "replay-time mismatch was not propagated to the parent");
    } else {
        require(result == 0, "matching snapshot replay failed");
        require(std::filesystem::exists(fst_path) && std::filesystem::file_size(fst_path) > 0,
                "snapshot replay did not produce an FST");
    }
    std::filesystem::remove(fst_path);
}
} // namespace

int main(int argc, char** argv) {
    try {
        require(argc == 2, "usage: fork_snapshot_test invalid|success|mismatch");
        const std::string mode = argv[1];
        if (mode == "invalid") {
            test_invalid_configs();
        } else if (mode == "success") {
            test_replay(false);
        } else if (mode == "mismatch") {
            test_replay(true);
        } else {
            throw std::runtime_error("unknown test mode");
        }
        std::cout << "fork snapshot " << mode << " test passed\n";
        return 0;
    } catch (const std::exception& error) {
        std::cerr << "fork snapshot test failed: " << error.what() << '\n';
        return 1;
    }
}

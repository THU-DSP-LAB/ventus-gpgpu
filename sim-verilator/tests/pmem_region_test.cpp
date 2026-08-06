#include "ventus_rtlsim.h"

#include <array>
#include <cstdint>
#include <filesystem>
#include <fstream>
#include <iostream>
#include <iterator>
#include <sstream>
#include <stdexcept>
#include <string>

namespace {
constexpr paddr_t kBufferBase = 0x90000000;
constexpr paddr_t kPdsBase = 0x90001000;
constexpr uint64_t kPageSize = 4096;
constexpr const char* kLogFile = "/tmp/ventus-pmem-region-test.log";

void require(bool condition, const std::string& message) {
    if (!condition) throw std::runtime_error(message);
}

ventus_rtlsim_config_t test_config() {
    ventus_rtlsim_config_t config;
    ventus_rtlsim_get_default_config(&config);
    config.log.console.enable = false;
    config.log.file.enable = true;
    config.log.file.level = "trace";
    config.log.file.filename = kLogFile;
    config.log.level = "trace";
    config.pmem.auto_alloc = true;
    config.waveform.enable = false;
    config.snapshot.enable = false;
    return config;
}

void require_zero(const std::array<uint8_t, 16>& data, const std::string& message) {
    for (const uint8_t byte : data) {
        require(byte == 0, message);
    }
}

void require_log_entry(
    const std::string& log, const std::string& level, const std::string& message) {
    std::istringstream lines(log);
    for (std::string line; std::getline(lines, line);) {
        if (line.find(level) != std::string::npos
            && line.find(message) != std::string::npos) {
            return;
        }
    }
    throw std::runtime_error(
        "missing " + level + " log entry containing: " + message);
}
} // namespace

int main() {
    try {
        std::filesystem::remove(kLogFile);
        auto config = test_config();
        ventus_rtlsim_t* sim = ventus_rtlsim_init(&config);
        require(sim != nullptr, "simulator initialization failed");

        require(
            ventus_rtlsim_pmem_region_register(
                sim, kBufferBase, 64, kPageSize,
                VENTUS_PMEM_REGION_BUFFER, 1),
            "buffer region registration failed");
        require(
            ventus_rtlsim_pmem_region_register(
                sim, kBufferBase, 64, kPageSize,
                VENTUS_PMEM_REGION_BUFFER, 1),
            "idempotent buffer registration failed");
        require(
            !ventus_rtlsim_pmem_region_register(
                sim, kBufferBase + 128, 64, kPageSize,
                VENTUS_PMEM_REGION_BUFFER, 2),
            "overlapping region was accepted");
        require(
            ventus_rtlsim_pmem_region_register(
                sim, kPdsBase, 128, kPageSize,
                VENTUS_PMEM_REGION_PDS, 2),
            "PDS region registration failed");

        std::array<uint8_t, 16> data {};
        require(
            !ventus_rtlsim_pmemcpy_d2h(sim, data.data(), kBufferBase, data.size()),
            "cold buffer read unexpectedly succeeded");
        require_zero(data, "cold buffer read was not zero-filled");

        require(
            !ventus_rtlsim_pmemcpy_d2h(
                sim, data.data(), kBufferBase + 128, data.size()),
            "allocation-padding read unexpectedly succeeded");
        require(
            !ventus_rtlsim_pmemcpy_d2h(sim, data.data(), kPdsBase, data.size()),
            "cold PDS read unexpectedly succeeded");
        require(
            !ventus_rtlsim_pmemcpy_d2h(
                sim, data.data(), kPdsBase + kPageSize, data.size()),
            "OOB read unexpectedly succeeded");

        auto stats = ventus_rtlsim_pmem_missing_read_stats(sim);
        require(stats.cold_page == 1, "cold buffer read was misclassified");
        require(stats.pds_cold_page == 1, "cold PDS read was misclassified");
        require(stats.allocation_padding == 1, "padding read was misclassified");
        require(stats.out_of_bounds == 1, "OOB read was misclassified");

        require(
            !ventus_rtlsim_pmem_region_unregister(sim, kBufferBase, 99),
            "wrong allocation id unregistered a region");
        require(
            ventus_rtlsim_pmem_region_unregister(sim, kBufferBase, 1),
            "buffer region unregister failed");
        require(
            !ventus_rtlsim_pmemcpy_d2h(sim, data.data(), kBufferBase, data.size()),
            "unregistered range read unexpectedly succeeded");
        stats = ventus_rtlsim_pmem_missing_read_stats(sim);
        require(stats.out_of_bounds == 2, "unregistered range was not classified as OOB");

        require(ventus_rtlsim_finish_checked(sim, false) == 0, "simulator cleanup failed");
        std::ifstream log_stream(kLogFile);
        require(log_stream.good(), "PMEM test log was not created");
        const std::string log(
            (std::istreambuf_iterator<char>(log_stream)),
            std::istreambuf_iterator<char>());
        require_log_entry(log, "warning", "PMEM cold page read");
        require_log_entry(log, "warning", "PMEM PDS cold page read");
        require_log_entry(log, "warning", "PMEM allocation-padding read");
        require_log_entry(log, "error", "PMEM out-of-bounds read");
        log_stream.close();
        std::filesystem::remove(kLogFile);
        std::cout << "PMEM region classification test passed\n";
        return 0;
    } catch (const std::exception& error) {
        std::cerr << "PMEM region classification test failed: " << error.what() << '\n';
        return 1;
    }
}

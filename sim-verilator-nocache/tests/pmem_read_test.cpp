#include "physical_mem.hpp"

#include <algorithm>
#include <array>
#include <cstdint>
#include <iostream>
#include <memory>
#include <stdexcept>
#include <string>

#include <spdlog/logger.h>
#include <spdlog/sinks/null_sink.h>

namespace {
constexpr paddr_t kBufferBase = 0x90000000;
constexpr paddr_t kPdsBase = 0x90002000;
constexpr uint64_t kPageSize = 4096;

void require(bool condition, const std::string& message) {
    if (!condition) throw std::runtime_error(message);
}
} // namespace

int main() {
    try {
        auto sink = std::make_shared<spdlog::sinks::null_sink_mt>();
        auto logger = std::make_shared<spdlog::logger>("pmem-read-test", sink);
        PhysicalMemory memory(true, kPageSize, logger);

        require(
            memory.region_register({
                kBufferBase, kPageSize + 16, 2 * kPageSize,
                PmemRegionKind::Buffer, 1,
            }),
            "buffer region registration failed");
        require(
            memory.region_register({
                kPdsBase, 64, kPageSize, PmemRegionKind::Pds, 2,
            }),
            "PDS region registration failed");

        std::array<uint8_t, 16> cold_data {};
        cold_data.fill(0xff);
        require(
            !memory.read(kBufferBase, cold_data.data(), cold_data.size()),
            "internal cold-page read unexpectedly succeeded");
        require(
            std::all_of(
                cold_data.begin(), cold_data.end(),
                [](uint8_t byte) { return byte == 0; }),
            "internal cold-page read was not zero-filled");

        cold_data.fill(0xff);
        require(
            memory.read_d2h(kBufferBase, cold_data.data(), cold_data.size()),
            "D2H cold-page read failed");
        require(
            std::all_of(
                cold_data.begin(), cold_data.end(),
                [](uint8_t byte) { return byte == 0; }),
            "D2H cold-page read was not zero-filled");

        const std::array<uint8_t, 16> prefix {
            0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27,
            0x28, 0x29, 0x2a, 0x2b, 0x2c, 0x2d, 0x2e, 0x2f,
        };
        require(
            memory.write(kBufferBase, prefix.data(), prefix.size()),
            "materialized prefix write failed");
        std::array<uint8_t, kPageSize + 16> mixed_data {};
        mixed_data.fill(0xff);
        require(
            memory.read_d2h(kBufferBase, mixed_data.data(), mixed_data.size()),
            "materialized/cold-tail D2H read failed");
        require(
            std::equal(prefix.begin(), prefix.end(), mixed_data.begin()),
            "materialized prefix changed during D2H read");
        require(
            std::all_of(
                mixed_data.begin() + prefix.size(), mixed_data.end(),
                [](uint8_t byte) { return byte == 0; }),
            "cold tail was not zero-filled");

        require(
            !memory.read_d2h(kBufferBase + kPageSize + 32,
                cold_data.data(), cold_data.size()),
            "allocation-padding D2H read unexpectedly succeeded");
        require(
            !memory.read_d2h(kPdsBase, cold_data.data(), cold_data.size()),
            "PDS D2H read unexpectedly succeeded");
        require(
            !memory.read_d2h(kPdsBase + kPageSize,
                cold_data.data(), cold_data.size()),
            "OOB D2H read unexpectedly succeeded");

        const auto stats = memory.missing_read_stats();
        require(stats.cold_page == 3, "cold-page reads were misclassified");
        require(stats.allocation_padding == 1, "padding read was misclassified");
        require(stats.pds_cold_page == 1, "PDS read was misclassified");
        require(stats.out_of_bounds == 1, "OOB read was misclassified");

        std::cout << "No-cache PMEM D2H test passed\n";
        return 0;
    } catch (const std::exception& error) {
        std::cerr << "No-cache PMEM D2H test failed: " << error.what() << '\n';
        return 1;
    }
}

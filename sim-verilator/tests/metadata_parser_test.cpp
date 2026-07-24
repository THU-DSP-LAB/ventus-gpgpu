#include "kernel.hpp"
#include <cstdint>
#include <filesystem>
#include <fstream>
#include <iomanip>
#include <iostream>
#include <limits>
#include <stdexcept>
#include <string>
#include <unistd.h>
#include <vector>

namespace {
void write_metadata(const std::filesystem::path& path, const std::vector<uint64_t>& items) {
    std::ofstream output(path);
    if (!output) {
        throw std::runtime_error("failed to create metadata fixture");
    }
    output << std::hex << std::setfill('0');
    for (uint64_t item : items) {
        output << std::setw(8) << static_cast<uint32_t>(item) << '\n';
        output << std::setw(8) << static_cast<uint32_t>(item >> 32) << '\n';
    }
}

std::vector<uint64_t> valid_metadata() {
    return {
        0x80000000, 0, 1, 1, 1, 32, 1, 0x90024000, 0x1000, 0x1000, 64, 64,
        0x90004000, 1, 6, 1, 1, 6, 1, 1, 0, 0, 0, 1, 0x90000000, 24, 64,
    };
}

void expect_rejected(const std::filesystem::path& path, const std::vector<uint64_t>& items) {
    write_metadata(path, items);
    try {
        Kernel kernel("rejected", path, "");
    } catch (const std::runtime_error&) {
        return;
    }
    throw std::runtime_error("invalid metadata was accepted");
}

void expect_file_rejected(const std::filesystem::path& path) {
    try {
        Kernel kernel("rejected", path, "");
    } catch (const std::runtime_error&) {
        return;
    }
    throw std::runtime_error("legacy repository metadata was accepted");
}
} // namespace

int main() {
    const std::filesystem::path directory
        = std::filesystem::temp_directory_path() / ("ventus-metadata-parser-" + std::to_string(getpid()));
    std::filesystem::create_directories(directory);
    try {
        const auto valid = valid_metadata();
        const auto valid_path = directory / "valid.metadata";
        write_metadata(valid_path, valid);
        Kernel kernel("valid", valid_path, "");
        const auto* metadata = kernel.get_metadata();
        if (metadata->num_thread_global[0] != 6 || metadata->num_thread_local[0] != 6
            || metadata->threadIdxOffset[0] != 0 || metadata->num_buffer != 1
            || metadata->buffer_base[0] != 0x90000000 || metadata->buffer_size[0] != 24
            || metadata->buffer_allocsize[0] != 64) {
            throw std::runtime_error("valid metadata decoded incorrectly");
        }

        Kernel repository_fixture(
            "repository", "../ventus/txt/test-metadata/vecadd_0_full.metadata", ""
        );
        const auto* repository_metadata = repository_fixture.get_metadata();
        if (repository_metadata->num_thread_global[0] != 6
            || repository_metadata->num_thread_local[0] != 6
            || repository_metadata->num_buffer != 7) {
            throw std::runtime_error("repository metadata fixture decoded incorrectly");
        }
        expect_file_rejected("../ventus/txt/adv_vecadd/vecadd4x4.metadata");

        std::vector<uint64_t> legacy(valid.begin(), valid.begin() + 13);
        legacy.insert(legacy.end(), {1, 0x90000000, 24, 64});
        expect_rejected(directory / "legacy.metadata", legacy);

        auto truncated = valid;
        truncated.pop_back();
        expect_rejected(directory / "truncated.metadata", truncated);

        auto oversized = std::vector<uint64_t>(valid.begin(), valid.begin() + 24);
        oversized[23] = std::numeric_limits<uint64_t>::max();
        expect_rejected(directory / "oversized.metadata", oversized);

        auto trailing = valid;
        trailing.push_back(0);
        expect_rejected(directory / "trailing.metadata", trailing);
    } catch (...) {
        std::filesystem::remove_all(directory);
        throw;
    }
    std::filesystem::remove_all(directory);
    std::cout << "metadata parser tests passed\n";
    return 0;
}

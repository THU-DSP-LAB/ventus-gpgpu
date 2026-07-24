#include "ventus_rtlsim.h"

#include <cstdint>
#include <filesystem>
#include <fstream>
#include <iostream>
#include <stdexcept>
#include <string>
#include <unistd.h>

namespace {
const std::filesystem::path kRoot = "/tmp/ventus-persistent-state-test";
const std::filesystem::path kState = kRoot / "checkpoint";
const std::filesystem::path kExpected = kRoot / "expected-suffix";
const std::filesystem::path kActual = kRoot / "actual-suffix";
const std::filesystem::path kSavedPmu = kRoot / "saved.pmu";
const std::filesystem::path kExpectedPmu = kRoot / "expected.pmu";
constexpr paddr_t kAddress = 0x90000000;
constexpr uint64_t kValue = 0x1122334455667788;
constexpr uint64_t kSavedTime = 95;
constexpr uint64_t kSuffixTime = 125;

void require(bool condition, const std::string& message) {
    if (!condition) throw std::runtime_error(message);
}

ventus_rtlsim_config_t test_config() {
    ventus_rtlsim_config_t config;
    ventus_rtlsim_get_default_config(&config);
    config.sim_time_max = 1000;
    config.log.console.enable = false;
    config.log.file.enable = false;
    config.pmem.auto_alloc = true;
    config.waveform.enable = false;
    config.snapshot.enable = false;
    return config;
}

void step_until(ventus_rtlsim_t* sim, uint64_t target) {
    while (ventus_rtlsim_get_time(sim) < target) {
        const auto* result = ventus_rtlsim_step(sim);
        require(result != nullptr && !result->error && !result->time_exceed && !result->hang,
                "simulation step failed");
    }
}

void write_bytes(std::ofstream& output, const void* data, size_t size) {
    output.write(static_cast<const char*>(data), static_cast<std::streamsize>(size));
    require(static_cast<bool>(output), "cannot write comparison data");
}

void write_pmu(const ventus_rtlsim_t* sim, const std::filesystem::path& path) {
    const auto pmu = ventus_rtlsim_get_pmu(sim);
    require(pmu.num_sm > 0 && pmu.pipeline != nullptr && pmu.inst_class != nullptr
                && pmu.dcache != nullptr,
            "PMU view is incomplete");
    std::ofstream output(path, std::ios::binary | std::ios::trunc);
    require(static_cast<bool>(output), "cannot open PMU comparison file");
    write_bytes(output, &pmu.num_sm, sizeof(pmu.num_sm));
    write_bytes(output, &pmu.has_dcache, sizeof(pmu.has_dcache));
    write_bytes(output, pmu.pipeline, pmu.num_sm * sizeof(*pmu.pipeline));
    write_bytes(output, pmu.inst_class, pmu.num_sm * sizeof(*pmu.inst_class));
    write_bytes(output, pmu.dcache, pmu.num_sm * sizeof(*pmu.dcache));
}

std::string read_file(const std::filesystem::path& path) {
    std::ifstream input(path, std::ios::binary);
    require(static_cast<bool>(input), "cannot open comparison file");
    return {
        std::istreambuf_iterator<char>(input), std::istreambuf_iterator<char>()};
}

void require_same_file(
    const std::filesystem::path& actual, const std::filesystem::path& expected,
    const std::string& message) {
    require(read_file(actual) == read_file(expected), message);
}

void save_state() {
    require(
        ventus_rtlsim_persistent_state_version() == 1,
        "savable library reports the wrong persistent-state ABI");
    std::filesystem::remove_all(kRoot);
    std::filesystem::create_directories(kRoot);
    auto config = test_config();
    ventus_rtlsim_t* sim = ventus_rtlsim_init(&config);
    require(sim != nullptr, "simulator initialization failed");
    require(ventus_rtlsim_pmemcpy_h2d(sim, kAddress, &kValue, sizeof(kValue)),
            "PMEM write failed");
    step_until(sim, kSavedTime);
    require(ventus_rtlsim_is_idle(sim), "save point is not idle");
    write_pmu(sim, kSavedPmu);
    require(ventus_rtlsim_save_state(sim, kState.c_str()) == 0,
            "persistent state save failed");
    step_until(sim, kSuffixTime);
    write_pmu(sim, kExpectedPmu);
    require(ventus_rtlsim_save_state(sim, kExpected.c_str()) == 0,
            "continuous suffix state save failed");
    std::ofstream pid_file(kRoot / "save.pid");
    pid_file << getpid() << '\n';
    pid_file.close();
    require(static_cast<bool>(pid_file), "cannot write save PID");
    require(ventus_rtlsim_finish_checked(sim, false) == 0, "save process cleanup failed");
}

void restore_state() {
    std::ifstream pid_file(kRoot / "save.pid");
    pid_t save_pid = 0;
    pid_file >> save_pid;
    require(save_pid > 0 && save_pid != getpid(), "restore did not use a new process");
    auto config = test_config();
    ventus_rtlsim_t* sim = ventus_rtlsim_restore_state(&config, kState.c_str());
    require(sim != nullptr, "persistent state restore failed");
    require(ventus_rtlsim_get_time(sim) == kSavedTime, "simulation time was not restored");
    require(ventus_rtlsim_is_idle(sim), "CTA idle state was not restored");
    uint64_t observed = 0;
    require(ventus_rtlsim_pmemcpy_d2h(sim, &observed, kAddress, sizeof(observed)),
            "restored PMEM read failed");
    require(observed == kValue, "restored PMEM value differs");
    write_pmu(sim, kRoot / "actual-saved.pmu");
    require_same_file(
        kRoot / "actual-saved.pmu", kSavedPmu, "saved-point PMU differs");
    step_until(sim, kSuffixTime);
    write_pmu(sim, kRoot / "actual.pmu");
    require_same_file(
        kRoot / "actual.pmu", kExpectedPmu, "suffix PMU differs");
    require(ventus_rtlsim_save_state(sim, kActual.c_str()) == 0,
            "restored suffix state save failed");
    require_same_file(
        kActual / "state.bin", kExpected / "state.bin",
        "restored suffix RTL state differs from continuous execution");
    require(ventus_rtlsim_finish_checked(sim, false) == 0, "restore process cleanup failed");
}

void corrupt_state() {
    const auto corrupt = kRoot / "corrupt";
    std::filesystem::copy(
        kState, corrupt, std::filesystem::copy_options::recursive);
    {
        std::fstream state(corrupt / "state.bin", std::ios::binary | std::ios::in | std::ios::out);
        require(static_cast<bool>(state), "cannot open copied state");
        char byte = 0;
        state.read(&byte, 1);
        byte ^= 0x1;
        state.seekp(0);
        state.write(&byte, 1);
    }
    auto config = test_config();
    require(ventus_rtlsim_restore_state(&config, corrupt.c_str()) == nullptr,
            "corrupted state was accepted");

    const auto truncated = kRoot / "truncated";
    std::filesystem::copy(
        kState, truncated, std::filesystem::copy_options::recursive);
    const auto truncated_path = truncated / "state.bin";
    std::filesystem::resize_file(
        truncated_path, std::filesystem::file_size(truncated_path) - 1);
    require(ventus_rtlsim_restore_state(&config, truncated.c_str()) == nullptr,
            "truncated state was accepted");

    const auto partial = kRoot / "partial";
    std::filesystem::copy(
        kState, partial, std::filesystem::copy_options::recursive);
    std::filesystem::remove(partial / "COMPLETE");
    require(ventus_rtlsim_restore_state(&config, partial.c_str()) == nullptr,
            "state without COMPLETE marker was accepted");

    const auto wrong_identity = kRoot / "wrong-identity";
    std::filesystem::copy(
        kState, wrong_identity, std::filesystem::copy_options::recursive);
    const auto manifest_path = wrong_identity / "manifest.json";
    std::ifstream manifest_input(manifest_path);
    std::string manifest(
        (std::istreambuf_iterator<char>(manifest_input)), std::istreambuf_iterator<char>());
    const auto position = manifest.find("\"state_version\": 1");
    require(position != std::string::npos, "cannot locate manifest identity");
    manifest.replace(position, std::string("\"state_version\": 1").size(), "\"state_version\": 2");
    std::ofstream manifest_output(manifest_path, std::ios::trunc);
    manifest_output << manifest;
    manifest_output.close();
    require(ventus_rtlsim_restore_state(&config, wrong_identity.c_str()) == nullptr,
            "wrong state identity was accepted");

    const auto wrong_schema = kRoot / "wrong-schema";
    std::filesystem::copy(
        kState, wrong_schema, std::filesystem::copy_options::recursive);
    const auto schema_manifest_path = wrong_schema / "manifest.json";
    std::ifstream schema_input(schema_manifest_path);
    std::string schema_manifest(
        (std::istreambuf_iterator<char>(schema_input)), std::istreambuf_iterator<char>());
    const auto schema_position = schema_manifest.find("\"schema_version\": 1");
    require(schema_position != std::string::npos, "cannot locate manifest schema");
    schema_manifest.replace(
        schema_position, std::string("\"schema_version\": 1").size(),
        "\"schema_version\": 2");
    std::ofstream schema_output(schema_manifest_path, std::ios::trunc);
    schema_output << schema_manifest;
    schema_output.close();
    require(ventus_rtlsim_restore_state(&config, wrong_schema.c_str()) == nullptr,
            "wrong manifest schema was accepted");

    const auto symlinked = kRoot / "symlinked";
    std::filesystem::copy(
        kState, symlinked, std::filesystem::copy_options::recursive);
    std::filesystem::rename(
        symlinked / "state.bin", symlinked / "state.real");
    std::filesystem::create_symlink("state.real", symlinked / "state.bin");
    require(ventus_rtlsim_restore_state(&config, symlinked.c_str()) == nullptr,
            "symlinked state file was accepted");
}

void unsupported_state() {
    require(
        ventus_rtlsim_persistent_state_version() == 0,
        "default library reports persistent-state support");
    auto config = test_config();
    ventus_rtlsim_t* sim = ventus_rtlsim_init(&config);
    require(sim != nullptr, "default simulator initialization failed");
    require(ventus_rtlsim_save_state(sim, (kRoot / "unsupported").c_str()) == -1,
            "default library accepted persistent save");
    require(ventus_rtlsim_finish_checked(sim, false) == 0,
            "default simulator cleanup failed");
    require(ventus_rtlsim_restore_state(&config, kState.c_str()) == nullptr,
            "default library accepted persistent restore");
}
} // namespace

int main(int argc, char** argv) {
    try {
        require(argc == 2, "usage: persistent_state_test save|restore|corrupt|unsupported");
        const std::string mode = argv[1];
        if (mode == "save") {
            save_state();
        } else if (mode == "restore") {
            restore_state();
        } else if (mode == "corrupt") {
            corrupt_state();
        } else if (mode == "unsupported") {
            unsupported_state();
        } else {
            throw std::runtime_error("unknown test mode");
        }
        std::cout << "persistent state " << mode << " test passed\n";
        return 0;
    } catch (const std::exception& error) {
        std::cerr << "persistent state test failed: " << error.what() << '\n';
        return 1;
    }
}

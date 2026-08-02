#include "persistent_state.hpp"

#include "Vdut.h"
#include "ventus_rtlsim_impl.hpp"
#include "verilated.h"
#include "verilated_save.h"

#include <array>
#include <cerrno>
#include <cstdint>
#include <filesystem>
#include <fstream>
#include <iomanip>
#include <iostream>
#include <limits>
#include <nlohmann/json.hpp>
#include <sstream>
#include <string>
#include <system_error>
#include <fcntl.h>
#include <unistd.h>

#ifdef VENTUS_RTL_SAVABLE
#include <openssl/evp.h>

namespace {
constexpr uint64_t kStateMagic = 0x5652544c53544154ull;
constexpr uint32_t kStateVersion = 1;
constexpr uint32_t kManifestVersion = 1;
constexpr const char* kStateFilename = "state.bin";
constexpr const char* kManifestFilename = "manifest.json";
constexpr const char* kCompleteFilename = "COMPLETE";

template <typename T>
void save_vector(VerilatedSerialize& output, const std::vector<T>& values) {
    output << static_cast<uint64_t>(values.size());
    if (!values.empty()) {
        output.write(values.data(), values.size() * sizeof(T));
    }
}

template <typename T>
bool restore_vector(
    VerilatedDeserialize& input, std::vector<T>& values, uint64_t expected_size) {
    uint64_t size = 0;
    input >> size;
    if (size != expected_size
        || size > std::numeric_limits<size_t>::max() / sizeof(T)) {
        return false;
    }
    values.resize(static_cast<size_t>(size));
    if (!values.empty()) {
        input.read(values.data(), values.size() * sizeof(T));
    }
    return true;
}

bool fsync_path(const std::filesystem::path& path, bool directory) {
    const int flags = directory ? O_RDONLY | O_DIRECTORY : O_RDONLY;
    const int fd = open(path.c_str(), flags);
    if (fd < 0) return false;
    const bool success = fsync(fd) == 0;
    close(fd);
    return success;
}

std::string sha256_file(const std::filesystem::path& path) {
    std::ifstream input(path, std::ios::binary);
    if (!input) return {};
    EVP_MD_CTX* context = EVP_MD_CTX_new();
    if (context == nullptr || EVP_DigestInit_ex(context, EVP_sha256(), nullptr) != 1) {
        EVP_MD_CTX_free(context);
        return {};
    }
    std::array<char, 1024 * 1024> buffer {};
    while (input) {
        input.read(buffer.data(), buffer.size());
        const std::streamsize count = input.gcount();
        if (count > 0
            && EVP_DigestUpdate(context, buffer.data(), static_cast<size_t>(count)) != 1) {
            EVP_MD_CTX_free(context);
            return {};
        }
    }
    if (!input.eof()) {
        EVP_MD_CTX_free(context);
        return {};
    }
    std::array<unsigned char, EVP_MAX_MD_SIZE> digest {};
    unsigned int digest_size = 0;
    if (EVP_DigestFinal_ex(context, digest.data(), &digest_size) != 1) {
        EVP_MD_CTX_free(context);
        return {};
    }
    EVP_MD_CTX_free(context);
    std::ostringstream result;
    result << std::hex << std::setfill('0');
    for (unsigned int index = 0; index < digest_size; ++index) {
        result << std::setw(2) << static_cast<unsigned int>(digest[index]);
    }
    return result.str();
}

nlohmann::json rtl_identity() {
    nlohmann::json parameters = nlohmann::json::object();
    for (const auto& [name, value] : rtl_parameters) {
        parameters[name] = value;
    }
    return {
        {"verilator", {
            {"name", Verilated::productName()},
            {"version", Verilated::productVersion()},
        }},
        {"rtl_parameters", std::move(parameters)},
        {"model_threads", kVentusRtlModelThreads},
        {"state_version", kStateVersion},
    };
}

bool write_json_file(
    const std::filesystem::path& path, const nlohmann::json& value) {
    std::ofstream output(path, std::ios::binary | std::ios::trunc);
    if (!output) return false;
    output << value.dump(2) << '\n';
    output.close();
    return static_cast<bool>(output) && fsync_path(path, false);
}

bool write_complete_file(const std::filesystem::path& path) {
    std::ofstream output(path, std::ios::binary | std::ios::trunc);
    if (!output) return false;
    output << "complete\n";
    output.close();
    return static_cast<bool>(output) && fsync_path(path, false);
}

bool read_manifest(
    const std::filesystem::path& directory, nlohmann::json& manifest) {
    try {
        if (std::filesystem::symlink_status(directory).type()
            != std::filesystem::file_type::directory) {
            return false;
        }
        const auto complete = directory / kCompleteFilename;
        const auto manifest_path = directory / kManifestFilename;
        const auto state_path = directory / kStateFilename;
        if (std::filesystem::symlink_status(complete).type()
                != std::filesystem::file_type::regular
            || std::filesystem::symlink_status(manifest_path).type()
                != std::filesystem::file_type::regular
            || std::filesystem::symlink_status(state_path).type()
                != std::filesystem::file_type::regular) {
            return false;
        }
        std::ifstream input(manifest_path);
        if (!input) return false;
        input >> manifest;
        return manifest.at("schema_version") == kManifestVersion
            && manifest.at("status") == "COMPLETE"
            && manifest.at("state_file") == kStateFilename
            && manifest.at("identity") == rtl_identity();
    } catch (const std::exception&) {
        return false;
    }
}

bool verify_state_file(
    const std::filesystem::path& directory, const nlohmann::json& manifest) {
    try {
        const auto state_path = directory / kStateFilename;
        const uint64_t expected_size = manifest.at("state_bytes");
        const std::string expected_sha256 = manifest.at("state_sha256");
        return std::filesystem::file_size(state_path) == expected_size
            && !expected_sha256.empty()
            && sha256_file(state_path) == expected_sha256;
    } catch (const std::exception&) {
        return false;
    }
}
} // namespace

bool ventus_rtlsim_t::save_state_binary(const std::string& filename) {
    if (!cta->is_idle() || step_status.error || step_status.time_exceed || step_status.hang
        || config.waveform.enable || tfp != nullptr || snapshots.is_child
        || !snapshots.children.empty()) {
        logger->error("persistent state save requires an idle, healthy simulator with no waveform or fork child");
        return false;
    }
    VerilatedSave output;
    output.open(filename);
    if (!output.isOpen()) return false;
    output << kStateMagic << kStateVersion;
    output << contextp << *dut;
    if (!pmem->save(output) || !cta->save_idle(output)) {
        output.close();
        return false;
    }
    output << pmu_num_sm << pmu_snapshot.has_dcache;
    save_vector(output, pmu_snapshot.pipeline);
    save_vector(output, pmu_snapshot.inst_class);
    save_vector(output, pmu_snapshot.dcache);
    output << step_status.error << step_status.time_exceed << step_status.idle << step_status.hang;
    output << last_pmu_progress_time << last_pmu_progress_value;
    output << need_icache_invalidate << need_perf_dump_summary;
    output.close();
    return true;
}

bool ventus_rtlsim_t::restore_state_binary(const std::string& filename) {
    VerilatedRestore input;
    input.open(filename);
    if (!input.isOpen()) return false;
    uint64_t magic = 0;
    uint32_t version = 0;
    input >> magic >> version;
    if (magic != kStateMagic || version != kStateVersion) {
        input.close();
        return false;
    }
    input >> contextp >> *dut;
    if (!pmem->restore(input) || !cta->restore_idle(input)) {
        input.close();
        return false;
    }
    uint32_t restored_num_sm = 0;
    bool restored_has_dcache = false;
    input >> restored_num_sm >> restored_has_dcache;
    if (restored_num_sm != pmu_num_sm
        || restored_has_dcache != pmu_snapshot.has_dcache
        || !restore_vector(input, pmu_snapshot.pipeline, pmu_num_sm)
        || !restore_vector(input, pmu_snapshot.inst_class, pmu_num_sm)
        || !restore_vector(input, pmu_snapshot.dcache, pmu_num_sm)) {
        input.close();
        return false;
    }
    input >> step_status.error >> step_status.time_exceed >> step_status.idle >> step_status.hang;
    input >> last_pmu_progress_time >> last_pmu_progress_value;
    input >> need_icache_invalidate >> need_perf_dump_summary;
    input.close();
    snapshots = {};
    if (!step_status.idle || step_status.error || step_status.time_exceed || step_status.hang
        || !cta->is_idle()) {
        return false;
    }
    return true;
}

int ventus_persistent_state_save(ventus_rtlsim_t* sim, const char* directory_raw) {
    if (sim == nullptr || directory_raw == nullptr || directory_raw[0] == '\0') return -1;
    const std::filesystem::path target(directory_raw);
    const std::filesystem::path parent = target.parent_path().empty() ? "." : target.parent_path();
    const std::filesystem::path lock = target.string() + ".lock";
    const std::filesystem::path partial
        = target.string() + ".partial-" + std::to_string(static_cast<long long>(getpid()));
    std::error_code error;
    if (!std::filesystem::is_directory(parent)
        || std::filesystem::exists(target)
        || !std::filesystem::create_directory(lock, error) || error) {
        sim->logger->error("persistent state target is unavailable: {}", target.string());
        return -1;
    }
    bool success = false;
    bool published = false;
    try {
        if (std::filesystem::exists(partial)
            || !std::filesystem::create_directory(partial)) {
            throw std::runtime_error("cannot create partial state directory");
        }
        const auto state_path = partial / kStateFilename;
        if (!sim->save_state_binary(state_path.string()) || !fsync_path(state_path, false)) {
            throw std::runtime_error("cannot save RTL state binary");
        }
        const std::string state_sha256 = sha256_file(state_path);
        if (state_sha256.empty()) {
            throw std::runtime_error("cannot hash RTL state binary");
        }
        const nlohmann::json manifest = {
            {"schema_version", kManifestVersion},
            {"status", "COMPLETE"},
            {"state_file", kStateFilename},
            {"state_bytes", std::filesystem::file_size(state_path)},
            {"state_sha256", state_sha256},
            {"saved_time", sim->contextp->time()},
            {"pmem_pages", sim->pmem->page_count()},
            {"identity", rtl_identity()},
        };
        if (!write_json_file(partial / kManifestFilename, manifest)
            || !write_complete_file(partial / kCompleteFilename)
            || !fsync_path(partial, true)) {
            throw std::runtime_error("cannot commit RTL state metadata");
        }
        std::filesystem::rename(partial, target);
        published = true;
        if (!fsync_path(parent, true)) {
            throw std::runtime_error("cannot fsync RTL state parent directory");
        }
        success = true;
    } catch (const std::exception& exception) {
        sim->logger->error("persistent state save failed: {}", exception.what());
    }
    if (!success) {
        std::filesystem::remove_all(published ? target : partial, error);
        if (published) {
            fsync_path(parent, true);
        }
    }
    std::filesystem::remove_all(lock, error);
    return success ? 0 : -1;
}

ventus_rtlsim_t* ventus_persistent_state_restore(
    const ventus_rtlsim_config_t* config, const char* directory_raw) {
    if (config == nullptr || directory_raw == nullptr || directory_raw[0] == '\0'
        || config->snapshot.enable || config->waveform.enable) {
        return nullptr;
    }
    const std::filesystem::path directory(directory_raw);
    nlohmann::json manifest;
    if (!read_manifest(directory, manifest)
        || !verify_state_file(directory, manifest)) {
        std::cerr << "persistent state validation failed: " << directory << '\n';
        return nullptr;
    }
    ventus_rtlsim_t* sim = nullptr;
    try {
        sim = new ventus_rtlsim_t();
        sim->constructor(config, false);
        if (!sim->restore_state_binary((directory / kStateFilename).string())
            || sim->contextp->time() != manifest.at("saved_time").get<uint64_t>()
            || sim->pmem->page_count() != manifest.at("pmem_pages").get<uint64_t>()) {
            throw std::runtime_error("restored state does not match its manifest");
        }
        return sim;
    } catch (const std::exception& exception) {
        std::cerr << "persistent state restore failed: " << exception.what() << '\n';
        if (sim != nullptr) {
            sim->destructor(false);
            delete sim;
        }
        return nullptr;
    }
}
#else
bool ventus_rtlsim_t::save_state_binary(const std::string& filename) {
    (void)filename;
    logger->error("persistent state requires a SAVABLE=1 RTL library");
    return false;
}

bool ventus_rtlsim_t::restore_state_binary(const std::string& filename) {
    (void)filename;
    logger->error("persistent state requires a SAVABLE=1 RTL library");
    return false;
}

int ventus_persistent_state_save(ventus_rtlsim_t* sim, const char* directory) {
    (void)directory;
    if (sim != nullptr) {
        sim->logger->error("persistent state requires a SAVABLE=1 RTL library");
    }
    return -1;
}

ventus_rtlsim_t* ventus_persistent_state_restore(
    const ventus_rtlsim_config_t* config, const char* directory) {
    (void)config;
    (void)directory;
    return nullptr;
}
#endif

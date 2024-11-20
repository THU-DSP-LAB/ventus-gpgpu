#pragma once
#include "ventus_rtlsim.h"
#include <cstdint>
#include <filesystem>
#include <functional>
#include <memory>
#include <spdlog/logger.h>
#include <string>
#include <vector>

struct dim3_t {
    uint32_t x, y, z;
};

typedef ventus_kernel_metadata_t metadata_t;

class Kernel {
public:
    Kernel(
        const std::string& kernel_name, const std::filesystem::path metadata_file, const std::filesystem::path data_file
    );
    Kernel(
        const metadata_t* metadata, std::function<void(const metadata_t*)> data_load_callback,
        std::function<void(const metadata_t*)> finish_callback, std::shared_ptr<spdlog::logger> logger
    );

    // Basic kernel info
    uint32_t get_kid() const { return m_kernel_id; }
    std::string get_kname() const { return m_kernel_name; }
    const metadata_t* get_metadata() const { return &m_metadata; }

    bool no_more_wg_to_dispatch() const;
    dim3_t get_next_wg_idx3d_in_kernel() const { return m_next_wg; }
    uint32_t get_next_wg_idx_in_kernel() const;
    uint32_t get_next_wgid() const;
    void wg_dispatched();

    dim3_t get_num_wg_3d() const { return m_grid_dim; }
    uint32_t get_num_wg() const { return m_grid_dim.x * m_grid_dim.y * m_grid_dim.z; }
    uint32_t get_num_wf() const { return m_metadata.wg_size; }
    uint32_t get_num_thread() const { return m_metadata.wf_size; }
    uint32_t get_num_lds() const { return m_metadata.ldsSize; }
    // uint32_t get_num_sgpr() const { return m_metadata.sgprUsage * m_metadata.wg_size; }
    // uint32_t get_num_vgpr() const { return m_metadata.vgprUsage * m_metadata.wg_size; }
    uint32_t get_num_sgpr_per_wf() const { return m_metadata.sgprUsage; }
    uint32_t get_num_vgpr_per_wf() const { return m_metadata.vgprUsage; }
    uint32_t get_start_pc() const { return m_metadata.startaddr; }
    uint32_t get_csr_baseaddr() const { return m_metadata.metaDataBaseAddr; }
    uint32_t get_gds_baseaddr() const { return 0; } // TODO
    uint32_t get_pds_baseaddr() const { return m_metadata.pdsBaseAddr; }
    uint32_t get_num_pds_per_thread() const { return m_metadata.pdsSize; }

    // 判断一个线程块是否属于本kernel
    bool is_wg_belonging(uint32_t wg_id, uint32_t* wg_idx_in_kernel = nullptr) const;

    // kernel and thread-block(workgroup) status
    void wg_finish(uint32_t wgid);
    bool is_running() const;
    bool is_finished() const;
    bool is_dispatching() const;
    bool is_activated() const { return m_is_activated; }

    // Load kernel init data (testcase.data file) and get ready to run
    void activate(uint32_t kernel_id, uint32_t wgid_base);
    void deactivate();
    const std::function<void(const metadata_t*)> m_finish_callback; // call this after kernel finished

    const std::filesystem::path m_datafile;

private:
    uint32_t m_kernel_id;
    const std::string m_kernel_name;
    metadata_t m_metadata;
    uint32_t m_wgid_base;

    // Helpers
    bool isHexCharacter(char c) const;
    int charToHex(char c) const;

    // Load kernel metadata (testcase.metadata file)
    void readHexFile(const std::string& filename, std::vector<uint64_t>& items, int itemSize = 64) const;
    void initMetaData(const std::string& filename);
    void assignMetadata(const std::vector<uint64_t>& metadata, metadata_t& mtd);
    std::function<void(const metadata_t*)> m_load_data_callback;

    // Get new thread-block
    dim3_t m_next_wg = { 0, 0, 0 }; // start from 0 ~ (grid_dim - 1)
    dim3_t m_grid_dim;

    // Thread-block(workgroup) status: waiting, running, finished
    enum { WG_STATUS_WAITING, WG_STATUS_RUNNING, WG_STATUS_FINISHED };
    std::vector<int> m_wg_status;

    // Kernel status
    bool m_is_activated; // activated: data loaded to memory and ready to run

    std::shared_ptr<spdlog::logger> logger;
};

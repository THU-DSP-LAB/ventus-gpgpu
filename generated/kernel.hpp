#pragma once
#include "MemBox.hpp"
#include <cstdint>
#include <fstream>
#include <iostream>
#include <string>
#include <vector>

struct dim3_t {
    uint64_t x, y, z;
};

struct metadata_t { // 这个metadata是供驱动使用的，而不是给硬件的
    uint64_t startaddr;
    uint64_t kernel_id;
    uint64_t kernel_size[3];   ///> 每个kernel的workgroup三维数目
    uint64_t wf_size;          ///> 每个warp的thread数目
    uint64_t wg_size;          ///> 每个workgroup的warp数目
    uint64_t metaDataBaseAddr; ///> CSR_KNL的值，
    uint64_t ldsSize;          ///> 每个workgroup使用的local memory的大小
    uint64_t pdsSize;          ///> 每个thread用到的private memory大小
    uint64_t sgprUsage;        ///> 每个workgroup使用的标量寄存器数目
    uint64_t vgprUsage;        ///> 每个thread使用的向量寄存器数目
    uint64_t pdsBaseAddr; ///> private memory的基址，要转成每个workgroup的基地址， wf_size*wg_size*pdsSize
    uint64_t num_buffer;  ///> buffer的数目，包括pc
    uint64_t* buffer_base;      ///> 各buffer的基址。第一块buffer是给硬件用的metadata
    uint64_t* buffer_size;      ///> 各buffer的size，以Bytes为单位。实际使用的大小，用于初始化.data
    uint64_t* buffer_allocsize; ///> 各buffer的size，以Bytes为单位。分配的大小
};

void increment_x_then_y_then_z(dim3_t& i, const dim3_t& bound);

class Kernel {
public:
    Kernel(const std::string& kernel_name, const std::string& metadata_file, const std::string& data_file, MemBox& mem);

    bool no_more_ctas_to_run() const {
        return (m_next_cta.x >= m_grid_dim.x || m_next_cta.y >= m_grid_dim.y || m_next_cta.z >= m_grid_dim.z);
    }
    std::string get_kname() const { return m_kernel_name; }
    dim3_t get_next_cta_id() const { return m_next_cta; }
    unsigned get_next_cta_id_single() const {
        return m_next_cta.x + m_grid_dim.x * m_next_cta.y + m_grid_dim.x * m_grid_dim.y * m_next_cta.z;
    }
    void increment_cta_id() { increment_x_then_y_then_z(m_next_cta, m_grid_dim); }

    uint32_t get_num_wf() const { return m_metadata.wg_size; }
    uint32_t get_num_thread() const { return m_metadata.wf_size; }
    uint32_t get_num_lds() const { return m_metadata.ldsSize; }
    // uint32_t get_num_sgpr() const { return m_metadata.sgprUsage * m_metadata.wg_size; }
    // uint32_t get_num_vgpr() const { return m_metadata.vgprUsage * m_metadata.wg_size; }
    uint32_t get_num_sgpr_per_thread() const { return m_metadata.sgprUsage; }
    uint32_t get_num_vgpr_per_thread() const { return m_metadata.vgprUsage; }
    uint32_t get_start_pc() const { return m_metadata.startaddr; }
    uint32_t get_csr_baseaddr() const { return m_metadata.metaDataBaseAddr; }
    uint32_t get_gds_baseaddr() const { return 0; } // TODO
    uint32_t get_pds_baseaddr() const {
        return m_metadata.pdsBaseAddr
            + get_next_cta_id_single() * m_metadata.wg_size * m_metadata.wf_size * m_metadata.pdsSize;
    };

    void cta_finish(int id) { cta_finished[id] = true; }
    bool kernel_finished() const ;

private:
    std::string m_kernel_name;
    metadata_t m_metadata;
    unsigned m_running_cta; // 当前正在运行的cta数量

    // Tools

    bool isHexCharacter(char c) const {
        return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f');
    }
    int charToHex(char c) const {
        if (c >= '0' && c <= '9')
            return c - '0';
        else if (c >= 'A' && c <= 'F')
            return c - 'A' + 10;
        else if (c >= 'a' && c <= 'f')
            return c - 'a' + 10;
        else
            return -1; // Invalid character
    }

    // Kernel init

    void readHexFile(const std::string& filename, std::vector<uint64_t>& items, int itemSize = 64) const;
    void initMetaData(const std::string& filename);
    void assignMetadata(const std::vector<uint64_t>& metadata, metadata_t& mtd);
    void readDataFile(const std::string& filename, MemBox& mem, metadata_t mtd);

    // Get new CTA block

    dim3_t m_next_cta = { 0, 0, 0 }; // start from 0 ~ (grid_dim - 1)
    dim3_t m_grid_dim;

    // Record finished CTA
    std::vector<bool> cta_finished;
};

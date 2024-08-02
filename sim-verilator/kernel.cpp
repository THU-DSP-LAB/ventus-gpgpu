#include "kernel.hpp"
#include <cassert>
#include <cstdint>
#include <fstream>
#include <iostream>
#include <memory>
#include <string>
#include <vector>

void increment_x_then_y_then_z(dim3_t& i, const dim3_t& bound) {
    i.x++;
    if (i.x >= bound.x) {
        i.x = 0;
        i.y++;
        if (i.y >= bound.y) {
            i.y = 0;
            if (i.z < bound.z)
                i.z++;
        }
    }
}

Kernel::Kernel(
    const std::string& kernel_name, const std::string& metadata_file, const std::string& data_file, MemBox& mem) {
    // Set kernel name
    m_kernel_name = kernel_name;
    // Get metadata of this kernel
    initMetaData(metadata_file);
    // Load initial data of this kernel
    readDataFile(data_file, mem, m_metadata);
    // Init finished-CTA record vector
    cta_finished.resize(m_metadata.kernel_size[0] * m_metadata.kernel_size[1] * m_metadata.kernel_size[2]);
    std::fill(cta_finished.begin(), cta_finished.end(), false);
}

void Kernel::readHexFile(const std::string& filename, std::vector<uint64_t>& items, int itemSize) const {
    // itemSize为每个数据的比特数，这里为64

    std::ifstream file(filename);

    if (!file) {
        std::cout << "Error opening file: " << filename << std::endl;
        return;
    }

    char c;
    int bits       = 0;
    uint64_t value = 0;
    bool leftside  = false;

    while (file.get(c)) {
        if (c == '\n') {
            if (bits != 0)
                leftside = true;
            continue;
        }

        if (!isHexCharacter(c)) {
            std::cout << "Invalid character found: " << c << " in " << filename << std::endl;
            continue;
        }

        int hexValue = charToHex(c);
        if (leftside)
            value = value | ((uint64_t)hexValue << (92 - bits));
        else
            value = (value << 4) | hexValue;
        bits += 4;

        if (bits >= itemSize) {
            items.push_back(value);
            value    = 0;
            bits     = 0;
            leftside = false;
        }
    }

    if (bits > 0) {
        std::cout << "Warning: Incomplete item found at the end of the file!" << std::endl;
    }

    file.close();
}

void Kernel::initMetaData(const std::string& filename) {
    std::vector<uint64_t> metadata;
    readHexFile(filename, metadata, 64);
    assignMetadata(metadata, m_metadata);
}

void Kernel::assignMetadata(const std::vector<uint64_t>& metadata, metadata_t& mtd) {
    int index = 0;

    mtd.startaddr = metadata[index++];

    mtd.kernel_id = metadata[index++];

    for (int i = 0; i < 3; i++) {
        mtd.kernel_size[i] = metadata[index++];
    }
    m_grid_dim.x = mtd.kernel_size[0];
    m_grid_dim.y = mtd.kernel_size[1];
    m_grid_dim.z = mtd.kernel_size[2];

    mtd.wf_size          = metadata[index++];
    mtd.wg_size          = metadata[index++];
    mtd.metaDataBaseAddr = metadata[index++];
    mtd.ldsSize          = metadata[index++];
    mtd.pdsSize          = metadata[index++];
    mtd.sgprUsage        = metadata[index++];
    mtd.vgprUsage        = metadata[index++];
    mtd.pdsBaseAddr      = metadata[index++];
    mtd.num_buffer       = metadata[index++];

    mtd.buffer_base = new uint64_t[mtd.num_buffer];

    for (int i = 0; i < mtd.num_buffer; i++) {
        mtd.buffer_base[i] = metadata[index++];
    }

    mtd.buffer_size = new uint64_t[mtd.num_buffer];
    for (int i = 0; i < mtd.num_buffer; i++) {
        mtd.buffer_size[i] = metadata[index++];
    }

    mtd.buffer_allocsize = new uint64_t[mtd.num_buffer];
    for (int i = 0; i < mtd.num_buffer; i++) {
        mtd.buffer_allocsize[i] = metadata[index++];
    }
}

void Kernel::readDataFile(const std::string& filename, MemBox& mem, metadata_t mtd) {
    std::ifstream file(filename);
    if (!file.is_open()) {
        std::cerr << "Failed to open file: " << filename << std::endl;
        assert(0);
    }

    std::string line;
    int bufferIndex = 0;
    std::vector<uint8_t> buffer;
    for (int bufferIndex = 0; bufferIndex < mtd.num_buffer; bufferIndex++) {
        buffer.reserve(mtd.buffer_size[bufferIndex]); // 提前分配空间
        int readbytes = 0;
        while (readbytes < mtd.buffer_size[bufferIndex]) {
            std::getline(file, line);
            for (int i = line.length(); i > 0; i -= 2) {
                std::string hexChars = line.substr(i - 2, 2);
                uint8_t byte         = std::stoi(hexChars, nullptr, 16);
                buffer.push_back(byte);
            }
            readbytes += 4;
        }
        assert(mtd.buffer_size[bufferIndex] == readbytes);
        mem.write(mtd.buffer_base[bufferIndex], buffer.data(), readbytes);
        buffer.clear();
    }
    std::getline(file, line);
    for (const char* ptr = line.c_str(); *ptr != '\0'; ptr++) {
        assert(*ptr == '0');
    }
    assert(file.eof());

    file.close();
}

bool Kernel::kernel_finished() const {
    for (const auto& i : cta_finished) {
        if (i == false)
            return false;
    }
    return true;
}

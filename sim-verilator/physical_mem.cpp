#include "physical_mem.hpp"
#include "verilated_save.h"

bool PhysicalMemory::page_alloc(paddr_t paddr) {
    if (paddr % m_pagesize != 0) {
        logger->warn("PMEM address 0x{:x} is not aligned to page! Align it...", paddr);
        paddr = get_page_base(paddr);
    }
    if (!m_auto_alloc && m_map.find(paddr) != m_map.end()) {
        logger->error("PMEM page at 0x{:x} duplicate allocation", paddr);
        return false;
    }
    m_map[paddr] = new (std::align_val_t(4096)) uint8_t[m_pagesize];
    return true;
}

bool PhysicalMemory::page_free(paddr_t paddr) {
    if (paddr % m_pagesize != 0) {
        logger->warn("PMEM address 0x{:x} is not aligned to page! Align it...", paddr);
        paddr = get_page_base(paddr);
    }
    if (m_map.find(paddr) == m_map.end()) {
        logger->error("PMEM page at 0x{:x} not allocated", paddr);
        return false;
    }
    delete[] m_map[paddr];
    m_map.erase(paddr);
    return true;
}

bool PhysicalMemory::write(paddr_t paddr, const void* data_, const bool mask[], uint64_t size) {
    const uint8_t* data = static_cast<const uint8_t*>(data_);
    paddr_t first_page_base = get_page_base(paddr);
    paddr_t first_page_end = first_page_base + m_pagesize - 1;
    if (paddr + size - 1 > first_page_end) {
        uint64_t size_this_copy = first_page_end - paddr + 1;
        if (!write(first_page_end + 1, data + size_this_copy, mask + size_this_copy, size - size_this_copy))
            return false;
        size = size_this_copy;
    }
    if (m_map.find(first_page_base) == m_map.end()) {
        if (m_auto_alloc) {
            page_alloc(first_page_base);
        } else {
            logger->critical("PMEM page at 0x{:x} not allocated, cannot write", paddr);
            return false;
        }
    }
    uint8_t* buf = m_map.at(first_page_base) + paddr - first_page_base;
    for (uint64_t i = 0; i < size; i++) {
        if (mask[i]) {
            buf[i] = data[i];
        }
    }
    return true;
}

bool PhysicalMemory::write(paddr_t paddr, const void* data_, uint64_t size) {
    const uint8_t* data = static_cast<const uint8_t*>(data_);
    paddr_t first_page_base = get_page_base(paddr);
    paddr_t first_page_end = first_page_base + m_pagesize - 1;
    if (paddr + size - 1 > first_page_end) {
        uint64_t size_this_copy = first_page_end - paddr + 1;
        if (!write(first_page_end + 1, data + size_this_copy, size - size_this_copy))
            return false;
        size = size_this_copy;
    }
    if (m_map.find(first_page_base) == m_map.end()) {
        if (m_auto_alloc) {
            page_alloc(first_page_base);
        } else {
            logger->critical("PMEM page at 0x{:x} not allocated, cannot write", paddr);
            return false;
        }
    }
    uint8_t* buf = m_map.at(first_page_base) + paddr - first_page_base;
    std::memcpy(buf, data, size);
    return true;
}

bool PhysicalMemory::read(paddr_t paddr, void* data_, uint64_t size) const {
    bool success = true;
    uint8_t* data = static_cast<uint8_t*>(data_);
    paddr_t first_page_base = get_page_base(paddr);
    paddr_t first_page_end = first_page_base + m_pagesize - 1;
    if (paddr + size - 1 > first_page_end) {
        uint64_t size_this_copy = first_page_end - paddr + 1;
        success = read(first_page_end + 1, data + size_this_copy, size - size_this_copy);
        size = size_this_copy;
    }
    if (m_map.find(first_page_base) == m_map.end()) {
        logger->error("PMEM page at 0x{:x} not allocated, read as all zero", paddr);
        std::memset(data, 0, size);
        return false;
    }
    uint8_t* buf = m_map.at(first_page_base) + paddr - first_page_base;
    std::memcpy(data, buf, size);
    return success;
}

bool PhysicalMemory::save(VerilatedSerialize& output) const {
    output << m_pagesize << static_cast<uint64_t>(m_auto_alloc) << static_cast<uint64_t>(m_map.size());
    for (const auto& [address, page] : m_map) {
        output << address;
        output.write(page, m_pagesize);
    }
    return true;
}

bool PhysicalMemory::restore(VerilatedDeserialize& input) {
    constexpr uint64_t kMaxRestoredPages = 1ull << 20;
    uint64_t page_size = 0;
    uint64_t auto_alloc = 0;
    uint64_t page_count = 0;
    input >> page_size >> auto_alloc >> page_count;
    if (page_size != m_pagesize || auto_alloc != static_cast<uint64_t>(m_auto_alloc)
        || page_count > kMaxRestoredPages || !m_map.empty()) {
        return false;
    }
    for (uint64_t index = 0; index < page_count; ++index) {
        paddr_t address = 0;
        input >> address;
        if (address % m_pagesize != 0 || m_map.find(address) != m_map.end()) {
            return false;
        }
        if (!page_alloc(address)) {
            return false;
        }
        input.read(m_map.at(address), m_pagesize);
    }
    return true;
}

PhysicalMemory::~PhysicalMemory() {
    if(!m_auto_alloc && !m_map.empty()) {
        logger->warn("PMEM pages not freed before destruction");
    }
    for (auto& [paddr, ptr] : m_map) {
        delete[] ptr;
    }
}

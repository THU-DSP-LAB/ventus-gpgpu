#pragma once

#include <algorithm>
#include <cstdint>
#include <vector>
inline constexpr int MEMACCESS_DATA_BYTE_SIZE = 32 * 4;
inline constexpr uint32_t LDS_BASEADDR        = 0x70000000;

class MemBox {
private:
    const uint32_t PAGESIZE;
    struct MemPage {
        uint8_t* ptr;
        uint32_t addr;
    };
    std::vector<struct MemPage> m_pages;
    uint32_t m_page_cnt;
    MemPage* page_new(uint32_t addr);
    MemPage* page_find(uint32_t addr);

public:
    MemBox(uint32_t pageSize = 4 * 1024)
        : PAGESIZE(pageSize)
        , m_page_cnt(0) {
        m_pages.reserve(32);
        MemPage emptyPage = { .ptr = nullptr, .addr = 0 };
        std::fill(m_pages.begin(), m_pages.end(), emptyPage);
    }

public:
    bool read(uint32_t addr, uint8_t buffer[], int len = MEMACCESS_DATA_BYTE_SIZE);
    void write(uint32_t addr, bool mask[], uint8_t data[], int len = MEMACCESS_DATA_BYTE_SIZE);
    void write(uint32_t addr, uint8_t data[], int len = MEMACCESS_DATA_BYTE_SIZE);
};

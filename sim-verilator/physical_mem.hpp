#pragma once

#include <cstdint>
#include <map>
#include <memory>
#include <spdlog/logger.h>

typedef uint64_t paddr_t;

class PhysicalMemory {
public:
    PhysicalMemory() {}
    PhysicalMemory(bool auto_alloc, uint64_t pagesize, std::shared_ptr<spdlog::logger> logger_)
        : m_auto_alloc(auto_alloc)
        , m_pagesize(pagesize)
        , logger(logger_) { }
    ~PhysicalMemory();

    bool page_alloc(paddr_t paddr);
    bool page_free(paddr_t paddr);
    bool write(paddr_t paddr, const void* data, const bool mask[], uint64_t size);
    bool write(paddr_t paddr, const void* data, uint64_t size);
    bool read(paddr_t paddr, void* data, uint64_t size) const ;
    inline paddr_t get_page_base(paddr_t paddr) const { return paddr - paddr % m_pagesize; }

private:
    const bool m_auto_alloc = false;
    const uint64_t m_pagesize = 4096;
    std::shared_ptr<spdlog::logger> logger = nullptr;

    std::map<paddr_t, uint8_t*> m_map;
};

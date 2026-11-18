#pragma once

#include <cstdint>
#include <map>
#include <memory>
#include <spdlog/logger.h>

typedef uint64_t paddr_t;

enum class PmemRegionKind : uint32_t {
    Buffer = 0,
    Pds = 1,
};

enum class PmemMissingReadKind : uint32_t {
    ColdPage = 0,
    PdsColdPage = 1,
    AllocationPadding = 2,
    OutOfBounds = 3,
};

struct PmemRegion {
    paddr_t base = 0;
    uint64_t requested_size = 0;
    uint64_t allocated_size = 0;
    PmemRegionKind kind = PmemRegionKind::Buffer;
    uint64_t allocation_id = 0;
};

struct PmemMissingReadStats {
    uint64_t cold_page = 0;
    uint64_t pds_cold_page = 0;
    uint64_t allocation_padding = 0;
    uint64_t out_of_bounds = 0;
};

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
    bool read_d2h(paddr_t paddr, void* data, uint64_t size) const;
    inline paddr_t get_page_base(paddr_t paddr) const { return paddr - paddr % m_pagesize; }
    bool region_register(const PmemRegion& region);
    bool region_unregister(paddr_t base, uint64_t allocation_id);
    PmemMissingReadKind classify_missing_read(paddr_t paddr, uint64_t size) const;
    PmemMissingReadStats missing_read_stats() const { return m_missing_read_stats; }

private:
    static bool range_end(paddr_t base, uint64_t size, paddr_t& end);
    const PmemRegion* find_region(paddr_t paddr, uint64_t size) const;
    void record_missing_read(PmemMissingReadKind kind) const;

    const bool m_auto_alloc = false;
    const uint64_t m_pagesize = 4096;
    std::shared_ptr<spdlog::logger> logger = nullptr;

    std::map<paddr_t, uint8_t*> m_map;
    std::map<paddr_t, PmemRegion> m_regions;
    mutable PmemMissingReadStats m_missing_read_stats;
};

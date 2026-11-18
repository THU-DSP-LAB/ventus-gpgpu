#pragma once

#include "ventus_rtlsim.h"

#include <sstream>
#include <stdexcept>

inline void register_standalone_pmem_regions(
    ventus_rtlsim_t* sim, const ventus_kernel_metadata_t& metadata) {
    if (sim == nullptr
        || (metadata.num_buffer != 0
            && (metadata.buffer_base == nullptr
                || metadata.buffer_allocsize == nullptr))) {
        throw std::runtime_error("invalid standalone PMEM region metadata");
    }

    for (uint64_t i = 0; i < metadata.num_buffer; ++i) {
        const paddr_t base = metadata.buffer_base[i];
        const uint64_t allocated_size = metadata.buffer_allocsize[i];
        if (allocated_size == 0) {
            continue;
        }

        // Standalone metadata records initialized bytes and the allocation envelope,
        // but not the allocator's requested size. Treat the full envelope as valid.
        const auto kind = metadata.pdsBaseAddr != 0 && base == metadata.pdsBaseAddr
            ? VENTUS_PMEM_REGION_PDS
            : VENTUS_PMEM_REGION_BUFFER;
        if (!ventus_rtlsim_pmem_region_register(
                sim, base, allocated_size, allocated_size, kind, base)) {
            std::ostringstream message;
            message << "failed to register standalone PMEM region at 0x"
                    << std::hex << base << " (size 0x" << allocated_size << ')';
            throw std::runtime_error(message.str());
        }
    }
}

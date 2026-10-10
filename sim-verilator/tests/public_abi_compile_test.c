#include "ventus_rtlsim.h"

#include <stddef.h>

_Static_assert(VENTUS_PMEM_REGION_BUFFER == 0, "buffer region ABI changed");
_Static_assert(VENTUS_PMEM_REGION_PDS == 1, "PDS region ABI changed");
_Static_assert(
    sizeof(ventus_pmem_missing_read_stats_t) == 4 * sizeof(uint64_t),
    "PMEM statistics ABI changed"
);
_Static_assert(
    offsetof(ventus_pmem_missing_read_stats_t, cold_page) == 0,
    "cold-page counter offset changed"
);
_Static_assert(
    offsetof(ventus_pmem_missing_read_stats_t, pds_cold_page) == sizeof(uint64_t),
    "PDS cold-page counter offset changed"
);
_Static_assert(
    offsetof(ventus_pmem_missing_read_stats_t, allocation_padding) == 2 * sizeof(uint64_t),
    "allocation-padding counter offset changed"
);
_Static_assert(
    offsetof(ventus_pmem_missing_read_stats_t, out_of_bounds) == 3 * sizeof(uint64_t),
    "out-of-bounds counter offset changed"
);

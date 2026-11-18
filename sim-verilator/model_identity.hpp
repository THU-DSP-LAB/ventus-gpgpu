#pragma once

#include <cstdint>

struct VentusRtlModelIdentity {
    const char* fingerprint;
    const char* variant;
    bool release_enabled;
    bool savable_enabled;
    bool trace_enabled;
    bool gvm_enabled;
    std::uint32_t model_threads;
};

extern const VentusRtlModelIdentity kVentusRtlModelIdentity;

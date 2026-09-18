// Exercise the actual CIRCT-emitted memory, not the legacy ChiselTest emitter.
#include "VRtLocalSram.h"
#include <cstdio>

int main() {
    VRtLocalSram dut;
    auto step = [&]() { dut.clock = 0; dut.eval(); dut.clock = 1; dut.eval(); dut.clock = 0; dut.eval(); };
    dut.reset = 1;
    dut.io_request_valid = 0;
    dut.io_response_ready = 0;
    step(); step();
    dut.reset = 0;
    dut.io_request_valid = 1;
    dut.io_request_bits_warpId = 3;
    dut.io_request_bits_fieldId = 33;
    dut.io_request_bits_write = 1;
    dut.io_request_bits_laneMask = 1;
    dut.io_request_bits_writeData_0 = 0x410baa38;
    step();
    dut.io_request_bits_write = 0;
    step();
    // A different warp requests a write while the first response is stalled.
    dut.io_request_bits_warpId = 4;
    dut.io_request_bits_fieldId = 5;
    dut.io_request_bits_write = 1;
    dut.io_request_bits_writeData_0 = 0xdeadbeef;
    for (int cycle = 0; cycle < 5; ++cycle) {
        dut.eval();
        std::printf("stall=%d valid=%d request_ready=%d data=%08x\n", cycle,
                    dut.io_response_valid, dut.io_request_ready, dut.io_response_bits_readData_0);
        if (!dut.io_response_valid || dut.io_request_ready || dut.io_response_bits_readData_0 != 0x410baa38)
            return 1;
        step();
    }
    dut.io_response_ready = 1;
    step();
    if (!dut.io_request_ready || dut.io_response_valid) return 2;
    step(); // accept the other warp's pending write
    dut.io_request_bits_write = 0;
    step(); // read that row back
    dut.io_request_valid = 0;
    dut.eval();
    if (!dut.io_response_valid || dut.io_response_bits_readData_0 != 0xdeadbeef) return 3;
    step();
    dut.io_request_bits_warpId = 3;
    dut.io_request_bits_fieldId = 33;
    dut.io_request_valid = 1;
    step();
    if (!dut.io_response_valid || dut.io_response_bits_readData_0 != 0x410baa38) return 4;
    dut.final();
    std::puts("PASS: stalled read preserved; subsequent cross-warp write isolated");
    return 0;
}

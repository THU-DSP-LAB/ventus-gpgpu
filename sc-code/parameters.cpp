#include "parameters.h"

// extern const int num_warp = 4;
// extern constexpr int depth_warp = 2; //
// extern const int xLen = 32;
// extern constexpr long unsigned int num_thread = 8;
// extern const int ireg_bitsize = 9;
// extern constexpr int ireg_size = 1 << ireg_bitsize;
// extern const int INS_LENGTH = 32; // the length of per instruction
// extern constexpr int PERIOD = 10;


uint32_t extractBits32(uint32_t number, int start, int end) {
    return (number >> end) & ((1 << (start - end + 1)) - 1);
}

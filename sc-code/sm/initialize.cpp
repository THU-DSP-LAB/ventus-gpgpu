#include "BASE.h"

void BASE::INIT_INS(int warp_id)
{
    WARPS[warp_id].ireg[0] = I_TYPE(vaddvv_, warp_id + 3, 1, 2);
    WARPS[warp_id].ireg[1] = I_TYPE(add_, 1, 3, 6);
    WARPS[warp_id].ireg[2] = I_TYPE(lw_, 4, 1, 9);
    WARPS[warp_id].ireg[3] = I_TYPE(vload_, 3, 9, 4);
    WARPS[warp_id].ireg[4] = I_TYPE(vaddvx_, 7, 4, 5);
    WARPS[warp_id].ireg[5] = I_TYPE(vaddvx_, 2, 1, 4 + warp_id);
    WARPS[warp_id].ireg[6] = I_TYPE(vfadd_, 3, 4, 2);
    WARPS[warp_id].ireg[7] = I_TYPE(beq_, 6, 7, -5);
    WARPS[warp_id].ireg[8] = I_TYPE(vaddvv_, 0, 1, 2);
    WARPS[warp_id].ireg[9] = I_TYPE(add_, 1, 3, 3);
    WARPS[warp_id].ireg[10] = I_TYPE(lw_, 0, 3, 9);
    WARPS[warp_id].ireg[11] = I_TYPE(vload_, 3, 8, 4);
    WARPS[warp_id].ireg[12] = I_TYPE(vload_, 3, 8, 5);
    WARPS[warp_id].ireg[13] = I_TYPE(vload_, 3, 8, 6);
    WARPS[warp_id].ireg[14] = I_TYPE(vload_, 3, 8, 7);
    WARPS[warp_id].ireg[15] = I_TYPE(vload_, 3, 8, 8);
    WARPS[warp_id].ireg[16] = I_TYPE(vload_, 3, 8, 9);
    WARPS[warp_id].ireg[17] = I_TYPE(vload_, 3, 8, 10);
    WARPS[warp_id].ireg[18] = I_TYPE(vload_, 3, 8, 11);
}

void BASE::INIT_REG(int warp_id)
{
    WARPS[warp_id].s_regfile[0] = 44;
    WARPS[warp_id].s_regfile[1] = -10;
    WARPS[warp_id].s_regfile[2] = 666;
    WARPS[warp_id].s_regfile[3] = 32;
    WARPS[warp_id].s_regfile[4] = 4;
    WARPS[warp_id].s_regfile[5] = 888;
    WARPS[warp_id].s_regfile[6] = 6;
    WARPS[warp_id].s_regfile[7] = 22;
    WARPS[warp_id].v_regfile[0].fill(1);
    WARPS[warp_id].v_regfile[1].fill(3);
    WARPS[warp_id].v_regfile[2].fill(-10);
    WARPS[warp_id].v_regfile[3].fill(7);
    WARPS[warp_id].v_regfile[4].fill(-1);
    WARPS[warp_id].v_regfile[5].fill(10);
    WARPS[warp_id].v_regfile[6].fill(8);
    WARPS[warp_id].v_regfile[7].fill(1);
}

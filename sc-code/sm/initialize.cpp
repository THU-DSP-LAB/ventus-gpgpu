#include "BASE.h"

void BASE::INIT_INS()
{
    ireg[0] = I_TYPE(vaddvv_, 0, 1, 2);
    ireg[1] = I_TYPE(add_, 1, 3, 0);
    ireg[2] = I_TYPE(lw_, 0, 3, 9);
    ireg[3] = I_TYPE(vload_, 3, 9, 4);
    ireg[4] = I_TYPE(vaddvx_, 7, 4, 5);
    ireg[5] = I_TYPE(vaddvx_, 0, 1, 4);
    ireg[6] = I_TYPE(vfadd_, 3, 4, 2);
    ireg[7] = I_TYPE(beq_, 0, 7, -5);

    ireg[8] = I_TYPE(vaddvv_, 0, 1, 2);
    ireg[9] = I_TYPE(add_, 1, 3, 3);
    ireg[10] = I_TYPE(lw_, 0, 3, 9);
    ireg[11] = I_TYPE(vload_, 3, 9, 4);
}

void BASE::INIT_REG()
{
    s_regfile[0] = 44;
    s_regfile[1] = -10;
    s_regfile[2] = 666;
    s_regfile[3] = 32;
    s_regfile[4] = 10;
    s_regfile[5] = 888;
    s_regfile[6] = 6;
    s_regfile[7] = 22;
    v_regfile[0].fill(1);
    v_regfile[1].fill(3);
    v_regfile[2].fill(-10);
    v_regfile[3].fill(7);
    v_regfile[4].fill(-1);
    v_regfile[5].fill(10);
    v_regfile[6].fill(8);
    v_regfile[7].fill(1);
    f_regfile[0].fill(1);
    f_regfile[1].fill(5.20);
    f_regfile[2].fill(-0.3);
    f_regfile[3].fill(3.14);
    f_regfile[4].fill(18.99);
}



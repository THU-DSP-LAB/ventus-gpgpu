#include "sm.h"

void sm::fetch()
{
    if (rst_n.read() == 0)
        pc = 0;
    else if (ibuf_full == false)
    {
        if (jump == 1)
            pc = jump_addr + pc;
        else
            pc = pc + 1;
        fetch_ins = ireg[pc];
        fetch_out.notify();
    }
}
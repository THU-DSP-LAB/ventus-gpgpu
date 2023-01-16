#include "fetch.h"

void fetch::PROGRAM_COUNTER()
{
    if (rst_n.read() == 0)
        pc = 0;
    else if (ibuf_full.read() == false)
    {
        if (jump.read() == 1)
            pc = jump_addr.read() + pc.read();
            // later work: should ensure pc valid
        else
            pc = pc.read() + 1;
        fetch_out->notify();
    }
}

void fetch::INSTRUCTION_REG()
{
    // initialize
    ireg[0] = I_TYPE(add_, 0, 1, 2);

    // read instuction reg
    while (true)
    {
        fetch_ins = ireg[pc.read()];
        wait(pc.value_changed_event());
    }
}
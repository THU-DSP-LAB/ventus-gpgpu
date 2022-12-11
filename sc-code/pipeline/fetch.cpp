#include "fetch.h"

void fetch::PROGRAM_COUNTER()
{
    if (rst_n.read() == 0)
        pc = 0;
    else if (ibuf_full.read() == 0)
    {
        if (jump.read() == 1)
            pc = jump_addr.read() + 1;
        else
            pc = pc.read() + 1;
        fetch_sendout->notify();
    }
}

void fetch::INSTRUCTION_REG()
{
    // initialize
    ;
    // read instuction reg
    while (true)
    {
        fetch_instruction = ireg[pc.read()];
        wait();
    }
}
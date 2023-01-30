#include "fetch.h"

void fetch::PROGRAM_COUNTER()
{
    if (rst_n.read() == 0)
        pc = -1;
    else if (ibuf_full.read() == false)
    {
        if (jump.read() == 1)
        {
            pc = jump_addr.read() + pc.read();
            cout << "jump=" << jump.read() << " at time " << sc_time_stamp() << endl;
        }

        // later work: should ensure pc valid
        else
            pc = pc.read() + 1;
        fetch_out->notify(PERIOD);
    }
}

void fetch::INSTRUCTION_REG()
{
    // initialize
    ireg[0] = I_TYPE(lw_, 0, 1, 2);
    ireg[1] = I_TYPE(add_, 0, 1, 2);
    ireg[2] = I_TYPE(vload_, 0, 1, 2);
    ireg[3] = I_TYPE(vaddvv_, 0, 1, 2);
    ireg[4] = I_TYPE(vaddvx_, 0, 1, 2);
    ireg[5] = I_TYPE(vfadd_, 0, 1, 2);
    ireg[6] = I_TYPE(beq_, 0, 1, 2);
    for (int i = 0; i < 7; i++)
        cout << "init: ireg[" << i << "] is initialized as " << ireg[i] << endl;

    cout << "now ireg[7] isn't initialized, it is " << ireg[7] << endl;

    // read instuction reg
    while (true)
    {
        fetch_ins = (pc.read() >= 0) ? ireg[pc.read()] : I_TYPE(INVALID_, 0, 0, 0);
        cout << "now pc=" << pc << ", fetch_ins is " << fetch_ins << " at time " << sc_time_stamp()
             << ", it will be " << ireg[pc.read()] << " at the next timestamp" << endl;
        wait(pc.value_changed_event());
    }
}
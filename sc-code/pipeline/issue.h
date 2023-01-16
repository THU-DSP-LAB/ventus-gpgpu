#ifndef _ISSUE_H
#define _ISSUE_H

#include "../parameters.h"

SC_MODULE(issue)
{
    sc_in_clk clk;
    sc_in<bool> can_dispatch; // from scoreboard
    sc_in<I_TYPE> ibuf_ins;
    sc_in<bool> opc_full;

    sc_out<I_TYPE> issue_ins;
    sc_port<event_if> dispatch; // to ibuffer

    void issue_action()
    {
        if (can_dispatch && opc_full == false)
        {
            issue_ins = ibuf_ins;
            dispatch->notify();
        }
    }

    SC_CTOR(issue)
    {
        SC_METHOD(issue_action);
        sensitive << clk.pos();
    }
};
#endif
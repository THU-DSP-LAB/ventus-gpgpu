#ifndef _IBUFFER_H
#define _IBUFFER_H

#include "systemc.h"
#include "parameters.h"

SC_MODULE(ibuffer)
{
    sc_in<bool> rst_n;
    sc_in<I_TYPE> fetch_instruction;
    sc_port<event_if> fetch_out;
    sc_port<event_if> read_ins;

    sc_out<I_TYPE> ibuf_instruction;
    sc_out<bool> ibuf_full;
    sc_port<event_if> emit_ins;

    void IBUF_ACTION();

    SC_CTOR(ibuffer)
    {
        SC_THREAD(IBUF_ACTION);
        sc_fifo<I_TYPE> ififo(10);
    }

private:
    sc_fifo<I_TYPE> ififo;
};

#endif
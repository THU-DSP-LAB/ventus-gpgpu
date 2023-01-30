#ifndef IBUFFER_STI_H_
#define IBUFFER_STI_H_

#include "../parameters.h"

SC_MODULE(ibuffer_sti)
{
    sc_out<bool> rst_n;
    sc_out<I_TYPE> fetch_ins;
    sc_port<event_if> fetch_out;
    sc_port<event_if> dispatch; // from issue

    sc_in<I_TYPE> ibuf_ins;
    sc_in<bool> ibuf_full; // to fetch
    void gen_sti()
    {
    }

    void display()
    {
    }

    SC_CTOR(ibuffer_sti)
    {
        SC_THREAD(gen_sti);
        SC_THREAD(display);
    }
};

#endif

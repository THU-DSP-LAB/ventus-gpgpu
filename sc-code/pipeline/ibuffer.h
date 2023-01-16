#ifndef _IBUFFER_H
#define _IBUFFER_H

#include "../parameters.h"
#include "tlm.h"
#include "tlm_core/tlm_1/tlm_req_rsp/tlm_channels/tlm_fifo/tlm_fifo.h"

SC_MODULE(ibuffer)
{
    sc_in<bool> rst_n;
    sc_in<I_TYPE> fetch_ins;
    sc_port<event_if> fetch_out;
    sc_port<event_if> dispatch;         // from issue

    sc_out<I_TYPE> ibuf_ins;
    sc_out<bool> ibuf_full;             // to fetch

    void IBUF_ACTION();

    SC_CTOR(ibuffer)
    {
        SC_THREAD(IBUF_ACTION);
        tlm::tlm_fifo<I_TYPE> ififo(10);
    }

private:
    tlm::tlm_fifo<I_TYPE> ififo;
    // N0T use sc_fifo, since it can't read
    // the top data without poping it out
};

#endif
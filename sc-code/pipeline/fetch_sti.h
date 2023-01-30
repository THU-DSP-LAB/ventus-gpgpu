#ifndef FETCH_STI_H_
#define FETCH_STI_H_

#include "../parameters.h"

SC_MODULE(fetch_sti)
{
    sc_in_clk clk;
    sc_in<I_TYPE> fetch_ins{"fetch_ins"};
    sc_port<event_if> fetch_out{"fetch_out"};

    sc_out<bool> rst_n{"rst_n"};
    sc_out<bool> ibuf_full{"ibuf_full"};
    sc_out<bool> jump{"jump"};
    sc_out<sc_int<ireg_bitsize + 1>> jump_addr{"jump_addr"};
    sc_out<bool> fetch_out_eventsig;

    void gen_sti()
    {
        rst_n = 0;
        wait(5, SC_NS);
        rst_n = 1;
        ibuf_full = 0;
        jump = 0;
        wait(20, SC_NS);
        wait(clk.posedge_event());
        jump = 1;
        jump_addr = 2;
        wait(SC_ZERO_TIME);
        wait(clk.posedge_event());
        jump = 0;
    }

    void display()
    {
        while (true)
        {
            cout << "sti: rst_n=" << rst_n
                 << ", ibuf_full=" << ibuf_full
                 << ", jump=" << jump
                 << ", jump_addr=" << jump_addr
                 << ", fetch_out triggered " << (fetch_out->obtain_event().triggered() ? "yes" : "no")
                 << ", fetch_ins=" << fetch_ins
                 << ", at time " << sc_time_stamp() << endl;
            wait(fetch_ins.value_changed_event() |
                 fetch_out->obtain_event() |
                 rst_n.value_changed_event() |
                 ibuf_full.value_changed_event() |
                 jump.value_changed_event() |
                 jump_addr.value_changed_event());
        }
    }

    void event_sig(){
        fetch_out_eventsig = 0;
        while (true)
        {
            wait(fetch_out->obtain_event());
            fetch_out_eventsig = 1;
            wait(1, SC_NS);
            fetch_out_eventsig =0;
        }
        
    }

    SC_CTOR(fetch_sti)
    {
        SC_THREAD(gen_sti);
        SC_THREAD(display);
        SC_THREAD(event_sig);
    }
};

#endif
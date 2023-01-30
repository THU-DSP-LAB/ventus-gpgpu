#ifndef HELLO_STI_H_
#define HELLO_STI_H_

#include "systemc.h"
#include "../parameters.h"

SC_MODULE(hello_sti)
{
    sc_in_clk clk;
    sc_out<bool> boolnum{"Boolnum"};
    sc_out<int> intnum{"Intnum"};
    sc_out<I_TYPE> ins{"ins"};

    void gen_sti()
    {
        wait(clk.negedge_event());
        ins = I_TYPE(add_, 1, 2, 3);
        boolnum = false;
        intnum = 10;
        wait(30, SC_NS);
        ins = I_TYPE(vaddvv_, 4, 5, 6);
        boolnum = true;
        intnum = 30;
        wait(clk.posedge_event());
        boolnum = false;
        intnum = 100;
    }

    void display()
    {
        cout << "boolnum=" << boolnum << ", intnum=" << intnum
             << ", ins=" << ins << ", at time " << sc_time_stamp() << endl;
    }

    SC_CTOR(hello_sti)
    {
        SC_THREAD(gen_sti);
        SC_METHOD(display);
        sensitive << boolnum << intnum;
    }
};

#endif
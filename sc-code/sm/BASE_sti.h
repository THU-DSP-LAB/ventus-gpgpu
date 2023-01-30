#ifndef BASE_STI_H_
#define BASE_STI_H_

#include "../parameters.h"

SC_MODULE(BASE_sti)
{
    sc_out<bool> rst_n{"rst_n"};
    void gen_sti()
    {
        rst_n = 0;
        wait(10, SC_NS);
        rst_n = 1;
    }
    SC_CTOR(BASE_sti)
    {
        SC_THREAD(gen_sti);
    }
};

#endif
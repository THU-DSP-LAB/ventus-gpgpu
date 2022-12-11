#ifndef _FETCH_H
#define _FETCH_H

#include "systemc.h"
#include "parameters.h"

SC_MODULE(fetch){
    sc_in_clk clk;
    sc_in<bool> rst_n;
    sc_in<bool> ibuf_full;
    sc_in<bool> jump;
    sc_in<sc_uint<9>> jump_addr; 

    sc_out<I_TYPE> fetch_instruction;
    sc_port<event_if> fetch_sendout;

    sc_signal<sc_uint<ireg_bitsize>> pc;
    I_TYPE ireg [ireg_size];

    void PROGRAM_COUNTER();
    void INSTRUCTION_REG();
    SC_CTOR(fetch){
        SC_METHOD(PROGRAM_COUNTER);
        sensitive << clk.pos() << rst_n.neg();
        SC_THREAD(INSTRUCTION_REG);
        sensitive << pc;
    }
};



#endif

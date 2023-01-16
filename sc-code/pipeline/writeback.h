#ifndef _WRITEBACK_H
#define _WRITEBACK_H

#include "../parameters.h"

SC_MODULE(writeback){
    sc_in<salu_out_t> salu_out;
    sc_port<event_if> salu_finish;

    sc_out<reg_t> rds1_data;
    sc_out<sc_uint<5>> rds1_addr;
    sc_port<event_if> write_s;

    sc_port<event_if> wb_event;     // 只要write就触发，用于通知scoreboard
    sc_out<I_TYPE> wb_ins;          // to scoreboard

    void write_scalar();
    SC_CTOR(writeback){
        SC_THREAD(write_scalar);
    }
};

#endif
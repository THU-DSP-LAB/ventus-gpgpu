#ifndef _REGFILE_H
#define _REGFILE_H

#include "systemc.h"
#include "parameters.h"

SC_MODULE(regfile)
{
    sc_in<sc_uint<5>> r_s_addr; // r-read, s-scalar
    sc_in<sc_uint<5>> r_v_addr;
    sc_in<sc_uint<5>> w_s_addr;
    sc_in<sc_uint<5>> w_v_addr;
    sc_in<sc_uint<32>> w_s_data;
    sc_in<sc_uint<32>> w_v_data[num_thread];
    sc_port<event_if> write_s;
    sc_port<event_if> write_v;
    // CSR还没加
    sc_out<sc_uint<32>> r_s_data;
    sc_out<sc_uint<32>> r_v_data[num_thread];

    sc_uint<32> s_regfile[32];
    sc_uint<32> v_regfile[32][num_thread];

    void read_scalar();
    void read_vector();
    void write_scalar();
    void write_vector();
    SC_CTOR(regfile)
    {
    }
};

#endif
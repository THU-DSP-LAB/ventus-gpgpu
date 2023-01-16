#ifndef _OPERANDCOLLECTOR_H
#define _OPERANDCOLLECTOR_H

#include "../parameters.h"

SC_MODULE(OperandCollector)
{
    sc_port<event_if> dispatch;
    sc_in<I_TYPE> ibuf_ins;
    sc_in<sc_int<32>> rss1_data;
    sc_in<sc_int<32>> rss2_data;
    sc_vector<sc_in<reg_t>> rsv1_data{"rsv1_data", num_thread};
    sc_vector<sc_in<reg_t>> rsv2_data{"rsv2_data", num_thread};
    sc_vector<sc_in<float>> rsf1_data{"rsf1_data", num_thread};
    sc_vector<sc_in<float>> rsf2_data{"rsf2_data", num_thread};

    sc_out<sc_uint<5>> rss1_addr;
    sc_out<sc_uint<5>> rss2_addr;
    sc_out<sc_uint<5>> rsv1_addr;
    sc_out<sc_uint<5>> rsv2_addr;
    sc_out<sc_uint<5>> rsf1_addr;
    sc_out<sc_uint<5>> rsf2_addr;
    sc_port<event_if> emito_salu;
    sc_port<event_if> emito_valu;
    sc_port<event_if> emito_vfpu;
    sc_port<event_if> emito_lsu;
    sc_out<I_TYPE> opcins_salu;
    sc_out<I_TYPE> opcins_valu;
    sc_out<I_TYPE> opcins_vfpu;
    sc_out<I_TYPE> opcins_lsu;
    sc_out<reg_t> salu_data1;
    sc_out<reg_t> salu_data2;
    sc_vector<sc_out<reg_t>> valu_data1{"valu_data1", num_thread};
    sc_vector<sc_out<reg_t>> valu_data2{"valu_data2", num_thread};
    sc_vector<sc_out<float>> vfpu_data1{"vfpu_data1", num_thread};
    sc_vector<sc_out<float>> vfpu_data2{"vfpu_data2", num_thread};

    sc_out<bool> opc_full;
    sc_out<bool> jump;
    sc_port<event_if> flush; // same as jump
    sc_out<sc_int<ireg_bitsize + 1>> jump_addr;

    void fifoin();
    void fifoout();
    void saluController();
    void valuController();
    void vfpuController();
    void lsuController();

    SC_CTOR(OperandCollector)
    {
        SC_THREAD(fifoin);
        SC_THREAD(fifoout);
        SC_THREAD(saluController);
        SC_THREAD(valuController);
        SC_THREAD(vfpuController);
        SC_THREAD(lsuController);
    }

private:
    tlm::tlm_fifo<I_TYPE> opcfifo;
    sc_signal<bool> salu_ready;
    sc_signal<bool> valu_ready;
    sc_signal<bool> vfpu_ready;
    sc_signal<bool> lsu_ready;
};

#endif
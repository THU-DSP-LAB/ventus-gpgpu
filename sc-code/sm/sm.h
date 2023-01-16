#ifndef _SM_H
#define _SM_H

#include "../parameters.h"

SC_MODULE(sm)
{
    sc_in_clk clk;
    sc_in<bool> rst_n;

    void fetch();
    void ibuffer();
    void scoreboard();
    void issue();
    void regfile();
    void opc();
    void alu();
    void writeback();

    SC_CTOR(sm)
    {
        SC_METHOD(fetch);
        sensitive << clk.pos();
        SC_THREAD(ibuffer);
        SC_THREAD(scoreboard);
        SC_METHOD(issue);
        sensitive << clk.pos();
        SC_THREAD(opc);
        SC_THREAD(regfile);
        SC_THREAD(alu);
        SC_THREAD(writeback);
    }

private:
    // fetch
    I_TYPE ireg[ireg_size];
    sc_uint<ireg_bitsize> pc;
    bool ibuf_full;
    bool jump;
    sc_uint<9> jump_addr;
    sc_event fetch_out;
    I_TYPE fetch_ins;
    // ibuffer
    tlm::tlm_fifo<I_TYPE> ififo;
    sc_event dispatch;
    I_TYPE ibuf_ins;
    // scoreboard
    sc_event writeback;
    std::set<SCORE_TYPE> score; // record regfile that's to be written
    bool can_dispatch;
    I_TYPE wb_ins;
    // issue
    bool opc_full;
    I_TYPE issue_ins;
    // regfile
    sc_int<32> s_regfile[32];
    sc_int<32> v_regfile[32][num_thread];
    float f_regfile[32][num_thread];
    sc_uint<5> rss1_addr, rss2_addr, rsv1_addr, rsv2_addr,
        rsf1_addr, rsf2_addr;
    sc_uint<5> rds_addr, rdv_addr, rdf_addr;
    sc_event write_s, write_v, write_f;
    sc_int<32> rss1_data, rss2_data;
    sc_int<32> *rsv1_data, *rsv2_data;
    float *rsf1_data, *rsf2_data;
    
};

#endif

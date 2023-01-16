#include "../parameters.h"
#include "fetch.h"
#include "ibuffer.h"
#include "issue.h"
#include "OperandCollector.h"
#include "regfile.h"
#include "sALU.h"
#include "scoreboard.h"
#include "writeback.h"

int sc_main(int argc, char *argv[])
{
    fetch fetch_impl("fetch");
    ibuffer ibuffer_impl("ibuffer");
    issue issue_impl("issue");
    OperandCollector opc_impl("OperandCollector");
    regfile regfile_impl("regfile");
    sALU sALU_impl("sALU", sc_time(30, SC_NS));
    scoreboard scoreboard_impl("scoreboard");
    writeback wb_impl("writeback");

    // fetch_impl
    sc_clock clk("clk", PERIOD, SC_NS);
    sc_signal<bool> rst_n;
    sc_signal<bool> ibuf_full, jump;
    sc_signal<sc_int<ireg_bitsize + 1>> jump_addr;
    sc_signal<I_TYPE> fetch_ins;
    event fetch_out("fetch_out");
    fetch_impl.clk(clk);
    fetch_impl.rst_n(rst_n);
    fetch_impl.ibuf_full(ibuf_full);
    fetch_impl.jump(jump);
    fetch_impl.jump_addr(jump_addr);
    fetch_impl.fetch_ins(fetch_ins);
    fetch_impl.fetch_out(fetch_out);

    // ibuffer_impl
    event dispatch("dispatch");
    sc_signal<I_TYPE> ibuf_ins;
    ibuffer_impl.rst_n(rst_n);
    ibuffer_impl.fetch_ins(fetch_ins);
    ibuffer_impl.fetch_out(fetch_out);
    ibuffer_impl.dispatch(dispatch);
    ibuffer_impl.ibuf_ins(ibuf_ins);
    ibuffer_impl.ibuf_full(ibuf_full);

    // issue_impl
    sc_signal<bool> can_dispatch, opc_full;
    sc_signal<I_TYPE> issue_ins;
    issue_impl.clk(clk);
    issue_impl.can_dispatch(can_dispatch);
    issue_impl.ibuf_ins(ibuf_ins);
    issue_impl.opc_full(opc_full);
    issue_impl.issue_ins(issue_ins);
    issue_impl.dispatch(dispatch);

    // opc_impl
    sc_signal<sc_int<32>> rss1_data;
    sc_signal<sc_int<32>> rss2_data;
    sc_vector<sc_signal<reg_t>> rsv1_data{"rsv1_data", num_thread};
    sc_vector<sc_signal<reg_t>> rsv2_data{"rsv2_data", num_thread};
    sc_vector<sc_signal<float>> rsf1_data{"rsf1_data", num_thread};
    sc_vector<sc_signal<float>> rsf2_data{"rsf2_data", num_thread};
    sc_signal<sc_uint<5>> rss1_addr;
    sc_signal<sc_uint<5>> rss2_addr;
    sc_signal<sc_uint<5>> rsv1_addr;
    sc_signal<sc_uint<5>> rsv2_addr;
    sc_signal<sc_uint<5>> rsf1_addr;
    sc_signal<sc_uint<5>> rsf2_addr;
    event emito_salu("emito_salu");
    event emito_valu("emito_valu");
    event emito_vfpu("emito_vfpu");
    event emito_lsu("emito_lsu");
    sc_signal<I_TYPE> opcins_salu;
    sc_signal<I_TYPE> opcins_valu;
    sc_signal<I_TYPE> opcins_vfpu;
    sc_signal<I_TYPE> opcins_lsu;
    sc_signal<sc_int<32>> salu_data1;
    sc_signal<sc_int<32>> salu_data2;
    sc_vector<sc_signal<reg_t>> valu_data1{"valu_data1", num_thread};
    sc_vector<sc_signal<reg_t>> valu_data2{"valu_data2", num_thread};
    sc_vector<sc_signal<float>> vfpu_data1{"vfpu_data1", num_thread};
    sc_vector<sc_signal<float>> vfpu_data2{"vfpu_data2", num_thread};
    event flush("flush"); // same as jump
    opc_impl.dispatch(dispatch);
    opc_impl.ibuf_ins(ibuf_ins);
    opc_impl.rss1_data(rss1_data);
    opc_impl.rss2_data(rss2_data);
    opc_impl.rsv1_data(rsv1_data);
    opc_impl.rsv2_data(rsv2_data);
    opc_impl.rsf1_data(rsf1_data);
    opc_impl.rsf2_data(rsf2_data);
    opc_impl.rss1_addr(rss1_addr);
    opc_impl.rss2_addr(rss2_addr);
    opc_impl.rsv1_addr(rsv1_addr);
    opc_impl.rsv2_addr(rsv2_addr);
    opc_impl.rsf1_addr(rsf1_addr);
    opc_impl.rsf2_addr(rsf2_addr);
    opc_impl.emito_salu(emito_salu);
    opc_impl.emito_valu(emito_valu);
    opc_impl.emito_vfpu(emito_vfpu);
    opc_impl.emito_lsu(emito_lsu);
    opc_impl.opcins_salu(opcins_salu);
    opc_impl.opcins_valu(opcins_valu);
    opc_impl.opcins_vfpu(opcins_vfpu);
    opc_impl.opcins_lsu(opcins_lsu);
    opc_impl.salu_data1(salu_data1);
    opc_impl.salu_data2(salu_data2);
    opc_impl.valu_data1(valu_data1);
    opc_impl.valu_data2(valu_data2);
    opc_impl.vfpu_data1(vfpu_data1);
    opc_impl.vfpu_data2(vfpu_data2);
    opc_impl.opc_full(opc_full);
    opc_impl.jump(jump);
    opc_impl.flush(flush);
    opc_impl.jump_addr(jump_addr);

    // regfile_impl
    sc_signal<sc_uint<5>> rds1_addr{"rds1_addr"};
    sc_signal<sc_uint<5>> rdv1_addr{"rdv1_addr"};
    sc_signal<sc_uint<5>> rdf1_addr{"rdf1_addr"};
    sc_signal<reg_t> rds1_data{"rds1_data"};
    sc_vector<sc_signal<reg_t>> rdv1_data{"rdv1_data", num_thread};
    sc_vector<sc_signal<float>> rdf1_data{"rdf1_data", num_thread};
    event write_s("write_s");
    event write_v("write_v");
    event write_f("write_f");
    regfile_impl.rss1_addr(rss1_addr);
    regfile_impl.rss2_addr(rss2_addr);
    regfile_impl.rsv1_addr(rsv1_addr);
    regfile_impl.rsv2_addr(rsv2_addr);
    regfile_impl.rsf1_addr(rsf1_addr);
    regfile_impl.rsf2_addr(rsf2_addr);
    regfile_impl.rds1_addr(rds1_addr);
    regfile_impl.rdv1_addr(rdv1_addr);
    regfile_impl.rdf1_addr(rdf1_addr);
    regfile_impl.rds1_data(rds1_data);
    regfile_impl.rdv1_data(rdv1_data);
    regfile_impl.rdf1_data(rdf1_data);
    regfile_impl.write_s(write_s);
    regfile_impl.write_v(write_v);
    regfile_impl.write_f(write_f);
    regfile_impl.rss1_data(rss1_data);
    regfile_impl.rss2_data(rss2_data);
    regfile_impl.rsv1_data(rsv1_data);
    regfile_impl.rsv2_data(rsv2_data);
    regfile_impl.rsf1_data(rsf1_data);
    regfile_impl.rsf2_data(rsf2_data);

    // salu_impl
    event salu_finish("salu_finish");
    sc_signal<salu_out_t> salu_out;
    sALU_impl.opcins(opcins_salu);
    sALU_impl.rss1_data(salu_data1);
    sALU_impl.rss2_data(salu_data2);
    sALU_impl.emito_salu(emito_salu);
    sALU_impl.finish(salu_finish);
    sALU_impl.out(salu_out);

    // scoreboard_impl
    event wb_event("wb_event");
    sc_signal<I_TYPE> wb_ins;
    scoreboard_impl.top_ins(ibuf_ins);
    scoreboard_impl.dispatch(dispatch);
    scoreboard_impl.wb_event(wb_event);
    scoreboard_impl.wb_ins(wb_ins);
    scoreboard_impl.can_dispatch(can_dispatch);

    // wb_impl
    wb_impl.salu_out(salu_out);
    wb_impl.salu_finish(salu_finish);
    wb_impl.rds1_data(rds1_data);
    wb_impl.rds1_addr(rds1_addr);
    wb_impl.write_s(write_s);
    wb_impl.wb_event(wb_event);
    wb_impl.wb_ins(wb_ins);

    sc_trace_file *tf = sc_create_vcd_trace_file("Wave");
    tf->set_time_unit(1, SC_NS);
    sc_trace(tf, clk, "Clk");
    sc_trace(tf, rst_n, "Rst_n");
    sc_trace(tf, regfile_impl.s_regfile[0], "s_regfile(0)");
    sc_trace(tf, regfile_impl.s_regfile[1], "s_regfile(1))");
    sc_trace(tf, regfile_impl.s_regfile[2], "s_regfile(2))");

    sc_start();
    rst_n = 0;
    wait(30, SC_NS);
    rst_n = 1;
    wait(100, SC_NS);
    sc_stop();

    return 0;
}
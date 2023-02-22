#include "BASE.h"
#include "BASE_sti.h"

int sc_main(int argc, char *argv[])
{
    BASE BASE_impl("BASE");
    BASE_sti BASE_sti_impl("BASE_STI");

    sc_clock clk("clk", PERIOD, SC_NS, 0.5, 0, SC_NS, false);
    sc_signal<bool> rst_n("rst_n");

    BASE_impl.clk(clk);
    BASE_impl.rst_n(rst_n);

    BASE_sti_impl.rst_n(rst_n);

    sc_trace_file *tf = sc_create_vcd_trace_file("BASE_wave");
    tf->set_time_unit(1, SC_NS);
    sc_trace(tf, clk, "Clk");
    sc_trace(tf, rst_n, "Rst_n");
    sc_trace(tf, BASE_impl.ibuf_full, "ibuf_full");
    sc_trace(tf, BASE_impl.jump, "jump");
    sc_trace(tf, BASE_impl.jump_addr, "jump_addr");
    sc_trace(tf, BASE_impl.fetch_valid, "fetch_valid");
    sc_trace(tf, BASE_impl.pc, "pc");
    sc_trace(tf, BASE_impl.fetch_ins, "fetch_ins");
    sc_trace(tf, BASE_impl.dispatch, "dispatch");
    sc_trace(tf, BASE_impl.ibuf_empty, "ibuf_empty");
    sc_trace(tf, BASE_impl.ibuftop_ins, "ibuftop_ins");
    sc_trace(tf, BASE_impl.ififo_elem_num, "ififo_elem_num");
    sc_trace(tf, BASE_impl.can_dispatch, "can_dispatch");
    sc_trace(tf, BASE_impl.opc_full, "opc_full");
    sc_trace(tf, BASE_impl.issue_ins, "issue_ins");
    sc_trace(tf, BASE_impl.opcfifo_elem_num, "opcfifo_elem_num");
    sc_trace(tf, BASE_impl.opctop_ins, "opctop_ins");
    sc_trace(tf, BASE_impl.emit, "emit");
    sc_trace(tf, BASE_impl.emito_salu, "emito_salu");
    sc_trace(tf, BASE_impl.emito_valu, "emito_valu");
    sc_trace(tf, BASE_impl.emito_vfpu, "emito_vfpu");
    sc_trace(tf, BASE_impl.emito_lsu, "emito_lsu");
    sc_trace(tf, BASE_impl.salu_ready, "salu_ready");
    sc_trace(tf, BASE_impl.rss1_addr, "rss1_addr");
    sc_trace(tf, BASE_impl.rss2_addr, "rss2_addr");
    sc_trace(tf, BASE_impl.rss1_data, "rss1_data");
    sc_trace(tf, BASE_impl.rss2_data, "rss2_data");
    sc_trace(tf, BASE_impl.salufifo_empty, "salufifo_empty");
    sc_trace(tf, BASE_impl.salutop_dat.ins, "salutop_dat.ins");
    sc_trace(tf, BASE_impl.salutop_dat.data, "salutop_dat.data");
    sc_trace(tf, BASE_impl.salufifo_elem_num, "salufifo_elem_num");
    sc_trace(tf, BASE_impl.valu_ready, "valu_ready");
    sc_trace(tf, BASE_impl.rsv1_addr, "rsv1_addr");
    sc_trace(tf, BASE_impl.rsv2_addr, "rsv2_addr");
    sc_trace(tf, BASE_impl.rsv1_data[0], "rsv1_data.data(0)");
    sc_trace(tf, BASE_impl.rsv1_data[1], "rsv1_data.data(1)");
    sc_trace(tf, BASE_impl.rsv1_data[2], "rsv1_data.data(2)");
    sc_trace(tf, BASE_impl.rsv1_data[3], "rsv1_data.data(3)");
    sc_trace(tf, BASE_impl.rsv1_data[4], "rsv1_data.data(4)");
    sc_trace(tf, BASE_impl.rsv1_data[5], "rsv1_data.data(5)");
    sc_trace(tf, BASE_impl.rsv1_data[6], "rsv1_data.data(6)");
    sc_trace(tf, BASE_impl.rsv1_data[7], "rsv1_data.data(7)");
    sc_trace(tf, BASE_impl.rsv2_data[0], "rsv2_data.data(0)");
    sc_trace(tf, BASE_impl.rsv2_data[1], "rsv2_data.data(1)");
    sc_trace(tf, BASE_impl.rsv2_data[2], "rsv2_data.data(2)");
    sc_trace(tf, BASE_impl.rsv2_data[3], "rsv2_data.data(3)");
    sc_trace(tf, BASE_impl.rsv2_data[4], "rsv2_data.data(4)");
    sc_trace(tf, BASE_impl.rsv2_data[5], "rsv2_data.data(5)");
    sc_trace(tf, BASE_impl.rsv2_data[6], "rsv2_data.data(6)");
    sc_trace(tf, BASE_impl.rsv2_data[7], "rsv2_data.data(7)");
    sc_trace(tf, BASE_impl.valufifo_empty, "valufifo_empty");
    sc_trace(tf, BASE_impl.valutop_dat, "valutop_dat");
    sc_trace(tf, BASE_impl.valufifo_elem_num, "valufifo_elem_num");
    sc_trace(tf, BASE_impl.vfpu_ready, "vfpu_ready");
    sc_trace(tf, BASE_impl.rsf1_addr, "rsf1_addr");
    sc_trace(tf, BASE_impl.rsf2_addr, "rsf2_addr");
    sc_trace(tf, BASE_impl.rsf1_data[0], "rsf1_data.data(0)");
    sc_trace(tf, BASE_impl.rsf1_data[1], "rsf1_data.data(1)");
    sc_trace(tf, BASE_impl.rsf1_data[2], "rsf1_data.data(2)");
    sc_trace(tf, BASE_impl.rsf1_data[3], "rsf1_data.data(3)");
    sc_trace(tf, BASE_impl.rsf1_data[4], "rsf1_data.data(4)");
    sc_trace(tf, BASE_impl.rsf1_data[5], "rsf1_data.data(5)");
    sc_trace(tf, BASE_impl.rsf1_data[6], "rsf1_data.data(6)");
    sc_trace(tf, BASE_impl.rsf1_data[7], "rsf1_data.data(7)");
    sc_trace(tf, BASE_impl.rsf2_data[0], "rsf2_data.data(0)");
    sc_trace(tf, BASE_impl.rsf2_data[1], "rsf2_data.data(1)");
    sc_trace(tf, BASE_impl.rsf2_data[2], "rsf2_data.data(2)");
    sc_trace(tf, BASE_impl.rsf2_data[3], "rsf2_data.data(3)");
    sc_trace(tf, BASE_impl.rsf2_data[4], "rsf2_data.data(4)");
    sc_trace(tf, BASE_impl.rsf2_data[5], "rsf2_data.data(5)");
    sc_trace(tf, BASE_impl.rsf2_data[6], "rsf2_data.data(6)");
    sc_trace(tf, BASE_impl.rsf2_data[7], "rsf2_data.data(7)");
    sc_trace(tf, BASE_impl.vfpufifo_empty, "vfpufifo_empty");
    sc_trace(tf, BASE_impl.vfputop_dat, "vfputop_dat");
    sc_trace(tf, BASE_impl.vfpufifo_elem_num, "vfpufifo_elem_num");
    sc_trace(tf, BASE_impl.lsu_ready, "lsu_ready");
    sc_trace(tf, BASE_impl.lsufifo_empty, "lsufifo_empty");
    sc_trace(tf, BASE_impl.lsutop_dat, "lsutop_dat");
    sc_trace(tf, BASE_impl.lsufifo_elem_num, "lsufifo_elem_num");
    sc_trace(tf, BASE_impl.write_s, "write_s");
    sc_trace(tf, BASE_impl.write_v, "write_v");
    sc_trace(tf, BASE_impl.write_f, "write_f");
    sc_trace(tf, BASE_impl.write_lsu, "write_lsu");
    sc_trace(tf, BASE_impl.wb_ena, "wb_ena");
    sc_trace(tf, BASE_impl.wb_ins, "wb_ins");
    // sc_trace(tf, BASE_impl., "");

    sc_start(500, SC_NS);
    sc_close_vcd_trace_file(tf);

    return 0;
}
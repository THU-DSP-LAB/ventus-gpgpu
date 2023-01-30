#include "fetch.h"
#include "fetch_sti.h"

int sc_main(int argc, char *argv[])
{
    fetch fetch_impl("fetch_impl");
    fetch_sti fetch_sti_impl("fetch_sti_impl");

    sc_clock clk("clk", PERIOD, SC_NS);
    sc_signal<bool> rst_n("rst_n"), ibuf_full{"ibuf_full"}, jump("jump");
    sc_signal<sc_int<ireg_bitsize + 1>> jump_addr{"jump_addr"};
    sc_signal<I_TYPE> fetch_ins("fetch_ins");
    event fetch_out("fetch_out");
    sc_signal<bool> fetch_out_eventsig;

    fetch_impl.clk(clk);
    fetch_impl.rst_n(rst_n);
    fetch_impl.ibuf_full(ibuf_full);
    fetch_impl.jump(jump);
    fetch_impl.jump_addr(jump_addr);
    fetch_impl.fetch_ins(fetch_ins);
    fetch_impl.fetch_out(fetch_out);

    fetch_sti_impl.clk(clk);
    fetch_sti_impl.fetch_ins(fetch_ins);
    fetch_sti_impl.fetch_out(fetch_out);
    fetch_sti_impl.rst_n(rst_n);
    fetch_sti_impl.ibuf_full(ibuf_full);
    fetch_sti_impl.jump(jump);
    fetch_sti_impl.jump_addr(jump_addr);
    fetch_sti_impl.fetch_out_eventsig(fetch_out_eventsig);

    sc_trace_file *tf = sc_create_vcd_trace_file("fetch_wave");
    tf->set_time_unit(1, SC_NS);
    sc_trace(tf, clk, "Clk");
    sc_trace(tf, rst_n, "Rst_n");
    sc_trace(tf, ibuf_full, "ibuf_full");
    sc_trace(tf, jump, "jump");
    sc_trace(tf, jump_addr, "jump_addr");
    sc_trace(tf, fetch_ins, "fetch_ins");
    sc_trace(tf, fetch_out_eventsig, "fetch_out_event");
    sc_trace(tf, fetch_impl.pc, "pc");
    sc_trace(tf, fetch_impl.ireg[0], "ireg(0)");
    sc_trace(tf, fetch_impl.ireg[1], "ireg(1)");

    sc_start(100, SC_NS);
    sc_close_vcd_trace_file(tf);

    return 0;
}

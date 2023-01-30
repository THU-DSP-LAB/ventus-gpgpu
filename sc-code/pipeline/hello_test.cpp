#include "systemc.h"
#include "hello_sti.h"

int sc_main(int argc, char *argv[])
{
    sc_signal<bool> boolnum;
    sc_signal<int> intnum;
    sc_clock clk("Clk", 10, SC_NS);
    sc_signal<I_TYPE> ins;

    hello_sti gen_impl("Gen_Sti");
    gen_impl.clk(clk);
    gen_impl.boolnum(boolnum);
    gen_impl.intnum(intnum);
    gen_impl.ins(ins);

    sc_trace_file *tf = sc_create_vcd_trace_file("hello_wave");
    tf->set_time_unit(1, SC_NS);
    sc_trace(tf, boolnum, "Boolnum");
    sc_trace(tf, intnum, "Intnum");
    sc_trace(tf, clk, "CLOCK");
    sc_trace(tf, ins, "INS");

    sc_start(200, SC_NS);
    sc_close_vcd_trace_file(tf);

    return 0;
}

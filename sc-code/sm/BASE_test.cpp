#include "BASE.h"
#include "BASE_sti.h"
#include <chrono>

int sc_main(int argc, char *argv[])
{
    // 处理命令行参数
    std::string inssrc;
    for (int i = 1; i < argc; i++)
    {
        if (strcmp(argv[i], "--inssrc") == 0)
        {
            inssrc = argv[i + 1];
            i++;
        }
    }

    BASE BASE_impl("BASE", inssrc);
    BASE_sti BASE_sti_impl("BASE_STI");

    sc_clock clk("clk", PERIOD, SC_NS, 0.5, 0, SC_NS, false);
    sc_signal<bool> rst_n("rst_n");

    BASE_impl.clk(clk);
    BASE_impl.rst_n(rst_n);

    BASE_sti_impl.rst_n(rst_n);

    sc_trace_file *tf[num_warp];
    for (int i = 0; i < num_warp; i++)
    {
        tf[i] = sc_create_vcd_trace_file(("BASE_wave_warp" + std::to_string(i)).c_str());
        tf[i]->set_time_unit(1, SC_NS);
        sc_trace(tf[i], clk, "Clk");
        sc_trace(tf[i], rst_n, "Rst_n");
        sc_trace(tf[i], BASE_impl.WARPS[i].jump, "jump");
        sc_trace(tf[i], BASE_impl.WARPS[i].jump_addr, "jump_addr");
        sc_trace(tf[i], BASE_impl.WARPS[i].branch_sig, "bramch_sig");
        sc_trace(tf[i], BASE_impl.WARPS[i].fetch_valid, "fetch_valid");
        sc_trace(tf[i], BASE_impl.WARPS[i].pc, "pc");
        sc_trace(tf[i], BASE_impl.WARPS[i].fetch_ins, "fetch_ins");
        sc_trace(tf[i], BASE_impl.WARPS[i].dispatch_warp_valid, "dispatch_warp_valid");
        sc_trace(tf[i], BASE_impl.WARPS[i].ibuf_empty, "ibuf_empty");
        sc_trace(tf[i], BASE_impl.WARPS[i].ibuf_swallow, "ibuf_swallow");
        sc_trace(tf[i], BASE_impl.WARPS[i].ibuftop_ins, "ibuftop_ins");
        sc_trace(tf[i], BASE_impl.WARPS[i].ififo_elem_num, "ififo_elem_num");
        sc_trace(tf[i], BASE_impl.WARPS[i].wait_bran, "wait_bran");
        sc_trace(tf[i], BASE_impl.WARPS[i].can_dispatch, "can_dispatch");
        sc_trace(tf[i], BASE_impl.opc_full, "opc_full");
        sc_trace(tf[i], BASE_impl.last_dispatch_warpid, "last_dispatch_warpid");
        sc_trace(tf[i], BASE_impl.issue_ins, "issue_ins");
        sc_trace(tf[i], BASE_impl.issueins_warpid, "issueins_warpid");
        sc_trace(tf[i], BASE_impl.dispatch_valid, "dispatch_valid");
        sc_trace(tf[i], BASE_impl.dispatch_ready, "dispatch_ready");
        sc_trace(tf[i], BASE_impl.opcfifo_elem_num, "opcfifo_elem_num");
        sc_trace(tf[i], BASE_impl.emit_ins, "emit_ins");
        sc_trace(tf[i], BASE_impl.emitins_warpid, "emitins_warpid");
        sc_trace(tf[i], BASE_impl.doemit, "doemit");
        sc_trace(tf[i], BASE_impl.findemit, "findemit");
        sc_trace(tf[i], BASE_impl.emit_idx, "emit_idx");
        sc_trace(tf[i], BASE_impl.emito_salu, "emito_salu");
        sc_trace(tf[i], BASE_impl.emito_valu, "emito_valu");
        sc_trace(tf[i], BASE_impl.emito_vfpu, "emito_vfpu");
        sc_trace(tf[i], BASE_impl.emito_lsu, "emito_lsu");
        sc_trace(tf[i], BASE_impl.salu_ready, "salu_ready");
        sc_trace(tf[i], BASE_impl.salufifo_empty, "salufifo_empty");
        sc_trace(tf[i], BASE_impl.salutmp2, "salutmp2");
        sc_trace(tf[i], BASE_impl.salutop_dat, "salutop_dat");
        sc_trace(tf[i], BASE_impl.salufifo_elem_num, "salufifo_elem_num");
        // valu
        sc_trace(tf[i], BASE_impl.valu_ready, "valu_ready");
        sc_trace(tf[i], BASE_impl.valuto_simtstk, "valuto_simtstk");
        sc_trace(tf[i], BASE_impl.branch_elsemask, "branch_elsemask");
        sc_trace(tf[i], BASE_impl.branch_elsepc, "branch_elsepc");
        sc_trace(tf[i], BASE_impl.vbranch_ins, "vbranch_ins");
        sc_trace(tf[i], BASE_impl.vbranchins_warpid, "vbranchins_warpid");

        sc_trace(tf[i], BASE_impl.valufifo_empty, "valufifo_empty");
        sc_trace(tf[i], BASE_impl.valutop_dat, "valutop_dat");
        sc_trace(tf[i], BASE_impl.valufifo_elem_num, "valufifo_elem_num");

        // simt-stack
        sc_trace(tf[i], BASE_impl.emito_simtstk, "emito_simtstk");
        sc_trace(tf[i], BASE_impl.WARPS[i].simtstk_jump, "simtstk_jump");
        sc_trace(tf[i], BASE_impl.WARPS[i].simtstk_jumpaddr, "simtstk_jumpaddr");
        sc_trace(tf[i], BASE_impl.WARPS[i].current_mask, "current_mask");
        sc_trace(tf[i], BASE_impl.WARPS[i].vbran_sig, "vbran_sig");

        sc_trace(tf[i], BASE_impl.vfpu_ready, "vfpu_ready");
        sc_trace(tf[i], BASE_impl.vfpufifo_empty, "vfpufifo_empty");
        sc_trace(tf[i], BASE_impl.vfputop_dat, "vfputop_dat");
        sc_trace(tf[i], BASE_impl.vfpufifo_elem_num, "vfpufifo_elem_num");
        sc_trace(tf[i], BASE_impl.lsu_ready, "lsu_ready");
        sc_trace(tf[i], BASE_impl.lsufifo_empty, "lsufifo_empty");
        sc_trace(tf[i], BASE_impl.lsutop_dat, "lsutop_dat");
        sc_trace(tf[i], BASE_impl.lsufifo_elem_num, "lsufifo_elem_num");
        sc_trace(tf[i], BASE_impl.write_s, "write_s");
        sc_trace(tf[i], BASE_impl.write_v, "write_v");
        sc_trace(tf[i], BASE_impl.write_f, "write_f");
        sc_trace(tf[i], BASE_impl.execpop_salu, "execpop_salu");
        sc_trace(tf[i], BASE_impl.execpop_valu, "execpop_valu");
        sc_trace(tf[i], BASE_impl.execpop_vfpu, "execpop_vfpu");
        sc_trace(tf[i], BASE_impl.execpop_lsu, "execpop_lsu");
        sc_trace(tf[i], BASE_impl.wb_ena, "wb_ena");
        sc_trace(tf[i], BASE_impl.wb_ins, "wb_ins");
        sc_trace(tf[i], BASE_impl.wb_warpid, "wb_warpid");
        for (int j = 0; j < 32; j++)
            sc_trace(tf[i], BASE_impl.WARPS[i].s_regfile[j], "s_regfile.data(" + std::to_string(j) + ")");
        sc_trace(tf[i], BASE_impl.WARPS[i].v_regfile[0][0], "v_regfile(0)(0)");
        sc_trace(tf[i], BASE_impl.WARPS[i].v_regfile[1][0], "v_regfile(1)(0)");
        sc_trace(tf[i], BASE_impl.WARPS[i].v_regfile[2][0], "v_regfile(2)(0)");
        sc_trace(tf[i], BASE_impl.WARPS[i].v_regfile[3][0], "v_regfile(3)(0)");
        sc_trace(tf[i], BASE_impl.WARPS[i].v_regfile[4][0], "v_regfile(4)(0)");
        sc_trace(tf[i], BASE_impl.WARPS[i].v_regfile[5][0], "v_regfile(5)(0)");
        sc_trace(tf[i], BASE_impl.WARPS[i].v_regfile[6][0], "v_regfile(6)(0)");
        sc_trace(tf[i], BASE_impl.WARPS[i].v_regfile[7][0], "v_regfile(7)(0)");
        sc_trace(tf[i], BASE_impl.external_mem[0], "external_mem.data(0)");
        // sc_trace(tf[i], BASE_impl., "");
    }

    auto start = std::chrono::high_resolution_clock::now();
    sc_start(1000, SC_NS);

    for (auto tf_ : tf)
        sc_close_vcd_trace_file(tf_);

    auto end = std::chrono::high_resolution_clock::now();
    auto duration = std::chrono::duration_cast<std::chrono::microseconds>(end - start);
    std::cout << "Time taken: " << duration.count() / 1000 << " milliseconds" << std::endl;

    return 0;
}
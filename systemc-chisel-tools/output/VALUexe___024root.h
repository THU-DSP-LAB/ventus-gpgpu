// Verilated -*- SystemC -*-
// DESCRIPTION: Verilator output: Design internal header
// See VALUexe.h for the primary calling header

#ifndef VERILATED_VALUEXE___024ROOT_H_
#define VERILATED_VALUEXE___024ROOT_H_  // guard

#include "systemc.h"
#include "verilated_sc.h"
#include "verilated.h"

class VALUexe__Syms;

class VALUexe___024root final : public VerilatedModule {
  public:

    // DESIGN SPECIFIC STATE
    CData/*0:0*/ __Vcellinp__ALUexe__clock;
    CData/*0:0*/ __Vcellinp__ALUexe__io_out2br_ready;
    CData/*0:0*/ __Vcellinp__ALUexe__io_out_ready;
    CData/*0:0*/ __Vcellinp__ALUexe__io_in_bits_ctrl_wxd;
    CData/*4:0*/ __Vcellinp__ALUexe__io_in_bits_ctrl_reg_idxw;
    CData/*5:0*/ __Vcellinp__ALUexe__io_in_bits_ctrl_alu_fn;
    CData/*1:0*/ __Vcellinp__ALUexe__io_in_bits_ctrl_branch;
    CData/*1:0*/ __Vcellinp__ALUexe__io_in_bits_ctrl_wid;
    CData/*0:0*/ __Vcellinp__ALUexe__io_in_valid;
    CData/*0:0*/ __Vcellinp__ALUexe__reset;
    CData/*0:0*/ ALUexe__DOT__alu__DOT__slt;
    CData/*0:0*/ ALUexe__DOT__result__DOT__ram_wb_wxd_rd_MPORT_en;
    CData/*0:0*/ ALUexe__DOT__result__DOT__maybe_full;
    CData/*0:0*/ ALUexe__DOT__result__DOT__do_deq;
    CData/*0:0*/ ALUexe__DOT__result_br__DOT__ram_wid_MPORT_en;
    CData/*0:0*/ ALUexe__DOT__result_br__DOT__maybe_full;
    CData/*0:0*/ ALUexe__DOT__result_br__DOT__do_deq;
    CData/*0:0*/ __Vtrigrprev__TOP____Vcellinp__ALUexe__clock;
    CData/*0:0*/ __VactContinue;
    IData/*31:0*/ __Vcellinp__ALUexe__io_in_bits_in3;
    IData/*31:0*/ __Vcellinp__ALUexe__io_in_bits_in1;
    IData/*31:0*/ ALUexe__DOT__alu__DOT__in2_inv;
    IData/*31:0*/ ALUexe__DOT__result__DOT__ram_wb_wxd_rd_MPORT_data;
    IData/*31:0*/ __VstlIterCount;
    IData/*31:0*/ __VicoIterCount;
    IData/*31:0*/ __VactIterCount;
    VlUnpacked<IData/*31:0*/, 1> ALUexe__DOT__result__DOT__ram_wb_wxd_rd;
    VlUnpacked<CData/*0:0*/, 1> ALUexe__DOT__result__DOT__ram_wxd;
    VlUnpacked<CData/*4:0*/, 1> ALUexe__DOT__result__DOT__ram_reg_idxw;
    VlUnpacked<CData/*1:0*/, 1> ALUexe__DOT__result__DOT__ram_warp_id;
    VlUnpacked<CData/*1:0*/, 1> ALUexe__DOT__result_br__DOT__ram_wid;
    VlUnpacked<CData/*0:0*/, 1> ALUexe__DOT__result_br__DOT__ram_jump;
    VlUnpacked<IData/*31:0*/, 1> ALUexe__DOT__result_br__DOT__ram_new_pc;
    sc_in<bool> clock;
    sc_in<bool> reset;
    sc_out<bool> io_in_ready;
    sc_in<bool> io_in_valid;
    sc_in<uint32_t> io_in_bits_ctrl_wid;
    sc_in<bool> io_in_bits_ctrl_fp;
    sc_in<uint32_t> io_in_bits_ctrl_branch;
    sc_in<bool> io_in_bits_ctrl_simt_stack;
    sc_in<bool> io_in_bits_ctrl_simt_stack_op;
    sc_in<bool> io_in_bits_ctrl_barrier;
    sc_in<uint32_t> io_in_bits_ctrl_csr;
    sc_in<bool> io_in_bits_ctrl_reverse;
    sc_in<uint32_t> io_in_bits_ctrl_sel_alu2;
    sc_in<uint32_t> io_in_bits_ctrl_sel_alu1;
    sc_in<bool> io_in_bits_ctrl_isvec;
    sc_in<uint32_t> io_in_bits_ctrl_sel_alu3;
    sc_in<bool> io_in_bits_ctrl_mask;
    sc_in<uint32_t> io_in_bits_ctrl_sel_imm;
    sc_in<uint32_t> io_in_bits_ctrl_mem_whb;
    sc_in<bool> io_in_bits_ctrl_mem_unsigned;
    sc_in<uint32_t> io_in_bits_ctrl_alu_fn;
    sc_in<bool> io_in_bits_ctrl_mem;
    sc_in<bool> io_in_bits_ctrl_mul;
    sc_in<uint32_t> io_in_bits_ctrl_mem_cmd;
    sc_in<uint32_t> io_in_bits_ctrl_mop;
    sc_in<uint32_t> io_in_bits_ctrl_reg_idx1;
    sc_in<uint32_t> io_in_bits_ctrl_reg_idx2;
    sc_in<uint32_t> io_in_bits_ctrl_reg_idx3;
    sc_in<uint32_t> io_in_bits_ctrl_reg_idxw;
    sc_in<bool> io_in_bits_ctrl_wfd;
    sc_in<bool> io_in_bits_ctrl_fence;
    sc_in<bool> io_in_bits_ctrl_sfu;
    sc_in<bool> io_in_bits_ctrl_readmask;
    sc_in<bool> io_in_bits_ctrl_writemask;
    sc_in<bool> io_in_bits_ctrl_wxd;
    sc_in<bool> io_out_ready;
    sc_out<bool> io_out_valid;
    sc_out<bool> io_out_bits_wxd;
    sc_out<uint32_t> io_out_bits_reg_idxw;
    sc_out<uint32_t> io_out_bits_warp_id;
    sc_in<bool> io_out2br_ready;
    sc_out<bool> io_out2br_valid;
    sc_out<uint32_t> io_out2br_bits_wid;
    sc_out<bool> io_out2br_bits_jump;
    sc_in<uint32_t> io_in_bits_in1;
    sc_in<uint32_t> io_in_bits_in2;
    sc_in<uint32_t> io_in_bits_in3;
    sc_in<uint32_t> io_in_bits_ctrl_inst;
    sc_in<uint32_t> io_in_bits_ctrl_pc;
    sc_out<uint32_t> io_out_bits_wb_wxd_rd;
    sc_out<uint32_t> io_out2br_bits_new_pc;
    VlTriggerVec<1> __VstlTriggered;
    VlTriggerVec<1> __VicoTriggered;
    VlTriggerVec<1> __VactTriggered;
    VlTriggerVec<1> __VnbaTriggered;

    // INTERNAL VARIABLES
    VALUexe__Syms* const vlSymsp;

    // CONSTRUCTORS
    VALUexe___024root(VALUexe__Syms* symsp, const char* name);
    ~VALUexe___024root();
    VL_UNCOPYABLE(VALUexe___024root);

    // INTERNAL METHODS
    void __Vconfigure(bool first);
} VL_ATTR_ALIGNED(VL_CACHE_LINE_BYTES);


#endif  // guard

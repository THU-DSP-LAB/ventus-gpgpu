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
    // Anonymous structures to workaround compiler member-count bugs
    struct {
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
        CData/*0:0*/ ALUexe__DOT__result_io_enq_ready;
        CData/*0:0*/ ALUexe__DOT__result_io_enq_valid;
        CData/*0:0*/ ALUexe__DOT__result_io_deq_valid;
        CData/*0:0*/ ALUexe__DOT__result_io_deq_bits_wxd;
        CData/*4:0*/ ALUexe__DOT__result_io_deq_bits_reg_idxw;
        CData/*1:0*/ ALUexe__DOT__result_io_deq_bits_warp_id;
        CData/*0:0*/ ALUexe__DOT__result_br_io_enq_ready;
        CData/*0:0*/ ALUexe__DOT__result_br_io_enq_valid;
        CData/*0:0*/ ALUexe__DOT__result_br_io_enq_bits_jump;
        CData/*0:0*/ ALUexe__DOT__result_br_io_deq_valid;
        CData/*1:0*/ ALUexe__DOT__result_br_io_deq_bits_wid;
        CData/*0:0*/ ALUexe__DOT__result_br_io_deq_bits_jump;
        CData/*0:0*/ ALUexe__DOT__alu__DOT__io_cmp_out_combo___05FV;
        CData/*0:0*/ ALUexe__DOT__result__DOT__io_enq_ready_combo___05FV;
        CData/*0:0*/ ALUexe__DOT__result__DOT__io_deq_valid_combo___05FV;
        CData/*0:0*/ ALUexe__DOT__result__DOT__io_deq_bits_wxd_combo___05FV;
        CData/*4:0*/ ALUexe__DOT__result__DOT__io_deq_bits_reg_idxw_combo___05FV;
        CData/*1:0*/ ALUexe__DOT__result__DOT__io_deq_bits_warp_id_combo___05FV;
        CData/*0:0*/ ALUexe__DOT__result__DOT__io_enq_ready_seq___05FV;
        CData/*0:0*/ ALUexe__DOT__result__DOT__io_deq_valid_seq___05FV;
        CData/*0:0*/ ALUexe__DOT__result__DOT__io_deq_bits_wxd_seq___05FV;
        CData/*4:0*/ ALUexe__DOT__result__DOT__io_deq_bits_reg_idxw_seq___05FV;
        CData/*1:0*/ ALUexe__DOT__result__DOT__io_deq_bits_warp_id_seq___05FV;
        CData/*0:0*/ ALUexe__DOT__result_br__DOT__io_enq_ready_combo___05FV;
        CData/*0:0*/ ALUexe__DOT__result_br__DOT__io_deq_valid_combo___05FV;
        CData/*1:0*/ ALUexe__DOT__result_br__DOT__io_deq_bits_wid_combo___05FV;
        CData/*0:0*/ ALUexe__DOT__result_br__DOT__io_deq_bits_jump_combo___05FV;
        CData/*0:0*/ ALUexe__DOT__result_br__DOT__io_enq_ready_seq___05FV;
        CData/*0:0*/ ALUexe__DOT__result_br__DOT__io_deq_valid_seq___05FV;
        CData/*1:0*/ ALUexe__DOT__result_br__DOT__io_deq_bits_wid_seq___05FV;
        CData/*0:0*/ ALUexe__DOT__result_br__DOT__io_deq_bits_jump_seq___05FV;
        CData/*0:0*/ __Vfunc_ALUexe__DOT__alu__DOT__ScalarALU_protectlib_combo_update__2__io_cmp_out;
        CData/*0:0*/ __Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_combo_update__6__io_enq_ready;
        CData/*0:0*/ __Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_combo_update__6__io_deq_valid;
        CData/*0:0*/ __Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_combo_update__6__io_deq_bits_wxd;
        CData/*4:0*/ __Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_combo_update__6__io_deq_bits_reg_idxw;
        CData/*1:0*/ __Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_combo_update__6__io_deq_bits_warp_id;
        CData/*0:0*/ __Vfunc_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_combo_update__12__io_enq_ready;
        CData/*0:0*/ __Vfunc_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_combo_update__12__io_deq_valid;
        CData/*1:0*/ __Vfunc_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_combo_update__12__io_deq_bits_wid;
        CData/*0:0*/ __Vfunc_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_combo_update__12__io_deq_bits_jump;
        CData/*0:0*/ __Vtrigrprev__TOP____Vcellinp__ALUexe__clock;
        CData/*0:0*/ __VactContinue;
        IData/*31:0*/ __Vcellinp__ALUexe__io_in_bits_in3;
        IData/*31:0*/ __Vcellinp__ALUexe__io_in_bits_in2;
        IData/*31:0*/ __Vcellinp__ALUexe__io_in_bits_in1;
        IData/*31:0*/ ALUexe__DOT__result_io_deq_bits_wb_wxd_rd;
        IData/*31:0*/ ALUexe__DOT__result_br_io_deq_bits_new_pc;
        IData/*31:0*/ ALUexe__DOT__alu__DOT__io_out_combo___05FV;
        IData/*31:0*/ ALUexe__DOT__result__DOT__io_deq_bits_wb_wxd_rd_combo___05FV;
        IData/*31:0*/ ALUexe__DOT__result__DOT__io_deq_bits_wb_wxd_rd_seq___05FV;
        IData/*31:0*/ ALUexe__DOT__result_br__DOT__io_deq_bits_new_pc_combo___05FV;
        IData/*31:0*/ ALUexe__DOT__result_br__DOT__io_deq_bits_new_pc_seq___05FV;
        IData/*31:0*/ __Vfunc_ALUexe__DOT__alu__DOT__ScalarALU_protectlib_combo_update__2__io_out;
    };
    struct {
        IData/*31:0*/ __Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_combo_update__6__io_deq_bits_wb_wxd_rd;
        IData/*31:0*/ __Vfunc_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_combo_update__12__io_deq_bits_new_pc;
        IData/*31:0*/ __VstlIterCount;
        IData/*31:0*/ __VicoIterCount;
        IData/*31:0*/ __VactIterCount;
        QData/*63:0*/ ALUexe__DOT__alu__DOT__handle___05FV;
        QData/*63:0*/ ALUexe__DOT__result__DOT__handle___05FV;
        QData/*63:0*/ ALUexe__DOT__result__DOT__last_combo_seqnum___05FV;
        QData/*63:0*/ ALUexe__DOT__result__DOT__last_seq_seqnum___05FV;
        QData/*63:0*/ ALUexe__DOT__result_br__DOT__handle___05FV;
        QData/*63:0*/ ALUexe__DOT__result_br__DOT__last_combo_seqnum___05FV;
        QData/*63:0*/ ALUexe__DOT__result_br__DOT__last_seq_seqnum___05FV;
        QData/*63:0*/ __Vfunc_ALUexe__DOT__alu__DOT__ScalarALU_protectlib_combo_update__2__Vfuncout;
        QData/*63:0*/ __Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_combo_update__6__Vfuncout;
        QData/*63:0*/ __Vfunc_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_combo_update__12__Vfuncout;
    };
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

// Verilated -*- SystemC -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See VALUexe.h for the primary calling header

#include "verilated.h"
#include "verilated_dpi.h"

#include "VALUexe___024root.h"

VL_ATTR_COLD void VALUexe___024root___eval_static(VALUexe___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VALUexe__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root___eval_static\n"); );
}

VL_ATTR_COLD void VALUexe___024root___eval_initial__TOP(VALUexe___024root* vlSelf);

VL_ATTR_COLD void VALUexe___024root___eval_initial(VALUexe___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VALUexe__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root___eval_initial\n"); );
    // Body
    VALUexe___024root___eval_initial__TOP(vlSelf);
    vlSelf->__Vtrigrprev__TOP____Vcellinp__ALUexe__clock 
        = vlSelf->__Vcellinp__ALUexe__clock;
}

VL_ATTR_COLD void VALUexe___024root___eval_final__TOP(VALUexe___024root* vlSelf);

VL_ATTR_COLD void VALUexe___024root___eval_final(VALUexe___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VALUexe__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root___eval_final\n"); );
    // Body
    VALUexe___024root___eval_final__TOP(vlSelf);
}

void VALUexe___024root____Vdpiimwrap_ALUexe__DOT__alu__DOT__ScalarALU_protectlib_final_TOP(QData/*63:0*/ handle___05FV);
void VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result__DOT__Queue_protectlib_final_TOP(QData/*63:0*/ handle___05FV);
void VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_final_TOP(QData/*63:0*/ handle___05FV);

VL_ATTR_COLD void VALUexe___024root___eval_final__TOP(VALUexe___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VALUexe__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root___eval_final__TOP\n"); );
    // Body
    VALUexe___024root____Vdpiimwrap_ALUexe__DOT__alu__DOT__ScalarALU_protectlib_final_TOP(vlSelf->ALUexe__DOT__alu__DOT__handle___05FV);
    VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result__DOT__Queue_protectlib_final_TOP(vlSelf->ALUexe__DOT__result__DOT__handle___05FV);
    VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_final_TOP(vlSelf->ALUexe__DOT__result_br__DOT__handle___05FV);
}

VL_ATTR_COLD void VALUexe___024root___eval_triggers__stl(VALUexe___024root* vlSelf);
#ifdef VL_DEBUG
VL_ATTR_COLD void VALUexe___024root___dump_triggers__stl(VALUexe___024root* vlSelf);
#endif  // VL_DEBUG
VL_ATTR_COLD void VALUexe___024root___eval_stl(VALUexe___024root* vlSelf);

VL_ATTR_COLD void VALUexe___024root___eval_settle(VALUexe___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VALUexe__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root___eval_settle\n"); );
    // Init
    CData/*0:0*/ __VstlContinue;
    // Body
    vlSelf->__VstlIterCount = 0U;
    __VstlContinue = 1U;
    while (__VstlContinue) {
        __VstlContinue = 0U;
        VALUexe___024root___eval_triggers__stl(vlSelf);
        if (vlSelf->__VstlTriggered.any()) {
            __VstlContinue = 1U;
            if ((0x64U < vlSelf->__VstlIterCount)) {
#ifdef VL_DEBUG
                VALUexe___024root___dump_triggers__stl(vlSelf);
#endif
                VL_FATAL_MT("generated/ALUexe.v", 432, "", "Settle region did not converge.");
            }
            vlSelf->__VstlIterCount = ((IData)(1U) 
                                       + vlSelf->__VstlIterCount);
            VALUexe___024root___eval_stl(vlSelf);
        }
    }
}

#ifdef VL_DEBUG
VL_ATTR_COLD void VALUexe___024root___dump_triggers__stl(VALUexe___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VALUexe__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root___dump_triggers__stl\n"); );
    // Body
    if ((1U & (~ (IData)(vlSelf->__VstlTriggered.any())))) {
        VL_DBG_MSGF("         No triggers active\n");
    }
    if (vlSelf->__VstlTriggered.at(0U)) {
        VL_DBG_MSGF("         'stl' region trigger index 0 is active: Internal 'stl' trigger - first iteration\n");
    }
}
#endif  // VL_DEBUG

void VALUexe___024root___ico_sequent__TOP__0(VALUexe___024root* vlSelf);

VL_ATTR_COLD void VALUexe___024root___eval_stl(VALUexe___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VALUexe__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root___eval_stl\n"); );
    // Body
    if (vlSelf->__VstlTriggered.at(0U)) {
        VALUexe___024root___ico_sequent__TOP__0(vlSelf);
    }
}

#ifdef VL_DEBUG
VL_ATTR_COLD void VALUexe___024root___dump_triggers__ico(VALUexe___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VALUexe__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root___dump_triggers__ico\n"); );
    // Body
    if ((1U & (~ (IData)(vlSelf->__VicoTriggered.any())))) {
        VL_DBG_MSGF("         No triggers active\n");
    }
    if (vlSelf->__VicoTriggered.at(0U)) {
        VL_DBG_MSGF("         'ico' region trigger index 0 is active: Internal 'ico' trigger - first iteration\n");
    }
}
#endif  // VL_DEBUG

#ifdef VL_DEBUG
VL_ATTR_COLD void VALUexe___024root___dump_triggers__act(VALUexe___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VALUexe__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root___dump_triggers__act\n"); );
    // Body
    if ((1U & (~ (IData)(vlSelf->__VactTriggered.any())))) {
        VL_DBG_MSGF("         No triggers active\n");
    }
    if (vlSelf->__VactTriggered.at(0U)) {
        VL_DBG_MSGF("         'act' region trigger index 0 is active: @(edge __Vcellinp__ALUexe__clock)\n");
    }
}
#endif  // VL_DEBUG

#ifdef VL_DEBUG
VL_ATTR_COLD void VALUexe___024root___dump_triggers__nba(VALUexe___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VALUexe__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root___dump_triggers__nba\n"); );
    // Body
    if ((1U & (~ (IData)(vlSelf->__VnbaTriggered.any())))) {
        VL_DBG_MSGF("         No triggers active\n");
    }
    if (vlSelf->__VnbaTriggered.at(0U)) {
        VL_DBG_MSGF("         'nba' region trigger index 0 is active: @(edge __Vcellinp__ALUexe__clock)\n");
    }
}
#endif  // VL_DEBUG

VL_ATTR_COLD void VALUexe___024root___ctor_var_reset(VALUexe___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VALUexe__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root___ctor_var_reset\n"); );
    // Body
    vlSelf->__Vcellinp__ALUexe__io_out2br_ready = VL_RAND_RESET_I(1);
    vlSelf->__Vcellinp__ALUexe__io_out_ready = VL_RAND_RESET_I(1);
    vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_wxd = VL_RAND_RESET_I(1);
    vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_reg_idxw = VL_RAND_RESET_I(5);
    vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_alu_fn = VL_RAND_RESET_I(6);
    vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_branch = VL_RAND_RESET_I(2);
    vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_wid = VL_RAND_RESET_I(2);
    vlSelf->__Vcellinp__ALUexe__io_in_bits_in3 = VL_RAND_RESET_I(32);
    vlSelf->__Vcellinp__ALUexe__io_in_bits_in2 = VL_RAND_RESET_I(32);
    vlSelf->__Vcellinp__ALUexe__io_in_bits_in1 = VL_RAND_RESET_I(32);
    vlSelf->__Vcellinp__ALUexe__io_in_valid = VL_RAND_RESET_I(1);
    vlSelf->__Vcellinp__ALUexe__reset = VL_RAND_RESET_I(1);
    vlSelf->__Vcellinp__ALUexe__clock = VL_RAND_RESET_I(1);
    vlSelf->ALUexe__DOT__result_io_enq_ready = VL_RAND_RESET_I(1);
    vlSelf->ALUexe__DOT__result_io_enq_valid = VL_RAND_RESET_I(1);
    vlSelf->ALUexe__DOT__result_io_deq_valid = VL_RAND_RESET_I(1);
    vlSelf->ALUexe__DOT__result_io_deq_bits_wb_wxd_rd = VL_RAND_RESET_I(32);
    vlSelf->ALUexe__DOT__result_io_deq_bits_wxd = VL_RAND_RESET_I(1);
    vlSelf->ALUexe__DOT__result_io_deq_bits_reg_idxw = VL_RAND_RESET_I(5);
    vlSelf->ALUexe__DOT__result_io_deq_bits_warp_id = VL_RAND_RESET_I(2);
    vlSelf->ALUexe__DOT__result_br_io_enq_ready = VL_RAND_RESET_I(1);
    vlSelf->ALUexe__DOT__result_br_io_enq_valid = VL_RAND_RESET_I(1);
    vlSelf->ALUexe__DOT__result_br_io_enq_bits_jump = VL_RAND_RESET_I(1);
    vlSelf->ALUexe__DOT__result_br_io_deq_valid = VL_RAND_RESET_I(1);
    vlSelf->ALUexe__DOT__result_br_io_deq_bits_wid = VL_RAND_RESET_I(2);
    vlSelf->ALUexe__DOT__result_br_io_deq_bits_jump = VL_RAND_RESET_I(1);
    vlSelf->ALUexe__DOT__result_br_io_deq_bits_new_pc = VL_RAND_RESET_I(32);
    vlSelf->ALUexe__DOT__alu__DOT__handle___05FV = 0;
    vlSelf->ALUexe__DOT__alu__DOT__io_out_combo___05FV = VL_RAND_RESET_I(32);
    vlSelf->ALUexe__DOT__alu__DOT__io_cmp_out_combo___05FV = VL_RAND_RESET_I(1);
    vlSelf->ALUexe__DOT__result__DOT__handle___05FV = 0;
    vlSelf->ALUexe__DOT__result__DOT__last_combo_seqnum___05FV = VL_RAND_RESET_Q(64);
    vlSelf->ALUexe__DOT__result__DOT__last_seq_seqnum___05FV = VL_RAND_RESET_Q(64);
    vlSelf->ALUexe__DOT__result__DOT__io_enq_ready_combo___05FV = VL_RAND_RESET_I(1);
    vlSelf->ALUexe__DOT__result__DOT__io_deq_valid_combo___05FV = VL_RAND_RESET_I(1);
    vlSelf->ALUexe__DOT__result__DOT__io_deq_bits_wb_wxd_rd_combo___05FV = VL_RAND_RESET_I(32);
    vlSelf->ALUexe__DOT__result__DOT__io_deq_bits_wxd_combo___05FV = VL_RAND_RESET_I(1);
    vlSelf->ALUexe__DOT__result__DOT__io_deq_bits_reg_idxw_combo___05FV = VL_RAND_RESET_I(5);
    vlSelf->ALUexe__DOT__result__DOT__io_deq_bits_warp_id_combo___05FV = VL_RAND_RESET_I(2);
    vlSelf->ALUexe__DOT__result__DOT__io_enq_ready_seq___05FV = VL_RAND_RESET_I(1);
    vlSelf->ALUexe__DOT__result__DOT__io_deq_valid_seq___05FV = VL_RAND_RESET_I(1);
    vlSelf->ALUexe__DOT__result__DOT__io_deq_bits_wb_wxd_rd_seq___05FV = VL_RAND_RESET_I(32);
    vlSelf->ALUexe__DOT__result__DOT__io_deq_bits_wxd_seq___05FV = VL_RAND_RESET_I(1);
    vlSelf->ALUexe__DOT__result__DOT__io_deq_bits_reg_idxw_seq___05FV = VL_RAND_RESET_I(5);
    vlSelf->ALUexe__DOT__result__DOT__io_deq_bits_warp_id_seq___05FV = VL_RAND_RESET_I(2);
    vlSelf->ALUexe__DOT__result_br__DOT__handle___05FV = 0;
    vlSelf->ALUexe__DOT__result_br__DOT__last_combo_seqnum___05FV = VL_RAND_RESET_Q(64);
    vlSelf->ALUexe__DOT__result_br__DOT__last_seq_seqnum___05FV = VL_RAND_RESET_Q(64);
    vlSelf->ALUexe__DOT__result_br__DOT__io_enq_ready_combo___05FV = VL_RAND_RESET_I(1);
    vlSelf->ALUexe__DOT__result_br__DOT__io_deq_valid_combo___05FV = VL_RAND_RESET_I(1);
    vlSelf->ALUexe__DOT__result_br__DOT__io_deq_bits_wid_combo___05FV = VL_RAND_RESET_I(2);
    vlSelf->ALUexe__DOT__result_br__DOT__io_deq_bits_jump_combo___05FV = VL_RAND_RESET_I(1);
    vlSelf->ALUexe__DOT__result_br__DOT__io_deq_bits_new_pc_combo___05FV = VL_RAND_RESET_I(32);
    vlSelf->ALUexe__DOT__result_br__DOT__io_enq_ready_seq___05FV = VL_RAND_RESET_I(1);
    vlSelf->ALUexe__DOT__result_br__DOT__io_deq_valid_seq___05FV = VL_RAND_RESET_I(1);
    vlSelf->ALUexe__DOT__result_br__DOT__io_deq_bits_wid_seq___05FV = VL_RAND_RESET_I(2);
    vlSelf->ALUexe__DOT__result_br__DOT__io_deq_bits_jump_seq___05FV = VL_RAND_RESET_I(1);
    vlSelf->ALUexe__DOT__result_br__DOT__io_deq_bits_new_pc_seq___05FV = VL_RAND_RESET_I(32);
    vlSelf->__Vfunc_ALUexe__DOT__alu__DOT__ScalarALU_protectlib_combo_update__2__Vfuncout = 0;
    vlSelf->__Vfunc_ALUexe__DOT__alu__DOT__ScalarALU_protectlib_combo_update__2__io_out = VL_RAND_RESET_I(32);
    vlSelf->__Vfunc_ALUexe__DOT__alu__DOT__ScalarALU_protectlib_combo_update__2__io_cmp_out = VL_RAND_RESET_I(1);
    vlSelf->__Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_combo_update__6__Vfuncout = 0;
    vlSelf->__Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_combo_update__6__io_enq_ready = VL_RAND_RESET_I(1);
    vlSelf->__Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_combo_update__6__io_deq_valid = VL_RAND_RESET_I(1);
    vlSelf->__Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_combo_update__6__io_deq_bits_wb_wxd_rd = VL_RAND_RESET_I(32);
    vlSelf->__Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_combo_update__6__io_deq_bits_wxd = VL_RAND_RESET_I(1);
    vlSelf->__Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_combo_update__6__io_deq_bits_reg_idxw = VL_RAND_RESET_I(5);
    vlSelf->__Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_combo_update__6__io_deq_bits_warp_id = VL_RAND_RESET_I(2);
    vlSelf->__Vfunc_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_combo_update__12__Vfuncout = 0;
    vlSelf->__Vfunc_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_combo_update__12__io_enq_ready = VL_RAND_RESET_I(1);
    vlSelf->__Vfunc_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_combo_update__12__io_deq_valid = VL_RAND_RESET_I(1);
    vlSelf->__Vfunc_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_combo_update__12__io_deq_bits_wid = VL_RAND_RESET_I(2);
    vlSelf->__Vfunc_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_combo_update__12__io_deq_bits_jump = VL_RAND_RESET_I(1);
    vlSelf->__Vfunc_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_combo_update__12__io_deq_bits_new_pc = VL_RAND_RESET_I(32);
    vlSelf->__VstlIterCount = 0;
    vlSelf->__VicoIterCount = 0;
    vlSelf->__Vtrigrprev__TOP____Vcellinp__ALUexe__clock = VL_RAND_RESET_I(1);
    vlSelf->__VactIterCount = 0;
    vlSelf->__VactContinue = 0;
}

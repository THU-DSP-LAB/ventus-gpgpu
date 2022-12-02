// Verilated -*- SystemC -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See VALUexe.h for the primary calling header

#include "verilated.h"
#include "verilated_dpi.h"

#include "VALUexe___024root.h"

void VALUexe___024root____Vdpiimwrap_ALUexe__DOT__alu__DOT__ScalarALU_protectlib_combo_update_TOP(QData/*63:0*/ handle___05FV, CData/*4:0*/ io_func, IData/*31:0*/ io_in2, IData/*31:0*/ io_in1, IData/*31:0*/ &io_out, CData/*0:0*/ &io_cmp_out, QData/*63:0*/ &ScalarALU_protectlib_combo_update__Vfuncrtn);
void VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result__DOT__Queue_protectlib_combo_update_TOP(QData/*63:0*/ handle___05FV, CData/*0:0*/ reset, CData/*0:0*/ &io_enq_ready, CData/*0:0*/ io_enq_valid, IData/*31:0*/ io_enq_bits_wb_wxd_rd, CData/*0:0*/ io_enq_bits_wxd, CData/*4:0*/ io_enq_bits_reg_idxw, CData/*1:0*/ io_enq_bits_warp_id, CData/*0:0*/ io_deq_ready, CData/*0:0*/ &io_deq_valid, IData/*31:0*/ &io_deq_bits_wb_wxd_rd, CData/*0:0*/ &io_deq_bits_wxd, CData/*4:0*/ &io_deq_bits_reg_idxw, CData/*1:0*/ &io_deq_bits_warp_id, QData/*63:0*/ &Queue_protectlib_combo_update__Vfuncrtn);
void VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_combo_update_TOP(QData/*63:0*/ handle___05FV, CData/*0:0*/ reset, CData/*0:0*/ &io_enq_ready, CData/*0:0*/ io_enq_valid, CData/*1:0*/ io_enq_bits_wid, CData/*0:0*/ io_enq_bits_jump, IData/*31:0*/ io_enq_bits_new_pc, CData/*0:0*/ io_deq_ready, CData/*0:0*/ &io_deq_valid, CData/*1:0*/ &io_deq_bits_wid, CData/*0:0*/ &io_deq_bits_jump, IData/*31:0*/ &io_deq_bits_new_pc, QData/*63:0*/ &Queue_1_protectlib_combo_update__Vfuncrtn);

VL_INLINE_OPT void VALUexe___024root___ico_sequent__TOP__0(VALUexe___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VALUexe__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root___ico_sequent__TOP__0\n"); );
    // Body
    VL_ASSIGN_ISI(1,vlSelf->__Vcellinp__ALUexe__clock, vlSelf->clock);
    VL_ASSIGN_ISI(32,vlSelf->__Vcellinp__ALUexe__io_in_bits_in3, vlSelf->io_in_bits_in3);
    VL_ASSIGN_ISI(1,vlSelf->__Vcellinp__ALUexe__io_out2br_ready, vlSelf->io_out2br_ready);
    VL_ASSIGN_ISI(5,vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_reg_idxw, vlSelf->io_in_bits_ctrl_reg_idxw);
    VL_ASSIGN_ISI(1,vlSelf->__Vcellinp__ALUexe__io_out_ready, vlSelf->io_out_ready);
    VL_ASSIGN_ISI(2,vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_branch, vlSelf->io_in_bits_ctrl_branch);
    VL_ASSIGN_ISI(1,vlSelf->__Vcellinp__ALUexe__reset, vlSelf->reset);
    VL_ASSIGN_ISI(2,vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_wid, vlSelf->io_in_bits_ctrl_wid);
    VL_ASSIGN_ISI(1,vlSelf->__Vcellinp__ALUexe__io_in_valid, vlSelf->io_in_valid);
    VL_ASSIGN_ISI(32,vlSelf->__Vcellinp__ALUexe__io_in_bits_in1, vlSelf->io_in_bits_in1);
    VL_ASSIGN_ISI(32,vlSelf->__Vcellinp__ALUexe__io_in_bits_in2, vlSelf->io_in_bits_in2);
    VL_ASSIGN_ISI(6,vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_alu_fn, vlSelf->io_in_bits_ctrl_alu_fn);
    VL_ASSIGN_ISI(1,vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_wxd, vlSelf->io_in_bits_ctrl_wxd);
    vlSelf->ALUexe__DOT__result_br_io_enq_valid = ((IData)(vlSelf->__Vcellinp__ALUexe__io_in_valid) 
                                                   & (0U 
                                                      != (IData)(vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_branch)));
    VALUexe___024root____Vdpiimwrap_ALUexe__DOT__alu__DOT__ScalarALU_protectlib_combo_update_TOP(vlSelf->ALUexe__DOT__alu__DOT__handle___05FV, 
                                                                                (0x1fU 
                                                                                & (IData)(vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_alu_fn)), vlSelf->__Vcellinp__ALUexe__io_in_bits_in2, vlSelf->__Vcellinp__ALUexe__io_in_bits_in1, vlSelf->__Vfunc_ALUexe__DOT__alu__DOT__ScalarALU_protectlib_combo_update__2__io_out, vlSelf->__Vfunc_ALUexe__DOT__alu__DOT__ScalarALU_protectlib_combo_update__2__io_cmp_out, vlSelf->__Vfunc_ALUexe__DOT__alu__DOT__ScalarALU_protectlib_combo_update__2__Vfuncout);
    vlSelf->ALUexe__DOT__alu__DOT__io_out_combo___05FV 
        = vlSelf->__Vfunc_ALUexe__DOT__alu__DOT__ScalarALU_protectlib_combo_update__2__io_out;
    vlSelf->ALUexe__DOT__alu__DOT__io_cmp_out_combo___05FV 
        = vlSelf->__Vfunc_ALUexe__DOT__alu__DOT__ScalarALU_protectlib_combo_update__2__io_cmp_out;
    vlSelf->ALUexe__DOT__result_io_enq_valid = ((IData)(vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_wxd) 
                                                & (IData)(vlSelf->__Vcellinp__ALUexe__io_in_valid));
    vlSelf->ALUexe__DOT__result_br_io_enq_bits_jump 
        = ((3U == (IData)(vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_branch)) 
           | ((2U == (IData)(vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_branch)) 
              | ((1U == (IData)(vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_branch)) 
                 & (IData)(vlSelf->ALUexe__DOT__alu__DOT__io_cmp_out_combo___05FV))));
    VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result__DOT__Queue_protectlib_combo_update_TOP(vlSelf->ALUexe__DOT__result__DOT__handle___05FV, (IData)(vlSelf->__Vcellinp__ALUexe__reset), vlSelf->__Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_combo_update__6__io_enq_ready, (IData)(vlSelf->ALUexe__DOT__result_io_enq_valid), vlSelf->ALUexe__DOT__alu__DOT__io_out_combo___05FV, (IData)(vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_wxd), vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_reg_idxw, (IData)(vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_wid), vlSelf->__Vcellinp__ALUexe__io_out_ready, vlSelf->__Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_combo_update__6__io_deq_valid, vlSelf->__Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_combo_update__6__io_deq_bits_wb_wxd_rd, vlSelf->__Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_combo_update__6__io_deq_bits_wxd, vlSelf->__Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_combo_update__6__io_deq_bits_reg_idxw, vlSelf->__Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_combo_update__6__io_deq_bits_warp_id, vlSelf->__Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_combo_update__6__Vfuncout);
    vlSelf->ALUexe__DOT__result__DOT__io_enq_ready_combo___05FV 
        = vlSelf->__Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_combo_update__6__io_enq_ready;
    vlSelf->ALUexe__DOT__result__DOT__io_deq_valid_combo___05FV 
        = vlSelf->__Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_combo_update__6__io_deq_valid;
    vlSelf->ALUexe__DOT__result__DOT__io_deq_bits_wb_wxd_rd_combo___05FV 
        = vlSelf->__Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_combo_update__6__io_deq_bits_wb_wxd_rd;
    vlSelf->ALUexe__DOT__result__DOT__io_deq_bits_wxd_combo___05FV 
        = vlSelf->__Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_combo_update__6__io_deq_bits_wxd;
    vlSelf->ALUexe__DOT__result__DOT__io_deq_bits_reg_idxw_combo___05FV 
        = vlSelf->__Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_combo_update__6__io_deq_bits_reg_idxw;
    vlSelf->ALUexe__DOT__result__DOT__io_deq_bits_warp_id_combo___05FV 
        = vlSelf->__Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_combo_update__6__io_deq_bits_warp_id;
    vlSelf->ALUexe__DOT__result__DOT__last_combo_seqnum___05FV 
        = vlSelf->__Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_combo_update__6__Vfuncout;
    VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_combo_update_TOP(vlSelf->ALUexe__DOT__result_br__DOT__handle___05FV, (IData)(vlSelf->__Vcellinp__ALUexe__reset), vlSelf->__Vfunc_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_combo_update__12__io_enq_ready, (IData)(vlSelf->ALUexe__DOT__result_br_io_enq_valid), vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_wid, (IData)(vlSelf->ALUexe__DOT__result_br_io_enq_bits_jump), vlSelf->__Vcellinp__ALUexe__io_in_bits_in3, (IData)(vlSelf->__Vcellinp__ALUexe__io_out2br_ready), vlSelf->__Vfunc_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_combo_update__12__io_deq_valid, vlSelf->__Vfunc_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_combo_update__12__io_deq_bits_wid, vlSelf->__Vfunc_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_combo_update__12__io_deq_bits_jump, vlSelf->__Vfunc_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_combo_update__12__io_deq_bits_new_pc, vlSelf->__Vfunc_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_combo_update__12__Vfuncout);
    vlSelf->ALUexe__DOT__result_br__DOT__io_enq_ready_combo___05FV 
        = vlSelf->__Vfunc_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_combo_update__12__io_enq_ready;
    vlSelf->ALUexe__DOT__result_br__DOT__io_deq_valid_combo___05FV 
        = vlSelf->__Vfunc_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_combo_update__12__io_deq_valid;
    vlSelf->ALUexe__DOT__result_br__DOT__io_deq_bits_wid_combo___05FV 
        = vlSelf->__Vfunc_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_combo_update__12__io_deq_bits_wid;
    vlSelf->ALUexe__DOT__result_br__DOT__io_deq_bits_jump_combo___05FV 
        = vlSelf->__Vfunc_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_combo_update__12__io_deq_bits_jump;
    vlSelf->ALUexe__DOT__result_br__DOT__io_deq_bits_new_pc_combo___05FV 
        = vlSelf->__Vfunc_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_combo_update__12__io_deq_bits_new_pc;
    vlSelf->ALUexe__DOT__result_br__DOT__last_combo_seqnum___05FV 
        = vlSelf->__Vfunc_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_combo_update__12__Vfuncout;
    if ((vlSelf->ALUexe__DOT__result__DOT__last_seq_seqnum___05FV 
         > vlSelf->ALUexe__DOT__result__DOT__last_combo_seqnum___05FV)) {
        vlSelf->ALUexe__DOT__result_io_deq_valid = vlSelf->ALUexe__DOT__result__DOT__io_deq_valid_seq___05FV;
        vlSelf->ALUexe__DOT__result_io_deq_bits_wb_wxd_rd 
            = vlSelf->ALUexe__DOT__result__DOT__io_deq_bits_wb_wxd_rd_seq___05FV;
        vlSelf->ALUexe__DOT__result_io_deq_bits_wxd 
            = vlSelf->ALUexe__DOT__result__DOT__io_deq_bits_wxd_seq___05FV;
        vlSelf->ALUexe__DOT__result_io_deq_bits_reg_idxw 
            = vlSelf->ALUexe__DOT__result__DOT__io_deq_bits_reg_idxw_seq___05FV;
        vlSelf->ALUexe__DOT__result_io_deq_bits_warp_id 
            = vlSelf->ALUexe__DOT__result__DOT__io_deq_bits_warp_id_seq___05FV;
        vlSelf->ALUexe__DOT__result_io_enq_ready = vlSelf->ALUexe__DOT__result__DOT__io_enq_ready_seq___05FV;
    } else {
        vlSelf->ALUexe__DOT__result_io_deq_valid = vlSelf->ALUexe__DOT__result__DOT__io_deq_valid_combo___05FV;
        vlSelf->ALUexe__DOT__result_io_deq_bits_wb_wxd_rd 
            = vlSelf->ALUexe__DOT__result__DOT__io_deq_bits_wb_wxd_rd_combo___05FV;
        vlSelf->ALUexe__DOT__result_io_deq_bits_wxd 
            = vlSelf->ALUexe__DOT__result__DOT__io_deq_bits_wxd_combo___05FV;
        vlSelf->ALUexe__DOT__result_io_deq_bits_reg_idxw 
            = vlSelf->ALUexe__DOT__result__DOT__io_deq_bits_reg_idxw_combo___05FV;
        vlSelf->ALUexe__DOT__result_io_deq_bits_warp_id 
            = vlSelf->ALUexe__DOT__result__DOT__io_deq_bits_warp_id_combo___05FV;
        vlSelf->ALUexe__DOT__result_io_enq_ready = vlSelf->ALUexe__DOT__result__DOT__io_enq_ready_combo___05FV;
    }
    if ((vlSelf->ALUexe__DOT__result_br__DOT__last_seq_seqnum___05FV 
         > vlSelf->ALUexe__DOT__result_br__DOT__last_combo_seqnum___05FV)) {
        vlSelf->ALUexe__DOT__result_br_io_deq_valid 
            = vlSelf->ALUexe__DOT__result_br__DOT__io_deq_valid_seq___05FV;
        vlSelf->ALUexe__DOT__result_br_io_deq_bits_wid 
            = vlSelf->ALUexe__DOT__result_br__DOT__io_deq_bits_wid_seq___05FV;
        vlSelf->ALUexe__DOT__result_br_io_deq_bits_jump 
            = vlSelf->ALUexe__DOT__result_br__DOT__io_deq_bits_jump_seq___05FV;
        vlSelf->ALUexe__DOT__result_br_io_deq_bits_new_pc 
            = vlSelf->ALUexe__DOT__result_br__DOT__io_deq_bits_new_pc_seq___05FV;
        vlSelf->ALUexe__DOT__result_br_io_enq_ready 
            = vlSelf->ALUexe__DOT__result_br__DOT__io_enq_ready_seq___05FV;
    } else {
        vlSelf->ALUexe__DOT__result_br_io_deq_valid 
            = vlSelf->ALUexe__DOT__result_br__DOT__io_deq_valid_combo___05FV;
        vlSelf->ALUexe__DOT__result_br_io_deq_bits_wid 
            = vlSelf->ALUexe__DOT__result_br__DOT__io_deq_bits_wid_combo___05FV;
        vlSelf->ALUexe__DOT__result_br_io_deq_bits_jump 
            = vlSelf->ALUexe__DOT__result_br__DOT__io_deq_bits_jump_combo___05FV;
        vlSelf->ALUexe__DOT__result_br_io_deq_bits_new_pc 
            = vlSelf->ALUexe__DOT__result_br__DOT__io_deq_bits_new_pc_combo___05FV;
        vlSelf->ALUexe__DOT__result_br_io_enq_ready 
            = vlSelf->ALUexe__DOT__result_br__DOT__io_enq_ready_combo___05FV;
    }
    VL_ASSIGN_SII(1,vlSelf->io_out_valid, vlSelf->ALUexe__DOT__result_io_deq_valid);
    VL_ASSIGN_SII(32,vlSelf->io_out_bits_wb_wxd_rd, vlSelf->ALUexe__DOT__result_io_deq_bits_wb_wxd_rd);
    VL_ASSIGN_SII(1,vlSelf->io_out_bits_wxd, vlSelf->ALUexe__DOT__result_io_deq_bits_wxd);
    VL_ASSIGN_SII(5,vlSelf->io_out_bits_reg_idxw, vlSelf->ALUexe__DOT__result_io_deq_bits_reg_idxw);
    VL_ASSIGN_SII(2,vlSelf->io_out_bits_warp_id, vlSelf->ALUexe__DOT__result_io_deq_bits_warp_id);
    VL_ASSIGN_SII(1,vlSelf->io_out2br_valid, vlSelf->ALUexe__DOT__result_br_io_deq_valid);
    VL_ASSIGN_SII(2,vlSelf->io_out2br_bits_wid, vlSelf->ALUexe__DOT__result_br_io_deq_bits_wid);
    VL_ASSIGN_SII(1,vlSelf->io_out2br_bits_jump, vlSelf->ALUexe__DOT__result_br_io_deq_bits_jump);
    VL_ASSIGN_SII(32,vlSelf->io_out2br_bits_new_pc, vlSelf->ALUexe__DOT__result_br_io_deq_bits_new_pc);
    VL_ASSIGN_SII(1,vlSelf->io_in_ready, ((0U == (IData)(vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_branch))
                                           ? (IData)(vlSelf->ALUexe__DOT__result_io_enq_ready)
                                           : ((1U == (IData)(vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_branch))
                                               ? (IData)(vlSelf->ALUexe__DOT__result_br_io_enq_ready)
                                               : ((IData)(vlSelf->ALUexe__DOT__result_br_io_enq_ready) 
                                                  & (IData)(vlSelf->ALUexe__DOT__result_io_enq_ready)))));
}

void VALUexe___024root___eval_ico(VALUexe___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VALUexe__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root___eval_ico\n"); );
    // Body
    if (vlSelf->__VicoTriggered.at(0U)) {
        VALUexe___024root___ico_sequent__TOP__0(vlSelf);
    }
}

void VALUexe___024root___eval_act(VALUexe___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VALUexe__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root___eval_act\n"); );
}

void VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_combo_ignore_TOP(QData/*63:0*/ handle___05FV, CData/*0:0*/ reset, CData/*0:0*/ io_enq_valid, CData/*1:0*/ io_enq_bits_wid, CData/*0:0*/ io_enq_bits_jump, IData/*31:0*/ io_enq_bits_new_pc, CData/*0:0*/ io_deq_ready);
void VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_seq_update_TOP(QData/*63:0*/ handle___05FV, CData/*0:0*/ clock, CData/*0:0*/ &io_enq_ready, CData/*0:0*/ &io_deq_valid, CData/*1:0*/ &io_deq_bits_wid, CData/*0:0*/ &io_deq_bits_jump, IData/*31:0*/ &io_deq_bits_new_pc, QData/*63:0*/ &Queue_1_protectlib_seq_update__Vfuncrtn);
void VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result__DOT__Queue_protectlib_combo_ignore_TOP(QData/*63:0*/ handle___05FV, CData/*0:0*/ reset, CData/*0:0*/ io_enq_valid, IData/*31:0*/ io_enq_bits_wb_wxd_rd, CData/*0:0*/ io_enq_bits_wxd, CData/*4:0*/ io_enq_bits_reg_idxw, CData/*1:0*/ io_enq_bits_warp_id, CData/*0:0*/ io_deq_ready);
void VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result__DOT__Queue_protectlib_seq_update_TOP(QData/*63:0*/ handle___05FV, CData/*0:0*/ clock, CData/*0:0*/ &io_enq_ready, CData/*0:0*/ &io_deq_valid, IData/*31:0*/ &io_deq_bits_wb_wxd_rd, CData/*0:0*/ &io_deq_bits_wxd, CData/*4:0*/ &io_deq_bits_reg_idxw, CData/*1:0*/ &io_deq_bits_warp_id, QData/*63:0*/ &Queue_protectlib_seq_update__Vfuncrtn);

VL_INLINE_OPT void VALUexe___024root___nba_sequent__TOP__0(VALUexe___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VALUexe__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root___nba_sequent__TOP__0\n"); );
    // Init
    CData/*0:0*/ ALUexe__DOT__result__DOT__io_enq_ready_tmp___05FV;
    CData/*0:0*/ ALUexe__DOT__result__DOT__io_deq_valid_tmp___05FV;
    IData/*31:0*/ ALUexe__DOT__result__DOT__io_deq_bits_wb_wxd_rd_tmp___05FV;
    CData/*0:0*/ ALUexe__DOT__result__DOT__io_deq_bits_wxd_tmp___05FV;
    CData/*4:0*/ ALUexe__DOT__result__DOT__io_deq_bits_reg_idxw_tmp___05FV;
    CData/*1:0*/ ALUexe__DOT__result__DOT__io_deq_bits_warp_id_tmp___05FV;
    CData/*0:0*/ ALUexe__DOT__result_br__DOT__io_enq_ready_tmp___05FV;
    CData/*0:0*/ ALUexe__DOT__result_br__DOT__io_deq_valid_tmp___05FV;
    CData/*1:0*/ ALUexe__DOT__result_br__DOT__io_deq_bits_wid_tmp___05FV;
    CData/*0:0*/ ALUexe__DOT__result_br__DOT__io_deq_bits_jump_tmp___05FV;
    IData/*31:0*/ ALUexe__DOT__result_br__DOT__io_deq_bits_new_pc_tmp___05FV;
    QData/*63:0*/ __Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_seq_update__8__Vfuncout;
    CData/*0:0*/ __Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_seq_update__8__io_enq_ready;
    CData/*0:0*/ __Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_seq_update__8__io_deq_valid;
    IData/*31:0*/ __Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_seq_update__8__io_deq_bits_wb_wxd_rd;
    CData/*0:0*/ __Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_seq_update__8__io_deq_bits_wxd;
    CData/*4:0*/ __Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_seq_update__8__io_deq_bits_reg_idxw;
    CData/*1:0*/ __Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_seq_update__8__io_deq_bits_warp_id;
    QData/*63:0*/ __Vfunc_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_seq_update__14__Vfuncout;
    CData/*0:0*/ __Vfunc_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_seq_update__14__io_enq_ready;
    CData/*0:0*/ __Vfunc_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_seq_update__14__io_deq_valid;
    CData/*1:0*/ __Vfunc_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_seq_update__14__io_deq_bits_wid;
    CData/*0:0*/ __Vfunc_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_seq_update__14__io_deq_bits_jump;
    IData/*31:0*/ __Vfunc_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_seq_update__14__io_deq_bits_new_pc;
    // Body
    VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_combo_ignore_TOP(vlSelf->ALUexe__DOT__result_br__DOT__handle___05FV, (IData)(vlSelf->__Vcellinp__ALUexe__reset), vlSelf->ALUexe__DOT__result_br_io_enq_valid, (IData)(vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_wid), vlSelf->ALUexe__DOT__result_br_io_enq_bits_jump, vlSelf->__Vcellinp__ALUexe__io_in_bits_in3, (IData)(vlSelf->__Vcellinp__ALUexe__io_out2br_ready));
    VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_seq_update_TOP(vlSelf->ALUexe__DOT__result_br__DOT__handle___05FV, (IData)(vlSelf->__Vcellinp__ALUexe__clock), __Vfunc_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_seq_update__14__io_enq_ready, __Vfunc_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_seq_update__14__io_deq_valid, __Vfunc_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_seq_update__14__io_deq_bits_wid, __Vfunc_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_seq_update__14__io_deq_bits_jump, __Vfunc_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_seq_update__14__io_deq_bits_new_pc, __Vfunc_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_seq_update__14__Vfuncout);
    ALUexe__DOT__result_br__DOT__io_enq_ready_tmp___05FV 
        = __Vfunc_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_seq_update__14__io_enq_ready;
    ALUexe__DOT__result_br__DOT__io_deq_valid_tmp___05FV 
        = __Vfunc_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_seq_update__14__io_deq_valid;
    ALUexe__DOT__result_br__DOT__io_deq_bits_wid_tmp___05FV 
        = __Vfunc_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_seq_update__14__io_deq_bits_wid;
    ALUexe__DOT__result_br__DOT__io_deq_bits_jump_tmp___05FV 
        = __Vfunc_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_seq_update__14__io_deq_bits_jump;
    ALUexe__DOT__result_br__DOT__io_deq_bits_new_pc_tmp___05FV 
        = __Vfunc_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_seq_update__14__io_deq_bits_new_pc;
    vlSelf->ALUexe__DOT__result_br__DOT__last_seq_seqnum___05FV 
        = __Vfunc_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_seq_update__14__Vfuncout;
    vlSelf->ALUexe__DOT__result_br__DOT__io_enq_ready_seq___05FV 
        = ALUexe__DOT__result_br__DOT__io_enq_ready_tmp___05FV;
    vlSelf->ALUexe__DOT__result_br__DOT__io_deq_valid_seq___05FV 
        = ALUexe__DOT__result_br__DOT__io_deq_valid_tmp___05FV;
    vlSelf->ALUexe__DOT__result_br__DOT__io_deq_bits_wid_seq___05FV 
        = ALUexe__DOT__result_br__DOT__io_deq_bits_wid_tmp___05FV;
    vlSelf->ALUexe__DOT__result_br__DOT__io_deq_bits_jump_seq___05FV 
        = ALUexe__DOT__result_br__DOT__io_deq_bits_jump_tmp___05FV;
    vlSelf->ALUexe__DOT__result_br__DOT__io_deq_bits_new_pc_seq___05FV 
        = ALUexe__DOT__result_br__DOT__io_deq_bits_new_pc_tmp___05FV;
    VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result__DOT__Queue_protectlib_combo_ignore_TOP(vlSelf->ALUexe__DOT__result__DOT__handle___05FV, (IData)(vlSelf->__Vcellinp__ALUexe__reset), vlSelf->ALUexe__DOT__result_io_enq_valid, vlSelf->ALUexe__DOT__alu__DOT__io_out_combo___05FV, (IData)(vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_wxd), vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_reg_idxw, (IData)(vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_wid), vlSelf->__Vcellinp__ALUexe__io_out_ready);
    VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result__DOT__Queue_protectlib_seq_update_TOP(vlSelf->ALUexe__DOT__result__DOT__handle___05FV, (IData)(vlSelf->__Vcellinp__ALUexe__clock), __Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_seq_update__8__io_enq_ready, __Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_seq_update__8__io_deq_valid, __Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_seq_update__8__io_deq_bits_wb_wxd_rd, __Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_seq_update__8__io_deq_bits_wxd, __Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_seq_update__8__io_deq_bits_reg_idxw, __Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_seq_update__8__io_deq_bits_warp_id, __Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_seq_update__8__Vfuncout);
    ALUexe__DOT__result__DOT__io_enq_ready_tmp___05FV 
        = __Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_seq_update__8__io_enq_ready;
    ALUexe__DOT__result__DOT__io_deq_valid_tmp___05FV 
        = __Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_seq_update__8__io_deq_valid;
    ALUexe__DOT__result__DOT__io_deq_bits_wb_wxd_rd_tmp___05FV 
        = __Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_seq_update__8__io_deq_bits_wb_wxd_rd;
    ALUexe__DOT__result__DOT__io_deq_bits_wxd_tmp___05FV 
        = __Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_seq_update__8__io_deq_bits_wxd;
    ALUexe__DOT__result__DOT__io_deq_bits_reg_idxw_tmp___05FV 
        = __Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_seq_update__8__io_deq_bits_reg_idxw;
    ALUexe__DOT__result__DOT__io_deq_bits_warp_id_tmp___05FV 
        = __Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_seq_update__8__io_deq_bits_warp_id;
    vlSelf->ALUexe__DOT__result__DOT__last_seq_seqnum___05FV 
        = __Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_seq_update__8__Vfuncout;
    vlSelf->ALUexe__DOT__result__DOT__io_enq_ready_seq___05FV 
        = ALUexe__DOT__result__DOT__io_enq_ready_tmp___05FV;
    vlSelf->ALUexe__DOT__result__DOT__io_deq_valid_seq___05FV 
        = ALUexe__DOT__result__DOT__io_deq_valid_tmp___05FV;
    vlSelf->ALUexe__DOT__result__DOT__io_deq_bits_wb_wxd_rd_seq___05FV 
        = ALUexe__DOT__result__DOT__io_deq_bits_wb_wxd_rd_tmp___05FV;
    vlSelf->ALUexe__DOT__result__DOT__io_deq_bits_wxd_seq___05FV 
        = ALUexe__DOT__result__DOT__io_deq_bits_wxd_tmp___05FV;
    vlSelf->ALUexe__DOT__result__DOT__io_deq_bits_reg_idxw_seq___05FV 
        = ALUexe__DOT__result__DOT__io_deq_bits_reg_idxw_tmp___05FV;
    vlSelf->ALUexe__DOT__result__DOT__io_deq_bits_warp_id_seq___05FV 
        = ALUexe__DOT__result__DOT__io_deq_bits_warp_id_tmp___05FV;
}

VL_INLINE_OPT void VALUexe___024root___nba_sequent__TOP__1(VALUexe___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VALUexe__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root___nba_sequent__TOP__1\n"); );
    // Body
    if ((vlSelf->ALUexe__DOT__result_br__DOT__last_seq_seqnum___05FV 
         > vlSelf->ALUexe__DOT__result_br__DOT__last_combo_seqnum___05FV)) {
        vlSelf->ALUexe__DOT__result_br_io_deq_valid 
            = vlSelf->ALUexe__DOT__result_br__DOT__io_deq_valid_seq___05FV;
        vlSelf->ALUexe__DOT__result_br_io_deq_bits_wid 
            = vlSelf->ALUexe__DOT__result_br__DOT__io_deq_bits_wid_seq___05FV;
        vlSelf->ALUexe__DOT__result_br_io_deq_bits_jump 
            = vlSelf->ALUexe__DOT__result_br__DOT__io_deq_bits_jump_seq___05FV;
        vlSelf->ALUexe__DOT__result_br_io_deq_bits_new_pc 
            = vlSelf->ALUexe__DOT__result_br__DOT__io_deq_bits_new_pc_seq___05FV;
        vlSelf->ALUexe__DOT__result_br_io_enq_ready 
            = vlSelf->ALUexe__DOT__result_br__DOT__io_enq_ready_seq___05FV;
    } else {
        vlSelf->ALUexe__DOT__result_br_io_deq_valid 
            = vlSelf->ALUexe__DOT__result_br__DOT__io_deq_valid_combo___05FV;
        vlSelf->ALUexe__DOT__result_br_io_deq_bits_wid 
            = vlSelf->ALUexe__DOT__result_br__DOT__io_deq_bits_wid_combo___05FV;
        vlSelf->ALUexe__DOT__result_br_io_deq_bits_jump 
            = vlSelf->ALUexe__DOT__result_br__DOT__io_deq_bits_jump_combo___05FV;
        vlSelf->ALUexe__DOT__result_br_io_deq_bits_new_pc 
            = vlSelf->ALUexe__DOT__result_br__DOT__io_deq_bits_new_pc_combo___05FV;
        vlSelf->ALUexe__DOT__result_br_io_enq_ready 
            = vlSelf->ALUexe__DOT__result_br__DOT__io_enq_ready_combo___05FV;
    }
    if ((vlSelf->ALUexe__DOT__result__DOT__last_seq_seqnum___05FV 
         > vlSelf->ALUexe__DOT__result__DOT__last_combo_seqnum___05FV)) {
        vlSelf->ALUexe__DOT__result_io_deq_valid = vlSelf->ALUexe__DOT__result__DOT__io_deq_valid_seq___05FV;
        vlSelf->ALUexe__DOT__result_io_deq_bits_wb_wxd_rd 
            = vlSelf->ALUexe__DOT__result__DOT__io_deq_bits_wb_wxd_rd_seq___05FV;
        vlSelf->ALUexe__DOT__result_io_deq_bits_wxd 
            = vlSelf->ALUexe__DOT__result__DOT__io_deq_bits_wxd_seq___05FV;
        vlSelf->ALUexe__DOT__result_io_deq_bits_reg_idxw 
            = vlSelf->ALUexe__DOT__result__DOT__io_deq_bits_reg_idxw_seq___05FV;
        vlSelf->ALUexe__DOT__result_io_deq_bits_warp_id 
            = vlSelf->ALUexe__DOT__result__DOT__io_deq_bits_warp_id_seq___05FV;
        vlSelf->ALUexe__DOT__result_io_enq_ready = vlSelf->ALUexe__DOT__result__DOT__io_enq_ready_seq___05FV;
    } else {
        vlSelf->ALUexe__DOT__result_io_deq_valid = vlSelf->ALUexe__DOT__result__DOT__io_deq_valid_combo___05FV;
        vlSelf->ALUexe__DOT__result_io_deq_bits_wb_wxd_rd 
            = vlSelf->ALUexe__DOT__result__DOT__io_deq_bits_wb_wxd_rd_combo___05FV;
        vlSelf->ALUexe__DOT__result_io_deq_bits_wxd 
            = vlSelf->ALUexe__DOT__result__DOT__io_deq_bits_wxd_combo___05FV;
        vlSelf->ALUexe__DOT__result_io_deq_bits_reg_idxw 
            = vlSelf->ALUexe__DOT__result__DOT__io_deq_bits_reg_idxw_combo___05FV;
        vlSelf->ALUexe__DOT__result_io_deq_bits_warp_id 
            = vlSelf->ALUexe__DOT__result__DOT__io_deq_bits_warp_id_combo___05FV;
        vlSelf->ALUexe__DOT__result_io_enq_ready = vlSelf->ALUexe__DOT__result__DOT__io_enq_ready_combo___05FV;
    }
    VL_ASSIGN_SII(1,vlSelf->io_out2br_valid, vlSelf->ALUexe__DOT__result_br_io_deq_valid);
    VL_ASSIGN_SII(2,vlSelf->io_out2br_bits_wid, vlSelf->ALUexe__DOT__result_br_io_deq_bits_wid);
    VL_ASSIGN_SII(1,vlSelf->io_out2br_bits_jump, vlSelf->ALUexe__DOT__result_br_io_deq_bits_jump);
    VL_ASSIGN_SII(32,vlSelf->io_out2br_bits_new_pc, vlSelf->ALUexe__DOT__result_br_io_deq_bits_new_pc);
    VL_ASSIGN_SII(1,vlSelf->io_out_valid, vlSelf->ALUexe__DOT__result_io_deq_valid);
    VL_ASSIGN_SII(32,vlSelf->io_out_bits_wb_wxd_rd, vlSelf->ALUexe__DOT__result_io_deq_bits_wb_wxd_rd);
    VL_ASSIGN_SII(1,vlSelf->io_out_bits_wxd, vlSelf->ALUexe__DOT__result_io_deq_bits_wxd);
    VL_ASSIGN_SII(5,vlSelf->io_out_bits_reg_idxw, vlSelf->ALUexe__DOT__result_io_deq_bits_reg_idxw);
    VL_ASSIGN_SII(2,vlSelf->io_out_bits_warp_id, vlSelf->ALUexe__DOT__result_io_deq_bits_warp_id);
    VL_ASSIGN_SII(1,vlSelf->io_in_ready, ((0U == (IData)(vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_branch))
                                           ? (IData)(vlSelf->ALUexe__DOT__result_io_enq_ready)
                                           : ((1U == (IData)(vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_branch))
                                               ? (IData)(vlSelf->ALUexe__DOT__result_br_io_enq_ready)
                                               : ((IData)(vlSelf->ALUexe__DOT__result_br_io_enq_ready) 
                                                  & (IData)(vlSelf->ALUexe__DOT__result_io_enq_ready)))));
}

void VALUexe___024root___eval_nba(VALUexe___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VALUexe__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root___eval_nba\n"); );
    // Body
    if (vlSelf->__VnbaTriggered.at(0U)) {
        VALUexe___024root___nba_sequent__TOP__0(vlSelf);
        VALUexe___024root___nba_sequent__TOP__1(vlSelf);
    }
}

void VALUexe___024root___eval_triggers__ico(VALUexe___024root* vlSelf);
#ifdef VL_DEBUG
VL_ATTR_COLD void VALUexe___024root___dump_triggers__ico(VALUexe___024root* vlSelf);
#endif  // VL_DEBUG
void VALUexe___024root___eval_triggers__act(VALUexe___024root* vlSelf);
#ifdef VL_DEBUG
VL_ATTR_COLD void VALUexe___024root___dump_triggers__act(VALUexe___024root* vlSelf);
#endif  // VL_DEBUG
#ifdef VL_DEBUG
VL_ATTR_COLD void VALUexe___024root___dump_triggers__nba(VALUexe___024root* vlSelf);
#endif  // VL_DEBUG

void VALUexe___024root___eval(VALUexe___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VALUexe__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root___eval\n"); );
    // Init
    CData/*0:0*/ __VicoContinue;
    VlTriggerVec<1> __VpreTriggered;
    IData/*31:0*/ __VnbaIterCount;
    CData/*0:0*/ __VnbaContinue;
    // Body
    vlSelf->__VicoIterCount = 0U;
    __VicoContinue = 1U;
    while (__VicoContinue) {
        __VicoContinue = 0U;
        VALUexe___024root___eval_triggers__ico(vlSelf);
        if (vlSelf->__VicoTriggered.any()) {
            __VicoContinue = 1U;
            if ((0x64U < vlSelf->__VicoIterCount)) {
#ifdef VL_DEBUG
                VALUexe___024root___dump_triggers__ico(vlSelf);
#endif
                VL_FATAL_MT("generated/ALUexe.v", 432, "", "Input combinational region did not converge.");
            }
            vlSelf->__VicoIterCount = ((IData)(1U) 
                                       + vlSelf->__VicoIterCount);
            VALUexe___024root___eval_ico(vlSelf);
        }
    }
    __VnbaIterCount = 0U;
    __VnbaContinue = 1U;
    while (__VnbaContinue) {
        __VnbaContinue = 0U;
        vlSelf->__VnbaTriggered.clear();
        vlSelf->__VactIterCount = 0U;
        vlSelf->__VactContinue = 1U;
        while (vlSelf->__VactContinue) {
            vlSelf->__VactContinue = 0U;
            VALUexe___024root___eval_triggers__act(vlSelf);
            if (vlSelf->__VactTriggered.any()) {
                vlSelf->__VactContinue = 1U;
                if ((0x64U < vlSelf->__VactIterCount)) {
#ifdef VL_DEBUG
                    VALUexe___024root___dump_triggers__act(vlSelf);
#endif
                    VL_FATAL_MT("generated/ALUexe.v", 432, "", "Active region did not converge.");
                }
                vlSelf->__VactIterCount = ((IData)(1U) 
                                           + vlSelf->__VactIterCount);
                __VpreTriggered.andNot(vlSelf->__VactTriggered, vlSelf->__VnbaTriggered);
                vlSelf->__VnbaTriggered.set(vlSelf->__VactTriggered);
                VALUexe___024root___eval_act(vlSelf);
            }
        }
        if (vlSelf->__VnbaTriggered.any()) {
            __VnbaContinue = 1U;
            if ((0x64U < __VnbaIterCount)) {
#ifdef VL_DEBUG
                VALUexe___024root___dump_triggers__nba(vlSelf);
#endif
                VL_FATAL_MT("generated/ALUexe.v", 432, "", "NBA region did not converge.");
            }
            __VnbaIterCount = ((IData)(1U) + __VnbaIterCount);
            VALUexe___024root___eval_nba(vlSelf);
        }
    }
}

#ifdef VL_DEBUG
void VALUexe___024root___eval_debug_assertions(VALUexe___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VALUexe__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root___eval_debug_assertions\n"); );
}
#endif  // VL_DEBUG

// Verilated -*- SystemC -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See VALUexe.h for the primary calling header

#include "verilated.h"

#include "VALUexe___024root.h"

VL_INLINE_OPT void VALUexe___024root___ico_sequent__TOP__0(VALUexe___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VALUexe__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root___ico_sequent__TOP__0\n"); );
    // Init
    IData/*31:0*/ __Vcellinp__ALUexe__io_in_bits_in2;
    CData/*0:0*/ ALUexe__DOT__result_io_enq_ready;
    CData/*0:0*/ ALUexe__DOT__result_br_io_enq_ready;
    CData/*0:0*/ ALUexe__DOT__alu__DOT___in2_inv_T_2;
    IData/*31:0*/ ALUexe__DOT__alu__DOT__adder_out;
    CData/*0:0*/ ALUexe__DOT__alu__DOT___shin_T_2;
    IData/*31:0*/ ALUexe__DOT__alu__DOT___shin_T_11;
    IData/*31:0*/ ALUexe__DOT__alu__DOT___shin_T_21;
    IData/*31:0*/ ALUexe__DOT__alu__DOT___shin_T_31;
    IData/*31:0*/ ALUexe__DOT__alu__DOT___shin_T_41;
    IData/*31:0*/ ALUexe__DOT__alu__DOT__shin;
    QData/*32:0*/ ALUexe__DOT__alu__DOT___shout_r_T_7;
    IData/*31:0*/ ALUexe__DOT__alu__DOT___shout_l_T_18;
    IData/*31:0*/ ALUexe__DOT__alu__DOT___shout_l_T_28;
    IData/*31:0*/ ALUexe__DOT__alu__DOT___shout_l_T_38;
    CData/*0:0*/ ALUexe__DOT__alu__DOT___minu_T;
    CData/*0:0*/ ALUexe__DOT__alu__DOT___mins_T;
    // Body
    VL_ASSIGN_ISI(1,vlSelf->__Vcellinp__ALUexe__clock, vlSelf->clock);
    VL_ASSIGN_ISI(1,vlSelf->__Vcellinp__ALUexe__reset, vlSelf->reset);
    VL_ASSIGN_ISI(32,vlSelf->__Vcellinp__ALUexe__io_in_bits_in3, vlSelf->io_in_bits_in3);
    VL_ASSIGN_ISI(2,vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_wid, vlSelf->io_in_bits_ctrl_wid);
    VL_ASSIGN_ISI(5,vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_reg_idxw, vlSelf->io_in_bits_ctrl_reg_idxw);
    VL_ASSIGN_ISI(1,vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_wxd, vlSelf->io_in_bits_ctrl_wxd);
    VL_ASSIGN_ISI(1,vlSelf->__Vcellinp__ALUexe__io_in_valid, vlSelf->io_in_valid);
    VL_ASSIGN_ISI(2,vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_branch, vlSelf->io_in_bits_ctrl_branch);
    VL_ASSIGN_ISI(1,vlSelf->__Vcellinp__ALUexe__io_out_ready, vlSelf->io_out_ready);
    VL_ASSIGN_ISI(1,vlSelf->__Vcellinp__ALUexe__io_out2br_ready, vlSelf->io_out2br_ready);
    VL_ASSIGN_ISI(32,__Vcellinp__ALUexe__io_in_bits_in2, vlSelf->io_in_bits_in2);
    VL_ASSIGN_ISI(32,vlSelf->__Vcellinp__ALUexe__io_in_bits_in1, vlSelf->io_in_bits_in1);
    VL_ASSIGN_ISI(6,vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_alu_fn, vlSelf->io_in_bits_ctrl_alu_fn);
    vlSelf->ALUexe__DOT__result__DOT__do_deq = ((IData)(vlSelf->ALUexe__DOT__result__DOT__maybe_full) 
                                                & (IData)(vlSelf->__Vcellinp__ALUexe__io_out_ready));
    ALUexe__DOT__result_io_enq_ready = (1U & ((~ (IData)(vlSelf->ALUexe__DOT__result__DOT__maybe_full)) 
                                              | (IData)(vlSelf->__Vcellinp__ALUexe__io_out_ready)));
    vlSelf->ALUexe__DOT__result_br__DOT__do_deq = ((IData)(vlSelf->ALUexe__DOT__result_br__DOT__maybe_full) 
                                                   & (IData)(vlSelf->__Vcellinp__ALUexe__io_out2br_ready));
    ALUexe__DOT__result_br_io_enq_ready = (1U & ((~ (IData)(vlSelf->ALUexe__DOT__result_br__DOT__maybe_full)) 
                                                 | (IData)(vlSelf->__Vcellinp__ALUexe__io_out2br_ready)));
    ALUexe__DOT__alu__DOT___mins_T = VL_GTS_III(32, vlSelf->__Vcellinp__ALUexe__io_in_bits_in1, __Vcellinp__ALUexe__io_in_bits_in2);
    ALUexe__DOT__alu__DOT___minu_T = (vlSelf->__Vcellinp__ALUexe__io_in_bits_in1 
                                      > __Vcellinp__ALUexe__io_in_bits_in2);
    ALUexe__DOT__alu__DOT___shin_T_11 = ((vlSelf->__Vcellinp__ALUexe__io_in_bits_in1 
                                          >> 0x10U) 
                                         | (vlSelf->__Vcellinp__ALUexe__io_in_bits_in1 
                                            << 0x10U));
    ALUexe__DOT__alu__DOT___shin_T_2 = ((5U == (0x1fU 
                                                & (IData)(vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_alu_fn))) 
                                        | (0xbU == 
                                           (0x1fU & (IData)(vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_alu_fn))));
    ALUexe__DOT__alu__DOT___in2_inv_T_2 = ((0xaU <= 
                                            (0x1fU 
                                             & (IData)(vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_alu_fn))) 
                                           & (0xfU 
                                              >= (0x1fU 
                                                  & (IData)(vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_alu_fn))));
    vlSelf->ALUexe__DOT__result__DOT__ram_wb_wxd_rd_MPORT_en 
        = ((IData)(ALUexe__DOT__result_io_enq_ready) 
           & ((IData)(vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_wxd) 
              & (IData)(vlSelf->__Vcellinp__ALUexe__io_in_valid)));
    VL_ASSIGN_SII(1,vlSelf->io_in_ready, ((0U == (IData)(vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_branch))
                                           ? (IData)(ALUexe__DOT__result_io_enq_ready)
                                           : ((1U == (IData)(vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_branch))
                                               ? (IData)(ALUexe__DOT__result_br_io_enq_ready)
                                               : ((IData)(ALUexe__DOT__result_br_io_enq_ready) 
                                                  & (IData)(ALUexe__DOT__result_io_enq_ready)))));
    vlSelf->ALUexe__DOT__result_br__DOT__ram_wid_MPORT_en 
        = ((IData)(ALUexe__DOT__result_br_io_enq_ready) 
           & ((IData)(vlSelf->__Vcellinp__ALUexe__io_in_valid) 
              & (0U != (IData)(vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_branch))));
    ALUexe__DOT__alu__DOT___shin_T_21 = ((0xff00ffU 
                                          & (ALUexe__DOT__alu__DOT___shin_T_11 
                                             >> 8U)) 
                                         | (0xff00ff00U 
                                            & (ALUexe__DOT__alu__DOT___shin_T_11 
                                               << 8U)));
    vlSelf->ALUexe__DOT__alu__DOT__in2_inv = ((IData)(ALUexe__DOT__alu__DOT___in2_inv_T_2)
                                               ? (~ __Vcellinp__ALUexe__io_in_bits_in2)
                                               : __Vcellinp__ALUexe__io_in_bits_in2);
    ALUexe__DOT__alu__DOT___shin_T_31 = ((0xf0f0f0fU 
                                          & (ALUexe__DOT__alu__DOT___shin_T_21 
                                             >> 4U)) 
                                         | (0xf0f0f0f0U 
                                            & (ALUexe__DOT__alu__DOT___shin_T_21 
                                               << 4U)));
    ALUexe__DOT__alu__DOT__adder_out = (vlSelf->__Vcellinp__ALUexe__io_in_bits_in1 
                                        + (vlSelf->ALUexe__DOT__alu__DOT__in2_inv 
                                           + (IData)(ALUexe__DOT__alu__DOT___in2_inv_T_2)));
    ALUexe__DOT__alu__DOT___shin_T_41 = ((0x33333333U 
                                          & (ALUexe__DOT__alu__DOT___shin_T_31 
                                             >> 2U)) 
                                         | (0xccccccccU 
                                            & (ALUexe__DOT__alu__DOT___shin_T_31 
                                               << 2U)));
    vlSelf->ALUexe__DOT__alu__DOT__slt = (1U & (((vlSelf->__Vcellinp__ALUexe__io_in_bits_in1 
                                                  >> 0x1fU) 
                                                 == 
                                                 (__Vcellinp__ALUexe__io_in_bits_in2 
                                                  >> 0x1fU))
                                                 ? 
                                                (ALUexe__DOT__alu__DOT__adder_out 
                                                 >> 0x1fU)
                                                 : 
                                                ((2U 
                                                  & (IData)(vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_alu_fn))
                                                  ? 
                                                 (__Vcellinp__ALUexe__io_in_bits_in2 
                                                  >> 0x1fU)
                                                  : 
                                                 (vlSelf->__Vcellinp__ALUexe__io_in_bits_in1 
                                                  >> 0x1fU))));
    ALUexe__DOT__alu__DOT__shin = ((IData)(ALUexe__DOT__alu__DOT___shin_T_2)
                                    ? vlSelf->__Vcellinp__ALUexe__io_in_bits_in1
                                    : ((0x55555555U 
                                        & (ALUexe__DOT__alu__DOT___shin_T_41 
                                           >> 1U)) 
                                       | (0xaaaaaaaaU 
                                          & (ALUexe__DOT__alu__DOT___shin_T_41 
                                             << 1U))));
    ALUexe__DOT__alu__DOT___shout_r_T_7 = (0x1ffffffffULL 
                                           & VL_SHIFTRS_QQI(33,33,5, 
                                                            (((QData)((IData)(
                                                                              ((IData)(ALUexe__DOT__alu__DOT___in2_inv_T_2) 
                                                                               & (ALUexe__DOT__alu__DOT__shin 
                                                                                >> 0x1fU)))) 
                                                              << 0x20U) 
                                                             | (QData)((IData)(ALUexe__DOT__alu__DOT__shin))), 
                                                            (0x1fU 
                                                             & __Vcellinp__ALUexe__io_in_bits_in2)));
    ALUexe__DOT__alu__DOT___shout_l_T_18 = ((0xff00ffU 
                                             & ((0xffff00U 
                                                 & ((IData)(ALUexe__DOT__alu__DOT___shout_r_T_7) 
                                                    << 8U)) 
                                                | (0xffU 
                                                   & (IData)(
                                                             (ALUexe__DOT__alu__DOT___shout_r_T_7 
                                                              >> 0x18U))))) 
                                            | (0xff00ff00U 
                                               & (((IData)(ALUexe__DOT__alu__DOT___shout_r_T_7) 
                                                   << 0x18U) 
                                                  | (0xffff00U 
                                                     & ((IData)(
                                                                (ALUexe__DOT__alu__DOT___shout_r_T_7 
                                                                 >> 0x10U)) 
                                                        << 8U)))));
    ALUexe__DOT__alu__DOT___shout_l_T_28 = ((0xf0f0f0fU 
                                             & (ALUexe__DOT__alu__DOT___shout_l_T_18 
                                                >> 4U)) 
                                            | (0xf0f0f0f0U 
                                               & (ALUexe__DOT__alu__DOT___shout_l_T_18 
                                                  << 4U)));
    ALUexe__DOT__alu__DOT___shout_l_T_38 = ((0x33333333U 
                                             & (ALUexe__DOT__alu__DOT___shout_l_T_28 
                                                >> 2U)) 
                                            | (0xccccccccU 
                                               & (ALUexe__DOT__alu__DOT___shout_l_T_28 
                                                  << 2U)));
    vlSelf->ALUexe__DOT__result__DOT__ram_wb_wxd_rd_MPORT_data 
        = ((8U == (0x1fU & (IData)(vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_alu_fn)))
            ? __Vcellinp__ALUexe__io_in_bits_in2 : 
           ((4U == (7U & ((IData)(vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_alu_fn) 
                          >> 2U))) ? ((0x11U == (0x1fU 
                                                 & (IData)(vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_alu_fn)))
                                       ? ((IData)(ALUexe__DOT__alu__DOT___mins_T)
                                           ? __Vcellinp__ALUexe__io_in_bits_in2
                                           : vlSelf->__Vcellinp__ALUexe__io_in_bits_in1)
                                       : ((0x10U == 
                                           (0x1fU & (IData)(vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_alu_fn)))
                                           ? ((IData)(ALUexe__DOT__alu__DOT___mins_T)
                                               ? vlSelf->__Vcellinp__ALUexe__io_in_bits_in1
                                               : __Vcellinp__ALUexe__io_in_bits_in2)
                                           : ((0x13U 
                                               == (0x1fU 
                                                   & (IData)(vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_alu_fn)))
                                               ? ((IData)(ALUexe__DOT__alu__DOT___minu_T)
                                                   ? __Vcellinp__ALUexe__io_in_bits_in2
                                                   : vlSelf->__Vcellinp__ALUexe__io_in_bits_in1)
                                               : ((IData)(ALUexe__DOT__alu__DOT___minu_T)
                                                   ? vlSelf->__Vcellinp__ALUexe__io_in_bits_in1
                                                   : __Vcellinp__ALUexe__io_in_bits_in2))))
             : (((0U == (0x1fU & (IData)(vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_alu_fn))) 
                 | (0xaU == (0x1fU & (IData)(vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_alu_fn))))
                 ? ALUexe__DOT__alu__DOT__adder_out
                 : (((0xcU <= (0x1fU & (IData)(vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_alu_fn))) 
                     & ((0xfU >= (0x1fU & (IData)(vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_alu_fn))) 
                        & (IData)(vlSelf->ALUexe__DOT__alu__DOT__slt))) 
                    | (((4U == (0x1fU & (IData)(vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_alu_fn)))
                         ? (vlSelf->__Vcellinp__ALUexe__io_in_bits_in1 
                            ^ __Vcellinp__ALUexe__io_in_bits_in2)
                         : ((6U == (0x1fU & (IData)(vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_alu_fn)))
                             ? (vlSelf->__Vcellinp__ALUexe__io_in_bits_in1 
                                | __Vcellinp__ALUexe__io_in_bits_in2)
                             : ((7U == (0x1fU & (IData)(vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_alu_fn)))
                                 ? (vlSelf->__Vcellinp__ALUexe__io_in_bits_in1 
                                    & __Vcellinp__ALUexe__io_in_bits_in2)
                                 : 0U))) | (((IData)(ALUexe__DOT__alu__DOT___shin_T_2)
                                              ? (IData)(ALUexe__DOT__alu__DOT___shout_r_T_7)
                                              : 0U) 
                                            | ((1U 
                                                == 
                                                (0x1fU 
                                                 & (IData)(vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_alu_fn)))
                                                ? (
                                                   (0x55555555U 
                                                    & (ALUexe__DOT__alu__DOT___shout_l_T_38 
                                                       >> 1U)) 
                                                   | (0xaaaaaaaaU 
                                                      & (ALUexe__DOT__alu__DOT___shout_l_T_38 
                                                         << 1U)))
                                                : 0U)))))));
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

VL_INLINE_OPT void VALUexe___024root___nba_sequent__TOP__0(VALUexe___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VALUexe__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root___nba_sequent__TOP__0\n"); );
    // Init
    IData/*31:0*/ __Vdlyvval__ALUexe__DOT__result__DOT__ram_wb_wxd_rd__v0;
    CData/*0:0*/ __Vdlyvset__ALUexe__DOT__result__DOT__ram_wb_wxd_rd__v0;
    CData/*0:0*/ __Vdlyvval__ALUexe__DOT__result__DOT__ram_wxd__v0;
    CData/*0:0*/ __Vdlyvset__ALUexe__DOT__result__DOT__ram_wxd__v0;
    CData/*4:0*/ __Vdlyvval__ALUexe__DOT__result__DOT__ram_reg_idxw__v0;
    CData/*0:0*/ __Vdlyvset__ALUexe__DOT__result__DOT__ram_reg_idxw__v0;
    CData/*1:0*/ __Vdlyvval__ALUexe__DOT__result__DOT__ram_warp_id__v0;
    CData/*0:0*/ __Vdlyvset__ALUexe__DOT__result__DOT__ram_warp_id__v0;
    CData/*1:0*/ __Vdlyvval__ALUexe__DOT__result_br__DOT__ram_wid__v0;
    CData/*0:0*/ __Vdlyvset__ALUexe__DOT__result_br__DOT__ram_wid__v0;
    CData/*0:0*/ __Vdlyvval__ALUexe__DOT__result_br__DOT__ram_jump__v0;
    CData/*0:0*/ __Vdlyvset__ALUexe__DOT__result_br__DOT__ram_jump__v0;
    IData/*31:0*/ __Vdlyvval__ALUexe__DOT__result_br__DOT__ram_new_pc__v0;
    CData/*0:0*/ __Vdlyvset__ALUexe__DOT__result_br__DOT__ram_new_pc__v0;
    // Body
    __Vdlyvset__ALUexe__DOT__result_br__DOT__ram_new_pc__v0 = 0U;
    __Vdlyvset__ALUexe__DOT__result_br__DOT__ram_wid__v0 = 0U;
    __Vdlyvset__ALUexe__DOT__result__DOT__ram_reg_idxw__v0 = 0U;
    __Vdlyvset__ALUexe__DOT__result__DOT__ram_warp_id__v0 = 0U;
    __Vdlyvset__ALUexe__DOT__result__DOT__ram_wxd__v0 = 0U;
    __Vdlyvset__ALUexe__DOT__result__DOT__ram_wb_wxd_rd__v0 = 0U;
    __Vdlyvset__ALUexe__DOT__result_br__DOT__ram_jump__v0 = 0U;
    if (vlSelf->ALUexe__DOT__result_br__DOT__ram_wid_MPORT_en) {
        __Vdlyvval__ALUexe__DOT__result_br__DOT__ram_new_pc__v0 
            = vlSelf->__Vcellinp__ALUexe__io_in_bits_in3;
        __Vdlyvset__ALUexe__DOT__result_br__DOT__ram_new_pc__v0 = 1U;
        __Vdlyvval__ALUexe__DOT__result_br__DOT__ram_wid__v0 
            = vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_wid;
        __Vdlyvset__ALUexe__DOT__result_br__DOT__ram_wid__v0 = 1U;
        __Vdlyvval__ALUexe__DOT__result_br__DOT__ram_jump__v0 
            = ((3U == (IData)(vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_branch)) 
               | ((2U == (IData)(vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_branch)) 
                  | ((1U == (IData)(vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_branch)) 
                     & ((IData)(vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_alu_fn) 
                        ^ ((8U & (IData)(vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_alu_fn))
                            ? (IData)(vlSelf->ALUexe__DOT__alu__DOT__slt)
                            : (0U == (vlSelf->__Vcellinp__ALUexe__io_in_bits_in1 
                                      ^ vlSelf->ALUexe__DOT__alu__DOT__in2_inv)))))));
        __Vdlyvset__ALUexe__DOT__result_br__DOT__ram_jump__v0 = 1U;
    }
    if (vlSelf->ALUexe__DOT__result__DOT__ram_wb_wxd_rd_MPORT_en) {
        __Vdlyvval__ALUexe__DOT__result__DOT__ram_reg_idxw__v0 
            = vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_reg_idxw;
        __Vdlyvset__ALUexe__DOT__result__DOT__ram_reg_idxw__v0 = 1U;
        __Vdlyvval__ALUexe__DOT__result__DOT__ram_warp_id__v0 
            = vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_wid;
        __Vdlyvset__ALUexe__DOT__result__DOT__ram_warp_id__v0 = 1U;
        __Vdlyvval__ALUexe__DOT__result__DOT__ram_wxd__v0 
            = vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_wxd;
        __Vdlyvset__ALUexe__DOT__result__DOT__ram_wxd__v0 = 1U;
        __Vdlyvval__ALUexe__DOT__result__DOT__ram_wb_wxd_rd__v0 
            = vlSelf->ALUexe__DOT__result__DOT__ram_wb_wxd_rd_MPORT_data;
        __Vdlyvset__ALUexe__DOT__result__DOT__ram_wb_wxd_rd__v0 = 1U;
    }
    if (vlSelf->__Vcellinp__ALUexe__reset) {
        vlSelf->ALUexe__DOT__result_br__DOT__maybe_full = 0U;
        vlSelf->ALUexe__DOT__result__DOT__maybe_full = 0U;
    } else {
        if (((IData)(vlSelf->ALUexe__DOT__result_br__DOT__ram_wid_MPORT_en) 
             != (IData)(vlSelf->ALUexe__DOT__result_br__DOT__do_deq))) {
            vlSelf->ALUexe__DOT__result_br__DOT__maybe_full 
                = vlSelf->ALUexe__DOT__result_br__DOT__ram_wid_MPORT_en;
        }
        if (((IData)(vlSelf->ALUexe__DOT__result__DOT__ram_wb_wxd_rd_MPORT_en) 
             != (IData)(vlSelf->ALUexe__DOT__result__DOT__do_deq))) {
            vlSelf->ALUexe__DOT__result__DOT__maybe_full 
                = vlSelf->ALUexe__DOT__result__DOT__ram_wb_wxd_rd_MPORT_en;
        }
    }
    if (__Vdlyvset__ALUexe__DOT__result_br__DOT__ram_new_pc__v0) {
        vlSelf->ALUexe__DOT__result_br__DOT__ram_new_pc[0U] 
            = __Vdlyvval__ALUexe__DOT__result_br__DOT__ram_new_pc__v0;
    }
    if (__Vdlyvset__ALUexe__DOT__result_br__DOT__ram_wid__v0) {
        vlSelf->ALUexe__DOT__result_br__DOT__ram_wid[0U] 
            = __Vdlyvval__ALUexe__DOT__result_br__DOT__ram_wid__v0;
    }
    if (__Vdlyvset__ALUexe__DOT__result__DOT__ram_reg_idxw__v0) {
        vlSelf->ALUexe__DOT__result__DOT__ram_reg_idxw[0U] 
            = __Vdlyvval__ALUexe__DOT__result__DOT__ram_reg_idxw__v0;
    }
    if (__Vdlyvset__ALUexe__DOT__result__DOT__ram_warp_id__v0) {
        vlSelf->ALUexe__DOT__result__DOT__ram_warp_id[0U] 
            = __Vdlyvval__ALUexe__DOT__result__DOT__ram_warp_id__v0;
    }
    if (__Vdlyvset__ALUexe__DOT__result__DOT__ram_wxd__v0) {
        vlSelf->ALUexe__DOT__result__DOT__ram_wxd[0U] 
            = __Vdlyvval__ALUexe__DOT__result__DOT__ram_wxd__v0;
    }
    if (__Vdlyvset__ALUexe__DOT__result__DOT__ram_wb_wxd_rd__v0) {
        vlSelf->ALUexe__DOT__result__DOT__ram_wb_wxd_rd[0U] 
            = __Vdlyvval__ALUexe__DOT__result__DOT__ram_wb_wxd_rd__v0;
    }
    if (__Vdlyvset__ALUexe__DOT__result_br__DOT__ram_jump__v0) {
        vlSelf->ALUexe__DOT__result_br__DOT__ram_jump[0U] 
            = __Vdlyvval__ALUexe__DOT__result_br__DOT__ram_jump__v0;
    }
    VL_ASSIGN_SII(32,vlSelf->io_out2br_bits_new_pc, 
                  vlSelf->ALUexe__DOT__result_br__DOT__ram_new_pc
                  [0U]);
    VL_ASSIGN_SII(2,vlSelf->io_out2br_bits_wid, vlSelf->ALUexe__DOT__result_br__DOT__ram_wid
                  [0U]);
    VL_ASSIGN_SII(5,vlSelf->io_out_bits_reg_idxw, vlSelf->ALUexe__DOT__result__DOT__ram_reg_idxw
                  [0U]);
    VL_ASSIGN_SII(2,vlSelf->io_out_bits_warp_id, vlSelf->ALUexe__DOT__result__DOT__ram_warp_id
                  [0U]);
    VL_ASSIGN_SII(1,vlSelf->io_out_bits_wxd, vlSelf->ALUexe__DOT__result__DOT__ram_wxd
                  [0U]);
    VL_ASSIGN_SII(32,vlSelf->io_out_bits_wb_wxd_rd, 
                  vlSelf->ALUexe__DOT__result__DOT__ram_wb_wxd_rd
                  [0U]);
    VL_ASSIGN_SII(1,vlSelf->io_out2br_bits_jump, vlSelf->ALUexe__DOT__result_br__DOT__ram_jump
                  [0U]);
    VL_ASSIGN_SII(1,vlSelf->io_out2br_valid, vlSelf->ALUexe__DOT__result_br__DOT__maybe_full);
    VL_ASSIGN_SII(1,vlSelf->io_out_valid, vlSelf->ALUexe__DOT__result__DOT__maybe_full);
}

VL_INLINE_OPT void VALUexe___024root___nba_sequent__TOP__1(VALUexe___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VALUexe__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root___nba_sequent__TOP__1\n"); );
    // Init
    CData/*0:0*/ ALUexe__DOT__result_io_enq_ready;
    CData/*0:0*/ ALUexe__DOT__result_br_io_enq_ready;
    // Body
    vlSelf->ALUexe__DOT__result__DOT__do_deq = ((IData)(vlSelf->ALUexe__DOT__result__DOT__maybe_full) 
                                                & (IData)(vlSelf->__Vcellinp__ALUexe__io_out_ready));
    ALUexe__DOT__result_io_enq_ready = (1U & ((~ (IData)(vlSelf->ALUexe__DOT__result__DOT__maybe_full)) 
                                              | (IData)(vlSelf->__Vcellinp__ALUexe__io_out_ready)));
    vlSelf->ALUexe__DOT__result_br__DOT__do_deq = ((IData)(vlSelf->ALUexe__DOT__result_br__DOT__maybe_full) 
                                                   & (IData)(vlSelf->__Vcellinp__ALUexe__io_out2br_ready));
    ALUexe__DOT__result_br_io_enq_ready = (1U & ((~ (IData)(vlSelf->ALUexe__DOT__result_br__DOT__maybe_full)) 
                                                 | (IData)(vlSelf->__Vcellinp__ALUexe__io_out2br_ready)));
    vlSelf->ALUexe__DOT__result__DOT__ram_wb_wxd_rd_MPORT_en 
        = ((IData)(ALUexe__DOT__result_io_enq_ready) 
           & ((IData)(vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_wxd) 
              & (IData)(vlSelf->__Vcellinp__ALUexe__io_in_valid)));
    VL_ASSIGN_SII(1,vlSelf->io_in_ready, ((0U == (IData)(vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_branch))
                                           ? (IData)(ALUexe__DOT__result_io_enq_ready)
                                           : ((1U == (IData)(vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_branch))
                                               ? (IData)(ALUexe__DOT__result_br_io_enq_ready)
                                               : ((IData)(ALUexe__DOT__result_br_io_enq_ready) 
                                                  & (IData)(ALUexe__DOT__result_io_enq_ready)))));
    vlSelf->ALUexe__DOT__result_br__DOT__ram_wid_MPORT_en 
        = ((IData)(ALUexe__DOT__result_br_io_enq_ready) 
           & ((IData)(vlSelf->__Vcellinp__ALUexe__io_in_valid) 
              & (0U != (IData)(vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_branch))));
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
                VL_FATAL_MT("generated/ALUexe.v", 429, "", "Input combinational region did not converge.");
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
                    VL_FATAL_MT("generated/ALUexe.v", 429, "", "Active region did not converge.");
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
                VL_FATAL_MT("generated/ALUexe.v", 429, "", "NBA region did not converge.");
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

// Verilated -*- SystemC -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See VALUexe.h for the primary calling header

#include "verilated.h"

#include "VALUexe___024root.h"

VL_ATTR_COLD void VALUexe___024root___eval_static(VALUexe___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VALUexe__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root___eval_static\n"); );
}

VL_ATTR_COLD void VALUexe___024root___eval_initial(VALUexe___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VALUexe__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root___eval_initial\n"); );
    // Body
    vlSelf->__Vtrigrprev__TOP____Vcellinp__ALUexe__clock 
        = vlSelf->__Vcellinp__ALUexe__clock;
}

VL_ATTR_COLD void VALUexe___024root___eval_final(VALUexe___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VALUexe__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root___eval_final\n"); );
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
                VL_FATAL_MT("generated/ALUexe.v", 429, "", "Settle region did not converge.");
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

VL_ATTR_COLD void VALUexe___024root___stl_sequent__TOP__0(VALUexe___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VALUexe__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root___stl_sequent__TOP__0\n"); );
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
    VL_ASSIGN_SII(1,vlSelf->io_out_valid, vlSelf->ALUexe__DOT__result__DOT__maybe_full);
    VL_ASSIGN_SII(1,vlSelf->io_out2br_valid, vlSelf->ALUexe__DOT__result_br__DOT__maybe_full);
    VL_ASSIGN_ISI(32,vlSelf->__Vcellinp__ALUexe__io_in_bits_in3, vlSelf->io_in_bits_in3);
    VL_ASSIGN_ISI(2,vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_wid, vlSelf->io_in_bits_ctrl_wid);
    VL_ASSIGN_ISI(5,vlSelf->__Vcellinp__ALUexe__io_in_bits_ctrl_reg_idxw, vlSelf->io_in_bits_ctrl_reg_idxw);
    VL_ASSIGN_SII(32,vlSelf->io_out_bits_wb_wxd_rd, 
                  vlSelf->ALUexe__DOT__result__DOT__ram_wb_wxd_rd
                  [0U]);
    VL_ASSIGN_SII(1,vlSelf->io_out_bits_wxd, vlSelf->ALUexe__DOT__result__DOT__ram_wxd
                  [0U]);
    VL_ASSIGN_SII(5,vlSelf->io_out_bits_reg_idxw, vlSelf->ALUexe__DOT__result__DOT__ram_reg_idxw
                  [0U]);
    VL_ASSIGN_SII(2,vlSelf->io_out_bits_warp_id, vlSelf->ALUexe__DOT__result__DOT__ram_warp_id
                  [0U]);
    VL_ASSIGN_SII(2,vlSelf->io_out2br_bits_wid, vlSelf->ALUexe__DOT__result_br__DOT__ram_wid
                  [0U]);
    VL_ASSIGN_SII(1,vlSelf->io_out2br_bits_jump, vlSelf->ALUexe__DOT__result_br__DOT__ram_jump
                  [0U]);
    VL_ASSIGN_SII(32,vlSelf->io_out2br_bits_new_pc, 
                  vlSelf->ALUexe__DOT__result_br__DOT__ram_new_pc
                  [0U]);
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

VL_ATTR_COLD void VALUexe___024root___eval_stl(VALUexe___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VALUexe__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root___eval_stl\n"); );
    // Body
    if (vlSelf->__VstlTriggered.at(0U)) {
        VALUexe___024root___stl_sequent__TOP__0(vlSelf);
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
        VL_DBG_MSGF("         'act' region trigger index 0 is active: @(posedge __Vcellinp__ALUexe__clock)\n");
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
        VL_DBG_MSGF("         'nba' region trigger index 0 is active: @(posedge __Vcellinp__ALUexe__clock)\n");
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
    vlSelf->__Vcellinp__ALUexe__io_in_bits_in1 = VL_RAND_RESET_I(32);
    vlSelf->__Vcellinp__ALUexe__io_in_valid = VL_RAND_RESET_I(1);
    vlSelf->__Vcellinp__ALUexe__reset = VL_RAND_RESET_I(1);
    vlSelf->__Vcellinp__ALUexe__clock = VL_RAND_RESET_I(1);
    vlSelf->ALUexe__DOT__alu__DOT__in2_inv = VL_RAND_RESET_I(32);
    vlSelf->ALUexe__DOT__alu__DOT__slt = VL_RAND_RESET_I(1);
    for (int __Vi0 = 0; __Vi0 < 1; ++__Vi0) {
        vlSelf->ALUexe__DOT__result__DOT__ram_wb_wxd_rd[__Vi0] = VL_RAND_RESET_I(32);
    }
    vlSelf->ALUexe__DOT__result__DOT__ram_wb_wxd_rd_MPORT_data = VL_RAND_RESET_I(32);
    vlSelf->ALUexe__DOT__result__DOT__ram_wb_wxd_rd_MPORT_en = VL_RAND_RESET_I(1);
    for (int __Vi0 = 0; __Vi0 < 1; ++__Vi0) {
        vlSelf->ALUexe__DOT__result__DOT__ram_wxd[__Vi0] = VL_RAND_RESET_I(1);
    }
    for (int __Vi0 = 0; __Vi0 < 1; ++__Vi0) {
        vlSelf->ALUexe__DOT__result__DOT__ram_reg_idxw[__Vi0] = VL_RAND_RESET_I(5);
    }
    for (int __Vi0 = 0; __Vi0 < 1; ++__Vi0) {
        vlSelf->ALUexe__DOT__result__DOT__ram_warp_id[__Vi0] = VL_RAND_RESET_I(2);
    }
    vlSelf->ALUexe__DOT__result__DOT__maybe_full = VL_RAND_RESET_I(1);
    vlSelf->ALUexe__DOT__result__DOT__do_deq = VL_RAND_RESET_I(1);
    for (int __Vi0 = 0; __Vi0 < 1; ++__Vi0) {
        vlSelf->ALUexe__DOT__result_br__DOT__ram_wid[__Vi0] = VL_RAND_RESET_I(2);
    }
    vlSelf->ALUexe__DOT__result_br__DOT__ram_wid_MPORT_en = VL_RAND_RESET_I(1);
    for (int __Vi0 = 0; __Vi0 < 1; ++__Vi0) {
        vlSelf->ALUexe__DOT__result_br__DOT__ram_jump[__Vi0] = VL_RAND_RESET_I(1);
    }
    for (int __Vi0 = 0; __Vi0 < 1; ++__Vi0) {
        vlSelf->ALUexe__DOT__result_br__DOT__ram_new_pc[__Vi0] = VL_RAND_RESET_I(32);
    }
    vlSelf->ALUexe__DOT__result_br__DOT__maybe_full = VL_RAND_RESET_I(1);
    vlSelf->ALUexe__DOT__result_br__DOT__do_deq = VL_RAND_RESET_I(1);
    vlSelf->__VstlIterCount = 0;
    vlSelf->__VicoIterCount = 0;
    vlSelf->__Vtrigrprev__TOP____Vcellinp__ALUexe__clock = VL_RAND_RESET_I(1);
    vlSelf->__VactIterCount = 0;
    vlSelf->__VactContinue = 0;
}

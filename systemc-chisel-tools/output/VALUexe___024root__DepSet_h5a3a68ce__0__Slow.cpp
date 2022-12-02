// Verilated -*- SystemC -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See VALUexe.h for the primary calling header

#include "verilated.h"
#include "verilated_dpi.h"

#include "VALUexe__Syms.h"
#include "VALUexe___024root.h"

void VALUexe___024root____Vdpiimwrap_ALUexe__DOT__alu__DOT__ScalarALU_protectlib_check_hash_TOP(IData/*31:0*/ protectlib_hash___05FV);
void VALUexe___024root____Vdpiimwrap_ALUexe__DOT__alu__DOT__ScalarALU_protectlib_create_TOP(std::string scope___05FV, QData/*63:0*/ &ScalarALU_protectlib_create__Vfuncrtn);
void VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result__DOT__Queue_protectlib_check_hash_TOP(IData/*31:0*/ protectlib_hash___05FV);
void VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result__DOT__Queue_protectlib_create_TOP(std::string scope___05FV, QData/*63:0*/ &Queue_protectlib_create__Vfuncrtn);
void VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_check_hash_TOP(IData/*31:0*/ protectlib_hash___05FV);
void VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_create_TOP(std::string scope___05FV, QData/*63:0*/ &Queue_1_protectlib_create__Vfuncrtn);

VL_ATTR_COLD void VALUexe___024root___eval_initial__TOP(VALUexe___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VALUexe__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root___eval_initial__TOP\n"); );
    // Init
    QData/*63:0*/ __Vfunc_ALUexe__DOT__alu__DOT__ScalarALU_protectlib_create__1__Vfuncout;
    QData/*63:0*/ __Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_create__5__Vfuncout;
    QData/*63:0*/ __Vfunc_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_create__11__Vfuncout;
    // Body
    VALUexe___024root____Vdpiimwrap_ALUexe__DOT__alu__DOT__ScalarALU_protectlib_check_hash_TOP(0xe9a4f7a9U);
    VALUexe___024root____Vdpiimwrap_ALUexe__DOT__alu__DOT__ScalarALU_protectlib_create_TOP(VL_SFORMATF_NX("%NALUexe.alu",
                                                                                vlSymsp->name()) , __Vfunc_ALUexe__DOT__alu__DOT__ScalarALU_protectlib_create__1__Vfuncout);
    vlSelf->ALUexe__DOT__alu__DOT__handle___05FV = __Vfunc_ALUexe__DOT__alu__DOT__ScalarALU_protectlib_create__1__Vfuncout;
    VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result__DOT__Queue_protectlib_check_hash_TOP(0xf58671daU);
    VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result__DOT__Queue_protectlib_create_TOP(VL_SFORMATF_NX("%NALUexe.result",
                                                                                vlSymsp->name()) , __Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_create__5__Vfuncout);
    vlSelf->ALUexe__DOT__result__DOT__handle___05FV 
        = __Vfunc_ALUexe__DOT__result__DOT__Queue_protectlib_create__5__Vfuncout;
    VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_check_hash_TOP(0x7a976d7cU);
    VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_create_TOP(VL_SFORMATF_NX("%NALUexe.result_br",
                                                                                vlSymsp->name()) , __Vfunc_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_create__11__Vfuncout);
    vlSelf->ALUexe__DOT__result_br__DOT__handle___05FV 
        = __Vfunc_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_create__11__Vfuncout;
}

#ifdef VL_DEBUG
VL_ATTR_COLD void VALUexe___024root___dump_triggers__stl(VALUexe___024root* vlSelf);
#endif  // VL_DEBUG

VL_ATTR_COLD void VALUexe___024root___eval_triggers__stl(VALUexe___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VALUexe__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root___eval_triggers__stl\n"); );
    // Body
    vlSelf->__VstlTriggered.at(0U) = (0U == vlSelf->__VstlIterCount);
#ifdef VL_DEBUG
    if (VL_UNLIKELY(vlSymsp->_vm_contextp__->debug())) {
        VALUexe___024root___dump_triggers__stl(vlSelf);
    }
#endif
}

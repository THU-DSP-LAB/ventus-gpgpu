// Verilated -*- SystemC -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See VALUexe.h for the primary calling header

#include "verilated.h"
#include "verilated_dpi.h"

#include "VALUexe__Syms.h"
#include "VALUexe___024root.h"

extern "C" void ScalarALU_protectlib_check_hash(int protectlib_hash___05FV);

VL_INLINE_OPT void VALUexe___024root____Vdpiimwrap_ALUexe__DOT__alu__DOT__ScalarALU_protectlib_check_hash_TOP(IData/*31:0*/ protectlib_hash___05FV) {
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root____Vdpiimwrap_ALUexe__DOT__alu__DOT__ScalarALU_protectlib_check_hash_TOP\n"); );
    // Body
    int protectlib_hash___05FV__Vcvt;
    for (size_t protectlib_hash___05FV__Vidx = 0; protectlib_hash___05FV__Vidx < 1; ++protectlib_hash___05FV__Vidx) protectlib_hash___05FV__Vcvt = protectlib_hash___05FV;
    ScalarALU_protectlib_check_hash(protectlib_hash___05FV__Vcvt);
}

extern "C" void* ScalarALU_protectlib_create(const char* scope___05FV);

VL_INLINE_OPT void VALUexe___024root____Vdpiimwrap_ALUexe__DOT__alu__DOT__ScalarALU_protectlib_create_TOP(std::string scope___05FV, QData/*63:0*/ &ScalarALU_protectlib_create__Vfuncrtn) {
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root____Vdpiimwrap_ALUexe__DOT__alu__DOT__ScalarALU_protectlib_create_TOP\n"); );
    // Body
    const char* scope___05FV__Vcvt;
    for (size_t scope___05FV__Vidx = 0; scope___05FV__Vidx < 1; ++scope___05FV__Vidx) scope___05FV__Vcvt = scope___05FV.c_str();
    void* ScalarALU_protectlib_create__Vfuncrtn__Vcvt;
    ScalarALU_protectlib_create__Vfuncrtn__Vcvt = ScalarALU_protectlib_create(scope___05FV__Vcvt);
    ScalarALU_protectlib_create__Vfuncrtn = VL_CVT_VP_Q(ScalarALU_protectlib_create__Vfuncrtn__Vcvt);
}

extern "C" long long ScalarALU_protectlib_combo_update(void* handle___05FV, const svLogicVecVal* io_func, const svLogicVecVal* io_in2, const svLogicVecVal* io_in1, svLogicVecVal* io_out, svLogic* io_cmp_out);

VL_INLINE_OPT void VALUexe___024root____Vdpiimwrap_ALUexe__DOT__alu__DOT__ScalarALU_protectlib_combo_update_TOP(QData/*63:0*/ handle___05FV, CData/*4:0*/ io_func, IData/*31:0*/ io_in2, IData/*31:0*/ io_in1, IData/*31:0*/ &io_out, CData/*0:0*/ &io_cmp_out, QData/*63:0*/ &ScalarALU_protectlib_combo_update__Vfuncrtn) {
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root____Vdpiimwrap_ALUexe__DOT__alu__DOT__ScalarALU_protectlib_combo_update_TOP\n"); );
    // Body
    void* handle___05FV__Vcvt;
    for (size_t handle___05FV__Vidx = 0; handle___05FV__Vidx < 1; ++handle___05FV__Vidx) handle___05FV__Vcvt = VL_CVT_Q_VP(handle___05FV);
    svLogicVecVal io_func__Vcvt[1];
    for (size_t io_func__Vidx = 0; io_func__Vidx < 1; ++io_func__Vidx) VL_SET_SVLV_I(5, io_func__Vcvt + 1 * io_func__Vidx, io_func);
    svLogicVecVal io_in2__Vcvt[1];
    for (size_t io_in2__Vidx = 0; io_in2__Vidx < 1; ++io_in2__Vidx) VL_SET_SVLV_I(32, io_in2__Vcvt + 1 * io_in2__Vidx, io_in2);
    svLogicVecVal io_in1__Vcvt[1];
    for (size_t io_in1__Vidx = 0; io_in1__Vidx < 1; ++io_in1__Vidx) VL_SET_SVLV_I(32, io_in1__Vcvt + 1 * io_in1__Vidx, io_in1);
    svLogicVecVal io_out__Vcvt[1];
    svLogic io_cmp_out__Vcvt;
    long long ScalarALU_protectlib_combo_update__Vfuncrtn__Vcvt;
    ScalarALU_protectlib_combo_update__Vfuncrtn__Vcvt = ScalarALU_protectlib_combo_update(handle___05FV__Vcvt, io_func__Vcvt, io_in2__Vcvt, io_in1__Vcvt, io_out__Vcvt, &io_cmp_out__Vcvt);
    io_out = VL_SET_I_SVLV(io_out__Vcvt);
    io_cmp_out = (1U & io_cmp_out__Vcvt);
    ScalarALU_protectlib_combo_update__Vfuncrtn = ScalarALU_protectlib_combo_update__Vfuncrtn__Vcvt;
}

extern "C" void ScalarALU_protectlib_combo_ignore(void* handle___05FV, const svLogicVecVal* io_func, const svLogicVecVal* io_in2, const svLogicVecVal* io_in1);

VL_INLINE_OPT void VALUexe___024root____Vdpiimwrap_ALUexe__DOT__alu__DOT__ScalarALU_protectlib_combo_ignore_TOP(QData/*63:0*/ handle___05FV, CData/*4:0*/ io_func, IData/*31:0*/ io_in2, IData/*31:0*/ io_in1) {
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root____Vdpiimwrap_ALUexe__DOT__alu__DOT__ScalarALU_protectlib_combo_ignore_TOP\n"); );
    // Body
    void* handle___05FV__Vcvt;
    for (size_t handle___05FV__Vidx = 0; handle___05FV__Vidx < 1; ++handle___05FV__Vidx) handle___05FV__Vcvt = VL_CVT_Q_VP(handle___05FV);
    svLogicVecVal io_func__Vcvt[1];
    for (size_t io_func__Vidx = 0; io_func__Vidx < 1; ++io_func__Vidx) VL_SET_SVLV_I(5, io_func__Vcvt + 1 * io_func__Vidx, io_func);
    svLogicVecVal io_in2__Vcvt[1];
    for (size_t io_in2__Vidx = 0; io_in2__Vidx < 1; ++io_in2__Vidx) VL_SET_SVLV_I(32, io_in2__Vcvt + 1 * io_in2__Vidx, io_in2);
    svLogicVecVal io_in1__Vcvt[1];
    for (size_t io_in1__Vidx = 0; io_in1__Vidx < 1; ++io_in1__Vidx) VL_SET_SVLV_I(32, io_in1__Vcvt + 1 * io_in1__Vidx, io_in1);
    ScalarALU_protectlib_combo_ignore(handle___05FV__Vcvt, io_func__Vcvt, io_in2__Vcvt, io_in1__Vcvt);
}

extern "C" void ScalarALU_protectlib_final(void* handle___05FV);

VL_INLINE_OPT void VALUexe___024root____Vdpiimwrap_ALUexe__DOT__alu__DOT__ScalarALU_protectlib_final_TOP(QData/*63:0*/ handle___05FV) {
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root____Vdpiimwrap_ALUexe__DOT__alu__DOT__ScalarALU_protectlib_final_TOP\n"); );
    // Body
    void* handle___05FV__Vcvt;
    for (size_t handle___05FV__Vidx = 0; handle___05FV__Vidx < 1; ++handle___05FV__Vidx) handle___05FV__Vcvt = VL_CVT_Q_VP(handle___05FV);
    ScalarALU_protectlib_final(handle___05FV__Vcvt);
}

extern "C" void Queue_protectlib_check_hash(int protectlib_hash___05FV);

VL_INLINE_OPT void VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result__DOT__Queue_protectlib_check_hash_TOP(IData/*31:0*/ protectlib_hash___05FV) {
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result__DOT__Queue_protectlib_check_hash_TOP\n"); );
    // Body
    int protectlib_hash___05FV__Vcvt;
    for (size_t protectlib_hash___05FV__Vidx = 0; protectlib_hash___05FV__Vidx < 1; ++protectlib_hash___05FV__Vidx) protectlib_hash___05FV__Vcvt = protectlib_hash___05FV;
    Queue_protectlib_check_hash(protectlib_hash___05FV__Vcvt);
}

extern "C" void* Queue_protectlib_create(const char* scope___05FV);

VL_INLINE_OPT void VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result__DOT__Queue_protectlib_create_TOP(std::string scope___05FV, QData/*63:0*/ &Queue_protectlib_create__Vfuncrtn) {
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result__DOT__Queue_protectlib_create_TOP\n"); );
    // Body
    const char* scope___05FV__Vcvt;
    for (size_t scope___05FV__Vidx = 0; scope___05FV__Vidx < 1; ++scope___05FV__Vidx) scope___05FV__Vcvt = scope___05FV.c_str();
    void* Queue_protectlib_create__Vfuncrtn__Vcvt;
    Queue_protectlib_create__Vfuncrtn__Vcvt = Queue_protectlib_create(scope___05FV__Vcvt);
    Queue_protectlib_create__Vfuncrtn = VL_CVT_VP_Q(Queue_protectlib_create__Vfuncrtn__Vcvt);
}

extern "C" long long Queue_protectlib_combo_update(void* handle___05FV, svLogic reset, svLogic* io_enq_ready, svLogic io_enq_valid, const svLogicVecVal* io_enq_bits_wb_wxd_rd, svLogic io_enq_bits_wxd, const svLogicVecVal* io_enq_bits_reg_idxw, const svLogicVecVal* io_enq_bits_warp_id, svLogic io_deq_ready, svLogic* io_deq_valid, svLogicVecVal* io_deq_bits_wb_wxd_rd, svLogic* io_deq_bits_wxd, svLogicVecVal* io_deq_bits_reg_idxw, svLogicVecVal* io_deq_bits_warp_id);

VL_INLINE_OPT void VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result__DOT__Queue_protectlib_combo_update_TOP(QData/*63:0*/ handle___05FV, CData/*0:0*/ reset, CData/*0:0*/ &io_enq_ready, CData/*0:0*/ io_enq_valid, IData/*31:0*/ io_enq_bits_wb_wxd_rd, CData/*0:0*/ io_enq_bits_wxd, CData/*4:0*/ io_enq_bits_reg_idxw, CData/*1:0*/ io_enq_bits_warp_id, CData/*0:0*/ io_deq_ready, CData/*0:0*/ &io_deq_valid, IData/*31:0*/ &io_deq_bits_wb_wxd_rd, CData/*0:0*/ &io_deq_bits_wxd, CData/*4:0*/ &io_deq_bits_reg_idxw, CData/*1:0*/ &io_deq_bits_warp_id, QData/*63:0*/ &Queue_protectlib_combo_update__Vfuncrtn) {
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result__DOT__Queue_protectlib_combo_update_TOP\n"); );
    // Body
    void* handle___05FV__Vcvt;
    for (size_t handle___05FV__Vidx = 0; handle___05FV__Vidx < 1; ++handle___05FV__Vidx) handle___05FV__Vcvt = VL_CVT_Q_VP(handle___05FV);
    svLogic reset__Vcvt;
    for (size_t reset__Vidx = 0; reset__Vidx < 1; ++reset__Vidx) reset__Vcvt = reset;
    svLogic io_enq_ready__Vcvt;
    svLogic io_enq_valid__Vcvt;
    for (size_t io_enq_valid__Vidx = 0; io_enq_valid__Vidx < 1; ++io_enq_valid__Vidx) io_enq_valid__Vcvt = io_enq_valid;
    svLogicVecVal io_enq_bits_wb_wxd_rd__Vcvt[1];
    for (size_t io_enq_bits_wb_wxd_rd__Vidx = 0; io_enq_bits_wb_wxd_rd__Vidx < 1; ++io_enq_bits_wb_wxd_rd__Vidx) VL_SET_SVLV_I(32, io_enq_bits_wb_wxd_rd__Vcvt + 1 * io_enq_bits_wb_wxd_rd__Vidx, io_enq_bits_wb_wxd_rd);
    svLogic io_enq_bits_wxd__Vcvt;
    for (size_t io_enq_bits_wxd__Vidx = 0; io_enq_bits_wxd__Vidx < 1; ++io_enq_bits_wxd__Vidx) io_enq_bits_wxd__Vcvt = io_enq_bits_wxd;
    svLogicVecVal io_enq_bits_reg_idxw__Vcvt[1];
    for (size_t io_enq_bits_reg_idxw__Vidx = 0; io_enq_bits_reg_idxw__Vidx < 1; ++io_enq_bits_reg_idxw__Vidx) VL_SET_SVLV_I(5, io_enq_bits_reg_idxw__Vcvt + 1 * io_enq_bits_reg_idxw__Vidx, io_enq_bits_reg_idxw);
    svLogicVecVal io_enq_bits_warp_id__Vcvt[1];
    for (size_t io_enq_bits_warp_id__Vidx = 0; io_enq_bits_warp_id__Vidx < 1; ++io_enq_bits_warp_id__Vidx) VL_SET_SVLV_I(2, io_enq_bits_warp_id__Vcvt + 1 * io_enq_bits_warp_id__Vidx, io_enq_bits_warp_id);
    svLogic io_deq_ready__Vcvt;
    for (size_t io_deq_ready__Vidx = 0; io_deq_ready__Vidx < 1; ++io_deq_ready__Vidx) io_deq_ready__Vcvt = io_deq_ready;
    svLogic io_deq_valid__Vcvt;
    svLogicVecVal io_deq_bits_wb_wxd_rd__Vcvt[1];
    svLogic io_deq_bits_wxd__Vcvt;
    svLogicVecVal io_deq_bits_reg_idxw__Vcvt[1];
    svLogicVecVal io_deq_bits_warp_id__Vcvt[1];
    long long Queue_protectlib_combo_update__Vfuncrtn__Vcvt;
    Queue_protectlib_combo_update__Vfuncrtn__Vcvt = Queue_protectlib_combo_update(handle___05FV__Vcvt, reset__Vcvt, &io_enq_ready__Vcvt, io_enq_valid__Vcvt, io_enq_bits_wb_wxd_rd__Vcvt, io_enq_bits_wxd__Vcvt, io_enq_bits_reg_idxw__Vcvt, io_enq_bits_warp_id__Vcvt, io_deq_ready__Vcvt, &io_deq_valid__Vcvt, io_deq_bits_wb_wxd_rd__Vcvt, &io_deq_bits_wxd__Vcvt, io_deq_bits_reg_idxw__Vcvt, io_deq_bits_warp_id__Vcvt);
    io_enq_ready = (1U & io_enq_ready__Vcvt);
    io_deq_valid = (1U & io_deq_valid__Vcvt);
    io_deq_bits_wb_wxd_rd = VL_SET_I_SVLV(io_deq_bits_wb_wxd_rd__Vcvt);
    io_deq_bits_wxd = (1U & io_deq_bits_wxd__Vcvt);
    io_deq_bits_reg_idxw = (0x1fU & VL_SET_I_SVLV(io_deq_bits_reg_idxw__Vcvt));
    io_deq_bits_warp_id = (3U & VL_SET_I_SVLV(io_deq_bits_warp_id__Vcvt));
    Queue_protectlib_combo_update__Vfuncrtn = Queue_protectlib_combo_update__Vfuncrtn__Vcvt;
}

extern "C" long long Queue_protectlib_seq_update(void* handle___05FV, svLogic clock, svLogic* io_enq_ready, svLogic* io_deq_valid, svLogicVecVal* io_deq_bits_wb_wxd_rd, svLogic* io_deq_bits_wxd, svLogicVecVal* io_deq_bits_reg_idxw, svLogicVecVal* io_deq_bits_warp_id);

VL_INLINE_OPT void VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result__DOT__Queue_protectlib_seq_update_TOP(QData/*63:0*/ handle___05FV, CData/*0:0*/ clock, CData/*0:0*/ &io_enq_ready, CData/*0:0*/ &io_deq_valid, IData/*31:0*/ &io_deq_bits_wb_wxd_rd, CData/*0:0*/ &io_deq_bits_wxd, CData/*4:0*/ &io_deq_bits_reg_idxw, CData/*1:0*/ &io_deq_bits_warp_id, QData/*63:0*/ &Queue_protectlib_seq_update__Vfuncrtn) {
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result__DOT__Queue_protectlib_seq_update_TOP\n"); );
    // Body
    void* handle___05FV__Vcvt;
    for (size_t handle___05FV__Vidx = 0; handle___05FV__Vidx < 1; ++handle___05FV__Vidx) handle___05FV__Vcvt = VL_CVT_Q_VP(handle___05FV);
    svLogic clock__Vcvt;
    for (size_t clock__Vidx = 0; clock__Vidx < 1; ++clock__Vidx) clock__Vcvt = clock;
    svLogic io_enq_ready__Vcvt;
    svLogic io_deq_valid__Vcvt;
    svLogicVecVal io_deq_bits_wb_wxd_rd__Vcvt[1];
    svLogic io_deq_bits_wxd__Vcvt;
    svLogicVecVal io_deq_bits_reg_idxw__Vcvt[1];
    svLogicVecVal io_deq_bits_warp_id__Vcvt[1];
    long long Queue_protectlib_seq_update__Vfuncrtn__Vcvt;
    Queue_protectlib_seq_update__Vfuncrtn__Vcvt = Queue_protectlib_seq_update(handle___05FV__Vcvt, clock__Vcvt, &io_enq_ready__Vcvt, &io_deq_valid__Vcvt, io_deq_bits_wb_wxd_rd__Vcvt, &io_deq_bits_wxd__Vcvt, io_deq_bits_reg_idxw__Vcvt, io_deq_bits_warp_id__Vcvt);
    io_enq_ready = (1U & io_enq_ready__Vcvt);
    io_deq_valid = (1U & io_deq_valid__Vcvt);
    io_deq_bits_wb_wxd_rd = VL_SET_I_SVLV(io_deq_bits_wb_wxd_rd__Vcvt);
    io_deq_bits_wxd = (1U & io_deq_bits_wxd__Vcvt);
    io_deq_bits_reg_idxw = (0x1fU & VL_SET_I_SVLV(io_deq_bits_reg_idxw__Vcvt));
    io_deq_bits_warp_id = (3U & VL_SET_I_SVLV(io_deq_bits_warp_id__Vcvt));
    Queue_protectlib_seq_update__Vfuncrtn = Queue_protectlib_seq_update__Vfuncrtn__Vcvt;
}

extern "C" void Queue_protectlib_combo_ignore(void* handle___05FV, svLogic reset, svLogic io_enq_valid, const svLogicVecVal* io_enq_bits_wb_wxd_rd, svLogic io_enq_bits_wxd, const svLogicVecVal* io_enq_bits_reg_idxw, const svLogicVecVal* io_enq_bits_warp_id, svLogic io_deq_ready);

VL_INLINE_OPT void VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result__DOT__Queue_protectlib_combo_ignore_TOP(QData/*63:0*/ handle___05FV, CData/*0:0*/ reset, CData/*0:0*/ io_enq_valid, IData/*31:0*/ io_enq_bits_wb_wxd_rd, CData/*0:0*/ io_enq_bits_wxd, CData/*4:0*/ io_enq_bits_reg_idxw, CData/*1:0*/ io_enq_bits_warp_id, CData/*0:0*/ io_deq_ready) {
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result__DOT__Queue_protectlib_combo_ignore_TOP\n"); );
    // Body
    void* handle___05FV__Vcvt;
    for (size_t handle___05FV__Vidx = 0; handle___05FV__Vidx < 1; ++handle___05FV__Vidx) handle___05FV__Vcvt = VL_CVT_Q_VP(handle___05FV);
    svLogic reset__Vcvt;
    for (size_t reset__Vidx = 0; reset__Vidx < 1; ++reset__Vidx) reset__Vcvt = reset;
    svLogic io_enq_valid__Vcvt;
    for (size_t io_enq_valid__Vidx = 0; io_enq_valid__Vidx < 1; ++io_enq_valid__Vidx) io_enq_valid__Vcvt = io_enq_valid;
    svLogicVecVal io_enq_bits_wb_wxd_rd__Vcvt[1];
    for (size_t io_enq_bits_wb_wxd_rd__Vidx = 0; io_enq_bits_wb_wxd_rd__Vidx < 1; ++io_enq_bits_wb_wxd_rd__Vidx) VL_SET_SVLV_I(32, io_enq_bits_wb_wxd_rd__Vcvt + 1 * io_enq_bits_wb_wxd_rd__Vidx, io_enq_bits_wb_wxd_rd);
    svLogic io_enq_bits_wxd__Vcvt;
    for (size_t io_enq_bits_wxd__Vidx = 0; io_enq_bits_wxd__Vidx < 1; ++io_enq_bits_wxd__Vidx) io_enq_bits_wxd__Vcvt = io_enq_bits_wxd;
    svLogicVecVal io_enq_bits_reg_idxw__Vcvt[1];
    for (size_t io_enq_bits_reg_idxw__Vidx = 0; io_enq_bits_reg_idxw__Vidx < 1; ++io_enq_bits_reg_idxw__Vidx) VL_SET_SVLV_I(5, io_enq_bits_reg_idxw__Vcvt + 1 * io_enq_bits_reg_idxw__Vidx, io_enq_bits_reg_idxw);
    svLogicVecVal io_enq_bits_warp_id__Vcvt[1];
    for (size_t io_enq_bits_warp_id__Vidx = 0; io_enq_bits_warp_id__Vidx < 1; ++io_enq_bits_warp_id__Vidx) VL_SET_SVLV_I(2, io_enq_bits_warp_id__Vcvt + 1 * io_enq_bits_warp_id__Vidx, io_enq_bits_warp_id);
    svLogic io_deq_ready__Vcvt;
    for (size_t io_deq_ready__Vidx = 0; io_deq_ready__Vidx < 1; ++io_deq_ready__Vidx) io_deq_ready__Vcvt = io_deq_ready;
    Queue_protectlib_combo_ignore(handle___05FV__Vcvt, reset__Vcvt, io_enq_valid__Vcvt, io_enq_bits_wb_wxd_rd__Vcvt, io_enq_bits_wxd__Vcvt, io_enq_bits_reg_idxw__Vcvt, io_enq_bits_warp_id__Vcvt, io_deq_ready__Vcvt);
}

extern "C" void Queue_protectlib_final(void* handle___05FV);

VL_INLINE_OPT void VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result__DOT__Queue_protectlib_final_TOP(QData/*63:0*/ handle___05FV) {
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result__DOT__Queue_protectlib_final_TOP\n"); );
    // Body
    void* handle___05FV__Vcvt;
    for (size_t handle___05FV__Vidx = 0; handle___05FV__Vidx < 1; ++handle___05FV__Vidx) handle___05FV__Vcvt = VL_CVT_Q_VP(handle___05FV);
    Queue_protectlib_final(handle___05FV__Vcvt);
}

extern "C" void Queue_1_protectlib_check_hash(int protectlib_hash___05FV);

VL_INLINE_OPT void VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_check_hash_TOP(IData/*31:0*/ protectlib_hash___05FV) {
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_check_hash_TOP\n"); );
    // Body
    int protectlib_hash___05FV__Vcvt;
    for (size_t protectlib_hash___05FV__Vidx = 0; protectlib_hash___05FV__Vidx < 1; ++protectlib_hash___05FV__Vidx) protectlib_hash___05FV__Vcvt = protectlib_hash___05FV;
    Queue_1_protectlib_check_hash(protectlib_hash___05FV__Vcvt);
}

extern "C" void* Queue_1_protectlib_create(const char* scope___05FV);

VL_INLINE_OPT void VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_create_TOP(std::string scope___05FV, QData/*63:0*/ &Queue_1_protectlib_create__Vfuncrtn) {
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_create_TOP\n"); );
    // Body
    const char* scope___05FV__Vcvt;
    for (size_t scope___05FV__Vidx = 0; scope___05FV__Vidx < 1; ++scope___05FV__Vidx) scope___05FV__Vcvt = scope___05FV.c_str();
    void* Queue_1_protectlib_create__Vfuncrtn__Vcvt;
    Queue_1_protectlib_create__Vfuncrtn__Vcvt = Queue_1_protectlib_create(scope___05FV__Vcvt);
    Queue_1_protectlib_create__Vfuncrtn = VL_CVT_VP_Q(Queue_1_protectlib_create__Vfuncrtn__Vcvt);
}

extern "C" long long Queue_1_protectlib_combo_update(void* handle___05FV, svLogic reset, svLogic* io_enq_ready, svLogic io_enq_valid, const svLogicVecVal* io_enq_bits_wid, svLogic io_enq_bits_jump, const svLogicVecVal* io_enq_bits_new_pc, svLogic io_deq_ready, svLogic* io_deq_valid, svLogicVecVal* io_deq_bits_wid, svLogic* io_deq_bits_jump, svLogicVecVal* io_deq_bits_new_pc);

VL_INLINE_OPT void VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_combo_update_TOP(QData/*63:0*/ handle___05FV, CData/*0:0*/ reset, CData/*0:0*/ &io_enq_ready, CData/*0:0*/ io_enq_valid, CData/*1:0*/ io_enq_bits_wid, CData/*0:0*/ io_enq_bits_jump, IData/*31:0*/ io_enq_bits_new_pc, CData/*0:0*/ io_deq_ready, CData/*0:0*/ &io_deq_valid, CData/*1:0*/ &io_deq_bits_wid, CData/*0:0*/ &io_deq_bits_jump, IData/*31:0*/ &io_deq_bits_new_pc, QData/*63:0*/ &Queue_1_protectlib_combo_update__Vfuncrtn) {
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_combo_update_TOP\n"); );
    // Body
    void* handle___05FV__Vcvt;
    for (size_t handle___05FV__Vidx = 0; handle___05FV__Vidx < 1; ++handle___05FV__Vidx) handle___05FV__Vcvt = VL_CVT_Q_VP(handle___05FV);
    svLogic reset__Vcvt;
    for (size_t reset__Vidx = 0; reset__Vidx < 1; ++reset__Vidx) reset__Vcvt = reset;
    svLogic io_enq_ready__Vcvt;
    svLogic io_enq_valid__Vcvt;
    for (size_t io_enq_valid__Vidx = 0; io_enq_valid__Vidx < 1; ++io_enq_valid__Vidx) io_enq_valid__Vcvt = io_enq_valid;
    svLogicVecVal io_enq_bits_wid__Vcvt[1];
    for (size_t io_enq_bits_wid__Vidx = 0; io_enq_bits_wid__Vidx < 1; ++io_enq_bits_wid__Vidx) VL_SET_SVLV_I(2, io_enq_bits_wid__Vcvt + 1 * io_enq_bits_wid__Vidx, io_enq_bits_wid);
    svLogic io_enq_bits_jump__Vcvt;
    for (size_t io_enq_bits_jump__Vidx = 0; io_enq_bits_jump__Vidx < 1; ++io_enq_bits_jump__Vidx) io_enq_bits_jump__Vcvt = io_enq_bits_jump;
    svLogicVecVal io_enq_bits_new_pc__Vcvt[1];
    for (size_t io_enq_bits_new_pc__Vidx = 0; io_enq_bits_new_pc__Vidx < 1; ++io_enq_bits_new_pc__Vidx) VL_SET_SVLV_I(32, io_enq_bits_new_pc__Vcvt + 1 * io_enq_bits_new_pc__Vidx, io_enq_bits_new_pc);
    svLogic io_deq_ready__Vcvt;
    for (size_t io_deq_ready__Vidx = 0; io_deq_ready__Vidx < 1; ++io_deq_ready__Vidx) io_deq_ready__Vcvt = io_deq_ready;
    svLogic io_deq_valid__Vcvt;
    svLogicVecVal io_deq_bits_wid__Vcvt[1];
    svLogic io_deq_bits_jump__Vcvt;
    svLogicVecVal io_deq_bits_new_pc__Vcvt[1];
    long long Queue_1_protectlib_combo_update__Vfuncrtn__Vcvt;
    Queue_1_protectlib_combo_update__Vfuncrtn__Vcvt = Queue_1_protectlib_combo_update(handle___05FV__Vcvt, reset__Vcvt, &io_enq_ready__Vcvt, io_enq_valid__Vcvt, io_enq_bits_wid__Vcvt, io_enq_bits_jump__Vcvt, io_enq_bits_new_pc__Vcvt, io_deq_ready__Vcvt, &io_deq_valid__Vcvt, io_deq_bits_wid__Vcvt, &io_deq_bits_jump__Vcvt, io_deq_bits_new_pc__Vcvt);
    io_enq_ready = (1U & io_enq_ready__Vcvt);
    io_deq_valid = (1U & io_deq_valid__Vcvt);
    io_deq_bits_wid = (3U & VL_SET_I_SVLV(io_deq_bits_wid__Vcvt));
    io_deq_bits_jump = (1U & io_deq_bits_jump__Vcvt);
    io_deq_bits_new_pc = VL_SET_I_SVLV(io_deq_bits_new_pc__Vcvt);
    Queue_1_protectlib_combo_update__Vfuncrtn = Queue_1_protectlib_combo_update__Vfuncrtn__Vcvt;
}

extern "C" long long Queue_1_protectlib_seq_update(void* handle___05FV, svLogic clock, svLogic* io_enq_ready, svLogic* io_deq_valid, svLogicVecVal* io_deq_bits_wid, svLogic* io_deq_bits_jump, svLogicVecVal* io_deq_bits_new_pc);

VL_INLINE_OPT void VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_seq_update_TOP(QData/*63:0*/ handle___05FV, CData/*0:0*/ clock, CData/*0:0*/ &io_enq_ready, CData/*0:0*/ &io_deq_valid, CData/*1:0*/ &io_deq_bits_wid, CData/*0:0*/ &io_deq_bits_jump, IData/*31:0*/ &io_deq_bits_new_pc, QData/*63:0*/ &Queue_1_protectlib_seq_update__Vfuncrtn) {
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_seq_update_TOP\n"); );
    // Body
    void* handle___05FV__Vcvt;
    for (size_t handle___05FV__Vidx = 0; handle___05FV__Vidx < 1; ++handle___05FV__Vidx) handle___05FV__Vcvt = VL_CVT_Q_VP(handle___05FV);
    svLogic clock__Vcvt;
    for (size_t clock__Vidx = 0; clock__Vidx < 1; ++clock__Vidx) clock__Vcvt = clock;
    svLogic io_enq_ready__Vcvt;
    svLogic io_deq_valid__Vcvt;
    svLogicVecVal io_deq_bits_wid__Vcvt[1];
    svLogic io_deq_bits_jump__Vcvt;
    svLogicVecVal io_deq_bits_new_pc__Vcvt[1];
    long long Queue_1_protectlib_seq_update__Vfuncrtn__Vcvt;
    Queue_1_protectlib_seq_update__Vfuncrtn__Vcvt = Queue_1_protectlib_seq_update(handle___05FV__Vcvt, clock__Vcvt, &io_enq_ready__Vcvt, &io_deq_valid__Vcvt, io_deq_bits_wid__Vcvt, &io_deq_bits_jump__Vcvt, io_deq_bits_new_pc__Vcvt);
    io_enq_ready = (1U & io_enq_ready__Vcvt);
    io_deq_valid = (1U & io_deq_valid__Vcvt);
    io_deq_bits_wid = (3U & VL_SET_I_SVLV(io_deq_bits_wid__Vcvt));
    io_deq_bits_jump = (1U & io_deq_bits_jump__Vcvt);
    io_deq_bits_new_pc = VL_SET_I_SVLV(io_deq_bits_new_pc__Vcvt);
    Queue_1_protectlib_seq_update__Vfuncrtn = Queue_1_protectlib_seq_update__Vfuncrtn__Vcvt;
}

extern "C" void Queue_1_protectlib_combo_ignore(void* handle___05FV, svLogic reset, svLogic io_enq_valid, const svLogicVecVal* io_enq_bits_wid, svLogic io_enq_bits_jump, const svLogicVecVal* io_enq_bits_new_pc, svLogic io_deq_ready);

VL_INLINE_OPT void VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_combo_ignore_TOP(QData/*63:0*/ handle___05FV, CData/*0:0*/ reset, CData/*0:0*/ io_enq_valid, CData/*1:0*/ io_enq_bits_wid, CData/*0:0*/ io_enq_bits_jump, IData/*31:0*/ io_enq_bits_new_pc, CData/*0:0*/ io_deq_ready) {
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_combo_ignore_TOP\n"); );
    // Body
    void* handle___05FV__Vcvt;
    for (size_t handle___05FV__Vidx = 0; handle___05FV__Vidx < 1; ++handle___05FV__Vidx) handle___05FV__Vcvt = VL_CVT_Q_VP(handle___05FV);
    svLogic reset__Vcvt;
    for (size_t reset__Vidx = 0; reset__Vidx < 1; ++reset__Vidx) reset__Vcvt = reset;
    svLogic io_enq_valid__Vcvt;
    for (size_t io_enq_valid__Vidx = 0; io_enq_valid__Vidx < 1; ++io_enq_valid__Vidx) io_enq_valid__Vcvt = io_enq_valid;
    svLogicVecVal io_enq_bits_wid__Vcvt[1];
    for (size_t io_enq_bits_wid__Vidx = 0; io_enq_bits_wid__Vidx < 1; ++io_enq_bits_wid__Vidx) VL_SET_SVLV_I(2, io_enq_bits_wid__Vcvt + 1 * io_enq_bits_wid__Vidx, io_enq_bits_wid);
    svLogic io_enq_bits_jump__Vcvt;
    for (size_t io_enq_bits_jump__Vidx = 0; io_enq_bits_jump__Vidx < 1; ++io_enq_bits_jump__Vidx) io_enq_bits_jump__Vcvt = io_enq_bits_jump;
    svLogicVecVal io_enq_bits_new_pc__Vcvt[1];
    for (size_t io_enq_bits_new_pc__Vidx = 0; io_enq_bits_new_pc__Vidx < 1; ++io_enq_bits_new_pc__Vidx) VL_SET_SVLV_I(32, io_enq_bits_new_pc__Vcvt + 1 * io_enq_bits_new_pc__Vidx, io_enq_bits_new_pc);
    svLogic io_deq_ready__Vcvt;
    for (size_t io_deq_ready__Vidx = 0; io_deq_ready__Vidx < 1; ++io_deq_ready__Vidx) io_deq_ready__Vcvt = io_deq_ready;
    Queue_1_protectlib_combo_ignore(handle___05FV__Vcvt, reset__Vcvt, io_enq_valid__Vcvt, io_enq_bits_wid__Vcvt, io_enq_bits_jump__Vcvt, io_enq_bits_new_pc__Vcvt, io_deq_ready__Vcvt);
}

extern "C" void Queue_1_protectlib_final(void* handle___05FV);

VL_INLINE_OPT void VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_final_TOP(QData/*63:0*/ handle___05FV) {
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root____Vdpiimwrap_ALUexe__DOT__result_br__DOT__Queue_1_protectlib_final_TOP\n"); );
    // Body
    void* handle___05FV__Vcvt;
    for (size_t handle___05FV__Vidx = 0; handle___05FV__Vidx < 1; ++handle___05FV__Vidx) handle___05FV__Vcvt = VL_CVT_Q_VP(handle___05FV);
    Queue_1_protectlib_final(handle___05FV__Vcvt);
}

#ifdef VL_DEBUG
VL_ATTR_COLD void VALUexe___024root___dump_triggers__ico(VALUexe___024root* vlSelf);
#endif  // VL_DEBUG

void VALUexe___024root___eval_triggers__ico(VALUexe___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VALUexe__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root___eval_triggers__ico\n"); );
    // Body
    vlSelf->__VicoTriggered.at(0U) = (0U == vlSelf->__VicoIterCount);
#ifdef VL_DEBUG
    if (VL_UNLIKELY(vlSymsp->_vm_contextp__->debug())) {
        VALUexe___024root___dump_triggers__ico(vlSelf);
    }
#endif
}

#ifdef VL_DEBUG
VL_ATTR_COLD void VALUexe___024root___dump_triggers__act(VALUexe___024root* vlSelf);
#endif  // VL_DEBUG

void VALUexe___024root___eval_triggers__act(VALUexe___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VALUexe__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VALUexe___024root___eval_triggers__act\n"); );
    // Body
    vlSelf->__VactTriggered.at(0U) = ((IData)(vlSelf->__Vcellinp__ALUexe__clock) 
                                      ^ (IData)(vlSelf->__Vtrigrprev__TOP____Vcellinp__ALUexe__clock));
    vlSelf->__Vtrigrprev__TOP____Vcellinp__ALUexe__clock 
        = vlSelf->__Vcellinp__ALUexe__clock;
#ifdef VL_DEBUG
    if (VL_UNLIKELY(vlSymsp->_vm_contextp__->debug())) {
        VALUexe___024root___dump_triggers__act(vlSelf);
    }
#endif
}

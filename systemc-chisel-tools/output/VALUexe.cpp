// Verilated -*- SystemC -*-
// DESCRIPTION: Verilator output: Model implementation (design independent parts)

#include "VALUexe.h"
#include "VALUexe__Syms.h"
#include "verilated_dpi.h"

//============================================================
// Constructors

VALUexe::VALUexe(sc_module_name /* unused */)
    : VerilatedModel{*Verilated::threadContextp()}
    , vlSymsp{new VALUexe__Syms(contextp(), name(), this)}
    , clock{vlSymsp->TOP.clock}
    , reset{vlSymsp->TOP.reset}
    , io_in_ready{vlSymsp->TOP.io_in_ready}
    , io_in_valid{vlSymsp->TOP.io_in_valid}
    , io_in_bits_ctrl_wid{vlSymsp->TOP.io_in_bits_ctrl_wid}
    , io_in_bits_ctrl_fp{vlSymsp->TOP.io_in_bits_ctrl_fp}
    , io_in_bits_ctrl_branch{vlSymsp->TOP.io_in_bits_ctrl_branch}
    , io_in_bits_ctrl_simt_stack{vlSymsp->TOP.io_in_bits_ctrl_simt_stack}
    , io_in_bits_ctrl_simt_stack_op{vlSymsp->TOP.io_in_bits_ctrl_simt_stack_op}
    , io_in_bits_ctrl_barrier{vlSymsp->TOP.io_in_bits_ctrl_barrier}
    , io_in_bits_ctrl_csr{vlSymsp->TOP.io_in_bits_ctrl_csr}
    , io_in_bits_ctrl_reverse{vlSymsp->TOP.io_in_bits_ctrl_reverse}
    , io_in_bits_ctrl_sel_alu2{vlSymsp->TOP.io_in_bits_ctrl_sel_alu2}
    , io_in_bits_ctrl_sel_alu1{vlSymsp->TOP.io_in_bits_ctrl_sel_alu1}
    , io_in_bits_ctrl_isvec{vlSymsp->TOP.io_in_bits_ctrl_isvec}
    , io_in_bits_ctrl_sel_alu3{vlSymsp->TOP.io_in_bits_ctrl_sel_alu3}
    , io_in_bits_ctrl_mask{vlSymsp->TOP.io_in_bits_ctrl_mask}
    , io_in_bits_ctrl_sel_imm{vlSymsp->TOP.io_in_bits_ctrl_sel_imm}
    , io_in_bits_ctrl_mem_whb{vlSymsp->TOP.io_in_bits_ctrl_mem_whb}
    , io_in_bits_ctrl_mem_unsigned{vlSymsp->TOP.io_in_bits_ctrl_mem_unsigned}
    , io_in_bits_ctrl_alu_fn{vlSymsp->TOP.io_in_bits_ctrl_alu_fn}
    , io_in_bits_ctrl_mem{vlSymsp->TOP.io_in_bits_ctrl_mem}
    , io_in_bits_ctrl_mul{vlSymsp->TOP.io_in_bits_ctrl_mul}
    , io_in_bits_ctrl_mem_cmd{vlSymsp->TOP.io_in_bits_ctrl_mem_cmd}
    , io_in_bits_ctrl_mop{vlSymsp->TOP.io_in_bits_ctrl_mop}
    , io_in_bits_ctrl_reg_idx1{vlSymsp->TOP.io_in_bits_ctrl_reg_idx1}
    , io_in_bits_ctrl_reg_idx2{vlSymsp->TOP.io_in_bits_ctrl_reg_idx2}
    , io_in_bits_ctrl_reg_idx3{vlSymsp->TOP.io_in_bits_ctrl_reg_idx3}
    , io_in_bits_ctrl_reg_idxw{vlSymsp->TOP.io_in_bits_ctrl_reg_idxw}
    , io_in_bits_ctrl_wfd{vlSymsp->TOP.io_in_bits_ctrl_wfd}
    , io_in_bits_ctrl_fence{vlSymsp->TOP.io_in_bits_ctrl_fence}
    , io_in_bits_ctrl_sfu{vlSymsp->TOP.io_in_bits_ctrl_sfu}
    , io_in_bits_ctrl_readmask{vlSymsp->TOP.io_in_bits_ctrl_readmask}
    , io_in_bits_ctrl_writemask{vlSymsp->TOP.io_in_bits_ctrl_writemask}
    , io_in_bits_ctrl_wxd{vlSymsp->TOP.io_in_bits_ctrl_wxd}
    , io_out_ready{vlSymsp->TOP.io_out_ready}
    , io_out_valid{vlSymsp->TOP.io_out_valid}
    , io_out_bits_wxd{vlSymsp->TOP.io_out_bits_wxd}
    , io_out_bits_reg_idxw{vlSymsp->TOP.io_out_bits_reg_idxw}
    , io_out_bits_warp_id{vlSymsp->TOP.io_out_bits_warp_id}
    , io_out2br_ready{vlSymsp->TOP.io_out2br_ready}
    , io_out2br_valid{vlSymsp->TOP.io_out2br_valid}
    , io_out2br_bits_wid{vlSymsp->TOP.io_out2br_bits_wid}
    , io_out2br_bits_jump{vlSymsp->TOP.io_out2br_bits_jump}
    , io_in_bits_in1{vlSymsp->TOP.io_in_bits_in1}
    , io_in_bits_in2{vlSymsp->TOP.io_in_bits_in2}
    , io_in_bits_in3{vlSymsp->TOP.io_in_bits_in3}
    , io_in_bits_ctrl_inst{vlSymsp->TOP.io_in_bits_ctrl_inst}
    , io_in_bits_ctrl_pc{vlSymsp->TOP.io_in_bits_ctrl_pc}
    , io_out_bits_wb_wxd_rd{vlSymsp->TOP.io_out_bits_wb_wxd_rd}
    , io_out2br_bits_new_pc{vlSymsp->TOP.io_out2br_bits_new_pc}
    , rootp{&(vlSymsp->TOP)}
{
    // Register model with the context
    contextp()->addModel(this);
    // Sensitivities on all clocks and combinational inputs
    SC_METHOD(eval);
    sensitive << clock;
    sensitive << reset;
    sensitive << io_in_valid;
    sensitive << io_in_bits_ctrl_wid;
    sensitive << io_in_bits_ctrl_branch;
    sensitive << io_in_bits_ctrl_alu_fn;
    sensitive << io_in_bits_ctrl_reg_idxw;
    sensitive << io_in_bits_ctrl_wxd;
    sensitive << io_out_ready;
    sensitive << io_out2br_ready;
    sensitive << io_in_bits_in1;
    sensitive << io_in_bits_in2;
    sensitive << io_in_bits_in3;

}

//============================================================
// Destructor

VALUexe::~VALUexe() {
    delete vlSymsp;
}

//============================================================
// Evaluation function

#ifdef VL_DEBUG
void VALUexe___024root___eval_debug_assertions(VALUexe___024root* vlSelf);
#endif  // VL_DEBUG
void VALUexe___024root___eval_static(VALUexe___024root* vlSelf);
void VALUexe___024root___eval_initial(VALUexe___024root* vlSelf);
void VALUexe___024root___eval_settle(VALUexe___024root* vlSelf);
void VALUexe___024root___eval(VALUexe___024root* vlSelf);

void VALUexe::eval_step() {
    VL_DEBUG_IF(VL_DBG_MSGF("+++++TOP Evaluate VALUexe::eval_step\n"); );
#ifdef VL_DEBUG
    // Debug assertions
    VALUexe___024root___eval_debug_assertions(&(vlSymsp->TOP));
#endif  // VL_DEBUG
    if (VL_UNLIKELY(!vlSymsp->__Vm_didInit)) {
        vlSymsp->__Vm_didInit = true;
        VL_DEBUG_IF(VL_DBG_MSGF("+ Initial\n"););
        VALUexe___024root___eval_static(&(vlSymsp->TOP));
        VALUexe___024root___eval_initial(&(vlSymsp->TOP));
        VALUexe___024root___eval_settle(&(vlSymsp->TOP));
    }
    VL_DEBUG_IF(VL_DBG_MSGF("+ Eval\n"););
    VALUexe___024root___eval(&(vlSymsp->TOP));
    // Evaluate cleanup
}

//============================================================
// Events and timing
bool VALUexe::eventsPending() { return false; }

uint64_t VALUexe::nextTimeSlot() {
    VL_FATAL_MT(__FILE__, __LINE__, "", "%Error: No delays in the design");
    return 0;
}

//============================================================
// Utilities

//============================================================
// Invoke final blocks

void VALUexe___024root___eval_final(VALUexe___024root* vlSelf);

VL_ATTR_COLD void VALUexe::final() {
    VALUexe___024root___eval_final(&(vlSymsp->TOP));
}

//============================================================
// Implementations of abstract methods from VerilatedModel

const char* VALUexe::hierName() const { return vlSymsp->name(); }
const char* VALUexe::modelName() const { return "VALUexe"; }
unsigned VALUexe::threads() const { return 1; }

// Verilated -*- SystemC -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See VALUexe.h for the primary calling header

#include "verilated.h"
#include "verilated_dpi.h"

#include "VALUexe__Syms.h"
#include "VALUexe___024root.h"

void VALUexe___024root___ctor_var_reset(VALUexe___024root* vlSelf);

VALUexe___024root::VALUexe___024root(VALUexe__Syms* symsp, const char* name)
    : VerilatedModule{name}
    , clock("clock")
    , reset("reset")
    , io_in_ready("io_in_ready")
    , io_in_valid("io_in_valid")
    , io_in_bits_ctrl_wid("io_in_bits_ctrl_wid")
    , io_in_bits_ctrl_fp("io_in_bits_ctrl_fp")
    , io_in_bits_ctrl_branch("io_in_bits_ctrl_branch")
    , io_in_bits_ctrl_simt_stack("io_in_bits_ctrl_simt_stack")
    , io_in_bits_ctrl_simt_stack_op("io_in_bits_ctrl_simt_stack_op")
    , io_in_bits_ctrl_barrier("io_in_bits_ctrl_barrier")
    , io_in_bits_ctrl_csr("io_in_bits_ctrl_csr")
    , io_in_bits_ctrl_reverse("io_in_bits_ctrl_reverse")
    , io_in_bits_ctrl_sel_alu2("io_in_bits_ctrl_sel_alu2")
    , io_in_bits_ctrl_sel_alu1("io_in_bits_ctrl_sel_alu1")
    , io_in_bits_ctrl_isvec("io_in_bits_ctrl_isvec")
    , io_in_bits_ctrl_sel_alu3("io_in_bits_ctrl_sel_alu3")
    , io_in_bits_ctrl_mask("io_in_bits_ctrl_mask")
    , io_in_bits_ctrl_sel_imm("io_in_bits_ctrl_sel_imm")
    , io_in_bits_ctrl_mem_whb("io_in_bits_ctrl_mem_whb")
    , io_in_bits_ctrl_mem_unsigned("io_in_bits_ctrl_mem_unsigned")
    , io_in_bits_ctrl_alu_fn("io_in_bits_ctrl_alu_fn")
    , io_in_bits_ctrl_mem("io_in_bits_ctrl_mem")
    , io_in_bits_ctrl_mul("io_in_bits_ctrl_mul")
    , io_in_bits_ctrl_mem_cmd("io_in_bits_ctrl_mem_cmd")
    , io_in_bits_ctrl_mop("io_in_bits_ctrl_mop")
    , io_in_bits_ctrl_reg_idx1("io_in_bits_ctrl_reg_idx1")
    , io_in_bits_ctrl_reg_idx2("io_in_bits_ctrl_reg_idx2")
    , io_in_bits_ctrl_reg_idx3("io_in_bits_ctrl_reg_idx3")
    , io_in_bits_ctrl_reg_idxw("io_in_bits_ctrl_reg_idxw")
    , io_in_bits_ctrl_wfd("io_in_bits_ctrl_wfd")
    , io_in_bits_ctrl_fence("io_in_bits_ctrl_fence")
    , io_in_bits_ctrl_sfu("io_in_bits_ctrl_sfu")
    , io_in_bits_ctrl_readmask("io_in_bits_ctrl_readmask")
    , io_in_bits_ctrl_writemask("io_in_bits_ctrl_writemask")
    , io_in_bits_ctrl_wxd("io_in_bits_ctrl_wxd")
    , io_out_ready("io_out_ready")
    , io_out_valid("io_out_valid")
    , io_out_bits_wxd("io_out_bits_wxd")
    , io_out_bits_reg_idxw("io_out_bits_reg_idxw")
    , io_out_bits_warp_id("io_out_bits_warp_id")
    , io_out2br_ready("io_out2br_ready")
    , io_out2br_valid("io_out2br_valid")
    , io_out2br_bits_wid("io_out2br_bits_wid")
    , io_out2br_bits_jump("io_out2br_bits_jump")
    , io_in_bits_in1("io_in_bits_in1")
    , io_in_bits_in2("io_in_bits_in2")
    , io_in_bits_in3("io_in_bits_in3")
    , io_in_bits_ctrl_inst("io_in_bits_ctrl_inst")
    , io_in_bits_ctrl_pc("io_in_bits_ctrl_pc")
    , io_out_bits_wb_wxd_rd("io_out_bits_wb_wxd_rd")
    , io_out2br_bits_new_pc("io_out2br_bits_new_pc")
    , vlSymsp{symsp}
 {
    // Reset structure values
    VALUexe___024root___ctor_var_reset(this);
}

void VALUexe___024root::__Vconfigure(bool first) {
    if (false && first) {}  // Prevent unused
}

VALUexe___024root::~VALUexe___024root() {
}

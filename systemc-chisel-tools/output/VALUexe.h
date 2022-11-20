// Verilated -*- SystemC -*-
// DESCRIPTION: Verilator output: Primary model header
//
// This header should be included by all source files instantiating the design.
// The class here is then constructed to instantiate the design.
// See the Verilator manual for examples.

#ifndef VERILATED_VALUEXE_H_
#define VERILATED_VALUEXE_H_  // guard

#include "systemc.h"
#include "verilated_sc.h"
#include "verilated.h"

class VALUexe__Syms;
class VALUexe___024root;

// This class is the main interface to the Verilated model
class VALUexe VL_NOT_FINAL : public ::sc_core::sc_module, public VerilatedModel {
  private:
    // Symbol table holding complete model state (owned by this class)
    VALUexe__Syms* const vlSymsp;

  public:

    // PORTS
    // The application code writes and reads these signals to
    // propagate new values into/out from the Verilated model.
    sc_in<bool> &clock;
    sc_in<bool> &reset;
    sc_out<bool> &io_in_ready;
    sc_in<bool> &io_in_valid;
    sc_in<uint32_t> &io_in_bits_ctrl_wid;
    sc_in<bool> &io_in_bits_ctrl_fp;
    sc_in<uint32_t> &io_in_bits_ctrl_branch;
    sc_in<bool> &io_in_bits_ctrl_simt_stack;
    sc_in<bool> &io_in_bits_ctrl_simt_stack_op;
    sc_in<bool> &io_in_bits_ctrl_barrier;
    sc_in<uint32_t> &io_in_bits_ctrl_csr;
    sc_in<bool> &io_in_bits_ctrl_reverse;
    sc_in<uint32_t> &io_in_bits_ctrl_sel_alu2;
    sc_in<uint32_t> &io_in_bits_ctrl_sel_alu1;
    sc_in<bool> &io_in_bits_ctrl_isvec;
    sc_in<uint32_t> &io_in_bits_ctrl_sel_alu3;
    sc_in<bool> &io_in_bits_ctrl_mask;
    sc_in<uint32_t> &io_in_bits_ctrl_sel_imm;
    sc_in<uint32_t> &io_in_bits_ctrl_mem_whb;
    sc_in<bool> &io_in_bits_ctrl_mem_unsigned;
    sc_in<uint32_t> &io_in_bits_ctrl_alu_fn;
    sc_in<bool> &io_in_bits_ctrl_mem;
    sc_in<bool> &io_in_bits_ctrl_mul;
    sc_in<uint32_t> &io_in_bits_ctrl_mem_cmd;
    sc_in<uint32_t> &io_in_bits_ctrl_mop;
    sc_in<uint32_t> &io_in_bits_ctrl_reg_idx1;
    sc_in<uint32_t> &io_in_bits_ctrl_reg_idx2;
    sc_in<uint32_t> &io_in_bits_ctrl_reg_idx3;
    sc_in<uint32_t> &io_in_bits_ctrl_reg_idxw;
    sc_in<bool> &io_in_bits_ctrl_wfd;
    sc_in<bool> &io_in_bits_ctrl_fence;
    sc_in<bool> &io_in_bits_ctrl_sfu;
    sc_in<bool> &io_in_bits_ctrl_readmask;
    sc_in<bool> &io_in_bits_ctrl_writemask;
    sc_in<bool> &io_in_bits_ctrl_wxd;
    sc_in<bool> &io_out_ready;
    sc_out<bool> &io_out_valid;
    sc_out<bool> &io_out_bits_wxd;
    sc_out<uint32_t> &io_out_bits_reg_idxw;
    sc_out<uint32_t> &io_out_bits_warp_id;
    sc_in<bool> &io_out2br_ready;
    sc_out<bool> &io_out2br_valid;
    sc_out<uint32_t> &io_out2br_bits_wid;
    sc_out<bool> &io_out2br_bits_jump;
    sc_in<uint32_t> &io_in_bits_in1;
    sc_in<uint32_t> &io_in_bits_in2;
    sc_in<uint32_t> &io_in_bits_in3;
    sc_in<uint32_t> &io_in_bits_ctrl_inst;
    sc_in<uint32_t> &io_in_bits_ctrl_pc;
    sc_out<uint32_t> &io_out_bits_wb_wxd_rd;
    sc_out<uint32_t> &io_out2br_bits_new_pc;

    // CELLS
    // Public to allow access to /* verilator public */ items.
    // Otherwise the application code can consider these internals.

    // Root instance pointer to allow access to model internals,
    // including inlined /* verilator public_flat_* */ items.
    VALUexe___024root* const rootp;

    // CONSTRUCTORS
    SC_CTOR(VALUexe);
    virtual ~VALUexe();
  private:
    VL_UNCOPYABLE(VALUexe);  ///< Copying not allowed

  public:
    // API METHODS
  private:
    void eval() { eval_step(); }
    void eval_step();
  public:
    void final();
    /// Are there scheduled events to handle?
    bool eventsPending();
    /// Returns time at next time slot. Aborts if !eventsPending()
    uint64_t nextTimeSlot();

    // Abstract methods from VerilatedModel
    const char* hierName() const override final;
    const char* modelName() const override final;
    unsigned threads() const override final;
} VL_ATTR_ALIGNED(VL_CACHE_LINE_BYTES);

#endif  // guard

// Verilated -*- SystemC -*-
// DESCRIPTION: Verilator output: Symbol table internal header
//
// Internal details; most calling programs do not need this header,
// unless using verilator public meta comments.

#ifndef VERILATED_VALUEXE__SYMS_H_
#define VERILATED_VALUEXE__SYMS_H_  // guard

#include "systemc.h"
#include "verilated_sc.h"
#include "verilated.h"

// INCLUDE MODEL CLASS

#include "VALUexe.h"

// INCLUDE MODULE CLASSES
#include "VALUexe___024root.h"

// SYMS CLASS (contains all model state)
class VALUexe__Syms final : public VerilatedSyms {
  public:
    // INTERNAL STATE
    VALUexe* const __Vm_modelp;
    bool __Vm_didInit = false;

    // MODULE INSTANCE STATE
    VALUexe___024root              TOP;

    // CONSTRUCTORS
    VALUexe__Syms(VerilatedContext* contextp, const char* namep, VALUexe* modelp);
    ~VALUexe__Syms();

    // METHODS
    const char* name() { return TOP.name(); }
} VL_ATTR_ALIGNED(VL_CACHE_LINE_BYTES);

#endif  // guard

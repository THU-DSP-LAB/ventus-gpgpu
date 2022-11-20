// Verilated -*- SystemC -*-
// DESCRIPTION: Verilator output: Symbol table implementation internals

#include "VALUexe__Syms.h"
#include "VALUexe.h"
#include "VALUexe___024root.h"

// FUNCTIONS
VALUexe__Syms::~VALUexe__Syms()
{
}

VALUexe__Syms::VALUexe__Syms(VerilatedContext* contextp, const char* namep, VALUexe* modelp)
    : VerilatedSyms{contextp}
    // Setup internal state of the Syms class
    , __Vm_modelp{modelp}
    // Setup module instances
    , TOP{this, namep}
{
    // Configure time unit / time precision
    _vm_contextp__->timeunit(-12);
    _vm_contextp__->timeprecision(-12);
    // Setup each module's pointers to their submodules
    // Setup each module's pointer back to symbol table (for public functions)
    TOP.__Vconfigure(true);
}

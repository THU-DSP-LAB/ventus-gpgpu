# systemc-chisel-tools
A collection of tools for working with Chisel-generated hardware in SystemC.

chisel-to-systemc.sh
=====================
This script translates a Chisel hardware design into SystemC (via Verilator),
then generates a simple SystemC testbench skeleton that instantiates the module and 
binds signals to the inputs and outputs.
There is also some preliminary support for working with Decoupled interfaces in Chisel:
interfaces with _ready/_valid/_bits naming are extracted and treated separately,
generating simple adapters that support bridging the SystemC-style FIFO interfaces
(sc_fifo<x>) and the signal-level interface. This enables creating independent SystemC
threads that feed the FIFOs with data.

Writing complicated testbenches in SystemC is may sometimes be easier compared to Chisel, since it is
possible to spawn threads executing in parallel and the entire C++ language is also at your
disposal for modelling the signal driver/tester behavior.

Usage: chisel-to-systemc-tb.sh verilog_module target_dir [-t]

If -t is specified, a testbench skeleton will also be generated.


Requirements
=============

 - working Chisel setup (sbt etc. -- see chisel.eecs.berkeley.edu)
 - Verilator
 - SystemC (SYSTEMC_ROOT, SYSTEMC_INCLUDE and SYSTEMC_LIBDIR env vars must be set)
 - Python
 

# Check for sanity to avoid later confusion
ifneq ($(words $(CURDIR)),1)
 $(error Unsupported: GNU Make cannot build in directories containing spaces, build elsewhere: '$(CURDIR)')
endif

######################################################################
# Set up variables

# If $VERILATOR_ROOT isn't in the environment, we assume it is part of a
# package install, and verilator is in your path. Otherwise find the
# binary relative to $VERILATOR_ROOT (such as when inside the git sources).
ifeq ($(VERILATOR_ROOT),)
VERILATOR = verilator
VERILATOR_COVERAGE = verilator_coverage
else
export VERILATOR_ROOT
VERILATOR = $(VERILATOR_ROOT)/bin/verilator
VERILATOR_COVERAGE = $(VERILATOR_ROOT)/bin/verilator_coverage
endif

# Generate C++ in executable form
VERILATOR_FLAGS += -cc --exe
# Generate makefile dependencies (not shown as complicates the Makefile)
#VERILATOR_FLAGS += -MMD
# Optimize
VERILATOR_FLAGS += -x-assign fast
# Warn abount lint issues; may not want this on less solid designs
#VERILATOR_FLAGS += -Wall
VERILATOR_FLAGS += -Wno-WIDTHEXPAND
# Make waveforms
#VERILATOR_FLAGS += --trace
# Check SystemVerilog assertions
VERILATOR_FLAGS += --assert
# Generate coverage analysis
#VERILATOR_FLAGS += --coverage
# Run Verilator in debug mode
#VERILATOR_FLAGS += --debug
# Add this trace to get a backtrace in gdb
#VERILATOR_FLAGS += --gdbbt

VERILATOR_FLAGS += -j 12
VERILATOR_FLAGS += -CFLAGS -std=c++17


######################################################################
DIR_SCALA = $(abspath ../ventus/src)

SRC_SCALA = 
SRC_V = dut.v
SRC_CXX = sim_main.cpp kernel.cpp MemBox.cpp
SRC_HEADER_CXX = kernel.hpp MemBox.hpp
VERILATOR_INPUT = $(SRC_V) $(SRC_CXX)

VERILATED_DIR = $(abspath obj_dir)
VERILATED_CXXFILES = $(shell find $(VERILATED_DIR) -name "*.cpp")
VERILATED_HFILES = $(shell find $(VERILATED_DIR) -name "*.h")
VERILATED_MKFILE = $(VERILATED_DIR)/Vdut.mk
VERILATED_FILES = $(VERILATED_MKFILE) $(VERILATED_CXXFILES) $(VERILATED_HFILES)

######################################################################
default: run

$(SRC_V): $(SRC_SCALA)
	cd .. && ./mill ventus.runMain top.emitVerilog
	mv GPGPU_SimTop.v $(SRC_V)

$(VERILATED_FILES): $(SRC_V)
	$(VERILATOR) $(VERILATOR_FLAGS) $(VERILATOR_INPUT)

$(VERILATED_DIR)/Vdut: $(VERILATED_FILES) $(SRC_CXX) $(SRC_HEADER_CXX)
	$(MAKE) -j12 -C $(VERILATED_DIR) -f Vdut.mk

run: $(VERILATED_DIR)/Vdut
	@rm -rf logs
	@mkdir -p logs
	@$(VERILATED_DIR)/Vdut
	#obj_dir/Vtop +trace

verilog: $(SRC_V)
.PHONY: run verilog

######################################################################
# Other targets

show-config:
	$(VERILATOR) -V

maintainer-copy::
clean mostlyclean distclean maintainer-clean::
	-rm -rf $(VERILATED_DIR)/* logs *.log *.dmp *.vpd coverage.dat core

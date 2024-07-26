# Check for sanity to avoid later confusion
ifneq ($(words $(CURDIR)),1)
 $(error Unsupported: GNU Make cannot build in directories containing spaces, build elsewhere: '$(CURDIR)')
endif

######################################################################
# Set up variables
NPROC = $(shell nproc)

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
VERILATOR_FLAGS += --trace-fst
# Check SystemVerilog assertions
VERILATOR_FLAGS += --assert
# Generate coverage analysis
#VERILATOR_FLAGS += --coverage
# Run Verilator in debug mode
#VERILATOR_FLAGS += --debug
# Add this trace to get a backtrace in gdb
#VERILATOR_FLAGS += --gdbbt

#VERILATOR_FLAGS += --threads $(shell expr $(NPROC) - 20)
#VERILATOR_FLAGS += --trace-threads 2
#VERILATOR_FLAGS += -j $(NPROC)
VERILATOR_FLAGS += -CFLAGS "-std=c++17 -g -O0"
VERILATOR_FLAGS += -LDFLAGS "-fuse-ld=mold"

CHISEL_VERILOG_ENABLE_PRINT = 0

######################################################################
DIR_SCALA = $(abspath ../ventus/src)

SRC_SCALA = $(shell find $(DIR_SCALA) -name "*.scala")
SRC_V = dut.v
SRC_CXX = sim_main.cpp kernel.cpp MemBox.cpp testcase.cpp
SRC_HEADER_CXX = kernel.hpp MemBox.hpp testcase.hpp
VERILATOR_INPUT = $(SRC_V) $(SRC_CXX)

VERILATED_DIR = $(abspath obj_dir)
VERILATED_CXXFILES = $(shell find $(VERILATED_DIR) -name "*.cpp")
VERILATED_HFILES = $(shell find $(VERILATED_DIR) -name "*.h")
VERILATED_MKFILE = $(VERILATED_DIR)/Vdut.mk
VERILATED_FILES = $(VERILATED_MKFILE) $(VERILATED_CXXFILES) $(VERILATED_HFILES)

######################################################################
default: $(VERILATED_DIR)/Vdut

$(SRC_V): $(SRC_SCALA)
	cd .. && ./mill ventus[6.4.0].runMain top.emitVerilog
	mv GPGPU_SimTop.v $(SRC_V)
	sed -i "1i\`define PRINTF_COND 0" $(SRC_V)

verilog: $(SRC_V)

verilator: verilog
	$(VERILATOR) $(VERILATOR_FLAGS) $(VERILATOR_INPUT)

#$(VERILATED_FILES): $(SRC_V)
#	$(VERILATOR) $(VERILATOR_FLAGS) $(VERILATOR_INPUT)

$(VERILATED_DIR)/Vdut: verilator
	$(MAKE) -j $(NPROC) -C $(VERILATED_DIR) -f Vdut.mk

run: $(VERILATED_DIR)/Vdut
	@-rm -f Vdut.fst
	@-rm -rf logs
	@mkdir -p logs
	@$(VERILATED_DIR)/Vdut

gdb: ${VERILATED_DIR}/Vdut
	gdb --tui $(VERILATED_DIR)/Vdut

.PHONY: run gdb verilog verilator

######################################################################
# Other targets

show-config:
	$(VERILATOR) -V

clean:
	-rm -f $(VERILATED_DIR)/*.a $(VERILATED_DIR)/*.d $(VERILATED_DIR)/*.o $(VERILATED_DIR)/Vdut
	-rm -f *.log *.dmp *.vpd coverage.dat core
	-rm -rf logs

clean_cpp: clean
	-rm -f $(VERILATED_DIR)/*

clean_verilog: clean_cpp
	-rm dut.V

clean_all: clean_verilog

.PHONY: clean clean_cpp clean_verilog clean_all

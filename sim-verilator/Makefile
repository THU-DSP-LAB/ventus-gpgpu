# Check for sanity to avoid later confusion
ifneq ($(words $(CURDIR)),1)
 $(error Unsupported: GNU Make cannot build in directories containing spaces, build elsewhere: '$(CURDIR)')
endif

######################################################################
# Helpers

# Choose the smaller one between two numbers
define MIN_FUNC
$(strip $(shell if [ $(1) -lt $(2) ]; then echo $(1); else echo $(2); fi))
endef

######################################################################
# Set up variables
NPROC_CPU = $(shell nproc)
NPROC_DUT = 10
NPROC_SIM = $(call MIN_FUNC, $(NPROC_CPU), $(NPROC_DUT))
NPROC_TRACE_FST = $(call MIN_FUNC, $(NPROC_SIM), 2)

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
VERILATOR_FLAGS += -cc --build --binary
# Generate makefile dependencies (not shown as complicates the Makefile)
VERILATOR_FLAGS += -MMD
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

VERILATOR_FLAGS += --threads $(NPROC_SIM)
VERILATOR_FLAGS += --trace-threads $(NPROC_TRACE_FST)
VERILATOR_FLAGS += -j $(NPROC_CPU)
VERILATOR_FLAGS += -CFLAGS "-std=c++17 -g -O0 -DSIM_LOG_ENABLE"
VERILATOR_FLAGS += -LDFLAGS "-fuse-ld=mold"

CHISEL_VERILOG_ENABLE_PRINT = 0

######################################################################
DIR_SCALA = ../ventus/src
DIR_OBJ = obj_dir

SRC_SCALA = $(shell find $(DIR_SCALA) -name "*.scala")
SRC_V = dut.v
SRC_CXX = sim_main.cpp kernel.cpp MemBox.cpp testcase.cpp cta_sche_wrapper.cpp
SRC_C = log.c
OBJ_C = $(SRC_C:%.c=$(DIR_OBJ)/%.o)
VERILATOR_INPUT = $(SRC_V) $(SRC_CXX) $(notdir $(OBJ_C))

VERILATED_DIR = $(DIR_OBJ)
VERILATED_CXXFILES = $(shell find $(VERILATED_DIR) -name "*.cpp")
VERILATED_HFILES = $(shell find $(VERILATED_DIR) -name "*.h")
VERILATED_MKFILE = $(VERILATED_DIR)/Vdut.mk
VERILATED_FILES = $(VERILATED_MKFILE) $(VERILATED_CXXFILES) $(VERILATED_HFILES)

######################################################################
default: verilator

$(SRC_V): $(SRC_SCALA)
	cd .. && ./mill ventus[6.4.0].runMain top.emitVerilog
	mv GPGPU_SimTop.v $(SRC_V)
	sed -i "1i\`define PRINTF_COND 0" $(SRC_V)

verilog: $(SRC_V)

$(DIR_OBJ)/%.o: %.c
	@mkdir -p $(DIR_OBJ)
	gcc -c -o $@ $<

verilator: verilog $(OBJ_C)
	@mkdir -p $(DIR_OBJ)
	$(VERILATOR) $(VERILATOR_FLAGS) $(VERILATOR_INPUT)

run: verilator
	@-rm -f $(VERILATED_DIR)/Vdut.fst
	@-rm -rf logs
	@mkdir -p logs
	$(VERILATED_DIR)/Vdut

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
	-rm -rf $(VERILATED_DIR)

clean_verilog: clean_cpp
	-rm -f dut.v

clean_all: clean_verilog

.PHONY: clean clean_cpp clean_verilog clean_all

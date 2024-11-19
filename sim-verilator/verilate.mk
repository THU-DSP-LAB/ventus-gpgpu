# Check for sanity to avoid later confusion
ifneq ($(words $(CURDIR)),1)
 $(error Unsupported: GNU Make cannot build in directories containing spaces, build elsewhere: '$(CURDIR)')
endif

RELEASE ?= 0

#=====================================================================
# Helpers
#=====================================================================

# Choose the smaller one between two numbers
define MIN_FUNC
$(strip $(shell if [ $(1) -lt $(2) ]; then echo $(1); else echo $(2); fi))
endef

#=====================================================================
# Find verilator executable
#=====================================================================

# If $VERILATOR_ROOT isn't in the environment, we assume it is part of a
# package install, and verilator is in your path. Otherwise find the
# binary relative to $VERILATOR_ROOT (such as when inside the git sources).
ifeq ($(VERILATOR_ROOT),)
VLIB_VERILATOR = verilator
VLIB_VERILATOR_COVERAGE = verilator_coverage
else
export VERILATOR_ROOT
VLIB_VERILATOR = $(VERILATOR_ROOT)/bin/verilator
VLIB_VERILATOR_COVERAGE = $(VERILATOR_ROOT)/bin/verilator_coverage
endif

#=====================================================================
# Source file list and build directories
#=====================================================================

VLIB_DIR_SCALA = ../ventus/src
VLIB_DIR_BUILD = build/libVentusRTL
VLIB_DIR_BUILDOBJ_DEBUG = $(VLIB_DIR_BUILD)/debug
VLIB_DIR_BUILDOBJ_RELEASE = $(VLIB_DIR_BUILD)/release
ifeq ($(RELEASE),1)
VLIB_DIR_BUILDOBJ = $(VLIB_DIR_BUILDOBJ_RELEASE)
else
VLIB_DIR_BUILDOBJ = $(VLIB_DIR_BUILDOBJ_DEBUG)
endif

VLIB_SRC_SCALA = $(shell find $(VLIB_DIR_SCALA) -name "*.scala")
VLIB_SRC_V = dut.v
VLIB_SRC_CXX_EXPORT = ventus_rtlsim.cpp # API in these files will be exported to shared library
VLIB_SRC_CXX = kernel.cpp cta_sche_wrapper.cpp ventus_rtlsim_impl.cpp $(VLIB_SRC_CXX_EXPORT)
VLIB_SRC_CXX_ABSPATH = $(abspath $(VLIB_SRC_CXX))
VLIB_VERILATOR_INPUT = $(VLIB_SRC_V) $(VLIB_SRC_CXX_ABSPATH)
VLIB_VERILATOR_OUTPUT = $(VLIB_DIR_BUILDOBJ)/libVdut.a
#VLIB_VERILATOR_OUTPUT = $(VLIB_DIR_BUILDOBJ)/libVdut.a $(VLIB_DIR_BUILDOBJ)/libverilated.a

VLIB_TARGET_NAME = VentusRTL
VLIB_TARGET_PATH = $(VLIB_DIR_BUILDOBJ)
VLIB_TARGET = $(VLIB_TARGET_PATH)/lib$(VLIB_TARGET_NAME).so

# API in these objects will be exported to shared library
VLIB_OBJ_EXPORT = $(VLIB_SRC_CXX_EXPORT:%.cpp=$(VLIB_DIR_BUILDOBJ)/%.o)

#=====================================================================
# Verilator and toolchain flags
#=====================================================================

# Verilated model parallelism config
VLIB_NPROC_CPU = $(shell nproc)
VLIB_NPROC_DUT = 11
VLIB_NPROC_SIM = $(call MIN_FUNC, $(VLIB_NPROC_CPU), $(VLIB_NPROC_DUT))
VLIB_NPROC_TRACE_FST = $(call MIN_FUNC, $(VLIB_NPROC_SIM), 2)

# Generate C++ in executable form
VLIB_VERILATOR_FLAGS += -cc --build
VLIB_VERILATOR_FLAGS += -MMD
# How to deal with verilog value 'x' and 'z'
ifeq ($(RELEASE),1)
VLIB_VERILATOR_FLAGS += -x-assign fast -x-initial fast
else
VLIB_VERILATOR_FLAGS += -x-assign unique -x-initial unique
endif
# Warn abount lint issues; may not want this on less solid designs
#VLIB_VERILATOR_FLAGS += -Wall
VLIB_VERILATOR_FLAGS += -Wno-WIDTHEXPAND
# Make waveforms
VLIB_VERILATOR_FLAGS += --trace-fst
# Check SystemVerilog assertions
VLIB_VERILATOR_FLAGS += --assert
# Generate coverage analysis
#VLIB_VERILATOR_FLAGS += --coverage
# Run Verilator in debug mode
#VLIB_VERILATOR_FLAGS += --debug
# Add this trace to get a backtrace in gdb
#VLIB_VERILATOR_FLAGS += --gdbbt

ifeq ($(RELEASE),1)
VLIB_CFLAGS += -O2
else
VLIB_CFLAGS += -g -O0
endif
VLIB_CFLAGS += -fPIC -fvisibility=hidden
VLIB_CXXFLAGS += $(VLIB_CFLAGS)
VLIB_CXXFLAGS += -std=c++20
VLIB_LDFLAGS += -lc
VLIB_LDFLAGS += -fuse-ld=mold

VLIB_VERILATOR_FLAGS += --threads $(VLIB_NPROC_SIM)
VLIB_VERILATOR_FLAGS += --trace-threads $(VLIB_NPROC_TRACE_FST)
VLIB_VERILATOR_FLAGS += -j $(VLIB_NPROC_CPU)
VLIB_VERILATOR_FLAGS += -CFLAGS "$(VLIB_CXXFLAGS)"
VLIB_VERILATOR_FLAGS += -LDFLAGS "$(VLIB_LDFLAGS)"
VLIB_VERILATOR_FLAGS += --prefix Vdut -Mdir $(VLIB_DIR_BUILDOBJ)

#=====================================================================
# Build rules and targets
#=====================================================================

default: lib

#$(VLIB_DIR_BUILDOBJ)/log.o: log.c
#	@mkdir -p $(VLIB_DIR_BUILDOBJ)
#	gcc -c $(VLIB_CFLAGS) -o $@ $<

$(VLIB_SRC_V): $(VLIB_SRC_SCALA)
	cd .. && ./mill ventus[6.4.0].runMain top.emitVerilog
	mv GPGPU_SimTop.v $(VLIB_SRC_V)
	sed -i "1i\`define PRINTF_COND 0" $(VLIB_SRC_V)

verilog: $(VLIB_SRC_V)

verilate: $(VLIB_SRC_V) $(VLIB_SRC_CXX) #$(VLIB_DIR_BUILDOBJ)/log.o
	@mkdir -p $(VLIB_DIR_BUILDOBJ)
	$(VLIB_VERILATOR) $(VLIB_VERILATOR_FLAGS) $(VLIB_VERILATOR_INPUT)
#	$(VLIB_VERILATOR) $(VLIB_VERILATOR_FLAGS) $(VLIB_VERILATOR_INPUT) log.o

$(VLIB_VERILATOR_OUTPUT): $(VLIB_SRC_V) $(VLIB_SRC_CXX) #$(VLIB_DIR_BUILDOBJ)/log.o
	@mkdir -p $(VLIB_DIR_BUILDOBJ)
	$(VLIB_VERILATOR) $(VLIB_VERILATOR_FLAGS) $(VLIB_VERILATOR_INPUT)
#	$(VLIB_VERILATOR) $(VLIB_VERILATOR_FLAGS) $(VLIB_VERILATOR_INPUT) log.o

$(VLIB_TARGET): $(VLIB_VERILATOR_OUTPUT)
	g++ $(VLIB_CXXFLAGS) $(VLIB_LDFLAGS) -shared -o $@ \
	  $(VLIB_OBJ_EXPORT) \
	  $(VLIB_DIR_BUILDOBJ)/libVdut.a $(VLIB_DIR_BUILDOBJ)/libverilated.a \
	  -lspdlog -lfmt -pthread -lpthread -lz -latomic  
	ln -sf $(abspath $(VLIB_TARGET)) $(VLIB_DIR_BUILD)/libVentusRTL.so

lib: $(VLIB_TARGET)

.PHONY: verilog verilate lib

#=====================================================================
# Other targets
#=====================================================================

info-verilator:
	$(VLIB_VERILATOR) -V

clean-lib:
	-rm -f $(VLIB_DIR_BUILDOBJ_DEBUG)/*.a $(VLIB_DIR_BUILDOBJ_DEBUG)/*.d $(VLIB_DIR_BUILDOBJ_DEBUG)/*.o $(VLIB_DIR_BUILDOBJ_DEBUG)/*.so
	-rm -f $(VLIB_DIR_BUILDOBJ_RELEASE)/*.a $(VLIB_DIR_BUILDOBJ_RELEASE)/*.d $(VLIB_DIR_BUILDOBJ_RELEASE)/*.o $(VLIB_DIR_BUILDOBJ_RELEASE)/*.so
	-rm -f $(VLIB_DIR_BUILD)/*.so

clean-verilated: 
	-rm -rf $(VLIB_DIR_BUILD)

clean-verilog: clean-verilated
	-rm -f $(VLIB_SRC_V)

.PHONY: clean-lib clean-verilated clean-verilog info-verilator

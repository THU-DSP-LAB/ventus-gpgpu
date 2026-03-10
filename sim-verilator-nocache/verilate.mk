# Check for sanity to avoid later confusion
ifneq ($(words $(CURDIR)),1)
 $(error Unsupported: GNU Make cannot build in directories containing spaces, build elsewhere: '$(CURDIR)')
endif

RELEASE ?= 0
PREFIX ?= $(CURDIR)/install

#=====================================================================
# Helpers
#=====================================================================

# Choose the smaller one between two numbers
define MIN_FUNC
$(strip $(shell if [ $(1) -lt $(2) ]; then echo $(1); else echo $(2); fi))
endef

#=====================================================================
# Toolchain check
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

CCACHE = $(shell which ccache)
ifeq ($(CCACHE),)
CC  = gcc
CXX = g++
else
CC  = ccache gcc
CXX = ccache g++
endif
MOLD = $(shell which mold)

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
SINGLE_SM_ROOT ?= singleSM
SINGLE_SM_PREFIX ?= singleSM
NUM_WARP ?= 8
NUM_THREAD ?= 32
NUM_BLOCK ?= $(NUM_WARP)
SHAREDMEM_CAPACITY_BYTES ?= 131072
SHAREDMEM_NBANKS ?= $(NUM_THREAD)
SHAREDMEM_BANDWIDTH_BITS = $(shell expr $(SHAREDMEM_NBANKS) \* 32)
GEN_DIR_NAME = $(SINGLE_SM_PREFIX)_warp$(NUM_WARP)_thread$(NUM_THREAD)_smem$(SHAREDMEM_CAPACITY_BYTES)B_smbank$(SHAREDMEM_NBANKS)_smbw$(SHAREDMEM_BANDWIDTH_BITS)
GEN_DIR = $(SINGLE_SM_ROOT)/$(GEN_DIR_NAME)
VLIB_SRC_V = $(GEN_DIR)/dut.v
VLIB_SRC_FIR = $(GEN_DIR)/GPGPU_top_nocache.fir
VLIB_PARAMS_JSON = $(GEN_DIR)/parameters.json
VLIB_SRC_CXX_EXPORT = ventus_rtlsim.cpp # API in these files will be exported to shared library
VLIB_SRC_CXX = kernel.cpp physical_mem.cpp cta_sche_wrapper.cpp ventus_rtlsim_impl.cpp rtl_parameters.cpp $(VLIB_SRC_CXX_EXPORT)
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
VLIB_NPROC_DUT = 8 # Depends on RTL circuit size, just try and find a verilator-allowed largest number
VLIB_NPROC_SIM = $(call MIN_FUNC, $(VLIB_NPROC_CPU), $(VLIB_NPROC_DUT))
VLIB_NPROC_TRACE_FST = $(call MIN_FUNC, $(VLIB_NPROC_SIM), 2)

# Generate C++ in executable form
VLIB_VERILATOR_FLAGS += -cc --build
VLIB_VERILATOR_FLAGS += -MMD
VLIB_VERILATOR_FLAGS += --error-limit 100
# How to deal with verilog value 'x' and 'z'
ifeq ($(RELEASE),1)
VLIB_VERILATOR_FLAGS += --x-assign fast --x-initial unique
else
VLIB_VERILATOR_FLAGS += --x-assign unique --x-initial unique
endif
# Warn about lint issues; may not want this on less solid designs
#VLIB_VERILATOR_FLAGS += -Wall
VLIB_VERILATOR_FLAGS += -Wno-WIDTHEXPAND
VLIB_VERILATOR_FLAGS += -Wno-WIDTHTRUNC
# Define macros for Verilog
# random init
VLIB_VERILATOR_FLAGS += -DPRINTF_COND=1
VLIB_VERILATOR_FLAGS += -DRANDOMIZE
VLIB_VERILATOR_FLAGS += -DRANDOMIZE_MEM_INIT
VLIB_VERILATOR_FLAGS += -DRANDOMIZE_REG_INIT
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
VLIB_CFLAGS += -O2 -fvisibility=hidden
else
VLIB_CFLAGS += -g -O0
endif
VLIB_CFLAGS += -fPIC
VLIB_CXXFLAGS += $(VLIB_CFLAGS)
VLIB_CXXFLAGS += -std=c++20
VLIB_CXXFLAGS += -DSPDLOG_ACTIVE_LEVEL=SPDLOG_LEVEL_TRACE
VLIB_LDFLAGS += -lc
ifeq ($(MOLD),1)
VLIB_LDFLAGS += -fuse-ld=mold
endif

VLIB_VERILATOR_FLAGS += --threads $(VLIB_NPROC_SIM)
VLIB_VERILATOR_FLAGS += --trace-threads $(VLIB_NPROC_TRACE_FST)
VLIB_VERILATOR_FLAGS += -j $(VLIB_NPROC_CPU)
VLIB_VERILATOR_FLAGS += -CFLAGS "$(VLIB_CXXFLAGS)"
VLIB_VERILATOR_FLAGS += -LDFLAGS "$(VLIB_LDFLAGS)"
VLIB_VERILATOR_FLAGS += --prefix Vdut -Mdir $(VLIB_DIR_BUILDOBJ)

GEN_ARGS ?=
VLIB_GEN_ARGS = --output-root sim-verilator-nocache/$(SINGLE_SM_ROOT) --dir-prefix $(SINGLE_SM_PREFIX) --num-warp $(NUM_WARP) --num-thread $(NUM_THREAD) --num-block $(NUM_BLOCK) --sharedmem-capacity-bytes $(SHAREDMEM_CAPACITY_BYTES) --sharedmem-nbanks $(SHAREDMEM_NBANKS) $(GEN_ARGS)

#=====================================================================
# Build rules and targets
#=====================================================================

default: lib

$(VLIB_SRC_V) $(VLIB_PARAMS_JSON) &: $(VLIB_SRC_SCALA)
	@mkdir -p $(GEN_DIR)
	cd .. && ./mill ventus[6.4.0].runMain top.SingleSMNoCacheGen $(VLIB_GEN_ARGS)
	firtool --verilog $(VLIB_SRC_FIR) -o $(VLIB_SRC_V)

rtl_parameters.cpp: $(VLIB_PARAMS_JSON) json2cpp.py
	python3 json2cpp.py $(VLIB_PARAMS_JSON) rtl_parameters.cpp

verilog: $(VLIB_SRC_V)

verilate: $(VLIB_SRC_V) $(VLIB_SRC_CXX)
	@mkdir -p $(VLIB_DIR_BUILDOBJ)
	+$(VLIB_VERILATOR) $(VLIB_VERILATOR_FLAGS) $(VLIB_VERILATOR_INPUT)

$(VLIB_VERILATOR_OUTPUT): $(VLIB_SRC_V) $(VLIB_SRC_CXX)
	@mkdir -p $(VLIB_DIR_BUILDOBJ)
	+$(VLIB_VERILATOR) $(VLIB_VERILATOR_FLAGS) $(VLIB_VERILATOR_INPUT)

$(VLIB_TARGET): $(VLIB_VERILATOR_OUTPUT)
	$(CXX) $(VLIB_CXXFLAGS) $(VLIB_LDFLAGS) -shared -o $@ \
	  $(VLIB_OBJ_EXPORT) \
	  $(VLIB_DIR_BUILDOBJ)/libVdut.a $(VLIB_DIR_BUILDOBJ)/libverilated.a \
	  -lspdlog -lfmt -pthread -lpthread -lz -latomic  
	ln -sf $(abspath $(VLIB_TARGET)) $(VLIB_DIR_BUILD)/lib$(VLIB_TARGET_NAME).so

lib: $(VLIB_TARGET)

.PHONY: verilog verilate lib

#=====================================================================
# Other targets
#=====================================================================

info-verilator:
	$(VLIB_VERILATOR) -V

install: $(VLIB_TARGET)
	install -d $(PREFIX)/lib
	install -m 644 $(VLIB_TARGET) $(PREFIX)/lib/
	install -m 644 $(VLIB_TARGET) $(PREFIX)/lib/lib$(VLIB_TARGET_NAME)-nocache.so
	install -d $(PREFIX)/include
	install -m 644 ventus_rtlsim.h $(PREFIX)/include/

clean-lib:
	-rm -f $(VLIB_DIR_BUILDOBJ_DEBUG)/*.a $(VLIB_DIR_BUILDOBJ_DEBUG)/*.o $(VLIB_DIR_BUILDOBJ_DEBUG)/*.so
	-rm -f $(VLIB_DIR_BUILDOBJ_RELEASE)/*.a $(VLIB_DIR_BUILDOBJ_RELEASE)/*.o $(VLIB_DIR_BUILDOBJ_RELEASE)/*.so
	-rm -f $(VLIB_DIR_BUILD)/*.so

clean-lib-dep: clean-lib
	-rm -f $(VLIB_DIR_BUILDOBJ_DEBUG)/*.d
	-rm -f $(VLIB_DIR_BUILDOBJ_RELEASE)/*.d

clean-verilated: 
	-rm -rf $(VLIB_DIR_BUILD)

clean-verilog: clean-verilated
	-rm -rf $(SINGLE_SM_ROOT)

.PHONY: clean-lib clean-lib-dep clean-verilated clean-verilog info-verilator install

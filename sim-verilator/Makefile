# Check for sanity to avoid later confusion
ifneq ($(words $(CURDIR)),1)
 $(error Unsupported: GNU Make cannot build in directories containing spaces, build elsewhere: '$(CURDIR)')
endif
export MAKEFLAGS += +r

RELEASE ?= 0

#=====================================================================
# Check toolchain
#=====================================================================

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

SRC_CXX = sim_main.cpp cmdarg.cpp kernel.cpp

#=====================================================================
# Include Ventus RTL library build rules
#=====================================================================

include verilate.mk

DIR_BUILD = build/driver_example
DIR_BUILDOBJ = $(DIR_BUILD)/$(VLIB_VARIANT_KEY)
OBJ_CXX = $(SRC_CXX:%.cpp=$(DIR_BUILDOBJ)/%.o)
DEP_CXX = $(SRC_CXX:%.cpp=$(DIR_BUILDOBJ)/%.d)

APP = $(DIR_BUILDOBJ)/sim-VentusRTL
METADATA_PARSER_TEST = build/tests/common/metadata_parser_test
FORK_SNAPSHOT_TEST = build/tests/$(VLIB_VARIANT_KEY)/fork_snapshot_test
PERSISTENT_STATE_TEST = build/tests/$(VLIB_VARIANT_KEY)/persistent_state_test
PMEM_REGION_TEST = build/tests/$(VLIB_VARIANT_KEY)/pmem_region_test

# Extend verilate.mk's default target with the matching host variant.
default: $(APP)

#=====================================================================
# Toolchain flags
#=====================================================================

ifeq ($(RELEASE),1)
CXXFLAGS += -O2
else
CXXFLAGS += -g -O0
endif
CXXFLAGS += -std=c++20 -MMD -MP

ifeq ($(RELEASE),1)
LDFLAGS += -fuse-ld=mold
endif
# shared library & link path
LDFLAGS += -lspdlog -lfmt
LDFLAGS += -L$(VLIB_TARGET_PATH) -l$(VLIB_TARGET_NAME) -Wl,-rpath=$(abspath $(VLIB_TARGET_PATH))

#=====================================================================
# Build rules and targets
#=====================================================================

-include $(DEP_CXX)
$(DIR_BUILDOBJ)/%.o: %.cpp
	@mkdir -p $(DIR_BUILDOBJ)
	$(CXX) -c $(CXXFLAGS) -o $@ $<

$(APP): $(VLIB_TARGET) $(OBJ_CXX)
	@mkdir -p $(DIR_BUILDOBJ)
	$(CXX) -o $@ $(OBJ_CXX) $(LDFLAGS)

run: $(APP)
	@echo
	-rm -f logs/ventus_rtlsim.log
	$(APP)
	@echo

$(METADATA_PARSER_TEST): tests/metadata_parser_test.cpp kernel.cpp kernel.hpp ventus_rtlsim.h
	@mkdir -p $(dir $@)
	$(CXX) -std=c++20 -g -O1 -fsanitize=address,undefined -fno-omit-frame-pointer \
	  -I. tests/metadata_parser_test.cpp kernel.cpp -lspdlog -lfmt -o $@

test-metadata-parser: $(METADATA_PARSER_TEST)
	ASAN_OPTIONS=detect_leaks=1 $(METADATA_PARSER_TEST)

$(FORK_SNAPSHOT_TEST): tests/fork_snapshot_test.cpp ventus_rtlsim.h $(VLIB_TARGET)
	@mkdir -p $(dir $@)
	$(CXX) -std=c++20 -g -O1 -I. tests/fork_snapshot_test.cpp \
	  -L$(VLIB_TARGET_PATH) -l$(VLIB_TARGET_NAME) \
	  -Wl,-rpath=$(abspath $(VLIB_TARGET_PATH)) -o $@

test-fork-snapshot: $(FORK_SNAPSHOT_TEST)
	$(FORK_SNAPSHOT_TEST) invalid
	$(FORK_SNAPSHOT_TEST) success
	$(FORK_SNAPSHOT_TEST) mismatch

$(PERSISTENT_STATE_TEST): tests/persistent_state_test.cpp ventus_rtlsim.h $(VLIB_TARGET)
	@mkdir -p $(dir $@)
	$(CXX) -std=c++20 -g -O1 -I. tests/persistent_state_test.cpp \
	  -L$(VLIB_TARGET_PATH) -l$(VLIB_TARGET_NAME) \
	  -Wl,-rpath=$(abspath $(VLIB_TARGET_PATH)) -o $@

test-persistent-state: $(PERSISTENT_STATE_TEST)
	$(PERSISTENT_STATE_TEST) save
	$(PERSISTENT_STATE_TEST) restore
ifeq ($(TRACE),1)
	$(PERSISTENT_STATE_TEST) restore-waveform
endif
	$(PERSISTENT_STATE_TEST) corrupt
ifneq ($(TRACE),1)
	$(PERSISTENT_STATE_TEST) no-trace-config
endif

print-build-identity: $(VLIB_MODEL_IDENTITY_JSON)
	@cat $(VLIB_MODEL_IDENTITY_JSON)

$(PMEM_REGION_TEST): tests/pmem_region_test.cpp standalone_pmem_regions.hpp ventus_rtlsim.h $(VLIB_TARGET)
	@mkdir -p $(dir $@)
	$(CXX) -std=c++20 -g -O1 -I. tests/pmem_region_test.cpp \
	  -L$(VLIB_TARGET_PATH) -l$(VLIB_TARGET_NAME) \
	  -Wl,-rpath=$(abspath $(VLIB_TARGET_PATH)) -o $@

test-pmem-region: $(PMEM_REGION_TEST)
	$(PMEM_REGION_TEST)

gdb: $(APP)
	@echo
	-rm -f logs/ventus_rtlsim.log
	gdb --tui $(APP)
	@echo

.PHONY: lib run test-metadata-parser test-fork-snapshot test-persistent-state test-pmem-region print-build-identity gdb

#=====================================================================
# Other targets
#=====================================================================

clean:
	-rm -rf $(DIR_BUILD)

clean-all: clean clean-verilog

.PHONY: clean clean-all

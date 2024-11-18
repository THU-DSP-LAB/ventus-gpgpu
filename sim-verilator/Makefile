# Check for sanity to avoid later confusion
ifneq ($(words $(CURDIR)),1)
 $(error Unsupported: GNU Make cannot build in directories containing spaces, build elsewhere: '$(CURDIR)')
endif

RELEASE ?= 0

#=====================================================================
# Source file list and build directories
#=====================================================================
DIR_BUILD = build/driver_example
DIR_BUILDOBJ_DEBUG = $(DIR_BUILD)/debug
DIR_BUILDOBJ_RELEASE = $(DIR_BUILD)/release
ifeq ($(RELEASE),1)
DIR_BUILDOBJ = $(DIR_BUILDOBJ_RELEASE)
else
DIR_BUILDOBJ = $(DIR_BUILDOBJ_DEBUG)
endif

SRC_CXX = sim_main.cpp sim_main2.cpp cmdarg.cpp kernel.cpp
OBJ_CXX = $(SRC_CXX:%.cpp=$(DIR_BUILDOBJ)/%.o)
DEP_CXX = $(SRC_CXX:%.cpp=$(DIR_BUILDOBJ)/%.d)

APP = $(DIR_BUILDOBJ)/sim-VentusRTL

#=====================================================================
# Include Ventus RTL library build rules
#=====================================================================

# default build target should be set by this Makefile
default: $(APP)

include verilate.mk

#=====================================================================
# Toolchain flags
#=====================================================================

ifeq ($(RELEASE),1)
CXXFLAGS += -O2
else
CXXFLAGS += -g -O0
endif
CXXFLAGS += -std=c++20 -MMD -MP

LDFLAGS += -fuse-ld=mold
# shared library & link path
LDFLAGS += -L$(VLIB_TARGET_PATH) -l$(VLIB_TARGET_NAME) -Wl,-rpath=$(abspath $(VLIB_TARGET_PATH))

#=====================================================================
# Build rules and targets
#=====================================================================

-include $(DEP_CXX)
$(DIR_BUILDOBJ)/%.o: %.cpp
	@mkdir -p $(DIR_BUILDOBJ)
	g++ -c $(CXXFLAGS) -o $@ $<

$(APP): $(VLIB_TARGET) $(OBJ_CXX)
	@mkdir -p $(DIR_BUILDOBJ)
	g++ -o $@ $(OBJ_CXX) $(LDFLAGS)

run: $(APP)
	@echo
	$(APP)

gdb: $(APP)
	@echo
	gdb --tui $(APP)

.PHONY: lib run gdb

#=====================================================================
# Other targets
#=====================================================================
clean:
	-rm -rf $(DIR_BUILD)

clean-all: clean clean-verilog

.PHONY: clean clean-all
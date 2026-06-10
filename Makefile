SHELL := /bin/bash

RELEASE ?= 0
PREFIX ?= $(CURDIR)/install
GVM_REF_DIR ?= $(PREFIX)/lib
GVM_TRACE ?= 1
BACKEND_BUILD_RETRIES ?= 3

define RUN_BACKEND_MAKE
+@attempt=1; \
while true; do \
  echo "$@: attempt $$attempt/$(BACKEND_BUILD_RETRIES): $(MAKE) $(1)"; \
  $(MAKE) $(1); \
  status=$$?; \
  if [ "$$status" -eq 0 ]; then \
    exit 0; \
  fi; \
  if [ "$$attempt" -ge "$(BACKEND_BUILD_RETRIES)" ]; then \
    echo "$@: failed after $$attempt attempts, last status $$status"; \
    exit $$status; \
  fi; \
  echo "$@: attempt $$attempt failed with status $$status, retrying"; \
  $(2); \
  attempt=$$((attempt + 1)); \
done
endef

init:
	git submodule update --init --recursive --progress
	git config blame.ignoreRevsFile .git-blame-ignore-revs

bump:
	git submodule foreach git stash
	git submodule update --remote
	git add dependencies

bsp:
	./mill -i mill.bsp.BSP/install

idea:
	./mill -i -j 0 mill.idea.GenIdea/idea

compile:
	./mill -i -j 0 __.compile

test:
	mkdir -p test_run_dir
	./mill -i ventus[6.4.0].tests.testOnly play.AdvancedTest 2>&1 | tee test_run_dir/test.log
	# ./mill -i ventus[6.4.0].tests.testOnly play.AsidTests 2>&1 | tee test_run_dir/test.log 	#for asid test, provide two asid test case

verilog:
	./mill ventus[6.4.0].run

fpga-verilog:
	./mill ventus[6.4.0].runMain circt.stage.ChiselMain --module top.GPGPU_axi_adapter_top --target chirrtl --target-dir gen_fpga_verilog/
	cd gen_fpga_verilog/ && firtool --split-verilog --repl-seq-mem --repl-seq-mem-file=mem.conf -o . GPGPU_axi_adapter_top.fir
	./scripts/gen_sep_mem.sh ./scripts/vlsi_mem_gen gen_fpga_verilog/mem.conf gen_fpga_verilog/

clean:
	rm -rf out/ test_run_dir/ .idea/

clean-git:
	git clean -fd

rtlsim-withcache:
	$(call RUN_BACKEND_MAKE,-C sim-verilator lib RELEASE=$(RELEASE) PREFIX=$(PREFIX),rm -rf sim-verilator/build/libVentusRTL sim-verilator/build/generated/rtl sim-verilator/dut.v sim-verilator/parameters.json sim-verilator/rtl_parameters.cpp)

rtlsim-nocache:
	$(call RUN_BACKEND_MAKE,-C sim-verilator-nocache lib RELEASE=$(RELEASE) PREFIX=$(PREFIX),rm -rf sim-verilator-nocache/build/libVentusRTL sim-verilator-nocache/build/generated/rtl sim-verilator-nocache/dut.v sim-verilator-nocache/parameters.json sim-verilator-nocache/rtl_parameters.cpp)

gvm-withcache:
	$(call RUN_BACKEND_MAKE,-C sim-verilator -f gvm.mk lib RELEASE=$(RELEASE) PREFIX=$(PREFIX) GVM_TRACE=$(GVM_TRACE) GVM_REF_DIR=$(GVM_REF_DIR),rm -rf sim-verilator/build/libVentusGVM sim-verilator/build/generated/gvm sim-verilator/verilog-out)

gvm-nocache:
	$(call RUN_BACKEND_MAKE,-C sim-verilator-nocache -f gvm.mk lib RELEASE=$(RELEASE) PREFIX=$(PREFIX) GVM_TRACE=$(GVM_TRACE) GVM_REF_DIR=$(GVM_REF_DIR),rm -rf sim-verilator-nocache/build/libVentusGVM sim-verilator-nocache/build/generated/gvm sim-verilator-nocache/verilog-out)

rtlsim-gvm-build: rtlsim-withcache rtlsim-nocache gvm-withcache gvm-nocache

rtlsim-gvm-install: rtlsim-gvm-build
	+$(MAKE) -C sim-verilator install RELEASE=$(RELEASE) PREFIX=$(PREFIX)
	+$(MAKE) -C sim-verilator-nocache install RELEASE=$(RELEASE) PREFIX=$(PREFIX)
	+$(MAKE) -C sim-verilator -f gvm.mk install RELEASE=$(RELEASE) PREFIX=$(PREFIX) GVM_REF_DIR=$(GVM_REF_DIR)
	+$(MAKE) -C sim-verilator-nocache -f gvm.mk install RELEASE=$(RELEASE) PREFIX=$(PREFIX) GVM_REF_DIR=$(GVM_REF_DIR)

.PHONY: init bump bsp idea compile test verilog fpga-verilog clean clean-git
.PHONY: rtlsim-withcache rtlsim-nocache gvm-withcache gvm-nocache rtlsim-gvm-build rtlsim-gvm-install

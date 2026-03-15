SHELL := /bin/bash

NUM_WARP ?= 8
NUM_THREAD ?= 32
NUM_BANK ?= 4
NUM_FETCH ?= 2
SIZE_IBUFFER ?= 2
NUM_BLOCK ?= 8
DCACHE_NSETS ?= 256
DCACHE_NWAYS ?= 2
DCACHE_BLOCK_WORDS ?= 32
DCACHE_MSHR_ENTRY ?= 4
DCACHE_MSHR_SUB_ENTRY ?= 2
DCACHE_WSHR_ENTRY ?= 4
SHAREDMEM_CAPACITY_BYTES ?= 131072
SHAREDMEM_NBANKS ?= $(NUM_THREAD)
LSU_NUM_ENTRY_EACH_WARP ?= 4
SM_OUTPUT_ROOT ?= gen_sm_verilog
SM_DIR_PREFIX ?=
SM_TOP_NAME := SM
SM_SHAREDMEM_PIPE_CUT ?= 0
SM_PIPE_BOOL := $(if $(filter 1 true TRUE yes YES,$(SM_SHAREDMEM_PIPE_CUT)),true,false)
SM_DCACHE_PIPE_CUT ?= 0
SM_DCACHE_PIPE_BOOL := $(if $(filter 1 true TRUE yes YES,$(SM_DCACHE_PIPE_CUT)),true,false)
SM_PIPE_TAG := $(if $(filter 1 true TRUE yes YES,$(SM_SHAREDMEM_PIPE_CUT)),_pipe1,)$(if $(filter 1 true TRUE yes YES,$(SM_DCACHE_PIPE_CUT)),_dcachepipe,)
SM_SHAREDMEM_BW_BITS := $(shell expr $(SHAREDMEM_NBANKS) \* 32)
SM_DIR_SUFFIX := warp$(NUM_WARP)_thread$(NUM_THREAD)_smem$(SHAREDMEM_CAPACITY_BYTES)B_smbank$(SHAREDMEM_NBANKS)_smbw$(SM_SHAREDMEM_BW_BITS)
SM_DIR_NAME := $(SM_TOP_NAME)$(SM_PIPE_TAG)$(if $(SM_DIR_PREFIX),_$(SM_DIR_PREFIX),)_$(SM_DIR_SUFFIX)
SM_TARGET_DIR ?= $(SM_OUTPUT_ROOT)/$(SM_DIR_NAME)

LSU_SHAREDMEM_OUTPUT_ROOT ?= gen_lsu_sharedmem_verilog
LSU_SHAREDMEM_DIR_PREFIX ?=
LSU_SHAREDMEM_TOP_NAME := LsuSharedMemTop
LSU_SHAREDMEM_PIPE_CUT ?= 0
LSU_SHAREDMEM_PIPE_TAG := $(if $(filter 1 true TRUE yes YES,$(LSU_SHAREDMEM_PIPE_CUT)),_pipe1,)
LSU_SHAREDMEM_PIPE_BOOL := $(if $(filter 1 true TRUE yes YES,$(LSU_SHAREDMEM_PIPE_CUT)),true,false)
LSU_SHAREDMEM_DIR_NAME := $(LSU_SHAREDMEM_TOP_NAME)$(LSU_SHAREDMEM_PIPE_TAG)$(if $(LSU_SHAREDMEM_DIR_PREFIX),_$(LSU_SHAREDMEM_DIR_PREFIX),)_$(SM_DIR_SUFFIX)
LSU_SHAREDMEM_TARGET_DIR ?= $(LSU_SHAREDMEM_OUTPUT_ROOT)/$(LSU_SHAREDMEM_DIR_NAME)

DC_RTL_BASE ?= /home/mmy/work/ventus/dc_compile/rtl1
BANK_LIST ?= 8 16 32 64 128
SM_DC_TARGET_DIR ?= $(DC_RTL_BASE)/$(SM_TOP_NAME)/$(SM_DIR_NAME)
LSU_SHAREDMEM_DC_TARGET_DIR ?= $(DC_RTL_BASE)/$(LSU_SHAREDMEM_TOP_NAME)/$(LSU_SHAREDMEM_DIR_NAME)

init:
	git submodule update --init --recursive --progress

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

sm-verilog:
	./mill -i ventus[6.4.0].runMain top.SingleSMGen \
		--target-dir $(SM_TARGET_DIR) \
		--num-warp $(NUM_WARP) \
		--num-thread $(NUM_THREAD) \
		--num-bank $(NUM_BANK) \
		--num-fetch $(NUM_FETCH) \
		--size-ibuffer $(SIZE_IBUFFER) \
		--num-block $(NUM_BLOCK) \
		--dcache-nsets $(DCACHE_NSETS) \
		--dcache-nways $(DCACHE_NWAYS) \
		--dcache-block-words $(DCACHE_BLOCK_WORDS) \
		--dcache-mshr-entry $(DCACHE_MSHR_ENTRY) \
		--dcache-mshr-sub-entry $(DCACHE_MSHR_SUB_ENTRY) \
		--dcache-wshr-entry $(DCACHE_WSHR_ENTRY) \
		--sharedmem-nbanks $(SHAREDMEM_NBANKS) \
		--sharedmem-capacity-bytes $(SHAREDMEM_CAPACITY_BYTES) \
		--lsu-num-entry-each-warp $(LSU_NUM_ENTRY_EACH_WARP) \
		--lsu-sharedmem-pipe-cut $(SM_PIPE_BOOL) \
		--lsu-dcache-pipe-cut $(SM_DCACHE_PIPE_BOOL) \
		$(GEN_ARGS)
	cd $(SM_TARGET_DIR) && firtool --split-verilog --repl-seq-mem --repl-seq-mem-file=mem.conf -o . SM.fir
	./scripts/gen_sep_mem.sh ./scripts/vlsi_mem_gen $(SM_TARGET_DIR)/mem.conf $(SM_TARGET_DIR)/
	@echo "sm-verilog output dir: $(SM_TARGET_DIR)"

lsu-sharedmem-verilog:
	./mill -i ventus[6.4.0].runMain top.LsuSharedMemGen \
		--target-dir $(LSU_SHAREDMEM_TARGET_DIR) \
		--num-warp $(NUM_WARP) \
		--num-thread $(NUM_THREAD) \
		--num-block $(NUM_BLOCK) \
		--dcache-nsets $(DCACHE_NSETS) \
		--dcache-nways $(DCACHE_NWAYS) \
		--dcache-block-words $(DCACHE_BLOCK_WORDS) \
		--dcache-mshr-entry $(DCACHE_MSHR_ENTRY) \
		--dcache-mshr-sub-entry $(DCACHE_MSHR_SUB_ENTRY) \
		--dcache-wshr-entry $(DCACHE_WSHR_ENTRY) \
		--sharedmem-nbanks $(SHAREDMEM_NBANKS) \
		--sharedmem-capacity-bytes $(SHAREDMEM_CAPACITY_BYTES) \
		--lsu-num-entry-each-warp $(LSU_NUM_ENTRY_EACH_WARP) \
		--lsu-sharedmem-pipe-cut $(LSU_SHAREDMEM_PIPE_BOOL) \
		$(GEN_ARGS)
	cd $(LSU_SHAREDMEM_TARGET_DIR) && firtool --split-verilog --repl-seq-mem --repl-seq-mem-file=mem.conf -o . LsuSharedMemTop.fir
	./scripts/gen_sep_mem.sh ./scripts/vlsi_mem_gen $(LSU_SHAREDMEM_TARGET_DIR)/mem.conf $(LSU_SHAREDMEM_TARGET_DIR)/
	@echo "lsu-sharedmem output dir: $(LSU_SHAREDMEM_TARGET_DIR)"

sm-dc-rtl:
	./mill -i ventus[6.4.0].runMain top.SingleSMGen \
		--target-dir $(SM_DC_TARGET_DIR) \
		--num-warp $(NUM_WARP) \
		--num-thread $(NUM_THREAD) \
		--num-bank $(NUM_BANK) \
		--num-fetch $(NUM_FETCH) \
		--size-ibuffer $(SIZE_IBUFFER) \
		--num-block $(NUM_BLOCK) \
		--dcache-nsets $(DCACHE_NSETS) \
		--dcache-nways $(DCACHE_NWAYS) \
		--dcache-block-words $(DCACHE_BLOCK_WORDS) \
		--dcache-mshr-entry $(DCACHE_MSHR_ENTRY) \
		--dcache-mshr-sub-entry $(DCACHE_MSHR_SUB_ENTRY) \
		--dcache-wshr-entry $(DCACHE_WSHR_ENTRY) \
		--sharedmem-nbanks $(SHAREDMEM_NBANKS) \
		--sharedmem-capacity-bytes $(SHAREDMEM_CAPACITY_BYTES) \
		--lsu-num-entry-each-warp $(LSU_NUM_ENTRY_EACH_WARP) \
		--lsu-sharedmem-pipe-cut $(SM_PIPE_BOOL) \
		--lsu-dcache-pipe-cut $(SM_DCACHE_PIPE_BOOL) \
		$(GEN_ARGS)
	cd $(SM_DC_TARGET_DIR) && firtool --split-verilog --repl-seq-mem --repl-seq-mem-file=mem.conf -o . SM.fir
	./scripts/gen_sep_mem.sh ./scripts/vlsi_mem_gen $(SM_DC_TARGET_DIR)/mem.conf $(SM_DC_TARGET_DIR)/
	@echo "dc-rtl output dir: $(SM_DC_TARGET_DIR)"

sm-dc-rtl-scan:
	@for bank in $(BANK_LIST); do \
		$(MAKE) sm-dc-rtl SHAREDMEM_NBANKS=$$bank; \
	done

lsu-sharedmem-dc-rtl:
	./mill -i ventus[6.4.0].runMain top.LsuSharedMemGen \
		--target-dir $(LSU_SHAREDMEM_DC_TARGET_DIR) \
		--num-warp $(NUM_WARP) \
		--num-thread $(NUM_THREAD) \
		--num-block $(NUM_BLOCK) \
		--dcache-nsets $(DCACHE_NSETS) \
		--dcache-nways $(DCACHE_NWAYS) \
		--dcache-block-words $(DCACHE_BLOCK_WORDS) \
		--dcache-mshr-entry $(DCACHE_MSHR_ENTRY) \
		--dcache-mshr-sub-entry $(DCACHE_MSHR_SUB_ENTRY) \
		--dcache-wshr-entry $(DCACHE_WSHR_ENTRY) \
		--sharedmem-nbanks $(SHAREDMEM_NBANKS) \
		--sharedmem-capacity-bytes $(SHAREDMEM_CAPACITY_BYTES) \
		--lsu-num-entry-each-warp $(LSU_NUM_ENTRY_EACH_WARP) \
		--lsu-sharedmem-pipe-cut $(LSU_SHAREDMEM_PIPE_BOOL) \
		$(GEN_ARGS)
	cd $(LSU_SHAREDMEM_DC_TARGET_DIR) && firtool --split-verilog --repl-seq-mem --repl-seq-mem-file=mem.conf -o . LsuSharedMemTop.fir
	./scripts/gen_sep_mem.sh ./scripts/vlsi_mem_gen $(LSU_SHAREDMEM_DC_TARGET_DIR)/mem.conf $(LSU_SHAREDMEM_DC_TARGET_DIR)/
	@echo "dc-rtl output dir: $(LSU_SHAREDMEM_DC_TARGET_DIR)"

lsu-sharedmem-dc-rtl-scan:
	@for bank in $(BANK_LIST); do \
		$(MAKE) lsu-sharedmem-dc-rtl SHAREDMEM_NBANKS=$$bank; \
	done

clean:
	rm -rf out/ test_run_dir/ .idea/

clean-git:
	git clean -fd

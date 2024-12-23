SHELL := /bin/bash
# 定义 behavior 名称列表
BEHAVIORS = \
    Attention_NaiveTest \
    Attention_fp16888Test \
    Attention_fp16848Test \
    Attention_mix848Test \
    Linear_fp16_848Test \
    Linear_mix848Test \
    Linear_fp16_888Test \
    Linear_NaiveTest \
    Convolution_fp16_848Test \
    Convolution_mix848Test \
    Convolution_fp16_888Test \
    Convolution_NaiveTest \
    RNN_fp16_888Test \
    RNN_fp16_848Test \
    RNN_mix848Test \
    RNN_NaiveTest

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
	./mill -i ventus[6.4.0].tests.testOnly play.AdvancedTest 2>&1 | tee test_run_dir/AItest-Attention.log

verilog:
	./mill ventus[6.4.0].run

clean:
	rm -rf out/ test_run_dir/ .idea/

clean-git:
	git clean -fd

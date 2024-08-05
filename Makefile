SHELL := /bin/bash

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
	./mill -i ventus[6.4.0].tests.testOnly play.AdvancedTest 2>&1 | tee test_run_dir/test.log

verilog:
	./mill ventus.run

clean:
	rm -rf out/ test_run_dir/ .idea/

clean-git:
	git clean -fd

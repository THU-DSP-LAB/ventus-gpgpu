init:
	git submodule update --init --recursive --progress

# patch:
# 	find patches -type f | awk -F/ '{print("(echo "$$0" && cd dependencies/" $$2 " && git apply ../../" $$0 ")")}' | sh

# depatch:
# 	git submodule foreach 'git reset --hard && git clean -fdx'

bump:
	git submodule foreach git stash
	git submodule update --remote
	git add dependencies

# update-patches:
# 	rm -rf patches
# 	sed '/BEGIN-PATCH/,/END-PATCH/!d;//d' readme.md | awk '{print("mkdir -p patches/" $$1 " && wget " $$2 ".patch -P patches/" $$1 )}' | parallel
# 	git add patches

bsp:
	./mill -i mill.bsp.BSP/install

idea:
	./mill -i -j 0 mill.idea.GenIdea/idea

compile:
	./mill -i -j 0 __.compile

test:
	./mill -i ventus[6.4.0].tests.testOnly play.AdvancedTest

verilog:
	./mill ventus.run

clean:
	rm -rf out/ test_run_dir/ .idea/

clean-git:
	git clean -fd

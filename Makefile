init:
	git submodule update --init

patch:
	find patches -type f | awk -F/ '{print("(echo "$$0" && cd dependencies/" $$2 " && git apply ../../" $$0 ")")}' | sh

depatch:
	git submodule foreach 'git reset --hard && git clean -fdx'

bump:
	git submodule foreach git stash
	git submodule update --remote
	git add dependencies

update-patches:
	rm -rf patches
	sed '/BEGIN-PATCH/,/END-PATCH/!d;//d' readme.md | awk '{print("mkdir -p patches/" $$1 " && wget " $$2 ".patch -P patches/" $$1 )}' | parallel
	git add patches

bsp:
	mill -i mill.bsp.BSP/install

compile:
	mill -i -j 0 __.compile

test:
	mill -i -j 0 sanitytests.rocketchip

clean:
	git clean -fd

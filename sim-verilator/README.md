# Ventus GPGPU verilator simulation environment

## Prerequisites

Required:
- [Verilator](https://verilator.org/guide/latest/install.html) version 5.024 or later. (You may want to install [mold](https://github.com/rui314/mold) first)
- [spdlog](https://github.com/gabime/spdlog): `libVentusRTL.so` use this for logging
- [fmt](https://github.com/fmtlib/fmt) depended by `spdlog`

optional
- [mold](https://github.com/rui314/mold) for accelerating (verilated model) linking. If you want to use mold, install it before compiling verilator.
- [ccache](https://ccache.dev/)

```bash
sudo apt install verilator # check if version >=5.024
sudo apt install ccache mold libspdlog-dev libfmt-dev
```


## Usage - 中文

此文件夹下的代码可分为三部分：

1. 生成`libVentusRTL.so`动态库，是对chisel硬件、物理内存、内核函数拆分模块（将内核函数拆分为线程块后提供给硬件）三部分的建模，可视为一个GPU板卡的仿真模型。
   对外暴露了一些C API供上层驱动调用（详见`ventus_rtlsim.h`）。
2. 一个驱动`libVentusRTL.so`的示例性质的driver，通过读取`.metadata`与`.data`两种文件来生成测试用例，具有一个简陋的命令行接口。可通过`-f ventus_args.txt`来读取预定义的命令行参数，从而获悉测试用例配置。
3. `testcase/`文件夹给出了少量测试用例，包括相应的`ventus_args.txt`配置。   
   注意：`ventus_args.txt`本体必须与对应的`.metadata`、`.data`文件处于同一路径下，但可以软链接（`ln -s`）到其它位置。

代码构建：
1. `verilate.mk`用于构建`libVentusRTL.so`动态库，可独立工作
2. `Makefile`用于构建迷你driver可执行文件，它include了`verilate.mk`，从而可在需要时自动构建所需要的`libVentusRTL.so`
3. 两者都支持Debug（默认，`-O0 -g`）编译与Release（`-O2`）编译模式

```bash
# 构建libVentusRTL.so与mini driver，可直接运行仿真
# Debug构建与仿真，输出位于build/***/debug中
make -j run
# Release构建与仿真，输出位于build/***/release中
make RELEASE=1 -j run
# 查看所支持的命令行参数
./build/driver_example/release/sim-VentusRTL --help
# 手动启动仿真
./build/driver_example/release/sim-ventusRTL -f ventus_args.txt

# 仅构建libVentusRTL.so动态库，到build/libVentusRTL/***/libVentusRTL.so
make -f verilate.mk 
make -f verilate.mk RELEASE=1
```


## Usage - English

The code under this folder can be divided into three parts:

1. Generating the dynamic library `libVentusRTL.so`, which models the Chisel hardware, physical memory, and a divide-Kernel-into-ThreadBlocks module. It can be considered as a simulation model of a GPU card. It exposes some C APIs for upper-layer drivers to call (see `ventus_rtlsim.h` for details).

2. An example mini driver that drives `libVentusRTL.so`. It generates test cases by reading `.metadata` and `.data` files and has a simple command-line interface. The predefined command-line arguments can be read through `-f ventus_args.txt` to get the test case configuration.

How to build:

```bash
# Build libVentusRTL.so and the mini-driver, ready to run the simulation
# Debug build and run, output located in build/***/debug
make -j run
# Release build and run, output located in build/***/release
make RELEASE=1 -j run
# find supported cmd args
./build/driver_example/release/sim-VentusRTL --help
# run the simulation by hand
./build/driver_example/release/sim-VentusRTL -f ventus_args.txt

# Only build libVentusRTL.so, to build/libVentusRTL/***/libVentusRTL.so
make -f verilate.mk 
make -f verilate.mk RELEASE=1
```

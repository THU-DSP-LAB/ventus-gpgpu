# Ventus GPGPU verilator simulation environment

## Prerequisites

Required:
- [Verilator](https://verilator.org/guide/latest/install.html) version 5.034 recommended. (You may want to install [mold](https://github.com/rui314/mold) first)
- [spdlog](https://github.com/gabime/spdlog): `libVentusRTL.so` use this for logging
- [fmt](https://github.com/fmtlib/fmt) depended by `spdlog`

optional
- [mold](https://github.com/rui314/mold) for accelerating (verilated model) linking. If you want to use mold, install it before compiling verilator.
- [ccache](https://ccache.dev/)

```bash
sudo apt install verilator # check if version >=5.026
sudo apt install ccache mold libspdlog-dev libfmt-dev
```

## Usage - 中文
Jump to [English](#usage---english) version

此文件夹下的代码可分为三部分：

1. 生成`libVentusRTL.so`动态库，是对chisel硬件、物理内存、内核函数拆分模块（将内核函数拆分为线程块后提供给硬件）三部分的建模，可视为一个GPU板卡的仿真模型。
   对外暴露了一些C API供上层驱动调用（详见`ventus_rtlsim.h`）。
2. 一个驱动`libVentusRTL.so`的示例性质的迷你driver，通过读取`.metadata`与`.data`两种文件来生成测试用例，具有一个简陋的命令行接口。可通过`-f ventus_args.txt`来读取预定义的命令行参数，从而获悉测试用例配置。
3. `testcase/`文件夹给出了少量测试用例，包括相应的`ventus_args.txt`配置。   
   注意：`ventus_args.txt`本体必须与对应的`.metadata`、`.data`文件处于同一路径下，但可以软链接（`ln -s`）到其它位置。

注意：`.metadata`+`.data`文件作为测例的方式目前仅做兼容性保留，更加推荐使用完整的工具链驱动`libVentusRTL.so`来运行仿真，详见[ventus-env](https://github.com/THU-DSP-LAB/ventus-env)

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
./build/driver_example/release/sim-VentusRTL -f ventus_args.txt

# 仅构建libVentusRTL.so动态库，到build/libVentusRTL/***/libVentusRTL.so
make -f verilate.mk 
make -f verilate.mk RELEASE=1
```

迷你driver `sim-VentusRTL` 支持的命令行参数可用`--help`参数查看，常用的如下：
* `-f ventus_args.txt`读入写在指定文件中的命令行选项，与直接将文件内容作为命令行选项传递给可执行文件等价
* `--waveform`开启波形导出功能，导出的FST波形在`logs`目录下，可用gtkwave查看
* `--dump-mem 0x90001000,0x90001020`会在仿真结束后导出物理地址0x90001000 ≤ addr ≤ 0x90001020范围内的数据，每4字节一行，帮助验证执行结果的正确性
* 在`ventus_args.txt`中通常还会使用`--kernel`, `--sim-time-max`, `--dump-mem`等参数，参见仓库中已有的示例修改即可

如何新生成`.metadata`和`.data`测例文件：使用[完整工具链](https://github.com/THU-DSP-LAB/ventus-env)运行OpenCL程序时，POCL会自动导出此两文件。如果程序会运行kernel多次，则会导出一系列配对的`.metadata`和`.data`文件，需要按照正确的顺序编写ventus_args.txt。再次提示，推荐使用完整工具链运行新测例。

## Usage - English
跳转到[中文](#usage---中文)版本

The code in this folder is organized into three parts:

1. **`libVentusRTL.so` dynamic library**  
   This models three components: the Chisel hardware, physical memory, and kernel function decomposition (splitting kernel functions into thread blocks for hardware execution).
   It can be viewed as a simulation model of a GPU board.
   A set of C APIs is exposed for use by upper-level drivers (see `ventus_rtlsim.h`).

2. **Example driver for `libVentusRTL.so`** (mini driver)  
   A simple driver that loads `.metadata` and `.data` files to generate test cases.
   It has a basic command-line interface and can read predefined cmd arguments from file using `-f ventus_args.txt`.

3. **`testcase/` folder**  
   Contains a few sample test cases along with their corresponding `ventus_args.txt` configurations.
   Note: The `ventus_args.txt` file must be in the same directory as its `.metadata` and `.data` files (though symbolic links created with `ln -s` are allowed).

⚠️ **Important**: Using `.metadata` + `.data` files for test cases is only kept for compatibility. It is strongly recommended to use the full toolchain to drive `libVentusRTL.so` for simulation instead. See [ventus-env](https://github.com/THU-DSP-LAB/ventus-env).


### Build Instructions

1. `verilate.mk` builds the `libVentusRTL.so` library (can be used independently).
2. `Makefile` builds the mini driver executable. It includes `verilate.mk`, so the required `libVentusRTL.so` will be built automatically if missing.
3. Both support **Debug** (default, `-O0 -g`) and **Release** (`-O2`) build modes.

```bash
# Build libVentusRTL.so and the mini driver, then run simulation
# Debug build (outputs in build/***/debug)
make -j run

# Release build (outputs in build/***/release)
make RELEASE=1 -j run

# View supported command-line options
./build/driver_example/release/sim-VentusRTL --help

# Manually run simulation with arguments
./build/driver_example/release/sim-VentusRTL -f ventus_args.txt

# Build only libVentusRTL.so (output at build/libVentusRTL/***/libVentusRTL.so)
make -f verilate.mk
make -f verilate.mk RELEASE=1
```

### Mini Driver (`sim-VentusRTL`) Options

Run with `--help` to view all options. Commonly used ones include:

* `-f ventus_args.txt`
  Loads command-line options from the specified file (equivalent to passing the file contents directly as arguments).
* `--waveform`
  Enables waveform export. Generated FST files are placed in the `logs` directory and can be viewed with **gtkwave**.
* `--waveform-window BEGIN END`
  Enables waveform export only for the `[BEGIN, END)` simulation-time window. This is useful after an assertion identifies the failure cycle.
* `--dump-mem 0x90001000,0x90001020`
  Dumps memory contents in the specified range (`0x90001000 ≤ addr ≤ 0x90001020`) after simulation. Data is printed in 4-byte lines to help verify correctness.

In `ventus_args.txt`, parameters such as `--kernel`, `--sim-time-max`, and `--dump-mem` are commonly used. Refer to existing examples in the repository for guidance.

### Generating New `.metadata` and `.data` Files

When running OpenCL programs with the [full toolchain](https://github.com/THU-DSP-LAB/ventus-env), POCL automatically exports `.metadata` and `.data` files.

* If a program launches multiple kernels, a series of `.metadata` and `.data` file pairs will be generated.
* These must be referenced in the correct order in `ventus_args.txt`.

🔑 Reminder: It is recommended to use the **full toolchain** to run new test cases instead of relying on `.metadata`/`.data` directly.

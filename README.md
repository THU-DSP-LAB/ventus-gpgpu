# Ventus(承影) GPGPU
GPGPU processor supporting RISCV-V extension, developed with Chisel HDL.

Copyright 2021-2023 by International Innovation Center of Tsinghua University, Shanghai

We are calling for contributors. If you are interested in Ventus GPGPU, please contact yang-zx21@mails.tsinghua.edu.cn

“承影”在RVV编译器工具链、验证环境开发和硬件设计方面还有很多不足，如果您有意愿参与到“承影”的开发中，欢迎在github上pull request，也欢迎联系 yang-zx21@mails.tsinghua.edu.cn

承影2.0架构文档在[这里](https://github.com/THU-DSP-LAB/ventus-gpgpu/blob/master/docs/承影GPGPU架构文档手册v2.0.pdf)，添加了对OpenCL支持所需的改动。如果您在软硬件方面有任何建议，欢迎提issue或邮件联系。

## Architecture

The micro-architecture overview of Ventus(承影) is shown below. 

ISA and micro-architecture docs is [here](https://github.com/THU-DSP-LAB/ventus-gpgpu/blob/master/docs/ventus%20GPGPU%20architecture%20whitepaper%20v2.0.pdf). Chinese docs is [here](https://github.com/THU-DSP-LAB/ventus-gpgpu/blob/master/docs/承影GPGPU架构文档手册v2.0.pdf). 

OpenCL C compiler based on LLVM is developed by Terapines([兆松科技](https://www.terapines.com/)).

Use the script in [ventus-llvm](https://github.com/THU-DSP-LAB/llvm-project) to configure the complete software toolchain, including [isa-simulator](https://github.com/THU-DSP-LAB/ventus-gpgpu-isa-simulator), [pocl](https://github.com/THU-DSP-LAB/pocl) and [driver](https://github.com/THU-DSP-LAB/pocl).


![](./docs/images/ventus_arch.png)

## Quick Start
[从零开始的配置教程](https://zhuanlan.zhihu.com/p/586445036)（中文，从WSL和IDEA安装讲起）

The tutorial of Chisel development environment configuration comes from [chipsalliance/playground: chipyard in mill :P](https://github.com/chipsalliance/playground)

0. Install dependencies and setup environments: 
- Arch Linux  
`pacman -Syu --noconfirm make parallel wget cmake ninja mill dtc verilator git llvm clang lld protobuf antlr4 numactl`
- Nix  
`nix-shell`
- Ubuntu  
```shell
apt-get install make parallel wget cmake verilator git llvm clang lld protobuf-compiler antlr4 numactl
curl -L https://github.com/com-lihaoyi/mill/releases/download/0.10.8/0.10.8 > mill && chmod +x mill
```
> We recomment using java 17 or higher versions.

1. Init and update dependences

```shell
make init
make patch
```

2. IDE support  `make bsp # generate IDE bsp`

3. to generate verilog file, use `make verilog`. The output file is `GPGPU_top.v` . Notice that if you install 'mill' with 'curl', use `./mill ` to replace `mill` in Makefile commands.

4. to run tests, use `make tests`. Output waveform file is at `test_run_dir`  

> Notice that current codes are not stable and there are conflicts between existing codes and testcase `gaussian` `gemm`. We are preparing new testcase format to integrate with software toolchain and please wait for our new version.

## Acknowledgement

We refer to some open-source design when developing Ventus GPGPU.

| Sub module                | Source                                                                                                                                              | Detail                                                                         |
|---------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------|
| CTA scheduler             | [MIAOW](https://github.com/VerticalResearchGroup/miaow)                                                                                             | Our CTA scheduler module is based on MiaoW ultra-threads dispatcher.           |
| L2Cache                   | [block-inclusivecache-sifive](https://github.com/sifive/block-inclusivecache-sifive) | Our L2Cache design is inspired by Sifive's block-inclusivecache     |
| Multiplier                | [XiangShan](https://github.com/OpenXiangShan/XiangShan)                                                                                             | We reused Array Multiplier in XiangShan. FPU design is also inspired by XiangShan. |
| Config, ... |  [rocket-chip](https://github.com/chipsalliance/rocket-chip)                                                                                                                                         | Some modules are sourced from RocketChip                                       |




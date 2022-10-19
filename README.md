# Ventus(承影) GPGPU
GPGPU processor supporting RISCV-V extension, developed with Chisel HDL.

Copyright 2021-2022 by International Innovation Center of Tsinghua University, Shanghai

We are calling for contributors. If you are interested in Ventus GPGPU, please contact yangkx20@mails.tsinghua.edu.cn

“承影”在RVV编译器工具链、验证环境开发和硬件设计方面还有很多不足，如果您有意愿参与到“承影”的开发中，欢迎在github上pull request，也欢迎联系 yangkx20@mails.tsinghua.edu.cn

8月23日线下活动的ppt已经上传到[这里](https://github.com/THU-DSP-LAB/ventus-gpgpu-doc/tree/main/slides)。

## Architecture

The micro-architecture overview of Ventus(承影) is shown below.
Chinese docs is [here](https://github.com/THU-DSP-LAB/ventus-gpgpu/blob/master/docs/Ventus-GPGPU-doc.md). English version will come soon.

![](./docs/images/ventus_arch.png)

For ISA simulator and riscv-gnu-toolchain, see [ventus-gpgpu-isa-simulator](https://github.com/THU-DSP-LAB/ventus-gpgpu-isa-simulator) and [ventus-riscv-gnu-toolchain](https://github.com/THU-DSP-LAB/riscv-gnu-toolchain)

## Quick Start

The tutorial of Chisel development environment configuration comes from [chipsalliance/playground: chipyard in mill :P](https://github.com/chipsalliance/playground)

0. Install dependencies and setup environments: 
- Arch Linux `pacman -Syu --noconfirm make parallel wget cmake ninja mill dtc verilator git llvm clang lld protobuf antlr4 numactl`
- Nix `nix-shell`
- Ubuntu
```shell
apt-get install make parallel wget cmake verilator git llvm clang lld protobuf-compiler antlr4 numactl
curl -L https://github.com/com-lihaoyi/mill/releases/download/0.10.8/0.10.8 > mill && chmod +x mill
```
1. Init and update dependences

```shell
make init
make patch
```

2. IDE support  `make bsp # generate IDE bsp`
3. to generate verilog file, use `make verilog`. The output file is `GPGPU_axi_top.v`
4. to run tests, use `make tests`. Output waveform file is at `test_run_dir` 

## Acknowledgement

We refer to some open-source design when developing Ventus GPGPU.

| Sub module                | Source                                                                                                                                              | Detail                                                                         |
|---------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------|
| CTA scheduler             | [MIAOW](https://github.com/VerticalResearchGroup/miaow)                                                                                             | Our CTA scheduler module is based on MiaoW ultra-threads dispatcher.           |
| L2Cache                   | [block-inclusivecache-sifive](https://github.com/sifive/block-inclusivecache-sifive) | Our L2Cache design is inspired by Sifive's block-inclusivecache     |
| Multiplier                | [XiangShan](https://github.com/OpenXiangShan/XiangShan)                                                                                             | We reused Array Multiplier in XiangShan. FPU design is also inspired by XiangShan. |
| Instructions, Config, ... |  [rocket-chip](https://github.com/chipsalliance/rocket-chip)                                                                                                                                         | Some modules are sourced from RocketChip                                       |




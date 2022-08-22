# Ventus(承影) GPGPU
GPGPU processor supporting RISCV-V extension, developed with Chisel HDL.

Copyright 2021-2022 by International Innovation Center of Tsinghua University, Shanghai
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
| L2Cache                   | [block-inclusivecache-sifive](https://github.com/sifive/block-inclusivecache-sifive) | Our L2Cache design is inspired by Sifive's block-inclusivicache     |
| Multiplier                | [XiangShan](https://github.com/OpenXiangShan/XiangShan)                                                                                             | We reused Array Multiplier in XiangShan. FPU design is also inspired by XiangShan. |
| Instructions, Config, ... |  [rocket-chip](https://github.com/chipsalliance/rocket-chip)                                                                                                                                         | Some modules are sourced from RocketChip                                       |




# ventus-gpgpu
GPGPU processor supporting RISCV-V extension, developed with Chisel HDL

## Quick Start

Chisel development environment config tutorial comes from [chipsalliance/playground: chipyard in mill :P (github.com)](https://github.com/chipsalliance/playground)

0. Install dependencies and setup environments: 
- Arch Linux `pacman -Syu --noconfirm make parallel wget cmake ninja mill dtc verilator git llvm clang lld protobuf antlr4 numactl`
- Nix `nix-shell`
1. Init and update dependences

```shell
cd ventus-gpgpu
make init
make patch
```

2. IDE support  `make bsp # generate IDE bsp`
3. to generate verilog file, use `make verilog`. The output file is `GPGPU_axi_top.v`
4. to run tests, use `make tests`. 

## Acknowledgement

We refer to some open-source design when developing Ventus GPGPU.

| Sub module    | Source                                                  | Detail                                                       |
| ------------- | ------------------------------------------------------- | ------------------------------------------------------------ |
| CTA scheduler |                                                         | We refer to code and design in miaow, adding                 |
| L2Cache       | https://github.com/chipsalliance/rocket-chip            | Our L2Cache design is inspired by Sifive's block-inclusivicache and RocketChip |
| Multiplier    | [XiangShan](https://github.com/OpenXiangShan/XiangShan) | We reused Array Multiplier in XiangShan. FPU design is also inspired by XiangShan. |
|               |                                                         |                                                              |




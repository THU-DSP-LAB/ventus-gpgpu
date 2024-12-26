# Ventus(乘影) GPGPU

GPGPU processor supporting RISCV-V extension, developed with Chisel HDL.

Copyright 2021-2024 by International Innovation Center of Tsinghua University, Shanghai.

We are calling for contributors. If you are interested in Ventus GPGPU, please contact <yff22@mails.tsinghua.edu.cn>.

“乘影”在RVV编译器工具链、验证环境开发和硬件设计方面还有很多不足，如果您有意愿参与到“乘影”的开发中，欢迎在github上pull request，也欢迎联系 <yff22@mails.tsinghua.edu.cn>。

乘影2.0架构文档在[这里](https://github.com/THU-DSP-LAB/ventus-gpgpu/blob/master/docs/乘影GPGPU架构文档手册v2.01.pdf)，添加了对OpenCL支持所需的改动。如果您在软硬件方面有任何建议，欢迎提issue或邮件联系。

乘影开源GPGPU项目网站：[opengpgpu.org.cn](https://opengpgpu.org.cn/)。

Home page of Ventus-GPGPU project: [opengpgpu.org.cn](https://opengpgpu.org.cn/).

乘影软件工具链release版本在[这里](https://opengpgpu.org.cn/html/web/project/release/index.html)获取。

You can get the release version of software toolchain [here](https://opengpgpu.org.cn/html/web/project/release/index.html).

## Architecture

The micro-architecture overview of Ventus(乘影) is shown below.

ISA and micro-architecture docs is [here](https://github.com/THU-DSP-LAB/ventus-gpgpu/blob/master/docs/ventus%20GPGPU%20architecture%20whitepaper%20v2.01.pdf). Chinese docs is [here](https://github.com/THU-DSP-LAB/ventus-gpgpu/blob/master/docs/乘影GPGPU架构文档手册v2.01.pdf).

OpenCL C compiler based on LLVM is developed by Terapines([兆松科技](https://www.terapines.com/)).

Use the script in [ventus-llvm](https://github.com/THU-DSP-LAB/llvm-project) to configure the complete software toolchain, including [isa-simulator](https://github.com/THU-DSP-LAB/ventus-gpgpu-isa-simulator), [pocl](https://github.com/THU-DSP-LAB/pocl) and [driver](https://github.com/THU-DSP-LAB/pocl).

![](./docs/images/ventus_arch.png)

## Quick Start

如果你需要从头开始配置WSL和IDEA的开发环境，可以参考中文教程[从零开始的配置教程](https://zhuanlan.zhihu.com/p/586445036)。这个教程的部分命令已经过时，但依然是很好的参考。

The tutorial of Chisel development environment configuration comes from [chipsalliance/playground: chipyard in mill :P](https://github.com/chipsalliance/playground)

0. Install dependencies and setup environments:

- Arch Linux  
`pacman -Syu --noconfirm make parallel wget cmake ninja mill dtc verilator git llvm clang lld protobuf antlr4 numactl`
- Nix  
`nix-shell`
- Ubuntu  

```shell
apt-get install gcc g++ make parallel wget cmake verilator git llvm clang lld protobuf-compiler antlr4 numactl
```

> We recomment using java 17 or higher versions. **We test the project under java 19.**

1. Clone project, init and update dependencies

```shell
git clone https://github.com/THU-DSP-LAB/ventus-gpgpu.git
make init
```

2. IDE support `make idea` or `make bsp # generate IDE bsp`

3. to generate verilog file, use `make verilog`. The output file is `GPGPU_top.v` .

4. to run tests, use `make test`. Output waveform file is at `test_run_dir` . Due to the limitations of `chiseltest`, we have customized another simulation framework based on Verilator. Please refer to the `sim-verilator` folder's README for more details.

## Kernel Metadata and Data File Format

Each test case for the **ventus-gpgpu** project includes two key files for describing kernel-specific information:

1. `.metadata` file: Contains metadata that provides detailed information about the kernel.
2. `.data` file: Contains initialization data for various memory buffers used by the kernel.

This section explains the format and purpose of these files.

### `.metadata` File Structure

The `.metadata` file contains the following structure:

```c++
struct meta_data {
    uint64_t start_addr;          // Instruction start address
    uint64_t kernel_id;           // Kernel ID
    uint64_t kernel_size[3];      // Workgroup dimensions (3D)
    uint64_t wf_size;             // Number of threads per warp
    uint64_t wg_size;             // Number of warps per workgroup
    uint64_t metaDataBaseAddr;    // CSR_KNL value
    uint64_t ldsSize;             // Size of local memory (shared memory) per workgroup
    uint64_t pdsSize;             // Size of private memory per thread
    uint64_t sgprUsage;           // Scalar register usage per warp
    uint64_t vgprUsage;           // Vector register usage per warp
    uint64_t pdsBaseAddr;         // Base address of kernel private memory. 
                                  // This value will be converted by the driver/test stimuli 
                                  // into the starting address for each workgroup, 
                                  // with an offset calculated as wf_size * wg_size * pdsSize.
    uint64_t num_buffer;          // Number of buffers (includes instruction buffer, kernel 
                                  // argument buffer, and private memory)
    uint64_t buffer_base[num_buffer];  // Base address of each buffer
    uint64_t buffer_size[num_buffer];  // Size (in bytes) of initialization data in each buffer 
                                       // (from the `.data` file)
    uint64_t buffer_allocsize[num_buffer];  // Actual allocated size (in bytes) of each buffer 
                                            // (allocsize >= size)
};
```

#### File Layout

Each `uint64_t` occupies 8 bytes and is written sequentially in the `.metadata` file (2 lines for each `uint64_t`). Fields such as `buffer_base`, `buffer_size`, and `buffer_allocsize` are dynamically sized based on `num_buffer`.

### `.data` File Structure

The `.data` file contains initialization data for all buffers defined in the `.metadata` file. Key details include:

- The file stores a total of `sum(buffer_size)` bytes, representing the initialization data for all buffers.
- **Buffers are stored sequentially** in the file. The data for each buffer corresponds to its entry in the `buffer_base` and `buffer_size` fields in `.metadata`.  
- Each buffer’s initialization data:
  - Starts at its respective `buffer_base` address.
  - Spans exactly `size` bytes as specified in `buffer_size`.

## Memory Access

- **Private Memory**:
  - Accessed by each thread using a dedicated instruction.
  - `allocSize` is the total size of private memory for the kernel.

- **Global Memory**:
  - Private memory and global memory share the same hierarchy and are accessed via the L2 cache.

- **Local Memory**:
  - Shared memory space within an SM.
  - Each workgroup uses a portion of local memory, adjusted based on the `CSR_LDS` value of the block. During block allocation, the offset is added dynamically.
  - The compiler ensures proper address adjustments during memory access.

- **Register Allocation**:
  - Registers within an SM are fixed in number. Each block occupies a portion of the available registers during execution.

## Understanding Program Output in Our Project

This section is dedicated to explaining the output generated by our program, which is crucial for developers who wish to understand the inner workings or debug the software. The output is structured to provide detailed insights into the program's execution, including instruction addresses, operations on warp units, and register manipulations.

### Warp Execution Output

The program output is like:

```plain
warp 3 0x800001d4 0x0042a303 x 6 90000000
```

- **`warp 3`** identifies the warp unit in action, which start from 0.
- **`0x800001d4`** specifies the virtual address of the instruction being executed.
- **`0x0042a303`** represents the instruction itself.
- **`x 6 90000000`** signifies an operation where the program writes the value `90000000` to scalar register 6 of warp 3.

For a more complex example:

```plain
warp 2 0x80000200 0x0002a2fb v 5 0001 00000000 00000000 00000000 be8d0fac
```

- **`v 5 0001`** indicates an operation on vector register 5 of the second warp, where `0001` is a mask specifying that only the last thread is active.
- The data **`be8d0fac`** is written to the last element of the vector register due to the mask setting.

### Jump Instructions

The output related to jump instructions follows this format:

```plain
warp 1 0x80000490 0x00008067 Jump? 1 800002f4
```

- **`Jump? 1 800002f4`** indicates a conditional jump to the address `0x800002f4` depending on the evaluation of the preceding condition.

### Load/Store Instructions

Load and store operations are crucial for reading from and writing to memory:

```plain
warp 2 0x80000200 0x0002a2fb lsu.r v 5 op 3 @ 00000000 bdcccccd 3e54ad4b 90002038
warp 2 0x80000200 0x0002a2fb v  5 0001 00000000 00000000 00000000 be8d0fac
```

- **`lsu.r`** specifies a load operation from memory into a register.
- **`@ 90002038`** marks the memory addresses from which data is loaded.
- **`v  5 0001 00000000 00000000 00000000 be8d0fac`** represents the data is loaded,  and only the last element of v[5] is set to `be8d0fac` due to the mask (and apparently only `90002038` is a valid address).

For write operations:

```plain
warp 2 0x80000240 0x0052607b lsu.w v  5 op 3 mask 0001 00000000 bdcccccd 3e54ad4b 3f0e5e0a @ 00000000 90000034 90000038 9000003c
warp 2 0x80000240 0x0052607b lsu.w fin
```

### Branching Output

Branch-related outputs are essential for SIMT arch support. Example:

```plain
warp 3 0x80000248 0x0483305b  setrpc 0x8000028c
warp 3 0x8000024c 0x0401905b vbranch     current mask and npc:   0001    0x80000250
warp 3 0x8000028c 0x0000205b join    mask and npc:    1110 0x8000028c pop stack ? 1
```

## Citation and Presentation Materials

Our work has been accepted to **The 42nd IEEE International Conference on Computer Design (ICCD 2024)**. If you find this repository helpful for your research, please consider citing our paper:

### Paper Information

**Title:** Ventus: A High-performance Open-source GPGPU Based on RISC-V and Its Vector Extension  
**Authors:** Jingzhou Li, Kexiang Yang, Chufeng Jin, Xudong Liu, Zexia Yang, Fangfei Yu, Yujie Shi, Mingyuan Ma, Li Kong, Jing Zhou, Hualin Wu, and Hu He  
**DOI:** coming soon...

**Citation Format (BibTeX):**

```bibtex
@inproceedings{ventus_iccd2024,
  title     = {Ventus: A High-performance Open-source GPGPU Based on RISC-V and Its Vector Extension},
  author    = {Jingzhou Li and Kexiang Yang and Chufeng Jin and Xudong Liu and Zexia Yang and Fangfei Yu and Yujie Shi and Mingyuan Ma and Li Kong and Jing Zhou and Hualin Wu and Hu He},
  booktitle = {The 42nd IEEE International Conference on Computer Design (ICCD)},
  year      = {2024}
}
```

### Presentation Slides

The presentation slides from ICCD 2024 are available in the [`docs`](https://github.com/THU-DSP-LAB/ventus-gpgpu/tree/master/docs) folder of this repository. You can view or download them directly [here](https://github.com/THU-DSP-LAB/ventus-gpgpu/blob/master/docs/ICCD2024_Ventus_Presentation.pdf).

## Acknowledgement

We refer to some open-source design when developing Ventus GPGPU.

| Sub module    | Source                                                                               | Detail                                                                             |
| ------------- | ------------------------------------------------------------------------------------ | ---------------------------------------------------------------------------------- |
| CTA scheduler | [MIAOW](https://github.com/VerticalResearchGroup/miaow)                              | Our CTA scheduler module is based on MiaoW ultra-threads dispatcher.               |
| L2Cache       | [block-inclusivecache-sifive](https://github.com/sifive/block-inclusivecache-sifive) | Our L2Cache design is inspired by Sifive's block-inclusivecache                    |
| Multiplier    | [XiangShan](https://github.com/OpenXiangShan/XiangShan)                              | We reused Array Multiplier in XiangShan. FPU design is also inspired by XiangShan. |
| Config, ...   | [rocket-chip](https://github.com/chipsalliance/rocket-chip)                          | Some modules are sourced from RocketChip                                           |

## License and Project Origin

This repository is licensed under the Mulan Permissive Software License, Version 2 (Mulan PSL v2), except for certain files which are licensed under different terms.

- `build.sc`, `common.sc` and `shell.nix` are licensed under the Apache License, Version 2.0. These files were originally derived from the [chipsalliance/playground](https://github.com/chipsalliance/playground) repository. While these files served as the foundation, the build system has since undergone significant evolution.

- `ventus/src/config/config.scala` is licensed under the Apache License, Version 2.0.

- `ventus/src/pipeline/ALU.scala` is licensed under both the Apache License, Version 2.0 and the BSD 3-Clause License, reflecting its origins from the [rocket-chip](https://github.com/chipsalliance/rocket-chip) repository.

For more details, please see the `NOTICE` file and the headers of the respective files.

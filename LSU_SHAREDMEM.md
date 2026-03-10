# LSU SharedMemory 子模块生成说明

## 相关代码位置

- 专用生成入口：`ventus/src/top/LsuSharedMemGen.scala`
  - 只保留 LSU 侧 shared memory `req/rsp` 接口。
  - 内部直接实例化真实的 `SharedMemory`，包含 bank conflict arbiter 和读写 crossbar。
  - 默认输出到 `sim-verilator-nocache/lsuSharedMem/` 下的自动命名子文件夹。

- 子模块 top：`ventus/src/top/LsuSharedMemGen.scala`
  - 这次用于综合的 top module 名称是 `LsuSharedMemTop`。

- 参数覆盖支持：`ventus/src/top/parameters.scala`
  - `HardwareConfig` 会在 elaboration 前覆盖 `num_warp`、`num_thread`、`sharedmem_nBanks`、`sharedmem_capacityBytes` 等参数。

## 模块范围

这个模块专注于：

- LSU 侧 shared memory 请求/返回 bundle
- shared memory bank conflict 仲裁
- lane-to-bank 写 crossbar
- bank-to-lane 读 crossbar
- shared memory banked SRAM datapath

这个模块不包含：

- CTA scheduler
- warp frontend
- icache/dcache
- 其它 SM 后端执行单元

因此它更适合单独评估 shared memory 带宽变化对面积、时序、功耗的影响。

## 命令示例

直接生成 `.sv` 和 `parameters.json`：

```bash
./mill -i ventus[6.4.0].runMain top.LsuSharedMemGen \
  --num-warp 8 \
  --num-thread 32 \
  --sharedmem-nbanks 64 \
  --sharedmem-capacity-bytes 131072
```

## 生成 Verilog 的位置

默认输出目录格式：

```text
sim-verilator-nocache/lsuSharedMem/<自动命名子文件夹>/
```

例如：

```text
sim-verilator-nocache/lsuSharedMem/LsuSharedMemTop_warp8_thread32_smem131072B_smbank64_smbw2048/LsuSharedMemTop.sv
```

同一目录下还会有：

```text
sim-verilator-nocache/lsuSharedMem/LsuSharedMemTop_warp8_thread32_smem131072B_smbank64_smbw2048/parameters.json
```

## 子文件夹自动命名

默认自动命名规则：

```text
LsuSharedMemTop[_<DIR_PREFIX>]_warp<NUM_WARP>_thread<NUM_THREAD>_smem<BYTES>B_smbank<NBANKS>_smbw<BITS>
```

其中：

- `smbank` 是 shared memory bank 数
- `smbw` 是总带宽，按 `sharedmem_nBanks * 32 bit/cycle` 计算

如果显式传入 `--target-dir`，则会直接使用该目录，不再自动命名。

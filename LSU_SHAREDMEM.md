# LSU SharedMemory 子模块生成说明

## 相关代码位置

- 专用生成入口：`ventus/src/top/LsuSharedMemGen.scala`
  - 内部保留真实的 `LSUexe + SharedMemory` 连接。
  - 包含地址计算、MSHR、shared-memory bank conflict arbiter 和读写 crossbar。
  - `dcache` 作为外部接口保留，不把整颗 SM 都带进来。
  - 默认输出到 `gen_lsu_sharedmem_verilog/` 下的自动命名子文件夹。

- 子模块 top：`ventus/src/top/LsuSharedMemGen.scala`
  - 这次用于综合的 top module 名称是 `LsuSharedMemTop`。

- 参数覆盖支持：`ventus/src/top/parameters.scala`
  - `HardwareConfig` 会在 elaboration 前覆盖 `num_warp`、`num_thread`、`sharedmem_nBanks`、`sharedmem_capacityBytes` 等参数。

## 模块范围

这个模块专注于：

- LSU 地址计算与 shared-memory 请求形成
- LSU 内部 MSHR / response merge
- shared memory bank conflict 仲裁
- lane-to-bank 写 crossbar
- bank-to-lane 读 crossbar
- shared memory banked SRAM datapath

这个模块不包含：

- CTA scheduler
- warp frontend
- icache
- dcache 本体
- 其它 SM 后端执行单元

因此它更适合单独评估 shared memory 带宽变化对面积、时序、功耗的影响。

## 命令示例

直接生成 `.fir` 和 `parameters.json`：

```bash
./mill -i ventus[6.4.0].runMain top.LsuSharedMemGen \
  --num-warp 8 \
  --num-thread 32 \
  --num-block 8 \
  --sharedmem-nbanks 64 \
  --sharedmem-capacity-bytes 131072
```

或通过根目录 `Makefile` 直接生成 split Verilog：

```bash
make lsu-sharedmem-verilog \
  NUM_WARP=8 NUM_THREAD=32 NUM_BLOCK=8 \
  SHAREDMEM_NBANKS=64 SHAREDMEM_CAPACITY_BYTES=131072
```

## 生成 Verilog 的位置

默认输出目录格式：

```text
gen_lsu_sharedmem_verilog/<自动命名子文件夹>/
```

例如：

```text
gen_lsu_sharedmem_verilog/LsuSharedMemTop_warp8_thread32_smem131072B_smbank64_smbw2048/LsuSharedMemTop.sv
```

同一目录下还会有：

```text
gen_lsu_sharedmem_verilog/LsuSharedMemTop_warp8_thread32_smem131072B_smbank64_smbw2048/filelist.f
gen_lsu_sharedmem_verilog/LsuSharedMemTop_warp8_thread32_smem131072B_smbank64_smbw2048/mem.conf
gen_lsu_sharedmem_verilog/LsuSharedMemTop_warp8_thread32_smem131072B_smbank64_smbw2048/metadata/seq_mems.json
gen_lsu_sharedmem_verilog/LsuSharedMemTop_warp8_thread32_smem131072B_smbank64_smbw2048/parameters.json
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

## 推荐综合路径

推荐直接生成到 `/home/mmy/work/ventus/dc_compile/rtl1/LsuSharedMemTop/<DESIGN>/`：

```bash
make lsu-sharedmem-dc-rtl \
  NUM_WARP=8 NUM_THREAD=32 NUM_BLOCK=8 \
  SHAREDMEM_CAPACITY_BYTES=131072 SHAREDMEM_NBANKS=64
```

批量扫 bank：

```bash
make lsu-sharedmem-dc-rtl-scan \
  NUM_WARP=8 NUM_THREAD=32 NUM_BLOCK=8 \
  SHAREDMEM_CAPACITY_BYTES=131072 \
  BANK_LIST="8 16 32 64 128"
```

然后在 `/home/mmy/work/ventus/dc_compile` 里执行：

```bash
bash do_all_dc.sh
```

DC run 结束后，再手动运行：

```bash
scripts/post/report_total_area.sh \
  LsuSharedMemTop \
  LsuSharedMemTop_warp8_thread32_smem131072B_smbank64_smbw2048
```

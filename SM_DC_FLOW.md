# SM / LSU-SharedMemory DC + CACTI Flow

## 目标

这套流程用于两类模块：

- `SM`
  - 完整单个 SM
  - top module 是 `SM`
- `LsuSharedMemTop`
  - `LSUexe + SharedMemory` 子系统
  - top module 是 `LsuSharedMemTop`

目标是把参数化 RTL 先生成到 [`/home/mmy/work/ventus/dc_compile/rtl1`](/home/mmy/work/ventus/dc_compile/rtl1)，再复用 [`/home/mmy/work/ventus/dc_compile/do_all_dc.sh`](/home/mmy/work/ventus/dc_compile/do_all_dc.sh) 并行启动 DC。  
DC 只统计逻辑面积；`SyncReadMem` 替换出的 SRAM 用 CACTI 后处理补上面积。

## 代码入口

- `SM` 生成器：[`ventus/src/top/SingleSMGen.scala`](/home/mmy/work/ventus/ventus-gpgpu/ventus/src/top/SingleSMGen.scala)
- `LsuSharedMemTop` 生成器：[`ventus/src/top/LsuSharedMemGen.scala`](/home/mmy/work/ventus/ventus-gpgpu/ventus/src/top/LsuSharedMemGen.scala)
- 统一生成命令：[`Makefile`](/home/mmy/work/ventus/ventus-gpgpu/Makefile)
- DC 批量启动：[`/home/mmy/work/ventus/dc_compile/do_all_dc.sh`](/home/mmy/work/ventus/dc_compile/do_all_dc.sh)
- DC 脚本：[`/home/mmy/work/ventus/dc_compile/scripts/dc/run_dc.tcl`](/home/mmy/work/ventus/dc_compile/scripts/dc/run_dc.tcl)
- CACTI 后处理：[`/home/mmy/work/ventus/dc_compile/scripts/post/report_total_area.sh`](/home/mmy/work/ventus/dc_compile/scripts/post/report_total_area.sh)

## 流程原则

1. 在 `ventus-gpgpu` 侧只做 RTL 生成，不直接启动 DC。
2. 生成结果统一落到 `dc_compile/rtl1/<TOP>/<DESIGN>/`。
3. `do_all_dc.sh` 只负责并行启动逻辑综合。
4. 所有频率 run 完成后，再批量做 CACTI 面积后处理并导出 CSV。

## 目录约定

生成后的目录结构如下：

```text
/home/mmy/work/ventus/dc_compile/rtl1/
  SM/
    <DESIGN>/
      filelist.f
      mem.conf
      metadata/seq_mems.json
      parameters.json
      ...
  LsuSharedMemTop/
    <DESIGN>/
      filelist.f
      mem.conf
      metadata/seq_mems.json
      parameters.json
      ...
```

`<DESIGN>` 会自动携带关键参数，例如：

```text
SM_warp8_thread32_smem131072B_smbank32_smbw1024
LsuSharedMemTop_warp8_thread32_smem131072B_smbank64_smbw2048
```

这里：

- `warp8` 表示 `num_warp = 8`
- `thread32` 表示 `num_thread = 32`
- `smem131072B` 表示 `sharedmem_capacityBytes = 128KB`
- `smbank64` 表示 `sharedmem_nBanks = 64`
- `smbw2048` 表示 `64 * 32 = 2048 bit/cycle`

## Memory 的处理边界

这条流程主要处理 `SyncReadMem` 替换出的 memory。

关键文件：

- `mem.conf`
  - 记录 externalized SRAM 的几何信息
- `metadata/seq_mems.json`
  - 记录每种 SRAM 的实例数
- `filelist.f`
  - DC 的 RTL 入口
- `*_ext.v`
  - 行为级 SRAM 模型，只用于占位，不进入 DC 逻辑综合

当前 DC 脚本会过滤：

- `*_ext.v`
- `*_ext.sv`
- `ram_*.sv`
- `data_*x*.sv`
- `tag_*x*.sv`
- `stack_mem_*.sv`

并把未解析 memory 当 blackbox。  
因此：

- DC 报告的是逻辑面积
- SRAM 面积由 CACTI 后处理补上

## 操作步骤

### 第 1 步：生成一个或多个设计到 `dc_compile/rtl1`

先进入仓库：

```bash
cd /home/mmy/work/ventus/ventus-gpgpu
```

### 第 1A 步：生成完整单 SM

单个配置：

```bash
make sm-dc-rtl \
  NUM_WARP=8 \
  NUM_THREAD=32 \
  NUM_BLOCK=8 \
  SHAREDMEM_CAPACITY_BYTES=131072 \
  SHAREDMEM_NBANKS=32
```

扫描多个 bank：

```bash
make sm-dc-rtl-scan \
  NUM_WARP=8 \
  NUM_THREAD=32 \
  NUM_BLOCK=8 \
  SHAREDMEM_CAPACITY_BYTES=131072 \
  BANK_LIST="8 16 32 64 128"
```

### 第 1B 步：生成 LSU + SharedMemory 子系统

单个配置：

```bash
make lsu-sharedmem-dc-rtl \
  NUM_WARP=8 \
  NUM_THREAD=32 \
  NUM_BLOCK=8 \
  SHAREDMEM_CAPACITY_BYTES=131072 \
  SHAREDMEM_NBANKS=32
```

扫描多个 bank：

```bash
make lsu-sharedmem-dc-rtl-scan \
  NUM_WARP=8 \
  NUM_THREAD=32 \
  NUM_BLOCK=8 \
  SHAREDMEM_CAPACITY_BYTES=131072 \
  BANK_LIST="8 16 32 64 128"
```

### 第 2 步：检查生成结果

以一个 `SM` 设计为例，应至少包含：

- `filelist.f`
- `mem.conf`
- `metadata/seq_mems.json`
- `parameters.json`

例如：

```bash
ls /home/mmy/work/ventus/dc_compile/rtl1/SM/SM_warp8_thread32_smem131072B_smbank32_smbw1024
```

### 第 3 步：配置 DC 批量脚本

编辑 [`/home/mmy/work/ventus/dc_compile/do_all_dc.sh`](/home/mmy/work/ventus/dc_compile/do_all_dc.sh)：

- 修改 `TARGETS`
  - 决定要跑 `SM`、`LsuSharedMemTop` 或两者都跑
- 修改 `FREQUENCIES`
  - 例如 `FREQUENCIES=(1000 1100 1200)`

当前脚本已经默认支持：

- `SM`，模式 `filelist`
- `LsuSharedMemTop`，模式 `filelist`

## 多频率兼容性

兼容。`do_all_dc.sh` 会对同一个 `DESIGN` 的每个频率分别启动一个 tmux session。

run 目录名会自动带频率，例如：

```text
/home/mmy/work/ventus/dc_compile/runs/SM_warp8_thread32_smem131072B_smbank32_smbw1024/2026-03-11_10-00-00__1200MHz__Toggle0.18
/home/mmy/work/ventus/dc_compile/runs/SM_warp8_thread32_smem131072B_smbank32_smbw1024/2026-03-11_10-05-00__1100MHz__Toggle0.18
```

所以：

- 同一个 design 跑多个频率没有冲突
- 你可以直接从 `runs/<DESIGN>/` 子目录名看出对应频率

需要注意的是，后处理时如果一个 design 对应多个频率 run，最好显式传入具体 `RUN_DIR`，不要依赖“自动取最新目录”。

### 第 4 步：启动 DC

进入 `dc_compile`：

```bash
cd /home/mmy/work/ventus/dc_compile
```

启动批量综合：

```bash
bash do_all_dc.sh
```

查看 tmux：

```bash
tmux ls
```

## 第 5 步：等待 DC 结束

每个 run 会写到：

```text
/home/mmy/work/ventus/dc_compile/runs/<DESIGN>/<RUN_TAG>/
```

典型输出包括：

- `report/area.rpt`
- `report/timing_setup.rpt`
- `report/timing_hold.rpt`
- `report/power.rpt`
- `report/breakdown_summary.rpt`
- `data/<TOP>_post.v`

## 第 6 步：批量做 CACTI 后处理并导出 CSV

```bash
cd /home/mmy/work/ventus/dc_compile

python3 scripts/post/collect_total_area.py \
  --out-csv results/sm_area_summary.csv
```

这个脚本会：

1. 遍历 `runs/<DESIGN>/<RUN>/`
2. 自动定位对应的 `rtl1/<TOP>/<DESIGN>/`
3. 读取 `mem.conf` 和 `metadata/seq_mems.json`
4. 调用 CACTI 生成或复用 `postprocess/sram_area.csv`
5. 读取每个 run 的 `report/area.rpt`
6. 把 `logic area + sram area + total area + timing slack + 关键参数` 追加到 CSV

CSV 默认是追加模式。
如果同一个 `run_dir` 已经在 CSV 里，脚本会自动跳过，避免重复。

### 常用过滤方式

只统计 `SM`：

```bash
python3 scripts/post/collect_total_area.py \
  --tops SM \
  --out-csv results/sm_only.csv
```

只统计 `LsuSharedMemTop`：

```bash
python3 scripts/post/collect_total_area.py \
  --tops LsuSharedMemTop \
  --out-csv results/lsu_sharedmem_only.csv
```

只统计某批 design：

```bash
python3 scripts/post/collect_total_area.py \
  --design-regex 'smbank(8|16|32|64|128)_' \
  --out-csv results/bank_sweep.csv
```

如果希望强制重新跑 CACTI，而不是复用已有 `postprocess/sram_area.csv`：

```bash
python3 scripts/post/collect_total_area.py \
  --out-csv results/sm_area_summary.csv \
  --force
```

### 输出字段

CSV 中会包含这些字段：

- `top`
- `design`
- `run_tag`
- `run_dir`
- `timestamp`
- `frequency_mhz`
- `toggle`
- `num_warp`
- `num_thread`
- `num_block`
- `sharedmem_capacity_bytes`
- `sharedmem_nbanks`
- `sharedmem_total_bandwidth_bits`
- `logic_area_um2`
- `sram_area_um2`
- `total_area_um2`
- `setup_slack_ns`
- `hold_slack_ns`
- `setup_startpoint`
- `setup_endpoint`
- `hold_startpoint`
- `hold_endpoint`

## 第 7 步：需要时，单独检查某个 run

如果你只想临时检查某一个 run，旧脚本仍然可以直接用：

```bash
cd /home/mmy/work/ventus/dc_compile

scripts/post/report_total_area.sh \
  SM \
  SM_warp8_thread32_smem131072B_smbank32_smbw1024 \
  /home/mmy/work/ventus/dc_compile/runs/SM_warp8_thread32_smem131072B_smbank32_smbw1024/2026-03-11_10-00-00__1200MHz__Toggle0.18
```

## 常用实验模板

### 模板 A：完整单 SM，固定 128KB，扫 bank

```bash
cd /home/mmy/work/ventus/ventus-gpgpu

make sm-dc-rtl-scan \
  NUM_WARP=8 \
  NUM_THREAD=32 \
  NUM_BLOCK=8 \
  SHAREDMEM_CAPACITY_BYTES=131072 \
  BANK_LIST="8 16 32 64 128"
```

### 模板 B：LSU + SharedMemory，固定 128KB，扫 bank

```bash
cd /home/mmy/work/ventus/ventus-gpgpu

make lsu-sharedmem-dc-rtl-scan \
  NUM_WARP=8 \
  NUM_THREAD=32 \
  NUM_BLOCK=8 \
  SHAREDMEM_CAPACITY_BYTES=131072 \
  BANK_LIST="8 16 32 64 128"
```

### 模板 C：在 `dc_compile` 下启动多频率 DC

先把 `do_all_dc.sh` 改成：

```bash
FREQUENCIES=(1000 1100 1200)
```

然后执行：

```bash
cd /home/mmy/work/ventus/dc_compile
bash do_all_dc.sh
```

## 当前脚本的假设

- 工艺库来自 N22
- CACTI 路径默认是 `/home/mmy/work/ventus/cacti/cacti`
- 时钟端口名默认是 `clock`
- 当前 DC 是 `ZeroWireload`
- 这还不是 topographical / physical-aware flow

## 调试建议

- 如果 DC 报 `FILELIST not found`
  - 检查 `rtl1/<TOP>/<DESIGN>/filelist.f` 是否存在
- 如果 DC 报 elaboration 失败
  - 检查 `do_all_dc.sh` 里的 `TOP` 是否和实际 Verilog 顶层一致
- 如果总面积后处理结果不对
  - 检查 `mem.conf`
  - 检查 `metadata/seq_mems.json`
  - 检查传入的 `RUN_DIR` 是否对应目标频率

## 当前推荐口径

主流程是：

1. `ventus-gpgpu` 生成参数化 RTL 到 `dc_compile/rtl1`
2. `dc_compile/do_all_dc.sh` 并行跑逻辑综合
3. `scripts/post/collect_total_area.py` 批量补 SRAM 面积并输出 CSV

`ventus-gpgpu/dc-sm-flow/` 仍保留，但只适合单个 design 的本地调试，不是主推荐路径。

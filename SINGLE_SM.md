# Single SM 生成说明

## 相关代码位置

- 单 SM 生成入口：`ventus/src/top/SingleSMNoCacheGen.scala`
  - 固定生成 `1` 个 SM。
  - 默认输出到 `sim-verilator-nocache/singleSM/` 下的自动命名子文件夹。
  - 支持通过命令行覆盖部分 SM 内部硬件参数，包括 shared memory 容量和 bank 数。

- 参数覆盖支持：`ventus/src/top/parameters.scala`
  - `HardwareConfig` 会在 elaboration 前覆盖 `num_warp`、`num_thread`、`num_bank`、`sharedmem_nBanks`、`sharedmem_capacityBytes` 等参数。

- nocache 构建入口：`sim-verilator-nocache/verilate.mk`
  - `make verilog`、`make lib` 会调用 `top.SingleSMNoCacheGen`。
  - 输出目录会根据参数自动拼接。

- 参数 JSON 转换：`sim-verilator-nocache/json2cpp.py`
  - 改成支持从指定子文件夹读取 `parameters.json`。

## 命令示例

直接生成 single SM 的 `.fir` 和 `parameters.json`：

```bash
./mill -i ventus[6.4.0].runMain top.SingleSMNoCacheGen \
  --num-warp 4 \
  --num-thread 8 \
  --num-block 4 \
  --sharedmem-nbanks 64 \
  --sharedmem-capacity-bytes 65536
```

通过 `make` 生成 Verilog：

```bash
make -C sim-verilator-nocache verilog \
  NUM_WARP=4 NUM_THREAD=8 NUM_BLOCK=4 \
  SHAREDMEM_NBANKS=64 SHAREDMEM_CAPACITY_BYTES=65536
```

附带更多 SM 内参数：

```bash
make -C sim-verilator-nocache verilog \
  NUM_WARP=4 NUM_THREAD=8 NUM_BLOCK=4 \
  SHAREDMEM_NBANKS=64 SHAREDMEM_CAPACITY_BYTES=65536 \
  GEN_ARGS="--num-bank 4 --num-fetch 2"
```

## 生成 Verilog 的位置

默认输出目录格式：

```text
sim-verilator-nocache/singleSM/<自动命名子文件夹>/
```

例如：

```text
sim-verilator-nocache/singleSM/singleSM_warp4_thread8/dut.v
```

同一目录下还会有：

```text
sim-verilator-nocache/singleSM/singleSM_warp4_thread8/GPGPU_top_nocache.fir
sim-verilator-nocache/singleSM/singleSM_warp4_thread8/parameters.json
```

用于综合时，Verilog 文件里的 top module 名称是：

```text
GPGPU_top_nocache
```

也就是说，文件名虽然通常是 `dut.v`，但 DC 里应使用：

```tcl
current_design GPGPU_top_nocache
```

## 子文件夹自动命名

默认自动命名规则：

```text
singleSM_warp<NUM_WARP>_thread<NUM_THREAD>
```

例如：

```text
singleSM_warp4_thread8
```

当前自动命名会体现 `warp`、`thread`、shared memory 容量、bank 数和总带宽。  
其中总带宽按 `sharedmem_nBanks * 32 bit/cycle` 计算。  
如果需要，也可以通过生成器参数覆盖：

```bash
./mill -i ventus[6.4.0].runMain top.SingleSMNoCacheGen \
  --output-root sim-verilator-nocache/singleSM \
  --dir-prefix singleSM \
  --num-warp 4 \
  --num-thread 8 \
  --sharedmem-nbanks 64 \
  --sharedmem-capacity-bytes 65536
```

如果显式传入 `--target-dir`，则会直接使用该目录，不再自动命名。

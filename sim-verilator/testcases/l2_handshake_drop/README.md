# L2 Handshake Drop GPU Workload PoC

This testcase is a GPU software workload for the L2 Scheduler bug where
`directory.io.result.ready` can acknowledge a dirty or `last_flush` result even
when `dir_result_buffer.io.enq.ready` is low and `hit == false`.

The workload runs two workgroups from source. Workgroup 0 dirties a smaller
line set and exits quickly so its dcache flush reaches L2. Workgroup 1 keeps
issuing cache-line-spaced stores/loads for longer, increasing the chance that
flush `last_flush` handling overlaps with dirty writeback/SourceD pressure.

Detection is handled by the temporary assertion in
`ventus/src/L2cache/Scheduler.scala`. It fires when a non-hit dirty or
`last_flush` directory result is accepted by `directory.io.result.ready` while
`dir_result_buffer.io.enq.ready` is low. Its purpose is to turn the failure into
a precise cycle marker; then only a small waveform window around that cycle is
needed for box/scope traceback.

Contents:

- `kernel/l2_handshake_drop.s`: runnable RISC-V/Ventus assembly workload.
- `kernel/l2_handshake_drop.cl`: OpenCL source version of the same access
  pattern.
- `scripts/build_workload.sh`: assembles the workload and regenerates the
  sim-verilator kernel image.
- `scripts/build_workload_image.py`: emits `.metadata/.data` for the
  sim-verilator kernel loader.
- `scripts/run_sim_verilator_workload.sh`: builds/runs the testcase through the
  local `sim-verilator` driver. Pass `--waveform-window BEGIN END` only after an
  assertion cycle is known. It sources
  `/home/ain/fuzz_gpgpu/fuzzgpu/targets/ventus_env.sh` by default so the local
  Verilator/FIRTool binaries are on `PATH`.
- `image/l2_handshake_drop.metadata` and `image/l2_handshake_drop.data`: generated
  workload image consumed by the driver.

Run from the repository root:

```bash
sim-verilator/testcases/l2_handshake_drop/scripts/build_workload.sh
timeout 300 sim-verilator/testcases/l2_handshake_drop/scripts/run_sim_verilator_workload.sh
```

If the assertion fires at cycle `C`, rerun with a narrow waveform window for
box/scope traceback:

```bash
timeout 300 sim-verilator/testcases/l2_handshake_drop/scripts/run_sim_verilator_workload.sh --waveform-window $((C - 200)) $((C + 100))
```

The FST is written under `sim-verilator/logs/`. Start from `L2cache.Scheduler`
signals `directory.io.result`, `dir_result_buffer.io.enq`,
`dir_result_buffer.io.deq`, `schedule.d`, and `sourceD.io.req`, then trace
upstream/downstream boxes from that point.

The first `sim-verilator` build can be slow because it regenerates and compiles
the Verilator model. After the model is built, rerunning this testcase should
primarily exercise the workload and trip the assertion if the buggy handshake
window is reached.

Set `FUZZGPU_ROOT` or `VENTUS_ENV` before running the script if the environment
script is in a different location.

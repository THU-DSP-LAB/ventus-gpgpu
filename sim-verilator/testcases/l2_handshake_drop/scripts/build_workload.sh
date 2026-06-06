#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
CASE_DIR="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
KERNEL_DIR="${CASE_DIR}/kernel"
OUT_DIR="${CASE_DIR}/image"

VENTUS_INSTALL_PREFIX="${VENTUS_INSTALL_PREFIX:-/home/ain/fuzz_gpgpu/fuzzgpu/targets/ventus-env/install}"
CLANG="${CLANG:-${VENTUS_INSTALL_PREFIX}/bin/clang}"
ASSEMBLE_SH="${ASSEMBLE_SH:-${VENTUS_INSTALL_PREFIX}/lib/scripts/assemble.sh}"

"${CLANG}" --target=riscv64-unknown-elf -march=rv64g -mabi=lp64 -nostdlib \
  -Wl,-Ttext=0x80000000 \
  "${KERNEL_DIR}/l2_handshake_drop.s" \
  -o "${KERNEL_DIR}/l2_handshake_drop.riscv"

VENTUS_INSTALL_PREFIX="${VENTUS_INSTALL_PREFIX}" \
  bash "${ASSEMBLE_SH}" "${KERNEL_DIR}/l2_handshake_drop"

"${SCRIPT_DIR}/build_workload_image.py" \
  --vmem "${KERNEL_DIR}/l2_handshake_drop.vmem" \
  --out-dir "${OUT_DIR}"

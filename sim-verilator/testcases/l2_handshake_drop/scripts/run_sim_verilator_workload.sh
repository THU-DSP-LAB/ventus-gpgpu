#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
CASE_DIR="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
REPO_ROOT="$(cd -- "${SCRIPT_DIR}/../../../../" && pwd)"
FUZZGPU_ROOT="${FUZZGPU_ROOT:-/home/ain/fuzz_gpgpu/fuzzgpu}"
VENTUS_ENV="${VENTUS_ENV:-${FUZZGPU_ROOT}/targets/ventus_env.sh}"
ARGS_FILE="${CASE_DIR}/image/ventus_cmdargs.txt"
EXTRA_ARGS=()

while [[ $# -gt 0 ]]; do
  case "$1" in
    --args-file)
      if [[ -z "${2:-}" ]]; then
        echo "usage: $0 [--args-file FILE] [--waveform-window BEGIN END]" >&2
        exit 2
      fi
      ARGS_FILE="$2"
      shift 2
      ;;
    --waveform-window)
      if [[ -z "${2:-}" || -z "${3:-}" ]]; then
        echo "usage: $0 [--args-file FILE] [--waveform-window BEGIN END]" >&2
        exit 2
      fi
      EXTRA_ARGS=(--waveform-window "$2" "$3")
      shift 3
      ;;
    *)
      echo "unknown argument: $1" >&2
      exit 2
      ;;
  esac
done

if [[ -f "${VENTUS_ENV}" ]]; then
  # Provides the local firtool/verilator binaries used by this environment.
  source "${VENTUS_ENV}"
fi

cd "${REPO_ROOT}/sim-verilator"
make
exec build/driver_example/debug/sim-VentusRTL -f "${ARGS_FILE}" "${EXTRA_ARGS[@]}"

#!/usr/bin/env bash
# dma_runtime_events.sh — unified DMA runtime suite / extract / compare tool.

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SUITE_DIR="$ROOT_DIR/test_run_dir/fullgpu_dma_single_instruction"
ARTIFACT_ROOT="$SUITE_DIR/artifacts"
VENTUS_ENV_DIR="/home/liyb/ventus-env"

usage() {
  cat <<'USAGE_EOF'
usage:
  dma_runtime_events.sh suite [--mode smoke|full] [--continue-on-fail]
  dma_runtime_events.sh extract <case_id> <backend> <status> <rc> <run_log> <expected_events.json> <runtime_dump.json> <out_observed.json>
  dma_runtime_events.sh compare <expected_events.json> <rtlsim_observed.json> <spike_observed.json> <out_compare.json> [--strict]

notes:
  - `suite` is the full-GPU single-instruction DMA validation entrypoint.
  - For convenience, passing suite options directly also works:
      dma_runtime_events.sh --mode smoke
USAGE_EOF
}

json_escape() {
  local raw="$1"
  raw=${raw//\\/\\\\}
  raw=${raw//\"/\\\"}
  raw=${raw//$'\n'/\\n}
  raw=${raw//$'\r'/}
  printf '%s' "$raw"
}

sha256_or_empty() {
  local f="$1"
  if [[ -f "$f" ]]; then
    sha256sum "$f" | awk '{print $1}'
  else
    printf ''
  fi
}

strip_ansi() {
  sed -E 's/\x1B\[[0-9;]*[[:alpha:]]//g'
}

as_bool() {
  if [[ "$1" == "true" ]]; then
    printf 'true'
  else
    printf 'false'
  fi
}

extract_string_field() {
  local file="$1"
  local key="$2"
  grep -oE "\"${key}\"[[:space:]]*:[[:space:]]*\"[^\"]+\"" "$file" | head -n1 | sed -E 's/.*"([^"]+)"/\1/'
}

extract_scalar_field() {
  local file="$1"
  local key="$2"
  local raw
  raw=$(grep -oE "\"${key}\"[[:space:]]*:[[:space:]]*(\"[^\"]*\"|true|false|null|[0-9]+)" "$file" | head -n1 | sed -E "s/^\"${key}\"[[:space:]]*:[[:space:]]*//")
  raw=${raw#\"}
  raw=${raw%\"}
  printf '%s' "$raw"
}

cmd_suite() {
  local MODE="full"
  local STOP_ON_FAIL=1

  while [[ $# -gt 0 ]]; do
    case "$1" in
      --mode)
        MODE="${2:-}"
        shift 2
        ;;
      --continue-on-fail)
        STOP_ON_FAIL=0
        shift
        ;;
      -h|--help)
        usage
        exit 0
        ;;
      *)
        echo "[FAIL] Unknown argument: $1" >&2
        echo "usage: dma_runtime_events.sh suite [--mode smoke|full] [--continue-on-fail]" >&2
        exit 2
        ;;
    esac
  done

  if [[ "$MODE" != "smoke" && "$MODE" != "full" ]]; then
    echo "[FAIL] --mode must be smoke or full, got: $MODE" >&2
    exit 2
  fi

  local FULL_CASES=(
    case_a_single_block
    case_b_cross_cacheline
    case_c_min_copysize
    case_d_fence_dependency
    case_e_inflight_sync
  )
  local SMOKE_CASES=(
    case_a_single_block
    case_d_fence_dependency
  )
  local CASES
  local STRICT_COMPARE

  if [[ "$MODE" == "smoke" ]]; then
    CASES=("${SMOKE_CASES[@]}")
    STRICT_COMPARE=0
  else
    CASES=("${FULL_CASES[@]}")
    STRICT_COMPARE=1
  fi

  local RUN_ID
  local RUN_DIR
  local SUMMARY_JSON
  local SUMMARY_MD
  local first_case=1
  local any_rtlsim_fail=0
  local any_spike_nonpass=0
  local any_compare_fail=0

  env_guard() {
    if [[ ! -d "$VENTUS_ENV_DIR" ]]; then
      echo "[FAIL] ventus-env directory not found at $VENTUS_ENV_DIR"
      return 10
    fi

    pushd "$VENTUS_ENV_DIR" >/dev/null

    if [[ ! -f ./pocl/build/examples/vecadd/vecadd ]]; then
      echo "[FAIL] vecadd binary not found in ventus-env"
      popd >/dev/null
      return 10
    fi

    # shellcheck disable=SC1091
    source env.sh

    if ! ldd ./pocl/build/examples/vecadd/vecadd | grep -q "ventus-env/install/lib/libOpenCL.so.1"; then
      echo "[FAIL] runtime OpenCL binding is not ventus-env install/lib/libOpenCL.so.1"
      popd >/dev/null
      return 10
    fi

    if ! env | grep -q "OCL_ICD_VENDORS=.*/ventus-env/install/lib/libpocl.so"; then
      echo "[FAIL] OCL_ICD_VENDORS is not bound to ventus-env libpocl.so"
      popd >/dev/null
      return 10
    fi

    set +e
    VENTUS_BACKEND=nonexist ./pocl/build/examples/vecadd/vecadd 64 64 >/tmp/ventus_dma_suite_backend_probe.log 2>&1
    local probe_rc=$?
    set -e
    if [[ $probe_rc -eq 0 ]]; then
      echo "[FAIL] backend probe should fail but succeeded"
      popd >/dev/null
      return 10
    fi

    popd >/dev/null
    return 0
  }

  run_backend() {
    local case_id="$1"
    local backend="$2"
    local out_dir="$3"
    local case_src_dir="$SUITE_DIR/$case_id"
    local events_src="$case_src_dir/events.json"
    local dump_out="$out_dir/runtime_dump.json"
    local rc
    local status="pass"

    mkdir -p "$out_dir"

    set +e
    DMA_SUITE_DUMP_JSON="$dump_out" bash "$case_src_dir/run_case.sh" "$backend" >"$out_dir/run.log" 2>&1
    rc=$?
    set -e

    if [[ $rc -eq 200 ]]; then
      status="skip"
    elif [[ $rc -ne 0 ]]; then
      status="fail"
    fi

    echo "$rc" >"$out_dir/rc.txt"
    echo "$status" >"$out_dir/status.txt"

    cmd_extract \
      "$case_id" \
      "$backend" \
      "$status" \
      "$rc" \
      "$out_dir/run.log" \
      "$events_src" \
      "$dump_out" \
      "$out_dir/observed_runtime.json"
  }

  write_case_result_md() {
    local case_id="$1"
    local case_run_dir="$2"
    local rtlsim_status="$3"
    local spike_status="$4"
    local compare_status="$5"

    cat >"$case_run_dir/result.md" <<EOF_MD
# $case_id

- rtlsim_status: $rtlsim_status
- spike_status: $spike_status
- compare_status: $compare_status
- rtlsim_log: rtlsim/run.log
- spike_log: spike/run.log
- rtlsim_observed: rtlsim/observed_runtime.json
- spike_observed: spike/observed_runtime.json
- compare_json: compare/compare_result.json
EOF_MD
  }

  write_waveform_index() {
    local case_id="$1"
    local case_run_dir="$2"
    local case_src_dir="$SUITE_DIR/$case_id"
    local hint_file="$case_src_dir/waveform_path.txt"

    if [[ -f "$hint_file" ]]; then
      local waveform_path
      waveform_path="$(<"$hint_file")"
      if [[ -f "$ROOT_DIR/$waveform_path" ]]; then
        cat >"$case_run_dir/waveform_index.md" <<EOF_WAVE
# Waveform Index

- waveform: $waveform_path
- note: file exists and can be inspected for this case
EOF_WAVE
        return
      fi
      cat >"$case_run_dir/waveform_index.md" <<EOF_WAVE
# Waveform Index

- waveform: $waveform_path
- note: file not found in current run window
EOF_WAVE
      return
    fi

    cat >"$case_run_dir/waveform_index.md" <<'EOF_WAVE'
# Waveform Index

- note: no waveform hint declared for this case
EOF_WAVE
  }

  RUN_ID="$(date +%Y-%m-%d_%H-%M-%S)"
  RUN_DIR="$ARTIFACT_ROOT/$RUN_ID"
  mkdir -p "$RUN_DIR"

  echo "[INFO] run_id=$RUN_ID"
  echo "[INFO] mode=$MODE"
  echo "[INFO] strict_compare=$STRICT_COMPARE"

  echo "[1/5] Runtime binding guard"
  if ! env_guard; then
    echo "[FAIL] runtime guard failed"
    exit 10
  fi

  echo "[2/5] Execute case backends"

  SUMMARY_JSON="$RUN_DIR/summary.json"
  SUMMARY_MD="$RUN_DIR/summary.md"

  printf "# DMA FullGPU Single-Instruction Summary\n\n" >"$SUMMARY_MD"
  printf -- "- mode: %s\n" "$MODE" >>"$SUMMARY_MD"
  printf -- "- strict_compare: %s\n\n" "$STRICT_COMPARE" >>"$SUMMARY_MD"
  printf "| case | rtlsim | spike | compare |\n" >>"$SUMMARY_MD"
  printf "|---|---|---|---|\n" >>"$SUMMARY_MD"

  printf "{\n" >"$SUMMARY_JSON"
  printf "  \"run_id\": \"%s\",\n" "$RUN_ID" >>"$SUMMARY_JSON"
  printf "  \"mode\": \"%s\",\n" "$MODE" >>"$SUMMARY_JSON"
  printf "  \"strict_compare\": %s,\n" "$STRICT_COMPARE" >>"$SUMMARY_JSON"
  printf "  \"cases\": [\n" >>"$SUMMARY_JSON"

  for case_id in "${CASES[@]}"; do
    echo "[CASE] $case_id"

    local case_run_dir="$RUN_DIR/$case_id"
    mkdir -p "$case_run_dir/rtlsim" "$case_run_dir/spike" "$case_run_dir/compare"

    run_backend "$case_id" "rtlsim" "$case_run_dir/rtlsim"
    run_backend "$case_id" "spike" "$case_run_dir/spike"

    local compare_cmd=(
      "$0" compare
      "$SUITE_DIR/$case_id/events.json"
      "$case_run_dir/rtlsim/observed_runtime.json"
      "$case_run_dir/spike/observed_runtime.json"
      "$case_run_dir/compare/compare_result.json"
    )
    if [[ $STRICT_COMPARE -eq 1 ]]; then
      compare_cmd+=("--strict")
    fi

    set +e
    "${compare_cmd[@]}"
    local compare_rc=$?
    set -e
    if [[ ! -f "$case_run_dir/compare/compare_result.json" ]]; then
      cat >"$case_run_dir/compare/compare_result.json" <<EOF_COMPARE
{
  "case": "$case_id",
  "status": "fail",
  "reason": "compare script exited without output",
  "first_mismatch": {
    "kind": "compare_runner",
    "detail": "compare_rc=$compare_rc"
  }
}
EOF_COMPARE
    fi

    local rtlsim_status spike_status compare_status case_failed=0
    rtlsim_status="$(<"$case_run_dir/rtlsim/status.txt")"
    spike_status="$(<"$case_run_dir/spike/status.txt")"
    compare_status="$(extract_string_field "$case_run_dir/compare/compare_result.json" "status")"

    if [[ "$rtlsim_status" != "pass" ]]; then
      any_rtlsim_fail=1
      case_failed=1
    fi

    if [[ "$MODE" == "full" && "$spike_status" != "pass" ]]; then
      any_spike_nonpass=1
      case_failed=1
    fi

    if [[ "$compare_status" == "fail" || ( "$MODE" == "full" && "$compare_status" != "pass" ) || $compare_rc -ne 0 ]]; then
      any_compare_fail=1
      case_failed=1
    fi

    write_case_result_md "$case_id" "$case_run_dir" "$rtlsim_status" "$spike_status" "$compare_status"
    write_waveform_index "$case_id" "$case_run_dir"

    printf "| %s | %s | %s | %s |\n" "$case_id" "$rtlsim_status" "$spike_status" "$compare_status" >>"$SUMMARY_MD"

    if [[ $first_case -eq 0 ]]; then
      printf ",\n" >>"$SUMMARY_JSON"
    fi
    first_case=0
    printf "    {\"case\":\"%s\",\"rtlsim_status\":\"%s\",\"spike_status\":\"%s\",\"compare_status\":\"%s\"}" \
      "$case_id" "$rtlsim_status" "$spike_status" "$compare_status" >>"$SUMMARY_JSON"

    if [[ $STOP_ON_FAIL -eq 1 && $case_failed -eq 1 ]]; then
      echo "[STOP] stop-on-fail is enabled, break at $case_id"
      break
    fi
  done

  printf "\n  ]\n}\n" >>"$SUMMARY_JSON"

  echo "[3/5] Summary generated"
  echo "- summary.json: $SUMMARY_JSON"
  echo "- summary.md: $SUMMARY_MD"

  echo "[4/5] Exit code decision"
  if [[ $any_rtlsim_fail -ne 0 ]]; then
    echo "[FAIL] at least one rtlsim case failed"
    exit 12
  fi
  if [[ "$MODE" == "full" && $any_spike_nonpass -ne 0 ]]; then
    echo "[FAIL] at least one spike case is non-pass under full strict mode"
    exit 13
  fi
  if [[ $any_compare_fail -ne 0 ]]; then
    echo "[FAIL] compare gate failed"
    exit 15
  fi

  echo "[5/5] Suite completed"
}

cmd_extract() {
  if [[ $# -ne 8 ]]; then
    echo "usage: dma_runtime_events.sh extract <case_id> <backend> <status> <rc> <run_log> <expected_events.json> <runtime_dump.json> <out_observed.json>" >&2
    exit 2
  fi

  local CASE_ID="$1"
  local BACKEND="$2"
  local STATUS="$3"
  local RC="$4"
  local RUN_LOG="$5"
  local EXPECTED_JSON="$6"
  local RUNTIME_DUMP="$7"
  local OUT_JSON="$8"

  mkdir -p "$(dirname "$OUT_JSON")"

  local extract_case_meta
  extract_case_meta() {
    local key="$1"
    local line
    line=$(grep -m1 -E "^\[CASE_META\] ${key}=" "$RUN_LOG" || true)
    printf '%s' "${line#*=}"
  }

  local run_log_exists=false
  local expected_exists=false
  local dump_exists=false

  [[ -f "$RUN_LOG" ]] && run_log_exists=true
  [[ -f "$EXPECTED_JSON" ]] && expected_exists=true
  [[ -f "$RUNTIME_DUMP" ]] && dump_exists=true

  local run_log_sha256
  local expected_sha256
  local dump_sha256
  run_log_sha256=$(sha256_or_empty "$RUN_LOG")
  expected_sha256=$(sha256_or_empty "$EXPECTED_JSON")
  dump_sha256=$(sha256_or_empty "$RUNTIME_DUMP")

  local module_selector=""
  local vecadd_backend=""
  local vecadd_args=""
  local case_id_meta=""
  local first_error=""

  local all_tests_passed=false
  local ok_marker=false
  local skip_marker=false

  local tests_succeeded="null"
  local tests_failed="null"
  local tests_canceled="null"
  local tests_ignored="null"
  local tests_pending="null"

  if [[ -f "$RUN_LOG" ]]; then
    module_selector=$(extract_case_meta "module_test_selector")
    vecadd_backend=$(extract_case_meta "vecadd_backend")
    vecadd_args=$(extract_case_meta "vecadd_args")
    case_id_meta=$(extract_case_meta "case_id")

    if strip_ansi <"$RUN_LOG" | grep -q "All tests passed\."; then
      all_tests_passed=true
    fi
    if grep -qE '^OK$' "$RUN_LOG"; then
      ok_marker=true
    fi
    if grep -q "\[SKIP\]" "$RUN_LOG"; then
      skip_marker=true
    fi

    local tests_line
    tests_line=$(strip_ansi <"$RUN_LOG" | grep -m1 -E 'Tests:[[:space:]]*succeeded' || true)
    if [[ -n "$tests_line" ]]; then
      local succ fail cancel ignored pending
      succ=$(printf '%s' "$tests_line" | sed -nE 's/.*succeeded ([0-9]+).*/\1/p')
      fail=$(printf '%s' "$tests_line" | sed -nE 's/.*failed ([0-9]+).*/\1/p')
      cancel=$(printf '%s' "$tests_line" | sed -nE 's/.*canceled ([0-9]+).*/\1/p')
      ignored=$(printf '%s' "$tests_line" | sed -nE 's/.*ignored ([0-9]+).*/\1/p')
      pending=$(printf '%s' "$tests_line" | sed -nE 's/.*pending ([0-9]+).*/\1/p')
      [[ -n "$succ" ]] && tests_succeeded="$succ"
      [[ -n "$fail" ]] && tests_failed="$fail"
      [[ -n "$cancel" ]] && tests_canceled="$cancel"
      [[ -n "$ignored" ]] && tests_ignored="$ignored"
      [[ -n "$pending" ]] && tests_pending="$pending"
    fi

    first_error=$(strip_ansi <"$RUN_LOG" | grep -m1 -E '\[FAIL\]|AssertionError|Exception|FAILED|error:' || true)
  fi

  local generated_at
  generated_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)

  local escaped_case_id_meta escaped_module_selector escaped_vecadd_backend escaped_vecadd_args
  local escaped_first_error escaped_run_log escaped_expected_json escaped_runtime_dump
  escaped_case_id_meta=$(json_escape "$case_id_meta")
  escaped_module_selector=$(json_escape "$module_selector")
  escaped_vecadd_backend=$(json_escape "$vecadd_backend")
  escaped_vecadd_args=$(json_escape "$vecadd_args")
  escaped_first_error=$(json_escape "$first_error")
  escaped_run_log=$(json_escape "$RUN_LOG")
  escaped_expected_json=$(json_escape "$EXPECTED_JSON")
  escaped_runtime_dump=$(json_escape "$RUNTIME_DUMP")

  cat >"$OUT_JSON" <<EOF
{
  "schema_version": "obs1.1",
  "case": "$(json_escape "$CASE_ID")",
  "backend": "$(json_escape "$BACKEND")",
  "status": "$(json_escape "$STATUS")",
  "rc": $RC,
  "generated_at": "$generated_at",
  "case_id_meta": "$escaped_case_id_meta",
  "module_test_selector": "$escaped_module_selector",
  "vecadd_backend": "$escaped_vecadd_backend",
  "vecadd_args": "$escaped_vecadd_args",
  "all_tests_passed": $(as_bool "$all_tests_passed"),
  "ok_marker": $(as_bool "$ok_marker"),
  "skip_marker": $(as_bool "$skip_marker"),
  "tests_succeeded": $tests_succeeded,
  "tests_failed": $tests_failed,
  "tests_canceled": $tests_canceled,
  "tests_ignored": $tests_ignored,
  "tests_pending": $tests_pending,
  "first_error": "$escaped_first_error",
  "expected_path": "$escaped_expected_json",
  "expected_sha256": "$expected_sha256",
  "expected_exists": $(as_bool "$expected_exists"),
  "run_log_path": "$escaped_run_log",
  "run_log_sha256": "$run_log_sha256",
  "run_log_exists": $(as_bool "$run_log_exists"),
  "runtime_dump_path": "$escaped_runtime_dump",
  "runtime_dump_sha256": "$dump_sha256",
  "runtime_dump_exists": $(as_bool "$dump_exists")
}
EOF
}

cmd_compare() {
  local STRICT=0
  if [[ "${5:-}" == "--strict" ]]; then
    STRICT=1
  fi

  if [[ $# -ne 4 && $# -ne 5 ]]; then
    echo "usage: dma_runtime_events.sh compare <expected_events.json> <rtlsim_observed.json> <spike_observed.json> <out_compare.json> [--strict]" >&2
    exit 2
  fi

  local EXPECTED_JSON="$1"
  local RTL_JSON="$2"
  local SPIKE_JSON="$3"
  local OUT_JSON="$4"

  mkdir -p "$(dirname "$OUT_JSON")"

  local extract_expected_vecadd_args
  extract_expected_vecadd_args() {
    local line
    line=$(grep -oE '"vecadd_args"[[:space:]]*:[[:space:]]*\[[[:space:]]*[0-9]+[[:space:]]*,[[:space:]]*[0-9]+[[:space:]]*\]' "$EXPECTED_JSON" | head -n1 || true)
    if [[ -z "$line" ]]; then
      printf ''
      return
    fi
    printf '%s' "$line" | sed -E 's/.*\[[[:space:]]*([0-9]+)[[:space:]]*,[[:space:]]*([0-9]+)[[:space:]]*\].*/\1 \2/'
  }

  local write_result
  write_result() {
    local case_id="$1"
    local status="$2"
    local reason="$3"
    local mismatch_kind="$4"
    local mismatch_detail="$5"

    local escaped_reason escaped_detail
    escaped_reason=$(json_escape "$reason")
    escaped_detail=$(json_escape "$mismatch_detail")

    if [[ "$status" == "pass" ]]; then
      cat >"$OUT_JSON" <<JSON
{
  "case": "$case_id",
  "status": "pass",
  "reason": "$escaped_reason",
  "first_mismatch": null
}
JSON
    else
      cat >"$OUT_JSON" <<JSON
{
  "case": "$case_id",
  "status": "$status",
  "reason": "$escaped_reason",
  "first_mismatch": {
    "kind": "$mismatch_kind",
    "detail": "$escaped_detail"
  }
}
JSON
    fi
  }

  if [[ ! -f "$EXPECTED_JSON" ]]; then
    write_result "unknown" "fail" "expected events template missing" "missing_input" "$EXPECTED_JSON"
    exit 1
  fi

  if [[ ! -f "$RTL_JSON" ]]; then
    if [[ $STRICT -eq 1 ]]; then
      write_result "unknown" "fail" "rtlsim observed file missing under strict mode" "missing_input" "$RTL_JSON"
      exit 1
    fi
    write_result "unknown" "skip" "rtlsim observed file missing" "missing_input" "$RTL_JSON"
    exit 0
  fi

  if [[ ! -f "$SPIKE_JSON" ]]; then
    local local_case
    local_case=$(extract_string_field "$RTL_JSON" "case")
    if [[ $STRICT -eq 1 ]]; then
      write_result "$local_case" "fail" "spike observed file missing under strict mode" "missing_input" "$SPIKE_JSON"
      exit 1
    fi
    write_result "$local_case" "skip" "spike observed file missing" "missing_input" "$SPIKE_JSON"
    exit 0
  fi

  local CASE_ID RTL_STATUS SPIKE_STATUS
  CASE_ID=$(extract_string_field "$RTL_JSON" "case")
  RTL_STATUS=$(extract_string_field "$RTL_JSON" "status")
  SPIKE_STATUS=$(extract_string_field "$SPIKE_JSON" "status")

  if [[ "$RTL_STATUS" != "pass" || "$SPIKE_STATUS" != "pass" ]]; then
    if [[ $STRICT -eq 1 ]]; then
      write_result "$CASE_ID" "fail" "non-pass backend status under strict mode" "backend_status" "rtlsim=$RTL_STATUS spike=$SPIKE_STATUS"
      exit 1
    fi
    if [[ "$RTL_STATUS" == "fail" || "$SPIKE_STATUS" == "fail" ]]; then
      write_result "$CASE_ID" "fail" "backend failed" "backend_status" "rtlsim=$RTL_STATUS spike=$SPIKE_STATUS"
      exit 1
    fi
    write_result "$CASE_ID" "skip" "backend skipped in non-strict mode" "backend_status" "rtlsim=$RTL_STATUS spike=$SPIKE_STATUS"
    exit 0
  fi

  local EXPECTED_SHA RTL_EXPECTED_SHA SPIKE_EXPECTED_SHA
  EXPECTED_SHA=$(sha256sum "$EXPECTED_JSON" | awk '{print $1}')
  RTL_EXPECTED_SHA=$(extract_string_field "$RTL_JSON" "expected_sha256")
  SPIKE_EXPECTED_SHA=$(extract_string_field "$SPIKE_JSON" "expected_sha256")

  if [[ "$RTL_EXPECTED_SHA" != "$EXPECTED_SHA" || "$SPIKE_EXPECTED_SHA" != "$EXPECTED_SHA" ]]; then
    write_result "$CASE_ID" "fail" "expected template hash mismatch" "expected_hash" "expected=$EXPECTED_SHA rtlsim=$RTL_EXPECTED_SHA spike=$SPIKE_EXPECTED_SHA"
    exit 1
  fi

  local EXPECTED_MODULE_SELECTOR EXPECTED_VECADD_ARGS EXPECTED_CROSS_CHECK
  EXPECTED_MODULE_SELECTOR=$(grep -oE '"module_test_selector"[[:space:]]*:[[:space:]]*"[^"]+"' "$EXPECTED_JSON" | head -n1 | sed -E 's/.*"([^"]+)"/\1/' || true)
  EXPECTED_VECADD_ARGS=$(extract_expected_vecadd_args)
  EXPECTED_CROSS_CHECK=$(grep -oE '"cross_backend_check"[[:space:]]*:[[:space:]]*"[^"]+"' "$EXPECTED_JSON" | head -n1 | sed -E 's/.*"([^"]+)"/\1/' || true)

  local RTL_MODULE_SELECTOR RTL_VECADD_ARGS SPIKE_VECADD_ARGS
  RTL_MODULE_SELECTOR=$(extract_string_field "$RTL_JSON" "module_test_selector")
  RTL_VECADD_ARGS=$(extract_string_field "$RTL_JSON" "vecadd_args")
  SPIKE_VECADD_ARGS=$(extract_string_field "$SPIKE_JSON" "vecadd_args")

  if [[ -n "$EXPECTED_MODULE_SELECTOR" && "$RTL_MODULE_SELECTOR" != "$EXPECTED_MODULE_SELECTOR" ]]; then
    write_result "$CASE_ID" "fail" "module test selector mismatch" "module_selector" "expected=$EXPECTED_MODULE_SELECTOR rtlsim=$RTL_MODULE_SELECTOR"
    exit 1
  fi

  if [[ -n "$EXPECTED_VECADD_ARGS" && ( "$RTL_VECADD_ARGS" != "$EXPECTED_VECADD_ARGS" || "$SPIKE_VECADD_ARGS" != "$EXPECTED_VECADD_ARGS" ) ]]; then
    write_result "$CASE_ID" "fail" "vecadd args mismatch against template" "vecadd_args" "expected=$EXPECTED_VECADD_ARGS rtlsim=$RTL_VECADD_ARGS spike=$SPIKE_VECADD_ARGS"
    exit 1
  fi

  if [[ -n "$EXPECTED_CROSS_CHECK" && "$EXPECTED_CROSS_CHECK" != "runtime_dump_sha256_equal" ]]; then
    write_result "$CASE_ID" "fail" "unsupported cross backend check policy" "cross_backend_check" "expected_policy=$EXPECTED_CROSS_CHECK"
    exit 1
  fi

  local RTL_DUMP_EXISTS SPIKE_DUMP_EXISTS RTL_DUMP_SHA SPIKE_DUMP_SHA
  RTL_DUMP_EXISTS=$(extract_scalar_field "$RTL_JSON" "runtime_dump_exists")
  SPIKE_DUMP_EXISTS=$(extract_scalar_field "$SPIKE_JSON" "runtime_dump_exists")
  RTL_DUMP_SHA=$(extract_string_field "$RTL_JSON" "runtime_dump_sha256")
  SPIKE_DUMP_SHA=$(extract_string_field "$SPIKE_JSON" "runtime_dump_sha256")

  if [[ "$RTL_DUMP_EXISTS" != "true" || "$SPIKE_DUMP_EXISTS" != "true" ]]; then
    write_result "$CASE_ID" "fail" "runtime dump missing" "runtime_dump" "rtlsim_exists=$RTL_DUMP_EXISTS spike_exists=$SPIKE_DUMP_EXISTS"
    exit 1
  fi

  if [[ -z "$RTL_DUMP_SHA" || -z "$SPIKE_DUMP_SHA" ]]; then
    write_result "$CASE_ID" "fail" "runtime dump hash missing" "runtime_dump" "rtlsim_sha=$RTL_DUMP_SHA spike_sha=$SPIKE_DUMP_SHA"
    exit 1
  fi

  if [[ "$RTL_DUMP_SHA" != "$SPIKE_DUMP_SHA" ]]; then
    write_result "$CASE_ID" "fail" "runtime dump hash mismatch" "runtime_dump" "rtlsim_sha=$RTL_DUMP_SHA spike_sha=$SPIKE_DUMP_SHA"
    exit 1
  fi

  local RTL_SKIP_MARKER RTL_TESTS_SUCCEEDED RTL_TESTS_FAILED
  RTL_SKIP_MARKER=$(extract_scalar_field "$RTL_JSON" "skip_marker")
  RTL_TESTS_SUCCEEDED=$(extract_scalar_field "$RTL_JSON" "tests_succeeded")
  RTL_TESTS_FAILED=$(extract_scalar_field "$RTL_JSON" "tests_failed")

  if [[ "$RTL_SKIP_MARKER" == "true" ]]; then
    write_result "$CASE_ID" "fail" "rtlsim log contains skip marker" "module_test" "skip_marker=true"
    exit 1
  fi

  if [[ "$RTL_MODULE_SELECTOR" != "none" ]]; then
    if [[ "$RTL_TESTS_SUCCEEDED" == "null" || -z "$RTL_TESTS_SUCCEEDED" || "$RTL_TESTS_SUCCEEDED" -lt 1 ]]; then
      write_result "$CASE_ID" "fail" "module test execution count is insufficient" "module_test" "tests_succeeded=$RTL_TESTS_SUCCEEDED"
      exit 1
    fi
  fi

  if [[ -n "$RTL_TESTS_FAILED" && "$RTL_TESTS_FAILED" != "0" && "$RTL_TESTS_FAILED" != "null" ]]; then
    write_result "$CASE_ID" "fail" "module test reported failures" "module_test" "tests_failed=$RTL_TESTS_FAILED"
    exit 1
  fi

  write_result "$CASE_ID" "pass" "expected hash, selector, vecadd args, and runtime dump hashes all matched" "" ""
  exit 0
}

if [[ $# -lt 1 ]]; then
  usage >&2
  exit 2
fi

SUBCMD="$1"
if [[ "$SUBCMD" == "suite" || "$SUBCMD" == "extract" || "$SUBCMD" == "compare" || "$SUBCMD" == "-h" || "$SUBCMD" == "--help" || "$SUBCMD" == "help" ]]; then
  shift
else
  SUBCMD="suite"
fi

case "$SUBCMD" in
  suite)
    cmd_suite "$@"
    ;;
  extract)
    cmd_extract "$@"
    ;;
  compare)
    cmd_compare "$@"
    ;;
  -h|--help|help)
    usage
    ;;
  *)
    echo "unknown subcommand: $SUBCMD" >&2
    usage >&2
    exit 2
    ;;
esac

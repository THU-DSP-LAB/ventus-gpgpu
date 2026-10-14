"""Exercise default Make configuration and generated nocache host parameters."""
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[2]


class CiBuildConfigurationTests(unittest.TestCase):
    def test_installed_public_header_is_independent_of_backend_install_order(self):
        backends = (("sim-verilator", "verilate.mk"),
                    ("sim-verilator-nocache", "verilate.mk"),
                    ("sim-verilator", "gvm.mk"),
                    ("sim-verilator-nocache", "gvm.mk"))
        for order in (backends, tuple(reversed(backends))):
            with self.subTest(order=order), tempfile.TemporaryDirectory() as temp:
                path = Path(temp)
                prefix = path / "install"
                library = path / "fixture.so"
                # Exercise installation without pretending to build an RTL model.
                library.write_bytes(b"install-recipe fixture")
                for directory, makefile in order:
                    result = subprocess.run(
                        ["make", "--no-print-directory", "-s", "-f", makefile,
                         "VLIB_NPROC_CPU=1", f"PREFIX={prefix}",
                         f"VLIB_TARGET={library}", "-o", str(library), "install"],
                        cwd=ROOT / directory, capture_output=True, text=True)
                    self.assertEqual(result.returncode, 0, result.stderr)
                    installed = prefix / "include/ventus_rtlsim.h"
                    self.assertEqual(installed.read_bytes(),
                                     (ROOT / "sim-verilator/ventus_rtlsim.h").read_bytes())
                    result = subprocess.run(
                        ["cc", "-std=c11", "-fsyntax-only", f"-I{prefix / 'include'}",
                         str(ROOT / "sim-verilator/tests/public_abi_compile_test.c")],
                        capture_output=True, text=True)
                    self.assertEqual(result.returncode, 0, result.stderr)

    def test_gvm_pmu_recipe_passes_default_and_inherited_modes(self):
        for mode in (None, "basic", "detailed"):
            with self.subTest(mode=mode), tempfile.TemporaryDirectory() as temp:
                path = Path(temp)
                params = path / "parameters.json"
                output = path / "pmu.inc"
                expected = path / "expected.inc"
                params.write_text(json.dumps({"num_sm": 1, "num_l2cache": 1}))
                env = os.environ.copy()
                env.pop("VENTUS_PMU_MODE", None)
                if mode is not None:
                    env["VENTUS_PMU_MODE"] = mode
                result = subprocess.run(
                    ["make", "--no-print-directory", "-s", "-f", "gvm.mk",
                     "VLIB_NPROC_CPU=1", f"VLIB_PARAMS_JSON={params}",
                     f"VLIB_PMU_SNAPSHOT_INC={output}", "-o", str(params), str(output)],
                    cwd=ROOT / "sim-verilator", env=env, capture_output=True, text=True)
                self.assertEqual(result.returncode, 0, result.stderr)
                subprocess.run(
                    ["python3", str(ROOT / "sim-verilator/gen_pmu_snapshot_inc.py"),
                     str(params), str(expected), mode or "off"], check=True)
                self.assertEqual(output.read_text(), expected.read_text())

    def test_pmu_default_environment_and_command_line(self):
        for inherited, explicit, expected in (
            (None, None, "off"), ("basic", None, "basic"),
            ("detailed", None, "detailed"), ("basic", "off", "off"),
        ):
            with self.subTest(inherited=inherited, explicit=explicit):
                env = os.environ.copy()
                env.pop("VENTUS_PMU_MODE", None)
                env.pop("VLIB_PMU_MODE", None)
                if inherited is not None:
                    env["VENTUS_PMU_MODE"] = inherited
                command = ["make", "--no-print-directory", "-s", "-f", "verilate.mk", "VLIB_NPROC_CPU=1"]
                if explicit is not None:
                    command.append(f"VLIB_PMU_MODE={explicit}")
                command += ["--eval", 'ci-pmu: ; @printf "%s %s\\n" "$(VLIB_PMU_MODE)" "$$VENTUS_PMU_MODE"', "ci-pmu"]
                result = subprocess.run(command, cwd=ROOT / "sim-verilator", env=env,
                                        capture_output=True, text=True)
                self.assertEqual(result.returncode, 0, result.stderr)
                self.assertEqual(result.stdout.strip(), f"{expected} {expected}")

    def test_nocache_header_uses_the_same_sm_count_as_runtime_map(self):
        for count in (1, 2):
            with self.subTest(count=count), tempfile.TemporaryDirectory() as temp:
                path = Path(temp)
                params, cpp, header = (path / name for name in ("parameters.json", "parameters.cpp", "parameters.hpp"))
                params.write_text(json.dumps({"num_sm": count}))
                result = subprocess.run(["python3", str(ROOT / "sim-verilator-nocache/json2cpp.py"),
                                         str(params), str(cpp), str(header)], capture_output=True, text=True)
                self.assertEqual(result.returncode, 0, result.stderr)
                self.assertIn(f"#define VENTUS_RTL_NUM_SM {count}", header.read_text())
                self.assertIn(f'{{ "num_sm", {count} }}', cpp.read_text())

    def test_nocache_rejects_unsupported_sm_count(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp)
            params = path / "parameters.json"
            params.write_text(json.dumps({"num_sm": 3}))
            result = subprocess.run(["python3", str(ROOT / "sim-verilator-nocache/json2cpp.py"),
                                     str(params), str(path / "parameters.cpp"), str(path / "parameters.hpp")],
                                    capture_output=True, text=True)
            self.assertNotEqual(result.returncode, 0)
            self.assertIn("num_sm=1 or 2", result.stderr)


if __name__ == "__main__":
    unittest.main()

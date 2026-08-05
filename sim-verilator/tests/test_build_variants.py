import subprocess
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def make_variables(makefile: str, assignments: dict[str, str]) -> dict[str, str]:
    variable_names = (
        "VLIB_VARIANT_KEY",
        "VLIB_DIR_BUILDOBJ",
        "VLIB_MODEL_IDENTITY_CPP",
        "APP",
        "FORK_SNAPSHOT_TEST",
        "PERSISTENT_STATE_TEST",
    )
    recipe = " ".join(f'\"{name}=$({name})\"' for name in variable_names)
    command = [
        "make",
        "-s",
        "-f",
        makefile,
        *(f"{name}={value}" for name, value in assignments.items()),
        "--eval",
        f'print-variant-vars: ; @printf "%s\\n" {recipe}',
        "print-variant-vars",
    ]
    completed = subprocess.run(
        command,
        cwd=ROOT,
        check=True,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    return dict(line.split("=", 1) for line in completed.stdout.splitlines())


class BuildVariantTests(unittest.TestCase):
    def test_rtl_variants_have_disjoint_library_host_and_test_paths(self) -> None:
        common = {"RELEASE": "1", "SAVABLE": "1", "VLIB_NPROC_CPU": "1"}
        no_trace = make_variables(
            "Makefile", {**common, "TRACE": "0", "VLIB_NPROC_SIM": "5"}
        )
        trace = make_variables(
            "Makefile", {**common, "TRACE": "1", "VLIB_NPROC_SIM": "1"}
        )
        self.assertEqual(
            no_trace["VLIB_VARIANT_KEY"], "rtl-release-savable-notrace-t5"
        )
        self.assertEqual(
            trace["VLIB_VARIANT_KEY"], "rtl-release-savable-trace-t1"
        )
        for name in (
            "VLIB_DIR_BUILDOBJ",
            "VLIB_MODEL_IDENTITY_CPP",
            "APP",
            "FORK_SNAPSHOT_TEST",
            "PERSISTENT_STATE_TEST",
        ):
            self.assertTrue(no_trace[name], name)
            self.assertTrue(trace[name], name)
            self.assertNotEqual(no_trace[name], trace[name], name)
            self.assertIn(no_trace["VLIB_VARIANT_KEY"], no_trace[name])
            self.assertIn(trace["VLIB_VARIANT_KEY"], trace[name])

    def test_gvm_variant_key_is_separate_from_rtl(self) -> None:
        gvm = make_variables(
            "gvm.mk",
            {
                "RELEASE": "1",
                "GVM_TRACE": "0",
                "VLIB_NPROC_CPU": "1",
                "VLIB_NPROC_SIM": "5",
            },
        )
        self.assertEqual(
            gvm["VLIB_VARIANT_KEY"], "gvm-release-nonsavable-notrace-t5"
        )
        self.assertIn(gvm["VLIB_VARIANT_KEY"], gvm["VLIB_DIR_BUILDOBJ"])
        self.assertIn(
            gvm["VLIB_VARIANT_KEY"], gvm["VLIB_MODEL_IDENTITY_CPP"]
        )


if __name__ == "__main__":
    unittest.main()

import importlib.util
import json
import tempfile
import unittest
from pathlib import Path


MODULE_PATH = Path(__file__).resolve().parents[1] / "gen_model_identity.py"
SPEC = importlib.util.spec_from_file_location("gen_model_identity", MODULE_PATH)
assert SPEC is not None and SPEC.loader is not None
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class ModelIdentityTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temporary = tempfile.TemporaryDirectory()
        self.root = Path(self.temporary.name)
        (self.root / "rtl").mkdir()
        (self.root / "rtl" / "dut.v").write_text("module dut; endmodule\n")
        (self.root / "sim.cpp").write_text("int simulator = 1;\n")
        self.inputs = [self.root / "rtl" / "dut.v", self.root / "sim.cpp"]

    def tearDown(self) -> None:
        self.temporary.cleanup()

    def identity(self, **overrides: object) -> dict[str, object]:
        arguments: dict[str, object] = {
            "root": self.root,
            "inputs": self.inputs,
            "variant": "rtl-release-savable-notrace-t5",
            "release_enabled": True,
            "savable_enabled": True,
            "trace_enabled": False,
            "gvm_enabled": False,
            "model_threads": 5,
        }
        arguments.update(overrides)
        return MODULE.build_identity(**arguments)

    def test_fingerprint_is_stable_across_input_order(self) -> None:
        first = self.identity()
        second = self.identity(inputs=list(reversed(self.inputs)))
        self.assertEqual(first["fingerprint"], second["fingerprint"])
        self.assertEqual(
            list(first["inputs_sha256"]), ["rtl/dut.v", "sim.cpp"]
        )

    def test_source_or_build_setting_changes_fingerprint(self) -> None:
        original = self.identity()["fingerprint"]
        (self.root / "sim.cpp").write_text("int simulator = 2;\n")
        self.assertNotEqual(original, self.identity()["fingerprint"])
        self.assertNotEqual(
            self.identity()["fingerprint"],
            self.identity(trace_enabled=True)["fingerprint"],
        )
        self.assertNotEqual(
            self.identity()["fingerprint"],
            self.identity(model_threads=1)["fingerprint"],
        )

    def test_absolute_worktree_path_is_not_part_of_identity(self) -> None:
        other_root = self.root / "other"
        (other_root / "rtl").mkdir(parents=True)
        for relative in (Path("rtl/dut.v"), Path("sim.cpp")):
            (other_root / relative).write_bytes((self.root / relative).read_bytes())
        other = MODULE.build_identity(
            root=other_root,
            inputs=[other_root / "rtl/dut.v", other_root / "sim.cpp"],
            variant="rtl-release-savable-notrace-t5",
            release_enabled=True,
            savable_enabled=True,
            trace_enabled=False,
            gvm_enabled=False,
            model_threads=5,
        )
        self.assertEqual(self.identity()["fingerprint"], other["fingerprint"])

    def test_outputs_are_machine_readable_and_compile_time_constants(self) -> None:
        cpp_path = self.root / "generated" / "model_identity.cpp"
        json_path = self.root / "generated" / "model_identity.json"
        identity = self.identity()
        MODULE.write_outputs(identity, cpp_path, json_path)
        self.assertEqual(json.loads(json_path.read_text()), identity)
        source = cpp_path.read_text()
        self.assertIn(str(identity["fingerprint"]), source)
        self.assertIn("rtl-release-savable-notrace-t5", source)
        self.assertIn("    false,\n    false,\n    5,", source)

    def test_input_tree_hashes_split_rtl_files(self) -> None:
        (self.root / "rtl" / "bank.sv").write_text("module bank; endmodule\n")
        identity = self.identity(inputs=[self.root / "sim.cpp"], input_trees=[self.root / "rtl"])
        self.assertEqual(
            list(identity["inputs_sha256"]),
            ["rtl/bank.sv", "rtl/dut.v", "sim.cpp"],
        )

    def test_rejects_outside_or_duplicate_inputs(self) -> None:
        outside = Path(self.temporary.name).parent / "outside-model-input"
        outside.write_text("outside\n")
        try:
            with self.assertRaisesRegex(ValueError, "outside the build root"):
                self.identity(inputs=[outside])
        finally:
            outside.unlink()
        with self.assertRaisesRegex(ValueError, "duplicate"):
            self.identity(inputs=[self.inputs[0], self.inputs[0]])


if __name__ == "__main__":
    unittest.main()

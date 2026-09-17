"""Host-only observer tests, independent of Verilator and a full application run."""
import json
from pathlib import Path
import subprocess
import tempfile
import unittest


class DetailedPmuTest(unittest.TestCase):
    def test_handshakes_histograms_and_censored_samples(self):
        root = Path(__file__).resolve().parents[1]
        with tempfile.TemporaryDirectory() as directory:
            binary = Path(directory) / "observer-test"
            subprocess.run(["g++", "-std=c++17", "-Wall", "-Wextra", "-Werror",
                            "-I", str(root), str(root / "tests/rt_detailed_pmu_test.cpp"),
                            "-o", str(binary)], check=True)
            output = subprocess.check_output([str(binary)], text=True)
        tagged, first, second = map(json.loads, output.splitlines())
        self.assertEqual(tagged["lsu"]["histograms"]["latency_cycles"], {"1": 1, "2": 1, "4": 1})
        self.assertEqual(tagged["lsu"]["counters"]["outstanding_at_snapshot"], 0)
        self.assertEqual(tagged["lsu"]["counters"]["busy_cycles"], 5)
        self.assertEqual(tagged["lsu"]["counters"].get("unmatched_results", 0), 0)
        self.assertEqual(first, second)
        self.assertEqual(first["post_eviction_reaccess"], 0)
        self.assertEqual(first["as_policy"], "retained_reverse_replace")
        unit = first["units"][0]["statistics"]
        self.assertEqual(unit["counters"]["issues"], 2)
        self.assertEqual(unit["counters"]["results"], 1)
        self.assertEqual(unit["counters"]["output_blocked_cycles"], 1)
        self.assertEqual(unit["counters"]["outstanding_at_snapshot"], 1)
        self.assertEqual(unit["histograms"]["latency_cycles"], {"3": 1})
        self.assertEqual(first["lsu"]["histograms"]["latency_cycles"], {"2": 1})
        self.assertEqual(first["statistics"]["counters"]["as_live_entries_at_snapshot"], 1)


if __name__ == "__main__":
    unittest.main()

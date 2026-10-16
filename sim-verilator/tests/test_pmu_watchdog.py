"""Compile the production watchdog bodies against deterministic PMU/time inputs.

No Verilator model is needed here. The integration regressions still exercise
the real model; these tests cover off/basic/detailed capability and deadlines.
"""
from pathlib import Path
import subprocess
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[2]
HARNESS = r'''
#include "ventus_rtlsim.h"
#include <cstdint>
#include <cstdlib>
#include <stdexcept>
#include <string>
#include <vector>

static bool pipeline_enabled, inst_class_enabled;
static unsigned parameter_reads;
static uint32_t rtl_parameter_u32(const char* name) {
    ++parameter_reads;
    const std::string key(name);
    if (key == "PMU_PIPELINE") return pipeline_enabled;
    if (key == "PMU_INST_CLASS") return inst_class_enabled;
    throw std::runtime_error("unexpected parameter: " + key);
}
struct Context {
    uint64_t now = 0;
    uint64_t time() const { return now; }
};
struct Logger {
    template<class... Args> void critical(Args&&...) {}
};
struct Snapshot {
    uint32_t num_sm = 1;
    std::vector<ventus_rtlsim_pipeline_pmu_t> pipeline{1};
    std::vector<ventus_rtlsim_inst_class_pmu_t> inst_class{1};
    std::vector<ventus_rtlsim_dcache_pmu_t> dcache{1};
    std::vector<ventus_rtlsim_rtcore_pmu_t> rtcore{1};
};
struct ventus_rtlsim_t {
    Context context;
    Context* contextp = &context;
    Logger log;
    Logger* logger = &log;
    ventus_rtlsim_config_t config{};
    ventus_rtlsim_step_result_t step_status{};
    Snapshot pmu_snapshot;
    uint64_t last_pmu_progress_time = 0, last_pmu_progress_value = 0;
    void update_pmu_watchdog();
};
static void require(bool condition, const char* message) {
    if (!condition) throw std::runtime_error(message);
}
'''
MAIN = r'''
int main(int argc, char** argv) {
    require(argc == 4, "arguments");
    pipeline_enabled = std::atoi(argv[1]);
    inst_class_enabled = std::atoi(argv[2]);
    const std::string scenario(argv[3]);
    ventus_rtlsim_t sim;
    sim.config.hang_timeout = 10;
    if (scenario == "off") {
        sim.context.now = 1000000;
        sim.update_pmu_watchdog();
        sim.context.now = 2000000;
        sim.update_pmu_watchdog();
        require(!sim.step_status.hang && !sim.step_status.error,
                "disabled PMU falsely reported a hang");
    } else if (scenario == "progress") {
        sim.context.now = 1;
        if (pipeline_enabled) sim.pmu_snapshot.pipeline[0].total_vector_issued = 1;
        else sim.pmu_snapshot.inst_class[0].mem_issued = 1;
        sim.update_pmu_watchdog();
        sim.context.now = 10;
        sim.update_pmu_watchdog();
        require(!sim.step_status.hang, "hang before deadline");
        sim.context.now = 11;
        sim.update_pmu_watchdog();
        require(sim.step_status.hang && sim.step_status.error,
                "enabled watchdog missed its deadline");
    } else if (scenario == "reset") {
        sim.context.now = 1;
        sim.pmu_snapshot.inst_class[0].mem_issued = 1;
        sim.update_pmu_watchdog();
        sim.context.now = 10;
        sim.pmu_snapshot.inst_class[0].mem_issued = 2;
        sim.update_pmu_watchdog();
        sim.context.now = 19;
        sim.update_pmu_watchdog();
        require(!sim.step_status.hang, "progress did not reset the deadline");
    } else {
        sim.context.now = 100;
        if (scenario == "disabled") sim.config.hang_timeout = 0;
        else if (scenario == "idle") sim.step_status.idle = true;
        else if (scenario == "error") sim.step_status.error = true;
        else if (scenario == "time_limit") sim.step_status.time_exceed = true;
        else throw std::runtime_error("unexpected scenario");
        sim.update_pmu_watchdog();
        require(!sim.step_status.hang, "inactive watchdog reported hang");
        require(sim.last_pmu_progress_time == sim.context.now, "time not reset");
    }
    require(parameter_reads <= 2, "PMU capability must not be looked up every step");
}
'''


class PmuWatchdogTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temp = tempfile.TemporaryDirectory()
        cls.addClassCleanup(cls.temp.cleanup)
        cls.binaries = []
        for backend in ("sim-verilator", "sim-verilator-nocache"):
            source = (ROOT / backend / "ventus_rtlsim_impl.cpp").read_text()
            start = source.index("void ventus_rtlsim_t::update_pmu_watchdog() {")
            end = source.index("\n}\n", start) + 3
            # Test the production implementation, not a rewritten watchdog.
            body = source[start:end]
            cpp = Path(cls.temp.name) / f"{backend}.cpp"
            binary = cpp.with_suffix("")
            cpp.write_text(HARNESS + body + MAIN)
            subprocess.run(["c++", "-std=c++20", "-O0", "-I", str(ROOT / "sim-verilator"),
                            str(cpp), "-o", str(binary)], check=True, capture_output=True)
            cls.binaries.append((backend, binary))

    def test_capabilities_and_deadlines(self):
        cases = [(0, 0, "off"), (1, 0, "progress"), (1, 1, "progress"),
                 (0, 1, "progress"), (1, 1, "reset"), (1, 0, "disabled"),
                 (1, 0, "idle"), (1, 0, "error"), (1, 0, "time_limit")]
        for backend, binary in self.binaries:
            for pipeline, inst, scenario in cases:
                with self.subTest(backend=backend, pipeline=pipeline, inst=inst,
                                  scenario=scenario):
                    result = subprocess.run([str(binary), str(pipeline), str(inst), scenario],
                                            text=True, capture_output=True)
                    self.assertEqual(result.returncode, 0, result.stderr)


if __name__ == "__main__":
    unittest.main()

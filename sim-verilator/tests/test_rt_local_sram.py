"""Check held SRAM responses using an already generated full-app RTL file.

Usage: python3 sim-verilator/tests/test_rt_local_sram.py --rtl path/to/dut.v
Only RtLocalSram and its memory modules are compiled. This intentionally uses
the full-app emitter: the legacy ChiselTest memory model can mask invalid
SyncReadMem outputs after a read-disable cycle.
"""
import argparse
import os
from pathlib import Path
import subprocess
import tempfile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--rtl", type=Path, required=True)
    args = parser.parse_args()
    rtl = args.rtl.resolve(strict=True)
    source = Path(__file__).with_name("rt_local_sram_hold.cpp").resolve()
    with tempfile.TemporaryDirectory(prefix="rt-local-sram-hold-") as directory:
        build = subprocess.run([
            "verilator", "--cc", "--exe", "--build", "-j", "4",
            "--top-module", "RtLocalSram", "--Mdir", directory,
            "-Wno-fatal", "--x-assign", "fast", str(rtl), str(source),
        ], env={**os.environ, "CCACHE_DISABLE": "1"},
            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
        if build.returncode:
            raise RuntimeError(build.stdout[-4000:])
        subprocess.run([str(Path(directory) / "VRtLocalSram")], check=True)


if __name__ == "__main__":
    main()

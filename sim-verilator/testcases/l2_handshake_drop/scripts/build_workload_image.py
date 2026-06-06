#!/usr/bin/env python3
import argparse
from pathlib import Path


def write_u64_le32_lines(f, value):
    f.write(f"{value & 0xffffffff:08x}\n")
    f.write(f"{(value >> 32) & 0xffffffff:08x}\n")


def main():
    parser = argparse.ArgumentParser(description="Build sim-verilator metadata/data for l2_handshake_drop")
    parser.add_argument("--vmem", required=True, type=Path)
    parser.add_argument("--out-dir", required=True, type=Path)
    parser.add_argument("--name", default="l2_handshake_drop")
    parser.add_argument("--num-blocks", type=lambda x: int(x, 0), default=2)
    parser.add_argument("--data-bytes", type=lambda x: int(x, 0), default=0x400000)
    parser.add_argument("--sim-time-max", type=lambda x: int(x, 0), default=10000000)
    args = parser.parse_args()

    instr_words = [line.strip() for line in args.vmem.read_text().splitlines() if line.strip()]
    if not instr_words:
        raise SystemExit(f"empty vmem: {args.vmem}")

    args.out_dir.mkdir(parents=True, exist_ok=True)
    metadata_path = args.out_dir / f"{args.name}.metadata"
    data_path = args.out_dir / f"{args.name}.data"
    args_path = args.out_dir / "ventus_cmdargs.txt"

    code_bytes = len(instr_words) * 4
    data_bytes = (args.data_bytes + 3) & ~3

    # metadata_t fields in sim-verilator/kernel.cpp, encoded as 64-bit values
    # with low 32 bits first, matching existing testcase metadata files.
    values = [
        0x80000000, # startaddr
        0,          # kernel_id, overwritten by runtime activation
        args.num_blocks, 1, 1, # kernel_size[3]
        32,         # wf_size
        8,          # wg_size
        0x90000000, # metaDataBaseAddr
        0x1000,     # ldsSize
        0x1000,     # pdsSize
        0x40,       # sgprUsage
        0x40,       # vgprUsage
        0x90004000, # pdsBaseAddr
        2,          # num_buffer
        0x80000000, # buffer_base[0]: text
        0x90000000, # buffer_base[1]: GDS/output buffer
        code_bytes, # buffer_size[0]
        data_bytes, # buffer_size[1]
        0x1000,     # buffer_allocsize[0]
        data_bytes, # buffer_allocsize[1]
    ]

    with metadata_path.open("w") as f:
        for value in values:
            write_u64_le32_lines(f, value)

    with data_path.open("w") as f:
        for word in instr_words:
            if len(word) != 8:
                raise SystemExit(f"bad vmem word {word!r}")
            f.write(word.lower() + "\n")
        for _ in range(data_bytes // 4):
            f.write("00000000\n")

    common_args = (
        "# sim-verilator arguments for the l2_handshake_drop GPU software workload\n"
        f"--kernel name={args.name},metafile={metadata_path.name},datafile={data_path.name}\n"
        f"--sim-time-max {args.sim_time_max}\n"
    )
    args_path.write_text(common_args)

    print(metadata_path)
    print(data_path)
    print(args_path)


if __name__ == "__main__":
    main()

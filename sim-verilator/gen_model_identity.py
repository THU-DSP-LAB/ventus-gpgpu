#!/usr/bin/env python3

import argparse
import hashlib
import json
from pathlib import Path


SCHEMA_VERSION = 1


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def canonical_bytes(value: object) -> bytes:
    return json.dumps(
        value, sort_keys=True, separators=(",", ":"), ensure_ascii=True
    ).encode("ascii")


def normalized_inputs(
    root: Path, inputs: list[Path], input_trees: list[Path] | None = None
) -> dict[str, str]:
    result: dict[str, str] = {}
    resolved_root = root.resolve()
    expanded_inputs = list(inputs)
    for raw_tree in input_trees or []:
        tree = raw_tree.resolve()
        if not tree.is_dir():
            raise ValueError(f"model identity input tree is missing: {raw_tree}")
        try:
            tree.relative_to(resolved_root)
        except ValueError as error:
            raise ValueError(
                f"model identity input tree is outside the build root: {raw_tree}"
            ) from error
        expanded_inputs.extend(path for path in tree.rglob("*") if path.is_file())
    for raw_path in expanded_inputs:
        path = raw_path.resolve()
        if not path.is_file():
            raise ValueError(f"model identity input is missing: {raw_path}")
        try:
            name = path.relative_to(resolved_root).as_posix()
        except ValueError as error:
            raise ValueError(
                f"model identity input is outside the build root: {raw_path}"
            ) from error
        if name in result:
            raise ValueError(f"duplicate model identity input: {name}")
        result[name] = sha256_file(path)
    return dict(sorted(result.items()))


def build_identity(
    *,
    root: Path,
    inputs: list[Path],
    variant: str,
    release_enabled: bool,
    savable_enabled: bool,
    trace_enabled: bool,
    gvm_enabled: bool,
    model_threads: int,
    input_trees: list[Path] | None = None,
) -> dict[str, object]:
    if not variant or any(character.isspace() for character in variant):
        raise ValueError("model identity variant must be nonempty and contain no whitespace")
    if model_threads <= 0:
        raise ValueError("model thread count must be positive")
    payload: dict[str, object] = {
        "schema_version": SCHEMA_VERSION,
        "settings": {
            "gvm_enabled": gvm_enabled,
            "model_threads": model_threads,
            "release_enabled": release_enabled,
            "savable_enabled": savable_enabled,
            "trace_enabled": trace_enabled,
            "variant": variant,
        },
        "inputs_sha256": normalized_inputs(root, inputs, input_trees),
    }
    return {
        **payload,
        "fingerprint": hashlib.sha256(canonical_bytes(payload)).hexdigest(),
    }


def cpp_bool(value: bool) -> str:
    return "true" if value else "false"


def write_outputs(identity: dict[str, object], cpp_path: Path, json_path: Path) -> None:
    settings = identity["settings"]
    assert isinstance(settings, dict)
    cpp_path.parent.mkdir(parents=True, exist_ok=True)
    json_path.parent.mkdir(parents=True, exist_ok=True)
    cpp_path.write_text(
        "\n".join(
            [
                '#include "model_identity.hpp"',
                "",
                "const VentusRtlModelIdentity kVentusRtlModelIdentity {",
                f'    "{identity["fingerprint"]}",',
                f'    "{settings["variant"]}",',
                f'    {cpp_bool(bool(settings["release_enabled"]))},',
                f'    {cpp_bool(bool(settings["savable_enabled"]))},',
                f'    {cpp_bool(bool(settings["trace_enabled"]))},',
                f'    {cpp_bool(bool(settings["gvm_enabled"]))},',
                f'    {int(settings["model_threads"])},',
                "};",
                "",
            ]
        ),
        encoding="ascii",
    )
    json_path.write_text(
        json.dumps(identity, indent=2, sort_keys=True) + "\n", encoding="ascii"
    )


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--input", type=Path, action="append", default=[])
    parser.add_argument("--input-tree", type=Path, action="append", default=[])
    parser.add_argument("--variant", required=True)
    parser.add_argument("--release", type=int, choices=(0, 1), required=True)
    parser.add_argument("--savable", type=int, choices=(0, 1), required=True)
    parser.add_argument("--trace", type=int, choices=(0, 1), required=True)
    parser.add_argument("--gvm", type=int, choices=(0, 1), required=True)
    parser.add_argument("--model-threads", type=int, required=True)
    parser.add_argument("--output-cpp", type=Path, required=True)
    parser.add_argument("--output-json", type=Path, required=True)
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    identity = build_identity(
        root=args.root,
        inputs=args.input,
        variant=args.variant,
        release_enabled=bool(args.release),
        savable_enabled=bool(args.savable),
        trace_enabled=bool(args.trace),
        gvm_enabled=bool(args.gvm),
        model_threads=args.model_threads,
        input_trees=args.input_tree,
    )
    write_outputs(identity, args.output_cpp, args.output_json)


if __name__ == "__main__":
    main()

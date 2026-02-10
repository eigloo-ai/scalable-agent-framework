#!/usr/bin/env python3
"""
Run a single plan/task function call using protobuf payloads over stdin/stdout.
"""

from __future__ import annotations

import argparse
import base64
import importlib.util
import sys
import traceback
from pathlib import Path

from agentic_common.pb import PlanInput, PlanResult, TaskInput, TaskResult


def _load_callable(script_path: str, mode: str):
    module_name = f"user_executor_{mode}"
    spec = importlib.util.spec_from_file_location(module_name, script_path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"Unable to load python file: {script_path}")

    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)

    function_name = "plan" if mode == "plan" else "task"
    if not hasattr(module, function_name):
        raise RuntimeError(f"Expected function '{function_name}' in {script_path}")

    return getattr(module, function_name)


def _parse_input(mode: str, encoded_input: str):
    payload = base64.b64decode(encoded_input.encode("ascii"))
    if mode == "plan":
        return PlanInput.FromString(payload)
    return TaskInput.FromString(payload)


def _serialize_output(mode: str, output):
    if mode == "plan":
        if not isinstance(output, PlanResult):
            raise TypeError(f"plan(...) must return PlanResult, got {type(output)!r}")
    else:
        if not isinstance(output, TaskResult):
            raise TypeError(f"task(...) must return TaskResult, got {type(output)!r}")
    return base64.b64encode(output.SerializeToString()).decode("ascii")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--mode", choices=("plan", "task"), required=True)
    parser.add_argument("--script", required=True)
    args = parser.parse_args()

    script_path = Path(args.script)
    if not script_path.exists():
        raise FileNotFoundError(f"Script does not exist: {script_path}")

    encoded_input = sys.stdin.read().strip()
    if not encoded_input:
        raise RuntimeError("No input payload was provided on stdin")

    fn = _load_callable(str(script_path), args.mode)
    input_message = _parse_input(args.mode, encoded_input)
    output_message = fn(input_message)
    sys.stdout.write(_serialize_output(args.mode, output_message))
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception:
        traceback.print_exc(file=sys.stderr)
        raise SystemExit(1)

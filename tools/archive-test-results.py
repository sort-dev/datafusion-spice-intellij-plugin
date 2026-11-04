#!/usr/bin/env python3
"""Preserve each test lane's XML before another Kotlin mode replaces it."""
import argparse
from datetime import datetime, timezone
import json
from pathlib import Path
import shutil
import subprocess
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
COUNTS = ("tests", "failures", "errors", "skipped")


def archive_results(source, destination, tasks):
    # Never merge with an older run: deleted or renamed test classes would leave
    # stale XML and inflate the apparent coverage.
    destination.mkdir(parents=True, exist_ok=False)
    lanes = {}
    for task in tasks:
        if task not in ("test", "test262", "test263", "testDataGrip"):
            raise ValueError(f"Unsupported task: {task}")
        files = sorted((source / task).glob("TEST-*.xml"))
        counts = dict.fromkeys(COUNTS, 0)
        invalid = []
        copied = []
        target = destination / task
        target.mkdir()
        for report in files:
            if report.is_symlink():
                raise ValueError(f"Refusing a linked report: {report}")
            shutil.copyfile(report, target / report.name)
            copied.append(report.name)
            try:
                suite = ET.parse(report).getroot()
                if suite.tag != "testsuite":
                    raise ValueError("Expected a Gradle testsuite report")
                values = {key: int(suite.get(key, "0")) for key in COUNTS}
                if any(value < 0 for value in values.values()):
                    raise ValueError("Negative test count")
                for key, value in values.items():
                    counts[key] += value
            except (ET.ParseError, ValueError) as error:
                invalid.append({"file": report.name, "error": str(error)})
        status = "incomplete" if not files or invalid or not counts["tests"] else (
            "failed" if counts["failures"] or counts["errors"] else (
                "skipped" if counts["skipped"] else "passed"))
        lanes[task] = dict(status=status, counts=counts, reports=copied, invalid=invalid)
    result = dict(created=datetime.now(timezone.utc).isoformat(), lanes=lanes)
    for name, arguments in (("commit", ["rev-parse", "HEAD"]), ("worktreeStatus", ["status", "--porcelain"])):
        command = subprocess.run(["git", *arguments], cwd=ROOT, text=True, capture_output=True)
        result[name] = command.stdout.strip() if command.returncode == 0 else None
    (destination / "summary.json").write_text(json.dumps(result, indent=2) + "\n")
    return result


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, default=ROOT / "build/test-results")
    parser.add_argument("--destination", type=Path, required=True)
    parser.add_argument("--tasks", nargs="+", choices=["test", "test262", "test263", "testDataGrip"], default=["test", "test262", "test263"])
    parser.add_argument("--require-passing", action="store_true", help="Fail for missing, failed, invalid or skipped results")
    args = parser.parse_args()
    result = archive_results(args.source, args.destination, args.tasks)
    for task, lane in result["lanes"].items():
        print(f"{task}: {lane['status']} {lane['counts']}")
    if args.require_passing and any(lane["status"] != "passed" for lane in result["lanes"].values()):
        raise SystemExit(1)

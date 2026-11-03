"""Run with python3 -m unittest discover -s tools -p 'test_*.py'."""
import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("archive_results", ROOT / "tools/archive-test-results.py")
archive = importlib.util.module_from_spec(spec)
spec.loader.exec_module(archive)


class ArchiveResultsTest(unittest.TestCase):
    def setUp(self):
        parent = ROOT / "build/archive-results-tests"
        parent.mkdir(parents=True, exist_ok=True)
        self.directory = tempfile.TemporaryDirectory(dir=parent)
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        self.source = self.root / "results"

    def report(self, task, name="Example", tests=2, failures=0, errors=0, skipped=0):
        directory = self.source / task
        directory.mkdir(parents=True, exist_ok=True)
        (directory / f"TEST-{name}.xml").write_text(
            f'<testsuite tests="{tests}" failures="{failures}" errors="{errors}" skipped="{skipped}"/>')

    def testCountsAndRawXmlArePreservedSeparatelyForEachRuntime(self):
        for task in ("test", "test262", "test263"):
            self.report(task)
        destination = self.root / "absent"
        result = archive.archive_results(self.source, destination, ["test", "test262", "test263"])
        self.assertEqual(3, len(result["lanes"]))
        for task, lane in result["lanes"].items():
            self.assertEqual("passed", lane["status"])
            self.assertEqual(2, lane["counts"]["tests"])
            self.assertEqual((self.source / task / "TEST-Example.xml").read_bytes(),
                             (destination / task / "TEST-Example.xml").read_bytes())
        self.assertEqual(result, json.loads((destination / "summary.json").read_text()))

    def testAnArchiveCannotBeMergedWithAnOlderRun(self):
        self.report("test")
        archive.archive_results(self.source, self.root / "snapshot", ["test"])
        with self.assertRaises(FileExistsError):
            archive.archive_results(self.source, self.root / "snapshot", ["test"])

    def testFailuresErrorsAndSkipsAreNotReportedAsPassing(self):
        for key, status in (("failures", "failed"), ("errors", "failed"), ("skipped", "skipped")):
            self.report("test", **{key: 1})
            result = archive.archive_results(self.source, self.root / key, ["test"])
            self.assertEqual(status, result["lanes"]["test"]["status"])

    def testMissingAndMalformedReportsArePreservedAsIncompleteEvidence(self):
        self.report("test")
        (self.source / "test/TEST-Broken.xml").write_text("<testsuite")
        result = archive.archive_results(self.source, self.root / "partial", ["test", "test262"])
        self.assertEqual("incomplete", result["lanes"]["test"]["status"])
        self.assertEqual(1, len(result["lanes"]["test"]["invalid"]))
        self.assertEqual("incomplete", result["lanes"]["test262"]["status"])
        self.assertTrue((self.root / "partial/test/TEST-Broken.xml").is_file())

    def testCliRequirePassingFailsAfterPreservingFailedReports(self):
        self.report("test", failures=1)
        destination = self.root / "failed-cli"
        result = subprocess.run([sys.executable, str(ROOT / "tools/archive-test-results.py"),
            "--source", str(self.source), "--destination", str(destination), "--tasks", "test", "--require-passing"],
            text=True, capture_output=True)
        self.assertEqual(1, result.returncode, result.stderr)
        self.assertTrue((destination / "test/TEST-Example.xml").is_file())
        self.assertEqual("failed", json.loads((destination / "summary.json").read_text())["lanes"]["test"]["status"])

    def testCliRequirePassingSucceedsForACompleteSelectedLane(self):
        self.report("test262")
        result = subprocess.run([sys.executable, str(ROOT / "tools/archive-test-results.py"),
            "--source", str(self.source), "--destination", str(self.root / "passed-cli"), "--tasks", "test262", "--require-passing"],
            text=True, capture_output=True)
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("test262: passed", result.stdout)

# Test evidence snapshots

Gradle replaces a task's JUnit XML when that task runs again. CI previously ran
the Kotlin-absent and Kotlin-present matrices into the same directories and only
uploaded the last set. The job log showed both commands, but the artifact did not
preserve both sets of XML.

CI now snapshots each mode immediately after its matrix, including partial output
when the test command fails. The artifact includes:

- `build/ci/kotlin-absent/{test,test262,test263}/`
- `build/ci/kotlin-present/{test,test262,test263}/`
- One `summary.json` per mode with raw report names, counts, status, commit and
  worktree status. These names identify test tasks, not a claim about an unrecorded
  interactive IDE session.

The same helper can preserve local results:

```sh
python3 tools/archive-test-results.py --destination build/evidence/my-new-run --require-passing
python3 -m unittest discover -s tools -p 'test_*.py'
```

The destination must not already exist. This prevents removed or renamed test
classes from leaving stale XML in a merged archive. Source files are copied byte
for byte before parsing. A missing runtime, malformed XML, zero tests, skipped
tests or failures never produces a `passed` status.

Without `--require-passing`, archiving failed evidence succeeds and the summary
still records failure or incompleteness. CI uses the flag, so a passing test
command with missing or skipped evidence cannot silently pass the snapshot gate.
The original test step's failure is also retained. Six Python tests cover counts,
raw copies, immutable destinations, partial/malformed reports, failures/skips and
the CLI exit status. No third-party Python packages are required.

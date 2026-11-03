# Earlier delivery evidence

Historical records only. The current install ZIP, checks and remaining gates are
in [testing.md](../testing.md) and [TODO.md](../TODO.md).

## Hosted checkpoints

- [41673732794](https://github.com/sort-dev/datafusion-spice-intellij-plugin/actions/runs/41673732794)
  passed baseline implementation `f656bae740f68e599fd493dfca74fc9f0dec2a49`.
  Both Kotlin modes on three runtimes, distribution checks and artifacts passed.
- [41688372935](https://github.com/sort-dev/datafusion-spice-intellij-plugin/actions/runs/41688372935)
  passed lifecycle implementation `5f7b4ea2e509b23b722f374e1797bcd7197b1f25`.
  Both Kotlin modes on 261/262/263, packaging/distribution checks and artifacts passed.
- [41692113541](https://github.com/sort-dev/datafusion-spice-intellij-plugin/actions/runs/41692113541)
  passed first-connection identity implementation `7ece225e76cc424c273668e84221f4c21065deb1`.
- [41693793919](https://github.com/sort-dev/datafusion-spice-intellij-plugin/actions/runs/41693793919)
  passed console/evidence checkpoint `3410a54b15e7d67669d823c136f45e0ca3c9cf2b`.
  The downloaded artifact contains separate Kotlin-absent/present raw XML and
  summaries, each with 67 tests on all three runtimes. Six Python evidence-tool
  tests also passed. Download: `build/nap/console-ci-artifacts/`.
- [41696634679](https://github.com/sort-dev/datafusion-spice-intellij-plugin/actions/runs/41696634679)
  passed editor/cancellation checkpoint `599c27a`, with 72 tests per runtime in
  both Kotlin modes and distribution/artifact checks.

The hosted verifier was manual-only and skipped on these pushes. Local verifier
audits are separate evidence, not hosted verdicts. There is no publication job.

## Baseline clean ZIP

The baseline ZIP from `f656bae` was
`datafusion-spice-intellij-plugin-0.1.0-dev.202611031623+f656bae740f6.zip`, SHA-256
`f8853a50fecec325d3670356be30378f1d001dcae94f4c70ee79ecfcd9379387`.
All 124 normalized runtime-payload entries matched that baseline's audited dirty
build. The comparison record is `build/readiness/clean-payload-equivalence.json`.
That clean build passed 50 tests in an isolated runtime of installed IDE 262.9437.185.

## Lifecycle clean ZIP

The lifecycle ZIP from `5f7b4ea` was
`datafusion-spice-intellij-plugin-0.1.0-dev.202611031752+5f7b4ea2e509.zip`, SHA-256
`1fa8cddff46c09499f31c103b0b21658d812d7741fc25120c369acaeff8005ee`.
Its 125 normalized entries matched the lifecycle audit, before the subsequent
first-connection identity fix. Evidence: `build/lifecycle/clean-payload-equivalence.json`.
It passed 63 tests on installed IDE 262.9437.185 and distribution verification,
recorded in `build/lifecycle/clean-installed-runtime.log`. Other 63-test installed
runtime evidence is in `build/lifecycle/installed-runtime.log` and `installed-runtime/`.

These old ZIPs are not the current dogfood recommendation. Payload equivalence
does not relabel an old verifier run or grant API/publication approval.

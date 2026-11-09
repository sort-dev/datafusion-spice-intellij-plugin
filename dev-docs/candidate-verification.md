# Local Brikk race-fix candidate verification

This records the historical owner-authorized Maven Local SNAPSHOT qualification,
not a stable or public release. The ordinary default has since moved to public
0.4.1 and then Brikk 1.0.1. See [stable-041-verification.md](stable-041-verification.md)
and [release-readiness.md](release-readiness.md). Statements below
about the unchanged 0.4.0 default describe the snapshot acceptance checkpoint.

## Exact input

Candidate version:
`0.4.1-livecatalog-races-20261104-g2a46bd08efcd-SNAPSHOT`.

Group: `dev.brikk.house`. The aligned artifacts are
`brikk-sql-intellij-support`, `brikk-sql-intellij-jdbc-support` and
`brikk-sql-intellij-test-support`. SQL core/metadata/verification API stay on
released 0.23.0 and were not republished locally.

Source commit: `2a46bd08efcde21e080e8f0a88d68fc997383fc8`, in the clean local
Brikk main checkout, not pushed upstream. Source fingerprint:
`90f0f4fe51f1a040e73ffbe290b298a55bdcf384cac631e817f32663813ba84e`.

Verified JAR SHA-256s:

| Artifact | SHA-256 |
| --- | --- |
| Common support | `8400fa9e25462716c085bb7c00801600b8c5ce2cf162148b3792b90ad1a61323` |
| JDBC support | `19bacdaa249fa9408fb7aef5f09df7f80800ab2c1beeddc7565ebc27d259dd05` |
| Test support | `127f5478841f986c3f518ffe63e1ab5a4a73ad909cba741fcfa4e97e8d0945ac` |

The supplied JAR, POM, Gradle-module and source-JAR hashes were all checked.
POM/module closure and resolved test/sandbox classpaths were checked as well:
no old common/JDBC/test-support implementation was selected and SQL stays at
released 0.23.0. Full publication manifest, upstream gate and resolved paths are
preserved under `build/candidate-qualification/`.

## Consumer verdict

- IDEA 261/262/263: 84 ordinary tests each, with Kotlin absent and present,
  504 executions, zero failures/errors/skips.
- Actual DataGrip 261.24374.56 and 262.10315.132: 84 ordinary tests each,
  168 executions, using only each selected DataGrip SDK on its runtime classpath.
- 328 passing live executions. These include 20 desired-contract race executions
  across both products and all five IDEA/DataGrip runtime lanes. Both contracts
  previously failed on released 0.4.0.
- Other live checks include ordinary/PIPE console execution, manual and
  out-of-band refresh, helper startup-script isolation, encrypted Password Safe
  persistence across fresh writer/reader JVMs, TLS/hostname verification and
  bounded active-stream cancellation with same-connection recovery.
- Twelve private-classloader/current-peer isolation checks pass across the three
  IDEA runtimes. All 84 tests also pass on the isolated installed IDEA runtime.
- Nine Python evidence/proxy tests pass.

The old stale-helper results are now rejected without recreating forgotten
metadata or replacing a newer accepted same-identity result. Superseded feedback
has been made generic so it does not incorrectly blame every rejected result on
edited connection settings.

Evidence: `ordinary-*`, `final-ordinary-*`, `datagrip-*`, `live/`,
`live-summary.json`, `peer-isolation/`, `installed-runtime/`,
`live-and-final-matrix.log` and `upstream-manifest.json` under
`build/candidate-qualification/`.

## Packaging and remaining boundaries

Final clean plugin build from consumer implementation `52908a14aabc`:

`build/dogfood/datafusion-spice-intellij-plugin-0.1.0-qualification-dev.202611041624+52908a14aabc.zip`

SHA-256: `8239c5cdf1da9312a0d0dbe2fca104ff66d87862532438bd3e211e59951fa6ec`.

The exact clean ZIP passed `verifyDistribution` and all five full Plugin Verifier
targets: DataGrip 261.24374.56 / 262.10315.132 and IDEA 262.10968.63 / 263.4732.28 /
263.5701.42. Each remains Compatible with 1 internal, 11 experimental and 1
deprecated usage. No exemptions or publication approval were added.

Independent ZIP inspection requires the two embedded candidate runtime JARs to
match the supplied publication hashes. Test support and Arrow JDBC are absent;
the descriptor/version explicitly says qualification. All upstream input hashes
were rechecked unchanged after qualification. Final log and machine-readable ZIP
record are `build/candidate-qualification/final-build.log` and
`final-build-record.json`. Final consumer acceptance was sent to the existing
Brikk agent; it does not authorize a final/public library release or remote push.

The exact candidate runtime coordinates are included in the qualification-only
third-party notice section. The legal/dependency gate is not waived. Candidate
builds use `0.1.0-qualification-dev.*`; release mode rejects snapshot overrides.
No test support, Arrow driver or fixture implementation belongs in the plugin ZIP.

The ordinary 0.4.0 default still has the reproduced races. Stable publication
requires a separately authorized formal dependency release and review. Actual
live-window/driver-dialog/OS-keychain interaction, unsupported full-path namespace
switching, database-DDL rename and arbitrary noncooperative RPC/cleanup behavior
are not established by these passing checks. No public release, tag or
Marketplace publication is authorized by this qualification.

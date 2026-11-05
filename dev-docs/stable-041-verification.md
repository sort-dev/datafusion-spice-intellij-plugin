# Released Brikk 0.4.1 consumer verification

The ordinary dependency default is now public Maven Central IntelliJ
common/JDBC/test support 0.4.1. SQL core, metadata and verification API remain
released 0.23.0. The guarded snapshot override remains opt-in and cannot be used
with release mode. No plugin release, tag or Marketplace publication is authorized.

## Public provenance

Upstream source: `2a46bd08efcde21e080e8f0a88d68fc997383fc8`.
Authorized library release workflow:
https://github.com/brikk/brikk-sql/actions/runs/42065832531.

The consumer independently downloaded and hash-checked all 12 public JAR, POM,
module and sources files for the three aligned artifacts. Binary and sources
JARs match the accepted snapshot byte-for-byte. POM/module dependencies select
released support 0.4.1 and SQL 0.23.0. Evidence is under `build/stable-041/public/`,
`public-manifest.json` and `upstream-public-audit.json`. The upstream audit also
checked 12 PGP signatures; this consumer audit checked hashes and closure, not
those signatures independently.

Runtime JAR SHA-256s:

- Common: `8400fa9e25462716c085bb7c00801600b8c5ce2cf162148b3792b90ad1a61323`
- JDBC: `19bacdaa249fa9408fb7aef5f09df7f80800ab2c1beeddc7565ebc27d259dd05`
- Test support: `127f5478841f986c3f518ffe63e1ab5a4a73ad909cba741fcfa4e97e8d0945ac`

## Consumer gates

Stable requalification passed without `BRIKK_SUPPORT_CANDIDATE` or
`qualification.brikkSupportVersion`:

- IDEA 261/262/263, 84 tests per runtime with Kotlin absent and present,
  504 ordinary executions.
- Actual SQL-only DataGrip 261/262, 84 tests each, 168 executions with pure
  DataGrip runtime classpaths.
- 328 live checks, including all 20 desired-contract race executions across
  both products and all five IDEA/DataGrip lanes. These also cover console/PIPE,
  refresh, credential restart, TLS/auth/hostname checks and active-stream cancel.
- Twelve peer-isolation checks, 84 installed-IDE tests and nine Python tests.
- All five full verifier targets Compatible, with unchanged 1 internal,
  11 experimental and 1 deprecated usage per target. No exemptions were added.
- Build, descriptor, legal and dependency distribution checks passed.

All executed tests above have zero failures, errors or skips. Immutable XML and
checked summaries are under `build/stable-041/ordinary-*`, `datagrip-*`, `live/`,
`peer-isolation/` and `installed-runtime/`. The serialized driver and full log are
`qualify.py` and `qualification.log`. Resolved Gradle/sandbox artifacts match the
public hashes and select only 0.4.1 support plus released SQL 0.23.0, without
Maven Local. `*-resolved.json` records each inspected runtime classpath.

## Fresh clean dogfood ZIP

Built from pushed adoption commit `b4734e4bc399ae3d2305886f8e00fa72c58a3022`,
with a clean checkout and no candidate override:

`build/dogfood/datafusion-spice-intellij-plugin-0.1.0-dev.202611051726+b4734e4bc399.zip`

SHA-256: `a4a96d9069aa226be664bd8fcffe68e570947e763509e5fad765c5971c570613`.

This fresh normal ZIP passed all five full verifier targets and distribution
checks. Independent inspection confirms its descriptor/version, exact six-JAR
payload, released SQL 0.23.0, public support/JDBC 0.4.1 hashes and packaged legal
notices. Test support, Arrow JDBC, old support and snapshot JARs are absent. The
historical qualification ZIP was not relabeled. Final evidence is
`build/stable-041/final-build.log` and `final-build-record.json`.

Stable consumer adoption was acknowledged to the existing Brikk agent. Hosted
consumer CI is tracked separately at
https://github.com/sort-dev/datafusion-spice-intellij-plugin/actions/runs/42074662198.
The run passed on adoption commit `b4734e4bc399`: both Kotlin modes on all three
IDEA runtimes, evidence-tool checks, build/distribution gates and artifact upload.
Downloaded `test-results` artifacts confirm 84 passing tests per runtime per mode,
504 hosted executions with zero failures/errors/skips. Evidence is under
`build/stable-041/hosted-test-results/ci/`. The manual-only hosted Plugin Verifier
job was skipped; all five verifier verdicts above are from the separate local run.

Live-window cold references/schema selection, interactive installs/driver
upgrades, OS-keychain UI, unload/disposal and noncooperative RPC/cleanup remain
separate qualification gaps. API review and publication approval remain open.

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

A fresh clean normal `0.1.0-dev.*` ZIP is built after committing this tested
adoption. The historical qualification ZIP is not relabeled.

Live-window cold references/schema selection, interactive installs/driver
upgrades, OS-keychain UI, unload/disposal and noncooperative RPC/cleanup remain
separate qualification gaps. API review and publication approval remain open.

# Current verification

This report separates automated checks, real disposable endpoints and live UI
confirmation. The plugin is still a development build, not a published or fully
qualified release. Earlier exploratory results are in
[archive/local-baseline-testing.md](archive/local-baseline-testing.md).

## Standard runtime checks

Build JDK 21.0.2, Gradle 9.0.0, Kotlin 2.4.10, SQL 0.23.0 and IntelliJ
support/JDBC/test support 0.4.0. Tests use each IDE's bundled runtime.

```sh
./gradlew test test262 test263 buildPlugin verifyDistribution
./gradlew test test262 test263 -Ptest.kotlin=present
```

The standard suite passes 50 tests per runtime with zero failures/errors/skips,
300 executions across the two Kotlin modes. The final local pass is recorded in
`build/readiness/runtime-matrix.log` and `build/readiness/kotlin-present.log`;
preserved XML is under `build/readiness/matrix-absent/` and `matrix-present/`.
The configured builds are 261.25134.95, 262.10968.63 and 263.5701.42.

Coverage now includes:

- Actual descriptor/service loading, exact product ownership, explicit mappings,
  local parser errors, function kinds, real completion insertion, formatting and
  incremental edits.
- Default catalog/schema metadata readback, new-scope initialization without
  overwriting explicit selections, cold qualified/unqualified reference loading,
  typed-path introspection and no-edit model updates. The database boundary is
  held/recorded, not a live connection.
- Physical model facts/navigation, local alias rename without string replacement,
  named arguments retaining one arrow and their existing value, product-specific
  documentation links, explicit dollar-string SQL injection and Unicode offsets.
- Blank connection VM options plus inherited driver flags in the real
  `ConsoleRunConfiguration` assembly, not merely XML text.
- Both products' native inventories serialized and reloaded through a new
  `LiveCatalogStore` under the IDE serialization runtime. Session-only facts must
  not reappear from disk. This is not fresh-process/cross-version cache migration.
- All four intention description/example directories through IntelliJ metadata
  lookup, including both actions of each family hidden by settings deduplication.
- Shared PIPE console contracts, native-mode defaults, refusal/staleness checks,
  stock prompting/destination handling and unavailable background verification.

The suite captures background logged errors. Live server and isolated-loader
classes are excluded from this normal matrix and run separately below.

## Real disposable Flight SQL fixtures

```sh
python3 tools/run-flight-smoke.py --java-home /path/to/jdk21
```

The runner starts its own Spice container on ephemeral loopback ports and a
separate bounded DataFusion process. It stops only those fixtures in `finally`.
It does not use saved IDE data sources or the user's `datafusion-spice-local`
container. No models, paid calls, external connectors or leak-stress probes run.

Six opt-in tests per product check:

1. JDBC product identity separately from `SELECT version()`, and actual rows.
2. The released `SpiceCatalogHarvester` / `DatafusionCatalogHarvester` over JDBC,
   including the production namespace readback query.
3. Typed parameter binding/rebinding without literal substitution; nullable
   binding only on the qualified DataFusion fixture.
4. All ten qualified Brikk stages executed as lowered SQL, with row/value checks.
5. Spice JSON functions, or DataFusion session-only view isolation plus a healthy
   second connection after the expected missing-view error.
6. Fresh connections after close.

Observed inventories: Spice product `Spice.ai`, version 2.4.0, namespace
`spice.public`, 447 functions; DataFusion fixture version 55.2.0, namespace
`datafusion.public`, 320 functions. Parser settings remain distinct: PostgreSQL /
normalization off for Spice, Generic / normalization on for DataFusion.

Logs, explicit pass/fail status and XML copies are under `build/readiness/`:
`flight-smoke-runner.log`, `<product>-live-tests.log`,
`<product>-live-status.json`, `<product>-test-results/`, and `live/` inventory /
PIPE-result records. The DataFusion Cargo manifest, lock and execution source
match upstream release commit `b406a1bd894963f3a09f63f02f6b04e606f083f2`.
Its fixture binary was built with `cargo build --locked --offline`, outside the
upstream checkout's output directories.

These are plaintext loopback fixtures. They do not establish TLS, a production
authorization policy, arbitrary cancellation, or reliable Spice prepared updates /
NULL bindings. A returned update count is not treated as an execution success.

## Private classloaders and sibling coexistence

Four isolated tests pass per runtime on 261/262/263, 12 executions. The shared
`-Ptest.pluginIsolation=true` lane removes flattened Brikk/main runtime classes
and loads actual plugin distributions. It checks private core/support class
copies, ownership of both languages, installed cooperative Execute chains and
single delegation / detachment of this plugin's customizer. Background errors fail
the run. Kotlin is absent in this lane; it is tested separately above.

Peer inputs are immutable archived ZIPs from `plugin-builds/20261017-main-dogfood`:
ClickHouse 0.1.2, Doris 1.6.2, DuckDB 0.4.2, Trino 0.4.2. SQL Transpiler was explicitly
absent. This is not proof against every current/newer peer ZIP or every possible
wrapper registration permutation. Reports: `build/readiness/isolation.log` and
`build/readiness/isolation-results/`.

## Fresh full Plugin Verifier audit

`./gradlew verifyPlugin` passed all five configured targets with **Compatible**:

- DataGrip 261.24374.56 and 262.10315.132.
- IntelliJ 262.10968.63, 263.4732.28 and 263.5701.42.

Each target reports 1 internal usage, 11 experimental usages and 1 deprecated
override. The internal factory reference is in shared GenericJdbcIntrospectorGate;
the deprecated override is in shared PgFamilyCatalogModelHelper. Experimental
connection-hook APIs include three references in this plugin's ProductHarvest.
Capturing Project before the EDT handoff removed four unnecessary ProtoConnection
references; no allowance or suppression was added.

The audited development label is
`0.1.0-dev.202611031545+459380e400df.dirty`. Exact input versions and complete
reports are in `build/readiness/plugin-verifier-final.log` and
`build/reports/pluginVerifier/`. A subsequent commit-derived packaging label is
not by itself a new verifier audit. `api-usage-approvals.txt` remains empty of
exemptions: **Compatible is not publication approval**.

## Delivery and remaining UI checks

The shared `verifyDistribution` checks exact embedded JARs, bytecode, duplicate
dependencies, legal resources and descriptor/version agreement. The plugin bundles
neither Arrow JDBC, test support, Kotlin stdlib nor a serialization runtime copy.
Run `./gradlew dogfoodZip` for the current Git-derived ZIP path and checksum; do
not pick an older ZIP merely because it is first in a directory listing.

CI follows the peers' checkout/JDK/Gradle/runtime/build/artifact flow.
[Hosted run 41673732794](https://github.com/sort-dev/datafusion-spice-intellij-plugin/actions/runs/41673732794)
passed on implementation commit `f656bae740f68e599fd493dfca74fc9f0dec2a49`:
the three-runtime matrix with Kotlin absent and present, build/distribution
verification and ZIP/test-artifact uploads all succeeded. The hosted verifier job
is manual-only and was skipped on this push; the five verdicts above are the
separate local audit, not a hosted verifier result. There is no publication job.

The clean local dogfood built from that commit is
`datafusion-spice-intellij-plugin-0.1.0-dev.202611031623+f656bae740f6.zip`, SHA-256
`f8853a50fecec325d3670356be30378f1d001dcae94f4c70ee79ecfcd9379387`.
All 124 runtime-payload entries match the audited dirty build after normalizing
only the exact build-version fields in the descriptor/manifest. The comparison
record is `build/readiness/clean-payload-equivalence.json`; this is payload
equivalence, not a newly relabeled Plugin Verifier run. The final clean build also
passed all 50 tests in an isolated runtime using installed IDE build 262.9437.185.

The user's live editor symptoms, actual driver dialog/process, clean interactive
installs, TLS/auth policy, fresh-process cache migration and production endpoint
scope remain open in [TODO.md](TODO.md). The installed IDE 262.9437.185 was tested
earlier in an isolated runtime lane, not controlled as a live UI. Reconnection and
schema/model behavior still need actual dogfooding. Namespace-changing SQL remains
disabled; readback and default-scope selection are separate from a USE guarantee.

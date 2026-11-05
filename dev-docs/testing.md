# Current verification

This report separates automated checks, real disposable endpoints and live UI
confirmation. The plugin is still a development build, not a published or fully
qualified release. Earlier exploratory results are in
[archive/local-baseline-testing.md](archive/local-baseline-testing.md).

The engineering follow-up is recorded in
[engineering-qualification.md](engineering-qualification.md). The ordinary default
now uses public IntelliJ support 0.4.1, whose binaries and sources match the
accepted race-fix snapshot. Stable consumer requalification is recorded in
[stable-041-verification.md](stable-041-verification.md). The earlier snapshot
evidence remains in [candidate-verification.md](candidate-verification.md).

## Standard runtime checks

The current clean dogfood ZIP uses released support 0.4.1:
`build/dogfood/datafusion-spice-intellij-plugin-0.1.0-dev.202611051726+b4734e4bc399.zip`.
SHA-256: `a4a96d9069aa226be664bd8fcffe68e570947e763509e5fad765c5971c570613`.
It passed the full stable-default matrix and all five local verifier targets.
See stable-041-verification.md for exact evidence and remaining approval gaps.

Build JDK 21.0.2, Gradle 9.0.0, Kotlin 2.4.10, SQL 0.23.0 and IntelliJ
support/JDBC/test support 0.4.1. Tests use each IDE's bundled runtime.

```sh
./gradlew test test262 test263 buildPlugin verifyDistribution
./gradlew test test262 test263 -Ptest.kotlin=present
```

The expanded standard suite passes 84 tests per runtime with zero
failures/errors/skips, 504 executions across the two Kotlin modes. The latest
serialized local pass is `build/engineering/serial-resume.log`; XML and checked
summaries are under `build/engineering/stable84-absent/` and `stable84-present/`.
Actual DataGrip 261/262 each pass the same 84-test suite in a dedicated SQL-only
lane, with only the chosen DataGrip SDK on the runtime classpath. Their evidence
is under `build/engineering/stable84-datagrip-261/` and `stable84-datagrip-262/`.
The global physical-reference search was also rerun on all three runtimes in
`build/nap/physical-indexed-matrix.log` and `physical-indexed-xml/`.
The earlier 63-test lifecycle evidence remains under `build/lifecycle/`.
The configured builds are 261.25134.95, 262.10968.63 and 263.5701.42.

Coverage now includes:

- Project-backed physical-column ReferencesSearch across same-named tables in two
  schemas, and reference-only SQL rename that does not mutate the database model.
- Both explicit tagged-dollar injected dialects, Unicode host ranges, local parse
  errors and registered function completion insertion that preserves the host.
- Caller cancellation through both actual consumer hooks and the released
  harvester at a held synthetic remote statement. Cancel/close, no namespace
  readback and no scope/cache seeding are required. This is not a real-server/RPC
  cancellation bound. See [editor-and-cancellation-qualification.md](editor-and-cancellation-qualification.md).

- Actual consumer-hook/shared-harvester lifecycle checks for both products. URL
  edits during harvesting or namespace readback and a foreign-driver switch must
  not seed stale scope. Explicit scope, foreign/helper isolation and failed-new-
  identity fallback are checked. The stale-scope regression failed before the fix.
- Explicit Refresh notifications through the existing consumer hook, including
  escaped source names, credential-detail suppression, non-success outcomes and
  silence for automatic events. See [catalog-lifecycle-followup.md](catalog-lifecycle-followup.md).
- Separate cache writer/reader JVMs on each IDE runtime. Current-format native
  inventories survive; memory-only session functions, namespace and version do
  not. This is not cross-library-version format migration.

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
  not reappear from disk. Separate-process coverage is described above;
  cross-library-version migration remains unqualified.
- All four intention description/example directories through IntelliJ metadata
  lookup, including both actions of each family hidden by settings deduplication.
- Shared PIPE console contracts, native-mode defaults, refusal/staleness checks,
  stock prompting/destination handling and unavailable background verification.

The suite captures background logged errors. Live server and isolated-loader
classes are excluded from this normal matrix and run separately below.

Saved-credential persistence/isolation, fail-closed cache formats, namespace
advertisement and quoted physical-reference/host cases extend the earlier suite.
The earlier DataGrip probe's Java-host dependency failure is superseded by the
public-DSL SQL-only lane; no vendored code or SDK plugins were patched/transplanted.

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

The default lane uses plaintext loopback fixtures. It does not establish a
production authorization policy, arbitrary cancellation or reliable Spice prepared
updates / NULL bindings. A returned update count is not treated as execution success.

### Disposable TLS and API-key checks

```sh
python3 tools/run-flight-smoke.py --product spice --spice-secure --java-home /path/to/jdk21
```

Spice 2.4.0 / Arrow JDBC 19.0.0 checks cover header and normal password-handshake
API-key authentication, missing/invalid keys, untrusted roots and mismatched
hostnames. Certificate verification stays enabled. Negative tests check the
distinct observed reasons, not merely any exception. The expanded transport
matrix below repeats these checks on all three IDEA runtimes and actual DataGrip.

The runner generates one-day test certificates and a random API key in a private
temporary fixture directory. Spice reads a read-only mount; instance state lives
in container tmpfs. Only ephemeral loopback ports are published. Runner-owned
containers are removed and temporary keys/certificates are deleted in cleanup.
The key goes to the test JVM through its environment, not a JDBC URL or command
argument. Saved fixture logs redact it. No IDE credential store is changed.

Direct header tests use Arrow's `tlsRootCerts` and non-reserved RPC-header
properties. Real SDK sources instead use the ordinary password handshake and
Database Tools' credential API, including a private persisted Password Safe
backend and fresh reader JVMs. DataFusion now has its own TLS-termination fixture.
See engineering-qualification.md for the exact storage, transport and pending-RPC
cancellation boundaries; none of this requests production credentials.

Evidence: `build/lifecycle/secure-runner.log`,
`build/readiness/spice-secure-live-tests.log`, `spice-secure-live-status.json`,
`spice-secure-test-results/` and `spice-secure-fixture.log`.

### IDE remote JDBC process

`--ide-process` adds six checks through an actual IDE-launched Arrow JDBC
process: first-connect native inventory/default scope and inherited VM flags,
helper refresh that must skip an invalid user startup script, and healthy fresh
connections after close. The fourth opens a real `JdbcConsole`, uses the actual
registered Execute action for ordinary SQL and opted-in PIPE, confirms PIPE is
lowered before server submission without replacing the user's document, invokes
the registered manual Refresh action, and executes a healthy next request.
Connect/SELECT remain quiet and manual Refresh gives one success notification.
The fifth changes inventory outside observed editor execution, proves the new
view is queryable while the metadata cache stays old, then requires manual Refresh
to update native inventory/editor-facts revision without editing the document.
Spice uses a runner-owned pod reload; DataFusion uses the same session's raw
connection. See [out-of-band-refresh.md](out-of-band-refresh.md).
The sixth reloads encrypted saved credentials and uses them for a driver
connection, a helper refresh and an actual console. Three additional namespace
contract checks distinguish real SET behavior from rejected USE and no-op JDBC
setSchema. `--saved-credentials-restart` runs the persistence case in separate
writer/reader JVMs rather than merely reopening a backend in one process.
`--test-task test262` or `test263` selects another
configured runtime. See [remote-driver-qualification.md](remote-driver-qualification.md).

Both plaintext products pass 15 checks on 261/262/263. Secure Spice passes 22,
and TLS-terminated DataFusion passes 18. Reports are in
`build/readiness/<product>-remote-<task>-test-results/` and the matching
`-live-tests.log`; the final runner matrix is
`build/engineering/serial-resume.log`. The first attempted run reproduced inventory
loss before the consumer identity fix. This is still not interactive installation
or production credential-store qualification.
The four transport/product variants total 210 passing IDEA executions. Separate
credential writer/reader JVMs add 12; active-stream cancellation/recovery adds
six, with the observed cancel calls returning in 1 to 3 ms. Actual secure console
runs on DataGrip 261/262 add 80. There are 308 passing live executions in total,
separate from the four failing desired-contract Brikk race reproductions.
Raw directories and counts are recorded in
`build/engineering/passing-live-summary.json`. Runtime-specific cancellation
records are `build/readiness/live/*-stream-cancellation-<build>.txt`.
DataGrip live XML and exact product-info are preserved under
`build/engineering/stable-datagrip-live-<generation>-<product>/`.

## Private classloaders and sibling coexistence

Four isolated tests pass per runtime on 261/262/263, 12 executions. The shared
`-Ptest.pluginIsolation=true` lane removes flattened Brikk/main runtime classes
and loads actual plugin distributions. It checks private core/support class
copies, ownership of both languages, installed cooperative Execute chains and
single delegation / detachment of this plugin's customizer. Background errors fail
the run. Kotlin is absent in this lane; it is tested separately above.

The latest inputs are read-only snapshots of the user's installed peer payloads:
ClickHouse `0.2.2-dev.202611031440+5ebe3a85e48d`, Doris `1.6.4`, DuckDB
`0.5.1-dev.202611030056+c097fc5d8050.dirty` and Trino
`0.4.4-dev.202610302129+2486f8f6b75e.dirty`. SQL Transpiler was explicitly absent.
These are installed-payload snapshots, not rebuilt or newly released peer ZIPs.
Per-file hashes and ZIP hashes are in
`build/lifecycle/installed-peers/manifest.json`; capture checked that installed
files stayed unchanged. All 12 executions pass with zero failures/errors/skips.
Reports: `build/lifecycle/installed-isolation.log` and `installed-isolation/`.
The older archived ZIP pass remains under `build/readiness/isolation-results/`.
Neither pass establishes every possible wrapper registration permutation.
The final clean `17ad87d` checkpoint repeated all 12 installed-peer executions
successfully. The installed files and snapshot ZIP hashes were rechecked unchanged
in `build/nap/installed-peers-recheck.json`. Final XML and checked counts are under
`build/nap/final-installed-isolation/`.

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

The latest audited development label is
`0.1.0-dev.202611031917+7a970b8d628f.dirty`, including the read-only namespace
strategy. Exact input versions and reports are in `build/engineering/plugin-verifier.log`,
`build/engineering/verifier-verdicts.json` and
`build/reports/pluginVerifier/`. A subsequent commit-derived packaging label is
not by itself a new verifier audit. `api-usage-approvals.txt` remains empty of
exemptions: **Compatible is not publication approval**.

## Delivery and remaining UI checks

The shared `verifyDistribution` checks exact embedded JARs, bytecode, duplicate
dependencies, legal resources and descriptor/version agreement. The plugin bundles
neither Arrow JDBC, test support, Kotlin stdlib nor a serialization runtime copy.
Run `./gradlew dogfoodZip` for the current Git-derived ZIP path and checksum; do
not pick an older ZIP merely because it is first in a directory listing.

CI follows the peers' checkout/JDK/Gradle/runtime/build/artifact flow and preserves
each Kotlin mode's raw XML and checked summaries separately. Six Python tests
cover the snapshot helper. See [test-evidence.md](test-evidence.md).
[Hosted run 41699614216](https://github.com/sort-dev/datafusion-spice-intellij-plugin/actions/runs/41699614216)
passed on final implementation `17ad87d`. The downloaded artifact independently
confirms 72 passing tests on each runtime in both Kotlin modes, zero
failures/errors/skips, and the exact clean implementation commit in each summary.
The hosted verifier was skipped/manual-only. Status and downloaded XML are in
`build/nap/final-hosted-status.json` and `final-ci-artifacts/`.
Earlier successful checkpoint runs and old ZIP hashes are retained in
[earlier delivery evidence](archive/earlier-delivery-evidence.md), not offered as
the current install recommendation. The hosted verifier remains manual-only;
there is no publication job.

The current clean dogfood ZIP from implementation `17ad87dd194e0dca53c120aeba0cbd14c802249f` is
`datafusion-spice-intellij-plugin-0.1.0-dev.202611031903+17ad87dd194e.zip`, SHA-256
`b176b30b217ce4c672a03b72735c14684f7bbda9b87e793eb8d91f83128e83e8`.
All 127 normalized runtime-payload entries match the identity-fix verifier input.
Only the main JAR filename and exact descriptor/manifest build-version fields were
normalized. Evidence: `build/nap/clean-payload-equivalence.json`. This proves payload
equivalence, not a newly relabeled verifier run or API approval.

That clean commit passed the 432-execution matrix, 12 peer-isolation executions,
distribution verification and all 72 tests in an isolated Kotlin-absent runtime of
installed IntelliJ 2026.2.1 / 262.9437.185. The summaries record the exact clean
commit and an empty worktree status. Installed-IDE metadata and XML are in
`build/nap/final-installed-ide.json` and `final-installed-runtime/`.

The 17ad87d ZIP above predates this engineering follow-up. The verified candidate
ZIP is now `datafusion-spice-intellij-plugin-0.1.0-qualification-dev.202611041624+52908a14aabc.zip`,
SHA-256 `8239c5cdf1da9312a0d0dbe2fca104ff66d87862532438bd3e211e59951fa6ec`.
It embeds the exact authorized local snapshot, passes all five verifier targets
and distribution checks, and is not a stable/public release. See
candidate-verification.md for provenance, all passing race/matrix results and
the historical snapshot checkpoint. The ordinary default now consumes public
0.4.1; stable-041-verification.md records its separate rebuild and qualification.

The user's live-window symptoms, driver-dialog upgrades, clean interactive installs,
production TLS/auth policy, cross-library cache migration and full DataFusion
tree contract remain open in [TODO.md](TODO.md). The installed IDE was tested in
an isolated runtime, not controlled as a live UI. Broader reconnect/removal/disposal
ordering and schema/model UI behavior still need qualification. Namespace-changing
SQL remains disabled; readback and scope selection are separate from a USE guarantee.

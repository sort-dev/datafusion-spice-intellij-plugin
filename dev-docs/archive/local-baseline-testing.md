# Historical local baseline verification

This records the early local builds before the safety checkpoint/push. Paths,
versions and statements about uncommitted work describe that earlier state.
For current readiness results, see [../testing.md](../testing.md).

## Baseline on 2026-11-03

Build process: Java 21.0.2, Gradle 9.0.0, Kotlin 2.4.10, SQL 0.23.0 and IntelliJ
support/JDBC/test support 0.4.0. IDE tests use each IDE's bundled runtime.

```sh
JAVA_HOME=/home/jayson/.local/share/mise/installs/java/21.0.2 \
  ./gradlew test test262 test263 buildPlugin verifyDistribution dogfoodZip \
  --no-daemon --max-workers=2 --console=plain
```

The 35-test suite passed on each configured IDE with the Kotlin plugin absent:

| Lane | IDE build | Tests | Failures/errors/skips |
| --- | --- | --- | --- |
| test | 261.25134.95 | 35 | 0/0/0 |
| test262 | 262.10968.63 | 35 | 0/0/0 |
| test263 | 263.5701.42 | 35 | 0/0/0 |

Log: `build/final-local.log`. XML/HTML results live under `build/test-results/`
and `build/reports/tests/`; later runs of the same lanes replace those reports.

The same 35 tests also passed on each of the three builds with the Kotlin plugin
present, with zero failures/errors/skips. Command:

```sh
JAVA_HOME=/home/jayson/.local/share/mise/installs/java/21.0.2 \
  ./gradlew test test262 test263 -Ptest.kotlin=present \
  --no-daemon --max-workers=2 --console=plain
```

Log: `build/kotlin-present.log`. Total across the absent/present runtime matrix is
210 test executions. This establishes coexistence with the Kotlin IDE plugin,
not automatic Brikk Kotlin-host recognition or injection support.

Coverage includes real production descriptor adapters and annotated service
instantiation, exact language/DBMS ownership, explicit file mapping, generic JDBC
introspector routing, disabled namespace/graphical Explain operations, independent
catalog observer wiring, parser/highlighter registration, local parse errors,
product-specific function completion and fallback labels, dollar-string formatting,
incremental edits, two driver templates and loaded VM flags, workspace PIPE toggles,
local-only native validation, Spice warning ranges/ownership, and the actual
downloaded Arrow JDBC JAR's URL handling and SHA-256.

Eleven console contracts each run for both products using their own registered
drivers with the shared recording console: claims/delegation, stock selection,
whole-script refusal, stale documents, destination/namespace changes during
prompting, cancellation, run-to-stage, nested lowering and refusal notifications.
The recording connection does not open a Flight socket or certify transport.
Platform tests capture background logged errors and fail if any escape.

## Local ZIP

`build/dogfood/datafusion-spice-intellij-plugin-0.1.0-dev.202611031127+73541838d830.dirty.zip`

Current SHA-256: `79c05e416b3a630616989987b4fe626a1e67d355e98536dab9f32123c2131835`.
The original baseline hash was
`73e9b2dbd9c15d170331daaa83479b9d752fd1683d0437bdbc92903838d27fa9`;
the VM-default fix and transport UI removal below rebuilt the same dirty-version
filename. The intermediate VM-default-only ZIP had SHA-256
`71860ad783a40f1f78e4f1f22fc14c01d3c743e4c7ee33c4a874bcb0a7a498f8`.
The transport-UI-removal ZIP had SHA-256
`e0f33b3881589d829cc40d80683c241b748d47962a0ce486f181b20727b1bb89`;
the current ZIP also includes the catalog/editor follow-up below.
The shared task writes a `.sha256` beside it. The descriptor and filename agree;
IDE range is 261 through 263.*. Distribution verification and build-logic provenance
checks passed. The ZIP has one main JAR plus five Brikk runtime JARs, all three
root legal resources, no driver or test-support JAR and no bundled Kotlin or
serialization copies. See [artifact-provenance.md](../artifact-provenance.md).

The `.dirty` marker is intentional: the local initialization commit is followed by
uncommitted implementation files. There has been no push, release tag or publication.

## Plugin Verifier

These results apply to the original baseline ZIP. The subsequent driver VM-default
change, transport UI removal and catalog/editor follow-up were tested and rebuilt
below; Plugin Verifier was not rerun for those changes. The current catalog hooks
must receive a fresh API audit before publication.

```sh
JAVA_HOME=/home/jayson/.local/share/mise/installs/java/21.0.2 \
  ./gradlew verifyPlugin '-PverificationIdes=IU-262.10968.63 IU-263.5701.42' \
  --no-daemon --max-workers=2 --console=plain
```

Plugin Verifier 1.411 reports **Compatible** on both selected IDEs, with the same
remaining API notices on each:

- 1 internal usage: `DBIntrospector.Factory` in the shared
  `GenericJdbcIntrospectorGate`.
- 8 experimental usages: the platform's `DatabaseConnectionInterceptor` and
  `ProtoConnection` hooks in shared `HarvestConnectionInterceptor`.
- 1 deprecated override: `ModelHelper.getJdbcType(BasicTypedElement)` in shared
  `PgFamilyCatalogModelHelper`.

These are references to platform APIs from the embedded library, not experimental
markers on our own APIs. Nothing was suppressed or added to
`api-usage-approvals.txt`. Compatibility is not Marketplace approval; internal and
experimental review remains open for this plugin. No shared library was changed.

Log: `build/plugin-verifier.log`. Complete verdict, dependency and API usage reports
are retained under `build/reports/pluginVerifier/IU-262.10968.63/` and
`build/reports/pluginVerifier/IU-263.5701.42/`.

## Remaining qualification

Clean interactive installs, the full default verifier target set including
DataGrip, isolated sibling-plugin classloader coexistence and real Spice/DataFusion
Flight smoke tests are not covered by this baseline. See [TODO.md](../TODO.md);
no production transport or Marketplace approval is implied.

## Driver VM-default correction

Both product driver XML definitions now default to:

```text
--add-opens=java.base/java.nio=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow
```

The README no longer tells normal new-connection users to add the second flag.
Existing explicit driver overrides remain user-owned, and the Java 21 build
process is unchanged.

`test test262 test263 buildPlugin verifyDistribution dogfoodZip` passed again.
There are now 36 tests per IDE, with zero failures/errors/skips on each of
261.25134.95, 262.10968.63 and 263.5701.42: 108 executions for this change.
The new regression test launches each IDE's Java executable with the actual loaded
driver VM options and requires a successful exit. The loaded driver checks assert
both flags, and inspection of the rebuilt ZIP confirms both defaults are packaged.
Log: `build/driver-vm-defaults.log`. The earlier Kotlin-present matrix remains
baseline evidence; it was not rerun for this resource change.

A separate real JDBC read smoke also passed on Java 25.0.2 using the same VM
options read directly from the driver XML, the qualified Arrow 19.0.0 JAR, and
the disposable `datafusion-spice-local` Spice 2.4.0 container. It returned
`1 Ada 30`, `2 Bob 40` from `sample`, and JDBC product `Spice.ai`, then closed
the connection. Log: `build/local-spice/driver-vm-smoke.log`. This proves this
loopback read path, not TLS/auth, every transport operation or the IDE dialog's
actual connection launch; those broader checks remain tracked.

## Remove version-blind upstream transport warnings

Removed both the Spice editor banner provider and the transport statement
annotator. Neither checked the installed driver or server version; both could
keep reporting issues after a user upgraded. The banner's truncated text was
also unreadable. Upstream defects now have README notes naming the observed
Spice 2.4.0/Arrow JDBC 19.0.0 pair, correct server/driver ownership and issue links.
The plugin makes no claim to detect whether a later version contains a fix.

The replacement policy tests assert that the plugin registers no editor banner,
does not emit the old transport warnings on utility SQL, and still reports local
syntax errors. All 36 tests pass on each of 261/262/263, 108 executions with zero
failures/errors/skips. Build, distribution verification and dogfood packaging pass.
Log: `build/remove-transport-warnings.log`.

ZIP inspection confirms the removed classes and extension registrations are
absent, while both required driver VM flags remain present. The current checksum
above identifies this rebuilt ZIP. This change does not implement a transport
workaround, change execution delegation or suppress actual driver/server errors.

## Cold highlighting, namespace readback and lazy introspection

The catalog/editor hooks described in
[catalog-editor-integration.md](../catalog-editor-integration.md) are now packaged.
`test test262 test263 buildPlugin verifyDistribution dogfoodZip` passes with
43 tests per runtime on 261.25134.95, 262.10968.63 and 263.5701.42, zero
failures/errors/skips: 129 executions. Log: `build/catalog-integration.log`.

Seven added integration tests cover shared resolver ownership, readback of actual
catalog/schema names at a recording metadata boundary, no guessing when metadata
is missing, unset-scope initialization, preservation of explicit selections,
cold qualified/unqualified query introspection and typed-path completion. Model
updates complete without a document edit and the loaded relation resolves. The
readback SQL also parses with both core dialects. No live SQL or user connection
was used by these fixtures.

The final code additionally passes all 43 tests using the user's installed IDE
runtime build 262.9437.185 through:

```sh
JAVA_HOME=/home/jayson/.local/share/mise/installs/java/21.0.2 \
  ./gradlew test262 \
  -Ptest.ide262=/home/jayson/.local/share/JetBrains/Toolbox/apps/intellij-idea \
  --no-daemon --max-workers=2 --console=plain
```

Log: `build/catalog-user-build.log`. This launches an isolated test sandbox on the
installed IDE binaries; it does not drive or change the user's running IDE window.
There are 172 executions across these four runtime checks. The latest test262
XML/HTML reports represent 262.9437.185 because that run replaces the earlier lane.

No transport banner was reintroduced, both driver VM defaults remain defined, and
namespace-changing SQL is still disabled. Reinstall/restart/reconnect and retest
the live schema menu/default scope/cold editor state. Existing explicit schema
selections are not overwritten. A fresh verifier run and interactive endpoint
qualification remain open, as does the separate intention-description resource
finding captured from the user's log.

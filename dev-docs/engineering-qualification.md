# Engineering qualification follow-up

This report preserves the released 0.4.0 baseline and original qualification
request. Both races subsequently passed the accepted snapshot and public 0.4.1;
the ordinary default now consumes Brikk 1.0.1 (see release-readiness.md). See
stable-041-verification.md for the 0.4.1 evidence and candidate-verification.md
for the snapshot checkpoint.

This pass separates supported behavior, tested exclusions and dependency blockers.
It does not turn a green ordinary suite into a claim that the race reproducers pass.

## Saved credentials

The consumer uses Database Tools' normal credential API. Spice 2.4.0 accepts its
API key as the JDBC password in a Basic handshake and ignores the username. The
real driver tests now use this route instead of an advanced Authorization property.
Valid and invalid password/API-key handshakes are checked separately from TLS.

`PrivatePasswordSafe` selects JetBrains' real KeePass backend with a database and
master-key file under an owned build-directory fixture. It never selects the OS
keychain or changes the user's Password Safe configuration. The backend is
Kotlin-internal, so only the test fixture reflects its public JVM constructor;
no implementation, dependency or API exemption is added to the plugin.

The ordinary tests check encrypted-store reload, data-source XML reload with SDK
driver resolution, replacement isolation, copy/credential-key isolation, and that
memory-only credentials do not become persistent. Synthetic secret markers must
not appear in XML or encrypted-store files.

The opt-in restart lane runs a writer and a fresh reader test JVM with the same
owned store and saved driver reference. Neither reader memory nor the writer's
Password Safe service instance is reused. Each phase opens a real driver
connection, refreshes through a helper, and executes through a real console.

```sh
python3 tools/run-flight-smoke.py --product spice --spice-secure --ide-process --saved-credentials-restart --java-home /path/to/jdk21
```

Live Gradle tests are deliberately non-cacheable. The runner also requires fresh,
nonempty XML with no skipped/failed cases; a cached task cannot masquerade as a
successful reader phase. OS-keychain UI and driver-dialog interaction remain
separate manual checks, not requests for production secrets.

## Namespace contract

The qualified Spice 2.4.0 and DataFusion 55.2.0 endpoints reject `USE`, both through
Statement and PreparedStatement. Arrow JDBC 19's `setSchema` returns but neither
server readback nor a distinguishing unqualified query changes.

DataFusion does accept an explicit single-key
`SET datafusion.catalog.default_schema = 'information_schema'`. The test requires
both settings readback and successful unqualified lookup of `tables`, restores
`public`, and verifies that a fresh connection starts at the default. Spice rejects
that SET. The DataFusion embedding rejects multi-statement and multi-variable SET
forms needed to change an arbitrary catalog/schema pair in the IDE's single-SQL
switch hook. An explicit schema SET is not a qualified full-path UI switch.

The shared namespace strategy is now explicitly read-only, rather than retaining
an unused USE generator. Tests require no SQL switch/reset, no JDBC-switch
advertisement and `DbImplUtil.canSwitchTo == false`. Readback, introspection-scope
selection and manually authored native SQL remain separate features.

## Cache compatibility policy

V1 has no earlier published consumer cache format to migrate. The released store
reloads its current format; unknown library headers, unknown native formats and
malformed native payloads fall back to bundled metadata. Additive fields in the
current native format remain readable. A changed connection identity never
adopts another identity's file. These are explicit fail-closed compatibility tests,
not an invented cross-version migration or a relabeling of older identity keys.

## Confirmed shared-service blockers

The desired-contract `SharedFlightCatalogRaceTest` uses real SDK helper
connections and released native harvesters with deterministic completion gates.
Its two tests fail on released `brikk-sql-intellij-support:0.4.0`, for both products:

1. A harvest captured before `forget(dataSourceId)` can finish afterward and
   recreate the forgotten inventory.
2. Two same-identity helper refreshes can complete out of order. An older captured
   result overwrites the newer result already recorded. Revision markers belong
   to the test wrapper, not to fabricated endpoint versions.

```sh
python3 tools/run-flight-smoke.py --product spice --spice-secure --ide-process --shared-lifecycle-probe --java-home /path/to/jdk21
python3 tools/run-flight-smoke.py --product datafusion --ide-process --shared-lifecycle-probe --java-home /path/to/jdk21
```

These opt-in commands must fail until the dependency is fixed. They are excluded
from ordinary/live smoke selection unless explicitly requested. Evidence is under
`build/readiness/*-shared-race-test-results/` and
`build/engineering/shared-races.log`. The owner of recording/order fencing is the
released shared `LiveCatalogService`, not Arrow or Spice. A shared fix needs
per-source invalidation and refresh-generation fencing before recording; this
consumer must not grow a second cache/generation manager or use a locally patched
library to hide the issue. A reviewed released dependency is required to close
these gates.

## Cancellation and TLS scope

The explicit cancellation lane receives a real first streaming row, calls cancel
under a five-second observation bound, requires the cursor to stop and queries
again on the same connection. The generator is bounded-batch, fixtures are
runner-owned and the entire test process has a 45-second outer limit. This is not
a guarantee about pending prepare/schema RPCs, noncooperative cleanup or all
server-side work after a client stream cancellation.

DataFusion TLS qualification uses a test-only TLS-terminating, ALPN-h2 byte proxy
in front of the unmodified upstream fixture. It changes no SQL, RPC headers or
authentication decisions. It tests the DataFusion driver's encrypted transport
and certificate/hostname verification, not a universal DataFusion server-side
authentication policy. The bounded reference endpoint intentionally does not
validate arbitrary deployment passwords or implement production authorization.

## DataGrip consumer lane

The earlier DataGrip probe reused an IDEA lane whose bundled dependencies always
included Java/Kotlin. That was a build-lane limitation, not a DataGrip runtime
failure. A dedicated SQL-only consumer lane now uses JetBrains' public testing
DSL and requests only Database Tools, JSON and Git, which DataGrip bundles.
No vendored build logic, SDK binary or plugin dependency is patched/transplanted.

```sh
./gradlew testDataGrip -Ptest.dataGripIde=/path/to/datagrip
python3 tools/run-flight-smoke.py --test-task testDataGrip --data-grip-ide /path/to/datagrip --ide-process --java-home /path/to/jdk21
```

All 84 ordinary consumer tests pass on actual DataGrip 261.24374.56 and
262.10315.132. Classpath checks require only the selected DataGrip SDK root, not
mixed IDEA SDK classes. XML, product-info and summaries are under
`build/engineering/stable84-datagrip-261/` and `stable84-datagrip-262/`. This remains automated
headless qualification, not a claim to have driven the user's DataGrip window.

## Latest local verdict

The ordinary 84-test suite passes on IDEA 261/262/263 with Kotlin absent and
present, 504 executions, plus 84 tests on each actual DataGrip 261/262 runtime.
The final serialized runner is `build/engineering/serial-resume.log`; its raw XML
and summaries are in the `stable84-*` directories. Nine Python tests pass.

The passing live checks are separate from the failing shared-race reproducers:
the four IDEA transport/product variants total 210 executions, the separate
credential writer/reader JVMs add 12, active-stream cancellation adds six, and
secure real-console runs on DataGrip 261/262 add 80. The total is 308 passing live
executions. The two Brikk race contracts still fail for each product and are not
included in that passing total.

The owner has asked the existing Brikk agent for an upstream fix and a local
SNAPSHOT candidate. That candidate must pass the failed contracts and this
consumer matrix before a final candidate dogfood build. No shared sources or
published release coordinates have been patched by this consumer session.

Candidate-only runs accept `-Pqualification.brikkSupportVersion=<explicit-SNAPSHOT>`
or `BRIKK_SUPPORT_CANDIDATE=<explicit-SNAPSHOT>`. The shared repository policy
resolves Brikk snapshots exclusively from Maven Local. The ordinary default stays
on 0.4.0; SQL core/metadata/verify API stay on released 0.23.0. The candidate build
base becomes `0.1.0-qualification`, so it cannot overwrite or masquerade as the
ordinary dogfood label. Non-snapshot overrides and `-Prelease=true` with a
candidate are rejected. Exact candidate coordinates and hashes are required
before testing. This is an explicit owner-authorized integration exception, not
permission to publish a stable library or plugin release.

Exact runtime/count evidence belongs in testing.md. Live UI, owner API review and
publication remain separate gates.

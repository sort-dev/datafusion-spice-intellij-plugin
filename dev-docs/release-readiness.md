# Release readiness review

## Next release: IDE 2026.2 and later

The next release drops IDE line 2026.1 (build 261). The vendored conventions at
`387c9f39bf66142cf9f07074bc340f44ae1d3801` compile against the 262 SDK with
since-build 262 (until-build stays 263.*) and Java 25 bytecode, class major 69,
because 2026.2 requires Java 25. `mise.toml`, CI and the release workflow input
use Java 25. The draft change notes in plugin.xml and `change-notes.html` carry an
Unreleased line saying so; past entries are unchanged. No version was chosen.

Local gates on JDK 25.0.2, all with zero failures, errors or skips:

| Gate | Result |
| --- | --- |
| `./gradlew build` (includes `verifyDistribution`, `verifyBuildLogic`) | 85 tests on 262, ZIP verified |
| `test262 test263 -Ptest.kotlin=absent` | 85 per runtime |
| `test test262 test263 -Ptest.kotlin=present` | 85 per runtime |
| Python tools tests | 15 |
| `buildPlugin verifyPlugin` (four targets, no 261) | Compatible on all four: 1 internal, 11 experimental, 1 deprecated, unchanged |
| `check-api-approvals.py` | 1 approved internal usage, 0 unapproved per target |

The verifier targets are DB-262.10315.132, IU-262.10968.63, IU-263.4732.28 and
IU-263.5701.42. The packaged descriptor declares since-build 262, and every
plugin class is major 69. The `-Ptest.pluginIsolation=true` lane, with SQL
Transpiler absent and the released ClickHouse 0.4.0, Doris 1.7.0, DuckDB 0.7.0
and Trino 0.6.0 ZIPs, runs only `PluginIsolationTest`: 5 tests per runtime on
`test`/`test262`/`test263`, 15 executions. The installed-ZIP variant, the
DataGrip SQL-only lane and the live Flight fixtures were not rerun.

## Published 0.3.0 (Brikk 1.0.1)

The owner-authorized GitHub release is published:
https://github.com/sort-dev/datafusion-spice-intellij-plugin/releases/tag/v0.3.0.
Source/tag commit: `6b11b06fbb805cafee0439e2443bbfd4cf9cc50c`, which passed main CI
https://github.com/sort-dev/datafusion-spice-intellij-plugin/actions/runs/42717571779
with 85 tests per runtime in both Kotlin modes. The shared release workflow passed:
https://github.com/sort-dev/datafusion-spice-intellij-plugin/actions/runs/42723883476.

The published `datafusion-spice-intellij-plugin-0.3.0.zip` has SHA-256
`51010ca81f84d285f468042b8ba5c46a946ea2f28eddc32ca368429669a43a5a`, matching the
GitHub asset digest. It passes `verify-release-zip.py`, declares version 0.3.0 and
builds 261–263.*, and embeds the same five Brikk 1.0.1 JARs as the local
release-mode ZIP below. The main plugin JAR differs from the local build only in
its manifest's Build-JVM and Build-OS lines. Marketplace upload was not performed.

### Preparation record

Plugin 0.3.0 moves SQL core, metadata and verification API, and IntelliJ
common/JDBC/test support, from 0.23.0/0.4.1 to 1.0.1 on Maven Central (brikk-sql
main `0ecace0f`, changelog `brikk-sql/docs/sql-1.0.1.md`). No plugin code
changed. Brikk catches are not used in `src/main`, so the new `BrikkSqlError` base
class needs no consumer change. Change notes are in plugin.xml and
`change-notes.html`; suggested GitHub text is in `github-0.3.0.md`.

Of the SQLGlot 30.29 catch-up fixes, two reach the DataFusion and Spice dialects:

- `13END` is now read as `13 END`, so Brikk no longer reports "Expected END after
  CASE". The editor still shows the inherited PostgreSQL lexer's "Bad characters"
  error for it, so the change notes do not claim it as fixed. See TODO.md.
- A VALUES alias is written before its nested join in generated DataFusion SQL.
  Direct `transpile` probes for both dialects confirmed the 0.23.0 misordering
  and the corrected 1.0.1 output.

The MySQL, Doris and DuckDB items in that changelog do not apply to these dialects.
`EditorFeaturesTest` adds one test for both cases. On 0.23.0/0.4.1 it fails with
Brikk's CASE error; on 1.0.1 it passes.

Local gates on JDK 21.0.2, all with zero failures, errors or skips:

| Gate | Result |
| --- | --- |
| `./gradlew build` (includes `verifyDistribution`) | 85 tests on 261, ZIP verified |
| `test test262 test263 -Ptest.kotlin=absent` | 85 per runtime |
| `test test262 test263 -Ptest.kotlin=present` | 85 per runtime |
| Python tools tests | 15 |
| `buildPlugin verifyPlugin` (five targets) | Compatible on all five: 1 internal, 11 experimental, 1 deprecated, unchanged |
| `buildPlugin verifyPlugin -Prelease=true` with the release workflow's two IDEs | Compatible |
| `check-api-approvals.py` | 1 approved internal usage, 0 unapproved per target |
| `verify-release-zip.py` | passes for `datafusion-spice-intellij-plugin-0.3.0.zip` |
| Installed-ZIP lane, published 0.2.0 then 0.3.0 in the same sandboxes | 5 tests × 3 runtimes each, 30 executions |

The local release-mode ZIP embeds exactly the five 1.0.1 runtime JARs listed in
artifact-provenance.md, which match the Maven Central SHA-1 sidecars. Its local
SHA-256 was `061330cbdd4b589b26d8d303eccc81ff329b7848fbd6cb5735837b989702fa37`;
the workflow builds its own ZIP. The opt-in dynamic lifecycle probe, DataGrip
SQL-only lane and live Flight fixtures were not rerun; their earlier results
and the owner's deferral of restart-free changes still apply. Logs and archived
XML are under `build/release-030/`.

The owner authorized the dispatch from `release/0.3.0`. Marketplace upload
remains separate.

## Published 0.2.0

The owner-authorized GitHub release is published:
https://github.com/sort-dev/datafusion-spice-intellij-plugin/releases/tag/v0.2.0.
Source/tag commit: `becd4ce3ab9f571567b52a0dcd7e26ea694673a2`.
The shared release workflow passed:
https://github.com/sort-dev/datafusion-spice-intellij-plugin/actions/runs/42255315150.

The published `datafusion-spice-intellij-plugin-0.2.0.zip` was downloaded and
independently checked for descriptor/version, the exact runtime payload and
public Brikk 0.4.1 hashes. SHA-256:
`af512ddef536e684555341ba4e1d52b5377aeb255d0548a7df369ebe2937ee28`.
The checksum file is attached to the release. Public release text is in
`dev-docs/release/github-0.2.0.md`. Local records are under
`build/release-020/published/` and `published-record.json`.

Both Kotlin modes passed 84 ordinary tests on each of the three IDEA runtimes,
504 executions. All five local verifier targets were Compatible; the accepted
API baseline passed the exact approval check. Captures are left to the owner
for later. Marketplace upload was not performed. The sample IDE was left open.

## Original preparation record

The owner has now authorized a 0.2.0 GitHub release, including its branch/tag and
release ZIP. Screenshots will be supplied by the owner afterward and do not
block this release. Marketplace upload is not part of this authorization.
The original preparation record below is retained for context. The owner has
confirmed interactive dogfooding for both DataFusion and Spice. Exact IDE builds
were not supplied, so that confirmation is not a cross-build install/unload matrix.
Spice saved-credential restoration and username/password transmission were also
confirmed; its local server did not enforce authentication.

## API review

Owner decision: the existing usages are accepted, consistent with the sibling
plugins. Do not request another decision on this baseline unless new or changed
usages appear. The exact internal reference from this plugin's report is recorded
in `api-usage-approvals.txt`; no sibling file or broad package exemption was copied.
The risk discussion below records the original review, not an outstanding request.

The released support 0.4.1 ZIP has the same findings on all five configured full
verifier targets. Exact findings and report hashes are in
`build/release-review/api-audit.json`, generated with:

```sh
python3 tools/audit-release-readiness.py \
  --version 0.1.0-dev.202611051726+b4734e4bc399 \
  --output build/release-review/api-audit.json
```

| Finding | Count per target | Owner and assessment |
| --- | --- | --- |
| Internal `DBIntrospector.Factory` reference in `GenericJdbcIntrospectorGate` | 1 | Released Brikk JDBC support. The gate vetoes PostgreSQL native introspection inherited through extension fallback, selecting generic JDBC instead. Removing it could send PostgreSQL catalog queries to either engine. Owner accepted this exact reference. |
| Experimental `DatabaseConnectionInterceptor` / `ProtoConnection` usages | 8 | Released Brikk support. These implement connection harvesting and helper exclusion. Keep per-IDE real connection regression coverage. |
| Experimental `ProtoConnection` usages in `ProductHarvest` | 3 | This plugin. Two signature references and one `getProject()` call. The method captures the project before the EDT handoff and pins identity around harvest/readback. Removing it loses tested first-connect scope behavior. Retain only with explicit risk acceptance or a reviewed replacement. |
| Deprecated `ModelHelper.getJdbcType(BasicTypedElement)` override | 1 | Released Brikk support, `PgFamilyCatalogModelHelper`. Still resolves on the supported targets. An upstream replacement is preferable to changing the consumer's model contract. |

Retain the tested connection behavior. Routine verifier checks should detect
new or changed usages, not reopen the already accepted ones. No sibling approval
files or internal-API package allowlists were copied. No shared/vendored code was edited.

The published JDBC-support source explicitly documents the absence of a public
factory replacement and why a saved "Use JDBC-based introspector" flag is not a
replacement: the platform resets it after an IDE build change. Any replacement
needs an upstream adapter review and released dependency, not copied SQL or
platform implementation code. The published sources used for this inspection
are retained under `build/release-review/upstream-sources/`.

`tools/audit-release-readiness.py --require-api-clean` is a zero-usage diagnostic,
not a release requirement for the accepted baseline. Its historical report does
not grant approval itself. The shared release workflow checks the exact owner
approval file and still rejects additional internal references.

## Installation, upgrade and unload coverage

Existing ordinary suites load actual descriptors and exercise services, PSI,
console integration and background logged errors across 261/262/263. Private
classloader tests exercise all four Execute wrappers and explicit customizer
detach. Neither alone proves whole-plugin dynamic unload or classloader reclamation.

The opt-in installed-ZIP lane loads an archived main JAR and its exact runtime
libraries through the plugin classloader, with no flattened product/Brikk runtime.
The test compares every installed library's name and SHA-256 to the input ZIP.
Each IDE task has separate sandbox config/system/plugin directories. Reusing
each sandbox for snapshot then stable replacement checks that old snapshot
libraries do not remain. This is restart-time installation/upgrade coverage,
not the Plugins dialog or driver-settings migration UI.

The snapshot and stable-upgrade passes each completed five private-classloader
tests on all three IDEA runtimes, 30 executions total with zero failures/errors/
skips. Immutable evidence is `build/release-review/snapshot/` and
`stable-upgrade/`; the serialized log is `build/installed-zip-matrix.log`.

```sh
./gradlew test test262 test263 \
  -Ptest.pluginIsolation=true -Ptest.sqlTranspiler=absent \
  -Ptest.installedPluginZip=/absolute/path/to/plugin.zip
```

This private-classloader lane also requires the shared convention's local peer
ZIPs, or their explicit `-Ptest.<peer>PluginZip` paths. It does not download peer
plugins from Marketplace or modify their checkouts.

Adding `-Ptest.dynamicPluginLifecycle=true` attempts actual dynamic disable and
re-enable of the archived plugin in the disposable test IDE. It requires unload
eligibility, checks that both languages and Refresh actions disappear, reloads,
then checks that both return. The normal isolation tests also verify the Execute
chains afterward. This is idle structural lifecycle coverage, not an active
connection/held-RPC disposal test or proof of classloader garbage collection.
The harness drives the SDK's internal `DynamicPluginEnabler` with a method-scoped
test-only opt-in. This test class is never shipped; no production API approval
or distribution exemption is granted by that annotation. Any saved enable-state
changes are confined to the disposable test sandbox, never the user's IDE profile.

The first 261 lifecycle attempt used disable semantics, which retained the
installed descriptor and made reload fail as "still loaded". Its failed XML/logs
are preserved in `build/release-review/dynamic-lifecycle-261-failed/` and
`dynamic-lifecycle-261.log`. A subsequent low-level probe encountered a changed
262 test-driver API, also preserved in `dynamic-lifecycle-262-api-failed/`.
The final harness uses the SDK's own enable/disable path instead of low-level
plugin-set assumptions, without editing production behavior or suppressing errors.
261 passes; 262 and 263 reject dynamic disable in the final headless probe. This is an open
qualification result, not proof of an end-user production defect. Final per-target
results are retained in `build/release-review/dynamic-stable-entrypoint/` and
`dynamic-stable-entrypoint.log`: five tests pass on 261; each newer lane has four
passes and one strict dynamic-disable failure, 13 passes and two failures total.
Earlier test-driver API mismatches remain in the historical failed archives.
The final harness calls the common single-argument enable/disable entry points,
not SDK-version-specific overloads. No result is converted to an expected skip.

Owner decision: accept this limitation and defer further checking to normal use.
Do not advertise restart-free disable/update. These headless results are retained,
but further investigation is not a release blocker. No descriptor restart policy
was changed as part of this decision.

Pending interactive matrix, in disposable IDE profiles rather than saved user settings:

1. On each of 262/263, install the exact clean ZIP through Install Plugin from Disk.
2. Upgrade an older plugin using both default driver settings and an explicit driver
   VM override. Verify explicit overrides survive and Java 25 defaults retain both flags.
3. Open both dialects and a console, then disable the plugin. Record whether the IDE
   requires restart. If unload is allowed, check actions, languages, services and
   connection callbacks detach without logged errors. Re-enable and check both again.
4. Restart, reconnect and inspect logs. A restart-required result is acceptable only
   if documented; do not claim dynamic unload from the verifier's "probably" verdict.

## Listing and legal assets

- Listing copy: `dev-docs/release/listing.html`.
- Initial change notes draft: `dev-docs/release/change-notes.html`.
- Privacy notice draft: `dev-docs/release/privacy.md`, including database traffic,
  local metadata caches and the separate IDE/driver privacy boundary.
- Listing identity stays `dev.sort.datafusion-spice-intellij-plugin`, title
  `SQL Dialects for DataFusion & Spice`, vendor Sortdev SRL.
- Existing original icons are `src/main/resources/META-INF/pluginIcon.svg` and
  `pluginIcon_dark.svg`. They use geometric artwork rather than upstream logos.
- LICENSE is Apache-2.0. NOTICE and THIRD_PARTY_NOTICES retain upstream attribution
  and exact runtime coordinates. Distribution checks require their packaged copies.
- Copy disclaims affiliation, limits DataFusion endpoint/tree claims, makes PIPE
  opt-in and distinguishes local diagnostics from native verification and server DDL.
- Needed before listing submission: real, redacted screenshots of each dialect's
  completion/navigation, the connection template without credentials, and PIPE
  preview. No screenshots were fabricated or captured from the owner's live IDE.
- Owner still reviews product-name/trademark wording, vendor/contact details,
  privacy/support links and listing screenshots. This is not legal clearance.

## Owner-only release procedure

The prepared `.github/workflows/release.yml` has only manual dispatch. It calls
the released shared workflow at the exact vendored-conventions commit. It has
no push/tag trigger, no Marketplace step and no publishing token. Do not dispatch
it until the owner separately authorizes a GitHub release.

That shared workflow creates a public tag and GitHub release, so even a manual
dispatch is a publication action. Its typed confirmation is a safeguard, not an
authorization granted by this review.

Before a separately authorized release:

1. Preserve the accepted API baseline and deferred restart-free behavior. Reopen
   API review only if new usages appear; never import approvals from a peer.
2. Choose the release version and update the single literal `version = "..."` in
   `build.gradle.kts`. Candidate labeling remains a separate guarded adjustment.
   Account for JetBrains ordering a same-base `-dev` build after the bare version;
  the current 0.1.0 dogfood must not silently prevent users receiving a release.
  The owner selected 0.2.0 to supersede the existing 0.1.0-dev builds.
3. Replace the development change notes in plugin.xml with the approved notes,
   then merge the prepared release commit into main after the full verification.
4. Only with explicit authorization, create `release/<version>` at that exact
   main commit and manually dispatch with confirmation `publish-github-release`.
   The shared workflow checks main ancestry, branch/version identity, release
   dependencies, verifier findings and ZIP identity before making a tag/release.
5. Marketplace upload remains a second, separate owner-authorized action. Upload
   only the audited release ZIP with the approved listing/assets; never rename
   a dogfood or snapshot qualification ZIP into a release.

No workflow dispatch, release branch, tag, GitHub release or Marketplace upload
was performed as part of this review.

## Validation of this preparation

The ordinary IDEA matrix passed 84 tests per runtime in each Kotlin mode,
504 executions, with zero failures/errors/skips. All five full verifier targets
remain Compatible with the API findings above. Build/distribution and the
snapshot-plus-release rejection guard pass. Fifteen Python evidence/review tests
and actionlint with shellcheck pass. Logs and immutable ordinary XML are under
`build/release-review/`; `final-api-audit.json` records the fresh verifier audit.
These green results do not override the separately retained dynamic-lifecycle
failures or grant publication approval.

Hosted CI passed on preparation commit `d8df30a05a519410175fb090496a499bfb63b016`:
https://github.com/sort-dev/datafusion-spice-intellij-plugin/actions/runs/42247149493.
Downloaded artifacts confirm 84 passing ordinary tests per runtime in both Kotlin
modes, 504 executions. Evidence-tool tests, distribution checks and artifact
uploads passed. Hosted verifier was skipped; the five verifier results above
are local. This CI run does not execute the opt-in dynamic lifecycle probe and
does not supersede its retained 262/263 failures. Downloaded XML/summaries are
under `build/release-review/hosted-test-results/`.

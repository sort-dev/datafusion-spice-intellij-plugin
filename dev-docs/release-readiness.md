# Release readiness review

This is preparation, not approval to tag, release or publish. The owner has
confirmed interactive dogfooding for both DataFusion and Spice. Exact IDE builds
were not supplied, so that confirmation is not a cross-build install/unload matrix.
Spice saved-credential restoration and username/password transmission were also
confirmed; its local server did not enforce authentication.

## API review

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
| Internal `DBIntrospector.Factory` reference in `GenericJdbcIntrospectorGate` | 1 | Released Brikk JDBC support. The gate vetoes PostgreSQL native introspection inherited through extension fallback, selecting generic JDBC instead. Removing it could send PostgreSQL catalog queries to either engine. No exemption granted. |
| Experimental `DatabaseConnectionInterceptor` / `ProtoConnection` usages | 8 | Released Brikk support. These implement connection harvesting and helper exclusion. Keep per-IDE real connection regression coverage. |
| Experimental `ProtoConnection` usages in `ProductHarvest` | 3 | This plugin. Two signature references and one `getProject()` call. The method captures the project before the EDT handoff and pins identity around harvest/readback. Removing it loses tested first-connect scope behavior. Retain only with explicit risk acceptance or a reviewed replacement. |
| Deprecated `ModelHelper.getJdbcType(BasicTypedElement)` override | 1 | Released Brikk support, `PgFamilyCatalogModelHelper`. Still resolves on the supported targets. An upstream replacement is preferable to changing the consumer's model contract. |

The engineering recommendation is to retain the tested connection behavior for
now, keep the internal reference as an explicit owner decision, and require a
fresh full verifier audit for every supported SDK update. This recommendation
does not approve an exemption. No sibling approval files or internal-API package
allowlists were copied. No shared/vendored code was edited.

The published JDBC-support source explicitly documents the absence of a public
factory replacement and why a saved "Use JDBC-based introspector" flag is not a
replacement: the platform resets it after an IDE build change. Any replacement
needs an upstream adapter review and released dependency, not copied SQL or
platform implementation code. The published sources used for this inspection
are retained under `build/release-review/upstream-sources/`.

`tools/audit-release-readiness.py --require-api-clean` intentionally fails while
the internal/experimental usages remain. The shared release workflow independently
checks exact owner-approved internal call sites. Without an owner approval file,
the current internal finding blocks that workflow.

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

Recommended current policy: do not advertise restart-free disable/update. Keep
restart-based installation available and require owner acceptance of that
limitation or complete the investigation before publication. No descriptor
restart policy was changed without that decision.

Pending interactive matrix, in disposable IDE profiles rather than saved user settings:

1. On each of 261/262/263, install the exact clean ZIP through Install Plugin from Disk.
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

1. Finish the API decision and install/unload matrix. An exact internal-API approval
   may only be added with explicit owner authorization; never import one from a peer.
2. Choose the release version and update the single literal `version = "..."` in
   `build.gradle.kts`. Candidate labeling remains a separate guarded adjustment.
   Account for JetBrains ordering a same-base `-dev` build after the bare version;
  the current 0.1.0 dogfood must not silently prevent users receiving a release.
  A 0.1.1 first public version would supersede the current 0.1.0-dev builds;
  that is the recommended versioning choice, not a version change made here.
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

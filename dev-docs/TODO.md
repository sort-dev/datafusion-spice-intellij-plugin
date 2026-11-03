# Readiness backlog

This is not a finished-plugin checklist with everything ticked. Automated evidence
is in [testing.md](testing.md). A working build and compatible verifier verdict do
not establish the live editor/connection experience.

## Core gates still open

- [ ] Confirm the reported cold/open-query red references, schema dropdown,
  default namespace selection and typing-triggered introspection in a live IDE.
  The missing hooks are implemented and recording-model tests cover no-edit sync;
  that is not a claim to have driven the user's window.
- [ ] Exercise clean interactive ZIP installs on 261/262/263, including actual
  driver-process launch, secure credential handling, URL controls, both dialects,
  settings, preview/run UI and disposal/unload behavior.
- [ ] Verify driver-settings upgrade behavior interactively. Connection VM fields
  contain additions, not inherited driver values. The user's saved Spice driver
  override had only the old --add-opens value; it must be corrected at driver
  level once. Preserve explicit overrides; do not edit live IDE settings on disk.
- [ ] Qualify DataFusion TLS/auth, production credential handling and bounded
  cancellation for supported endpoints. Disposable Spice 2.4.0 / Arrow 19 checks
  now pass with TLS and API keys, including four rejection paths, but direct JDBC
  is not a Password Safe or driver-dialog test. DataFusion's fixture handshake is
  not a production authorization policy. Do not run dangerous leak/failure probes
  in the user's IDE or claim unsupported operations work.
- [ ] Cover fresh-process/cross-version cache migration, out-of-band metadata
  changes, stale in-flight refresh and reconnect/disposal in the consumer. Native
  inventory disk round-trip and memory-only session behavior are now tested under
  each IDE runtime, but that is narrower than this full lifecycle gate.
  This pass adds separate writer/reader JVMs and connection-identity race checks;
  cross-library-format migration and same-identity concurrent refresh/removal
  semantics remain separate gates, not implied by those tests.
- [ ] Expand physical-column rename/find-usages and injected-host coverage.
  Navigation to loaded model columns is now checked in the no-edit introspection
  fixtures. Function docs/links, named-option insertion, local alias rename and
  explicit dollar-string injection have regression tests. The stock PostgreSQL
  single-quoted host fixture retained doubled quote escapes; do not treat that as
  qualified host decoding or add automatic Kotlin recognition as a local fix.

## Decisions and release gates

- [ ] Review the current API audit before publication: 1 internal factory
  reference, 11 experimental usages and 1 deprecated model-helper override per
  audited IDE. Three experimental references are in ProductHarvest; capturing the
  Project before the EDT handoff removed four unnecessary ProtoConnection usages.
  No exemptions have been granted or copied from peers. Compatible is not approval.
- [ ] Define the advertised DataFusion endpoint/tree contract. The bounded test
  embedding has native information-schema inventories but lacks JDBC table-metadata
  RPCs. Do not promise a full database tree from those facts or invent a local
  native-model writer; a broader tree requirement needs a reviewed shared design.
- [ ] Qualify namespace-changing operations before enabling UI-generated USE.
  Readback and unset-scope seeding work independently; a zero update count cannot
  prove a server namespace changed.
- [ ] Prepare the shared reusable release workflow and final listing assets,
  trademark/licensing review and owner-only upload procedure. Commit/push is now
  authorized; GitHub releases/tags and Marketplace publication are not.
- [ ] Request sixth-plugin adoption in shared staging and reverse peer-isolation
  lists. Do not modify the vendored conventions to hide hardcoded shared lists.

## Optional, not silently part of v1

- [ ] Automatic Brikk Kotlin-host and SQL Transpiler .bsql integration. No Kotlin
  helper or custom host recognition is currently shipped. Ordinary explicit SQL
  injection is a separate, narrower feature.

## Implemented and automatically checked

- [x] Checkpoint and push the complete baseline, source, legal files and developer
  backlog to main. Initial safety checkpoint: 459380e.
- [x] Both exact dialect/DBMS identities, explicit file mapping, parsing,
  highlighting, local diagnostics and product-specific Arrow driver definitions.
- [x] Both required VM flags in both driver defaults; real launch-configuration
  assembly retains them even with a blank connection-level VM-options field.
- [x] Shared native catalog lifecycles, stable resolvers, IDE-model facts, namespace
  readback, empty-scope seeding and targeted lazy introspection on open/completion.
- [x] Connection, refresh and session-inventory invalidation without text edits.
  Explicit user schema selections remain unchanged.
- [x] Manual Refresh has one-off outcome feedback through the service's existing
  callback and shared escaped-notification helper. Automatic events stay quiet,
  raw driver details are not exposed, and no editor banner is registered.
- [x] Fix the consumer scope race by pinning identity before the native harvest,
  then checking identity/product ownership before readback and before applying
  defaults. The new regression reproduced the stale-scope bug before the fix.
- [x] Separate off-by-default workspace PIPE settings, cooperative execution,
  preview and run-to-stage. Ten qualified lowered stages execute in both pinned
  disposable endpoints; unsupported stages stay refusals.
- [x] Actual shared harvesters over JDBC for Spice 2.4.0 and the pinned DataFusion
  55.2.0 execution embedding, namespace query, parameter rebinding and fresh
  connections. JSON-function query passes on Spice; DataFusion session-only view
  isolation and qualified nullable binding pass. No models or paid calls were run.
- [x] Disposable Spice TLS/API-key fixture with certificate verification enabled.
  All six server checks pass over TLS; positive auth, missing/invalid key,
  untrusted root and hostname-mismatch checks also pass on the 261 runtime.
- [x] Native catalog disk serialization and no persistence of session-only facts.
  Separate writer/reader JVMs now check current-format reload for both products;
  this does not close the cross-library migration gate.
- [x] Intention descriptions and before/after resources for all four PIPE actions,
  with real IntelliJ metadata/resource lookup tests.
- [x] Function documentation/link, named-argument insertion, alias rename and
  explicit injected SQL parser/Unicode-offset regressions.
- [x] Isolated private Brikk copies and cooperative Execute chains with four
  archived peer ZIPs and snapshots of current installed ClickHouse/Doris/DuckDB/
  Trino payloads on 261/262/263; four checks per runtime pass in each set. Snapshot
  versions and hashes are recorded in testing.md and the ignored build evidence.
- [x] Full configured verifier target set, including DataGrip: Compatible, with
  all API notices retained. See the open approval gate above.
- [x] CI workflow matching peer build/test/artifact conventions; no publication job.
  First hosted run 41673732794 passed on implementation commit f656bae: both
  Kotlin modes on 261/262/263, build/distribution checks and artifact upload.
  The hosted verifier job is manual-only and was skipped on that push; the five
  compatible verifier verdicts above came from the separate local audit.
- [x] Remove version-blind transport banners/statement warnings. README names the
  observed versions, actual upstream owners and issue links instead.

## Intentional boundaries

Background native verification is unavailable: do not execute EXPLAIN or prepare
user SQL to validate the editor. Upstream Spice/Arrow transport defects are not
plugin-owned SQL errors. A newer installed driver/server must not inherit assumed
bug warnings. Do not map GenericSQL/PostgreSQL/all Flight URLs to either product.

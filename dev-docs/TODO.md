# Readiness backlog

This is not a finished-plugin checklist with everything ticked. Automated evidence
is in [testing.md](testing.md). A working build and compatible verifier verdict do
not establish the live editor/connection experience.

## Core gates still open

- [ ] Confirm the reported cold/open-query red references, schema dropdown,
  default namespace selection and typing-triggered introspection in a live IDE.
  The missing hooks are implemented and recording-model tests cover no-edit sync;
  that is not a claim to have driven the user's window.
  Real remote-process tests also exposed and fixed first-connect version learning
  invalidating the catalog identity. See remote-driver-qualification.md.
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
  Consumer caller-cancellation now closes a held synthetic metadata statement
  without seeding scope. That does not bound real server cancellation, namespace
  probes or RPC cleanup when a driver ignores cancel.
- [ ] Qualify cross-library cache-format migration, same-identity concurrent
  refresh/removal, arbitrary out-of-band metadata changes and reconnect/disposal
  ordering in the consumer. Native
  inventory disk round-trip and memory-only session behavior are now tested under
  each IDE runtime, but that is narrower than this full lifecycle gate.
  This pass adds separate writer/reader JVMs and connection-identity race checks;
  cross-library-format migration and same-identity concurrent refresh/removal
  semantics remain separate gates, not implied by those tests.
  Out-of-band inventory changes now have real-console manual-refresh checks on
  all three runtimes. Native cache/editor-facts revisions change without an edit;
  arbitrary server-side changes and reconnect/disposal ordering remain broader gates.
- [ ] Qualify database-DDL rename UI and broaden quoted physical-column/host
  coverage. Indexed physical-column find usages and reference-only rename now
  keep same-named schema columns and string literals separate. Both explicitly
  injected dialects have tagged-dollar Unicode/error/completion-insertion checks.
  Those do not qualify database mutation or automatic host recognition. The stock
  PostgreSQL single-quoted host fixture retained doubled quote escapes; do not
  treat that as qualified host decoding or add automatic Kotlin recognition as a fix.

## Decisions and release gates

- [ ] Obtain a reviewed DataGrip consumer-test lane in shared conventions. The
  DataGrip 262 attempt fails before tests because the convention always requests
  the unavailable bundled Java host plugin. Do not edit vendored build logic or
  transplant IDEA's Java plugin to disguise this gap. Verifier compatibility is
  separate from an executed DataGrip consumer suite.
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
- [x] Separate user connection settings from automatically observed server version
  in consumer catalog identity. A first IDE remote connection and first helper
  refresh now retain their inventory; actual user/driver/property edits still
  invalidate it. No shared SQL machinery or vendored library was changed.
- [x] Real IDE-launched Arrow driver process against disposable Spice/DataFusion,
  inherited VM flags, first native harvest/scope, helper startup-script isolation
  and fresh connections after close. This is not an interactive driver-dialog test.
- [x] Actual JdbcConsole Execute for ordinary SQL and opted-in PIPE, registered
  manual Refresh, correlated session inventory and a healthy following request.
  Both plaintext products and secure Spice pass on 261/262/263. Generated SQL is
  submitted without replacing the user's PIPE document; automatic events stay quiet.
- [x] Out-of-band Spice pod reload and DataFusion session-registry changes become
  visible to real-console manual Refresh without changing document text. The
  mutation is proved by reading actual rows, not an update count. Both plaintext
  products and secure Spice pass on all three runtimes. DataFusion's synthetic
  remote password uses Database Tools' memory credentials API, not the URL.
- [x] Separate off-by-default workspace PIPE settings, cooperative execution,
  preview and run-to-stage. Ten qualified lowered stages execute in both pinned
  disposable endpoints; unsupported stages stay refusals.
- [x] Actual shared harvesters over JDBC for Spice 2.4.0 and the pinned DataFusion
  55.2.0 execution embedding, namespace query, parameter rebinding and fresh
  connections. JSON-function query passes on Spice; DataFusion session-only view
  isolation and qualified nullable binding pass. No models or paid calls were run.
- [x] Disposable Spice TLS/API-key fixture with certificate verification enabled.
  All six server checks pass over TLS; positive auth, missing/invalid key,
  untrusted root and hostname-mismatch checks also pass on all three runtimes.
- [x] Native catalog disk serialization and no persistence of session-only facts.
  Separate writer/reader JVMs now check current-format reload for both products;
  this does not close the cross-library migration gate.
- [x] Intention descriptions and before/after resources for all four PIPE actions,
  with real IntelliJ metadata/resource lookup tests.
- [x] Function documentation/link, named-argument insertion, alias rename and
  explicit injected SQL parser/Unicode-offset regressions.
- [x] Indexed physical-column references across same-named tables in different
  schemas, reference-only SQL rename, tagged-dollar injected completion insertion
  for both products, and actual consumer-hook cancellation at a recorded boundary.
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
  Lifecycle implementation 5f7b4ea also passed hosted CI 41688372935 with both
  Kotlin modes, all three runtimes, distribution checks and artifact uploads.
  First-connect identity implementation 7ece225 passed hosted CI 41692113541.
  CI now preserves both modes' raw XML and summaries instead of only the last mode.
  Console/evidence checkpoint 3410a54 passed CI 41693793919; its downloaded artifact
  contains both modes' raw XML. Editor/cancellation checkpoint 599c27a passed CI
  41696634679 with 72 tests per runtime in each mode.
  Final implementation 17ad87d passed CI 41699614216; downloaded artifacts confirm
  both 72-test modes on all runtimes. Its clean local ZIP also passed 432 matrix
  executions, 12 current-peer isolation checks and 72 tests on installed IDE
  262.9437.185. API/publication and live-window gates remain open.
- [x] Remove version-blind transport banners/statement warnings. README names the
  observed versions, actual upstream owners and issue links instead.

## Intentional boundaries

Background native verification is unavailable: do not execute EXPLAIN or prepare
user SQL to validate the editor. Upstream Spice/Arrow transport defects are not
plugin-owned SQL errors. A newer installed driver/server must not inherit assumed
bug warnings. Do not map GenericSQL/PostgreSQL/all Flight URLs to either product.

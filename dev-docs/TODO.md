# Readiness backlog

This is not a finished-plugin checklist with everything ticked. Automated evidence
is in [testing.md](testing.md). A working build and compatible verifier verdict do
not establish the live editor/connection experience.

## Core gates still open

- [ ] Exercise clean interactive ZIP installs on 261/262/263, including actual
  driver-process launch, secure credential handling, URL controls, both dialects,
  settings, preview/run UI and disposal/unload behavior.
  Actual archived snapshot and stable ZIPs now load through private plugin
  classloaders on all three IDEA runtimes, with exact payload hashes and stale-JAR
  removal checked. This does not close Plugins-dialog or saved-driver upgrade UI.
- [ ] Verify driver-settings upgrade behavior interactively. Connection VM fields
  contain additions, not inherited driver values. The user's saved Spice driver
  override had only the old --add-opens value; it must be corrected at driver
  level once. Preserve explicit overrides; do not edit live IDE settings on disk.
- [ ] Qualify pending metadata/prepare RPC cancellation and noncooperative cleanup
  separately from active-stream cancellation. Active streamed-row cancel and
  same-connection recovery have a bounded disposable-process lane. A driver that
  ignores cancel or blocks inside cancel/close is not covered by that result.
- [ ] Qualify broader reconnect/disposal ordering beyond the two fixed catalog
  races. Released Brikk 0.4.1 now passes both desired contracts on real helpers
  across IDEA 261/262/263 and DataGrip 261/262. No second consumer cache/generation
  manager or locally patched library was added. See stable-041-verification.md.
  A strict opt-in headless lifecycle probe passes 261 disable/re-enable but 262
  and 263 reject dynamic disable. The owner accepted this limitation and deferred
  further checking to normal use. It is not a release blocker. Preserve the
  evidence and do not advertise restart-free plugin changes.
- [ ] Qualify database-DDL rename UI and broaden quoted physical-column/host
  coverage. Indexed physical-column find usages and reference-only rename now
  keep same-named schema columns and string literals separate. Both explicitly
  injected dialects have tagged-dollar Unicode/error/completion-insertion checks.
  Those do not qualify database mutation or automatic host recognition. The stock
  PostgreSQL single-quoted host fixture retained doubled quote escapes; do not
  treat that as qualified host decoding or add automatic Kotlin recognition as a fix.

## Decisions and release gates

- [x] Owner accepted the existing API usages: 1 internal factory
  reference, 11 experimental usages and 1 deprecated model-helper override per
  audited IDE. Three experimental references are in ProductHarvest; capturing the
  Project before the EDT handoff removed four unnecessary ProtoConnection usages.
  The exact internal reference is recorded in api-usage-approvals.txt from this
  plugin's own report, not copied from a peer. Do not reopen this decision unless
  new or changed usages appear. See release-readiness.md.
- [ ] Define the advertised DataFusion endpoint/tree contract. The bounded test
  embedding has native information-schema inventories but lacks JDBC table-metadata
  RPCs. Do not promise a full database tree from those facts or invent a local
  native-model writer; a broader tree requirement needs a reviewed shared design.
- [ ] Finish real listing screenshots and final owner trademark/licensing/privacy
  review and Marketplace upload approval. The owner approved the listing/text plan
  and authorized the 0.2.0 GitHub release. The owner will supply screenshots later;
  they are not a release blocker. Marketplace upload remains separate.
  See release-readiness.md.
- [ ] Request sixth-plugin adoption in shared staging and reverse peer-isolation
  lists. Do not modify the vendored conventions to hide hardcoded shared lists.

## Optional, not silently part of v1

- [ ] Automatic Brikk Kotlin-host and SQL Transpiler .bsql integration. No Kotlin
  helper or custom host recognition is currently shipped. Ordinary explicit SQL
  injection is a separate, narrower feature.

## Owner-confirmed local dogfood

- [x] Local Spice DDL, editor references/completion, refresh and reconnect,
  confirmed on 2026-11-06. The owner subsequently confirmed saved-credential
  restoration and correct username/password transmission, without server-side
  authentication enforcement in this fixture. This does not
  qualify every IDE build, database-DDL rename UI or unload/disposal.
  See stable-041-verification.md for fixture details and the observed DDL limits.
- [x] Owner confirmed DataFusion basic execution, cold-open references, completion,
  schema selection, refresh, reconnect and PIPE had already been checked. Treat
  both products' local interactive dogfood checklists as complete, without asking
  for repeats or inventing an exact IDE build/cross-version UI matrix.

## Implemented and automatically checked

- [x] Review API ownership and risk, prepare listing/change-notes/privacy drafts
  and a manual-only pinned shared GitHub-release workflow. No release/tag
  creation, workflow dispatch or Marketplace publication.
  The owner subsequently accepted the existing API usages; the exact internal
  reference is now approved without broad package exemptions.
  See release-readiness.md for owner-only steps and outstanding decisions.
- [x] Load the exact archived snapshot ZIP then the stable ZIP in separate IDE
  JVMs on 261/262/263, reusing each runtime's disposable installation directory.
  Thirty private-classloader test executions pass, including full installed JAR
  name/hash equality and no leftover snapshot JARs after stable replacement.
- [x] Adopt public Maven Central IntelliJ common/JDBC/test support 0.4.1, keeping
  SQL core/metadata/verification API on 0.23.0. Public binaries/sources match the
  accepted snapshot. Fresh stable-default qualification passes 504 IDEA and 168
  DataGrip ordinary executions, 328 live checks including all 20 race executions,
  12 peer checks, 84 installed-IDE tests, nine Python tests and all five verifier
  targets. No candidate override or Maven Local release dependency is needed.
  Plugin tags, releases and Marketplace publication remain unauthorized.
  Hosted CI 42074662198 passed on b4734e4; downloaded evidence confirms both
  Kotlin modes and 84 tests on each IDEA runtime. Hosted verifier was skipped;
  the five full compatible verdicts come from the separate local audit.
- [x] Verify the owner-authorized upstream race-fix snapshot, including 20 real
  consumer race executions across both products and all five IDEA/DataGrip lanes.
  All ordinary, live transport/credential/console, peer-isolation and installed-IDE
  gates pass against the exact hash-checked candidate. This is not a stable release.
- [x] Qualify saved-credential integration through Database Tools and a private real
  Password Safe KeePass backend: encrypted reload, source/driver resolution,
  replacement/copy isolation, memory-only exclusion and fresh reader JVMs. Spice
  API keys use the ordinary password handshake, not saved Authorization properties.
  The OS keychain UI and connection dialog remain manual installation checks.
- [x] Add an opt-in DataFusion TLS-termination fixture without changing the pinned
  backend, including trusted/untrusted root and hostname checks. Server-side
  authorization is embedding-specific, not a universal DataFusion feature claim.
- [x] Define the v1 cache-format compatibility policy and fail-closed tests. Unknown
  library/native formats and malformed native data fall back; additive compatible
  fields reload; another identity's cache is not migrated. No old published
  consumer cache format is claimed to have a semantic migration path.
- [x] Qualify namespace behavior: USE is rejected by both pinned endpoints; Arrow
  19 setSchema is a silent no-op; explicit DataFusion schema SET is verified by
  readback/query/reset/reconnect. Full-path generated UI switch/reset stays
  unsupported rather than inventing multi-command or JDBC success. Reader and
  introspection-scope behavior remain supported.
- [x] Add a dedicated public-DSL SQL-only DataGrip consumer lane, without modifying
  vendored conventions or transplanting Java/Kotlin. Actual DataGrip 261 and 262
  execute the ordinary consumer suite with pure DataGrip runtime classpaths.
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

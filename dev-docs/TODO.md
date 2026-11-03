# Deferred work and qualification

This is the developer backlog, not a list of supported features. Keep evidence and
new findings here as local implementation proceeds. The approved project plan
remains the full implementation/release checklist.

- [ ] Qualify real Flight SQL sessions against pinned Spice 2.4.0 and a named
  DataFusion 55.2.0 embedding. Cover TLS/auth, catalogs, reconnect, parameters,
  actual utility state changes, cancellation and known upstream failures. Do not
  use the user's connections or run failure/leak probes in their IDE.
- [ ] Install the packaged ZIP interactively in clean 261/262/263 IDEs. Verify
  driver launch flags, secure credential handling, URL controls, user overrides,
  settings and both dialect selections. Headless platform tests are not this gate.
- [ ] Run isolated-classloader coexistence tests with the four sibling ZIPs and
  the remaining default Plugin Verifier targets, including DataGrip. The selected
  IU 262/263 audit is compatible, not an API exemption or publication approval.
- [ ] Review the concrete shared-library API findings: 1 internal factory
  reference in GenericJdbcIntrospectorGate, 8 experimental connection-hook usages
  in HarvestConnectionInterceptor and 1 deprecated model-helper override on each
  audited IDE. Obtain this plugin's own approval where required; do not transfer
  peer exemptions or edit the library/vendored conventions to hide findings.
- [ ] Decide whether a full native-catalog Database Tools tree is needed.
  Generic JDBC cannot populate the bounded DataFusion fixture's missing metadata
  RPCs. Keep this limitation visible; no local model-writer substitute.
- [ ] Qualify namespace switching before enabling UI-generated USE or inherited
  PostgreSQL session commands. A zero update count does not prove a switch.
  Read-only namespace readback and default introspection-scope seeding are now
  implemented; they do not certify execution of namespace-changing statements.
- [ ] Retest the reported cold/open query red highlight, empty schema selector,
  initial default scope and typing-triggered introspection in the user's IDE.
  Missing shared lifecycle/introspection hooks are now wired, with recording-model
  regression tests. Do not equate those checks with having driven the live UI.
- [ ] Optional Brikk Kotlin and SQL Transpiler host integration remains separate
  scope. No Kotlin helper, automatic host recognition or broad language claims.
- [ ] Check cache persistence and serialization across all IDE runtimes with
  released dependencies. Report library gaps upstream rather than bundling a
  second Kotlin/serialization runtime.
- [ ] Expand consumer-specific editor coverage for injected SQL, documentation
  links, navigation/rename, named-argument insertion and live/cached inventory
  changes. Registered upstream adapters are not proof of every packaged behavior.
- [ ] Add/check intention description resources. The user's IDE log reports
  missing `description.html` for `DatafusionPipePreviewIntention`; check all four
  preview/run intentions. Descriptor instantiation tests did not catch this UI
  resource lookup. Keep this separate from catalog/namespace resolution fixes.
- [ ] Complete real connection launch checks. The downloaded-driver URL/checksum
  tests and driver XML/template/VM-option loading tests do not certify a running
  IDE driver process. A standalone Java 25 JDBC read against the disposable Spice
  container now passes with the XML's exact options; TLS/auth and the actual IDE
  connection launch still need qualification.
  Both required VM flags now belong to both driver defaults, not user setup.
  Include upgrade behavior for existing customized driver VM options in the
  interactive check; preserve explicit user overrides.
  **Diagnosed user report:** on installed IDE 262.9437.185, the new-connection
  field displays connection-only additions, while launch configuration combines
  driver and connection options. The installed JAR matches the latest dogfood and
  contains both flags. The user's saved Spice driver override contains only the
  older `--add-opens` flag. Update that driver-level override through the IDE once;
  do not silently edit live settings or copy defaults into every connection.
- [ ] Prepare CI and release workflow, listing assets, trademark review and exact
  legal/API evidence before publication. No push or publication is authorized by
  the local-base task.
- [ ] Request shared tooling adoption for this sixth plugin. Existing staging
  and peer isolation lists are hardcoded; never edit vendored conventions locally.

## Known shared-library findings to recheck

- Native catalogs feed editor facts, not a Database Tools model writer.
- Catalog profiles currently have no refresh-result notification group; manual
  refresh may be silent. Do not substitute a new profile just to add balloons.
- Stock parameter prompting does not expose a qualified post-prompt NULL guard.
  No transport-warning UI is implemented. Upstream issues are version-scoped
  README notes, not plugin-owned SQL errors or unconditional upgrade warnings.

## Completed local baseline

- [x] Single-module build with unmodified shared conventions, released Central
  dependencies, legal resources, original icons and local Git dev versioning.
- [x] Both production dialect/DBMS registrations, independent catalog lifecycles,
  explicit file mappings, local diagnostics and product driver definitions.
- [x] Separate workspace PIPE settings and cooperative preview/execution adapters.
- [x] Headless 261/262/263 tests, exact distribution checks and a checksummed local
  dogfood ZIP. These are not live-transport or interactive-install qualification.
- [x] Downloaded Arrow 19.0.0 URL handling and qualified binary SHA-256 test.
- [x] Kotlin-present and Kotlin-absent runtime matrix: 35 tests per mode per IDE,
  210 total executions. This is coexistence, not custom Kotlin-host integration.
- [x] Plugin Verifier 1.411 on IU-262.10968.63 and IU-263.5701.42: compatible,
  with all API notices retained and tracked above.
- [x] Both required VM flags are automatic defaults for both drivers. Updated
  36-test suites pass on 261/262/263, including JVM option acceptance, and a real
  Java 25 Arrow JDBC read against the local Spice sample passes.
- [x] Remove the version-blind, truncated Spice transport banner and statement
  warnings. Keep version-specific upstream attribution/issue links in the README;
  a newer user-installed driver must not inherit assumed bug diagnostics.
- [x] Diagnose the blank VM-options field using installed IDE bytecode and
  selected saved-driver fields. README now explains driver-level inheritance and
  existing overrides, instead of treating a blank connection field as failure.
- [x] Wire per-product shared lazy introspection, read-only namespace readback,
  unset-scope seeding, IDE model facts and highlight invalidation. All 43 tests
  pass on the three pinned runtimes and the user's installed 262.9437.185 test
  runtime. Actual live-window symptom confirmation remains open above.

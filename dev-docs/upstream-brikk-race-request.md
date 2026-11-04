# Shared catalog ordering fix request

Affected released artifact: `dev.brikk.house:brikk-sql-intellij-support:0.4.0`.
Owner: Brikk's `LiveCatalogService` recording and invalidation logic. This is not
an Arrow JDBC or Spice server defect.

## Desired contract

1. `forget(dataSourceId)` invalidates results already in flight for that source.
   Completing one must not recreate memory or disk inventory after the forget.
2. Once a newer same-identity helper refresh has recorded a complete inventory,
   a result captured by an older refresh must not replace it.
3. Existing connection-identity and console-reconnect checks remain in force.
4. Failed, partial, timed-out and cancelled refreshes must preserve the last valid
   accepted snapshot. A stale result must not delete another identity's files.
5. Debounced refreshes and session/helper paths need the same invalidation policy,
   without serializing unrelated sources or performing SQL from editor hooks.

The shared service should capture an invalidation epoch before opening a helper
and compare it before recording. Refresh ordering also needs a captured generation
or equivalent token. The policy for a later failed request must be explicit;
latest accepted successful inventory must never be overwritten by older work.
Those tokens must be part of the shared service, not a second consumer cache.

## Reproducers

`SharedFlightCatalogRaceTest` uses SDK-opened real helpers and the released native
harvesters. The first result is held after all metadata has been collected, so a
subsequent network disconnect cannot erase the captured result. Deterministic
revision markers distinguish old and new captures; they are not fake server
version reports.

Both desired-contract tests currently fail for both Spice 2.4.0 and the pinned
DataFusion 55.2.0 fixture. Run with the explicit `--shared-lifecycle-probe` flag
documented in [engineering-qualification.md](engineering-qualification.md).
These failures stay separate from passing smoke/ordinary test totals.

## Consumption gate

Fix and review this upstream, publish an authorized release, then update the root
catalog override and rerun the failing reproducers on every consumer runtime.
Do not edit vendored build logic, copy peer exemptions, relabel a Maven Local
candidate as a stable release, or add a consumer generation manager to make tests
appear green. This repository has not modified the shared source or published a
dependency release.

The owner has now explicitly requested the existing Brikk OpenChamber agent to
prepare the upstream fix and publish a local SNAPSHOT candidate. Candidate-only
verification may consume those published local coordinates, with hashes and
provenance recorded. Released 0.4.0 must not be overwritten, the ordinary released
dependency default remains distinct, and public publication still needs separate
authorization. The final dogfood build follows successful consumer verification.

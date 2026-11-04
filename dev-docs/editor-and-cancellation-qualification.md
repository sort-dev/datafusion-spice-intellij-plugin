# Editor and cancellation qualification

The follow-up checks use the loaded consumer plugin, platform PSI/model objects
and the released harvester. They are narrower than interactive database DDL,
automatic host recognition or arbitrary server cancellation.

## Physical model references

The fixture loads two schemas with same-named tables and columns, selects both
schemas in the data source's introspection scope, and attaches a console namespace.
`ReferencesSearch` must find only the selected physical column's three uses, not
the other schema's column or a matching string literal. Reference-level rename
edits those three SQL identifiers and leaves the physical model unchanged.

This is the platform's PSI reference edit, not its database rename/DDL dialog.
The latter remains unqualified. An unselected schema is not an adequate fixture
for proving native physical-symbol find usages.

## Explicit injection

Both dialects are explicitly injected into tagged PostgreSQL dollar strings.
The tests keep non-BMP Unicode before the function reference, verify mapped host
offsets and retain local parse errors inside the injected fragment. Completion
must run against the actual injected editor window and change only its query.
The tests do not add Kotlin recognition or claim that single-quoted PostgreSQL
host escape decoding has been fixed.

## Consumer cancellation

The cancellation fixture invokes each actual product connection hook and released
native harvester over a held synthetic remote metadata statement. Interrupting its
worker is insufficient; cancellation must call the statement's cancel operation.
The test then requires statement closure, completion within its observation bound,
no namespace readback, no scope seeding and no stored inventory.

This qualifies caller cancellation against a cooperative recorded boundary. It
does not establish that Arrow/server cancellation, namespace probes, remote RPC
cleanup or a driver ignoring cancel has a bounded production latency. Those gates
remain open. No user SQL is executed by this fixture.

## DataGrip runtime probe

A direct headless DataGrip 262 runtime attempt stopped before executing tests.
The shared convention requests the Java/Kotlin host bundled plugins even when
Kotlin tests are absent, and DataGrip 2026.2.5 lacks `com.intellij.java`.
The failure is recorded in `build/nap/datagrip-probe.log`. No DataGrip test pass
is claimed from this attempt. The five-target Plugin Verifier result is separate.
Do not patch the vendored conventions or load an arbitrary IDEA Java plugin into
DataGrip to turn this qualification gap into a nominal pass.

This old lane failure is superseded by the dedicated public-DSL SQL-only lane in
[engineering qualification](engineering-qualification.md#datagrip-consumer-lane).
It executes the expanded suite on actual DataGrip 261 and 262 without Java/Kotlin
transplants or a mixed IDEA runtime classpath. The earlier failed log remains
historical evidence, not the current qualification verdict.

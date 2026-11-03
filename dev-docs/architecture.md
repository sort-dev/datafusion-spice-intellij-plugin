# Consumer wiring

The plugin is one Gradle module with two dialect singletons and DBMS objects.
`dev.sort.datafusion-spice-intellij-plugin` is the sole plugin ID.

| Product | Language | DBMS | Shared catalog |
| --- | --- | --- | --- |
| DataFusion | DataFusionSQL | DATAFUSION_BRIKK | DatafusionCatalogProfile |
| Spice | SpiceSQL | SPICE_BRIKK | SpiceCatalogProfile |

`META-INF/plugin.xml` registers editor/DBMS/catalog hooks and includes
`datafusion-spice-pipes.xml` for cooperative Execute, settings and PIPE actions.
The plugin registers no new SQL file type and no GenericSQL/PostgreSQL aliases.
Two product-specific driver templates share the owned Arrow Flight SQL JDBC 19.0.0
artifact. A common Flight URL never selects a product on its own.

Both templates set the complete Arrow driver VM options through the existing
`<vm-options>` element: `--add-opens=java.base/java.nio=ALL-UNNAMED
--sun-misc-unsafe-memory-access=allow`. These target the bundled IDE driver runtime,
not the Java 21 Gradle build process. Preserve explicit user driver customizations;
do not mutate global IDE settings or saved VM overrides behind the user's back.
An explicitly selected older driver JVM is a custom configuration; Java 21 does
not accept the Java 25 unsafe-memory option. This does not change the Java 21
build/bytecode target or make manual setup necessary for the normal bundled runtime.

Editor adapters are no-argument subclasses of released Brikk support. Each stable
`ConnectedDatafusionEditorProfile` reads copied facts from its own application
catalog service. Connection harvesting, cleanup and project execution observers
use that same catalog's stable resolver. The library owns cache/session lifecycle;
production persistence is not disabled as it is in the upstream test fixture.
`DasRelationFacts` is now the first additional provider, so the already-introspected
IDE model supplies analysis/navigation facts before the session live-catalog
fallback becomes available.

PG-family models and generic JDBC introspector gates follow Trino/DuckDB. They are
implementation reuse, not PostgreSQL wire support. Generic JDBC helpers/source
loading and disabled graphical Explain avoid inherited PG behavior. Namespace
readback uses the shared `CatalogSchemaSearchPath` over known
`information_schema.df_settings` catalog/schema values. New empty introspection
scopes are seeded with those names, using the same public scope/storage APIs as
Doris; explicit selections and helper-connection isolation are preserved. Namespace
switching still returns no operation because SQL USE transport is not qualified.
The live native catalog supplies editor facts; it does not fill a missing JDBC
database tree.

Trino/DuckDB-style `AutoIntrospectService` subclasses now own per-product targeted
introspection. Both qualified-path completion and the reference annotator use the
shared service, including cold/opened queries and unqualified names in the current
console namespace. The shared narrow out-of-scope filter applies only while a
known namespace is not loaded; missing objects in loaded namespaces remain errors.
No unconditional editor banner is added.

ClickHouse's `VerificationConnectionListener` pattern restarts highlighting on
owned user connection changes, ignoring Brikk helper connections. Successful
manual refreshes also restart analysis. Like DuckDB's execution observer callback,
the product observer detects the first session/catalog association and changed
inventory identities and schedules reanalysis, without modifying document text
or submitting user SQL for validation.

Both diagnostic adapters share a stable product-specific native error profile.
It reports local syntax problems and never uses EXPLAIN or statement preparation
for background validation. There is no second blanket PSI error filter.

Each PIPE setting uses shared workspace `ToggleSettings`; the same pipe profile
feeds diagnostics, stages, preview, run-to-stage and Execute interception. One
cooperative configuration tries the product-owned interceptors. Only DECLINED
delegates to the predecessor action. Native SQL and foreign sessions remain stock.

Upstream transport bugs are documented in the README with their owners, observed
versions and issue links. They do not produce a plugin editor banner or statement
warnings. The removed implementation did not check the installed driver/server
version and would have warned even after an upgrade; it also produced truncated
banner text. Do not reintroduce version-blind warnings or interpret SQL syntax as
evidence that the installed transport still has a bug. Local syntax/semantic
diagnostics remain independent. Prompted parameter values are not read or persisted.

## Peer trace

- All four peers: wrapper, included `build-logic`, version catalog, thin shared
  adapters, service scopes, legal packaging, local developer ZIP conventions.
- Trino/DuckDB: PG-family model plus generic introspector gate, owned downloadable
  driver artifact, PIPE descriptor include and paired catalog cleanup listeners.
- ClickHouse: explicit completion ordering, grouped adapters, workspace toggles,
  cooperative execution pattern and background-error test capture.
- Trino: disabled inherited graphical Explain and user-authored console EXPLAIN.
- DataFusion/Spice handoff: exact two identities and the full editor/catalog/PIPE
  registration set. No fixture installer or source dependency is used by the plugin.

The shared conventions are vendored at
`a30cc5588f9ba1e40dcaf3e39e7735849f5fe2d2`. SQL 0.23.0/support 0.4.0 use per-repo
version overrides and Maven Central, not Maven Local. Original plugin artwork
avoids claiming rights to product logos.

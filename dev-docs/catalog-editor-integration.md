# Catalog/editor integration follow-up

The user reported unresolved references on opening a console until editing its
text, an empty schema selector, no useful initial schema selection, and no lazy
introspection when typing a qualified namespace.

## Missing consumer wiring

- The base database dialect deliberately returned no namespace readback. It now
  uses the existing shared `CatalogSchemaSearchPath` with a read-only query of
  `information_schema.df_settings` for the actual default catalog and schema.
  No hardcoded `spice.public` is applied to DataFusion or custom endpoints.
- The base had live catalog harvesting but no `AutoIntrospectService`, qualified
  path contributor, reference trigger or out-of-scope filter. These now follow
  Trino/DuckDB's shared-kit wiring with stable, product-specific resolvers.
- The editor profile had only the native inventory provider. `DasRelationFacts`
  now precedes it, as allowed by the consumer handoff, so the IDE model's already
  loaded tables can participate without waiting for an executed request to link
  the console to its session inventory.
- There was no connection-driven highlight restart. A product-scoped subclass of
  shared `VerificationConnectionListener` follows ClickHouse's pattern. Successful
  manual refresh and first/changed execution-session inventory association also
  schedule reanalysis. They do not modify the document or execute validation SQL.

## Initial scope and namespace policy

After a non-helper product connection, read its metadata namespace and select it
only if the data source's introspection scope is unset. Use `CatalogScopes.select`
and the owning `DataSourceStorage.updateDataSource`, following Doris's explicit
scope normalization pattern. Keep explicit existing selections unchanged. No
unrelated data source or global driver VM setting is altered.

The metadata readback query is a product-specific addition over the same
information-schema settings required by the shared native harvester. It uses no
user SQL, preparation-based validator or EXPLAIN. A missing/failed namespace read
stays unknown. This work does not enable SQL USE execution or fabricate success
from a zero JDBC update count; namespace-changing operations remain separately
tracked.

## Regression boundaries

`CatalogIntegrationTest` uses shared `HeldIntrospections` and `RecordingConsole`
with offline source URLs. It checks both products' scopes/resolvers, records the
namespace query at a fake metadata boundary, and models catalog/schema loading
without a real JDBC connection. Opening qualified and unqualified references and
typing a qualified path must queue targeted introspection. Completing that sync
must leave the document stamp unchanged and resolve the loaded relation. These
are actual platform PSI/model/completion paths, not an interactive run in the
user's IDE or proof of a live endpoint's JDBC metadata implementation.

The user's real IDE window, current data-source scope, schema dropdown and cold
highlighting still need a retest after installing the rebuilt ZIP and reconnecting.
Do not claim those UI observations have been verified by the automated fixtures.

## Separate observed findings

The existing IDE log contains prior execution errors for an uppercase
`spice.public.SAMPLE`, plus a missing PIPE intention-description resource. Do not
hide real server errors or assume they explain every red highlight. The intention
resource problem is deferred in TODO.md. If a red reference remains after model
sync, collect its exact tooltip to distinguish an unresolved namespace, local
syntax diagnostic and an old execution error.

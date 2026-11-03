# DataFusion and Spice SQL for JetBrains IDEs

One plugin with separate Apache DataFusion and Spice SQL dialects for DataGrip and
IntelliJ IDEA with Database Tools, on IDE builds 261 through 263.

The plugin provides product-specific SQL parsing, highlighting, function completion,
documentation and local diagnostics. Select **DataFusion (sort.dev)** or
**Spice (sort.dev)** for SQL files. Product-specific data sources select the matching
dialect for their consoles.

This is an independent plugin by Sortdev SRL, not an official Apache DataFusion,
Apache Software Foundation or Spice.ai product. Product names identify the engines
the plugin supports.

## Connections

The DataFusion and Spice data-source templates use Apache Arrow Flight SQL JDBC
19.0.0. Connection credentials and TLS settings belong to the data source.
DataFusion is an embedding library, not a universal server: a deployment must expose
the Flight SQL and information-schema contract used by this plugin.

Driver JVM options include `--add-opens=java.base/java.nio=ALL-UNNAMED`.
For a Java 25 driver runtime, also add
`--sun-misc-unsafe-memory-access=allow` in the driver's VM options. Do not change
the IDE's global VM options.

## Limits

SQL checks are local. The plugin does not prepare statements, run EXPLAIN or execute
your queries in the background to validate them.

Spice's qualified Flight SQL transport has known limitations with SQL
PREPARE/EXECUTE/DEALLOCATE, SET, no-result updates and NULL JDBC parameters.
A returned update count of zero does not prove a statement ran. Failed JDBC
parameter binding can leak driver-process memory. Native SQL execution follows
Database Tools; editor support does not guarantee transport execution support.

Bundled metadata covers DataFusion 55.2.0 and Spice 2.4.0. Other versions receive
labeled fallback metadata, not an exact-version support claim. Live inventories
keep the two products and their versions separate. Database-tree coverage depends
on the endpoint's JDBC metadata support.

Local development builds are not a published or fully qualified production release.

# DataFusion and Spice SQL for DataGrip and IntelliJ IDEA

Add separate Apache DataFusion and Spice SQL dialects to DataGrip and IntelliJ IDEA
with Database Tools. Brikk's product-specific parsers provide local syntax checks,
alongside highlighting, function completion, documentation and reference navigation.
These are independent SQL dialect implementations, not the database engines' own
parsers. Compatible with IDE builds 261 through 263.

Bundled metadata covers **DataFusion 55.2.0** and **Spice 2.4.0**. Flight SQL endpoint
verification uses the same versions. Other versions receive labeled fallback metadata,
not an exact-version support claim.

Optional Brikk PIPE support lowers supported pipelines into SQL before execution.
Local checks reject unsupported lowering and remaining PIPE syntax, but native server
verification is unavailable. See [Brikk PIPE](#brikk-pipe).

Select **DataFusion (sort.dev)** or **Spice (sort.dev)** for SQL files. Product-specific
data sources select the matching dialect for their consoles.

This is an independent plugin by Sortdev SRL, not an official Apache DataFusion,
Apache Software Foundation or Spice.ai product. Product names identify the engines
the plugin supports.

## Connections

The DataFusion and Spice data-source templates use Apache Arrow Flight SQL JDBC
19.0.0. Connection credentials and TLS settings belong to the data source.
DataFusion is an embedding library, not a universal server: a deployment must expose
the Flight SQL and information-schema contract used by this plugin.

For a Spice endpoint with API-key authentication, use the IDE's username/password
authentication fields. Supply a nonempty username, such as `spice`, and put the
API key in the **Password** field; Spice uses the password as the key and ignores
the username. Let Database Tools/Password Safe remember it if desired. Do not put
the key in the JDBC URL or an ordinary advanced driver property. This route is
verified with Spice 2.4.0 and Arrow JDBC 19.0.0. Keep TLS certificate verification
enabled for authenticated remote connections.

After connecting, the plugin reads the current catalog and schema. A new data
source's unset introspection scope selects that reported default namespace;
existing schema selections are preserved. Completion and SQL references can
request unloaded namespaces as you type or open a query. Catalog changes restart
editor analysis without requiring a text edit. Live inventories keep the two
products and their versions separate. Database-tree coverage depends on the
endpoint's JDBC metadata support.

### Current namespace

Reading the server's current catalog/schema and selecting schemas to introspect
are supported. The plugin does not generate console namespace-switch/reset SQL.
`USE` is rejected by the tested DataFusion 55.2.0 embedding and Spice 2.4.0, and
Arrow JDBC 19.0.0's `setSchema` returns without changing the server namespace.

On the tested DataFusion embedding, an explicitly executed command such as
`SET datafusion.catalog.default_schema = 'analytics'` changes that connection's
default schema. The target schema must exist. This does not promise equivalent
support in every embedding; Spice 2.4.0 rejects this `SET` operation. Merely
changing IDE introspection selections does not execute it.

Both driver templates define the required VM options. In **Data Sources and
Drivers**, select the **Drivers** tab, then **Spice (sort.dev)** or
**DataFusion (sort.dev)**, then **Advanced**. For a Java 25 driver runtime, the
driver's **VM options** should contain:

```text
--add-opens=java.base/java.nio=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow
```

The **connection's** Advanced VM options field may be blank: it shows extra
connection-specific options, not the inherited driver options. Database Tools
combines both when launching the driver, so you do not need to copy the flags
into every connection.

If you customized the driver before upgrading, the IDE preserves that saved
override. If the **driver-level** value is blank or incomplete, replace it with
the full value above once; all connections using that driver then inherit it.
These are database driver options, not project or global IDE VM options.
If you deliberately use a Java 21 driver runtime, use only the `--add-opens` option.

## Limits

SQL checks are local. The plugin does not prepare statements, run EXPLAIN or execute
your queries in the background to validate them.

### Upstream issues observed with Spice 2.4.0 and Arrow JDBC 19.0.0

These are upstream server/driver issues, not errors reported by this plugin:

- **Spice server:** some errors and SQL PREPARE/EXECUTE/DEALLOCATE operations can
  return an empty schema or a misleading zero update count. See
  [#15944](https://github.com/spiceai/spiceai/issues/15944),
  [#15946](https://github.com/spiceai/spiceai/issues/15946) and
  [#15948](https://github.com/spiceai/spiceai/issues/15948).
- **Spice server:** incorrect parameter nullability can make NULL JDBC bindings
  fail. See [#15950](https://github.com/spiceai/spiceai/issues/15950).
- **Apache Arrow JDBC driver:** failed parameter binding can leak driver-process
  memory. See [#1439](https://github.com/apache/arrow-java/issues/1439).

These observations concern those versions, not every later release. The plugin
does not display an editor banner or mark SQL as faulty because of these issues,
and it does not determine whether a user-installed driver/server contains a fix.
Database Tools reports actual execution results and driver/server errors.

## A small Spice query

This uses JSON functions without requiring a model or an external dataset:

```sql
SELECT
    json_get_str('{"name":"Ada","age":30}', 'name') AS name,
    json_get_int('{"name":"Ada","age":30}', 'age') AS age;
```

Expected result: `Ada | 30`.

## Brikk PIPE

Enable either product independently under **Settings > Tools > Database >
DataFusion Brikk PIPE** or **Spice Brikk PIPE**. Both are off by default. PIPE support
is included in this plugin; a separate SQL Transpiler installation is not required.

Before execution, Brikk lowers supported stages into ordinary SQL for the same selected
product: DataFusion PIPE becomes DataFusion SQL, and Spice PIPE becomes Spice SQL. This
is not cross-dialect conversion or native pipe-syntax support. Only the generated SQL is
sent to the server; your PIPE document is not rewritten.

Local checks reject unsupported lowering and any remaining PIPE syntax. The generated SQL
is not validated by DataFusion or Spice itself before submission, so these checks do not
guarantee server acceptance. Transport limitations still apply.

The editor popup offers **Preview Generated SQL** and **Run Stages up to Caret**. Preview
does not execute SQL. Run-to-stage submits only the top-level pipeline prefix through the
stage at your cursor.

## Related plugins and tools

- [Apache Doris SQL](https://plugins.jetbrains.com/plugin/32777-sql-dialect-for-apache-doris)
  dialect for JetBrains IDEs.
- [ClickHouse SQL](https://github.com/sort-dev/clickhouse-intellij-plugin) dialect for JetBrains
  IDEs.
- [DuckDB SQL](https://plugins.jetbrains.com/plugin/33098-sql-dialect-for-duckdb-embedded--quack-)
  dialect for JetBrains IDEs, for embedded DuckDB and Quack servers.
- [Trino SQL](https://plugins.jetbrains.com/plugin/33124-sql-dialect-for-trino) dialect for
  JetBrains IDEs.
- [SQL Transpiler](https://plugins.jetbrains.com/plugin/32900-sql-transpiler) converts SQL between
  supported database dialects in JetBrains IDEs.

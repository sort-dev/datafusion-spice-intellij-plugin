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
editor analysis without requiring a text edit.

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

Bundled metadata covers DataFusion 55.2.0 and Spice 2.4.0. Other versions receive
labeled fallback metadata, not an exact-version support claim. Live inventories
keep the two products and their versions separate. Database-tree coverage depends
on the endpoint's JDBC metadata support.

Local development builds are not a published or fully qualified production release.

## Brikk PIPE

Enable either product independently under **Settings > Tools > Database >
DataFusion Brikk PIPE** or **Spice Brikk PIPE**. Both are off by default.

The editor popup offers generated-SQL preview and run-to-stage. Brikk PIPE lowers
supported stages to SQL for the selected product; it is not a promise of native
pipe-syntax support. Preview states that native server verification is unavailable.
Transport limitations still apply to the generated SQL.

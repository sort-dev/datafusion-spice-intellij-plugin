# Privacy notice

SQL Dialects for DataFusion & Spice runs inside the JetBrains IDE. Editor SQL
diagnostics are local; the plugin does not execute, prepare or EXPLAIN your SQL
in the background to validate it.

Database connections, catalog introspection and refresh contact the database
endpoint you configure. Explicit SQL execution and opted-in PIPE execution send
SQL to that endpoint through Database Tools and the selected JDBC driver. The
plugin does not require a separate Sortdev or Brikk service account.

Live catalog metadata is cached locally using the IDE's storage directories.
It can include database object names and definitions. Session-only catalog facts
are not written into the shared persistent inventory. Removing a data source
invalidates its plugin inventory.

Connection credentials are handled through JetBrains Database Tools and Password
Safe according to your IDE's credential-storage settings. Do not put secrets in
JDBC URLs or ordinary saved Authorization properties. For Spice API-key endpoints,
use the Password field as described in the README.

The consumer plugin has no dedicated analytics or error-upload endpoint. The
IDE, installed JDBC driver, configured database services and any external
documentation pages you open have their own networking and privacy behavior.
Database Tools may download the JDBC driver when you request it.

For support, use the project's GitHub issues or contact jayson@sort.dev. GitHub
issues are public. Remove passwords, API keys, private SQL, data and identifying
connection details before sharing screenshots or logs.

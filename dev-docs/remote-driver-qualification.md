# Remote Flight driver qualification

The opt-in fixture runner can now exercise the IDE's real remote JDBC process,
not just JDBC inside the test JVM:

```sh
python3 tools/run-flight-smoke.py --ide-process --java-home /path/to/jdk21
python3 tools/run-flight-smoke.py --product spice --spice-secure --ide-process --test-task test262 --java-home /path/to/jdk21
```

The test copies the registered product driver into a disposable driver definition,
keeps its dialect, forced DBMS and VM defaults, replaces the download reference
with the same resolved Arrow 19 JAR, and removes the copy during cleanup. It uses
runner-owned loopback endpoints and temporary sandbox data sources. User drivers,
saved connections and the live IDE are not modified.

## First-connection identity defect

The first remote-process run exposed a cold-start defect that direct JDBC and
recording-boundary tests did not reveal. Both a first manual helper refresh and a
normal first connection learned the server version during connection setup.
The released DataFusion profile includes `dataSource.databaseVersion.version` in
its identity, so learning `2.4.0` changed the identity from the pre-connect value.
The helper returned `Superseded`; selection after a normal connect had no usable
inventory. An INFO log confirmed that the harvest was discarded as if the user
had edited the endpoint.

`ProductCatalogProfile` still uses the released DataFusion/Spice harvester,
resolver, metadata, merge policy and lifecycle service. Its only override is the
consumer identity policy. That policy hashes the product, exact DBMS, driver ID
and class, URL, user, embedding profile and sorted effective connection/driver
properties. The automatically observed server version is not a settings edit.
Actual versions remain in harvested inventory, and every console connection still
uses the released every-connect policy.

This changes cache identities. Old identity-keyed snapshots are not relabeled or
migrated into the new identity. A successful connect or manual refresh repopulates
them; this is not a cross-library cache-format migration guarantee.

The new live regression starts with an unknown version and requires a populated
inventory after the first connect. A first helper refresh must succeed without
running even an invalid user startup script. Standard identity tests separately
require URL/user/driver/property edits to change identity and property ordering
to leave it unchanged. No SQL engine code or vendored library was copied.

## Boundaries

The next qualification step adds an actual `JdbcConsole`. Its ordinary SELECT,
opted-in PIPE Execute, registered manual Refresh and healthy subsequent request
pass on all three runtimes, for both plaintext products and secure Spice. The
observer correlates the console inventory after its first request. The test checks
the SQL submitted to the real server and keeps the user's PIPE document unchanged.

These are automated sandbox operations, not interactive installation, the user's
window or production credential-store qualification. Spice fixture authorization
is a synthetic advanced driver header. It is not a claim that API keys belong in
saved plain-text properties. Broader console UI behavior, tree/model introspection
and arbitrary server cancellation need their own evidence. The subsequent
[out-of-band refresh checks](out-of-band-refresh.md) cover a pod reload and a
same-session registry mutation, not every possible external change.
Exact lanes and results are recorded in testing.md.

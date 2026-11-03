# Out-of-band catalog refresh checks

These opt-in tests use the runner's own disposable endpoints and real IDE
consoles. They do not mutate a saved user data source or infer success from a JDBC
update count.

## Mutations outside observed execution

- Spice: the test adds a simple view to the runner-owned Spicepod. The fixture
  explicitly enables `--pods-watcher-enabled`; direct `spiced` invocation does
  not enable the watcher by default. The pod path must resolve under this build's
  readiness directory, have the expected fixture name, and contain the fixture
  manifest marker. The original contents are restored in `finally`.
- DataFusion: a test-only raw request creates a view over the existing console
  connection without supplying an observed SQL statement to the execution
  observer. This models an embedding changing that session's registry outside
  editor execution. It does not claim that a different Flight connection shares
  session-only objects.

In each case the same connection must first query the new view and read `43`.
That proves the mutation happened. Until manual Refresh, the selected inventory
and native editor-facts revision must still be the old cached ones.

The test then invokes the registered Refresh action. The new inventory must
contain the view, the cache-only editor provider must publish new relation facts
with a later revision, and the document modification stamp must stay unchanged.
The real console then queries the new view and executes another ordinary query.
Only the manual refresh produces a notification.

## Credentials and boundaries

The remote DataFusion fixture now supplies its synthetic password through
Database Tools' `DatabaseCredentials` API with `Storage.MEMORY`, not a password
parameter in the saved URL. Real remote connection and console execution exercise
that path. This is not qualification of an OS keychain, persistent Password Safe,
arbitrary production authentication or the connection dialog.

Spice's synthetic API key remains a temporary test-only RPC header. Secure fixture
files are private and removed by the runner. No production credential-storage
recommendation is implied by that test setup.

The default runner remains opt-in. Ordinary build/test runs exclude all live
classes, and no SQL is run merely to validate editor text. Exact runtime results
belong in testing.md.

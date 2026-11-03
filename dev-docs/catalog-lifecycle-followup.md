# Catalog lifecycle follow-up

This pass addresses manual-refresh feedback and connection/cache edge cases. It
does not replace live-window dogfooding, TLS/auth qualification or namespace USE
execution checks.

## Manual refresh feedback

The released DataFusion/Spice profiles have no notificationGroupId. The services
now use their existing `onRefreshed` consumer hook plus shared `notifyEscaped`,
following the peer notification pattern, to report an explicit Refresh action.
Success reports observed function/relation counts. Connection, incomplete-harvest,
settings-changed and transaction outcomes get short, distinct messages.

There is no new editor-notification provider or permanent warning banner.
Connect/execution background events stay quiet. The fallback does not duplicate
a shared profile's own notification channel. Raw driver exceptions are not
included because their messages can contain URLs or credentials; user-supplied
source names are HTML-escaped by the existing helper.

## Identity pinning before harvesting

The old scope initializer pinned identity after `super.handleConnected` returned.
The new regression changed the source URL during the shared harvest and initially
failed: the native inventory was correctly rejected by the library, but consumer
code still read the old connection's namespace and initialized the edited source's
scope.

Identity is now captured before the harvest and checked again before namespace
readback and on the EDT before applying the initial scope. Product ownership is
also rechecked. An existing explicit scope is preserved. Recording-boundary tests
invoke the actual consumer callback and released harvester for both products,
including identity changes in both phases, a driver switch, foreign connections,
helper isolation and failed-identity cache fallback.

## Cache process boundaries

`CatalogCacheProcessProbe` is a test-only entry point. A writer and a separate
reader JVM use the released LiveCatalogStore and the current IDE runtime/classpath.
The reader must see disk provenance, the original native inventory and no
session-only function, version or namespace. The parent also checks persisted
files for those private markers.

The IDE classpath is too long for a JVM system-property argument on Linux. Gradle
therefore writes it to an ignored file and passes only its path. Child JVMs use
standard Java argument files. No build convention was changed and neither helper
nor test-support classes are bundled in the plugin.

This proves process-separated use of the current format when the checks pass.
Do not call it an arbitrary library-version migration test or a check of the
user's live cache. Exact results belong in testing.md.

## Remaining lifecycle questions

- Qualify same-identity concurrent helper refresh ordering, removal/forget during
  an in-flight harvest, and reconnect/disposal through a real IDE driver process.
  The published service's recording/cancellation internals are private; do not
  add a second consumer cache/connection-generation manager as a substitute.
- Verify the manual-action notifications and schema/editor state in a live
  window. Automated message-bus and recording-metadata checks are narrower.

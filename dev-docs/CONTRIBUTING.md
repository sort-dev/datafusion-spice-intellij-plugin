# Development

The build follows the sibling sort-dev SQL plugins: Gradle 9.0.0, vendored
`dev.sort.sql-dialect-plugin` conventions, Kotlin 2.4.10 and Java 25 bytecode.
SQL 1.0.1 and IntelliJ support 1.0.1 resolve from Maven Central.
`mise.toml` pins Java 25.0.2 as Trino does; IDE lines 2026.2 and 2026.3 require
Java 25, and 2026.1 (261) is no longer supported. Use that JDK as JAVA_HOME;
with mise, trust this local project configuration and use `mise exec -- ./gradlew`.

```sh
./gradlew test buildPlugin verifyDistribution
./gradlew test262 test263
./gradlew runIde
```

Development ZIPs live in `build/distributions/` and carry the shared Git-derived
version. Keep that filename unchanged. Install locally through Plugins > Install
Plugin from Disk. No build or test command publishes anything.

JetBrains sorts a `-dev` build after the same bare base version. A future owner-
approved release must account for that ordering, or users must remove/downgrade a
same-base development build manually. Never rename this ZIP into a release.

Do not modify `build-logic/`; its provenance is checked against the shared
conventions commit. Per-repository version changes belong in settings.

See [TODO.md](TODO.md) for deferred qualification and [testing.md](testing.md) for
the verification record. [architecture.md](architecture.md) records consumer wiring
and [artifact-provenance.md](artifact-provenance.md) records resolved JARs.
Keep the README user-facing. Move superseded developer
records into `dev-docs/archive/` only when they actually become obsolete.

GitHub pushes, releases, Marketplace uploads and internal-API approvals require
separate owner authorization. A local successful build is not publication approval.

See [release-readiness.md](release-readiness.md) for the exact API review,
installed-ZIP qualification lane, listing drafts and owner-only release procedure.
The prepared shared release workflow is manual-only and must not be dispatched
without separate authorization. Marketplace publication is not part of it.

## Optional disposable Flight checks

`tools/run-flight-smoke.py` starts and cleans up its own Spice 2.4.0 container and
bounded DataFusion execution fixture. It does not connect to saved IDE sources or
the user's dogfood container. Docker must be available. Build the shared library's
qualified test embedding into this repo's ignored output directory first:

```sh
cargo build --locked --manifest-path /path/to/brikk-sql/tools/datafusion-reference/flight/Cargo.toml \
  --bin brikk-datafusion-flight-execution --target-dir "$PWD/build/datafusion-reference" -j 2
python3 tools/run-flight-smoke.py --java-home "$JAVA_HOME"
```

Use JDK 25 for the Gradle process. The actual tests use the IDE's bundled runtime
and the driver JVM flags. Run `--product spice` or `--product datafusion` to select
one fixture. Results/status are under `build/readiness/`. These loopback checks
are not production TLS/authentication or interactive IDE qualification.

For execution issues, record the installed driver/server versions and actual error
before assigning ownership. Known upstream bugs belong in version-scoped README
notes with issue links, not unconditional editor banners or plugin SQL warnings.

For the cold-highlight/schema follow-up, see
[catalog-editor-integration.md](catalog-editor-integration.md). Its automated
checks use offline recording boundaries. Keep those results separate from
interactive confirmation in the user's running IDE.

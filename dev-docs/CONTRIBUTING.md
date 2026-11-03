# Development

The build follows the sibling sort-dev SQL plugins: Gradle 9.0.0, vendored
`dev.sort.sql-dialect-plugin` conventions, Kotlin 2.4.10 and Java 21 bytecode.
SQL 0.23.0 and IntelliJ support 0.4.0 resolve from Maven Central.
`mise.toml` pins Java 21.0.2 as DuckDB and Doris do. Use that JDK as JAVA_HOME;
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

For execution issues, record the installed driver/server versions and actual error
before assigning ownership. Known upstream bugs belong in version-scoped README
notes with issue links, not unconditional editor banners or plugin SQL warnings.

For the cold-highlight/schema follow-up, see
[catalog-editor-integration.md](catalog-editor-integration.md). Its automated
checks use offline recording boundaries. Keep those results separate from
interactive confirmation in the user's running IDE.

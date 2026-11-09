# Local dependency provenance

The build resolves released Brikk artifacts from Maven Central through the
unmodified shared convention. It has no source-project or Maven Local release
override and no snapshot dependency by default. The guarded opt-in snapshot
qualification mechanism is documented in candidate-verification.md.

Released runtime payloads, updated for Brikk SQL and IntelliJ support 1.0.1:

| File | SHA-256 |
| --- | --- |
| brikk-sql-jvmMain-1.0.1.jar | ee7bbd32701cb305adf382b3c6574d57eef97ff065891b3bcf29f272e41ff33b |
| brikk-sql-metadata-jvmMain-1.0.1.jar | 93970064c34628c2bfccb7d4ef0ee3856a05162192bdec8f55c68376552b7169 |
| brikk-sql-verify-api-1.0.1.jar | 80688aca1fd5532098bd4580099995a8cb0b9042450901f62e524a424f6d7cf9 |
| brikk-sql-intellij-support-1.0.1.jar | 1d032242b0b31a8b9e42fa01134c20c69835ac9b9247e59b3b0c1129ae715bd0 |
| brikk-sql-intellij-jdbc-support-1.0.1.jar | 19bacdaa249fa9408fb7aef5f09df7f80800ab2c1beeddc7565ebc27d259dd05 |

The verification API and JDBC support JARs are byte-identical to the 0.23.0 and
0.4.1 payloads they replace. The five packaged JARs match the SHA-1 sidecars
published on Maven Central. The 0.4.1-era hashes are in stable-041-verification.md.

Gradle Module Metadata selects the `jvmMain` classifier for core and metadata,
as in the sibling builds. The plain Maven artifact filenames inspected during
planning were not the final Gradle runtime filenames. `embeddedLibraries` now
checks the actual classifier names, without widening the allowed dependency set.

`verifyDistribution` checks exact embedded libraries, duplicates, bytecode levels,
legal resources, required notices and descriptor/version consistency. The main
plugin JAR packages LICENSE, NOTICE and THIRD_PARTY_NOTICES.md. The ZIP contains
only the main JAR and the five Brikk runtime JARs above. It contains neither the
test-support JAR nor Arrow JDBC, Kotlin stdlib or serialization runtime copies.

Arrow Flight SQL JDBC 19.0.0 is a separate IDE-managed single-JAR download. Its
qualified SHA-256 is
`d3beee43c613c457789825343368f652d570d76c08799dad38a43a10e569b57f`.
The same non-transitive artifact is test-only in Gradle; tests check the real
driver's URL handling and downloaded binary hash without opening a connection.

The 1.0.1 publication record is `brikk-sql/docs/sql-1.0.1.md`, released from
brikk-sql main `0ecace0f`. SQL and IntelliJ support share that version line.
Consumer PGP verification is not claimed. The earlier 0.4.1 public hash and
dependency-closure audit is in stable-041-verification.md.

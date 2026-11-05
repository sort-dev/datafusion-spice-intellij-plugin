# Local dependency provenance

The build resolves released Brikk artifacts from Maven Central through the
unmodified shared convention. It has no source-project or Maven Local release
override and no snapshot dependency by default. The guarded opt-in snapshot
qualification mechanism is documented in candidate-verification.md.

Released runtime payloads, updated for public IntelliJ support 0.4.1:

| File | SHA-256 |
| --- | --- |
| brikk-sql-jvmMain-0.23.0.jar | 4d272ed1c0a6e84ba9086b5a3e21c62a1c2e094f2d886de3fcbce6d86e9e0d38 |
| brikk-sql-metadata-jvmMain-0.23.0.jar | be6cda26ed8e7de2a89b3bfc15724c466d4d0ec95bf52fb2d4cedfdfe4c158c1 |
| brikk-sql-verify-api-0.23.0.jar | 80688aca1fd5532098bd4580099995a8cb0b9042450901f62e524a424f6d7cf9 |
| brikk-sql-intellij-support-0.4.1.jar | 8400fa9e25462716c085bb7c00801600b8c5ce2cf162148b3792b90ad1a61323 |
| brikk-sql-intellij-jdbc-support-0.4.1.jar | 19bacdaa249fa9408fb7aef5f09df7f80800ab2c1beeddc7565ebc27d259dd05 |

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

The 0.4.1 binary and sources JARs were independently downloaded and compared with
the accepted snapshot. See stable-041-verification.md for that public hash and
dependency-closure audit. Consumer PGP verification is not claimed. The SQL
prerequisite publication record is
`brikk-sql/docs/sql-0.23.0.md`, released from
`b406a1bd894963f3a09f63f02f6b04e606f083f2`.

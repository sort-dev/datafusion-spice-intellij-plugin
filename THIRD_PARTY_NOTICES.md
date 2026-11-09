# Third-party notices

This plugin embeds the following Apache License 2.0 artifacts, Copyright Brikk /
Sortdev SRL, from https://github.com/brikk/brikk-sql:

- `brikk-sql-jvm:1.0.1`
- `brikk-sql-metadata-jvm:1.0.1`
- `brikk-sql-verify-api:1.0.1`
- `brikk-sql-intellij-support:1.0.1`
- `brikk-sql-intellij-jdbc-support:1.0.1`

## Historical owner-authorized local qualification candidate

Ordinary builds use released 1.0.1 above. The 0.2.0-era pre-release qualification
used the coordinates below, retained here for its archived ZIP. Only under that
explicit qualification override were the two IntelliJ runtime artifacts replaced
with the following Apache License 2.0 local SNAPSHOT publications, Copyright
Brikk / Sortdev SRL:

- `brikk-sql-intellij-support:0.4.1-livecatalog-races-20261104-g2a46bd08efcd-SNAPSHOT`
- `brikk-sql-intellij-jdbc-support:0.4.1-livecatalog-races-20261104-g2a46bd08efcd-SNAPSHOT`

Candidate source commit: `2a46bd08efcde21e080e8f0a88d68fc997383fc8` in
https://github.com/brikk/brikk-sql. That candidate paired them with released SQL
core/metadata/verification API 0.23.0. It was not a stable/public release. The
aligned candidate test-support artifact was test-only and not embedded. The
upstream licenses and inherited notices below apply unchanged.

## Inherited Brikk SQL notices

Brikk SQL includes a Kotlin port of SQLGlot and portions derived from polyglot
and DuckDB, under the MIT license:

Copyright (c) 2026 Toby Mao
Copyright (c) 2026 TobiLG <github@tobilg.com>
Copyright 2018-2025 Stichting DuckDB Foundation

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

Bundled metadata includes facts from Apache DataFusion, Apache Doris, StarRocks,
Trino, ClickHouse and Spice under their upstream licenses, and DuckDB under MIT.
Spice named-argument and type information is source-backed metadata, not runtime
model configuration or an execution guarantee. See the released Brikk SQL notices
and source evidence for the inventory provenance.

This product includes software developed at The Apache Software Foundation.
StarRocks: Copyright 2021-present, StarRocks Inc.
Apache Doris: Copyright 2018-2021 The Apache Software Foundation.

The build conventions from Sortdev SRL are vendored under Apache License 2.0:
https://github.com/sort-dev/intellij-sql-plugin-gradle-conventions

The Apache Arrow Flight SQL JDBC driver 19.0.0 is offered as a separate download,
not bundled in the plugin ZIP. It is licensed under Apache License 2.0 and carries
its own third-party notices. The test support artifact is test-only and is not
bundled either.

The plugin icons are original Sortdev artwork, not product logos.

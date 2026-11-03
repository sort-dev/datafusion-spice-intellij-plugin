package dev.sort.datafusionspice.live

import dev.brikk.house.sql.intellij.catalog.*
import dev.brikk.house.sql.intellij.pipe.DatafusionPipeDialect
import dev.brikk.house.sql.intellij.pipe.PipeTranslation
import dev.brikk.house.sql.intellij.pipe.lower
import dev.sort.datafusionspice.PluginPlatformTestCase
import dev.sort.datafusionspice.catalog.datafusionCatalog
import dev.sort.datafusionspice.catalog.spiceCatalog
import dev.sort.datafusionspice.sql.DatafusionSqlDialect
import dev.sort.datafusionspice.sql.SpiceSqlDialect
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Types
import java.util.Properties

/** Opt-in only, against runner-owned disposable fixtures, never a user's saved data source. */
class FlightSqlServerTest : PluginPlatformTestCase() {
    private val product get() = requireNotNull(System.getProperty("flight.test.product")).also {
        require(it in setOf("spice", "datafusion"))
    }

    private fun connect(): Connection {
        val url = requireNotNull(System.getProperty("flight.test.url"))
        require(url.startsWith("jdbc:arrow-flight-sql://127.0.0.1:")) { "Only disposable loopback fixtures are accepted" }
        Class.forName("org.apache.arrow.driver.jdbc.ArrowFlightJdbcDriver")
        DriverManager.setLoginTimeout(10)
        val properties = Properties()
        System.getenv("BRIKK_FLIGHT_TEST_TLS_ROOT")?.let { properties["tlsRootCerts"] = it }
        // Arrow forwards non-reserved properties as RPC headers. Spice expects
        // Bearer authorization on each call; this fixture does not negotiate a token.
        System.getenv("BRIKK_FLIGHT_TEST_TOKEN")?.let { properties["Authorization"] = "Bearer $it" }
        if (product == "datafusion") {
            properties["user"] = "fixture"
            properties["password"] = "fixture"
        }
        return DriverManager.getConnection(url, properties)
    }

    private fun query(connection: Connection, sql: String): List<List<String?>> = connection.createStatement().use { statement ->
        statement.queryTimeout = 10
        statement.executeQuery(sql).use { result ->
            buildList {
                while (result.next()) {
                    add((1..result.metaData.columnCount).map(result::getString))
                    check(size <= 2000) { "Fixture row budget exceeded" }
                }
            }
        }
    }

    fun testProductIdentityAndSimpleRows() = connect().use { connection ->
        val metadata = connection.metaData
        assertTrue(metadata.databaseProductName, metadata.databaseProductName.contains(product, true))
        assertEquals(listOf(listOf("1", "Ada")), query(connection, "SELECT id, name FROM sample WHERE id = 1"))
        assertTrue(query(connection, "SELECT version()").single().single()!!.contains("DataFusion"))
    }

    fun testActualSharedHarvesterAndNamespaceSettings() = connect().use { connection ->
        val rows = object : CatalogRowSource, CatalogServerInfoSource {
            override fun serverInfo() = CatalogServerInfo(connection.metaData.databaseProductName, connection.metaData.databaseProductVersion)
            override fun forEachRow(sql: String, onRow: (CatalogRow) -> Unit) {
                connection.createStatement().use { statement ->
                    statement.queryTimeout = 10
                    statement.executeQuery(sql).use { result ->
                        while (result.next()) onRow(CatalogRow(result::getString))
                    }
                }
            }
        }
        val harvester = if (product == "spice") SpiceCatalogHarvester() else DatafusionCatalogHarvester()
        val harvest = requireNotNull(harvester.harvest(rows)) { "Incomplete $product native harvest" }
        assertEquals(listOf(product, "public"), harvest.session.namespace)
        assertTrue(harvest.shared.functions.isNotEmpty())
        assertTrue(harvest.shared.nativeCatalog!!.relations.any { it.path == listOf(product, "public", "sample") })
        val readback = query(connection, dev.sort.datafusionspice.catalog.NAMESPACE_QUERY)
        assertEquals(listOf(listOf(product, "public")), readback)
        val output = Files.createDirectories(Path.of(System.getProperty("flight.test.output")))
        Files.writeString(output.resolve("$product-inventory.txt"), buildString {
            appendLine("product=${connection.metaData.databaseProductName}")
            appendLine("version=${connection.metaData.databaseProductVersion}")
            appendLine("namespace=${harvest.session.namespace}")
            appendLine("functions=${harvest.shared.functions.size}")
            appendLine("relations=${harvest.shared.nativeCatalog!!.relations.size}")
            appendLine("parserSettings=${harvest.shared.nativeCatalog!!.settings}")
        })
        Unit
    }

    fun testTypedParametersRebindWithoutLiteralSubstitution() = connect().use { connection ->
        connection.prepareStatement("SELECT CAST(\$1 AS BIGINT) + 1 AS n").use { statement ->
            statement.queryTimeout = 10
            for (value in listOf(7L, 9L)) {
                statement.setLong(1, value)
                statement.executeQuery().use { rows ->
                    assertTrue(rows.next())
                    assertEquals(value + 1, rows.getLong(1))
                    assertFalse(rows.next())
                }
            }
            if (product == "datafusion") {
                statement.setNull(1, Types.BIGINT)
                statement.executeQuery().use { rows ->
                    assertTrue(rows.next())
                    assertNull(rows.getObject(1))
                }
            }
        }
    }

    fun testAllQualifiedPipeStagesExecuteAsLoweredSql() = connect().use { connection ->
        val language = if (product == "spice") SpiceSqlDialect else DatafusionSqlDialect
        val catalog = if (product == "spice") spiceCatalog else datafusionCatalog
        val pipe = DatafusionPipeDialect(language, catalog) { true }
        val stages = listOf(
            "SELECT id", "WHERE id = 1", "EXTEND id + 1 AS next_id", "AS s", "AGGREGATE count(*) AS n",
            "DISTINCT", "ORDER BY id", "LIMIT 1", "OFFSET 0", "WINDOW row_number() OVER (ORDER BY id) AS rn",
        )
        val expectedIds = if (product == "spice") listOf("1", "2") else listOf("1")
        val output = StringBuilder()
        for (stage in stages) {
            val lowered = pipe.lower("FROM sample |> $stage") as? PipeTranslation.Lowered
                ?: error("Refused qualified $product stage: $stage")
            val rows = query(connection, lowered.sql)
            output.appendLine("$stage => $rows\n${lowered.sql}")
            when {
                stage.startsWith("AGGREGATE") -> assertEquals(listOf(listOf(expectedIds.size.toString())), rows)
                stage.startsWith("WHERE") -> assertEquals(listOf("1"), rows.map { it.first() })
                stage.startsWith("LIMIT") -> assertEquals(1, rows.size)
                else -> {
                    assertEquals(stage, expectedIds, rows.map { it.first() }.sortedBy { it!!.toLong() })
                    if (stage.startsWith("EXTEND")) assertTrue(rows.all { it.last()!!.toLong() == it.first()!!.toLong() + 1 })
                    if (stage.startsWith("WINDOW")) assertTrue(rows.all { it.last() == it.first() })
                    if (stage.startsWith("ORDER")) assertEquals(expectedIds, rows.map { it.first() })
                }
            }
        }
        val directory = Files.createDirectories(Path.of(System.getProperty("flight.test.output")))
        Files.writeString(directory.resolve("$product-pipes.txt"), output.toString())
        Unit
    }

    fun testSpiceJsonFunctionsOrDatafusionSessionIsolation() = connect().use { connection ->
        if (product == "spice") {
            assertEquals(listOf(listOf("Ada", "30")), query(connection,
                "SELECT json_get_str('{\"name\":\"Ada\",\"age\":30}', 'name'), " +
                    "json_get_int('{\"name\":\"Ada\",\"age\":30}', 'age')"))
        } else {
            connection.createStatement().use { it.execute("CREATE VIEW isolated_readiness AS SELECT 42 AS n") }
            assertEquals(listOf(listOf("42")), query(connection, "SELECT n FROM isolated_readiness"))
            connect().use { other ->
                val failure = runCatching { query(other, "SELECT n FROM isolated_readiness") }.exceptionOrNull()
                assertNotNull("A second handshake must not share session-only views", failure)
                assertTrue(failure.toString(), failure.toString().contains("isolated_readiness"))
                assertEquals(listOf(listOf("1")), query(other, "SELECT 1"))
            }
        }
    }

    fun testFreshConnectionAfterCloseStillWorks() {
        repeat(2) { connect().use { assertEquals(listOf(listOf("1")), query(it, "SELECT 1")) } }
    }
}

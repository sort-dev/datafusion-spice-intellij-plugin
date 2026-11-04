package dev.sort.datafusionspice.live

import dev.sort.datafusionspice.PluginPlatformTestCase
import dev.sort.datafusionspice.catalog.NAMESPACE_QUERY
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.Properties

/** Characterizes real namespace state, never a JDBC update-count-only success. */
class FlightNamespaceContractTest : PluginPlatformTestCase() {
    private val product get() = requireNotNull(System.getProperty("flight.test.product"))
    private fun connect(): Connection {
        val url = requireNotNull(System.getProperty("flight.test.url"))
        require(url.startsWith("jdbc:arrow-flight-sql://127.0.0.1:"))
        require(product in setOf("spice", "datafusion"))
        Class.forName("org.apache.arrow.driver.jdbc.ArrowFlightJdbcDriver")
        return DriverManager.getConnection(url, Properties().apply {
            setProperty("user", "fixture")
            setProperty("password", System.getenv("BRIKK_FLIGHT_TEST_TOKEN") ?: "fixture")
            System.getenv("BRIKK_FLIGHT_TEST_TLS_ROOT")?.let { setProperty("tlsRootCerts", it) }
        })
    }

    private fun namespace(connection: Connection): List<String> = connection.createStatement().use { statement ->
        statement.queryTimeout = 5
        statement.executeQuery(NAMESPACE_QUERY).use { rows ->
            assertTrue(rows.next())
            listOf(rows.getString(1), rows.getString(2)).also { assertFalse(rows.next()) }
        }
    }

    private fun tablesResolve(connection: Connection): Boolean = try {
        connection.createStatement().use { statement ->
            statement.queryTimeout = 5
            statement.executeQuery("SELECT count(*) FROM tables").use { rows ->
                assertTrue(rows.next())
                assertTrue(rows.getLong(1) > 0)
            }
        }
        true
    } catch (_: SQLException) { false }

    private fun execute(connection: Connection, sql: String, prepared: Boolean) {
        if (prepared) connection.prepareStatement(sql).use { it.queryTimeout = 5; it.execute() }
        else connection.createStatement().use { it.queryTimeout = 5; it.execute(sql) }
    }

    fun testUseIsRejectedAndCannotBeAdvertisedAsANamespaceSwitch() {
        for (prepared in listOf(false, true)) for (sql in listOf(
            "USE \"$product\".\"information_schema\"", "USE information_schema",
        )) connect().use { connection ->
            assertEquals(listOf(product, "public"), namespace(connection))
            assertFalse(tablesResolve(connection))
            val rejected = try { execute(connection, sql, prepared); false } catch (_: SQLException) { true }
            assertTrue("Qualified endpoint unexpectedly accepts USE; requalify the switch policy", rejected)
            assertEquals(listOf(product, "public"), namespace(connection))
            assertFalse(tablesResolve(connection))
        }
    }

    fun testArrow19JdbcSetSchemaIsANoOpNotProofOfSwitching() = connect().use { connection ->
        val before = namespace(connection)
        connection.schema = "information_schema"
        assertEquals("Arrow 19's setter must not be mistaken for a server switch", before, namespace(connection))
        assertFalse(tablesResolve(connection))
    }

    fun testSetChangesDatafusionSchemaButIsRejectedBySpiceAndReconnectStartsFresh() {
        for (prepared in listOf(false, true)) connect().use { connection ->
            val sql = "SET datafusion.catalog.default_schema = 'information_schema'"
            if (product == "spice") {
                val rejected = try { execute(connection, sql, prepared); false } catch (_: SQLException) { true }
                assertTrue("Spice's SET policy changed; requalify it rather than treating count=0 as success", rejected)
                assertEquals(listOf(product, "public"), namespace(connection))
                assertFalse(tablesResolve(connection))
            } else {
                execute(connection, sql, prepared)
                assertEquals(listOf(product, "information_schema"), namespace(connection))
                assertTrue(tablesResolve(connection))
                execute(connection, "SET datafusion.catalog.default_schema = 'public'", prepared)
                assertEquals(listOf(product, "public"), namespace(connection))
                assertFalse(tablesResolve(connection))
            }
        }
        connect().use { assertEquals(listOf(product, "public"), namespace(it)) }
    }
}

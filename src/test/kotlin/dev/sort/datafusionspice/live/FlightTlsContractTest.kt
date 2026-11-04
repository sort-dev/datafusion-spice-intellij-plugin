package dev.sort.datafusionspice.live

import dev.sort.datafusionspice.PluginPlatformTestCase
import java.sql.DriverManager
import java.sql.SQLException
import java.util.Properties

/** DataFusion client transport over a TLS-terminating fixture; not a universal server auth policy. */
class FlightTlsContractTest : PluginPlatformTestCase() {
    private fun query(root: String, host: String = "127.0.0.1") {
        check(System.getProperty("flight.test.product") == "datafusion")
        val url = requireNotNull(System.getProperty("flight.test.url"))
        require(url.startsWith("jdbc:arrow-flight-sql://127.0.0.1:") && url.endsWith("?useEncryption=true"))
        Class.forName("org.apache.arrow.driver.jdbc.ArrowFlightJdbcDriver")
        DriverManager.getConnection(url.replace("//127.0.0.1:", "//$host:"), Properties().apply {
            setProperty("user", "fixture")
            setProperty("password", "fixture")
            setProperty("tlsRootCerts", root)
            setProperty("connectTimeoutMs", "3000")
        }).use { connection -> connection.createStatement().use { statement ->
            statement.queryTimeout = 5
            statement.executeQuery("SELECT name FROM sample WHERE id = 1").use { rows ->
                assertTrue(rows.next())
                assertEquals("Ada", rows.getString(1))
                assertFalse(rows.next())
            }
        } }
    }

    private fun rejects(root: String, reason: String, host: String = "127.0.0.1") {
        try {
            query(root, host)
            fail("TLS accepted an invalid fixture certificate/hostname")
        } catch (failure: SQLException) {
            val chain = generateSequence<Throwable>(failure) { it.cause }.joinToString("\n") { it.message.orEmpty() }
            assertTrue("Expected TLS rejection: $reason", chain.contains(reason, ignoreCase = true))
        }
    }

    fun testTrustedRootKeepsVerificationEnabledAndReachesDatafusion() = query(requireNotNull(System.getenv("BRIKK_FLIGHT_TEST_TLS_ROOT")))
    fun testAnUntrustedRootIsRejected() = rejects(requireNotNull(System.getenv("BRIKK_FLIGHT_TEST_UNTRUSTED_ROOT")), "PKIX path building failed")
    fun testWrongHostnameIsRejected() = rejects(requireNotNull(System.getenv("BRIKK_FLIGHT_TEST_TLS_ROOT")), "No name matching localhost", "localhost")
}

package dev.sort.datafusionspice.live

import dev.sort.datafusionspice.PluginPlatformTestCase
import java.sql.DriverManager
import java.sql.SQLException
import java.util.Properties

/** TLS/auth rejection checks on the runner's disposable Spice endpoint, not saved IDE credentials. */
class FlightSqlSecurityTest : PluginPlatformTestCase() {
    private fun query(root: String?, token: String?, host: String = "127.0.0.1") {
        check(System.getProperty("flight.test.product") == "spice")
        val url = requireNotNull(System.getProperty("flight.test.url"))
        require(url.startsWith("jdbc:arrow-flight-sql://127.0.0.1:") && url.endsWith("?useEncryption=true"))
        Class.forName("org.apache.arrow.driver.jdbc.ArrowFlightJdbcDriver")
        val properties = Properties().apply {
            root?.let { setProperty("tlsRootCerts", it) }
            token?.let { setProperty("Authorization", "Bearer $it") }
            setProperty("connectTimeoutMs", "3000")
        }
        DriverManager.getConnection(url.replace("//127.0.0.1:", "//$host:"), properties).use { connection ->
            connection.createStatement().use { statement ->
                statement.queryTimeout = 5
                statement.executeQuery("SELECT 1").use { rows ->
                    assertTrue(rows.next())
                    assertEquals(1, rows.getInt(1))
                    assertFalse(rows.next())
                }
            }
        }
    }

    private fun rejects(root: String?, token: String?, expected: String, host: String = "127.0.0.1") {
        try {
            query(root, token, host)
            fail("The disposable endpoint accepted a rejected TLS/auth configuration")
        } catch (failure: SQLException) {
            // Check the reason without putting the token or raw driver message in assertions.
            val chain = generateSequence<Throwable>(failure) { it.cause }.joinToString("\n") { it.message.orEmpty() }
            val safe = System.getenv("BRIKK_FLIGHT_TEST_TOKEN")?.let { chain.replace(it, "[redacted]") } ?: chain
            assertTrue("Expected $expected rejection: $safe", chain.contains(expected, ignoreCase = true))
        }
    }

    fun testTrustedCertificateAndValidApiKey() = query(
        requireNotNull(System.getenv("BRIKK_FLIGHT_TEST_TLS_ROOT")), requireNotNull(System.getenv("BRIKK_FLIGHT_TEST_TOKEN")))

    fun testMissingApiKeyIsRejected() = rejects(System.getenv("BRIKK_FLIGHT_TEST_TLS_ROOT"), null, "Missing authorization header")

    fun testInvalidApiKeyIsRejected() = rejects(System.getenv("BRIKK_FLIGHT_TEST_TLS_ROOT"), "invalid-fixture-key", "Invalid credentials")

    fun testUntrustedCertificateIsRejected() = rejects(System.getenv("BRIKK_FLIGHT_TEST_UNTRUSTED_ROOT"),
        System.getenv("BRIKK_FLIGHT_TEST_TOKEN"), "PKIX path building failed")

    fun testWrongHostnameIsRejected() = rejects(System.getenv("BRIKK_FLIGHT_TEST_TLS_ROOT"),
        System.getenv("BRIKK_FLIGHT_TEST_TOKEN"), "No name matching localhost", "localhost")
}

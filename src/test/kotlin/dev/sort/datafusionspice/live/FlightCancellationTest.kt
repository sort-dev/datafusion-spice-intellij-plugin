package dev.sort.datafusionspice.live

import dev.sort.datafusionspice.PluginPlatformTestCase
import java.nio.file.Files
import java.nio.file.Path
import java.sql.DriverManager
import java.sql.SQLException
import java.util.Properties
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Explicit opt-in only: one active bounded-batch stream in disposable client/server processes. */
class FlightCancellationTest : PluginPlatformTestCase() {
    fun testActiveStreamCancellationReturnsWithinTheBoundAndTheConnectionRecovers() {
        val product = requireNotNull(System.getProperty("flight.test.product"))
        require(product in setOf("spice", "datafusion"))
        val url = requireNotNull(System.getProperty("flight.test.url"))
        require(url.startsWith("jdbc:arrow-flight-sql://127.0.0.1:"))
        Class.forName("org.apache.arrow.driver.jdbc.ArrowFlightJdbcDriver")
        val properties = Properties().apply {
            setProperty("user", "fixture")
            setProperty("password", System.getenv("BRIKK_FLIGHT_TEST_TOKEN") ?: "fixture")
            System.getenv("BRIKK_FLIGHT_TEST_TLS_ROOT")?.let { setProperty("tlsRootCerts", it) }
        }
        val worker = Executors.newSingleThreadExecutor()
        try {
            DriverManager.getConnection(url, properties).use { connection ->
                connection.createStatement().use { statement ->
                    statement.queryTimeout = 8
                    // The pinned engine's table generator emits 8192-row batches,
                    // not a materialized 100-million-element array. Read one row.
                    statement.executeQuery("SELECT value FROM generate_series(1, 100000000) AS t(value)").use { rows ->
                        assertTrue(rows.next())
                        assertEquals(1L, rows.getLong(1))
                        val started = System.nanoTime()
                        val cancelled = worker.submit(Callable { statement.cancel() })
                        try { cancelled.get(5, TimeUnit.SECONDS) }
                        finally { if (!cancelled.isDone) cancelled.cancel(true) }
                        val elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)
                        assertTrue("Active-stream cancel exceeded its five-second observation bound", elapsed < 5000)
                        val stopped = try { !rows.next() } catch (_: SQLException) { true }
                        assertTrue("Cancelled cursor continued yielding rows", stopped)
                        connection.createStatement().use { probe ->
                            probe.queryTimeout = 3
                            probe.executeQuery("SELECT 1").use { healthy ->
                                assertTrue(healthy.next())
                                assertEquals(1, healthy.getInt(1))
                                assertFalse(healthy.next())
                            }
                        }
                        val output = Files.createDirectories(Path.of(System.getProperty("flight.test.output")))
                        val build = com.intellij.openapi.application.ApplicationInfo.getInstance().build.asString()
                        Files.writeString(output.resolve("$product-stream-cancellation-$build.txt"),
                            "product=$product\nide=$build\ndriver=19.0.0\nfirstRowReceived=true\ncancelMillis=$elapsed\ncursorStopped=true\nsameConnectionHealthy=true\n")
                    }
                }
            }
        } finally { worker.shutdownNow() }
    }
}

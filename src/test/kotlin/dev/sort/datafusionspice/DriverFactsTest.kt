package dev.sort.datafusionspice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.sql.Driver

class DriverFactsTest {
    private fun driver() = Class.forName("org.apache.arrow.driver.jdbc.ArrowFlightJdbcDriver")
        .getDeclaredConstructor().newInstance() as Driver

    @Test
    fun pinnedShadedDriverAcceptsFlightUrlsWithoutClaimingPostgres() {
        val driver = driver()
        assertTrue(driver.acceptsURL("jdbc:arrow-flight-sql://localhost:50051/?useEncryption=true"))
        assertTrue(driver.acceptsURL("jdbc:arrow-flight-sql://[::1]:50051/?useEncryption=false"))
        assertFalse(driver.acceptsURL("jdbc:postgresql://localhost/test"))
    }

    @Test
    fun downloadedJarMatchesTheQualifiedDriverChecksum() {
        // The IDE's PathClassLoader hides JAR origins; Gradle supplies the resolved test artifact.
        val jar = Path.of(requireNotNull(System.getProperty("test.flightDriverJar")))
        assertTrue(jar.toString(), Files.isRegularFile(jar))
        val hash = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(jar))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        assertEquals("d3beee43c613c457789825343368f652d570d76c08799dad38a43a10e569b57f", hash)
    }
}

package dev.sort.datafusionspice

import com.intellij.database.dataSource.DatabaseDriverManager
import com.intellij.database.dataSource.LocalDataSource
import com.intellij.database.run.ConsoleRunConfiguration
import com.intellij.database.dataSource.url.template.JdbcTemplateParser
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory
import java.nio.file.Path
import java.util.concurrent.TimeUnit

class DriverConfigurationTest : PluginPlatformTestCase() {
    fun testBlankConnectionOptionsInheritBothDriverOptionsInTheRealLaunchConfiguration() {
        val manager = DatabaseDriverManager.getInstance()
        for (id in listOf("datafusion-brikk-flight", "spice-brikk-flight")) {
            val driver = requireNotNull(manager.getDriver(id))
            val source = LocalDataSource.fromDriver(driver, "jdbc:offline:vm-inheritance", true)
            assertNull(source.vmOptions)
            val configuration = ConsoleRunConfiguration.newConfiguration(project)
            configuration.setOptionsFromDataSource(source)
            assertTrue(configuration.VM_PARAMETERS, configuration.VM_PARAMETERS.contains("--add-opens=java.base/java.nio=ALL-UNNAMED"))
            assertTrue(configuration.VM_PARAMETERS, configuration.VM_PARAMETERS.contains("--sun-misc-unsafe-memory-access=allow"))
            source.vmOptions = "-Ddatafusion.spice.test.option=true"
            configuration.setOptionsFromDataSource(source)
            assertTrue(configuration.VM_PARAMETERS.contains("--sun-misc-unsafe-memory-access=allow"))
            assertTrue(configuration.VM_PARAMETERS.contains("-Ddatafusion.spice.test.option=true"))
        }
    }

    fun testBothLoadedDriversKeepProductIdentitiesAndPinnedArtifact() {
        val manager = DatabaseDriverManager.getInstance()
        for ((id, dbms, language) in listOf(
            Triple("datafusion-brikk-flight", DatafusionDbms.DATAFUSION_BRIKK, "DataFusionSQL"),
            Triple("spice-brikk-flight", SpiceDbms.SPICE_BRIKK, "SpiceSQL"),
        )) {
            val driver = requireNotNull(manager.getDriver(id))
            assertSame(dbms, driver.forcedDbms)
            assertEquals(language, driver.sqlDialect)
            assertEquals("org.apache.arrow.driver.jdbc.ArrowFlightJdbcDriver", driver.driverClass)
            assertEquals(
                "--add-opens=java.base/java.nio=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow",
                driver.vmOptions,
            )
            assertEquals(1, driver.artifacts.size)
            assertTrue(driver.matchesUrl("jdbc:arrow-flight-sql://localhost:50051/?useEncryption=true"))
            assertFalse(driver.matchesUrl("jdbc:postgresql://localhost/test"))
        }
    }

    fun testDefaultDriverVmOptionsAreAcceptedByTheIdeRuntime() {
        val java = Path.of(System.getProperty("java.home"), "bin", "java").toString()
        val manager = DatabaseDriverManager.getInstance()
        for (id in listOf("datafusion-brikk-flight", "spice-brikk-flight")) {
            val options = requireNotNull(requireNotNull(manager.getDriver(id)).vmOptions).trim().split(Regex("\\s+"))
            val process = ProcessBuilder(listOf(java) + options + "-version").redirectErrorStream(true).start()
            try {
                assertTrue("$id JVM did not exit", process.waitFor(15, TimeUnit.SECONDS))
                val output = process.inputStream.bufferedReader().readText()
                assertEquals("$id rejected its default VM options: $output", 0, process.exitValue())
            } finally {
                if (process.isAlive) process.destroyForcibly().waitFor()
            }
        }
    }

    fun testTemplatesAreSecureAndExposeCanonicalConnectionFields() {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
            javaClass.getResourceAsStream("/config/datafusion-spice-brikk-drivers.xml"),
        )
        val templates = doc.getElementsByTagName("url-template")
        assertEquals(2, templates.length)
        for (index in 0 until templates.length) {
            val template = (templates.item(index) as Element).getAttribute("template")
            val collector = TemplateFields()
            JdbcTemplateParser.parse(template, collector)
            assertEmpty(collector.errors)
            assertTrue(collector.fields.toString(), collector.fields.containsAll(listOf("host", "port", "user", "password")))
            assertTrue(template.contains("useEncryption:param:true"))
            assertFalse(template.contains("useEncryption=false"))
        }
    }

    fun testArtifactIsOneOwnedDownloadNotARollingStockArrowVersion() {
        val text = requireNotNull(javaClass.getResource("/config/datafusion-spice-brikk-artifacts.xml")).readText()
        assertTrue(text.contains("version=\"19.0.0\""))
        assertTrue(text.contains("flight-sql-jdbc-driver-19.0.0.jar"))
        assertTrue(text.contains("type=\"jar\""))
    }

    private class TemplateFields : JdbcTemplateParser.EventProcessor {
        val fields = mutableListOf<String>()
        val errors = mutableListOf<String>()
        override fun processString(s: String) {}
        override fun processGroupStart() {}
        override fun processGroupFinish() {}
        override fun processOptionality() {}
        override fun processNegation() {}
        override fun processParameter(name: String?, p2: String?, p3: String?, p4: String?, p5: String?) { name?.let(fields::add) }
        override fun processError(message: String) { errors += message }
        override fun processListBranch(b: Boolean) {}
        override fun processListStart(s: String) {}
        override fun processListFinish() {}
    }
}

package dev.sort.datafusionspice.live

import com.intellij.database.dataSource.*
import com.intellij.database.dataSource.connection.ConnectionRequestor
import com.intellij.database.psi.DbPsiFacade
import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.ui.classpath.SimpleClasspathElementFactory
import dev.brikk.house.sql.intellij.catalog.*
import dev.sort.datafusionspice.PluginPlatformTestCase
import dev.sort.datafusionspice.catalog.DatafusionLiveCatalogService
import dev.sort.datafusionspice.catalog.SpiceLiveCatalogService
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.Callable

/** Opt-in: the IDE launches Arrow in its remote JDBC process against a runner-owned endpoint. */
class RemoteFlightProcessTest : PluginPlatformTestCase() {
    private lateinit var driver: DatabaseDriverImpl
    private lateinit var source: LocalDataSource
    private lateinit var catalog: LiveCatalogService
    private val product get() = requireNotNull(System.getProperty("flight.test.product"))

    override fun setUp() {
        super.setUp()
        require(product in setOf("spice", "datafusion"))
        val url = requireNotNull(System.getProperty("flight.test.url"))
        require(url.startsWith("jdbc:arrow-flight-sql://127.0.0.1:"))
        project.basePath?.let { Files.createDirectories(Path.of(it)) }
        val drivers = DatabaseDriverManager.getInstance()
        driver = requireNotNull(drivers.getDriver("$product-brikk-flight")).copy("remote-flight-${UUID.randomUUID()}", false) as DatabaseDriverImpl
        driver.artifacts = emptyList()
        val jar = requireNotNull(System.getProperty("test.flightDriverJar"))
        driver.additionalClasspathElements = SimpleClasspathElementFactory.createElements("jar://$jar!/")
        drivers.updateDriver(driver)
        source = LocalDataSource().apply {
            name = "disposable $product remote process"
            databaseDriver = driver
            this.url = if (product == "datafusion") "$url&user=fixture&password=fixture" else url
            isAutoSynchronize = false
            isKeepAlive = false
            workingDir = Files.createDirectories(Path.of("build", "remote-flight-working-dir")).toAbsolutePath().toString()
            System.getenv("BRIKK_FLIGHT_TEST_TLS_ROOT")?.let { driverProperties["tlsRootCerts"] = it }
            System.getenv("BRIKK_FLIGHT_TEST_TOKEN")?.let { driverProperties["Authorization"] = "Bearer $it" }
        }
        LocalDataSourceManager.getInstance(project).addDataSource(source)
        catalog = ApplicationManager.getApplication().getService(if (product == "spice")
            SpiceLiveCatalogService::class.java else DatafusionLiveCatalogService::class.java)
    }

    override fun tearDown() {
        try {
            if (::source.isInitialized) {
                if (::catalog.isInitialized) catalog.forget(source.uniqueId)
                LocalDataSourceManager.getInstance(project).removeDataSource(source)
                DbPsiFacade.getInstance(project).clearCaches()
            }
            if (::driver.isInitialized) DatabaseDriverManager.getInstance().removeDriver(driver)
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        } finally { super.tearDown() }
    }

    private fun <T> background(body: suspend () -> T): T {
        val future = AppExecutorUtil.getAppExecutorService().submit(Callable { runBlocking { body() } })
        try {
            PlatformTestUtil.waitWithEventsDispatching("remote Flight operation finishes", { future.isDone }, 45)
            return future.get()
        } finally { if (!future.isDone) future.cancel(true) }
    }

    private suspend fun <T> connection(body: (DatabaseConnection) -> T): T =
        requireNotNull(DatabaseConnectionManager.getInstance().build(project, source)
            .setRequestor(ConnectionRequestor.Anonymous()).create()).use { body(it.get()) }

    private fun scalar(connection: DatabaseConnection, sql: String): String? {
        val statement = connection.remoteConnection.createStatement()
        try {
            statement.setQueryTimeout(10)
            val rows = statement.executeQuery(sql)
            try {
                assertTrue(rows.next())
                return rows.getString(1).also { assertFalse(rows.next()) }
            } finally { rows.close() }
        } finally { statement.close() }
    }

    fun testActualRemoteDriverLaunchHarvestAndInitialScope() {
        assertTrue(source.databaseVersion.version.isEmpty())
        val before = catalog.profile.identity(source)
        background {
            connection { connection ->
                assertSame(source.dbms, connection.dbms)
                assertTrue(connection.configuration.VM_PARAMETERS.contains("--add-opens=java.base/java.nio=ALL-UNNAMED"))
                assertTrue(connection.configuration.VM_PARAMETERS.contains("--sun-misc-unsafe-memory-access=allow"))
                assertEquals("Ada", scalar(connection, "SELECT name FROM sample WHERE id = 1"))
                assertFalse(source.databaseVersion.version.isEmpty())
                assertEquals("Learning server metadata is not a settings edit", before, catalog.profile.identity(source))
                val inventory = requireNotNull(catalog.selection(source).inventory)
                assertTrue(inventory.functions.isNotEmpty())
                assertTrue(requireNotNull(inventory.nativeCatalog).relations.any { it.path == listOf(product, "public", "sample") })
            }
        }
        assertFalse(source.introspectionScope.isEmpty)
        assertNull("The connection-level field stays additive", source.vmOptions)
    }

    fun testHelperRefreshSkipsTheUserStartupScript() {
        // This intentionally invalid fixture-only script must never run on a helper.
        source.initScript = "SELECT * FROM must_not_run_on_catalog_helper"
        val before = catalog.profile.identity(source)
        val outcome = background { catalog.refresh(project, source) }
        assertEquals(before, catalog.profile.identity(source))
        assertInstanceOf(outcome, RefreshOutcome.Refreshed::class.java)
        assertNotNull(catalog.selection(source).inventory)
        assertTrue("Helper connections must not seed the user's scope", source.introspectionScope.isEmpty)
    }

    fun testFreshRemoteConnectionsWorkAfterClose() {
        repeat(2) { background { connection { assertEquals("1", scalar(it, "SELECT 1")) } } }
    }
}

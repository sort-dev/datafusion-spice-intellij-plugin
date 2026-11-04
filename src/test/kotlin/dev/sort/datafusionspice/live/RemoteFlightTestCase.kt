package dev.sort.datafusionspice.live

import com.intellij.credentialStore.OneTimeString
import com.intellij.database.access.DatabaseCredentials
import com.intellij.database.dataSource.*
import com.intellij.database.dataSource.connection.ConnectionRequestor
import com.intellij.database.psi.DbPsiFacade
import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.ui.classpath.SimpleClasspathElementFactory
import dev.brikk.house.sql.intellij.catalog.LiveCatalogService
import dev.sort.datafusionspice.PluginPlatformTestCase
import dev.sort.datafusionspice.catalog.DatafusionLiveCatalogService
import dev.sort.datafusionspice.catalog.SpiceLiveCatalogService
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.Callable

/** Shared opt-in setup for runner-owned endpoints, never a saved user connection. */
abstract class RemoteFlightTestCase : PluginPlatformTestCase() {
    protected lateinit var driver: DatabaseDriverImpl
    protected lateinit var source: LocalDataSource
    protected lateinit var catalog: LiveCatalogService
    protected val product get() = requireNotNull(System.getProperty("flight.test.product"))

    override fun setUp() {
        super.setUp()
        require(product in setOf("spice", "datafusion"))
        val url = requireNotNull(System.getProperty("flight.test.url"))
        require(url.startsWith("jdbc:arrow-flight-sql://127.0.0.1:"))
        project.basePath?.let { Files.createDirectories(Path.of(it)) }
        val drivers = DatabaseDriverManager.getInstance()
        val fixtureId = System.getenv("BRIKK_FLIGHT_CREDENTIAL_DRIVER")?.also { require(it.startsWith("credential-fixture-")) }
            ?: "remote-flight-${UUID.randomUUID()}"
        if (System.getenv("BRIKK_FLIGHT_CREDENTIAL_DRIVER") != null) drivers.getDriver(fixtureId)?.let(drivers::removeDriver)
        val base = requireNotNull(drivers.getDriver("$product-brikk-flight")) as DatabaseDriverImpl
        driver = if (System.getenv("BRIKK_FLIGHT_CREDENTIAL_DRIVER") != null) {
            // copy(name, ...) generates a new ID; restart tests need a stable reference.
            DatabaseDriverImpl(fixtureId, false).apply { loadState(base.getState(null), false, true, Int.MAX_VALUE) }
        } else base.copy(fixtureId, false)
        driver.artifacts = emptyList()
        val jar = requireNotNull(System.getProperty("test.flightDriverJar"))
        driver.additionalClasspathElements = SimpleClasspathElementFactory.createElements("jar://$jar!/")
        drivers.updateDriver(driver)
        assertSame(driver, drivers.getDriver(driver.id))
        source = LocalDataSource().apply {
            name = "disposable $product remote process"
            databaseDriver = driver
            this.url = url
            if (product == "datafusion" || System.getenv("BRIKK_FLIGHT_TEST_TOKEN") != null) {
                username = "fixture"
                passwordStorage = LocalDataSource.Storage.MEMORY
            }
            isAutoSynchronize = false
            isKeepAlive = false
            workingDir = Files.createDirectories(Path.of("build", "remote-flight-working-dir")).toAbsolutePath().toString()
            System.getenv("BRIKK_FLIGHT_TEST_TLS_ROOT")?.let { driverProperties["tlsRootCerts"] = it }
        }
        if (product == "datafusion" || System.getenv("BRIKK_FLIGHT_TEST_TOKEN") != null) {
            DatabaseCredentials.getInstance().storePassword(source, OneTimeString(System.getenv("BRIKK_FLIGHT_TEST_TOKEN") ?: "fixture"))
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

    protected fun <T> background(body: suspend () -> T): T {
        val future = AppExecutorUtil.getAppExecutorService().submit(Callable { runBlocking { body() } })
        try {
            PlatformTestUtil.waitWithEventsDispatching("remote Flight operation finishes", { future.isDone }, 45)
            return future.get()
        } finally { if (!future.isDone) future.cancel(true) }
    }

    protected suspend fun <T> connection(body: (DatabaseConnection) -> T): T =
        requireNotNull(DatabaseConnectionManager.getInstance().build(project, source)
            .setRequestor(ConnectionRequestor.Anonymous()).create()).use { body(it.get()) }

    protected fun scalar(connection: DatabaseConnection, sql: String): String? {
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
}

package dev.sort.datafusionspice.live

import com.intellij.database.dataSource.*
import com.intellij.database.dataSource.connection.ConnectionRequestor
import com.intellij.database.console.JdbcConsole
import com.intellij.database.console.session.DatabaseSessionManager
import com.intellij.database.console.session.DatabaseSessionViewService
import com.intellij.database.datagrid.DataAuditor
import com.intellij.database.DataBus
import com.intellij.database.datagrid.DataRequest
import com.intellij.database.connection.throwable.info.ErrorInfo
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.database.psi.DbPsiFacade
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.TestActionEvent
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.ui.classpath.SimpleClasspathElementFactory
import dev.brikk.house.sql.intellij.catalog.*
import dev.sort.datafusionspice.PluginPlatformTestCase
import dev.sort.datafusionspice.catalog.DatafusionLiveCatalogService
import dev.sort.datafusionspice.catalog.SpiceLiveCatalogService
import dev.sort.datafusionspice.catalog.DatafusionExecutionObserver
import dev.sort.datafusionspice.catalog.SpiceExecutionObserver
import dev.sort.datafusionspice.pipes.DatafusionPipesSettings
import dev.sort.datafusionspice.pipes.SpicePipesSettings
import dev.sort.datafusionspice.sql.DatafusionSqlDialect
import dev.sort.datafusionspice.sql.SpiceSqlDialect
import dev.brikk.house.sql.intellij.testing.setSqlDialectMapping
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.CopyOnWriteArrayList

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

    fun testRealConsoleExecutePipeAndManualRefresh() {
        val language = if (product == "spice") SpiceSqlDialect else DatafusionSqlDialect
        if (product == "spice") project.service<SpiceExecutionObserver>().start()
        else project.service<DatafusionExecutionObserver>().start()
        val file = myFixture.addFileToProject("real-console-${UUID.randomUUID()}.sql", "SELECT 1")
        setSqlDialectMapping(project, file.virtualFile, language, testRootDisposable)
        myFixture.configureFromExistingVirtualFile(file.virtualFile)
        val session = DatabaseSessionManager.openSession(project, source, "disposable $product console")
        val console = JdbcConsole.newConsole(project).forFile(file.virtualFile).fromDataSource(source).useSession(session).build()
        val finished = CopyOnWriteArrayList<DataRequest>()
        val failures = CopyOnWriteArrayList<String>()
        val statements = CopyOnWriteArrayList<String>()
        val notifications = CopyOnWriteArrayList<Notification>()
        DataBus.addRootAuditor(project, object : DataAuditor {
            override fun requestFinished(context: DataRequest.Context) { finished += context.request }
            override fun error(context: DataRequest.Context, error: ErrorInfo) { failures += error.message }
            override fun beforeStatement(context: DataRequest.Context) { statements += context.query.orEmpty() }
        }, testRootDisposable)
        project.messageBus.connect(testRootDisposable).subscribe(Notifications.TOPIC, object : Notifications {
            override fun notify(notification: Notification) {
                if (notification.groupId == catalog.profile.productLabel) {
                    notification.setSuppressShowingPopup(true)
                    notifications += notification
                }
            }
        })
        val settings = if (product == "spice") project.service<SpicePipesSettings>() else project.service<DatafusionPipesSettings>()
        val enabled = settings.enabled
        fun event() = TestActionEvent.createTestEvent(SimpleDataContext.builder()
            .add(CommonDataKeys.PROJECT, project).add(CommonDataKeys.PSI_FILE, myFixture.file)
            .add(CommonDataKeys.EDITOR, myFixture.editor).add(CommonDataKeys.VIRTUAL_FILE, file.virtualFile)
            .add(com.intellij.database.console.client.SessionClientHolder.CLIENT_KEY, console)
            .add(PlatformCoreDataKeys.CONTEXT_COMPONENT, myFixture.editor.contentComponent).build())
        fun execute(sql: String) {
            WriteCommandAction.runWriteCommandAction(project) { myFixture.editor.document.setText(sql) }
            PsiDocumentManager.getInstance(project).commitAllDocuments()
            FileDocumentManager.getInstance().saveDocument(myFixture.editor.document)
            myFixture.editor.caretModel.moveToOffset(0)
            myFixture.editor.selectionModel.removeSelection()
            val before = finished.size
            requireNotNull(ActionManager.getInstance().getAction("Console.Jdbc.Execute")).actionPerformed(event())
            PlatformTestUtil.waitWithEventsDispatching("actual console Execute completes", { finished.size > before && session.isIdle }, 45)
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
            assertEmpty(failures)
        }
        try {
            execute("SELECT id, name FROM sample ORDER BY id")
            assertNotNull("The actual observer must correlate the console's first harvest", catalog.selection(source, session).inventory)
            assertTrue(language.editorProfile.functions(myFixture.file).any { it.name == "regexp_like" })
            assertEmpty("Connect and SELECT must not show refresh feedback", notifications)
            settings.enabled = true
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
            execute("FROM sample |> WHERE id = 1 |> SELECT name")
            assertTrue(statements.toString(), statements.size >= 2)
            assertTrue("PIPE syntax reached the server unchanged: $statements", statements.none { "|>" in it })
            assertTrue("The user's SQL must not be replaced by generated SQL", myFixture.file.text.startsWith("FROM sample |>"))
            val refresh = if (product == "spice") "Spice.RefreshCatalog" else "Datafusion.RefreshCatalog"
            requireNotNull(ActionManager.getInstance().getAction(refresh)).actionPerformed(event())
            PlatformTestUtil.waitWithEventsDispatching("manual console Refresh reports its outcome", { notifications.isNotEmpty() }, 45)
            assertEquals(NotificationType.INFORMATION, notifications.single().type)
            assertTrue(notifications.single().content, notifications.single().content.contains("refreshed"))
            assertNotNull(catalog.selection(source, session).inventory)
            execute("SELECT 42 AS after_refresh")
            assertEquals(1, notifications.size)
        } finally {
            settings.enabled = enabled
            notifications.forEach(Notification::expire)
            DatabaseSessionViewService.getInstance().destroyView(console)
            Disposer.dispose(console)
            Disposer.dispose(session)
        }
    }
}

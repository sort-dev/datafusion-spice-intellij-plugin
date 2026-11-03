package dev.sort.datafusionspice.catalog

import com.intellij.database.Dbms
import com.intellij.database.dataSource.DatabaseConnectionCore
import com.intellij.database.dataSource.DatabaseConnectionInterceptor
import com.intellij.database.dataSource.DatabaseDriverManager
import com.intellij.database.dataSource.LocalDataSource
import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.PlatformTestUtil
import dev.brikk.house.sql.intellij.catalog.*
import dev.sort.datafusionspice.PluginPlatformTestCase
import kotlinx.coroutines.runBlocking
import java.lang.reflect.Proxy

/** Invokes the real consumer hook/shared harvester; only the remote metadata boundary is recorded. */
class HarvestLifecycleTest : PluginPlatformTestCase() {
    private val sources = mutableListOf<Pair<LocalDataSource, LiveCatalogService>>()

    override fun tearDown() {
        try {
            sources.forEach { (source, service) -> service.forget(source.uniqueId) }
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        } finally { super.tearDown() }
    }

    private fun source(product: String): Pair<LocalDataSource, LiveCatalogService> {
        val source = LocalDataSource().apply {
            databaseDriver = requireNotNull(DatabaseDriverManager.getInstance().getDriver("$product-brikk-flight"))
            url = "jdbc:offline:harvest-original"
            isAutoSynchronize = false
            isKeepAlive = false
        }
        val service = if (product == "spice") ApplicationManager.getApplication().getService(SpiceLiveCatalogService::class.java)
            else ApplicationManager.getApplication().getService(DatafusionLiveCatalogService::class.java)
        return (source to service).also(sources::add)
    }

    private fun connect(product: String, source: LocalDataSource, endpoint: Endpoint) {
        val hook = if (product == "spice") SpiceHarvest() else DatafusionHarvest()
        val proto = proxy(DatabaseConnectionInterceptor.ProtoConnection::class.java) { method, _, _ ->
            when (method) {
                "getProject" -> project
                "getConnectionPoint" -> source
                else -> error(method)
            }
        } as DatabaseConnectionInterceptor.ProtoConnection
        val future = ApplicationManager.getApplication().executeOnPooledThread {
            runBlocking { hook.handleConnected(endpoint.connection(source), proto) }
        }
        try {
            PlatformTestUtil.waitWithEventsDispatching("consumer harvest hook completes", { future.isDone }, 15)
            future.get()
        } finally {
            if (!future.isDone) future.cancel(true)
        }
    }

    fun testSettingsChangedDuringHarvestDoNotSeedTheOldNamespace() {
        for (product in listOf("spice", "datafusion")) {
            val (source, service) = source(product)
            var changed = false
            val endpoint = Endpoint(product).apply {
                beforeQuery = {
                    if (!changed) {
                        changed = true
                        source.url = "jdbc:offline:harvest-replacement"
                    }
                }
            }
            connect(product, source, endpoint)
            assertNull(service.selection(source).inventory)
            assertTrue("A stale connection seeded scope: ${source.introspectionScope}", source.introspectionScope.isEmpty)
            assertFalse(endpoint.queries.contains(NAMESPACE_QUERY))
        }
    }

    fun testSuccessfulHarvestSeedsScopeButPreservesAnExplicitSelection() {
        for (product in listOf("spice", "datafusion")) {
            val (source, service) = source(product)
            val endpoint = Endpoint(product)
            connect(product, source, endpoint)
            assertNotNull(service.selection(source).inventory)
            assertFalse(source.introspectionScope.isEmpty)
            assertEquals(1, endpoint.queries.count { it == NAMESPACE_QUERY })
            val explicit = CatalogScopes.select(listOf(com.intellij.database.model.ObjectKind.DATABASE,
                com.intellij.database.model.ObjectKind.SCHEMA), listOf("keep", "analytics"))
            source.introspectionScope = explicit
            connect(product, source, Endpoint(product))
            assertSame(explicit, source.introspectionScope)
        }
    }

    fun testSettingsChangedDuringNamespaceReadbackAlsoPreventScopeSeeding() {
        for (product in listOf("spice", "datafusion")) {
            val (source, service) = source(product)
            connect(product, source, Endpoint(product).apply {
                beforeQuery = { if (it == NAMESPACE_QUERY) source.url = "jdbc:offline:changed-during-readback" }
            })
            assertTrue(source.introspectionScope.isEmpty)
            assertNull(service.selection(source).inventory)
        }
    }

    fun testSwitchingToAForeignDriverWhileHarvestingCannotSeedTheScope() {
        val (source, _) = source("spice")
        var changed = false
        val endpoint = Endpoint("spice").apply {
            beforeQuery = {
                if (!changed) {
                    changed = true
                    source.databaseDriver = requireNotNull(DatabaseDriverManager.getInstance().getDriver("postgresql"))
                }
            }
        }
        connect("spice", source, endpoint)
        assertTrue(source.introspectionScope.isEmpty)
        assertFalse(endpoint.queries.contains(NAMESPACE_QUERY))
    }

    fun testAChangedIdentityCannotUseAnotherConnectionsCachedInventory() {
        for (product in listOf("spice", "datafusion")) {
            val (source, service) = source(product)
            val originalUrl = source.url
            connect(product, source, Endpoint(product))
            val original = requireNotNull(service.selection(source).inventory)
            source.url = "jdbc:offline:different-endpoint"
            connect(product, source, Endpoint(product).apply { beforeQuery = { throw java.sql.SQLException("fixture metadata unavailable") } })
            assertNull(service.selection(source).inventory)
            source.url = originalUrl
            assertSame("Failed refresh must not delete another identity's good shared cache", original, service.selection(source).inventory)
        }
    }

    fun testForeignConnectionsAreNeverReadByTheProductHook() {
        val (source, _) = source("spice")
        val endpoint = Endpoint("spice", Dbms.POSTGRES)
        connect("spice", source, endpoint)
        assertEmpty(endpoint.queries)
        assertTrue(source.introspectionScope.isEmpty)
    }

    fun testHelperConnectionsDoNotHarvestOrSeedAScope() {
        for (product in listOf("spice", "datafusion")) {
            val (source, service) = source(product)
            val endpoint = Endpoint(product, helper = true)
            connect(product, source, endpoint)
            assertEmpty(endpoint.queries)
            assertTrue(source.introspectionScope.isEmpty)
            assertNull(service.selection(source).inventory)
        }
    }

    private class Endpoint(private val product: String, private val dbms: Dbms? = null, private val helper: Boolean = false) {
        val queries = mutableListOf<String>()
        var beforeQuery: (String) -> Unit = {}
        private val rows get() = mapOf(
            DatafusionCatalogHarvester.SQL_VERSION to listOf(listOf("Apache DataFusion 55.2.0")),
            DatafusionCatalogHarvester.SQL_SETTINGS to listOf(
                listOf("datafusion.catalog.default_catalog", product), listOf("datafusion.catalog.default_schema", "public")),
            DatafusionCatalogHarvester.SQL_SCHEMAS to listOf(listOf(product, "public")),
            DatafusionCatalogHarvester.SQL_TABLES to listOf(listOf(product, "public", "sample", "VIEW")),
            DatafusionCatalogHarvester.SQL_COLUMNS to listOf(listOf(product, "public", "sample", "id", "1", null, "NO", "Int64")),
            DatafusionCatalogHarvester.SQL_VIEWS to listOf(listOf(product, "public", "sample", "SELECT 1 AS id")),
            DatafusionCatalogHarvester.SQL_ROUTINES to listOf(listOf(product, "public", "abs", "SCALAR")),
            DatafusionCatalogHarvester.SQL_FUNCTIONS to listOf(listOf("abs", null, null, null, "SCALAR")),
            NAMESPACE_QUERY to listOf(listOf(product, "public")),
        )

        fun connection(source: LocalDataSource): DatabaseConnectionCore = proxy(
            if (helper) com.intellij.database.dataSource.DatabaseConnection::class.java else DatabaseConnectionCore::class.java,
        ) { method, _, remoteType ->
            when (method) {
                "getRequestor" -> helperRequestor()
                "getDbms" -> dbms ?: source.dbms
                "getConnectionPoint" -> source
                "getAutoCommit" -> true
                "getRemoteConnection" -> proxy(remoteType) { remoteMethod, _, statementType ->
                    when (remoteMethod) {
                        "getMetaData" -> proxy(statementType) { metaMethod, _, _ -> when (metaMethod) {
                            "getDatabaseProductName" -> if (product == "spice") "Spice.ai" else "Apache DataFusion"
                            "getDatabaseProductVersion" -> if (product == "spice") "2.4.0" else "55.2.0"
                            else -> error(metaMethod)
                        } }
                        "createStatement" -> proxy(statementType) { statementMethod, args, rowsType ->
                            when (statementMethod) {
                                "close", "cancel", "setQueryTimeout" -> null
                                "executeQuery" -> {
                                    val sql = args!![0] as String
                                    queries += sql
                                    beforeQuery(sql)
                                    val values = requireNotNull(rows[sql]) { sql }
                                    var index = -1
                                    proxy(rowsType) { resultMethod, resultArgs, metaType -> when (resultMethod) {
                                        "close" -> null
                                        "next" -> (++index < values.size)
                                        "getString" -> values[index][(resultArgs!![0] as Int) - 1]
                                        "getMetaData" -> proxy(metaType) { metaMethod, metaArgs, _ -> when (metaMethod) {
                                            "getColumnCount" -> values.first().size
                                            "getColumnLabel" -> if (metaArgs!![0] == 1) "catalog" else "schema"
                                            else -> error(metaMethod)
                                        } }
                                        else -> error(resultMethod)
                                    } }
                                }
                                else -> error(statementMethod)
                            }
                        }
                        else -> error(remoteMethod)
                    }
                }
                else -> error(method)
            }
        } as DatabaseConnectionCore
    }

    companion object {
        private fun proxy(type: Class<*>, call: (String, Array<out Any?>?, Class<*>) -> Any?): Any =
            Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { self, method, args -> when (method.name) {
                "toString" -> "recording harvest metadata"
                "hashCode" -> System.identityHashCode(self)
                "equals" -> args?.firstOrNull() === self
                else -> call(method.name, args, method.returnType)
            } }
    }
}

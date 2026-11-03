package dev.sort.datafusionspice.catalog

import com.intellij.database.dataSource.DatabaseConnectionCore
import com.intellij.database.dataSource.DatabaseConnectionInterceptor
import com.intellij.database.dataSource.DatabaseDriverManager
import com.intellij.database.dataSource.LocalDataSource
import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.PlatformTestUtil
import dev.brikk.house.sql.intellij.catalog.LiveCatalogService
import dev.sort.datafusionspice.PluginPlatformTestCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.lang.reflect.Proxy
import java.sql.SQLException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Real consumer hook and released harvester, with a cancellable held remote metadata boundary. */
class HarvestCancellationTest : PluginPlatformTestCase() {
    fun testCallerCancellationClosesTheStatementAndNeverSeedsNamespaceScope() {
        for (product in listOf("spice", "datafusion")) {
            val source = LocalDataSource().apply {
                databaseDriver = requireNotNull(DatabaseDriverManager.getInstance().getDriver("$product-brikk-flight"))
                url = "jdbc:offline:cancellation"
                isAutoSynchronize = false
                isKeepAlive = false
            }
            val service: LiveCatalogService = ApplicationManager.getApplication().getService(if (product == "spice")
                SpiceLiveCatalogService::class.java else DatafusionLiveCatalogService::class.java)
            val endpoint = HeldEndpoint(product, source)
            val proto = proxy(DatabaseConnectionInterceptor.ProtoConnection::class.java) { method, _, _ ->
                when (method) {
                    "getProject" -> project
                    "getConnectionPoint" -> source
                    else -> error(method)
                }
            } as DatabaseConnectionInterceptor.ProtoConnection
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            val hook = if (product == "spice") SpiceHarvest() else DatafusionHarvest()
            val job = scope.async { hook.handleConnected(endpoint.connection(), proto) }
            try {
                PlatformTestUtil.waitWithEventsDispatching("native metadata statement starts", { endpoint.entered.count == 0L }, 10)
                val start = System.nanoTime()
                job.cancel()
                PlatformTestUtil.waitWithEventsDispatching("consumer cancellation finishes", { job.isCompleted }, 5)
                assertTrue(job.isCancelled)
                assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 5000)
                assertTrue(endpoint.cancelled.get() > 0)
                assertTrue(endpoint.closed.get() > 0)
                assertTrue(endpoint.finished.await(1, TimeUnit.SECONDS))
                assertEquals(1, endpoint.queries.size)
                assertFalse(endpoint.queries.contains(NAMESPACE_QUERY))
                assertNotEmpty(endpoint.timeouts)
                assertTrue(endpoint.timeouts.all { it > 0 })
                assertTrue(source.introspectionScope.isEmpty)
                assertNull(service.selection(source).inventory)
            } finally {
                endpoint.release.countDown()
                scope.cancel()
                runBlocking { withTimeout(5000) { job.join() } }
                service.forget(source.uniqueId)
            }
        }
    }

    private class HeldEndpoint(private val product: String, private val source: LocalDataSource) {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val finished = CountDownLatch(1)
        val cancelled = AtomicInteger()
        val closed = AtomicInteger()
        val queries = CopyOnWriteArrayList<String>()
        val timeouts = CopyOnWriteArrayList<Int>()

        fun connection() = proxy(DatabaseConnectionCore::class.java) { method, _, returnType -> when (method) {
            "getDbms" -> source.dbms
            "getConnectionPoint" -> source
            "getRemoteConnection" -> proxy(returnType) { remoteMethod, _, remoteType -> when (remoteMethod) {
                "getMetaData" -> proxy(remoteType) { metaMethod, _, _ -> when (metaMethod) {
                    "getDatabaseProductName" -> if (product == "spice") "Spice.ai" else "Apache DataFusion"
                    "getDatabaseProductVersion" -> if (product == "spice") "2.4.0" else "55.2.0"
                    else -> error(metaMethod)
                } }
                "createStatement" -> proxy(remoteType) { statementMethod, args, _ -> when (statementMethod) {
                    "setQueryTimeout" -> { timeouts += args!![0] as Int; null }
                    "cancel" -> { cancelled.incrementAndGet(); release.countDown(); null }
                    "close" -> { closed.incrementAndGet(); null }
                    "executeQuery" -> {
                        queries += args!![0] as String
                        entered.countDown()
                        try {
                            // Only cancel or explicit test cleanup releases this boundary.
                            // Thread interruption alone must not make the test pass.
                            while (release.count > 0) {
                                try { check(release.await(10, TimeUnit.SECONDS)) { "Cancellation fixture was not released" } }
                                catch (_: InterruptedException) { }
                            }
                            throw SQLException("Synthetic cancelled metadata query")
                        } finally { finished.countDown() }
                    }
                    else -> error(statementMethod)
                } }
                else -> error(remoteMethod)
            } }
            else -> error(method)
        } } as DatabaseConnectionCore
    }

    companion object {
        private fun proxy(type: Class<*>, call: (String, Array<out Any?>?, Class<*>) -> Any?): Any =
            Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { self, method, args -> when (method.name) {
                "toString" -> "held synthetic metadata boundary"
                "hashCode" -> System.identityHashCode(self)
                "equals" -> self === args?.firstOrNull()
                else -> call(method.name, args, method.returnType)
            } }
    }
}

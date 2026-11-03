package dev.sort.datafusionspice.catalog

import dev.brikk.house.sql.intellij.catalog.*
import dev.brikk.house.sql.metadata.FunctionDef
import dev.brikk.house.sql.metadata.FunctionKind
import dev.brikk.house.sql.scope.Nullability
import dev.sort.datafusionspice.PluginPlatformTestCase
import java.nio.file.Files
import java.nio.file.Path

class CatalogPersistenceTest : PluginPlatformTestCase() {
    fun testBothProductInventoriesRoundTripWithTheIdeSerializationRuntime() {
        for ((product, version) in listOf("spice" to "2.4.0", "datafusion" to "55.2.0")) {
            val parent = Files.createDirectories(Path.of("build", "catalog-persistence"))
            val directory = Files.createTempDirectory(parent, "$product-")
            try {
                val identity = ConnectionIdentity.of(listOf(product, "loopback-fixture", "user"))
                val native = NativeCatalog(product, "Apache DataFusion 55.2.0", mapOf(
                    "datafusion.catalog.default_catalog" to product,
                    "datafusion.catalog.default_schema" to "public",
                ), listOf(listOf(product, "public")), listOf(
                    NativeCatalogRelation(listOf(product, "public", "a.b"), "VIEW", listOf(
                        NativeCatalogColumn("MixedColumn", 1, "Struct(x Int64)", Nullability.UNKNOWN),
                    ), "SELECT 1 AS \"MixedColumn\""),
                ), "revision-1")
                val inventory = LiveInventory(version,
                    listOf(LiveFunction(FunctionDef("regexp_like", FunctionKind.SCALAR))), false,
                    nativeCatalog = native)
                val store = LiveCatalogStore(directory)
                store.store(product, identity, inventory)
                val session = Any()
                store.storeSession(session, product, identity, SessionFacts(
                    functions = listOf(LiveFunction(FunctionDef("session_only", FunctionKind.SCALAR))),
                    inventory = inventory.copy(serverVersion = "session-version"), namespace = listOf(product, "private"),
                ))
                assertEquals("session-version", store.select(product, identity, session).inventory?.serverVersion)

                val afterRestart = LiveCatalogStore(directory).select(product, identity, session)
                assertInstanceOf(afterRestart.source, CatalogSource.Disk::class.java)
                assertEquals(inventory, afterRestart.inventory)
                assertEquals(SessionFacts.NONE, afterRestart.session)
                assertEquals(native, afterRestart.inventory?.nativeCatalog)
                assertSame(CatalogSource.Bundled,
                    LiveCatalogStore(directory).select(product, ConnectionIdentity.of(listOf("different-user"))).source)
                assertTrue(afterRestart.inventory!!.nativeCatalog!!.relationFacts(listOf(product, "public"))
                    .tables.keys.any { it.contains("\"a.b\"") })
            } finally {
                directory.toFile().deleteRecursively()
            }
        }
    }
}

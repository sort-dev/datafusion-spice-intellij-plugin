package dev.sort.datafusionspice.catalog

import dev.brikk.house.sql.intellij.catalog.*
import dev.brikk.house.sql.metadata.FunctionDef
import dev.brikk.house.sql.metadata.FunctionKind
import dev.brikk.house.sql.scope.Nullability
import java.nio.file.Path

/** Separate JVM entry point: no IDE application, connection or user data source. */
object CatalogCacheProcessProbe {
    @JvmStatic
    fun main(args: Array<String>) {
        val (mode, directory, product) = args.toList()
        require(product in setOf("spice", "datafusion"))
        val identity = ConnectionIdentity.of(listOf(product, "process-fixture", "user"))
        val store = LiveCatalogStore(Path.of(directory))
        val source = "process-fixture-$product"
        if (mode == "write") {
            val native = NativeCatalog(product, "Apache DataFusion 55.2.0", emptyMap(),
                listOf(listOf(product, "public")), listOf(NativeCatalogRelation(
                    listOf(product, "public", "a.b"), "VIEW",
                    listOf(NativeCatalogColumn("MixedColumn", 1, "Int64", Nullability.UNKNOWN)),
                )), "process-revision")
            val inventory = LiveInventory(if (product == "spice") "2.4.0" else "55.2.0",
                listOf(LiveFunction(FunctionDef("abs", FunctionKind.SCALAR))), false, nativeCatalog = native)
            store.store(source, identity, inventory)
            val session = Any()
            store.storeSession(session, source, identity, SessionFacts(
                functions = listOf(LiveFunction(FunctionDef("private_session_function", FunctionKind.SCALAR))),
                inventory = inventory.copy(serverVersion = "private-session-version"),
                namespace = listOf(product, "private-session-schema"),
            ))
            check(store.select(source, identity, session).inventory?.serverVersion == "private-session-version")
            println("written:$product")
        } else {
            require(mode == "read")
            val selected = store.select(source, identity)
            check(selected.source is CatalogSource.Disk)
            check(selected.session == SessionFacts.NONE)
            val inventory = requireNotNull(selected.inventory)
            check(inventory.functions.single().def.name == "abs")
            check(inventory.serverVersion == if (product == "spice") "2.4.0" else "55.2.0")
            val relation = requireNotNull(inventory.nativeCatalog).relations.single()
            check(relation.path == listOf(product, "public", "a.b"))
            check(relation.columns.single().name == "MixedColumn")
            check(relation.columns.single().nullable == Nullability.UNKNOWN)
            println("read:$product")
        }
    }
}

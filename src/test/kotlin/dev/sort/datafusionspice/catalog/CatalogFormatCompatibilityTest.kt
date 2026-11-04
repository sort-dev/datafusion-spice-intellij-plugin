package dev.sort.datafusionspice.catalog

import dev.brikk.house.sql.intellij.catalog.*
import dev.brikk.house.sql.metadata.FunctionDef
import dev.brikk.house.sql.metadata.FunctionKind
import dev.sort.datafusionspice.PluginPlatformTestCase
import com.intellij.openapi.util.Disposer
import java.nio.file.Files
import java.nio.file.Path

/** V1 cache policy: current formats reload; foreign, corrupt and unknown formats fail closed. */
class CatalogFormatCompatibilityTest : PluginPlatformTestCase() {
    private fun each(check: (Path, String, ConnectionIdentity, LiveInventory) -> Unit) {
        for (product in listOf("spice", "datafusion")) {
            val directory = Files.createTempDirectory(Files.createDirectories(Path.of("build", "catalog-format-compatibility")), "$product-")
            Disposer.register(testRootDisposable) { directory.toFile().deleteRecursively() }
            val identity = ConnectionIdentity.of(listOf(product, "format-fixture"))
            val inventory = LiveInventory("fixture", listOf(LiveFunction(FunctionDef("abs", FunctionKind.SCALAR))), false,
                nativeCatalog = NativeCatalog(product, "fixture", emptyMap(), listOf(listOf(product, "public")), emptyList(), "fixture"))
            LiveCatalogStore(directory).store(product, identity, inventory)
            val file = Files.list(directory).use { it.filter(Files::isRegularFile).toList().single() }
            check(file, product, identity, inventory)
        }
    }

    private fun bundled(file: Path, product: String, identity: ConnectionIdentity) {
        val selected = LiveCatalogStore(file.parent).select(product, identity)
        assertSame(CatalogSource.Bundled, selected.source)
        assertNull(selected.inventory)
        assertEquals(SessionFacts.NONE, selected.session)
    }

    fun testUnknownLibraryCacheHeaderFallsBackWithoutInventingAMigration() = each { file, product, identity, _ ->
        val text = Files.readString(file)
        Files.writeString(file, "unknown-future-library-format\n" + text.substringAfter('\n'))
        bundled(file, product, identity)
    }

    fun testUnknownNativeInventoryFormatFallsBackRatherThanUsingPartialMetadata() = each { file, product, identity, _ ->
        val text = Files.readString(file)
        assertTrue(text.contains("\"format\":1"))
        Files.writeString(file, text.replace("\"format\":1", "\"format\":999"))
        bundled(file, product, identity)
    }

    fun testMalformedNativePayloadFallsBackRatherThanDroppingOnlyTheUnknownPart() = each { file, product, identity, _ ->
        val text = Files.readString(file)
        Files.writeString(file, text.lines().joinToString("\n") { if (it.startsWith("native\t")) "native\t{broken" else it })
        bundled(file, product, identity)
    }

    fun testAdditiveNativeFieldsAreCompatibleAndAnotherIdentityCannotReadTheCache() = each { file, product, identity, inventory ->
        val text = Files.readString(file)
        Files.writeString(file, text.replace("native\t{", "native\t{\"future_optional_field\":true,"))
        val selected = LiveCatalogStore(file.parent).select(product, identity)
        assertInstanceOf(selected.source, CatalogSource.Disk::class.java)
        assertEquals(inventory, selected.inventory)
        bundled(file, product, ConnectionIdentity.of(listOf(product, "changed-policy-identity")))
    }
}

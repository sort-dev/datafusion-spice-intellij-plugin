package dev.sort.datafusionspice.catalog

import com.intellij.database.dataSource.DatabaseDriverManager
import com.intellij.database.dataSource.LocalDataSource
import com.intellij.database.model.NameVersion
import dev.sort.datafusionspice.PluginPlatformTestCase

class CatalogIdentityTest : PluginPlatformTestCase() {
    private fun source(product: String) = LocalDataSource().apply {
        databaseDriver = requireNotNull(DatabaseDriverManager.getInstance().getDriver("$product-brikk-flight"))
        url = "jdbc:offline:identity"
        username = "fixture"
    }

    fun testObservedServerVersionDoesNotInvalidateAConnectionOrItsCache() {
        for ((product, profile) in listOf("datafusion" to datafusionCatalog, "spice" to spiceCatalog)) {
            var observed = ""
            val source = object : LocalDataSource() {
                override fun getDatabaseVersion() = NameVersion("fixture", observed)
            }.apply {
                databaseDriver = requireNotNull(DatabaseDriverManager.getInstance().getDriver("$product-brikk-flight"))
                url = "jdbc:offline:identity"
                username = "fixture"
            }
            val before = profile.identity(source)
            observed = "2.4.0"
            assertEquals("2.4.0", source.databaseVersion.version)
            assertEquals(before, profile.identity(source))
            observed = "2.5.0"
            assertEquals(before, profile.identity(source))
        }
    }

    fun testUserControlledConnectionEditsStillInvalidateIdentity() {
        for ((product, profile) in listOf("datafusion" to datafusionCatalog, "spice" to spiceCatalog)) {
            for (edit in listOf<(LocalDataSource) -> Unit>(
                { it.url = "jdbc:offline:replacement" }, { it.username = "different-user" },
                { it.driverProperties["Authorization"] = "Bearer synthetic-test-key" },
                { it.databaseDriver = requireNotNull(it.databaseDriver).copy("replacement-driver", false) },
            )) {
                val source = source(product)
                val before = profile.identity(source)
                edit(source)
                assertFalse("A user edit kept the old $product identity", before == profile.identity(source))
            }
        }
    }

    fun testConnectionPropertyOrderDoesNotChangeIdentityOrExposeCredentialText() {
        for ((product, profile) in listOf("datafusion" to datafusionCatalog, "spice" to spiceCatalog)) {
            val first = source(product).apply {
                driverProperties["Authorization"] = "Bearer synthetic-test-key"
                driverProperties["tlsRootCerts"] = "/fixture/root.pem"
            }
            val second = source(product).apply {
                driverProperties["tlsRootCerts"] = "/fixture/root.pem"
                driverProperties["Authorization"] = "Bearer synthetic-test-key"
            }
            assertEquals(profile.identity(first), profile.identity(second))
            assertFalse(profile.identity(first).toString().contains("synthetic-test-key"))
        }
    }

    fun testProductOwnershipNeverCollapsesToTheSharedFlightUrl() {
        assertFalse(datafusionCatalog.identity(source("datafusion")) == spiceCatalog.identity(source("spice")))
    }
}

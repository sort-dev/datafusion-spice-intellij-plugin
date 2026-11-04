package dev.sort.datafusionspice.credentials

import com.intellij.credentialStore.OneTimeString
import com.intellij.database.access.DatabaseCredentials
import com.intellij.database.dataSource.DatabaseDriverManager
import com.intellij.database.dataSource.LocalDataSource
import com.intellij.database.dataSource.LocalDataSourceSerialization
import com.intellij.openapi.util.Disposer
import dev.sort.datafusionspice.PluginPlatformTestCase
import java.io.StringReader
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

class SavedCredentialsTest : PluginPlatformTestCase() {
    private lateinit var vault: PrivatePasswordSafe

    override fun setUp() {
        super.setUp()
        val directory = Files.createTempDirectory(Files.createDirectories(Path.of("build", "private-password-safe")), "sdk-").toAbsolutePath()
        Disposer.register(testRootDisposable) { directory.toFile().deleteRecursively() }
        vault = PrivatePasswordSafe(directory)
        Disposer.register(testRootDisposable) { vault.close() }
        vault.install(testRootDisposable)
    }

    private fun source(product: String) = LocalDataSource().apply {
        databaseDriver = requireNotNull(DatabaseDriverManager.getInstance().getDriver("$product-brikk-flight"))
        url = "jdbc:arrow-flight-sql://127.0.0.1:65000/?useEncryption=true"
        username = "saved-credential-user"
        passwordStorage = LocalDataSource.Storage.PERSIST
        isAutoSynchronize = false
        isKeepAlive = false
    }

    private fun restored(source: LocalDataSource): Pair<LocalDataSource, String> {
        val xml = LocalDataSourceSerialization.serializedSettings(project, source).toString()
        val reloaded = LocalDataSource()
        LocalDataSourceSerialization.deserialize(project, reloaded, StringReader(xml))
        reloaded.resolveDriver()
        assertEquals(source.uniqueId, reloaded.uniqueId)
        assertEquals(source.databaseDriver?.id, reloaded.databaseDriver?.id)
        return reloaded to xml
    }

    private fun noPlaintext(secret: String, xml: String) {
        assertFalse("Credential leaked into data-source XML", xml.contains(secret))
        Files.walk(vault.directory).use { paths -> paths.filter(Files::isRegularFile).forEach { file ->
            assertFalse("Credential leaked into a vault file", String(Files.readAllBytes(file), Charsets.ISO_8859_1).contains(secret))
        } }
    }

    fun testBothDriverCredentialsSurviveEncryptedStoreAndDataSourceReload() {
        for (product in listOf("datafusion", "spice")) {
            val source = source(product)
            val secret = "dummy-${UUID.randomUUID()}"
            DatabaseCredentials.getInstance().storePassword(source, OneTimeString(secret))
            val (reloaded, xml) = restored(source)
            vault.reopen()
            assertTrue("Saved credential was not restored", DatabaseCredentials.getInstance().loadPassword(reloaded)?.toString() == secret)
            assertEquals(LocalDataSource.Storage.PERSIST, reloaded.passwordStorage)
            assertFalse(reloaded.url.orEmpty().contains("password="))
            noPlaintext(secret, xml)
        }
    }

    fun testMemoryOnlyCredentialsNeverBecomePersistent() {
        for (product in listOf("datafusion", "spice")) {
            val source = source(product).apply { passwordStorage = LocalDataSource.Storage.MEMORY }
            val secret = "memory-${UUID.randomUUID()}"
            DatabaseCredentials.getInstance().storePassword(source, OneTimeString(secret))
            val (_, xml) = restored(source)
            vault.save()
            // A second service instance has no memory-helper cache, as after an IDE restart.
            val second = PrivatePasswordSafe(vault.directory)
            Disposer.register(testRootDisposable) { second.close() }
            second.install(testRootDisposable)
            assertNull(DatabaseCredentials.getInstance().loadPassword(source))
            noPlaintext(secret, xml)
        }
    }

    fun testReplacingASavedPasswordReplacesOnlyThatDataSourcesEntry() {
        val credentials = DatabaseCredentials.getInstance()
        val first = source("spice")
        val other = source("spice")
        val old = "old-${UUID.randomUUID()}"
        val replacement = "new-${UUID.randomUUID()}"
        val unrelated = "other-${UUID.randomUUID()}"
        credentials.storePassword(first, OneTimeString(old))
        credentials.storePassword(other, OneTimeString(unrelated))
        credentials.storePassword(first, OneTimeString(replacement))
        vault.reopen()
        assertTrue("Replacement password was not restored", credentials.loadPassword(first)?.toString() == replacement)
        assertTrue("Another data source's credential changed", credentials.loadPassword(other)?.toString() == unrelated)
        noPlaintext(old, restored(first).second)
        noPlaintext(replacement, restored(first).second)
    }

    fun testCopiedConnectionDoesNotInheritTheOriginalCredentialKey() {
        val original = source("datafusion")
        val secret = "original-${UUID.randomUUID()}"
        DatabaseCredentials.getInstance().storePassword(original, OneTimeString(secret))
        val copy = LocalDataSourceSerialization.createDataSourceFromSettings(project,
            LocalDataSourceSerialization.serializedSettings(project, original).toString(), false)
        assertFalse(original.uniqueId == copy.uniqueId)
        assertNull(DatabaseCredentials.getInstance().loadPassword(copy))
    }
}

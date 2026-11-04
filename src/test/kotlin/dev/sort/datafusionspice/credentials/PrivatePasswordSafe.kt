package dev.sort.datafusionspice.credentials

import com.intellij.credentialStore.EncryptionSpec
import com.intellij.credentialStore.EncryptionType
import com.intellij.credentialStore.CredentialStore
import com.intellij.credentialStore.PasswordSafeSettings
import com.intellij.credentialStore.ProviderType
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.ide.passwordSafe.impl.BasePasswordSafe
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.registerOrReplaceServiceInstance
import java.nio.file.Path

/** Test-only real encrypted backend. Neither default Password Safe settings nor OS keychains are used. */
internal class PrivatePasswordSafe(val directory: Path) : AutoCloseable {
    private val database = directory.resolve("credentials.kdbx")
    private val key = directory.resolve("fixture-master-key")
    private val options = PasswordSafeSettings().apply {
        providerType = ProviderType.KEEPASS
        keepassDb = database.toString()
    }
    // The backend is Kotlin-internal but its constructor is public JVM API.
    // Reflection stays in this fixture; no production exemption or OS provider.
    private val backend = Class.forName("com.intellij.credentialStore.keePass.KeePassCredentialStore")
    private fun open() = backend.getConstructor(Path::class.java, Path::class.java).newInstance(database, key) as CredentialStore
    private var store = open()
    val safe = object : BasePasswordSafe() {
        override val settings get() = options
    }.apply { currentProvider = store }

    fun install(until: Disposable) {
        ApplicationManager.getApplication().registerOrReplaceServiceInstance(PasswordSafe::class.java, safe, until)
    }

    fun save() {
        backend.getMethod("save", EncryptionSpec::class.java).invoke(store, EncryptionSpec(EncryptionType.BUILT_IN, null))
    }

    fun reopen() {
        save()
        (store as AutoCloseable).close()
        store = open()
        safe.currentProvider = store
    }

    override fun close() = (store as AutoCloseable).close()
}

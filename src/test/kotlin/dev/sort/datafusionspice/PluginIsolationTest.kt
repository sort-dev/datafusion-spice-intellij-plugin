package dev.sort.datafusionspice

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.ide.plugins.DynamicPluginEnabler
import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.ActionWithDelegate
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.impl.DynamicActionConfigurationCustomizer
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.AccessToken
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.PluginId
import com.intellij.testFramework.LoggedErrorProcessor
import com.intellij.testFramework.TestApplicationManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.nio.file.Files
import java.security.MessageDigest
import java.util.zip.ZipFile

/** Like ClickHouse's isolated tests: no flattened Brikk/test-support dependency at runtime. */
class PluginIsolationTest : BasePlatformTestCase() {
    private val errors = CopyOnWriteArrayList<Throwable>()
    private var logging: AccessToken? = null
    private val ownId = "dev.sort.datafusion-spice-intellij-plugin"

    override fun setUp() {
        TestApplicationManager.getInstance()
        logging = LoggedErrorProcessor.executeWith(object : LoggedErrorProcessor() {
            override fun processError(category: String, message: String, details: Array<out String>, t: Throwable?): Set<Action> {
                errors += t ?: AssertionError(message + "\n" + details.joinToString("\n"))
                return super.processError(category, message, details, t)
            }
        })
        super.setUp()
    }

    override fun tearDown() {
        try {
            super.tearDown()
        } finally {
            logging?.finish()
            if (errors.isNotEmpty()) throw AssertionError("Background errors in isolated plugin startup").apply {
                errors.forEach(::addSuppressed)
            }
        }
    }

    private fun loaders(): List<ClassLoader> {
        val ids = listOf(ownId) + System.getProperty("test.siblingPlugins", "").split(',')
            .filter(String::isNotBlank).map { "dev.sort.$it-intellij-plugin" }
        return ids.map { id ->
            val descriptor = requireNotNull(PluginManagerCore.getPlugin(PluginId.getId(id)))
            assertTrue(id, descriptor.isEnabled)
            requireNotNull(descriptor.pluginClassLoader)
        }
    }

    // Test-only IDE plugin-manager driver; this class is never shipped in the ZIP.
    @OptIn(com.intellij.openapi.util.IntellijInternalApi::class)
    fun testInstalledDistributionMatchesZipPayload() {
        val archive = System.getProperty("test.installedPluginZip") ?: return
        val descriptor = requireNotNull(PluginManagerCore.getPlugin(PluginId.getId(ownId)))
        val directory = descriptor.pluginPath.resolve("lib")
        fun hash(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).toList()
        val expected = ZipFile(archive).use { zip ->
            zip.entries().asSequence().filter { !it.isDirectory && it.name.endsWith(".jar") }
                .associate { it.name.substringAfterLast('/') to hash(zip.getInputStream(it).use { stream -> stream.readBytes() }) }
        }
        val actual = Files.list(directory).use { files ->
            files.filter { it.fileName.toString().endsWith(".jar") }.toList()
                .associate { it.fileName.toString() to hash(Files.readAllBytes(it)) }
        }
        assertEquals("Installed lib directory differs from archived payload", expected, actual)
        assertTrue("Product must load through its private plugin loader", loaders().first() !== javaClass.classLoader)
        if (java.lang.Boolean.getBoolean("test.dynamicPluginLifecycle")) {
            val enabler = DynamicPluginEnabler()
            try {
                assertTrue("Dynamic disable was rejected", enabler.disable(listOf(descriptor)))
                assertNull(Language.findLanguageByID("DataFusionSQL"))
                assertNull(Language.findLanguageByID("SpiceSQL"))
                assertNull(ActionManager.getInstance().getAction("Datafusion.RefreshCatalog"))
                assertNull(ActionManager.getInstance().getAction("Spice.RefreshCatalog"))
            } finally {
                assertTrue("Dynamic re-enable failed", enabler.enable(listOf(descriptor)))
            }
            assertNotNull(Language.findLanguageByID("DataFusionSQL"))
            assertNotNull(Language.findLanguageByID("SpiceSQL"))
            assertNotNull(ActionManager.getInstance().getAction("Datafusion.RefreshCatalog"))
            assertNotNull(ActionManager.getInstance().getAction("Spice.RefreshCatalog"))
        }
    }

    fun testEveryPluginOwnsItsCoreAndIdeSupportCopies() {
        val loaders = loaders()
        for (name in listOf("dev.brikk.house.sql.BrikkSql", "dev.brikk.house.sql.intellij.dialect.SqlDialectRegistry")) {
            val copies = loaders.map { loader ->
                val type = Class.forName(name, false, loader)
                assertSame(name, loader, type.classLoader)
                type
            }
            assertEquals(copies.size, copies.toSet().size)
        }
    }

    fun testBothLanguagesBelongToThisPluginAndKotlinIsAbsent() {
        val own = loaders().first()
        for (id in listOf("DataFusionSQL", "SpiceSQL")) {
            val language = requireNotNull(Language.findLanguageByID(id))
            assertSame(id, own, language.javaClass.classLoader)
        }
        assertFalse(PluginManagerCore.getPlugin(PluginId.getId("org.jetbrains.kotlin"))?.isEnabled == true)
    }

    fun testInstalledExecuteChainsRetainAllDialectWrappers() {
        for (id in IDS) {
            var action = requireNotNull(ActionManager.getInstance().getAction(id))
            val chain = mutableListOf<Class<*>>()
            val seen = Collections.newSetFromMap(IdentityHashMap<AnAction, Boolean>())
            while (seen.add(action)) {
                chain += action.javaClass
                action = (action as? ActionWithDelegate<*>)?.delegate as? AnAction ?: break
            }
            for (loader in loaders()) {
                val wrapper = Class.forName("dev.brikk.house.sql.intellij.CooperativeExecuteAction", false, loader)
                assertTrue("$id missing $loader: $chain", wrapper in chain)
            }
            assertTrue(chain.toString(), chain.last().name.startsWith("com.intellij.database.actions.RunQueryAction"))
        }
    }

    fun testOwnCustomizerDelegatesUnownedInputOnceAndDetachesCleanly() {
        val own = loaders().first()
        val customizer = own.loadClass("dev.sort.datafusionspice.pipes.DatafusionSpiceExecuteConfiguration")
            .getConstructor().newInstance() as DynamicActionConfigurationCustomizer
        val manager = ActionManager.getInstance()
        val originals = IDS.associateWith { requireNotNull(manager.getAction(it)) }
        var calls = 0
        val event = AnActionEvent.createEvent(SimpleDataContext.getProjectContext(project), Presentation(),
            "DatafusionSpiceIsolation", ActionUiKind.NONE, null)
        try {
            for (id in IDS) manager.replaceAction(id, object : AnAction() {
                override fun actionPerformed(e: AnActionEvent) { calls++ }
            })
            customizer.registerActions(manager)
            for (id in IDS) requireNotNull(manager.getAction(id)).actionPerformed(event)
            assertEquals(IDS.size, calls)
            customizer.unregisterActions(manager)
            for (id in IDS) requireNotNull(manager.getAction(id)).actionPerformed(event)
            assertEquals(IDS.size * 2, calls)
        } finally {
            customizer.unregisterActions(manager)
            originals.forEach { (id, action) -> manager.replaceAction(id, action) }
        }
    }

    companion object {
        private val IDS = listOf("Console.Jdbc.Execute", "Console.Jdbc.Execute.2", "Console.Jdbc.Execute.3", "Console.Jdbc.Execute.Selection")
    }
}

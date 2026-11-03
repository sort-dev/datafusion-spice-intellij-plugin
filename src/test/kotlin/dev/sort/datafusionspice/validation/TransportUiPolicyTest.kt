package dev.sort.datafusionspice.validation

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.extensions.impl.ExtensionPointImpl
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.EditorNotificationProvider
import dev.sort.datafusionspice.PluginPlatformTestCase
import dev.sort.datafusionspice.sql.SpiceSqlDialect

class TransportUiPolicyTest : PluginPlatformTestCase() {
    fun testPluginDoesNotRegisterAnUnconditionalTransportBanner() {
        val name = "com.intellij.editorNotificationProvider"
        val area = listOf(project.extensionArea, ApplicationManager.getApplication().extensionArea)
            .first { it.hasExtensionPoint(name) }
        val point = area.getExtensionPoint<EditorNotificationProvider>(name)
        val own = (point as ExtensionPointImpl<EditorNotificationProvider>).sortedAdapters.filter {
            it.pluginDescriptor.pluginId == PluginId.getId("dev.sort.datafusion-spice-intellij-plugin")
        }
        assertEmpty(own)
    }

    fun testStatementsDoNotReceiveUngatedUpstreamTransportWarnings() {
        val sql = "PREPARE p AS SELECT 1; EXECUTE p; DEALLOCATE p; SET datafusion.execution.batch_size = 1024"
        myFixture.configureFromExistingVirtualFile(LightVirtualFile("transport.sql", SpiceSqlDialect, sql))
        val descriptions = myFixture.doHighlighting().mapNotNull { it.description }
        assertTrue(descriptions.toString(), descriptions.none {
            it.contains("Spice Flight SQL:") || it.contains("NULL JDBC binding") || it.contains("driver memory")
        })
    }

    fun testRemovingTransportWarningsDoesNotSuppressLocalSyntaxErrors() {
        myFixture.configureFromExistingVirtualFile(LightVirtualFile("syntax.sql", SpiceSqlDialect, "SELECT ("))
        assertTrue(myFixture.doHighlighting().any { it.severity == HighlightSeverity.ERROR })
    }
}

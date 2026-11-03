package dev.sort.datafusionspice

import com.intellij.codeInsight.intention.impl.config.IntentionManagerSettings
import com.intellij.codeInsight.intention.impl.config.IntentionActionMetaData
import dev.sort.datafusionspice.pipes.*

class IntentionResourcesTest : PluginPlatformTestCase() {
    fun testInstalledIntentionMetadataLoadsDescriptionsAndExamples() {
        // The settings view deduplicates by family and flat tests use the core loader.
        val families = setOf("DataFusion Brikk PIPE", "Spice Brikk PIPE")
        val registered = IntentionManagerSettings.getInstance().getMetaData().filter { it.family in families }
        assertEquals(families, registered.map { it.family }.toSet())
        registered.forEach { assertTrue(it.description.getText().contains("<html>")) }
        // Use the same public resource metadata lookup for every concrete action,
        // including the second action of each deduplicated settings family.
        val metadata = listOf(DatafusionPipePreviewIntention(), DatafusionPipeRunIntention(),
            SpicePipePreviewIntention(), SpicePipeRunIntention()).map { action ->
            IntentionActionMetaData(action, action.javaClass.classLoader,
                arrayOf(action.familyName), action.javaClass.simpleName, false)
        }
        for (entry in metadata) {
            val description = entry.description.getText()
            assertTrue(entry.toString(), description.contains("<html>"))
            assertTrue(description, description.contains("DataFusion") || description.contains("Spice"))
            assertEquals(1, entry.exampleUsagesBefore.size)
            assertEquals(1, entry.exampleUsagesAfter.size)
            assertTrue(entry.exampleUsagesBefore.single().getText().contains("|>"))
            assertTrue(entry.exampleUsagesAfter.single().getText().contains("SELECT id FROM sample"))
        }
    }
}

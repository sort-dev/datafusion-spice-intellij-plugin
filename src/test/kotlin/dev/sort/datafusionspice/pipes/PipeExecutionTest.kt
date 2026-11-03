package dev.sort.datafusionspice.pipes

import com.intellij.openapi.components.service
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.LightVirtualFile
import dev.brikk.house.sql.intellij.testing.PipeExecutionContract
import dev.sort.datafusionspice.PluginPlatformTestCase
import java.util.UUID

class PipeExecutionTest : PluginPlatformTestCase() {
    private val previousFlags = mutableMapOf<String, String?>()

    override fun setUp() {
        super.setUp()
        for (pipe in listOf(datafusionPipe, spicePipe)) {
            previousFlags[pipe.vetoProperty] = System.getProperty(pipe.vetoProperty)
            System.clearProperty(pipe.vetoProperty)
        }
    }

    override fun tearDown() {
        try {
            project.service<DatafusionPipesSettings>().enabled = false
            project.service<SpicePipesSettings>().enabled = false
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
            previousFlags.forEach { (name, value) ->
                if (value == null) System.clearProperty(name) else System.setProperty(name, value)
            }
        } finally {
            super.tearDown()
        }
    }

    private fun each(check: PipeExecutionContract.() -> Unit) {
        for ((pipe, driver) in listOf(datafusionPipe to "datafusion-brikk-flight", spicePipe to "spice-brikk-flight")) {
            fun enabled(value: Boolean) {
                when (pipe) {
                    datafusionPipe -> project.service<DatafusionPipesSettings>().enabled = value
                    spicePipe -> project.service<SpicePipesSettings>().enabled = value
                }
                PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
            }
            enabled(true)
            val contract = PipeExecutionContract(pipe, driver, "postgresql", ::enabled) { sql ->
                myFixture.configureFromExistingVirtualFile(LightVirtualFile("pipe-${UUID.randomUUID()}.sql", pipe.language, sql))
                myFixture.editor
            }
            contract.check()
            enabled(false)
        }
    }

    fun testClaimsOnlyPipes() = each { claimsOnlyPipes() }
    fun testForeignAndDisabledSessionsDelegate() = each { declinesWhenNotOwned() }
    fun testSelectionsUseStockStatementRules() = each { selectsStatementsLikeStockExecute() }
    fun testRefusedStatementBlocksWholeScript() = each { refusesWholeScriptsItCannotRun() }
    fun testStaleEditsDuringSaveAndPromptingAreRefused() = each { refusesStaleInput() }
    fun testDestinationChangesDuringPromptingAreRefused() = each { refusesDestinationChangesDuringPrompting() }
    fun testNamespaceChangesDuringPromptingAreRefused() = each { refusesNamespaceChangesDuringPrompting() }
    fun testCancellationPropagates() = each { propagatesCancellation() }
    fun testRunToStage() = each { runsTopLevelStagesToTheCaret() }
    fun testNestedPipelines() = each { lowersNestedOnlyPipelines() }
    fun testRefusalsUseTheProductsNotificationGroup() = each { notifiesRefusals() }
}

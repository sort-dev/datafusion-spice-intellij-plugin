package dev.sort.datafusionspice.pipes

import com.intellij.openapi.components.service
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.LightVirtualFile
import dev.brikk.house.sql.intellij.pipe.PipeTranslation
import dev.brikk.house.sql.intellij.pipe.lower
import dev.brikk.house.sql.verify.VerifyResult
import dev.sort.datafusionspice.PluginPlatformTestCase
import dev.sort.datafusionspice.sql.DatafusionSqlDialect
import dev.sort.datafusionspice.sql.SpiceSqlDialect
import dev.sort.datafusionspice.validation.datafusionErrors
import dev.sort.datafusionspice.validation.spiceErrors

class PipeSettingsTest : PluginPlatformTestCase() {
    fun testNativeModeDefaultsAndIndependentWorkspaceSettings() {
        val df = project.service<DatafusionPipesSettings>()
        val sp = project.service<SpicePipesSettings>()
        assertFalse(df.enabled)
        assertFalse(sp.enabled)
        try {
            df.enabled = true
            assertTrue(datafusionPipe.isProjectEnabled(project))
            assertFalse(spicePipe.isProjectEnabled(project))
            sp.enabled = true
            df.enabled = false
            assertFalse(datafusionPipe.isProjectEnabled(project))
            assertTrue(spicePipe.isProjectEnabled(project))
        } finally {
            df.enabled = false
            sp.enabled = false
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        }
    }

    fun testUnsupportedStagesAreRefusedRatherThanSubmittedRaw() {
        for (pipe in listOf(datafusionPipe, spicePipe)) {
            assertTrue(pipe.lower("FROM sample |> DROP id") is PipeTranslation.Refused)
            assertTrue(pipe.lower("SELECT * FROM (FROM sample |> DROP id) s") is PipeTranslation.Refused)
            assertTrue(pipe.lower("FROM sample |> SELECT 1 AS n") is PipeTranslation.Lowered)
            assertTrue(pipe.previewNote.contains("not native pipe syntax"))
        }
    }

    fun testNativeVerificationIsUnavailableAndLocalErrorsRemainEnabled() {
        for ((language, errors, pipe) in listOf(
            Triple(DatafusionSqlDialect, datafusionErrors, datafusionPipe),
            Triple(SpiceSqlDialect, spiceErrors, spicePipe),
        )) {
            myFixture.configureFromExistingVirtualFile(LightVirtualFile("native.sql", language, "SELECT 1"))
            assertTrue(errors.localParser)
            assertTrue(errors.reportsPsiErrors)
            assertTrue(pipe.validator.verify("SELECT 1", null) is VerifyResult.Unavailable)
        }
    }
}

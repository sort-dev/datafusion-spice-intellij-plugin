package dev.sort.datafusionspice

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.brikk.house.sql.intellij.testing.BackgroundErrors

abstract class PluginPlatformTestCase : BasePlatformTestCase() {
    private var backgroundErrors: BackgroundErrors? = null

    override fun setUp() {
        super.setUp()
        backgroundErrors = BackgroundErrors(testRootDisposable)
    }

    override fun tearDown() {
        try {
            super.tearDown()
        } finally {
            backgroundErrors?.assertNone()
        }
    }
}

package dev.sort.datafusionspice.sql

import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.sql.dialects.postgres.PgDialect
import com.intellij.sql.psi.SqlStringLiteralExpression
import com.intellij.testFramework.LightVirtualFile
import dev.brikk.house.sql.intellij.dialect.DatafusionFamilyDialect
import dev.brikk.house.sql.metadata.FunctionPresentation
import dev.sort.datafusionspice.PluginPlatformTestCase
import java.util.UUID

/** Explicit tagged-dollar injection only; no automatic host recognition or host quote decoding fix. */
class InjectedEditorTest : PluginPlatformTestCase() {
    override fun setUp() {
        super.setUp()
        val settings = CodeInsightSettings.getInstance()
        val previous = settings.AUTOCOMPLETE_ON_CODE_COMPLETION
        settings.AUTOCOMPLETE_ON_CODE_COMPLETION = false
        Disposer.register(testRootDisposable) { settings.AUTOCOMPLETE_ON_CODE_COMPLETION = previous }
    }

    private fun inject(language: DatafusionFamilyDialect, sql: String, delimiter: String = "${'$'}body${'$'}"): Pair<SqlStringLiteralExpression, com.intellij.psi.PsiFile> {
        val host = "SELECT $delimiter$sql$delimiter"
        myFixture.configureFromExistingVirtualFile(LightVirtualFile("injected-${UUID.randomUUID()}.sql", PgDialect.INSTANCE, host))
        val literal = requireNotNull(PsiTreeUtil.findChildOfType(myFixture.file, SqlStringLiteralExpression::class.java))
        val hostFile = myFixture.file
        val hostVirtualFile = hostFile.virtualFile
        val hostStart = literal.textRange.startOffset
        val manager = InjectedLanguageManager.getInstance(project)
        manager.registerMultiHostInjector(object : MultiHostInjector {
            override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
                val host = context as? SqlStringLiteralExpression ?: return
                // Completion works on a copied file containing a dummy identifier.
                // Keep the explicit claim on that copy, not just the original PSI object.
                if (manager.isInjectedFragment(host.containingFile)) return
                if (host.containingFile.originalFile.virtualFile != hostVirtualFile || host.textRange.startOffset != hostStart) return
                registrar.startInjecting(language).addPlace(null, null, host,
                    TextRange(delimiter.length, host.textLength - delimiter.length)).doneInjecting()
            }
            override fun elementsToInjectIn() = listOf(SqlStringLiteralExpression::class.java)
        }, testRootDisposable)
        val injected = requireNotNull(manager.getInjectedPsiFiles(literal)).single().first.containingFile
        assertSame(language, injected.language)
        assertEquals(sql, injected.text)
        return literal to injected
    }

    fun testBothExplicitDialectsKeepUnicodeOffsetsAndLocalErrorsInsideTaggedHosts() {
        for (language in listOf(DatafusionSqlDialect, SpiceSqlDialect)) {
            val sql = "SELECT '😀' AS label, regexp_like('ABC', 'abc', 'i'); SELECT ("
            val (literal, injected) = inject(language, sql)
            val manager = InjectedLanguageManager.getInstance(project)
            val function = sql.indexOf("regexp_like")
            val hostRange = manager.injectedToHost(injected, TextRange(function, function + "regexp_like".length))
            assertEquals("regexp_like", myFixture.file.text.substring(hostRange.startOffset, hostRange.endOffset))
            assertTrue(literal.textRange.contains(hostRange))
            val errors = PsiTreeUtil.findChildrenOfType(injected, PsiErrorElement::class.java)
            assertNotEmpty(errors)
            assertTrue(errors.all { manager.injectedToHost(injected, it.textRange).startOffset >= literal.textRange.startOffset + 6 })
        }
    }

    fun testRegisteredFunctionCompletionEditsOnlyTheInjectedQuery() {
        for (language in listOf(DatafusionSqlDialect, SpiceSqlDialect)) {
            val (_, injected) = inject(language, "SELECT '😀' AS label, regexp_li")
            val offset = myFixture.file.text.indexOf("regexp_li") + "regexp_li".length
            myFixture.editor.caretModel.moveToOffset(offset)
            val items = myFixture.completeBasic().orEmpty()
            val item = items.single { it.lookupString.equals("regexp_like", true) && it.`object` is FunctionPresentation }
            myFixture.lookup.currentItem = item
            myFixture.finishLookup(Lookup.NORMAL_SELECT_CHAR)
            val host = myFixture.file.text
            assertTrue(host, host.startsWith("SELECT ${'$'}body${'$'}SELECT '😀' AS label, "))
            assertTrue(host, host.endsWith("${'$'}body${'$'}"))
            assertTrue(host, host.contains("regexp_like", ignoreCase = true))
            assertFalse(injected.text.contains("${'$'}body${'$'}"))
        }
    }

    fun testSingleQuotedHostsWithoutEscapesKeepBothExplicitParserIdentities() {
        for (language in listOf(DatafusionSqlDialect, SpiceSqlDialect)) {
            val (_, injected) = inject(language, "SELECT 1 AS n", "'")
            assertEmpty(PsiTreeUtil.findChildrenOfType(injected, PsiErrorElement::class.java))
        }
    }
}

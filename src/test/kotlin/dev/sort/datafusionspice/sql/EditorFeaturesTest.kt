package dev.sort.datafusionspice.sql

import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.lang.LanguageParserDefinitions
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.LightVirtualFile
import dev.brikk.house.sql.metadata.FunctionPresentation
import dev.brikk.house.sql.intellij.dialect.DatafusionFamilyDialect
import dev.brikk.house.sql.intellij.testing.setSqlDialectMapping
import dev.brikk.house.sql.metadata.FunctionKind
import dev.sort.datafusionspice.PluginPlatformTestCase
import java.util.UUID

class EditorFeaturesTest : PluginPlatformTestCase() {
    private val dialects get() = listOf(DatafusionSqlDialect, SpiceSqlDialect)

    override fun setUp() {
        super.setUp()
        val settings = CodeInsightSettings.getInstance()
        val before = settings.AUTOCOMPLETE_ON_CODE_COMPLETION
        settings.AUTOCOMPLETE_ON_CODE_COMPLETION = false
        Disposer.register(testRootDisposable) { settings.AUTOCOMPLETE_ON_CODE_COMPLETION = before }
    }

    private fun open(sql: String, dialect: DatafusionFamilyDialect) {
        val caret = sql.indexOf("<caret>")
        myFixture.configureFromExistingVirtualFile(LightVirtualFile("editor-${UUID.randomUUID()}.sql", dialect, sql.replace("<caret>", "")))
        if (caret >= 0) myFixture.editor.caretModel.moveToOffset(caret)
    }

    fun testBothParsersKeepQueriesAndUtilityStatementBoundaries() {
        for (dialect in dialects) {
            open("SELECT regexp_like('ABC', 'abc', 'i'); RESET datafusion.execution.batch_size; SELECT [1, 2] AS xs", dialect)
            assertNotNull(LanguageParserDefinitions.INSTANCE.forLanguage(dialect))
            assertEmpty(PsiTreeUtil.findChildrenOfType(myFixture.file, PsiErrorElement::class.java))
            assertSame(dialect, myFixture.file.language)
        }
    }

    fun testExplicitDialectMappingCanSelectEitherProductOnAnOrdinarySqlFile() {
        val file = myFixture.configureByText("association.sql", "SELECT 1")
        for (dialect in dialects) {
            setSqlDialectMapping(project, file.virtualFile, dialect)
            assertSame(dialect, myFixture.file.language)
        }
    }

    fun testIncompleteSqlKeepsLocalErrors() {
        for (dialect in dialects) {
            open("SELECT (", dialect)
            assertNotEmpty(PsiTreeUtil.findChildrenOfType(myFixture.file, PsiErrorElement::class.java))
        }
    }

    fun testProductMetadataAndFallbackProvenanceStayIndependent() {
        for (dialect in dialects) {
            open("SELECT 1", dialect)
            val profile = dialect.editorProfile
            assertSame(profile, dialect.editorProfile)
            assertEquals("bundled fallback", profile.catalogProvenance(myFixture.file))
            val names = profile.functions(myFixture.file).map { it.name }
            assertTrue("regexp_like" in names)
            assertEquals(dialect === SpiceSqlDialect, "ai_decide" in names)
            assertNull(profile.serverVersion(myFixture.file))
        }
    }

    fun testCompletionUsesTheRealRegisteredContributors() {
        for (dialect in dialects) {
            open("SELECT regexp_li<caret>", dialect)
            val items = myFixture.completeBasic().orEmpty()
            assertTrue(items.map { it.lookupString }.toString(), items.any { it.lookupString.equals("regexp_like", true) })
            assertEquals(1, items.count { it.lookupString.equals("regexp_like", true) && it.`object` is FunctionPresentation })
        }
    }

    fun testSpiceSearchIsOnlyOfferedAsATableFunction() {
        open("SELECT * FROM vec<caret>", SpiceSqlDialect)
        val table = myFixture.completeBasic().orEmpty().mapNotNull { it.`object` as? FunctionPresentation }
        assertTrue(table.toString(), table.any { it.name == "vector_search" && it.kind == FunctionKind.TABLE_VALUED })
        open("SELECT vec<caret>", SpiceSqlDialect)
        val scalar = myFixture.completeBasic().orEmpty().mapNotNull { it.`object` as? FunctionPresentation }
        assertTrue(scalar.none { it.name == "vector_search" })
    }

    fun testFormattingDoesNotChangeDollarQuotedValues() {
        for (dialect in dialects) {
            open("SELECT ${'$'}${'$'}a; β${'$'}${'$'} AS value; SELECT 2", dialect)
            WriteCommandAction.runWriteCommandAction(project) { CodeStyleManager.getInstance(project).reformat(myFixture.file) }
            assertTrue(myFixture.file.text, myFixture.file.text.contains("${'$'}${'$'}a; β${'$'}${'$'}"))
            assertEmpty(PsiTreeUtil.findChildrenOfType(myFixture.file, PsiErrorElement::class.java))
        }
    }

    fun testIncrementalEditDoesNotCorruptFollowingStatement() {
        for (dialect in dialects) {
            open("SELECT 'x;y'; SELECT 2 AS n", dialect)
            WriteCommandAction.runWriteCommandAction(project) { myFixture.editor.document.replaceString(8, 9, "β😀") }
            PsiDocumentManager.getInstance(project).commitAllDocuments()
            assertTrue(myFixture.file.text.endsWith("SELECT 2 AS n"))
            assertEmpty(PsiTreeUtil.findChildrenOfType(myFixture.file, PsiErrorElement::class.java))
        }
    }
}

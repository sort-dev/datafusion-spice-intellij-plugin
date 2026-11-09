package dev.sort.datafusionspice.sql

import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.lang.LanguageParserDefinitions
import com.intellij.lang.LanguageDocumentation
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiElement
import com.intellij.sql.psi.SqlAsExpression
import com.intellij.sql.psi.SqlStringLiteralExpression
import com.intellij.refactoring.rename.RenameProcessor
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupElementPresentation
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

    fun testDatafusionBrikkDiagnosticsKeepNumberBeforeEndAndValuesAliasBeforeNestedJoin() {
        // The inherited PostgreSQL lexer still reports 13END as bad characters; see dev-docs/TODO.md.
        open("SELECT CASE WHEN true THEN 13END", DatafusionSqlDialect)
        val brikk = myFixture.doHighlighting().mapNotNull { it.description }.filter { it.contains("brikk-sql") }
        assertTrue(brikk.toString(), brikk.isEmpty())

        open("SELECT * FROM (VALUES (1)) t1(a) LEFT JOIN (VALUES (1)) t2(b) JOIN (VALUES (1)) t3(c) ON true ON true", DatafusionSqlDialect)
        assertEmpty(PsiTreeUtil.findChildrenOfType(myFixture.file, PsiErrorElement::class.java))
        val errors = myFixture.doHighlighting().filter { it.severity == HighlightSeverity.ERROR }
        assertTrue(errors.map { it.description }.toString(), errors.isEmpty())
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

    fun testNamedArgumentCompletionKeepsTheExistingValueAndOneArrow() {
        open("SELECT * FROM vector_search(docs,'q', lim<caret> => 5)", SpiceSqlDialect)
        val item = myFixture.completeBasic().orEmpty().single {
            it.lookupString == "limit" && LookupElementPresentation.renderElement(it).typeText == "vector_search argument"
        }
        myFixture.lookup.currentItem = item
        myFixture.finishLookup(Lookup.NORMAL_SELECT_CHAR)
        assertEquals("SELECT * FROM vector_search(docs,'q', limit => 5)", myFixture.editor.document.text)
    }

    fun testRegisteredDocumentationKeepsProductEvidenceAndLinks() {
        open("SELECT ai_decide(body, '{}', model => 'local') FROM docs", SpiceSqlDialect)
        val docs = LanguageDocumentation.INSTANCE.allForLanguage(SpiceSqlDialect).filterIsInstance<SpiceDocumentation>().single()
        val offset = myFixture.file.text.indexOf("ai_decide")
        val target = requireNotNull(docs.getCustomDocumentationElement(myFixture.editor, myFixture.file, myFixture.file.findElementAt(offset), offset))
        val html = requireNotNull(docs.generateDoc(target, null))
        assertTrue(html, html.contains("2.4.0"))
        assertTrue(html, html.contains("model configuration"))
        assertEquals(listOf("https://spiceai.org/docs/reference/sql/ai"), docs.getUrlFor(target, null))
    }

    fun testLocalAliasRenameDoesNotChangeMatchingStringLiterals() {
        for (dialect in dialects) {
            open("WITH q AS (SELECT 1 AS item) SELECT item, 'item' FROM q", dialect)
            val alias = PsiTreeUtil.findChildrenOfType(myFixture.file, SqlAsExpression::class.java).first { it.name == "item" }
            RenameProcessor(project, alias, "renamed", false, false).run()
            assertTrue(myFixture.file.text, myFixture.file.text.contains("AS renamed", true))
            assertTrue(myFixture.file.text, myFixture.file.text.contains("SELECT renamed", true))
            assertTrue(myFixture.file.text.contains("'item'"))
        }
    }

    fun testExplicitInjectedSpiceSqlInDollarStringKeepsUnicodeAndParserIdentity() {
        val text = "SELECT ${'$'}${'$'}SELECT '😀' AS label, json_get_int('{\"a\":7}', 'a') AS age${'$'}${'$'}"
        myFixture.configureFromExistingVirtualFile(LightVirtualFile("host.sql", com.intellij.sql.dialects.postgres.PgDialect.INSTANCE, text))
        val literal = requireNotNull(PsiTreeUtil.findChildOfType(myFixture.file, SqlStringLiteralExpression::class.java))
        val manager = InjectedLanguageManager.getInstance(project)
        manager.registerMultiHostInjector(object : MultiHostInjector {
            override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
                if (context !== literal) return
                registrar.startInjecting(SpiceSqlDialect)
                    .addPlace(null, null, literal, TextRange(2, literal.textLength - 2)).doneInjecting()
            }
            override fun elementsToInjectIn() = listOf(SqlStringLiteralExpression::class.java)
        }, testRootDisposable)
        val injected = requireNotNull(manager.getInjectedPsiFiles(literal)).single().first.containingFile
        assertSame(SpiceSqlDialect, injected.language)
        assertTrue(injected.text, injected.text.contains("json_get_int('{\"a\":7}', 'a')"))
        assertTrue(injected.text.contains("'😀'"))
        assertEmpty(PsiTreeUtil.findChildrenOfType(injected, PsiErrorElement::class.java))
        val offset = injected.text.indexOf("json_get_int")
        val range = manager.injectedToHost(injected, TextRange(offset, offset + "json_get_int".length))
        assertEquals("json_get_int", text.substring(range.startOffset, range.endOffset))
    }
}

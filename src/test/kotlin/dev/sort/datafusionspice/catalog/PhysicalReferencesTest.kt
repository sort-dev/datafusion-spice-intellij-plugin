package dev.sort.datafusionspice.catalog

import com.intellij.database.dataSource.DatabaseDriverManager
import com.intellij.database.dataSource.LocalDataSource
import com.intellij.database.dataSource.LocalDataSourceManager
import com.intellij.database.dialects.postgres.model.PgRoot
import com.intellij.database.model.ModelFactory
import com.intellij.database.model.ObjectKind
import com.intellij.database.model.basic.BasicModel
import com.intellij.database.psi.DbPsiFacade
import com.intellij.database.util.ObjectPath
import com.intellij.database.util.SearchPath
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.testFramework.IndexingTestUtil
import dev.brikk.house.sql.intellij.testing.RecordingConsole
import dev.brikk.house.sql.intellij.catalog.CatalogScopes
import dev.brikk.house.sql.intellij.testing.setSqlDialectMapping
import dev.sort.datafusionspice.PluginPlatformTestCase
import dev.sort.datafusionspice.sql.DatafusionSqlDialect
import dev.sort.datafusionspice.sql.SpiceSqlDialect
import java.util.UUID

/** Real platform model symbols and indexed PSI references; no database rename or SQL execution. */
class PhysicalReferencesTest : PluginPlatformTestCase() {
    private fun each(body: (LocalDataSource, String) -> Unit) {
        for ((product, language) in listOf("spice" to SpiceSqlDialect, "datafusion" to DatafusionSqlDialect)) {
            val driver = requireNotNull(DatabaseDriverManager.getInstance().getDriver("$product-brikk-flight"))
            val model = ModelFactory.BLACK_HOLE.createModel(requireNotNull(driver.forcedDbms))
            val source = LocalDataSource().apply {
                databaseDriver = driver
                url = "jdbc:offline:physical-reference"
                isAutoSynchronize = false
                isKeepAlive = false
                this.model = model
            }
            val root = model.root as PgRoot
            for (schema in listOf("public", "other")) {
                val table = root.databases.createOrGet(product).schemas.createOrGet(schema).tables.createOrGet("sample")
                table.columns.createOrGet("id")
                table.columns.createOrGet("name")
                source.introspectionScope = CatalogScopes.widen(source.introspectionScope,
                    CatalogScopes.select(listOf(ObjectKind.DATABASE, ObjectKind.SCHEMA), listOf(product, schema)))
            }
            LocalDataSourceManager.getInstance(project).addDataSource(source)
            try {
                DbPsiFacade.getInstance(project).clearCaches()
                val sql = "SELECT p.id, q.id, p.id AS picked, 'id' AS literal FROM $product.public.sample p " +
                    "JOIN $product.other.sample q ON p.id = q.id"
                val file = myFixture.addFileToProject("physical-${UUID.randomUUID()}.sql", sql)
                setSqlDialectMapping(project, file.virtualFile, language, testRootDisposable)
                myFixture.configureFromExistingVirtualFile(file.virtualFile)
                IndexingTestUtil.waitUntilIndexesAreReady(project)
                RecordingConsole(project, file.virtualFile, source).use { console ->
                    console.console.setSearchPath(SearchPath.of(ObjectPath.create(product, ObjectKind.DATABASE).append("public", ObjectKind.SCHEMA)))
                    body(source, product)
                }
            } finally {
                LocalDataSourceManager.getInstance(project).removeDataSource(source)
                DbPsiFacade.getInstance(project).clearCaches()
            }
        }
    }

    fun testFindUsagesKeepsSameNamedColumnsInDifferentSchemasSeparate() = each { source, product ->
        val model = (source.model as BasicModel).root as PgRoot
        val column = model.databases.single { it.name == product }.schemas.single { it.name == "public" }
            .tables.single { it.name == "sample" }.columns.single { it.name == "id" }
        val target = requireNotNull(DbPsiFacade.getInstance(project).findDataSource(source.uniqueId)?.findElement(column))
        val reference = requireNotNull(myFixture.file.findReferenceAt(myFixture.file.text.indexOf("p.id") + 2))
        assertTrue(reference.isReferenceTo(target))
        val usages = ReferencesSearch.search(target).findAll().filter { it.element.containingFile == myFixture.file }
        assertEquals(usages.toString(), 3, usages.size)
        assertTrue(usages.all { it.isReferenceTo(target) })
        assertTrue(usages.none { it.element.text.contains("q.id") || it.element.text.contains("'id'") })
    }

    fun testPhysicalReferenceRenameOnlyEditsItsBoundSqlIdentifiers() = each { source, product ->
        val column = ((source.model as BasicModel).root as PgRoot).databases.single { it.name == product }.schemas.single { it.name == "public" }
            .tables.single { it.name == "sample" }.columns.single { it.name == "id" }
        val target = requireNotNull(DbPsiFacade.getInstance(project).findDataSource(source.uniqueId)?.findElement(column))
        val usages = ReferencesSearch.search(target).findAll().filter { it.element.containingFile == myFixture.file }
        assertEquals(3, usages.size)
        // This is the platform's reference edit operation, not a database DDL rename action.
        WriteCommandAction.runWriteCommandAction(project) {
            usages.sortedByDescending { it.element.textOffset }.forEach { it.handleElementRename("renamed") }
        }
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        assertEquals("SELECT p.renamed, q.id, p.renamed AS picked, 'id' AS literal FROM $product.public.sample p " +
            "JOIN $product.other.sample q ON p.renamed = q.id", myFixture.file.text)
        assertEquals("id", column.name)
    }
}

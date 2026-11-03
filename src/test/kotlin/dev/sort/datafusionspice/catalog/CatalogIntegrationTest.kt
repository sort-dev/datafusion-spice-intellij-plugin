package dev.sort.datafusionspice.catalog

import com.intellij.database.dataSource.DatabaseConnectionCore
import com.intellij.database.dataSource.DatabaseDriverManager
import com.intellij.database.dataSource.LocalDataSource
import com.intellij.database.dataSource.LocalDataSourceManager
import com.intellij.database.dataSource.DataSourceSyncManager
import com.intellij.database.dialects.postgres.model.PgRoot
import com.intellij.database.model.ModelFactory
import com.intellij.database.model.ObjectKind
import com.intellij.database.psi.DbPsiFacade
import com.intellij.database.util.ObjectPath
import com.intellij.database.util.SearchPath
import com.intellij.database.util.TreePatternUtils
import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.lang.LanguageAnnotators
import com.intellij.lang.annotation.AnnotationSession
import com.intellij.codeInsight.daemon.impl.AnnotationHolderImpl
import com.intellij.openapi.components.service
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.registerOrReplaceServiceInstance
import dev.brikk.house.sql.intellij.catalog.*
import dev.brikk.house.sql.intellij.testing.*
import dev.sort.datafusionspice.*
import dev.sort.datafusionspice.sql.DatafusionSqlDialect
import dev.sort.datafusionspice.sql.SpiceSqlDialect
import java.lang.reflect.Proxy

class CatalogIntegrationTest : PluginPlatformTestCase() {
    private lateinit var held: HeldIntrospections
    private lateinit var errors: BackgroundErrors
    private val sources = mutableListOf<LocalDataSource>()

    override fun setUp() {
        super.setUp()
        errors = BackgroundErrors(testRootDisposable)
        held = HeldIntrospections(testRootDisposable)
        val settings = CodeInsightSettings.getInstance()
        val before = settings.AUTOCOMPLETE_ON_CODE_COMPLETION
        settings.AUTOCOMPLETE_ON_CODE_COMPLETION = false
        Disposer.register(testRootDisposable) { settings.AUTOCOMPLETE_ON_CODE_COMPLETION = before }
        project.registerOrReplaceServiceInstance(DatafusionAutoIntrospectService::class.java,
            DatafusionAutoIntrospectService(project, errors.newScope(testRootDisposable)), testRootDisposable)
        project.registerOrReplaceServiceInstance(SpiceAutoIntrospectService::class.java,
            SpiceAutoIntrospectService(project, errors.newScope(testRootDisposable)), testRootDisposable)
    }

    override fun tearDown() {
        try {
            held.finishAll()
            errors.awaitScopes()
            sources.forEach { LocalDataSourceManager.getInstance(project).removeDataSource(it) }
            DbPsiFacade.getInstance(project).clearCaches()
            errors.assertNone()
        } finally {
            super.tearDown()
        }
    }

    private fun namespace(catalog: String, schema: String) =
        SearchPath.of(ObjectPath.create(catalog, ObjectKind.DATABASE).append(schema, ObjectKind.SCHEMA))

    fun testInitialScopeSelectsActualCatalogAndSchemaButPreservesUserSelections() {
        for (catalog in listOf("spice", "datafusion", "cat.one")) {
            val source = LocalDataSource()
            assertTrue(initializeProductScope(source, namespace(catalog, "public")))
            val expected = CatalogScopes.select(listOf(ObjectKind.DATABASE, ObjectKind.SCHEMA), listOf(catalog, "public"))
            assertEquals(TreePatternUtils.serialize(expected), TreePatternUtils.serialize(source.introspectionScope))
            assertFalse(initializeProductScope(source, namespace(catalog, "another")))
            assertEquals(TreePatternUtils.serialize(expected), TreePatternUtils.serialize(source.introspectionScope))
        }
    }

    fun testReadbackUsesOneMetadataQueryAndReturnsCatalogAndSchema() {
        for (dialect in listOf("spice", "datafusion")) {
            dev.brikk.house.sql.dialects.Dialects.forName(dialect).parse(NAMESPACE_QUERY)
        }
        val sql = mutableListOf<String>()
        val connection = metadataConnection("spice", "public", sql)
        val actual = SpiceDatabaseDialect().tryToLoadSearchPath(connection)
        assertEquals(namespace("spice", "public").current, actual?.current)
        assertEquals(listOf(NAMESPACE_QUERY), sql)
        assertNull(SpiceDatabaseDialect().sqlSetSearchPath(requireNotNull(actual)))
    }

    fun testUnknownNamespaceDoesNotGuessPublic() {
        assertNull(DatafusionDatabaseDialect().tryToLoadSearchPath(metadataConnection("datafusion", null, mutableListOf())))
    }

    fun testBothProductsUseTheirNativeResolversForLazyIntrospection() {
        assertSame(datafusionCatalog.dataSources, project.service<DatafusionAutoIntrospectService>().profile.dataSources)
        assertSame(spiceCatalog.dataSources, project.service<SpiceAutoIntrospectService>().profile.dataSources)
        assertEquals(DatafusionAutoIntrospectService::class.java, project.service<DatafusionExecutionObserver>().profile.autoIntrospect)
        assertEquals(SpiceAutoIntrospectService::class.java, project.service<SpiceExecutionObserver>().profile.autoIntrospect)
    }

    fun testOpeningAQualifiedReferenceLoadsItsNamespaceWithoutEditing() = loadOnOpen(qualified = true)
    fun testOpeningAnUnqualifiedReferenceUsesTheConsoleNamespace() = loadOnOpen(qualified = false)
    fun testTypingAQualifiedNamespaceTriggersTheRegisteredCompletionHook() = loadOnOpen(qualified = true, completion = true)

    private fun loadOnOpen(qualified: Boolean, completion: Boolean = false) {
        for ((language, driverId, catalog) in listOf(
            Triple(SpiceSqlDialect, "spice-brikk-flight", "spice"),
            Triple(DatafusionSqlDialect, "datafusion-brikk-flight", "datafusion"),
        )) {
            val driver = requireNotNull(DatabaseDriverManager.getInstance().getDriver(driverId))
            val model = ModelFactory.BLACK_HOLE.createModel(requireNotNull(driver.forcedDbms))
            val source = LocalDataSource().apply {
                databaseDriver = driver
                url = "jdbc:offline:catalog-integration"
                isAutoSynchronize = false
                isKeepAlive = false
                this.model = model
            }
            val root = model.root as PgRoot
            val schema = root.databases.createOrGet(catalog).schemas.createOrGet("public")
            LocalDataSourceManager.getInstance(project).addDataSource(source)
            sources += source
            DbPsiFacade.getInstance(project).clearCaches()
            val table = if (completion) "$catalog.public." else if (qualified) "$catalog.public.sample" else "sample"
            val file = myFixture.configureByText("catalog.sql", "SELECT ${if (completion) "*" else "id"} FROM $table")
            setSqlDialectMapping(project, file.virtualFile, language, testRootDisposable)
            myFixture.editor.caretModel.moveToOffset(myFixture.editor.document.textLength)
            IndexingTestUtil.waitUntilIndexesAreReady(project)
            RecordingConsole(project, file.virtualFile, source).use { recording ->
                recording.console.setSearchPath(namespace(catalog, "public"))
                val stamp = myFixture.editor.document.modificationStamp
                val requests: AutoIntrospectService = if (catalog == "spice")
                    project.service<SpiceAutoIntrospectService>() else project.service<DatafusionAutoIntrospectService>()
                if (completion) {
                    myFixture.completeBasic()
                    LookupManager.getInstance(project).hideActiveLookup()
                } else {
                    val annotator = LanguageAnnotators.INSTANCE.allForLanguage(language)
                        .filterIsInstance<ProductReferenceIntrospection>().single()
                    val holder = AnnotationHolderImpl(AnnotationSession(myFixture.file), false)
                    holder.runAnnotatorWithContext(myFixture.file, com.intellij.lang.annotation.Annotator { _, annotations ->
                        annotator.annotate(myFixture.file, annotations)
                    })
                    holder.assertAllAnnotationsCreated()
                }
                val refresh = held.next()
                assertEquals(catalog, refresh.context.tasks.single().databaseName)
                refresh.finish {
                    val loaded = schema.tables.createOrGet("sample")
                    loaded.columns.createOrGet("id")
                    loaded.columns.createOrGet("name")
                }
                PlatformTestUtil.waitWithEventsDispatching("namespace introspection finishes without an edit", {
                    !DataSourceSyncManager.getInstance().isActive(source) && requests.pending(file.virtualFile) == null
                }, 10)
                errors.awaitScopes()
                assertEquals(stamp, myFixture.editor.document.modificationStamp)
                assertEquals(AutoIntrospectEngine.State.Done,
                    requests.engine.state(AutoIntrospectEngine.Key(source.uniqueId, listOf(catalog, "public"))))
                val facts = language.editorProfile.providers.first().facts(myFixture.file)
                assertInstanceOf(facts, dev.brikk.house.sql.intellij.relation.RelationFactsResult.Available::class.java)
                assertTrue(schema.tables.any { it.name == "sample" })
                val unresolved = myFixture.doHighlighting().filter { it.description?.contains("resolve", true) == true }
                assertEmpty(unresolved)
                if (!completion) {
                    val offset = myFixture.file.text.indexOf("id")
                    val navigation = if (catalog == "spice") dev.sort.datafusionspice.sql.SpiceRelationNavigation()
                        else dev.sort.datafusionspice.sql.DatafusionRelationNavigation()
                    val targets = navigation.getGotoDeclarationTargets(myFixture.file.findElementAt(offset), offset, myFixture.editor).orEmpty()
                    assertTrue(targets.toList().toString(), targets.any {
                        ((it as? com.intellij.database.psi.DbElement)?.delegate as? com.intellij.database.model.DasObject)?.name == "id"
                    })
                }
            }
            LocalDataSourceManager.getInstance(project).removeDataSource(source)
            sources.remove(source)
            DbPsiFacade.getInstance(project).clearCaches()
        }
    }

    /** A recording metadata boundary, not a JDBC connection or user query execution. */
    private fun metadataConnection(catalog: String?, schema: String?, queries: MutableList<String>): DatabaseConnectionCore {
        fun proxy(type: Class<*>, call: (String, Array<out Any?>?, Class<*>) -> Any?): Any =
            Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, method, args ->
                when (method.name) {
                    "toString" -> "recording namespace metadata"
                    "hashCode" -> 1
                    "equals" -> false
                    else -> call(method.name, args, method.returnType)
                }
            }
        return proxy(DatabaseConnectionCore::class.java) { method, _, remoteType ->
            check(method == "getRemoteConnection") { method }
            proxy(remoteType) { remoteMethod, _, statementType ->
                check(remoteMethod == "createStatement") { remoteMethod }
                proxy(statementType) { statementMethod, args, rowsType ->
                    if (statementMethod == "close") null else {
                        check(statementMethod == "executeQuery") { statementMethod }
                        queries += args!![0] as String
                        var next = true
                        proxy(rowsType) { rowsMethod, rowArgs, metaType ->
                            when (rowsMethod) {
                                "next" -> next.also { next = false }
                                "close" -> null
                                "getString" -> if (rowArgs!![0] == 1) catalog else schema
                                "getMetaData" -> proxy(metaType) { metaMethod, metaArgs, _ ->
                                    when (metaMethod) {
                                        "getColumnCount" -> 2
                                        "getColumnLabel" -> if (metaArgs!![0] == 1) "catalog" else "schema"
                                        else -> error(metaMethod)
                                    }
                                }
                                else -> error(rowsMethod)
                            }
                        }
                    }
                }
            }
        } as DatabaseConnectionCore
    }
}

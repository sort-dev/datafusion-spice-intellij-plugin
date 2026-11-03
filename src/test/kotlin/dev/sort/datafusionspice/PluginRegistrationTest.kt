package dev.sort.datafusionspice

import com.intellij.lang.Language
import com.intellij.openapi.components.service
import com.intellij.database.Dbms
import dev.brikk.house.sql.intellij.dialect.SqlDialectRegistry
import dev.brikk.house.sql.intellij.testing.PluginRegistrationContract
import dev.brikk.house.sql.intellij.testing.assertIntrospectorRoutesToGeneric
import dev.sort.datafusionspice.catalog.*
import dev.sort.datafusionspice.sql.DatafusionSqlDialect
import dev.sort.datafusionspice.sql.SpiceSqlDialect

class PluginRegistrationTest : PluginPlatformTestCase() {
    fun testAllProductionDescriptorAdaptersAndServicesInstantiate() {
        val (registrations, services) = PluginRegistrationContract(
            "dev.sort.datafusion-spice-intellij-plugin", "dev.sort.datafusionspice",
        ).check(project)
        assertTrue(registrations > 50)
        assertTrue(services >= 6)
    }

    fun testBothIdentitiesAreRegisteredWithoutGenericOrPostgresAliases() {
        assertSame(DatafusionSqlDialect, Language.findLanguageByID("DataFusionSQL"))
        assertSame(SpiceSqlDialect, Language.findLanguageByID("SpiceSQL"))
        assertEquals("datafusion", SqlDialectRegistry.forLanguage(DatafusionSqlDialect)?.coreDialect)
        assertEquals("spice", SqlDialectRegistry.forLanguage(SpiceSqlDialect)?.coreDialect)
        assertNull(SqlDialectRegistry.forLanguage(requireNotNull(Language.findLanguageByID("GenericSQL"))))
        assertFalse(datafusionCatalog.dataSources.accepts(Dbms.POSTGRES))
        assertFalse(spiceCatalog.dataSources.accepts(Dbms.POSTGRES))
        assertFalse(datafusionCatalog.dataSources.accepts(SpiceDbms.SPICE_BRIKK))
        assertFalse(spiceCatalog.dataSources.accepts(DatafusionDbms.DATAFUSION_BRIKK))
    }

    fun testGenericIntrospectionCannotFallBackToPgCatalogQueries() {
        assertIntrospectorRoutesToGeneric(DatafusionDbms.DATAFUSION_BRIKK)
        assertIntrospectorRoutesToGeneric(SpiceDbms.SPICE_BRIKK)
    }

    fun testObserversRetainTheirOwnCatalogAndStableResolver() {
        val df = project.service<DatafusionExecutionObserver>()
        val sp = project.service<SpiceExecutionObserver>()
        assertEquals(DatafusionLiveCatalogService::class.java, df.profile.liveCatalog)
        assertEquals(SpiceLiveCatalogService::class.java, sp.profile.liveCatalog)
        assertSame(datafusionCatalog.dataSources, df.profile.dataSources)
        assertSame(spiceCatalog.dataSources, sp.profile.dataSources)
        df.start()
        df.start()
        sp.start()
        sp.start()
        assertFalse(datafusionCatalog.persistenceId == spiceCatalog.persistenceId)
    }

    fun testUnqualifiedNamespaceAndGraphicalExplainAreDisabled() {
        for (dialect in listOf(DatafusionDatabaseDialect(), SpiceDatabaseDialect())) {
            assertNull(dialect.sqlResetSearchPath())
        }
        val version = requireNotNull(com.intellij.database.util.Version.parse("55.2.0"))
        for (provider in listOf(DatafusionExplainPlanProvider(), SpiceExplainPlanProvider())) {
            assertFalse(provider.isSupported(version, false))
            assertFalse(provider.isSupported(version, true))
            assertFalse(provider.isRawSupported(version, false))
        }
    }
}

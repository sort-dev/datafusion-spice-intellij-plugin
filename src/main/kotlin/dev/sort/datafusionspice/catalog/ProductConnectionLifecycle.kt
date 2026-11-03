package dev.sort.datafusionspice.catalog

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.database.dataSource.DatabaseConnection
import com.intellij.database.dataSource.DatabaseConnectionCore
import com.intellij.database.dataSource.DatabaseConnectionInterceptor
import com.intellij.database.dataSource.DataSourceStorage
import com.intellij.database.dataSource.LocalDataSource
import com.intellij.database.util.SearchPath
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import dev.brikk.house.sql.intellij.catalog.*
import dev.brikk.house.sql.intellij.diagnostics.VerificationConnectionListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Read known information-schema settings, not user SQL or a PostgreSQL SET/SHOW.
internal const val NAMESPACE_QUERY = "SELECT c.value AS catalog, s.value AS schema " +
    "FROM information_schema.df_settings c CROSS JOIN information_schema.df_settings s " +
    "WHERE c.name = 'datafusion.catalog.default_catalog' AND s.name = 'datafusion.catalog.default_schema'"

internal val productSearchPath = CatalogSchemaSearchPath(
    quote = '"', useSchema = "USE {catalog}.{schema}",
    catalogProbes = listOf(NamespaceProbe.Column(NAMESPACE_QUERY, "catalog")),
    schemaProbes = listOf(NamespaceProbe.Column(NAMESPACE_QUERY, "schema")), schemaRequired = true,
)

/** Like Doris scope normalization, initialize only an unset scope, never a user's selection. */
internal fun initializeProductScope(source: LocalDataSource, namespace: SearchPath): Boolean {
    if (!source.introspectionScope.isEmpty) return false
    val path = namespace.current
    val names = generateSequence(path) { it.parent }.toList().asReversed().map { it.name }
    if (names.size != 2 || names.any(String::isBlank)) return false
    source.introspectionScope = CatalogScopes.select(listOf(com.intellij.database.model.ObjectKind.DATABASE,
        com.intellij.database.model.ObjectKind.SCHEMA), names)
    return true
}

abstract class ProductHarvest(private val service: Class<out LiveCatalogService>) : HarvestConnectionInterceptor(service) {
    override suspend fun handleConnected(connection: DatabaseConnectionCore, proto: DatabaseConnectionInterceptor.ProtoConnection) {
        val profile = ApplicationManager.getApplication().getService(service)?.profile ?: return
        if (!profile.enabled || !profile.dataSources.accepts(connection.dbms)) return
        super.handleConnected(connection, proto)
        if (proto.project.isDisposed) return
        if (isHelperRequestor((connection as? DatabaseConnection)?.requestor)) return
        val source = connection.connectionPoint.dataSource
        if (!source.introspectionScope.isEmpty) return
        val identity = profile.identity(source)
        val namespace = productSearchPath.readBack(connection) ?: return
        withContext(Dispatchers.EDT) {
            if (!proto.project.isDisposed && profile.identity(source) == identity && initializeProductScope(source, namespace)) {
                listOfNotNull(DataSourceStorage.getProjectStorage(proto.project), DataSourceStorage.getStorage())
                    .firstOrNull { source in it.ownDataSources }?.updateDataSource(source)
            }
        }
    }
}

// ClickHouse's connection-listener pattern: facts changing must not require a text edit.
class ProductConnectionListener : VerificationConnectionListener() {
    override fun connectionChanged(connection: DatabaseConnection, added: Boolean) {
        if (datafusionCatalog.dataSources.accepts(connection.dbms) || spiceCatalog.dataSources.accepts(connection.dbms)) {
            super.connectionChanged(connection, added)
        }
    }
}

internal fun restartProductHighlighting(project: Project) {
    ApplicationManager.getApplication().invokeLater({
        if (!project.isDisposed) DaemonCodeAnalyzer.getInstance(project).restart("SQL catalog facts changed")
    }, project.disposed)
}

package dev.sort.datafusionspice.catalog

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import dev.brikk.house.sql.intellij.catalog.*
import kotlinx.coroutines.CoroutineScope
import com.intellij.database.console.client.SessionClient
import com.intellij.database.console.session.DatabaseSession
import com.intellij.database.datagrid.DataRequest
import java.util.WeakHashMap

private fun observerProfile(catalog: DatafusionCatalogProfile, service: Class<out LiveCatalogService>, introspect: Class<out AutoIntrospectService>) =
    object : ExecutionObserverProfile {
        override val dbms = catalog.dbms
        override val dataSources = catalog.dataSources
        override val statements = catalog.statementDetector
        override val liveCatalog = service
        override val autoIntrospect = introspect
    }

abstract class ProductExecutionObserver(project: Project, scope: CoroutineScope) : ExecutionObserver(project, scope) {
    private val seen = WeakHashMap<DatabaseSession, LiveInventory?>()
    override fun requestFinished(context: DataRequest.Context) {
        if (project.isDisposed) return
        super.requestFinished(context)
        val session = (context.request.owner as? SessionClient<*>)?.session as? DatabaseSession ?: return
        val source = session.connectionPoint.dataSource
        if (!profile.dataSources.accepts(source.dbms)) return
        val type = profile.liveCatalog ?: return
        val catalog = com.intellij.openapi.application.ApplicationManager.getApplication().getService(type) ?: return
        val inventory = catalog.selection(source, session).inventory
        val changed = synchronized(seen) {
            val changed = !seen.containsKey(session) || seen[session] !== inventory
            seen[session] = inventory
            changed
        }
        if (changed) restartProductHighlighting(project)
    }
}

@Service(Service.Level.PROJECT)
class DatafusionExecutionObserver(project: Project, scope: CoroutineScope) : ProductExecutionObserver(project, scope) {
    override val profile = observerProfile(datafusionCatalog, DatafusionLiveCatalogService::class.java, DatafusionAutoIntrospectService::class.java)
}

@Service(Service.Level.PROJECT)
class SpiceExecutionObserver(project: Project, scope: CoroutineScope) : ProductExecutionObserver(project, scope) {
    override val profile = observerProfile(spiceCatalog, SpiceLiveCatalogService::class.java, SpiceAutoIntrospectService::class.java)
}

class DatafusionObserverStartup : ExecutionObserverStartup(DatafusionExecutionObserver::class.java)
class SpiceObserverStartup : ExecutionObserverStartup(SpiceExecutionObserver::class.java)

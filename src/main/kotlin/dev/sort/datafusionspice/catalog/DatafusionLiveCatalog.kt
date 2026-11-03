package dev.sort.datafusionspice.catalog

import com.intellij.openapi.components.Service
import dev.brikk.house.sql.intellij.catalog.*
import dev.sort.datafusionspice.DatafusionDbms
import kotlinx.coroutines.CoroutineScope
import com.intellij.database.dataSource.LocalDataSource
import com.intellij.openapi.project.Project

val datafusionCatalog: DatafusionCatalogProfile = ProductCatalogProfile(DatafusionDbms.DATAFUSION_BRIKK, "datafusion")

@Service(Service.Level.APP)
class DatafusionLiveCatalogService(scope: CoroutineScope) : LiveCatalogService(scope) {
    override val profile = datafusionCatalog
    override fun onRefreshed(project: Project, dataSource: LocalDataSource, outcome: RefreshOutcome, reason: RefreshReason) {
        super.onRefreshed(project, dataSource, outcome, reason)
        catalogRefreshCompleted(project, dataSource, profile, outcome, reason)
    }
}

class DatafusionHarvest : ProductHarvest(DatafusionLiveCatalogService::class.java)
class DatafusionCleanup : LiveCatalogCleanup(DatafusionLiveCatalogService::class.java)
class DatafusionRefreshCatalogAction : RefreshLiveCatalogAction(DatafusionLiveCatalogService::class.java)
